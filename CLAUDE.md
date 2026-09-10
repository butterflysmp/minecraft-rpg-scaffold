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

## WHERE THE RULES LIVE — decided 2026-09-10, after the split happened twice

**`CLAUDE.md` carries the rule in the form that changes what you do next. `NEXT.md` carries the named
rule, its worked examples, and the history of what it cost to learn.**

Two families had already split across both files — the mutation-lies table (here, examples there) and
the control-rules pair (operational form here, named rules there). **Two occurrences is a convention
forming by accident**, so it is stated rather than left: a reader looking for "the rules" should not
have to know which family a rule is in before knowing where to look.

**Why this way round, and the argument is about who reads what.** This file's own first line is *read
this before writing any code*, and it is loaded every session; `NEXT.md` is nine thousand lines read
on demand. **A rule that lives only in `NEXT.md` will not be read by the person about to break it.**
The cost is that this file grows, and it is paid down by keeping each entry here to the operational
core — what to DO — and leaving the persuasion, the worked example and the dated instance to
`NEXT.md`.

**Overturnable.** The opposite convention — everything in `NEXT.md`, pointers here — keeps this file
short, and if it grows past being readable in one sitting that is the trade to revisit.

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

> **AND THE PASSING TWIN, which has no conversation to interrupt: A CONTROL THAT SUCCEEDS FOR THE
> WRONG REASON.** A gate row staged FOUR mobs against a chain limit of FOUR — so it passed whether the
> limit existed or not, and no explanation was needed because nothing looked wrong. **When two
> independent quantities in a row are equal, at least one of them is not being tested.** Ask what the
> row does if the rule it checks is deleted; if the answer is "the same thing", it is measuring the
> fixture. Full entry in `NEXT.md`, beside its red-side twin.

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

  > **THE MARKER GREP IS NOT BELT-AND-BRACES. IT IS THE ONLY THING THAT MAKES MUTATION
  > TESTING A MEASUREMENT RATHER THAN AN ASSERTION.** Twice now the edit has silently
  > failed to apply, by two unrelated mechanisms, and **the grep was the only thing that
  > caught either.** Neither exit codes, nor the test run, nor reading the command back
  > would have.
  >
  > - The Flint Staff's **P6**: the edit was split across two calls and the second never
  >   landed.
  > - **2026-09-08**, elements slice: `perl -pi -e` **exited 0, printed nothing, and left
  >   the file byte-identical** — while the *identical* regex matched when the same line
  >   was piped to `perl -ne`. The tool performing the check reported success while doing
  >   nothing.
  >
  > A mutation run whose marker was never grepped tells you nothing at all. A green suite
  > under an unapplied mutation reads exactly like a green suite under a real one — which
  > is this page's own defect, with the tooling rather than the test as the thing that
  > lied. So: **grep the marker, and grep it again after restoring** (`markers left: 0`),
  > or do not report the result.
  >
  > When an in-place edit no-ops, do not retry it with a cleverer pattern. Splice by line
  > number (`head`/`sed -n`/`tail` into the file) and re-grep. Retrying an edit that
  > reports success while doing nothing is how the same failure survives three attempts.
