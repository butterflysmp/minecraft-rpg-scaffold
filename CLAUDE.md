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
  > rule itself under *Standing decisions*. **The quote is updated rather than left standing, because
  > an example that quotes a rule verbatim goes stale exactly when the rule moves** — and a worked
  > example of a *well-formed pointer* that misquotes its own pointer is the failure it is teaching
  > against. The example's point is untouched: the surviving half is still obeyable without the
  > account, which is the property being demonstrated.
- **The pointer names a SECTION, never a line.** `WeaponLoader`'s `cooldown_ticks` section, not
  `WeaponLoader.java:175`. A line citation is falsified by any insertion above it, silently and
  invisibly to every test — this repo carries an open finding measuring that blast radius at ~140
  sites, produced by exactly the other choice.

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
>
> **AND IT APPLIES TO EXPECTED VALUES, NOT ONLY TO FIXTURE INPUTS — WHICH IS THE COLLISION SWEEP,
> POINTED AT TEST DATA.** `content/`'s numbers are swept so a bare gate reading is unambiguous;
> **a test row's expected values need the same property, and for the same reason.** Two expected
> values that collide cannot detect a **transposition** between them — swap the two quantities in the
> code and the row still passes, because both readings are the same number.
>
> **2026-09-12, the elapsed figure.** One event at tick 100, read at 340, gives `window 0` against
> `elapsed 240`. Staged at a tick where they happened to agree, the row would have survived a
> window/elapsed swap — **and that swap is precisely what `MUTSWAP` was written to catch.** Choose the
> staging so no two quantities the row reads are equal, then a transposition has nowhere to hide.

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
- **A WRONG DIAGNOSIS OF A BLIND SPOT IS WORSE THAN THE BLIND SPOT.** The blind spot is silent; the
  diagnosis is **silent AND believed.** A wrong assertion goes red — **wrong advice about the next
  guard sends the next person to a value that passes while guarding nothing, and they leave
  satisfied.**

  > **COMPUTE THE CLASS, DO NOT CHARACTERISE IT.** *"The values that are blind"* is a set **with a
  > definition**, and a description that happens to fit the three examples in front of you **is not
  > that definition.**
  >
  > **2026-09-12.** Having found that all three measured cooldowns were blind to a `4 → 2` grid
  > mutation, the replacement guard was described as *"any cooldown that is not a multiple of 2"* —
  > plausible, and **wrong**: `11` and `15` are odd and blind. Computed, the grids disagree exactly
  > when the authored value is **≡ 1 or 2 (mod 4)**, and the three points are `≡ 3, 3, 0` — *which is
  > why they are blind*, a fact the characterisation missed entirely. **Anyone following the wrong
  > advice would have added a green test that guarded nothing.**
  >
  > **Practically:** enumerate the set over a range and read off the rule. Three examples are not a
  > class, and the cheapest way to find that out is a loop.

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
- **FOUR WAYS TO MANUFACTURE A FALSE ABSENCE, AND ALL FOUR END IN "IT ISN'T THERE".** A search that
  returns nothing, an instrument that is wrong, **a pattern that cannot match the text it is looking
  for**, and **a tool that normalises away the very thing being tested for** all produce the same
  sentence, and it is a *finding* — so it gets reported, acted on, and is much harder to retract than
  a wrong positive.

  > **THE FIRST THREE ARE FACTS ABOUT THE DOCUMENT; THE FOURTH IS A FACT ABOUT THE INSTRUMENT.** That
  > split is why the fourth is filed here rather than left as another search tip: the first three are
  > fixed by searching better, and **the fourth cannot be.**

  > **A GREP FOR YOUR OWN VOCABULARY IS NOT A SEARCH OF THE DOCUMENT.** You match the word **you**
  > would have written; the author wrote theirs. **Search by content — a number, an identifier, a
  > quoted figure — and treat a null result on a word you chose as NO RESULT AT ALL**, not as
  > evidence of absence.
  >
  > **2026-09-12, twice in one session.** A squash body was grepped for *"fencepost"*, returned
  > nothing, and was nearly reported as missing the defect. The defect is there, under *"Two commit
  > counts were wrong, both by exactly +1"* — the author's words, not the searcher's. The checkable
  > form was available and cheap: grep for `+1`, or for the SHA.
  >
  > **AND AN INSTRUMENT THAT MISREPORTS THE CHECKABLE FACTS IS NOT EVIDENCE ABOUT THE UNCHECKABLE
  > ONES — not even negative evidence.** Same day: a PR page read twice, both times claiming the PR
  > open and the branch present, **both refuted by `ls-remote`.** An instrument wrong about what you
  > *can* verify tells you nothing about what you cannot. **Say "unverified" and name the
  > instrument**, rather than converting its silence into a finding.
  >
  > **AND THE THIRD: A MULTI-WORD PATTERN CANNOT MATCH ACROSS A LINE WRAP.** `grep` is line-based, so
  > a phrase broken by a newline is invisible to a pattern containing it — **and prose files wrap at
  > 100 columns, so the longer and more distinctive your search phrase, the likelier it is split.**
  > The pattern is right, the text is there, and the tool is working perfectly.
  >
  > **2026-09-13.** `git grep "multiples of 8"` over `CLAUDE.md` found the rule and **neither of the
  > two places that quote it** — this file's own worked example breaks after *"multiples of"*, and
  > `HeldFireQuantisationPinTest`'s javadoc after *"if it may ever"*. **A citation sweep run on that
  > phrase would have updated the rule and left both copies stale**, which is the exact failure the
  > two-homes convention exists to prevent.
  >
  > **Practically: search ONE TOKEN, never a phrase.** `dual-wield` found all four sites. A
  > hyphenated word, an identifier, a number — anything that cannot be broken in half.
  >
  > **AND THE FOURTH, WHICH IS NOT A FACT ABOUT THE DOCUMENT AT ALL: THE TOOL NORMALISES THE THING
  > YOU ARE LOOKING FOR.** The first three are properties of the text — your word, the author's word,
  > a line wrap. **This one is a property of the INSTRUMENT, and no amount of better searching fixes
  > it.** The pattern is right, the text is right, and the tool silently removes the difference before
  > it ever compares.
  >
  > **2026-09-13.** `grep -c -v $'\r$'` over a CRLF `NEXT.md` reported **every line as lacking CRLF**
  > — `sed` and `grep` in Git Bash **strip CR before matching**, so a `\r$` test can never match on
  > any file. `cat -A` through a `sed` pipe lied the same way, printing `$` where the raw bytes held
  > `^M$`. **Two independent text tools, same normalisation, same false absence.**
  >
  > **THE RULE: WHEN THE THING YOU ARE TESTING FOR IS SOMETHING A TOOL IS ENTITLED TO NORMALISE, THE
  > TEST MUST GO THROUGH SOMETHING THAT CANNOT NORMALISE IT.** Line endings, trailing whitespace,
  > unicode form, case — all of these are things a text tool may quietly canonicalise, and all of
  > them are therefore invisible to a text tool.
  >
  > **Practically:** count bytes, not lines. `tr -cd '\r' | wc -c` against the line count settles
  > CRLF — **and it is right because `tr` cannot normalise, not because it is more thorough.** The
  > same reasoning picks the instrument for the others: `cmp`/`od -c` for whitespace, `git diff
  > --numstat` for a change you were told did not happen.
  >
  > > ### AND SAY WHAT THAT COUNT PROVES, BECAUSE IT IS NARROWER THAN IT LOOKS — `core.autocrlf` IS `true` HERE
  > >
  > > **`tr -cd '\r' | wc -c` measures the WORKING TREE. It says nothing about the history.** With
  > > `core.autocrlf=true` git normalises CRLF to LF on the way into the index, so **every committed
  > > blob in this repo is LF** and the CRLF is reconstructed on checkout. Measured 2026-09-14:
  > > `RpgCommand.java` is `CR=2091` in the working tree and `CR=0` in its own committed blob.
  > >
  > > **2026-09-14, THE INSTANCE, AND IT IS A CORRECTION TO A REPORT RATHER THAN A NEW FINDING.**
  > > `sed -i` in Git Bash silently rewrote `RpgCommand.java` from CRLF to LF while reporting
  > > nothing, and the byte count caught it. **It was then reported as a defect caught before it
  > > could pollute the commit — and that second half was wrong.** The blob would have been
  > > byte-identical either way; there was nothing to pollute.
  > >
  > > **The incident stands and the claim is narrowed, which is the point.** `sed -i` really does
  > > normalise, the byte count really is the only instrument that sees it, and a working tree in
  > > the wrong state really does mislead every later `tr` reading and any tool that cares. **What
  > > it does not do is protect the history, and crediting it with that RETIRES THE QUESTION** —
  > > the next person reads "line endings are checked" and stops looking for the check that would
  > > actually see a line-ending change land.
  > >
  > > **A CONTROL CREDITED WITH PROTECTING SOMETHING IT DOES NOT TOUCH IS WORSE THAN NO CONTROL**,
  > > and it fails the same way a stale load-bearing flag does: silently, and by discouraging the
  > > work it appears to have done.
  > >
  > > **What DOES see a line-ending change reaching history:** `git diff --numstat` showing a file
  > > wholly rewritten, or `git show <ref>:<path> | tr -cd '\r' | wc -c` — the blob, named as the
  > > blob. That is the *integrity figure must say what it hashed* rule, applied to line endings.

  > **Practically, for all four:** before reporting an absence, ask *what would this look like if it
  > were present and my search were wrong?* If the answer is "identical", the search is not done.
  > **For the fourth, add: is what I am testing for something this tool is allowed to throw away?**

