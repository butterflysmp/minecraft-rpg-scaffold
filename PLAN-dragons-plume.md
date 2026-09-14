# PLAN — The Dragon's Plume

**Status: NOT IMPLEMENTED. No Java written, no content file authored, no test written, nothing
booted.** This document is the investigation, the rulings and the design. Everything below that is a
number a player experiences is either **RULED** (operator, and named as such) or **OPEN** (§7, put to
him with the arithmetic visible).

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
homes, a tap arrow is 12 and does not:**

```
charged, five per release, c = 20          40.678 dmg/s      1200 per magazine
tapping at T = 3, vanilla's own floor       37.037 dmg/s       300 per magazine   charge +9.8%
tapping at T = 8, a rate a hand can keep    21.277 dmg/s       300 per magazine   charge +91.2%
```

**AND THE ARGUMENT A PLAYER ACTUALLY FEELS IS NOT THE RATE — IT IS THE AMMUNITION.** The same
twenty-five rounds are worth **1200 damage charged and 300 tapped: four times**, so the tap-spammer
runs dry four times as often for the same work, and it compounds with every reload. **That needs no
hit-rate assumption**, which is why it carries more weight than the 9.8%.

> **NOT "SOLVED" — CLOSED WITH A MARGIN, AND THE REST IS UNQUANTIFIABLE HERE.** At vanilla's floor
> the gap is **9.8%**, and `T = 3` means a sustained **6.7 clicks per second** — a tool's rate, not a
> hand's. The removal of homing sits on top of that and **cannot be measured in this repository**: a
> hit rate against a moving target is a property of the player and the fight. **No hit-rate
> assumption is written into this plan to make a table come out.** P7 (§8) is where a person finds
> out.

> **AND R9 IS NOW LOAD-BEARING: below `reload_ticks` ≈ 71 the tap retakes the lead.** With
> `cooldown_ticks` at 0, the reload is the entire tap tax. **Lowering it re-opens §7.2**, which is
> exactly the coupling that gets broken by someone changing one field in isolation.

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

## 2. THE RULINGS — TEN LIVE, ONE OVERTURNED, AND THE REASONING R1 OVERTURNED

**Operator's, all final. Do not re-derive them from the code.**

- **R1 · THE VANILLA DRAW STANDS.** The bow draws normally. At **full** charge a tracker starts:
  every second adds one arrow, with a short sound on each, **rising in pitch** with the number ready.
  On release it fires the tracked amount.
- **R2 · MOBS ONLY.** The arrows do not seek players. The old repo's other exclusions — **Endermen**
  (neutral until provoked) and **summons owned by the shooter or their party** — carry forward as
  **candidates to be ruled later, not as settled**.
- **R3 · THE TRACKER IS CAPPED AT ROUNDS REMAINING.** It never climbs past what the quiver can pay
  for. Two rounds left means the ticks **stop at two**, and the sound never promises an arrow that is
  not coming. **The tracker reads the magazine LIVE** — ruled, and more work than the alternative
  deliberately.
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
  > `PlayerStopUsingItemEvent` is **not** pressed into serving `t < 3`. **The ruling simplifies the
  > design rather than constraining it** — see §3.1, where it reverses which event the shot hangs on:
  > the floor is now *wanted*, so it can come from the platform instead of from a second
  > implementation of the same rule.

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

- **R6 · FULL CHARGE IS ARROW 1.** Reaching full draw gives one arrow; each further second adds
  another, to five. **This replaces §7.0's flagged assumption with a ruling** — and it was *Reading
  A*, the one every figure was computed under, so **§7.1's rates stand unchanged** rather than
  needing recomputation.

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
> KEPT**, so it is now a thing to inherit: hanging the shot on `EntityShootBowEvent` gets it **from
> the platform, with no second implementation of the same rule** — and a re-implemented
> `ticksHeldFor >= 3` guard would be exactly the duplicated-rule shape this repo treats as a defect.
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
| lifespan | **120 ticks** | the leash | **P4** |

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
boot** and not after. **The full row set, P0-P7, is drafted in §8**; these five constants are
P1-P4.

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
READING A   full charge = 1 arrow ready, +1 per further second      <- RULED (R6)
            N arrows costs 20 + (N-1)c ticks;  5 arrows = 100t = 5.00s at c = 20
