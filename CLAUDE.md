# CLAUDE.md

Read this before writing any code in this repository.

## Commands

```bash
./mvnw clean package     # build all modules
./mvnw -pl core test     # unit tests (fast, no server)
./scripts/dev-server.sh  # build + deploy + boot a local Paper server
```

Always use the wrapper, never a system `mvn`. It pins Maven 3.9.9 so the build is
reproducible; there is no system Maven on this machine.

Always run `./mvnw -pl core test` after changing anything in `core/`. Prefer adding
a unit test to `core/` over testing in-game. In-game testing is a 60-second loop;
unit tests are a 2-second loop.

## Environment (verify before assuming)

| Thing | Value |
|---|---|
| Java | **25** (required by Minecraft 26.1+) |
| Paper API | pinned in `pom.xml` as `paper.version` |
| PacketEvents | pinned as `packetevents.version`, `provided` scope, **not shaded** |
| Minecraft versioning | year-based since 26.1 (`26.1`, `26.2`, ...), not `1.21.x` |

## BANNED PATTERNS

Your training data is saturated with Bukkit tutorials from 2015-2023. Most of
what you will reach for first is deprecated or wrong here. Do not write:

| Never write | Write instead |
|---|---|
| `plugin.yml` | `paper-plugin.yml` |
| `onCommand`, `CommandExecutor`, `TabCompleter` | Brigadier via `LifecycleEvents.COMMANDS` |
| `ChatColor`, `§` codes, `ChatColor.translateAlternateColorCodes` | Adventure `Component`, MiniMessage |
| `BukkitRunnable`, `Bukkit.getScheduler().runTask*` | the `Scheduler` interface in `paper/scheduler` |
| NBT reflection, NMS, `CraftPlayer` casts | Persistent Data Containers, `Keys.java` |
| `getServer().getPluginManager().registerEvents` sprawl | one registration point in `RpgPlugin` |
| Hardcoding an ability/weapon/boss in Java | a YAML file in `content/` |

If you believe an exception is warranted, say so and ask. Do not just do it.

## THREADING — the rule that will actually break production

**PacketEvents callbacks run on Netty I/O threads.** They are not the main
thread and not a Folia region thread.

- Inside `onPacketReceive` / `onPacketSend`: read the packet, compute, cancel.
  **Touch nothing else.**
- To reach the Bukkit API from a packet callback, use
  `PacketListenerBase.bukkit(player, () -> ...)`. There is no other sanctioned route.
- Never call `player.sendMessage(...)`, `world.spawnParticle(...)`,
  `entity.setVelocity(...)` or anything else Bukkit from a packet thread.

This bug does not reproduce with one player on a test server. It corrupts state
at forty. Treat a violation as a build-breaking error.

Separately: all scheduling goes through `Scheduler` (`onEntity`, `onRegion`,
`onRegionLater`, `onGlobal`, `async`). This exists so the project runs on Folia
later without a rewrite. `async` must never touch the Bukkit API.

## VERIFICATION — a check that did not run looks exactly like a check that passed

**Verify a check ran before believing it passed.**

**And when a guard fires and you have an explanation for why it does not count, the
explanation is a hypothesis. Test it.** A guard that ran and was argued with is worse
than one that never ran, because you now believe you checked.

> 2026-07-10: `*** MUTATION STILL IN DEPLOYED JAR ***` fired, correctly, and was
> dismissed as a race condition — a plausible story, since the build had been
> backgrounded. It was a file lock. The previous test server was still running, `rm -f`
> on the deployed jar failed with `Device or resource busy`, and `set -e` aborted
> `dev-server.sh` before it deployed anything. The mtimes settled it: target `05:40:54`,
> deployed `05:39:43`. Had the explanation been trusted, the mutated build would have
> booted a second time and the restore would have been blamed for the result.

This is a distinct failure from the four below, not a variant of them. Those are checks
that never ran. This is a check that ran, fired, and got talked out of. It survives
every other fix on this page: you can make every check run, confirm every mutation
applied, and still lose to a plausible story about why the red does not count.

The four below have bitten in the other direction:

1. `FakeWorld.schedule` discarded `delayTicks`, so no test could see *when* anything
   fired. A one-second bug shipped past 118 green tests.
2. Mutation checks run after `git checkout --` had destroyed the code being tested.
   The suite could not compile; the empty output read as "passed".
