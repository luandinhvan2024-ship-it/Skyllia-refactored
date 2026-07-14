package fr.euphyllia.skyllia.gui;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public final class PermissionGuiListener implements Listener {

    private final PermissionGui permissionGui;

    public PermissionGuiListener(PermissionGui permissionGui) {
        this.permissionGui = permissionGui;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClick(@NotNull InventoryClickEvent event) {
        Inventory topInventory = event.getInventory();
        if (!(topInventory.getHolder() instanceof PermissionGuiHolder holder)) return;

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof org.bukkit.entity.Player player)) return;

        Inventory clicked = event.getClickedInventory();
        if (clicked == null || !clicked.equals(topInventory)) return;

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType().isAir()) return;

        permissionGui.handleClick(player, holder, clickedItem);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryDrag(@NotNull InventoryDragEvent event) {
        Inventory topInventory = event.getInventory();
        if (topInventory.getHolder() instanceof PermissionGuiHolder) {
            event.setCancelled(true);
        }
    }
}
