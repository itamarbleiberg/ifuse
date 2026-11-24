package me.poggercity.infuse.Managers;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.logging.Logger;
import me.poggercity.infuse.Infuses.Augmented;
import me.poggercity.infuse.Infuses.Effects;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.CrafterCraftEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.RecipeChoice.ExactChoice;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionType;
import org.bukkit.util.RayTraceResult;
import org.jetbrains.annotations.NotNull;

public class InfuseRecipeManager implements Listener {
   private final JavaPlugin plugin;
   private final File recipeLimitFile;
   private final FileConfiguration recipeLimits;
   private final Map<String, ItemStack> firstTimeRewards;
   private final Map<String, ItemStack> standardResults;
   private final Set<Location> protectedBrewingStands = Collections.synchronizedSet(new HashSet<>());
   private final Map<String, Boolean> activeRituals = Collections.synchronizedMap(new HashMap<>());
   private final Set<NamespacedKey> registeredRecipes = Collections.synchronizedSet(new HashSet<>());

   public InfuseRecipeManager(JavaPlugin plugin) {
      this.plugin = plugin;
      this.recipeLimitFile = new File(plugin.getDataFolder(), "recipe_limits.yml");
      this.recipeLimits = YamlConfiguration.loadConfiguration(this.recipeLimitFile);
      this.firstTimeRewards = new HashMap<>();
      this.standardResults = new HashMap<>();

      try {
         this.loadRecipesFromConfig();
         plugin.getServer().getPluginManager().registerEvents(this, plugin);
         plugin.getLogger().info("InfuseRecipeManager initialized successfully");
      } catch (Exception var3) {
         plugin.getLogger().severe("Failed to initialize InfuseRecipeManager: " + var3.getMessage());
         var3.printStackTrace();
      }
   }

   public Map<String, ItemStack> getCraftedFirstTimeEffects() {
      Map<String, ItemStack> craftedEffects = new HashMap<>();

      for (Entry<String, ItemStack> entry : this.firstTimeRewards.entrySet()) {
         if (this.recipeLimits.getBoolean("first_time." + entry.getKey(), false)) {
            craftedEffects.put(entry.getKey(), entry.getValue());
         }
      }

      return craftedEffects;
   }

   private void saveRecipeLimits() {
      try {
         this.recipeLimits.save(this.recipeLimitFile);
      } catch (IOException var2) {
         var2.printStackTrace();
      }
   }

   private String fixKey(String recipeKey) {
      return recipeKey;
   }

   @EventHandler
   public void onCrafterCraft(CrafterCraftEvent event) {
      ItemStack result = event.getResult();
      if (result != null && result.getType() == Material.POTION) {
         event.setCancelled(true);
      }
   }

   private void spawnCustomBeam(Location brewingStandLocation) {
      if (this.plugin.getConfig().getBoolean("configs.brewing_beam", true)) {
         World world = brewingStandLocation.getWorld();
         Location crystalLoc = new Location(world, brewingStandLocation.getX(), 600.0, brewingStandLocation.getZ());
         Bukkit.getRegionScheduler().execute(this.plugin, brewingStandLocation, () -> {
            EnderCrystal crystal = (EnderCrystal)world.spawnEntity(crystalLoc, EntityType.END_CRYSTAL);
            crystal.setShowingBottom(false);
            crystal.setInvulnerable(true);
            crystal.setInvisible(true);
            Location targetLoc = brewingStandLocation.clone().subtract(0.0, 2.0, 0.0);
            ArmorStand marker = (ArmorStand)world.spawnEntity(targetLoc, EntityType.ARMOR_STAND);
            marker.setMarker(true);
            marker.setInvisible(true);
            marker.setInvulnerable(true);
            marker.setSilent(true);
            marker.setCustomNameVisible(false);
            crystal.setBeamTarget(marker.getLocation().toBlockLocation());
            int ritualDurationSeconds = this.plugin.getConfig().getInt("durations.ritual_duration", 600);
            Bukkit.getGlobalRegionScheduler().runDelayed(this.plugin, task -> Bukkit.getRegionScheduler().execute(this.plugin, brewingStandLocation, () -> {
               if (!crystal.isDead()) {
                  crystal.remove();
               }

               if (!marker.isDead()) {
                  marker.remove();
               }
            }), ritualDurationSeconds * 20L);
         });
      }
   }

