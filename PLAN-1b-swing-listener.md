# PLAN — Commit 1b: the real left-click, via the swing packet

**Plan only. No code until this is reviewed.** 1b is the first Netty-thread code in
the project. A threading race here works with one player and corrupts state at forty;
it cannot be playtested to confidence, so this document is where it gets caught —
the same standing the region-threading port had.

Read `CLAUDE.md`'s THREADING section first. It governs everything below.

## Context

Commit 1a proved the weapon spine with `/rpg swing_TEMP`, a command standing in for a
real click. 1b deletes that command and makes a genuine **left-click** fire the held
weapon's `left_click` trigger, by reading the client's **arm-swing packet** through
PacketEvents. Bukkit's `PlayerInteractEvent` is the wrong tool here: it misses
left-click-on-air and can double-fire. The swing packet is the correct, reliable signal
and is the first legitimate, non-speculative use of PacketEvents in the project.

Everything downstream of "who swung" already exists and is unit-proven: `WeaponService.fire`
(selection + gate-free firing, atomic cooldown/energy), `CastExecutor.execute` (effects),
attribution (aggro + kill credit). **1b adds almost no new pure logic — it adds packet
plumbing and one thread hop.** That is precisely why its risk is threading, not logic,
and why the verification below leans on review and a real-server observation rather than
claiming a unit test proves the thing it cannot.

---

## A correctness point the parent plan under-stated — resolve it before coding

`PLAN-weapons-elements-classes.md` says the swing listener's first branch reads
`weaponId` "off the player's held `ItemStack`" *in the packet callback*. **That is a
Bukkit call, and it must not run on the Netty thread.** `player.getInventory()
.getItemInMainHand()` reads inventory state the main thread may be mutating — the exact
"works at one, corrupts at forty" race `CLAUDE.md` forbids. `WeaponItems.heldWeaponId`
is a `paper` Bukkit read; it belongs **inside the hop**, not in `onPacketReceive`.

So the fast-path reject splits into two tiers:

- **Netty-thread tier (free, no Bukkit, first branch):** is this the arm-swing packet,
  main hand, from a `Player`? If not → `return`. Pure inspection of the packet and its
  PacketEvents wrapper. Allocates nothing, touches no Bukkit. An empty hand, a punch, a
  dirt block, a right-click — all fail this or carry no tag and cost effectively nothing.
- **Entity-thread tier (inside the hop):** read `heldWeaponId` (now a legal Bukkit call),
  and if untagged → `return` before building an aim or snapshot. This is where the
  weapon_id reject actually happens, because reading it is a Bukkit operation.

This is the single most important review point in 1b. It corrects the parent plan and
aligns with `CLAUDE.md`: *inside a packet callback, read the packet, compute, cancel —
touch nothing else.*

**Filter order is load-bearing, and it is exactly this (Confirm 1):**

```
1. packet-type reject   (ANIMATION? else return)      -- FIRST, on Netty, free
2. main-hand + Player   (off-hand / non-player? return) -- on Netty, free
3. hop                  (bukkit(player, ...))
4. weapon_id check      (untagged? return)             -- inside the hop, Bukkit-legal
```

The packet-type reject **must be the first branch**, because movement and look packets
are the high-frequency traffic — dozens per player per second — and none of them may
reach `scheduler.onEntity`. A main-hand swing hopping is fine: a swing is a *click*, not
a *tick*. Every packet hopping is not. If the type filter is not first, you are hopping
on every packet, and that is a different and much worse cost than the one the fast-path
was written to avoid.

---

## The listener

`WeaponSwingListener extends PacketListenerBase` in `paper/.../packet/`, constructed with
`(Scheduler, Keys, WeaponRegistry, WeaponService, AdapterContext)` and registered at the
existing PacketEvents registration point in `RpgPlugin.onEnable` (beside — or replacing —
`ExampleTelegraphListener`, which is a do-nothing reference and can stay or go). It reuses
`PacketListenerBase.bukkit(player, …)` verbatim — the sanctioned hop, already
`scheduler.onEntity(player, task)` — and does **zero** Bukkit work in the callback body.

