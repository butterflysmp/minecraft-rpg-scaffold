# PLAN — Weapons, Elements, Classes, and Loot

This is a design-and-build plan for three interlocking systems. It records
decisions already made in discussion, so they don't get re-derived, and it fixes
a build order that front-loads the thing you can *feel* and back-loads the thing
that's hardest to author.

Read `CLAUDE.md` and `NEXT.md` first. Nothing here overrides the invariants:
`core` stays dependency-free, content stays data not code, sealed switches stay
exhaustive, and a check that never ran looks exactly like a check that passed.

---

## The model, decided

Three identity axes. A player is a **(Class, Element)** pair and wields
**Weapons**. None of the three is a weak/strong combat system.

- **Element is identity, not math.** Fire, Water, Nature, Undead, Void, Wither,
  and **Kinetic** — where Kinetic is the deliberately flavorless *neutral*
  element. Element is **mandatory and never null**: a weapon or ability with no
  special element is `kinetic`, not absent. This is load-bearing — see Phase 2.
  An element flavors a kit and gates what abilities/weapons a player may use. It
  does **not** multiply damage against anything. There is no elemental triangle,
  matrix, or shield interaction. This is a deliberate, permanent exclusion —
  named the same way Renovate was declined. If "Water beats Fire" is ever wanted,
  it is a new system built from scratch, not a stub to preserve.

- **Class is playstyle.** Melee, Ranger, Mage, Summoner. The class decides *how*
  you fight — which cast shapes your kit uses, your range, your risk profile.

- **The (Class, Element) grid selects a kit**, and every cell is its own
  hand-authored kit. Fire Melee shares nothing with Water Melee unless a file
  chooses to. Maximum expressiveness, maximum authoring: 6 elements × 4 classes =
  24 cells, though the grid may be **sparse** (see below).

- **Weapons are the moment-to-moment combat.** Abilities are cooldown-gated
  punctuation; the weapon is what you do in between. A kit is a weapon plus
  abilities — which is why you cannot evaluate a kit without a weapon in hand, and
  why weapons come first.

---

## Why this order: Weapons → Elements → Classes

Milestone 1 answered "does an ability fire correctly." It did **not** answer "is
combat fun," because there is nothing to do between casts. A weapon is that
something. Build it first and every later "is this fun" question becomes
answerable; build it last and you tune 24 kits against dead air.

Elements come before Classes because removing `multiplierAgainst` is real surgery
on the damage path (it is live, called at `EffectApplier` line 59, and tested),
and it is cleaner to do that on today's small content set than after weapons and
kits reference elements everywhere.

Classes come last because the hard one — Summoner — needs an engine capability
that does not exist, and should not block the three classes the engine already
serves.

---

# PHASE 1 — WEAPONS — **DONE** (`1dcaf0a`…`559aa68`)

