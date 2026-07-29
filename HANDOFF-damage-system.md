# HANDOFF — Minecraft RPG plugin, damage-system phase (for a fresh chat)

You (the assistant in the new chat) are a **design/architecture reviewer** for a
Minecraft RPG plugin, working with the user who relays plans and boot results from
**Claude Code** (which does the actual building). Your job: review plans, catch
tier-3 bugs (threading/lifecycle/silent-failure) and design forks, draft "press n"
responses to Claude Code decision dialogs, and — importantly — reason from **what's
actually on master** (clone and check; don't trust memory). The user boots every
change on a live server before merge; "green tests are not the gate" for boot-only
properties (visual correctness, thread identity, feel).

## Repo & environment
- Repo: `github.com/butterflysmp/minecraft-rpg-scaffold`. Master HEAD at handoff:
  `543e597` (custom health stat engine).
- User on Windows, IntelliJ, **runs scripts from Git Bash** (IntelliJ terminal set to
  Git Bash). Dev server: `./scripts/dev-server.sh --refresh-content` (the
  `--refresh-content` flag clears deployed content so yml/source edits propagate).
- Three Maven modules: `core` (pure Java, ZERO Bukkit — invariant, unit-tested),
  `storage`, `paper`. Package `io.github.butterflysmp.rpg`. Paper api-version 26.1.
- Sealed hierarchies (EffectSpec, CastSpec, StatusDefinition, etc.) give compiler-
  enforced exhaustiveness — no `default` arms. Content is data-driven (yml in
  `paper/src/main/resources/content/`), validated by `ContentValidator`.
- **Reference implementation:** this is a ground-up redesign of an OLD project,
  `CreaperCrusher/Butterfly-SMP` (default branch `refactor/god-class`, package
  `org.example1.butterflysmp`). The old code is spaghetti/hardcoded — **extract
  MECHANISMS (which Bukkit calls, what order) as facts, never port the STRUCTURE.**
  Treat it like documentation. Cloned to `/tmp/bsmp` in the working env.

## Recurring discipline (from CLAUDE.md, hard-won)
- "A check that never ran looks exactly like a check that passed." Verify a mutation
  reddens a test (watch it fail, then revert) before believing the test guards
  anything. Use scratchpad copies, never `git checkout` live work.
- **Boot observations test the HARD case**, not the easy one hiding behind it.
- **"A thing jittering/creeping is being CORRECTED or PREVENTED-FROM-RESTING, not
  moving on its own — remove the correction/blocker, don't strengthen it."** (Emerged
  4+ times: immobilize strafe, item-marker jitter.)
- Named tuning constants, not magic numbers. Deletion commits reduce line count.
- **Display ≠ truth**: custom values are the source of truth; vanilla fields (health,
  name) are puppet display layers. Nothing reads vanilla back as truth.
- General mechanism / specific content split: build the engine general, filter/flavor
  specific (e.g. `Immobilize` mechanic vs `Rooted`/`Freeze` configs).
- **When a bug resists 2-3 rounds of symptom-diagnosis, STOP theorizing and
  INSTRUMENT it** (log with thread names + order, read the truth). The assistant was
  wrong twice on the current bug by pattern-guessing.

## DONE & on master (the arc so far)
- **Status tier** (all merged, boot-verified): Scorched (DoT), Rooted (immobilize —
  MOVEMENT_SPEED×0 + velocity-zero + EntityMoveEvent veto for strafers), Soaked
  (stacking slow via speed modifier), Freeze (immobilize + attack-suppression). Built
  on a per-tick scheduler primitive (RepeatingTask/RepeatingTaskTarget, lifecycle-as-
  acceptance, leak-proof cancel-on-death). `/rpg apply <status>` dev command.
