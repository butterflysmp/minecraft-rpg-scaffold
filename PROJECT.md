# Project: Class-based action RPG (Minecraft server network)

## What this is

A Destiny-like action RPG delivered as a Minecraft server network:
a lobby, selectable game modes, and one flagship RPG mode with classes,
elements, a large persistent world, hundreds of weapons and abilities,
and boss encounters.

## Scale assumptions (drive the architecture)

- Hundreds of abilities and weapons. Content must be authored, not coded.
- Players spread across a large world — the workload Folia is built for.
- Eventually multiple servers behind a Velocity proxy with shared player state.

## Deliberately deferred

Not in scope until milestone 5. Do not build these early:

- Velocity proxy, lobby server, cross-server transfer
- Redis, Postgres, cross-server messaging
- Running on Folia (stay Folia-*compatible*, don't run it yet)

The single biggest risk to this project is building infrastructure for 500
concurrent players before proving the combat is fun for 5.

## Milestones

1. **Vertical slice.** One class, one element, three abilities, one weapon.
   Single server, file storage. *Is it fun?*
2. **Combat feel.** Damage numbers, cooldown UI, resource bar, hit feedback.
3. **One boss.** Three phases, telegraphed attacks, a loot table.
4. **Content pipeline proven.** Add a second class entirely through YAML,
   zero Java changes. If this hurts, fix it before adding content.
5. **Network.** Lobby + Velocity + shared database.

If milestone 1 is not fun, no amount of infrastructure rescues the project.

## Design notes

- Elements are an enum (a design decision). Abilities are data (content).
- The elemental effectiveness matrix lives in `Element.multiplierAgainst`,
  not in config — it must be unit-testable and typo-proof.
