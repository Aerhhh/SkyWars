# SkyWars

This is my implementation of the SkyWars minigame. It is quite basic but contains the classic SkyWars experience.

# Setup

This plugin was created for Spigot 1.20.1.

For the plugin to work, you must have at least one world folder in `plugins/SkyWars/map-templates` that includes
a `config.json` file. Maps are randomly chosen from this folder when the plugin starts.

If the `config.json` file from the randomly chosen map cannot be read by the plugin, the server will shut down as it is required
to run.

Islands are defined in the configuration file as a JSON object containing the x, y, and z coordinates of the island. 

Chests can be defined in the configuration file as a JSON object containing the x, y, and z coordinates of the chest, the rotation
of the chest, and the type of chest. The rotation of the chest can be one of `NORTH`, `EAST`, `SOUTH`, or `WEST`. The type of chest
should be either `ISLAND` or `MIDDLE`. 

Alternatively, chests can also be defined on the map by using signs marked with `[chest]` on the first line. The second line 
should contain the type of chest.

See the below map configuration for an example.

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
            "type": "MIDDLE"
        }
    ]
    
}
```

# Commands
| Command      | Permission                | Aliases                        | Description                                                                                    |
|--------------|---------------------------|--------------------------------|------------------------------------------------------------------------------------------------|
| `/games`     | skywars.command.games     | `/listgames`                   | Show basic information for all active games on the server                                      |
| `/start`     | skywars.command.start     | `/sg`, `/startgame`            | Forcefully start the game you are in                                                           |
| `/end`       | skywars.command.end       | `/eg`, `/endgame`, `/stopgame` | Forcefully end the game you are in                                                             |
| `/skipevent` | skywars.command.skipevent | N/A                            | Skip the current event in your game                                                            |
