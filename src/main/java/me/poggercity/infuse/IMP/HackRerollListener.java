package me.poggercity.infuse.IMP;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import me.poggercity.infuse.Infuses.Apophis;
import me.poggercity.infuse.Infuses.Augmented;
import me.poggercity.infuse.Infuses.Effects;
import me.poggercity.infuse.Infuses.Emerald;
import me.poggercity.infuse.Infuses.Ender;
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
import me.poggercity.infuse.Managers.PlayerHackManager;
import org.bukkit.Bukkit;
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

   private ItemStack getAugmentedEnderSafe() {
      try {
         Method m = Augmented.class.getDeclaredMethod("createENDER");
         Object r = m.invoke(null);
         if (r instanceof ItemStack) {
            return (ItemStack)r;
         }
      } catch (Throwable var3) {
      }

      return Effects.createDRAGON();
   }

   public void openInfuses(Player player) {
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

      int[] var21 = new int[]{11, 12, 13, 14, 15, 21, 22, 23, 39, 40, 41, 49, 31};
      ItemStack[] effects = new ItemStack[]{
         Effects.createST(),
         Effects.createSPEED(),
         Effects.createREGEN(),
         Effects.createDRAGON(),
         Effects.createINVIS(),
         Effects.createHEART(),
         Effects.createHASTE(),
         Effects.createFROST(),
         Effects.createFIRE(),
         Effects.createFEATHER(),
         Effects.createEME(),
         Effects.createTHUNDER(),
         Effects.createAPOPHIS()
      };

      for (int var19 = 0; var19 < var21.length; var19++) {
         gui.setItem(var21[var19], effects[var19]);
      }

      for (int var20 = 0; var20 < gui.getSize(); var20++) {
         if (gui.getItem(var20) == null) {
            gui.setItem(var20, blackPane);
         }
      }

      player.openInventory(gui);
   }

   public void openAugmentedInfuses(Player player) {
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

      int[] var21 = new int[]{11, 12, 13, 14, 15, 21, 22, 23, 39, 40, 41, 49, 31};
      ItemStack[] effects = new ItemStack[]{
         Augmented.createST(),
         Augmented.createSPEED(),
         Augmented.createREGEN(),
         this.getAugmentedEnderSafe(),
         Augmented.createINVIS(),
         Augmented.createHEART(),
         Augmented.createHASTE(),
         Augmented.createFROST(),
         Augmented.createFIRE(),
         Augmented.createFEATHER(),
         Augmented.createEME(),
         Augmented.createTHUNDER(),
         Augmented.createAPOPHIS()
      };

      for (int var19 = 0; var19 < var21.length; var19++) {
         gui.setItem(var21[var19], effects[var19]);
      }

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
               this.openInfuses(player);
               return;
            }
         }
      }
   }

   @EventHandler
   public void onInventoryClick(InventoryClickEvent event) {
      if (event.getWhoClicked() instanceof Player) {
         Player player = (Player)event.getWhoClicked();
         if (event.getClickedInventory() != null && event.getClickedInventory().equals(event.getView().getTopInventory())) {
            String title = event.getView().getTitle();
            if (title.equals(ChatColor.BOLD + "Choose your Effect")) {
               event.setCancelled(true);
               ItemStack clickedItem = event.getCurrentItem();
               if (clickedItem != null
                  && clickedItem.getType() != Material.MAGENTA_STAINED_GLASS_PANE
                  && clickedItem.getType() != Material.ORANGE_STAINED_GLASS_PANE
                  && clickedItem.getType() != Material.BLACK_STAINED_GLASS_PANE) {
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
         if (event.getClickedInventory() != null && event.getClickedInventory().equals(event.getView().getTopInventory())) {
            String title = event.getView().getTitle();
            if (title.equals(ChatColor.BOLD + "Choose your Augmented Effect")) {
               event.setCancelled(true);
               ItemStack clickedItem = event.getCurrentItem();
               if (clickedItem != null
                  && clickedItem.getType() != Material.MAGENTA_STAINED_GLASS_PANE
                  && clickedItem.getType() != Material.ORANGE_STAINED_GLASS_PANE
                  && clickedItem.getType() != Material.BLACK_STAINED_GLASS_PANE) {
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
                  } else if (this.isEnder(clickedItem)) {
                     this.hackEquipListener.handleLegendaryHack(player, clickedItem, "Ender Effect");
                  }

                  CooldownManager.setCooldown(player.getUniqueId(), "ehhefbdha", 1L);
                  player.closeInventory();
                  this.effect(player);
                  ItemStack heldItem = player.getInventory().getItemInMainHand();
                  Location anchor = player.getLocation();
                  Bukkit.getRegionScheduler().runDelayed(this.plugin, anchor, task -> heldItem.setAmount(heldItem.getAmount() - 1), 1L);
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
            if (title.equals(ChatColor.BOLD + "Choose your Augmented Effect")) {
               Location anchor = player.getLocation();
               Bukkit.getRegionScheduler().runDelayed(this.plugin, anchor, task -> player.openInventory(event.getInventory()), 1L);
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