- **AND THE INVERSE, WHICH IS WORSE: A FALSE PRESENCE. PROSE THAT NAMES A KEY IS INDISTINGUISHABLE
  FROM THE KEY.** The three above are false *absences*. This is a match that should not have
  happened, and the asymmetry is the whole reason it is filed separately:

  > **A FALSE ABSENCE PROMPTS A SECOND LOOK. A FALSE PRESENCE READS AS CONFIRMATION, AND NOBODY
  > RE-CHECKS A CONFIRMATION.** "It isn't there" is a claim you feel obliged to defend; "there it is"
  > closes the question.
  >
  > **And the base rate runs the wrong way.** **A document that EXPLAINS why a key is absent contains
  > that key more often than a document that simply has it** — the explanation has to name the thing
  > it is denying, usually more than once.
  >
  > **2026-09-13.** `grep -l "applies_status" content/elements/*.yml` returned `fire.yml` **and
  > `kinetic.yml`** — because `kinetic.yml`'s comment reads *"It declares no applies_status."* The
  > substring match found **the prose denying the key** and reported it as the key. Two shipped weapon
  > files assert the opposite in their own comments, which is what prompted the re-check;
  > `grep -n "^applies_status"` returns `fire.yml` alone.
  >
  > **Practically: anchor the match to the syntax, not the word.** `^key:` for YAML at column 0,
  > `^\s+key:` for nested. An unanchored grep searches the commentary as well as the content, and in
  > this repo the commentary outweighs the content by an order of magnitude.

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

  > **AN UNTRACKED FILE IS THE PUREST CASE OF THIS RULE, NOT AN EXCEPTION TO IT — AND
  > `|| true` IS WHAT TURNS THE FAILURE SILENT.** `git checkout -- <path>` on a file git has never
  > seen **cannot restore anything**; there is no version to restore. It fails loudly, and that loud
  > failure is the whole protection.
  >
  > **`|| true` DISCARDS EXACTLY THE REPORT THE RULE EXISTS TO PRESERVE.** `cmd || true` is not
  > "tolerate a harmless error" — it is *do not tell me whether this worked*, applied to the one step
  > whose working is the point. Same family as `cmd | grep X; echo ok`: the status belongs to the
  > wrong thing.
  >
  > **2026-09-14.** `MUT-SCANBLIND` was "restored" with `git checkout -- <new test file> || true`.
  > The file was untracked, nothing was restored, the step printed nothing, and **the mutation stayed
  > in the tree**. The suite was **GREEN** at that moment — the blinded scan found zero keys and the
  > row's other assertions still held — so nothing else would have caught it. **The marker grep did,
  > and only because it was run afterwards on a file the restore had silently skipped.**
  >
  > **Practically: never `|| true` a restore, and prefer `cp` from the scratchpad for every mutated
  > file rather than only the tracked ones.** `cp` works on both, needs no knowledge of what git
  > knows, and is the instrument the rule already names.
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

  > **AND THE FOURTH ROUTE, WHICH NO AMOUNT OF RE-READING REACHES: A FIGURE MAINTAINED BY DELTA.**
  > The three above are each wrong **once**, at a moment you can name. This one is wrong **forever
  > and by a growing amount**, because every edit updates it correctly and inherits the base.
  >
  > **SO: RECOUNT THE FIGURE FROM ITS SOURCE, NEVER ADJUST IT BY THE DELTA OF YOUR OWN CHANGE** —
  > and **write the command that produces it beside it**, so the next editor recounts instead of
  > adjusting. If you find yourself computing *"twenty-nine minus three"*, stop and run the count.
  >
  > > ### AND THAT REMEDY IS GENERAL. IT IS STATED HERE BECAUSE HERE IS WHERE IT IS INTRODUCED, NOT BECAUSE IT IS ABOUT COUNTING ROWS
  > >
  > > ***ANY MEASUREMENT RECORDED IN THIS FILE CARRIES THE COMMAND THAT REPRODUCES IT, OR IT IS A
  > > CLAIM RATHER THAN A MEASUREMENT.*** Row counts, byte counts, test counts, line deltas, kill
  > > sets, percentages — **the shape is "a number written next to an assertion", and nothing about
  > > the remedy depends on what is being counted.**
  > >
  > > **EVERY MEASURED FIGURE IN THIS FILE IS A FIGURE MAINTAINED BY DELTA WAITING TO HAPPEN.** It is
  > > right when written, nothing re-checks it, and the next reader has no way to tell a figure taken
  > > this week from one taken six months ago.
  > >
  > > **THE EVIDENCE THAT INDEXING IT BY *ROW COUNTS* WAS TOO NARROW IS TWO ENTRIES BELOW THIS ONE.**
  > > A CR-byte count was written there with no command, **went stale inside one PR** when a later
  > > change added lines to the file it measured, and was quoted as fact in the meantime. **The rule
  > > it needed was already on this page** — it was filed under counting rows, and nobody connects a
  > > byte count to that heading.
  > >
  > > **The cheap form, when no single command produces it:** name the tree or revision it was taken
  > > at, and the event that invalidates it. **A bare number is the one thing that must not appear.**
  >
  > **AND A FRACTION MUST SAY WHAT UNIT EACH SIDE COUNTS.** A numerator in sub-items over a
  > denominator in items renders as a perfectly plausible fraction. **Prefer NAMING the items to
  > counting them** where the list is short enough — a name carries its granularity and a number
  > does not.
  >
  > **This is not hypothetical and the instance is in this repo:** `GATE-nexus.md`'s status section,
  > which is **the account** — four commits, an inherited `−4`, a further `−2` added by the very
  > commit whose subject was fixing that line, and a `−1` in the denominator that survived because a
  > second error cancelled it. **Every one of those edits passed *add the parts up*.**

- **A PUSH NAMES ITS BRANCH.** `git push -u origin <branch>`, always. A bare `git push` resolves
  against whatever the current branch tracks, so it can land unreviewed work on `master`; the
  explicit form fails loudly instead of guessing.

- **A PUSH IS A FACT ABOUT THE WIRE, AND ONLY `git ls-remote` OBSERVES IT.** *"Pushed"* and *"not
  pushed"* in a report are **both claims**. Report the command's output, not your belief about the
  command's output — the same rule as the `--numstat` one above, applied to the remote.

  > **A WIRE LINE IS GENERATED, NEVER WRITTEN.** State it from `ls-remote`'s output **in the same
  > breath you run it**, the way a commit count is stated from `rev-list --count`. **A wire claim
  > composed from memory is stale BY CONSTRUCTION, because the only thing that changes it is the act
  > you are reporting.**
  >
  > **2026-09-13.** A report ended *"Nothing committed, nothing pushed — the wire is still
  > `5167c5d`."* It was true when written. The next instruction was *"get this on git"*, and the push
  > falsified it — **the same mechanism as `GATE-volley.md`'s `1462`**, a figure correct when drafted
  > and falsified by its own commit. The class is not carelessness; it is **a claim whose subject is
  > the action being taken**.

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

