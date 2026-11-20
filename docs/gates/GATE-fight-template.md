# GATE — THE TEMPLATE-FILE ARENA SOURCE (PLAN-dragon-mobs.md C3, OWED SINCE F8b; THE SEAT'S QUEUE, ITEM 2)

**Status: NOT RUN, and no boot of its own.** Written 2026-11-19 on `feat/fight-template`, stacked on
`feat/fight-revive`, **for a later boot**, before any boot of this branch. A boot of this branch carries
`GATE-fight-revive.md`'s R0 with three changes:
- R0d reads **229 scenarios across 29 gates**, with `fight-template` last;
- R0b gains `ArenaTemplates.class` and `TemplateLayout.class`;
- R0c's fight line is unchanged on the shipped `source: generated`.

**Game mode:** SURVIVAL. **Nothing here can hurt him** (no dragon, no Mage; he stands at C; every strike deals 0).

## THE DESIGN QUESTION AND THE PROPOSAL (FLAGGED FOR BEN)

**How does the file say where the podium and the crystal spots are?** CC's proposal, built:

> **MARKER BLOCKS.** The builder places ONE `target` block standing on the floor at the arena's centre (the PODIUM),
> and ONE `lodestone` block where each crystal will stand. The file is placed so the podium's column lands on the
> fight world's (0, 0) and the floor under it on the content's `floor_y`, and every marker is removed. Crystal 1 is the
> marker due east of the podium, the rest anticlockwise, as the generated pillars are numbered.

**Why this one: it is the simplest thing a builder can do in game.** Two blocks from the creative menu, no command, no
text. It also fails loudly: the reader refuses a file with no podium, two podiums, or a crystal count that is not
`crystal_count`.

**The alternatives, not built:**
- **Structure blocks in DATA mode**, their metadata `podium` or `crystal`. This is vanilla's own marker, kept in the
  file as a block entity. But it is five clicks a marker and a typed word, and a typo is a silent missing crystal.
- **Named entities** (an armour stand called `crystal`), saved with entities. They are invisible in a schematic view
  and easy to knock over while building.
- **A side file of coordinates** beside the structure. A second file to keep in step: the two-homes problem.

**Open for Ben:** the two marker blocks (`target`, `lodestone`, SEAT PLACEHOLDERS) and this way of marking at all.

## WHAT WAS READ, EACH MARKED

- **Saving and loading** (READ, javap on the pinned `paper-api`): `Bukkit.getStructureManager()` has
  `loadStructure(File)`, `saveStructure(File, Structure)` and `createStructure()`. A `Structure` has `fill(Location,
  BlockVector, boolean)`, `place(Location, boolean, StructureRotation, Mirror, int, float, Random)`, `getSize()` and
  `getPalettes()`. A `Palette`'s `getBlocks()` gives each block's state with its position in the file.
- **Size limits** (READ, the server jar):
  - `CraftStructure.fill` refuses only a size below 1x1x1.
  - The 48-a-side limit is the in-game structure block's (`StructureBlockEntity.MAX_SIZE_PER_AXIS = 48`), not the
    template's.
  - **So the API saves and places the 113 x 16 x 113 arena, and a builder CANNOT save it with one structure block.
    `/rpg fight export` is his save as well as the tests' file.** Flagged for Ben.
- **What fill stores** (READ): every block but `structure_void`, AIR INCLUDED. So placing writes every position in the
  box, about 204,304 for the shipped arena (113 x 16 x 113, computed), against the generated build's 30,615.
- **Synchronous** (READ): `CraftStructure.place` builds `StructurePlaceSettings` and places on the calling thread;
  nothing in it is scheduled. **Its cost is unread:** `FIGHTWORLD template placed … ms=<t>` measures it. The generated
  build read 93-98 ms.
- **Where a palette's block positions start:** INFERRED, from the file's corner. That is what the round trip of TA1
  then TB1 reads.

## THE ROWS

### TA1 — export the generated arena as a template (SURVIVAL; this boot or any)

