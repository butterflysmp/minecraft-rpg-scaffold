# PLAN (design) — Weapon-lore tooltips (hybrid: auto stats + authored flavor)

Read `CLAUDE.md` first. Branch off `master` (`0d41146` — attack-damage stat merged). This is the
**endgame** the stat detour was for: the item tooltip finally shows the weapon's identity. It's a
display pass — no new mechanics, no seam, no threading. Lower-risk than the combat passes; the gate
is *does the tooltip read well and stay accurate*.

## Goal + the locked decision

`WeaponItems.mint` sets the name and PDC tag but **no lore today** — the tooltip is net-new. Locked:
**hybrid** — the stats/mechanics block is **auto-derived** from `WeaponDefinition` (so it can never
drift from the real numbers — the whole point of making stats real), plus an **authored flavor**
field for prose. Stats are truth; flavor is voice.

## What's renderable (all present now)

`rarity` (6-tier enum, `RarityColors.of` already maps tier→color for the name), `element`,
`attackDamage` (the real stat), and `triggers` — each an input bound to an `AbilityDefinition` with
`cast` (`CastSpec`: Self/Melee/Ray/Projectile/Dash), `cooldownTicks`, `cost` (`ResourceCost`), and
`onHit` (`EffectSpec`: Damage/WeaponDamage/Heal/Knockback/Status/Burst/Area/Visual/ThrowEmbers).

## The design

### 1. Content — the authored flavor field

- `WeaponDefinition` gains `List<String> flavor` (each entry a line; empty when absent — optional).
  Add a convenience constructor so existing `new WeaponDefinition(...)` test sites stay compiling.
- `WeaponLoader` reads `flavor:` — accept a YAML list of strings, or coerce a single scalar to a
  one-element list. `ContentValidator`: optional, so only a type check (list-of-strings); no
  presence requirement — a weapon with no flavor renders stats only.
- Author flavor on **ironblade + emberblade** as proof (a line or two each); other weapons can add
  it later. Render flavor only when present.

### 2. Rendering — a paper `WeaponLore` builder

`WeaponLore.build(WeaponDefinition) → List<Component>`, applied in `WeaponItems.mint` via
`meta.lore(WeaponLore.build(weapon))`. Adventure/`NamedTextColor` is paper-only, so the builder lives
in paper (like `HeartBarRenderer`/`NameplateText`). Proposed layout — **tune at boot**:

```
<Rarity> <Element> Weapon        ← rarity-colored header (RarityColors.of)
                                 ← blank
Attack Damage: 8                 ← only if attackDamage > 0
                                 ← blank
Left-Click  · Melee · 0.5s       ← one line per trigger (see effect summary)
Right-Click · 40 Energy · 3.0s
  Fireball — 12 fire dmg, Scorch
                                 ← blank
"<authored flavor>"              ← italic gray, bottom
```

- **Exhaustive switches, no default arm** over `CastSpec` and `EffectSpec` (both sealed) — a new cast
  or effect type is then a compile error at the lore site until described, the codebase's standing
  discipline. Summarize player-relevant effects (Damage: "N element dmg"; WeaponDamage: the weapon's
  `attackDamage`; Status: "Scorch (3s)"; Burst: recurse, prefix "AoE"; Knockback/Heal: short); **skip
  cosmetics** (Visual, and ThrowEmbers' trail) — they're not tooltip-worthy.
- Reuse `RarityColors.of(rarity)` for the header; element can be plain or lightly colored.

### 3. Decisions settled (flag the presentational ones for boot)

- **Flavor shape:** `List<String>`, optional — settled.
- **Effect depth:** player-relevant only (damage/status/cost/cooldown), cosmetics skipped — settled.
- **Attack-damage redundancy (tune at boot):** the melee trigger's `WeaponDamage` effect and the
  "Attack Damage: N" stat line are the *same* number. Recommend the stat line owns the number and the
  melee trigger line reads "Left-Click · Melee swing" *without* repeating it — but confirm which reads
  better at boot.
- **Layout/colors:** proposed above; the exact order, separators, and colors are a boot-tune, not a
  correctness property.

## Deferred (record in NEXT.md)

- **Attack-speed line** — not a stat yet (deferred pass). This pass can show a trigger's `cooldownTicks`
  as its cadence, but there's no "Attack Speed" stat to display until that pass lands.
- **Rarity/enchant stat bonuses on the tooltip** — Phase 4; rarity only colors + labels here.
- **Per-trigger authored descriptions** — flavor is weapon-level this pass; per-trigger prose is a
  later refinement if the auto trigger-lines read too dry.
- **Live tooltip refresh** — lore is applied at `mint` (give/kit-grant). A weapon already in an
  inventory keeps its old lore until re-minted; fine for now (re-give to refresh). No reactive refresh.

## Testability

Good pure surface — extract the formatters so they're reddening-tested, keep only the Component
styling/layout boot-witnessed:
- `cooldownTicks → "0.5s"` (÷20, one decimal) — pure, redden by breaking the divisor.
- `ResourceCost → "40 Energy" / "Free"` — pure.
- Effect → summary **string** (plain text: "12 fire dmg", "Scorch (3s)") — pure and exhaustive;
  redden by dropping a clause. The Component coloring wraps these strings in paper.
- The overall visual (layout, colors, wrapping, does it read well) is **boot-witnessed** — a tooltip's
  quality is a look-at-it property, like the nameplate.
- `core/` gets the pure summarizer tests if the summarizer lives in core (it can — plain strings, no
  Adventure); the paper builder is boot-only.

## Boot verification — the gate

- **Reads well:** hold ironblade/emberblade — the tooltip shows rarity+element header, "Attack Damage:
  8/7", the trigger lines, and the authored flavor, in a clean readable order (no overflow, no ugly
  wrapping, colors sane in the vanilla tooltip).
- **Accurate + can't drift:** the numbers match content exactly (attack 8/7, emberblade special 40
  energy / 3s / 12 dmg + scorch). Change `attack_damage` in yml, `--refresh-content`, re-give → the
  tooltip follows with no lore edit (proves auto, not authored).
- **All shapes render:** sword (ironblade/emberblade), bow (`hunters_bow`, attackDamage 0 → no stat
  line, its shot shown as the trigger's literal damage), staff (`ember_staff`), ability_stone —
  each renders sane lore, none crash the give.
- **Flavor:** authored flavor renders italic/gray at the bottom; a weapon with no flavor shows stats
  only (no empty section).
- **No regression:** name color, PDC tag, vanilla-melee suppression all unchanged (lore is additive).

## Scope guard

Auto stats + authored flavor, applied at mint. If attack-speed lines, enchant/rarity stat bonuses,
per-trigger authored prose, or reactive lore refresh surface mid-build, **decline** — each is its own
later refinement. This pass makes the tooltip real and non-drifting; polish iterates at boot.
