# GATE — THE GUARANTEED MAGE ENCHANTS: THE PRESS TICK, THE CONTENT CHANGES, THE CAST TUNES, WILDFIRE

**Status: NOT RUN.** Every prediction below was written before any boot. **ONE R0 for the whole stack**: #273 and #274
(notes), #275 `feat/press-tick` (PR A), #276 `feat/mage-content-changes` (PR C), #277 `feat/guaranteed-cast-tunes` (PR D),
#278 `feat/wildfire` (PR E), and this gate (`feat/mage-guaranteed-gate`). **PR B (the Volley Stone's deletion) and PR F
(Mana Burn) are STOPPED** (`PLAN-enchant-columns.md` sections 21 and 25): no row reads either, and R0c keeps the
`volley_stone` carve-out. **No boot until Ben says "ready" in his own words and the seat approves the code and this gate
from origin.** The booter's checklist is `.claude/rules/booting.md`.

> **DATED NOTE, 2026-11-24, `feat/mage-guaranteed-gate-2` (the seat's rulings of the same day): EXTENDED, STILL NOT RUN.**
> PR B and PR F were RULED and are BUILT on this gate's branch: #280 `feat/volley-stone-deleted` (PR B), #281
> `feat/mana-burn-stamp` (F1), #282 `feat/mana-burn-damage` (F2), #283 `feat/mana-burn-content` (F3), and this extension
> on top. **The stack top is now `feat/mage-guaranteed-gate-2`, so the boot reads R1a to R1e (below R0), predicted
> against a jar built from it; R0a to R0e stay as written and are NOT read** (their jar is not the stack top). **MG2 is
> REPLACED by MG2R** (the seat's gate fix; MG2's steps are gone from the scenario). **New rows MB1 to MB5** read Mana
> Burn. PR B's rows are **R1b's jar sweep** (no `volley_stone` in the jar) and **R1c's WARN count** (none). Every
> sentence above this note that says PR B or PR F is STOPPED, or that R0c keeps the carve-out, is the record of the
> first draft. **One typed command** now (the run sheet). The whole run: see *HOW LONG THE RUN IS*.

`/rpg selftest mage-guaranteed`. **SURVIVAL, every row. ALL AUTO, no ASK:** every reading is a server line (CMD, REPLY,
PLAYERHIT, DOTTICK, VOLLEYSHOT, ORB, PROBE, SLOT, MENU); no row's prediction is something only a client can show.

## BEN'S WORDS THIS GATE SERVES

Verbatim, all of them, in `PLAN-enchant-columns.md` section 20 (*Ben's words of 2026-11-24*). Not restated here.

## WHAT WAS READ OR EXECUTED, EACH MARKED

- **READ (our source):** `EnchantMenu`'s click intents: a locked candidate's click is UNLOCK (buy I and make it active, one
  click); an unlocked inactive one's is ACTIVATE (no spend). The selftest's `click:` clicks the first top-inventory item
  whose name starts with its text (`SelfTestDrivers`, CLICK). `reload:` drives the shipped left-click handler (Lapis
  Crystal's granted throw, as `GATE-enchant-columns.md` LC1R2). `clear_state` clears his cooldowns, Tremors' streaks,
  (on this branch) Wildfire's chains, and removes his orb (cause `selftest`).
- **EXECUTED (jshell on `core/target/classes`, 2026-11-24):** `EnchantCost.xpPoints(1, 0)` = **352** (an unlock at
  bookshelf power 0: the pad has no shelf); `XpCurve.totalPoints(30, 0f)` = **1395**; Wildfire III's factors on 45:
  **45.0, 51.74999999999999, 58.5, 65.25, 72.0, 72.0**.
- **READ (section 19 of the plan, and PR A):** the selftest's `use` runs in his entity phase on tick T; `WeaponFire` stamps
  T before its hop; the cast runs at T + 1; a volley's first shot, `scheduleOn(windup)` filed from the global phase, runs at
  **T + windup**. So `VOLLEYSHOT … shot=0 pressed=T tick=T+windup`: **both reads are server ticks (`Bukkit.getCurrentTick`):
  `pressed` on the press's tick, in the entity phase; `tick` on the shot's tick, in the entity phase.**
- **READ (`Ignite`):** an Ignite never damages a player (`c.state().player()` is skipped), at any distance. No mob in
  this gate dies, so no Ignite fires (each row's health sums below).
- **UNVERIFIED:** that `rpg playerxp set @s 30 levels` puts 1395 points on the vanilla bar the table's wallet reads
  (`EnchantMenu` reads `XpCurve.totalPoints(getLevel(), getExp())`). MG2 reads the wallet before and after with
  `probe_self`, so the row is read either way; its 352 is the price, not the wallet.

  > **DATED NOTE, 2026-11-24 (the seat's gate fix): READ, AND IT DOES NOT; AND THE SENTENCE ABOVE WAS WRONG TWICE.**
  > `rpg playerxp` (`RpgCommand.playerXp`) writes the RPG profile's lifetime XP (`ProfileService.setLifetimeXp`) and
  > nothing else; nothing in `paper/src/main/java` mirrors it onto the vanilla bar (`setLevel`/`setExp` are written only by
  > the anvil, the table, the grindstone and the selftest snapshot, READ by `git grep`). **The table charges the vanilla
  > bar** (`EnchantMenu`: `XpCurve.totalPoints(viewer.getLevel(), viewer.getExp())`, written back by `setLevel`/`setExp`).
  > So in MG2 `rpg playerxp set @s 30 levels` did nothing for the wallet: it set his RPG level to 30, which MG2 reads
  > nowhere. **And `probe_self` prints no wallet at all** (`SELF customHP=… at=… onGround=… fall=…`, READ
  > `SelfTestDrivers.probeSelf`), so "MG2 reads the wallet … with `probe_self`" was false. **MG2R replaces it.**

## THE DAMAGE SUMS (`verification.md`, *A DAMAGE PREDICTION NAMES EVERY TERM*)

`sent = (((amount + TUNE) x score / 100) x enchantMultiplier + classDamageBonus + weaponLevelDamage) x chargeScale x
critMultiplier x WILDFIRE`. Staged in every damage row: **score 100** (`rpg gearscore set 100`); **enchantMultiplier
1.0** (no damage enchant: `rpg enchant clear`, then only the row's own weapon enchant); **classDamageBonus 0** (his
storage emptied, nothing worn grants class damage); **weaponLevelDamage 0** (`rpg playerxp set @s 1 levels`, level 1, then
40 ticks for the stats to reconcile, as `GATE-level.md` WD1); **chargeScale 1.0** (a trigger); **critMultiplier 1.0**,
**x 2.0 the second allowed reading** of any hit; **TUNE** and **WILDFIRE** as the row says, else 0 and 1.0.

## SAFETY: THE MOST THE RUN CAN DEAL HIM

Every mob is a frozen NoAI zombie: it attacks nothing. His own shots strike mobs only (the ray, projectile and volley paths
skip the caster; a Knell beam pierces mobs, never him). Ignite skips every player (READ). Scorch burns only what his hits
scorched. **Sum on him: 0.** He stands on the pad's stand point for every row; MG8 and MG10 turn him in place (`tp` with a
facing, no move).

> **DATED NOTE, 2026-11-24 (the extension): THE MANA BURN ROWS, AND THE SUM ON HIM IS STILL 0.** Every mob that can
> die scorched there (`m` in MB2 and MB3, which are predicted to live; `v` in MB4a, MB4b and MB5, killed by command; `b`,
> which an Ignite scorches; and every mob killed by a row's closing `kill`) stands **6 east of him or further** (`b` at 6
> east, 2 south). **Ignite cannot hurt a player at any distance: READ, `Ignite.detonate`, whose loop over
> `world.combatantsNear(at, RADIUS)` does `if (c.state().player()) continue;` before `applyDamage`**, a check F2 leaves
> untouched (pinned, `ManaBurnIgniteTest.aStampedBlastStillSparesAPlayer`). And independently of that skip, he is outside
> every blast: `combatantsNear` is a box of half-width 4 round the corpse (READ, plan section 25), and he is 6 out on the x
> axis. A Flint Staff bolt strikes mobs only; scorch burns only what his hits scorched. **Sum on him: 0.** Each MB row ends
> by killing its mobs and waiting 30 ticks, so an Ignite from a scorched mob killed there (20 ticks after the kill) lands
> before the next row summons anything.

## CARRIED STATE (CC, IN THIS ORDER) — A CHECKLIST, EACH LINE TICKED IN THE REPORT, NOT REMEMBERED

1. No `java` process (`Get-Process java`), as every boot.
2. The world backed up (`run\world`, `run\world_nether`, `run\world_the_end`) with its file counts, as the last boot did.
3. `run\plugins\Rpg\config.yml` carries `dev-pack-on-join: true` as before; no `rpgfight` folder.
4. **The world's saved difficulty is read and written down** (memory: it wins over `server.properties`); the setup sets
   normal and the restore puts it back.
5. The tree clean and pushed: `git status --porcelain` empty; `git rev-parse HEAD` equals `git ls-remote --heads origin
   feat/mage-guaranteed-gate`.

## THE RUN SHEET — BEN TYPES THESE THREE LINES

```
/rpg mobtrace on
/rpg selftest mage-guaranteed
/stop
```

He logs in only after he is told R0 PASSED; he stands anywhere (the setup moves him to the pad); he types the first two,
waits for `SELFTEST mage-guaranteed END`, then `/stop`.

> **DATED NOTE, 2026-11-24 (the extension; the seat: "keep it to one typed command"): ONE TYPED COMMAND.** `/rpg
> mobtrace on` is dropped: the scenario's own `trace: on` (setup) flips the same switch (READ, `SelfTestDrivers.trace`:
> `if (!run.nameplates().tracing()) run.nameplates().toggleTrace()`, the switch PLAYERHIT and DOTTICK are logged under).
> He is told **R1** passed (not R0), then types
>
> ```
> /rpg selftest mage-guaranteed
> ```
>
> waits for `SELFTEST mage-guaranteed END`, and types `/stop` (the after-run step, not part of the run).

## HOW LONG THE RUN IS

**The waits alone sum to 2713 ticks, 135.65 seconds** (`awk '/^  - wait: /{s+=$3} END {print s}'` over the scenario, at
this branch). Every other step costs at least one tick and there are 452 of them (582 steps less 130 waits: `grep -c` of `^  - ` and of `^  - wait: ` over the scenario), and
twelve of them tally 500 rolls each; **so about 2.5 to 3 minutes in all (an ESTIMATE: the per-step cost is not
measured).** The run's own `SELFTEST mage-guaranteed START` and `END` lines give the real figure.

## R0 — THE BUILD. READ IT FIRST. IF ANY R0 FAILS, STOP

**#273 and #274 are plan notes only: nothing of them is in the jar.** R0 reads PR A, C, D, E and this gate.

### R0a — the build line names the stack top's head, clean

| prediction | instrument | READING |
|---|---|---|
| `[Rpg] Build: <sha>`, `<sha>` being `feat/mage-guaranteed-gate`'s head as `git ls-remote --heads origin feat/mage-guaranteed-gate` prints it, shortened. **Not `-dirty`, not `unknown`.** | `Select-String -Path run\logs\latest.log -Pattern '\[Rpg\] Build:'` on THIS boot's log | |

### R0b — the jar carries the four PRs and this gate

The scan is `docs/gates/GATE-mage-guaranteed-r0b.ps1`, committed beside this file (its lines are the prediction's
subject). **The prediction, line for line:** before `'--- controls'`, **28 `PRESENT`, 0 `ABSENT`**; after it, **6
`ABSENT`, 0 `PRESENT`**. Counted from the script: `Select-String -Path <script> -Pattern '^(ValueLine|Has) ' | Measure`
gives 34, and 6 follow the controls line. Run before the commit: see *R0b, RUN BEFORE COMMIT* at the end.

### R0c — every Loaded line, and nothing refused

`[Rpg] Loaded … 22 enchants …` (**22: the five cast tunes and `wildfire` added** to the 16 of `c1ebabab`; every other
count as on `c1ebabab`). **No `[Rpg]` WARN beyond `volley_stone`'s known pair** (PR B is STOPPED: the pair stays), in
particular none naming any of the six new enchants, `cursed_emerald` or `flint_staff` (the emerald's authored 30 is above
its new floor of 24: no `Content:` warning). No `Refusing`, `Skipping`, `SEVERE`, `Exception`, `Dev-only`, `currently
prevented`.

### R0d — every scenario loaded

Exactly one **`[Rpg] Selftest: dev server, 390 scenarios across 47 gates [melee-cell, …, focused-beam, mage-guaranteed]`**
(recount: 47 files, 437 keys, 47 setups; `SelfTestScenariosTest`). The controls: `375 scenarios` and `391 scenarios`
ABSENT.

### R0e — the orphan sweep with nothing staged

One `FIGHTWORLD sweep path=…\rpgfight folders=0`, no `deleting`, no `orphan`.

## R1 — THE BUILD OF THE EXTENDED STACK (2026-11-24). THE BOOT READS THESE, NOT R0. IF ANY R1 FAILS, STOP

**The stack top is `feat/mage-guaranteed-gate-2`.** The jar carries PR A, C, D, E, the first gate commit, PR B (#280), F1
(#281), F2 (#282), F3 (#283) and this extension. The carried state's step 5 reads `feat/mage-guaranteed-gate-2`.

### R1a — the build line names the new stack top's head, clean

| prediction | instrument | READING |
|---|---|---|
| `[Rpg] Build: <sha>`, `<sha>` being `feat/mage-guaranteed-gate-2`'s head as `git ls-remote --heads origin feat/mage-guaranteed-gate-2` prints it, shortened. **Not `-dirty`, not `unknown`.** | `Select-String -Path run\logs\latest.log -Pattern '\[Rpg\] Build:'` on THIS boot's log | |

### R1b — the jar carries B, F1 to F3 and the extension, AND NO ENTRY NAMES `volley_stone` (PR B's jar row)

The scan is `docs/gates/GATE-mage-guaranteed-r1b.ps1`, committed beside this file. **The prediction, line for line:**
before `'--- absent'`, **22 `PRESENT`, 0 `ABSENT`**; between `'--- absent'` and `'--- controls'`, **7 `ABSENT`, 0
`PRESENT`**; after `'--- controls'`, **5 `ABSENT`, 0 `PRESENT`**. Counted from the script: `Select-String -Path <script>
-Pattern '^(ValueLine|Has|Entry|Sweep) '` gives 34 (22 + 7 + 5).

- **PR B's row, "no `volley_stone` in the jar":** `Sweep 'volley_stone'` reads EVERY entry of the jar (each class's
  constant pool and each resource) and prints `ABSENT sweep 'volley_stone' in 0 of <n> entries`; the same for
  `volley_beam` and `volley_cast`; and the three files' entries are absent. **Its positive control is `Sweep 'boltor'`,
  in the first section: the same function over the same jar must print `PRESENT … in <k> entries`**, so a sweep that reads
  nothing cannot pass as absence. (Comments do not reach a class file; the shipped YAML comments that named the stone
  were reworded on #280.)
- The F2 rows read `Ignite.detonate`'s new descriptor, `(…UUID;UUID;ID)V` (an int depth, then the double stamp), in
  `Ignite.class` and at its call site in `RpgListeners.class`; the control is the swapped `DI`, absent.

### R1c — every Loaded line, and NO `[Rpg]` WARN AT ALL (PR B's WARN row)

- `[Rpg] Loaded 11 abilities, 44 visuals, 5 statuses, 7 elements, 23 enchants, …, 23 weapons, …`: against `c1ebabab`,
  **visuals 46 → 44** (the stone's two moved to test fixtures), **enchants 16 → 23** (the five cast tunes, `wildfire`,
  `mana_burn`), **weapons 24 → 23** (the stone); every other count as on `c1ebabab`. Recounted by `ls` over
  `paper/src/main/resources/content/{visuals,enchants,weapons}` at this branch: 44, 23, 23.
- `[Rpg] Loaded loot 'cascade': default exotic 1.00% (truly 1.000%), legendary 3.00% (truly 2.970%), epic 8.00% (truly
  7.682%), rare 12.00% (truly 10.602%), uncommon 20.00% (truly 15.549%), common 40.00% (truly 24.879%), nothing truly
  37.318%; 1 sources [dev_example_not_a_drop]`, **word for word as before PR B** (`LootLoaderTest` asserts it verbatim,
  green on `b68ad67b` and on #280: the line prints the tiers and source names, not the pools).
- **THE WARN COUNT: 0.** `(Select-String -Path run\logs\latest.log -Pattern 'WARN\]: \[Rpg\]').Count` reads **0**, up to
  the first `joined the game` and in the whole log. **Its positive control:** `(Select-String -Path run\logs\latest.log
  -Pattern 'INFO\]: \[Rpg\] Loaded').Count` reads 1 or more on the same log, so the pattern's shape matches this log's
  lines. **Why 0 and not "beyond the known pair":** the pair was the stone's (`verification.md`'s R0c clause carries its
  dated note on #280); the last boot (`GATE-focused-beam.md` R0c, AS PREDICTED) printed only that pair; and the shipped
  weapons now validate with no warning (`VolleyFixtureTest.theShippedWeaponsValidateWithNoWarning…`). **A warning from
  another loader would be new, and R1c stops the boot on it.**
- No `Refusing`, `Skipping`, `SEVERE`, `Exception`, `Dev-only`, `currently prevented`.

### R1d — every scenario loaded

Exactly one **`[Rpg] Selftest: dev server, 396 scenarios across 47 gates [melee-cell, …, focused-beam, mage-guaranteed]`**
(recount: 47 files, 443 keys, 47 setups; `SelfTestScenariosTest`, whose control is 437 - 1 + 7 = 443). The controls: `390
scenarios` and `397 scenarios` ABSENT.

### R1e — the orphan sweep with nothing staged

As R0e: one `FIGHTWORLD sweep path=…\rpgfight folders=0`, no `deleting`, no `orphan`.

## THE ROWS

Each row's lines are those between `SELFTEST mage-guaranteed <row> START` and its `DONE`. Every row starts from cleared
cooldowns (`clear_state`); no row presses an ability another row pressed within its cooldown.

### MG1 — THE POOLS: A FRESH ROLL OF EACH WEAPON, AND 500 ROLLS

| weapon | `rpg enchant show`, slot 0, in this order, every candidate level 0, none active | `rollstats 500`, slot 0 (weapon) |
|---|---|---|
| flint_staff | `large_sparks` | 1 candidate in all 500 |
| crystal_shard_staff | `sharp_crystals` | 1 in all 500 |
| lapis_staff | `polished_scepter`, `lapis_crystal` (**guaranteed FIRST**) | 2 in all 500 |
| living_lantern | `wildfire`, `airburst_flames` (**FIRST**) | 2 in all 500 |
| cursed_emerald | `echo_beam`, `focused_beam` (**FIRST**) | 2 in all 500 |
| knell_beam | `early_toll` | 1 in all 500 |
| blaze_kings_staff | **empty** (no guaranteed one: Ben, "leave as is") | 0 in all 500 |
| breacher | `shatter_caps`, **unchanged** | 1 in all 500 |

**Why every count is exact, not a distribution:** each weapon's slot-0 REST is 0 or 1 enchant, and the cascade's last
tier always lands 1 of a pool of 1 (`CandidateCascade.count`, READ). The guaranteed one is in every roll. Slots 1 to 3 are
not predicted here (unchanged code).

### MG2 — THE FIX, THROUGH THE TABLE (the Lapis Staff)

| step | prediction |
|---|---|
| staged | `rpg enchant show`: slot 0 `polished_scepter` (0), `lapis_crystal` (I, ACTIVE) |
| `reload` | one `ORB rest owner=<him> …` line (the crystal thrown); `probe_mana` not read here |
| the table, `click: "Polished Scepter"` | a REPLY naming Polished Scepter now active at I and a spend of **352** XP; the MENU dump after it shows slot 0's active line on Polished Scepter; `probe_self`'s wallet **352 points** below its reading before the table |
| `reload` | **no `ORB rest` line** in the row between this `reload` and the next `clear_state`: the granted left click is gone (Lapis Crystal inactive) |
| the table, `click: "Lapis Crystal"` | a REPLY `Lapis Crystal is now active.`; **no spend**: `probe_self`'s wallet equal to its reading after the first click |
| `reload` | one `ORB rest` line again |

> **DATED NOTE, 2026-11-24 (the seat's gate fix): MG2 IS REPLACED BY MG2R AND ITS STEPS ARE GONE FROM THE SCENARIO.** It
> leaned on his own wallet: `rpg playerxp set @s 30 levels` writes his RPG lifetime XP, not the vanilla bar the table
> charges (READ, *WHAT WAS READ*), and its `probe_self` reads printed no wallet. Not read; kept as the record.

### MG2R — THE FIX, THROUGH THE TABLE, ON A STAGED WALLET (the Lapis Staff)

**READ:** the table's wallet is the VANILLA bar (`EnchantMenu`: `XpCurve.totalPoints(viewer.getLevel(), viewer.getExp())`,
and the spend written back by `setLevel` / `setExp`). **Staged by the vanilla command that sets exactly that:** `xp set @s 30
levels` (`setExperienceLevels`) then `xp set @s 0 points` (`setExperiencePoints`; READ by javap, `ExperienceCommand$Type`).
**Read by `xp query`** (READ by javap: `levels` is `experienceLevel`; `points` is `Mth.floor(experienceProgress x
getXpNeededForNextLevel())`, the points INTO the level), whose REPLY is the vanilla `commands.experience.query.*` text, as
`GATE-dragon-shot.md` SH9 read it (`… experience points` / `… experience levels`). **The run's snapshot restores his bar**
(READ, `SelfTestSnapshot`: `setLevel(vanillaLevel)`, `setExp(vanillaProgress)`, captured at the run's start). **No `rpg
playerxp` in the row: it did nothing there.** EXECUTED (jshell on `core/target/classes`, 2026-11-24):
`XpCurve.totalPoints(30, 0f)` = **1395**; 1395 - 352 = **1043**; `levelFor(1043)` = **26**, `progressFor(1043)` = **0.5**,
of 92 (`5 x 26 - 38`) to the next, so `xp query … points` reads floor(0.5 x 92) = **46**; `totalPoints(26, 0.5f)` = 1043
back.

| step | prediction |
|---|---|
| staged | `rpg enchant show`: slot 0 `polished_scepter` (0), `lapis_crystal` (I, ACTIVE) |
| the wallet, before | REPLY **`… has 30 experience levels`** and **`… has 0 experience points`**: **1395 points** (`totalPoints(30, 0)`) |
| `reload` | one `ORB rest owner=<him> …` line (the crystal thrown) |
| the table, `click: "Polished Scepter"` | a REPLY naming Polished Scepter now active at I, and a spend of **352** XP; the MENU dump after it shows slot 0's active line on Polished Scepter |
| the wallet, after the unlock | REPLY **`… has 26 experience levels`** and **`… has 46 experience points`**: **1043 points** = 1395 - 352 |
| `reload` | **no `ORB rest` line** in the row between this `reload` and the next `clear_state`: the granted left click is gone |
| the table, `click: "Lapis Crystal"` | a REPLY `Lapis Crystal is now active.`; no spend |
| the wallet, after the activate | REPLY **`… has 26 experience levels`** and **`… has 46 experience points`**: unchanged, 1043 |
| `reload` | one `ORB rest` line again |

### MG3 — LARGE SPARKS III, ONE FLINT STAFF BOLT

Zombie `m` 6 east, 1000 health. One `PLAYERHIT … source=flint_staff/right_click … sent=32.000`: **((20 + 12) x 100/100) x
1.0 + 0 + 0, x 1.0 x 1.0** (64.000 the second allowed reading, a crit). `probe m` reads 968 (936 a crit), less the scorch
ticks it has taken by then (DOTTICK lines, each `cap=16.000` and `sent=16.000`: half the TUNED 32, below 5% of 1000).

### MG4 — SHARP CRYSTALS II, ONE CRYSTAL SHARD

Zombie `m` 10 east, 1000 health. One `PLAYERHIT … sent=22.000`: **((16 + 6) x 100/100) x 1.0 + 0 + 0, x 1.0 x 1.0** (44.000
a crit). Kinetic: no DOTTICK. `probe m` 978 (956).

### MG5 — POLISHED SCEPTER III

Zombie `m` **36 east**. One `PLAYERHIT … source=lapis_staff/right_click … sent=21.000`: **(21 x 100/100) x 1.0 + 0 +
0, x 1.0 x 1.0** (the range 26 + 12 = 38 reaches it); `probe m` 979 (958).

### MG5C — THE CONTROL, NO ENCHANT

The same zombie: **no PLAYERHIT** on `m`,
`probe m` 1000/1000 (26 does not reach 36).

### MG6 — THE PLAIN CURSED EMERALD: THREE BOLTS (Ben's 3)

Zombie `m` 10 east. **Exactly three** `PLAYERHIT … source=cursed_emerald/right_click … sent=27.000`, each **(27 x 100/100) x
1.0 + 0 + 0, x 1.0 x 1.0** (any may read 54.000, a crit); `probe m` 1000 - 81 = 919 with no crit.

### MG7a — ECHO BEAM I

The same staging. **Exactly four** `PLAYERHIT … sent=27.000` (each its own crit reading); `probe m` 892 with no crit.

### MG7b — ECHO BEAM II

**Exactly five**; `probe m` 865 with no crit.

### MG7c — ECHO BEAM III

**Exactly six**; `probe m` 838 with no crit.

### MG8 — THE OWED AIM ROW: TURNED DURING THE WIND-UP

Pressed facing east (E 10 east); 10 ticks later `tp @s ~ ~ ~ 0 0` turns him south (S 10 south); the plain volley re-reads
his aim at each shot (its first at the wind-up's end). **Three `PLAYERHIT` on `s`, `sent=27.000` each; none on `e`**;
`probe e` 1000/1000. (The owed row named Focused Beam's one ray; the plain volley's first shot reads the same aim at the
same tick: `CastExecutor.volley`, READ.)

### MG9a — EARLY TOLL III (the clock PR A fixes)

Zombie `m` 10 east. One `VOLLEYSHOT caster=<him> source=knell_beam/right_click shot=0 pressed=<T> tick=<t>` each:
**`t - T` = 16 (MG9a, Early Toll III: 24 - 8) and 24 (MG9b, none).** `T` is read on the press's tick in his entity phase
(`WeaponFire`, before the hop); `t` on the shot's tick in his entity phase. One `PLAYERHIT … sent=63.000` each: **(63 x
100/100) x 1.0 + 0 + 0, x 1.0 x 1.0** (126.000 a crit).

### MG9b — NO ENCHANT, THE SAME PRESS

The same staging with no enchant: `t - T` = **24**; the same `PLAYERHIT … sent=63.000`.

### MG10 — WILDFIRE III: SIX HITS ON ONE MOB, A SECOND MOB BETWEEN, A GAP

`m` 10 east and `n` 9 south, each 2000 health (vanilla max 400), each beyond the shot's 7-block seek from the muzzle.
Presses 16 ticks apart (the Lantern's cooldown): two east, a turn south and one, a turn east and four, then 80 ticks and
one more east. The shot travels 0.35 a tick, so each lands about 29 ticks (m) or 26 (n) after its press; **each of m's
hits lands within 60 of the one before, until the gap.**

| hit on `m` | `sent=` | the sum |
|---|---|---|
| 1 | 45.000 | (45 x 100/100) x 1.0 + 0 + 0, x 1.0 x 1.0 x **1.00** |
| 2 | 51.750 | … x **1.15** (51.74999999999999, EXECUTED) |
| 3 | 58.500 | … x **1.30** |
| 4 | 65.250 | … x **1.45** |
| 5 | 72.000 | … x **1.60** |
| 6 | 72.000 | … x **1.60** (the cap) |
| 7, after the gap | 45.000 | … x **1.00**, a new chain |

**`n`: one `PLAYERHIT … sent=45.000`** (its own chain's first hit), and m's third hit, landing after it, still reads x1.30.
Any hit may read x 2.0 more (a crit). **Scorch (DOTTICK, `cap=` the larger of half of each hit so far):** on `m` the caps
read 22.500, 25.875, 29.250, 32.625, 36.000 as the hits climb, and each tick `sent=` equals its `cap` (5% of 2000 is 100,
above every cap); **no DOTTICK reads a multiplied amount beyond its cap**, and no scorch tick is a PLAYERHIT. `probe m`
and `probe n` alive.

### MG11 — THE TOOLTIPS, EVERY LINE (`dump: held`)

The Flint Staff's lines as `golden-lore.txt`'s `flint_staff` block on this branch, the flavor reading **`Prehistoric
magicians learned you could`** / **`create fire by flinging mana into flint.`**; the Cursed Emerald's as its block, with
**`Channel, then loose three`** and **`Kinetic Damage: 27 x 3`**. No enchant line (each fresh, enchant state cleared).

## THE MANA BURN ROWS (the extension, 2026-11-24)

**The sums, every term** (`verification.md`, *A DAMAGE PREDICTION NAMES EVERY TERM*). A Flint Staff bolt:
`sent = ((20 + TUNE) x score/100) x enchantMultiplier + classDamageBonus + weaponLevelDamage) x chargeScale x
critMultiplier x WILDFIRE` = **((20 + 0) x 100/100) x 1.0 + 0 + 0, x 1.0 x 1.0 x 1.0 = 20.000** (40.000 the second allowed
reading, a crit). TUNE 0: slot 0 holds `large_sparks` at **level 0** where staged (the lint requires slot 0 before slot
1; a level-0 candidate acts on nothing, B2); WILDFIRE 1.0 (not the Lantern). **A scorch tick:** `min(5% x max, cap) x (1 +
STAMP/100)`, the cap `0.5 x` the hit without its crit = **10** (a crit does not move it), STAMP **30** at Mana Burn III and
**0** without; the cap itself is NOT multiplied (the seat's ruling). **An Ignite:** `6 x (1 + STAMP/100)` on every mob in
its box but the corpse; its own scorch on a bystander has cap `0.5 x` that and NO stamp. EXECUTED (jshell, 2026-11-24):
`5.0 x 1.3` = **6.5**; `10.0 x 1.3` = **13.0**; `6.0 x 1.3` = **7.800000000000001** (`%.3f` 7.800); its half
**3.9000000000000004** (3.900); `1000 - 7.800000000000001` = **992.2** (992.200). **Health readings** (`PROBE … customHP=`)
assume no defence on a frozen zombie, as MG3 does (INFERRED, unread); **the DOTTICK and PLAYERHIT lines are the rows'
readings, and they are written before any defence.** A default zombie (vanilla max 20) is **100** health and one at vanilla
max 200 is **1000**: the ratio `GATE-breacher-rework.md` LL9R2 read (its S, a default zombie, "100 health", READ there).

### MB1 — THE POOLS: SLOT 1 OF EACH FIRE WEAPON, AND 500 ROLLS

| weapon | `rpg enchant show`, slot 1 | `rollstats 500`, slot 1 |
|---|---|---|
| flint_staff | `mana_burn`, level 0, not active | 1 candidate in all 500 |
| living_lantern | `mana_burn` | 1 in all 500 |
| blaze_kings_staff | `mana_burn` | 1 in all 500 |
| dragons_breath | **empty** (fire, but RANGER: the class filter's control) | 0 in all 500 |

**Why exact:** each fire mage weapon's unique pool is `[mana_burn]` alone (`ManaBurnContentTest`,
`UniqueSlotContentTest`), and the cascade's last tier always lands 1 of a pool of 1 (`CandidateCascade.count`, READ). Slot
0 as MG1 reads it (the guaranteed `large_sparks`; the Lantern's `wildfire` and `airburst_flames`; Blaze King's Staff
empty).

### MB2 — A SCORCH TICK WITHOUT THE ENCHANT, ON A 100-HEALTH MOB

`m`, a default zombie, 6 east. `probe m` before: **`customHP=100.000/100.000`**. One `PLAYERHIT … source=flint_staff/right_click
… sent=20.000` (40.000 a crit). Then **six `DOTTICK … status=scorch applier=<him> sent=5.000 cap=10.000`** on `m`: each
`min(5% x 100, 10) x (1 + 0/100) = min(5, 10) x 1.0 = 5.000`. `probe m` after: **50.000/100.000** = 100 - 20 - 6 x 5 (30.000
a crit). `m` lives (no Ignite).

### MB3 — THE SAME TICK WITH MANA BURN III

The same staging, slot 1 `mana_burn` at III, active. `probe m` before 100.000/100.000. One `PLAYERHIT … sent=20.000` (the
stamp does not touch the hit). Then **six `DOTTICK … sent=6.500 cap=10.000`**: `min(5, 10) x (1 + 30/100) = 5 x 1.3 =
6.500`; **the cap reads 10.000, unchanged.** `probe m` after: **41.000/100.000** = 100 - 20 - 6 x 6.5 (21.000 a crit). `m`
lives.

### MB4a — ONE IGNITE FROM A MOB THAT DIES SCORCHED WITH THE STAMP

`v` (1000 health) 6 east, `b` (1000 health) **6 east and 2 south: 2 blocks from `v`, inside the Ignite's box (half-width 4)**,
and off the bolt's line. Mana Burn III. `probe b` before **1000.000/1000.000**. One `PLAYERHIT` on `v`, `sent=20.000`;
every `DOTTICK` on `v` before the kill reads **`sent=13.000 cap=10.000`** (5% of 1000 is 50, so the cap of 10 binds, and
the stamp multiplies the SETTLED tick: `min(50, 10) x 1.3 = 13.000`). `probe v` just before the kill: alive, `fireTicks` above 0. `v` is killed by command 50 ticks after the press
(the death listener's domain: it dies SCORCHED). 25 ticks after the kill, **`probe b` reads `992.200/1000.000`** = 1000 -
6 x (1 + 30/100) = 1000 - 7.8. **Then `b`'s own burn, from the blast: every `DOTTICK` on `b` reads `sent=3.900 cap=3.900`**
(cap `0.5 x 7.8`; its stamp 0, so `min(50, 3.9) x 1.0`). No PLAYERHIT for the blast (an Ignite is not an ability hit).

### MB4b — THE SAME IGNITE WITHOUT THE STAMP

No enchant. One `PLAYERHIT` on `v`, `sent=20.000`; every `DOTTICK` on `v` reads **`sent=10.000 cap=10.000`**. **`probe b`
after: `994.000/1000.000`** = 1000 - 6 x 1.0. `b`'s burn: **`sent=3.000 cap=3.000`**. **Against MB4a: 6 against 7.8**, and
3.000 against 3.900.

### MB5 — AN UNENCHANTED HIT CLEARS THE STAMP

As MB4a's staging, Mana Burn III. First `PLAYERHIT` on `v`, `sent=20.000`; its `DOTTICK` lines **`sent=13.000
cap=10.000`**. Then `rpg enchant clear` (the `rpg enchant show` after it shows no active enchant), `clear_state` (his
cooldown), and a second, plain bolt: a second `PLAYERHIT` on `v`, `sent=20.000`. **Every `DOTTICK` on `v` after that second
PLAYERHIT reads `sent=10.000 cap=10.000`**: the plain hit refreshed the scorch and the stamp followed the newest applier to
0 (the seat's ruling; `ScorchStampStatusTest.anUnenchantedRefreshClearsTheStamp…`); the cap stays the larger, 10 = 10.
`v` killed; **`probe b` 25 ticks later reads `994.000/1000.000`**: the Ignite is plain, 6.

## UNREAD BY DESIGN

> **DATED NOTE, 2026-11-24 (the extension):** PR B and PR F are BUILT and read here (R1b, R1c, MB1 to MB5). The first
> bullet below is the record of the first draft.

- **PR B and PR F:** STOPPED, no row. **No "no `volley_stone` anywhere" row**; R0c keeps its carve-out.
- **The weapon tooltips with a tune active** are NOT changed (OPEN FOR BEN, plan section 23): no row reads them.
- **A real client's right click** (plan section 19's NOT READ): every press here is the selftest's.
- **The chain's boundary tick on a server** (exactly 60 starts again): `HitChainsTest` holds it; MG10's gap is 80.

## R0b, RUN BEFORE COMMIT

Run 2026-11-24, 15:45, against `paper\target\rpg-0.1.0-SNAPSHOT.jar` built by `./mvnw clean package` (exit 0; core 2485
(1 skipped), storage 99, paper 1949, 0 failures) from the working tree of `feat/mage-guaranteed-gate` on `feat/wildfire`
`3210e538`, this file and the scenario uncommitted (so the jar is no commit: the boot's R0 re-reads it on the deployed one):
`powershell -File docs\gates\GATE-mage-guaranteed-r0b.ps1 -JarPath paper\target\rpg-0.1.0-SNAPSHOT.jar` printed **28
`PRESENT`, 0 `ABSENT`** before `--- controls` and **6 `ABSENT`, 0 `PRESENT`** after it. **As predicted.**

The lint's positive control, before the commit: MG5's probe reach set to 16 for a mob 36 out printed `LINT
mage-guaranteed#MG5 PROBE_REACH probe m: 36 out, reach 16 (one block of margin)` and reddened the clean-lint row; restored,
`cmp` identical. (A first run printed nothing: the gate was not yet in `selftest/index.yml`, so nothing loaded it. Found
by the gate-list pin staying green, and fixed before the control was re-run.)

## R1b, RUN BEFORE COMMIT (the extension, 2026-11-24)

Run 2026-11-24, 17:30, against `paper\target\rpg-0.1.0-SNAPSHOT.jar` built by `./mvnw clean package` (BUILD SUCCESS; core
2496 (1 skipped), storage 99, paper 1968, 0 failures) from the working tree of `feat/mage-guaranteed-gate-2` on
`feat/mana-burn-content` `87bd9019`, this file, the scenario, `SelfTestScenariosTest` and the R1b script uncommitted (so
the jar is no commit: the boot's R1 re-reads the deployed one): `powershell -File docs\gates\GATE-mage-guaranteed-r1b.ps1
-JarPath paper\target\rpg-0.1.0-SNAPSHOT.jar` printed **22 `PRESENT`, 0 `ABSENT`** before `--- absent`; **7 `ABSENT`, 0
`PRESENT`** between it and `--- controls`; **5 `ABSENT`, 0 `PRESENT`** after. **As predicted.** The sweep read **1432**
entries; `volley_stone`, `volley_beam` and `volley_cast` were each in **0**; its positive control, `boltor`, in **20**.

The lint's positive control, before the commit: MB4a's first `probe: b` set to `"b 6"` (a reach of 6 for a mob 6 out)
printed `LINT mage-guaranteed#MB4a PROBE_REACH probe b: 6 out, reach 6 (one block of margin)` and reddened the clean-lint
row; restored, `cmp` identical. **The lint also caught a real defect in the first draft of MB3, MB4a and MB5** (`ENCHANT_ORDER`:
a slot-1 candidate after `rpg enchant clear`, when only slot 0 can be added); fixed by staging `large_sparks` at level 0 in
slot 0 first, as the other slot-1 rows of this repo do.
