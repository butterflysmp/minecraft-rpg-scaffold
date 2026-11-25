# GATE — THE SUMMONER: THE BONE WHISTLE AND ITS WOLF

**Branch `feat/summoner-gate`, on #298 `feat/summoner-5` (the stack #289 → #298).** NOT BOOTED. Written 2026-11-25,
predictions committed before any boot. **No boot without Ben's own "ready" and the seat's approval of code and gate
from origin** (`.claude/rules/booting.md`, ticked line by line).

**The scenario is `paper/src/main/resources/selftest/GATE-summoner.yml`, typed after the mage gate as ONE command:**

```
/rpg selftest mage-guaranteed+summoner
```

Mage-guaranteed's NEW readings in that run: **MG1R, MG2R2, MB1R, MB4aR, MB4bR, MB5R** (predicted in
`GATE-mage-guaranteed.md`); every other mage row is a **regression read** against its own prediction there.
Summoner's rows: **ALL AUTO, no ASK.** SURVIVAL, on the pad, facing east, every row self-staged: nothing is asked of
Ben's ground, position or inventory. Lint: green (`SelfTestScenariosTest` pins `GATE-summoner.yml` lint-clean).

## WHAT WAS READ OR EXECUTED, EACH MARKED

- **Difficulty scaling of a mob's hit on a PLAYER — READ, javap, `Player.hurtServer` (26.1.2 mojang-mapped):** when
  `DamageSource.scalesWithDifficulty()`, PEACEFUL returns false (no damage), EASY gives `min(a / 2 + 1, a)`, HARD gives
  `a * 3 / 2`, NORMAL is unchanged; then `Avatar.hurtServer` (super) runs, so the event our pricing reads carries the
  scaled amount. **The scaling is in `Player`, so a hit on the wolf is NOT scaled.**
- **Pricing — READ, `MobDamagePricing.price` → `MobScaling.attackDamage`:** `VANILLA_FACTOR (5.0) × gsFactor × base`
  for a vanilla mob; `MobGearScore` bounds a stored score to 1..500 (READ, `MIN`, `CAP`).
- **His health — READ:** `/rpg heal 1000000` (`healSelf`: `stats.heal(id, amount, id, true)`) fills his custom HP to
  max. **The snapshot captures his custom health at run start** (`SelfTestSnapshot`, `stats.current(id)`) and the
  restore writes it back (`healthToRestore`, logged `HEALTH saved=…`). It also restores the world's difficulty
  (`world.setDifficulty(difficulty)`) and the stored `max_summons` base (`SelfTestSummonsBase`, C1).
- **`difficulty: easy` parses — READ:** `SelfTestStaging`, `Difficulty.valueOf(arg.trim().toUpperCase…)`.
- **`clear_state` — READ, `SelfTestDrivers.clearState`:** clears cooldowns and removes all his summons with cause
  `CLEAR`, logged `STAGE clear_state … summons=<n>`.
- **`/rpg mobdamage` cannot kill the wolf — READ:** it calls `applyDamage(amount, player)`, whose source is PLAYER, and
  `refusedBySides` refuses PLAYER → SUMMON. So SM9 kills it by a mob's priced hit (the vanilla `damage … by` command).
- **A missing summon at cast time — READ, `SummonService.cast`:** `warnOnce` and `SummonRefused("That summon is not
  loaded.")`. **The load-time check (a weapon naming an unloaded summon refused by `ContentValidator`) is NOT built:
  OWED.**
- **INFERRED, the boot decides:** that the vanilla `damage <e> <n> minecraft:mob_attack by <zombie>` command reaches our
  mob-hit pricing as a melee hit by that zombie (SM3A, SM7, SM9); that a `tp` of the wolf and a `use` in the same tick
  put the wolf on the ray's line before the ray is cast (SM6).

## SAFETY: THE MOST THE RUN CAN DEAL HIM — A SUM, BEFORE THE ROWS

