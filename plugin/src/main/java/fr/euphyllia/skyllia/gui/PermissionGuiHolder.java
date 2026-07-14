package fr.euphyllia.skyllia.gui;

import fr.euphyllia.skyllia.api.permissions.PermissionNode;
import fr.euphyllia.skyllia.api.skyblock.model.RoleType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class PermissionGuiHolder implements InventoryHolder {

    public enum GuiMode {
        ROLE_SELECT,
        PERMISSION_LIST
    }

    private final GuiMode mode;
    private final RoleType selectedRole;
    private final int page;
    private final List<PermissionNode> permissions;
    private Inventory inventory;

    public PermissionGuiHolder(GuiMode mode, RoleType selectedRole, int page, List<PermissionNode> permissions) {
        this.mode = mode;
        this.selectedRole = selectedRole;
        this.page = page;
        this.permissions = permissions;
    }

    public GuiMode getMode() {
        return mode;
    }

    public RoleType getSelectedRole() {
        return selectedRole;
    }

    public int getPage() {
        return page;
    }

    public List<PermissionNode> getPermissions() {
        return permissions;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
