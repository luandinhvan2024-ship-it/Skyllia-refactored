package fr.euphyllia.skyllia.commands.common.subcommands;

import fr.euphyllia.skyllia.Skyllia;
import fr.euphyllia.skyllia.api.SkylliaAPI;
import fr.euphyllia.skyllia.api.commands.SubCommandInterface;
import fr.euphyllia.skyllia.api.permissions.PermissionId;
import fr.euphyllia.skyllia.api.permissions.PermissionNode;
import fr.euphyllia.skyllia.api.skyblock.Island;
import fr.euphyllia.skyllia.configuration.ConfigLoader;
import fr.euphyllia.skyllia.utils.PlayerUtils;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class UnlockSubCommand implements SubCommandInterface {

    private static final String NODE = "command.island.unlock";
    private final PermissionId UNLOCK_COMMAND_PERMISSION;

    public UnlockSubCommand() {
        this.UNLOCK_COMMAND_PERMISSION = SkylliaAPI.getPermissionRegistry().register(new PermissionNode(
                new NamespacedKey(SkylliaAPI.getPlugin(), NODE),
                "island.permission.command.unlock.name",
                "island.permission.command.unlock.description"
        ));
    }

    @Override
    public void onExecute(@NotNull Plugin plugin, @NotNull CommandSender sender, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            ConfigLoader.language.sendMessage(sender, "island.player.player-only-command");
            return;
        }
        if (!PlayerUtils.hasPermission(player, "skyllia.island.command.unlock")) {
            ConfigLoader.language.sendMessage(player, "island.player.permission-denied");
            return;
        }

        Island island = SkylliaAPI.getIslandByPlayerId(player.getUniqueId());
        if (island == null) {
            ConfigLoader.language.sendMessage(player, "island.player.no-island");
            return;
        }

        boolean hasPermission = SkylliaAPI.getPermissionsManager()
                .hasPermission(player, island, UNLOCK_COMMAND_PERMISSION, null, ConfigLoader.general.getDebugSettings().permission());

        if (!hasPermission) {
            ConfigLoader.language.sendMessage(player, "island.player.permission-denied");
            return;
        }

        boolean isUpdate = island.setPrivateIsland(false);

        if (isUpdate) {
            ConfigLoader.language.sendMessage(player, "island.lock.open");
        }
    }

    @Override
    public @NotNull List<String> onTabComplete(@NotNull Plugin plugin, @NotNull CommandSender sender, @NotNull String[] args) {
        return Collections.emptyList();
    }
}
