package me.poggercity.infuse.Managers;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Converts config.yml from Infuse 2.4 and 2.5 to the current layout (config_version 3), where everything about an
 * effect is in one section. The old file is kept as a backup. The new file is the commented template from the jar
 * with the server's values filled in, so the comments explaining every setting are there after the update too.
 */
public final class ConfigMigration {
   public static final int VERSION = 3;
   public static final String[] EFFECTS = Infuse.EFFECT_KEYS;
   private static final String[] RADIUS_EFFECTS = new String[]{"fire", "invis", "ocean", "thunder", "feather", "frost", "apophis"};
   // Keys that never did anything (not read by any code); dropped without a warning. Invis never had aura particles
   // (they would give invisible players away), so those two settings were never used either.
   private static final Set<String> DEAD_KEYS = Set.of(
      "damage.thunder",
      "damage.apophis_wither_duration",
      "damage.apophis_area_duration",
      "effect_settings.invis.particles.aura_enabled",
      "effect_settings.invis.particles.aura_amount"
   );
   /** True while config.yml can't be read (a YAML error). Nothing is converted or saved over it then. */
   private static boolean broken;

   private ConfigMigration() {
   }

   /** Old path -> new path, for values that moved one to one. */
   private static Map<String, String> simpleMoves() {
      Map<String, String> m = new LinkedHashMap<>();
      m.put("configs.disabled_worlds", "general.disabled_worlds");
      m.put("configs.apophis_enabled", "general.apophis_enabled");
      m.put("configs.drain_cooldown", "general.swap_cooldown");
      m.put("configs.cant_unequip_while_on_cooldown", "general.no_swap_while_spark_on_cooldown");
      m.put("configs.only_allow_one_primary", "general.only_one_primary_effect");
      m.put("configs.drop_effects_on_death", "general.drop_effects_on_death");
      m.put("configs.default_offhand_for_ability", "general.sparks_on_swap_hands_key");
      m.put("configs.random_effect_on_join", "general.random_effect_on_first_join");
      m.put("configs.random_primary_effect_on_join", "general.random_primary_effect_on_first_join");
      m.put("configs.random_secondary_effect_on_join", "general.random_secondary_effect_on_first_join");
      m.put("configs.roll_enable_ender", "general.random_effect_can_be_ender");
      m.put("configs.roll_enable_apophis", "general.random_effect_can_be_apophis");
      m.put("configs.action_bar_update_time", "general.action_bar_update_ticks");
      m.put("configs.event_update_time", "general.passive_update_ticks");
      m.put("configs.brewing_beam", "rituals.beam");
      m.put("rituals.one_at_a_time", "rituals.one_at_a_time");
      m.put("rituals.brewing_stand_opens_crafting", "rituals.brewing_stand_opens_crafting");
      m.put("damage.feather_spark", "effects.feather.spark.damage");
      m.put("damage.fire_launch_strength", "effects.fire.spark.launch_strength");
      m.put("damage.speed_dash_strength", "effects.speed.spark.dash_strength");
      m.put("damage.dragon", "effects.ender.spark.damage");
      // 2.4 keys that 2.5 had already moved into its per-effect section
      m.put("damage.feather_mace", "effects.feather.passives.fall_attack.multiplier");
      m.put("damage.increasing_fire_damage_multiplier", "effects.fire.passives.fire_damage_boost.step");
      m.put("damage.max_fire_damage_multiplier", "effects.fire.passives.fire_damage_boost.max");
      m.put("damage.ocean_drown", "effects.ocean.passives.drowning_aura.damage");
      m.put("radius.ocean_drown", "effects.ocean.passives.drowning_aura.radius");
      m.put("damage.trident_pull_strength", "effects.ocean.passives.trident_pull.strength");
      m.put("damage.thunder_hearts", "effects.thunder.passives.smite_on_hit.hearts");
      m.put("radius.ender", "effects.ender.passives.glowing.radius");
      m.put("cooldowns.ender_dragon_breath", "effects.ender.passives.dragon_fireball.cooldown_seconds");
      m.put("durations.dragon_curse_duration", "effects.ender.passives.curse.seconds");
      m.put("configs.chain_lightning", "effects.thunder.passives.chain_lightning.enabled");
      m.put("configs.fire_lava_fly", "effects.fire.passives.lava_flight.enabled");
      m.put("configs.apophis_lava_fly", "effects.apophis.passives.lava_flight.enabled");

      for (String e : EFFECTS) {
         m.put("cooldowns." + e + "_spark", "effects." + e + ".spark.cooldown");
         m.put("durations." + e + "_spark", "effects." + e + ".spark.duration");
         m.put("augments." + e, "effects." + e + ".craft.augmented_crafts");
         m.put("effects." + e, "effects." + e + ".craft.normal_crafts");
         // 2.5 per-effect section
         String old = "effect_settings." + e;
         if (!e.equals("invis")) {
            m.put(old + ".particles.aura_enabled", "effects." + e + ".particles.aura");
            m.put(old + ".particles.aura_amount", "effects." + e + ".particles.aura_amount");
         }

         m.put(old + ".particles.ability_percent", "effects." + e + ".particles.spark_percent");
         m.put(old + ".ritual.dimensions", "effects." + e + ".craft.dimensions");
         m.put(old + ".ritual.normal_seconds", "effects." + e + ".craft.normal_ritual_seconds");
         m.put(old + ".ritual.augmented_seconds", "effects." + e + ".craft.augmented_ritual_seconds");
      }

      for (String e : RADIUS_EFFECTS) {
         m.put("radius." + e, "effects." + e + ".spark.radius");
      }

      return m;
   }

