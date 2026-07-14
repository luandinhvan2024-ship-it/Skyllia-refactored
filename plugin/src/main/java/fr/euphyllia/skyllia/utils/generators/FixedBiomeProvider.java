package fr.euphyllia.skyllia.utils.generators;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class FixedBiomeProvider extends BiomeProvider {

    private final Biome biome;
    private final List<Biome> biomes;

    public FixedBiomeProvider(@NotNull Biome biome) {
        this.biome = biome;
        this.biomes = List.of(biome);
    }

    public static @NotNull FixedBiomeProvider fromConfig(
            @NotNull World.Environment env,
            String biomeIdOrName
    ) {
        Biome fallback = switch (env) {
            case NETHER -> Biome.NETHER_WASTES;
            case THE_END -> Biome.THE_END;
            default -> Biome.PLAINS;
        };

        if (biomeIdOrName == null || biomeIdOrName.isBlank()) {
            return new FixedBiomeProvider(fallback);
        }

        Biome parsed = Registry.BIOME.get(NamespacedKey.fromString(biomeIdOrName));
        if (parsed == null) {
            try {
                parsed = Biome.valueOf(biomeIdOrName.toUpperCase(java.util.Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                parsed = null;
            }
        }
        return new FixedBiomeProvider(parsed != null ? parsed : fallback);
    }

    @Override
    public @NotNull Biome getBiome(@NotNull WorldInfo worldInfo, int x, int y, int z) {
        return biome;
    }

    @Override
    public @NotNull List<Biome> getBiomes(@NotNull WorldInfo worldInfo) {
        return biomes;
    }
}
