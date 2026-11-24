package me.poggercity.infuse.Infuses;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import me.poggercity.infuse.Managers.AttributeBonus;
import me.poggercity.infuse.Managers.CooldownManager;
import me.poggercity.infuse.Managers.Infuse;
import me.poggercity.infuse.Managers.Settings;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

/**
 * Enchantment bonuses from passives: Haste (Efficiency 10, Fortune 5, Unbreaking 5 on tools), Emerald (Looting 5 on
 * swords) and the Apophis spark (Efficiency/Fortune on tools, Sharpness/Looting on swords and axes).
 *
 * Infuse 2.4 wrote these enchantments onto the held item. They stayed on the item forever: after the effect was
 * drained, after the item was given or sold to another player, and Apophis even stripped enchantments the item had
 * before. The bonuses are now applied only while the player has the effect, without changing the item:
 * Efficiency is a mining-efficiency attribute bonus, Fortune and Unbreaking are applied when the block drops or the
 * item takes damage, Sharpness adds damage on hit. Looting is the one enchantment the game only reads from the item
 * itself, so it is put on the weapon only for the killing hit and taken off again right after the loot is rolled.
 */
public class ToolBonuses implements Listener {
   private static final Set<Material> MINING_TOOLS = EnumSet.noneOf(Material.class);
   private static final Set<Material> FORTUNE_BLOCKS = EnumSet.of(
      Material.NETHER_QUARTZ_ORE,
      Material.GRAVEL,
      Material.GLOWSTONE,
      Material.MELON,
      Material.SEA_LANTERN,
      Material.AMETHYST_CLUSTER,
      Material.WHEAT,
      Material.CARROTS,
      Material.POTATOES,
      Material.BEETROOTS,
      Material.NETHER_WART,
      Material.SHORT_GRASS,
      Material.TALL_GRASS,
      Material.FERN,
      Material.LARGE_FERN
   );
   private final Plugin plugin;
   private final NamespacedKey efficiencyKey;
   private final NamespacedKey tempLootingKey;
   // Players holding an item with temporary Looting (normally empty, at most for one tick)
   private final Set<UUID> pendingLooting = new HashSet<>();

   static {
      for (Material m : Material.values()) {
         String n = m.name();
         if (!m.isLegacy() && (n.endsWith("_PICKAXE") || n.endsWith("_AXE") || n.endsWith("_SHOVEL") || n.endsWith("_HOE"))) {
            MINING_TOOLS.add(m);
         }
      }
   }

   public ToolBonuses(Plugin plugin) {
      this.plugin = plugin;
      this.efficiencyKey = new NamespacedKey(plugin, "tool_bonus_efficiency");
      this.tempLootingKey = new NamespacedKey(plugin, "temp_looting");
   }

   private static boolean hasEffect(Player player, String name) {
      for (String slot : new String[]{"1", "2"}) {
         String hack = Infuse.getInstance().getHackManager().getHack(player.getUniqueId(), slot);
         if (hack != null) {
            String stripped = ChatColor.stripColor(hack);
            if (stripped.equalsIgnoreCase(name + " Effect") || stripped.equalsIgnoreCase("Augmented " + name + " Effect")) {
               return true;
            }
         }
      }

      return false;
   }

   private static boolean worldDisabled(Player player) {
      return Infuse.getInstance().getConfig().getStringList("configs.disabled_worlds").stream().anyMatch(w -> w.equalsIgnoreCase(player.getWorld().getName()));
   }

   private static boolean isSword(Material m) {
      return Tag.ITEMS_SWORDS.isTagged(m);
   }

   private static boolean isAxe(Material m) {
      return m.name().endsWith("_AXE");
   }

   private static boolean isMiningTool(Material m) {
      return MINING_TOOLS.contains(m);
   }

   private static boolean apophisSpark(Player player) {
      return Infuse.getInstance().isApophisEnabled()
         && CooldownManager.isEffectActive(player.getUniqueId(), "apophis")
         && Settings.passive("apophis", "spark_tool_bonus");
   }

