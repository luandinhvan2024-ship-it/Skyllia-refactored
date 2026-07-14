package fr.euphyllia.skyllia.gui;

import fr.euphyllia.skyllia.api.SkylliaAPI;
import fr.euphyllia.skyllia.api.permissions.CompiledPermissions;
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

/**
 * Pure view layer for the Permission system.
 * Contains NO permission logic. All operations delegate to PermissionService.
 *
 * Architecture:
 *   GUI (this) → PermissionService → IslandPermissionQuery + CompiledPermissions → Database
 *
 * The GUI only:
 *   - Displays data (reads via PermissionService.hasPermission)
 *   - Receives clicks (calls PermissionService.setPermission/togglePermission)
 */
public final class PermissionGui {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final NamespacedKey ACTION_KEY = new NamespacedKey(SkylliaAPI.getPlugin(), "pgui_action");
    private static final NamespacedKey ROLE_KEY = new NamespacedKey(SkylliaAPI.getPlugin(), "pgui_role");
    private static final NamespacedKey PERM_KEY = new NamespacedKey(SkylliaAPI.getPlugin(), "pgui_perm");
    private static final NamespacedKey PAGE_KEY = new NamespacedKey(SkylliaAPI.getPlugin(), "pgui_page");

    private static final int PAGE_SIZE = 36;

    public static void open(@NotNull Player player, @NotNull Island island) {
        openRoleSelect(player, island);
    }

    public static void openRoleSelect(@NotNull Player player, @NotNull Island island) {
        Inventory inv = Bukkit.createInventory(
                new PermissionGuiHolder(island, null, 0),
                27,
                MM.deserialize("<dark_gray>» <gradient:#4FC3F7:#0288D1><bold>Select Role</bold></gradient>")
        );

        RoleType[] roles = {RoleType.OWNER, RoleType.CO_OWNER, RoleType.MODERATOR, RoleType.MEMBER, RoleType.VISITOR, RoleType.BAN};
        int[] slots = {11, 13, 15, 20, 22, 24};
        org.bukkit.Material[] mats = {
                org.bukkit.Material.GOLDEN_HELMET,
                org.bukkit.Material.DIAMOND_HELMET,
                org.bukkit.Material.IRON_HELMET,
                org.bukkit.Material.CHAINMAIL_HELMET,
                org.bukkit.Material.LEATHER_HELMET,
                org.bukkit.Material.BARRIER
        };
        String[] colors = {"<gold>", "<aqua>", "<white>", "<green>", "<yellow>", "<red>"};

        for (int i = 0; i < roles.length; i++) {
            ItemStack item = new ItemStack(mats[i]);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.displayName(MM.deserialize(colors[i] + "<bold>" + roles[i].name() + "</bold>"));
                List<Component> lore = new ArrayList<>();
                lore.add(MM.deserialize("<gray>Click to manage permissions"));
                meta.lore(lore);
                meta.addItemFlags(ItemFlag.values());
                meta.getPersistentDataContainer().set(ACTION_KEY, PersistentDataType.STRING, "select_role");
                meta.getPersistentDataContainer().set(ROLE_KEY, PersistentDataType.STRING, roles[i].name());
                item.setItemMeta(meta);
            }
            inv.setItem(slots[i], item);
        }

        ItemStack close = new ItemStack(org.bukkit.Material.BARRIER);
        ItemMeta closeMeta = close.getItemMeta();
        if (closeMeta != null) {
            closeMeta.displayName(MM.deserialize("<red><bold>Close</bold></red>"));
            closeMeta.addItemFlags(ItemFlag.values());
            closeMeta.getPersistentDataContainer().set(ACTION_KEY, PersistentDataType.STRING, "close");
            close.setItemMeta(closeMeta);
        }
        inv.setItem(26, close);

