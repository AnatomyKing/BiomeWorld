package net.anatomyworld.biomeWorld.util;

import net.anatomyworld.biomeWorld.BiomeWorldMain;
import org.bukkit.command.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class BiomeWorldCommand implements CommandExecutor, TabCompleter {

    private final BiomeWorldMain plugin;

    public BiomeWorldCommand(BiomeWorldMain plugin) {
        this.plugin = plugin;
        // don’t crash if the command is not yet registered
        Optional.ofNullable(plugin.getCommand("biomeworld"))
                .ifPresent(c -> c.setTabCompleter(this));
    }

    /* ------------------ Command logic (unchanged) ------------------- */

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

                BiomeParameterLoader loader = new BiomeParameterLoader(plugin, profileName);
                CustomNoiseSettingsRegistry.registerProfileNoiseSettings(
                        loader.getProfileName(), loader.getSeaLevel());

                List<String> keys = new ArrayList<>(loader.getAllKeys());
                if (keys.isEmpty()) {
                    sender.sendMessage("§c[BiomeWorld] Failed to load or empty profile: " + profileName);
                } else {
                    sender.sendMessage("§a[BiomeWorld] Reloaded profile '" + profileName
                            + "' with " + keys.size() + " biomes.");
                    sender.sendMessage("§7Sea level set to: " + loader.getSeaLevel());
                }
            } else {
                sender.sendMessage("§cUsage: /biomeworld reload <profile>");
            }
            return true;
        }

        sender.sendMessage("§cUnknown subcommand. Usage: /biomeworld reload <profile>");
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender,
                                                @NotNull Command command,
                                                @NotNull String alias,
                                                @NotNull String[] args) {

        if (args.length == 1) {
            return List.of("reload");
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("reload")) {
            File profilesDir = new File(plugin.getDataFolder(), "profiles");
            if (profilesDir.exists() && profilesDir.isDirectory()) {
                return Arrays.stream(Objects.requireNonNull(
                                profilesDir.listFiles((dir, name) -> name.endsWith(".yml"))))
                        .map(f -> f.getName().replace(".yml", ""))
                        .collect(Collectors.toList());
            }
        }
        return Collections.emptyList();
    }
}