**The cap: 20.000 over the whole `mage-guaranteed+summoner` run.**

| source | staged attack | speed / AI | distance | lifetime | most hits × biggest hit |
|---|---|---|---|---|---|
| mage-guaranteed, every mob | — | frozen NoAI zombies (its own SAFETY section, READ) | 6+ east | per row | **0** |
| SM3A `s3a` | none acting: one scripted `damage @s 0.2 minecraft:mob_attack by s3a` | NoAI | 3 east | 7 ticks | **1 × (0.2 EASY-scaled: min(0.2/2+1, 0.2) = 0.2) × 5.0 × (gs ≤ 500)/100 = 5.000** |
| SM3B `s3b` | `attack_damage` base **0.0** | live, set on him (`mob_target`) | 12 east | 5 ticks, then killed | **0** (vanilla 0 → EASY `min(1, 0)` = 0 → `price` returns 0 for a non-positive amount, READ) |
| SM3C, SM4, SM5, SM6, SM7, SM9 zombies | — | NoAI | 2–6 east | their row | **0** |
| the wolf | — | ours; SUMMON → PLAYER is a refused cell | beside him | the run | **0** |
| his own Boltor ray (SM6) | — | the ray skips the caster | — | one shot | **0** |

**Sum: 5.000 ≤ 20.000**, with every term at its worst (the zombie's score at the cap of 500; his armour and the
EASY scaling counted as nothing). His health is staged full in the setup, so even the bound leaves him far above 0.

## CARRIED STATE (CC, IN THIS ORDER) — A CHECKLIST, EACH LINE TICKED IN THE REPORT

1. No `java` process; the stack heads equal origin (`git ls-remote --heads origin feat/summoner-gate`).
2. `git status --porcelain` empty; `git rev-parse HEAD` equals the remote head.
3. The world's saved difficulty is read and reported (memory: it can differ from `server.properties`); the setup sets
   EASY and the restore puts it back.
4. `booting.md`, every step.

## THE RUN SHEET — BEN TYPES ONE LINE

He is told **R0 passed**, logs in, stands anywhere, types `/rpg selftest mage-guaranteed+summoner`, waits for
`SELFTEST summoner END`, then `/stop` (the after-run step).

## HOW LONG THE RUN IS

**Summoner's waits sum to 434 ticks (21.70 s)** (`awk '/^  - wait: /{s+=$3}'` over the scenario); 150 other steps at
≥ 1 tick each (206 steps less 56 waits, `grep -c`). **About 30 to 40 seconds (an ESTIMATE: per-step cost not
measured).** Mage-guaranteed's waits are 2690 ticks; so the whole run is **about 3 to 3.5 minutes (ESTIMATE)**. The
run's `START` and `END` lines give the real figure.

## R0 — THE BUILD. READ IT FIRST. IF ANY R0 FAILS, STOP

### R0a — the build line names the stack top's head, clean

`[Rpg] Build: <sha>`, `<sha>` being `feat/summoner-gate`'s head as `git ls-remote --heads origin feat/summoner-gate`
prints it, shortened. **Not `-dirty`, not `unknown`.** Instrument: `Select-String -Path run\logs\latest.log -Pattern
'\[Rpg\] Build:'`.

### R0b — the jar carries the summoner stack and this gate

The scan is `docs/gates/GATE-summoner-r0b.ps1`, committed beside this file. **Prediction, line for line:** before
`'--- absent'`, **24 `PRESENT`, 0 `ABSENT`**; between `'--- absent'` and `'--- controls'`, **3 `ABSENT`, 0 `PRESENT`**;
after `'--- controls'`, **4 `ABSENT`, 0 `PRESENT`**. The controls: two PREFIX controls (`health: 1000`, `damage: 2`
absent while `health: 100` and `damage: 20` are present, so a line is matched whole), a `Has` and an `Entry` of a name
no file carries. **Run before commit on a locally packaged jar: see the PR body.**

