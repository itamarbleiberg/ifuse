package me.poggercity.infuse.Infuses;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public class Effects {
   private static JavaPlugin PLUGIN;
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
   private static final String DRAGON = HexColorTranslator.translateToMinecraftColor("#541351") + "Ender Effect";
   private static final String APOPHIS = HexColorTranslator.translateToMinecraftColor("#301145") + "Apophis Effect";
   private static final String INVIS_COLOR = HexColorTranslator.translateToMinecraftColor("#5800B0");
   private static final String HASTE_COLOR = HexColorTranslator.translateToMinecraftColor("#804B00");
   private static final String REGEN_COLOR = HexColorTranslator.translateToMinecraftColor("#B0009A");
   private static final String FEATHER_COLOR = HexColorTranslator.translateToMinecraftColor("#84728C");
   private static final String THUNDER_COLOR = HexColorTranslator.translateToMinecraftColor("#B0A600");
   private static final String STRENGTH_COLOR = HexColorTranslator.translateToMinecraftColor("#6F1616");
   private static final String FROST_COLOR = HexColorTranslator.translateToMinecraftColor("#559FB0");
   private static final String HEART_COLOR = HexColorTranslator.translateToMinecraftColor("#B00030");
   private static final String EMERALD_COLOR = HexColorTranslator.translateToMinecraftColor("#09B000");
   private static final String OCEAN_COLOR = HexColorTranslator.translateToMinecraftColor("#003EB0");
   private static final String FIRE_COLOR = HexColorTranslator.translateToMinecraftColor("#B05302");
   private static final String SPEED_COLOR = HexColorTranslator.translateToMinecraftColor("#b5883a");
   private static final String DRAGON_COLOR = HexColorTranslator.translateToMinecraftColor("#3A0735");
   private static final String APOPHIS_COLOR = HexColorTranslator.translateToMinecraftColor("#301145");
   private static final String INVIS_COLOR2 = HexColorTranslator.translateToMinecraftColor("#7E00FC");
   private static final String HASTE_COLOR2 = HexColorTranslator.translateToMinecraftColor("#B96C00");
   private static final String REGEN_COLOR2 = HexColorTranslator.translateToMinecraftColor("#FC00DD");
   private static final String FEATHER_COLOR2 = HexColorTranslator.translateToMinecraftColor("#BEA3CA");
   private static final String THUNDER_COLOR2 = HexColorTranslator.translateToMinecraftColor("#FCED00");
   private static final String STRENGTH_COLOR2 = HexColorTranslator.translateToMinecraftColor("#9E2020");
   private static final String FROST_COLOR2 = HexColorTranslator.translateToMinecraftColor("#7AE3FC");
   private static final String HEART_COLOR2 = HexColorTranslator.translateToMinecraftColor("#FC0046");
   private static final String EMERALD_COLOR2 = HexColorTranslator.translateToMinecraftColor("#0DFC00");
   private static final String OCEAN_COLOR2 = HexColorTranslator.translateToMinecraftColor("#005AFC");
   private static final String FIRE_COLOR2 = HexColorTranslator.translateToMinecraftColor("#FC7803");
   private static final String SPEED_COLOR2 = HexColorTranslator.translateToMinecraftColor("#d1a44b");
   private static final String DRAGON_COLOR2 = HexColorTranslator.translateToMinecraftColor("#541351");

   public static void init(JavaPlugin plugin) {
      PLUGIN = plugin;
   }

   public static ItemStack createEME() {
      ItemStack gem = new ItemStack(Material.POTION);
      PotionMeta meta = (PotionMeta)gem.getItemMeta();
      if (meta != null) {
         meta.setDisplayName(EME);
         meta.setColor(Color.fromRGB(0, 255, 0));
         List<String> lore = new ArrayList<>();
         String var10001 = EMERALD_COLOR;
         lore.add(var10001 + ChatColor.BOLD + "ᴘᴀꜱꜱɪᴠᴇ ᴇꜰꜰᴇᴄᴛꜱ:");
         lore.add(EMERALD_COLOR2 + "\ud83d\udcb2 §x§6§D§6§D§6§DConsumables have a 50% chance of not being consumed");
         lore.add(EMERALD_COLOR2 + "\ud83d\udcb2 §x§6§D§6§D§6§DEnchanting table always on level 30");
         lore.add(EMERALD_COLOR2 + "\ud83d\udcb2 §x§6§D§6§D§6§DGain 3x exp");
         lore.add(EMERALD_COLOR2 + "\ud83d\udcb2 §x§6§D§6§D§6§DGives looting 5 automatically");
         lore.add(ChatColor.GRAY.toString());
         var10001 = EMERALD_COLOR;
         lore.add(var10001 + ChatColor.BOLD + "ꜱᴘᴀʀᴋ ᴇꜰꜰᴇᴄᴛꜱ:");
         lore.add(EMERALD_COLOR2 + "\ud83d\udcb2 §x§6§D§6§D§6§DHero of the village 255");
         lore.add(ChatColor.GRAY.toString());
         lore.add(HexColorTranslator.translateToMinecraftColor("#007DD0") + "ᴅᴜʀᴀᴛɪᴏɴ: 10s");
         lore.add(HexColorTranslator.translateToMinecraftColor("#007DD0") + "ᴄᴏᴏʟᴅᴏᴡɴ: 30s");
         meta.setLore(lore);
         meta.setCustomModelData(1);
         meta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ADDITIONAL_TOOLTIP});
         gem.setItemMeta(meta);
      }

      return gem;
   }

   public static ItemStack createFEATHER() {
      ItemStack gem = new ItemStack(Material.POTION);
      PotionMeta meta = (PotionMeta)gem.getItemMeta();
      if (meta != null) {
         meta.setDisplayName(FEATHER);
         meta.setColor(Color.fromRGB(190, 163, 202));
         List<String> lore = new ArrayList<>();
         String var10001 = FEATHER_COLOR;
         lore.add(var10001 + ChatColor.BOLD + "ᴘᴀꜱꜱɪᴠᴇ ᴇꜰꜰᴇᴄᴛꜱ:");
         lore.add(FEATHER_COLOR2 + "☁ §x§6§D§6§D§6§DNo Fall damage");
         lore.add(FEATHER_COLOR2 + "☁ §x§6§D§6§D§6§DAttacking from a 7+ block fall does 1.5x damage");
         lore.add(FEATHER_COLOR2 + "☁ §x§6§D§6§D§6§DAuto windcharge counter after being attacked 10 times");
         lore.add(FEATHER_COLOR2 + "☁ §x§6§D§6§D§6§DWindcharges have 0.5x cooldown");
         lore.add(FEATHER_COLOR2 + "☁ §x§6§D§6§D§6§DWindcharges have 2x velocity");
         lore.add(FEATHER_COLOR2 + "☁ §x§6§D§6§D§6§DWindcharges go perfectly straight");
         lore.add(ChatColor.GRAY.toString());
         var10001 = FEATHER_COLOR;
         lore.add(var10001 + ChatColor.BOLD + "ꜱᴘᴀʀᴋ ᴇꜰꜰᴇᴄᴛꜱ:");
         lore.add(FEATHER_COLOR2 + "☁ §x§6§D§6§D§6§DLaunches the player upward");
         lore.add(FEATHER_COLOR2 + "☁ §x§6§D§6§D§6§DSlams back into the floor damaging nearby players");
         lore.add(ChatColor.GRAY.toString());
         lore.add(HexColorTranslator.translateToMinecraftColor("#007DD0") + "ᴅᴜʀᴀᴛɪᴏɴ: 5s");
         lore.add(HexColorTranslator.translateToMinecraftColor("#007DD0") + "ᴄᴏᴏʟᴅᴏᴡɴ: 40s");
         meta.setLore(lore);
         meta.setCustomModelData(2);
         meta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ADDITIONAL_TOOLTIP});
         gem.setItemMeta(meta);
      }

      return gem;
   }

   public static ItemStack createFIRE() {
      ItemStack gem = new ItemStack(Material.POTION);
      PotionMeta meta = (PotionMeta)gem.getItemMeta();
      if (meta != null) {
         meta.setDisplayName(FIRE);
         meta.setColor(Color.fromRGB(252, 120, 3));
         List<String> lore = new ArrayList<>();
         String var10001 = FIRE_COLOR;
         lore.add(var10001 + ChatColor.BOLD + "ᴘᴀꜱꜱɪᴠᴇ ᴇꜰꜰᴇᴄᴛꜱ:");
         lore.add(FIRE_COLOR2 + "\ud83d\udd25 §x§6§D§6§D§6§DFire Resistance");
         lore.add(FIRE_COLOR2 + "\ud83d\udd25 §x§6§D§6§D§6§DFly through lava");
         lore.add(FIRE_COLOR2 + "\ud83d\udd25 §x§6§D§6§D§6§DSwim in Lava");
         lore.add(FIRE_COLOR2 + "\ud83d\udd25 §x§6§D§6§D§6§DAuto Smelting on all blocks");
         lore.add(FIRE_COLOR2 + "\ud83d\udd25 §x§6§D§6§D§6§DSet players on fire every 10 hits");
         lore.add(FIRE_COLOR2 + "\ud83d\udd25 §x§6§D§6§D§6§DFire damage increases over time");
         lore.add(FIRE_COLOR2 + "\ud83d\udd25 §x§6§D§6§D§6§DEvaporate water");
         lore.add(FIRE_COLOR2 + "\ud83d\udd25 §x§6§D§6§D§6§DLava buckets cancel fall damage");
         lore.add(ChatColor.GRAY.toString());
         var10001 = FIRE_COLOR;
         lore.add(var10001 + ChatColor.BOLD + "ꜱᴘᴀʀᴋ ᴇꜰꜰᴇᴄᴛ:");
         lore.add(FIRE_COLOR2 + "\ud83d\udd25 §x§6§D§6§D§6§DIgnite all players within 5 blocks");
         lore.add(FIRE_COLOR2 + "\ud83d\udd25 §x§6§D§6§D§6§DFinal Ignition erupts and launches all players upward");
         lore.add(ChatColor.GRAY.toString());
         lore.add(HexColorTranslator.translateToMinecraftColor("#007DD0") + "ᴅᴜʀᴀᴛɪᴏɴ: 5s");
         lore.add(HexColorTranslator.translateToMinecraftColor("#007DD0") + "ᴄᴏᴏʟᴅᴏᴡɴ: 60s");
         meta.setLore(lore);
         meta.setCustomModelData(3);
         meta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ADDITIONAL_TOOLTIP});
         gem.setItemMeta(meta);
      }

      return gem;
   }

   public static ItemStack createFROST() {
      ItemStack gem = new ItemStack(Material.POTION);
      PotionMeta meta = (PotionMeta)gem.getItemMeta();
      if (meta != null) {
         meta.setDisplayName(FROST);
         meta.setColor(Color.fromRGB(0, 255, 255));
         List<String> lore = new ArrayList<>();
         String var10001 = FROST_COLOR;
         lore.add(var10001 + ChatColor.BOLD + "ᴘᴀꜱꜱɪᴠᴇ ᴇꜰꜰᴇᴄᴛꜱ:");
         lore.add(FROST_COLOR2 + "❄ §x§6§D§6§D§6§DFreezes players every 10th hit");
         lore.add(FROST_COLOR2 + "❄ §x§6§D§6§D§6§DFreezes mobs every hit");
         lore.add(FROST_COLOR2 + "❄ §x§6§D§6§D§6§DIncreased speed on all ice and snow blocks");
         lore.add(FROST_COLOR2 + "❄ §x§6§D§6§D§6§DWind charge cooldown increased to 3 seconds");
         lore.add(FROST_COLOR2 + "❄ §x§6§D§6§D§6§DFly through powdered snow");
         lore.add(ChatColor.DARK_PURPLE.toString());
         var10001 = FROST_COLOR;
         lore.add(var10001 + ChatColor.BOLD + "ꜱᴘᴀʀᴋ ᴇꜰꜰᴇᴄᴛꜱ:");
         lore.add(FROST_COLOR2 + "❄ §x§6§D§6§D§6§DFreezes players on every hit");
         lore.add(FROST_COLOR2 + "❄ §x§6§D§6§D§6§DReduces jump height of enemies on every hit");
         lore.add(ChatColor.GRAY.toString());
         lore.add(HexColorTranslator.translateToMinecraftColor("#007DD0") + "ᴅᴜʀᴀᴛɪᴏɴ: 10s");
         lore.add(HexColorTranslator.translateToMinecraftColor("#007DD0") + "ᴄᴏᴏʟᴅᴏᴡɴ: 90s");
         meta.setLore(lore);
         meta.setCustomModelData(4);
         meta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ADDITIONAL_TOOLTIP});
         gem.setItemMeta(meta);
      }

      return gem;
   }

   public static ItemStack createTHUNDER() {
      ItemStack gem = new ItemStack(Material.POTION);
      PotionMeta meta = (PotionMeta)gem.getItemMeta();
      if (meta != null) {
         meta.setDisplayName(THUNDER);
         meta.setColor(Color.fromRGB(252, 237, 0));
         List<String> lore = new ArrayList<>();
         String var10001 = THUNDER_COLOR;
         lore.add(var10001 + ChatColor.BOLD + "ᴘᴀꜱꜱɪᴠᴇ ᴇꜰꜰᴇᴄᴛꜱ:");
         lore.add(THUNDER_COLOR2 + "⚡ §x§6§D§6§D§6§DSmite players every 10th hit (5th hit if its raining)");
         lore.add(THUNDER_COLOR2 + "⚡ §x§6§D§6§D§6§DThunder chains to nearby enemies");
         lore.add(THUNDER_COLOR2 + "⚡ §x§6§D§6§D§6§DTridents smite enemies when thrown");
         lore.add(ChatColor.GRAY.toString());
         var10001 = THUNDER_COLOR;
         lore.add(var10001 + ChatColor.BOLD + "ꜱᴘᴀʀᴋ ᴇꜰꜱᴇᴄᴛꜱ:");
         lore.add(THUNDER_COLOR2 + "⚡ §x§6§D§6§D§6§DSmite all players within 10 blocks");
         lore.add(THUNDER_COLOR2 + "⚡ §x§6§D§6§D§6§DEach player can be only smited 3 times");
         lore.add(ChatColor.DARK_GRAY.toString());
         lore.add(HexColorTranslator.translateToMinecraftColor("#007DD0") + "ᴅᴜʀᴀᴛɪᴏɴ: 10s");
         lore.add(HexColorTranslator.translateToMinecraftColor("#007DD0") + "ᴄᴏᴏʟᴅᴏᴡɴ: 90s");
         meta.setLore(lore);
         meta.setCustomModelData(13);
         meta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ADDITIONAL_TOOLTIP});
         gem.setItemMeta(meta);
      }

      return gem;
   }

   public static ItemStack createHASTE() {
      ItemStack gem = new ItemStack(Material.POTION);
      PotionMeta meta = (PotionMeta)gem.getItemMeta();
      if (meta != null) {
         meta.setDisplayName(HASTE);
         meta.setColor(Color.fromRGB(185, 108, 0));
         List<String> lore = new ArrayList<>();
         String var10001 = HASTE_COLOR;
         lore.add(var10001 + ChatColor.BOLD + "ᴘᴀꜱꜱɪᴠᴇ ᴇꜰꜰᴇᴄᴛꜱ:");
         lore.add(HASTE_COLOR2 + "⛏ §x§6§D§6§D§6§DFortune 5");
         lore.add(HASTE_COLOR2 + "⛏ §x§6§D§6§D§6§DUnbreaking 5");
         lore.add(HASTE_COLOR2 + "⛏ §x§6§D§6§D§6§DEfficiency 10");
         lore.add(HASTE_COLOR2 + "⛏ §x§6§D§6§D§6§DHaste 2");
         lore.add(HASTE_COLOR2 + "⛏ §x§6§D§6§D§6§DVein miner when crouched");
         lore.add(HASTE_COLOR2 + "⛏ §x§6§D§6§D§6§DShield cooldown is halved");
         lore.add(ChatColor.GRAY.toString());
         var10001 = HASTE_COLOR;
         lore.add(var10001 + ChatColor.BOLD + "ꜱᴘᴀʀᴋ ᴇꜰꜰᴇᴄᴛꜱ:");
         lore.add(HASTE_COLOR2 + "⛏ §x§6§D§6§D§6§DAttack faster");
         lore.add(HASTE_COLOR2 + "⛏ §x§6§D§6§D§6§DMine faster");
         lore.add(ChatColor.GRAY.toString());
         lore.add(HexColorTranslator.translateToMinecraftColor("#007DD0") + "ᴅᴜʀᴀᴛɪᴏɴ: 15s");
         lore.add(HexColorTranslator.translateToMinecraftColor("#007DD0") + "ᴄᴏᴏʟᴅᴏᴡɴ: 60s");
         meta.setLore(lore);
         meta.setCustomModelData(5);
         meta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ADDITIONAL_TOOLTIP});
         gem.setItemMeta(meta);
      }

      return gem;
   }

   public static ItemStack createHEART() {
      ItemStack gem = new ItemStack(Material.POTION);
      PotionMeta meta = (PotionMeta)gem.getItemMeta();
      if (meta != null) {
         meta.setDisplayName(HEART);
         meta.setColor(Color.fromRGB(252, 0, 70));
         List<String> lore = new ArrayList<>();
         String var10001 = HEART_COLOR;
         lore.add(var10001 + ChatColor.BOLD + "ᴘᴀꜱꜱɪᴠᴇ ᴇꜰꜰᴇᴄᴛꜱ:");
         lore.add(HEART_COLOR2 + "❤ §x§6§D§6§D§6§D+5 Hearts");
         lore.add(HEART_COLOR2 + "❤ §x§6§D§6§D§6§DAll food gives absorption");
         lore.add(HEART_COLOR2 + "❤ §x§6§D§6§D§6§DEgaps gives +10 absorption hearts");
         lore.add(HEART_COLOR2 + "❤ §x§6§D§6§D§6§DSee player's health every 10 hits");
         lore.add(ChatColor.GRAY.toString());
         var10001 = HEART_COLOR;
         lore.add(var10001 + ChatColor.BOLD + "ꜱᴘᴀʀᴋ ᴇꜰꜰᴇᴄᴛꜱ:");
         lore.add(HEART_COLOR2 + "❤ §x§6§D§6§D§6§DHeal players to 20 hearts instantly");
         lore.add(ChatColor.GRAY.toString());
         lore.add(HexColorTranslator.translateToMinecraftColor("#007DD0") + "ᴅᴜʀᴀᴛɪᴏɴ: 60s");
         lore.add(HexColorTranslator.translateToMinecraftColor("#007DD0") + "ᴄᴏᴏʟᴅᴏᴡɴ: 120s");
         meta.setLore(lore);
         meta.setCustomModelData(6);
         meta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ADDITIONAL_TOOLTIP});
         gem.setItemMeta(meta);
      }

      return gem;
   }

   public static ItemStack createINVIS() {
      ItemStack gem = new ItemStack(Material.POTION);
      PotionMeta meta = (PotionMeta)gem.getItemMeta();
      if (meta != null) {
         meta.setDisplayName(INVIS);
         meta.setColor(Color.fromRGB(126, 0, 252));
         List<String> lore = new ArrayList<>();
         String var10001 = INVIS_COLOR;
         lore.add(var10001 + ChatColor.BOLD + "ᴘᴀꜱꜱɪᴠᴇ ᴇꜰꜰᴇᴄᴛꜱ:");
         lore.add(INVIS_COLOR2 + "\ud83d\udc41 §x§6§D§6§D§6§DInvis");
         lore.add(INVIS_COLOR2 + "\ud83d\udc41 §x§6§D§6§D§6§DHide enemies health every 10 attacks");
         lore.add(INVIS_COLOR2 + "\ud83d\udc41 §x§6§D§6§D§6§DBlinds enemies every 10 attacks");
         lore.add(INVIS_COLOR2 + "\ud83d\udc41 §x§6§D§6§D§6§DFully invisible to mobs");
         lore.add(INVIS_COLOR2 + "\ud83d\udc41 §x§6§D§6§D§6§DBow shots give blindness");
         lore.add(ChatColor.DARK_PURPLE.toString());
         var10001 = INVIS_COLOR;
         lore.add(var10001 + ChatColor.BOLD + "ꜱᴘᴀʀᴋ ᴇꜰꜰᴇᴄᴛꜱ:");
         lore.add(INVIS_COLOR2 + "\ud83d\udc41 §x§6§D§6§D§6§DSummons a smoke bomb");
         lore.add(INVIS_COLOR2 + "\ud83d\udc41 §x§6§D§6§D§6§DArmor Invis to user");
         lore.add(INVIS_COLOR2 + "\ud83d\udc41 §x§6§D§6§D§6§DBlinds and hides health of all enemies within the smoke screen");
         lore.add(ChatColor.GRAY.toString());
         lore.add(HexColorTranslator.translateToMinecraftColor("#007DD0") + "ᴅᴜʀᴀᴛɪᴏɴ: 10s");
         lore.add(HexColorTranslator.translateToMinecraftColor("#007DD0") + "ᴄᴏᴏʟᴅᴏᴡɴ: 90s");
         meta.setLore(lore);
         meta.setCustomModelData(7);
         meta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ADDITIONAL_TOOLTIP});
         gem.setItemMeta(meta);
      }

      return gem;
   }

   public static ItemStack createOCEAN() {
      ItemStack gem = new ItemStack(Material.POTION);
      PotionMeta meta = (PotionMeta)gem.getItemMeta();
      if (meta != null) {
         meta.setDisplayName(OCEAN);
         meta.setColor(Color.fromRGB(0, 90, 252));
         List<String> lore = new ArrayList<>();
         String var10001 = OCEAN_COLOR;
         lore.add(var10001 + ChatColor.BOLD + "ᴘᴀꜱꜱɪᴠᴇ ᴇꜰꜰᴇᴄᴛꜱ:");
         lore.add(OCEAN_COLOR2 + "\ud83c\udf0a §x§6§D§6§D§6§DSwim faster");
         lore.add(OCEAN_COLOR2 + "\ud83c\udf0a §x§6§D§6§D§6§DBreathe underwater");
         lore.add(OCEAN_COLOR2 + "\ud83c\udf0a §x§6§D§6§D§6§DMake everyone around you start drowning");
         lore.add(OCEAN_COLOR2 + "\ud83c\udf0a §x§6§D§6§D§6§DTridents pull players");
         lore.add(ChatColor.DARK_PURPLE.toString());
         var10001 = OCEAN_COLOR;
         lore.add(var10001 + ChatColor.BOLD + "ꜱᴘᴀʀᴋ ᴇꜰꜰᴇᴄᴛꜱ:");
         lore.add(OCEAN_COLOR2 + "\ud83c\udf0a §x§6§D§6§D§6§DSummons a Whirlhole");
         lore.add(OCEAN_COLOR2 + "\ud83c\udf0a §x§6§D§6§D§6§DPulls in all players and entities in 15 blocks");
         lore.add(ChatColor.GRAY.toString());
         lore.add(HexColorTranslator.translateToMinecraftColor("#007DD0") + "ᴅᴜʀᴀᴛɪᴏɴ: 5s");
         lore.add(HexColorTranslator.translateToMinecraftColor("#007DD0") + "ᴄᴏᴏʟᴅᴏᴡɴ: 90s");
         meta.setLore(lore);
         meta.setCustomModelData(8);
         meta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ADDITIONAL_TOOLTIP});
         gem.setItemMeta(meta);
      }

      return gem;
   }

   public static ItemStack createREGEN() {
      ItemStack gem = new ItemStack(Material.POTION);
      PotionMeta meta = (PotionMeta)gem.getItemMeta();
      if (meta != null) {
         meta.setDisplayName(REGEN);
         meta.setColor(Color.fromRGB(252, 0, 221));
         List<String> lore = new ArrayList<>();
         String var10001 = REGEN_COLOR;
         lore.add(var10001 + ChatColor.BOLD + "ᴘᴀꜱꜱɪᴠᴇ ᴇꜰꜰᴇᴄᴛꜱ:");
         lore.add(REGEN_COLOR2 + "+ §x§6§D§6§D§6§DEvery hit grants Regeneration 2 for 2 second");
         lore.add(REGEN_COLOR2 + "+ §x§6§D§6§D§6§DHealing gives 3 extra saturation bars");
         lore.add(ChatColor.LIGHT_PURPLE.toString());
         var10001 = REGEN_COLOR;
         lore.add(var10001 + ChatColor.BOLD + "ꜱᴘᴀʀᴋ ᴇꜰꜰᴇᴄᴛ:");
         lore.add(REGEN_COLOR2 + "+ §x§6§D§6§D§6§DDealing damage heals you");
         lore.add(ChatColor.LIGHT_PURPLE.toString());
         lore.add(HexColorTranslator.translateToMinecraftColor("#007DD0") + "ᴅᴜʀᴀᴛɪᴏɴ: 30s");
         lore.add(HexColorTranslator.translateToMinecraftColor("#007DD0") + "ᴄᴏᴏʟᴅᴏᴡɴ: 60s");
         meta.setLore(lore);
         meta.setCustomModelData(9);
         meta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ADDITIONAL_TOOLTIP});
         gem.setItemMeta(meta);
      }

      return gem;
   }

   public static ItemStack createSPEED() {
      ItemStack gem = new ItemStack(Material.POTION);
      PotionMeta meta = (PotionMeta)gem.getItemMeta();
      if (meta != null) {
         meta.setDisplayName(SPEED);
         meta.setColor(Color.fromRGB(209, 164, 75));
         List<String> lore = new ArrayList<>();
         String var10001 = SPEED_COLOR;
         lore.add(var10001 + ChatColor.BOLD + "ᴘᴀꜱꜱɪᴠᴇ ᴇꜰꜰᴇᴄᴛꜱ:");
         lore.add(SPEED_COLOR2 + "⏩ §x§6§D§6§D§6§DSpeed 1");
         lore.add(SPEED_COLOR2 + "⏩ §x§6§D§6§D§6§DIncrease speed level by 1 after each hit");
         lore.add(SPEED_COLOR2 + "⏩ §x§6§D§6§D§6§DSpeed resets after 1 second of no activity");
         lore.add(SPEED_COLOR2 + "⏩ §x§6§D§6§D§6§DRanged weapons charge 1.5x faster");
         lore.add(SPEED_COLOR2 + "⏩ §x§6§D§6§D§6§DEnemy invincibility frames are halved");
         lore.add(ChatColor.GRAY.toString());
         var10001 = SPEED_COLOR;
         lore.add(var10001 + ChatColor.BOLD + "ꜱᴘᴀʀᴋ ᴇꜰꜰᴇᴄᴛꜱ:");
         lore.add(SPEED_COLOR2 + "⏩ §x§6§D§6§D§6§DSpeed Dash");
         lore.add(ChatColor.DARK_GRAY.toString());
         lore.add(HexColorTranslator.translateToMinecraftColor("#007DD0") + "ᴅᴜʀᴀᴛɪᴏɴ: 1s");
         lore.add(HexColorTranslator.translateToMinecraftColor("#007DD0") + "ᴄᴏᴏʟᴅᴏᴡɴ: 20s");
         meta.setLore(lore);
         meta.setCustomModelData(10);
         meta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ADDITIONAL_TOOLTIP});
         gem.setItemMeta(meta);
      }

      return gem;
   }

   public static ItemStack createST() {
      ItemStack gem = new ItemStack(Material.POTION);
      PotionMeta meta = (PotionMeta)gem.getItemMeta();
      if (meta != null) {
         meta.setDisplayName(ST);
         meta.setColor(Color.fromRGB(158, 32, 32));
         List<String> lore = new ArrayList<>();
         String var10001 = STRENGTH_COLOR;
         lore.add(var10001 + ChatColor.BOLD + "ᴘᴀꜱꜱɪᴠᴇ ᴇꜰꜰᴇᴄᴛꜱ:");
         lore.add(STRENGTH_COLOR2 + "\ud83d\udee1 §x§6§D§6§D§6§DDouble Damage to all mobs");
         lore.add(STRENGTH_COLOR2 + "\ud83d\udee1 §x§6§D§6§D§6§DDisable shields for 10 seconds");
         lore.add(STRENGTH_COLOR2 + "\ud83d\udee1 §x§6§D§6§D§6§DRanged weapons pierce shields");
         lore.add(STRENGTH_COLOR2 + "\ud83d\udee1 §x§6§D§6§D§6§D+1 Damage when under 6 Hearts");
         lore.add(STRENGTH_COLOR2 + "\ud83d\udee1 §x§6§D§6§D§6§D+2 Damage when under 4 Hearts");
         lore.add(STRENGTH_COLOR2 + "\ud83d\udee1 §x§6§D§6§D§6§D+3 Damage when under 2 Hearts");
         lore.add(ChatColor.GRAY.toString());
         var10001 = STRENGTH_COLOR;
         lore.add(var10001 + ChatColor.BOLD + "ꜱᴘᴀʀᴋ ᴇꜰꜰᴇᴄᴛꜱ:");
         lore.add(STRENGTH_COLOR2 + "\ud83d\udee1 §x§6§D§6§D§6§DActivate auto-crit for 15 seconds");
         lore.add(ChatColor.DARK_GRAY.toString());
         lore.add(HexColorTranslator.translateToMinecraftColor("#007DD0") + "ᴅᴜʀᴀᴛɪᴏɴ: 15s");
         lore.add(HexColorTranslator.translateToMinecraftColor("#007DD0") + "ᴄᴏᴏʟᴅᴏᴡɴ: 60s");
         meta.setLore(lore);
         meta.setCustomModelData(11);
         meta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ADDITIONAL_TOOLTIP});
         gem.setItemMeta(meta);
      }

      return gem;
   }

   public static ItemStack createAPOPHIS() {
      ItemStack gem = new ItemStack(Material.POTION);
      PotionMeta meta = (PotionMeta)gem.getItemMeta();
      if (meta != null) {
         meta.setDisplayName(APOPHIS);
         meta.setColor(Color.fromRGB(48, 17, 69));
         List<String> lore = new ArrayList<>();
         String var10001 = APOPHIS_COLOR;
         lore.add(var10001 + ChatColor.BOLD + "ᴘᴀꜱꜱɪᴠᴇ ᴇꜰꜰᴇᴄᴛꜱ:");
         lore.add(APOPHIS_COLOR + "❤ §x§3§0§1§1§4§5+5 Hearts");
         lore.add(APOPHIS_COLOR + "\ud83d\udee1 §x§3§0§1§1§4§5Double damage to mobs & disable shields (10s)");
         lore.add(APOPHIS_COLOR + "\ud83c\udf0a §x§3§0§1§1§4§5Swim faster & breathe underwater");
         lore.add(APOPHIS_COLOR + "\ud83d\udd25 §x§3§0§1§1§4§5Fire Resistance, swim/fly in lava");
         lore.add(APOPHIS_COLOR + "\ud83d\udcb2 §x§3§0§1§1§4§550% chance consumables aren't consumed");
         lore.add(ChatColor.GRAY.toString());
         var10001 = APOPHIS_COLOR;
         lore.add(var10001 + ChatColor.BOLD + "ꜱᴘᴀʀᴋ ᴇꜰꜰᴇᴄᴛꜱ:");
         lore.add(APOPHIS_COLOR + "◆ Combines Heart + Strength + Emerald sparks");
         lore.add(APOPHIS_COLOR + "◆ Inflicts Blindness & Wither to enemies in 5 blocks for 20s");
         lore.add(APOPHIS_COLOR + "◆ Your hits apply Wither I for 15s");
         lore.add(ChatColor.GRAY.toString());
         lore.add(HexColorTranslator.translateToMinecraftColor("#007DD0") + "ᴅᴜʀᴀᴛɪᴏɴ: 20s");
         lore.add(HexColorTranslator.translateToMinecraftColor("#007DD0") + "ᴄᴏᴏʟᴅᴏᴡɴ: 90s");
         meta.setLore(lore);
         meta.setCustomModelData(12);
         meta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ADDITIONAL_TOOLTIP});
         tagEffect(meta, "apophis", "normal");
         gem.setItemMeta(meta);
      }

      return gem;
   }

   public static ItemStack createDRAGON() {
      ItemStack gem = new ItemStack(Material.POTION);
      PotionMeta meta = (PotionMeta)gem.getItemMeta();
      if (meta != null) {
         meta.setDisplayName(DRAGON);
         meta.setColor(Color.fromRGB(84, 19, 81));
         List<String> lore = new ArrayList<>();
         String var10001 = DRAGON_COLOR;
         lore.add(var10001 + ChatColor.BOLD + "ᴘᴀꜱꜱɪᴠᴇ ᴇꜰꜰᴇᴄᴛꜱ:");
         lore.add(DRAGON_COLOR2 + "⭐ §x§6§D§6§D§6§DAll nearby untrusted players have glowing");
         lore.add(DRAGON_COLOR2 + "⭐ §x§6§D§6§D§6§DUse dragon's breath to shoot powerful fireballs that curse players");
         lore.add(DRAGON_COLOR2 + "⭐ §x§6§D§6§D§6§DCurse untrusted players on hit which shares damage with all cursed players");
         lore.add(ChatColor.GRAY.toString());
         var10001 = DRAGON_COLOR;
         lore.add(var10001 + ChatColor.BOLD + "ꜱᴘᴀʀᴋ ᴇꜰꜰᴇᴄᴛꜱ:");
         lore.add(DRAGON_COLOR2 + "⭐ §x§6§D§6§D§6§DTeleport to the cursor position within a 15 block radius");
         lore.add(DRAGON_COLOR2 + "⭐ §x§6§D§6§D§6§DInstantly kills any mob and curses players");
         lore.add(DRAGON_COLOR2 + "⭐ §x§6§D§6§D§6§DCursing a player resets the spark cooldown");
         lore.add(ChatColor.GRAY.toString());
         lore.add(HexColorTranslator.translateToMinecraftColor("#007DD0") + "ᴅᴜʀᴀᴛɪᴏɴ: 1s");
         lore.add(HexColorTranslator.translateToMinecraftColor("#007DD0") + "ᴄᴏᴏʟᴅᴏᴡɴ: 10s");
         meta.setLore(lore);
         meta.setEnchantmentGlintOverride(true);
         meta.setCustomModelData(1000);
         meta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ADDITIONAL_TOOLTIP});
         gem.setItemMeta(meta);
      }

      return gem;
   }

   public static boolean ISST(ItemStack item) {
      return isPotionWithName(item, ST);
   }

   public static boolean ISHEART(ItemStack item) {
      return isPotionWithName(item, HEART);
   }

   public static boolean ISREGEN(ItemStack item) {
      return isPotionWithName(item, REGEN);
   }

   public static boolean ISINVIS(ItemStack item) {
      return isPotionWithName(item, INVIS);
   }

   public static boolean ISEME(ItemStack item) {
      return isPotionWithName(item, EME);
   }

   public static boolean ISSPEED(ItemStack item) {
      return isPotionWithName(item, SPEED);
   }

   public static boolean ISHASTE(ItemStack item) {
      return isPotionWithName(item, HASTE);
   }

   public static boolean ISFEATHER(ItemStack item) {
      return isPotionWithName(item, FEATHER);
   }

   public static boolean ISOCEAN(ItemStack item) {
      return isPotionWithName(item, OCEAN);
   }

   public static boolean ISFROST(ItemStack item) {
      return isPotionWithName(item, FROST);
   }

   public static boolean ISFIRE(ItemStack item) {
      return isPotionWithName(item, FIRE);
   }

   public static boolean ISTHUNDER(ItemStack item) {
      return isPotionWithName(item, THUNDER);
   }

   public static boolean ISDRAGON(ItemStack item) {
      return isPotionWithName(item, DRAGON);
   }

   public static boolean ISAPOPHIS(ItemStack item) {
      return isPotionWithName(item, APOPHIS);
   }

   private static boolean isPotionWithName(ItemStack item, String expectedName) {
      if (item != null && item.getType() == Material.POTION && item.hasItemMeta()) {
         ItemMeta meta = item.getItemMeta();
         if (meta != null && meta.hasDisplayName()) {
            String strippedName = ChatColor.stripColor(meta.getDisplayName());
            return strippedName.equals(ChatColor.stripColor(expectedName));
         }
      }

      return false;
   }

   private static void tagEffect(ItemMeta meta, String effectId, String tier) {
      if (PLUGIN != null) {
         PersistentDataContainer pdc = meta.getPersistentDataContainer();
         pdc.set(new NamespacedKey(PLUGIN, "infuse_effect"), PersistentDataType.STRING, effectId);
         pdc.set(new NamespacedKey(PLUGIN, "infuse_tier"), PersistentDataType.STRING, tier);
         pdc.set(new NamespacedKey(PLUGIN, "uuidpad"), PersistentDataType.STRING, UUID.randomUUID().toString());
      }
   }
}
