package me.poggercity.infuse.Infuses;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
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
import org.bukkit.World;
import org.bukkit.Particle.DustOptions;
import org.bukkit.World.Environment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;

public class Thunder implements Listener {
   public static final String KEY = "thunder";
   private final Set<UUID> frozenAttackers = new HashSet<>();
   private static final String GEM_NAME = ChatColor.YELLOW + "Thunder Effect";
   private static final String THUNDER = "thunder";
   private final Plugin plugin;
   private final Map<UUID, Integer> meleeHitCounter = new HashMap<>();
   private final Set<UUID> activeSparks = new HashSet<>();
   private final Map<UUID, Long> entityLightningCooldowns = new HashMap<>();
   private static final long LIGHTNING_COOLDOWN_MS = 2000L;
   private final Map<UUID, Map<UUID, Integer>> hitCounts = new HashMap<>();

   public Thunder(Plugin plugin) {
      this.plugin = plugin;
   }

   // damage.thunder_hearts existed in the config but every strike did a hardcoded 1 heart. It is now the
   // smite_on_hit "hearts" setting and is used for all strikes.
   private static double strikeHearts() {
      return Settings.value(KEY, "smite_on_hit", "hearts", 1.0);
   }

   private void applyThunderDamage(LivingEntity target, double hearts) {
      if (!(hearts <= 0.0)) {
         double damage = hearts * 2.0;
         target.damage(damage, (Entity)null);
      }
   }

   private static void strikeParticles(Location location) {
      int count = Settings.particles(KEY, 10);
      if (count > 0) {
         location.getWorld().spawnParticle(Particle.DUST, location, count, 0.5, 0.5, 0.5, 0.0, new DustOptions(Color.YELLOW, 1.5F));
      }
   }

   public static boolean isInvincibilityGem(ItemStack item) {
      return item != null && item.getType() == Material.POTION && item.getItemMeta() != null
         ? item.getItemMeta().hasCustomModelData() && item.getItemMeta().getCustomModelData() == 13
         : false;
   }

   private boolean hasImmortalHackEquipped(Player player) {
      String hack1 = Infuse.getInstance().getHackManager().getHack(player.getUniqueId(), "1");
      String hack2 = Infuse.getInstance().getHackManager().getHack(player.getUniqueId(), "2");
      return hack1 != null && (hack1.equalsIgnoreCase(ChatColor.stripColor(GEM_NAME)) || hack1.equals("Augmented Thunder Effect"))
         || hack2 != null && (hack2.equalsIgnoreCase(ChatColor.stripColor(GEM_NAME)) || hack2.equals("Augmented Thunder Effect"));
   }

   private boolean isAugmentedThunder(Player player) {
      return Infuse.getInstance().getHackManager().getHack(player.getUniqueId(), "1") != null
            && ChatColor.stripColor(Infuse.getInstance().getHackManager().getHack(player.getUniqueId(), "1")).toLowerCase().contains("augmented thunder")
         || Infuse.getInstance().getHackManager().getHack(player.getUniqueId(), "2") != null
            && ChatColor.stripColor(Infuse.getInstance().getHackManager().getHack(player.getUniqueId(), "2")).toLowerCase().contains("augmented thunder");
   }

   @EventHandler
   public void onTridentHit(EntityDamageByEntityEvent event) {
      if (event.getDamager() instanceof Trident trident && !trident.hasMetadata("thunderProcessed")) {
         trident.setMetadata("thunderProcessed", new FixedMetadataValue(this.plugin, true));
         if (trident.getShooter() instanceof Player attacker
            && Settings.passive(KEY, "trident_smite")
            && this.hasImmortalHackEquipped(attacker)
            && event.getEntity() instanceof LivingEntity target) {
            if (target instanceof Player tp && Infuse.getInstance().getTrustManager().isTrusted(attacker, tp)) {
               return;
            }

            target.getWorld().strikeLightningEffect(target.getLocation());
            this.applyThunderDamage(target, strikeHearts());
            attacker.playSound(attacker.getLocation(), Sound.ITEM_TRIDENT_THUNDER, 1.0F, 1.0F);
            strikeParticles(target.getLocation().add(0.0, 1.0, 0.0));
         }
      }
   }