- Before believing a **test guards** something, **break the thing and watch it fail.**
  A test that cannot fail is worth nothing, however green.

  > **AND THE SAME APPLIES TO A GUARD IN PRODUCTION CODE. THE ENFORCEMENT RULE WAS AIMED
  > ONE STEP SHORT.** `NEXT.md`'s rule — *"when a comment claims a rule is enforced, the
  > claim names a file, so open it"* — catches **missing** enforcement. It does not catch
  > **unreachable** enforcement, and it would have passed the case below cleanly, because
  > the file contained the guard.
  >
  > **A guard that cannot fire is indistinguishable from one that protects you.** It
  > compiles, it reads correctly, it survives review, and every test around it is green.
  >
  > **2026-09-08, elements slice.** `ElementLoader.damageSymbol` validated its MiniMessage
  > with `try { deserialize } catch (RuntimeException)`, and the javadoc claimed a malformed
  > glyph became a named, skipped file. A probe over **ten** malformed inputs — unknown tag,
  > unknown colour, bad hex, unclosed tag, mismatched close, a bare `<`, a bogus gradient —
  > measured that **MiniMessage throws for none of them**; it renders an unparsed tag as its
  > own source text. The catch could never execute. **This was written one commit after the
  > marker-grep rule above was strengthened**, which is the evidence that the rule was
  > mis-aimed rather than ignored.
  >
  > The only thing that catches this is mutation discipline pointed at the *guard* instead
  > of at the code: **write the test that makes the guard fire, and watch it fire.** That
  > test went red, which is the sole reason the dead catch was found rather than shipped
  > with a javadoc asserting protection that did not exist.
  >
  > So, for any guard whose failure path has never been observed: **feed it the bad input on
  > purpose.** A `catch` around a library call especially — leniency is a library's default
  > far more often than anyone assumes, and it is never stated where you are looking.
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
- **Report the FILE LIST from `git diff --numstat` AND from what you touched, and RECONCILE THEM.**
  **A row in one and not the other is the interesting one.** Account for every row, in both
  directions. The two diverge exactly when something interesting happened — a file you edited
  because the change falsified something in it, a field that moved from the reviewed plan, a file
  that was never in git at all. **A report that reads as complete and is not is how `master` gains a
  change with no record of why.** Say when a reviewed value changed even where the reason is good:
  the operator should not have to diff to learn what you did.

  > **NEITHER LIST IS THE AUTHORITY, AND THE FIRST DRAFT OF THIS RULE NAMED THE WRONG ONE.** It said
  > *report from `--numstat`, **not** from memory* — and applied literally that hides an **untracked**
  > file, which `--numstat` cannot see **by design**. **Memory alone DRIFTS** (it reports the plan
  > instead of the work); **`--numstat` alone has BLIND SPOTS** (untracked files, anything outside the
  > range you diffed). **The disagreement is the signal.**
  >
  > Found 2026-09-10 by the rule's own second application: `PLAN-cursed-emerald.md` had been reviewed,
  > accepted, and edited by two chats, and had **never been committed** — and the four-row `--numstat`
  > report that omitted it read as complete. Full entry in `NEXT.md`.
  >
  > **Practically:** for a new or moved file use `git status --porcelain`, or
  > `git diff --cached --numstat --diff-filter=A` after staging, so an addition cannot hide inside a
  > list of modifications.

- **ANY FIGURE QUOTING THE SUITE IS RE-READ FROM THE FINAL VERIFY RUN, AFTER THE LAST FILE LANDS** —
  never from the run that was green when the paragraph was written. Quote the breakdown with it
  (`853 / 17 / 597`), so the total is checkable by addition rather than on trust.

  > **A FIGURE WRITTEN MID-COMMIT DESCRIBES THE TREE BEFORE THE COMMIT, AND NOTHING RE-CHECKS IT.**
  > `GATE-volley.md` opened with *"1462 tests"*. The suite was **1467**, and the five that made the
  > difference were `VolleyFixtureTest`'s — **shipped in the same commit as the file that undercounted
  > them** (`49e7fd4`). It was never true of any tree, and it stays plausible forever, so no later
  > edit exposes it.
  >
  > **Two routes to the same wrong number, and the remedy is not care:**
  >
  > | | route | when it was false |
  > |---|---|---|
  > | `8e8731b`'s `1403` | **fabricated** — typed fresh and simply wrong | always |
  > | `GATE-volley.md`'s `1462` | **falsified by its own commit** — correct when drafted | from the moment it was committed |
  >
  > Same wrong number, same kind of file, opposite causes. Care fixes the first; only re-reading
  > after the last file lands fixes the second. Third in the family is the `27`-for-`29` plan count —
  > *a figure taken from a glance at something adjacent to the answer.* Full entries in `NEXT.md`.

