package me.poggercity.infuse.Infuses;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import me.poggercity.infuse.Managers.CooldownManager;
import me.poggercity.infuse.Managers.Infuse;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class Heart implements Listener {
   private static final String HEALTH_KEY = "health";
   private final Map<UUID, Map<UUID, Integer>> hitCounts = new HashMap<>();
   private final Map<UUID, ArmorStand> healthDisplays = new HashMap<>();
   private final ConcurrentHashMap<UUID, Double> playerHealthCache = new ConcurrentHashMap<>();
   private final Set<UUID> activeSpark = ConcurrentHashMap.newKeySet();

   public static boolean isVirusGlitchGem(ItemStack item) {
      if (item != null && item.getType() == Material.POTION) {
         ItemMeta meta = item.getItemMeta();
         return meta != null && meta.hasCustomModelData() && meta.getCustomModelData() == 6;
      } else {
         return false;
      }
   }

   private boolean hasHeartEffectEquipped(Player player) {
      String hack1 = Infuse.getInstance().getHackManager().getHack(player.getUniqueId(), "1");
      String hack2 = Infuse.getInstance().getHackManager().getHack(player.getUniqueId(), "2");
      if (hack1 != null) {
         String stripped = ChatColor.stripColor(hack1).toLowerCase();
         if (stripped.contains("heart effect")) {
            return true;
         }
      }

      if (hack2 != null) {
         String stripped = ChatColor.stripColor(hack2).toLowerCase();
         if (stripped.contains("heart effect")) {
            return true;
         }
      }

      return false;
   }

   @EventHandler
   public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
      if (event.getDamager() instanceof Player player && event.getEntity() instanceof LivingEntity target && this.hasHeartEffectEquipped(player)) {
         UUID playerUUID = player.getUniqueId();
         UUID targetUUID = target.getUniqueId();
         this.hitCounts.putIfAbsent(playerUUID, new HashMap<>());
         Map<UUID, Integer> playerHits = this.hitCounts.get(playerUUID);
         int hitCount = playerHits.getOrDefault(targetUUID, 0) + 1;
         playerHits.put(targetUUID, hitCount);
         if (hitCount == 10) {
            if (target instanceof Player pTarget) {
               this.showHealthAbovePlayer(pTarget);
               player.playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, 1.0F, 1.0F);
            } else {
               this.showAndUpdateHealthAboveEntity(target);
               player.playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, 1.0F, 1.0F);
            }

            playerHits.put(targetUUID, 0);
         }
      }
   }

   private void showAndUpdateHealthAboveEntity(LivingEntity entity) {
      this.updateHealthDisplay(entity);
      entity.setCustomNameVisible(true);
      Location anchor = entity.getLocation();
      long[] ticks = new long[]{0L};
      Bukkit.getRegionScheduler().runAtFixedRate(Infuse.getInstance(), anchor, task -> {
         if (entity.isValid() && !entity.isDead() && ticks[0] < 100L) {
            this.updateHealthDisplay(entity);
            ticks[0] += 10L;
         } else {
            task.cancel();
            entity.setCustomNameVisible(false);
            entity.setCustomName(null);
         }
      }, 1L, 10L);
   }

   private void showHealthAbovePlayer(Player player) {
      UUID playerUUID = player.getUniqueId();
      if (this.healthDisplays.containsKey(playerUUID)) {
         this.healthDisplays.get(playerUUID).remove();
      }

      Location location = player.getLocation().add(0.0, 100.0, 0.0);
      ArmorStand armorStand = (ArmorStand)player.getWorld().spawn(location, ArmorStand.class);
      armorStand.setInvisible(true);
      armorStand.setMarker(true);
      armorStand.setGravity(false);
      armorStand.setCustomNameVisible(true);
      armorStand.setCustomName(this.getHealthText(player));
      player.addPassenger(armorStand);
      this.healthDisplays.put(playerUUID, armorStand);
      Location anchor = player.getLocation();
      long[] ticks = new long[]{0L};
      Bukkit.getRegionScheduler().runAtFixedRate(Infuse.getInstance(), anchor, task -> {
         if (player.isOnline() && !player.isDead() && ticks[0] < 100L && player.getPassengers().contains(armorStand)) {
            armorStand.setCustomName(this.getHealthText(player));
            ticks[0]++;
         } else {
            task.cancel();
            this.removeHealthDisplay(playerUUID);
         }
      }, 1L, 1L);
   }

   private void removeHealthDisplay(UUID playerUUID) {
      if (this.healthDisplays.containsKey(playerUUID)) {
         this.healthDisplays.get(playerUUID).remove();
         this.healthDisplays.remove(playerUUID);
      }
   }

   private String getHealthText(LivingEntity entity) {
      double health = entity.getHealth();
      ChatColor color = health > 10.0 ? ChatColor.YELLOW : ChatColor.RED;
      return "" + color + ChatColor.BOLD + "❤ " + String.format("%.1f", health);
   }

   private void updateHealthDisplay(LivingEntity entity) {
      entity.setCustomName(this.getHealthText(entity));
   }

   @EventHandler
   public void onPlayerEat(PlayerItemConsumeEvent event) {
      Player player = event.getPlayer();
      if (this.hasHeartEffectEquipped(player)) {
         ItemStack item = event.getItem();
         if (item.getType() == Material.ENCHANTED_GOLDEN_APPLE) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 2400, 4));
         } else {
            player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 600, 0));
         }
      }
   }

   public void activateSpark(Player player) {
      UUID playerUUID = player.getUniqueId();
      if (!CooldownManager.isOnCooldown(playerUUID, "health")) {
         particles.spawnEffect("heart", player, Color.fromRGB(252, 0, 70));
         particles.spawnHearts("heart", player);
         AttributeInstance maxHealthAttribute = player.getAttribute(Attribute.MAX_HEALTH);
         if (maxHealthAttribute != null) {
            maxHealthAttribute.setBaseValue(40.0);
         }

         player.setHealth(player.getAttribute(Attribute.MAX_HEALTH).getValue());
         this.activeSpark.add(playerUUID);
         int duration = Infuse.getInstance().getConfig().getInt("durations.heart_spark", 60);
         CooldownManager.setDuration(playerUUID, "health", duration);
         int configcooldown = Infuse.getInstance().getConfig().getInt("cooldowns.heart_spark", 120);
         String h1 = Infuse.getInstance().getHackManager().getHack(playerUUID, "1");
         String h2 = Infuse.getInstance().getHackManager().getHack(playerUUID, "2");
         boolean isAugmented = this.hasHeartEffectEquipped(player)
            && (h1 != null && h1.toLowerCase().contains("augmented heart") || h2 != null && h2.toLowerCase().contains("augmented heart"));
         int cooldown = isAugmented ? configcooldown / 2 + duration : configcooldown + duration;
         CooldownManager.setCooldown(playerUUID, "health", cooldown);
         Location anchor = player.getLocation();
         Bukkit.getRegionScheduler().runDelayed(Infuse.getInstance(), anchor, task -> {
            this.activeSpark.remove(playerUUID);
            AttributeInstance maxAttr = player.getAttribute(Attribute.MAX_HEALTH);
            if (maxAttr != null) {
               if (this.hasHeartEffectEquipped(player)) {
                  maxAttr.setBaseValue(30.0);
                  if (player.getHealth() > 30.0) {
                     player.setHealth(30.0);
                  }

                  this.playerHealthCache.put(playerUUID, 30.0);
               } else {
                  maxAttr.setBaseValue(20.0);
                  if (player.getHealth() > 20.0) {
                     player.setHealth(20.0);
                  }

                  this.playerHealthCache.remove(playerUUID);
               }
            }
         }, duration * 20L);
      }
   }

   @EventHandler
   public void onPlayerJoin(PlayerJoinEvent event) {
      Player player = event.getPlayer();
      Location anchor = player.getLocation();
      Bukkit.getRegionScheduler().runDelayed(Infuse.getInstance(), anchor, task -> {
         AttributeInstance maxHealthAttribute = player.getAttribute(Attribute.MAX_HEALTH);
         if (maxHealthAttribute != null) {
            maxHealthAttribute.setBaseValue(20.0);
            player.setHealth(20.0);
         }

         this.activeSpark.remove(player.getUniqueId());
      }, 15L);
   }

   @EventHandler
   public void onPlayerQuit(PlayerQuitEvent event) {
      Player player = event.getPlayer();
      AttributeInstance max = player.getAttribute(Attribute.MAX_HEALTH);
      if (max != null) {
         max.setBaseValue(20.0);
         if (player.getHealth() > 20.0) {
            player.setHealth(20.0);
         }
      }

      this.activeSpark.remove(player.getUniqueId());
   }

   @EventHandler
   public void onPlayerMove(PlayerMoveEvent event) {
      Player player = event.getPlayer();
      if (event.getFrom().getBlockX() != event.getTo().getBlockX()
         || event.getFrom().getBlockY() != event.getTo().getBlockY()
         || event.getFrom().getBlockZ() != event.getTo().getBlockZ()) {
         AttributeInstance maxHealthAttribute = player.getAttribute(Attribute.MAX_HEALTH);
         if (maxHealthAttribute == null) {
            return;
         }

         UUID playerId = player.getUniqueId();
         boolean hasEffect = this.hasHeartEffectEquipped(player);
         boolean isSparked = this.activeSpark.contains(playerId);
         if (!hasEffect && !isSparked) {
            if (maxHealthAttribute.getBaseValue() != 20.0) {
               maxHealthAttribute.setBaseValue(20.0);
               if (player.getHealth() > 20.0) {
                  player.setHealth(20.0);
               }
            }

            this.playerHealthCache.remove(playerId);
         } else {
            if (isSparked) {
               if (maxHealthAttribute.getBaseValue() < 40.0) {
                  maxHealthAttribute.setBaseValue(40.0);
               }
            } else if (maxHealthAttribute.getBaseValue() < 30.0) {
               maxHealthAttribute.setBaseValue(30.0);
            }

            if (player.getHealth() > maxHealthAttribute.getBaseValue()) {
               player.setHealth(maxHealthAttribute.getBaseValue());
            }

            this.playerHealthCache.put(playerId, maxHealthAttribute.getBaseValue());
         }
      }
   }
}