- **AN INTEGRITY FIGURE PROVES WHICH BYTES, NEVER THAT THEY ARE THE RIGHT ONES. READ THE DIFF.**
  The wire SHA, the tree SHA, the blob SHA and `--numstat` all answer *did what I think landed,
  land?* — and every one of them reconciles perfectly around a splice that ate the wrong lines.

  > **AND A HASH MUST SAY WHAT IT HASHED.** Working tree or blob, and with which line endings. **An
  > integrity figure whose subject is unstated cannot be reproduced by a second party, which is the
  > only thing an integrity figure is for.**
  >
  > **2026-09-12.** A report proved a mutated file restored by quoting `md5sum` of the **working
  > tree**. The reader compared it against the **blob** at `origin` and got a different value — with
  > no discrepancy in the content, because **the file is mixed CRLF/LF**. So in a report *about a
  > line-ending defect*, the integrity figure offered as proof was the one figure that defect makes
  > uncheckable from the remote.
  >
  > **Practically: prefer the absence.** The proof was already in the same report — the file was
  > **not listed by `git diff --numstat`**, and *a file absent from the diff is byte-identical by
  > construction*. **Cite the absence, not the hash**: it needs no subject, no encoding, and no
  > second party's trust.

  > **2026-09-11, `443c086`.** A `head`/`tail` splice into `GATE-quiver-a2.md` deleted its two
  > opening lines — **`**Status: NOT RUN.**` and the sentence recording that the rows were written
  > before any boot** — and pasted a later section's heading into the masthead in their place. The
  > commit reported `+77 −11` and every figure in the report agreed with every other: wire matched
  > local, tree matched, **blob hash matched**, and the deletion of the status line was simply one
  > of the eleven. *The eleven were correct.*
  >
  > **The field lost was the one the whole document exists to carry** — a gate that cannot say
  > whether it has been run, and cannot say its rows were not back-fitted to a run. It was found by
  > the operator OPENING THE FILE, which is the only thing that would have.
  >
  > **The tell that is available and was skipped: a splice's own output.** `head -n N ... ; cat >> ;
  > tail -n +M` is two line numbers, and a line number is stale the moment any earlier edit in the
  > same session shifts the file. After any splice, **print the region** — `sed -n '1,12p'` on the
  > head, or `git diff` on the hunk — before believing the marker grep or the byte delta. Both of
  > those were run here and both passed.
  >
  > **When a figure is given alongside its parts, add the parts up.** The offline case of the rule
  > above, and the cheapest: it needs no command at all.
  >
  > **2026-09-12.** The correction to a stale commit count was itself stale, because the correcting
  > commit was a new member of the set being counted. The page said *fourteen*, then *nine shown*
  > and *six not shown*, two clauses apart. **Adding a row and fixing the count in the same edit
  > changes the set the count is over.**
  >
  > **AND A RANGE IS EXCLUSIVE AT ITS BASE, WHICH IS WHERE THE OFF-BY-ONE LIVES.** State a commit
  > count as `git rev-list --count A..B`, or as the number of lines in the log you are about to
  > paste — **never as a number you formed by looking at the range.** `A..B` excludes `A`.
  >
  > **2026-09-12, measured across four reports: two were wrong, both by exactly +1.** The two that
  > were right were the two counted off the accompanying table; the two that were wrong were formed
  > from the range. **A repeated +1 is a mechanism, not a slip** — and in both failures the prose
  > number disagreed with the table printed directly beneath it, so *adding the parts up* would have
  > caught it with no command at all. Same family as *eight shots span seven intervals*.

- **A CONTROL CARRIED PAST ITS PRECONDITION STOPS BEING A CONTROL WITHOUT STOPPING BEING QUOTED.**
  A proof holds under conditions. **When the conditions lapse the proof becomes a habit, and a habit
  reads exactly like a proof** — same words, same confidence, no warning.

  > **2026-09-13.** One report proved nothing had been eaten by a splice with *"**0 deletions**
  > across all three files, so no splice ate anything."* Sound: a pure append cannot delete. **The
  > next change stopped being an append** — a recorded count in a heading had to go from THREE to
  > FOUR, and **a count in a heading cannot be raised by appending** — and the framing carried
  > forward unremarked. The `−5` was clean, but *"zero deletions"* had quietly become a sentence
  > about nothing.
  >
  > **The precondition failed SILENTLY, which is the whole difficulty.** Nothing announces that a
  > proof's premise has lapsed; the proof just keeps being available to quote.
  >
  > **Practically: state the precondition WITH the control, in one breath** — *"a pure append, so
  > zero deletions proves nothing was eaten"* — so the day it is not an append, the sentence is
  > visibly wrong instead of quietly empty. **And when there ARE deletions, read them**: `git diff
  > --cached | grep "^-"` costs nothing and answers the question the count no longer can.

- **AN EXIT STATUS PROVES A PROCESS ENDED, NEVER THAT IT DID ITS WORK. CHECK THE ARTEFACT, NOT THE
  INVOCATION.** When a command's job is to WRITE something — regenerate a golden file, emit a report,
  produce a jar — the thing to read is the file it was told to write, not the status it returned.

  > **2026-09-12.** A `golden-lore.txt` regenerate ran, did not fail, and **wrote nothing.** It was
  > caught by `git status` on the FILE, which showed the path unmodified in a commit that had just
  > added a weapon — a commit in which the golden could not possibly be unchanged. Nothing about the
  > invocation said so.
  >
  > **This is the third member of a family already on this page twice, and the three differ by where
  > the lie sits.** *Zero-exit is not evidence* below is this sentence scoped to a scripted EDIT.
  > **AN INTEGRITY FIGURE PROVES WHICH BYTES** above is it one layer in, where the file did change and
  > changed wrongly. This is the outermost case: **the file did not change at all — so every figure
  > computed over it is correct, reconciles perfectly, and is about the old content.**
  >
  > **The asymmetry that makes it quiet: a golden that was never regenerated still matches the code it
  > was last generated from.** The suite comparing them is green because both failed to move. A silent
  > no-op and a correct run are the same picture until someone else's diff.
  >
  > **Practically:** after any command whose output is a file, run `git status --porcelain <path>` —
  > and decide *before* running it which answer you expect. When you have just changed an input,
  > ***unmodified* is the alarming answer**, and it is the one that looks like nothing went wrong.

  > **A SECOND ROUTE TO THE SAME EMPTY GOLDEN, 2026-09-13 — AND THIS ONE FAILS WITH THE RIGHT EXIT
  > CODE.** The regenerating run is *designed* to throw, so **exit 1 is the success signal**. That
  > makes it indistinguishable from a run that never reached the module at all.
  >
  > **The cause, named exactly, because the wrong form is the one you will reach for:**
  > `-DfailIfNoSpecifiedTests=false` **is not a property surefire reads.** The name is
  > **`-Dsurefire.failIfNoSpecifiedTests=false`**. The wrong name silently does nothing, so
  > `-Dtest=GoldenLoreTest -am` aborts in `rpg-core` with *"No tests matching pattern"* — and
  > **`rpg-paper` is SKIPPED, so the module that writes the golden never runs.** Exit 1, no golden, and
  > the procedure itself supplies the plausible explanation for the 1.
  >
  > **The whole regenerate line, correct:**
  > `./mvnw -pl paper -am test -Dtest=GoldenLoreTest -Dsurefire.failIfNoSpecifiedTests=false -Dgolden.regenerate=true`
  >
  > **Read `REGENERATED` in the output AND `git status` on the file. Neither alone is enough here** —
  > the status is the artefact check, and the log line is what distinguishes "ran and wrote" from
  > "never ran".

  > **AND `-pl paper` WITHOUT `-am` COMPILES AGAINST WHATEVER WAS LAST INSTALLED.** Measured
  > 2026-09-13: the installed `rpg-core` jar was **five days stale** and predated `AccrualRule`, so a
  > paper-only build failed at the *imports* of six test files nobody had touched. **It reads as a
  > real break in unrelated code**, and the instinct is to go looking at those files.
  >
  > **The tell is that the failures are in files your change never went near, at import lines.** The
  > fix is `-am` (or a full-reactor `./mvnw test`), not an investigation.

- **AN ESTIMATE PLACED BESIDE MEASUREMENTS BECOMES ONE. PROXIMITY LAUNDERS IT.** A number you
  eyeballed, printed in a column of numbers you measured, is indistinguishable from them and inherits
  their authority. Either measure it too, or mark it as an estimate *in the same cell*.

  > **2026-09-12.** A held-changes table listed five files with their line deltas. Four came from
  > `git diff --numstat`. The fifth — `PLAN-boltor.md +23` — was eyeballed off the edit and never
  > measured; staging reported **`+21`**. **Nothing in the table marked which was which**, and it
  > surfaced only because the file was later committed and the real number printed itself.
  >
  > **The laundering runs one way and that is what makes it worth a rule.** The four measured rows
  > lent the fifth their credibility; the fifth did not visibly borrow anything. **Alone, the same
  > number would have been read as the guess it was.**
  >
  > **The correct handling is in the same session, one decision later**, and is the reason this is
  > stated as a rule rather than an apology: an open finding quoted three measured counts and then
  > **declined to estimate** what fraction were already stale, *because an estimate there would have
  > been indistinguishable from the three above it.* **Naming a probe someone can run beats supplying
  > a number nobody can check.**
  >
  > **Practically:** if a number is going next to measured numbers, run the command. If you cannot,
  > label it where it sits — `~20 (est.)` — never in a footnote the eye skips.

- **AND ITS SIBLING ON THE OTHER AXIS: DESCENT LAUNDERS. A NUMBER DERIVED FROM A PLACEHOLDER BECOMES
  A PRECEDENT.** Proximity launders across a *page*; descent launders across *time*. **Nobody
  re-decides an inherited figure** — they derive the next one from it.

  > **THE REASON IT IS INVISIBLE: EVERY INDIVIDUAL STEP IS HONEST.** Each generation is one defensible
  > derivation from the last, so **no step is the one where the error entered.** Auditing any single
  > link finds nothing wrong. The arbitrariness is in the root, and the root is usually gone by the
  > time anyone asks.
  >
  > **2026-09-12.** The Boltor's `attack_damage` was first anchored on `ironblade` — **a dev weapon,
  > not balanced meaningfully.** The operator ruled `19` **outright** instead, and the stated reason
  > was not "wrong anchor" but that **a comparison had been used as a source.** Had `19` been derived,
  > it would have become the Ranger tier's reference point, and `ironblade`'s arbitrariness would have
  > **outlived `ironblade`** — which is now scheduled for deletion.
  >
  > **Practically:** when you reach for a precedent, ask **what the precedent was itself derived
  > from**, and stop at the first number nobody ruled. **Never derive a new weapon's numbers from a
  > dev weapon's**; parity with a placeholder is parity with nothing. If a figure must be inherited,
  > say whose ruling it descends from, so the chain can be walked back.

- **A MEASUREMENT TAKEN FOR A LATER DECISION MUST CARRY THE REVISION IT WAS TAKEN AT AND THE EVENT
  THAT INVALIDATES IT.** **Age is not visible on a number.** The figure that is right today and
  quietly wrong on the day it is used is **indistinguishable from the figure that was always wrong —
  and it is read with more confidence, because someone measured it.**

  > **Split the measurement by how it ages, because the halves need different treatment.**
  > **DURABLE** facts are structural and are re-verified in one command; **PERISHABLE** facts are
  > true of one tree and must be **re-measured** before use. A perishable figure with no stated
  > expiry becomes a durable one by sitting still.
  >
  > **2026-09-12.** A deletion-impact measurement was taken at `2a3fb68` for a decision due days
  > later, against a tree that will have gained weapons by then. Its golden-file attribution —
  > `9 / 10 / 14` of `134` — has a **denominator that is a line count over a directory about to
  > grow**: wrong the moment the next weapon ships, and wrong silently. Its structural half (`core`
  > has no test resources, so no `core` test can load content) holds until someone adds a directory.
  > **Same entry, same day, two completely different shelf lives.**
  >
  > **Practically:** head the section with the revision and the invalidating condition, in those
  > words, and sort every figure under **DURABLE** or **PERISHABLE** before anyone has to guess. The
  > invalidator is usually an *event*, not a date — "the next weapon that ships" is checkable;
  > "probably stale by next week" is not.

- **A MEASUREMENT OUTLIVES ITS FIXTURE, BUT ONLY IF THE RECORD SAYS THE FIXTURE IS GONE.** Deleting
  the thing a reading was taken on does not falsify the reading — it makes the reading
  **unverifiable, and indistinguishable from a stale one.** Restate the reading before removing its
  instrument, or lose it.

  > **2026-09-12.** The input-quantisation model — `fire interval = ceil(effective / 4) x 4` — rests
  > on three measured points: `11 -> 12` on `quiver_stone`, `15 -> 16` on `hunters_bow`, `16 -> 16`
  > on the Boltor. **Two of the three sit on weapons in a pending deletion set**, and the survivor is
  > the one that cannot by itself distinguish the rule from a coincidence at 16.
  >
  > **The asymmetry is what makes this worth a rule.** A deleted fixture leaves the reading TRUE and
  > UNCHECKABLE. Nothing fails, no test reddens, no grep lists it — the next reader simply finds
  > citations to weapons that do not exist and **has no way to tell a preserved measurement from an
  > abandoned one.** That is the same picture as a stale figure, arrived at by an honest route.
  >
  > **Practically:** before deleting anything a reading was taken on, restate every reading in one
  > place, each carrying **the fixture it was taken on, the date, and the note that the fixture was
  > subsequently deleted.** The third field is the one that does the work; without it the first two
  > read as an oversight.

- **A FINDING THAT LIVES ONLY IN THE CONVERSATION IS NOT RECORDED.** A chat transcript is **not
  greppable by the person who will next touch the file**, does not survive the session, and **cannot
  fail.** If a finding is worth stating, it is worth a commit.

  > **AND THE MOMENT IT IS MOST LIKELY TO BE LOST IS EXACTLY THE MOMENT IT WAS FOUND** — after the
  > record it belonged in was already written. That is not bad luck; it is **selection**. Review is
  > when findings surface, and review happens *after* the document. So the findings most likely to go
  > unrecorded are systematically the ones produced by the process meant to catch them.
  >
  > **2026-09-12.** The `quiver_size: 1` blind value was found during merge review, after the PR body
  > was finalised. It could not reach the squash; the branch was then deleted, making that body the
  > only account of the work. **For about an hour the finding existed solely in a chat transcript**,
  > and it was recorded only because someone grepped `master` for it and noticed it was nowhere.
  >
  > **Practically:** when a finding arrives too late for the commit it belonged in, **it gets its own
  > commit — now, not "next time that file is touched".** A follow-up commit costs minutes; the same
  > note in a transcript costs the next person the whole rediscovery. And when the branch is already
  > merged, the follow-up is on a new branch, which is cheaper than it sounds and is what this rule
  > is for.

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


### A PREDICTION THAT SEVERAL OUTCOMES SATISFY IS NOT A CONTROL, IT IS A RANGE

**Predict the COUNTS, because counts are what the tool reports.** A shape stated in words gets checked
by eye against a figure stated in numbers, and the eye passes it.

> **2026-09-13.** A golden regeneration was predicted, in advance and in writing, as *"one ADDITION
> plus one MODIFICATION"* — the control existing because **a count cannot see a substitution**, and a
> rarity change had turned an addition-only diff into a mixed one. It reported **`13 / 2`**. The tick
> went in.
>
> **A modification costs one deletion and one insertion, so ONE modification yields ONE deletion. Two
> were reported.** The decomposition: 11 lines of new block, 1 new footer, **1 new `=== 91 renderings
> ===` tail** — against 2 deletions, the old footer and the old tail. **The golden was right; the
> control was loose enough that one modification and two both satisfied it**, and the reported figure
> distinguished them and was never used.
>
> **Practically:** write the expected `insertions / deletions` before running, then compare numbers to
> numbers. If you cannot predict the counts, you do not yet understand the change well enough to be
> checking it.

> **AND THE TRAP UNDERNEATH IT: A LINE THAT CARRIES A COUNT IS ITSELF A LINE.** The renderings tail was
> tracked all through the plan as *a number that moves* — `90 -> 91`, quoted in three places — and
> **forgotten as text that changes.** It was the second modified line, and the reason the prediction
> was one short.
>
> **Anything self-describing is both, and predictions about it have to be made twice**: a total, a
> version string, a generated-on stamp, a `=== N items ===` footer. Once as the value, once as the
> diff hunk.
>
> Same family as *eight shots span seven intervals* and the `A..B` commit counts — **the thing being
> counted and the thing doing the counting are not the same thing**, and it is always the second that
> gets dropped.

### TWO RULES FROM ONE REVIEW, AT THE SEAM BETWEEN A FIGURE AND THE SENTENCE ABOUT IT

**Filed together because they fail in opposite directions and either alone reads as a one-off.** Both
were produced by the same document revision on 2026-09-13.

- **A REVISION REGRESSES WHAT IT WAS NOT REVISING.** A draft stated a value correctly at
  `s > 12/4.5 = 2.6667`. The next draft, whose attention was on a table one section away, rewrote that
  **untouched** sentence into `s > 12/0.5, unreachable` — **wrong in the value and wrong in the
  conclusion, from a draft that had the right number and a sweep that had printed it.**

  > **Diff a revision against the draft it replaced, not only against the defect list.** The defect
  > list says what was wrong; it never says what was right and got touched anyway. **Prefer targeted
  > edits to rewrites**, for exactly this reason.

- **A GENERAL FORM MUST REPRODUCE THE WORKED VALUES IT SITS ABOVE.** The same revision generalised a
  verified closed form to `s > n/(m - 0.5)`. It is `m + 0.5`; the sign inverted while **pattern-
  matching the `3.5`** in the one-step form `n/(n - 3.5)` instead of rederiving
  `round(n/s) <= m  iff  n/s < m + 0.5`. It ran ~13% high at every step — **which does not read as an
  error, it reads as a weapon having more headroom than it has.**

  > **THE TELL WAS TOTAL, AND THAT IS THE DIAGNOSTIC.** The stated form disagreed with **every** row
  > printed beneath it. **A formula that is merely mis-stated usually matches somewhere by luck; one
  > that matches nowhere was never checked against its own data.** Substituting one row back costs
  > nothing.
  >
  > **And the values are not the portable artefact — the formula is.** The rows describe two weapons;
  > the formula is what the next author reaches for when pricing the third.

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
  marker, **AND** a measured line/byte delta **against the file as it was immediately before the
  edit** (`diff` against a scratchpad copy). Zero-exit is not evidence. For a mutation, assert
  **both** directions: the marker landed **and** the original is gone.

  > **THOSE TWO ARE NOT ALTERNATIVES, AND THIS USED TO SAY "OR".** Measured 2026-09-10: a
  > `perl -i -pe 's{A}g; s{B}g if $. >= L && $. <= L+35'` rewrote **forty other sites** while
  > **both halves of the marker grep passed** — marker present at the target, original gone at the
  > target. **The grep proves an edit LANDED and says nothing about WHERE ELSE.** Only a line/byte
  > delta sees the overreach. See the sixth row of the mutation-lies table below.

  > ### AND THE DELTA MUST BE TAKEN AGAINST THE RIGHT BASELINE — THIS BULLET SAID `git diff --numstat` UNTIL 2026-09-15
  >
  > **`--numstat`'s baseline is HEAD, and in a mutation pass HEAD ALREADY HOLDS THE SLICE.** So it
  > reports **the slice's** delta, not the mutation's, and **returns the same number whether the
  > mutation touched one line or forty** — which is precisely the discrimination the sixth row of
  > the table exists to make. **A prescribed instrument that cannot distinguish the thing it exists
  > to detect**, in the file people follow instead of remembering around.
  >
  > **2026-09-15, the instance.** `MUTSTATS19` changed **one line** in `NexusMenuLayout.java`, a
  > file that slice had already added 20 lines to. `git diff --numstat` printed **`20  0`** — the
  > slice's own addition — and would have printed `20  0` for a mutation that rewrote the whole
  > file. It read as a clean pass.
  >
  > **The correct baseline is the file immediately before the mutation, which is the scratchpad copy
  > the restore rule already requires you to make.** It costs nothing extra:
  >
  > ```bash
  > cp "$F" "$SCRATCH/$(basename $F).orig"        # the restore copy, per the never-checkout rule
  > # ... apply the mutation ...
  > diff "$SCRATCH/$(basename $F).orig" "$F" | grep -c '^<'   # must equal the lines you meant to change
  > ```
  >
  > Reported `1` for every one of eight mutations in that pass, including the one `--numstat` had
  > called `20  0`.
  >
  > **`--numstat` IS STILL CORRECT WHERE IT IS PRESCRIBED ELSEWHERE ON THIS PAGE** — the file-list
  > reconciliation under *Report the FILE LIST*, and the byte-identity argument under *AN INTEGRITY
  > FIGURE*. **Those want the HEAD baseline; a mutation does not.** Same command, two baselines, and
  > only one of them is HEAD. **Do not "fix" the other sites.**
- **A grep filter over tool output** must be proven capable of matching a failure *before* its
  silence is read as success. Run it once against a known-bad input and require the hit. A filter
  that has only ever been run against passing output has never been tested.
- **Never let a filtered command decide an outcome.** `cmd | grep X; echo ok` prints `ok` whatever
  happened — the exit status belongs to `echo`. Check the command's own status, or print the
  unfiltered tail.

The rule underneath all three: **silence is not a result.** An instrument that outputs nothing has
either found nothing or done nothing, and those are the same picture.


### THE EIGHT WAYS A MUTATION LIES, AND EACH GUARD IS BLIND TO THE NEXT

The first three were hit in one slice (2026-09-08, elements); the fourth and fifth arrived on
2026-09-09, Ignite — **from two different mechanisms, one commit apart**, which is the evidence that
this is a family and not a run of bad luck. The sixth followed on 2026-09-10, the seventh on
2026-09-12 and the eighth on 2026-09-16. They are one table because the shape only becomes visible
together: **each guard catches the previous failure and cannot see the one below it.**

**The table has THREE parts, and they need three different responses:**

- **Rows one to six are BROKEN EDITS** — the mutation did not do what was written. **Response: fix
  the edit and re-run it.**
- **The seventh is not broken at all** — the edit is perfect and the CONCLUSION drawn from it is too
  broad. **Response: run more mutations.** No amount of care with `perl` reaches it.
- **The eighth is neither** — the edit is perfect AND the conclusion would be sound, but **the
  INSTRUMENT does not apply to this shape of mutation.** **Response: use a different instrument.**
  Care with `perl` does not reach it either, and neither does running more mutations.

| failure | what happened | what catches it |
|---|---|---|
| **didn't apply** | `perl -i` exited 0 and left the file byte-identical | the marker grep, **"marker present"** half |
| **applied to PROSE, reported as applied to CODE** | the target string appeared in **both a javadoc and the code it describes**, and `perl`'s non-global `s///` replaced the *comment* — the one that came first in the file. Marker present, code untouched | the marker grep, **"original gone" half — AND ONLY THAT HALF** |
| **THE MARKER BROKE THE EDIT** | the replacement text was `/* MUT_MARK */`, and its slashes **terminated `perl`'s `s///` early** — so the edit landed as a bare *deletion* and the marker never went in. Original gone, marker absent | the marker grep, **"marker present" half — the other one** |
| **applied, no bite** | the edit landed and the test stayed green — the assertion matched a *duplicate* of the mutated token | **nothing mechanical** — only reading the red you expected and not getting it |
| **applied, wrong side** | the test passed on an accident (a floating-point coincidence; an undefended victim where `dealt == amount`) rather than on the thing it guards | **nothing at all** — only designing the fixture so the two values differ |
| **APPLIED TOO WIDELY** | a scope guard that **silently did not bind**: `perl -i -pe 's{A}g; s{B}g if $. >= L && $. <= L+35'` — **the `if` binds ONLY to the last statement in the chain**, so `s{A}` ran over the whole file | **NOTHING in the marker grep — BOTH halves pass.** Only a measured line/byte delta **against a pristine copy of the file taken before the edit** sees it — **NOT `git diff --numstat`**, whose baseline is HEAD and which therefore reports the slice rather than the mutation. Account in the *scripted edit* bullet above |
| **APPLIED, BIT, AND CERTIFIED ONE AXIS OF TWO** | the expression had **more than one degree of freedom** and the mutation moved one. `now - lastTick` names two: *which endpoint* (first/last) and *which reference* (now/last event). One splice reddened rows and proved only its own axis | **nothing mechanical, and the RED makes it worse** — only counting the axes in the expression before counting the mutations |
| **THE MUTATION WAS A DELETION, SO THE MARKER GREP DOES NOT APPLY** | the replacement text was a **SUBSTRING of the search text** — the mutation removed a line rather than changing one. Both halves of the marker grep then count *the same surviving text*: measured, `marker present: 3` and `original gone: 5` where a working edit gives `1` and `0`. **Not wrong — UNINTERPRETABLE** | **the marker grep cannot, by construction.** Only a measured line delta against a **pristine copy taken before the edit** |

