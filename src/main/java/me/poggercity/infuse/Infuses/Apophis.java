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
import me.poggercity.infuse.Managers.PlayerHackManager;
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
   private static final String GEM_NAME = HexColorTranslator.translateToMinecraftColor("#301145") + "Apophis Effect";
   private static final String APOPHIS_KEY = "apophis";
   private final PlayerHackManager hackManager = new PlayerHackManager();
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
                     player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 40, 0, false, false, false));
                     player.addPotionEffects(
                        Arrays.asList(
                           new PotionEffect(PotionEffectType.CONDUIT_POWER, 40, 0, false, false, false),
                           new PotionEffect(PotionEffectType.DOLPHINS_GRACE, 40, 0, false, false, false)
                        )
                     );
                     player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 40, 0, false, false, false));
                     player.addPotionEffect(new PotionEffect(PotionEffectType.LUCK, 40, 0, false, false, false));
                     if (Infuse.getInstance().getConfig().getBoolean("configs.haste_enchant", true)) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 40, 1, false, false, false));
                     }

                     this.handleLavaFlight(player);
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
         Location anchor = player.getLocation();
         Bukkit.getRegionScheduler().run(Infuse.getInstance(), anchor, task -> {
            double base = player.getAttribute(Attribute.MAX_HEALTH).getBaseValue();
            if (base < 40.0) {
               double next = Math.min(base + 20.0, 40.0);
               player.getAttribute(Attribute.MAX_HEALTH).setBaseValue(next);
               player.setHealth(Math.min(player.getHealth() + 20.0, player.getAttribute(Attribute.MAX_HEALTH).getValue()));
            }
         });
         CooldownManager.setDuration(uuid, "st", duration);
         if (Infuse.getInstance().getConfig().getBoolean("configs.emerald_enchant", true)) {
            ItemStack main = player.getInventory().getItemInMainHand();
            if (main != null && main.getType() != Material.AIR) {
               String t = main.getType().toString();
               if (t.contains("PICKAXE") || t.contains("SHOVEL") || t.contains("AXE")) {
                  main.addUnsafeEnchantment(Enchantment.EFFICIENCY, 10);
                  main.addUnsafeEnchantment(Enchantment.FORTUNE, 5);
               }

               if (t.contains("SWORD") || t.contains("AXE")) {
                  main.addUnsafeEnchantment(Enchantment.SHARPNESS, 5);
                  main.addUnsafeEnchantment(Enchantment.LOOTING, 5);
               }
            }
         }

         boolean aoeTicks = true;
         double radius = 5.0;

         for (Entity e : player.getNearbyEntities(5.0, 5.0, 5.0)) {
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
         new Fire(Infuse.getInstance()).activateSpark(player);
         CooldownManager.setDuration(uuid, "apophis", duration);
         int baseCd = Infuse.getInstance().getConfig().getInt("cooldowns.apophis_spark", 90);
         boolean augmented = this.isAugmentedApophis(player);
         int finalCd = augmented ? baseCd / 2 + duration : baseCd + duration;
         CooldownManager.setCooldown(uuid, "apophis", finalCd);
         Bukkit.getRegionScheduler().runDelayed(Infuse.getInstance(), anchor, task -> {
            if (player.isOnline()) {
               player.getAttribute(Attribute.MAX_HEALTH).setBaseValue(20.0);
               if (player.getHealth() > 20.0) {
                  player.setHealth(20.0);
               }

               ItemStack main = player.getInventory().getItemInMainHand();
               if (main != null && main.getType() != Material.AIR) {
                  if (main.getEnchantmentLevel(Enchantment.EFFICIENCY) > 5) {
                     main.removeEnchantment(Enchantment.EFFICIENCY);
                  }

                  if (main.getEnchantmentLevel(Enchantment.FORTUNE) > 3) {
                     main.removeEnchantment(Enchantment.FORTUNE);
                  }

                  if (main.getEnchantmentLevel(Enchantment.SHARPNESS) > 5) {
                     main.removeEnchantment(Enchantment.SHARPNESS);
                  }

                  if (main.getEnchantmentLevel(Enchantment.LOOTING) > 3) {
                     main.removeEnchantment(Enchantment.LOOTING);
                  }
               }
            }
         }, duration * 20L);
      }
   }

   @EventHandler
   public void onPlayerHit(EntityDamageByEntityEvent event) {
      if (event.getDamager() instanceof Player attacker
         && event.getEntity() instanceof Player target
         && this.hasApophisEquipped(attacker)
         && !Infuse.getInstance().getTrustManager().isTrusted(attacker, target)) {
         UUID attackerId = attacker.getUniqueId();
         int hits = this.apophisHitCount.getOrDefault(attackerId, 0) + 1;
         if (hits >= 10) {
            this.apophisHitCount.put(attackerId, 0);
            Location tAnchor = target.getLocation();
            Bukkit.getRegionScheduler().run(this.plugin, tAnchor, task -> {
               target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 100, 0, false, false));
               target.getWorld().playSound(target.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 1.0F, 1.0F);
            });
         } else {
            this.apophisHitCount.put(attackerId, hits);
         }
      }
   }
}
