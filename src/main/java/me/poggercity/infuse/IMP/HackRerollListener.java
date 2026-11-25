package me.poggercity.infuse.IMP;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import me.poggercity.infuse.Infuses.Apophis;
import me.poggercity.infuse.Infuses.Augmented;
import me.poggercity.infuse.Infuses.Effects;
import me.poggercity.infuse.Infuses.Emerald;
import me.poggercity.infuse.Infuses.Ender;
import me.poggercity.infuse.Infuses.Feather;
import me.poggercity.infuse.Infuses.Fire;
import me.poggercity.infuse.Infuses.Frost;
import me.poggercity.infuse.Infuses.Gamble;
import me.poggercity.infuse.Infuses.Haste;
import me.poggercity.infuse.Infuses.Heart;
import me.poggercity.infuse.Infuses.Invis;
import me.poggercity.infuse.Infuses.Ocean;
import me.poggercity.infuse.Infuses.Regen;
import me.poggercity.infuse.Infuses.Speed;
import me.poggercity.infuse.Infuses.Strength;
import me.poggercity.infuse.Infuses.Thunder;
import me.poggercity.infuse.Managers.CooldownManager;
import me.poggercity.infuse.Managers.EffectKeys;
import me.poggercity.infuse.Managers.Settings;
import me.poggercity.infuse.Managers.PlayerHackManager;
import org.bukkit.Bukkit;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.EventPriority;
import org.bukkit.NamespacedKey;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

public class HackRerollListener implements Listener, CommandExecutor {
   private final PlayerHackManager hackManager = new PlayerHackManager();
   private final Set<Player> cooldownPlayers = new HashSet<>();
   private final JavaPlugin plugin;
   private final HackEquipListener hackEquipListener;

   public HackRerollListener(JavaPlugin plugin, HackEquipListener hackEquipListener) {
      this.plugin = plugin;
      this.hackEquipListener = hackEquipListener;
   }

   private static final String NORMAL_TITLE = ChatColor.BOLD + "Choose your Effect";
   private static final String AUGMENTED_TITLE = ChatColor.BOLD + "Choose your Augmented Effect";

   private NamespacedKey pendingKey() {
      return new NamespacedKey(this.plugin, "pending_augmented_pick");
   }

   public static boolean isSelector(ItemStack item) {
      if (item != null && item.getType() == Material.DRAGON_BREATH) {
         ItemMeta meta = item.getItemMeta();
         return meta != null && meta.hasDisplayName() && meta.getDisplayName().equals(ChatColor.LIGHT_PURPLE.toString() + ChatColor.BOLD + "Choose your Effect");
      } else {
         return false;
      }
   }

   /** The order effects are shown in the menus. */
   public static final List<String> MENU_ORDER = List.of(
      "strength", "speed", "regen", "ender", "invis", "heart", "haste", "frost", "fire", "feather", "emerald", "thunder", "apophis", "ocean", "gamble"
   );
   // Three rows of five in the middle of a 54-slot menu.
   private static final int[][] ROWS = new int[][]{{11, 12, 13, 14, 15}, {20, 21, 22, 23, 24}, {29, 30, 31, 32, 33}};

   /** The effect item for a key; Ender has no augmented version, so its normal item is used for both menus. */
   public static ItemStack effectItem(String key, boolean augmented) {
      String name = EffectKeys.title(key) + " Effect";
      HackMapping mapping = augmented ? HackMapping.fromHackName("Augmented " + name) : null;
      if (mapping == null) {
         mapping = HackMapping.fromHackName(name);
      }

      return mapping == null ? null : mapping.createItem();
   }

   /** The effects players can pick with the selector (starting_effects in config.yml). */
   private static List<ItemStack> startingItems(boolean augmented) {
      List<ItemStack> items = new ArrayList<>();

      for (String key : MENU_ORDER) {
         ItemStack item = Settings.startingAllowed(key) ? effectItem(key, augmented) : null;
         if (item != null) {
            items.add(item);
         }
      }

      return items;
   }

   /** Puts effect items in rows of five; a shorter last row is centered. */
   public static void placeEffects(Inventory gui, List<ItemStack> items) {
      for (int i = 0; i < items.size() && i < 15; i++) {
         int row = i / 5;
         int inRow = Math.min(5, items.size() - row * 5);
         int offset = (5 - inRow) / 2;
         gui.setItem(ROWS[row][offset + i % 5], items.get(i));
      }
   }