> #### 2026-07-10 — all three archetypes shipped and proven on a real server
>
> Phase 1's weapons are complete. The three-weapon vertical slice below was built and
> booted, each played before the next, exactly as planned:
>
> - **`ironblade`** — the spine, in two commits. 1a (`e3d5b74`): weapon content type,
>   `WeaponLoader`, `WeaponDefinition`, held-item lookup, `/rpg give`, a gate-free
>   `WeaponService.fire`, and a temporary `/rpg swing_TEMP`, all proven server-free in
>   `core` with `FakeWorld`. 1b (`d5a2012`): the PacketEvents swing listener replaced the
>   temp command — a real left-click, on a Netty thread, hopping via `PacketListenerBase`
>   before touching Bukkit. The hop's deferral and the packet filter are unit-tested; the
>   region-thread identity was witnessed on a real-server boot (`isOwnedByCurrentRegion`
>   true), the requireOwned discipline. Vanilla melee is neutralised by an attack-damage
>   item modifier at mint, so the swing deals only the trigger's damage.
> - **`emberblade`** (`d0d1f38`) — the free + costed sword. Right-click via Bukkit
>   `PlayerInteractEvent` (reliable for right-click, main-thread, no packet cache), a
>   dispatch-only handler in the one `RpgListeners`. The costed special spends the shared
>   `energy` pool and is blocked when drained — the shared-resource model, proven server-free
>   and on a real server. Per-trigger cancellation: only a weapon that binds `right_click`
>   suppresses the vanilla interaction, so ironblade still opens doors.
> - **`hunters_bow`** (`559aa68`) — the ranged weapon. A free `Projectile` shot on
>   right-click, click-to-shoot, fire rate as a cooldown (no charge). The right-click firing
>   and per-trigger cancellation were **reused unchanged** — the bow is content plus one new
>   piece, per-weapon `material` (the bow is the first non-sword item). The vanilla draw is
>   suppressed for the bow (it binds right_click) but a plain vanilla bow still draws.
>
> Booted and played: all three weapons load, `/rpg give` mints each, left-click swings,
> right-click specials fire and spend, fire rate gates the bow, no double-hit or double-spend,
> and vanilla interactions are suppressed only for the inputs a weapon binds. 208 tests green,
> each new guard confirmed falsifiable by a reverted mutation.
>
> What is **not** yet done in Phase 1: nothing in the weapon slice. Elements remain the
> Destiny placeholders (`SOLAR` etc.) until Phase 2; `emberblade`/`hunters_bow` carry `solar`
> as the fire-theme placeholder. The record below is the plan as written, kept intact.

The insight that makes this small: **a weapon is a container of triggers, and each
trigger is an ability with an input attached.** Left-click fires one trigger,
right-click another. Every trigger resolves through the existing
`AbilityService` → `CastExecutor` path, spends from the existing shared `energy`
pool, and respects a cooldown via the existing `CooldownTracker`.

Your three weapon archetypes are three *shapes of one YAML schema*, not three code
paths:

- **free-only** — one trigger, no cost (a basic sword)
- **free + costed** — a free left-click, a costed right-click special
- **costed-only** — every trigger spends resource (a staff)

Decided parameters:
- Weapons fire on a **cooldown** (swing speed / fire rate). Reuses `CooldownTracker`.
- Costed weapon triggers spend the **same `energy` pool** as abilities. A free
  weapon lets energy regen while you swing; a costed weapon competes with your
  abilities. That tension is intended.
- Bows are **click-to-shoot**, not draw-and-charge. Fire rate is a cooldown stat,
  so it composes with future fire-rate buffs — the reason charge was rejected.
- Weapons are obtained via **`/rpg give <weapon>`** for now. Loot comes later.
- Weapons carry a **`rarity:`** field and a **mandatory `element:`** field
  (defaulting to `kinetic`). Both are **reserved data in Phase 1** — they load,
  they can color the item name, but rarity's mechanical meaning (enchant slots and
  strength) is Phase 4, and element gating is Phase 3. Authoring them now, inert,
  teaches their shape before later phases depend on them — the same reason abilities
  carried `element` before gating existed.

### Rarity ladder (reserved data)

Six tiers, linear, no special-case flag. Exotic is simply the top tier — not
Destiny's one-equipped rule. Store the tier→color→(slots, strength) mapping in
**one** place (a rarity content type or config), never inline — same rule as `Keys`.

| Rarity | Color (`NamedTextColor`) |
|---|---|
| Common | `WHITE` |
| Uncommon | `GREEN` (lime) |
| Rare | `BLUE` |
| Epic | `DARK_PURPLE` |
| Legendary | `GOLD` |
| Exotic | `AQUA` (cyan) |

Readability check when weapons first render names: `DARK_PURPLE` (Epic) and `BLUE`
(Rare) sit close in dark inventory text. Not a reason to change the palette —
just confirm Rare and Epic are distinguishable at a glance, since that matters in a
loot game and is trivial to verify. File under "check when names render," not
"decide now".

