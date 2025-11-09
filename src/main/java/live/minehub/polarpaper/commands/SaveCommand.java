package live.minehub.polarpaper.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import live.minehub.polarpaper.*;
import live.minehub.polarpaper.source.FilePolarSource;
import live.minehub.polarpaper.userdata.WorldUserData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.joml.Vector3i;

public class SaveCommand {

    public static final NamespacedKey POS_1_KEY = new NamespacedKey("polarpaper", "pos1");
    public static final NamespacedKey POS_2_KEY = new NamespacedKey("polarpaper", "pos2");

    protected static int run(CommandContext<CommandSourceStack> ctx) {
        String worldName = ctx.getArgument("worldname", String.class);

        World bukkitWorld = Bukkit.getWorld(worldName);
        if (bukkitWorld == null) {
            ctx.getSource().getSender().sendMessage(
                    Component.text()
                            .append(Component.text("World '", NamedTextColor.RED))
                            .append(Component.text(worldName, NamedTextColor.RED))
                            .append(Component.text("' does not exist!", NamedTextColor.RED))
            );
            return Command.SINGLE_SUCCESS;
        }

        PolarWorld polarWorld = PolarWorld.fromWorld(bukkitWorld);
        if (polarWorld == null) {
            ctx.getSource().getSender().sendMessage(
                    Component.text()
                            .append(Component.text("World '", NamedTextColor.RED))
                            .append(Component.text(worldName, NamedTextColor.RED))
                            .append(Component.text("' is not a polar world!", NamedTextColor.RED))
            );
            return Command.SINGLE_SUCCESS;
        }
        PolarGenerator polarGenerator = PolarGenerator.fromWorld(bukkitWorld);
        if (polarGenerator == null) return Command.SINGLE_SUCCESS;

        ctx.getSource().getSender().sendMessage(
                Component.text()
                        .append(Component.text("Saving '", NamedTextColor.GRAY))
                        .append(Component.text(worldName, NamedTextColor.GRAY))
                        .append(Component.text("'...", NamedTextColor.GRAY))
        );

        Vector3i schemOffset = null;
        BlockSelector blockSelector = BlockSelector.ALL;
        if (ctx.getSource().getSender() instanceof Player player) {
            PersistentDataContainer data = player.getPersistentDataContainer();
            int[] pos1Array = data.get(POS_1_KEY, PersistentDataType.INTEGER_ARRAY);
            int[] pos2Array = data.get(POS_2_KEY, PersistentDataType.INTEGER_ARRAY);
            if (pos1Array != null && pos2Array != null) {
                Vector3i pos1 = new Vector3i(pos1Array);
                Vector3i pos2 = new Vector3i(pos2Array);
                blockSelector = BlockSelector.RegionBlockSelector.fromCorners(pos1, pos2);

                schemOffset = player.getLocation().toVector().toVector3i();

                player.sendMessage(Component.text("Saving only the blocks inside the selected region", NamedTextColor.YELLOW));
            }
        }

        long before = System.nanoTime();

        Polar.updateConfig(bukkitWorld, bukkitWorld.getName()); // config should only be updated synchronously

        BlockSelector finalBlockSelector = blockSelector;
        Vector3i finalSchemOffset = schemOffset;
        Bukkit.getAsyncScheduler().runNow(PolarPaper.getPlugin(), (task) -> {
            Polar.updateWorld(bukkitWorld, polarWorld, polarGenerator.getWorldAccess(), finalBlockSelector);
            if (finalSchemOffset != null) polarWorld.userData(WorldUserData.writeSchematicOffset(finalSchemOffset));
            byte[] worldBytes = PolarWriter.write(polarWorld);
            FilePolarSource.defaultFolder(bukkitWorld.getName()).saveBytes(worldBytes);

            int ms = (int) ((System.nanoTime() - before) / 1_000_000);
            ctx.getSource().getSender().sendMessage(
                    Component.text()
                            .append(Component.text("Saved '", NamedTextColor.AQUA))
                            .append(Component.text(worldName, NamedTextColor.AQUA))
                            .append(Component.text("' in ", NamedTextColor.AQUA))
                            .append(Component.text(ms, NamedTextColor.AQUA))
                            .append(Component.text("ms", NamedTextColor.AQUA))
            );
        });

        return Command.SINGLE_SUCCESS;
    }

}
