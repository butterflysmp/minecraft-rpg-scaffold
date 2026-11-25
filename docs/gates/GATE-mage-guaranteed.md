# GATE — THE GUARANTEED MAGE ENCHANTS: THE PRESS TICK, THE CONTENT CHANGES, THE CAST TUNES, WILDFIRE

**Status: NOT RUN.** Every prediction below was written before any boot. **ONE R0 for the whole stack**: #273 and #274
(notes), #275 `feat/press-tick` (PR A), #276 `feat/mage-content-changes` (PR C), #277 `feat/guaranteed-cast-tunes` (PR D),
#278 `feat/wildfire` (PR E), and this gate (`feat/mage-guaranteed-gate`). **PR B (the Volley Stone's deletion) and PR F
(Mana Burn) are STOPPED** (`PLAN-enchant-columns.md` sections 21 and 25): no row reads either, and R0c keeps the
`volley_stone` carve-out. **No boot until Ben says "ready" in his own words and the seat approves the code and this gate
from origin.** The booter's checklist is `.claude/rules/booting.md`.

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

## UNREAD BY DESIGN

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
