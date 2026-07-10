# NEXT.md — plan after the Folia correctness port change

Read `CLAUDE.md` and `PROJECT.md` first. Work these in order. Stop and report
after each commit.

## Where this stands

Done and pushed: `1901981` through `e020f1f`. The visuals/statuses feedback
loop, the burst/area split, the `FakeWorld` timing harness, the snapshot/handle
port, stepped rays, and attribution. 156 tests green.

Verified on a real server: the plugin loads, `/rpg cast solar_grenade` detonates
with particles and sound, mobs ignite on the detonation frame, the lingering
field pulses, mobs aggro the caster, and killing one credits them.

**What is not verified on a real server**, and this shapes the plan:

- The stepped ray. No `Ray` ability ships, so `ChunkTraversal`, the chunk-plane
  DDA, and the 1-2 tick flight have only ever run against `FakeWorld`.
- `PaperCombatWorld.combatant(UUID)` and its `Regions.requireOwned` call. It runs
  only for `CastSpec.Self`, and no `Self` ability ships.
- `CastSpec.Melee`. Unit-tested, never cast.
- `StatusDefinition.Potion`. `StatusLoader` parses it; no content uses it, so
  `Registry.MOB_EFFECT.get(...)` has never executed.

Four code paths, proven by a fake and nothing else. Commit E is what walks them.

---

## Commit D — Housekeeping

Small, and all four items serve PROJECT.md priorities 1 and 3 directly.

### D1 — `scripts/dev-server.sh` is a hybrid, and that is worse than broken

Line 30 was patched to `"$(dirname "$0")/../mvnw"`, so the build resolves from
the script's own location. Line 5 is still `RUN_DIR="run"`, relative to the
caller's working directory. So it builds from the right place and looks for the
server in the wrong one — which is why `scripts/run/` appeared.

Half-fixed looks fixed. `README.md` and `CLAUDE.md` both point at it.

Either replace it with a version that resolves the repo root from
`${BASH_SOURCE[0]}` before doing anything, or delete it and document the manual
steps actually in use. A script nobody runs, referenced as the way to test, is
the same defect class as a javadoc asserting a property that does not hold.

Requirements if replaced: resolve the repo root from the script's own location;
use `./mvnw`; run `clean` (see D2); preflight for `paper.jar`, a PacketEvents
jar, and `eula.txt` containing `eula=true`; report every missing item at once
rather than one per run.

**Done when:** invoking it from `scripts/`, from the repo root, and from a
nested directory all behave identically. Or the file is gone and the README
tells the truth.
**Commit:** `chore: fix dev-server.sh repo-root resolution`

### D2 — CI, and `mvn clean` in particular

This is the item with a real justification, not hygiene.

Maven will not recompile a `paper/` file because a `core/` file changed. So a
non-exhaustive `switch` over a sealed interface compiles happily on an
incremental build — `mvn -pl paper -am compile` returns success. Only
`mvn clean compile` reports *"the switch statement does not cover all possible
input values."* `ContentValidator` had exactly that hole, and it was found by
accident.

Every sealed-interface guarantee in this codebase — `EffectSpec`, `CastSpec`,
`VisualSpec`, `StatusDefinition` — rests on `clean` running somewhere. Nowhere
runs it automatically today.

GitHub Actions, on push and PR:

```yaml
- uses: actions/setup-java@v4
  with: { java-version: '25', distribution: 'temurin' }
- run: ./mvnw -B clean verify
```

`clean` is not optional, and the workflow should carry a comment saying why.

**Done when:** a deliberately non-exhaustive switch in `paper/` fails CI. Test
this by pushing one to a scratch branch, confirming red, then deleting it. Per
`CLAUDE.md`'s verification rule: a check that never ran looks exactly like a
check that passed. That rule applies to the CI job itself.
**Commit:** `ci: build and test on push, with clean to enforce exhaustiveness`

### D3 — Rename the package

`io.github.yourname` appears in 70 files. Do it after D2 so CI watches the
rename. An IDE refactor handles the Java; check `paper-plugin.yml`'s `main:` and
the `<artifactSet>` includes in `paper/pom.xml` by hand.

**Done when:** `grep -rl 'io\.github\.yourname' .` returns nothing and the
server still boots.
**Commit:** `chore: rename package to <your real namespace>`

### D4 — Dependency automation

Renovate or Dependabot on `paper.version` and `packetevents.version`. Per
`CLAUDE.md`'s upgrade procedure, PacketEvents is the gate: it must bump first,
and it lags a Minecraft drop by one to two weeks. A bot opening the PR is how
you learn the gate opened.

**Commit:** `chore: automate dependency bumps`

---

## Commit E — Milestone 1 content: three abilities, zero Java

`Melee`, `Ray`, and `Self` are unit-tested and no content file exercises them.
Milestone 1 wants three abilities.

**This is not just content.** It is the first in-game execution of four code
paths listed at the top of this file, and it is a live rehearsal of milestone 4
("add a second class entirely through config").

### The acceptance criterion is mechanical

```
git diff --stat   # only .yml files
```

If Commit E requires touching Java, **the content pipeline is broken**, and you
want to discover that now with three abilities rather than later with three
hundred. Do not fix it by adding Java. Stop, report what the schema cannot
express, and treat that as the finding.

