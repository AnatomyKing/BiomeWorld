package net.anatomyworld.biomeWorld;

import net.minecraft.world.level.biome.Biome;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class NmsBiomeProvider extends BiomeProvider {

    private final Biome biome;

    public NmsBiomeProvider(Biome biome) {
        this.biome = biome;
    }

    @Override
    public @NotNull org.bukkit.block.Biome getBiome(@NotNull WorldInfo worldInfo, int x, int y, int z) {
        // You can’t directly map NMS -> Bukkit Biome anymore.
        // But this provider bypasses Bukkit system using injected biome.
        return org.bukkit.block.Biome.PLAINS;
    }

    @Override
    public @NotNull List<org.bukkit.block.Biome> getBiomes(@NotNull WorldInfo worldInfo) {
        return Collections.singletonList(org.bukkit.block.Biome.PLAINS);
    }

    public Biome getBiomeBase() {
        return biome;
    }
}
