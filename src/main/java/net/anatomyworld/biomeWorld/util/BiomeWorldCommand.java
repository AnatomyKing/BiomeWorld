package net.anatomyworld.biomeWorld.util;

import net.anatomyworld.biomeWorld.BiomeWorldMain;
import org.bukkit.command.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Reload command – now *only* refreshes YAML + cache.  The registry
 * entry itself cannot be changed once the server is running.
 */
public class BiomeWorldCommand implements CommandExecutor, TabCompleter {

    private final BiomeWorldMain plugin;

    public BiomeWorldCommand(BiomeWorldMain plugin) {
        this.plugin = plugin;
        Optional.ofNullable(plugin.getCommand("biomeworld"))
                .ifPresent(c -> c.setTabCompleter(this));
    }

    /* ------------------------------------------------------------------ */

    @Override
    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             @NotNull String[] args) {

        if (args.length == 0) {
            sender.sendMessage("§eUsage: /biomeworld reload <profile>");
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (args.length == 2) {
                String profileName = args[1];

                /* 1) read YAML fresh ------------------------------------ */
                BiomeParameterLoader loader =
                        new BiomeParameterLoader(plugin, profileName);

                /* 2) update in-memory cache for next server start ------- */
                ProfileCache.put(profileName, loader);

                /* 3) user feedback -------------------------------------- */
                int size = loader.getAllKeys().size();
                if (size == 0) {
                    sender.sendMessage("§c[BiomeWorld] Failed to load or empty profile: "
                            + profileName);
                } else {
                    sender.sendMessage("§a[BiomeWorld] Reloaded profile '"
                            + profileName + "' with " + size + " biomes.");
                    sender.sendMessage("§7Sea level (next start): " + loader.getSeaLevel());
                }
            } else {
                sender.sendMessage("§cUsage: /biomeworld reload <profile>");
            }
            return true;
        }

        sender.sendMessage("§cUnknown subcommand. Usage: /biomeworld reload <profile>");
        return true;
    }

    /* ------------------------------------------------------------------ */

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender,
                                                @NotNull Command command,
                                                @NotNull String alias,
                                                @NotNull String[] args) {
        if (args.length == 1) return List.of("reload");

        if (args.length == 2 && args[0].equalsIgnoreCase("reload")) {
            File dir = new File(plugin.getDataFolder(), "profiles");
            if (dir.exists() && dir.isDirectory()) {
                return Arrays.stream(Objects.requireNonNull(
                                dir.listFiles((d, n) -> n.endsWith(".yml"))))
                        .map(f -> f.getName().replace(".yml", ""))
                        .collect(Collectors.toList());
            }
        }
        return Collections.emptyList();
    }
}