3. A mutation that was a compile error, not a mutation. It printed nothing.
4. A defect-asserting test that passed on a floating-point accident rather than on the
   filter it was written to guard. Deleting that filter did not fail it.

So:

- Before believing a **mutation** result, confirm the mutation **compiled and applied**.
  `grep` for your marker; run `test-compile` first. A mutation that does not compile is
  not a mutation.
- Before believing a **test guards** something, **break the thing and watch it fail.**
  A test that cannot fail is worth nothing, however green.
- Anything that **discovers** rather than asserts — a scan, a glob, a registry walk —
  must **fail loudly when it discovers nothing.** Finding zero items is a defect, not a
  quiet no-op. `getResource("content/")` on a shaded jar returns a non-null URL whose
  stream is zero bytes and whose `list()` is `null`: it does not throw, it silently
  finds nothing, and on a server whose data folder is already populated that is
  indistinguishable from working. Only a *fresh* data folder exposes it.
- `BUILD SUCCESS` with no `Tests run:` line means **zero tests ran**. Surefire's
  `-Dtest=` takes commas, not `+`; a bad pattern reports success having executed nothing.
- Never `git checkout --` a file with uncommitted work to undo a mutation. Copy it to the
  scratchpad first and restore from there.
- When you report something as verified, **say what you executed** and what it printed.

## Architecture invariants

```
core/     pure Java. ZERO dependencies. No Bukkit, no Paper, no PacketEvents, no Gson.
storage/  PlayerRepository port + File impl. Async by contract.
paper/    adapters. The only module that knows Minecraft exists.
```

1. **`core/` must never import `org.bukkit`, `io.papermc`, or
   `com.github.retrooper`.** Its `pom.xml` has no dependencies on purpose. If you
   need a Bukkit type in `core`, you are modelling the wrong thing — define a
   port interface (see `CombatantHandle`, `CombatWorld`) and implement it in `paper/`.
   Reads and writes are separate ports: `CombatantSnapshot` is a value captured on the
   thread that owns the entity, `CombatantHandle` only dispatches. You cannot hop a
   thread and still return a value.

2. **Content is data, not code.** Abilities, weapons, elements' *instances*,
   loot tables, boss phases live in YAML under `content/`. `AbilityLoader` is the
   only class that knows the schema. Adding the 500th weapon must not require a
   recompile.

3. **Never assume one server.** No static mutable singletons holding player
   state. Player data goes through `PlayerRepository`. `Bukkit.getOnlinePlayers()`
   is not the source of truth for "who is playing".

4. **Prefer the Bukkit API over packets.** Every hand-written packet is a thing
   that breaks on protocol changes. Reach for PacketEvents only when the API
   genuinely cannot express the effect (custom UI, damage numbers, telegraphs,
   fake entities).

## Upgrade procedure

Do **not** bump `paper.version` alone. Order of operations:

0. **Notice the release. Nothing does this for you.** There is no bot on this
   repo, by decision — see `NEXT.md` D4. Check
   <https://modrinth.com/plugin/packetevents/versions> yourself. An absent
   notification looks exactly like nothing to notify, and this step is the one
   that silently never happens.
1. Check PacketEvents supports the new Minecraft drop. It typically lags a
   Minecraft release by 1–2 weeks. It is the gate.
2. Bump `packetevents.version` first, confirm it builds.
3. Bump `paper.version`.
4. Run `./mvnw -pl core test`. If `core` tests break on a Paper bump, `core` has an
   illegal dependency — that is the real bug.
5. Boot `./scripts/dev-server.sh` and smoke-test one ability end to end.

Never use version ranges. Pin exact builds so the build is reproducible.

## Squash-merge bodies — the squash outlives everything it was made from

A squash body is the only durable account of a branch. The commits are gone, and once the branch is
deleted the tree comparison behind `ABSORBED` **cannot be re-run by anyone** — only the report of it
survives. So write the body for a reader who has none of that.

**Open with a FRESH SUMMARY OF THE FINAL STATE**, written at merge time. Then the per-commit history
underneath it.

