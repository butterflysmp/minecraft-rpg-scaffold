# PLAN (design) — Attack-damage as a custom Stat (players + mobs), retiring the Pass-2 bridge

Read `CLAUDE.md` first. Branch off `master` (`f1f1c2e` — full damage+death arc merged). This is a
**design/scoping plan** for review; the execution-ready form (with the sub-decisions below settled)
comes next. Scope locked: attack-damage only — **attack-speed is deferred**.

## The goal, and why it's net-new

The arc's endgame is weapon-lore tooltips, which need *real stats to display*. Today a weapon's
damage is a **buried content literal** — `ironblade` left_click → `on_hit: damage amount: 8` — not a
stat. There is no `ATTACK_DAMAGE` anywhere; `Stat.java`'s javadoc reserves itself for exactly this
(*"attack damage and attack speed reuse it unchanged"*). Element is **identity, not math** (no
multiplier — `EffectApplier` applies the literal directly), so this pass is purely: promote the
literal into a first-class `ATTACK_DAMAGE` Stat that the basic hit reads and the tooltip can show.

This pass also retires the one remaining vanilla bridge: Pass 2's mob→player `event.getDamage()`.

## Scope this pass = MELEE attack-damage (symmetric with the current combat surface)

Keep it to the **basic melee hit**, both directions — which is exactly the surface that exists today:
- Player melee swing (`ironblade`/`emberblade` `left_click`) reads the player's `ATTACK_DAMAGE`.
- Mob melee → player (Pass 2's `onMobMeleeAttack`) reads the mob's `ATTACK_DAMAGE`, not `event.getDamage()`.

Deferred, on purpose (record in NEXT.md): **ranged/costed-special payloads** (bow shot, staff bolt,
emberblade special, Rekindle burst) keep their literal `amount:` this pass — they're distinct ability
effects, and whether a bow has "attack damage" is a later call. Mob **projectile**→player is already
deferred (Pass 2). So melee-only is the clean, symmetric slice.

## Recommended architecture (the execution plan should confirm/detail these)

**1. `ATTACK_DAMAGE` is a plain `Stat` — resolved value only, no `current`.** Unlike max-HP it has no
depletion; it's `base + Σ(modifiers)` read on demand. `Stat.java` supports it unchanged.

**2. Storage — one lifecycle, not a parallel store.** Extend the existing per-combatant state to carry
an attack-damage `Stat` alongside max-HP, so `register`/`clear`/`bootstrapIfAbsent`/reconcile and the
join/quit/**death/respawn** wiring we just built are reused once — rather than standing up a second
store that duplicates all of it. (A parallel store is the alternative; it's heavier on lifecycle. The
execution plan picks one — I recommend extend.)

**3. Player: base 0 + a main-hand modifier = the held weapon's declared attack-damage.** Unarmed is 0
(consistent with weapon-only melee — no weapon, no hit). The weapon declares a top-level
`attack_damage:` in content (new `WeaponDefinition` field, validated by `ContentValidator`). The
per-player reconcile loop — the same one that converges +HP modifiers — also converges an
`ATTACK_DAMAGE` modifier keyed by `MAIN_HAND` slot to the held weapon's value. Swap weapon → stat
follows, exactly like `HealthModifierItems` foretold.

**4. The basic melee hit reads the stat — single source of truth.** Replace the melee `left_click`
`on_hit: damage amount: 8` literal with a variant that means *"deal the caster's `ATTACK_DAMAGE`"* —
a new `EffectSpec` case (e.g. `WeaponDamage`) beside the existing literal `Damage`. Sealed hierarchy,
so it's exhaustive-checked. `EffectApplier` resolves the caster's `ATTACK_DAMAGE` from the store and
calls `applyDamage(statValue, casterId)`. **Wiring note:** `EffectApplier` currently only reads the
literal `d.amount()`; for `WeaponDamage` it needs a way to read the *caster's* attack stat — thread a
stat resolver in. Flag this in the execution plan; it's the one real new coupling.

**5. Mob: bootstrap `ATTACK_DAMAGE` from vanilla, then read the stat.** Mirror the HP bootstrap
(mob HP from vanilla `MAX_HEALTH`): a mob's attack-damage bootstraps from its vanilla attack-damage
attribute. `onMobMeleeAttack` reads the mob's `ATTACK_DAMAGE` stat instead of `event.getDamage()` —
**retiring the bridge.** Same number initially (proven by the *path* reading the store, not the
magnitude — same discipline as mob HP), but now a mob can be scaled past vanilla (the attack-side
mirror of the >1024 HP boss payoff).

## Invariants / carry-throughs

- **Weapon-only melee holds:** base 0 means unarmed deals nothing — no regression.
- **Death/respawn already resets HP; attack-damage rides the same lifecycle** if we extend the state
  (reason #2 to extend, not parallel). On respawn the reconcile loop re-derives the main-hand modifier.
- **Element stays identity** — this pass does not add a damage multiplier.
- **Popup/heart/nameplate/death seam unchanged** — the *amount* is now stat-sourced, but it still
  flows through `applyDamage → stats.damage → HealthChange`, so every downstream display is untouched.

## Testability

Good pure surface here, unlike the recent boot-only passes:
- `Stat` math is already unit-tested; the attack `Stat` reuses it.
- The reconcile of a `MAIN_HAND` attack modifier is pure `Map`-diffing — reddening-testable like the
  HP reconcile (watch a weapon swap change the desired map).
- Reading the stat for a basic hit (`WeaponDamage` → caster's `ATTACK_DAMAGE`) — the resolution is
  pure given a stat value; the Bukkit-coupled parts (equipment scan, mob bootstrap) are boot-witnessed.
- `core/` gets real new tests here (the stat + reconcile), not just paper.

## Boot verification — the gate

- **Player melee reads the stat:** ironblade swing drops the mob plate by the weapon's `attack_damage`;
  emberblade by its own — swap weapon, the number follows. Unarmed still does nothing.
- **Mob→player reads the mob stat:** a zombie hit drains the player's custom HP by the mob's
  attack-damage (same as before the bridge retired — proves the path, not a magnitude change).
- **Scale proof (optional):** set a mob's attack-damage stat > vanilla and confirm mob→player hits
  harder than vanilla would — the bridge is genuinely gone.
- **Lifecycle:** swap/drop the weapon → stat updates within a reconcile tick; die/respawn → stat
  re-derives; no leak (`trackedCount` stable).
- **Regression:** costed specials/bow/staff/Rekindle still deal their literal amounts; death, popup,
  nameplate, heart bar all unchanged.

## What this unblocks

Once attack-damage is a real, readable stat, the **weapon-lore tooltip** (the arc's endgame) can
display it — and `attack-speed` (deferred here) slots in as the next stat using the same machinery,
modifying the per-trigger `cooldown_ticks` fire-rate.

## Scope guard

Attack-damage, melee, players + mobs, bridge retired. If attack-speed, ranged/ability-payload scaling,
elemental multipliers, or weapon rarity-bonus modifiers are proposed, **decline** — each is its own
later pass. The execution plan's job is to settle the three flagged sub-decisions (extend-vs-parallel
store, the `WeaponDamage` effect variant + `EffectApplier` stat-resolver wiring, the mob bootstrap
source) and nothing more.
