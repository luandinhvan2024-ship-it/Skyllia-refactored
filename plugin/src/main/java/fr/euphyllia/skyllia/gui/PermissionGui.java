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
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Permission GUI view layer - modeled after SuperiorSkyblock2's MenuIslandPrivileges.
 *
 * Architecture:
 *   GUI (this) → PermissionService → IslandPermissionQuery + CompiledPermissions → Database
 *
 * This class ONLY:
 *   - Displays data (reads via PermissionService.hasPermission)
 *   - Receives clicks (calls PermissionService.setPermission)
 *   - Reads all display info (name, icon, lore) from permissions-gui.yml
 *
 * Contains ZERO permission logic. Contains ZERO database access.
 */
public final class PermissionGui {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final NamespacedKey ACTION_KEY = new NamespacedKey(SkylliaAPI.getPlugin(), "pgui_action");
    private static final NamespacedKey PERM_KEY = new NamespacedKey(SkylliaAPI.getPlugin(), "pgui_perm");
    private static final NamespacedKey ROLE_KEY = new NamespacedKey(SkylliaAPI.getPlugin(), "pgui_role");
    private static final NamespacedKey CAT_KEY = new NamespacedKey(SkylliaAPI.getPlugin(), "pgui_cat");
    private static final NamespacedKey PAGE_KEY = new NamespacedKey(SkylliaAPI.getPlugin(), "pgui_page");

    private static final int PAGE_SIZE = 36;

    private final Skyllia plugin;
    private FileConfiguration config;

    public PermissionGui(Skyllia plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        File file = new File(plugin.getDataFolder(), "config/permissions-gui.yml");
        if (!file.exists()) {
            plugin.saveResource("config/permissions-gui.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(file);
        InputStream defaults = plugin.getResource("config/permissions-gui.yml");
        if (defaults != null) {
            config.setDefaults(YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaults, StandardCharsets.UTF_8)));
        }
    }

    public void open(@NotNull Player player, @NotNull Island island) {
        openRoleSelect(player, island);
    }

    public void openRoleSelect(@NotNull Player player, @NotNull Island island) {
        String title = config.getString("titles.role-select", "<white>Select Role");
        Inventory inv = Bukkit.createInventory(new PermissionGuiHolder(island, null, 0), 27, MM.deserialize(title));

        ItemStack bg = createBackgroundItem();
        for (int i = 0; i < 27; i++) inv.setItem(i, bg);

        ConfigurationSection roles = config.getConfigurationSection("roles");
        if (roles != null) {
            int[] slots = {10, 12, 14, 16, 19, 21, 23, 25};
            int idx = 0;
            for (String roleStr : roles.getKeys(false)) {
                if (idx >= slots.length) break;
                try {
                    RoleType role = RoleType.valueOf(roleStr);
                    ConfigurationSection rs = roles.getConfigurationSection(roleStr);
                    if (rs == null) continue;
                    ItemStack item = createSimpleItem(rs, Map.of());
                    ItemMeta meta = item.getItemMeta();
                    if (meta != null) {
                        meta.getPersistentDataContainer().set(ACTION_KEY, PersistentDataType.STRING, "select_role");
                        meta.getPersistentDataContainer().set(ROLE_KEY, PersistentDataType.STRING, role.name());
                        item.setItemMeta(meta);
                    }
                    inv.setItem(slots[idx], item);
                    idx++;
                } catch (IllegalArgumentException ignored) {
                }
            }
        }

        inv.setItem(26, createNavButton("close", "navigation.close"));
        player.openInventory(inv);
    }

