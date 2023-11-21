package net.aerh.skywars.menu;

import net.aerh.skywars.SkyWarsPlugin;
import net.aerh.skywars.util.Utils;
import net.aerh.skywars.util.menu.CustomMenu;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.List;

public class PlayerTrackerMenu extends CustomMenu {

    public PlayerTrackerMenu() {
        super("Remaining Players", 54, true, true);
    }

    @Override
    protected void initializeItems(Player player) {
        SkyWarsPlugin.getInstance().getGameManager().findGame(player).ifPresent(skyWarsGame -> {
            skyWarsGame.getAlivePlayers().stream()
                .filter(skyWarsPlayer -> skyWarsPlayer.getBukkitPlayer().isPresent())
                .map(skyWarsPlayer -> skyWarsPlayer.getBukkitPlayer().get())
                .forEach(alivePlayer -> {
                    ItemStack itemStack = new ItemStack(Material.PLAYER_HEAD);
                    SkullMeta skullMeta = (SkullMeta) itemStack.getItemMeta();
                    skullMeta.setOwningPlayer(alivePlayer);
                    skullMeta.setDisplayName(ChatColor.GOLD + alivePlayer.getDisplayName());
                    skullMeta.setLore(List.of(
                        ChatColor.GRAY + "Health: " + ChatColor.WHITE + Utils.TWO_DECIMAL_PLACES_FORMAT.format(alivePlayer.getHealth()) + "❤",
                        "",
                        ChatColor.YELLOW + "Click to teleport!"
                    ));
                    itemStack.setItemMeta(skullMeta);

                    addItem(itemStack, (p, event) -> p.teleport(alivePlayer));
                });
        });
    }
}
