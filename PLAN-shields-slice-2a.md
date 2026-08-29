# Shields, Slice 2a — the gear-gating axis and Bulwark

Branched off `25d110b`, verified from the wire (`git ls-remote --heads origin` →
`25d110bb4e980059d2c8d1c3e30275d0da1d8944`, equal to local `master`).

**2b (Riposte) is a separate PR off the new `master`.** 2a is independently shippable, and landing it
first verifies the axis migration before the reflect seam stacks on it.

## Context

Slice 1 (`027da30`) shipped a mintable `roundshield` and made blocking real, but shipped it
**enchant-COMPATIBLE and not enchant-ROLLED**: `EnchantRoll.roll`, `EnchantEffectLine.bare` and
`showEnchants` were all keyed on `WeaponClass`, and a shield has none. `NEXT.md:628-640` names those
three. Exploration found a **fourth** the record does not mention — `EnchantMenu` was
`WeaponDefinition`-typed end to end and refused a shield at the door with *"That is not one of your
weapons."*

2a closes the axis and hangs the first shield enchant off it: a shield rolls its own candidates, a
player unlocks them **at the table**, and Bulwark rides the existing block seam.

---

## What shipped

### The axis

`GearClass { MELEE, RANGER, MAGE, SHIELD }` replaces `WeaponClass` in the **enchant-gating role
only**. A weapon maps in through `GearClass.of` (exhaustive, no default arm); a shield presents
`SHIELD`; `null` still means universal.

`EnchantDefinition`'s javadoc argued against exactly this — *"a parallel enum would need SUMMONER
adding in two places, and the exhaustive-switch discipline only works with one enum."* The first half
is true and accepted; the second was **wrong, and the compiler settled it**: deleting an arm from
`GearClass.of` gives `GearClass.java:[72,16] the switch expression does not cover all possible input
values`, BUILD FAILURE. Two places, one of which the compiler names. That javadoc is rewritten.

**Not migrated, deliberately:** `ClassDamageModifiers` (a ring's `+N <Class> Damage` gates on the
WEAPON you fight with; a shield in the other hand must not change it) and `WeaponClassLabel`
(`WeaponLore` and `ClassDamageModifierItems` read the weapon axis, where "Shield" is meaningless).
`GearClassLabel` is a sibling, not a replacement.

### Bulwark

`Shield.clamp(base_dr + 0.05/0.10/0.15)`, composed in `ShieldBlock.resolve`, applied by the rider.
Executed: roundshield `0.5 → 0.55 / 0.60 / 0.65`; a 15.0 hit passes `7.5 → 6.749999999999999 / 6.0 /
5.25`.

Additive because it is the reading whose label is honest, and because the two rejected readings are
**bit-identical at the shipped 0.5** (`0.525 / 0.55 / 0.575` for both multiplicative and
diminishing). The test therefore also asserts at `0.8`, where all three separate: `0.5` pins the
composition at one point, `0.8` pins the rule.

**No new clamp** — the boosted value routes through `Shield.clamp`, which had exactly one caller
until now. This is the second consumer, which is what validates it.

### The broken gate, and the three meanings of NONE

`ShieldBlock.resolve` returns `Outcome.NONE` for a broken shield, so base DR, Bulwark and 2b's
reflect fall off **one predicate**. `Outcome.blockDr` is renamed `effectiveDr` — a component still
called "the shield's DR" while carrying an enchant's contribution is how a witness log starts lying.

`Outcome.NONE` now has three coherent causes: untagged / dangling / broken. **Vanilla shields stay
zero-protection**; no default DR is invented.

Consequence, and the right one: `resolve` runs before the wear, so the block that BREAKS the shield
still mitigates in full and only the next one does nothing.

`ShieldBrokenNotice` has its **own throttle key** (`__broken_shield_notice`). Sharing
`BrokenNotice`'s would let a broken sword silence it for 40 ticks, and a player fighting with both
spent is exactly when both need to speak.

### Binding by effect, not by id

`BlockEnchantItems` follows `DamageEnchantItems.damageGrantsOn`, not the Unbreaking seam. Unbreaking
is read by hardcoded id because its curve is Java; Bulwark's curve is content, so the definition must
be resolved anyway and filtering on `effect()` is free — which makes the second block enchant a yml
file rather than a recompile. **There is deliberately no `Bulwark.ID`.**

### The table

`EnchantMenu` accepts shields through `PlacedGear`, shape-aligned with `RpgCommand.HeldGear`. Without
it the roll is a dead end: a shield would mint with locked candidates no player could ever buy.

