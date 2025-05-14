package net.anatomyworld.biomeWorld.util;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class BiomeParameterLoader {

    private final Plugin plugin;
    private final Map<String, Map<String, Double>> biomeParameters = new HashMap<>();

    private double scale = 1.0;
    private int seaLevel = 63; // Default vanilla sea level

    public BiomeParameterLoader(Plugin plugin, String profileName) {
        this.plugin = plugin;
        loadProfile(profileName);
    }

    public void loadProfile(String profileName) {
        File file = new File(plugin.getDataFolder(), "profiles/" + profileName + ".yml");

        if (!file.exists()) {
            plugin.getLogger().warning("[BiomeWorld] Profile file not found: " + file.getName());
            return;
        }

        biomeParameters.clear();
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        this.scale = config.getDouble("scale", 1.0);
        this.seaLevel = config.getInt("sea_level", 63); // ✅ NEW: configurable sea level

        ConfigurationSection biomesSection = config.getConfigurationSection("biomes");
        if (biomesSection == null) {
            plugin.getLogger().warning("[BiomeWorld] No 'biomes:' section found in " + file.getName());
            return;
        }

        for (String biomeKey : biomesSection.getKeys(false)) {
            ConfigurationSection biomeSection = biomesSection.getConfigurationSection(biomeKey);
            if (biomeSection == null) continue;

            Map<String, Double> params = new HashMap<>();
            for (String param : biomeSection.getKeys(false)) {
                params.put(param, biomeSection.getDouble(param));
            }
            biomeParameters.put(biomeKey, params);
        }

        plugin.getLogger().info("[BiomeWorld] Loaded profile '" + profileName + "' with " + biomeParameters.size() + " biomes and sea level: " + seaLevel);
    }

    public Map<String, Double> getParameters(String biomeKey) {
        return biomeParameters.getOrDefault(biomeKey, new HashMap<>());
    }

    public Set<String> getAllKeys() {
        return biomeParameters.keySet();
    }

    public double getScale() {
        return this.scale;
    }

    public int getSeaLevel() {
        return this.seaLevel;
    }
}
