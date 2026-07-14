package fr.euphyllia.skyllia.gui;

import fr.euphyllia.skyllia.api.skyblock.Island;
import fr.euphyllia.skyllia.api.skyblock.model.RoleType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * InventoryHolder for the Permission GUI.
 * Carries the island context and current view state (selected role + page).
 * Contains NO logic - just data carrier.
 */
public final class PermissionGuiHolder implements InventoryHolder {

    private final Island island;
    private final RoleType selectedRole;
    private final int page;
    private Inventory inventory;

    public PermissionGuiHolder(@NotNull Island island, @Nullable RoleType selectedRole, int page) {
        this.island = island;
        this.selectedRole = selectedRole;
        this.page = page;
    }

    public Island getIsland() {
        return island;
    }

    public RoleType getSelectedRole() {
        return selectedRole;
    }

    public int getPage() {
        return page;
    }

    public void setInventory(@NotNull Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
