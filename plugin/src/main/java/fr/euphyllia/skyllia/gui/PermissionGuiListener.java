package fr.euphyllia.skyllia.gui;

import fr.euphyllia.skyllia.Skyllia;
import fr.euphyllia.skyllia.api.SkylliaAPI;
import fr.euphyllia.skyllia.api.skyblock.Island;
import fr.euphyllia.skyllia.api.skyblock.model.RoleType;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

public class PermissionGuiListener implements Listener {

    private final Skyllia plugin;
    private final PermissionGui permissionGui;
    private final NamespacedKey navKey;
    private final NamespacedKey roleKey;
    private final NamespacedKey permKey;

    public PermissionGuiListener(Skyllia plugin, PermissionGui permissionGui) {
        this.plugin = plugin;
        this.permissionGui = permissionGui;
        this.navKey = new NamespacedKey(SkylliaAPI.getPlugin(), "gui_nav");
        this.roleKey = new NamespacedKey(SkylliaAPI.getPlugin(), "gui_role");
        this.permKey = new NamespacedKey(SkylliaAPI.getPlugin(), "gui_perm");
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClick(@NotNull InventoryClickEvent event) {
        Inventory topInventory = event.getInventory();
        if (!(topInventory.getHolder() instanceof PermissionGuiHolder)) return;

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;

        Inventory clicked = event.getClickedInventory();
        if (clicked == null || !clicked.equals(topInventory)) return;

        InventoryAction action = event.getAction();
        if (action == InventoryAction.NOTHING) return;

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType().isAir()) return;

        ItemMeta meta = clickedItem.getItemMeta();
        if (meta == null) return;

        var pdc = meta.getPersistentDataContainer();

        if (!(topInventory.getHolder() instanceof PermissionGuiHolder holder)) return;

        String navAction = pdc.get(navKey, PersistentDataType.STRING);
        if (navAction != null) {
            handleClick(player, holder, navAction);
            return;
        }

        String roleStr = pdc.get(roleKey, PersistentDataType.STRING);
        if (roleStr != null && holder.getMode() == PermissionGuiHolder.GuiMode.ROLE_SELECT) {
            try {
                RoleType role = RoleType.valueOf(roleStr);
                Island island = SkylliaAPI.getIslandByPlayerId(player.getUniqueId());
                if (island == null) {
                    player.closeInventory();
                    return;
                }
                final RoleType finalRole = role;
                Bukkit.getRegionScheduler().run(plugin, player.getLocation(), t ->
                        permissionGui.openPermissionList(player, island, finalRole, 0));
            } catch (IllegalArgumentException ignored) {
            }
            return;
        }

        String permKeyStr = pdc.get(permKey, PersistentDataType.STRING);
        if (permKeyStr != null && holder.getMode() == PermissionGuiHolder.GuiMode.PERMISSION_LIST) {
            Island island = SkylliaAPI.getIslandByPlayerId(player.getUniqueId());
            if (island == null) {
                player.closeInventory();
                return;
            }

            RoleType role = holder.getSelectedRole();
            if (role == null) return;

            NamespacedKey key = NamespacedKey.fromString(permKeyStr);
            if (key == null) return;

            final int currentPage = holder.getPage();
            Bukkit.getAsyncScheduler().runNow(plugin, t -> {
                boolean success = permissionGui.togglePermission(island, role, key);
                if (success) {
                    Bukkit.getRegionScheduler().run(plugin, player.getLocation(), t2 ->
                            permissionGui.openPermissionList(player, island, role, currentPage));
                }
            });
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryDrag(@NotNull InventoryDragEvent event) {
        Inventory topInventory = event.getInventory();
        if (topInventory.getHolder() instanceof PermissionGuiHolder) {
            event.setCancelled(true);
        }
    }

    private void handleClick(Player player, PermissionGuiHolder holder, String action) {
        Island island = SkylliaAPI.getIslandByPlayerId(player.getUniqueId());
        if (island == null) {
            player.closeInventory();
            return;
        }

        switch (action) {
            case "nav:previous" -> {
                if (holder.getMode() == PermissionGuiHolder.GuiMode.PERMISSION_LIST && holder.getSelectedRole() != null) {
                    final RoleType role = holder.getSelectedRole();
                    final int newPage = holder.getPage() - 1;
                    Bukkit.getRegionScheduler().run(plugin, player.getLocation(), t ->
                            permissionGui.openPermissionList(player, island, role, newPage));
                }
            }
            case "nav:next" -> {
                if (holder.getMode() == PermissionGuiHolder.GuiMode.PERMISSION_LIST && holder.getSelectedRole() != null) {
                    final RoleType role = holder.getSelectedRole();
                    final int newPage = holder.getPage() + 1;
                    Bukkit.getRegionScheduler().run(plugin, player.getLocation(), t ->
                            permissionGui.openPermissionList(player, island, role, newPage));
                }
            }
            case "nav:back" -> {
                if (holder.getMode() == PermissionGuiHolder.GuiMode.PERMISSION_LIST) {
                    Bukkit.getRegionScheduler().run(plugin, player.getLocation(), t ->
                            permissionGui.openRoleSelect(player, island));
                }
            }
            case "nav:close" -> {
                player.closeInventory();
            }
        }
    }
}
