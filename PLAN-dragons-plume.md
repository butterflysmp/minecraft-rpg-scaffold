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
R4''' a partial draw fires ONE arrow -- NO HOMING, 9 / 17 / 26 by band   (overturns R4'')
R7    cooldown_ticks = 0, ruled                                  "fast shooting arrows is fine"
R9'   reload_ticks   = 60                                        the tax that actually bites
```

**Redone in DAMAGE PER SECOND, because R4‴ made arrows incommensurable — a charged arrow is 34 and
homes, a tap arrow is 9, 17 or 26 and does not. RECOMPUTED under R14 (c = 16) and R4‴:**

```
charged, five per release, c = 16          53.125 dmg/s       850 per magazine
band 1 tap, held 3t, no gap at all         34.091 dmg/s       the FLOOR band, 9 damage
band 3 tap, held 15t, no gap at all        30.952 dmg/s       the TOP band, 26 damage
```

**THE TAP'S BEST CASE IS THE FLOOR BAND WITH NO GAP, AND THE CHARGE STILL LEADS IT BY 55.8%.**
Quoted at gap 0 deliberately: every other figure rests on an inter-click gap **never measured for
discrete clicks** (§7.2's table says so at each row), and gap 0 is the only assumption-free reading.

**THE MARGIN IS UNCHANGED AT 55.8%, AND THAT IS A COINCIDENCE RATHER THAN A CONSTANT.** R8′ left it
there against a flat-8 tap at `T = 3`; R14 and R4‴ moved the charged path up (47.22 → 53.13) and the
tap's best case up with it (30.30 → 34.09) by almost exactly the same factor. **Two ratios landing on
one number is the thing this project checks twice** — they are different quantities and nothing holds
them together.

**AND THE ARGUMENT A PLAYER ACTUALLY FEELS IS NOT THE RATE — IT IS THE AMMUNITION.** The same
twenty-five rounds are worth **850 damage charged against 225 / 425 / 650 tapped — 3.78× / 2.00× /
1.31× by band** — so a tap-spammer runs dry sooner for the same work, and it compounds with every
reload.

> ### ⚠ R4‴ WEAKENED THIS ARGUMENT, AND THE PREVIOUS DRAFT OF THIS PARAGRAPH SAID IT COULD NOT
>
> **It read: *"850 charged and 200 tapped: 4.25 times… That ratio is damage per ROUND, so every
> ruling here has moved TIME and left it alone… the one figure in this file that has only ever got
> stronger."*** **R4‴ moved damage per round**, which is the one thing that sentence assumed nothing
> would. At band 3 the multiple is **1.31×** and the argument barely argues.
>
> **THE OPERATOR'S OWN READING WENT THE OTHER WAY AND IS FALSIFIED HERE RATHER THAN QUIETLY
> DROPPED:** *"per ROUND, band 3 is 26 against the charged 34, so §7.2a's ammunition argument
> strengthens rather than weakens."* **26 against 34 is a RATIO OF 1.31, where the flat 8 gave 4.25**
> — the gap narrowed, so the brake weakened. **The comparison that matters is tap-to-charged, not
> tap-to-tap**, and reading a bigger tap number as a stronger brake inverts it.
>
> **What holds band 3 back instead is the RATE table (§7.2), and that one DOES rest on an unmeasured
> inter-click gap.** So the weapon's least assumption-dependent argument is also its weakest at the
> top band. **Flagged for P7, not repaired here.**

> **THE SHAPE OF THE RISK HAS INVERTED, AND IT IS RECORDED AS CHOSEN RATHER THAN DISCOVERED.** The
> original finding was *the tap beats the charge, so nobody would charge*. At **+55.8%** the danger is
> the opposite one: **a tap nobody would ever use is a mechanic costing a trigger binding, a content
> block and a gate row for nothing.**
>
> **That is NOT a defect today, and the reason is not about rate at all.** R4‴ exists so the weapon
> is **never dead at close range** — a player caught at three blocks needs something to do that is
> not a 2.6-second hold. A shot that is deliberately worse still beats no shot. **The tap is priced
> to be a last resort and it now unambiguously is one**, which is the design working rather than
> failing.
>
> **It is written down because the two failures look identical in a table and opposite in play**, and
> because the next person to read *"the tap is 56% behind"* will otherwise read it as a defect
> somebody missed.

> **~~R9 IS LOAD-BEARING: below `reload_ticks` ≈ 71 the tap retakes the lead.~~ THAT COUPLING IS
> DEAD, AND IT HAS NOW DIED THREE TIMES.** R1′ killed it; **R8′ was re-derived rather than assumed to
> inherit the verdict**, because a coupling dead under one parameter set is not dead under another;
> **R14 + R4‴ were re-derived again on the same principle.** At band 1's floor the tie is
> **`reload_ticks` = −4.32** (fencepost A) and **−8.40** (B), where `d = 8` gave −1.85 and −5.77.
> **The higher bands are further out still — −172 and −685** — because their hold floors cost more
> than their damage buys.
>
> **THE SIGN AT `R = 0` FLIPPED**, which is stronger than "still dead": under the old set a zero
> reload would have handed the lead back at `T = 3` (tie at +4). Now nothing does. **`reload_ticks`
> has stopped bearing on the rate comparison at all.**
>
> **AND `reload_ticks: 60` NOW RESTS ON A RULING RATHER THAN AN ARGUMENT.** Of 90's three supports,
> the coupling is dead and the ticks-per-round comparison was **overturned by the operator with the
> consequence in front of him** — the Plume is now the cheapest ammunition in the project by 36.5%,
> which is the outcome 90 was chosen to avoid. **What is left is pacing, which is a feel judgement.**
> That is legitimate and it is written down because **a number with no derivation is one somebody
> later "fixes".** Full working in §7.3.

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

## 2. THE RULINGS — FOURTEEN LIVE, AND THE REASONING EACH OVERTURN REPLACED

> **THE COUNT IS `grep -c "^- \*\*R" ` OVER THIS SECTION, NOT A NUMBER TYPED FROM MEMORY** — it read
> THIRTEEN until R14 and was correct; a count in a heading is a line that has to change with the set
> it counts, which is the failure `CLAUDE.md` records twice.
>
> **AND THE HEADING USED TO SAY *"ONE OVERTURNED"*, WHICH STOPPED BEING TRUE SEVERAL RULINGS AGO.**
> R1 → R1′, R4 → R4′ → R4″ → R4‴, R8 → R8′, R9 → R9′, and now R6's `c` → R14. **A tally of overturns
> is a figure nobody re-checks**, so the heading no longer carries one and each entry carries its own
> chain instead.

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
  > - **§7.5 · THE CHARGE SECOND `c` = 20 — UNCHANGED BY R1′.** A step was still one second, so every
  >   boundary sat at exactly the tick it always had. What R1′ changed is the step's **YIELD**
  >   (+1 → +2) and the **NUMBER** of steps (5 → 3). **`c` was never the quantity in question** in
  >   that amendment.
  >
  >   > ***AND R14 HAS SINCE MOVED IT ANYWAY: `c` = 16.*** **That is a SEPARATE ruling, not this one
  >   > leaking** — R1′ really did leave `c` alone, and this entry stays true as written about R1′.
  >   > **The sentence that had to go is *"do not read a shorter charge as a shorter second"***, which
  >   > described the state of the world for one amendment and is now precisely wrong: the charge IS
  >   > shorter because the second is. **A survival note is a claim about ONE ruling and ages the
  >   > moment another one lands on its subject.**
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
- **R4‴ · A PARTIAL DRAW FIRES ONE ARROW, WITH NO HOMING, AT 9 / 17 / 26 BY BAND.** One round each.

  > **THE SHAPE IS R4′'s AND IS UNTOUCHED THROUGH EVERY OVERTURN** — one arrow, no seeking, a
  > literal rather than `weapon_damage`. **Only the number has ever moved**, and R4‴ is the first
  > time it moved into a LADDER rather than to another single value.

  > **THIS IS AN OVERTURN OF R4″, NOT AN EXTENSION OF IT, AND THE WHOLE CHAIN IS WRITTEN DOWN.**
  >
  > ```
  > R4    a partial draw fires one arrow, AND IT STILL HOMES          -- superseded
  > R4'   a partial draw fires one arrow, NO HOMING, 12 damage        -- superseded by R8'
  > R4''  a partial draw fires one arrow, NO HOMING, 8 damage         -- superseded by R4'''
  > R4''' one arrow, NO HOMING, 9 / 17 / 26 BY BAND                   -- RULED
  > ```
  >
  > ### *** THE 8 IS GONE, NOT JOINED ***
  >
  > **9 does not extend the ladder downward from 8; it RE-RULES the floor.** Ben was shown that 8 was
  > the already-ruled value and chose 9 knowingly. **When a value changes the check is not "is the new
  > value right", it is "IS THE OLD VALUE GONE"** — which is why the sweep for this ruling greps `8`
  > in context rather than reading for `9`.
  >
  > **R4 was not a default. The homing-versus-no-homing question was put to Ben when R4 was made, and
  > he chose homing.** He has now chosen the option he previously declined, and added a damage cut.
  > **A silently rewritten ruling reads as one nobody ever questioned**, which would make the next
  > reader think the earlier choice was never considered. It was.

  > **THE BANDS ARE DERIVED, AND THE DERIVATION IS VANILLA'S OWN POWER CURVE CUT IN THIRDS.**
  > `DrawRelease` already transcribes `power(t) = (x² + 2x)/3` with `x = t/20`. Solving it at `1/3`
  > and `2/3`:
  >
  > ```
  > power = 1/3   ->  x^2 + 2x - 1 = 0   ->  x = sqrt(2) - 1 = 0.4142136   ->  t =  8.2842712
  > power = 2/3   ->  x^2 + 2x - 2 = 0   ->  x = sqrt(3) - 1 = 0.7320508   ->  t = 14.6410162
  > ```
  >
  > **RECOMPUTED, NOT ADOPTED — and the operator's 8.28 and 14.64 are both right.** The integer bands
  > are the floors of those roots, so band 1 runs to **8**, band 2 to **14**, and band 3 takes the
  > rest up to 19. `DrawReleaseTest` re-solves the first root rather than pinning `8`, so a band
  > boundary that drifts off the curve reddens.
  >
  > **THE REJECTED ALTERNATIVE, ON THE RECORD:** the bow's three VISIBLE pull textures, which change
  > at pull `0.0 / 0.65 / 0.9` — `t = 0 / 13 / 18`. **That would have made the bands match what the
  > PLAYER SEES**, which is a real argument. Rejected because its top band is **two ticks wide**
  > (18..19): the highest-damage tap would be unhittable on purpose and hit by accident.
  >
  > **This one was ruled by the operator rather than put to Ben**, and is marked as such so the
  > authority behind it is not later assumed to be Ben's.

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

- **R8′ · `attack_damage` = 34 PER CHARGED ARROW → 170 IN ONE INSTANT at full charge.**

  > **THIS IS AN AMENDMENT TO R8, AND IT CAME AS ONE ANSWER WITH R9′ AND R4″.** R8's 48 was taken on
  > the **instantaneous-parity** anchor against a five-second cycle, with §7.1's spike paragraph in
  > front of Ben — the spike was **chosen, not overlooked**. R1′ moved the cycle out from under it,
  > 48 was flagged in the content file as ruled-against-a-superseded-cycle, and this is the answer.
  >
  > ```
  > R8   48 per arrow, 240 in one instant, against a 29.5s magazine cycle   -- superseded
  > R8'  34 per arrow, 170 in one instant, against an 18.0s cycle           -- RULED
  >      34 per arrow, 170 in one instant, against a  16.0s cycle           -- R14 moved the CYCLE
  > ```
  >
  > **34 STILL SITS ABOVE BOTH LOCUST ANCHORS, EXACTLY AS 48 DID** (§7.1) — because the reload
  > shortened in the same ruling and pulled the anchors down with it. **The cut bought 1.89× → 1.45×
  > against the Locust, not a crossing**, and a reader expecting otherwise may take a second bite at
  > a number that has been answered.
  >
  > > **AND R14 HAS SINCE PUT IT BACK TO 1.63× WITHOUT TOUCHING `attack_damage`.** A shorter charge
  > > second shortened the cycle 18.0s → 16.0s, and **both anchors are proportional to the cycle**, so
  > > they fell 11.1% under a fixed 34. **The ruled value did not move and its relationship to every
  > > anchor did** — which is why §7.1's anchor table now carries a row per cycle rather than per
  > > damage ruling.

- **R9′ · `reload_ticks` = 60.** 3.0 seconds; **2.40 ticks per round** — **the cheapest ammunition in
  the project, by 36.5% over `quiver_stone`'s 3.78.**

  > **THAT IS THE OUTCOME 90 WAS CHOSEN TO AVOID, AND IT IS CHOSEN RATHER THAN OVERLOOKED.** R9's own
  > entry said 3.60 was *"just under `quiver_stone`'s 3.78"* and §7.3 said **"at the old 60 it was
  > 2.40, the cheapest by 36%"**. **60 is that value**, overturned by the operator with the
  > consequence in front of him.
  >
  > **SO 60 RESTS ON A RULING AND NOT ON AN ARGUMENT.** Of 90's three supports the rate coupling is
  > dead (three times, re-derived at `d = 8` and again at R14 + R4‴ rather than inherited — §7.3), the ticks-per-round
  > comparison is overturned, and **what remains is PACING, a feel judgement.** That is legitimate and
  > it is said out loud because **a number with no derivation is one somebody later "fixes"** —
  > anyone re-deriving it from the ammunition table will be reversing a ruling, not fixing an
  > oversight.

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

  > **AND R14 MAKES H-1b HARDER, WHICH IS THE OPPOSITE OF WHAT R1′ DID.** R1′ cut five rungs to
  > three; R14 puts those three **0.8 seconds apart instead of one second**, so there is less time to
  > place a tick by ear. **The row is noted rather than restaged** — the property R13 asks for has
  > not changed, only the difficulty of reading it.

- **R14 · THE CHARGE SECOND IS 16 TICKS, NOT 20.** Ruled on feel, 2026-09-15. A step costs **0.8
  seconds**; the three steps land at **20 / 36 / 52** ticks held.

  > **THIS OVERTURNS R6/§7.5's `c = 20`, AND THE OVERTURNED REASONING IS RECORDED RATHER THAN
  > SOFTENED.** §7.5 argued 20 because *a step is literally one second, so the sound ticks once per
  > second and "every second adds an arrow" means what it says.* **That is now false of the weapon**,
  > and the sentence is struck rather than reworded — it was a real argument, not a placeholder.
  >
  > ```
  > R6/§7.5   c = 20, a step is one second      -- superseded
  > R14       c = 16, a step is 0.8 seconds     -- RULED
  > ```
  >
  > **`FULL_DRAW_TICKS` STAYS 20 AND IS NOT PART OF THIS RULING.** It is **measured** —
  > `BowItem.MAX_DRAW_DURATION` — not chosen. **The two 20s were never the same quantity**, and §7.5
  > said so while they still shared a value.
  >
  > ### THE ARGUMENT THAT KEPT THEM APART IS NOW VINDICATED, WHICH IS WHY THIS IS A ONE-CONSTANT CHANGE
  >
  > `DrawCharge` held `FULL_DRAW_TICKS` and `CHARGE_SECOND_TICKS` as two constants on the explicit
  > grounds that they were **different quantities that merely happened to agree**. **Because nothing
  > had been conflated, R14 is one edit** — no hunt through everything that said 20, no disambiguation
  > pass. **A convention that costs something up front and pays out exactly once is worth recording
  > the day it pays**, because that is the only day the argument is checkable.
  >
  > **THE CONSTANT IS NOT RENAMED.** `CHARGE_SECOND_TICKS` holding 16 is a name that no longer
  > describes its value, and that is deliberate: the constant is *what a step costs*, which is what
  > every call site wants. **A later reader "correcting" it back to 20 is the risk**, and the javadoc
  > names it.

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

> **AND ONE UNIT RULE, BECAUSE R4′ BROKE THE OLD ONE.** A charged arrow is **34 and homes**; a tap
> arrow is **8 and does not**. **Twenty-five arrows is no longer twenty-five arrows**, so
> **arrows/second is meaningless ACROSS the two paths** and every cross-path comparison below is in
> **damage per second**. Within a single path all the arrows are alike and arrows/s still means
> something; that is why both appear.

### 7.0 CLOSED — **R6**: FULL CHARGE IS ARROW 1

**What it decided:** whether holding to full charge and letting go immediately gives you one arrow or
none.

```
READING A   full charge = 1 arrow ready, then the increment per further second   <- RULED (R6)
            step k costs 20 + (k-1)c ticks;  step 3 = 52t = 2.60s at c = 16
READING B   full charge = 0, the first second adds the first arrow
            which makes a full-charge release with no wait fire nothing at all, contradicting R4'
```

**R6 ruled Reading A — the one every figure was already computed under, so nothing moved.** Recorded
as closed rather than deleted: **a plan that silently drops a question leaves the next reader unable
to tell a ruling from an oversight.**

> **THE TICK ARITHMETIC IN READING A WAS RESTATED FOR R1′, AND THE READING ITSELF WAS NOT TOUCHED.**
> It used to read *"N arrows costs 20 + (N-1)c ticks; 5 arrows = 100t"*, which is the same rule
> **indexed by arrows** — correct only while a step was one arrow. It is now indexed by **step**,
> and step 3 is `20 + 2c = 52t` (R14). **The question R6 answered, and the answer, are unchanged**; what
> moved is the unit the formula counts in.

**`c` IS THE CHARGE SECOND, RULED AT 16 (R14, §7.5). IT IS NEVER WRITTEN `T`** — `T` is §7.2's tap
interval, and the two collided once already, in review, by a careful reader.

> **AND `c` IS NO LONGER 20, WHICH RETIRES A SECOND COLLISION THIS PLAN HAS BEEN LIVING WITH.**
> `FULL_DRAW_TICKS` is **20 and stays 20** — it is MEASURED, `BowItem.MAX_DRAW_DURATION`, not a
> choice. `CHARGE_SECOND_TICKS` was 20 by ruling and is now **16**. **The two quantities that
> §7.5 insisted were different while sharing a value no longer share it**, so the argument for
> keeping them apart is vindicated rather than merely asserted — and keeping them apart is exactly
> why R14 is a one-constant change instead of a hunt through everything that said 20.

### 7.1 ***CLOSED AGAIN*** — **R8′: 34 PER CHARGED ARROW, WITH `reload_ticks` 60; THE TAP HAS SINCE BEEN RE-RULED**

**What it decided:** how hard a full release hits, re-answered after R1′ moved the cycle out from
under R8's 48. **Three numbers moved in ONE ruling — 48 → 34, 90 → 60, 12 → 8 — and they must be
read as one answer**, because each was balanced against the other two.

> **AND THE THIRD OF THE THREE HAS SINCE BEEN OVERTURNED ON ITS OWN, WHICH THIS SECTION'S "ONE
> ANSWER" FRAMING DID NOT ANTICIPATE.** R4‴ replaced the flat 8 with **9 / 17 / 26 by band**. **34
> and 60 did NOT move with it**, so the balanced triple is now a balanced pair plus a ladder — and
> the re-derivations that mattered are in §7.2, §7.2a and §7.3, all of which were re-run rather
> than inherited. **The section heading used to end *"AND THE TAP AT 8"* and that is deleted, not
> footnoted**: the 8 is gone, not joined.

**Ben took the instantaneous-parity anchor originally, with the spike paragraph below in front of
him.** The spike is **chosen, not overlooked**, and it is now **170** rather than 240.

```
one release   5 x 34                =   170   in one frame        was 240
magazine      25 x 34               =   850   against the Locust's 312
cycle         5 x 52t + 60t reload  =   320t  = 16.00s            was 360t = 18.00s   (R14, c=16)

   sustained, with reload   850 / 16.00s  =  53.125 dmg/s    Locust 32.500  +63.5%
   within the magazine      850 / 13.00s  =  65.385 dmg/s    Locust 47.273  +38.3%
```

> **THE COMPARISON THE TABLE CANNOT SHOW, AND THE ONE THE RULING WAS TAKEN AGAINST: the Plume lands
> its whole release in ONE INSTANT.** **170** in a single frame, against the **104** the Locust lands
> in its first two seconds (four shots at 12 ticks: t = 0, 12, 24, 36). **A number that reads modest
> as a rate reads extreme as a spike**, and this weapon is all spike. **That half is untouched by
> R14** — a shorter charge second did not move the spike, only how often it lands.
>
> > **THIS PARAGRAPH SAID `240` UNTIL 2026-09-15, THREE LINES UNDER A BLOCK THAT SAID `was 240`.**
> > A stale figure from R8′, surviving in the prose of the very section that recorded its
> > replacement. **Found by grepping the DIGITS during R14's sweep, not by reading** — the same shape
> > as the five stale figures §3 of R14's brief found in `DrawRelease`, and the reason the sweep
> > greps `240` rather than "the spike".

#### THE ANCHORS, RECOMPUTED AGAIN — AND 34 IS STILL ABOVE BOTH, WHICH IS THE COUNTER-INTUITIVE PART

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
   cycle 29.50s (R8)         D = 38.35              D = 51.13      <- 48 sat BETWEEN these
   cycle 19.50s (R1')        D = 25.35              D = 33.80      <- 48 sat ABOVE both
   cycle 18.00s (R8')        D = 23.40              D = 31.20      <- 34 sat ABOVE both
   cycle 16.00s (R14)        D = 20.80              D = 27.73      <- 34 SITS ABOVE BOTH, FURTHER
```

> ### ⚠ R14 MOVED THE WEAPON AWAY FROM PARITY WITHOUT TOUCHING ITS DAMAGE, AND THAT IS THE WHOLE
> ### POINT OF KEEPING THE ANCHORS PROPORTIONAL TO THE CYCLE
>
> `attack_damage` did not move. **Both anchors fell 11.1%**, because a cycle 40 ticks shorter needs
> a smaller per-arrow number to match the Locust. **A ruling about how long a charge FEELS re-priced
> the weapon against every anchor in this section**, and nothing in the ruling said so.
>
> **This is the reason §9 says 34 / 60 / the partial draw were one balanced ruling.** `c` was not in
> that triple — and it should have been, because it multiplies the cycle that every anchor divides.
> **Recorded as a finding for Ben rather than as a reason to re-open 34**, which is his to re-open
> and not this file's to pre-empt.

> **CUTTING THE DAMAGE DID NOT MOVE THE WEAPON TOWARD PARITY, AND THAT IS THE OBVIOUS WRONG READING
> OF THIS RULING.** 48 → 34 is a 29% cut, and the anchors fell with it — because
> `reload_ticks` 90 → 60 shortened the cycle in the same breath, and the anchors are proportional to
> the cycle. **34 stands in the same relationship to both anchors that 48 did.**
>
> ```
> 48 against its anchors    1.89x the sustained-parity anchor     48 / 25.35   R1'
> 34 against its anchors    1.45x                                 34 / 23.40   R8'
> 34 against its anchors    1.63x                                 34 / 20.80   R14  <- BACK UP
> ```
>
> **What R8′ bought is 1.89× → 1.45×, not a crossing.** Anyone reading "the damage came down"
> and expecting the weapon to have landed between the anchors will find it has not, and may take a
> second bite at a number that has already been answered.
>
> > **AND R14 GAVE TWO FIFTHS OF IT BACK — `0.18` of R8′'s `0.44` — WITH NO RULING ABOUT DAMAGE.**
> > 1.45× → 1.63× is the anchor
> > moving under a fixed 34. **A reader who remembers "R8′ brought the Plume closer to parity" is
> > remembering a state that lasted one ruling.**

> **A FENCEPOST INCONSISTENCY THIS RECOMPUTATION EXPOSED, AND IT PREDATES THE AMENDMENT.** The table
> above quotes the Locust's burst as **47.27** (fencepost A) while **both old anchors were derived
> from 43.33** (fencepost B) — `43.333 × 29.50 / 25 = 51.13` exactly. **Two conventions for one
> quantity, in one section.** Neither figure is wrong; they answer *"12 shots in how long"*
> differently, and §7.2's own fencepost box already names this family. **Recorded rather than
> silently harmonised**, because harmonising it would move a number Ben's ruling was taken against.

> ### ✔ ANSWERED — THREE QUESTIONS, ONE SHOT, **ONE RULING**
>
> ```
> attack_damage: 48 -> 34                          STILL 34
> reload_ticks:  90 -> 60                          STILL 60
> the tap:       12 -> 8   -> 9 / 17 / 26 BY BAND  OVERTURNED AGAIN, R4''' -- the 8 is GONE
> the tap's NAME     RULED: the plain label, chosen -- now rendering THREE lines, not one
> ```
>
> **They were ONE answer and were balanced as one.** Each was set against the other two: 34 was
> chosen against a cycle that 60 shortens, and 8 against the 34 it is a fraction of. **Changing any
> one alone re-opens arithmetic the other two were settled against**, which is why
> `dragons_plume.yml`'s header says so at the top of the file rather than at each field.
>
> > **AND EXACTLY THAT HAS NOW HAPPENED, TWICE IN ONE MESSAGE — WHICH IS WHY R14 AND R4‴ SHIPPED IN
> > ONE PR.** R4‴ moved the third member alone; R14 moved `c`, which was never in the triple and
> > turns out to price it. **Landing either on `master` without the other would have left §7's
> > arithmetic wrong in between**, which is the whole argument for the single PR and is recorded
> > here because the triple's own note is what predicted it.
>
> ### ⚠ AND `reload_ticks: 60` NOW RESTS ON A RULING AND NOT ON AN ARGUMENT — SAY IT PLAINLY
>
> **90 had THREE supports. Two are gone, and the one that remains is a feel judgement.** That is
> legitimate and it must be VISIBLE, because **a number with no derivation is one somebody later
> "fixes".**
>
> | support for 90 | what happened to it |
> |---|---|
> | **the rate coupling** — *"below ≈ 71 ticks the tap retakes the lead"* | **DEAD, three times over.** R1′ killed it; the re-derivation at `d = 8` killed it harder; R14 + R4‴ took the tie to **−4.32** — see §7.3 |
> | **ticks per round** — 3.60, *"deliberately just UNDER `quiver_stone`'s 3.78"* | **OVERTURNED BY THE OPERATOR**, with the consequence in front of him |
> | **pacing** — *"longer than anything shipped, but not punishing"* | **THE ONLY THING LEFT, and it is a feel judgement** |
>
> **THE SECOND ROW IS THE ONE TO READ TWICE.** `dragons_plume.yml` argued, in its own words, that
> 3.60 kept the Plume off the cheapest-ammunition spot and that **"at the 60 first proposed it would
> have been 2.40, cheapest by 36%"**. **60 IS THAT VALUE.** Recomputed: `25 / 60 = 2.40` against
> `quiver_stone`'s `9 / 34 = 3.78` — **the Plume is now the cheapest ammunition in the project by
> 36.5%**, which is precisely the outcome 90 was chosen to avoid.
>
> ```
> boltor 7.50    locust 5.00    quiver_stone 3.78    PLUME 2.40
> ```
>
> > **SO DO NOT RE-DERIVE 60 FROM THE AMMUNITION COMPARISON.** That comparison was the argument FOR
> > 90; it was heard and overruled. **Anyone who recomputes it will find 2.40 well under 3.78, read
> > that as a defect, and be reversing a ruling rather than fixing an oversight.** The only thing
> > that can move this number is another feel judgement.
>
> **What the reload IS still for is an AMMUNITION argument, which is not a rate one.** With
> `cooldown_ticks` at 0 the reload remains the whole price of tapping: the same 25 rounds are worth
> **850 charged against 225 / 425 / 650 tapped — 3.78× / 2.00× / 1.31× by band** (§7.2a).
>
> > **THIS USED TO READ *"200 tapped — 4.25×"* AND CLAIMED THE RATIO WAS *"UNTOUCHED BY ANY OF
> > THIS"*.** R4‴ touched it, and in the direction that weakens the argument. **A sentence asserting
> > its own figure is immune to change is the one to re-check first**, because nobody re-checks it
> > afterwards.
>
> **AND A THIRD, WHICH IS NOT A NUMBER: WHAT THE TAP IS CALLED.**
>
> ```
> tooltip when the ruling was taken   "Dragon's Plume  Tap"
> tooltip today                       "Dragon's Plume  Tap1"
>                                     "Dragon's Plume  Tap2"
>                                     "Dragon's Plume  Tap3"
> ```
>
> **No tap binding authors a `name:`, so `WeaponLoader` falls back to the weapon's own display name**
> — which is why the lines read as they do. `quiver_stone` names its shot **"Loose"**, `emberblade`
> names one "Fireball"; every other ability block in the project carries a name of its own.
>
> **It went in that dialog because it is the same shot the other questions are about** — the tap is
> what `cooldown_ticks` prices and what the damage is set against.
>
> > **THE RULING WAS TAKEN ON ONE BINDING AND NOW GOVERNS THREE — EXTENDED, NOT RE-TAKEN.** Ben ruled
> > the plain label against `quiver_stone`'s "Loose" when there was a single `tap:`. **R4‴ renders it
> > three times**, and *"Tap1 / Tap2 / Tap3"* is a different reading experience from one line.
> > **Flagged rather than assumed**: if three plain labels read worse than one did, that is a finding
> > for Ben and three `name:` keys are the whole fix. The same note is in `dragons_plume.yml`, where
> > the absence lives.
>
> ### ✔ RULED: LEAVE IT READING "Tap"
>
> **Ben chose the plain label**, with `quiver_stone`'s **"Loose"** in front of him as the worked
> alternative. **The absent `name:` key is the decision, not the leftover.**
>
> > **SO AN ABSENT KEY HERE MEANS WHAT AN AUTHORED `0` MEANS AT `cooldown_ticks`: A RULING THAT
> > LOOKS LIKE AN OMISSION.** R7's own entry insists the zero be *"authored as a RULING — not left at
> > 0 by omission, because the headline finding is exactly the consequence somebody would later
> > fix"*. **A name cannot be authored as an absence**, so the comment in `dragons_plume.yml` is the
> > only thing separating the two readings, and it is written there rather than here alone.
>
> **A `name:` key is still the whole change if the ruling is ever revisited.**

> **THE OTHER TWO WERE ONE ANSWER, WHICH IS WHY THEY WERE ASKED TOGETHER.** Lowering 48 and
> shortening 90 both pull the same rate down, so answering them a week apart would have risked paying
> for the amendment twice — and the recompute confirms they were: **34 alone would have overshot,
> because 60 raised the rate back up.** See the anchors above, where the two moves cancel almost
> exactly in the ratio to the anchors and not at all in the ratio to the Locust.
>
> **BOTH ARE NOW AUTHORED IN `dragons_plume.yml` AND THEIR FLAGS ARE GONE**, because the question was
> asked and answered. **A number left in place because nobody re-asked is the defect this project
> keeps finding; a number in place because the operator was asked and answered is a ruling** — and the
> two are indistinguishable in the file unless the file says which, which is what the flags were for
> and why removing them is part of the answer rather than tidying.

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
`c` = the **charge second**, ruled 16 (R14), §7.0/§7.5, never written `T`.

```
charged   5 x (20 + 2c) + 60  =  320t  =  16.00s  ->   850 dmg  ->  53.125 dmg/s
tapping   25 taps + the reload   band 1  ->  225 dmg  ->   4500 / (24T + 60)   conv. A
                                 band 2  ->  425 dmg  ->   8500 / (24T + 60)
                                 band 3  ->  650 dmg  ->  13000 / (24T + 60)
                                 conv. B replaces 24T with 25T throughout
