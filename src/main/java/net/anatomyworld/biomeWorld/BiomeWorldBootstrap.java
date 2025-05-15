package net.anatomyworld.biomeWorld;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import net.anatomyworld.biomeWorld.util.BiomeParameterLoader;
import net.anatomyworld.biomeWorld.util.ProfileCache;

import java.io.File;

public class BiomeWorldBootstrap implements PluginBootstrap {

    @Override
    public void bootstrap(BootstrapContext context) {
        System.out.println(">>> [BiomeWorld] Bootstrap running");

        File profilesDir = context.getDataDirectory().resolve("profiles").toFile();
        if (!profilesDir.exists()) return;

        File[] files = profilesDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;

        for (File file : files) {
            String profile = file.getName().replace(".yml", "");
            BiomeParameterLoader loader = new BiomeParameterLoader(context.getLogger(), profilesDir, profile);
            ProfileCache.put(profile, loader);
        }

        System.out.println(">>> [BiomeWorld] Bootstrap loaded " + ProfileCache.all().size() + " profile(s)");
    }
}
