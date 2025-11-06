package live.minehub.polarpaper.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import live.minehub.polarpaper.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.World;

import java.util.Map;

public class ReloadConfigCommand {

    protected static int run(CommandContext<CommandSourceStack> ctx) {
        PolarPaper.getPlugin().reloadConfig();

        int numWorlds = 0;
        for (World bukkitWorld : Bukkit.getWorlds()) {
            if (!Polar.isInConfig(bukkitWorld.getName())) continue;

            PolarWorld world = PolarWorld.fromWorld(bukkitWorld);
            if (world == null) continue;
            PolarGenerator generator = PolarGenerator.fromWorld(bukkitWorld);
            if (generator == null) continue;

            Config config = Config.readFromConfig(PolarPaper.getPlugin().getConfig(), bukkitWorld.getName());

            generator.setConfig(config);

            bukkitWorld.setDifficulty(org.bukkit.Difficulty.valueOf(config.difficulty().name()));
            bukkitWorld.setSpawnFlags(config.allowMonsters(), config.allowAnimals());

            for (Map.Entry<String, Object> gamerule : config.gamerules().entrySet()) {
                GameRule<?> rule = GameRule.getByName(gamerule.getKey());
                if (rule == null) continue;
                Polar.setGameRule(bukkitWorld, rule, gamerule.getValue());
            }

            Polar.startAutoSaveTask(bukkitWorld, config);

            numWorlds++;
        }

        ctx.getSource().getSender().sendMessage(
                Component.text()
                        .append(Component.text("Reloaded config for ", NamedTextColor.AQUA))
                        .append(Component.text(numWorlds, NamedTextColor.AQUA))
                        .append(Component.text(" worlds", NamedTextColor.AQUA))
        );

        return Command.SINGLE_SUCCESS;
    }

}

