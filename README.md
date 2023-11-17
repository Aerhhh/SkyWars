# SkyWars

This is my implementation of the SkyWars minigame. It is quite basic but contains the classic SkyWars experience.

# Setup

This plugin was created for Spigot 1.20.1.

For the plugin to work, you must have at least one world folder in `plugins/SkyWars/map-templates` that includes
a `config.json` file. Maps are randomly chosen from this folder when the plugin starts.

If the `config.json` file from the chosen map cannot be read by the plugin, the server will shut down as it is required
to run.

Right now, chests are handled through in-game signs with `[chest]` on the first line and either `ISLAND` or `MIDDLE` on
the second line. The plugin will automatically fill the chest with items.

## Example Map Config

```json
{
    "name": "Example Map",
    "locations": {
        "pregame": {
            "x": 0,
            "y": 60,
            "z": 0
        }
    },
    "islands": [
        {
            "x": 57,
            "y": 23,
            "z": 1
        },
        {
            "x": 51,
            "y": 23,
            "z": -22
        },
        {
            "x": 36,
            "y": 23,
            "z": -40
        }
    ],
    "chests": [
        {
            "x": 64,
            "y": 24,
            "z": 4,
            "rotation": "NORTH",
            "type": "ISLAND"
        },
        {
            "x": 64,
            "y": 24,
            "z": -17,
            "rotation": "WEST",
            "type": "ISLAND"
        }
    ]
    
}
```

# Commands

| Command      | Aliases                        | Description                                                                                    |
|--------------|--------------------------------|------------------------------------------------------------------------------------------------|
| `/gameinfo`  | `/gi`                          | Get information on a single game. If no world is specified it defaults to the world you are in |
| `/games`     | `/listgames`                   | Show basic information for all active games on the server                                      |
| `/start`     | `/sg`, `/startgame`            | Forcefully start the game you are in                                                           |
| `/end`       | `/eg`, `/endgame`, `/stopgame` | Forcefully end the game you are in                                                             |
| `/skipevent` | N/A                            | Skip the current event in your game                                                            |
