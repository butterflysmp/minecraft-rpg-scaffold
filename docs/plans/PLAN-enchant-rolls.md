# PLAN — the enchant rolls pass

Closes `NEXT.md`'s *"The per-instance enchant ROLL and the class POOLS are deferred; Pass 1 assigns
candidates by hand."*

Branched off `origin/master` at `c0213ad` (PR #19, the Enchant UI pass), which is where the table
landed.

## What this pass is

The table has worked since PR #19 and has been opening on nothing. A real weapon arrived with no
slots, so `/rpg enchant candidate` was the only thing that could put a candidate in front of a
player. This pass makes a weapon **arrive pre-rolled**: three slots of class-valid, locked
candidates, decided once when the item is created and never again.

`Keys.enchantRolled` (BYTE) has been written and carried across every re-mint since Pass 1 with
nothing reading it, reserved for exactly this. This pass is the reader, and the forecast held — the
carry needed no change at all.

Unlocking is still **free**. The XP economy and the bookshelf discount are still the next pass.

## The commits

| # | commit | what it is |
|---|---|---|
| 1 | `81e443e` feat(core): a weapon's candidates are rolled, once | `EnchantRoll` — the pure selection |
| 2 | `23cc78f` feat: a minted weapon arrives pre-rolled | `EnchantRollItems` + the two call sites |
| 3 | `9288e52` docs: the no-stacking rule is permanent, and rarity sizes candidates | four javadocs |
| 4 | this file | the record and the gate |

## The load-bearing invariant, and the trap under it

**The roll fires exactly once per item instance and never again.** Re-rolling on a refresh destroys
unlocks a player has paid for, and it is invisible until someone refreshes.

**The trap is that the obvious guard looks correct and fails open.** `remint` calls `mint`
(`WeaponItems.java:211`), so a roll hooked inside `mint` fires on every join, every `/rpg refresh`,
every `/rpg enchant` sub-op and every enchant-table click. Guarding it on
`EnchantItems.isRolled` does not help: `mint` builds a **fresh `ItemMeta` with an empty container**,
and `carryInstanceData` — which restores `enchant_rolled` from the old item — runs *after* `mint`
returns. So `isRolled` reads **false inside `mint` for every item, new or not**, and the guard reads
as present while doing nothing.

So the rule is about the **call site**, not the flag: *the roll is never called from inside
`WeaponItems`.* That is one grep-able sentence, and `EnchantRollItems`' javadoc and
`EnchantItems.isRolled`'s both carry the warning where someone would go looking.

The flag is still checked, and it is what makes `rollOnAcquire` safe to call from any future
acquisition path — a loot drop, a quest reward — without that path knowing whether the item is new.
`carryEnchants` moves `enchant_data` and `enchant_rolled` as raw bytes, so a rolled item stays
rolled through every re-mint there will ever be.

**No unit test can catch a misplaced hook.** This invariant is boot-owed, not test-owed, and gate
rows 9–10 are where it is actually proved.

## The decisions

**The roll hooks in at the two `mint` call sites that create a weapon FOR a player** — `/rpg give`
(`RpgCommand.java:640`) and the kit grant (`RpgCommand.java:1198`). Neither routes through `remint`.
There is no loot table yet; when one lands it calls the same helper.

In `grantWeapons` the call sits **inside** the `for (WeaponGrant grant : kit.weapons())` loop, so
each kit weapon rolls its own candidates. A line after the loop would leave every kit weapon but the
last un-rolled — a bug that hides completely in a one-weapon kit, which is what both shipped kits
are today.

*Consequence, not a defect:* `/rpg class` then `/rpg element` both funnel into `grantWeapons`, so
onboarding already mints kit weapons twice. Those are two separate item instances and each correctly
rolls once.

**All weapons roll, including `ability_stone`.** Candidates arrive locked, so the stone's behaviour
is unchanged until someone deliberately unlocks one; the dev workflow now opens with `/rpg enchant
clear` anyway, which blanks it by the same step it blanks anything else. The argument the other way
is real and worth recording — the stone's `class: mage` is documented as arbitrary (*"a dev tool;
class is required, mage is as good as any here"*), so the roll gates it on a meaningless axis. What
decided it was cost: `WeaponDefinition` has no boolean fields and `WeaponLoader.parse` reads only
eight keys, so an opt-out is new schema surface for one dev item, and a hardcoded id list in Java is
a CLAUDE.md banned pattern outright. If it ever proves annoying the clean escape is a `rollable:`
content field — one key, one reader, no id list.

**The distribution is uniform and unweighted, independent per slot.** The pool is
`adapters.enchants().all()` in registry order, filtered to `isUniversal() || weaponClass ==
heldClass`. Per slot: a count uniform over `1..min(3, poolSize)`, then that many picks uniform
without replacement from a **fresh copy** of the pool.

With today's roster every class has a pool of exactly two — its damage enchant plus Unbreaking — so
counts are `{1, 2}` and a three-candidate slot is unreachable. **That sparseness is accepted** and
becomes reachable the day a fourth enchant ships; `candidateCount` already ranges to 3 and is tested
there, so nothing has to change when it does.

**Not rarity-weighted, deliberately.** At a pool of two, a tier curve is unobservable — you cannot
tell a legendary from a common by a 1-versus-2 count without a sample far larger than a boot gate —
so it would ship unwitnessable. `Rarity`'s reserved meaning was rescoped to the candidate axis for
exactly this, rather than deleted.

**The fresh copy per slot IS the same-enchant-across-slots rule.** Distinctness is a within-slot
property, enforced by `EnchantSlot`'s own constructor, and two slots may both offer Sharpness.
`EnchantState.effective()` resolves that to the highest level either holds it at, never the sum —
now the decided rule rather than a provisional one.

**The dev workflow is give → clear → candidate.** On a rolled melee weapon whose slot 0 already
offers `sharpness`, `/rpg enchant candidate 0 sharpness` hits `EnchantSlot`'s distinctness refusal.
That is correct and stays: `clear` removes **both** keys, returning the weapon to genuinely
never-rolled, and because the roll fires strictly at acquisition a cleared weapon stays blank across
`/rpg refresh` and a rejoin. Making the dev command replace-instead-of-throw would give it different
duplicate semantics from `EnchantState.addCandidate` — one rule in two places — and would mask a
real duplicate in a hand-built test setup.

**The seam is decomposed by decision KIND, not by draw.** `candidateCount(poolSize, roll)` and
`pick(remaining, roll)` each take one already-drawn double, the way `Unbreaking.consumes` does, so
each boundary is pinned at an exact value in isolation. `roll(...)` is a thin orchestrator over a
`DoubleSupplier` so one readable end-to-end test can own the shape. That split is deliberate: a flat
array of draws would couple every boundary assertion to the implementation's draw order, which is
invisible — reorder the draws in a refactor and each literal silently addresses a different decision
while the test stays green. Exactly the failure this repo polices, moved into index arithmetic.

## What was verified, and how

`./mvnw clean package` → **BUILD SUCCESS, 606 tests** (core 340, storage 17, paper 249).
`scripts/check-tests.sh` reports per-module counts and `scripts/check-jar.sh` passes.

*Note for the record:* `PLAN-enchant-table-ui.md` recorded paper at 247. It was **248** at
`origin/master` — that figure predates the four commits after the UI merge. This pass adds one paper
test, hence 249, not 248.

**Four mutations were run**, each after confirming the marker was in the source and `test-compile`
was clean, and each restored from a scratchpad copy and re-verified green:

| mutation | result |
|---|---|
| `candidateCount`: drop the `1 +` | 3 red — but **not** the predicted empty slot. `Math.max(1, ..)` absorbs the zero and every count collapses to 1 instead: *"0.5 is the first draw that offers two ==> expected: `<2>` but was: `<1>`"*. The two clamps bound different ends and are not redundant |
| `roll`: drop `remaining.remove(picked)` | 7 red, by **error not assertion**: *"slot offers 'unbreaking' twice; a slot's candidates must be distinct"*. `EnchantSlot`'s constructor throws before the distinctness assertion runs, so that test guards by erroring; the assertion stays as defence in depth against that rule moving |
| `poolFor`: drop the `weaponClass() != null` arm | 2 red — Unbreaking leaves every class pool |
| `roll`: hoist `remaining` out of the slot loop | 2 red — `theSameEnchantMayBeOfferedInMoreThanOneSlot`, which is the property it owns |

The first two comments in `EnchantRollTest` were **rewritten to what the runs actually printed**
rather than what they were predicted to print. The first is the more useful of the two: the
predicted red was wrong, and believing it would have left a comment claiming a guard the code does
not have.

A fifth mutation was attempted and **silently did not apply** — `perl -0777` with `\Q...\E` around a
pattern containing `\n`, where `\Q` quotes the backslash and the newline never matches. It was
caught by the `grep` for the marker, which is the entire reason that step exists. Re-run line-based,
it reddened as designed.

## Boot gate — OWED IN FULL BY A HUMAN

Every row needs a `Player`. A console log can only prove the plugin loaded, and the invariant this
whole pass rests on (rows 9–10) is unreachable from a console.

Run `./scripts/dev-server.sh --refresh-content`. Note that `--refresh-content` is a **shell flag on
that script** which re-copies content YAML from the jar — there is no `/rpg refresh-content` command;
the in-game re-mint is **`/rpg refresh`**. Stop any previous server first: a live one holds the jar
lock, which is the incident CLAUDE.md's verification section opens with.

**Count the item before and after every row that moves one.**

| # | action | expect | proves |
|---|---|---|---|
| 1 | boot | `Loaded ... 4 enchants`, no `Skipping malformed enchant` | the roster the pool is drawn from |
| 2 | `/rpg give ironblade`, open the table | 3 columns, each **1–2 locked** candidates, all grey *"Click to unlock at I."* | the roll fires at acquisition |
| 3 | read row 2's icons | only **Sharpness** and **Unbreaking** — never Power, never Attunement | the class gate |
| 4 | `/rpg give hunters_bow`, open | only **Power** / Unbreaking | the gate on a second class |
| 5 | `/rpg give ember_staff`, open | only **Attunement** / Unbreaking | the gate on the third |
| 6 | `/rpg give ironblade` ×5, open each, **record the three counts** | counts **vary** across slots and items; never 0, never 3 | the count is rolled, not fixed — and the pool-of-2 cap |
| 7 | across row 6's items | some weapon offers the **same enchant in two columns** | same-enchant-across-slots is allowed |
| 8 | `/rpg enchant show` on a fresh weapon | 3 slots, every level `=0`, every active `:-1` | locked, nothing active |
| 9 | fresh `ironblade`: `show` and **write the blob down**, unlock one candidate at the table, `/rpg refresh`, `show` again | **identical** candidates; the unlock survives | **no re-roll on re-mint** |
| 10 | same item: disconnect, rejoin, `show` | identical; the unlock survives | **no re-roll on the join scan** |
| 10b | same item: `stop` the server, reboot, `show` | identical; the unlock survives | the flag survives a real save/load, not just a re-mint |
| 11 | `/rpg give ability_stone`, open | rolled Attunement/Unbreaking, **locked**; the stone still casts normally | the accepted decision, witnessed rather than assumed |
| 12 | `/rpg give ironblade` → `/rpg enchant clear` → `show` | *"Enchant data cleared -- both keys removed"*; then **no data at all**, not rolled-empty | `clear` is a true blank slate |
| 13 | then `candidate 0 sharpness`, `/rpg refresh`, rejoin, `show` | the hand-built state stands; **no roll re-appears** | a cleared weapon stays blank |
| 14 | on a **fresh** (un-cleared) `ironblade` whose slot 0 offers `sharpness`, `/rpg enchant candidate 0 sharpness` | refused with the model's own sentence; item intact | distinctness still throws — the reason row 12 exists |
| 15 | open the table on any rolled weapon | never the red overflow sentence | 3×≤3 fits the layout |
| 16 | `/rpg class` (or `/rpg element`) on a fresh profile | every kit weapon arrives **rolled**, not just the last one | the call is inside the loop |

Rows 2–8, 11 and 16 witness the **roll**. Rows 9, 10 and 10b are the pass's whole point and a green
build proves nothing about them. Rows 12–14 witness the dev path, and row 14 carries information
rather than confirming a change — it is the behaviour change the workflow has to route around.

Row 6 needs the counts written down as it goes. A roll that always returned 1 would look entirely
reasonable in any single screenshot, and "the counts varied" is not something the *after* state can
be asked afterwards.
