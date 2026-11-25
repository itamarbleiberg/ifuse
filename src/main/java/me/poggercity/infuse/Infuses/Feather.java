package me.poggercity.infuse.Infuses;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import me.poggercity.infuse.Managers.CooldownManager;
import me.poggercity.infuse.Managers.Infuse;
import me.poggercity.infuse.Managers.Settings;
import me.poggercity.infuse.Managers.TrustManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.WindCharge;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public class Feather implements Listener {
   public static final String KEY = "feather";
   private static final String GEM_NAME = ChatColor.WHITE + "Feather Effect";
   private static final String EMERALD_KEY = "feather";
   private final Plugin plugin;
   private final Map<UUID, Integer> hitCounter = new HashMap<>();
   private static final String DAMAGE = "effects.feather.spark.damage";
   private final Set<UUID> landingPlayers = new HashSet<>();
   private final TrustManager trustManager = new TrustManager();

   public Feather(Plugin plugin) {
      this.plugin = plugin;
   }

   private boolean hasImmortalHackEquipped(Player player, String tier) {
      String currentHack = Infuse.getInstance().getHackManager().getHack(player.getUniqueId(), tier);
      return currentHack != null && (currentHack.equals("Feather Effect") || currentHack.equals("Augmented Feather Effect"))
         || Gamble.borrowsPassive(player, KEY);
   }

   public void activateSpark(Player player) {
      UUID playerUUID = player.getUniqueId();
      if (!CooldownManager.isOnCooldown(playerUUID, "feather")) {
         int nectar = Settings.particles(KEY, 20);
         if (nectar > 0) {
            player.getWorld().spawnParticle(Particle.FALLING_NECTAR, player.getLocation().add(0.0, 1.0, 0.0), nectar, 0.5, 0.5, 0.5, 0.1);
         }

         player.playSound(player.getLocation(), Sound.ENTITY_PHANTOM_FLAP, 1.0F, 1.2F);
         Vector dashDirection = player.getEyeLocation().getDirection().normalize();
         Vector launchVector = dashDirection.multiply(0).setY(1);
         player.setVelocity(launchVector);
         CooldownManager.setDuration(playerUUID, "feather", 5L);
         int configcooldown = Infuse.getInstance().getConfig().getInt("effects.feather.spark.cooldown", 40);
         boolean isAugmented = Infuse.getInstance().getHackManager().getHack(playerUUID, "1") != null
               && ChatColor.stripColor(Infuse.getInstance().getHackManager().getHack(playerUUID, "1")).toLowerCase().contains("augmented feather")
            || Infuse.getInstance().getHackManager().getHack(playerUUID, "2") != null
               && ChatColor.stripColor(Infuse.getInstance().getHackManager().getHack(playerUUID, "2")).toLowerCase().contains("augmented feather");
         int cooldown = isAugmented ? configcooldown / 2 + 6 : configcooldown + 6;
         CooldownManager.setCooldown(playerUUID, "feather", cooldown);
         Location anchor = player.getLocation();
         Bukkit.getRegionScheduler().runDelayed(Infuse.getInstance(), anchor, task -> {
            if (player.isOnline()) {
               player.addPotionEffect(new PotionEffect(PotionEffectType.UNLUCK, 600, 27, false, false, false));
            }
         }, 10L);
      }
   }

   @EventHandler
   public void onPlayerLand(PlayerMoveEvent event) {
      Player player = event.getPlayer();
      if (player.isOnGround() && player.hasPotionEffect(PotionEffectType.UNLUCK)) {
         PotionEffect effect = player.getPotionEffect(PotionEffectType.UNLUCK);
         if (effect != null && effect.getAmplifier() == 27) {
            player.removePotionEffect(PotionEffectType.UNLUCK);
            Location loc = player.getLocation();
            World world = player.getWorld();
            double explosionRadius = Infuse.getInstance().getConfig().getDouble("effects.feather.spark.radius", 4.0);
            double dmg = Infuse.getInstance().getConfig().getDouble("effects.feather.spark.damage", 8.0);
            if (dmg <= 0.0) {
               dmg = 8.0;
            }

            for (Entity entity : player.getNearbyEntities(explosionRadius, explosionRadius, explosionRadius)) {
               if (entity instanceof LivingEntity target
                  && !target.equals(player)
                  && (!(target instanceof Player) || !Infuse.getInstance().getTrustManager().isTrusted(player, (Player)target))) {
                  target.damage(dmg, player);
                  Vector knockback = new Vector(0, 1, 0);
                  target.setVelocity(target.getVelocity().add(knockback));
                  Location anchor = target.getLocation();
                  Bukkit.getRegionScheduler()
                     .run(
                        Infuse.getInstance(),
                        anchor,
                        task -> target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 80, 0, false, false, false))
                     );
               }
            }

            int clouds = Settings.particles(KEY, 50);
            if (clouds > 0) {
               world.spawnParticle(Particle.CLOUD, loc, clouds, 0.0, 0.0, 0.0, 2.0);
            }

            world.playSound(loc, Sound.ITEM_MACE_SMASH_GROUND_HEAVY, 1.5F, 1.0F);
            // The slam throws the player again; a Feather spark from Gamble keeps them safe for that landing too.
            Gamble.extendRolledSpark(player, KEY, 15L);
            Location anchor = player.getLocation();
            Bukkit.getRegionScheduler().runDelayed(this.plugin, anchor, task -> {
               if (player.isOnline()) {
                  Vector dashDirection = player.getEyeLocation().getDirection().normalize();
                  Vector launchVector = dashDirection.multiply(5);
                  player.setVelocity(launchVector);
               }
            }, 1L);
         }
      }
   }

   @EventHandler
   public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
      if (event.getDamager() instanceof Player attacker
         && Settings.passive(KEY, "fall_attack")
         && (this.hasImmortalHackEquipped(attacker, "1") || this.hasImmortalHackEquipped(attacker, "2"))) {
         double fallDistance = attacker.getFallDistance();
         if (fallDistance >= Settings.value(KEY, "fall_attack", "min_fall", 7.0)) {
            attacker.getWorld().playSound(attacker.getLocation(), Sound.ITEM_MACE_SMASH_AIR, 1.0F, 1.0F);
            World world = attacker.getWorld();
            Location particleLoc = event.getDamager().getLocation();
            if (Settings.particles(KEY, 1) > 0) {
               world.spawnParticle(Particle.GUST_EMITTER_SMALL, particleLoc, 1, 0.0, 0.0, 0.0, 0.0);
            }

            attacker.setVelocity(new Vector(0.0, 1.8, 0.0));
            double multiplier = Settings.value(KEY, "fall_attack", "multiplier", 1.5);
            event.setDamage(event.getDamage() * multiplier);
         }
      }
   }

   @EventHandler
   public void onPlayerHit(EntityDamageEvent event) {
      if (event.getEntity() instanceof Player) {
         Player player = (Player)event.getEntity();
         if ((this.hasImmortalHackEquipped(player, "1") || this.hasImmortalHackEquipped(player, "2"))
            && event.getCause() != DamageCause.FALL
            && Settings.passive(KEY, "wind_charge_counter")) {
            UUID uuid = player.getUniqueId();
            int count = this.hitCounter.getOrDefault(uuid, 0) + 1;
            this.hitCounter.put(uuid, count);
            if (count >= Math.max(1, Settings.intValue(KEY, "wind_charge_counter", "hits", 10))) {
               this.hitCounter.put(uuid, 0);
               Location chargeLocation = player.getLocation().add(0.0, 1.0, 0.0);
               WindCharge windCharge = (WindCharge)player.getWorld().spawn(chargeLocation, WindCharge.class);
               Location targetLocation = player.getLocation().subtract(0.0, 1.0, 0.0);
               Vector direction = targetLocation.toVector().subtract(chargeLocation.toVector()).normalize();
               windCharge.setVelocity(direction.multiply(1));
               windCharge.setShooter(player);
               player.setVelocity(new Vector(0.0, 0.5, 0.0));
            }
         }
      }
   }

   @EventHandler
   public void onPlayerFallDamage(EntityDamageEvent event) {
      if (event.getEntity() instanceof Player && event.getCause() == DamageCause.FALL) {
         Player player = (Player)event.getEntity();
         if ((this.hasImmortalHackEquipped(player, "1") || this.hasImmortalHackEquipped(player, "2")) && Settings.passive(KEY, "no_fall_damage")
            // The Feather spark from Gamble's slot machine launches the player too; landing from it doesn't hurt.
            || Gamble.rolledSpark(player, KEY)) {
            event.setCancelled(true);
         }
      }
   }

   @EventHandler
   public void onPlayerRightClickWindcharge(PlayerInteractEvent event) {
      Player player = event.getPlayer();
      if ((this.hasImmortalHackEquipped(player, "1") || this.hasImmortalHackEquipped(player, "2")) && Settings.passive(KEY, "wind_charge_cooldown")) {
         ItemStack item = player.getInventory().getItemInMainHand();
         int ticks = Math.max(0, Settings.intValue(KEY, "wind_charge_cooldown", "ticks", 5));
         if (item != null
            && item.getType() == Material.WIND_CHARGE
            && !player.hasCooldown(Material.WIND_CHARGE)
            && (event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_AIR)) {
            Location anchor = player.getLocation();
            Bukkit.getRegionScheduler().runDelayed(this.plugin, anchor, task -> player.setCooldown(Material.WIND_CHARGE, ticks), 1L);
         }
      }
   }

   @EventHandler
   public void onWindChargeLaunch(ProjectileLaunchEvent event) {
      // Only for Feather users: this used to make every player's wind charges twice as fast and perfectly straight.
      if (event.getEntity() instanceof WindCharge fireball
         && fireball.getShooter() instanceof Player player
         && Settings.passive(KEY, "wind_charge_boost")
         && (this.hasImmortalHackEquipped(player, "1") || this.hasImmortalHackEquipped(player, "2"))) {
         double velocity = Math.max(0.1, Settings.value(KEY, "wind_charge_boost", "velocity", 2.0));
         Vector direction = player.getEyeLocation().getDirection().normalize().multiply(velocity);
         fireball.setVelocity(direction);
      }
   }

   public static boolean isInvincibilityGem(ItemStack item) {
      return item != null
         && item.getType() == Material.POTION
         && item.hasItemMeta()
         && item.getItemMeta().hasCustomModelData()
         && item.getItemMeta().getCustomModelData() == 2;
   }
}
