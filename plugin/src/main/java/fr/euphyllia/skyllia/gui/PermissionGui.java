package fr.euphyllia.skyllia.gui;

import fr.euphyllia.skyllia.Skyllia;
import fr.euphyllia.skyllia.api.SkylliaAPI;
import fr.euphyllia.skyllia.api.database.IslandPermissionQuery;
import fr.euphyllia.skyllia.api.permissions.CompiledPermissions;
import fr.euphyllia.skyllia.api.permissions.PermissionId;
import fr.euphyllia.skyllia.api.permissions.PermissionNode;
import fr.euphyllia.skyllia.api.permissions.PermissionRegistry;
import fr.euphyllia.skyllia.api.permissions.PermissionSet;
import fr.euphyllia.skyllia.api.skyblock.Island;
import fr.euphyllia.skyllia.api.skyblock.model.RoleType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PermissionGui {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final NamespacedKey NAV_KEY = new NamespacedKey(SkylliaAPI.getPlugin(), "gui_nav");
    private static final NamespacedKey ROLE_KEY = new NamespacedKey(SkylliaAPI.getPlugin(), "gui_role");
    private static final NamespacedKey PERM_KEY = new NamespacedKey(SkylliaAPI.getPlugin(), "gui_perm");

    private final Skyllia plugin;
    private final PermissionGuiConfig config;

    public PermissionGui(Skyllia plugin) {
        this.plugin = plugin;
        this.config = new PermissionGuiConfig(plugin);
        this.config.load();
    }

    public void reloadConfig() {
        this.config.load();
    }

    public void openRoleSelect(Player player, Island island) {
        int rows = config.getRoleSelectRows();
        int size = rows * 9;
        PermissionGuiHolder holder = new PermissionGuiHolder(PermissionGuiHolder.GuiMode.ROLE_SELECT, null, 0, null);
        Inventory inv = Bukkit.createInventory(holder, size, MM.deserialize(
                replacePlaceholders(config.getRoleSelectTitle(), Map.of())));

        for (Map.Entry<String, PermissionGuiConfig.RoleItemConfig> entry : config.getRoleItems().entrySet()) {
            String roleName = entry.getKey();
            PermissionGuiConfig.RoleItemConfig rc = entry.getValue();
            if (rc.slot < 0 || rc.slot >= size) continue;
            try {
                RoleType role = RoleType.valueOf(roleName);
                inv.setItem(rc.slot, createRoleItem(role, rc));
            } catch (IllegalArgumentException ignored) {
            }
        }

        holder.setInventory(inv);
        player.openInventory(inv);
    }

    public void openPermissionList(Player player, Island island, RoleType role, int page) {
        PermissionRegistry registry = SkylliaAPI.getPermissionRegistry();
        List<PermissionNode> allPerms = new ArrayList<>();
        for (PermissionNode node : registry.nodes()) {
            if (node != null) allPerms.add(node);
        }

        int pageSize = config.getPermissionListPageSize();
        int totalPages = (int) Math.ceil(allPerms.size() / (double) pageSize);
        if (totalPages == 0) totalPages = 1;
        if (page < 0) page = 0;
        if (page >= totalPages) page = totalPages - 1;

        int start = page * pageSize;
        int end = Math.min(start + pageSize, allPerms.size());
        List<PermissionNode> pagePerms = start < end ? allPerms.subList(start, end) : new ArrayList<>();

        PermissionGuiHolder holder = new PermissionGuiHolder(PermissionGuiHolder.GuiMode.PERMISSION_LIST, role, page, pagePerms);
        int size = config.getPermissionListRows() * 9;
        Inventory inv = Bukkit.createInventory(holder, size, MM.deserialize(
                replacePlaceholders(config.getPermissionListTitle(), Map.of(
                        "%role%", role.name(),
                        "%page%", String.valueOf(page + 1),
                        "%total%", String.valueOf(totalPages)
                ))));

        CompiledPermissions compiled = island.getCompiledPermissions();
        compiled.ensureUpToDate(registry);

        int slot = 0;
        for (PermissionNode node : pagePerms) {
            if (node == null) continue;
            PermissionId pid = registry.getIfPresent(node.node());
            if (pid == null) continue;
            boolean value = compiled.has(registry, role, pid);
            if (slot < size) {
                inv.setItem(slot, createPermissionItem(node, value));
                slot++;
            }
        }

        if (page > 0) {
            inv.setItem(config.getNavPrevious().slot, createNavButton(config.getNavPrevious(), "nav:previous"));
        }
        if (page < totalPages - 1) {
            inv.setItem(config.getNavNext().slot, createNavButton(config.getNavNext(), "nav:next"));
        }
        inv.setItem(config.getNavBack().slot, createNavButton(config.getNavBack(), "nav:back"));

        PermissionGuiConfig.NavConfig ri = config.getRoleIndicator();
        PermissionGuiConfig.RoleItemConfig rc = config.getRoleItems().get(role.name());
        if (rc != null) {
            inv.setItem(ri.slot, createRoleItem(role, rc));
        } else {
            ItemStack item = new ItemStack(ri.material);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.displayName(MM.deserialize(replacePlaceholders(ri.displayName, Map.of("%role%", role.name()))));
                List<Component> lore = new ArrayList<>();
                for (String line : ri.lore) {
                    lore.add(MM.deserialize(replacePlaceholders(line, Map.of("%role%", role.name()))));
                }
                meta.lore(lore);
                applyCustomModelData(meta, ri.customModelData);
                meta.addItemFlags(ItemFlag.values());
                item.setItemMeta(meta);
            }
            inv.setItem(ri.slot, item);
        }

        holder.setInventory(inv);
        player.openInventory(inv);
    }

    private ItemStack createRoleItem(RoleType role, PermissionGuiConfig.RoleItemConfig rc) {
        ItemStack item = new ItemStack(rc.material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MM.deserialize(replacePlaceholders(rc.displayName, Map.of("%role%", role.name()))));
            List<Component> lore = new ArrayList<>();
            for (String line : rc.lore) {
                lore.add(MM.deserialize(replacePlaceholders(line, Map.of("%role%", role.name()))));
            }
            meta.lore(lore);
            applyCustomModelData(meta, rc.customModelData);
            meta.addItemFlags(ItemFlag.values());
            meta.getPersistentDataContainer().set(ROLE_KEY, PersistentDataType.STRING, role.name());
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createPermissionItem(PermissionNode node, boolean value) {
        PermissionGuiConfig.ItemConfig ic = value
                ? config.getPermissionItemEnabled()
                : config.getPermissionItemDisabled();

        ItemStack item = new ItemStack(ic.material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String permKey = node.node().getNamespace() + ":" + node.node().getKey();
            String desc = node.description() != null ? node.description() : "";

            meta.displayName(MM.deserialize(replacePlaceholders(ic.displayName, Map.of(
                    "%permission%", permKey,
                    "%description%", desc
            ))));

            List<Component> lore = new ArrayList<>();
            for (String line : ic.lore) {
                lore.add(MM.deserialize(replacePlaceholders(line, Map.of(
                        "%permission%", permKey,
                        "%description%", desc
                ))));
            }
            meta.lore(lore);
            applyCustomModelData(meta, ic.customModelData);
            meta.addItemFlags(ItemFlag.values());
            meta.getPersistentDataContainer().set(PERM_KEY, PersistentDataType.STRING, permKey);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createNavButton(PermissionGuiConfig.NavConfig nc, String action) {
        ItemStack item = new ItemStack(nc.material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MM.deserialize(replacePlaceholders(nc.displayName, Map.of())));
            List<Component> lore = new ArrayList<>();
            for (String line : nc.lore) {
                lore.add(MM.deserialize(replacePlaceholders(line, Map.of())));
            }
            meta.lore(lore);
            applyCustomModelData(meta, nc.customModelData);
            meta.addItemFlags(ItemFlag.values());
            meta.getPersistentDataContainer().set(NAV_KEY, PersistentDataType.STRING, action);
            item.setItemMeta(meta);
        }
        return item;
    }

    private void applyCustomModelData(ItemMeta meta, int cmd) {
        if (cmd >= 0) {
            meta.setCustomModelData(cmd);
        }
    }

    private String replacePlaceholders(String input, Map<String, String> placeholders) {
        if (input == null) return "";
        String result = input;
        Map<String, String> safe = placeholders != null ? placeholders : Map.of();
        for (Map.Entry<String, String> entry : safe.entrySet()) {
            String v = entry.getValue();
            result = result.replace(entry.getKey(), v != null ? v : "");
        }
        return result;
    }

    public boolean togglePermission(Island island, RoleType role, NamespacedKey permKey) {
        PermissionRegistry registry = SkylliaAPI.getPermissionRegistry();
        PermissionId pid = registry.getIfPresent(permKey);
        if (pid == null) return false;

        CompiledPermissions compiled = island.getCompiledPermissions();
        compiled.ensureUpToDate(registry);
        boolean current = compiled.has(registry, role, pid);
        boolean next = !current;

        IslandPermissionQuery query = plugin.getInterneAPI()
                .getIslandQuery()
                .getIslandPermissionQuery();
        if (query == null) return false;

        boolean success = query.set(island.getId(), role, pid, next);
        if (!success) return false;

        PermissionSet set = compiled.setFor(role);
        if (set == null) return false;
        set.set(pid, next);
        return true;
    }
}
