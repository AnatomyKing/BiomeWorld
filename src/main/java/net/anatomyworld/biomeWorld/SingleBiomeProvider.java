package net.anatomyworld.biomeWorld;

import org.bukkit.block.Biome;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class SingleBiomeProvider extends org.bukkit.generator.BiomeProvider {

    private final @NotNull Biome biome;

    public SingleBiomeProvider(@NotNull Biome biome) {
        this.biome = biome;
    }

    @Override
    public @NotNull Biome getBiome(@NotNull WorldInfo worldInfo, int x, int y, int z) {
        return biome;
    }

    @Override
    public @NotNull List<Biome> getBiomes(@NotNull WorldInfo worldInfo) {
        return Collections.singletonList(biome);
    }
}
