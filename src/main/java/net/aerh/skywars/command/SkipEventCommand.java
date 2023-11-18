package net.aerh.skywars.command;

import net.aerh.skywars.SkyWarsPlugin;
import net.aerh.skywars.game.GameState;
import net.aerh.skywars.game.SkyWarsGame;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SkipEventCommand implements CommandExecutor {

    private final SkyWarsPlugin plugin;

    public SkipEventCommand(SkyWarsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can execute this command!");
            return true;
        }

        SkyWarsGame game = plugin.getGameManager().findGame(player);

        if (game == null) {
            player.sendMessage(ChatColor.RED + "You are not in a game!");
            return true;
        }

        if (game.getState() != GameState.IN_GAME) {
            player.sendMessage(ChatColor.RED + "You can only skip events when the game is running!");
            return true;
        }

        game.getGameLoop().getNextEvent().ifPresentOrElse(gameEvent -> game.getGameLoop().next(true), () -> player.sendMessage(ChatColor.RED + "There is no next event!"));
        return true;
    }
}
