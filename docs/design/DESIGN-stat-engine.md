# DESIGN — Custom Stat Engine (max-health first)

The captured target for the stat system. This phase builds **custom max health** end
to end; attack damage and attack speed follow the same shape in later phases. It is the
foundation enchantments, passives, and the build system all stand on, so the *shape*
matters more than the speed.

---

## Why custom, not vanilla-backed (the decisive reason)

Vanilla `MAX_HEALTH` caps at 1024. The old project hit this wall: bosses needing more
than 1024 HP forced ugly workarounds. So **health cannot be vanilla-backed** — the RPG
can't be built under a ceiling vanilla imposes.

And once *health* is custom, damage must be custom too: if a boss has 5000 custom HP
but attacks deal vanilla attack-damage (a ~20-HP-world number), the math speaks two
languages. Custom health *forces* custom damage — they're one currency. So the whole
core-combat stat set (health, attack damage, attack speed) is custom, owned by us, free
of vanilla's caps and ranges.

**Cost we accept:** we become the source of truth for combat math and health display.
Vanilla was doing those for us; now we do. That's the price of escaping the caps, and
the old project proves it's the right trade.

---

## The stat model — `base + modifiers`, on the combatant

A stat is a named value on a **combatant** (player or mob), computed as:

```
value = base + Σ(modifiers)
```

- **base** — the combatant's intrinsic value (player base max HP = 100).
- **modifiers** — contributions from equipped items, later enchantments, passives,
  build pieces. Each modifier is `(source, amount)`; add on equip, remove on unequip.

This is **exactly the pattern already built and proven for Soaked/Rooted** — the
`MOVEMENT_SPEED` `AttributeModifier` add/remove lifecycle, with clean removal and no
leaks. The stat engine *generalizes that*: instead of one movement-speed modifier from
a status, it's a general "combatant stat = base + modifiers, sources add and remove
their modifiers." The Soaked cleanup discipline (modifier removed exactly once, base
exactly restored, no leak on death) carries over directly and is the load-bearing
correctness property here too.

**Stats live on the combatant; equipped items contribute modifiers.** A weapon doesn't
"have" 100 HP that the player borrows — the weapon, while equipped, *adds a max-HP
modifier* to the combatant's stat. Equip → modifier added; unequip → modifier removed.
The combatant owns the resolved stat.

Every future stat source is the same shape: an enchantment is a modifier source, a
passive is a modifier source, a build aspect is a modifier source. Build the modifier
system general now so those slot in as new sources, not new mechanisms.

---

## This phase: custom max health

### The stat

- Every combatant has a **custom max health** (`base + modifiers`). Player base = 100.
- Every combatant has a **custom current health** ≤ its custom max.
- Damage reduces current; healing raises it (capped at max). All combat health math is
  against these custom numbers, not vanilla health.

### Max-HP change semantics (the modifier edge case — state it, don't discover it)

When a modifier changes max HP, current HP must be handled explicitly (this is where
the Soaked cleanup lesson applies — the hard part is the transition, not the steady
state):

- **Max increases** (equip a +HP item): current HP is **unchanged** — you gain
  headroom, not health. 100/100 + (+300 max) → **100/400** (now at 25%, "hurt-looking"
  until you heal). This prevents equip = free heal.
- **Max decreases** (unequip): current HP is **clamped** to the new max. 400/400 −
  (−300 max) → **100/100**. Prevents current > max. If you were below the new max
  (e.g. 50/400 → unequip → 50/100), current is unchanged (still ≤ new max).

Stating this explicitly makes it a decision, not an emergent accident — and it's the
exact property a mutation test guards (equip/unequip and assert current tracks the
rule, doesn't free-heal, doesn't exceed max).

### Damage observability (the seam two things hook)

Health mutation is **observable**: applying damage or healing emits an event carrying
`(amount, target, source/dealer, newCurrent, max)`, where the dealer is identifiable as
a player if it was one (reuse `CombatantSnapshot.player`). This seam feeds:

- **now** — the health displays (nameplate + heart bar) refresh when health changes;
- **next phase** — the damage-number popup hooks the same event to render per-dealer
  floating numbers.

Building the seam now (even though only displays consume it this phase) means the
popup, a future combat log, aggro, etc. all attach without retrofitting observability.
If health mutated silently, every one of those would need the engine reopened.

---

## Displays (both driven by the observability seam)

### Mobs — LOS-gated text nameplate

Ported mechanism from the old project (`MobHealthBarManager`), extracted clean:

- The nameplate is the mob's display name: **`<name> <cur>/<max> ❤`**, the `❤`
  (U+2764) in red. Shows the **custom** numbers (so a boss reads `Boss 5000/5000 ❤` —
  the whole reason it's custom; vanilla can't show >1024).
- **Line-of-sight gated, per viewer.** A mob's nameplate is visible to a player only
  when that player has direct line of sight to it. Because different players have
  different sightlines to the same mob, this is **inherently per-viewer** — it can't be
  a plain all-or-nothing custom name. It's sent per-player via packets (PacketEvents —
  this is why PacketEvents is in the project): a per-viewer tick loop finds mobs in
  range (~64 blocks, the vanilla name-render cap), checks `hasLineOfSight` per viewer,
  and sends the `CustomNameVisible` metadata to that player accordingly.
