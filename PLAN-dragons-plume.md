# PLAN — The Dragon's Plume

**Status: PARTLY BUILT.** This document is the investigation, the rulings and the design — **it is no
longer ahead of the code, and saying so is the point of this line.** Landed since it was written:
**F** (homing in `core`), **F2** (the sight gate), **G** (`roundsRemaining`), **H1** (the draw, the
tracker and the tick — it fires nothing by design), and **`dragons_plume.yml`**, a first pass
authoring only what H1's boot needs. **H2 — the release — is not built.**

**AND ONE ROW HAS BEEN READ.** `GATE-plume-draw.md` H-1 carries a real observation, taken on a
`pling` the weapon no longer uses; the rest is still prediction. A plan that keeps claiming *nothing
booted* after a boot is the same falsified-account defect it warns about elsewhere.

**EVERY NUMBER A PLAYER EXPERIENCES IS RULED** — thirteen rulings, one of them an overturn, each named
where it lands (§2, and the closed sections of §7) — **and so is the charge sound** (R13's material,
§7.6). **What remains open is not a number:** §5's **homing constants**, which stay `INHERITED AND
UNJUDGED` with a gate row each.

---

## THE HEADLINE FINDING — AND IT IS NOW CLOSED, BY A LEVER THE DIALOG NEVER OFFERED

> **THIS IS THE POINTER. THE ACCOUNT IS §7.2** — the damage-per-second tables, both fencepost
> conventions, the reload sensitivity and the symbol discipline live there.

**The finding was: *clicking fast beat charging, so nobody would have used the charge.*** It was real
— at the values the weapon then had, tapping led the charged release by every measure.

**It is answered by three rulings taken together, and by none of them alone:**

```
R4'  a partial draw fires one arrow -- NO HOMING, 12 damage      (overturns R4)
R7   cooldown_ticks = 0, ruled                                   "fast shooting arrows is fine"
R9   reload_ticks   = 90                                         the tax that actually bites
```

**Redone in DAMAGE PER SECOND, because R4′ made arrows incommensurable — a charged arrow is 48 and
homes, a tap arrow is 12 and does not. RECOMPUTED under R1 as amended (1/3/5), and every figure here
moved except the tap:**

```
charged, five per release, c = 20          61.538 dmg/s      1200 per magazine
tapping at T = 3, vanilla's own floor       37.037 dmg/s       300 per magazine   charge +66.2%
tapping at T = 8, a rate a hand can keep    21.277 dmg/s       300 per magazine   charge +189.2%
```

**THE TAP DID NOT MOVE AND DID NOT NEED TO.** R4′ is one arrow at 12 for one round, which the
amendment does not touch; the charged path got **51.3% faster** because a maximum release now costs a
**60-tick** draw instead of 100. The margin went from **+9.8%** to **+66.2%** without anyone
re-pricing the tap.

**AND THE ARGUMENT A PLAYER ACTUALLY FEELS IS NOT THE RATE — IT IS THE AMMUNITION.** The same
twenty-five rounds are worth **1200 damage charged and 300 tapped: four times**, so the tap-spammer
runs dry four times as often for the same work, and it compounds with every reload. **That ratio is
UNCHANGED by the amendment** — it is damage per ROUND, and the amendment moved time, not rounds —
which is why it was the argument worth having.

> **THE SHAPE OF THE RISK HAS INVERTED, AND IT IS RECORDED AS CHOSEN RATHER THAN DISCOVERED.** The
> original finding was *the tap beats the charge, so nobody would charge*. At **+66%** the danger is
> the opposite one: **a tap nobody would ever use is a mechanic costing a trigger binding, a content
> block and a gate row for nothing.**
>
> **That is NOT a defect today, and the reason is not about rate at all.** R4′ exists so the weapon
> is **never dead at close range** — a player caught at three blocks needs something to do that is
> not a three-second hold. A shot that is deliberately worse still beats no shot. **The tap is priced
> to be a last resort and it now unambiguously is one**, which is the design working rather than
> failing.
>
> **It is written down because the two failures look identical in a table and opposite in play**, and
> because the next person to read *"the tap is 66% behind"* will otherwise read it as a defect
> somebody missed.

> **~~R9 IS LOAD-BEARING: below `reload_ticks` ≈ 71 the tap retakes the lead.~~ THAT COUPLING IS
> DEAD, AND IT DIED WITH THE AMENDMENT.** Recomputed at 1/3/5: the tie is at **`reload_ticks` = 4**
> (fencepost A) and **exactly 0** (fencepost B) — and at `T = 4` it is **negative**, meaning the tap
> cannot retake the lead at any reload whatsoever. **The charge now leads at every reload value
> anybody would author.**
>
> **R9 KEEPS ITS OTHER REASONS AND LOSES THIS ONE.** 90 ticks is still 3.60 ticks per round, still
> just under `quiver_stone`'s 3.78, and still the only thing pricing a tap-spammer's ammunition. What
> it is no longer is *the number holding the headline finding up*. **A stale load-bearing flag is
> worse than no flag** — it makes the next author afraid of a field for a reason that has stopped
> existing. Full working in §7.3.

### THE LESSON IS THE FRAMING, NOT THE ARITHMETIC

**The dialog asked *"how much do we tax the tap"* and offered three values of `cooldown_ticks`. Ben
answered on a different axis — *make the tap a different, worse shot*.** The cooldown was never the
only lever; the dialog presented it as though it were, and the answer that worked came from outside
the set of options on offer.

> **A DIALOG THAT OFFERS THREE VALUES OF ONE VARIABLE CAN HIDE THE VARIABLE.**

**The tell is available before the answer comes back: every option on offer moved the same field.**
When that is true, the question being asked is *"what value"*, and nobody has asked *"which
quantity"*. It is **name the quantity, and name the set of things that have it** (`CLAUDE.md`) turned
on the designer's own question instead of on an enchant.

> **AND THE FAMILY IT BELONGS TO IS UNCHANGED, WHICH IS WHY IT IS STILL AT THE TOP.** Q7's quantised
> input; Rapid Fire as a percentage of a quantised cooldown; Expanded Quiver as a percentage of an
> integer magazine; and now **a mechanic whose own cheaper alternative was better than it.** Every one
> was arithmetic nobody had done until somebody did.

---

## 0. THERE IS NO ANCESTOR FOR THIS MECHANIC. DO NOT GO LOOKING FOR A PORT

**The old repo's report is valuable for the ALGORITHM and useless for the PLUMBING, and the two
halves must not be mixed.** The old project's bow **deletes the vanilla draw** and fires an **instant
fan of three**. R1 keeps the draw and fires up to five. So the old repo is not a precedent for the
input, the charge, the cadence or the count — it is a precedent for **how an arrow steers** (§5) and
for **who it refuses to steer at** (R2). Nothing else crosses.

**And the hit path cannot cross either, by construction.** The old repo's entire on-hit apparatus —
`onManagedBowArrowHit` — exists because its arrows were vanilla `Arrow` entities whose damage rode
`EntityDamageByEntityEvent`. In this repo `BukkitCombatant.applyDamage` routes to the custom health
store and **never calls `entity.damage()`**, so that event never fires and there is nothing for such a
handler to attach to.

> **THE TREE ALREADY RECORDS THE CONSEQUENCE, FROM THE OTHER DIRECTION.**
> `HANDOFF-damage-system.md:176-182` names the old project's *"Dragon's Plume i-frame bypass"* as
> evidence that i-frame interaction is real in that style of system. **The old Plume needed an
> explicit bypass because its damage went through the vanilla event.** Ours cannot go through it, so
> the whole bypass question is **absent by construction rather than overlooked** — and a reader who
> ports the old hit path would be importing a problem this repo does not have.

**So the plan's shape is: the algorithm is inherited and unjudged; the plumbing is new here.**

---

## 1. THE DRAW-WITHOUT-ARROWS QUESTION — MEASURED, AND THE CONSTRAINT HOLDS

The question was raised as an inference and is answered here as a **measurement**, from the server
this repo actually boots rather than from recollection of how bows behave.

**What was executed.** `run/versions/26.1.2/paper-26.1.2.jar` is the patched, Mojang-mapped server
jar under the pinned `paper.version 26.1.2.build.74-stable`. Classes were extracted to the scratchpad
and disassembled:

```
javap -p -c -classpath <scratch> net.minecraft.world.item.BowItem
javap -p -c -classpath <scratch> net.minecraft.world.entity.player.Player
javap -p -c -classpath <scratch> net.minecraft.world.item.ProjectileWeaponItem
javap -p -c -classpath <scratch> net.minecraft.world.entity.LivingEntity
javap -p -c -classpath <scratch> net.minecraft.server.network.ServerGamePacketListenerImpl
unzip -p run/versions/26.1.2/paper-26.1.2.jar data/minecraft/enchantment/infinity.json
```

### 1.1 Does the pinned Paper let a bow draw with no arrow in inventory? **NO.**

`BowItem.use`, read off the bytecode (offsets 10-50):

```java
ItemStack bow  = player.getItemInHand(hand);
boolean hasAmmo = !player.getProjectile(bow).isEmpty();          // 10-24
if (!player.hasInfiniteMaterials() && !hasAmmo) return FAIL;      // 26-41
player.startUsingItem(hand);                                      // 42
return CONSUME;                                                   // 47
```

`Player.getProjectile` checks **OFF_HAND then MAIN_HAND** (`ProjectileWeaponItem.getHeldProjectile`),
then walks the whole inventory container, and at the end returns a **fresh `Items.ARROW` only when
`hasInfiniteMaterials()`** — otherwise `ItemStack.EMPTY`.

**So a survival player holding a Plume with twenty-five rounds loaded and no loose arrows gets
`FAIL`, `startUsingItem` is never called, and there is no server-side draw state at all.** The
operator's inference was correct.

> **THE ANSWER DOES NOT REST ON ANYTHING CLIENT-SIDE, AND THAT IS WHY IT IS DECISIVE.** One could
> argue about whether the client plays the pull-back animation anyway. It does not matter: with
> `startUsingItem` unreached, the server has no active item, so `getActiveItemUsedTime()` is 0,
> `hasActiveItem()` is false, `releaseUsingItem()` never runs and **no release event of any kind is
> delivered**. R1's tracker has nothing to start and nothing to end. The mechanic has no input.

> **AND THE REPO'S OWN Q7 READING IS CONSISTENT WITH IT, WHICH IS CORROBORATION AND NOT PROOF.**
> `GATE-q7.md` measured `hunters_bow` (`material: bow`) at **56 inputs over a 276-tick hold, min 4t**.
> A client that had latched into a draw sends **one** use packet, not fifty-six; the repeat exists
> because the use failed. That is an inference about the client from a server-side count, so it is
> filed as agreement with the bytecode rather than as a second measurement. The `4t` floor matching
> vanilla's `rightClickDelay` is the prior `GATE-q7.md` recorded **in advance**, for this reason.

### 1.2 Does Infinity change it? **NO — and it still requires one arrow.**

Measured twice over, in the code and in the shipped data pack:

- `ProjectileWeaponItem.useAmmo` calls `EnchantmentHelper.processAmmoUse` and uses the result **only
  as the count to remove**. It is reached from `draw(...)`, which runs **at release**.
- `data/minecraft/enchantment/infinity.json` in the pinned jar is exactly one effect:
  `minecraft:ammo_use → set 0.0`, conditioned on the ammo item being `minecraft:arrow`.

**Infinity governs CONSUMPTION, never PRESENCE.** The `use()` gate above is upstream of it, and so is
`BowItem.releaseUsing`, which **re-checks `getProjectile(bow).isEmpty()` and returns with no shot**
(offsets 18-35). An Infinity Plume still needs one arrow in the bag; it simply never spends it.

> **AND A RELATED MEASUREMENT THAT PRICES THE OBVIOUS ALTERNATIVE.**
> `EntityShootBowEvent.setConsumeItem(false)` **is not read anywhere on the bow path.** Counted:
> `shouldConsumeItem()` appears **0** times in `BowItem`, **0** in `ProjectileWeaponItem`, **0** in
> `CraftEventFactory` — and **2** times in `CrossbowItem`. The consumption has already happened inside
> `draw(...)` before the event is constructed, so **neither setting the flag nor cancelling the event
> gives the arrow back.** Keeping an arrow alive across releases means Infinity, or handing one back
> ourselves.

### 1.3 Can a plugin force or fake the draw state, and at what cost?

**Three routes exist. None is free, and one of them is documented to fail in exactly our input
condition.**

| route | what it is | cost, measured |
|---|---|---|
| **A · Carry one arrow** | the vanilla gate is satisfied the ordinary way | one arrow somewhere in the bag. It is **eaten on every release** unless the Plume carries Infinity (§1.2). Needs no new API and no experimental surface. |
| **B · Force the draw** | `LivingEntity.startUsingItem(EquipmentSlot)` — present in the pinned API, and `CraftLivingEntity` calls the NMS method **directly, bypassing `BowItem.use` and therefore the ammo gate** | the API's own javadoc: *"When used on a player, the client will stop using the item **if right click is held down**."* That is R1's input, named. Also `@ApiStatus.Experimental`. **Unproven; needs a boot probe before any design leans on it.** |
| **C · Change the material** | `DataComponentTypes.CONSUMABLE` + `ItemUseAnimation.BOW` gives any item a bow-draw animation of arbitrary length with **no ammo requirement**, and `ITEM_MODEL` can render a bow on a non-bow item | **`material: bow` is settled**, so this needs a ruling to reopen. And it cannot be done ON a bow: `BowItem` **overrides `use()`** and its bytecode never consults `CONSUMABLE`, so the component is inert on a bow-material item. Also: `grep -rn "DataComponent" paper/src` returns **zero** — this would be the project's first data component, from an Experimental family. |

**Route A is the recommendation, and the reason is that it is the only one with no unknown in it.**
B may work and may not, and the way to find out costs a boot. C costs a settled ruling plus a new API
family.

> **RULED: R5 TAKES ROUTE A, IN THE OFF-HAND, AND PARKS THE REST.** See §2 for the ruling, the
> mechanism that makes the off-hand the right slot, the fact that it carries **no trigger**, and what
> the player is not told. **B and C stay written down** — a parked workaround whose alternatives were
> deleted is a decision nobody can revisit.

> **AND ROUTE A IS CHEAPER HERE THAN IT WOULD HAVE BEEN A WEEK AGO, BECAUSE SLICE E ALREADY SHIPPED
> THE ARROWS.** `QuiverAmmo` (merged `c7d9667`) makes **plain `minecraft:ARROW` the thing that loads
> every quiver**, one arrow per round. A Ranger carrying a Plume is already carrying arrows — the
> weapon's ammunition and the vanilla gate's ammunition are **the same item**.
>
> **Two measured details make the fit better than it first looks, and one makes it worse:**
>
> - `QuiverAmmo.sources` walks **`getStorageContents()`**, which excludes the **off-hand**. Vanilla's
>   `getHeldProjectile` checks the off-hand **first**. So **an arrow in the off-hand satisfies the
>   draw gate and is invisible to every quiver reload** — it cannot be swallowed by a reload that
>   needed one more round.
> - Creative is already special-cased in `QuiverAmmo.supply`, and creative also short-circuits the
>   vanilla gate via `hasInfiniteMaterials()`. The two agree without being told to.
> - **AND THE FAILURE MODE IS MADE MORE LIKELY, NOT LESS.** A player who loads all twenty-five rounds
>   has **spent** those arrows — the reload consumes them 1:1. The state the operator was worried
>   about, *full quiver and no loose arrows*, is now **the normal end state of a reload**, not an
>   exotic one. **This is the thing to rule on, and §7 puts it to Ben.**

### 1.4 Is there a signal we can read WITHOUT a real draw — and does R1 survive on it?

**Yes to the signal. Partly to R1.**

`ServerGamePacketListenerImpl.handleUseItem` calls `CraftEventFactory.callPlayerInteractEvent`
(offsets 252 and 385) **before** `ServerPlayerGameMode.useItem` (offset 512). **So
`PlayerInteractEvent` is delivered on every right-click regardless of ammo**, and the held-repeat
stream `GATE-q7.md` measured is available on a bow exactly as it is on a crossbow.

**What survives on it:** a plugin-side charge clock, the per-second arrow accrual, the rising tick,
the cap at rounds remaining, and the release. All of R1's *mechanism*.

**What does not:** the draw itself. There is **no pull-back animation, no charge state on the client,
and no vanilla release event** — the player holds a bow that does not move. R1 says *"the bow draws
normally"*; on this route it does not draw at all. **That is a different weapon, and it is the
operator's call, not a detail.**

> **AND THE TWO INPUT MODELS ARE MUTUALLY EXCLUSIVE, WHICH IS WHY THIS CANNOT BE A FALLBACK BOLTED
> ONTO ROUTE A.** The 4-tick repeat exists *because* the client's use attempt fails. Give the player
> an arrow and the client latches into the draw and **stops re-sending** — the repeat stream dries up
> at exactly the moment the draw starts working. A design that reads both signals reads whichever one
> the player's inventory happens to select. **Pick one.**

---

## 2. THE RULINGS — THIRTEEN LIVE, ONE OVERTURNED, AND THE REASONING R1 OVERTURNED

**Operator's, all final. Do not re-derive them from the code.**

- **R1′ · THE VANILLA DRAW STANDS, AND THE CHARGE IS THREE STEPS OF TWO.** The bow draws normally. At
  **full** charge a tracker starts: **every second adds TWO arrows**, with a short sound on each,
  **rising in pitch** with the step reached. On release it fires the tracked amount.

  ```
  full draw        ->  1 arrow    tick 1
  +1 second        ->  3 arrows   tick 2
  +1 second more   ->  5 arrows   tick 3     <- maximum, TWO seconds past full draw
  ```

  > **THIS IS AN AMENDMENT TO R1, NOT A RESTATEMENT, AND THE THING IT REPLACED IS WRITTEN DOWN.**
  >
  > ```
  > R1   every second adds ANOTHER arrow, to five    5 steps, 5 sounds, FOUR seconds  -- superseded
  > R1'  every second adds TWO, to five              3 steps, 3 sounds, TWO seconds   -- RULED
  > ```
  >
  > **Ruled after HEARING the weapon, not on paper.** The one-per-second ladder was designed and
  > costed before anything had ever ticked on a server; the amendment came from listening to it.
  > **A silently rewritten ruling reads as one nobody ever questioned**, and the original was a real
  > choice made for real reasons — it is superseded, not mistaken.

  > **TWO RULINGS LOOK LIKE THEY SHOULD HAVE MOVED WITH IT AND DID NOT. SAYING SO IS PART OF THE
  > AMENDMENT**, because both are the obvious thing to "fix" next and both are already correct:
  >
  > - **R6 · FULL CHARGE IS ARROW 1 — UNCHANGED.** The first step still yields exactly one arrow.
  >   **Only the INCREMENT moved**, +1 to +2, and the increment has never applied to the first step;
  >   that is R6's entire content. Reading the new increment into the base gives 2/4/6, which is not
  >   the ruling and is not five.
  > - **§7.5 · THE CHARGE SECOND `c` = 20 — UNCHANGED.** A step is still one second, so every
  >   boundary sits at exactly the tick it always did. What changed is the step's **YIELD** (+1 → +2)
  >   and the **NUMBER** of steps (5 → 3). **`c` was never the quantity in question** — do not
  >   re-open it, and do not read a shorter charge as a shorter second.
  >
  > **The two together are why the amendment cost no code in `DrawCharge` beyond the yield:** the
  > tick boundaries, the full-draw constant and the off-by-one all survived it untouched.
- **R2 · MOBS ONLY.** The arrows do not seek players. The old repo's other exclusions — **Endermen**
  (neutral until provoked) and **summons owned by the shooter or their party** — carry forward as
  **candidates to be ruled later, not as settled**.
- **R3 · THE TRACKER IS CAPPED AT ROUNDS REMAINING.** It never climbs past what the quiver can pay
  for, and the sound never promises an arrow that is not coming. **The tracker reads the magazine
  LIVE** — ruled, and more work than the alternative deliberately.

  > **R3a · THE CAP CAN NOW LAND BETWEEN STEPS, AND THE TICKS STOP WHEN THE NEXT STEP IS
  > UNAFFORDABLE.** Operator's ruling, made because R1′ created the case.
  >
  > **Under 1/2/3/4/5 the cap always landed ON a step** — every round count was some step's exact
  > yield, so there was nothing to decide. **Under 1/3/5 it can land between:** two rounds pays for
  > step 1 and not step 3; four pays for step 2 and not step 3.
  >
  > ```
  > rounds 2   ->  one tick,  ONE arrow,    one round STRANDED
  > rounds 4   ->  two ticks, THREE arrows, one round STRANDED
  > ```
  >
  > **The release fires exactly the tracked step. The remainder is stranded until a reload.**
  >
  > > **WHY NOT `min(step, rounds)`, WHICH STRANDS NOTHING: THREE CHANNELS NOW REPORT THE SAME
  > > QUANTITY TO TWO AUDIENCES.** The **sound** tells the shooter which step they are on; the **fan
  > > width** tells everyone watching; and the **arrows** are the thing itself. A release that fires
  > > more than it sounded, and wider than it fanned, puts all three in disagreement.
  > >
  > > **A disagreement between readouts is worse than a stranded round** the player clears with a
  > > reload they were going to make anyway. Stranding is visible and recoverable; a sound that lied
  > > is neither.
  > >
  > > **Note the fan is what makes this a THREE-channel problem rather than a two-channel one.** Before
  > > the fan ruling the only witnesses were the shooter's ear and the arrows; the fan put the same
  > > quantity in front of everyone else in the fight.
  >
  > **OVERTURNABLE, AND CHEAP EITHER WAY** — one comparison in `DrawCharge.affordableStep`, with its
  > own core row. Ruled by the operator rather than by Ben.
- **R4′ · A PARTIAL DRAW FIRES ONE ARROW, WITH NO HOMING, AT 12 DAMAGE.** Costs one round.

  > **THIS IS AN OVERTURN OF R4, NOT AN AMENDMENT, AND THE THING IT REPLACED IS WRITTEN DOWN.**
  >
  > ```
  > R4   a partial draw fires one arrow, AND IT STILL HOMES      -- superseded
  > R4'  a partial draw fires one arrow, NO HOMING, 12 damage    -- RULED
  > ```
  >
  > **R4 was not a default. The homing-versus-no-homing question was put to Ben when R4 was made, and
  > he chose homing.** He has now chosen the option he previously declined, and added a damage cut.
  > **A silently rewritten ruling reads as one nobody ever questioned**, which would make the next
  > reader think the earlier choice was never considered. It was.

  > **AND THE MORE USEFUL HALF IS WHY THE DIALOG FAILED TO REACH THIS.** §7.2 asked *"how much do we
  > tax the tap"* and offered three values of `cooldown_ticks`. Ben answered on a **different axis** —
  > *make the tap a different, worse shot*. The cooldown was never the only lever, and the dialog
  > presented it as though it were.
  >
  > > **A DIALOG THAT OFFERS THREE VALUES OF ONE VARIABLE CAN HIDE THE VARIABLE.**
  >
  > **The tell is available before the answer comes back:** every option on offer moved the same
  > field. When that is true, the question being asked is *"what value"*, and nobody has asked
  > *"which quantity"*. It is the same shape as **name the quantity, and name the set of things that
  > have it** (`CLAUDE.md`) — asked of the designer's own question rather than of an enchant.

- **R5 · ONE ARROW IN THE OFF-HAND, FOR NOW.** The draw gate of §1 is satisfied by keeping a single
  plain arrow in the off-hand.

  **WHY IT WORKS IS A MECHANISM, NOT A COINCIDENCE, AND THAT IS THE PART TO RECORD.** Vanilla's
  `ProjectileWeaponItem.getHeldProjectile` checks **OFF_HAND first**; `QuiverAmmo.sources` walks
  **`getStorageContents()`, which excludes the off-hand**. So the arrow **satisfies the draw and no
  reload can ever eat it** — the two halves are in different sets by construction. **A later change
  to that walk would break the Plume silently**, with nothing to report it, which is why the reason is
  written here rather than left to be re-derived from the fact that it happens to work.

  > **RECORDED AS A KNOWN WORKAROUND WITH NO TRIGGER.** The operator's words: *"in the future we will
  > have a better solution."* **No grep-evaluable condition is manufactured for this one, because
  > there is no honest one** — it is not waiting on a fact about the tree, it is waiting on a decision
  > nobody has made. **A park with a fake trigger is worse than one that says NO CONDITION; THIS NEEDS
  > DECIDING**, because the fake one looks like it will fire.

  **AND WHAT THE PLAYER HAS TO KNOW, WHICH NOTHING TEACHES THEM.** Measured against the tree:
  `WeaponLore` renders no such line, there is no notice path for it, and the vanilla failure is
  silent — the bow simply does not move. **So: nothing currently tells them, and a legendary bow that
  will not draw is indistinguishable from a broken item.** That is the cost of R5 as it stands, and
  it is the strongest argument for the better solution the ruling defers.

- **R6 · FULL CHARGE IS ARROW 1.** Reaching full draw gives one arrow. **This replaces §7.0's flagged
  assumption with a ruling** — and it was *Reading A*, the one every figure was computed under.

  > **R6 SURVIVED R1′ UNTOUCHED, AND IT IS THE RULING MOST LIKELY TO BE "FIXED" BY MISTAKE.** Its
  > second clause used to read *"each further second adds another, to five"*; that clause is R1's and
  > it is now *adds TWO*. **R6 itself is only ever about the FIRST step** — that reaching full charge
  > GIVES an arrow rather than starting a count.
  >
  > **The increment has never applied to it, which is why +1 → +2 leaves it alone.** In code that is
  > `1 + (step - 1) * ARROWS_PER_STEP` and not `step * ARROWS_PER_STEP`; the latter is 2/4/6, which
  > is neither ruling and does not end at five. `DrawChargeTest` has the row.

- **R7 · `cooldown_ticks` = 0, AUTHORED AS A RULING.** *"Fast shooting arrows is fine."* **The tap is
  gated by being a worse shot, not by a timer.**

  > §7.2 had already written the condition this satisfies: *"if the draw is meant to be the only
  > gate, the answer is `0` and it should be authored as a RULING — not left at 0 by omission,
  > because the headline finding is exactly the consequence somebody would later fix."* **It is now
  > ruled, so the `0` in the content file is a decision with a name on it** rather than an absent key.

- **R8 · `attack_damage` = 48 PER CHARGED ARROW → 240 IN ONE INSTANT at full charge.** Taken on the
  **instantaneous-parity** anchor, **with §7.1's spike paragraph in front of him** — so the spike is
  **chosen, not overlooked**, and that paragraph stays in the file beside the ruling rather than
  being tidied away now that the number exists.

- **R9 · `reload_ticks` = 90.** 4.5 seconds; **3.60 ticks per round**, just under `quiver_stone`'s
  3.78 — so **the Plume is no longer the cheapest ammunition in the project.**

  > **THIS NUMBER NOW DOES MORE WORK THAN IT LOOKS LIKE IT DOES, AND §7.3 CARRIES THE ACCOUNT.** With
  > `cooldown_ticks` at 0, **the reload is the only thing pricing a tap-spammer** — they meet it far
  > sooner per unit of damage dealt. **Measured in §7.2: lowering it back below ~71 ticks hands the
  > lead back to tapping.**

- **R10 · QUICK TAPS KEEP VANILLA'S FLOOR.** Under **3 ticks**, nothing fires —
  `getPowerForTime(t) < 0.1`, measured in §3.1.

  > **SO §3.1's FINDING IS A FOOTNOTE ON R4′ RATHER THAN A CHANGE TO IT**, and
  > `PlayerStopUsingItemEvent` is **not** pressed into serving `t < 3`.
  >
  > > ### ⚠ THE RULING STANDS. ITS JUSTIFICATION DID NOT SURVIVE H1, AND THE FLOOR IS OURS NOW
  > >
  > > **R10 was ruled as *keep vanilla's behaviour*, on the reasoning that the floor could be
  > > INHERITED — *"it can come from the platform instead of from a second implementation of the same
  > > rule."* H1 made that impossible, and H1 shipped after R10 was written.**
  > >
  > > `PlumeDraw.onRelease` calls `clearActiveItem()`, so `LivingEntity.releaseUsingItem`'s re-read
  > > yields EMPTY and **`BowItem.releaseUsing` never runs.** The power gate is inside that method.
  > > **The gate never runs, so the floor never applies, so there is nothing to inherit** — and
  > > **H-3 measured exactly that on a server**, reading *arrow YES, log YES*.
  > >
  > > **THE RULING IS UNCHANGED — under 3 ticks, nothing.** What changed is its CHARACTER: an
  > > inherited behaviour cannot drift, and a copied number can. It is now
  > > `DrawRelease.MIN_RELEASE_TICKS`, with §3.1's measurement beside it, a core row pinning it
  > > against a transcription of vanilla's own curve, and `MUT-FLOOR` to prove the row bites.
  > >
  > > **A re-implemented `ticksHeldFor >= 3` guard is no longer the duplicated-rule shape this repo
  > > treats as a defect**, because there is no longer a first implementation for it to duplicate.

- **R11 · REACH ≈ 300 BLOCKS** — the old repo's **120-tick** lifespan at **speed 2.5**.

  > **RULED AGAINST §7.4's OWN ARGUMENT, WHICH IS WHAT MAKES IT A RULING RATHER THAN AN
  > INHERITANCE.** §7.4 put the case *against*: 300 is **1.9× the longest reach in the project** and
  > **past render distance**, so the weapon can kill a thing its wielder cannot see. **Ben read that
  > and took *"never gives up"* anyway.** *Inherited because nobody looked* and *chosen over an
  > argument* look identical a month later and age in opposite directions, so the file says which
  > this is.

- **R12 · THE ARROW DROPS LIKE A NORMAL ARROW.** `gravity` from the shipped band — **`0.05`
  authored**, because `hunters_bow` is **the only bow in the project** and the 0.03 figures belong to
  staves. **The ruling's own words name the weapon class the value should come from** (§7.4).

  > **AND IT IS THE HALF OF THE PAIR THAT KEEPS R11 HONEST.** With gravity continuing on a targetless
  > arrow (§3.3), a stray shot is in the ground at **22.5 blocks** fired flat and **129** at 45° — so
  > **the 300-block leash binds only for arrows actually chasing something.** Computed, not asserted:
  > at 0.03 the 45° stray arrow is still airborne when the leash expires, and the claim weakens.

- **R13 · THE STEPS MUST BE TELLABLE APART BY EAR.** A player holding the draw must be able to
  **release on the step they wanted without looking at anything**, so the pitch mapping is authored
  for **SEPARATION, not subtlety** — the geometric row in §7.6, every step the same RATIO, rather
  than the linear one whose top step is smaller than its bottom one.

  > **THREE STEPS NOW, NOT FIVE — R1′ — AND THE RULE IS RE-EVALUATED RATHER THAN SAMPLED.** The
  > ladder is `0.8 × r^(n-1)` with **`r = (2.0/0.8)^(1/2) = 1.5811`**, not three of the old five
  > values. **Roughly EIGHT semitones a step instead of four.** Taking three of `0.80 1.01 1.27 1.59
  > 2.00` would have kept the numbers and lost the property they were chosen for, since no three of
  > them are evenly spaced AND span the range.
  >
  > **THE SOUND RULING SURVIVES THE AMENDMENT INTACT. Ben ruled the TIMBRE, not the ladder** — he
  > heard that drum and it suits the weapon, and nothing about three steps rather than five bears on
  > which drum it is.

  > **AND R13's SOUND IS RULED: `block.note_block.hat`**, 2026-09-14, on feel — Ben heard it and it
  > suits the weapon. It is the third key the tick has worn: `pling`, withdrawn on a listen (*"the
  > piano doesn't suit it, try snare or kick"*), then `basedrum`, the kick, now superseded.
  >
  > **THE KICK'S ARGUMENT IS WITHDRAWN, NOT REWRITTEN ONTO THE HAT.** It ran: *a snare's sharper
  > transient is easier to count as discrete hits, while a kick carries the rise more legibly and so
  > tells you which step you are on.* That argument **selected a key that is no longer ruled**, and
  > the ruling that replaced it gave a different kind of reason. Retargeting it would put an argument
  > in Ben's mouth.
  >
  > **THE RULING IS ON FEEL AND DOES NOT DISCHARGE R13.** R13's property is that a player can NAME
  > THE STEP by ear. **No position trial has been run on any candidate** — `GATE-plume-draw.md`'s
  > H-1b is still OPEN, and what it owes is the position reading on this key.

  > **AND P6 GAINS A SECOND HALF THAT ITS FIRST CANNOT ANSWER.** Not only *is the top of the range
  > still pleasant*, but **can a person NAME THE COUNT WITHOUT LOOKING.** That is the ruling's actual
  > test, and it is a different question from whether the sound is nice. **The 2.0 ceiling stays
  > flagged as outside knowledge this machine cannot measure.**

> **AND THE HOLE THE LAST THREE LEFT BETWEEN THEM IS RULED IN §3.3** — *what a TARGETLESS arrow does
> past 15 blocks*, which is **every shot that misses**. That one is the operator's rather than Ben's,
> and it is marked overturnable where it is made.

**Settled previously and unchanged:** `legendary`, `void`, `ranger`, `material: bow`,
`quiver_size: 25`, **one round per arrow** — so five arrows is five rounds and twenty-five is five
maximum releases. **An empty quiver does nothing: the weapon is dead.**

> **TWO CONSEQUENCES OF THE SETTLED SET THAT NOBODY HAS HAD TO LOOK AT YET, BOTH MEASURED:**
>
> - **The Plume would be the project's FIRST legendary.** `Rarity.LEGENDARY` exists in the enum;
>   `grep -rn "^rarity:" content/weapons/*.yml` returns common ×2, uncommon ×4, rare ×3, exotic ×3 and
>   **legendary ×0**. So its tier colour has never been rendered on a client by anything.
> - **`void` draws a glyph on every damage number.** `void.yml` is two lines and its
>   `damage_symbol` is `<dark_purple>✧</dark_purple>`. A five-arrow release therefore puts **five
>   purple-sparkled numbers up at once**. It accrues nothing — the anchored sweep
>   `grep -n "^applies_status" content/elements/*.yml` returns **`fire.yml` alone** — so the
>   fast-accrual worry does not arise.

### 2.1 THE REASONING R1 OVERTURNED, RECORDED RATHER THAN SOFTENED

**The old repo deleted the vanilla draw for a stated reason: *"the Fire Rate stat could never speed
it up."*** R1 accepts that cost **knowingly**, and the consequence is stated in the words it deserves:

> **THE PLUME'S CHARGE IS THE FIRST PLAYER-FACING DURATION IN THIS PROJECT THAT NO STAT, GEAR OR
> ENCHANT CAN MOVE.**

**It is the Q7 family again, one rung further out.** Rapid Fire was a percentage of a *quantised*
cooldown; Expanded Quiver a percentage of an *integer* magazine; **this is a headline mechanic
anchored outside the stat system entirely.** The first two were caught by *name the quantity, and
name the set of things that have it* (`CLAUDE.md`). This one is not a miss — it is the same question,
asked and answered the other way.

**ACCEPTED FOR FEEL, NOT OVERLOOKED.** Ben's rising-pitch tick is the mitigation and deserves naming
as one: **it does not make the second scalable, but it makes the duration LEGIBLE**, which is most of
what a stat would have bought.

**THE TRIGGER FOR REVISITING, as a condition rather than as "later":** **the first ranged stat
authored to modify draw or charge time.** On that day this weapon is **the one that cannot receive
it**, and this section is what that author should read first.

---

## 3. THE ARCHITECTURE RULING — OUR PROJECTILE, NOT A STEERED VANILLA ARROW

**Operator's ruling, and it is his rather than Ben's.**

`type: projectile` already ships with `speed`, `gravity` and `max_lifetime_ticks`, and every hit lands
at **`CastExecutor.java:597-598`** — `detonate`, the single on-hit site shared by ray, projectile and
volley. **Homing becomes a NEW FIELD ON A SHAPE WE ALREADY HAVE**, rather than a damage bridge built
for one weapon.

**THE FEEL COST IS REAL AND BEN CAN OVERTURN IT: no arrow sticking in a wall, nothing to pick up off
the ground.** Named here so the overturn is cheap if he wants it — the alternative is a vanilla
`Arrow` plus a damage bridge that exists for exactly one weapon, and `§0` is what that costs.

### 3.1 KEEP THE DRAW, DISCARD ITS ARROW — AND NEITHER REPO DOES THIS TODAY

**The vanilla draw is an INPUT DEVICE.** It gives the animation, the charge state and a real release
event. **The vanilla `Arrow` it would produce is cancelled, and our projectiles are spawned in its
place.**

**Say it plainly, because a reader will otherwise assume one of the two precedents applies:** the old
repo cancels **the draw and the shot**; this repo has **never had a draw at all**. This combination is
new to both.

**The two seams, measured, both ordinary Bukkit events on the main thread — no packet listener, so
the threading rule in `CLAUDE.md` is not in play:**

| event | when | what it carries | our use |
|---|---|---|---|
| `EntityShootBowEvent` | only when vanilla decides to shoot — so **`t >= 3` and an arrow present** | `getForce()`, `getProjectile()` | **the shot, AND the suppressor.** Cancelling calls `Entity.remove()` on the arrow before it is added to the world. |
| `PlayerStopUsingItemEvent` | **every** release, **before** `ItemStack.releaseUsing` (`LivingEntity.releaseUsingItem`, event at offset 64, item at 84) | `getTicksHeldFor()` = `getTicksUsingItem()` = `useDuration − remaining`, counting **up** | **the tracker's stop.** Not cancellable, and **not the shot** — see R10. |

> **R10 REVERSED THIS TABLE, AND THE REVERSAL IS WORTH RECORDING BECAUSE THE RULING MADE THE DESIGN
> SIMPLER RATHER THAN NARROWER.**
>
> **The measured dead zone.** `BowItem.releaseUsing` returns with no shot when
> `getPowerForTime(heldTicks) < 0.1` (offsets 54-65). The curve is `((t/20)² + 2(t/20)) / 3`:
>
> ```
> t = 2   power 0.0700   NO SHOT, and no EntityShootBowEvent
> t = 3   power 0.1075   shoots
> ```
>
> **The earlier draft hung the shot on `PlayerStopUsingItemEvent`** precisely so that a sub-3-tick tap
> could still fire — treating vanilla's floor as something to work around. **R10 rules the floor
> KEPT**, so it was to be a thing to inherit: hanging the shot on `EntityShootBowEvent` would get it
> **from the platform, with no second implementation of the same rule.**
>
> > **~~AND THAT IS HOW IT IS IMPLEMENTED.~~ IT IS NOT, AND THE SHOT HANGS ON
> > `PlayerStopUsingItemEvent` AFTER ALL — H1 SETTLED IT THE OTHER WAY.** `PlumeDraw.onRelease`
> > clears the active item, so `BowItem.releaseUsing` never runs and `EntityShootBowEvent` is never
> > raised for one of our bows. **This table's first row describes an event the Plume does not
> > receive**, and the guard that watches for it (`suppressManagedBowShot`) exists precisely to shout
> > if it ever does.
> >
> > **So the floor IS a second implementation**, `DrawRelease.MIN_RELEASE_TICKS` — not because
> > anybody chose duplication, but because the thing it would have duplicated no longer executes.
> > See R10's entry in §2 for the full account.
>
> **`PlayerStopUsingItemEvent` is still needed, and not for the shot.** A hold that ends with **no**
> release — under 3 ticks, a hotbar swap, a death — fires it and **not** the bow event, so it is the
> only signal that can **stop the charge tracker and its sound**. Without it a dry release leaves a
> tracker ticking.
>
> **The arrow COUNT comes from neither event.** `getForce()` saturates at `1.0` after 20 ticks
> (§3.2), so it **cannot distinguish a 20-tick hold from a 100-tick one** — the count is the
> tracker's, which has to exist anyway to play the per-second sound.

### 3.2 TWO MEASURED CONSTANTS THE DESIGN SITS ON

```
BowItem.MAX_DRAW_DURATION = 20        full charge at 20 ticks held; getPowerForTime(20) = 1.0 exactly
BowItem.getUseDuration    = 72000     one hour -- so the hold NEVER times out on its own,
                                      and getActiveItemUsedTime() keeps counting past full charge
```

**The second is what makes R1 implementable without fighting the platform:** the tracker can simply
read how long the bow has been held, for as long as the player holds it.

### 3.3 A TARGETLESS ARROW KEEPS ITS BALLISTIC BEHAVIOUR — **THE COMMON CASE, AND IT WAS UNRULED**

**Three rulings met and left a hole between them, and the hole is every shot that misses:**

```
§5    a null target FLIES STRAIGHT ON
§7.4  ballistic for the first 15 blocks, steering afterwards
R12   it falls like an arrow
```

**So what does an arrow with NO TARGET do past 15 blocks — keep falling, or sail flat for up to 300
blocks?** Nothing ruled it, and **it is the common case**: a homing arrow only homes when there is
something to home at.

> **THE RULING IS THE OPERATOR'S, NOT BEN'S, AND IT IS OVERTURNABLE.**
>
> **A TARGETLESS ARROW KEEPS ITS BALLISTIC BEHAVIOUR. GRAVITY CONTINUES.** It behaves like an arrow
> until something to chase appears — the least surprising answer, and the one a player already
> predicts from every other bow they have used.

**AND THE CLAIM MADE FOR IT IS THAT IT MAKES R11 AND R12 WORK TOGETHER RATHER THAN FIGHT: under
gravity a stray arrow reaches the ground long before 300 blocks, so THE LONG LEASH ONLY BINDS FOR
ARROWS ACTUALLY CHASING SOMETHING.** *The reach Ben ruled is a reach for seeking, not a licence for
stray arrows to cross the map.*

**THAT CLAIM IS COMPUTED HERE RATHER THAN REPEATED**, because a plan that asserts its own convenience
is how a wrong premise survives. The model is `ProjectileFlight.step`'s own: position advances by
`velocity`, **then** gravity is added — so after `n` ticks the drop is `g·n(n-1)/2` and the horizontal
distance is `speed·n`. **There is no drag in that loop** (see the caveat below). Fired from a
player's eye at 1.62 blocks, speed 2.5:

| | `g = 0.05` (RULED) | `g = 0.03` |
|---|---|---|
| **flat fire** | lands tick **9**, **22.5 blocks** | lands tick 11, 27.5 blocks |
| **45°, the max-range launch** | lands tick **73**, **129.0 blocks** | **still airborne at tick 120** — 212.1 blocks, stopped by the leash rather than by the ground |
| drop over the whole 120-tick leash | **357 blocks** | 214 blocks |

**THE CLAIM HOLDS, AND IT HOLDS BETTER AT 0.05 — which is one of the reasons §7.4 chose it.** At
0.05 even the optimal 45° stray arrow is in the ground at **129 blocks**, well short of 300 and well
inside the leash. **At 0.03 the 45° shot is leash-limited rather than ground-limited**, so the ruling
would do *less* of the work claimed for it — the honest version of "if it is over, the plan says so
instead of repeating the claim."

**And the full 300 blocks is unreachable for a stray arrow in any ordinary geometry:** staying up for
120 ticks means falling **357 blocks**, which needs that much open air beneath the shooter. Firing
off build height over a void is the only shape of world that allows it.

> **ONE CAVEAT, BECAUSE R12's WORDS ARE "LIKE A NORMAL ARROW" AND THIS IS THE PLACE THAT IS NOT
> TRUE.** A vanilla arrow also has **drag** — it loses a fraction of its speed every tick —
> and `ProjectileFlight.step` has **none**: its horizontal speed is constant until it lands. So a
> Plume arrow **falls** like an arrow and **does not slow down** like one. Named rather than fixed:
> adding drag is a change to the shared flight loop that every other projectile in the project rides,
> which is a decision well outside this weapon.

**GATE ROW P8 (§8) is the visible half: fire at nothing and watch where the arrow goes.**

---

## 4. WHAT THE ENGINE ALREADY HAS, AND WHERE IT GROWS

**Measured against the tree, so the slice can be sized before it is written.**

| need | already there | what it costs |
|---|---|---|
| the on-hit payload | `CastExecutor:597-598`, shared by every cast shape | nothing |
| the flight loop | `ProjectileFlight.step`, pure `core`, re-schedules itself one tick at a time | **a homing branch inside `step`** |
| the target search | `CombatWorld.combatantsNear(centre, radius)` | nothing — **no new port method** |
| R2, mobs only | `CombatantSnapshot.player` is a **boolean already on the snapshot** | a filter, in `core`, unit-testable |
| R3, rounds remaining | `Quivers.stateOf(held, keys, weapon)` re-reads the item, and `QuiverState.capacity()` survives | **see the warning below** |
| the homing fields | `CastSpec.Projectile` is a record with a documented convenience-constructor ladder | a new component + one more rung |
| the tick sound | `Player#playSound(loc, key, volume, pitch)`, the private-to-holder form `QuiverNotice` and `BrokenNotice` both use; sounds are authored as **strings** (`"block.dispenser.fail"`) | a key and a pitch mapping (§7) |
| the core test | `FakeWorld` implements `combatantsNear` and a delay-honouring `schedule` | **write the `core` test before the `paper` wiring** |

> **R3 IS THE ONE THAT TOUCHES A DOOR SOMEBODY DELIBERATELY CLOSED.** `QuiverState` **removed
> `loaded()` on purpose** in the record-to-class conversion, and `roundsNeeded()`'s own javadoc warns
> against re-adding it: *"Re-adding the accessor to let `paper` do the subtraction would walk straight
> back through that door."*
>
> **So R3 must not be delivered as `capacity() − roundsNeeded()` computed in `paper`.** That is the
> subtraction the javadoc names, and it is **wrong on an unstamped item anyway**: `roundsNeeded()`
> returns **0** when unstamped, which would report a full magazine for an item that has no count at
> all.
>
> **The shape that fits: a `roundsRemaining()` on `QuiverState` itself**, computed inside the class
> from the private `loaded` field, honest about the unstamped case, with its own `core` test.
>
> **AND SAY THE OBJECTION OUT LOUD, BECAUSE IT IS THE FIRST THING A REVIEWER WILL SEE:
> `roundsRemaining()` IS `loaded()`.** Algebraically `capacity − (capacity − loaded) = loaded`, so it
> returns exactly the accessor the record conversion removed, under a name that describes the Plume's
> use of it. **Answering that is the work; adding the method is not.**
>
> **THE ANSWER, AND IT IS THE TEST'S OWN RULE RATHER THAN A NEW ARGUMENT.**
> `QuiversSignatureTest.theInstanceSurfaceOfQuiverStateIsNamedToo` states the rule beside its set:
> *no accessor may hand back the **RAW STAMP** and the **AUTHORED VALUE** separately, because a
> caller holding both can redo `capacityOf`'s resolution outside it.* **The guarded pair is
> (stamped, authored). `loaded` is NEITHER of them** — it is a third quantity, and `capacity()`
> already hands out the **RESOLVED** value, so `roundsNeeded() + roundsRemaining()` recovers only
> `capacity()`, which is public already. **Nothing new becomes derivable.**
>
> **AND `loaded()`'s REMOVAL WAS COLLATERAL, NOT A RULING AGAINST THE QUANTITY.** The class javadoc
> records it as one of three accessors lost to the record-to-class conversion with **zero callers
> anywhere in either module**, and calls the narrower surface an improvement. *"Nobody was using it"*
> and *"this must never come back"* are different sentences, and only the first one was written.
>
> **IT IS ALSO THE PRECEDENT ONE SLICE OLD.** `roundsNeeded()` joined that same set in Slice E and is
> allowed under the same rule, for the same reason — *a single computed int that exposes neither
> input*. `roundsRemaining()` is its mirror.
>
> **THE TEST WILL FIRE, BY DESIGN, AND THE ANSWER GOES BESIDE THE SET BEFORE THE ASSERTION ASKS.**
> The named set today is `capacity, fireVerdict, isReloading, reloadTicksRemaining, reloadVerdict,
> roundsNeeded`; adding a seventh member is a deliberate edit to that list. **An answer written under
> the pressure of a red test is worth less than the same answer written while deciding**, which is
> why it is drafted here, in the plan, rather than left for the moment the suite goes red.
>
> **AND THE UNSTAMPED CASE IS PART OF THE ANSWER, NOT AN EDGE.** `roundsNeeded()` returns 0 when
> unstamped *because* `reloadVerdict` refuses such an item on an earlier rung. `roundsRemaining()`
> must not copy that 0 blindly — **an item with no count has no rounds, and R3's cap on an unstamped
> Plume is a question the `core` test has to state an answer to** rather than inherit.

---

## 5. THE HOMING NUMBERS — INHERITED AND UNJUDGED, WITH A GATE ROW EACH

**These are a different game's numbers.** They were tuned against **instant fire and a fixed fan of
three**, and R1 makes this a **charged weapon firing up to five**. **NOTHING CARRIES OVER
AUTOMATICALLY.** They are marked exactly as `locust_beam.yml`'s `samples_per_block` and `size` are
marked, and for the same reason: *a number that has been copied once looks more settled than the
number it was copied from.*

| constant | inherited value | what it means here | gate row |
|---|---|---|---|
| lerp factor | **0.65**, renormalised to the original speed | how hard the arrow turns each tick | **P1** |
| activation | **ballistic until 15 blocks** from spawn | the arrow flies straight, then seeks | **P2** |
| search radius | `getNearbyEntities(10, 10, 10)` → **`combatantsNear(pos, 10)`** | the box it looks in, per tick | **P3** |
| re-target | **every tick**; a null target flies straight on | no target lock | **P3** |
| lifespan | **120 ticks** — **RULED (R11)**, no longer inherited | the leash; ≈300 blocks at speed 2.5 | **P4** |

**Three things about that table are already known to be wrong-shaped, and saying so is cheaper than
letting a gate discover it:**

- **A cube is not a sphere.** The old repo's `getNearbyEntities(10, 10, 10)` is a **20×20×20 box**;
  `combatantsNear(pos, 10)` is a **radius-10 sphere**. The corner of the box is at
  `sqrt(3) × 10 ≈ 17.32` blocks. **These are not the same search**, and the sphere is the smaller one
  everywhere except along the axes. Do not report P3 as "the old radius held".
- **120 ticks is longer than anything this project ships.** Authored lifetimes today are **40, 40, 60,
  80, 100**. And a lifetime on a **seeking** body is not the same quantity as on a ballistic one: it
  is the leash, not the drop-off point.
- **`range:` IS NOT A PROJECTILE KEY.** `CastSpec.Projectile` has **no `range`** — reach is
  `speed × max_lifetime_ticks`. The Locust's `96` is a **Ray** range and does not transfer. On
  `hunters_bow` the equivalent is `2.5 × 60 = 150` blocks.

**WHAT DISCHARGES THEM:** a booted reading, per row, in a `GATE-dragons-plume.md` written **before the
boot** and not after. **The full row set, P0-P9, is drafted in §8**; these five constants are
P1-P4, and **the lifespan is no longer one of them — R11 ruled it** (§7.4).

---

## 6. THE COST SENTENCE: FIVE ARROWS IS FIVE INDEPENDENT CHAINS

`ProjectileFlight.step` ends with `world.schedule(next, 1, () -> step(...))` — **a self-rescheduling
one-tick chain per projectile**, and `Scheduler` offers no repeating primitive (`onEntity`,
`onEntityLater`, `onRegion`, `onRegionLater`, `onGlobal`, `async`), so that is the shape.

**A five-arrow release is therefore five chains, each doing per tick:** one `castRay` over the
segment travelled, one `driveMarker` entity write, one trail `present`, **plus — new for this weapon —
one `combatantsNear` query and a filter pass over its result**.

**And each arrow carries a marker entity** (`spawnMarker`), so five arrows is **five entities** in
flight as well as five chains.

**Against the old repo that is a different load profile, and the difference is not 5/3.** Its fan was
three, fired **instantly and repeatedly**; ours is five, fired **once per charge** — so peak
concurrency rises and duty cycle falls. **Neither number is measured here.** What is measured is the
per-tick work list above; a figure for it belongs to the gate, not to this paragraph.

> **AND SLICE F2 ADDED A SECOND PER-TICK COST: A SIGHT TRACE.** Ben ruled that a bolt must be able
> to see what it chases, so each chain now also calls `lineOfSightClear` — **from the bolt, not from
> the caster** — before it will take a target.
>
> **It is bounded by candidates, not by candidates × ticks, and the bound is loose.** The trace runs
> only for a candidate that has already beaten the running nearest, which is `CastExecutor`'s own
> ordering: cheap distance bound first, trace second, assignment last. **Worst case is one trace per
> candidate per tick** — when candidates arrive in decreasing distance order — and the typical case
> is far fewer. Measured in the unit fixtures: **6 to 8 traces across a whole 9-to-13-tick flight**
> with two mobs in range.
>
> **Five bolts multiply it, like everything else in this section.** A number, not a worry.

> **THE ONE LOAD QUESTION WORTH ASKING BEFORE ANY CODE:** `combatantsNear` **per arrow per tick** is
> the only new cost in that list, and at five arrows × 120 ticks it is **600 queries per release** in
> the worst case. The cheap mitigations (share one query per tick across arrows in the same release;
> re-target every *n* ticks instead of every tick) both **change the algorithm**, so they are
> **not adopted here** — they are named so that a gate row measuring the cost has somewhere to land.

---

## 7. THE NUMBERS — WHAT EACH ONE DECIDES, THEN THE ARITHMETIC UNDERNEATH

> **HOW THESE SECTIONS ARE WRITTEN, AND IT IS A CORRECTION TO HOW THEY WERE WRITTEN BEFORE.** Each
> opens with **one plain sentence saying what is being decided and what it feels like**, and puts the
> arithmetic below it as **evidence**. The earlier version led with DPS tables, fenceposts and
> crossovers and buried the choice underneath — **it did not land, and the operator said so.** The
> version that landed opened with *"right now clicking fast beats charging, so nobody would use the
> charge"* and put the numbers second. **The numbers are not the question; they are the evidence for
> the question.**

> **AND ONE UNIT RULE, BECAUSE R4′ BROKE THE OLD ONE.** A charged arrow is **48 and homes**; a tap
> arrow is **12 and does not**. **Twenty-five arrows is no longer twenty-five arrows**, so
> **arrows/second is meaningless ACROSS the two paths** and every cross-path comparison below is in
> **damage per second**. Within a single path all the arrows are alike and arrows/s still means
> something; that is why both appear.

### 7.0 CLOSED — **R6**: FULL CHARGE IS ARROW 1

**What it decided:** whether holding to full charge and letting go immediately gives you one arrow or
none.

```
READING A   full charge = 1 arrow ready, then the increment per further second   <- RULED (R6)
            step k costs 20 + (k-1)c ticks;  step 3 = 60t = 3.00s at c = 20
READING B   full charge = 0, the first second adds the first arrow
            which makes a full-charge release with no wait fire nothing at all, contradicting R4'
```

**R6 ruled Reading A — the one every figure was already computed under, so nothing moved.** Recorded
as closed rather than deleted: **a plan that silently drops a question leaves the next reader unable
to tell a ruling from an oversight.**

> **THE TICK ARITHMETIC IN READING A WAS RESTATED FOR R1′, AND THE READING ITSELF WAS NOT TOUCHED.**
> It used to read *"N arrows costs 20 + (N-1)c ticks; 5 arrows = 100t"*, which is the same rule
> **indexed by arrows** — correct only while a step was one arrow. It is now indexed by **step**,
> and step 3 is `20 + 2c = 60t`. **The question R6 answered, and the answer, are unchanged**; what
> moved is the unit the formula counts in.

**`c` IS THE CHARGE SECOND, RULED AT 20 (§7.5). IT IS NEVER WRITTEN `T`** — `T` is §7.2's tap
interval, and the two collided once already, in review, by a careful reader.

### 7.1 ***RE-OPENED*** — **R8's 48 WAS RULED AGAINST A CYCLE THAT NO LONGER EXISTS**

**What it decided:** how hard a full release hits. **240 damage lands in a single frame** — and
**240 is UNCHANGED by R1′.** What changed is that it now arrives **every three seconds instead of
every five.**

**Ben took the instantaneous-parity anchor, with the spike paragraph below in front of him.** The
spike is **chosen, not overlooked.** But the anchor he took it against has moved underneath it.

```
one release   5 x 48                =   240   in one frame        UNCHANGED
magazine      25 x 48               =  1200   against the Locust's 312    UNCHANGED
cycle         5 x 60t + 90t reload  =   390t  = 19.50s            was 590t = 29.50s

   sustained, with reload   1200 / 19.50s  =  61.54 dmg/s     Locust 32.50   +89.3%
   within the magazine      1200 / 15.00s  =  80.00 dmg/s     Locust 47.27   +69.2%
```

> **THE COMPARISON THE TABLE CANNOT SHOW, AND THE ONE THE RULING WAS TAKEN AGAINST: the Plume lands
> its whole release in ONE INSTANT.** 240 in a single frame, against the **104** the Locust lands in
> its first two seconds (four shots at 12 ticks: t = 0, 12, 24, 36). **A number that reads modest as
> a rate reads extreme as a spike**, and this weapon is all spike. **That half is untouched** — the
> amendment did not move the spike, only how often it lands.

#### THE ANCHORS, RECOMPUTED — AND 48 HAS CROSSED FROM BETWEEN THEM TO ABOVE BOTH

**The anchor convention, stated because it is not obvious from the names** and both previous anchors
were computed under it: `D = LocustRate × PlumeCycleSeconds / 25`. So *"instantaneous parity"* does
**not** mean the two weapons' bursts match — it means **the Plume's SUSTAINED output equals the
Locust's BURST output**, which is a deliberately harder bar.

```
Locust, from its own file (26 dmg, 12 rounds, 60t reload, 12t cooldown):
   sustained      312 / 9.60s  = 32.500 dmg/s     (132t of shooting + 60t reload)
   burst          312 / 6.60s  = 47.273 dmg/s     fencepost A -- 12 shots span 11 intervals
                  312 / 7.20s  = 43.333 dmg/s     fencepost B -- 12 shots span 12 intervals

                        sustained parity     instantaneous parity (B)
   OLD cycle 29.50s          D = 38.35              D = 51.13      <- 48 sat BETWEEN these
   NEW cycle 19.50s          D = 25.35              D = 33.80      <- 48 sits ABOVE both
```

> **A FENCEPOST INCONSISTENCY THIS RECOMPUTATION EXPOSED, AND IT PREDATES THE AMENDMENT.** The table
> above quotes the Locust's burst as **47.27** (fencepost A) while **both old anchors were derived
> from 43.33** (fencepost B) — `43.333 × 29.50 / 25 = 51.13` exactly. **Two conventions for one
> quantity, in one section.** Neither figure is wrong; they answer *"12 shots in how long"*
> differently, and §7.2's own fencepost box already names this family. **Recorded rather than
> silently harmonised**, because harmonising it would move a number Ben's ruling was taken against.

> ### ⚠ OPEN — AND IT IS BEN'S. DO NOT QUIETLY KEEP 48.
>
> **48 was ruled against a five-second cycle. The cycle is now three seconds and the anchors have
> moved under it by a third.** At 48 the Plume sustains **61.54 dmg/s — about 1.9× the Locust's
> 32.50**, where it was 1.25× before.
>
> ```
> Locust sustained       32.50 dmg/s      parity now ~25 per arrow   (was ~38)
> Locust instantaneous   43.33 dmg/s      parity now ~34 per arrow   (was ~51)
> Plume at 48            61.54 dmg/s      ~1.9x the Locust sustained
> ```
>
> **A legendary outclassing a rare may be exactly right. That is not this document's call** — and it
> is not the operator's either; R8 is Ben's ruling and only Ben can restate it. **Put those three
> lines to him and leave 48 authored until he answers.**
>
> **AND THE SECOND QUESTION FOR THE SAME ANSWER: `reload_ticks: 90` CHANGED MEANING WITHOUT CHANGING
> VALUE.**
>
> ```
> ruled at   90 / 590 = 15.25% of the magazine cycle
> now        90 / 390 = 23.08%                        a factor of 1.51
> ```
>
> **Ben ruled 4.5 seconds as *"longer than anything shipped, but not punishing"* against a
> 29.5-second magazine.** R1′ cut the shooting half from 500 ticks to 300 and left the reload alone,
> so **the seconds did not move and the SHARE did, by half again.** Nobody edited the number.
>
> **This is NOT the dead coupling of §7.3 and the two are easy to conflate:** that one says a
> *reason* for 90 stopped existing; this one says the *cost* of 90 grew. A reader who sees the
> coupling struck out could reasonably conclude the number became less important. It became more
> expensive.
>
> **90 IS OVER-DETERMINED RATHER THAN UNSUPPORTED**, which is why this is doubt about the **context**
> and not about the value: its ticks-per-round reason — 3.60 against `quiver_stone`'s 3.78 — is a
> comparison between two weapons' magazines, and R1′ moved neither. **The flag asks whether the share
> is what Ben intended, not whether the number has any support left.**
>
> **BOTH QUESTIONS GO IN ONE DIALOG, DELIBERATELY.** They are two consequences of one amendment and
> they interact — lowering 48 and shortening 90 both pull the same rate down, so answering them a
> week apart risks paying for the amendment twice. **Asking the second one later is also how the
> first one's answer gets quietly generalised into a ruling nobody gave.**
>
> **BOTH STAY IN `dragons_plume.yml` AND ARE FLAGGED THERE**, not adjusted on anyone's initiative.
> **A number left in place because nobody re-asked is the defect this project keeps finding; a number
> left in place because the operator was asked and said keep it is a ruling** — and the two are
> indistinguishable in the file unless the file says which.

### 7.2 THE HEADLINE, RE-DONE IN **DAMAGE PER SECOND** — AND THE UNITS ARE THE POINT

**What it decided, in one sentence:** *clicking fast used to beat charging, so nobody would have used
the charge* — and it is now settled, **not by a timer but by making the tap a different, worse shot.**

> **THIS IS THE ACCOUNT. THE POINTER IS THE HEADLINE SECTION AT THE TOP OF THIS FILE.**

**THE OLD ANSWER IS SUPERSEDED AND IS RECORDED RATHER THAN DELETED.** This section used to end in
`cooldown_ticks >= 21`, derived over **arrows per second**. **R7 ruled `cooldown_ticks = 0`**, and
**R4′ made arrows incommensurable**, so that inequality is void — it priced a lever nobody pulled,
in a unit that no longer works. *A superseded derivation that vanishes leaves the next reader
re-deriving it.*

**SYMBOLS.** `T` = the **tap interval** in ticks, this section only, never a charged-path quantity.
`c` = the **charge second**, ruled 20, §7.0/§7.1/§7.5, never written `T`.

```
charged   5 x (20 + 2c) + 90  =  390t  =  19.50s  ->  1200 dmg  ->  61.538 dmg/s
tapping   25 taps + the reload                    ->   300 dmg  ->  6000 / (24T + 90)   conv. A
                                                                    6000 / (25T + 90)   conv. B
```

| `T` | tap dmg/s (A) | charged 61.538 is | tap dmg/s (B) | charged is |
|---|---|---|---|---|
| **3 — vanilla's floor** | **37.037** | **+66.2%** | **36.364** | **+69.2%** |
| 4 | 32.258 | +90.8% | 31.579 | +94.9% |
| 8 | 21.277 | +189.2% | 20.690 | +197.4% |
| 12 | 15.873 | +287.7% | 15.385 | +300.0% |
| 20 | 10.526 | +484.6% | 10.169 | +505.1% |

**THE CHARGE IS AHEAD AT EVERY `T`, INCLUDING VANILLA'S OWN FLOOR — AND NOW BY A MARGIN NOBODY HAS
TO SQUINT AT.**

> **THE TAP COLUMN IS UNCHANGED BY R1′ AND THE CHARGED COLUMN MOVED 51.3%.** R4′ is one arrow at 12
> for one round; the amendment touched neither. **Every percentage in this table moved because one
> of its two inputs did**, which is worth saying because a table where every number changed usually
> means both sides were re-derived.

> **THE HISTORY OF THIS TABLE, KEPT BECAUSE IT HAS TURNED OVER TWICE AND FOR DIFFERENT REASONS.**
>
> ```
> reload 60, 1/s ladder   charged 42.857   tap@3 45.455   TAP ahead by 6.1%     <- as first sent
> reload 90, 1/s ladder   charged 40.678   tap@3 37.037   CHARGE ahead by 9.8%  <- R9 applied
> reload 90, 1/3/5        charged 61.538   tap@3 37.037   CHARGE ahead by 66.2% <- R1' applied
> ```
>
> **R9 turned the verdict over; R1′ turned a nine-percent margin into a two-thirds one.** The first
> was a correction to arithmetic nobody had done; the second is a design ruling that happened to land
> on the same quantity.

> **THE HONEST RESULT IS NOW "SOLVED WITH ROOM", AND THE REASON HAS MOVED AGAIN.** At vanilla's floor
> the margin is **66.2%**, and `T = 3` still means a sustained **6.7 clicks per second** — a tool's
> rate, not a hand's. **At any rate a person can actually keep up (`T = 8`, 2.5 clicks/s) the charge
> is ahead by 189%.**
>
> **WHICH IS WHY THE RISK NOW POINTS THE OTHER WAY — see the headline section.** The question is no
> longer *would anyone charge*; it is *would anyone ever tap*. R4′'s answer is that the tap is not a
> rate choice at all: it is what a player does at three blocks with no time to hold.
>
> **AND THE REMOVAL OF HOMING SITS ON TOP OF ALL OF IT, UNQUANTIFIED AND UNQUANTIFIABLE HERE.** A
> homing arrow's hit rate against a moving target is a property of **the player and the fight**, not
> of the tree. **No hit-rate assumption is written into this plan to make a table come out** — an
> unmeasurable quantity named as unmeasurable is worth more than a number invented to close a gap,
> and **P7 (§8) is where a person finds out.**

#### THE FENCEPOST — NAME THE CONVENTION, BECAUSE "EXACT" WAS TOO STRONG

```
A  24 intervals   25 taps SPAN 24 gaps.  The locust convention -- shots at the END of intervals.
                  It omits one tap's own draw, while the charged side counts all five of its own.
B  25 intervals   every tap costs a whole T, symmetric with the charged cycle.
```

**The two disagree by about one percentage point and agree on every verdict in the table above**, so
nothing here waits on the choice. It is named because **the earlier draft called a crossover *exact*
when it moved by 0.83 ticks with the convention** — this is the same fencepost family that has
already cost this project twice on the ranged weapons.

#### WHAT THE `T = 3` FLOOR RESTS ON — ONE STATEMENT, CITED TWICE

**The earlier draft derived the tap interval as *"3 ticks plus the client's 4-tick re-use delay, so
roughly 7 or 8"* — and argued one paragraph later that the 4-tick grid's premise is absent for a
latching input. Both could not stand.** Resolved the way the draw gate was: by going and looking.

1. **THE 4 IS A CLIENT CONSTANT AND THE PINNED JAR CANNOT CONTAIN IT.**
   `unzip -l run/versions/26.1.2/paper-26.1.2.jar | grep -c "net/minecraft/client/"` returns **0**;
   no `MultiPlayerGameMode`, no `LocalPlayer`. **The instrument is absent, and no better search fixes
   an absent class.**
2. **THE SERVER IMPOSES NO RELEVANT FLOOR, AND THAT PART IS IN THE JAR.**
   `ServerGamePacketListenerImpl.handleUseItem`'s only rate check is `checkLimit` — Paper's spam
   limiter, at most **8** use-item packets per `incoming-packet-threshold` ms, the ninth dropped.
   This repo's `run/config/paper-global.yml` sets **300**, so the ceiling is **26.67/s, 1.33 per
   tick**: an order above any tap rate, **never binding.**
3. **THE REPO'S ONLY MEASUREMENT OF THE 4 WAS TAKEN ON A HELD REPEAT** — `GATE-q7.md`, two weapons,
   two materials, `min 4t`. Untouched, and exactly as strong as it was.

> **SO: `4` GOVERNS THE HELD-REPEAT STREAM, MEASURED. WHETHER IT GOVERNS DISCRETE CLICKS IS
> UNMEASURED HERE AND UNRESOLVABLE FROM THIS JAR.** Both paragraphs below cite this rather than using
> the number in opposite directions.

**FIRST CITATION — THE TAP FLOOR.** Applying the latch/repeat distinction to **this plan's own tap
interval**, which is where the earlier draft failed its own test: `T`'s measured lower bound is **3
ticks** (R10, and `getPowerForTime < 0.1` in §3.1 — server-side, and a property of the *release*, so
it holds for taps). **There is no measured upper bound**, which is why the table runs to `T = 20`.
*A control carried past its precondition stops being a control*, and the 4 was being carried.

**SECOND CITATION — THE MULTIPLES-OF-4 RULE IS NOT INHERITED HERE.** `CLAUDE.md`'s *author multiples
of 4* exists because **a held right-click delivers inputs on a 4-tick grid**. This weapon's input is
a **latch**, not a repeat (§1.4). **By the statement above, the grid's premise is measured only for
the repeat** — which is also why **R7's `cooldown_ticks: 0` needs no grid argument at all.**

**WHAT DISCHARGES IT: GATE ROW P7 (§8)**, with `/rpg firerate` — Q7's own instrument pointed at
discrete clicks instead of at a hold.

### 7.2a THE REAL INCENTIVE IS AMMUNITION, AND IT IS IN NO RATE TABLE

**What it decides, in one sentence:** *a tap-spammer runs dry four times as often for the same work* —
and that is what a player actually feels, not a nine-percent difference in a rate nobody can see.

```
25 rounds tapped     25 x 12              =   300 damage
25 rounds charged    5 releases x 5 x 48  =  1200 damage     FOUR TIMES, from the same magazine
```

**It compounds with every reload, which a rate comparison never shows.** The tapper at `T = 3` empties
the magazine in **72 ticks and then waits 90** — *more time reloading than shooting*:

```
duty cycle   tapping at T=3    72 / 162  =  44.4% shooting      UNCHANGED
             charging         300 / 390  =  76.9% shooting      was 500 / 590 = 84.7%
```

> **THE DUTY-CYCLE CONTRAST NARROWED, AND IT IS THE ONE FIGURE IN THIS SECTION R1′ MOVED THE WRONG
> WAY.** The charged side's shooting phase shrank from 500 ticks to 300 while the reload stayed at
> 90, so **a charging player now spends proportionally MORE of their time reloading than before** —
> 84.7% down to 76.9%. The gap over the tapper is 1.73× rather than 1.91×.
>
> **It does not touch the argument, which is about ROUNDS and not about time:** the 4× ammunition
> ratio is damage per round, and R1′ moved time, not rounds. **Recorded because the recomputation
> found it** and a figure that moved against the claim it supports is exactly the one that gets
> quietly left at its old value.

**The ammunition argument is the stronger one for charging and it needs no hit-rate assumption**,
which is exactly why it belongs beside the rate table rather than inside it.

### 7.3 CLOSED — **R9**: `reload_ticks` = 90, AND IT IS NOW THE TAP TAX

**What it decided, in one sentence:** *how long you stand there doing nothing* — 4.5 seconds — and,
because `cooldown_ticks` is 0, **it is the only thing in the weapon that prices spam.**

```
boltor         8 rounds / 60t = 7.50 ticks per round
locust        12 rounds / 60t = 5.00
quiver_stone   9 rounds / 34t = 3.78
PLUME         25 rounds / 90t = 3.60      <- no longer the cheapest ammunition in the project
```

At the old 60 it was **2.40**, the cheapest by 36%. **R9 gives that title back to `quiver_stone`** by
a margin of 0.18 ticks per round.

> **~~THIS NUMBER IS LOAD-BEARING: below `reload_ticks` ≈ 71 the tap retakes the lead.~~ THAT
> COUPLING DIED WITH R1′, AND THE STRIKETHROUGH IS DELIBERATE.** With `cooldown_ticks = 0` (R7) the
> reload is still the whole tap tax, and the tapper still meets it five times as often per unit of
> damage dealt. **What is no longer true is that lowering it hands the lead back.**
>
> **RECOMPUTED AT 1/3/5, and the crossover did not move a little — it fell off the bottom:**
>
> ```
> T = 3   fencepost A   tie at reload_ticks =    4.00      was ~70.67
> T = 3   fencepost B   tie at reload_ticks =    0.00      was  66.67
> T = 4   fencepost A   tie at reload_ticks =  -28.00      no tie exists
> T = 4   fencepost B   tie at reload_ticks =  -33.33      no tie exists
> ```
>
> **A NEGATIVE TIE MEANS THE TAP CANNOT RETAKE THE LEAD AT ANY RELOAD WHATSOEVER**, and at vanilla's
> own floor it would take a reload of four ticks. **The charge leads at every value anybody would
> author.**
>
> **SO R9 KEEPS ITS OTHER REASONS AND LOSES THIS ONE**, and they are separated rather than left to
> be untangled: 90 ticks is **3.60 ticks per round**, still just under `quiver_stone`'s 3.78, and
> still the only thing pricing a tap-spammer's ammunition (§7.2a). It is simply no longer *the number
> holding the headline finding up*.
>
> **A STALE LOAD-BEARING FLAG IS WORSE THAN NO FLAG.** It makes the next author afraid to touch a
> field for a reason that has stopped existing — and the fear reads exactly like the real coupling
> did, which is why this paragraph corrects rather than deletes.

### 7.4 CLOSED — **R11**: REACH ≈ 300 BLOCKS, AND **R12**: IT DROPS LIKE AN ARROW

**What was decided:** *how far a Plume arrow travels before it gives up* — **it does not, in any
practical sense** — and *whether it falls on the way* — **it does, like any arrow.**

```
speed 2.5   x   max_lifetime_ticks 120   =   ~300 blocks        R11
gravity 0.05                                                     R12  -- see the choice below
```

> **R11 WAS RULED AGAINST THIS SECTION'S OWN ARGUMENT, AND THAT IS WHAT MAKES IT A RULING.** §7.4
> argued the other way: 300 blocks is **1.9× the longest reach in the project** — 160
> (`ember_staff` 2.0 × 80, `emberblade` 1.6 × 100) — and **well past render distance**, so the
> weapon can hit a thing its wielder cannot see. **Ben read that and took *"never gives up"*
> anyway.** Recorded, because *inherited because nobody looked* and *chosen over an argument* age in
> opposite directions and look identical a month later.

**WHICH GRAVITY, AND WHY — 0.05.** R12 says *drops like a normal arrow*, and the shipped band is
`0.03`-`0.05`. **`hunters_bow` authors `0.05`, and it is the only BOW in the project**; the 0.03
figures belong to staves (`ember_staff`, `emberblade`). **The precedent that matches the ruling's own
words is the bow one**, so 0.05 — not as a tie-break, but because "like a normal arrow" names the
weapon class the value should come from.

**AND IT IS ALSO THE VALUE THAT MAKES R11 AND R12 AGREE RATHER THAN FIGHT** — see §3.3, where the
targetless case is ruled and the drop is computed at both candidates. **0.03 leaves a 45° stray
arrow airborne for the entire 120-tick leash; 0.05 puts it in the ground at tick 73.**

### 7.5 CLOSED — **R6/§7.0**: THE CHARGE SECOND `c` IS **RULED AT 20**

**What it decided:** *how long a second of charge takes* — literally one second, so the sound ticks
once per second and *"every second adds an arrow"* means what it says.

**What the ruling chose against, kept because a ruling with no alternatives beside it reads as a
default nobody considered.** ***RECOMPUTED AT 1/3/5*** — the table below was computed at one arrow
per second throughout, so every cell in it moved:

| `c` | five arrows | magazine cycle | charged dmg/s |
|---|---|---|---|
| 16 | 52t (2.60s) | `5 × 52 + 90 = 350t = 17.50s` | 68.57 |
| **20 — RULED** | **60t (3.00s)** | **390t = 19.50s** | **61.54** |
| 24 | 68t (3.40s) | `5 × 68 + 90 = 430t = 21.50s` | 55.81 |

> **`c` IS NOT RE-OPENED BY R1′, AND THIS TABLE IS NOT AN INVITATION TO RE-OPEN IT.** A step is still
> one second; the amendment moved the step's **yield** and the **number** of steps. The alternatives
> are recomputed so the ruling still has a live comparison beside it — **not because the ruling is in
> question.**
>
> **What the recomputation does show is that `c` matters LESS than it did.** The spread across the
> three candidates was `47.06 → 35.82`, a factor of **1.31**; it is now `68.57 → 55.81`, a factor of
> **1.23**. With only two increments left to charge, a longer second buys proportionally less delay.

### 7.6 CLOSED — **R13**: THE FIVE STEPS MUST BE TELLABLE APART BY EAR

**What was decided, in one plain sentence.** *A player holding the draw must be able to release on
the count they wanted without looking at anything* — so the mapping is authored for **SEPARATION,
not for subtlety**. That legibility is the whole mitigation for the one thing this weapon gave up
(§2.1: its charge is the first duration in the project no stat can move).

**The material — RULED, after a listen, which is the only instrument there is for it.** Sounds are
authored as **strings** and played **privately to the holder** — the form `QuiverNotice` uses
(`"block.dispenser.fail"`, `"item.crossbow.loading_start"`).

```
RULED        block.note_block.hat         2026-09-14, on feel -- "it suits the weapon"
SHIPPED AND SUPERSEDED
             block.note_block.basedrum    the kick; ruled, shipped, replaced by the hat
             block.note_block.pling       "the piano doesn't suit it, try snare or kick"
NEVER AUDITIONED
             block.note_block.snare       the kick ruling's recorded alternative
             + four click candidates      listed in GATE-plume-draw.md, all measured present
```

**THE REASON GIVEN WAS FEEL, AND THAT IS THE WHOLE REASON ON RECORD.** Ben heard it and it suits the
weapon. **The argument that had chosen the kick — *a snare is easier to COUNT, a kick tells you WHICH
STEP you are on* — selected a different key and is withdrawn rather than reworded onto this one.**

> **THE MATERIAL IS RULED; R13's PROPERTY IS NOT MEASURED, AND THE TWO ARE DIFFERENT QUESTIONS.**
> R13 asks whether a player can tell **how many arrows they hold** — knowing your **POSITION** on the
> ladder, not counting events, and nameable having started listening late. **A feel judgement does
> not establish that, and no position trial exists on any candidate.** `GATE-plume-draw.md`'s H-1b
> is the row, it is OPEN, and the ruling narrowed it rather than closing it: one key to test instead
> of eight.

**THE MAPPING R13 SELECTS, AND THE ARITHMETIC THAT SELECTS IT.** Pitch in Minecraft is a **playback
rate**, so what an ear hears as a *step* is the **ratio** between two pitches, not the difference.
A mapping with even *differences* therefore has **shrinking steps**, and it shrinks exactly where the
decision matters most — at the top, when the player is deciding whether to let go.

***THREE STEPS NOW, NOT FIVE — R1′.*** The rule is **re-evaluated at the new count**, not sampled
from the old five:

```
                 1        2        3      each step, as a RATIO
linear      1.00     1.50     2.00        1.500   1.333    <- the step shrinks
RULED       0.80   1.2649     2.00        1.5811  1.5811   <- even
```

**The ruled row is geometric — `pitch = 0.8 × r^(n-1)` with `r = (2.0/0.8)^(1/2) = 1.5811`** — which
is *"authored for separation"* stated as a number rather than as an intention: **every step is the
same size to an ear**, and it is the largest even step the assumed span allows.

> **THE STEP IS NOW ABOUT EIGHT SEMITONES INSTEAD OF FOUR** — `12·log₂(1.5811) = 7.93` against
> `12·log₂(1.2574) = 3.97`. **That figure is a familiar scale for how far apart the steps are and
> NOT an interval anyone hears**, for the reason in the note below: a note-block drum is noise.
>
> **RE-EVALUATE RATHER THAN SAMPLE — AND HERE THE TWO AGREE EXACTLY, WHICH IS WORTH KNOWING RATHER
> THAN ASSUMING.** Computed, not characterised:
>
> ```
> old five     0.80000  1.00595  1.26491  1.59054  2.00000     r = 1.257433
> new three    0.80000           1.26491           2.00000     r = 1.581139
> ```
>
> **The new ladder IS the old one's 1st, 3rd and 5th rungs, to the last decimal place** — because
> every other term of a geometric sequence is itself geometric with ratio `r²`, and
> `1.257433² = 1.581139` exactly. **The amendment kept the arrow counts 1, 3 and 5, which are the
> ODD POSITIONS on the old ladder**, so the two routes cannot disagree.
>
> > **THE FIRST DRAFT OF THIS NOTE CLAIMED THE OPPOSITE** — that no three of the old five were both
> > even and spanning, and that sampling would have been *"nearly right by luck"*. **It is a
> > two-line calculation and it says they are identical.** Characterising a set instead of computing
> > it is the failure `CLAUDE.md` records, and this is an instance of it caught by running the
> > numbers.
>
> **The METHOD is still re-evaluation, and the agreement here is a property of THIS amendment.**
> Halving the increment landed on the odd positions; a ruling of 1/2/3 arrows, or 1/4/5, would have
> put the steps somewhere the old ladder has no rung at all. **Sampling happens to work once; the
> rule works every time.**
>
> **AND THE AMENDMENT COST NO RENUMBERING**, because nothing was ever numbered: `DrawCharge` computes
> the ladder from `MAX_STEPS` and its test asserts the PROPERTY — equal ratios, both endpoints — so
> changing the count re-spaced it and the row followed. A test that had pinned the five values would
> have gone red for the right reason and been repaired with the wrong numbers.

> **THE LADDER SURVIVED THE INSTRUMENT CHANGE; THE ARGUMENT THAT CHOSE IT DID NOT, AND THAT IS WORTH
> ONE LINE SO NOBODY RE-DERIVES IT.** This table used to read in **semitones** and called each step a
> major third. **The RATIO half survives** — equal ratios are still probably equal perceptual steps,
> so the ladder stands unchanged. **The INTERVAL half does not: a note-block drum is NOISE, not a
> tone.** Shifting its playback rate makes it shorter and brighter; there is no interval to hear.
> A sentence about semitones is a sentence about an instrument this weapon no longer uses.

**THE 2.0 CEILING REMAINS OUTSIDE KNOWLEDGE THIS MACHINE CANNOT MEASURE — AND THE INSTRUMENT CHANGE
WIDENED WHAT IS UNKNOWN ABOUT IT.** Checked and not found: `World#playSound`'s javadoc in the pinned
API documents **no range** for `pitch`, and `ClientboundSoundPacket` carries a **raw float** — so any
cap is the client's. **Do not restate it as a fact of the platform.**

**That half is unchanged by either swap. What changed when the tick stopped being a tone is how the
top can FAIL.** Pitch is a **playback rate**, so any note-block percussion at 2.0 is **half as long
and brighter**, where a pling at 2.0 was simply a high note. So the fifth step raises two questions
rather than one — *is it audible* and *is it still the same sound* — and a person listening is the
only instrument for either.

> **AND WHAT THE MOVE FROM KICK TO HAT DID TO THAT RISK IS NOT ESTIMATED HERE.** A hat is the driest
> and shortest of the candidates, so there is less body to lose — **but no one has listened to one at
> 2.0**, and a plausible sentence about which way it went would sit among measured ones and be read
> as another. **The fifth-step question is unchanged and still owed.**

> **THE SOUND KEY IS RULED** — `block.note_block.hat`, above — **so what remains for a listener is
> the LADDER rather than the material.** R13's test is the half that a pleasant sound can still fail:
> not *is the top of the range nice*, but **can a person NAME THE COUNT WITHOUT LOOKING** — and, more
> sharply, name it **having started listening late**, since the ruling's subject is position rather
> than counting.
>
> **The one reading this mechanism has ever produced was taken on the PLING and carries across
> neither swap.** It is preserved in `GATE-plume-draw.md` under H-1, with H-1b as the row that owes a
> position reading on the hat. **No trial exists on the kick or on the hat.**

## 8. THE GATE ROWS, DRAFTED BEFORE THE BOOT

**They go in `GATE-dragons-plume.md` when the slice is written. Drafted here so the constants in §5
arrive with their discharge conditions attached rather than acquiring them afterwards.**

| row | staging | what to record |
|---|---|---|
| **P0** | **THE GATE QUESTION OF §1.** Hold a Plume with a loaded quiver and **no arrows anywhere in the inventory**. Hold right-click for five seconds. Then place **one arrow in the off-hand** and repeat. | Whether the bow **visibly draws**, in each case. Whether any charge tick sounds. **This is the row that proves or refutes route A end to end.** |
| **P1** | Fire one arrow at a mob **20 blocks away, moving laterally**. Repeat at lerp `0.65` and at one neighbouring value if it reads wrong. | Whether it connects; how wide the turn looks. **The value is INHERITED AND UNJUDGED until this row is read.** |
| **P2** | Fire at a mob **inside** 15 blocks, and at one **beyond** it. | Whether the near shot homes at all (it should not, by the inherited constant) and whether that reads as a bug to a player. |
| **P3** | Stand where **two** mobs are within 10 blocks and one is at **~15** (inside the old box's corner, outside our sphere). | Which one it takes, and whether the far one is ever acquired. **The cube-vs-sphere note in §5 is what this row settles.** |
| **P4** | Fire with **no target in range**, over open ground. | How far it travels before it dies, in blocks, and whether 120 ticks reads as "forever". |
| **P5** | **R3, the live magazine — LANDED AS `GATE-plume-release.md`'s R-1, AND RESTAGED THERE BY R3a.** With **two** rounds loaded, hold past full charge. | **RESTAGED: the tracker must stop at ONE tick / ONE arrow, stranding a round** — not "stop at two", which was correct only while a step was one arrow. **Run the row in the release gate, not this draft**; it carries the 2-and-4 staging and the reason those are the only counts where the ruling is observable. |
| **P6** | Hold to five arrows with the sound on — **then have someone hold it while a second person, NOT WATCHING THE SCREEN, calls the count out loud.** | **TWO ANSWERS, AND THE SECOND IS R13's ACTUAL TEST.** (i) whether the rise is legible and the top of the range is still pleasant — the mitigation R1's overturn rests on (§2.1), and where the 2.0 ceiling gets tested rather than repeated (§7.6); (ii) **whether a person can NAME THE COUNT WITHOUT LOOKING.** A sound can be pleasant and still fail (ii), which is why the row asks both. |
| **P7** | **THE TAP INTERVAL, AND THE ONLY PLACE THE HOMING QUESTION CAN BE ANSWERED.** `/rpg give` the weapon, then **tap right-click as fast as you can for ten seconds** — genuinely tapping, not holding, since the instrument cannot tell the difference and the whole row depends on it. Then `/rpg firerate`. **Then do it again at a moving mob**, tapping, and then charged. | **`INPUTS count, window, mean and min`, verbatim.** The `min` is `T`'s real floor. **Record it even if it is 4** — a measured 4 for discrete clicks is a different fact from Q7's measured 4 for a held repeat, and only this row can tell them apart (§7.2). **And say, in words, how many of the un-homing taps MISSED** — §7.2 leaves the homing advantage deliberately unquantified, and a person shooting at a moving target is the only instrument there is. |

| **P8** | **THE TARGETLESS ARROW — §3.3, and it is the common case.** Fire **at nothing**, flat, over open ground: charged, and then a single tap. Then fire at nothing **at roughly 45°**. | **Where the arrow lands, in blocks**, for each. §3.3 computes **22.5 flat** and **129 at 45°** at `gravity 0.05` — **a reading far from those means the flight model in this plan is wrong**, and every reach claim built on it with it. **Also: does it visibly ARC, or does it look flat?** R12's whole content is that it drops like an arrow. |

| **P9** | **THE SIGHT GATE — the visible half, which no unit test can show.** Stand a mob **behind a wall** and fire at it from beyond the 15-block activation distance. Then have it **step out** from behind the wall while a second bolt is in flight. | **Whether the bolt flies PAST** instead of burying itself in the masonry — and whether the second one **picks the mob up when it comes into view**, which is what "re-chosen every tick" buys. **Also: does flying past read as the weapon being broken?** A homing arrow that declines to home is the one case where correct behaviour and a bug look alike to a player. |

> **P5's second half is the one that earns the ruling.** *"Reads the magazine live"* and *"reads it
> once at full charge"* are **indistinguishable** on a quiver that does not change mid-hold — so a row
> that only holds a static magazine would pass under either implementation. **Staging a reload that
> completes during the hold is what makes the two disagree.**

---

## 9. WHAT THIS PLAN DOES NOT DO

- **It authors no content file and no Java.** No `dragons_plume.yml`, no schema change, no listener.
  The next slice starts from §4's growth points and writes the `core` test first.
- **It prices nothing itself — and §7 is CLOSED EXCEPT FOR ONE NUMBER, WHICH R1′ RE-OPENED.**

  ```
  arrow 1 at full charge   R6    charge second c = 20      R6     partial draw 12    R4'
  cooldown_ticks 0         R7    reload_ticks 90           R9     gravity 0.05       R12
  vanilla's 3-tick floor   R10   max_lifetime_ticks 120    R11    speed 2.5          R11
  1 / 3 / 5, three steps   R1'   the cap strands           R3a    the pitch mapping  R13

  attack_damage 48         R8    *** RE-OPENED -- ruled against the superseded 5-second
                                     cycle; §7.1 carries the dialog for Ben. Authored and
                                     FLAGGED in dragons_plume.yml until he answers. ***
  ```

  **The sound key is ruled** — `block.note_block.hat`, on feel 2026-09-14 (§7.6), the only instrument
  there was for it — **and it survived R1′ untouched**, because Ben ruled the timbre and the
  amendment moved the ladder. **The MATERIAL being ruled is not R13 being discharged:** H-1b still
  owes the position reading, on the ruled key, and the amendment made that row **easier** rather than
  closing it.

  **What is NOT ruled is not a number:** the **homing constants** of §5, which stay
  `INHERITED AND UNJUDGED` with a gate row each.
- **It does not rule the Endermen or the summons exclusions.** R2 carries them as candidates; they need
  a distinction `core` does not currently have — `CombatantSnapshot` knows `player` and nothing else —
  so ruling them in costs either a port extension or a `paper`-side predicate. **Named, not chosen.**
- **It does not touch `CLAUDE.md`.** Nothing here is settled enough to be a rule yet; when the draw
  route is ruled, the one-line operational form belongs there and this file stays the account.
