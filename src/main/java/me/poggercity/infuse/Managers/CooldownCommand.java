package me.poggercity.infuse.Managers;

import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CooldownCommand implements CommandExecutor {
   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (args.length == 0) {
         if (sender instanceof Player targetPlayer) {
            CooldownManager.removeAllCooldowns(targetPlayer.getUniqueId());
            targetPlayer.sendMessage(ChatColor.GREEN + "Your cooldowns have been cleared!");
            targetPlayer.setCooldown(Material.DRAGON_BREATH, 1);
         } else {
            sender.sendMessage(ChatColor.RED + "Only players can clear their own cooldowns.");
         }

         return true;
      } else {
         Player targetPlayer = Bukkit.getPlayer(args[0]);
         if (targetPlayer != null && targetPlayer.isOnline()) {
            UUID playerUUID = targetPlayer.getUniqueId();
            CooldownManager.removeAllCooldowns(playerUUID);
            targetPlayer.setCooldown(Material.DRAGON_BREATH, 1);
            String var10001 = String.valueOf(ChatColor.GREEN);
            sender.sendMessage(var10001 + "Cooldowns cleared for " + targetPlayer.getName() + "!");
         } else {
            String var10001 = String.valueOf(ChatColor.RED);
            sender.sendMessage(var10001 + "Player '" + args[0] + "' not found or not online.");
         }

         return true;
      }
   }
}
