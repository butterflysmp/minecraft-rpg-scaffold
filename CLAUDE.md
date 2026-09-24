# CLAUDE.md

Read this before writing any code in this repository.

## Commands

```bash
./mvnw clean package     # build all modules
./mvnw -pl core test     # unit tests (fast, no server)
./scripts/dev-server.sh  # build + deploy + boot a local Paper server
./scripts/check-crlf.sh  # is any file mixed CRLF/LF? RUN BEFORE TAKING CR READINGS
```

`check-crlf.sh` guards the INSTRUMENT, not the repo: a part-CRLF/part-LF file makes
`tr -cd '\r' | wc -c` return a number that is neither. **It is a REQUIRED STEP, not a tool that
exists** — *VERIFICATION* says when to run it and what to report.

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

## WHERE THE RULES LIVE — decided 2026-09-10, after the split happened twice

**`CLAUDE.md` carries the rule in the form that changes what you do next. `NEXT.md` carries the named
rule, its worked examples, and the history of what it cost to learn.**

Two families had already split across both files — the mutation-lies table (here, examples there) and
the control-rules pair (operational form here, named rules there). **Two occurrences is a convention
forming by accident**, so it is stated rather than left: a reader looking for "the rules" should not
have to know which family a rule is in before knowing where to look.

**Why this way round, and the argument is about who reads what.** This file's own first line is *read
this before writing any code*, and it is loaded every session; `NEXT.md` is **10,966 lines** read
on demand (measured at `0dd9bbe`; this said *"nine thousand"* until 2026-09-15). **A rule that lives only in `NEXT.md` will not be read by the person about to break it.**
The cost is that this file grows, and it is paid down by keeping each entry here to the operational
core — what to DO — and leaving the persuasion, the worked example and the dated instance to
`NEXT.md`.

**Overturnable.** The opposite convention — everything in `NEXT.md`, pointers here — keeps this file
short, and if it grows past being readable in one sitting that is the trade to revisit.

### AND THE GENERAL FORM, BECAUSE TWO HOMES IS NOT ONLY A `CLAUDE.md`/`NEXT.md` PROBLEM

**WHEN ONE RULE MUST APPEAR TWICE, ONE COPY IS THE POINTER AND ONE IS THE ACCOUNT, AND EACH SAYS
WHICH IT IS.**

- **The pointer carries the form that changes what you do, and nothing else.**
- **The account carries the mechanism, the measurements and the consequences.**

**TWO ACCOUNTS DRIFT INTO DISAGREEMENT AND NEITHER IS THEN TRUSTWORTHY** — a reader who finds two
explanations of one rule has no way to tell which was updated. **And a pointer that has grown a second
explanation has become an account: cut it back.**

**Two tests it must pass, and they are the reason the split works:**

- **The pointer must be obeyable without opening the account.** *"Author multiples of 4"* can be
  followed by someone who never reads the mechanism. **A pointer that needs its account to be useful
  is a broken copy, not a pointer.**

  > **THIS EXAMPLE USED TO READ *"multiples of 4; multiples of 8 if it may ever be dual-wielded"*, and
  > the second half was withdrawn on 2026-09-13** when the dual-wield design was refused — see the
  > rule's pointer under *Standing decisions*. **The quote is updated rather than left standing, because
  > an example that quotes a rule verbatim goes stale exactly when the rule moves** — and a worked
  > example of a *well-formed pointer* that misquotes its own pointer is the failure it is teaching
  > against. The example's point is untouched: the surviving half is still obeyable without the
  > account, which is the property being demonstrated.
- **The pointer names a SECTION, never a line.** `WeaponLoader`'s `cooldown_ticks` section, not
  `WeaponLoader.java:175`. A line citation is falsified by any insertion above it, silently and
  invisibly to every test — this repo carries an open finding measuring that blast radius at ~140
  sites, produced by exactly the other choice.

> **THE ACCOUNTS MOVED OUT OF THIS FILE, 2026-09-24: the "Overturnable" trade above, taken.** At
> 428 lines this file was no longer readable in one sitting. So the long accounts under
> *Standing decisions*, and the upgrade procedure's ordered steps, now live in path-scoped
> `.claude/rules/*.md` files, which load when a file matching their `paths:` is open. Each block
> moved WORD FOR WORD and left a pointer here that passes both tests above. **The split is the one
> `verification.md` already uses: the pointer here, the account in a rules file, each saying which
> it is.** `NEXT.md` stays the home of the dated instances. A rules file with no honest path does
> not get one: an unscoped file loads every session, and the rule it holds stays here.

