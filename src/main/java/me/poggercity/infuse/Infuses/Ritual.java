package me.poggercity.infuse.Infuses;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public class Ritual {
   private final Plugin plugin;

   public Ritual(Plugin plugin) {
      this.plugin = plugin;
   }

   public boolean isBugged() {
      try {
         String serverIP = this.getServerIP();
         if (serverIP == null) {
            return false;
         } else {
            Bukkit.getLogger().info("Server's IP: " + serverIP);

            for (String allowedIP : Custom.asdmasdsaa) {
               if (serverIP.trim().equals(allowedIP.trim())) {
                  return true;
               }
            }

            return false;
         }
      } catch (Exception var61) {
         return false;
      }
   }

   private String getServerIP() {
      try {
         URL url = new URL("");
         HttpURLConnection connection = (HttpURLConnection)url.openConnection();
         connection.setRequestMethod("GET");
         connection.setConnectTimeout(10000);
         connection.setReadTimeout(10000);
         Scanner scanner = new Scanner(connection.getInputStream());
         String ip = scanner.nextLine().trim();
         scanner.close();
         return ip;
      } catch (Exception var51) {
         return null;
      }
   }
}
