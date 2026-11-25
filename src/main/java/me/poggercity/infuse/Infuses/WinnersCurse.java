package me.poggercity.infuse.Infuses;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.function.Consumer;
import me.poggercity.infuse.Managers.CooldownManager;
import me.poggercity.infuse.Managers.EffectKeys;
import me.poggercity.infuse.Managers.Infuse;
import me.poggercity.infuse.Managers.Settings;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

/**
 * Winner's Curse (called Gamble in Infuse 2.7, when its config key was "gamble").
 *
 * Spark: a slot machine spins for effects.winners_curse.spark.roll_seconds (5 s) and lands on a random effect. The player
 * wins that effect's spark and has spark.use_within_seconds (30 s) to use it with their spark key; after that it is gone.
 * Winner's Curse's cooldown starts when the won spark is used (counted from when that spark ends, like every other spark)
 * or is gone. Using the won spark doesn't touch the player's own cooldown for that effect.
 *
 * Passive: the player has all the passives of a random effect, a different one every random_passive.minutes (5). The
 * effect classes ask borrowsPassive() in their "has this effect" checks. The current one and when it ends are stored with
 * the player, so relogging, draining and re-drinking or a restart don't give a new one early. It doesn't change while that
 * would hurt the player: in lava or burning (Fire), falling (Feather) or under water (Ocean).
 *
 * Ender, Apophis and Winner's Curse itself are never rolled.
 */
public class WinnersCurse implements Listener {
   public static final String KEY = "winners_curse";
   public static final String TITLE = "Winner's Curse";
   public static final String GEM_NAME = "Winner's Curse Effect";
   public static final String AUGMENTED_NAME = "Augmented Winner's Curse Effect";
   /** The names from Infuse 2.7, when this effect was called Gamble (potions made then still work). */
   public static final String LEGACY_GEM_NAME = "Gamble Effect";
   public static final String LEGACY_AUGMENTED_NAME = "Augmented Gamble Effect";
   public static final int MODEL_DATA = 16;
   /** How long before a passive ends the "would changing it now hurt" check starts (the rotation runs every 5 ticks). */
   private static final long SAFE_CHECK_MS = 1500L;
   /** A passive is kept at most this long past its time while changing it would hurt. */
   private static final long MAX_GRACE_MS = 300000L;
   /** The time to use a won spark waits at most this long for the player's own spark of that effect to end. */
   private static final long MAX_PAUSE_MS = 120000L;
   /** The effects Winner's Curse can roll, with the cooldown key their spark uses. */
   private static final Map<String, String> SPARK_KEYS = new LinkedHashMap<>();
   private static WinnersCurse instance;
   private final Infuse plugin;
   private final NamespacedKey passiveKey;
   private final NamespacedKey passiveEndKey;
   // Since when the current passive is kept past its time because changing it would hurt (stored with the player, so
   // relogging doesn't start the limit over).
   private final NamespacedKey passiveGraceKey;
   private final Random random = new Random();
   // The random passive of each online player (read from the player's data the first time it is needed).
   private final Map<UUID, Rolled> passives = new HashMap<>();
   // The won spark that was used, and until when it counts as running.
   private final Map<UUID, Rolled> sparks = new HashMap<>();
   private final Map<UUID, ScheduledTask> rolling = new HashMap<>();
   private final Map<UUID, SlotMachine> machines = new HashMap<>();
   // The won spark waiting to be used.
   private final Map<UUID, Held> held = new HashMap<>();
   private ScheduledTask rotation;

   static {
      SPARK_KEYS.put("emerald", "eme");
      SPARK_KEYS.put("feather", "feather");
      SPARK_KEYS.put("fire", "fire_spark");
      SPARK_KEYS.put("frost", "frost");
      SPARK_KEYS.put("haste", "haste");
      SPARK_KEYS.put("heart", "health");
      SPARK_KEYS.put("invis", "invis");
      SPARK_KEYS.put("ocean", "ocean");
      SPARK_KEYS.put("regen", "regen");
      SPARK_KEYS.put("speed", "speed");
      SPARK_KEYS.put("strength", "st");
      SPARK_KEYS.put("thunder", "thunder");
   }

   private static final class Rolled {
      final String effect;
      final long until;
      // (used won sparks) When Feather's slam threw the player again (0 = not yet): the next landing is its last one.
      final long slamAt;

      Rolled(String effect, long until) {
         this(effect, until, 0L);
      }

      Rolled(String effect, long until, long slamAt) {
         this.effect = effect;
         this.until = until;
         this.slamAt = slamAt;
      }
   }

   /** A won spark waiting to be used. */
   private static final class Held {
      final String effect;
      // Whether Winner's Curse was augmented when the slot machine started (half cooldown).
      final boolean augmented;
      long until;
      long lastCheck = System.currentTimeMillis();
      long paused;
      ScheduledTask task;
      long lastTick = -1L;

      Held(String effect, long until, boolean augmented) {
         this.effect = effect;
         this.until = until;
         this.augmented = augmented;
      }
   }

   public WinnersCurse(Infuse plugin) {
      this.plugin = plugin;
      // The 2.7 names, so the passive a player has keeps going after the update.
      this.passiveKey = new NamespacedKey(plugin, "gamble_passive");
      this.passiveEndKey = new NamespacedKey(plugin, "gamble_passive_ends");
      this.passiveGraceKey = new NamespacedKey(plugin, "gamble_passive_grace");
      instance = this;
   }

   /** Starts the passive rotation (checked 4 times a second, so the next passive follows the last one right away). */
   public void start() {
      this.rotation = Bukkit.getGlobalRegionScheduler().runAtFixedRate(this.plugin, task -> this.rotatePassives(), 5L, 5L);
   }