- **Two signature abilities**: Ember Step (Fire Mage — velocity-impulse dash, WASD
  direction via getCurrentInput(), lift to clear ground friction, swept-line hit) and
  Rekindle (Fire Ranger — reverse-facing dash + 3 thrown embers in a ±40° fan that
  detonate on a fuse). The `Dash` CastSpec + shared `ProjectileFlight` launcher (used
  by both the Projectile cast and Rekindle) + the **Ability Stone** (a weapon whose
  click casts the ability under test — the only way to test WASD-relative casts, since
  a chat command releases movement input). **The class axis was CONFIRMED**: Fire
  Ranger and Fire Mage now feel like different classes (kite vs commit).
- **Custom health stat engine (stat engine phases 1-2)** — the current foundation:
  - **Custom stats = `base + Σ(modifiers)` on a combatant** (generalizes the Soaked
    speed-modifier lifecycle). Custom because vanilla MAX_HEALTH caps at 1024 and the
    user needs bosses >1024. Player base = 100.
  - `CombatantStats` store (ConcurrentHashMap<UUID>), emits `HealthChange` events (the
    **observability seam** — carries amount/target/dealer/dealerIsPlayer/newCurrent/max
    and now **`reachedZero`**, the death hook, built-unconsumed).
  - **Modifier reconcile loop** (per-player tick loop, diffs desired-vs-applied by
    equipment SLOT) — structurally leak-proof across drop/die/clear/swap (chosen over
    event-driven precisely so no removal path can leak). `health_boost_TEMP` item
    proves the equip/unequip lifecycle. Max-change semantics: **increase = headroom
    (no free heal), decrease = clamp current to new max.**
  - **Player heart bar**: two-tier scale (first 100 HP = 10 hearts @ 10 HP each; every
    100 above = 1 heart @ 100 HP each), fill = (current/max) × heartCount, rendered on
    the vanilla heart bar (heart-counts stay small so the 1024 cap never bites). Floors
    at half a heart (scaffold for absent death — a display write must not kill a live
    player).
  - **Mob nameplate**: `<name> <cur>/<max> ❤` (red heart), LOS-gated **per-viewer via
    packets** (name index 2 / visible index 3, verified against MC wiki 26.2 — NOT the
    old project's stale constant). Fully per-viewer — the mob's REAL name is never
    touched (no death-message leak). Text resent on first-sight-or-version-change;
    visibility every cycle. Bootstraps mob custom HP from vanilla MAX_HEALTH at spawn
    (so >1024 payoff is deferred to a mob-scaling phase; the mechanism is proven by the
    PATH reading custom store, not yet the magnitude).
  - **Known deferred limitation:** mobs loaded at plugin-enable aren't nameplated until
    a chunk reload (new spawns + chunk loads ARE covered).

## THE DAMAGE SYSTEM (current work — stat engine phase 3)
Design doc written: **`DESIGN-damage-system.md`** (also at
`/mnt/user-data/outputs/DESIGN-damage-system.md`). Core decisions:

- **Ride-the-event pattern** (the crux): don't replace vanilla combat damage — vanilla
  damage is a BUNDLE of cosmetic reaction (red flash, hurt sound, i-frames) + mechanics
  (amount, knockback). Keep the cosmetics, own the mechanics. Let the vanilla
  `EntityDamageByEntityEvent` FIRE (token its damage to ~0.01 → keeps flash/i-frames,
  can't double-damage or kill), CANCEL vanilla knockback, apply real custom damage +
  custom knockback separately. Death fires from CUSTOM HP, never vanilla.
- **One damage path**: a single `applyDamage(target, amount, source, knockback?)` that
  both basic attacks AND ability payloads (Ember Step, Rekindle) route through — drains
  custom HP, fires the seam, clamps at 0 (does NOT kill — death deferred, but
  `reachedZero` fires for the future death phase to hook).
- **Knockback opt-in per weapon** (default NONE — >25% of weapons, esp. Mage, have no
  KB; you don't write `knockback: none`, you ADD it when wanted). Vanilla KB always
  cancelled.
- **Death deferred** to a later pass (this pass clamps at 0, entity sits at the half-
  heart floor alive — documented phase boundary; `reachedZero` is the built-unconsumed
  hook).
- **Phasing decided** (Claude Code caught that player→mob and mob→player aren't
  symmetric — only mob→player needs the vanilla-event RIDER):
  - **Pass 1a (BUILT, on branch `feat/damage-pipeline-1a`, commits 4313020 / 9a1ab05 /
    af1df09):** the pipeline, player→mob only. `applyDamage`, reachedZero, knockback
    opt-in, token-can't-kill, ability aggro (setTarget the source), MELEE flash via the
    tokened vanilla event / ABILITY flash manual (playHurtAnimation, gated on
    getNoDamageTicks()==0 so it never double-flashes melee). **The mob nameplate is the
    witness — no popup needed for 1a.** Weapon-only melee decided (unarmed does nothing;
    kits always grant weapons; reversible later if weaponless states matter).
  - **Pass 1b (NOT STARTED):** the damage-number popup — the single largest chunk, all
    net-new PacketEvents surface. Split OUT of 1a so packet-protocol risk is isolated
    from damage-math risk.
  - **Later pass:** mob→player (the vanilla-event rider) + the **i-frame feel fork**
    (can mobs hit every tick, or preserve vanilla invulnerability windows).
- **Deferred/recorded gaps** (in NEXT.md): Scorch DoT bypasses custom HP (vanilla fire);
  `applyHeal` uses vanilla setHealth (ability heals bypass custom HP too — same class,
  heal side). Both "predates the pipeline, wire in later."

## THE CURRENT BUG (unresolved — where the fresh chat picks up)
`/rpg mobdamage <n>` (dev command, now routed through `applyDamage`) updates the mob
nameplate **only on "every other cast," AND only if enough time passes between casts**
(rapid casts scramble the pattern). Real combat works fine (ironblade drops the plate
by exactly its damage; Ember Staff updates + aggros). So this is likely a **DEV-COMMAND
artifact, not a combat bug.**

Diagnosis history (assistant was WRONG twice — a caution):
1. First guess "command bypasses the seam" — WRONG. Claude Code read the code:
   `mobMutate`→`stats.damage()` DID fire the seam. Real cause of the *original* no-update
   was a **map-gap**: `MobNameplateManager.onChange` no-ops if the mob isn't in the
   nameplates map (store and nameplate populated on separate paths). Fixed by calling
   `nameplates.onMobAppear(target)` (idempotent) before mutating.
2. That fix introduced the "every other cast" behavior. Guessed "version-parity toggle
   from onMobAppear double-touching state" — but then the user found it's **timing-
   dependent**, which rules out pure parity.
3. Current best theory (UNCONFIRMED): `applyDamage` **hops to the entity thread**, so
   each cast does work across the command thread (onMobAppear/viewer-state) AND the
   entity-region thread (applyDamage→onChange→nameplate). With time between casts they
   settle deterministically-but-wrong (every-other); rapid casts interleave and
   scramble. A THREADING/ORDERING bug from the thread-hop interacting with the per-cast
   onMobAppear touch.

**NEXT ACTION: STOP pattern-guessing. INSTRUMENT it** — log every onMobAppear, onChange,
version bump, and nameplate packet send with THREAD NAME + timestamp/cast-id, cast a few
times with pauses, read the actual sequence. Likely fix direction: `onMobAppear` should
be **register-if-ABSENT (true no-op if present)**, not called unconditionally before
every mutate (the per-cast re-touch is the suspect); and/or make the dev command apply
**synchronously on the right thread**. Also confirmed worth asking: **is this even worth
a deep fix, or just make the command deterministic?** — real combat is unaffected.

There is also an unexpected **hit/damage SOUND** on the command's first cast (the vanilla
hurt sound firing through the real path) — decide if the dev command should play it.

## OLD-PROJECT DAMAGE MECHANISMS (extracted for pass 1b + the i-frame fork)
Read from `/tmp/bsmp` — `combat/CombatListener.java` and
`combat/DamageIndicatorService.java`. **Extract mechanism, not structure.**

- **Damage-number popup (`DamageIndicatorService`) — the pass-1b blueprint:** floating
  numbers are **fake client-side-only `TEXT_DISPLAY` entities sent via packets to ONE
  viewer** — a spawn packet + a metadata packet, sent only to the dealer, then a
  `WrapperPlayServerDestroyEntities` after ~0.75s (`runTaskLater`). Never a real entity.
  Metadata indices **verified against the MC wiki and commented** (`META_TEXT = 23`,
  `META_STYLE = 27` for TextDisplay on their version — RE-VERIFY against 26.1/26.2, same
  discipline as the nameplate). Crit = yellow `§e`, normal = white `§f`. This is exactly
  the per-dealer isolation pass 1b wants — port the mechanism into the clean structure.
- **Ride-the-event (`CombatListener`):** uses event PRIORITIES — `HIGHEST`
  (ignoreCancelled) to adjust damage via a single `event.setDamage(damage)` call, then
  `MONITOR` to spawn the indicator after all math settles. (Your design tokens + applies
  separately instead, which is fine — but note the old project modifies the event's
  damage directly rather than tokening.)
- **i-frames ARE a real factor** — the old project has explicit **i-frame-BYPASS paths**
  ("Dragon's Plume i-frame bypass," "Bonus Shot i-frame bypass") where the indicator is
  spawned MANUALLY because the damage bypassed the normal event. This **confirms i-frame
  interaction is a real, known problem in this style of system** — strong support that
  the current `/rpg mobdamage` bug is i-frame/thread related, and that the i-frame feel
  fork (later pass) will need explicit bypass handling for abilities that should hit
  through i-frames.
- Other combat pieces exist to mine later: `KnockbackRegistry`, `CritRoll`, `FireDot`,
  `DamageReductionListener`, `DamageMathCharacterizationTest`.

## IMMEDIATE NEXT STEPS (for the fresh chat)
1. **Resolve the `/rpg mobdamage` bug by INSTRUMENTING** (not guessing) — logs with
   thread + order; likely `onMobAppear` register-if-absent + synchronous command
   application. Confirm real combat is unaffected (it appears to be) and decide if it's
   worth a deep fix vs. just making the command deterministic. Address the stray hurt
   sound.
2. **Finish pass 1a boot verification**: ironblade drops the plate by EXACTLY its damage
   (no double-damage) and flashes ONCE (gate held) or TWICE (switch to the per-tick
   vanilla-flashed marker fallback Claude Code has ready). Then merge 1a.
3. **Pass 1b — the damage popup**, porting `DamageIndicatorService`'s mechanism (fake
   client-side TEXT_DISPLAY, packets to the dealer only, spawn+metadata+destroy, indices
   re-verified for 26.1) into the clean structure, hooking the `HealthChange` seam 1a
   fires.
4. **Later**: mob→player pass (vanilla-event rider + i-frame feel fork), then DEATH
   (consumes `reachedZero`), then attack-damage/attack-speed as custom stats, then mob
   HP scaling (the >1024 payoff), then the weapon-lore rendering that STARTED this whole
   stat detour (stats + ability descriptions + rarity on tooltips — needs real stats to
   display).

## Behavioral notes for the reviewer
- Draft "press n" responses to Claude Code decision dialogs; review plans for the
  core/paper split (testable math in core, Bukkit/feel in paper-boot), reddening
  mutations on load-bearing properties, and general-vs-specific.
- The user's own instincts are often right and worth backing (weapon-only melee, "I've
  seen it work" → read the old project).
- When wrong, own it and re-diagnose from evidence — the assistant was wrong twice on the
  current bug; the lesson (INSTRUMENT don't guess) is now explicit.
- Every status/stat/damage change is HUMAN-BOOTED before merge. Pass work on branches;
  the user pushes/merges.
