package me.poggercity.infuse.Managers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public class TrustManager {
   private final File file = new File(Infuse.getInstance().getDataFolder(), "trusts.yml");
   private FileConfiguration config;

   public TrustManager() {
      if (!this.file.exists()) {
         try {
            this.file.createNewFile();
         } catch (IOException var2) {
            var2.printStackTrace();
         }
      }

      this.config = YamlConfiguration.loadConfiguration(this.file);
   }

   public void reloadConfig() {
      this.config = YamlConfiguration.loadConfiguration(this.file);
   }

   public void trustPlayer(Player owner, Player trusted) {
      Set<String> trustedPlayers = this.getTrustedPlayers(owner);
      trustedPlayers.add(trusted.getUniqueId().toString());
      this.config.set(owner.getUniqueId().toString(), new ArrayList<>(trustedPlayers));
      this.saveConfig();
   }

   public void untrustPlayer(Player owner, Player trusted) {
      Set<String> trustedPlayers = this.getTrustedPlayers(owner);
      trustedPlayers.remove(trusted.getUniqueId().toString());
      this.config.set(owner.getUniqueId().toString(), new ArrayList<>(trustedPlayers));
      this.saveConfig();
   }

   public Set<String> getTrustedPlayers(Player owner) {
      List<String> trustedList = this.config.getStringList(owner.getUniqueId().toString());
      return new HashSet<>(trustedList);
   }

   public boolean isTrusted(Player owner, Player trusted) {
      return this.getTrustedPlayers(owner).contains(trusted.getUniqueId().toString());
   }

   private void saveConfig() {
      try {
         this.config.save(this.file);
      } catch (IOException var2) {
         var2.printStackTrace();
      }
   }
}