| prediction | stands on | READING |
|---|---|---|
| (1) The birth, as FD1's (`GATE-fight-down.md`). (2) **`FIGHTWORLD template exported name=ta_one file=<abs>\run\plugins\Rpg\arenas\ta_one.nbt size=113 16 113 corner=-56 62 -56 markers=11 ms=<t>`** (t READ), and the file on disk afterwards. The size: x and z twice the disc radius 56 plus 1; y from the floor's bottom (62) to one above the pillars' top (77). (3) **`ARENAPROBE when=command … centre_top=END_STONE bedrock_in_column=0 fire=0 (y 65..79) crystals=0 dragons=0`**: the markers were put back (centre_top is end stone, not `target`). (4) The end, the world deleted. **The file stays** in `arenas/` | `FightTemplateSourceTest` *exportingMarksTheFile…* | **AS PREDICTED, EXCEPT THE FILE PATH, WHICH THE LINE PRINTS RELATIVE (2026-11-20, the stack's boot at `5c2dee2c`, 11:01:25 to 11:01:29).** (1) The birth as FD1's: `end-config`, `created … environment=THE_END`, `end-check … inert=true`, `arena built … blocks=30615`, `FIGHT start … site=arena`, `entered … mode=SURVIVAL`. (2) **`FIGHTWORLD template exported name=ta_one file=plugins\Rpg\arenas\ta_one.nbt size=113 16 113 corner=-56 62 -56 markers=11 ms=278`**. The size, corner and markers are as predicted. **The `file=` is RELATIVE to the server's directory (`run`), not the predicted `<abs>\run\plugins\Rpg\arenas\ta_one.nbt`**, and it names the same file. On disk after the stop: `run/plugins/Rpg/arenas/ta_one.nbt`, 569,338 bytes, 11:01. (3) **`ARENAPROBE when=command … environment=THE_END centre_top=END_STONE bedrock_in_column=0 fire=0 (y 65..79) crystals=0 dragons=0`**: the markers were put back. (4) `left … why=leave`, `FIGHT nobody inside t=22`, `ended reason=nobody-inside-RUNNING`, `deleted …fight_77bdf54d entries=17`; **the file stays.** `DONE ticks=90` |

### TB1 — the template placed (a later boot whose `dragon.yml` says `source: template`, `template_file: ta_one`)

| prediction | stands on | READING |
|---|---|---|
| (1) R0c's fight line ends its arena clause with **`environment end, all down 5 s, template 'ta_one'; dragon`**. (2) At `rpg fight enter`: **`FIGHTWORLD template placed name=ta_one world=<n> size=113 16 113 origin=-56 62 -56 crystals=10 markers_removed=11 ms=<t>`**, and no `arena built` line. (3) `ARENAPROBE when=command … centre_top=END_STONE bedrock_in_column=0 fire=0 (y 65..79)`: the round trip lands the same arena | `ArenaLayoutTest` *aTemplateIsPlaced…*, *theGeneratedLayoutIs…* | |

### TB2 — the template's crystal spots are the generated arena's

| prediction | stands on | READING |
|---|---|---|
| A Mage at the centre and the slow close: **`CRYSTALSPOT index=1 at=40.50 77.00 0.50`**, as the generated arena's spot 1 (pillar 1 due east), then spots 2 to 10 anticlockwise | `ArenaLayoutTest` (the order) | |

### TB3 — the podium: C

| prediction | stands on | READING |
|---|---|---|
| `FIGHTWORLD entered … ` at C, `FIGHT start … centre=0.5 65.0 0.5 … site=arena`; with the dragon, `FIGHT dragon spawned … podium=-0.50 0.00 -0.50`, as the generated arena's | `ArenaLayoutTest` | |

### TB4 — a missing file falls back to the generated arena

| prediction | stands on | READING |
|---|---|---|
| With `template_file: no_such`: **`FIGHTWORLD template refused name=no_such why=no file <abs>\arenas\no_such.nbt -- the generated arena is built instead`**, then `FIGHTWORLD arena built … blocks=30615`, and the fight as on the generated arena | `FightTemplateSourceTest` *theSourceDecides…* | |

### TB5 — a malformed file (the wrong crystal count) falls back too

| prediction | stands on | READING |
|---|---|---|
| A template with one crystal marker removed: **`FIGHTWORLD template refused name=<n> why=9 crystal markers, and the fight has 10 crystals -- the generated arena is built instead`**, then `arena built` | `ArenaLayoutTest` *aTemplateWithTheWrongCrystalCount…* | |

### TB6 — a builder's own file (Ben's, when he has one)

| prediction | stands on | READING |
|---|---|---|
| His arena, exported with `/rpg fight export <name>` from a fight world he built in: placed, its markers removed, its spots read. **Unread until he builds one** | the above | |

## WHAT THIS GATE DOES NOT READ

- **The cost of placing 204,304 positions** until TB1's `ms=`.
- **Rotation or mirroring:** none; the file is placed as saved.
- **Entities in a template:** not saved (`fill(…, false)`) and not placed.
- **That the generated arena's merged rows do not move:** this is a code claim, not a boot reading. The site and the
  probe read the generated layout, whose numbers `ArenaLayoutTest` pins to the shape's. The shipped loaded line is
  unchanged (`FightLoaderTest`).
