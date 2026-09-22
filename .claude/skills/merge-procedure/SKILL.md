---
name: merge-procedure
description: Merging a PR, deleting a branch, or writing a squash body on this repo. Use when a branch is being cleaned up, absorbed, or squashed -- `git branch --merged` and two-dot `git diff` both LIE here, because every PR is squash-merged.
---

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
> | **a MARKER GREP**, after a scripted edit | **whether that string EXISTS in the file, never whether YOUR EDIT is what put it there** |
> | **a count taken in the WRONG SCOPE** | *how many are in the place I looked*, not *how many exist* |
> | **a PROXIMITY BOUND in a source scan** (`within N lines`) | *are these two statements CLOSE*, not *are they in the same method* — and in a file whose methods carry thirty lines of javadoc, that is a question about PROSE |
>
> > ### AND THE PROXIMITY BOUND IS THE ONE WHERE THE INSTRUMENT IS CORRECT AND ITS SCOPE DRIFTS UNDER IT
> >
> > **Every other row in this table is an instrument that was wrong from the start.** This one is
> > right when written and becomes wrong **without anybody touching it** — the code it measures does
> > not move, the *commentary around it* grows, and a bound of "within 30 lines" silently becomes an
> > assertion about comment length.
> >
> > **2026-09-17, three times in one slice.** `VaultWiringSignatureTest` asserted that `onClose`'s
> > write sits within 30 lines of the declaration. It failed when an UNWITNESSED note was added,
> > was widened to 60, and failed again when a disconnect branch was documented. **Neither failure
> > was a defect**, and the first arrived DURING a mutation run, where it was nearly attributed to
> > the mutation — a false red charged to the wrong cause, which is the one thing a mutation pass
> > cannot afford.
> >
> > **Practically: bound a source scan STRUCTURALLY, never by a line count.** The claim is almost
> > always *in the same method, in this order* — so find the next member declaration and bound on
> > that. It costs one helper, it needs its own control (a helper that returns `from + 1` makes every
> > ordering assertion vacuous), and it cannot drift.
> >
> > **The tell that you have one:** an assertion whose failure message is about structure and whose
> > expression contains a number nobody chose on purpose.
>
> > ### *** A ZERO IN THE WRONG SCOPE IS INDISTINGUISHABLE FROM AN ABSENCE ***
> >
> > **The other members hand you a misleading NUMBER, and a number invites a second look. This one
> > hands you a ZERO** — and zero does not read as a measurement at all. It reads as *the thing is
> > not there*, in every instrument, in every language, with no further inference required. **It
> > needs no misreading to do its damage.**
> >
> > **2026-09-17, and the instance is the reviewer's own.** Checking whether `GATE-nexus.md`'s rows
> > carried a status, `Status: NOT RUN` was grepped **inside the ROW 92+ range** and returned **0** —
> > one sentence away from being reported as *the rows carry no status*. **The string was there.**
> > **The block label sits ABOVE the rows and the row label is a different string entirely** —
> > `**Status: NOT RUN.**` against `**READING:** _(not run)_`. The scope was wrong, not the file.
> >
> > ### AND THE SECOND HALF, WHICH IS THE SAME MISTAKE FROM THE OTHER SIDE
> >
> > ***A COUNT FROM A PATTERN WHOSE SHAPE WAS NEVER STATED IS INDISTINGUISHABLE FROM A CENSUS.***
> > The same reviewer, in the same slice, produced a `64` for that row label — and **the pattern was
> > the variable, not the ref**, which is why nobody could reproduce it from the number alone. One
> > file, one moment, three answers:
> >
> > ```
> > # all three: git show 1c030e2:GATE-nexus.md | <pattern>      REPRODUCED, at that SHA
> > grep -c '^\*\*READING:\*\* _(not run)_$'            58   bare, one shape
> > grep -coiE '^\*\*READING:\*\* *_?\(not run\)_?'     59   + one trailing-clause line
> > grep -ci 'not run'                                  89   + prose, headers, commentary
> > ```
> >
> > **SHA-ANCHORED, NOT `origin/master`** — the file grows, and the same three commands over this
> > branch's working tree give `72 / 73 / 105`. **A count against a moving ref stays correct of a
> > tree the sentence no longer names**, which is the rule two bullets down from here, and the one
> > this very entry would have broken by quoting three bare numbers.
> >
> > **AND THE LOOSE TAIL FOLDED A VARIANT INTO THE FIGURE THAT WAS USED TO DENY VARIANTS EXIST.**
> > The claim attached to the count was *"one shape, zero variants"*. It is false, and the known
> > variant is this line, whose reading continues past the marker:
> >
> > ```
> > **READING:** _(not run)_ — **5c and 5d are not readable without all three of: the STAGING ROUTE
> > ```
> >
> > **The pattern that hid the variant is the pattern the no-variants claim was made from.** A strict
> > anchored count and a loose one differ by exactly the thing being asserted about.
> >
> > **Practically: a count ships with the pattern that produced it, or it is not a count.** Two
> > readings of one file that differ by 31 are both correct and answer different questions, and the
> > number alone cannot say which was asked. **Before believing a zero, run the same pattern over a
> > scope you KNOW is non-empty; before believing a total, print the pattern beside it.** Same
> > discipline as `check-absorbed.sh`'s control line, pointed at a grep.
> >
> > **WHY IT IS NOT THE FALSE-ABSENCE RULE ALREADY ON THIS PAGE.** Those four are *your word vs the
> > author's*, *a broken instrument*, *a pattern that cannot match*, and *a tool that normalises*.
> > **All four are answered by searching better.** This one is a perfectly good pattern, a perfectly
> > good tool, and a correct answer — **to a question about a region rather than about the
> > document.** Widening the search is not "searching better"; it is asking a different question.
> >
> > **Practically: before believing a zero, run the same count over a scope you KNOW is non-empty.**
> > The positive control costs one command and turns *"it is not there"* into *"it is not there, and
> > my instrument can find it when it is."* Same discipline as `check-absorbed.sh`'s control line,
> > pointed at a grep.
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
> > ### *** FIRST, THE HALF THAT IS TRUE UNDER EVERY CONFIGURATION: A MIXED TREE POISONS EVERY LATER CR COUNT ***
> >
> > **A file that is part CRLF and part LF breaks the INSTRUMENT, not the history**, and it does so
> > on every seat, under every setting. `tr -cd '\r' | wc -c` against the line count is how this page
> > tells CRLF from LF; against a mixed file it returns a number that is neither, and every reading
> > taken with it afterwards is untrustworthy. **2026-09-19: `AdapterContext.java` ended 68 lines /
> > 67 CR** — one LF line from a `\n` in a `perl` replacement — in a slice that took many CR
> > readings. **Fix a mixed file the moment the count disagrees with the line count, whatever you
> > believe about the blob.**
> >
> > ### AND THE BLOB CLAIM, WHICH IS CONDITIONAL AND USED TO BE STATED AS IF IT WERE NOT
> >
> > **EVERY COMMITTED BLOB IS LF *WHILE COMMIT-TIME NORMALISATION IS ON*.** It holds under
> > `core.autocrlf=true`, under `input`, and under a `.gitattributes` carrying `* text=auto`.
> > ***IT IS FALSE UNDER `core.autocrlf=false` WITH NO MATCHING `.gitattributes` RULE — there mixed
> > endings go into the blob verbatim.***
> >
> > **THIS REPO RESTS ON THE PER-CLONE CONFIG, NOT ON ANYTHING COMMITTED, AND THAT IS MEASURED.**
> > Read 2026-09-19: `core.autocrlf` is `true` in this clone's LOCAL config, global unset — and
> > `.gitattributes` exists but does **not** carry `* text=auto`. It names three paths only:
> > `mvnw text eol=lf`, `*.sh text eol=lf`, `mvnw.cmd text eol=crlf`. **So for a `.java` file the
> > protection is one uncommitted setting on one machine.**
> >
> > **So do not carry the conclusion, carry the condition:** the two commands that answer it are
> > `git config core.autocrlf` and reading `.gitattributes`. **A reader who inherits the bare
> > reassurance on a differently-configured clone skips the sweep and commits the mixed file** —
> > which is the same shape as a control credited with protecting something it does not touch.
> >
> > So a seat with `core.autocrlf=false` — Linux, typically — has **ZERO CR bytes in the working tree
> > AND in the blob**, and the WORKING-TREE hazard does not exist for them at all.
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

