package me.poggercity.infuse.Managers;

import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.entity.Player;

/**
 * Keyed, transient attribute modifiers for passive bonuses. Transient modifiers are never written to player data, so a
 * crash, restart or relog can't leave a bonus behind, and removing by key never touches anything another plugin added.
 */
public final class AttributeBonus {
   private AttributeBonus() {
   }

   public static NamespacedKey key(String name) {
      return new NamespacedKey(Infuse.getInstance(), name);
   }

   /** Applies the bonus (amount != 0) or removes it (amount == 0). Only touches the attribute when something changes. */
   public static void set(Player player, Attribute attribute, NamespacedKey key, double amount, Operation operation) {
      AttributeInstance instance = player.getAttribute(attribute);
      if (instance != null) {
         AttributeModifier current = instance.getModifier(key);
         if (amount == 0.0) {
            if (current != null) {
               instance.removeModifier(key);
               if (attribute == Attribute.MAX_HEALTH) {
                  clampHealth(player);
               }
            }
         } else if (current == null || current.getAmount() != amount || current.getOperation() != operation) {
            if (current != null) {
               instance.removeModifier(key);
            }

            instance.addTransientModifier(new AttributeModifier(key, amount, operation));
            if (attribute == Attribute.MAX_HEALTH) {
               clampHealth(player);
            }
         }
      }
   }

   public static void remove(Player player, Attribute attribute, NamespacedKey key) {
      set(player, attribute, key, 0.0, Operation.ADD_NUMBER);
   }

   public static boolean has(Player player, Attribute attribute, NamespacedKey key) {
      AttributeInstance instance = player.getAttribute(attribute);
      return instance != null && instance.getModifier(key) != null;
   }

   /** Health can't stay above the (new) maximum after a max-health bonus is removed. */
   public static void clampHealth(Player player) {
      AttributeInstance max = player.getAttribute(Attribute.MAX_HEALTH);
      if (max != null && !player.isDead() && player.getHealth() > max.getValue()) {
         player.setHealth(max.getValue());
      }
   }
}
