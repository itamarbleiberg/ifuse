package me.poggercity.infuse.Infuses;

import me.poggercity.infuse.Managers.Settings;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Particle.DustOptions;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class particles {
   /** Spark activation burst, scaled by effect_settings.&lt;effect&gt;.particles.ability_percent. */
   public static void spawnEffect(String effect, Player player, Color color) {
      Location base = player.getLocation().add(0.0, 0.0, 0.0);
      int count = Settings.particles(effect, 30);
      double spread = 0.5;

      for (int i = 0; i < count; i++) {
         double x = (Math.random() - 0.5) * spread * 2.0;
         double y = Math.random() * 1.2;
         double z = (Math.random() - 0.5) * spread * 2.0;
         Location spawn = base.clone().add(x, y, z);
         player.getWorld().spawnParticle(Particle.ENTITY_EFFECT, spawn, 1, 0.0, 0.0, 0.0, 0.0, color);
      }
   }

   /** Passive aura for a normal effect. */
   public static void spawnEffect2(Player player, Color color, int amount) {
      if (amount > 0) {
         player.getWorld().spawnParticle(Particle.ENTITY_EFFECT, player.getLocation().add(0.0, 1.0, 0.0), amount, 0.0, 0.5, 0.0, 0.1, color);
      }
   }

   /** Passive aura for an augmented effect. */
   public static void spawnAugmented(Player player, Color color, int amount) {
      if (amount > 0) {
         DustOptions dust = new DustOptions(color, 1.5F);
         player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(0.0, 1.0, 0.0), amount, 0.3, 0.5, 0.3, 0.1, dust);
      }
   }

   public static void spawnHearts(String effect, Player player) {
      Location location = player.getLocation().add(0.0, 1.0, 0.0);
      Vector direction = player.getLocation().getDirection();
      double primarySpread = 0.5;
      double secondarySpread = 0.1;
      double offsetX;
      double offsetZ;
      if (Math.abs(direction.getZ()) > Math.abs(direction.getX())) {
         offsetX = primarySpread;
         offsetZ = secondarySpread;
      } else {
         offsetX = secondarySpread;
         offsetZ = primarySpread;
      }

      int count = Settings.particles(effect, 5);

      for (int i = 0; i < count; i++) {
         double randomX = (Math.random() * 2.0 - 1.0) * offsetX;
         double randomY = Math.pow(Math.random(), 2.0) * 1.0;
         double randomZ = (Math.random() * 2.0 - 1.0) * offsetZ;
         Location spawnLoc = location.clone().add(randomX, randomY, randomZ);
         player.getWorld().spawnParticle(Particle.HEART, spawnLoc, 1, 0.0, 0.0, 0.0, 0.0);
      }
   }
}