### Schema (illustrative — the loader defines the truth)

```yaml
# content/weapons/emberblade.yml
id: emberblade
display_name: "Emberblade"
element: fire
triggers:
  left_click:
    cast: melee
    reach: 3.5
    cooldown_ticks: 10
    cost: free
    on_hit:
      - { type: damage, amount: 8 }
  right_click:
    cast: projectile
    speed: 1.4
    cooldown_ticks: 200
    cost: { resource: energy, amount: 40 }
    on_hit:
      - { type: damage, amount: 20, element: fire }
```

Every field except `triggers`/`left_click`/`right_click` is already parsed by
`AbilityLoader`. A trigger is structurally an `AbilityDefinition` plus an input.
**Do not duplicate the ability schema — reuse it.** If `AbilityDefinition` and a
weapon trigger diverge, that is a smell; a trigger should *be* an ability with an
input binding, sharing the loader for cast/cost/effects.

### The genuinely new engine work

1. **Weapon content type** — `content/weapons/*.yml`, a `WeaponLoader`,
   `ContentValidator` coverage for its ability-shaped references. Same pipeline
   proven three times. `WeaponDefinition` lives in `core` (it is data + rules);
   the loader lives in `paper`.

2. **Held-item → weapon resolution** — read `ctx.keys().weaponId` (the PDC key
   `weapon_id`, stubbed since day one and used by nothing) off the player's held
   `ItemStack`, look up the `WeaponDefinition`.

3. **Input → trigger dispatch** — the one real new seam. An input event maps to a
   trigger; the trigger's cast runs through `AbilityService`. This is where the
   sharp edges live.

4. **`/rpg give <weapon>`** — mint an `ItemStack` tagged with `weapon_id` in its
   PDC (via `Keys`, never an inline `NamespacedKey`). Placed in the player's **first free
   inventory slot**; if the inventory is full, tell them and don't drop it.

**Weapon identity, and the hot-path rule.** An item is one of ours **iff** it
carries the `weapon_id` PDC tag. No tag → not a weapon → the system does nothing,
vanilla behavior is untouched. The swing listener's **first branch** is "held item
has no `weapon_id` → return" — the cheapest possible rejection, before any thread
hop, lookup, or allocation. This matters because the swing packet fires for every
player on every click; an empty hand, a dirt block, a vanilla sword all hit this
fast path and cost nothing.

**Vanilla items are deliberately out of scope for Phase 1.** A plain iron sword
does *nothing* in the weapon system — it stays vanilla. Bringing vanilla gear into
the system (mapping `Material`s to weapon definitions) is a real sub-system with its
own decisions (does a diamond sword differ from a wooden one?) and is a **named
future phase**, not an omission. Recorded here so no one later "fixes" the untagged
no-op by guessing a default.

### The core test seam — build the logic server-free

A weapon trigger is structurally an `AbilityDefinition` with an input binding, so
**firing a trigger must be unit-testable in `core` with `FakeWorld`**, exactly like
an ability cast. Do not let the weapon path grow up as untestable `paper` code
because it happens to start from an item and a packet.

The split: `core` owns *given this trigger and this aim, resolve the cast and apply
the effects* — proven with `FakeWorld`, no server. `paper` owns *item in hand →
which trigger → which aim*, and hands resolved inputs to `core`. The item lookup,
the PDC read, and the packet are `paper`'s; the combat outcome is `core`'s. This is
the same snapshot/handle discipline the ability system already follows — a weapon
firing should reach the identical `AbilityService`/`EffectApplier` path, tested the
identical way.

**Acceptance includes:** a `core` test that a weapon trigger fires its cast and
applies its effect with no Bukkit present. If that test needs a server, the seam is
in the wrong place.

### The two sharp edges

**Left-click detection — this is the Tier-3 part, and the one to review carefully.**

