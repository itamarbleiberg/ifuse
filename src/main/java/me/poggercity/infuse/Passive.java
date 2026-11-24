package me.poggercity.infuse;

import me.poggercity.infuse.Infuses.particles;
import me.poggercity.infuse.Managers.Infuse;
import me.poggercity.infuse.Managers.PlayerHackManager;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

public class Passive {
   private final PlayerHackManager hackManager = new PlayerHackManager();

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
               this.spawn(player, Color.fromRGB(139, 0, 0), isAugmented);
            } else if (hack.contains("thunder")) {
               this.spawn(player, Color.fromRGB(252, 237, 0), isAugmented);
            } else if (hack.contains("emerald")) {
               this.spawn(player, Color.GREEN, isAugmented);
            } else if (hack.contains("feather")) {
               this.spawn(player, Color.fromRGB(190, 163, 202), isAugmented);
            } else if (hack.contains("fire")) {
               this.spawn(player, Color.fromRGB(252, 120, 3), isAugmented);
            } else if (hack.contains("frost")) {
               this.spawn(player, Color.fromRGB(0, 255, 255), isAugmented);
            } else if (hack.contains("haste")) {
               this.spawn(player, Color.fromRGB(185, 108, 0), isAugmented);
            } else if (hack.contains("heart")) {
               this.spawn(player, Color.fromRGB(252, 0, 70), isAugmented);
            } else if (hack.contains("ocean")) {
               this.spawn(player, Color.fromRGB(0, 90, 252), isAugmented);
            } else if (hack.contains("regen")) {
               this.spawn(player, Color.fromRGB(255, 3, 239), isAugmented);
            } else if (hack.contains("speed")) {
               this.spawn(player, Color.fromRGB(209, 164, 75), isAugmented);
            } else if (hack.contains("ender")) {
               spawnDragon(player);
            }
         }
      }
   }

   public static void spawnDragon(Player player) {
      player.getWorld().spawnParticle(Particle.REVERSE_PORTAL, player.getLocation().add(0.0, 1.0, 0.0), 32, 0.3, 0.5, 0.3, 0.0);
   }

   private void spawn(Player player, Color color, boolean isAugmented) {
      if (isAugmented) {
         particles.spawnAugmented(player, color);
      } else {
         particles.spawnEffect2(player, color);
      }
   }
}
