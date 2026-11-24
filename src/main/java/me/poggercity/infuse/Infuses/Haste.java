package me.poggercity.infuse.Infuses;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
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
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class Haste implements Listener {
   private static final String GEM_NAME = ChatColor.GOLD + "Haste Effect";
   private static final String IMMORTAL_KEY = "haste";
   private final PlayerHackManager hackManager = new PlayerHackManager();
   private static final Set<Material> VALID_TOOLS = EnumSet.of(
      Material.WOODEN_PICKAXE,
      Material.STONE_PICKAXE,
      Material.IRON_PICKAXE,
      Material.GOLDEN_PICKAXE,
      Material.DIAMOND_PICKAXE,
      Material.NETHERITE_PICKAXE,
      Material.WOODEN_AXE,
      Material.STONE_AXE,
      Material.IRON_AXE,
      Material.GOLDEN_AXE,
      Material.DIAMOND_AXE,
      Material.NETHERITE_AXE,
      Material.WOODEN_HOE,
      Material.STONE_HOE,
      Material.IRON_HOE,
      Material.GOLDEN_HOE,
      Material.DIAMOND_HOE,
      Material.NETHERITE_HOE,
      Material.WOODEN_SHOVEL,
      Material.STONE_SHOVEL,
      Material.IRON_SHOVEL,
      Material.GOLDEN_SHOVEL,
      Material.DIAMOND_SHOVEL,
      Material.NETHERITE_SHOVEL
   );
   private static final Map<Enchantment, Integer> ENCHANTMENTS = Map.of(Enchantment.FORTUNE, 5, Enchantment.EFFICIENCY, 10, Enchantment.UNBREAKING, 5);

   public void applyHasteEffects() {
      Bukkit.getOnlinePlayers().stream().filter(this::hasImmortalHackEquipped).forEach(this::enchantItemIfApplicable);
      Bukkit.getOnlinePlayers().stream().filter(this::hasImmortalHackEquipped).forEach(this::sigma);
   }

   private void sigma(Player player) {
      Bukkit.getRegionScheduler()
         .run(Infuse.getInstance(), player.getLocation(), task -> player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 40, 1, false, false, false)));
   }

   @EventHandler
   public void onBlockBreak(BlockBreakEvent event) {
      Player player = event.getPlayer();
      if (this.hasImmortalHackEquipped(player) && player.isSneaking()) {
         ItemStack item = player.getInventory().getItemInMainHand();
         if (item.getType().name().endsWith("_PICKAXE")) {
            Material oreType = event.getBlock().getType();
            Set<Material> ores = EnumSet.of(
               Material.COAL_ORE,
               Material.DEEPSLATE_COAL_ORE,
               Material.IRON_ORE,
               Material.DEEPSLATE_IRON_ORE,
               Material.GOLD_ORE,
               Material.DEEPSLATE_GOLD_ORE,
               Material.COPPER_ORE,
               Material.DEEPSLATE_COPPER_ORE,
               Material.LAPIS_ORE,
               Material.DEEPSLATE_LAPIS_ORE,
               Material.REDSTONE_ORE,
               Material.DEEPSLATE_REDSTONE_ORE,
               Material.DIAMOND_ORE,
               Material.DEEPSLATE_DIAMOND_ORE,
               Material.EMERALD_ORE,
               Material.DEEPSLATE_EMERALD_ORE,
               Material.NETHER_QUARTZ_ORE,
               Material.NETHER_GOLD_ORE,
               Material.ANCIENT_DEBRIS
            );
            if (ores.contains(oreType)) {
               boolean hasSilkTouch = item.containsEnchantment(Enchantment.SILK_TOUCH);
               int fortuneLevel = item.getEnchantmentLevel(Enchantment.FORTUNE);
               Set<Block> minedBlocks = new HashSet<>();
               this.veinMineOre(event.getBlock(), player, oreType, minedBlocks, hasSilkTouch, fortuneLevel);
            }
         }
      }
   }

   private void veinMineOre(Block block, Player player, Material oreType, Set<Block> minedBlocks, boolean hasSilkTouch, int fortuneLevel) {
      if (minedBlocks.add(block)) {
         Collection<ItemStack> drops;
         if (hasSilkTouch) {
            drops = Collections.singletonList(new ItemStack(oreType, 1));
         } else {
            drops = block.getDrops(player.getInventory().getItemInMainHand());
            if (fortuneLevel > 0 && oreType != Material.ANCIENT_DEBRIS) {
               List<ItemStack> modifiedDrops = new ArrayList<>();

               for (ItemStack drop : drops) {
                  int amount = drop.getAmount() + ThreadLocalRandom.current().nextInt(1, fortuneLevel + 2);
                  modifiedDrops.add(new ItemStack(drop.getType(), amount));
               }

               drops = modifiedDrops;
            }
         }

         block.getWorld().playSound(block.getLocation(), Sound.BLOCK_STONE_BREAK, 1.0F, 1.0F);
         drops.forEach(dropx -> block.getWorld().dropItemNaturally(block.getLocation(), dropx));
         block.setType(Material.AIR);

         for (BlockFace face : BlockFace.values()) {
            Block adjacent = block.getRelative(face);
            if (adjacent.getType() == oreType) {
               this.veinMineOre(adjacent, player, oreType, minedBlocks, hasSilkTouch, fortuneLevel);
            }
         }
      }
   }

   private boolean hasImmortalHackEquipped(Player player) {
      String hack1 = Infuse.getInstance().getHackManager().getHack(player.getUniqueId(), "1");
      String hack2 = Infuse.getInstance().getHackManager().getHack(player.getUniqueId(), "2");
      hack1 = hack1 != null ? ChatColor.stripColor(hack1) : null;
      hack2 = hack2 != null ? ChatColor.stripColor(hack2) : null;
      return hack1 != null && (hack1.equalsIgnoreCase(ChatColor.stripColor(GEM_NAME)) || hack1.equalsIgnoreCase("Augmented Haste Effect"))
         || hack2 != null && (hack2.equalsIgnoreCase(ChatColor.stripColor(GEM_NAME)) || hack2.equalsIgnoreCase("Augmented Haste Effect"));
   }

   private void enchantItemIfApplicable(Player player) {
      if (Infuse.getInstance().getConfig().getBoolean("configs.haste_enchant")) {
         ItemStack item = player.getInventory().getItemInMainHand();
         if (item != null && item.getType() != Material.AIR && VALID_TOOLS.contains(item.getType())) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
               ENCHANTMENTS.forEach((enchant, level) -> meta.addEnchant(enchant, level, true));
               item.setItemMeta(meta);
            }
         }
      }
   }

   public void activateSpark(Player player) {
      UUID playerUUID = player.getUniqueId();
      if (!CooldownManager.isOnCooldown(playerUUID, "haste")) {
         player.getWorld().spawnParticle(Particle.CRIT, player.getLocation().add(0.0, 1.0, 0.0), 20, 0.5, 0.5, 0.5, 0.1);
         player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1.0F, 1.5F);
         int duration = Infuse.getInstance().getConfig().getInt("durations.haste_spark", 15);
         Location anchor = player.getLocation();
         Bukkit.getRegionScheduler()
            .run(Infuse.getInstance(), anchor, task -> player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, duration * 20, 3, false, false, false)));
         CooldownManager.setDuration(playerUUID, "haste", duration);
         int configcooldown = Infuse.getInstance().getConfig().getInt("cooldowns.haste_spark", 60);
         boolean isAugmented = Infuse.getInstance().getHackManager().getHack(playerUUID, "1") != null
               && ChatColor.stripColor(Infuse.getInstance().getHackManager().getHack(playerUUID, "1")).toLowerCase().contains("augmented haste")
            || Infuse.getInstance().getHackManager().getHack(playerUUID, "2") != null
               && ChatColor.stripColor(Infuse.getInstance().getHackManager().getHack(playerUUID, "2")).toLowerCase().contains("augmented haste");
         int cooldown = isAugmented ? configcooldown / 2 + duration : configcooldown + duration;
         CooldownManager.setCooldown(playerUUID, "haste", cooldown);
      }
   }

   @EventHandler
   public void onEntityDamageByEntity2(EntityDamageByEntityEvent event) {
      if (event.getEntity() instanceof Player player) {
         ItemStack offHand = player.getInventory().getItemInOffHand();
         if (offHand.getType() == Material.SHIELD
            && player.isBlocking()
            && this.hasImmortalHackEquipped(player)
            && event.getDamager() instanceof Player attacker
            && attacker.getInventory().getItemInMainHand().getType().toString().endsWith("_AXE")) {
            player.getWorld().playSound(player.getLocation(), Sound.ITEM_SHIELD_BREAK, 1.0F, 1.0F);
            Location anchor = player.getLocation();
            Bukkit.getRegionScheduler().runDelayed(Infuse.getInstance(), anchor, task -> this.stunShield(player), 20L);
         }
      }
   }

   private void stunShield(Player player) {
      player.setCooldown(Material.SHIELD, 50);
   }

   public static boolean isInvincibilityGem(ItemStack item) {
      return item != null
         && item.getType() == Material.POTION
         && item.hasItemMeta()
         && item.getItemMeta().hasCustomModelData()
         && item.getItemMeta().getCustomModelData() == 5;
   }
}
