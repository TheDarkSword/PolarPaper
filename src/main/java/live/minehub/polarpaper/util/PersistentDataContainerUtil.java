package live.minehub.polarpaper.util;

import com.google.common.io.ByteArrayDataOutput;
import live.minehub.polarpaper.PolarPaper;
import org.bukkit.ChunkSnapshot;
import org.bukkit.persistence.PersistentDataContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.logging.Level;

import static live.minehub.polarpaper.util.ByteArrayUtil.getByteArray;
import static live.minehub.polarpaper.util.ByteArrayUtil.writeByteArray;

public class PersistentDataContainerUtil {

    private PersistentDataContainerUtil() {

    }

    /**
     * Gets the persistent data container from the given user data.
     * This method MUST be called after {@link EntityUtil#getEntities(ByteBuffer)}
     * @param bb The user data to get the persistent data container from
     */
    public static byte[] getPersistentDataContainer(@Nullable ByteBuffer bb) {
        if (bb == null) return new byte[0];

        return getByteArray(bb);
    }

    /**
     * Writes the persistent data container to the given user data.
     * This method MUST be called after {@link EntityUtil#writeEntities(java.util.List, ByteArrayDataOutput)}
     * @param chunk The chunk to get the persistent data container from
     * @param persistentDataContainer The persistent data container to write to the user data
     * @param userData The user data to write the persistent data container to
     */
    public static void writePersistentDataContainer(@NotNull ChunkSnapshot chunk, @NotNull PersistentDataContainer persistentDataContainer, @NotNull ByteArrayDataOutput userData)  {
        byte[] persistentDataContainerBytes;
        try {
            persistentDataContainerBytes = persistentDataContainer.serializeToBytes();
        } catch (IOException e) {
            PolarPaper.logger().log(Level.SEVERE, "Failed to serialize persistent data container for chunk at " +
                    chunk.getX() + ", " + chunk.getZ() + " in world " + chunk.getWorldName(), e);
            persistentDataContainerBytes = new byte[0];
        }
        writeByteArray(persistentDataContainerBytes, userData);
    }
}
