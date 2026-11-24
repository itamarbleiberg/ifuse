package me.poggercity.infuse.Infuses;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import me.poggercity.infuse.Managers.CooldownManager;
import me.poggercity.infuse.Managers.Infuse;
import me.poggercity.infuse.Managers.Settings;
import io.papermc.paper.datacomponent.DataComponentTypes;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentOffer;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.ThrownExpBottle;
import org.bukkit.entity.WindCharge;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class Emerald implements Listener {
   public static final String KEY = "emerald";
   private static final String GEM_NAME = ChatColor.GREEN + "Emerald Effect";
   private static final String EMERALD_KEY = "eme";
   private final Plugin plugin;
   private final Random random = new Random();

   public Emerald(Plugin plugin) {
      this.plugin = plugin;
   }

   // Luck and Hero of the Village while equipped. Looting is handled by ToolBonuses: it used to be written onto the
   // sword in hand and stayed there forever.
   public void applyEmeraldEffects() {
      boolean luck = Settings.passive(KEY, "luck");
      boolean hero = Settings.passive(KEY, "hero_of_the_village");
      if (luck || hero) {
         int luckAmp = Math.max(1, Settings.intValue(KEY, "luck", "level", 10)) - 1;
         int heroAmp = Math.max(1, Settings.intValue(KEY, "hero_of_the_village", "level", 3)) - 1;

         for (Player player : Bukkit.getOnlinePlayers()) {
            if (this.hasImmortalHackEquipped(player, "1") || this.hasImmortalHackEquipped(player, "2")) {
               Bukkit.getRegionScheduler().run(Infuse.getInstance(), player.getLocation(), task -> {
                  if (luck) {
                     player.addPotionEffect(new PotionEffect(PotionEffectType.LUCK, 40, luckAmp, false, false, false));
                  }

                  if (hero) {
                     player.addPotionEffect(new PotionEffect(PotionEffectType.HERO_OF_THE_VILLAGE, 40, heroAmp, false, false, false));
                  }
               });
            }
         }
      }
   }

   private static boolean sparkActive(Player player) {
      PotionEffect heroEffect = player.getPotionEffect(PotionEffectType.HERO_OF_THE_VILLAGE);
      return heroEffect != null && heroEffect.getAmplifier() >= 200;
   }

   private boolean hasImmortalHackEquipped(Player player, String tier) {
      String currentHack = Infuse.getInstance().getHackManager().getHack(player.getUniqueId(), tier);
      return currentHack != null && (currentHack.equalsIgnoreCase(ChatColor.stripColor(GEM_NAME)) || currentHack.equals("Augmented Emerald Effect"));
   }

   private boolean isSword(ItemStack item) {
      if (item == null) {
         return false;
      } else {
         Material type = item.getType();
         return type == Material.WOODEN_SWORD
            || type == Material.STONE_SWORD
            || type == Material.IRON_SWORD
            || type == Material.GOLDEN_SWORD
            || type == Material.DIAMOND_SWORD
            || type == Material.NETHERITE_SWORD;
      }
   }

   @EventHandler
   public void onPlayerExpChange(PlayerExpChangeEvent event) {
      Player player = event.getPlayer();
      if ((this.hasImmortalHackEquipped(player, "1") || this.hasImmortalHackEquipped(player, "2")) && Settings.passive(KEY, "exp_boost")) {
         double multiplier = sparkActive(player)
            ? Settings.value(KEY, "exp_boost", "spark_multiplier", 3.0)
            : Settings.value(KEY, "exp_boost", "multiplier", 1.5);
         event.setAmount((int)(event.getAmount() * Math.max(0.0, multiplier)));
      }
   }

   @EventHandler
   public void onPrepareItemEnchant(PrepareItemEnchantEvent event) {
      Player player = event.getEnchanter();
      ItemStack item = event.getItem();
      if ((this.hasImmortalHackEquipped(player, "1") || this.hasImmortalHackEquipped(player, "2")) && Settings.passive(KEY, "enchanting_table")) {
         try {
            event.getClass().getMethod("setEnchantmentBonus", int.class).invoke(event, 15);
         } catch (Exception var8) {
         }

         Random seeded = new Random(player.getEnchantmentSeed());
         EnchantmentOffer[] offers = event.getOffers();

         for (int i = 0; i < offers.length; i++) {
            if (offers[i] != null) {
               Enchantment randomEnchantment = this.getCompatibleRandomEnchantment(item, seeded);
               if (randomEnchantment != null) {
                  offers[i].setEnchantment(randomEnchantment);
                  offers[i].setCost(30);
                  offers[i].setEnchantmentLevel(seeded.nextInt(21) + 10);
               }
            }
         }
      }
   }

   private Enchantment getCompatibleRandomEnchantment(ItemStack item, Random random) {
      List<Enchantment> validEnchantments = new ArrayList<>();

      for (Enchantment enchantment : Enchantment.values()) {
         if (enchantment.canEnchantItem(item)) {
            validEnchantments.add(enchantment);
         }
      }

      return validEnchantments.isEmpty() ? null : validEnchantments.get(random.nextInt(validEnchantments.size()));
   }

   @EventHandler
   public void onPlayerConsume(PlayerItemConsumeEvent event) {
      if (event.getHand() == EquipmentSlot.HAND) {
         Player player = event.getPlayer();
         ItemStack consumedItem = event.getItem();
         int originalCount = consumedItem.getAmount();
         if ((this.hasImmortalHackEquipped(player, "1") || this.hasImmortalHackEquipped(player, "2")) && Settings.passive(KEY, "consumable_refund")) {
            if (consumedItem.getType() == Material.POTION) {
               return;
            }

            // Stackable items that leave something behind (honey bottles) would duplicate the leftover bottle.
            if (consumedItem.getMaxStackSize() > 1 && consumedItem.hasData(DataComponentTypes.USE_REMAINDER)) {
               return;
            }

            double chance = sparkActive(player)
               ? Settings.chance(KEY, "consumable_refund", "spark_chance", 50.0)
               : Settings.chance(KEY, "consumable_refund", "chance", 25.0);
            if (Math.random() < chance) {
               // The item stays exactly as it was. This used to put one extra item into the stack before eating it,
               // so milk buckets and stews also handed out their empty bucket/bowl: a free bucket on every refund.
               ItemStack keep = consumedItem.clone();
               keep.setAmount(originalCount);
               event.setReplacement(keep);
               Location anchor = player.getLocation();
               int count = Settings.particles(KEY, 12);
               Bukkit.getRegionScheduler().runDelayed(this.plugin, anchor, task -> {
                  player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 1.0F);
                  if (count > 0) {
                     player.getWorld().spawnParticle(Particle.EGG_CRACK, player.getLocation(), count, 1.5, 0.5, 0.5, 0.01);
                  }
               }, 1L);
            }
         }
      }
   }

   // BUG FIX (wind charge dupe): the refund used to be rolled in PlayerInteractEvent, i.e. on every right-click while
   // holding a wind charge or XP bottle. Right-clicking a container (chest, barrel, ...) fires that event but opens
   // the container instead of throwing, so nothing was consumed and the refund was free, as often as you clicked.
   // The refund is now rolled only when the item is really thrown, which is when its projectile launches.
   // MONITOR + ignoreCancelled: no refund if another plugin (claims, regions) cancels the throw.
   @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
   public void onConsumableProjectileLaunch(ProjectileLaunchEvent event) {
      Projectile projectile = event.getEntity();
      if (projectile.getShooter() instanceof Player player) {
         Material thrownItem;
         if (projectile instanceof WindCharge) {
            thrownItem = Material.WIND_CHARGE;
         } else {
            if (!(projectile instanceof ThrownExpBottle)) {
               return;
            }

            thrownItem = Material.EXPERIENCE_BOTTLE;
         }

         // Main hand only, as before. The item is still in the hand here: vanilla consumes it right after the launch.
         if (player.getInventory().getItemInMainHand().getType() == thrownItem
            && Settings.passive(KEY, "throwable_refund")
            && (this.hasImmortalHackEquipped(player, "1") || this.hasImmortalHackEquipped(player, "2"))) {
            double chance = sparkActive(player)
               ? Settings.chance(KEY, "throwable_refund", "spark_chance", 50.0)
               : Settings.chance(KEY, "throwable_refund", "chance", 25.0);

            if (Math.random() < chance) {
               // One tick later, after vanilla has removed the thrown item from the stack.
               Location anchor = player.getLocation();
               Bukkit.getRegionScheduler().runDelayed(this.plugin, anchor, task -> {
                  player.getInventory().addItem(new ItemStack[]{new ItemStack(thrownItem)});
                  player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 1.0F);
                  int clouds = Settings.particles(KEY, 8);
                  if (thrownItem == Material.WIND_CHARGE && clouds > 0) {
                     player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), clouds, 0.5, 0.5, 0.5, 0.02);
                  }
               }, 1L);
            }
         }
      }
   }

   public void activateSpark(Player player) {
      UUID playerUUID = player.getUniqueId();
      if (!CooldownManager.isOnCooldown(playerUUID, "eme")) {
         int duration = Infuse.getInstance().getConfig().getInt("durations.emerald_spark", 10);
         int configcooldown = Infuse.getInstance().getConfig().getInt("cooldowns.emerald_spark", 30);
         Location anchor = player.getLocation();
         Bukkit.getRegionScheduler()
            .run(Infuse.getInstance(), anchor, task -> player.addPotionEffect(new PotionEffect(PotionEffectType.HERO_OF_THE_VILLAGE, duration * 20, 254)));
         CooldownManager.setDuration(playerUUID, "eme", duration);
         int happy = Settings.particles(KEY, 15);
         if (happy > 0) {
            player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation().add(0.0, 1.0, 0.0), happy, 0.5, 0.5, 0.5, 0.1);
         }
         player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_CELEBRATE, 1.0F, 1.0F);
         boolean isAugmented = Infuse.getInstance().getHackManager().getHack(playerUUID, "1") != null
               && ChatColor.stripColor(Infuse.getInstance().getHackManager().getHack(playerUUID, "1")).toLowerCase().contains("augmented emerald")
            || Infuse.getInstance().getHackManager().getHack(playerUUID, "2") != null
               && ChatColor.stripColor(Infuse.getInstance().getHackManager().getHack(playerUUID, "2")).toLowerCase().contains("augmented emerald");
         int cooldown = isAugmented ? configcooldown / 2 + duration : configcooldown + duration;
         CooldownManager.setCooldown(playerUUID, "eme", cooldown);
      }
   }

   public static boolean isInvincibilityGem(ItemStack item) {
      return item != null
         && item.getType() == Material.POTION
         && item.hasItemMeta()
         && item.getItemMeta().hasCustomModelData()
         && item.getItemMeta().getCustomModelData() == 1;
   }
}
