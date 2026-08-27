# PLAN — the resource rename: `energy` → `mana`

Branched off `origin/master` at `8c35401` (PR #21, the enchant economy pass), verified from origin
rather than assumed — `git fetch` then `git log --oneline -1 origin/master` printed `8c35401`, tree
clean.

## What this pass is

A **rebrand, not a mechanic change.** Nothing about how the pool works, regenerates, or is spent
moves. The resource stays a `resourceId` string; `ResourcePool` stays generic and keeps its name.
Only the id, the names derived from it, and the prose that said "energy" change.

No data migration was needed, and this was checked rather than assumed: `ResourcePool` is
runtime-only, per-player, and cleared on quit — nothing writes the id to disk — and item costs are
read from content at fire time, so already-minted weapons need nothing. They reference content that
now says `mana`.

## Do not touch — the invariants this pass depends on

These are deliberate. Each one is the thing a later "cleanup" would most plausibly undo.

1. **`ResourcePool` keeps its name.** Not `ManaPool`. It is keyed by an opaque `resourceId` and
   knows nothing about which resource it holds; the id only ever arrives as a per-call argument.
   Renaming it would bake one resource into a class built to hold any.

2. **The tooltip cost line and the insufficient-resource message stay id-derived.**
   `WeaponLoreLines.cadenceLine` builds its label as `capitalize(cost.resourceId())`, and both
   message sites — `RpgCommand.java:1064` and `RpgListeners.java:227` — interpolate
   `lacking.resourceId()` into `"Not enough %s"`. **Neither needed a code change in this pass**;
   they read "Mana Cost: 30" and "Not enough mana" purely because the id changed. Typing `"Mana"`
   into either would look like a harmless simplification and would silently break the next rename.
   See the reddening check below — that guard exists specifically to catch it.

3. **The reserved `"none"` id is untouched.** `ResourceCost.FREE` and `WeaponLoreLines.isFree`'s
   `"none"` check are the free-trigger sentinel, unrelated to this rename.

4. **The historical `PLAN-*.md` records still say "energy", by decision — not by an incomplete
   sweep.** `PLAN-rekindle.md:145` and `PLAN-ember-step.md:157` carry `resource: energy` snippets
   that accurately record what those passes shipped; rekindle really did cost energy then.
   Rewriting them would make the record claim rekindle shipped under a name it never had. The rule
   applied was **rename what asserts current truth, preserve what records past fact** — so
   `NEXT.md:575` (which named `MAX_ENERGY`/`ENERGY_PER_TICK` as constants that exist right now) and
   the two open forks in `DESIGN-build-system.md` were updated, while the five blockquoted `NEXT.md`
   journal entries and every `PLAN-*.md` were left alone. A repo-wide `grep -ri energy` therefore
   still returns hits, and that is correct. Do not "finish the job."

## Why the gate is a grep and not a boot

**Nothing validates the resource id at load time.** `ContentValidator` has zero `cost`/`resource`
references, and `ResourcePool.current()` returns `max` for an id it has never seen
(`entry == null ? max`). So an ability left on `energy` would not warn, would not error, and would
not fail to load — it would quietly spend from a **second, separate, permanently-full pool** while
its tooltip disagreed with every other ability. A half-done rename is invisible at runtime and looks
exactly like a working one.

That is why the acceptance condition is a clean grep over the code, not a successful boot.

## What was executed, and what it printed

| check | result |
|---|---|
| `./mvnw -pl core test` | `Tests run: 371, Failures: 0, Errors: 0, Skipped: 0` — a real `Tests run:` line, not a bare `BUILD SUCCESS` |
| `./mvnw clean package` | core 371 + paper 255, 0 failures, `BUILD SUCCESS` |
| `grep -rni energy core/ paper/ --exclude-dir=target` | **zero hits** |
| gate liveness probe | planted `# probe: energy` under `content/`, re-ran the gate, it **was seen**; probe removed, gate clean again |
| boot, `./scripts/dev-server.sh --refresh-content` | `Done (6.311s)!`, `[Rpg] Loaded 6 abilities, 7 visuals, 5 statuses, 7 elements, 4 enchants, 2 kits, 5 weapons, 1 mobs` — no plugin warnings |
| deployed content after refresh | all 8 `resource:` lines read `mana`; `grep -rni energy run/plugins/Rpg/content/` → zero |

The gate was probed on purpose. A `grep` that finds nothing because it is looking in the wrong place
prints exactly what a passing gate prints, and this repo has been bitten by a discovery that
silently found nothing before.

`--refresh-content` was **required, not optional**, and the run proved it: the deployed
`solar_lance.yml` still read `resource: energy` immediately before the refresh. The plugin ships
defaults with `saveResource(path, false)`, which never overwrites. Without the flag the server boots
the stale copy, the ability still spends `energy`, and the boot looks like a pass while proving
nothing.

### The reddening check

`WeaponLoreLinesTest` held a pair: `cadenceFoldsCostAfterCooldown` on the shipped id, and
`cadenceResourceNameComesFromResourceId` on a deliberately **foreign** id, to prove the label is
derived rather than typed. That foreign id was **`"mana"`** — so a naive rename would have collapsed
both fixtures onto one value and left a test that passes against a hardcoded `"Mana"`. The foreign
fixture was moved to `"focus"`.

Executed: replaced `capitalize(cost.resourceId())` with a literal `"Mana"` in `cadenceLine`, with a
`/* MUTATION_HARDCODED_MANA */` marker grepped to confirm it applied, and `-pl core test-compile`
run first to confirm it compiled. Result:

```
Tests run: 16, Failures: 1
cadenceResourceNameComesFromResourceId
  expected: <Cooldown: 1.0s | Focus Cost: 30> but was: <Cooldown: 1.0s | Mana Cost: 30>
```

**One failure out of sixteen.** `cadenceFoldsCostAfterCooldown` passed happily under the hardcode —
which is the direct evidence that the `"focus"` swap was necessary and not cosmetic. Had both
fixtures said `"mana"`, this mutation would have gone green and invariant 2 above would have had no
guard at all.

Restored from a scratchpad copy (`cp`, not `git checkout --`, since the tree held uncommitted work);
`md5sum` matched the pre-mutation hash `b795d298f96f43c9a1da245ed17dc4fb`, and the marker grep came
back clean.

## Not verified, and not counted as passed

The in-game half of the boot gate **did not run**. The server boots, loads, and serves the renamed
content, but joining as a player needs a Minecraft client, which this pass had no access to. So
these three remain witnessed only by unit tests and the loaders:

- the tooltip **rendering on a held item** as `Mana Cost: 30`;
- a weapon **firing** and the pool visibly draining;
- a **drained** pool printing `Not enough mana: 30 needed, 0 available` in chat.

What *is* proven without a client is stronger than a fixture, and worth stating precisely:
`AbilityLoaderTest.bundledSolarGrenadeContentLoads` and the two `WeaponLoaderTest` cases read the
**real shipped yml off the classpath** and assert `cost().resourceId()` is `"mana"` — so content
genuinely parses to the new id. The label derivation is pinned by `WeaponLoreLinesTest` and by
`WeaponLoreTest`'s `"Cooldown: 3.0s | Mana Cost: 40"`.

The gap is the render and the spend, not the id. To close it:

```
./scripts/dev-server.sh --refresh-content
/rpg give ember_staff     # tooltip should read "Mana Cost: 30" and "commit your mana to cast."
                          # fire it, drain the pool, fire again -> "Not enough mana: ..."
/rpg give ironblade       # left-click stays free and still renders no cost line ("none" untouched)
```

## Scope guard

Declined as out of scope for a rebrand, each its own later call: giving `ResourcePool` per-resource
max/regen (today `max` and `regenPerTick` are per-pool, so a *second* id in the same instance would
silently inherit mana's 100/60s curve — a real latent issue for **adding** a resource, not for
renaming one); adding load-time validation of the resource id; an id→display-name mapping layer
(`cadenceLine` uppercases only the first character, so a multi-word id like `soul_mana` would render
`Soul_mana Cost` — `inputLabel()` has the underscore-splitting logic `cadenceLine` does not use);
and making the two `"Not enough %s"` sites testable, which would mean extracting them out of their
switch arms.
