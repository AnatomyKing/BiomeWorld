package net.anatomyworld.biomeWorld;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.anatomyworld.biomeWorld.util.*;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.biome.Biome;
import org.bukkit.NamespacedKey;
import org.bukkit.command.PluginCommand;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.*;
import java.util.logging.Level;

public class BiomeWorldMain extends JavaPlugin {

    @Override
    public void onLoad() {
        getLogger().info("[BiomeWorld] onLoad — nothing to do (all profiles registered in bootstrap)");
    }


    @Override
    public void onEnable() {
        /* 1 ─ Ensure the default profile exists ------------------------ */
        File profilesDir = new File(getDataFolder(), "profiles");
        if (!profilesDir.exists() && !profilesDir.mkdirs()) {
            getLogger().severe("[BiomeWorld] Cannot create profiles directory!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        File vanilla = new File(profilesDir, "vanilla.yml");
        if (!vanilla.exists()) {
            saveResource("profiles/vanilla.yml", false);
            getLogger().info("[BiomeWorld] Default profile 'vanilla.yml' extracted.");
        }

        /* 2 ─ Manually register /biomeworld ---------------------------- */
        try {
            Constructor<PluginCommand> ctor =
                    PluginCommand.class.getDeclaredConstructor(String.class, Plugin.class);
            ctor.setAccessible(true);

            PluginCommand cmd = ctor.newInstance("biomeworld", this);
            cmd.setDescription("BiomeWorld plugin commands");
            cmd.setUsage("/biomeworld reload <profile>");
            cmd.setPermission("biomeworld.use");

            // register first (so getCommand returns non-null)
            String namespace = getName().toLowerCase(Locale.ROOT);
            getServer().getCommandMap().register(namespace, cmd);

            // hook up executor and tab-completer
            BiomeWorldCommand executor = new BiomeWorldCommand(this);
            cmd.setExecutor(executor);
            cmd.setTabCompleter(executor);

        } catch (ReflectiveOperationException | IllegalStateException e) {
            getLogger().log(Level.SEVERE, "[BiomeWorld] Failed to register /biomeworld command", e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        /* 3 ─ Done ----------------------------------------------------- */
        getLogger().info("[BiomeWorld] Plugin enabled!");
    }



    @Override
    public ChunkGenerator getDefaultWorldGenerator(@NotNull String worldName, @Nullable String id) {
        // === Profile-based ===
        if (id != null && !id.isEmpty() && !id.contains(":") && !id.contains("[")) {
            BiomeParameterLoader profileLoader = new BiomeParameterLoader(this, id);
            List<Holder<Biome>> biomes = getBiomesFromParameters(profileLoader);

            BiomeProvider provider = biomes.isEmpty()
                    ? new SingleBiomeProvider(org.bukkit.block.Biome.PLAINS)
                    : new MultiBiomeProvider(biomes, profileLoader);

            return new PassthroughChunkGenerator(provider, profileLoader);
        }

        // === Fallback ===
        BiomeParameterLoader fallback = new BiomeParameterLoader(this, "vanilla");
        return new PassthroughChunkGenerator(getDefaultBiomeProvider(worldName, id), fallback);
    }


    public BiomeProvider getDefaultBiomeProvider(@NotNull String worldName, @Nullable String id) {
        if (id == null || id.isEmpty()) {
            getLogger().warning("[SBW] No ID provided. Using plains.");
            return new SingleBiomeProvider(org.bukkit.block.Biome.PLAINS);
        }

        // === Profile-based loading ===
        if (!id.contains(":") && !id.contains("[")) {
            getLogger().info("[SBW] Treating ID as profile name: " + id);
            BiomeParameterLoader profileLoader = new BiomeParameterLoader(this, id);
            List<Holder<Biome>> biomes = getBiomesFromParameters(profileLoader);
            if (biomes.isEmpty()) {
                getLogger().warning("[SBW] No valid biomes found in profile: " + id);
                return new SingleBiomeProvider(org.bukkit.block.Biome.PLAINS);
            }
            return new MultiBiomeProvider(biomes, profileLoader);
        }

        // === List of biomes ===
        if (id.contains("[")) {
            List<Holder<Biome>> biomeList = parseBiomeList(id);
            if (!biomeList.isEmpty()) {
                getLogger().info("[SBW] Using MultiBiomeProvider with " + biomeList.size() + " biomes.");
                return new MultiBiomeProvider(biomeList, new BiomeParameterLoader(this, "vanilla")); // fallback
            }

            getLogger().warning("[SBW] No valid biomes parsed from list: " + id);
            return new SingleBiomeProvider(org.bukkit.block.Biome.PLAINS);
        }

        // === Single biome by key ===
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
            getLogger().info("[SBW] Using NMS biome (datapack): " + key);
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

    private List<Holder<Biome>> getBiomesFromParameters(BiomeParameterLoader loader) {
        List<Holder<Biome>> biomes = new ArrayList<>();
        MinecraftServer server = MinecraftServer.getServer();
        var frozen = server.registryAccess();
        var lookup = frozen.lookup(net.minecraft.core.registries.Registries.BIOME);
        if (lookup.isEmpty()) return biomes;

        for (String key : loader.getAllKeys()) {
            try {
                ResourceLocation resLoc = ResourceLocation.parse(key);
                ResourceKey<Biome> biomeKey = ResourceKey.create(net.minecraft.core.registries.Registries.BIOME, resLoc);
                lookup.get().get(biomeKey).ifPresent(biomes::add);
            } catch (Exception e) {
                getLogger().warning("[SBW] Invalid biome in profile: " + key);
            }
        }

        return biomes;
    }
}
