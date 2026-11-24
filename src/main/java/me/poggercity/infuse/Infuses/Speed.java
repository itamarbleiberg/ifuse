package me.poggercity.infuse.Infuses;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import me.poggercity.infuse.Managers.CooldownManager;
import me.poggercity.infuse.Managers.Infuse;
import me.poggercity.infuse.Managers.PlayerHackManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Particle.DustOptions;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public class Speed implements Listener {
   private static final String GEM_NAME = HexColorTranslator.translateToMinecraftColor("#d1a44b") + "Speed Effect";
   private static final String SPEED = "speed";
   private final PlayerHackManager hackManager = new PlayerHackManager();
   private final Plugin plugin;
   private final Map<UUID, Integer> speedLevels = new HashMap<>();
   private final Map<UUID, Long> lastHitTime = new HashMap<>();
   private final Map<UUID, Long> bowPullStartTime = new HashMap<>();
   private final Map<UUID, Integer> originalMaxNoDamageTicks = new HashMap<>();
   private final Map<UUID, ScheduledTask> pendingRestores = new HashMap<>();

   public Speed(Plugin plugin) {
      this.plugin = plugin;
   }

   public void applySpeedEffects() {
      long currentTime = System.currentTimeMillis();
      Bukkit.getOnlinePlayers().stream().filter(p -> this.hasSpeed(p, "1") || this.hasSpeed(p, "2")).forEach(p -> {
         UUID uuid = p.getUniqueId();
         this.speedLevels.putIfAbsent(uuid, 1);
         if (currentTime - this.lastHitTime.getOrDefault(uuid, 0L) > 500L) {
            this.speedLevels.put(uuid, 1);
         }

         int currentLevel = Math.max(0, this.speedLevels.get(uuid) - 1);
         Location anchor = p.getLocation();
         Bukkit.getRegionScheduler().run(Infuse.getInstance(), anchor, task -> {
            if (p.isOnline() && !p.isDead()) {
               p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 10, currentLevel, false, false, false));
            }
         });
      });
   }

   @EventHandler
   public void onPlayerDamage(EntityDamageEvent event) {
      if (event.getEntity() instanceof Player) {
         Player player = (Player)event.getEntity();
         UUID uuid = player.getUniqueId();
         this.speedLevels.put(uuid, 1);
         this.lastHitTime.put(uuid, System.currentTimeMillis());
      }
   }

   // MONITOR so the cancelled check below sees the final outcome; this handler never modifies the event.
   @EventHandler(priority = EventPriority.MONITOR)
   public void onEntityDamageByEntityEvent(EntityDamageByEntityEvent event) {
      if (event.getDamager() instanceof Player player && (this.hasSpeed(player, "1") || this.hasSpeed(player, "2"))) {
         UUID uuid = player.getUniqueId();
         long currentTime = System.currentTimeMillis();
         long lastHit = this.lastHitTime.getOrDefault(uuid, 0L);
         if (currentTime - lastHit >= 200L) {
            this.lastHitTime.put(uuid, currentTime);
            this.speedLevels.put(uuid, this.speedLevels.getOrDefault(uuid, 1) + 1);
         }

         // BUG FIX (spam-hit damage): this used to set the target's max no-damage ticks to 10 and never restore it,
         // then reset its no-damage ticks to 0 one tick after the hit. That wiped the i-frames, so spam clicks landed
         // as full hits, and the target kept halved i-frames against every attacker even after the Speed user left.
         if (!event.isCancelled() && event.getEntity() instanceof LivingEntity target) {
            this.halveInvulnerability(target);
         }
      }
   }

   // "Enemy invincibility frames are halved". Vanilla only lets a full-damage hit through while
   // noDamageTicks <= maxNoDamageTicks / 2, and a full hit sets noDamageTicks = maxNoDamageTicks right after this
   // event. Halving the max here gives this hit a 5 tick window instead of 10 (default max 20). The real max is put
   // back when that window ends, so the shorter window only applies to hits from Speed users.
   // setNoDamageTicks is deliberately not used: on players it also sets spawn invulnerability.
   private void halveInvulnerability(LivingEntity target) {
      if (!(target.getNoDamageTicks() > target.getMaximumNoDamageTicks() / 2.0F)) {
         UUID id = target.getUniqueId();
         int original = this.originalMaxNoDamageTicks.computeIfAbsent(id, k -> target.getMaximumNoDamageTicks());
         int halved = original / 2;
         target.setMaximumNoDamageTicks(halved);
         ScheduledTask previous = this.pendingRestores.remove(id);
         if (previous != null) {
            previous.cancel();
         }

         this.pendingRestores.put(id, Bukkit.getRegionScheduler().runDelayed(this.plugin, target.getLocation(), task -> {
            this.pendingRestores.remove(id);
            Integer max = this.originalMaxNoDamageTicks.remove(id);
            if (max != null) {
               target.setMaximumNoDamageTicks(max);
            }
         }, Math.max(1, halved / 2)));
      }
   }

   // Scheduled restores do not run once the plugin is disabled (/reload), so restore every halved target now.
   @EventHandler
   public void onPluginDisable(PluginDisableEvent event) {
      if (event.getPlugin() == this.plugin) {
         for (Entry<UUID, Integer> entry : this.originalMaxNoDamageTicks.entrySet()) {
            if (Bukkit.getEntity(entry.getKey()) instanceof LivingEntity living) {
               living.setMaximumNoDamageTicks(entry.getValue());
            }
         }

         this.originalMaxNoDamageTicks.clear();
         this.pendingRestores.clear();
      }
   }

   private boolean hasSpeed(Player player, String tier) {
      String currentHack = Infuse.getInstance().getHackManager().getHack(player.getUniqueId(), tier);
      return currentHack == null ? false : currentHack.equalsIgnoreCase(ChatColor.stripColor("Speed Effect")) || currentHack.equals("Augmented Speed Effect");
   }

   public void activateSpark(Player player) {
      UUID playerUUID = player.getUniqueId();
      if (!CooldownManager.isOnCooldown(playerUUID, "speed")) {
         particles.spawnEffect(player, Color.fromRGB(209, 164, 75));
         Vector direction = player.getEyeLocation().getDirection().normalize();
         double dashStrength = Infuse.getInstance().getConfig().getDouble("damage.speed_dash_strength", 1.0);
         Vector dashVector = direction.clone().multiply(dashStrength);
         player.setVelocity(dashVector);
         DustOptions dustOptions = new DustOptions(Color.fromRGB(230, 220, 170), 1.5F);
         int[] ticksPassed = new int[]{0};
         Location[] previousLocation = new Location[]{player.getLocation().clone()};
         Location anchor = player.getLocation();
         Bukkit.getRegionScheduler().runAtFixedRate(this.plugin, anchor, task -> {
            if (!player.isOnline()) {
               task.cancel();
            } else {
               Location currentLocation = player.getLocation();
               double distance = previousLocation[0].distance(currentLocation);
               if (distance > 0.1) {
                  Vector step = currentLocation.toVector().subtract(previousLocation[0].toVector()).normalize().multiply(0.3);
                  Location particleLocation = previousLocation[0].clone();

                  for (double d = 0.0; d <= distance; d += step.length()) {
                     particleLocation.add(step);
                     player.getWorld().spawnParticle(Particle.DUST, particleLocation, 5, 0.1, 0.05, 0.1, 0.05, dustOptions);
                  }

                  previousLocation[0] = currentLocation.clone();
               }

               if (ticksPassed[0] >= 3 && player.isOnGround()) {
                  task.cancel();
               }

               ticksPassed[0]++;
            }
         }, 1L, 1L);
         CooldownManager.setDuration(playerUUID, "speed", 1L);
         boolean isAugmented = Infuse.getInstance().getHackManager().getHack(playerUUID, "1") != null
               && ChatColor.stripColor(Infuse.getInstance().getHackManager().getHack(playerUUID, "1")).toLowerCase().contains("augmented speed")
            || Infuse.getInstance().getHackManager().getHack(playerUUID, "2") != null
               && ChatColor.stripColor(Infuse.getInstance().getHackManager().getHack(playerUUID, "2")).toLowerCase().contains("augmented speed");
         int configcooldown = Infuse.getInstance().getConfig().getInt("cooldowns.speed_spark", 20);
         int cooldown = isAugmented ? configcooldown / 2 + 1 : configcooldown + 1;
         CooldownManager.setCooldown(playerUUID, "speed", cooldown);
      }
   }

   public static boolean isInvincibilityGem(ItemStack item) {
      return item != null
         && item.getType() == Material.POTION
         && item.hasItemMeta()
         && item.getItemMeta().hasCustomModelData()
         && item.getItemMeta().getCustomModelData() == 10;
   }
}
