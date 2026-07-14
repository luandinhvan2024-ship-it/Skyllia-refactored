package fr.euphyllia.skyllia.gui;

import fr.euphyllia.skyllia.Skyllia;
import fr.euphyllia.skyllia.api.SkylliaAPI;
import fr.euphyllia.skyllia.api.permissions.CompiledPermissions;
import fr.euphyllia.skyllia.api.permissions.PermissionId;
import fr.euphyllia.skyllia.api.permissions.PermissionNode;
import fr.euphyllia.skyllia.api.permissions.PermissionRegistry;
import fr.euphyllia.skyllia.api.database.IslandPermissionQuery;
import fr.euphyllia.skyllia.api.permissions.PermissionSet;
import fr.euphyllia.skyllia.api.skyblock.Island;
import fr.euphyllia.skyllia.api.skyblock.model.RoleType;
import fr.euphyllia.skyllia.configuration.ConfigLoader;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class PermissionGui {

    private static final int PAGE_SIZE = 36;
    private static final MiniMessage MM = MiniMessage.miniMessage();

    public static void openRoleSelect(Player player, Island island) {
        PermissionGuiHolder holder = new PermissionGuiHolder(PermissionGuiHolder.GuiMode.ROLE_SELECT, null, 0, null);
        Inventory inv = Bukkit.createInventory(holder, 27, MM.deserialize(
                ConfigLoader.language.translateRaw(player.locale(), "island.permission.gui.title-roles", null)));

        int[] slots = {11, 13, 15, 20, 22, 24};
        RoleType[] roles = {RoleType.OWNER, RoleType.CO_OWNER, RoleType.MODERATOR, RoleType.MEMBER, RoleType.VISITOR, RoleType.BAN};

        for (int i = 0; i < roles.length; i++) {
            inv.setItem(slots[i], createRoleItem(roles[i]));
        }

        holder.setInventory(inv);
        player.openInventory(inv);
    }

    public static void openPermissionList(Player player, Island island, RoleType role, int page) {
        PermissionRegistry registry = SkylliaAPI.getPermissionRegistry();
        List<PermissionNode> allPerms = new ArrayList<>(registry.nodes());

        int totalPages = (int) Math.ceil(allPerms.size() / (double) PAGE_SIZE);
        if (totalPages == 0) totalPages = 1;
        if (page < 0) page = 0;
        if (page >= totalPages) page = totalPages - 1;

        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, allPerms.size());
        List<PermissionNode> pagePerms = allPerms.subList(start, end);

        PermissionGuiHolder holder = new PermissionGuiHolder(PermissionGuiHolder.GuiMode.PERMISSION_LIST, role, page, pagePerms);
        Inventory inv = Bukkit.createInventory(holder, 54, MM.deserialize(
                ConfigLoader.language.translateRaw(player.locale(), "island.permission.gui.title-permissions",
                        java.util.Map.of("%role%", role.name(), "%page%", String.valueOf(page + 1), "%total%", String.valueOf(totalPages)))));

        CompiledPermissions compiled = island.getCompiledPermissions();
        compiled.ensureUpToDate(registry);

        for (int i = 0; i < pagePerms.size(); i++) {
            PermissionNode node = pagePerms.get(i);
            PermissionId pid = registry.getIfPresent(node.node());
            boolean value = compiled.has(registry, role, pid);
            inv.setItem(i, createPermissionItem(node, value, role));
        }

        // Navigation buttons
        if (page > 0) {
            inv.setItem(45, createNavButton(Material.ARROW,
                    ConfigLoader.language.translateRaw(player.locale(), "island.permission.gui.nav-previous", null),
                    "nav:previous"));
        }
        if (page < totalPages - 1) {
            inv.setItem(53, createNavButton(Material.ARROW,
                    ConfigLoader.language.translateRaw(player.locale(), "island.permission.gui.nav-next", null),
                    "nav:next"));
        }

        // Back button
        inv.setItem(49, createNavButton(Material.BARRIER,
                ConfigLoader.language.translateRaw(player.locale(), "island.permission.gui.nav-back", null),
                "nav:back"));

        // Info item (role indicator)
        inv.setItem(4, createRoleItem(role));

        holder.setInventory(inv);
        player.openInventory(inv);
    }

    private static ItemStack createRoleItem(RoleType role) {
        Material mat = switch (role) {
            case OWNER -> Material.GOLDEN_HELMET;
            case CO_OWNER -> Material.DIAMOND_HELMET;
            case MODERATOR -> Material.IRON_HELMET;
            case MEMBER -> Material.CHAINMAIL_HELMET;
            case VISITOR -> Material.LEATHER_HELMET;
            case BAN -> Material.BARRIER;
        };

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MM.deserialize("<gold>" + role.name()));
            List<Component> lore = new ArrayList<>();
            lore.add(MM.deserialize("<gray>" + ConfigLoader.language.translateRaw(
                    java.util.Locale.getDefault(), "island.permission.gui.lore-click-to-view", null)));
            meta.lore(lore);

            // Store role in PDC
            meta.getPersistentDataContainer().set(
                    new NamespacedKey(SkylliaAPI.getPlugin(), "gui_role"),
                    PersistentDataType.STRING,
                    role.name()
            );
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack createPermissionItem(PermissionNode node, boolean value, RoleType role) {
        Material mat = value ? Material.LIME_DYE : Material.GRAY_DYE;
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String key = node.node().getNamespace() + ":" + node.node().getKey();
            meta.displayName(MM.deserialize("<white>" + key));

            List<Component> lore = new ArrayList<>();
            String statusKey = value ? "island.permission.gui.lore-enabled" : "island.permission.gui.lore-disabled";
            String statusColor = value ? "<green>" : "<red>";
            lore.add(MM.deserialize(statusColor + ConfigLoader.language.translateRaw(
                    java.util.Locale.getDefault(), statusKey, null)));

            // Description
            String desc = node.description();
            if (desc != null && !desc.isEmpty()) {
                lore.add(MM.deserialize("<dark_gray>" + desc));
            }

            lore.add(MM.deserialize(""));
            lore.add(MM.deserialize("<yellow>" + ConfigLoader.language.translateRaw(
                    java.util.Locale.getDefault(), "island.permission.gui.lore-click-to-toggle", null)));

            meta.lore(lore);

            // Store permission key in PDC
            meta.getPersistentDataContainer().set(
                    new NamespacedKey(SkylliaAPI.getPlugin(), "gui_perm"),
                    PersistentDataType.STRING,
                    key
            );
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack createNavButton(Material mat, String label, String action) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MM.deserialize(label));

            meta.getPersistentDataContainer().set(
                    new NamespacedKey(SkylliaAPI.getPlugin(), "gui_nav"),
                    PersistentDataType.STRING,
                    action
            );
            item.setItemMeta(meta);
        }
        return item;
    }

    public static boolean togglePermission(Island island, RoleType role, NamespacedKey permKey) {
        PermissionRegistry registry = SkylliaAPI.getPermissionRegistry();
        PermissionId pid = registry.getIfPresent(permKey);
        if (pid == null) return false;

        CompiledPermissions compiled = island.getCompiledPermissions();
        compiled.ensureUpToDate(registry);
        boolean current = compiled.has(registry, role, pid);
        boolean next = !current;

        IslandPermissionQuery query = Skyllia.getInstance()
                .getInterneAPI()
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
