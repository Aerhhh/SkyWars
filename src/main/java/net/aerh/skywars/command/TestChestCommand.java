package net.aerh.skywars.command;

import net.aerh.skywars.SkyWarsPlugin;
import net.aerh.skywars.game.SkyWarsGame;
import net.aerh.skywars.game.chest.ChestType;
import net.aerh.skywars.game.chest.RefillableChest;
import net.aerh.skywars.util.Utils;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class TestChestCommand implements CommandExecutor {

    private final SkyWarsPlugin plugin;

    public TestChestCommand(SkyWarsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can execute this command!");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            player.sendMessage(ChatColor.RED + "Usage: /" + label + " <chestType>");
            return true;
        }

        SkyWarsGame skyWarsGame = plugin.findGame(player);

        if (skyWarsGame == null) {
            player.sendMessage(ChatColor.RED + "You are not in a game!");
            return true;
        }

        ChestType chestType = ChestType.valueOfOrElse(args[0].toUpperCase(), ChestType.ISLAND);
        RefillableChest refillableChest = new RefillableChest(player.getLocation(), chestType);
        skyWarsGame.getRefillableChests().add(refillableChest);
        refillableChest.spawn();
        refillableChest.refillChest();
        //Bukkit.getScheduler().runTaskLater(plugin, refillableChest::refillChest, 1L);

        player.sendMessage(ChatColor.GREEN + "Spawned " + refillableChest.getType() + " chest at " + Utils.parseLocationToString(refillableChest.getLocation()) + "!");
        return true;
    }
}
