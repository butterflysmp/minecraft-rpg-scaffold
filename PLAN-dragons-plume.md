# PLAN — The Dragon's Plume

**Status: NOT IMPLEMENTED. No Java written, no content file authored, no test written, nothing
booted.** This document is the investigation, the rulings and the design. Everything below that is a
number a player experiences is either **RULED** (operator, and named as such) or **OPEN** (§7, put to
him with the arithmetic visible).

---

## THE HEADLINE FINDING — THE MECHANIC IS DOMINATED BY IGNORING IT

> **THIS IS THE POINTER. THE ACCOUNT IS §7.2** — the derivation, both fencepost conventions, the
> full rate tables and the symbol discipline live there, and nothing is restated here that is not
> needed to act.

**As the Plume is specified today there is no `cooldown_ticks` ruling, so nothing taxes a tap. R4
says a partial draw fires one arrow for one round. Therefore:**

> **UNGATED, TAPPING DELIVERS MORE THAN TWICE THE CHARGED RATE — and possibly more than four times.
> The mechanic this weapon exists for is the worse option at the values it is currently specified
> with.**

**The spread is honest and it is bounded below by a measurement**, so the finding does not wait on
anything (§7.2 resolves where the spread comes from):

```
charged, five arrows per release, c = 20 ruled     0.8929 arrows/s
tapping at T = 8 ticks                             1.9841 arrows/s    x2.22
tapping at T = 3 ticks, vanilla's own floor        3.7879 arrows/s    x4.24
```

**WHAT BEN RULES, IN THE UNITS HE AUTHORS — one field, one inequality:**

```
cooldown_ticks >= 21        the charge wins

  at 21   0.8865 arrows/s   charging ahead
  at 20   0.9259 arrows/s   tapping ahead by 3.7%
```

**21 is the answer under BOTH fencepost conventions** (§7.2 names them), so the number does not move
whichever is adopted — and **a cooldown anywhere up to 100 ticks cannot touch the five-arrow path at
all**, whose releases are already 100 ticks apart. **This field prices R4 and only R4.**

> **AND IT IS THE FOURTH OF A FAMILY, WHICH IS WHY IT IS AT THE TOP RATHER THAN IN §7.** Q7's
> quantised input; Rapid Fire as a percentage of a quantised cooldown; Expanded Quiver as a
> percentage of an integer magazine; and now **a mechanic whose own cheaper alternative is better
> than it.** Every one of the four was arithmetic nobody had done until somebody did.

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

## 2. THE SIX RULINGS, AND THE REASONING R1 OVERTURNED

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
- **R4 · A PARTIAL DRAW FIRES ONE ARROW, AND IT STILL HOMES.** Costs one round. Full charge is purely
  upside; the Plume is never dead at close range.

  > **AND R4 HAS A FLOOR VANILLA IMPOSES, WHICH IS THE SAME RULING SEEN FROM THE OTHER END.** A draw
  > shorter than **3 ticks** fires nothing and reports nothing — `getPowerForTime(t) < 0.1`, measured
  > in §3.1. *"A partial draw fires one arrow"* and *"the shortest taps fire nothing and say nothing"*
  > describe one behaviour, and **which release signal the shot hangs on decides whether R4 is honest
  > below 3 ticks** (§3.1). It is also the floor the headline's `T = 3` row stands on.

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
| `PlayerStopUsingItemEvent` | **every** release, **before** `ItemStack.releaseUsing` (`LivingEntity.releaseUsingItem`, event at offset 64, item at 84) | `getTicksHeldFor()`, which is `getTicksUsingItem()` = `useDuration − remaining`, counting **up** | **the release signal.** Not cancellable. |
| `EntityShootBowEvent` | only when vanilla decides to shoot | `getForce()`, `getProjectile()` | **the suppressor.** Cancelling calls `Entity.remove()` on the arrow before it is added to the world. |

> **HANG THE SHOT ON THE FIRST ONE, NOT THE SECOND, AND THE REASON IS A MEASURED DEAD ZONE.**
> `BowItem.releaseUsing` returns with no shot when `getPowerForTime(heldTicks) < 0.1` (offsets 54-65).
> The curve is `((t/20)² + 2(t/20)) / 3`, so:
>
> ```
> t = 2   power 0.0700   NO SHOT, and no EntityShootBowEvent
> t = 3   power 0.1075   shoots
> ```
>
> **A tap under three ticks fires nothing in vanilla and delivers no bow event at all.** R4 says a
> partial draw fires one arrow; hung on `EntityShootBowEvent`, the shortest taps would be **silently
> dead** and nothing would report it. `PlayerStopUsingItemEvent` fires either way and carries the
> tick count, so it can serve R4 down to `t = 1` if Ben wants it to. **Whether it should is an open
> number (§7).**

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

