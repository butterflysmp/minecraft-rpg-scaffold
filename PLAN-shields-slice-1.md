# Shields, Slice 1 — a mintable common shield and the block-DR mechanic

## Context

There is no shield or block handling in this repo. `grep -in shield` across code, `NEXT.md`
and every `PLAN-*.md` returns nothing but a `"⛨"` glyph in `StatsBarText` (the armor icon).
This is greenfield.

Blocking is currently a no-op against custom HP, structurally. `RpgListeners.onMobMeleeAttack`
(`paper/.../listener/RpgListeners.java:630`) deals **the mob's attack stat**, not
`event.getDamage()`:

```java
double incoming = adapters.stats().attackValue(attacker.getUniqueId());  // the STAT
event.setDamage(TOKEN_DAMAGE);                                           // 0.01, cosmetics only
BukkitCombatant.of(victim, adapters).handle().applyDamage(incoming, attacker.getUniqueId());
```

Whatever vanilla's shield does to `event.getDamage()` is thrown away with the token. So a player
raising a shield today takes exactly what they'd take with their hands down. This slice makes a
shield mintable and gives blocking a real effect: **50% of the damage gets through**, then armor
Defense reduces the remainder inside `applyDamage`. Block-then-armor, both apply.

Outcome: a first non-weapon gear item, and a second mitigation stat that composes with the first.

---

## Decisions already taken (from the planning questions)

**Shield wear is OURS, not vanilla's.** `Unbreaking` is a custom enchant — `Unbreaking.consumes`
is called from exactly one place, `WeaponDurability.applyWearOnUse`, and its javadoc states the
no-vanilla-enchants policy outright. Vanilla wearing a shield on a block would never consult it,
so Unbreaking would be a dead enchant on shields. Instead: `ShieldDurability.applyWearOnBlock`
mirrors the weapon path, **and vanilla's own shield wear is suppressed by cancelling
`PlayerItemDamageEvent` for our minted shield** so wear is not doubled. `WEAR_PER_BLOCK = 1`
(matches vanilla). Witness: N blocks moves the bar by N, not 2N.

**The shield is a stock `Material.SHIELD` — no `blocks_attacks` component up front.** Vanilla's
default full block, 90° arc and frontal validity are what we inherit. `Shield.applyBlock` is the
sole DR; vanilla's own number is tokened to 0.01 and never reaches the player, so the component
would only affect *whether the event fires*, not the damage. That is a one-boot measurement — the
witness boot was already planned. Only if a full block turns out to suppress the event do we add
the component (`factor = blockDr`, one authored number driving both it and `Shield.applyBlock`)
and re-witness. Do not adopt the `@ApiStatus.Experimental` API to hedge a measurement we are
about to take.

---

## The load-bearing unknown, and what settles it

**Does a shield-blocked mob hit still fire `EntityDamageByEntityEvent` on this build, and how is
"this was a valid block" exposed on it?**

What is already settled, read out of the pinned sources jar
(`~/.m2/.../paper-api-26.1.2.build.74-stable-sources.jar`, extracted and read — not recalled):

| Candidate | Status on this build |
|---|---|
| `EntityDamageEvent.DamageModifier.BLOCKING` | **Exists.** Javadoc: *"the damage reduction caused by blocking, only present for Players."* The enum is `@Deprecated(since="1.12")` but **not** `forRemoval`. |
| `event.getDamage(DamageModifier)` | Safe to read — returns `0` for an inapplicable modifier. Only `setDamage(modifier, …)` throws `UnsupportedOperationException`. |
| `event.isApplicable(DamageModifier)` | Exists; tells us whether the modifier is in the map at all. |
| `event.getDamageSource()` → `DamageSource` | Exists, but carries **no** block signal (`getDamageType`, `getCausingEntity`, `getDirectEntity`, `getDamageLocation`, `isIndirect`, `getFoodExhaustion`, `scalesWithDifficulty`). Not a candidate. |
| `HumanEntity.isBlocking()` | Exists — and is direction-blind, exactly the failure mode we must not ship. |
| `LivingEntity.getActiveItem()` | Exists — the item vanilla considers in use, which is the *exact* stack that blocked. |
| `io.papermc.paper.datacomponent.item.BlocksAttacks` + `blocksattacks.DamageReduction` | Exists (`factor`, `horizontalBlockingAngle`, `blockSound`, `itemDamage`, `bypassedBy`), `@ApiStatus.Experimental`. **The contingency, not the plan.** |

