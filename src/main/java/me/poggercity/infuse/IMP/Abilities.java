package me.poggercity.infuse.IMP;

import java.util.List;
import java.util.UUID;
import me.poggercity.infuse.Infuses.Apophis;
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
import me.poggercity.infuse.Managers.Infuse;
import me.poggercity.infuse.Managers.PlayerHackManager;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

public class Abilities implements CommandExecutor, Listener {
   private final PlayerHackManager hackManager = new PlayerHackManager();
   private final Infuse plugin;
   private final Emerald emeraldAbility;
   private final Feather featherAbility;
   private final Fire fireAbility;
   private final Frost frostAbility;
   private final Haste hasteAbility;
   private final Heart heartAbility;
   private final Invis InvisAbility;
   private final Ocean oceanAbility;
   private final Regen regenAbility;
   private final Speed speedAbility;
   private final Strength strengthAbility;
   private final Thunder thunderAbility;
   private final Ender dragon;
   private final Apophis apophisAbility;

   public Abilities(Infuse plugin) {
      this.plugin = plugin;
      this.emeraldAbility = new Emerald(plugin);
      this.featherAbility = new Feather(plugin);
      this.fireAbility = new Fire(plugin);
      this.frostAbility = new Frost(plugin);
      this.hasteAbility = new Haste();
      this.heartAbility = new Heart();
      this.InvisAbility = new Invis(plugin);
      this.oceanAbility = new Ocean(plugin);
      this.regenAbility = new Regen(plugin);
      this.speedAbility = new Speed(plugin);
      this.strengthAbility = new Strength();
      this.thunderAbility = new Thunder(plugin);
      this.dragon = new Ender();
      this.apophisAbility = new Apophis(plugin);
   }

   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (!(sender instanceof Player player)) {
         sender.sendMessage(ChatColor.RED + "Only players can use this command.");
         return true;
      } else {
         UUID playerUUID = player.getUniqueId();
         String slot;
         if (label.equalsIgnoreCase("lspark")) {
            slot = "1";
         } else {
            if (!label.equalsIgnoreCase("rspark")) {
               return false;
            }

            slot = "2";
         }

         String equippedHack = Infuse.getInstance().getHackManager().getHack(playerUUID, slot);
         if (equippedHack == null) {
            return true;
         } else {
            String strippedHack = ChatColor.stripColor(equippedHack);
            this.activateAbility(player, strippedHack);
            return true;
         }
      }
   }

   @EventHandler
   public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
      Player player = event.getPlayer();
      if (player.hasPermission("ability.use")) {
         int abilityNumber = player.isSneaking() ? 1 : 2;
         UUID playerUUID = player.getUniqueId();
         String equippedHack = Infuse.getInstance().getHackManager().getHack(playerUUID, String.valueOf(abilityNumber));
         if (equippedHack != null) {
            String strippedHack = ChatColor.stripColor(equippedHack);
            event.setCancelled(true);
            this.activateAbility(player, strippedHack);
         }
      }
   }

   private void activateAbility(Player player, String abilityName) {
      String worldName = player.getWorld().getName().toLowerCase();
      List<String> disabledWorlds = Infuse.getInstance().getConfig().getStringList("configs.disabled_worlds");
      if (!disabledWorlds.stream().map(String::toLowerCase).anyMatch(w -> w.equals(worldName)) && abilityName.contains("Effect")) {
         boolean isAugmented = abilityName.startsWith("Augmented ");
         if (abilityName.equals("Apophis Effect") || abilityName.equals("Augmented Apophis Effect")) {
            if (CooldownManager.isOnCooldown(player.getUniqueId(), "apophis")) {
               return;
            }

            this.apophisAbility.activateSpark(player);
            if (isAugmented) {
               player.playSound(player.getLocation(), Sound.ENTITY_WARDEN_DEATH, 1.0F, 1.0F);
            } else {
               player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1.0F, 2.0F);
            }

            return;
         }

         switch (abilityName) {
            case "Emerald Effect":
            case "Augmented Emerald Effect":
               if (CooldownManager.isOnCooldown(player.getUniqueId(), "eme")) {
                  return;
               }

               this.emeraldAbility.activateSpark(player);
               if (isAugmented) {
                  player.playSound(player.getLocation(), Sound.ENTITY_WARDEN_DEATH, 1.0F, 1.0F);
               } else {
                  player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1.0F, 2.0F);
               }
               break;
            case "Feather Effect":
            case "Augmented Feather Effect":
               if (CooldownManager.isOnCooldown(player.getUniqueId(), "feather")) {
                  return;
               }

               this.featherAbility.activateSpark(player);
               if (isAugmented) {
                  player.playSound(player.getLocation(), Sound.ENTITY_WARDEN_DEATH, 1.0F, 1.0F);
               } else {
                  player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1.0F, 2.0F);
               }
               break;
            case "Fire Effect":
            case "Augmented Fire Effect":
               if (CooldownManager.isOnCooldown(player.getUniqueId(), "fire")) {
                  return;
               }

               this.fireAbility.activateSpark(player);
               if (isAugmented) {
                  player.playSound(player.getLocation(), Sound.ENTITY_WARDEN_DEATH, 1.0F, 1.0F);
               } else {
                  player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1.0F, 2.0F);
               }
               break;
            case "Frost Effect":
            case "Augmented Frost Effect":
               if (CooldownManager.isOnCooldown(player.getUniqueId(), "frost")) {
                  return;
               }

               this.frostAbility.activateSpark(player);
               if (isAugmented) {
                  player.playSound(player.getLocation(), Sound.ENTITY_WARDEN_DEATH, 1.0F, 1.0F);
               } else {
                  player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1.0F, 2.0F);
               }
               break;
            case "Haste Effect":
            case "Augmented Haste Effect":
               if (CooldownManager.isOnCooldown(player.getUniqueId(), "haste")) {
                  return;
               }

               this.hasteAbility.activateSpark(player);
               if (isAugmented) {
                  player.playSound(player.getLocation(), Sound.ENTITY_WARDEN_DEATH, 1.0F, 1.0F);
               } else {
                  player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1.0F, 2.0F);
               }
               break;
            case "Heart Effect":
            case "Augmented Heart Effect":
               if (CooldownManager.isOnCooldown(player.getUniqueId(), "heart")) {
                  return;
               }

               this.heartAbility.activateSpark(player);
               if (isAugmented) {
                  player.playSound(player.getLocation(), Sound.ENTITY_WARDEN_DEATH, 1.0F, 1.0F);
               } else {
                  player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1.0F, 2.0F);
               }
               break;
            case "Invis Effect":
            case "Augmented Invis Effect":
               if (CooldownManager.isOnCooldown(player.getUniqueId(), "invis")) {
                  return;
               }

               this.InvisAbility.activateSpark(player);
               if (isAugmented) {
                  player.playSound(player.getLocation(), Sound.ENTITY_WARDEN_DEATH, 1.0F, 1.0F);
               } else {
                  player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1.0F, 2.0F);
               }
               break;
            case "Ocean Effect":
            case "Augmented Ocean Effect":
               if (CooldownManager.isOnCooldown(player.getUniqueId(), "ocean")) {
                  return;
               }

               this.oceanAbility.activateSpark(player);
               if (isAugmented) {
                  player.playSound(player.getLocation(), Sound.ENTITY_WARDEN_DEATH, 1.0F, 1.0F);
               } else {
                  player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1.0F, 2.0F);
               }
               break;
            case "Regen Effect":
            case "Augmented Regen Effect":
               if (CooldownManager.isOnCooldown(player.getUniqueId(), "regen")) {
                  return;
               }

               this.regenAbility.activateSpark(player);
               if (isAugmented) {
                  player.playSound(player.getLocation(), Sound.ENTITY_WARDEN_DEATH, 1.0F, 1.0F);
               } else {
                  player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1.0F, 2.0F);
               }
               break;
            case "Speed Effect":
            case "Augmented Speed Effect":
               if (CooldownManager.isOnCooldown(player.getUniqueId(), "speed")) {
                  return;
               }

               this.speedAbility.activateSpark(player);
               if (isAugmented) {
                  player.playSound(player.getLocation(), Sound.ENTITY_WARDEN_DEATH, 1.0F, 1.0F);
               } else {
                  player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1.0F, 2.0F);
               }
               break;
            case "Strength Effect":
            case "Augmented Strength Effect":
               if (CooldownManager.isOnCooldown(player.getUniqueId(), "st")) {
                  return;
               }

               this.strengthAbility.activateSpark(player);
               if (isAugmented) {
                  player.playSound(player.getLocation(), Sound.ENTITY_WARDEN_DEATH, 1.0F, 1.0F);
               } else {
                  player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1.0F, 2.0F);
               }
               break;
            case "Thunder Effect":
            case "Augmented Thunder Effect":
               if (CooldownManager.isOnCooldown(player.getUniqueId(), "thunder")) {
                  return;
               }

               this.thunderAbility.activateSpark(player);
               if (isAugmented) {
                  player.playSound(player.getLocation(), Sound.ENTITY_WARDEN_DEATH, 1.0F, 1.0F);
               } else {
                  player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1.0F, 2.0F);
               }
               break;
            case "Ender Effect":
               if (CooldownManager.isOnCooldown(player.getUniqueId(), "dragon")) {
                  return;
               }

               this.dragon.activateSpark(player);
               player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1.0F, 2.0F);
               break;
            default:
               String var10001 = String.valueOf(ChatColor.RED);
               player.sendMessage(var10001 + "Unknown ability: " + abilityName);
         }
      }
   }
}
