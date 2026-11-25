package me.poggercity.infuse.Managers;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.action.DialogActionCallback;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.World.Environment;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

/**
 * /infuseconfig: the in-game settings editor, built with Minecraft dialogs (1.21.6+).
 *
 * The pages are built from the config.yml inside the jar, so every setting in the file is in the editor, in the same
 * order and with the same explanations (the comments in the file). Values come from the server's config.yml.
 * Recipes have their own editor (RecipeEditor), where the items are placed in a crafting grid.
 */
public class ConfigDialog implements TabExecutor {
   public static final String PERMISSION = "infuse.config";
   private static final ClickCallback.Options CALLBACK_OPTIONS = ClickCallback.Options.builder()
      .uses(ClickCallback.UNLIMITED_USES)
      .lifetime(Duration.ofHours(1))
      .build();
   private static final String[] DIMENSIONS = new String[]{"overworld", "nether", "end"};
   private static final String[][] EFFECT_PARTS = new String[][]{
      {"craft", "Crafting & ritual", "How many can be made, ritual times and where it can be crafted"},
      {"spark", "Spark", "The ability: cooldown, duration, radius and strength"},
      {"particles", "Particles", "Aura particles and how many spark particles are shown"},
      {"passives", "Passives", "What the effect does all the time; every passive can be turned off"}
   };
   private final Infuse plugin;
   private final RecipeEditor recipeEditor;
   private YamlConfiguration template;

   /** One editable setting. */
   private static final class Field {
      final String path;
      final String label;
      final Object def;
      final String help;

      Field(String path, String label, Object def, String help) {
         this.path = path;
         this.label = label;
         this.def = def;
         this.help = help;
      }

      boolean isDimensions() {
         return this.path.endsWith(".dimensions");
      }
   }

   public ConfigDialog(Infuse plugin, RecipeEditor recipeEditor) {
      this.plugin = plugin;
      this.recipeEditor = recipeEditor;
   }

   private FileConfiguration cfg() {
      return this.plugin.getConfig();
   }

   /** The commented config.yml from the jar: the list of settings, their order and their explanations. */
   private YamlConfiguration template() {
      if (this.template == null) {
         InputStream in = this.plugin.getResource("config.yml");
         this.template = in == null ? new YamlConfiguration() : YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
      }

      return this.template;
   }

   // ----- command -----