- **Recurring loop.** LOS changes constantly (players move and look around), so this is
  a live per-tick-ish loop bounded by online players × mobs-in-radius. The old project
  runs exactly this and it scales acceptably; re-assert visibility each cycle so a
  metadata reset (teleport, chunk reload) self-corrects within a cycle.
- The **name text** rebuilds on health change (via the seam); the **visibility** is the
  per-viewer LOS loop. Two concerns: what the name says (health) and who can see it
  (LOS).

### Players — two-tier heart bar

The player's custom HP renders on the **vanilla heart bar**, via a custom two-tier
scale:

- **Heart count from MAX HP** (non-linear):
  - first 100 HP → 10 hearts (10 HP per heart),
  - every 100 HP above that → 1 more heart (100 HP per heart).
  - Formula: `max ≤ 100 → ceil(max/10)`; `max > 100 → 10 + ceil((max−100)/100)`.
  - 100 → 10 hearts; 400 → 13; 250 → 12; 1000 → 19.
- **Fill from PERCENTAGE** (`current/max`), independent of the tier scale:
  - filled hearts = `(current/max) × heartCount`.
  - 200/400 → 0.5 × 13 = **6.5 hearts** filled.
- **Rendered on the vanilla heart display.** Heart *counts* stay small (~10–20) even
  when custom HP is large, so they fit under vanilla's display range — the 1024 cap
  never bites here (it bites raw numbers, and hearts aren't raw numbers). Set the
  player's displayed vanilla `MAX_HEALTH` to `heartCount × 2` and displayed health to
  `filledHearts × 2` (vanilla renders in half-heart units). The tier math and fill are
  ours; the rendering rides vanilla's heart bar.
- Updates on **both** current-HP change (fill) and max-HP change (heart count) — both
  come through the seam.

> The 1024 cap is why *mobs* show a raw nameplate number (custom) but *players* can ride
> the vanilla heart bar (heart-counts are small). Two audiences, two displays, one
> custom-health source of truth.

---

## The TEMP modifier item (prove the modifier system)

A dev/test item — `health_boost_TEMP` — that grants **+max-HP while held/equipped**,
purely to prove the modifier lifecycle before real weapon stats exist. Same `_TEMP`
fixture spirit as `swing_TEMP` / `rooted_TEMP`: it exercises the mechanism and comes out
when real weapon stats arrive.

- Equip → adds a max-HP modifier (e.g. +300) → max 100 → 400 → heart count 10 → 13,
  current unchanged (100/400, per the semantics above).
- Unequip → removes the modifier → max 400 → 100 → current clamped (400/400 → 100/100).
- This is the first exercise of "equipped item contributes a stat modifier" — it proves
  the add-on-equip / remove-on-unequip / clean-removal lifecycle that all real weapon
  stats will use. Prove it here on the simplest case, exactly as Rooted proved the
  per-tick primitive before Soaked/Freeze inherited it.

---

## Scope

**In this phase:**
1. Custom max-health + current-health stat on combatants (`base + modifiers`, no vanilla
   cap), player base 100.
2. Max-HP change semantics (headroom on increase, clamp on decrease).
3. Damage/heal observability seam (event with amount/target/dealer, dealer-player-ID'd).
4. Mob nameplate: `<name> <cur>/<max> ❤`, LOS-gated per-viewer via packets.
5. Player heart bar: two-tier count + percentage fill on the vanilla heart display.
6. `health_boost_TEMP` item proving the equip/unequip modifier lifecycle.

**Explicitly OUT (later phases):**
- The damage *system* / basic attacks dealing custom damage — **next phase**.
- The damage-number popup — **next phase**, hooks the seam built here.
- Weapon *attack* stats (attack damage/speed) — the damage phase.
- Enchantments, rarity rolls — Phase 4 loot.
- Real weapon-stat sources — replace `health_boost_TEMP` when weapon stats are built.

---

## Load-bearing correctness (what the tests must guard)

- **Modifier cleanup** — equip/unequip leaves base exactly restored, no leaked
  modifier, current HP follows the headroom/clamp rules. (The Soaked lesson, re-applied:
  the transition is the hazard.)
- **No leak on death** — a mob that dies mid-modifier doesn't strand state (the primitive
  lesson). A player who logs out/in restores stats correctly.
- **Heart math** — the two-tier count and percentage fill compute correctly across the
  tier boundary (the `max=100` edge, fractional fill). Pure math → unit-testable.
- **Display ≠ truth** — the vanilla heart bar and the nameplate are *displays*; the
  custom stat is truth. A test must confirm damage reduces the *custom* health (and the
  display follows), not the other way around — so nothing reads vanilla health as the
  source.

The stat math (heart tiers, fill %, base+modifiers, max-change semantics) is pure and
core-testable. The nameplate packets and vanilla-heart rendering are Bukkit/paper and
boot-witnessed. Split accordingly: enforce the math in core, name the display behavior
for boot.
