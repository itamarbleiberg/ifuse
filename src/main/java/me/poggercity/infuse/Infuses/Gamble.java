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
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

/**
 * Gamble.
 *
 * Spark: a slot machine spins for effects.gamble.spark.roll_seconds (5 s), then the spark of the effect it lands on is
 * used. That spark doesn't touch the player's own cooldown for that effect (if they have it too).
 *
 * Passive: the player has all the passives of a random effect, a different one every random_passive.minutes (5).
 * The effect classes ask borrowsPassive() in their "has this effect" checks. The current one and when it ends are
 * stored with the player, so relogging, draining and re-drinking Gamble or a restart don't give a new one early.
 *
 * Ender, Apophis and Gamble itself are never rolled.
 */
public class Gamble implements Listener {
   public static final String KEY = "gamble";
   public static final String GEM_NAME = "Gamble Effect";
   public static final String AUGMENTED_NAME = "Augmented Gamble Effect";
   public static final int MODEL_DATA = 16;
   /** The effects Gamble can roll, with the cooldown key their spark uses. */
   private static final Map<String, String> SPARK_KEYS = new LinkedHashMap<>();
   private static Gamble instance;
   private final Infuse plugin;
   private final NamespacedKey passiveKey;
   private final NamespacedKey passiveEndKey;
   private final Random random = new Random();
   // The random passive of each online player (read from the player's data the first time it is needed).
   private final Map<UUID, Rolled> passives = new HashMap<>();
   // The spark the slot machine landed on, and until when it counts as running.
   private final Map<UUID, Rolled> sparks = new HashMap<>();
   private final Map<UUID, ScheduledTask> rolling = new HashMap<>();
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