   private static String effectKeyOf(ItemStack item) {
      ItemMeta meta = item == null ? null : item.getItemMeta();
      return meta != null && meta.hasDisplayName() ? EffectKeys.keyOf(meta.getDisplayName()) : null;
   }

   private boolean hasPendingPick(Player player) {
      return player.getPersistentDataContainer().has(this.pendingKey(), PersistentDataType.BYTE);
   }

   // The selector GUIs can't be used to move items: clicks in the player's own inventory used to be allowed, so the
   // selector could be moved out of the hand before the second pick and was never used up.
   @EventHandler(priority = EventPriority.LOWEST)
   public void onSelectorDrag(InventoryDragEvent event) {
      String title = event.getView().getTitle();
      if (title.equals(NORMAL_TITLE) || title.equals(AUGMENTED_TITLE)) {
         event.setCancelled(true);
      }
   }

   // A player who left before the second (augmented) pick gets the menu again when they come back.
   @EventHandler
   public void onJoinWithPendingPick(PlayerJoinEvent event) {
      Player player = event.getPlayer();
      if (this.hasPendingPick(player)) {
         Bukkit.getRegionScheduler().runDelayed(this.plugin, player.getLocation(), task -> {
            if (player.isOnline() && this.hasPendingPick(player)) {
               this.openAugmentedInfuses(player);
            }
         }, 40L);
      }
   }

   private boolean nameIs(ItemStack item, String plainName) {
      if (item == null) {
         return false;
      } else {
         ItemMeta m = item.getItemMeta();
         return m != null && m.hasDisplayName() ? ChatColor.stripColor(m.getDisplayName()).equalsIgnoreCase(plainName) : false;
      }
   }

   private boolean isEnder(ItemStack item) {
      return Ender.isStrengthGem(item) || this.nameIs(item, "Ender Effect");
   }

   private boolean isAugmentedEnder(ItemStack item) {
      try {
         Method m = Augmented.class.getDeclaredMethod("ISENDER", ItemStack.class);
         Object r = m.invoke(null, item);
         if (r instanceof Boolean && (Boolean)r) {
            return true;
         }
      } catch (Throwable var4) {
      }

      return this.nameIs(item, "Augmented Ender Effect");
   }

   public void openInfuses(Player player) {
      if (startingItems(false).isEmpty()) {
         player.sendMessage(ChatColor.RED + "No effects can be chosen right now (starting_effects in config.yml).");
         return;
      }

      Inventory gui = Bukkit.createInventory((InventoryHolder)null, 54, ChatColor.BOLD + "Choose your Effect");
      ItemStack magentaPane = new ItemStack(Material.MAGENTA_STAINED_GLASS_PANE);
      ItemMeta magentaMeta = magentaPane.getItemMeta();
      magentaMeta.setDisplayName(" ");
      magentaPane.setItemMeta(magentaMeta);
      ItemStack orangePane = new ItemStack(Material.ORANGE_STAINED_GLASS_PANE);
      ItemMeta orangeMeta = orangePane.getItemMeta();
      orangeMeta.setDisplayName(" ");
      orangePane.setItemMeta(orangeMeta);
      ItemStack blackPane = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
      ItemMeta blackMeta = blackPane.getItemMeta();
      blackMeta.setDisplayName(" ");
      blackPane.setItemMeta(blackMeta);
      int[] magentaSlots = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 17};

      for (int i : magentaSlots) {
         gui.setItem(i, magentaPane);
      }

      int[] var16 = new int[]{36, 37, 38, 42, 43, 44, 45, 46, 47, 48, 50, 51, 52, 53};

      for (int slot : var16) {
         gui.setItem(slot, orangePane);
      }

      placeEffects(gui, startingItems(false));

      for (int var20 = 0; var20 < gui.getSize(); var20++) {
         if (gui.getItem(var20) == null) {
            gui.setItem(var20, blackPane);
         }
      }

