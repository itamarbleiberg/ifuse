package me.poggercity.infuse.Infuses;

public class HexColorTranslator {
   public static String translateToMinecraftColor(String hexCode) {
      if (hexCode.startsWith("#") && hexCode.length() == 7) {
         StringBuilder minecraftColor = new StringBuilder("§x");

         for (char c : hexCode.substring(1).toCharArray()) {
            minecraftColor.append("§").append(c);
         }

         return minecraftColor.toString();
      } else {
         return "Invalid hex code! Use format: #RRGGBB";
      }
   }

   public static void main(String[] args) {
      String hexCode = "#FF5733";
      String minecraftFormattedColor = translateToMinecraftColor(hexCode);
      System.out.println("Minecraft Chat Color: " + minecraftFormattedColor);
   }
}
