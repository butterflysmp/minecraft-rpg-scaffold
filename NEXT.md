# NEXT.md — plan after the Folia correctness port change

Read `CLAUDE.md` and `PROJECT.md` first. Work these in order. Stop and report
after each commit.

## Where this stands

Done and pushed: `1901981` through `45b4f05`. Commit D items D1 and D2 are complete;
D3 and D4 remain. The repo now lives at `butterflysmp/minecraft-rpg-scaffold`.

Earlier: The visuals/statuses feedback
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

### D1 — `scripts/dev-server.sh` is a hybrid, and that is worse than broken — **DONE** (`b296f9b`)

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

> #### 2026-07-10 — what verification turned up
>
> The repo-root fix was already in the working tree. The work was proving it, and
> the proof found a second defect the plan had not predicted.
>
> **`-h` was broken by the very bug D1 exists to fix.** It ran
> `sed -n '2,9p' "${BASH_SOURCE[0]}"` *after* the script `cd`s to the repo root, so a
> relative `$0` no longer resolved: `cd scripts && ./dev-server.sh -h` died with
> `sed: can't read ./dev-server.sh`, exit 2. It worked from the repo root by
> coincidence. Fixed with an absolute `SELF`. The `2,9p` range was also off by one and
> cut the help mid-sentence.
>
> Working for a reason you did not intend is not the same as working. This is the
> third instance in this project, after the projectile test whose target sat in
> column 0, and the M2 mutation that passed on a floating-point accident.
>
> `mvnw` and `scripts/dev-server.sh` were both mode `100644` in git. On
> `ubuntu-latest` that is `Permission denied` before Maven starts — a red run that
> proves nothing. Both are `100755` now.


### D2 — CI, and `mvn clean` in particular — **DONE** (`d101dd5`, corrected by `2b22522`)

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

> #### 2026-07-10 — everything above about `clean` is wrong
>
> The text stands as written. What follows is what happened when it was measured.
>
> **Predicted:** `mvn -pl paper -am compile` succeeds on a non-exhaustive switch in
> `paper/`; only `mvn clean compile` reports the error.
>
> **Ran.** A new record added to `EffectSpec.Targeted`'s `permits`, handled in
> `EffectApplier` so `core/` stays green, deliberately *not* handled in
> `ContentValidator.checkEffect`. Then, against a warm `target/`:
>
> ```
> ./mvnw -pl paper -am compile   BUILD FAILURE   ContentValidator.java:[84,9]
>                                                the switch statement does not cover
>                                                all possible input values
> ./mvnw -B compile              BUILD FAILURE   same error, after first printing
>                                                "Compiling 24 source files"
> ./mvnw clean compile           BUILD FAILURE   same error
> ```
>
> **True:** maven-compiler-plugin 3.13 sees the changed dependency module and
> recompiles all of `paper/`. There is no incremental hole. `clean` catches nothing a
> plain build does not, and on a fresh CI runner there is no `target/` to delete, so
> it cannot be the mechanism there either.
>
> The switch is checked whenever `paper/` is compiled. Full stop. What actually let
> `ContentValidator`'s original hole survive is that the daily loop —
> `./mvnw -pl core test` — never compiles `paper/` at all. **That** is the gap CI
> fills, and it would be filled just as well by `verify` without `clean`.
>
> **The shape of the error.** A real observation (Maven's incremental compilation is
> conservative) was extended to a plausible, unmeasured conclusion (*therefore* `clean`
> is what catches exhaustiveness). Nobody ran the two commands side by side. The
> conclusion was then written into `ContentValidator`'s javadoc, cited from there into
> this file, cited from this file into `scripts/dev-server.sh`'s build comment, and
> cited again into the implementation plan. Four documents in agreement, none of them
> in agreement with the compiler. Each new citation read as corroboration.
>
> `clean` is kept in the workflow and in `dev-server.sh` as defence-in-depth — against
> a `target/` cache action, a self-hosted runner, some future warm checkout — and is
> labelled there as measurably inert today. Not as the thing that makes the check work.

