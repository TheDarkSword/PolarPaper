package live.minehub.polarpaper.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import live.minehub.polarpaper.PolarPaper;
import live.minehub.polarpaper.PolarReader;
import live.minehub.polarpaper.PolarWorld;
import live.minehub.polarpaper.schematic.BlockModifier;
import live.minehub.polarpaper.schematic.Schematic;
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
        if (!(sender instanceof Player player)) {
            ctx.getSource().getSender().sendMessage(
                    Component.text()
                            .append(Component.text("Usage: /polar paste <world> [rotation] (while in a world)", NamedTextColor.RED))
            );
            return Command.SINGLE_SUCCESS;
        }

        String worldName = ctx.getArgument("worldname", String.class);
        String rotationString = ctx.getArgument("rotation", String.class);

        Rotation rotation = Rotation.NONE;
        if (rotationString != null) {
            try {
                rotation = Rotation.valueOf(rotationString.toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }

        Path pluginFolder = Path.of(PolarPaper.getPlugin().getDataFolder().getAbsolutePath());
        Path worldsFolder = pluginFolder.resolve("worlds");
        Path path = worldsFolder.resolve(worldName + ".polar");

        byte[] polarBytes;
        try {
            polarBytes = Files.readAllBytes(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        PolarWorld polarWorld = PolarReader.read(polarBytes);

        BlockModifier modifier = new BlockModifier.PosRot(player.getLocation().toVector().toVector3i(), rotation);

        Schematic.paste(polarWorld, player.getWorld(), modifier, Schematic.IgnoreAir.EMPTY_SECTION);

        return Command.SINGLE_SUCCESS;
    }
}
