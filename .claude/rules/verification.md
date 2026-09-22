---
paths:
  - "**/src/test/**"
  - "scripts/**"
---

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
- **ANY REPORT QUOTING A BYTE-SHAPE FIGURE CARRIES `./scripts/check-crlf.sh`'s READING**, the way a
  mutation pass carries its marker grep. **A step, not a tool that exists** — its line goes in the
  report: `control: PASS …`, the file count, and `CLEAN`.

  > **A CR COUNT TAKEN ON A MIXED TREE IS A NUMBER THAT IS NEITHER**, and **nothing else in the
  > chain can see it.** The blob is clean under `core.autocrlf=true`, so `--numstat` is silent, the
  > build is green, and CI has no working tree to look at. **A green CI check here would be a
  > control that cannot fail**; the obligation is therefore procedural, which is why it lives in
  > this list rather than in a workflow file.
  >
  > **RUN IT AFTER the scripted edits, not only before quoting.** The reading describes the tree
  > those edits left behind, so a run beforehand certifies nothing.
  >
  > > ### *** AND THE SAME MECHANISM THAT BLINDS `--numstat` HERE IS WHAT KEEPS IT TRUSTWORTHY EVERYWHERE ELSE ***
  > >
  > > The paragraph above reads as a warning about `--numstat`, and the warning is narrow: it is
  > > blind to **working-tree line endings**, and to nothing else. **Stated on its own it invites the
  > > opposite conclusion — that a file whose endings a tool has mangled has a `--numstat` reading
  > > that cannot be trusted. It does not.**
  > >
  > > **Commit-time normalisation is what makes the file-list reconciliation immune.** Under
  > > `core.autocrlf` or a `text` attribute, git compares the NORMALISED blob, so a `sed -i` or a
  > > `perl -i` that rewrites every line ending produces **no diff at all** — not a whole-file
  > > rewrite, and not a wrong insertion count. `--numstat` reports the CONTENT that changed, which
  > > is exactly what the reconciliation rule asks it for.
  > >
  > > **Measured 2026-09-21, twice, both ways round:** a committed, unmodified file deleted and
  > > checked out went `CR=0` → `CR=69` across 69 unchanged lines with `git status` **clean
  > > throughout**; and a `perl` insert that put two CRLF lines into an otherwise-LF file was
  > > invisible to `--numstat` while `tr -cd '\r' | wc -c` reported it immediately.
  > >
  > > **So the two instruments do not overlap and neither substitutes for the other.**
  > > `--numstat` answers *what changed*; the CR byte count answers *what shape the working tree is
  > > in*. **A report needs both, and it needs them for different claims** — which is why this sweep
  > > is a separate obligation rather than something the diff could have covered.
  >
  > **Quote the `control:` line with the verdict.** The sweep refuses a verdict when its own
  > controls fail (`BLIND`, exit 3), and **a bare CLEAN is indistinguishable from a classifier that
  > always says CLEAN** — measured: mutating one comparison left it reporting CLEAN on a mixed tree
  > until the control caught it.
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

  > **AND ON THE RED SIDE: READ THE FIRST FAILURE, NOT THE COUNT. A TOTAL EXAGGERATES AS EASILY AS
  > IT EXPOSES.** **2026-09-20:** a static test fixture threw in `<clinit>` and surefire reported
  > **26 errors in one class** — 22 of them rows that predate the change. **ONE report named the
  > cause; the other 25 read `NoClassDefFoundError`, which names nothing.** A count of 26 reads as
  > catastrophe and was one bad fixture.
  >
  > **Practically: a class-initialiser failure inflates the count by the size of the class**, so the
  > number carries no information about severity. **And prefer factory METHODS to `static final`
  > fixtures** — a method throws only for the rows that call it, where a field takes the whole class
  > down with it.

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

  > ### *** AND THE ANCHOR IS NOT ONLY A REVISION. IT IS EVERY PARAMETER THE FIGURE IS A FUNCTION OF, AND WHICH OF SEVERAL SITES IT MEASURES ***
  >
  > **The entry above anchors on TIME, and time is one axis of three.** A figure can be perfectly
  > current and still unusable, because it never said **what it is a function of** or **which thing
  > it measured** — and both failures read exactly like a figure that needed no anchor at all.
  >
  > **2026-09-21, one set of three figures failing both of the new axes at once.** The scorch
  > crossover healths — `emberblade 120`, `flint_staff 200`, `solar_grenade 20` — were **every one
  > arithmetically correct** and survived every check:
  >
  > | axis | what was missing | consequence |
  > |---|---|---|
  > | **PARAMETER** | the cap basis is the **scaled** damage, so each crossover is `x score/100` over a legal band of `100..500` | each figure is **one point on a five-fold range**, presented as the answer |
  > | **SITE** | two of the three name a **weapon** and quote one of its **several** fire sites | `emberblade 120` is the **fireball**; its **melee swing** crosses at **70** |
  >
  > **THE ASYMMETRY THAT MAKES THIS WORSE THAN A STALE NUMBER: a stale figure is eventually
  > contradicted by a re-measurement. A figure with no parameter anchor is NEVER contradicted**,
  > because it is true wherever anyone happens to check it against the value it was taken at.
  >
  > **Practically: before writing a figure down, ask what it is a FUNCTION of and how many things
  > could answer to its name.** If the answer to the first is "something that varies", quote a
  > **range with its anchor** rather than a point. If the answer to the second is "more than one",
  > **name the one** — `emberblade`'s *swing*, not `emberblade`. Neither costs a command; both are
  > invisible to every check in this file.

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
  >
  > ### *** AND THE BAN RUNS ONE STEP UPSTREAM OF `--body-file`, WHICH IS WHERE IT IS EASIEST TO DEFEAT ***
  >
  > **The rule is satisfied by the command that CONSUMES the file and defeated by the command that
  > BUILDS it.** `gh pr create --body-file body.md` is safe; `printf '...' >> body.md` with the prose
  > inline is the identical hazard one line earlier, and it looks like compliance because the flag
  > is right there.
  >
  > **2026-09-21, and it is this file's own author doing it.** A PR body was assembled by appending
  > a shell-quoted `printf` to a file that had been written properly, then passed to
  > `--body-file`. **The prose still travelled through shell quoting**; the file was a staging post,
  > not a barrier.
  >
  > ***THE CARRIER IS NOT THE FLAG, IT IS THE LAST POINT AT WHICH PROSE IS A SHELL WORD.*** Ask where
  > the bytes came from, not which option they were handed to. **Write the whole file with a file
  > tool, then pass it** — `-F`, `--body-file`, `--body-file` again for a merge. A `printf`, an
  > `echo` or a heredoc anywhere in the chain puts the quote break back.


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

  > ### THREE WAYS A `perl` EDIT REPORTS SUCCESS AND CHANGES NOTHING — THE POINTER, BECAUSE THE ACCOUNTS ARE ELSEWHERE AND NOBODY FINDS THEM
  >
  > **Each of these has cost a mutation on this project, and all three accounts live somewhere a
  > person about to WRITE an edit is not reading** — one inside the mutation-lies table's
  > commentary, one inside *A LONG-LIVED BRANCH*'s instrument table, and one in no repo file at all.
  > **A trap whose account is three sections away is a trap nobody is warned about.** So the
  > operational form is here, where the edit is about to be written:
  >
  > | trap | what to do instead |
  > |---|---|
  > | **THE DELIMITER.** A `/` — or your own delimiter — anywhere in the replacement closes `s///` early, so the edit lands as a bare deletion | `s{...}{...}`, and **`s#...#...#` the moment the replacement contains a brace.** A marker with no punctuation at all |
  > | **THE `$` ANCHOR ON A CRLF FILE.** `\r` sits between the last character and the newline, so a `$`-anchored pattern matches nothing and exits 0 | match on inner text, or splice by line number (`if $. == N`) and **print the region afterwards** |
  > | **`\Q...\E` AROUND AN EMBEDDED `\n`.** `\Q` escapes the backslash, so the pattern hunts a literal backslash-n and never matches a real newline | keep newlines OUTSIDE the `\Q...\E`, or splice by line number |
  > | **ANY BACKSLASH ESCAPE INSIDE `\Q...\E`** — the general form of the row above. `\Q` makes a backslash LITERAL, so `\[` hunts backslash-bracket. Exits 0, changes nothing | write the character plainly inside `\Q...\E`; it is already literal |
  > | *** **`@` INSIDE `\Q...\E`. `\Q` DOES NOT STOP INTERPOLATION** — `@link` parses as an array variable and interpolates to EMPTY. And `\@` inside `\Q...\E` becomes a literal backslash, so **THERE IS NO CORRECT `\Q` FORM AT ALL** *** | do not use `\Q...\E` on text containing `@`. Use a literal-match editor, or splice by line number |
  >
  > **The delimiter account is in the mutation-lies table's third row; the CRLF account is under
  > *A LONG-LIVED BRANCH*. The `\Q` one has no account in this repo** — it is carried in a memory
  > file, which is not greppable by whoever next opens the file it will bite. **That is the standing
  > `NEXT.md` debt, and it is named here rather than paid by growing this pointer into an account.**
  >
  > **The table was THREE rows when this note was written and is five now** — the live count is not
  > restated here, because a line carrying a count is itself a line, and this sentence has already
  > been falsified once by the table growing under it.
  >
  > **Two rows fired on 2026-09-17**, the slice that opened this entry: a `$` anchor silently skipped
  > an import insertion, and a `{` in a replacement aborted `perl` at parse time. **The second failed
  > LOUDLY and the first did not**, which is the whole reason the table is ordered this way.
  >
  > **Three more fired on 2026-09-19, and the ORDER OF DISCOVERY is the lesson.** A `{` in an
  > `s{}{}` replacement, and a `#` delimiter colliding with `{@link X#y}`, both aborted at parse time
  > — **loud, free, fixed in a minute each.** The `@` interpolation cost **four silent no-ops and a
  > bisect**, because the pattern did not fail outright: `through {@link` still MATCHED, having
  > quietly become `through {`. ***A pattern that FAILS is a tripwire; a pattern that HALF-MATCHES is
  > an investigation.*** The silent rows are the ones worth reading; the loud ones are in the table
  > only for completeness.

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