**Two gear records rather than one shared abstraction, on purpose.** Designing a common `Gear` type
from two examples is designing it from one and a half. **ARMOR is the third shape and the trigger to
extract one** — recorded in `NEXT.md`.

The economy needed nothing: `EnchantCost.xpPoints(targetLevel, bookshelfPower)` reads neither
definition nor rarity.

---

## Verification

**Baseline at `25d110b`, measured before touching anything: core 488 / storage 17 / paper 322.**
(The figures in the older plans — 457/17/308 — are from `e5f0bd5`, two slices back.)

**Final: core 505 / storage 17 / paper 340. BUILD SUCCESS.**

### The fixture fix, and why it led

`EnchantLoaderTest` **would not have reddened** for `bulwark.yml`, and that is worse than if it had.
The fixture *enumerated* the roster (`SHIPPED = List.of("unbreaking", "sharpness", "power",
"attunement")`) and copied those four into a `@TempDir`, while asserting *"the shipped enchant roster
is exactly these four files"* — a claim it could not make. A fifth shipped file would have been
loaded by no test at all.

Now it lists the classpath directory and **refuses an empty result**. Positive control, measured:

| | probe present (malformed, no `class:`) |
|---|---|
| OLD fixture | `Tests run: 17, Failures: 0` — **BUILD SUCCESS**, `_probe` appears **zero** times in the log |
| NEW fixture | exit 1, `Tests run: 18, Failures: 7`, `expected: <[…, _probe]> but was: <[attunement, unbreaking, power, sharpness]>` |

And the same control on the real file: flipping `bulwark.yml`'s `class: shield` to `class: universal`
reddens 7 tests and names it. Under the old fixture that edit was silent.

### Mutations run (marker-grepped, `test-compile` first, restored from the scratchpad)

| Mutation | Result |
|---|---|
| `GearClass.of`: `case MAGE -> SHIELD` | 2 red — *"the two axes must agree on the spelling of MAGE"* |
| `GearClass.of`: delete an arm | **compile error** — the exhaustiveness claim, verified |
| `Bulwark`: multiplicative | 3 red — `expected: <0.8500000000000001> but was: <0.8400000000000001>` |
| `Bulwark`: diminishing | 4 red — `expected: <0.8500000000000001> but was: <0.81>` |
| `requireCurve`: drop the size check | 2 red — the **pre-existing** damage-curve test AND the new block-curve one, proving the lift is shared |
| `bulwark.yml`: `class: universal` | 7 red, naming the file |

**A comment that overclaimed, caught by running the mutations rather than reasoning.** The first
draft said a 0.5-only test "could not tell a wrong implementation from the right one". False —
additive differs from both rivals at 0.5 and both mutations duly reddened the shipped-shield test.
What 0.5 cannot do is say *which* wrong rule was followed. Corrected in the class and the test.

### `clean` is NOT inert for test sources — a new finding

`./mvnw -pl paper -am test-compile` returned **exit 0 with 48 compile errors present**: the test
sources were unchanged, so Maven skipped them and stale `test-classes` satisfied it. `clean
test-compile` reported all 48.

`NEXT.md` records `clean` as measurably inert — that was measured for MAIN sources against a changed
dependency module, and it still holds. This is a different axis: **TEST sources against changed MAIN
sources in the SAME module**, where the incremental hole is real. Run `clean` before believing a
green test-compile after a signature change.

---

## Boot gate — ROW 1 RUN AND PASSED; ROWS 2-14 OWED BY A HUMAN

`./scripts/dev-server.sh --refresh-content`.

### Row 1, run 2026-08-29 13:04 — the content loads on a real server

Paper 26.1.2.build.74. Deploy verified BEFORE booting, by mtime and size, not assumed — target
`13:02:43` / `464427` bytes, deployed `13:03:42` / `464427` bytes — and `bulwark.yml` confirmed
present inside the shaded jar (`unzip -l`, 3115 bytes) rather than only in `src`.

```
[Rpg] Loaded 6 abilities, 7 visuals, 5 statuses, 7 elements, 5 enchants,
      2 kits, 5 weapons, 1 shields, 1 mobs
Done (5.164s)!
```

**5 enchants, up from 4.** Zero `Skipping malformed enchant` lines anywhere in the log; every
`WARNING` in it is JVM or Maven noise, none from Rpg. So `class: shield` parses through the real
`GearClass` token list, `effect: block_dr` binds through the real `EnchantEffect`, and the widened
compact-constructor validation accepts the shipped file on a live server rather than only in a
`@TempDir`.

**What row 1 does NOT establish:** anything mechanical. It is a load check. No item was minted, no
hit was blocked, no tooltip was read.

