# DESIGN — The Build System (Ultimates, Actives, Passives, Aspects & Fragments, UI)

Status: **design target, not scheduled work.** This captures a system large enough
to be re-litigated three times if it isn't written down. Nothing here is built.
The near-term move (last section) is deliberately much smaller than the whole.

This doc assumes the merged state as of the Phase-3 branch: weapons exist, elements
are content, and a **kit** is the `(class, element)` grant of weapons + abilities.
The build system turns that static kit into a **player-assembled loadout**.

---

## Why this exists — the deferred verdict

Two thin cells (Fire Ranger, Fire Mage) were played head-to-head. Verdict:
**"not enough content to make them drastically different."**

That is a content-depth finding, **not** an axis-failure finding — the distinction
matters and must not be collapsed. Two kits of 2-3 near-identical abilities cannot
make a class *feel* like a class; the thing that would differentiate them (a deep,
distinct pool per cell) does not exist yet. So the class-axis verdict is **deferred
until a cell is deep enough to feel**, not answered.

The build system is what makes a cell deep. But building the *whole* system before
confirming depth makes classes distinct is the 24-kit mountain one level up — an
entire loadout-crafting system on an unproven assumption. So this doc separates
**the target** (the full system) from **the test** (the smallest thing that proves
depth matters). Build the test first.

---

## The core architectural shift

Today a kit **is** its content file — `ranger_fire.yml` grants a fixed list. The
build system changes that:

- A cell's content file defines a **pool** — the ultimates, actives, passives,
  aspects, and fragments *available* to that (class, element).
- Each player has a **loadout** — which ultimate, which actives, which aspects,
  which fragments in which slots — that they assembled and that persists.

That is the **definition/instance split**, and it is now the *third* time this
project has hit it: `AbilityDefinition` vs a runtime cast; a weapon base vs a
rolled instance (Phase 4 loot); and now a kit *pool* vs a player *loadout*. The
pattern is familiar, which de-risks it — but the consequence is real: **the player
profile gains loadout state**, and that is a schema bump plus a place to store "what
this player has equipped."

`PlayerProfile` already carries `schemaVersion` and has migrated before (v1→v2 for
the two-axis class/element split). A loadout field is the same kind of change.

---

## The five parts, from cheapest to build to most expensive

### 1. Actives — you already have these

An "active" is an `AbilityDefinition`: a cast, a cooldown, a cost, effects. The only
new thing is **selection** — a pool of N actives, of which the player equips some
smaller number into ability slots. That's loadout state + a picker, not new engine.

### 2. Ultimates — an active wearing a bigger number

An "ultimate" is an active with a large cost and a long cooldown (or a separate
charge resource, if you later want one). Pick 1 of 2-3. Mechanically this is an
active in a dedicated slot with a choice attached. **No new effect type** — it's
numbers and a slot. The only genuinely new sub-question: do ultimates charge on a
separate meter (kills/damage/time) rather than the shared energy pool? That's a
resource-model decision, deferrable until ultimates are actually built.

### 3. Passives — the first genuinely new engine concept

Everything in the engine today is a **cast**: an effect resolves at a moment,
against a target or point, and finishes. `EffectSpec` is sealed into `Targeted`
and `Untargeted`, and every variant is a thing that *happens when triggered*.

A passive is **always-on while equipped**: "move faster while not taking damage,"
"abilities cost 10% less after standing still 2s," "arrows pierce." It is not
triggered by a cast — it is a modifier on the player's state that holds as long as
it's slotted.

This is new because it needs:
- **a passive definition** — a content type describing an always-on effect and its
  condition, distinct from `AbilityDefinition` (which is inherently a cast);
- **an application point** — something that reads a player's equipped passives and
  keeps their effects live, re-evaluating on the relevant trigger (tick, damage
  taken, movement). This is *not* the cast pipeline; it's a separate loop.
- **a decision on expressiveness**, which is the same fork as weapon enchants
  (Phase 4): are passives **stat modifiers** (bounded numbers — +speed, −cost,
  +range) or **behaviors** (pierce, chain, on-hit effects = attaching abilities to
  a permanent trigger)? Stat-modifier passives are a small, bounded system.
  Behavior passives are a sub-language. **Recommendation: stat-modifier passives
  first** — they are the biggest *felt* differentiator for the least engine, and
  they're the thing most likely to make Ranger ≠ Mage before any aspect exists.

Passives are the highest-leverage new concept in this whole doc: always-on effects
change how a class *feels* without the player pressing anything, which is exactly
what "the classes don't feel different" is asking for.

### 4. Aspects & Fragments — the Destiny 2 two-tier system

Precise, because "more slots" undersells it. In Destiny 2:

- An **Aspect** is a big, build-defining choice. Each Aspect grants an
  ability/behavior **and** a number of **Fragment slots**. You equip a small number
  of Aspects (2-ish).
