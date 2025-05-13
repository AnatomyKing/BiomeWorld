package net.anatomyworld.biomeWorld;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.anatomyworld.biomeWorld.util.BiomeParameterLoader;
import net.anatomyworld.biomeWorld.util.BiomeWorldCommand;
import net.anatomyworld.biomeWorld.util.PassthroughChunkGenerator;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.biome.Biome;
import org.bukkit.NamespacedKey;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.logging.Level;

public class BiomeWorldMain extends JavaPlugin {

    private BiomeParameterLoader biomeParameterLoader;

    @Override
    public void onEnable() {
        biomeParameterLoader = new BiomeParameterLoader(this);

        // Register command
        Objects.requireNonNull(getCommand("biomeworld")).setExecutor(new BiomeWorldCommand(this));

        getLogger().info("[BiomeWorld] Plugin enabled!");
    }

    public BiomeParameterLoader getBiomeParameterLoader() {
        return biomeParameterLoader;
    }

    @Override
    public ChunkGenerator getDefaultWorldGenerator(@NotNull String worldName, @Nullable String id) {
        return new PassthroughChunkGenerator(getDefaultBiomeProvider(worldName, id));
    }

    public BiomeProvider getDefaultBiomeProvider(@NotNull String worldName, String id) {
        if (id == null || id.isEmpty()) {
            getLogger().warning("[SBW] Biome ID is null or empty, using plains.");
            return new SingleBiomeProvider(org.bukkit.block.Biome.PLAINS);
        }

        if (id.contains("[")) {
            List<Holder<Biome>> biomeList = parseBiomeList(id);
            if (!biomeList.isEmpty()) {
                getLogger().info("[SBW] Using MultiBiomeProvider with " + biomeList.size() + " biomes.");
                return new MultiBiomeProvider(biomeList, getBiomeParameterLoader());
            }

            getLogger().warning("[SBW] No valid biomes parsed from list: " + id);
            return new SingleBiomeProvider(org.bukkit.block.Biome.PLAINS);
        }

        NamespacedKey key = NamespacedKey.fromString(id);
        if (key == null) {
            getLogger().warning("[SBW] Invalid biome key: " + id);
            return new SingleBiomeProvider(org.bukkit.block.Biome.PLAINS);
        }

        Optional<org.bukkit.block.Biome> bukkit = getBukkitBiome(key);
        if (bukkit.isPresent()) {
            getLogger().info("[SBW] Using Bukkit biome: " + key);
            return new SingleBiomeProvider(bukkit.get());
        }

        Optional<Biome> nms = getNmsBiome(key);
        if (nms.isPresent()) {
            getLogger().info("[SBW] Using datapack (NMS) biome: " + key);
            return new NmsBiomeProvider(nms.get());
        }

        getLogger().warning("[SBW] Biome not found, falling back to plains: " + key);
        return new SingleBiomeProvider(org.bukkit.block.Biome.PLAINS);
    }

    private Optional<org.bukkit.block.Biome> getBukkitBiome(NamespacedKey key) {
        try {
            var registry = RegistryAccess.registryAccess().getRegistry(RegistryKey.BIOME);
            return Optional.ofNullable(registry.get(key));
        } catch (Exception e) {
            getLogger().log(Level.WARNING, "[SBW] Bukkit registry access failed: " + key, e);
            return Optional.empty();
        }
    }

    private Optional<Biome> getNmsBiome(NamespacedKey key) {
        try {
            MinecraftServer server = MinecraftServer.getServer();
            var frozen = server.registryAccess();
            var lookup = frozen.lookup(net.minecraft.core.registries.Registries.BIOME);
            if (lookup.isEmpty()) return Optional.empty();

            ResourceLocation resLoc = ResourceLocation.parse(key.toString());
            ResourceKey<Biome> biomeKey = ResourceKey.create(net.minecraft.core.registries.Registries.BIOME, resLoc);

            return lookup.get().get(biomeKey).map(Holder.Reference::value);
        } catch (Exception e) {
            getLogger().log(Level.WARNING, "[SBW] NMS biome lookup failed: " + key, e);
            return Optional.empty();
        }
    }

    private List<Holder<Biome>> parseBiomeList(String id) {
        try {
            int start = id.indexOf('[');
            int end = id.indexOf(']');
            if (start == -1 || end == -1 || end <= start) return List.of();

            String inner = id.substring(start + 1, end);
            String[] parts = inner.split(",");

            MinecraftServer server = MinecraftServer.getServer();
            var frozen = server.registryAccess();
            var lookup = frozen.lookup(net.minecraft.core.registries.Registries.BIOME);
            if (lookup.isEmpty()) return List.of();

            List<Holder<Biome>> result = new ArrayList<>();
            for (String part : parts) {
                String trimmed = part.trim();
                ResourceLocation resLoc = ResourceLocation.parse(trimmed);
                ResourceKey<Biome> biomeKey = ResourceKey.create(net.minecraft.core.registries.Registries.BIOME, resLoc);
                lookup.get().get(biomeKey).ifPresent(result::add);
            }

            return result;
        } catch (Exception e) {
            getLogger().log(Level.WARNING, "[SBW] Failed to parse biome list from ID: " + id, e);
            return List.of();
        }
    }
}
