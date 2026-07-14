package fr.euphyllia.skyllia.gui;

import fr.euphyllia.skyllia.Skyllia;
import fr.euphyllia.skyllia.api.SkylliaAPI;
import fr.euphyllia.skyllia.api.skyblock.Island;
import fr.euphyllia.skyllia.api.skyblock.model.RoleType;
import fr.euphyllia.skyllia.configuration.ConfigLoader;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

public class PermissionGuiListener implements Listener {

    private final Skyllia plugin;

    public PermissionGuiListener(Skyllia plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onInventoryClick(@NotNull InventoryClickEvent event) {
        Inventory topInventory = event.getView().getTopInventory();
        if (!(topInventory.getHolder() instanceof PermissionGuiHolder holder)) return;

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getClickedInventory() == null) return;
        if (!event.getClickedInventory().equals(topInventory)) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) return;

        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) return;

        var pdc = meta.getPersistentDataContainer();

        // Check navigation buttons
        String navAction = pdc.get(
                new NamespacedKey(SkylliaAPI.getPlugin(), "gui_nav"),
                PersistentDataType.STRING
        );
        if (navAction != null) {
            handleNavigation(player, holder, navAction);
            return;
        }

        // Check role selection
        String roleStr = pdc.get(
                new NamespacedKey(SkylliaAPI.getPlugin(), "gui_role"),
                PersistentDataType.STRING
        );
        if (roleStr != null) {
            try {
                RoleType role = RoleType.valueOf(roleStr);
                Island island = SkylliaAPI.getIslandByPlayerId(player.getUniqueId());
                if (island == null) {
                    player.closeInventory();
                    return;
                }
                Bukkit.getRegionScheduler().run(plugin, player.getLocation(), t ->
                        PermissionGui.openPermissionList(player, island, role, 0));
            } catch (IllegalArgumentException ignored) {
            }
            return;
        }

        // Check permission toggle
        String permKey = pdc.get(
                new NamespacedKey(SkylliaAPI.getPlugin(), "gui_perm"),
                PersistentDataType.STRING
        );
        if (permKey != null && holder.getMode() == PermissionGuiHolder.GuiMode.PERMISSION_LIST) {
            Island island = SkylliaAPI.getIslandByPlayerId(player.getUniqueId());
            if (island == null) {
                player.closeInventory();
                return;
            }

            RoleType role = holder.getSelectedRole();
            if (role == null) return;

            NamespacedKey key = NamespacedKey.fromString(permKey);
            if (key == null) return;

            Bukkit.getAsyncScheduler().runNow(plugin, t -> {
                boolean success = PermissionGui.togglePermission(island, role, key);
                if (success) {
                    Bukkit.getRegionScheduler().run(plugin, player.getLocation(), t2 ->
                            PermissionGui.openPermissionList(player, island, role, holder.getPage()));
                } else {
                    ConfigLoader.language.sendMessage(player, "island.permission.update.failed");
                }
            });
        }
    }

    private void handleNavigation(Player player, PermissionGuiHolder holder, String action) {
        Island island = SkylliaAPI.getIslandByPlayerId(player.getUniqueId());
        if (island == null) {
            player.closeInventory();
            return;
        }

        switch (action) {
            case "nav:previous" -> {
                if (holder.getMode() == PermissionGuiHolder.GuiMode.PERMISSION_LIST && holder.getSelectedRole() != null) {
                    Bukkit.getRegionScheduler().run(plugin, player.getLocation(), t ->
                            PermissionGui.openPermissionList(player, island, holder.getSelectedRole(), holder.getPage() - 1));
                }
            }
            case "nav:next" -> {
                if (holder.getMode() == PermissionGuiHolder.GuiMode.PERMISSION_LIST && holder.getSelectedRole() != null) {
                    Bukkit.getRegionScheduler().run(plugin, player.getLocation(), t ->
                            PermissionGui.openPermissionList(player, island, holder.getSelectedRole(), holder.getPage() + 1));
                }
            }
            case "nav:back" -> {
                if (holder.getMode() == PermissionGuiHolder.GuiMode.PERMISSION_LIST) {
                    Bukkit.getRegionScheduler().run(plugin, player.getLocation(), t ->
                            PermissionGui.openRoleSelect(player, island));
                }
            }
        }
    }
}
