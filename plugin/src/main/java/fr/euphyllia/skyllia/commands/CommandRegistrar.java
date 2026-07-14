package fr.euphyllia.skyllia.commands;

import fr.euphyllia.skyllia.Skyllia;
import fr.euphyllia.skyllia.api.commands.SubCommandRegistry;
import fr.euphyllia.skyllia.commands.admin.SubAdminCommandImpl;
import fr.euphyllia.skyllia.commands.common.SkylliaCommand;
import fr.euphyllia.skyllia.commands.common.SubCommandImpl;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.Plugin;

@SuppressWarnings("UnstableApiUsage")
public class CommandRegistrar {

    private final Skyllia plugin;
    private final SubCommandRegistry commandRegistry;
    private final SubCommandRegistry adminCommandRegistry;

    public CommandRegistrar(Skyllia plugin) {
        this.plugin = plugin;
        this.commandRegistry = new SubCommandImpl();
        this.adminCommandRegistry = new SubAdminCommandImpl();
    }

    public void registerCommands() {
        LifecycleEventManager<@org.jetbrains.annotations.NotNull Plugin> manager = plugin.getLifecycleManager();

        manager.registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            final Commands commands = event.registrar();

            commands.register(
                    "is",
                    "Islands commands",
                    java.util.List.of("ob"),
                    new SkylliaCommand(plugin)
            );
        });
    }

    public SubCommandRegistry getCommandRegistry() {
        return commandRegistry;
    }

    public SubCommandRegistry getAdminCommandRegistry() {
        return adminCommandRegistry;
    }
}
