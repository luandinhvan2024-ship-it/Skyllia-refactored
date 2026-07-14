package fr.euphyllia.skyllia.commands.common.subcommands;

import fr.euphyllia.skyllia.Skyllia;
import fr.euphyllia.skyllia.api.SkylliaAPI;
import fr.euphyllia.skyllia.api.commands.SubCommandInterface;
import fr.euphyllia.skyllia.api.configuration.WorldConfig;
import fr.euphyllia.skyllia.api.permissions.PermissionId;
import fr.euphyllia.skyllia.api.permissions.PermissionNode;
import fr.euphyllia.skyllia.api.skyblock.Island;
import fr.euphyllia.skyllia.api.skyblock.Players;
import fr.euphyllia.skyllia.api.skyblock.model.RoleType;
import fr.euphyllia.skyllia.api.utils.RegionUtils;
import fr.euphyllia.skyllia.configuration.ConfigLoader;
import fr.euphyllia.skyllia.utils.PlayerUtils;
import fr.euphyllia.skyllia.utils.WorldUtils;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class LockSubCommand implements SubCommandInterface {

    private static final String NODE = "command.island.lock";
    private final PermissionId LOCK_COMMAND_PERMISSION;

    public LockSubCommand() {
        this.LOCK_COMMAND_PERMISSION = SkylliaAPI.getPermissionRegistry().register(new PermissionNode(
                new NamespacedKey(SkylliaAPI.getPlugin(), NODE),
                "island.permission.command.lock.name",
                "island.permission.command.lock.description"
        ));
    }

    @Override
    public void onExecute(@NotNull Plugin plugin, @NotNull CommandSender sender, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            ConfigLoader.language.sendMessage(sender, "island.player.player-only-command");
            return;
        }
        if (!PlayerUtils.hasPermission(player, "skyllia.island.command.lock")) {
            ConfigLoader.language.sendMessage(player, "island.player.permission-denied");
            return;
        }

        Island island = SkylliaAPI.getIslandByPlayerId(player.getUniqueId());
        if (island == null) {
            ConfigLoader.language.sendMessage(player, "island.player.no-island");
            return;
        }

        boolean hasPermission = SkylliaAPI.getPermissionsManager()
                .hasPermission(player, island, LOCK_COMMAND_PERMISSION, null, ConfigLoader.general.getDebugSettings().permission());

        if (!hasPermission) {
            ConfigLoader.language.sendMessage(player, "island.player.permission-denied");
            return;
        }

        boolean isUpdate = island.setPrivateIsland(true);

        if (isUpdate) {
            ConfigLoader.language.sendMessage(player, "island.lock.close");

            for (WorldConfig worldConfig : WorldUtils.getWorldConfigs()) {
                RegionUtils.getEntitiesInRegion(
                        Skyllia.getInstance(),
                        ConfigLoader.general.getIslandSettings().regionDistance(),
                        EntityType.PLAYER,
                        worldConfig.getWorld(),
                        island.getRegionCoordinate(),
                        island.getSize(),
                        entity -> {
                            Player playerInIsland = (Player) entity;

                            if (PlayerUtils.hasPermission(playerInIsland, "skyllia.island.command.lock.bypass"))
                                return;

                            Players players = island.getMember(playerInIsland.getUniqueId());
                            if (players == null
                                    || players.getRoleType().equals(RoleType.BAN)
                                    || players.getRoleType().equals(RoleType.VISITOR)) {
                                PlayerUtils.teleportPlayerSpawn(playerInIsland);
                            }
                        }
                );
            }
        }
    }

    @Override
    public @NotNull List<String> onTabComplete(@NotNull Plugin plugin, @NotNull CommandSender sender, @NotNull String[] args) {
        return Collections.emptyList();
    }
}
