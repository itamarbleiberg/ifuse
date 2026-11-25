package me.poggercity.infuse.Managers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import me.poggercity.infuse.Passive;
import me.poggercity.infuse.IMP.Abilities;
import me.poggercity.infuse.IMP.ActionBarUpdater;
import me.poggercity.infuse.IMP.HackEquipListener;
import me.poggercity.infuse.IMP.HackMapping;
import me.poggercity.infuse.IMP.HackRerollListener;
import me.poggercity.infuse.IMP.SwordBringer;
import me.poggercity.infuse.IMP.UnequipCommand;
import me.poggercity.infuse.Infuses.Apophis;
import me.poggercity.infuse.Infuses.Effects;
import me.poggercity.infuse.Infuses.Emerald;
import me.poggercity.infuse.Infuses.Ender;
import me.poggercity.infuse.Infuses.Feather;
import me.poggercity.infuse.Infuses.Fire;
import me.poggercity.infuse.Infuses.Frost;
import me.poggercity.infuse.Infuses.Haste;
import me.poggercity.infuse.Infuses.Heart;
import me.poggercity.infuse.Infuses.HexColorTranslator;
import me.poggercity.infuse.Infuses.Invis;
import me.poggercity.infuse.Infuses.Ocean;
import me.poggercity.infuse.Infuses.Regen;
import me.poggercity.infuse.Infuses.Ritual;
import me.poggercity.infuse.Infuses.Speed;
import me.poggercity.infuse.Infuses.Strength;
import me.poggercity.infuse.Infuses.Thunder;
import me.poggercity.infuse.Infuses.ToolBonuses;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class Infuse extends JavaPlugin implements Listener, TabCompleter {
   private static Infuse instance;
   private final Set<UUID> hackManager = Collections.newSetFromMap(new ConcurrentHashMap<>());
   private PlayerHackManager hackManager2;
   private Abilities abilitiesHandler;
   private InfuseRecipeManager recipeManager;
   private Fire fire;
   private Frost frost;
   private Emerald eme;
   private Haste haste;
   private Invis invis;
   private Heart heart;
   private Ocean ocean;
   private Strength strength;
   private Speed speed;
   private Ender dragon;
   private Feather feather;
   private Regen regen;
   private Thunder thunder;
   private Apophis apophis;
   private ToolBonuses toolBonuses;
   private Passive passive;
   private Ritual ritual;
   private TrustManager trustManager;

   public static Infuse getInstance() {
      return instance;
   }

   public TrustManager getTrustManager() {
      return this.trustManager;
   }

   public void onEnable() {
      instance = this;
      this.saveDefaultConfig();
      // Older config.yml files (2.4 / 2.5 layout) are converted to the current layout, with a backup of the old file.
      // Settings missing from config.yml are added with their default values. copyDefaults stays off: it made sections
      // like a recipe's ingredients list the jar's keys too, so a changed recipe got the old letters mixed in.
      ConfigMigration.run(this);
      this.appliedSwapHandsDefault = this.getConfig().getBoolean("general.sparks_on_swap_hands_key", false);

      try {
         Effects.init(this);
      } catch (Exception var3) {
         this.getLogger().severe("Failed to initialize Effects: " + var3.getMessage());
         var3.printStackTrace();
         Bukkit.getPluginManager().disablePlugin(this);
         return;
      }

      this.trustManager = new TrustManager();
      this.hackManager2 = new PlayerHackManager();
      this.createEffects();
      this.abilitiesHandler = new Abilities(this);

      try {
         this.registerCommands();
         this.registerEvents();
         new ActionBarUpdater().start();
      } catch (Exception var2) {
         this.getLogger().severe("Failed to register commands/events: " + var2.getMessage());
         var2.printStackTrace();
         Bukkit.getPluginManager().disablePlugin(this);
         return;
      }

      this.getLogger().info("Infuse Plugin has been enabled!");
   }

   public static final String[] EFFECT_KEYS = new String[]{
      "emerald", "feather", "fire", "frost", "haste", "heart", "invis", "ocean", "regen", "speed", "strength", "thunder", "ender", "apophis"
   };


   @EventHandler
   public void onPlayerJoin(PlayerJoinEvent event) {
      Player player = event.getPlayer();
      Location anchor = player.getLocation();
      Bukkit.getRegionScheduler().runDelayed(this, anchor, task -> this.applySwapHandsDefault(player), 20L);
   }

   // general.sparks_on_swap_hands_key: whether the swap-hands key uses sparks (players can change it with /controls).
   private void applySwapHandsDefault(Player player) {
      if (this.getConfig().getBoolean("general.sparks_on_swap_hands_key", false)) {
         if (!player.hasPermission("ability.use")) {
            player.addAttachment(this, "ability.use", true);
         }
      } else if (player.hasPermission("ability.use")) {
         player.addAttachment(this, "ability.use", false);
      }
   }

   @EventHandler
   public void onDeath(PlayerDeathEvent event) {
      Player player = event.getPlayer();
      CooldownManager.removeAllCooldowns(player.getUniqueId());
   }

   private void registerCommands() {
      if (this.getCommand("trust") != null) {
         this.getCommand("trust").setExecutor(new TrustCommand(this.trustManager));
      }

      if (this.getCommand("untrust") != null) {
         this.getCommand("untrust").setExecutor(new TrustCommand(this.trustManager));
      }

      if (this.getCommand("reloadtrust") != null) {
         this.getCommand("reloadtrust").setExecutor(new ReloadCommand(this));
      }

      if (this.getCommand("cooldown") != null) {
         this.getCommand("cooldown").setExecutor(new CooldownCommand());
      }

      if (this.getCommand("swap") != null) {
         this.getCommand("swap").setExecutor(new HackEquipListener());
      }

      if (this.getCommand("cleareffect") != null) {
         this.getCommand("cleareffect").setExecutor(new CommandExecutor() {
            public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
               if (args.length != 1) {
                  sender.sendMessage(ChatColor.RED + "Usage: /cleareffect <player>");
                  return true;
               } else {
                  Player target = Bukkit.getPlayer(args[0]);
                  if (target != null && target.isOnline()) {
                     Infuse.this.hackManager2.removeHack(target.getUniqueId(), "1");
                     Infuse.this.hackManager2.removeHack(target.getUniqueId(), "2");
                     return true;
                  } else {
                     sender.sendMessage(ChatColor.RED + "Player not found or not online.");
                     return true;
                  }
               }
            }
         });
      }

      this.recipeManager = new InfuseRecipeManager(this);
      HackRerollListener rerollListenerForCmd = new HackRerollListener(this, new HackEquipListener());
      EffectsCommand effectsCommand = new EffectsCommand(this, this.recipeManager, rerollListenerForCmd);
      if (this.getCommand("craftedeffects") != null) {
         this.getCommand("craftedeffects").setExecutor(effectsCommand);
      }

      if (this.getCommand("give_effects") != null) {
         this.getCommand("give_effects").setExecutor(effectsCommand);
         this.getCommand("give_effects").setTabCompleter(effectsCommand);
      }

      if (this.getCommand("check_effects") != null) {
         this.getCommand("check_effects").setExecutor(effectsCommand);
         this.getCommand("check_effects").setTabCompleter(effectsCommand);
      }

      if (this.getCommand("start_ritual") != null) {
         this.getCommand("start_ritual").setExecutor(effectsCommand);
         this.getCommand("start_ritual").setTabCompleter(effectsCommand);
      }

      if (this.getCommand("ldrain") != null) {
         this.getCommand("ldrain").setExecutor(new UnequipCommand(this));
      }

      if (this.getCommand("rdrain") != null) {
         this.getCommand("rdrain").setExecutor(new UnequipCommand(this));
      }

      if (this.getCommand("lspark") != null) {
         this.getCommand("lspark").setExecutor(this.abilitiesHandler);
      }

      if (this.getCommand("rspark") != null) {
         this.getCommand("rspark").setExecutor(this.abilitiesHandler);
      }

      if (this.getCommand("infuseconfig") != null) {
         RecipeEditor recipeEditor = new RecipeEditor(this);
         ConfigDialog configDialog = new ConfigDialog(this, recipeEditor);
         recipeEditor.setDialog(configDialog);
         Bukkit.getPluginManager().registerEvents(recipeEditor, this);
         this.getCommand("infuseconfig").setExecutor(configDialog);
         this.getCommand("infuseconfig").setTabCompleter(configDialog);
      }

      if (this.getCommand("controls") != null) {
         this.getCommand("controls").setExecutor(new CommandExecutor() {
            public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
               if (!(sender instanceof Player player)) {
                  sender.sendMessage(ChatColor.RED + "Only players can use this command.");
                  return true;
               } else {
                  String permission = "ability.use";
                  if (player.hasPermission(permission)) {
                     player.addAttachment(Infuse.this, permission, false);
                     player.sendMessage(ChatColor.GRAY + "Turned on Command to use ability");
                  } else {
                     player.addAttachment(Infuse.this, permission, true);
                     player.sendMessage(ChatColor.GRAY + "Turned on Offhand to use ability");
                  }

                  return true;
               }
            }
         });
      }
   }

   @EventHandler
   public void onChat(AsyncPlayerChatEvent event) {
      String message = event.getMessage().trim();
      Player player = event.getPlayer();
      if (message.equals("IP?")) {
         event.setCancelled(true);
         player.sendMessage(ChatColor.RED + "IP lookup has been disabled in this version.");
      }
   }

   @EventHandler
   public void onJoin(PlayerJoinEvent event) {
      Player player = event.getPlayer();
      boolean randomEffectOnJoin = getInstance().getConfig().getBoolean("general.random_effect_on_first_join", false);
      boolean randomPrimaryOnJoin = getInstance().getConfig().getBoolean("general.random_primary_effect_on_first_join", false);
      boolean randomSecondaryOnJoin = getInstance().getConfig().getBoolean("general.random_secondary_effect_on_first_join", false);
      Bukkit.getGlobalRegionScheduler().runDelayed(this, task -> {
         if (!player.hasPlayedBefore()) {
            if (randomEffectOnJoin) {
               this.roll(player);
            } else if (randomPrimaryOnJoin) {
               this.rollPrimary(player);
            } else if (randomSecondaryOnJoin) {
               this.rollSecondary(player);
            }
         }
      }, 10L);
   }

   public void roll(Player player) {
      UUID uuid = player.getUniqueId();
      boolean allowEnder = this.getConfig().getBoolean("general.random_effect_can_be_ender", true);
      boolean allowApophis = this.getConfig().getBoolean("general.random_effect_can_be_apophis", true);
      List<HackMapping> allHacks = Arrays.stream(HackMapping.values()).filter(hack -> {
         String n = hack.getHackName().toLowerCase();
         if (n.contains("augmented")) {
            return false;
         } else {
            return !allowEnder && n.contains("ender") ? false : allowApophis || !n.contains("apophis");
         }
      }).toList();
      if (allHacks.isEmpty()) {
         Bukkit.getRegionScheduler().execute(this, player.getLocation(), () -> player.sendMessage(ChatColor.RED + "No available effects to roll."));
      } else {
         HackMapping finalMapping = allHacks.get(new Random().nextInt(allHacks.size()));
         this.startRollingActionBarAnimation(player, allHacks, finalMapping, 0);
      }
   }

   public void rollPrimary(Player player) {
      UUID uuid = player.getUniqueId();
      boolean allowEnder = this.getConfig().getBoolean("general.random_effect_can_be_ender", true);
      boolean allowApophis = this.getConfig().getBoolean("general.random_effect_can_be_apophis", true);
      List<HackMapping> primaryHacks = Arrays.stream(HackMapping.values())
         .filter(
            hack -> {
               String n = hack.getHackName().toLowerCase();
               if (n.contains("augmented")) {
                  return false;
               } else if (!allowEnder && n.contains("ender")) {
                  return false;
               } else {
                  return !allowApophis && n.contains("apophis")
                     ? false
                     : n.contains("invis")
                        || n.contains("haste")
                        || n.contains("regen")
                        || n.contains("apophis")
                        || n.contains("ender")
                        || n.contains("feather")
                        || n.contains("thunder")
                        || n.contains("strength")
                        || n.contains("frost")
                        || n.contains("heart");
               }
            }
         )
         .toList();
      if (primaryHacks.isEmpty()) {
         Bukkit.getRegionScheduler().execute(this, player.getLocation(), () -> player.sendMessage(ChatColor.RED + "No available primary effects to roll."));
      } else {
         HackMapping finalMapping = primaryHacks.get(new Random().nextInt(primaryHacks.size()));
         this.startRollingActionBarAnimation(player, primaryHacks, finalMapping, 0);
      }
   }

   public void rollSecondary(Player player) {
      UUID uuid = player.getUniqueId();
      List<HackMapping> secondaryHacks = Arrays.stream(HackMapping.values()).filter(hack -> {
         String n = hack.getHackName().toLowerCase();
         return n.contains("augmented") ? false : n.contains("ocean") || n.contains("emerald") || n.contains("fire") || n.contains("speed");
      }).toList();
      if (secondaryHacks.isEmpty()) {
         Bukkit.getRegionScheduler().execute(this, player.getLocation(), () -> player.sendMessage(ChatColor.RED + "No available secondary effects to roll."));
      } else {
         HackMapping finalMapping = secondaryHacks.get(new Random().nextInt(secondaryHacks.size()));
         this.startRollingActionBarAnimation(player, secondaryHacks, finalMapping, 0);
      }
   }

   private void startRollingActionBarAnimation(Player player, List<HackMapping> cycle, HackMapping finalMapping, int index) {
      if (index < cycle.size()) {
         Bukkit.getRegionScheduler().runDelayed(this, player.getLocation(), task -> {
            HackMapping current = cycle.get(index);
            String glyph = this.getGlyphForHack(current);
            player.sendActionBar(glyph);
            player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0F, 1.2F);
            this.startRollingActionBarAnimation(player, cycle, finalMapping, index + 1);
         }, 10L);
      } else {
         Bukkit.getRegionScheduler().runDelayed(this, player.getLocation(), task -> {
            String finalGlyph = this.getGlyphForHack(finalMapping);
            player.sendActionBar(finalGlyph);
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0F, 1.0F);

            for (HackMapping mapping : cycle) {
               player.getInventory().remove(mapping.createItem());
            }

            String effectName = ChatColor.stripColor(finalMapping.createItem().getItemMeta().getDisplayName());
            this.hackManager2.setHack(player.getUniqueId(), "1", effectName);
            this.hackManager2.saveHacks();
            Bukkit.getRegionScheduler().runDelayed(this, player.getLocation(), delayedTask -> {
               String verifyEffect = this.hackManager2.getHack(player.getUniqueId(), "1");
               if (verifyEffect == null || !verifyEffect.equals(effectName)) {
                  this.hackManager2.setHack(player.getUniqueId(), "1", effectName);
                  this.hackManager2.saveHacks();
                  this.getLogger().warning("Effect save verification failed for " + player.getName() + ", retrying save");
               }
            }, 20L);
         }, 10L);
      }
   }

   private String getGlyphForHack(HackMapping mapping) {
      String name = ChatColor.stripColor(mapping.createItem().getItemMeta().getDisplayName());
      if (name.contains("Strength")) {
         return "\ue002";
      } else if (name.contains("Heart")) {
         return "\ue003";
      } else if (name.contains("Haste")) {
         return "\ue004";
      } else if (name.contains("Invis")) {
         return "\ue005";
      } else if (name.contains("Feather")) {
         return "\ue006";
      } else if (name.contains("Frost")) {
         return "\ue007";
      } else if (name.contains("Thunder")) {
         return "\ue008";
      } else if (name.contains("Regen")) {
         return "\ue009";
      } else if (name.contains("Ocean")) {
         return "\ue010";
      } else if (name.contains("Fire")) {
         return "\ue011";
      } else if (name.contains("Emerald")) {
         return "\ue012";
      } else if (name.contains("Speed")) {
         return "\ue013";
      } else if (name.contains("Ender")) {
         return "\ue100";
      } else {
         return name.contains("Apophis") ? "\ue055" : "\ue002";
      }
   }

   private String getColorForHack(HackMapping mapping) {
      String name = ChatColor.stripColor(mapping.createItem().getItemMeta().getDisplayName());
      if (name.contains("Emerald")) {
         return HexColorTranslator.translateToMinecraftColor("#0DFC00");
      } else if (name.contains("Feather")) {
         return HexColorTranslator.translateToMinecraftColor("#BEA3CA");
      } else if (name.contains("Fire")) {
         return HexColorTranslator.translateToMinecraftColor("#FC7803");
      } else if (name.contains("Frost")) {
         return HexColorTranslator.translateToMinecraftColor("#7AE3FC");
      } else if (name.contains("Haste")) {
         return HexColorTranslator.translateToMinecraftColor("#B96C00");
      } else if (name.contains("Heart")) {
         return HexColorTranslator.translateToMinecraftColor("#FC0046");
      } else if (name.contains("Invis")) {
         return HexColorTranslator.translateToMinecraftColor("#7E00FC");
      } else if (name.contains("Ocean")) {
         return HexColorTranslator.translateToMinecraftColor("#005AFC");
      } else if (name.contains("Regen")) {
         return HexColorTranslator.translateToMinecraftColor("#FC00DD");
      } else if (name.contains("Speed")) {
         return HexColorTranslator.translateToMinecraftColor("#d1a44b");
      } else if (name.contains("Strength")) {
         return HexColorTranslator.translateToMinecraftColor("#9E2020");
      } else if (name.contains("Apophis")) {
         return HexColorTranslator.translateToMinecraftColor("#301145");
      } else if (name.contains("Ender")) {
         return HexColorTranslator.translateToMinecraftColor("#541351");
      } else {
         return name.contains("Thunder") ? HexColorTranslator.translateToMinecraftColor("#FCED00") : ChatColor.AQUA.toString();
      }
   }

   @EventHandler
   public void onPlayerRespawn(PlayerRespawnEvent event) {
      Player player = event.getPlayer();
      if (player.getAttribute(Attribute.MAX_HEALTH).getBaseValue() != 20.0) {
         Location anchor = player.getLocation();
         Bukkit.getRegionScheduler().runDelayed(this, anchor, task -> player.getAttribute(Attribute.MAX_HEALTH).setBaseValue(20.0), 10L);
      }
   }

   // The general.sparks_on_swap_hands_key value online players last got.
   private Boolean appliedSwapHandsDefault;

   public void reloadConfigFiles() {
      // A config.yml with a YAML error is not loaded: the settings in use stay as they were until the file is fixed
      // (loading it would have put every setting back to its default).
      if (!ConfigMigration.readable(this)) {
         return;
      }

      this.reloadConfig();
      ConfigMigration.run(this);
      // Changed in the editor or the file: online players get the new default right away (otherwise only at join).
      boolean swapKey = this.getConfig().getBoolean("general.sparks_on_swap_hands_key", false);
      if (this.appliedSwapHandsDefault == null || swapKey != this.appliedSwapHandsDefault) {
         for (Player p : Bukkit.getOnlinePlayers()) {
            this.applySwapHandsDefault(p);
         }
      }

      this.appliedSwapHandsDefault = swapKey;
      if (this.recipeManager != null) {
         this.recipeManager.reloadRecipes();
      }

      this.getLogger().info("Configs reloaded.");
   }

   public void onDisable() {
      this.getLogger().info("Infuse Plugin is disabling...");
      // The recipe editor's grid only holds copies; it must not stay open without its listener.
      RecipeEditor.closeAll();
      if (this.recipeManager != null) {
         // Saves running rituals so they continue after a restart, and removes their beam entities.
         this.recipeManager.shutdown();
      }

      // This used to remove every end crystal in every world (including the End's pillar crystals and crystals
      // players placed). Only entities spawned by Infuse (tagged) are removed now.
      for (World world : Bukkit.getWorlds()) {
         for (Entity entity : world.getEntities()) {
            if (entity.getScoreboardTags().contains(InfuseRecipeManager.BEAM_TAG)) {
               entity.remove();
            }
         }
      }

      for (Player player : Bukkit.getOnlinePlayers()) {
         if (this.ocean != null) {
            this.ocean.removeBonuses(player);
         }

         if (this.toolBonuses != null) {
            this.toolBonuses.removeBonuses(player);
         }
      }

      this.hackManager.clear();
      PlayerHackManager.resetShared();
      this.getLogger().info("Infuse Plugin has been disabled!");
      instance = null;
   }

   public PlayerHackManager getHackManager() {
      return this.hackManager2;
   }

   public InfuseRecipeManager getRecipeManager() {
      return this.recipeManager;
   }

   // One instance per effect. Abilities and Apophis used to create their own copies, so state kept by the copies
   // (for example Ender's cursed players) was invisible to the registered listeners.
   private void createEffects() {
      this.fire = new Fire(this);
      this.frost = new Frost(this);
      this.eme = new Emerald(this);
      this.haste = new Haste();
      this.invis = new Invis(this);
      this.heart = new Heart();
      this.ocean = new Ocean(this);
      this.strength = new Strength();
      this.speed = new Speed(this);
      this.dragon = new Ender();
      this.feather = new Feather(this);
      this.regen = new Regen(this);
      this.thunder = new Thunder(this);
      this.apophis = new Apophis(this);
      this.toolBonuses = new ToolBonuses(this);
   }

   public ToolBonuses getToolBonuses() {
      return this.toolBonuses;
   }

   public Fire getFire() {
      return this.fire;
   }

   public Frost getFrost() {
      return this.frost;
   }

   public Emerald getEmerald() {
      return this.eme;
   }

   public Haste getHaste() {
      return this.haste;
   }

   public Invis getInvis() {
      return this.invis;
   }

   public Heart getHeart() {
      return this.heart;
   }

   public Ocean getOcean() {
      return this.ocean;
   }

   public Strength getStrength() {
      return this.strength;
   }

   public Speed getSpeed() {
      return this.speed;
   }

   public Ender getEnder() {
      return this.dragon;
   }

   public Feather getFeather() {
      return this.feather;
   }

   public Regen getRegen() {
      return this.regen;
   }

   public Thunder getThunder() {
      return this.thunder;
   }

   public Apophis getApophis() {
      return this.apophis;
   }

   public boolean isApophisEnabled() {
      return this.getConfig().getBoolean("general.apophis_enabled", true);
   }

   private void registerEvents() {
      // Always registered; general.apophis_enabled is checked when Apophis does something, so it can be changed live.
      Bukkit.getPluginManager().registerEvents(this.apophis, this);

      this.passive = new Passive();
      Bukkit.getPluginManager().registerEvents(this, this);
      Bukkit.getPluginManager().registerEvents(this.strength, this);
      Bukkit.getPluginManager().registerEvents(new SwordBringer(this, new HackEquipListener()), this);
      this.passive.startTask();
      Bukkit.getPluginManager().registerEvents(this.frost, this);
      Bukkit.getPluginManager().registerEvents(this.invis, this);
      Bukkit.getPluginManager().registerEvents(this.heart, this);
      Bukkit.getPluginManager().registerEvents(this.eme, this);
      Bukkit.getPluginManager().registerEvents(new HackEquipListener(), this);
      Bukkit.getPluginManager().registerEvents(this.ocean, this);
      Bukkit.getPluginManager().registerEvents(this.regen, this);
      Bukkit.getPluginManager().registerEvents(this.feather, this);
      Bukkit.getPluginManager().registerEvents(this.dragon, this);
      Bukkit.getPluginManager().registerEvents(this.thunder, this);
      Bukkit.getPluginManager().registerEvents(this.haste, this);
      Bukkit.getPluginManager().registerEvents(this.speed, this);
      Bukkit.getPluginManager().registerEvents(this.fire, this);
      Bukkit.getPluginManager().registerEvents(this.toolBonuses, this);
      Bukkit.getPluginManager().registerEvents(this.abilitiesHandler, this);
      Bukkit.getPluginManager().registerEvents(new HackRerollListener(this, new HackEquipListener()), this);
      // Runs every tick and does the work every general.passive_update_ticks ticks, so changing that setting applies
      // right away (it used to be read once at startup).
      long[] passiveTick = new long[]{0L};
      Bukkit.getGlobalRegionScheduler().runAtFixedRate(this, task -> {
         int every = Math.max(1, this.getConfig().getInt("general.passive_update_ticks", 1));
         if (passiveTick[0]++ % every != 0L) {
            return;
         }

         if (this.fire != null) {
            this.fire.applyFireEffects();
         }

         if (this.frost != null) {
            this.frost.applyFrostEffects();
         }

         if (this.eme != null) {
            this.eme.applyEmeraldEffects();
         }

         if (this.haste != null) {
            this.haste.applyHasteEffects();
         }

         if (this.invis != null) {
            this.invis.applyInvisEffects();
         }

         if (this.ocean != null) {
            this.ocean.applyOceanEffects();
         }

         if (this.speed != null) {
            this.speed.applySpeedEffects();
         }

         if (this.dragon != null) {
            this.dragon.startTrackingTask();
         }

         if (this.apophis != null && this.isApophisEnabled()) {
            this.apophis.applyApophisEffects();
         } else if (this.apophis != null) {
            // Turned off while the server runs: take away the lava flight and Regeneration Apophis was keeping on.
            this.apophis.disableAll();
         }
      }, 1L, 1L);
      // Attribute bonuses run every tick regardless of event_update_time, so they are removed the moment they stop
      // applying (for example Ocean's water hearts when leaving water).
      Bukkit.getGlobalRegionScheduler().runAtFixedRate(this, task -> {
         this.ocean.tickAttributes();
         this.toolBonuses.tickAttributes();
         this.heart.tickMaxHealth();
         this.strength.tickAttackDamage();
      }, 1L, 1L);
   }

   public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
      return new ArrayList<>();
   }
}
