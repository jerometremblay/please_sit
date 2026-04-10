Quick test
==========

Start the dev client from the project root:

```bash
./gradlew runClient
```

What is already prepared:

- NeoForge 21.1.x dev runtime on Minecraft 1.21.x
- Minecraft assets downloaded into the local Gradle cache
- Runtime folder initialized at `run/`

First in-game check:

1. Create a Creative world.
2. Search for `Please Sit Controller` in the inventory.
3. Right-click a target block with the Please Sit Controller item to store the seat.
4. Optionally, sneak-right-click a villager with the item to lock that villager to the controller.
5. Place the Please Sit Controller near villagers.
6. Power it with a lever or redstone torch.
7. Without a villager lock, an eligible nearby villager should walk to the assigned seat and stay seated while powered.
8. If the controller was locked to a villager, only that villager should respond and other villagers should ignore that controller.
9. Remove power and the villager should be released beside the seat.
10. Open the Mods list, select `Please Sit`, press `Config`, and confirm `Villager Search Radius` can be changed in the NeoForge config screen.

Useful paths:

- Run directory: `run/`
- Built jar: `build/libs/PleaseSit-1.0.1.jar`
