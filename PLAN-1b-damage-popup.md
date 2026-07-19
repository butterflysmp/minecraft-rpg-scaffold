# PLAN — pass 1b: the damage-number popup

Read `CLAUDE.md` first. Branch off `feat/damage-pipeline-1a` (merge 1a first, or branch
from its tip). This is the packet-protocol pass, split OUT of 1a precisely so packet risk
is isolated from damage-math risk — so the discipline here is **verify the wire format
against the real protocol, boot-witness it, don't trust a ported constant.**

## What this is

A floating damage number shown to the **dealer only**, over the target, for ~0.75s, as a
**fake client-side `TEXT_DISPLAY`** sent via packets — never a real server entity, never
broadcast. It hooks the `HealthChange` seam pass 1a already fires; the seam was built
carrying `dealer` / `dealerIsPlayer` for exactly this.

## Where it hooks (seam is ready, one wire)

Add a new `HealthListener` (the popup) to the composite at `RpgPlugin.java:133`:

```java
this.stats = new CombatantStats(new CompositeHealthListener(healthSystem, nameplates, damagePopup));
```

`CompositeHealthListener` (verified) isolates delivery: a throwing popup will not break
combat or the heart bar / nameplate, and it can't propagate back into the store mutation
(a live combat tick). So the popup owns its own error handling and can fail safe.

## The mechanism — extracted from the old `DamageIndicatorService` (`/tmp/bsmp`, `refactor/god-class`)

Port the MECHANISM, not the structure. What the old service actually does, per hit:

1. Allocate a **fake entity id** from an `AtomicInteger` counting **down from
   `Integer.MAX_VALUE`**. The server assigns real ids counting up from 0, so the ranges
   can't collide within a server's uptime (ids reset on restart). One id per number, never
   reused.
2. **Spawn packet** — `WrapperPlayServerSpawnEntity(entityId, Optional.of(randomUUID),
   EntityTypes.TEXT_DISPLAY, Vector3d(x,y,z), yaw=0, pitch=0, headYaw=0, data=0,
   Optional.empty())`.
3. **Metadata packet** — `WrapperPlayServerEntityMetadata(entityId, [ billboard, text,
   style ])` with three `EntityData` entries (indices below).
4. **Send both to the dealer only** via `PacketEvents.getAPI().getPlayerManager()
   .sendPacket(viewer, …)`.
5. **~0.75s later, destroy** — `WrapperPlayServerDestroyEntities(entityId)`, guarded by
   `viewer.isOnline()`. Old lifetime is `LIFETIME_TICKS = 15L`.

Old cosmetic constants (carry as named constants, re-verify the indices — see Step 0):
`BILLBOARD_CENTER = 3`; `STYLE = 0x07` (shadow | see-through | default-background);
text via `EntityDataTypes.ADV_COMPONENT`; billboard/style via `EntityDataTypes.BYTE`.

## What the scaffold already gives you (build on it, don't reinvent)

`PacketNameplateSender` is the project's existing packet-SEND and the template for this —
same lib (`packetevents-spigot`), same `WrapperPlayServerEntityMetadata`, same per-viewer
`sendPacket`. Reuse its hard-won discipline:

- **A fresh wrapper per send.** Its comment: *PacketEvents frees a wrapper's buffer after
  encoding, so one must never be shared across sends.* Build spawn/metadata/destroy fresh
  each hit (the old service already does).
- **Indices are verified, not ported.** It re-checked base-Entity index 2 (custom name) / 3
  (visible) against minecraft.wiki for 26.2 vs the server's 26.1 rather than trusting the old
  project's stale constant. Do the same for the Display/TextDisplay indices (Step 0).
- **Never `BukkitRunnable` / `Bukkit.getScheduler().runTaskLater`.** The old service uses
  `runTaskLater` for the destroy — that is FORBIDDEN here (`Scheduler` javadoc). Schedule the
  destroy through the project `Scheduler`: `scheduler.onEntityLater(dealer, destroy, 15)` —
  tying it to the dealer means it self-drops if the dealer logs off, replacing the old
  `isOnline` guard with the lifecycle-as-acceptance pattern the codebase already uses.

## The one real design fork — resolving (dealer Player, target Location) from a UUID-only seam

`HealthChange` carries `target` and `dealer` as **UUIDs**, `newCurrent`, and the faction
bits — but **no `Location` and no `Player` object.** The popup needs the target's position
(to spawn the number) and the dealer's `Player` (to send to). This is the decision to make
before coding:

- **Recommended (Option A — ride the seam):** the popup is a paper `HealthListener`.
  `onChange` fires **inside `applyDamage`'s `scheduler.onEntity(target, …)` block — i.e. on
  the target's own owning thread** (next tick), so reading the target entity's `Location`
  there is legal. Steps: filter (`kind == DAMAGE`, `dealerIsPlayer`, `dealer != null`),
  resolve the target entity by UUID → read its `Location`, format the text (pure), resolve
  the dealer `Player`, send spawn+metadata, schedule the destroy via `Scheduler`.
