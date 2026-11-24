package me.poggercity.infuse.Managers;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.World.Environment;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

/**
 * /infuseconfig: in-game settings editor built with Minecraft dialogs (1.21.6+). One page per effect with its spark
 * settings, particles, every passive and its ritual (dimensions, ritual times, craft limits), plus a page for the
 * global ritual settings. Saving writes config.yml and applies the change right away.
 */
public class ConfigDialog implements CommandExecutor {
   public static final String PERMISSION = "infuse.config";
   private static final ClickCallback.Options CALLBACK_OPTIONS = ClickCallback.Options.builder()
      .uses(ClickCallback.UNLIMITED_USES)
      .lifetime(Duration.ofHours(1))
      .build();
   private final Infuse plugin;

   private enum Kind {
      BOOL,
      INT,
      DECIMAL
   }

   /** One editable setting: a config path shown as a toggle or a slider. */
   private static final class Field {
      final String path;
      final String label;
      final Kind kind;
      final float min;
      final float max;
      final float step;

      Field(String path, String label, Kind kind, float min, float max, float step) {
         this.path = path;
         this.label = label;
         this.kind = kind;
         this.min = min;
         this.max = max;
         this.step = step;
      }
   }

   public ConfigDialog(Infuse plugin) {
      this.plugin = plugin;
   }

   private FileConfiguration cfg() {
      return this.plugin.getConfig();
   }