**The file lock is real, and it bit on the way out.** After the boot, `rm` on the deployed jar failed
with `Device or resource busy` -- the exact error CLAUDE.md records -- because the server JVM
outlived the script that started it. Two `java.exe` (an Oracle `javapath` shim plus the JDK process
it spawns) had to be stopped before the jar could be replaced. **Confirm the previous server is dead
before the next deploy**, or `set -e` aborts the deploy and a stale build boots looking fine.

### Rows 2-14 — owed by a human

**Give a FRESH `roundshield` first.** `rollOnAcquire` fires only at acquisition, never from
`mint`/`remint`, so a Slice-1 shield still in the tester's inventory carries no `enchant_rolled` flag
and nothing will ever roll it. It would show empty slots and read exactly like a broken roll.

| # | Check | Expected |
|---|---|---|
| 1 | boot log | `Loaded … 5 enchants`; no `Skipping malformed enchant` |
| 2 | `/rpg give roundshield`, read the tooltip | candidates rolled; `Block: 50%`; `Common Shield` still last |
| 3 | place it in the enchant table | candidates render; **no** "That is not one of your weapons" |
| 4 | `/rpg give ironblade` ×3, open each | only Sharpness / Unbreaking — **never** Bulwark. The gate, weapon side |
| 5 | `/rpg enchant show` on a shield | replies rather than refusing |
| 6 | activate Sharpness on a shield | `inert: a Melee enchant on a shield` |
| 7 | unblocked mob hit | `15.0` |
| 8 | blocked, no Bulwark | `7.5` |
| 9 | buy Bulwark I/II/III at the table, blocked each time | `6.75` / `6.00` / `5.25` |
| 10 | the shield tooltip at Bulwark III | `Block: 65%`, agreeing with row 9 |
| 11 | hold the shield in the MAIN hand, `/rpg durability set 334` | `2/336 uses left (damage 334)` — not yet broken |
| 12 | block once | `ShieldBrokenNotice` fires **once**; reduction on THIS hit is still `7.5` (or the Bulwark figure) |
| 13 | block again | **no reduction** — `15.0`; notice does **not** repeat inside 40 ticks; **record what vanilla still animates** |
| 14 | `/rpg repair`, block again | back to `7.5` — the gate is the durability state, not a one-way latch |

**Rows 11-14 exist because 2a could not otherwise witness its own break gate.** `WEAR_PER_BLOCK` is 1
against a vanilla shield's 336 uses, so reaching broken by blocking meant ~250 hits, and with
Unbreaking III roughly a thousand. `/rpg durability` gained the shield arm for exactly this — the
same argument that put the shield arm on `/rpg enchant` in Slice 1.

**Every number above is executed against the real `Durability` kernel, not worked out by hand:**

```
MIN_USES = 1;  broken threshold is damage >= 335
  set 334 -> damage 334  usesLeft 2  broken=false
  + one block of wear -> damage 335  usesLeft 1  broken=TRUE   <- the notice fires here
  + another block      -> damage 335  (floored; a spent shield stops wearing)
  set 999 -> damage 335  usesLeft 1  broken=true (clamps straight to broken, skipping the crossing)
```

Use `set 334`, not `set 999`: the latter arrives already broken, so `resolve` returns NONE, no wear
happens, and **the notice never fires** — a row that would read as a broken notice rather than as a
skipped crossing. Run row 11 on a shield with **no Unbreaking active**, or the wear is probabilistic
and the crossing may take several blocks.

**Row 13 carries the named seam trap.** `Durability.wear` floors at one remaining use, so a "broken"
shield is still a functional item to vanilla: it will keep playing the raise, the block sound and the
knockback dampen, and keep reporting `BLOCKING < 0`, while `resolve` returns NONE and the player
takes the hit in full. **Record what the player sees and takes**, and whether one crossing notice is
enough against vanilla continuing to animate a block that does nothing. That is a feel question this
slice does not settle — it is the row most likely to send a decision back into 2b.

**A guard that fires is not a hypothesis to argue with.**

---

## What 2a does NOT do

- **Riposte.** Slice 2b, its own plan and PR.
- **A `ShieldRefresher`** for the join / `/rpg refresh` path. Still outstanding from Slice 1: a
  shield's lore does not rebuild from content on rejoin the way a weapon's does — and now that the
  lore carries an enchant-dependent block percent, that gap is slightly more visible.
- **A shared `Gear` abstraction.** Two records, aligned; armor is the trigger.
- **Anything about the immunity ceiling.** Additive Bulwark makes total immunity reachable on a
  future `block_dr >= 0.85` shield (`0.9 + 0.15 = 1.05`, clamped to `1.0`). Nothing shipped is close;
  the decision is owed the day a high-DR shield is authored.
