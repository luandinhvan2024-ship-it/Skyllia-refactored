package fr.euphyllia.skyllia.managers.world;

import fr.euphyllia.skyllia.api.SkylliaAPI;
import fr.euphyllia.skyllia.api.coordinate.ChunkCoordinate;
import fr.euphyllia.skyllia.api.coordinate.RegionCoordinate;
import fr.euphyllia.skyllia.api.skyblock.Island;
import fr.euphyllia.skyllia.api.utils.RegionUtils;
import fr.euphyllia.skyllia.configuration.ConfigLoader;
import fr.euphyllia.skyllia.configuration.manager.GeneralConfigManager;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

@ApiStatus.Internal
public class WorldModifier {

    private static final Logger log = LoggerFactory.getLogger(WorldModifier.class);

    private final JavaPlugin plugin;
    private final ScheduledExecutorService deleteScheduler;

    public WorldModifier(JavaPlugin plugin) {
        this.plugin = plugin;
        GeneralConfigManager.ChunkProcessingSettings cfg = ConfigLoader.general.getIslandSettings().chunkProcessing();

        this.deleteScheduler = Executors.newScheduledThreadPool(
                cfg.resolvedDeleteThreads(),
                r -> {
                    Thread t = new Thread(r, "skyllia-delete-processor");
                    t.setDaemon(true);
                    t.setPriority(Thread.MIN_PRIORITY);
                    return t;
                }
        );

        log.info("WorldModifier initialized — delete: {} thread(s) / {}ms delay",
                cfg.resolvedDeleteThreads(), cfg.deleteDelayMs());
    }

    public void shutdown() {
        deleteScheduler.shutdown();
    }

    public void deleteIsland(@NotNull Island island, @NotNull World world, int regionDistance, Consumer<Boolean> onFinish) {
        RegionCoordinate position = island.getRegionCoordinate();
        List<ChunkCoordinate> chunks = RegionUtils.computeChunksToDelete(position, regionDistance, island.getSize());
        if (chunks.isEmpty()) {
            if (onFinish != null) onFinish.accept(true);
            return;
        }
        AtomicInteger toDelete = new AtomicInteger(chunks.size());
        AtomicBoolean failed = new AtomicBoolean(false);
        for (int i = 0; i < chunks.size(); i++) {
            final ChunkCoordinate chunkPos = chunks.get(i);
            final long delay = (long) i * ConfigLoader.general.getIslandSettings().chunkProcessing().deleteDelayMs();
            deleteScheduler.schedule(() -> {
                world.getChunkAtAsync(chunkPos.x(), chunkPos.z()).thenAccept(ignored -> {
                    try {
                        SkylliaAPI.getWorldNMS().resetChunk(world, chunkPos);
                    } catch (Exception e) {
                        failed.set(true);
                    }
                    if (toDelete.decrementAndGet() == 0 && onFinish != null) {
                        onFinish.accept(!failed.get());
                    }
                });
            }, delay, TimeUnit.MILLISECONDS);
        }
    }
}