> #### 2026-07-10 — the evidence, so it is not re-litigated
>
> **Red for the right reason.** Branch `ci-proof-scratch` (`73c284d`) *deleted* the
> `case EffectSpec.Knockback ignored -> { }` arm from `ContentValidator.checkEffect`.
> Emptying the body would not have worked: the arm still handles `Knockback`, the
> switch stays exhaustive, and it compiles.
>
> ```
> ContentValidator.java:[101,9] the switch statement does not cover all possible input values
> rpg-core     SUCCESS
> rpg-storage  SUCCESS
> rpg-paper    FAILURE
> ```
>
> Wrong reasons ruled out explicitly rather than inferred from the red X:
> `setup-java` resolved Temurin 25 (`release 25]` in the log), `./mvnw` executed, and
> three of four modules built. It reached `javac` and died there. Event: `push`.
>
> The log also prints Maven's own confirmation of the correction above:
> `[INFO] Recompiling the module because of changed dependency.`
>
> Red run:   https://github.com/butterflysmp/minecraft-rpg-scaffold/actions/runs/29070685567
> Green run: https://github.com/butterflysmp/minecraft-rpg-scaffold/actions/runs/29071504222
>
> **Green with tests provably running.** Master run for `45b4f05`, event `push`, all
> four modules SUCCESS, and the guard step printed:
>
> ```
> Tests run across all modules: 156 (from 17 report files)
> ```
>
> A green build that ran zero tests is indistinguishable from one that ran all of
> them. Hence the guard. Counted independently: core 9, storage 2, paper 6 = 17 test
> classes.
>
> **Known gap in the guard.** It fails when the total is `0`. It does not fail at
> `101`. Add `<skip>true</skip>` to `paper`'s surefire config and you get core 88 +
> storage 13 = 101 — non-zero, green, and missing a whole module. Assert that *each of
> the three modules produced reports*, not merely that the total is non-zero. Module
> presence is the invariant; a bare total is not.
>
> **Cleanup owed.** Paste both run URLs above, then delete `ci-proof-scratch`.
> Workflow runs survive branch deletion; branches do not survive being forgotten.
>
> **A correction that stays.** `d101dd5`'s commit message says the guard sums
> "156 from 12 report files." The 156 was measured; the 12 was carried forward from an
> earlier core-only run and never checked. The real count is 17. Not force-pushed —
> history gets annotated, not rewritten, and this is the same failure as the `clean`
> claim directly above: a number inherited rather than measured, in a commit that was
> specifically about measuring numbers.


### D3 — Rename the package to `io.github.butterflysmp.rpg` — **DONE** (`f57536d`, guarded by `7b1ce26`)

**Namespace decided.** The repo now lives under the `butterflysmp` GitHub org, so
`io.github.butterflysmp` is a namespace the project controls rather than one the
account happens to own. `net.butterflysmp.rpg` would be the more correct form, but
reverse-domain notation is a claim of ownership, and the domain is not registered.
A package named after a domain you do not control is a small false assertion
repeated across 71 files. This project has spent a week deleting those.

71 files, reconciled: 65 `.java`, 4 `pom.xml`, `paper-plugin.yml`, and `NEXT.md`.
Nothing hides outside `*.java` / `*.xml` / `*.yml`.

**Two of them fail silently, and CI stays green through both.** An IDE refactor
handles the Java and misses these:

1. **`paper-plugin.yml`'s `main:`** — Paper loads the plugin by fully-qualified
   class name. Miss it and you get a clean build, a valid jar, and a server that
   simply does not load the plugin. No error worth reading.
2. **`paper/pom.xml`'s `<artifactSet>` includes** — `io.github.yourname:rpg-core`
   and `:rpg-storage`, visible in the CI shade log. Miss them and shade stops
   bundling your own modules into the jar. Green build, broken plugin.

Also check the four `groupId` elements.

**Done when:** `grep -rl 'io\.github\.yourname' .` returns nothing **and the
server boots and casts a grenade.** A green CI check is not the acceptance
criterion here — neither silent failure above is a compile error.
**Commit:** `chore: rename package to io.github.butterflysmp.rpg`

