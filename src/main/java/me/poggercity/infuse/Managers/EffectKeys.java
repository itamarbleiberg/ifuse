package me.poggercity.infuse.Managers;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.ChatColor;

/** Names, colors and icons of the effects, by their config key ("fire", "gamble", ...). */
public final class EffectKeys {
   /** Primary effects (general.only_one_primary_effect, random primary effect on first join). */
   public static final List<String> PRIMARY = List.of("invis", "haste", "regen", "apophis", "ender", "feather", "thunder", "strength", "frost", "heart", "gamble");
   /** Secondary effects. */
   public static final List<String> SECONDARY = List.of("ocean", "emerald", "fire", "speed");
   private static final Map<String, String> HEX = Map.ofEntries(
      Map.entry("emerald", "#0DFC00"),
      Map.entry("feather", "#BEA3CA"),
      Map.entry("fire", "#FC7803"),
      Map.entry("frost", "#7AE3FC"),
      Map.entry("haste", "#B96C00"),
      Map.entry("heart", "#FC0046"),
      Map.entry("invis", "#7E00FC"),
      Map.entry("ocean", "#005AFC"),
      Map.entry("regen", "#FC00DD"),
      Map.entry("speed", "#D1A44B"),
      Map.entry("strength", "#9E2020"),
      Map.entry("thunder", "#FCED00"),
      Map.entry("ender", "#541351"),
      Map.entry("apophis", "#301145"),
      Map.entry("gamble", "#1EE8B6")
   );
   private static final Map<String, String> ICON = Map.ofEntries(
      Map.entry("emerald", "\ud83d\udcb2"),
      Map.entry("feather", "☁"),
      Map.entry("fire", "\ud83d\udd25"),
      Map.entry("frost", "❄"),
      Map.entry("haste", "⛏"),
      Map.entry("heart", "❤"),
      Map.entry("invis", "\ud83d\udc41"),
      Map.entry("ocean", "\ud83c\udf0a"),
      Map.entry("regen", "✚"),
      Map.entry("speed", "⏩"),
      Map.entry("strength", "\ud83d\udee1"),
      Map.entry("thunder", "⚡"),
      Map.entry("ender", "⭐"),
      Map.entry("apophis", "◆"),
      Map.entry("gamble", "\ud83c\udfb2")
   );

   private EffectKeys() {
   }

   /** "Augmented Fire Effect", "§6Fire Effect" or "fire" become "fire". Null if it isn't an effect. */
   public static String keyOf(String name) {
      if (name == null) {
         return null;
      } else {
         String s = ChatColor.stripColor(name).trim().toLowerCase(Locale.ROOT);
         if (s.startsWith("augmented ")) {
            s = s.substring("augmented ".length());
         }

         if (s.endsWith(" effect")) {
            s = s.substring(0, s.length() - " effect".length());
         }

         s = s.trim();
         return Arrays.asList(Infuse.EFFECT_KEYS).contains(s) ? s : null;
      }
   }

   /** "fire" becomes "Fire". */
   public static String title(String key) {
      return key == null || key.isEmpty() ? "" : Character.toUpperCase(key.charAt(0)) + key.substring(1);
   }

   public static String hex(String key) {
      return HEX.getOrDefault(key, "#FFFFFF");
   }

   public static String icon(String key) {
      return ICON.getOrDefault(key, "?");
   }

   public static boolean isPrimary(String key) {
      return PRIMARY.contains(key);
   }

   public static boolean isSecondary(String key) {
      return SECONDARY.contains(key);
   }
}
