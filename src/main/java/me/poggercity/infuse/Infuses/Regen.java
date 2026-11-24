package me.poggercity.infuse.Infuses;

import java.util.UUID;
import me.poggercity.infuse.Managers.CooldownManager;
import me.poggercity.infuse.Managers.Infuse;
import me.poggercity.infuse.Managers.PlayerHackManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class Regen implements Listener {
   private static final String GEM_NAME = ChatColor.LIGHT_PURPLE + "Regen Effect";
   private static final String SPARK_COOLDOWN_KEY = "regen";
   private final PlayerHackManager hackManager = new PlayerHackManager();
   private final Plugin plugin;

   public Regen(Plugin plugin) {
      this.plugin = plugin;
   }

   public static boolean isInvincibilityGem(ItemStack item) {
      return item != null && item.getType() == Material.POTION && item.getItemMeta() != null
         ? item.getItemMeta().hasCustomModelData() && item.getItemMeta().getCustomModelData() == 9
         : false;
   }

   private boolean hasImmortalHackEquipped(Player player, String tier) {
      String currentHack = Infuse.getInstance().getHackManager().getHack(player.getUniqueId(), tier);
      return currentHack != null && (currentHack.equalsIgnoreCase(ChatColor.stripColor(GEM_NAME)) || currentHack.equals("Augmented Regen Effect"));
   }

   @EventHandler
   public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
      if (event.getDamager() instanceof Player player && (this.hasImmortalHackEquipped(player, "1") || this.hasImmortalHackEquipped(player, "2"))) {
         Location anchor = player.getLocation();
         Bukkit.getRegionScheduler()
            .run(Infuse.getInstance(), anchor, task -> player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 40, 1, false, false)));
      }
   }

   @EventHandler
   public void onEntityDamageByEntityHeal(EntityDamageByEntityEvent event) {
      if (event.getEntity() instanceof Player) {
         Player player = (Player)event.getEntity();
         if ((this.hasImmortalHackEquipped(player, "1") || this.hasImmortalHackEquipped(player, "2")) && event.getFinalDamage() <= 0.0) {
            player.setSaturation(Math.min(player.getSaturation() + 6.0F, 20.0F));
         }
      }
   }

   public void activateSpark(Player player) {
      UUID playerUUID = player.getUniqueId();
      if (!CooldownManager.isOnCooldown(playerUUID, "regen")) {
         int duration = Infuse.getInstance().getConfig().getInt("durations.regen_spark", 30);
         int configcooldown = Infuse.getInstance().getConfig().getInt("cooldowns.regen_spark", 60);
         particles.spawnEffect("regen", player, Color.fromRGB(255, 3, 239));
         player.playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, 1.0F, 1.0F);
         boolean isAugmented = Infuse.getInstance().getHackManager().getHack(playerUUID, "1") != null
               && ChatColor.stripColor(Infuse.getInstance().getHackManager().getHack(playerUUID, "1")).toLowerCase().contains("augmented regen")
            || Infuse.getInstance().getHackManager().getHack(playerUUID, "2") != null
               && ChatColor.stripColor(Infuse.getInstance().getHackManager().getHack(playerUUID, "2")).toLowerCase().contains("augmented regen");
         int cooldown = isAugmented ? configcooldown / 2 + duration : configcooldown + duration;
         CooldownManager.setCooldown(playerUUID, "regen", cooldown);
         CooldownManager.setDuration(playerUUID, "regen", duration);
      }
   }

   @EventHandler
   public void onEntityDamageByPlayer(EntityDamageByEntityEvent event) {
      if (event.getDamager() instanceof Player) {
         Player damager = (Player)event.getDamager();
         if (CooldownManager.isEffectActive(damager.getUniqueId(), "regen")) {
            double damage = event.getFinalDamage();
            double healAmount = damage / 2.0;
            double maxHealth = damager.getAttribute(Attribute.MAX_HEALTH).getValue();
            damager.setHealth(Math.min(damager.getHealth() + healAmount, maxHealth));

            for (Player nearbyPlayer : damager.getWorld().getPlayers()) {
               if (!nearbyPlayer.equals(damager)
                  && nearbyPlayer.getLocation().distance(damager.getLocation()) <= 5.0
                  && Infuse.getInstance().getTrustManager().isTrusted(damager, nearbyPlayer)) {
                  double allyMaxHealth = nearbyPlayer.getAttribute(Attribute.MAX_HEALTH).getValue();
                  nearbyPlayer.setHealth(Math.min(nearbyPlayer.getHealth() + healAmount, allyMaxHealth));
               }
            }
         }
      }
   }

   @EventHandler
   public void onConsume(PlayerItemConsumeEvent event) {
      Player player = event.getPlayer();
      if (this.hasImmortalHackEquipped(player, "1") || this.hasImmortalHackEquipped(player, "2")) {
         player.setSaturation(Math.min(player.getSaturation() + 6.0F, 20.0F));
      }
   }
}