Bukkit's `PlayerInteractEvent` for left-click is unreliable: it misses
left-click-on-air and can double-fire. Reading the swing/dig **packet** via
PacketEvents is the correct tool, and this is the first legitimate, non-speculative
use of PacketEvents in the project.

It is also the first code that runs on a Netty I/O thread. `CLAUDE.md`'s threading
contract is absolute here: **first branch is the untagged fast-path reject** (no
`weapon_id` → return, on the I/O thread, touching nothing); then **read the packet,
touch nothing Bukkit, hop to the owning region thread via the `Scheduler` before
resolving the cast.** A race here will not reproduce with one player and will corrupt state at
forty. `PacketListenerBase` already encodes the hop — the swing listener extends
it and does its world-touching inside `bukkit(player, …)`, never in
`onPacketReceive`.

**This listener gets its own commit and its own review.** Do not fold it into the
weapon-content commit. It is the one piece of Phase 1 where a second opinion earns
its keep, for the same reason the region-threading port did.

**Right-click collides with vanilla** — eating, block placement, bow draw. A held
weapon must cancel the vanilla interaction (`event.setCancelled(true)` on the
Bukkit interact event for a weapon-tagged item). Contained, but real. Note the
collision: a bow is click-to-shoot on left or right? Decide when the bow is built;
if right-click is the special slot generally, the bow's shot may want left-click
and its special right — resolve it against the first bow, not in the abstract.

### Phase 1 vertical slice — one capability per weapon

Build and **play** each before the next. The milestone-1 discipline.

1. **`ironblade`, free-only sword — in TWO commits, packet code isolated.**

   The whole spine plus the packet listener is too much for one commit, and the
   packet listener is the risky part. Split it, so a failure is attributable:

   - **1a — spine, no packets.** Weapon content type, `WeaponLoader`,
     `WeaponDefinition`, held-item lookup, `/rpg give`, and a **temporary
     `/rpg swing` command** that fires the held weapon's `left_click` trigger.
     This proves `/rpg give` → held-item lookup → trigger → Melee cast → damage →
     aggro + credit (free, via existing attribution), and the `core` test seam,
     with **zero packet code**. A red here is a spine problem.

   - **1b — the packet listener, its own commit and its own review.** The
     PacketEvents swing listener replaces `/rpg swing`: a real left-click now fires
     the trigger. A red here means the packet wiring or the thread hop is wrong,
     not that the spine is broken. This is the Tier-3 commit — see the threading
     note below, and send it for review before it lands.

   Same isolate-the-risky-thing logic that put `check-jar.sh` before the rename: a
   known-good baseline first, the dangerous change alone against it.

2. **`emberblade`, free + costed sword.** Adds `right_click` as a second trigger
   and proves the shared-resource model: the special spends the same `energy` an
   ability would, and a drained pool blocks it.

3. **`hunters_bow`, click-to-shoot bow.** Proves the ranged trigger (Projectile)
   and forces the right-click/vanilla-draw cancellation.

### Phase 1 acceptance

- Each weapon is a `.yml` file; the only Java is the four engine pieces above,
  landed *before* the content. Adding the 4th weapon must be content-only —
  prove it by adding one whose name is in no `.java`, booting, and giving it.
- The swing listener: a mutation confirming a cast fired from a packet actually
  hops threads (assert the world-touch ran on a region thread, not the I/O thread)
  — or, if that is hard to assert directly, at minimum a test that the listener
  does nothing Bukkit-touching in `onPacketReceive`.
- In-game: give `ironblade`, swing at a mob, it takes damage and aggros. Give
  `emberblade`, confirm the special spends energy and is blocked when empty. Give
  the bow, confirm it shoots and does not trigger a vanilla draw.

Weapons carry an `element:` field now, deliberately — it is inert in Phase 1 (no
gating yet), but authoring it here teaches the element tag's shape before Phase 3
depends on it.

---

# PHASE 2 — ELEMENTS

