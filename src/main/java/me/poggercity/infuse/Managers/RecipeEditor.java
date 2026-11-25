package me.poggercity.infuse.Managers;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * In-game recipe editor (/infuseconfig &lt;effect&gt; recipe, or the Recipe button in the editor). A chest menu with a
 * 3x3 grid: click a grid slot while holding an item to put that item type there (the item in your hand is not used up,
 * the grid only holds copies), click with an empty hand to clear it, then Save. The recipe is written to
 * effects.&lt;effect&gt;.craft.recipe in config.yml and the crafting recipe is replaced right away.
 */
public class RecipeEditor implements Listener {
   private static final int[] GRID = new int[]{10, 11, 12, 19, 20, 21, 28, 29, 30};
   private static final int RESULT = 24;
   private static final int HELP = 4;
   private static final int SAVE = 48;
   private static final int RESET = 49;
   private static final int BACK = 50;
   private final Infuse plugin;
   private ConfigDialog dialog;

   /** Marks the menu, so clicks in it are recognized (and never taken as normal inventory clicks). */
   public static final class Holder implements InventoryHolder {
      final String effect;
      Inventory inventory;

      Holder(String effect) {
         this.effect = effect;
      }

      @Override
      public Inventory getInventory() {
         return this.inventory;
      }
   }

   public RecipeEditor(Infuse plugin) {
      this.plugin = plugin;
   }

   public void setDialog(ConfigDialog dialog) {
      this.dialog = dialog;
   }

   private FileConfiguration cfg() {
      return this.plugin.getConfig();
   }

   private static String title(String effect) {
      return EffectKeys.title(effect);
   }

   private static ItemStack named(Material type, Component name, Component... lore) {
      ItemStack item = new ItemStack(type);
      ItemMeta meta = item.getItemMeta();
      meta.displayName(name.decoration(TextDecoration.ITALIC, false));
      List<Component> lines = new ArrayList<>();

      for (Component line : lore) {
         lines.add(line.decoration(TextDecoration.ITALIC, false));
      }

      meta.lore(lines);
      item.setItemMeta(meta);
      return item;
   }

   /** The recipe as 9 item types (null = empty slot), read from a recipe section with shape and ingredients. */
   static Material[] readGrid(ConfigurationSection recipe) {
      Material[] grid = new Material[9];
      if (recipe != null) {
         List<String> shape = recipe.getStringList("shape");
         ConfigurationSection ings = recipe.getConfigurationSection("ingredients");

         for (int row = 0; row < 3 && row < shape.size(); row++) {
            String line = shape.get(row);

            for (int col = 0; col < 3 && col < line.length(); col++) {
               char c = line.charAt(col);
               if (c != ' ' && ings != null) {
                  grid[row * 3 + col] = Material.matchMaterial(String.valueOf(ings.getString(String.valueOf(c), "")));
               }
            }
         }
      }

      return grid;
   }

   public void open(Player player, String effect) {
      if (!player.hasPermission(ConfigDialog.PERMISSION)) {
         player.sendMessage(Component.text("You don't have permission to change Infuse settings.", NamedTextColor.RED));
         return;
      }

      Holder holder = new Holder(effect);
      Inventory inv = Bukkit.createInventory(holder, 54, Component.text("Recipe: " + title(effect)));
      holder.inventory = inv;
      ItemStack filler = named(Material.GRAY_STAINED_GLASS_PANE, Component.text(" "));

      for (int i = 0; i < inv.getSize(); i++) {
         inv.setItem(i, filler);
      }

      this.showGrid(inv, readGrid(this.recipeSection(effect)));
      inv.setItem(
         HELP,
         named(
            Material.OAK_SIGN,
            Component.text("How to change the recipe", NamedTextColor.GOLD),
            Component.text("Click a grid slot while holding an item", NamedTextColor.GRAY),
            Component.text("to put that item there. Your item is not used up.", NamedTextColor.GRAY),
            Component.text("Click with an empty hand to clear a slot.", NamedTextColor.GRAY),
            Component.text("Only the item type counts (not names,", NamedTextColor.GRAY),
            Component.text("enchantments or amounts).", NamedTextColor.GRAY)
         )
      );
      inv.setItem(23, named(Material.ARROW, Component.text("makes", NamedTextColor.GRAY)));
      ItemStack result = this.plugin.getRecipeManager() == null ? null : this.plugin.getRecipeManager().getEffectPreview(effect);
      inv.setItem(RESULT, result != null ? result : named(Material.POTION, Component.text(title(effect) + " Effect")));
      inv.setItem(SAVE, named(Material.LIME_CONCRETE, Component.text("Save recipe", NamedTextColor.GREEN), Component.text("Writes it to config.yml and uses it right away", NamedTextColor.GRAY)));
      inv.setItem(RESET, named(Material.YELLOW_CONCRETE, Component.text("Default recipe", NamedTextColor.YELLOW), Component.text("Puts the original recipe in the grid (press Save to keep it)", NamedTextColor.GRAY)));
      inv.setItem(BACK, named(Material.RED_CONCRETE, Component.text("Back", NamedTextColor.RED), Component.text("Back to the settings without saving", NamedTextColor.GRAY)));
      player.openInventory(inv);
   }