```

> **THE TAPPING LINE USED TO BE ONE ROW, `4000 / (24T + 60)`, AND THAT NUMERATOR IS GONE.** It was
> `25 x 8 x 20`, R4″'s flat tap. **R4‴ gives three numerators and three FLOORS on `T`**, since a band
> cannot be fired faster than it can be drawn — which is the whole reason the table below is indexed
> by gap rather than by `T`.

> ### ⚠ THE TABLE'S OLD SHAPE DIED WITH R4‴, AND SAYING SO IS PART OF THE RECOMPUTE
>
> It was parameterised by a single `T`, the tap interval, because the tap was **one flat shot** and
> `T` was the only thing a tapper chose. **R4‴ makes the tap three shots with three different HOLD
> COSTS baked in** — 3, 9 and 15 ticks — so `T` is no longer free: it is `hold + gap`, and the hold
> is set by which band you want.
>
> **A one-`T` table would have to pretend the three bands cost the same to produce, which is the one
> thing the bands are about.** So the parameter becomes the INTER-CLICK GAP, and each band gets a
> column.

`T = hold + gap`, so each band's floor is its own hold. Damage per second **including the reload**,
fencepost A:

| gap | band 1 (9, held 3t) | band 2 (17, held 9t) | band 3 (26, held 15t) | charged 53.125 leads by |
|---|---|---|---|---|
| **0 — a tapper with no gap at all** | **34.091** | 30.797 | 30.952 | **+55.8%** at worst |
| 2 | 25.000 | 26.235 | 27.778 | +91.2% |
| **4 — Q7's measured HELD-REPEAT gap** | 19.737 | 22.849 | 25.194 | +110.9% |
| 8 | 13.889 | 18.162 | 21.242 | +150.1% |

**THE CHARGE IS AHEAD OF EVERY BAND AT EVERY GAP, INCLUDING A GAP OF ZERO.**

> **EVERY FIGURE BELOW THE FIRST ROW RESTS ON A NUMBER NEVER MEASURED FOR DISCRETE CLICKS.**
> `GATE-q7.md` measured a 4-tick floor on a HELD REPEAT, on two weapons; **nothing in this repository
> has ever measured the gap between two deliberate clicks.** The **gap-0 row assumes nothing**, which
> is why the verdict is quoted from it — it is the tapper's best possible case and the charge still
> leads by 55.8%. **P7 (§8) still owes that measurement**, and has since Q7.
>
> **AND THE BANDS ARE NOT MONOTONE AT GAP 0, WHICH IS WORTH READING TWICE.** Band 1 (34.091) beats
> band 3 (30.952), and band 2 sits lowest of the three. **A cheap shot fired often beats a dear one
> at zero gap**, and the ordering inverts once the gap is 2. That is the bands working as designed:
> the higher bands buy damage with DRAW TIME, so they only pay off once a real hand's gap is in the
> denominator. **A reader who expects "higher band = better" will find it false exactly where the
> assumption is most heroic.**

> **THE HISTORY OF THIS TABLE, KEPT BECAUSE IT HAS TURNED OVER TWICE AND FOR DIFFERENT REASONS.**
>
> ```
> reload 60, 1/s ladder            charged 42.857  tap@3 45.455  TAP ahead 6.1%     <- as first sent
> reload 90, 1/s ladder            charged 40.678  tap@3 37.037  CHARGE ahead 9.8%  <- R9
> reload 90, 1/3/5                 charged 61.538  tap@3 37.037  CHARGE ahead 66.2% <- R1'
> reload 60, 1/3/5, D=34, d=8      charged 47.222  tap@3 30.303  CHARGE ahead 55.8% <- R8'
> reload 60, c=16, bands 9/17/26   charged 53.125  band1 34.091  CHARGE ahead 55.8% <- R14 + R4'''
> ```
>
> **R9 turned the verdict over; R1′ turned a nine-percent margin into a two-thirds one; R8′ brought
> it back to just over half.** The first was a correction to arithmetic nobody had done; the second
> and third are design rulings that happened to land on the same quantity.
>
> > ### ⚠ ROWS FOUR AND FIVE AGREE TO THREE FIGURES AND IT IS AN ACCIDENT
> >
> > **Both sides moved and they moved by the SAME FACTOR, which is the only reason 55.8% survived
> > R14 and R4‴ intact.** Charged: `360 -> 320` ticks per magazine, **×1.125**. Tapping: `8 -> 9`
> > damage, **×1.125**. `9/8` and `360/320` are the same ratio, and **nothing chose that** — `c` moved
> > on feel and the tap floor moved on a power curve cut in thirds, in the same message, for unrelated
> > reasons.
> >
> > **SO DO NOT READ ROW FIVE AS EVIDENCE THAT THE MARGIN IS STABLE UNDER RULINGS.** It is evidence
> > that two independent rulings happened to cancel. **The next ruling to move either side alone will
> > move it**, and the figure downstream that did NOT survive is the zero-reload tie below: `2.9412 ->
> > 2.8676`, because the reload is not in that one and so nothing cancels.
>
> **NOTE ROWS ONE, FOUR AND FIVE SHARE A RELOAD AND LITTLE ELSE.** All three are at 60, and row one
> sits on the opposite side of the verdict from the other two — 6.1% for the tap against 55.8% for
> the charge, twice. **A reader who remembers "60 was the value where the tap won" is remembering row
> one**, which was computed at one arrow per second and a 12-damage tap. The reload was never what
> decided it.

> **THE HONEST RESULT IS "SOLVED WITH ROOM", AND THE ROOM IS SMALLER THAN IT WAS.** At vanilla's
> floor the margin is **55.8%**, and band 1 at `T = 3` still means a sustained **6.7 clicks per
> second** — a tool's rate, not a hand's. **At any rate a person can actually keep up — a gap of 8,
> which is band 1 at `T = 11` (1.8 clicks/s) or band 3 at `T = 23` — the charge is ahead by 150.1%,
> and band 3 is the tapper's best answer there.**
>
> > **AND A MARGIN NOBODY CHOSE, WHICH WAS THIN ENOUGH TO NAME AND HAS SINCE MORE THAN DOUBLED.**
> > The tap interval at which tapping would draw level **on a zero reload** is now **`T = 2.8676`**
> > for band 1, against R10's floor of **3** — a margin of **0.1324 ticks**, where R4″'s flat 8 gave
> > **2.9412** and **0.0588**. Nothing picked either number: the 3 is read off vanilla's power curve
> > and the tie falls out of ruled values chosen for other reasons.
> >
> > ```
> > T = d x releases x rel / (D x (mag - 1))   conv A, 25 taps span 24 intervals
> >
> > band 1   9 x 5 x 52 / (34 x 24)  =  2340 / 816  =  2.8676     hold floor  3   margin 0.1324
> > band 2  17 x 5 x 52 / (34 x 24)  =  4420 / 816  =  5.4167     hold floor  9   margin 3.5833
> > band 3  26 x 5 x 52 / (34 x 24)  =  6760 / 816  =  8.2843     hold floor 15   margin 6.7157
> > ```
> >
> > **BAND 1 IS STILL THE BINDING CASE AND THE OTHER TWO ARE NOT CLOSE**, because a band's own hold
> > is a floor on its `T` and the higher bands' floors outrun their ties by ticks rather than by
> > hundredths. **R4‴ did not add two new ways to lose; it widened the one that existed.**
> >
> > **A NEAR-COLLISION WORTH NOT MISTAKING FOR AN IDENTITY:** band 3's tie is `8.2843137` and the
> > power-curve root that sets band 1's ceiling is `20(√2 − 1) = 8.2842712`. **They agree to four
> > significant figures and are different quantities** — one is a DPS crossover in ruled damage, the
> > other is where vanilla's charge reaches a third. Nothing connects them.
> >
> > **It is a COINCIDENCE, not a design margin, and it has a condition**: if a future Paper moves
> > the power curve so the floor drops below 2.8676, the tap retakes the lead at a zero reload and
> > §7.3's dead coupling is alive again. **Recorded at `DrawRelease.MIN_RELEASE_TICKS` as well as
> > here**, because that is the file an upgrade makes somebody open and this one is not.
> >
> > **AND THE MARGIN MOVES WHEN *WE* MOVE THE RULED VALUES, NOT ONLY WHEN MOJANG MOVES THE CURVE** —
> > which is how it just moved. `d`, `c` and `D` are all in the numerator's gift. **The upgrade
> > condition above is the one everybody watches; the ruling condition is the one that has actually
> > fired, twice.**
>
> **WHICH IS WHY THE RISK NOW POINTS THE OTHER WAY — see the headline section.** The question is no
> longer *would anyone charge*; it is *would anyone ever tap*. R4‴'s answer is that the tap is not a
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
it holds for taps). **There is no measured upper bound**, which is why the table above runs out to a
gap of **8** — `T = 23` in band 3 — rather than stopping at anything the repo can defend.
*A control carried past its precondition stops being a control*, and the 4 was being carried.

> **R4‴ ADDS A LOWER BOUND THE 3 NEVER HAD, AND IT IS NOT VANILLA'S.** Band 2 cannot be fired faster
> than `T = 9` and band 3 faster than `T = 15`, because **the hold is part of the shot.** Those two
> floors are OURS — they come out of `DrawRelease.bandFor`, not out of the jar — and unlike the 3
> they move the day the bands move.

**SECOND CITATION — THE MULTIPLES-OF-4 RULE IS NOT INHERITED HERE.** `CLAUDE.md`'s *author multiples
of 4* exists because **a held right-click delivers inputs on a 4-tick grid**. This weapon's input is
a **latch**, not a repeat (§1.4). **By the statement above, the grid's premise is measured only for
the repeat** — which is also why **R7's `cooldown_ticks: 0` needs no grid argument at all.**

**WHAT DISCHARGES IT: GATE ROW P7 (§8)**, with `/rpg firerate` — Q7's own instrument pointed at
discrete clicks instead of at a hold.

### 7.2a THE REAL INCENTIVE IS AMMUNITION, AND IT IS IN NO RATE TABLE

**What it decides, in one sentence:** *a tap-spammer runs dry sooner for the same work* — and that is
what a player actually feels, not a difference in a rate nobody can see.

> **THIS SENTENCE USED TO READ *"four times as often"* AND R4‴ KILLED THE NUMBER, NOT THE CLAIM.**
> The multiple is now **3.78× / 2.00× / 1.31× by band**, so a single figure cannot stand here. The
> sentence is weakened rather than re-quantified because **the honest summary is a ladder**, and the
> ladder is directly below.

```
25 rounds tapped     25 x 9 / 17 / 26     =   225 / 425 / 650 damage, by band
25 rounds charged    5 releases x 5 x 34  =   850 damage     3.8x / 2.0x / 1.3x by band
```

> ### ⚠ R4‴ TURNS ONE RATIO INTO THREE, AND THE TOP BAND ALMOST CLOSES IT
>
> **This section's whole claim was "four times", a single number.** With bands it is a ladder, and
> **the ladder's top rung is very nearly parity:**
>
> ```
> band 1    850 / 225  =  3.78x     stronger than the old 4.25x? NO -- weaker
> band 2    850 / 425  =  2.00x
> band 3    850 / 650  =  1.31x     <- the ammunition argument nearly VANISHES here
> ```
>
> **THE ARGUMENT THIS SECTION RESTS ON IS WEAKER THAN IT WAS AT EVERY BAND, AND ALMOST GONE AT BAND
> 3.** A player who taps at band 3 gets **76% of the magazine's charged value**, where a flat-8
> tapper got 24%. *"A tap-spammer runs dry four times as often"* was true of R4″ and is **false of
> R4‴'s top band.**
>
> **This is the one figure in the recompute that moved AGAINST the design's stated intent**, and it
> is recorded rather than smoothed: the ammunition brake was the argument that needed *no hit-rate
> assumption*, and band 3 is where it stops carrying weight. **The rate argument (§7.2, +110.9% at a
> 4-tick gap) is what holds band 3 back**, and that argument DOES rest on an unmeasured gap.
>
> **Not a defect, and not obviously fine either** — flagged for whoever runs P7, because it is the
> first time the two arguments for charging have pointed in different directions.

**It compounds with every reload, which a rate comparison never shows.** A band-1 tapper with no gap
empties the magazine in **72 ticks and then waits 60**:

```
duty cycle   band 1, gap 0     72 / 132  =  54.5% shooting
             band 3, gap 0    360 / 420  =  85.7% shooting   <- barely below the charger
             charging         260 / 320  =  81.3% shooting   was 83.3% at c=20
```

> **AND THE DUTY-CYCLE CONTRAST INVERTS AT BAND 3.** A band-3 tapper spends MORE of their time
> shooting than a charger does, because 15 ticks of hold per arrow is 360 ticks of magazine against
> the charger's 260. **The contrast was the argument; at band 3 it runs the other way.**
>
> **Same shape as the ammunition ladder above and the same conclusion:** what holds band 3 back is
> the RATE, not the brake. Recorded because a figure that reverses is the one nobody re-checks.

**The ammunition argument needs no hit-rate assumption**, which is exactly why it belongs beside the
rate table rather than inside it. **It is no longer the STRONGER of the two**, and that half of the
sentence was deleted rather than softened: at band 3 it is the weaker, and the rate table — the one
that *does* rest on an unmeasured gap — is what carries the verdict there.

### 7.3 CLOSED — **R9′**: `reload_ticks` = 60, **RULED AND NOT DERIVED**

**What it decided, in one sentence:** *how long you stand there doing nothing* — 3.0 seconds.

> ### ⚠ THIS VALUE RESTS ON A FEEL JUDGEMENT AND ON NOTHING ELSE, AND THAT MUST BE VISIBLE
>
> **90 had THREE supports. 60 has one.** A number with no derivation is one somebody later "fixes",
> so the account is here rather than the value alone:
>
> | support for 90 | status |
> |---|---|
> | the rate coupling | **DEAD**, twice over — below |
> | ticks per round | **OVERTURNED**, with the consequence in front of the operator |
> | pacing — *"longer than anything shipped, but not punishing"* | **the only one left, and it is a feel judgement** |

```
boltor         8 rounds / 60t = 7.50 ticks per round
locust        12 rounds / 60t = 5.00
quiver_stone   9 rounds / 34t = 3.78
PLUME         25 rounds / 60t = 2.40      <- the CHEAPEST ammunition in the project, by 36.5%
```

**~~R9 gives that title back to `quiver_stone`.~~ R9′ takes it straight back off it.** This section
argued, in its own words, that 3.60 was *"deliberately just UNDER `quiver_stone`'s 3.78"* and that
**"at the old 60 it was 2.40, the cheapest by 36%"**. **60 IS THAT VALUE, and the outcome 90 was
chosen to avoid is now the ruled one.**

> **SO DO NOT RE-DERIVE 60 FROM THE AMMUNITION COMPARISON.** That comparison was the argument FOR 90;
> it was heard and overruled. **Anyone who recomputes it will find 2.40 well under 3.78, read that as
> a defect, and be reversing a ruling rather than fixing an oversight.** The only thing that can move
> this number is another feel judgement.

> **~~THIS NUMBER IS LOAD-BEARING: below `reload_ticks` ≈ 71 the tap retakes the lead.~~ THAT
> COUPLING IS DEAD, AND IT HAS NOW DIED THREE TIMES — RE-DERIVED AT `c = 16` AND THE THREE BANDS,
> NOT CARRIED FORWARD.**
>
> A coupling declared dead under one parameter set is **not** dead under another, and that sentence
> is quoted in several places — so it was recomputed rather than inherited. **Solving
> `850/(260+R) = 25d/(kT+R)` for `R`, at each band's own floor on `T`:**
>
> ```
>                                          tie at reload_ticks
> band 1  T = 3    fencepost A                   -4.32     was -1.85 at d=8, c=20
> band 1  T = 3    fencepost B                   -8.40     was -5.77
> band 1  T = 4    fencepost A                  -36.96     was -33.23
> band 1  T = 4    fencepost B                  -42.40     was -38.46
> band 2  T = 9    fencepost A / B         -172 / -190     new with R4'''
> band 3  T = 15   fencepost A / B         -685 / -749     new with R4'''
> ```
>
> **THE SIGN AT `R = 0` HAS FLIPPED AND HAS NOW MOVED FURTHER FROM ZERO.** Under the pre-R8′ set a
> **zero** reload would have handed the lead back to the tap at `T = 3` (tie at +4). Under this one
> even a zero reload leaves the charge ahead, by more than before. **`reload_ticks` has stopped
> bearing on the rate comparison at all** — there is no value, however small, that revives it.
>
> **AND R4‴ DID NOT GIVE THE TAP A NEW WAY BACK IN, WHICH IS THE THING WORTH CHECKING AND NOT
> ASSUMING.** The higher bands hit harder, so the obvious worry is that one of them revives the
> coupling. **It is the opposite by two orders of magnitude**: their hold floors enter the
> denominator at 9 and 15 ticks, which costs far more than 17 or 26 damage buys. **Band 1 is the
> binding case at every quantity in this plan** — the tie, the margin, and now the coupling.
>
> **AND THE REASON IS NOT THAT THE RELOAD GOT SHORTER.** It has not moved since R9′; what moved is
> `c`, which shortened the CHARGED cycle from 360 to 320 and so raised the side the tap is chasing.
>
> **What the reload is still for is an AMMUNITION argument, which is not a rate one** — §7.2a's
> ladder. **That ladder is NOT untouched, and this sentence used to say it was:** R4‴ moved it from
> a flat 4.25× to 3.78× / 2.00× / 1.31×, and at band 3 it barely argues anything. Damage per ROUND
> is still the right unit; the conclusion it supports is just much weaker than it was.

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

### 7.5 ***CLOSED AGAIN*** — **R14: THE CHARGE SECOND `c` IS RULED AT 16**

**What it decides:** *how long a second of charge takes* — **0.8 seconds, ruled on feel 2026-09-15.**

> ### ⚠ R14 OVERTURNS R6/§7.0's 20, AND THE OVERTURNED REASONING IS RECORDED RATHER THAN SOFTENED
>
> **What §7.5 argued for 20, in its own words:** *a step is literally one second, so the sound ticks
> once per second and "every second adds an arrow" means what it says.* **That is now false of the
> weapon.** A step is 16 ticks, the ticks are 0.8 seconds apart, and *"every second"* is a name
> rather than a measurement.
>
> **THE NAME SURVIVES THE RULING AND `CHARGE_SECOND_TICKS` IS NOT RENAMED**, because the constant is
> *the thing a step costs* and that is what every call site wants. **The javadoc carries the
> mismatch explicitly** — a constant with "second" in its name holding 16 is exactly the sort of
> thing a later reader quietly "corrects" back to 20.
>
> **AND §7.5's OTHER ARGUMENT — THE ONE ABOUT TWO QUANTITIES — IS VINDICATED, NOT OVERTURNED.** It
> insisted `FULL_DRAW_TICKS` and `CHARGE_SECOND_TICKS` were different quantities that merely happened
> to share the value 20, and kept them as two constants on that basis. **The coincidence is now
> gone**, and because the file had kept them apart, R14 is a **one-constant change**: nothing had to
> be disambiguated, because nothing had been conflated. **That is the argument paying out.**

**What the ruling chose against, kept because a ruling with no alternatives beside it reads as a
default nobody considered.** ***RECOMPUTED AT R8′*** — at `D = 34` and `reload_ticks` 60:

| `c` | five arrows (step 3) | magazine cycle | charged dmg/s |
|---|---|---|---|
| **16 — RULED (R14)** | **52t (2.60s)** | **`5 × 52 + 60 = 320t = 16.00s`** | **53.13** |
| 20 — *was ruled (R6)* | 60t (3.00s) | `5 × 60 + 60 = 360t = 18.00s` | 47.22 |
| 24 | 68t (3.40s) | `5 × 68 + 60 = 400t = 20.00s` | 42.50 |

> **THE RULING TOOK THE ROW THE TABLE ALREADY HELD.** 16 was a live alternative under R6 and was
> passed over; it has now been taken. **Neither reading is a default** — the table is the record that
> both were in front of the operator, twice.
>
> **The spread does not narrow, which is the one thing the recomputation says.** Across the three
> candidates it was a factor of **1.31** at one arrow per second, **1.23** after R1′, and **1.25**
> at R8′ and still — essentially flat.
>
> > **AND THAT NEAR-REVERSAL IS WORTH ONE LINE, BECAUSE AN EARLIER REVISION CLAIMED A TREND.** It
> > read *"`c` matters LESS than it did"* off a single step from 1.31 to 1.23. **Two points are not a
> > trend**, and the third came back at 1.25. The ratio is set by `(20 + 2c)` against a fixed reload,
> > so it drifts with the reload rather than moving in one direction — **and the sentence that
> > generalised from two points was wrong within one ruling.**
> >
> > **R14 IS THE FOURTH POINT AND IT DID NOT MOVE THE SPREAD AT ALL**, because moving `c` moves which
> > ROW is ruled and not the ratio between rows. **A figure that survives a ruling aimed straight at
> > its subject is worth one line of confirmation** — the earlier draft would have reported it as
> > stable after two points and been wrong; it is reported as stable after four, for a stated reason.

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
- **It prices nothing itself — and §7 is now CLOSED, every number in it ruled.**

  ```
  arrow 1 at full charge   R6    charge second c = 16      R14    gravity 0.05       R12
  cooldown_ticks 0         R7    vanilla's 3-tick floor    R10    speed 2.5          R11
  max_lifetime_ticks 120   R11   1 / 3 / 5, three steps    R1'    the cap strands    R3a
  the pitch mapping        R13   the sound key             R13

  attack_damage 34         R8'   ) WAS ONE RULING, THREE FIELDS. Each was balanced against
  reload_ticks   60        R9'   ) the other two, so changing one alone re-opens the
  partial draw 9/17/26     R4''' ) arithmetic the others were settled against.
  ```

  > **AND THE THIRD FIELD HAS BEEN CHANGED ALONE, WHICH IS WHAT THIS BLOCK WARNED AGAINST.** R4‴
  > replaced the flat 8 with three bands while 34 and 60 stood. **The warning was honoured rather
  > than ignored**: the arithmetic the other two were settled against was *re-run*, in §7.1, §7.2,
  > §7.2a and §7.3, and one of those re-runs found the ammunition argument had weakened at band 3.
  > **R14 landed in the same PR for the same reason** — `c` prices the cycle every anchor divides,
  > so shipping it separately would have left §7 wrong on `master` in between.
  >
  > **THE HONEST STATE OF THE TRIPLE: it is no longer a triple.** `c` belongs in it and was never in
  > it; the tap is a ladder rather than a field. **Named here rather than re-drawn**, because
  > re-drawing it is a ruling and this file does not take those.

  **`reload_ticks: 60` IS RULED AND NOT DERIVED, AND §7.3 SAYS SO IN THOSE WORDS.** Of 90's three
  supports the rate coupling is dead and the ticks-per-round comparison was overturned; **pacing, a
  feel judgement, is the only one left.** A number with no derivation is one somebody later "fixes",
  so both the plan and `dragons_plume.yml` warn against re-deriving it from the ammunition table.

  **THE TAP'S NAME WAS THE THIRD ITEM AND IS RULED TOO** — the plain label, chosen over a named one.
  **The absent `name:` key is the decision**, which is why `dragons_plume.yml` says so at every
  binding: a name cannot be authored as an absence. **R4‴ extends that ruling from one line to
  three** — `"Tap1" / "Tap2" / "Tap3"` — which is flagged at the bindings as an extension rather than
  a re-taking.

  **`item: arrow` IS GONE, NOT RESOLVED-IN-PLACE — SLICE K DELETED IT.** The **BODY** ruling
  (2026-09-15) says the arrow body vanishes on resolve — no pickup, no sticking — so the body was
  never an item at all. **The derivation did not need ruling; it needed deleting**, and every cast
  now authors **`body: arrow`**: a real arrow entity, oriented along its own travel, inert in every
  other way. The two keys are **mutually exclusive and the loader refuses a file that authors
  both**, so the old key cannot come back by accident.

  > ### ⚠ THE ORPHAN EXIT IS RULED, AND WHAT WAS ACCEPTED IS NOT WHAT THE ITEM MARKER ACCEPTED
  >
  > **`spawnMarker`'s pre-age trick does NOT transfer, measured from the pinned jar.** An item
  > marker subtracts from `ItemEntity.LIFETIME`, a **constant compiled into the server**, and gets
  > an exact `expectedLifetimeTicks + grace` window. An arrow's limit is read from **config at
  > runtime**, and worse, `AbstractArrow.life` does not increment at all until
  > `tickCount > max-arrow-despawn-invulnerability` — **200 on this server, and unreadable from the
  > API**.
  >
  > ```
  > item marker    discard at  120 + 60          = 180t exactly    3.0s
  > arrow body     discard at  200 + 1           = 201t FLOOR     ~10.05s
  > ```
  >
  > **OPTION A RULED: accept the ~201-tick floor.** The two alternatives were rejected on the
  > record — lowering the config is **server-wide**, changing every arrow every player fires to fix
  > one weapon's decoration; and a removal mechanism of our own **is the failure it is meant to
  > cover**, since the third exit exists precisely because a scheduled chain can fail to run. That
  > second argument is `spawnMarker`'s own and nothing in the measurement touched it.
  >
  > ***AND THE ACCEPTED COST IS NOT WHAT IT SOUNDS LIKE: THE ORPHAN TRAVELS.*** An orphaned ITEM
  > marker stops — gravity off, velocity never renewed. An orphaned ARROW keeps flying on its last
  > velocity, because the no-physics branch is `setPos(position + delta)` and the only thing acting
  > on `delta` is a 1%/tick inertia that is **not** gated on `noPhysics`.
  >
  > ```
  > sum of 0.99^n over 201 ticks  =  86.74 tick-lengths  ->  216.8 blocks at speed 2.5
  > bounded above, any window     =  100 tick-lengths    ->  250 blocks
  > ```
  >
  > **So: "a stray body drifts up to ~217 blocks THROUGH TERRAIN for ten seconds", not "a stray body
  > hangs for ten seconds."** It cannot hit, damage, stick or be picked up while it does, and it
  > dies on vanilla's own timer with no code of ours running — a stronger guarantee than any
  > mechanism of ours could make, which is why A wins anyway. `GATE-plume-release.md`'s **R-K3** is
  > the row, and it says to look DOWNRANGE rather than at the impact point.

  **STILL UNRULED AND STILL MARKED AS SUCH:** the **homing constants** of §5, `INHERITED AND
  UNJUDGED` with gate rows P1-P3.

  **The sound key is ruled** — `block.note_block.hat`, on feel 2026-09-14 (§7.6), the only instrument
  there was for it — **and it survived R1′ untouched**, because Ben ruled the timbre and the
  amendment moved the ladder. **The MATERIAL being ruled is not R13 being discharged:** H-1b still
  owes the position reading, on the ruled key. R1′ made that row **easier** — three rungs to name
  instead of five — and **R14 has now made it HARDER again**: the three ticks land **0.8 seconds
  apart** rather than one second apart, so there is less time to place one by ear. **The row is
  noted, not restaged.**

  **What is NOT ruled is not a number:** the **homing constants** of §5, which stay
  `INHERITED AND UNJUDGED` with a gate row each.
- **It does not rule the Endermen or the summons exclusions.** R2 carries them as candidates; they need
  a distinction `core` does not currently have — `CombatantSnapshot` knows `player` and nothing else —
  so ruling them in costs either a port extension or a `paper`-side predicate. **Named, not chosen.**
- **It does not touch `CLAUDE.md`.** Nothing here is settled enough to be a rule yet; when the draw
  route is ruled, the one-line operational form belongs there and this file stays the account.
