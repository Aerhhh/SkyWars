package net.aerh.skywars.command;

import net.aerh.skywars.SkyWarsPlugin;
import net.aerh.skywars.game.SkyWarsGame;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class GameInfoCommand implements CommandExecutor {

    private final SkyWarsPlugin plugin;

    public GameInfoCommand(SkyWarsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage("Only players can execute this command!");
            return true;
        }

        Player player = (Player) commandSender;
        SkyWarsGame game = plugin.findGame(player);

        if (game == null) {
            player.sendMessage(ChatColor.RED + "You are not in a game!");
            return true;
        }

        String stringBuilder = ChatColor.GOLD + "Game Info (" + game.getWorld().getName() + "):" + "\n" +
            ChatColor.YELLOW + "  State: " + ChatColor.RESET + game.getState() + "\n" +
            ChatColor.YELLOW + "  Players: " + ChatColor.RESET + game.getBukkitPlayers().size() + "/" + SkyWarsGame.MAX_PLAYER_COUNT + "\n" +
            ChatColor.YELLOW + "  Spectators: " + ChatColor.RESET + game.getBukkitSpectators().size() + "\n" +
            ChatColor.YELLOW + "  Events: " + ChatColor.RESET + game.getGameLoop().getGameEvents().size() + "\n" +
            ChatColor.YELLOW + "  Next Event: " + ChatColor.RESET + "In " + game.getGameLoop().getTimeUntilNextEvent() +
            ChatColor.GRAY + " (" + game.getGameLoop().getNextEvent().getClass().getSimpleName() + ")" + "\n" +
            ChatColor.YELLOW + "  Winner: " + ChatColor.RESET + (game.getWinner() != null ? game.getWinner().getUuid() : "None") + "\n" +
            ChatColor.YELLOW + "  Islands: " + ChatColor.RESET + game.getIslands().size() + "\n" +
            ChatColor.YELLOW + "  Chests: " + ChatColor.RESET + game.getRefillableChests().size() + "\n";

        player.sendMessage(stringBuilder);
        return false;
    }
}