- **CONFIRM before building:** that `onChange` really runs on a thread where the target
  entity is legal to read (trace it in the real 1a `applyDamage`; on this Paper server it's
  the main thread, but state it as verified, not assumed). And that resolving the dealer
  `Player` + `sendPacket` is safe off the dealer's region — default to routing the send (and
  the destroy) through `Scheduler.onEntity(dealer, …)` so nothing touches Bukkit off-thread.
- **Fallback (Option B):** if `onChange`'s thread can't legally read the target entity,
  capture the `Location` in `applyDamage` itself (it already holds the target entity) and
  hand it to a dedicated popup dispatch, instead of resolving off the generic seam. Costs a
  second call path; buys correct, on-thread location access. Pick A if the thread check
  passes; fall back to B only if it doesn't.

## Scope — lock these now (defer, don't silently drop)

- **Damage only.** Gate on `kind == DAMAGE`. No heal popups this pass (the seam has `HEAL`;
  defer green heal numbers).
- **Player dealers only.** `dealerIsPlayer` gate. Pass 1a is player→mob, so dealers are
  players; when mob→player lands later, a mob dealer simply shows no number until that pass
  decides the self-facing case.
- **No crit color yet.** The old project colored crit `§e` / normal `§f`, but crit isn't in
  the engine — `CritRoll` was never ported and the seam carries no crit bit. Render all
  numbers one color (white/normal) this pass; wire crit coloring when attack-damage/crit
  land as custom stats (a later pass). Do NOT reopen the seam to add a crit bit now.
- **No stacking/offset polish.** Rapid hits spawn numbers at the same point and overlap;
  leave it. A small random offset / vertical drift is later polish, not 1b.

## Core / paper split + reddening tests (green tests aren't the gate, but the pure parts still get guarded)

Pure → `core`, unit-tested and reddened; wire format → `paper`, boot-witnessed (like
`PacketNameplateSender`, which has no wire unit test — the protocol is proven at boot).

- **`DamageNumberText.of(amount)` → Component** (pure, core, mirrors `NameplateText.of`):
  the number's formatting/rounding and color. Reddening test: assert the rendered string /
  color; flip the format to redden it. (When crit lands, this is where the color branch goes.)
- **Fake-id allocator** (pure): counts strictly down, never repeats. Reddening test: two
  allocations differ and decrease; break it (return a constant) to redden.
- **Show predicate** (pure): `shouldShow(change)` = `kind == DAMAGE && dealerIsPlayer &&
  dealer != null`. Reddening test over the `HealthChange.Kind` cases (sealed/enum — cover
  DAMAGE/HEAL/MAX_CHANGE). Break the gate to redden.

The packet wrappers, indices, and `EntityDataTypes` are NOT unit-tested — they're the
boot-witnessed surface. That's the whole reason 1b is isolated.

## Step 0 — verify the wire format BEFORE writing the send (the isolated risk)

The old constants (`META_BILLBOARD = 15`, `META_TEXT = 23`, `META_STYLE = 27`) are for the
old project's protocol version. A wrong index silently routes the number into another field
(or spawns an invisible/garbage entity) — it won't throw. Before coding the sender:

1. **Re-verify the TextDisplay/Display metadata indices against minecraft.wiki for the
   server's protocol (26.1; wiki documents 26.2 — reconcile as `PacketNameplateSender` did).**
   Base Entity 0–7, Display block, then TextDisplay (text, line width, background, style…).
   Confirm billboard, text, and style indices and their `EntityDataTypes`.
2. **Confirm the PacketEvents API surface against the pinned `packetevents-spigot` version**
   (it's inherited, not pinned inline in `paper/pom.xml` — resolve the effective version):
   the `WrapperPlayServerSpawnEntity` constructor **arity/signature** (it shifts between PE
   releases — the old 9-arg form may not match), and that `EntityTypes.TEXT_DISPLAY` and
   `EntityDataTypes.ADV_COMPONENT` exist under those names.
3. Only then write the sender. Comment each index with the wiki reference and version, same
   as `PacketNameplateSender`.

## Boot verification — the real gate (this is a boot-only, per-viewer, protocol property)

- A number appears **over the target**, showing the **exact damage dealt**, and vanishes
  after ~0.75s.
- It is visible to the **dealer only** — a second player watching sees **no** number
  (per-viewer isolation is the whole point).
- **No real entity is left behind:** after the number expires (and after rapid repeated
  hits), the target area has no lingering `TEXT_DISPLAY` — check `/data`, F3, or a
  second-client view; confirm no entity-id leak under a burst of hits (Ember Step / Rekindle
  multi-hit is the hard case — many numbers, all cleaned up).
- The number tracks the real damage from `applyDamage`, matching the nameplate drop 1a
  already verifies (the popup and the plate read the same seam event — they must agree).
- Ability multi-hits (Rekindle fan, swept-line) each produce their own number to the caster.

## Commit shape & scope guard

Suggested: one commit for the pure core pieces + tests, one for the paper sender + wiring +
boot notes (or fold if small). If a general "floating text" framework, an entity-tracking
manager, or crit/heal support is proposed, **decline** — 1b is the damage-number popup on the
existing seam, damage-only, player-dealer-only, one color. Everything else is a later pass.
