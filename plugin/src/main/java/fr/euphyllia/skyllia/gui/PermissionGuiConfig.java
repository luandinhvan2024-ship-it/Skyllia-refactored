package fr.euphyllia.skyllia.gui;

import fr.euphyllia.skyllia.Skyllia;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
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

    private int roleSelectRows;
    private String roleSelectTitle;
    private final Map<String, RoleItemConfig> roleItems = new HashMap<>();

    private int permissionListRows;
    private String permissionListTitle;
    private int permissionListPageSize;
    private final ItemConfig permissionItemEnabled = new ItemConfig();
    private final ItemConfig permissionItemDisabled = new ItemConfig();
    private final NavConfig navPrevious = new NavConfig();
    private final NavConfig navNext = new NavConfig();
    private final NavConfig navBack = new NavConfig();
    private final NavConfig roleIndicator = new NavConfig();

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

        loadRoleSelect();
        loadPermissionList();
    }

    private void loadRoleSelect() {
        ConfigurationSection rs = config.getConfigurationSection("role-select");
        if (rs == null) return;

        roleSelectRows = rs.getInt("rows", 3);
        roleSelectTitle = rs.getString("title", "<white>Select Role");

        ConfigurationSection roles = rs.getConfigurationSection("roles");
        if (roles != null) {
            for (String key : roles.getKeys(false)) {
                ConfigurationSection rs2 = roles.getConfigurationSection(key);
                if (rs2 == null) continue;
                RoleItemConfig rc = new RoleItemConfig();
                rc.slot = rs2.getInt("slot", 0);
                rc.material = parseMaterial(rs2.getString("material", "STONE"), Material.STONE);
                rc.customModelData = rs2.getInt("custom-model-data", -1);
                rc.displayName = rs2.getString("display-name", "<white>" + key);
                rc.lore = rs2.getStringList("lore");
                roleItems.put(key.toUpperCase(), rc);
            }
        }
    }

    private void loadPermissionList() {
        ConfigurationSection pl = config.getConfigurationSection("permission-list");
        if (pl == null) return;

        permissionListRows = pl.getInt("rows", 6);
        permissionListTitle = pl.getString("title", "<white>Permissions - %role%");
        permissionListPageSize = pl.getInt("page-size", 36);

        ConfigurationSection pi = pl.getConfigurationSection("permission-item");
        if (pi != null) {
            ConfigurationSection en = pi.getConfigurationSection("enabled");
            if (en != null) loadItemConfig(permissionItemEnabled, en);
            ConfigurationSection dis = pi.getConfigurationSection("disabled");
            if (dis != null) loadItemConfig(permissionItemDisabled, dis);
        }

        ConfigurationSection nav = pl.getConfigurationSection("navigation");
        if (nav != null) {
            ConfigurationSection prev = nav.getConfigurationSection("previous");
            if (prev != null) loadNavConfig(navPrevious, prev);
            ConfigurationSection next = nav.getConfigurationSection("next");
            if (next != null) loadNavConfig(navNext, next);
            ConfigurationSection back = nav.getConfigurationSection("back");
            if (back != null) loadNavConfig(navBack, back);
        }

        ConfigurationSection ri = pl.getConfigurationSection("role-indicator");
        if (ri != null) loadNavConfig(roleIndicator, ri);
    }

    private void loadItemConfig(ItemConfig item, ConfigurationSection section) {
        item.material = parseMaterial(section.getString("material", "STONE"), Material.STONE);
        item.customModelData = section.getInt("custom-model-data", -1);
        item.displayName = section.getString("display-name", "<white>Permission");
        item.lore = section.getStringList("lore");
    }

    private void loadNavConfig(NavConfig nav, ConfigurationSection section) {
        nav.slot = section.getInt("slot", 0);
        loadItemConfig(nav, section);
    }

    private Material parseMaterial(String name, Material fallback) {
        if (name == null || name.isEmpty()) return fallback;
        try {
            return Material.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    public int getRoleSelectRows() { return roleSelectRows; }
    public String getRoleSelectTitle() { return roleSelectTitle; }
    public Map<String, RoleItemConfig> getRoleItems() { return roleItems; }

    public int getPermissionListRows() { return permissionListRows; }
    public String getPermissionListTitle() { return permissionListTitle; }
    public int getPermissionListPageSize() { return permissionListPageSize; }
    public ItemConfig getPermissionItemEnabled() { return permissionItemEnabled; }
    public ItemConfig getPermissionItemDisabled() { return permissionItemDisabled; }
    public NavConfig getNavPrevious() { return navPrevious; }
    public NavConfig getNavNext() { return navNext; }
    public NavConfig getNavBack() { return navBack; }
    public NavConfig getRoleIndicator() { return roleIndicator; }

    public static class ItemConfig {
        public Material material = Material.STONE;
        public int customModelData = -1;
        public String displayName = "<white>Item";
        public List<String> lore = new ArrayList<>();
    }

    public static class NavConfig extends ItemConfig {
        public int slot = 0;
    }

    public static class RoleItemConfig extends NavConfig {
    }
}