   private void showGrid(Inventory inv, Material[] grid) {
      for (int i = 0; i < 9; i++) {
         inv.setItem(GRID[i], grid[i] == null || !grid[i].isItem() ? null : new ItemStack(grid[i]));
      }
   }

   private static int gridIndex(int slot) {
      for (int i = 0; i < GRID.length; i++) {
         if (GRID[i] == slot) {
            return i;
         }
      }

      return -1;
   }

   @EventHandler(priority = EventPriority.LOWEST)
   public void onClick(InventoryClickEvent event) {
      if (event.getView().getTopInventory().getHolder() instanceof Holder holder) {
         HumanEntity who = event.getWhoClicked();
         boolean top = event.getRawSlot() >= 0 && event.getRawSlot() < event.getView().getTopInventory().getSize();
         if (!top) {
            // The player's own inventory works normally, except moves that could pull items out of the grid or push
            // items into it (shift-click, double-click collect).
            if (event.getClick() != ClickType.LEFT && event.getClick() != ClickType.RIGHT) {
               event.setCancelled(true);
            }

            return;
         }

         event.setCancelled(true);
         if (!(who instanceof Player player)
            || !player.hasPermission(ConfigDialog.PERMISSION)
            || event.getClick() != ClickType.LEFT && event.getClick() != ClickType.RIGHT) {
            return;
         }

         Inventory inv = event.getView().getTopInventory();
         int slot = event.getRawSlot();
         int index = gridIndex(slot);
         if (index >= 0) {
            ItemStack cursor = event.getCursor();
            inv.setItem(slot, cursor == null || cursor.getType().isAir() ? null : new ItemStack(cursor.getType()));
         } else if (slot == SAVE) {
            this.save(player, holder.effect, inv);
         } else if (slot == RESET) {
            this.showGrid(inv, readGrid(this.defaultRecipe(holder.effect)));
         } else if (slot == BACK) {
            player.closeInventory();
            if (this.dialog != null) {
               this.dialog.openEffect(player, holder.effect);
            }
         }
      }
   }

   @EventHandler(priority = EventPriority.LOWEST)
   public void onDrag(InventoryDragEvent event) {
      if (event.getView().getTopInventory().getHolder() instanceof Holder) {
         int size = event.getView().getTopInventory().getSize();

         for (int raw : event.getRawSlots()) {
            if (raw < size) {
               event.setCancelled(true);
               return;
            }
         }
      }
   }

   /** The recipe in config.yml, or the jar's default recipe if config.yml has none (taken as a whole, never mixed). */
   private ConfigurationSection recipeSection(String effect) {
      String path = Settings.recipePath(effect);
      return this.cfg().contains(path, true) ? this.cfg().getConfigurationSection(path) : this.defaultRecipe(effect);
   }

