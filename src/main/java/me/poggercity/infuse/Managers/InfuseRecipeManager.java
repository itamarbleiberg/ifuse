package me.poggercity.infuse.Managers;

import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.entity.Entity;
import java.util.ArrayList;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
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
import me.poggercity.infuse.Infuses.WinnersCurse;
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
import org.bukkit.event.world.WorldLoadEvent;
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
   /** Scoreboard tag on entities Infuse spawns for ritual beams, so only those are removed on shutdown. */
   public static final String BEAM_TAG = "infuse_ritual_beam";
   /** recipe_limits.yml section with the number of normal crafts made per effect. */
   private static final String CRAFTED = "crafted";
   private final JavaPlugin plugin;
   private final File recipeLimitFile;
   private final FileConfiguration recipeLimits;
   private final Map<String, ItemStack> firstTimeRewards;
   private final Map<String, ItemStack> standardResults;
   private final Set<Location> protectedBrewingStands = Collections.synchronizedSet(new HashSet<>());
   private final List<ActiveRitual> activeRituals = new ArrayList<>();
   // Saved rituals that can't run yet (their world isn't loaded); kept as read from rituals.yml.
   private final List<Map<String, Object>> waitingRituals = new ArrayList<>();
   private final File ritualFile;
   private final Set<NamespacedKey> registeredRecipes = Collections.synchronizedSet(new HashSet<>());

   public InfuseRecipeManager(JavaPlugin plugin) {
      this.plugin = plugin;
      this.recipeLimitFile = new File(plugin.getDataFolder(), "recipe_limits.yml");
      this.recipeLimits = YamlConfiguration.loadConfiguration(this.recipeLimitFile);
      this.firstTimeRewards = new HashMap<>();
      this.standardResults = new HashMap<>();
      this.ritualFile = new File(plugin.getDataFolder(), "rituals.yml");

      try {
         this.migrateCraftLimits();
         this.migrateRenamedEffects();
         this.loadRecipesFromConfig();
         plugin.getServer().getPluginManager().registerEvents(this, plugin);
         this.resumeSavedRituals();
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

   // Keys typed or saved by an older version ("gamble" is Winner's Curse since 2.8).
   private String fixKey(String recipeKey) {
      return EffectKeys.normalize(recipeKey);
   }

   // Winner's Curse was called Gamble in 2.7: its crafts made and augments used are kept under the new key.
   private void migrateRenamedEffects() {
      boolean changed = false;

      for (Entry<String, String> rename : EffectKeys.RENAMED.entrySet()) {
         for (String section : new String[]{"first_time", CRAFTED}) {
            String oldPath = section + "." + rename.getKey();
            if (this.recipeLimits.contains(oldPath)) {
               String newPath = section + "." + rename.getValue();
               this.recipeLimits.set(newPath, this.recipeLimits.getInt(newPath, 0) + this.recipeLimits.getInt(oldPath, 0));
               this.recipeLimits.set(oldPath, null);
               changed = true;
            }
         }
      }

      if (changed) {
         this.saveRecipeLimits();
         this.plugin.getLogger().info("recipe_limits.yml: Gamble's crafts were moved to its new name, Winner's Curse (winners_curse)");
      }
   }

   @EventHandler
   public void onCrafterCraft(CrafterCraftEvent event) {
      ItemStack result = event.getResult();
      if (result != null && result.getType() == Material.POTION) {
         event.setCancelled(true);
      }
   }

   /** A ritual in progress at a brewing stand. Saved to rituals.yml on shutdown and resumed on startup. */
   private static final class ActiveRitual {
      final String effect;
      final boolean augmented;
      final Location location;
      final String playerName;
      final int totalTicks;
      int elapsedTicks;
      BossBar bossBar;
      ScheduledTask task;
      final List<Entity> beam = new ArrayList<>();

      ActiveRitual(String effect, boolean augmented, Location location, String playerName, int totalTicks, int elapsedTicks) {
         this.effect = effect;
         this.augmented = augmented;
         this.location = location;
         this.playerName = playerName;
         this.totalTicks = totalTicks;
         this.elapsedTicks = elapsedTicks;
      }
   }

   private static String dimensionLabel(World world) {
      return switch (world.getEnvironment()) {
         case NORMAL -> ChatColor.GREEN + "Overworld";
         case NETHER -> ChatColor.DARK_RED + "Nether";
         case THE_END -> ChatColor.DARK_PURPLE + "End";
         default -> ChatColor.GRAY + world.getName();
      };
   }

   private void spawnCustomBeam(ActiveRitual ritual) {
      if (this.plugin.getConfig().getBoolean("rituals.beam", true)) {
         Location brewingStandLocation = ritual.location.clone().add(0.5, 0.5, 0.5);
         World world = brewingStandLocation.getWorld();
         Location crystalLoc = new Location(world, brewingStandLocation.getX(), 600.0, brewingStandLocation.getZ());
         EnderCrystal crystal = (EnderCrystal)world.spawnEntity(crystalLoc, EntityType.END_CRYSTAL);
         crystal.setShowingBottom(false);
         crystal.setInvulnerable(true);
         crystal.setInvisible(true);
         // Tagged and not saved with the chunk, so only these are removed on shutdown (the old code removed every
         // end crystal in every world) and none are left behind after a crash.
         crystal.addScoreboardTag(BEAM_TAG);
         crystal.setPersistent(false);
         Location targetLoc = brewingStandLocation.clone().subtract(0.0, 2.0, 0.0);
         ArmorStand marker = (ArmorStand)world.spawnEntity(targetLoc, EntityType.ARMOR_STAND);
         marker.setMarker(true);
         marker.setInvisible(true);
         marker.setInvulnerable(true);
         marker.setSilent(true);
         marker.setCustomNameVisible(false);
         marker.addScoreboardTag(BEAM_TAG);
         marker.setPersistent(false);
         crystal.setBeamTarget(marker.getLocation().toBlockLocation());
         ritual.beam.add(crystal);
         ritual.beam.add(marker);
      }
   }

   // The beam entities aren't saved with the chunk, so they vanish when the chunk unloads (everyone walked away from a
   // long ritual, or the ritual was resumed after a restart). They are put back once the chunk is loaded again, and
   // follow configs.brewing_beam if it is changed while a ritual runs.
   private void refreshBeam(ActiveRitual ritual) {
      if (!this.plugin.getConfig().getBoolean("rituals.beam", true)) {
         removeBeam(ritual);
         return;
      }

      World world = ritual.location.getWorld();
      int cx = ritual.location.getBlockX() >> 4;
      int cz = ritual.location.getBlockZ() >> 4;
      if (world.isChunkLoaded(cx, cz) && world.getChunkAt(cx, cz).isEntitiesLoaded()) {
         boolean intact = !ritual.beam.isEmpty();

         for (Entity e : ritual.beam) {
            if (!e.isValid()) {
               intact = false;
               break;
            }
         }

         if (!intact) {
            removeBeam(ritual);
            this.spawnCustomBeam(ritual);
         }
      }
   }

   private static void removeBeam(ActiveRitual ritual) {
      for (Entity e : ritual.beam) {
         if (e.isValid()) {
            e.remove();
         }
      }

      ritual.beam.clear();
   }

   private ItemStack rewardFor(String effect, boolean augmented) {
      ItemStack item = augmented ? this.firstTimeRewards.get(effect) : this.standardResults.get(effect);
      return item == null ? null : item.clone();
   }

   private boolean anyRitualRunning() {
      return !this.activeRituals.isEmpty();
   }

   private boolean oneAtATime() {
      return this.plugin.getConfig().getBoolean("rituals.one_at_a_time", true);
   }

   /** Starts a ritual. Ritual length comes from effects.<effect>.craft (augmented_ritual_seconds / normal_ritual_seconds). */
   private void startRitual(Player player, String recipeKey, Location brewingStand, boolean augmented) {
      int seconds = Settings.ritualSeconds(recipeKey, augmented);
      ActiveRitual ritual = new ActiveRitual(
         recipeKey, augmented, brewingStand.getBlock().getLocation(), player.getName(), Math.max(1, seconds * 20), 0
      );
      this.beginRitual(ritual, true);
   }

   private void beginRitual(ActiveRitual ritual, boolean announce) {
      ItemStack reward = this.rewardFor(ritual.effect, ritual.augmented);
      if (reward == null) {
         this.plugin.getLogger().warning("Ritual for unknown effect '" + ritual.effect + "' dropped");
         return;
      }

      Location blockLoc = ritual.location;
      this.protectedBrewingStands.add(blockLoc);
      this.activeRituals.add(ritual);
      ItemMeta meta = reward.getItemMeta();
      String displayName = meta != null && meta.hasDisplayName() ? meta.getDisplayName() : ChatColor.WHITE + "Effect";
      String itemName = displayName.replace("Effect", "");
      BarColor barColor = this.getColorFromItemName(itemName);
      String colorOnly = ChatColor.getLastColors(itemName);
      String formattedItemName = ChatColor.BOLD.toString() + colorOnly + "\ud83e\uddea " + ChatColor.stripColor(itemName) + " \ud83e\uddea";
      BossBar bossBar = Bukkit.createBossBar(formattedItemName, barColor, BarStyle.SOLID, new BarFlag[0]);
      ritual.bossBar = bossBar;

      for (Player p : Bukkit.getOnlinePlayers()) {
         bossBar.addPlayer(p);
      }

      World world = blockLoc.getWorld();
      Location soundLoc = blockLoc.clone().add(0.5, 0.5, 0.5);
      if (announce) {
         Bukkit.broadcastMessage(
            colorOnly
               + "\ud83e\uddea "
               + ritual.playerName
               + " is cooking up the "
               + itemName
               + " at "
               + ChatColor.BOLD
               + ChatColor.AQUA
               + blockLoc.getBlockX()
               + ", "
               + blockLoc.getBlockY()
               + ", "
               + blockLoc.getBlockZ()
               + "... "
               + dimensionLabel(world)
         );

         for (Player p : world.getPlayers()) {
            p.playSound(soundLoc, Sound.BLOCK_END_PORTAL_SPAWN, 1.0F, 1.0F);
         }
      } else {
         String msg = colorOnly + "\ud83e\uddea The " + itemName + "ritual at " + ChatColor.BOLD + ChatColor.AQUA + blockLoc.getBlockX() + ", "
            + blockLoc.getBlockY() + ", " + blockLoc.getBlockZ() + ChatColor.RESET + colorOnly + " continues... " + dimensionLabel(world);
         // Resumed during startup: nobody is online yet and broadcasts don't reach the console that early, so it's logged.
         this.plugin.getLogger().info(ChatColor.stripColor(msg));

         for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(msg);
         }
      }

      this.refreshBeam(ritual);
      this.saveRituals();
      ritual.task = Bukkit.getGlobalRegionScheduler().runAtFixedRate(this.plugin, task -> {
         ritual.elapsedTicks++;
         if (ritual.elapsedTicks % 20 == 0) {
            this.refreshBeam(ritual);
         }

         double progress = Math.min(1.0, Math.max(0.0, (double)ritual.elapsedTicks / ritual.totalTicks));
         bossBar.setProgress(progress);
         if (progress >= 1.0) {
            task.cancel();
            this.finishRitual(ritual, itemName, soundLoc);
         }
      }, 1L, 1L);
   }

   private void finishRitual(ActiveRitual ritual, String itemName, Location soundLoc) {
      ritual.bossBar.removeAll();
      Bukkit.broadcastMessage(ChatColor.WHITE + ChatColor.stripColor(itemName) + " has been brewed!");
      World world = ritual.location.getWorld();

      for (Player p : world.getPlayers()) {
         p.playSound(soundLoc, Sound.BLOCK_END_PORTAL_SPAWN, 1.0F, 1.0F);
         p.playSound(soundLoc, Sound.BLOCK_BREWING_STAND_BREW, 1.0F, 1.2F);
      }

      // Load the chunk first so the reward isn't dropped into an unloaded chunk.
      ritual.location.getChunk();
      ItemStack reward = this.rewardFor(ritual.effect, ritual.augmented);
      if (reward != null) {
         Item dropped = world.dropItemNaturally(soundLoc, reward);
         dropped.setInvulnerable(true);
      }

      removeBeam(ritual);
      this.protectedBrewingStands.remove(ritual.location);
      this.activeRituals.remove(ritual);
      this.saveRituals();
   }

   private void saveRituals() {
      YamlConfiguration out = new YamlConfiguration();
      int i = 0;

      for (ActiveRitual r : this.activeRituals) {
         String path = "rituals." + i++;
         out.set(path + ".effect", r.effect);
         out.set(path + ".augmented", r.augmented);
         out.set(path + ".world", r.location.getWorld().getName());
         out.set(path + ".x", r.location.getBlockX());
         out.set(path + ".y", r.location.getBlockY());
         out.set(path + ".z", r.location.getBlockZ());
         out.set(path + ".player", r.playerName);
         out.set(path + ".total_ticks", r.totalTicks);
         out.set(path + ".elapsed_ticks", r.elapsedTicks);
      }

      // Rituals that couldn't be resumed yet (world not loaded) are written back unchanged instead of being lost.
      for (Map<String, Object> waiting : this.waitingRituals) {
         out.createSection("rituals." + i++, waiting);
      }

      try {
         out.save(this.ritualFile);
      } catch (IOException var5) {
         this.plugin.getLogger().warning("Could not save rituals.yml: " + var5.getMessage());
      }
   }

   // A restart used to throw away running rituals: the ingredients and the augmented craft were already used up and
   // the effect was never handed out. Rituals are saved on shutdown and continue where they stopped.
   private void resumeSavedRituals() {
      if (this.ritualFile.exists()) {
         YamlConfiguration in = YamlConfiguration.loadConfiguration(this.ritualFile);
         ConfigurationSection section = in.getConfigurationSection("rituals");
         if (section != null) {
            for (String id : section.getKeys(false)) {
               ConfigurationSection r = section.getConfigurationSection(id);
               if (r != null) {
                  this.waitingRituals.add(r.getValues(false));
               }
            }

            this.resumeWaitingRituals();
         }
      }
   }

   // Starts the saved rituals whose world is loaded. The others wait (and stay in rituals.yml) until their world is
   // loaded, for example by a world manager plugin that loads its worlds after Infuse.
   private void resumeWaitingRituals() {
      for (Map<String, Object> r : new ArrayList<>(this.waitingRituals)) {
         String worldName = String.valueOf(r.getOrDefault("world", ""));
         World world = Bukkit.getWorld(worldName);
         // (a ritual saved by 2.7 can have the old key "gamble")
         String effect = EffectKeys.normalize(String.valueOf(r.getOrDefault("effect", "")));
         boolean augmented = Boolean.TRUE.equals(r.get("augmented"));
         if (world == null) {
            this.plugin.getLogger().info("Saved " + effect + " ritual is in world '" + worldName + "', which isn't loaded yet; it resumes when it loads");
         } else if (this.rewardFor(effect, augmented) == null) {
            this.plugin.getLogger().warning("Saved ritual for unknown effect '" + effect + "' kept in rituals.yml (no recipe with that name)");
         } else {
            this.waitingRituals.remove(r);
            Location loc = new Location(world, intOf(r.get("x")), intOf(r.get("y")), intOf(r.get("z")));
            ActiveRitual ritual = new ActiveRitual(
               effect, augmented, loc, String.valueOf(r.getOrDefault("player", "Someone")), Math.max(1, intOf(r.get("total_ticks"))),
               Math.max(0, intOf(r.get("elapsed_ticks")))
            );
            this.beginRitual(ritual, false);
         }
      }
   }

   private static int intOf(Object o) {
      return o instanceof Number n ? n.intValue() : 0;
   }

   @EventHandler
   public void onWorldLoad(WorldLoadEvent event) {
      if (!this.waitingRituals.isEmpty()) {
         this.resumeWaitingRituals();
      }
   }

   /** Saves running rituals for the next startup and removes their beams. */
   public void shutdown() {
      this.saveRituals();

      for (ActiveRitual r : new ArrayList<>(this.activeRituals)) {
         if (r.task != null) {
            r.task.cancel();
         }

         if (r.bossBar != null) {
            r.bossBar.removeAll();
         }

         removeBeam(r);
      }

      this.activeRituals.clear();
      this.protectedBrewingStands.clear();
   }

   @EventHandler
   public void onJoinShowRituals(PlayerJoinEvent event) {
      for (ActiveRitual r : this.activeRituals) {
         if (r.bossBar != null) {
            r.bossBar.addPlayer(event.getPlayer());
         }
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

         if (strippedName.contains("augmented winner's curse")) {
            return BarColor.GREEN;
         }
      }

      return BarColor.WHITE;
   }

   private static boolean brewingStandNearby(Player player) {
      Location loc = player.getLocation();
      World world = loc.getWorld();

      for (int x = -5; x <= 5; x++) {
         for (int y = -5; y <= 5; y++) {
            for (int z = -5; z <= 5; z++) {
               if (world.getBlockAt(loc.getBlockX() + x, loc.getBlockY() + y, loc.getBlockZ() + z).getType() == Material.BREWING_STAND) {
                  return true;
               }
            }
         }
      }

      return false;
   }

   private static String tooFarMessage() {
      return "Effects can only be crafted within " + Settings.maxCraftDistance() + " blocks of 0, 0";
   }

   private static String effectLabel(String recipeKey) {
      return EffectKeys.title(recipeKey);
   }

   /** Removes one of every ingredient, keeping the rest of each stack (and its data) as it was. */
   private static void consumeIngredients(CraftingInventory inv) {
      ItemStack[] matrix = inv.getMatrix();

      for (int i = 0; i < matrix.length; i++) {
         if (matrix[i] != null && matrix[i].getType() != Material.AIR) {
            int newAmt = matrix[i].getAmount() - 1;
            if (newAmt > 0) {
               ItemStack rest = matrix[i].clone();
               rest.setAmount(newAmt);
               matrix[i] = rest;
            } else {
               matrix[i] = null;
            }
         }
      }

      inv.setMatrix(matrix);
   }

   @EventHandler
   public void onCraft(CraftItemEvent event) {
      if (event.getRecipe() instanceof ShapedRecipe shaped) {
         String recipeKey = this.fixKey(shaped.getKey().getKey());
         if (this.firstTimeRewards.containsKey(recipeKey) && event.getWhoClicked() instanceof Player player) {
            Inventory topInv = event.getView().getTopInventory();
            if (topInv.getType() != InventoryType.WORKBENCH) {
               event.setCancelled(true);
               player.sendMessage(ChatColor.RED + "You must use a crafting table to craft this item!");
               return;
            }

            // Only a plain click. Shift-click was already blocked, but Ctrl+Q on the result slot crafts again and again
            // in one click while the craft limit was lowered only once.
            if (event.getClick() != ClickType.LEFT && event.getClick() != ClickType.RIGHT) {
               event.setCancelled(true);
               player.sendMessage(ChatColor.RED + "Take the effect out with a normal click.");
               return;
            }

            if (!brewingStandNearby(player)) {
               event.setCancelled(true);
               return;
            }

            if (!Settings.ritualAllowedIn(recipeKey, player.getWorld())) {
               event.setCancelled(true);
               player.sendMessage(
                  ChatColor.RED + effectLabel(recipeKey) + " can only be made in: " + Settings.describeDimensions(Settings.ritualDimensions(recipeKey))
               );
               return;
            }

            if (!Settings.withinCraftDistance(player.getLocation())) {
               event.setCancelled(true);
               player.sendMessage(ChatColor.RED + tooFarMessage());
               return;
            }

            String firstTimeKey = "first_time." + recipeKey;
            String craftedKey = CRAFTED + "." + recipeKey;
            int limit = this.getCraftLimit(recipeKey);
            int crafted = this.recipeLimits.getInt(craftedKey, 0);
            int maxAugments = Settings.augmentedCrafts(recipeKey);
            int usedAugments = this.recipeLimits.getInt(firstTimeKey, 0);
            boolean augmented = maxAugments == -1 || usedAugments < maxAugments;
            if (!augmented && limit != -1 && crafted >= limit) {
               event.setCancelled(true);
               player.sendMessage(ChatColor.RED + "You have reached the crafting limit for this item!");
               return;
            }

            boolean ritual = augmented || Settings.ritualSeconds(recipeKey, false) > 0;
            if (!ritual) {
               // Normal effect with no ritual time: crafted right away (the craft itself is done by the game).
               // A click with something on the cursor can't pick the potion up, so the game crafts nothing; counting
               // it used up the server-wide craft limit without anyone getting the effect.
               if (!player.getItemOnCursor().getType().isAir()) {
                  event.setCancelled(true);
                  return;
               }

               // The result slot can still show the augmented effect (it is only refreshed when that player's grid
               // changes) after someone else used the last augment, so the normal effect is put in before the game
               // hands it over.
               ItemStack result = this.standardResults.get(recipeKey);
               if (result == null) {
                  event.setCancelled(true);
                  return;
               }

               result = result.clone();
               event.getInventory().setResult(result);
               if (limit != -1) {
                  this.recipeLimits.set(craftedKey, crafted + 1);
                  this.saveRecipeLimits();
               }

               String disp = result.hasItemMeta() && result.getItemMeta().hasDisplayName() ? result.getItemMeta().getDisplayName() : ChatColor.WHITE + "Effect";
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
                     + dimensionLabel(player.getWorld())
               );
               return;
            }

            event.setCancelled(true);
            Block targetBlock = player.getTargetBlockExact(4);
            if (targetBlock == null || targetBlock.getType() != Material.BREWING_STAND) {
               player.sendMessage(ChatColor.RED + "You must use a brewing stand to start the ritual.");
               return;
            }

            Location blockLoc = targetBlock.getLocation();
            if (this.protectedBrewingStands.contains(blockLoc)) {
               player.sendMessage(ChatColor.RED + "There is an ongoing ritual here.");
               return;
            }

            if (this.oneAtATime() && this.anyRitualRunning()) {
               player.sendMessage(ChatColor.RED + "There is already an ongoing ritual.");
               return;
            }

            consumeIngredients(event.getInventory());
            player.updateInventory();
            player.closeInventory();
            if (augmented) {
               this.recipeLimits.set(firstTimeKey, usedAugments + 1);
            } else if (limit != -1) {
               this.recipeLimits.set(craftedKey, crafted + 1);
            }

            this.saveRecipeLimits();
            this.startRitual(player, recipeKey, blockLoc, augmented);
         }
      }
   }

   /** A copy of the normal effect item that the recipe makes (for the recipe editor). */
   public ItemStack getEffectPreview(String recipeKey) {
      ItemStack item = this.standardResults.get(recipeKey);
      return item == null ? null : item.clone();
   }

   public int getCraftLimit(String recipeKey) {
      return Settings.normalCrafts(recipeKey);
   }

   public Map<String, ItemStack> getFirstTimeRewards() {
      return this.firstTimeRewards;
   }

   /** Normal crafts left: the configured limit minus the normal crafts made so far, or -1 when unlimited. */
   public int getCraftsLeft(String recipeKey) {
      int limit = this.getCraftLimit(recipeKey);
      return limit == -1 ? -1 : Math.max(0, limit - this.recipeLimits.getInt(CRAFTED + "." + recipeKey, 0));
   }

   // recipe_limits.yml used to store the crafts LEFT ("craft_limit.<effect>"), seeded from the config on the first
   // craft, so changing "effects.<effect>" later did nothing. It now stores the crafts MADE, like the augments, and the
   // limit always comes from the config. Old files are converted once using the current limits.
   private void migrateCraftLimits() {
      ConfigurationSection old = this.recipeLimits.getConfigurationSection("craft_limit");
      if (old != null) {
         for (String key : old.getKeys(false)) {
            int limit = this.getCraftLimit(key);
            if (!this.recipeLimits.contains(CRAFTED + "." + key) && limit != -1) {
               this.recipeLimits.set(CRAFTED + "." + key, Math.max(0, limit - old.getInt(key)));
            }
         }

         this.recipeLimits.set("craft_limit", null);
         this.saveRecipeLimits();
         this.plugin.getLogger().info("recipe_limits.yml: converted crafts left (craft_limit) to crafts made (" + CRAFTED + ")");
      }
   }

   public int getMaxAugments(String recipeKey) {
      return Settings.augmentedCrafts(recipeKey);
   }

   public int getUsedAugments(String recipeKey) {
      return this.recipeLimits.getInt("first_time." + recipeKey, 0);
   }

   @EventHandler
   public void onPrepareCraft(PrepareItemCraftEvent event) {
      if (event.getRecipe() instanceof ShapedRecipe shaped) {
         String recipeKey = this.fixKey(shaped.getKey().getKey());
         if (this.firstTimeRewards.containsKey(recipeKey)) {
            if (event.getView().getTopInventory().getType() != InventoryType.WORKBENCH) {
               event.getInventory().setResult(null);
               return;
            }

            Player player = (Player)event.getView().getPlayer();
            if (!brewingStandNearby(player)) {
               event.getInventory().setResult(null);
            } else if (!Settings.ritualAllowedIn(recipeKey, player.getWorld())) {
               event.getInventory().setResult(null);
               player.sendActionBar(
                  ChatColor.RED + effectLabel(recipeKey) + " can only be made in: " + Settings.describeDimensions(Settings.ritualDimensions(recipeKey))
               );
            } else if (!Settings.withinCraftDistance(player.getLocation())) {
               event.getInventory().setResult(null);
               player.sendActionBar(ChatColor.RED + tooFarMessage());
            } else {
               int maxAug = Settings.augmentedCrafts(recipeKey);
               int usedAug = this.recipeLimits.getInt("first_time." + recipeKey, 0);
               int limit = this.getCraftLimit(recipeKey);
               if (maxAug == -1 || usedAug < maxAug) {
                  event.getInventory().setResult(this.firstTimeRewards.get(recipeKey));
               } else if (limit != -1 && this.getCraftsLeft(recipeKey) <= 0) {
                  event.getInventory().setResult(null);
               } else {
                  event.getInventory().setResult(this.standardResults.get(recipeKey));
               }
            }
         }
      }
   }

   // ignoreCancelled: claim/protection plugins that block using someone else's brewing stand are respected now.
   @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
   public void onBrewingStandInteract(PlayerInteractEvent event) {
      if (event.getAction() == Action.RIGHT_CLICK_BLOCK
         && event.getClickedBlock() != null
         && event.getClickedBlock().getType() == Material.BREWING_STAND
         && this.plugin.getConfig().getBoolean("rituals.brewing_stand_opens_crafting", true)) {
         event.setCancelled(true);
         Player player = event.getPlayer();
         Bukkit.getScheduler().runTask(this.plugin, () -> player.openWorkbench(null, true));
      }
   }

   private void loadRecipesFromConfig() {
      this.clearAllRecipes();
      this.firstTimeRewards.clear();
      this.standardResults.clear();
      // Each effect's recipe is in effects.<effect>.craft.recipe. A recipe is taken as a whole from config.yml, or as a
      // whole from the jar's defaults if config.yml doesn't have it (never mixed, the letters wouldn't match).
      for (String key : Infuse.EFFECT_KEYS) {
         String path = Settings.recipePath(key);
         ConfigurationSection sec = this.plugin.getConfig().contains(path, true)
            ? this.plugin.getConfig().getConfigurationSection(path)
            : this.plugin.getConfig().getDefaults() == null ? null : this.plugin.getConfig().getDefaults().getConfigurationSection(path);
         if (sec != null) {
            this.loadRecipe(key, sec);
         }
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

   // Loads one effect recipe from config.yml. If anything about it is wrong (shape, unknown item, a letter without an
   // item), it isn't registered and the effect gets its built-in default recipe instead (addMissingHardcodedRecipes),
   // so an effect is never left without a recipe.
   private void loadRecipe(String key, ConfigurationSection sec) {
      try {
         String recipeName = sec.getString("key", key);
         ItemStack firstTime = this.getAugmentedItem(key);
         ItemStack standard = this.getEffectItem(key);
         if (firstTime == null || standard == null) {
            this.plugin.getLogger().warning("No items found for recipe '" + key + "', skipping config recipe");
            return;
         }

         NamespacedKey nk = new NamespacedKey(this.plugin, recipeName);
         if (this.registeredRecipes.contains(nk)) {
            Bukkit.removeRecipe(nk);
            this.registeredRecipes.remove(nk);
         }

         List<String> shape = sec.getStringList("shape");
         if (shape.size() != 3) {
            this.plugin.getLogger().warning("Recipe '" + key + "' has an invalid shape (it needs exactly 3 rows), using the default recipe: " + shape);
            return;
         }

         ShapedRecipe recipe = new ShapedRecipe(nk, standard);
         recipe.shape(new String[]{shape.get(0), shape.get(1), shape.get(2)});
         String usedLetters = String.join("", shape);
         ConfigurationSection ings = sec.getConfigurationSection("ingredients");

         for (char c : usedLetters.toCharArray()) {
            if (c != ' ' && (ings == null || !ings.contains(String.valueOf(c), true))) {
               this.plugin.getLogger().warning("Recipe '" + key + "' uses '" + c + "' in its shape but has no item for it, using the default recipe");
               return;
            }
         }

         if (ings != null) {
            for (String ch : ings.getKeys(false)) {
               // Letters the shape doesn't use are skipped (2.5 could save extra letters into changed recipes).
               if (ch.length() == 1 && usedLetters.indexOf(ch.charAt(0)) >= 0) {
                  char c = ch.charAt(0);
                  String materialName = ings.getString(ch, "").toUpperCase(Locale.ROOT);
                  if (key.equalsIgnoreCase("heart") && materialName.equals("POTION")) {
                     recipe.setIngredient(c, new ExactChoice(buildHealingPotionStack()));
                  } else {
                     Material mat = Material.matchMaterial(materialName);
                     if (mat == null || !mat.isItem() || mat.isAir()) {
                        this.plugin.getLogger().warning("Unknown item '" + materialName + "' for '" + c + "' in recipe '" + key + "', using the default recipe");
                        return;
                     }

                     recipe.setIngredient(c, mat);
                  }
               }
            }
         }

         Bukkit.addRecipe(recipe);
         this.registeredRecipes.add(nk);
         this.firstTimeRewards.put(key, firstTime);
         this.standardResults.put(key, standard);
         this.plugin.getLogger().info("Loaded recipe from config: " + key + " (NamespacedKey: " + nk.getKey() + ")");
      } catch (Exception ex) {
         this.firstTimeRewards.remove(key);
         this.standardResults.remove(key);
         this.plugin.getLogger().warning("Recipe '" + key + "' could not be added, using the default recipe: " + ex.getMessage());
      }
   }

   private void addMissingHardcodedRecipes() {
      for (String key : Infuse.EFFECT_KEYS) {
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
            break;
         case WinnersCurse.KEY:
            this.WinnersCurseRecipe();
      }
   }

   private void WinnersCurseRecipe() {
      if (!this.firstTimeRewards.containsKey(WinnersCurse.KEY)) {
         ItemStack firstTime = Augmented.createCURSE();
         ItemStack standard = Effects.createCURSE();
         this.firstTimeRewards.put(WinnersCurse.KEY, firstTime);
         this.standardResults.put(WinnersCurse.KEY, standard);
         NamespacedKey key = new NamespacedKey(this.plugin, WinnersCurse.KEY);
         ShapedRecipe recipe = new ShapedRecipe(key, standard);
         recipe.shape(new String[]{"VGV", "ERE", "VGV"});
         recipe.setIngredient('V', Material.VEX_ARMOR_TRIM_SMITHING_TEMPLATE);
         recipe.setIngredient('G', Material.GOLD_BLOCK);
         recipe.setIngredient('E', Material.EMERALD_BLOCK);
         recipe.setIngredient('R', Material.MUSIC_DISC_RELIC);

         try {
            Bukkit.addRecipe(recipe);
            this.registeredRecipes.add(key);
         } catch (Exception ex) {
            this.plugin.getLogger().warning("Failed to add Winner's Curse recipe: " + ex.getMessage());
         }
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
         case WinnersCurse.KEY:
            return Augmented.createCURSE();
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
         case WinnersCurse.KEY:
            return Effects.createCURSE();
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
         String key = recipeKey.toLowerCase(Locale.ROOT);
         boolean augmented = true;
         if (key.startsWith("augmented_")) {
            key = key.substring("augmented_".length());
         } else if (key.startsWith("augment_")) {
            key = key.substring("augment_".length());
         }

         key = this.fixKey(key);
         Location blockLoc = brewingStandLoc.getBlock().getLocation();
         if (!this.firstTimeRewards.containsKey(key)) {
            player.sendMessage(ChatColor.RED + "Unknown ritual key: " + recipeKey);
            return false;
         } else if (blockLoc.getBlock().getType() != Material.BREWING_STAND) {
            player.sendMessage(ChatColor.RED + "That location is not a brewing stand.");
            return false;
         } else if (this.protectedBrewingStands.contains(blockLoc)) {
            player.sendMessage(ChatColor.RED + "There is an ongoing ritual here.");
            return false;
         } else if (this.oneAtATime() && this.anyRitualRunning()) {
            player.sendMessage(ChatColor.RED + "There is already an ongoing ritual.");
            return false;
         } else {
            this.startRitual(player, key, blockLoc, augmented);
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
