// ============================================================================
//  CustomNoiseSettingsRegistry.java
//  package: net.anatomyworld.biomeWorld.util
// ============================================================================

package net.anatomyworld.biomeWorld.util;

import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.CraftServer;

import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseRouter;

import java.util.Locale;

/**
 * Registers (once per server run) a clone of the vanilla overworld
 * {@link NoiseGeneratorSettings} under <code>biomeworld:&lt;profile&gt;</code>
 * with three changes:
 * <ol>
 *   <li>custom {@code sea_level}</li>
 *   <li>surface rule splice that puts water/stone at the new level</li>
 *   <li>vertical offset of the <em>continents</em> & <em>depth</em> density
 *       functions so actual oceans disappear / re‑appear</li>
 * </ol>
 */
public final class CustomNoiseSettingsRegistry {

    /** Vanilla sea level that Mojang balanced terrain against. */
    private static final int VANILLA_SEA_LEVEL = 63;

    /**
     * Ensure a {@link NoiseGeneratorSettings} entry for this profile &amp;
     * sea‑level exists in the live registry.
     *
     * @param profileName  the profile (file) name, becomes the namespace id
     * @param seaLevel     absolute Y you want water to render at
     */
    public static void registerProfileNoiseSettings(String profileName, int seaLevel) {
        /* ------------------------------------------------------------------
           1)  Registry look‑ups / early exit
           ---------------------------------------------------------------- */
        ResourceLocation id = ResourceLocation.parse(
                "biomeworld:" + profileName.toLowerCase(Locale.ROOT));
        ResourceKey<NoiseGeneratorSettings> key =
                ResourceKey.create(Registries.NOISE_SETTINGS, id);

        MinecraftServer                  server   =
                ((CraftServer) Bukkit.getServer()).getServer();
        RegistryAccess                   access   = server.registryAccess();
        Registry<NoiseGeneratorSettings> registry =
                access.lookupOrThrow(Registries.NOISE_SETTINGS);

        if (registry.getOptional(key).isPresent()) return;   // done already ✔

        NoiseGeneratorSettings vanilla = registry
                .getOptional(NoiseGeneratorSettings.OVERWORLD)
                .orElseThrow(() -> new IllegalStateException(
                        "Vanilla overworld settings missing – cannot clone."));

        /* ------------------------------------------------------------------
           2)  Create shifted density functions so ocean basins vanish
           ---------------------------------------------------------------- */
        double dfOffset         = (VANILLA_SEA_LEVEL - seaLevel) / 64.0D;
        DensityFunction shift   = DensityFunctions.constant(dfOffset);

        NoiseRouter vRouter     = vanilla.noiseRouter(); // shortcut

        NoiseRouter routed = new NoiseRouter(
                /* barrierNoise                     */ vRouter.barrierNoise(),
                /* fluidLevelFloodednessNoise       */ vRouter.fluidLevelFloodednessNoise(),
                /* fluidLevelSpreadNoise            */ vRouter.fluidLevelSpreadNoise(),
                /* lavaNoise                        */ vRouter.lavaNoise(),
                /* temperature                      */ vRouter.temperature(),
                /* vegetation                       */ vRouter.vegetation(),

                /* continents  (shifted)            */
                DensityFunctions.add(vRouter.continents(),   shift),

                /* erosion                          */ vRouter.erosion(),

                /* depth (shifted)                  */
                DensityFunctions.add(vRouter.depth(),        shift),

                /* ridges                           */ vRouter.ridges(),
                /* initialDensityWithoutJaggedness  */ vRouter.initialDensityWithoutJaggedness(),
                /* finalDensity                     */ vRouter.finalDensity(),
                /* vein helpers                     */ vRouter.veinToggle(),
                vRouter.veinRidged(),
                vRouter.veinGap()
        );

        /* ------------------------------------------------------------------
           3)  Assemble the fully‑customised settings object
           ---------------------------------------------------------------- */
        NoiseGeneratorSettings custom = new NoiseGeneratorSettings(
                vanilla.noiseSettings(),            // same resolution etc.
                vanilla.defaultBlock(),
                vanilla.defaultFluid(),
                routed,                             // ← shifted terrain
                SurfaceRuleHelper.overworld(seaLevel),
                vanilla.spawnTarget(),
                seaLevel,                           // new aquifer reference
                vanilla.disableMobGeneration(),
                vanilla.aquifersEnabled(),
                vanilla.oreVeinsEnabled(),
                vanilla.useLegacyRandomSource()
        );

        /* ------------------------------------------------------------------
           4)  Put it into the live registry
           ---------------------------------------------------------------- */
        Registry.register(registry, key, custom);
    }

    /** Utility class – no instances. */
    private CustomNoiseSettingsRegistry() {}
}
