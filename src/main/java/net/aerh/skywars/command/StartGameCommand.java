package net.aerh.skywars.command;

import net.aerh.skywars.SkyWarsPlugin;
import net.aerh.skywars.game.state.GameState;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class StartGameCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can execute this command!");
            return true;
        }

        SkyWarsPlugin.getInstance().getGameManager().findGame(player).ifPresentOrElse(skyWarsGame -> {
            if (skyWarsGame.getState() != GameState.PRE_GAME && skyWarsGame.getState() != GameState.STARTING) {
                player.sendMessage(ChatColor.RED + "You can only start the game in the pre-game state!");
                return;
            }

            skyWarsGame.start();
            player.sendMessage(ChatColor.GREEN + "You started the game!");
        }, () -> player.sendMessage(ChatColor.RED + "You are not in a game!"));

        return true;
    }
}