   /** The enchantment level the player's passives give this item (0 = none). */
   public int bonusLevel(Player player, ItemStack item, Enchantment enchantment) {
      if (item == null || item.getType().isAir() || worldDisabled(player)) {
         return 0;
      } else {
         Material type = item.getType();
         int level = 0;
         if (isMiningTool(type) && hasEffect(player, "Haste") && Settings.passive("haste", "tool_enchants")) {
            if (enchantment.equals(Enchantment.EFFICIENCY)) {
               level = Math.max(level, Settings.intValue("haste", "tool_enchants", "efficiency", 10));
            } else if (enchantment.equals(Enchantment.FORTUNE)) {
               level = Math.max(level, Settings.intValue("haste", "tool_enchants", "fortune", 5));
            } else if (enchantment.equals(Enchantment.UNBREAKING)) {
               level = Math.max(level, Settings.intValue("haste", "tool_enchants", "unbreaking", 5));
            }
         }

         if (isSword(type) && enchantment.equals(Enchantment.LOOTING) && hasEffect(player, "Emerald") && Settings.passive("emerald", "looting")) {
            level = Math.max(level, Settings.intValue("emerald", "looting", "level", 5));
         }

         if (apophisSpark(player)) {
            boolean tool = type.name().endsWith("_PICKAXE") || type.name().endsWith("_SHOVEL") || isAxe(type);
            boolean weapon = isSword(type) || isAxe(type);
            if (tool && enchantment.equals(Enchantment.EFFICIENCY)) {
               level = Math.max(level, 10);
            } else if (tool && enchantment.equals(Enchantment.FORTUNE)) {
               level = Math.max(level, 5);
            } else if (weapon && enchantment.equals(Enchantment.SHARPNESS)) {
               level = Math.max(level, 5);
            } else if (weapon && enchantment.equals(Enchantment.LOOTING)) {
               level = Math.max(level, 5);
            }
         }

         return Math.max(0, level);
      }
   }

   private int itemLevel(ItemStack item, Enchantment enchantment) {
      if (item == null || item.getType().isAir()) {
         return 0;
      } else {
         Integer original = this.originalLooting(item);
         return enchantment.equals(Enchantment.LOOTING) && original != null ? original : item.getEnchantmentLevel(enchantment);
      }
   }

   /** The level that counts for this player: the item's own level or the passive bonus, whichever is higher. */
   public int effectiveLevel(Player player, ItemStack item, Enchantment enchantment) {
      return Math.max(this.itemLevel(item, enchantment), this.bonusLevel(player, item, enchantment));
   }

   /** A copy of the item with the passive enchantment levels on it, for computing drops. */
   public ItemStack virtualTool(Player player, ItemStack item) {
      ItemStack copy = item.clone();

      for (Enchantment e : new Enchantment[]{Enchantment.FORTUNE, Enchantment.EFFICIENCY, Enchantment.UNBREAKING}) {
         int bonus = this.bonusLevel(player, item, e);
         if (bonus > copy.getEnchantmentLevel(e)) {
            copy.addUnsafeEnchantment(e, bonus);
         }
      }

      return copy;
   }

   private static double efficiencyValue(int level) {
      return level <= 0 ? 0.0 : (double)level * level + 1.0;
   }

   private static double sharpnessValue(int level) {
      return level <= 0 ? 0.0 : 0.5 * level + 0.5;
   }

   /** Runs every tick: Efficiency as an attribute bonus, and safety cleanup of temporary Looting. */
   public void tickAttributes() {
      for (Player player : Bukkit.getOnlinePlayers()) {
         ItemStack hand = player.getInventory().getItemInMainHand();
         int own = hand.getEnchantmentLevel(Enchantment.EFFICIENCY);
         int effective = player.isDead() ? own : Math.max(own, this.bonusLevel(player, hand, Enchantment.EFFICIENCY));
         double extra = efficiencyValue(effective) - efficiencyValue(own);
         AttributeBonus.set(player, Attribute.MINING_EFFICIENCY, this.efficiencyKey, Math.max(0.0, extra), Operation.ADD_NUMBER);
      }

      if (!this.pendingLooting.isEmpty()) {
         for (UUID id : new ArrayList<>(this.pendingLooting)) {
            Player player = Bukkit.getPlayer(id);
            if (player != null) {
               this.revertAll(player);
            }

            this.pendingLooting.remove(id);
         }
      }
   }

   public void removeBonuses(Player player) {
      AttributeBonus.remove(player, Attribute.MINING_EFFICIENCY, this.efficiencyKey);
      this.revertAll(player);
   }

