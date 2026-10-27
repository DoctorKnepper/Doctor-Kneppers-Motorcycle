# Doctor Knepper's Motorcycles

A vanilla-style, upgradeable motorcycle for Minecraft Java **26.3** (Fabric).

## Features

- **Arcade handling** — throttle up with W, brake/reverse with S, steer with A/D. Steps up full blocks automatically.
- **Any furnace fuel works** — drop it in the fuel slot. Some fuels are special:
  - **Blaze rod** — burns longer and gives a speed boost while it lasts
  - **Lava bucket** — long burn, hands the empty bucket back
  - **Dried kelp block** — long burn, cheap and renewable
  - Everything else burns according to its furnace burn time
- **Engine upgrades** — Copper → Iron → Diamond → Netherite

  | Engine | Top speed (approx.) |
  |---|---|
  | Copper | 13 blocks/s |
  | Iron | 18 blocks/s |
  | Diamond | 25 blocks/s |
  | Netherite | 34 blocks/s |

- **Fuel tank upgrade** — triples fuel capacity (about 10 → 30 minutes of riding)
- **Storage** — up to 3 saddlebags plus a 27-slot, chest-sized trunk
- **Second passenger** seat on the back
- **Headlight** — really lights the ground ahead (stops mob spawns). Toggle with **H**.
- **Dyeable paint** — right-click the bike with any dye
- **Ramming** — hitting mobs at speed damages them and the bike
- **Repair** — right-click with an iron ingot. A destroyed bike drops its upgrades, cargo and some iron.

## Controls

| Action | Input |
|---|---|
| Mount | Right-click |
| Open bike inventory / upgrades | Sneak + right-click (or your inventory key while riding) |
| Headlight on/off | H (rebindable) |
| Dismount | Sneak |

## Recipes

| Item | Ingredients |
|---|---|
| Motorcycle | Copper engine, 3 iron ingots, leather, 2 black wool |
| Copper engine | 7 copper ingots, redstone, furnace |
| Iron engine | Copper engine, 7 iron ingots, redstone |
| Diamond engine | Iron engine, 4 diamonds, redstone block |
| Netherite engine | Smithing table: netherite upgrade template + diamond engine + netherite ingot |
| Fuel tank | 8 iron ingots, bucket |
| Saddlebag | 5 leather, 2 string, gold nugget |
| Trunk | 8 iron ingots, chest |

Recipes unlock in the recipe book as you collect the ingredients.

## Building

Requires Java 25.

```
./gradlew build
```

The mod jar is written to `build/libs/`. Install it with Fabric Loader and Fabric API in your `mods` folder.