**Preferred signal:** `event.isApplicable(BLOCKING) && event.getDamage(BLOCKING) < 0`. This inherits
vanilla's own validity — raised **and** frontal **and** in-arc — rather than re-deriving it.
`isBlocking()` alone would block hits from behind.

The comparison stays **strict `< 0`**. A full block's `BLOCKING` is `-raw` — still negative — so
detection holds through the `final == 0` case; the only thing that can defeat it is the event not
firing at all, which is exactly what the `LOWEST` witness below settles. Do not weaken it to
`!= 0` or reach for `getFinalDamage() == 0`.

**What is NOT settled and only a boot can settle:** whether a *full* block (blocked amount == raw,
final == 0) fires the event at all on 26.1.2, or short-circuits before CraftBukkit gets there.

### The `[BLOCK]` witness

Two handlers, shipped in the PR and **stripped before merge** — the repo's convention (there is no
witness logging in the tree today; observations get written into `NEXT.md` as a dated boot record).

1. **`@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)`** — a separate,
   temporary handler on `EntityDamageByEntityEvent`. This exists so that *"the event never fired"*
   is distinguishable from *"it fired and something cancelled it before HIGH"*. Without it,
   `onMobMeleeAttack` being `HIGH + ignoreCancelled = true` makes those two look identical.
2. **`[BLOCK] RIDER …`** inside `onMobMeleeAttack`, at the branch — not beside it.

Line shape (`[TAG] SUBTAG key=value`, doubles at 4dp, the house format):

```
[BLOCK] LOWEST victim=<uuid> attacker=<uuid> cause=ENTITY_ATTACK cancelled=false
        raw=6.0000 final=0.0000 blockingApplicable=true blocking=-6.0000
        isBlocking=true active=SHIELD offhand=SHIELD shieldId=roundshield facingDot=0.9130
[BLOCK] RIDER  victim=<uuid> blocked=true blockDr=0.5000 incoming=8.0000 reduced=4.0000
[BLOCK] WEAR   victim=<uuid> shieldId=roundshield vanillaItemDamageEvent=true cancelled=true
```

Honouring the three rules this repo learned the hard way:

- **It must be able to contradict the hypothesis.** It prints on *every* mob→player hit, blocked
  or not, shield or not — never only the case we expect. (The crit witness that logged only
  `CRITICAL_HIT` "would have printed nothing and read as 'no crits happened'.")
- **Never draw a second value to print.** `blocked`, `blockDr` and `reduced` are the same locals
  the rider decides on, not a re-read.
- **Print identity, not type.** UUIDs, not `getType()`.

`facingDot` is the dot product of the victim's look vector with the direction to the attacker,
computed only for the log. It is what lets us say empirically whether `BLOCKING` tracks vanilla's
arc rather than assuming it does.

**Order is load-bearing:** read `getDamage(BLOCKING)` **before** `event.setDamage(TOKEN_DAMAGE)`.
`EntityDamageEvent.setDamage(double)` re-derives every modifier by scaling; reading after would
report the token's share, not the block.

**If the event does not fire on a full block**, the hook moves — that is what the witness is for.
The contingency is the `blocks_attacks` component with `factor = blockDr`, which leaves the hit
partially unblocked so a damage event definitely fires; then re-witness. Do not guess which
branch we are in.

---

## Core (write these and their tests first)

### `core/.../combat/Shield.java`

Pure, static, no state — the shape of `Defense` and `AttackCharge`.

```java
public static double applyBlock(double damage, double blockDr);  // damage * (1 - clamp(blockDr))
public static double passThrough(double blockDr);                // 1 - clamp(blockDr)
public static boolean blocks(double blockDr);                     // blockDr > 0
public static final double NONE = 0.0;
```

The clamp to `[0, 1]` is **not** ceremony; it is the only thing standing between a hand-edited
`block_dr` and two catastrophes, in opposite directions:

