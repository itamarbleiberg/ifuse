package me.poggercity.infuse.Infuses;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import me.poggercity.infuse.Managers.CooldownManager;
import me.poggercity.infuse.Managers.Infuse;
import me.poggercity.infuse.Managers.PlayerHackManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public class Fire implements Listener {
   private static final String GEM_NAME = ChatColor.GOLD + "Fire Effect";
   private static final String SPARK_COOLDOWN_KEY = "fire_spark";
   private final PlayerHackManager hackManager = new PlayerHackManager();
   private final Map<UUID, Integer> hitCounter = new HashMap<>();
   private final Set<UUID> lavaSwimmers = new HashSet<>();
   private final Map<UUID, Double> damageMultipliers = new HashMap<>();
   private final Map<UUID, ScheduledTask> decayTasks = new HashMap<>();
   private final Map<UUID, Long> lavaClutchTimes = new HashMap<>();
   int duration = sparkDuration();
   double radius = sparkRadius();

   private static int sparkDuration() {
      return Infuse.getInstance().getConfig().getInt("durations.fire_spark", 4);
   }

   private static double sparkRadius() {
      return Infuse.getInstance().getConfig().getDouble("radius.fire", 5.0);
   }

   private static boolean lavaFlyEnabled() {
      return Infuse.getInstance().getConfig().getBoolean("configs.fire_lava_fly", true);
   }

   public Fire(Infuse plugin) {
   }

   public void applyFireEffects() {
      for (Player p : Bukkit.getOnlinePlayers()) {
         if (this.hasImmortalHackEquipped(p, "1") || this.hasImmortalHackEquipped(p, "2")) {
            this.applyFireResistance(p);
         } else if (this.lavaSwimmers.contains(p.getUniqueId())) {
            // BUG FIX (infinite flight): lava flight used to be revoked only for players who still had Fire equipped.
            // Unequipping Fire while flying in lava (/ldrain, /rdrain, replacing it with another effect, /cleareffect,
            // losing it on death) skipped that cleanup, so the player kept flight forever.
            this.handleLavaSwim(p, false);
         }
      }
   }

   // Never leave a player with flight granted by Fire after they log out.
   @EventHandler
   public void onPlayerQuit(PlayerQuitEvent event) {
      this.handleLavaSwim(event.getPlayer(), false);
   }

   @EventHandler
   public void onPlayerBucketEmpty(PlayerBucketEmptyEvent event) {
      Player player = event.getPlayer();
      if ((this.hasImmortalHackEquipped(player, "1") || this.hasImmortalHackEquipped(player, "2")) && event.getBucket() == Material.LAVA_BUCKET) {
         this.lavaClutchTimes.put(player.getUniqueId(), System.currentTimeMillis());
      }
   }

   @EventHandler
   public void onPlayerFireDamage(EntityDamageEvent event) {
      if (event.getEntity() instanceof Player victim && (event.getCause() == DamageCause.FIRE || event.getCause() == DamageCause.FIRE_TICK)) {
         Player fireCauser = this.getFireCauser(victim);
         if (fireCauser != null && (this.hasImmortalHackEquipped(fireCauser, "1") || this.hasImmortalHackEquipped(fireCauser, "2"))) {
            double increaseFactor = Infuse.getInstance().getConfig().getDouble("damage.increasing_fire_damage_multiplier", 1.3);
            double currentMultiplier = this.damageMultipliers.getOrDefault(victim.getUniqueId(), 1.0);
            double maxMultiplier = Infuse.getInstance().getConfig().getDouble("damage.max_fire_damage_multiplier", 2.0);
            double newMultiplier = Math.min(currentMultiplier * increaseFactor, maxMultiplier);
            this.damageMultipliers.put(victim.getUniqueId(), newMultiplier);
            event.setDamage(event.getDamage() * newMultiplier);
            this.startDecayTask(victim);
         }
      }
   }

   private void startDecayTask(Player victim) {
      UUID victimId = victim.getUniqueId();
      ScheduledTask oldTask = this.decayTasks.remove(victimId);
      if (oldTask != null) {
         oldTask.cancel();
      }

      this.decayTasks.put(victimId, Bukkit.getRegionScheduler().runAtFixedRate(Infuse.getInstance(), victim.getLocation(), task -> {
         double currentMultiplier = this.damageMultipliers.getOrDefault(victimId, 1.0);
         double decreaseFactor = Infuse.getInstance().getConfig().getDouble("damage.increasing_fire_damage_multiplier", 1.3);
         currentMultiplier /= decreaseFactor;
         if (currentMultiplier <= 1.0) {
            this.damageMultipliers.remove(victimId);
            this.decayTasks.remove(victimId);
            task.cancel();
         } else {
            this.damageMultipliers.put(victimId, currentMultiplier);
         }
      }, 10L, 10L));
   }

   private Player getFireCauser(Player victim) {
      Player nearest = null;
      double nearestDistance = 1.7976931348623157E308;

      for (Entity entity : victim.getNearbyEntities(10.0, 10.0, 10.0)) {
         if (entity instanceof Player p) {
            double distance = victim.getLocation().distance(p.getLocation());
            if (!(distance > 10.0) && (this.hasImmortalHackEquipped(p, "1") || this.hasImmortalHackEquipped(p, "2")) && distance < nearestDistance) {
               nearest = p;
               nearestDistance = distance;
            }
         }
      }

      return nearest;
   }

   private boolean worldDisabled(Player player) {
      return Infuse.getInstance()
         .getConfig()
         .getStringList("configs.disabled_worlds")
         .stream()
         .map(String::toLowerCase)
         .anyMatch(w -> w.equalsIgnoreCase(player.getWorld().getName()));
   }

   private void applyFireResistance(Player player) {
      if (this.worldDisabled(player)) {
         this.handleLavaSwim(player, false);
      } else {
         Location anchor = player.getLocation();
         Bukkit.getRegionScheduler()
            .run(
               Infuse.getInstance(),
               anchor,
               task -> {
                  Infuse.getInstance()
                     .getServer()
                     .getRegionScheduler()
                     .execute(
                        Infuse.getInstance(),
                        player.getLocation(),
                        () -> player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 40, 0, true, false))
                     );
                  this.handleLavaSwim(player, lavaFlyEnabled());
                  this.handleWater2(player);
               }
            );
      }
   }

   @EventHandler
   public void onBlockBreak(BlockBreakEvent event) {
      Player player = event.getPlayer();
      if (this.hasImmortalHackEquipped(player, "1") || this.hasImmortalHackEquipped(player, "2")) {
         ItemStack item = player.getInventory().getItemInMainHand();
         if (item.getType().name().endsWith("_PICKAXE") && !item.containsEnchantment(Enchantment.SILK_TOUCH)) {
            Material blockType = event.getBlock().getType();
            ItemStack drop = null;
            int amount = 1;
            int fortuneLevel = item.getEnchantmentLevel(Enchantment.FORTUNE);
            if (fortuneLevel > 0) {
               amount += ThreadLocalRandom.current().nextInt(1, fortuneLevel + 2);
            }

            switch (blockType) {
               case IRON_ORE:
               case DEEPSLATE_IRON_ORE:
                  drop = new ItemStack(Material.IRON_INGOT, amount);
                  break;
               case GOLD_ORE:
               case DEEPSLATE_GOLD_ORE:
                  drop = new ItemStack(Material.GOLD_INGOT, amount);
                  break;
               case COPPER_ORE:
               case DEEPSLATE_COPPER_ORE:
                  drop = new ItemStack(Material.COPPER_INGOT, amount);
                  break;
               case ANCIENT_DEBRIS:
                  drop = new ItemStack(Material.NETHERITE_SCRAP, amount);
                  break;
               default:
                  return;
            }

            event.getBlock().getWorld().spawnParticle(Particle.FLAME, event.getBlock().getLocation().add(0.5, 0.5, 0.5), 10, 0.2, 0.2, 0.2, 0.01);
            event.setDropItems(false);
            player.getWorld().dropItemNaturally(event.getBlock().getLocation(), drop);
         }
      }
   }

   private void handleLavaSwim(Player player, boolean enabled) {
      UUID uuid = player.getUniqueId();
      if (!enabled) {
         if (this.lavaSwimmers.remove(uuid)) {
            player.setFlying(false);
            player.setAllowFlight(false);
         }
      } else {
         boolean inLava = player.isInLava();
         if (inLava) {
            if (this.lavaSwimmers.add(uuid)) {
               player.setAllowFlight(true);
               player.setFlying(true);
            }
         } else if (this.lavaSwimmers.remove(uuid)) {
            player.setFlying(false);
            player.setAllowFlight(false);
         }
      }
   }

   private void handleWater2(Player player) {
      if (player.getFireTicks() > 0) {
         int radius = 3;
         if (player.hasPotionEffect(PotionEffectType.UNLUCK)) {
            PotionEffect effect = player.getPotionEffect(PotionEffectType.UNLUCK);
            if (effect != null && effect.getAmplifier() == 7) {
               radius = 7;
            }
         }

         this.evaporateWaterAt(player, player.getLocation(), radius);
      }
   }

   private void evaporateWaterAt(Player caster, Location center, int radius) {
      World world = center.getWorld();
      if (world != null) {
         double protectSq = 4.0;
         Set<Location> protectedSpots = new HashSet<>();
         protectedSpots.add(caster.getLocation());

         for (Player p : world.getPlayers()) {
            if (!p.equals(caster)
               && p.getWorld() == world
               && !(p.getLocation().distanceSquared(center) > (radius + 2) * (radius + 2))
               && Infuse.getInstance().getTrustManager().isTrusted(caster, p)) {
               protectedSpots.add(p.getLocation());
            }
         }

         boolean playedSound = false;

         for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
               for (int z = -radius; z <= radius; z++) {
                  Location blockLoc = center.clone().add(x, y, z);
                  boolean skip = false;

                  for (Location prot : protectedSpots) {
                     if (prot.getWorld() == world && prot.distanceSquared(blockLoc) <= 4.0) {
                        skip = true;
                        break;
                     }
                  }

                  if (!skip) {
                     Block block = blockLoc.getBlock();
                     Material type = block.getType();
                     BlockData data = block.getBlockData();
                     boolean removed = false;
                     if (type == Material.WATER
                        || type == Material.KELP
                        || type == Material.KELP_PLANT
                        || type == Material.SEAGRASS
                        || type == Material.TALL_SEAGRASS) {
                        block.setType(Material.AIR);
                        removed = true;
                     } else if (data instanceof Waterlogged wl && wl.isWaterlogged()) {
                        wl.setWaterlogged(false);
                        block.setBlockData(wl);
                        removed = true;
                     }

                     if (removed) {
                        if (!playedSound) {
                           world.playSound(center, Sound.BLOCK_FIRE_EXTINGUISH, 0.25F, 1.0F);
                           playedSound = true;
                        }

                        world.spawnParticle(Particle.SMOKE, blockLoc, 1, 0.2, 0.2, 0.2, 0.01);
                     }
                  }
               }
            }
         }
      }
   }

   private boolean hasImmortalHackEquipped(Player player, String tier) {
      String currentHack = Infuse.getInstance().getHackManager().getHack(player.getUniqueId(), tier);
      return currentHack != null && (currentHack.equalsIgnoreCase(ChatColor.stripColor(GEM_NAME)) || currentHack.equals("Augmented Fire Effect"));
   }

   @EventHandler
   public void onEntityShootBow(EntityShootBowEvent event) {
      if (event.getEntity() instanceof Player player
         && (this.hasImmortalHackEquipped(player, "1") || this.hasImmortalHackEquipped(player, "2"))
         && event.getForce() >= 1.0F
         && event.getProjectile() instanceof Projectile projectile) {
         projectile.setFireTicks(60);
      }
   }

   @EventHandler
   public void onEntityDamage(EntityDamageEvent event) {
      if (event.getEntity() instanceof Player player
         && event.getCause() == DamageCause.FALL
         && (this.hasImmortalHackEquipped(player, "1") || this.hasImmortalHackEquipped(player, "2"))) {
         Material blockType = player.getLocation().getBlock().getType();
         if (blockType == Material.LAVA || blockType == Material.LAVA_CAULDRON) {
            event.setCancelled(true);
            return;
         }

         Long lastClutchTime = this.lavaClutchTimes.get(player.getUniqueId());
         if (lastClutchTime != null) {
            long timeDiff = System.currentTimeMillis() - lastClutchTime;
            if (timeDiff <= 2000L) {
               event.setCancelled(true);
               player.sendMessage(ChatColor.GOLD + "Lava bucket clutch saved you from fall damage!");
               player.getWorld().spawnParticle(Particle.LAVA, player.getLocation(), 20, 0.5, 0.5, 0.5, 0.1);
               player.playSound(player.getLocation(), Sound.BLOCK_LAVA_POP, 1.0F, 1.0F);
               this.lavaClutchTimes.remove(player.getUniqueId());
            }
         }
      }
   }

   @EventHandler
   public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
      if (event.getDamager() instanceof Player player && (this.hasImmortalHackEquipped(player, "1") || this.hasImmortalHackEquipped(player, "2"))) {
         UUID uuid = player.getUniqueId();
         int count = this.hitCounter.getOrDefault(uuid, 0) + 1;
         if (count >= 10) {
            event.getEntity().setFireTicks(60);
            count = 0;
         }

         this.hitCounter.put(uuid, count);
      }
   }

   public void activateSpark(Player player) {
      UUID playerUUID = player.getUniqueId();
      if (!CooldownManager.isOnCooldown(playerUUID, "fire_spark")) {
         Location anchor = player.getLocation();
         Bukkit.getRegionScheduler()
            .run(Infuse.getInstance(), anchor, task -> player.addPotionEffect(new PotionEffect(PotionEffectType.UNLUCK, 100, 7, false, false)));
         player.getWorld().spawnParticle(Particle.FLAME, player.getLocation().add(0.0, 1.0, 0.0), 30, 0.5, 0.5, 0.5, 0.1);
         player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1.0F, 1.0F);
         Location center = player.getLocation();
         World world = center.getWorld();
         this.evaporateWaterAt(player, center, 5);

         for (int baseCooldown = 0; baseCooldown < 360; baseCooldown += 2) {
            double rad = Math.toRadians(baseCooldown);
            double offsetX = this.radius * Math.cos(rad);
            double offsetZ = this.radius * Math.sin(rad);
            Location particleLoc = center.clone().add(offsetX, 0.0, offsetZ);
            world.spawnParticle(Particle.LAVA, particleLoc, 2, 0.0, 0.5, 0.0, 0.0);
         }

         world.playSound(player.getLocation(), Sound.ENTITY_BLAZE_DEATH, 0.5F, 1.0F);

         for (Entity entity : player.getNearbyEntities(this.radius, 5.0, this.radius)) {
            if (entity instanceof LivingEntity target
               && !target.equals(player)
               && !(target instanceof Player targetPlayer && Infuse.getInstance().getTrustManager().isTrusted(player, targetPlayer))) {
               target.setFireTicks(99999999);
            }
         }

         this.spawnSparkEffect(player);
         CooldownManager.setDuration(playerUUID, "fire_spark", this.duration + 1);
         int var141 = Infuse.getInstance().getConfig().getInt("cooldowns.fire_spark", 60);
         boolean isAug = Infuse.getInstance().getHackManager().getHack(playerUUID, "1") != null
               && ChatColor.stripColor(Infuse.getInstance().getHackManager().getHack(playerUUID, "1")).toLowerCase().contains("augmented fire")
            || Infuse.getInstance().getHackManager().getHack(playerUUID, "2") != null
               && ChatColor.stripColor(Infuse.getInstance().getHackManager().getHack(playerUUID, "2")).toLowerCase().contains("augmented fire");
         int cooldown = (isAug ? var141 / 2 : var141) + this.duration;
         this.handleWater2(player);
         CooldownManager.setCooldown(playerUUID, "fire_spark", cooldown);
      }
   }

   private void spawnSparkEffect(Player caster) {
      int[] tick = new int[]{0};
      Location anchor = caster.getLocation();
      Bukkit.getRegionScheduler()
         .runAtFixedRate(
            Infuse.getInstance(),
            anchor,
            task -> {
               if (tick[0] >= this.duration * 20) {
                  task.cancel();
                  this.startDarkRedDustEffect(caster.getLocation(), caster);
               } else {
                  Location center = caster.getLocation();
                  World world = center.getWorld();
                  if (tick[0] % 5 == 0) {
                     this.evaporateWaterAt(caster, center, 5);
                  }

                  if (tick[0] > 0 && tick[0] % 20 == 0) {
                     world.playSound(center, Sound.ENTITY_BLAZE_SHOOT, 1.0F, 1.0F);

                     for (Entity entity : caster.getNearbyEntities(this.radius, 5.0, this.radius)) {
                        if (entity instanceof LivingEntity target
                           && !target.equals(caster)
                           && !(target instanceof Player p && Infuse.getInstance().getTrustManager().isTrusted(caster, p))) {
                           target.setFireTicks(99999999);
                        }
                     }

                     world.spawnParticle(Particle.TRIAL_SPAWNER_DETECTION, center, 25, 0.2, 0.0, 0.2, 0.0);

                     for (int angle = 0; angle < 360; angle += 2) {
                        double rad = Math.toRadians(angle);
                        double offsetX = this.radius * Math.cos(rad);
                        double offsetZ = this.radius * Math.sin(rad);
                        Location particleLoc = center.clone().add(offsetX, 0.0, offsetZ);
                        world.spawnParticle(Particle.LAVA, particleLoc, 2, 0.0, 0.5, 0.0, 0.0);
                     }
                  }

                  tick[0]++;
               }
            },
            1L,
            1L
         );
   }

   private void startDarkRedDustEffect(Location startLoc, Player caster) {
      World world = startLoc.getWorld();
      double explosionRadius = this.radius;
      double launch = Infuse.getInstance().getConfig().getDouble("damage.fire_launch_strength", 4.0);

      for (Player target : world.getPlayers()) {
         if (!target.equals(caster)
            && !Infuse.getInstance().getTrustManager().isTrusted(caster, target)
            && target.getLocation().distance(startLoc) <= explosionRadius) {
            target.setVelocity(new Vector(0.0, launch, 0.0));
         }
      }

      world.playSound(startLoc, Sound.ENTITY_GENERIC_EXPLODE, 2.0F, 1.0F);
      world.playSound(startLoc, Sound.ENTITY_BLAZE_DEATH, 0.5F, 1.0F);
      double fixedRadius = this.radius;
      double fixedHeight = startLoc.getY();

      for (int angle = 0; angle < 360; angle++) {
         double rad = Math.toRadians(angle);
         double offsetX = fixedRadius * Math.cos(rad);
         double offsetZ = fixedRadius * Math.sin(rad);
         Location particleLoc = new Location(world, startLoc.getX() + offsetX, fixedHeight, startLoc.getZ() + offsetZ);
         world.spawnParticle(Particle.DUST_PILLAR, particleLoc, 20, 0.0, 0.0, 0.0, launch / 4.0, Material.LAVA.createBlockData());
      }
   }

   public static boolean isStrengthGem(ItemStack item) {
      return item != null && item.getType() == Material.POTION && item.getItemMeta() != null
         ? item.getItemMeta().hasCustomModelData() && item.getItemMeta().getCustomModelData() == 3
         : false;
   }
}