> **THE SIXTH ROW IS THE MIRROR IMAGE OF THE FIRST, AND ONE INSTRUMENT CANNOT COVER BOTH.**
> *Didn't apply* is **too little**; *applied too widely* is **too much**. The marker grep sees the
> first and is **blind** to the second, because at the target site both halves read exactly as they
> would on a correct edit.
>
> **The generalisation: a scoped edit whose SCOPE silently does not apply is the same family as an
> in-place edit that NO-OPS.** Both report success; both are invisible to the check written for the
> other.
>
> **2026-09-10, the instance.** Restaging one test row rewrote `new Vec3(1, 0, 0)` across the whole
> file — `FORWARD`, `EYE_FORWARD`, unrelated victim fixtures. **Three unrelated tests failed and the
> intended row never reported.** It was caught **only because those failures were obviously wrong**
> — luck of the fixture, the same *"by luck of ordering, not by design"* that caught the third
> instrument in the table above. Restored byte-identical from a scratchpad copy and redone with an
> editor on the specific lines.

> **THE SEVENTH ROW IS ONE OF TWO WHERE THE MUTATION WORKED PERFECTLY, AND THAT IS WHAT MAKES IT
> HARD TO SEE.** (It said *"the ONLY one"* until the eighth row was added on 2026-09-16 — **a claim
> that was true of the table and falsified by growing it**, which is this page's own ordinal rule
> biting the page itself. The eighth is the other; they differ in whether the flaw is in the
> CONCLUSION or in the INSTRUMENT.) Rows one to six are broken edits. Here the edit applies, is
> scoped correctly, bites,
> and reddens exactly the row written for it. **It is a partial test wearing a complete one's
> colour** — and it is the most convincing kind, because a red result reads as proof and the report
> carries a number.
>
> **THE GENERAL FORM: A MUTATION MOVES ONE AXIS OF AN EXPRESSION THAT HAS MORE THAN ONE.**
>
> ```
> sinceLastEventTicks = currentTick - running.lastTick     TWO degrees of freedom:
>   which endpoint    first vs last        <- one splice moves this
>   which reference   now   vs last event  <- and says nothing about this
> ```
>
> **2026-09-12, the elapsed figure.** `MUTELAPSED` (last → first) reddened **1 of 12**. Reported
> alone it would have read as a verified mutation. `MUTSWAP` (now → last event) then reddened **3 of
> 12** — and **two of those three rows are invisible to `MUTELAPSED` by construction**, because they
> stage a single event, where `firstTick == lastTick` and the two implementations are numerically
> identical. One splice could not have reached them at any fixture.
>
> **THE TELL, AND IT COSTS NOTHING: COUNT THE AXES BEFORE COUNTING THE MUTATIONS.** If the expression
> names two quantities and a relation between them, one splice cannot certify all of it. This is the
> neighbour of *a control that succeeds for the wrong reason* and is **not** that: this control
> succeeds for the **right** reason, over too small a set.

> ### THE EIGHTH ROW IS NOT THE FIRST ONE IN A NEW COSTUME, AND THE RESPONSES ARE WHY IT IS SEPARATE
>
> **The first row is a mutation that DID NOT APPLY. The eighth is a mutation that applied
> PERFECTLY while the instrument checking it was inapplicable by construction.** They look similar
> in the terminal — a marker grep whose numbers are not `1` and `0` — and they call for opposite
> actions:
>
> ```
> row one     the numbers are 0 and 1     the EDIT is broken        -> fix the edit, re-run
> row eight   the numbers are 3 and 5     the INSTRUMENT is wrong   -> different instrument
> ```
>
> **THE ASSUMPTION THE MARKER GREP RESTS ON, NAMED BECAUSE IT IS NEVER STATED: the marker is
> DISTINCT FROM THE ORIGINAL.** "Marker present, original gone" is only a biconditional when the
> two texts do not overlap. **A deletion violates that rather than failing it** — the new text is
> contained in the old, so both halves count the same surviving characters and neither means what
> it says.
>
> **2026-09-16, the instance.** `MUT4B-NOCACHE` deleted one line from a two-line block. `marker
> present: 3`, `original gone: 5`. Neither number is a bug in the edit; both are the greps matching
> fragments of text that is legitimately still there. The line delta against a scratchpad copy read
> **`0`**, correctly, and that is what caught it.
>
> **Practically: before mutating, ask whether your replacement is a SUBSTRING of what it replaces.**
> If it is, the mutation is a deletion, the marker grep is off the table, and the line delta is the
> whole verification. Prefer deleting a line by **splicing it out and printing the region** over
> trying to make a grep meaningful about text that did not move.
>
> **The same session produced a row-ONE failure too**, one mutation apart, which is what made the
> distinction visible: `MUT4B-WRITE-ON-FAIL` used `perl`'s `!` delimiter against a pattern
> containing `!loading.isDone()` and closed early — an ordinary broken edit, fixed by re-running it
> with brace delimiters. **Two failures, adjacent, same terminal output shape, opposite remedies.**

> **AND THE COROLLARY OF THE SEVENTH ROW: A ROW CAN BE THE ONLY GUARD OF SOMETHING IT DOES NOT
> MENTION.** Counting axes tells you how many mutations to run. This tells you how to read the
> results. **Coverage is a property of what a mutation KILLS, not of what a test is NAMED AFTER** —
> so the map from row to guarantee has to be measured, and it is routinely not what the names suggest.
>
> **2026-09-12, `HeldFireQuantisationPinTest`.** Four mutations over three rows:
>
> ```
> MUT-COOLDOWN  fixture re-authored     -> row 2 only
> MUT-DELETE    fixture removed         -> row 2 only      two mutations, ONE axis
> MUT-CEIL      ceil -> floor           -> rows 1 and 3
> MUT-GRID      INPUT_FLOOR_TICKS 4->2  -> ROW 3 ONLY
> ```
>
> **The constant's sole guard is the row documented as a balance consequence that "asserts no
> opinion".** Rows 1 and 2 are blind to it: a 4-grid and a 2-grid disagree only where the authored
> value is ≡ 1 or 2 (mod 4), and all three measured points are ≡ 3 or 0. **Rule the balance question
> and the mechanism loses its protection, with nothing going red.**
>
> **Practically:** when a mutation kills fewer rows than you expected, do not move on — **ask which
> row is now the only thing holding that behaviour, and say so in that row's own javadoc.** A test
> that quietly became load-bearing for something outside its name is deleted by the next person
> tidying up.