   private ConfigurationSection defaultRecipe(String effect) {
      InputStream in = this.plugin.getResource("config.yml");
      if (in == null) {
         return null;
      } else {
         return YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8)).getConfigurationSection(Settings.recipePath(effect));
      }
   }

   /** The grid flipped left to right (vanilla also accepts a shaped recipe mirrored). */
   static Material[] mirror(Material[] grid) {
      Material[] out = new Material[9];

      for (int i = 0; i < 9; i++) {
         out[i] = grid[(i / 3) * 3 + (2 - i % 3)];
      }

      return out;
   }

   /** The grid with empty rows and columns cut off, as vanilla matches shaped recipes (anywhere in the grid). */
   static String normalized(Material[] grid) {
      int minRow = 3;
      int maxRow = -1;
      int minCol = 3;
      int maxCol = -1;

      for (int i = 0; i < 9; i++) {
         if (grid[i] != null) {
            minRow = Math.min(minRow, i / 3);
            maxRow = Math.max(maxRow, i / 3);
            minCol = Math.min(minCol, i % 3);
            maxCol = Math.max(maxCol, i % 3);
         }
      }

      StringBuilder sb = new StringBuilder();

      for (int r = minRow; r <= maxRow; r++) {
         for (int c = minCol; c <= maxCol; c++) {
            Material m = grid[r * 3 + c];
            sb.append(m == null ? "-" : m.name()).append(',');
         }

         sb.append('/');
      }

      return sb.toString();
   }

   private void save(Player player, String effect, Inventory inv) {
      Material[] grid = new Material[9];
      boolean any = false;

      for (int i = 0; i < 9; i++) {
         ItemStack item = inv.getItem(GRID[i]);
         if (item != null && !item.getType().isAir()) {
            grid[i] = item.getType();
            any = true;
         }
      }

      if (!any) {
         player.sendMessage(Component.text("The recipe is empty. Put at least one item in the grid.", NamedTextColor.RED));
         return;
      }

      if (ConfigMigration.isBroken()) {
         player.sendMessage(Component.text("config.yml has an error (see the server console). Fix it and use /infuseconfig reload first.", NamedTextColor.RED));
         return;
      }

      String mine = normalized(grid);
      String mirrored = normalized(mirror(grid));

      for (String other : Infuse.EFFECT_KEYS) {
         String theirs = normalized(readGrid(this.recipeSection(other)));
         if (!other.equals(effect) && (mine.equals(theirs) || mirrored.equals(theirs))) {
            player.sendMessage(Component.text(title(other) + " already uses this recipe. Change it a little first.", NamedTextColor.RED));
            return;
         }
      }

      // The game's own matcher (it also matches mirrored and smaller layouts): refuse a grid that already crafts
      // something else, a vanilla item or another effect.
      ItemStack[] matrix = new ItemStack[9];

      for (int i = 0; i < 9; i++) {
         matrix[i] = grid[i] == null ? null : new ItemStack(grid[i]);
      }

      org.bukkit.inventory.Recipe existing = Bukkit.getCraftingRecipe(matrix, player.getWorld());
      if (existing instanceof org.bukkit.Keyed keyed) {
         org.bukkit.NamespacedKey key = keyed.getKey();
         boolean mineToo = key.getNamespace().equals(this.plugin.getName().toLowerCase(Locale.ROOT)) && key.getKey().equals(effect);
         if (!mineToo) {
            String what = existing.getResult().getType().name().toLowerCase(Locale.ROOT).replace('_', ' ');
            player.sendMessage(Component.text("These items already craft " + what + " (" + key + "). Use a different recipe.", NamedTextColor.RED));
            return;
         }
      }

      // Letters: the one the default recipe uses for that item, else the item's first letter, else any free letter.
      // So Default + Save gives back exactly the original recipe, and "D: DIRT" stays easy to read in config.yml.
      Map<Material, Character> preferred = new LinkedHashMap<>();
      ConfigurationSection def = this.defaultRecipe(effect);
      ConfigurationSection defIngs = def == null ? null : def.getConfigurationSection("ingredients");
      if (defIngs != null) {
         for (String symbol : defIngs.getKeys(false)) {
            Material m = Material.matchMaterial(String.valueOf(defIngs.getString(symbol, "")));
            if (m != null && symbol.length() == 1) {
               preferred.put(m, symbol.charAt(0));
            }
         }
      }

      Map<Material, Character> letters = new LinkedHashMap<>();

      for (Material m : grid) {
         if (m != null && !letters.containsKey(m)) {
            Character pick = preferred.get(m);
            if (pick == null || letters.containsValue(pick)) {
               pick = Character.toUpperCase(m.name().charAt(0));
            }

            if (letters.containsValue(pick)) {
               for (char c = 'A'; c <= 'Z'; c++) {
                  if (!letters.containsValue(c)) {
                     pick = c;
                     break;
                  }
               }
            }

            letters.put(m, pick);
         }
      }

      List<String> shape = new ArrayList<>();

      for (int row = 0; row < 3; row++) {
         StringBuilder line = new StringBuilder();

         for (int col = 0; col < 3; col++) {
            Material m = grid[row * 3 + col];
            line.append(m == null ? ' ' : letters.get(m));
         }

         shape.add(line.toString());
      }

      String path = Settings.recipePath(effect);
      FileConfiguration cfg = this.cfg();
      cfg.set(path, null);
      cfg.set(path + ".shape", shape);

      for (Map.Entry<Material, Character> e : letters.entrySet()) {
         cfg.set(path + ".ingredients." + e.getValue(), e.getKey().name());
      }

      this.plugin.saveConfig();
      this.plugin.reloadConfigFiles();
      List<String> names = new ArrayList<>();

      for (Material m : letters.keySet()) {
         names.add(m.name().toLowerCase(Locale.ROOT));
      }

      player.sendMessage(Component.text("Saved the " + title(effect) + " recipe: " + String.join(", ", names), NamedTextColor.GREEN));
   }

   /** Closes every open recipe menu (on plugin disable, so the grid's copies can never be taken out). */
   public static void closeAll() {
      for (Player p : Bukkit.getOnlinePlayers()) {
         if (p.getOpenInventory().getTopInventory().getHolder() instanceof Holder) {
            p.closeInventory();
         }
      }
   }
}
