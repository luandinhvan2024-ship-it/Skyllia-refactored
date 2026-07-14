package fr.euphyllia.skyllia.gui;

import fr.euphyllia.skyllia.Skyllia;
import fr.euphyllia.skyllia.api.SkylliaAPI;
import fr.euphyllia.skyllia.api.permissions.PermissionId;
import fr.euphyllia.skyllia.api.permissions.PermissionNode;
import fr.euphyllia.skyllia.api.permissions.PermissionRegistry;
import fr.euphyllia.skyllia.api.skyblock.Island;
import fr.euphyllia.skyllia.api.skyblock.model.RoleType;
import fr.euphyllia.skyllia.permissions.PermissionService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class PermissionGui {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final NamespacedKey ACTION_KEY = new NamespacedKey(SkylliaAPI.getPlugin(), "pgui_action");
    private static final NamespacedKey CATEGORY_KEY = new NamespacedKey(SkylliaAPI.getPlugin(), "pgui_category");
    private static final NamespacedKey ROLE_KEY = new NamespacedKey(SkylliaAPI.getPlugin(), "pgui_role");
    private static final NamespacedKey PERM_KEY = new NamespacedKey(SkylliaAPI.getPlugin(), "pgui_perm");

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

    // ========== Screen 1: Category Select ==========

    public void openCategorySelect(@NotNull Player player, @NotNull Island island) {
        int size = config.getCategoryRows() * 9;
        PermissionGuiHolder holder = new PermissionGuiHolder(island, PermissionGuiHolder.Screen.CATEGORY_SELECT, null, null, 0);
        Inventory inv = Bukkit.createInventory(holder, size, MM.deserialize(config.getCategoryTitle()));

        fillBackground(inv, config.getCategoryBackground(), size);

        for (Map.Entry<String, PermissionGuiConfig.ItemConfig> entry : config.getCategoryItems().entrySet()) {
            PermissionGuiConfig.ItemConfig ic = entry.getValue();
            if (ic instanceof PermissionGuiConfig.NavConfig nc && nc.slot >= 0 && nc.slot < size) {
                inv.setItem(nc.slot, createItem(ic, Map.of(
                        ACTION_KEY, "select_category",
                        CATEGORY_KEY, entry.getKey()
                )));
            }
        }

        PermissionGuiConfig.NavConfig close = config.getCategoryClose();
        if (close.slot >= 0 && close.slot < size) {
            inv.setItem(close.slot, createItem(close, Map.of(ACTION_KEY, "close")));
        }

        holder.setInventory(inv);
        player.openInventory(inv);
    }

    // ========== Screen 2: Role Select ==========

    public void openRoleSelect(@NotNull Player player, @NotNull Island island, @NotNull String category) {
        int size = config.getRoleRows() * 9;
        String title = replacePlaceholders(config.getRoleTitle(), Map.of("%category%", category));
        PermissionGuiHolder holder = new PermissionGuiHolder(island, PermissionGuiHolder.Screen.ROLE_SELECT, category, null, 0);
        Inventory inv = Bukkit.createInventory(holder, size, MM.deserialize(title));

        fillBackground(inv, config.getRoleBackground(), size);

        for (Map.Entry<String, PermissionGuiConfig.RoleItemConfig> entry : config.getRoleItems().entrySet()) {
            PermissionGuiConfig.RoleItemConfig rc = entry.getValue();
            if (rc.slot < 0 || rc.slot >= size) continue;
            try {
                RoleType role = RoleType.valueOf(entry.getKey());
                inv.setItem(rc.slot, createItem(rc, Map.of(
                        ACTION_KEY, "select_role",
                        ROLE_KEY, role.name()
                )));
            } catch (IllegalArgumentException ignored) {
            }
        }

        PermissionGuiConfig.NavConfig back = config.getRoleBack();
        if (back.slot >= 0 && back.slot < size) {
            inv.setItem(back.slot, createItem(back, Map.of(ACTION_KEY, "back_to_categories")));
        }

        PermissionGuiConfig.NavConfig close = config.getRoleClose();
        if (close.slot >= 0 && close.slot < size) {
            inv.setItem(close.slot, createItem(close, Map.of(ACTION_KEY, "close")));
        }

        holder.setInventory(inv);
        player.openInventory(inv);
    }

    // ========== Screen 3: Permission List ==========

    public void openPermissionList(@NotNull Player player, @NotNull Island island,
                                   @NotNull String category, @NotNull RoleType role, int page) {
        List<String> permKeys = config.getPermissionsByCategory(category);
        int pageSize = config.getPermListPageSize();
        int totalPages = (int) Math.ceil(permKeys.size() / (double) pageSize);
        if (totalPages == 0) totalPages = 1;
        if (page < 0) page = 0;
        if (page >= totalPages) page = totalPages - 1;

        int start = page * pageSize;
        int end = Math.min(start + pageSize, permKeys.size());
        List<String> pageKeys = start < end ? permKeys.subList(start, end) : new ArrayList<>();

        int size = config.getPermListRows() * 9;
        String title = replacePlaceholders(config.getPermListTitle(), Map.of(
                "%category%", category,
                "%role%", role.name(),
                "%page%", String.valueOf(page + 1),
                "%total%", String.valueOf(totalPages)
        ));
        PermissionGuiHolder holder = new PermissionGuiHolder(island, PermissionGuiHolder.Screen.PERMISSION_LIST, category, role, page);
        Inventory inv = Bukkit.createInventory(holder, size, MM.deserialize(title));

        PermissionRegistry registry = SkylliaAPI.getPermissionRegistry();

        int slot = 0;
        int permAreaEnd = Math.min(pageSize, size);
        for (String permKey : pageKeys) {
            if (slot >= permAreaEnd) break;

            NamespacedKey nk = NamespacedKey.fromString(permKey);
            if (nk == null) continue;
            PermissionId pid = registry.getIfPresent(nk);
            if (pid == null) continue;

            boolean value = PermissionService.get().hasPermission(island, role, pid);
            PermissionGuiConfig.PermissionItemConfig pic = config.getPermissionItem(permKey);
            if (pic == null) continue;

            PermissionGuiConfig.ItemConfig ic = value ? pic.enabled : pic.disabled;
            inv.setItem(slot, createItem(ic, Map.of(
                    ACTION_KEY, "toggle_perm",
                    PERM_KEY, permKey
            )));
            slot++;
        }

        fillBackground(inv, config.getPermListBackground(), size, permAreaEnd);

        if (page > 0) {
            PermissionGuiConfig.NavConfig prev = config.getPermPrevPage();
            inv.setItem(prev.slot, createItem(prev, Map.of(ACTION_KEY, "prev_page")));
        }
        if (page < totalPages - 1) {
            PermissionGuiConfig.NavConfig next = config.getPermNextPage();
            inv.setItem(next.slot, createItem(next, Map.of(ACTION_KEY, "next_page")));
        }

        PermissionGuiConfig.NavConfig back = config.getPermBack();
        inv.setItem(back.slot, createItem(back, Map.of(ACTION_KEY, "back_to_roles")));

        PermissionGuiConfig.NavConfig close = config.getPermClose();
        inv.setItem(close.slot, createItem(close, Map.of(ACTION_KEY, "close")));

        holder.setInventory(inv);
        player.openInventory(inv);
    }

    // ========== Click Handler ==========

    public void handleClick(@NotNull Player player, @NotNull PermissionGuiHolder holder, @NotNull ItemStack clicked) {
        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) return;

        var pdc = meta.getPersistentDataContainer();
        String action = pdc.get(ACTION_KEY, PersistentDataType.STRING);
        if (action == null) return;

        Island island = holder.getIsland();
        if (island == null) {
            player.closeInventory();
            return;
        }

        switch (action) {
            case "select_category" -> {
                String category = pdc.get(CATEGORY_KEY, PersistentDataType.STRING);
                if (category != null) {
                    openRoleSelect(player, island, category);
                }
            }
            case "select_role" -> {
                String roleStr = pdc.get(ROLE_KEY, PersistentDataType.STRING);
                String category = holder.getCategory();
                if (roleStr != null && category != null) {
                    try {
                        RoleType role = RoleType.valueOf(roleStr);
                        openPermissionList(player, island, category, role, 0);
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }
            case "toggle_perm" -> {
                RoleType role = holder.getSelectedRole();
                String category = holder.getCategory();
                if (role == null || category == null) return;
                String permKey = pdc.get(PERM_KEY, PersistentDataType.STRING);
                if (permKey == null) return;
                NamespacedKey nk = NamespacedKey.fromString(permKey);
                if (nk == null) return;
                PermissionId pid = PermissionService.get().getPermissionId(nk);
                if (pid == null) return;

                PermissionService.get().togglePermission(island, role, pid);
                openPermissionList(player, island, category, role, holder.getPage());
            }
            case "prev_page" -> {
                RoleType role = holder.getSelectedRole();
                String category = holder.getCategory();
                if (role != null && category != null) {
                    openPermissionList(player, island, category, role, holder.getPage() - 1);
                }
            }
            case "next_page" -> {
                RoleType role = holder.getSelectedRole();
                String category = holder.getCategory();
                if (role != null && category != null) {
                    openPermissionList(player, island, category, role, holder.getPage() + 1);
                }
            }
            case "back_to_categories" -> openCategorySelect(player, island);
            case "back_to_roles" -> {
                String category = holder.getCategory();
                if (category != null) {
                    openRoleSelect(player, island, category);
                }
            }
            case "close" -> player.closeInventory();
        }
    }

    // ========== Helpers ==========

    private ItemStack createItem(PermissionGuiConfig.ItemConfig ic, Map<NamespacedKey, String> pdcData) {
        ItemStack item = new ItemStack(ic.material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MM.deserialize(ic.displayName));
            List<Component> lore = new ArrayList<>();
            for (String line : ic.lore) {
                lore.add(MM.deserialize(line));
            }
            meta.lore(lore);
            meta.addItemFlags(ItemFlag.values());
            if (pdcData != null) {
                for (Map.Entry<NamespacedKey, String> entry : pdcData.entrySet()) {
                    meta.getPersistentDataContainer().set(entry.getKey(), PersistentDataType.STRING, entry.getValue());
                }
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private void fillBackground(Inventory inv, PermissionGuiConfig.ItemConfig bg, int size) {
        if (bg == null) return;
        ItemStack bgItem = createItem(bg, null);
        for (int i = 0; i < size; i++) {
            if (inv.getItem(i) == null) {
                inv.setItem(i, bgItem);
            }
        }
    }

    private void fillBackground(Inventory inv, PermissionGuiConfig.ItemConfig bg, int size, int startFrom) {
        if (bg == null) return;
        ItemStack bgItem = createItem(bg, null);
        for (int i = startFrom; i < size; i++) {
            if (inv.getItem(i) == null) {
                inv.setItem(i, bgItem);
            }
        }
    }

    private String replacePlaceholders(String input, Map<String, String> placeholders) {
        if (input == null) return "";
        String result = input;
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                result = result.replace(entry.getKey(), entry.getValue() != null ? entry.getValue() : "");
            }
        }
        return result;
    }
}
