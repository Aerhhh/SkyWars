package net.aerh.skywars.menu;

public class SpectatorSettingsMenu /*extends CustomMenu*/ {

    /*private static final int MAX_SPEED = 5;
    private static final Material[] SPEED_ITEMS = {Material.LEATHER_BOOTS, Material.CHAINMAIL_BOOTS, Material.IRON_BOOTS, Material.GOLDEN_BOOTS, Material.DIAMOND_BOOTS};
    private static final int FIRST_SPEED_SLOT = 11;

    public SpectatorSettingsMenu() {
        super("Spectator Settings", 45, true, true);
    }

    @Override
    protected void initializeItems(Player player) {
        for (int i = 0; i < MAX_SPEED; i++) {
            ItemStack itemStack = new ItemBuilder(SPEED_ITEMS[i])
                .setDisplayName(ChatColor.GREEN + "Speed " + convertNumberToRomanNumeral(i + 1))
                .setLore(ChatColor.GRAY + "Click to select!")
                .addItemFlags(ItemFlag.values())
                .build();

            int finalI = i;
            setItem(FIRST_SPEED_SLOT + i, itemStack, (clicker, event) -> {
                clicker.setFlySpeed((float) (0.1 * (finalI + 1)));
                clicker.sendMessage(ChatColor.YELLOW + "You set your fly speed to " + itemStack.getItemMeta().getDisplayName() + ChatColor.YELLOW + "!");
            });
        }

        SkyWarsPlugin.getInstance().getGameManager().findGame(player).ifPresent(skyWarsGame -> {
            skyWarsGame.getPlayerManager().getSpectator(player.getUniqueId()).ifPresent(skyWarsPlayer -> {
                boolean toggle = skyWarsPlayer.canSeeSpectators();

                ItemStack hideSpectators = new ItemBuilder(Material.ENDER_PEARL)
                    .setDisplayName(ChatColor.YELLOW + "Hide Spectators")
                    .setLore(ChatColor.GRAY + "Click to hide other spectators!")
                    .build();
                ItemStack showSpectators = new ItemBuilder(Material.ENDER_EYE)
                    .setDisplayName(ChatColor.YELLOW + "Show Spectators")
                    .setLore(ChatColor.GRAY + "Click to show other spectators!")
                    .build();

                if (toggle) {
                    setItem(22, hideSpectators, (clicker, event) -> {
                        skyWarsGame.getPlayerManager().getSpectatorsBukkit().forEach(spectator -> clicker.hidePlayer(SkyWarsPlugin.getInstance(), spectator));
                        clicker.sendMessage(ChatColor.YELLOW + "You can no longer see other spectators!");
                        skyWarsPlayer.setCanSeeSpectators(false);
                    });
                } else {
                    setItem(22, showSpectators, (clicker, event) -> {
                        skyWarsGame.getPlayerManager().getSpectatorsBukkit().forEach(spectator -> clicker.showPlayer(SkyWarsPlugin.getInstance(), spectator));
                        clicker.sendMessage(ChatColor.YELLOW + "You can now see other spectators!");
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
            default -> String.valueOf(number);
        };
    }*/
}
