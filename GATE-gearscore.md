# GATE — Gear Score (Slices 12 and 12b)

**Status: PARTIALLY READ — 17 of 24 rows. Slice 12b's eleven on `4648283`, 2026-09-20; slice 12c's
six on `593e6a7`, 2026-09-21. R12 PASSED ON (ii) AND ITS (i) READING IS WITHDRAWN — see *ONE READING
WITHDRAWN* below. R2 is NARROWED, not voided.**

> ### *** THIS FILE SAID "SLICE 12c … NONE HAS BEEN BOOTED" WHILE 12c WAS MERGED AND SHIPPED ***
>
> **12c was booted on 2026-09-21 and all six rows passed. The readings existed only in a chat log
> for a day**, so `master` carried a gate file asserting that a shipped slice had never been read.
> **That is the exact failure this file's own opening records for slice 12** — *"the file existing
> read as the gate being handled"* — arriving a second time, one slice later, in the same file.
>
> **A chat transcript is not a record.** It is not greppable by whoever next opens the file, it does
> not survive the session, and it cannot fail. The readings are written in below.

```
READ AND PASSED   16   R0 R1 R2 R6b R10 R10b R11 R13 R14 R15          (12 / 12b, on 4648283)
                       R16 R17 R18 R19 R20 R21                        (12c, on 593e6a7)
READ, PART VOID    1   R12   (ii) PASSED; (i) reading WITHDRAWN 2026-09-20
NOT RUN            7   R3 R4 R5 R6 R7 R8 R9      (reason recorded in each cell)
                  ──
                  24   = git grep -c '^### R[0-9]' <ref> -- GATE-gearscore.md
```

> ***THE NEEDLE GAINED ITS `[0-9]` ON 2026-09-21, AND IT WAS NOT COSMETIC.*** It was `^### R`, which
> matches **any** heading beginning with R — and this file now has prose headings that do. Writing
> one took the count to **25 against a block claiming 24**, with no row added. **A row-count
> instrument that counts prose is the file's own figure disagreeing with itself**, and the
> disagreement is the only thing that would have reported it. `[0-9]` is what makes the needle match
> rows and nothing else.

**R0 IS READ TWICE, AND THE TWO READINGS SIT BESIDE EACH OTHER RATHER THAN ONE OVER THE OTHER.**
A different jar and a different slice are different subjects; overwriting 12b's reading would delete
the only record that 12b's jar was ever confirmed. The file's convention — *readings beside
predictions, never over* — applies to readings against each other for the same reason.

**Readings are PASS/FAIL with no figures recorded — Ben's ruling, 2026-09-20.** Each cell says so
in those words. **An unread cell and an unrecorded figure must never look alike**, which is exactly
what the block below is about, so a passed row states its verdict, its date and its SHA rather than
sitting empty.

> ### *** AND SLICE 12 MERGED WITH EVERY ONE OF ITS CELLS EMPTY. THAT WAS AN OVERSIGHT, NOT A DECISION. ***
>
> **Rows R1–R9 belong to slice 12, which merged as `1c030e2` with all ten of its `READ` cells
> empty.** Gear score scaling was live on `master` from that commit until 2026-09-20 **with nothing
> having watched it work** — and R1 and R2 are its only sole witnesses, neither visible to any
> module, because no test can construct an `ItemStack`. **Both have now been read and both passed.**
>
> **Nobody decided to ship it unread.** The file existing read as the gate being handled. It is
> named here, at the top, rather than only in a merge description — **a PR body is read once and
> this file is read every time someone opens it.**
>
> **12b READ ELEVEN ROWS AND LEFT SEVEN.** That scope, its reason, and the fact that it grew from
> seven to eleven during the slice are recorded under *THE BOOT SCOPE* below. **This line is the
> pointer; that section is the account.**
>
> > **THE "SEVEN LEFT" FIGURE SAID SIX UNTIL 2026-09-20, IN THIS FILE AND IN THE BOOT SHEET BOTH**,
> > because every hand-typed list omitted **R6**. It was caught by tallying the table rather than
> > re-reading the list — `11 + 7 = 18` — which is this project's own *add the parts up* rule
> > finding a count that three people had copied forward without summing.

**Every prediction below was written BEFORE any boot, and no prediction is edited once a row has been
read.** Readings go in the `READ` column beside the prediction they answer, never over it.

**GAME MODE: SURVIVAL, for every row unless the row says otherwise.** Declared per the standing debt
in `CLAUDE.md` — sixteen `GATE-*.md` files still owe this line and this one is not going to be the
seventeenth.

> **WHY SURVIVAL MATTERS LESS HERE THAN IT DID FOR THE PLUME, AND IT IS STILL DECLARED.** The
> creative-divergence register's shape is *creative removes a COST* — ammunition, durability,
> consumption. **Gear score scales a number rather than spending anything**, so no row below is
> satisfied for free by a removed cost. The two rows that could diverge are **R7** (the armour bar,
> which creative hides entirely) and **R9** (the enchant-table control). Both say so in the row.

---

## The band is RULED, and this gate is completable

**Ben ruled the band while the slice was being built: a drop rolls uniformly over `average - 5` to
`average + 15`.** In `GearScoreBand`'s own parameterisation that is **SPREAD 10, SKEW 5** --
`skew - spread = -5`, `skew + spread = +15`, both endpoints exact.

**So nothing in this file is owed any more.** R6b was marked OWED and UNSTAGEABLE in the first
draft of this gate, because a zero-width band has no inside to land in; it is now a full row with
predictions, and the constants are named `SPREAD` and `SKEW` rather than `SPREAD_OWED` and
`SKEW_OWED`. **The zero was discharged by a ruling, not deleted because someone tired of it** --
`GearScoreBand`'s javadoc keeps that history, since the next person to meet an owed number in this
codebase should be able to see what the placeholder bought.

**HOW LONG THE CLIMB TAKES -- SIMULATED BY THE OPERATOR, NOT DERIVED HERE: median 182 scored drops**
from the floor to the 400 soft cap over 400 simulated players, min 150, max 216. **A later tuning
pass RE-RUNS that simulation rather than adjusting the figure**, and its invalidator is an event:
any change to SPREAD, SKEW, or `averageOf`'s denominator or floor. No row below measures it -- the
suite asserts the band's arithmetic and says nothing about the length of the walk.

---

## *** R0 — THE FIRST ROW OF EVERY GATE FILE FROM NOW ON ***

### R0 — The deployed build carries this slice

**SOLE WITNESS, AND IT IS THE WITNESS FOR EVERY OTHER ROW IN THE FILE.** Nothing below means
anything if the jar under the server is not the one the rows were written against — and a wrong
jar does not announce itself. It produces *readings*, in the right shape, at plausible values.

> ### *** THIS ROW EXISTS BECAUSE 12b LOST A BOOT TO ITS ABSENCE, ON 2026-09-20 ***
>
> The slice was booted from a jar built out of the **master worktree**. The branch lives in a
> second worktree; only the master one has a `run/` directory. **The jar was rebuilt that morning**,
> so its mtime was current and every staleness check cleared it.
>
> **It produced a complete, self-consistent, entirely false set of readings:**
>
> | | |
> |---|---|
> | **R1 PASS** | true of master, and taken as evidence about the branch |
> | **R10, R11 FAIL** | read as a live defect in the trigger path — two weapons, two authored amounts, both unscaled. **Hours went into diagnosing code that was not in the jar.** |
> | **R10b, R12(ii)** | would have read **PASS** — their predictions are the AUTHORED value, which is what a build with no scaling produces at every score. **A FALSE PASS FAMILY.** |
> | **R14** | would have read **FAIL** — `unscored` was absent from the deployed content, so a fresh `volley_stone` takes a rolled score. **A FALSE FAIL FAMILY, and it looks exactly like a real defect in the stamp refusal.** |
>
> **Both families in one run.** The only reason it was caught is that R10 failed LOUDLY on a claim
> nobody could explain from content — had the slice been display-only, or had R10 not been staged,
> every row would have been believed.
>
> **And the instrument that settled it is the one this row prescribes**, not an mtime and not a
> `git status`: reading the class files *inside the deployed jar*. `grep` on the jar returns 0 for
> everything, because a jar is a ZIP and its classes are deflated — **an instrument that cannot
> express what it is being asked.**

**RUN THIS FIRST. IF IT FAILS, STOP: NO OTHER ROW IN THE FILE IS READABLE.**

