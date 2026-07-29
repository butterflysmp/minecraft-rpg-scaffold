# PLAN — fix the `/rpg mobdamage` nameplate "every other cast" bug

Read `CLAUDE.md` first. Branch: `feat/damage-pipeline-1a`. This is a one-line
behavioral fix plus a reddening test and a comment-honesty cleanup — resist
expanding scope.

## Diagnosis (CONFIRMED by code-read, not a guess)

The root cause is `onMobAppear` being a **full replace**, not register-if-absent,
combined with the fix commit `af1df09` calling it **once per cast** and describing
it as "idempotent" in two places. It is not idempotent with respect to the plate.

- `MobNameplateManager.java:91` — `onMobAppear` ends with
  `nameplates.put(id, new Nameplate(...))`, constructing a fresh `Nameplate` whose
  version starts at **1**. Unconditional. (`stats.bootstrapIfAbsent`, the *store*
  half, IS idempotent; the `nameplates.put`, the *plate* half, is NOT. The commit
  conflated the two under one word.)
- `RpgCommand.java:239` — the new `mobMutate` calls `nameplates.onMobAppear(target)`
  on **every** cast (region thread), so the plate version is reset to 1 every cast.
- `BukkitCombatant.applyDamage` — does `ctx.scheduler().onEntity(entity, …)`: it
  **hops to the entity thread and defers**, where `stats.damage → onChange →
  Nameplate.update` bumps the version 1→2.
- `ViewerNameplateState.java:37` — `decide()` gates resends on
  `lastSent != version` and its own docstring states the invariant it depends on:
  *versions only ever increase, on a health change.* The per-cast reset breaks that
  invariant. Whether a cast's update reaches the client depends on whether the
  viewer's 4-tick LOS loop samples the intermediate `version == 1` between the
  region-thread reset and the deferred entity-thread bump → timing-dependent
  "every other cast, and only with time between casts."
- Secondary: a **lost-update race** — `onChange` (entity thread) can read the old
  `Nameplate` reference just as `onMobAppear` (region thread) swaps a new one in;
  that `update()` lands on the discarded object.

**Why real combat is immune (the clincher):** `onMobAppear` has exactly two callers
— `RpgListeners.java:99` (once, on `EntityAddToWorldEvent`) and `RpgCommand.java:239`
(every dev cast). Weapon/ability hits never re-appear the mob, so its version climbs
monotonically and `decide()` works. Only the dev command re-appears per hit. This is
why ironblade/Ember Staff are correct and only `/rpg mobdamage` misbehaves.

## Step 0 — Evidence gate: instrument, don't guess (this bug already burned two wrong guesses)

Do NOT apply the fix until the live log confirms the mechanism.

- Temporary logs (thread name on every line):
  - `onMobAppear`: id, CREATED-vs-REPLACED (did an entry already exist?), resulting
    version, `Thread.currentThread().getName()`.
  - `Nameplate.update` / `onChange`: id, old→new version, thread.
  - viewer send (`tickViewer`): id, `snapshot.version()`, `lastSent`, `includeName`,
    thread.
- Reproduce: cast `/rpg mobdamage <n>` a few times with pauses, then rapidly. Read
  the sequence.
- CONFIRM the prediction: `onMobAppear` logs **REPLACED, version reset to 1** on every
  cast; on a "missed" cast the viewer never observes version 1 (only the final 2), so
  `lastSent` already equals the version → `includeName=false`.
- If the logs contradict this, **STOP and report** — the diagnosis is wrong and the
  fix below would be built on sand.

## Step 1 — The fix: register-if-absent (restore monotonicity)

In `MobNameplateManager.onMobAppear`, leave an already-present mob's plate untouched.
Replace the unconditional put:

```java
// was: nameplates.put(id, new Nameplate(baseName, NameplateText.of(baseName, state.current(), state.max())));
nameplates.computeIfAbsent(id, k ->
        new Nameplate(baseName, NameplateText.of(baseName, state.current(), state.max())));
```

This makes the plate half match the store half (`bootstrapIfAbsent`). It is the whole
behavioral fix, correct on BOTH callers:
- Dev command: version no longer resets per cast → monotonic → `decide()` fires every
  cast; the lost-update race disappears (no swap on a present mob).