   public void activateSpark(Player caster) {
      UUID playerUUID = caster.getUniqueId();
      if (!CooldownManager.isOnCooldown(playerUUID, "thunder")) {
         int duration = Infuse.getInstance().getConfig().getInt("durations.thunder_spark", 10);
         double radius = Infuse.getInstance().getConfig().getDouble("radius.thunder", 7.0);
         particles.spawnEffect("thunder", caster, Color.fromRGB(252, 237, 0));
         caster.playSound(caster.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1.0F, 2.0F);
         this.activeSparks.add(playerUUID);
         CooldownManager.setDuration(playerUUID, "thunder", duration);
         int configcooldown = Infuse.getInstance().getConfig().getInt("cooldowns.thunder_spark", 90);
         boolean isAugmented = this.isAugmentedThunder(caster);
         int cooldown = isAugmented ? configcooldown / 2 + duration : configcooldown + duration;
         CooldownManager.setCooldown(playerUUID, "thunder", cooldown);
         World world = caster.getWorld();
         Map<UUID, Integer> strikeCount = new HashMap<>();
         int effectDuration = duration * 20;
         int[] ticksElapsed = new int[]{0};
         Location anchor = caster.getLocation();
         Bukkit.getRegionScheduler()
            .runAtFixedRate(
               this.plugin,
               anchor,
               task -> {
                  if (ticksElapsed[0] >= effectDuration) {
                     this.activeSparks.remove(playerUUID);
                     task.cancel();
                  } else {
                     Location center = caster.getLocation();
                     if (center.getWorld().equals(world)) {
                        if (ticksElapsed[0] % 5 == 0 && Settings.particles(KEY, 1) > 0) {
                           for (int angle = 0; angle < 360; angle += 15) {
                              double radians = Math.toRadians(angle);
                              double x = center.getX() + radius * Math.cos(radians);
                              double z = center.getZ() + radius * Math.sin(radians);
                              double y = center.getY();
                              Location particleLoc = new Location(world, x, y, z);
                              world.spawnParticle(Particle.DUST, particleLoc, 1, 0.0, 0.0, 0.0, 0.0, new DustOptions(Color.GRAY, 1.0F));
                              world.spawnParticle(Particle.DUST, particleLoc, 1, 0.0, 0.0, 0.0, 0.0, new DustOptions(Color.YELLOW, 1.0F));
                              world.spawnParticle(Particle.DUST, particleLoc, 1, 0.0, 0.0, 0.0, 0.0, new DustOptions(Color.BLACK, 1.0F));
                           }
                        }

                        for (Entity entity : world.getNearbyEntities(center, radius, radius, radius)) {
                           if (entity instanceof LivingEntity target
                              && !target.equals(caster)
                              && !(target instanceof Player p && Infuse.getInstance().getTrustManager().isTrusted(caster, p))) {
                              UUID targetUUID = target.getUniqueId();
                              if (!CooldownManager.isOnCooldown(targetUUID, "lightning") && !CooldownManager.isOnCooldown(targetUUID, "chill")) {
                                 int strikes = strikeCount.getOrDefault(targetUUID, 0);
                                 if (strikes < 3) {
                                    target.getWorld().strikeLightningEffect(target.getLocation());
                                    this.applyThunderDamage(target, strikeHearts());
                                    strikeParticles(target.getLocation().add(0.0, 0.0, 0.0));
                                    CooldownManager.setCooldown(targetUUID, "chill", 2L);
                                    strikeCount.put(targetUUID, strikes + 1);
                                 }
                              }
                           }
                        }

                        ticksElapsed[0]++;
                        return;
                     }

                     task.cancel();
                  }
               },
               1L,
               1L
            );
      }
   }

   @EventHandler
   public void onPlayerAttack(EntityDamageByEntityEvent event) {
      if (event.getDamager() instanceof Player attacker && this.hasImmortalHackEquipped(attacker) && event.getEntity() instanceof LivingEntity target) {
         if (target instanceof Player tp && Infuse.getInstance().getTrustManager().isTrusted(attacker, tp)) {
            return;
         }

         UUID targetUUID = target.getUniqueId();
         long now = System.currentTimeMillis();
         Long last = this.entityLightningCooldowns.get(targetUUID);
         if (last == null || now - last >= 2000L) {
            this.entityLightningCooldowns.put(targetUUID, now);
            List<Entity> nearbyEntities = target.getNearbyEntities(3.0, 3.0, 3.0);
            Optional<Entity> nextChainTarget = nearbyEntities.stream().filter(e -> e instanceof LivingEntity && !e.equals(attacker)).findFirst();
            if (nextChainTarget.isPresent()) {
               target.getWorld().strikeLightningEffect(target.getLocation());
               this.applyThunderDamage(target, strikeHearts());
               CooldownManager.setCooldown(targetUUID, "lightning", 1L);
               strikeParticles(target.getLocation().add(0.0, 1.0, 0.0));
               // As in 2.4, the toggle (configs.chain_lightning before) only controls the chain to the nearby
               // entities; the strike on the hit target always happens.
               if (Settings.passive(KEY, "chain_lightning")) {
                  this.chainLightning(target, attacker);
               }
            }
         }
      }
   }

