package me.poggercity.infuse.IMP;

import me.poggercity.infuse.Infuses.Apophis;
import me.poggercity.infuse.Infuses.Augmented;
import me.poggercity.infuse.Infuses.Effects;
import me.poggercity.infuse.Infuses.Emerald;
import me.poggercity.infuse.Infuses.Ender;
import me.poggercity.infuse.Infuses.Feather;
import me.poggercity.infuse.Infuses.Fire;
import me.poggercity.infuse.Infuses.Frost;
import me.poggercity.infuse.Infuses.Haste;
import me.poggercity.infuse.Infuses.Heart;
import me.poggercity.infuse.Infuses.Invis;
import me.poggercity.infuse.Infuses.Ocean;
import me.poggercity.infuse.Infuses.Regen;
import me.poggercity.infuse.Infuses.Speed;
import me.poggercity.infuse.Infuses.Strength;
import me.poggercity.infuse.Infuses.Thunder;
import org.bukkit.inventory.ItemStack;

public enum HackMapping {
   IMMORTAL("Strength Effect") {
      @Override
      public boolean matchesItem(ItemStack item) {
         return Strength.isStealthGem(item);
      }

      @Override
      public ItemStack createItem() {
         return Effects.createST();
      }
   },
   VIRUS("Heart Effect") {
      @Override
      public boolean matchesItem(ItemStack item) {
         return Heart.isVirusGlitchGem(item);
      }

      @Override
      public ItemStack createItem() {
         return Effects.createHEART();
      }
   },
   POT("Regen Effect") {
      @Override
      public boolean matchesItem(ItemStack item) {
         return Regen.isInvincibilityGem(item);
      }

      @Override
      public ItemStack createItem() {
         return Effects.createREGEN();
      }
   },
   FAKE("Invis Effect") {
      @Override
      public boolean matchesItem(ItemStack item) {
         return Invis.isStealthGem(item);
      }

      @Override
      public ItemStack createItem() {
         return Effects.createINVIS();
      }
   },
   THUNDER2("Thunder Effect") {
      @Override
      public boolean matchesItem(ItemStack item) {
         return Thunder.isInvincibilityGem(item);
      }

      @Override
      public ItemStack createItem() {
         return Effects.createTHUNDER();
      }
   },
   GLIDE("Emerald Effect") {
      @Override
      public boolean matchesItem(ItemStack item) {
         return Emerald.isInvincibilityGem(item);
      }

      @Override
      public ItemStack createItem() {
         return Effects.createEME();
      }
   },
   TELE("Speed Effect") {
      @Override
      public boolean matchesItem(ItemStack item) {
         return Speed.isInvincibilityGem(item);
      }

      @Override
      public ItemStack createItem() {
         return Effects.createSPEED();
      }
   },
   HEART("Haste Effect") {
      @Override
      public boolean matchesItem(ItemStack item) {
         return Haste.isInvincibilityGem(item);
      }

      @Override
      public ItemStack createItem() {
         return Effects.createHASTE();
      }
   },
   TNT("Feather Effect") {
      @Override
      public boolean matchesItem(ItemStack item) {
         return Feather.isInvincibilityGem(item);
      }

      @Override
      public ItemStack createItem() {
         return Effects.createFEATHER();
      }
   },
   re("Ocean Effect") {
      @Override
      public boolean matchesItem(ItemStack item) {
         return Ocean.isInventoryGlitchGem(item);
      }

      @Override
      public ItemStack createItem() {
         return Effects.createOCEAN();
      }
   },
   FROST("Frost Effect") {
      @Override
      public boolean matchesItem(ItemStack item) {
         return Frost.isStealthGem(item);
      }

      @Override
      public ItemStack createItem() {
         return Effects.createFROST();
      }
   },
   DREAM("Fire Effect") {
      @Override
      public boolean matchesItem(ItemStack item) {
         return Fire.isStrengthGem(item);
      }

      @Override
      public ItemStack createItem() {
         return Effects.createFIRE();
      }
   },
   DRAGON("Ender Effect") {
      @Override
      public boolean matchesItem(ItemStack item) {
         return Ender.isStrengthGem(item);
      }

      @Override
      public ItemStack createItem() {
         return Effects.createDRAGON();
      }
   },
   IMMORTAL1("Augmented Strength Effect") {
      @Override
      public boolean matchesItem(ItemStack item) {
         return Augmented.ISST(item);
      }

      @Override
      public ItemStack createItem() {
         return Augmented.createST();
      }
   },
   VIRUS1("Augmented Heart Effect") {
      @Override
      public boolean matchesItem(ItemStack item) {
         return Augmented.ISHEART(item);
      }

      @Override
      public ItemStack createItem() {
         return Augmented.createHEART();
      }
   },
   POT1("Augmented Regen Effect") {
      @Override
      public boolean matchesItem(ItemStack item) {
         return Augmented.ISREGEN(item);
      }

      @Override
      public ItemStack createItem() {
         return Augmented.createREGEN();
      }
   },
   FAKE1("Augmented Invis Effect") {
      @Override
      public boolean matchesItem(ItemStack item) {
         return Augmented.ISINVIS(item);
      }

      @Override
      public ItemStack createItem() {
         return Augmented.createINVIS();
      }
   },
   GLIDE1("Augmented Emerald Effect") {
      @Override
      public boolean matchesItem(ItemStack item) {
         return Augmented.ISEME(item);
      }

      @Override
      public ItemStack createItem() {
         return Augmented.createEME();
      }
   },
   TELE1("Augmented Speed Effect") {
      @Override
      public boolean matchesItem(ItemStack item) {
         return Augmented.ISSPEED(item);
      }

      @Override
      public ItemStack createItem() {
         return Augmented.createSPEED();
      }
   },
   HEART1("Augmented Haste Effect") {
      @Override
      public boolean matchesItem(ItemStack item) {
         return Augmented.ISHASTE(item);
      }

      @Override
      public ItemStack createItem() {
         return Augmented.createHASTE();
      }
   },
   TNT1("Augmented Feather Effect") {
      @Override
      public boolean matchesItem(ItemStack item) {
         return Augmented.ISFEATHER(item);
      }

      @Override
      public ItemStack createItem() {
         return Augmented.createFEATHER();
      }
   },
   OCEAN1("Augmented Ocean Effect") {
      @Override
      public boolean matchesItem(ItemStack item) {
         return Augmented.ISOCEAN(item);
      }

      @Override
      public ItemStack createItem() {
         return Augmented.createOCEAN();
      }
   },
   FROST1("Augmented Frost Effect") {
      @Override
      public boolean matchesItem(ItemStack item) {
         return Augmented.ISFROST(item);
      }

      @Override
      public ItemStack createItem() {
         return Augmented.createFROST();
      }
   },
   THUNDER("Augmented Thunder Effect") {
      @Override
      public boolean matchesItem(ItemStack item) {
         return Augmented.ISTHUNDER(item);
      }

      @Override
      public ItemStack createItem() {
         return Augmented.createTHUNDER();
      }
   },
   DREAM1("Augmented Fire Effect") {
      @Override
      public boolean matchesItem(ItemStack item) {
         return Augmented.ISFIRE(item);
      }

      @Override
      public ItemStack createItem() {
         return Augmented.createFIRE();
      }
   },
   APOPHIS("Apophis Effect") {
      @Override
      public boolean matchesItem(ItemStack item) {
         return Apophis.isApophisGem(item);
      }

      @Override
      public ItemStack createItem() {
         return Effects.createAPOPHIS();
      }
   },
   APOPHIS1("Augmented Apophis Effect") {
      @Override
      public boolean matchesItem(ItemStack item) {
         return Augmented.ISAPOPHIS(item);
      }

      @Override
      public ItemStack createItem() {
         return Augmented.createAPOPHIS();
      }
   };

   private final String hackName;

   private HackMapping(String hackName) {
      this.hackName = hackName;
   }

   public String getHackName() {
      return this.hackName;
   }

   public abstract boolean matchesItem(ItemStack var1);

   public abstract ItemStack createItem();

   public static HackMapping fromItem(ItemStack item) {
      for (HackMapping mapping : values()) {
         if (mapping.matchesItem(item)) {
            return mapping;
         }
      }

      return null;
   }

   public static HackMapping fromHackName(String name) {
      for (HackMapping mapping : values()) {
         if (mapping.getHackName().equals(name)) {
            return mapping;
         }
      }

      return null;
   }

   private static HackMapping[] $values() {
      return new HackMapping[]{
         IMMORTAL,
         VIRUS,
         POT,
         FAKE,
         THUNDER2,
         GLIDE,
         TELE,
         HEART,
         TNT,
         re,
         FROST,
         DREAM,
         DRAGON,
         IMMORTAL1,
         VIRUS1,
         POT1,
         FAKE1,
         GLIDE1,
         TELE1,
         HEART1,
         TNT1,
         OCEAN1,
         FROST1,
         THUNDER,
         DREAM1,
         APOPHIS,
         APOPHIS1
      };
   }
}
