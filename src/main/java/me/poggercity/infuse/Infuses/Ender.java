package me.poggercity.infuse.Infuses;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import me.poggercity.infuse.Managers.CooldownManager;
import me.poggercity.infuse.Managers.Infuse;
import me.poggercity.infuse.Managers.Settings;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.DragonFireball;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public class Ender implements Listener {
   public static final String KEY = "ender";

   private static boolean isSelector(ItemStack item) {
      ItemMeta meta = item.getItemMeta();
      return meta != null && meta.hasDisplayName() && ChatColor.stripColor(meta.getDisplayName()).equalsIgnoreCase("Choose your Effect");
   }
   private static final String HEALTH_KEY = "ender";
   private final Map<UUID, Long> cursedPlayers = new HashMap<>();
   private final Set<UUID> currentlySharing = new HashSet<>();
   private final Map<UUID, BossBar> activeBars = new HashMap<>();

   public static boolean isStrengthGem(ItemStack item) {
      if (item != null && item.getType() == Material.POTION) {
         ItemMeta meta = item.getItemMeta();
         return meta != null && meta.hasCustomModelData() && meta.getCustomModelData() == 1000;
      } else {
         return false;
      }
   }

   public void startTrackingTask() {
      List<String> disabledWorlds = Infuse.getInstance().getConfig().getStringList("configs.disabled_worlds").stream().map(String::toLowerCase).toList();
      if (!Settings.passive(KEY, "glowing")) {
         return;
      }

      double radius = Settings.value(KEY, "glowing", "radius", 50.0);

      for (Player player : Bukkit.getOnlinePlayers()) {
         if (!this.hasImmortalHackEquipped(player)) {
            continue;
         }

         Location location = player.getLocation();
         Bukkit.getRegionScheduler()
            .run(
               Infuse.getInstance(),
               location,
               scheduler -> {
                  String worldName = player.getWorld().getName().toLowerCase();
                  if (!disabledWorlds.contains(worldName) && this.hasImmortalHackEquipped(player)) {
                     for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
                        if (entity instanceof Player target
                           && !disabledWorlds.contains(target.getWorld().getName().toLowerCase())
                           && !Infuse.getInstance().getTrustManager().isTrusted(player, target)) {
                           target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 20, 0, false, false, false));
                        }
                     }
                  }
               }
            );
      }
   }

   @EventHandler
   public void onRightClick(PlayerInteractEvent event) {
      Player player = event.getPlayer();
      ItemStack item = event.getItem();
      // The effect selector is also a dragon's breath item; it must never be shot (that used it up).
      if (this.hasImmortalHackEquipped(player)
         && Settings.passive(KEY, "dragon_fireball")
         && item != null
         && item.getType() == Material.DRAGON_BREATH
         && !isSelector(item)
         && (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)
         && !player.hasCooldown(Material.DRAGON_BREATH)) {
         int configcooldown = Settings.intValue(KEY, "dragon_fireball", "cooldown_seconds", 30);
         player.setCooldown(Material.DRAGON_BREATH, configcooldown * 20);
         item.setAmount(item.getAmount() - 1);
         Location loc = player.getEyeLocation().add(player.getLocation().getDirection().multiply(1));
         DragonFireball fireball = (DragonFireball)player.getWorld().spawnEntity(loc, EntityType.DRAGON_FIREBALL);
         fireball.setDirection(player.getLocation().getDirection().normalize());
         fireball.setShooter(player);
         player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_SHOOT, 1.0F, 1.0F);
      }
   }

   @EventHandler
   public void onDragonFireballHit(ProjectileHitEvent event) {
      if (event.getEntity() instanceof DragonFireball fireball
         && fireball.getShooter() instanceof Player shooter
         && event.getHitEntity() instanceof Player target
         && !target.equals(shooter)
         && !Infuse.getInstance().getTrustManager().isTrusted(shooter, target)) {
         target.damage(4.0, shooter);
         if (Settings.passive(KEY, "curse")) {
            this.applyCursed(target);
         }
      }
   }

   @EventHandler
   public void onAreaEffectCloudSpawn(EntitySpawnEvent event) {
      if (event.getEntity() instanceof AreaEffectCloud cloud && cloud.getParticle() == Particle.DRAGON_BREATH) {
         ProjectileSource source = cloud.getSource();
         // Only clouds from an Ender player's fireball curse. Clouds without a player owner (the Ender Dragon's own
         // breath during the dragon fight) used to curse every player standing in them.
         if (!(source instanceof Player owner) || !this.hasImmortalHackEquipped(owner) || !Settings.passive(KEY, "curse")) {
            return;
         }

         Bukkit.getRegionScheduler()
            .runAtFixedRate(
               Infuse.getInstance(),
               cloud.getLocation(),
               task -> {
                  if (cloud.isValid() && !cloud.isDead()) {
                     for (Entity nearbyEntity : cloud.getNearbyEntities(cloud.getRadius(), 0.75, cloud.getRadius())) {
                        if (nearbyEntity instanceof Player player
                           && !player.equals(owner)
                           && !Infuse.getInstance().getTrustManager().isTrusted(owner, player)
                           && !this.cursedPlayers.containsKey(player.getUniqueId())) {
                           this.applyCursed(player);
                        }
                     }
                  } else {
                     task.cancel();
                  }
               },
               1L,
               5L
            );
      }
   }

   @EventHandler
   public void onCloudDamage(EntityDamageByEntityEvent event) {
      if (event.getEntity() instanceof Player player
         && event.getDamager() instanceof AreaEffectCloud cloud
         && cloud.getSource() instanceof Player owner
         && player.equals(owner)
         && this.hasImmortalHackEquipped(player)) {
         event.setCancelled(true);
      }
   }

   @EventHandler
   public void onCursedDamage(EntityDamageEvent event) {
      if (event.getEntity() instanceof Player damaged) {
         UUID damagedUUID = damaged.getUniqueId();
         if (this.cursedPlayers.containsKey(damagedUUID) && !this.currentlySharing.contains(damagedUUID)) {
            long endTime = this.cursedPlayers.get(damagedUUID);
            if (System.currentTimeMillis() > endTime) {
               this.removeCurse(damagedUUID);
            } else {
               double damage = event.getDamage();
               this.currentlySharing.add(damagedUUID);

               for (UUID uuid : this.cursedPlayers.keySet()) {
                  if (!uuid.equals(damagedUUID)) {
                     Player target = Bukkit.getPlayer(uuid);
                     if (target != null && target.isOnline()) {
                        target.damage(damage, damaged);
                     }
                  }
               }

               Bukkit.getScheduler().runTaskLater(Infuse.getInstance(), () -> this.currentlySharing.remove(damagedUUID), 1L);
            }
         }
      }
   }

   private void applyCursed(Player player) {
      UUID uuid = player.getUniqueId();
      double time = Math.max(0.5, Settings.value(KEY, "curse", "seconds", 7.0));
      long expireTime = System.currentTimeMillis() + (long)(time * 1000.0);
      this.cursedPlayers.put(uuid, expireTime);
      // Being cursed again used to add a second boss bar and lose track of the first, which then never went away.
      BossBar previous = this.activeBars.remove(uuid);
      if (previous != null) {
         previous.removeAll();
      }

      BossBar bar = Bukkit.createBossBar(ChatColor.LIGHT_PURPLE + "Cursed", BarColor.PURPLE, BarStyle.SOLID, new BarFlag[0]);
      bar.addPlayer(player);
      bar.setProgress(1.0);
      this.activeBars.put(uuid, bar);
      Bukkit.getGlobalRegionScheduler().runAtFixedRate(Infuse.getInstance(), task -> {
         if (!this.cursedPlayers.containsKey(uuid)) {
            task.cancel();
         } else {
            long remaining = this.cursedPlayers.get(uuid) - System.currentTimeMillis();
            if (remaining <= 0L) {
               this.removeCurse(uuid);
               task.cancel();
            } else {
               bar.setProgress(Math.max(0.0, remaining / (time * 1000.0)));
            }
         }
      }, 1L, 1L);
   }

   private void removeCurse(UUID uuid) {
      this.cursedPlayers.remove(uuid);
      BossBar bar = this.activeBars.remove(uuid);
      if (bar != null) {
         bar.removeAll();
      }
   }

   public void activateSpark(Player player) {
      UUID playerUUID = player.getUniqueId();
      if (!CooldownManager.isOnCooldown(playerUUID, "ender")) {
         World world = player.getWorld();
         Location eyeLocation = player.getEyeLocation();
         Vector direction = eyeLocation.getDirection().normalize();
         double maxDistance = 15.0;
         Location targetLocation = null;
         RayTraceResult entityHit = world.rayTraceEntities(eyeLocation, direction, maxDistance, e -> e instanceof LivingEntity && !e.equals(player));
         if (entityHit != null && entityHit.getHitEntity() != null) {
            Location hitLoc = entityHit.getHitEntity().getLocation().clone();
            hitLoc.setY(hitLoc.getY() + 1.0);
            targetLocation = hitLoc;
         }

         if (targetLocation == null) {
            RayTraceResult blockHit = world.rayTraceBlocks(eyeLocation, direction, maxDistance, FluidCollisionMode.NEVER, true);
            if (blockHit != null && blockHit.getHitPosition() != null) {
               targetLocation = blockHit.getHitPosition().toLocation(world);
               targetLocation.setY(targetLocation.getY() + 1.0);
            } else {
               targetLocation = eyeLocation.clone().add(direction.multiply(maxDistance));
            }
         }

         Location finalTarget = targetLocation.clone();
         finalTarget.setDirection(player.getLocation().getDirection());
         Vector originalVelocity = player.getVelocity();
         player.teleportAsync(finalTarget);
         player.setVelocity(originalVelocity);
         int portal = Settings.particles(KEY, 40);
         if (portal > 0) {
            player.getWorld().spawnParticle(Particle.REVERSE_PORTAL, finalTarget, portal, 0.5, 1.0, 0.5, 0.5);
         }

         CooldownManager.setDuration(playerUUID, "ender", 1L);
         int configcooldown = Infuse.getInstance().getConfig().getInt("cooldowns.ender_spark", 10);
         CooldownManager.setCooldown(playerUUID, "ender", configcooldown + 2L);
         player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_TELEPORT, 1.0F, 1.0F);

         for (Entity target : finalTarget.getWorld().getNearbyEntities(finalTarget, 2.0, 2.0, 2.0)) {
            if (!target.equals(player)) {
               if (target instanceof Player targetPlayer) {
                  if (!Infuse.getInstance().getTrustManager().isTrusted(player, targetPlayer)) {
                     UUID targetUUID = targetPlayer.getUniqueId();
                     Map<String, Long> savedCooldowns = new HashMap<>();

                     for (String key : new HashSet<>(CooldownManager.displayNames.values())) {
                        if (CooldownManager.isOnCooldown(targetUUID, key)) {
                           long duration = CooldownManager.getCachedDuration(targetUUID, key);
                           if (duration > 0L) {
                              savedCooldowns.put(key, duration);
                           }
                        }
                     }

                     CooldownManager.resetAllCooldowns(targetUUID);
                     this.applyCursed(targetPlayer);
                     double damage = Infuse.getInstance().getConfig().getDouble("damage.dragon", 4.0);
                     targetPlayer.damage(damage);

                     for (Entry<String, Long> entry : savedCooldowns.entrySet()) {
                        CooldownManager.setCooldown(targetUUID, entry.getKey(), entry.getValue());
                     }
                  }
               } else if (target instanceof LivingEntity living && !(living instanceof EnderDragon)) {
                  living.damage(living.getHealth());
               }
            }
         }
      }
   }

   private boolean hasImmortalHackEquipped(Player player) {
      String hack1 = Infuse.getInstance().getHackManager().getHack(player.getUniqueId(), "1");
      String hack2 = Infuse.getInstance().getHackManager().getHack(player.getUniqueId(), "2");
      return hack1 != null && hack1.equalsIgnoreCase(ChatColor.stripColor("Ender Effect"))
         || hack2 != null && hack2.equalsIgnoreCase(ChatColor.stripColor("Ender Effect"));
   }
}
