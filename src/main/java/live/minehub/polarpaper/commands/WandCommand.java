package live.minehub.polarpaper.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class WandCommand {

    private static final ItemStack ITEM_STACK = ItemStack.of(Material.BREEZE_ROD);
    public static final NamespacedKey ITEM_STACK_KEY = new NamespacedKey("polarpaper", "wand");

    static {
        ITEM_STACK.editMeta(ItemMeta.class, meta -> {
            meta.displayName(Component.text("Polar Wand", NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
            PersistentDataContainer data = meta.getPersistentDataContainer();
            data.set(ITEM_STACK_KEY, PersistentDataType.BOOLEAN, true);
        });
    }

    protected static int run(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        if (!(sender instanceof Player player)) {
            ctx.getSource().getSender().sendMessage(
                    Component.text()
                            .append(Component.text("Usage: /polar wand (while in a world)", NamedTextColor.RED))
            );
            return Command.SINGLE_SUCCESS;
        }

        boolean success = player.getInventory().addItem(ITEM_STACK).isEmpty();
        if (!success) {
            player.sendMessage(Component.text("Cannot give you a wand because your inventory is full!", NamedTextColor.RED));
        }
        player.sendMessage(Component.text("You have been given a polar wand", NamedTextColor.AQUA));

        return Command.SINGLE_SUCCESS;
    }

}