- Spawn path: a chunk reload no longer resets version. `onMobRemove` still clears both
  store and plate on unload, so a genuine re-appear re-creates fresh (absent → create).

## Step 2 — Reddening test (a check that never ran looks exactly like one that passed)

Load-bearing property to guard: **`onMobAppear` on an already-registered mob preserves
its version (does not replace the plate).**

`onMobAppear` takes a `LivingEntity` (painful to fake), so extract the pure decision
into a Bukkit-free, package-private seam — matching the core/paper split:

- e.g. `long registerIfAbsent(Map<UUID, Nameplate> plates, UUID id, Component baseName,
  double current, double max)` that does the `computeIfAbsent` and returns the resulting
  version. `onMobAppear` reads the entity (Bukkit) then delegates.
- Make `Nameplate` package-private (or add a version accessor) so the test can read it.

Test (paper test dir, beside `ViewerNameplateStateTest`): call `registerIfAbsent` twice
for the same id with DIFFERENT `current` values; assert the second call returns the SAME
version AND the cached text is unchanged (proves no replace).

**Watch it redden:** temporarily restore the old `put`-based body, run the test, SEE IT
FAIL (version changes / text replaced), then restore `computeIfAbsent`, SEE IT PASS. Use a
scratch copy, never `git checkout` live work. Only after it reddens is the test guarding
anything.

Optional regression guard: a `decide()` test asserting that a monotonic version sequence
always resends — pins the invariant `decide()` depends on.

## Step 3 — Comment honesty + cleanup (a comment that lies is worse than none)

Now that `onMobAppear` IS register-if-absent, "idempotent" is finally true — state it as
a property of the behavior, not asserted over a replace:
- Fix the javadoc at `RpgCommand.java:213` and the `onMobAppear` javadoc.
- (Can't edit `af1df09`'s message; just don't repeat the claim in new commits.)

Remove the Step 0 instrumentation (deletion — line count down).

## Step 4 — Boot verification (green tests are not the gate; "every-other" is a boot-only property)

- HARD case, not the easy one behind it: cast `/rpg mobdamage` **rapidly** (the scrambling
  case), not just slowly. Every cast must drop the plate by exactly its amount, in order,
  no skips, rapid or slow.
- Confirm no regression to spawn-time nameplating from the shared `onMobAppear` change:
  fresh-spawned mobs AND chunk-reloaded mobs still get a plate.
- Confirm real combat still exact: ironblade/emberblade drop the plate by exactly their
  damage; Ember Staff updates + aggros.
- The open "deep fix vs. just make it deterministic" question is now moot — the deep fix
  and the shallow fix are the same one-liner. Take the real fix.

## Step 5 — Secondary items (address or explicitly defer; don't silently drop)

- **Stray hurt SOUND:** on this branch `applyDamage` no longer calls vanilla
  `entity.damage()` — it's `stats.damage(...)` + `playHurtAnimation(0f)` (flash only). The
  vanilla path that plays the hurt sound is gone. Re-check whether the stray first-cast
  sound still reproduces. If it does, it's coming from another path (weapon swing / packet)
  — instrument that before chasing. If it doesn't, close it as fixed by the `applyDamage`
  rewrite.
- **`displayCurrent` in `mobMutate`:** it's a region-thread approximation
  (`stats.current(id) - amount`) computed before the entity-thread `applyDamage` runs. Fine
  for a dev command; add a one-line comment that it's an estimate and the plate is the
  truth, so no future "why do these disagree" hunt.
- **Record in NEXT.md:** with register-if-absent, renaming a mob while it's tracked won't
  refresh the plate's `baseName` until it's removed/re-added. Consistent with "custom truth
  drives display," not a regression. One line.

## Commit shape

Prefer one commit: fix + reddening test + comment cleanup + instrumentation removal (net
small, likely negative line count). Keep the Step 0 instrumentation OUT of committed history
(add/use/delete within this pass). NEXT.md note can fold in or be its own line.

## Scope guard ("press n" if asked)

If a broader nameplate/versioning refactor is proposed, decline: the fix is one line
(`put` → `computeIfAbsent`) plus a test and honest comments. Don't expand it.
