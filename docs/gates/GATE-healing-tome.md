# GATE — THE HEALING TOME AND ITS RADIANT PULSE (2026-11-26), WITH THE BONE CLUB GATE, THE BLAST FUNGUS GATE AND TT20 TO TT27

**Status: RUN 2026-11-26 on `1f447197`, inside `GATE-blink-tome.md`'s one command; readings there, under READINGS.** The plan is `docs/plans/PLAN-healing-tome.md`. The scenario is
`paper/src/main/resources/selftest/GATE-healing-tome.yml`. The stack, bottom up: #316 `feat/class-damage-labels`, #317
`feat/fused-projectile`, #318 `feat/blast-fungus`, #319 `feat/fast-and-fungal`, #320 `feat/cluster-bombs`, #322
`feat/bone-club`, #323 `feat/drumming-beat`, then `feat/healing-tome`, `feat/tome-pulse`, `feat/tome-selftest`, and the ONE
gate branch at the top, `feat/blast-fungus-gate` (#321), MOVED UP onto `feat/tome-selftest` (its three commits unchanged,
`git range-diff` `=`), carrying this gate as well. **Every prediction below was written before any boot, and is frozen
from the seat's approval.** No boot without Ben's own "ready" and the seat's approval of the code AND this gate from origin.

> **SUPERSEDED FOR THE BOOT, 2026-11-26 (the seat's Blink Tome brief).** This gate's branch was MOVED UP again, onto
> `feat/blink-selftest` (its four commits unchanged, `git range-diff` `=`), and now also carries `GATE-blink-tome.md`.
> **Ben types that gate's ONE command**,
> `/rpg selftest bone-club+blast-fungus+true-tooltips.TT20.TT21.TT22.TT23.TT24.TT25.TT26.TT27+healing-tome+blink-tome`, in
> which this gate runs unchanged, after both asks and before the Blink Tome's; **`GATE-blink-tome.md`'s R0 governs, and
> the R0 below is kept as the record.** Each row-start CLEAN line and each `STAGE clear_state` line now ends
> ` blink=none`; the RESTORED line gains ` blink=none` before ` drag_tick=` (and see `GATE-blink-tome.md`'s dated note on
> H15's wording, which is read by its fields). The command below is the record of this gate's own stack.

## WHAT BEN TYPES — ONE COMMAND FOR THE WHOLE STACK

```
/rpg selftest bone-club+blast-fungus+true-tooltips.TT20.TT21.TT22.TT23.TT24.TT25.TT26.TT27+healing-tome
```

- `bone-club`, `blast-fungus` and `true-tooltips.TT20 … TT27` exactly as `GATE-bone-club.md` lists them (unchanged).
- `healing-tome`, this whole gate, **LAST**: its setup, then HT1 to HT8. **It asks nothing.** It runs after both asks,
  so its staging (his class, his accessory slots, his armour, his saturation) touches no row of the gates below it.
- The queue, step for step, is `HealingTomeGateCommandTest` (parsed against the shipped scenarios).
- **This gate's R0 supersedes the R0 of every gate below it** (theirs are left as the record).

He types it on the pad's server, standing anywhere; the first setup takes him to the pad. **He answers TWO questions in the
whole command, BF9's and TT27's, as before: this gate adds none.**

**How long it runs (an ESTIMATE from the scenarios' waits, not a measurement):** this gate's `wait:` steps sum to 1802
ticks (setup 40; HT1 50, HT2 228, HT3 228, HT4 228, HT5 220, HT6 312, HT7 282, HT8 214), about 90 s, with each row's CLEAN
about a minute more in all. With `GATE-bone-club.md`'s estimate for the rest (about three and three quarter minutes, plus
two answers): **about six and a half minutes, plus two answers.**

## SAFETY — 19.000 FOR THE WHOLE COMMAND; EVERY TERM EXECUTED

- **The wound is `/rpg damage <W>`, the engine's own self-damage path.** READ, `RpgCommand.damageSelf`:
  `stats.damage(id, W, id, true)`. So the dealer is HE: no mob, no attack attribute, no event amount, no difficulty, no
  score (S-3 has no dealer's score to seed). The only other term is his DEFENCE, through `Defense.applyDefense`.
- **His defence is STAGED to 0**: every row takes off his armour and off hand (`item replace … with minecraft:air`; W1 puts
  his own back), clears his accessory slots (H15 puts his own back), and stages level 2 or 3, whose level defence is 0
  (EXECUTED, `LevelBonus.at(2).defense()` = `at(3).defense()` = 0). **The HURT line reads it** (`defense=`). His
  fragments are his own and are not staged; a defence above 0 only LOWERS what goes through, so every bound below holds.
- **Each wound EXECUTED through the store** (`HealingTomeGateArithmeticTest.theWounds_andTheSafetySum`: his store at the
  staged level, full, then `CombatantStats.damage(id, W, id, true)`):

| row | level | staged max (EXECUTED) | W | HURT, EXECUTED | his health after | half his max |
|---|---|---|---|---|---|---|
| HT2 | 2 | 105 | 7 | `before=7.000 after=7.000` | 98.000/105.000 | 52.5 |
| HT3 | 3 | 110 | 8 | `before=8.000 after=8.000` | 102.000/110.000 | 55 |
| HT4 | 3 | 110 | 2 | `before=2.000 after=2.000` | 108.000/110.000 | 55 |
| HT6 | 2 | 105 | 2 | `before=2.000 after=2.000` | 103.000/105.000 | 52.5 |

- **This gate: 7 + 8 + 2 + 2 = 19.000** (EXECUTED, the same test). No row wounds him below half his staged max (asserted
  per row). **Every wounding row is SURVIVAL and ends with `rpg heal 1000000`** and a REGEN reading at full.
- **`GATE-bone-club.md`: 0.000. `GATE-blast-fungus.md`: 0.000. TT20 to TT27: 0.000** (each that gate's own SAFETY).
- **THE SUM FOR THE WHOLE COMMAND: 0.000 + 0.000 + 0.000 + 19.000 = 19.000, at or under 20.000.**

## CARRIED STATE — THE BOOTER'S CHECKLIST, IN THIS ORDER (`.claude/rules/booting.md`, each step ticked in the report)

1. The seat's approval of the code AND this gate from origin; Ben's own "ready".
2. **The heads:** `git ls-remote --heads origin` for the eleven branches, equal to what the seat diffed; CI green on each.
   `git status --porcelain` prints nothing and `git rev-parse HEAD` equals `feat/blast-fungus-gate`'s head.
3. **No JVM:** `Get-CimInstance Win32_Process -Filter "Name='java.exe'"` counts 0.
4. **The backup**, to a NEW folder `C:\Users\Neb91\IdeaProjects\rpg-run-backups\<date>-pre-healing-tome`: `run\world`,
   `run\plugins\Rpg` (which holds `accessories\<his uuid>.json` and `players\<his uuid>.json`) with their file counts and
   copy counts, and his player `.dat` with its bytes. **His accessory file's bytes and his profile's `archetypeId` are
   quoted before the boot**, so the run's CAPTURED line has a third witness.
5. **The world's saved difficulty,** read from the decompressed `level.dat`, before and after the run.
6. **Config:** `dev-pack-on-join: true`. No `run\rpgfight`.
7. **"BOOTING NOW", in its own message, BEFORE the boot command**, with: "Ben: do NOT log in until I tell you R0 passed."
8. `./scripts/dev-server.sh --refresh-content`; the `Done` matched to a log newer than the deployed jar.
9. R0 read on this boot's log; **"R0 PASSED" in its own message, before he logs in.** His first `joined the game` time
   reported beside R0's.
10. After his `/stop`: the clean-stop lines, no `java` process left; **his accessory file's bytes and his profile's
    `archetypeId` read again and compared with step 4's** (H15's third witness).

**HIS CLASS AND ACCESSORIES, BEFORE AND AFTER (H15):** the run's first lines are `SELFTEST RUN START …` and
`SELFTEST RUN CAPTURED armour=<n> offhand=<0|1> class=<his class> accessories=[<slot 0>,<slot 1>,<slot 2>,<slot 3>]`
(each slot his accessory's id, `-` for empty, `unreadable` for a stored item this server cannot read). **The prediction:
the RESTORED line, after the last row, ends `… armour=<n> offhand=<0|1> saturation=<s> accessories=[<the same four>]` with
NO `NOT-RESTORED=accessories(…)`, and its `cell=<his class>/<his element>` equals the CAPTURED class.** His level is put
back by his lifetime XP (`xp=<the captured xp>`), as every run's is (READ, `SelfTestSnapshot.restore`). His saturation is
put back too (`saturation=` on the RESTORED line, the value CAPTURED at run start).

## R0 — THE BUILD. READ IT FIRST. IF ANY R0 FAILS, STOP

- **R0a:** `[Rpg] Build: <sha>`, `feat/blast-fungus-gate`'s head as `git ls-remote --heads origin feat/blast-fungus-gate`
  prints it, shortened. Not `-dirty`, not `unknown`.
- **R0b:** ALL THREE scripts on the DEPLOYED jar.
  - `docs/gates/GATE-healing-tome-r0b.ps1`: before `'--- absent'` **25 `PRESENT`, 0 `ABSENT`** (6 `Entry`, 9 `Has`, 10
    `ValueLine`); between `'--- absent'` and `'--- controls'` **4 `ABSENT`, 0 `PRESENT`** (no `modifiers:`, no
    `mana_cost:`, in no loot source, no `ResourcePool` in `TomePulses`); after `'--- controls'` **2 `PRESENT`, 0 `ABSENT`**.
  - `docs/gates/GATE-bone-club-r0b.ps1`: **21 / 0, 5 / 0, 2 / 0**, unchanged.
  - `docs/gates/GATE-blast-fungus-r0b.ps1`: **18 / 0, 4 / 0, 2 / 0**, unchanged.
  - **Run before commit on a jar built from this tree (`paper/target/rpg-0.1.0-SNAPSHOT.jar`, 2026-11-26): 25 / 0, 4 / 0,
    2 / 0; 21 / 0, 5 / 0, 2 / 0; and 18 / 0, 4 / 0, 2 / 0, as predicted.**
- **R0c:** `[Rpg] Loaded …, 27 weapons, …, 7 accessories, …` (recounted: `ls content/accessories/*.yml | wc -l` = 7, 6
  at `01bf729a`; the weapons and enchants unchanged from `GATE-bone-club.md`'s 27 and 27). `Mint-on-craft: 42 indexed, 42
  claiming, of 61 gear definitions`, unchanged (`BootCountLinesTest` green: accessories are not gear definitions of the
  crafting index, ruling A3). **0** `WARN]: [Rpg]` lines (control: `INFO]: [Rpg] Loaded` reads 1 or more); none of
  `Refusing`, `Skipping`, `SEVERE`, `Exception`.
- **R0d:** exactly one **`[Rpg] Selftest: dev server, 519 scenarios across 54 gates [melee-cell, …, blast-fungus,
  bone-club, healing-tome]`**. Recount: 54 files, 573 keys (`cat GATE-*.yml | grep -cE '^[A-Za-z0-9_-]+:'`), 54 setups, so
  519. Control: 564 + 9 (its setup and HT1 to HT8) = 573. Controls: `511 scenarios` ABSENT, `53 gates` ABSENT.
- **R0e:** one `FIGHTWORLD sweep path=…\rpgfight folders=0`, no `deleting`, no `orphan`; and no `FUSED removed …
  cause=orphan`.

## THE ROWS

Each row's lines are those between `SELFTEST healing-tome <row> START` and its `DONE`. **Every row is AUTO, SURVIVAL, on
the pad, and stages itself** (see the scenario's header): level, empty inventory and state, armour and off hand off,
`accessory: clear` (`STAGE accessory clear cleared=<n>`), `class: mage` (`STAGE class found=<c> set=mage element=<e>
ok=true`), `saturation: 0` (`STAGE saturation found=<s> set=0.0`), 40 ticks, `rpg heal 1000000`, then a REGEN line.

**THE CLOCK: the server tick.** A `wait: N` resumes exactly N ticks later, and steps with no wait between them run in one
tick (READ, `SelfTestRun.advance`). **E** is the tick of an `accessory: equip 0` step, printed on its own line:
`STAGE accessory equip slot=0 id=healing_tome tick=E`. **T0, THE ANCHOR, is printed by the clock itself:**
`TOMEPULSE START wearer=<him> accessory=healing_tome slot=0 tick=T0 interval=80`, written by the first reconcile pass that
sees the tome contribute. The pass runs every 5 ticks per player (READ, `PlayerHealthSystem.RECONCILE_PERIOD_TICKS`), so
**T0 is in [E, E + 5]** (CC'S CALL: the anchor is that pass; `PLAN-healing-tome.md` H4). **Each pulse is EXACTLY 80 ticks
after the one before, from T0: T0 + 80, T0 + 160.** READ: `RepeatingTask.start` arms its first step one period out
(`target.scheduleTick(periodTicks, …)`), and each step re-arms by one period. INFERRED: Paper's entity scheduler runs a
task delayed by 80 exactly 80 ticks later; the pulse lines' `tick=` read it.

**THE PULSE LINE (H10):** `TOMEPULSE wearer=<him> level=<L> tick=<t> candidates=<n> healed=<h>`, on EVERY pulse while trace
is on (the setup turns it on), a pulse that heals nobody included. For each player healed, at the heal and in this order:
`HEALED amount=<a> after=<c>/<m> dealer=self` (the selftest's ear on the store: the dealer is the wearer, who is HE),
`TOMEPULSE target=<him> before=<b> amount=<a> after=<c>`, and `TOMEPULSE feedback target=<him> particle=happy_villager x6
heart=heart sound=block.amethyst_block.chime`. **`candidates=1` in every row: he stands alone on the pad** (another player
within 8 blocks would read 2, and is the one thing this gate cannot stage; see WHAT ONLY A SECOND PLAYER CAN SHOW).

**HIS REGENERATION, EVERY TERM (the seat: a reading is exact, not "rose").** READ, `HealthRegenSystem`: his own per-player
loop pays `HealthRegen.healAmount(rate, saturated, 20, current, max)` every 20 ticks, on its own phase, through
`CombatantStats.heal`, so **every payment is a `HEALED … dealer=self` line not followed by a `TOMEPULSE target=` line.**
Staged: saturation 0, so the UNSATURATED tier, ×1 (READ, `HealthRegen.UNSATURATED_MULTIPLIER`); the rate is the base 0.2
plus his level's (0 at levels 2 and 3, EXECUTED) plus his gear's (none: armour off, slots cleared). **Predicted REGEN line:
`REGEN rate=0.200 saturation=0.0 customHP=<max>/<max>`.** Each payment is then **0.200** (EXECUTED,
`HealthRegen.healAmount(0.2, false, 20, 98, 105)` = 0.2). **If the REGEN line reads another rate r (his fragments are his
own), every figure below is restated at r, and a row whose bound then fails is VOID, not a pass.**
- From a wound at E + 6 to a pulse at T0 + 80 is 74 to 79 ticks, so **n1, the payments before the first pulse, is 3 or 4**;
  to T0 + 160, 154 to 159 ticks, so **n2 is 7 or 8**. Both counts are READ off the HEALED lines; every row's prediction is
  EXECUTED at every allowed count (`HealingTomeGateArithmeticTest.thePulses_atEveryAllowedRegenCount`).

### HT1 — THE TOOLTIP, WHOLE, AND A4'S REFUSALS BY TEXT

Level 2, a mage, the tome given and held (NOT equipped).
1. `HELDID slot=<n> item=healing_tome`.
2. `dump: held`, WHOLE (EXECUTED, `HealingTomeGateArithmeticTest.theHeldLine`, through `WeaponItems.displayName`,
   `AccessoryLore.build` and the instrument's own `SelfTestLines.styled`):
   **`HELD healing_tome BOOK x1 name="Healing Tome" [blue] lore=["Radiant Pulse" [gold]; "Every 4s, heals hurt players
   within 8 blocks" [gray]; "for 1 health per your level" [gray]; "Mage class accessory" [gray]; "" []; "Rare Accessory"
   [blue]]`**. No modifier line (it has none), no flavour (none authored), no gear score, no enchant block (A4).
   INFERRED: `ItemStack#effectiveName` returns the display name the mint set; the name's half is read here.
3. **H13, A4's existing words, each a REPLY** (one per command, P-R1: each answers the earliest unanswered command):
   `rpg enchant show` → **`REPLY "Accessories cannot be enchanted." [red]`**; `rpg gearscore set 100` → **`REPLY
   "Accessories carry no gear score." [red]`**; `rpg durability damage 1` → **`REPLY "Accessories do not wear out."
   [red]`**. The anvil's and grindstone's words (`Accessories cannot be worked at the anvil.`, `Accessories have nothing to
   grind off.`) are the same constants (`AccessoryRefusals`, asserted by text in `TomePulsesWiringTest.a4sWordsAreUnchanged`)
   behind the stations' explicit A4 arms; **driving those two screens is NOT in this gate** (owed, below).
4. No `TOMEPULSE` line in the row (carried, not equipped).

### HT2 — THE PULSE (L = 2)

Equipped at E, wounded by 7 at E + 6: **`HURT before=7.000 after=7.000 defense=0.000 dealer=self dealerType=self`**.
Then, at the ticks the clock prints:

| tick | line | prediction (r = 0.200; n1 ∈ {3, 4}, n2 ∈ {7, 8} READ off the HEALED lines) |
|---|---|---|
| T0 ∈ [E, E + 5] | `TOMEPULSE START` | `… accessory=healing_tome slot=0 tick=T0 interval=80` |
| T0 + 80 | pulse 1 | **`TOMEPULSE wearer=<him> level=2 tick=<T0+80> candidates=1 healed=1`**, then `HEALED amount=2.000 after=<98.000 + 0.200 × n1 + 2.000>/105.000 dealer=self`, **`TOMEPULSE target=<him> before=<98.000 + 0.200 × n1> amount=2.000 after=<before + 2.000>`**, and the feedback line. At n1 = 3: before 98.600, after 100.600; at n1 = 4: 98.800, 100.800. |
| T0 + 160 | pulse 2 | **`… level=2 tick=<T0+160> candidates=1 healed=1`**, **`TOMEPULSE target=<him> before=<98.000 + 0.200 × n2 + 2.000> amount=2.000 after=<before + 2.000>`**, the feedback line. |

- **amount=2.000 = L x heal_per_level = 2 x 1 at both pulses** (EXECUTED at every n1, n2: the least missing at pulse 2 is
  7 − 1.6 − 2 = 3.4, more than 2).
- **Between the two pulses, exactly 80 ticks, and no third pulse:** at E + 176 `accessory: clear`; the next pass (≤ E + 181)
  writes `TOMEPULSE STOP wearer=<him> tick=<t> reason=inactive`, before T0 + 240 (≥ E + 240).
- The REGEN line after the pulses: `REGEN rate=0.200 saturation=0.0 customHP=<98.000 + 0.200 × n + 4.000>/105.000` (n
  the payments to then); after `rpg heal 1000000`: **`REGEN rate=0.200 saturation=0.0 customHP=105.000/105.000`**.

### HT3 — THE LEVEL (L = 3)

HT2 at level 3 (max **110**, EXECUTED), wounded by 8: **`HURT before=8.000 after=8.000 defense=0.000 dealer=self
dealerType=self`**. Pulse 1: **`level=3 … healed=1`**, **`TOMEPULSE target=<him> before=<102.000 + 0.200 × n1>
amount=3.000 after=<before + 3.000>`**. Pulse 2: **`amount=3.000`** again (EXECUTED: the least missing at pulse 2 is 8 −
1.6 − 3 = 3.4). **The amount is the staged level, 3, not 2: Ben's "1 health per the players level".** Ends at
**`customHP=110.000/110.000`**.

### HT4 — THE CAP (L = 3, missing less than L)

Wounded by 2: **`HURT before=2.000 after=2.000 defense=0.000 dealer=self dealerType=self`**, health 108.000/110.000.
- Pulse 1: **`level=3 … candidates=1 healed=1`**, **`TOMEPULSE target=<him> before=<108.000 + 0.200 × n1>
  amount=<110.000 − before> after=110.000`**: at n1 = 3, **before 108.600, amount 1.400, after 110.000**; at n1 = 4,
  **108.800, 1.200, 110.000** (EXECUTED through the store: exactly max, `HealingTomeGateArithmeticTest`). **amount = what
  is missing, never L; health EXACTLY max.**
- Pulse 2: **`level=3 … candidates=1 healed=0`**, and **no** target, HEALED-by-pulse or feedback line after it (full; his
  regeneration pays nothing at max).

### HT5 — NOT HURT (L = 2)

Full health (`REGEN … customHP=105.000/105.000`), equipped, no wound.
- Two pulses, at T0 + 80 and T0 + 160: **`TOMEPULSE wearer=<him> level=2 tick=<t> candidates=1 healed=0`**, each.
- **No `TOMEPULSE target=` line, no `TOMEPULSE feedback` line, and no `HEALED` line at all** in the row after the
  `rpg heal 1000000`'s own (full: regeneration pays nothing, READ `HealthRegen.healAmount` returns 0 at max). **No
  feedback call** (H9: feedback is called only after a heal landed).

### HT6 — INACTIVE, AND LIVE AGAIN

Equipped as a mage at E, then **`class: ranger` in the same tick** (`STAGE class found=mage set=ranger …`), then wounded
by 2 in the same tick: **`HURT before=2.000 after=2.000 defense=0.000 dealer=self dealerType=self`**.
- **INACTIVE:** across E to M = E + 170, **no `TOMEPULSE` line of any kind** (no START: no reconcile pass ever saw him a
  mage wearing it, because the three steps ran in one tick). His wound stays open for the whole window (EXECUTED: 2.0 at
  0.2 a payment needs 10 payments, and 170 ticks holds at most 9), so a live tome WOULD have healed him.
- **THE SHEET, inactive** (the `rpg stats` at E + 6), its REPLY lines include **`REPLY "Accessories" [gold]`** and
  **`REPLY "  Healing Tome: inactive (requires Mage)" [dark_gray]`** (READ `AccessorySheet.lines`; EXECUTED
  `AccessoryLoreLines.sheetLine`).
- **LIVE AGAIN, the control:** `class: mage` at M. **`TOMEPULSE START … tick=T0`, T0 ∈ [M, M + 5].** The sheet at M + 6:
  **`REPLY "  Healing Tome: Radiant Pulse" [gray]`**. **One pulse at T0 + 80: `TOMEPULSE wearer=<him> level=2 tick=<T0+80>
  candidates=1 healed=0`**: healed=0 because his regeneration has filled him by then (250 ticks or more since the wound,
  12 or more payments; EXECUTED: exactly full at every count from 12 to 20, `ht6sControlPulseFindsHimExactlyFull`). **The
  control is that the line exists at all:** an inactive tome writes none. Then `accessory: clear` at M + 90, and the STOP
  line before T0 + 160.

### HT7 — UNEQUIP STOPS IT; RE-EQUIP RESTARTS THE COUNT

Full health, no wound. Equipped at **E1**: `START … tick=T0`, T0 ∈ [E1, E1 + 5]; **one pulse at T0 + 80** (`healed=0`).
`accessory: clear` at E1 + 100: **`TOMEPULSE STOP … reason=inactive`** at the next pass, ≤ E1 + 105. Re-given and
re-equipped at **E2 = E1 + 132** (`STAGE accessory equip … tick=E2`): **a NEW `START … tick=T0'`, T0' ∈ [E2, E2 + 5]**, and
its **first pulse at T0' + 80**, not sooner. **The old clock's next pulse, T0 + 160 ∈ [E1 + 160, E1 + 165], NEVER comes:**
no `TOMEPULSE wearer=` line between the second START and T0' + 80 (≥ E1 + 212). Then `accessory: clear` at E2 + 100 and its
STOP line. Exactly **two** pulse lines in the row.

### HT8 — CARRIED, NOT EQUIPPED

A mage at level 2 holding the tome in his main hand (`HELDID slot=<n> item=healing_tome`), his slots empty: **no
`TOMEPULSE` line of any kind across 170 ticks.** Control: HT5, the same staging with the tome EQUIPPED, wrote a START and
two pulses.

### H15 — AFTER THE LAST ROW

The RESTORED line (see CARRIED STATE): **`accessories=[…]` equal to the CAPTURED line's, no `NOT-RESTORED=accessories`,
`cell=<his class>/<his element>` with his CAPTURED class**, `xp=<his captured lifetime XP>`, `saturation=<captured>`, and
W1's `armour=` and `offhand=` as captured with no `NOT-RESTORED=` slot.

### WHAT ONLY A SECOND PLAYER CAN SHOW — NOT READ BY THIS BOOT

**That another player within 8 blocks is healed** (candidates 2, a second target line). This boot has one player; it is
**NOT READ**, and is covered by the pure decision's table (`HealingPulsesTest`: a hurt player at 8.0 exactly is healed by
L, at 8.001 is not, a player in another world is not) and the wiring test (`TomePulsesWiringTest`: every other target is
healed on its own thread through the same `apply`). **OWED** to a boot with two players.

## THE SCAN RUN BEFORE COMMIT

- `ScenarioLint` over all 54 gates: this gate's findings **none** (`HealingTomeGateCommandTest.theGateLintsClean_andAsksNothing`:
  every rule, `ACCESSORY_UNSTAGED` included), and no new finding anywhere (`SelfTestScenariosTest`'s pins unchanged but
  for the counts and lists re-pinned above). `AskWindowLintTest` unchanged: this gate has no ASK. The gate-document test
  sees both sections.
- **The R0b scripts, run on a jar built from this tree:** quoted under R0b, all three as predicted. The full suite on
  this tree: core 2692 run (1 skipped), storage 103, paper 2186, 0 failures.

## CC'S CALLS IN THIS GATE

1. **The gate runs LAST in the command**, after both asks, so its staging touches no lower row.
2. **The wound lands after the anchor** (E + 6), so the regeneration window before each pulse is bounded both ways.
3. **HT6's control is in the same row** (back to mage), as well as HT2's healing pulse: the inactive window and the
   live window share one staging.
4. **Armour and the off hand are taken off by `item replace … with minecraft:air`** (vanilla), not by a new verb: W1
   already puts them back by name at the gate boundary and the run's end.

## PLACEHOLDERS OPEN FOR BEN

`PLAN-healing-tome.md`, PLACEHOLDERS: the ability's name, the tooltip and sheet wording, the feedback (particles, heart,
sound and its volume and pitch), two wearers stacking, no line of sight needed, and the trade and shelf refusal words.

## WHAT ONLY THE BOOT CAN SHOW

That the server's tick spacing agrees with the READs (every `tick=` on the START and pulse lines); that the reconcile pass
anchors the clock within 5 ticks of an equip; that the class verb, the accessory verbs and H15's restore run on a real
store; that the feedback PLAYS and RENDERS (particles and the sound are the client's: AUTO cannot see or hear them, and
this gate asks nothing, so **the feedback's look is NOT READ**, only that it was called); that the tooltip RENDERS as
dumped; **that a second player is healed (NOT READ)**; and **that a villager refuses to buy the tome and a chiseled
bookshelf refuses it (H12: NOT READ by this gate; pinned by `TomePulsesWiringTest` and READ in the jar)**. The two
stations' A4 screens (anvil, grindstone) for the tome are **NOT READ** here (their words are asserted by text).
