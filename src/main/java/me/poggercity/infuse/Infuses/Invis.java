package me.poggercity.infuse.Infuses;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import me.poggercity.infuse.Managers.CooldownManager;
import me.poggercity.infuse.Managers.Infuse;
import me.poggercity.infuse.Managers.PlayerHackManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public class Invis implements Listener {
   private static final String GEM_NAME = ChatColor.DARK_PURPLE + "Invis Effect";
   private static final String IMMORTAL_KEY = "invis";
   private final PlayerHackManager hackManager = new PlayerHackManager();
   private final Plugin plugin;
   private final Map<UUID, Integer> meleeHitCounter = new HashMap<>();
   double radius = Infuse.getInstance().getConfig().getDouble("radius.invis", 10.0);

   public Invis(Plugin plugin) {
      this.plugin = plugin;
   }

   public void applyInvisEffects() {
      for (Player player : Bukkit.getOnlinePlayers()) {
         if (this.hasImmortalHackEquipped(player)) {
            Location loc = player.getLocation();
            Bukkit.getRegionScheduler()
               .run(this.plugin, loc, task -> player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 40, 0, false, false, false)));
         }
      }
   }

   @EventHandler
   public void onWitherDamage(EntityDamageByEntityEvent event) {
      if (event.getEntity() instanceof Player && event.getDamager().getType() == EntityType.WITHER) {
         Player player = (Player)event.getEntity();
         if (player.hasPotionEffect(PotionEffectType.BLINDNESS)) {
            for (PotionEffect effect : player.getActivePotionEffects()) {
               if (effect.getType() == PotionEffectType.BLINDNESS) {
                  event.setCancelled(true);
                  return;
               }
            }
         }
      }
   }

   public static boolean isStealthGem(ItemStack item) {
      return item != null
         && item.getType() == Material.POTION
         && item.hasItemMeta()
         && item.getItemMeta().hasCustomModelData()
         && item.getItemMeta().getCustomModelData() == 7;
   }

   private boolean hasImmortalHackEquipped(Player player) {
      String hack1 = Infuse.getInstance().getHackManager().getHack(player.getUniqueId(), "1");
      String hack2 = Infuse.getInstance().getHackManager().getHack(player.getUniqueId(), "2");
      return hack1 != null && (hack1.equalsIgnoreCase(ChatColor.stripColor(GEM_NAME)) || hack1.equals("Augmented Invis Effect"))
         || hack2 != null && (hack2.equalsIgnoreCase(ChatColor.stripColor(GEM_NAME)) || hack2.equals("Augmented Invis Effect"));
   }

   @EventHandler
   public void onProjectileHit(ProjectileHitEvent event) {
      if (event.getEntity().getShooter() instanceof Player shooterPlayer
         && this.hasImmortalHackEquipped(shooterPlayer)
         && event.getEntity() instanceof Arrow
         && event.getHitEntity() instanceof LivingEntity target) {
         if (target instanceof Player playerTarget && Infuse.getInstance().getTrustManager().isTrusted(shooterPlayer, playerTarget)) {
            return;
         }

         Location loc = target.getLocation();
         Bukkit.getRegionScheduler().run(this.plugin, loc, task -> {
            target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 80, 0, false, false));
            target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 80, 0, false, false));
         });
         this.spawnBlackParticles(target, 3);
      }
   }

   @EventHandler
   public void onMeleeHit(EntityDamageByEntityEvent event) {
      if (event.getDamager() instanceof Player attacker && event.getEntity() instanceof LivingEntity target && this.hasImmortalHackEquipped(attacker)) {
         if (target instanceof Player playerTarget && Infuse.getInstance().getTrustManager().isTrusted(attacker, playerTarget)) {
            return;
         }

         int count = this.meleeHitCounter.getOrDefault(attacker.getUniqueId(), 0) + 1;
         this.meleeHitCounter.put(attacker.getUniqueId(), count);
         if (count >= 10) {
            Location loc = target.getLocation();
            Bukkit.getRegionScheduler().run(this.plugin, loc, task -> {
               target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 80, 0, false, false));
               target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 80, 0, false, false));
            });
            this.spawnBlackParticles(target, 4);
            this.meleeHitCounter.put(attacker.getUniqueId(), 0);
         }
      }
   }

   private void spawnBlackParticles(Entity target, int durationInSeconds) {
      int maxTicks = durationInSeconds * 20;
      int[] ticksElapsed = new int[]{0};
      Location anchor = target.getLocation();
      Bukkit.getRegionScheduler().runAtFixedRate(this.plugin, anchor, task -> {
         if (ticksElapsed[0] >= maxTicks) {
            task.cancel();
         } else {
            target.getWorld().spawnParticle(Particle.SQUID_INK, target.getLocation().add(0.0, 1.0, 0.0), 20, 0.0, 0.0, 0.0, 0.05);
            ticksElapsed[0] += 10;
         }
      }, 1L, 10L);
   }

   @EventHandler
   public void onEntityTarget(EntityTargetEvent event) {
      if (event.getTarget() instanceof Player target && this.hasImmortalHackEquipped(target)) {
         event.setCancelled(true);
      }
   }

   public void activateSpark(Player caster) {
      UUID playerUUID = caster.getUniqueId();
      if (!CooldownManager.isOnCooldown(playerUUID, "invis")) {
         particles.spawnEffect("invis", caster, Color.fromRGB(126, 0, 252));
         caster.playSound(caster.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 0.3F, 2.0F);
         World world = caster.getWorld();
         world.playSound(caster.getLocation(), Sound.ENTITY_WITHER_SHOOT, 0.5F, 1.0F);
         world.playSound(caster.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, 0.5F, 0.5F);
         int duration = Infuse.getInstance().getConfig().getInt("durations.invis_spark", 10);
         long durationTicks = duration * 20L;
         Set<Player> vanishedPlayers = new HashSet<>();
         vanishedPlayers.add(caster);

         for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getWorld().equals(world)
               && p.getLocation().distance(caster.getLocation()) <= this.radius
               && Infuse.getInstance().getTrustManager().isTrusted(caster, p)) {
               vanishedPlayers.add(p);
            }
         }

         for (Player vp : vanishedPlayers) {
            vp.hidePlayer(this.plugin, vp);

            for (Player other : Bukkit.getOnlinePlayers()) {
               if (!vp.equals(other) && !Infuse.getInstance().getTrustManager().isTrusted(vp, other)) {
                  other.hidePlayer(this.plugin, vp);
               }
            }
         }

         Set<Player> vanishedFinal = new HashSet<>(vanishedPlayers);
         Location anchor = caster.getLocation();
         long[] ticksElapsed = new long[]{0L};
         Bukkit.getRegionScheduler()
            .runAtFixedRate(
               this.plugin,
               anchor,
               task -> {
                  if (ticksElapsed[0] >= durationTicks) {
                     task.cancel();

                     for (Player vp2 : vanishedFinal) {
                        vp2.showPlayer(this.plugin, vp2);

                        for (Player otherx : Bukkit.getOnlinePlayers()) {
                           otherx.showPlayer(this.plugin, vp2);
                        }
                     }
                  } else {
                     Location center = caster.getLocation();
                     world.spawnParticle(Particle.LARGE_SMOKE, caster.getLocation(), 5, 0.2, 0.0, 0.2, 0.0);

                     for (int angle = 0; angle < 360; angle += 5) {
                        double rad = Math.toRadians(angle);
                        double baseX = center.getX() + this.radius * Math.cos(rad);
                        double baseZ = center.getZ() + this.radius * Math.sin(rad);

                        for (int i = 0; i < 3; i++) {
                           double offsetX = (Math.random() - 0.5) * 0.3;
                           double offsetZ = (Math.random() - 0.5) * 0.3;
                           Location particleLoc = new Location(world, baseX + offsetX, center.getY(), baseZ + offsetZ);
                           world.spawnParticle(Particle.SQUID_INK, particleLoc.add(0.0, 1.5, 0.0), 20, 0.0, 0.0, 0.0, 0.05);
                        }
                     }

                     for (Player px : Bukkit.getOnlinePlayers()) {
                        if (px.getWorld().equals(world)
                           && px.getLocation().distance(center) <= this.radius
                           && !Infuse.getInstance().getTrustManager().isTrusted(caster, px)
                           && !px.equals(caster)) {
                           Location targetAnchor = px.getLocation();
                           Bukkit.getRegionScheduler().run(this.plugin, targetAnchor, task2 -> {
                              px.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 0, false, false));
                              px.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 20, 0, false, false));
                              this.spawnBlackParticles(px, 1);
                           });
                        }
                     }

                     if (ticksElapsed[0] % 40L == 0L) {
                        world.playSound(caster.getLocation(), Sound.ENTITY_BREEZE_SLIDE, 1.0F, 0.6F);
                     }

                     ticksElapsed[0] += 10L;
                  }
               },
               1L,
               10L
            );
         CooldownManager.setDuration(playerUUID, "invis", duration);
         int configcooldown = Infuse.getInstance().getConfig().getInt("cooldowns.invis_spark", 90);
         boolean isAugmented = Infuse.getInstance().getHackManager().getHack(playerUUID, "1") != null
               && ChatColor.stripColor(Infuse.getInstance().getHackManager().getHack(playerUUID, "1")).toLowerCase().contains("augmented invis")
            || Infuse.getInstance().getHackManager().getHack(playerUUID, "2") != null
               && ChatColor.stripColor(Infuse.getInstance().getHackManager().getHack(playerUUID, "2")).toLowerCase().contains("augmented invis");
         int cooldown = isAugmented ? configcooldown / 2 + duration : configcooldown + duration;
         CooldownManager.setCooldown(playerUUID, "invis", cooldown);
      }
   }

   @EventHandler
   public void onPlayerAttack(PlayerAnimationEvent event) {
      if (event.getAnimationType() == PlayerAnimationType.ARM_SWING) {
         Player attacker = event.getPlayer();
         ItemStack weapon = attacker.getInventory().getItemInMainHand();
         double totalDamage = this.getTotalDamage(attacker, weapon);

         for (Player nearby : attacker.getWorld().getPlayers()) {
            if (!nearby.equals(attacker)
               && !attacker.canSee(nearby)
               && nearby.getLocation().distance(attacker.getLocation()) <= 3.0
               && this.isFacingEntityBody(attacker, nearby)
               && nearby.getNoDamageTicks() <= 0) {
               Bukkit.getRegionScheduler().run(this.plugin, nearby.getLocation(), task -> {
                  if (!nearby.isDead() && nearby.isOnline()) {
                     if (nearby.isBlocking()) {
                        if (!this.isAxe(weapon)) {
                           nearby.getWorld().playSound(nearby.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1.0F, 1.0F);
                           return;
                        }

                        nearby.setCooldown(Material.SHIELD, 100);
                        nearby.getWorld().playSound(nearby.getLocation(), Sound.ITEM_SHIELD_BREAK, 1.0F, 1.0F);
                     }

                     nearby.damage(totalDamage, attacker);
                     if (this.isCrit(attacker)) {
                        nearby.getWorld().playSound(nearby.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0F, 1.0F);
                        nearby.getWorld().spawnParticle(Particle.CRIT, nearby.getLocation().add(0.0, 1.5, 0.0), 20, 0.0, 0.0, 0.0, 0.5);
                     } else {
                        nearby.getWorld().playSound(nearby.getLocation(), Sound.ENTITY_PLAYER_ATTACK_STRONG, 1.0F, 1.0F);
                     }
                  }
               });
            }
         }
      }
   }

   private double getTotalDamage(Player attacker, ItemStack weapon) {
      double baseWeaponDamage = this.getWeaponBaseDamage(weapon);
      double extraEnchantDamage = this.getSharpnessBonus(weapon);
      double strengthBonus = 0.0;
      if (attacker.hasPotionEffect(PotionEffectType.STRENGTH)) {
         PotionEffect strengthEffect = attacker.getPotionEffect(PotionEffectType.STRENGTH);
         if (strengthEffect != null) {
            int amplifier = strengthEffect.getAmplifier();
            strengthBonus = 3.0 * (amplifier + 1);
         }
      }

      float cooldownMultiplier = attacker.getAttackCooldown();
      double totalDamage = (baseWeaponDamage + extraEnchantDamage + strengthBonus) * cooldownMultiplier;
      if (this.isCrit(attacker)) {
         totalDamage *= 1.5;
      }

      return totalDamage;
   }

   private double getWeaponBaseDamage(ItemStack weapon) {
      if (weapon != null && weapon.getType() != Material.AIR) {
         switch (weapon.getType()) {
            case WOODEN_SWORD:
               return 4.0;
            case STONE_SWORD:
               return 5.0;
            case IRON_SWORD:
               return 6.0;
            case DIAMOND_SWORD:
               return 7.0;
            case NETHERITE_SWORD:
               return 8.0;
            case WOODEN_AXE:
               return 7.0;
            case STONE_AXE:
               return 9.0;
            case IRON_AXE:
               return 9.0;
            case DIAMOND_AXE:
               return 9.0;
            case NETHERITE_AXE:
               return 10.0;
            default:
               return 1.0;
         }
      } else {
         return 1.0;
      }
   }

   private double getSharpnessBonus(ItemStack weapon) {
      if (weapon == null) {
         return 0.0;
      } else {
         int level = weapon.getEnchantmentLevel(Enchantment.SHARPNESS);
         return level > 0 ? 1.0 + 0.5 * level : 0.0;
      }
   }

   private boolean isCrit(Player attacker) {
      return attacker.getFallDistance() > 0.0F
         && !attacker.isOnGround()
         && !attacker.isClimbing()
         && !attacker.isInWater()
         && !attacker.hasPotionEffect(PotionEffectType.BLINDNESS)
         && !attacker.isSprinting();
   }

   private boolean isAxe(ItemStack weapon) {
      if (weapon == null) {
         return false;
      } else {
         switch (weapon.getType()) {
            case WOODEN_AXE:
            case STONE_AXE:
            case IRON_AXE:
            case DIAMOND_AXE:
            case NETHERITE_AXE:
               return true;
            default:
               return false;
         }
      }
   }

   private boolean isFacingEntityBody(Player attacker, LivingEntity target) {
      Vector attackerDir = attacker.getEyeLocation().getDirection().normalize();
      Vector toTarget = target.getLocation().add(0.0, target.getHeight() / 2.0, 0.0).toVector().subtract(attacker.getEyeLocation().toVector()).normalize();
      double dot = attackerDir.dot(toTarget);
      return dot >= 0.98;
   }
}
