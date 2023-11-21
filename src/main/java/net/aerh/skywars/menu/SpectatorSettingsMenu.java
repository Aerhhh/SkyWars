package net.aerh.skywars.menu;

import net.aerh.skywars.SkyWarsPlugin;
import net.aerh.skywars.util.menu.CustomMenu;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class SpectatorSettingsMenu extends CustomMenu {

    private static final int MAX_SPEED = 5;
    private static final Material[] SPEED_ITEMS = {Material.LEATHER_BOOTS, Material.CHAINMAIL_BOOTS, Material.IRON_BOOTS, Material.GOLDEN_BOOTS, Material.DIAMOND_BOOTS};
    private static final int FIRST_SPEED_SLOT = 11;

    public SpectatorSettingsMenu() {
        super("Spectator Settings", 45, true, true);
    }

    @Override
    protected void initializeItems(Player player) {
        for (int i = 0; i < MAX_SPEED; i++) {
            ItemStack itemStack = new ItemStack(SPEED_ITEMS[i]);
            ItemMeta itemMeta = itemStack.getItemMeta();
            itemMeta.setDisplayName(ChatColor.GREEN + "Speed " + convertNumberToRomanNumeral(i + 1));
            itemMeta.setLore(List.of(ChatColor.GRAY + "Click to select!"));
            itemMeta.addItemFlags(ItemFlag.values());
            itemStack.setItemMeta(itemMeta);

            int finalI = i;
            setItem(FIRST_SPEED_SLOT + i, itemStack, (clicker, event) -> {
                clicker.setFlySpeed((float) (0.1 * (finalI + 1)));
                clicker.sendMessage(ChatColor.YELLOW + "You set your fly speed to " + itemStack.getItemMeta().getDisplayName() + ChatColor.YELLOW + "!");
            });
        }

        SkyWarsPlugin.getInstance().getGameManager().findGame(player).ifPresent(skyWarsGame -> {
            skyWarsGame.getSpectator(player).ifPresent(skyWarsPlayer -> {
                boolean toggle = skyWarsPlayer.canSeeSpectators();
                // TODO create an item builder for this mess
                ItemStack hideSpectatorsItemStack = new ItemStack(Material.ENDER_PEARL);
                ItemMeta hideSpectatorsItemMeta = hideSpectatorsItemStack.getItemMeta();
                hideSpectatorsItemMeta.setDisplayName(ChatColor.GREEN + "Hide Spectators");
                hideSpectatorsItemMeta.setLore(List.of(ChatColor.GRAY + "Click to hide other spectators!"));
                hideSpectatorsItemStack.setItemMeta(hideSpectatorsItemMeta);
                ItemStack showSpectatorsItemStack = new ItemStack(Material.ENDER_EYE);
                ItemMeta showSpectatorsItemMeta = showSpectatorsItemStack.getItemMeta();
                showSpectatorsItemMeta.setDisplayName(ChatColor.RED + "Show Spectators");
                showSpectatorsItemMeta.setLore(List.of(ChatColor.GRAY + "Click to show other spectators!"));
                showSpectatorsItemStack.setItemMeta(showSpectatorsItemMeta);

                if (toggle) {
                    setItem(22, hideSpectatorsItemStack, (clicker, event) -> {
                        skyWarsGame.getBukkitSpectators().forEach(spectator -> clicker.hidePlayer(SkyWarsPlugin.getInstance(), spectator));
                        clicker.sendMessage(ChatColor.GREEN + "You can no longer see other spectators!");
                        skyWarsPlayer.setCanSeeSpectators(false);
                    });
                } else {
                    setItem(22, showSpectatorsItemStack, (clicker, event) -> {
                        skyWarsGame.getBukkitSpectators().forEach(spectator -> clicker.showPlayer(SkyWarsPlugin.getInstance(), spectator));
                        clicker.sendMessage(ChatColor.RED + "You can now see other spectators!");
                        skyWarsPlayer.setCanSeeSpectators(true);
                    });
                }
            });
        });
    }

    private String convertNumberToRomanNumeral(int number) {
        return switch (number) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> "";
        };
    }
}
