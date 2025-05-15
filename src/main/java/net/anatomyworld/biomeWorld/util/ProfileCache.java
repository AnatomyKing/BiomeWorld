package net.anatomyworld.biomeWorld.util;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class ProfileCache {
    public static final Map<String, BiomeParameterLoader> PROFILES = new HashMap<>();

    public static void put(String name, BiomeParameterLoader loader) {
        PROFILES.put(name.toLowerCase(Locale.ROOT), loader);
    }

    public static Map<String, BiomeParameterLoader> all() {
        return PROFILES;
    }

    private ProfileCache() {}
}
