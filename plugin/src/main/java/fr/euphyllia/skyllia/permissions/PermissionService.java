package fr.euphyllia.skyllia.permissions;

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
import org.bukkit.NamespacedKey;

import java.util.UUID;

/**
 * Single source of truth for permission operations.
 * Both /is permission command and Permission GUI MUST use this service.
 * No duplicate logic allowed anywhere else.
 */
public final class PermissionService {

    private static PermissionService instance;

    private final Skyllia plugin;

    private PermissionService(Skyllia plugin) {
        this.plugin = plugin;
    }

    public static synchronized void init(Skyllia plugin) {
        if (instance == null) {
            instance = new PermissionService(plugin);
        }
    }

    public static PermissionService get() {
        if (instance == null) {
            throw new IllegalStateException("PermissionService not initialized. Call PermissionService.init() first.");
        }
        return instance;
    }

    public PermissionRegistry getRegistry() {
        return SkylliaAPI.getPermissionRegistry();
    }

    public IslandPermissionQuery getQuery() {
        return plugin.getInterneAPI()
                .getIslandQuery()
                .getIslandPermissionQuery();
    }

    public boolean hasPermission(Island island, RoleType role, PermissionId pid) {
        CompiledPermissions compiled = island.getCompiledPermissions();
        compiled.ensureUpToDate(getRegistry());
        return compiled.has(getRegistry(), role, pid);
    }

    public PermissionId getPermissionId(NamespacedKey key) {
        return getRegistry().getIfPresent(key);
    }

    public PermissionId registerPermission(PermissionNode node) {
        return getRegistry().register(node);
    }

    /**
     * Set a permission value for a role on an island.
     * This is the ONLY method that writes permission changes.
     * Used by both /is permission command and Permission GUI.
     *
     * Flow:
     *   IslandPermissionQuery.set()  → writes to DB (load → flip bit → save blob)
     *   CompiledPermissions.setFor().set() → updates runtime cache
     */
    public boolean setPermission(Island island, RoleType role, PermissionId pid, boolean value) {
        IslandPermissionQuery query = getQuery();
        if (query == null) return false;

        boolean success = query.set(island.getId(), role, pid, value);
        if (!success) return false;

        PermissionRegistry registry = getRegistry();
        CompiledPermissions compiled = island.getCompiledPermissions();
        compiled.ensureUpToDate(registry);

        PermissionSet set = compiled.setFor(role);
        if (set == null) return false;

        set.set(pid, value);
        return true;
    }

    /**
     * Toggle a permission value for a role on an island.
     * Reads current value via CompiledPermissions, then calls setPermission with the opposite.
     */
    public boolean togglePermission(Island island, RoleType role, PermissionId pid) {
        boolean current = hasPermission(island, role, pid);
        return setPermission(island, role, pid, !current);
    }

    public Island getIsland(UUID playerId) {
        return SkylliaAPI.getIslandByPlayerId(playerId);
    }
}