- **A PUSH IS A FACT ABOUT THE WIRE, AND ONLY `git ls-remote` OBSERVES IT.** *"Pushed"* and *"not
  pushed"* in a report are **both claims**. Report the command's output, not your belief about the
  command's output — the same rule as the `--numstat` one above, applied to the remote.

  > **A REPORT SAYING "UNVERIFIABLE" IS STILL A REPORT**, and it is the one that most reliably stops
  > the check from happening, because skipping feels free when there is said to be nothing to check.
  >
  > **2026-09-10.** A report said *"Not pushed — say the word"*; the next turn claimed the same
  > report *"showed it on the wire"*. It did not. The reply that caught it had itself skipped
  > `ls-remote`, **on the strength of the report it was about to correct.** Whether the commit was on
  > `origin` at that moment is now **permanently unresolvable**: `ls-remote` is a point-in-time read,
  > neither party recorded one, and a commit's existence today says nothing about when it arrived.
  > **Two careful parties, no evidence on either side, and none obtainable.**
  >
  > **Practically:** paste the `git ls-remote --heads origin` line for the ref, before and after.
  > One line in a report retires this class.

- **PROSE REACHING A COMMAND GOES THROUGH A FILE, NEVER THROUGH QUOTING.** `git commit -F <file>`,
  `gh pr create --body-file`, `gh pr merge --body-file`, a written file for anything else. Never
  `-m`, never `--body`, never a heredoc.

  > **THE FAILURE MODE IS THAT A COMPOUND COMMAND ABORTS AT PARSE TIME, SO EARLIER STEPS IN IT NEVER
  > RUN EITHER.** A commit message heredoc broke on an unbalanced quote at line 107; the `git add`
  > *in the same command* never executed, and the next `git commit -F` then found nothing staged.
  > **The loud failure is what saved it. The next one may abort something that looked like it
  > succeeded.**
  >
  > **`--body` was banned for one instance of this — a quote break truncating a PR body SILENTLY.**
  > The ban names too narrow a carrier: it is not `--body` that is dangerous, it is **any
  > shell-quoted path from prose to argument.** Heredocs and `-m` are the same hazard, and a PR body
  > was lost to an unescaped quote before either of the two instances in this session.
  >
  > Two occurrences is a convention forming by accident, so it is stated rather than left.


### EVERY FILTER AND EVERY SCRIPTED EDIT NEEDS A POSITIVE CONTROL

**Three instruments reported success without having checked anything, inside a single slice**
(2026-09-08, elements). Recorded together, because three instances of one shape is a pattern and
three notes in three places would be three anecdotes:

| instrument | what it reported | what was true |
|---|---|---|
| `perl -pi -e '…'` | exit 0, no output | file byte-identical; the *same* regex matched when piped to `perl -ne` |
| `… \| grep -E "error:"` | no output, so "compiled" printed | Maven prints `[ERROR]`, not `error:`. The tree could not compile |
| `mvn -q test-compile` behind that filter | `BUILD SUCCESS` printed by the script | a `*/` orphaned by a bad splice; compilation failed |

Two of the three were caught only by a **marker grep**. The third was caught only because a later,
unfiltered run failed — i.e. by luck of ordering, not by design.

**This is what happens when verification apparatus grows faster than its own controls.** Each of
these tools was *added* to make a check trustworthy, and each became a new way to be told a check
passed when it never ran — this file's own headline defect, one level up.

**The operational form, which generalises past these three:**

- **A scripted edit** must be followed by something that must be present if it worked — `grep` for a
  marker, or a measured line/byte delta (`before`/`after`, `git diff --numstat`). Zero-exit is not
  evidence. For a mutation, assert **both** directions: the marker landed **and** the original is
  gone.
- **A grep filter over tool output** must be proven capable of matching a failure *before* its
  silence is read as success. Run it once against a known-bad input and require the hit. A filter
  that has only ever been run against passing output has never been tested.
- **Never let a filtered command decide an outcome.** `cmd | grep X; echo ok` prints `ok` whatever
  happened — the exit status belongs to `echo`. Check the command's own status, or print the
  unfiltered tail.

The rule underneath all three: **silence is not a result.** An instrument that outputs nothing has
either found nothing or done nothing, and those are the same picture.


### THE FIVE WAYS A MUTATION LIES, AND EACH GUARD IS BLIND TO THE NEXT

The first three were hit in one slice (2026-09-08, elements); the fourth and fifth arrived on
2026-09-09, Ignite — **from two different mechanisms, one commit apart**, which is the evidence that
this is a family and not a run of bad luck. They are one table because the shape only becomes visible
together: **each guard catches the previous failure and cannot see the one below it.**