> ### *** AND A ZERO IS NOT A FAILURE, WHICH IS WHY THE CHECK THAT DIES IS THE ONE THAT SUCCEEDED ***
>
> **`grep -c` returning `0` EXITS 1.** So `cmd | grep -c X && next` aborts the chain precisely when
> the count is zero — and **zero is the answer a removal-check wants.** Every *"did I delete it"*
> verification is wired to kill the script at the moment it passes.
>
> **That is worse than `tee` eating `$?`, and the difference is the reusable part: `tee`'s hazard is
> CONSTANT, this one is CORRELATED WITH THE GOOD OUTCOME.** It bites only on the success path; on the
> failure path everything runs and looks fine. So it reads as a tooling flake rather than a defect.
>
> **2026-09-19.** A verification chain printed *"orAbsentStat gone: 0"* and then silently skipped
> every remaining check in the `&&` chain. **Operationally: `|| true` on the count, or capture and
> compare — never let a count decide control flow.**
>
> **AND THE SECOND MEMBER, SAME FAMILY, THREE LAYERS DEEP.** `./mvnw -pl paper test` without `-am`
> produces **NO OUTPUT AT ALL** when the reactor aborts on a stale installed jar; a `grep` filter
> turns no-output into no-match; and no-match reads as no-problem. **Three individually reasonable
> layers stacking into a pass.** Measured the same day: the identical command with `-am` reported the
> failure immediately. **Run the unfiltered tail, or check the command's own status — the filter is
> never allowed to be the witness.**

