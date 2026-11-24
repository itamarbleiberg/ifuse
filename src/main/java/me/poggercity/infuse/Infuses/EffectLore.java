package me.poggercity.infuse.Infuses;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import me.poggercity.infuse.Managers.Infuse;
import me.poggercity.infuse.Managers.Settings;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Builds the potion lore from the config: disabled passives are left out, and numbers (chances, hit counts, levels,
 * durations, cooldowns) are the configured ones. The lore used to be fixed text, so it often didn't match what the
 * effect did (for example Emerald said 50% and 3x exp while the code used 25% and 1.5x).
 */
public final class EffectLore {
   private static final String GRAY_TEXT = "§x§6§D§6§D§6§D";

   private EffectLore() {
   }

   private static String hex(String hex) {
      return HexColorTranslator.translateToMinecraftColor(hex);
   }

   private static FileConfiguration cfg() {
      return Infuse.getInstance().getConfig();
   }

   private static String num(double value) {
      return value == Math.rint(value) ? String.valueOf((long)value) : String.format(Locale.ROOT, "%.1f", value);
   }

   private static String v(String effect, String passive, String key, double def) {
      return num(Settings.value(effect, passive, key, def));
   }

   private static String ordinal(String n) {
      if (n.endsWith("1") && !n.endsWith("11")) {
         return n + "st";
      } else if (n.endsWith("2") && !n.endsWith("12")) {
         return n + "nd";
      } else {
         return n.endsWith("3") && !n.endsWith("13") ? n + "rd" : n + "th";
      }
   }

   private static final class Builder {
      final String effect;
      final String header;
      final String bullet;
      final String icon;
      final List<String> passives = new ArrayList<>();
      final List<String> spark = new ArrayList<>();

      Builder(String effect, String header, String bullet, String icon) {
         this.effect = effect;
         this.header = header;
         this.bullet = bullet;
         this.icon = icon;
      }

      Builder passive(String passive, String... lines) {
         if (passive == null || Settings.passive(this.effect, passive)) {
            for (String line : lines) {
               this.passives.add(this.bullet + this.icon + " " + GRAY_TEXT + line);
            }
         }

         return this;
      }

      Builder spark(String... lines) {
         for (String line : lines) {
            this.spark.add(this.bullet + this.icon + " " + GRAY_TEXT + line);
         }

         return this;
      }

      List<String> build(int durationSeconds, int cooldownSeconds) {
         List<String> lore = new ArrayList<>();
         lore.add(this.header + ChatColor.BOLD + "ᴘᴀꜱꜱɪᴠᴇ ᴇꜰꜰᴇᴄᴛꜱ:");
         lore.addAll(this.passives);
         lore.add(ChatColor.GRAY.toString());
         lore.add(this.header + ChatColor.BOLD + "ꜱᴘᴀʀᴋ ᴇꜰꜰᴇᴄᴛꜱ:");
         lore.addAll(this.spark);
         lore.add(ChatColor.GRAY.toString());
         lore.add(hex("#007DD0") + "ᴅᴜʀᴀᴛɪᴏɴ: " + durationSeconds + "s");
         lore.add(hex("#007DD0") + "ᴄᴏᴏʟᴅᴏᴡɴ: " + cooldownSeconds + "s");
         return lore;
      }
   }

   private static int cooldown(String key, int def, boolean augmented) {
      int base = cfg().getInt("cooldowns." + key, def);
      return augmented ? base / 2 : base;
   }

   private static int duration(String key, int def) {
      return cfg().getInt("durations." + key, def);
   }