        player.openInventory(inv);
    }

    public static void openPermissionList(@NotNull Player player, @NotNull Island island, @NotNull RoleType role, int page) {
        PermissionRegistry registry = SkylliaAPI.getPermissionRegistry();
        List<PermissionNode> allPerms = new ArrayList<>();
        for (PermissionNode node : registry.nodes()) {
            if (node != null) allPerms.add(node);
        }

        int totalPages = (int) Math.ceil(allPerms.size() / (double) PAGE_SIZE);
        if (totalPages == 0) totalPages = 1;
        if (page < 0) page = 0;
        if (page >= totalPages) page = totalPages - 1;

        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, allPerms.size());
        List<PermissionNode> pagePerms = start < end ? allPerms.subList(start, end) : new ArrayList<>();

        Inventory inv = Bukkit.createInventory(
                new PermissionGuiHolder(island, role, page),
                54,
                MM.deserialize("<dark_gray>» <gradient:#4FC3F7:#0288D1><bold>" + role.name() + " Permissions</bold></gradient> <gray>(Page " + (page + 1) + "/" + totalPages + ")</gray>")
        );

        CompiledPermissions compiled = island.getCompiledPermissions();
        compiled.ensureUpToDate(registry);

        int slot = 0;
        for (PermissionNode node : pagePerms) {
            if (node == null) continue;
            PermissionId pid = registry.getIfPresent(node.node());
            if (pid == null) continue;

            boolean value = PermissionService.get().hasPermission(island, role, pid);
            String permKey = node.node().getNamespace() + ":" + node.node().getKey();

            ItemStack item = new ItemStack(value ? org.bukkit.Material.LIME_DYE : org.bukkit.Material.GRAY_DYE);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.displayName(MM.deserialize("<white>" + permKey));
                List<Component> lore = new ArrayList<>();
                if (value) {
                    lore.add(MM.deserialize("<green>✓ Enabled"));
                    lore.add(MM.deserialize("<yellow>Click to disable"));
                } else {
                    lore.add(MM.deserialize("<red>✗ Disabled"));
                    lore.add(MM.deserialize("<yellow>Click to enable"));
                }
                String desc = node.description();
                if (desc != null && !desc.isEmpty()) {
                    lore.add(MM.deserialize("<dark_gray>" + desc));
                }
                meta.lore(lore);
                meta.addItemFlags(ItemFlag.values());
                meta.getPersistentDataContainer().set(ACTION_KEY, PersistentDataType.STRING, "toggle_perm");
                meta.getPersistentDataContainer().set(PERM_KEY, PersistentDataType.STRING, permKey);
                item.setItemMeta(meta);
            }
            inv.setItem(slot, item);
            slot++;
        }

        ItemStack navBg = new ItemStack(org.bukkit.Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta navBgMeta = navBg.getItemMeta();
        if (navBgMeta != null) {
            navBgMeta.displayName(Component.empty());
            navBg.setItemMeta(navBgMeta);
        }
        for (int i = 45; i < 54; i++) {
            inv.setItem(i, navBg);
        }

        if (page > 0) {
            ItemStack prev = new ItemStack(org.bukkit.Material.ARROW);
            ItemMeta prevMeta = prev.getItemMeta();
            if (prevMeta != null) {
                prevMeta.displayName(MM.deserialize("<yellow>← Previous Page"));
                prevMeta.addItemFlags(ItemFlag.values());
                prevMeta.getPersistentDataContainer().set(ACTION_KEY, PersistentDataType.STRING, "prev_page");
                prev.setItemMeta(prevMeta);
            }
            inv.setItem(45, prev);
        }

        if (page < totalPages - 1) {
            ItemStack next = new ItemStack(org.bukkit.Material.ARROW);
            ItemMeta nextMeta = next.getItemMeta();
            if (nextMeta != null) {
                nextMeta.displayName(MM.deserialize("<yellow>Next Page →"));
                nextMeta.addItemFlags(ItemFlag.values());
                nextMeta.getPersistentDataContainer().set(ACTION_KEY, PersistentDataType.STRING, "next_page");
                next.setItemMeta(nextMeta);
            }
            inv.setItem(53, next);
        }

        ItemStack back = new ItemStack(org.bukkit.Material.BARRIER);
        ItemMeta backMeta = back.getItemMeta();
        if (backMeta != null) {
            backMeta.displayName(MM.deserialize("<red>Back to Role Selection"));
            backMeta.addItemFlags(ItemFlag.values());
            backMeta.getPersistentDataContainer().set(ACTION_KEY, PersistentDataType.STRING, "back");
            back.setItemMeta(backMeta);
        }
        inv.setItem(48, back);

        ItemStack close = new ItemStack(org.bukkit.Material.BARRIER);
        ItemMeta closeMeta = close.getItemMeta();
        if (closeMeta != null) {
            closeMeta.displayName(MM.deserialize("<red><bold>Close</bold></red>"));
            closeMeta.addItemFlags(ItemFlag.values());
            closeMeta.getPersistentDataContainer().set(ACTION_KEY, PersistentDataType.STRING, "close");
            close.setItemMeta(closeMeta);
        }
        inv.setItem(50, close);

        player.openInventory(inv);
    }

    public static void handleClick(@NotNull Player player, @NotNull PermissionGuiHolder holder, @NotNull ItemStack clicked) {
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
            case "select_role" -> {
                String roleStr = pdc.get(ROLE_KEY, PersistentDataType.STRING);
                if (roleStr == null) return;
                try {
                    RoleType role = RoleType.valueOf(roleStr);
                    openPermissionList(player, island, role, 0);
                } catch (IllegalArgumentException ignored) {
                }
            }
            case "toggle_perm" -> {
                RoleType role = holder.getSelectedRole();
                if (role == null) return;
                String permKeyStr = pdc.get(PERM_KEY, PersistentDataType.STRING);
                if (permKeyStr == null) return;
                NamespacedKey key = NamespacedKey.fromString(permKeyStr);
                if (key == null) return;

                PermissionId pid = PermissionService.get().getPermissionId(key);
                if (pid == null) return;

                PermissionService.get().togglePermission(island, role, pid);
                openPermissionList(player, island, role, holder.getPage());
            }
            case "prev_page" -> {
                RoleType role = holder.getSelectedRole();
                if (role == null) return;
                openPermissionList(player, island, role, holder.getPage() - 1);
            }
            case "next_page" -> {
                RoleType role = holder.getSelectedRole();
                if (role == null) return;
                openPermissionList(player, island, role, holder.getPage() + 1);
            }
            case "back" -> openRoleSelect(player, island);
            case "close" -> player.closeInventory();
        }
    }
}