Adding `.yml` under `content/visuals/` and `content/statuses/` is still zero
Java. Adding a `case` to `VisualLoader` is not.

### Ship them in this order

**E1 — `void_slash`, a `Melee`.** `reach: 3.5`, `arc_degrees: 120`. A `Burst`
carrying `Damage` and `Knockback`. Needs a new `content/visuals/void_slash.yml`.
First in-game execution of the melee arc.

**E2 — `arc_surge`, a `Self`.** `Heal` plus a `Status` of `kind: potion`,
`potion_type: speed`. This one does double duty:

- it is the only path through `PaperCombatWorld.combatant(UUID)`, hence the only
  live exercise of a `Regions.requireOwned` capture site that has never executed;
- it is the first time `Registry.MOB_EFFECT.get(...)` runs. `ContentValidator`
  checks potion types at startup, but nothing has ever resolved one.

Deliberately typo `potion_type` once and confirm the validator warns at boot,
naming the file, and the server still starts. Then fix it. That is the fail-soft
path, and it has never been exercised on a potion.

**E3 — `solar_lance`, a `Ray`.** `range: 30`. Ship it last.

This is the one that matters. C2 made a 30-block ray cost one to two ticks
depending on aim, and nobody has felt it. Cast it down an axis, then diagonally
— a diagonal crosses more chunk planes, so it is slower. Does that read as a
weapon, or as lag?

Watch also for a miss that should have hit. A mob whose centre sits across a
chunk plane is invisible to the segment walking past it. That defect is pinned
by `rayMissesAnEntityWhoseCentreLiesAcrossAChunkPlane`, deliberately unfixed, and
it will look like a bug when it happens to you. It is a known one.

### Then boot and cast all three

Watch the console. `Regions.requireOwned` has never thrown, and on Paper it never
can — but `combatant(UUID)` is a path nothing has walked. Silence there is new
information, not the absence of it.

**Done when:** three abilities cast, `git diff --stat` shows only `.yml`, and the
console is clean.
**Commit:** `feat: add void slash, arc surge and solar lance`

---

## Commit F — One class

`PlayerProfile.archetypeId` and `unlockedAbilities` are persisted and read by
nothing. `/rpg cast` lets any permitted player cast any loaded ability.

Milestone 1 asks for one class. Gate `/rpg cast <ability>` on the caster's
archetype and their unlocked list, and add `/rpg class <archetype>` to set it.

Keep archetypes in content — `content/archetypes/hunter.yml` listing granted
abilities — not in an enum. The set of *elements* is a design decision and stays
an enum. The set of *classes* is content, exactly like abilities. That
distinction is the one this codebase has been enforcing all along.

**Done when:** a player with no class cannot cast, and `/rpg class hunter` grants
exactly the abilities `hunter.yml` names.
**Commit:** `feat: gate ability casting on the player's archetype`

---

## Then stop and play it

That is milestone 1 complete: one class, one element, three abilities, one world,
file storage. The only acceptance question is the one no test answers.

Before milestone 2, two things worth measuring rather than assuming:

- **The tuning loop.** Edit
  `run/plugins/Rpg/content/abilities/solar_lance.yml`, restart, cast. Time it.
  Under a minute means milestone 4 will be easy. Five minutes is a finding.
- **The energy economy.** `MAX_ENERGY` and `ENERGY_PER_TICK` are Java constants
  in `RpgPlugin`. Three abilities with different costs is the first time that
  will feel wrong. When it does, they become per-archetype content — the same
  cheap-now/migrate-later argument that moved `VisualSpec` to a `steps:` list.

---

## Deferred, deliberately

- **A ray misses an entity whose hitbox straddles a chunk plane.** Observable in
  `FakeWorld`, pinned by a test asserting the miss, carrying an inversion warning
  in its javadoc. Fixing it needs a widened trace or a neighbour-column query.
- **A projectile's per-tick segment still spans chunk columns** — the same bug
  class the ray had, now visible in the fake. `step` is unchanged.
- **`Bukkit::getCurrentTick`** backs `CooldownTracker` and `ResourcePool`. Folia
  has no single global tick; regions tick independently. This is the one Folia
  hazard the `Scheduler` abstraction does not cover, because it is not a
  scheduling call. Revisit before running Folia.
- **Read-then-write ordering in `EffectApplier`.** Reads come from a snapshot
  taken strictly before the writes. Harmless until an on-kill effect exists, at
  which point five area pulses will all see a living target and all fire.
- **`Element.multiplierAgainst`** is still the placeholder 1.5x/1.0x rule.
- **`RpgCommand`'s hop is on the caster's eye**, not the impact point. Now
  harmless: reads are snapshots captured under `requireOwned`, and the ray steps
  region by region. The comment there should say that rather than apologise for
  it.

---

## Rules for this work

- After every commit: `./mvnw -pl core test`. After every batch:
  `./mvnw clean package` and a manual boot.
- If a fix requires importing Bukkit into `core/`, stop and ask.
- Prefer a failing `core/` test that reproduces the bug before fixing it.
- Do not fix a compile error by widening the architecture.
- When you say something is verified, say what you executed.
- **Verify a check ran before believing it passed.** See `CLAUDE.md`.
