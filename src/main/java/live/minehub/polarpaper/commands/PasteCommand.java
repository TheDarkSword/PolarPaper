package live.minehub.polarpaper.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import live.minehub.polarpaper.PolarPaper;
import live.minehub.polarpaper.PolarReader;
import live.minehub.polarpaper.PolarWorld;
import live.minehub.polarpaper.schematic.BlockModifier;
import live.minehub.polarpaper.schematic.Schematic;
import live.minehub.polarpaper.util.ExceptionUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.world.level.block.Rotation;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class PasteCommand {

    protected static int run(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        if (!(sender instanceof Player)) {
            ctx.getSource().getSender().sendMessage(
                    Component.text()
                            .append(Component.text("Usage: /polar paste <world> [rotation] (while in a world)", NamedTextColor.RED))
            );
            return Command.SINGLE_SUCCESS;
        }

        return paste(ctx, Rotation.NONE);
    }

    protected static int runWithRotation(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        if (!(sender instanceof Player)) {
            ctx.getSource().getSender().sendMessage(
                    Component.text()
                            .append(Component.text("Usage: /polar paste <world> [rotation] (while in a world)", NamedTextColor.RED))
            );
            return Command.SINGLE_SUCCESS;
        }

        String rotationString = ctx.getArgument("rotation", String.class);

        try {
            return paste(ctx, Rotation.valueOf(rotationString.toUpperCase()));
        } catch (IllegalArgumentException ignored) {
            ctx.getSource().getSender().sendMessage(Component.text("Invalid rotation '" + rotationString + "'", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }
    }

    private static int paste(CommandContext<CommandSourceStack> ctx, Rotation rotation) {
        if (!(ctx.getSource().getSender() instanceof Player player)) return Command.SINGLE_SUCCESS;

        String worldName = ctx.getArgument("worldname", String.class);

        Path pluginFolder = Path.of(PolarPaper.getPlugin().getDataFolder().getAbsolutePath());
        Path worldsFolder = pluginFolder.resolve("worlds");
        Path path = worldsFolder.resolve(worldName + ".polar");

        if (!Files.exists(path)) {
            player.sendMessage(Component.text("Couldn't find file '" + worldName + ".polar' in the worlds folder", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        PolarWorld polarWorld;
        try {
            byte[] polarBytes;
            try {
                polarBytes = Files.readAllBytes(path);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            polarWorld = PolarReader.read(polarBytes);
        } catch (Exception e) {
            PolarPaper.logger().warning("Failed to load world '" + worldName + ".polar'");
            player.sendMessage(Component.text("Failed to load world '" + worldName + ".polar'", NamedTextColor.RED));
            ExceptionUtil.log(e);
            return Command.SINGLE_SUCCESS;
        }

        BlockModifier modifier = new BlockModifier.PosRot(player.getLocation().toVector().toVector3i(), rotation);

        Schematic.paste(polarWorld, player.getWorld(), modifier, Schematic.IgnoreAir.EMPTY_SECTION);

        return Command.SINGLE_SUCCESS;
    }

}
