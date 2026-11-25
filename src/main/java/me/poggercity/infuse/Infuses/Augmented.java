package me.poggercity.infuse.Infuses;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;

public class Augmented {
   private static final String EME = HexColorTranslator.translateToMinecraftColor("#0DFC00") + "Augmented Emerald Effect";
   private static final String FEATHER = HexColorTranslator.translateToMinecraftColor("#BEA3CA") + "Augmented Feather Effect";
   private static final String FIRE = HexColorTranslator.translateToMinecraftColor("#FC7803") + "Augmented Fire Effect";
   private static final String FROST = HexColorTranslator.translateToMinecraftColor("#7AE3FC") + "Augmented Frost Effect";
   private static final String HASTE = HexColorTranslator.translateToMinecraftColor("#B96C00") + "Augmented Haste Effect";
   private static final String HEART = HexColorTranslator.translateToMinecraftColor("#FC0046") + "Augmented Heart Effect";
   private static final String INVIS = HexColorTranslator.translateToMinecraftColor("#7E00FC") + "Augmented Invis Effect";
   private static final String OCEAN = HexColorTranslator.translateToMinecraftColor("#005AFC") + "Augmented Ocean Effect";
   private static final String REGEN = HexColorTranslator.translateToMinecraftColor("#FC00DD") + "Augmented Regen Effect";
   private static final String SPEED = HexColorTranslator.translateToMinecraftColor("#d1a44b") + "Augmented Speed Effect";
   private static final String ST = HexColorTranslator.translateToMinecraftColor("#9E2020") + "Augmented Strength Effect";
   private static final String THUNDER = HexColorTranslator.translateToMinecraftColor("#FCED00") + "Augmented Thunder Effect";
   private static final String APOPHIS = HexColorTranslator.translateToMinecraftColor("#301145") + "Augmented Apophis Effect";
   private static final String CURSE = HexColorTranslator.translateToMinecraftColor("#1EE8B6") + WinnersCurse.AUGMENTED_NAME;
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

   private static String divider() {
      return ChatColor.GRAY + " ";
   }

   public static ItemStack createEME() {
      ItemStack gem = new ItemStack(Material.POTION);
      PotionMeta meta = (PotionMeta)gem.getItemMeta();
      if (meta != null) {
         meta.setDisplayName(EME);
         meta.setColor(Color.fromRGB(0, 255, 0));
         meta.setLore(EffectLore.lore("emerald", true));
         meta.setEnchantmentGlintOverride(true);
         meta.setCustomModelData(999);
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
         meta.setLore(EffectLore.lore("feather", true));
         meta.setEnchantmentGlintOverride(true);
         meta.setCustomModelData(999);
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
         meta.setLore(EffectLore.lore("fire", true));
         meta.setEnchantmentGlintOverride(true);
         meta.setCustomModelData(999);
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
         meta.setLore(EffectLore.lore("frost", true));
         meta.setEnchantmentGlintOverride(true);
         meta.setCustomModelData(999);
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
         meta.setLore(EffectLore.lore("haste", true));
         meta.setEnchantmentGlintOverride(true);
         meta.setCustomModelData(999);
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
         meta.setLore(EffectLore.lore("heart", true));
         meta.setEnchantmentGlintOverride(true);
         meta.setCustomModelData(999);
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
         meta.setLore(EffectLore.lore("invis", true));
         meta.setEnchantmentGlintOverride(true);
         meta.setCustomModelData(999);
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
         meta.setLore(EffectLore.lore("ocean", true));
         meta.setEnchantmentGlintOverride(true);
         meta.setCustomModelData(999);
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
         meta.setLore(EffectLore.lore("regen", true));
         meta.setEnchantmentGlintOverride(true);
         meta.setCustomModelData(999);
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
         meta.setLore(EffectLore.lore("strength", true));
         meta.setEnchantmentGlintOverride(true);
         meta.setCustomModelData(999);
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
         meta.setLore(EffectLore.lore("thunder", true));
         meta.setEnchantmentGlintOverride(true);
         meta.setCustomModelData(999);
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
         meta.setLore(EffectLore.lore("speed", true));
         meta.setEnchantmentGlintOverride(true);
         meta.setCustomModelData(999);
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
         meta.setLore(EffectLore.lore("apophis", true));
         meta.setEnchantmentGlintOverride(true);
         meta.setCustomModelData(999);
         meta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ADDITIONAL_TOOLTIP});
         gem.setItemMeta(meta);
      }

      return gem;
   }

   public static ItemStack createCURSE() {
      ItemStack gem = new ItemStack(Material.POTION);
      PotionMeta meta = (PotionMeta)gem.getItemMeta();
      if (meta != null) {
         meta.setDisplayName(CURSE);
         meta.setColor(Color.fromRGB(30, 232, 182));
         meta.setLore(EffectLore.lore(WinnersCurse.KEY, true));
         meta.setEnchantmentGlintOverride(true);
         meta.setCustomModelData(999);
         meta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ADDITIONAL_TOOLTIP});
         gem.setItemMeta(meta);
      }

      return gem;
   }

   public static boolean ISAPOPHIS(ItemStack item) {
      return isPotionWithName(item, APOPHIS);
   }

   /** Also an "Augmented Gamble Effect" made with Infuse 2.7 (the old name of Winner's Curse). */
   public static boolean ISCURSE(ItemStack item) {
      return isPotionWithName(item, CURSE) || isPotionWithName(item, WinnersCurse.LEGACY_AUGMENTED_NAME);
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

   private static String formatSeconds(int total) {
      int m = total / 60;
      int s = total % 60;
      return (m > 0 ? m + "m " : "") + s + "s";
   }
}