> **`37c0ea7` is the worked example of getting this wrong.** Its body opens with *"Nine suggestions
> in row 4"* — true when the first of nine commits was written, **false three commits later in the
> same squash**, and now the top-line description of what slice 5 shipped. The column had moved to
> column 7 and shrunk to three cells before the branch merged. Nothing lied; the first paragraph
> simply aged out from under itself, and a default squash body always leads with the OLDEST
> description of the work.

**Record in the body, because they become unverifiable the moment the branch goes:**

- the branch **tip SHA** and its **tree SHA**
- the squash's own tree SHA, and that the two were EQUAL
- the `check-absorbed.sh` verdict verbatim, including the positive-control line

Then `git branch -D`, `git push origin --delete`, `git fetch --prune`.

## Branch cleanup — `--merged` LIES here, and so does the obvious replacement

Every PR on this repo is **squash**-merged. The branch's commits never become ancestors of `master`,
so:

```
git branch --merged master     # prints NOTHING, even for a fully merged branch
```

It reports every merged branch as unmerged. That is worse than useless: it either talks you out of a
safe `-D`, or it trains you to ignore a check you will one day need. `-d` will refuse for the same
reason, which is why deleting an absorbed branch here correctly needs `-D`.

**The obvious replacement is also wrong**, and this is the part worth remembering:

```
git diff master <branch> --stat    # empty => absorbed?   ONLY if master has not moved since
```

It is right for a branch you just merged, and it silently inverts for any older one. Once `master`
gains one more commit, an absorbed branch diffs non-empty — not because it holds unmerged work, but
because it is *behind*. Measured 2026-08-28: `feat/sweep-rides-vanilla`, fully absorbed by `d586941`,
diffed against `master` as `24 files changed, 29 insertions(+), 1122 deletions(-)`. Every one of those
lines was the crit commit sitting on top of it. A rule built on that check refuses to clean up
anything except the branch merged five minutes ago.

**The check that actually answers the question** — "is this branch's content already somewhere in
master's history?" — compares TREES, and needs no knowledge of which commit squashed it. **Run the
tool, not a hand-pasted loop:**

```bash
./scripts/check-absorbed.sh <branch>
```

A match means some commit reachable from `master` has a byte-identical tree, so deleting the branch
loses nothing.

**It has THREE outcomes, not two, and the third is the one that will bite you:**

| exit | verdict | means |
|---|---|---|
| 0 | `ABSORBED at <sha>` | safe to delete |
| 1 | `NOT ABSORBED` | tree absent AND master has not moved — a real STOP, unmerged work |
| 2 | `INCONCLUSIVE` | tree absent BUT master advanced past the merge-base — **this check cannot authorize either way** |
| 3 | `BLIND` | the positive control failed; no verdict was reached |

Exit 2 exists because the tree compared is the branch TIP's. If master gains any commit between the
branch point and the squash, the squash is built on that newer base — tree `newbase + changes`
against the branch's `oldbase + changes`. Genuinely absorbed, no match. Collapsing that into "NOT
ABSORBED" is a **false refusal**, and it happens the first time a PR lands behind another one.
There, confirm from the PR's merged state (`gh pr view <n> --json state,mergedAt,mergeCommit`) and
**say which source the conclusion came from** rather than letting a stale STOP stand.

So the check is sound in one direction only, which is the right direction for a delete guard: it
can never falsely say ABSORBED, but it can say "absent" for a reason that is not unmerged work.

Exit 3 is why the tool exists rather than the loop. A search that finds nothing reads exactly like
a search that ran correctly and found nothing — the defect this file records twice for content
scans. The script asserts a known-present tree (the base tip's own) is found on **every** run, and
refuses to render any verdict if it is not. Verified by mutation: break the search and it reports
BLIND instead of a confident, false `NOT ABSORBED`.

(The cheap special case, when you happen to know the squash commit: `git diff <squash-sha> <branch>`
empty proves the same thing. That is what was used before the general check existed.)

Then `git branch -D <branch>`, `git push origin --delete <branch>` if it exists remotely, and
`git fetch --prune` to drop the stale tracking ref. **List `git ls-remote --heads origin` before and
after** — the wire, not a local ref, is what says the remote branch is gone.

## Working with me

- I am rusty at Java. If you use a language feature I may not know — sealed
  interfaces, records, pattern matching in `switch`, `var` — explain it in one
  line rather than assuming.
- Write the `core/` unit test before the `paper/` wiring.
- Keep changes small enough that I can read them.
