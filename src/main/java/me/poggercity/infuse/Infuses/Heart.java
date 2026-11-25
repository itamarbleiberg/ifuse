package me.poggercity.infuse.Infuses;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import me.poggercity.infuse.Managers.CooldownManager;
import me.poggercity.infuse.Managers.Infuse;
import me.poggercity.infuse.Managers.Settings;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class Heart implements Listener {
   public static final String KEY = "heart";
   private static final String HEALTH_KEY = "health";
   private static final double DEFAULT_MAX = 20.0;
   private static final double SPARK_MAX = 40.0;
   private final Map<UUID, Map<UUID, Integer>> hitCounts = new HashMap<>();
   private final Map<UUID, ArmorStand> healthDisplays = new HashMap<>();
   private NamespacedKey logoutHealthKey;

   public static boolean isVirusGlitchGem(ItemStack item) {
      if (item != null && item.getType() == Material.POTION) {
         ItemMeta meta = item.getItemMeta();
         return meta != null && meta.hasCustomModelData() && meta.getCustomModelData() == 6;
      } else {
         return false;
      }
   }

   private NamespacedKey logoutHealthKey() {
      if (this.logoutHealthKey == null) {
         this.logoutHealthKey = new NamespacedKey(Infuse.getInstance(), "logout_health");
      }

      return this.logoutHealthKey;
   }

   public boolean hasHeartEffectEquipped(Player player) {
      String hack1 = Infuse.getInstance().getHackManager().getHack(player.getUniqueId(), "1");
      String hack2 = Infuse.getInstance().getHackManager().getHack(player.getUniqueId(), "2");
      return hack1 != null && ChatColor.stripColor(hack1).toLowerCase().contains("heart effect")
         || hack2 != null && ChatColor.stripColor(hack2).toLowerCase().contains("heart effect")
         || WinnersCurse.borrowsPassive(player, KEY);
   }

   /** The base max health this player should have right now. */
   private double desiredBaseMaxHealth(Player player) {
      UUID id = player.getUniqueId();
      double base = DEFAULT_MAX;
      if (this.hasHeartEffectEquipped(player) && Settings.passive(KEY, "extra_hearts")) {
         base += Math.max(0.0, Settings.value(KEY, "extra_hearts", "hearts", 5.0)) * 2.0;
      }

      if (CooldownManager.isEffectActive(id, HEALTH_KEY) || CooldownManager.isEffectActive(id, "apophis")) {
         base = Math.max(base, SPARK_MAX);
      }

      return base;
   }

   /**
    * Runs every tick. Sets the base max health from the Heart passive (+5 hearts), the Heart spark and the Apophis spark
    * (both 20 hearts). This used to run only when a player moved to another block, so standing still kept the extra
    * hearts after the effect was gone, and it clamped health against the base value, which ignored other bonuses such
    * as Ocean's water hearts.
    */
   public void tickMaxHealth() {
      for (Player player : Bukkit.getOnlinePlayers()) {
         this.updateMaxHealth(player);
      }
   }

   public void updateMaxHealth(Player player) {
      if (!player.isDead()) {
         AttributeInstance max = player.getAttribute(Attribute.MAX_HEALTH);
         if (max != null) {
            double desired = this.desiredBaseMaxHealth(player);
            if (max.getBaseValue() != desired) {
               max.setBaseValue(desired);
            }

            if (player.getHealth() > max.getValue()) {
               player.setHealth(max.getValue());
            }
         }
      }
   }

   @EventHandler
   public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
      if (event.getDamager() instanceof Player player
         && event.getEntity() instanceof LivingEntity target
         && this.hasHeartEffectEquipped(player)
         && Settings.passive(KEY, "health_display")) {
         UUID playerUUID = player.getUniqueId();
         UUID targetUUID = target.getUniqueId();
         int threshold = Math.max(1, Settings.intValue(KEY, "health_display", "hits", 10));
         this.hitCounts.putIfAbsent(playerUUID, new HashMap<>());
         Map<UUID, Integer> playerHits = this.hitCounts.get(playerUUID);
         int hitCount = playerHits.getOrDefault(targetUUID, 0) + 1;
         playerHits.put(targetUUID, hitCount);
         if (hitCount >= threshold) {
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
      ArmorStand armorStand = player.getWorld().spawn(location, ArmorStand.class);
      armorStand.setInvisible(true);
      armorStand.setMarker(true);
      armorStand.setGravity(false);
      armorStand.setPersistent(false);
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
            if (Settings.passive(KEY, "golden_apple_absorption")) {
               int level = Math.max(1, Settings.intValue(KEY, "golden_apple_absorption", "level", 5));
               int seconds = Math.max(1, Settings.intValue(KEY, "golden_apple_absorption", "seconds", 120));
               player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, seconds * 20, level - 1));
            }
         } else if (Settings.passive(KEY, "food_absorption")) {
            int level = Math.max(1, Settings.intValue(KEY, "food_absorption", "level", 1));
            int seconds = Math.max(1, Settings.intValue(KEY, "food_absorption", "seconds", 30));
            player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, seconds * 20, level - 1));
         }
      }
   }

   public void activateSpark(Player player) {
      UUID playerUUID = player.getUniqueId();
      if (!CooldownManager.isOnCooldown(playerUUID, HEALTH_KEY)) {
         particles.spawnEffect(KEY, player, Color.fromRGB(252, 0, 70));
         particles.spawnHearts(KEY, player);
         int duration = Infuse.getInstance().getConfig().getInt("effects.heart.spark.duration", 60);
         CooldownManager.setDuration(playerUUID, HEALTH_KEY, duration);
         // 20 hearts while the spark lasts; tickMaxHealth takes them away again when it ends.
         this.updateMaxHealth(player);
         player.setHealth(player.getAttribute(Attribute.MAX_HEALTH).getValue());
         int configcooldown = Infuse.getInstance().getConfig().getInt("effects.heart.spark.cooldown", 120);
         String h1 = Infuse.getInstance().getHackManager().getHack(playerUUID, "1");
         String h2 = Infuse.getInstance().getHackManager().getHack(playerUUID, "2");
         boolean isAugmented = this.hasHeartEffectEquipped(player)
            && (h1 != null && h1.toLowerCase().contains("augmented heart") || h2 != null && h2.toLowerCase().contains("augmented heart"));
         int cooldown = isAugmented ? configcooldown / 2 + duration : configcooldown + duration;
         CooldownManager.setCooldown(playerUUID, HEALTH_KEY, cooldown);
      }
   }

   // Joining used to set health to 20 for everyone, so relogging fully healed a player. Health is now kept: it is saved
   // when the player leaves (before their max health is put back to normal) and restored when they join.
   @EventHandler(priority = EventPriority.LOWEST)
   public void onPlayerJoin(PlayerJoinEvent event) {
      Player player = event.getPlayer();
      Double saved = player.getPersistentDataContainer().get(this.logoutHealthKey(), PersistentDataType.DOUBLE);
      player.getPersistentDataContainer().remove(this.logoutHealthKey());
      this.updateMaxHealth(player);
      if (saved != null && saved > 0.0 && !player.isDead()) {
         AttributeInstance max = player.getAttribute(Attribute.MAX_HEALTH);
         if (max != null) {
            player.setHealth(Math.min(saved, max.getValue()));
         }
      }
   }

   // Max health goes back to 20 in the saved player data, so nothing is left behind if Infuse is removed. The real
   // health is stored first so it survives the relog.
   @EventHandler(priority = EventPriority.MONITOR)
   public void onPlayerQuit(PlayerQuitEvent event) {
      Player player = event.getPlayer();
      if (!player.isDead()) {
         player.getPersistentDataContainer().set(this.logoutHealthKey(), PersistentDataType.DOUBLE, player.getHealth());
      }

      AttributeInstance max = player.getAttribute(Attribute.MAX_HEALTH);
      if (max != null) {
         max.setBaseValue(DEFAULT_MAX);
         if (player.getHealth() > max.getValue()) {
            player.setHealth(max.getValue());
         }
      }

      this.removeHealthDisplay(player.getUniqueId());
   }
}