| | |
|---|---|
| **Setup** | Name one symbol this slice introduces that did not exist before it — a method, a class, or a content key. Then read it out of the **deployed** jar, not the built one: `unzip -p <deployed>.jar <path/to/Class>.class \| tr -cd '[:print:]\n' \| grep -c <symbol>`, and for content `unzip -p <deployed>.jar content/<file> \| grep -c '^<key>:'`. |
| **Predict** | Every probe returns **≥ 1**. A **0** means the deployed jar predates this slice, whatever its mtime says. |
| **Predict** | The same probe against the jar you just built returns the same answers — **if they differ, the deploy step did not run**, which `set -e` and a file lock have both caused on this project before. |
| **Predict** | **STATE WHICH TREE THE JAR WAS BUILT FROM.** A worktree checkout makes "the repo" ambiguous, and that ambiguity is what cost the 2026-09-20 boot. |
| **READ, 12b** | **PASS** — 2026-09-20, `4648283`. `CombatWorld.triggerScoreOf` = 1, `GearScore.carriesScore` = 1, `volley_stone.yml` `^unscored:` = 1, all read from the deployed jar; the same three read **0** on the jar booted earlier that day. |
| **READ, 12c** | **PASS** — 2026-09-21, `593e6a7`. Deployed jar `run/plugins/rpg-0.1.0-SNAPSHOT.jar`, sha256 `39ABF8E3…`, written 02:37:13, server up 02:37:14. **474 class files scanned**; `deliveredShots` present in exactly `core/weapon/WeaponLoreLines.class` (the declaration) and `paper/weapon/WeaponLore.class` (the call site). Built from the `minecraft-rpg-scaffold` worktree on `feat/12c-display-truthfulness` @ `593e6a7`, **whose `run/` is the one booted**; the second worktree `rpg-12b` @ `905fcf8` was not involved. `carriesScore` not probed — redundant once both hits landed. |

> ***THE CLASS COUNT IS IN THAT CELL BECAUSE IT IS THE NO-OP VALUE.*** `474 scanned` is what makes
> "present in exactly two classes" a measurement; **`0 scanned` would make the same sentence a
> reading of an empty directory**, and the two are indistinguishable without it.

> ### *** AND THE BOOT FOUND A DEFECT IN THE INSTRUMENT NEXT TO THIS ONE ***
>
> **`dev-server.sh`'s `Jar OK` line cannot fail under the rival hypothesis, and is therefore not
> evidence.** The hypothesis R0 exists to refute is *"the deployed jar is the wrong build"* — and
> measured on 2026-09-21, **`master` and `12c` carry the same `0.1.0-SNAPSHOT`, the same
> `finalName`, and the same interpolated `paper-plugin.yml` version.**
>
> **So neither the jar's NAME nor the version Paper prints at startup can discriminate between
> them.** A wrong-build boot produces a `Jar OK` line and a plugin-enabled line that are
> byte-identical to a right-build boot.
>
> **It is the passing twin of this row's own autopsy**: 12b lost a boot to a jar whose mtime was
> current and whose every staleness check cleared it. *A control that succeeds for the wrong reason
> needs no explanation, because nothing looks wrong.* **Only the class-file probe in this row
> discriminates**, which is why it is the row and the script's line is not.

---

## The two rows that cost something, and are sole witnesses

### R1 — Two scores of one definition deal damage in the ratio of their scores

**Sole witness. No test in any module can construct an `ItemStack`, so this claim exists nowhere
else.** The unit suite asserts the *arithmetic* (`GearScoreTest.twoScoresOfOneDefinitionDealDamageInTheRatioOfTheirScores`);
only a boot can show the arithmetic actually reaching a hit.

**Staged at 175 and 340. NOT at 100 and 400, and the departure is deliberate.**

> Ben's ruling states the claim as *"a GS 100 weapon and a GS 400 weapon … in exactly the ratio
> 100:400"*, and **100:400 is a clean 4x** — which is satisfied by a defect that doubles twice, by one
> that squares the ratio of the *hundreds digits*, and by several other wrong arithmetics. `175:340`
> is satisfied by the rule and by nothing convenient: neither is a multiple of the other, neither is a
> multiple of the baseline, and `340/175` is not a ratio any plausible bug produces by accident. This
> is CLAUDE.md's collision rule applied to a gate row's staging rather than to a unit fixture.

