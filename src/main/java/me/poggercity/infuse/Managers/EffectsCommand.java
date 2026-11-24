package me.poggercity.infuse.Managers;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import me.poggercity.infuse.IMP.HackMapping;
import me.poggercity.infuse.IMP.HackRerollListener;
import me.poggercity.infuse.Infuses.Effects;
import me.poggercity.infuse.Infuses.HexColorTranslator;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class EffectsCommand implements CommandExecutor, TabCompleter {
   private static final String EME = HexColorTranslator.translateToMinecraftColor("#0DFC00") + "Emerald Effect";
   private static final String FEATHER = HexColorTranslator.translateToMinecraftColor("#BEA3CA") + "Feather Effect";
   private static final String FIRE = HexColorTranslator.translateToMinecraftColor("#FC7803") + "Fire Effect";
   private static final String FROST = HexColorTranslator.translateToMinecraftColor("#7AE3FC") + "Frost Effect";
   private static final String HASTE = HexColorTranslator.translateToMinecraftColor("#B96C00") + "Haste Effect";
   private static final String HEART = HexColorTranslator.translateToMinecraftColor("#FC0046") + "Heart Effect";
   private static final String INVIS = HexColorTranslator.translateToMinecraftColor("#7E00FC") + "Invis Effect";
   private static final String OCEAN = HexColorTranslator.translateToMinecraftColor("#005AFC") + "Ocean Effect";
   private static final String REGEN = HexColorTranslator.translateToMinecraftColor("#FC00DD") + "Regen Effect";
   private static final String SPEED = HexColorTranslator.translateToMinecraftColor("#d1a44b") + "Speed Effect";
   private static final String ST = HexColorTranslator.translateToMinecraftColor("#9E2020") + "Strength Effect";
   private static final String THUNDER = HexColorTranslator.translateToMinecraftColor("#FCED00") + "Thunder Effect";
   private static final String ENDER = HexColorTranslator.translateToMinecraftColor("#541351") + "Ender Effect";
   private static final String APOPHIS = HexColorTranslator.translateToMinecraftColor("#301145") + "Apophis Effect";
   private static final List<String> ALL_KEYS = Arrays.asList(
      "emerald", "feather", "fire", "frost", "haste", "heart", "invis", "ocean", "regen", "speed", "strength", "thunder", "ender", "apophis"
   );
   private static final List<String> EFFECT_OPTIONS = Arrays.asList(
      "Fire",
      "Augment_Fire",
      "Augmented_Fire",
      "Frost",
      "Augment_Frost",
      "Augmented_Frost",
      "Emerald",
      "Augment_Emerald",
      "Augmented_Emerald",
      "Feather",
      "Augment_Feather",
      "Augmented_Feather",
      "Haste",
      "Augment_Haste",
      "Augmented_Haste",
      "Heart",
      "Augment_Heart",
      "Augmented_Heart",
      "Invis",
      "Augment_Invis",
      "Augmented_Invis",
      "Ocean",
      "Augment_Ocean",
      "Augmented_Ocean",
      "Regen",
      "Augment_Regen",
      "Augmented_Regen",
      "Speed",
      "Augment_Speed",
      "Augmented_Speed",
      "Strength",
      "Augment_Strength",
      "Augmented_Strength",
      "Ender",
      "Augment_Ender",
      "Augmented_Ender",
      "Apophis",
      "Augment_Apophis",
      "Augmented_Apophis",
      "fire",
      "augment_fire",
      "augmented_fire",
      "frost",
      "augment_frost",
      "augmented_frost",
      "emerald",
      "augment_emerald",
      "augmented_emerald",
      "feather",
      "augment_feather",
      "augmented_feather",
      "haste",
      "augment_haste",
      "augmented_haste",
      "heart",
      "augment_heart",
      "augmented_heart",
      "invis",
      "augment_invis",
      "augmented_invis",
      "ocean",
      "augment_ocean",
      "augmented_ocean",
      "regen",
      "augment_regen",
      "augmented_regen",
      "speed",
      "augment_speed",
      "augmented_speed",
      "strength",
      "augment_strength",
      "augmented_strength",
      "ender",
      "augment_ender",
      "augmented_ender",
      "apophis",
      "augment_apophis",
      "augmented_apophis"
   );
   private static final List<String> START_RITUAL_KEYS = Arrays.asList(
      "emerald",
      "feather",
      "fire",
      "frost",
      "haste",
      "heart",
      "invis",
      "ocean",
      "regen",
      "speed",
      "strength",
      "thunder",
      "ender",
      "apophis",
      "augment_emerald",
      "augmented_emerald",
      "augment_feather",
      "augmented_feather",
      "augment_fire",
      "augmented_fire",
      "augment_frost",
      "augmented_frost",
      "augment_haste",
      "augmented_haste",
      "augment_heart",
      "augmented_heart",
      "augment_invis",
      "augmented_invis",
      "augment_ocean",
      "augmented_ocean",
      "augment_regen",
      "augmented_regen",
      "augment_speed",
      "augmented_speed",
      "augment_strength",
      "augmented_strength",
      "augment_thunder",
      "augmented_thunder",
      "augment_ender",
      "augmented_ender",
      "augment_apophis",
      "augmented_apophis"
   );
   private final JavaPlugin plugin;
   private final InfuseRecipeManager recipeManager;
   private final HackRerollListener ui;

   public EffectsCommand(JavaPlugin plugin, InfuseRecipeManager recipeManager, HackRerollListener ui) {
      this.plugin = plugin;
      this.recipeManager = recipeManager;
      this.ui = ui;
   }

   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      String cmd = command.getName().toLowerCase(Locale.ROOT);
      if (cmd.equals("start_ritual")) {
         if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
         } else if (!sender.hasPermission("infuse.startritual")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
         } else if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /start_ritual <effectKey> [x y z]");
            return true;
         } else {
            Player player = (Player)sender;
            String key = args[0].toLowerCase(Locale.ROOT);
            Location loc;
            if (args.length >= 4) {
               try {
                  double x = Double.parseDouble(args[1]);
                  double y = Double.parseDouble(args[2]);
                  double z = Double.parseDouble(args[3]);
                  loc = new Location(player.getWorld(), x, y, z);
               } catch (NumberFormatException var15) {
                  sender.sendMessage(ChatColor.RED + "Coordinates must be numbers. Usage: /start_ritual <effectKey> [x y z]");
                  return true;
               }
            } else {
               loc = player.getLocation().getBlock().getLocation().add(0.5, 0.0, 0.5);
            }

            if (loc.getBlock().getType() != Material.BREWING_STAND) {
               sender.sendMessage(ChatColor.RED + "Stand on (or specify) a brewing stand block.");
               return true;
            } else {
               boolean ok = this.recipeManager.triggerRitualAt(player, key, loc);
               if (!ok) {
                  sender.sendMessage(ChatColor.RED + "Failed to start ritual. Is the key valid, and is there no ritual running already?");
               }

               return true;
            }
         }
      } else if (cmd.equals("check_effects")) {
         if (!sender.hasPermission("infuse.checkeffects")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
         } else if (args.length != 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /check_effects <player>");
            return true;
         } else {
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
               sender.sendMessage(ChatColor.RED + "Player not found.");
               return true;
            } else {
               String hack1Raw = Infuse.getInstance().getHackManager().getHack(target.getUniqueId(), "1");
               String hack2Raw = Infuse.getInstance().getHackManager().getHack(target.getUniqueId(), "2");
               String slot1 = this.colorizeByHackName(hack1Raw);
               String slot2 = this.colorizeByHackName(hack2Raw);
               sender.sendMessage(ChatColor.DARK_GRAY + "──────── " + ChatColor.AQUA + target.getName() + ChatColor.DARK_GRAY + " — Effects ────────");
               sender.sendMessage(ChatColor.GRAY + "Slot 1: " + (slot1 == null ? ChatColor.DARK_GRAY + "None" : slot1));
               sender.sendMessage(ChatColor.GRAY + "Slot 2: " + (slot2 == null ? ChatColor.DARK_GRAY + "None" : slot2));
               return true;
            }
         }
      } else if (cmd.equals("give_effects")) {
         if (!sender.hasPermission("infuse.giveeffects")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
         } else if (args.length != 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /give_effects <player|@a> <effect>");
            return true;
         } else {
            String canonical = this.toCanonicalHackName(args[1]);
            if (canonical == null) {
               sender.sendMessage(ChatColor.RED + "Unknown effect: " + args[1]);
               sender.sendMessage(
                  ChatColor.GRAY + "Try: " + ChatColor.AQUA + String.join(ChatColor.GRAY + ", " + ChatColor.AQUA, this.shortlistOptions(args[1]))
               );
               return true;
            } else {
               ItemStack item = null;
               HackMapping mapping = HackMapping.fromHackName(canonical);
               if (mapping != null) {
                  item = mapping.createItem();
               } else if (canonical.equalsIgnoreCase("Apophis Effect")) {
                  item = Effects.createAPOPHIS();
               }

               if (item == null) {
                  sender.sendMessage(ChatColor.RED + "Unknown effect: " + canonical);
                  return true;
               } else if (!args[0].equalsIgnoreCase("@a")) {
                  Player target = Bukkit.getPlayer(args[0]);
                  if (target == null) {
                     sender.sendMessage(ChatColor.RED + "Player not found.");
                     return true;
                  } else {
                     target.getInventory().addItem(new ItemStack[]{item});
                     sender.sendMessage(ChatColor.GREEN + "Gave " + ChatColor.YELLOW + canonical + ChatColor.GREEN + " to " + ChatColor.AQUA + target.getName());
                     return true;
                  }
               } else {
                  int count = 0;

                  for (Player p : Bukkit.getOnlinePlayers()) {
                     p.getInventory().addItem(new ItemStack[]{item.clone()});
                     count++;
                  }

                  sender.sendMessage(
                     ChatColor.GREEN + "Gave " + ChatColor.YELLOW + canonical + ChatColor.GREEN + " to " + ChatColor.AQUA + count + " player(s)"
                  );
                  return true;
               }
            }
         }
      } else if (!(sender instanceof Player playerx)) {
         sender.sendMessage(ChatColor.RED + "Only players can use this command.");
         return true;
      } else if (!cmd.equals("craftedeffects")) {
         if (args.length == 0) {
            this.ui.openInfuses(playerx);
            return true;
         } else {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (!sub.equals("aug") && !sub.equals("augments")) {
               playerx.sendMessage(
                  ChatColor.GRAY
                     + "Usage: "
                     + ChatColor.AQUA
                     + "/"
                     + label
                     + ChatColor.GRAY
                     + " "
                     + ChatColor.YELLOW
                     + "[aug|augments]"
                     + ChatColor.GRAY
                     + " — default opens the Effects GUI."
               );
               return true;
            } else {
               this.ui.openAugmentedInfuses(playerx);
               return true;
            }
         }
      } else {
         playerx.sendMessage(ChatColor.DARK_PURPLE + "ᴄʀᴀꜰᴛᴇᴅ ᴇꜰꜰᴇᴄᴛꜱ:");
         playerx.sendMessage(ChatColor.DARK_GRAY + "──────────────────────────");

         for (String keyx : ALL_KEYS) {
            String displayName = this.capitalizeEffectName(keyx);
            String statusLine = this.safeFormatEffectStatus(keyx);
            playerx.sendMessage("• " + displayName + " " + statusLine);
         }

         return true;
      }
   }

   public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
      String cmd = command.getName().toLowerCase(Locale.ROOT);
      if (cmd.equals("check_effects")) {
         return args.length == 1
            ? Bukkit.getOnlinePlayers()
               .stream()
               .<String>map(Player::getName)
               .filter(n -> n.toLowerCase(Locale.ROOT).startsWith(args[0].toLowerCase(Locale.ROOT)))
               .sorted()
               .collect(Collectors.toList())
            : Collections.emptyList();
      } else if (cmd.equals("start_ritual")) {
         return args.length == 1
            ? START_RITUAL_KEYS.stream().filter(k -> k.startsWith(args[0].toLowerCase(Locale.ROOT))).sorted().collect(Collectors.toList())
            : Collections.emptyList();
      } else if (cmd.equals("give_effects")) {
         if (args.length == 1) {
            List<String> suggestions = Bukkit.getOnlinePlayers()
               .stream()
               .<String>map(Player::getName)
               .filter(n -> n.toLowerCase(Locale.ROOT).startsWith(args[0].toLowerCase(Locale.ROOT)))
               .collect(Collectors.toList());
            if ("@a".startsWith(args[0].toLowerCase(Locale.ROOT))) {
               suggestions.add("@a");
            }

            return suggestions.stream().sorted().collect(Collectors.toList());
         } else {
            return args.length == 2
               ? EFFECT_OPTIONS.stream()
                  .filter(opt -> opt.toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT)))
                  .sorted()
                  .collect(Collectors.toList())
               : Collections.emptyList();
         }
      } else {
         return Collections.emptyList();
      }
   }

   private String colorizeByHackName(String hackName) {
      if (hackName != null && !hackName.isEmpty()) {
         String s = ChatColor.stripColor(hackName).trim();
         String lower = s.toLowerCase(Locale.ROOT);
         boolean isAugmented = lower.contains("augmented");
         String baseColored;
         if (lower.contains("emerald")) {
            baseColored = EME;
         } else if (lower.contains("feather")) {
            baseColored = FEATHER;
         } else if (lower.contains("fire")) {
            baseColored = FIRE;
         } else if (lower.contains("frost")) {
            baseColored = FROST;
         } else if (lower.contains("haste")) {
            baseColored = HASTE;
         } else if (lower.contains("heart")) {
            baseColored = HEART;
         } else if (lower.contains("invis")) {
            baseColored = INVIS;
         } else if (lower.contains("ocean")) {
            baseColored = OCEAN;
         } else if (lower.contains("regen")) {
            baseColored = REGEN;
         } else if (lower.contains("speed")) {
            baseColored = SPEED;
         } else if (lower.equals("st") || lower.contains("strength")) {
            baseColored = ST;
         } else if (lower.contains("thunder")) {
            baseColored = THUNDER;
         } else if (lower.contains("ender")) {
            baseColored = ENDER;
         } else if (lower.contains("apophis")) {
            baseColored = APOPHIS;
         } else {
            baseColored = ChatColor.AQUA + s;
         }

         return isAugmented && !lower.startsWith("augmented") ? ChatColor.DARK_PURPLE + "Augmented " + baseColored : baseColored;
      } else {
         return null;
      }
   }

   private String toCanonicalHackName(String raw) {
      if (raw != null && !raw.isEmpty()) {
         String s = ChatColor.stripColor(raw).trim();
         s = s.replaceAll("\\s*\\[[^\\]]*]$", "");
         s = s.replace('_', ' ').replace('-', ' ').replaceAll("\\s+", " ").trim();
         boolean augmented = false;
         String lower = s.toLowerCase(Locale.ROOT);
         if (lower.startsWith("augment ")) {
            augmented = true;
            s = s.substring("augment ".length()).trim();
         } else if (lower.startsWith("augmented ")) {
            augmented = true;
            s = s.substring("augmented ".length()).trim();
         }

         String baseLower = s.toLowerCase(Locale.ROOT);
         String base;
         if (baseLower.equals("st") || baseLower.equals("strength")) {
            base = "Strength";
         } else if (baseLower.equals("ender")) {
            base = "Ender";
         } else if (baseLower.equals("apophis")) {
            base = "Apophis";
         } else {
            base = this.capitalizeWord(s);
         }

         String hack = base + " Effect";
         if (augmented) {
            hack = "Augmented " + hack;
         }

         return hack;
      } else {
         return null;
      }
   }

   private String capitalizeWord(String s) {
      return s.isEmpty() ? s : s.substring(0, 1).toUpperCase(Locale.ROOT) + s.substring(1).toLowerCase(Locale.ROOT);
   }

   private List<String> shortlistOptions(String typed) {
      String t = typed == null ? "" : typed.toLowerCase(Locale.ROOT);
      return EFFECT_OPTIONS.stream().filter(o -> o.toLowerCase(Locale.ROOT).startsWith(t)).limit(6L).collect(Collectors.toList());
   }

   private String safeFormatEffectStatus(String key) {
      int usedAug = 0;
      int maxAug = 0;
      int totalLimit = 0;
      int leftNormal = 0;

      try {
         usedAug = this.recipeManager.getUsedAugments(key);
      } catch (Exception var12) {
      }

      try {
         maxAug = this.recipeManager.getMaxAugments(key);
      } catch (Exception var11) {
      }

      try {
         totalLimit = this.recipeManager.getCraftLimit(key);
      } catch (Exception var10) {
      }

      try {
         leftNormal = this.recipeManager.getCraftsLeft(key);
      } catch (Exception var9) {
      }

      if (maxAug < 0) {
         maxAug = 0;
      }

      if (totalLimit < 0) {
         totalLimit = 0;
      }

      if (leftNormal < 0) {
         leftNormal = 0;
      }

      int usedNormal = Math.max(0, totalLimit - leftNormal);
      String augColor = this.getColorByRemainingRatio(Math.max(0, maxAug - usedAug), Math.max(1, maxAug));
      String normalColor = this.getColorByRemainingRatio(leftNormal, Math.max(1, totalLimit));
      return ChatColor.LIGHT_PURPLE
         + "| ᴀᴜɢᴍᴇɴᴛᴇᴅ: "
         + augColor
         + usedAug
         + "/"
         + maxAug
         + ChatColor.GRAY
         + " | "
         + ChatColor.AQUA
         + "ɴᴏʀᴍᴀʟ: "
         + normalColor
         + usedNormal
         + "/"
         + totalLimit;
   }

   private String getColorByRemainingRatio(int remaining, int total) {
      if (total <= 0) {
         return ChatColor.RED.toString();
      } else {
         double ratio = (double)remaining / total;
         if (ratio == 1.0) {
            return ChatColor.GREEN.toString();
         } else if (ratio >= 0.75) {
            return ChatColor.DARK_GREEN.toString();
         } else if (ratio >= 0.5) {
            return ChatColor.YELLOW.toString();
         } else {
            return ratio >= 0.25 ? ChatColor.GOLD.toString() : ChatColor.RED.toString();
         }
      }
   }

   private String capitalizeEffectName(String key) {
      if (key != null && !key.isEmpty()) {
         String lower = key.toLowerCase(Locale.ROOT);
         switch (lower) {
            case "emerald":
               return EME;
            case "feather":
               return FEATHER;
            case "fire":
               return FIRE;
            case "frost":
               return FROST;
            case "haste":
               return HASTE;
            case "heart":
               return HEART;
            case "invis":
               return INVIS;
            case "ocean":
               return OCEAN;
            case "regen":
               return REGEN;
            case "speed":
               return SPEED;
            case "strength":
            case "st":
               return ST;
            case "thunder":
               return THUNDER;
            case "ender":
               return ENDER;
            case "apophis":
               return APOPHIS;
            default:
               return key.substring(0, 1).toUpperCase(Locale.ROOT) + key.substring(1).toLowerCase(Locale.ROOT) + " Effect";
         }
      } else {
         return key;
      }
   }
}
