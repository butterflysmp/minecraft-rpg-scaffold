# DESIGN — Damage System (stat engine phase 3)

The captured target for custom damage. This phase makes custom health *combat*:
basic attacks and ability payloads both deal **custom damage** through one path,
draining custom HP via the observability seam (which drives hearts, nameplates, and
the new damage-number popup). Death is deferred to the next pass; this phase builds
the hook for it.

Read `DESIGN-stat-engine.md` first — this is its phase 3.

---

## Implementation note (2026-07-17): this phase ships in passes

The captured target below is written whole; it is being built in passes, and two things the doc
states generically turned out to need splitting against *this* codebase:

- **Pass 1a — the player→mob damage pipeline (DONE).** Unify ability + basic-attack damage through
  custom HP (`applyDamage` → `CombatantStats.damage`), the `reachedZero` death hook (built,
  unconsumed), knockback opt-in (default none, vanilla KB cancelled), token-can't-kill floor. The
  **mob nameplate** is the witness (it drains by the custom number). Core-tested + boot-witnessed.
- **Pass 1b — the per-dealer damage-number popup (NEXT).** Its own pass: net-new PacketEvents
  surface (client-only Text Display, protocol-26.1 metadata indices, fake entity ids, rise-and-fade).
  Isolated so packet-index risk doesn't ride with damage-math risk. The nameplate already witnesses
  1a, so the popup is a nicer witness, not a blocking one.
- **Pass 2 — mob→player (LATER).** The one place the "ride the vanilla `EntityDamageByEntityEvent`"
  language below literally applies. **Player→mob does not ride an incoming event** — a weapon swing is
  packet-driven and abilities fire no event; the flash is instead *tokened* (melee's own vanilla
  swing, capped + KB-cancelled) or triggered *manually* (`playHurtAnimation`, ability path). Only
  **mob→player** (a zombie hitting a player) has a real incoming event to ride, and it carries an
  **i-frame feel fork** (preserve vanilla player invulnerability windows vs. reset them — a
  swarm-melts-you balance call) decided deliberately there.

The rest of this document is the unchanged captured target.

---

## The core problem, and the pattern that solves it

Naively, "custom damage" sounds like "replace vanilla combat damage." But full
replacement throws away things we want to keep: **the mob's red hurt-flash, the hurt
sound, the hurt animation, i-frames** — vanilla's *cosmetic* reaction to being hit.
We want those. What we want to *own* is the mechanical part: the damage **amount**
(custom, uncapped) and the **knockback** (custom, or none per weapon).

So the realization: **"vanilla damage" is a bundle — cosmetic reaction + mechanical
effect — and we want to keep the cosmetics and replace the mechanics.**

### The pattern: ride the event, don't replace it

Let the vanilla `EntityDamageByEntityEvent` **fire** (that's what triggers the flash,
sound, animation, i-frames), but neuter its *mechanical* effects and apply our own:

1. **Don't cancel the event** — cancelling kills the flash. Let it through.
2. **Set the vanilla damage to a token amount** (near-zero, e.g. 0.01) — enough for
   vanilla to react (flash/sound/i-frames), not enough to matter mechanically.
   Vanilla health is our *display* layer (small heart-count) — the token barely moves
   it, and death is driven by custom HP, not vanilla, so a token can't kill.
3. **Always cancel vanilla knockback** — we own knockback now.
4. **Apply our real custom damage** to the `CombatantStats` store — the mechanical
   damage, in our numbers, firing the `HealthChange` seam (→ hearts, nameplate, popup).
5. **Apply our custom knockback** — the weapon/ability's *declared* knockback, or none.

Net: vanilla reacts cosmetically (red flash kept), we own the number and the
knockback. We're **riding** vanilla's event for the cosmetics while owning the
mechanics — the surgical answer to "keep the flash but control damage and knockback."

### The invariant this rests on (display ≠ truth, again)

The token vanilla damage **must not** meaningfully drain the display bar or trigger
vanilla death. Vanilla health is a puppet; custom health is truth; **death fires from
custom HP, never vanilla.** This is the phase-1 half-heart-floor invariant carried
forward: the floor already keeps vanilla health from hitting 0 and killing a
live-in-custom-terms entity. A test must confirm the token damage can't cause a
vanilla death while custom HP is positive.