   @Override
   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (!(sender instanceof Player player)) {
         sender.sendMessage("The settings editor opens a dialog, so it can only be used in game. Edit config.yml instead.");
         return true;
      } else if (!player.hasPermission(PERMISSION)) {
         player.sendMessage(Component.text("You don't have permission to change Infuse settings.", NamedTextColor.RED));
         return true;
      } else {
         if (args.length == 1 && List.of(Infuse.EFFECT_KEYS).contains(args[0].toLowerCase(Locale.ROOT))) {
            this.openEffect(player, args[0].toLowerCase(Locale.ROOT));
         } else {
            this.openMenu(player);
         }

         return true;
      }
   }

   private static String title(String effect) {
      return Character.toUpperCase(effect.charAt(0)) + effect.substring(1);
   }

   private static String humanize(String key) {
      String s = key.replace('_', ' ');
      return Character.toUpperCase(s.charAt(0)) + s.substring(1);
   }

   private static boolean allowed(Audience audience) {
      return !(audience instanceof Player p) || p.hasPermission(PERMISSION);
   }

   private static ActionButton button(String label, NamedTextColor color, String tooltip, int width, DialogAction action) {
      ActionButton.Builder b = ActionButton.builder(Component.text(label, color)).width(width);
      if (action != null) {
         b.action(action);
      }

      if (tooltip != null) {
         b.tooltip(Component.text(tooltip));
      }

      return b.build();
   }

   private DialogAction callback(io.papermc.paper.registry.data.dialog.action.DialogActionCallback cb) {
      return DialogAction.customClick(cb, CALLBACK_OPTIONS);
   }

   // ----- main menu -----

   public void openMenu(Player player) {
      List<ActionButton> buttons = new ArrayList<>();

      for (String effect : Infuse.EFFECT_KEYS) {
         buttons.add(button(title(effect), NamedTextColor.AQUA, "Ability, particles, passives and ritual of " + title(effect), 150, this.callback((view, audience) -> {
            if (audience instanceof Player p && allowed(p)) {
               this.openEffect(p, effect);
            }
         })));
      }

      buttons.add(button("Rituals (all effects)", NamedTextColor.GOLD, "Settings that apply to every ritual", 150, this.callback((view, audience) -> {
         if (audience instanceof Player p && allowed(p)) {
            this.openGeneral(p);
         }
      })));
      Dialog dialog = Dialog.create(
         b -> b.empty()
            .base(
               DialogBase.builder(Component.text("Infuse settings", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD))
                  .canCloseWithEscape(true)
                  .body(List.of(DialogBody.plainMessage(Component.text("Pick an effect to change its ability, particles, passives and ritual."))))
                  .build()
            )
            .type(DialogType.multiAction(buttons).columns(2).exitAction(button("Close", NamedTextColor.GRAY, null, 150, null)).build())
      );
      player.showDialog(dialog);
   }

   // ----- per-effect page -----

   private static final String[][] ABILITY = new String[][]{
      {"emerald", "cooldowns.emerald_spark", "durations.emerald_spark"},
      {"feather", "cooldowns.feather_spark", "damage.feather_spark", "radius.feather"},
      {"fire", "cooldowns.fire_spark", "durations.fire_spark", "radius.fire", "damage.fire_launch_strength"},
      {"frost", "cooldowns.frost_spark", "durations.frost_spark", "radius.frost"},
      {"haste", "cooldowns.haste_spark", "durations.haste_spark"},
      {"heart", "cooldowns.heart_spark", "durations.heart_spark"},
      {"invis", "cooldowns.invis_spark", "durations.invis_spark", "radius.invis"},
      {"ocean", "cooldowns.ocean_spark", "durations.ocean_spark", "radius.ocean"},
      {"regen", "cooldowns.regen_spark", "durations.regen_spark"},
      {"speed", "cooldowns.speed_spark", "damage.speed_dash_strength"},
      {"strength", "cooldowns.strength_spark", "durations.strength_spark"},
      {"thunder", "cooldowns.thunder_spark", "durations.thunder_spark", "radius.thunder"},
      {"ender", "cooldowns.ender_spark", "damage.dragon"},
      {"apophis", "cooldowns.apophis_spark", "durations.apophis_spark", "radius.apophis"}
   };

   private static Field abilityField(String path) {
      if (path.startsWith("cooldowns.")) {
         return new Field(path, "Spark cooldown (seconds)", Kind.INT, 0.0F, 600.0F, 1.0F);
      } else if (path.startsWith("durations.")) {
         return new Field(path, "Spark duration (seconds)", Kind.INT, 0.0F, 300.0F, 1.0F);
      } else if (path.startsWith("radius.")) {
         return new Field(path, "Spark radius (blocks)", Kind.DECIMAL, 0.0F, 50.0F, 0.5F);
      } else if (path.equals("damage.fire_launch_strength")) {
         return new Field(path, "Final ignition launch strength", Kind.DECIMAL, 0.0F, 10.0F, 0.5F);
      } else if (path.equals("damage.speed_dash_strength")) {
         return new Field(path, "Dash strength", Kind.DECIMAL, 0.0F, 5.0F, 0.1F);
      } else {
         return new Field(path, "Spark damage", Kind.DECIMAL, 0.0F, 40.0F, 0.5F);
      }
   }

   /** Slider range and step for a passive setting, based on its name and default. */
   private static Field passiveField(String path, String passive, String param, double def) {
      String label = humanize(passive) + ": " + param.replace('_', ' ');
      switch (param) {
         case "chance":
         case "spark_chance":
            return new Field(path, label + " (%)", Kind.INT, 0.0F, 100.0F, 1.0F);
         case "multiplier":
         case "spark_multiplier":
            return new Field(path, label, Kind.DECIMAL, 0.0F, 10.0F, 0.1F);
         case "hits":
         case "rain_hits":
            return new Field(path, label, Kind.INT, 1.0F, 50.0F, 1.0F);
         case "seconds":
         case "cooldown_seconds":
            return new Field(path, label, Kind.DECIMAL, 0.0F, 300.0F, 0.5F);
         case "reset_seconds":
            return new Field(path, label, Kind.DECIMAL, 0.1F, 10.0F, 0.1F);
         case "ticks":
            return new Field(path, label + " (20 = 1 s)", Kind.INT, 0.0F, 100.0F, 1.0F);
         case "radius":
         case "spark_radius":
            return new Field(path, label + " (blocks)", Kind.DECIMAL, 0.0F, (float)Math.max(20.0, def * 2.0), 0.5F);
         case "damage":
         case "hearts":
            return new Field(path, label, Kind.DECIMAL, 0.0F, 40.0F, 0.5F);
         case "step":
            return new Field(path, label + " (x per hit)", Kind.DECIMAL, 1.0F, 3.0F, 0.05F);
         case "max":
            return new Field(path, label + " (x)", Kind.DECIMAL, 1.0F, 10.0F, 0.1F);
         case "strength":
         case "velocity":
            return new Field(path, label, Kind.DECIMAL, 0.0F, 5.0F, 0.1F);
         case "max_blocks":
            return new Field(path, label, Kind.INT, 1.0F, 256.0F, 1.0F);
         case "min_fall":
            return new Field(path, label + " (blocks)", Kind.INT, 0.0F, 50.0F, 1.0F);
         case "efficiency":
         case "luck":
         case "level":
            return new Field(path, label, Kind.INT, 0.0F, 20.0F, 1.0F);
         default:
            return new Field(path, label, Kind.INT, 0.0F, (float)Math.max(10.0, def * 4.0), 1.0F);
      }
   }

   private List<Field> fieldsFor(String effect) {
      List<Field> fields = new ArrayList<>();

      for (String[] row : ABILITY) {
         if (row[0].equals(effect)) {
            for (int i = 1; i < row.length; i++) {
               fields.add(abilityField(row[i]));
            }
         }
      }

      String base = Settings.ROOT + "." + effect;
      fields.add(new Field(base + ".particles.aura_enabled", "Aura particles", Kind.BOOL, 0.0F, 0.0F, 0.0F));
      fields.add(new Field(base + ".particles.aura_amount", "Aura particles amount", Kind.INT, 0.0F, 20.0F, 1.0F));
      fields.add(new Field(base + ".particles.ability_percent", "Spark particles (%)", Kind.INT, 0.0F, 200.0F, 10.0F));
      ConfigurationSection defaults = this.cfg().getDefaults() == null ? null : this.cfg().getDefaults().getConfigurationSection(base + ".passives");
      ConfigurationSection passives = defaults != null ? defaults : this.cfg().getConfigurationSection(base + ".passives");
      if (passives != null) {
         for (String passive : passives.getKeys(false)) {
            String ppath = base + ".passives." + passive;
            fields.add(new Field(ppath + ".enabled", humanize(passive), Kind.BOOL, 0.0F, 0.0F, 0.0F));
            ConfigurationSection params = passives.getConfigurationSection(passive);
            if (params != null) {
               for (String param : params.getKeys(false)) {
                  if (!param.equals("enabled")) {
                     fields.add(passiveField(ppath + "." + param, passive, param, params.getDouble(param)));
                  }
               }
            }
         }
      }

      fields.add(new Field(base + ".ritual.augmented_seconds", "Ritual time: augmented (seconds)", Kind.INT, 0.0F, 3600.0F, 10.0F));
      fields.add(new Field(base + ".ritual.normal_seconds", "Ritual time: normal (seconds, 0 = instant)", Kind.INT, 0.0F, 3600.0F, 10.0F));
      fields.add(new Field("augments." + effect, "Augmented crafts allowed (-1 = unlimited)", Kind.INT, -1.0F, 20.0F, 1.0F));
      fields.add(new Field("effects." + effect, "Normal crafts allowed (-1 = unlimited)", Kind.INT, -1.0F, 50.0F, 1.0F));
      return fields;
   }

   private DialogInput input(String key, Field f) {
      Component label = Component.text(f.label);
      if (f.kind == Kind.BOOL) {
         return DialogInput.bool(key, label).initial(this.cfg().getBoolean(f.path, true)).build();
      } else {
         float value = (float)this.cfg().getDouble(f.path, f.min);
         // Never clamp a value the admin typed into config.yml: widen the slider instead.
         float min = Math.min(f.min, value);
         float max = Math.max(f.max, value);
         if (max <= min) {
            max = min + f.step;
         }

         return DialogInput.numberRange(key, label, min, max).step(f.step).initial(value).width(300).build();
      }
   }

   public void openEffect(Player player, String effect) {
      List<Field> fields = this.fieldsFor(effect);
      List<DialogInput> inputs = new ArrayList<>();

      for (int i = 0; i < fields.size(); i++) {
         inputs.add(this.input("f" + i, fields.get(i)));
      }

      Set<Environment> dims = Settings.ritualDimensions(effect);
      inputs.add(DialogInput.bool("dim_overworld", Component.text("Ritual allowed in the Overworld")).initial(dims.contains(Environment.NORMAL)).build());
      inputs.add(DialogInput.bool("dim_nether", Component.text("Ritual allowed in the Nether")).initial(dims.contains(Environment.NETHER)).build());
      inputs.add(DialogInput.bool("dim_end", Component.text("Ritual allowed in the End")).initial(dims.contains(Environment.THE_END)).build());
      ActionButton save = button("Save", NamedTextColor.GREEN, "Write these values to config.yml and apply them now", 150, this.callback((view, audience) -> {
         if (audience instanceof Player p && allowed(p)) {
            int changed = this.save(effect, fields, view);
            p.sendMessage(Component.text("Saved " + title(effect) + " settings (" + changed + " changed).", NamedTextColor.GREEN));
            this.openEffect(p, effect);
         }
      }));
      ActionButton back = button("Back", NamedTextColor.YELLOW, "Back to the effect list without saving", 150, this.callback((view, audience) -> {
         if (audience instanceof Player p && allowed(p)) {
            this.openMenu(p);
         }
      }));
      Dialog dialog = Dialog.create(
         b -> b.empty()
            .base(
               DialogBase.builder(Component.text(title(effect) + " settings", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD))
                  .canCloseWithEscape(true)
                  .body(
                     List.of(
                        DialogBody.plainMessage(
                           Component.text("Spark, particles, passives and ritual for " + title(effect) + ". Chances are in %. Nothing changes until you press Save.")
                        )
                     )
                  )
                  .inputs(inputs)
                  .build()
            )
            .type(DialogType.multiAction(List.of(save, back)).columns(2).exitAction(button("Close", NamedTextColor.GRAY, null, 150, null)).build())
      );
      player.showDialog(dialog);
   }

   // 5 in config.yml and 5.0 from a slider are the same setting, so it isn't rewritten or counted as a change.
   private static boolean sameValue(Object a, Object b) {
      if (a instanceof Number x && b instanceof Number y) {
         return Math.abs(x.doubleValue() - y.doubleValue()) < 1.0E-6;
      } else {
         return a.equals(b);
      }
   }

   /** Applies the dialog values. Returns how many settings changed. */
   private int save(String effect, List<Field> fields, DialogResponseView view) {
      FileConfiguration cfg = this.cfg();
      int changed = 0;

      for (int i = 0; i < fields.size(); i++) {
         Field f = fields.get(i);
         String key = "f" + i;
         Object newValue;
         if (f.kind == Kind.BOOL) {
            Boolean b = view.getBoolean(key);
            newValue = b;
         } else {
            Float v = view.getFloat(key);
            if (v == null) {
               newValue = null;
            } else if (f.kind == Kind.INT) {
               newValue = Math.round(v);
            } else {
               newValue = Math.round(v * 100.0) / 100.0;
            }
         }

         if (newValue != null && !sameValue(newValue, cfg.get(f.path))) {
            cfg.set(f.path, newValue);
            changed++;
         }
      }

      List<String> dims = new ArrayList<>();
      if (Boolean.TRUE.equals(view.getBoolean("dim_overworld"))) {
         dims.add("overworld");
      }

      if (Boolean.TRUE.equals(view.getBoolean("dim_nether"))) {
         dims.add("nether");
      }

      if (Boolean.TRUE.equals(view.getBoolean("dim_end"))) {
         dims.add("end");
      }

      String dimPath = Settings.ROOT + "." + effect + ".ritual.dimensions";
      if (!dims.equals(cfg.getStringList(dimPath))) {
         cfg.set(dimPath, dims);
         changed++;
      }

      if (changed > 0) {
         this.plugin.saveConfig();
         // Recipes are rebuilt so newly crafted potions show the new numbers in their lore.
         this.plugin.reloadConfigFiles();
      }

      return changed;
   }

   // ----- global ritual settings -----

   public void openGeneral(Player player) {
      List<Field> fields = List.of(
         new Field("rituals.one_at_a_time", "Only one ritual at a time on the server", Kind.BOOL, 0.0F, 0.0F, 0.0F),
         new Field("rituals.brewing_stand_opens_crafting", "Right-clicking a brewing stand opens a crafting table", Kind.BOOL, 0.0F, 0.0F, 0.0F),
         new Field("configs.brewing_beam", "Show a beam over running rituals", Kind.BOOL, 0.0F, 0.0F, 0.0F)
      );
      List<DialogInput> inputs = new ArrayList<>();

      for (int i = 0; i < fields.size(); i++) {
         inputs.add(this.input("f" + i, fields.get(i)));
      }

      ActionButton save = button("Save", NamedTextColor.GREEN, null, 150, this.callback((view, audience) -> {
         if (audience instanceof Player p && allowed(p)) {
            int changed = 0;

            for (int i = 0; i < fields.size(); i++) {
               Boolean value = view.getBoolean("f" + i);
               if (value != null && !value.equals(this.cfg().get(fields.get(i).path))) {
                  this.cfg().set(fields.get(i).path, value);
                  changed++;
               }
            }

            if (changed > 0) {
               this.plugin.saveConfig();
               this.plugin.reloadConfigFiles();
            }

            p.sendMessage(Component.text("Saved ritual settings (" + changed + " changed).", NamedTextColor.GREEN));
            this.openMenu(p);
         }
      }));
      ActionButton back = button("Back", NamedTextColor.YELLOW, null, 150, this.callback((view, audience) -> {
         if (audience instanceof Player p && allowed(p)) {
            this.openMenu(p);
         }
      }));
      Dialog dialog = Dialog.create(
         b -> b.empty()
            .base(
               DialogBase.builder(Component.text("Ritual settings", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD))
                  .canCloseWithEscape(true)
                  .body(List.of(DialogBody.plainMessage(Component.text("Each effect's dimensions and ritual times are on that effect's page."))))
                  .inputs(inputs)
                  .build()
            )
            .type(DialogType.multiAction(List.of(save, back)).columns(2).exitAction(button("Close", NamedTextColor.GRAY, null, 150, null)).build())
      );
      player.showDialog(dialog);
   }
}