## 7. OPEN NUMBERS — A DIALOG. THE ARITHMETIC IS SHOWN SO EACH CAN BE RULED IN A SENTENCE

**Ben rules any number a player experiences. None of these is invented below.**

### 7.0 CLOSED — THE ONE THAT CHANGED EVERY OTHER NUMBER ON THIS PAGE IS NOW **R6**

**The question was: does full charge itself deliver the first arrow, or the zeroth?**

```
READING A   full charge = 1 arrow ready, +1 per further second      <- RULED (R6)
            N arrows costs 20 + (N-1)c ticks;  5 arrows = 100t = 5.00s at c = 20
READING B   full charge = 0, the first second adds the first arrow
            5 arrows = 20 + 5c = 120t = 6.00s, and a full-charge release with no wait would
            fire nothing at all, which contradicts R4
```

**R6 ruled Reading A, which is the one every figure below was already computed under — so nothing in
§7.1 moves.** It is recorded as closed rather than deleted: **a plan that silently drops the question
leaves the next reader unable to tell a ruling from an oversight**, and the two readings differ by a
full second of hold at five arrows.

**`c` IS THE CHARGE SECOND AND IT IS RULED AT 20** (§7.5). **It is never written `T`** — `T` is the
tap interval and belongs only to §7.2. The two are different quantities and the collision is easy to
make: it was made once, in review, by a careful reader.

### 7.1 `attack_damage` PER ARROW — AND THEREFORE ×5 AT FULL CHARGE

**The cycle, stated so the fencepost is visible.** Twenty-five rounds at one round per arrow is
**five releases of five**. One release costs `20 + 4c` ticks — **`c` is the charge second, ruled at
20 (R6, §7.5), and it is the only symbol used for it on this page** — so `20 + 80 = 100t`, and the
magazine empties in `5 × 100 = 500t`.

**THERE IS NO FENCEPOST SUBTRACTION ON THIS SIDE, AND THAT ASYMMETRY IS THE ONE TO NAME.** Unlike the
Locust's *twelve shots span eleven intervals*, each Plume release **consumes a whole window
including its own draw** rather than sitting at the end of a gap — so the charged cycle counts all
five draws. **§7.2's tap cycle does not count all twenty-five**, which is exactly why the crossover
has two conventions. Add the reload:

```
500t hold + 60t reload = 560t = 28.00s per magazine of 25 arrows
   with reload      25 / 28.00s = 0.8929 arrows/s
   within magazine  25 / 25.00s = 1.0000 arrows/s
   instantaneous    5 arrows in one frame
```

**The Locust, for comparison, from its own file — three rates, and the operator's "roughly 43" is a
fourth:**

```
sustained (with reload)   12 / 9.60s = 1.2500 sh/s x26 = 32.50 DPS
burst (within magazine)   12 x 26 / 6.60s            = 47.2727 DPS
instantaneous             20/12 x 26                 = 43.33 DPS   <- the "roughly 43"
```

**So, per-arrow damage D against both rates:**

| D | one release (5 arrows) | sustained, with reload | per magazine |
|---|---|---|---|
| 19 — the Boltor's | 95 | 16.96 | 475 |
| 26 — the Locust's | 130 | 23.21 | 650 |
| **36** | 180 | **32.14** | 900 |
| 40 | 200 | 35.71 | 1000 |
| 45 | 225 | 40.18 | 1125 |
| 52 | 260 | 46.43 | 1300 |

**D = 36.40 makes the Plume's sustained damage exactly the Locust's 32.50.** **D = 48.53 matches the
Locust's instantaneous 43.33.** Neither is proposed; the pair is shown because **the answer depends
entirely on which of the Locust's rates Ben is pricing against**, and the two differ by a third.

> **THE COMPARISON THE TABLE CANNOT SHOW: the Plume lands its whole release in ONE INSTANT and then
> costs a full draw plus four seconds.** At D = 36 that is **180 damage in a single frame**, against
> the **104** the Locust lands in its first two seconds (four shots at 12 ticks: t = 0, 12, 24, 36).
> **A number that reads modest as a rate can read extreme as a spike**, and this weapon is all spike.

### 7.2 `cooldown_ticks` — THE ACCOUNT BEHIND THE HEADLINE FINDING

