package me.poggercity.infuse.Infuses;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import me.poggercity.infuse.Managers.CooldownManager;
import me.poggercity.infuse.Managers.Infuse;
import me.poggercity.infuse.Managers.PlayerHackManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class Frost implements Listener {
   private final Set<UUID> frozenAttackers = new HashSet<>();
   private static final String GEM_NAME = ChatColor.AQUA + "Frost Effect";
   private static final String IMMORTAL_KEY = "frost";
   private final PlayerHackManager hackManager = new PlayerHackManager();
   private final Plugin plugin;
   private final Map<UUID, Integer> meleeHitCounter = new HashMap<>();
   private static final Set<Material> ICE_BLOCKS = EnumSet.of(Material.ICE, Material.SNOW_BLOCK, Material.PACKED_ICE, Material.BLUE_ICE);
   double radius = Infuse.getInstance().getConfig().getDouble("radius.frost", 5.0);

   public Frost(Plugin plugin) {
      this.plugin = plugin;
   }

   public void applyFrostEffects() {
      Bukkit.getOnlinePlayers()
         .stream()
         .filter(player -> this.hasImmortalHackEquipped(player) && player.getVelocity().lengthSquared() >= 0.01)
         .forEach(
            player -> {
               Material blockBelow = player.getLocation().clone().subtract(0.0, 1.0, 0.0).getBlock().getType();
               if (ICE_BLOCKS.contains(blockBelow)) {
                  Location anchor = player.getLocation();
                  Bukkit.getRegionScheduler()
                     .run(Infuse.getInstance(), anchor, task -> player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, 2, false, false, false)));
               }
            }
         );
   }

   public static boolean isStealthGem(ItemStack item) {
      return item != null && item.getType() == Material.POTION && item.getItemMeta() != null
         ? item.getItemMeta().hasCustomModelData() && item.getItemMeta().getCustomModelData() == 4
         : false;
   }

   @EventHandler
   public void onPlayerInteractWithWindCharge(PlayerInteractEvent event) {
      Player player = event.getPlayer();
      ItemStack item = player.getInventory().getItemInMainHand();
      if (item != null
         && item.getType() == Material.WIND_CHARGE
         && (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)
         && !player.hasCooldown(Material.WIND_CHARGE)
         && player.getFreezeTicks() > 1) {
         Bukkit.getGlobalRegionScheduler().runDelayed(Infuse.getInstance(), task -> player.setCooldown(Material.WIND_CHARGE, 60), 1L);
      }
   }

   private boolean hasImmortalHackEquipped(Player player) {
      String hack1 = Infuse.getInstance().getHackManager().getHack(player.getUniqueId(), "1");
      String hack2 = Infuse.getInstance().getHackManager().getHack(player.getUniqueId(), "2");
      return hack1 != null && (hack1.equalsIgnoreCase(ChatColor.stripColor(GEM_NAME)) || hack1.equals("Augmented Frost Effect"))
         || hack2 != null && (hack2.equalsIgnoreCase(ChatColor.stripColor(GEM_NAME)) || hack2.equals("Augmented Frost Effect"));
   }

   @EventHandler
   public void onMeleeHit(EntityDamageByEntityEvent event) {
      if (event.getDamager() instanceof Player attacker && event.getEntity() instanceof LivingEntity target && this.hasImmortalHackEquipped(attacker)) {
         if (target instanceof Player playerTarget) {
            if (Infuse.getInstance().getTrustManager().isTrusted(attacker, playerTarget)) {
               return;
            }

            int count = this.meleeHitCounter.getOrDefault(attacker.getUniqueId(), 0) + 1;
            this.meleeHitCounter.put(attacker.getUniqueId(), count);
            if (count >= 10) {
               this.meleeHitCounter.put(attacker.getUniqueId(), 0);
               playerTarget.setFreezeTicks(200);
            }
         } else {
            target.setFreezeTicks(200);
         }
      }
   }

   public void activateSpark(Player caster) {
      UUID playerUUID = caster.getUniqueId();
      if (!CooldownManager.isOnCooldown(playerUUID, "frost")) {
         caster.getWorld().spawnParticle(Particle.SNOWFLAKE, caster.getLocation().add(0.0, 1.0, 0.0), 25, 0.5, 0.5, 0.5, 0.1);
         caster.playSound(caster.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1.0F, 2.0F);
         int duration = Infuse.getInstance().getConfig().getInt("durations.frost_spark", 10);
         Location anchor = caster.getLocation();
         CooldownManager.setDuration(playerUUID, "frost", duration);
         int configcooldown = Infuse.getInstance().getConfig().getInt("cooldowns.frost_spark", 90);
         boolean isAugmented = Infuse.getInstance().getHackManager().getHack(playerUUID, "1") != null
               && ChatColor.stripColor(Infuse.getInstance().getHackManager().getHack(playerUUID, "1")).toLowerCase().contains("augmented frost")
            || Infuse.getInstance().getHackManager().getHack(playerUUID, "2") != null
               && ChatColor.stripColor(Infuse.getInstance().getHackManager().getHack(playerUUID, "2")).toLowerCase().contains("augmented frost");
         int cooldown = isAugmented ? configcooldown / 2 + duration : configcooldown + duration;
         CooldownManager.setCooldown(playerUUID, "frost", cooldown);
         Location center = caster.getLocation();
         World world = caster.getWorld();
         Set<Player> affectedPlayers = new HashSet<>();

         for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.equals(caster)
               && !Infuse.getInstance().getTrustManager().isTrusted(player, caster)
               && player.getWorld().equals(world)
               && player.getLocation().distance(center) <= this.radius) {
               affectedPlayers.add(player);
               AttributeInstance jumpAttribute = player.getAttribute(Attribute.GENERIC_JUMP_STRENGTH);
               if (jumpAttribute != null) {
                  jumpAttribute.setBaseValue(0.0);
               }
            }
         }

         this.frozenAttackers.add(caster.getUniqueId());
         Bukkit.getRegionScheduler().runDelayed(this.plugin, anchor, task -> {
            for (Player playerx : affectedPlayers) {
               AttributeInstance jumpAttributex = playerx.getAttribute(Attribute.GENERIC_JUMP_STRENGTH);
               if (jumpAttributex != null) {
                  jumpAttributex.setBaseValue(0.42);
               }
            }

            this.frozenAttackers.remove(caster.getUniqueId());
         }, duration * 20L);
      }
   }

   @EventHandler
   public void onPlayerJoin(PlayerJoinEvent event) {
      Player player = event.getPlayer();
      AttributeInstance jumpAttribute = player.getAttribute(Attribute.GENERIC_JUMP_STRENGTH);
      if (jumpAttribute != null && jumpAttribute.getBaseValue() == 0.0) {
         jumpAttribute.setBaseValue(0.42);
      }
   }

   @EventHandler
   public void onPlayerAttack(EntityDamageByEntityEvent event) {
      if (event.getDamager() instanceof Player && event.getEntity() instanceof Player) {
         Player attacker = (Player)event.getDamager();
         Player target = (Player)event.getEntity();
         if (CooldownManager.isEffectActive(attacker.getUniqueId(), "frost") && !Infuse.getInstance().getTrustManager().isTrusted(attacker, target)) {
            target.setFreezeTicks(200);
         }
      }
   }
}