- A **Fragment** is a smaller modifier slotted into the slots Aspects provide. You
  have limited slots, and some Fragments carry **stat penalties** — so slotting is a
  genuine tradeoff, not just accumulation.

The genius: Aspects define the build's *shape*, Fragments *tune* it, and the slot
economy (Aspects grant slots, Fragments consume them, penalties force choices) makes
every loadout a real decision. The top tier gates how much of the bottom tier you
get.

Mapped here:
- An **Aspect** ≈ a passive-or-behavior grant + a slot count. Structurally it's a
  passive (part 3) that also modifies the loadout's capacity.
- A **Fragment** ≈ a small stat-modifier (part 3's stat-passive shape) with an
  associated slot cost and possible penalty.
- The **slot economy** is new: validation and UI logic that tracks slots granted vs
  consumed and enforces the budget. It's data + rules, not deep engine — *once
  passives exist*, an aspect/fragment is largely a passive with slot accounting.

So Aspects & Fragments are **cheap once passives exist** and **impossible before**.
They sit on top of the passive system; they are not a separate engine.

### 5. The build UI — the biggest fork in the doc

"A user-friendly way for players to select and build their kit" has two
implementations that are *wildly* different in cost:

- **In-game inventory UI** (chest-GUI): clickable item-icons in a menu, standard
  Bukkit. Entirely in-plugin, no external dependencies. But building a
  loadout-crafting screen out of item frames is **clunky** — it fights the medium,
  and complex build screens end up feeling like spreadsheets made of icons. Every
  Minecraft RPG does this and they all feel a bit like that. Cheapest, ships with
  the plugin, playable.
- **External UI** (web app or resource-pack-driven custom interface): vastly better
  UX, vastly more work, a separate skill set (frontend + a plugin↔UI bridge), and
  effectively **its own project**. "User-friendly" quietly means "the hardest
  frontend work in the codebase" if you go this route.

This fork is milestones away and does not need deciding now — but it must be *named*,
because "friendly build UI" hides a project-sized decision. **Recommendation:** when
the UI is actually needed, ship the **in-game chest-GUI first** (it proves the
loadout system end-to-end and is playable), and treat an external UI as a later,
optional upgrade *if* the game warrants it. Do not let the desire for a polished UI
block shipping a functional one.

---

## The dependency order (if the whole thing is ever built)

1. **Actives-with-selection** — loadout state + picker. Small.
2. **Passives (stat-modifier)** — the first new engine concept. The high-leverage one.
3. **Ultimates** — an active in a choice-slot; resource-model sub-decision.
4. **Aspects & Fragments** — sits on passives; the slot economy is the new part.
5. **Behavior passives / behavior fragments** — only if stat-modifiers prove too
   limited. This is the sub-language; defer hard.
6. **Build UI** — chest-GUI first; external UI only if warranted.

Each tier depends on the one before. Passives (2) is the gate: almost everything
rich depends on "always-on effect" existing.

---

## THE NEAR-TERM TEST — build this, not the above

The whole system exists to serve one deferred verdict: **do classes feel distinct
when a cell is deep?** You do not need aspects, fragments, or a UI to answer that.
You need two cells with *genuinely different, deeper* pools. The minimum:

1. **Deeper active pools** — author several more actives per cell (content, today,
   on the working tuning loop) so Fire Ranger and Fire Mage draw from *different*
   ability sets, not 2-3 near-identical ones. This is pure YAML.

2. **One passive each (stat-modifier only)** — the single cheapest lever that makes
   a class feel different without the player doing anything. E.g. Ranger: "move
   faster while not recently hit"; Mage: "abilities cost less after standing still."
   This requires building **only** the stat-modifier passive concept (part 3's
   smaller half) — one new content type, one application loop, no aspects, no slots,
   no UI.

3. **Then replay Ranger vs Mage** with real pools + a defining passive each. *That*
   is the axis verdict. If kite and commit now feel like different games → the axis
   holds, and the full build system is worth its cost. If they still blur → the
   problem is deeper than content, and no amount of aspects/fragments will fix it.

This tests the exact assumption the whole build system rests on, at the cost of one
new engine concept instead of five. It is the vertical slice, one level up.

---

## What to do right now

- **Merge the two Phase-3 cells** (machinery + Ranger + Mage). Solid foundation; the
  verdict staying open does not invalidate them.
- **Keep this doc** as the captured target so the build system isn't re-derived.
- **Do not start the grid, and do not start the build system.** Build the near-term
  test: deeper pools + one passive each. Replay. Then decide.

## Forks left open, on purpose

- **Stat-modifier vs behavior** for passives *and* fragments — same fork as weapon
  enchants (Phase 4). Recommendation: stat-modifier first, both places.
- **Ultimate charge source** — shared energy vs a separate meter.
- **Build UI** — in-game chest-GUI (cheap, clunky, ships) vs external (great,
  project-sized). Name it now; decide when the loadout system needs a face.
- **The axis verdict itself** — still deferred, now correctly, until a cell is deep.