   private void startRitual(Player player, String recipeKey, Location brewingStandLocation, ItemStack craftedItem) {
      if (brewingStandLocation == null) {
         player.sendMessage(ChatColor.RED + "You need to craft this in a brewing stand!");
      } else {
         this.protectedBrewingStands.add(brewingStandLocation.getBlock().getLocation());
         this.activeRituals.put("Augmented", true);
         ItemMeta meta = craftedItem.getItemMeta();
         String displayName = meta != null && meta.hasDisplayName() ? meta.getDisplayName() : ChatColor.WHITE + "Augmented Effect";
         String itemName = displayName.replace("Effect", "");
         BarColor barColor = this.getColorFromItemName(itemName);
         String colorOnly = ChatColor.getLastColors(itemName);
         String formattedItemName = ChatColor.BOLD.toString() + colorOnly + "\ud83e\uddea " + ChatColor.stripColor(itemName) + " \ud83e\uddea";
         BossBar bossBar = Bukkit.createBossBar(formattedItemName, barColor, BarStyle.SOLID, new BarFlag[0]);

         for (Player p : Bukkit.getOnlinePlayers()) {
            bossBar.addPlayer(p);
         }

         Environment env = brewingStandLocation.getWorld().getEnvironment();

         Bukkit.broadcastMessage(
            colorOnly
               + "\ud83e\uddea "
               + player.getName()
               + " is cooking up the "
               + itemName
               + " at "
               + ChatColor.BOLD
               + ChatColor.AQUA
               + brewingStandLocation.getBlockX()
               + ", "
               + brewingStandLocation.getBlockY()
               + ", "
               + brewingStandLocation.getBlockZ()
               + "... "
               + switch (env) {
                  case NORMAL -> ChatColor.GREEN + "Overworld";
                  case NETHER -> ChatColor.DARK_RED + "Nether";
                  case THE_END -> ChatColor.DARK_PURPLE + "End";
                  default -> ChatColor.GRAY + brewingStandLocation.getWorld().getName();
               }
         );
         Location playerLocation = player.getLocation();

         for (Player p : player.getWorld().getPlayers()) {
            Bukkit.getRegionScheduler().execute(this.plugin, p.getLocation(), () -> p.playSound(playerLocation, Sound.BLOCK_END_PORTAL_SPAWN, 1.0F, 1.0F));
         }

         this.spawnCustomBeam(brewingStandLocation);
         int ritualDurationSeconds = this.plugin.getConfig().getInt("durations.ritual_duration", 600);
         int totalTicks = ritualDurationSeconds * 20;
         int[] ticksElapsed = new int[]{0};
         Bukkit.getGlobalRegionScheduler()
            .runAtFixedRate(
               this.plugin,
               task -> {
                  ticksElapsed[0]++;
                  double rawProgress = (double)ticksElapsed[0] / totalTicks;
                  double clampedProgress = Math.min(1.0, Math.max(0.0, rawProgress));
                  bossBar.setProgress(clampedProgress);
                  if (clampedProgress >= 1.0) {
                     bossBar.removeAll();
                     Bukkit.broadcastMessage(ChatColor.WHITE + ChatColor.stripColor(itemName) + " has been brewed!");

                     for (Player p : player.getWorld().getPlayers()) {
                        Bukkit.getRegionScheduler()
                           .execute(this.plugin, p.getLocation(), () -> p.playSound(playerLocation, Sound.BLOCK_END_PORTAL_SPAWN, 1.0F, 1.0F));
                     }

                     Bukkit.getRegionScheduler().execute(this.plugin, brewingStandLocation, () -> {
                        Item dropped = brewingStandLocation.getWorld().dropItemNaturally(brewingStandLocation, craftedItem);
                        Material type = craftedItem.getType();
                        if (type == Material.POTION || type == Material.SPLASH_POTION || type == Material.LINGERING_POTION) {
                           dropped.setInvulnerable(true);
                        }
                     });

                     for (Player p : player.getWorld().getPlayers()) {
                        Bukkit.getRegionScheduler()
                           .execute(this.plugin, p.getLocation(), () -> p.playSound(playerLocation, Sound.BLOCK_BREWING_STAND_BREW, 1.0F, 1.2F));
                     }

                     this.protectedBrewingStands.remove(brewingStandLocation.getBlock().getLocation());
                     this.activeRituals.remove("Augmented");
                     task.cancel();
                  }
               },
               1L,
               1L
            );
      }
   }

   @EventHandler
   public void onBlockBreak(BlockBreakEvent event) {
      if (this.protectedBrewingStands.contains(event.getBlock().getLocation())) {
         event.setCancelled(true);
         event.getPlayer().sendMessage(ChatColor.RED + "You can't break a brewing stand while it's cooking up something!");
      }
   }

   private Location findNearestBrewingStand(Player player) {
      RayTraceResult result = player.rayTraceBlocks(5.0);
      if (result != null && result.getHitBlock() != null && result.getHitBlock().getType() == Material.BREWING_STAND) {
         Block block = result.getHitBlock();
         return block.getLocation().add(0.5, 0.5, 0.5);
      } else {
         return null;
      }
   }

   private BarColor getColorFromItemName(String itemName) {
      if (itemName != null && !itemName.isEmpty()) {
         String strippedName = ChatColor.stripColor(itemName).toLowerCase(Locale.ROOT);
         if (strippedName.contains("augmented strength")) {
            return BarColor.RED;
         }

         if (strippedName.contains("augmented frost")) {
            return BarColor.BLUE;
         }

         if (strippedName.contains("augmented thunder")) {
            return BarColor.YELLOW;
         }

         if (strippedName.contains("augmented haste")) {
            return BarColor.YELLOW;
         }

         if (strippedName.contains("augmented invis")) {
            return BarColor.PURPLE;
         }

         if (strippedName.contains("augmented heart")) {
            return BarColor.RED;
         }

         if (strippedName.contains("augmented emerald")) {
            return BarColor.GREEN;
         }

         if (strippedName.contains("augmented ocean")) {
            return BarColor.BLUE;
         }

         if (strippedName.contains("augmented fire")) {
            return BarColor.YELLOW;
         }

         if (strippedName.contains("augmented speed")) {
            return BarColor.YELLOW;
         }

         if (strippedName.contains("augmented regen")) {
            return BarColor.PINK;
         }

         if (strippedName.contains("augmented apophis")) {
            return BarColor.PURPLE;
         }

         if (strippedName.contains("augmented feather")) {
            return BarColor.WHITE;
         }
      }

      return BarColor.WHITE;
   }

