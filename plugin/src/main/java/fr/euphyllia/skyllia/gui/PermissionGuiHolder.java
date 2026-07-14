package fr.euphyllia.skyllia.gui;

import fr.euphyllia.skyllia.api.skyblock.Island;
import fr.euphyllia.skyllia.api.skyblock.model.RoleType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class PermissionGuiHolder implements InventoryHolder {

    public enum Screen {
        CATEGORY_SELECT,
        ROLE_SELECT,
        PERMISSION_LIST
    }

    private final Island island;
    private final Screen screen;
    private final String category;
    private final RoleType selectedRole;
    private final int page;
    private Inventory inventory;

    public PermissionGuiHolder(@NotNull Island island, @NotNull Screen screen,
                               @Nullable String category, @Nullable RoleType selectedRole, int page) {
        this.island = island;
        this.screen = screen;
        this.category = category;
        this.selectedRole = selectedRole;
        this.page = page;
    }

    public Island getIsland() { return island; }
    public Screen getScreen() { return screen; }
    public String getCategory() { return category; }
    public RoleType getSelectedRole() { return selectedRole; }
    public int getPage() { return page; }

    public void setInventory(@NotNull Inventory inventory) { this.inventory = inventory; }

    @Override
    public @NotNull Inventory getInventory() { return inventory; }
}