| failure | what happened | what catches it |
|---|---|---|
| **didn't apply** | `perl -i` exited 0 and left the file byte-identical | the marker grep, **"marker present"** half |
| **applied to PROSE, reported as applied to CODE** | the target string appeared in **both a javadoc and the code it describes**, and `perl`'s non-global `s///` replaced the *comment* — the one that came first in the file. Marker present, code untouched | the marker grep, **"original gone" half — AND ONLY THAT HALF** |
| **THE MARKER BROKE THE EDIT** | the replacement text was `/* MUT_MARK */`, and its slashes **terminated `perl`'s `s///` early** — so the edit landed as a bare *deletion* and the marker never went in. Original gone, marker absent | the marker grep, **"marker present" half — the other one** |
| **applied, no bite** | the edit landed and the test stayed green — the assertion matched a *duplicate* of the mutated token | **nothing mechanical** — only reading the red you expected and not getting it |
| **applied, wrong side** | the test passed on an accident (a floating-point coincidence; an undefended victim where `dealt == amount`) rather than on the thing it guards | **nothing at all** — only designing the fixture so the two values differ |

> **ROWS TWO AND THREE ARE WHY THE MARKER GREP IS TWO CHECKS, AND WHY YOU NEED BOTH HALVES.** They
> fail in opposite directions and each half catches exactly one of them:
>
> - *"Marker present"* passes cleanly on a **misplaced** application — the marker really is in the
>   file. Only *"original gone"* notices the code still says what it always said.
> - *"Original gone"* passes cleanly when **the marker itself broke the edit** — the target really is
>   gone, replaced by nothing. Only *"marker present"* notices the edit is not the one you wrote.
>
> A one-directional grep reports one of these as a verified mutation, and in both cases the test run
> beneath it is green or red for reasons that have nothing to do with your hypothesis.
>
> **AND A LUCKY RESULT FROM A BROKEN INSTRUMENT IS STILL NOT EVIDENCE.** In the third row's real
> instance the accidental deletion *happened to equal* the intended mutation, so the reddening was
> correct — and it was discarded and re-run anyway. **That is the whole discipline: a right answer
> from an uncontrolled instrument is the same reading you would get from a broken one.** Do not keep
> it because it looks right.
>
> **Practically:** never put `/`, or the delimiter you are using, inside the replacement text. Use
> `s{...}{...}`, and prefer a marker with no punctuation at all.
>
> **This repo makes the shape common rather than rare.** Its javadocs quote their own constants and
> call sites constantly — `DefenseRule.APPLIES` appeared in a comment three lines above the
> `applyDamage` call it described — so **a mutation target that appears in both a comment and the
> code it documents is the normal case here, not an edge one.** Grep the target for its occurrence
> count before mutating, and mutate a string unique to the code (`CritState.NORMAL,
> DefenseRule.APPLIES, "fire"`, not `DefenseRule.APPLIES,`).

The marker grep proves the **edit landed**. It cannot prove the edit **reached what the assertion
reads**. So a green run after a confirmed-applied mutation is not a pass — it means the mutation was
too narrow, and it must be widened and re-run before anything is reported.

**`contains()` is the loosest common assertion form and therefore where a partial mutation hides.**
A message asserted with `contains("chevron")` survives a mutation that removes one of two
occurrences of "chevron" — which is exactly what happened here, and the first mutation was reported
as a failure rather than a verification because of it.

**So, when mutating to test a MESSAGE, do one of these two — and write down which:**

- **Mutate the WHOLE message**, not a clause of it. This is what was done here. It works because it
  cannot leave a surviving copy of any asserted token anywhere in the string.
- **Or assert on a token that appears EXACTLY ONCE** in the message, verified with `grep -c`. This
  works for the mirror-image reason: with one occurrence there is no duplicate for a partial
  mutation to hide behind, so any mutation touching the asserted fact necessarily removes it.

The first is safer when the message is being rewritten anyway; the second is better for a standing
assertion you expect to survive future edits, because it keeps the mutation small and local.

### AND THE ARM THAT MOST EARNS ITS KEEP IS THE ONE MOST LIKELY TO BE UNREACHABLE

The predictive form of the unreachable-guard rule above, and worth applying *before* writing a
guard rather than after.

