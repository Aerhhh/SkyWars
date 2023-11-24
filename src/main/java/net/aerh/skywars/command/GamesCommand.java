package net.aerh.skywars.command;

import net.aerh.skywars.SkyWarsPlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class GamesCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append(ChatColor.GOLD)
            .append("Games (")
            .append(SkyWarsPlugin.getInstance().getGameManager().getGames().size())
            .append("):")
            .append("\n");

        SkyWarsPlugin.getInstance().getGameManager().getGames().forEach(skyWarsGame -> {
            stringBuilder.append(ChatColor.YELLOW)
                .append(skyWarsGame.getWorld().getName()).append(": ").append(ChatColor.RESET).append(skyWarsGame.getState()).append("\n")
                .append(ChatColor.YELLOW).append("  Next Event: ").append(ChatColor.RESET);

            skyWarsGame.getGameLoop().getNextEvent().ifPresentOrElse(gameEvent -> {
                stringBuilder.append(gameEvent.getDisplayName())
                    .append(" in ")
                    .append(skyWarsGame.getGameLoop().getFormattedTimeToNextEvent())
                    .append(ChatColor.GRAY)
                    .append(" (")
                    .append(gameEvent.getClass().getSimpleName())
                    .append(")");
            }, () -> stringBuilder.append("None"));

            stringBuilder.append("\n").append(ChatColor.YELLOW).append("  Current Event: ").append(ChatColor.RESET);

            skyWarsGame.getGameLoop().getCurrentEvent().ifPresentOrElse(event -> {
                stringBuilder.append(ChatColor.RESET)
                    .append(event.getDisplayName())
                    .append(ChatColor.GRAY)
                    .append(" (")
                    .append(event.getClass().getSimpleName())
                    .append(")");
            }, () -> stringBuilder.append(ChatColor.RESET).append("None"));

            stringBuilder.append("\n")
                .append(ChatColor.YELLOW).append("  Islands: ").append(ChatColor.RESET).append(skyWarsGame.getIslands().size()).append("\n")
                .append(ChatColor.YELLOW).append("  Chests: ").append(ChatColor.RESET).append(skyWarsGame.getRefillableChests().size()).append("\n")
                .append(ChatColor.YELLOW).append("  Players: ").append(ChatColor.RESET).append(skyWarsGame.getPlayerManager().getPlayersBukkit().size()).append("/")
                .append(skyWarsGame.getMaxPlayers()).append("\n")
                .append(ChatColor.YELLOW).append("  Spectators: ").append(ChatColor.RESET).append(skyWarsGame.getPlayerManager().getSpectatorsBukkit().size()).append("\n")
                .append(ChatColor.YELLOW).append("  Online: ").append(ChatColor.RESET).append(skyWarsGame.getPlayerManager().getOnlinePlayers().size()).append("\n")
                .append(ChatColor.YELLOW).append("  Winner: ").append(ChatColor.RESET);

            skyWarsGame.getWinner().ifPresentOrElse(skyWarsPlayer -> stringBuilder.append(skyWarsPlayer.getUuid()).append("\n"),
                () -> stringBuilder.append("None").append("\n")
            );
        });

        sender.sendMessage(stringBuilder.toString());
        return true;
    }
}