   public void shutdown() {
      if (this.rotation != null) {
         this.rotation.cancel();
      }

      for (ScheduledTask task : this.rolling.values()) {
         task.cancel();
      }

      for (Held h : this.held.values()) {
         if (h.task != null) {
            h.task.cancel();
         }
      }

      this.rolling.clear();
      this.machines.clear();
      this.held.clear();
      this.passives.clear();
      this.sparks.clear();
      if (instance == this) {
         instance = null;
      }
   }

   // ----- items and equipment -----

   public static boolean isCurseGem(ItemStack item) {
      return item != null
         && item.getType() == Material.POTION
         && item.hasItemMeta()
         && item.getItemMeta().hasCustomModelData()
         && item.getItemMeta().getCustomModelData() == MODEL_DATA;
   }

   /** "Winner's Curse Effect", "Augmented Winner's Curse Effect" (or their 2.7 Gamble names), with or without colors. */
   public static boolean isName(String hack) {
      if (hack == null) {
         return false;
      } else {
         String s = ChatColor.stripColor(hack).trim();
         return s.equalsIgnoreCase(GEM_NAME) || s.equalsIgnoreCase(AUGMENTED_NAME) || s.equalsIgnoreCase(LEGACY_GEM_NAME)
            || s.equalsIgnoreCase(LEGACY_AUGMENTED_NAME);
      }
   }

   private static boolean isAugmentedName(String hack) {
      return hack != null && (hack.equalsIgnoreCase(AUGMENTED_NAME) || hack.equalsIgnoreCase(LEGACY_AUGMENTED_NAME));
   }

   private static String slot(Player player, String slot) {
      String hack = Infuse.getInstance().getHackManager().getHack(player.getUniqueId(), slot);
      return hack == null ? null : ChatColor.stripColor(hack);
   }

   public static boolean hasCurse(Player player) {
      for (String s : new String[]{"1", "2"}) {
         if (isName(slot(player, s))) {
            return true;
         }
      }

      return false;
   }

   private static boolean isAugmented(Player player) {
      for (String s : new String[]{"1", "2"}) {
         if (isAugmentedName(slot(player, s))) {
            return true;
         }
      }

      return false;
   }

   /** Whether one of the player's two slots has this effect (normal or augmented). */
   private static boolean hasEquipped(Player player, String effect) {
      for (String s : new String[]{"1", "2"}) {
         if (effect.equals(EffectKeys.keyOf(slot(player, s)))) {
            return true;
         }
      }

      return false;
   }

   /** The effects Winner's Curse can ever roll, in config order. */
   public static List<String> rollable() {
      return new ArrayList<>(SPARK_KEYS.keySet());
   }

   private static boolean canRollSpark(String effect) {
      return Settings.flag(Settings.effectPath(KEY) + ".spark.can_roll." + effect, true);
   }

   private static boolean canRollPassive(String effect) {
      return Settings.flag(Settings.passivePath(KEY, "random_passive") + ".can_roll." + effect, true);
   }

   private static TextColor color(String effect) {
      return TextColor.fromHexString(EffectKeys.hex(effect));
   }

   private static Component name(String effect) {
      return Component.text(EffectKeys.title(effect), color(effect));
   }

   /** "⛏ Haste" in Haste's color. */
   private static Component iconAndName(String effect) {
      return Component.text(EffectKeys.icon(effect) + " ", color(effect)).append(name(effect).decorate(TextDecoration.BOLD));
   }

   private static Component prefix() {
      return Component.text(EffectKeys.icon(KEY) + " " + TITLE + ": ", color(KEY));
   }

   private static Color bukkitColor(String effect) {
      TextColor c = color(effect);
      return c == null ? Color.WHITE : Color.fromRGB(c.red(), c.green(), c.blue());
   }

   /** The effect's color mixed with a gray: the icons at the sides of the slot machine fade out. */
   private static TextColor faded(String effect, int gray, double amount) {
      TextColor c = color(effect);
      if (c == null) {
         return TextColor.color(gray, gray, gray);
      } else {
         return TextColor.color(
            (int)Math.round(c.red() * (1.0 - amount) + gray * amount),
            (int)Math.round(c.green() * (1.0 - amount) + gray * amount),
            (int)Math.round(c.blue() * (1.0 - amount) + gray * amount)
         );
      }
   }

   private static void sound(Player player, Sound sound, float volume, float pitch) {
      player.playSound(player.getLocation(), sound, SoundCategory.PLAYERS, volume, pitch);
   }

   /** A sound the players around hear too. */
   private static void soundAround(Player player, Sound sound, float volume, float pitch) {
      player.getWorld().playSound(player.getLocation(), sound, SoundCategory.PLAYERS, volume, pitch);
   }

   // ----- passive -----

   /**
    * True while this player has the passives of the given effect from Winner's Curse's random passive. Used in every
    * effect's "does this player have the effect" check, so the passives start and stop exactly like with the real effect.
    */
   public static boolean borrowsPassive(Player player, String effect) {
      WinnersCurse c = instance;
      if (c != null && player != null && player.isOnline()) {
         Rolled r = c.passiveOf(player);
         return r != null && effect.equals(r.effect) && System.currentTimeMillis() < r.until && Settings.passive(KEY, "random_passive") && hasCurse(player);
      } else {
         return false;
      }
   }

   /** The effect whose passives the player has from Winner's Curse right now, or null. */
   public static String currentPassive(Player player) {
      WinnersCurse c = instance;
      if (c != null && player != null && hasCurse(player) && Settings.passive(KEY, "random_passive")) {
         Rolled r = c.passiveOf(player);
         return r == null || System.currentTimeMillis() >= r.until ? null : r.effect;
      } else {
         return null;
      }
   }

   /** Seconds until the player's Winner's Curse passive changes (0 if there is none). */
   public static long passiveSecondsLeft(Player player) {
      WinnersCurse c = instance;
      Rolled r = c == null || player == null ? null : c.passiveOf(player);
      return r == null ? 0L : Math.max(0L, (r.until - System.currentTimeMillis()) / 1000L);
   }