```
onPacketReceive(event):
    if (event.getPacketType() != PacketType.Play.Client.ANIMATION) return;   // free, pure
    // main-hand only: an off-hand swing is not an attack. Read from the wrapper
    // (WrapperPlayClientAnimation#getHand) -- PacketEvents data, not Bukkit.
    if (hand(event) != InteractionHand.MAIN_HAND) return;                    // free, pure
    if (!(event.getPlayer() instanceof Player p)) return;                    // free, pure

    // The ONLY statement after the filter. Everything Bukkit lives inside the hop.
    bukkit(p, () -> onSwing(p));

onSwing(Player p):   // runs on the entity/region thread -- Bukkit is legal here
    heldWeaponId(p, keys).ifPresent(id ->
      weapons.find(id).ifPresent(weapon -> {
        Aim aim = <eye>;  CombatantSnapshot caster = BukkitCombatant.snapshot(p, adapters);
        weaponService.fire(caster, weapon, "left_click", aim)
            .ifPresent(result -> onSuccess -> scheduler.onRegion(eye, execute));  // as swing_TEMP does
      }));
```

**Packet choice.** `ANIMATION` (arm swing) is the common denominator of every left-click
— on air, on a block, on an entity — so the weapon's `Melee` cast then finds its own
targets in the arc. `PLAYER_DIGGING(START_DESTROY_BLOCK)` and `INTERACT_ENTITY(attack)`
are alternatives, but each covers only one target kind and firing on several risks a
double-fire per click. Recommend `ANIMATION`, main-hand only; the exact wrapper accessor
and any de-dup are confirmed at implementation and are a review point.

---

## How the hop is proven — stated honestly

**Directly asserting that the world-touch runs on a region/entity thread and *not* on
Netty is NOT unit-testable.** Thread identity is a runtime property of the real
schedulers: a fake `Scheduler` runs the task inline on the calling thread, and a genuine
region thread exists only on a booted server. No unit test can distinguish them. I will
not dress a fake's inline call up as proof of a region thread.

The verification is therefore three layers, and the plan names each for what it is:

1. **Unit — the hop defers (nameable, real).**
   `PacketListenerBaseTest.bukkitHandsTheTaskToTheSchedulerInsteadOfRunningItInline`:
   inject a fake `Scheduler` that records `onEntity` calls without running them, plus a
   minimal stub `Player` whose only live method is `isOnline() == true`; call
   `bukkit(stubPlayer, task)`; assert the task **did not run on the calling thread** and
   **was handed to `scheduler.onEntity`**. This proves the callback defers world-work
   through the hop rather than doing it inline — the unit-expressible half of "nothing
   Bukkit runs on the Netty thread." It does **not** prove the destination thread's
   identity; that is the server half below.

2. **Unit — the packet filter (nameable, real).**
   `WeaponSwingListenerTest.onlyTheMainHandArmSwingCountsAsASwing`: pin a pure predicate
   over `(PacketType, hand)` so non-swing packets and off-hand swings are rejected before
   any player or item work. This is the only genuinely new pure logic 1b adds.

3. **Real server — the thread identity (observation, not a unit test, and named as such).**
   A one-boot instrumented check in the region-threading-port style: inside `onSwing`
   assert `Bukkit.isOwnedByCurrentRegion(p)` (or `isPrimaryThread()` on non-Folia) is
   **true**, and in the callback confirm no Bukkit work ran; log once, observe on boot,
   then remove before the commit lands. This is the layer that actually witnesses the
   region thread — and it is explicitly a review/boot check, not a test in the suite.

The structural guarantee underpinning all three: the callback body is a pure filter plus
a single `bukkit(p, …)`; every Bukkit statement is inside the hopped runnable. That is a
review invariant, and layer 1 is its unit-level proxy.

---

## What replaces `swing_TEMP`

`swing_TEMP` is **deleted in this commit** — the `.then(Commands.literal("swing_TEMP")…)`
branch and the `swingTemp(...)` helper come out of `RpgCommand`, along with any imports
left unused by their removal. After 1b, **no debug command survives to players**: `/rpg`
carries `abilities`, `cast`, `class`, and `give` — all permanent — and the swing is a
real left-click, not a command. `/rpg give` **stays** (it is how you obtain a weapon; it
was always permanent Phase-1 work). The `rpg.command.give` description in `paper-plugin.yml`
loses its "Also gates the temporary swing_TEMP" clause.

