package me.poggercity.infuse.IMP;

import java.util.Arrays;
import java.util.List;
import me.poggercity.infuse.Infuses.Apophis;
import me.poggercity.infuse.Infuses.Augmented;
import me.poggercity.infuse.Infuses.Effects;
import me.poggercity.infuse.Infuses.Emerald;
import me.poggercity.infuse.Infuses.Feather;
import me.poggercity.infuse.Infuses.Fire;
import me.poggercity.infuse.Infuses.Frost;
import me.poggercity.infuse.Infuses.Haste;
import me.poggercity.infuse.Infuses.Heart;
import me.poggercity.infuse.Infuses.Invis;
import me.poggercity.infuse.Infuses.Ocean;
import me.poggercity.infuse.Infuses.Regen;
import me.poggercity.infuse.Infuses.Speed;
import me.poggercity.infuse.Infuses.Strength;
import me.poggercity.infuse.Infuses.Thunder;
import me.poggercity.infuse.Managers.CooldownManager;
import me.poggercity.infuse.Managers.Infuse;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

public class SwordBringer implements Listener, CommandExecutor {
   private final HackEquipListener hackEquipListener;
   private static final String EME = "Emerald Effect";
   private static final String FEATHER = "Feather Effect";
   private static final String FIRE = "Fire Effect";
   private static final String FROST = "Frost Effect";
   private static final String HASTE = "Haste Effect";
   private static final String HEART = "Heart Effect";
   private static final String INVIS = "Invis Effect";
   private static final String OCEAN = "Ocean Effect";
   private static final String REGEN = "Regen Effect";
   private static final String SPEED = "Speed Effect";
   private static final String ST = "Strength Effect";
   private static final String THUNDER = "Thunder Effect";
   private static final String AUGEME = "Augmented Emerald Effect";
   private static final String AUGFEATHER = "Augmented Feather Effect";
   private static final String AUGFIRE = "Augmented Fire Effect";
   private static final String AUGFROST = "Augmented Frost Effect";
   private static final String AUGHASTE = "Augmented Haste Effect";
   private static final String AUGHEART = "Augmented Heart Effect";
   private static final String AUGINVIS = "Augmented Invis Effect";
   private static final String AUGOCEAN = "Augmented Ocean Effect";
   private static final String AUGREGEN = "Augmented Regen Effect";
   private static final String AUGSPEED = "Augmented Speed Effect";
   private static final String AUGST = "Augmented Strength Effect";
   private static final String AUGTHUNDER = "Augmented Thunder Effect";
   private static final String APOPHIS = "Apophis Effect";
   private static final String AUGAPOPHIS = "Augmented Apophis Effect";
   private final JavaPlugin plugin;
   private final List<ItemStack> gems = Arrays.asList(
      Effects.createINVIS(),
      Effects.createHASTE(),
      Effects.createREGEN(),
      Effects.createAPOPHIS(),
      Effects.createFEATHER(),
      Effects.createTHUNDER(),
      Effects.createST(),
      Effects.createFROST(),
      Effects.createHEART(),
      Effects.createEME(),
      Effects.createOCEAN(),
      Effects.createFIRE(),
      Effects.createSPEED(),
      Effects.createDRAGON()
   );
   private final List<ItemStack> gems2 = Arrays.asList(
      Augmented.createINVIS(),
      Augmented.createHASTE(),
      Augmented.createREGEN(),
      Augmented.createAPOPHIS(),
      Augmented.createFEATHER(),
      Augmented.createTHUNDER(),
      Augmented.createST(),
      Augmented.createFROST(),
      Augmented.createHEART(),
      Augmented.createEME(),
      Augmented.createOCEAN(),
      Augmented.createFIRE(),
      Augmented.createSPEED(),
      Effects.createDRAGON()
   );

   public SwordBringer(JavaPlugin plugin, HackEquipListener hackEquipListener) {
      this.plugin = plugin;
      Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
      plugin.getCommand("effects").setExecutor(this);
      plugin.getCommand("augments").setExecutor(this);
      plugin.getCommand("giveselector").setExecutor(this);
      this.hackEquipListener = hackEquipListener;
   }

   private void openSwordSelectionGUI(Player player) {
      Inventory gui = Bukkit.createInventory((InventoryHolder)null, 54, "§lEffects");
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

      for (int slot : magentaSlots) {
         gui.setItem(slot, magentaPane);
      }

      int[] var16 = new int[]{36, 37, 38, 42, 43, 44, 45, 46, 47, 48, 50, 51, 52, 53};

      for (int slotIndex : var16) {
         gui.setItem(slotIndex, orangePane);
      }

      int[] var21 = new int[]{11, 12, 13, 14, 15, 21, 22, 23, 39, 40, 41, 49, 31};

      for (int var18 = 0; var18 < var21.length; var18++) {
         if (var18 < this.gems.size()) {
            gui.setItem(var21[var18], this.gems.get(var18));
         }
      }

      for (int var19 = 0; var19 < gui.getSize(); var19++) {
         if (gui.getItem(var19) == null) {
            gui.setItem(var19, blackPane);
         }
      }

      player.openInventory(gui);
   }