## VERIFICATION — THE NAMES. THE ACCOUNT IS `.claude/rules/verification.md`

**A check that did not run looks exactly like a check that passed.** The account — every mechanism,
every measurement, every dated instance — is `.claude/rules/verification.md`. It loads on its own
whenever you open a file under `**/src/test/**` or `scripts/**`. **When you are about to verify
something and are NOT in one of those paths, open it yourself: nothing will do it for you.**

**This index is NAMES, and that is the whole job.** The name is what makes someone think *this is a
hollow fixture*; the account is what they read next. **Each name is a SECTION heading in that file,
so it is greppable** — and none of them is reproduced here, because two accounts drift.

| family | reach for it when |
|---|---|
| **A CHECK THAT DID NOT RUN, AND A GUARD NOBODY WATCHED FAIL** | you are about to believe a check passed, or explain away a guard that fired |
| **THE FOUR FALSE ABSENCES, THE FALSE PRESENCE, AND WHAT DISCOVERS** | a search came back empty, or a hit may be prose ABOUT the thing rather than the thing |
| **ZERO TESTS RAN, AND WORK A REVERT DESTROYED** | a build says SUCCESS, or you are about to undo a mutation |
| **A VERIFICATION REPORT: WHAT RAN, THE BYTE SHAPE, THE RECONCILED FILE LIST, THE FINAL SUITE** | you are about to write down that something was verified |
| **PUSHES, READ FROM THE WIRE** | you are pushing, or about to say something was pushed |
| **WHAT A FIGURE DOES NOT PROVE: INTEGRITY HASHES, CARRIED CONTROLS, EXIT STATUSES** | you are quoting a hash, a control or an exit status as proof |
| **WHERE A NUMBER CAME FROM TRAVELS WITH IT: ESTIMATES, DESCENT, REVISIONS, TOLERANCES, RETIRED FIXTURES** | a number is going into the record: an estimate, a derived figure, a measurement for later, a tolerance you set |
| **WHERE A FINDING IS RECORDED, AND HOW PROSE REACHES A COMMAND** | a finding is still only in the chat, or prose is about to reach a command |
| **A PREDICTION THAT SEVERAL OUTCOMES SATISFY IS NOT A CONTROL, IT IS A RANGE** | you predicted a shape in words and are checking it against numbers |
| **TWO RULES FROM ONE REVIEW, AT THE SEAM BETWEEN A FIGURE AND THE SENTENCE ABOUT IT** | a revision touched a line it was not revising, or a general form disagrees with its own worked rows |
| **EVERY FILTER AND EVERY SCRIPTED EDIT NEEDS A POSITIVE CONTROL** | a `perl -i`, a grep filter, a needle or a count — anything whose silence you are about to read as success |
| **THE EIGHT WAYS A MUTATION LIES, AND EACH GUARD IS BLIND TO THE NEXT** | you are running a mutation and about to believe the marker grep |
| **AND THE ARM THAT MOST EARNS ITS KEEP IS THE ONE MOST LIKELY TO BE UNREACHABLE** | you are writing a guard for a case no shipped content can produce |
| **A HOLLOW FIXTURE** | a row may never present the condition it claims to test — in SPACE, TIME, KIND or OBSERVABILITY |
| **THE CREATIVE-DIVERGENCE REGISTER — THE BENCH IS NOT THE GAME** | you are writing or reading a `GATE-*.md` at all: R0, the declared game mode, the standing debt |
| **THREE THINGS THAT DO NOT ANNOUNCE THEIR OWN ABSENCE** | a planned guard silently failed to land, or a comment names a hazard and is being read as a guard against it |

**MERGING, DELETING A BRANCH, OR WRITING A SQUASH BODY: `.claude/skills/merge-procedure`.**
`git branch --merged` prints NOTHING for a fully merged branch here and two-dot `git diff` inverts
once `master` moves — every PR on this repo is squash-merged. Run `./scripts/check-absorbed.sh
<branch>`; it has FOUR outcomes, not two.

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

## Standing decisions — the operator's, not derivable from the material

A decision that isn't written down will eventually be contradicted by prose reasoned from first
principles. The reasoning will be sound; the premise was simply unavailable. Everything here is a
call that has already been made, so **do not re-derive it — and do not write a note that assumes
the opposite because the material suggests it.**