> **THIS IS THE ACCOUNT. THE POINTER IS THE HEADLINE SECTION AT THE TOP OF THIS FILE**, and it
> carries the one thing Ben has to act on — `cooldown_ticks >= 21` — in a form that can be obeyed
> without reading a line of this section.

**SYMBOLS, STATED FIRST, BECAUSE THE COLLISION HAS ALREADY HAPPENED ONCE — IN REVIEW, BY A CAREFUL
READER, WHICH IS THE EVIDENCE THAT THE NOTATION INVITES IT AND NOT THAT SOMEONE WAS HASTY:**

```
T   the TAP INTERVAL, in ticks.  THIS SECTION ONLY.  Never appears in a charged-path expression.
c   the CHARGE SECOND, ruled 20 (R6, and §7.5).  Sections 7.0, 7.1, 7.5.  Never written T.
```

**The two cycles, side by side:**

```
charged   5 x (20 + 4c) + 60  =  560t = 28.00s  ->  0.892857 arrows/s
          every one of the five draws is counted -- see §7.1

tapping   25 taps, with the reload                  ->  500 / (24T + 60)   convention A
                                                        500 / (25T + 60)   convention B
```

#### THE FENCEPOST — NAME THE CONVENTION, BECAUSE "EXACT" WAS TOO STRONG

**The earlier draft called the crossover *exact*. A figure that moves by 0.83 when a convention
changes is not exact until the convention is named**, and this is the same fencepost family that has
already cost this project twice on the ranged weapons.

```
A  24 intervals   25 taps SPAN 24 gaps.  The locust convention -- shots at the END of intervals.
                  It omits one tap's own draw, while the charged side counts all five of its own.
B  25 intervals   every tap costs a whole T, symmetric with the charged cycle.
```

| `T` | A: `500/(24T+60)` | vs charging | B: `500/(25T+60)` | vs charging |
|---|---|---|---|---|
| 3 | 3.7879/s | **×4.24** | 3.7037/s | ×4.15 |
| 7 | 2.1930/s | ×2.46 | 2.1277/s | ×2.38 |
| 8 | 1.9841/s | **×2.22** | 1.9231/s | ×2.15 |
| 16 | 1.1261/s | +26.1% | 1.0870/s | +21.7% |
| 20 | 0.9259/s | +3.7% | 0.8929/s | **dead level** |
| 21 | 0.8865/s | **−0.7%** | 0.8547/s | **−4.3%** |

```
crossover     A: T = 20.8333        B: T = 20.0000 exactly
in integers   cooldown_ticks >= 21  under BOTH
```

> **THE ANSWER SURVIVES THE AMBIGUITY, AND THAT IS WORTH MORE THAN SETTLING IT.** The two conventions
> disagree about the crossover by 0.83 ticks and **agree about the field Ben authors**, because
> `cooldown_ticks` is an integer. **So the ruling does not wait on the convention** — it is named
> because the *word* "exact" was a claim about a number that has two values, not because the decision
> hangs on it.

#### WHAT THE 4 RESTS ON — ONE STATEMENT, CITED TWICE, BECAUSE THE EARLIER DRAFT USED IT AND DENIED IT

**The earlier draft derived the ungated tap interval as *"3 ticks plus the client's 4-tick re-use
delay, so roughly 7 or 8"* — and then argued, one paragraph later, that the 4-tick grid's premise is
absent for a latching input. Both cannot stand unexamined, and the contradiction set the headline
number.** It was resolved the way the draw gate was resolved: by going and looking.

**THE STATEMENT, and it is the only place this is asserted on this page:**

1. **THE 4 IS A CLIENT CONSTANT, AND THE PINNED JAR CANNOT CONTAIN IT.**
   `unzip -l run/versions/26.1.2/paper-26.1.2.jar | grep -c "net/minecraft/client/"` returns **0**;
   `MultiPlayerGameMode` and `LocalPlayer` are absent. **The instrument is missing, and no better
   search fixes that** — this is an absence of the *class*, not of a match.
2. **THE SERVER IMPOSES NO RELEVANT FLOOR OF ITS OWN, AND THAT PART IS IN THE JAR.**
   `ServerGamePacketListenerImpl.handleUseItem`'s only rate check is `checkLimit`, Paper's spam
   limiter: at most **8** use-item packets per `incoming-packet-threshold` milliseconds, the ninth
   dropped. This repo's own `run/config/paper-global.yml` sets it to **300**, so the server's ceiling
   is **26.67 packets/s — 1.33 per tick**. An order of magnitude above any tap rate, and therefore
   **never the binding constraint.**
