package me.poggercity.infuse.Managers;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class PlayerHackManager {
   private final File file = new File(Infuse.getInstance().getDataFolder(), "player_hacks.yml");
   private FileConfiguration config;
   private long lastModified = 0L;

   public PlayerHackManager() {
      if (!this.file.exists()) {
         try {
            this.file.createNewFile();
         } catch (IOException var2) {
            var2.printStackTrace();
         }
      }

      this.loadConfig();
   }

   private void loadConfig() {
      long fileLastModified = this.file.lastModified();
      if (fileLastModified != this.lastModified || this.config == null) {
         this.config = YamlConfiguration.loadConfiguration(this.file);
         this.lastModified = fileLastModified;
      }
   }

   private void forceLoadConfig() {
      this.config = YamlConfiguration.loadConfiguration(this.file);
      this.lastModified = this.file.lastModified();
   }

   private void saveConfig() {
      try {
         this.config.save(this.file);
      } catch (IOException var2) {
         var2.printStackTrace();
      }
   }

   public void setHack(UUID playerUUID, String type, String hackName) {
      this.forceLoadConfig();
      this.config.set(playerUUID + "." + type, hackName);
      this.saveConfig();
   }

   public String getHack(UUID playerUUID, String type) {
      this.loadConfig();
      return this.config.getString(playerUUID + "." + type, (String)null);
   }

   public void removeHack(UUID playerUUID, String type) {
      this.forceLoadConfig();
      this.config.set(playerUUID + "." + type, null);
      this.saveConfig();
   }

   public void loadHacks() {
      this.loadConfig();
   }

   public void saveHacks() {
      this.saveConfig();
   }
}