A validation arm justified as *"this case would otherwise be silent"* is, by construction, guarding
a case **nobody has produced yet**. That is the same sentence as *"no shipped content reaches it"* —
so its only exercise is a test, and if that test asserts the arm EXISTS rather than causing the
condition, the arm is a dead catch with a green suite around it.

Worked example, `ContentValidator.validateElements`: the arm that warns when an element declares a
status which cannot accrue (`applies_status: rooted` — resolves perfectly, does nothing forever).
Every bundled element declares `scorch` or nothing, so **production cannot reach that arm at all.**

**So, for any guard whose triggering case does not exist in shipped content:**

- Write the test to **CAUSE the condition** — author the bad content, run the real walk, observe the
  warning by its text — never to assert the arm is present.
- **Say so in the arm's own javadoc:** that no bundled content reaches it, and its only exercise is
  that test. Otherwise the next reader assumes production covers it, which is how a guard stops
  being maintained while still looking load-bearing.


### THREE THINGS THAT DO NOT ANNOUNCE THEIR OWN ABSENCE

Everything else in a plan or a diff is noticed by someone who wanted it. These are not, and each
cost a slice to find.

**A MUTATION CONFINED TO AN UNREACHABLE BRANCH REDDENS EXACTLY THE ROW THAT KEEPS IT ALIVE.**
A mutation can only redden through the rows that exercise it. So a dead branch and the row covering
it justify each other — the branch makes the row pass, the row's red makes the branch look guarded —
and **the pair is self-sustaining while neither touches production.** It looks exactly like coverage.
The tell is a row whose fixture had to INVENT a state the system cannot produce; when you find one,
check the branch it covers before you fix the row.

**THE PLAN ITEMS THAT SILENTLY FAIL TO LAND ARE THE GUARDS.** Not a random sample — selection. A
missing feature is reported by the person who wanted it; a missing guard produces no symptom at all,
which is the same property that made it worth planning. Two went missing in one slice
(`ScorchStatus`'s monotone refresh, `ScorchSinkSignatureTest`) and both were invisible for exactly
the reason they were needed. **The remedy is not "plan less":** at the end of a slice, diff the plan's
NAMED ARTIFACTS against what exists on the branch — the same counting that gave `7 -> 7` on the
lambda sites and `12` on the fire damage sites. Two names, one grep.

**A FALSIFIED COMMENT MISLEADS A READER WHO CAN CHECK IT. A FALSIFIED FLAVOUR LINE MISLEADS A PLAYER
WHO CANNOT.** So the sweep for prose that outlived its mechanism covers `flavor:` and `description:`
in content, not only code comments. A developer can diff a comment against the code beside it; a
player has only the string. `emberblade.yml`'s *"Swing to cut; loose to burn"* became false the day
melee started accruing scorch, and nothing but a person reading it would ever have said so.

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
- the `check-absorbed.sh` verdict, in full, **including the positive-control line** — a verdict
  quoted without its control is a search that may have been blind

> **THE THIRD ITEM CANNOT GO IN THE BODY, AND THE FIRST DRAFT ASKED FOR IT ANYWAY.** It said to
> record *"the squash's own tree SHA, and that the two were EQUAL"*. **A commit message cannot
> contain its own tree SHA** — the tree is an input to the hash, and the message is part of the
> commit that names it. Nor can it contain the `ABSORBED` verdict, which is computed against the
> squash that does not exist yet. Both were discovered on the **first attempt to follow the
> convention**, one merge after it landed, by trying to write the body and finding two of its three
> required fields unwritable.
>
> **Recording the branch tree is what makes the equality checkable, and it is sufficient**, because
> the squash's own tree is free forever: `git rev-parse <squash>^{tree}`. A reader compares that to
> the tree written down here. The convention was asking the body to carry a number the reader can
> always recompute, at the price of a number they cannot — which is backwards.
>
> So: the **verdict goes in the PR conversation** (it survives the branch, is timestamped, and is
> where a reviewer looks), and the **body carries the tip and tree** that make it re-checkable. State
> in the body that the check ran and passed; do not paste a SHA into it that you had to invent to get
> there. **A convention that cannot be followed gets followed approximately**, which is worse than a
> narrower one that can.

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
