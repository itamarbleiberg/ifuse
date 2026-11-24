package me.poggercity.infuse.Managers;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TrustCommand implements CommandExecutor {
   private final TrustManager trustManager;

   public TrustCommand(TrustManager trustManager) {
      this.trustManager = trustManager;
   }

   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (!(sender instanceof Player player)) {
         sender.sendMessage(ChatColor.RED + "Only players can use this command.");
         return true;
      } else if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
         sender.sendMessage(ChatColor.GREEN + "Trust configuration reloaded successfully.");
         return true;
      } else if (args.length != 1) {
         String var10001 = String.valueOf(ChatColor.RED);
         player.sendMessage(var10001 + "Usage: /" + label + " <player>");
         return true;
      } else {
         Player target = Bukkit.getPlayer(args[0]);
         if (target == null) {
            player.sendMessage(ChatColor.RED + "Player not found.");
            return true;
         } else {
            if (label.equalsIgnoreCase("trust")) {
               if (this.trustManager.isTrusted(player, target)) {
                  String var10001 = String.valueOf(ChatColor.YELLOW);
                  player.sendMessage(var10001 + target.getName() + " is already trusted.");
               } else {
                  this.trustManager.trustPlayer(player, target);
                  String var10001 = String.valueOf(ChatColor.GREEN);
                  player.sendMessage(var10001 + "You have trusted " + target.getName() + ".");
               }
            } else if (label.equalsIgnoreCase("untrust")) {
               if (!this.trustManager.isTrusted(player, target)) {
                  String var10001 = String.valueOf(ChatColor.YELLOW);
                  player.sendMessage(var10001 + target.getName() + " is not trusted.");
               } else {
                  this.trustManager.untrustPlayer(player, target);
                  String var10001 = String.valueOf(ChatColor.RED);
                  player.sendMessage(var10001 + "You have untrusted " + target.getName() + ".");
               }
            }

            return true;
         }
      }
   }
}