3. **THE REPO'S ONLY MEASUREMENT OF THE 4 WAS TAKEN ON A HELD REPEAT.** `GATE-q7.md`, two weapons and
   two materials, `min 4t`. That reading is untouched and stays exactly as strong as it was.

> **SO: `4` GOVERNS THE HELD-REPEAT STREAM — MEASURED. WHETHER IT ALSO GOVERNS DISCRETE CLICKS IS
> UNMEASURED HERE AND UNRESOLVABLE FROM THIS JAR.** Both paragraphs below cite this statement instead
> of using the number in opposite directions.

**FIRST CITATION — THE TAP FLOOR.** Applying the latch/repeat distinction to **my own tap interval**,
which is where the earlier draft failed its own test: `T` has a measured lower bound of **3 ticks**
(vanilla's `getPowerForTime < 0.1` gate, §3.1 — server-side, in the jar, and it holds for taps
because it is a property of the *release*, not of the input stream). **It has no measured upper
bound.** *A control carried past its precondition stops being a control*, and the 4 was being carried.

**WHAT THAT DOES TO THE HEADLINE: IT CAN ONLY MAKE IT LARGER.**

```
T = 8   the 4 applied to taps as well      1.9841/s   x2.22    the CONSERVATIVE reading
T = 3   vanilla's own floor, alone         3.7879/s   x4.24    the tighter one
```

**Tapping dominates under every reading**, which is why the headline states the bound that survives
both — *more than twice* — and names the spread rather than picking the flattering end of it.

**SECOND CITATION — THE MULTIPLES-OF-4 RULE IS NOT INHERITED HERE.** `CLAUDE.md`'s *author multiples
of 4* exists because **a held right-click delivers inputs on a 4-tick grid**. This is the first weapon
in the project whose input is a **latch** rather than a repeat (§1.4): one use packet, one release.
**By the statement above, the grid's premise is measured only for the repeat** — so `12`, `20` or `21`
may be honest values here where they would be quantised elsewhere. **That is a re-derivation owed
before any cooldown is authored, not an exception granted in advance.**

**WHAT DISCHARGES ALL OF IT: GATE ROW P7 (§8), AND THE INSTRUMENT ALREADY EXISTS.** `/rpg firerate`
reads `FireCadence`, which counts **INPUTS before every gate** — Q7's own instrument, pointed at
discrete clicks instead of at a hold.

#### AND THIS FIELD PRICES R4 AND ONLY R4

> **A COOLDOWN ANYWHERE UP TO 100 TICKS DOES NOT TOUCH THE FIVE-ARROW PATH AT ALL**, because its
> releases are already `20 + 4c = 100` ticks apart. **So `cooldown_ticks` on this weapon means
> something different from what it means on every other weapon in the project**: it is not a fire
> rate, it is the tap tax — which is the reason the question is put rather than defaulted.
>
> **If the draw is meant to be the only gate, the answer is `0` and it should be authored as a
> RULING** — not left at 0 by omission, because the headline finding is exactly the consequence
> somebody would later "fix".

### 7.3 `reload_ticks` — 60 IS THE DEFAULT, AND 25 ROUNDS IS WHY THE QUESTION IS ASKED

**Shipped values and what they cost per round:**

```
boltor        8 rounds / 60t = 7.50 ticks per round
locust       12 rounds / 60t = 5.00
quiver_stone  9 rounds / 34t = 3.78
PLUME        25 rounds / 60t = 2.40    <- 36.5% cheaper per round than the cheapest shipped
```

**Matching the Locust's per-round rate would be 125 ticks (6.25s); matching the Boltor's, 187.5.**
**60 is proposed as the default**; the table is what argues against it.

### 7.4 `speed` AND `max_lifetime_ticks` — THERE IS NO `range` TO AUTHOR

**Shipped projectiles:** `speed` 1.4-2.5, `gravity` 0.03-0.05, `max_lifetime_ticks` 40-100.
`hunters_bow` is `2.5 / 0.05 / 60` → **150 blocks of reach**. The inherited lifespan is **120**.

**Two sub-questions that only exist because of homing:**

- **`gravity` during the ballistic phase.** The arrow is ballistic for the first 15 blocks (§5), so
  gravity is real there and then fights the lerp afterwards. **`0.0` is representable and may be the
  honest answer for a seeking arrow.** Unruled.
- **Lifetime is the leash.** At speed 2.5 a 120-tick arrow travels **300 blocks** if it never turns.
  The longest reach shipped today is **160** (`ember_staff` 2.0 × 80, `emberblade` 1.6 × 100), so the
  inherited lifespan is **1.9× the longest thing in the project** and well past render distance.

