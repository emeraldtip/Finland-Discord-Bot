package me.emerald.finlandbot.utils;

import me.emerald.finlandbot.Main;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


@SuppressWarnings("unchecked")
public class ConfigUtils {
    public static boolean loadConfig() {
        try {
            Yaml yaml = new Yaml();
            InputStream stream = new FileInputStream(Main.jarLoc+"config.yml");
            Main.configuration = yaml.load(stream);
            return true;
        }
        catch (IOException e) {
            System.out.println("Error loading config :3\n"+e);
            return false;
        }
    }


    public static boolean saveConfig() {
        try {
            Yaml yaml = new Yaml();
            FileWriter writer = new FileWriter(Main.jarLoc+"config.yml");
            yaml.dump(Main.configuration,writer);
            writer.close();
            return true;
        }
        catch (IOException e) {
            System.out.println("Error saving config :3\n"+e);
            return false;
        }
    }


    public static Set<String> getServers() {
        Map<String,Object> guilds = (Map<String,Object>)Main.configuration.get("guilds");
        return guilds.keySet();
    }


    public static HashMap<String, String> getServerSettings(String serverID) {
        Map<String,Object> guilds = (Map<String,Object>)Main.configuration.get("guilds");

        for(String guild : guilds.keySet()) {
            if (guild.equals(serverID)) {
                return (HashMap<String,String>) guilds.get(serverID);
            }
        }
        return new HashMap<>();
    }


    public static boolean setServerSetting(String serverID, String setting, String value) {
        Map<String,Object> guilds = (Map<String,Object>)Main.configuration.get("guilds");
        if (guilds == null) {
            guilds = new HashMap<>();
        }

        Map<String, String> settings;

        for(String guild : guilds.keySet()) {
            if (guild.equals(serverID)) {
                settings = (Map<String,String>)guilds.get(serverID);
                settings.put(setting,value);
                guilds.put(serverID,settings);
                Main.configuration.put("guilds",guilds);

                return saveConfig();
            }
        }

        settings = new HashMap<>(){{put(setting,value);}};
        guilds.put(serverID,settings);
        Main.configuration.put("guilds",guilds);

        return saveConfig();
    }

    public static ArrayList<String> getAds() {
        ArrayList<String> ads = (ArrayList<String>) Main.configuration.get("ads");
        if (ads==null) {
            return new ArrayList<>();
        }
        return ads;
    }

    public static boolean addAd(String adHash) {
        ArrayList<String> ads = (ArrayList<String>) Main.configuration.get("ads");
        if (ads==null) {
            ads = new ArrayList<>();
        }
        ads.add(adHash);
        Main.configuration.put("ads",ads);
        return saveConfig();
    }
}
