package live.minehub.polarpaper.schematic;

import ca.spottedleaf.moonrise.patches.chunk_system.scheduling.ChunkHolderManager;
import ca.spottedleaf.moonrise.patches.chunk_system.scheduling.NewChunkHolder;
import live.minehub.polarpaper.PolarChunk;
import live.minehub.polarpaper.PolarPaper;
import live.minehub.polarpaper.PolarSection;
import live.minehub.polarpaper.PolarWorld;
import live.minehub.polarpaper.userdata.WorldUserData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.block.data.CraftBlockData;
import org.joml.Vector3i;

import java.util.HashSet;
import java.util.Set;

public class Schematic {

    public static void paste(PolarWorld polarWorld, World world, BlockModifier blockModifier, IgnoreAir ignoreAir) {
        CraftWorld craftWorld = (CraftWorld) world;
        ServerLevel serverLevel = craftWorld.getHandle();
        ChunkHolderManager chunkHolderManager = serverLevel.moonrise$getChunkTaskScheduler().chunkHolderManager;

        byte[] userData = polarWorld.userData();
        Vector3i offset = WorldUserData.readSchematicOffset(userData);
        if (offset == null) offset = new Vector3i();

        int minSection = craftWorld.getMinHeight() / 16;
        for (PolarChunk chunk : polarWorld.chunks()) {
            int i = 0;
            for (PolarSection section : chunk.sections()) {
                Vector3i blockOffset = new Vector3i(chunk.x() * 16, (i + minSection) * 16, chunk.z() * 16)
                        .sub(offset);
                pasteSection(section, chunkHolderManager, blockModifier, blockOffset, ignoreAir);
                i++;
            }
        }

        Set<ChunkPos> chunksToRefresh = new HashSet<>();
        for (PolarChunk chunk : polarWorld.chunks()) {
            Vector3i chunkOffset = new Vector3i(chunk.x() * 16, 0, chunk.z() * 16)
                    .sub(offset);
            blockModifier.modify(chunkOffset);

            int cX = (int)Math.floor(chunkOffset.x / 16.0);
            int cZ = (int)Math.floor(chunkOffset.z / 16.0);

            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    chunksToRefresh.add(new ChunkPos(cX + x, cZ + z));
                }
            }
        }

        // refresh blocks and light
        for (ChunkPos c : chunksToRefresh) {
            world.refreshChunk(c.x, c.z);
        }
        serverLevel.getChunkSource().getLightEngine().starlight$serverRelightChunks(chunksToRefresh, a -> {}, a -> {});
    }

    private static void pasteSection(PolarSection polarSection, ChunkHolderManager chunkHolderManager, BlockModifier blockModifier, Vector3i offset, IgnoreAir ignoreAir) {
        // Blocks
        int[] blockData = polarSection.blockData();

        String[] rawBlockPalette = polarSection.blockPalette();
        BlockState[] materialPalette = new BlockState[rawBlockPalette.length];
        for (int i = 0; i < rawBlockPalette.length; i++) {
            try {
                materialPalette[i] = ((CraftBlockData) Bukkit.getServer().createBlockData(rawBlockPalette[i])).getState();
            } catch (IllegalArgumentException e) {
                PolarPaper.logger().warning("Failed to parse block state: " + rawBlockPalette[i]);
                materialPalette[i] = Blocks.AIR.defaultBlockState();
            }
        }

        if (rawBlockPalette.length <= 1) {
            BlockState blockState = materialPalette[0];
            if (blockState.isAir() && (ignoreAir == IgnoreAir.ALL || ignoreAir == IgnoreAir.EMPTY_SECTION)) return;

            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        Vector3i blockPos = new Vector3i(x, y, z);
                        blockPos.add(offset);
                        blockModifier.modify(blockPos, blockState);

                        setBlockFast(chunkHolderManager, blockPos.x, blockPos.y, blockPos.z, blockState);
                    }
                }
            }
        } else {
            int blockIndex = 0;
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        BlockState blockState = materialPalette[blockData[blockIndex++]];
                        if (ignoreAir == IgnoreAir.ALL && blockState.isAir()) continue;

                        Vector3i blockPos = new Vector3i(x, y, z);
                        blockPos.add(offset);
                        blockModifier.modify(blockPos, blockState);

                        setBlockFast(chunkHolderManager, blockPos.x, blockPos.y, blockPos.z, blockState);
                    }
                }
            }
        }
    }

    private static void setBlockFast(ChunkHolderManager chunkHolderManager, int x, int y, int z, BlockState blockState) {
        int chunkX = (int)Math.floor(x / 16.0);
        int chunkZ = (int)Math.floor(z / 16.0);
        int section = (int)Math.floor(y / 16.0);

        NewChunkHolder chunkHolder = chunkHolderManager.getChunkHolder(chunkX, chunkZ);
        if (chunkHolder == null) return;
        ChunkAccess chunkAccess = chunkHolder.getCurrentChunk();
        if (chunkAccess == null) return;

        int sectionI = section - chunkAccess.getMinSectionY();
        if (sectionI >= chunkAccess.getSections().length) return;
        if (sectionI < 0) return;

        LevelChunkSection levelChunkSection = chunkAccess.getSection(sectionI);
        int newBlockX = x % 16;
        if (newBlockX < 0) newBlockX = 16 + newBlockX;
        int newBlockY = y % 16;
        if (newBlockY < 0) newBlockY = 16 + newBlockY;
        int newBlockZ = z % 16;
        if (newBlockZ < 0) newBlockZ = 16 + newBlockZ;

        levelChunkSection.setBlockState(newBlockX, newBlockY, newBlockZ, blockState);
    }

    public enum IgnoreAir {
        /**
         * Ignore all air blocks
         */
        ALL,
        /**
         * Only ignore empty sections (default)
         */
        EMPTY_SECTION,
        /**
         * Do not ignore air
         */
        NONE
    }

}