READING B   full charge = 0, the first second adds the first arrow
            5 arrows = 20 + 5c = 120t = 6.00s, and a full-charge release with no wait would
            fire nothing at all, which contradicts R4'
```

**R6 ruled Reading A — the one every figure was already computed under, so nothing moved.** Recorded
as closed rather than deleted: **a plan that silently drops a question leaves the next reader unable
to tell a ruling from an oversight.**

**`c` IS THE CHARGE SECOND, RULED AT 20 (§7.5). IT IS NEVER WRITTEN `T`** — `T` is §7.2's tap
interval, and the two collided once already, in review, by a careful reader.

### 7.1 CLOSED — **R8**: 48 PER CHARGED ARROW, **240 IN ONE INSTANT**

**What it decided:** how hard a full release hits. **240 damage lands in a single frame**, and then
the weapon costs a full draw plus four seconds before it can do it again.

**Ben took the instantaneous-parity anchor, with the spike paragraph below in front of him.** The
spike is **chosen, not overlooked.**

```
one release   5 x 48                 =   240   in one frame
magazine      25 x 48                =  1200   against the Locust's 312
cycle         5 x 100t + 90t reload  =   590t  = 29.50s

   sustained, with reload   1200 / 29.50s  =  40.68 dmg/s     Locust 32.50   +25.2%
   within the magazine      1200 / 25.00s  =  48.00 dmg/s     Locust 47.27    +1.5%
```

> **THE COMPARISON THE TABLE CANNOT SHOW, AND THE ONE THE RULING WAS TAKEN AGAINST: the Plume lands
> its whole release in ONE INSTANT and then costs a full draw plus four seconds.** 240 in a single
> frame, against the **104** the Locust lands in its first two seconds (four shots at 12 ticks:
> t = 0, 12, 24, 36). **A number that reads modest as a rate reads extreme as a spike**, and this
> weapon is all spike.

> **A NOTE THE RULING ITSELF CREATED, AND IT IS NOT A COMPLAINT ABOUT THE RULING.** The anchors Ben
> chose against were computed at the **old 60-tick reload**: sustained parity `D = 36.40`,
> instantaneous parity `D = 48.53`, and **48 sits just under the instantaneous one.** **R9 moved
> those anchors in the same message** — at a 90-tick reload they become **38.35** and **51.13**, so
> 48 now sits *between* them rather than at the top of the range.
>
> **Both rulings arrived together, so this is a fact to record rather than an error to fix** — and it
> is recorded because a reader who recomputes the anchors will otherwise think 48 was chosen against
> these ones. **It was chosen against the other two.**

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
charged   5 x (20 + 4c) + 90  =  590t  =  29.50s  ->  1200 dmg  ->  40.678 dmg/s
tapping   25 taps + the reload                    ->   300 dmg  ->  6000 / (24T + 90)   conv. A
                                                                    6000 / (25T + 90)   conv. B
```

| `T` | tap dmg/s (A) | charged 40.678 is | tap dmg/s (B) | charged is |
|---|---|---|---|---|
| **3 — vanilla's floor** | **37.037** | **+9.8%** | **36.364** | **+11.9%** |
| 4 | 32.258 | +26.1% | 31.579 | +28.8% |
| 8 | 21.277 | +91.2% | 20.690 | +96.6% |
| 12 | 15.873 | +156.3% | — | — |
| 20 | 10.526 | +286.4% | — | — |

**THE CHARGE IS AHEAD AT EVERY `T`, INCLUDING VANILLA'S OWN FLOOR.**

> **AND THAT IS A CORRECTION TO THE ARITHMETIC THIS REVISION WAS ASKED TO REDO, NOT A RESTATEMENT OF
> IT.** The rulings were sent with the figures *"charged 42.86, tap 45.45, the tap still ahead by
> about 6%"* — and **those are the 60-tick-reload figures.** **R9 raised the reload to 90 in the same
> message, and it lands on both paths unequally:**
>
> ```
> reload 60   charged 42.857   tap@3 45.455   TAP ahead by 6.1%     <- the figures as sent
> reload 90   charged 40.678   tap@3 37.037   CHARGE ahead by 9.8%  <- with R9 applied
> ```
>
> **The instinct behind R9 — *"the reload is the actual tap tax"* — is not merely right, it is
> load-bearing: it is what turns the answer over.** The tie is at **`reload_ticks` ≈ 70.67**
> (convention A) or **66.67** (convention B); R9's 90 clears both.