> ### *** A GREP COUNT THAT ANSWERS A DIFFERENT QUESTION THAN THE ONE ASKED — FOUR MECHANISMS, ONE SHAPE ***
>
> *"Who calls this?"* is the cheapest question in a codebase and it has four ways of lying -- the
> first three measured in one slice (2026-09-19), the fourth on 2026-09-21:
>
> | | what the count said | what was true |
> |---|---|---|
> | **ZERO CALLERS ON A NEW ACCESSOR** | nothing reads it | **the feature does not exist yet.** `WeaponDefinition.unscored()` was authored in YAML, parsed, stored — and read by nobody. **A value authored, parsed, stored and read by nobody is INDISTINGUISHABLE FROM ONE THE LOADER SILENTLY DROPS**, and every row in both suites stays green either way |
> | **A PREFIX NEEDLE** | the guard is present | **it survived the change it exists to detect.** `heldScore(player, keys)` is a PREFIX of `heldScore(player, keys, weapons)`, so a signature-test needle kept MATCHING after the widening — green, and no longer checking the thing it names |
> | **PROSE COUNTED AS CALLERS** | four callers outside the package | **all four were javadoc**, and two named a method that was no longer the enforcer |
> | **A NEEDLE ANCHORED TO A DELIMITER THE INSTANCES DO NOT CARRY** | one site, nothing to consolidate | **three sites, and the two it missed were the ones that would have gone red.** `"  x ` is anchored to the opening quote, so it found the render literal and could not match either `WeaponLoreTest` assertion, where the same text sits MID-LITERAL inside `"Kinetic Damage: 4  x 3"`. **Anchor to the syntax only where the instances actually carry it** |
>
> **THE ZERO-CALLER ONE HAS A HABIT ATTACHED: AFTER ADDING AN ACCESSOR, GREP ITS CALLERS BEFORE
> MOVING ON.** Zero means the wiring is not built, and it is the one answer nothing reports.
>
> **AND THE EXISTING GUARD COVERED AN ADJACENT CLAIM, WHICH IS THE FALSE-PRESENCE FAMILY WITH A NEW
> MECHANISM.** `knownKeysAndTheKeysParseActuallyReadsAreTheSameSet` proves **the key is READ**; nothing
> proved **the value ARRIVES**. Two different claims that look like one, and the real, passing guard
> was about the other one. **The round-trip row must assert BOTH answers** — a true-only row passes on
> a loader that hardcoded true, a false-only row passes on one that dropped the read. *Neither
> direction alone is a round trip.*
>
> **THE PREFIX ONE HAS AN OPERATIONAL FIX: ANCHOR A SIGNATURE NEEDLE AT BOTH ENDS** — include the
> closing paren or the terminating semicolon, so a widening cannot extend past the match. **Widening a
> signature is the single most likely edit to a scanned call site**, which is exactly when a prefix
> needle goes blind. It only failed here by luck: the closing paren happened to be inside it.
>
> **AND NARROWING VISIBILITY IS WHAT MAKES THE THIRD FINDABLE.** Making a method package-private turns
> *"who calls this"* into a question with a **checkable** answer, and everything left over is prose
> claiming to be code.