---

## One damage path — both basic attacks and abilities

**A single custom-damage entry point:**

```
applyDamage(target, amount, source, knockback?)
```

- drains `target`'s custom current HP by `amount`,
- fires the `HealthChange` seam (amount, target, source/dealer, dealerIsPlayer,
  newCurrent, max, **reachedZero**),
- applies `knockback` if declared (else none),
- clamps custom HP at 0 (does **not** kill — death deferred).

**Both** basic attacks and ability payloads route through this one path:

- **Basic attacks** — the ride-the-event flow calls `applyDamage` with the attacker's
  weapon damage + the weapon's declared knockback.
- **Ability payloads** — Ember Step's "8 fire damage", Rekindle's burst, etc. become
  custom-HP damage *through the same `applyDamage`*. They already deal "damage"; this
  phase makes that damage flow through the unified path.

**Why one path:** two damage systems (basic-attack damage ≠ ability damage) would need
separate balancing, resistance handling, popup wiring — everything twice. One entry
point means damage is balanced once, resisted once, displayed once. Ability damage and
sword damage are the same currency through the same door.

---

## Knockback — opt-in per weapon/ability

Knockback is **declared, not default.** More than ~25% of weapons (Mage weapons
especially) apply *no* knockback, so "no knockback" is the common case, not a special
one — you shouldn't have to write `knockback: none` on a quarter of the content.

- **Default: no knockback.** A damage/on-hit spec with no `knockback` field deals none.
- **Declared: a `knockback` field** (strength, and direction model — away-from-source
  is the usual) applies custom knockback, tuned per weapon/ability in content.