   /** True while the item carries Looting that Infuse added for a killing blow. */
   public boolean isTemporary(ItemStack item) {
      return this.originalLooting(item) != null;
   }

   // ----- Fortune -----

   private static boolean fortuneMatters(Material type) {
      return FORTUNE_BLOCKS.contains(type)
         || Tag.COAL_ORES.isTagged(type)
         || Tag.IRON_ORES.isTagged(type)
         || Tag.GOLD_ORES.isTagged(type)
         || Tag.COPPER_ORES.isTagged(type)
         || Tag.LAPIS_ORES.isTagged(type)
         || Tag.REDSTONE_ORES.isTagged(type)
         || Tag.DIAMOND_ORES.isTagged(type)
         || Tag.EMERALD_ORES.isTagged(type)
         || Tag.LEAVES.isTagged(type);
   }

   // Fortune is applied when the block's drops are decided, after the break can no longer be cancelled. The drop list
   // is edited in place so other plugins (auto-pickup and so on) still see the items.
   @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
   public void onBlockDrops(BlockDropItemEvent event) {
      Player player = event.getPlayer();
      ItemStack tool = player.getInventory().getItemInMainHand();
      Material type = event.getBlockState().getType();
      if (fortuneMatters(type) && !tool.containsEnchantment(Enchantment.SILK_TOUCH)) {
         int bonus = this.bonusLevel(player, tool, Enchantment.FORTUNE);
         if (bonus > tool.getEnchantmentLevel(Enchantment.FORTUNE)) {
            List<ItemStack> drops = new ArrayList<>();

            for (ItemStack drop : event.getBlockState().getDrops(this.virtualTool(player, tool), player)) {
               if (drop != null && !drop.getType().isAir()) {
                  drops.add(drop);
               }
            }

            List<Item> items = event.getItems();

            for (int i = items.size() - 1; i >= 0; i--) {
               Item entity = items.get(i);
               ItemStack match = null;

               for (ItemStack drop : drops) {
                  if (drop.isSimilar(entity.getItemStack())) {
                     match = drop;
                     break;
                  }
               }

               if (match == null) {
                  items.remove(i);
               } else {
                  entity.setItemStack(match);
                  drops.remove(match);
               }
            }

            Location at = event.getBlockState().getLocation().add(0.5, 0.5, 0.5);

            for (ItemStack extra : drops) {
               at.getWorld().dropItemNaturally(at, extra);
            }
         }
      }
   }

   // ----- Unbreaking -----

   @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
   public void onItemDamage(PlayerItemDamageEvent event) {
      ItemStack item = event.getItem();
      int own = item.getEnchantmentLevel(Enchantment.UNBREAKING);
      int bonus = this.bonusLevel(event.getPlayer(), item, Enchantment.UNBREAKING);
      if (bonus > own && isMiningTool(item.getType())) {
         // Same rule as vanilla Unbreaking for tools: each point of damage is taken with chance 1 / (level + 1).
         int taken = 0;

         for (int i = 0; i < event.getOriginalDamage(); i++) {
            if (ThreadLocalRandom.current().nextInt(bonus + 1) == 0) {
               taken++;
            }
         }

         event.setDamage(taken);
      }
   }

   // ----- Sharpness (Apophis spark) -----

   @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
   public void onHit(EntityDamageByEntityEvent event) {
      if (event.getDamager() instanceof Player player && event.getEntity() instanceof LivingEntity) {
         ItemStack weapon = player.getInventory().getItemInMainHand();
         int own = weapon.getEnchantmentLevel(Enchantment.SHARPNESS);
         int bonus = this.bonusLevel(player, weapon, Enchantment.SHARPNESS);
         if (bonus > own) {
            double extra = (sharpnessValue(bonus) - sharpnessValue(own)) * player.getAttackCooldown();
            event.setDamage(event.getDamage() + extra);
         }
      }
   }

   // ----- Looting -----

   private Integer originalLooting(ItemStack item) {
      if (item != null && item.hasItemMeta()) {
         return item.getItemMeta().getPersistentDataContainer().get(this.tempLootingKey, PersistentDataType.INTEGER);
      } else {
         return null;
      }
   }