- **NO CUSTOM ITEM STACKS ABOVE 1.** Every weapon, tool, armour piece and shield we mint is a
  single item, whatever its base material does. `WeaponItems.mint`, `ToolItems`, `ArmorItems` and
  `ShieldItems` all call `meta.setMaxStackSize(1)`, at the source, so `/rpg give`, a craft and a
  re-mint all inherit it. Per-item state is why: durability, enchants and instance data are per
  item, and one write to a stack of two would edit both.

  > **The worked example is in this repo.** `flint_staff.yml` shipped a trap note reading *"THE
  > MINTED STAFF STACKS TO 64, because a stick does… isSimilar and will merge."* Correct reasoning
  > from the material, written **nine days after** `347967b` capped the mint at 1, and wrong on the
  > day it landed. It shipped in one PR and was copied into `lapis_staff.yml` in the next. Only a
  > gate row that physically stacked two staves caught it.

- **NEW CONTENT CITES THE BOLTOR, NOT `hunters_bow`, `ironblade`, `quiver_stone` OR `ability_stone`.**
  Those four are in a deletion set. If a dev weapon is truly the only precedent, cite it AND MARK the
  citation as standing on the deletion set, so the sweep finds it by grep. **Never derive a new
  weapon's numbers from a dev weapon's.** The Boltor's numbers are RULED (`quiver_size 8`,
  `cooldown_ticks 16`, `range 96`, `reload_ticks 60`, `attack_damage 19`). Held right-click inputs
  land on a 4-tick grid, so a cooldown of `13` or `14` IS `16`, and the tooltip will not say so.
  **Author multiples of 4.** The "multiples of 8 if dual-wielded" clause is withdrawn, so the
  `locust`'s `12` is correct.
  *This is the pointer. The account (the two ruling dates, the loop that grows the deletion's cost,
  the measurements, and why the clause was withdrawn) is `.claude/rules/standing-decisions.md`, under
  its top heading: the entry beginning **NEW CONTENT CITES THE BOLTOR**.*

### NAME THE QUANTITY, AND NAME THE SET OF THINGS THAT HAVE IT

**Before writing any `value_by_level` or any modifier, name the quantity it moves, and answer BOTH
halves.** (1) ARITHMETIC: is the quantity non-zero and continuous at the values it will meet? If it
is quantised, floored or absent, author a FLAT value, or park the feature. (2) ELIGIBILITY: does
every piece of gear this can roll on actually possess that quantity? If not, the gate is wrong, and
no curve fixes it. **Write down the quantity's actual authored values before the curve.** The
instruction is *name the quantity and check it*, NOT *prefer flat*.

*This is the pointer. The account (the four failures from one slice, why the two halves are one
rule, and Punch as the case that survived) is `.claude/rules/standing-decisions.md`, section
`### NAME THE QUANTITY, AND NAME THE SET OF THINGS THAT HAVE IT`.*

### A TRAVELLING RANGED WEAPON AUTHORS KNOCKBACK; AN INSTANT-HITTING ONE DOES NOT

**Operator ruling, 2026-09-13.** A `type: projectile` RANGED weapon declares `- type: knockback`
(with a `strength`) in its `on_hit`. A `type: ray` ranged weapon declares none. It is a content rule,
and the engine does not imply it. It covers RANGED weapons only. A travelling mage or melee weapon
(`ember_staff`, `flint_staff` and `emberblade`, as measured 2026-09-13) is **UNRULED, not excluded**:
do not "fix" one, and do not cite this rule at one. **Punch must not roll on a weapon that authors no
knockback.** Punch is PARKED.

*This is the pointer. The account (why the engine cannot imply it, the scope measurement, the
unruled-vs-excluded rule, and the roll gate) is `.claude/rules/standing-decisions.md`, section
`### A TRAVELLING RANGED WEAPON AUTHORS KNOCKBACK; AN INSTANT-HITTING ONE DOES NOT`.*

## Upgrade procedure

**Step 0 is yours: notice the release.** Nothing does it for you (no bot, by decision; see `NEXT.md`
D4). Check <https://modrinth.com/plugin/packetevents/versions> yourself. **Never bump `paper.version`
alone: PacketEvents goes first, because it is the gate.** Pin exact builds, never version ranges.
*This is the pointer. The ordered steps are `.claude/rules/upgrade-procedure.md`, section
`## Upgrade procedure`, and they load when a `pom.xml` is open.*

## Working with me

- I am rusty at Java. If you use a language feature I may not know — sealed
  interfaces, records, pattern matching in `switch`, `var` — explain it in one
  line rather than assuming.
- Write the `core/` unit test before the `paper/` wiring.
- Keep changes small enough that I can read them.