> **AND THE INVERSE, WHICH THE SAME MATRIX EXPOSED: MUTATION COVERAGE MEASURES GUARDING. SOME ROWS
> EXIST TO RECORD.** **A row with no unique kill is not thereby removable.** The question is **what
> is lost if it goes**, not what it catches.
>
> **A coverage metric cannot see provenance.** So a row whose job is to make a measurement executable
> will always look redundant to it — and **will look most redundant exactly when its fixtures are
> about to disappear**, which is the moment it is most needed.
>
> **2026-09-12, same file, opposite failure.** In `HeldFireQuantisationPinTest`, **every mutation that
> reddens row 1 also reddens row 3**, so row 1 has *no unique kill at all* and a sweep would mark it
> removable. Row 1 is the row holding the three measured readings — the provenance, and the half that
> cannot be re-derived once the fixtures are deleted.
>
> ```
> row 3   sole killer MUT-GRID   reads as "a balance consequence"     IS the only guard of a constant
> row 1   NO unique killer       reads as "redundant, prune it"       IS the only record of a reading
> ```
>
> **The two are a pair and they fail in opposite directions: a guard nobody would name as one, and a
> record that reads as a redundant guard.** Both are invisible to the framing people use when pruning
> tests. **Mark each in its own row**, not only in the class header — the person deleting a row is
> reading the row.

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


### A HOLLOW FIXTURE — THE POINTER. THE ACCOUNTS ARE THE INSTANCES LISTED BENEATH IT

**A FIXTURE IS HOLLOW WHEN IT NEVER PRESENTS THE CONDITION THE ROW CLAIMS TO TEST**, and the
condition can fail to arrive in **SPACE, TIME, KIND, or OBSERVABILITY**.

| arm | the condition… | worked instance |
|---|---|---|
| **KIND** | arrives, but it is a *different* condition | `#92` — two in one file |
| **OBSERVABILITY** | arrives correctly, and the row then claims something **no instrument in the room can read** | `#93` — 5d's deleted star, one of two byte-identical stacks |
| **SPACE**, **TIME** | named for completeness | **none recorded.** Say so; do not invent one |

**The last row is the rule applied to itself.** A taxonomy with two proven arms and two
named-but-unwitnessed ones is an honest record; four arms each with a plausible-sounding example is
the defect this rule is about, wearing the rule's own clothes.

> ### A HOLLOW *MEASUREMENT* — KIND, AT A BOUNDARY THE ARMS HAD NOT BEEN POINTED AT: THE INSTRUMENT
>
> The condition can fail to arrive **at the SHELL, before the tool ever runs.** Same arm — a
> different condition arrived — but the fixture is fine and it is the *instrument* that was never
> the one you named.
>
> **2026-09-15.** `grep -c $'\x00' <file>` was run to count NUL bytes and reported **225**. **Bash
> cannot carry a NUL in a word: `$'\x00'` expands to ZERO BYTES**, so what executed was
> `grep -c ''` — an empty pattern matching every line. Measured, both forms return the identical
> `225`. The file's true NUL count is **1** (`tr -cd '\0'`, `perl -0777` and `od` all agree).
>
> **Three things let it survive, and they are the reusable part:**
>
> - **The shell swallowed the pattern SILENTLY** — no error, no empty-pattern warning.
> - **The number was PLAUSIBLE for the quantity claimed.** A wrong figure needs nothing else to
>   survive a reading.
> - **The command printed its own interpretation beside its output**, so the reading and the label
>   arrived together and appeared to confirm one another. **ONE COMMAND CANNOT BE ITS OWN CONTROL.**
>
> **AND "SAY WHICH INSTRUMENT" DOES NOT COVER THIS, WHICH IS WHY IT IS FILED HERE RATHER THAN THERE.**
> The instrument *was* named. What was named is the instrument the author **thought they had run** —
> and the rule cannot tell the two apart, because both produce the same sentence.
>
> **The fix is one line: a command that measures a byte must be shown able to EXPRESS that byte.**
> `printf '%s' $'\x00' | wc -c` returns **0** and settles it in one keystroke.
>
> > **SECOND INSTANCE, 2026-09-16, AND IT IS THE SAME SHAPE WITH THE SWEEP AS THE INSTRUMENT:
> > A SWEEP OF THE ENUMERATED VALUES CANNOT SEE THE DEFAULT, BECAUSE THE DEFAULT IS NOT ONE OF
> > THEM.**
> >
> > A selection mark was to be a glint, which is only unambiguous if no icon glints already. **Ten
> > content files were swept for `icon:` and the scheme reported clear — correctly.** The eleventh
> > material has no `icon:` line to find: it is a **fallback in Java**, reached by a definition that
> > is null or names nothing, and it is `ENCHANTED_BOOK`, which **glints by material**.
> >
> > **The instrument was in the wrong FILE TYPE, not merely pointed at too few files** — which is
> > why "sweep harder" would not have found it and why this is the instrument arm rather than an
> > incomplete enumeration.
> >
> > **AND THE GATE ROW WRITTEN TO CATCH THE NEIGHBOURING DEFECT STAGES THIS ONE.** Row 23 boots a
> > **misspelled** `icon:` to prove the loader names it — which is exactly the input that reaches
> > the fallback. The row would have rendered a permanently glinting candidate on the one screen
> > where glint means *selected*: **a row exercising its own defect while testing something else.**
> >
> > **Practically: when a scheme depends on a property holding for every value, sweep the AUTHORED
> > values and then go and read the DEFAULT.** They live in different files, and only one of them
> > has a grep.
>
> > **Where `225` actually comes from, since the plausible explanation is also wrong.** It is not
> > `wc -l` plus an unterminated final line — the file **is** newline-terminated (`tail -c 1` is
> > `\n`) and `wc -l` is **224**. Controlled: delete the single NUL and `grep -c ''` drops to
> > **224**; `grep -ac ''` on the original is **224** too. **The `+1` is a BINARY-MODE artefact,
> > supplied by the very byte being counted.**
> >
> > > **AND IT IS NOT VERSION-SCOPED — THIS ENTRY SAID *"GNU grep 3.0; scope it to that version"*
> > > UNTIL 2026-09-15, AND THAT WAS THE VERSION-SCOPING RULE FAILING IN MIRROR.** Measured on
> > > **3.11** (operator's reading, see below): `grep -c ''` **225**, `grep -ac ''` **224**,
> > > `wc -l` **224**. **The artefact reproduces exactly. Both builds have it.**
> > >
> > > **TWO DIFFERENT BEHAVIOURS WERE COLLAPSED INTO ONE SCOPE, AND ONLY ONE OF THEM IS
> > > VERSION-DEPENDENT:**
> > >
> > > | behaviour | version-dependent? |
> > > |---|---|
> > > | the `-c` **COUNT** returning `225` against `wc -l`'s `224` | **NO** — 3.0 and 3.11 both |
> > > | the `Binary file … matches` **NOTICE** on stdout when piped | **YES** — 3.0 narrates it, 3.11 suppresses it. This is what produced the `995` / `994` split below |
> > >
> > > **THE CONFLATION IS VISIBLE ON ONE BUILD, WITHOUT THE SECOND READING — WHICH IS WHY IT SHOULD
> > > HAVE BEEN CAUGHT WHEN IT WAS WRITTEN.** On **3.0** alone: `grep -c '' <file> | wc -l` is
> > > **1**, and that one line is `225`. **`-c` emits NO notice**, so the `+1` cannot be a narrated
> > > line; it is inside the count. Separately, and on the same build, `grep '' <file> | wc -l` is
> > > **1** — the notice, suppressing all 224 real lines — against `grep -a '' <file> | wc -l` at
> > > **224**. **Two distinct mechanisms, both demonstrable on one grep.**
> > >
> > > **OVER-SCOPING IS THE MIRROR OF THE RULE BELOW AND IT FAILS WORSE, BY THE FALSE-PRESENCE
> > > ASYMMETRY.** *Scope every version-dependent claim to its version* guards against a property of
> > > ONE build stated as a property of the tool — an **UNDER**-scoped claim, which a reader on
> > > another build may test and disprove. **An OVER-scoped claim is a FALSE ABSENCE**: it tells the
> > > 3.11 reader *this does not concern you*, and **nobody re-checks a claim that has excused
> > > them.** Same distinction as the ordinal rule under *Squash-merge bodies*, on a different axis.
> > >
> > > **So the instruction is BOTH DIRECTIONS: scope a claim to the builds you MEASURED, and to no
> > > fewer.** *"Measured on 3.0 and 3.11"* is a different sentence from *"3.0-specific"*, and the
> > > second is a claim about 3.11 that nobody made.
> > >
> > > **PROVENANCE, because this entry is about instruments lying: the `3.11` readings are the
> > > OPERATOR'S, taken on their shell.** Only GNU grep **3.0** is reachable from this working
> > > environment — `grep`, `/usr/bin/grep` and `/bin/grep` are all 3.0, and there is no WSL — so
> > > the 3.11 column above is **not independently reproduced here** and is named as theirs rather
> > > than absorbed into the measured set. The 3.0 column, and the whole two-mechanism separation,
> > > are reproduced.

**The accounts, by section — the concept is old here, only the enumeration was homeless:**

- `NEXT.md`, *A RULE AND ITS IMPLEMENTATION AGREE ON THE CASES SOMEONE CHECKED* — *"a hollow row,
  prescribed by a note about hollow rows"*, and the same finding again under *DEFERRED — PER-CAUSE
  AMOUNT RULES*.
- `NEXT.md`, *OPEN FINDING — THE INLINE TWO-MAP MERGE IS A KNOWN-HOLLOW GUARD* — still open.
- `GATE-ignite.md`, *FIVE ROWS CANNOT ACCEPT A BLANKET* — *"a hollow I7 does not merely lose its own
  coverage; it withdraws the licence from all eleven."*
- The `#92` and `#93` squash bodies carry the two arms above.

> **WHY THIS ENTRY EXISTS AT ALL, AND IT IS THE GENERALISABLE PART.** The four arms were cited across
> two reviews as *"this repo's own taxonomy"* and were **in no file in this repo** — measured,
> `CLAUDE.md` contained no occurrence of *hollow*. They came from a handoff document: a chat message.
>
> **A RULE EVERYONE FOLLOWS AND NO FILE STATES IS ONE CONTEXT LOSS AWAY FROM BEING GONE**, and this
> project has now lost context once. It was followed correctly the whole time, by convention, carried
> in conversation — **invisible to anything that greps.**
>
> **Second instance, so it is a pattern and not an accident:** the explicit-branch push rule shipped
> as `#91`, having been owed for sessions on exactly the same terms.

### THE CREATIVE-DIVERGENCE REGISTER — THE BENCH IS NOT THE GAME

**WE TEST IN CREATIVE AND WE SHIP TO SURVIVAL, SO EVERY CREATIVE READING CERTIFIES CREATIVE.** This
is a **POINTER**, and its one job is to answer *"can this row be read in creative?"* **before** a boot
is spent finding out. Each entry names the divergence and where the account lives.

**BLANKET PARITY IS NOT ON OFFER AND IS NOT BEING PROMISED HERE.** Creative changes vanilla's own
behaviour, not only ours; the register records the divergences we have MEASURED, and its growth is
the point. **An empty-looking register means nobody has looked, not that the modes agree.**

| divergence | effect | account |
|---|---|---|
| **`hasInfiniteMaterials()`** short-circuits `BowItem.use`'s ammunition check | the Plume's **draw always starts** and **its arrow is never consumed**. `R-N1`'s condition cannot be produced at all | `GATE-plume-draw.md` and `GATE-plume-release.md`, *GAME MODE*; bytecode in `PLAN-dragons-plume.md` §1.1 |
| **The own-inventory screen decomposes a gesture into independent single-slot writes** — `InventoryCreativeEvent`, one slot and its new item | a slot-guard is asked about **half a gesture**: the half that MOVES the protected item is refused, the half that CREATES one is innocent. **Duplicates the Nexus star**; a chest view does not reproduce it | `GATE-nexus.md` Row 8 |
| **`MenuRouting` refuses `CLONE_STACK` outright** — creative middle-click *"makes items out of nothing"* | our own refusal, not vanilla's. **The one creative constant whose loss is a real economy hole** | `MenuRouting`'s `CLONE_STACK` section; `GATE-crafting.md` S10 is its **sole witness** |

> **THE FIRST TWO ARE VANILLA'S AND THE THIRD IS OURS, AND THE REGISTER IS WORTH MORE FOR MIXING
> THEM.** A reader asking *"can I boot this in creative?"* does not care whose code diverges; they
> care whether the answer changes. **Sorting by owner would put the two halves of that question in
> different lists.**

> **AND THE SHAPE TO LOOK FOR WHEN ADDING THE FOURTH: CREATIVE REMOVES A COST.** Ammunition,
> durability, consumption, the need to have the item at all. **A row whose reading is "the thing is
> still there" is satisfied for free the moment the cost is gone**, and it passes without exercising
> anything. That is the hollow-fixture rule above, with creative as the mechanism — which is why this
> register sits beside it rather than in the gate-file conventions.

**STANDING DEBT — OTHER RULES CURRENTLY CARRIED ONLY BY CONVENTION.** Named, not fixed; each is a
candidate for the same treatment. Measured against `CLAUDE.md` at `c5ee6a0`:

- **The test-count instrument.** This file states the rule **nowhere but in this debt entry** — every
  occurrence of the marker in this file is one of the lines you are reading. The rule is
  `git grep -ho '@Test' <ref> -- '<module>/src/test/**/*.java' | wc -l`, and *say which instrument*.

  > **NO NUMBER IS GIVEN HERE, AND THAT IS THE FIX RATHER THAN AN OMISSION.** This bullet used to
  > say the marker appeared **0** times — false, because the line asserting it contained the marker.
  > **Replacing it with the true count failed the same way**: the corrected sentence quoted the
  > marker twice more and was wrong before it was saved.
  >
  > ***A COUNT OF THIS FILE'S OWN TEXT CANNOT BE WRITTEN AS A LIVE LITERAL, BECAUSE WRITING IT
  > CHANGES IT.*** Not a hard figure to keep current — an impossible one. **Give the command and no
  > live figure.**
  >
  > **A QUOTED HISTORICAL ONE IS FINE AND THIS ENTRY USES SEVERAL** — *"it used to say 0"*, *"3
  > became 4"*. Those are records of a past state, not assertions about the current file, and
  > nothing falsifies them. **The ban is on a number the reader would check against the file in
  > front of them.**
  **`grep -rho` IS NOT AN EQUIVALENT, AND THE DIFFERENCE IS NOT ARITHMETIC:** it classifies any file
  holding a NUL byte as **binary** and counts **nothing** from it. `core` has one — `EnchantCodecTest`
  carries a **single** NUL inside a string literal, and **15** `@Test` that grep never sees.
  `grep -rhoa` returns **1009**, agreeing with `git grep` and with `perl`.

  > **SO THE MAGNITUDE IS THE WRONG FORM FOR THIS RULE, AND ONLY THE MECHANISM SURVIVES.** The gap is
  > whatever lives in whichever files *that* grep, on *that* machine, in *that* locale calls binary.
  > It moves when a test is added to one of them, and a new NUL-carrying file joins the set with no
  > announcement.
  >
  > **It is not even stable across grep versions on one tree.** Measured at `c5ee6a0`: GNU grep
  > **3.11** returns **994**, GNU grep **3.0** returns **995** — and that extra line **is not a
  > match**. It is grep's own `Binary file … matches` notice, written **to stdout, while piped**, and
  > counted by `wc -l` as though it were data. Both versions emit **zero** real matches from the file;
  > they differ only in whether they narrate the skip into the count.
  >
  > **AN INSTRUMENT THAT SKIPPED A FILE LOOKS EXACTLY LIKE ONE THAT COUNTED IT** — and it either
  > suppresses its warning precisely when it is being measured with (3.11, silent when stdout is not a
  > terminal) or feeds the warning into the measurement (3.0). **Silence and self-description are both
  > failures here, and neither is visible to `| wc -l`.** This file's own headline defect, one layer
  > down: the instrument, not the test, is what lied.
  >
  > > **AND THE RULE THAT DISAGREEMENT TAUGHT: SCOPE EVERY VERSION-DEPENDENT CLAIM TO ITS VERSION, IN
  > > THE SENTENCE ITSELF.** The `995` / `994` split was not a disagreement about the tree — both
  > > readings were correct, on different greps. **Each side had written a property of ONE grep as a
  > > property of grep**, which is why neither could reproduce the other and why the reconciliation
  > > took three instruments.
  > >
  > > A claim about a tool's behaviour is a claim about **a build of that tool**, and it is worth
  > > exactly nothing to a reader on a different one unless the version is in the sentence. Print
  > > `--version` beside the reading, not in a footnote.
- **Reason from `origin/<ref>`, never a local ref.** Stated nowhere but here, and every mention of
  the remote ref in this file is either this bullet or an instruction about **pushing** rather than
  about reasoning from it. **Counts deliberately omitted — see the bullet above for why a count of
  this file's own text cannot be written down.** *(Both figures were previously bare, and both had
  drifted.)*
