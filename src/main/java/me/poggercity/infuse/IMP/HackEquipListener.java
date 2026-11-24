package me.poggercity.infuse.IMP;

import me.poggercity.infuse.Managers.CooldownManager;
import me.poggercity.infuse.Managers.Infuse;
import me.poggercity.infuse.Managers.PlayerHackManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class HackEquipListener implements Listener, CommandExecutor {
   private final PlayerHackManager hackManager = new PlayerHackManager();

   private boolean isPrimaryEffect(String hackName) {
      if (hackName == null) {
         return false;
      } else {
         String stripped = ChatColor.stripColor(hackName).toLowerCase();
         return stripped.contains("invis")
            || stripped.contains("haste")
            || stripped.contains("regen")
            || stripped.contains("apophis")
            || stripped.contains("ender")
            || stripped.contains("feather")
            || stripped.contains("thunder")
            || stripped.contains("strength")
            || stripped.contains("frost")
            || stripped.contains("heart");
      }
   }

   private boolean isSecondaryEffect(String hackName) {
      if (hackName == null) {
         return false;
      } else {
         String stripped = ChatColor.stripColor(hackName).toLowerCase();
         return stripped.contains("ocean") || stripped.contains("emerald") || stripped.contains("fire") || stripped.contains("speed");
      }
   }

   private boolean canEquipWithRestriction(Player player, String newHackName, String targetSlot) {
      if (!Infuse.getInstance().getConfig().getBoolean("configs.only_allow_one_primary", false)) {
         return true;
      } else {
         String slot1 = Infuse.getInstance().getHackManager().getHack(player.getUniqueId(), "1");
         String slot2 = Infuse.getInstance().getHackManager().getHack(player.getUniqueId(), "2");
         boolean newIsPrimary = this.isPrimaryEffect(newHackName);
         boolean slot1HasPrimary = this.isPrimaryEffect(slot1);
         boolean slot2HasPrimary = this.isPrimaryEffect(slot2);
         if (newIsPrimary) {
            return targetSlot.equals("1") ? !slot2HasPrimary : !slot1HasPrimary;
         } else {
            boolean newIsSecondary = this.isSecondaryEffect(newHackName);
            if (newIsSecondary) {
               return targetSlot.equals("1") ? !this.isSecondaryEffect(slot2) : !this.isSecondaryEffect(slot1);
            } else {
               return true;
            }
         }
      }
   }

   public void handleLegendaryHack(Player player, ItemStack item, String hackName) {
      boolean hasTier1 = this.equipped(player, "1");
      boolean hasTier2 = this.equipped(player, "2");
      String hack1 = Infuse.getInstance().getHackManager().getHack(player.getUniqueId(), "1");
      HackMapping hackMapping = HackMapping.fromHackName(hack1);
      if (hasTier1 && hasTier2) {
         if (!this.canEquipWithRestriction(player, hackName, "1")) {
            player.sendMessage(ChatColor.RED + "Cannot equip: Only one primary effect allowed!");
            return;
         }

         int cooldownSeconds = Infuse.getInstance().getConfig().getInt("configs.drain_cooldown", 300);
         CooldownManager.setCooldown(player.getUniqueId(), "unequip", cooldownSeconds);
         ItemStack item2 = hackMapping != null ? hackMapping.createItem() : null;
         Bukkit.getGlobalRegionScheduler().runDelayed(Infuse.getInstance(), task -> {
            if (item2 != null) {
               if (player.getInventory().firstEmpty() != -1) {
                  player.getInventory().addItem(new ItemStack[]{item2});
               } else {
                  player.getWorld().dropItemNaturally(player.getLocation(), item2);
                  player.sendMessage(ChatColor.RED + "Your inventory was full, so the effect was dropped!");
               }
            }
         }, 5L);
         this.hackManager.setHack(player.getUniqueId(), "1", hackName);
         Bukkit.getGlobalRegionScheduler().runDelayed(Infuse.getInstance(), task -> {
            if (!this.hasVirusGlitchGemEquipped(player, "1") || !this.hasVirusGlitchGemEquipped(player, "2")) {
               player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(20.0);
            } else if (this.hasVirusGlitchGemEquipped(player, "1") || this.hasVirusGlitchGemEquipped(player, "2")) {
               player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(30.0);
            }
         }, 20L);
         Bukkit.getGlobalRegionScheduler().runDelayed(Infuse.getInstance(), task -> {
            if (!this.hasOceanGlitchGemEquipped(player, "1") || !this.hasOceanGlitchGemEquipped(player, "2")) {
               player.getAttribute(Attribute.GENERIC_WATER_MOVEMENT_EFFICIENCY).setBaseValue(0.0);
            } else if (this.hasOceanGlitchGemEquipped(player, "1") || this.hasOceanGlitchGemEquipped(player, "2")) {
               player.getAttribute(Attribute.GENERIC_WATER_MOVEMENT_EFFICIENCY).setBaseValue(1.0);
            }
         }, 20L);
      } else if (hasTier1) {
         if (!this.canEquipWithRestriction(player, hackName, "2")) {
            player.sendMessage(ChatColor.RED + "Cannot equip: Only one primary effect allowed!");
            return;
         }

         this.equipHack(player, item, hackName, "2");
      } else if (hasTier2) {
         if (!this.canEquipWithRestriction(player, hackName, "1")) {
            player.sendMessage(ChatColor.RED + "Cannot equip: Only one primary effect allowed!");
            return;
         }

         this.equipHack(player, item, hackName, "1");
      } else {
         this.equipHack(player, item, hackName, "1");
      }
   }

   private boolean hasVirusGlitchGemEquipped(Player player, String tier) {
      String currentHack = Infuse.getInstance().getHackManager().getHack(player.getUniqueId(), tier);
      return currentHack != null
         && (
            currentHack.equalsIgnoreCase(ChatColor.stripColor("Heart Effect"))
               || currentHack.equals("Augmented Heart Effect")
               || currentHack.equalsIgnoreCase(ChatColor.stripColor("Apophis Effect"))
               || currentHack.equals("Augmented Apophis Effect")
         );
   }

   private boolean hasOceanGlitchGemEquipped(Player player, String tier) {
      String currentHack = Infuse.getInstance().getHackManager().getHack(player.getUniqueId(), tier);
      return currentHack != null
         && (
            currentHack.equalsIgnoreCase(ChatColor.stripColor("Ocean Effect"))
               || currentHack.equals("Augmented Ocean Effect")
               || currentHack.equalsIgnoreCase(ChatColor.stripColor("Apophis Effect"))
               || currentHack.equals("Augmented Apophis Effect")
         );
   }

   private boolean equipped(Player player, String tier) {
      String currentHack = Infuse.getInstance().getHackManager().getHack(player.getUniqueId(), tier);
      return currentHack != null && !currentHack.equalsIgnoreCase("uh");
   }

   private boolean equipHack(Player player, ItemStack item, String hackName, String type) {
      String currentHack = Infuse.getInstance().getHackManager().getHack(player.getUniqueId(), type);
      if (currentHack != null && !currentHack.equalsIgnoreCase("uh")) {
         return false;
      } else {
         this.hackManager.setHack(player.getUniqueId(), type, hackName);
         return true;
      }
   }

   @EventHandler
   public void onPlayerConsume(PlayerItemConsumeEvent event) {
      Player player = event.getPlayer();
      ItemStack item = event.getItem();
      if (item.getType() == Material.POTION && item.hasItemMeta()) {
         ItemMeta meta = item.getItemMeta();
         if (meta != null && meta.hasCustomModelData()) {
            boolean hasTier1 = this.equipped(player, "1");
            boolean hasTier2 = this.equipped(player, "2");
            if (hasTier1 && hasTier2 && CooldownManager.isOnCooldown(player.getUniqueId(), "unequip")) {
               long secondsLeft = CooldownManager.getCooldownTimeLeft(player.getUniqueId(), "unequip") / 1000L;
               long minutes = secondsLeft / 60L;
               long seconds = secondsLeft % 60L;
               String var10001 = String.valueOf(ChatColor.RED);
               player.sendMessage(var10001 + "You can swap your effect in " + ChatColor.BOLD + minutes + ":" + seconds);
               event.setCancelled(true);
            } else {
               boolean cantunequip = Infuse.getInstance().getConfig().getBoolean("configs.cant_unequip_while_on_cooldown", true);
               if ((!cantunequip || !CooldownManager.hasAnyCooldown(player.getUniqueId())) && !CooldownManager.hasAnyEffect(player.getUniqueId())) {
                  HackMapping hackMapping = HackMapping.fromItem(item);
                  String hackNameFallback = null;
                  String disp = meta.hasDisplayName() ? ChatColor.stripColor(meta.getDisplayName()) : "";
                  if (hackMapping == null) {
                     if ("Apophis Effect".equalsIgnoreCase(disp)) {
                        hackNameFallback = "Apophis Effect";
                     } else if ("Augmented Apophis Effect".equalsIgnoreCase(disp)) {
                        hackNameFallback = "Augmented Apophis Effect";
                     }
                  }

                  if (hackMapping == null && hackNameFallback == null) {
                     player.sendMessage(ChatColor.RED + "That effect couldn't be recognized.");
                     event.setCancelled(true);
                  } else {
                     String hackName = hackMapping != null ? hackMapping.getHackName() : hackNameFallback;
                     boolean restrictionViolated = false;
                     if (Infuse.getInstance().getConfig().getBoolean("configs.only_allow_one_primary", false)) {
                        if (hasTier1 && hasTier2) {
                           if (!this.canEquipWithRestriction(player, hackName, "1")) {
                              restrictionViolated = true;
                           }
                        } else if (hasTier1) {
                           if (!this.canEquipWithRestriction(player, hackName, "2")) {
                              restrictionViolated = true;
                           }
                        } else if (hasTier2 && !this.canEquipWithRestriction(player, hackName, "1")) {
                           restrictionViolated = true;
                        }
                     }

                     if (restrictionViolated) {
                        player.sendMessage(ChatColor.RED + "Cannot equip: Only one primary effect allowed!");
                        event.setCancelled(true);
                        return;
                     }

                     this.handleLegendaryHack(player, item, hackName);
                     this.consumeMainHandItem(player);
                     player.playSound(player.getLocation(), Sound.BLOCK_BREWING_STAND_BREW, 1.0F, 1.2F);
                     Bukkit.getGlobalRegionScheduler().runDelayed(Infuse.getInstance(), task -> {
                        ItemStack mainHand = player.getInventory().getItemInMainHand();
                        ItemStack offHand = player.getInventory().getItemInOffHand();
                        if (this.isSameItem(mainHand, item)) {
                           player.getInventory().setItemInMainHand((ItemStack)null);
                        } else if (this.isSameItem(offHand, item)) {
                           player.getInventory().setItemInOffHand((ItemStack)null);
                        }
                     }, 1L);
                  }
               } else {
                  player.sendMessage(ChatColor.RED + "You can't swap your effect while you are on cooldown");
                  event.setCancelled(true);
               }
            }
         }
      }
   }

   private boolean isSameItem(ItemStack a, ItemStack b) {
      return a != null && b != null
         ? a.getType() == b.getType()
            && a.hasItemMeta()
            && b.hasItemMeta()
            && a.getItemMeta().hasCustomModelData()
            && b.getItemMeta().hasCustomModelData()
            && a.getItemMeta().getCustomModelData() == b.getItemMeta().getCustomModelData()
         : false;
   }

   private void consumeMainHandItem(Player player) {
      Location anchor = player.getLocation();
      Bukkit.getRegionScheduler().runDelayed(Infuse.getInstance(), anchor, task -> {
         ItemStack mainHandItem = player.getInventory().getItemInMainHand();
         if (mainHandItem != null && mainHandItem.getType() != Material.AIR) {
            if (mainHandItem.getAmount() > 1) {
               mainHandItem.setAmount(mainHandItem.getAmount() - 1);
            } else {
               player.getInventory().setItemInMainHand((ItemStack)null);
            }
         }
      }, 1L);
   }

   @EventHandler
   public void onPlayerDeath(PlayerDeathEvent event) {
      Player player = event.getEntity();
      String hack1 = Infuse.getInstance().getHackManager().getHack(player.getUniqueId(), "1");
      String hack2 = Infuse.getInstance().getHackManager().getHack(player.getUniqueId(), "2");
      if (hack1 != null && hack2 != null) {
         boolean slot1IsAugmented = this.isAugmented(hack1);
         boolean slot2IsAugmented = this.isAugmented(hack2);
         if (slot1IsAugmented && !slot2IsAugmented) {
            this.dropHackOnDeath(player, "1");
         } else if (slot2IsAugmented && !slot1IsAugmented) {
            this.dropHackOnDeath(player, "2");
         } else {
            this.dropHackOnDeath(player, "2");
         }
      } else if (hack2 != null) {
         this.dropHackOnDeath(player, "2");
      } else if (hack1 != null) {
         this.dropHackOnDeath(player, "1");
      }
   }

   private boolean isAugmented(String hackName) {
      return hackName != null && hackName.toLowerCase().contains("augmented");
   }

   private void dropHackOnDeath(Player player, String type) {
      int dropEffects = Infuse.getInstance().getConfig().getInt("configs.drop_effects_on_death", 1);
      if (dropEffects > 0) {
         if (dropEffects >= 2) {
            this.dropSingleHack(player, "1");
            this.dropSingleHack(player, "2");
         } else {
            this.dropSingleHack(player, type);
         }
      }
   }

   private void dropSingleHack(Player player, String type) {
      String hackName = Infuse.getInstance().getHackManager().getHack(player.getUniqueId(), type);
      if (hackName != null) {
         this.hackManager.removeHack(player.getUniqueId(), type);
         HackMapping hackMapping = HackMapping.fromHackName(hackName);
         ItemStack item = hackMapping != null ? hackMapping.createItem() : null;
         if (item != null) {
            Location anchor = player.getLocation();
            Bukkit.getRegionScheduler().runDelayed(Infuse.getInstance(), anchor, task -> {
               Item droppedItem = player.getWorld().dropItemNaturally(player.getLocation(), item);
               if (item.getType() == Material.POTION) {
                  droppedItem.setInvulnerable(true);
               }
            }, 1L);
         }
      }
   }

   @EventHandler
   public void onPlayerDropItem(PlayerDropItemEvent event) {
      ItemStack dropped = event.getItemDrop().getItemStack();
      if (dropped.getType() == Material.POTION) {
         Location anchor = event.getPlayer().getLocation();
         Bukkit.getRegionScheduler().runDelayed(Infuse.getInstance(), anchor, task -> event.getItemDrop().setInvulnerable(true), 1L);
      }
   }

   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (!(sender instanceof Player player)) {
         sender.sendMessage(ChatColor.RED + "Only players can use this command.");
         return true;
      } else {
         String hack1 = Infuse.getInstance().getHackManager().getHack(player.getUniqueId(), "1");
         String hack2 = Infuse.getInstance().getHackManager().getHack(player.getUniqueId(), "2");
         if ((hack1 == null || hack1.equalsIgnoreCase("uh")) && (hack2 == null || hack2.equalsIgnoreCase("uh"))) {
            player.sendMessage(ChatColor.RED + "You do not have any effects equipped to swap.");
            return true;
         } else {
            this.hackManager.setHack(player.getUniqueId(), "1", hack2);
            this.hackManager.setHack(player.getUniqueId(), "2", hack1);
            player.sendMessage(ChatColor.GREEN + "Your Effects have been swapped.");
            return true;
         }
      }
   }
}
