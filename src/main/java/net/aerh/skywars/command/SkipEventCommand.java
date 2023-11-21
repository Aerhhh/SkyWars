package net.aerh.skywars.command;

import net.aerh.skywars.SkyWarsPlugin;
import net.aerh.skywars.game.GameState;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SkipEventCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can execute this command!");
            return true;
        }

        SkyWarsPlugin.getInstance().getGameManager().findGame(player).ifPresentOrElse(skyWarsGame -> {
            if (skyWarsGame.getState() != GameState.IN_GAME) {
                player.sendMessage(ChatColor.RED + "You can only skip events when the game is running!");
                return;
            }

            skyWarsGame.getGameLoop().getNextEvent().ifPresentOrElse(gameEvent -> skyWarsGame.getGameLoop().next(), skyWarsGame::end);
        }, () -> player.sendMessage(ChatColor.RED + "You are not in a game!"));

        return true;
    }
}