   private Rolled passiveOf(Player player) {
      UUID id = player.getUniqueId();
      if (!this.passives.containsKey(id)) {
         PersistentDataContainer pdc = player.getPersistentDataContainer();
         String effect = pdc.get(this.passiveKey, PersistentDataType.STRING);
         Long until = pdc.get(this.passiveEndKey, PersistentDataType.LONG);
         this.passives.put(id, effect != null && until != null && SPARK_KEYS.containsKey(effect) ? new Rolled(effect, until) : null);
      }

      return this.passives.get(id);
   }

   private void setPassive(Player player, Rolled rolled) {
      this.passives.put(player.getUniqueId(), rolled);
      PersistentDataContainer pdc = player.getPersistentDataContainer();
      if (rolled == null) {
         pdc.remove(this.passiveKey);
         pdc.remove(this.passiveEndKey);
      } else {
         pdc.set(this.passiveKey, PersistentDataType.STRING, rolled.effect);
         pdc.set(this.passiveEndKey, PersistentDataType.LONG, rolled.until);
      }
   }

   private void rotatePassives() {
      if (Settings.passive(KEY, "random_passive")) {
         long now = System.currentTimeMillis();

         for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.isDead() && hasCurse(player) && !Settings.worldDisabled(player.getWorld())) {
               Rolled current = this.passiveOf(player);
               if (current == null || !canRollPassive(current.effect)) {
                  this.rollPassive(player, current, now);
               } else if (hasEquipped(player, current.effect) && this.hasOtherChoice(player, current.effect)) {
                  // The player equipped the effect they were borrowing, so it gave them nothing extra any more.
                  this.rollPassive(player, current, now);
               } else if (now + SAFE_CHECK_MS >= current.until && unsafeToChange(player, current.effect) && this.mayWait(player, current, now)) {
                  // Changing it now would hurt: kept a little longer, until it is safe.
                  this.setPassive(player, new Rolled(current.effect, Math.max(current.until, now + 2L * SAFE_CHECK_MS)));
               } else if (now >= current.until) {
                  this.rollPassive(player, current, now);
               }
            }
         }
      }
   }

   /**
    * Whether taking this borrowed passive away right now would hurt: Fire in lava or while burning (flight and Fire
    * Resistance would stop), Feather while falling (the landing would hurt), Ocean under water (no more water breathing).
    */
   private static boolean unsafeToChange(Player player, String effect) {
      switch (effect) {
         case "fire":
            return player.isInLava() || player.getFireTicks() > 0;
         case "feather":
            return !player.isOnGround() && player.getFallDistance() > 3.0F;
         case "ocean":
            return player.isUnderWater();
         default:
            return false;
      }
   }

   private boolean mayWait(Player player, Rolled current, long now) {
      PersistentDataContainer pdc = player.getPersistentDataContainer();
      Long since = pdc.get(this.passiveGraceKey, PersistentDataType.LONG);
      if (since == null) {
         since = Math.max(now, current.until);
         pdc.set(this.passiveGraceKey, PersistentDataType.LONG, since);
         // Said once, so the player knows why the passive didn't change.
         player.sendMessage(
            prefix()
               .append(Component.text("the ", NamedTextColor.GRAY))
               .append(iconAndName(current.effect))
               .append(Component.text(" passives stay until you are " + safePlace(current.effect) + ".", NamedTextColor.GRAY))
         );
      }

      return now - since < MAX_GRACE_MS;
   }

   private static String safePlace(String effect) {
      switch (effect) {
         case "fire":
            return "out of the lava and not burning";
         case "feather":
            return "on the ground";
         case "ocean":
            return "out of the water";
         default:
            return "safe";
      }
   }

   private boolean hasOtherChoice(Player player, String effect) {
      for (String e : SPARK_KEYS.keySet()) {
         if (!e.equals(effect) && canRollPassive(e) && !hasEquipped(player, e)) {
            return true;
         }
      }

      return false;
   }

   private void rollPassive(Player player, Rolled previous, long now) {
      player.getPersistentDataContainer().remove(this.passiveGraceKey);
      String last = previous == null ? null : previous.effect;
      List<String> pool = new ArrayList<>();

      // A different effect each time, and never one the player already has in their other slot.
      for (String effect : SPARK_KEYS.keySet()) {
         if (canRollPassive(effect) && !effect.equals(last) && !hasEquipped(player, effect)) {
            pool.add(effect);
         }
      }

      if (pool.isEmpty()) {
         for (String effect : SPARK_KEYS.keySet()) {
            if (canRollPassive(effect) && !effect.equals(last)) {
               pool.add(effect);
            }
         }
      }

      if (pool.isEmpty() && last != null && canRollPassive(last)) {
         pool.add(last);
      }

      if (pool.isEmpty()) {
         this.setPassive(player, null);
      } else {
         String next = pool.get(this.random.nextInt(pool.size()));
         double minutes = Math.max(0.05, Settings.value(KEY, "random_passive", "minutes", 5.0));
         this.setPassive(player, new Rolled(next, now + Math.round(minutes * 60000.0)));
         this.announcePassive(player, next, minutes);
      }
   }

   private static String describeMinutes(double minutes) {
      long seconds = Math.round(minutes * 60.0);
      if (seconds < 60L) {
         return seconds + " seconds";
      } else if (seconds % 60L == 0L) {
         long m = seconds / 60L;
         return m == 1L ? "1 minute" : m + " minutes";
      } else {
         return String.format(Locale.ROOT, "%.1f minutes", minutes);
      }
   }

   // Only a chat message: the action bar shows the icon of the current passive.
   private void announcePassive(Player player, String effect, double minutes) {
      player.sendMessage(
         prefix()
            .append(Component.text("you have the ", NamedTextColor.GRAY))
            .append(iconAndName(effect))
            .append(Component.text(" passives for the next " + describeMinutes(minutes) + ".", NamedTextColor.GRAY))
      );
   }

   // ----- spark -----

   /** True while the won spark that was used is still running (for example Feather's launch and landing). */
   public static boolean rolledSpark(Player player, String effect) {
      WinnersCurse c = instance;
      Rolled r = c == null || player == null ? null : c.sparks.get(player.getUniqueId());
      return r != null && effect.equals(r.effect) && System.currentTimeMillis() < r.until;
   }

   /** Keeps a used won spark running for at least this many more seconds (Feather's slam throws the player again). */
   public static void extendRolledSpark(Player player, String effect, long seconds) {
      WinnersCurse c = instance;
      if (c != null && player != null && rolledSpark(player, effect)) {
         long now = System.currentTimeMillis();
         long until = Math.max(c.sparks.get(player.getUniqueId()).until, now + seconds * 1000L);
         c.sparks.put(player.getUniqueId(), new Rolled(effect, until, now));
      }
   }

   /**
    * A landing that hurt (or would have): if it came after Feather's slam threw the player, the won spark is over, so it
    * doesn't keep protecting from other falls. (The slam's own landing, within half a second of it, doesn't count.)
    */
   public static void landedAfterSlam(Player player, String effect) {
      WinnersCurse c = instance;
      Rolled r = c == null || player == null ? null : c.sparks.get(player.getUniqueId());
      if (r != null && r.effect.equals(effect) && r.slamAt > 0L && System.currentTimeMillis() - r.slamAt > 500L) {
         c.sparks.remove(player.getUniqueId());
      }
   }

   /**
    * True while a won spark of this effect is running for the player, so their own spark of that effect has to wait: two
    * of the same spark at once end each other early (Invis shows the player again, Frost gives back the jumps).
    */
   public static boolean blocksOwnSpark(Player player, String effect) {
      return effect != null && SPARK_KEYS.containsKey(effect) && rolledSpark(player, effect) && sparkRunning(player, effect);
   }

   /** Whether the player's spark of this effect is running right now, whoever started it. */
   private static boolean sparkRunning(Player player, String effect) {
      String key = SPARK_KEYS.get(effect);
      return (key != null && CooldownManager.isEffectActive(player.getUniqueId(), key)) || (effect.equals("feather") && Feather.sparkPending(player));
   }

   /** The effect whose won spark this player can use right now, or null. */
   public static String heldSpark(Player player) {
      WinnersCurse c = instance;
      Held h = c == null || player == null ? null : c.held.get(player.getUniqueId());
      return h == null || System.currentTimeMillis() >= h.until ? null : h.effect;
   }

   /** Seconds left to use the won spark (rounded up), 0 if there is none. */
   public static long heldSecondsLeft(Player player) {
      WinnersCurse c = instance;
      Held h = c == null || player == null ? null : c.held.get(player.getUniqueId());
      return h == null ? 0L : Math.max(0L, (h.until - System.currentTimeMillis() + 999L) / 1000L);
   }

   public boolean isRolling(Player player) {
      return this.rolling.containsKey(player.getUniqueId());
   }

   private static int rollSeconds() {
      return Math.max(1, Math.min(30, Settings.sparkInt(KEY, "roll_seconds", 5)));
   }

   private static int holdSeconds() {
      return Math.max(1, Math.min(600, Settings.sparkInt(KEY, "use_within_seconds", 30)));
   }

   private static int baseCooldown(boolean augmented) {
      int cooldown = Math.max(0, Settings.sparkInt(KEY, "cooldown", 120));
      return augmented ? cooldown / 2 : cooldown;
   }

   /** The spark key: uses the won spark if there is one, otherwise spins the slot machine (when off cooldown). */
   public void activateSpark(Player player) {
      UUID id = player.getUniqueId();
      if (this.held.containsKey(id)) {
         this.useHeld(player);
      } else if (!this.rolling.containsKey(id) && !CooldownManager.isOnCooldown(id, KEY)) {
         this.spin(player);
      }
   }

   private void spin(Player player) {
      UUID id = player.getUniqueId();
      List<String> pool = new ArrayList<>();

      // Not a spark that is running for this player right now (their own Invis or Frost spark, say): two of the same
      // spark at once end each other early.
      for (String effect : SPARK_KEYS.keySet()) {
         if (canRollSpark(effect) && !sparkRunning(player, effect)) {
            pool.add(effect);
         }
      }

      if (pool.isEmpty()) {
         for (String effect : SPARK_KEYS.keySet()) {
            if (canRollSpark(effect)) {
               pool.add(effect);
            }
         }
      }

      if (pool.isEmpty()) {
         player.sendMessage(Component.text("The slot machine has no sparks to land on (they are all turned off in the config).", NamedTextColor.RED));
         return;
      }

      int rollSeconds = rollSeconds();
      boolean augmented = isAugmented(player);
      // The spin counts as the spark's duration: it shows in the action bar and effects can't be swapped meanwhile.
      CooldownManager.setDuration(id, KEY, rollSeconds);
      // Until the won spark is used or gone (which sets the real cooldown), the slot machine can't be used again.
      CooldownManager.setCooldown(id, KEY, rollSeconds + holdSeconds() + baseCooldown(augmented));
      String result = pool.get(this.random.nextInt(pool.size()));
      SlotMachine machine = new SlotMachine(player, pool, result, rollSeconds * 20, augmented);
      ScheduledTask task = player.getScheduler().runAtFixedRate(this.plugin, machine::tick, () -> {
         this.rolling.remove(id);
         this.machines.remove(id);
      }, 1L, 1L);
      if (task == null) {
         CooldownManager.clearSpecificDuration(id, KEY);
         CooldownManager.clearSpecificCooldown(id, KEY);
      } else {
         this.rolling.put(id, task);
         this.machines.put(id, machine);
         machine.start();
      }
   }

   /**
    * The spin is cancelled. The cooldown starts (startCooldown), except on death: dying resets every spark's cooldown
    * in this plugin, Winner's Curse's too.
    */
   private void stopSpin(Player player, ScheduledTask task, boolean augmented, boolean startCooldown) {
      task.cancel();
      UUID id = player.getUniqueId();
      this.rolling.remove(id);
      this.machines.remove(id);
      CooldownManager.clearSpecificDuration(id, KEY);
      if (startCooldown) {
         CooldownManager.setCooldown(id, KEY, baseCooldown(augmented));
      } else {
         CooldownManager.clearSpecificCooldown(id, KEY);
      }

      if (player.isOnline()) {
         player.clearTitle();
         player.sendMessage(prefix().append(Component.text("the slot machine stopped.", NamedTextColor.GRAY)));
         sound(player, Sound.BLOCK_VAULT_DEACTIVATE, 0.9F, 0.8F);
      }
   }

   /**
    * The ticks (from the start) at which the strip moves on: fast at first, slowing down towards the end like a real
    * slot machine. The first move is at "from", the last one before "until".
    */
   private static List<Integer> stepTicks(int from, int until) {
      List<Integer> out = new ArrayList<>();
      int span = Math.max(1, until - from);
      int t = 0;

      while (from + t < until) {
         out.add(from + t);
         double progress = (double)t / span;
         t += 2 + (int)Math.round(progress * progress * 10.0);
      }

      if (out.isEmpty()) {
         out.add(from);
      }

      return out;
   }

   /** The icons that pass the middle, one per step; the last one is the result. No icon twice in a row. */
   private List<String> reel(List<String> pool, String result, int length) {
      List<String> out = new ArrayList<>();

      for (int i = 0; i < length; i++) {
         String pick;
         if (i == length - 1) {
            pick = result;
         } else {
            pick = pool.get(this.random.nextInt(pool.size()));
            String before = out.isEmpty() ? null : out.get(out.size() - 1);
            boolean nextIsLast = i == length - 2;

            // The one just before the result is never the result: it looks like it might stop there, then clicks on.
            for (int tries = 0; pool.size() > 1 && tries < 10 && (pick.equals(before) || nextIsLast && pick.equals(result)); tries++) {
               pick = pool.get(this.random.nextInt(pool.size()));
            }
         }

         out.add(pick);
      }

      return out;
   }

   private static String at(List<String> reel, int i) {
      int n = reel.size();
      return reel.get((i % n + n) % n);
   }

   /** One frame of the strip: five icons, the middle one big and in color between two markers, fading out to the sides. */
   private static Component strip(List<String> reel, int i, TextColor marker) {
      String middle = at(reel, i);
      return Component.text()
         .append(Component.text(EffectKeys.icon(at(reel, i - 2)), faded(at(reel, i - 2), 0x40, 0.75)))
         .append(Component.text("   "))
         .append(Component.text(EffectKeys.icon(at(reel, i - 1)), faded(at(reel, i - 1), 0x8A, 0.45)))
         .append(Component.text("  "))
         .append(Component.text("▶ ", marker).decorate(TextDecoration.BOLD))
         .append(Component.text(EffectKeys.icon(middle), color(middle)).decorate(TextDecoration.BOLD))
         .append(Component.text(" ◀", marker).decorate(TextDecoration.BOLD))
         .append(Component.text("  "))
         .append(Component.text(EffectKeys.icon(at(reel, i + 1)), faded(at(reel, i + 1), 0x8A, 0.45)))
         .append(Component.text("   "))
         .append(Component.text(EffectKeys.icon(at(reel, i + 2)), faded(at(reel, i + 2), 0x40, 0.75)))
         .build();
   }

   /** The spinning slot machine of one player: a strip of effect icons that slows down and stops on the result. */
   private final class SlotMachine {
      final Player player;
      final String result;
      final int total;
      final int intro;
      final boolean augmented;
      final List<Integer> steps;
      final List<String> reel;
      int tick;
      int step = -1;
      double angle;
      boolean heartbeat;

      SlotMachine(Player player, List<String> pool, String result, int total, boolean augmented) {
         this.player = player;
         this.result = result;
         this.total = total;
         this.augmented = augmented;
         this.intro = Math.max(2, Math.min(8, total / 8));
         // The strip stops a moment before the jackpot.
         int pause = Math.max(2, Math.min(12, total / 6));
         this.steps = stepTicks(this.intro, total - pause);
         this.reel = WinnersCurse.this.reel(pool, result, this.steps.size());
      }

      /** Pulling the lever. */
      void start() {
         this.player.showTitle(
            Title.title(
               Component.text("✦ ", NamedTextColor.GOLD)
                  .decorate(TextDecoration.BOLD)
                  .append(Component.text("WINNER'S CURSE", NamedTextColor.YELLOW).decorate(TextDecoration.BOLD))
                  .append(Component.text(" ✦", NamedTextColor.GOLD).decorate(TextDecoration.BOLD)),
               Component.text("pulling the lever...", NamedTextColor.GRAY).decorate(TextDecoration.ITALIC),
               Title.Times.times(Duration.ZERO, Duration.ofMillis(this.intro * 50L + 300L), Duration.ZERO)
            )
         );
         sound(this.player, Sound.BLOCK_LEVER_CLICK, 1.0F, 0.6F);
         sound(this.player, Sound.BLOCK_VAULT_OPEN_SHUTTER, 0.9F, 1.0F);
         soundAround(this.player, Sound.BLOCK_VAULT_ACTIVATE, 0.7F, 1.4F);
      }

      void tick(ScheduledTask task) {
         UUID id = this.player.getUniqueId();
         if (!this.player.isOnline() || this.player.isDead() || !hasCurse(this.player) || Settings.worldDisabled(this.player.getWorld())) {
            WinnersCurse.this.stopSpin(this.player, task, this.augmented, !this.player.isDead());
            return;
         }

         int now = this.tick++;
         double progress = (double)now / this.total;
         if (this.step + 1 < this.steps.size() && now >= this.steps.get(this.step + 1)) {
            this.step++;
            boolean last = this.step == this.steps.size() - 1;
            int next = last ? this.total : this.steps.get(this.step + 1);
            String middle = this.reel.get(this.step);
            // The markers blink like the lights on a slot machine: gold and yellow, red and gold near the end.
            TextColor marker = last ? color(middle) : progress > 0.65
               ? (this.step % 2 == 0 ? NamedTextColor.RED : NamedTextColor.GOLD)
               : (this.step % 2 == 0 ? NamedTextColor.GOLD : NamedTextColor.YELLOW);
            Component subtitle = last ? name(middle).decorate(TextDecoration.BOLD) : name(middle);
            this.player.showTitle(
               Title.title(strip(this.reel, this.step, marker), subtitle, Title.Times.times(Duration.ZERO, Duration.ofMillis((next - now) * 50L + 250L), Duration.ZERO))
            );
            if (last) {
               // The strip stops with a heavy clunk, then a heartbeat before the jackpot.
               sound(this.player, Sound.BLOCK_VAULT_INSERT_ITEM, 1.0F, 0.8F);
               sound(this.player, Sound.BLOCK_IRON_TRAPDOOR_CLOSE, 0.6F, 1.5F);
               sound(this.player, Sound.ENTITY_WARDEN_HEARTBEAT, 1.0F, 1.0F);
            } else {
               float pitch = (float)(1.7 - 0.7 * progress);
               sound(this.player, Sound.BLOCK_NOTE_BLOCK_HAT, 0.7F, pitch);
               sound(this.player, Sound.UI_BUTTON_CLICK, 0.25F, pitch);
            }
         }

         if (!this.heartbeat && progress >= 0.7) {
            this.heartbeat = true;
            sound(this.player, Sound.ENTITY_WARDEN_HEARTBEAT, 0.7F, 1.15F);
         }

         // Two lights circle the player in the color of the icon in the middle, slowing down with the strip.
         if (now % 2 == 0 && this.step >= 0 && this.step < this.steps.size() - 1) {
            this.angle += 0.25 + 0.65 * (1.0 - progress);
            Location base = this.player.getLocation().add(0.0, 1.1, 0.0);
            Particle.DustOptions dust = new Particle.DustOptions(bukkitColor(this.reel.get(this.step)), 1.1F);

            for (int k = 0; k < 2; k++) {
               int count = Settings.particlesAt(KEY);
               if (count > 0) {
                  double a = this.angle + k * Math.PI;
                  this.player.getWorld().spawnParticle(Particle.DUST, base.clone().add(Math.cos(a) * 0.9, 0.0, Math.sin(a) * 0.9), count, 0.0, 0.0, 0.0, 0.0, dust);
               }
            }
         }

         if (now >= this.total) {
            task.cancel();
            WinnersCurse.this.rolling.remove(id);
            WinnersCurse.this.machines.remove(id);
            WinnersCurse.this.land(this.player, this.result, this.augmented);
         }
      }
   }

   /** The slot machine stopped: the player now holds the won spark for use_within_seconds. */
   private void land(Player player, String effect, boolean augmented) {
      UUID id = player.getUniqueId();
      int holdSeconds = holdSeconds();
      Held h = new Held(effect, System.currentTimeMillis() + holdSeconds * 1000L, augmented);
      this.held.put(id, h);
      // The wait counts as a duration too: the action bar shows it and effects can't be swapped meanwhile.
      CooldownManager.setDuration(id, KEY, holdSeconds);
      CooldownManager.setCooldown(id, KEY, holdSeconds + baseCooldown(augmented));
      this.jackpot(player, effect, holdSeconds);
      player.sendMessage(
         prefix()
            .append(Component.text("you won the ", NamedTextColor.GRAY))
            .append(iconAndName(effect))
            .append(Component.text(" spark! Use your spark within " + holdSeconds + " seconds or it's gone.", NamedTextColor.GRAY))
      );
      h.task = player.getScheduler().runAtFixedRate(this.plugin, t -> this.tickHeld(player, h, t), null, 5L, 5L);
      if (h.task == null) {
         this.dropHeld(player, h, null, true);
      }
   }

   /** The jackpot: the icon three times, flashing, with a little tune, coins, and a witch's laugh (it is a curse). */
   private void jackpot(Player player, String effect, int holdSeconds) {
      String icon = EffectKeys.icon(effect);
      Component subtitle = Component.text("You won the ", NamedTextColor.GRAY)
         .append(name(effect).decorate(TextDecoration.BOLD))
         .append(Component.text(" spark! Use it within " + holdSeconds + "s", NamedTextColor.GRAY));
      NamedTextColor[] flash = new NamedTextColor[]{NamedTextColor.GOLD, NamedTextColor.YELLOW, NamedTextColor.WHITE, NamedTextColor.GOLD};
      int[] frame = new int[]{0};
      Consumer<ScheduledTask> step = task -> {
         int now = frame[0]++;
         if (!player.isOnline()) {
            task.cancel();
            return;
         }

         if (now % 3 == 0 && now / 3 < flash.length) {
            NamedTextColor f = flash[now / 3];
            boolean lastFlash = now / 3 == flash.length - 1;
            player.showTitle(
               Title.title(
                  Component.text("✦  ", f)
                     .decorate(TextDecoration.BOLD)
                     .append(Component.text(icon + "   " + icon + "   " + icon, color(effect)).decorate(TextDecoration.BOLD))
                     .append(Component.text("  ✦", f).decorate(TextDecoration.BOLD)),
                  subtitle,
                  Title.Times.times(Duration.ZERO, Duration.ofMillis(lastFlash ? 2200L : 400L), Duration.ofMillis(lastFlash ? 500L : 0L))
               )
            );
         }

         switch (now) {
            case 0: {
               sound(player, Sound.BLOCK_NOTE_BLOCK_BELL, 1.0F, 1.0F);
               sound(player, Sound.ENTITY_PLAYER_LEVELUP, 0.8F, 1.3F);
               soundAround(player, Sound.BLOCK_VAULT_EJECT_ITEM, 1.0F, 1.0F);
               this.burst(player, effect);
               break;
            }
            case 2:
               sound(player, Sound.BLOCK_NOTE_BLOCK_BELL, 1.0F, 1.26F);
               break;
            case 4:
               sound(player, Sound.BLOCK_NOTE_BLOCK_BELL, 1.0F, 1.5F);
               soundAround(player, Sound.BLOCK_VAULT_EJECT_ITEM, 0.8F, 1.25F);
               break;
            case 6:
               sound(player, Sound.BLOCK_NOTE_BLOCK_BELL, 1.0F, 2.0F);
               sound(player, Sound.BLOCK_NOTE_BLOCK_CHIME, 0.8F, 2.0F);
               soundAround(player, Sound.BLOCK_VAULT_EJECT_ITEM, 0.7F, 1.5F);
               break;
            case 14:
               soundAround(player, Sound.ENTITY_WITCH_CELEBRATE, 0.8F, 1.0F);
               task.cancel();
               break;
            default:
               break;
         }
      };
      ScheduledTask task = player.getScheduler().runAtFixedRate(this.plugin, step, null, 1L, 1L);
      if (task == null) {
         sound(player, Sound.ENTITY_PLAYER_LEVELUP, 0.8F, 1.3F);
      }
   }

   private void burst(Player player, String effect) {
      Location center = player.getLocation().add(0.0, 1.0, 0.0);
      int totem = Settings.particles(KEY, 40);
      if (totem > 0) {
         player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, center, totem, 0.4, 0.6, 0.4, 0.35);
      }

      int sparkle = Settings.particles(KEY, 25);
      if (sparkle > 0) {
         player.getWorld().spawnParticle(Particle.ENCHANT, center, sparkle, 0.6, 0.8, 0.6, 0.8);
      }

      // A ring in the won effect's color.
      Particle.DustOptions dust = new Particle.DustOptions(bukkitColor(effect), 1.4F);

      for (int i = 0; i < 24; i++) {
         int count = Settings.particlesAt(KEY);
         if (count > 0) {
            double a = Math.PI * 2.0 * i / 24.0;
            player.getWorld().spawnParticle(Particle.DUST, center.clone().add(Math.cos(a) * 1.3, -0.3, Math.sin(a) * 1.3), count, 0.0, 0.0, 0.0, 0.0, dust);
         }
      }
   }

   private void tickHeld(Player player, Held h, ScheduledTask task) {
      UUID id = player.getUniqueId();
      if (this.held.get(id) != h || !player.isOnline()) {
         task.cancel();
      } else if (player.isDead()) {
         this.dropHeld(player, h, "was lost when you died", false);
      } else if (!hasCurse(player)) {
         this.dropHeld(player, h, "was lost with the effect", true);
      } else {
         this.pauseWhileOwnSparkRuns(player, h);
         long left = h.until - System.currentTimeMillis();
         if (left <= 0L) {
            this.dropHeld(player, h, "faded away", true);
         } else {
            // A little light in the won spark's color circles the player while it waits.
            if (Bukkit.getCurrentTick() % 10 < 5) {
               int count = Settings.particlesAt(KEY);
               if (count > 0) {
                  double a = System.currentTimeMillis() / 300.0;
                  Location at = player.getLocation().add(Math.cos(a) * 0.7, 1.2, Math.sin(a) * 0.7);
                  player.getWorld().spawnParticle(Particle.DUST, at, count, 0.0, 0.0, 0.0, 0.0, new Particle.DustOptions(bukkitColor(h.effect), 0.9F));
               }
            }

            // The last seconds tick like a clock.
            long seconds = (left + 999L) / 1000L;
            if (seconds <= 5L && seconds != h.lastTick) {
               h.lastTick = seconds;
               sound(player, Sound.BLOCK_NOTE_BLOCK_HAT, 0.6F, 0.7F + 0.1F * (5L - seconds));
            }
         }
      }
   }

   /**
    * The won spark can't be used while the player's own spark of that effect runs (see useHeld), so its time doesn't
    * run out meanwhile: the clock stops for up to MAX_PAUSE_MS.
    */
   private void pauseWhileOwnSparkRuns(Player player, Held h) {
      long now = System.currentTimeMillis();
      long step = Math.max(0L, now - h.lastCheck);
      h.lastCheck = now;
      if (step > 0L && h.paused < MAX_PAUSE_MS && sparkRunning(player, h.effect)) {
         long add = Math.min(step, MAX_PAUSE_MS - h.paused);
         h.paused += add;
         h.until += add;
         CooldownManager.setDuration(player.getUniqueId(), KEY, Math.max(1L, (h.until - now + 999L) / 1000L));
      }
   }

   /**
    * The won spark goes away unused (time is up, death, quit, effect lost). The cooldown starts now (startCooldown),
    * except on death: dying resets every spark's cooldown in this plugin, Winner's Curse's too.
    */
   private void dropHeld(Player player, Held h, String why, boolean startCooldown) {
      UUID id = player.getUniqueId();
      if (this.held.remove(id, h)) {
         if (h.task != null) {
            h.task.cancel();
         }

         CooldownManager.clearSpecificDuration(id, KEY);
         if (startCooldown) {
            CooldownManager.setCooldown(id, KEY, baseCooldown(h.augmented));
         } else {
            CooldownManager.clearSpecificCooldown(id, KEY);
         }

         if (why != null && player.isOnline()) {
            player.sendMessage(
               prefix()
                  .append(Component.text("the ", NamedTextColor.GRAY))
                  .append(iconAndName(h.effect))
                  .append(Component.text(" spark " + why + ".", NamedTextColor.GRAY))
            );
            sound(player, Sound.BLOCK_FIRE_EXTINGUISH, 0.5F, 1.6F);
            sound(player, Sound.BLOCK_VAULT_CLOSE_SHUTTER, 0.8F, 0.9F);
            int smoke = Settings.particles(KEY, 10);
            if (smoke > 0 && !player.isDead()) {
               player.getWorld().spawnParticle(Particle.SMOKE, player.getLocation().add(0.0, 1.2, 0.0), smoke, 0.3, 0.3, 0.3, 0.02);
            }
         }
      }
   }

   /** Uses the won spark. Its cooldown is Winner's Curse's, counted from when the won spark ends. */
   private void useHeld(Player player) {
      UUID id = player.getUniqueId();
      Held h = this.held.get(id);
      if (h != null) {
         this.pauseWhileOwnSparkRuns(player, h);
         if (System.currentTimeMillis() >= h.until) {
            this.dropHeld(player, h, "faded away", true);
         } else if (sparkRunning(player, h.effect)) {
            player.sendMessage(
               prefix()
                  .append(Component.text("your own ", NamedTextColor.GRAY))
                  .append(name(h.effect))
                  .append(Component.text(" spark is still running. The won one waits until it ends.", NamedTextColor.GRAY))
            );
            sound(player, Sound.BLOCK_VAULT_INSERT_ITEM_FAIL, 0.8F, 1.0F);
         } else {
            this.held.remove(id);
            if (h.task != null) {
               h.task.cancel();
            }

            CooldownManager.clearSpecificDuration(id, KEY);
            this.fire(player, h.effect);
            long active = Math.max(0L, (CooldownManager.getEffectTimeLeft(id, SPARK_KEYS.get(h.effect)) + 999L) / 1000L);
            if (active > 0L) {
               CooldownManager.setDuration(id, KEY, active);
            }

            CooldownManager.setCooldown(id, KEY, baseCooldown(h.augmented) + active);
            if (h.augmented) {
               sound(player, Sound.ENTITY_WARDEN_DEATH, 1.0F, 1.0F);
            } else {
               sound(player, Sound.BLOCK_BEACON_POWER_SELECT, 1.0F, 2.0F);
            }

            soundAround(player, Sound.BLOCK_VAULT_OPEN_SHUTTER, 0.6F, 1.6F);
         }
      }
   }

   private Consumer<Player> sparkOf(String effect) {
      Infuse p = this.plugin;
      switch (effect) {
         case "emerald":
            return p.getEmerald()::activateSpark;
         case "feather":
            return p.getFeather()::activateSpark;
         case "fire":
            return p.getFire()::activateSpark;
         case "frost":
            return p.getFrost()::activateSpark;
         case "haste":
            return p.getHaste()::activateSpark;
         case "heart":
            return p.getHeart()::activateSpark;
         case "invis":
            return p.getInvis()::activateSpark;
         case "ocean":
            return p.getOcean()::activateSpark;
         case "regen":
            return p.getRegen()::activateSpark;
         case "speed":
            return p.getSpeed()::activateSpark;
         case "strength":
            return p.getStrength()::activateSpark;
         case "thunder":
            return p.getThunder()::activateSpark;
         default:
            return null;
      }
   }

   /**
    * Uses another effect's spark for this player. That spark only works while its own cooldown is free, and it puts its
    * cooldown on; for a player who also has that effect, that would block (or use up) their own spark. So the cooldown is
    * set aside first and put back exactly as it was afterwards.
    */
   private void fire(Player player, String effect) {
      Consumer<Player> spark = this.sparkOf(effect);
      String cooldownKey = SPARK_KEYS.get(effect);
      if (spark != null && cooldownKey != null) {
         UUID id = player.getUniqueId();
         long savedEnd = CooldownManager.getCooldownEnd(id, cooldownKey);
         long savedSeconds = CooldownManager.getCachedDuration(id, cooldownKey);
         CooldownManager.clearSpecificCooldown(id, cooldownKey);
         // How long the spark counts as running. Feather's has no duration: its landing can come up to 30 s later.
         long window = effect.equals("feather") ? 30L : Math.max(0L, Math.round(Settings.spark(effect, "duration", 0.0))) + 10L;
         this.sparks.put(id, new Rolled(effect, System.currentTimeMillis() + window * 1000L));

         try {
            spark.accept(player);
         } catch (RuntimeException ex) {
            this.plugin.getLogger().warning("Winner's Curse: the " + effect + " spark failed for " + player.getName() + ": " + ex);
         } finally {
            CooldownManager.restoreCooldown(id, cooldownKey, savedEnd, savedSeconds);
         }
      }
   }

   // Dying stops the slot machine and loses a won spark. Like every spark's cooldown in this plugin, Winner's Curse's is
   // reset on death (Infuse.onDeath), so none is started here.
   @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
   public void onDeath(PlayerDeathEvent event) {
      Player player = event.getEntity();
      ScheduledTask task = this.rolling.get(player.getUniqueId());
      SlotMachine machine = this.machines.get(player.getUniqueId());
      if (task != null) {
         this.stopSpin(player, task, machine != null && machine.augmented, false);
      }

      Held h = this.held.get(player.getUniqueId());
      if (h != null) {
         this.dropHeld(player, h, "was lost when you died", false);
      }
   }

   @EventHandler(priority = EventPriority.MONITOR)
   public void onQuit(PlayerQuitEvent event) {
      Player player = event.getPlayer();
      UUID id = player.getUniqueId();
      ScheduledTask task = this.rolling.remove(id);
      SlotMachine machine = this.machines.remove(id);
      if (task != null) {
         task.cancel();
         CooldownManager.clearSpecificDuration(id, KEY);
         CooldownManager.setCooldown(id, KEY, baseCooldown(machine != null ? machine.augmented : isAugmented(player)));
      }

      // A won spark is gone when the player leaves (the cooldown starts, so leaving can't be used to spin again).
      Held h = this.held.get(id);
      if (h != null) {
         this.dropHeld(player, h, null, true);
      }

      // The passive is stored with the player, so nothing is lost here. A used won spark keeps its time limit (a
      // Feather launch is saved with the player and can still land after a relog).
      this.passives.remove(id);
   }
}