> #### 2026-07-10 — four defects, all in the plan's own checks
>
> **The `grep` above cannot be run as written.** `grep -rl … --exclude-dir=.git`
> also matches `target/` and `run/`, which are gitignored but not grep-excluded; it
> returns 74. `git grep -l` returns the predicted **71** — 65 `.java`, 4 `.xml`,
> 1 `.yml`, 1 `.md`. The count was measured; the command to re-measure it was not.
> It is also inverted: `grep` exits 1 on no match, so `# must return nothing`
> aborts under `set -e` precisely when it passes.
>
> **The two silent failures are not caught by booting. They are caught by `unzip`.**
> `RpgPlugin` types its fields with `AbilityRegistry`, `AbilityService`,
> `CooldownTracker`, `ResourcePool`; a missing `rpg-core` fails linkage at
> `onEnable`, long before any `/rpg cast`. So "casting the grenade proves rpg-core
> made it into the shaded jar" is false — the boot line already subsumes it, and
> `scripts/check-jar.sh` (`7b1ce26`) answers the question outright, with no server.
> It landed *before* the rename, on the `io.github.yourname` tree, so its green was
> established on a known-good baseline. It derives the package root from
> `project.groupId`, so the rename required no edit to it.
>
> **The boot log this file predicted does not exist.** It asks for two lines
> (`Loaded 1 abilities` / `Loaded 1 visuals, 1 statuses`). `RpgPlugin` emits one:
> `Loaded 1 abilities, 1 visuals, 1 statuses`. The string `Loaded 1 visuals` occurs
> in no build. An acceptance criterion that cannot be met reads, at a glance,
> exactly like one that was.
>
> **The acceptance criterion is not machine-executable.** `RpgCommand` gates casting
> on `instanceof Player` — "Players only." Everything else was verified; the grenade
> is still owed by a human.
>
> **What was actually run.** `git grep` clean; 65/65 `.java` files' `package` line
> equals their directory path; 6/6 source roots moved, recorded by git as 65 renames
> rather than add+delete; `./mvnw -pl core test` → 88 tests; `clean package` → two
> `Including io.github.butterflysmp:` lines; `check-jar.sh` → `Jar OK`;
> `dev-server.sh` → `[Rpg] Loaded 1 abilities, 1 visuals, 1 statuses` /
> `Done (5.087s)!`; deployed jar carries one package root.
>
> **`authors: [ yourname ]` was also a placeholder**, and the grep pattern did not
> match it. The pattern was a tool that missed one, not the definition of the scope.
> Now `authors: [ CreaperCrusher ]`. A sweep for other scaffold leftovers found
> nothing else: zero `TODO`/`FIXME`/`CHANGEME` markers. `<name>RPG Network</name>`
> in the root pom is left alone — possibly a default, possibly deliberate.
>
> **`check-jar.sh`'s own first draft failed the rename's acceptance grep.** Written
> as `case "$LISTING" in *yourname*)`, the guard spelled the superseded package in
> its own error message, inside the file asserting that package is gone. The fix was
> not an exclusion: "contains no `yourname`" is the wrong invariant. It now asserts
> *exactly one package root, and it is `$ROOT`* — no predecessor named, no second
> source of truth, and it catches the next rename too.

### D4 — Dependency automation — **PARTIALLY DONE** (`b2aaa44`)

Renovate or Dependabot on `paper.version` and `packetevents.version`. Per
`CLAUDE.md`'s upgrade procedure, PacketEvents is the gate: it must bump first,
and it lags a Minecraft drop by one to two weeks. A bot opening the PR is how
you learn the gate opened.

**Commit:** `chore: automate dependency bumps`

