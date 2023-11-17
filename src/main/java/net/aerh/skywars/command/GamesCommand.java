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
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(ChatColor.GOLD).append("Games (").append(plugin.getGameManager().getGames().size()).append("):").append("\n");

        plugin.getGameManager().getGames().forEach(skyWarsGame -> {
            stringBuilder.append(ChatColor.YELLOW).append(skyWarsGame.getWorld().getName()).append(": ").append(ChatColor.RESET).append(skyWarsGame.getState()).append("\n")
                .append(ChatColor.YELLOW).append("  Next Event: ").append(ChatColor.RESET);


            if (skyWarsGame.getGameLoop().getNextEvent() != null) {
                stringBuilder.append("In ").append(skyWarsGame.getGameLoop().getTimeUntilNextEvent())
                    .append(" (").append(skyWarsGame.getGameLoop().getNextEvent().getDisplayName()).append(")");
            } else {
                stringBuilder.append("None");
            }

            stringBuilder.append("\n")
                .append(ChatColor.YELLOW).append("  Islands: ").append(ChatColor.RESET).append(skyWarsGame.getIslands().size()).append("\n")
                .append(ChatColor.YELLOW).append("  Chests: ").append(ChatColor.RESET).append(skyWarsGame.getRefillableChests().size()).append("\n")
                .append(ChatColor.YELLOW).append("  Players: ").append(ChatColor.RESET).append(skyWarsGame.getBukkitPlayers().size()).append("/").append(SkyWarsGame.MAX_PLAYER_COUNT).append("\n")
                .append(ChatColor.YELLOW).append("  Spectators: ").append(ChatColor.RESET).append(skyWarsGame.getBukkitSpectators().size()).append("\n");

            if (skyWarsGame.getWinner() != null) {
                stringBuilder.append(ChatColor.YELLOW).append("  Winner: ").append(ChatColor.RESET).append(skyWarsGame.getWinner().getUuid());
            }
        });

        sender.sendMessage(stringBuilder.toString());
        return true;
    }
}
