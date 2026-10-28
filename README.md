# Doctor Knepper's Motorcycles

A vanilla-style, upgradeable motorcycle for Minecraft Java **26.3** (Fabric).

## Features

- **Arcade handling.** Throttle up with W, brake or reverse with S, steer with A/D. The bike climbs full blocks on its own, and steering gets gentler at high speed.
- **Edge braking.** The bike stops instead of driving off a drop deeper than 3 blocks. Hold **Sprint** to go over anyway.
- **Riders are safe from falls.** If the bike drops, the bike takes the damage instead of the rider.
- **Any furnace fuel works.** Put it in the fuel slot. Some fuels are special:
  - **Blaze rod**: burns longer and gives a speed boost while it lasts
  - **Lava bucket**: long burn, and you get the empty bucket back
  - **Dried kelp block**: long burn, cheap and renewable
  - Everything else burns for its furnace burn time
- **Engine upgrades:** Copper → Iron → Diamond → Netherite

  | Engine | Top speed |
  |---|---|
  | Copper | 8 blocks/s |
  | Iron | 11 blocks/s |
  | Diamond | 14 blocks/s |
  | Netherite | 18 blocks/s |

  For comparison, sprinting is about 5.6 blocks/s.

- **Large Fuel Tank:** triples fuel capacity (about 10 → 30 minutes of riding).
- **Storage:** up to 3 Saddlebags plus a 27-slot Motorcycle Trunk.
- **Sidecars:** add one on each side, for up to 4 riders. Friends hop in, and mobs you're leading on a lead climb in when you right-click the bike. Put a lead on a sidecar passenger to lead it back out. A bike with sidecars is wider (2 blocks with one, about 3 with two).
- **Headlight** that really lights the ground ahead (stops mob spawns). Toggle it with **H**.
- **Horn:** beep-beep with **G**.
- **Dyeable paint:** right-click the bike with any dye. Sidecars match the paint.
- **Water:** in deep water the engine floods and the bike floats. You can paddle it slowly back to shore.
- **Ramming:** hitting hostile and neutral mobs at speed hurts them and the bike. Players, tamed pets, villagers, golems, named mobs and mobs on a lead are never hurt.
- **Repair:** right-click with an iron ingot. A destroyed bike drops its upgrades, cargo and some iron.
- **Owner lock:** whoever places a bike owns it. Only the owner can drive it, open it, pick it up or damage it. Others can still ride along as passengers. Giving someone the bike item gives them the bike.

## Controls

| Action | Input |
|---|---|
| Get on / ride along | Right-click |
| Open bike inventory and upgrades | Sneak + right-click (or your inventory key while riding) |
| Pick the bike up as an item (keeps upgrades, cargo and fuel) | Sneak + punch it, with nobody aboard (any punch in creative) |
| Load mobs you're leading into the sidecars | Right-click the bike |
| Drive over an edge | Hold Sprint |
| Headlight on/off | H |
| Horn | G |
| Get off | Sneak |

All keys can be changed in Options → Controls → Doctor Knepper's Motorcycles.

## Recipes

| Item | Ingredients |
|---|---|
| Motorcycle | Copper Engine, 3 iron ingots, leather, 2 black wool |
| Copper Engine | 7 copper ingots, redstone, furnace |
| Iron Engine | Copper Engine, 7 iron ingots, redstone |
| Diamond Engine | Iron Engine, 4 diamonds, redstone block |
| Netherite Engine | Smithing table: netherite upgrade template + Diamond Engine + netherite ingot |
| Large Fuel Tank | 8 iron ingots, bucket |
| Saddlebag | 5 leather, 2 string, gold nugget |
| Motorcycle Trunk | 8 iron ingots, chest |
| Sidecar | Minecart, 2 iron ingots, leather |

Recipes unlock in the recipe book as you collect the ingredients.

## Server settings

The first launch creates `config/dkmotorcycles.json`:

| Setting | Default | What it does |
|---|---|---|
| `engineSpeedsBlocksPerSecond` | 8 / 11 / 14 / 18 | Top speed of each engine |
| `fuelUseMultiplier` | 1.0 | 0.5 makes fuel last twice as long |
| `rammingDamageMultiplier` | 1.0 | 0 turns ramming damage off |
| `rammingHurtsPlayers` | false | Whether bikes hurt other players |
| `edgeBrakingDepth` | 3 | Brake at drops deeper than this (0 = off) |
| `ownerLock` | true | Only the owner can drive, open or pick up a bike |

The server's settings are sent to players, so everyone on a server rides by the same rules.

## Building

Requires Java 25.

```
./gradlew build
```

The mod jar is written to `build/libs/`. Install it with Fabric Loader and Fabric API in your `mods` folder.

Tests and screenshots run in a real game client (on a headless machine, use `xvfb-run`):

- `./gradlew runClientGameTest -PbehaviorTests` plays the mod and checks the riding rules.
- `./gradlew runClientGameTest` stages scenes and saves promo screenshots to `build/run/clientGameTest/screenshots`.

## License

[CC0 1.0 Universal](LICENSE). Public domain: use it however you like.
