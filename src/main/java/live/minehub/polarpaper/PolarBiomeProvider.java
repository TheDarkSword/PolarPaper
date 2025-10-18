package live.minehub.polarpaper;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import live.minehub.polarpaper.util.CoordConversion;
import net.kyori.adventure.key.Key;
import org.bukkit.Registry;
import org.bukkit.block.Biome;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class PolarBiomeProvider extends BiomeProvider {

    private final @NotNull PolarWorld polarWorld;
    public PolarBiomeProvider(@NotNull PolarWorld polarWorld) {
        this.polarWorld = polarWorld;
    }

    @Override
    public @NotNull Biome getBiome(@NotNull WorldInfo worldInfo, int x, int y, int z) {
        int chunkX = CoordConversion.globalToChunk(x);
        int chunkZ = CoordConversion.globalToChunk(z);

        Biome defaultBiome = switch (worldInfo.getEnvironment()) {
            case NORMAL, CUSTOM -> Biome.PLAINS;
            case NETHER -> Biome.NETHER_WASTES;
            case THE_END -> Biome.THE_END;
        };

        PolarChunk chunk = polarWorld.chunkAt(chunkX, chunkZ);
        if (chunk == null) return defaultBiome;

        int sectionIndex = CoordConversion.sectionIndex(y, polarWorld.minSection());

        if (sectionIndex < 0) return defaultBiome;
        if (sectionIndex >= chunk.sections().length) return defaultBiome;

        PolarSection section = chunk.sections()[sectionIndex];

        // Biomes
        String[] rawBiomePalette = section.biomePalette();

        if (rawBiomePalette.length == 1) {
            return parseBiome(rawBiomePalette[0]);
        }

        int localX = CoordConversion.globalToSectionRelative(x);
        int localY = CoordConversion.globalToSectionRelative(y);
        int localZ = CoordConversion.globalToSectionRelative(z);

        int index = localX / 4 + (localZ / 4) * 4 + (localY / 4) * 16;
        int[] biomeDataArray = section.biomeData();

        int biomeData = biomeDataArray[index];
        return parseBiome(rawBiomePalette[biomeData]);
    }

    @Override
    public @NotNull List<Biome> getBiomes(@NotNull WorldInfo worldInfo) {
        Registry<Biome> registry = RegistryAccess.registryAccess().getRegistry(RegistryKey.BIOME);
        return registry.stream().toList();
    }

    private Biome parseBiome(String s) {
        Registry<Biome> registry = RegistryAccess.registryAccess().getRegistry(RegistryKey.BIOME);
        try {
            return registry.get(Key.key(s));
        } catch (IllegalArgumentException e) {
            PolarPaper.logger().warning("Failed to parse biome " + s);
            return Biome.PLAINS;
        }
    }
}
