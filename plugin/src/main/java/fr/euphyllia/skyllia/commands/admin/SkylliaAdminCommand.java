package fr.euphyllia.skyllia.commands.admin;

import fr.euphyllia.skyllia.Skyllia;
import fr.euphyllia.skyllia.api.commands.SubCommandInterface;
import fr.euphyllia.skyllia.api.commands.SubCommandRegistry;
import fr.euphyllia.skyllia.commands.admin.subcommands.*;
import fr.euphyllia.skyllia.configuration.ConfigLoader;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class SkylliaAdminCommand implements SubCommandInterface {

    private final Skyllia plugin;
    private final SubCommandRegistry registry;

    public SkylliaAdminCommand(Skyllia Skyllia, SubCommandRegistry registry) {
        this.plugin = Skyllia;
        this.registry = registry;
        registerDefaultCommands();
    }

    @Override
    public void onExecute(@NotNull Plugin plugin, @NotNull CommandSender sender, @NotNull String[] args) {
        if (!sender.hasPermission(permission())) {
            ConfigLoader.language.sendMessage(sender, "island.player.permission-denied");
            return;
        }
        if (args.length != 0) {
            String subCommand = args[0].trim().toLowerCase();
            String[] listArgs = Arrays.copyOfRange(args, 1, args.length);
            SubCommandInterface subCommandInterface = registry.getSubCommandByName(subCommand);
            if (subCommandInterface == null) {
                ConfigLoader.language.sendMessage(sender, "misc.unknown-command");
                return;
            }
            Bukkit.getAsyncScheduler().runNow(this.plugin, task ->
                    subCommandInterface.onExecute(this.plugin, sender, listArgs));
        }
    }

    @Override
    public @NotNull List<String> onTabComplete(@NotNull Plugin plugin, @NotNull CommandSender sender, @NotNull String[] args) {
        if (!sender.hasPermission(permission())) {
            return Collections.emptyList();
        }
        Set<String> commands = registry.getCommandMap().keySet();
        if (args.length == 0) {
            return List.copyOf(commands);
        } else if (args.length == 1) {
            String partial = args[0].trim().toLowerCase();
            return commands.stream().filter(command -> command.toLowerCase().startsWith(partial)).toList();
        } else {
            String subCommand = args[0].trim().toLowerCase();
            String[] listArgs = Arrays.copyOfRange(args, 1, args.length);
            SubCommandInterface subCommandInterface = registry.getSubCommandByName(subCommand);
            if (subCommandInterface != null) {
                return subCommandInterface.onTabComplete(this.plugin, sender, listArgs);
            }
        }
        return Collections.emptyList();
    }

    private void registerDefaultCommands() {
        registry.registerSubCommand(new CurrentSubCommands(), "current");
        registry.registerSubCommand(new ForceDeleteSubCommands(), "force_delete", "forcedelete");
        registry.registerSubCommand(new ForceTransferSubCommands(), "force_transfer", "forcetransfer");
        registry.registerSubCommand(new InfoSubCommand(), "info", "information");
        registry.registerSubCommand(new ReloadSubCommands(), "reload");
        registry.registerSubCommand(new SetMaxMembersSubCommands(), "set_max_member", "setmaxmembers");
        registry.registerSubCommand(new SetSizeSubCommands(), "set_size", "setsize");
        registry.registerSubCommand(new SetHeightSubCommands(), "set_height", "setheight");
        registry.registerSubCommand(new SchematicSubCommands(), "schematic", "schem");
        registry.registerSubCommand(new ForceCreateSubCommands(), "create");

        // extra
        registry.registerSubCommand(new AdminSetDescriptionCommand(), "set_name", "setname");
        registry.registerSubCommand(new AdminSetDescriptionCommand(), "set_description", "setdescription");
    }

    @Override
    public String permission() {
        return "skyllia.admins.commands";
    }
}
