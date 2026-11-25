package me.poggercity.infuse.Infuses;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
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
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

/**
 * Enchantment bonuses that only last while the player has the effect: Emerald (Looting 5 on swords) and the Apophis
 * spark (Efficiency/Fortune on tools, Sharpness/Looting on swords and axes). Haste is different on purpose: it puts
 * real enchantments on the tool, which the tool keeps (see Haste.enchantTool).
 *
 * These bonuses don't change the item: Efficiency is a mining-efficiency attribute bonus, Fortune is applied when the
 * block drops, Sharpness adds damage on hit. Looting is the one enchantment the game only reads from the item itself,
 * so it is put on the weapon only for the killing hit and taken off again right after the loot is rolled.
 */
public class ToolBonuses implements Listener {
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

      return Gamble.borrowsPassive(player, name.toLowerCase(Locale.ROOT));
   }

   private static boolean worldDisabled(Player player) {
      return Infuse.getInstance().getConfig().getStringList("general.disabled_worlds").stream().anyMatch(w -> w.equalsIgnoreCase(player.getWorld().getName()));
   }

   private static boolean isSword(Material m) {
      return Tag.ITEMS_SWORDS.isTagged(m);
   }

   private static boolean isAxe(Material m) {
      return m.name().endsWith("_AXE");
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
         // Haste is not here: it puts real enchantments on the tool (Haste.enchantTool), which the tool keeps.
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
         this.revertPending();
      }

      // Fallback for an item that left the inventory while it had temporary Looting and came back later (from a
      // container, an item frame, the ground): whatever a player holds never keeps it for more than a tick.
      for (Player player : Bukkit.getOnlinePlayers()) {
         this.revert(player.getInventory().getItemInMainHand());
         this.revert(player.getItemOnCursor());
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

   // Fortune is applied when the block's drops are decided, after the break can no longer be cancelled. LOWEST, and
   // the drop list is edited in place (extra stacks are added as not-yet-spawned items, which Paper spawns with the
   // rest), so auto-pickup and other drop plugins see the final drops and a cancelled event drops nothing.
   @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
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
            List<Item> unmatched = new ArrayList<>();

            for (Item entity : items) {
               ItemStack match = null;

               for (ItemStack drop : drops) {
                  if (drop.isSimilar(entity.getItemStack())) {
                     match = drop;
                     break;
                  }
               }

               if (match == null) {
                  unmatched.add(entity);
               } else {
                  entity.setItemStack(match);
                  drops.remove(match);
               }
            }

            // Items the new roll doesn't have take its remaining stacks; any still left over don't drop.
            for (Item entity : unmatched) {
               if (drops.isEmpty()) {
                  items.remove(entity);
               } else {
                  entity.setItemStack(drops.remove(0));
               }
            }

            Location at = event.getBlockState().getLocation().add(0.5, 0.5, 0.5);

            for (ItemStack extra : drops) {
               Item entity = at.getWorld().createEntity(at, Item.class);
               entity.setItemStack(extra);
               items.add(entity);
            }
         }
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
   // Armor stands are skipped: a hit only wobbles them (no death event follows) and they have no loot to boost.
   @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
   public void onLethalHit(EntityDamageByEntityEvent event) {
      if (event.getDamager() instanceof Player player
         && event.getEntity() instanceof LivingEntity target
         && !(target instanceof Player)
         && !(target instanceof ArmorStand)) {
         ItemStack weapon = player.getInventory().getItemInMainHand();
         // A weapon still carrying temporary Looting from an earlier hit is put back first, so it never counts for
         // a kill by someone who doesn't have the bonus.
         this.revert(weapon);
         int own = weapon.getEnchantmentLevel(Enchantment.LOOTING);
         int bonus = this.bonusLevel(player, weapon, Enchantment.LOOTING);
         // The final damage already has absorption taken off, so the hit kills when it reaches the target's health.
         if (bonus > own && event.getFinalDamage() >= target.getHealth()) {
            ItemMeta meta = weapon.getItemMeta();
            meta.getPersistentDataContainer().set(this.tempLootingKey, PersistentDataType.INTEGER, own);
            meta.addEnchant(Enchantment.LOOTING, bonus, true);
            weapon.setItemMeta(meta);
            // The loot is rolled inside this same hit. Right after it the death event (or the totem event if the
            // target is saved) takes the Looting off again; the next tick is only a fallback.
            this.pendingLooting.add(player.getUniqueId());
         }
      }
   }

   private void revertPending() {
      for (UUID id : new ArrayList<>(this.pendingLooting)) {
         Player player = Bukkit.getPlayer(id);
         if (player != null) {
            this.revertAll(player);
         }

         this.pendingLooting.remove(id);
      }
   }

   // LOWEST: the drops were rolled just before this event, so the Looting can come off before any other plugin runs.
   @EventHandler(priority = EventPriority.LOWEST)
   public void onDeath(EntityDeathEvent event) {
      if (!this.pendingLooting.isEmpty()) {
         this.revertPending();
      }
   }

   // A target saved by a totem doesn't die, so no death event follows the hit.
   @EventHandler(priority = EventPriority.MONITOR)
   public void onResurrect(EntityResurrectEvent event) {
      if (!event.isCancelled() && !this.pendingLooting.isEmpty()) {
         this.revertPending();
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

      // Also an item taken out of a container (or any other inventory) that still has temporary Looting.
      ItemStack current = event.getCurrentItem();
      if (current != null && this.revert(current)) {
         event.setCurrentItem(current);
      }
   }

   // Right-clicking a block or entity can move the held item somewhere no inventory event sees (decorated pots,
   // shelves, armor stands), so the Looting comes off first.
   @EventHandler(priority = EventPriority.LOWEST)
   public void onInteract(PlayerInteractEvent event) {
      this.revertAll(event.getPlayer());
   }

   @EventHandler(priority = EventPriority.LOWEST)
   public void onInteractAt(PlayerInteractAtEntityEvent event) {
      this.revertAll(event.getPlayer());
   }

   @EventHandler(priority = EventPriority.LOWEST)
   public void onArmorStand(PlayerArmorStandManipulateEvent event) {
      this.revertAll(event.getPlayer());
   }

   @EventHandler(priority = EventPriority.LOWEST)
   public void onPickup(EntityPickupItemEvent event) {
      ItemStack stack = event.getItem().getItemStack();
      if (this.revert(stack)) {
         event.getItem().setItemStack(stack);
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
