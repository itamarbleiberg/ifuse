package me.poggercity.infuse.Infuses;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import me.poggercity.infuse.Managers.CooldownManager;
import me.poggercity.infuse.Managers.Infuse;
import me.poggercity.infuse.Managers.Settings;
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
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class Haste implements Listener {
   public static final String KEY = "haste";
   private static final String GEM_NAME = ChatColor.GOLD + "Haste Effect";
   private static final Set<Material> ORES = EnumSet.of(
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
   private static final BlockFace[] NEIGHBOURS = new BlockFace[]{
      BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN
   };
   // Players whose vein mine is running, so the block breaks it causes don't start another vein mine.
   private final Set<UUID> veinMining = new HashSet<>();

   // Haste II while equipped, and the tool enchantments.
   public void applyHasteEffects() {
      boolean hasteEffect = Settings.passive(KEY, "haste_effect");
      boolean toolEnchants = Settings.passive(KEY, "tool_enchants");
      int amplifier = Math.max(1, Settings.intValue(KEY, "haste_effect", "level", 2)) - 1;

      for (Player player : Bukkit.getOnlinePlayers()) {
         if (this.hasImmortalHackEquipped(player)) {
            if (hasteEffect) {
               Bukkit.getRegionScheduler()
                  .run(Infuse.getInstance(), player.getLocation(), task -> player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 40, amplifier, false, false, false)));
            }

            if (toolEnchants && !Settings.worldDisabled(player.getWorld())) {
               enchantTool(player.getInventory().getItemInMainHand());
            }
         }
      }
   }

   private static boolean isTool(Material type) {
      String n = type.name();
      return !type.isLegacy() && (n.endsWith("_PICKAXE") || n.endsWith("_AXE") || n.endsWith("_SHOVEL") || n.endsWith("_HOE"));
   }

   /**
    * The pickaxe, axe, shovel or hoe a Haste player holds gets Efficiency, Fortune and Unbreaking as real enchantments
    * (shown on the item). The tool keeps them for good, also after Haste is gone and when it is given to another
    * player, as in Infuse 2.4. Levels are only raised: a tool that already has a higher level keeps it. Returns true
    * if the item was changed.
    */
   public static boolean enchantTool(ItemStack item) {
      if (item == null || !isTool(item.getType())) {
         return false;
      } else {
         ItemMeta meta = item.getItemMeta();
         if (meta == null) {
            return false;
         } else {
            boolean changed = false;
            Object[][] levels = new Object[][]{
               {Enchantment.EFFICIENCY, Settings.intValue(KEY, "tool_enchants", "efficiency", 10)},
               {Enchantment.FORTUNE, Settings.intValue(KEY, "tool_enchants", "fortune", 5)},
               {Enchantment.UNBREAKING, Settings.intValue(KEY, "tool_enchants", "unbreaking", 5)}
            };

            for (Object[] entry : levels) {
               Enchantment enchantment = (Enchantment)entry[0];
               int level = Math.min(255, (Integer)entry[1]);
               if (level > 0 && meta.getEnchantLevel(enchantment) < level) {
                  meta.addEnchant(enchantment, level, true);
                  changed = true;
               }
            }

            if (changed) {
               item.setItemMeta(meta);
            }

            return changed;
         }
      }
   }

   // Vein miner (sneak + pickaxe on an ore). Every extra block is broken with Player#breakBlock, so it uses the
   // pickaxe, drops like a normal break and is refused in places where the player isn't allowed to break blocks
   // (land claims). It used to remove blocks directly, ignoring claims, had no size limit and recursed once per block.
   @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
   public void onBlockBreak(BlockBreakEvent event) {
      Player player = event.getPlayer();
      UUID id = player.getUniqueId();
      if (!this.veinMining.contains(id) && player.isSneaking() && Settings.passive(KEY, "vein_miner") && this.hasImmortalHackEquipped(player)) {
         ItemStack item = player.getInventory().getItemInMainHand();
         Material oreType = event.getBlock().getType();
         if (item.getType().name().endsWith("_PICKAXE") && ORES.contains(oreType)) {
            int maxBlocks = Math.max(1, Settings.intValue(KEY, "vein_miner", "max_blocks", 64));
            Block start = event.getBlock();
            Bukkit.getScheduler().runTask(Infuse.getInstance(), () -> this.veinMine(player, start, oreType, maxBlocks));
         }
      }
   }

   private void veinMine(Player player, Block start, Material oreType, int maxBlocks) {
      UUID id = player.getUniqueId();
      if (player.isOnline()) {
         this.veinMining.add(id);

         try {
            Set<Block> seen = new HashSet<>();
            Deque<Block> queue = new ArrayDeque<>();
            seen.add(start);
            queue.add(start);
            int broken = 1;

            while (!queue.isEmpty() && broken < maxBlocks) {
               Block current = queue.poll();

               for (BlockFace face : NEIGHBOURS) {
                  Block next = current.getRelative(face);
                  if (broken < maxBlocks && next.getType() == oreType && seen.add(next)) {
                     ItemStack tool = player.getInventory().getItemInMainHand();
                     if (!tool.getType().name().endsWith("_PICKAXE") || player.getLocation().distanceSquared(next.getLocation()) > 400.0) {
                        return;
                     }

                     if (player.breakBlock(next)) {
                        broken++;
                        queue.add(next);
                     }
                  }
               }
            }
         } finally {
            this.veinMining.remove(id);
         }
      }
   }

   public boolean hasImmortalHackEquipped(Player player) {
      String hack1 = Infuse.getInstance().getHackManager().getHack(player.getUniqueId(), "1");
      String hack2 = Infuse.getInstance().getHackManager().getHack(player.getUniqueId(), "2");
      hack1 = hack1 != null ? ChatColor.stripColor(hack1) : null;
      hack2 = hack2 != null ? ChatColor.stripColor(hack2) : null;
      return hack1 != null && (hack1.equalsIgnoreCase(ChatColor.stripColor(GEM_NAME)) || hack1.equalsIgnoreCase("Augmented Haste Effect"))
         || hack2 != null && (hack2.equalsIgnoreCase(ChatColor.stripColor(GEM_NAME)) || hack2.equalsIgnoreCase("Augmented Haste Effect"))
         || WinnersCurse.borrowsPassive(player, KEY);
   }

   public void activateSpark(Player player) {
      UUID playerUUID = player.getUniqueId();
      if (!CooldownManager.isOnCooldown(playerUUID, "haste")) {
         int crit = Settings.particles(KEY, 20);
         if (crit > 0) {
            player.getWorld().spawnParticle(Particle.CRIT, player.getLocation().add(0.0, 1.0, 0.0), crit, 0.5, 0.5, 0.5, 0.1);
         }

         player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1.0F, 1.5F);
         int duration = Infuse.getInstance().getConfig().getInt("effects.haste.spark.duration", 15);
         Location anchor = player.getLocation();
         Bukkit.getRegionScheduler()
            .run(Infuse.getInstance(), anchor, task -> player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, duration * 20, 3, false, false, false)));
         CooldownManager.setDuration(playerUUID, "haste", duration);
         int configcooldown = Infuse.getInstance().getConfig().getInt("effects.haste.spark.cooldown", 60);
         boolean isAugmented = Infuse.getInstance().getHackManager().getHack(playerUUID, "1") != null
               && ChatColor.stripColor(Infuse.getInstance().getHackManager().getHack(playerUUID, "1")).toLowerCase().contains("augmented haste")
            || Infuse.getInstance().getHackManager().getHack(playerUUID, "2") != null
               && ChatColor.stripColor(Infuse.getInstance().getHackManager().getHack(playerUUID, "2")).toLowerCase().contains("augmented haste");
         int cooldown = isAugmented ? configcooldown / 2 + duration : configcooldown + duration;
         CooldownManager.setCooldown(playerUUID, "haste", cooldown);
      }
   }

   // Shield cooldown is shorter (default 2.5 s instead of 5 s) when an axe disables a Haste player's shield.
   @EventHandler
   public void onEntityDamageByEntity2(EntityDamageByEntityEvent event) {
      if (event.getEntity() instanceof Player player) {
         ItemStack offHand = player.getInventory().getItemInOffHand();
         if (offHand.getType() == Material.SHIELD
            && player.isBlocking()
            && Settings.passive(KEY, "shield_cooldown")
            && this.hasImmortalHackEquipped(player)
            && event.getDamager() instanceof Player attacker
            && attacker.getInventory().getItemInMainHand().getType().toString().endsWith("_AXE")) {
            player.getWorld().playSound(player.getLocation(), Sound.ITEM_SHIELD_BREAK, 1.0F, 1.0F);
            int ticks = (int)Math.round(Math.max(0.0, Settings.value(KEY, "shield_cooldown", "seconds", 2.5)) * 20.0);
            Location anchor = player.getLocation();
            Bukkit.getRegionScheduler().runDelayed(Infuse.getInstance(), anchor, task -> player.setCooldown(Material.SHIELD, ticks), 20L);
         }
      }
   }

   public static boolean isInvincibilityGem(ItemStack item) {
      return item != null
         && item.getType() == Material.POTION
         && item.hasItemMeta()
         && item.getItemMeta().hasCustomModelData()
         && item.getItemMeta().getCustomModelData() == 5;
   }
}
