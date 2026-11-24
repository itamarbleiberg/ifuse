package me.poggercity.infuse.Managers;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ReloadCommand implements CommandExecutor {
   private final Infuse plugin;

   public ReloadCommand(Infuse plugin) {
      this.plugin = plugin;
   }

   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (command.getName().equalsIgnoreCase("reloadtrust")) {
         this.plugin.reloadConfigFiles();
         sender.sendMessage(ChatColor.GREEN + "The plugin configuration and files have been reloaded!");
         return true;
      } else {
         return false;
      }
   }
}
