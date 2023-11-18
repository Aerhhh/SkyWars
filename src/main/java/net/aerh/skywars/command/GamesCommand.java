package net.aerh.skywars.command;

import net.aerh.skywars.SkyWarsPlugin;
import net.aerh.skywars.game.SkyWarsGame;
import net.aerh.skywars.util.Utils;
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

            skyWarsGame.getGameLoop().getCurrentEvent().ifPresentOrElse(gameEvent -> {
                stringBuilder.append(gameEvent.getDisplayName()).append(" in ").append(Utils.formatTime(skyWarsGame.getGameLoop().getSecondsToNextEvent()))
                    .append(ChatColor.GRAY).append(" (").append(gameEvent.getClass().getSimpleName()).append(")");
            }, () -> stringBuilder.append("None"));

            stringBuilder.append("\n")
                .append(ChatColor.YELLOW).append("  Islands: ").append(ChatColor.RESET).append(skyWarsGame.getIslands().size()).append("\n")
                .append(ChatColor.YELLOW).append("  Chests: ").append(ChatColor.RESET).append(skyWarsGame.getRefillableChests().size()).append("\n")
                .append(ChatColor.YELLOW).append("  Players: ").append(ChatColor.RESET).append(skyWarsGame.getBukkitPlayers().size()).append("/").append(SkyWarsGame.MAX_PLAYER_COUNT).append("\n")
                .append(ChatColor.YELLOW).append("  Spectators: ").append(ChatColor.RESET).append(skyWarsGame.getBukkitSpectators().size()).append("\n")
                .append(ChatColor.YELLOW).append("  Winner: ").append(ChatColor.RESET);

            skyWarsGame.getWinner().ifPresentOrElse(skyWarsPlayer -> stringBuilder.append(skyWarsPlayer.getUuid()).append("\n"),
                () -> stringBuilder.append("None").append("\n")
            );
        });

        sender.sendMessage(stringBuilder.toString());
        return true;
    }
}
