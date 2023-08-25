package net.aerh.skywars.command;

import net.aerh.skywars.SkyWarsPlugin;
import net.aerh.skywars.game.SkyWarsGame;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class GamesCommand implements CommandExecutor {

    private final SkyWarsPlugin plugin;

    public GamesCommand(SkyWarsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        sender.sendMessage(ChatColor.GOLD + "Games (" + plugin.getGames().size() + "):");

        plugin.getGames().forEach(skyWarsGame -> {
            sender.sendMessage(ChatColor.RESET + skyWarsGame.getWorld().getName() + ChatColor.GRAY + " - " + skyWarsGame.getState());
            sender.sendMessage(ChatColor.YELLOW + "  Players: " + ChatColor.RESET + skyWarsGame.getBukkitPlayers().size() + "/" + SkyWarsGame.MAX_PLAYER_COUNT);
            sender.sendMessage(ChatColor.YELLOW + "  Spectators: " + ChatColor.RESET + skyWarsGame.getBukkitSpectators().size());

            if (skyWarsGame.getWinner() != null) {
                sender.sendMessage(ChatColor.YELLOW + "  Winner: " + ChatColor.RESET + skyWarsGame.getWinner().getUuid());
            }
        });

        return false;
    }
}
