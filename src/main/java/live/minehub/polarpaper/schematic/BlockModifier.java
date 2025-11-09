package live.minehub.polarpaper.schematic;

import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3i;

public interface BlockModifier {
    void modify(Vector3i pos, BlockState blockState);
    void modify(Vector3i pos);

    record PosRot(Vector3i offset, Rotation rotation) implements BlockModifier {
        @Override
        public void modify(Vector3i pos, BlockState blockState) {
            if (rotation != Rotation.NONE) {
                blockState.rotate(rotation);
                rotatePos(pos, rotation);
            }
            pos.add(offset);
        }

        @Override
        public void modify(Vector3i pos) {
            if (rotation != Rotation.NONE) {
                rotatePos(pos, rotation);
            }
            pos.add(offset);
        }

        public static void rotatePos(@NotNull Vector3i point, @NotNull Rotation rotation) {
            switch (rotation) {
                case CLOCKWISE_90 -> {
                    int x = -point.z;
                    int z = point.x;
                    point.x = x;
                    point.z = z;
                }
                case CLOCKWISE_180 -> {
                    int x = -point.x;
                    int z = -point.z;
                    point.x = x;
                    point.z = z;
                }
                case COUNTERCLOCKWISE_90 -> {
                    int x = point.z;
                    int z = -point.x;
                    point.x = x;
                    point.z = z;
                }
            }
        }
    }
}