      Rolled(String effect, long until) {
         this.effect = effect;
         this.until = until;
      }
   }

   public Gamble(Infuse plugin) {
      this.plugin = plugin;
      this.passiveKey = new NamespacedKey(plugin, "gamble_passive");
      this.passiveEndKey = new NamespacedKey(plugin, "gamble_passive_ends");
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

      this.rolling.clear();
      this.passives.clear();
      this.sparks.clear();
      if (instance == this) {
         instance = null;
      }
   }

   // ----- items and equipment -----

   public static boolean isGambleGem(ItemStack item) {
      return item != null
         && item.getType() == Material.POTION
         && item.hasItemMeta()
         && item.getItemMeta().hasCustomModelData()
         && item.getItemMeta().getCustomModelData() == MODEL_DATA;
   }

   private static String slot(Player player, String slot) {
      String hack = Infuse.getInstance().getHackManager().getHack(player.getUniqueId(), slot);
      return hack == null ? null : ChatColor.stripColor(hack);
   }

   public static boolean hasGamble(Player player) {
      for (String s : new String[]{"1", "2"}) {
         String hack = slot(player, s);
         if (hack != null && (hack.equalsIgnoreCase(GEM_NAME) || hack.equalsIgnoreCase(AUGMENTED_NAME))) {
            return true;
         }
      }

      return false;
   }

   private static boolean isAugmented(Player player) {
      for (String s : new String[]{"1", "2"}) {
         String hack = slot(player, s);
         if (hack != null && hack.equalsIgnoreCase(AUGMENTED_NAME)) {
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

   /** The effects Gamble can ever roll, in config order. */
   public static List<String> rollable() {
      return new ArrayList<>(SPARK_KEYS.keySet());
   }

   private static boolean canRollSpark(String effect) {
      return Settings.flag(Settings.effectPath(KEY) + ".spark.can_roll." + effect, true);
   }

   private static boolean canRollPassive(String effect) {
      return Settings.flag(Settings.passivePath(KEY, "random_passive") + ".can_roll." + effect, true);
   }

   private static Component name(String effect) {
      return Component.text(EffectKeys.title(effect), TextColor.fromHexString(EffectKeys.hex(effect)));
   }

   private static TextColor gambleColor() {
      return TextColor.fromHexString(EffectKeys.hex(KEY));
   }

   // ----- passive -----

   /**
    * True while this player has the passives of the given effect from Gamble's random passive. Used in every effect's
    * "does this player have the effect" check, so the passives start and stop exactly like with the real effect.
    */
   public static boolean borrowsPassive(Player player, String effect) {
      Gamble g = instance;
      if (g != null && player != null && player.isOnline()) {
         Rolled r = g.passiveOf(player);
         return r != null && effect.equals(r.effect) && System.currentTimeMillis() < r.until && Settings.passive(KEY, "random_passive") && hasGamble(player);
      } else {
         return false;
      }
   }

   /** The effect whose passives the player has from Gamble right now, or null. */
   public static String currentPassive(Player player) {
      Gamble g = instance;
      if (g != null && player != null && hasGamble(player) && Settings.passive(KEY, "random_passive")) {
         Rolled r = g.passiveOf(player);
         return r == null || System.currentTimeMillis() >= r.until ? null : r.effect;
      } else {
         return null;
      }
   }

   /** Seconds until the player's Gamble passive changes (0 if there is none). */
   public static long passiveSecondsLeft(Player player) {
      Gamble g = instance;
      Rolled r = g == null || player == null ? null : g.passiveOf(player);
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
            if (!player.isDead() && hasGamble(player) && !Settings.worldDisabled(player.getWorld())) {
               Rolled current = this.passiveOf(player);
               if (current == null || now >= current.until || !canRollPassive(current.effect)) {
                  this.rollPassive(player, current, now);
               }
            }
         }
      }
   }

   private void rollPassive(Player player, Rolled previous, long now) {
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

   private void announcePassive(Player player, String effect, double minutes) {
      String time = describeMinutes(minutes);
      player.sendMessage(
         Component.text("\ud83c\udfb2 Gamble: ", gambleColor())
            .append(Component.text("you have the ", NamedTextColor.GRAY))
            .append(name(effect).decorate(TextDecoration.BOLD))
            .append(Component.text(" passives for the next " + time + ".", NamedTextColor.GRAY))
      );
      // The slot machine uses the title while it spins.
      if (!this.rolling.containsKey(player.getUniqueId())) {
         player.showTitle(
            Title.title(
               Component.text(EffectKeys.icon(effect) + " ", TextColor.fromHexString(EffectKeys.hex(effect))).append(name(effect).decorate(TextDecoration.BOLD)),
               Component.text("Gamble passive for " + time, NamedTextColor.GRAY),
               Title.Times.times(Duration.ofMillis(150), Duration.ofMillis(1800), Duration.ofMillis(500))
            )
         );
      }

      player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0F, 1.2F);
   }

   // ----- spark -----

   /** True while the spark the slot machine landed on is still running (for example Feather's launch and landing). */
   public static boolean rolledSpark(Player player, String effect) {
      Gamble g = instance;
      Rolled r = g == null || player == null ? null : g.sparks.get(player.getUniqueId());
      return r != null && effect.equals(r.effect) && System.currentTimeMillis() < r.until;
   }

   /** Keeps a rolled spark running for at least this many more seconds (Feather's slam throws the player again). */
   public static void extendRolledSpark(Player player, String effect, long seconds) {
      Gamble g = instance;
      if (g != null && player != null && rolledSpark(player, effect)) {
         long until = Math.max(g.sparks.get(player.getUniqueId()).until, System.currentTimeMillis() + seconds * 1000L);
         g.sparks.put(player.getUniqueId(), new Rolled(effect, until));
      }
   }

   public boolean isRolling(Player player) {
      return this.rolling.containsKey(player.getUniqueId());
   }

   public void activateSpark(Player player) {
      UUID id = player.getUniqueId();
      if (!CooldownManager.isOnCooldown(id, KEY) && !this.rolling.containsKey(id)) {
         List<String> pool = new ArrayList<>();

         // Not a spark that is running for this player right now (their own Invis or Frost spark, say): when one of
         // the two ended it would end the other one's effects too.
         for (String effect : SPARK_KEYS.keySet()) {
            if (canRollSpark(effect) && !CooldownManager.isEffectActive(id, SPARK_KEYS.get(effect))) {
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

         int rollSeconds = Math.max(1, Math.min(30, Settings.sparkInt(KEY, "roll_seconds", 5)));
         int cooldown = Math.max(0, Settings.sparkInt(KEY, "cooldown", 120));
         // The spin counts as the spark's duration: it shows in the action bar and effects can't be swapped meanwhile.
         CooldownManager.setDuration(id, KEY, rollSeconds);
         CooldownManager.setCooldown(id, KEY, (isAugmented(player) ? cooldown / 2 : cooldown) + rollSeconds);
         String result = pool.get(this.random.nextInt(pool.size()));
         int total = rollSeconds * 20;
         List<Integer> changes = reelChanges(total);
         List<String> reel = this.reel(pool, result, changes.size());
         int[] tick = new int[]{0};
         int[] shown = new int[]{-1};
         ScheduledTask task = player.getScheduler().runAtFixedRate(this.plugin, t -> {
            if (!player.isOnline() || player.isDead() || !hasGamble(player) || Settings.worldDisabled(player.getWorld())) {
               this.stopRoll(player, t);
            } else {
               int now = tick[0]++;
               if (shown[0] + 1 < changes.size() && now >= changes.get(shown[0] + 1)) {
                  shown[0]++;
                  int next = shown[0] + 1 < changes.size() ? changes.get(shown[0] + 1) : total;
                  this.showReel(player, reel, shown[0], next - now);
                  float pitch = 0.8F + 1.0F * now / total;
                  player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.8F, pitch);
                  player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.35F, pitch);
               }

               if (now >= total) {
                  t.cancel();
                  this.rolling.remove(id);
                  this.land(player, result);
               }
            }
         }, () -> this.rolling.remove(id), 1L, 1L);
         if (task != null) {
            this.rolling.put(id, task);
            player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1.0F, 2.0F);
         }
      }
   }

   private void stopRoll(Player player, ScheduledTask task) {
      task.cancel();
      this.rolling.remove(player.getUniqueId());
      CooldownManager.clearSpecificDuration(player.getUniqueId(), KEY);
      if (player.isOnline()) {
         player.clearTitle();
         player.sendMessage(Component.text("\ud83c\udfb2 The slot machine stopped.", NamedTextColor.GRAY));
      }
   }

   /** The ticks at which the reel moves on: fast at first, slowing down towards the end like a real slot machine. */
   private static List<Integer> reelChanges(int total) {
      List<Integer> out = new ArrayList<>();
      int t = 0;

      while (t < total) {
         out.add(t);
         double progress = (double)t / total;
         t += 2 + (int)Math.round(progress * progress * 10.0);
      }

      return out;
   }

   /** The symbols the reel shows, one per change; the last one is the result. No symbol twice in a row. */
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

            for (int tries = 0; pool.size() > 1 && tries < 10 && (pick.equals(before) || nextIsLast && pick.equals(result)); tries++) {
               pick = pool.get(this.random.nextInt(pool.size()));
            }
         }

         out.add(pick);
      }

      return out;
   }

   private static Component reelLine(String left, String middle, String right) {
      return Component.text("[ ", NamedTextColor.DARK_GRAY)
         .append(Component.text(EffectKeys.icon(left), NamedTextColor.GRAY))
         .append(Component.text("   "))
         .append(Component.text(EffectKeys.icon(middle), TextColor.fromHexString(EffectKeys.hex(middle))).decorate(TextDecoration.BOLD))
         .append(Component.text("   "))
         .append(Component.text(EffectKeys.icon(right), NamedTextColor.GRAY))
         .append(Component.text(" ]", NamedTextColor.DARK_GRAY));
   }

   private void showReel(Player player, List<String> reel, int index, int ticksShown) {
      String middle = reel.get(index);
      String left = index > 0 ? reel.get(index - 1) : reel.get(reel.size() - 1);
      String right = index + 1 < reel.size() ? reel.get(index + 1) : reel.get(0);
      player.showTitle(
         Title.title(
            reelLine(left, middle, right),
            name(middle),
            Title.Times.times(Duration.ZERO, Duration.ofMillis(Math.max(150L, ticksShown * 50L + 150L)), Duration.ZERO)
         )
      );
   }

   private void land(Player player, String effect) {
      String icon = EffectKeys.icon(effect);
      TextColor color = TextColor.fromHexString(EffectKeys.hex(effect));
      player.showTitle(
         Title.title(
            Component.text(icon + "   " + icon + "   " + icon, color).decorate(TextDecoration.BOLD),
            name(effect).append(Component.text(" spark!", NamedTextColor.WHITE)),
            Title.Times.times(Duration.ZERO, Duration.ofMillis(1500), Duration.ofMillis(500))
         )
      );
      player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.4F);
      player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0F, 1.6F);
      int burst = Settings.particles(KEY, 40);
      if (burst > 0) {
         player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation().add(0.0, 1.0, 0.0), burst, 0.4, 0.6, 0.4, 0.3);
      }

      player.sendMessage(
         Component.text("\ud83c\udfb2 Gamble: ", gambleColor())
            .append(Component.text("the slot machine landed on ", NamedTextColor.GRAY))
            .append(name(effect).decorate(TextDecoration.BOLD))
            .append(Component.text("!", NamedTextColor.GRAY))
      );
      this.useSpark(player, effect);
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
    * Uses another effect's spark for this player. That spark only works while its own cooldown is free, and it puts
    * its cooldown on; for a player who also has that effect, that would block (or use up) their own spark. So the
    * cooldown is set aside first and put back exactly as it was afterwards.
    */
   private void useSpark(Player player, String effect) {
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
            this.plugin.getLogger().warning("Gamble: the " + effect + " spark failed for " + player.getName() + ": " + ex);
         } finally {
            CooldownManager.restoreCooldown(id, cooldownKey, savedEnd, savedSeconds);
         }

         player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1.0F, 2.0F);
      }
   }

   @EventHandler(priority = EventPriority.MONITOR)
   public void onQuit(PlayerQuitEvent event) {
      UUID id = event.getPlayer().getUniqueId();
      ScheduledTask task = this.rolling.remove(id);
      if (task != null) {
         task.cancel();
         CooldownManager.clearSpecificDuration(id, KEY);
      }

      // The passive is stored with the player, so nothing is lost here. A rolled spark keeps its time limit (a
      // Feather launch is saved with the player and can still land after a relog).
      this.passives.remove(id);
   }
}