   private void chainLightning(Entity startEntity, Player attacker) {
      Set<Entity> processed = new HashSet<>();
      Queue<Entity> queue = new LinkedList<>();
      queue.add(startEntity);
      int maxStrikes = 5;
      int[] strikes = new int[]{0};
      Location anchor = attacker.getLocation();
      Bukkit.getRegionScheduler().runAtFixedRate(this.plugin, anchor, task -> {
         if (!queue.isEmpty() && strikes[0] < maxStrikes) {
            Entity current = null;

            while (!queue.isEmpty()) {
               Entity candidate = queue.poll();
               if (candidate instanceof LivingEntity && !processed.contains(candidate)) {
                  current = candidate;
                  break;
               }
            }

            if (current != null) {
               processed.add(current);
               LivingEntity le = (LivingEntity)current;
               if (!le.equals(attacker)) {
                  if (le instanceof Player tp && Infuse.getInstance().getTrustManager().isTrusted(attacker, tp)) {
                     return;
                  }

                  le.getWorld().strikeLightningEffect(le.getLocation());
                  this.applyThunderDamage(le, strikeHearts());
                  strikeParticles(le.getLocation().add(0.0, 1.0, 0.0));
                  strikes[0]++;

                  for (Entity e : le.getNearbyEntities(3.0, 3.0, 3.0)) {
                     if (e instanceof LivingEntity && !processed.contains(e)) {
                        queue.add(e);
                     }
                  }
               }
            }
         } else {
            task.cancel();
         }
      }, 1L, 20L);
   }

   @EventHandler
   public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
      if (event.getDamager() instanceof Player player
         && event.getEntity() instanceof LivingEntity target
         && Settings.passive(KEY, "smite_on_hit")
         && this.hasImmortalHackEquipped(player)) {
         UUID playerUUID = player.getUniqueId();
         UUID targetUUID = target.getUniqueId();
         World world = player.getWorld();
         boolean isWeatherNotClear = world.hasStorm() || world.isThundering();
         boolean isInOverworld = world.getEnvironment() == Environment.NORMAL;
         int hitThreshold = Math.max(1, isWeatherNotClear && isInOverworld
            ? Settings.intValue(KEY, "smite_on_hit", "rain_hits", 5)
            : Settings.intValue(KEY, "smite_on_hit", "hits", 10));
         this.hitCounts.putIfAbsent(playerUUID, new HashMap<>());
         Map<UUID, Integer> playerHits = this.hitCounts.get(playerUUID);
         int hitCount = playerHits.getOrDefault(targetUUID, 0) + 1;
         playerHits.put(targetUUID, hitCount);
         if (hitCount >= hitThreshold) {
            if (target instanceof Player tp && Infuse.getInstance().getTrustManager().isTrusted(player, tp)) {
               playerHits.put(targetUUID, 0);
               return;
            }

            this.Strike((Player)(target instanceof Player ? target : null));
            target.getWorld().strikeLightningEffect(target.getLocation());
            this.applyThunderDamage(target, strikeHearts());
            strikeParticles(target.getLocation().add(0.0, 1.0, 0.0));
            player.playSound(player.getLocation(), Sound.ITEM_TRIDENT_THUNDER, 1.0F, 1.0F);
            playerHits.put(targetUUID, 0);
         }
      }
   }

   public void Strike(Player target) {
      if (target != null && !CooldownManager.isOnCooldown(target.getUniqueId(), "lightning")) {
         target.getWorld().strikeLightningEffect(target.getLocation());
         CooldownManager.setCooldown(target.getUniqueId(), "lightning", 1L);
         this.applyThunderDamage(target, strikeHearts());
         strikeParticles(target.getLocation().add(0.0, 1.0, 0.0));
      }
   }
}