   @Override
   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (args.length > 0 && args[0].equalsIgnoreCase("reload") && sender.hasPermission(PERMISSION)) {
         // Also from the console, after editing config.yml by hand.
         this.plugin.reloadConfigFiles();
         sender.sendMessage(ConfigMigration.isBroken()
            ? Component.text("config.yml has an error, see the console. The settings in use were kept.", NamedTextColor.RED)
            : Component.text("config.yml reloaded.", NamedTextColor.GREEN));
         return true;
      } else if (!(sender instanceof Player player)) {
         sender.sendMessage("The settings editor opens a dialog, so it can only be used in game. Edit config.yml instead (then /infuseconfig reload).");
         return true;
      } else if (!player.hasPermission(PERMISSION)) {
         player.sendMessage(Component.text("You don't have permission to change Infuse settings.", NamedTextColor.RED));
         return true;
      } else {
         String first = args.length > 0 ? args[0].toLowerCase(Locale.ROOT) : "";
         if (first.equals("general") || first.equals("rituals")) {
            this.openSection(player, first, title(first), () -> this.openMenu(player));
         } else if (first.equals("starting_effects") || first.equals("starting")) {
            this.openSection(player, "starting_effects", "Starting effects", () -> this.openMenu(player));
         } else if (isEffect(first)) {
            String part = args.length > 1 ? args[1].toLowerCase(Locale.ROOT) : "";
            String[] match = null;

            for (String[] p : EFFECT_PARTS) {
               if (p[0].equals(part)) {
                  match = p;
               }
            }

            if (part.equals("recipe")) {
               this.recipeEditor.open(player, first);
            } else if (match != null) {
               this.openSection(player, Settings.effectPath(first) + "." + match[0], title(first) + ": " + match[1], () -> this.openEffect(player, first));
            } else {
               this.openEffect(player, first);
            }
         } else {
            this.openMenu(player);
         }

         return true;
      }
   }

   @Override
   public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
      List<String> options = new ArrayList<>();
      if (args.length == 1) {
         options.add("general");
         options.add("starting_effects");
         options.add("rituals");
         options.add("reload");
         options.addAll(Arrays.asList(Infuse.EFFECT_KEYS));
      } else if (args.length == 2 && isEffect(args[0].toLowerCase(Locale.ROOT))) {
         for (String[] p : EFFECT_PARTS) {
            options.add(p[0]);
         }

         options.add("recipe");
      }

      String typed = args.length == 0 ? "" : args[args.length - 1].toLowerCase(Locale.ROOT);
      options.removeIf(o -> !o.startsWith(typed));
      return options;
   }

   private static boolean isEffect(String name) {
      return Arrays.asList(Infuse.EFFECT_KEYS).contains(name);
   }

   // ----- helpers -----

   private static String title(String key) {
      return key.isEmpty() ? key : Character.toUpperCase(key.charAt(0)) + key.substring(1);
   }

   private static String humanize(String key) {
      String s = key.replace('_', ' ').replace("0 0", "0, 0");
      return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
   }

   private static boolean allowed(Audience audience) {
      return audience instanceof Player p && p.hasPermission(PERMISSION);
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

   /** A button that runs only for players who still have the permission (a replayed click from anyone else does nothing). */
   private DialogAction callback(PlayerAction action) {
      DialogActionCallback cb = (view, audience) -> {
         if (allowed(audience)) {
            action.run(view, (Player)audience);
         }
      };
      return DialogAction.customClick(cb, CALLBACK_OPTIONS);
   }

   @FunctionalInterface
   private interface PlayerAction {
      void run(DialogResponseView view, Player player);
   }

   private static Dialog dialog(Component title, List<DialogBody> body, List<DialogInput> inputs, List<ActionButton> buttons, int columns) {
      return Dialog.create(
         b -> b.empty()
            .base(DialogBase.builder(title).canCloseWithEscape(true).body(body).inputs(inputs).build())
            .type(DialogType.multiAction(buttons).columns(columns).exitAction(button("Close", NamedTextColor.GRAY, null, 150, null)).build())
      );
   }

   private static Component heading(String text) {
      return Component.text(text, NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD);
   }

   // ----- main menu -----

   public void openMenu(Player player) {
      List<ActionButton> buttons = new ArrayList<>();
      buttons.add(button("General", NamedTextColor.GOLD, "Disabled worlds, swapping, death drops, update speed", 150,
         this.callback((view, p) -> this.openSection(p, "general", "General", () -> this.openMenu(p)))));
      buttons.add(button("Starting effects", NamedTextColor.GOLD, "Which effects players can start with (selector, random effect on first join)", 150,
         this.callback((view, p) -> this.openSection(p, "starting_effects", "Starting effects", () -> this.openMenu(p)))));
      buttons.add(button("Rituals", NamedTextColor.GOLD, "Settings for every ritual, like how far from 0, 0 effects can be made", 150,
         this.callback((view, p) -> this.openSection(p, "rituals", "Rituals", () -> this.openMenu(p)))));

      for (String effect : Infuse.EFFECT_KEYS) {
         buttons.add(button(title(effect), NamedTextColor.AQUA, "Crafting, recipe, spark, particles and passives of " + title(effect), 150,
            this.callback((view, p) -> this.openEffect(p, effect))));
      }

      buttons.add(button("Reload config.yml", NamedTextColor.YELLOW, "Read config.yml again after editing the file by hand", 150,
         this.callback((view, p) -> {
            this.plugin.reloadConfigFiles();
            p.sendMessage(ConfigMigration.isBroken()
               ? Component.text("config.yml has an error, see the console. The settings in use were kept.", NamedTextColor.RED)
               : Component.text("config.yml reloaded.", NamedTextColor.GREEN));
            this.openMenu(p);
         })));
      List<DialogBody> body = List.of(DialogBody.plainMessage(Component.text("Everything in config.yml can be changed here. Pick a part.")));
      player.showDialog(dialog(heading("Infuse settings"), body, List.of(), buttons, 2));
   }

   // ----- effect menu -----

   public void openEffect(Player player, String effect) {
      List<ActionButton> buttons = new ArrayList<>();

      for (String[] part : EFFECT_PARTS) {
         String path = Settings.effectPath(effect) + "." + part[0];
         buttons.add(button(part[1], NamedTextColor.AQUA, part[2], 150,
            this.callback((view, p) -> this.openSection(p, path, title(effect) + ": " + part[1], () -> this.openEffect(p, effect)))));
      }

      buttons.add(button("Recipe", NamedTextColor.AQUA, "Change the crafting recipe by placing items in a grid", 150,
         this.callback((view, p) -> this.recipeEditor.open(p, effect))));
      buttons.add(button("Back", NamedTextColor.YELLOW, null, 150, this.callback((view, p) -> this.openMenu(p))));
      List<DialogBody> body = List.of(DialogBody.plainMessage(Component.text("What do you want to change for " + title(effect) + "?")));
      player.showDialog(dialog(heading(title(effect) + " settings"), body, List.of(), buttons, 2));
   }

   // ----- generic section page -----

   /** All settings under a section of the template, in file order. The recipe has its own editor. */
   private List<Field> fields(String sectionPath) {
      List<Field> out = new ArrayList<>();
      ConfigurationSection section = this.template().getConfigurationSection(sectionPath);
      if (section != null) {
         for (String rel : section.getKeys(true)) {
            String path = sectionPath + "." + rel;
            if (!section.isConfigurationSection(rel) && !rel.startsWith("recipe.") && !rel.equals("recipe")) {
               out.add(new Field(path, this.label(sectionPath, rel), section.get(rel), this.help(sectionPath, rel)));
            }
         }
      }

      return out;
   }

   private String label(String sectionPath, String rel) {
      String[] parts = rel.split("\\.");
      String name = parts[parts.length - 1];
      String unit = unitFor(name, this.template().get(sectionPath + "." + rel));
      if (parts.length >= 2) {
         String owner = humanize(parts[parts.length - 2]);
         return name.equals("enabled") ? owner + " (on/off)" : owner + ": " + name.replace('_', ' ') + unit;
      } else {
         return humanize(name) + unit;
      }
   }

   private static String unitFor(String name, Object def) {
      if (def instanceof Boolean || def instanceof List) {
         return "";
      } else if (name.contains("chance") || name.contains("percent")) {
         return " (%)";
      } else if (name.endsWith("seconds") || name.equals("minutes")) {
         // The name already says it ("Roll seconds", "Augmented ritual seconds")
         return "";
      } else if (name.contains("seconds") || name.equals("cooldown") || name.equals("duration") || name.equals("swap_cooldown")) {
         return " (seconds)";
      } else if (name.contains("ticks")) {
         return " (20 = 1 second)";
      } else if (name.contains("radius") || name.contains("distance") || name.equals("min_fall") || name.equals("max_blocks")) {
         return " (blocks)";
      } else if (name.contains("multiplier") || name.equals("step") || name.equals("max")) {
         return " (x)";
      } else if (name.contains("crafts")) {
         return " (-1 = unlimited)";
      } else if (name.equals("damage")) {
         return " (2 = 1 heart)";
      } else {
         return "";
      }
   }

   /**
    * The explanation for a setting: its comment in the template. A passive's comment (above the passive) is shown
    * once, with its on/off switch.
    */
   private String help(String sectionPath, String rel) {
      YamlConfiguration t = this.template();
      String path = sectionPath + "." + rel;
      List<String> lines = new ArrayList<>(t.getInlineComments(path));
      if (lines.isEmpty()) {
         lines.addAll(t.getComments(path));
      }

      // A group's comment (above "fall_attack:" or "can_roll:") is shown once, with the group's first setting.
      int dot = path.lastIndexOf('.');
      String parent = dot > 0 ? path.substring(0, dot) : "";
      ConfigurationSection group = parent.equals(sectionPath) ? null : t.getConfigurationSection(parent);
      if (lines.isEmpty() && group != null && !group.getKeys(false).isEmpty() && group.getKeys(false).iterator().next().equals(path.substring(dot + 1))) {
         lines.addAll(t.getComments(parent));
      }

      return clean(lines);
   }

   // Comment lines without blank lines, separator lines and the capital-letter titles of the big sections.
   private static String clean(List<String> lines) {
      List<String> kept = new ArrayList<>();

      for (String line : lines) {
         String l = line == null ? "" : line.trim();
         if (!l.isEmpty() && !l.startsWith("---") && !l.startsWith("===") && !(l.length() <= 20 && l.equals(l.toUpperCase(Locale.ROOT)))) {
            kept.add(l);
         }
      }

      return String.join(" ", kept).trim();
   }

   private String current(Field f) {
      Object value = this.cfg().get(f.path);
      if (value instanceof List<?> list) {
         List<String> parts = new ArrayList<>();

         for (Object o : list) {
            parts.add(String.valueOf(o));
         }

         return String.join(", ", parts);
      } else {
         return value == null ? "" : String.valueOf(value);
      }
   }

   /** The values a page showed when it was opened, by input key: Save only writes what the admin changed. */
   private List<DialogInput> inputs(List<Field> fields, Map<String, Object> shown) {
      List<DialogInput> inputs = new ArrayList<>();

      for (int i = 0; i < fields.size(); i++) {
         Field f = fields.get(i);
         Component label = Component.text(f.label);
         if (f.def instanceof Boolean) {
            boolean value = this.cfg().getBoolean(f.path, (Boolean)f.def);
            shown.put("f" + i, value);
            inputs.add(DialogInput.bool("f" + i, label).initial(value).build());
         } else if (f.isDimensions()) {
            // Read the way the plugin reads it (Nether, the_nether, ... all count).
            Set<Environment> dims = Settings.ritualDimensions(f.path.split("\\.")[1]);

            for (String dim : DIMENSIONS) {
               boolean on = dims.contains(Settings.dimensionFromName(dim));
               shown.put("f" + i + "_" + dim, on);
               inputs.add(DialogInput.bool("f" + i + "_" + dim, Component.text("Can be crafted in the " + title(dim))).initial(on).build());
            }
         } else {
            String value = this.current(f);
            int max = f.def instanceof List ? Math.max(512, value.length() + 256) : 32;
            shown.put("f" + i, value);
            inputs.add(DialogInput.text("f" + i, label).initial(value).maxLength(Math.max(max, value.length())).width(300).build());
         }
      }

      return inputs;
   }

   private List<DialogBody> body(String intro, List<Field> fields) {
      List<DialogBody> body = new ArrayList<>();
      if (intro != null && !intro.isEmpty()) {
         body.add(DialogBody.plainMessage(Component.text(intro), 350));
      }

      List<String> notes = new ArrayList<>();

      for (Field f : fields) {
         if (!f.help.isEmpty()) {
            String name = f.label.replace(" (on/off)", "");
            notes.add("• " + name + ": " + f.help);
         }
      }

      if (!notes.isEmpty()) {
         body.add(DialogBody.plainMessage(Component.text(String.join("\n", notes), NamedTextColor.GRAY), 350));
      }

      return body;
   }

   public void openSection(Player player, String sectionPath, String title, Runnable back) {
      List<Field> fields = this.fields(sectionPath);
      String intro = clean(this.template().getComments(sectionPath));
      for (String[] part : EFFECT_PARTS) {
         if (sectionPath.endsWith("." + part[0])) {
            intro = part[2] + ".";
         }
      }

      Map<String, Object> shown = new HashMap<>();
      List<DialogInput> inputs = this.inputs(fields, shown);
      List<ActionButton> buttons = new ArrayList<>();
      buttons.add(button("Save", NamedTextColor.GREEN, "Write these values to config.yml and apply them now", 150, this.callback((view, p) -> {
         if (ConfigMigration.isBroken()) {
            p.sendMessage(Component.text("config.yml has an error (see the server console). Fix it and use /infuseconfig reload first.", NamedTextColor.RED));
            return;
         }

         List<String> errors = new ArrayList<>();
         int changed = this.save(fields, shown, view, errors);
         if (changed > 0) {
            p.sendMessage(Component.text("Saved " + title + " (" + changed + " changed).", NamedTextColor.GREEN));
         } else if (errors.isEmpty()) {
            p.sendMessage(Component.text("Nothing changed.", NamedTextColor.GRAY));
         }

         for (String error : errors) {
            p.sendMessage(Component.text(error, NamedTextColor.RED));
         }

         this.openSection(p, sectionPath, title, back);
      })));
      buttons.add(button("Back", NamedTextColor.YELLOW, "Back without saving", 150, this.callback((view, p) -> back.run())));
      player.showDialog(dialog(heading(title), this.body(intro, fields), inputs, buttons, 2));
   }

   // ----- saving -----

   // 5 in config.yml and 5.0 typed in the editor are the same setting, so it isn't rewritten or counted as a change.
   private static boolean sameValue(Object a, Object b) {
      if (a instanceof Number x && b instanceof Number y) {
         return Math.abs(x.doubleValue() - y.doubleValue()) < 1.0E-9;
      } else {
         return a == null ? b == null : a.equals(b);
      }
   }

   /** Parses a typed value the way the template's default is typed. Returns null and adds an error if it isn't valid. */
   private static Object parse(Field f, String text, List<String> errors) {
      String t = text == null ? "" : text.trim();
      if (f.def instanceof List) {
         List<String> list = new ArrayList<>();

         for (String part : t.split(",")) {
            if (!part.isBlank()) {
               list.add(part.trim());
            }
         }

         return list;
      } else if (f.def instanceof Number) {
         double min = f.path.endsWith("_crafts") ? -1.0 : 0.0;
         double v;
         try {
            v = Double.parseDouble(t.replace(',', '.'));
         } catch (NumberFormatException ex) {
            errors.add(f.label + ": '" + t + "' is not a number (kept the old value)");
            return null;
         }

         if (Double.isNaN(v) || Double.isInfinite(v) || v < min) {
            errors.add(f.label + ": can't be below " + (min < 0.0 ? "-1" : "0") + " (kept the old value)");
            return null;
         } else if (v == Math.rint(v) && Math.abs(v) < 2.0E9 && !(f.def instanceof Double)) {
            // Whole numbers are saved as whole numbers ("3", not "3.0").
            return (int)v;
         } else {
            return v;
         }
      } else {
         return t;
      }
   }

   private int save(List<Field> fields, Map<String, Object> shown, DialogResponseView view, List<String> errors) {
      FileConfiguration cfg = this.cfg();
      int changed = 0;

      for (int i = 0; i < fields.size(); i++) {
         Field f = fields.get(i);
         Object value;
         if (f.def instanceof Boolean) {
            Boolean b = view.getBoolean("f" + i);
            // Left as it was shown: not written, so a change made meanwhile (another admin, the file) is kept.
            value = b == null || b.equals(shown.get("f" + i)) ? null : b;
         } else if (f.isDimensions()) {
            List<String> dims = new ArrayList<>();
            boolean touched = false;

            for (String dim : DIMENSIONS) {
               boolean on = Boolean.TRUE.equals(view.getBoolean("f" + i + "_" + dim));
               touched |= !Boolean.valueOf(on).equals(shown.get("f" + i + "_" + dim));
               if (on) {
                  dims.add(dim);
               }
            }

            value = touched ? dims : null;
         } else {
            String text = view.getText("f" + i);
            value = text == null || text.trim().equals(String.valueOf(shown.get("f" + i)).trim()) ? null : parse(f, text, errors);
         }

         if (value != null && !sameValue(value, cfg.get(f.path))) {
            cfg.set(f.path, value);
            changed++;
         }
      }

      if (changed > 0) {
         this.plugin.saveConfig();
         // Applies everything (recipes, lore of newly made potions, timers) right away.
         this.plugin.reloadConfigFiles();
      }

      return changed;
   }
}
