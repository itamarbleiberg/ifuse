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
         meta.setLore(EffectLore.lore("emerald", false));
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
         meta.setLore(EffectLore.lore("feather", false));
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
         meta.setLore(EffectLore.lore("fire", false));
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
         meta.setLore(EffectLore.lore("frost", false));
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
         meta.setLore(EffectLore.lore("thunder", false));
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
         meta.setLore(EffectLore.lore("haste", false));
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
         meta.setLore(EffectLore.lore("heart", false));
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
         meta.setLore(EffectLore.lore("invis", false));
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
         meta.setLore(EffectLore.lore("ocean", false));
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
         meta.setLore(EffectLore.lore("regen", false));
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
         meta.setLore(EffectLore.lore("speed", false));
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
         meta.setLore(EffectLore.lore("strength", false));
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
         meta.setLore(EffectLore.lore("apophis", false));
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
         meta.setLore(EffectLore.lore("ender", false));
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