| | |
|---|---|
| **Setup** | `/rpg give boltor` twice. `/rpg gearscore set 175` on the first, `set 340` on the second. Same mob, same element, no enchants on either (a fresh give may roll candidates — confirm with `/rpg enchant show` that neither has an ACTIVE damage enchant, or the ratio is measuring Sharpness). |
| **Predict** | Boltor authors `attack_damage: 19`. The 175 bolt deals **33.25**; the 340 bolt deals **64.6**. Cross-multiplied: `33.25 x 340 = 11305 = 64.6 x 175`. |
| **Predict** | The tooltips read `Gear Score: 175` and `Gear Score: 340` BEFORE either is fired — the number is readable off the screen ahead of the hit, which is what makes this falsifiable rather than a post-hoc reading. |
| **READ** | **PASS** — 2026-09-20, booted by Ben on `4648283`. Confirmed against the prediction; figure not recorded (Ben's ruling). |

### R2 — An item minted before this slice deals exactly the damage it dealt yesterday

**Sole witness, and NOTHING in the suite can see it.** `GearScoreTest.anUnstampedItemScalesNothing`
asserts that `ABSENT` is the identity *in the arithmetic*; it cannot assert that a real unstamped
`ItemStack` reaches that arithmetic at all. This is the absent-equals-100 claim, and it is the whole
justification for shipping with **no stamp, no migration and no schema bump**.

> ### *** THE POPULATION THIS ROW DEFENDS DOES NOT EXIST. RECORDED 2026-09-21. ***
>
> This row's stated rationale is that *every item predating the system is unchanged*. **Ben has
> ruled there are ZERO deployed versions of this plugin**, so there is no population of pre-system
> items and **there never was one.** The row was written to defend nobody.
>
> **`GearScore.ABSENT` DOES NOT CHANGE, AND THAT IS NOT A CONCESSION.** It is a sound identity
> default on its own terms — an unstamped item genuinely *is* a baseline item, which is why absence
> has a correct reading rather than a missing one. What is withdrawn is the *migration* argument,
> not the constant.
>
> **Why one line is worth spending here:** the sentence above reads as a compatibility guarantee,
> and a reader six weeks out would cite it as load-bearing — then hesitate to touch `ABSENT`
> because "legacy items depend on it". **Nothing depends on it.** The `clear` path in the Setup
> cell is the only way to manufacture the state this row reads, and it says so.
>
> The row is **kept and still passing**: `clear` is a real command, the three predictions are real
> claims about it, and R2 is the only witness that an unstamped stack reaches the identity at all.
> **What it is not is a bridge from an older build.**

| | |
|---|---|
| **Setup** | `/rpg give emberblade`, then **`/rpg gearscore clear`** on it. That is the only way to manufacture a legacy item: nothing mints unstamped gear any more, so without `clear` this row would need an inventory saved from before the build. |
| **Predict** | `/rpg gearscore show` reports `Held: no stamp (reads 100)` — **not** `Held: 100`. The two are reported differently on purpose; if they read alike this row is indistinguishable from one staged on a genuine 100. |
| **Predict** | The tooltip carries **NO** `Gear Score:` line at all. A legacy item is quiet, not labelled 100. |
| **Predict** | It deals emberblade's authored **7**, exactly — the same number it dealt before this slice existed. |
| **READ** | **PASS** — 2026-09-20, booted by Ben on `4648283`. Confirmed against the prediction; figure not recorded (Ben's ruling). **NARROWED, NOT VOIDED, 2026-09-20:** the tooltip prediction detects a build that renders an ABSENT score as `100` — that mutation puts a line on the tooltip and reddens this row. It is blind only to `clear` FAILING TO RE-RENDER, because `/rpg give` left no line for `clear` to remove. **A real guard over a smaller claim than it reads.** The other two predictions are unaffected. Account below R12. |

---

## The average

### R3 — Six slots, with slots deliberately empty

**The row that proves the denominator is 6 and not "however many are filled".**

| | |
|---|---|
| **Setup** | Strip to nothing. Equip ONE piece of our armour, `/rpg gearscore set 400` on it. Both hands empty. |
| **Predict** | `/rpg gearscore show` reports `Slots: [400, 0, 0, 0, 0, 0]` and `Gear Score: 66` (`400 / 6 = 66.67`, floored). |
| **Predict** | **If it reports 400, empty slots are being skipped** and the denominator has become the filled count. That is the `MUT12-SKIPEMPTY` defect, in the field. |
| **READ** | _(NOT RUN — out of 12b's boot scope. Its claim is arithmetic and `GearScoreTest` executes it.)_ |

### R4 — The top two across hotbar AND offhand together

**Staged so that a wrong rule gives a DIFFERENT number, which took one attempt to get right.**

> **THE FIRST STAGING OF THIS ROW WAS HOLLOW AND IS RECORDED RATHER THAN QUIETLY REPLACED.** It put
> the offhand at **400** and the hotbar at 250 and 175. Under the correct rule the top two are
> `[400, 250]`; under the wrong rule — *the offhand gets a guaranteed seat and the hotbar contributes
> its best* — they are **also** `[400, 250]`. **Identical answer, so the row measured the fixture.**
> That is CLAUDE.md's *control that succeeds for the wrong reason*, caught by asking what the row
> reports if the rule it checks is deleted.
>
> **The fix is to make the OFFHAND the LOWEST of the three**, so a guaranteed seat displaces a
> higher hotbar item and the average moves.

| | |
|---|---|
| **Setup** | No armour. Hotbar: two weapons at `set 400` and `set 250`. Offhand: a shield at `set 175`. |
| **Predict** | One pool, top two: `[400, 250]` → `Slots: [0, 0, 0, 0, 400, 250]`, `Gear Score: 108` (`650 / 6`). |
| **Predict** | **If it reports 95**, the offhand is being given a slot of its own (`400 + 175 = 575 / 6`) instead of competing in one pool. |
| **READ** | _(NOT RUN — out of 12b's boot scope. Its claim is arithmetic and `GearScoreTest` executes it.)_ |

### R5 — A tool in the hotbar does not enter the top two

**Ben's ruling: a pickaxe must never become one of the top two and raise a drop level without
contributing anything to a fight.**

> **Staged so the two answers differ, for R4's reason.** A pickaxe reads `ABSENT` (100) *if it is
> admitted at all*, so pairing it with a 340 and a 250 weapon would be hollow — 100 loses the top-two
> contest anyway and the row would pass either way. **One scored weapon only**, so an admitted
> pickaxe actually takes the second seat.

| | |
|---|---|
| **Setup** | No armour, offhand empty. Hotbar: ONE weapon at `set 175`, plus a minted tool (`/rpg give` any pickaxe). |
| **Predict** | `Hand pool: [175]` — the tool is **absent from the pool entirely**, not present as a 0. `Gear Score: 29` (`175 / 6 = 29.17`). |
| **Predict** | **If it reports 45**, the tool was admitted and read as a baseline item (`275 / 6 = 45.83`). That is `MUT12-TOOLID` in the field. |
| **Predict** | `/rpg gearscore set 300` **on the held pickaxe** still leaves it out of the pool on the next `show`: the stamp path refuses tools through `GearScore.scoreable`, so the write lands on the PDC and the read never counts it. **Two independent refusals, and this predicts both.** |
| **READ** | _(NOT RUN — out of 12b's boot scope. Its claim is arithmetic and `GearScoreTest` executes it.)_ |

---

## The roll

### R6 — A drop bands on the average and never exceeds 400

| | |
|---|---|
| **Setup** | Equip four armour pieces and two weapons, all at `set 400`. Confirm `Gear Score: 400`. Then `/rpg give` a fresh weapon and read its score. |
| **Predict** | The unclamped band at a 400 average is `395..415`. The fresh weapon rolls somewhere in **395..400** — never 401, never 415, never 500. The soft cap truncates the top half of the band, which is exactly what `clampDrop` is for. |
| **Predict** | `show` reports `Next drop rolls in: 395..400` — **both ends printed already clamped**, so the cap is visible in the readout before a single drop is taken. |
| **Predict** | With everything stripped instead, the average is 0, the unclamped band is `-5..15`, and a fresh `/rpg give` rolls **exactly 100** — `show` reports `100..100`. The whole band is under the floor. |
| **READ** | _(NOT RUN — out of 12b's boot scope. Its claim is arithmetic and `GearScoreTest` executes it.)_ |

### R6b — No drop falls outside the reported band, and the draw is not being ignored

> ### *** RETITLED 2026-09-20, AFTER IT WAS READ, TO THE CLAIM IT ACTUALLY EARNED ***
>
> **It read: *"A drop rolls somewhere INSIDE the band, and the band is 21 values wide."* The width
> half is NOT WITNESSED, and this row cannot witness it.**
>
> **Twenty draws landing inside `245..265` is satisfied by a NARROWER band.** Nothing in the reading
> distinguishes a 21-wide band from a 9-wide one sitting inside it — only the observed minimum and
> maximum could, and readings here are PASS/FAIL with no figures recorded. **And the two halves are
> not independent:** `show`'s reported range and the draws themselves both read the same `SPREAD`
> constant, so **they agree with each other whether or not either is right.**
>
> **What the row DID earn, and it is a real pass:** no draw fell outside the band the plugin itself
> reported, and the twenty were not all the same number — which is the defect a single-drop reading
> cannot see, and the reason the row takes twenty.
>
> **Do not cite R6b for the band's width.** The prediction below is UNEDITED, per this file's own
> rule; it is the TITLE that was overreaching, and a row retitled to its evidence is worth more than
> one whose title outruns it.

**STAGEABLE AS OF BEN'S RULING.** This row was OWED and unstageable in the first draft of this gate;
with SPREAD 10 and SKEW 5 there is now an inside to land in.

**Staged at a 250 average, deliberately: both clamps are far away**, so the endpoints observed are
the BAND's and not the floor's or the cap's. At 400 the cap supplies the top and the row would be
measuring `clampDrop` again — which R6 already does.

| | |
|---|---|
| **Setup** | Six slots all at `set 250` — four armour, two hands. Confirm `Gear Score: 250`. Then `/rpg give boltor` **twenty times**, reading each score. |
| **Predict** | `show` reports `Next drop rolls in: 245..265`. |
| **Predict** | Every one of the twenty lands in **245..265 inclusive**. None below 245, none above 265. |
| **Predict** | **They are NOT all the same number.** A band 21 values wide over twenty draws that all agreed would mean the draw is being ignored — the defect a single-drop reading cannot see, which is why this row takes twenty. |
| **Predict** | Their mean sits **above 250**, because the skew is +5 and not 0. A mean at 250 means the band is centred and the ladder converges instead of climbing. *(Twenty draws is a small sample — a mean landing a little either side of 255 is noise; a mean at or below 250 is not.)* |
| **READ** | **PASS** — 2026-09-20, booted by Ben on `4648283`. **Containment only, see the note above the table: no draw fell outside the reported band and the draws were not all equal. WIDTH IS NOT WITNESSED.** Figures not recorded (Ben's ruling). |

---

## The trap rows

### R7 — The armour bar is not empty on a high-scored set

**`nativeArmor` must accumulate the UNSCALED points.** Feed it the scaled figure and
`ArmorBarOverride` over-subtracts, the vanilla attribute lands negative, Minecraft clamps it to zero,
and **the bar reads EMPTY on the most-armoured player in the game** — while the stat, the mitigation
and the tooltip all stay correct. Nothing throws, and no unit test in any module can see it.

**SURVIVAL ONLY, AND THIS ROW IS VOID IN CREATIVE** — creative hides the armour bar outright, so the
reading cannot be taken there at all.

| | |
|---|---|
| **Setup** | Full diamond set, all four ours, each at `set 400`. |
| **Predict** | The armour bar is **visibly populated**, not empty. |
| **Predict** | Each piece's tooltip shows a Defense figure 4x its material's points — a diamond chestplate reads **32** (`8 x 4`), a helmet **12** (`3 x 4`). |
| **Predict** | `/rpg stats` Defense is the sum of the four scaled figures, and the tooltips add up to it. |
| **READ** | _(NOT RUN — out of 12b's boot scope. Its claim is arithmetic and `GearScoreTest` executes it.)_ |

### R8 — A relog does not downgrade anything

**A re-mint happens on every join, and losing the stamp there reverts every scored item to 100 —
silently, to a legal value, with the whole suite green.**

| | |
|---|---|
| **Setup** | Hold and wear scored gear from R7. `/stop`, reboot, rejoin. |
| **Predict** | Every score is **unchanged**. No tooltip loses its `Gear Score:` line and the average is the same number as before the restart. |
| **Predict** | `/rpg refresh` also leaves every score unchanged — it re-mints deliberately, which is the same path. |
| **READ** | _(NOT RUN — out of 12b's boot scope. Its claim is arithmetic and `GearScoreTest` executes it.)_ |

### R9 — CONTROL: the vault, the hub and the enchant table still behave

**The control, and it is not a formality: this slice widened three lore builders, the one carry
method, the reconcile loop and the Brigadier tree.**

| | |
|---|---|
| **Setup** | Open `/menu`. Open the vault screen. Enchant something at a real table. `/rpg vault dump`. |
| **Predict** | The hub paints every slot as before, with **one added line** on the stats head: `Gear Score` between the progression block and the eight combat stats. |
| **Predict** | The enchant table still rolls, still charges, and an enchanted item **keeps its gear score** across the table's re-mint. Protection still composes into the Defense line — **and the Defense line now shows the SCALED base with the UNSCALED bonus added on top**. |
| **Predict** | The vault still round-trips an item with its PDC intact, **score included**. |
| **Predict** | *(Creative note: `MenuRouting` refuses `CLONE_STACK`, unchanged by this slice.)* |
| **READ** | _(NOT RUN — out of 12b's boot scope. Its claim is arithmetic and `GearScoreTest` executes it.)_ |

---

## Mutations — applied and measured, no kill-set predictions

Run before this file was written, on the tree at the slice's own HEAD. **Every one was applied to a
scratchpad-backed copy (`cp`, never `git checkout --`), verified with both halves of the marker grep
where the marker was distinct from the original, verified with a LINE DELTA AGAINST THE PRISTINE COPY
in every case, and restored with a byte-identity check.** Full-tree marker sweep afterwards: **0**.

| mutation | what it changed | reddened |
|---|---|---|
| `MUT12-SOFTCAP` | `clampDrop` ceiling `SOFT_CAP` → `HARD_CAP` | **2** — `aRollNeverExceedsTheSoftCap`, `theDropClampStopsAtTheSOFTCapWhereTheReadClampWouldNot` |
| `MUT12-ABSENT` | `orAbsent` returns `EMPTY` instead of `ABSENT` | **1** — `anUnstampedItemReadsAsTheBaselineRatherThanZero` |
| `MUT12-TOPONE` | `topTwo` loop bound `HAND_SLOTS` → `1` | **2** — `theTwoHighestHandsCountAndTheRestDoNot`, `theSixSlotsAreFourArmourThenTheTwoHighestHands` |
| `MUT12-TOOLS` | `scoreable`: `case TOOL -> false` → `true` | **2** — `weaponsArmourAndShieldsScoreAndToolsDoNot`, `everyGearClassIsAnsweredWithoutADefaultArm` |
| `MUT12-SKIPEMPTY` | `averageOf` skips empties and divides by the filled count | **2** — `aNewPlayerHoldingOneBaselineSwordAveragesSixteen`, `theAverageIsOverAllSixSlotsWithEmptiesCounted` |
| `MUT12-NOCARRY` | `GearScoreItems.carry` **deleted** from `carryInstanceData` | **1** — `theGearScoreIsCARRIEDAcrossAReMint` |
| `MUT12-TOOLID` | `candidateScore` also reads `keys.toolId` | **1** — `theAverageReadsTheThreeScoreableTagsAndNotToolId` |
| `MUT12-NATIVEORDER` | `nativeArmor += vanilla` moved BELOW the scaled declaration | **1** — `theNativeArmourSumIsTheUNSCALEDOneAndAccumulatesFirst` |
| `MUT12-NATIVEVALUE` | the same accumulate changed to `+= scaled` | **1** — same row, **but on a DIFFERENT assertion** (see below) |
| `MUT12-NOSCALE` | the MAIN_HAND modifier reverted to the authored `attackDamage()` | **1** — `weaponDamageIsSCALEDByTheHeldItemsScore` |

### Three things the mutation pass found, which are the reason it was run

**`MUT12-NOCARRY` IS A DELETION, SO THE MARKER GREP DOES NOT APPLY TO IT.** The replacement text is
empty, which makes both halves of the grep uninterpretable rather than merely wrong — CLAUDE.md's
eighth mutation-lie. **The line delta was the whole verification**: `-1`, with the deleted line read
back rather than counted.

**`MUT12-NATIVEVALUE` COULD NOT BE ISOLATED TO ITS OWN AXIS, AND THE ROW'S SECOND ASSERTION IS
THEREFORE UNWITNESSED.** `scaled` is declared *below* the accumulate in the correct ordering, so
`nativeArmor += scaled` **does not compile** there; the only mutation that compiles moves the
accumulate down first, and the row then dies on the FIRST assertion instead. Measured: it failed on
*"the native sum must still accumulate the raw vanilla points"*, and the loop banning
`nativeArmor += scaled` **was never reached**. That ban is kept — it becomes reachable exactly when
someone hoists the declaration, which is the tidy-up it exists to catch — and it is **named as
unwitnessed in the test's own javadoc** so nobody mistakes it for a verified guard.

**A MARKER GREP LIED DURING THIS SLICE, AND IT IS WHY THE SIGNATURE TEST FILTERS COMMENTS.** A grep
for `OptionalInt averageScore) {` reported a signature widening as applied. It had matched a
**different method's parameter list**; the widening had silently no-opped, because a `{\n`-adjacent
pattern **cannot match on a CRLF working tree** — `\r` sits between the brace and the newline. Caught
by *printing the region* instead of trusting the grep. Both failures are recorded in
`GearScoreWiringSignatureTest`'s class javadoc.

### What the mutations do NOT cover, stated so the gap is not rediscovered

`GearScore.BASELINE`, `MIN` and `ABSENT` are **all 100**, and they are three unrelated quantities —
the divisor, the floor, and what an unstamped item reads as. **No mutation can distinguish them and
no row in the suite can either**; swapping any two leaves everything green. Naming all three is the
only protection available, and `GearScore`'s class javadoc says so at the top.

---

## SEVEN of thirteen weapons do not scale — and Ben has RULED that trigger damage does

**The first draft of this section said THREE, and it was narrower than the truth.** It named the three
staves and stopped, because those were the weapons whose damage I had traced. Re-measured across all
thirteen weapon files:

| | weapons | `attack_damage` |
|---|---|---|
| **SCALE (6)** | boltor 19 · dragons_plume 34 · locust 26 · ironblade 8 · emberblade 7 · hunters_bow 6 | authored, positive |
| **DO NOT (4)** | ember_staff · flint_staff · lapis_staff · ability_stone | **no key at all** |
| **DO NOT (3)** | cursed_emerald · quiver_stone · volley_stone | **authored `0`** |

Instrument: `grep -c '^attack_damage:'` and `grep -m1 '^attack_damage:'` over each of the thirteen
files, anchored at column 0 so a prose mention cannot be counted as the key. PERISHABLE — the next
weapon that ships changes both the counts and the lists.

**The "six scale" figure was exact. The complement was not**, and the two failures are different:
four weapons author no `attack_damage` key, and three author it as `0`. **`cursed_emerald`'s `0` is
not a bug** — it is authored deliberately, and its own file says so: there is no basic attack on that
weapon, the volley is the whole thing, and that is a choice. Its damage is six shots of 27 in
`on_hit`.

### Ben has ruled: TRIGGER DAMAGE SCALES. That is slice 12b, not a patch to this one.

**Four of the seven become scaling weapons under that ruling** — the three staves and
`cursed_emerald`. **It is a SLICE and not an amendment**, because it is a second scaling site in a
different subsystem: `EffectApplier`'s damage path, which already threads `enchantDamagePercent`,
`classDamageBonus`, `chargeScale` and `critMultiplier`. Bolting a fifth factor onto a branch that is
built, measured and verified is how one clean slice becomes two half-slices. **12b comes off 12's
squash.**

> **THE OLD REPO ALREADY DID THIS, AND THE FILE SAYS SO:** `GearScore.scalePower(42, level)` — with
> the note *"no item levels here, which is why 27 had to be re-decided by the operator rather than
> ported"*. **42 was the pre-scaling authored value; 27 is what it became with nothing to scale it.**
> So this ruling reconnects the weapon to the system it was written against.
> 
> **The 27 is to be RE-EXAMINED against 42 when scaling lands — not silently reverted.** The operator
> ruled 27 on its own terms, and a revert would be a derivation replacing a ruling, which is the
> descent defect `GearScoreBand` names.

### THE THREE STONES STAY SCORED — RULED 2026-09-21. THEY ARE DEV WEAPONS ON THEIR WAY OUT.

**`ability_stone`, `quiver_stone` and `volley_stone` remain unscaling after the trigger ruling** —
and all three are in the WEAPONS registry, so all three **carry a gear score and can occupy one of
the two hand seats.** A high-rolled stone therefore raises the level of every drop the player takes
while contributing nothing to a fight.

**That is precisely the exploit Ben closed for TOOLS, and the tools ruling does not reach items in
the weapons registry** — `GearScore.scoreable` gates on `GearClass`, and all three stones present a
fighting class (`volley_stone`'s own file calls it **a test instrument**; `ability_stone`'s says its
`class: mage` is arbitrary — *"a dev tool; class is required, mage is as good as any here"*).

**PUT IT TO BEN IN 12b. DO NOT EXCLUDE THEM ON MY OWN AUTHORITY.** An exclusion invented here would
be a ruling with no author, which is the failure the *unruled, not excluded* rule exists to stop.

> ### *** IT WAS PUT TO HIM AND HE RULED. 2026-09-21: LEAVE THEM SCORED. ***
>
> **Ben's words: they are dev weapons on their way out.** The exploit is real and is accepted for as
> long as the stones exist, because the fix is deletion rather than an exclusion — and an exclusion
> added now would be a rule outliving the three items it was written for.
>
> **The heading above is rewritten and the body is kept**, so the next reader finds the argument
> *and* the answer. This question has been raised twice; without the ruling written in, it would be
> raised a third time.
>
> ### AND THE RULING LEANS ON SOMETHING THAT IS NOT QUEUED ANYWHERE — BEN'S CALL, NOT MINE
>
> **`ability_stone` is NOT in the dev-weapon deletion set.** That set names exactly three, measured
> in `CLAUDE.md`'s *Standing decisions*: **`hunters_bow`, `ironblade` and `quiver_stone`.**
> `volley_stone` is separately excluded by content (`unscored: true`), so it does not depend on this
> ruling at all.
>
> **So "on their way out" is true of `quiver_stone`, already true by another mechanism for
> `volley_stone`, and QUEUED NOWHERE for `ability_stone`.** Its removal is not scheduled, not
> triggered and not written down — which means the ruling's premise holds for two of the three
> stones and is an assumption for the third.
>
> **TWO WAYS TO CLOSE IT, AND CHOOSING BETWEEN THEM IS BEN'S:**
>
> 1. **Add `ability_stone` to the deletion set**, and the premise becomes true of all three.
> 2. **Record that the ruling stands on its own merits** — that a scored dev stone is acceptable
>    regardless of when or whether it is deleted — and the premise stops being load-bearing.
>
> **Neither is chosen here.** Writing either one in would be inventing the second ruling to prop up
> the first, which is the same defect as an exclusion with no author, one level up.

---

# SLICE 12b — TRIGGER DAMAGE SCALES. ROWS TO FOLLOW.

**Status: NOT RUN.** Ben ruled on 2026-09-19 that trigger damage scales everywhere it appears, and
that **`volley_stone` carries no gear score at all** — a content declaration, not an id check.

## *** DISCHARGED: THE volley_stone EXCLUSION IS WIRED. ALL SIX ITEMS LANDED. ***

**This block was opened mid-slice, while the exclusion was half-built, and it is discharged rather
than deleted — a list that only ever loses rows cannot be told from one nobody is maintaining.**

**What it recorded while it was open**, and it was true for several commits:
`PaperCombatWorld.triggerScoreOf` called `heldScore(player, keys)` with no weapon registry, so it
could not consult a definition — and **`volley_stone`'s two `amount: 4` payloads scaled with the
holder's gear score like every other trigger weapon.** Nothing was red, nothing was missing from a
diff, and a reader opening `triggerScoreOf` saw a correct-looking method. **That is why an
INCOMPLETENESS was written into the repo rather than left in a conversation: it is exactly as
transcript-fragile as a finding, and harder to spot.**

**All six, each verified present rather than assumed:**

| # | item | landed as |
|---|---|---|
| 1 | `boolean unscored` on `WeaponDefinition`; `unscored: true` in `volley_stone.yml` | the record's 15th component, and the YAML declaration with its own note |
| 2 | the composed predicate, kind AND instance, asked once | `GearScore.carriesScore(GearClass, boolean)` |
| 3 | `triggerScoreOf` refusing **before** the `heldScore` read | the door is inside `heldScore` itself, ahead of `orAbsent` |
| 4 | `candidateScore` returning `OptionalInt.empty()`, **not** `EMPTY`/0 | so the stone is not a candidate for the top-two pool at all |
| 5 | `stampOnAcquire` widened to carry the declaration | takes a `GearDefinition`; both facts from one object, no boolean to default |
| 6 | `GearScore.scoreable(GearClass)` made **package-private**, in the same commit as 5 | `paper` can no longer ask the kind-level question alone |

> **ITEM 6 WAS PROVED, NOT CLAIMED.** A deliberate `GearScore.scoreable(null)` was inserted into
> `WeaponAttackItems` and the compiler refused it — *"scoreable(GearClass) is not public in
> GearScore; cannot be accessed from outside package"* — then removed byte-identical. **A
> compile-enforced rule has no test to redden, so its positive control is a violation that must fail
> to build.**

**AND THE STAMP REFUSES AS WELL AS THE READ, WHICH WAS THE POINT OF 5 AND 6 SHIPPING TOGETHER.** Had
5 landed alone, a freshly minted `volley_stone` would still have taken a score in its PDC while only
the read refused — **two independent refusals collapsing into one, silently.**

> ***AND THAT COLLAPSE WAS MEASURED, NOT FEARED.*** `MUT12B-STAMPFALSE` — the stamp passing a
> hardcoded `false` — was applied to the finished tree and **the entire suite stayed GREEN at 2070
> tests, 0 failures.** The average stays correct under it because `candidateScore` refuses
> independently; only the bytes in a minted stone's PDC differ, and no module can construct an
> `ItemStack` to look. A signature row now bans the literal and reddens under the same mutation, and
> **R14 is the sole behavioural witness.**

## *** OWED TO THE PR BODY. NOT BLOCKERS, AND NOT OMISSIONS EITHER. ***

**Recorded here because they were asked for twice in conversation and did not reach the repo, which
is the exact condition this project rules against.** Neither gates the code; both must appear in the
PR body before it can be called complete.

### (a) THE 52.5 / 47.5 FIGURES — an arithmetic gap in a slice about arithmetic, so it is a DEFECT entry

`MUT12BORDER` reported `expected: <55.0> but was: <47.5>`, and the report of it said *"the predicted
52.5"* — three figures, with no statement of which was which.

**RESOLVED, AND THE RESOLUTION IS THAT THERE WAS NO GAP: `52.5` is DAMAGE, `47.5` is HEALTH.**

```
correct   16 * 250/100 = 40      40 + 5 = 45        health 100 - 45   = 55.0    <- expected
mutant    hitBase(16,0,5) = 21   21 * 250/100 = 52.5  health 100 - 52.5 = 47.5  <- actual
```

**The mutant landed exactly where predicted; the model of the chain was off by nothing.** The defect
was a report quoting two figures from different UNITS in one sentence without labelling either —
this file's own *a fraction must say what unit each side counts* rule, applied to a mutation reading.
**It belongs in the defects section rather than being dropped**, because an unexplained arithmetic
gap in an ordering slice is indistinguishable from an ordering defect until someone checks.

### (b) THE FOUR JOINTS — MEASURED, and the answer is a NAMED FINDING rather than a coverage gap

`HitDamage.hitBase` threads four factors. `MUT12BORDER` guards the score's position against exactly
one of them. **Does any row stage the other three non-neutral?**

**Measured in `EffectApplierTest`:** `enchantDamagePercent` **8**, `classDamageBonus` **11**,
`critMultiplier` **4**, `chargeScale` **1** — all four are staged non-neutral somewhere. **Rows
staging a score ALONGSIDE another non-neutral factor: 1** — `theScoreScalesTheLiteralBeforeTheClass
BonusIsAdded`, and it is new in this slice.

***BUT THE COVERAGE QUESTION IS THE WRONG ONE, BECAUSE ONLY ONE JOINT EXISTS.*** The chain is

```
((base * M) + B) * C * R        M enchant   B class bonus   C charge   R crit
```

**`B` is the only ADDEND.** The score's position relative to a MULTIPLIER is not a distinguishable
position at all. Measured rather than reasoned — a sweep of **5040** combinations of realistic bases,
scores, percents, charges and crits:

| | |
|---|---|
| combinations where crossing a multiplier changes the double | **926 / 5040** |
| max ABSOLUTE difference | **1.137e-13** |
| max RELATIVE difference | **2.114e-16** |
| the suite's assertion delta | `1e-9` |

**So the differences are real and four orders of magnitude below anything a row could assert.**

***THE FINDING: ONE OF ONE REAL JOINTS IS GUARDED. THE OTHER THREE ARE UNGUARDABLE, NOT
UNGUARDED.*** Stated in those words on purpose — **the danger is a later reader "fixing" the gap
with three rows that cannot fail**, which is this file's own dead-guard shape. The instinct that
multiplication commutes is also wrong here (926 cases differ bitwise); it is the MAGNITUDE that
settles it, not the algebra, and only execution shows that.

---

## THE ROWS — R10 to R15. Status: NOT RUN, every prediction written before any boot.

### *** THE BOOT SCOPE IS TEN ROWS, AND IT IS STATED SO IT CANNOT DRIFT BY ADDITION ***

**READ IN THIS SLICE — ELEVEN:** `R0 · R1 · R2 · R6b · R10 · R10b · R11 · R12 · R13 · R14 · R15`

**LEFT UNREAD — SEVEN, with the reason:** `R3 · R4 · R5 · R6 · R7 · R8 · R9`. **This said SIX and omitted R6 until 2026-09-20, in the boot sheet and in this file both; the count is now taken from the table (10 read + 7 unread = 17 rows) rather than from a list somebody typed.** Their claims are arithmetic
and `GearScoreTest` executes them at the two-second loop; they buy redundancy, where the ten above
buy the only evidence that exists.

> **THE FIRST THREE ARE SLICE 12's, AND THEY ARE HERE BECAUSE SLICE 12 MERGED WITH ITS ENTIRE GATE
> BLOCK UNBOOTED.** Ten `READ` cells, all empty, on code that has been live on `master` since
> `1c030e2`. **That was an oversight, not a decision** — the file existing read as the gate being
> handled. **R1** (the ratio) and **R2** (absent = 100) are slice 12's only sole witnesses and
> nothing in any module can see either; **R6b** is different again — it was OWED and UNSTAGEABLE
> while the band was unruled, because a zero-width band has no inside to land in, so it was
> **blocked rather than skipped** and is newly readable now that SPREAD and SKEW are ruled.
>
> **The count moved from seven to ELEVEN during this slice** — `R10b` split the GS-100 identity out of
> `R10`, and `R14` was added when a review found that every planned row was READ-side and the
> write-side refusal had no behavioural witness at all. **Both additions are recorded here rather
> than left to be inferred from the row list**, because a scope that grows silently is
> indistinguishable from one nobody agreed.

**GAME MODE: SURVIVAL**, per this file's header. No row below is satisfied for free by a removed
cost — scaling multiplies a number rather than spending anything — and none reads an armour bar.

### *** THE STAGING DEPARTS FROM THE 12b BRIEF, AND THIS FILE'S OWN R1 IS WHY ***

The brief states the claim as *"a GS 100 and a GS 400 cursed emerald ... in exactly the ratio
100:400"*. **R1 above already refused that staging for slice 12 and gave the reason: 100:400 is a
clean 4x, satisfied by a defect that doubles twice, by one that squares the ratio of the hundreds
digits, and by several other wrong arithmetics.** Restaging it here would reintroduce a weakness
this file has already ruled against, two hundred lines below the ruling.

**So R10 is staged at 175 and 340, exactly as R1 is — and the GS 100 identity, which the brief's
staging DOES buy, is not lost: it gets its own row (R10b) where it can fail on its own.** One row,
one claim.

---

### R10 — Trigger damage scales, and the ratio is the scores' ratio

**SOLE WITNESS.** `EffectApplierTest` asserts the arithmetic at the two-second loop; **no test in any
module can construct an `ItemStack`**, so nothing but a boot shows a real score reaching a real
trigger payload. This is the whole of Ben's *trigger damage scales* ruling, observed.

**`cursed_emerald` on purpose: its damage is SIX shots of an authored 27 through `on_hit`, and it
declares `attack_damage: 0`.** A weapon with a real `attack_damage` could not distinguish this from
slice 12's already-shipped attack-path scaling.

| | |
|---|---|
| **Setup** | `/rpg give cursed_emerald` twice. `/rpg gearscore set 175` on the first, `set 340` on the second. Confirm with `/rpg enchant show` that neither carries an ACTIVE damage enchant, or the ratio is measuring Attunement. Same mob for both, full mana. |
| **Predict** | Authored 27. The 175 emerald deals **47.25** per shot; the 340 emerald deals **91.8** per shot. Cross-multiplied: `47.25 x 340 = 16065 = 91.8 x 175`. |
| **Predict** | **SIX shots each, counted on screen.** Six is the fixture: a build that scaled the volley's shot COUNT instead of its damage would show the same total and a different count. |
| **Predict** | Both tooltips read their `Gear Score:` line BEFORE either is fired, so the row is falsifiable ahead of the hit rather than after it. |
| **READ** | **PASS** — 2026-09-20, booted by Ben on `4648283`. Confirmed against the prediction; figure not recorded (Ben's ruling). |

### R10b — At GS 100 the emerald deals exactly its authored 27

**The IDENTITY, which is the whole reason this slice needs no content re-tuning.** Separated from R10
because it fails for a different reason: R10 catches a wrong RATIO, this catches a scale applied
where none was asked for.

| | |
|---|---|
| **Setup** | `/rpg give cursed_emerald`, `/rpg gearscore set 100`. |
| **Predict** | Each of the six shots deals **exactly 27** — the number the weapon dealt before 12b existed. |
| **Predict** | **If it reads anything else, GS 100 is not the identity** and every content number in the game has silently changed meaning. |
| **READ** | **PASS** — 2026-09-20, booted by Ben on `4648283`. Confirmed against the prediction; figure not recorded (Ben's ruling). |

### R11 — A staff scales too, through a DIFFERENT trigger shape

**Not a duplicate of R10.** `cursed_emerald` is a `volley` of direct `on_hit` damage;
`ember_staff` authors its 16 inside a **`burst`** nested under `on_hit`. Those are different paths to
`EffectSpec.Damage`, and a build that scaled one and not the other passes R10.

| | |
|---|---|
| **Setup** | `/rpg give ember_staff` twice, `set 175` and `set 340`. |
| **Predict** | Authored 16 → **28** and **54.4**. Cross-multiplied: `28 x 340 = 9520 = 54.4 x 175`. |
| **READ** | **PASS** — 2026-09-20, booted by Ben on `4648283`. Confirmed against the prediction; figure not recorded (Ben's ruling). |

---

## The volley_stone exclusion — three rows, and they are three because they fail independently

### R12 — A volley_stone shows NO gear score line, and its damage does not move between two players

**READ side, BOTH HALVES, and they are recorded as two readings in one row on purpose: the tooltip
alone passes on a build that HIDES THE LINE AND SCALES ANYWAY.**

| | |
|---|---|
| **Setup** | Two players at clearly different averages — one stripped (average near 0), one in `set 400` armour. `/rpg give volley_stone` to each. |
| **Predict** | **(i)** Neither tooltip carries a `Gear Score:` line at all. |
| **Predict** | **(ii)** Both stones deal the **same** damage: `amount: 4` per shot, 3 shots on right-click and 8 on left-click. **Count the shots as well as the number** — the authored 4 is a legibility figure and the count is what makes it readable. |
| **Predict** | **If (i) passes and (ii) fails, the line is hidden and the scaling is live** — which is the precise defect this row is split to catch. |
| **READ** | **(i) VOID — the reading is WITHDRAWN, 2026-09-20. (ii) PASS** — booted by Ben on `4648283`, confirmed against the prediction; figures not recorded (Ben's ruling). **(i) could not have failed:** `stampOnAcquire` writes the PDC and nothing re-renders, so NO freshly given weapon of any kind carries a `Gear Score:` line — (i) passes identically on a build with no exclusion at all. **It measured the give path, not the exclusion.** Account immediately below. (ii) is untouched and stands. |

## *** ONE READING WITHDRAWN — R12(i), AND THE FAMILY ENTRY IT EARNED ***

**R12(i) carried a PASS at `6c4a563` that it could not have failed.** `GearScoreItems.stampOnAcquire`
writes the score into the PDC and **nothing re-renders** — `mint` has already run, against a
still-empty container. `RpgCommand.java:1357-1368` is mint → roll → stamp → `addItem` with no refresh;
the kit grant (`:2164`) and `InventoryCraft.java:320` are the same three steps. So **no freshly given
weapon of any kind carries a `Gear Score:` line**, and R12(i) reads identically on a build with the
exclusion deleted outright. **It measured the give path, not the exclusion.**

**THE SWEEP SHIPS WITH THE PATTERN THAT PRODUCED IT.** Every `**Predict**` cell in this file touching a
tooltip score line, classified by whether its staging RE-MINTS:

```
grep -nE '^\| \*\*(Predict|Setup)\*\*' GATE-gearscore.md | grep -iE 'tooltip|gear score|score line|lore'
```

| row | staging | verdict |
|---|---|---|
| **R12(i)** | `/rpg give` alone | **VOID** — blind to its own subject |
| **R2**, 2nd predict | `give` then `clear` | **NARROWED** — detects *absent rendered as 100*, blind to *`clear` not re-rendering* |
| R1, R10 | `set`, which re-mints | real, read, unaffected |
| R7, R8 | `set`, which re-mints | real, NOT RUN |

**R2 IS NARROWED AND NOT VOIDED, AND THAT DIFFERENCE IS THE DISCIPLINE.** Voiding both would have
discarded a real guard: R2's tooltip line still reddens under a build that renders an ABSENT score as
`100`. ***"Every row staged on `give`" was a CHARACTERISATION and it is wrong about one of the two.***
Compute the class.

> ### *** A GATE SHEET CHECKS EACH ROW AGAINST REALITY AND NEVER CHECKS THE ROWS AGAINST EACH OTHER ***
>
> **R14 passed: the PDC gets a score at `give`. R12(i) passed: no fresh item shows a score line. BOTH
> READINGS ARE HONEST, BOTH ROWS ARE CORRECT, AND TOGETHER THEY IMPLY A DEFECT NEITHER ONE REPORTS** —
> the stamp works and the render does not.
>
> **Every row here is checked against the game. No row is checked against its neighbour.** That is the
> whole gap: **two rows whose claims overlap are a free cross-check nobody is taking**, free precisely
> because both readings already exist and neither costs another boot.
>
> **Practically: when two rows in one file touch one mechanism from different sides, write down what
> their CONJUNCTION implies** — in one of the two rows, not in a summary nobody re-reads. Neither row
> above is wrong, and no amount of care with either would have found this.

**The defect is fixed in 12c**, which pairs every write-at-acquisition with a re-render.

**R12(i) IS NOT RE-PREDICTED HERE.** A prediction is not edited once its row has been read — the
reading is withdrawn and the prediction stands as written. 12c adds a new row staged on a **freshly
acquired, never-set** item, carrying R14's control structure.

---

### R13 — A volley_stone that ALREADY carries a score in its PDC is still unscored

**ABSENCE IS NOT THE TEST, and this row is the whole reason the exclusion is a DECLARATION rather
than a missing key.** Slice 12 shipped *no key means 100*, so "unscored" and "predates the score"
would otherwise be the same bytes — **and stones minted since slice 12 already carry a real score.**

| | |
|---|---|
| **Setup** | `/rpg give volley_stone`, then **`/rpg gearscore set 400`** on it — this manufactures exactly the item a player minted last week already holds. |
| **Predict** | `/rpg gearscore show` reports the stamp is **present and 400** — the PDC genuinely holds it. |
| **Predict** | And the stone **still deals 4 per shot**, unchanged. The declaration overrides the stamp. |
| **Predict** | **If the damage moves to 16, the exclusion is reading the PDC instead of the definition** and is inert for every stone already in the world. |
| **READ** | **PASS** — 2026-09-20, booted by Ben on `4648283`. Confirmed against the prediction; figure not recorded (Ben's ruling). |

### R14 — *** A volley_stone MINTED ON THIS BUILD CARRIES NO gear_score KEY AT ALL. SOLE WITNESS. ***

**THE WRITE SIDE, AND NOTHING ELSE IN THE PROJECT CAN SEE IT — MEASURED, NOT ASSUMED.**

> **`MUT12B-STAMPFALSE` -- the stamp path passing a hardcoded `false` -- was applied to the tree and
> THE ENTIRE SUITE STAYED GREEN AT 2070 TESTS, 0 FAILURES.** The average stays correct under it,
> because `candidateScore` refuses independently; the read path stays correct, because it consults
> the definition. **Only the bytes in a freshly minted stone's PDC differ**, and no module can
> construct an `ItemStack` to look.
>
> A signature row (`theStampAsksTheCOMPOSEDDoorWithTheDerivedFlagAndNotALiteral`) now bans the
> literal and reddens under that mutation — **but that is a source scan, not a behaviour.** This row
> is the only thing that observes the stamp actually declining to write.

**AND IT MUST NOT BE READ OFF THE AVERAGE.** A stone excluded from the top two proves the READ
refused, which R12 and R13 already cover. **The claim here is about the item's own bytes.**

| | |
|---|---|
| **Setup** | On this build, freshly: `/rpg give volley_stone`. Do NOT `set` anything on it. |
| **Predict** | `/rpg gearscore show` reports **`Held: no stamp (reads 100)`** — the wording R2 relies on, which distinguishes an ABSENT key from a stored 100. |
| **Predict** | **NOT `Held: 100`.** A stored 100 means the stamp fired and wrote a baseline — the refusal did not happen, and the two independent refusals have collapsed into one. |
| **Predict** | For contrast in the same reading: `/rpg give boltor` and confirm it **does** report a rolled score. **A row where nothing is stamped proves nothing if the stamp is broken for everything.** |
| **READ** | **PASS** — 2026-09-20, booted by Ben on `4648283`. **Including the `/rpg give boltor` control in the same reading.** Confirmed against the prediction; figure not recorded (Ben's ruling). |

---

### R15 — CONTROL: everything slice 12 shipped still behaves

**This slice edited the scoring path, the stamp signature, `AdapterContext`, and `scoreable`'s
visibility. A mistake in any of those shows up in a screen this slice is not about.**

| | |
|---|---|
| **Setup** | `/rpg give boltor`, `set 175`. Then open the Nexus hub, the vault, the enchant table and the grindstone. |
| **Predict** | The Boltor's `attack_damage: 19` still scales on the ATTACK path: **33.25** at 175, exactly as R1 predicts. **12b must not have moved the attack path at all.** |
| **Predict** | The hub's stats screen still shows a gear score average; the vault, enchant table and grindstone all open and behave. |
| **Predict** | `/rpg gearscore show` still reports six slots, a hand pool and a band. |
| **READ** | **PASS** — 2026-09-20, booted by Ben on `4648283`. Confirmed against the prediction; figure not recorded (Ben's ruling). |

---

## SLICE 12c — THE TOOLTIP. Rows R16–R21, all NOT RUN.

**GAME MODE: SURVIVAL**, per this file's header. **No row below has been read, and every prediction
was written before any 12c boot.**

**Why they are here and not in a new file:** same subsystem, and one tally. The R6 omission — a row
lost from every hand-typed list until the table was summed — is the argument for one count rather
than two.

> ### *** WHAT SLICE 12c CLAIMS, IN ONE SENTENCE, SO A FAILING ROW CAN BE ATTRIBUTED ***
>
> **The number a player SEES equals the number the system PRODUCES.** Since `1c030e2` it has not: a
> weapon's real damage became a function of its gear-score stamp and the tooltip kept rendering the
> authored figure. A Boltor at GS 340 read `Ranged Damage: 19` and hit for `64.6`.
>
> **THE UNIT SUITE CANNOT WITNESS ANY OF THIS.** No module can construct an `ItemStack`, and
> `golden-lore.txt` renders through the definitions-only overloads where the score is empty and
> scaling is the identity — **measured: it did not redden under any of the four mutations run
> against the render sites.** Five new `WeaponLoreTest` rows stage a non-identity score and do guard
> the arithmetic; what no test reaches is a real item, a real stamp and a real acquisition.

### R16 — *** A FRESHLY ACQUIRED, NEVER-SET WEAPON SHOWS ITS SCORE AND ITS SCALED DAMAGE. SOLE WITNESS. ***

**NEVER-SET IS THE WHOLE STAGING.** Every reading that has ever confirmed scaled damage — R1, R10,
R15 — staged through `set`, which re-mints. **Nobody has ever measured a weapon that was given and
then left alone**, which is the state every real drop is in.

> **IT IS ALSO THE ONLY WITNESS FOR A SOURCE TRACE.** `stampOnAcquire` wrote the PDC and nothing
> re-rendered, so a fresh item carried a score and showed no line. That is fixed in 12c through
> `GearItems.refreshLore`, and **no unit test can see it** — the same blind spot that let
> `MUT12B-STAMPFALSE` leave 2070 tests green.

| | |
|---|---|
| **Setup** | On this build, freshly: `/rpg give boltor`. **Do NOT `set` anything on it.** Hold it in the main hand and **wait one second before reading the damage** — see the clock note below. |
| **Predict** | The tooltip carries a `Gear Score: N` line, with N the rolled score. **Before 12c it carried none at all**, whatever the PDC held. |
| **Predict** | `/rpg gearscore show` reports the same N. **The tooltip and the PDC agree** — that is the claim. |
| **Predict** | `Ranged Damage:` reads `19 × N / 100`, not `19`. |
| **Predict** | A hit deals that same number. **Tooltip, stamp and dealt damage are three readings of one fact.** |
| **Predict** | **CONTROL, in the same reading:** `/rpg give volley_stone` shows **no** score line and `Kinetic Damage: 4  x 3`. Without it this row cannot tell *the render is fixed* from *every weapon now renders a line regardless*. |
| **READ** | **PASS** -- 2026-09-21, `593e6a7`. Confirmed against the prediction. **FIGURES NOT CAPTURED, AND THAT IS A RULING RATHER THAN AN OVERSIGHT** -- Ben, 2026-09-21, the same ruling 12b's rows carry. The verdict is the whole reading: neither the tooltip string nor the dealt number was written down. |

> **THE ONE-SECOND WAIT IS A STAGING INSTRUCTION, NOT A COURTESY.** The main-hand attack damage is
> not written at mint and is not cached: it is reconciled from the held stack's stamp by
> `PlayerHealthSystem`'s repeating task every **`RECONCILE_PERIOD_TICKS = 5`** — one writer, one
> clock, no cache (recorded at `CombatantStats.reconcileAttackModifiers`). **For up to 250 ms after
> the hand changes, the stat still describes the previous item.** No one types a command and reads a
> tooltip that fast, so this is not a hazard for a hand-run row — **it is written into the staging so
> an automated reading later cannot produce a false FAIL nobody can explain.**

### R17 — *** A STAMPED, EXCLUDED WEAPON RENDERS ITS AUTHORED NUMBER. SOLE WITNESS. ***

***R13 CANNOT SEE THIS. R13 READS DAMAGE; THIS IS DISPLAY, AND R13 PASSES WHILE THE TOOLTIP LIES.***
A build that scales the display while the exclusion holds at runtime satisfies R13 completely.

| | |
|---|---|
| **Setup** | `/rpg give volley_stone`, then **`/rpg gearscore set 400`** on it — the same manufactured item R13 uses, read on the SCREEN rather than at the target. |
| **Predict** | The damage line reads `Kinetic Damage: 4  x 3` on right-click's trigger and `4  x 8` on left-click's. **The authored 4, unscaled.** |
| **Predict** | **NOT `16`.** `scaledDamage(4, 400)` is 16, and 16 is what a build asking `orAbsent` alone renders. |
| **Predict** | The tooltip carries **NO** `Gear Score:` line, though `/rpg gearscore show` reports the stamp present at 400. **The stamp is real; the display refuses it.** |
| **READ** | **PASS** -- 2026-09-21, `593e6a7`. The excluded weapon rendered its authored number. **FIGURES NOT CAPTURED (Ben's ruling, 2026-09-21).** As a SOLE WITNESS this row's verdict is now the only surviving evidence that the exclusion reaches the tooltip, and it rests on the reader rather than on a recorded string. |

### R18 — A cleared weapon's damage line reverts WITH its score line

**`clear` is the quiet one.** It drops the score line and could leave a stale scaled figure beside
it — a tooltip with no score showing a scaled number is indistinguishable from an authored one to
anyone who does not know the weapon.

| | |
|---|---|
| **Setup** | `/rpg give boltor`, `set 340`, read the tooltip. Then **`/rpg gearscore clear`** and read it again. |
| **Predict** | Before: `Gear Score: 340` and `Ranged Damage: 64.6`. |
| **Predict** | After: **no score line AND `Ranged Damage: 19`.** Both move together or the row fails. |
| **Predict** | The hit then deals 19. |
| **READ** | **PASS** -- 2026-09-21, `593e6a7`. Both lines reverted together. **FIGURES NOT CAPTURED (Ben's ruling, 2026-09-21)** -- what is recorded is that the two lines moved TOGETHER, which is the claim; the values they moved between are not. |

### R19 — The rendered number equals the dealt number, on BOTH render sites, at one non-identity score

**Two sites, two arms, and they scale through different code.** A basic attack reads the
ATTACK_DAMAGE stat that `WeaponAttackItems` folds the score into; an ability literal is scaled by
`EffectApplier` at cast. **One weapon cannot exercise both**, so this row uses two.

| | |
|---|---|
| **Setup** | `/rpg give boltor`, `set 175` (basic attack). `/rpg give flint_staff`, `set 175` (ability literal). |
| **Predict** | Boltor: tooltip `Ranged Damage: 33.25`, and a hit deals **33.25**. |
| **Predict** | Flint Staff: tooltip `Fire Damage: 35`, and a bolt deals **35** (`20 × 175/100`). |
| **Predict** | **175 is chosen so the product is not a round multiple of the authored figure** — `33.25` cannot be reached by doubling, and `19`, `175` and `33.25` are three different numbers. |
| **READ** | **PASS** -- 2026-09-21, `593e6a7`. Both sites agreed: the rendered number equalled the dealt number on the basic attack and on the ability literal. **FIGURES NOT CAPTURED (Ben's ruling, 2026-09-21) -- AND THIS ROW IS THE ONE THAT PAID FOR IT.** Its whole purpose was the cross-multiplication `33.25 x 340 = 11305 = 64.6 x 175`, and **that ratio can no longer be re-derived from this file.** What survives is a verdict that two numbers matched, on the word of the person who read them; a future drift in the scale factor cannot be located by reading this cell. |

### R20 — The longest line this change can produce still reads

**There is no width test in this repo** — measured; the only `91.8` in the tree is this file's R10
prediction. So this is an eyeball reading, and it is the only kind available.

| | |
|---|---|
| **Setup** | `/rpg give cursed_emerald`, `set 340`. Its element line is the longest label in shipped content and its volley renders a multiplier. |
| **Predict** | The line reads `Kinetic Damage: 91.8  x 6` and is **not truncated, not wrapped, and does not push the tooltip off-screen**. |
| **Predict** | The fraction renders as a fraction. **No rounding to `92`** — rounding is the same lie one decimal place smaller. |
| **READ** | **PASS** -- 2026-09-21, `593e6a7`. The longest line read without truncation or wrapping, and the fraction rendered as a fraction. **FIGURES NOT CAPTURED (Ben's ruling, 2026-09-21)** -- and this row was always an eyeball reading, so the ruling costs it least of the six. |

### R21 — CROSS-CHECK: the rendered shot count equals the observed shot count

**R12 predicted the counts from a document. This reads them off the weapon**, so the plugin and the
boot sheet are two independent sources rather than one copied twice.

> **R12's OWN PREDICTIONS ARE NOT EDITED.** That row has been read, and a prediction is not edited
> once a row has been read — so the cross-check is a NEW row that cites it rather than an amendment
> to it. The convention is the reason, not tidiness.

| | |
|---|---|
| **Setup** | `/rpg give volley_stone`. Read the two damage lines, then fire each trigger and count the projectiles. |
| **Predict** | The tooltip says `x 3` on right-click and `x 8` on left-click. |
| **Predict** | **The observed counts are 3 and 8, matching what the tooltip claims.** A disagreement means the criterion reads a different field from the one the cast executes. |
| **Predict** | `cursed_emerald` likewise renders `x 6` and fires six. |
| **READ** | **PASS** -- 2026-09-21, `593e6a7`. Rendered counts and observed counts agreed on both weapons. **FIGURES NOT CAPTURED (Ben's ruling, 2026-09-21)** -- the AGREEMENT is recorded and the counts are not, so the cross-check against R12 is no longer re-runnable from this file alone. |
