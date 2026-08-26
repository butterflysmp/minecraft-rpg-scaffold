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
- **The energy economy.** `MAX_ENERGY` and `ENERGY_PER_TICK` are Java constants
  in `RpgPlugin`. Three abilities with different costs is the first time that
  will feel wrong. When it does, they become per-archetype content — the same
  cheap-now/migrate-later argument that moved `VisualSpec` to a `steps:` list.

---

## Deferred, deliberately

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
- **`BukkitCombatant.applyHeal` is vanilla-only — ability heals bypass custom HP.** It calls
  `entity.setHealth(...)`, not `CombatantStats.heal`, so an ability `Heal` effect (e.g. `arc_surge`)
  raises *vanilla* health and never fires the seam — the heart bar / nameplate don't follow. Same class
  as the damage gap 1a fixed, on the heal side. `/rpg mobheal` sidesteps it by calling `stats.heal`
  directly. Wire `applyHeal` to the custom store in the status/heal pass.
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
    burning vanilla health, `applyHeal` raising vanilla health, and mob projectile→player skipping the
    melee gate (all above). They are one problem wearing four hats — *every* route into an entity's
    health that is not `applyDamage` is invisible to the custom store — and are best solved as one
    decision about where the boundary sits, rather than four independent patches.
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

  **There are now FIVE `_TEMP` fixtures, not two, and three of them are a different shape.**
  This entry was written when the debt was status-content only; the stat passes added three
  ITEM fixtures, which live in Java rather than yml and so will not turn up in a content-pass
  grep of `content/`:

  | fixture | mints via | lives in | proves |
  |---|---|---|---|
  | `rooted_TEMP` | ability content | `content/abilities/*.yml` | status castable in-game |
  | `soaked_TEMP` | ability content | `content/abilities/void_slash.yml` | as above |
  | `health_boost_TEMP` | `/rpg healthboost` | `paper/health/HealthModifierItems.java` | the equip/unequip max-HP modifier lifecycle |
  | `attack_speed_boost_TEMP` | `/rpg attackspeed` | `paper/weapon/AttackSpeedModifierItems.java` | the same lifecycle for attack speed |
  | `class_damage_boost_TEMP` | `/rpg classdamage <class> [amt]` | `paper/weapon/ClassDamageModifierItems.java` | the same lifecycle again, plus the class GATE: it goes inert when you swap to another class's weapon |

  The three item fixtures come out when real content grants those stats (an enchant, a passive,
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
  UI pass; see `PLAN-enchant-table-ui.md`). The original entry is kept verbatim below; what follows
  it is what it turned into. Pass 2. This pass's
  relationship to the table is exactly Durability Pass 2's to auto-wear: build the mechanism, drive
  it with a dev instrument, and leave the thing that will really drive it for a pass that can decide
  its own questions. The table needs the XP economy and bookshelf power below, neither of which
  exists.
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

- **Still deferred after Pass 2:** the per-instance roll and the class pools (now with a real roster
  of three damage enchants to draw from), ~~the enchant table UI~~ (DONE — the Enchant UI pass), the
  XP economy and bookshelf power, and a SUMMONER enchant — which still waits on the class, which
  still waits on mob-to-mob damage.

- **The tooltip shows no percent, and the Phase-4 entry stays OPEN.** Pass 2 grants the first real
  enchant STAT DELTA, which is what that entry was waiting for, but `EnchantLore` still renders only
  "Sharpness III" — no `+15%` line, and the weapon's damage numbers are still its authored base.
  Surfacing a RESOLVED number belongs with the stat screen, beside `+N Melee Damage` and the resolved
  attack speed, per the standing rule that lore describes the weapon and not whoever holds it.


- **The enchant table's boot gate is OWED IN FULL BY A HUMAN.** Every one of its 23 rows needs a
  `Player` — the menu, the clicks, the item-safety cases and the shutdown return — and a console log
  can only prove the plugin loaded. Nothing in the pass was witnessed in-game. The gate is in
  `PLAN-enchant-table-ui.md`; rows 6–10c and 18–22 need the item COUNTED before and after, because a
  dupe that creates a second weapon and a theft that removes one look identical in a screenshot of
  the after state.

