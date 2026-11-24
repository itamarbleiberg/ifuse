package me.poggercity.infuse.Managers;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Typed access to the per-effect settings in config.yml ("effect_settings.&lt;effect&gt;").
 * Values are read on every call, so changes from /infuseconfig or a config reload apply immediately.
 */
public final class Settings {
   public static final String ROOT = "effect_settings";

   private Settings() {
   }

   private static FileConfiguration cfg() {
      return Infuse.getInstance().getConfig();
   }

   public static String passivePath(String effect, String passive) {
      return ROOT + "." + effect + ".passives." + passive;
   }

   /** Whether a passive is turned on. Missing keys count as on. */
   public static boolean passive(String effect, String passive) {
      return cfg().getBoolean(passivePath(effect, passive) + ".enabled", true);
   }

   public static double value(String effect, String passive, String key, double def) {
      return cfg().getDouble(passivePath(effect, passive) + "." + key, def);
   }

   public static int intValue(String effect, String passive, String key, int def) {
      return (int)Math.round(value(effect, passive, key, def));
   }

   /** A percentage setting (0-100) converted to a 0-1 chance. */
   public static double chance(String effect, String passive, String key, double defPercent) {
      return Math.max(0.0, Math.min(100.0, value(effect, passive, key, defPercent))) / 100.0;
   }

   // ----- particles -----

   public static boolean auraEnabled(String effect) {
      return cfg().getBoolean(ROOT + "." + effect + ".particles.aura_enabled", true);
   }

   public static int auraAmount(String effect) {
      return Math.max(0, cfg().getInt(ROOT + "." + effect + ".particles.aura_amount", 2));
   }

   /** Scales a spark/ability particle count by the effect's ability_percent setting. */
   public static int particles(String effect, int base) {
      double percent = Math.max(0.0, cfg().getDouble(ROOT + "." + effect + ".particles.ability_percent", 100.0));
      if (base <= 0 || percent <= 0.0) {
         return 0;
      } else {
         return Math.max(1, (int)Math.round(base * percent / 100.0));
      }
   }

   // ----- rituals -----

   public static Set<Environment> ritualDimensions(String effect) {
      List<String> names = cfg().getStringList(ROOT + "." + effect + ".ritual.dimensions");
      Set<Environment> result = EnumSet.noneOf(Environment.class);

      for (String name : names) {
         Environment env = dimensionFromName(name);
         if (env != null) {
            result.add(env);
         }
      }

      return result;
   }

   public static boolean ritualAllowedIn(String effect, World world) {
      return ritualDimensions(effect).contains(world.getEnvironment());
   }

   /** Ritual length in seconds. 0 means the effect is handed out instantly (only used for normal effects). */
   public static int ritualSeconds(String effect, boolean augmented) {
      String key = augmented ? "augmented_seconds" : "normal_seconds";
      return Math.max(0, cfg().getInt(ROOT + "." + effect + ".ritual." + key, augmented ? 600 : 0));
   }

   public static Environment dimensionFromName(String name) {
      if (name == null) {
         return null;
      } else {
         switch (name.trim().toLowerCase(Locale.ROOT)) {
            case "overworld":
            case "normal":
            case "world":
               return Environment.NORMAL;
            case "nether":
            case "the_nether":
               return Environment.NETHER;
            case "end":
            case "the_end":
               return Environment.THE_END;
            default:
               return null;
         }
      }
   }

   public static String dimensionName(Environment env) {
      switch (env) {
         case NORMAL:
            return "overworld";
         case NETHER:
            return "nether";
         case THE_END:
            return "end";
         default:
            return env.name().toLowerCase(Locale.ROOT);
      }
   }

   public static String describeDimensions(Set<Environment> dims) {
      List<String> names = new ArrayList<>();

      for (Environment env : dims) {
         switch (env) {
            case NORMAL:
               names.add("Overworld");
               break;
            case NETHER:
               names.add("Nether");
               break;
            case THE_END:
               names.add("End");
               break;
            default:
               names.add(env.name());
         }
      }

      return names.isEmpty() ? "nowhere" : String.join(", ", names);
   }
}
