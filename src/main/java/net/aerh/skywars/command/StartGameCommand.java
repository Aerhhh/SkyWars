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

public class StartGameCommand implements CommandExecutor {

    private final SkyWarsPlugin plugin;

    public StartGameCommand(SkyWarsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can execute this command!");
            return true;
        }

        if (!sender.isOp()) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to execute this command!");
            return true;
        }

        Player player = (Player) sender;
        SkyWarsGame game = plugin.getGameManager().findGame(player);

        if (game == null) {
            player.sendMessage(ChatColor.RED + "You are not in a game!");
            return true;
        }

        if (game.getState() != GameState.PRE_GAME && game.getState() != GameState.STARTING) {
            player.sendMessage(ChatColor.RED + "You can only start the game in the pre-game state!");
            return true;
        }

        if (game.getCountdownTask() != null) {
            game.setCountdownTask(null);
        }

        game.start();
        player.sendMessage(ChatColor.GREEN + "You started the game!");
        return true;
    }
}
