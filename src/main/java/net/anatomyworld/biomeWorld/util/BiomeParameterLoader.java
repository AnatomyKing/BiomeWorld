package net.anatomyworld.biomeWorld.util;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class BiomeParameterLoader {

    private final Plugin plugin;
    private final Map<String, Map<String, Double>> biomeParameters = new HashMap<>();
    private double scale = 1.0;

    public BiomeParameterLoader(Plugin plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        File file = new File(plugin.getDataFolder(), "biome-parameters.yml");
        if (!file.exists()) {
            plugin.saveResource("biome-parameters.yml", false);
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        this.scale = config.getDouble("scale", 1.0);

        ConfigurationSection biomesSection = config.getConfigurationSection("biomes");
        if (biomesSection != null) {
            for (String biomeKey : biomesSection.getKeys(false)) {
                ConfigurationSection biomeSection = biomesSection.getConfigurationSection(biomeKey);
                if (biomeSection == null) continue;

                Map<String, Double> params = new HashMap<>();
                for (String param : biomeSection.getKeys(false)) {
                    params.put(param, biomeSection.getDouble(param));
                }
                biomeParameters.put(biomeKey, params);
            }
        }

        plugin.getLogger().info("[BiomeWorld] Loaded biome-parameters.yml with scale: " + scale);
    }

    public Map<String, Double> getParameters(String biomeKey) {
        return biomeParameters.getOrDefault(biomeKey, new HashMap<>());
    }

    public double getScale() {
        return this.scale;
    }
}
