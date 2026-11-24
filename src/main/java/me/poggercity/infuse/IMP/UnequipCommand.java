package me.poggercity.infuse.IMP;

import me.poggercity.infuse.Managers.CooldownManager;
import me.poggercity.infuse.Managers.Infuse;
import me.poggercity.infuse.Managers.PlayerHackManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

public class UnequipCommand implements CommandExecutor, Listener {
   private final PlayerHackManager hackManager = new PlayerHackManager();
   private final Plugin plugin;

   public UnequipCommand(Plugin plugin) {
      this.plugin = plugin;
   }

   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (!(sender instanceof Player player)) {
         sender.sendMessage("Only players can use this command.");
         return true;
      } else {
         String cmd = label.toLowerCase();
         String hackType = null;
         String slot1 = Infuse.getInstance().getHackManager().getHack(player.getUniqueId(), "1");
         String slot2 = Infuse.getInstance().getHackManager().getHack(player.getUniqueId(), "2");
         if (cmd.equals("ldrain")) {
            if (slot1 != null) {
               hackType = "1";
            } else if (slot2 != null) {
               hackType = "2";
            }
         } else if (cmd.equals("rdrain")) {
            if (slot2 != null) {
               hackType = "2";
            } else if (slot1 != null) {
               hackType = "1";
            }
         }

         if (hackType == null) {
            return true;
         } else {
            String currentHack = Infuse.getInstance().getHackManager().getHack(player.getUniqueId(), hackType);
            if (player.getInventory().firstEmpty() == -1) {
               player.sendMessage(ChatColor.RED + "Your inventory is full! Make space before unequipping.");
               return true;
            } else if (CooldownManager.isOnCooldown(player.getUniqueId(), "unequip")) {
               long secondsLeft = CooldownManager.getCooldownTimeLeft(player.getUniqueId(), "unequip") / 1000L;
               long minutes = secondsLeft / 60L;
               long seconds = secondsLeft % 60L;
               String var10001 = String.valueOf(ChatColor.RED);
               player.sendMessage(var10001 + "You can drain your effect in " + ChatColor.BOLD + minutes + ":" + seconds);
               return true;
            } else {
               boolean cantunequip = this.plugin.getConfig().getBoolean("configs.cant_unequip_while_on_cooldown", false);
               if ((!cantunequip || !CooldownManager.hasAnyCooldown(player.getUniqueId())) && !CooldownManager.hasAnyEffect(player.getUniqueId())) {
                  int cooldownSeconds = this.plugin.getConfig().getInt("configs.drain_cooldown", 300);
                  CooldownManager.setCooldown(player.getUniqueId(), "unequip", cooldownSeconds);
                  this.hackManager.removeHack(player.getUniqueId(), hackType);
                  player.playSound(player.getLocation(), Sound.BLOCK_BREWING_STAND_BREW, 1.0F, 1.2F);
                  Location anchor = player.getLocation();
                  Bukkit.getRegionScheduler().runDelayed(this.plugin, anchor, task -> {
                     HackMapping mapping = HackMapping.fromHackName(currentHack);
                     if (mapping != null) {
                        ItemStack glitchItem = mapping.createItem();
                        if (glitchItem != null) {
                           player.getInventory().addItem(new ItemStack[]{glitchItem});
                        }
                     }
                  }, 10L);
                  Bukkit.getRegionScheduler().runDelayed(this.plugin, anchor, task -> {
                     if (!this.hasVirusGlitchGemEquipped(player, "1") && !this.hasVirusGlitchGemEquipped(player, "2")) {
                        player.getAttribute(Attribute.MAX_HEALTH).setBaseValue(20.0);
                     }
                  }, 20L);
                  Bukkit.getRegionScheduler().runDelayed(this.plugin, anchor, task -> {
                     if (!this.hasOceanGlitchGemEquipped(player, "1") || !this.hasOceanGlitchGemEquipped(player, "2")) {
                        player.getAttribute(Attribute.WATER_MOVEMENT_EFFICIENCY).setBaseValue(0.0);
                     }
                  }, 20L);
                  return true;
               } else {
                  long secondsLeft = CooldownManager.getCooldownTimeLeft(player.getUniqueId(), "unequip") / 1000L;
                  long minutes = secondsLeft / 60L;
                  long seconds = secondsLeft % 60L;
                  player.sendMessage(ChatColor.RED + "You can't drain your effect while in you are on cooldown");
                  return true;
               }
            }
         }
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
}
