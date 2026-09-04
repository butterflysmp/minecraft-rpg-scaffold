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

> #### 2026-07-10 — the `requireOwned` half of that list is wrong
>
> **Measured, by grepping for the call site rather than reasoning about it.**
> `Regions.requireOwned` has exactly one caller: `BukkitCombatant.java:43`, inside
> `snapshot()`. Three methods reach `snapshot()` via `BukkitCombatant.of` —
> `combatantsNear` (`PaperCombatWorld.java:65`), `combatant(UUID)` (`:77`), and
> `castRay` (`:112`).
>
> `EffectApplier.java:99` calls `combatantsNear` for every `Burst` and every `Area`
> pulse; `castRay` runs on every tick of a projectile's flight. So one cast of
> `solar_grenade` walks that line six-plus times. It is among the **most-executed**
> lines in the plugin, not an unexecuted one.
>
> What is genuinely virgin is narrower, and worth stating exactly: **`combatant(UUID)`
> itself** — the `world.getEntity(id) instanceof LivingEntity` branch and its
> `Optional` — which only `CastSpec.Self` reaches. `arc_surge` walks that. It does not
> light up `requireOwned`, which needs no lighting.
>
> Same shape as the `clean` claim above: a plausible inference, never measured,
> corroborated only by its own restatements. It was asserted here, repeated in three
> messages, and propagated into two implementation plans before anyone grepped for the
> call site. The correction stays; the wrong text stays above it.

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
> — **Met. This sentence was false the day after it was written.** The grenade was
> cast on the renamed build: particles, blaze sound, ignition on the detonation frame,
> lingering pulses, mob aggro, kill credit. Attribution and the rename both survive on
> a real server. Line 15 of this file already recorded the pre-rename cast, so the
> sentence above contradicted its own document. A doc asserting an unmet obligation
> that was met is the same defect class as a javadoc asserting a property that does not
> hold — here, in the file that exists to catch them.
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

### D4 — Dependency automation — **guard DONE** (`b2aaa44`); **bot DECLINED**

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
> #### 2026-07-10 — the bot is declined, and `renovate.json5` is deleted
>
> It was written, committed unvalidated in `b2aaa44`, and removed again. Recording
> why, because the argument for adding it later will be the same argument that added
> it this time.
>
> **It could not be validated here.** `renovate-config-validator` and
> `renovate --dry-run` both need Node; this machine has no node, npm, or python
> beyond the Store shim. Neither would have been the real proof anyway — they check
> schema and resolution, not whether the App, against this repo, with real
> permissions, finds `packetevents-spigot` inside a Maven `<properties>` value.
>
> **The only proof was the App's first Dependency Dashboard**, which nobody had read.
> So the config sat in the tree in the one state this project does not tolerate:
> a check that has never run, indistinguishable from one that passed. A
> misconfigured `renovate.json5` does not fail loudly. Renovate simply opens no PR —
> and "no PR" is exactly what a correctly-configured bot looks like on a day when
> nothing needs bumping. The failure mode of the thing meant to watch the gate is
> silence, and silence is also its success mode.
>
> **One repo, one maintainer.** The bot would have watched a single property. The
> cost of checking Modrinth by hand is a minute, once every few weeks. The cost of a
> silently-dead bot is believing the gate is watched when it is not — and that belief
> is worse than knowing it is unwatched, because it stops you looking.
>
> `paper.version` was never a candidate: a PR on it arrives at step 3 of the upgrade
> procedure, before the step-1 information that decides whether to act; and
> `26.2.build.53-alpha` sorts *above* `26.1.2.build.74-stable` under Maven ordering,
> because the `-stable`/`-alpha` channel is a Paper convention with no representation
> in artifact metadata.
>
> **What replaces it:** `CLAUDE.md`'s upgrade procedure now opens at **step 0** —
> notice the release; nothing does this for you — naming
> <https://modrinth.com/plugin/packetevents/versions> and pointing back here. An
> absent notification looks exactly like nothing to notify. Making the manual step
> explicit is the honest version of automating it badly.
>
> If this is revisited: the acceptance criterion is not "the config is committed."
> It is a Dependency Dashboard listing `com.github.retrooper:packetevents-spigot` as
> detected and **not** listing `io.papermc.paper:paper-api`. Until someone has read
> that, there is no bot — only a file.

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

> #### 2026-07-10 — it does require touching Java, and the schema is not why
>
> The criterion worked. It found a real defect at three abilities rather than three
> hundred, and a **different** one than it was written to find. What the schema cannot
> express is the empty set: `melee`, `self`, `ray`, `heal`, `knockback`, `status`,
> `burst`, `kind: potion` and the `VOID`/`ARC`/`SOLAR` elements are all already there.
>
> The blocker is **packaging**. `RpgPlugin.java:42` names its shipped content in a
> hardcoded `String[]`, `saveResource` copies only the paths in it, and the loaders read
> the *data folder*, not the jar. There is no resource scan anywhere in `paper/`. So a
> `.yml` committed to `paper/src/main/resources/content/` reaches a running server only
> if somebody adds a line of Java. Seven new files, seven new lines.
>
> `CLAUDE.md` invariant 2 says "adding the 500th weapon must not require a recompile."
> **The pipeline is intact in the direction the invariant is usually read, and broken in
> the direction Commit E needs.** A server operator drops a `.yml` into
> `plugins/Rpg/content/abilities/` and it loads, zero Java. The *project*, shipping
> default content, cannot. Milestone 4 asks whether a whole class can be added through
> config alone. The answer was no, and nobody knew.
>
> Fixed by **Commit E0** before E, so that E is genuinely `.yml`-only — the same
> structure that put `check-jar.sh` before the rename.

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
  <br>**— wrong. See the correction at the top of this file.** `combatant(UUID)` is
  the virgin path. Its `requireOwned` capture site is one of the hottest lines in the
  plugin, reached by `combatantsNear` on every `Burst` and every `Area` pulse.
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

> The first clause is sound and the second is not. `requireOwned` throwing would be
> news; it has run thousands of times without doing so. `combatant(UUID)` is the path
> nothing has walked — but it reaches `requireOwned` through the same `snapshot()` as
> everything else, so **`requireOwned`'s silence during `arc_surge` carries no
> information at all.** What is new is `world.getEntity(id)` resolving, and the
> `Optional` coming back non-empty. Watch for *that*.

**Done when:** three abilities cast, `git diff --stat` shows only `.yml`, and the
console is clean.
**Commit:** `feat: add void slash, arc surge and solar lance`

---

## Commit F — One class — **DONE** (`4f3032a`)

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

> #### 2026-07-10 — what shipped, and the one line still owed to a human
>
> **The gate lives in `core`.** `AbilityService.cast` gained a `Set<String> castable`
> and a fifth `CastResult` variant, `Locked`, returned after the registry lookup and
> *before* the cooldown and energy checks. `paper` resolves archetype → granted abilities
> → the set and passes it in; core never learns what an archetype is. Adding `Locked` to
> the sealed interface was a compile error at exactly one site — `RpgCommand`'s switch,
> confirmed by grep to carry no `default` arm before the change — so the compiler found it,
> not a test at runtime.
>
> **The cast gate keys on `unlockedAbilities`, not on the archetype re-resolved each cast.**
> `/rpg class` writes the grant into `unlockedAbilities` once (via a new
> `ProfileService.setArchetype`, a command-thread mutator that persists immediately); the
> cast reads that list. So both persisted fields are finally read, `archetypeId` decides
> the *message* a locked classless player sees, and the two can never drift because
> `/rpg class` sets them together. This is the resolution of the "gate on archetype AND
> unlocked list" phrasing — the archetype is enforced transitively through the list it
> populates.
>
> **Two mutations, reported separately, because they guard different things.** Both on
> `AbilityService.cast`, restored from a scratchpad copy, never `git checkout`:
> - *Deleting* the `castable.contains` check reddened **both** gate tests — a non-granted
>   ability returned `Success`. That guards the check's existence.
> - *Reordering* it after the energy spend reddened **only** the order-pinning test
>   (`expected 100.0 but was 60.0` — a silent 40-energy drain), while the plain
>   "Locked returns" test stayed green. That is the reorder the sealed switch cannot catch
>   — every arm still exists — and the reason the order test was written at all.
>
> **`ContentValidator` now checks archetypes** behind a `Predicate<String>` seam: a warning
> per ability id no ability declares, plus one when a class's *resolved* set is empty (a
> class nobody can play, which a per-id check passes). The most invisible dangling
> reference of the four — a missing visual is a missing particle you can see; a dangling
> ability in a class is a permission gap that reads as intended design.
>
> **What the boot proved, and what it did not.** `./mvnw clean package` → 176 tests
> (core 94, storage 15, paper 67), both modules shaded. Booted Paper 26.1.2 build 74:
> `Loaded 4 abilities, 4 visuals, 2 statuses, 1 archetypes`, `Done (5.257s)!`, no
> dangling-archetype warning. `hunter.yml` shipped into a *fresh* `archetypes/` data
> folder — the `saveResource(false)` tuning-loop defect does not bite a new directory, so
> there was no "already exists" WARN for it, only for the pre-existing three content types.
>
> **The acceptance criterion is not machine-executable.** `/rpg cast` and `/rpg class` both
> gate on `instanceof Player` — "Players only." — so the console cannot walk them. The
> classless-refusal → `/rpg class hunter` → cast sequence is owed by a human on a client,
> the same shape as the grenade cast this file recorded for the rename. Until someone runs
> it, "a player with no class cannot cast" is verified only by the unit test that asserts
> `Locked`, not by a player who was told so.

---

## Then stop and play it

That is milestone 1 complete: one class, one element, three abilities, one world,
file storage. The only acceptance question is the one no test answers.

Before milestone 2, two things worth measuring rather than assuming:

- **The tuning loop.** Edit
  `run/plugins/Rpg/content/abilities/solar_lance.yml`, restart, cast. Time it.
  Under a minute means milestone 4 will be easy. Five minutes is a finding.
- **The mana economy.** `MAX_MANA` and `MANA_PER_TICK` are Java constants
  in `RpgPlugin`. Three abilities with different costs is the first time that
  will feel wrong. When it does, they become per-archetype content — the same
  cheap-now/migrate-later argument that moved `VisualSpec` to a `steps:` list.

---

## Deferred, deliberately

### Flint Staff visuals, PR 1 (the bolt becomes visible and audible) — what it created or exposed

The Fire Bolt was functionally correct and visually unusable: you could not see where it went, what
it hit, or whether it hit. This slice gives it a per-tick trail and a cast sound. The ITEM BODY — a
real tumbling flint — is PR 2, deliberately.

#### THE GATE — 15 rows, observed in-game at tip `c734362` / tree `15ddaeb`

Reported green by the operator. Thirteen rows are binary and their expectations ARE the row text,
reproduced here so this record stands without the gate page:

| row | expectation | result |
|---|---|---|
| A1 | boot `--refresh-content`: 10 visuals (7 + 3), no `Unknown visual_id`, no unresolved sound key | OBSERVED |
| B1 | sky shot: both cast sounds immediate, ~2s before the mid-air pop | OBSERVED |
| B2 | spam through the 24-tick cooldown: one sound pair per cooldown | OBSERVED |
| B3 | mana below 5: silence | OBSERVED |
| C1 | F5 third person: stream starts ahead of the model, not at its head | OBSERVED |
| C2 | first person, dark: no flame flash at the crosshair on the click frame | OBSERVED |
| C3 | sky arc: flame and smoke once per tick along the whole path | OBSERVED |
| C4 | `flint_trail` flame behaviour | **MEASUREMENT — NOT CAPTURED** (see below) |
| C5 | `ember_trail` flame behaviour | **MEASUREMENT — NOT CAPTURED** (see below) |
| D1 | mob hit: a fwoosh (`item.firecharge.use`), not a bang | OBSERVED |
| D2 | sky expiry: impact plays in mid-air at ~40 ticks | OBSERVED |
| D3 | mob visibly burns for ~4s after a hit | OBSERVED |
| E1 | `ability_stone` Rekindle unchanged | OBSERVED |
| E2 | `ember_staff` / `solar_lance` unchanged | OBSERVED |
| E3 | `hunters_bow` leaves no trail | OBSERVED |

**The thirteen are deliberately NOT expanded into reconstructed one-line results.** The verdict is a
day old and the detail is not. A reconstructed detail is a fabricated detail even when the verdict it
decorates is true, and it is WORSE than a missing one, because it reads as contemporaneous.

#### NAMED DEBT: C4 AND C5 WERE RUN AND REPORTED GOOD, BUT THEIR FIGURES WERE NEVER CAPTURED, SO THE `speed` FIELD HAS NO PRODUCTION WITNESS

The pair was the only end-to-end evidence that `speed` reaches the renderer. `flint_trail` authors
0.0 and `ember_trail` runs at the unchosen 1.0 default, so **flint hanging BESIDE ember drifting** is
the field doing its job.

**C4 alone is not evidence.** A still flame is also exactly what a field that never arrived would
produce. "Good" is true of both outcomes and distinguishes neither — which is the whole reason the
row was a matched PAIR and not a single observation.

What IS established without it: `VisualLoaderTest` asserts an absent `speed` is 1.0 and an explicit
0.0 survives; `ContentValidatorTest` asserts the shipped files carry cfde822's four values through
the real loaders. **The field is proven as far as `VisualSpec.Particles`.** The unwitnessed span is
the last hop — `PaperCombatWorld.present`'s 7-arg `spawnParticle` to what a player actually sees.
E1/E2 passing is weak evidence for that hop (six visuals at 1.0 look unchanged), and *weak* is the
right word: it tests the DEFAULT surviving, not a non-default arriving.

**Recoverable, and cheaply.** Boot, fire the staff, fire Rekindle, watch one flame of each, write two
words. Under a minute. **Closes at the PR 2 boot**, where the same pair has to be looked at anyway.

#### A GLOBAL AFFIRMATION IS NOT A MEASUREMENT, AND AN INSTRUMENT THAT COUNTS TICKS WILL COLLECT TICKS

Why the debt above exists at all, and it is about the gate page rather than about anyone's care.

The page marked C4 and C5 as measurements and then gave each **a checkbox AND a text field — and
only the checkbox moved the progress bar.** So completing the page meant ticking, and the figure the
row existed to collect contributed nothing to the count. Asking twice afterwards does not repair an
instrument that made the wrong answer the easy one.

> **NEXT GATE PAGE: A ROW THAT NEEDS A FIGURE GETS NO CHECKBOX.** Its text field is what marks it
> complete, and the progress count treats a blank field as UNRUN. The page must not be able to reach
> 100% with a measurement missing.

This is the same family as the defects `CLAUDE.md` already records — a check that did not run looking
exactly like a check that passed — but one layer out: here the check DID run, and the instrument
discarded its result while recording that it had happened.

**IT HAS NOW HAPPENED THREE TIMES, WHICH IS WHY THE FIX BELONGS IN THE PAGE AND NOT IN ANYONE'S
ATTENTION.** Found by grepping this file and `GATE-crafting.md` for the phrasing such a debt gets
written in:

- `GATE-crafting.md:756` and `NEXT.md:3275` — Crafting Slice 7, per-row figures not captured. **Still
  open.**
- `NEXT.md:3897` — the defense/shields row 5, where the exact HP figure was not captured and the row
  fell back to "operator-observed rather than measured". The predicted ladder (`75 / 70 / 50 / 40`
  over four hits) was written down; the observed one was not, so the row cannot distinguish the four
  cases it was designed to separate.
- C4/C5, here.

Three independent slices, the same instrument, the same loss. Nobody was careless three times; the
page collects what it counts.

#### A PRE-REGISTERED EXPLANATION FOR AN UNEXPECTED RESULT IS A BLINDFOLD IF THE RESULT HAS A SECOND CAUSE

This is the durable one out of this slice, and it is a rule about *planning*, not about particles.

The plan said: ship cfde822's trail numbers UNCHANGED, and if the stream reads sparse at the boot,
record that as EXPECTED (the item body is missing) rather than fixing it. That instruction is
correct — raising the count now would mean PR 2 adds the body back into a stream tuned for its
absence, and the finished bolt is too busy, from a tuning nobody made wrongly.

It is also safe **only if the numbers really are cfde822's**. Three of the four new particle steps
were not. They omitted `speed`, and so inherited the schema default of 1.0 where the old repo had
0.0 — a fast outward spray in place of particles that sit where they are put. Had that shipped, the
instruction would have converted **a wrong appearance into an expected one and closed the
question**: the stream would have read wrong, the pre-registered explanation would have absorbed it,
and nobody would have looked again.

> **Deciding in advance what a wrong reading will mean costs you the reading.** Pair the prediction
> with the thing that would DISTINGUISH the two causes, or do not pre-register it.

Here the distinguishing thing is a test:
`ContentValidatorTest.theShippedFlintStaffCarriesCfde822sNumbers` loads the actual shipped yml
through the actual loaders and asserts every `speed`. With it green, "sparse" has exactly one
candidate cause. Verified by mutation — delete the `speed: 0.0` from `flint_trail.yml` and it
reddens with `expected: <0.0> but was: <1.0>`, which is precisely the defect described above.

Note where the error sat: the same document that FOUND the 1.0 default, and correctly protected six
shipped files from a 0.0 default, then authored four new steps that inherited 1.0 a page later. The
evidence was on screen in the message that named the remedy.

#### THE PARTICLE `extra` DEFAULT WAS 1.0 ALL ALONG, AND NOBODY CHOSE IT

`PaperCombatWorld.present` called the 6-argument `spawnParticle(Particle, Location, int, double,
double, double)`. Decompiled from the pinned Paper API (`javap -p -c org.bukkit.World`), its default
chain ends at `dconst_1`. So every visual in `content/` — `ember_burst`, `ember_trail`,
`solar_detonation`, `solar_lance`, `arc_surge`, `void_slash` — has been running at extra 1.0, tuned
by eye against a value nobody selected.

**The finding's blast radius runs in BOTH directions, and only one is obvious.** It says the new
`speed` field must default to 1.0 (0.0, the reflexive default for a new numeric field, would
silently restyle all six). It ALSO says every newly authored file must state its own `extra`,
because "absent" now means a value chosen for backward compatibility rather than for that file. The
first direction was applied immediately; the second was missed. Guarded now by
`VisualLoaderTest.anAbsentSpeedIsOnePreservingWhatEveryOlderVisualWasAuthoredAgainst`.

**This observation was MADE at the PR 1 boot and its figure was LOST — it is gate row C5, and the
debt above is where it now lives.** `ember_trail.yml` is `count 1, spread 0.0` and its comment claims
it "places a single flame at the ember and nowhere else", while running at extra 1.0. Whether it
actually sits still is the question C5 answered and did not write down. **Do not change the file; look
at the PR 2 boot, and record what is seen.** If it drifts, the comment is wrong and that is a second,
independent witness for authoring `extra` explicitly everywhere.

#### THE LAUNCH-FRAME TRAIL PUFF LANDED IN THE CASTER'S EYE, AND A COUNT ASSERTION CANNOT SEE IT

`ProjectileFlight.step` presents the trail at `position` BEFORE stepping, and on the launch frame
`position` IS `aim.origin()` — which for a weapon is the player's eye. So the first flame of every
shot spawned inside the shooter's own camera. cfde822 never did this: its tracker was
`runTaskTimer(plugin, 1L, 1L)`, first draw one tick AFTER launch at an already-moved position. Fixed
by skipping the present when `elapsed == 0`, which reproduces that delay exactly.

The part worth keeping: **the obvious test is structurally blind to it.** "The trail presents once
per flight tick" is TRUE whether or not the first draw is in your face — the count is identical
either way. Only the POSITION distinguishes them, which is why `FakeWorld` gained `presentedAt`
alongside `presented`. A guard that cannot fail for the reason it claims is the defect this file
already records twice.

Deliberately NOT generalised to `EffectApplier.trackEmber`, which draws inline on its own launch
frame and should keep doing so: `throw_embers` spawns a real item AT the origin, so its frame-0
particle sits on a visible body rather than in a face. Different situation; `ability_stone` was out
of scope.

#### A SHARED VISUAL IS A COUPLING, AND THE COUPLING IS INVISIBLE AT BOTH ENDS

`ember_burst.yml`'s own comment records that its count went 30 → 10 and its spread 1.0 → 0.3, "kept
tight so three near-simultaneous detonations don't read as scattered clutter" — a tuning made for
the Ability Stone's three-ember throw. The Flint Staff, which fires ONE bolt on a 24-tick cooldown
and has the exact opposite need, was firing that same file and silently inherited the fix.

Neither weapon's file was touched. Nothing warned at either end. That is rule 3 in tuning: a change
made for one consumer degraded another that shares the asset. The staff now has `flint_impact.yml`,
`flint_trail.yml` and `flint_cast.yml` of its own, and each says in its header what it is for.

There is no mechanism proposed to detect the next one. Worth knowing that a `visual_id` reference is
a dependency with no arity: nothing in the repo can say "this file has two consumers with opposing
needs".

#### `on_cast` IS A NEW CONTENT-MODEL CONCEPT, AND ITS BOUND IS DELIBERATELY NARROWER THAN ITS TYPE

The repo's older idiom for a cast noise is an untargeted `visual` in `on_hit` — `rekindle.yml` and
`ability_stone.yml` both do it, and it works because a Dash's untargeted effects fire once at the
origin whether or not it catches anyone. A **projectile** breaks it: its `on_hit` fires at the impact
point, after flight, so the staff would have announced itself a fifth of a second late at the far
end of the shot.

`AbilityDefinition.onCast` is typed `List<EffectSpec.Visual>`, **not** `List<EffectSpec.Untargeted>`,
and the narrowness is the point. `Untargeted` also permits `Area`, `Burst` and `ThrowEmbers`. None
was designed for this position; `Burst` would deal mob damage at the caster's own eye on every cast,
and `ThrowEmbers` is outright degenerate there — the applier's four-argument entry point passes a
ZERO direction, so the fan would be computed around a zero vector. Enumerating the one case you have
and then picking a bound that carries three you have not decided about is how a schema grows
behaviour nobody chose.

#### DEFERRED WITH A TEST: HIT AND MISS ARE INDISTINGUISHABLE IN THE VISUAL FILE

On lifetime expiry `ProjectileFlight` calls `onImpact.at(null, next)`, so a MISS detonates and plays
the same visual and the same sound a HIT plays. Targeted effects are skipped; the visual is not.

cfde822 had the same property — same sound both ways, and its bursts differed only 14 FLAME against
12, which is imperceptible. So fixing it is **new scope, not restoration**, and it is not bundled
under "match the old repo".

**Why it is deferred rather than built.** The reason you cannot tell a hit from a miss is that the
bolt is invisible, and that is what this slice fixes. Once you can see it, a bolt that stops in a
mob's chest and one that sails past and pops in empty air are already distinguishable by POSITION. A
hit also carries feedback a miss structurally cannot: hurt flash, knockback, health drop, and scorch
burning the mob for 80 ticks. The two events are not indistinguishable in the field — they are
indistinguishable in the VISUAL FILE, which is a smaller claim than it sounds. And `on_cast` is
already one new content-model concept in this slice; `on_miss` would be a second, in the same slice,
for a weapon with one trigger.

**THE TEST, AND IT RUNS AFTER PR 2, NOT PR 1.** Fire at a mob at max range, then deliberately past
it. If it is not clear within about a second which one happened, it comes back as its own scoped item
and gets built against a bolt that has actually been watched — rather than tuned now against a
baseline nobody has seen. It is evaluated after the item body lands because the body changes how the
terminal frame reads, and that judgment is not being spent twice against a partial state.

**GATE ROW D3 CONFIRMED THE PREMISE THIS DEFERRAL RESTS ON, and that is recorded here rather than in
the row list, because the deferral is what a future reader will act on.** D3 — "mob visibly burns for
~4s after a hit" — was OBSERVED. So the paragraph above is not a hopeful argument any more: a hit
really does carry feedback a miss structurally cannot, and hit and miss ARE distinguishable **in the
field** even though they remain identical **in the visual file**. Had D3 failed, the deferral would
have lost its leg and `on_miss` would be owed now rather than after PR 2.

#### WHAT PR 1's GATE MAY AND MAY NOT CLAIM — read this before citing "15/15 green"

**This gate did not claim the staff matches cfde822, and still does not.** It claimed exactly two
things: **a per-tick flame trail is drawn at the bolt's computed position, and the two cast sounds
play on trigger.** Faithfulness is a PR 2 row, because half of what would be compared — the flint
item body — does not exist yet. **A future reader seeing "15/15 green" will otherwise read it as
"the port was verified."**

PR 1 is a proper SUBSET of the target, not an approximation of it: cfde822 drew FLAME x2 + SMOKE x1
at the item's live position, and that position is the same number whether or not a flint chunk is
rendered there. Nothing here gets thrown away or rewritten by PR 2.

Two things are open at the PR 2 boot, not one:

1. **C4/C5** — the `speed` field's production witness, above.
2. **Whether the trail needs the count it deliberately did not get.** Only answerable now, because
   the stream has been seen WITHOUT the body and will be seen again WITH it. Do not pre-judge it in
   either direction; that is the whole reason the numbers shipped untuned.

### Crafting, Slice 7 (custom recipes, and the Flint Staff) — what it created or exposed

The first slice in which crafting a thing the server did not previously know how to craft produces
RPG gear. Six slices of mint-on-craft could only ride recipes Minecraft already had.

#### NAMED DEBT: `scorch` is vanilla-rated and does not credit the caster

**Recorded 2026-09-03, in the slice that shipped the first weapon to depend on it.**

`content/statuses/scorch.yml` is `kind: fire`, which `BukkitCombatant.applyStatus` resolves to
`entity.setFireTicks(...)`. So the burn is **vanilla fire**: no kill credit to the caster, no
`DamageSources.magic(shooter)`, and no interaction with the damage system at all.

**What the Flint Staff reproduces exactly, and what it does not.** The old repo's
`StaffListener.resolveFlintStaffHit` did **two** things, and `status scorch 80` is only the first:

| old | ours |
|---|---|
| `target.setFireTicks(FLINT_STAFF_BURN_TICKS)`, 80 ticks | **identical** — this is what `scorch 80` is |
| `applyFlintStaffIgnition` — 5 damage/second for 4 seconds, `DamageSources.magic(shooter)`, knockback suppressed per tick, cancelled on target death | **not shipped** |

That table is the debt, sized. It is written here rather than left to be discovered as *"the DOT
feels wrong"*, which is what it will look like from inside the game.

**What paying it down would take:** a real DOT effect kind — a status that ticks damage through
`CombatantHandle` on a scheduler, crediting a source — which is a status-system change, not a
content edit. `applyFlintStaffIgnition` in `BSMPMenu` is the working reference implementation.

**Do it in the slice that implements DOT statuses generally**, not for one weapon.

#### NAMED DEBT: `CraftResultIndex` holds two indexes and one of them is not a result index

**Recorded 2026-09-03.** The class now answers two questions: *which gear does this crafted
MATERIAL become* (the original axis) and *which gear does this RECIPE OF OURS mint* (new). The name
describes the first only.

**Why it was not renamed in this slice:** a rename touches every consumer — `AdapterContext`,
`RpgPlugin`, `InventoryCraft`, `RecipeProbe`, `RecipeCatalogue` and the test — for zero behavioural
change, in a slice that already moves five call sites. **What paying it down would take:** one
rename plus 6 import lines; re-verify is `./mvnw clean test`, no boot gate.

**Do it when something else already has those files open.**

#### THE AXIS: PLACES THAT USE DURABILITY AS A PROXY FOR PROVENANCE

**This is the entry to read before touching anything durability-shaped**, and it exists because
three sites that all had to change in this slice were found three separate ways. Three found by
three insights is luck. Three found by one enumeration is repeatable, and it is what catches the
fourth.

```bash
grep -rn "WeaponDurability.maxOf" --include=*.java core paper storage \
  | grep -v "/target/" | grep -v "/src/test/"
```

**It returns SIX sites, not three, and the split is the useful half of this entry** — a reader who
does not have it will either edit three correct sites or dismiss the whole list:

| site | durability is... | verdict |
|---|---|---|
| `RpgCommand.java:1045` | the SUBJECT — reports "has no durability", naming `ember_staff` and `ability_stone` | correct, leave alone |
| `ShieldBlock.java:155` | the SUBJECT — "An item with no durability at all is NOT broken" | correct, leave alone |
| `ShieldDurability.java:103` | the SUBJECT — early-out on empty | correct, leave alone |
| **`InventoryCraft.java` / `RecipeProbe.claimedBy`** | a **PROXY for "is this ours"** | narrowed in slice 7 to the material arm only |
| **`RpgListeners.onCrafterCraft`** | a **PROXY for "is this ours"** (the Crafter output policy) | gained a recipe-key arm in slice 7 |

The second group is the axis. **Durability was a complete statement of "this craft is ours" only
while a claim could only be made on a material** — every claimable material happened to be durable.
Recipe-identity claiming ends that: our recipes register a plain vanilla result, and the Flint
Staff's is a `stick`.

> **A javadoc argued the gate was safe, and this slice falsified the argument.**
> `RecipeProbe.claimedBy` said the durability gate was belt-and-braces "because boot already refuses
> a `craft_result` on a material with no durability, so nothing in the index can fail it". True of
> the material axis, and never true of the recipe axis — `validateCraftResults` makes no such demand
> of a recipe claim, by design. The paragraph was rewritten in the same edit that broke it. **A
> justification that outlives its premise is worse than no justification**, because the next reader
> trusts it.

#### DECISION: the Flint Staff can never break, and it is the first CRAFTABLE gear that cannot

`material: stick`, and `Durability.isBroken` opens with `if (maxDurability <= 0) return false` under
a javadoc naming it **the staff-and-stone exemption**. So the staff never wears and never breaks.

**It is not the first indestructible gear — it is the first craftable one.** `ember_staff`
(`blaze_rod`) and `ability_stone` (`amethyst_shard`) are already non-durable on master and already
never break; the exemption is *named* for them. What is new is that indestructible gear can now
reach a player through the **economy** rather than only through `/rpg give` or a kit, which is the
half that matters for balance. The **old** staff could break — `StaffListener` checked
`ItemRegistry.isBroken` before firing, which is only meaningful on an item that wears.

> **The first draft of this note said "the FIRST piece of gear that never degrades — every shipped
> weapon, shield, tool and armor piece is durable", and then named `ember_staff` and
> `ability_stone` as counterexamples four lines later, in its own corollary.** It contradicted
> itself inside one paragraph, and `CraftResultIndex.forRecipe`'s javadoc — written in the same
> commit — listed all three non-durable weapons correctly. A claim of primacy is worth checking
> against the tree before it is written down; `grep -rn "^material:" content/weapons` answers it.

**The corollary matters more than the decision: nobody may "fix" this by requiring gear to be
durable.** That would break an exemption the codebase holds deliberately, and which the two weapons
above depend on. If indestructible is not wanted, **the material is the lever**, and it costs one
word now against a re-mint later.

#### THE 2x2 INVENTORY GRID IS UNGUARDED, AND THE FLINT STAFF IS SAFE BY SHAPE, NOT BY GUARD

The player's own 2x2 crafting grid is the one crafting surface that neither mints nor is hijacked.
`onPrepareCraft` screens it for our gear used as an **ingredient**, but nothing there replaces a
vanilla result with minted gear — `commitCraft` is reachable only from `CraftingMenu`.

**This is pre-existing, not opened by custom recipes.** `content/tools/iron.yml` has
`shears: craft_result: shears`, and vanilla shears is a 2x2 recipe — so crafting shears in the
inventory grid **today** yields a plain vanilla pair.

`flint_staff` cannot reach it because its shape is three rows tall. Since the person who one day
shortens a shape will not think to look, `RecipeDefinition.fitsInTwoByTwo()` exists and the boot
**warns** when a custom recipe fits — so shortening a shape is loud rather than silent.

> **Do NOT close this by refusing our recipe keys in `onPrepareCraft`.** `commitCraft` uses the
> *player* overload of `craftItemResult`, which fires `PrepareItemCraftEvent` by contract, and the
> existing handler's `setResult(null)` is honoured — `InventoryCraft` relies on exactly that. A
> blanket refusal would null our own result. Scoping it by `InventoryType` needs **a measurement
> that has not been made**: which `InventoryType` `Bukkit.craftItemResult` synthesises. Make that
> measurement first, or use `CraftItemEvent`, which our path never fires.

#### THE DEFECT `fits` HAD BEEN HIDING BEHIND max-stack-1

`InventoryCraft.craft` asked `MenuSafety.fits(viewer, recipe.getResult())` — the recipe's
**registered** result. From this slice that is not the item the player receives: a custom recipe
registers a plain vanilla stack and delivers a minted one.

`fits` credits room in any stack it finds `isSimilar`. A player with no empty slot but 32 plain
sticks is told a stick fits — and it would; the minted staff carries meta, is not similar, and needs
a whole slot. `fits` says yes sixty-four times and sixty-four minted weapons hit the floor: the pile
`fits`' own javadoc was written to prevent.

**It was unreachable before now, and by accident.** Every `craft_result` is a durable material, max
stack 1, so a matching stack of one credits nothing and only empty slots ever counted — the two
items agreed for a reason nobody chose. **A stackable craft result is what made them disagree.**

`CraftingMenu.craftRepeatedly` needed no fix and must not grow one: it reads the result slot, which
`refreshPreview` already minted. Same reasoning, opposite conclusion, and both are commented so the
asymmetry is not "tidied" later.

#### A GOLDEN-FILE TEST THAT CANNOT FAIL FOR THE REASON IT CLAIMS

`RecipeDefinitionTest.theShapeAndIngredientMapAreDefensivelyCopied` was written asserting that both
the shape list and the ingredient map are copied from the caller. **The ingredient half could not
fail**: normalisation rebuilds the map into a fresh `LinkedHashMap` before storing it, so the
caller's map is never aliased whatever `Map.copyOf` does. Verified by mutation — `ingredients =
normalised` with `Map.copyOf` removed, all 16 tests still green.

Split into `theShapeIsDefensivelyCopiedFromTheCaller` (a real guard, mutation-proven) and
`theStoredCollectionsAreUnmodifiable`, which is what actually witnesses `Map.copyOf` — and which
reddens under that same mutation. **This is CLAUDE.md's fourth recorded verification defect** — a
defect-asserting test that passes on an accident rather than on the thing it names — found by
running the mutation rather than by reading the test.

#### `./mvnw` FROM THE WRONG DIRECTORY IS A SILENT PASS

A `cd` inside one tool call left the shell inside `paper/src/main/java/...`. Three subsequent
`./mvnw ... | grep ERROR` invocations printed nothing and read as green builds. They had not run at
all: `./mvnw` did not exist there, and `bash: No such file or directory` contains no "ERROR" for the
grep to find. Two "compiles clean" claims were wrong, and the next real build failed on two missing
imports that had been there the whole time.

**The same shape as every other entry on CLAUDE.md's verification page**: a check that did not run
looks exactly like a check that passed. The cheap habit that closes it is grepping for the POSITIVE
token — `BUILD SUCCESS` — rather than the absence of a negative one.

#### GREEN CI ON A BOOT-WITNESSED SLICE IS A REGRESSION SIGNAL WEARING A COMPLETION BADGE

**The suite is green either way for everything this slice does.** Registration, minting on all three
surfaces, the Crafter refusal and the `fits` fix have **no unit witness between them** — every one
of them needs a running server. 1257 passing tests say the slice broke nothing that was already
covered. They say **nothing** about whether the slice works.

"CI never boots a server" is true and too soft to act on. The statement that changes behaviour is
the one above, because the danger is not that someone believes CI covers this — it is that a green
check at the top of a PR is exactly what makes a gate feel optional. **This is the moment a gate
gets skipped**, and the table below is the list of what would be skipped with it.

#### THE PATTERN: ASK WHERE THE OBSERVATION SITS RELATIVE TO THE STATE IT DESCRIBES

**Three rows in one gate section had the same defect, and none of them could be caught by reading
the row.** All three were internally coherent, all three cited a real mechanism, and all three were
found only by attempting them.

| row | its witness | why it could not work |
|---|---|---|
| **R2**, first form | a second `Custom recipes:` line after `/reload` | vanilla `/reload` never re-enables the plugin, so the line is **unproducible** |
| **R5**, first form | a second `Recipe catalogue built:` line after `/reload` | the catalogue is cached on the `RpgListeners` instance made in `onEnable`, so likewise **unproducible** |
| **R5**, second form | catalogue counts before a `/reload` and after a RESTART | both readings are **single-registration** states; the double-registered state exists for half a minute between them and nothing looks at it |

The first two are "the state never occurs". **The third is subtler and is the one worth naming**: the
state DID occur, the row's mechanism was real, and the measurement was simply taken on either side
of it. Reading that row gives no warning at all — it names a genuine cause and then samples around
it.

> **FOR EVERY ROW, ASK WHERE THE OBSERVATION SITS RELATIVE TO THE STATE IT DESCRIBES.** Not only
> "is this state reachable" but **"does the measurement happen while it holds"**.

That question would have caught all three at writing time, and it is cheap: trace the procedure
step by step and mark, at each step, what the world looks like and what is being read.

##### And when a row fails that question, SIZE IT BEFORE REWRITING IT

R5's third form would have been a one-line fix — reload *first*, then open the browser for the first
time, so the catalogue builds after two registrations. Correct, and it would have been the wrong
move, because the row's condition **cannot occur at all**:

`RecipeCatalogue.build()` walks `Bukkit.recipeIterator()` → `RecipeManager.getRecipes()` →
`RecipeMap.values()` → **`byKey.values()`**, and `byKey` is a `java.util.Map<ResourceKey,
RecipeHolder>`. One value per key, by definition of `Map`. A duplicate cannot reach the count
whatever the registrar does.

*(`RecipeMap` does hold a duplicate-capable `Multimap byType`, and `removeRecipe` was verified to
clean both structures — but the iterator never reads `byType`. Two independent reasons, and the
weaker one is the interesting one: had the iterator walked `byType`, the row would have been worth
keeping with the corrected ordering.)*

So R5 was struck as **IMPOSSIBLE — never a test**, on the same footing as 7H. **A row rewritten
until it runs, when its condition cannot occur, credits coverage that does not exist** — which is
worse than no row, because the count says 24 and one of them can never fail.

**The rule that follows: when a row cannot observe its state, ask whether the state is reachable
BEFORE fixing the procedure.** The reflex is to repair the measurement; the question is whether
there is anything to measure.

#### A /reload SILENTLY STRIPPED EVERY RECIPE WE REGISTER — found by gate row R2, fixed in the same slice

**Observed 2026-09-04.** Vanilla `/reload` rebuilds the server's recipe manager and does **not**
re-enable plugins. `onEnable` was the only caller of `registerAll`, so after any reload our recipes
were **gone until the next restart**: the craft stopped working, the browser stopped listing it,
and **nothing logged and nothing warned**.

**THIS WAS NEVER ABOUT THE FLINT STAFF.** It was a property of anything registered into the recipe
manager at enable time on this build, so the next mechanism that registers something would have met
it too. The finding and the fix therefore belong at the **registrar**, not on a content file — which
is why the handler sits beside `RecipeRegistrar` and `flint_staff.yml` says nothing about reloads.

**The fix:** `io.papermc.paper.event.server.ServerResourcesReloadedEvent` re-runs `registerAll`.
Paper's own javadoc names the use — *"Intended for use to re-register custom recipes, advancements
that may be lost during a reload like this"* — and the event was **confirmed present on
paper-api 26.1.2.build.74-stable before it was chosen**, because R2 exists precisely because
`/reload confirm` was a remembered command that turned out not to be there.

Re-entry is safe for free: the registrar's unconditional remove-then-add was written so a key that
survived and a key that vanished take the same path, which is what makes the handler three lines
rather than a redesign.

##### WHY IT SHIPPED IN THIS SLICE, AND THE FIRST REASON IS NOT THE REAL ONE

"Do not ship a mechanism with a known defect" **does not settle it, and this slice is its own
counter-example** — it deliberately ships a documented gap: scorch is `kind: fire`, so the staff's
burn is vanilla-rated and does not credit the caster. If that principle decided things, this slice
would already be blocked by its own named debt.

**The real distinction is STATED versus SILENT.** Scorch's gap is visible: written in the content
file, filed above, explainable to a player who notices. The reload gap was invisible — no log, no
warning, the recipe simply gone.

**And the argument that actually settles it is about the alternative.** Not fixing meant a caveat
carried on every remaining gate row — *never run this after a `/reload`*:

> **A CAVEAT THAT MUST BE REMEMBERED ON EVERY ROW IS ONE THAT GETS FORGOTTEN.**

That is `continue`-inside-the-loop against `STATUS_SLOTS` — a set that *cannot* contain the close
slot beats a loop that remembers to skip it — and the memory version was watched to fail **in this
same week's work**: the stale-jar trap was disarmed by `set -e` happening to fire, not by anyone
remembering it was armed. A structural fix removes the thing to remember.

##### DO NOT ADD A CATALOGUE INVALIDATION — it is the reflexive move and it is wrong

`RecipeCatalogue` caches for the server's lifetime and holds key, tier and ingredients. **A
drop-and-re-add under the SAME key changes none of them**, and `RecipeCatalogue.resolve` re-checks
the live roster on every click regardless. Invalidating on reload would throw away a correct cache
to fix nothing. Written here because it is exactly what the next reader will reach for.

##### The number that confirmed the finding, and the one that would have refuted it

The re-registration logs a second `Custom recipes:` line, and its `replaced` is a cross-check rather
than decoration:

- **`0 replaced`** — `removeRecipe` found nothing, so the reload really had dropped the recipe.
  **This is what was observed**, so R2's finding is confirmed by a second, independent route.
- `1 replaced` would have meant the key was still present and R2's failure had some other cause —
  a contradiction to resolve before shipping, not a pass.

Worth keeping as a shape: a fix whose log line also re-tests the defect it fixes turns "did the fix
fire" into "did the fix fire, **and was the thing it fixes real**".

#### STANDING CONDITION: every scripted boot leaves a JVM holding the jar, so the stale-jar trap is ARMED BY DEFAULT

**Recorded 2026-09-03. This is not a quirk of one run — it is the state the dev loop is in after
every scripted boot, and it arms the trap CLAUDE.md's VERIFICATION section opens with.**

`echo stop | ./scripts/dev-server.sh` does not stop the server. **And the reason is not the one it
looks like.** The first note written about this said "`stop` never takes on non-tty stdin", which is
wrong: the command arrives fine. Read the log instead of reasoning about it and the real mechanism
is there, identically in all three boots of this slice:

```
[21:52:54 INFO]: Done (5.863s)! For help, type "help"
[21:52:54 ERROR]: Command exception: /stop
java.lang.NullPointerException: Cannot invoke "net.minecraft.server.level.ServerLevel.getGameRules()"
        because the return value of "net.minecraft.commands.CommandSourceStack.getLevel()" is null
        at net.minecraft.server.dedicated.DedicatedServer.handleConsoleInputs(DedicatedServer.java:622)
[21:52:54 INFO]: An unexpected error occurred trying to execute that command
```

A piped `stop` is delivered **instantly**, so it executes on the first tick that processes console
input — the same second the server finishes starting — when the console command source has no level
yet. It throws, the throw is swallowed as "an unexpected error", **and the server runs forever.**

So the chain, which holds after every scripted boot:

1. the stop is swallowed, so the JVM survives the script;
2. the surviving JVM holds `run/plugins/rpg-*.jar`;
3. the next `dev-server.sh` cannot `rm -f` it (`Device or resource busy`);
4. `set -e` aborts **before the deploy**, and the boot after that reads a STALE jar.

**Today step 4 saved a verification, by accident.** The R1 provenance re-run hit exactly this and
aborted. Nothing was designed to disarm the trap — `set -e` happened to fire. Had the deploy been
non-fatal, or had the script deployed before removing, the server would have booted the previous
build and printed a correct-looking R1 line for the wrong jar.

**Until the script changes, the manual discipline is: kill every `java.exe` and confirm the deployed
jar is not locked BEFORE booting, and compare `target` and deployed mtimes AFTER.** Both were done
for the R1 re-run (target `21:51:53`, deployed `21:52:48`), which is the only reason its number can
be trusted.

##### OWED: make `dev-server.sh` stop the server rather than hope

Not done in this slice — it is a dev-script change and this slice is about custom recipes — but
sized here so it is a decision rather than a rediscovery. Two candidate shapes, and the second is
better:

- **Delay the stop** (`(sleep 30; echo stop) | ...`). One line, and it fixes the NPE by letting the
  command land after the level exists. But it trades a hang for a guess about boot time, and a slow
  boot silently returns to the current behaviour — the failure mode is the same one, just rarer,
  which is worse than loud.
- **PID file plus an explicit kill.** `dev-server.sh` already `exec`s java as the last statement, so
  it would need to background it, write `$!` to `run/server.pid`, `wait`, and trap EXIT/INT to kill
  that PID. A `--stop` flag then kills by PID file, and the boot path can **refuse to start while a
  live PID file exists** rather than discovering the lock at `rm` time. That converts a silent
  precondition into a named refusal, which is the direction every other guard in this repo goes.

**Acceptance for either:** two consecutive scripted boots with no manual kill between them, and the
second one's deployed-jar mtime newer than its `target` mtime. That criterion is what today's run
had to be checked by hand.

#### VERIFY THE MUTATION AT THE ARTIFACT, AND THE **REVERT** AT THE ARTIFACT TOO

Two lessons from running gate row R4, and the second is the one that usually gets dropped.

**A marker you can only find in the source proves nothing about the thing that ran.** R4's mutation
was marked with a comment, and a comment **cannot survive compilation** — so `grep` over the source
would have confirmed only that the file was edited, never that the running jar had the change. Two
witnesses, on different substrates, are what settled it:

| substrate | witness |
|---|---|
| RUNTIME | two `R4 PASS n -- Custom recipes: ...` lines in the boot log |
| BUILD | the `R4 PASS 1` / `R4 PASS 2` **string constants** in the compiled `RpgPlugin.class`, and `javap -p -c` showing **two** `RecipeRegistrar.registerAll` invocations |

The log lines alone are weaker than they look: two lines could in principle come from one call
inside a loop. **The bytecode cannot.** A string constant survives compilation where a comment does
not, which is why the marker for a mutation should be something the compiler keeps.

**Then hold the UNDO to the same standard.** A revert is a change like any other, and "it went back"
is a claim, not an observation — but the discipline is almost always spent on the mutation and none
is left for the restore. R4's revert was checked four ways: restored from a scratchpad copy (never
`git checkout --`), `md5sum` identical to the pre-mutation file, `git status` clean, and — after a
fresh `./mvnw clean package` — **zero** `R4 PASS` strings and **one** `registerAll` invocation in
the REBUILT class, confirmed by a boot printing exactly one `Custom recipes:` line.

This sits beside the marker-count entries above for a reason: they are the same defect seen from
opposite ends. Those record a mutation believed without evidence it applied; this records the
symmetric risk of a revert believed without evidence it landed — and a stale mutation left in a
build is worse than one that never applied, because everything after it is measured through it.

#### A PROVENANCE RE-RUN ON A BOOT-WITNESSED ROW IS NOT BOOKKEEPING — IT IS THE CHECK

R1 was re-run only to attach an observation to a commit: the first reading came from a jar built
before the slice was committed, from a tree **asserted** rather than verified to match. Tidying.

**It caught a live defect** — the held jar and the aborted deploy above.

That is the general lesson, and it is sharper than "re-run things": **a row whose entire content is
a log line has no second signal.** R1 passes by printing
`Custom recipes: 1 registered, 0 replaced, 0 refused, of 1 authored`, and a stale jar prints exactly
that too, because the previous build was also correct. Nothing downstream could have caught it —
not the suite, which is green either way; not CI, which never boots; not the row itself, which had
already "passed". The only thing that distinguishes the two readings is **which build produced
them**, which is precisely what a provenance re-run establishes and nothing else does.

So on a boot-witnessed row, "which build was this observed on" is not metadata about the check. It
**is** the check.

#### No automated witness

| no automated witness | lives in | what goes wrong unseen | sole witness (row in `GATE-crafting.md`) |
|---|---|---|---|
| recipe REGISTRATION | `RecipeRegistrar` | nothing is on the roster; the staff is unobtainable | R1 |
| what `/reload` does to our recipes | the server | unknown, by admission — the code is correct either way | R2 (records the number) |
| the `replaced` counter itself | `RecipeRegistrar` | a dead counter makes R2's number meaningless | **R4** (the instrument's own control) |
| minting from a custom recipe | `commitCraft` | a plain stick for a flint | 7C |
| the Crafter refusing our recipe | `onCrafterCraft` | a redstone flint-to-stick machine | 7E |
| recipes surviving a `/reload` | `onResourcesReloaded` | every custom recipe silently gone until restart | R2 (the craft), R6 (the roster) |
| a duplicate recipe key | — | nothing: `byKey` is a Map, so it cannot occur. R5 struck IMPOSSIBLE | none needed |
| `fits` against the delivered item | `InventoryCraft.craft` | up to 64 weapons on the floor | 7F |
| WEAPON tier occupying the column | `SuggestionTiers` | the first ordinal was unreachable until now | Q15, Q16 |


### Crafting, Slice 5 (Quick Craft, first half) — what it created or exposed

- **THE GATE ROWS WERE NOT IN THE REPOSITORY, AND NOTHING FAILED WHEN THEY WERE NOT.** Q1-Q14 were
  written in the slice plan and in review chat, and two commits landed on the branch — one of them
  explicitly a docs commit naming the Q2 instrument — without either carrying them. `GATE-crafting.md`
  on the branch was **byte-identical to master's**, last touched by the slice 4 flip.

  **This is the corollary this file already records, recurring one slice after it was written:**
  *A WITNESS THAT IS NOT IN THE REPOSITORY IS NOT A WITNESS.* The plan's own verification step said
  "NEXT.md and GATE-crafting.md updated in the same commit that builds the rows"; the step did not
  run, and no check anywhere noticed. A verification step that is itself unverified is the same shape
  as the gate row it was written to protect.

  **What would actually catch it** is not more discipline: it is committing the rows in the SAME
  commit as the code they witness, so an empty gate diff is visible in the same review as the
  feature. Recorded rather than fixed, because a pre-commit hook checking "did GATE-crafting.md move"
  would fire on every non-gate commit and be disabled within a week.

- **"CONSUMED = INPUT MATRIX MINUS RESULTING MATRIX" IS INCOHERENT, and it was the reviewer's own
  prescription.** It is the obvious way to debit an inventory craft and it is wrong for exactly the
  case rows 12 and 12c exist for: a milk bucket does not DECREASE when a cake is made, it BECOMES an
  empty bucket. There is no per-slot quantity to subtract, and code that tries lands on either
  "three buckets vanished" or "three buckets appeared from nowhere".

  What holds instead, and is now in `commitCraft`'s javadoc:

  ```
  A       is exactly what left the inventory (what the assembly took, BY SLOT)
  R + O   is exactly what the engine says remains
  the player ends at   inventory - A + R + O + result
  ```

  **True without ever knowing which part of A was consumed and which was transformed.** Any
  formulation that needs to know is wrong for cake. The GRID gets this free — writing the resulting
  matrix over the slots the input came from IS `-A + R` in one operation — which is precisely why the
  asymmetry between the two callers is easy to miss. The reviewer's DIAGNOSIS was right; the
  mechanism prescribed would have shipped the cake bug on a new surface.

- **`RecipeChoice` CANNOT BE ENUMERATED TOTALLY, so the question was inverted.** Verified against the
  pinned jar: `getItemStack()` is DEPRECATED, and the three implementations expose their contents
  three different ways — `MaterialChoice.getChoices()`, `ExactChoice.getChoices()` (meta-sensitive),
  `ItemTypeChoice.itemTypes()`. A fourth, `PredicateRecipeChoice`, wraps an arbitrary lambda and is
  not enumerable by anybody.

  `test(ItemStack)` is on the interface, undeprecated, and total — and it is the question the craft
  itself will ask. So the adapter probes each ingredient slot with the player's own distinct stacks.
  It works because the two sides have opposite cardinality: the recipe's accepted set is unbounded,
  the inventory is a few dozen stacks. **Ask the small side.** "Enumerate `MaterialChoice`, skip the
  rest" was rejected as `ANY_BUT_SHIELD` in a fourth costume.

- **STATE THE MECHANISM YOU VERIFIED; DEFER THE MEMBERSHIP YOU CANNOT.** The general rule the
  firework correction below produced, and it outlives the specific claim.

  Two different things get written in the same sentence: *why a boundary exists*, and *what falls on
  each side of it*. `ComplexRecipe` declaring no methods is checkable in the pinned jar in ten
  seconds. **Which vanilla recipes are registered as complex is server RUNTIME data that cannot be
  read from the API jar at all** — not by the reviewer, not by the builder.

  So: **the code explains why the boundary exists; the gate row discovers where it falls.** A javadoc
  that names items is asserting something it cannot check, and it will be repeated by everyone who
  reads it. A row that observes both sides is a measurement.

  That resolution is correct **regardless of who was right about rockets**, which is what makes it a
  rule rather than a patch — and it is the same shape as the arc's other standing answer to
  unverifiable confidence: `getMaxDurability()` became a predicate rather than a direct call so the
  walk stayed testable and the Bukkit question stayed at the boot.

- **THIS ARC IS VIGILANT ABOUT TWO COPIES OF CODE AND CASUAL ABOUT TWO COPIES OF AN EXPLANATION.**
  The firework claim was wrong in **FIVE places**. The review that caught it said two. Nobody had
  counted, including the person correcting it.

  **The mechanism, and it is worth more than the correction:** when a claim is restated in a new
  file, **it is copied from the previous restatement, not re-derived from the source.** Slice 1
  wrote "firework rockets, firework stars and dye tables"; slice 3 quoted slice 1; slice 5 quoted
  slice 3 twice and the gate row once. Each author believed they were repeating something already
  checked. **Nobody re-opened the jar, because the sentence already existed and looked settled.**

  This project has an extraction rule, an exhaustive-switch rule and a single-source-of-truth rule
  for CODE — `GearItems` exists precisely because an if-chain had five copies coming. **Prose has no
  compiler**, no `md5sum` check like the one that verified those method bodies were byte-identical
  before they moved, and no test that reddens. So duplicated explanation is the one duplication this
  repo does not defend against at all.

  **What to actually do about it**, since "be careful" is not a mechanism:
  - When restating a claim in a new file, **cite where it was verified** — "verified against the
    pinned jar", with the check — rather than restating the conclusion alone. A citation is a
    pointer back to a source; a conclusion is a copy.
  - When a claim turns out wrong, **grep for it before correcting one site.** The count is the
    finding. `grep -rn 'firework' --include=*.java --include=*.md .` took one command and found
    three sites nobody had named.

- **RULE 1 APPLIES TO THE GREP YOU VERIFY WITH, NOT ONLY TO THE CODE YOU WRITE.** The same defect
  from the other side, found when the arrow was deleted.

  The enumeration used was `ARROW_SLOT|ARROW\b` — the identifier and the shouted word. It reported
  five sites and the true count was **seven**: two more mentions existed in ordinary lowercase prose,
  where the arrow was a POSITIONAL LANDMARK rather than a named thing — *"one cell right of the
  arrow"*, *"the column between the grid and the arrow"*. Neither pattern could ever have matched
  them. Measured afterwards: `ARROW_SLOT|ARROW\b` finds 3 hits in the tree; `grep -i arrow` finds 60.

  **This is "enumerate the axis, not the cases you currently have" pointed at the search itself.**
  A grep built from the identifier enumerates the cases the identifier happens to cover; the AXIS is
  *every mention of the thing* — any casing, any part of speech, and including the ones where it is
  used to locate something else rather than to name itself. The identifier-shaped grep is a denylist
  wearing a different hat: it admits everything nobody thought to spell out.

  **And a diagram is neither.** The same deletion left an ASCII layout map drawing the close button
  at a slot it had moved away from, in a picture whose own legend gave the right number. No grep for
  slot numbers or row words reaches a glyph in a drawing. **The check for a diagram is reading it
  against the code, cell by cell** — a map looks like documentation and is actually a claim.

  Practically: when removing or moving a thing, `grep -i` its plain-English name as well as its
  identifier, and open every diagram that draws it.

- **COMPLEX RECIPES ARE PERMANENTLY ABSENT FROM SUGGESTIONS — but "firework rockets are absent" was
  TOO BROAD, and it was written in five places before anyone checked.** `ComplexRecipe` is a bare
  marker interface (verified from the pinned jar: it declares nothing), so a recipe registered that
  way exposes no ingredients and cannot be counted. **That is the mechanism, and it is all that was
  ever verifiable.**

  What was NOT verifiable, and was asserted anyway: WHICH vanilla recipes those are. The basic
  one-flight firework rocket is an ordinary shapeless recipe and enumerates perfectly well; only the
  customizable multi-star variants are complex. The blanket claim reached `GATE-crafting.md`'s Q10
  row, `RecipeProbe`'s class javadoc, two places in `CraftingMenu`'s, and this file — because it was
  inherited from slice 1's wording and repeated rather than re-checked.

  **Which recipes are complex is server RUNTIME data and cannot be read from the API jar at all.** So
  every site now states the mechanism, and the LIST belongs to the gate, where it is observed rather
  than asserted. **Q10 is a better row for it**: it checks both halves — the basic rocket appears and
  crafts, the multi-star one does not appear and still crafts in the grid — so it says where the
  boundary actually falls instead of claiming a blanket absence.

  Whatever falls on the complex side still crafts in the GRID through the server's matcher. **The
  grid remains the complete surface**; Quick Craft is a convenience over the enumerable subset.
  Hand-implementing them is exactly the mistake `CraftingMenu`'s class javadoc records the previous
  project making.

- **THE SUGGESTION ORDERING IS A SIX-POSITION TIER AXIS, MINTED FIRST**: `WEAPON → ACCESSORY → TOOL
  → ARMOR → MATERIAL → VANILLA`, then most-craftable, then key. A shield is ACCESSORY — a display
  category deliberately wider than the gear kind, so a future accessory kind joins it rather than
  forcing a seventh position.

  **Core owns the axis and the sort; paper decides which one a recipe IS.** Classifying needs
  `CraftResultIndex` and the sealed `GearDefinition`, both Bukkit-side, so `SuggestionTiers.of` does
  it with an **exhaustive switch and no default arm** — a fifth gear kind is a compile error until
  someone ranks it, rather than silently sorting below vanilla planks. Same inversion as the recipe
  probe: core is told, and sorts.

  **Two consequences, stated rather than discovered:**
  - **Nothing maps to `MATERIAL` today.** No source of truth says "this vanilla item is an
    intermediate rather than a product", so `SuggestionTiers` cannot return it. It is a held-open
    position with **no test and no gate row** — exercised only as a sort position in
    `theSixTiersSortInDeclarationOrder`. Recorded on the constant itself.
  - **A player may never see a `VANILLA` suggestion at all.** 24 armor + 5 tools + 1 shield claim a
    `craft_result` against NINE slots, so common materials can fill the column with gear. That is the
    direction asked for, and it is written down because *"sticks and torches vanished from the
    crafting helper"* is exactly what it will look like from outside.

- **THE BULK LOOP DROPPED ITEMS ON THE FLOOR, AND IT WAS ALREADY SHIPPED.** `craftRepeatedly` is the
  SHARED loop: 64 shields into a filling inventory has always ended with `MenuSafety.give` dropping
  the remainder at the player's feet — a pile of entities, a lag vector, and the same message 64
  times. Quick Craft did not introduce it; it would have inherited it.

  Both loops now ask `MenuSafety.fits` BEFORE each pass and stop cleanly, saying how many were made.
  Nothing reaches the ground for any item, whatever its stack size. `MAX_BULK_CRAFTS` stays exactly
  what its javadoc says — the runaway guard, not a batch size.

  **Lowering `MAX_BULK_CRAFTS` was rejected:** one number cannot serve a stackable output (64 sticks
  is fine) and a non-stackable one, and it would leave the drop path intact whenever the inventory is
  nearly full. **This re-gates the GRID rows — S1, S2, 13, N5b — not only Q6.**

- **NEW ENTRIES, ALL GATE-ONLY.** Same cause as every crafting slice: `CraftingMenu` cannot be
  constructed without a server.

  | no automated witness | lives in | what goes wrong unseen | sole witness (row in GATE-crafting.md) |
  |---|---|---|---|
  | the inventory DEBIT | `CraftingMenu.debit` | the wrong stacks are reduced when materials span several | **row Q13 ONLY** |
  | craft-before-debit ordering | `CraftingMenu.craftOneFromInventory` | a refusal takes the ingredients anyway — theft, on the least-tested path | **row Q8 ONLY** |
  | the remainder give | `CraftingMenu.craftOneFromInventory` | a cake's three empty buckets are destroyed | **row Q14 ONLY** |
  | `isGear` on the probe | `RecipeProbe.groupsOf` | a minted item is counted as a material and consumed | **row Q7 ONLY** |
  | the bulk re-probe | `CraftingMenu.craftFromSuggestion` | the roster is walked 64 times per shift-click | **row Q6 ONLY** |
  | the recompute cadence | `CraftingMenu.refreshSuggestions` | a full walk on every grid change | **row Q2 ONLY** |

  **Rows 12 and 12c matter MORE this slice, not less.** On the grid the resulting matrix and the
  overflow have different destinations and those two rows witness them separately. On the inventory
  path **both collapse into "give it to the player"**, so no Q row can tell the two calls apart —
  nothing new would notice if `getOverflowItems` stopped being read.

- **Q2 RAN AND PASSED: 298 MICROSECONDS against a 50000-microsecond tick.** 0.6% of a tick, roughly
  168x headroom, operator-confirmed 2026-09-02. **The cadence stands** — the recompute trigger is
  unchanged and Q5/Q6 are unaffected. The named risk that the tier sort landed in the path Q2
  measures is closed.

  **The instrument was removed in the same commit that recorded the number**, per
  `PLAN-1b-swing-listener.md:134`. Worth stating why that mattered here: **a PASSING row is exactly
  when "remove before merge" gets skipped** — a failure forces a decision, a pass invites moving on.

  **The counts did not reach the record**, and they are not decoration: the probe costs
  `distinct stacks × recipes`, so 298µs from three stacks and from thirty are different measurements
  wearing the same number. Recorded as missing rather than guessed. **And the honest scope**: cold
  first recompute, one inventory, one player, a test server. Not a load test, and nothing at this
  headroom depends on it being one.

- **STILL OWED:** the rest of the gate — Q1 and Q3-Q17 are unrun. And the second half: the browser,
  navigate-only, with pure page math in `core/`.
- **BOTH DELIVERED, slice 6, 2026-09-03.** The gate rows are **Q24-Q31**, numbered from the real
  high-water mark rather than from the placeholder's Q11/Q12. **And the browser is NOT
  navigate-only** -- it crafts, which is why **Q12 is SUPERSEDED rather than enabled**: the row's
  observable stopped being the correct one when the gesture changed.

---

### Q24's MEASUREMENTS — CLOSED 2026-09-04. Q2's ARE STILL OWED.

**Recorded from the slice-7 gate run**, which opened the browser three times across three server
lifetimes. The instrument was deleted in the same commit, as the rule below requires.

| measurement | value |
|---|---|
| entries | **1095** |
| catalogue build time | **7137 µs / 7994 µs / 11159 µs** — all three runs |
| unkeyed skipped | **0** |
| not fully listable — **Q29's evidence** | **0** |
| mutation 8: stageable, or unrunnable with a reason | **STILL OWED** |

**THE SPREAD IS PART OF THE MEASUREMENT, NOT NOISE AROUND IT.** 7.1 ms to 11.2 ms is a **57 %
spread across three runs**. A single reading would have been written down as "7 ms" and would have
been misleading — and the entry this one replaced was itself a number recorded without what it
means. *Do not create the second instance while closing the first.*

**WHAT IT MEANS AGAINST Q2, which is why this note exists at all.** The two figures measure
different things and were always at risk of being confused:

| | build | of a 50 ms tick |
|---|---|---|
| Q2 — the suggestion probe | **298 µs** | 0.6 % (168x headroom) |
| Q24 — the catalogue walk | **7137–11159 µs** | **14–22 %** |

The catalogue is **24x to 37x** the probe, which is exactly what *"no early bail"* predicted: the
probe stops once it has three cells, and the walk cannot stop at all. It is paid **once per server
lifetime**, inside a player's click.

**"No perceptible stall" HOLDS — with far less headroom than Q2's.** A fifth of a tick is not a
stutter, but it is not 0.6 % either, and a row that recorded only "passed" would have hidden the
difference. If the roster grows or the walk gains work, this is the figure that moves first.

**`Q2`'s craftable / probed / distinct-stack counts from SLICE 5 REMAIN OWED**, and are **not**
recoverable from the gate run: no instrument for them exists in the code and no such line appears in
any server log. Closing them needs a new instrument, which is a change, not a reading. **Better an
open debt than one that quietly looks closed** — that is the whole reason this page exists rather
than a report.

> **THE INSTRUMENT SHIPPED, DELIBERATELY, and the reasoning was in `RecipeCatalogue` and the gate.**
> *No number, no deletion* — the rule *"delete it in the commit that records the number"* protects
> the **number**, not the deletion, and deleting it first would have made an only-once measurement
> permanently unrecoverable rather than merely unrecorded. That rule paid off exactly as written:
> the figures above were read out of logs from a gate run nobody was thinking about measurement
> during, a slice after they were owed.

> **AND IT CORRECTED A NUMBER THE REPO HAD ASSERTED FIVE TIMES.** The roster was written down as
> **1214** in `GATE-crafting.md` (Q11's rewording), twice in this file, and — found only by
> enumerating rather than fixing the cited sites — twice more in Java: `RecipeBrowserMenu` and
> `RecipeCatalogue`. It was an estimate that predated the catalogue filtering to keyed
> shaped/shapeless recipes, and it was never re-read once a real number existed. **The observation
> outranks the prose**, and for once the correction arrived *with* the observation rather than a
> slice later. It also makes Q11's own example work: 1214 gives a last page of 44 out of 45, which
> barely illustrates "a genuinely short last page"; the measured 1095 gives 15.
---

### OWED: NOTHING IN THE REPO CAN DETECT THE NEXT ICON COLLISION

**Found 2026-09-03 by the barrier/barrier clash on the empty browser** — the close button and the
empty-state notice, same material, one screen, distinguished only by name and position.

**Neither of the two rows that touch icons can see it.** Q33 pins the empty-state notice's material;
Q35 pins the recipe book's. **Neither asks whether any two icons on one screen share one**, and no
unit test can — `MenuIcons.icon`, `close` and `filler` need a live server, which was verified by
grepping `paper/src/test` rather than assumed.

**The collision came from two individually-correct decisions meeting.** Barrier was right for the
empty state when it was chosen, because the browser had no close button yet; barrier was right for
the close button; and nothing connects the two changes. **An icon choice is only unique with respect
to the screen as it exists on the day it is made** — so the same is available to a third, and the
next one may not be harmless.

**THE CHEAP SHAPE, and it makes this a unit test instead of a gate row nobody thought to write:**

> **Let the layout classes own the chrome MATERIALS beside the slot constants** — the way
> `CraftingMenuLayout` already owns *where*. `RecipeBrowserLayout` would declare `CLOSE_MATERIAL`,
> `EMPTY_STATE_MATERIAL`, `PREV_MATERIAL`, `PAGE_MATERIAL`, `BACK_MATERIAL` next to `CLOSE_SLOT`,
> `EMPTY_STATE_SLOT` and the rest.
>
> **Screen-wide uniqueness then becomes an assertion over a set**, in the same file and the same test
> that already assert no slot is both an entry and a control — `RecipeBrowserLayoutTest` has that
> shape already. `Material` is a plain enum and constructs headless, so this is testable where the
> icons themselves are not.

**Not done in slice 6**, deliberately: it touches `CraftingMenu`, `EnchantMenu`, `RecipeBrowserMenu`
and two layout classes, and slice 6 has already reversed its own premise once. **Do it when something
else already has those files open** — the same disposition the `core/weapon` debt and the duplicate
`isEmpty` take. Until then the guard is that this entry exists.

---

### THE FOURTH LOWER-BOUND COUNT — AND THE FIRST ONE INSIDE A **REVIEW**

**2026-09-03.** The slice 6 gate was named before the run, as the rule requires, and the set held
**ten** browser rows. The section holds **eleven**. `Q34` — armor sorts head, chest, legs, feet — was
missing, and the tick-through page published for the operator carried the omission through. Caught in
review; the gate then ran 32 of 32.

**HOW THE SET GOT SHORT.** It was assembled by extending the **re-run half twice** for the UI changes
and treating the **new half as settled**. Q34 had landed in `8bec9e4` alongside Q32 and Q33, in the
window between those two extensions.

**AND THE REVIEWER'S CHECK HAD THE SAME SHAPE AS THE MISTAKE — this is the part worth keeping.** It
was a grep for the rows *expected to have been added*:

```
**Q(2[569]|3[23])**
```

rather than for **the axis: every live `Q` row in the section**. Q34 matched neither the assembling
pattern nor the reviewing one. **Two independent checks, both enumerating the cases they already had
in mind, and therefore both blind in exactly the same place.**

> **RULE 1 APPLIES TO THE GREP YOU REVIEW WITH, NOT ONLY THE ONE YOU VERIFY WITH.** *Enumerate the
> axis, not the cases you currently have.* The rules section states this for gates, switch statements
> and denylists; it is just as true of the pattern you check someone's work with, and a review grep
> feels like verification while being another hand count.

**This is the FOURTH instance this session of a count obtained by looking being reported as a total,
and the first inside a review rather than a sweep:**

| reported | actual | how the number was obtained |
|---|---|---|
| five arrow references | seven | a grep shaped by the identifier |
| two places carrying the Q10 claim | five | a hand search of the likely spots |
| two orphaned javadocs | five | the files that slice had open |
| **ten rows in the gate set** | **eleven** | **a grep for the rows expected to be there** |

**WHY IT WAS CATCHABLE AT ALL, and this is the load-bearing half:** naming the set before the run did
not make it **complete** — it made it **RECOVERABLE**. A set named at report time would have been ten
rows, self-consistent, and would have read as *31 of 31* for ever with nothing to compare against.
**The rule added at `aee4fe1` did not prevent this defect; it is the only reason the defect could be
found.** That is a fair thing to want from a process rule and worth separating from prevention.

---

### A WORKING READOUT WAS WEARING THE PLACEHOLDER'S CLOTHES, AND THE GATE ROW WOULD HAVE PASSED ON IT

**2026-09-03.** The recipe browser's empty state was built with `MenuIcons.placeholder`, so a player
with an empty inventory saw:

```
Nothing you can make right now
Not implemented yet.
materials for any recipe
```

**It announced the feature was MISSING while the feature was working correctly and had measured
zero.**

> **This is `placeholder`'s own javadoc warning, inverted.** That method exists because *"a readout
> showing 0% when nothing is counted is indistinguishable from a working readout that measured
> zero."* Here the working readout that measured zero **was built out of the placeholder** — so it
> did not merely fail to distinguish itself, it actively claimed the opposite.
>
> **The rule that falls out: reaching for `placeholder` is a CLAIM THAT SOMETHING IS NOT BUILT.** A
> surface that correctly measured nothing needs `icon`, and needs to say so in its name.

**AND GATE ROW Q33 WOULD HAVE PASSED ON IT.** The row's expected text named the notice — *"an
explicit 'Nothing you can make right now' notice"* — and said nothing about its lore. An operator
would have hovered, read the name, ticked a **SOLE WITNESS**, and left the screen saying the feature
was unimplemented. **A row that cannot fail on the defect in front of it is not a witness.** Tightened
to require the name *and the absence of lore*, explicitly naming "Not implemented yet." as the thing
that must not be there.

**The aggravating detail:** `MenuIcons.icon`, `close` and `filler` have **no unit test at all** —
they need a live server — which was verified rather than assumed (`grep` for them across
`paper/src/test` returns nothing). So that row's wording is not one witness among several. It is the
only one.

---

### `MenuIcons.placeholder` IS UNUSED AGAIN — SECOND GRADUATION, DIFFERENT SHAPE, AND IT IS KEPT

Its javadoc carried *"Currently unused, and kept on purpose"* from the first graduation. **That
sentence was FALSE while it was written down**, in the interval where the browser button and the
empty state were both using it — a small instance of the same drift this file keeps recording, and
one nothing would have reported.

**The two graduations are not the same event, and both are worth having:**

| # | what happened | the pattern |
|---|---|---|
| 1 | the enchant bookshelf slot became a **readout by gaining a SCALE** | *"0/30"* reads as a measurement where a bare *"0%"* could not. **Worth copying** |
| 2 | the browser button became a **real feature**; and separately a **working readout that had been wearing the placeholder's clothes** got its own icon | the second half **shipped a wrong message** — see the entry above |

**DECIDED, WITH A DATE: KEPT.** Zero consumers twice is a fair argument for deletion and it was
weighed rather than waved past. It stays because `MenuIcons` is the reusable base — `Menu`,
`MenuRouting` and `MenuSafety` all landed with no consumer at all — and because the anvil,
class-select and stat screens are each still ahead and will want the rule it encodes.

> **The condition for reversing that, so "kept for future use" cannot run forever:** if a THIRD
> graduation arrives and none of those three screens has been built, delete it. At that point the
> prediction has been wrong twice.

---

### THE CLOSE BUTTON LOST ITS LORE, AND THE ARGUMENT FOR THE LORE WAS KEPT

`MenuIcons.close()` is name-only now. Its javadoc used to argue for the line it carried — *"returning
the weapon is the part a player standing there holding something valuable actually wants to know"* —
and **that argument was sound**, so it is quoted and answered rather than deleted with the line.
Deleting it would lose why the line existed, and the next person to think "the close button should
explain itself" would rediscover it from scratch.

**Two things make the loss small rather than free, and neither was obvious:**

- **The behaviour was never the BUTTON's.** `Menu.returnEverything` runs on every close — Esc, death,
  disconnect, shutdown — so lore on the button implied the return was a property of clicking it.
  **Gate row 16 closes with Esc precisely because it is not.**
- **It is ONE button on TWO screens.** The enchant table is where *"Returns your weapon"* was most
  accurate, and keeping it there was **considered and rejected**: it would mean two close buttons,
  which is the drift `MenuIcons`' class javadoc exists to stop. One slightly plainer button beats two
  that are subtly different.

---

### TWO BARRIERS ON ONE SCREEN — ACCEPTED, DELIBERATELY, AND THE GATE ROW PAYS FOR IT

An empty browser shows the close button **and** the empty-state notice — both `BARRIER`, same
screen, distinguished only by name and position. Clicking the wrong one is harmless (`Menu` cancels
first and neither is an input slot), so this was never a defect; it is a distinction that **requires
a hover to perceive**.

**It was changed to `STRUCTURE_VOID` and then changed back on operator instruction, 2026-09-03.**
Barrier is this plugin's "nothing here" icon and reads that way at a glance; the collision costs a
hover in one state. **Recorded so the duplication reads as a decision someone took, rather than as an
icon nobody noticed was already in use** — which is exactly what it would look like otherwise, and
the reading a future reviewer would be right to have.

> **THE COST LANDED ON THE GATE, WHICH IS WHERE A REVERSAL'S COST USUALLY SHOWS UP.** Q33 hovers the
> empty-state notice and checks it has a name and **no lore**. With two barriers on screen, the close
> button *also* answers that description — a `BARRIER`, named, no lore — so an operator hovering the
> wrong one gets **exactly what the row is looking for and ticks it**. The row now names **slot 22**
> and warns about the second barrier.
>
> That is the second time in two changes that Q33 could have passed without observing the thing it
> witnesses. **A sole-witness row degrades quietly when the screen around it changes**, and neither
> degradation came from editing the row.

**And worth recording about the ORIGINAL collision, separately from the decision:** barrier was
chosen for the empty state **before the browser had its own close button**, and was correct then. The
collision arrived with a later, unrelated change, and nothing connects the two. That is a general
shape rather than a slip: **an icon choice is only unique with respect to the screen as it exists on
the day it is made.**

---

### THE BROWSER'S FOUNDING PREMISE WAS REVERSED, DELIBERATELY, AFTER IT WAS BUILT

**2026-09-03, operator decision.** The recipe browser now shows **only what the player can craft
right now**. It was built to page through the whole 1095-recipe roster.

**The argument it was built on was mine, and it was correct for the brief it was made under:**

> *"A browser paging through `Result.suggestions` is a taller suggestion column, not a browser."*

That holds for a browser whose purpose is to make the Q16 squeeze reachable — the three-cell column
fills with gear, so armor and every vanilla recipe are unreachable from the crafting screen, and
something has to answer for them. **It is not the brief any more.** The purpose is *"an easy way to
craft quickly"*, and against that purpose 1095 entries is clutter. The reversal is recorded here, and
the old reasoning is replaced at each of its call sites rather than left sitting in the files looking
live — `RecipeCatalogue`'s "why this is not RecipeProbe" section said the opposite in so many words.

**WHAT SURVIVED UNCHANGED, which is most of it:**

- the **static catalogue** — built lazily on first open, cached for the server lifetime. Still the
  right shared structure, because roster membership, key, tier, body slot and ingredient shape do not
  depend on any player. The browser filters it per player at open. **Gate row Q24 is untouched.**
- the **tier ordering**, and its row.

**WHAT IT COST, AND THE COST IS THE PART WORTH WRITING DOWN:**

> **ARMOR THE PLAYER CANNOT YET AFFORD IS NOW INVISIBLE EVERYWHERE.** Squeezed out of the column by
> tier order (Q16), hidden in the browser by the filter. **No surface answers "what does a netherite
> helmet need?"**
>
> That is a consequence of the product decision, **not a defect** — and it is written into
> `RecipeCatalogue`, `RecipeBrowserMenu` and the gate, at the three places someone would meet it,
> rather than left to arrive as a complaint nobody can explain. If it ever needs answering, the
> answer is a **third surface**, a lookup, not a filter flag on this one.

**WHAT IT RESOLVED — the inert-entry apparatus became unnecessary and came out.**

It existed because the browser claimed to show EVERYTHING, so omitting a recipe that the vanilla grid
*can* craft would have been a **false absence** — Q10's mistake in UI form. Under *"what you can
craft here, right now"* a multi-star firework is absent **honestly**: it genuinely cannot be crafted
here. Gone with it: the `inert` flag, the red pane, and the "Cannot be crafted here / use the
crafting grid" lore. Gate rows **Q30 and Q31 are struck as SUPERSEDED**, exactly as Q12 was — the
contract changed, so their observables stopped being correct. Not deleted, not wrong.

> **THE `not fully listable` BOOT COUNT IS KEPT, and this is the distinction that saved it.** It was
> Q31's runnability evidence; it is now **Q29's**. A listed recipe whose ingredients cannot be fully
> enumerated still needs *"(accepts more than can be listed)"*. **That hazard was always about LORE,
> never about craftability** — which is exactly why it survived a reversal that deleted everything
> around it. A count kept for the wrong reason would have been deleted with the apparatus.

**WHAT CHANGED SHAPE, and one of these would have been an unrunnable row:**

- **Q26 was "an entry you cannot afford".** That is now **impossible by construction on a fresh
  view** — every listed entry was affordable when the list was built. Rewritten as a **staleness**
  row: spend the materials elsewhere, then click the entry the list still shows. Q8's shape on the
  third surface. Kept, because *"refuses cleanly, nothing debited"* is still what must hold and the
  debit-before-craft hazard has not moved.
- **Q25 stopped being "the row Q16 hands off to."** Reworded to check tier order among what IS
  craftable.

**TWO NEW FAILURE MODES THE FILTER CREATES**, both now rows, both sole witnesses:

| mode | why it is new | row |
|---|---|---|
| **the list SHRINKS under the player** | crafting removes entries, so the last page can cease to exist. `PageMath.clampPage` existed already; the defect was calling it only on NAVIGATION — the obvious moment, and **not** the one that changes the page count | **Q32** |
| **an empty inventory means an empty browser** | which reads as broken. `MenuIcons.placeholder`'s argument exactly: a surface showing nothing because it MEASURED nothing must be distinguishable from one that is broken | **Q33** |

**COST NOTE, and getting it wrong is the confusion Q24 was written to prevent.** Filtering scores the
**whole roster per open**, not 45 entries per page. That is the walk **Q2** measured at **298µs**
against a 50000µs tick. **Q24 is a different walk** — the catalogue BUILD, paid once per server. Both
numbers are cited at their own call sites, and neither is allowed to stand in for the other.

---

### ARMOR SORTS HEAD, CHEST, LEGS, FEET — AND THE ENUM GAINED A CONSUMER WITHOUT GAINING ITS RULE

`ArmorSlot` was **already** declared `HEAD, CHEST, LEGS, FEET`, so the required order is its
declaration order and no new constant was needed. `CraftOrder.WITHIN_TIER` sorts armor by
`ordinal()`.

**Its javadoc said "a closed, UNORDERED enum".** By the time anything sorted by it, that was false —
**rule 3, caught late**. Nothing failed: the old text was not wrong about anything the code did on
the day it was written; it simply stopped describing the enum the moment something depended on the
order, and no compiler, test or reader reports that. `SuggestionTier` carries the same warning, and
gained *its* second consumer in this same arc. **An enum that acquires an ordering acquires a rule,
and the rule lives in its javadoc or nowhere.**

**ONE COMPARATOR, THREE CONSUMERS — and "one comparator" does NOT mean "one total order".**

The column ranks `tier -> COUNT -> tiebreak`; the browser and the catalogue rank
`tier -> tiebreak`. Those are genuinely different orders and must stay different: three cells should
spend themselves on what the player can make most of, while a browser that led with count would
reshuffle its whole list every time the player crafted one item. **What must not be duplicated is the
tiebreak underneath both**, so `CraftOrder` is an interface implemented by `CraftCount.Craftable` and
by `RecipeCatalogue.Entry`, and the shared piece is `WITHIN_TIER`.

> **WHY THIS WAS WORTH THE INTERFACE.** Armor is squeezed out of the three-cell column (Q16), so a
> column that ordered armor differently from the browser **would look identical in play** for as long
> as the column stayed at three cells. Two orderings that agree today, written in two places, with no
> observable that can tell them apart — the same shape as the craft path before `InventoryCraft`.
> Closed the same way.

**AND A MUTATION CAUGHT THE TEST THAT WAS SUPPOSED TO CATCH IT.** The column's armor test first used
`diamond_helmet` (HEAD) and `leather_boots` (FEET) — where `d < l` alphabetically **and** head
precedes feet, so a key-only tiebreak and the body-slot tiebreak give the **same answer**. The
mutation that swaps the shared tiebreak for a key-only one **ran green** against it. Rewritten with
`z_helmet` (HEAD) and `a_boots` (FEET), where the two orders disagree completely; the mutation then
reddened. **A test that cannot fail is worth nothing however green** — and the second time this
session that a mutation has found a defect in its own test rather than in the code.

---

### FIVE ORPHANED JAVADOCS, INVISIBLE BECAUSE JAVADOC IGNORES THEM SILENTLY

**Found 2026-09-03 while moving code, not while looking for them.** Java attaches a doc comment to
the declaration that *immediately* follows it. Two doc comments in a row means the **first one is
attached to nothing and is discarded** — no warning from `javac`, none from the IDE, and the text
still reads perfectly well in the source file.

**TWO were found BY HAND while moving code. A SWEEP THEN FOUND THREE MORE**, in files this slice does
not otherwise touch. That is the finding worth keeping: the hand-found pair had already been written
up as "two", and **two was never a count of the defect — it was the reach of where I happened to be
looking.** A defect that is invisible to reading is also invisible to *incidental* discovery, so the
first number any hand-search produces is a lower bound and should be reported as one.

| file | the orphan | it was sitting above | fix |
|---|---|---|---|
| `CraftingMenu` | *"Which gear definition, if any, this vanilla result should be replaced by"* — 16 lines, incl. the belt-and-braces durability reasoning | `identityOf`, which has its own javadoc | moved with `claimFor` into `InventoryCraft` |
| `RecipeProbe` | *"The player's carried items, grouped by `isSimilar`…"* — the whole grouping rationale | `probeOne`, which has its own javadoc | reattached to `groupsOf` |
| `BukkitCombatant` | the whole `snapshot` freeze rationale, incl. the region-ownership argument | a SECOND doc comment on the **same** method | **merged** — this pair describes one declaration, so it does not move |
| `RpgPlugin` | *"Warns, never disables the plugin. Fail-soft…"* | `entityType`, which it plainly does not describe | moved to `validateContent`, which had none |
| `GearClassLabel` | *"The whole noun phrase, for naming the gear an enchant is SITTING ON"* | `describeEnchant` | moved to `describe`, declared BELOW it and documented nowhere |

**The three fixes are not one shape.** One pair merges, two move, and deciding which needs reading
what the text actually describes — `RpgPlugin`'s orphan is about validation and was sitting above an
entity-type lookup. A blind "delete the first of two" would have destroyed three explanations.

**What makes this worth an entry rather than a tidy-up:** the failure is *invisible in the place you
would look*. Reading the source, the comment sits directly above the thing it describes and looks
correct; only generated javadoc, or a careful reader counting `*/` against `{`, shows the loss. It is
the documentation equivalent of a check that did not run — **the text is there, and it does nothing.**

> **THIS IS NOT THE STALE-PROSE FAMILY, AND FILING IT THERE WOULD LOSE THE POINT.** Stale prose —
> `37c0ea7`'s "nine suggestions in row 4", `SuggestionTier`'s "NINE suggestion slots", the withdrawn
> browser note — **was TRUE when it was written** and aged out from under itself. The remedy is to
> re-read it when the thing it describes moves, and the failure is a *lapsed* claim.
>
> **An orphaned doc comment was NEVER WIRED UP.** It was wrong from the keystroke: present in the
> file, absent from the generated docs, invisible to the compiler, invisible to every test, and
> invisible to any `grep` for its content — because the content is right there, spelled correctly,
> next to the method it describes. **There is no moment at which re-reading it would help**, which is
> what makes it a different defect and not a variant.
>
> The two families do share one property, and it is the one this file keeps circling: **the artefact
> looks correct in the place you would look.** Stale prose reads true because it once was; an orphan
> reads attached because it is adjacent. Neither is caught by reading. Both need a mechanical check —
> and for orphans that check is trivial, had never been run before, and found FIVE on its first run.

Found because a class-extraction moved `claimFor` and its doc comment had to be located to move with
it. It would not have been found by reading.

**The sweep, so this is a measurement rather than an anecdote** — a `*/` line immediately followed by
a `/**` line. **Across EVERY source file, not one package**: scoping the first run to `menu/` is
exactly what made the count "two" when it was five.

```bash
find core/src storage/src paper/src -name '*.java' -type f -exec \
  awk 'prev ~ /^[[:space:]]*\*\/$/ && $0 ~ /^[[:space:]]*\/\*\*$/ {print FILENAME":"FNR} {prev=$0}' {} +
```

Two details in that one line, both of which were wrong first:

- **`FNR`, not `NR`.** With `-exec ... {} +` awk receives many files in ONE invocation, so `NR` keeps
  counting across file boundaries. The first run of the corrected sweep reported an orphan at
  `CraftCount.java: 7694` — in a file of 250 lines. **The FILE was right and the LINE was nonsense**,
  which is the worst kind of wrong for a tool whose output you are about to go and look at.
- **`[[:space:]]*` for the indentation**, not a fixed four or five spaces: the original hardcoded the
  depth of a top-level member and would have missed every orphan on a nested class or record.

**Five before, ZERO after**, across 378 source files.

> **AND THE SWEEP CARRIES A POSITIVE CONTROL, because it is a DISCOVERY and not an assertion.** A
> scan reporting zero is indistinguishable from a scan that cannot see — CLAUDE.md:104, which this
> file has now recorded instances of three separate times. So an orphan is injected and the sweep is
> required to report it before "zero" is allowed to mean anything:
>
> ```
>   control marker (must be 1): 1
>   sweep sees: 1  (must be 1)
>   restored, markers left: 0
> ```
>
> Without that, "zero orphans" and "the awk pattern has a typo" are the same output.

---

### THE MOVE'S FAITHFULNESS WAS PROVED BY DIFF, AND THE SUITE COULD NOT HAVE PROVED IT

**Slice 6 moved `commitCraft`, `craftOneFromInventory`, `debit`, `claimFor` and the bulk loop out of
`CraftingMenu` into `InventoryCraft`** so the recipe browser could share them instead of copying the
most-gated method in the arc.

**"Full suite green" is not evidence for a pure relocation.** It was green before the move as well.
The suite is a REGRESSION signal; what was actually claimed — *these bodies are unchanged* — needs a
different instrument. So each moved body was extracted from both files and diffed:

```
IDENTICAL  commitCraft            (47 lines)
IDENTICAL  claimFor               (5 lines)
IDENTICAL  craftOneFromInventory  (33 lines)
IDENTICAL  debit                  (14 lines)
IDENTICAL  the pin + the bulk loop (16 lines)
```

> **AND "IDENTICAL" THERE IS OVER-ROUNDED. Reviewer-caught, 2026-09-03, and the rounding matters more
> than the fact.** Those verdicts are post-NORMALISATION. The raw bodies **differ**, in exactly five
> places, all requalifications:
>
> | | `MenuSafety.isEmpty` | `CraftingMenu.matches` |
> |---|---|---|
> | `commitCraft` | 1 | 1 |
> | `claimFor` | 1 | — |
> | `craftOneFromInventory` | 1 | — |
> | `debit` | 1 | — |
>
> **The accurate claim is: "identical apart from five call-site requalifications, each to a target
> separately proved byte-identical."** Both targets were then proved, with a control:
>
> ```
> MenuSafety.isEmpty vs the old CraftingMenu.isEmpty  IDENTICAL apart from the access modifier
>     old:  return item == null || item.getType().isAir() || item.getAmount() <= 0;
>     new:  return item == null || item.getType().isAir() || item.getAmount() <= 0;
> identityOf IDENTICAL (3 lines) · matches IDENTICAL (4 lines)     [vs master 4187cd1]
> control: PASS -- injecting `<= 0` -> `< 0` was seen
> ```
>
> **THE POINT IS THAT ROUNDING DEFEATS THE WHOLE METHOD.** Proving a move by diff rather than by
> suite exists to produce an EXACT claim; collapsing it to IDENTICAL discards precisely the thing a
> reader would re-check, and hands them a stronger claim than was tested. **It is also where a real
> defect of this kind would hide**: a `MenuSafety.isEmpty` differing from the predicate it replaced by
> a single character would change four call sites at once, silently, and every normalised diff would
> still print IDENTICAL. The normalisation is only sound *because* the targets were separately
> proved — so a report that omits the target proof is not a weaker version of this argument, it is
> a different and invalid one.

The normalisation covered exactly three things, and no others: the access modifier, the
`CraftingMenu.` qualifier on the moved `matches` call, and `isEmpty` → `MenuSafety.isEmpty`.

> **A CAUTION ON THE EXTRACTION ITSELF, from the reviewer's own pass.** Their first attempt reported
> `matches: CHANGED` — a **false finding**, produced by a crude fixed-line-range slice that had
> picked up neighbouring code shifted by `claimFor`'s removal. A brace-matched extraction, counting
> `{` against `}` from the signature, showed it identical. **The check that got it right parsed
> structure; the one that got it wrong counted lines.** Worth keeping because the false finding
> arrived inside a message asking for MORE precision, which is exactly when a plausible-looking red
> is least likely to be re-examined.

**And the comparison carried a positive control**, because a diff that finds nothing looks exactly
like a diff that ran and matched — this file's oldest lesson. A change was injected into the moved
loop and the comparison was required to report it:

```
control: PASS -- the injected change was seen:
      4c4
      <         int crafted = 0;
      >         int crafted = 1; // CONTROLMARK
```

Without that line, five `IDENTICAL` verdicts would have been worth nothing.

---

### A THIRD COPY OF `isEmpty` WAS ABOUT TO BE WRITTEN

`CraftingMenu` and `MenuRouting` each carry a private `isEmpty(ItemStack)` with byte-identical
bodies. `InventoryCraft` would have been the **third**, which is where a duplication stops being
something a reader can hold in their head.

**A canonical `MenuSafety.isEmpty` now exists and new code uses it. The two existing copies were
deliberately NOT migrated**, and the reason is the same disposition the `core/weapon` debt takes:
both files are heavily boot-gated — `MenuRouting` carries the routing rows — and widening this
slice's diff into them to inline a one-line predicate buys a tidier `grep` at the price of re-gating
routing. **They go when something else already has those files open.**

Recorded so it is a decision with a date on it rather than an inconsistency someone finds later and
has to reconstruct.

---

### `check-tests.sh` COULD REPORT A TOTAL FOR CODE THAT WAS NO LONGER THERE

**Found 2026-09-03, and it is the stale-jar trap occurring inside the tool used to prove the tests
ran.** The script existed to catch "a green build that ran no tests". It could not catch "a green
count describing an older tree", because it reads whatever surefire reports are on disk, from
whenever they were written. **Reports outlive the code they tested.**

**Measured, not reasoned:** a branch carrying ~14 tests' worth of new work was reverted with
`git stash`, and the script — run against the reverted tree with no build in between — printed

```
Tests run across all modules: 1196
```

for a tree that has **1182**. Green, precise, and about code that had just been removed. It was
noticed only because the number was recognisable; a smaller drift would have passed unremarked, and
the whole point of the script is that nobody re-derives the number by hand.

**The guard added:** if any source file is newer than the newest surefire report, the reports
describe an older tree — **exit 1**, naming the offending sources. Not a warning: a warning printed
under a green total is read as green, and the failure mode being closed is precisely that a stale
total is indistinguishable from a fresh one.

**Both directions were proved before it was believed**, per this file's own rule that a check which
never fires looks exactly like one that passed:

| state | result |
|---|---|
| the stale tree that produced the 1196 | **fires**, exit 1, names `CraftingMenu.java` |
| immediately after `./mvnw clean test` | **passes**, exit 0, and reports the true 1182 |

> **The wider point, and it generalises past this script.** This repo has a verification section
> whose four entries are all "a check that did not run looks like a check that passed". This is the
> fifth shape: **a check that ran, correctly, against something other than what you are looking at.**
> The mutation-marker discipline (`grep` for the marker; confirm it compiled) already guards that for
> mutations. Nothing guarded it for the test count.
>
> **It also raises a fair question about every count relayed in that session, and that one closes
> clean:** the reviewer counted `@Test` occurrences directly from origin at each commit — 1176 at
> `bdca392`, 1182 at `d70bb25` and at master — by a different method than the script uses, and they
> agree. The trap did not bite retroactively. Two independent methods agreeing is what settles it;
> re-running the same script would not have.

> **AND THE FIRST SHAPE REAPPEARED INSIDE THE FIX FOR THE FIFTH.** Found in review, one commit
> later. The guard above was written as
>
> ```bash
> newest_report=$(find ... -printf '%T@ %p\n' 2>/dev/null | sort -rn | head -1 | cut -d' ' -f2-)
> if [ -n "$newest_report" ]; then
> ```
>
> **`find -printf` is GNU-only.** Where `find` lacks it — a BSD/macOS `find`, a busybox one, a
> different `find` first on `PATH` — that is an error, **swallowed by `2>/dev/null`**, producing an
> empty result, skipping the `if`, and printing a green total for a stale tree **with no message at
> all**. The guard against "a count describing an older tree" would have disabled itself silently and
> produced exactly the 1196-for-an-1182-tree outcome it was written to prevent.
>
> The dangerous case was never "no reports on disk" — the module loop already shouts about that. It
> was **reports present, `find` broken**, which is CLAUDE.md:104 — *a discovery finding nothing is a
> defect* — broken by the guard that enforces it.
>
> **The pairing is the useful part, and it is why this sits here rather than in its own entry:** the
> four original shapes are all "a check that did not run looks like a check that passed"; the fifth is
> "a check that ran against something else". Writing the fifth did not confer any immunity to the
> first. **A new guard is a new place for the old shapes to live**, and it arrives without any of the
> scar tissue that protects the code it is guarding.
>
> **The fix is a positive control INSIDE the guard**, the same device `check-absorbed.sh` uses to
> reach its BLIND verdict: the report count is taken a second time **without `-printf`**, using the
> `find | wc -l` idiom this script already documents as pipefail-safe. Reports on disk plus an empty
> timestamp scan is now an **error naming the likely cause**, not a skip. Both `find`s capture stderr
> instead of discarding it.
>
> **Proved in three directions, and the third is the new one.** Clean tree → passes, exit 0, 1182.
> Touched source → fires, exit 1, names the file. `-printf` broken → **errors**, exit 1, *"130
> report(s) are on disk, but the timestamp scan returned nothing"*; restore → exit 0 again. On the
> first attempt at the third the **marker count read 0** — the substitution had not applied — and the
> exit 1 came from the staleness path, not the broken-`printf` path. Counting the marker before
> believing the result is what caught it, for the fifth time this session.
>
> **A second defect rode along, and it was already documented twice in the same file.**
> `... | sort -rn | head -1` under `set -euo pipefail`: `sort` buffers all input, `head` exits after
> one line, `sort` takes SIGPIPE, `pipefail` propagates, `set -e` kills the script with a bare exit 1
> and no output — reading as a test failure. **Measured rather than argued:**
> `seq 1 5000000 | sort -rn | head -1` exits **141** under those flags; the real find-based shape
> exits **1**. It survives today only because 130 report files fit inside the 64KB pipe buffer, and
> would have begun failing a few hundred reports from now as an unexplained exit 1. Twenty lines
> above it, that script explains this exact hazard for `find | wc -l` and again for `grep`. **Having
> the rule written down twice, in the file being edited, did not stop it being walked into** — so the
> comment now names both hazards as KNOWN IN THIS SCRIPT, to be found rather than rediscovered.

> **A KNOWN SHARP EDGE IN THE FIX, RECORDED RATHER THAN SANDED OFF — reviewer-raised 2026-09-03.**
> The new guard hard-fails on **any** bytes on that `find`'s stderr, not only on an unsupported
> `-printf`. A benign cause — an unreadable directory under some module's `target/` — would abort a
> correct run with `THE STALENESS CHECK COULD NOT RUN`.
>
> **That is a real tension with the script's own stated design cost:** its staleness comment says the
> false-positive price is *"a scary message on a correct run, and this guard is worth nothing if
> people learn to ignore it."* This check can produce exactly that.
>
> **Deliberately NOT narrowed** (e.g. to `grep -q 'printf'`), because the alternative failure is the
> one the fix exists to prevent: a stderr cause nobody predicted, filtered out, and silently
> swallowed — the self-disabling shape all over again, reintroduced by the narrowing. **Fail loud on
> the unexpected** is the correct default for a guard whose whole subject is checks that quietly do
> not run.
>
> **So: WATCH IT, do not pre-emptively change it.** If it ever fires for a benign reason, that is
> evidence, and the fix at that point is to handle *that specific cause* by name — not to widen the
> filter back out. Logged here so the first person it bites finds a decision instead of a bug.

---

### A DECISION DIALOG'S ANALYSIS PANEL IS A CLAIM, NOT A VERIFIED FACT

**Slice 6 planning, 2026-09-03.** A dialog option's description asserted that an unprobeable
`RecipeChoice` is dropped by `satisfyingGroups` → `CraftCount`, and a whole design section was built
on it: an "exclusion axis" with two members, inert rendering for both, and a gate row recording one
as possibly-unrunnable.

**It was false, and four lines of code said so.** `RecipeProbe.satisfyingGroups` calls
`choice.test(...)`, and `PredicateRecipeChoice.test` works — testing is the one thing a predicate
does. The only empty path is `choice == null`, which `ingredientsOf` already filters. A
predicate-choice recipe is fully probeable, countable and **craftable**.

**The javadoc directly above that method, written in the previous slice by the same author, says it
outright:** *"Every `RecipeChoice` implementation answers `test`; none of them is required to answer
anything else."* That sentence exists because slice 5's whole inversion depends on it.

**This is the `MAIN_HAND_ONLY` miss again** — reasoning from a plausible framing instead of opening
the method — with the aggravating detail that the framing came from a structured decision panel,
which reads as settled analysis rather than as a proposition. **Both parties took it as read.**

**What survived the correction:** the exclusion axis has exactly ONE member, `ComplexRecipe`, which
genuinely exposes no ingredients. And the real limitation is narrower and different in kind: a
predicate choice cannot **enumerate what it accepts for display**. That is a LORE honesty problem —
*"these are the materials"* versus *"these are the materials I can list"* — not a craftability one.

---

### NAMED DEBT: `core/weapon` holds everything that is crafting-and-menu arithmetic, not weapons

**Recorded 2026-09-02, during Quick Craft's first half. Re-sized 2026-09-03, because it drifted
inside the same slice that recorded it.**

> **THE HEADING USED TO SAY "FOUR CLASSES", AND THAT IS THE FINDING.** `SuggestionTier` landed in
> `core/weapon` in the same slice, from the same author, days after this entry was written to stop
> exactly this — and it was not added to the table. By its own javadoc it is *"a display CATEGORY,
> not the gear axis"*, so it belongs with the crafting arithmetic rather than the gear model.
>
> **This is rule 1 pointed at a table instead of at code.** The entry enumerated *the cases that
> existed when it was written* rather than *the axis it was guarding*, so a fifth arrival had nothing
> to fail against. A list of names needs updating by hand every time; a stated axis absorbs the next
> one. The heading now names the axis, and anything matching it belongs in the table below whether or
> not someone remembers to add it.

`core/src/main/java/.../core/weapon/` currently contains, alongside the actual gear model:

| class | what it is |
|---|---|
| `CollectPlan` | which stacks a double-click gathers, in what order |
| `CraftResultIndex` | which gear definition a crafted vanilla item becomes |
| `CraftResultToken` | material-token normalisation |
| `CraftCount` | how many of each recipe the player can make, ranked |
| `SuggestionTier` | which display category a craft suggestion sorts in |
| `PageMath` | where page N of a paged menu starts and stops |

**None of the six is about weapons.** All six are crafting-and-menu arithmetic that happened to be
extractable into `core`, and `weapon` was simply the package `core` already had.

**The deviation was deliberate and is still the right call.** Slice 5 considered opening a
`core/craft` package for `CraftCount` and did not, because the alternative was crafting logic split
across two packages with no principle separating them — one badly-named package beats two arbitrary
ones. This entry exists so that reasoning is on the record rather than looking like an oversight.

**What paying it down would take**, so a future slice can size it honestly:

- Move the six classes to `core/.../core/craft/`, plus their test files.
- **The import cost, MEASURED 2026-09-03 rather than estimated** — the previous "~20 imports" was a
  guess and was wrong in both directions:
  - **10 import lines to rewrite, across 7 distinct files** outside the package (`MenuRouting`,
    `CraftingMenu`, `CraftMatrixScreen`, `RecipeProbe`, `RpgPlugin` — which carries
    `CraftResultIndex` on `AdapterContext` — and their tests).
  - **Plus up to 15 files INSIDE `core/weapon` that would GAIN an import**, because they reference
    these types with no import today. That half was missing from the old estimate entirely, and it
    is the larger half. Upper bound: the count is `grep -l` on the class names, so it includes
    javadoc mentions and the classes' own files; the real figure is lower.
  - Command, so the next reader re-measures rather than trusting this:
    `grep -rlE "import io\.github\.butterflysmp\.rpg\.core\.weapon\.(CollectPlan|CraftResultIndex|CraftResultToken|CraftCount|SuggestionTier|PageMath);" --include=*.java core/src paper/src storage/src`
- **`GearDefinition` and the four gear records STAY in `core/weapon`.** `CraftResultIndex` takes a
  `Collection<? extends GearDefinition>`, so the new package would import the old one — which is
  correct and one-directional, and is the check that the split is real rather than cosmetic.
- **Re-verify:** `./mvnw clean test` (a package move is exactly the kind of change an incremental
  build hides — see the slice 4 entry), and the marker sweep. No boot gate: nothing observable
  changes, which is what makes this cheap and also what makes it easy to keep deferring.

**Do it when something else already touches those files**, not on its own. A pure-rename commit is
expensive to review and buys nothing a reader could see.

**AND WHEN YOU ADD A CLASS TO `core/weapon`, ASK WHICH SIDE OF THIS TABLE IT IS ON.** That question
is the only thing standing between this debt and the next silent arrival — it has failed once, in
the slice that wrote it down, and been answered once since.

> **`PageMath` is the answer, 2026-09-03, slice 6.** It went into `core/weapon` for the same reason
> the other five did — it is pure arithmetic and that is where `core`'s pure arithmetic lives — and
> it was added to the table **in the commit that created it**, rather than being noticed a slice
> later. That is the whole mechanism working once. It is recorded because "the rule fired" is as
> worth knowing as "the rule was missed": the previous entry only has evidence of the failure mode,
> which makes the rule look like a lament rather than something that works when run.

---

### Crafting, Slice 4 (tools, the fourth gear kind) — what it created or exposed

- **THE BRIEF'S HEADLINE FINDING WAS WRONG, AND IT WAS THE REVIEWER'S.** The slice-4 brief carried a
  section headed *"THE ONE REAL HAZARD: `Gate.MAIN_HAND_ONLY` SILENTLY ADMITS TOOLS"*, claiming a
  pickaxe becomes eligible to roll Sharpness. It does not. That arm is already an **allowlist** —
  `null || MELEE || RANGER || MAGE` — so `class: tool` was refused at boot before this slice wrote a
  line, and `EnchantRoll.poolFor`'s `!= heldClass` filter is total over a class it does not know.

  **The mechanism is the useful part: a read of `requireGate` was TRUNCATED at the `SHIELD_ONLY`
  arm, and `MAIN_HAND_ONLY` was then reasoned about from its NAME** — which sounds slot-shaped —
  rather than from its body. A partial read treated as complete. Recorded as retracted rather than
  quietly dropped: a superseded claim gets a note saying what stopped being true.

  **Renaming it to `WEAPON_ONLY` was proposed and rejected as actively harmful.** `NEXT.md:4269`
  cites `MAIN_HAND_ONLY` **by name** as rule 1's own worked example. Renaming retires a live rule's
  only exemplar — rule 3 (a change can delete a rule's only witness) applied to a rename.

- **THE COMPILER COVERED SIX SITES. THE SWEEP FOUND FOUR MORE, AND ONE WAS A REAL HOLE.**
  `GearClassTest` already records that adding `ARMOR` gave three compile errors and five silent
  sites. Adding `TOOL` gave **six** compile errors (`GearItems` x3, `GearClassLabel` x3) plus one red
  test. Where the old five landed:

  | the site `GearClassTest` names | for `TOOL` |
  |---|---|
  | `HeldGear.gearClass` | **deleted** — collapsed onto `GearItems.gearClassOf` |
  | HeldGear's effect tail | covered: `EnchantEffectLine` never enumerates `GearClass`, it compares `!=` and formats through two exhaustive switches |
  | the `/rpg enchant` SHOW refusal | already gone; only stale prose remained |
  | `EnchantMenu.PlacedGear.gearClass` | **deleted** — same collapse |
  | `ANY_BUT_SHIELD` | now `MAIN_HAND_ONLY`, an allowlist. All THREE class-gated arms are allowlists, so `TOOL` is refused by all three |

  **And four the old list did not name**, none compiler-checked: `/rpg give`'s final `else` ·
  `GearRefresher`'s three-tag chain · `RpgPlugin`'s `allGear` and `claimants` · and
  **`CraftMatrixScreen.isGear`**, which is the one that mattered.

- **`isGear` IS A WHITELIST, AND A KIND MISSING FROM IT IS NOT "NOT GEAR" — IT IS GEAR THE CRAFTING
  SURFACE WILL EAT.** That chain is what makes `CONTAINS_GEAR` true, and `CONTAINS_GEAR` is what
  stops a vanilla recipe consuming a minted item. It is four `||`-ed calls: no switch, no sealed
  type, no test that can see an omission, and nothing fails to compile when it falls behind
  `GearDefinition`. It was found by grepping every `ArmorItems.armorId(` call site, not by the
  compiler and not by 1144 green tests. **Witnessed by gate row T10 and by nothing else.**

- **THE TWO ACCESSORS FAILED DIFFERENTLY, AND ONLY ONE WAS SILENT.** Worth keeping because it
  decides what a gate can catch. With a fourth field added to the old `HeldGear`/`PlacedGear`:
  `remint` and `displayName` end in `armor.xxx()` with `armor` null and **throw**; `gearClass` ends
  in `shield != null ? SHIELD : ARMOR` and **returns ARMOR** — the tool draws Protection, Growth and
  Mana Bank, enchants that can never fire on it. That is `requireGate`'s own named failure, *"sell a
  player an XP unlock that does nothing"*, arriving through a door that check does not watch. The
  NPEs need no gate row. **The silent one is T9.**

- **`./mvnw compile` REPORTED `BUILD SUCCESS` WITH THREE COMPILE ERRORS PRESENT.** Adding
  `ToolDefinition` to the `permits` clause and `TOOL` to `GearClass` touched only `core/`, so
  incremental compilation **skipped `paper/` entirely** and never re-checked the sealed type or the
  enum. `GearItems.class` was stamped 14:51:25 against edits made at 15:36. `clean` was required to
  see any of it.

  This does NOT contradict `ContentValidator.checkEffect`'s javadoc, which says clean catches
  nothing there that a plain build does not — **that was measured for a change inside `paper/`**.
  The bound is sharper: **a module whose own sources did not change is not recompiled, however the
  sealed types and enums it consumes have grown.** Cross-module exhaustiveness needs `clean`, and a
  green incremental build is not evidence of anything after a `core/`-only edit.

- **THE LOADER READS A FLAT LIST, DELIBERATELY UNLIKE `ArmorLoader`.** Armor walks the `ArmorSlot`
  ENUM rather than the file's keys, so a tier defining three slots is a named refusal. That is right
  for armor and wrong for tools: **of the 84 durable materials, the tier-by-kind grid describes 24**
  (the sweep at the slice-2 entry above). Shears, brush, fishing rod, flint and steel, mace, carrot
  on a stick and seven spears sit outside it. A `tiers x kinds` shape would model a structure that
  fits under a third of what the loader will hold, and the first irregular would need a special case
  in a loader designed around regularity.

  Two consequences stated where they are relied on: a tool file's key **is** the id and the material
  (one string, so they cannot disagree), and **a bad entry costs the ENTRY, not the file** — the
  opposite of armor's all-or-nothing tier, because tools are not a set. Shears ships in the same
  file as the four regular kinds precisely so the flat list is exercised rather than asserted.

- **A `kind` THAT DISAGREES WITH ITS MATERIAL IS REFUSED AT BOOT**, the same answer slice 2 gave to
  `craft_result != material`. It lives in the record rather than `ContentValidator` because it needs
  no Bukkit registry, which makes it a refusal and a unit test instead of a boot warning.

  **The comparison is `endsWith("_" + token)`, and the underscore IS the check.**
  `"iron_pickaxe".endsWith("axe")` is `true`, so a bare suffix test accepts `material: iron_pickaxe`
  with `kind: axe` — the one pair the refusal exists for. Watched red as mutation 2, at both the
  `ToolKind` and `ToolDefinition` layers.

- **A TOOL'S ROLL POOL IS ONE, AND `EnchantRoll`'s PARAGRAPH WAS WRONG A THIRD TIME.** No shipped
  enchant is gated on tools, so `poolFor(TOOL, ..)` returns the universal set alone — Unbreaking —
  and `candidateCount` clamps to `min(1, 3) = 1`: every slot offers the same single book. Nothing
  throws. That file already admits its pool paragraph *"was wrong for two slices before anyone
  noticed"*; a one-enchant pool falsifies its observability claim in the opposite direction from
  armor's four, and it is corrected there rather than left for a fourth time.

- **BOOT GATE RUN AND PASSED, 2026-09-02 — 17 of 18, operator-confirmed.** The eighteen are T1–T12,
  the five re-opened rows (N1, N4, 21, 22, S1), and M6. Every row passed. **The one that is not a
  pass is M6, which was not run for the second consecutive slice and is now closed as WILL NOT BE
  RUN** — see the slice 3 entry below for the exposure that leaves.

  **The rows that carried the slice:** T5 (shears, the untiered tool — the row a `tiers x kinds`
  loader could not pass without a special case) · T7 (mining with it: the property no tooltip can
  show, and the only thing separating a working mint from an item that renders correctly and digs
  like a fist) · T9 (the silent accessor, which the three NPE-throwing ones did not need a row for)
  · T10 (the `CONTAINS_GEAR` hole in `isGear`, whose only witness this is) · T12 (a bad entry
  costing the entry rather than the file).

- **STILL OWED:** a tool-gated enchant, whenever the roster pass happens — until then T8 records the
  degenerate one-book table honestly rather than the code pretending otherwise.

---

### Crafting, Slice 3 (the grid's vanilla feel) — what it created or exposed

- **THE GATE WAS NOT IN THE REPOSITORY, AND THAT IS THE FINDING.** `GATE-crafting.md` is now
  committed. Until it was, the row text lived in a chat transcript and two published PR bodies —
  outside git, invisible to anyone reading the repo, and unrecoverable once the conversation
  scrolled. **Row 20's text could not be recovered from inside the repository at all.**

  This file has recorded across two slices that nine behaviours are boot-gate-only and that the gate
  must be re-run when they change. A refactor of `MenuRouting` would have found a table naming
  exactly what was unwitnessed **and no way to run the thing that witnessed it.**

  **A WITNESS THAT IS NOT IN THE REPOSITORY IS NOT A WITNESS.** Rule 4 says a gate row can claim
  coverage it does not have; this says a row can claim coverage while not existing anywhere durable.
  Recorded as a corollary in the rules section rather than a fifth rule.

  **The reviewer's defect, not the builder's** — the gate was authored as chat messages and web pages
  because that suited the operator, and nobody asked where it lived. Same attribution rule 4 carries.
  It was NOT reconstructed: rebuilding rows from the sole-witness column would have produced a
  plausible row that had never been run, which is rule 4's failure mode applied to the record of
  rule 4.

  **The two documents are joined now**, not parallel: the unwitnessed tables below name the row id
  in `GATE-crafting.md` that witnesses each entry, so a reader who was not here can get from "this
  is unwitnessed" to "here is the check".

- **BULK CRAFT WAS UNPINNED — it kept going with a DIFFERENT recipe.** Found by running the slice 2
  gate. A grid loaded as the shield recipe with 6 planks per plank slot and 50 iron crafted its six
  shields, ran out of planks, re-matched to the IRON NUGGET recipe, and **converted the player's
  remaining 44 ingots into nuggets.** `craftRepeatedly` re-derived the recipe every pass.

  The commit is now pinned to the recipe the PREVIEW matched — **you receive what you were shown** —
  and that closes by construction the divergence this arc has met three times: Stats Slice 3's
  *"SHARING A FORMULA IS NOT SHARING ITS INPUTS"*, the substitution re-check in `craftOnceToCursor`,
  and row N8's caveat. First time it is made unreachable rather than guarded.

  **`Recipe` DECLARES NO KEY** — verified by javap against the pinned jar, where the interface has
  exactly one method, `getResult()`. The identity narrows through `instanceof Keyed`, which covers
  `CraftingRecipe` (shaped, shapeless) **and `ComplexRecipe`** (customizable fireworks, dye recipes
  — corrected slice 5: the BASIC rocket is shapeless, not complex). That
  completeness matters: slice 1 delegated to the server's matcher SPECIFICALLY because it handles
  complex recipes, so a pin that could not represent one would have re-introduced the hand-rolled
  matcher this arc deleted. `MerchantRecipe` is the only unkeyed recipe in the API and no crafting
  grid produces one.

- **THE PIN IS CAPTURED ONCE, BEFORE THE LOOP — the fix-shaped bug is re-reading it.** Each pass
  recomputes the preview, so the field MOVES during the loop; re-reading it per pass re-pins to
  whatever the shrinking grid now makes, which is the original defect restored **with a pin visibly
  in place**. It has no unit witness — the field's movement needs a live menu — so it was to be run
  as a mutation against gate row S1 rather than carried as a green table row. **That mutation, M6,
  was never run and is now closed as WILL NOT BE RUN; see the entry below.** Slice 4 made
  `craftRepeatedly` take the pin as a parameter, which makes the violation conspicuous rather than
  witnessed.

- **A DRAG NEVER REFRESHED THE PREVIEW.** `Menu.handleDrag` un-cancelled and dispatched nothing, so
  the one-tick hop `onClick` schedules was never scheduled and the grid changed behind a stale
  preview. A new `onDragPermitted` hook fires on the un-cancel path.

  **`getNewItems()` was rejected and the javadoc says why**: it would allow a synchronous projection
  with no scheduler hop, which is cleverer and wrong — a SECOND way of answering "the contents
  changed, recompute" beside one that already exists with a documented reason. Two copies of that
  rule drift, and the drifting one would be the drag path, because nobody exercises it.

  **The two fixes ship together on purpose:** once the commit is pinned, a stale preview makes the
  craft REFUSE rather than produce the wrong item — safer, and it reads as a broken table rather than
  as theft.

- **DOUBLE-CLICK IS SUPPORTED BY BEING PERFORMED, and the original objection is answered rather than
  dropped.** `COLLECT_TO_CURSOR` STAYS in `ALWAYS_REFUSED`; the gesture is intercepted by TYPE ahead
  of it, exactly as the number key and F are. Un-cancelling would let vanilla sweep every matching
  stack out of the top inventory, and this menu paints forty identical filler panes. Performing it
  does not, because WE choose the sources.

  **SOURCES ARE STACKING SLOTS, NOT `inputSlots()`.** `MenuRouting` is shared with `EnchantMenu`,
  whose single EXCLUSIVE slot holds a weapon — iterating input slots would have made that weapon a
  collect source. Harmless today ONLY because a weapon's max stack is 1, so a cursor holding one is
  already full: **arithmetic accident, not design**, and it stops being true the first time an
  EXCLUSIVE slot holds something stackable. Excluded by the policy switch instead. Gate row S11 is
  its only witness.

  **Ordering deviates from vanilla at exactly one point, deliberately.** Inventory drains first,
  then the grid; smallest-first WITHIN each tier. Smallest-first across both would prefer the grid
  precisely when a recipe is loaded — a staged recipe is partial stacks by definition — so the most
  faithful ordering is the one that most reliably eats the player's layout. This grid is not
  vanilla's transient one: the reported workflow stages stacks and returns to them.

- **`CREATIVE` GOT ITS OWN REFUSAL STATEMENT.** One line used to cover it and `DOUBLE_CLICK`
  together. Creative middle-click makes items out of nothing — the one constant there whose loss is a
  real duplication — so the two can no longer share a fate through a condition someone relaxes for
  the other's sake. Gate row S10 is its only witness, and it exists because a mutation removing that
  guard must redden something.

- **THE RESULT SLOT'S DOUBLE-CLICK IS A NAMED REFUSAL, not a fall-through.** A double-click fires
  LEFT then DOUBLE_CLICK. Slice 1 declined to port the old project's `MenuThrottle` — which guarded
  exactly that pair — because `DOUBLE_CLICK` was refused by TYPE before dispatch. **This slice
  withdrew that guarantee**, so treating it as a take would give one gesture two crafts, and with the
  pin in place the second one succeeds. Both crafts pay, so it is not a duplication — just a craft
  nobody asked for and nobody notices. Gate row S8's expectation is therefore **one craft, not
  nothing**.

- **A FALSE JAVADOC, corrected at the source.** `CraftingMenuLayout.GRID_SLOTS` promised *"Iteration
  order is 0..8, not a hash order"*, but the value is `Set.copyOf(...)`, whose order the JDK leaves
  unspecified — the `LinkedHashSet` order is discarded. It sat on the exact constant an ordered grid
  walk would reach for. `MenuRouting` already worked around it with `TreeSet` twice, and the collect
  makes a third.

- **`MAX_BULK_CRAFTS`' javadoc corrected**: 64 is reachable in normal play (64 planks per slot is 64
  shields), so it is both a runaway guard AND a per-gesture batch size. The runaway framing alone
  invites treating arrival there as a defect, or "fixing" it by removing the bound.

- **BOOT GATE RUN AND PASSED, 2026-09-02 — operator-confirmed.** Every slice 3 row, plus the
  re-opened rows this slice's surface changes require: **6, 9, 10, 11, 13, 1c, 1d, 16-19**, and
  **12, 12c** because `commitCraft` changed shape again, and **N5b, N8** from slice 2.

  **Four rows carry it, and each is the only check its behaviour has.** **S1** is the reported
  defect measured by counting — six shields and forty-four ingots still in the grid, because the
  defect's signature is a number rather than an appearance. **S6** is the dead-second-tier row:
  without it a collect that never reaches the grid passes S5 perfectly, and "inventory first" is
  indistinguishable from "inventory only". **S11** is the base-class regression against
  `EnchantMenu`, whose weapon slot the enchant tests are structurally blind to — the same blindness
  that made 1c and 1d necessary. **S12** is the one worth failing the slice over: holding glass panes
  matching the filler and double-clicking must move nothing, which is what proves the
  collect-to-cursor exploit is still closed now that the gesture is performed rather than refused.

- **M6 IS NOT RUN, AND THIS ENTRY BRIEFLY CLAIMED IT WAS.** Corrected on 2026-09-02, one commit
  after the claim reached master.

  The gate result was reported as run and passed without naming M6, and M6 was written into this
  record as run anyway — **inferred from "the gate was run" because M6 sits in the gate file's slice
  3 section.** It is not a row; it is a separate build. Nobody said it had been done.

  That is a check credited from inference rather than from a report, which is rule 4's defect
  committed by the person writing the record instead of the person specifying the rows: *an
  unwitnessed defect wearing a passing line*, and the suite cannot notice because there is nothing
  here for a suite to run. **Recorded rather than quietly fixed**, because the next reader's question
  is "was this checked", and "it was written down without being run" is a different answer from
  "it passed".

  **The rule it belongs under:** the corollary already says a witness outside the repository is not a
  witness. This adds that a witness recorded from inference is not a witness either. A pass goes into
  this file only when someone says it was observed — never because it would be consistent with what
  they did say.

- **M6 WILL NOT BE RUN, and the pin's capture-once property therefore has NO WITNESS AND IS NOT
  GETTING ONE.** Closed 2026-09-02 after a second consecutive skip. It was the only check the
  property could ever have: build with the pin re-read inside `craftRepeatedly`'s loop rather than
  captured before it, run gate row S1, watch the ingot count go to zero.

  It was declined twice, both times argued as cheap because a server was booting anyway. **Twice is
  the answer.** It is a separate BUILD rather than a row, so no gate run reaches it, and nobody is
  going to hand-make a mutant build to check a property they already believe. Recorded as WILL NOT
  BE RUN rather than left owed, because "owed" reads as coverage that is arriving.

  **The exposure, stated rather than implied:** if someone changes the pin to be re-read inside the
  loop, every test stays green, every gate row still passes, and a player crafting into a depleting
  grid gets their remaining ingots converted to a recipe they never saw. The defect is *fix-shaped*
  — a re-read pin and a captured pin are indistinguishable by reading the code — and the field moves
  during the loop precisely because each pass recomputes the preview.

  **What was done instead is a SMALLER thing than a witness, and is written as the smaller claim:**
  `craftRepeatedly` now takes the pin as a **parameter** rather than reading the field itself. The
  field is still a field; the bug is still reachable; nothing tests it. What changed is that
  re-reading `previewedRecipe` inside the loop now has to be *written in*, against a parameter
  that is already correct, instead of being the natural thing to reach for. **Conspicuous, not
  witnessed.**

### Crafting, Slice 2 (mint on craft) — what it created or exposed

- **THE ZERO-GUARD CAUGHT A REAL DEFECT ON ITS FIRST BOOT, and it was the two-content-trees trap.**
  The new index prints what it registered, not only what it refused, and the first boot said:

  ```
  Mint-on-craft: 0 result(s) indexed from 0 gear definition(s) that claim one
  No craft_result claims were indexed -- crafting will never mint RPG gear, and a crafted
  shield will give ZERO custom protection.
  ```

  Nothing was wrong with the code. `saveResource(path, false)` **never overwrites**, so
  `run/plugins/Rpg/content/` still held the pre-slice files: `grep -c craft_result` was **0** in the
  deployed tree and **1** in the source. A boot with `--refresh-content` then read
  **25 result(s) indexed from 25** — one shield, twenty-four armor pieces.

  **Without the count line this would have looked like success.** A silent index and a working index
  produce the same log, every craft would have stayed vanilla, and the natural next step — opening the
  gate rows and finding a plain shield — would have sent the hunt into the mint path rather than into
  the deployed content folder. Print what a scan FOUND, not only what it rejected.

  **AND THE COUNT NEEDS A DENOMINATOR FROM OUTSIDE THE THING IT MEASURES.** The line first read
  `N result(s) indexed from M gear definition(s) that claim one` — honest, and a weaker control than
  it looks, because BOTH numbers come from the same parse. A bug that dropped every armor claim zeroes
  them together and prints `1 indexed from 1`, which is internally consistent and reads as a server
  where only the shield opted in. It now prints `25 indexed, 25 claiming, of 30 gear definitions`,
  where the last number comes from the registries: the same bug then reads `1, 1, of 30` and is wrong
  at a glance. Same defect shape as a grep with no positive control — a self-consistent pair proves
  nothing.

- **NO WEAPON OPTED IN, SO NO SWORD MINTS ON CRAFT. The roadmap's largest category is still
  uncovered, and that is stated here rather than left to be inferred from a count.** The 25 indexed
  results are 24 armor pieces plus one shield; `weapons.all()` contributed nothing.

  That is the expected consequence of the `iron_sword` contest, not an oversight. `ironblade` and
  `emberblade` both render as `iron_sword`, so whichever claimed it would be handed to a player who
  crafted a plain sword — and an Emberblade from six sticks and two ingots is an economy decision no
  index should make on its own.

  **What would unblock it**, in order of preference:
  1. Give the swords distinct materials, so a claim is unambiguous — `ironblade` keeps `iron_sword`
     and `emberblade` moves to something of its own. One file each, no code.
  2. Or accept that plain vanilla swords stay plain and mint only distinctive ones. Also no code.

  **The gate has no weapon-mint row for the same reason, and that absence is deliberate.** Adding one
  now would be a row that cannot pass, which is worse than no row — see rule 4. It arrives with the
  first weapon that claims a result.

- **`craft_result` is a NEW opt-in content key, and it is deliberately not `material`.** A material is
  PRESENTATION: `WeaponDefinition.DEFAULT_MATERIAL` is `iron_sword` and every sword-shaped weapon
  leaves it there, so `ironblade` and `emberblade` already share one today and always will. An index
  keyed on materials would have `iron_sword` permanently contested, would warn on every boot about
  content that is correct, and **no sword would ever mint**. A warning that fires forever is one
  people learn to scroll past. The claim is made once, by one definition, and a second claim is a
  genuine authoring error rather than the norm.

  A contested result is **DROPPED, not first-wins**: the loaders sort files for determinism, so
  first-wins would let alphabetical order make an economy decision, and a rename would silently change
  which weapon a player receives.

- **TWO CONTENT MISTAKES THAT ARE INVISIBLE IN PLAY, refused at boot.** Both are the shape
  `ArmorConsistency` already documents — no throw, no log, no test can see them, and boot is the only
  moment the claim and the Bukkit registry are in the same JVM.
  1. A claim that differs from its own `material` would let a player **craft iron and receive
     diamond**, because the mint builds from `material()` and not from what was crafted.
  2. A claim on a material with no durability would index cleanly and then be dropped by the mint's
     durability gate on every craft, forever, with the author seeing nothing at all.

- **`commitCraft` returns the FINISHED item, and that is a defect avoided rather than a style
  choice.** It used to return an `ItemCraftResult`, which meant the mint had to be applied by its
  callers — and there are **three**, not two: `craftOnceToCursor`, `craftRepeatedly` and the preview.
  `craftRepeatedly` is the shift-click bulk path, so the version that applied the mint per-caller
  would have shipped **every bulk-crafted item plain and unrolled while a single click minted** — and
  gate row 13 passes either way, because it counts output rather than opening it. One place to forget
  instead of three.

- **The preview MINTS but does not ROLL, obtained structurally.** The roll is a `ThreadLocalRandom`
  draw: if both sides rolled they would draw independently and the slot would advertise candidates the
  player will not receive. The preview never enters `commitCraft`, so this is a property of the call
  graph rather than a rule someone has to remember. **The enchant lines are the one expected
  difference** between what the result slot shows and what arrives — a gate row that does not say so
  fails on a correct build.

- **`gearClassOf` was extracted before it became a fifth copy.** Deriving a roll's `GearClass` already
  had four (`RpgCommand:914/922/931/1624`); crafting would have made five, which is exactly the shape
  `GearItems.remint` was created to kill. It is now an exhaustive switch with no default arm, and
  **tools — the next slice, and a fourth arm — will stop the build here** until someone says what
  class they roll on. Without it a crafted pickaxe would silently never roll, and because gear is
  never rolled retroactively, every one made before anyone noticed would be permanently unrollable.

- **STILL NO AUTOMATED WITNESS — three new entries, all gate-only.** Same cause as Slice 1:
  `CraftingMenu` and `RpgListeners` cannot be constructed without a server.

  | no automated witness | lives in | what goes wrong unseen | sole witness (row in GATE-crafting.md) |
  |---|---|---|---|
  | the Crafter's DURABLE-RESULT guard | `RpgListeners.onCrafterCraft` | plain vanilla gear leaks in through a Crafter, or every Crafter jams | **rows N9 + N10** |
  | mint-on-result itself | `CraftingMenu.commitCraft` | a crafted shield arrives plain, giving zero protection | rows N2, N3 |
  | the bulk path minting | `CraftingMenu.craftRepeatedly` | bulk crafts ship plain while single clicks mint | **row N5b ONLY** |

  `Material.getMaxDurability()` throws `ExceptionInInitializerError` headless — established while
  resolving row 12c — which is what puts the durability guard here rather than in a test.

- **THE TWO CRAFTER GUARDS MUST NEVER BE MERGED.** `CONTAINS_GEAR` is a **correctness invariant**
  protecting an INGREDIENT a player already owns; the durable-result refusal is a **policy** about
  OUTPUTS, applying to items no definition has ever claimed. Someone will eventually want to relax the
  policy — a config flag, a permission, one material — and if the two are one condition they will
  relax the invariant with it and a Crafter will quietly start eating minted weapons again.

- **THE HONEST BILL FOR THE CRAFTER, SWEPT NOT ASSERTED.** `getMaxDurability() > 0` across `Material`
  on a booted server: **84 durable materials**. Beyond weapons, armour, tools and shields, a Crafter
  can no longer produce:

  `BRUSH`, `CARROT_ON_A_STICK`, `FISHING_ROD`, `FLINT_AND_STEEL`, `SHEARS`,
  `WARPED_FUNGUS_ON_A_STICK`, `WOLF_ARMOR`, `MACE`, and the seven `*_SPEAR` variants.

  **Automated shear production for a wool farm stops working**, and that is a real thing players
  build. Refusing early is still right: allowing automated iron-chestplate production today and
  minting chestplates tomorrow means either breaking farms people have built or grandfathering plain
  gear into the economy. A cancelled `CrafterCraftEvent` keeps its ingredients and has no feedback
  channel, so **a refused Crafter looks like a jam, not an error**.

- **BOOT GATE RUN AND PASSED IN FULL, 2026-09-02 — operator-confirmed.** Row N1 was the machine's;
  every other row is the operator's. Nothing failed.

  **The four re-runs were not optional and are the reason this slice could be believed.**
  `commitCraft` changed shape, and rows **12** and **12c** are the ONLY witnesses that
  `getResultingMatrix` and `getOverflowItems` have anywhere in this project — the suite passes with
  either of them deleted. Rows **7** and **8** cover the gear screen this slice builds on top of, and
  8 exercises the Crafter path that decision 2 changed outright.

  **The rows that carried the new work:** N2 and N3 are the mint itself (a crafted shield reading
  `Damage Reduction: 35%`, a crafted chestplate reading `Defense: 6` — the NAME distinguishes
  neither, only the lore does). **N5b** is the bulk path, the third caller of the craft output, and it
  had to be run by OPENING every bulk-crafted item rather than counting them, because a count passes
  on the very defect it exists to catch. N9 and N10 are the Crafter's two directions: a durable result
  refused, a non-durable one still crafting — and without N10 a guard that refuses everything looks
  identical to a guard that works.

- **Still owed:** nothing in this slice. The weapon-mint row arrives with the first weapon that claims
  a `craft_result`, per the entry above.

### Crafting, Slice 1 (the grid surface) — what it created or exposed

- **THE BOOT GATE IS THE SOLE WITNESS FOR EVERY PATH THAT CAN LOSE OR DUPLICATE AN ITEM.** Read this
  before trusting the test count. The slice ends with **1078 tests green (core 627, storage 17,
  paper 434)** and that number is genuinely misleading about what is covered here.

  Five mutations were witnessed red. **All five are on the PURE classes** — `GridClickIntent`,
  `CraftingMenuLayout`, and the matrix screen's walk. Everything below has **NO automated witness of
  any kind**:

  **Every row below is witnessed by a GATE ROW AND NOTHING ELSE.** The gate has now been run once,
  by hand, and passed. Re-run it after any change to these methods, because the suite will not
  notice.

  | no automated witness | lives in | what goes wrong unseen | sole witness (row in GATE-crafting.md) |
  |---|---|---|---|
  | merge overflow arithmetic | `MenuRouting.merge` | a 64-onto-40 place loses the remainder | row 10 |
  | the cursor swap | `MenuRouting.swapCursor` | two writes, one of them wrong | row 9 |
  | the drag widening | `Menu.handleDrag` | a drag spanning both inventories is permitted | row 6 |
  | shift-click top-up ORDERING | `MenuRouting.shiftMove` | 64 cobblestone lands beside the stack instead of on it | row 11 |
  | `shiftClickDispatches` performing no move | `MenuRouting.shiftMove` | the preview is given away AND the craft output on top | row 13 |
  | the `getResultingMatrix` write-back | `CraftingMenu.commitCraft` | a cake's three empty buckets are destroyed | row 12 |
  | the `getOverflowItems` give | `CraftingMenu.commitCraft` | remainders that would not fit vanish | **row 12c ONLY** |
  | `returnEverything`'s clear-before-give | `Menu.returnEverything` | duplication on a re-entrant close | rows 16-19 |
  | own-inventory-only drags still passing | `Menu.handleDrag` | every menu silently blocks backpack drags | rows 1c/1d |
  | preview and commit agreeing | `CraftingMenu.craftOnceToCursor` | a substituted result is merged onto the cursor | **NOTHING — see below** |

  **The last row has no witness at all, and cannot get one here.** A third-party
  `PrepareItemCraftEvent` listener may CHANGE a result rather than null it, and nothing on the dev
  server does that — so gate row 1e passes on a build that has the bug. The re-check inside
  `craftOnceToCursor` is the only protection that exists; it is not belt-and-braces over a gate row.

  **The cause is structural, not laziness.** `MenuRouting`, `Menu` and `CraftingMenu` cannot be
  constructed without a running server and this project has no MockBukkit — a fact already stated in
  four places in this repo. **Proven rather than assumed:** `placeAllowed`'s EXCLUSIVE occupancy rule
  was mutated to always-allow and **all 428 paper tests stayed green**. That mutation was run
  specifically to establish the absence of a witness.

  So: **a future reader who sees "1078 tests, all green" and refactors `MenuRouting` has nothing
  stopping them.** That is the real cost of having no MockBukkit, recorded here rather than argued
  away. Anyone touching this surface re-runs the operator gate; the suite will not save them.

  What was done about it instead of shrugging: the decidable part was extracted until it WAS
  witnessable (`GridClickIntent` is a pure function precisely because the router is not testable),
  `accepted` was threaded through it as a parameter so the acceptance gate has a witness at all, and
  the matrix screen's walk was split behind a `Predicate` seam following `ContentValidator`'s
  precedent — which is what catches a screen that only ever looks at slot 0, the bug that passes
  every casual in-game trial where the tester used the first cell.

- **The plan's own mutation table was wrong and is corrected at the source.** It listed twelve rows
  as though each had a witness. Five did. The prose predicting the other seven is known-false and the
  table now says which rows are boot-gate-owned. This is NEXT.md rule 2 applied to a plan rather than
  to a test.

- **`craftItemResult` MAY MODIFY THE MATRIX ARRAY IT IS GIVEN, and the first implementation passed
  live references.** Only `getCraftingRecipe` disclaims mutation, and it does so by pointing at the
  craft call as the thing that does: *"This method will not modify the provided ItemStack array, for
  that, use `craftItem(ItemStack[], World, Player)`."* No `craftItemResult` overload disclaims it.
  Since `Inventory.getItem` hands back a stack mirroring the slot, the server could write THROUGH
  into the player's grid — on the PREVIEW path, which runs several times a second, and on the
  commit's empty-result abort, which deliberately writes nothing back. `readMatrix` now clones.
  Caught by reading the javadoc while answering a review question, not by any test, which is the
  point of the entry above.

- **BOOT GATE RUN AND PASSED IN FULL, 2026-09-01 — all 28 rows, operator-confirmed.** Rows 7, 8, 12
  and 12c passed by name; nothing failed at any point.

  **Rows 7, 8, 12 and 12c are the ones that carry the slice**, and each is the sole witness for
  something no test in this repo can see. Row 7 puts a minted weapon in the grid beside otherwise
  valid ingredients and the result stays empty — the only check that the gear-tag screen fires. Row 8
  repeats it in the player's own 2x2 grid AND in a Crafter block, which row 7 cannot reach and which
  is the only in-game proof `CrafterCraftEvent` is wired at all. Row 12 crafts a cake and counts
  buckets, the only witness for `getResultingMatrix()`. Row 12c is described below.

  Row **12b did not run because it was impossible as written**: it called for two milk buckets in one
  slot, and `MILK_BUCKET.getMaxStackSize()` is **1**. It was written from reasoning and the number
  was never checked. It did not fail and did not pass — **it was never a test**, and it is replaced
  by 12c rather than counted.

- **`getOverflowItems()` HAD NO WITNESS ANYWHERE, INCLUDING IN THE GATE THAT CLAIMED TO COVER IT.**
  This is the sharper half of the 12b mistake and it survived the first correction. Row 12's cake
  DOES leave three empty buckets, but they **fit back into the matrix**, so it exercises
  `getResultingMatrix()` and never reaches the overflow give. The unwitnessed table above credited a
  check to a row that could never have exercised it — the same shape as a mutation row predicting a
  red it never gets, one level up.

  **Reaching it needs a remainder-producing ingredient that STACKS**, so consuming one leaves the
  slot occupied and the remainder homeless. Buckets are exactly the wrong choice. Read from the live
  registry rather than recalled — `Material.getMaxStackSize()` throws
  `ExceptionInInitializerError` headless, so this needed a booted server:

  | material | max stack | crafting remainder |
  |---|---|---|
  | `DRAGON_BREATH` | 64 | `GLASS_BOTTLE` |
  | `HONEY_BOTTLE` | **16** | `GLASS_BOTTLE` |
  | `LAVA_BUCKET` / `MILK_BUCKET` / `WATER_BUCKET` | **1** | `BUCKET` |

  **That sweep is exhaustive over the whole `Material` enum: exactly TWO stacking materials with a
  crafting remainder exist.** So the row is reachable, and honey is the only practical family.

  Then verified by making the server perform the craft rather than by reasoning about it — 2 honey
  bottles in each of four cells of a 2x2, through the same `craftItemResult` call
  `CraftingMenu.commitCraft` uses:

  ```
  PROBE recipe=ItemStack{HONEY_BLOCK x 1}
  PROBE result=ItemStack{HONEY_BLOCK x 1}
  PROBE resultingMatrix=HONEY_BOTTLE x1 | HONEY_BOTTLE x1 | AIR | HONEY_BOTTLE x1 | HONEY_BOTTLE x1 | AIR | ...
  PROBE overflow=[GLASS_BOTTLE x1, GLASS_BOTTLE x1, GLASS_BOTTLE x1, GLASS_BOTTLE x1]
  ```

  **Gate row 12c** is therefore: two honey bottles in each of four cells, craft one honey block,
  expect **four glass bottles delivered to you** with each cell still holding a honey bottle. Count
  bottles before and after. **RUN AND PASSED.** It is the only row in existence that reaches
  `getOverflowItems()` — the probe above proved the API path, and 12c is what proves our handling of
  it.

- **THE STALE-JAR TRAP FIRED, AND WAS NOT ARGUED WITH.** While chasing the above, `dev-server.sh`
  printed `rm: cannot remove 'run/plugins/rpg-...jar': Device or resource busy` and `set -e` aborted
  before deploying — the exact incident CLAUDE.md's VERIFICATION section records. The mtimes settled
  it rather than a story: deployed `21:54:24` / 551386 bytes against target `22:00:48` / 552257
  bytes, and `grep -c "Done ("` on the log returned **0**, so no server had booted at all. Two
  orphaned `paper.jar --nogui` JVMs from an earlier boot held the lock; killing them and confirming
  `rm` then succeeded was what fixed it. **Had the sweep been read at that moment it would have shown
  nothing, and "no results" is indistinguishable from "no such material exists"** — which is the
  precise wrong answer this whole entry exists to avoid.

- **TWO CALLERS THAT AGREE TODAY ARE NOT TWO CALLERS SHARING AN INPUT — third instance.** The Stats
  Slice 3 section already names this shape (*"SHARING A FORMULA IS NOT SHARING ITS INPUTS ... they
  agree TODAY"*), and crafting met it again: the preview uses the event-free `craftItemResult`
  overload and the commit uses the event-firing one, so a third-party `PrepareItemCraftEvent`
  listener that CHANGES a result (rather than nulling it) makes them disagree. The empty-result abort
  cannot see that, because a substituted result is not empty; on the merge-onto-cursor path the
  player would have received more of what was already on their cursor instead of what they crafted.
  `craftOnceToCursor` now re-checks the CRAFTED item rather than trusting the preview it was
  authorised against.

  **No gate row can catch it** — nothing on the dev server mutates a craft result, so the
  preview-matches-what-you-receive row passes on a build that has the bug. The code guard is the only
  protection, not a belt-and-braces on top of one. **Slice 2 meets this shape again** the moment
  recipes come from content and the two callers stop being the same server matcher.

- **A guard that fails OPEN is worse than no guard, and one shipped for a day.**
  `onCrafterCraft` began `if (!(getState() instanceof Crafter crafter)) return;` -- a bare return
  with no cancel. Near-unreachable, but it was the single line in the slice that said "unsure means
  CRAFT" while every other line said the opposite, and it sat in the guard for the surface with the
  weakest witness. It now cancels. The cost of being wrong that way is a Crafter that will not
  craft; the cost of the other way is a player's weapon.

- **`RpgListeners` is the single Listener, and both crafting guards went into it.** The plan
  specified a separate `CraftGuardListener` class; `RpgListeners:77-80` says *"The single Bukkit
  Listener. Registered once, in RpgPlugin. Resist adding a second one."* The instruction wins.
  Consequence: `RpgPlugin` needed no change at all, and `onDisable`'s `getHolder() instanceof Menu`
  walk already covers `CraftingMenu`.

  **What caught it was INCIDENTAL, and that is the lesson rather than the outcome.** The brief, the
  plan, and the plan review all specified a second Listener class against an explicit written
  instruction none of them had read. It surfaced only because someone went looking for where to put
  a handler and read the surrounding javadoc on the way past. Nothing in the process was aiming at
  it. **A plan-versus-instructions pass belongs BEFORE building** -- read CLAUDE.md and the javadoc
  of every file the plan says it will modify, and check the plan against them deliberately. Cheap,
  and it turns a lucky catch into a check. The same pass would have flagged the `MenuIcons`/`Menu`
  javadoc conventions this slice had to infer.

- **`PrepareItemCraftEvent` cannot reach the Crafter block, structurally.** `CrafterInventory`'s
  superinterfaces are `{Inventory, Iterable<ItemStack>}`; it does not extend `CraftingInventory`, and
  `PrepareItemCraftEvent`'s only constructor takes one. `CrafterCraftEvent` covers that surface and,
  unlike the other event, IS `Cancellable`. A single-handler implementation would have left a
  redstone-driven Crafter eating minted gear with nothing firing.

- **Still owed:** the whole operator gate except row 1. Rows 7, 8 and 12 carry the slice — 8 is the
  only in-game proof `CrafterCraftEvent` is wired, 12 (craft a cake, count buckets) the only one that
  catches container-remainder destruction, and the own-inventory drag rows the only ones that catch a
  regression in every OTHER menu. **Gate row 1b (the region/thread witness) is operator-owned, not
  machine-runnable** — it needs a player clicking in a menu, and the plan was wrong to list it as
  something the build could run.

### Stats, Slice 3 (`/rpg stats`) — what it created or exposed

- **BOOT GATE RUN AND PASSED, 2026-08-31: all eight rows, operator-confirmed**, including both
  discriminating ones — and rows 2 and 6 **re-run** after the `/5s` display change and the mana
  rebalance.

  **Rows 3 and 5 carry the slice.** Row 3 ran the command as a non-op player, which is the only check
  that the `rpg.command.stats` yml entry landed rather than silently defaulting to op-only. Row 5
  swung the weapon and compared the number to the sheet — the only check in existence for the input
  seam below, which no unit test can see.

  Rows 3, 4, 5 and 7 were deliberately **not** re-run after the tuning: it changed two numbers and no
  behaviour, and the byte-identical `EffectApplierTest` and `golden-lore.txt` say so mechanically.
  That is what scoping a re-gate is for.

- **THE STATS ARC IS CLOSED.** Slice 1 built health regen, Slice 2 lifted the mana rate to a
  per-player stat, Slice 3 displays all eight and de-duplicated the damage composition on the way.

- **THE DAMAGE COMPOSITION HAD TWO COPIES AND ITS EXPLANATION HAD THREE.** `EffectApplier` wrote
  `(base × multiplier(pct) + classBonus) × chargeScale × critMultiplier` out twice, and the 14.95
  ordering witness appeared in `EffectApplier`, `Caster` **and** `AttackCharge`. Now one home,
  `HitDamage`, split into `hitBase` (where the ordering hazard is) and `dealt` (the product tail).
  `EffectApplierTest` stayed byte-identical, and all four composition mutations reddened **it** as
  well as the new test — the extraction is load-bearing for the shipped combat path.

- **`AttackCharge` said the charge scale "is the LAST transform". It was false** — crit applies after
  it. Written before crit existed and never revisited; harmless only because both are bare multiplies
  outside the parenthesis. Corrected.

- **SHARING A FORMULA IS NOT SHARING ITS INPUTS, and this is the slice's one residual risk.**
  `HitDamage` guarantees the sheet and the combat path compute the same way. It cannot guarantee they
  are fed the same numbers: combat reads its three summands off a snapshot frozen at cast time, the
  sheet reads them live. They agree **today** because `BukkitCombatant.snapshot` is a straight read of
  `attackValue` / `classDamageValue` / `enchantDamagePercentValue` and `Caster.of` copies all three
  through unchanged — verified at the source during this slice, and `snapshot` now carries a javadoc
  saying so.

  **If a transform is ever added there, the sheet drifts from the swing and NOTHING REDDENS** — the
  formula would still be shared and both callers would still be correct in isolation. Transform at the
  STAT if you must, so both sides move together. Gate row 5 is the only standing check.

- **`GearLoreLines.trimNumber` cannot format a rate**: a gear-modified rate of `0.2 + 0.1` per second
  prints over five seconds as `1.5000000000000002`. Executed, and asserted in `StatsSheetLinesTest`
  so nobody re-adopts it. `StatsSheetLines` uses `trimNumber` for capacities and two decimals for
  rates and damage — which means **the sheet and the action bar can differ in the last digit** (a
  137.5 max reads `137.5` here and `138` there, because the bar `Math.round`s). Deliberate: one is
  exact, one is glanceable.

  > **The original witness was the mana base itself** (`1.6666666666666665`), and the Slice 3
  > rebalance made both bases land on whole numbers over five seconds — which is exactly the state in
  > which two decimals look like over-engineering. The replacement above is reachable with any
  > off-round bonus, so the reason survives the retune rather than evaporating with it.

- **REGEN IS DISPLAYED PER FIVE SECONDS, not per second.** Health regen reads `1.00/5s` and mana
  `5.00/5s`. At one second the interesting rates are all fractions — base health regen is 0.2, which
  reads as noise — and five is how both stats were designed ("1 HP every 5 seconds") and how a player
  counts. The ×5 is presentation and never leaves `StatsSheetLines.perFiveSeconds`; nothing
  downstream sees a per-5s number, and the tick→second conversion stays in `ManaRegen.perSecond`.

- **A `<player>` ARGUMENT IS DEFERRED FOR A THREADING REASON, not an oversight.** `Stat.modifiers` is
  a plain `LinkedHashMap` and `Stat.value()` iterates it; only the outer `states` map is concurrent.
  Reading your OWN stats runs on your own region thread — the same thread your reconcile loop runs on
  — so nothing mutates underneath. Reading **another** player's would iterate maps their loop mutates
  on their region thread four times a second: a `ConcurrentModificationException` out of a command, or
  a torn sum, across eight lines plus the resolver reads inside `ResourcePool`. It needs a region hop
  or a snapshot type first. **Do not add the argument without doing that work.**

- **One mutation came back GREEN and stays in the table.** Hardcoding `NamedTextColor.RED` where
  `StatsBarText.HEALTH_COLOR` belongs cannot be reddened by any unit test — they are the same
  singleton. Asserting against the constant catches DIVERGENCE the day a HUD colour changes; it cannot
  catch the hardcoding. The compile-time import is the guard. `ArmorLoreTest` has the identical limit,
  which is worth knowing before someone tries to "fix" either test.

- **`rpg.command.stats` is the first `default: true` node added since `cast` and `class`**, and the
  first player-facing `/rpg` subcommand in this arc. Declared in `paper-plugin.yml`; an undeclared
  node silently defaults to op-only.

### Stats, Slice 2 (Mana Regen as a per-player stat) — what it created or exposed

- **BOOT GATE RUN AND PASSED, 2026-08-31: all seven rows, operator-confirmed**, including both
  discriminating ones. Reported at that granularity — seven of seven, rows 4 and 5 by name.

  **Rows 4 and 5 carry the slice.** Cast to empty, idle ~12 s without touching gear, THEN equip the
  fixture: mana did not jump, and unequipping did not drop it. They are the only rows that fail
  without the pin, and row 4 fails visibly — a ~20-mana jump — on the parent commit. So the
  lazy-integration re-pricing is closed in both directions in the wild, not merely in a unit test.

  Row 2 is the row that would have caught the opposite failure: a reconcile that always reported
  "changed" pins four times a second, `asOfTick` never advances, and the bar simply stops moving.

- **A ONE-ULP RENAME WOULD HAVE RE-RATED EVERY PLAYER ON THE SERVER.** The plan called for making
  per-second canonical and renaming `MANA_PER_TICK` → `MANA_PER_SECOND`. Measured before writing
  anything: `MAX_MANA / (60 * 20)` is `0x1.5555555555555p-4`, `(MAX_MANA / 60.0) / 20.0` is
  `0x1.5555555555556p-4`, and `==` is **false**. The rename would have shifted the regeneration rate
  for everyone — including players wearing no mana gear — silently, and by an amount no gate row
  could see.

  So the expression is textually unchanged, per-second is derived FROM it, and the resolver composes
  **in ticks**: `MANA_PER_TICK + ManaRegen.perTick(bonus)`. With no bonus that is `x + 0.0`, which is
  exactly `x`. The constant now carries a javadoc saying it must not be tidied.

  > **Stats Slice 3 retired this bit-identity deliberately** — the mana base was rebalanced from a
  > 60-second to a 100-second refill, so the shipped rate changed on purpose. **What it also did,
  > invisibly, was remove this entry's live example**: at the 100-second base the two orderings agree
  > exactly (`0x1.999999999999ap-5` either way, `==` true). The hazard is a property of division
  > ordering, not of any particular divisor, so the rule and the single-division form stay — and
  > `ManaRegenTest` now keeps the 60-second case as a standing witness precisely so the rule does not
  > read as unmotivated to whoever finds it next.

  **The obvious round-trip test would also have been a false law.** `perTick` and `perSecond` are not
  exact inverses: `(x*20)/20` round-trips for every value tried, `(x/20)*20` does not — it fails for
  `1.6666666666666667`, precisely the per-second figure a person writes by hand for this pool. The
  derived base is `…665`, not `…667`. `ManaRegenTest` asserts the round trip only on values measured
  to survive it and carries a standing `assertNotEquals` against the one that does not.

- **THE `void` vs `boolean` RULE WAS PINNED ON THE WRONG AXIS, and Slice 1 wrote it that way.**
  `healthRegenTarget`'s javadoc said health regen can return `void` because it is "a RATE with no
  current anywhere". That is a **proxy, not the cause**. `manaRegenBonus` is the counter-example: also
  a rate, also no current of its own, and it needs both a boolean and a pin.

  The real axis is **eager vs lazy-integrated**. `HealthRegenSystem` pays `rate × dt` every second, so
  nothing is ever accrued-but-unpaid to re-price. `ResourcePool.regenerated` computes
  `amount + elapsed × rate` on READ, so raising the rate pays the new rate for ticks that already
  elapsed — a player empty for twelve seconds has accrued 20 mana, and equipping a rate-doubler makes
  the next read say 40. Both javadocs are corrected at the source.

- **`ManaTransition` exists because of a trap no inline version could be tested for.** Written
  `if (reconcileMax(...) || reconcileManaRegen(...))`, `||` short-circuits: on any tick where the
  ceiling changed, the RATE reconcile never runs and regen modifiers silently stop converging. Inside
  the paper loop nothing can observe that. Extracted to core — every argument was already a core type
  — it is a unit test that reddens with `expected: <2.0> but was: <0.0>`.

- **Three of that class's five mutations are silent in production**: the short circuit stops a stat
  converging, an unconditional pin freezes regeneration entirely (the 2b `asOfTick` lesson, now
  applying to the rate too), and a missing rate pin grants free mana on equip. **None would fail a
  boot gate that only checks "does mana go up".**

- **Still not done here:** mana persistence (`storage/` still never mentions mana; a rejoin starts a
  player full), per-archetype base `MAX_MANA`/`MANA_PER_TICK`, and showing the rate on the action bar.

- **THE BASE RATE DOES NOT SCALE WITH A RAISED CEILING, and this slice did not fix it.**
  `MANA_PER_TICK` is derived from `MAX_MANA` to mean "a full bar in `MANA_REFILL_SECONDS`". With Mana
  Bank at +120 the ceiling is 220 while the rate stays base, so an enchanted player takes
  **220 seconds** to fill — the enchant makes their bar bigger and their refill proportionally slower.
  Shipped behaviour, predating this slice and untouched by it. Whether the rate should scale with the
  ceiling is an archetype-content decision, not a regen-lift one, and it wants deciding alongside
  per-archetype `MAX_MANA` rather than before it.

  > **Updated by Stats Slice 3's rebalance.** The base moved from a 60-second to a 100-second refill,
  > so the Mana-Bank fill time moved with it: it was 132 s, it is now 220 s. The coupling is
  > unchanged — the number simply tracks the base, which is the point of recording it as a ratio
  > rather than a constant.

### Stats, Slice 1 (Health Regen) — what it created or exposed

- **BOOT GATE RUN AND PASSED, 2026-08-31**, operator-confirmed: rows 1, 2, 3, 6, 7, 8, 11 pass.
  **Row 4 returned its STOP signal, and that is the slice's main finding rather than a failure.**

- **CANCELLING THE `SATIATED` REGAIN DOES NOT STOP VANILLA CHARGING EXHAUSTION FOR IT.** Measured on
  Paper 26.1.2: with our heal cancelled and no charge of our own, a fed idle player's saturation
  still drained in **~4–5 seconds**. Vanilla drains saturation regardless of whether its regen tick
  was allowed to heal.

  The slice was designed around the opposite premise. The saturated window was to charge exhaustion
  per HP healed, justified as **restorative** — restoring the drain that suppression removed. That
  premise is **unfounded on this build**: nothing was removed, so the charge would have been a second
  one and the drain would have doubled.

  **The design got what it wanted for free.** Food still gates the rate — fed you regenerate at the
  saturated tier, and once vanilla has drained the saturation you drop to the floor. The two-tier
  fed/hungry economy is vanilla's drain plus our multiplier, with no custom cost anywhere.

  `EXHAUSTION_PER_HP`, `HealthRegen.exhaustionFor`, the `setExhaustion` call and both their tests were
  **removed, not shipped dormant** — a constant sitting at 0 with a live method behind it is a
  mechanism nobody can see is dead.

  **This is the row that justifies the whole "witness the premise before you build on it" ordering.**
  The measurement was sequenced deliberately before the commit it would have authorized: commits 1–6
  shipped with the constant at 0, so the gate could observe *suppression in, charge off*. Commit 7 was
  never written. Had the constant shipped at its derived 1.2, the doubled drain would have looked like
  a tuning problem rather than a false premise, and the number would have been tuned down toward zero
  one gate at a time without anyone learning why.

- **`SATURATED_MULTIPLIER` is 5.0, not the planned 4.0** — a fed player at the base rate regenerates a
  round **1.0 HP/s**, dropping to the 0.2 HP/s floor when saturation runs out. Retuned after the gate,
  so the *mechanism* is boot-witnessed but this *number* is not: rows 2 and 3 were run at ×4.

- **THE POTION REROUTE OVERHEALS AT HIGH MAX HP — REVISIT.** `RpgListeners.onRegainHealth` translates
  a cancelled `MAGIC`/`MAGIC_REGEN` amount through `HeartScale.customFromHealthPoints`, which scales
  the heal to a PROPORTION of custom max. That is right near 100 HP — a 4-point potion is two hearts,
  so 20 HP — and badly wrong above it: at a Growth-raised ceiling the same potion heals **300+**.
  Proportional was chosen over 1:1 because 1:1 makes every potion worthless as ceilings rise; the
  answer is neither, and it needs **a cap or a fixed custom heal amount** in a later slice.

- **Row 5 was dropped** (it witnessed the exhaustion charge, which no longer exists). **Row 12**
  (peaceful `REGEN`) stays in the exhaustive switch but is low-priority and was not run — the target
  server is never on peaceful, and at `difficulty=easy` that arm is unreachable.

- **`applyHeal` was vanilla-only and healed ZERO custom HP** — a shipped silent no-op, closed here.
  See the entry further down, now marked closed. What remains is that the port carries no `sourceId`,
  so a rerouted or ability heal cannot credit anyone.

- **The `_TEMP` fixture table in this file had gone stale by two** before this slice touched it. See
  that entry: it now lists eight, and carries the grep that would catch the next drift along with the
  trap in that grep (it returns nine; the ninth is the already-retired `swing_TEMP`).

### Armor, Slice 2a (the gating axis, Protection and Growth) — what it created or exposed

- **BOOT GATE OWED.** Eleven rows, in `PLAN-armor-slice-2a.md`. Row 6 is the discriminating one: with
  Protection active the armor bar must be a PARTIAL FILL, not empty. Rows 4, 5 and 7 -- tooltip,
  `⛨` field, damage taken -- all pass whether or not the Defense/nativeArmor split landed, because
  the stat and the mitigation are correct without it. Only the bar breaks, and it breaks to EMPTY.

- **THE COMPILER CATCHES THREE OF EIGHT SITES, AND FOUR PLACES IN THIS REPO SAID OTHERWISE.**
  `RpgCommand`, `ArmorDefinition`, this file and `PLAN-armor-slice-1.md` all claimed adding a
  `GearClass` constant "is a compile error in `GearClassLabel`'s two exhaustive switches and in
  `GearClassTest`'s axis enumeration". Accurate as far as it goes and badly incomplete.
  `GearClass.of` switches `WeaponClass`, not `GearClass`, so it does NOT break; `fromName` is a
  `values()` loop that silently begins accepting `class: armor` the instant the constant exists.

  Two compile errors, one runtime failure, and **five silent sites** found by hand:
  `HeldGear.gearClass()` kept returning null; `HeldGear.effectSuffix()` kept returning "";
  the `/rpg enchant show` arm kept refusing; `EnchantMenu.PlacedGear` was a TWO-WAY TERNARY that
  would have minted a helmet as a shield; and the damage gate below. `GearClassTest` now records the
  count, because it is very nearly the only thing that notices.

  **Do not repeat the claim that the compiler covers this.** It covers the exhaustive switch
  EXPRESSIONS and nothing else; the rest is a checklist. Generalised under "ENUMERATE THE AXIS" in
  Rules for this work, which now carries the audit that says which switches are actually safe.

- **THREE INSTANCES OF ONE DEFECT, and they are consolidated under "ENUMERATE THE AXIS, NOT THE
  CASES YOU CURRENTLY HAVE" in Rules for this work rather than kept as three incident notes.**
  `ANY_BUT_SHIELD` was a denylist gate that admitted every kind nobody had thought of, so
  `effect: damage` + `class: armor` loaded clean and could never fire. `requireGate`'s switch was a
  STATEMENT, so a new `Gate` constant would have fallen through to no validation at all -- a second
  instance of the REFLECT bug sitting three methods below the comment describing it. And
  `"inert: a " + GearClassLabel.of(...)` hardcoded the article, shipping **"a Armor enchant"** to
  players once a vowel-initial label existed.

  All three were rules written against the values that existed when they were written. None was
  caught by a compiler or by a test. The article one is the teaching example because it shipped and
  slipped everything, including the golden -- see the consolidated lesson for the rule and for the
  standing audit it now carries.

- **ONE MAP TO TWO JOBS IS GONE, and the arithmetic is pinned.** `DefenseModifierItems.scan` returns
  a `Worn` record with the stat map and the native sum from ONE walk. Two methods would be two walks,
  and a player swapping a piece between them would get a stat and a bar computed from different
  equipment.

  `DefenseTest` gained the case it never had: every prior `barModifier` assertion passed the SAME
  VALUE TWICE, because until an enchant could add Defense the two arguments were always equal. Feed
  defense where nativeArmor belongs and the attribute lands at **-28.82**, clamped to 0 -- an EMPTY
  bar on the most-armored player in the game, with stat, mitigation and tooltip all still correct.

  Executed, not derived, and the identity is **NOT bit-exact**:
  `nativeArmor + barModifier(56, 20)` is `7.179487179487179` against `armorBarPoints(56)`'s
  `7.17948717948718`.

- **TWO WAYS TO GET A SECOND STAT SOURCE WRONG, BOTH SILENT.** Both were designed around rather than
  discovered, and both are worth knowing before adding a third source to any stat:

  1. `ModifierReconciler.reconcile` removes every applied source ABSENT from the map it is handed. It
     is exhaustive per call, so two calls against one target -- one per source -- have each wipe the
     other's, leaving whichever ran last. There must stay exactly ONE reconcile call per stat per
     tick; merge first.
  2. `Stat.putModifier` is put-or-REPLACE. Two sources on one key means the second silently wins.

  Defense sidesteps both by keeping ONE ENTRY PER SLOT whose value is that piece's total. Max health
  could not: `HealthModifierItems` walks ALL slots on bare slot names, so Growth needed the **first
  namespaced source key in the codebase** -- `"growth:CHEST"`.

- **`percent_by_level` IS NOW `value_by_level`, and `BlockEnchantItems` IS NOW `EnchantValues`.** Two
  names that were imprecise before they were wrong. The curve field never divided -- every `/100`
  lives in the mechanism -- and `BlockEnchantItems` had been parameterized by `EnchantEffect` since
  2b and never knew anything about blocking. Protection and Growth turned both into lies, and the
  alternative to renaming `EnchantValues` was a second copy of its sum loop for armor. **Structure
  may be duplicated here; logic never is.**

  `DamageEnchants.percentAt` deliberately KEPT its name: a damage curve genuinely is a percent, and
  leaving it alone preserves the zero-edit `DamageEnchantsTest` pass that `EnchantCurve`'s javadoc
  names as the faithfulness check for the original lift.

  Breaking content-schema change, no alias. An old-key file parses as having no curve, which
  `requireCurve` refuses -- a named, skipped file rather than an enchant that silently grants nothing.

- **PROTECTION NEEDS NO CLAMP, AND THAT IS PROVEN RATHER THAN ASSERTED.** Bulwark needs
  `Shield.clamp` because `block_dr 0.9 + 0.15` clamps to 1.0 and a 15.0 hit passes 0.0 -- total
  immunity, reachable. `Defense.applyDefense` is asymptotic, so no quantity of points can get there:
  `ProtectionTest` walks +100, +1000, +10000 and +1e9 and a hit still lands every time.

  The real ceiling, executed: full diamond with Protection III in all four slots is 56 points ->
  **35.8974358974359%** reduction, against bare diamond's **16.666666666666666%**. Roughly double,
  and still not halving a hit. **The per-piece value is the balance lever, not the stacking.**

- **GROWTH IS THE ONLY ENCHANT THAT CAN TAKE HEALTH AWAY**, and that is correct rather than a bug. A
  player at full health who removes a Growth piece WILL see their hearts drop, because
  `HealthState` clamps current on a max decrease -- which is what stops equip/unequip cycling being
  a free heal. `growth.yml` says so out loud so it is not reported as a defect.

  Three rules, all pinned: equipping is headroom (100/100 -> 100/130), removing clamps down
  (130/130 -> 100/100), removing while hurt does NOT (40/130 -> 40/100). The third exists because
  without it the second would pass on an implementation that set `current = max` on every removal --
  making taking armor OFF a heal.

  **Growth and Protection are NOT equally scaled**, though they read as siblings on a tooltip: +36
  Defense is bent by an asymptotic curve into roughly twice the mitigation, while +120 Max Health is
  a straight doubling of the pool because nothing curves it. If Growth is ever retuned that is the
  reason, and `growth.yml` is the lever.

- **A theoretical hazard, recorded rather than guarded.** A max-health source that transiently
  vanished and returned would clamp current on the way down and NOT restore it on the way up --
  permanent HP loss per flicker. Not reachable today: the registry is fixed at boot and the scan
  reads the slot the piece is actually in, so a Growth source only disappears when the piece does.
  Building a guard for it would be building one for a case that cannot occur.

- **The armor roll pool is 3 in 2a** -- Protection, Growth, Unbreaking -- the same size as a shield's,
  so `EnchantRoll.MAX_CANDIDATES` is not yet exceeded. **Mana Bank takes it to 4 in 2b: the first
  shipped pool ever past the cap.** `candidateCount` caps correctly, but that regime has never
  shipped, and `EnchantRoll`'s javadoc still says a shield's 3 is the largest.


### Armor, Slice 2b (Mana Bank, and per-player Max Mana) — what it created or exposed

- **BOOT GATE RUN AND PASSED, 2026-08-31: all eleven rows, operator-confirmed.** Reported at that
  granularity -- eleven of eleven, including both discriminating rows by name.

  **Row 5 is the one that carries the slice.** Row 4 -- equip at full mana, expect headroom --
  **passes on the pre-2b behaviour by accident**, because a player who has already cast has a stored
  entry and gets headroom either way. Row 5 is the same action by a player who has NEVER cast this
  session, the absent-means-full case, and it is the only row that fails if the pin is missing. It
  passed, so the pin fires.

  **Row 9 is the only row that reaches `tryConsume`'s guard**; every other row reads through
  `current()`. An ability edited past 100 in the deployed tree was uncastable bare and castable with
  Mana Bank on, which is the per-player ceiling proven on the spend path rather than the display one.

- **TWO PLANNED MUTATIONS DID NOT REDDEN, AND FINDING THAT OUT IS THE MAIN THING THIS SLICE
  PRODUCED.** Both were written into the plan's mutation table as though the checks existed. Both
  compiled, applied, and left every test green. This is the file's own recurring defect -- a check
  that cannot fail is indistinguishable from one that passes -- caught this time only because the
  mutations were RUN rather than reasoned about.

  1. **Deleting `setCurrent`'s `Math.min` left all ten tests green.** The test written to guard the
     unequip clamp reads through `current()`; `current()` calls `regenerated()`; `regenerated()`
     ends in its own `Math.min` against the same ceiling. So it reports the clamped number whether
     or not the clamp was ever WRITTEN. Its own comment claimed it "reddens where a read-only test
     would not."

     The stored amount is observable in exactly one window: when the ceiling rises again with **no
     pin behind it**. The obvious fix -- unequip, then re-equip -- does not work either, and that
     was measured too: the re-equip pins a value it just read back, which is already clamped, so
     the mutation heals itself. `setCurrentClampsAtTheMomentOfWRITING...` asserts the method's
     contract in isolation instead.

  2. **Resolving the ceiling inside `compute()` broke nothing.** The plan claimed it would redden
     `concurrentSpendsCannotOverdrawThePool`; all 22 pool tests stayed green. A resolver over a
     plain map neither deadlocks nor returns anything different, so the deadlock story was never
     the observable part -- the ARITY is. Two reads straddling a gear change make "the guard
     passed, then the spend refused" reachable, which reaches the player as **"needs 110, you have
     130"**. `tryConsumeAsksTheResolverEXACTLYONCE...` counts the calls.

  Both `ResourcePool` javadocs asserting the old stories are corrected in place. **A mutation row
  that never reddens is a check that did not run**, and two of six here were exactly that.

- **THERE WERE FOUR READS OF THE GLOBAL MAX, NOT THREE.** The fourth was a verbatim duplicate of
  `current()`'s absent-owner branch, nested inside `tryConsume`'s `compute` mapping function, which
  is why a grep for `max` found three and reading found four. `tryConsume` now resolves once at the
  top and passes the local to `regenerated`.

- **THE INCREASE SIDE WAS THE BROKEN ONE, AND THE DECREASE WAS NEARLY FREE.** The opposite of what
  the brief assumed. `ResourcePool` stores *(amount, tick)* rather than a current value, so
  `Math.min` in the regen path already pulled current down on the next read -- the explicit clamp is
  about STATING it, not about the arithmetic.

  The increase had a live inconsistency: an owner **with** an entry got headroom, and an owner with
  **no** entry read the new ceiling instantly, because absent means full. **The same enchant behaved
  two ways depending on whether the player had ever cast.** One pin-on-change mechanism serves both
  directions.

- **A RECONCILE THAT ALWAYS REPORTS "CHANGED" STOPS MANA REGENERATING ENTIRELY.** Found while
  writing the test, not in review. `reconcileMaxManaModifiers` returns `boolean` where its siblings
  are `void` precisely because the pin must fire on a real transition and nothing else: the loop
  runs four times a second, and a pin every tick re-stamps the entry's `asOfTick`, so the elapsed
  count never grows. Lazy regeneration makes an unconditional write a **silent, total** loss of
  regen, with no error and a stat block that still reads correctly.

- **THE PLAN'S `stats.tracks(owner)` GUARD WAS DESIGNED OUT RATHER THAN WRITTEN.** `CombatantStats.max(id)`
  throws for an untracked id, so the plan guarded the resolver. Modelling the stat as a **bonus with
  base 0.0** rather than a total with base 100.0 makes `maxManaBonusValue` total by construction --
  the same shape `defenseValue` already had -- so the resolver cannot throw and there is no guard to
  forget. A resolver called from inside a cast must be TOTAL; the base stays in paper, which is
  where the archetype pass wants it anyway.

- **THE ROLL ROSTER FIXTURE HAD NEVER SEEN ARMOR.** `EnchantRollTest.ROSTER` claimed to be "the
  shipped roster, id for id and class for class" with six entries against content's eight, missing
  `protection` and `growth` -- so the file guarding the roll had never exercised the one class about
  to pass the candidate cap. Stale since `82b0959`, independent of this slice, and the same
  discovery-that-finds-nothing shape recorded twice elsewhere in this file: a fixture that claims to
  mirror content and silently does not.

- **ARMOR IS THE FIRST SHIPPED POOL TO EXCEED `MAX_CANDIDATES`.** Four rollables, cap of three, and
  the clamp does the job it was written for. Pinned by name so it reads as intended rather than as
  an oversight. Raising the cap would not let a player run all four anyway (`SLOTS` x one active
  each is three), and it is **not the one-constant change it looks like**: `rawSlotFor` puts a
  fourth candidate on row 5 at 47/**49**/51, `INPUT_SLOT` is 49, and `renderCandidates` writes
  unconditionally -- so it would paint a candidate icon over the player's own armor and `onClose`
  would hand the icon back.

- **THE "ENUMERATE THE AXIS" CHECKLIST CONFIRMED RATHER THAN ASSUMED.** Adding `MAX_MANA` produced
  exactly three compile errors -- `EnchantDefinition`'s `gate` and `curved`, `EnchantEffectLine`'s
  `bare` -- all of them switch EXPRESSIONS, all of them converted in 2a. The one statement the 2a
  audit flagged (`RpgCommand`'s `switch (op)` over `EnchantOp`) was untouched because no `EnchantOp`
  was added. That is the lesson working: the conversions made this a compile error instead of a bug
  report.

- **`EnchantCost`'s javadoc is now half-wrong, deliberately.** It cites `MAX_MANA` as the archetype
  of "a uniform system knob rather than per-enchant content". The BASE stays uniform; the resolved
  value does not. Whoever does the per-archetype pass should read that paragraph before quoting it.

- **Still not done here:** mana persistence (`storage/` never mentions mana, and a rejoin still
  starts a player full), per-archetype base `MAX_MANA`, and raising `MAX_CANDIDATES`.


### The gear extraction (GearDefinition/GearItems) — what it created or exposed

- **BOOT WITNESSED, 2026-08-30: the `GearRefresher` rebuild works for all three gear kinds.** A
  minted item carrying OLD content rebuilt to the new definition on join, confirmed live for a
  weapon, a shield AND a piece of armor, after a `--no-build` re-boot against an edited deployed
  tree. Operator-witnessed. That is the row the extraction's fourth commit owed: shields and armor
  had never rebuilt from content before it, so only the weapon leg of this was previously true.

  The other three commits needed no boot row -- `GoldenLoreTest` covers them, and it is a stronger
  check than a boot could be for that half.

- **THE TWO CONTENT TREES, AND HOW A TUNING CHECK GOES WRONG IN BOTH DIRECTIONS.** Recorded because
  it cost real confusion once and the two failures look identical from in-game ("my edit did
  nothing"):

  | tree | who reads it | editing it |
  |---|---|---|
  | `paper/src/main/resources/content/` | `GoldenLoreTest` renders from it | **reddens the golden and breaks the build** |
  | `run/plugins/Rpg/content/` | the server, at BOOT | what a tuning check must edit |

  So the procedure is: edit the **deployed** tree, then `./scripts/dev-server.sh --no-build`.

  **And the re-boot is not optional, because there is no reload path.** `GearRefresher.refresh`
  re-mints from the registry loaded at boot, not from the files on disk -- `/rpg refresh` rebuilds
  items from definitions already in memory. So editing the deployed tree WITHOUT re-booting changes
  nothing the server can see, and `/rpg refresh` will cheerfully report a non-zero count while
  rebuilding every item to exactly what it already was.

  Written into `GoldenLoreTest`'s javadoc as well, since that is where someone lands when the golden
  reddens on an edit they thought was harmless. **Regenerating the golden to make a source edit green
  is only correct when the tooltip change is the INTENT and ships. It is never how to run a tuning
  experiment.**

  The obvious follow-up -- a real `/rpg reload` that re-reads the deployed tree into the registries --
  is NOT scoped here. It is a genuine feature with its own hazards (what happens to an in-flight
  ability whose definition vanished mid-cast) and deserves its own pass rather than being smuggled in
  as a convenience.

- **THE GOLDEN CAUGHT WHAT 376 HAND-WRITTEN TESTS DID NOT, AND THAT IS MEASURED.** Mutation: drop the
  explicit `ITALIC=false` from `GearLore.blank()`, so every spacer line in every tooltip silently
  changes decoration state. **Every one of the 376 existing tests stayed GREEN; the golden was the
  only thing that reddened.** The hand-written lore tests assert the tooltip's SHAPE and remain the
  primary guard -- they say what is true and why. What they cannot do is notice an incidental change
  nobody thought to assert, which is exactly what a behaviour-preserving refactor most needs a guard
  against.

- **The abstraction was designed from THREE examples and the third earned its keep immediately.**
  `GearDefinition`'s five members are exactly the intersection of the three records, and every
  candidate sixth member failed against one of them: not a stat (attack damage, block DR and defense
  are three different quantities, and the shield's is a FRACTION where the other two are absolute);
  not a class (`WeaponClass` is required on a weapon and absent on the other two, `GearClass` has no
  armor constant); not durability (armor's is vanilla's); not a lore builder (different inputs each,
  returning Components, which cannot exist in `core`). Designing this from two would have produced a
  wider interface that the third shape then contradicted.

- **Sealing `GearDefinition` is what makes a fourth gear kind a compile error rather than a silent
  no-op.** `GearItems.remint` is an exhaustive switch with no default arm: a new kind stops the build
  until someone says how it re-mints. The alternative -- a catch-all returning the item unchanged --
  is the quiet failure, because the item then keeps stale lore forever and nothing ever says so.

- **What deliberately did NOT move, and it is most of the interesting code.** `mint` itself (a weapon
  pins an attack modifier and hides it, a shield pins and hides nothing, armor hides vanilla's armor
  lines -- and `ShieldItems`' javadoc argues explicitly AGAINST the flag armor requires);
  `materialOf` (three fallbacks, each load-bearing for its own reason); `applyLore`; and the stat
  line. A "generic stat line" would have to take the label, the value, the colour and a composition
  rule, which is every part of it. The rarity footer's NOUN stays the caller's for the same reason:
  class-derived, literal, and slot-derived are three right answers.

- **`ArmorConsistency`'s duplicated vanilla-armor read is paid off**, as the armor slice said it would
  be. It now calls `DefenseModifierItems.vanillaArmorPoints`. One copy matters more here than in most
  places: a check reading that number a DIFFERENT way would verify content against a value the stat
  does not use, and would then report a clean run while every armor tooltip lied.

- **Still owed, and unchanged by this pass:** no `ContentValidator` for shields or armor; the missing
  crafting hook (a vanilla shield still gives zero custom protection); and the one-map coupling in
  `PlayerHealthSystem` that Slice 2 must split first.


### Armor, Slice 1 (mintable pieces that source Defense) — what it created or exposed

- **BOOT GATE RUN AND PASSED IN FULL, 2026-08-30 -- all eleven rows.** Row 1 the machine's, rows
  2-11 the operator's. Rows 5-7 are the ones that mattered: they re-take the Defense pass's own
  numbers (`⛨` 3 -> 11 -> 17 -> 20, ~83 off `/rpg damage 100`, a bar ~1/6 full) on MINTED diamond
  rather than vanilla diamond, which is the slice's whole claim witnessed instead of asserted.
  Recorded at the granularity reported -- the per-row figures were not captured into this record, so
  what stands is "the operator ran these and they passed", the precedent shields Slice 1 set and 2b
  restated.

- **AND WRITING THE GATE UP SHARPENED A ROW THE PLAN HAD HALF-RIGHT.** The plan said to read rows 4
  and 7 TOGETHER because neither alone separates "`HIDE_ATTRIBUTES` missing" from "modifiers
  stripped". Working the stripped case through the actual code, the pairing is lopsided and **row 7
  is doing all of the work**: because `armorOf` reads the MATERIAL's defaults and never the stack,
  stripping the modifiers leaves row 4 passing (no lines to show), row 5 passing (`⛨` still 20) and
  row 6 passing (mitigation runs off the stat). Only the bar breaks -- `barModifier(20, 20)` is
  `-16.67` onto a stripped base of 0, which Minecraft clamps to 0, so the bar reads **EMPTY**, not a
  sixth. Row 7's three reachable states are ~1/6 correct, empty if stripped, full if the override
  never ran; all three are eyeballable, which is why that row being observed rather than measured
  costs nothing. **Row 4 is very nearly vacuous alone -- it passes under the exact failure it was
  written to catch.**

- **THE DEFENSE SOURCE DID NOT CHANGE, AND COULD NOT HAVE.** `DefenseModifierItems.armorOf` reads
  `ItemType.getDefaultAttributeModifiers(slot)` -- the MATERIAL's vanilla points, blind to anything
  on the ItemStack. Since a minted piece's Defense mirrors exactly those points, a minted diamond
  helmet and a plain one contribute the identical 3 through the identical code path. That file's own
  javadoc predicted this slice ("when it lands, this is the one method that has to learn about it");
  the answer is that it does not have to yet. It learns in Slice 2, when Protection diverges the two.

  Verified by diff, not by argument: twelve files are byte-identical to `59400c0` -- `WeaponItems`,
  `ShieldItems`, `WeaponLore`, `ShieldLore`, `WeaponDefinition`, `ShieldDefinition`, `Defense`,
  `DefenseModifierItems`, `ArmorBarOverride`, `PlayerHealthSystem`, `CombatantStats`, `EnchantMenu`.

- **ONE MAP STILL SERVES TWO JOBS, AND SLICE 2 MUST SPLIT IT.** `PlayerHealthSystem:181-184` feeds
  `DefenseModifierItems.desiredModifiers(player)` to BOTH `reconcileDefenseModifiers` (the stat) and
  `total(...)` (the `nativeArmor` the bar cancels). Sound only while a piece's Defense equals its
  material's vanilla points. `Defense.barModifier`'s javadoc guards the OTHER half of this -- never
  read the live attribute, which the code already does correctly -- and says nothing about this one.
  **Protection (+3/6/9) breaks it by design, so splitting these two reads is Slice 2's first task.**

- **`defense:` in content is DISPLAY-ONLY, and `ArmorConsistency` is what keeps that honest.** The
  authored number feeds the tooltip; vanilla feeds the stat, the mitigation and the bar. Nothing
  makes them agree, and a mismatch is invisible from every vantage point: the tooltip reads
  "Defense: 9" and looks right, the action bar reads 8 and looks right, the bar fills to the DR of 8
  and looks right, the damage matches 8 and looks right. `core` cannot reach an `ItemType` registry
  and `paper` has no live server in the unit loop, so boot is the ONLY moment the two numbers share a
  JVM. Hence a boot check rather than a test.

  It also warns on ZERO pieces, and that branch is the load-bearing one: if `content/armor` loads
  empty, Defense keeps working (it is sourced from vanilla, not from a tag), so every other signal
  reads healthy and a silent "0 mismatches" would be the strongest-looking evidence that nothing is
  wrong. Mutation-tested: made to return 0 silently, two tests redden.

- **`ArmorSlot`'s constant names are a WIRE FORMAT, not a naming choice.** `DefenseModifierItems`
  keys its desired map by `EquipmentSlot.name()` and `ModifierReconciler` matches sources by that
  string; `ArmorConsistency` does `EquipmentSlot.valueOf(slot.name())`. Renaming `HEAD` to `HELMET`
  reads better and compiles everywhere. `ArmorSlotTest` restates the four Bukkit tokens as literals
  -- core cannot import `EquipmentSlot`, which is why the enum exists at all -- and the mutation
  reddened three tests. It also exposed a second consequence nobody had listed: `fromName("helmet")`
  starts resolving, so the rename would silently change content parsing too.

- **`HIDE_ATTRIBUTES` is display-only, and STRIPPING the modifiers instead is the quiet way to break
  the bar.** The piece must keep granting its vanilla armor, because that sum is `barModifier`'s
  input. `armorOf` reads the MATERIAL's defaults, not the stack's, so `setAttributeModifiers(empty)`
  would leave it reporting 20 for a full diamond set while the live attribute is 0: bar off by the
  whole set, Defense stat still right, nothing failing. Recorded in `ArmorItems`' javadoc.
  `ShieldItems` argues AGAINST the flag ("nothing to hide"); armor is the opposite case, and the
  second line it hides -- Armor Toughness -- advertises a stat this project does not implement.
  **Boot rows 4 and 7 must be read together**: neither alone tells "flag missing" from "modifiers
  stripped".

- **One file, four definitions -- armor's deliberate divergence from every other loader.** Rarity and
  flavour are per-tier properties; 24 files repeating them would put one fact in four places. Ids come
  from each piece's `material` token, NOT the filename, and that is what keeps leather correct:
  vanilla's leather pieces are **Cap, Tunic, Pants, Boots** against materials named `leather_helmet`
  and friends, so a name-derived id would have produced `leather_cap` for a `leather_helmet`.
  `display_name` is authored per piece for the same reason. Their footers still read "Common Helmet"
  -- the footer says what KIND of gear an item is, the job "Rare Melee Weapon" does on a weapon.

  `parseTier` walks the SLOT AXIS, not the file's keys, so a missing slot is a named refusal and a
  typo'd `foot:` is never read. A bad tier is refused WHOLE, and the warning says it took four pieces
  with it -- a roster four short does not look broken in-game, it looks like a tier nobody authored.

- **Armor is enchant-COMPATIBLE, not enchant-ROLLED -- the same line shields drew in their Slice 1.**
  `EnchantRoll` is keyed on `GearClass`, which has no `ARMOR` constant. Adding one is a compile error
  in `GearClassLabel`'s two exhaustive switches and in `GearClassTest`'s axis enumeration, BY DESIGN,
  because it forces the decision about whether armor is one class or four. `HeldGear.gearClass()`
  returns null for armor and every caller gates on `isArmor()`; `/rpg enchant show` refuses armor
  rather than passing null into `EnchantEffectLine`'s no-default-arm switch. **Do not "fix" that by
  handing it SHIELD** -- a helmet would become eligible for Bulwark and Thorns.

- **`EnchantMenu` was left untouched on purpose**, against a plan that proposed a third `PlacedGear`
  arm. `acceptsInput` already refuses armor at the door with an accurate message and both
  `resolveGear` callers null-guard. The table is the roll/unlock UI and armor is not rolled here, so
  an armor arm would be an arm that immediately refuses -- and would make `gearClass()` nullable in a
  second place.

- **NETHERITE IS NOT A HIGHER TIER THAN DIAMOND**, and that is a decision rather than an oversight.
  Vanilla gives them identical armor points; what netherite adds is armor toughness and knockback
  resistance, neither of which this project models. Rarity says what gear is worth HERE, so ranking
  netherite above diamond would be the tooltip promising a difference the mechanics do not deliver.

- **A mutation reported nothing and it was NOT a pass.** `./mvnw -pl paper test-compile` exited 1
  with *"Could not collect dependencies ... rpg-parent:pom ... was not found"* -- a reactor
  resolution failure, not a compile error, and the `grep 'error:'` used to read it matched none of
  it. The test never ran and the empty output read exactly like green. **Paper-only runs need `-am`,
  and the surefire property is `-Dsurefire.failIfNoSpecifiedTests=false`, not the bare
  `-DfailIfNoSpecifiedTests`** (which surefire 3.5.2 ignores, then aborts in rpg-core with "No tests
  matching pattern"). Every mutation verdict in the plan doc was re-taken from the REPORT FILE.

- **THE GEAR EXTRACTION IS NOW OWED, AND IT IS THE NEXT PR, NOT A SOMEDAY.** Armor is the third shape
  -- `WeaponDefinition`/`ShieldDefinition`/`ArmorDefinition`, `WeaponItems`/`ShieldItems`/`ArmorItems`,
  `WeaponLore`/`ShieldLore`/`ArmorLore` -- which is the trigger this file, all three shield plans and
  the Slice-1 squash commit all name. The third copy is transient BY DESIGN; leaving it is drift.
  Single gate for that PR: **minted weapon, shield and armor byte-identical before and after.** It
  should also fold the whole-number trimmer (now FOUR copies: `WeaponLore.number`,
  `WeaponLoreLines.trimNumber`, `ShieldLoreLines.trimNumber`, `ArmorLoreLines.trimNumber`) and
  `ArmorConsistency`'s duplicated vanilla-armor read, which repeats `armorOf`'s `ADD_NUMBER` sum
  solely so this slice could leave `DefenseModifierItems` byte-identical.

  **And it carries the two owed refreshers with it** -- `ShieldRefresher`, outstanding since shields
  Slice 1, and `ArmorRefresher`, owed from this one. Neither is a coincidence of scheduling: a
  refresher is `WeaponRefresher`'s shape over a definition and a re-mint, which is exactly the pair
  the extraction is factoring. Writing two more copies BEFORE the abstraction exists would make five
  shapes to reconcile instead of three, so they wait for it rather than the other way round.
  `RefreshVerdict` is `WeaponDefinition`-typed today (`RefreshVerdict.Remint` carries one) and is the
  piece of that job with actual design in it.

- **Deferred with armor, each a decision rather than an omission:**
  - **Turtle helmet.** A HEAD-only seventh tier that breaks the 6x4 grid the per-tier loader is built
    on, and it grants Water Breathing -- a vanilla status effect this project does not model, so
    minting it forces an out-of-scope call about whether the effect survives. Waiting on
    status-effects-on-gear (`DESIGN-status-effects.md`), a named dependency.
  - **Armor durability is vanilla's, untouched.** Weapons and shields own their wear; armor does not.
    And because mob melee is tokened to `0.01`, minted armor will barely wear at all.
  - **No `ArmorRefresher`.** Armor lore will not rebuild from content on rejoin. `ShieldRefresher` is
    still missing for the same reason; the extraction PR is the place to fix both at once.
  - **No `ContentValidator.validateArmor`.** Shields have none either.
  - **UNTAGGED VANILLA ARMOR STILL SOURCES DEFENSE, and always did.** Unlike a vanilla shield, which
    gives zero custom protection, a plain diamond chestplate works fully. Minting adds rarity, lore
    and an enchant container -- NOT mitigation. Said plainly so nobody later reads it as a bug.
  - **Non-tokened damage** (fall, fire, explosions, projectiles) never reaches custom HP -- no
    handler exists -- and `ArmorBarOverride` has already driven the vanilla `armor` attribute down to
    the DR value, so vanilla's own mitigation of those is computed against ~3.33 rather than 20.
    **Pre-existing from the Defense pass, not introduced here**, and it lands on vanilla health that
    `HeartBarRenderer` overwrites from custom HP.

- **Max Mana is NOT a reconciled stat, and Slice 2's Mana Bank is a real slice because of it.**
  `ResourcePool.max` is a single `final double` shared by every player, with no `ModifierTarget`, no
  `reconcileManaModifiers`, no scanner and no per-player state -- unlike `HealthState.max`, which has
  all four. Making it one needs a max-decrease clamp decision that `ResourcePool` currently gets for
  free from `Math.min` only because max never moves, and `ResourcePool.current` returns `max()` for
  an unseen owner, which reads a GLOBAL max today. **Verify this before planning Slice 2** rather
  than assuming Growth and Mana Bank are symmetric.


### Shields, Slice 2b (Thorns, the reflect) — what it created or exposed

- **BOOT GATE RUN AND PASSED IN FULL, 2026-08-30.** Row 1 from the boot log; rows 2-9 by the
  operator. The reflect fires out of the mob->player rider credited to the blocker, is independent of
  Bulwark, falls off the same predicate as the reduction, pays out a lethal kill, and the shield's
  Damage Reduction line reads as intended in the Defense colour.

  **ROW 4 IS MEASURED: the popup read 5 at Thorns III**, confirmed 2026-08-30. That is the most
  load-bearing figure in the gate, because it is the one thing NO unit test can reach -- whether the
  RIDER passed the pre-mitigation blow. `Thorns.reflected` and `EnchantCurve.percentAt` are both
  unit-tested, so the arithmetic was never in question; only the value selection at the call site was.

  A reading of 5 is produced by the shipped path and by nothing else. Executed against the real
  classes: off the pass-through it reads 3 (2 with Bulwark III on), and with the two `percentFor`
  effect arguments transposed in `resolve` it reads 2 (1 if both faults are present). **So the one
  figure settles two things** -- the reflect is off the pre-mitigation blow, AND the effect arguments
  are not swapped, a fault that is invisible to every unit test in the suite.

  It settles the second **regardless of whether Bulwark was equipped**, since every swapped or
  pass-through variant lands at 1, 2 or 3. The "Thorns alone" confound was still right to state -- it
  is what makes the row easy to read -- but this conclusion does not rest on it.

  **Rows 2, 3 and 5-9 stay operator-observed rather than measured**, at the granularity reported --
  the same call Slice 1 made for its armored-block row. None of them carries a rival reading a wrong
  implementation could plausibly produce, which is why row 4 was the one worth chasing a number for
  and they were not.

- **A FINALISATION PASS moved the names and the numbers, and every constant was RE-EXECUTED.** The
  enchant is `Thorns` (the design doc VACATED the name -- see below); the shield is `shield`, not
  `roundshield`; `block_dr` is **0.35**, so a 15.0 hit passes **9.75**; the stat is called **Damage
  Reduction** on the item and in Bulwark's effect line; its number renders in
  `StatsBarText.DEFENSE_COLOR` (GREEN), read off the HUD rather than picked, because a shield's
  reduction and armor's Defense are the same kind of number; and the reflect line is gone from the
  shield lore.

- **THE "THORNS" RESERVATION IS RESOLVED, NOT DUPLICATED.** `DESIGN-status-effects.md` held the name
  for a Nature propagation status that has no code, no content file and no slice. It yields: there is
  exactly one Thorns in this project and it is the shield enchant. **The four anti-loop safety rules
  travel with the MECHANIC, not with the word** -- they are about propagation, and the enchant does
  none of it -- so when that status is built it takes a new name and the rules go with it under that
  name. A grep for "thorns" now finds the enchant everywhere and the vacation note once.

- **LOWERING THE BASE TO 0.35 CHANGED THE ARGUMENT FOR ADDITIVE BULWARK, not just the numbers.** At
  0.5 the two REJECTED readings were bit-identical to each other (`0.525 / 0.55 / 0.575`), so the
  shipped shield could catch a wrong implementation but never say WHICH wrong rule it followed. At
  0.35 all three separate -- executed: additive `0.39999999999999997`, multiplicative `0.3675`,
  diminishing `0.38250000000000006`. The shipped base now discriminates the rule by itself.
  `BulwarkTest` keeps 0.5 as `LEGACY_HALF`, because a base where two rivals coincide is exactly the
  case a blind test would survive, and adds an assertion that the shipped base separates.

- **THE BULWARK FRACTIONS ARE NOT THE CLEAN NUMBERS THEY LOOK LIKE.** `0.35 + 0.05` in binary
  floating point is `0.39999999999999997`, and `+ 0.10` is `0.44999999999999996`. A 15.0 hit passes
  `9.000000000000002` at Bulwark I, not 9.0. The tooltip's one-decimal rounding is the only reason
  the item reads "40%" and "45%"; the tests carry the exact doubles and must not be tidied. This is
  the fourth time hand-predicted floats would have been wrong in this project.

- **The gate discriminators moved with the base.** The rejected off-pass-through reading now reads
  **1 / 2 / 3** rather than 1 / 2 / 2, because the pass-through is 9.75 instead of 7.5. All three
  rungs still separate from the correct 2 / 3 / 5, and III (5 versus 3) is still the clearest.

**The three original shield goals are complete**: block DR (Slice 1), Bulwark and the gear-gating
axis (2a), and now the reflect. See `PLAN-shields-slice-2b.md`. **Boot gate RUN AND PASSED IN FULL,
2026-08-30 -- all nine rows.**

- **THE REFLECT SEAM IS THE ONLY NEW THING, and it turned out to be a one-liner on proven rails.**
  Dealing custom damage to a second entity credited to a dealer is exactly what
  `onPlayerSweepAttack` has done since the sweep pass; what is new is the DIRECTION — out of the
  mob→player rider rather than the player→mob one. The value it deals, the popup it paints, the
  aggro it re-asserts and the kill credit it earns all come from the existing pipeline unchanged.

- **`ShieldExchange` EXISTS BECAUSE THE RULE WAS OTHERWISE UNTESTABLE, and that is a different
  reason from tidiness.** Thorns's one load-bearing rule -- it reflects a fraction of the
  PRE-MITIGATION blow -- lived in `RpgListeners.onMobMeleeAttack`, which cannot be unit-tested (a
  live `Player`, a live `LivingEntity`, a real `BLOCKING` modifier; no listener test exists or can).
  A pure `Thorns.reflected` test pins the arithmetic but cannot say WHICH value the rider passed.

  Moving the CHOICE into a pure core record makes the mistake reddening in two seconds instead of
  boot-only: `of()` takes the raw blow once and returns both numbers, so the reduction happens inside
  and **there is no `incoming` variable in the rider any more** to mis-pass. Measured -- the mutation
  reflects off the reduced figure and fails 4, including *"2.25 here means it reflected off the
  pass-through"*.

  **What it does NOT cover, so the coverage is not overclaimed:** the order the rider deals the two
  numbers, and the inline `requireOwned` throw that makes "reflect last" the safe placement. Those
  stay in the rider and stay boot-gated.

- **"APPLY LAST" IS RIGHT FOR THE THROW, NOT FOR TICK ORDERING -- an earlier reasoning was theatre.**
  Both `applyDamage` calls defer to their entity's next tick, so "the victim's damage lands first"
  holds on Paper by FIFO accident and is meaningless on Folia, where the two may be in different
  regions. Nothing observable depends on the order.

  What IS ordering-sensitive: `BukkitCombatant.of` runs **inline** and its first act is
  `Regions.requireOwned`, which throws off-region. Placed above the token, that throw skips
  `setDamage` -- so **vanilla's FULL damage lands on the player** -- and skips the custom hit as well.
  Placed last, a throw costs the thorns and nothing else.

- **THE VALIDATION SWITCH WAS NEVER EXHAUSTIVENESS-CHECKED, AND 2a's COMMENT SAID IT WAS.** Adding
  `EnchantEffect.REFLECT` was supposed to be a compile error at two sites. It was one.
  `EnchantDefinition`'s validation was a switch **statement**, and Java only enforces exhaustiveness
  on switch **expressions** -- so the new constant compiled clean and fell through to **no validation
  at all**: no curve rule, no gate rule. For a reflect that is an unvalidated negative percent
  reaching `stats.damage` and **healing the attacking mob**.

  Fixed by choosing the rules as VALUES -- two switch expressions pick the `Gate` and whether a curve
  is required -- and proven by dropping an arm: *"the switch expression does not cover all possible
  input values"*, BUILD FAILURE. **The lesson generalises: a no-default switch STATEMENT guarantees
  nothing.** Anywhere this codebase claims a switch is a compile-time gate, check which kind it is.

- **Two more silent-pass tests, both closed by Thorns being the first enchant that could.**
  `EnchantEffectLineTest` had ZERO coverage of the `BLOCK_DR` arm 2a added (`grep -c` returned 0)
  while its javadoc claimed it asserted "every arm" -- it asserted every arm its hand-listed fixture
  array knew about, the same discovery trap as the loader fixture. And `BlockEnchantItems`' effect
  filter was unguarded: 2a's cross-effect fixture was Unbreaking, whose curve is empty, so deleting
  the filter returned `0.0` either way and its own comment named that risk before agreeing with it.
  Bulwark and Thorns are the first two enchants that BOTH carry curves and bind DIFFERENT
  mechanisms; the mutation now fails with `expected: <15.0> but was: <45.0>`.

- **A shipped-content typo that no fixture could catch.** 2a's directory-scan fixture picks
  `thorns.yml` up for free and asserts the id SET -- so it catches a file the loader REFUSES, and
  nothing else. `thorns.yml` authored with `effect: block_dr` gates fine on `class: shield`, loads
  cleanly, and reddens nothing: Thorns silently becomes a second Bulwark. Closed by asserting the
  effect and curve by VALUE, proven by flipping the shipped file.

- **A shield's enchant pool is THREE, and the 3-candidate slot is finally real.** Bulwark + Thorns +
  Unbreaking, against two for every weapon class. So `EnchantMenuLayout.CANDIDATES == 3` stops being
  a constant pinned against another constant, and the distinctness rule (`remaining.remove(picked)`)
  runs at pool size 3 for the first time -- the third pick comes from a pool already shrunk twice.
  Rarity-weighting stays deferred, but it is now deferred BY CHOICE rather than by impossibility.

- **THE POPUP ROUNDS, so the gate reads 2 / 3 / 5 rather than 1.5 / 3.0 / 4.5.** `DamageNumberText`
  is `Math.round`. All three rungs still separate on screen, and so does the rejected
  off-pass-through reading (1 / 2 / 2) -- but only at II and III. **Gate on III (5 versus 2)**; I is
  one apart. And pin the ABSOLUTE value: with Bulwark III active the rejected reading still rounds to
  2 at III, so "the number did not move" is not the discriminator.

- **Thorns gives Slice 1's negative-zero trap its first VISIBLE instrument.** `vanillaBlocked`'s
  strict `< 0` was previously checkable only by reading the heart bar for half-versus-full. With
  Thorns III equipped, a hit from behind with the shield raised must paint NO number over the mob --
  if that comparison were ever relaxed to `<=`, a white `5` appears over a mob that hit you in the
  back. Same free instrument for the broken-shield gate.

- **Three side effects of the reflect, traced rather than discovered.** The mob **flashes red with no
  vanilla hurt sound** (`playHurtAnimation` fires because the MOB's i-frames are clear -- it was not
  what vanilla damaged); `mob.setTarget` fires an `EntityTargetLivingEntityEvent` per reflect (inert
  today, no listener, but the guard is `source instanceof LivingEntity` rather than "had no target",
  so it CAN override a different one); and **a lethal reflect from a player who disconnects inside the
  one-tick deferral window kills the mob with nothing credited** -- `Attribution` returns null,
  `dealerIsPlayer` goes false, and `MobDeathSystem` gates credit on it, so no popup, drops, XP or
  statistics, though the damage still lands. Asymmetric and silent; a known, not a bug fixed here.

- **The reflect line on the tooltip is CONDITIONAL where the block line is not.** Every shield has a
  `block_dr`, so "Block: 0%" is information; no shield has a base reflect, so a permanent
  "Reflect: 0%" would advertise a stat the gear does not have. Two traps in putting the lines
  adjacent, both tested: a block fraction is `0.5` rendering "50%" while a reflect percent is `30`
  rendering "30%" (a fraction here would advertise "0.3%"), and `blockPercent` clamps while
  `reflectLabel` deliberately does not -- a reflect has no natural ceiling, and clamping would
  silently under-report a shield an author made vicious.

- **`percentAt` moved to `EnchantCurve`, discharging a 2a deferral on schedule.** The trigger 2a
  named ("a third caller") was over-met: four external callers, two of them block/reflect. The reason
  is the misleading NAME, not deduplication -- a block enchant asking `DamageEnchants` for its
  percentage implies a coupling that does not exist. `DamageEnchants.percentAt` delegates, so
  `DamageEnchantsTest` passes with zero edits, and there is deliberately no `EnchantCurveTest`: a
  mutation already reddens through the delegation.

### Shields, Slice 2a (the gear-gating axis and Bulwark) — what it created or exposed

**Closes four Slice-1 deferrals at once**: the class axis for gear, the enchant ROLL for shields,
`/rpg enchant show` for shields, and "does a broken shield stop blocking". See
`PLAN-shields-slice-2a.md`. Thorns is 2b, its own PR.

**Boot gate — RUN AND PASSED IN FULL, 2026-08-29. All 14 rows.** Paper 26.1.2.build.74. Row 1 from
the boot log, rows 2-14 by the operator at the keyboard.

Row 1, with the deploy verified by mtime AND size before booting and `bulwark.yml` confirmed inside
the shaded jar: `Loaded 6 abilities, 7 visuals, 5 statuses, 7 elements, 5 enchants, 2 kits,
5 weapons, 1 shields, 1 mobs` / `Done (5.164s)`, ZERO `Skipping malformed enchant`. So
`class: shield` and `effect: block_dr` parse through the real loader on a live server, not only in a
`@TempDir`.

Rows 2-14: the roll, the table accepting a shield, the weapon-side gate holding, `show`, the inert
wording, the Bulwark ladder, and the break gate — **full damage on the post-break block**, with
`ShieldBrokenNotice` firing exactly once at the `334 -> 335` crossing and not repeating. The one
result no plan predicted in full is the first entry below.

> **Re-run 2026-08-29 16:05 after the finalisation pass** -- `Loaded ... 6 enchants ... 1 shields`,
> `Done (5.096s)`, zero skipped, zero id-collision warnings. The 15:19 run predates the rename and
> the 0.35 base and is superseded.
>
> **The lock cost a run this time.** `dev-server.sh`'s own `rm -f` hit `Device or resource busy`,
> `set -e` aborted before deploying, and nothing booted. Two orphaned `java.exe` held the jar -- and
> `tasklist /FI ... | grep -c java.exe` reported **0** while both were running. Use
> `Get-CimInstance Win32_Process`, and prove the lock is gone by opening the jar exclusively rather
> than by trusting a process list.
>
> The file lock bit on the way out, exactly as CLAUDE.md records: `rm` on the deployed jar failed with
> `Device or resource busy` because the server JVM outlived the script that started it (an Oracle
> `javapath` shim plus the JDK process it spawns — two `java.exe`, both to be stopped). Confirm the
> previous server is dead before the next deploy, or `set -e` aborts it and a stale build boots
> looking fine.

- **`GearClass` is a second enum, and `EnchantDefinition`'s javadoc argued against it.** That javadoc
  said a parallel enum "would need SUMMONER adding in two places, and the exhaustive-switch
  discipline only works with one enum". First half true and accepted; second half false, and the
  compiler settled it — deleting an arm from `GearClass.of` gives *"the switch expression does not
  cover all possible input values"*, BUILD FAILURE. Two places, one of which the compiler names. The
  javadoc is rewritten rather than left contradicting the code.

- **`ClassDamageModifiers` did NOT migrate, and that is the axis split working.** A ring's
  `+N <Class> Damage` gates on the WEAPON you fight with; a shield in the other hand must not change
  it. `WeaponClassLabel` stays for the same reason. `GearClassLabel` is a sibling, not a replacement.

- **THE ENCHANT LOADER TEST COULD NOT SEE A NEW SHIPPED FILE, and would not have reddened.** Its
  fixture *enumerated* the roster — `SHIPPED = List.of("unbreaking","sharpness","power","attunement")`
  — and copied those four into a `@TempDir`, while asserting *"the shipped enchant roster is exactly
  these four files"*, a claim it could not make. `bulwark.yml` would have shipped with no schema
  check, no class-token check and no curve check, every count still green. This is the
  `getResource("content/")` defect one directory over: **a scan that finds only what it was told to
  look for is indistinguishable from a scan that works.** Fixed to list the classpath and refuse an
  empty result, and proved by positive control rather than argument — with a malformed probe present,
  the OLD fixture reported `Tests run: 17, Failures: 0, BUILD SUCCESS` and never mentioned the file;
  the new one failed 7 and named it.

- **`clean` is NOT inert for TEST sources, which is a different axis from the one refuted above.**
  `./mvnw -pl paper -am test-compile` returned **exit 0 with 48 compile errors present**: the test
  sources had not changed, so Maven skipped them and stale `test-classes` satisfied it. `clean
  test-compile` reported all 48. Commit D2's correction — that `clean` catches nothing a plain build
  does not — was measured for MAIN sources against a changed dependency module and still holds. TEST
  sources against changed MAIN sources in the SAME module is a real incremental hole. **Run `clean`
  before believing a green test-compile after a signature change.**

- **A comment that overclaimed, caught by running a mutation rather than reasoning about it.**
  `BulwarkTest` first said a 0.5-only test "could not tell a wrong implementation from the right
  one". False: additive differs from BOTH rejected readings at 0.5, and both mutations duly reddened
  the shipped-shield test. What 0.5 cannot do is say *which* wrong rule was followed —
  multiplicative and diminishing are bit-identical there (`0.525/0.55/0.575`). The test asserts at
  `0.8` as well, where all three separate: 0.5 pins the composition at a point, 0.8 pins the rule.

- **A plan figure that was wrong, corrected by writing the test.** The plan said a shield's pool
  would be three. It is **two** in 2a — Bulwark plus Unbreaking, the same as every weapon class.
  Three arrives with Thorns, and that is when a 3-candidate slot and `EnchantMenuLayout.CANDIDATES
  == 3` are first exercised by a real roll rather than only by `candidateCount` in isolation.
  `EnchantRoll`'s rarity-weighting javadoc was about to be rewritten around the wrong number.

- **`Outcome.NONE` now has THREE causes and one meaning** — untagged, dangling, broken. The broken
  gate went to `ShieldBlock.resolve` rather than to `ShieldDurability`, where Slice 1 said it would
  go, because that is where the other two already live: base DR, Bulwark and 2b's reflect fall off
  one predicate. `Outcome.blockDr` is renamed `effectiveDr` — a component still called "the shield's
  DR" while carrying an enchant's contribution is how a witness log starts lying.

- **The broken gate makes an existing guard unreachable, and that is named rather than tested.**
  With the gate in `resolve`, `block.blocked()` is false for a broken shield, so
  `applyWearOnBlock`'s own already-broken early return is never entered through the rider. A test
  claiming to guard the broken case VIA the rider would pass without exercising anything. It stays
  as defence in depth, documented as untested.

- **2a SHIPPED A CONSEQUENCE WITHOUT THE INSTRUMENT TO REACH IT, and that is a tooling gap the
  slice created.** A broken shield stops blocking, and `/rpg durability` was weapon-only -- so the
  only way to produce a broken shield in-game was ~250 blocks (`WEAR_PER_BLOCK` 1 against 336 uses),
  or roughly a thousand with Unbreaking III. **A consequence that cannot be produced cannot be
  gate-witnessed**, and an unwitnessable mechanic is the thing the dev commands exist to prevent.
  Fixed inside 2a by widening `/rpg durability` (and `/rpg repair`, which shares its body) to our
  shields. Same argument that put the shield arm on `/rpg enchant` in Slice 1: the mechanic and the
  instrument that exercises it ship together, or the mechanic ships unproven.

  It is a TAG check rather than `resolveHeldGear`, deliberately: that resolver also requires the
  content DEFINITION because all its callers end in a re-mint, and durability needs none --
  `wear`/`repair`/`set` are pure `ItemStack` questions. Routing through it would newly refuse a
  dangling-id WEAPON that works today, which is a behaviour change on the weapon path. No dispatch
  was needed anyway: the kernel is already shared.

- **A BROKEN SHIELD IS NOT FULLY INERT — measured on the live boot, and it is a MECHANICAL residue,
  not only a cosmetic one.** The custom half works: no DR, full damage through, `resolve` returning
  `Outcome.NONE`. But vanilla **still plays the raise AND still dampens mob knockback**, because
  `Durability.wear` floors at one remaining use so vanilla never sees the item as destroyed, and we
  decline to add our mitigation rather than cancelling vanilla's block.

  The plan predicted the shape of this and named the cause correctly. What the boot added is the part
  that matters: **knockback dampening is a real benefit a player keeps**, so "broken" means *no custom
  mitigation*, not *dead*. A player with a spent shield is still measurably better off than one with
  no shield at all.

  **Making broken mean fully-dead requires intercepting vanilla's own block**, not merely declining to
  add ours — a different kind of change from anything in 2a, and deliberately out of scope. Recorded
  as a decision rather than left to be rediscovered as a bug. Note it interacts with the vanilla-shield
  known below: an untagged shield and a broken one now behave identically in play, which is coherent.

- **A "broken" shield is still functional to VANILLA — predicted here, now confirmed above.**
  `Durability.wear` floors at one remaining use, so vanilla keeps playing the raise, the block sound
  and the knockback dampen, and keeps reporting `BLOCKING < 0`, while `resolve` returns NONE and the
  player takes the hit in full. `ShieldBrokenNotice` is the only thing distinguishing that from a
  broken mechanic. Whether ONE crossing notice is enough against vanilla continuing to animate a
  block that does nothing is a **feel** question this slice does not settle. Its throttle key is
  distinct from `BrokenNotice`'s on purpose: a broken sword must not silence it.

- **Binding is by EFFECT, not by id — the opposite of the Unbreaking seam, deliberately.**
  `ShieldDurability` reads Unbreaking by hardcoded id and never consults the registry, which is right
  there because its curve is Java. Bulwark's curve is content, so the definition must be resolved
  anyway and filtering on `effect()` is free — which makes the second block enchant a yml file rather
  than a recompile. **There is no `Bulwark.ID` on purpose.** The cost is the asymmetry
  `DamageEnchantItems` already records: deleting `bulwark.yml` silently switches it off while the
  tooltip still renders it.

- **ADDITIVE BULWARK MAKES TOTAL IMMUNITY REACHABLE, and that is a constraint on future content.**
  A shield authored at `block_dr >= 0.85` is untouchable at Bulwark III (`0.9 + 0.15 = 1.05`, clamped
  to `1.0`, a 15.0 hit passes `0.0`). Nothing shipped is close — the shield tops out at 0.65 —
  and the clamp makes the failure mode "invulnerable" rather than "negative damage". **The day a
  high-DR shield is authored is the day to decide on a soft cap below 1.0**, not before.

- **Two gear records, not one shared abstraction, and ARMOR is the trigger.**
  `RpgCommand.HeldGear` and `EnchantMenu.PlacedGear` are shape-aligned — same three members, same
  meanings — and deliberately not factored together. Designing a common `Gear` type from two examples
  is designing it from one and a half; armor is the third shape and the point at which it can be
  checked against something. Same reasoning `ShieldDefinition`/`ShieldItems` already carry.

- **A vanilla shield still gives ZERO custom protection, and the fix is NOT in the block path.**
  Re-confirmed rather than revisited. The deceptive part — vanilla animates a block that absorbs
  nothing — is a symptom of the MISSING CRAFTING HOOK, not of `resolve`: the real fix is the gear-arc
  step where crafting a shield produces an RPG shield, so a plain vanilla shield stops being
  something a player holds. Inventing a default `block_dr` for an item no content file describes
  would blur the tag boundary the whole system keys on.

- **Gear already in an inventory is never rolled retroactively.** `rollOnAcquire` fires only at
  acquisition, never from `mint`/`remint` (the once-per-item rule). A shield minted before 2a
  carries no `enchant_rolled` flag and nothing will come back to give it one. Same property weapons
  have had since the rolls pass. **It matters at a boot gate**: an old shield shows empty slots and
  reads exactly like a broken roll. Give a fresh one.

- **Still outstanding from Slice 1: no `ShieldRefresher`.** A shield's lore does not rebuild from
  content on rejoin the way a weapon's does — and now that the lore carries an enchant-dependent
  block percent, that gap is slightly more visible than it was.

### Shields, Slice 1 (a mintable shield and the block-DR mechanic) — what it created or exposed

A `shield` is mintable, and blocking has a real effect for the first time: vanilla decides
WHETHER a hit was blocked, `Shield.applyBlock` decides what that is worth, and armor `Defense`
reduces the remainder inside `applyDamage`. Block-then-armor, both apply. Before this, a raised
shield was mechanically identical to empty hands — `onMobMeleeAttack` deals the mob's ATTACK stat
and tokens `event.getDamage()` to `0.01`, so whatever vanilla's shield did to the event was thrown
away with the token.

- **`isBlocking()` is direction-blind and is NOT the signal.** It is true for anyone holding
  right-click, so a shield read that way blocks a hit landing in your back. The signal is the
  event's `DamageModifier.BLOCKING` — vanilla's own raised-AND-frontal-AND-in-arc verdict, already
  computed — read with a strict `< 0` because it is a REDUCTION, so a full block is `-raw` and
  still negative. The enum is deprecated since 1.12 but not for removal, and on the pinned Paper it
  is the only block signal on the event: `DamageSource` carries none, and the `blocks_attacks` data
  component describes the ITEM, not the hit. The deprecation is confined to `ShieldBlock`.

- **The block must be read BEFORE the token.** `EntityDamageEvent.setDamage(double)` re-derives
  every modifier by scaling it against the new base, so reading `BLOCKING` after
  `setDamage(TOKEN_DAMAGE)` reports the token's share of the block rather than the block. This is
  the one ordering constraint in the rider.

- **Our Unbreaking forced shield wear to be ours too.** `Unbreaking` is a custom enchant whose curve
  is written out in core, because the no-vanilla-enchants policy means a held item never carries a
  vanilla enchant to delegate to. Vanilla charging the shield on a block would never consult it, so
  Unbreaking would sit on the tooltip doing nothing — AND the shield would be charged twice for one
  block. So `ShieldDurability.applyWearOnBlock` mirrors the weapon path, and `onShieldItemDamage`
  cancels `PlayerItemDamageEvent` for anything carrying `shield_id`. The witness for that is a
  POSITIVE count — N blocks move the bar by exactly N — never a comparison against an un-suppressed
  run, because vanilla's shield wear can scale with the damage blocked rather than being flat.

- **A vanilla, untagged shield gives ZERO custom protection.** `ShieldBlock` resolves it to
  `Outcome.NONE`, so the mob's full stat reaches custom HP; and vanilla's own reduction is tokened
  away. A player holding a plain shield is, mechanically, not blocking at all. **A known, not a
  bug** — the alternative is inventing a default block fraction for an item no content file
  describes. It becomes a real decision when shields drop as loot or players craft them.

- **A broken shield still blocks.** `Durability.wear` floors at one remaining use, so a spent shield
  can never be destroyed — it simply stops wearing and keeps blocking at full strength. Weapons have
  a break gate; shields deliberately do not get one in this slice. `ShieldDurability` is where it
  would be enforced. Deferred because "your shield is broken" needs a shield-shaped notice
  (`BrokenNotice` says "your weapon is broken — repair it before using it", two lies at once) and
  because a shield that stops blocking mid-fight is a feel decision, not a mechanical one.

- **`/rpg enchant show` is weapon-only, and that is a TYPE hole rather than laziness.**
  `showEnchants` reaches `EnchantEffectLine.of(enchantDef, level, definition.weaponClass())`, whose
  `heldClass` is contractually never-null: the `DAMAGE` arm dereferences it through
  `WeaponClassLabel.of`, an exhaustive switch with no default arm. A shield has no `WeaponClass`.
  Widening that switch to tolerate null would put a shield-shaped hole in a class whose whole design
  is one exhaustive switch, so instead SHOW refuses and the ACTIVE arm's effect suffix is empty for
  a shield. The five WRITE arms are contained in one `HeldGear` dispatch. **This is the same axis
  that blocks the enchant ROLL** — `EnchantRoll.roll` is keyed on `WeaponClass` too — so a shield
  ships enchant-COMPATIBLE (it carries the container, `/rpg enchant` can write it, the wear path
  reads it) but not enchant-ROLLED. Slice 2 owns the class axis for gear, and it closes all three at
  once.

- **`ShieldDefinition`/`ShieldItems`/`ShieldLore` duplicate their weapon counterparts on purpose.**
  Reusing `WeaponDefinition` was not an option: its constructor REJECTS an empty trigger list and
  REQUIRES a `WeaponClass`. Factoring a shared `GearDefinition`/`GearItems` now would mean designing
  the abstraction from a single example; when armor lands there are three shapes to check it
  against. What is NOT duplicated is anything with logic in it — `WeaponItems.displayName`,
  `RarityColors`, `EnchantItems`, `EnchantLore` and `WeaponDurability`'s pure-item helpers are all
  called directly.

- **The order of block and armor is NOT observable in the arithmetic**, and a test that claimed to
  guard it did not. At raw 8 / DR 0.5 / defense 20 the two orderings are bit-identical; across 22400
  combinations they differ in 4780, by at most `2.842170943040401e-14`. Swapping the two steps is
  REASSOCIATION, not commutation. The mutation was RUN and left all 11 tests green, so the test was
  renamed from `...AndTheOrderIsBlockThenArmor` to
  `blockAndArmorBothApplyRatherThanOneShadowingTheOther`. The order is fixed by the pipeline instead
  — block in the rider, defense a thread hop later in `CombatantStats.damage` — and there is no call
  site at which they could be swapped.

- **The percent formatter had the same trap and it was caught by probing first.**
  `0.29 * 100 == 28.999999999999996` and `0.55 * 100 == 55.00000000000001`. A naive trim prints
  those verbatim on the item. The shipped 0.5 multiplies to a clean 50.0, so this would have passed
  every boot gate in this slice and waited for the first author who typed an odd fraction.

#### Boot record — 2026-08-29, shields slice 1 (23 mob->player hits: 20 blocked, 3 not)

`./scripts/dev-server.sh --no-build --refresh-content`, Paper 26.1.2.build.74-stable.
Deploy verified by mtime and size before booting, not assumed (target 09:44:15, deployed
09:44:52, both 452911 bytes) — CLAUDE.md records a run where `rm -f` failed on a locked jar,
`set -e` aborted the deploy, and a stale build booted looking fine.

**THE LOAD-BEARING UNKNOWN IS SETTLED: a full block DOES fire the event.**
`raw=29.2500 final=0.0000 blocking=-29.2500 cancelled=false`, and the rider ran. The
`blocks_attacks` contingency is dead; the stock `Material.SHIELD` stays.

| # | Check | Result |
|---|---|---|
| 1 | `/rpg give shield` mints | PASS (mechanically — see caveat below) |
| 2 | Unbreaking onto a held shield | PASS — III active, so `HeldGear` dispatch + `ShieldItems.remint` both work |
| 3 | Unblocked baseline | PASS — `reduced=15.0000`, the mob's ATTACK stat |
| 4 | Blocked, frontal | PASS — `reduced=7.5000`, exactly half, on all 20 |
| 5 | Armored **and** blocking | PASS (operator-observed; see caveat) |
| 6 | Hit from behind | PASS — no reduction at 107.4° and 160.8° |
| 7 | Shield down | PASS — `reduced=15.0000` |
| 8 | Durability, N blocks | PASS — see below |
| 9 | Vanilla feedback (sound, dampen) | **NOT VISUALLY CONFIRMED** |
| 10 | `[BLOCK]` witness | PASS — fires, signal present/absent correctly |

**Row 8, and it is the strongest result here.** Bar 336 -> 331 = **5 consumed over 20 blocks**,
with Unbreaking III active. `consumeChance(3) = 0.25`, so expected 5.00, sigma 1.94, z = +0.000,
inside a 3-sigma band. The value is not what makes this conclusive — sigma 1.94 means 1..9 would
also have passed, and one N=20 run cannot separate p=0.25 from p=0.2. What IS conclusive is the
distance to every rival: our wear never ran -> 0; Unbreaking not consulted -> 20; our wear plus
vanilla double-wear -> ~25. Observed 5.

- **`onShieldItemDamage` is HARMLESS, not load-bearing — corrected.** Zero `[BLOCK] WEAR` lines
  across 20 blocks: vanilla never fired `PlayerItemDamageEvent` for a blocked hit on this build,
  so there was no double-wear to prevent. The commit that added it claimed it prevents doubling;
  that claim is unproven here. It stays, because we own the item's durability outright and a
  future vanilla path charging it would be an unaccounted second source — but it is a guard
  against a thing not currently happening, and should be described that way.

**`DamageModifier.BLOCKING` is not the signal its javadoc implies.** `blockingApplicable=true` on
EVERY player damage event, including one taken bare-handed with nothing in the inventory. "Only
present for Players" means exactly that. The SIGN is the verdict, not the presence.

**And the sign has a negative-zero trap.** An unblocked hit taken with the shield RAISED reports
`blocking=-0.0000`. Executed: `-0.0 < 0` is false (correct), `-0.0 <= 0` is TRUE, and
`Double.compare(-0.0, 0.0)` is -1. So `<=`, or the idiomatic-looking `Double.compare(...) < 0`,
would have inverted row 6 — half damage from behind, with vanilla playing no block cue to
contradict it. The strict `<` was right for a reason nobody had stated.

**The frontal arc, bracketed rather than pinned.** Blocked out to `facingDot=+0.0131` (89.2° off
facing); passed at `-0.2987` (107.4°) and `-0.9444` (160.8°). Consistent with vanilla's 90°
`horizontalBlockingAngle`. Not pinned: the probe is a 3D dot and vanilla's check is horizontal.
`isBlocking=true` on both passed hits — the direction-blindness, measured.

**What this boot did NOT establish, and must not be read as having:**

- **Row 5 passed on the operator's reading, and the log is STRUCTURALLY SILENT on it.** All 23
  rider lines read `reduced=7.5000` or `reduced=15.0000` -- exactly what they would read with or
  without armor, because defense is applied a thread hop later inside `CombatantStats.damage`,
  past where the witness can see. Nothing in this log confirms or refutes the row; it rests on the
  heart bar being read in game. The predicted ladder, computed against the real classes: per hit
  `6.25` armored+blocking, against `7.5` blocked-only, `12.5` armored-only, `15.0` raw -- and over
  four hits from full, HP `75` / `70` / `50` / `40` respectively, which is the read that separates
  all four. THE EXACT HP FIGURE WAS NOT CAPTURED into this record, so the row is logged as
  operator-observed rather than measured. If it is ever doubted, four hits from `/rpg heal 1000`
  settles it in one pass.
- **Row 9 was not visually confirmed.** Nothing was cancelled, so the raise, sound and
  knockback-dampen should be vanilla's — but "should be" is not "was seen".
- **Rows 1 and 2 are mechanically confirmed, visually unverified.** Unbreaking III cannot be
  active on a shield unless give, the enchant dispatch and the re-mint all worked. The
  TOOLTIP — rarity colour, `Common Shield` footer last, enchant block above it — was not read back.
- **`disagree=true` never appeared, and that is not evidence about the active-hand fix.** All 20
  blocks held exactly one shield, where the active-hand and positional readings agree by
  construction. The mixed loadout that separates them (a VANILLA shield in the raised hand, ours
  in the other) cannot be hit by accident and was not attempted. That path stays reasoned, not
  witnessed.

**Deferred to Slice 2 -- ALL CLOSED IN 2a EXCEPT THE FIRST, see the Slice 2a section above.** Thorns
(shipped in 2b under that name -- it briefly went out as "Riposte" to avoid the design-doc
reservation, and the finalisation pass took the name back when that reservation VACATED), Bulwark,
the class axis for gear (enchant gating, the roll, and
`show`), and a `ShieldRefresher` for the join / `/rpg refresh` path — a shield's lore does NOT
currently rebuild from content on rejoin the way a weapon's does.

### Crit (a chance/damage stat pair on all custom damage) — what it created or exposed

Closes "vanilla crits are live and unpaid-for" below. Two stats — `critChance` (a probability, base
0.15) and `critDamage` (a BONUS, base 1.0, so a base crit is exactly `2.0x`) — join the six already
converged by the reconcile loop, which now carries eight. The roll is drawn once per cast in
`BukkitCombatant.snapshot`, decided by a pure `Crit.multiplier` in core, and frozen on
`CombatantSnapshot`/`Caster` beside `chargeScale`. `EffectApplier` multiplies by it in both damage
arms, so it reaches every custom damage effect — swing, ability literal, projectile, area — with no
per-arm branch.

- **The bonus convention, not the multiplier convention.** `critDamage` is a summand with base 1.0
  and gear adds to it, so the multiplier is `1 + critDamage`. Storing the multiplier directly (base
  2.0) would have made gear either multiplicative — a different composition rule from every other
  stat in the store — or additive on a base already containing the 1. Keeping it a bonus is what lets
  it "stack additively exactly like the other stats" without a second rule.

- **A mob never crits because its STAT is 0, not because a check says so.** `HealthState` bases both
  stats on the combatant's frozen faction. There is no gate at the roll site: `Crit.crits` compares
  `roll < 0`, false for every roll a half-open source can produce. Gating at the call site instead
  would have left the stat reading 0.15 on a zombie while something elsewhere quietly contradicted
  it — the divergence the store exists to prevent, and the reason a future stat screen can read this
  stat and be right.

- **`Crit.chance` clamps at the point of use, and the STAT does not.** A crit-chance stat of 2.15
  reports 2.15 and resolves to a capped 1.0 where it is read. The cap is therefore stated once. At or
  above 100% every hit crits rather than overflowing into a second tier — a decision, not hygiene.
  **The clamp's tests are the ones worth keeping honest:** removing `Math.min`/`Math.max` changes no
  crit OUTCOME (a chance of 2.0 still always crits; a chance of -1.0 still never does), so only the
  assertions calling `chance()` directly catch it. A suite that checked crits alone would have passed
  a broken clamp.

- **Sweep inherits crit for free, and does not re-roll.** One roll per cast means a swept mob takes a
  fraction of the primary's already-multiplied number. Rolling at the damage arm instead would have
  given a Burst catching five bodies five independent crits. **Its POPUP stays white**, though:
  the damage inherits the crit in full, the presentation does not, because the roll was for the hit
  the player aimed at and colouring every bystander would claim each crit independently. Visible
  consequence: a yellow `28` on the primary beside white `14`s. Recorded as a decision, not a gap.

- **`wasCrit` widened the `applyDamage` port, whose javadoc said "a number and a culprit, nothing
  else".** It earns the widening by being underivable downstream rather than by being convenient: the
  multiplier is rolled on the DEALER's thread and frozen, then the damage hops onto the TARGET's
  thread and lands a tick later, where the amount alone cannot say whether it was doubled. It changes
  no arithmetic. `DamageNumberText` had already named this absence — *"the seam carries no crit bit,
  so there is no crit/normal branch yet"* — so the design was pre-decided by the codebase rather than
  by this pass.

- **Every new overload is ADDITIVE.** `applyDamage(amount, sourceId)` is now a `default` delegating
  with `false`; `CombatantStats.damage` and `HealthChange` gained back-compat forms; `EffectApplier`
  and `CastExecutor` kept their existing constructors. No existing caller, implementor or test
  changed, and the non-crit multiplier is exactly `1.0` so the multiply is an exact identity on the
  ~85% of swings that do not crit.

- **VANILLA's crit particles are now suppressed by packet, and that is the sanctioned exception.**
  There is no Bukkit event for the crit visual — it is not a cancellable damage side effect, not an
  interceptable particle spawn, and not exposed on any attack event; vanilla sends it straight from
  `Player#attack`. This is the case the prefer-the-API rule reserves packets for. Before crit existed
  the vanilla burst was a small lie about an unremarkable hit; with our own burst it became the same
  symbol meaning two things, and a player counting bursts would have counted jump attacks.

- **The two particle paths were confirmed DISTINCT before the cancel was written.** Vanilla's is
  `ENTITY_ANIMATION` / `CRITICAL_HIT`; ours is `spawnParticle(Particle.CRIT, ...)`, i.e. `PARTICLE`.
  Checked against the pinned PacketEvents 2.13.0 API — not 2.12.1, which was what happened to be
  cached. Had they shared a type, the cancel would have eaten our own burst and the boot would have
  reported "crit particles stopped working" with the cause three layers away.

- **Feedback is colour-only now.** An earlier revision appended a `!` so the crit survived greyscale;
  it was dropped on the call that the number should stay a number. The redundancy that argument
  wanted did not vanish, it MOVED: the particle burst is the second channel, and suppressing
  vanilla's is what makes that channel mean one thing.

  > #### 2026-08-28 — the crit boot, and the instrumentation defect it never got to expose
  >
  > **Caught before it shipped, not by the boot:** the first `[CRIT]` witness drew a SECOND
  > `ThreadLocalRandom` roll to print, separate from the one that decided the crit. The logged rate
  > would have been statistically ~15% and causally unrelated to the crits actually dealt — `crit=false`
  > printed on the very tick a yellow number appeared. It was rewritten to draw once into a local used
  > by both. **This is the third instrumentation defect in three passes** (the sweep pass logged
  > `getType()` where identity was needed; the knockback pass nearly logged below its own early
  > return), and they share one shape: *the log was written to confirm the answer rather than to be
  > capable of contradicting it.* The locals it introduced survive the strip.
  >
  > - **`[CRITPKT] type=CRITICAL_HIT suppressed=true`**, and a jump attack produced no particles.
  > - **No `MAGIC_CRITICAL_HIT` in the whole session** — the expected absence, now measured. An
  >   absence over one session is not a proof, which is why it was observed rather than cancelled on
  >   the strength of the argument.
  > - Crits still fire with particles; the yellow number reads without the marker; `/rpg mana refill`
  >   works.
  >
  > The witness log printed EVERY animation type, not only the one being cancelled. A log restricted
  > to `CRITICAL_HIT` could not have told us we had picked the wrong packet — it would have printed
  > nothing and read as "no crits happened", which is the failure mode this file keeps recording.

- **Not in scope, recorded:** crit on mob→player, a crit-immunity or crit-resist stat, crit against
  the multi-attacker edge, and cancelling `MAGIC_CRITICAL_HIT` if it ever proves reachable.
  `crit_chance_boost_TEMP` and `crit_damage_boost_TEMP` join the other `_TEMP` fixtures owing removal
  when real content grants crit.

### Sweep (vanilla's sweep events drive it, at a fraction of the primary) — what it created or exposed

Closes "sweep is cancelled, not owned" below. `onPlayerSweepAttack` no longer cancels
`ENTITY_SWEEP_ATTACK` outright: it rides it, and each swept mob takes `sweep × what the primary was
hit for` — `0.5` on both shipped swords, declared per weapon. Vanilla still decides everything hard
(full charge, sword, which mobs are in the hitbox); we only supply the number. Because the number is
a fraction of the primary's FINAL figure, sweep inherits the enchant percentage, the class bonus and
the charge by construction, with no second multiplier chain to keep in step.

- **The post-mitigation reading was IMPOSSIBLE, not merely worse.** The plan asked for "what the
  primary got hit for" as its post-mitigation figure. That figure does not exist when a sweep event
  fires: `applyDamage` defers onto the victim's entity scheduler (`entity.getScheduler().run`, which
  lands NEXT tick) and mitigation happens inside `CombatantStats.damage` at the far end of that hop,
  while vanilla raises every sweep event inside the SAME synchronous `Player#attack` as the primary.
  So the seam reports the PRE-mitigation swing output and each swept mob mitigates once — which is
  also the reading that avoids double-counting armor. Moot today either way: mobs are never
  reconciled, so every mob resolves to 0 defense and the two numbers are identical. It becomes
  observable the day mobs are given defense.

- **Sweep does not inherit the crit, because there is no crit to inherit.** Vanilla's ×1.5 lands on
  the tokened number and contributes nothing to the custom one. Sweep will pick it up for free, by
  the same construction that gives it the enchant and the class bonus, on the day a crit multiplier
  reaches the custom amount. Until then "a crit sweeps harder" is false and must not be written down
  as a feature.

- **The number is OBSERVED, not recomputed.** `EffectApplier` gained an `onDirectDamage` seam —
  purely additive, no-op by default, modelled on the `onBasicAttackUse` Runnable `CastExecutor`
  already takes — fired INSIDE the `amount > 0 && alive()` gate in both damage arms. The rejected
  alternative was to re-derive the figure in `WeaponFire` from the caster, which would have needed a
  second copy of the enchant multiplier, the class bonus, the charge AND that liveness gate, and
  would have drifted the day any one of them moved. Reporting inside the gate is what makes "a swing
  that dealt nothing sweeps nothing" true rather than hoped for; the test that reddens when the
  `accept` is moved one line out is the one that says so.

- **The stash does not consume, for the same reason the knockback signal does not.** One sweeping
  swing raises one damage event per swept mob, and all of them need the same number. A
  consume-on-read would have served the first bystander and silently skipped the rest — a bug
  invisible in any test with two mobs standing together. Tick-stamped instead, so it expires on its
  own and a stamp from an earlier tick is refused as belonging to a different swing.

- **`sweep:` is optional, and that is a migration decision, not an oversight.** Adding a REQUIRED
  field to the weapon schema is what broke Stage 2's first deploy (see below): an operator's edited
  content file is rejected on restart. Absent `sweep` simply means no sweep, so an old file loads and
  quietly does not sweep. A declared sweep on a weapon with no vanilla-driven melee trigger DOES
  throw — it can never fire, so it is named per-file rather than silently ignored — reusing the same
  `hasVanillaMeleeTrigger` predicate `mint`, `meleeCadence` and the `attack_speed` guard ask.

- **The gates run BEFORE the token, unlike the primary rider.** That handler tokens unconditionally,
  so a refused click still flashes the mob — accepted as cosmetic in Stage 1, with the fix recorded
  as "a decision later rather than a discovery". This takes the decision: a bystander that will not
  be swept is neither flashed nor given i-frames. That ordering is precisely what the old
  cancel-outright existed to protect, now bought rather than argued away.

- **A PvP leak was introduced and caught before commit.** The first draft of the rider `return`ed on
  a player victim, where the old handler had cancelled EVERY player-damager sweep. That would have
  let a vanilla sweep land on a player — PvP arriving by accident, through the one path nobody would
  think to look at. Non-mob victims now keep the cancel.

  > #### 2026-08-28 — the sweep boot, and the two orderings it settled
  >
  > Both orderings the design rests on were WITNESSED, not inferred. 23 primary hits, 9 swept mobs
  > across 7 sweeping ticks, 180 knockback events.
  >
  > - **The primary's damage event precedes the sweep events.** `[SWEEP] PRIMARY` leads every
  >   sweeping tick, so the stash is always written before a sweep event reads it. Had this been
  >   reversed, sweep would have silently never fired.
  > - **A swept mob's own window claim precedes its own knockback event.** All 9 `SWEEP_ATTACK`
  >   knockback events read `landedThisTick=true`, and the pairing is exact per tick — 2/2 at tick
  >   2086, 2/2 at 2238, 1/1 at each of 2141, 2223, 2254, 3049, 3077. Nine swept mobs damaged, nine
  >   sweep-cause knockbacks, no mismatch.
  > - **A DISTINCT `SWEEP_ATTACK` cause exists, and this is a CORRECTION.** The knockback boot saw
  >   only `ENTITY_ATTACK`, and this pass expected the same. The log shows 9 events with cause
  >   `SWEEP_ATTACK`, class `EntityKnockbackByEntityEvent` — the same subclass, a different cause.
  >   They return at `onCombatKnockback`'s cause check and reach vanilla UNGATED, which is what gives
  >   a swept mob its shove. So no second cause was needed, but NOT for the reason first assumed:
  >   `landedThisTick` is never consulted for a sweep push. The `true` on those 9 lines is evidence of
  >   ORDERING, not of the gate doing work — do not describe it as the gate, which is the same error
  >   this file already had to correct once for spam knockback.
  >
  > **The instrumentation could not have answered the question it was built for.** The knockback
  > witness logged `getType()`, not identity, and every mob in the session was a `ZOMBIE` — so "a
  > knockback naming a DIFFERENT victim than the primary" is undecidable in this log. What rescued it
  > was the CAUSE: only a swept mob raises `SWEEP_ATTACK`, so those 9 lines are entity-unambiguous
  > without a UUID. Eyeballing a same-tick block and calling the second `ZOMBIE` the swept one would
  > have been a story, not a reading. **The next witness log prints the UUID.**
  >
  > **Feel, at the keyboard:** half-of-the-primary reads as a reward for a big swing rather than an
  > instant clear; a non-full-charge swing raises no sweep events at all; a weapon with no `sweep`
  > sweeps nothing; a broken weapon is inert.
  >
  > **`--refresh-content` was required, and the trap was confirmed live BEFORE the boot rather than
  > diagnosed after.** `grep -c sweep run/plugins/Rpg/content/weapons/ironblade.yml` returned `0`: the
  > deployed file predates the field, `saveResource(path, false)` never overwrites, and a boot without
  > the refresh would have shown sweep doing nothing with the code taking the blame. Same shape as the
  > Stage 2 deploy below, avoided by reading that record first.

- **Still open:** a Sweeping-Edge-style enchant on the fraction, sweep × the multi-attacker/co-op
  edge, and mob→mob sweep. Also open: `landedThisTick=true` on a sweep knockback is currently
  incidental — if sweep knockback ever needs gating, the cause check has to learn `SWEEP_ATTACK`, and
  the ordering above says the claim will be there to read.

### Knockback (vanilla owns melee knockback, gated to the hit window) — what it created or exposed

Closes the fork Stage 1 opened below. `onCombatKnockback` no longer cancels vanilla's player→mob
`ENTITY_ATTACK` knockback unconditionally; it cancels *unless* `MeleeHits.landedThisTick(victim)`,
which is true only on the tick a hit actually claimed the damage window. So the push keeps the exact
cadence of the damage — once per window — and a windowed-out spam-click gets neither. Vanilla's base
push, upward pop and sprint bonus all arrive unmodified, because none of them is re-derived: the
sprint bonus in particular is free, taken by vanilla from the attacker's state at hit time. No
content change, no `EffectSpec.Knockback` on any weapon, and therefore no need to fix
`landBasicMelee`'s zero-direction origin — the `EffectApplier` knockback arm is not on this path.

- **The signal is DERIVED, not stored.** `landedThisTick` reads
  `CooldownTracker.ticksRemaining(victim, WINDOW_KEY) == WINDOW_TICKS` — a full window remaining can
  only mean the claim happened on this very tick. No second map, so nothing extra to bound and no way
  for the two to disagree about what a hit was. It is deliberately NOT "is the window open": a mob
  hit three ticks ago still has an open window, and reading that would leak a push to precisely the
  spam-click the gate exists to refuse. The unit test that reddens under a `> 0` mutation is the one
  that says so.

- **The gate does not consume.** Paper's `EntityPushedByEntityAttackEvent` javadoc warns one attack
  may raise the event more than once ("multiple acceleration calculations are done"), so a one-shot
  signal could cancel the second event and silently eat the sprint bonus. The tick stamp bounds it
  instead, expiring on its own at the tick boundary.

- **Mob→mob `ENTITY_ATTACK` knockback stays cancelled — pre-existing, not introduced here.**
  `onCombatKnockback` keys on the *knocked* entity and never looks at the attacker, so a zombie
  hitting a villager has had its knockback cancelled since Stage 1. No signal is ever set for it, so
  this pass leaves it exactly as it was. Worth fixing when the handler learns to read the attacker —
  the same prerequisite the per-attacker gate below needs.

- **Untagged vanilla weapons are token-NEUTERED on the melee path, and now also take knockback.**
  Not "untouched". An untagged sword has no `weapon_id`, so it mints no suppressor and its hit
  reaches `onPlayerMeleeAttack`, where `event.setDamage(TOKEN_DAMAGE)` runs **unconditionally** —
  before any weapon lookup. `landVanillaMelee` then finds no weapon and returns, so no custom damage
  comes back. Net: ~0.01 damage, and as of this pass a full vanilla push. It shoves mobs while
  dealing nothing. The comment at `RpgListeners.java:489` reading "an untagged vanilla sword is
  untouched" is scoped to the *broken-weapon* gate directly above it and does not describe the token.

- **OPEN DESIGN QUESTION: should untagged vanilla weapons be neutered at all?** The behaviour above
  is an unexamined side effect of tokening unconditionally, not a decision anyone made. The
  alternatives are real ones — let an untagged item behave as pure vanilla (return before the token
  when no `weapon_id` is present), or neuter it on purpose so only RPG weapons fight. Either is
  defensible; what is not defensible is arriving at one by accident. Correct for our weapons, which
  is why it did not block the knockback pass.

- **Co-op leaks one push, by design — see the multi-attacker i-frame edge below.** Recorded there
  rather than here because it is the same defect wearing a second hat.

  > #### 2026-08-28 — the witness boot, and the one thing it changed
  >
  > Instrumented both handlers, booted, swung. The log settled three things that had been reasoned
  > about rather than seen, and one of them was nearly a shipped bug.
  >
  > - **Ordering holds.** `[KB] CLAIMED` precedes `[KB] KNOCK` for the same tick and victim on all
  >   fifteen hits, so the rider's claim is always recorded before the knockback event asks. The gate
  >   rests on this.
  > - **A single hit can raise TWO knockback events, and it does so when sprinting.** Eleven
  >   non-sprint hits raised one event each; three of the four sprint hits raised two:
  >
  >   ```
  >   tick  sprinting  knockEvents
  >   534   false      1
  >   553   true       1
  >   568   true       2
  >   647   true       2
  >   721   true       2
  >   ```
  >
  >   **This is why the signal does not consume.** A consume-on-read gate would have cancelled the
  >   second event on a sprint hit and eaten the sprint bonus — the single feature this pass exists
  >   to deliver, destroyed by the guard meant to protect it. Paper's javadoc warned of it
  >   ("multiple acceleration calculations"); the boot turned the warning into a number. Note the
  >   553/568 pair: sprinting was true for both and only one raised two, so `isSprinting()` at damage
  >   time is not by itself the predictor. The mechanism is not established here, only the count.
  > - **The concrete event is `EntityKnockbackByEntityEvent`, cause `ENTITY_ATTACK`, on all 18
  >   events.** It reaches a handler registered on `EntityKnockbackEvent` because neither it nor
  >   `EntityPushedByEntityAttackEvent` declares its own `HandlerList`. It also exposes
  >   `getHitBy()`/`getPushedBy()` — see the multi-attacker note below, which this partly answers.
  > - **Feel rows confirmed at the keyboard:** a real hit pushes and a sprint hit sends the mob
  >   further (the 18-event run above is the same swings); **mob→player knockback still knocks the
  >   player back**, unchanged and still vanilla's; **a staff moves nothing** (flat-0 attack damage,
  >   so no damage event and no knockback event — the suppression claim holds in game, not just in
  >   `WeaponItems.mint`); **a broken weapon is inert** — no damage, no flash, no push, because the
  >   rider cancels the event before the claim so no signal is ever set.
  >
  > **The cancel branch was NOT witnessed firing — and the boot explained why.** Read this as
  > "unwitnessed, with a cause", never as a passed row.
  >
  > **SUPERSEDED 2026-08-28 by the sweep boot: the cancel branch IS witnessed, live, 146 times.**
  > Not on a fresh mob — on a victim with enough HP to keep you swinging into a closed window. 93
  > `IRON_GOLEM` and 53 `ZOMBIE` knockback events logged `landedThisTick=false`, and NOT ONE of them
  > shared a tick with a hit that dealt custom damage. So they are exactly what the gate was written
  > for: non-claiming re-hits, whose push is cancelled because no damage was dealt to earn it. What
  > the knockback boot could not reach with one attacker on low-HP mobs, a longer fight reached
  > without being designed to. The paragraph below stays true as written — vanilla suppresses the
  > *first* windowed-out re-hit upstream — but "the cancel line never ran once" is now a statement
  > about that session only.
  >
  > The escalating-charge shape WAS produced: at **tick 12170** a windowed-out re-hit reached the
  > rider — an `ATTACK` line with no `CLAIMED` — so the damage event fired and the window refused it.
  > It raised **no knockback event at all.** Vanilla suppresses the windowed-out re-hit's push on its
  > own, upstream, before `onCombatKnockback` is ever consulted. Across the whole session every one
  > of the 18 knockback events logged `landedThisTick=true`, and the cancel line never ran once.
  >
  > **So the spam-knockback leak this gate was written to stop does not manifest for a single
  > attacker, and the gate is not what prevents it — vanilla is.** The earlier framing in this file
  > and in the code comments claimed otherwise; both have been corrected. Do not describe the gate as
  > "what stops spam knockback". It would be crediting our code with vanilla's work.
  >
  > **The gate stays anyway, and must not be removed on the strength of one boot.** What was
  > disproved is one failure mode under one attacker; three cases this session could not speak to
  > remain live: **co-op**, where a second player's refused click is a separate attack vanilla has no
  > reason to suppress; **external-damage desync**, where something else moves the victim's state out
  > from under our window; and **a future Paper version where re-hits do knock.** The gate is one
  > comparison on an already-existing stamp, correct in all three, and cheap enough that keeping it
  > needs no further justification than that.

### Stage 2 (attack speed is a managed stat) — CLOSED, and what it left

Closes the Stage 1 deferral below: the attack-speed stat now drives the player's vanilla
`ATTACK_SPEED` attribute, reconciled on the same 5-tick loop as HP, defense and the rest, so
`attack_speed_boost_TEMP` finally moves a basic melee swing. The mint-time pin is gone — an item
modifier cannot follow a stat that changes while the item sits in your hand, which is precisely why
the boost was inert. Melee weapons author `attack_speed:` directly; `cooldown_ticks` is dropped from
a melee basic, where nothing read it any more.

- **The single-target sustained cap is REAL, INTENDED, and not a Stage 2 regression.** A boost past
  an *effective speed of 2.0* — equivalently, a charge period at or below `MeleeHits.WINDOW_TICKS` —
  stops adding single-target sustained damage. That is Finding 2's guarantee working: one custom hit
  per victim per window, however fast swings arrive. Vanilla's own i-frame rule enforces the same
  cap upstream, refusing the re-hit before any event reaches us, so it is vanilla-faithful rather
  than ours: fast weapons clear crowds, they do not melt single targets. The boost still scales
  **feel, multi-target throughput, and burst**.

  State the threshold as *effective speed ≥ 2.0*, **never** as a ratio like "past 1.25×" — that
  number is specific to a 1.6 base and is wrong for any other weapon.

  Rejected, and recorded so they are not re-proposed as fixes: owning the i-frames (un-vanilla
  single-target melt, and it unravels the window guard) and capping the stat (throws away the
  multi-target and feel benefit, and needs a weapon-relative cap to mean anything).

- **The tooltip's "Attack Speed" line now has two sources**, branched on `BasicMelee.isVanillaDriven`
  — the same predicate that routes the hit. Melee reads the authored `attack_speed`; ranged still
  derives `20 / cooldown_ticks`, because for a bow that genuinely IS the cadence. Giving the bow a
  display-only `attack_speed:` was rejected: the two would drift the first time someone edited one
  and forgot the other.

- **Weapon swap lags by up to one reconcile period (5 ticks, 0.25s).** Inherent to reconciling from
  the held weapon rather than pinning on the item, and identical to the lag the `ATTACK_DAMAGE` stat
  has always had on the same scan. An item-side base plus a `MULTIPLY_SCALAR_1` player modifier would
  make the swap instant, but it splits one number across two mechanisms and would also speed up
  bare-hand and staff punching.

- **`AttackSpeed.effectiveCooldownTicks` is now ranged-only, and its tests say so.** The exclusion is
  structural rather than conditional: `WeaponFire.attempt` refuses a vanilla-driven melee trigger
  before `AbilityService` ever sees it. `AbilityServiceTest`'s basic-attack fixture was a Melee +
  WeaponDamage shape that since Stage 1 could not reach that code at all — the tests passed while
  documenting a dead path, and now use the bow, the actual surviving consumer.

  **The two must still never both throttle.** A content cooldown and a vanilla charge period are
  alternatives: under a cooldown gate every allowed swing is already fully charged, and
  `AttackCharge` becomes dead code. That is why a melee basic authors no `cooldown_ticks`.


  > #### 2026-08-28 — a REQUIRED content field is a breaking content change, and the boot proved it
  >
  > The first Stage 2 deploy booted clean, said `Done (6.134s)`, and loaded **3 weapons instead of
  > 5**. Both swords were skipped:
  >
  > ```
  > [Rpg] Skipping malformed weapon 'ironblade.yml': weapon 'ironblade' has a vanilla-driven
  >       melee trigger, so attack_speed must be > 0, got: 0.0
  > ```
  >
  > The jar was correct. The DATA FOLDER was not: `saveResource(path, false)` never overwrites, so
  > `run/plugins/Rpg/content/` still held the pre-Stage-2 files with no `attack_speed`, and the new
  > required-field guard duly rejected them. `--refresh-content` fixed the dev loop.
  >
  > **The part that is not a dev-loop annoyance:** this is exactly what happens on a REAL server
  > upgrading to this build. An operator's content files are the source of truth and we must not
  > overwrite them, so every weapon they have edited loses its melee basic on restart until they add
  > `attack_speed:` by hand. Adding a required field to the content schema is therefore a migration,
  > not a code change, and the next one needs a decision it did not get this time: default it,
  > migrate it on load, or version the schema.
  >
  > It failed the right way -- loudly, named, per-file, with the rest of the server up -- which is
  > the whole argument for the guard existing. But "loud" only helps someone who reads the boot log.
  > `Done (` said nothing was wrong.
- **Still open from Stage 1, untouched here:** sweep, the optional player-attributed-`DamageSource`
  kill, a crit multiplier, melee knockback, the multi-attacker i-frame edge, and PvP.

### Stage 1 (vanilla drives basic melee) — what it created or exposed

  > #### 2026-08-28 — the in-game gate is RUN and PASSED, in full
  >
  > All eleven boot rows, plus the two fixes the gate itself turned up. Reported by the player at
  > the keyboard; recorded here at the granularity it was reported, not embroidered per-row.
  >
  > - **The eleven rows** — crosshair not cone, spam vs timed, early swing, kill, broken weapon,
  >   Sharpness through the event path, staff still inert, sweep bystander, durability on connect,
  >   tooltip and attack indicator, one flash not two.
  > - **Kill statistics** (`601b816`) — the counter moves by one per kill. This also retires the
  >   double-count worry: it moving by exactly one confirms `setHealth(0)` credits nothing on its
  >   own, which had been measured only as "the counter sat at zero".
  > - **Enchant glint** (`4fd2767`) — an enchanted weapon shimmers; deactivating clears it.
  > - **Advancements** — witnessed FIRING (see `9dc2775`). This one contradicted a claim rather
  >   than confirming one.
  >
  > **Row 3 was restated before it was run**, and the restatement is the useful part. The plan
  > expected "immediate second swing → ~20-40% damage". With attack speed pinned to 2.0 that cannot
  > happen: vanilla refuses a re-hit inside i-frames unless the new raw amount exceeds `lastHurt`,
  > so an early follow-up after a full-charge hit loses that comparison and raises no damage event
  > at all. `MeleeHits.claimWindow` is never even consulted. The observable form is two rows — an
  > early swing on a FRESH mob for the reduced number, and a re-hit on the same mob for zero — and
  > a full-damage re-hit would have been the real red.
  >
  > **Known cosmetic, accepted on sight:** a click that the window refuses still tokens the vanilla
  > event, so it still flashes. Judged acceptable feedback rather than a phantom hit. If that ever
  > reads wrong, the fix is to move the token below the window claim in `onPlayerMeleeAttack` --
  > recorded now so it is a decision later rather than a discovery.

- **`onCombatKnockback` now suppresses a REAL knockback.** CLOSED by the knockback pass below, which
  took the "let vanilla's through" branch. Kept as the record of the fork and when it opened. As
  written at the time: it cancels vanilla `ENTITY_ATTACK`
  knockback for player→mob, and until Stage 1 there was no vanilla attack to cancel — the melee
  suppressor meant vanilla never entered its attack path at all. Melee still pushes nothing, so the
  *feel* is unchanged, but the suppression is live code for the first time. Owning melee knockback
  (or letting vanilla's through, since no melee weapon declares a `knockback` effect) is a
  deliberate fork, not a side effect of this pass.

- **Vanilla crits are live and unpaid-for.** CLOSED by the crit pass above, which gave the custom
  amount its own rolled multiplier AND suppressed vanilla's particle by packet, so the visual stops
  claiming a hit the engine did not pay for. Kept as the record of what was measured and when. As
  written at the time: measured on the Step 0 boot, a full-charge swing at
  base 6.0 produced `rawDamage=9.0000` — vanilla's ×1.5. That lands on the *tokened* number, so a
  crit currently plays its particles and sound while adding nothing to the custom damage. A `crit`
  multiplier on the custom amount is its own pass; until then the tooltip-free visual is a small
  lie.

- **Multi-attacker i-frames: one custom hit per mob per ~10-tick window, whoever swings.**
  `MeleeHits.claimWindow` keys on the victim, not the attacker, so in co-op a second player hitting
  the same mob inside the window deals **nothing** — where vanilla would let a strictly stronger hit
  add the difference. Correct for Stage 1 (it is what makes spam un-exploitable) and a deliberate
  divergence, not an oversight. A per-attacker window, or mirroring vanilla's difference rule for
  the custom amount, is its own later fork.

- **Co-op knockback leaks one push, from the same per-victim window.** The knockback gate reads
  `MeleeHits.landedThisTick(victim)`, keyed on the victim exactly like the window above — so if
  player A lands a real hit at tick T and player B clicks the same mob on tick T, B is refused the
  damage but B's knockback event still sees A's signal and pushes. One extra shove on a click worth
  zero. **Deliberate, and deliberately not fixed at the gate:** the knockback gate must not be
  finer-grained than the damage window it mirrors — uniform granularity is the whole point of tying
  them together. Same root as the entry above, same eventual fix: a per-attacker window and a
  per-attacker gate, upgraded together in the multi-attacker pass. That pass must first verify
  `EntityKnockbackEvent` reliably exposes the attacking entity, which this handler does not use
  today — `EntityPushedByEntityAttackEvent.getPushedBy()` is the candidate, and the witness boot found the
  concrete class was `EntityKnockbackByEntityEvent` — which exposes it — on all 18 observed events.
  That is encouraging but NOT a guarantee: 18 events from one player against two mob types does not
  establish that every ENTITY_ATTACK knockback arrives as that subclass, and gating on it where it
  does not would over-cancel and stop melee pushing at all. Confirm across more paths before relying
  on it.

- **The `attackSpeed` STAT no longer reaches basic melee.** CLOSED by Stage 2 above, which drives
  the vanilla attribute from the stat; kept here as the record of what Stage 1 deferred and why.
  As written at the time: `AttackSpeed.effectiveCooldownTicks`
  still governs the bow's fire rate through `CooldownTracker`, but a melee swing's cadence is now
  the vanilla `attack_speed` attribute pinned at mint. So `attack_speed_boost_TEMP` is inert on
  swings until Stage 2 drives that attribute from the stat. The two must not both throttle: a
  content cooldown *and* a vanilla charge period are alternatives, because under a cooldown gate
  every allowed swing is already fully charged and `AttackCharge` would be dead code.

- **Authored 2.0 vs true-vanilla 1.6 per material.** SETTLED by Stage 2: melee weapons author
  `attack_speed` directly and both shipped swords declare the true-vanilla 1.6. The old derivation
  read: `mint` derives the pinned attack speed from the
  trigger's `cooldown_ticks` (10 → 2.0/s), so the charge period matches the tooltip's "Attack Speed"
  line. Whether a sword-shaped weapon should instead inherit its material's native vanilla speed is
  a content decision, left open.

- **Vanilla reach is ~3 blocks**, against the `reach: 3.5` the content still declares. Melee is
  slightly shorter than it was. The `cast: type: melee` block is retained regardless — it is the
  discriminator `BasicMelee.isVanillaDriven` reads, not decoration.

- **Sweep is cancelled, not owned.** `onPlayerSweepAttack` cancels `ENTITY_SWEEP_ATTACK` from a
  player outright. Tokening it instead would set a bystander's i-frames and block the next real hit
  on it for ten ticks; leaving it alone would let the rider deal full custom damage to every mob in
  sweep range, which is the retired cone under another name. When sweep becomes a declared effect it
  stops being cancelled there.

  > #### 2026-08-28 — the guard that the boot rewrote
  >
  > The reviewed plan's anti-spam guard read `victim.getNoDamageTicks() <= 10` — vanilla's own
  > "is this a fresh hit" condition — captured in `PrePlayerAttackEntityEvent` before vanilla
  > mutates it. **Step 0's instrumentation disproved it before a line of it was written.** Logging
  > every non-player `EntityDamageEvent` produced 20 `FIRE_TICK` events plus `FALL` and `LAVA`,
  > each driving the same counter:
  >
  > ```
  > [STEP0] OTHER victim=ZOMBIE cause=FIRE_TICK rawDamage=1.0000 victimIFrames=0
  > [STEP0] OTHER victim=BAT    cause=LAVA      rawDamage=4.0000 victimIFrames=10
  > ```
  >
  > A burning mob takes a fire tick a second, so that guard would have read "not fresh" for ~10 of
  > every 20 ticks and dealt **zero** custom melee damage for half of every second — strictly worse
  > than the vanilla it imitates, and worst against precisely the mobs this project's fire element
  > and Scorch DoT are built to create.
  >
  > `MeleeHits` therefore keys on OUR OWN hit history. The general lesson, which is the reason this
  > is recorded rather than just fixed: **vanilla's `noDamageTicks` is a shared bus.** Anything that
  > damages an entity writes it. Reading it as "did *we* just hit this" is a category error that
  > looks correct in a one-mob test and fails the moment the world touches the same mob.


- **`DamagePopupManager.onChange` reads Bukkit inline on the seam thread — a Folia cross-region
  hazard.** The popup listener resolves `Bukkit.getPlayer` / `Bukkit.getEntity` / `getLocation`
  directly in `onChange`. For the **combat** path that runs on the target's owning thread (safe). But
  `/rpg damage` (self) emits `DAMAGE` on the **command thread**, so once Folia is on, reading the
  target/dealer entity there is a cross-region read. On Paper today it's all one thread — safe now.
  The nameplate avoids this (pure `onChange`; entity reads happen in the per-viewer loop on the
  viewer's thread). **Robust fix:** source the target `Location` from `applyDamage` (which already
  holds the entity on its owning thread) and hand it to the popup, rather than re-reading in
  `onChange`. Note now; don't reopen the seam yet — revisit before Folia. (Pass 1b.)
- **Combat via the manual-flash path is silent (no hurt sound).** `applyDamage` uses
  `playHurtAnimation(0f)` (flash only), so ability hits and `/rpg mobdamage` flash but play no hurt
  sound; melee weapons keep the vanilla hurt sound via their tokened `EntityDamageByEntityEvent`.
  The design wanted flash AND sound kept — the sound is a casualty of dropping vanilla
  `entity.damage()`. **Boot-confirmed 2026-07-18:** no *stray* sound either (the old vanilla-damage
  path that produced the stray first-cast sound is gone — the Step 5 check, resolved). If the ability
  path should sound, play the mob's hurt sound alongside `playHurtAnimation`. Polish, not blocking.
- **A mob's nameplate `baseName` does not refresh on rename while tracked.** With `onMobAppear`
  register-if-absent (the fix for the `/rpg mobdamage` every-other-cast bug), renaming a mob while
  it is already plated won't update the plate's base name until it is removed and re-added
  (despawn/chunk-unload → re-appear). Consistent with "custom truth drives display," not a
  regression; note only.
- **A custom mob's nameplate sometimes shows its NAME but no HP until it is first hit.** Observed at
  the custom-mob boot (2026-08-23). Repro: `/rpg spawn knell` shortly after another mob spawns
  nearby — the plate reads `Knell` with no `360/360 ❤` until the Knell takes a hit, at which point the
  `HealthChange` version bump forces a resend and the numbers appear. Minor, cosmetic, and
  self-correcting.

  **Two unconfirmed hypotheses, and do not act on either without measuring first.** It may be a
  per-viewer first-sight / version-state timing race (`ViewerNameplateState.decide` and the 4-tick LOS
  sample against a plate registered on the entity-add event), or it may be the interaction between the
  mob's server-side CustomName — which custom mobs now set, unlike every other mob — and the
  per-viewer packet override of the same metadata index on first sight. Those are different bugs with
  different fixes, and the symptom does not distinguish them.

  **If it proves repeatable, instrument the per-viewer first-sight sends (text + version) and read
  what actually happens. Do not pattern-match a fix.** This is the failure mode CLAUDE.md's
  verification section is about: a plausible story about a race is exactly the kind of explanation
  that gets believed without being tested.
- **Status DoT bypasses custom HP — `scorch` burns *vanilla* health.** `StatusDefinition.Fire`
  applies via `entity.setFireTicks(...)` (vanilla fire), so the burn ticks down vanilla HP with no
  `applyDamage` and no `HealthChange` seam — the mob nameplate never moves as it burns, and the DoT
  is disconnected from the custom-HP source of truth. Confirmed at the damage-pass-1a boot (2026-07-17).
  A later **status-damage pass** should route DoT ticks through `applyDamage` (a per-tick task dealing
  custom damage), the way basic attacks and abilities now do. Deliberately out of scope for pass 1a.
- ~~**`BukkitCombatant.applyHeal` is vanilla-only — ability heals bypass custom HP.**~~ **CLOSED in
  Stats Slice 1.** It called `entity.setHealth(...)`, not `CombatantStats.heal`, so an ability `Heal`
  effect raised *vanilla* health and never fired the seam. For a player that attribute is a DISPLAY,
  rewritten from the custom numbers on the next `HealthChange` or reconcile tick — so the effect
  healed exactly zero of the health combat uses, moved the bar for a fraction of a second, and errored
  about nothing. It now routes to the store. **What it still cannot do is CREDIT anyone**:
  `CombatantHandle.applyHeal` takes only an amount, no `sourceId`, so it self-attributes, which is why
  `/rpg mobheal` still calls `stats.heal` directly. Widening that port is where a heal-credit feature
  (a support archetype's contribution, a heal popup) has to start.
- **Mob projectile→player bypasses custom HP.** Pass 2 (`onMobMeleeAttack`) owns *melee* mob→player
  only — it gates on a `LivingEntity` damager. A skeleton's arrow fires `EntityDamageByEntityEvent`
  with the *arrow* (`Projectile`) as damager, not the mob, so the gate skips it and the shot ticks
  vanilla hearts with no `applyDamage` / no seam. Owning it needs shooter resolution off the
  projectile (`ProjectileSource`), then the same ride-and-token treatment. Its own follow-up, same
  isolate-risk discipline.
- **PvP (player→player) is a rules decision, not wired.** `onMobMeleeAttack` deliberately skips a
  `Player` damager (`attacker instanceof Player → return`), so a player hitting a player currently
  does nothing custom. Whether/how PvP drains custom HP (factions, safe zones, friendly-fire) is a
  design fork for its own pass, not a mechanical gap to quietly fill.
- **Custom HP moves ONLY through the `applyDamage` pipeline, and that pipeline is player↔mob.** The
  umbrella over the four entries above, made concrete at the custom-mob boot (2026-08-23): an iron
  golem fighting a wither skeleton does not touch the custom store at all. Mob→mob, vanilla mob
  combat, fall damage, fire, drowning, cactus — none of it enters `applyDamage`, so none of it fires a
  `HealthChange`, so **the nameplate diverges from the mob's real state**. Measured on the golem
  specifically: its damage state is not tied to its nameplate HP; it can be badly hurt by vanilla
  means while its plate still reads full, and it can die with a full-looking plate.

  **Fine for now, and deliberately so** — players are the only dealer of custom damage, so within the
  loop the game actually has, custom HP is the truth and the plate follows it. The divergence is only
  visible in fights the player is not part of.

  Two consequences, both later passes, neither an optimisation:
  - **Mob→mob is a hard prerequisite for SUMMONER.** A summoner's minions fight mobs; if minion→mob
    and mob→minion damage never enter the pipeline, a minion cannot meaningfully hurt anything and
    cannot meaningfully be hurt. This is why `WeaponClass.SUMMONER` is still deliberately absent from
    the enum — the class needs this before it needs a weapon.
  - **Vanilla/environmental → custom HP is the existing recorded gap**, in its several forms: `scorch`
    burning vanilla health, ~~`applyHeal` raising vanilla health~~ (closed, Stats Slice 1), and mob
    projectile→player skipping the melee gate (all above). They are one problem wearing four hats —
    *every* route into an entity's health that is not `applyDamage` is invisible to the custom store —
    and are best solved as one decision about where the boundary sits, rather than four independent
    patches. **Stats Slice 1 took a bite of that decision rather than a patch**: it also owns
    `EntityRegainHealthEvent`, so the four cancelled player heal reasons are either replaced or
    translated into the store. What remains uncovered is the DAMAGE side.
- **Players are immortal to environmental damage.** Fall/fire/lava/drowning hit *vanilla* health, which
  the heart bar floors at half a heart, so they never kill and never touch custom HP. A known
  consequence of the environmental→custom-HP gap (same class as the Scorch DoT bypass above): player
  death now exists (Death Pass B), but environmental sources still can't trigger it — they don't reach
  custom HP, so `reachedZero` never fires from a fall. Routing them through `applyDamage` is the
  environmental-damage pass's job; revisit there. (The display floor is why a fall can't kill via the
  vanilla bar either.)
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
- **Weapon-lore tooltip — refinements deferred out of the display pass.** The lore pass shipped
  auto-derived stats + authored `flavor`, applied at `WeaponItems.mint` (`WeaponLore` in paper over
  `WeaponLoreLines` in core). Colour ownership was settled with it: the **element** line wears the
  element's own colour, read from `content/elements/*.yml` (an open, content-driven axis — so no
  `ElementColors` switch, deliberately), and the **item name** wears its rarity tier's colour
  unconditionally via `WeaponItems.displayName` (a closed enum — so `RarityColors` in code). An
  authored colour in a weapon's `display_name` no longer wins; the tags were stripped from content
  so no file claims a control it lacks. Deliberately left for later, each its own refinement:
  - ~~**Attack-speed line.**~~ **DONE — and the stat behind it now exists too.** Basic attacks show
    `Attack Speed: 2.0` (attacks per second, vanilla's convention, via
    `WeaponLoreLines.attackSpeedLabel`), derived from the trigger's `cooldown_ticks` (`20.0 / ticks`).
    Attack speed is now a real, modifiable `Stat` (a multiplier, base 1.0) that scales a basic
    attack's **effective cooldown** — see the attack-speed entry below.
    **The tooltip deliberately still shows the weapon's BASE speed, not the holder's resolved stat**,
    per the standing "lore describes the weapon, not whoever holds it" rule that keeps a minted
    tooltip from lying to the next player who picks the item up. `WeaponLoreTest` pins that.
  - **Rarity/enchant stat bonuses on the tooltip.** Phase 4. Rarity only colours + labels the footer
    now; when rarity/enchant grant real stat deltas, surface them in the ability blocks.

    > #### 2026-08-25 — PARTLY ADDRESSED. The block renders; no enchant grants a stat yet.
    >
    > Enchant Pass 1 added `EnchantLore`, which renders one grey-italic line per ACTIVE enchant
    > ("Unbreaking III") at the TOP of the tooltip, above the element line, read from the item's own
    > state. The rarity/class footer is still last, pinned by
    > `theRarityFooterIsStillLastOnceAnEnchantBlockIsApplied`.
    >
    > This entry stays OPEN, because it is about stat DELTAS and Unbreaking grants none — it
    > modifies a durability roll, not a number on the tooltip. The damage-modifier enchant type
    > (Sharpness/Power), which is what would actually need a `+N` line in an ability block, is its
    > own deferred entry below.

    > #### 2026-08-25 — the delta now EXISTS, and the entry still stays open
    >
    > Enchant Pass 2 shipped it: Sharpness/Power/Attunement grant a real percent that multiplies the
    > weapon's damage. So the thing the note above was waiting for has arrived, and the answer to
    > "when rarity/enchant grant real stat deltas, surface them" is now actionable rather than
    > hypothetical.
    >
    > Nothing was surfaced. `EnchantLore` renders "Sharpness III" and no percent; the stat block still
    > shows the weapon's authored base. That is the standing rule holding — lore describes the WEAPON,
    > not whoever holds it — and an enchant is per-item, so it is genuinely the first case where a
    > `+15%` line COULD be honest on a minted tooltip without lying to the next player who picks it up.
    > Which is precisely why it wants deciding rather than drifting into: the enchant percent is
    > item-borne and legitimately renderable, while the class bonus beside it is holder-borne and is
    > not. Two sources into one ability block, one of which may be shown and one of which may not.
    >
    > Note again what did NOT happen: `WeaponLore.build` still takes only `(WeaponDefinition,
    > ElementRegistry)`, and the enchant block is still a separate class over the item's own state.
    > Whatever surfaces the percent should keep that split.
    >
    > Note what did NOT happen: `WeaponLore.build` still takes only `(WeaponDefinition,
    > ElementRegistry)`. Its "mint-time only and cannot drift" promise is enforced by that
    > signature, and `WeaponLoreTest` says so explicitly, so the per-item block went into a separate
    > class rather than widening it. Whatever surfaces these bonuses should do the same.
  - ~~**Per-trigger authored prose.**~~ **DONE** — a per-trigger `name:` (gold ability line) and
    `description:` (YAML-list prose) now render in each ability block; `AbilityDefinition` gained a
    `description` component. Weapon-level `flavor` (italic gray) coexists with it.
  - **Live tooltip refresh.** Lore is applied at mint (give/kit-grant). A weapon already in an
    inventory keeps its old lore until re-minted — re-give to refresh. No reactive refresh wired.
  - ~~**Should an ability payload carry the weapon-class damage label at all?**~~ **ANSWERED: no.**
    The label now follows `WeaponLoreLines.DamageSource`, which models where a trigger's number comes
    from. A `WeaponDamage` effect READS the ATTACK_DAMAGE stat, so a future `+N Melee Damage` modifier
    genuinely reaches it — that line keeps the **class** label. A literal `Damage(12)` reads no stat,
    so no class-typed modifier can ever touch it; it is labelled by its **element** ("Fire Damage: 12")
    rather than promising a relationship that does not exist. The same discriminator decides layout:
    a stat-reading trigger is a basic attack and renders as a stat block, not an ability section.
  - **Trigger damage stops at the first damage-bearing effect.** `WeaponLoreLines.triggerDamage`
    returns the first `Damage`/`WeaponDamage` it finds (recursing into Burst/Area) and stops, so a
    trigger's Status/Knockback/Heal payloads render nothing — the Emberblade fireball's `scorch` is
    invisible on the tooltip, and only the authored `description:` prose mentions it. A plain
    completeness item: surface non-damage payloads ("Scorch (2s)", AoE radius) in the ability block.
- **Custom mobs: what shipped, and the four things deliberately left out.** `content/mobs/*.yml` →
  `MobDefinition` (`core/mob`), tagged onto a spawned entity with a `mob_id` PDC and looked up by that
  tag in `MobNameplateManager.seedCombatStats` via the pure `MobSeeding.maxHealth`. Keyed by mob id,
  **never by entity type** — the Knell is a wither skeleton with 360 HP and ordinary wither skeletons
  are untouched. An entity with no tag takes the vanilla path byte for byte. Deferred:
  - **Natural-spawn integration.** Custom mobs currently appear only via `/rpg spawn <id>`. Making
    them occur in the world — spawn tables, biome/light/condition rules, replacing a proportion of
    natural spawns, spawner blocks — is its own layer. This pass ships the identity and the stat;
    *where they come from* is a separate problem with its own balance questions.
  - **The rest of the mob stat block.** `MobDefinition` is shaped to grow, but only `max_health` is
    wired; attack damage still bootstraps from the vanilla `ATTACK_DAMAGE` attribute for custom and
    ordinary mobs alike. `attack_damage` is the obvious next component.
  - **A damaged custom mob heals to full on chunk unload/reload.** Pre-existing and true of *all*
    mobs — `onMobRemove` clears the store, and `onMobAppear` re-seeds from scratch — but it stops
    being a curiosity once bosses exist. A Knell at 12/360 that the player walks away from comes back
    at 360/360, and a raid boss that resets when a chunk unloads is unfightable. **Custom-HP
    persistence becomes a requirement for the boss use case**, not an optimisation: it needs current
    HP written to the entity's PDC (or a store keyed beyond the entity's lifetime) and re-read on
    appear, instead of the bootstrap-from-max the seed does today.
  - **`randomizeData: false` leaves a spawned mob without vanilla default equipment.** A `/rpg spawn`
    wither skeleton has no stone sword, where a naturally-spawned one does. Deliberate now — a dev
    spawn should be deterministic, and two spawns of the same mob should be the same mob — but
    revisit it with mob equipment in the stat-block layer: content will want to say what a mob
    carries, at which point "no equipment" and "random vanilla equipment" are both wrong defaults.
- **Attack speed is a real Stat now — what it does and what it deliberately does not.** A third
  `Stat` on `HealthState` (multiplier, base 1.0) via the same `ModifierTarget` seam as attack damage,
  converged by the same per-player reconcile loop (`CombatantStats.reconcileAttackSpeedModifiers`).
  `AbilityService.resolve` scales the cooldown it arms by it — `AttackSpeed.effectiveCooldownTicks`,
  `max(1, round(ticks / speed))` — but **only for basic attacks**, keyed on the shared
  `DamagePayload.isBasicAttack`, the same call the tooltip uses to pick a stat block. Decisions worth
  not rediscovering:
  - **Ability cooldowns are untouched, by decision.** An ability's cadence is its balance, not a swing
    rate. A haste stat that also shortened Fireball is a much larger balance question; if it is ever
    wanted, it is its own pass.
  - **A speed change mid-cooldown does not retroactively adjust a running timer.** `isReady` reads
    whatever was armed, so the swing you committed to keeps the cadence it was committed at.
  - **The caster's speed rides `CombatantSnapshot`**, frozen on the caster's own thread, never read
    live from the store inside `resolve` — that would be the same cross-region read documented on
    `CombatWorld.attackDamage`. `BukkitCombatant.snapshot` takes the store rather than offering a
    neutral-defaulting overload, so no call site can quietly ignore modifiers.
  - **Untracked reads 1.0, not 0.0**, unlike `attackValue`. Attack speed is a DIVISOR; 0 would mean an
    untracked caster never swings again.
  - **`attack_speed_boost_TEMP` (`/rpg attackspeed`) owes removal**, with the other `_TEMP` fixtures.
    It exists only because no content grants attack speed yet, so the stat would otherwise be
    invisible at boot and provable only by unit test.
  - Still deferred: showing the RESOLVED (not base) speed anywhere — that belongs to the stat-screen
    pass, alongside `+N Melee Damage` and the rarity/enchant bonuses already parked there.
- **Class-typed damage modifiers — DONE.** `+N <Class> Damage` gear is live for all three classes.
  A per-caster **class-damage bonus** is a fourth `Stat` on `HealthState` (base 0.0, no constructor
  parameter — the whole value is gear-contributed, like attack speed), converged by the same
  per-player reconcile loop, frozen onto `CombatantSnapshot` and carried on `Caster`, and added by
  `EffectApplier` to **both** direct-damage arms. What is worth not rediscovering:

  - **The gate is the HELD WEAPON'S CLASS, not `DamagePayload.isBasicAttack`.** This is the whole
    reason the pass works, and it is what the two entries below were blocked on. A grant counts only
    while a weapon of its class is in the main hand: `+3 Magic` does nothing holding a sword. The
    rule is `ClassDamageModifiers.matching` in `core` — pure, and tested, because its paper
    counterpart (`ClassDamageModifierItems`) needs a live `Player` and so cannot be, exactly like
    `HealthModifierItems` and `AttackSpeedModifierItems` before it.
  - **It is a SEPARATE stat, not class-gated `ATTACK_DAMAGE` modifiers.** The held weapon already
    contributes its `attack_damage` under the `MAIN_HAND` source; folding the class bonus in there
    would double-count through that same source. Keeping them apart is also what keeps the weapon's
    *inherent* damage distinct from the gear bonus added on top — so the emberblade's fireball takes
    +Melee **without** inheriting the swing's 8.
  - **Both arms, so a LITERAL `Damage` gets it too** — `d.amount() + bonus` beside
    `caster.attackDamage() + bonus`. Burst/Area needed no recursion code: they reach damage only
    through `applyTargeted`, carrying the same frozen `Caster`, so changing the two leaves covered
    every nesting. `EffectApplierTest.aLiteralNestedInABurstReceivesTheClassDamageBonus` proves that
    rather than asserting it in a comment.
  - **Folia-safety came free.** The bonus rides the existing cast-time freeze
    (`CombatantSnapshot` to `Caster`), so a bow's `+Ranged` and a mage projectile's `+Magic` are
    fixed at launch. Pinned by
    `ProjectileFlightTest.aProjectileDealsTheClassDamageBonusFrozenAtLaunchNotAtImpact`, which
    changes the bonus mid-flight — **the only possible proof, since Paper is single-region.** Swap
    weapons mid-flight and the arrow keeps the bonus its launch-time class earned: the shot was paid
    for when it was taken.
  - **`amount > 0` now guards the LITERAL arm too**, which previously checked only `alive()`.
    `Stat` permits negative modifiers, so a future `-N <Class> Damage` curse could drive a literal
    net-negative, and `applyDamage` would otherwise push a negative amount into the `HealthChange`
    seam and the damage popup while `HealthState.damage` silently no-ops it. No shipped content
    changes behaviour. `/rpg classdamage` accepts a negative amount specifically so the case is
    witnessable before content authors one.
  - **Unarmed still deals nothing, and now STRUCTURALLY.** No held weapon, so a null held class, so
    `matching` returns an empty map, so the bonus is 0. Weapon-only melee cannot be resurrected by
    gear, and that is enforced in the gate rather than by a convention downstream.
  - **`classDamageValue` returns 0.0 when untracked, NOT 1.0.** A summand, like `attackValue`; the
    asymmetry with `attackSpeedValue`'s 1.0 (a divisor) is deliberate and must not be flattened.
    Reddened by a mutation that returns `AttackSpeed.BASE`.
  - **The reconcile is SILENT** — no `HealthChange`, like attack damage and attack speed. There is
    no display seam for it.
  - **Weapon tooltips are unchanged**, per the standing rule that lore describes the weapon, not
    whoever holds it. `WeaponLoreTest` still pins the base numbers.
  - **A doc lie was retired with it.** `DamagePayload.DamageSource` justified the element label on an
    ability literal with *"No class-typed modifier can ever touch it"* — false as of this pass. The
    labels did not change; the reason did. A basic attack carries the CLASS label because its number
    IS the class's damage stat; an ability literal carries the ELEMENT label because the element is
    that ability's identity and its number is an authored base. **The label is identity, not a claim
    about which modifiers apply.** `WeaponClass`'s "inert beyond labelling the tooltip" and
    `EffectSpec.WeaponDamage`'s inherited "resolved at hit time" (stale since `7af9c43`) went too.

  **The boot record.** Played on a client (`dev-server.sh --refresh-content`; the deploy was
  confirmed byte-identical to the build output with `cmp`, and the three new classes confirmed
  present *inside* the running jar, rather than inferred from a green build). The fixture goes in
  the OFFHAND — the main hand holds the weapon.

  | held | gear | measured |
  |---|---|---|
  | `ember_staff` Ember Bolt | +5 Magic | **16 → 21** |
  | `hunters_bow` Quick Shot | +5 Magic | **inert** (6, unchanged) |
  | `hunters_bow` Quick Shot | +5 Ranged | **6 → 11** |
  | `ironblade` swing | +5 Melee | **8 → 13** |
  | `emberblade` swing | +5 Melee | **7 → 12** |
  | `emberblade` Fireball | +5 Melee | **12 → 17** |
  | `ember_staff` tooltip | +5 Magic | still reads base **16** |

  **Two of those lines are the whole argument, and neither could have come from a unit test.**

  - **`ember_staff` 16 → 21 is the blocker retired.** That weapon reads no stat at all — a literal
    `damage:` payload on `attack_damage: 0` — which is exactly why a modifier keyed on ATTACK_DAMAGE
    could not have reached it, and why the entry below forbade shipping one. The bonus reaching it
    without any weapon conversion is the proof that keying on the held weapon's class was the right
    axis.
  - **Fireball 12 → 17, NOT 24, is the separate-`Stat` architecture proven.** 24 is what the
    folded-into-ATTACK_DAMAGE design produces: the 12 literal, plus the emberblade's inherent swing
    damage of 7, plus the 5 bonus. 17 is what a separate stat produces. **The number distinguishes
    the two designs**, so this is a positive result about the architecture rather than the absence of
    a bug — the bonus adds on top of a payload without that payload inheriting the weapon's swing
    damage. If this line ever reads 24, the fourth `Stat` has been folded into the attack stat.
  - **The staff tooltip still reading base 16** closes the loop on the doc correction above: the
    label rule changed its *reason* this pass without changing its *behaviour*, and that is now
    witnessed rather than argued. The resolved, gear-boosted number remains the stat-screen pass's
    job.

  **Two qualitative sightings, confirmed as designed, no figures taken.** Recorded because both look
  like bugs to anyone who has not read the decisions above:

  - A **standalone ability cast while holding a matching-class weapon takes the bonus** — the
    consequence recorded in its own entry below. Seen, and correct: `CastExecutor` builds the
    `Caster` for every cast, so this is the design, not a leak.
  - **`ability_stone` is `class: mage`**, so merely holding it activates `+Magic`. Correct by the
    rule — it is a weapon of ours and it declares a class — but surprising, since it is a dev tool
    rather than a combat weapon. Not a defect; if it should be exempt, that is a content decision
    about whether a utility item should declare a class at all.

- **CONSEQUENCE: standalone ability literals are now gear-scalable.** `CastExecutor` builds the
  `Caster` for **every** cast, so `/rpg cast solar_grenade` with a sword in hand takes +Melee, and a
  kit spell with a staff in hand takes +Magic. This is deliberate, not a leak: `+Class Damage` is a
  build stat that scales your whole class output, not just the weapon's autoattack, and it is what
  the held-weapon-gated `Caster` model already produces. Three things follow:
  - an ability's authored `amount:` is now a **base**, with matching gear adding on top;
  - an ability that deals no direct damage (a pure dash, a heal, a status) is untouched;
  - **cross-class casting gates by the held weapon, by design** — cast a mage spell holding a sword
    and it takes +Melee, not +Magic.

  The alternative (weapon-trigger-only) was declined on cost: it would need `WeaponService` to mark
  the cast and `AbilityService`/`CastExecutor` to thread a from-weapon flag, for a distinction the
  build-stat framing does not want.

- **What the class bonus deliberately does NOT reach.** Both are pre-existing boundaries, recorded
  so their absence is not later read as a bug:
  - **Mob to player melee.** `RpgListeners.onMobMeleeAttack` reads `stats.attackValue(attackerId)`
    directly at hit time — no snapshot, no `Caster`. Mobs are never reconciled, so a mob's
    class-damage stat stays at base 0 regardless; `CombatantStatsTest` pins that a bootstrapped mob
    gets its vanilla attack damage and no class bonus.
  - **Status DoT.** Unchanged, and part of the separate environmental/custom-HP gap below.

- ~~**Ranged and magic have no stat-reading basic attack, so class-typed modifiers have nothing to
  grip.**~~ **RESOLVED FOR DAMAGE, ALL THREE CLASSES.** The entry quoted below was written on the
  assumption that a class-typed modifier would key on the ATTACK_DAMAGE stat, which made a
  `+Magic Damage` inert on `ember_staff` (a literal `damage:` payload, `attack_damage: 0`) and led
  to the instruction *"the class-modifier pass must not ship a `+Magic Damage` stat that silently
  does nothing."* The modifier pass **retired the blocker rather than working around it**: keying on
  the held weapon's class and adding to both damage arms means `+Magic` grips the staff's literal
  bolt with no weapon conversion at all. The original text is kept because the reasoning that
  produced the blocker is worth not repeating.

  **Still true, and now the whole of what is left:** `ember_staff` has no stat-reading basic attack,
  so it gets **no attack-speed scaling**. That is the residue, and the decision entry is narrowed to
  it.

- **DECISION OWED (narrowed) — does the Mage get a basic attack? An ATTACK-SPEED question only.**
  `+Magic Damage` is live and needs nothing from this decision. What `ember_staff` still lacks is a
  `weapon_damage` payload, and the only thing that now costs is attack-speed scaling — which keys on
  `DamagePayload.isBasicAttack`. The fork is unchanged in shape: convert the costed Ember Bolt (which
  would declare a costed spell to be the Mage's basic attack, and attack-speed-scale its cooldown —
  something the attack-speed pass deliberately excluded for abilities), or author a **separate free
  basic attack** (net-new content, and it changes the "the verb is commit" economy `ember_staff`'s
  own comments describe). No longer blocking anything.

**The three superseded entries, kept verbatim below — the record gets annotated, not rewritten:**

> - **Ranged and magic have no stat-reading basic attack, so class-typed modifiers have nothing to
>   grip. Decide this UP FRONT in the modifier pass, not during it.** Only a `weapon_damage` effect
>   reads ATTACK_DAMAGE, and in shipped content exactly two triggers use one: `ironblade/left_click`
>   and `emberblade/left_click` — both MELEE. `hunters_bow`'s Quick Shot and `ember_staff`'s Ember Bolt
>   carry **literal** `damage:` payloads and declare `attack_damage: 0`, so a `+N Ranged Damage` or
>   `+N Magic Damage` modifier would have no stat to modify and would silently do nothing. The tooltip
>   already tells this truth (neither weapon renders a class-labelled damage line at all), which is how
>   it surfaced.
> 
>   **The attack-speed pass widened this.** Because attack speed also keys on
>   `DamagePayload.isBasicAttack`, bow and staff get **no attack-speed scaling either** — an
>   `attack_speed_boost_TEMP` visibly speeds up a sword and does nothing at all to a bow. So the same
>   root gap now costs ranged/magic two stats, not one, and the question is correspondingly bigger.
> 
>   The modifier pass must choose deliberately, before writing the modifier:
>   - **Convert bow/staff first** — give them `attack_damage:` and a `weapon_damage` on_hit so their
>     basic attacks become stat-driven, and both `+N Ranged/Magic Damage` and attack speed start
>     working for them. **This is NOT a content-only edit, and the entry below is the reason:** a
>     `weapon_damage` payload on a PROJECTILE resolves on the target's region, cross-region from the
>     caster, which is the documented Folia race. Converting them requires first snapshotting the
>     caster's attack damage at CAST time into the effect payload. Budget that work as part of the
>     choice, not as a surprise inside it. Note also the staff's bolt is costed, so "basic attack" may
>     not be the right shape for it regardless.
>   - **Or ship melee-only** — accept that `+Ranged`/`+Magic` are inert until those weapons get real
>     basic attacks, and say so in the modifier's own lore rather than shipping a dead stat.
>   Either is defensible; picking by accident is not.
> 
>   **RESOLVED FOR RANGER, STILL OPEN FOR MAGE.** The cast-time-snapshot pass paid the plumbing cost the
>   bullet above warned about — the freeze is built and `CombatWorld.attackDamage` is gone — and converted
>   `hunters_bow`: `attack_damage: 6` plus a `weapon_damage` on_hit. So `+N Ranged Damage` now has a stat
>   to grip, and the bow picked up attack-speed scaling in the same move (both key on
>   `DamagePayload.isBasicAttack`). **The staff was deliberately left alone**, because its blocker was
>   never the plumbing — it is a design question, recorded as its own item below. So the modifier pass
>   inherits a narrower version of this choice: `+Ranged` works, `+Magic` still has nothing to modify.
> 
> - **DECISION OWED — does the Mage get a basic attack, and is it the staff's bolt?** `ember_staff`'s
>   Ember Bolt is a **costed burst**: 30 energy, a 20-tick cooldown, and its damage nested inside a
>   `burst:` rather than at the top of `on_hit`. Converting it to `weapon_damage` is mechanically trivial
>   now (the projectile freeze is built, and `WeaponDamage` is `Targeted` so it nests inside a burst
>   legally), which is exactly why it must be decided rather than drifted into. It would mean:
>   - declaring a **costed spell to be the Mage's basic attack**, and
>   - **attack-speed-scaling that spell's cooldown** — which the attack-speed pass deliberately excluded
>     for abilities, on the grounds that an ability's declared cooldown is its balance, not a swing rate.
> 
>   The alternative is a **separate free basic attack** for the staff (a left-click wand bolt, say), which
>   mages in most games want anyway — something to do when the energy bar is empty — but that is net-new
>   content, not a conversion, and it changes the Mage's "the verb is commit" economy that `ember_staff`'s
>   own comments describe.
> 
>   Until this is answered: `+Magic Damage` and attack speed are inert for mage weapons, and **the
>   class-modifier pass must not ship a `+Magic Damage` stat that silently does nothing.** Either convert,
>   or author the free basic attack, or say plainly in the modifier's own lore that Magic is not yet live.
> - **Class-typed stat modifiers.** `WeaponClass` (MELEE/RANGER/MAGE; SUMMONER deferred until it has
>   mechanics) is now a required weapon axis — it labels the tooltip (`WeaponClassLabel`: Melee/Ranged/
>   Magic) and nothing more. The intended mechanic: a `+N <Class> Damage` modifier that applies **only**
>   while the held weapon's class matches (a mage ring boosts staves, not swords). This pass adds the
>   axis; the modifier + its resolution (a stat screen / the modifier item's own lore, **never** folded
>   into the weapon's static base display) is a later pass. The tooltip's damage numbers stay the
>   weapon's declared base, pre-modifiers, by design — that is why they can't drift.

- **The tuning loop is silently broken, and has been all along.** `RpgPlugin` ships
  defaults with `saveResource(path, false)`, which **never overwrites**. So editing
  `paper/src/main/resources/content/abilities/solar_lance.yml` in the repo, rebuilding,
  and rebooting does **nothing** to a server whose data folder already holds that file.
  You tune, you restart, you cast, nothing changed, and the only complaint is a `WARN`
  you have been reading past since the first boot:

  ```
  [Rpg] Could not save solar_grenade.yml to plugins\Rpg\content\abilities\solar_grenade.yml
        because solar_grenade.yml already exists.
  ```

  Three of those, every boot. Not an error, so nobody looks. This is the loop "Then
  stop and play it" asks you to *time*, and it would have measured the wrong thing.

  **Workaround, until fixed:** delete the file from `run/plugins/Rpg/content/` before
  rebooting. Or edit `run/plugins/Rpg/content/…` directly and copy back to the repo
  when you like the numbers — which is what the tuning-loop text actually describes.

  The real fix is a content *reload* that reads the data folder without `saveResource`
  in the path, or a `--refresh-content` flag on `dev-server.sh`. Not folded into E.

  > #### 2026-07-10 — the `--refresh-content` flag, and what re-triggered it
  >
  > It bit exactly as predicted, one phase later. Phase 2B re-elemented the sample content
  > (solar→fire, arc→nature) in the source and committed it clean; a source grep confirmed
  > no `element: solar` remained. The boot then warned about **12 dangling elements** anyway
  > — because it loads `run/plugins/Rpg/content/`, not the source, and `saveResource(false)`
  > had left the Phase-1 files (weapons AND abilities, all `solar`/`arc`) untouched. The
  > acceptance grep checked the source; the server read the stale data folder; the two
  > diverged silently. Same false-green shape as a check aimed at the wrong artifact.
  >
  > **`dev-server.sh --refresh-content`** is the fix the text above named. It clears
  > `plugins/Rpg/content` after deploy so the plugin re-copies fresh from the jar on boot.
  > Deliberately opt-in, and deliberately NOT `saveResource(path, true)`: overwrite-on-boot
  > would clobber an operator's intentional edits on a real server. The flag touches only the
  > local `run/` folder and leaves `saveResource(false)`'s protection intact.
  >
  > Proven both ways, on a real boot: dirty the deployed `emberblade` to `solar`, then
  > `--refresh-content` re-copies it to `fire` and the log reads `7 elements, 0 dangling`;
  > a *plain* boot of the same dirtied folder leaves it `solar` and warns on 3 dangling —
  > the no-clobber default is unchanged. The tuning loop now actually works: edit source,
  > `dev-server.sh --refresh-content`, and the running server loads what you wrote.

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
- **The `_TEMP` fixtures owe removal in the content pass** (`rooted_TEMP`, `soaked_TEMP`, and
  the two item fixtures noted at the end of this entry). They are throwaway
  fixtures wired onto real abilities to make statuses castable in-game before `/rpg apply`
  existed. `/rpg apply` is the permanent replacement — it applies any status at any
  stack/duration without touching ability content, which is the entire reason the `_TEMP`
  pattern existed. Deliberately left in place through the Freeze merge (`5620943`); removing
  them is content work, not status work. Five reference sites:

  ```
  paper/src/main/resources/content/abilities/solar_grenade.yml
  paper/src/main/resources/content/abilities/void_slash.yml
  paper/src/test/java/io/github/butterflysmp/rpg/paper/content/AbilityLoaderTest.java
  paper/src/test/java/io/github/butterflysmp/rpg/paper/content/ContentValidatorTest.java
  PLAN-dev-apply-command.md            # context only, no fixture to remove
  ```

  **The ambush: the last two are guards that assert `rooted_TEMP` is PRESENT.** So this is
  not a delete-two-YAML-keys job. Strip the fixture without touching the tests in the *same*
  change and the suite goes red — bundled-content assertions failing on content you
  legitimately removed. Measured, not inferred; the three that break, exactly:

  - `AbilityLoaderTest` — `assertEquals(3, burst.effects().size())` becomes 2.
  - `AbilityLoaderTest` — `burst.effects().get(2)` throws `IndexOutOfBounds`, so it fails
    on an exception rather than a clean assertion diff. Delete the `rootedTemp` block.
  - `ContentValidatorTest` — stages `rooted.yml` beside `scorch.yml` and asserts
    `assertEquals(2, statuses.size())`. Back to `scorch.yml` only, and 1.

  Both test files already carry in-place comments saying so at the assertion; this entry
  exists because the content pass starts here, not there.

  **`soaked_TEMP` is NOT test-guarded** — it lives only in `void_slash.yml` and comes out
  clean. The asymmetry is the trap: removing `soaked_TEMP` first will pass and teach you
  that removing `rooted_TEMP` is the same job. It is not.

  **There are now NINE `_TEMP` fixtures, not two, and seven of them are a different shape.**
  This entry was written when the debt was status-content only; the stat passes added seven
  ITEM fixtures, which live in Java rather than yml and so will not turn up in a content-pass
  grep of `content/`:

  | fixture | mints via | lives in | proves |
  |---|---|---|---|
  | `rooted_TEMP` | ability content | `content/abilities/*.yml` | status castable in-game |
  | `soaked_TEMP` | ability content | `content/abilities/void_slash.yml` | as above |
  | `health_boost_TEMP` | `/rpg healthboost` | `paper/health/HealthModifierItems.java` | the equip/unequip max-HP modifier lifecycle |
  | `attack_speed_boost_TEMP` | `/rpg attackspeed` | `paper/weapon/AttackSpeedModifierItems.java` | the same lifecycle for attack speed |
  | `class_damage_boost_TEMP` | `/rpg classdamage <class> [amt]` | `paper/weapon/ClassDamageModifierItems.java` | the same lifecycle again, plus the class GATE: it goes inert when you swap to another class's weapon |
  | `crit_chance_boost_TEMP` | `/rpg critchance [bonus]` | `paper/health/CritModifierItems.java` | the same lifecycle for how OFTEN you crit |
  | `crit_damage_boost_TEMP` | `/rpg critdamage [bonus]` | `paper/health/CritModifierItems.java` | and for how HARD -- two stats in one class, moving independently |
  | `health_regen_boost_TEMP` | `/rpg healthregen [bonus]` | `paper/health/HealthRegenModifierItems.java` | the same lifecycle for the regeneration RATE: +0.8 on a 0.2 base is a resolved 1.0 HP/s, countable at a glance |
  | `mana_regen_boost_TEMP` | `/rpg manaregen [bonus]` | `paper/health/ManaRegenModifierItems.java` | the same lifecycle for the MANA rate, plus the PIN: equip it after idling and mana must not jump |

  **THIS TABLE HAD GONE STALE BY TWO BEFORE Stats Slice 1 TOUCHED IT.** It said FIVE and listed
  five; the crit pair landed in the crit slice, whose own retrospective says in as many words
  that they "join the other `_TEMP` fixtures owing removal", and they were never added here.
  Nothing pins this list against the code — it is hand-written and can drift again, exactly as
  `EnchantRollTest.ROSTER` did for two slices. There is no test. The check is

  ```bash
  grep -rhoiE "[a-z_]+_temp" paper/src/main --include=*.java --include=*.yml \
    | tr 'A-Z' 'a-z' | sort -u
  ```

  **The `tr` is not optional.** Without it the `-i` search returns each fixture twice — once as
  `health_boost_TEMP` from the prose and once as `health_boost_temp` from the `NamespacedKey` — so
  the "count" is roughly double and means nothing. That was wrong in this entry for one slice, which
  is a small instance of the thing this file keeps recording: a documented check that does not do
  what the prose beside it claims.

  **And it returns TEN, not nine.** The extra one is `swing_TEMP`, REMOVED when the swing listener
  shipped, surviving only in prose describing its own removal (`WeaponSwingListener`,
  `solar_grenade.yml`, `void_slash.yml`). A retired fixture and a live one look identical to that
  grep, so read each hit before counting it — a count taken straight off the grep is wrong today
  and will be wrong differently later.

  The seven item fixtures come out when real content grants those stats (an enchant, a passive,
  a build aspect) — `WeaponAttackItems` is already the shape that replaces them, sourcing a
  stat from actual weapon content instead of a fixture. Each also owns a `/rpg` dev subcommand
  and a `Keys` PDC entry, so removing one is three sites, not one.

  **`class_damage_boost_TEMP` is FOUR sites, not three.** It is the first fixture needing two PDC
  values — an amount *and* the class it grants to — so it carries two `Keys` entries
  (`classDamageBoost`, `classDamageBoostClass`). A grant missing either, or naming a class
  `WeaponClass.fromName` no longer recognises, is treated as not ours and contributes nothing.

- **`WeaponDamage` reused for a RANGED weapon was a Folia cross-region race — RETIRED.** The
  attack-damage pass made the basic melee hit deal the caster's `ATTACK_DAMAGE` stat, read at HIT time
  via `CombatWorld.attackDamage(casterId)` (`PaperCombatWorld` -> `CombatantStats.attackValue`). That read
  was only legal on the thread owning the caster: safe for MELEE, where the caster is within reach of the
  target and so shares its region, but a race for a **ranged** weapon, whose projectile resolves its
  payload on the TARGET'S region, cross-region from the caster.

  The cast-time-snapshot pass closed it, and closed it **structurally rather than by convention**:
  `CombatantSnapshot` gained `attackDamage`, captured in `BukkitCombatant.snapshot` under
  `Regions.requireOwned` exactly as `attackSpeed` already was; `Caster` (`core.combat`) is the frozen
  projection of a snapshot down to what an effect landing LATER may read — an id plus stats, deliberately
  not position/liveness — and it is threaded through `CastExecutor`, `ProjectileFlight` and
  `EffectApplier` in place of the bare `casterId`. **`CombatWorld.attackDamage` no longer exists**, along
  with its `PaperCombatWorld` implementation and the `FakeWorld` map. So a hit-time read is not merely
  discouraged; the port offers no method that could perform one.

  Melee moved to the same frozen semantics rather than keeping its live read — a recorded decision, not a
  side effect. Cast is effectively hit for a swing within reach, so the value is current, and unifying
  leaves ONE path instead of two with the race half-present.

  Proven by `ProjectileFlightTest.aProjectileDealsTheAttackDamageFrozenAtLaunchNotAtImpact`, which
  changes the caster's stat mid-flight and asserts the launch-time value lands. That core test is the
  only possible proof: **Paper is single-region, so no boot can make the two regions differ.**
- **Attack-SPEED is deferred.** The attack-damage pass built the stat machinery (a second `Stat` on
  `HealthState`, a `reconcileAttackModifiers` loop, `WeaponAttackItems`); attack-speed slots in as the next
  stat using the same machinery, modifying the per-trigger `cooldown_ticks` fire rate. Not started.
- **COSTED/ability payloads keep their LITERAL `amount:`.** The staff (`ember_staff`), the emberblade
  right-click special, `solar_grenade`, and Rekindle still deal a literal `EffectSpec.Damage`, not
  `WeaponDamage`. The **bow is no longer on this list** — `hunters_bow` was converted by the
  cast-time-snapshot pass and now declares `attack_damage: 6` with a `weapon_damage` on_hit, so the
  Ranger's shot is stat-reading and attack-speed-scaled. The staff was deliberately NOT converted; see
  the Mage basic-attack decision below.
- **Elemental damage is still identity, not math.** The attack-damage pass did not add a multiplier;
  `WeaponDamage.element()` flavours the hit and gates kits exactly as `Damage.element()` does. See
  `Element.multiplierAgainst` below.
- **`Element.multiplierAgainst`** is still the placeholder 1.5x/1.0x rule.
- **`RpgCommand`'s hop is on the caster's eye**, not the impact point. Now
  harmless: reads are snapshots captured under `requireOwned`, and the ray steps
  region by region. The comment there should say that rather than apologise for
  it.
- **Naturally-spawned enchanted equipment is the one leak in the no-vanilla-enchants policy.** The
  policy holds for player-held items by construction — custom UI replaces both the anvil and the
  enchanting table, so a player can never apply a vanilla name or enchant, which is why the Lore
  Refresher regenerates canonically with nothing to preserve on that axis. What it does not cover is
  gear that arrives already enchanted: a skeleton spawning with an enchanted bow, and any of it that
  reaches a player's hands as a drop. Out of the refresher's scope and untouched by it (no
  `weapon_id`, so the scan reads it as not ours and forms no opinion), but a real future decision:
  **strip / convert to a custom weapon / leave** vanilla-enchanted mob gear.

  > #### 2026-08-25 — THE POLICY IS NOW LOAD-BEARING, not aspirational.
  >
  > Until Enchant Pass 1 nothing depended on it. Now Unbreaking is ours end to end: a custom curve
  > in `core/enchant/Unbreaking.java`, read off our own PDC blob, applied at our own wear seam. No
  > vanilla `Enchantment` is consulted anywhere on a player-held item, and none can be.
  >
  > Recorded because the shortcut is genuinely tempting and was genuinely taken before: the old repo
  > (`BSMPMenu`) mapped its custom Unbreaking straight onto vanilla's in
  > `item/EnchantVanillaSync.java:51` and let the server apply it. That is a one-line "simplification"
  > someone will re-derive. It is forbidden here, and the leak above — mob gear that spawns already
  > enchanted — remains the only vanilla enchant that can reach a player.
- **Is durability an RPG axis at all — should custom weapons be unbreakable?** Raised by the Lore
  Refresher and deliberately not answered by it. The refresh carries accumulated wear forward
  (`WeaponItems.carryWear`) so it stays strictly display-only: resetting it would silently repair
  every weapon on every login, which is a relog-to-repair exploit *and* a balance decision made as a
  side effect of a presentation pass. Carrying it forward forecloses nothing — if weapons later mint
  `Unbreakable`, the carry-forward quietly becomes a no-op. Nothing in the codebase sets
  `Unbreakable` or reads `Damageable` (verified: no `setDamage` / `Damageable` / `setUnbreakable`
  anywhere in `paper/src/main` or `core/src/main`), so wear is entirely vanilla's business.

  > #### 2026-08-24 — "vanilla wear accrues untouched" was an assumption, and the boot contradicted it
  >
  > This entry originally asserted that wear accrues because "the melee suppressor zeroes a swing's
  > *damage*, not the durability the swing costs." That was reasoning, not a measurement, and the
  > lore-refresher boot found the opposite: **nothing wears items today.** Melee is packet-driven and
  > bypasses vanilla's durability charge, so `carryWear` currently copies a damage of 0 and its clamp
  > `min(0, max-1)` is trivially satisfied. The carry-forward is correct and inert, not load-bearing.
  >
  > What this does NOT settle, and should be measured rather than re-reasoned when the durability
  > pass starts: *why* the charge is skipped. The plausible story is that vanilla only calls
  > `hurtEnemy` when the attack actually lands damage, and the suppressor brings the swing to 0 — but
  > that is again a story. Swing a weapon thirty times and read the durability bar; that settles it in
  > one observation, and it is worth doing while some other boot is already running.
  >
  > #### 2026-08-25 — MEASURED. The bar does not move.
  >
  > Taken on the Durability Pass 1 boot: **six iron golems, ~78 swings with a tagged weapon, zero
  > durability charge** — the bar did not move at all. The standing question is answered on the axis
  > that matters: **no wear accrues**, so `carryWear` copying a damage of 0 is the real steady state
  > and not an artefact of a short test. Pass 1 adds no auto-wear either — its `/rpg durability`
  > command is the only thing in the build that moves the value — so this still holds as shipped.
  >
  > Recorded as the observable, which is what the plan depends on. ~78 swings landing zero charge
  > rules out "it charges slowly"; it does not by itself discriminate between the candidate
  > mechanisms for *why* (vanilla charging only on the attack path our ANIMATION-packet route never
  > takes, versus charging only when damage lands and the suppressor zeroing it). Pass 2 has to pick
  > a wear trigger deliberately regardless, so the mechanism stops being load-bearing the moment
  > wear is ours rather than inherited — which is exactly why this is now closed rather than left
  > open as a story.
  >
  > #### 2026-08-25 — ANSWERED by Pass 2: wear is ours now, and the mechanism stopped mattering
  >
  > Durability Pass 2 supplies the wear the measurement above proved vanilla will never supply. It
  > is applied by us, on the actions we choose — a melee basic attack that connects, a ranged basic
  > attack at launch, flat 1, `Damageable` materials only — through one chokepoint
  > (`WeaponDurability.applyWearOnUse`), with the *when* decided in `CastExecutor.execute`.
  >
  > So the open sub-question — *why* vanilla skipped the charge — is now moot rather than answered,
  > which is the outcome the entry above predicted. Nothing depends on it: we do not ride vanilla's
  > trigger, so it does not matter which of the two candidate mechanisms was suppressing it.
  >
  > **Two consequences worth naming.** `WeaponItems.carryWear` stops being inert — it now carries
  > a real accumulated value across a re-mint rather than copying a 0, so its clamp is live code on
  > the next content re-theme, not insurance against a case that cannot arise. And the *"should
  > custom weapons be unbreakable"* question this entry opened is answered in the negative by
  > construction: they wear, they floor, they go inert, they repair. The never-DESTROYED promise is
  > untouched; it was never the same promise as never-worn.
- **The Lore Refresher's boot gate step 9 (the lower-durability clamp) is DEFERRED, un-runnable.**
  It needs a *worn* item whose material then changes to a lower-max material (iron 250 → gold 32), and
  per the correction above nothing wears items in the current build, so the case cannot be produced.

  > #### 2026-08-25 — NOW PRODUCIBLE (Pass 2), and no longer needs a forged item
  >
  > Pass 2 wears weapons in play, so the *worn item* half of this case is now reachable by simply
  > swinging: hit a mob a few times and read the bar. That retires the forged-`/give` candidate
  > below — it was only ever a way to manufacture wear that did not exist, and the `/rpg refresh`
  > count diagnostic it needed goes with it. Pass 1's `/rpg durability damage <n>` is the faster
  > path still, since it reaches a chosen value in one command.
  >
  > Not run as part of Pass 2, deliberately: this pass was closing the wear axis, and step 9 belongs
  > to the refresher's gate. It is now a normal runnable step rather than a deferred one.
  Deliberately not held against the refresher: the clamp is correct, inert, and cheap insurance
  against a real data-loss bug (an item copied to a damage value past its new maximum is a *broken*
  item). **Boot-witnessing the clamp is the first gate of the durability pass** — same wear axis,
  exactly where it belongs. Steps 4, 5 and 7 were witnessed and are recorded with that pass.

  There is no existing dev path to force wear: no `/rpg` subcommand mutates an item, and nothing in
  the plugin touches `Damageable`. The candidate that needs no new code is a forged item via vanilla
  `/give` — PDC `STRING`s live in `minecraft:custom_data` and the plugin's namespace is `rpg`, so
  `/give @s iron_sword[minecraft:custom_data={"rpg:weapon_id":"emberblade"},minecraft:damage=200]`
  *should* mint a pre-worn tagged weapon. Unverified. If it is tried, `/rpg refresh`'s **count** is
  the diagnostic that keeps a failed forge from being misread as a broken refresher: `0` means the
  tag never round-tripped, `1` means it did and the re-mint ran.

  > #### 2026-08-25 — step 7 was witnessed, but NOT by the procedure the gate described
  >
  > The gate said *"delete `emberblade.yml`, restart, relog"*. **That cannot run.** Every shipped
  > weapon is pinned to the classpath by at least two tests, so deleting any of them reddens the
  > suite and `dev-server.sh` aborts on `set -euo pipefail` (`:13`) before it boots. `emberblade.yml`
  > alone is asserted by `WeaponLoaderTest:513`, `WeaponLoaderTest:616` (the three-weapon loop, which
  > also pins `registry.size() == 3` at `:623`) and `WeaponLoreTest:345`. The step was unreachable
  > rather than merely awkward, and it sat in the PR body looking runnable — which is the failure
  > worth recording, not the deletion itself.
  >
  > Witnessed instead with a **throwaway weapon**: add a minimal `testdangle.yml` (only `class:` and a
  > `triggers:` section are required, and the id is the *filename*), `/rpg give testdangle`, delete
  > the file, rebuild, `--refresh-content`, relog. The warn-once line appeared and the item survived
  > untouched. The fixture was never committed.
  >
  > **`--refresh-content` is load-bearing on that second boot.** `saveResource(path, false)` never
  > *removes*, so without the flag's `rm -rf "$CONTENT_DIR"` the stale deployed copy still loads, the
  > id never dangles, and the gate passes having tested nothing.
  >
  > The asymmetry that makes this work is worth keeping for the next fixture: **nothing in the repo
  > resolves `content/weapons` as a directory** — every test names its ids explicitly — so an *added*
  > file is invisible to the suite while a *deleted* one is not. Note that
  > `WeaponLoreTest.everyShippedWeaponRendersAgainstTheShippedElements` reads as though it scans and
  > does not; it is a hardcoded five-id array, and its own javadoc names the trap without closing it.
- **`/rpg refresh` reports the opposite of what the `Dangling` verdict decided.** Carrying only a
  dangling item, the count is 0 — `refreshed++` lives in the `Remint` arm alone
  (`WeaponRefresher.java:71`) — so the command says *"Refreshed 0 weapons -- you are carrying none of
  ours."* That is **false**: the item *is* ours, which is exactly what `RefreshVerdictTest:88-89`
  pins (*"it IS ours -- silently skipping it would hide a real content break"*). The verdict draws
  the distinction and the chat line collapses it. The truth reaches the console once via `warnOnce`,
  so a *second* `/rpg refresh` prints the misleading line with no warning beside it at all.

  Surfaced by the step-7 boot on 2026-08-25, where carrying an ironblade *alongside* the dangling
  item was what made the count mean anything — `1` rather than `0` proves the scan ran and then
  deliberately declined to touch the dangling slot. Without that, "found nothing" and "did nothing"
  are the same observation, which is the trap the count exists to close.

  The fix is a third message ("N refreshed, M unknown"). Not taken in #12: it is a behaviour change
  to a shipped command, and #12 was closing a documentation gap.
- **`carryWear` copies ABSOLUTE damage, so re-theming a weapon to a lower-max material arrives
  BROKEN.** Witnessed on the durability pass's boot gate 10 (2026-08-25): a healthy 100/250 iron
  ironblade, re-minted onto `golden_sword` (max 32), clamps to damage 31 — which is 1 use, and 1 use
  IS the broken floor. So a content re-theme silently hands every player who owns that weapon a dead
  one until they repair it.

  **This is the never-break promise working, not failing.** Without the clamp that item is damaged
  past its maximum, i.e. destroyed outright; with it, the worst case is useless-until-repaired. But
  "useless until repaired" is still a gameplay event a *presentation* change caused, and it is worth
  being explicit that a clamped item is ALWAYS broken rather than merely worn: `Durability.clamp`
  floors at `max - MIN_USES` and `Durability.isBroken` fires at `>= max - MIN_USES`, the same value.
  There is no such thing as a clamped-but-usable item, by construction.

  Only bites on a change to a *lower*-maximum material, which is rare — #12 already accepted the
  fraction shift in the other direction (50/250 iron becoming 50/1561 diamond reads as less worn).
  If re-theming should never cost a player a working weapon, the fix is to preserve the wear
  FRACTION across a material change rather than the raw value: 40% worn stays 40% worn, so
  100/250 iron becomes 13/32 gold and the weapon survives usable. Not taken here — it changes what
  a re-mint means on every material change, not just the lossy direction, and this pass was closing
  the break gate rather than reopening #12's carry-forward contract.

  > #### 2026-08-25 — Pass 2 makes this bite for real
  >
  > While nothing wore items, this was a latent bug: `carryWear` copied a damage of 0, so the clamp
  > could never fire. Pass 2 means players now carry genuinely worn weapons, so the FIRST re-theme
  > to a lower-max material will hand every owner of that weapon a dead one. Still not fixed here
  > (same reason — it is #12's contract, not this pass's), but it has moved from theoretical to
  > waiting, and the fraction-preserving fix above is the one to take when it is.
- **Per-weapon wear rate is deferred; Pass 2 ships a flat 1.** `WeaponDurability.WEAR_PER_USE` is a
  constant, and the natural next step is a content `wear:` field on the weapon (default 1) so a
  heavy weapon can cost more per swing than a light one. Not shipped with Pass 2 because it means
  picking a number for five weapons before a single one has been felt in play, and the flat value
  is the honest default until then. The constant is the only thing that has to move.
- ~~**The custom Unbreaking enchant consumes the seam built in Pass 2.**~~ **DONE** (Enchant Pass 1)
  `WeaponDurability`
  `.applyWearOnUse` carries a commented `THE UNBREAKING SEAM` block at exactly the point the roll
  belongs — after the non-Damageable and already-broken exemptions, before the `wear()` — so the
  enchant is *add the roll*, not *reopen the wear sites*. Shape: roughly a `1/(level+1)` chance to
  consume durability, mirroring vanilla's own curve, returning without wearing on a skip.

  This is a CUSTOM enchant, not vanilla's: per the no-vanilla-enchants policy above, a player-held
  item can never carry a vanilla enchant here, so there is nothing to read off the item — it will
  come from wherever the enchant system puts a level, and that system does not exist yet. The seam
  is deliberately built ahead of it because building it later means touching wear again.

  > #### 2026-08-25 — CONSUMED. The seam cost two lines, as predicted.
  >
  > `EnchantItems.activeLevel(held, keys, Unbreaking.ID)` then
  > `if (!Unbreaking.consumes(unbreaking, ThreadLocalRandom.current().nextDouble())) return;`, in
  > place of the comment block and nowhere else. Neither hook was reopened, which is the whole
  > claim the Pass 2 seam was built on, now paid off.
  >
  > The curve is `core/enchant/Unbreaking.java` — `1/(level+1)`, strict `<` against a half-open
  > `[0,1)` draw, clamped at both ends. The clamps are not decoration: a NEGATIVE level makes the
  > unclamped threshold `-1.0`, which no roll is below, so a corrupt blob would produce an
  > INDESTRUCTIBLE weapon; level 99 makes it `0.01`. Both are pinned in `UnbreakingTest`.
  >
  > The sentence above that says *"that system does not exist yet"* is now false. The level comes
  > from `EnchantItems.activeLevel` → `EnchantState.effective()`, off the item's own PDC blob. The
  > seam deliberately does NOT consult the enchant registry — `consumes` clamps for itself, so the
  > hot path stays one PDC read plus a short parse, and a deleted content file leaves the enchant
  > working rather than silently switching it off.
- **Pass 2's once-per-swing dedup is guarded by a core test, not by a boot step, and that is not a
  gap.** The rule is that one swing costs one use however many bodies its payload reaches — vanilla
  charges a sword once for a sweep. **It cannot be witnessed in-game**, because
  `CastExecutor.meleeTarget` resolves the single *nearest* body in the arc: ironblade and emberblade
  damage at most one thing per swing, so no shipped weapon can produce a two-mob swing at all.
  Witnessing it would mean inventing a throwaway multi-target weapon for a hypothetical.

  `CastExecutorTest.oneMeleeSwingChargesOneUseHoweverManyItSplashes` is the witness instead — a
  melee cast with a `Burst` payload over two dummies, asserting one use — and it is a real guard,
  not a green no-op: billing per body in reach reddens it at `expected: <1> but was: <2>` while the
  single-body test stays green, which is the discrimination it claims. Recorded here rather than
  left as a boot step that reads runnable and is not, which is the failure the step-7 gate above
  already paid for once.
- **The refresher's coverage boundary: `getContents()` does not reach the ender chest or the inside
  of a shulker box.** The scan walks a PlayerInventory's 41 slots — storage, hotbar, armour, offhand
  — so a weapon stashed in an ender chest or boxed up refreshes only once it is back in the main
  inventory at a join (or on a `/rpg refresh`). Harmless in practice, and worth being explicit about
  *why*: behaviour is id-driven, so a stashed weapon keeps working the whole time; only its baked
  display is stale, and only until it is carried again. Not built this pass, same standing as the
  on-enable sweep below.
- **The on-enable sweep of already-online players is not built.** It would only cover
  `/reload`-without-disconnect, which is not in the dev loop — `dev-server.sh` restarts the server,
  and a restart reconnects you, which is the join trigger. `/rpg refresh` already covers "refresh
  without relogging" for the case that actually happens. On Folia it would also need a per-player
  scheduler hop rather than a straight loop, so it is not the three-line freebie it looks like.
- **The `_TEMP` *item* fixtures cannot be refreshed by this mechanism.** `health_boost_TEMP`,
  `attack_speed_boost_TEMP` and `class_damage_boost_TEMP` (`HealthModifierItems`,
  `AttackSpeedModifierItems`, `ClassDamageModifierItems`) do carry real instance-PDC — an amount, and
  for the last one a class — but they are minted from a **command argument**, not from a content
  definition. There is nothing to regenerate them *from*, so "re-mint from the current definition"
  has no meaning for them; they would need their own mechanism. They are therefore **not** the
  natural next target for the refresher. (Distinct from `rooted_TEMP` / `soaked_TEMP` above, which
  are YAML status effects carrying no PDC and no item at all.)
- **MAINTENANCE, OUTSTANDING: Paper 26.1.2 → 26.2, gated on PacketEvents supporting 26.2.** The
  Durability Pass 2 boot (2026-08-25) warned *"you are 2 release(s) behind the latest stable release
  (26.2)"* — which is upgrade-step-0 (*"Notice the release. Nothing does this for you"*) actually
  happening, by accident, because a boot printed it. Recorded here so the next person reads it
  rather than re-noticing it. Follow `CLAUDE.md`'s order: check
  <https://modrinth.com/plugin/packetevents/versions> first, bump `packetevents.version` and confirm
  it builds, then `paper.version`, then `./mvnw -pl core test` (a `core` break on a Paper bump means
  `core` has an illegal dependency — that is the real bug), then boot and smoke-test one ability.
  Per D4 there is no bot, by decision, so this line IS the notification.
- ~~**The per-instance enchant ROLL and the class POOLS are deferred; Pass 1 assigns candidates by
  hand.**~~ **DONE** (the enchant rolls pass, PR #20; see `PLAN-enchant-rolls.md` for the record
  and its boot gate). Both shapes it left open are now DECIDED: **slot count is fixed at 3**, and the
  **1--3 rolling lives at the CANDIDATE level inside each slot**. `Keys.enchantRolled` is read at
  last, by `EnchantRollItems.rollOnAcquire`, and the forecast below held exactly -- the carry
  needed no change at all. The original entry is kept verbatim below.

- **The per-instance enchant ROLL and the class POOLS are deferred; Pass 1 assigns candidates by
  hand.** `/rpg enchant candidate <slot> <enchant>` is the stand-in, exactly as `/rpg durability`
  stood in for auto-wear. `Keys.enchantRolled` (BYTE) is already written and already carried across
  a re-mint, and nothing reads it — it is reserved precisely so the roster pass adds the roll
  without reopening the carry. `Rarity`'s own javadoc already anticipates sizing that roll by tier
  (*"the ordering is load-bearing: Phase 4 compares tiers to size an enchant roll"*).

  Two shapes stay genuinely undecided, and the code is arranged so neither is prejudged.
  **Fixed-3 versus rolled-1–3 slots**: `EnchantState` deliberately does NOT cap slot count, and the
  only bound in the tree is `RpgCommand.MAX_DEV_SLOT = 2` — a command-side guard at the reachable
  surface, not a rule in the kernel. **Same-enchant-across-slots and stacking**: see the next entry.
- ~~**The same-enchant-across-slots rule is provisionally MAX, and that is a placeholder, not the
  answer.**~~ **DECIDED: it is MAX, permanently** (the enchant rolls pass). The placeholder was
  kept as the answer, and the three reasons it was chosen are the three reasons it survived. **No
  mutual exclusion was added**: a roll may offer the same enchant in more than one slot, both may
  be active at once, and it resolves to the highest level either holds it at. The `PROVISIONAL`
  javadoc on `EnchantState.effective()` is gone. The original entry is kept verbatim below.

- **The same-enchant-across-slots rule is provisionally MAX, and that is a placeholder, not the
  answer.** `EnchantState.effective()` returns one entry per distinct active id at the HIGHEST level
  any active slot holds it at. Chosen because it cannot exceed `MAX_LEVEL` (so it can never hand a
  player a level no tooltip ever showed, which summing can), because it is order-independent (where
  first-wins would depend on slot order the player cannot see), and because it therefore makes
  duplicating an enchant strictly non-beneficial while the real rule is undecided — the right
  direction to be wrong in.

  The aggregation lives in `effective()` rather than at the seam, so the cap holds against a
  duplicate from ANY source — a hand-edited item, a future roll, an older build's blob — not merely
  one the dev command could produce. `/rpg enchant active` also warns when it activates a copy, but
  the resolver is the actual rule. The roster pass replaces the aggregation, and only it.

  Note for whoever writes that: the test case has to make max and sum disagree BELOW the cap.
  `EnchantStateTest` uses two slots at I each (max 1, sum 2), because an earlier version used I and
  III — where sum is 4, which the clamp folds back to 3, the same answer max gives. A mutation run
  proved that version reddened nothing at all.
- ~~**The enchant TABLE UI is not built; `/rpg enchant` stands in for it.**~~ **DONE** (the Enchant
  UI pass, PR #19; see `PLAN-enchant-table-ui.md` for the pass's record and its boot gate). The
  original entry is kept verbatim below; what follows is what it turned into.

- **The enchant TABLE UI is not built; `/rpg enchant` stands in for it.** Pass 2. This pass's
  relationship to the table is exactly Durability Pass 2's to auto-wear: build the mechanism, drive
  it with a dev instrument, and leave the thing that will really drive it for a pass that can decide
  its own questions. The table needs the XP economy and bookshelf power below, neither of which
  exists.
- ~~**The XP economy and bookshelf power are deferred; unlocking is free in Pass 1.**~~ **DONE** (the
  enchant economy pass; see `PLAN-enchant-economy.md` for the record and its boot gate). The forecast
  held exactly — the cost check went in front of the call, `EnchantClickIntent` needed no change at
  all, and the whole economy is a guard and two lines ahead of the transition plus a deduction behind
  it. **What the entry did NOT forecast, and what the pass turned on, is the UNIT.** The original
  entry is kept verbatim below.

  > #### 2026-08-27 — the price is in XP POINTS, and pricing it in levels was wrong
  >
  > The first design charged `round(BASE_levels × (1 − power/100))` with `BASE = {16, 25, 40}` and
  > discounted the **level count**. XP levels are not a linear currency: reaching III is 40 levels but
  > **2920 points**, and 28 levels is **1186**. So a "30% discount" was really **59.4% off** at III,
  > and a different number at every rung.
  >
  > The whole cost model moved into points. `BASE_LEVELS` survives as the tuning knob because it is
  > the unit a designer thinks in, but it is *derived* through `XpCurve` into `{352, 910, 2920}`
  > before anything is charged. **"16 levels" is an unambiguous amount of money at exactly one point
  > on the curve — a player starting from zero — and nowhere else.** That is also what makes the gate
  > readable: set your level to 16 and the unlock takes you to exactly 0.
  >
  > Nothing structural moved with it. The seam ordering, the ring geometry, the frozen-at-open
  > decision and the pure/impure split were all unaffected.

- **The XP economy and bookshelf power are deferred; unlocking is free in Pass 1.**
  `EnchantState.withLevel` is the seam an economy gates, and it is a pure function on an immutable
  value, so gating it costs nothing structural — the cost check goes in front of the call, not
  inside the model.
- ~~**The damage-modifier enchant type (Sharpness / Power) is deferred.**~~ **DONE** (Enchant Pass 2).
  The original entry is kept verbatim below; what follows is what it turned into.

  > #### 2026-08-25 — SHIPPED, and the schema decision it predicted was the easy half
  >
  > The prediction held exactly: it hooks the Caster projection, shares none of Pass 1's machinery
  > beyond the state model, and needed a typed schema decision rather than an ad-hoc key. Four
  > enchants now ship — `unbreaking` (durability), `sharpness` (melee), `power` (ranger),
  > `attunement` (mage) — and the roster the deferred roll will draw from is real.
  >
  > **THE FORMULA, and it is the whole pass:**
  >
  > ```
  >     base * (1 + pct/100)  +  classDamageBonus
  > ```
  >
  > Percent on the WEAPON'S BASE, flat gear bonus on top. This is a real fork with two candidates
  > that give different numbers, and the number is the only thing that tells them apart: an
  > 8-damage sword with Sharpness III and +5 Melee deals `8*1.15 + 5 = 14.2`, where multiplying the
  > sum gives `(8+5)*1.15 = 14.95`. Both are "the enchant and the bonus applied". Pinned by
  > `EffectApplierTest.theEnchantPercentMultipliesTheWeaponBaseAndTheClassBonusIsAddedAfter`, and a
  > mutation that moved the multiply outside the addition reddened **that test and nothing else in
  > 550** — which is what justifies writing an assertion whose value distinguishes two designs
  > rather than merely confirming a change happened. Same shape as `Fireball 12 -> 17, NOT 24`.
  >
  > **The multiplier is applied AT THE ARM, not pre-baked at projection**, and that is what makes
  > Attunement possible at all: `d.amount()` is not known until the effect fires, so a multiplier
  > folded into the caster's `attackDamage` could never reach `ember_staff`'s authored literal. The
  > same reasoning that sent the class bonus to both arms one pass earlier.
  >
  > **THE STAT CARRIES A PERCENT, NOT A MULTIPLIER, and this is the decision most likely to be
  > "simplified" later.** `Stat.value()` is `base + Sum(modifiers)`. Percentages compose by
  > addition, so summing is correct and the neutral is **0.0** — the same absent-value rule as
  > `attackValue` and `classDamageValue`, rather than a third convention beside `attackSpeedValue`'s
  > 1.0. A multiplier-valued stat would have to base at 1.0, which `Stat` resolves to **2.0** with
  > two sources; and a 0.0 slip on one would not be a small buff like copying attack speed's 1.0
  > would be — it would **zero every untracked combatant's damage**. There is exactly one sensible
  > absent value for a percent, which is the point. `DamageEnchants.multiplier` owns the
  > `1 + pct/100` conversion so the two arms cannot disagree.
  >
  > That the 311 pre-existing core tests stayed green through the arm change IS the regression
  > result, not a formality: the 0.0 neutral is what left every number in the suite alone.
  >
  > **`class: ranger`, not `ranged`.** The design brief said `ranged`; the enum and every weapon yml
  > say `ranger`, and "Ranged" is only `WeaponClassLabel`'s display string. The enchant reuses
  > `WeaponClass.fromName`, so an enchant and the weapon it sits on are parsed by ONE function and
  > cannot disagree. A parallel `EnchantClass` enum was rejected: SUMMONER would need adding in two
  > places the day it lands, and the exhaustive-switch discipline only works with one enum.
  > `EnchantLoaderTest` pins that `ranged` is REFUSED, so the brief's typo cannot come back quietly.
  >
  > **Why `percent_by_level` is data when Unbreaking's curve is Java** — the question the schema
  > decision actually turned on, and it is not numbers-versus-code. Unbreaking is ONE enchant with
  > ONE curve, so its curve IS its mechanism. Sharpness, Power and Attunement are THREE enchants
  > sharing one mechanism, differing only in a class gate and three numbers; a Java class each would
  > be three copies of the same arithmetic and the fourth would be a recompile, which invariant 2
  > forbids. Numbers become data at the point where they stop being the mechanism. `EnchantEffect`
  > names the mechanism; content parameterises it. `unbreaking.yml`'s comment block was **rewritten,
  > not appended to** — it previously said there was deliberately no behaviour field, and leaving
  > that above an `effect:` key would have left the file contradicting itself.
  >
  > **Five schema rules, each throwing so the loader names and skips the file.** `effect` required
  > (never defaulted — a default would silently turn a misspelled damage enchant into an Unbreaking);
  > `class` required, with `universal` spelled out rather than being what you get by forgetting the
  > line; a damage enchant needs a curve; **the curve's length must EQUAL `max_level`**, which is
  > what makes level -> percent total; and a durability enchant may claim neither a class nor a
  > curve, because nothing reads either and a file must not claim a control it does not have.
  >
  > **`DamageEnchantItems` reads MAIN HAND ONLY**, the whole difference from
  > `ClassDamageModifierItems` beside it: a class grant is worn elsewhere and pointed at your weapon,
  > so that one scans every slot; a damage enchant IS on the weapon. It is also **not a `_TEMP`
  > fixture** — the source is real content on a real item, so nothing here owes removal. What is
  > still a stand-in is how an enchant GETS onto the item (`/rpg enchant`), not this read.
  >
  > **It DOES consult the registry, unlike the durability seam, and that asymmetry is a consequence
  > rather than an inconsistency.** `EnchantItems.activeLevel` compares an id and never looks
  > anything up, so deleting `unbreaking.yml` leaves Unbreaking WORKING. A damage enchant keeps its
  > gate and its curve in the definition, so a dangling id has no percent to grant — it renders on
  > the tooltip (EnchantLore's fail-soft) while granting 0. Visible, and the safe direction.
  >
  > `percentAt` **clamps to its own list**, and that guard is not defensive habit:
  > `EnchantState.effective()` clamps to the model's global `MAX_LEVEL` (3), NOT to an individual
  > enchant's authored `max_level`. The loader holds those equal for a file it accepted, but a
  > hand-edited item or a blob written against different content can ask a two-entry curve for level
  > 3 — an `IndexOutOfBounds` thrown from inside a reconcile tick, on a path that must be total.
  >
  > Folia-safety came free again: the percent rides the existing `CombatantSnapshot -> Caster`
  > freeze, so a Power III arrow is a 15% arrow for its whole flight even if the bow leaves the hand
  > before impact. Pinned by `ProjectileFlightTest.aProjectileDealsTheEnchantMultiplierFrozenAt
  > LaunchNotAtImpact`, which is the only possible proof while Paper is single-region.
  >
  > **Verified:** `./mvnw clean package` -> core **320**, storage **17**, paper **213**.
  > `check-jar.sh` -> `Jar OK`. All four ymls confirmed inside the shaded jar at real byte sizes
  > (unbreaking 1908, sharpness 1886, attunement 1076, power 695). **No Java was needed to ship
  > them** — RpgPlugin's jar scan replaced the hardcoded `String[]` in E0, and this is the first
  > time that has been exercised by net-new content files.

- **The damage-modifier enchant type (Sharpness / Power) is deferred.** It hooks the Caster
  projection, not the durability seam, so it shares none of this pass's machinery beyond the state
  model. This is why `content/enchants/*.yml` carries **no behaviour field**: an enchant's effect is
  a mechanism, not a number, and content names an effect without defining one — the same
  relationship ability yml has with `EffectSpec`. Adding `durability_skip: 0.25` to the schema is
  the obvious next temptation and it is a typed schema decision belonging to that pass, not a
  one-liner.
- **`EnchantCodec` is the repo's first string codec, and the carry deliberately never uses it.**
  `WeaponItems.carryEnchants` moves the RAW string across a re-mint; only READERS parse. That is
  what makes the never-wipe-unlocks invariant survive a version skew: a v2 blob read by a v1 build
  renders as unenchanted and is handed back byte for byte, rather than being rewritten into v1 and
  losing whatever v2 added. It is also the only reason `decode`'s unknown-version arm can safely
  return empty — that arm would be data loss if the carry round-tripped through it.

  Breaking the house convention (one typed PDC key per scalar, assembled in paper) is justified by
  genuinely variable arity: slots × candidates, both of them counts the roster pass has not chosen.
  A per-scalar scheme means either a fixed grid written whether used or not, or a key count that
  changes with the data, and neither versions atomically.

  Because it is a new pattern the test bar is higher: `EnchantCodecTest` pins the LITERAL wire form,
  not merely a round trip. A round-trip test stays green when encode and decode break together —
  the accident CLAUDE.md names as failure #4 — and a mutation run confirmed the discrimination is
  real: changing the candidate separator reddens the exact-grammar test while `aFullStateRoundTrips`
  stays green.
- **`applyWearOnUse` now parses a short string on every basic attack.** One PDC read plus a
  `split`, per connecting swing. Bounded by the fast-reject at the top of `EnchantItems.activeLevel`
  — no meta, or no `enchant_data` key, returns 0 having allocated nothing, which is the
  overwhelmingly common case. Not optimised because it is not measurable next to the two
  `getItemMeta()` calls already in that method. If it ever is, the cheap fixes in order: a separate
  INTEGER `unbreaking_level` key written alongside the blob, or caching the decode per stack.
- **`/rpg enchant` re-mints the whole item on every edit, and that is deliberate.** It writes the
  state then calls `WeaponItems.remint`, rather than patching lore. The cost is an item replacement
  per command; the payoff is that there is exactly ONE lore path, so the enchant block cannot be
  doubled or left stale by any edit route. It also means every use of the command exercises the
  carry-forward, so the invariant this pass exists to protect is hammered continuously rather than
  checked once at login.
- **A dangling enchant id renders but is not warned about.** `EnchantLore` falls back to the
  title-cased id (matching `WeaponLore.elementLine`'s fail-soft for an unknown element) rather than
  hiding the line. Deliberate, and it follows from the seam rather than from taste: the seam
  compares ids and never consults the registry, so deleting `unbreaking.yml` leaves the enchant
  WORKING — and an enchant that silently skips durability while showing nothing is a far worse bug
  than one with an ugly name. `EnchantLore` is pure and cannot reach `warnOnce`;
  `RefreshVerdict.Dangling`'s warn-once route is where a warning would go if one is ever wanted.

  > #### 2026-08-25 — Pass 2 makes this HALF true, and the half that changed is the important one
  >
  > It still holds for a DURABILITY enchant, for exactly the reason above. It does **not** hold for a
  > damage enchant: `DamageEnchantItems` must consult the registry, because a damage enchant's class
  > gate and curve live in its definition and there is nothing to apply without one. So deleting
  > `sharpness.yml` leaves the tooltip rendering "Sharpness III" while the sword deals base damage.
  >
  > That is the *opposite* trade from Unbreaking's, and it is still the safe direction — a dangling
  > damage enchant grants nothing, where a dangling Unbreaking keeps working. The asymmetry is a
  > consequence of where each mechanism keeps its numbers, not an inconsistency to iron out. Worth
  > knowing before someone "fixes" one to match the other.

- **Enchant Pass 1's boot record, landed late.** `f7845b6` ended "Not yet boot-witnessed" and the
  gate was then run on 2026-08-25 (12:12–12:41) without being written up; `run/boot_enchant1.log`
  held the evidence on disk through the merge of PR #17. Recorded here from that log rather than
  from memory, and **scoped to what a console log can actually prove** — the plugin's replies to
  `/rpg enchant` go to the player, not the console, so the per-command responses were seen by a
  human at the time and are not recoverable from this file.

  What the log proves outright:

  ```
  :15   WARNING: Skipping malformed enchant 'overpowered.yml':
                 enchant 'overpowered' max_level must be 1..3, was 9
  :17   WARNING: 1 enchant file(s) were skipped. The server is still running, ...
  :117  [Rpg] Loaded 6 abilities, 7 visuals, 5 statuses, 7 elements, 1 enchants, 2 kits, 5 weapons, 1 mobs
  :130  Done (5.294s)!
  ```

  So the **fail-soft path was witnessed live**, not merely unit-tested: a deliberately malformed
  `overpowered.yml` (`max_level: 9`) was dropped into the deployed content folder, named and skipped
  at boot, and the server started anyway with the rest of the roster intact. Same throwaway-fixture
  technique as the dangling-weapon gate's `testdangle.yml`, and it was cleaned up afterwards —
  `run/plugins/Rpg/content/enchants/` held only `unbreaking.yml` afterwards.

  The command sequence walked candidate -> level -> active, the wear path
  (`/rpg durability damage 254` x3, `repair 250` x3), `clear`, two unknown ids (`owahjwa`,
  `nosuchenchant`), an out-of-range candidate (`active 0 3`), an out-of-range level (`level 0 0 34`,
  refused by Brigadier before the handler), and the same flow on `ember_staff`. At 12:40:51 the
  player pasted the raw blob into chat — **`v1;unbreaking=3:0`** — which is the carry evidence the
  `show` command exists to produce, in the literal wire form `EnchantCodecTest` pins.

- **Enchant Pass 2's boot gate: step 1 is DONE, steps 2–10 are OWED BY A HUMAN.** Recording the
  split explicitly, because "the boot gate ran" and "the boot gate ran as far as a console can go"
  look identical in a summary.

  **Step 1, run and passed** (`./scripts/dev-server.sh --refresh-content`, 2026-08-25 21:42):

  ```
  [Rpg] Loaded 6 abilities, 7 visuals, 5 statuses, 7 elements, 4 enchants, 2 kits, 5 weapons, 1 mobs
  Done (8.095s)!
  ```

  `4 enchants`, up from 1, with **no** `Skipping malformed enchant` and no skip-count warning in the
  server phase — so all four shipped files parse under the new required schema on a real boot, not
  just in surefire. (The loader warnings that DO appear earlier in that log are surefire's, from
  `EnchantLoaderTest`'s own malformed fixtures during the build. Worth noting so they are not misread
  as boot failures.) The deployed jar was confirmed **byte-identical to the build output** with
  `cmp`, and `--refresh-content` re-copied all four ymls at the same byte sizes they carry inside the
  jar. The server was stopped afterwards — a live server holds the jar lock, which is the incident
  CLAUDE.md's verification section opens with.

  **Everything that reads a NUMBER is still owed**, because `/rpg give` and `/rpg enchant` both gate
  on `instanceof Player` and the damage popup is a per-viewer packet. The same shape as the grenade
  cast owed after the rename, and `/rpg class` after Commit F. The gate, with the numbers derived
  from content in advance rather than read off and rationalised after:

  | # | held | enchant | expect | proves |
  |---|---|---|---|---|
  | 2 | `ironblade` | Sharpness III | tooltip "Sharpness III"; `show` prints `+15% damage, x1.15` | the read, before any swing |
  | 3 | `ironblade` | Sharpness III | swing **9** (plain is **8**) | the ARM applies the multiplier |
  | 4 | `ember_staff` | Attunement I then III | bolt **17** then **18** (plain **16**) | the LEVEL reaches the curve |
  | 5 | `hunters_bow` | Sharpness III | shot **6**, unchanged | the class gate; `show` says "inert" |
  | 5b | `hunters_bow` | Power III | shot **7** | and the right enchant does land |
  | 6 | `ironblade` + `+5 Melee` offhand | Sharpness III | **14**, *not 15* | percent on base, flat on top |
  | 7 | `ironblade` | Sharpness I *and* III, two slots | **9**, *not 10* | `effective()` takes MAX, not sum |
  | 8 | `ironblade` | Unbreaking + Sharpness | wear skips AND damage scales | dispatch by `effect` |
  | 9 | any | any | `show`'s raw blob identical across a re-mint | Pass 1's carry, still held |
  | 10 | `ironblade` | Unbreaking III, then Sharpness III | the `/rpg enchant active` REPLY reads `(consumes durability on 25% of uses)`, then `(+15% damage, x1.15)` | the ACTIVATION reply dispatches by `effect` -- pre-fix it said "consumes durability" for BOTH |
  | 10b | `hunters_bow` | Sharpness III | the `active` REPLY reads `(inert: a Melee enchant on a Ranged weapon)` | the inert case reaches the reply, not only `show` |

  **Step 10 exists because step 2 checked `show` and nothing checked the ACTIVATION reply.** That
  gap is the whole reason `/rpg enchant active` spent Pass 2 appending Unbreaking's consume rate to
  every enchant -- activating Sharpness reported "consumes durability on 25% of uses" -- while step
  2 sat one command away reading the correct percent out of `show`. A gate that reads one of two
  surfaces looks exactly like a gate that reads both.

  Its shape is the point: one held weapon, two activations, **two different asserted strings from
  the same command**. A single hardcoded description cannot pass it whichever effect it happens to
  describe, which is the property step 8 has for the swing path and the reply had for nothing.
  `EnchantEffectLineTest` now pins both strings in the two-second loop, so step 10 witnesses the
  wiring rather than the arithmetic.

  **Steps 4, 6 and 7 are the ones that carry information**, and each is a number that separates two
  designs rather than confirming a change:
  - **4 uses the staff and not the sword deliberately.** The popup rounds
    (`DamageNumberText.of` -> `Math.round`), and at `[5, 10, 15]` the ironblade renders I/II/III as
    **8 / 9 / 9** — Sharpness I is *indistinguishable from unenchanted*, and II from III. On the
    staff's base 16 they are 17 / 18 / 18, so I and III separate from each other and from 16. The
    curve was NOT steepened to make a popup readable; it is proven exactly, at full precision, in
    `DamageEnchantsTest`, and the boot only witnesses the wiring.
  - **6 is 14, not 15.** `8*1.15+5 = 14.2` versus `(8+5)*1.15 = 14.95`. If it ever reads 15 the
    multiply has moved outside the addition.
  - **7 is 9, not 10.** MAX gives 15% (9.2); summing would give 20% (9.6 -> 10). This is the case
    NEXT.md's stacking entry asks for — one where max and sum actually disagree.

- **The `DamageNumberText` double-rounding tolerance is now LIVE, and its own javadoc predicted
  this pass.** It says the skew is *"latent; revisit (round both off one basis) only if element
  multipliers make fractional damage visible."* A damage enchant is what makes damage fractional for
  the first time: the popup rounds `amount`, while the mob nameplate rounds current and max
  independently, so a visible plate drop can differ from the popup by ±1 on any enchanted hit.

  **Consequence for the gate: trust the popup, not the plate delta.** They are two readings of one
  `applyDamage` event and PLAN-1b asserts they agree — that assertion is now approximate for
  fractional damage, and a ±1 disagreement is the documented skew rather than a new bug. Fixing it
  means rounding both off one basis; not folded into this pass.

- **CONSEQUENCE: a melee weapon's ABILITY takes its melee enchant. ANSWERED 2026-08-25: KEEP IT, and
  the reason is that it matches the class bonus.** The emberblade's Fireball is a literal
  `Damage(12)` and the multiplier reaches both arms, so Sharpness III scales it to 13.8. Any ability
  cast while holding a matching-class enchanted weapon is scaled the same way — `/rpg cast
  solar_grenade` with a Sharpness sword in hand included.

  This is exactly consistent with the standing consequence recorded for the class bonus (*"standalone
  ability literals are now gear-scalable"*), and it follows from the same held-weapon-gated `Caster`.
  The alternative — restricting an enchant to the weapon's own triggers — would need `WeaponService`
  to mark the cast and a from-weapon flag threaded through `AbilityService`/`CastExecutor`, the same
  cost that was declined for the class bonus.

  **The decision was to keep the shipped behaviour, taken deliberately and before the roll rather
  than discovered after it.** What it settles is not really the Fireball; it is that
  **`+Class Damage` gear and a class-typed enchant are ONE rule, not two.** Both are build stats
  gated on the held weapon's class, both reach every direct-damage effect the caster deals, and
  neither is keyed on where the payload came from. A player who has learned what their sword does to
  their gear does not then have to learn a second, narrower rule for what it does to their enchants.

  So the pair of them now carries a single invariant worth stating once: **anything gated on the held
  weapon's class scales the caster's whole class output, not just that weapon's autoattack.** If that
  is ever revisited it should be revisited for BOTH — splitting them so gear scales abilities and
  enchants do not would be the genuinely confusing outcome, and it is the one this answer forecloses.

  Consequence for the roll, which is the pass that would otherwise have inherited the question: it
  inherits nothing. A rolled Sharpness behaves like a hand-assigned one, and the roll's design does
  not need to know that abilities exist.

- **Still deferred after Pass 2:** ~~the per-instance roll and the class pools~~ (DONE — the
  enchant rolls pass, which drew from exactly that roster of three), ~~the enchant table UI~~
  (DONE — the Enchant UI pass), ~~the XP economy and bookshelf power~~ (DONE — the enchant economy
  pass), and a SUMMONER enchant — which still waits on the class, which still waits on mob-to-mob
  damage. **That is the last of the enchant deferrals except the SUMMONER one**, which is blocked on
  something outside enchanting entirely.

- **The tooltip shows no percent, and the Phase-4 entry stays OPEN.** Pass 2 grants the first real
  enchant STAT DELTA, which is what that entry was waiting for, but `EnchantLore` still renders only
  "Sharpness III" — no `+15%` line, and the weapon's damage numbers are still its authored base.
  Surfacing a RESOLVED number belongs with the stat screen, beside `+N Melee Damage` and the resolved
  attack speed, per the standing rule that lore describes the weapon and not whoever holds it.


- **The enchant ROLLS boot gate: RUN AND PASSED, all 17 rows** (live server, 2026-08-26). The gate
  is in `PLAN-enchant-rolls.md` (PR #20). Recorded here because the pass ships one invariant -- a
  weapon rolls ONCE, ever -- that **no unit test can reach**: what would break it is a hook in the
  wrong place rather than wrong arithmetic, so the 606-test build said nothing at all about it and
  these rows are the only evidence that exists.

  This is the runner's witnessed result, **not a pasted transcript** -- no console output was
  captured into this file. Said plainly rather than dressed up as a log, because Pass 2's record
  above quotes real boot lines and the difference between the two should be visible at a glance.

  The four results worth keeping:

  - **Rows 9, 10 and 10b -- the whole point -- held.** No re-roll across `/rpg refresh`, across a
    disconnect and rejoin, or across a full `stop` and reboot. The candidates and the unlocked
    level were identical each time. Those are three different paths to the same item: a re-mint,
    the join scan, and a real save/load, and only the last one proves the flag survives
    serialisation rather than merely surviving `carryEnchants`.
  - **The class gate held on all three classes.** Melee offered only Sharpness/Unbreaking, ranger
    only Power/Unbreaking, mage only Attunement/Unbreaking. Never a cross-class candidate.
  - **Candidate counts varied within the pool-of-2 cap** -- across slots and across items, never 0
    and never 3. That is the row that needed writing down as it went: a roll that always returned 1
    looks entirely reasonable in any single screenshot, and the *after* state cannot be asked
    afterwards whether it varied.
  - **A malformed enchant fail-softs out of the roll pool.** Worth noting that this was **beyond
    the 17 rows** -- the gate only asked (row 1) that all four shipped files parse. It holds one
    level above the roll and by construction: `EnchantLoader.loadAll` catches per file and skips
    before `registry.register`, so a malformed enchant never enters `EnchantRegistry`, and
    `EnchantRollItems.roster()` reads `enchants().all()`. The pool cannot see what the registry
    never got. Nothing in `EnchantRoll` needs an arm for it.

  **The trap stays recorded even though the gate is discharged**, because it is what a future pass
  would otherwise re-discover the expensive way: `remint` calls `mint`, so a roll in `mint` fires on
  every join, refresh and table click -- **and guarding it on `EnchantItems.isRolled` does not
  help**, because `mint` builds a fresh meta and the carry restores `enchant_rolled` only
  afterwards, so the flag reads false in there for every item. The guard would look present and do
  nothing. The rule is about the CALL SITE: the roll is never called from inside `WeaponItems`.

  Row 16 was the one most easily skipped, and it guards a bug that hides in shipped content: the kit
  grant rolls INSIDE its loop, so a line placed after the loop would leave every kit weapon but the
  last un-rolled -- invisible today, because both shipped kits grant one weapon. It passed, so that
  bug is not present; it is not proof against the same bug being reintroduced by a future kit.

- **`Rarity`'s reserved meaning is now the CANDIDATE axis, not slot count.** The rolls pass fixed
  every weapon at 3 slots, which contradicted the half of `Rarity`'s javadoc promising "enchant
  slots ... per tier", so that half was dropped rather than left promising what shipped code
  contradicts. What is KEPT and still reserved: how many candidates a slot offers, and how rich a
  pool it draws from. The ordinal-ordering rationale survives intact on that axis. Tiering the SLOT
  count again would be a new decision and would need a layout change to go with it.

- **The enchant table's boot gate is OWED IN FULL BY A HUMAN.** Every one of its 23 rows needs a
  `Player` — the menu, the clicks, the item-safety cases and the shutdown return — and a console log
  can only prove the plugin loaded. Nothing in the pass was witnessed in-game. The gate is in
  `PLAN-enchant-table-ui.md`; rows 6–10c and 18–22 need the item COUNTED before and after, because a
  dupe that creates a second weapon and a theft that removes one look identical in a screenshot of
  the after state.

- ~~**The bookshelf readout is a labelled placeholder, not a `0%` count.**~~ **DONE** (the enchant
  economy pass). Slot 8 counts, and **the deviation was resolved by adding a SCALE rather than by
  giving in on the zero.** It reads "Bookshelf Power N/30", so a bare table showing `0/30` is legible
  as a measurement against a known maximum, where the `0%` the layout brief asked for was not
  distinguishable from an unimplemented readout. That is the general shape of the fix and worth
  reusing: *a placeholder becomes a readout by gaining a maximum, not by gaining a number.*
  `MenuIcons.placeholder` is kept, now with no consumer, because the anvil, class-select and stat
  screens will each want it. The original entry is kept verbatim below.

- **The bookshelf readout is a labelled placeholder, not a `0%` count.** Nothing counts bookshelves,
  and slot 8 says "Not implemented yet" in as many words rather than rendering a zero. A readout
  reporting `0%` when nothing is measured is indistinguishable from a working readout that measured
  zero — CLAUDE.md's own failure mode, in a place a player can see. This is a deliberate deviation
  from the layout brief, which asked for a greyed `0%`.

- ~~**Unlocking is FREE, and the XP economy gates the same click.**~~ **DONE** (the enchant economy
  pass). It landed exactly as forecast and `EnchantClickIntent` was not touched. What the pass added
  beyond the forecast is a THIRD question beside it: `EnchantCharge` answers *what the click buys*,
  after the intent has answered *what it means* and before the wallet answers *whether you can afford
  it*. It is Bukkit-free for the same reason `EnchantClickIntent` is — `EnchantMenu` cannot be built
  in a unit test — and its switch has no default arm, so a seventh intent is a compile error until
  someone prices it. The original entry is kept verbatim below.

- **Unlocking is FREE, and the XP economy gates the same click.** `EnchantClickIntent.of` is a pure
  function and the cost check goes in FRONT of it, never inside: what a click MEANS and whether you
  can afford it are different questions. So the economy pass adds a guard and a cost to
  `applyCandidateClick` and changes nothing structural, and the bookshelf discount lands on the
  readout that is already sitting there saying it does not exist yet.

- **The enchant ECONOMY boot gate: RUN AND PASSED, 26 rows witnessed and 2 structurally guaranteed**
  (live server, 2026-08-27). The gate is in `PLAN-enchant-economy.md` (PR #21). Recorded here because
  the pass ships two things **no unit test can reach** — the seam ordering, where moving the deduction
  above the transition leaves all 643 tests green, and the world read in `BookshelfPower.at`, which is
  referenced by no test at all. The build said nothing whatever about either, so these rows are the
  only evidence that exists.

  This is the runner's witnessed result, **not a pasted transcript** — no console output was captured
  into this file.

  **Rows 1–20 and 23–28 were witnessed live and passed.** The four results worth keeping:

  - **Row 13 is the row this pass was rewritten for, and it held.** A full ring charges **2044** for
    III, which is exactly 70% of 2920. The discarded level model would have charged
    `totalForLevel(28)` = **1186** — 59.4% off wearing a 30% label. Pricing in points is the only
    reason the number on the bookshelf readout means what it says, and this row is where that stops
    being an argument and becomes an observation.
  - **The derivation is visible in game.** Rows 4–6: bank exactly level 16 / 25 / 40 and the unlock,
    the II and the III each land the player on exactly **level 0 with an empty bar**. That is what
    makes "352 points" and "a level-16 bank" the same sentence rather than two claims.
  - **The seam ordering held from both sides.** Row 7 refused at 315 against a 352 price with the
    weapon untouched, and row 24 confirmed the blob was unchanged after a refused click — so an
    unaffordable click really is a click that did not happen. Row 10, one point short at 351, is what
    proves the wallet is read as POINTS and not as a level count.
  - **The discount floors, in front of a player.** Row 16: one shelf, and II reads **900**, not 901.
    Rounding to nearest would have taken a point off the player on that exact click.

  **Rows 21 and 22 were NOT witnessed, and are not currently witnessable single-player** — you cannot
  place a block while a GUI is open, so no one player can build a ring without first closing the menu
  that the row is about. They are discharged as **structurally guaranteed rather than observed**, and
  the distinction is kept because a guarantee argued from code is not the same kind of evidence as a
  row someone watched. Verified by reading the code rather than by asserting it:

  - `bookshelfPower` is a `private final int` assigned **exactly once**, in the constructor, from the
    only call to `BookshelfPower.at` in the codebase. Every other mention of it is a read. The
    compiler enforces the single assignment, so row 21 (frozen at open) cannot fail without a
    compile error.
  - **No `Block` field exists** — `Block` appears in `EnchantMenu` only as a constructor parameter.
    There is nothing for a re-scan to be written against, which is the specific hazard freezing was
    chosen to foreclose.
  - `RpgListeners:185` constructs a **new** `EnchantMenu` on every right-click, so row 22
    (re-count on reopen) is the constructor running again.

  What that argument does NOT cover is whether the scan reads the world correctly — but rows 13 and
  18–20 witnessed exactly that and passed, so the only unobserved surface left is the freeze itself,
  which is the part the `final` field settles. **Both rows stay witnessable with a second player**
  (one places shelves while the other holds the menu open) and are worth running if one is ever to
  hand; they are not being written off as unreachable for ever.

- **Three wrong predictions about floating point, all made before running anything.** Recorded
  together because the pattern matters more than any one of them. (1) A one-off in `XpCurve`'s
  *cumulative* band boundaries reddens nothing — vanilla's parabolas intersect at consecutive integer
  pairs, so a branch anywhere in {14,15,16} or {29,30,31} is the identical function; only a shift of
  two is observable. The *bar* bands intersect at a single point each and a one-off there reddens
  five tests. (2) Rewriting a curve band with `2.5`/`40.5` doubles reddens nothing either; both are
  exact binary fractions at these magnitudes. (3) The double discount was predicted to break at II
  and does not — `910 × 0.7` rounds back up to 637 while `2920 × 0.7` does not, so it is wrong in
  **exactly one of nine** price cells. **The reviewer caught (3) before it was run.**

  All three are kept next to what actually executed rather than quietly replaced, and (3) is why
  `EnchantCostTest` asserts the whole 3×4 price grid instead of a sample: `(int)` truncation breaks
  only at power 30, `Math.round` breaks only away from it, and asserting one power leaves a
  reimplementation green.

- **This table is now the plugin's only XP sink, and `setKeepLevel` means the wallet only grows.**
  Fine now, and worth knowing later: a long-lived player eventually stops feeling the price.
  `BASE_LEVELS = {16, 25, 40}` — 4182 points to take one enchant to III — is a proposal that has
  never been played, and it is a Java constant so it moves in one place. Note that changing it now
  changes the price NON-linearly, which is the thing this pass got wrong once already.

- **`XpCurve` duplicates vanilla and will drift if Mojang ever changes the curve.** The anchors in
  `XpCurveTest` (level 16 = 352, 25 = 910, 40 = 2920) are the tripwire; nothing checks them against
  the running server. It lives in its own `core.xp` package rather than in `core.enchant` because it
  is a fact about Minecraft, not a rule about enchanting — the table is only its first caller.

- **No air-gap rule on bookshelves, deliberately.** Vanilla wants the block between table and shelf
  transparent; this does not, so a shelf walled in behind stone counts. It halves the reads, drops a
  rule players already find opaque, and makes a full ring something a boot gate can actually build.
  The escape hatch is one occlusion check per `BookshelfRing.Offset`, which is itself the argument for
  offsets being a first-class thing rather than a nested loop inside a block scan.

- **Bookshelf power is FROZEN at open, and only the `int` is kept — never the `Block`.** "Place
  shelves, then reopen" is the simpler interaction, and freezing buys the Folia-correct thing for
  free: the scan runs inside `PlayerInteractEvent` for the very block clicked, on the thread that owns
  it, where a re-read from `InventoryClickEvent` would not be. Keeping the `Block` would invite
  exactly that re-read, which is why it is not kept.

- **`/rpg enchant` stays FREE, and there is no creative exemption.** The economy gates the table, not
  the dev instrument: a priced command would put a wallet in the setup line of every future boot gate.
  Creative is charged because a creative player can `/xp` freely anyway, so an exemption buys nothing
  and costs an untested branch — and the gate runner is almost certainly in creative, so it would
  guarantee the gate never witnessed a charge.

- **The input model is "an empty slot ← exactly one item", and the ROUTER owns it, not the menu.**
  Two things a future input-slot menu will otherwise rediscover the hard way.
  `InventoryClickEvent` fires BEFORE the place applies, so a handler reading the input slot sees it
  empty — acceptance has to be decided against the CURSOR, in `Menu.acceptsInput`. And vanilla
  MERGES a place onto a matching stack rather than swapping: two freshly minted weapons of ours
  share identical meta, so a cursor of one item passes every validity check the menu could make and
  the slot still ends up holding two. Occupancy is therefore checked in `MenuRouting` and validity
  in the menu, both before the place. Never accept-then-eject — that has a window in which the menu
  holds an item it has already decided it does not want.

- **A weapon can never stack, and that is fixed at the SOURCE — with two guards kept behind it.**
  `WeaponItems.mint` sets `meta.setMaxStackSize(1)`, so no shipped path produces a stack at all.
  It is fixed in `mint` rather than only at the enchant table because durability, enchants and
  instance data are ALL per-item, and the table is not the only thing that will ever read them —
  two shipped weapons mint on stackable materials (`ember_staff` is a `blaze_rod`, `ability_stone`
  an `amethyst_shard`) and two fresh mints are byte-identical, so `/rpg give ember_staff` twice
  used to produce a stack of two. `remint` calls `mint`, so it inherits the cap.

  **Both `getAmount() != 1` guards stay**, and they are not now redundant: a hand-edited item, or
  one minted before this change, can still arrive stacked. `acceptsInput` stops it at the door, and
  `applyCandidateClick` re-asserts it because THAT is the operation that mints — `editMeta` would
  enchant every item in the stack and `remint` returns a fresh stack of amount 1, silently
  collapsing it. The operation that can destroy an item guards itself rather than trusting a seam
  upstream, including a seam as solid as a stack-size component.

  Not yet witnessed in-game, in either direction.

- **A weapon carrying more than 3×3 is REFUSED at the door, not truncated.** The extra slots survive
  every transition and keep working, so rendering the first nine would leave an enchant that is
  ACTIVE and INVISIBLE — the same defect `EnchantLore` refuses to create when it renders an unknown
  id rather than hiding it. A roll that can exceed three needs either a bigger layout or a real cap,
  and that is the roster pass's decision, which is why the bound sits in `EnchantMenuLayout` at the
  reachable surface rather than in `EnchantState`.

- **An enchanting table's right-click is cancelled UNCONDITIONALLY; sneaking only chooses what
  happens instead.** The custom table replaces vanilla enchanting, so the vanilla screen must never
  open — not with an empty hand, not while sneaking, not ever. Sneaking then falls through to the
  weapon's `right_click` trigger, which is the escape hatch that keeps a Mage able to cast while
  standing at a table.

  **This was a real bug and not a hypothetical.** The cancel originally sat INSIDE the `!isSneaking`
  guard, so a sneak-right-click skipped the block entirely, nothing cancelled the event, and vanilla
  enchanting opened — the one screen the whole pass exists to replace. The guard was resting on a
  rule that does not exist: sneaking suppresses a container GUI only when you are holding a
  PLACEABLE item, and with an empty hand it does nothing at all.

  **Accepted consequence:** a block can no longer be placed against an enchanting table. Both that
  and vanilla enchanting are things the custom table takes over, and anyone who wants to build
  against one can break and re-place it. Boot rows 3, 4, 4b and 4c judge all four paths.

- **The single-button interaction model is a PROPOSAL.** Locked → unlock-and-activate, unlocked →
  activate, active → level up, one click each. It is a pure function precisely so tuning it means
  editing `EnchantClickIntent` and its tests rather than re-reading a Bukkit class. Boot rows 12–15b
  are where it is judged.

- **All three cross-inventory gestures are PERFORMED by the router, never permitted.** Shift-click,
  the number keys and F are supported, and none is ever un-cancelled. The reason they were refused
  outright at first still stands and is exactly why: vanilla picks the destination itself — across
  the whole other inventory for a shift-click, and as a two-way SWAP for a number key or F — so
  permitting any of them would be accepting a slot chosen by someone other than us, bypassing every
  check. `MenuRouting`'s `shiftMove`, `hotbarMove` and `offhandMove` choose the destination
  themselves and move the item by hand.

  **`NUMBER_KEY` and `SWAP_OFFHAND` are intercepted by TYPE, ahead of even `ALWAYS_REFUSED`**, and
  that ordering is the point: a number-key press over an EMPTY slot does not resolve to
  `HOTBAR_SWAP`, so matching on the action alone would miss exactly the case that matters.
  `HOTBAR_SWAP` and `HOTBAR_MOVE_AND_READD` were therefore removed from the refused set — only a
  number key produces them, and listing them after the type intercept would be dead weight that
  reads like a guard. `DOUBLE_CLICK` and `CREATIVE` stay blanket-refused.

  **Nothing may ever be added to `OWN_INVENTORY_ACTIONS`**, which would hand the destination
  straight back to the server. FOUR inbound paths — click-place, shift-click, number-key, F — share
  ONE `placeAllowed`, and the number key and F share one `swapWithInput` on top of that, so the
  "empty slot ← exactly one item" model cannot hold on one gesture and not another.

  **The number key and F are bidirectional, and one rule gives both directions.** Exactly one side
  must hold something: an empty input slot and a full other slot moves IN, a full input slot and an
  empty other slot moves OUT, and the two remaining cases are refused — both full is the two-way
  swap vanilla would do, and both empty is nothing to move. `placeAllowed` is consulted only on the
  way IN; it asks what may come in, and the only rule going the other way is a destination that is
  already known to be empty.

  The fall-through is load-bearing rather than incidental: because the whitelist means a move the
  router does not perform simply does not happen, a filler pane cannot leak into an inventory, a
  hotbar or the offhand even though all three gestures now reach that code. A shift-click INSIDE
  the player's own inventory is cancelled too — a small loss of convenience for the guarantee that
  the router moves everything or nothing.

- **The menu framework has ONE consumer, and nothing in it is generalised in advance.**
  `Menu.inputSlots()`, `Menu.acceptsInput` and `MenuRouting`'s whitelist are the seams a second menu
  uses. The anvil UI is the reuse test — if it needs changes to the base, the base was wrong. Its
  `MenuClick` deliberately carries no `InventoryClickEvent`: a consumer that needs the event is a
  consumer about to introduce a duplication bug.

- **One non-guarantee, written down so nobody spends a pass on it.** A player who logs out while a
  menu holds their weapon, in a world that then fails to save, loses it. That is true of every item
  in the game and is not this feature's problem.

- **`setEnchantmentGlintOverride` is the sanctioned glow, and the old shortcut is still forbidden.**
  The active candidate glints via the display-only flag, which adds no `Enchantment` instance, on an
  item that is never player-held. `addEnchant(Enchantment.UNBREAKING, 1, true)` + `HIDE_ENCHANTS`
  remains the thing the vanilla-enchant policy forbids, and is now MORE tempting than before, because
  there is finally a glinting item in the codebase to copy the idea from. It is not what that item
  does.

---

## Rules for this work

### A COUNT OBTAINED BY LOOKING IS A LOWER BOUND

**Until a mechanical sweep with a positive control has run, report it as "at least N, unswept."**

This sits above the individual findings below because it is what all of them have in common, and it
is why each was believed downstream. **Three instances in one session, identical structure**, each a
hand count reported as a total:

| reported | actual | how the number was obtained |
|---|---|---|
| five arrow references | **seven** | a `grep` shaped by the identifier (`ARROW_SLOT\|ARROW\b`), not by the word. `grep -i arrow` found 60 lines |
| two places carrying the over-broad Q10 claim | **five** | a hand search of the places it seemed likely to be |
| two orphaned javadocs | **five** | the files that slice happened to have open |

**None of the three was a careless count.** Each was an honest tally of what the searcher could see,
and in each the search itself was the limit — the identifier was not the word, the likely spots were
not all the spots, the open files were not the repo. *"Two" was never a count of the defect; it was
the reach of where I happened to be looking.*

**The tell is that a hand count and a total are reported in the same words.** "There are two" and
"I found two" are indistinguishable in a report, and only one of them is a claim about the codebase.
So say which:

- **"at least N, unswept"** — a hand count. Correct, bounded, and honest about the bound.
- **"N"** — only after a sweep that could have found more, *and* a positive control proving it can
  see. A discovery that finds nothing looks exactly like a discovery that cannot look
  (CLAUDE.md:104), so the control is not optional decoration — without it the sweep's number is
  another hand count wearing a script.

**Corollary, and it is the sharp end:** a defect that is invisible to reading is *also invisible to
incidental discovery*. Orphaned javadocs were found while moving code, not while looking for them —
so the count that came out of that encounter had no relationship to the population. **The moment you
notice a defect class you did not know existed, the first number you have is the weakest one you will
ever have.** Sweep before reporting it.

### A NORMALISATION INSIDE A COMPARISON IS A PREMISE, NOT A FORMATTING STEP

**Every normalised diff claims that what it erased does not matter, and that claim needs its evidence
attached where the result is stated.**

Slice 6's move proved five method bodies `IDENTICAL` — after `sed` had rewritten `isEmpty(` to
`MenuSafety.isEmpty(` and `matches(` to `CraftingMenu.matches(` on one side. The raw bodies differ in
exactly five places. Reporting the post-normalisation verdict as "identical" asserted something
stronger than had been tested.

**The accurate form is: "identical apart from five call-site requalifications, each to a target
separately proved byte-identical."** And the reason the distinction is not pedantry: the
normalisation is sound ONLY BECAUSE the targets were separately proved, so **a report that omits the
target proof is not a weaker version of the argument — it is a different and invalid one.**

**It is also exactly where a defect of this shape hides.** A `MenuSafety.isEmpty` differing from the
predicate it replaced by a single character would change four call sites at once, silently, and every
normalised diff would still print `IDENTICAL`.

**Every `sed`-normalised comparison in this repo has this structure whether or not anyone wrote the
premise down.** When you erase something to make two things compare equal, state what you erased and
why it was safe to erase, next to the verdict — not in a script nobody reads.

### DO NOT POINT A LINE EDITOR AT A FILE THAT CONTAINS ANOTHER LANGUAGE'S SYNTAX

`sed -i` on `NEXT.md` — which carries `awk`, `sed`, shell and Java as *content* — mangled it twice in
one session, once because an inserted line beginning `awk` was parsed as sed's `a` (append) command.
That is a **category error, not a slip**: the fast tool is fast because it interprets, and this file
is the worst possible target for interpretation. Use the editing tool for prose files; keep `sed` for
files whose content is not itself a program. (The recovery held because the file had been copied to
the scratchpad first, and because the restore was *verified* byte-identical rather than assumed.)

### ENUMERATE THE AXIS, NOT THE CASES YOU CURRENTLY HAVE

**Armor Slice 2a hit this three times in one slice, in three different disguises. It is one defect.**

Every instance had the same shape: a rule written against *the values that existed when it was
written*, correct on the day, and silently wrong the first time the axis grew. None of the three was
caught by a compiler and none by a test. They were found by reading, by a test written afterwards,
and by a test written afterwards.

**The teaching example, because it shipped and slipped everything:**

```java
"inert: a " + GearClassLabel.of(gearClass) + " enchant"
```

Correct for Melee, Ranged, Magic and Shield -- every label that existed. `GearClass.ARMOR` arrives
and a player reads **"a Armor enchant"** on their own tooltip. The article was hardcoded because at
the time there was only one article to hardcode. Nothing failed: it compiled, the whole suite stayed
green, the golden dump could not see it (no shipped item carries an active armor enchant), and the
three inert arms each held their own copy of the mistake.

The other two, same defect, different clothes:

| disguise | what was enumerated | what broke |
|---|---|---|
| **denylist gate** (`ANY_BUT_SHIELD`) | the one kind to refuse | `effect: damage` + `class: armor` LOADED CLEAN and could never fire |
| **switch STATEMENT** (`requireGate`) | the constants that existed | a new `Gate` value would fall through to NO validation at all |
| **hardcoded article** (`"a " + label`) | the labels that existed | "a Armor enchant", player-visible |

#### The rule

State what the axis IS, not which of its values you happen to be handling.

- **A gate says what it CAN be, never what it cannot.** `MAIN_HAND_ONLY` refuses everything it does
  not name; `ANY_BUT_SHIELD` admitted everything nobody had thought of. The next gear kind must be
  refused by default, then deliberately admitted -- not admitted by default and deliberately refused.
- **A switch over an enum must be an EXPRESSION**, so the compiler enforces exhaustiveness. A switch
  STATEMENT over enum constants may cover nothing and compile. (Pattern switches over sealed types
  are already required to be exhaustive even as statements -- the trap is specifically
  enum-constant labels.)
- **Derive per-value text from the value**, never from a template that assumed today's values.
  `GearClassLabel.describeEnchant` builds the whole noun phrase in one exhaustive switch, so a new
  constant has to be given an article as well as a label before it compiles.

#### And the corollary this slice measured

**The compiler covers far less of an enum widening than this repo claimed.** Adding `GearClass.ARMOR`
produced TWO compile errors and one runtime failure. **Five further sites changed silently**, one of
which would have minted a helmet as a shield. Four separate documents -- including this file --
asserted the compiler caught it. **It catches the exhaustive switch EXPRESSIONS and nothing else.
Widening an enum is a checklist, not a build.**

#### STANDING CHECKLIST: audit the enum-constant switch statements before the next enum grows

Run before adding any enum constant, and specifically before Slice 2b, which adds
`EnchantEffect.MAX_MANA` and a `Gate` arm and would otherwise walk straight into this:

```bash
grep -rn 'switch (' --include=*.java core/src/main paper/src/main storage/src/main
```

Classify each: a switch is safe if it is an EXPRESSION (assigned, returned or yielded) or a PATTERN
switch over a sealed type. It is a trap only if it is a STATEMENT over enum CONSTANTS.

**Audited 2026-09-02 at `0677e37`, all 51 switches in main sources** — re-run before adding
`GearClass.TOOL` and creating `ToolKind`, which is what this checklist is for. Ten are statements;
**nine are pattern switches over sealed types** (`CastSpec`, `EffectSpec` x3, `StatusDefinition`,
`VisualSpec`, `CastResult` x2, `ContentValidator:327`), verified by reading each first `case` label.
Every one is compiler-checked. **The tenth is the known trap, and it is now DISCHARGED:**

- `RpgCommand.java:1231`, `switch (op)` over `EnchantOp`: three of six constants, no default arm.
  **The line had drifted from the 1069 recorded at the last audit** — which is itself the argument
  for re-running the grep rather than trusting the note. Deliberately partial (the other ops return
  before reaching it), so never a live bug. Discharged with the **comment** option rather than a
  throwing default, and the comment says why: the switch sits AFTER the state write and the re-mint,
  so a default that threw would turn a missing chat message into a half-applied edit — worse than
  the silence it replaced.

> **The previous audit stood for three slices and four enum additions.** "Run before adding any enum
> constant" is easy to skip precisely because nothing enforces it, and a stale audit reads exactly
> like a current one. The date and SHA above REPLACE the old line rather than sitting beside it, so
> there is only ever one answer to "when was this last true".

`ContentValidator.checkEffect` is the counter-example worth knowing: a switch STATEMENT that IS
exhaustiveness-checked, because its labels are type patterns over a sealed interface. Its javadoc
already records the build failure that proves it. Do not "fix" it.

### A MUTATION IS A HYPOTHESIS UNTIL YOU HAVE WATCHED IT REDDEN

**A mutation-table row that predicts "this reddens test X" is a claim, not a guard.**

And it can be wrong two ways at once:

1. **The assertion path masks the mutation.** The test asserts through code that repairs the damage
   before it is visible.
2. **The predicted test is insensitive to it.** The test exercises the mutated line and observes
   something else about it entirely.

**Run each mutation red before writing the row down.** A row that never reddens is a check that did
not run -- the same failure as a blind grep, one level up. `CLAUDE.md` already says a check that did
not run looks exactly like a check that passed; this is that rule applied to the tool used to *test*
the checks, which is the level at which it is easiest to forget.

**The worked example is in history: `48b8db9` (Armor Slice 2b), and specifically the commit inside it
titled "the two pool guards that could not fail, and the javadoc that said they could".** Six
mutations planned, **two came back green**, and each was a different one of the two failure modes:

- **Deleting `ResourcePool.setCurrent`'s `Math.min` left all ten tests green** -- mode 1. The
  assertion read back through `current()` -> `regenerated()`, which ends in its *own* `Math.min`
  against the same ceiling, so it reported the clamped number whether or not the clamp had ever been
  written. The test's own comment claimed it "reddens where a read-only test would not."

  The obvious repair does not work either, **and that had to be measured rather than assumed**:
  unequip-then-re-equip **self-heals**, because the re-equip pins a value it just read back, already
  clamped. The mutation erases its own evidence. What holds it now is an assertion on `setCurrent`'s
  contract in ISOLATION, at the one moment nothing downstream is covering for it.

- **Resolving the ceiling inside `compute()` was green across all 22 pool tests** -- mode 2. The
  predicted test was the concurrency one, and the prediction rested on a deadlock story that was
  never the observable part. The real property is **arity**: the resolver must be asked exactly once,
  or two reads straddling a gear change make "the guard passed, then the spend refused" reachable.
  A guard that counts the calls holds it; the concurrency test never could.

Both javadocs asserting the old stories are corrected in place, at the source, rather than left to be
re-derived. That is the other half of the rule: **when a predicted mutation comes back green, the
prose that predicted it is now known-false and is part of the fix.**

### A TUNING CHANGE CAN DELETE A RULE'S ONLY WITNESS WITHOUT TOUCHING THE RULE

**A rule survives because someone can see why it exists.** Retunes do not read rules — they change
numbers — and a number change can quietly remove the example that made a rule obvious, leaving the
rule standing with nothing behind it. **The rule is then correct and looks unmotivated, which is the
state in which it gets deleted as cleanup.**

This is not the same failure as the two above. Those are about checks that did not run, or ran and
were argued with. This one is about a check that runs, passes, and **stops demonstrating anything** —
so the next reader removes it in good faith, and the defect it prevented becomes reachable again with
no red anywhere on the way.

> Stats Slice 3 rebalanced mana from a 60-second refill to 100 seconds. That one constant silently
> retired **two** witnesses:
>
> 1. **The ULP hazard.** Slice 2's entire derive-from-ticks rule rested on
>    `100/(60*20) != (100/60.0)/20.0` — one ULP apart, so composing in seconds would have re-rated
>    every player. At the 100-second base the two orderings **agree exactly**. The forbidden rewrite
>    became harmless, and the rule forbidding it became a puzzle.
> 2. **The two-decimal rate formatter.** Its justification was that `trimNumber` printed the mana base
>    as `1.6666666666666665`. After the retune both bases land on whole numbers over five seconds, so
>    the trimmer looks perfectly adequate.
>
> Neither rule was wrong. Both had simply become unfalsifiable-looking.

**So: when a retune removes a rule's example, re-anchor the rule to a witness that survives.** Two
kinds work:

- **A fact about the domain rather than about your code.** `ManaRegenTest` keeps the retired
  60-second base as a standing case, asserting that the two division orderings differ *there*. It has
  no mutation and says so out loud — it is not testing our code, it is holding open the reason our
  code is shaped as it is.
- **A reachable edge case rather than the shipped default.** The formatter's witness moved from the
  base rate to `0.2 + 0.1` per second, which is `0.30000000000000004` and prints
  `1.5000000000000002` over five seconds. Any off-round bonus reaches it, so no future retune of the
  *defaults* can take it away.

**Say which measurement the retune retired, and where the replacement lives.** The prose that cited
the old example is now false and is part of the change — the same closing clause as the rule above.
A superseded plan gets a note at the top naming what stopped being true; it does not get edited into
looking as though it always said the new thing.


### A GATE ROW CAN BE IMPOSSIBLE, OR REAL BUT NON-DISCRIMINATING, AND BOTH CREDIT COVERAGE THAT DOES NOT EXIST

**WHY THIS IS A RULE HERE AND NOT GENERAL ADVICE.** In a project with real automated coverage, a
miscredited manual check is a nuisance: the suite still runs the code, and a defect has other ways to
surface. **In this repo the boot gate is not a supplement to the suite — for eight behaviours it IS
the suite**, exactly as the unwitnessed table in the Crafting Slice 1 section now records. Nothing
constructs `MenuRouting`, `Menu` or `CraftingMenu`, because nothing can without a server. So a
miscredited gate row here is not a gap in redundant cover. It is **an unwitnessed defect wearing a
passing row**, and the suite is structurally incapable of noticing it — no red exists to be missed.
That is what makes this worth a rule rather than a habit.

**HOW IT DIFFERS FROM RULE 2, precisely.** Rule 2's second failure mode is INSENSITIVITY: the test
runs the mutated line and observes something else. This one is NON-REACHABILITY: the check never
enters the call at all. Different defects, and the second is harder to see — insensitivity at least
has you standing in the right method, where the fix is a better assertion. Non-reachability puts you
in a neighbouring call that passes honestly, so there is nothing to sharpen and nothing to notice.

Crafting Slice 1 produced one of each, in the same afternoon, over the same property, and **both were
written by the reviewer specifying the checks** — which is the point. This catches the person whose
job is deciding what gets verified, not the person implementing it.

**IMPOSSIBLE.** Row 12b was written from reasoning as *"two milk buckets in one slot"*.
`MILK_BUCKET.getMaxStackSize()` is **1**, so the state it described cannot exist, and the number was
never checked — the first rule in `CLAUDE.md`, applied to a gate table instead of to code. It did not
fail and it did not pass. **A row that cannot run is not a partial pass and must not be counted**:
the gate was 27/27, never 27/28.

**REAL BUT NON-DISCRIMINATING, and this is the dangerous one.** Row 12 — craft a cake, count the
three empty buckets — is a genuine row that genuinely passed. It was then credited, in the review
message that put the line into this file, as the witness for `getOverflowItems()`, **and it can never
reach that call**: the cake's buckets FIT BACK into the matrix, so the row exercises
`getResultingMatrix()` and stops. A passing row stood in for a call that had never once executed.
Nothing looked wrong, because nothing was red — and here, nothing else was ever going to be.

Reaching the overflow give needed a remainder-producing ingredient that **stacks**, so consuming one
leaves the slot occupied and the remainder homeless. Buckets are precisely the wrong family. A sweep
of the whole `Material` enum found **exactly two** candidates — `HONEY_BOTTLE` (16) and
`DRAGON_BREATH` (64), both remaindering to `GLASS_BOTTLE` — and row 12c was built from one of them.

So, when a row is named as the witness for a specific code path:

- **State which line it reaches.** "Row 12 covers the remainder handling" is not a claim until you
  can say *which call* it enters. Two adjacent calls in the same method are not the same witness.
- **Check the row is physically possible before writing it.** Stack sizes, recipe shapes and
  registry contents are all queryable. `Material.getMaxStackSize()` throws headless, so it needs a
  booted server — that is a reason to boot, not a reason to guess.
- **Prefer a row whose PASS is impossible without the path.** Row 12c fails visibly if the overflow
  give is deleted, because four glass bottles simply never arrive. Row 12 passes either way.
- **A row that turns out to be impossible gets REPLACED and SAID SO**, never quietly swapped. The
  original stays in the table marked as never-a-test, because the next reader's question is "was this
  checked", and "it was replaced because it could not exist" is a different answer from "it passed".

#### Corollary — A WITNESS THAT IS NOT IN THE REPOSITORY IS NOT A WITNESS

Rule 4 says a gate row can claim coverage it does not have. This says a row can claim coverage
**while not existing anywhere durable**, and it is the same defect one level further out.

For two slices this file recorded that nine behaviours on the crafting surface were boot-gate-only —
the suite passes with any of their checks deleted — and obliged a re-run whenever they changed. The
gate itself lived in a chat transcript and two published PR bodies. **Row 20's text was
unrecoverable from inside the repository.** A refactor of `MenuRouting` would have found a table
naming precisely what was unwitnessed and no way to run the thing that witnessed it.

`GATE-crafting.md` is committed for that reason, and it is the source of truth: a rendered
tick-through page may exist for convenience, and if the two disagree the file wins.

**This is the REVIEWER's defect, not the builder's** — the gate was authored as chat messages and
web pages because that suited the operator, and nobody asked where it lived. Recorded with the same
attribution rule 4 already carries, for the same reason: this catches the person deciding what gets
verified.

**It also must not be reconstructed.** Rebuilding lost rows from a sole-witness column produces a
plausible row that has never been run — rule 4's own failure mode, applied to the record of rule 4.
A lost row is replaced and said so, or recovered from outside; it is never inferred.

**And a RESULT is never inferred either — this is the same rule about the other column.** Crafting
Slice 3 recorded mutation M6 as run because the operator confirmed "the gate was run" and M6 sits in
the gate file's slice 3 section. It had not been run. M6 is a separate BUILD rather than a row, so a
report covering the rows never covered it, and the gap was filled by inference rather than noticed
as a gap.

**A pass goes into this file only when someone says it was OBSERVED** — never because it would be
consistent with what they did say. The failure is invisible in exactly the way the rest of this
section describes: there is no red to miss, because there is nothing for a suite to run. When a
report is silent about a check, the honest record is silent too, and the check stays owed.

- After every commit: `./mvnw -pl core test`. After every batch:
  `./mvnw clean package` and a manual boot.
- If a fix requires importing Bukkit into `core/`, stop and ask.
- Prefer a failing `core/` test that reproduces the bug before fixing it.
- Do not fix a compile error by widening the architecture.
- When you say something is verified, say what you executed.
- **Verify a check ran before believing it passed.** See `CLAUDE.md`.