> **THE HONEST RESULT IS STILL NOT "SOLVED", AND THE REASON HAS MOVED.** At vanilla's floor the
> margin is **9.8%**, which is *close*, and `T = 3` means a sustained **6.7 clicks per second** —
> reachable by a tool, not by a hand. **At any rate a person can actually keep up (`T = 8`, 2.5
> clicks/s) the charge is ahead by 91%.**
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
duty cycle   tapping at T=3   72 / 162   =  44.4% shooting
             charging          500 / 590  =  84.7% shooting
```

**This is the stronger argument for charging and it needs no hit-rate assumption**, which is exactly
why it belongs beside the rate table rather than inside it.

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

> **THIS NUMBER IS LOAD-BEARING NOW, AND THAT IS A COUPLING SOMEBODY WILL BREAK BY CHANGING ONE
> FIELD IN ISOLATION.** With `cooldown_ticks = 0` (R7), **the reload is the whole tap tax**: the
> tapper meets it five times as often per unit of damage dealt.
>
> **MEASURED, SO THE COUPLING IS CHECKABLE RATHER THAN ASSERTED: below `reload_ticks` ≈ 71 the tap
> retakes the lead at `T = 3`** (66.67 under the symmetric convention). **Lowering this number
> re-opens §7.2.** It is not a comfort setting.

### 7.4 OPEN — HOW FAR THE ARROW REACHES, AND WHETHER IT FALLS ON THE WAY

**What is being decided, in two plain sentences.** *How far a Plume arrow travels before it gives up
and disappears* — and *whether a seeking arrow should drop toward the ground at all during the first
15 blocks, before it starts steering.* Everything below is the argument, not the question.

**The reach argument.** There is **no `range` key on a projectile** — reach is `speed × lifetime`.
The old repo's inherited 120-tick lifespan at a shipped-style speed of 2.5 is **300 blocks**: the
longest reach anything in this project has today is **160** (`ember_staff` 2.0 × 80, `emberblade`
1.6 × 100), so the inherited figure is **1.9× the longest**, and well past render distance. **Whether
that reads as "reaches anything you can see" or as "never gives up" is the decision.**

**The drop argument.** Shipped projectiles author `gravity` 0.03-0.05. The arrow is **ballistic for
the first 15 blocks** (§5) and steers afterwards, so gravity is real for that stretch and then fights
the lerp. **`0.0` is representable and may be the honest answer for a seeking arrow** — but a bow
that fires perfectly flat is a different-feeling weapon from one that arcs. **Unruled.**

### 7.5 CLOSED — **R6/§7.0**: THE CHARGE SECOND `c` IS **RULED AT 20**

**What it decided:** *how long a second of charge takes* — literally one second, so the sound ticks
once per second and *"every second adds an arrow"* means what it says.

**What the ruling chose against, kept because a ruling with no alternatives beside it reads as a
default nobody considered** (all three at R9's 90-tick reload):

| `c` | five arrows | magazine cycle | charged dmg/s |
|---|---|---|---|
| 16 | 84t (4.20s) | `5 × 84 + 90 = 510t = 25.50s` | 47.06 |
| **20 — RULED** | **100t (5.00s)** | **590t = 29.50s** | **40.68** |
| 24 | 116t (5.80s) | `670t = 33.50s` | 35.82 |

### 7.6 OPEN — WHAT THE CHARGE SOUNDS LIKE AS IT FILLS

**What is being decided, in one plain sentence.** *Whether a player can tell, with their ears and
without looking at anything, how many arrows they are holding* — because that legibility is the
whole mitigation for the one thing this weapon gave up (§2.1: its charge is the first duration in the
project no stat can move).

**The material.** Sounds are authored as **strings** and played **privately to the holder** — the
form `QuiverNotice` uses (`"block.dispenser.fail"`, `"item.crossbow.loading_start"`). Candidates for
Ben's ear rather than mine: `block.note_block.pling`, `item.crossbow.loading_middle`,
`entity.experience_orb.pickup`.

**The mapping, proposed and not ruled:**

```
arrows ready   1      2      3      4      5
pitch        1.00   1.25   1.50   1.75   2.00        pitch = 1.0 + 0.25 x (n - 1)
```

**That assumes 2.0 is the top of the useful range, and THAT IS OUTSIDE KNOWLEDGE, UNVERIFIED HERE.**
Checked and not found: `World#playSound`'s javadoc in the pinned API documents **no range** for
`pitch`, and `ClientboundSoundPacket` carries a **raw float** — so any cap is the client's, and
nothing on this machine can measure it. **Do not restate it as a fact of the platform.**

