# PLAN — the action bar: a health + mana HUD

Branched off `origin/master` at `6c4b8f3` (PR #22, the resource rename), verified from origin rather
than assumed — `git fetch` then `git log --oneline -1 origin/master` printed `6c4b8f3`, tree clean.

Closes the unbuilt half of `PROJECT.md:31` — *"Damage numbers, cooldown UI, resource bar, hit
feedback."* Damage numbers and hit feedback shipped in the popup/nameplate passes; this is the
resource bar. Cooldown UI remains.

## What this pass is

A per-player action bar showing custom HP and mana, redrawn every 10 ticks so it never fades.

**Display only.** No regen, no clamping, no writes of any kind: mana regenerates lazily inside
`ResourcePool` on read, health is owned by `PlayerHealthSystem`. This pass reads two stores and
draws. No override either — `grep sendActionBar|ActionBar` returned **zero** hits repo-wide before
this pass, so the stats bar owns the action bar outright and competes with nothing.

**There was no old action bar to port.** Greps across the working tree *and* all 198 commits
(`git log --all -S`) for `sendActionBar`, `ActionBar`, `✦`, `⛨`, `LegacyComponentSerializer`,
`ChatColor` returned nothing. The `❤ ⛨ ✦` layout lives in the *old project* — a separate codebase
this repo cites (`DESIGN-stat-engine.md:12,105`, `PLAN-1b-damage-popup.md:98`) but does not contain.
This is a fresh design matching the remembered feel. The only `❤` here was the mob nameplate, and
the HUD deliberately reuses its glyph and colour.

## Do not touch — the invariants

1. **`StatsBarText` lives in `paper/`, not `core/`, and that is not a compromise.** `core/pom.xml`
   has no `<dependencies>` element at all, so `Component` cannot compile there. The property that
   mattered — pure, side-effect-free, unit-tested in the fast loop — is fully kept, and the repo had
   already resolved this exact tension twice in writing: `DamageNumberText` (*"Lives in paper (not
   core) because it depends on Adventure, which core does not carry"*) and `RarityColors`
   (*"Adventure's NamedTextColor cannot live in core"*). Do not "fix" this by moving it to core; it
   will not compile. Do not fix it by adding Adventure to core either — that pom is empty on
   purpose.

2. **Nothing was added to `Scheduler`.** The repeating primitive already existed:
   `RepeatingTask` + `RepeatingTaskTarget` + `TaskHandle`, with `EntityTaskTarget` binding a real
   entity. It re-arms per period through `onEntityLater` on the entity's own thread, which is what
   makes it Folia-correct. `PlayerHealthSystem`, `MobNameplateManager`, `SoakedStatus` and
   `ImmobilizeStatus` all run on it. Adding a parallel `runAtFixedRate` path would mean two
   repeating mechanisms with different cancel semantics; the only thing it buys is drift-free
   cadence, which is imperceptible on a 0.5s cosmetic refresh against a ~3s fade.

3. **Defense is absent, not zeroed.** No defense stat exists to read
   (`DESIGN-damage-system.md:197-198` defers resistances). A placeholder field reading 0 would be
   the exact failure `NEXT.md:1856` names: *"A readout reporting 0% when nothing is measured is
   indistinguishable from a working readout that measured zero."* Pass 3 inserts `⛨` **between**
   health and mana and will shift the child indices the tests pin — that reddening is the signal,
   not a nuisance.

4. **`ResourceCost.DEFAULT_RESOURCE` is one constant read by two sites** —
   `AbilitySchema.parseCost`'s default and `StatsBarSystem`'s pool lookup. Do not re-inline `"mana"`
   at either. The failure mode is silent: nothing validates a resource id at load, and
   `ResourcePool.current` returns the pool's max for an id it has never seen, so a display reading a
   different id than the abilities spend would show a **full, never-moving bar** rather than
   throwing or warning.

## The two guards that are easy to get wrong

**`stats.tracks(id)` before every read.** `CombatantStats.current`/`max` **throw**
`IllegalStateException` for an untracked id — they do not return 0. An unguarded read would throw
every 10 ticks rather than merely display something wrong. Health registers synchronously in
`PlayerHealthSystem.onJoin`, so this is the edge case, not the norm — which is exactly why it would
survive casual testing and fail on someone else's timing.

**`onRespawn` must restart the loop.** `EntityTaskTarget.isActive()` is
`entity.isValid() && !entity.isDead()`, so the loop **self-cancels the moment the player dies** and
never returns without an explicit restart. `PlayerHealthSystem:106-113` and `RpgListeners:346-351`
document this trap for the two loops that hit it before; the HUD is the third. It also means the
death-screen window needs no separate guard: between death and respawn `stats.tracks(id)` is still
true and `current(id)` reads `0.0` (*"onQuit does not run on death, so custom HP sits at 0 until
onRespawn"*), so an unguarded loop would render `0/400` — the target prevents it first.

Unlike the health and nameplate loops, which discard their handles and re-`start` unconditionally,
this one keeps the handle and guards on `isRunning()` (the `ImmobilizeStatus`/`SoakedStatus`
precedent). Two live loops on one player would be two sends racing on one action bar, with no way to
cancel either.

## What was executed, and what it printed

| check | result |
|---|---|
| `./mvnw clean package` | core 371 + paper 263 (8 new), 0 failures, `BUILD SUCCESS` |
| focused run | `Tests run: 8` naming `StatsBarTextTest` explicitly — not a bare `BUILD SUCCESS` |
| boot, `--refresh-content` | `Done (6.471s)!`, `[Rpg] Enabling`, `Loaded 6 abilities, … 5 weapons, 1 mobs`, no exceptions |

Note on the focused run: the flag is **`-Dsurefire.failIfNoSpecifiedTests=false`**, not
`-DfailIfNoSpecifiedTests`. The short form is silently ignored and the run aborts in `rpg-core`,
which has no matching test.

### The reddening checks

Three mutations, each applied with a grepped marker, `test-compile`d first to prove it compiled, and
restored from a scratchpad copy (`cp`, never `git checkout --`) with `md5sum` confirmed back to
`f78cbe2234dcddac5ec0150001e6b556`.

| mutation | result |
|---|---|
| swap the two fields | **7 of 8 fail**, both order tests included |
| render mana in `HEALTH_COLOR` | **exactly 1 fails** — `and the whole field is blue ==> expected: <blue> but was: <red>` |
| format the raw double | **7 of 8 fail** — `expected: <❤ 9/10> but was: <❤ 9.4/10.0>` |

The middle one is the informative one: content assertions all passed while only the colour assertion
failed, so colour is guarded independently of text rather than incidentally. And in the swap, the
one survivor is `theTwoFieldsAreSeparatedBySpacing` — correct, since swapping the fields does not
move the gap.

Every expected string was produced by **executing** `Math.round`, not by reasoning about it
(`9.4→9`, `0.6→1`, `99.5→100` half-up, `73.5→74`), and each test asserts the whole rendered field
rather than a bare number, because a rounding error lands in one field.

## The in-game gate — RUN AND PASSED

Run on a real server against the merged build, on `./scripts/dev-server.sh --refresh-content`. This
closes the one gap the pass shipped with: every check below was previously listed as outstanding
because no Minecraft client was available to the build, and all five now pass.

| # | check | result |
|---|---|---|
| 1 | join → the bar renders `❤ 100/100    ✦ 100/100`, red then blue, and **does not fade** | pass |
| 2 | costed cast (`/rpg cast solar_grenade`, 40 mana) → the mana field drops and climbs back as it regenerates | pass |
| 3 | take damage → the health field moves | pass |
| 4 | **die → respawn → the bar comes back** | pass |
| 5 | quit → rejoin → exactly one bar, not two overlapping refreshes | pass |

Row 4 is the one that mattered most, and it is the only evidence that will ever exist for it.
`EntityTaskTarget.isActive()` is `entity.isValid() && !entity.isDead()`, so the loop self-cancels the
instant the player dies; a missing `onRespawn` wiring is invisible to every unit test and to the boot
check, and shows up only as a bar that never returns for the rest of the session. It returned.

Row 5 is the other check no test reaches: it witnesses the `isRunning()` guard in
`StatsBarSystem.start` doing its job. Two live loops would be two sends racing on one action bar,
with the older handle unreachable — a single bar on rejoin is what says the handle was kept and
honoured.

Rows 1 and 2 together are what make the readout real rather than decorative: row 1 proves the
10-tick period beats the fade, and row 2 proves the mana field reads the same pool abilities spend
from — the phantom-pool failure that `ResourceCost.DEFAULT_RESOURCE` exists to prevent would have
shown here as a bar frozen at full while the cast still succeeded.

## Scope guard

Declined, each its own later call: any regen; a defense field (pass 3, and it needs the stat first);
a soul resource; cooldown UI (the other unbuilt half of the milestone line); per-resource max in
`ResourcePool` (today `max` is per-pool, so a second id inherits mana's curve); task cleanup in
`onDisable` (no task registry exists, and Paper cancels a plugin's tasks on disable); and adding a
`RepeatingTaskTarget` seam to `StatsBarSystem` purely to unit-test its lifecycle — `FakeTickTarget`
is package-private in `paper...adapter`, and this lifecycle is boot-witnessed exactly as the health
and nameplate loops are.