> ### *** THE LINE DELTA IS NOT A WARRANT AND IS NOT WORTHLESS. IT IS THE SOLE INSTRUMENT FOR THE ONE CLASS THE BUILD CANNOT SEE. ***
>
> Both halves were measured on 2026-09-19, and they point opposite ways:
>
> | the edit | the warrant | why the other instrument is blind |
> |---|---|---|
> | **A SPLICE into brace-delimited code** | **THE BUILD** | the delta lies in BOTH directions — it reported **18 removed** where 1 was predicted (`diff` realigned on a matching `}`, all 18 still present), and reported **exactly the predicted 555 lines** on the edit that had put a DUPLICATE BRACE in the file. Every figure agreed and the file was wrong |
> | **A SUBSTITUTION that may no-op** | **THE PREDICTED-COUNT DELTA** | the result COMPILES. 3 of 10 substitutions silently no-opped, `perl` exited 0, printed nothing, and **the build was green.** Only the count fired |
>
> **So do not drop the delta and do not trust it alone.** A splice's real check is reading the region
> and compiling; a substitution's real check is a count you predicted **before** running it.
>
> **AND A LINE-NUMBER SPLICE IS A CLAIM ABOUT WHAT IS AT THAT LINE, WHICH NOTHING IN THE CHAIN
> CHECKS.** The duplicate brace came from targeting line 201 believing it was the closing brace; it
> was the blank line after it. **Print the target line before splicing** — the count agrees whether or
> not you hit the line you meant.

> ### *** A FIXTURE STAGING A FACTOR AT ITS IDENTITY VALUE CANNOT DISTINGUISH ORDERINGS INVOLVING IT ***
>
> An addend at `0.0` or a multiplier at `1.0` makes *before* and *after* **numerically identical**, so
> the row passes under either and the mutation that swaps them finds nothing to bite.
>
> **THIS IS HARDER TO SEE THAN A HOLLOW FIXTURE, WHICH IS WHY IT IS ITS OWN ENTRY.** A hollow row
> never presents its condition. These rows were **not hollow** — every assertion in them was real and
> meaningful FOR ITS OWN CLAIM. They were **DEGENERATE ON EXACTLY THE AXIS UNDER TEST**: correct rows,
> blind at one joint.
>
> **2026-09-19.** Three rows guarding gear-score scaling all staged `classDamageBonus` at its neutral
> `0.0`. A mutation moving the scale across that addend reddened **only** a fourth row written for it.
> **Had the three been accepted and the pass stopped there, the ordering axis would have shipped with
> a FULL-LOOKING KILL SET AND NO GUARD** — and that is the danger: not a missing row, but a kill set
> that looks complete.
>
> **Operationally: ANY ROW THAT STAGES A CHAIN FACTOR AT ITS NEUTRAL VALUE IS BLIND TO WHERE THAT
> FACTOR SITS.** Count the axes in the expression, then ask which of them each fixture can actually
> see.
>
> > **AND THE COMPANION QUESTION, ASKED AND CLOSED RATHER THAN LEFT OPEN.** `HitDamage` threads four
> > factors, so *"are the other three joints unguarded?"* looks like an obvious follow-up. **Measured:
> > NO — there is ONE joint, not four.** The chain is `((base * M) + B) * C * R` and **`B` is the only
> > ADDEND**; a factor's position relative to a MULTIPLIER is not a distinguishable position. A sweep
> > of 5040 realistic combinations found crossing a multiplier changes the double in **926** of them,
> > by at most **1.137e-13** against a `1e-9` delta. **The other three are UNGUARDABLE, NOT
> > UNGUARDED** — and saying so in those words is the point, because the alternative is three rows
> > that cannot fail. *The instinct that multiplication commutes is also wrong; it is the MAGNITUDE
> > that settles it, and only execution shows that.*

