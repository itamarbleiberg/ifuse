package me.poggercity.infuse.Managers;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Typed access to config.yml. Every effect has one section, "effects.&lt;effect&gt;", with craft / spark / particles /
 * passives in it. Values are read on every call, so changes from /infuseconfig or a reload apply immediately.
 */
public final class Settings {
   public static final String ROOT = "effects";

   private Settings() {
   }

   private static FileConfiguration cfg() {
      return Infuse.getInstance().getConfig();
   }

   // Reads a value from config.yml, then from the jar's config.yml, and only then uses the given default.
   private static double num(String path, double def) {
      Object v = cfg().get(path);
      return v instanceof Number n ? n.doubleValue() : def;
   }

   private static boolean bool(String path, boolean def) {
      Object v = cfg().get(path);
      return v instanceof Boolean b ? b : def;
   }

   /** A true/false setting at any path. */
   public static boolean flag(String path, boolean def) {
      return bool(path, def);
   }

   /** Whether players can start with this effect (effect selector, random effect on first join). */
   public static boolean startingAllowed(String effect) {
      return bool("starting_effects." + effect, true);
   }

   public static String effectPath(String effect) {
      return ROOT + "." + effect;
   }

   // ----- general -----

   public static List<String> disabledWorlds() {
      return cfg().getStringList("general.disabled_worlds");
   }

   public static boolean worldDisabled(World world) {
      for (String name : disabledWorlds()) {
         if (name.equalsIgnoreCase(world.getName())) {
            return true;
         }
      }

      return false;
   }

   // ----- spark -----

   public static double spark(String effect, String key, double def) {
      return num(effectPath(effect) + ".spark." + key, def);
   }

   public static int sparkInt(String effect, String key, int def) {
      return (int)num(effectPath(effect) + ".spark." + key, def);
   }

   // ----- passives -----

   public static String passivePath(String effect, String passive) {
      return effectPath(effect) + ".passives." + passive;
   }

   /** Whether a passive is turned on. Missing keys count as on. */
   public static boolean passive(String effect, String passive) {
      return bool(passivePath(effect, passive) + ".enabled", true);
   }

   public static double value(String effect, String passive, String key, double def) {
      return num(passivePath(effect, passive) + "." + key, def);
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
      return bool(effectPath(effect) + ".particles.aura", true);
   }

   public static int auraAmount(String effect) {
      return Math.max(0, (int)num(effectPath(effect) + ".particles.aura_amount", 2));
   }

   public static double abilityParticlePercent(String effect) {
      return Math.max(0.0, num(effectPath(effect) + ".particles.spark_percent", 100.0));
   }

   /** Scales a spark/ability particle count by the effect's spark_percent setting. */
   public static int particles(String effect, int base) {
      double percent = abilityParticlePercent(effect);
      if (base <= 0 || percent <= 0.0) {
         return 0;
      } else {
         return Math.max(1, (int)Math.round(base * percent / 100.0));
      }
   }

   /**
    * For shapes drawn from many single particles (rings, spirals): the count to spawn at one point. At 100% this is 1,
    * at 50% it is 0 or 1 at random (half the points), at 200% it is 2.
    */
   public static int particlesAt(String effect) {
      double factor = abilityParticlePercent(effect) / 100.0;
      int whole = (int)factor;
      return whole + (Math.random() < factor - whole ? 1 : 0);
   }

   // ----- crafting and rituals -----

   public static String craftPath(String effect) {
      return effectPath(effect) + ".craft";
   }

   public static String recipePath(String effect) {
      return craftPath(effect) + ".recipe";
   }

   /** Normal crafts allowed after the augmented ones (-1 = unlimited). */
   public static int normalCrafts(String effect) {
      return (int)num(craftPath(effect) + ".normal_crafts", 3);
   }

   /** Augmented crafts allowed (-1 = unlimited). */
   public static int augmentedCrafts(String effect) {
      return (int)num(craftPath(effect) + ".augmented_crafts", 1);
   }

   public static Set<Environment> ritualDimensions(String effect) {
      List<String> names = cfg().getStringList(craftPath(effect) + ".dimensions");
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
      String key = augmented ? "augmented_ritual_seconds" : "normal_ritual_seconds";
      return Math.max(0, (int)num(craftPath(effect) + "." + key, augmented ? 600 : 0));
   }

   /** Blocks from 0, 0 within which effects can be crafted (0 or less = anywhere). */
   public static int maxCraftDistance() {
      return (int)num("rituals.max_distance_from_0_0", 0);
   }

   /** Whether effects can be crafted at this location (horizontal distance from 0, 0 in its own world). */
   public static boolean withinCraftDistance(Location loc) {
      int max = maxCraftDistance();
      if (max <= 0) {
         return true;
      } else {
         double x = loc.getX();
         double z = loc.getZ();
         return x * x + z * z <= (double)max * max;
      }
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