- The ride-the-event flow **always cancels vanilla knockback**, then applies the
  declared custom knockback (or nothing). So a Mage staff cancels vanilla KB and adds
  nothing (mob takes damage, flashes, doesn't move); a Melee weapon cancels vanilla KB
  and adds its declared KB.

Content-driven and tuned on the `--refresh-content` loop, like every other number.

---

## Death — deferred, but the hook is built

**Death is the next pass, not this one.** This phase: damage drains custom HP and
**clamps at 0** — an entity at 0 custom HP sits at the phase-1 half-heart floor,
alive. That is a **deliberate phase boundary**, documented like the floor was, so a
boot where a mob hits 0 and doesn't die reads as "death is next phase," not "damage is
broken."

**But build the hook now, unconsumed** — same discipline as building the observability
seam before the popup existed:

- The `HealthChange` event carries a **`reachedZero`** signal when a hit brings the
  target to 0 custom HP.
- Nothing consumes it this phase. Next phase's death system hooks `reachedZero → die`
  **without reopening `applyDamage`.**
- If damage just clamped at 0 silently, the death phase would have to retrofit death
  detection into the damage path. Emitting `reachedZero` now means death is "consume
  the signal," not "reopen the engine."

Document explicitly: **0-custom-HP-not-dead is a known temporary state** (in the plan,
the commit body, and a code comment near the clamp), the same way the half-heart floor
was documented as a scaffold for absent death.

---

## The damage-number popup (built with the system, as its witness)

Per-player floating damage numbers, visible **only to the dealer** — the reason
PacketEvents is in the project, and the reason the observability seam was built two
phases early.

- **Hooks the `HealthChange` seam** — every `applyDamage` fires it; the popup listens.
- **Per-player targeted** — the popup packet goes *only to the dealer* (identified via
  `dealerIsPlayer` + the dealer's identity on the event). A client-side-only display
  (packet-based text display / hologram), never a real entity — so only the dealer
  sees their own numbers, no shared-world clutter. Same per-viewer packet discipline
  as the mob nameplate (name never touched, purely visual, isolated).
- **Built alongside the damage system as its witness** — the popup is *how you
  boot-verify* custom damage works, the way hearts witnessed phase 1 and the nameplate
  witnessed phase 2. You hit a mob, you see your number float up; the number is the
  proof the custom-damage path fired.

Content/feel: number formatting, color (maybe by element/crit later), rise-and-fade
animation, position above the target. Tunable; start simple (white number, rises,
fades).

---

## Scope

**In this phase:**
1. `applyDamage(target, amount, source, knockback?)` — the one custom-damage path;
   drains custom HP, fires the seam (with `reachedZero`), clamps at 0.
2. Basic attacks ride the vanilla event: fire it (keep flash/sound/i-frames), token
   vanilla damage, cancel vanilla knockback, call `applyDamage`.
3. Ability payloads (Ember Step, Rekindle) route through `applyDamage` — one path.
4. Knockback opt-in per weapon/ability (declared `knockback` field; default none;
   vanilla KB always cancelled).
5. The damage-number popup — per-dealer PacketEvents floating numbers, hooks the seam,
   the witness for this phase.
6. `reachedZero` signal on the seam — built, unconsumed, for the death phase.

**Explicitly OUT (later phases):**
- **Death** — entity dying at 0 custom HP (death messages, drops, respawn). Next pass;
  this phase builds only the `reachedZero` hook.
- **Attack damage / attack speed as custom STATS** — this phase, a weapon's basic-attack
  damage can be a simple declared number; making attack damage a `base + modifiers`
  custom stat (like health) is the stat-expansion phase. (Confirm: is basic-attack
  damage a flat content number this phase, or already the custom stat? Lean: flat
  number this phase, stat later — keep phase 3 about the damage *pipeline*, not the
  stat system's expansion.)
- **Damage types / resistances / crits** — the damage is typed by `element` already
  (content), but resistance/weakness math and crits are later. One number, one path,
  now; modifiers on it later.
- **PvP damage rules** — whether/how players damage players. Basic attacks this phase
  are player→mob and mob→player; player→player is a later rules decision.

---

## Testable (core) vs boot-witnessed (paper)

- **Core (unit):** `applyDamage` math — HP drains by amount, clamps at 0, `reachedZero`
  fires exactly when a hit reaches 0 (mutation: skip the signal → reddens). Knockback
  declared-vs-default resolution (a spec with no `knockback` → none; with one → that
  value). The unified-path property — an ability payload and a basic attack produce the
  same `applyDamage` call shape.
- **Boot (human, named):**
  - **Red flash kept** — hit a mob, it flashes red / plays hurt sound (the ride-the-
    event cosmetics survived). *The witness that we didn't throw away vanilla's reaction.*
  - **Custom damage lands** — the popup shows the number, custom HP drains by it
    (nameplate updates), vanilla health barely moves (token). *Display ≠ truth in action.*
  - **Knockback opt-in** — a KB weapon knocks the mob back; a no-KB weapon (Mage) deals
    damage + flash but *no movement*. Both cancelled vanilla KB.
  - **0-HP-not-dead** — damage a mob to 0 custom HP; it sits at the half-heart floor,
    alive (the documented phase boundary — death is next).
  - **Popup is per-dealer** — only the dealing player sees their numbers; a second
    player doesn't see the first's popups.
  - **No vanilla death from token** — a mob with positive custom HP never dies from the
    token vanilla damage.

---

## Load-bearing correctness (what the tests/boot must guard)

- **Token can't kill** — vanilla token damage never causes a vanilla death while custom
  HP is positive (display ≠ truth; death is custom-only).
- **One path** — basic attacks and ability payloads both go through `applyDamage`; no
  second damage route that skips the seam/popup/knockback rules.
- **`reachedZero` fires correctly** — exactly when a hit reaches 0, so the death phase
  can trust it. Mutation-guarded.
- **Knockback default is none** — a spec without a `knockback` field applies no
  knockback (not a fallback default that sneaks KB onto Mage weapons).
- **Popup isolation** — per-dealer only, client-side packet, never a real entity, never
  touches shared state (same discipline as the nameplate).

The damage math, clamp, reachedZero, and knockback resolution are pure → core-tested.
The ride-the-event cosmetics, the popup packets, and the feel are paper → boot-
witnessed. Enforce the math in core; name the cosmetics/popup for boot.
