package me.poggercity.infuse.Infuses;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import me.poggercity.infuse.Managers.CooldownManager;
import me.poggercity.infuse.Managers.Infuse;
import me.poggercity.infuse.Managers.Settings;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Particle.DustOptions;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public class Speed implements Listener {
   public static final String KEY = "speed";
   private static final String STACKING = "speed_stacking";
   private final Plugin plugin;
   // Extra speed levels earned by hitting (0 = base level only)
   private final Map<UUID, Integer> stacks = new HashMap<>();
   private final Map<UUID, Long> lastHitTime = new HashMap<>();

   public Speed(Plugin plugin) {
      this.plugin = plugin;
   }

   private static int baseLevel() {
      return Math.max(0, Settings.intValue(KEY, STACKING, "base_level", 1));
   }

   // Stacks used to grow forever (Speed 20+ after a few seconds of hitting). They are now capped at max_level.
   private static int maxLevel() {
      return Math.max(baseLevel(), Settings.intValue(KEY, STACKING, "max_level", 3));
   }

   public void applySpeedEffects() {
      long now = System.currentTimeMillis();
      boolean enabled = Settings.passive(KEY, STACKING);
      long resetMs = Math.round(Settings.value(KEY, STACKING, "reset_seconds", 0.5) * 1000.0);
      int baseLevel = baseLevel();
      int maxLevel = maxLevel();

      for (Player p : Bukkit.getOnlinePlayers()) {
         UUID uuid = p.getUniqueId();
         if (!enabled || !this.hasSpeed(p, "1") && !this.hasSpeed(p, "2")) {
            this.stacks.remove(uuid);
         } else {
            if (now - this.lastHitTime.getOrDefault(uuid, 0L) > resetMs) {
               this.stacks.put(uuid, 0);
            }

            int level = Math.min(maxLevel, baseLevel + this.stacks.getOrDefault(uuid, 0));
            if (level > 0) {
               int amplifier = level - 1;
               Bukkit.getRegionScheduler().run(Infuse.getInstance(), p.getLocation(), task -> {
                  if (p.isOnline() && !p.isDead()) {
                     p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 10, amplifier, false, false, false));
                  }
               });
            }
         }
      }
   }

   // Taking damage resets the stacks.
   @EventHandler
   public void onPlayerDamage(EntityDamageEvent event) {
      if (event.getEntity() instanceof Player player) {
         UUID uuid = player.getUniqueId();
         this.stacks.put(uuid, 0);
         this.lastHitTime.put(uuid, System.currentTimeMillis());
      }
   }

   // Each hit (at most one every 200 ms) adds a speed level, up to max_level.
   // The old "enemy invincibility frames are halved" passive is removed: hits from Speed users no longer touch the
   // target's invincibility frames at all.
   @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
   public void onEntityDamageByEntityEvent(EntityDamageByEntityEvent event) {
      if (event.getDamager() instanceof Player player && (this.hasSpeed(player, "1") || this.hasSpeed(player, "2"))) {
         UUID uuid = player.getUniqueId();
         long currentTime = System.currentTimeMillis();
         long lastHit = this.lastHitTime.getOrDefault(uuid, 0L);
         if (currentTime - lastHit >= 200L) {
            this.lastHitTime.put(uuid, currentTime);
            int maxStacks = Math.max(0, maxLevel() - baseLevel());
            this.stacks.put(uuid, Math.min(maxStacks, this.stacks.getOrDefault(uuid, 0) + 1));
         }
      }
   }

   @EventHandler
   public void onQuit(PlayerQuitEvent event) {
      this.stacks.remove(event.getPlayer().getUniqueId());
      this.lastHitTime.remove(event.getPlayer().getUniqueId());
   }

   private boolean hasSpeed(Player player, String tier) {
      String currentHack = Infuse.getInstance().getHackManager().getHack(player.getUniqueId(), tier);
      return currentHack != null && (currentHack.equalsIgnoreCase(ChatColor.stripColor("Speed Effect")) || currentHack.equals("Augmented Speed Effect"))
         || WinnersCurse.borrowsPassive(player, KEY);
   }

   public void activateSpark(Player player) {
      UUID playerUUID = player.getUniqueId();
      if (!CooldownManager.isOnCooldown(playerUUID, "speed")) {
         particles.spawnEffect(KEY, player, Color.fromRGB(209, 164, 75));
         Vector direction = player.getEyeLocation().getDirection().normalize();
         double dashStrength = Infuse.getInstance().getConfig().getDouble("effects.speed.spark.dash_strength", 1.0);
         Vector dashVector = direction.clone().multiply(dashStrength);
         player.setVelocity(dashVector);
         DustOptions dustOptions = new DustOptions(Color.fromRGB(230, 220, 170), 1.5F);
         int trailCount = Settings.particles(KEY, 5);
         int[] ticksPassed = new int[]{0};
         Location[] previousLocation = new Location[]{player.getLocation().clone()};
         Location anchor = player.getLocation();
         Bukkit.getRegionScheduler().runAtFixedRate(this.plugin, anchor, task -> {
            if (!player.isOnline()) {
               task.cancel();
            } else {
               Location currentLocation = player.getLocation();
               double distance = previousLocation[0].getWorld() == currentLocation.getWorld() ? previousLocation[0].distance(currentLocation) : 0.0;
               if (distance > 0.1 && trailCount > 0) {
                  Vector step = currentLocation.toVector().subtract(previousLocation[0].toVector()).normalize().multiply(0.3);
                  Location particleLocation = previousLocation[0].clone();

                  for (double d = 0.0; d <= distance; d += step.length()) {
                     particleLocation.add(step);
                     player.getWorld().spawnParticle(Particle.DUST, particleLocation, trailCount, 0.1, 0.05, 0.1, 0.05, dustOptions);
                  }
               }

               previousLocation[0] = currentLocation.clone();
               if (ticksPassed[0] >= 3 && player.isOnGround() || ticksPassed[0] >= 200) {
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
         int configcooldown = Infuse.getInstance().getConfig().getInt("effects.speed.spark.cooldown", 20);
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
