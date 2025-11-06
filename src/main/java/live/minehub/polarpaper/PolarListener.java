package live.minehub.polarpaper;

import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.bukkit.craftbukkit.CraftChunk;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.world.ChunkPopulateEvent;
import org.bukkit.generator.ChunkGenerator;

@SuppressWarnings("unused")
public class PolarListener implements Listener {

    @EventHandler
    public void onChunkPopulate(ChunkPopulateEvent event) {
        PolarWorld polarWorld = PolarWorld.fromWorld(event.getWorld());
        if (polarWorld == null) return;

        PolarChunk chunk = polarWorld.chunkAt(event.getChunk().getX(), event.getChunk().getZ());
        if (chunk == null) return;

        ChunkGenerator generator = event.getWorld().getGenerator();
        if (!(generator instanceof PolarGenerator polarGenerator)) return;

        // TODO: chunks still seem to be getting marked unsaved when they shouldn't, worst case is the chunk gets resaved when it didn't need to
        CraftChunk craftChunk = ((CraftChunk) event.getChunk());
        boolean wasUnsaved = craftChunk.getHandle(ChunkStatus.FULL).tryMarkSaved();
//        System.out.println("Marked saved: " + wasUnsaved + " (" + chunk.x() + ", " + chunk.z() + ")");

        PolarWorldAccess worldAccess = polarGenerator.getWorldAccess();
        if (chunk.userData().length > 0) {
            worldAccess.populateChunkData(event.getChunk(), chunk.userData());
        }
    }

    @EventHandler
    public void onBlockFade(BlockFadeEvent event) { // coral death
        PolarGenerator generator = PolarGenerator.fromWorld(event.getBlock().getWorld());
        if (generator == null) return;
        Config config = generator.getConfig();
        Object enabled = config.gamerules().getOrDefault("coralDeath", true);
        if (!(enabled instanceof Boolean enabledBool)) return;
        event.setCancelled(!enabledBool);
    }

    @EventHandler
    public void onBlockFromTo(BlockFromToEvent event) { // liquid flow / dragon egg
        PolarGenerator generator = PolarGenerator.fromWorld(event.getBlock().getWorld());
        if (generator == null) return;
        Config config = generator.getConfig();
        Object enabled = config.gamerules().getOrDefault("liquidPhysics", true);
        if (!(enabled instanceof Boolean enabledBool)) return;
        event.setCancelled(!enabledBool);
    }

    @EventHandler
    public void onBlockPhysics(BlockPhysicsEvent event) { // block placement rules
        PolarGenerator generator = PolarGenerator.fromWorld(event.getBlock().getWorld());
        if (generator == null) return;
        Config config = generator.getConfig();
        Object enabled = config.gamerules().getOrDefault("blockPhysics", true);
        if (!(enabled instanceof Boolean enabledBool)) return;
        event.setCancelled(!enabledBool);
    }

    @EventHandler
    public void onChangeBlock(EntityChangeBlockEvent event) { // gravity blocks
        if (!event.getBlock().getType().hasGravity()) return;

        PolarGenerator generator = PolarGenerator.fromWorld(event.getBlock().getWorld());
        if (generator == null) return;
        Config config = generator.getConfig();
        Object enabled = config.gamerules().getOrDefault("blockGravity", true);
        if (!(enabled instanceof Boolean enabledBool)) return;
        event.setCancelled(!enabledBool);
    }



}
