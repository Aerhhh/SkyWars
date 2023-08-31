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
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage("Only players can execute this command!");
            return true;
        }

        Player player = (Player) commandSender;
        SkyWarsGame game = plugin.findGame(player);

        if (args.length > 0) {
            game = plugin.getGame(args[0]);
        }

        if (game == null) {
            if (args.length > 0) {
                player.sendMessage(ChatColor.RED + "That world doesn't seem to hold a game!");
            } else {
                player.sendMessage(ChatColor.RED + "You are not in a game!");
            }
            return true;
        }

        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append(ChatColor.GOLD + "Game Info (").append(game.getWorld().getName()).append("):").append("\n")
            .append(ChatColor.YELLOW).append("  State: ").append(ChatColor.RESET).append(game.getState()).append("\n")
            .append(ChatColor.YELLOW).append("  Players: ").append(ChatColor.RESET).append(game.getBukkitPlayers().size()).append("/").append(SkyWarsGame.MAX_PLAYER_COUNT).append("\n")
            .append(ChatColor.YELLOW).append("  Spectators: ").append(ChatColor.RESET).append(game.getBukkitSpectators().size()).append("\n")
            .append(ChatColor.YELLOW).append("  Events Remaining: ").append(ChatColor.RESET).append(game.getGameLoop().getGameEvents().size()).append("\n")
            .append(ChatColor.YELLOW).append("  Next Event: ").append(ChatColor.RESET).append("In ").append(game.getGameLoop().getTimeUntilNextEvent())
            .append(ChatColor.GRAY).append(" (").append(game.getGameLoop().getNextEvent().getClass().getSimpleName()).append(")").append("\n")
            .append(ChatColor.YELLOW).append("  Winner: ").append(ChatColor.RESET).append(game.getWinner() != null ? game.getWinner().getUuid() : "None").append("\n")
            .append(ChatColor.YELLOW).append("  Islands: ").append(ChatColor.RESET).append(game.getIslands().size()).append("\n")
            .append(ChatColor.YELLOW).append("  Chests: ").append(ChatColor.RESET).append(game.getRefillableChests().size()).append("\n");

        player.sendMessage(stringBuilder.toString());
        return false;
    }
}
