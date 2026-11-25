package me.poggercity.infuse;

import me.poggercity.infuse.Infuses.particles;
import me.poggercity.infuse.Managers.Infuse;
import me.poggercity.infuse.Managers.Settings;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

/** Aura particles around players with effects (every 19 ticks). Amount per effect: effects.&lt;effect&gt;.particles. */
public class Passive {
   public void startTask() {
      Bukkit.getGlobalRegionScheduler().runAtFixedRate(Infuse.getInstance(), task -> {
         for (Player player : Bukkit.getOnlinePlayers()) {
            this.applyParticlesForHack(player, "1");
            this.applyParticlesForHack(player, "2");
         }
      }, 1L, 19L);
   }

   private void applyParticlesForHack(Player player, String type) {
      if (!player.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
         String hack = Infuse.getInstance().getHackManager().getHack(player.getUniqueId(), type);
         if (hack != null) {
            hack = hack.toLowerCase();
            boolean isAugmented = hack.contains("augmented");
            if (hack.contains("strength")) {
               this.spawn("strength", player, Color.fromRGB(139, 0, 0), isAugmented);
            } else if (hack.contains("thunder")) {
               this.spawn("thunder", player, Color.fromRGB(252, 237, 0), isAugmented);
            } else if (hack.contains("emerald")) {
               this.spawn("emerald", player, Color.GREEN, isAugmented);
            } else if (hack.contains("feather")) {
               this.spawn("feather", player, Color.fromRGB(190, 163, 202), isAugmented);
            } else if (hack.contains("fire")) {
               this.spawn("fire", player, Color.fromRGB(252, 120, 3), isAugmented);
            } else if (hack.contains("frost")) {
               this.spawn("frost", player, Color.fromRGB(0, 255, 255), isAugmented);
            } else if (hack.contains("haste")) {
               this.spawn("haste", player, Color.fromRGB(185, 108, 0), isAugmented);
            } else if (hack.contains("heart")) {
               this.spawn("heart", player, Color.fromRGB(252, 0, 70), isAugmented);
            } else if (hack.contains("ocean")) {
               this.spawn("ocean", player, Color.fromRGB(0, 90, 252), isAugmented);
            } else if (hack.contains("regen")) {
               this.spawn("regen", player, Color.fromRGB(255, 3, 239), isAugmented);
            } else if (hack.contains("speed")) {
               this.spawn("speed", player, Color.fromRGB(209, 164, 75), isAugmented);
            } else if (hack.contains("ender")) {
               if (Settings.auraEnabled("ender")) {
                  spawnDragon(player, Settings.auraAmount("ender") * 16);
               }
            } else if (hack.contains("apophis")) {
               this.spawn("apophis", player, Color.fromRGB(48, 17, 69), isAugmented);
            } else if (hack.contains("gamble")) {
               this.spawn("gamble", player, Color.fromRGB(30, 232, 182), isAugmented);
            }
         }
      }
   }

   public static void spawnDragon(Player player, int amount) {
      if (amount > 0) {
         player.getWorld().spawnParticle(Particle.REVERSE_PORTAL, player.getLocation().add(0.0, 1.0, 0.0), amount, 0.3, 0.5, 0.3, 0.0);
      }
   }

   private void spawn(String effect, Player player, Color color, boolean isAugmented) {
      if (Settings.auraEnabled(effect)) {
         int amount = Settings.auraAmount(effect);
         if (isAugmented) {
            particles.spawnAugmented(player, color, amount);
         } else {
            particles.spawnEffect2(player, color, amount);
         }
      }
   }
}