   @EventHandler
   public void onCraft(CraftItemEvent event) {
      if (event.getRecipe() instanceof ShapedRecipe) {
         ShapedRecipe shaped = (ShapedRecipe)event.getRecipe();
         String rawKey = shaped.getKey().getKey();
         String recipeKey = this.fixKey(rawKey);
         if (this.firstTimeRewards.containsKey(recipeKey)) {
            Inventory topInv = event.getView().getTopInventory();
            if (topInv.getType() != InventoryType.WORKBENCH) {
               event.setCancelled(true);
               ((Player)event.getWhoClicked()).sendMessage(ChatColor.RED + "You must use a crafting table to craft this item!");
               return;
            }

            if (event.isShiftClick()) {
               event.setCancelled(true);
               ((Player)event.getWhoClicked()).sendMessage(ChatColor.RED + "Don't shift click");
               return;
            }

            Player player = (Player)event.getWhoClicked();
            Location playerLocation = player.getLocation();
            World world = playerLocation.getWorld();
            boolean brewingStandNearby = false;
            int x = -5;

            label127:
            while (true) {
               if (x <= 5 && !brewingStandNearby) {
                  int y = -5;

                  label122:
                  while (true) {
                     if (y <= 5 && !brewingStandNearby) {
                        int z = -5;

                        while (true) {
                           if (z <= 5) {
                              Location check = playerLocation.clone().add(x, y, z);
                              if (world.getBlockAt(check).getType() != Material.BREWING_STAND) {
                                 z++;
                                 continue;
                              }

                              brewingStandNearby = true;
                           }

                           y++;
                           continue label122;
                        }
                     }

                     x++;
                     continue label127;
                  }
               }

               if (!brewingStandNearby) {
                  event.setCancelled(true);
                  return;
               }

               String firstTimeKey = "first_time." + recipeKey;
               String limitKey = "craft_limit." + recipeKey;
               int limit = this.plugin.getConfig().getInt("effects." + recipeKey, 3);
               int remaining = this.recipeLimits.getInt(limitKey, limit);
               int maxAugments = this.plugin.getConfig().getInt("augments." + recipeKey, 1);
               int usedAugments = this.recipeLimits.getInt(firstTimeKey, 0);
               if (maxAugments != -1 && usedAugments >= maxAugments) {
                  if (limit != -1 && remaining <= 0) {
                     event.setCancelled(true);
                     player.sendMessage(ChatColor.RED + "You have reached the crafting limit for this item!");
                     return;
                  }

                  if (limit != -1) {
                     this.recipeLimits.set(limitKey, remaining - 1);
                     this.saveRecipeLimits();
                  }
                  String var26 = switch (player.getWorld().getEnvironment()) {
                     case NORMAL -> ChatColor.GREEN + "Overworld";
                     case NETHER -> ChatColor.DARK_RED + "Nether";
                     case THE_END -> ChatColor.DARK_PURPLE + "End";
                     default -> ChatColor.GRAY + "Unknown Dimension";
                  };
                  ItemStack result = event.getRecipe().getResult();
                  String disp = result.hasItemMeta() && result.getItemMeta().hasDisplayName()
                     ? result.getItemMeta().getDisplayName()
                     : ChatColor.WHITE + "Effect";
                  String itemName = ChatColor.stripColor(disp).replace("Effect", "");
                  String color = ChatColor.getLastColors(disp);
                  Bukkit.broadcastMessage(
                     color
                        + "\ud83e\uddea "
                        + itemName
                        + " has been crafted at "
                        + ChatColor.BOLD
                        + ChatColor.AQUA
                        + (int)player.getX()
                        + ", "
                        + (int)player.getY()
                        + ", "
                        + (int)player.getZ()
                        + "... "
                        + var26
                  );
                  return;
               }

               CraftingInventory inv = event.getInventory();
               ItemStack[] matrix = inv.getMatrix();
               if (matrix == null) {
                  event.setCancelled(true);
                  return;
               }

               Block targetBlock = player.getTargetBlockExact(4);
               if (targetBlock == null || targetBlock.getType() != Material.BREWING_STAND) {
                  player.sendMessage(ChatColor.RED + "You must use a brewing stand to start the ritual.");
                  return;
               }

               Location blockLoc = targetBlock.getLocation();
               if (this.protectedBrewingStands.contains(blockLoc)) {
                  player.sendMessage(ChatColor.RED + "There is an ongoing ritual here.");
                  event.setCancelled(true);
                  return;
               }

               if (Boolean.TRUE.equals(this.activeRituals.get("Augmented"))) {
                  player.sendMessage(ChatColor.RED + "There is already an ongoing ritual.");
                  event.setCancelled(true);
                  return;
               }

               for (int i = 0; i < matrix.length; i++) {
                  if (matrix[i] != null && matrix[i].getType() != Material.AIR) {
                     int newAmt = matrix[i].getAmount() - 1;
                     matrix[i] = newAmt > 0 ? new ItemStack(matrix[i].getType(), newAmt) : null;
                  }
               }

               inv.setMatrix(matrix);
               player.updateInventory();
               player.closeInventory();
               this.recipeLimits.set(firstTimeKey, usedAugments + 1);
               this.saveRecipeLimits();
               this.startRitual(player, recipeKey, blockLoc, this.firstTimeRewards.get(recipeKey));
               event.setCancelled(true);
               break;
            }
         }
      }
   }

   public int getCraftLimit(String recipeKey) {
      return this.plugin.getConfig().getInt("effects." + recipeKey, 3);
   }

   public Map<String, ItemStack> getFirstTimeRewards() {
      return this.firstTimeRewards;
   }

   public int getCraftsLeft(String recipeKey) {
      int defaultLimit = this.getCraftLimit(recipeKey);
      return this.recipeLimits.getInt("craft_limit." + recipeKey, defaultLimit);
   }

   public int getMaxAugments(String recipeKey) {
      return this.plugin.getConfig().getInt("augments." + recipeKey, 1);
   }