- ~~**A squash body must carry the DEFECTS FOUND, not only what worked.**~~ **PAID** — it is now
  stated under *Squash-merge bodies*, with the `#92` instance. **Left struck rather than deleted, so
  the list records that the debt was paid rather than silently shortening**; a debt list that only
  ever loses rows cannot be told from one nobody is maintaining.
- **The gate-file conventions**: `Status: NOT RUN`, every prediction written *before* the boot, the
  reading written *beside* the prediction, and the prediction not edited once a row has been read.
  Stated in each `GATE-*.md` header and nowhere central; this file's one *NOT RUN* mention is an
  anecdote about a splice, not the rule.
- **A GATE FILE DECLARES ITS GAME MODE, in the header and per row.** Measured across all 19
  `GATE-*.md` at `e9b3e0e` for `gamemode|survival|creative|adventure|spectator`: **only
  `GATE-quiver-ammo.md` and `GATE-crafting.md` declare one.** `GATE-nexus.md` and the two Plume files
  have since been fixed; **16 files are still owed it and are NOT edited here.** The cost is measured
  rather than asserted — two of `GATE-nexus.md` Row 6's six readings are **VOID** for want of this
  line, and `GATE-plume-release.md`'s `R-N1` **fails while testing nothing** in the wrong mode.

  > **AND THIS FIGURE IS OWED A RE-MEASUREMENT, WHICH IS NOT THE SAME AS BEING WRONG.** It is
  > anchored at `e9b3e0e` and three files have been fixed since, so the *"16 still owed"* describes
  > a tree that has moved. **A token sweep is NOT the method** — see immediately below — so
  > re-deriving it means opening each `GATE-*.md` and reading its header for a declared mode:
  >
  > ```bash
  > grep -lE 'gamemode|survival|creative|adventure|spectator' GATE-*.md   # CANDIDATES ONLY
  > ```
  >
  > **That command over-reports and must not be pasted as the answer.** Owed work, named here
  > rather than guessed at, and deliberately not done in a slice that was not about it.
  >
  > **THE GREP FOR THIS DEBT IS A FALSE-PRESENCE TRAP, SO THE COUNT IS STATED WITH ITS METHOD.**
  > Two more files match those tokens and declare nothing: `GATE-nexus.md`'s three hits were all
  > `InventoryCreativeEvent` as a MECHANISM, and `GATE-vanilla-damage.md`'s one hit is *"nearly
  > recorded as a survival"* — **a fall the player lived through, not a game mode.** A bare
  > `grep -l` says 4 of 19 and the answer is 2.
- **THIS FILE'S OWN LENGTH, AND THE ACCOUNT FILE THAT STOPPED BEING WRITTEN.** `CLAUDE.md` is loaded
  every session by both seats, so **its length is paid on every turn, forever.** Measured:
  **`618bc9e` 1377 → `0dd9bbe` 1620 — +243 lines, +17.6%, in one week, across four commits**
  (`#91`, `#94`, `#95`, `#98`). **Anchored to `0dd9bbe` because this entry cost 19 more: a line that
  carries a count is itself a line, and an unanchored figure here would be stale on arrival.**

  > **AND IT WAS STALE ANYWAY, WHICH IS THE POINT OF THE RULE ABOVE RATHER THAN A FAILURE OF IT.**
  > Anchoring told the reader *when* the figure was taken; it did not tell them the file had grown
  > since. **RE-DERIVE BOTH ENDS IN LINES OF `CLAUDE.md`, AND SUBTRACT NOTHING BY HAND:**
  >
  > ```bash
  > wc -l < CLAUDE.md                      # lines, now
  > git show 0dd9bbe:CLAUDE.md | wc -l     # lines, at the anchor above
  > ```
  >
  > **No current figure and no delta are written here on purpose** — either would be a number
  > maintained by delta, in the entry that names that defect.
  >
  > **The growth is the debt, so the figure has to be current to mean anything** — and this is the
  > one entry in the file whose subject IS its own number.

  > **THE CAUSE IS A MISSING DESTINATION, NOT CARELESSNESS.** `NEXT.md` has not been written since
  > `2b41c62` (`#71`, 2026-09-13). **Every account since has had nowhere to go** — the squash bodies
  > have been carrying them, which is why they are so good, and the remainder has landed *here*, in
  > the POINTER file, for want of an alternative. **The project lost its account file and has been
  > writing accounts into its pointer file ever since.**
  >
  > **The failure mode is the one the two-homes convention exists to prevent, one level up: a loaded
  > file that grows past being read stops being loaded IN PRACTICE, and then every rule in it is
  > homeless again.** The cure and the disease are the same mechanism at different lengths.
  >
  > **THE QUESTION IS WHETHER `NEXT.md` IS REVIVED OR SUCCEEDED, AND IT IS BEN'S — NOT A CLEANUP.**
  > Named here rather than fixed: fixing it inside any PR would widen that PR by more than the rule
  > it was carrying.


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

**AND THE THIRD MEMBER, WHICH NEITHER OF THOSE SWEEPS CAN CATCH: AN EXPLANATION WHOSE EVERY
SENTENCE IS TRUE BUT WHOSE EMPHASIS IS WRONG SURVIVES EVERY CHECK A FALSE ONE FAILS.** Nothing
expired, so the stale-prose sweep walks past it.

**So: when a comment gives two reasons for one construct, LEAD WITH THE ONE SPECIFIC TO THIS SITE.**
A reason shared with every sibling goes second, or in the shared place. **The tell when auditing: a
construct on one path and not its sibling, justified by a reason true of both.** Account, and the
week it cost, in `#107`'s body.

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

