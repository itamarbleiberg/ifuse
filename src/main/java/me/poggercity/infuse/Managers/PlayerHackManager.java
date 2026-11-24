package me.poggercity.infuse.Managers;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Stores which effect each player has in slot "1" and "2" (player_hacks.yml).
 *
 * Every class used to create its own instance with its own copy of the file, re-checking the file's modification time
 * on every single lookup (several times per tick per player). Two writes in the same millisecond could leave the other
 * copies stale, so a player could keep an effect that had already been removed. All instances now share one in-memory
 * copy. The file is still re-read when someone edits it by hand, but that check runs at most once per server tick.
 */
public class PlayerHackManager {
   private static final Object LOCK = new Object();
   private static File file;
   private static YamlConfiguration config;
   private static long lastModified = -1L;
   private static int lastCheckedTick = Integer.MIN_VALUE;

   public PlayerHackManager() {
      synchronized (LOCK) {
         if (file == null) {
            file = new File(Infuse.getInstance().getDataFolder(), "player_hacks.yml");
            if (!file.exists()) {
               try {
                  file.getParentFile().mkdirs();
                  file.createNewFile();
               } catch (IOException var4) {
                  var4.printStackTrace();
               }
            }

            reload();
         }
      }
   }

   private static void reload() {
      config = YamlConfiguration.loadConfiguration(file);
      lastModified = file.lastModified();
   }

   private static void reloadIfEditedOnDisk() {
      int tick = Bukkit.getCurrentTick();
      if (tick != lastCheckedTick) {
         lastCheckedTick = tick;
         if (file.lastModified() != lastModified) {
            reload();
         }
      }
   }

   private static void save() {
      try {
         config.save(file);
      } catch (IOException var1) {
         var1.printStackTrace();
      }

      lastModified = file.lastModified();
   }

   public void setHack(UUID playerUUID, String type, String hackName) {
      synchronized (LOCK) {
         reloadIfEditedOnDisk();
         config.set(playerUUID + "." + type, hackName);
         save();
      }
   }

   public String getHack(UUID playerUUID, String type) {
      synchronized (LOCK) {
         reloadIfEditedOnDisk();
         return config.getString(playerUUID + "." + type, null);
      }
   }

   public void removeHack(UUID playerUUID, String type) {
      synchronized (LOCK) {
         reloadIfEditedOnDisk();
         config.set(playerUUID + "." + type, null);
         save();
      }
   }

   public void loadHacks() {
      synchronized (LOCK) {
         reload();
      }
   }

   public void saveHacks() {
      synchronized (LOCK) {
         save();
      }
   }

   /** Called on plugin disable so a /reload re-reads the file. */
   public static void resetShared() {
      synchronized (LOCK) {
         file = null;
         config = null;
         lastModified = -1L;
         lastCheckedTick = Integer.MIN_VALUE;
      }
   }
}