- **The bookshelf readout is a labelled placeholder, not a `0%` count.** Nothing counts bookshelves,
  and slot 8 says "Not implemented yet" in as many words rather than rendering a zero. A readout
  reporting `0%` when nothing is measured is indistinguishable from a working readout that measured
  zero — CLAUDE.md's own failure mode, in a place a player can see. This is a deliberate deviation
  from the layout brief, which asked for a greyed `0%`.

- **Unlocking is FREE, and the XP economy gates the same click.** `EnchantClickIntent.of` is a pure
  function and the cost check goes in FRONT of it, never inside: what a click MEANS and whether you
  can afford it are different questions. So the economy pass adds a guard and a cost to
  `applyCandidateClick` and changes nothing structural, and the bookshelf discount lands on the
  readout that is already sitting there saying it does not exist yet.

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

- **The table WINS a right-click on an enchanting table; sneaking bypasses to the weapon trigger.**
  Vanilla's own convention — a block interaction beats an item's use unless you sneak — and the
  escape hatch is what makes it safe: the other ordering means a Mage holding their staff can never
  open the table they are standing at. Flagged for tuning; boot rows 3 and 4 are where it is judged.

- **The single-button interaction model is a PROPOSAL.** Locked → unlock-and-activate, unlocked →
  activate, active → level up, one click each. It is a pure function precisely so tuning it means
  editing `EnchantClickIntent` and its tests rather than re-reading a Bukkit class. Boot rows 12–15b
  are where it is judged.

- **The cross-inventory gestures are PERFORMED by the router, never permitted.** Shift-click and the
  number keys are both supported, and neither is ever un-cancelled. The reason they were refused
  outright at first still stands and is exactly why: vanilla picks the destination itself — across
  the whole other inventory for a shift-click, and as a two-way SWAP for a number key — so
  permitting either would be accepting a slot chosen by someone other than us, bypassing every
  check. `MenuRouting.shiftMove` and `MenuRouting.hotbarMove` choose the destination themselves and
  move the item by hand.

  **`NUMBER_KEY` is intercepted by TYPE, ahead of even `ALWAYS_REFUSED`**, and that ordering is the
  point: a number-key press over an EMPTY slot does not resolve to `HOTBAR_SWAP`, so matching on the
  action alone would miss exactly the case that matters. `HOTBAR_SWAP` and `HOTBAR_MOVE_AND_READD`
  were therefore removed from the refused set — only a number key produces them, and listing them
  after the type intercept would be dead weight that reads like a guard.

  **Neither may ever be added to `OWN_INVENTORY_ACTIONS`**, which would hand the destination
  straight back to the server. All three entry paths — click-place, shift-click, number-key — share
  ONE `placeAllowed`, so the "empty slot ← exactly one item" model cannot hold on one and not
  another.

  The fall-through is load-bearing rather than incidental: because the whitelist means a move the
  router does not perform simply does not happen, a filler pane cannot leak into an inventory or a
  hotbar even though shift-clicking and number-keying one now both reach that code. `SWAP_OFFHAND`,
  `DOUBLE_CLICK` and `CREATIVE` stay blanket-refused. A shift-click INSIDE the player's own
  inventory is cancelled too — a small loss of convenience for the guarantee that the router moves
  everything or nothing.

  Only the INWARD direction has a number-key path. Taking the weapon back out is a shift-click or a
  plain left click, both already supported; a number-key take-out needs its own empty-hotbar-slot
  rule and nothing asks for it, so it stays out rather than being half-built.

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

- After every commit: `./mvnw -pl core test`. After every batch:
  `./mvnw clean package` and a manual boot.
- If a fix requires importing Bukkit into `core/`, stop and ask.
- Prefer a failing `core/` test that reproduces the bug before fixing it.
- Do not fix a compile error by widening the architecture.
- When you say something is verified, say what you executed.
- **Verify a check ran before believing it passed.** See `CLAUDE.md`.