- **NEW CONTENT CITES THE BOLTOR, NOT `hunters_bow`, `ironblade` OR `quiver_stone`.** Those three are
  **in a deletion set** (ruled 2026-09-12, parked on *enough shipped weapons to replace them* — see
  `NEXT.md`). Every citation of one written between now and then is a comment that will point at a
  file that does not exist.

  **When a dev weapon is genuinely the only precedent, cite it AND MARK the citation** as standing on
  a weapon in the deletion set, so the eventual sweep finds it by **grepping for the marker** instead
  of re-deriving the reference graph.

  > **THE PLAN HAS A LOOP IN IT AND THIS IS THE FREE HALF OF THE FIX.** The precondition for deleting
  > the dev weapons is **more real weapons** — and new weapons are also **what adds references to the
  > dev weapons**, because the dev weapons are the precedents new prose derives from. `boltor.yml`,
  > the newest weapon in the project, **cites all three.** So every weapon authored between now and
  > the deletion raises the deletion's cost.
  >
  > It compounds quietly because it is **staleness, not breakage**: nothing fails, nothing is listed,
  > and the bill arrives later as a sweep nobody scoped. Measured at `2a3fb68`: **every mention of
  > these three in `main` is a comment except one**, so the deletion's real cost is prose, not code.

  **TWO THINGS TO KNOW ABOUT THE BOLTOR, SINCE IT IS NOW THE REFERENCE POINT:**

  - **Its numbers are RULED, not derived** — `quiver_size 8`, `cooldown_ticks 16`, `range 96`,
    `reload_ticks 60`, `attack_damage 19`. **`19` was ruled outright BECAUSE deriving it from
    `ironblade` was wrong**: `ironblade` is a dev weapon and is not balanced meaningfully.
    **So: never derive a new weapon's numbers from a dev weapon's.** Parity with a placeholder is
    parity with nothing, and it propagates — the derived number then becomes the next weapon's
    precedent and the placeholder's arbitrariness outlives the placeholder.
  - **`16` is on the 4-tick input grid, deliberately.** A held right-click's inputs are quantised onto
    a 4-tick grid, so a weapon's real fire interval is its authored cooldown **rounded UP to the next
    multiple of 4** — author `13` or `14` and you have authored `16`, **and the tooltip will not say
    so.** **Author multiples of 4.**

    > **THIS SAID "delivers an input only every 4 ticks" UNTIL 2026-09-13, AND THAT PERIOD CLAIM IS
    > FALSE.** `GATE-locust.md` row 1 read `INPUTS min 3t`, twice. **Holding right-click does not
    > produce a periodic stream at all** — operator's ruling, a property of the vanilla client.
    > **The grid survives and every prediction it makes survives with it**; what died is the sentence
    > explaining *why* there is a grid. Do not restore *every*, and do not change the 4 — the
    > arithmetic is confirmed at four measured points and the mechanism was never what the arithmetic
    > rested on. The mechanism, the measurements and the tooltip consequence are
    at `WeaponLoader`'s `cooldown_ticks` section — **this is the pointer, that is the account.**

    > **THE "MULTIPLES OF 8 IF IT MAY EVER BE DUAL-WIELDED" CLAUSE IS WITHDRAWN, 2026-09-13 — AND IT
    > IS WITHDRAWN FOR WANT OF A SUBJECT, NOT BECAUSE IT WAS WRONG.** The operator ruled that the
    > second Ranger weapon is a single item: *"it's not going to be Dual wielded."* **Nothing halves
    > any more**, so a rule about surviving halving has nothing to apply to. The `locust` ships at
    > **12** — not a multiple of 8, and correct.
    >
    > **`16` IS NOT RE-RULED, AND THE RECORD MUST STILL EXPLAIN WHY 16 AND NOT 12.** The clause was
    > one of the Boltor's reasons and its other reasons stand — the account at `WeaponLoader`'s
    > `cooldown_ticks` section, and the derivation in `boltor.yml`'s `cooldown_ticks` block, which
    > carries the same withdrawal note.
    >
    > **AND THE CLAUSE WAS UNSATISFIABLE ANYWAY, WHICH IS WHY THIS IS A WITHDRAWAL AND NOT A PAUSE.**
    > Halving moves a weapon onto a smaller cooldown, and **the attack-speed dead zone widens as the
    > cooldown shrinks** — `8 -> 4` is inert entirely, `16 -> 8` dead to +77.8%, `32 -> 16` to +28.0%.
    > To buy a *dual* dead zone as narrow as the Boltor's *single* +28% you must author **32**, a
    > 1.6-second shot. **Clean halving, attack-speed headroom and a fast weapon are jointly
    > unsatisfiable.** Full argument in `PLAN-locust.md`.

### NAME THE QUANTITY, AND NAME THE SET OF THINGS THAT HAVE IT

**Before writing any `value_by_level`: name the quantity the curve moves, and answer BOTH halves.**

1. **ARITHMETIC — is the quantity non-zero and continuous at the values it will meet?** If it is
   quantised, floored, or absent, **author a FLAT value instead** — or park the feature.
2. **ELIGIBILITY — does every piece of gear this can ROLL ON actually possess that quantity?** If
   some do not, **the gate is wrong, and no curve fixes it.**

> **THE TWO HALVES ARE ONE RULE, NOT TWO, AND THAT IS THE WHOLE POINT.** An enchant can be
> **arithmetically honest and still grant zero**, because the weapon it landed on does not possess
> the thing it modifies. **Passing the first half reads as passing** — the number is real, the
> multiplicand is real, and the player still receives nothing.
>
> **THE SAME ENCHANT FAILED BOTH HALVES**, which is why they cannot be separate rules: Expanded
> Quiver failed the arithmetic half (a percentage of an integer magazine floors to nothing) and was
> ruled FLAT — **and then failed the eligibility half anyway**, because `hunters_bow` is
> `class: ranger` and authors no `quiver_size`. Fixing the first did not touch the second.

**The failure is not a weak enchant. It is a tooltip that advertises a number while the player
receives ZERO**, and nothing goes red, because a curve that resolves correctly and lands on nothing
is indistinguishable from one that works.

Three enchants proposed in one slice (2026-09-13) produced **four** failures of this rule, across both
halves — which is why it is stated as a rule rather than four notes:

| proposed | half | the quantity | how it fails |
|---|---|---|---|
| **Rapid Fire** | arithmetic | a **QUANTISED** cooldown | the 4-tick input grid swallows anything under **+28%** |
| **Expanded Quiver** | arithmetic | an **INTEGER** magazine | `QuiverSize.arrows` floors — one arrow is 11% at 9 rounds, so every tier below that grants nothing |
| **Punch** | **eligibility** | a knockback base that **DOES NOT EXIST** on a ray | `applyDamage` never calls `entity.damage()`, so a ray hit raises no `EntityKnockbackEvent`; base push `0.0` |
| **Expanded Quiver**, *again* | **eligibility** | a magazine the weapon **does not have** | `hunters_bow` is `class: ranger` with no `quiver_size`, so the enchant rolls on and renders `+2 Quiver Arrows` for nothing |

**Quantised, floored, absent, unpossessed — four different mechanisms, one question catches all
four**, and it is cheaper than any of the four investigations that found them separately.

> **THE FOURTH ROW IS WHY THE RULE HAS TWO HALVES.** It was found only after the arithmetic half had
> already "passed" the enchant and a flat value had been ruled. **`+2 arrows` is `+2 arrows`** — the
> arithmetic test cannot see it. It arrived through the **ROLL TABLE** instead, which is a door the
> one-half version of this rule does not watch.

> **AND PUNCH IS THE ONE THAT SURVIVED, WHICH IS THE HALF OF THIS RULE WORTH KNOWING.** Operator
> ruling, 2026-09-13: *"Other ranged weapons will have knockback, the instant hitting ranged weapons
> don't."* A **travelling** weapon authors an `EffectSpec.Knockback` in its `on_hit`, and +20/40/60%
> of an authored `strength` is a real percentage of a real value. **Punch's numbers shipped as first
> ruled — the only one of the three that needed no renumbering at all.**
>
> **THE FIX WAS NOT A DIFFERENT CURVE. IT WAS NAMING THE QUANTITY.** The other two were repaired by
> changing the arithmetic — park it, or go flat. This one was repaired by **supplying the missing
> multiplicand**, and the curve never moved. So the rule's instruction is *name the quantity and
> check it*, in that order, and **not** *"prefer flat"*: two of three failures did need flat, and
> reading the rule as a preference for flat would have renumbered a curve that was correct.
>
> **The check is the same either way, and that is the point** — you cannot tell which of the two
> outcomes you are in until you have named the quantity and gone and looked at it.

> **THE THIRD ONE IS THE REASON THIS IS A CHECK AND NOT A REMINDER TO BE CAREFUL.** *Is this
> continuous?* is a question about a number you can see. *Does this base exist at all?* is a question
> about a call you have to go and read — and the plausible answer was the wrong one, because
> `VanillaDamagePolicy` says knockback rides the vanilla event and the ability path raises no vanilla
> event for that sentence to be about.

**Practically: write down the quantity's actual authored values before the curve.** The live
magazine spread is `8` and `12`; the live knockback base on a ray is `0.0`. Both took one grep and
one trace, and both were available before any design.

Full account, all three instances, and the Punch base trace: `PLAN-enchants-ranged.md`.

### A TRAVELLING RANGED WEAPON AUTHORS KNOCKBACK; AN INSTANT-HITTING ONE DOES NOT

**Operator ruling, 2026-09-13.** A `type: projectile` ranged weapon **lands with impact** and declares
an `EffectSpec.Knockback` in its `on_hit`. A `type: ray` ranged weapon is **hitscan** and declares
none.

```yaml
on_hit:
  - type: weapon_damage
    element: kinetic
  - type: knockback          # travelling weapons only
    strength: 0.4
```

**No new schema.** `EffectSpec.Knockback(double)`, `EffectApplier` and
`CombatantHandle.applyKnockback` all already exist; `AbilitySchema` already parses `type: knockback`.

**THIS IS A CONTENT RULE, AND THE CODE DOES NOT IMPLY IT — WHICH IS THE ONLY REASON IT NEEDS WRITING
DOWN.** Neither cast shape produces knockback on its own: `CastExecutor.detonate` is the single
on-hit site and **every** shape routes through the custom health store, so nothing calls
`entity.damage()` and no `EntityKnockbackEvent` is ever raised. **You cannot read this rule off the
engine — both shapes look identical there.** It is a decision about what content declares.

**This is why Punch does nothing on a Boltor, and that is CORRECT rather than a gap.** A ray is
meant to have no push.

> **SCOPE, STATED BECAUSE THE OBVIOUS SWEEP IS WRONG.** The ruling is about **ranged** weapons.
> Measured 2026-09-13: three shipped weapons are `type: projectile` and author no knockback —
> `ember_staff` and `flint_staff` (mage) and `emberblade` (melee). **This rule does not reach them.**
> Do not "fix" them, and do not cite this rule at a staff.
>
> **AND THEY ARE UNRULED, NOT EXCLUDED — THE TWO ARE ONE KEYSTROKE APART AND ONLY ONE IS TRUE.**
> Nobody has decided that a travelling MAGE weapon should not push. **The question was never put.**
>
> > **AN UNRULED CASE MUST BE RECORDED AS UNRULED, NOT AS EXCLUDED.** *"Excluded"* says a decision
> > was taken and closes the question; *"unruled"* says it is still open and invites it. **Writing
> > the first when the second is true silently converts an omission into a ruling nobody made** —
> > and the next person to look finds a settled-looking answer with no author.

**THE ROLL GATE, AND IT IS WHAT KEEPS THE TOOLTIP HONEST:**

> **PUNCH MUST NOT ROLL ON A WEAPON THAT AUTHORS NO KNOCKBACK.** Otherwise a player enchants a
> Boltor, the tooltip reads *"+40% knockback"*, and nothing happens — **the exact dishonesty the rule
> above eliminated three times, arriving a fourth time through the ROLL TABLE instead of the
> arithmetic.**
>
> It is **mechanically checkable**: the weapon's `on_hit` list either contains a `Knockback` or it
> does not. That is a loader / roll-eligibility check, **not a review obligation** — and it is the
> whole difference between *inert by design, visible in the content* and *inert in a way only a
> player discovers*.

