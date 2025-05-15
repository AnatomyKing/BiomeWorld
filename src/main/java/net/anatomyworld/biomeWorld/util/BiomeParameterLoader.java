package net.anatomyworld.biomeWorld.util;

import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.*;
import java.util.function.Consumer;

public class BiomeParameterLoader {

    private final String profileName;
    private final Map<String, Map<String, Double>> biomeParameters = new HashMap<>();

    private double scale = 1.0;
    private int seaLevel = 63;

    // Plugin-based constructor (used at runtime)
    public BiomeParameterLoader(Plugin plugin, String profileName) {
        this.profileName = profileName;

        File file = new File(plugin.getDataFolder(), "profiles/" + profileName + ".yml");
        if (!file.exists()) {
            plugin.getLogger().warning("[BiomeWorld] Profile file not found: " + file.getName());
            return;
        }

        loadConfig(file, plugin.getLogger()::info, plugin.getLogger()::warning);
    }

    // Bootstrap-based constructor (used at startup)
    public BiomeParameterLoader(ComponentLogger logger, File profilesDir, String profileName) {
        this.profileName = profileName;

        File file = new File(profilesDir, profileName + ".yml");
        if (!file.exists()) {
            logger.warn("[BiomeWorld] Profile file not found: {}", file.getName());
            return;
        }

        loadConfig(file, logger::info, logger::warn);
    }

    // Shared config loading logic
    private void loadConfig(File file,
                            Consumer<String> infoLogger,
                            Consumer<String> warnLogger) {

        biomeParameters.clear();
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        this.scale = config.getDouble("scale", 1.0);
        this.seaLevel = config.getInt("sea_level", 63);

        ConfigurationSection biomesSection = config.getConfigurationSection("biomes");
        if (biomesSection == null) {
            warnLogger.accept("[BiomeWorld] No 'biomes:' section in " + file.getName());
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

        infoLogger.accept("[BiomeWorld] Loaded profile '" + profileName + "' with " +
                biomeParameters.size() + " biomes (sea level=" + seaLevel + ")");
    }

    public String getProfileName() {
        return profileName;
    }

    public Map<String, Double> getParameters(String biomeKey) {
        return biomeParameters.getOrDefault(biomeKey, Collections.emptyMap());
    }

    public Set<String> getAllKeys() {
        return biomeParameters.keySet();
    }

    public double getScale() {
        return scale;
    }

    public int getSeaLevel() {
        return seaLevel;
    }
}