      player.openInventory(gui);
   }

   public void openAugmentedInfuses(Player player) {
      if (startingItems(true).isEmpty()) {
         player.sendMessage(ChatColor.RED + "No effects can be chosen right now (starting_effects in config.yml).");
         return;
      }

      Inventory gui = Bukkit.createInventory((InventoryHolder)null, 54, ChatColor.BOLD + "Choose your Augmented Effect");
      ItemStack magentaPane = new ItemStack(Material.MAGENTA_STAINED_GLASS_PANE);
      ItemMeta magentaMeta = magentaPane.getItemMeta();
      magentaMeta.setDisplayName(" ");
      magentaPane.setItemMeta(magentaMeta);
      ItemStack orangePane = new ItemStack(Material.ORANGE_STAINED_GLASS_PANE);
      ItemMeta orangeMeta = orangePane.getItemMeta();
      orangeMeta.setDisplayName(" ");
      orangePane.setItemMeta(orangeMeta);
      ItemStack blackPane = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
      ItemMeta blackMeta = blackPane.getItemMeta();
      blackMeta.setDisplayName(" ");
      blackPane.setItemMeta(blackMeta);
      int[] magentaSlots = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 17};

      for (int i : magentaSlots) {
         gui.setItem(i, magentaPane);
      }

      int[] var16 = new int[]{36, 37, 38, 42, 43, 44, 45, 46, 47, 48, 50, 51, 52, 53};

      for (int slot : var16) {
         gui.setItem(slot, orangePane);
      }

      placeEffects(gui, startingItems(true));

      for (int var20 = 0; var20 < gui.getSize(); var20++) {
         if (gui.getItem(var20) == null) {
            gui.setItem(var20, blackPane);
         }
      }

      player.openInventory(gui);
   }

   @EventHandler
   public void onPlayerRightClick(PlayerInteractEvent event) {
      Player player = event.getPlayer();
      ItemStack item = event.getItem();
      if (!player.getInventory().getItemInOffHand().isSimilar(item) && item != null && item.getType() == Material.DRAGON_BREATH) {
         ItemMeta meta = item.getItemMeta();
         if (meta != null && meta.hasDisplayName()) {
            String var10000 = String.valueOf(ChatColor.LIGHT_PURPLE);
            String expected = var10000 + ChatColor.BOLD + "Choose your Effect";
            if (meta.getDisplayName().equals(expected)) {
               if (CooldownManager.isOnCooldown(player.getUniqueId(), "selector")) {
                  player.sendMessage(ChatColor.RED + "Wait 1 second before using the selector again");
                  return;
               }

               player.swingMainHand();
               if (this.hasPendingPick(player)) {
                  this.openAugmentedInfuses(player);
               } else {
                  this.openInfuses(player);
               }

               return;
            }
         }
      }
   }

   @EventHandler
   public void onInventoryClick(InventoryClickEvent event) {
      if (event.getWhoClicked() instanceof Player) {
         Player player = (Player)event.getWhoClicked();
         String title = event.getView().getTitle();
         if (title.equals(NORMAL_TITLE)) {
            event.setCancelled(true);
         }

         if (event.getClickedInventory() != null && event.getClickedInventory().equals(event.getView().getTopInventory())) {
            if (title.equals(NORMAL_TITLE)) {
               ItemStack clickedItem = event.getCurrentItem();
               if (clickedItem != null
                  && clickedItem.getType() != Material.MAGENTA_STAINED_GLASS_PANE
                  && clickedItem.getType() != Material.ORANGE_STAINED_GLASS_PANE
                  && clickedItem.getType() != Material.BLACK_STAINED_GLASS_PANE) {
                  // The selector is used up on the first pick, before anything is equipped. It used to be removed
                  // only after the second (augmented) pick, from whatever was in the hand at that moment, so leaving
                  // the game or moving the selector first meant it was never used up. Picking again with both slots
                  // full gives back the old effect as an item, so this made unlimited effect potions.
                  ItemStack hand = player.getInventory().getItemInMainHand();
                  if (!isSelector(hand)) {
                     player.closeInventory();
                     player.sendMessage(ChatColor.RED + "Hold the effect selector to choose an effect.");
                     return;
                  }

                  String pickedKey = effectKeyOf(clickedItem);
                  if (pickedKey == null || !Settings.startingAllowed(pickedKey)) {
                     player.closeInventory();
                     player.sendMessage(ChatColor.RED + "That effect can't be chosen right now.");
                     return;
                  }

                  hand.setAmount(hand.getAmount() - 1);
                  player.getPersistentDataContainer().set(this.pendingKey(), PersistentDataType.BYTE, (byte)1);

                  if (Frost.isStealthGem(clickedItem)) {
                     this.hackEquipListener.handleLegendaryHack(player, clickedItem, "Frost Effect");
                  } else if (Emerald.isInvincibilityGem(clickedItem)) {
                     this.hackEquipListener.handleLegendaryHack(player, clickedItem, "Emerald Effect");
                  } else if (Heart.isVirusGlitchGem(clickedItem)) {
                     this.hackEquipListener.handleLegendaryHack(player, clickedItem, "Heart Effect");
                  } else if (Strength.isStealthGem(clickedItem)) {
                     this.hackEquipListener.handleLegendaryHack(player, clickedItem, "Strength Effect");
                  } else if (Invis.isStealthGem(clickedItem)) {
                     this.hackEquipListener.handleLegendaryHack(player, clickedItem, "Invis Effect");
                  } else if (Ocean.isInventoryGlitchGem(clickedItem)) {
                     this.hackEquipListener.handleLegendaryHack(player, clickedItem, "Ocean Effect");
                  } else if (Regen.isInvincibilityGem(clickedItem)) {
                     this.hackEquipListener.handleLegendaryHack(player, clickedItem, "Regen Effect");
                  } else if (Feather.isInvincibilityGem(clickedItem)) {
                     this.hackEquipListener.handleLegendaryHack(player, clickedItem, "Feather Effect");
                  } else if (Haste.isInvincibilityGem(clickedItem)) {
                     this.hackEquipListener.handleLegendaryHack(player, clickedItem, "Haste Effect");
                  } else if (Speed.isInvincibilityGem(clickedItem)) {
                     this.hackEquipListener.handleLegendaryHack(player, clickedItem, "Speed Effect");
                  } else if (Fire.isStrengthGem(clickedItem)) {
                     this.hackEquipListener.handleLegendaryHack(player, clickedItem, "Fire Effect");
                  } else if (Thunder.isInvincibilityGem(clickedItem)) {
                     this.hackEquipListener.handleLegendaryHack(player, clickedItem, "Thunder Effect");
                  } else if (Apophis.isApophisGem(clickedItem) || this.nameIs(clickedItem, "Apophis Effect")) {
                     this.hackEquipListener.handleLegendaryHack(player, clickedItem, "Apophis Effect");
                  } else if (Gamble.isGambleGem(clickedItem)) {
                     this.hackEquipListener.handleLegendaryHack(player, clickedItem, Gamble.GEM_NAME);
                  } else if (this.isEnder(clickedItem)) {
                     this.hackEquipListener.handleLegendaryHack(player, clickedItem, "Ender Effect");
                  }

                  player.closeInventory();
                  this.openAugmentedInfuses(player);
                  CooldownManager.setCooldown(player.getUniqueId(), "selector", 1L);
               }
            }
         }
      }
   }

   @EventHandler
   public void onInventoryClick2(InventoryClickEvent event) {
      if (event.getWhoClicked() instanceof Player) {
         Player player = (Player)event.getWhoClicked();
         String title = event.getView().getTitle();
         if (title.equals(AUGMENTED_TITLE)) {
            event.setCancelled(true);
         }

         if (event.getClickedInventory() != null && event.getClickedInventory().equals(event.getView().getTopInventory())) {
            if (title.equals(AUGMENTED_TITLE)) {
               ItemStack clickedItem = event.getCurrentItem();
               if (clickedItem != null
                  && clickedItem.getType() != Material.MAGENTA_STAINED_GLASS_PANE
                  && clickedItem.getType() != Material.ORANGE_STAINED_GLASS_PANE
                  && clickedItem.getType() != Material.BLACK_STAINED_GLASS_PANE) {
                  // Only reachable after a first pick that used up a selector.
                  if (!this.hasPendingPick(player)) {
                     CooldownManager.setCooldown(player.getUniqueId(), "ehhefbdha", 1L);
                     player.closeInventory();
                     return;
                  }

                  String pickedKey = effectKeyOf(clickedItem);
                  if (pickedKey == null || !Settings.startingAllowed(pickedKey)) {
                     player.sendMessage(ChatColor.RED + "That effect can't be chosen right now.");
                     return;
                  }

                  player.getPersistentDataContainer().remove(this.pendingKey());
                  if (Augmented.ISFROST(clickedItem)) {
                     this.hackEquipListener.handleLegendaryHack(player, clickedItem, "Augmented Frost Effect");
                  } else if (Augmented.ISEME(clickedItem)) {
                     this.hackEquipListener.handleLegendaryHack(player, clickedItem, "Augmented Emerald Effect");
                  } else if (Augmented.ISHEART(clickedItem)) {
                     this.hackEquipListener.handleLegendaryHack(player, clickedItem, "Augmented Heart Effect");
                  } else if (Augmented.ISST(clickedItem)) {
                     this.hackEquipListener.handleLegendaryHack(player, clickedItem, "Augmented Strength Effect");
                  } else if (Augmented.ISINVIS(clickedItem)) {
                     this.hackEquipListener.handleLegendaryHack(player, clickedItem, "Augmented Invis Effect");
                  } else if (Augmented.ISOCEAN(clickedItem)) {
                     this.hackEquipListener.handleLegendaryHack(player, clickedItem, "Augmented Ocean Effect");
                  } else if (Augmented.ISREGEN(clickedItem)) {
                     this.hackEquipListener.handleLegendaryHack(player, clickedItem, "Augmented Regen Effect");
                  } else if (Augmented.ISFEATHER(clickedItem)) {
                     this.hackEquipListener.handleLegendaryHack(player, clickedItem, "Augmented Feather Effect");
                  } else if (Augmented.ISHASTE(clickedItem)) {
                     this.hackEquipListener.handleLegendaryHack(player, clickedItem, "Augmented Haste Effect");
                  } else if (Augmented.ISSPEED(clickedItem)) {
                     this.hackEquipListener.handleLegendaryHack(player, clickedItem, "Augmented Speed Effect");
                  } else if (Augmented.ISFIRE(clickedItem)) {
                     this.hackEquipListener.handleLegendaryHack(player, clickedItem, "Augmented Fire Effect");
                  } else if (Augmented.ISTHUNDER(clickedItem)) {
                     this.hackEquipListener.handleLegendaryHack(player, clickedItem, "Augmented Thunder Effect");
                  } else if (this.isAugmentedEnder(clickedItem)) {
                     this.hackEquipListener.handleLegendaryHack(player, clickedItem, "Augmented Ender Effect");
                  } else if (Augmented.ISAPOPHIS(clickedItem) || this.nameIs(clickedItem, "Augmented Apophis Effect")) {
                     this.hackEquipListener.handleLegendaryHack(player, clickedItem, "Augmented Apophis Effect");
                  } else if (Augmented.ISGAMBLE(clickedItem)) {
                     this.hackEquipListener.handleLegendaryHack(player, clickedItem, Gamble.AUGMENTED_NAME);
                  } else if (this.isEnder(clickedItem)) {
                     this.hackEquipListener.handleLegendaryHack(player, clickedItem, "Ender Effect");
                  }

                  CooldownManager.setCooldown(player.getUniqueId(), "ehhefbdha", 1L);
                  player.closeInventory();
                  this.effect(player);
                  CooldownManager.setCooldown(player.getUniqueId(), "selector", 1L);
               }
            }
         }
      }
   }

   @EventHandler
   public void onInventoryClose(InventoryCloseEvent event) {
      if (event.getPlayer() instanceof Player) {
         Player player = (Player)event.getPlayer();
         if (!CooldownManager.isOnCooldown(player.getUniqueId(), "ehhefbdha")) {
            String title = event.getView().getTitle();
            if (title.equals(AUGMENTED_TITLE) && this.hasPendingPick(player)) {
               Location anchor = player.getLocation();
               Bukkit.getRegionScheduler().runDelayed(this.plugin, anchor, task -> {
                  if (player.isOnline() && this.hasPendingPick(player)) {
                     player.openInventory(event.getInventory());
                  }
               }, 1L);
            }
         }
      }
   }

   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (command.getName().equalsIgnoreCase("clearhack")) {
         if (args.length != 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /clearhack <player>");
            return true;
         } else {
            Player target = Bukkit.getPlayer(args[0]);
            if (target != null && target.isOnline()) {
               this.hackManager.removeHack(target.getUniqueId(), "2");
               this.hackManager.removeHack(target.getUniqueId(), "1");
               return true;
            } else {
               return true;
            }
         }
      } else {
         return false;
      }
   }

   private void effect(Player player) {
      Location base = player.getLocation().add(0.0, 0.1, 0.0);
      World world = player.getWorld();
      player.playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, 1.0F, 1.0F);
      Location anchor = player.getLocation();
      double[] y = new double[]{0.0};
      Bukkit.getRegionScheduler().runAtFixedRate(this.plugin, anchor, task -> {
         if (y[0] > 2.0) {
            task.cancel();
         } else {
            int points = 16;
            double radius = 0.6;

            for (int i = 0; i < points; i++) {
               double angle = 6.283185307179586 * i / points;
               double x = Math.cos(angle) * radius;
               double z = Math.sin(angle) * radius;
               Location loc = base.clone().add(x, y[0], z);
               world.spawnParticle(Particle.FALLING_OBSIDIAN_TEAR, loc, 1, 0.0, 0.0, 0.0, 0.0);
            }

            y[0] += 0.15;
         }
      }, 1L, 1L);
   }
}