   public int getUsedAugments(String recipeKey) {
      return this.recipeLimits.getInt("first_time." + recipeKey, 0);
   }

   @EventHandler
   public void onPrepareCraft(PrepareItemCraftEvent event) {
      if (event.getRecipe() instanceof ShapedRecipe shaped) {
         String rawKey = shaped.getKey().getKey();
         String recipeKey = this.fixKey(rawKey);
         if (this.firstTimeRewards.containsKey(recipeKey)) {
            if (event.getView().getTopInventory().getType() != InventoryType.WORKBENCH) {
               event.getInventory().setResult(null);
               return;
            }

            Player player = (Player)event.getView().getPlayer();
            Location loc = player.getLocation();
            boolean hasNearbyStand = false;

            for (int x = -5; x <= 5 && !hasNearbyStand; x++) {
               for (int y = -5; y <= 5 && !hasNearbyStand; y++) {
                  for (int z = -5; z <= 5; z++) {
                     if (player.getWorld().getBlockAt(loc.clone().add(x, y, z)).getType() == Material.BREWING_STAND) {
                        hasNearbyStand = true;
                        break;
                     }
                  }
               }
            }

            if (!hasNearbyStand) {
               event.getInventory().setResult(null);
            } else {
               int maxAug = this.plugin.getConfig().getInt("augments." + recipeKey, 1);
               int usedAug = this.recipeLimits.getInt("first_time." + recipeKey, 0);
               int limit = this.plugin.getConfig().getInt("effects." + recipeKey, 3);
               int remaining = this.recipeLimits.getInt("craft_limit." + recipeKey, limit);
               if (maxAug == -1 || usedAug < maxAug) {
                  event.getInventory().setResult(this.firstTimeRewards.get(recipeKey));
               } else if (limit != -1 && remaining <= 0) {
                  event.getInventory().setResult(null);
               } else {
                  event.getInventory().setResult(this.standardResults.get(recipeKey));
               }
            }
         }
      }
   }

