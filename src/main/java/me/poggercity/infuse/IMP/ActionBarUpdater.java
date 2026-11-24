package me.poggercity.infuse.IMP;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import me.poggercity.infuse.Infuses.HexColorTranslator;
import me.poggercity.infuse.Managers.CooldownManager;
import me.poggercity.infuse.Managers.Infuse;
import me.poggercity.infuse.Managers.PlayerHackManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class ActionBarUpdater {
   int startdelay = Infuse.getInstance().getConfig().getInt("configs.action_bar_update_time", 1);
   private final PlayerHackManager hackManager = new PlayerHackManager();
   private final Set<UUID> playersWithActiveEffects = new HashSet<>();
   private final Map<String, String> hackKeys = new HashMap<>();

   public void start() {
      Bukkit.getGlobalRegionScheduler().runAtFixedRate(Infuse.getInstance(), task -> this.updateAllPlayers(), 1L, this.startdelay);
   }

   private void updateAllPlayers() {
      for (Player player : Bukkit.getOnlinePlayers()) {
         this.updateActionBarForPlayer(player);
      }
   }

   public ActionBarUpdater() {
      this.hackKeys.put("Strength Effect", "st");
      this.hackKeys.put("Speed Effect", "speed");
      this.hackKeys.put("Heart Effect", "health");
      this.hackKeys.put("Emerald Effect", "eme");
      this.hackKeys.put("Fire Effect", "fire_spark");
      this.hackKeys.put("Feather Effect", "feather");
      this.hackKeys.put("Haste Effect", "haste");
      this.hackKeys.put("Ocean Effect", "ocean");
      this.hackKeys.put("Invis Effect", "invis");
      this.hackKeys.put("Frost Effect", "frost");
      this.hackKeys.put("Regen Effect", "regen");
      this.hackKeys.put("Thunder Effect", "thunder");
      this.hackKeys.put("Ender Effect", "ender");
      this.hackKeys.put("Augmented Strength Effect", "st");
      this.hackKeys.put("Augmented Speed Effect", "speed");
      this.hackKeys.put("Augmented Heart Effect", "health");
      this.hackKeys.put("Augmented Emerald Effect", "eme");
      this.hackKeys.put("Augmented Fire Effect", "fire_spark");
      this.hackKeys.put("Augmented Feather Effect", "feather");
      this.hackKeys.put("Augmented Haste Effect", "haste");
      this.hackKeys.put("Augmented Ocean Effect", "ocean");
      this.hackKeys.put("Augmented Invis Effect", "invis");
      this.hackKeys.put("Augmented Frost Effect", "frost");
      this.hackKeys.put("Augmented Regen Effect", "regen");
      this.hackKeys.put("Augmented Thunder Effect", "thunder");
      this.hackKeys.put("Apophis Effect", "apophis");
      this.hackKeys.put("Augmented Apophis Effect", "apophis");
   }

   private String getHackColor(String hack) {
      if (!hack.equals("Apophis Effect") && !hack.equals("Augmented Apophis Effect")) {
         switch (hack) {
            case "Strength Effect":
            case "Augmented Strength Effect":
               return HexColorTranslator.translateToMinecraftColor("#9E2020");
            case "Heart Effect":
            case "Augmented Heart Effect":
               return ChatColor.RED.toString();
            case "Haste Effect":
            case "Augmented Haste Effect":
               return HexColorTranslator.translateToMinecraftColor("#B96C00");
            case "Invis Effect":
            case "Augmented Invis Effect":
               return HexColorTranslator.translateToMinecraftColor("#7E00FC");
            case "Feather Effect":
            case "Augmented Feather Effect":
               return HexColorTranslator.translateToMinecraftColor("#BEA3CA");
            case "Frost Effect":
            case "Augmented Frost Effect":
               return HexColorTranslator.translateToMinecraftColor("#7AE3FC");
            case "Thunder Effect":
            case "Augmented Thunder Effect":
               return HexColorTranslator.translateToMinecraftColor("#FCED00");
            case "Regen Effect":
            case "Augmented Regen Effect":
               return HexColorTranslator.translateToMinecraftColor("#FC00DD");
            case "Ocean Effect":
            case "Augmented Ocean Effect":
               return HexColorTranslator.translateToMinecraftColor("#005AFC");
            case "Fire Effect":
            case "Augmented Fire Effect":
               return HexColorTranslator.translateToMinecraftColor("#FC7803");
            case "Emerald Effect":
            case "Augmented Emerald Effect":
               return ChatColor.GREEN.toString();
            case "Speed Effect":
            case "Augmented Speed Effect":
               return HexColorTranslator.translateToMinecraftColor("#d1a44b");
            case "Ender Effect":
               return HexColorTranslator.translateToMinecraftColor("#3A0735");
            default:
               return ChatColor.GRAY.toString();
         }
      } else {
         return HexColorTranslator.translateToMinecraftColor("#301145");
      }
   }

   private void updateActionBarForPlayer(Player player) {
      UUID uuid = player.getUniqueId();
      if (!this.playersWithActiveEffects.contains(uuid)) {
         String commonHack = Infuse.getInstance().getHackManager().getHack(uuid, "1");
         String legendaryHack = Infuse.getInstance().getHackManager().getHack(uuid, "2");
         String firstTime = "";
         String firstEmoji = "";
         if (commonHack != null) {
            String secondTime = ChatColor.stripColor(commonHack);
            String key = this.hackKeys.get(secondTime);
            if (key != null) {
               String stripped = this.getHackColor(secondTime);
               if (CooldownManager.isEffectActive(uuid, key)) {
                  long timeLeftSec = CooldownManager.getEffectTimeLeft(uuid, key) / 1000L;
                  firstTime = this.formatTime(timeLeftSec, stripped);
                  firstEmoji = this.getDurationEmoji(secondTime);
               } else if (CooldownManager.isOnCooldown(uuid, key)) {
                  long timeLeftSec = CooldownManager.getCooldownTimeLeft(uuid, key) / 1000L;
                  firstTime = this.formatTime(timeLeftSec, ChatColor.WHITE.toString());
                  firstEmoji = this.getCooldownEmoji(secondTime);
               } else {
                  firstEmoji = this.getCooldownEmoji(secondTime);
               }
            }
         }

         String secondTime = "";
         String key = "";
         if (legendaryHack != null) {
            String stripped = ChatColor.stripColor(legendaryHack);
            String sidePadding = this.hackKeys.get(stripped);
            if (sidePadding != null) {
               String extraPadding = this.getHackColor(stripped);
               if (CooldownManager.isEffectActive(uuid, sidePadding)) {
                  long timeLeftSec = CooldownManager.getEffectTimeLeft(uuid, sidePadding) / 1000L;
                  secondTime = this.formatTime(timeLeftSec, extraPadding);
                  key = this.getDurationEmoji(stripped);
               } else if (CooldownManager.isOnCooldown(uuid, sidePadding)) {
                  long timeLeftSec = CooldownManager.getCooldownTimeLeft(uuid, sidePadding) / 1000L;
                  secondTime = this.formatTime(timeLeftSec, ChatColor.WHITE.toString());
                  key = this.getCooldownEmoji(stripped);
               } else {
                  key = this.getCooldownEmoji(stripped);
               }
            }
         }

         String stripped = "    ";
         String sidePadding = "    ";
         String extraPadding = "        ";
         String formattedFirstTime = firstTime.isEmpty() ? stripped : firstTime;
         String formattedFirstEmoji = firstEmoji.isEmpty() ? stripped : firstEmoji;
         String formattedSecondTime = secondTime.isEmpty() ? stripped : secondTime;
         String formattedSecondEmoji = key.isEmpty() ? stripped : key;
         boolean hasFirst = !firstTime.isEmpty() || !firstEmoji.isEmpty();
         boolean hasSecond = !secondTime.isEmpty() || !key.isEmpty();
         String actionBarMessage;
         if (hasFirst && hasSecond) {
            actionBarMessage = sidePadding
               + " "
               + formattedFirstTime
               + " "
               + formattedFirstEmoji
               + " "
               + formattedSecondEmoji
               + " "
               + formattedSecondTime
               + " "
               + sidePadding;
         } else if (hasFirst) {
            actionBarMessage = sidePadding + " " + formattedFirstTime + " " + formattedFirstEmoji + " " + extraPadding;
         } else if (hasSecond) {
            actionBarMessage = sidePadding + " " + formattedSecondTime + " " + formattedSecondEmoji + " " + extraPadding;
         } else {
            actionBarMessage = "";
         }

         actionBarMessage = actionBarMessage.trim();
         if (!actionBarMessage.isEmpty()) {
            player.sendActionBar(actionBarMessage);
         }
      }
   }

   private String formatTime(long totalSeconds, String color) {
      long minutes = totalSeconds / 60L;
      long seconds = totalSeconds % 60L;
      String timeString = minutes + ":" + String.format("%02d", seconds);
      return color + ChatColor.BOLD + timeString + ChatColor.RESET;
   }

   private String getCooldownEmoji(String hack) {
      if (hack.equals("Apophis Effect")) {
         return "\ue055";
      } else if (hack.equals("Augmented Apophis Effect")) {
         return "\ue057";
      } else {
         switch (hack) {
            case "Strength Effect":
               return "\ue002";
            case "Heart Effect":
               return "\ue003";
            case "Haste Effect":
               return "\ue004";
            case "Invis Effect":
               return "\ue005";
            case "Feather Effect":
               return "\ue006";
            case "Frost Effect":
               return "\ue007";
            case "Thunder Effect":
               return "\ue008";
            case "Regen Effect":
               return "\ue009";
            case "Ocean Effect":
               return "\ue010";
            case "Fire Effect":
               return "\ue011";
            case "Emerald Effect":
               return "\ue012";
            case "Speed Effect":
               return "\ue013";
            case "Ender Effect":
               return "\ue100";
            case "Augmented Strength Effect":
               return "\ue026";
            case "Augmented Heart Effect":
               return "\ue027";
            case "Augmented Haste Effect":
               return "\ue028";
            case "Augmented Invis Effect":
               return "\ue029";
            case "Augmented Feather Effect":
               return "\ue030";
            case "Augmented Frost Effect":
               return "\ue031";
            case "Augmented Thunder Effect":
               return "\ue032";
            case "Augmented Regen Effect":
               return "\ue033";
            case "Augmented Ocean Effect":
               return "\ue034";
            case "Augmented Fire Effect":
               return "\ue035";
            case "Augmented Emerald Effect":
               return "\ue036";
            case "Augmented Speed Effect":
               return "\ue037";
            default:
               return "";
         }
      }
   }

   private String getDurationEmoji(String hack) {
      if (hack.equals("Apophis Effect")) {
         return "\ue054";
      } else if (hack.equals("Augmented Apophis Effect")) {
         return "\ue056";
      } else {
         switch (hack) {
            case "Strength Effect":
               return "\ue014";
            case "Heart Effect":
               return "\ue015";
            case "Haste Effect":
               return "\ue016";
            case "Invis Effect":
               return "\ue017";
            case "Feather Effect":
               return "\ue018";
            case "Frost Effect":
               return "\ue019";
            case "Thunder Effect":
               return "\ue020";
            case "Regen Effect":
               return "\ue021";
            case "Ocean Effect":
               return "\ue022";
            case "Fire Effect":
               return "\ue023";
            case "Emerald Effect":
               return "\ue024";
            case "Speed Effect":
               return "\ue025";
            case "Ender Effect":
               return "\ue101";
            case "Augmented Strength Effect":
               return "\ue038";
            case "Augmented Heart Effect":
               return "\ue039";
            case "Augmented Haste Effect":
               return "\ue040";
            case "Augmented Invis Effect":
               return "\ue041";
            case "Augmented Feather Effect":
               return "\ue042";
            case "Augmented Frost Effect":
               return "\ue043";
            case "Augmented Thunder Effect":
               return "\ue044";
            case "Augmented Regen Effect":
               return "\ue045";
            case "Augmented Ocean Effect":
               return "\ue046";
            case "Augmented Fire Effect":
               return "\ue047";
            case "Augmented Emerald Effect":
               return "\ue048";
            case "Augmented Speed Effect":
               return "\ue049";
            default:
               return "";
         }
      }
   }
}
