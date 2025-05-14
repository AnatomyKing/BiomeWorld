package net.anatomyworld.biomeWorld.util;

import net.anatomyworld.biomeWorld.MultiBiomeProvider;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.NotNull;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;

import java.util.Locale;

/**
 * Pass-through generator that references a pre-registered noise settings key
 */
public class PassthroughChunkGenerator extends ChunkGenerator {

    private final BiomeProvider biomeProvider;
    private final ResourceKey<NoiseGeneratorSettings> noiseKey;

    public PassthroughChunkGenerator(BiomeProvider biomeProvider, BiomeParameterLoader loader) {
        this.biomeProvider = biomeProvider;
        this.noiseKey = ResourceKey.create(
                net.minecraft.core.registries.Registries.NOISE_SETTINGS,
                ResourceLocation.parse("biomeworld:" + loader.getProfileName().toLowerCase(Locale.ROOT))
        );
    }

    @Override
    public @NotNull BiomeProvider getDefaultBiomeProvider(@NotNull WorldInfo worldInfo) {
        if (biomeProvider instanceof MultiBiomeProvider multi) {
            multi.setSeed(worldInfo.getSeed());
        }
        return biomeProvider;
    }

    public ResourceKey<NoiseGeneratorSettings> getNoiseSettingsKey() {
        return noiseKey;
    }

    @Override public boolean shouldGenerateNoise()        { return true; }
    @Override public boolean shouldGenerateSurface()      { return true; }
    @Override public boolean shouldGenerateCaves()        { return true; }
    @Override public boolean shouldGenerateDecorations()  { return true; }
    @Override public boolean shouldGenerateMobs()         { return true; }
    @Override public boolean shouldGenerateStructures()   { return true; }
}