### 7.5 CLOSED — THE CHARGE SECOND `c` IS **RULED AT 20**

**Ben's ruling.** `c = 20` — the only figure that makes *"every second"* literal, and the value every
rate on this page was already computed under. **The relation is `N arrows = 20 + (N-1)c` ticks**, so
five arrows is `20 + 4c = 100t`.

**What the ruling chose against, kept because a ruling with no alternatives beside it reads as a
default nobody considered:**

| `c` | five arrows | magazine | sustained |
|---|---|---|---|
| 16 | 84t (4.20s) | `5 × 84 + 60 = 480t = 24.00s` | 1.0417/s |
| **20 — RULED** | **100t (5.00s)** | **560t = 28.00s** | **0.8929/s** |
| 24 | 116t (5.80s) | `640t = 32.00s` | 0.7813/s |

> **`c` IS NOT `T`.** `T` is §7.2's tap interval and the two have collided once already. The charge
> second never appears as `T` anywhere on this page.

### 7.6 THE SOUND, AND THE PITCH MAPPING ACROSS 1..5

**Which sound.** The precedent is that sounds are authored as **strings** and played
**privately to the holder** (`QuiverNotice`: `"block.dispenser.fail"`, `"item.crossbow.loading_start"`).
Candidates worth Ben's ear rather than mine: `block.note_block.pling`, `item.crossbow.loading_middle`,
`entity.experience_orb.pickup`.

**The mapping, proposed and not ruled:**

```
arrows ready   1      2      3      4      5
pitch        1.00   1.25   1.50   1.75   2.00        pitch = 1.0 + 0.25 x (n - 1)
```

**That mapping assumes 2.0 is the top of the useful range, and THAT IS OUTSIDE KNOWLEDGE, UNVERIFIED
HERE.** Checked and not found: `World#playSound`'s javadoc in the pinned API documents no range for
`pitch` at all, and `ClientboundSoundPacket` carries a **raw float** — so any cap is the client's, and
nothing on this machine can measure it. **Do not restate it as a fact of the platform.**

**It matters because the proposed mapping spends the whole assumed span on five arrows** and could not
be extended if the cap ever rose above five. **An alternative that leaves room:**
`pitch = 0.8 x 1.15^(n-1)` → 0.80, 0.92, 1.06, 1.22, 1.40. **Unruled; P6 is a person listening, and it
is also where the 2.0 assumption gets tested rather than repeated.**

---

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
| **P7** | **THE TAP INTERVAL — what the headline finding's spread rests on.** `/rpg give` the weapon, then **tap right-click as fast as you can for ten seconds** — genuinely tapping, not holding, since the instrument cannot tell the difference and the whole row depends on it. Then `/rpg firerate`. | **`INPUTS count, window, mean and min`, verbatim.** The `min` is `T`'s real floor. **Record it even if it is 4** — a measured 4 for discrete clicks is a different fact from Q7's measured 4 for a held repeat, and only this row can tell them apart (§7.2). |

> **P5's second half is the one that earns the ruling.** *"Reads the magazine live"* and *"reads it
> once at full charge"* are **indistinguishable** on a quiver that does not change mid-hold — so a row
> that only holds a static magazine would pass under either implementation. **Staging a reload that
> completes during the hold is what makes the two disagree.**

---

## 9. WHAT THIS PLAN DOES NOT DO

- **It authors no content file and no Java.** No `dragons_plume.yml`, no schema change, no listener.
  The next slice starts from §4's growth points and writes the `core` test first.
- **It prices nothing itself.** Two of §7's numbers are now **RULED** and marked CLOSED — the charge
  second `c = 20` (§7.5, R6) and the arrow-1 reading (§7.0, R6). **Every remaining number in §7 is a
  question with its arithmetic attached, not a proposal**, and the open set is `attack_damage`,
  `cooldown_ticks`, `reload_ticks`, `speed`/`max_lifetime_ticks`/`gravity`, and the sound with its
  pitch mapping.
- **It does not rule the Endermen or the summons exclusions.** R2 carries them as candidates; they need
  a distinction `core` does not currently have — `CombatantSnapshot` knows `player` and nothing else —
  so ruling them in costs either a port extension or a `paper`-side predicate. **Named, not chosen.**
- **It does not touch `CLAUDE.md`.** Nothing here is settled enough to be a rule yet; when the draw
  route is ruled, the one-line operational form belongs there and this file stays the account.