   /** Old 2.4 keys that turned on or off several passives at once. */
   private static Map<String, String[]> multiMoves() {
      Map<String, String[]> m = new LinkedHashMap<>();
      m.put("configs.haste_enchant", new String[]{"effects.haste.passives.tool_enchants.enabled", "effects.apophis.passives.haste.enabled"});
      m.put(
         "configs.emerald_enchant",
         new String[]{
            "effects.emerald.passives.looting.enabled",
            "effects.emerald.passives.luck.enabled",
            "effects.emerald.passives.hero_of_the_village.enabled",
            "effects.apophis.passives.spark_tool_bonus.enabled"
         }
      );
      return m;
   }

   private static YamlConfiguration template(JavaPlugin plugin) {
      InputStream in = plugin.getResource("config.yml");
      if (in == null) {
         return new YamlConfiguration();
      } else {
         return YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
      }
   }

   private static boolean isSet(YamlConfiguration cfg, String path) {
      return cfg.contains(path, true) && !cfg.isConfigurationSection(path);
   }

   /**
    * Converts the plugin's config.yml if it is from an older version. Returns true when the file was rewritten (the
    * plugin's config is reloaded in that case).
    */
   public static boolean isBroken() {
      return broken;
   }

   /** Checks that config.yml can be read. If it can't, the error is logged and isBroken() is true. */
   public static boolean readable(JavaPlugin plugin) {
      File file = new File(plugin.getDataFolder(), "config.yml");
      try {
         if (file.exists()) {
            new YamlConfiguration().load(file);
         }

         broken = false;
         return true;
      } catch (IOException | org.bukkit.configuration.InvalidConfigurationException ex) {
         broken = true;
         plugin.getLogger().severe("[config] config.yml can't be read (" + ex.getMessage().split("\n")[0]
            + "). The settings in use were kept; fix the file and use /infuseconfig reload.");
         return false;
      }
   }

