package me.poggercity.infuse.Infuses;

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
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

public class Strength implements Listener {
   public static final String KEY = "strength";
   private static final String GEM_NAME = ChatColor.DARK_RED + "Strength Effect";
   private static final String AUTO_CRIT_KEY = "st";
   private static final double CRIT_MULTIPLIER = 1.35;

   private boolean hasStrengthHackEquipped(Player player) {
      UUID uuid = player.getUniqueId();
      String legendaryHack = Infuse.getInstance().getHackManager().getHack(uuid, "1");
      String commonHack = Infuse.getInstance().getHackManager().getHack(uuid, "2");
      return legendaryHack != null && (legendaryHack.equalsIgnoreCase(ChatColor.stripColor(GEM_NAME)) || legendaryHack.equals("Augmented Strength Effect"))
         || commonHack != null && (commonHack.equalsIgnoreCase(ChatColor.stripColor(GEM_NAME)) || commonHack.equals("Augmented Strength Effect"))
         || WinnersCurse.borrowsPassive(player, KEY);
   }

   private void updateAttackDamageBasedOnHealth(Player player) {
      if (!this.hasStrengthHackEquipped(player) || !Settings.passive(KEY, "low_health_damage")) {
         this.resetAttackDamage(player);
      } else {
         double currentHealth = player.getHealth();
         AttributeInstance attackDamage = player.getAttribute(Attribute.ATTACK_DAMAGE);
         if (attackDamage != null) {
            double baseValue = attackDamage.getBaseValue();
            double newValue = 1.0;
            if (currentHealth <= 4.0) {
               newValue = 4.0;
            } else if (currentHealth <= 8.0) {
               newValue = 3.0;
            } else if (currentHealth <= 12.0) {
               newValue = 2.0;
            }

            if (baseValue != newValue) {
               attackDamage.setBaseValue(newValue);
            }
         }
      }
   }

   /** Runs every tick so the low-health bonus follows health right away, and is removed when Strength is gone. */
   public void tickAttackDamage() {
      for (Player player : Bukkit.getOnlinePlayers()) {
         if (!player.isDead()) {
            this.updateAttackDamageBasedOnHealth(player);
         }
      }
   }

   private void resetAttackDamage(Player player) {
      AttributeInstance attackDamage = player.getAttribute(Attribute.ATTACK_DAMAGE);
      if (attackDamage != null && attackDamage.getBaseValue() != 1.0) {
         attackDamage.setBaseValue(1.0);
      }
   }

   public void activateSpark(Player player) {
      UUID playerUUID = player.getUniqueId();
      if (!CooldownManager.isOnCooldown(playerUUID, "st")) {
         int duration = Infuse.getInstance().getConfig().getInt("effects.strength.spark.duration", 15);
         particles.spawnEffect("strength", player, Color.fromRGB(139, 0, 0));
         CooldownManager.setDuration(playerUUID, "st", duration);
         int configcooldown = Infuse.getInstance().getConfig().getInt("effects.strength.spark.cooldown", 60);
         boolean isAugmented = Infuse.getInstance().getHackManager().getHack(playerUUID, "1") != null
               && ChatColor.stripColor(Infuse.getInstance().getHackManager().getHack(playerUUID, "1")).toLowerCase().contains("augmented strength")
            || Infuse.getInstance().getHackManager().getHack(playerUUID, "2") != null
               && ChatColor.stripColor(Infuse.getInstance().getHackManager().getHack(playerUUID, "2")).toLowerCase().contains("augmented strength");
         int cooldown = isAugmented ? configcooldown / 2 + duration : configcooldown + duration;
         CooldownManager.setCooldown(playerUUID, "st", cooldown);
      }
   }

   @EventHandler
   public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
      if (event.getDamager() instanceof Player player) {
         if (CooldownManager.isEffectActive(player.getUniqueId(), "st") && !event.isCritical()) {
            double originalDamage = event.getDamage();
            double critDamage = originalDamage * 1.35;
            event.setDamage(critDamage);
            Entity hitEntity = event.getEntity();
            hitEntity.getWorld().playSound(hitEntity.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0F, 1.0F);
            int crits = Settings.particles(KEY, 20);
            if (crits > 0) {
               hitEntity.getWorld().spawnParticle(Particle.CRIT, hitEntity.getLocation().add(0.0, 1.5, 0.0), crits, 0.0, 0.0, 0.0, 0.5);
               hitEntity.getWorld().spawnParticle(Particle.CRIT, hitEntity.getLocation().add(0.0, 0.5, 0.0), crits, 0.0, 0.0, 0.0, 0.5);
            }
         }

         this.updateAttackDamageBasedOnHealth(player);
      }
   }

   @EventHandler
   public void onEntityShootBow(EntityShootBowEvent event) {
      if (event.getEntity() instanceof Player player
         && event.getBow() != null
         && event.getBow().getType() == Material.BOW
         && Settings.passive(KEY, "piercing_arrows")
         && this.hasStrengthHackEquipped(player)
         && event.getProjectile() instanceof Arrow arrow) {
         arrow.setPierceLevel(100);
      }
   }

   @EventHandler
   public void onEntityDamageMob(EntityDamageByEntityEvent event) {
      if (event.getDamager() instanceof Player attacker
         && event.getEntity() instanceof LivingEntity entity
         && !(entity instanceof Player)
         && Settings.passive(KEY, "mob_damage")
         && this.hasStrengthHackEquipped(attacker)) {
         double originalDamage = event.getDamage();
         event.setDamage(originalDamage * Math.max(0.0, Settings.value(KEY, "mob_damage", "multiplier", 2.0)));
      }
   }

   @EventHandler
   public void onEntityDamageByEntity2(EntityDamageByEntityEvent event) {
      if (event.getEntity() instanceof Player player) {
         ItemStack offHand = player.getInventory().getItemInOffHand();
         if (offHand.getType() == Material.SHIELD
            && player.isBlocking()
            && event.getDamager() instanceof Player attacker
            && attacker.getInventory().getItemInMainHand().getType().toString().endsWith("_AXE")
            && Settings.passive(KEY, "shield_disable")
            && this.hasStrengthHackEquipped(attacker)) {
            player.getWorld().playSound(player.getLocation(), Sound.ITEM_SHIELD_BREAK, 1.0F, 1.0F);
            Location anchor = player.getLocation();
            Bukkit.getRegionScheduler().runDelayed(Infuse.getInstance(), anchor, task -> this.stunShield(player), 20L);
         }
      }
   }

   @EventHandler
   public void onPlayerDeath(PlayerDeathEvent event) {
      Player player = event.getEntity();
      this.resetAttackDamage(player);
   }

   @EventHandler
   public void onPlayerQuit(PlayerQuitEvent event) {
      Player player = event.getPlayer();
      this.resetAttackDamage(player);
   }

   @EventHandler
   public void onPlayerRegainHealth(EntityRegainHealthEvent event) {
      if (event.getEntity() instanceof Player) {
         Player player = (Player)event.getEntity();
         Location anchor = player.getLocation();
         Bukkit.getRegionScheduler().runDelayed(Infuse.getInstance(), anchor, task -> this.updateAttackDamageBasedOnHealth(player), 1L);
      }
   }

   public static boolean isStealthGem(ItemStack item) {
      return item != null && item.getType() == Material.POTION && item.getItemMeta() != null
         ? item.getItemMeta().hasCustomModelData() && item.getItemMeta().getCustomModelData() == 11
         : false;
   }

   private void stunShield(Player player) {
      player.setCooldown(Material.SHIELD, Math.max(1, Settings.intValue(KEY, "shield_disable", "seconds", 10)) * 20);
   }
}
