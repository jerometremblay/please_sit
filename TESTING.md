Quick test
==========

Start the dev client from the project root:

```bash
./gradlew runClient
```

What is already prepared:

- NeoForge 1.21.1 dev runtime
- Minecraft assets downloaded into the local Gradle cache
- Runtime folder initialized at `run/`

First in-game check:

1. Create a Creative world.
2. Search for `Please Sit Controller` in the inventory.
3. Right-click a target block with the Please Sit Controller item to store the seat.
4. Place the Please Sit Controller near a villager.
5. Power it with a lever or redstone torch.
6. The villager should walk to the assigned seat and stay seated while powered.
7. Remove power and the villager should be released beside the seat.

Useful paths:

- Run directory: `run/`
- Built jar: `build/libs/PleaseSit-1.0.0.jar`