   private void openAugmented(Player player) {
      Inventory gui = Bukkit.createInventory((InventoryHolder)null, 54, "§lEffects");
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

      for (int slot : magentaSlots) {
         gui.setItem(slot, magentaPane);
      }

      int[] var16 = new int[]{36, 37, 38, 42, 43, 44, 45, 46, 47, 48, 50, 51, 52, 53};

      for (int slotIndex : var16) {
         gui.setItem(slotIndex, orangePane);
      }

      int[] var21 = new int[]{11, 12, 13, 14, 15, 21, 22, 23, 39, 40, 41, 49, 31};

      for (int var18 = 0; var18 < var21.length; var18++) {
         if (var18 < this.gems2.size()) {
            gui.setItem(var21[var18], this.gems2.get(var18));
         }
      }

      for (int var19 = 0; var19 < gui.getSize(); var19++) {
         if (gui.getItem(var19) == null) {
            gui.setItem(var19, blackPane);
         }
      }

      player.openInventory(gui);
   }

   @EventHandler
   public void onInventoryClick(InventoryClickEvent event) {
      if (event.getWhoClicked() instanceof Player) {
         Player player = (Player)event.getWhoClicked();
         Inventory clickedInventory = event.getClickedInventory();
         if (clickedInventory != null
            && event.getView().getTitle().equals("§lEffects")
            && !CooldownManager.isOnCooldown(player.getUniqueId(), "pickinganeffectlol")
            && !player.isOp()) {
            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null
               || clickedItem.getType() == Material.MAGENTA_STAINED_GLASS_PANE
               || clickedItem.getType() == Material.ORANGE_STAINED_GLASS
               || clickedItem.getType() == Material.BLACK_STAINED_GLASS_PANE) {
               return;
            }

            if (this.hasEffect(player)) {
               player.closeInventory();
            }

            if (Apophis.isApophis(clickedItem)) {
               this.hackEquipListener.handleLegendaryHack(player, clickedItem, "Apophis Effect");
            } else if (Augmented.ISAPOPHIS(clickedItem)) {
               this.hackEquipListener.handleLegendaryHack(player, clickedItem, "Augmented Apophis Effect");
            } else if (Frost.isStealthGem(clickedItem)) {
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
            } else if (Augmented.ISFROST(clickedItem)) {
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
            }

            CooldownManager.setCooldown(player.getUniqueId(), "pickinganeffectlol", 1L);
            event.setCancelled(true);
         }
      }
   }

   private boolean hasEffect(Player player) {
      String hack1 = Infuse.getInstance().getHackManager().getHack(player.getUniqueId(), "1");
      String hack2 = Infuse.getInstance().getHackManager().getHack(player.getUniqueId(), "2");
      return hack1 != null && !hack1.isEmpty() || hack2 != null && !hack2.isEmpty();
   }

   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (command.getName().equalsIgnoreCase("effects")) {
         if (args.length == 0) {
            if (sender instanceof Player player) {
               this.openSwordSelectionGUI(player);
            } else {
               sender.sendMessage(ChatColor.RED + "/effects <player>");
            }
         } else {
            Player player = Bukkit.getPlayerExact(args[0]);
            if (player != null && player.isOnline()) {
               this.openSwordSelectionGUI(player);
               String var10001 = String.valueOf(ChatColor.GREEN);
               sender.sendMessage(var10001 + "Opened GUI for " + player.getName());
            } else {
               sender.sendMessage(ChatColor.RED + "Player not found or is offline.");
            }
         }

         return true;
      } else if (command.getName().equalsIgnoreCase("augments")) {
         if (args.length == 0) {
            if (sender instanceof Player player) {
               this.openAugmented(player);
            } else {
               sender.sendMessage(ChatColor.RED + "/augments <player>");
            }
         } else {
            Player player = Bukkit.getPlayerExact(args[0]);
            if (player != null && player.isOnline()) {
               this.openAugmented(player);
               String var10001 = String.valueOf(ChatColor.GREEN);
               sender.sendMessage(var10001 + "Opened GUI for " + player.getName());
            } else {
               sender.sendMessage(ChatColor.RED + "Player not found or is offline.");
            }
         }

         return true;
      } else if (!command.getName().equalsIgnoreCase("giveselector")) {
         return false;
      } else if (!(sender instanceof Player) && args.length == 0) {
         sender.sendMessage(ChatColor.RED + "Usage: /giveselector <player|@a|*>");
         return true;
      } else if (!sender.isOp()) {
         sender.sendMessage(ChatColor.RED + "Only operators can use this command.");
         return true;
      } else {
         ItemStack selector = new ItemStack(Material.DRAGON_BREATH);
         ItemMeta meta = selector.getItemMeta();
         String var10001 = String.valueOf(ChatColor.LIGHT_PURPLE);
         meta.setDisplayName(var10001 + ChatColor.BOLD + "Choose your Effect");
         meta.setCustomModelData(1);
         selector.setItemMeta(meta);
         if (args.length == 0) {
            Player target = (Player)sender;
            target.getInventory().addItem(new ItemStack[]{selector});
            target.sendMessage(ChatColor.GREEN + "Selector given to you!");
         } else if (!args[0].equalsIgnoreCase("@a") && !args[0].equalsIgnoreCase("*")) {
            Player target = Bukkit.getPlayerExact(args[0]);
            if (target != null && target.isOnline()) {
               target.getInventory().addItem(new ItemStack[]{selector});
               target.sendMessage(ChatColor.GREEN + "You have been given a selector!");
               var10001 = String.valueOf(ChatColor.GREEN);
               sender.sendMessage(var10001 + "Selector given to " + target.getName());
            } else {
               sender.sendMessage(ChatColor.RED + "Player not found or is offline.");
            }
         } else {
            for (Player online : Bukkit.getOnlinePlayers()) {
               online.getInventory().addItem(new ItemStack[]{selector.clone()});
               online.sendMessage(ChatColor.GREEN + "You have been given a selector!");
            }

            sender.sendMessage(ChatColor.GREEN + "Selector given to all players!");
         }

         return true;
      }
   }
}