- `block_dr: -1` → `damage * (1 - (-1))` = **double damage**. A shield that amplifies the hit.
- `block_dr: 2` → `damage * (1 - 2)` = **negative damage**, which reaches `CombatantStats.damage`
  and *heals* the victim. A shield that makes you invincible by being hit.

Both get a named test. `blockDr <= 0` returns the damage untouched, so a shield declaring no DR
blocks nothing rather than silently doing something.

### `core/.../weapon/ShieldDefinition.java`

A `record`, mirroring `WeaponDefinition`'s compact-constructor validation (throw
`IllegalArgumentException`; the loader catches `RuntimeException`, skips and names the file):

```java
public record ShieldDefinition(String id, String displayName, Rarity rarity,
                               String material, double blockDr, List<String> flavor)
```

`DEFAULT_MATERIAL = "shield"`. Validate: `id` non-blank, `rarity != null`, `material` non-blank,
`blockDr` in `[0,1]` and not `NaN`, defensive `List.copyOf` on `flavor`.

No `element`, no `WeaponClass`, no triggers — a shield has none, and `WeaponDefinition`'s
constructor rejects an empty trigger list anyway, which is why this cannot reuse that record.

### `core/.../weapon/ShieldRegistry.java`

Byte-for-byte the shape of `WeaponRegistry` (28 lines): `LinkedHashMap`, `register` throwing on
a duplicate id, `find`, `all`, `size`.

### `core/.../weapon/ShieldLoreLines.java`

The plain-text half, so the tooltip strings are unit-testable — `WeaponLoreLines`' role.
`blockLabel(double blockDr)` → `"Block: 50%"`. Pin the formatting of a non-round value by
execution, not by reasoning.

### Core tests

Mirror `DefenseTest` / `SweepShareTest` exactly: package-private class, JUnit 5 only,
`private static final double EPS = 1e-9;`, `// --- Section ---` banners, sentence-shaped method
names, a class javadoc naming the headline test and restating the provenance rule, and **every
test closing with a `// Mutation: … -> reddens.` line**.

`ShieldTest`:

| Test | Guards |
|---|---|
| `aBlockedHitLandsTheDeclaredFraction` | the multiply itself |
| `aShieldThatDeclaresNoDrBlocksNothingAtAll` | the `<= 0` guard; a 0-DR shield is untouched |
| `aNegativeBlockDrIsGuardedRatherThanAmplifyingTheHit` | `-1` → double damage |
| `aBlockDrAboveOneIsClampedRatherThanHealingTheVictim` | `2` → negative damage → a heal |
| `theReducedHitRisesWithTheDamageAndFallsWithTheDrAndNeverLeavesItsBounds` | shape: `[0, damage]`, monotonic, looped over a literal array with the offending input in the message |
| **`blockAndArmorBothApplyAndTheOrderIsBlockThenArmor`** | **the headline.** `Defense.applyDefense(Shield.applyBlock(d, 0.5), 20)` is strictly less than either mitigation alone, and lands on a pinned value the boot gate can read off rather than discover. |

Also `ShieldDefinitionTest` (validation, mirroring `WeaponDefinitionTest`), `ShieldRegistryTest`,
`ShieldLoreLinesTest`.

**Every expected number is produced by executing the expression and pasting what it printed.**
Never predicted from the algebra — `DefenseTest`'s own javadoc records a case where the algebra
would have demanded exact equality and failed on correct code.

---

## Paper — the mint

Reuse, not reinvention. The machinery weapons already have that transfers verbatim:

- **`WeaponItems.displayName(name, rarity)`** — public static, already does the MiniMessage→plain
  round trip so rarity colour always wins. Call it directly.
- **`RarityColors.of(rarity)`** — exhaustive switch, no default arm.
- **`EnchantItems.read/write/clear/activeLevel`** — the enchant container is item-agnostic.
- **`EnchantLore.lines(state, enchants)` / `EnchantLore.applied(base, lines)`** — `applied`
  returns `base` unchanged when there are no enchants, and prepends at index 0 so the rarity
  footer stays literally last.