**Punch is PARKED, design complete** — no weapon can take it today. Trigger and full design in
`NEXT.md`; the design itself in `PLAN-enchants-ranged.md` §4.

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
- **THE DEFECTS FOUND, NOT ONLY WHAT WORKED** — see immediately below

### AND THE BODY CARRIES THE DEFECTS FOUND, NOT ONLY WHAT WORKED

**A body that records only the outcome is a CHANGELOG. The defects are the part that cannot be
re-derived from the diff**, because the diff shows what the code became and never what it nearly
became, what was tried and abandoned, or which green check turned out to be measuring nothing.

**THE DEFECTS ARE ALSO THE HALF MOST LIKELY TO BE OMITTED, AND THE OMISSION IS SELECTION RATHER THAN
CHANCE.** A body is written by the person who has just finished the work, at the moment they most
want it to read as finished. What worked is in front of them; what went wrong is behind them, and
leaving it out costs nothing that anyone will notice this week.

> **The measured consequence, and it is this repo's own: `#92`'s body was written to carry eleven
> named defects, and when the context that produced it was lost, THAT BODY IS WHAT RESTORED IT.** A
> changelog-shaped body of the same work would have said the Nexus shipped and left the two hollow
> checks, the unreachable-guard finding and the two tooling traps to be rediscovered.
>
> **Two later slices ran off it.** `#93` found `5d`'s prediction unfalsifiable because `#92`'s body
> had named the hollow-check shape; `#94` found a taxonomy with no home because `#93`'s body named
> the arm. **None of that is in any diff.**

**So: a body with no defects section is a claim that none were found, and that claim is almost always
false.** If a slice genuinely produced none, say so in those words — it is a surprising result and it
should read as one.

Applies to a PR conversation the same way: the verdict, the controls, and what they refuted.

> **THE SQUASH'S OWN TREE SHA CANNOT GO IN THE BODY, AND THE FIRST DRAFT ASKED FOR IT ANYWAY.** That
> draft required *"the squash's own tree SHA, and that the two were EQUAL"*. **A commit message
> cannot contain its own tree SHA** — the tree is an input to the hash, and the message is part of
> the commit that names it. Nor can it contain the `ABSORBED` verdict, which is computed against the
> squash that does not exist yet. Both were discovered on the **first attempt to follow the
> convention**, one merge after it landed, by trying to write the body and finding two of **that
> draft's** three required fields unwritable.
>
> > **THIS HEADING SAID *"THE THIRD ITEM"* UNTIL 2026-09-15, AND THE COMMIT THAT ADDED THE DEFECTS
> > SECTION IS WHAT FALSIFIED IT.** The list above had **two** items when that note was written, so
> > *"the third item"* had **no referent in it** — a reader counting to three found nothing, re-read,
> > and resolved it from the note's own text. Adding **THE DEFECTS FOUND** as a third bullet gave the
> > ordinal a live and **exactly wrong** referent: the note now appeared to say that the defects
> > cannot go in the body, which is the opposite of the section it was pushed below.
> >
> > **AN ORDINAL INTO A LIST IS A LINE CITATION WEARING DIFFERENT CLOTHES**, and it fails by this
> > file's own rule — *the pointer names a SECTION, never a line* — falsified by any insertion above
> > it, silently, and invisibly to every test. **The insertion was made by the commit that also added
> > this note's neighbours, and nothing flagged it**: the `###` heading spliced in between put **27
> > lines** (measured, not estimated) between the list and the note, so the two were never adjacent
> > enough for the collision to be seen.
> >
> > **And it got WORSE by being fixed elsewhere**, which is the part worth carrying: a dangling
> > ordinal is a FALSE ABSENCE and prompts a second look; an ordinal that resolves to the wrong item
> > is a FALSE PRESENCE and closes the question. **Growing a list is enough to convert one into the
> > other.** So: name the item, never its position.
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

> **THIS IS A PROPERTY OF THE COMMAND, NOT OF THIS ONE USE — AND SCOPING IT TO "IS THIS BRANCH
> ABSORBED" IS AN OVER-SCOPED CLAIM, WHICH IS A FALSE ABSENCE.** Two-dot is tip-vs-tip for **every**
> question anyone asks with it, so it manufactures a phantom difference for any of them. **Use
> `git diff master...<branch>` — three dots — whenever you want what the BRANCH CHANGED**: a
> collision check before editing a file another PR touches, a review of someone else's work, a
> "what is still outstanding on that branch".
>
> **2026-09-16, and the warning EXCUSED the walk-in rather than preventing it.** A collision check
> between two doc branches read the two-dot diff and reported that the open PR would silently revert
> a correction merged after it was branched. It would not: three-dot showed it changes two hunks and
> never touches that text. **The warning was present, read, and concluded to be about absorption.**
> Same failure as scoping the binary-mode artefact to one `grep` version — a tool's behaviour written
> as one use's caveat tells every other caller it does not concern them.

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

### A LONG-LIVED BRANCH: DISTANCE IS NOT THE PREDICTOR OF ROT. OVERLAP IS

**A branch does not decay with time. It decays when something lands on the files it touches.**

> **2026-09-16, and the numbers run the wrong way round.** One branch was rebased twice.
> **SIX merges behind: 16 compile errors** — a slice had turned `NexusLock.LOCKED_SLOT` from a
> `static final` into a per-player parameter. **EIGHT merges behind: CLEAN**, because the
> re-derivation held and the two intervening PRs touched other files.
>
> **The instinct that fails here is "this is old, rebase it again"** — which would have re-derived
> work that was already correct. **Ask which FILES have changed, not how many commits have.**

**AND A CLEAN MERGE STILL PROVES NOTHING — `merge-tree` ANSWERS A NARROWER QUESTION.** Zero conflict
markers means *these diffs do not touch the same lines*. It cannot mean *this still compiles against
a signature that moved underneath it*. **Both rebases above merged clean; one of them did not
build.**

> **So: REBASE AND BUILD, never rebase and read the merge.** Same family as `git diff --numstat` for
> mutation overreach and two-dot for *what did this branch change* — **an instrument answering a
> narrower question than the one being asked, returning a number that reads as a pass.**
>
> **THE MEMBERS ARE NAMED RATHER THAN COUNTED, BECAUSE THIS ENTRY SAID *"Third member, one week"*
> AND THE NEXT ADDITION FALSIFIED IT.** An ordinal into a growing list is a line citation wearing
> different clothes — this page's own rule, biting the page again:
>
> | instrument | the narrower question it actually answers |
> |---|---|
> | `git diff --numstat` for mutation overreach | *what changed since HEAD*, not *what this edit changed* |
> | two-dot `git diff` for what a branch changed | *tip vs tip*, not *what the branch contributed* |
> | `merge-tree` / a clean rebase | *do these diffs touch the same lines*, not *does it still compile* |
> | **`perl -i` with a `$` anchor, on a CRLF WORKING TREE** | **nothing at all — see below** |
> | **a generic bound** (`n-1` for sum-of-floors vs floor-of-sum) | **what the quantity can NEVER EXCEED, not what it IS** |
>
> **THE BOUND IS THE MEMBER THAT NEEDS NO TOOLING TO COMMIT, AND IT IS NOT A FABRICATION.** A
> fabricated quote has no source and is caught by reading the line. **A bound HAS a source and is
> TRUE** — it is caught only by asking whether the question it answers is the question that was
> put. ***"Up to `n-1`" is always safe to say and almost never the number.***
>
> **2026-09-16.** A brief and a javadoc both put the tray-batching gap at **13**, the `n-1` bound
> for fourteen items. **The reachable maximum is 12.** Per-item spends are sums of
> `{352, 1262, 4182}`, so `(c * 35) mod 100` takes only the ten values `{0,10,…,90}`; fourteen
> items at `90` sum to `1260`, whose own remainder is `60`, so the gap floors to **12**. The `13`
> was never wrong — it was never a measurement.
>
> **This is *compute the class, do not characterise it*, one level up**: there the description
> fitted the examples in front of you, here the bound fits every case and identifies none. **Same
> remedy: enumerate over the reachable range and read the answer off.**
>
> **AND THE FOURTH IS THE PUREST OF THEM, BECAUSE IT RETURNS NO NUMBER TO MISREAD.** The other three
> hand you a misleading figure. **This one exits 0 having changed nothing**, and silence is
> indistinguishable from success.
>
> **On a clone with `core.autocrlf=true` the working tree is CRLF, and a `$`-anchored pattern cannot
> match** — `\r` sits between the last character and the newline, so `s{...;$}{...}` matches nothing,
> reports success, and leaves the file byte-identical.
>
> > ### *** THIS IS A PROPERTY OF A CLONE, NOT OF THIS REPO — AND THE FIRST DRAFT SAID "on this tree" ***
> >
> > **EVERY COMMITTED BLOB IS LF**, because `autocrlf` normalises on the way into the index. So a
> > seat with `core.autocrlf=false` — Linux, typically — has **ZERO CR bytes in the working tree AND
> > in the blob**, and this hazard does not exist for them at all.
> >
> > **Measured 2026-09-16, both seats, same file `EnchantMenu.java`:**
> >
> > | seat | working tree | blob |
> > |---|---|---|
> > | `autocrlf=true`, MINGW64 | **743 CR** | **0 CR** |
> > | `autocrlf=false`, Linux | **0 CR** | **0 CR** |
> >
> > **AND IT IS NOT EVEN UNIFORM WITHIN ONE CLONE.** A file just WRITTEN is LF until git next checks
> > it out; measured the same day, two freshly committed files read **0 CR in the working tree** on
> > the `autocrlf=true` seat while `EnchantMenu.java` read 743. `git add` warns *"LF will be replaced
> > by CRLF the next time Git touches it"* — **the file is immune today and vulnerable after the next
> > checkout.**
> >
> > **THE BYTE COUNT IS PERISHABLE AND THIS ENTRY ALREADY CARRIED A STALE ONE.** It said **689**,
> > measured before a later PR added lines to that file; it is **743** now and will move again. **The
> > count is not the finding** — the finding is CR-present-at-all.
> >
> > **So the durable form is the COMMAND, not the number:**
> >
> > ```bash
> > tr -cd '\r' < "$F" | wc -c      # working tree: non-zero => $ anchors are dead here
> > git show HEAD:"$F" | tr -cd '\r' | wc -c    # blob: expected 0 on every seat
> > ```
> >
> > **AND A CONSEQUENCE FOR REPORTS: line endings, trailing whitespace and byte shape CANNOT BE
> > RE-DERIVED FROM `origin` BY A SECOND PARTY.** *"Verified from origin"* never covers that class.
> > **For byte-shape claims only, quote the measurement AND the command that produced it**, run on
> > the tree it was measured on, so a reader can judge whether the command answers the question.
> > **A named exception, not a general loosening** — reach for it outside byte shape and it is the
> > wrong instrument.
>
> **THE CONSEQUENCE THAT MAKES THIS WORTH A ROW: A MUTATION PASS CAN REPORT A FULL KILL SET WHILE
> NEVER HAVING MUTATED ANYTHING.** Every mutation no-ops, every test stays green, and green under an
> unapplied mutation reads exactly like green under a real one.
>
> **Practically: do not use `$` anchors in a scripted edit on this tree.** Match on inner text, or
> splice by line number and print the region. **The marker grep and the line delta catch it** — both
> halves were already mandatory for mutations, and they are what caught this. Same instruction as the
> in-place-no-op rule above: when an edit reports success and changes nothing, **change the
> instrument rather than retrying with a cleverer pattern.**

**Practically, for a branch parked on purpose:** name the files it touches, and it is safe to sit
until something lands on one of them. That turns *"is this stale?"* into a grep instead of a feeling.

## Working with me

- I am rusty at Java. If you use a language feature I may not know — sealed
  interfaces, records, pattern matching in `switch`, `var` — explain it in one
  line rather than assuming.
- Write the `core/` unit test before the `paper/` wiring.
- Keep changes small enough that I can read them.
