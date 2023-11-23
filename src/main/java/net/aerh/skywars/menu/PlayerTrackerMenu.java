package net.aerh.skywars.menu;

import net.aerh.skywars.SkyWarsPlugin;
import net.aerh.skywars.util.ItemBuilder;
import net.aerh.skywars.util.Utils;
import net.aerh.skywars.util.menu.CustomMenu;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
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
                    ItemStack playerHead = new ItemBuilder(Material.PLAYER_HEAD)
                        .setPlayer(alivePlayer)
                        .setDisplayName(ChatColor.GOLD + alivePlayer.getDisplayName())
                        .setLore(new ArrayList<>(List.of(
                            ChatColor.GRAY + "Health: " + ChatColor.WHITE + Utils.TWO_DECIMAL_PLACES_FORMAT.format(alivePlayer.getHealth()) + "❤",
                            " ",
                            ChatColor.YELLOW + "Click to teleport!"
                        )))
                        .build();

                    addItem(playerHead, (p, event) -> p.teleport(alivePlayer));
                });
        });
    }
}