### R0c — every Loaded line, and no `[Rpg]` WARN

- `[Rpg] Loaded 11 abilities, 44 visuals, 5 statuses, 7 elements, 23 enchants, …, 24 weapons, …`: **weapons 23 → 24**
  (the Bone Whistle); every other count as R2c of `GATE-mage-guaranteed.md`. Recounted by `ls` at this branch:
  abilities 11, visuals 44, statuses 5, elements 7, enchants 23, weapons 24.
- **`[Rpg] Loaded 1 summon(s)`** (`RpgPlugin`, `"Loaded " + summonDefs.size() + " summon(s)"`; `ls content/summons` = 1).
- `Mint-on-craft: 38 indexed, 38 claiming, of 54 gear definitions` (`BootCountLinesTest`).
- **0** `WARN]: [Rpg]` lines (control: `INFO]: [Rpg] Loaded` 1 or more); none of `Refusing`, `Skipping`, `SEVERE`,
  `Exception`, `Dev-only`, `currently prevented`.

### R0d — every scenario loaded

Exactly one **`[Rpg] Selftest: dev server, 410 scenarios across 48 gates [melee-cell, …, mage-guaranteed, summoner]`**
(recount: 48 files, 458 keys, 48 setups; `SelfTestScenariosTest`, control 444 + 14 = 458). Controls: `409 scenarios`
and `411 scenarios` ABSENT.

### R0e — the orphan sweep with nothing staged

One `FIGHTWORLD sweep path=…\rpgfight folders=0`, no `deleting`, no `orphan`.

## THE ROWS

Each row's lines are those between `SELFTEST summoner <row> START` and its `DONE`. **Clock: the server tick; every
"within N ticks" counts from the step that acts.** Wolf figures: `health = 100 × score/100`, `damage = 20 × score/100`
(Ben: "stats scale with gearscore"). Every wolf probe is `probe_summon "8"`:
`SUMMONPROBE weapon=bone_whistle body=<uuid> alive=true health=<cur>/<max> at=… dist_him=<d> within=8:<bool> target=…
directed=…`, or `SUMMONPROBE none owner=<him> store=<n>`.

### SM1 — THE CAST, THE COOLDOWN, THE REPLACE

- Press 1 (score 100): `SUMMON spawn … summon=bone_whistle_wolf score=100 health=100.000 damage=20.000`; mana falls by
  **40** (probe_mana before and after; the 5-tick regen is the only other term, UNVERIFIED rate, so the read is
  `before − after` in **[40 − regen(5 ticks), 40]**); `SUMMONPROBE … health=100.000/100.000 … within=8:true`.
- Press 2, 7 ticks later: **refused by the cooldown (200)**: no `SUMMON spawn`, no mana spent.
- Press 3 at score 200, ≥ 207 ticks after press 1: **`SUMMON removed … cause=replace`, then `SUMMON spawn … score=200
  health=200.000 damage=40.000`**; probe `health=200.000/200.000`.

### SM2 — TELEPORT BACK

The wolf is put 50 east of him (beyond `teleport_back` 48). **`SUMMON teleport … from=<his x + 50> …`** within the
5-tick wait; probe `within=8:true`. The line does not depend on where the wolf walked: the `from=` x is his x + 50.

### SM3A — A MOB THAT DAMAGED HIM: why=damaged