   // MONITOR: after every other plugin has changed the damage, so we know whether the hit really kills.
   @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
   public void onLethalHit(EntityDamageByEntityEvent event) {
      if (event.getDamager() instanceof Player player && event.getEntity() instanceof LivingEntity target && !(target instanceof Player)) {
         ItemStack weapon = player.getInventory().getItemInMainHand();
         int own = weapon.getEnchantmentLevel(Enchantment.LOOTING);
         int bonus = this.bonusLevel(player, weapon, Enchantment.LOOTING);
         if (bonus > own && this.originalLooting(weapon) == null && event.getFinalDamage() >= target.getHealth() + target.getAbsorptionAmount()) {
            ItemMeta meta = weapon.getItemMeta();
            meta.getPersistentDataContainer().set(this.tempLootingKey, PersistentDataType.INTEGER, own);
            meta.addEnchant(Enchantment.LOOTING, bonus, true);
            weapon.setItemMeta(meta);
            // The loot is rolled in this same tick; it is taken off on the death event, or next tick at the latest.
            this.pendingLooting.add(player.getUniqueId());
         }
      }
   }

   @EventHandler(priority = EventPriority.LOWEST)
   public void onDeath(EntityDeathEvent event) {
      if (!(event.getEntity() instanceof Player) && event.getEntity().getKiller() != null) {
         this.revertAll(event.getEntity().getKiller());
      }
   }

   /** Restores the original Looting level on an item that got it temporarily. Returns true if the item was changed. */
   public boolean revert(ItemStack item) {
      Integer original = this.originalLooting(item);
      if (original == null) {
         return false;
      } else {
         ItemMeta meta = item.getItemMeta();
         PersistentDataContainer pdc = meta.getPersistentDataContainer();
         pdc.remove(this.tempLootingKey);
         meta.removeEnchant(Enchantment.LOOTING);
         if (original > 0) {
            meta.addEnchant(Enchantment.LOOTING, original, true);
         }

         item.setItemMeta(meta);
         return true;
      }
   }

   public void revertAll(Player player) {
      if (!this.pendingLooting.contains(player.getUniqueId())) {
         return;
      }

      for (ItemStack item : player.getInventory().getContents()) {
         if (item != null) {
            this.revert(item);
         }
      }

      this.revert(player.getItemOnCursor());
      this.pendingLooting.remove(player.getUniqueId());
   }

   @EventHandler(priority = EventPriority.LOWEST)
   public void onDrop(PlayerDropItemEvent event) {
      if (this.pendingLooting.contains(event.getPlayer().getUniqueId())) {
         ItemStack stack = event.getItemDrop().getItemStack();
         if (this.revert(stack)) {
            event.getItemDrop().setItemStack(stack);
         }

         this.revertAll(event.getPlayer());
      }
   }

   @EventHandler(priority = EventPriority.LOWEST)
   public void onClick(InventoryClickEvent event) {
      if (event.getWhoClicked() instanceof Player player) {
         this.revertAll(player);
      }
   }

   @EventHandler(priority = EventPriority.LOWEST)
   public void onDrag(InventoryDragEvent event) {
      if (event.getWhoClicked() instanceof Player player) {
         this.revertAll(player);
      }
   }

   @EventHandler(priority = EventPriority.LOWEST)
   public void onSwap(PlayerSwapHandItemsEvent event) {
      this.revertAll(event.getPlayer());
   }

   @EventHandler(priority = EventPriority.LOWEST)
   public void onHeld(PlayerItemHeldEvent event) {
      this.revertAll(event.getPlayer());
   }

   @EventHandler(priority = EventPriority.LOWEST)
   public void onInteractEntity(PlayerInteractEntityEvent event) {
      this.revertAll(event.getPlayer());
   }

   @EventHandler(priority = EventPriority.LOWEST)
   public void onPlayerDeath(PlayerDeathEvent event) {
      if (this.pendingLooting.contains(event.getPlayer().getUniqueId())) {
         for (ItemStack drop : event.getDrops()) {
            this.revert(drop);
         }
      }

      this.revertAll(event.getPlayer());
   }

   @EventHandler(priority = EventPriority.LOWEST)
   public void onQuit(PlayerQuitEvent event) {
      this.removeBonuses(event.getPlayer());
   }
}
