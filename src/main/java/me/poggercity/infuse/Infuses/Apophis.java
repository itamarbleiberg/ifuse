package me.poggercity.infuse.Infuses;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import me.poggercity.infuse.Managers.CooldownManager;
import me.poggercity.infuse.Managers.Infuse;
import me.poggercity.infuse.Managers.Settings;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class Apophis implements Listener {
   public static final String KEY = "apophis";
   private static final String GEM_NAME = HexColorTranslator.translateToMinecraftColor("#301145") + "Apophis Effect";
   private static final String APOPHIS_KEY = "apophis";
   private final Plugin plugin;
   private final Set<UUID> lavaFlyers = new HashSet<>();
   private final Map<UUID, Integer> apophisHitCount = new HashMap<>();

   public Apophis(Plugin plugin) {
      this.plugin = plugin;
   }

   public static boolean isApophis(ItemStack item) {
      if (item != null && item.getType() == Material.POTION && item.hasItemMeta()) {
         ItemMeta meta = item.getItemMeta();
         if (meta == null) {
            return false;
         } else {
            return meta.hasCustomModelData() && meta.getCustomModelData() == 15
               ? true
               : meta.hasDisplayName() && ChatColor.stripColor(meta.getDisplayName()).equals(ChatColor.stripColor(GEM_NAME));
         }
      } else {
         return false;
      }
   }

   public static boolean isApophisGem(ItemStack item) {
      return item != null
         && item.getType() == Material.POTION
         && item.hasItemMeta()
         && item.getItemMeta().hasCustomModelData()
         && item.getItemMeta().getCustomModelData() == 15;
   }

   private boolean hasApophisEquipped(Player player) {
      String h1 = Infuse.getInstance().getHackManager().getHack(player.getUniqueId(), "1");
      String h2 = Infuse.getInstance().getHackManager().getHack(player.getUniqueId(), "2");
      return h1 != null && (h1.equalsIgnoreCase(ChatColor.stripColor(GEM_NAME)) || h1.equals("Augmented Apophis Effect"))
         || h2 != null && (h2.equalsIgnoreCase(ChatColor.stripColor(GEM_NAME)) || h2.equals("Augmented Apophis Effect"));
   }

   private boolean isAugmentedApophis(Player player) {
      String h1 = Infuse.getInstance().getHackManager().getHack(player.getUniqueId(), "1");
      String h2 = Infuse.getInstance().getHackManager().getHack(player.getUniqueId(), "2");
      return h1 != null && ChatColor.stripColor(h1).toLowerCase().contains("augmented apophis")
         || h2 != null && ChatColor.stripColor(h2).toLowerCase().contains("augmented apophis");
   }

   public void applyApophisEffects() {
      List<String> disabledWorlds = Infuse.getInstance().getConfig().getStringList("configs.disabled_worlds").stream().map(String::toLowerCase).toList();

      for (Player player : Bukkit.getOnlinePlayers()) {
         String worldName = player.getWorld().getName().toLowerCase();
         if (disabledWorlds.contains(worldName)) {
            this.disableLavaFlightIfNeeded(player);
         } else if (!this.hasApophisEquipped(player)) {
            this.disableLavaFlightIfNeeded(player);
         } else {
            Location anchor = player.getLocation();
            Bukkit.getRegionScheduler()
               .run(
                  Infuse.getInstance(),
                  anchor,
                  task -> {
                     if (Settings.passive(KEY, "regeneration")) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 40, 0, false, false, false));
                     }

                     if (Settings.passive(KEY, "water_breathing")) {
                        player.addPotionEffects(
                           Arrays.asList(
                              new PotionEffect(PotionEffectType.CONDUIT_POWER, 40, 0, false, false, false),
                              new PotionEffect(PotionEffectType.DOLPHINS_GRACE, 40, 0, false, false, false)
                           )
                        );
                     }

                     if (Settings.passive(KEY, "fire_resistance")) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 40, 0, false, false, false));
                     }

                     if (Settings.passive(KEY, "luck")) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.LUCK, 40, 0, false, false, false));
                     }

                     if (Settings.passive(KEY, "haste")) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 40, 1, false, false, false));
                     }

                     // configs.apophis_lava_fly existed but was never checked; it is now the lava_flight passive.
                     if (Settings.passive(KEY, "lava_flight")) {
                        this.handleLavaFlight(player);
                     } else {
                        this.disableLavaFlightIfNeeded(player);
                     }
                  }
               );
         }
      }
   }

   private void handleLavaFlight(Player player) {
      UUID id = player.getUniqueId();
      if (player.isInLava()) {
         if (this.lavaFlyers.add(id)) {
            player.setAllowFlight(true);
            player.setFlying(true);
         }
      } else if (this.lavaFlyers.remove(id)) {
         player.setFlying(false);
         player.setAllowFlight(false);
      }
   }

   @EventHandler
   public void onQuit(PlayerQuitEvent event) {
      this.disableLavaFlightIfNeeded(event.getPlayer());
      this.apophisHitCount.remove(event.getPlayer().getUniqueId());
   }

   private void disableLavaFlightIfNeeded(Player player) {
      UUID id = player.getUniqueId();
      if (this.lavaFlyers.remove(id)) {
         player.setFlying(false);
         player.setAllowFlight(false);
      }
   }

   public void activateSpark(Player player) {
      UUID uuid = player.getUniqueId();
      if (!CooldownManager.isOnCooldown(uuid, "apophis")) {
         particles.spawnEffect("apophis", player, Color.fromRGB(48, 17, 69));
         player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1.0F, 2.0F);
         int duration = Infuse.getInstance().getConfig().getInt("durations.apophis_spark", 20);
         // 20 hearts for the spark's duration: Heart.tickMaxHealth gives them while the "apophis" duration is active
         // and takes them away afterwards. The tool/weapon enchantments are given by ToolBonuses for the same time;
         // they used to be written onto the held item, stayed there if the player switched items, and enchantments
         // the item already had were deleted when the spark ended.
         CooldownManager.setDuration(uuid, "apophis", duration);
         Infuse.getInstance().getHeart().updateMaxHealth(player);
         player.setHealth(Math.min(player.getHealth() + 20.0, player.getAttribute(Attribute.MAX_HEALTH).getValue()));
         CooldownManager.setDuration(uuid, "st", duration);
         double radius = Infuse.getInstance().getConfig().getDouble("radius.apophis", 5.0);

         for (Entity e : player.getNearbyEntities(radius, radius, radius)) {
            if (e.getWorld() != null
               && e.getWorld().equals(player.getWorld())
               && e instanceof Player target
               && !Infuse.getInstance().getTrustManager().isTrusted(player, target)) {
               Location tAnchor = target.getLocation();
               Bukkit.getRegionScheduler().run(this.plugin, tAnchor, task -> {
                  target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 200, 0, false, false));
                  target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 200, 0, false, false));
               });
            }
         }

         CooldownManager.setCooldown(uuid, "fire_spark", 0L);
         Infuse.getInstance().getFire().activateSpark(player);
         int baseCd = Infuse.getInstance().getConfig().getInt("cooldowns.apophis_spark", 90);
         boolean augmented = this.isAugmentedApophis(player);
         int finalCd = augmented ? baseCd / 2 + duration : baseCd + duration;
         CooldownManager.setCooldown(uuid, "apophis", finalCd);
      }
   }

   @EventHandler
   public void onPlayerHit(EntityDamageByEntityEvent event) {
      if (event.getDamager() instanceof Player attacker
         && event.getEntity() instanceof Player target
         && this.hasApophisEquipped(attacker)
         && Settings.passive(KEY, "wither_on_hit")
         && !Infuse.getInstance().getTrustManager().isTrusted(attacker, target)) {
         UUID attackerId = attacker.getUniqueId();
         int hits = this.apophisHitCount.getOrDefault(attackerId, 0) + 1;
         int witherTicks = Math.max(1, Settings.intValue(KEY, "wither_on_hit", "seconds", 5)) * 20;
         if (hits >= Math.max(1, Settings.intValue(KEY, "wither_on_hit", "hits", 10))) {
            this.apophisHitCount.put(attackerId, 0);
            Location tAnchor = target.getLocation();
            Bukkit.getRegionScheduler().run(this.plugin, tAnchor, task -> {
               target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, witherTicks, 0, false, false));
               target.getWorld().playSound(target.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 1.0F, 1.0F);
            });
         } else {
            this.apophisHitCount.put(attackerId, hits);
         }
      }
   }
}
