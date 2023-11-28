package net.aerh.skywars.menu;

import net.aerh.skywars.SkyWarsPlugin;
import net.aerh.skywars.game.SkyWarsGame;
import net.aerh.skywars.util.ItemBuilder;
import net.aerh.skywars.util.Utils;
import net.aerh.skywars.util.menu.CustomMenu;
import net.aerh.skywars.util.menu.PlayerTaskWrapper;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class PlayerTrackerCustomMenu extends CustomMenu {

    public PlayerTrackerCustomMenu(SkyWarsGame skyWarsGame) {
        super("Player Teleporter", (int) Math.ceil((double) skyWarsGame.getPlayerManager().getAlivePlayers().size() / 9));
    }

    @Override
    public void initializeElements(Player player) {
        SkyWarsPlugin.getInstance().getGameManager().findGame(player).ifPresent(skyWarsGame -> {
            skyWarsGame.getPlayerManager().getAlivePlayers().stream()
                .filter(skyWarsPlayer -> skyWarsPlayer.getBukkitPlayer().isPresent())
                .map(skyWarsPlayer -> skyWarsPlayer.getBukkitPlayer().get())
                .forEach(alivePlayer -> {
                    ItemStack itemStack = new ItemBuilder(Material.PLAYER_HEAD)
                        .setPlayer(alivePlayer)
                        .setDisplayName(ChatColor.GOLD + alivePlayer.getDisplayName())
                        .setLore(List.of(
                            ChatColor.GRAY + "Health: " + ChatColor.WHITE + Utils.TWO_DECIMAL_PLACES_FORMAT.format(alivePlayer.getHealth()) + "❤",
                            " ",
                            ChatColor.YELLOW + "Click to teleport!"
                        ))
                        .build();

                    addAnimatedElement(itemStack, event -> event.getWhoClicked().teleport(alivePlayer.getLocation()),
                        new PlayerTaskWrapper((player1, inventoryItem) -> {
                            ItemMeta itemMeta = inventoryItem.getItemStack().getItemMeta();
                            itemMeta.getLore().set(0, ChatColor.GRAY + "Health: " + ChatColor.WHITE + Utils.TWO_DECIMAL_PLACES_FORMAT.format(alivePlayer.getHealth()) + "❤");
                            inventoryItem.getItemStack().setItemMeta(itemMeta);

                            if (skyWarsGame.getPlayerManager().isSpectator(alivePlayer)) {
                                removeElement(inventoryItem);
                                //Bukkit.broadcastMessage("Shifting elements for " + player.getDisplayName() + "'s menu");
                                //shiftElements(player);
                            }
                        })
                    );
                });
        });

        super.initializeElements(player);
    }
}
