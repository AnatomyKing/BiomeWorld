package net.anatomyworld.biomeWorld.util;

import net.anatomyworld.biomeWorld.BiomeWorldMain;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class BiomeWorldCommand implements CommandExecutor, TabCompleter {

    private final BiomeWorldMain plugin;

    public BiomeWorldCommand(BiomeWorldMain plugin) {
        this.plugin = plugin;

        // Register tab completer in constructor
        Objects.requireNonNull(plugin.getCommand("biomeworld")).setTabCompleter(this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§eUsage: /biomeworld reload");
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            plugin.getBiomeParameterLoader().loadConfig();
            sender.sendMessage("§a[BiomeWorld] Biome parameters reloaded successfully!");
            return true;
        }

        sender.sendMessage("§cUnknown subcommand. Usage: /biomeworld reload");
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return Collections.singletonList("reload");
        }

        return Collections.emptyList();
    }
}
