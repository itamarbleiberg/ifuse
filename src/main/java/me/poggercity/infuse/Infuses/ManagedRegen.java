package me.poggercity.infuse.Infuses;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Regeneration kept on a player by a passive (Ocean in water, Apophis), done so that it really heals and so that only
 * the effect this passive added is ever touched.
 *
 * Regeneration heals when its remaining time is a multiple of (50 >> amplifier) ticks. Passives used to re-add a
 * 40-tick effect every tick, so it never got there and never healed. Here the effect is kept short and extended by
 * whole healing periods when it runs low. It starts just under one period, so getting it doesn't heal at once.
 *
 * The passive remembers the level and the end tick of what it added. Any Regeneration that doesn't match exactly
 * (a potion, a golden apple, a beacon, the Regen effect's on-hit Regeneration, another passive) is left alone.
 */
public final class ManagedRegen {
   // player -> {amplifier, tick at which the added effect ends}
   private final Map<UUID, int[]> added = new HashMap<>();

   private static int step(int amplifier) {
      int period = Math.max(1, 50 >> Math.max(0, amplifier));
      return period * Math.max(1, (int)Math.ceil(50.0 / period));
   }

   private boolean isOurs(Player p, PotionEffect current) {
      int[] mine = this.added.get(p.getUniqueId());
      if (mine == null || current == null || current.getAmplifier() != mine[0] || current.hasParticles() || current.isAmbient()) {
         return false;
      } else {
         int expected = mine[1] - Bukkit.getCurrentTick();
         return Math.abs(current.getDuration() - expected) <= 2;
      }
   }

   private void give(Player p, int duration, int amplifier) {
      p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, duration, amplifier, false, false, true));
      this.added.put(p.getUniqueId(), new int[]{amplifier, Bukkit.getCurrentTick() + duration});
   }

   /** Call every tick (or every passive update) with whether the player should have the Regeneration now. */
   public void tick(Player p, boolean wanted, int amplifier) {
      PotionEffect current = p.getPotionEffect(PotionEffectType.REGENERATION);
      boolean ours = this.isOurs(p, current);
      if (!wanted) {
         this.remove(p);
      } else if (current == null) {
         this.give(p, step(amplifier) - 1, amplifier);
      } else if (ours && current.getAmplifier() == amplifier) {
         if (current.getDuration() <= 10) {
            this.give(p, current.getDuration() + step(amplifier), amplifier);
         }
      } else if (ours) {
         // The level was changed in the config.
         p.removePotionEffect(PotionEffectType.REGENERATION);
         this.give(p, step(amplifier) - 1, amplifier);
      } else {
         // Someone else's Regeneration: leave it alone (ours, if hidden under it, runs out on its own within 3 s).
         this.added.remove(p.getUniqueId());
      }
   }

   /** Takes away the Regeneration this passive added, if that is what the player has right now. */
   public void remove(Player p) {
      if (this.isOurs(p, p.getPotionEffect(PotionEffectType.REGENERATION))) {
         p.removePotionEffect(PotionEffectType.REGENERATION);
      }

      this.added.remove(p.getUniqueId());
   }
}