One scripted 0.2 (`mob_attack` by the frozen `s3a`). His custom HP falls by **0.2 × 5.0 × gs(s3a)/100** (gs read
from `probe s3a`; EASY leaves 0.2 unchanged) — every term named: base 0.2, difficulty ×1 (0.2 < 2), vanilla factor 5.0,
`s3a`'s score, his armour (DefenseRule as for any mob hit, read from the hit line). Then **`SUMMON target … target=<s3a>
why=damaged`**; probe `target=<s3a>`.

### SM3B — A MOB TARGETING HIM WITHIN 30: why=targeting

`DRIVE mob_target <s3b> tag=s3b`, then **`SUMMON target … target=<s3b> why=targeting`** within 3 ticks; probe
`target=<s3b>`. His health unchanged (attack 0).

### SM3C — A DIRECTED MOB: why=directed

`DRIVE reload held=bone_whistle` (the left click), then **`SUMMON target … target=<s3c> why=directed`**; probe
`directed=<s3c> target=<s3c>`.

### SM4 — A MOB HE MERELY HIT IS LEFT ALONE

His iron sword (charged by a 25-tick wait) hits the frozen `s4`: `DRIVE attack Player#attack target=<s4> …`; `probe s4`
falls by the hit line's `sent=`. **The wolf does NOT take it:** no `SUMMON target … target=<s4>`; probe
`target=none`.

### SM5 — THE CHASE LIMIT

Directed at `s5` (as SM3C), then `s5` put 40 east of him (beyond `chase_limit` 32): **`SUMMON target … target=none …
dropped=limit`** within 3 ticks; probe `target=none directed=none`.

### SM6 — SIDES: HIS RAY, THE WOLF ON THE LINE

The Boltor at score 100, aimed at the frozen `s6` 6 east, the wolf put 3 ahead on the eye line in the tick of the
press. **The wolf: health unchanged (`200.000/200.000`, the SM1 wolf), and no hit line naming it. `s6`: its health falls
by the shot's `sent=`**, whose terms are: attack 19 × score 100/100, the enchants `rpg enchant show` prints in the row,
level bonus 0 (level 1), no charge, crit as the line says. **The prediction is the equality `probe s6` loss = `sent=`,
and the wolf's 0.** (INFERRED: the same-tick tp; if the wolf is not on the line the row is VOID, not passed.)
**Owed:** the seat's shockwave, crystal shot and burst rows are NOT written (see the PR body).

### SM7 — A MOB'S PRICED HIT ON THE WOLF LANDS

A scripted 2 (`mob_attack` by the frozen `s7`). **The wolf's health falls by 2 × 5.0 × gs(s7)/100** (no difficulty
term: not a player; gs from `probe s7`; INFERRED: no defense term on a summon victim — the hit line decides).

### SM8 — WORLD DAMAGE REFUSED; THE VOID AND /kill REMOVE

`in_fire`, `fall`, `drown` 5 each: **health unchanged.** `out_of_world` 5: **`SUMMON removed … cause=void`**, probe
`none`. A fresh cast (score 100): `SUMMON spawn … health=100.000`. `/kill`: **`SUMMON removed … cause=killed`**, probe
`none`.

### SM9 — ITS DEATH (O1), THEN A FRESH CAST

A fresh wolf (score 100), then a scripted 1000000 by the frozen `s9` (≥ 5 × 1000000 × 1/100 ≫ 100):
**`SUMMON removed … cause=death`**, probe `none`. Then `clear_state` and a new cast: **`SUMMON spawn … score=100
health=100.000 damage=20.000`**, probe `health=100.000/100.000`.

### SM10 — THE CAP AT max_summons 0

**`SUMMON refused owner=<him> weapon=bone_whistle verdict=REFUSED_FULL max=0`**; the notice **"You cannot summon more
(max 0)."** once; mana **unchanged** (before = after); probe `none`. `rpg maxsummons reset` after.

### SM11 — CLEAR_STATE LEAVES NO SUMMON; NOTHING LEFT

A cast, then `clear_state`: **`STAGE clear_state … summons=1`** and **`SUMMON removed … cause=clear`**; probe
**`SUMMONPROBE none owner=<him> store=0`**. After the run: the restore's `max_summons` line puts the base back; no
`selftest`-tagged body and no wolf within 64 (R0e's sweep is re-read after the run: no orphan).

**Removal causes the pad stages: replace (SM1), void and killed (SM8), death (SM9), clear (SM11).** Not stageable here:
owner-death, quit, world, stop, orphan, backstop (unit tests only).
