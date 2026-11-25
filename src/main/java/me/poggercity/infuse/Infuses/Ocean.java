package me.poggercity.infuse.Infuses;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import me.poggercity.infuse.Managers.AttributeBonus;
import me.poggercity.infuse.Managers.CooldownManager;
import me.poggercity.infuse.Managers.Infuse;
import me.poggercity.infuse.Managers.Settings;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public class Ocean implements Listener {
   public static final String KEY = "ocean";
   private static final String GEM_NAME = ChatColor.BLUE + "Ocean Effect";
   private final Plugin plugin;
   private final Map<UUID, Integer> customAirMap = new HashMap<>();
   private final Map<UUID, Long> lastDrownDamageTime = new HashMap<>();
   private final Map<UUID, ScheduledTask> airRegenTasks = new HashMap<>();
   private final Map<UUID, ScheduledTask> upgradeTasks = new HashMap<>();
   private final Map<UUID, Long> lastUseTime = new HashMap<>();
   private final NamespacedKey swimKey;
   // Hits on players since the last Mining Fatigue, per Ocean player.
   private final Map<UUID, Integer> fatigueHits = new HashMap<>();

   public Ocean(Plugin plugin) {
      this.plugin = plugin;
      this.swimKey = new NamespacedKey(plugin, "ocean_swim");
   }

   private static boolean worldDisabled(Player player) {
      return Infuse.getInstance()
         .getConfig()
         .getStringList("general.disabled_worlds")
         .stream()
         .anyMatch(w -> w.equalsIgnoreCase(player.getWorld().getName()));
   }

   /** Runs every tick: "swim faster" (same as Depth Strider III) while Ocean is equipped. */
   public void tickAttributes() {
      boolean swimEnabled = Settings.passive(KEY, "swim_faster");

      for (Player p : Bukkit.getOnlinePlayers()) {
         boolean ocean = !p.isDead() && this.hasOceanEquipped(p) && !worldDisabled(p);
         AttributeBonus.set(p, Attribute.WATER_MOVEMENT_EFFICIENCY, this.swimKey, ocean && swimEnabled ? 1.0 : 0.0, Operation.ADD_NUMBER);
      }
   }

   public void removeBonuses(Player p) {
      AttributeBonus.remove(p, Attribute.WATER_MOVEMENT_EFFICIENCY, this.swimKey);
   }

   // Every few hits on a player (mining_fatigue_on_hit: hits) that player gets Mining Fatigue. Counted per attacker,
   // over all the players they hit; trusted players are never affected and don't count.
   @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
   public void onHitPlayer(EntityDamageByEntityEvent event) {
      if (event.getDamager() instanceof Player attacker
         && event.getEntity() instanceof Player target
         && !attacker.equals(target)
         && Settings.passive(KEY, "mining_fatigue_on_hit")
         && this.hasOceanEquipped(attacker)
         && !worldDisabled(attacker)
         && !Infuse.getInstance().getTrustManager().isTrusted(attacker, target)) {
         UUID id = attacker.getUniqueId();
         int hits = this.fatigueHits.getOrDefault(id, 0) + 1;
         if (hits >= Math.max(1, Settings.intValue(KEY, "mining_fatigue_on_hit", "hits", 10))) {
            int level = Math.max(1, Math.min(255, Settings.intValue(KEY, "mining_fatigue_on_hit", "level", 2)));
            int ticks = (int)Math.round(Math.max(0.05, Settings.value(KEY, "mining_fatigue_on_hit", "seconds", 3.0)) * 20.0);
            target.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, ticks, level - 1));
            target.getWorld().playSound(target.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.4F, 1.4F);
            int bubbles = Settings.particles(KEY, 15);
            if (bubbles > 0) {
               target.getWorld().spawnParticle(Particle.BUBBLE_POP, target.getLocation().add(0.0, 1.0, 0.0), bubbles, 0.3, 0.5, 0.3, 0.05);
            }

            hits = 0;
         }

         this.fatigueHits.put(id, hits);
      }
   }

   @EventHandler(priority = EventPriority.MONITOR)
   public void onQuit(PlayerQuitEvent event) {
      this.removeBonuses(event.getPlayer());
      UUID id = event.getPlayer().getUniqueId();
      this.customAirMap.remove(id);
      this.lastDrownDamageTime.remove(id);
      this.fatigueHits.remove(id);
   }

   @EventHandler(priority = EventPriority.MONITOR)
   public void onDeath(PlayerDeathEvent event) {
      this.removeBonuses(event.getPlayer());
   }

   // Infuse 2.4 raised the player's *base* water movement efficiency to 1.0 and only put it back on a later move, so a
   // player who unequipped Ocean and logged out kept Depth Strider swimming forever. Swimming is now a removable
   // modifier; put any base value left behind by the old version back to the default.
   @EventHandler
   public void onJoin(PlayerJoinEvent event) {
      AttributeInstance water = event.getPlayer().getAttribute(Attribute.WATER_MOVEMENT_EFFICIENCY);
      if (water != null && water.getBaseValue() != water.getDefaultValue()) {
         water.setBaseValue(water.getDefaultValue());
      }
   }

   public void applyOceanEffects() {
      Set<UUID> affectedPlayers = new HashSet<>();
      boolean conduit = Settings.passive(KEY, "conduit_power");
      boolean aura = Settings.passive(KEY, "drowning_aura");
      double drownRadius = Settings.value(KEY, "drowning_aura", "radius", 5.0);
      double damage = Settings.value(KEY, "drowning_aura", "damage", 4.0);
      List<String> disabledWorlds = Infuse.getInstance().getConfig().getStringList("general.disabled_worlds").stream().map(String::toLowerCase).toList();

      // Runs directly on the server thread. It used to be deferred to the next tick while affectedPlayers was read
      // right away, which restarted and cancelled the air-regeneration task every tick.
      for (Player p : Bukkit.getOnlinePlayers()) {
         String worldName = p.getWorld().getName().toLowerCase();
         if (!disabledWorlds.contains(worldName) && this.hasOceanEquipped(p)) {
            if (conduit) {
               p.addPotionEffect(new PotionEffect(PotionEffectType.CONDUIT_POWER, 40, 0, false, false, false));
            }

            if (aura) {
               for (Entity e : p.getNearbyEntities(drownRadius, drownRadius, drownRadius)) {
                  if (e instanceof Player target
                     && !target.equals(p)
                     && !target.isDead()
                     && !disabledWorlds.contains(target.getWorld().getName().toLowerCase())
                     && !Infuse.getInstance().getTrustManager().isTrusted(p, target)) {
                     affectedPlayers.add(target.getUniqueId());
                     this.upgrade(target);
                     UUID id = target.getUniqueId();
                     int currentAir = this.customAirMap.getOrDefault(id, target.getRemainingAir());
                     boolean submerged = target.getLocation().getBlock().getType() == Material.WATER
                        && target.getEyeLocation().getBlock().getType() == Material.WATER;
                     int oxygenDepletion = submerged ? 8 : 1;
                     int newAir = Math.max(currentAir - oxygenDepletion, -10);
                     this.customAirMap.put(id, newAir);
                     target.setRemainingAir(Math.max(newAir, -20));
                     if (newAir <= 0) {
                        long now = System.currentTimeMillis();
                        long last = this.lastDrownDamageTime.getOrDefault(id, 0L);
                        if (now - last >= 1000L) {
                           target.damage(damage);
                           target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT_DROWN, 1.0F, 1.0F);
                           this.lastDrownDamageTime.put(id, now);
                        }
                     }

                     this.cancelAirRegenTask(target);
                  }
               }
            }
         }
      }

      for (Player p : Bukkit.getOnlinePlayers()) {
         UUID id = p.getUniqueId();
         if (!affectedPlayers.contains(id) && this.customAirMap.containsKey(id) && !this.airRegenTasks.containsKey(id)) {
            this.startGradualAirRegen(p);
         }
      }
   }

   private void startGradualAirRegen(Player player) {
      UUID id = player.getUniqueId();
      this.airRegenTasks.put(id, Bukkit.getRegionScheduler().runAtFixedRate(Infuse.getInstance(), player.getLocation(), task -> {
         if (!player.isOnline()) {
            this.cleanupAirRegen(id, task);
         } else {
            int currentAir = player.getRemainingAir();
            int maxAir = player.getMaximumAir();
            if (currentAir >= maxAir) {
               this.cleanupAirRegen(id, task);
            } else {
               int newAir = Math.min(currentAir + 1, maxAir);
               player.setRemainingAir(newAir);
               this.customAirMap.put(id, newAir);
            }
         }
      }, 1L, 10L));
   }

   private void cleanupAirRegen(UUID id, ScheduledTask task) {
      this.customAirMap.remove(id);
      this.lastDrownDamageTime.remove(id);
      this.airRegenTasks.remove(id);
      task.cancel();
   }

   private void cancelAirRegenTask(Player player) {
      UUID id = player.getUniqueId();
      ScheduledTask t = this.airRegenTasks.remove(id);
      if (t != null) {
         t.cancel();
      }
   }

   public void upgrade(Player player) {
      UUID playerId = player.getUniqueId();
      this.lastUseTime.put(playerId, System.currentTimeMillis());
      if (!this.upgradeTasks.containsKey(playerId)) {
         this.upgradeTasks.put(playerId, Bukkit.getRegionScheduler().runAtFixedRate(Infuse.getInstance(), player.getLocation(), task -> {
            if (!player.isOnline()) {
               this.cleanupUpgrade(playerId, task);
            } else {
               long lastUsed = this.lastUseTime.getOrDefault(playerId, 0L);
               if (System.currentTimeMillis() - lastUsed > 1000L) {
                  this.cleanupUpgrade(playerId, task);
               } else {
                  long ticks = player.getWorld().getFullTime() % 80L;
                  if (ticks < 60L) {
                     this.showUpgradeParticles(player);
                  }
               }
            }
         }, 1L, 5L));
      }
   }

   private void cleanupUpgrade(UUID playerId, ScheduledTask task) {
      task.cancel();
      this.upgradeTasks.remove(playerId);
      this.lastUseTime.remove(playerId);
   }

   private void showUpgradeParticles(Player player) {
      Location loc = player.getLocation();
      World world = player.getWorld();
      double r = 0.5;
      int points = 5;

      for (int i = 0; i < points; i++) {
         double angle = 6.283185307179586 * ((double)i / points);
         double x = loc.getX() + r * Math.cos(angle);
         double z = loc.getZ() + r * Math.sin(angle);

         for (double yOffset = 0.3; yOffset <= 1.0; yOffset += 0.5) {
            int count = Settings.particlesAt(KEY);
            if (count > 0) {
               Location pLoc = new Location(world, x, loc.getY() + yOffset, z);
               world.spawnParticle(Particle.OMINOUS_SPAWNING, pLoc, count, 0.1, 0.5, 0.1, 0.0);
            }
         }
      }
   }

   public void stopUpgradeEffect(Player player) {
      UUID id = player.getUniqueId();
      ScheduledTask t = this.upgradeTasks.remove(id);
      if (t != null) {
         t.cancel();
      }
   }

   public static boolean isInventoryGlitchGem(ItemStack item) {
      return item != null
         && item.getType() == Material.POTION
         && item.hasItemMeta()
         && item.getItemMeta().hasCustomModelData()
         && item.getItemMeta().getCustomModelData() == 8;
   }

   public boolean hasOceanEquipped(Player player) {
      String hack1 = Infuse.getInstance().getHackManager().getHack(player.getUniqueId(), "1");
      String hack2 = Infuse.getInstance().getHackManager().getHack(player.getUniqueId(), "2");
      return hack1 != null && (hack1.equalsIgnoreCase(ChatColor.stripColor(GEM_NAME)) || hack1.equals("Augmented Ocean Effect"))
         || hack2 != null && (hack2.equalsIgnoreCase(ChatColor.stripColor(GEM_NAME)) || hack2.equals("Augmented Ocean Effect"))
         || WinnersCurse.borrowsPassive(player, KEY);
   }

   @EventHandler
   public void onProjectileLaunch(ProjectileLaunchEvent event) {
      if (event.getEntity() instanceof Trident && event.getEntity().getShooter() instanceof Player) {
         Player shooter = (Player)event.getEntity().getShooter();
         if (this.hasOceanEquipped(shooter) && Settings.passive(KEY, "trident_pull")) {
            shooter.getWorld().playSound(shooter.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_3, 1.0F, 1.0F);
         }
      }
   }

   @EventHandler
   public void onProjectileHit(ProjectileHitEvent event) {
      if (event.getEntity().getShooter() instanceof Player shooter
         && this.hasOceanEquipped(shooter)
         && Settings.passive(KEY, "trident_pull")
         && event.getEntity() instanceof Trident
         && event.getHitEntity() instanceof LivingEntity target) {
         Bukkit.getRegionScheduler().runDelayed(Infuse.getInstance(), target.getLocation(), task -> {
            if (target.isValid() && !target.isDead() && shooter.isOnline() && shooter.getWorld().equals(target.getWorld())) {
               Location shooterLoc = shooter.getLocation();
               Location targetLoc = target.getLocation();
               Vector direction = shooterLoc.toVector().subtract(targetLoc.toVector());
               if (direction.lengthSquared() == 0.0) {
                  direction = shooter.getLocation().getDirection();
               }

               direction.normalize();
               direction.setY(direction.getY() + 0.2);
               double pull = Settings.value(KEY, "trident_pull", "strength", 1.5);
               target.setVelocity(direction.multiply(pull));
               Bukkit.getRegionScheduler().runAtFixedRate(Infuse.getInstance(), target.getLocation(), new Consumer<ScheduledTask>() {
                  private int ticks = 0;

                  public void accept(ScheduledTask t) {
                     if (target.isValid() && !target.isDead() && shooter.isOnline() && shooter.getWorld().equals(target.getWorld())) {
                        if (target.getLocation().distance(shooter.getLocation()) <= 1.0) {
                           target.setVelocity(new Vector(0, 0, 0));
                           t.cancel();
                        } else if (++this.ticks >= 10) {
                           t.cancel();
                        }
                     } else {
                        t.cancel();
                     }
                  }
               }, 1L, 1L);
            }
         }, 1L);
      }
   }

   public void activateSpark(Player caster) {
      UUID playerUUID = caster.getUniqueId();
      if (!CooldownManager.isOnCooldown(playerUUID, "ocean")) {
         particles.spawnEffect(KEY, caster, Color.fromRGB(0, 90, 252));
         caster.playSound(caster.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1.0F, 2.0F);
         double wallRadius = Infuse.getInstance().getConfig().getDouble("effects.ocean.spark.radius", 15.0);
         int duration = Infuse.getInstance().getConfig().getInt("effects.ocean.spark.duration", 5);
         long durationTicks = duration * 20L;
         World world = caster.getWorld();
         Location anchor = caster.getLocation();
         long[] ticksElapsed = new long[]{0L};
         double[] spinAngle = new double[]{0.0};
         Bukkit.getRegionScheduler().runAtFixedRate(this.plugin, anchor, task -> {
            if (ticksElapsed[0] >= durationTicks || !caster.isOnline() || !caster.getWorld().equals(world)) {
               task.cancel();
            } else {
               Location casterLoc = caster.getLocation();

               for (int angle = 0; angle < 360; angle += 10) {
                  double rad = Math.toRadians(angle + spinAngle[0]);
                  double distance = casterLoc.getX() + wallRadius * Math.cos(rad);
                  double pz = casterLoc.getZ() + wallRadius * Math.sin(rad);

                  for (int yOffset = 0; yOffset <= 2; yOffset++) {
                     int count = Settings.particlesAt(KEY);
                     if (count > 0) {
                        Location wallParticleLoc = new Location(world, distance, casterLoc.getY() + yOffset, pz);
                        world.spawnParticle(Particle.NAUTILUS, wallParticleLoc, count, 0.0, 0.0, 0.0, 0.0);
                     }
                  }
               }

               for (double t = 1.0; t >= 0.0; t -= 0.04) {
                  int count = Settings.particlesAt(KEY);
                  if (count > 0) {
                     double spiralRadius = wallRadius * t;
                     double anglex = Math.toRadians(spinAngle[0] + (1.0 - t) * 720.0);
                     double x = casterLoc.getX() + spiralRadius * Math.cos(anglex);
                     double z = casterLoc.getZ() + spiralRadius * Math.sin(anglex);
                     double y = casterLoc.getY() + Math.sin(t * 3.141592653589793 / 2.0) * 2.0;
                     Location spiralLoc = new Location(world, x, y, z);
                     world.spawnParticle(Particle.NAUTILUS, spiralLoc, count, 0.0, 0.0, 0.0, 0.0);
                  }
               }

               spinAngle[0] += 10.0;
               if (ticksElapsed[0] % 20L == 0L) {
                  world.playSound(casterLoc, Sound.BLOCK_CONDUIT_AMBIENT_SHORT, 2.0F, 1.0F);
                  world.playSound(casterLoc, Sound.BLOCK_CONDUIT_AMBIENT_SHORT, 2.0F, 1.0F);
               }

               for (Entity entity : caster.getNearbyEntities(wallRadius, wallRadius, wallRadius)) {
                  if (!entity.equals(caster)) {
                     boolean shouldPull = false;
                     if (entity instanceof LivingEntity) {
                        if (entity instanceof Player other && Infuse.getInstance().getTrustManager().isTrusted(caster, other)) {
                           continue;
                        }

                        shouldPull = true;
                     }

                     if (entity instanceof Item) {
                        shouldPull = true;
                     }

                     if (shouldPull) {
                        double distance = entity.getLocation().distance(casterLoc);
                        if (distance > 2.0) {
                           Vector pull = casterLoc.toVector().subtract(entity.getLocation().toVector()).normalize();
                           if (distance > 4.0) {
                              entity.setVelocity(pull.multiply(0.5));
                           } else {
                              entity.setVelocity(pull.multiply(0.1));
                           }
                        }
                     }
                  }
               }

               ticksElapsed[0]++;
            }
         }, 1L, 1L);
         CooldownManager.setDuration(playerUUID, "ocean", duration);
         int configcooldown = Infuse.getInstance().getConfig().getInt("effects.ocean.spark.cooldown", 90);
         boolean isAugmented = Infuse.getInstance().getHackManager().getHack(playerUUID, "1") != null
               && ChatColor.stripColor(Infuse.getInstance().getHackManager().getHack(playerUUID, "1")).toLowerCase().contains("augmented ocean")
            || Infuse.getInstance().getHackManager().getHack(playerUUID, "2") != null
               && ChatColor.stripColor(Infuse.getInstance().getHackManager().getHack(playerUUID, "2")).toLowerCase().contains("augmented ocean");
         int cooldown = isAugmented ? configcooldown / 2 + duration : configcooldown + duration;
         CooldownManager.setCooldown(playerUUID, "ocean", cooldown);
      }
   }
}