Two separable pieces: **delete the combat math**, then **make elements content**.

### 2A — Remove `multiplierAgainst`

It is live, not a stub-in-name. `EffectApplier` line 59 multiplies damage by
`d.element().multiplierAgainst(target.state().shieldElement())`, `ElementTest`
asserts its 1.5×/1.0× behavior, and `CombatantSnapshot` carries a `shieldElement`
field that exists only to feed it.

Removing it is real surgery, in this order:

1. `EffectApplier` — damage becomes `d.amount()`, no multiplier. The `Damage`
   effect still *carries* an element (for theming and future gating), but it no
   longer resolves against a shield.
2. Delete `Element.multiplierAgainst` and its tests in `ElementTest`.
3. Delete `shieldElement` from `CombatantSnapshot`, its read in `BukkitCombatant`,
   and the `shield_element` PDC key in `Keys`. Nothing else should reference it —
   grep to confirm, because a leftover reader is a silent no-op.
4. The javadocs on `CombatantHandle`/`CombatantSnapshot` that explain the
   multiplier's design must go too, or they become lies about deleted code.

**Acceptance:** `grep -rn "multiplierAgainst\|shieldElement\|shield_element" core paper`
returns nothing but this plan's memory. Damage is now `amount`, full stop, proven
by a test that a Solar hit and a Void hit for the same `amount` deal the same
damage to the same target.

**This is a subtraction commit.** It should *reduce* line count. If it grows the
codebase, something was reframed instead of removed.

### 2B — Elements become content

Once no code switches on `Element.FIRE`, the *set* of elements stops being a
design axis the compiler balances around, so the enum's justification is gone.

1. Replace the `Element` enum with the real seven: Fire, Water, Nature, Undead,
   Void, Wither, and **Kinetic**. Kinetic is the **neutral** element — flavorless,
   the value that means "no special element." Element is **mandatory; null is never
   a state.** This is deliberate and it is the reason 2A and 2B are adjacent: 2A
   deletes nullable-element handling from the damage path, and 2B must **not**
   reintroduce it. "Elementless" is spelled `kinetic`, never absent, so nothing
   downstream — weapons, gating, loot — ever branches on null.

   **Decision to confirm:** enum or content?
   - **Enum** (smaller step): edit the six values, keep `fromName`. Adding a
     seventh element later is a recompile. Fine if the set is genuinely fixed.
   - **Content** (`content/elements/*.yml`): an element is an `id` +
     `display_name` and nothing else — it carries no logic now. Adding "Lightning"
     later is a file. This is the more consistent choice given elements are pure
     identity, and it is barely more work since the enum has almost no behavior
     left after 2A.
   - **Recommendation:** content. The only thing the enum still did was
     `multiplierAgainst`, and 2A deletes that. A `Set<String>` of valid element
     ids, loaded at boot and validated, replaces it. `Damage.element` becomes a
     `String` id rather than an enum value.

2. `ContentValidator` gains element validation: every `element:` referenced by a
   weapon or ability must name a defined element. Same `Predicate<String>` seam
   as visuals/statuses. A dangling element id warns at boot, naming the file.

3. The Destiny elements (`KINETIC/SOLAR/ARC/VOID`) leave the codebase entirely,
   including the sample content that uses them (`solar_grenade` etc. get
   re-elemented or renamed — decide whether they survive as Fire/Void or get
   replaced).

**Acceptance:** boot with the six real elements; a weapon tagged `element: fire`
loads; a weapon tagged `element: plasma` warns at boot and the server still
starts. No `SOLAR`/`ARC`/`KINETIC` anywhere.

---

# PHASE 3 — CLASSES — **3A + 3B SHIPPED & CI-VERIFIED** (`b721c92`, `385864b`)