   public static boolean run(JavaPlugin plugin) {
      File file = new File(plugin.getDataFolder(), "config.yml");
      if (!file.exists()) {
         broken = false;
         return false;
      }

      // A file with a YAML error must not be taken for an old, empty one: it would be replaced with the defaults.
      YamlConfiguration old = new YamlConfiguration();
      try {
         old.load(file);
         broken = false;
      } catch (IOException | org.bukkit.configuration.InvalidConfigurationException ex) {
         broken = true;
         plugin.getLogger().severe("[config] config.yml can't be read, so the defaults are used until it is fixed (nothing was changed in the file): "
            + ex.getMessage());
         return false;
      }

      if (old.getInt("config_version", 0) >= VERSION) {
         return fillMissing(plugin, file, old);
      }

      String from = old.contains("effect_settings", true) ? "2.5" : "2.4";
      YamlConfiguration fresh = template(plugin);
      Set<String> used = new HashSet<>();

      // 1) Values already in the current layout (a file without config_version) are kept as they are.
      for (String path : fresh.getKeys(true)) {
         // (In 2.4 and 2.5 "effects.<effect>" was a number, the craft limit, so these paths can't match there.)
         if (!fresh.isConfigurationSection(path) && !path.contains(".recipe.") && !path.equals("config_version") && isSet(old, path)) {
            fresh.set(path, old.get(path));
            used.add(path);
         }
      }

      // 2) 2.4 ritual time for every effect (the per-effect 2.5 value, if any, wins below).
      if (isSet(old, "durations.ritual_duration")) {
         for (String e : EFFECTS) {
            fresh.set("effects." + e + ".craft.augmented_ritual_seconds", old.getInt("durations.ritual_duration"));
         }

         used.add("durations.ritual_duration");
      }

      // 3) Very old files had augments/effects/durations/cooldowns/damage/radius under "configs".
      for (String sec : new String[]{"augments", "effects", "durations", "cooldowns", "damage", "radius"}) {
         ConfigurationSection nested = old.getConfigurationSection("configs." + sec);
         if (nested != null && !old.isConfigurationSection(sec)) {
            for (String key : nested.getKeys(true)) {
               if (!nested.isConfigurationSection(key)) {
                  old.set(sec + "." + key, nested.get(key));
                  used.add("configs." + sec + "." + key);
               }
            }
         }
      }

      // 4) Moves one to one, and the 2.4 switches that controlled several passives.
      for (Map.Entry<String, String> move : simpleMoves().entrySet()) {
         if (isSet(old, move.getKey())) {
            fresh.set(move.getValue(), old.get(move.getKey()));
            used.add(move.getKey());
         }
      }

      for (Map.Entry<String, String[]> move : multiMoves().entrySet()) {
         if (isSet(old, move.getKey())) {
            for (String target : move.getValue()) {
               fresh.set(target, old.get(move.getKey()));
            }

            used.add(move.getKey());
         }
      }

      // 5) 2.5 passives keep their names. Ocean's Speed in water was removed (it gives Regeneration now).
      for (String e : EFFECTS) {
         ConfigurationSection passives = old.getConfigurationSection("effect_settings." + e + ".passives");
         if (passives != null) {
            for (String key : passives.getKeys(true)) {
               if (!passives.isConfigurationSection(key)) {
                  String oldPath = "effect_settings." + e + ".passives." + key;
                  String newPath = "effects." + e + ".passives." + key;
                  used.add(oldPath);
                  if (fresh.contains(newPath)) {
                     fresh.set(newPath, passives.get(key));
                  } else {
                     plugin.getLogger().info("[config] '" + oldPath + "' no longer exists and was not carried over");
                  }
               }
            }
         }
      }

      // 6) Recipes: recipes.<effect> (2.4 also had Apophis's recipe at the top level as "apophis").
      for (String e : EFFECTS) {
         ConfigurationSection recipe = old.getConfigurationSection("recipes." + e);
         String prefix = "recipes." + e;
         if (recipe == null && e.equals("apophis") && old.isConfigurationSection("apophis") && old.contains("apophis.shape", true)) {
            recipe = old.getConfigurationSection("apophis");
            prefix = "apophis";
         }

         if (recipe == null && old.isConfigurationSection("effects." + e + ".craft.recipe")) {
            recipe = old.getConfigurationSection("effects." + e + ".craft.recipe");
            prefix = "effects." + e + ".craft.recipe";
         }

         String target = Settings.recipePath(e);
         if (recipe != null && recipe.contains("shape") && recipe.isConfigurationSection("ingredients") && sameRecipe(recipe, fresh.getConfigurationSection(target))) {
            // Unchanged recipe: the template's copy (with its comments) stays.
            for (String key : recipe.getKeys(true)) {
               used.add(prefix + "." + key);
            }
         } else if (recipe != null && recipe.contains("shape") && recipe.isConfigurationSection("ingredients")) {
            fresh.set(target, null);
            List<String> shape = recipe.getStringList("shape");
            fresh.set(target + ".shape", shape);
            ConfigurationSection ings = recipe.getConfigurationSection("ingredients");
            String letters = String.join("", shape);

            // Only the letters the shape uses: 2.5 saved the jar's letters into changed recipes too, and a letter
            // that isn't in the shape makes the whole recipe fail to load.
            for (String symbol : ings.getKeys(false)) {
               if (symbol.length() == 1 && letters.indexOf(symbol.charAt(0)) >= 0) {
                  fresh.set(target + ".ingredients." + symbol, ings.getString(symbol));
               }
            }

            for (String key : recipe.getKeys(true)) {
               used.add(prefix + "." + key);
            }
         }
      }

      // Anything else in the old file is reported so nothing disappears silently (the backup still has it).
      List<String> dropped = new ArrayList<>();

      for (String path : old.getKeys(true)) {
         if (!old.isConfigurationSection(path) && !used.contains(path) && !DEAD_KEYS.contains(path) && !path.equals("config_version")
            && !path.matches("recipes\\.[a-z]+\\.key") && !path.equals("apophis.key")) {
            dropped.add(path);
         }
      }

      fresh.set("config_version", VERSION);
      File backup = new File(plugin.getDataFolder(), "config-backup-" + from + ".yml");
      if (backup.exists()) {
         backup = new File(plugin.getDataFolder(), "config-backup-" + from + "-" + System.currentTimeMillis() + ".yml");
      }

      try {
         Files.copy(file.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
         fresh.save(file);
      } catch (IOException ex) {
         plugin.getLogger().severe("[config] Could not convert config.yml to the new layout: " + ex.getMessage());
         return false;
      }

      plugin.getLogger().info("[config] config.yml from Infuse " + from + " was converted to the new layout (everything for an effect is now"
         + " in effects.<effect>). The old file was saved as " + backup.getName());
      if (!dropped.isEmpty()) {
         plugin.getLogger().warning("[config] These old settings don't exist any more and were not carried over: " + String.join(", ", dropped));
      }

      plugin.reloadConfig();
      return true;
   }

   /**
    * A current-layout file that lacks some settings (deleted by hand, or added in a later version) gets them from the
    * jar's config.yml, so the editor, the file and the plugin always agree. A missing recipe is added as a whole.
    */
   private static boolean fillMissing(JavaPlugin plugin, File file, YamlConfiguration current) {
      YamlConfiguration template = template(plugin);
      List<String> added = new ArrayList<>();

      for (String path : template.getKeys(true)) {
         if (!template.isConfigurationSection(path) && !path.contains(".recipe.") && !current.contains(path, true)) {
            current.set(path, template.get(path));
            added.add(path);
         }
      }

      for (String e : EFFECTS) {
         String recipe = Settings.recipePath(e);
         if (!current.contains(recipe, true) && template.isConfigurationSection(recipe)) {
            current.set(recipe + ".shape", template.getStringList(recipe + ".shape"));
            ConfigurationSection ings = template.getConfigurationSection(recipe + ".ingredients");

            for (String symbol : ings.getKeys(false)) {
               current.set(recipe + ".ingredients." + symbol, ings.getString(symbol));
            }

            added.add(recipe);
         }
      }

      if (added.isEmpty()) {
         return false;
      }

      try {
         current.save(file);
      } catch (IOException ex) {
         plugin.getLogger().warning("[config] Could not add missing settings to config.yml: " + ex.getMessage());
         return false;
      }

      plugin.getLogger().info("[config] Added missing settings to config.yml with their default values: " + String.join(", ", added));
      plugin.reloadConfig();
      return true;
   }

   private static Map<String, String> ingredients(ConfigurationSection recipe) {
      Map<String, String> out = new LinkedHashMap<>();
      ConfigurationSection ings = recipe == null ? null : recipe.getConfigurationSection("ingredients");
      if (ings != null) {
         for (String symbol : ings.getKeys(false)) {
            out.put(symbol, String.valueOf(ings.getString(symbol)).toUpperCase(java.util.Locale.ROOT));
         }
      }

      return out;
   }

   private static boolean sameRecipe(ConfigurationSection a, ConfigurationSection b) {
      return a != null && b != null && a.getStringList("shape").equals(b.getStringList("shape")) && ingredients(a).equals(ingredients(b));
   }
}
