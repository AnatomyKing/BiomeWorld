package net.anatomyworld.biomeWorld.util;

import net.anatomyworld.biomeWorld.MultiBiomeProvider;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.NotNull;

public class PassthroughChunkGenerator extends ChunkGenerator {

    private final BiomeProvider biomeProvider;

    public PassthroughChunkGenerator(BiomeProvider biomeProvider) {
        this.biomeProvider = biomeProvider;
    }

    @Override
    public @NotNull BiomeProvider getDefaultBiomeProvider(@NotNull WorldInfo worldInfo) {
        if (biomeProvider instanceof MultiBiomeProvider multi) {
            multi.setSeed(worldInfo.getSeed()); // ✅ Pass world seed!
        }
        return biomeProvider;
    }

    @Override public boolean shouldGenerateNoise() { return true; }
    @Override public boolean shouldGenerateSurface() { return true; }
    @Override public boolean shouldGenerateCaves() { return true; }
    @Override public boolean shouldGenerateDecorations() { return true; }
    @Override public boolean shouldGenerateMobs() { return true; }
    @Override public boolean shouldGenerateStructures() { return true; }
}