> #### 2026-07-11 — the (class, element) gate and two cells shipped; the grid is deliberately NOT filled
>
> Phase 3's engine (3A) and its vertical slice (3B) are built, booted, and CI-verified.
> `master` is at `385864b`, pushed to `origin` as part of run
> [29141597199](https://github.com/butterflysmp/minecraft-rpg-scaffold/actions/runs/29141597199):
> **214 tests green** across three modules (core 101, storage 17, paper 96 — the
> per-module guard counted report files, not just a total), `check-jar.sh` printed
> `Jar OK` (RpgPlugin present, core+storage bundled), no guard step skipped.
>
> - **3A — the gate (`b721c92`).** Commit F's archetype gate is generalized from
>   `archetype` to the composite **(class, element)**. The Archetype content type is
>   fully retired (`Archetype`/`ArchetypeRegistry`/`ArchetypeLoader`/`hunter.yml` and
>   their tests deleted), replaced by a **kit** system: `KitDefinition`/`KitRegistry`
>   keyed on a `KitKey(classId, elementId)` **record — never a concatenated string**
>   (the `("ranger_f","ire")` vs `("ranger","fire")` collision is impossible by
>   construction, proven by a reverted-mutation test). `PlayerProfile` gained
>   `elementId` (schema v1→v2, additive migration). A kit grants **weapons and
>   abilities together**; weapons carry a `WeaponGrant(weaponId, equip)` so the
>   `equip: true` weapon lands in the main hand on selection — a fresh class never
>   swings an empty hand. `/rpg class` + `/rpg element` set the two axes; the gate
>   **fails closed** with explicit precedence (half-selected → "choose a class and an
>   element"; both-set-but-no-kit → "that combination isn't available yet"; real kit →
>   grant). `ContentValidator.validateKits` warns on a kit whose grants don't resolve.
> - **3B — TWO cells, not one (`b721c92` Ranger, `385864b` Mage).** The written plan
>   below says "author *one* cell." Execution deliberately authored **two, same
>   element** — `ranger_fire` (kite: free sustained bow, energy freed for the
>   `arc_surge` utility) and `mage_fire` (commit: a costed `ember_staff` + costed
>   spells all drawing one energy bar). The reason is honest: one cell proves the
>   *wiring*, but it **cannot** prove the Ranger-vs-Mage economy axis is a real
>   distinction — that needs kite and commit played back-to-back. Same element on both
>   isolates the class as the only variable.
>
> **What is NOT done, and is deliberately withheld:**
> - **The grid is unfilled — 2 of 24 cells.** This is not an omission; it is the plan.
>   The verdict "does kite differ from commit, or is it one loop with different
>   numbers?" is owed to **head-to-head play**, and it decides whether the remaining 22
>   cells are worth authoring. A fun Ranger cell does **not** greenlight the grid.
> - **The design verdict is unrecorded** because it hasn't been played yet. When it is,
>   it belongs here.
> - **3C (Summoner) remains a deferred milestone** — the engine still has no
>   owned-autonomous-entity system; unchanged by this work.
> - Elements are still combat-inert (Phase 2's decision); a kit's element flavors and
>   gates, it does not multiply damage.
>
> The record below is the plan as written, kept intact — including the "author one
> cell" instruction that execution refined to two.

Depends on Commit F (`4f3032a`), which gates casting on a player's archetype and
made `PlayerProfile.archetypeId` load-bearing. Phase 3 generalizes that gate from
`archetype` to `(class, element)`.

### 3A — The grid, for the three engine-served classes

Melee, Ranger, Mage need **no new engine work** — they map onto cast shapes that
exist:

- **Melee** → `CastSpec.Melee`
- **Ranger** → `CastSpec.Ray` / `CastSpec.Projectile`
- **Mage** → `CastSpec.Projectile` + `Area` effects

**Ranger vs Mage — decided.** Ranger leans on **free-cost, bow-based** attacks:
sustained, energy-neutral, the bow carries the damage and energy is freed for
utility. Mage leans on **paid staffs and costed abilities**: managing an energy
budget across weapon and abilities *is* the class. The distinction is a **resource
economy**, not a cast shape — which is what makes it a real class axis rather than
three coats of paint.

Consequence for the schema: **a class implies a weapon economy**, so a kit grants
**weapons and abilities together**, not abilities alone. A Ranger kit grants
bow-shaped free weapons; a Mage kit grants staff-shaped costed weapons.
`content/kits/{class}_{element}.yml` lists both.

One thing to design *toward* when authoring the first cell (a play-and-tune
question, not a plan one): make Ranger's free economy mean "energy freed for
movement/utility," not merely "you can ignore the energy bar." A class whose whole
identity is skipping a mechanic plays *less* of the game.

Mechanics:

1. `PlayerProfile` gains `elementId` beside `archetypeId`. Schema-versioned
   already (`schemaVersion` exists); this is a migration bump, a solved problem
   here.
2. The grant lookup keys on **(class, element)**, not class alone. Commit F's
   "archetype grants a list of ability ids" becomes "the kit for (class, element)
   grants **weapons and abilities**." `content/kits/{class}_{element}.yml`.
3. Commands: `/rpg class <class>` and `/rpg element <element>` (or one
   `/rpg pick <class> <element>`). A player needs **both** set before they can
   cast — decide what a half-chosen player sees (recommend: refused, "choose a
   class and an element").
4. `ContentValidator` checks kits: every granted ability resolves, and — reusing
   Commit F's empty-set warning — a kit granting zero valid abilities warns.

**Sparse grid:** the 24 cells need not all exist. A player picking an unauthored
(class, element) combo is refused ("that combination isn't available yet"), which
is the empty-kit case Commit F already handles. Author cells as you build them;
`ContentValidator` warns on a *referenced* missing cell, not on every empty one.

### 3B — Vertical slice: one cell, played, before the grid is filled

24 kits is a content mountain and you have four abilities. **Do not author the
grid. Author one cell completely** — pick the (class, element) that excites you,
give it a weapon from Phase 1, its abilities, its element flavor — and **play
it.** If one cell is fun, the grid is worth filling. If it isn't, you've saved 23
kits of wasted tuning. Same discipline as milestone 1: one vertical slice before
scaling.

### 3C — Summoner, deferred as its own milestone

Summoner does **not** fit the engine. Every effect today resolves against a
target/point and finishes. A summon *persists*: it creates an owned entity that
acts autonomously, has a lifetime, must not hit its owner, and should credit its
owner's kills. That is a new `EffectSpec.Summon` plus an owned-entity system —
lifecycle, scheduling (the summon ticks), ownership/attribution, cleanup on
logout/death.

**Open question that scopes it:** how autonomous? "A stationary turret firing
every 2s" is nearly an `Area` effect wearing an entity costume — cheap, maybe
expressible soon. "A wolf that pathfinds, picks targets, and fights" is a real AI
system and a milestone unto itself. Decide where on that spectrum Summoner sits
*before* scoping it. Until then, the grid is Melee/Ranger/Mage × six elements, and
Summoner is a named future milestone, not a blocker.

---

---

# PHASE 4 — LOOT: RARITY, ENCHANTMENTS, RANDOM ROLLS

**This is the largest single system in the project — bigger than Summoner, bigger
than the packet listener.** It is Destiny 2 weapon rolls. It gets its own
dedicated planning pass when reached; what follows is the architectural shape so
earlier phases don't box it out.

Blocked on Phase 1 (weapons must exist to attach rolls to). Should land **after**
the class/element grid, so it attaches to weapons already proven fun.

### The architectural fact that must be respected from Phase 1 onward

Today a weapon is **fully defined by its content file** — `emberblade.yml` *is* the
Emberblade, completely. Random rolls break that. A rolled weapon has:

- a **definition** — the base: cast shapes, damage, element, and its **enchant
  pool**. Content, in a file. Shared by every Emberblade.
- an **instance** — *this dropped weapon*: which enchantments rolled, at what
  strength. **Per-item runtime state, stored in the `ItemStack`'s PDC.**

Two identical base weapons are not identical items — they rolled differently. This
is the same definition/instance split as `AbilityDefinition` vs a runtime cast, and
as `PlayerProfile` persisted state. The consequence: **weapons need per-item
persistent state**, and the item's PDC stores `weapon_id` (which base) **plus** a
serialized roll (which enchants, what strength). `/rpg give` grows from "mint a
tagged item" into "generate a weapon instance with a rolled enchant set."

**Phase 1 weapons must stay dumb precisely to protect this.** Ship the three-weapon
slice with **no rarity mechanics, no enchantments, no rolls** — fixed, file-defined
weapons. That proves the combat spine and the packet listener without entangling
the loot system. Rarity and element ride along as inert fields.

### What rarity does here

Rarity stops being cosmetic and becomes the **parameter of the roll**: it sets the
number of **enchant slots** and the **strength** of what rolls into them. Common
gets few weak slots; Exotic gets many strong ones. The rarity content type
(reserved in Phase 1) supplies slots + strength per tier.

### The one open question that scopes this entire phase

**Are enchantments stat-modifiers or new behaviors?**

- **Stat-modifier enchants** (+15% damage, −20% cooldown, +2 range) are a bounded
  numeric system: an enchant is a set of multipliers applied to a weapon's
  triggers; a roll is "which multipliers, how big." Tunable, expressible as data,
  a few weeks.
- **Behavior enchants** (add a burst on hit, chain lightning, apply a status) are
  effectively **attaching abilities to weapons** — an enchant becomes a little
  content program composing effects onto a trigger. Much larger; a sub-language.

Destiny 2 has both, which is why its system is deep and enormous. **This answer is
the difference between Phase 4 being a modifier system and a whole behavior
engine.** It is deliberately left open — it is milestones away — but it is *the*
scoping decision, and Phase 4's real planning pass starts by answering it.

## Build order, condensed

1. **Phase 1 weapons**, in three commits: free sword (with the packet listener,
   reviewed separately), free+costed sword, bow. Play each.
2. **Phase 2A**: delete `multiplierAgainst` — a subtraction commit.
3. **Phase 2B**: the six real elements, as content.
4. **Phase 3A**: generalize the gate to (class, element); answer "what makes the
   classes distinct" first.
5. **Phase 3B**: author **one** cell, play it, decide if the grid is worth filling.
6. **Summoner**: separate milestone, scoped once autonomy is decided.
7. **Phase 4 Loot** (rarity + enchantments + rolls): the project's largest system,
   its own planning pass, blocked on Phase 1 and best landed after the grid. Answer
   "stat-modifier vs behavior enchants" first.

## Where a second opinion earns its keep

Most of this is Tier-2 execution you and Claude Code run without review. The
exceptions, worth arguing before building:

- **The PacketEvents swing listener** (Phase 1) — first packet code, unforgiving
  threading, corrupts silently at scale.
- **"What makes Ranger ≠ Mage"** (Phase 3A) — a design fork that 18 kits depend on.
- **Summoner's autonomy level** (Phase 3C) — decides whether it's a feature or a
  milestone.
- **Stat-modifier vs behavior enchantments** (Phase 4) — decides whether the loot
  system is a numeric modifier or a whole behavior sub-language. The biggest scoping
  question in the project.

Everything else — weapon content, element deletion, the grid wiring — is the
sealed-interface, test-first, mutation-checked pattern already run a dozen times.

## Rules (unchanged)

- `./mvnw -pl core test` after every commit; `clean package` and a real boot after
  each phase.
- No Bukkit in `core`. If a fix wants it there, the fix is wrong.
- Confirm a mutation applied before believing its result.
- When you say something is verified, say what you executed.
- Deletion commits should reduce line count; if they grow it, something was
  reframed instead of removed.