**It matters because the proposed mapping spends the whole assumed span on five arrows.** An
alternative that leaves room: `pitch = 0.8 x 1.15^(n-1)` → 0.80, 0.92, 1.06, 1.22, 1.40. **P6 is a
person listening, and it is also where the 2.0 assumption gets tested rather than repeated.**

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
| **P5** | **R3, the live magazine.** With **two** rounds loaded, hold past full charge for five seconds. | **How many ticks sound, and at what pitches.** The tracker must stop at two. **Then repeat with a reload completing mid-hold**, and record whether the cap moves. |
| **P6** | Hold to five arrows with the sound on. | **Whether the rise is legible** — the mitigation R1's overturn rests on (§2.1). A person listening is the only instrument for this. **Also the row where the 2.0 pitch ceiling gets tested rather than repeated** (§7.6). |
| **P7** | **THE TAP INTERVAL, AND THE ONLY PLACE THE HOMING QUESTION CAN BE ANSWERED.** `/rpg give` the weapon, then **tap right-click as fast as you can for ten seconds** — genuinely tapping, not holding, since the instrument cannot tell the difference and the whole row depends on it. Then `/rpg firerate`. **Then do it again at a moving mob**, tapping, and then charged. | **`INPUTS count, window, mean and min`, verbatim.** The `min` is `T`'s real floor. **Record it even if it is 4** — a measured 4 for discrete clicks is a different fact from Q7's measured 4 for a held repeat, and only this row can tell them apart (§7.2). **And say, in words, how many of the un-homing taps MISSED** — §7.2 leaves the homing advantage deliberately unquantified, and a person shooting at a moving target is the only instrument there is. |

> **P5's second half is the one that earns the ruling.** *"Reads the magazine live"* and *"reads it
> once at full charge"* are **indistinguishable** on a quiver that does not change mid-hold — so a row
> that only holds a static magazine would pass under either implementation. **Staging a reload that
> completes during the hold is what makes the two disagree.**

---

## 9. WHAT THIS PLAN DOES NOT DO

- **It authors no content file and no Java.** No `dragons_plume.yml`, no schema change, no listener.
  The next slice starts from §4's growth points and writes the `core` test first.
- **It prices nothing itself, and most of §7 is now CLOSED rather than open.** Ruled: the arrow-1
  reading and the charge second `c = 20` (R6, §7.0/§7.5), `attack_damage` 48 (R8, §7.1),
  `cooldown_ticks` 0 (R7, §7.2), `reload_ticks` 90 (R9, §7.3), the partial draw at 12 with no homing
  (R4′) and vanilla's 3-tick floor (R10). **Two remain open, and each is written as a question with
  its arithmetic underneath rather than as a blank:** `speed`/`gravity`/`max_lifetime_ticks` (§7.4)
  and the sound with its pitch mapping (§7.6).
- **It does not rule the Endermen or the summons exclusions.** R2 carries them as candidates; they need
  a distinction `core` does not currently have — `CombatantSnapshot` knows `player` and nothing else —
  so ruling them in costs either a port extension or a `paper`-side predicate. **Named, not chosen.**
- **It does not touch `CLAUDE.md`.** Nothing here is settled enough to be a rule yet; when the draw
  route is ruled, the one-line operational form belongs there and this file stays the account.
