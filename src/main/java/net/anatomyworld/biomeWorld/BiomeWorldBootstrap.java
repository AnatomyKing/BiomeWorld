package net.anatomyworld.biomeWorld;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import net.anatomyworld.biomeWorld.util.BiomeParameterLoader;
import net.anatomyworld.biomeWorld.util.ProfileCache;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

/**
 * Runs very early – before Paper finishes loading plugins.
 * Makes sure profile YAML files exist and caches them, so
 * BiomeWorldMain.onLoad() can register noise-settings with the correct
 * sea-level.
 */
public final class BiomeWorldBootstrap implements PluginBootstrap {

    @Override
    public void bootstrap(@NotNull BootstrapContext context) {
        ComponentLogger log  = context.getLogger();
        File dataDir         = context.getDataDirectory().toFile();
        File profilesDir     = new File(dataDir, "profiles");

        /* 1 ─ ensure directory + default profile are present ----------- */
        if (!profilesDir.exists() && !profilesDir.mkdirs()) {
            log.warn("[BiomeWorld] Cannot create profiles directory {}", profilesDir);
            return;
        }
        copyIfMissing("profiles/vanilla.yml", profilesDir, log);
        // (copy bundled extra profiles here if you have any)

        /* 2 ─ read every *.yml* now on disk ---------------------------- */
        File[] files = profilesDir.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) return;

        for (File f : files) {
            String name = f.getName().replace(".yml", "");
            BiomeParameterLoader loader =
                    new BiomeParameterLoader(log, profilesDir, name);
            ProfileCache.put(name, loader);
        }
        log.info("[BiomeWorld] Bootstrap cached {} profile(s)", ProfileCache.all().size());
    }

    /* ------------------------------------------------------------------ */

    private void copyIfMissing(String resource, File targetDir, ComponentLogger log) {
        String name   = new File(resource).getName();       // e.g. vanilla.yml
        File   target = new File(targetDir, name);

        if (target.exists()) return;

        try (InputStream in = getClass().getClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                log.warn("[BiomeWorld] Bundled resource '{}' not found", resource);
                return;
            }
            Files.copy(in, target.toPath());
            log.info("[BiomeWorld] Extracted default '{}'", name);
        } catch (IOException ex) {
            log.warn("[BiomeWorld] Cannot write '{}': {}", name, ex.getMessage());
        }
    }
}
