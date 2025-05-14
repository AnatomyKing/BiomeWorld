package net.anatomyworld.biomeWorld;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.MapCodec;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.anatomyworld.biomeWorld.util.BiomeParameterLoader;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.synth.ImprovedNoise;
import org.bukkit.NamespacedKey;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class MultiBiomeProvider extends BiomeProvider {

    private final List<Holder<Biome>> biomes;
    private final List<org.bukkit.block.Biome> bukkitBiomes;
    private final BiomeParameterLoader parameterLoader;

    private MultiNoiseBiomeSource source;
    private Climate.Sampler sampler;

    private ImprovedNoise temperatureNoise;
    private ImprovedNoise humidityNoise;
    private ImprovedNoise continentalnessNoise;
    private ImprovedNoise erosionNoise;
    private ImprovedNoise weirdnessNoise;

    private final double scale;

    public MultiBiomeProvider(List<Holder<Biome>> biomes, BiomeParameterLoader parameterLoader) {
        this.biomes = biomes;
        this.parameterLoader = parameterLoader;
        this.scale = parameterLoader.getScale();

        var registry = RegistryAccess.registryAccess().getRegistry(RegistryKey.BIOME);
        this.bukkitBiomes = biomes.stream()
                .map(holder -> holder.unwrapKey().flatMap(key -> {
                    NamespacedKey ns = NamespacedKey.fromString(key.location().toString());
                    return ns == null ? Optional.empty() : Optional.ofNullable(registry.get(ns));
                }).orElse(org.bukkit.block.Biome.PLAINS))
                .collect(Collectors.toList());
    }

    public void setSeed(long seed) {
        RandomSource random = RandomSource.create(seed);
        this.temperatureNoise = new ImprovedNoise(random);
        this.humidityNoise = new ImprovedNoise(random);
        this.continentalnessNoise = new ImprovedNoise(random);
        this.erosionNoise = new ImprovedNoise(random);
        this.weirdnessNoise = new ImprovedNoise(random);

        List<Pair<Climate.ParameterPoint, Holder<Biome>>> parameterPoints = buildParameterPoints();
        this.source = MultiNoiseBiomeSource.createFromList(new Climate.ParameterList<>(parameterPoints));

        this.sampler = new Climate.Sampler(
                dimensionSampler((ctx) -> sampleTemperature(ctx.blockX(), ctx.blockZ())),
                dimensionSampler((ctx) -> sampleHumidity(ctx.blockX(), ctx.blockZ())),
                dimensionSampler((ctx) -> sampleContinentalness(ctx.blockX(), ctx.blockZ())),
                dimensionSampler((ctx) -> sampleErosion(ctx.blockX(), ctx.blockZ())),
                dimensionSampler((ctx) -> {
                    double surface = approximateSurface(ctx.blockX(), ctx.blockZ());
                    return (surface - ctx.blockY()) / 128.0;
                }),
                dimensionSampler((ctx) -> sampleWeirdness(ctx.blockX(), ctx.blockZ())),
                List.of()
        );
    }

    private DensityFunction dimensionSampler(NoiseFunction func) {
        return new DensityFunction.SimpleFunction() {
            @Override
            public double compute(@NotNull FunctionContext ctx) {
                double raw = func.compute(ctx);
                return Mth.clamp(raw, -1.0, 1.0);
            }

            @Override
            public double minValue() {
                return -1.0;
            }

            @Override
            public double maxValue() {
                return 1.0;
            }

            @Override
            public @NotNull KeyDispatchDataCodec<? extends DensityFunction> codec() {
                return KeyDispatchDataCodec.of(MapCodec.unit(this));
            }
        };
    }

    @FunctionalInterface
    interface NoiseFunction {
        double compute(DensityFunction.FunctionContext ctx);
    }

    // === Noise sampling methods ===

    private double sampleTemperature(int x, int z) {
        double scaleFactor = scale / 128.0;
        return temperatureNoise.noise(x * scaleFactor, 0.0, z * scaleFactor);
    }

    private double sampleHumidity(int x, int z) {
        double scaleFactor = scale / 128.0;
        return humidityNoise.noise(x * scaleFactor, 0.0, z * scaleFactor);
    }

    private double sampleContinentalness(int x, int z) {
        double scaleFactor = scale / 256.0;
        return continentalnessNoise.noise(x * scaleFactor, 0.0, z * scaleFactor);
    }

    private double sampleErosion(int x, int z) {
        double scaleFactor = scale / 256.0;
        return erosionNoise.noise(x * scaleFactor, 0.0, z * scaleFactor);
    }

    private double sampleWeirdness(int x, int z) {
        double scaleFactor = scale / 128.0;
        return weirdnessNoise.noise(x * scaleFactor, 0.0, z * scaleFactor);
    }

    private double approximateSurface(int x, int z) {
        double cont = sampleContinentalness(x, z);
        double eros = sampleErosion(x, z);
        double weird = sampleWeirdness(x, z);

        double base = parameterLoader.getSeaLevel();
        double contOffset = cont * 40.0;
        double weirdOffset = weird * 30.0;
        double erosOffset = -eros * 15.0;

        return base + contOffset + weirdOffset + erosOffset;
    }

    // === Biome parameter processing ===

    private List<Pair<Climate.ParameterPoint, Holder<Biome>>> buildParameterPoints() {
        List<Pair<Climate.ParameterPoint, Holder<Biome>>> result = new ArrayList<>();

        for (Holder<Biome> holder : biomes) {
            String keyName = holder.unwrapKey()
                    .map(ResourceKey::location)
                    .map(ResourceLocation::toString)
                    .orElse("");

            Climate.ParameterPoint point = buildParameterPoint(keyName);
            result.add(Pair.of(point, holder));
        }

        return result;
    }

    private Climate.ParameterPoint buildParameterPoint(String biomeKey) {
        Map<String, Double> params = parameterLoader.getParameters(biomeKey);

        float temperature = params.getOrDefault("temperature", 0.0).floatValue();
        float humidity = params.getOrDefault("humidity", 0.0).floatValue();
        float continentalness = params.getOrDefault("continentalness", 0.3).floatValue();
        float erosion = params.getOrDefault("erosion", 0.4).floatValue();
        float depth = params.getOrDefault("depth", -0.1).floatValue();
        float weirdness = params.getOrDefault("weirdness", 0.0).floatValue();

        return new Climate.ParameterPoint(
                Climate.Parameter.point(temperature),
                Climate.Parameter.point(humidity),
                Climate.Parameter.point(continentalness),
                Climate.Parameter.point(erosion),
                Climate.Parameter.point(depth),
                Climate.Parameter.point(weirdness),
                0L
        );
    }

    // === BiomeProvider API ===

    @Override
    public @NotNull org.bukkit.block.Biome getBiome(@NotNull WorldInfo worldInfo, int x, int y, int z) {
        Holder<Biome> biome = source.getNoiseBiome(
                QuartPos.fromBlock(x),
                QuartPos.fromBlock(y),
                QuartPos.fromBlock(z),
                sampler
        );

        var registry = RegistryAccess.registryAccess().getRegistry(RegistryKey.BIOME);
        return biome.unwrapKey()
                .flatMap(key -> {
                    NamespacedKey ns = NamespacedKey.fromString(key.location().toString());
                    return ns == null ? Optional.empty() : Optional.ofNullable(registry.get(ns));
                })
                .orElse(org.bukkit.block.Biome.PLAINS);
    }

    @Override
    public @NotNull List<org.bukkit.block.Biome> getBiomes(@NotNull WorldInfo worldInfo) {
        return bukkitBiomes;
    }
}
