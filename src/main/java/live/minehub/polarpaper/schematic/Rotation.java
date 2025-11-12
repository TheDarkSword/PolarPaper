package live.minehub.polarpaper.schematic;

import org.jetbrains.annotations.Nullable;

public enum Rotation {
    NONE("none", net.minecraft.world.level.block.Rotation.NONE),
    CLOCKWISE_90("90", net.minecraft.world.level.block.Rotation.CLOCKWISE_90),
    CLOCKWISE_180("180", net.minecraft.world.level.block.Rotation.CLOCKWISE_180),
    CLOCKWISE_270("270", net.minecraft.world.level.block.Rotation.COUNTERCLOCKWISE_90);

    private static final Rotation[] ROTATIONS = values();
    private final String friendlyName;
    private final net.minecraft.world.level.block.Rotation mcRot;
    Rotation(String friendlyName, net.minecraft.world.level.block.Rotation mcRot) {
        this.friendlyName = friendlyName;
        this.mcRot = mcRot;
    }

    public int toDegrees() {
        return ordinal() * 90;
    }

    public String getFriendlyName() {
        return friendlyName;
    }

    public net.minecraft.world.level.block.Rotation getMcRot() {
        return mcRot;
    }

    public Rotation rotate(Rotation rotation) {
        return values()[(ordinal() + rotation.ordinal()) % 4];
    }

    public static @Nullable Rotation fromFriendlyName(String friendlyName) {
        for (Rotation rotation : ROTATIONS) {
            if (rotation.friendlyName.equals(friendlyName)) return rotation;
        }
        return null;
    }
}