    public void openCategoryList(@NotNull Player player, @NotNull Island island, @NotNull RoleType role) {
        String title = config.getString("titles.main", "<white>Categories");
        Inventory inv = Bukkit.createInventory(new PermissionGuiHolder(island, role, 0), 27, MM.deserialize(title));

        ItemStack bg = createBackgroundItem();
        for (int i = 0; i < 27; i++) inv.setItem(i, bg);

        ConfigurationSection cats = config.getConfigurationSection("categories");
        if (cats != null) {
            for (String catKey : cats.getKeys(false)) {
                ConfigurationSection cs = cats.getConfigurationSection(catKey);
                if (cs == null) continue;
                int slot = cs.getInt("slot", -1);
                if (slot < 0 || slot >= 27) continue;

                ItemStack item = createSimpleItem(cs, Map.of());
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.getPersistentDataContainer().set(ACTION_KEY, PersistentDataType.STRING, "open_category");
                    meta.getPersistentDataContainer().set(CAT_KEY, PersistentDataType.STRING, catKey);
                    meta.getPersistentDataContainer().set(ROLE_KEY, PersistentDataType.STRING, role.name());
                    item.setItemMeta(meta);
                }
                inv.setItem(slot, item);
            }
        }

        inv.setItem(22, createRoleIndicator(role));
        inv.setItem(26, createNavButton("close", "navigation.close"));
        player.openInventory(inv);
    }

    public void openPermissionList(@NotNull Player player, @NotNull Island island, @NotNull RoleType role, @NotNull String category, int page) {
        ConfigurationSection catSec = config.getConfigurationSection("categories." + category);
        if (catSec == null) return;

        List<String> permKeys = catSec.getStringList("permissions");
        String catName = catSec.getString("display-name", category);

        int totalPages = (int) Math.ceil(permKeys.size() / (double) PAGE_SIZE);
        if (totalPages == 0) totalPages = 1;
        if (page < 0) page = 0;
        if (page >= totalPages) page = totalPages - 1;

        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, permKeys.size());
        List<String> pageKeys = start < end ? permKeys.subList(start, end) : new ArrayList<>();

        String title = config.getString("titles.category", "<white>%category% (%role%)")
                .replace("%category%", stripColor(catName))
                .replace("%role%", role.name());
        Inventory inv = Bukkit.createInventory(new PermissionGuiHolder(island, role, page), 54, MM.deserialize(title));

        ItemStack bg = createBackgroundItem();
        for (int i = 36; i < 54; i++) inv.setItem(i, bg);

        PermissionRegistry registry = SkylliaAPI.getPermissionRegistry();
        PermissionService svc = PermissionService.get();

        int slot = 0;
        for (String permKeyStr : pageKeys) {
            NamespacedKey key = NamespacedKey.fromString(permKeyStr);
            if (key == null) continue;
            PermissionId pid = registry.getIfPresent(key);
            if (pid == null) continue;

            boolean enabled = svc.hasPermission(island, role, pid);
            ItemStack item = createPermissionItem(permKeyStr, enabled, role);
            if (slot < 36) {
                inv.setItem(slot, item);
                slot++;
            }
        }

        if (page > 0) {
            inv.setItem(45, createNavButton("prev_page", "navigation.previous-page"));
        }
        if (page < totalPages - 1) {
            inv.setItem(53, createNavButton("next_page", "navigation.next-page"));
        }

        ItemStack back = createNavButton("back", "navigation.back");
        ItemMeta backMeta = back.getItemMeta();
        if (backMeta != null) {
            backMeta.getPersistentDataContainer().set(ROLE_KEY, PersistentDataType.STRING, role.name());
            back.setItemMeta(backMeta);
        }
        inv.setItem(48, back);

        inv.setItem(50, createNavButton("close", "navigation.close"));
        inv.setItem(4, createRoleIndicator(role));

        player.openInventory(inv);
    }

    public void handleClick(@NotNull Player player, @NotNull PermissionGuiHolder holder, @NotNull ItemStack clicked, boolean leftClick) {
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
                    openCategoryList(player, island, role);
                } catch (IllegalArgumentException ignored) {
                }
            }
            case "open_category" -> {
                String roleStr = pdc.get(ROLE_KEY, PersistentDataType.STRING);
                String cat = pdc.get(CAT_KEY, PersistentDataType.STRING);
                if (roleStr == null || cat == null) return;
                try {
                    RoleType role = RoleType.valueOf(roleStr);
                    openPermissionList(player, island, role, cat, 0);
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

                boolean current = PermissionService.get().hasPermission(island, role, pid);
                PermissionService.get().setPermission(island, role, pid, !current);

                String cat = pdc.get(CAT_KEY, PersistentDataType.STRING);
                if (cat != null) {
                    openPermissionList(player, island, role, cat, holder.getPage());
                }
            }
            case "prev_page" -> {
                RoleType role = holder.getSelectedRole();
                if (role == null) return;
                String cat = pdc.get(CAT_KEY, PersistentDataType.STRING);
                if (cat == null) return;
                openPermissionList(player, island, role, cat, holder.getPage() - 1);
            }
            case "next_page" -> {
                RoleType role = holder.getSelectedRole();
                if (role == null) return;
                String cat = pdc.get(CAT_KEY, PersistentDataType.STRING);
                if (cat == null) return;
                openPermissionList(player, island, role, cat, holder.getPage() + 1);
            }
            case "back" -> {
                String roleStr = pdc.get(ROLE_KEY, PersistentDataType.STRING);
                if (roleStr != null) {
                    try {
                        RoleType role = RoleType.valueOf(roleStr);
                        openCategoryList(player, island, role);
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }
            case "close" -> player.closeInventory();
        }
    }

    private ItemStack createPermissionItem(@NotNull String permKeyStr, boolean enabled, @NotNull RoleType role) {
        ConfigurationSection permSec = config.getConfigurationSection("permissions." + permKeyStr);
        String templatePath = enabled ? "templates.permission-enabled" : "templates.permission-disabled";
        ConfigurationSection template = config.getConfigurationSection(templatePath);

        Material material;
        String name;
        List<String> lore;
        int cmd = -1;

        if (permSec != null) {
            material = parseMaterial(permSec.getString("material", "STONE"), Material.STONE);
            name = permSec.getString("display-name", "<white>" + permKeyStr);
            String desc = permSec.getString("description", "");
            cmd = permSec.getInt("custom-model-data", -1);

            lore = new ArrayList<>();
            if (template != null) {
                lore = template.getStringList("lore");
            }
            lore = new ArrayList<>();
            if (template != null) {
                for (String line : template.getStringList("lore")) {
                    lore.add(line);
                }
            }
            for (int i = 0; i < lore.size(); i++) {
                lore.set(i, lore.get(i)
                        .replace("%name%", stripColor(name))
                        .replace("%description%", desc)
                        .replace("%role%", role.name()));
            }
        } else {
            material = template != null ? parseMaterial(template.getString("material", "STONE"), Material.STONE) : Material.STONE;
            name = template != null ? template.getString("name", "<white>" + permKeyStr) : "<white>" + permKeyStr;
            lore = template != null ? template.getStringList("lore") : new ArrayList<>();
            cmd = template != null ? template.getInt("custom-model-data", -1) : -1;
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MM.deserialize(name));
            List<Component> loreComp = new ArrayList<>();
            for (String line : lore) {
                loreComp.add(MM.deserialize(line));
            }
            meta.lore(loreComp);
            if (cmd >= 0) meta.setCustomModelData(cmd);
            meta.addItemFlags(ItemFlag.values());
            meta.getPersistentDataContainer().set(ACTION_KEY, PersistentDataType.STRING, "toggle_perm");
            meta.getPersistentDataContainer().set(PERM_KEY, PersistentDataType.STRING, permKeyStr);
            meta.getPersistentDataContainer().set(ROLE_KEY, PersistentDataType.STRING, role.name());
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createSimpleItem(@NotNull ConfigurationSection sec, @NotNull Map<String, String> placeholders) {
        Material material = parseMaterial(sec.getString("material", "STONE"), Material.STONE);
        String name = sec.getString("display-name", sec.getString("name", "<white>Item"));
        int cmd = sec.getInt("custom-model-data", -1);

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String processedName = name;
            for (var e : placeholders.entrySet()) {
                processedName = processedName.replace(e.getKey(), e.getValue());
            }
            meta.displayName(MM.deserialize(processedName));

            List<Component> lore = new ArrayList<>();
            String desc = sec.getString("description", "");
            if (!desc.isEmpty()) {
                lore.add(MM.deserialize("<gray>" + desc));
            }
            for (String line : sec.getStringList("lore")) {
                String processed = line;
                for (var e : placeholders.entrySet()) {
                    processed = processed.replace(e.getKey(), e.getValue());
                }
                lore.add(MM.deserialize(processed));
            }
            if (!lore.isEmpty()) meta.lore(lore);
            if (cmd >= 0) meta.setCustomModelData(cmd);
            meta.addItemFlags(ItemFlag.values());
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createNavButton(@NotNull String action, @NotNull String configPath) {
        ConfigurationSection nav = config.getConfigurationSection(configPath);
        Material material = nav != null ? parseMaterial(nav.getString("material", "BARRIER"), Material.BARRIER) : Material.BARRIER;
        String name = nav != null ? nav.getString("name", "<red>Button") : "<red>Button";
        int slot = nav != null ? nav.getInt("slot", -1) : -1;
        int cmd = nav != null ? nav.getInt("custom-model-data", -1) : -1;

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MM.deserialize(name));
            List<Component> lore = new ArrayList<>();
            if (nav != null) {
                for (String line : nav.getStringList("lore")) {
                    lore.add(MM.deserialize(line));
                }
            }
            if (!lore.isEmpty()) meta.lore(lore);
            if (cmd >= 0) meta.setCustomModelData(cmd);
            meta.addItemFlags(ItemFlag.values());
            meta.getPersistentDataContainer().set(ACTION_KEY, PersistentDataType.STRING, action);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createRoleIndicator(@NotNull RoleType role) {
        ConfigurationSection ri = config.getConfigurationSection("templates.role-indicator");
        Material material = ri != null ? parseMaterial(ri.getString("material", "PLAYER_HEAD"), Material.PLAYER_HEAD) : Material.PLAYER_HEAD;
        String name = ri != null ? ri.getString("name", "<gold>Role: %role%") : "<gold>Role: %role%";
        int cmd = ri != null ? ri.getInt("custom-model-data", -1) : -1;

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MM.deserialize(name.replace("%role%", role.name())));
            List<Component> lore = new ArrayList<>();
            if (ri != null) {
                for (String line : ri.getStringList("lore")) {
                    lore.add(MM.deserialize(line.replace("%role%", role.name())));
                }
            }
            if (!lore.isEmpty()) meta.lore(lore);
            if (cmd >= 0) meta.setCustomModelData(cmd);
            meta.addItemFlags(ItemFlag.values());
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createBackgroundItem() {
        ConfigurationSection bg = config.getConfigurationSection("background");
        Material material = bg != null ? parseMaterial(bg.getString("material", "GRAY_STAINED_GLASS_PANE"), Material.GRAY_STAINED_GLASS_PANE) : Material.GRAY_STAINED_GLASS_PANE;
        String name = bg != null ? bg.getString("name", " ") : " ";

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MM.deserialize(name));
            meta.addItemFlags(ItemFlag.values());
            item.setItemMeta(meta);
        }
        return item;
    }

    private Material parseMaterial(@Nullable String name, @Nullable Material fallback) {
        if (name == null || name.isEmpty()) return fallback != null ? fallback : Material.STONE;
        try {
            return Material.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return fallback != null ? fallback : Material.STONE;
        }
    }

    private String stripColor(@NotNull String input) {
        return input.replaceAll("<[^>]+>", "").replaceAll("&[0-9a-fk-orA-FK-OR]", "");
    }
}