> ### *** MUTATION AS A PROBE: WHEN A DEFECT IS SUSPECTED, RUN THE MUTATION BEFORE WRITING THE GUARD ***
>
> **THE FIRST RUN IS THE MEASUREMENT. THE SECOND IS THE RECEIPT.** Most mutation work only ever does
> the second, which tells you a guard fires and **nothing about whether it existed before you wrote
> it** — so it cannot distinguish *"I added a row that catches this"* from *"a row already caught this
> and I added a duplicate."*
>
> **A GREEN FIRST RUN IS THE FINDING.** 2026-09-19: the stamp path passing a hardcoded `false` was
> applied to a finished tree and **the entire suite stayed green at 2070 tests.** The guard was then
> written, the identical mutation re-applied — same marker figures both times — and it reddened. Two
> runs, one edit, and the pair is what makes the claim *"nothing else could see this"* a measurement
> rather than an assertion.

> ### *** A COMPILE-ENFORCED RULE'S POSITIVE CONTROL IS A DELIBERATE VIOLATION THAT MUST FAIL TO BUILD ***
>
> A visibility narrowing, a sealed hierarchy, an abstract method with no `default` — **none of them
> has a test to redden**, so *"the compiler enforces it"* otherwise has the same standing as a javadoc.
>
> **2026-09-19.** `GearScore.scoreable` was made package-private so `paper` could not ask the
> kind-level question without the instance-level one. The control: insert `GearScore.scoreable(null)`
> into a paper class, read the refusal — *"scoreable(GearClass) is not public in GearScore; cannot be
> accessed from outside package"* — and remove it **byte-identical**.
>
> **The same shape paid on an abstract port method the week before**: `CombatWorld.triggerScoreOf` was
> declared abstract rather than `default`, and the first full-reactor build failed naming the
> implementor that had not answered. **A `default` would have let a test fixture stay silent and still
> compile**, which is the blind-fixture shape this page records elsewhere. **Prefer the compile error
> to the scanner: a caller who forgets cannot build, where a source scan only notices if its needle
> still matches.**


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
- **Reason from `origin/<ref>`, never a local ref — and RECORD from a SHA, never from `origin/<ref>`.**
  The remote ref is the authority on what is on the wire **now**, and a moving target the moment it
  is written down. Stated nowhere but here, and every mention of the remote ref in this file is
  either this bullet or an instruction about **pushing** rather than about reasoning from it.
  **Counts deliberately omitted — see the bullet above for why a count of this file's own text
  cannot be written down.** *(Both figures were previously bare, and both had drifted.)*

  > **THE SECOND HALF WAS ADDED 2026-09-17, BECAUSE THE BULLET SCOPED ITS RULE BACKWARDS.** As
  > written it said *prefer the remote ref*, full stop — and `GATE-nexus.md` states the opposite for
  > the other half of the job: **a recorded count names the ref it was taken against, and the ref is
  > a SHA**, because a count against `origin/master` stays correct of a tree the sentence no longer
  > names. **Two rules, one axis, and the pointer carried only one end of it.**
  >
  > Reading it and recording it are different acts with opposite requirements: *is this merged* wants
  > the wire, *how many rows were there* wants a SHA. **A reader who obeyed this bullet literally
  > while writing a figure down did the wrong thing and had a rule telling them to.**
- ~~**A squash body must carry the DEFECTS FOUND, not only what worked.**~~ **PAID** — it is now
  stated under *Squash-merge bodies*, with the `#92` instance. **Left struck rather than deleted, so
  the list records that the debt was paid rather than silently shortening**; a debt list that only
  ever loses rows cannot be told from one nobody is maintaining.
