# Please Sit

`Please Sit` is a NeoForge mod for Minecraft 1.21.x that adds a redstone-controlled villager controller. You select a target seat block first, place the control block, and then use redstone to call and release villagers.

![CurseForge thumbnail](docs/media/curseforge-thumbnail.png)

## Demo


- [Watch the demo video](https://www.youtube.com/watch?v=tpMgrF_vI0c)

## How It Works

- Use the Please Sit Controller item on a target block to store the seat location.
- Optionally, sneak-right-click a villager with the item to lock that villager to the controller item.
- Place the control block where you want the wiring.
- Power the block to call a villager to the target seat.
- If the controller was locked to a villager, only that villager will respond. Otherwise, the controller will pick an eligible nearby villager.
- Remove power to release the villager to a nearby clear block.

## Settings

- The mod uses NeoForge's built-in config screen.
- Open the Mods list, select `Please Sit`, and press `Config` to adjust settings.
- `Villager Search Radius` controls how far from the target seat the controller searches for a villager.

## Recipe

```text
R C R
W S W
P I P
```

- `R` = Redstone Dust
- `C` = Comparator
- `W` = Any wooden slab
- `S` = Stick
- `P` = Any planks
- `I` = Iron Ingot

## Requirements

- Minecraft `1.21.x` (developed against `1.21.1`)
- NeoForge `21.1.x` (developed against `21.1.221`)

## Development

```bash
./gradlew runClient
./gradlew build
```
