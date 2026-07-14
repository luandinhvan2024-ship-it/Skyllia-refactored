package fr.euphyllia.skyllia.gui;

import fr.euphyllia.skyllia.Skyllia;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PermissionGuiConfig {

    private final Skyllia plugin;
    private FileConfiguration config;

    private final Map<String, ItemConfig> categoryItems = new HashMap<>();
    private int categoryRows;
    private String categoryTitle;
    private ItemConfig categoryBackground;
    private NavConfig categoryClose;

    private final Map<String, RoleItemConfig> roleItems = new HashMap<>();
    private int roleRows;
    private String roleTitle;
    private ItemConfig roleBackground;
    private NavConfig roleBack;
    private NavConfig roleClose;

    private int permListRows;
    private String permListTitle;
    private int permListPageSize;
    private ItemConfig permListBackground;
    private NavConfig permPrevPage;
    private NavConfig permNextPage;
    private NavConfig permBack;
    private NavConfig permClose;
    private final Map<String, PermissionItemConfig> permissionItems = new HashMap<>();

    public PermissionGuiConfig(Skyllia plugin) {
        this.plugin = plugin;
    }

    public void load() {
        File file = new File(plugin.getDataFolder(), "config/permissions-gui.yml");
        if (!file.exists()) {
            plugin.saveResource("config/permissions-gui.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(file);

        InputStream defaults = plugin.getResource("config/permissions-gui.yml");
        if (defaults != null) {
            YamlConfiguration def = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaults, StandardCharsets.UTF_8));
            config.setDefaults(def);
        }

        loadCategories();
        loadRoles();
        loadPermissionList();
    }

    private void loadCategories() {
        ConfigurationSection cs = config.getConfigurationSection("categories");
        if (cs == null) return;
        categoryRows = cs.getInt("rows", 3);
        categoryTitle = cs.getString("title", "<white>Categories");

        categoryBackground = loadItem(cs.getConfigurationSection("background"));
        categoryClose = loadNav(cs.getConfigurationSection("close"));

        ConfigurationSection items = cs.getConfigurationSection("items");
        if (items != null) {
            for (String key : items.getKeys(false)) {
                ConfigurationSection is = items.getConfigurationSection(key);
                if (is == null) continue;
                categoryItems.put(key.toLowerCase(), loadItem(is));
            }
        }
    }

    private void loadRoles() {
        ConfigurationSection rs = config.getConfigurationSection("roles");
        if (rs == null) return;
        roleRows = rs.getInt("rows", 3);
        roleTitle = rs.getString("title", "<white>Select Role");

        roleBackground = loadItem(rs.getConfigurationSection("background"));
        roleBack = loadNav(rs.getConfigurationSection("back"));
        roleClose = loadNav(rs.getConfigurationSection("close"));

        ConfigurationSection items = rs.getConfigurationSection("items");
        if (items != null) {
            for (String key : items.getKeys(false)) {
                ConfigurationSection is = items.getConfigurationSection(key);
                if (is == null) continue;
                RoleItemConfig rc = new RoleItemConfig();
                rc.slot = is.getInt("slot", 0);
                loadItemInto(rc, is);
                roleItems.put(key.toUpperCase(), rc);
            }
        }
    }

    private void loadPermissionList() {
        ConfigurationSection pl = config.getConfigurationSection("permission-list");
        if (pl == null) return;
        permListRows = pl.getInt("rows", 6);
        permListTitle = pl.getString("title", "<white>Permissions");
        permListPageSize = pl.getInt("page-size", 36);

        permListBackground = loadItem(pl.getConfigurationSection("background"));
        permPrevPage = loadNav(pl.getConfigurationSection("previous-page"));
        permNextPage = loadNav(pl.getConfigurationSection("next-page"));
        permBack = loadNav(pl.getConfigurationSection("back"));
        permClose = loadNav(pl.getConfigurationSection("close"));

        ConfigurationSection perms = pl.getConfigurationSection("permissions");
        if (perms != null) {
            for (String key : perms.getKeys(false)) {
                ConfigurationSection ps = perms.getConfigurationSection(key);
                if (ps == null) continue;
                PermissionItemConfig pic = new PermissionItemConfig();
                pic.category = ps.getString("category", "physical").toLowerCase();
                pic.enabled = loadItem(ps.getConfigurationSection("enabled"));
                pic.disabled = loadItem(ps.getConfigurationSection("disabled"));
                permissionItems.put(key.toLowerCase(), pic);
            }
        }
    }

    private ItemConfig loadItem(ConfigurationSection section) {
        if (section == null) return new ItemConfig();
        ItemConfig ic = new ItemConfig();
        loadItemInto(ic, section);
        return ic;
    }

    private NavConfig loadNav(ConfigurationSection section) {
        if (section == null) return new NavConfig();
        NavConfig nc = new NavConfig();
        nc.slot = section.getInt("slot", 0);
        loadItemInto(nc, section);
        return nc;
    }

    private void loadItemInto(ItemConfig ic, ConfigurationSection section) {
        ic.material = parseMaterial(section.getString("material", "STONE"), Material.STONE);
        ic.displayName = section.getString("display-name", "<white>Item");
        ic.lore = section.getStringList("lore");
    }

    private Material parseMaterial(String name, Material fallback) {
        if (name == null || name.isEmpty()) return fallback;
        try {
            return Material.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    public Map<String, ItemConfig> getCategoryItems() { return categoryItems; }
    public int getCategoryRows() { return categoryRows; }
    public String getCategoryTitle() { return categoryTitle; }
    public ItemConfig getCategoryBackground() { return categoryBackground; }
    public NavConfig getCategoryClose() { return categoryClose; }

    public Map<String, RoleItemConfig> getRoleItems() { return roleItems; }
    public int getRoleRows() { return roleRows; }
    public String getRoleTitle() { return roleTitle; }
    public ItemConfig getRoleBackground() { return roleBackground; }
    public NavConfig getRoleBack() { return roleBack; }
    public NavConfig getRoleClose() { return roleClose; }

    public int getPermListRows() { return permListRows; }
    public String getPermListTitle() { return permListTitle; }
    public int getPermListPageSize() { return permListPageSize; }
    public ItemConfig getPermListBackground() { return permListBackground; }
    public NavConfig getPermPrevPage() { return permPrevPage; }
    public NavConfig getPermNextPage() { return permNextPage; }
    public NavConfig getPermBack() { return permBack; }
    public NavConfig getPermClose() { return permClose; }
    public Map<String, PermissionItemConfig> getPermissionItems() { return permissionItems; }

    public PermissionItemConfig getPermissionItem(String key) {
        return permissionItems.get(key.toLowerCase());
    }

    public List<String> getPermissionsByCategory(String category) {
        List<String> result = new ArrayList<>();
        for (Map.Entry<String, PermissionItemConfig> entry : permissionItems.entrySet()) {
            if (entry.getValue().category.equals(category)) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    public static class ItemConfig {
        public Material material = Material.STONE;
        public String displayName = "<white>Item";
        public List<String> lore = new ArrayList<>();
    }

    public static class NavConfig extends ItemConfig {
        public int slot = 0;
    }

    public static class RoleItemConfig extends NavConfig {
    }

    public static class PermissionItemConfig {
        public String category = "physical";
        public ItemConfig enabled = new ItemConfig();
        public ItemConfig disabled = new ItemConfig();
    }
}