> #### 2026-07-10 — the guard gap is closed; the bot is not yet proven
>
> **The guard.** `Assert tests actually ran` failed at `0` and not at `101`. Now
> `scripts/check-tests.sh` asserts per-module surefire report presence and names the
> module that produced none. Proven in both directions:
>
> *The old guard is blind.* Its bytes were lifted verbatim from
> `git show HEAD:.github/workflows/build.yml` (sha1 `09df5a34`) — not retyped — and
> executed against a tree carrying `<skip>true</skip>` on `paper`'s surefire,
> confirmed live via `help:effective-pom`. It printed `Tests run across all modules:
> 101 (from 11 report files)` and **exited 0**. It was never pushed: a branch
> carrying a step engineered to be green-while-wrong is a hole waiting to outlive its
> cleanup.
>
> *The new guard reddens.* On the same tree: `exit 1`, naming `paper`, after printing
> `core` 88 and `storage` 13 — so the red is the per-module invariant, not a global
> collapse.
>
> Green run:   https://github.com/butterflysmp/minecraft-rpg-scaffold/actions/runs/29081216862
> Red run:     https://github.com/butterflysmp/minecraft-rpg-scaffold/actions/runs/29081304424
>
> Master was pushed and read **first**. `ci-proof-per-module` then differed from that
> green commit by five lines in one file, so its red is attributable to
> `<skip>true</skip>` and nothing else. A red on a branch carrying three never-CI'd
> commits *plus* a mutation would have established only that something there fails.
> Wrong reasons ruled out explicitly: `BUILD SUCCESS`, `rpg-paper … SUCCESS`, and
> `check-jar.sh` passed. Only the guard step failed. Branch deleted; runs survive.
>
> That green run is also `check-jar.sh`'s first execution on `ubuntu-latest`. It had
> only ever run on Windows — the environment where its CRLF hazard does *not* bite.
>
> **The bot is NOT done.** `renovate.json5` covers `packetevents.version` only;
> `paper.version` is deliberately excluded and the config says why at length.
> **It was committed unvalidated.** `renovate-config-validator` and `renovate
> --dry-run` both need Node, and this machine has no node, npm, or python beyond the
> Store shim. Neither would have been the real proof: they check schema and
> resolution, not whether the Renovate App, against this repo, with real permissions,
> detects `packetevents-spigot` inside a Maven `<properties>` value and holds
> `paper-api`.
>
> **Owed, and D4 is not done until it is met:** install the Renovate GitHub App and
> read its first Dependency Dashboard. It must list
> `com.github.retrooper:packetevents-spigot` as detected and must **not** list
> `io.papermc.paper:paper-api`. `dependencyDashboard: true` is load-bearing for
> exactly this — without it, "no PRs opened" is indistinguishable from
> "misconfigured". A config that has never opened a PR is a check that has never run.

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
- ~~**The zero-test CI guard passes at 101.**~~ **DONE** (`b2aaa44`) —
  `scripts/check-tests.sh`, proven red at 101 and green at 156. See D4 above.
- **`*.gitattributes` does not pin `*.yml` to LF**, and `core.autocrlf=true` on the
  dev machine. So a fresh clone checks `paper-plugin.yml` out as CRLF, `main:` carries
  a `\r`, and `check-jar.sh` goes red locally while staying green on `ubuntu-latest`.
  `tr -d '\r'` guards the jar's bytes, which is the check's job — but the source is
  unfixed. Three sightings now: the `main:` parse, and git's own
  `LF will be replaced by CRLF` warning on `build.yml` and on 70 files during the
  rename. `*.yml text eol=lf` as its own commit; not folded into a rename diff.
- **`check-jar.sh`'s `GROUP_ID` validation has never fired.** Maven's four JVM
  `WARNING:` blocks go to stderr, so the `$( )` capture stayed clean and the
  `case "$GROUP_ID" in ''|*[!a-z0-9.]*)` arm guarded correctly without ever being
  needed. Per `CLAUDE.md`, a guard that has never fired is a guard taken on faith.
  Inject garbage on stdout and confirm it reddens.
- **maven-shade `META-INF/MANIFEST.MF` overlap warning**, nine lines in every build.
  Benign — three of your own modules with stock manifests, shade picks one. But noise
  is where real warnings go to hide, and the day a genuine overlapping-resource
  warning appears you want to see it. A `ManifestResourceTransformer`, or filtering
  the manifest out of the dependencies, silences it.
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