- **`WeaponDurability.maxOf / damageOf / wear`** — these are *pure item* questions, not weapon
  questions (`maxOf`'s javadoc says so). Reuse them; do not duplicate.
- **`Durability.*`, `Unbreaking.consumes`, `BrokenNotice.notify`** — all already shaped for this.

### `paper/.../adapter/Keys.java`

One field, one line in the constructor: `shieldId` → `"shield_id"`, beside `weaponId`.

### `paper/.../weapon/ShieldItems.java`

Mirrors `WeaponItems`, minus everything a shield has none of:

```java
public static ItemStack mint(ShieldDefinition shield, AdapterContext adapters);
public static ItemStack remint(ItemStack old, ShieldDefinition current, AdapterContext adapters);
public static Optional<String> shieldId(ItemStack item, Keys keys);
public static Optional<String> blockingShieldId(Player player, Keys keys);  // reads getActiveItem()
```

`mint` sets the display name, the `shieldId` PDC, `setMaxStackSize(1)`, and the lore. **No
attribute modifiers and no `HIDE_ATTRIBUTES`** — a shield pins no attack stats, so there is
nothing to suppress and nothing to hide. `materialOf` falls back to `Material.SHIELD` on an
unknown material, the way `WeaponItems` falls back to `IRON_SWORD` — a give never crashes.

`remint` carries `shieldId`, then wear, then the **raw** enchant blob and rolled byte
independently (never decode/re-encode), then rebuilds lore — the order in `WeaponItems.remint`
is load-bearing and the same reasoning applies here.

> **Deliberate duplication.** `ShieldItems` and `ShieldLore` repeat structure from their weapon
> counterparts. That is the brief: keep it focused now, generalize to a shared
> `GearDefinition`/`GearItems` when armor lands and there are three call sites to factor rather
> than two. Leave a note saying so at the top of each file.

### `paper/.../weapon/ShieldLore.java`

`build(ShieldDefinition)` — no item state, the structural guarantee that lore is mint-time-only.
Order: block-DR stat line, flavor (gray + italic, the only italic block), blank, then the footer
`"<Rarity> Shield"` in `RarityColors.of(rarity)`. No element line (a shield declares none) and no
`WeaponClassLabel` (a shield has no class). Enchant lines are prepended by
`EnchantLore.applied(...)` in `ShieldItems`, above everything, so the footer stays last.

`paper/src/test/` already holds pure Component tests (`WeaponLoreTest`) — add `ShieldLoreTest`
alongside it.

### `paper/.../weapon/ShieldDurability.java`

Mirrors `WeaponDurability.applyWearOnUse`, with the same exemptions in the same order and for the
same reasons:

```java
public static final int WEAR_PER_BLOCK = 1;

public static void applyWearOnBlock(Player player, EquipmentSlot slot, Keys keys,
                                    CooldownTracker cooldowns) {
    // material not damageable -> return   (WeaponDurability.maxOf)
    // already broken           -> return
    // THE UNBREAKING SEAM: read the ACTIVE level off THIS STACK, draw here, decide in core
    //   if (!Unbreaking.consumes(EnchantItems.activeLevel(shield, keys, Unbreaking.ID),
    //                            ThreadLocalRandom.current().nextDouble())) return;
    // wear, write the stack back to its slot explicitly, updateInventory
    // just-broke crossing -> BrokenNotice.notify(player, cooldowns)
}
```

The slot comes from whichever hand vanilla was actually using, not from a guess — `getActiveItem()`
identifies the blocking stack exactly, and a shield is legal in either hand.

Threading: this runs on the `EntityDamageByEntityEvent` thread, which owns the victim — the same
guarantee `WeaponDurability.applyWearOnUse` relies on. `ThreadLocalRandom`, not `Math.random()`,
for the reason that class already records.

### `paper/.../content/ShieldLoader.java`

Mirrors `WeaponLoader`: `listFiles(*.yml)`, `Arrays.sort` for deterministic order, id from the
filename, per-file `RuntimeException` → `log.warning("Skipping malformed shield '…': …")` plus an
aggregate warning. Keys: `display_name`, `rarity` (default `common`), `material` (default
`shield`), `block_dr`, `flavor`.

### `paper/src/main/resources/content/shields/roundshield.yml`

One common shield, `block_dr: 0.5`, in the house content voice — every non-obvious key carries a
paragraph saying *why*, including the rejected alternative. `RpgPlugin.saveDefaultContent()` needs
no change: it enumerates `content/**.yml` out of the jar with `JarFile` and `saveResource(path,
false)` each, precisely so a new subdirectory ships without a list to update.

---

## Paper — the wiring

### `RpgPlugin.onEnable`

- Load: `this.shields = new ShieldLoader(getLogger()).loadAll(new File(contentDir, "shields"));`
- Add the count to the aggregate boot log line.
- **A zero-check that fails loudly**, the pattern the enchant loader already uses and that
  CLAUDE.md records twice: `if (shields.size() == 0) getLogger().warning(...)`. Finding zero is a
  defect, not a quiet no-op, and on an already-populated data folder it is indistinguishable from
  working.
- Warn if a shield id collides with a weapon id (see `/rpg give` below).
- Thread `shields` into `RpgCommand.build(...)` and into the `new RpgListeners(...)` constructor —
  both are single call sites in the one registration block.

### `/rpg give <id>` — extend, don't fork

Keep one `give` argument. Suggest weapon ids **and** shield ids; resolve weapons first, then
shields; mint through the matching `*Items.mint`. `EnchantRollItems.rollOnAcquire` is **not**
called for a shield — `EnchantRoll.roll` is keyed on `WeaponClass`, and shield enchant-gating is
explicitly Slice 2. The shield ships enchant-*compatible* (it carries the container), not
enchant-*rolled*.

The boot-time id-collision warning is what keeps "weapons first" from silently shadowing a shield.

### `/rpg enchant` must accept a held shield

This is forced by the boot gate: Unbreaking has to get onto the shield somehow, and
`RpgCommand.enchant(...)` currently hard-rejects anything without a `weapon_id`
(`RpgCommand.java:878-882`, *"Hold one of our weapons."*).

**This is not a one-line change, and it must not become a fork.** `enchant(...)` is
`WeaponRegistry`/`WeaponDefinition`-typed end to end, and every op arm (`SHOW`, `CLEAR`,
`CANDIDATE`, `LEVEL`, `ACTIVE`, `DEACTIVATE`) terminates in `finishEnchant`/`showEnchants` →
`WeaponItems.remint`. Duplicating the command body into a weapon arm and a shield arm would
double six branches and guarantee they drift.

**Contain the five WRITE arms in one place.** `CANDIDATE`, `LEVEL`, `ACTIVE`, `DEACTIVATE` and
`CLEAR` all end in `finishEnchant` → `WeaponItems.remint`. Add a single private remint-dispatch
helper keyed on the held stack's own tag — `weaponId` present → `WeaponItems.remint`, else
`shieldId` present → `ShieldItems.remint` — so every op branch keeps exactly one call site and the
command body stays shape-identical. The "refuse rather than half-edit" rule that guards a dangling
`weapon_id` today (`RpgCommand.java:887-892`) applies unchanged to a dangling `shield_id`: no
loaded definition means no remint, so refuse the edit rather than leave PDC and lore disagreeing.

**`SHOW` is descoped for shields in Slice 1 — it is not a remint and the helper does not cover
it.** `showEnchants` is `WeaponDefinition`-typed (`RpgCommand.java:1063`) and reaches
`EnchantEffectLine.of(enchantDef, level, definition.weaponClass())`, whose `heldClass` is
contractually never-null: the `DAMAGE` arm dereferences it through `WeaponClassLabel.of(heldClass)`
(`EnchantEffectLine.java:74-75`), an exhaustive switch with **no default arm**. A shield has no
`WeaponClass` — it will not even compile through that signature, and passing `null` crashes that
arm.

So: a held shield hitting `/rpg enchant show` replies *"weapon-only for now."* and returns. The
boot flow is `candidate` → `level` → `active` and never touches `show`. **Do not pass `null` into
`EnchantEffectLine`, and do not touch its shared switch** — the class axis is Slice 2's problem,
when shields get their own enchant gating.

The dev flow is: hold the shield in the **main hand**, `/rpg enchant candidate 0 unbreaking` →
`level 0 0 3` → `active 0 0`, then move it to the offhand to block.

### `RpgListeners` — the block rider

Inside `onMobMeleeAttack`, between the stat read and `applyDamage`, and **before**
`event.setDamage(TOKEN_DAMAGE)`:

```java
double incoming = adapters.stats().attackValue(attacker.getUniqueId());
ShieldBlock.Outcome block = ShieldBlock.resolve(victim, event, adapters.keys(), shields);
if (block.blocked()) {
    incoming = Shield.applyBlock(incoming, block.blockDr());
    ShieldDurability.applyWearOnBlock(victim, block.slot(), adapters.keys(), cooldowns);
}
event.setDamage(TOKEN_DAMAGE);
BukkitCombatant.of(victim, adapters).handle().applyDamage(incoming, attacker.getUniqueId());
```

`ShieldBlock` (paper) is the thin adapter that answers "did vanilla consider this a valid block,
with which of our shields, in which slot" — it holds the `@SuppressWarnings("deprecation")` for
the `DamageModifier` read, so the deprecation is confined to one file. All arithmetic lives in
core's `Shield`.

### `RpgListeners` — suppress vanilla's shield wear

```java
@EventHandler(ignoreCancelled = true)
public void onShieldItemDamage(PlayerItemDamageEvent event) {
    if (ShieldItems.shieldId(event.getItem(), adapters.keys()).isPresent()) event.setCancelled(true);
}
```

Scoped to **our** shields by the `shield_id` tag, the same boundary `/rpg durability` and the
weapon gates draw — an untagged vanilla shield keeps wearing normally.

**Assert the real thing, not a difference against an unmeasured baseline.** Vanilla shield wear
can scale with the damage blocked, so an un-suppressed count is not reliably `2N` and "we saw less
than 2N" would prove nothing. The claim to witness is the positive one: **with the cancel in
place, N blocks move the bar by exactly N** (`WEAR_PER_BLOCK`), and with Unbreaking III by roughly
`N/4`. Count it; do not infer it, and do not measure it against `2N`.

Also worth knowing: an **untagged vanilla shield gives zero custom protection** —
`blockingShieldId` is empty, so the full mob stat passes through, and vanilla's own block is
tokened away by `setDamage(TOKEN_DAMAGE)`. A player holding a plain shield is, mechanically, not
blocking at all. Out of scope for this slice; **record it in `NEXT.md` as a known** so it is a
decision someone took rather than a bug someone finds.

---

## Files

**New — core:** `combat/Shield.java`, `weapon/ShieldDefinition.java`, `weapon/ShieldRegistry.java`,
`weapon/ShieldLoreLines.java`, and their tests under `core/src/test/java/…` (mirroring
`combat/DefenseTest.java` and `weapon/WeaponDefinitionTest.java`).

**New — paper:** `weapon/ShieldItems.java`, `weapon/ShieldLore.java`, `weapon/ShieldDurability.java`,
`weapon/ShieldBlock.java`, `content/ShieldLoader.java`,
`resources/content/shields/roundshield.yml`, `paper/src/test/…/weapon/ShieldLoreTest.java`.

**Modified:** `paper/.../adapter/Keys.java` (one field), `paper/.../RpgPlugin.java` (load, count,
zero-check, collision warning, two call sites), `paper/.../command/RpgCommand.java` (`give`
resolves either; `enchant` accepts a shield), `paper/.../listener/RpgListeners.java` (the block
rider, the `PlayerItemDamageEvent` cancel, and the two temporary `[BLOCK]` witnesses).

---

## Verification

### Unit — the fast loop

```bash
./mvnw -pl core test          # after every core change
./mvnw clean package          # core + storage + paper, before booting
```

Report the actual `Tests run:` counts. **`BUILD SUCCESS` with no `Tests run:` line means zero
tests ran.** Baseline at `e5f0bd5`: core 457 / storage 17 / paper 308.

### Mutation — before believing any test guards anything

For each headline test, break the thing and watch it fail:

- Delete the `blockDr <= 0` guard in `Shield.applyBlock` → `aNegativeBlockDrIsGuarded…` must red.
- Drop the upper clamp → `aBlockDrAboveOneIsClamped…` must red.
- ~~Swap the composition order in the block-then-armor test's expression → must red.~~ **Run, and
  it does NOT red.** At raw 8 / DR 0.5 / defense 20 the two orderings are bit-identical, and
  everywhere else they differ by at most 2.8e-14 — below any epsilon worth asserting. The order is
  **not observable in the arithmetic** and no core test can guard it; it is fixed by the pipeline
  (block in the rider, defense a thread hop later in `CombatantStats.damage`). The test was renamed
  to `blockAndArmorBothApplyRatherThanOneShadowingTheOther` so it stops claiming an order it does
  not check, and the finding is recorded in the test body. Replaced by: **drop either factor from
  the composition → must red.**
- Remove the `Unbreaking.consumes` call from `ShieldDurability` → the wear test must red.

Per CLAUDE.md: `grep` for the marker to confirm the mutation **applied**, run `test-compile`
first (a mutation that does not compile is not a mutation), and **copy the file to the scratchpad
and restore from there — never `git checkout --`** a file with uncommitted work.

### Boot gate — `./scripts/dev-server.sh --refresh-content`

`--refresh-content` because `saveResource(path, false)` never overwrites; the new `shields/`
directory would copy anyway, but a stale deployed tree has silently swallowed a content change
on this repo before (commit `117168e`).

| # | Check | Expected |
|---|---|---|
| 1 | `/rpg give roundshield` | mints; name in the common rarity colour; lore shows the block line, flavor, and `Common Shield` last |
| 2 | Main hand: `/rpg enchant candidate 0 unbreaking` → `level 0 0 3` → `active 0 0` | enchant lines sit **above** everything, footer still last. `/rpg enchant show` on a shield replies *"weapon-only for now."* rather than throwing |
| 3 | Take an unblocked mob hit; record the heart-bar drop | baseline |
| 4 | Block the same mob frontally | **~half** of #3 |
| 5 | Equip diamond armor, block | **less than either** #3-armored or #4 alone — block-then-armor, matching the pinned core value |
| 6 | Take a hit **from behind** while holding block | **no reduction** — vanilla's frontal validity honoured |
| 7 | Shield down | **no reduction** |
| 8 | Block N times, count durability | bar moves by **exactly N** (`WEAR_PER_BLOCK`); with Unbreaking III, roughly N/4. Assert the count itself — not a delta against an un-suppressed run, whose wear can scale with damage blocked |
| 9 | Vanilla feedback | shield raise animation, block sound, knockback dampen all still play (we never cancel) |
| 10 | `[BLOCK]` log | block signal **present** on #4/#5, **absent** on #6/#7; `LOWEST` line present on every hit so "didn't fire" and "was cancelled" stay distinguishable |

Record the observations in `NEXT.md` as a dated boot record, then **strip both `[BLOCK]`
handlers** before merge.

**A guard that fires is not a hypothesis to argue with.** If #6 shows a reduction, or #8 counts
2N, or the `LOWEST` line never prints — that is the result. Test the explanation before believing
it.

---

## Branch and scope

Branch `feat/shields-block-dr` off `e5f0bd5`. Verified from the wire, not a local ref:
`git ls-remote --heads origin` → `e5f0bd5…` for `refs/heads/master`, equal to local `HEAD`.

This plan ships in the branch as **`PLAN-shields-slice-1.md`** at the repo root, matching the
existing `PLAN-*.md` convention.

Order of work: **core + its tests first**, then the mint, then the rider and the `[BLOCK]`
witness — the standing rule that the `core/` unit test precedes the `paper/` wiring.

**Out of scope (Slice 2):** Thorns, Bulwark, shield enchant-gating (`EnchantRoll` is keyed on
`WeaponClass`, and so is `EnchantEffectLine`'s inert-check; a shield has neither), `/rpg enchant
show` for shields, and a `ShieldRefresher` for the join/`/rpg refresh` path.
**Also deferred, and worth naming in `NEXT.md`:**

- Whether a *broken* shield should stop blocking — weapons have that gate, shields do not get one
  in this slice.
- A vanilla, untagged shield gives **zero** custom protection (see the wear section above). A
  known, not a bug.
