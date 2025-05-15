package net.anatomyworld.biomeWorld.util;

import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.CraftServer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.NoiseRouter;

import java.util.Locale;

/**
 * Builds and registers a clone of the vanilla overworld settings with
 * a custom sea-level.  Runs only while the registry is still mutable.
 */
public final class CustomNoiseSettingsRegistry {

    private static final int VANILLA_SEA_LEVEL = 63;

    public static void registerProfileNoiseSettings(String profileName, int seaLevel) {
        ResourceLocation id  = ResourceLocation.parse(
                "biomeworld:" + profileName.toLowerCase(Locale.ROOT));
        ResourceKey<NoiseGeneratorSettings> key =
                ResourceKey.create(Registries.NOISE_SETTINGS, id);

        /* if already present we assume it was created earlier in this
           boot-cycle with the correct sea-level and simply exit.       */
        MinecraftServer server = ((CraftServer) Bukkit.getServer()).getServer();
        Registry<NoiseGeneratorSettings> registry =
                server.registryAccess().lookupOrThrow(Registries.NOISE_SETTINGS);
        if (registry.getOptional(key).isPresent()) return;

        /* ------------------------------------------------------------------
           Build a customised copy of the vanilla overworld settings
           ---------------------------------------------------------------- */
        NoiseGeneratorSettings vanilla = registry
                .getOptional(NoiseGeneratorSettings.AMPLIFIED)
                .orElseThrow(() -> new IllegalStateException(
                        "Vanilla overworld settings missing – cannot clone."));

        double offset         = (VANILLA_SEA_LEVEL - seaLevel) / 64.0D;
        DensityFunction shift = DensityFunctions.constant(offset);

        NoiseRouter v = vanilla.noiseRouter();
        NoiseRouter routed = new NoiseRouter(
                v.barrierNoise(), v.fluidLevelFloodednessNoise(), v.fluidLevelSpreadNoise(),
                v.lavaNoise(), v.temperature(), v.vegetation(),
                DensityFunctions.add(v.continents(), shift),  // shifted
                v.erosion(),
                DensityFunctions.add(v.depth(), shift),       // shifted
                v.ridges(), v.initialDensityWithoutJaggedness(), v.finalDensity(),
                v.veinToggle(), v.veinRidged(), v.veinGap());

        NoiseGeneratorSettings custom = new NoiseGeneratorSettings(
                vanilla.noiseSettings(), vanilla.defaultBlock(), vanilla.defaultFluid(),
                routed, SurfaceRuleHelper.overworld(seaLevel), vanilla.spawnTarget(),
                seaLevel, vanilla.disableMobGeneration(), vanilla.aquifersEnabled(),
                vanilla.oreVeinsEnabled(), vanilla.useLegacyRandomSource());

        Registry.register(registry, key, custom);   // first & only time
    }

    private CustomNoiseSettingsRegistry() {}
}