   public static List<String> lore(String effect, boolean augmented) {
      switch (effect) {
         case "emerald": {
            String e = "emerald";
            return new Builder(e, hex("#09B000"), hex("#0DFC00"), "\ud83d\udcb2")
               .passive("consumable_refund", "Consumables have a " + v(e, "consumable_refund", "chance", 25) + "% chance of not being consumed")
               .passive("throwable_refund", "Wind charges and XP bottles have a " + v(e, "throwable_refund", "chance", 25) + "% chance to come back")
               .passive("enchanting_table", "Enchanting table always on level 30")
               .passive("exp_boost", "Gain " + v(e, "exp_boost", "multiplier", 1.5) + "x exp")
               .passive("looting", "Swords get Looting " + v(e, "looting", "level", 5))
               .passive("luck", "Luck " + v(e, "luck", "level", 10))
               .passive("hero_of_the_village", "Hero of the village " + v(e, "hero_of_the_village", "level", 3))
               .spark("Hero of the village 255")
               .spark(
                  "Refund chance " + v(e, "consumable_refund", "spark_chance", 50) + "%, " + v(e, "exp_boost", "spark_multiplier", 3.0) + "x exp"
               )
               .build(duration("emerald_spark", 10), cooldown("emerald_spark", 30, augmented));
         }
         case "feather": {
            String e = "feather";
            return new Builder(e, hex("#84728C"), hex("#BEA3CA"), "☁")
               .passive("no_fall_damage", "No Fall damage")
               .passive(
                  "fall_attack",
                  "Attacking from a " + v(e, "fall_attack", "min_fall", 7) + "+ block fall does " + v(e, "fall_attack", "multiplier", 1.5) + "x damage"
               )
               .passive("wind_charge_counter", "Auto windcharge counter after being attacked " + v(e, "wind_charge_counter", "hits", 10) + " times")
               .passive("wind_charge_cooldown", "Windcharges have " + num(Settings.value(e, "wind_charge_cooldown", "ticks", 5) / 10.0) + "x cooldown")
               .passive("wind_charge_boost", "Windcharges have " + v(e, "wind_charge_boost", "velocity", 2.0) + "x velocity", "Windcharges go perfectly straight")
               .spark("Launches the player upward", "Slams back into the floor damaging nearby players")
               .build(5, cooldown("feather_spark", 40, augmented));
         }
         case "fire": {
            String e = "fire";
            return new Builder(e, hex("#B05302"), hex("#FC7803"), "\ud83d\udd25")
               .passive("fire_resistance", "Fire Resistance", "Swim in Lava")
               .passive("lava_flight", "Fly through lava")
               .passive("auto_smelt", "Auto smelts ores")
               .passive("ignite_on_hit", "Set players on fire every " + v(e, "ignite_on_hit", "hits", 10) + " hits")
               .passive("fire_damage_boost", "Fire damage increases over time")
               .passive("evaporate_water", "Evaporate water")
               .passive("lava_bucket_clutch", "Lava buckets cancel fall damage")
               .passive("flame_arrows", "Fully charged arrows are set on fire")
               .spark("Ignite all players within " + num(cfg().getDouble("radius.fire", 5.0)) + " blocks", "Final Ignition erupts and launches all players upward")
               .build(duration("fire_spark", 4), cooldown("fire_spark", 60, augmented));
         }
         case "frost": {
            String e = "frost";
            return new Builder(e, hex("#559FB0"), hex("#7AE3FC"), "❄")
               .passive("freeze_on_hit", "Freezes players every " + ordinal(v(e, "freeze_on_hit", "hits", 10)) + " hit")
               .passive("freeze_mobs", "Freezes mobs every hit")
               .passive("ice_speed", "Speed " + v(e, "ice_speed", "level", 3) + " on ice and snow blocks")
               .passive("frozen_wind_charge_cooldown", "Frozen enemies' wind charge cooldown is " + v(e, "frozen_wind_charge_cooldown", "seconds", 3) + " seconds")
               .spark("Freezes players on every hit", "Enemies within " + num(cfg().getDouble("radius.frost", 5.0)) + " blocks can't jump")
               .build(duration("frost_spark", 10), cooldown("frost_spark", 90, augmented));
         }
         case "haste": {
            String e = "haste";
            return new Builder(e, hex("#804B00"), hex("#B96C00"), "⛏")
               .passive(
                  "tool_enchants",
                  "Fortune " + v(e, "tool_enchants", "fortune", 5),
                  "Unbreaking " + v(e, "tool_enchants", "unbreaking", 5),
                  "Efficiency " + v(e, "tool_enchants", "efficiency", 10)
               )
               .passive("haste_effect", "Haste " + v(e, "haste_effect", "level", 2))
               .passive("vein_miner", "Vein miner when crouched")
               .passive("shield_cooldown", "Axes disable your shield for only " + v(e, "shield_cooldown", "seconds", 2.5) + "s")
               .spark("Attack faster", "Mine faster")
               .build(duration("haste_spark", 15), cooldown("haste_spark", 60, augmented));
         }
         case "heart": {
            String e = "heart";
            return new Builder(e, hex("#B00030"), hex("#FC0046"), "❤")
               .passive("extra_hearts", "+" + v(e, "extra_hearts", "hearts", 5) + " Hearts")
               .passive("food_absorption", "All food gives absorption")
               .passive(
                  "golden_apple_absorption",
                  "Egaps give +" + num(Settings.value(e, "golden_apple_absorption", "level", 5) * 2.0) + " absorption hearts"
               )
               .passive("health_display", "See player's health every " + v(e, "health_display", "hits", 10) + " hits")
               .spark("Heal players to 20 hearts instantly")
               .build(duration("heart_spark", 60), cooldown("heart_spark", 120, augmented));
         }
         case "invis": {
            String e = "invis";
            String hits = v(e, "blind_on_hit", "hits", 10);
            return new Builder(e, hex("#5800B0"), hex("#7E00FC"), "\ud83d\udc41")
               .passive("invisibility", "Invis")
               .passive("blind_on_hit", "Hide enemies health every " + hits + " attacks", "Blinds enemies every " + hits + " attacks")
               .passive("mob_invisibility", "Fully invisible to mobs")
               .passive("arrow_blind", "Bow shots give blindness")
               .spark("Summons a smoke bomb", "Armor Invis to user", "Blinds and hides health of all enemies within the smoke screen")
               .build(duration("invis_spark", 10), cooldown("invis_spark", 90, augmented));
         }
         case "ocean": {
            String e = "ocean";
            return new Builder(e, hex("#003EB0"), hex("#005AFC"), "\ud83c\udf0a")
               .passive("swim_faster", "Swim faster")
               .passive("conduit_power", "Breathe underwater")
               .passive(
                  "water_bonus",
                  "+" + v(e, "water_bonus", "extra_hearts", 2) + " hearts and Speed " + v(e, "water_bonus", "speed_level", 1) + " while in water"
               )
               .passive("drowning_aura", "Make everyone around you start drowning")
               .passive("trident_pull", "Tridents pull players")
               .spark("Summons a Whirlhole", "Pulls in all players and entities in " + num(cfg().getDouble("radius.ocean", 15.0)) + " blocks")
               .build(duration("ocean_spark", 5), cooldown("ocean_spark", 90, augmented));
         }
         case "regen": {
            String e = "regen";
            return new Builder(e, hex("#B0009A"), hex("#FC00DD"), "+")
               .passive(
                  "regen_on_hit",
                  "Every hit grants Regeneration " + v(e, "regen_on_hit", "level", 2) + " for " + v(e, "regen_on_hit", "seconds", 2) + " seconds"
               )
               .passive("extra_saturation", "Eating gives " + num(Settings.value(e, "extra_saturation", "amount", 6) / 2.0) + " extra saturation bars")
               .spark("Dealing damage heals you")
               .build(duration("regen_spark", 30), cooldown("regen_spark", 60, augmented));
         }
         case "speed": {
            String e = "speed";
            return new Builder(e, hex("#b5883a"), hex("#d1a44b"), "⏩")
               .passive(
                  "speed_stacking",
                  "Speed " + v(e, "speed_stacking", "base_level", 1),
                  "Increase speed level by 1 after each hit (up to Speed " + v(e, "speed_stacking", "max_level", 3) + ")",
                  "Speed resets after " + v(e, "speed_stacking", "reset_seconds", 0.5) + " seconds of no activity"
               )
               .spark("Speed Dash")
               .build(1, cooldown("speed_spark", 20, augmented));
         }
         case "strength": {
            String e = "strength";
            return new Builder(e, hex("#6F1616"), hex("#9E2020"), "\ud83d\udee1")
               .passive("mob_damage", v(e, "mob_damage", "multiplier", 2.0) + "x damage to all mobs")
               .passive("shield_disable", "Disable shields for " + v(e, "shield_disable", "seconds", 10) + " seconds")
               .passive("piercing_arrows", "Arrows pierce through enemies")
               .passive("low_health_damage", "+1 Damage when under 6 Hearts", "+2 Damage when under 4 Hearts", "+3 Damage when under 2 Hearts")
               .spark("Activate auto-crit for " + duration("strength_spark", 15) + " seconds")
               .build(duration("strength_spark", 15), cooldown("strength_spark", 60, augmented));
         }
         case "thunder": {
            String e = "thunder";
            return new Builder(e, hex("#B0A600"), hex("#FCED00"), "⚡")
               .passive(
                  "smite_on_hit",
                  "Smite players every " + ordinal(v(e, "smite_on_hit", "hits", 10)) + " hit (" + ordinal(v(e, "smite_on_hit", "rain_hits", 5)) + " hit if its raining)"
               )
               .passive("chain_lightning", "Thunder chains to nearby enemies")
               .passive("trident_smite", "Tridents smite enemies when thrown")
               .spark("Smite all players within " + num(cfg().getDouble("radius.thunder", 7.0)) + " blocks", "Each player can be only smited 3 times")
               .build(duration("thunder_spark", 10), cooldown("thunder_spark", 90, augmented));
         }
         case "ender": {
            String e = "ender";
            return new Builder(e, hex("#3A0735"), hex("#541351"), "⭐")
               .passive("glowing", "All nearby untrusted players have glowing")
               .passive("dragon_fireball", "Use dragon's breath to shoot powerful fireballs that curse players")
               .passive("curse", "Cursed players share damage with all cursed players")
               .spark(
                  "Teleport to the cursor position within a 15 block radius",
                  "Instantly kills any mob and curses players",
                  "Cursing a player resets the spark cooldown"
               )
               .build(1, cfg().getInt("cooldowns.ender_spark", 10));
         }
         case "apophis": {
            String c = hex("#301145");
            String t = "§x§3§0§1§1§4§5";
            List<String> lore = new ArrayList<>();
            lore.add(c + ChatColor.BOLD + "ᴘᴀꜱꜱɪᴠᴇ ᴇꜰꜰᴇᴄᴛꜱ:");
            lore.add(c + "❤ " + t + "+5 Hearts");
            lore.add(c + "\ud83d\udee1 " + t + "Double damage to mobs & disable shields (10s)");
            if (Settings.passive("apophis", "water_breathing")) {
               lore.add(c + "\ud83c\udf0a " + t + "Swim faster & breathe underwater");
            }

            if (Settings.passive("apophis", "fire_resistance") || Settings.passive("apophis", "lava_flight")) {
               lore.add(c + "\ud83d\udd25 " + t + "Fire Resistance, swim/fly in lava");
            }

            lore.add(c + "\ud83d\udcb2 " + t + "50% chance consumables aren't consumed");
            lore.add(ChatColor.GRAY.toString());
            lore.add(c + ChatColor.BOLD + "ꜱᴘᴀʀᴋ ᴇꜰꜰᴇᴄᴛꜱ:");
            lore.add(c + "◆ Combines Heart + Strength + Emerald sparks");
            lore.add(c + "◆ Inflicts Blindness & Wither to enemies in " + num(cfg().getDouble("radius.apophis", 5.0)) + " blocks for 10s");
            lore.add(c + "◆ Your hits apply Wither I for 15s");
            lore.add(ChatColor.GRAY.toString());
            lore.add(hex("#007DD0") + "ᴅᴜʀᴀᴛɪᴏɴ: " + duration("apophis_spark", 20) + "s");
            lore.add(hex("#007DD0") + "ᴄᴏᴏʟᴅᴏᴡɴ: " + cooldown("apophis_spark", 90, augmented) + "s");
            return lore;
         }
         default:
            return new ArrayList<>();
      }
   }
}