Mechanical proof at the end of 1b: `git diff` shows `swing_TEMP` removed and no other
`_TEMP`/debug command added; the new file lives under `packet/`, which is where 1b's code
*should* be (the inverse of 1a's "zero `packet/` files" check).

---

## Right-click, and the vanilla collision — out of scope for 1b, named where they land

**1b wires left-click only.** Right-click — the `emberblade`'s costed `right_click`
special, and the bow's shot/special assignment — is **out of scope**. It lands with
**weapon #2 (`emberblade`, the free+costed sword)** and **weapon #3 (the bow)** in the
parent plan's Phase-1 slice, because right-click is where the costed trigger and the
`use-item` packet first matter, and the bow is what forces the right-click-vs-vanilla-draw
decision. `WeaponService.fire` is already input-parameterized (`"left_click"`), so adding
right-click later is wiring a second packet plus a cancellation, not re-architecting.

**Two collisions that ride with that work, flagged so 1b does not silently inherit them:**

- **Vanilla left-click damage — neutralised in 1b, and tested at the decision (Confirm 2).**
  A real left-click on a mob also deals the item's vanilla attack (an iron sword's ~6),
  *on top of* the trigger's content damage. `swing_TEMP` never had this; 1b introduces it.
  1b neutralises the vanilla path at mint: `WeaponItems.mint` adds an `attack_damage`
  attribute modifier that cancels the player's base swing (base 1.0 → total 0), leaving the
  trigger's content damage authoritative. No packet cancellation, no cache, threading
  untouched.

  **The failure mode is invisible except in-game** (modifier not applied → double-hit),
  so it is verified two ways, each named for what it is:
  - **Unit — the decision (measured constraint: an `ItemStack` cannot be built in a unit
    test; `new ItemStack(...)` throws `No RegistryAccess implementation found`, and the
    project has no MockBukkit).** So the test pins the *decision*, not a constructed item:
    the suppressor targets the **`attack_damage`** attribute — the vanilla melee path —
    with an amount that **zeroes the base swing**, and the weapon's own damage flows
    through `EffectSpec.Damage → CombatantHandle.applyDamage`, which never touches an
    attribute (already proven damaging with no item in `WeaponServiceTest`). The
    load-bearing assertion is *distinct-from-trigger*: "zeroed the wrong damage path" is
    the review-passing, world-failing bug, and this is the assertion that catches it. To
    prevent drift, `mint` derives the Bukkit `Attribute` from the same string constant the
    test asserts, so the constant is the single source of truth.
    *(MockBukkit rejected: it may have no release matching year-based Paper 26.1, and a
    test dependency that lags the platform becomes a Paper-upgrade gate — the PacketEvents
    problem on the test side.)*
  - **Real-server boot — the outcome (named observation, not a suite test):** a real
    left-click swings, deals the trigger's damage **only**, and does **not** double-hit.

- **Packet cancellation needs Netty-thread weapon knowledge — which is why cancellation is
  deferred, not forgotten.** A packet can only be cancelled *synchronously in
  `onPacketReceive`*, never after the hop. So any future "this weapon replaces the vanilla
  interaction" (right-click eat/place/draw, or hard-cancelling the left-click attack)
  requires knowing "is holding a weapon" **on the Netty thread** — which means a
  concurrent, main-thread-maintained held-weapon cache, since the held-item read is Bukkit.
  That cache is real new surface with its own coherency correctness, and it belongs with
  the right-click/bow commit that first needs cancellation — **not** in 1b. Recording it
  here so no one later "fixes" the missing cancellation by reading the inventory on Netty.

---

## Files (1b)

**New:** `paper/.../packet/WeaponSwingListener.java`; tests
`PacketListenerBaseTest` (hop-defers), `WeaponSwingListenerTest` (packet filter), and
`WeaponItemsTest` (the melee-suppressor decision).
**Edited:** `RpgCommand.java` (delete `swing_TEMP` + `swingTemp`), `RpgPlugin.java`
(register the listener), `paper-plugin.yml` (give description), `WeaponItems.java`
(attack-damage suppressor at mint, derived from a tested string constant), and `Keys.java`
(a NamespacedKey identity for the suppressor modifier).

## Verification (1b)

- `./mvnw -pl core test` unaffected (core untouched); `./mvnw clean package` green, and
  the two named unit tests pass — with each proven falsifiable (break the filter → the
  packet test reddens; make `bukkit` run inline → the deferral test reddens).
- Real-server boot with the one-shot instrumentation (layer 3): swing a tagged weapon,
  confirm the world-touch is logged as region-owned and the callback did no Bukkit work;
  then remove the instrumentation.
- In-game acceptance: give `ironblade`, **left-click** a mob (no command) → it takes the
  trigger's damage, aggros, and credits on kill; an empty-hand click and a vanilla sword
  do nothing weapon-related; `swing_TEMP` is gone.

## This commit comes back for review before it lands

Same as the region-threading port. The listener is the Tier-3 piece of Phase 1; the plan
is reviewed now, and the implementation is reviewed before merge. Do not fold it into any
other commit.