- **The gate-file conventions**: `Status: NOT RUN`, every prediction written *before* the boot, the
  reading written *beside* the prediction, and the prediction not edited once a row has been read.
  Stated in each `GATE-*.md` header and nowhere central; this file's one *NOT RUN* mention is an
  anecdote about a splice, not the rule.

  > ### *** AND A GATE SHEET CHECKS EACH ROW AGAINST REALITY, NEVER THE ROWS AGAINST EACH OTHER ***
  >
  > **WHEN TWO ROWS IN ONE FILE TOUCH ONE MECHANISM FROM DIFFERENT SIDES, WRITE DOWN WHAT THEIR
  > CONJUNCTION IMPLIES — in one of the two rows, not in a summary nobody re-reads.**
  >
  > **Two PASSING rows can imply a defect that neither one reports**, and the cross-check is free:
  > both readings already exist and neither costs another boot. Nothing in the process takes it.
  > Account, and the withdrawn reading it cost, in `GATE-gearscore.md`'s *ONE READING WITHDRAWN*.

  > ### *** AND EVERY GATE FILE'S FIRST ROW IS NOW R0: THE DEPLOYED BUILD CARRIES THIS SLICE ***
  >
  > **It is the sole witness for every other row in the file, and a wrong jar does not announce
  > itself** — it produces readings, in the right shape, at plausible values.
  >
  > **2026-09-20 paid for this.** Slice 12b was booted from a jar built out of the **master
  > worktree** while the branch lived in a second worktree; only master's had a `run/`. **The jar
  > had been rebuilt that morning**, so its mtime was current and every staleness check cleared it.
  > The run produced **both** failure families at once: rows whose prediction is the AUTHORED value
  > would have read **PASS** on a build with no scaling at all, and the exclusion row would have read
  > **FAIL** because the content key was absent. *Hours went into diagnosing code that was not in
  > the jar.*
  >
  > **The instrument is the deployed jar's own bytes, not an mtime and not `git status`:**
  >
  > ```bash
  > unzip -p <deployed>.jar path/to/Class.class | tr -cd '[:print:]\n' | grep -c <newSymbol>
  > unzip -p <deployed>.jar content/<file>.yml | grep -c '^<newKey>:'
  > ```
  >
  > **`grep` on the jar itself returns 0 for everything** — a jar is a ZIP and its classes are
  > deflated, so it is an instrument that cannot express what it is being asked. **And R0 states
  > WHICH TREE the jar was built from**, because a worktree checkout makes *"the repo"* ambiguous
  > and that ambiguity is what cost the boot.
  >
  > **If R0 fails, STOP. No other row in the file is readable.**
  >
  > > ### *** AND THE RULE UNDERNEATH R0 IS NOT ABOUT JARS. IT IS ABOUT BINDING. ***
  > >
  > > ***A READING IS BOUND TO A SUBJECT, AND THE BINDING IS A SEPARATE FACT FROM THE READING.***
  > > **Every asynchronous verification must PIN its subject, not merely request it.**
  > >
  > > **NEITHER INSTANCE WAS A FALSE POSITIVE, AND THAT IS WHY THIS IS ITS OWN RULE.** Both were
  > > **true readings bound to the wrong subject** — which is worse, because **a false positive can
  > > be caught by distrusting the answer, and this cannot.** Nothing about the reading looks off.
  > >
  > > | | the reading | what it was true OF |
  > > |---|---|---|
  > > | **the wrong jar**, 2026-09-20 | R1 PASSED | the **master** jar, not the branch's |
  > > | **the wrong CI run**, 2026-09-20 | `build` SUCCEEDED | the **pre-rebase** tip, not the pushed one |
  > >
  > > The second: after a force-push, `gh run list --limit 1` returned the *previous* run, and
  > > `gh run watch` reported *"has already completed with 'success'"* — **a correct sentence about
  > > a commit that no longer existed on the branch.** The fix is one field:
  > > `gh run list --json databaseId,headSha` and wait only on runs whose `headSha` is the head you
  > > pushed.
  > >
  > > **THE GAP BETWEEN ASKING AND READING IS WHERE THE SUBJECT CHANGES**, so this covers every
  > > member of that shape — a backgrounded build, a deploy, a remote query, a scheduled job. R0 is
  > > this rule's instrument for jars; a `headSha` filter is its instrument for CI. **Same rule, two
  > > instruments, one entry.**
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