   @EventHandler(
      priority = EventPriority.HIGHEST
   )
   public void onBrewingStandInteract(PlayerInteractEvent event) {
      if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null && event.getClickedBlock().getType() == Material.BREWING_STAND) {
         event.setCancelled(true);
         Player player = event.getPlayer();
         Bukkit.getScheduler().runTask(this.plugin, () -> player.openWorkbench(null, true));
      }
   }

   private void loadRecipesFromConfig() {
      this.clearAllRecipes();
      this.firstTimeRewards.clear();
      this.standardResults.clear();
      ConfigurationSection recipes = this.plugin.getConfig().getConfigurationSection("recipes");
      if (recipes != null) {
         for (String key : recipes.getKeys(false)) {
            ConfigurationSection sec = recipes.getConfigurationSection(key);
            if (sec != null) {
               this.loadRecipe(key, sec);
            }
         }
      }

      ConfigurationSection apophisSection = this.plugin.getConfig().getConfigurationSection("apophis");
      if (apophisSection != null && !this.firstTimeRewards.containsKey("apophis")) {
         this.loadRecipe("apophis", apophisSection);
      }

      this.addMissingHardcodedRecipes();
   }

   private void clearAllRecipes() {
      for (NamespacedKey key : this.registeredRecipes) {
         try {
            Bukkit.removeRecipe(key);
            this.plugin.getLogger().info("Removed recipe: " + key.getKey());
         } catch (Exception var5) {
            Logger log = this.plugin.getLogger();
            log.warning("Failed to remove recipe " + key.getKey() + ": " + var5.getMessage());
         }
      }

      this.registeredRecipes.clear();
   }

   @NotNull
   private static ItemStack buildHealingPotionStack() {
      ItemStack healing = new ItemStack(Material.POTION);
      PotionMeta pm = (PotionMeta)healing.getItemMeta();
      pm.setBasePotionType(PotionType.HEALING);
      healing.setItemMeta(pm);
      return healing;
   }

   private void loadRecipe(String key, ConfigurationSection sec) {
      try {
         String recipeName = sec.getString("key", key);
         ItemStack firstTime = this.getAugmentedItem(key);
         ItemStack standard = this.getEffectItem(key);
         if (firstTime == null || standard == null) {
            this.plugin.getLogger().warning("No items found for recipe '" + key + "', skipping config recipe");
            return;
         }

         this.firstTimeRewards.put(key, firstTime);
         this.standardResults.put(key, standard);
         NamespacedKey nk = new NamespacedKey(this.plugin, recipeName);
         if (this.registeredRecipes.contains(nk)) {
            Bukkit.removeRecipe(nk);
            this.registeredRecipes.remove(nk);
         }

         ShapedRecipe recipe = new ShapedRecipe(nk, standard);
         List<String> shape = sec.getStringList("shape");
         if (shape.size() != 3) {
            this.plugin.getLogger().warning("Recipe '" + key + "' has invalid shape (need exactly 3 rows): " + shape);
            return;
         }

         recipe.shape(new String[]{shape.get(0), shape.get(1), shape.get(2)});
         ConfigurationSection ings = sec.getConfigurationSection("ingredients");
         if (ings != null) {
            boolean ingredientsValid = true;

            for (String ch : ings.getKeys(false)) {
               if (ch.length() == 1) {
                  char c = ch.charAt(0);
                  String materialName = ings.getString(ch, "").toUpperCase(Locale.ROOT);
                  if (key.equalsIgnoreCase("heart") && materialName.equals("POTION")) {
                     ItemStack healingPotion = buildHealingPotionStack();
                     RecipeChoice potionChoice = new ExactChoice(healingPotion);
                     recipe.setIngredient(c, potionChoice);
                  } else {
                     Material mat = Material.getMaterial(materialName);
                     if (mat == null) {
                        this.plugin.getLogger().warning("Unknown material '" + materialName + "' for symbol '" + c + "' in recipe '" + key + "'");
                        ingredientsValid = false;
                        break;
                     }

                     recipe.setIngredient(c, mat);
                  }
               }
            }

            if (!ingredientsValid) {
               this.plugin.getLogger().warning("Recipe '" + key + "' has invalid ingredients, skipping");
               this.firstTimeRewards.remove(key);
               this.standardResults.remove(key);
               return;
            }
         }

         try {
            Bukkit.addRecipe(recipe);
            this.registeredRecipes.add(nk);
            this.plugin.getLogger().info("Loaded recipe from config: " + key + " (NamespacedKey: " + nk.getKey() + ")");
         } catch (Exception var17) {
            this.plugin.getLogger().warning("Failed to add recipe '" + key + "': " + var17.getMessage());
            this.firstTimeRewards.remove(key);
            this.standardResults.remove(key);
         }
      } catch (Exception var18) {
         this.plugin.getLogger().severe("Error loading recipe '" + key + "': " + var18.getMessage());
         var18.printStackTrace();
      }
   }

   private void addMissingHardcodedRecipes() {
      String[] expectedRecipes = new String[]{
         "emerald", "feather", "fire", "frost", "haste", "heart", "invis", "ocean", "regen", "speed", "strength", "thunder", "ender", "apophis"
      };

      for (String key : expectedRecipes) {
         if (!this.firstTimeRewards.containsKey(key)) {
            this.plugin.getLogger().info("Adding hardcoded fallback recipe for: " + key);
            this.addHardcodedRecipe(key);
         }
      }
   }

   private void addHardcodedRecipe(String key) {
      String k = key.toLowerCase(Locale.ROOT);
      switch (k) {
         case "ender":
            this.addEnderRecipe();
            break;
         case "apophis":
            this.Apophis();
            break;
         case "emerald":
            this.Emerald();
            break;
         case "feather":
            this.Feather();
            break;
         case "fire":
            this.Fire();
            break;
         case "frost":
            this.Frost();
            break;
         case "haste":
            this.Haste();
            break;
         case "heart":
            this.Heart();
            break;
         case "invis":
            this.Invis();
            break;
         case "ocean":
            this.Ocean();
            break;
         case "regen":
            this.Regen();
            break;
         case "speed":
            this.Speed();
            break;
         case "strength":
            this.Strength();
            break;
         case "thunder":
            this.Thunder();
      }
   }

   private void addEnderRecipe() {
      if (!this.firstTimeRewards.containsKey("ender")) {
         ItemStack firstTime = Effects.createDRAGON();
         ItemStack standard = Effects.createDRAGON();
         this.firstTimeRewards.put("ender", firstTime);
         this.standardResults.put("ender", standard);
         NamespacedKey key = new NamespacedKey(this.plugin, "ender");
         ShapedRecipe recipe = new ShapedRecipe(key, standard);
         recipe.shape(new String[]{"HSH", "SDS", "HSH"});
         recipe.setIngredient('S', Material.SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE);
         recipe.setIngredient('D', Material.DRAGON_EGG);
         recipe.setIngredient('H', Material.DRAGON_HEAD);

         try {
            Bukkit.addRecipe(recipe);
            this.registeredRecipes.add(key);
            this.plugin.getLogger().info("Added hardcoded Ender recipe");
         } catch (Exception var6) {
            this.plugin.getLogger().warning("Failed to add Ender recipe: " + var6.getMessage());
         }
      }
   }

   private ItemStack getAugmentedItem(String key) {
      String k = key.toLowerCase(Locale.ROOT);
      switch (k) {
         case "emerald":
            return Augmented.createEME();
         case "feather":
            return Augmented.createFEATHER();
         case "fire":
            return Augmented.createFIRE();
         case "frost":
            return Augmented.createFROST();
         case "haste":
            return Augmented.createHASTE();
         case "heart":
            return Augmented.createHEART();
         case "invis":
            return Augmented.createINVIS();
         case "ocean":
            return Augmented.createOCEAN();
         case "regen":
            return Augmented.createREGEN();
         case "speed":
            return Augmented.createSPEED();
         case "strength":
            return Augmented.createST();
         case "thunder":
            return Augmented.createTHUNDER();
         case "ender":
            return Effects.createDRAGON();
         case "apophis":
            return Augmented.createAPOPHIS();
         default:
            return null;
      }
   }

   private ItemStack getEffectItem(String key) {
      String k = key.toLowerCase(Locale.ROOT);
      switch (k) {
         case "emerald":
            return Effects.createEME();
         case "feather":
            return Effects.createFEATHER();
         case "fire":
            return Effects.createFIRE();
         case "frost":
            return Effects.createFROST();
         case "haste":
            return Effects.createHASTE();
         case "heart":
            return Effects.createHEART();
         case "invis":
            return Effects.createINVIS();
         case "ocean":
            return Effects.createOCEAN();
         case "regen":
            return Effects.createREGEN();
         case "speed":
            return Effects.createSPEED();
         case "strength":
            return Effects.createST();
         case "thunder":
            return Effects.createTHUNDER();
         case "ender":
            return Effects.createDRAGON();
         case "apophis":
            return Effects.createAPOPHIS();
         default:
            return null;
      }
   }

   private void Emerald() {
      if (!this.firstTimeRewards.containsKey("emerald")) {
         ItemStack firstTime = Augmented.createEME();
         ItemStack standard = Effects.createEME();
         this.firstTimeRewards.put("emerald", firstTime);
         this.standardResults.put("emerald", standard);
         NamespacedKey key = new NamespacedKey(this.plugin, "emerald");
         ShapedRecipe recipe = new ShapedRecipe(key, standard);
         recipe.shape(new String[]{"WSW", "OEO", "CTC"});
         recipe.setIngredient('W', Material.WILD_ARMOR_TRIM_SMITHING_TEMPLATE);
         recipe.setIngredient('S', Material.SNIFFER_EGG);
         recipe.setIngredient('E', Material.EMERALD_BLOCK);
         recipe.setIngredient('O', Material.OMINOUS_BOTTLE);
         recipe.setIngredient('C', Material.ENDER_CHEST);
         recipe.setIngredient('T', Material.ENCHANTING_TABLE);

         try {
            Bukkit.addRecipe(recipe);
            this.registeredRecipes.add(key);
         } catch (Exception var6) {
            this.plugin.getLogger().warning("Failed to add Emerald recipe: " + var6.getMessage());
         }
      }
   }

   private void Feather() {
      if (!this.firstTimeRewards.containsKey("feather")) {
         ItemStack firstTime = Augmented.createFEATHER();
         ItemStack standard = Effects.createFEATHER();
         this.firstTimeRewards.put("feather", firstTime);
         this.standardResults.put("feather", standard);
         NamespacedKey key = new NamespacedKey(this.plugin, "feather");
         ShapedRecipe recipe = new ShapedRecipe(key, standard);
         recipe.shape(new String[]{"BPB", "FHF", "BPB"});
         recipe.setIngredient('B', Material.BREEZE_ROD);
         recipe.setIngredient('P', Material.PHANTOM_MEMBRANE);
         recipe.setIngredient('F', Material.FEATHER);
         recipe.setIngredient('H', Material.HEAVY_CORE);

         try {
            Bukkit.addRecipe(recipe);
            this.registeredRecipes.add(key);
         } catch (Exception var6) {
            this.plugin.getLogger().warning("Failed to add Feather recipe: " + var6.getMessage());
         }
      }
   }

   private void Fire() {
      if (!this.firstTimeRewards.containsKey("fire")) {
         ItemStack firstTime = Augmented.createFIRE();
         ItemStack standard = Effects.createFIRE();
         this.firstTimeRewards.put("fire", firstTime);
         this.standardResults.put("fire", standard);
         NamespacedKey key = new NamespacedKey(this.plugin, "fire");
         ShapedRecipe recipe = new ShapedRecipe(key, standard);
         recipe.shape(new String[]{"URU", "NPN", "URU"});
         recipe.setIngredient('U', Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE);
         recipe.setIngredient('R', Material.RESPAWN_ANCHOR);
         recipe.setIngredient('N', Material.NETHERITE_INGOT);
         recipe.setIngredient('P', Material.MUSIC_DISC_PIGSTEP);

         try {
            Bukkit.addRecipe(recipe);
            this.registeredRecipes.add(key);
         } catch (Exception var6) {
            this.plugin.getLogger().warning("Failed to add Fire recipe: " + var6.getMessage());
         }
      }
   }

   private void Frost() {
      if (!this.firstTimeRewards.containsKey("frost")) {
         ItemStack firstTime = Augmented.createFROST();
         ItemStack standard = Effects.createFROST();
         this.firstTimeRewards.put("frost", firstTime);
         this.standardResults.put("frost", standard);
         NamespacedKey key = new NamespacedKey(this.plugin, "frost");
         ShapedRecipe recipe = new ShapedRecipe(key, standard);
         recipe.shape(new String[]{"TPT", "HDH", "TPT"});
         recipe.setIngredient('T', Material.FLOW_ARMOR_TRIM_SMITHING_TEMPLATE);
         recipe.setIngredient('P', Material.PEARLESCENT_FROGLIGHT);
         recipe.setIngredient('H', Material.GOAT_HORN);
         recipe.setIngredient('D', Material.MUSIC_DISC_CREATOR);

         try {
            Bukkit.addRecipe(recipe);
            this.registeredRecipes.add(key);
         } catch (Exception var6) {
            this.plugin.getLogger().warning("Failed to add Frost recipe: " + var6.getMessage());
         }
      }
   }

   private void Haste() {
      if (!this.firstTimeRewards.containsKey("haste")) {
         ItemStack firstTime = Augmented.createHASTE();
         ItemStack standard = Effects.createHASTE();
         this.firstTimeRewards.put("haste", firstTime);
         this.standardResults.put("haste", standard);
         NamespacedKey key = new NamespacedKey(this.plugin, "haste");
         ShapedRecipe recipe = new ShapedRecipe(key, standard);
         recipe.shape(new String[]{"DBD", "GEG", "DND"});
         recipe.setIngredient('D', Material.DIAMOND_BLOCK);
         recipe.setIngredient('B', Material.BEACON);
         recipe.setIngredient('G', Material.GOLD_BLOCK);
         recipe.setIngredient('E', Material.DEEPSLATE_EMERALD_ORE);
         recipe.setIngredient('N', Material.NETHERITE_PICKAXE);

         try {
            Bukkit.addRecipe(recipe);
            this.registeredRecipes.add(key);
         } catch (Exception var6) {
            this.plugin.getLogger().warning("Failed to add Haste recipe: " + var6.getMessage());
         }
      }
   }

   private void Heart() {
      if (!this.firstTimeRewards.containsKey("heart")) {
         ItemStack firstTime = Augmented.createHEART();
         ItemStack standard = Effects.createHEART();
         this.firstTimeRewards.put("heart", firstTime);
         this.standardResults.put("heart", standard);
         NamespacedKey key = new NamespacedKey(this.plugin, "heart");
         ShapedRecipe recipe = new ShapedRecipe(key, standard);
         recipe.shape(new String[]{"PTP", "EBE", "PTP"});
         ItemStack healingPotion = buildHealingPotionStack();
         RecipeChoice potionChoice = new ExactChoice(healingPotion);
         recipe.setIngredient('P', potionChoice);
         recipe.setIngredient('T', Material.TOTEM_OF_UNDYING);
         recipe.setIngredient('E', Material.ENCHANTED_GOLDEN_APPLE);
         recipe.setIngredient('B', Material.BEETROOT);

         try {
            Bukkit.addRecipe(recipe);
            this.registeredRecipes.add(key);
         } catch (Exception var8) {
            this.plugin.getLogger().warning("Failed to add Heart recipe: " + var8.getMessage());
         }
      }
   }

   private void Invis() {
      if (!this.firstTimeRewards.containsKey("invis")) {
         ItemStack firstTime = Augmented.createINVIS();
         ItemStack standard = Effects.createINVIS();
         this.firstTimeRewards.put("invis", firstTime);
         this.standardResults.put("invis", standard);
         NamespacedKey key = new NamespacedKey(this.plugin, "invis");
         ShapedRecipe recipe = new ShapedRecipe(key, standard);
         recipe.shape(new String[]{"EME", "SOS", "ERE"});
         recipe.setIngredient('E', Material.ENDER_EYE);
         recipe.setIngredient('M', Material.MUSIC_DISC_5);
         recipe.setIngredient('S', Material.SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE);
         recipe.setIngredient('O', Material.OMINOUS_TRIAL_KEY);
         recipe.setIngredient('R', Material.RECOVERY_COMPASS);

         try {
            Bukkit.addRecipe(recipe);
            this.registeredRecipes.add(key);
         } catch (Exception var6) {
            this.plugin.getLogger().warning("Failed to add Invis recipe: " + var6.getMessage());
         }
      }
   }

   private void Ocean() {
      if (!this.firstTimeRewards.containsKey("ocean")) {
         ItemStack firstTime = Augmented.createOCEAN();
         ItemStack standard = Effects.createOCEAN();
         this.firstTimeRewards.put("ocean", firstTime);
         this.standardResults.put("ocean", standard);
         NamespacedKey key = new NamespacedKey(this.plugin, "ocean");
         ShapedRecipe recipe = new ShapedRecipe(key, standard);
         recipe.shape(new String[]{"EHE", "TCT", "EHE"});
         recipe.setIngredient('E', Material.TIDE_ARMOR_TRIM_SMITHING_TEMPLATE);
         recipe.setIngredient('H', Material.HEART_OF_THE_SEA);
         recipe.setIngredient('T', Material.TRIDENT);
         recipe.setIngredient('C', Material.CONDUIT);

         try {
            Bukkit.addRecipe(recipe);
            this.registeredRecipes.add(key);
         } catch (Exception var6) {
            this.plugin.getLogger().warning("Failed to add Ocean recipe: " + var6.getMessage());
         }
      }
   }

   private void Regen() {
      if (!this.firstTimeRewards.containsKey("regen")) {
         ItemStack firstTime = Augmented.createREGEN();
         ItemStack standard = Effects.createREGEN();
         this.firstTimeRewards.put("regen", firstTime);
         this.standardResults.put("regen", standard);
         NamespacedKey key = new NamespacedKey(this.plugin, "regen");
         ShapedRecipe recipe = new ShapedRecipe(key, standard);
         recipe.shape(new String[]{"EHE", "PAP", "EME"});
         recipe.setIngredient('E', Material.END_CRYSTAL);
         recipe.setIngredient('H', Material.HONEY_BLOCK);
         recipe.setIngredient('P', Material.PEARLESCENT_FROGLIGHT);
         recipe.setIngredient('A', Material.AXOLOTL_BUCKET);
         recipe.setIngredient('M', Material.MUSIC_DISC_CREATOR_MUSIC_BOX);

         try {
            Bukkit.addRecipe(recipe);
            this.registeredRecipes.add(key);
         } catch (Exception var6) {
            this.plugin.getLogger().warning("Failed to add Regen recipe: " + var6.getMessage());
         }
      }
   }

   private void Speed() {
      if (!this.firstTimeRewards.containsKey("speed")) {
         ItemStack firstTime = Augmented.createSPEED();
         ItemStack standard = Effects.createSPEED();
         this.firstTimeRewards.put("speed", firstTime);
         this.standardResults.put("speed", standard);
         NamespacedKey key = new NamespacedKey(this.plugin, "speed");
         ShapedRecipe recipe = new ShapedRecipe(key, standard);
         recipe.shape(new String[]{"RSR", "DHE", "RNR"});
         recipe.setIngredient('R', Material.RABBIT_FOOT);
         recipe.setIngredient('S', Material.SADDLE);
         recipe.setIngredient('D', Material.DUNE_ARMOR_TRIM_SMITHING_TEMPLATE);
         recipe.setIngredient('H', Material.DIAMOND_HORSE_ARMOR);
         recipe.setIngredient('E', Material.EYE_ARMOR_TRIM_SMITHING_TEMPLATE);
         recipe.setIngredient('N', Material.NETHERITE_BOOTS);

         try {
            Bukkit.addRecipe(recipe);
            this.registeredRecipes.add(key);
         } catch (Exception var6) {
            this.plugin.getLogger().warning("Failed to add Speed recipe: " + var6.getMessage());
         }
      }
   }

   private void Strength() {
      if (!this.firstTimeRewards.containsKey("strength")) {
         ItemStack firstTime = Augmented.createST();
         ItemStack standard = Effects.createST();
         this.firstTimeRewards.put("strength", firstTime);
         this.standardResults.put("strength", standard);
         NamespacedKey key = new NamespacedKey(this.plugin, "strength");
         ShapedRecipe recipe = new ShapedRecipe(key, standard);
         recipe.shape(new String[]{"PRP", "SWA", "PRP"});
         recipe.setIngredient('P', Material.PLAYER_HEAD);
         recipe.setIngredient('R', Material.RIB_ARMOR_TRIM_SMITHING_TEMPLATE);
         recipe.setIngredient('S', Material.NETHERITE_SWORD);
         recipe.setIngredient('W', Material.WITHER_ROSE);
         recipe.setIngredient('A', Material.NETHERITE_AXE);

         try {
            Bukkit.addRecipe(recipe);
            this.registeredRecipes.add(key);
         } catch (Exception var6) {
            this.plugin.getLogger().warning("Failed to add Strength recipe: " + var6.getMessage());
         }
      }
   }

   private void Thunder() {
      if (!this.firstTimeRewards.containsKey("thunder")) {
         ItemStack firstTime = Augmented.createTHUNDER();
         ItemStack standard = Effects.createTHUNDER();
         this.firstTimeRewards.put("thunder", firstTime);
         this.standardResults.put("thunder", standard);
         NamespacedKey key = new NamespacedKey(this.plugin, "thunder");
         ShapedRecipe recipe = new ShapedRecipe(key, standard);
         recipe.shape(new String[]{"BLB", "TCT", "BLB"});
         recipe.setIngredient('B', Material.BOLT_ARMOR_TRIM_SMITHING_TEMPLATE);
         recipe.setIngredient('L', Material.LODESTONE);
         recipe.setIngredient('T', Material.TRIDENT);
         recipe.setIngredient('C', Material.CREEPER_HEAD);

         try {
            Bukkit.addRecipe(recipe);
            this.registeredRecipes.add(key);
         } catch (Exception var6) {
            this.plugin.getLogger().warning("Failed to add Thunder recipe: " + var6.getMessage());
         }
      }
   }

   private void Apophis() {
      if (!this.firstTimeRewards.containsKey("apophis")) {
         ItemStack firstTime = Augmented.createAPOPHIS();
         ItemStack standard = Effects.createAPOPHIS();
         this.firstTimeRewards.put("apophis", firstTime);
         this.standardResults.put("apophis", standard);
         NamespacedKey key = new NamespacedKey(this.plugin, "apophis");
         ShapedRecipe recipe = new ShapedRecipe(key, standard);
         recipe.shape(new String[]{"ABA", "BCB", "DBE"});
         recipe.setIngredient('A', Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE);
         recipe.setIngredient('B', Material.NETHERITE_BLOCK);
         recipe.setIngredient('C', Material.NETHER_STAR);
         recipe.setIngredient('D', Material.MUSIC_DISC_OTHERSIDE);
         recipe.setIngredient('E', Material.ENCHANTED_BOOK);

         try {
            Bukkit.addRecipe(recipe);
            this.registeredRecipes.add(key);
            this.plugin.getLogger().info("Added hardcoded Apophis recipe");
         } catch (Exception var6) {
            this.plugin.getLogger().warning("Failed to add Apophis recipe: " + var6.getMessage());
         }
      }
   }

   public boolean triggerRitualAt(Player player, String recipeKey, Location brewingStandLoc) {
      if (player != null && recipeKey != null && brewingStandLoc != null) {
         recipeKey = this.fixKey(recipeKey.toLowerCase(Locale.ROOT));
         ItemStack craftedItem = this.firstTimeRewards.get(recipeKey);
         if (craftedItem == null) {
            player.sendMessage(ChatColor.RED + "Unknown ritual key: " + recipeKey);
            return false;
         } else if (brewingStandLoc.getBlock().getType() != Material.BREWING_STAND) {
            player.sendMessage(ChatColor.RED + "That location is not a brewing stand.");
            return false;
         } else if (this.protectedBrewingStands.contains(brewingStandLoc)) {
            player.sendMessage(ChatColor.RED + "There is an ongoing ritual here.");
            return false;
         } else if (Boolean.TRUE.equals(this.activeRituals.get("Augmented"))) {
            player.sendMessage(ChatColor.RED + "There is already an ongoing ritual.");
            return false;
         } else {
            this.startRitual(player, recipeKey, brewingStandLoc, craftedItem);
            return true;
         }
      } else {
         return false;
      }
   }

   public void reloadRecipes() {
      try {
         this.plugin.getLogger().info("Reloading recipes from config...");
         this.clearAllRecipes();
         this.plugin.reloadConfig();
         this.loadRecipesFromConfig();
         this.plugin.getLogger().info("Recipes reloaded successfully. Active recipes: " + this.firstTimeRewards.keySet());
      } catch (Exception var2) {
         this.plugin.getLogger().severe("Failed to reload recipes: " + var2.getMessage());
         var2.printStackTrace();
      }
   }

   public void debugRecipes() {
      this.plugin.getLogger().info("=== RECIPE DEBUG INFO ===");
      this.plugin.getLogger().info("Registered Bukkit recipes: " + this.registeredRecipes.size());

      for (NamespacedKey key : this.registeredRecipes) {
         this.plugin.getLogger().info("  - " + key.getKey());
      }

      this.plugin.getLogger().info("First time rewards: " + this.firstTimeRewards.keySet());
      this.plugin.getLogger().info("Standard results: " + this.standardResults.keySet());
      this.plugin.getLogger().info("========================");
   }
}
