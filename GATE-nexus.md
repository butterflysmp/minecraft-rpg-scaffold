# GATE — the Nexus: the six routes out, and the two things a unit test cannot see

## STATUS — WHICH ROWS HAVE BEEN READ, NAMED RATHER THAN COUNTED

**THIRTY-ONE ROWS ARE FULLY BOOTED — ALL OF SLICES 7, 8 AND 9, PLUS ROWS 92 AND 94.** Recounted
rather than adjusted, with an instrument validated against the figure it replaces: it reproduces
the `29` this line carried before rows 92 and 94 were read.

```
awk '/^## ROW/{r=$3} /^\*\*READING:\*\*/{if($0 ~ /PASS/ && $0 !~ /not run|VOID|PARTIAL|SUPERSEDED/) g[r]=1; else b[r]=1} END{n=0; for(k in g) if(!(k in b)) n++; print n}' GATE-nexus.md
```

**Six rows have partial readings, one row is VOID for want of staging**, and every remaining row
has none:

| row | readings | state |
|---|---|---|
| **ROW 6** | 6.1, 6.2, 6.3, 6.4, 6.4′, 6.5 — **six** | **4 PASS, 2 VOID.** Booted 2026-09-15 in CREATIVE against a row that named no mode; 6.4 and 6.5 are VOID rather than failed |
| **ROW 8** | 8a, 8b, 8c, 8d, 8e, 8f, 8g — **seven** | **3 GREEN (8e, 8f, 8g), 4 NOT RUN.** Booted 2026-09-16; the ship condition, not the whole row |
| **ROW 46** | 46a, 46b — **two** | **46a PASS; 46b NOT RUN.** Booted 2026-09-17. Superseded by a ruling the same day and UN-superseded when that ruling was reverted -- the restaging was withdrawn unread, and 46a's reading is about the shipped colour again |
| **ROWS 49–57** | ten readings over nine rows | **ALL PASS.** Run 2026-09-17, 03:58–04:00. **The first fully-booted BLOCK in this file**, and 56b turned Row 6.1's continued truth from an argument into an observation |
| **ROW 47** | one, then restaged | **PASS then SUPERSEDED BY RULING**, both 2026-09-17. It surfaced the ruling that superseded 46a while passing itself, and was then superseded by a second ruling about its own cell. **No row in this file is fully booted again** |
| **ROWS 58–65** | eight | **ALL PASS.** Run 2026-09-17, 05:25–05:27. **ROW 64 IS THE SHIP CONDITION AND ITS READING PREDATES THE MERGE IT AUTHORISED** — `#117` merged while this file still said `_(not run)_`. The decision was sound; the writing down was late, and the row carries the account |
| **ROWS 66–77** | twelve | **ALL PASS.** Run 2026-09-17, 06:48–06:52. **Row 67 is the mob row** — the only row in this file touching the real player path, and what stops a listener-less build passing the block. **Row 74's provenance flag is DISCHARGED**: its ruling was made from the javadoc, the screen agreed |

| **ROWS 92–96** | five | **2 PASS, 2 PARTIAL, 1 VOID.** Run 2026-09-17, Slice 11 PR 1, the vault's storage layer. **Rows 92 and 94 are the SOLE WITNESSES and both are FULLY witnessed** — 94 including its empty-vault control, the half most likely to be skipped. The PDC round trip and the shutdown flush have no unit rows anywhere in the project, because no module can construct an `ItemStack`. **93 and 95 are PARTIAL**: the directory check and the `/rpg stats` control were not run, and both stand NOT RUN in their rows. **96 is VOID** — no second account was online — and it was never a sole witness |

**Every other row in this file has no reading at all.**

> **AND A DISCHARGED FLAG IS RECORDED AS DISCHARGED, BECAUSE IT READS IDENTICALLY TO AN OPEN ONE.**
> Row 74 invited being overruled by the first person to open the hub at level 1. Somebody has, and
> it held. **Left unmarked, a caveat that has done its job is indistinguishable from one nobody has
> reached** — and the next reader either treats a settled question as open or re-opens it on the
> strength of a warning that is spent.

> **THREE OUTCOMES NOW APPEAR IN THIS FILE, AND THE THIRD IS NEW.** `VOID` means the conditions were
> wrong (Row 6). `FAIL` means the build was wrong (none yet). **`SUPERSEDED BY RULING` means the
> reading was CORRECT and a later decision falsified it** — Row 46a. It is not a failure of the row,
> the reader, or the build, and collapsing it into either of the other two loses which of the three
> happened.

> **AND `PARTIAL` IS NOT A FOURTH MEMBER OF THAT LIST. IT IS A ROW WITH A HALF STILL `NOT RUN`** —
> rows 93 and 95. **A PARTIAL is not a VOID**: the conditions were right, the build answered, and a
> *second* check the row itself demanded was not made. Collapsing it into PASS claims evidence that
> was never taken; collapsing it into VOID discards evidence that was.

**THE DENOMINATOR IS A COMMAND, NOT A NUMBER.** Re-derive it rather than trusting this line:

```
git grep -c '^## ROW' <ref> -- GATE-nexus.md
```

## *** AND THE COMMAND IS NOT ENOUGH: EVERY RECORDED COUNT NAMES THE REF IT WAS TAKEN AGAINST, AND THE REF IS A SHA ***

**A count against `origin/master`, or against "this working tree", is a count against a MOVING
TARGET and is stale the moment anything merges** — while the number itself stays correct, of a tree
the sentence no longer names.

```
git grep -c '^## ROW' d89cc2d -- GATE-nexus.md         # checkable forever
git grep -c '^## ROW' origin/master -- GATE-nexus.md   # true until the next merge
```

> **THIS IS THE FOURTH TURN OF THE *figure maintained by delta* WHEEL AND THE FIRST ONE RE-READING
> CANNOT REACH.** The other three are each wrong at a moment you can name, and **recounting fixes
> all three**. This one survives a recount: **re-running the command would have CONFIRMED the
> figure.**
>
> **The proof is the paragraph directly below.** It read *"65 on `origin/master` and 77 in this
> working tree"*, and `65` was a correct count — **of a tree that stopped being `origin/master`
> when slice 9 merged.** Nothing touched the number. **The figure did not go stale; its REFERENT
> MOVED UNDERNEATH IT.**
>
> **So the two halves are one rule:** the command says *how* the number was produced and the SHA
> says *what it was produced from*. **A command without a ref is reproducible and unfalsifiable** —
> you can re-run it forever and never learn that the sentence is about something else now.

**77 at `79a64a3`** (`#119`'s squash) — seventy-six integer-numbered rows plus **ROW 12b**, whose ID
is not an integer and which therefore **belongs to no range.** <b>96 in this working tree</b>, which
gained slice 10's fourteen (78–91) and slice 11 PR 1's five (92–96).

> **RECOUNTED WITH THE COMMAND, NOT ADJUSTED BY THE DELTA OF THE CHANGE THAT MOVED IT.** This line
> read `91` until slice 11 PR 1, and the temptation is to write `91 + 5`. The figure above came from
> `grep -c '^## ROW' GATE-nexus.md` run after the last row landed — which is the only form that
> survives someone else having added a row in between.

> **BOTH FIGURES ARE RECOUNTED, AND THE FIRST ONE NAMES A SHA WHILE THE SECOND CANNOT.**
> `79a64a3` is checkable forever; *"this working tree"* is the one phrase this rule cannot fix,
> because a working tree has no name. **So the working-tree figure is the perishable half and
> `79a64a3`'s is the durable one** — and when slice 10 merges, re-run the command against the new
> squash rather than assuming 91.
>
> This paragraph named `d89cc2d` until `#119` merged and moved the referent — <b>the fourth-turn
> defect it exists to describe, demonstrated on itself one merge later.</b> That is not a failure of
> the rule; it is the rule being followed: the figure was re-derived and the SHA moved with it.

**RECOUNTED with the command above, not adjusted.** It read *"65 on `origin/master` and 77 in this
working tree"* until 2026-09-17 — **true when written and falsified by slice 9's own merge**, which
is this file's own *figure maintained by delta* defect arriving through the one route re-reading
cannot reach: **the number did not change, the TREE did.** Re-run the command rather than trusting
either figure.

## *** EVERY READING LINE BEGINS `**READING:**` AND NOTHING ELSE ***

**Date, booter, outcome, conditions, restaging — ALL of it goes AFTER that prefix:**

```
  **READING:** _(not run)_
  **READING:** restaged 47 — _(not run)_
  **READING:** **2026-09-17, 03:58–04:00, booted by Ben. PASS.**
```

> **THE THREE EXAMPLES ABOVE ARE INDENTED TWO SPACES, AND THAT IS LOAD-BEARING.** Written flush
> left they **start with the prefix**, so the counting command below counts them — and the block
> explaining how to count reading lines silently added three, two of them unread. **Measured: 80
> became 83 and 47 became 49.** Caught before it shipped, by running the count over the file
> containing the new block.
>
> **Third instance in this file of prose being read as the thing it describes**, after the masthead
> cell and Row 64's note. **Do not un-indent them, and do not write a flush-left example of a
> reading line anywhere in this file.**

**THEN ONE PATTERN FINDS EVERY READING LINE FOREVER**, and `_(not run)_` / `_(never run` separates
unread from read:

```
grep -c '^\*\*READING:\*\*' GATE-nexus.md                                    # every reading line
grep '^\*\*READING:\*\*' GATE-nexus.md | grep -c '_(not run)_\|_(never run'  # the unread ones
```

At `4a7a648`: **80 reading lines, 47 unread, 33 read.** The parts add up, in one unit, on one tree.

> ### WHY THIS IS A VOCABULARY RULE AND NOT A BETTER REGEX
>
> **Measured 2026-09-17 before the pass: EIGHT distinct label shapes on one concept**, seven of
> them written in the last week — `**READING — <date>, booted by Ben. PASS.**`,
> `**READING (restaged 47):**`, and five more. **A pattern anchored on any one shape misses the
> rest, and THE SET GROWS EVERY TIME A ROW IS READ**, because a reading with history in it gets an
> elaborated label. So the next count is wrong again, by a different amount, for the same reason —
> **and the rows it misses are systematically the ones carrying the most history.**
>
> **That is exactly what happened.** A count of `^**READING:**` lines reported **45** unread; the
> true figure was **47**. The two it could not see were `restaged 46a` and `restaged 47` — *the two
> rows a ruling had already disturbed once*, which are the two least affordable to lose.
>
> **AND THE ENUMERATION COMMAND HAD THE SAME DISEASE.** `grep -o '^\*\*READING[^*]*\*\*' | sort |
> uniq -c` reports **seven** shapes and there were **eight**: the 2026-09-16 reading's closing `**`
> is on the *next* line, so the pattern cannot match it. **The instrument for counting the variants
> was itself defeated by a variant.**
>
> **SO THE FIX IS NOT A STRICTER DETECTOR — IT IS REMOVING THE THING BEING DETECTED.** Same move as
> `PAINTED_SLOTS`: one list used twice rather than two lists checked against each other. A regex
> is a detector and stays one boot behind; **a fixed prefix makes the class of error
> unrepresentable.**
>
> **The pass was mechanical, over 15 lines, and added and removed nothing** — 80 reading lines
> before and after, line count and CR count unchanged at 3153.

> ### AND THE HIGHEST ID IS 77 WHILE THERE ARE ONLY 76 INTEGER ROWS, BECAUSE **THERE IS NO ROW 48**
>
> Slice 6 ended at **47** and slice 7 opened at **49**. Nothing was deleted; 48 was simply never
> written. **Enumerated rather than inferred**, on 2026-09-17, because a maximum that exceeds a
> count is exactly the shape of a row someone has removed:
>
> ```
> for n in $(seq 1 77); do grep -q "^## ROW $n\b" GATE-nexus.md || echo "$n"; done   # -> 48
> ```
>
> **This is the second ID irregularity in this file and it is the opposite kind to the first.**
> `12b` is an ID that belongs to no range; `48` is a range position that has no ID. **Both break
> the same assumption — that the count and the highest number are the same fact** — and the
> masthead has already paid for that assumption once, in the inherited `−1` recorded below.

> ### THIS FIGURE IS RECOUNTED, NEVER ADJUSTED — AND THAT RULE IS THE POINT OF THIS SECTION
>
> **An edit that moves this count by a delta is the defect, not the maintenance.** Recount from the
> headings every time, with the command above. If you find yourself computing *"twenty-nine minus
> three"*, stop.
>
> **A SINGLE COUNT IS WHAT ROTTED, SO A BETTER COUNT IS NOT THE FIX.** The rows are named above
> because **a name cannot silently drift by six while remaining plausible**, and a number can — and
> did, through four revisions, in a file whose entire purpose is to record what was observed.

> ### THE ACCOUNT: −4 INHERITED THROUGH FOUR COMMITS, −6 BY THE LAST, OVER A −1 THAT HID BEHIND A CANCELLING ERROR
>
> Each numerator recounted from that commit's own headings — **never by adjusting the one above
> it** — with `git show <commit>:GATE-nexus.md | grep -c '^## ROW'` and the read rows named:
>
> | commit | said | read rows then | integer rows | never booted | offset |
> |---|---|---|---|---|---|
> | `d1eefe4` | nineteen of twenty-four | 6 | 24 | **23** | **−4** |
> | `837e53d` | twenty-five of thirty | 6 | 30 | **29** | **−4** |
> | `2bcadce` | twenty-nine of thirty-four | 6 | 34 | **33** | **−4** |
> | `2530378` | twenty-six of thirty-four | 6, 8 | 34 | **32** | **−6** |
>
> **THE OFFSET WAS REPORTED AS A CONSTANT −4 AND IT IS NOT ONE.** That reading was itself taken by
> subtracting, and the recount is what moved the last row. **An error found by arithmetic is not
> thereby measured by arithmetic** — the first description of this defect had to be recounted for
> the same reason the defect did.
>
> **THE FIRST THREE INHERITED. THE FOURTH MANUFACTURED TWO MORE.** Each of the first three moved the
> figure by a delta that was **correct for its own edit** and carried the base forward, so each
> author was right about the change and wrong about the base — which is why it survived three
> revisions with nobody at fault.
>
> **`2530378` IS THE ONE THAT MATTERS, AND IT IS THE ONLY ONE THAT MADE THINGS WORSE.** It exists to
> fix the status line's STALENESS, and its body says a status line that does not move when a row is
> read is this project's recurring defect. **It preserved the error inside the fix** by computing
> twenty-nine minus three instead of recounting — **and the three is where the extra −2 came from.**
> At a denominator counting ROW 8 as **one** row, reading 8e, 8f and 8g subtracts **one**, not three.
> **The category error and the arithmetic error are the same keystroke.**
>
> **AND THE DENOMINATOR CARRIED ITS OWN −1 FROM THE START, BY A MECHANISM THAT CONCEALED ITSELF.**
> `ROW 12b` entered at `d60d75a`, when the masthead had **no fraction at all**; the first fraction
> was written at `d1eefe4` against **25** headings and said *"twenty-four"*. **The count was derived
> from RANGES, and 12b is the one row whose ID is not an integer**, so no range can contain it.
>
> **BUT THE RANGE WAS ALSO WRONG, AND THE TWO ERRORS CANCELLED.** The masthead says **`SLICE 2
> (8–13)`**; that block's own heading, written in the same commit that created 12b, says **`ROWS
> 9–13, INCLUDING 12b`**. So the masthead widened the range by one at the bottom and dropped the
> qualifier at the top:
>
> ```
> the heading    9, 10, 11, 12, 12b, 13     6 rows, and it names 12b explicitly
> the masthead   8,  9, 10, 11,  12,  13    6 rows, and ROW 8 is standing in 12b's place
> ```
>
> **THE BLOCK'S TOTAL CAME OUT RIGHT — SIX EITHER WAY — SO ADDING THE RANGES UP COULD NEVER FIND
> IT.** `CLAUDE.md`'s *when a figure is given alongside its parts, add the parts up* is the cheapest
> check in this repo and it **passes on this defect**, because the substitution is one-for-one.
>
> **AND ROW 8 IS THEN IN THE SENTENCE TWICE.** By `2530378` the same masthead reads *"8a–8d remain
> NOT RUN"* **and** *"every row of SLICE 2 (8–13)"* — one row, two places, one sentence. **That
> contradiction was visible without any command, on the line being edited, in the commit whose
> subject was that line.**
>
> **`SLICE 2 (8–13)` IS BYTE-IDENTICAL IN ALL FOUR MASTHEADS.** Nobody re-derived it; each edit
> extended the list of ranges and carried the existing ones through untouched — `CLAUDE.md`'s
> *descent launders*, where **every individual step is honest** and no step is the one where the
> error entered.
>
> **THE GENERAL SHAPE, AND IT IS WHY THIS SECTION NAMES ROWS INSTEAD OF COUNTING THEM: A FRACTION
> HIDES ITS OWN UNITS.** A numerator counted in sub-rows over a denominator counted in rows is
> **still a plausible fraction** — it renders identically to a correct one, and no reader can tell
> from the figure which unit either side was counted in. **Named rows carry their granularity with
> them**, which is the one property the number could not have.
>
> **FOUND BY A READER DOING THE ARITHMETIC. NOTHING IN THIS FILE, AND NO TEST, COULD HAVE** — the
> figure is prose, the rows are prose, and nothing executes either. The pointer is in `CLAUDE.md`'s
> stale-figure entry; **this is the account**, and it is the only place the mechanism is written
> down.

> ### FOUR PREDICTIONS WERE RESTAGED ON 2026-09-16, AND EDITING A PREDICTION NORMALLY IS NOT ALLOWED
>
> Ben re-ruled four layout constants: the stats head **20 → 13**, the settings back button
> **45 → 48**, back's material **NETHER_STAR → ARROW**, and the hub's title **"Nexus" → "Nexus
> Menu"**. **Rows 14, 16, 27 and 30 were staged against the old values** and would have sent an
> operator to look at the wrong cell.
>
> **THE NO-EDIT RULE BINDS A PREDICTION THAT HAS BEEN READ, AND NONE OF THESE HAS BEEN.** The read
> rows are named in the status section above — **Row 6 and Row 8** — and none of the four is one of
> them, so there was nothing to falsify and nothing to void. **The predictions were rewritten in
> place**, which is the correct treatment for an unread row whose subject moved; the alternative is a
> gate that describes a screen that no longer exists.
>
> > **THIS SAID *"exactly ONE reading — Row 6's"* UNTIL 2026-09-16, AND IT WAS THE MASTHEAD DEFECT
> > ONE SCREEN DOWN.** Written at `9d1f5fa` and true then; falsified by `2530378`, which booted
> > 8e/8f/8g and **updated the masthead fraction without touching the sentence that counts the same
> > thing in words.** The argument above never depended on the number, which is exactly why nobody
> > re-read it. **It now points at the status section instead of restating it**, so there is one
> > place to update and no second copy to go stale.
>
> **HAD ANY OF THEM BEEN READ, THE TREATMENT WOULD HAVE BEEN THE OPPOSITE:** mark the reading VOID
> with its reason and restage beneath it, the way Row 6's creative readings were handled. **A reading
> is scoped to the conditions it was taken under, and a re-ruling is exactly a change of those
> conditions** — a stale pass on Row 30 in particular would be a CONTROL THAT STOPPED CONTROLLING,
> since its whole job is to say the hub is unchanged by everything else.
>
> **ROW 16 WAS NOT ON THE LIST AND NEEDED IT ANYWAY** — it ends *"the head is still in slot 13
> afterwards"*. Found by sweeping the file for the moved values rather than by trusting the three
> rows named. **No row reads the hub's TITLE**, so change four restaged nothing.

> ### AND A SECOND SET OF FOUR RULINGS RESTAGED MORE, INCLUDING ONE ROW THAT COULD NOT BE SAVED
>
> Ben re-ruled four more layout constants: crafting's back button **17 → 48** with the status bar
> shrinking to **SEVEN cells permanently**, the enchant screen gaining a back button **it never had
> at all**, the enchant candidate block dropping **one row** (input **10 → 19**), and the hub's
> title **"Nexus Menu" → "The Nexus"**.
>
> **Still no row reads the hub's TITLE, so that change restaged nothing for the second time
> running.** Swept, not assumed — but **the sweep needs its own caveat, because this paragraph is
> now the thing it would find.** `grep -n "Nexus Menu" GATE-nexus.md` matches **only the two lines
> immediately above and this one**: the record of the rename, never a row's expected value. The
> checkable form that is not self-polluting is `grep -n "Nexus Menu" GATE-nexus.md | grep "^1[0-9]\{3\}:"`
> — the row bodies, which return nothing.
>
> **A DOCUMENT THAT EXPLAINS WHY A STRING IS ABSENT CONTAINS THAT STRING.** Stated here rather than
> left, because the obvious sweep now reports a hit and the obvious reading of that hit is wrong.
>
> **ROW 33 WAS RESTAGED IN PLACE. ROW 34 COULD NOT BE, AND IS MARKED DEAD INSTEAD** — the
> difference is that Row 33's subject MOVED while Row 34's subject CEASED TO EXIST. A row whose
> expected value is wrong can be re-pointed; a row whose whole premise has been withdrawn has
> nothing to re-point at. See Row 34.
>
> **NOTHING HERE NAMED A CANDIDATE CELL, WHICH IS WHY THE BLOCK MOVING ONE ROW COST NO RESTAGING.**
> Measured rather than assumed — `grep -n "candidate" GATE-nexus.md` returns exactly one hit, in
> Row 8's prose about a guard, and Row 31d's only enchant-screen coordinate is the bookshelf at
> index 8, which did not move. **The candidate grid's literals live in `EnchantMenuLayoutTest` and
> nowhere else in any gate or plan file.**

**AND FROM SLICE 4b THE ROWS HAVE A SHARED PRECONDITION: the player's Nexus slot must be the
default 9 (index 8)** unless the row says otherwise. Rows **21, 24, 26 and 29 move it on purpose**
and say so in their own staging; **put it back before running an earlier row.** That is the
restaging `NexusLockTest`'s pin row has warned about since slice 1, and it is a precondition rather
than a rewrite because the rows still measure what they always did — slice 4b's block explains why.
Every row below
was written BEFORE any boot, and every expected value was recorded so that a later reading could
disagree with it. **When a row is read, its reading is written BESIDE its prediction and the
prediction is NOT edited.** A prediction revised after the fact proves nothing.

**Row 6 was booted in CREATIVE, and the row did not say so** — two of its six readings are therefore
**VOID** rather than PASS or FAIL. Row 6 carries the account. The declaration it was missing is
immediately below.

---

## GAME MODE — DECLARED, BECAUSE A ROW THAT DOES NOT SAY COSTS A BOOT TO FIND OUT

**EVERY ROW IN THIS FILE IS `/gamemode survival` UNLESS ITS OWN HEADING SAYS OTHERWISE.** The plugin
ships to a survival server; **a creative reading certifies creative**, and nothing else.

| row | mode | why it is stated rather than assumed |
|---|---|---|
| 1 | **SURVIVAL** | a creative player is hard to kill, and the row is about dying |
| 2 | **SURVIVAL** | **and the row must be read on the FRAME, not on slot 8.** In creative the held item is **not consumed** when it enters an item frame, so *"the star stays in slot 8"* is satisfied whether or not the handler fired — **half the prediction goes hollow in creative** |
| 3 | **SURVIVAL** | a crafting-table screen is a container in both modes, so this one is *believed* mode-independent — **stated as a belief, and survival is what is certified** |
| 4 | **SURVIVAL**, and **4a especially** | 4a is the own-inventory screen, which is **exactly the surface Row 8 shows behaves differently in creative**. A creative 4a reads the creative path and says nothing about the shipped one |
| 5 | **SURVIVAL** | the staging routes are commands and a `.dat` edit; none of them needs creative |
| 6 | **SURVIVAL** | **ruled after the fact** — it was booted in creative and two readings were lost to it |
| 7 | **SURVIVAL** | `/rpg health 300` is a command and works in either |
| **8** | **CREATIVE** | **its own row, not a caveat on another one.** A caveat on a row is a caveat that gets forgotten |
| **9–12b** | **SURVIVAL** | slice 2, the hub. The opener is a `PlayerInteractEvent` path and nothing in it reads a game mode |
| **13** | **CREATIVE** | slice 2's creative row, and **it exists because Row 8 taught this file not to assume the modes agree about inventory interaction** |
| **14–17, 19** | **SURVIVAL** | slice 3, the stats head. Nothing in the head's render or its lore reads a game mode; survival is what is certified |
| **20, 22, 23** | **SURVIVAL** | slice 4a. Joins, a hand-edited profile and a corrupt one — none of them reads a game mode, and survival is what ships |
| **21** | **SURVIVAL**, and **21b especially** | 21b moves an ordinary item around the **player's own hotbar**, which is the surface Row 8 shows behaves differently in creative. **A creative 21b certifies creative and says nothing about the shipped path** |
| **24** | **SURVIVAL** | it is a row about **dying**, and a creative player is hard to kill — Row 1's reason, unchanged |
| **25, 27, 28, 30** | **SURVIVAL** | slice 4b. A tooltip, two menu transitions, two refusal messages and a control — none reads a game mode |
| **31–34** | **SURVIVAL** | slice 5. Two stations, a bookshelf count and a Back button -- none reads a game mode, and 32 needs real placed blocks |
| **58–65** | **SURVIVAL** for all eight | slice 8, the star goes anywhere. **64 is the CONTROL and the reason the slice is safe to ship** -- a star in a storage cell is invisible to a world right-click, so slice 7's inventory click is the only way to open the hub from one |
| **35–47** | **SURVIVAL**, and **seven of the thirteen are VOID in creative** | slice 6, the grindstone. **Creative hides the XP bar**, so every row that reads a refund -- 35, 36, 37, 40, 41, 42 and 46 rest on it directly or on the button's figure -- has nothing on screen to read. **That is Row 6's lesson applied BEFORE the boot rather than after it cost two readings** |
| **49–57** | **SURVIVAL**, except **56 which stages BOTH** | slice 7, the star click. **56b is the creative half and is the row that keeps 6.1 honest** -- the arm is keyed LEFT/RIGHT and creative clicks arrive as ClickType.CREATIVE, so creative refuses BY CONSTRUCTION. That is true and unobserved until 56b is read |
| **66–77** | **SURVIVAL** for all twelve | slice 9, player level. **67 and 68 read the vanilla XP BAR against the player level**, and creative hides the bar — so both would be **VOID rather than failed**, which is Row 6's lesson for the second block running. The other ten stage with `/rpg playerxp`, which needs no mode, and are declared survival because **that is what ships** rather than because they read one |
| **26, 29** | **SURVIVAL**, and **26c especially** | 26c reads a **displaced item** surviving, and 29 counts hotbar cells. Creative makes items free, so *"the item is still there"* is satisfied for nothing — the register's own shape: **creative removes a cost, and a row whose reading is "the thing is still there" passes without exercising anything** |
| **18** | **SURVIVAL**, and **this one is load-bearing** | it moves an item **in the player's own inventory with a menu open** — the exact surface Row 8 shows behaves differently in creative. **A creative reading of 18 certifies creative and says nothing about the shipped path** |

> ### THIS TABLE WAS MALFORMED FOR A SLICE AND RENDERED WITHOUT COMPLAINT
>
> **Found 2026-09-17.** The `35–47` row had **two** cells and the `49–57` row had **four**: slice 6's
> explanation was sitting on slice 7's line, pasted in by an earlier splice. **Nothing objected.** A
> markdown table with the wrong cell count renders — short rows get blank cells, long rows get their
> tail dropped or shown, and either way the page looks fine.
>
> **That is the same family as a grep that reads commentary as content: THE INSTRUMENT ACCEPTS WHAT
> IT SHOULD REJECT AND SAYS NOTHING.** The renderer is not broken; it is lenient, and leniency is a
> default far more often than anyone assumes.
>
> **The check costs one line and needs no tool:** count the pipes per row and require them equal.
>
> ```bash
> awk '/^\|/ {n=gsub(/\|/,"|"); if (n-1 != 3) print NR": "(n-1)" cells"}' GATE-nexus.md
> ```
>
> Every other row in this table reported **3**. Those two reported **2** and **4**.

> **CREATIVE GETS A ROW, NOT A FOOTNOTE, AND THAT IS THE WHOLE LESSON OF ROW 6.** The alternative —
> *"row 6, but note it behaves differently in creative"* — is a sentence that survives exactly until
> someone reads row 6 and boots it. **A mode that changes the answer is a different row.**

> **THIS FILE'S OWN DEBT IS NOW PAID AND 16 OTHERS' ARE NOT.** Measured at `e9b3e0e` across all 19
> `GATE-*.md`: only `GATE-quiver-ammo.md` and `GATE-crafting.md` declare a mode. See Row 6's reading
> for the measurement and for the two files that **look** like they declare one and do not.

**The unit suite covers the DECISION, not the DELIVERY.** `NexusLockTest` has 21 rows over
`NexusLock`, and `NexusWiringSignatureTest` has 5 over the annotation wiring. Neither can see a
live `InventoryView`, a real `PlayerInventory` layout, or what the vanilla client does with a
cancelled event — the project has no MockBukkit, and `new ItemStack(...)` throws without a running
server. **That gap is this file.**

**Seven mutations were run against the unit suite before this file was written**, and their kill
sets are recorded at the bottom, because a row here that duplicates a mutation's coverage is a boot
slot spent on something already measured.

---

## WHAT THE UNIT SUITE ALREADY SETTLED, SO NO ROW RE-ASKS IT

- The verdict for every `ClickType`, both cursor states, every `InventoryAction` — 21 rows.
- That the locked slot is refused **whether or not it currently holds a star** (mutation M5).
- That a stray star is refused **wherever it sits** (mutation M6).
- That a menu's own slot 8 is **not** the locked slot.
- That the two annotation attributes the lock rests on are present and strictly ordered (M2).

**None of that is re-asked below.** These rows are the parts that are not decidable off-server.

---

## ROW 1 — THE CURSOR AT DEATH

**THE ONE OPEN QUESTION INHERITED FROM THE BRIEF, AND IT WAS RIGHT TO DOUBT IT.**
`RpgListeners.onPlayerDeath` sets `keepInventory(true)` and clears `getDrops()`. A cursor item is
**not a `getDrops()` entry** — it becomes a separate item entity — so `getDrops().clear()` cannot
reach it. The handler calls `closeInventory()` first, which returns the cursor only when one of OUR
menus is open, and then only through `MenuSafety.give`, which **drops at the player's feet when the
inventory is full**.

**Staging.** Three sub-rows, deliberately different so no two quantities in the row are equal:

| | screen open at death | inventory state |
|---|---|---|
| 1a | one of our menus (crafting) | 3 free slots |
| 1b | vanilla chest | 3 free slots |
| 1c | vanilla chest | **0 free slots** |

Put a **non-Nexus** item on the cursor for each (a stack of 7 cobblestone — 7 so it cannot be
confused with a count of 1, 3 or 8 anywhere else in this file), then die.

**PREDICTED:** 1a returns the cobblestone to the inventory. 1b and 1c are **UNKNOWN** — that is the
point of the row. 1c may drop at the feet even in the 1a shape.

**READING:** _(not run)_

**Why a non-Nexus item:** the star should be unable to reach the cursor at all, so staging with the
star would be testing two things at once and would fail to distinguish "the cursor path is safe"
from "the star never got there". Row 6 tests the second claim separately.

---

## ROW 2 — THE ITEM FRAME, AND THE ARMOUR STAND THAT NEEDED ITS OWN HANDLER

**MEASURED OFF-SERVER, STILL UNCONFIRMED IN PLAY.** Read from the pinned `paper-api` jar's constant
pools:

```
PlayerInteractEntityEvent        declares getHandlerList/getHandlers
PlayerInteractAtEntityEvent      declares NEITHER  -> inherits, so onNexusGiveToEntity receives it
PlayerArmorStandManipulateEvent  declares BOTH     -> its OWN HandlerList, so it does NOT
```

That is why there are two handlers and not one. **A wider NAME on a single handler would have left
the armour stand open**, which is the shape the route was nearly closed with.

**Staging.** Hold the star. In order: right-click an **item frame**, a **glow item frame**, and an
**armour stand with arms**.

**PREDICTED:** all three refuse. The star stays in slot 8, nothing enters the frame or the stand,
**no message and no sound**. Then repeat each with an ordinary cobblestone to confirm the handlers
are not refusing everything — **that control is the row**, because a handler that refuses every
interaction looks identical to one that refuses correctly.

**READING:** _(not run)_

---

## ROW 3 — THE PERFORMED ROUTES, WHICH ARE THE WHOLE REASON FOR THE PRIORITY ARRANGEMENT

**These two are live today and a cancel at a LATER priority cannot stop either**, because
`MenuRouting` performs them by calling `setCurrentItem` / `setItem` directly rather than by
un-cancelling.

**Staging.** Open a crafting table (hijacked to `CraftingMenu`, whose `acceptsInput` returns `true`
unconditionally over a `STACKING` grid).

- **3a** — shift-click the star from the hotbar toward the grid.
- **3b** — hover an empty grid cell and press **9**.
- **3c** — hover an empty grid cell and press **5** (an ordinary hotbar slot holding cobblestone).

**PREDICTED:** 3a and 3b refuse, star unmoved. **3c SUCCEEDS** — the cobblestone moves into the
grid. 3c is not padding: without it, 3a and 3b passing is equally consistent with the guard having
broken the crafting menu outright, which is the failure mode the whole design is shaped around.

**READING:** _(not run)_

---

## ROW 4 — THE COORDINATE CONVERSION, IN BOTH VIEWS

**THE ROW `NexusLockTest` EXISTS TO BE CHECKED AGAINST.** The lock works in `PlayerInventory` index
space; `NexusSlots` converts with `view.getInventory(raw)` + `view.convertSlot(raw)`. The raw
numbers differ per view and **raw 8 is the BOOTS slot in the own-inventory screen**:

| physical slot | own-inventory screen | 54-slot chest open |
|---|---|---|
| locked hotbar 8 | raw **44** | raw **89** |
| boots | raw **8** | raw **85** |

**Staging.** Wearing boots, with the star in slot 8:

- **4a** — press E (own inventory). Left-click the **boots**. Then left-click the **star**.
- **4b** — open a **double chest**. Left-click the **boots**. Then left-click the **star**.

**PREDICTED:** in both views the **boots move freely** and the **star does not**. A conversion bug
that compared raw numbers would invert exactly this in 4a — refusing the boots, permitting the star
— and would look like a working lock to anyone who only tested one of the two.

**READING:** _(not run)_

---

## ROW 5 — JOIN CONVERGENCE, INCLUDING THE CASE THAT DESTROYS AN ITEM IF IT IS WRONG

**The highest-stakes row in the file.** Every existing player joins once with slot 8 already
occupied.

**Staging.** Four sub-rows, each a separate join:

| | slot 8 before join | star elsewhere | free slots |
|---|---|---|---|
| 5a | diamond sword | none | 5 |
| 5b | diamond sword | none | **0** |
| 5c | cobblestone x13 | star in slot 3 | 5 |
| 5d | **the star** | a second star in slot 3 | 5 |

### 5c AND 5d HAVE NO IN-GAME ROUTE TO THEIR STARTING STATE, SO THE STAGING IS PART OF THE ROW

**Caught BEFORE the boot rather than after, which is the good version of finding it.** Both sub-rows
need a Nexus star resting at **slot 3** before the join, and 5d needs a **second** one. Neither
condition can arrive by playing — every route to it is refused by the guard the row exists to test:

| the route to slot 3 | what refuses it |
|---|---|
| left / right / shift-click the star at 8 | `touchesTheStar`, `LOCKED_SLOT` arm — the clicked index IS 8 |
| number key while hovering the star | the same arm, via the clicked slot |
| hover slot 3, press **9** | the same arm, via `getHotbarButton()` — the `NUMBER_KEY` arm contributes `(true, 8)` |
| **F** | `onNexusSwapHand`, and `SWAP_OFFHAND` contributes `(true, 40)` besides |
| **Q**, in a screen or in hand | the `DROP` / `CONTROL_DROP` arm, and `onNexusDrop` for the in-hand case |
| drag off the star | you cannot start one — picking it up is refused, and `refusesDrag` refuses a star on the cursor anyway |
| creative middle-click | `InventoryCreativeEvent` inherits `InventoryClickEvent`'s `HandlerList`, so it **REACHES** `onNexusClick` — **measured below, and reaching is NOT refusing: see the correction under the table** |
| minting a second star | `NexusItems.mint` has **exactly one call site**, inside `converge`, and it mints only when none is present |

**And `converge()` runs only ON JOIN**, which is the thing being measured — so the row cannot stage
itself by converging first.

**The creative row is MEASURED, not assumed**, because own-`HandlerList` inheritance is the exact
shape that produced defect 5 of `#92`. `javap -p` against the pinned
`paper-api 26.1.2.build.74-stable`:

```
InventoryCreativeEvent           declares NEITHER  -> inherits, so onNexusClick DOES receive it
PlayerInteractAtEntityEvent      declares NEITHER  -> inherits            (CONTROL, agrees with #92)
PlayerArmorStandManipulateEvent  declares BOTH     -> its own HandlerList (CONTROL, agrees with #92)
```

The two control lines reproduce `#92`'s recorded readings from the same jar on the same run. **An
instrument that finds nothing looks identical to one that is not looking**, so it is made to print a
known-present positive alongside the answer being sought.

> ### CORRECTION, 2026-09-15 — THE MEASUREMENT IS RIGHT AND THE COLUMN IT SITS IN IS WRONG
>
> **REACHING A HANDLER IS NOT BEING REFUSED BY IT.** Inheritance of the `HandlerList` is **NECESSARY**
> for refusal and **NOT SUFFICIENT**: it establishes that the handler is *invoked*, and says nothing
> about whether cancelling it stops the write. The `javap` reading above is correct, reproducible, and
> was filed under a column heading — *what refuses it* — that claims more than it measured.
>
> **Row 6's reading is the refutation, from the same feature on the same jar.** In creative, in the
> own-inventory screen, a number-key gesture **reaches the handler, the handler cancels, and a real
> item still lands in slot 1.** So at least one creative route to a second star is NOT refused, and
> the table asserted that all of them were.
>
> **THE SHAPE, AND IT IS THE REUSABLE PART: A NECESSARY CONDITION, MEASURED CORRECTLY, FILED AS A
> SUFFICIENT ONE.** Nothing about the measurement was sloppy — it was made against the pinned jar,
> with two positive controls, precisely because the author knew an instrument can fail to look. **The
> defect is one column to the left of the number.** A measurement inherits the claim of the heading it
> is placed under, and no control on the measurement can detect that, because the control is checking
> the instrument and the error is in the filing.
>
> **Practically: when a row in a "what refuses / what prevents / what guarantees" table is a
> MECHANISM rather than an OBSERVED REFUSAL, say which it is in the cell.** A cell reading *"reaches
> the handler"* under *what refuses it* is answering a different question from the one the column
> asks, and it reads as an answer.
>
> **What this does NOT invalidate:** rows 5c and 5d still have no in-game route to their starting
> state via any gesture the lock names — the seven other rows of the table are untouched, and the
> creative number-key route is a **defect**, not a sanctioned staging route. Staging still uses Route
> A, B or C below.

**THE READING IS VOID WITHOUT THE ROUTE.** A star placed by a staging route and a star stranded by a
real defect are not the same starting state, so **the reading names which route it used** or it
records an ending state with no known starting one.

**Route A — `/item replace … from …`, which COPIES a stack with its components.** No restart, no NBT
editor, no second account. It raises no inventory event, so the lock never sees it: exactly the class
of route `NexusSlots.converge`'s own javadoc names as the reason convergence exists rather than
mint-if-absent — *"a direct server-side `setItem`, which raises no event at all and so cannot be
refused"*. Hotbar index *n* is `hotbar.n`, so the locked slot is `hotbar.8` and slot 3 is `hotbar.3`.

- **5d** — one command, and the copy is byte-identical to the original **by construction**:

  ```
  /item replace entity @s hotbar.3 from entity @s hotbar.8
  ```

  Leave slot 8 alone. That is 5d's starting state: the star at 8, a second at 3.

- **5c** — the same copy FIRST, then overwrite the original. **In that order** — the reverse destroys
  the source before it has been copied:

  ```
  /item replace entity @s hotbar.3 from entity @s hotbar.8
  /item replace entity @s hotbar.8 with minecraft:cobblestone 13
  ```

  That leaves exactly one star, at 3, with 13 cobblestone at 8.

> **The `from entity` form is UNCONFIRMED on 26.1 and is written here unverified.** It is offered
> first because its failure is **LOUD** — an unparseable command errors at the console — and because
> tab-completion after `/item replace entity @s hotbar.3 ` settles it in one keystroke. If it is not
> there, fall to Route B or C rather than improvising.

**NEVER STAGE THE SECOND STAR WITH `with minecraft:nether_star`.** That mints an UNTAGGED lookalike
— row 7's whole subject — which `isNexus` rejects, so `converge` correctly leaves it where it sits
and the reading presents as *"the surplus star was not deleted"*. **A hollow fixture reported as a
defect in the code it was meant to test**, and it is the one wrong turn this row makes easy.

**Route B — the offline copy.** Stop the server and, in `world/playerdata/<uuid>.dat`, **COPY the
existing star's stack** into the slot-3 entry rather than authoring one. A copy cannot get the tag
wrong; a hand-written tag can, and its failure is the silent one above. The tag is `rpg:nexus` — a
`BYTE` under `new NamespacedKey(plugin, "nexus")`, the namespace being `paper-plugin.yml`'s
`name: Rpg` lowercased.

**Route C — the plugin-absent boot**, which is *"disable the plugin, move the star, re-enable"* made
executable: **Paper has no `/plugin disable`**, so the window is opened by removing the jar. Stop the
server, `rm -f run/plugins/rpg-*.jar`, boot the server **directly** — `cd run && java -jar paper.jar
--nogui`, because `dev-server.sh` re-deploys the jar even under `--no-build` — move the star by hand
with no guard registered, quit, restore the jar, boot normally, join.

> **AND THE TRAP THAT MAKES ROUTE C THE RISKIEST OF THE THREE: KILLING THE SCRIPT DOES NOT KILL THE
> SERVER.** Ctrl-C on `dev-server.sh` leaves `java` running. It holds `rpg-*.jar` open, so `rm -f`
> fails with *"Device or resource busy"* — and the boot you then take for plugin-absent is the old
> plugin-PRESENT server still answering, which refuses every move and reads as Route C being
> impossible. **Confirm no surviving `java` process before removing the jar, and confirm the jar is
> gone afterwards.** Same file lock as `CLAUDE.md`'s `*** MUTATION STILL IN DEPLOYED JAR ***` entry.

### COMMON TO ALL THREE ROUTES — AND THE HAZARD THAT WOULD PASS A ROW THAT TESTED NOTHING

**`converge` RUNS ON RESPAWN, NOT ONLY ON JOIN.** Two call sites, measured: `RpgListeners:433`
(`onJoin`) and `RpgListeners:1146` (`onPlayerRespawn`). `NexusSlots.converge`'s own javadoc says why —
`onQuit` does not run on death, so a star lost at death would otherwise be missing until the next
reconnect.

**SO A DEATH BETWEEN STAGING AND QUITTING SILENTLY CONVERGES THE STAGING AWAY.** The surplus is
deleted, the survivor promoted into slot 8, and the operator then quits and joins into an
**ALREADY-CONVERGED** state — seeing one star in slot 8, which is *exactly 5d's predicted end state*,
and ticking **PASS**. The row passes on a starting state that stopped existing before the join.

**That is not hypothetical in this file's own boot order.** Row 1 is the DEATH row and it sits
immediately before row 5. An operator working the sequence dies three times, then stages row 5.

> **Stage, run the control, quit. Do not die in between.** It binds Routes A, B and C alike: A stages
> in a live session where death is one mob away, and B and C both end with a join, after which any
> death before the measured one has the same effect.

**THE PRE-JOIN CONTROL — AND IT HAS TWO JOBS, WHICH IS WHY BOTH ARE NAMED.** Before quitting, in the
own-inventory screen:

- Left-click the item at **slot 3** — it must be **REFUSED**. `touchesTheStar`'s second arm refuses a
  star *wherever it sits*. It is independent of `converge`, and it is non-destructive: a refusal
  changes nothing, so the staging survives the check.
- Left-click an ordinary item elsewhere — it must **MOVE**. Without it, a refusal at slot 3 is
  equally consistent with the guard refusing everything, which is the control every other row in this
  file carries.

**A FAILED CONTROL HAS TWO CAUSES AND ONE SYMPTOM, SO THE SYMPTOM IS NOT THE READING.** The control
was first written to prove only that *the copy carried the tag*. It also catches *a respawn-converge
that has already run*, and both present as "the left-click at slot 3 was permitted":

| what slot 3 shows on a failed control | what happened | what to do |
|---|---|---|
| a star IS there, and it **MOVES** | the copy is **UNTAGGED** — the tag did not carry | re-stage; never with `with minecraft:nether_star` |
| slot 3 is **EMPTY**, one star at 8 | **you died** — `converge` already ran on respawn | re-stage, and do not die before quitting |

**Do not collapse those two into "the control failed".** One observation, two causes, no way to tell
them apart — the defect shape this whole file is about, and it does not stop being that shape because
it turned up inside the control written to close it.

**And for 5c only: no other cobblestone anywhere in the inventory.** The displaced stack goes through
`MenuSafety.give` → `addItem`, and `MenuSafety.fits`'s javadoc records that fill order in its own
words — *"partial matching stacks first, then empties"*. A second cobblestone stack therefore absorbs
the displaced 13, and the *"Count still 13"* reading is **destroyed rather than failed**.

**PREDICTED:**

- **5a** — sword moved to a free slot, star minted into 8. **Sword not destroyed.**
- **5b** — sword dropped at the player's feet **with the message** *"Your inventory was full --
  dropped at your feet."* Star in 8. **Sword not destroyed.**
- **5c** — star moved from 3 to 8; the 13 cobblestone land in a free slot (3 is now free, so most
  likely there). **Count still 13.** Thirteen because it collides with nothing else in this file.
- **5d** — one star remains, **in slot 8**, and no Nexus star sits anywhere else.

> **5d's PREDICTION WAS CHANGED WHILE THE STAGING WAS WRITTEN, AND THE OLD WORDING IS RECORDED
> BECAUSE IT WAS THE OPPOSITE OF WHAT THE CODE DOES.** It read *"the surplus star at slot 3 is
> **deleted**"*. Traced: `converge` collects `stars = [3, 8]` in index order, deletes every entry
> **after the first** — `setItem(stars.get(1), null)`, which is the star at **8** — then promotes the
> survivor at 3 into 8. The star that dies is slot **8**'s.
>
> **The row cannot read which one died, and that is why the prediction is an END STATE.** Both stars
> are byte-identical under every route above, so no observation distinguishes them. Staging with
> *distinguishable* stars to recover the mechanism would test convergence against a state convergence
> cannot produce — `mint` makes them identical — so a future reader must not turn this bullet back
> into a claim about WHICH star was deleted.
>
> This is a change to a prediction in a file whose own rule is that predictions are not edited. The
> rule binds **after a row is read**; this file is `Status: NOT RUN`, nothing has been read, and the
> old sentence would have sent the first reading to the wrong conclusion.
>
> > **THAT LICENCE HAS SINCE LAPSED, 2026-09-15, AND IT IS NOTED HERE RATHER THAN LEFT TO BE
> > RE-QUOTED.** Rows have since been read — **the status section at the top of this file names
> > which, and is the only place that does.** The paragraph above is a correct account of a change
> > made while nothing had been read, and it is **no longer a licence to make another one.** A
> > precondition that lapses silently is `CLAUDE.md`'s own *control carried past its precondition*:
> > 5c and 5d are still unread, but the sentence *"nothing has been read"* is now false of this file
> > and cannot be cited again as written.

**READING:** _(not run)_ — **5c and 5d are not readable without all three of: the STAGING ROUTE
(A, B or C), the outcome of the PRE-JOIN CONTROL, and the statement that NO DEATH OCCURRED between
staging and quitting.** The first two record what the starting state was; the third records that it
still existed at the join, because `converge` runs on respawn and a death silently replaces the
staged state with the row's own predicted end state. A reading missing any of the three records an
ending state with no known starting one — uninterpretable later, and not re-runnable.

**5d is the only deletion this feature performs and it is deliberate.** A surplus Nexus star is
plugin-minted, worth nothing and re-minted free; the guarantee is *never destroys a PLAYER's item*,
and the sword in 5a/5b is what that guarantee is about.

---

## ROW 6 — THE SILENT REFUSAL, AND THE CLIENT RESYNC THAT MAKES IT TRUE

**Ben's ruling: refusal is SILENT.** No message, no sound, nothing in chat. The player can see the
star did not move.

**THE RESYNC IS WHAT MAKES THAT LAST CLAUSE TRUE RATHER THAN MERELY INTENDED.** A cancelled
`InventoryClickEvent` leaves the client rendering the move until a resync arrives, so without
`player.updateInventory()` the player sees the star *did* move and then snap back — the ruling
violated in the visible direction. Every refusal path calls it.

**Staging.** With no menu open, press E and try, in order: pick up the star; drag from it; press Q
on it; press F on it; number-key it to slot 1. Watch the slot, not the chat.

**PREDICTED:** the star never appears to leave slot 8 even momentarily. No chat line, no sound, no
title. **If a flicker is visible on any of the five, name which** — the resync is per-path and a
flicker on one is not a flicker on all.

**READING:** **2026-09-15, booted by Ben. CONDITIONS: CREATIVE mode, own-inventory screen (E).**
The prediction above is untouched.

| | gesture | reading |
|---|---|---|
| 6.1 | pick up the star | **PASS** |
| 6.2 | drag from it | **PASS** |
| 6.3 | Q on it | **PASS** |
| 6.4 | F, star in the INVENTORY | **VOID** — a residual is left in the **OFFHAND** |
| 6.4′ | F, star **IN HAND** (no screen) | **PASS** |
| 6.5 | number-key it to slot 1 | **VOID** — the star returns to slot 8 **and a second star appears in slot 1**, persisting until `/clear` |

**Six readings from five staged gestures** — 6.4 was read twice, in-screen and in-hand, and the two
are different code paths rather than a repeat. **Four PASS, two VOID.** No chat line, no sound, no
title on any of the six, and **no flicker was named on any path**, so the resync claim is unrefuted
as far as this boot reaches.

**6.4 AND 6.5 ARE VOID, NOT FAILED, AND THE DIFFERENCE IS THE WHOLE POINT.** They were read under a
condition **this row never pinned and which decides the answer**: re-tested and confirmed, both
reproduce **ONLY in creative** and **ONLY in the own-inventory screen** — with a **CHEST open they do
not reproduce at all**. The row names no game mode. So there is no verdict to give, only a reading
whose scope cannot be interpreted; writing FAIL here would assert a survival defect this boot never
measured, and writing PASS would assert a creative one it never measured either.

> **THE OMISSION IS ITSELF THE FINDING, AND IT IS NOT LOCAL TO THIS ROW.** Measured at `e9b3e0e`
> over all 19 `GATE-*.md`, case-insensitively, for
> `gamemode|survival|creative|adventure|spectator`:
>
> ```
> 15 files   ZERO hits                                     declare no mode
>  1 file    GATE-nexus.md            3 hits               all three are InventoryCreativeEvent
>                                                          MECHANISM -- no boot condition
>  1 file    GATE-vanilla-damage.md   1 hit                "nearly recorded as a survival" --
>                                                          the word, not the mode
>  1 file    GATE-crafting.md         2 hits               DECLARES: "In creative mode", and a
>                                                          "survival-mode player" setup
>  1 file    GATE-quiver-ammo.md      8 hits               DECLARES, per row: "/gamemode survival
>                                                          rows 1-3 and 5. Row 4 is the creative one"
> ```
>
> **So the convention is applied in 2 files of 19, not 4** — and the two that look like it on a grep
> are `CLAUDE.md`'s FALSE PRESENCE, exactly: *prose that names a key is indistinguishable from the
> key.* `GATE-vanilla-damage.md`'s `survival` is a fall the player lived through. **That omission cost
> two of the six readings in this row.** The file-level fix and the standing debt for the other 16
> are tracked separately; this row records only what it cost here.

**THE PATTERN, WHICH IS THE PART THAT GENERALISES: the two VOID gestures are EXACTLY the two
`ClickType` arms in `NexusLock` whose touched set has a SECOND member.**

```
NUMBER_KEY     Set.copyOf(List.of(clicked, new Touched(true, hotbarButton)))   -> 6.5 VOID
SWAP_OFFHAND   Set.copyOf(List.of(clicked, new Touched(true, OFFHAND_SLOT)))   -> 6.4 VOID
every other arm          Set.of(clicked)                                       -> PASS
```

Measured against the switch, not inferred from the readings: those are the only two arms in it with
more than one member. **Every one-slot gesture passed; both second slots are in the failing set.**

**SO THE DECISION CLASS IS RIGHT, AND `NexusLock` IS NOT WHAT IS WRONG HERE.** The lock names both
second slots and refuses both; the residual arrives *after* a refusal that fired. The split is
evidence **for** the two-member arms, not against them — a lock that named only the clicked slot
would have produced the same two symptoms with no refusal behind them at all, and nothing to
distinguish the two cases.

**WHAT THIS READING FALSIFIES ELSEWHERE IN THIS FILE:** the 5c/5d staging table's creative row, which
reads *"`InventoryCreativeEvent` inherits … so it reaches `onNexusClick`"* under the column heading
**WHAT REFUSES IT**. The measurement is correct and the filing is not: **reaching a handler is
necessary for refusal, not sufficient.** Tonight is the evidence — the event reaches the handler, the
handler cancels, and a real item still lands in slot 1. Corrected in the table itself.

---

## ROW 7 — THE DEV STAR STILL WORKS

**The collision this design exists to survive.** `HealthModifierItems.mint` mints a `NETHER_STAR`
for `health_boost_TEMP`. The Nexus star is keyed by `keys.nexus`, never by Material.

**Staging.** `/rpg health 300` to mint a dev star. Confirm it is a nether star. Then drop it,
shift-click it into a chest, and stack-test it beside the Nexus star.

**PREDICTED:** the dev star **moves freely** — dropped, chested, picked up — while the Nexus star
does not. They **do not stack**, both because their PDC differs and because the Nexus star pins
`setMaxStackSize(1)`.

**READING:** _(not run)_

**A Material-keyed lock would fail this row in the most confusing possible way**: the dev star
would become undroppable and the bug would present as "the health item is stuck", nowhere near the
Nexus.

---

## ROW 8 — CREATIVE, THE OWN-INVENTORY SCREEN, AND THE STAR THAT DUPLICATES

**`/gamemode creative`. THIS ROW'S MODE IS THE ROW.** It exists because Row 6's 6.4 and 6.5 were
read here and are VOID against a survival gate. **Status: PARTIALLY RUN — 8e, 8f and 8g were booted
2026-09-16 and are GREEN; 8a–8d have never been booted.** Every prediction below was written before
any boot of this row, and the readings sit beside them rather than replacing them.

> **THIS SAID `Status: NOT RUN` UNTIL 2026-09-16, AFTER 8e/8f/8g HAD BEEN READ AND RECORDED IN THIS
> SAME SECTION.** `2530378` added the three readings and moved the masthead fraction, and **left this
> line — the one a reader hits FIRST — saying the row had never been booted.** That is the defect
> `2530378`'s own body names: a status line that does not move when a row is read. **The readings
> were never hidden; the header contradicted them**, and a contradiction between a header and the
> table beneath it is resolved by whichever the reader stops at.

### WHAT IS ALREADY KNOWN, SO THE ROW DOES NOT RE-ASK IT

Measured off-server from the pinned `paper-api 26.1.2.build.74-stable` with `javap -p`:

```
InventoryCreativeEvent extends InventoryClickEvent
  private ItemStack item;
  public InventoryCreativeEvent(InventoryView, InventoryType$SlotType, int slot, ItemStack newItem)
  public ItemStack getCursor()            <- OVERRIDDEN. Returns `item`, the NEW STACK for the slot
  public void setCursor(ItemStack)
```

**THE EVENT CARRIES ONE SLOT AND THE ITEM BEING WRITTEN INTO IT.** That is the shape of a
set-creative-slot packet, not of a container click — and `getCursor()` on this subclass does not mean
*"what is on the cursor"*, it means *"what this slot is about to become"*.

### THE MECHANISM, DEDUCED FROM THE UNIT TABLE AND THE READING RATHER THAN FROM THE CLIENT

**The deduction needs no knowledge of vanilla packets, which is why it is stated first.** A
number-key gesture arriving as **one** `InventoryClickEvent` with `ClickType.NUMBER_KEY` is refused
by `NexusLock` under either staging — hovering the star makes `clicked` the locked slot, and hovering
the destination makes `getHotbarButton()` contribute `(true, 8)`. `NexusLockTest` pins both.
**The gesture was NOT fully refused. Therefore it did not arrive as one event.** The identical
argument applies to F, which `swapOffhandIsREFUSEDFromBothEnds` pins from both ends.

**So the gesture arrives DECOMPOSED — two independent single-slot writes:**

```
write A   slot 8  <- AIR      clicked = LOCKED_SLOT           -> REFUSED   (star returns to 8)
write B   slot 1  <- the star clicked = 1, no star at 1 yet   -> PERMITTED (a second star appears)
```

**AND THE RULE IS COMPLETE ONLY IF THE GESTURE ARRIVES WHOLE.** `NexusLock` asks *"does this touch
the star?"* — a question about **slots**. Write B touches no star: slot 1 is empty at the moment the
event fires, and the star is not on the cursor in the ordinary sense. **The half that moves the star
is refused; the half that CREATES one is innocent by the rule as written.** That is not a hole in the
decision class, it is the decision class being asked about half a gesture.

> **THE OPERATOR'S HYPOTHESIS, CONFIRMED IN ITS ESSENTIAL CLAIM AND REFINED IN ITS REASON.** It read:
> *"in creative the client is authoritative over its own inventory screen and sends set-slot packets
> the server applies with little validation, rather than the container-click packets a chest view
> uses."* **The authoritative-client half is confirmed** — the decomposition, the event's one-slot
> shape, and the chest's non-reproduction all agree. **The "little validation" half is withdrawn:**
> the server validated fine and our guard fired correctly on write A. What defeats the guard is
> **decomposition, not laxity.** The distinction matters because it predicts where else to look:
> anywhere a multi-slot gesture is delivered as independent single-slot writes.

> **AND THE READING ALREADY PROVES `InventoryCreativeEvent` IS CANCELLABLE HERE, WHICH THE FIX RESTS
> ON.** Write A *was* refused — the star returned to slot 8 — so a cancel on this event is honoured
> by this server on this path. **That is an empirical result from Row 6, not an assumption about
> CraftBukkit internals**, and it is the reason the fix below is one guard rather than a new
> mechanism.

### THE STAGING

**Every sub-row states its own expected `/data` reading, because the whole question is whether the
server agrees with the screen.**

| | gesture, in creative, own-inventory screen (E) | then |
|---|---|---|
| 8a | number-key the star from slot 8 to slot 1 | `/data get entity @s Inventory` **from the console** |
| 8b | F on the star in the inventory | `/data get entity @s Inventory` from the console |
| 8c | 8a, then **quit and rejoin** | `/data get entity @s Inventory` from the console |
| 8d | **`/gamemode survival`**, then repeat 8a and 8b | the CONTROL |

**PREDICTED, 8a:** the console read shows **TWO** `rpg:nexus`-tagged stacks — one in the **hotbar
slot 1** entry, one in the **hotbar slot 8** entry. **That is the row's answer to "real or client
artifact": REAL.** It is predicted rather than assumed because *"it persisted until `/clear`"* is
strong but is still a screen reading — `/clear` is server-side, but what it removed was named by the
client.

> **THE PREDICTION IS A COUNT AND A PLACE, NOT AN NBT SLOT NUMBER, DELIBERATELY.** The classic
> encoding puts the offhand at `-106b` and armour at `100b`–`103b`, and **26.1 is not assumed to
> match it** — the inventory NBT layout is exactly the sort of thing a year-versioned drop rewrites.
> **Read the numbering off the first `/data` output and write it down beside the reading**; a
> prediction naming a number nobody has checked would fail on the encoding and be recorded as a
> failure of the star.

**PREDICTED, 8b:** **TWO** tagged stacks, one in the **hotbar slot 8** entry and one in the
**OFFHAND** entry. The offhand residual and the slot-1 duplicate are **the same defect on two arms**,
not two defects.

**PREDICTED, 8c:** **ONE** tagged stack, in the **hotbar slot 8** entry. **THIS IS THE
SURVIVABILITY CLAIM AND IT IS WHY THIS IS A DEFECT AND NOT AN EMERGENCY.** Traced in
`NexusSlots.converge`: it collects every star
index, deletes all but the lowest (`setItem(stars.get(i), null)` for `i >= 1`), then relocates the
survivor into `LOCKED_SLOT`. With stars at `[1, 8]` it deletes **8**'s, keeps **1**'s, and moves it
to 8 — **exactly one star, in the right slot.** It runs on join **and on respawn**, so a death heals
it too.

**PREDICTED, 8d:** **ONE** tagged stack, in the **hotbar slot 8** entry, after both gestures.
**8d IS THE ROW.**
Without it, 8a and 8b are equally consistent with the lock being broken everywhere, and this file
would be recording a general failure as a creative one. It is the same control as Row 3's 3c and
Row 2's cobblestone.

**READING:** _(not run)_

### THE FIX IS ONE GUARD, AND WHERE IT GOES IS AN OPEN QUESTION FOR THE OPERATOR

**IT IS CHEAP BY BEN'S TEST: it needs no second refusal mechanism and no second event.** The
deciding value is **already computed and already passed** — `NexusSlots.refuses` hands
`NexusItems.isNexus(event.getCursor(), keys)` to `NexusLock` as `cursorIsStar`, and on an
`InventoryCreativeEvent` that argument is exactly *"the item about to be written into this slot is a
Nexus star"*. **`NexusLock`'s `CREATIVE` arm discards it** — `CREATIVE` sits in the
`Set.of(clicked)` group, and only `DOUBLE_CLICK` consults `cursorIsStar` at all.

**The rule that closes it: a creative write whose NEW ITEM is a Nexus star is refused unless its
destination is the locked slot.** Nothing legitimate is lost — the only sanctioned way a star enters
an inventory is `converge`'s direct `setItem`, which raises no event and so is untouched by any
refusal.

**TWO PLACES IT COULD LIVE, AND THEY TRADE AGAINST EACH OTHER:**

| | where | cost |
|---|---|---|
| **A** | `NexusLock`'s `CREATIVE` arm consults `cursorIsStar` | **unit-testable** — `NexusLockTest` can pin it, and a mutation can kill it. **But the operator has said do not touch `NexusLock`** |
| **B** | an early return in `NexusSlots.refuses` for `InventoryCreativeEvent` | honours that instruction, **and lands in the one class this slice deliberately gave no test file.** Its only coverage would be Row 8 |

> ### RULED 2026-09-15: **A**, AND THE PROHIBITION WAS LIFTED FOR THIS CHANGE ONLY.
>
> **The operator verified the premise from `origin` rather than adopting it**, and refused **B** for
> the reason given above: `NexusSlots` is the class this slice deliberately left untested, and
> putting the one genuinely unit-testable piece of the fix inside it trades a killable test for a
> boot row.
>
> **AND THE RULE SHIPPED WITHOUT ITS EXEMPTION, WHICH IS A CHANGE FROM THE SENTENCE ABOVE.** *"…
> unless its destination is the locked slot"* was **dropped**: the switch arm below already refuses
> every gesture NAMING the locked slot, so that clause is **a branch nothing can reach.** An
> unreachable arm is indistinguishable from one that protects you, and this file has an open finding
> about exactly that. **The shipped guard is total —
> `if (click == ClickType.CREATIVE && cursorIsStar) return true;`** — one line, no exemption.
>
> **THE SWEEP THE RULING REQUIRED FIRST, and its answer is ONE ARM.** `cursorIsStar` is consulted in
> three places (the cursor-drop actions, `DOUBLE_CLICK`, and `refusesDrag`) and in none of the slot
> arms. For every other `ClickType` the payload is reachable anyway: it is either **the cursor** —
> where placing is a deliberate permit — or **the contents of a slot already in the touched set**,
> which `starAt` reads. `ClickType.CREATIVE` is the only one whose payload is in **neither**, because
> the client manufactures it and only `InventoryCreativeEvent.getCursor()` names it.

### 8e AND 8f — THE FIX, IN PLAY, ON BOTH GESTURES. **THE SHIP CONDITION.**

**WRITTEN BEFORE THE FIX WAS BOOTED.** The unit rows prove the decision; these prove the delivery,
and **the ruling makes them blocking rather than confirmatory**.

| | gesture, creative, own-inventory screen, **with the guard deployed** | expected |
|---|---|---|
| **8e** | number-key the star from slot 8 to slot 1 | `/data` shows **ONE** tagged stack, hotbar slot 8 |
| **8f** | **F** on the star in the inventory | `/data` shows **ONE** tagged stack, hotbar slot 8, **and the OFFHAND entry holds no star** |
| **8g** | the control: middle-click, drag and left-click ordinary items around the creative inventory | **everything still moves.** A guard that refuses every creative click reads identically to one that works, until someone tries to build |

> **THESE ROWS SAY "SLOT 8" AND THEY WERE WRITTEN WHEN THAT WAS A FACT ABOUT THE CODE.** Slice 4a
> made the locked slot per-player; this branch predates it and was rebased across it. **The rows are
> still correct for a player who has never opened the settings screen**, which is what the default
> makes everyone — the same precondition the masthead now carries for every row in this file.
>
> **So: run 8e–8g with the Nexus slot at the default 9 (index 8)**, or read "slot 8" as "whatever
> slot the star is in" throughout. **Do NOT re-stage them against a moved slot**: the gesture being
> measured is a creative DUPLICATION, and where the original sits is incidental to it.
>
> **AND ONE THING 4a ADDED THAT THESE ROWS DO NOT REACH.** The guard consults the PAYLOAD, not the
> slot, so it holds even while a player's profile has not loaded and the locked slot is unknown —
> a state that did not exist when these rows were written and that a creative player meets in their
> first seconds online. `aCreativeStarWriteIsREFUSEDEvenWhenTheLockedSlotIsUNKNOWN` is the unit row
> for it. **Not added as a gate row**: staging it needs a profile read to lose a race on purpose,
> which is Row 28a's problem and Row 28a already says it may be unstageable by hand.

**READING:** **2026-09-16, booted by Ben on the dev server, by hand. CONDITIONS: `/gamemode creative`,
own-inventory screen (E), the guard DEPLOYED, and the Nexus slot at the default 9 (index 8) as this
block's precondition requires. INSTRUMENT: the gesture performed by hand; the stack count read with
`/data get entity @s Inventory` from the console.** The predictions above are untouched.

| | gesture | reading |
|---|---|---|
| **8e** | number-key the star from slot 8 to slot 1 | **GREEN** — `/data` shows **ONE** `rpg:nexus`-tagged stack, in the hotbar slot 8 entry |
| **8f** | **F** on the star in the inventory | **GREEN** — **ONE** tagged stack, hotbar slot 8, and **the offhand entry holds no star** |
| **8g** | middle-click, drag and left-click ordinary items around the creative inventory | **GREEN** — everything still moves |

**THE SHIP CONDITION IS MET, AND 8f IS THE ROW THAT MET IT.** Its argument was one step longer than
8e's, and that step was the unmeasured one: **the offhand residual was refused by NEITHER the
`SWAP_OFFHAND` arm NOR `onNexusSwapHand`**, so whether the gesture raised an inventory event at all
was genuinely open. **It does — as `ClickType.CREATIVE` — and the guard takes it.**

> **THE PRE-AUTHORISED FALLBACK IS DEAD, NOT DECLINED.** *"Drop to gating it and leave the code
> alone"* was authorised only on 8f failing. 8f passed, so the branch is not choosing between two
> live options — **one of them stopped existing.** Recorded in those words because a fallback that
> is merely unused reads as a decision someone made, and this one was removed by a measurement.

> **8g IS WHY "TOTAL" DOES NOT MEAN TOTAL, AND DROPPING THE EXEMPTION IS WHAT MADE IT
> LOAD-BEARING.** The guard refuses a creative write CARRYING A STAR with no exemption for the
> locked slot — the exemption was dropped as an unreachable arm. **A guard that refused EVERY
> creative click would pass 8e and 8f identically**, and would break creative inventory editing
> wholesale with nothing else reddening. 8g is the only row that can tell those two apart.

> **IF 8e CLOSES AND 8f DOES NOT, THE FIX DOES NOT SHIP — operator's instruction, and the fallback is
> pre-authorised: drop to gating it and leave the code alone.** A half-fix is worse than none here,
> because the surviving symptom then presents as a **new** bug in a path just declared guarded, and
> the next person debugging it starts from *"but the creative arm handles this"*.
>
> **THE REASONING COVERS BOTH AND THAT IS PRECISELY WHY BOTH ARE MEASURED.** The argument for 8f is
> one step longer than for 8e and the extra step is the one that could be wrong: the offhand residual
> was **not** refused by the `SWAP_OFFHAND` arm and **not** by `onNexusSwapHand`, so it did not arrive
> as either; if it raises an inventory event at all, `ClickType.CREATIVE` is the only remaining
> candidate and the guard takes it. **The unmeasured case is that it raises NO event** — in which
> case nothing in this design can refuse it and the fallback is forced.
>
> This is `OFFHAND_SLOT`'s lesson in its live form: **one instance reasoned about, a second instance
> of the same shape one gesture away.**

---

# SLICE 2 — THE HUB. ROWS 9–13, INCLUDING 12b

**Status: NOT RUN.** Every row below was written **before any boot**, and before the branch that
adds them was pushed. **`NexusMenuLayoutTest` covers the layout and nothing else** — three rows over
two constants and a set. It cannot see an `InventoryView`, cannot open a menu, and cannot observe an
event firing twice. **That gap is these five rows.**

## ROW 9 — THE DOUBLE FIRE. **THE TRAP, AND EVERYTHING ELSE IN THE HANDLER IS BOOKKEEPING**

**`PlayerInteractEvent` FIRES TWICE FOR ONE PHYSICAL RIGHT-CLICK**, once per hand. The guard is
`event.getHand() == EquipmentSlot.HAND`, and it is the FIRST line of `onRightClick` — it predates
this slice and already protects `WeaponFire` from double-spending mana.

**Staging.** `/gamemode survival`. Hold the star. Right-click the air **once**, deliberately, with a
clean pause either side.

**PREDICTED:** the hub opens **ONCE**. No flicker of a close-and-reopen, no second screen behind the
first, and Esc returns straight to the world rather than to another copy of the hub.

> **WHAT A MISSING GUARD LOOKS LIKE, so the reading can name it rather than say "seemed fine":**
> the menu opens, closes and reopens within a tick — visible as a flicker — or two opens stack and
> the first Esc reveals the second. **A single `openInventory` on an already-open identical menu can
> also simply LOOK correct**, which is why the row asks for a *deliberate single* click with pauses:
> a fast double-click by hand is indistinguishable from the defect.

**READING:** _(not run)_

## ROW 10 — THE CANCEL. **THE CHEST MUST NOT OPEN BEHIND THE MENU**

The star's branch calls `event.setCancelled(true)` unconditionally. Without it a right-click that
lands on a block does **both** things: our menu opens *and* the block does whatever it does.

**Staging.** `/gamemode survival`. Place a **chest**. Holding the star, right-click the chest.
Then close the hub and look at what is behind it.

**PREDICTED:** the hub opens and **the chest does NOT**. On closing the hub the player is in the
world, not looking into a chest. The chest's contents are untouched and no container screen is
underneath.

> **AND THE CONTROL, WHICH IS THE ROW.** Switch to an ordinary hotbar slot — anything that is not
> the star — and right-click the same chest. **The chest MUST open normally.** Without this, row 10
> passing is equally consistent with the handler having broken right-click on every block in the
> game, which is a far worse defect than the one the row is looking for and would present as
> "chests stopped working".
>
> **A second control, cheap and worth it: right-click the star at a CRAFTING TABLE.** That block is
> hijacked to `CraftingMenu`, and the star's branch is deliberately ahead of `openHijackedBlock`.
> **PREDICTED: the NEXUS opens, not the crafting menu.** This is a ruling, not an accident — the
> item in the player's hand is what they pressed — and it is the one behaviour in this slice a
> reader is most likely to think is a bug.

**READING:** _(not run)_

## ROW 11 — CLOSE AND ESC BOTH RETURN NOTHING

`inputSlots()` is empty, so `onClose` has nothing to hand back. The row exists because *"nothing to
return"* is a claim about a code path nobody has watched.

**Staging.** `/gamemode survival`, with a **known, counted inventory** — note the exact contents of
the hotbar before opening. Open the hub and close it **twice, by different routes**: once with the
BARRIER at slot 49, once with **Esc**.

**PREDICTED:** both close the screen and **the inventory is byte-for-byte what it was** — nothing
gained, nothing lost, nothing dropped at the player's feet. **The star is still in slot 8.**

> **THE TWO ROUTES ARE NOT REDUNDANT.** The button calls `viewer.closeInventory()` and Esc raises
> the close event directly; they meet at `onClose` only if the button is wired correctly. A button
> that did nothing at all would be invisible to an Esc-only reading, and the screen would still
> close — with Esc.

**READING:** _(not run)_

## ROW 12 — THE TORCH SAYS IT IS NOT BUILT, AND THE ROW READS THE **LORE**

**THIS ROW IS SHAPED BY `Q33`'s FAILURE AND MUST NOT BE WRITTEN THE WAY `Q33` WAS.** That row named
a notice and not its lore, so an operator would have ticked a sole witness while the screen read
*"Not implemented yet."* over a working feature. **Naming the icon is not reading it.**

**Staging.** `/gamemode survival`. Open the hub. **Hover the REDSTONE_TORCH at slot 50 and read the
whole tooltip aloud.** Then click it.

**PREDICTED:** the name is **Settings**, dark gray, and the lore is **exactly two lines** —
*"Not implemented yet."* and *"No settings to change yet."* Clicking it does **nothing at all**: no
message, no sound, no screen change, and the hub stays open.

> **AND THE FORWARD-LOOKING HALF, RECORDED HERE BECAUSE SLICE 3 WILL BE WRITTEN BY SOMEONE WHO DID
> NOT RUN THIS ROW.** The stats head arriving next slice must **NOT** read *"Not implemented yet."*
> — it will carry real figures and only its click will be unbuilt. **If a future reading of this row
> finds that string above live numbers anywhere on the hub, that is the `Q33` defect recurring**,
> and the distinction is written up in `NexusMenu`'s class javadoc.

**READING:** _(not run)_

## ROW 12b — THE COLLISION SPEAKS, AND THE CONTROL IS THAT THE ORDINARY OPEN DOES NOT

**The star's branch runs ahead of `hijackedBlocks`, so a crafting table right-clicked with the star
in hand opens the HUB.** Ben ruled that precedence and it stands — what this row reads is that it
**says so**, because the shadowed block is invisible and *"doing nothing without an explanation
reads as a defect"* is the recorded ground `BrokenNotice` and `QuiverNotice` both stand on.

**Staging.** `/gamemode survival`. Two sub-rows, and **the second is the row**:

| | gesture, holding the star | expected |
|---|---|---|
| **12b-i** | right-click a **crafting table** | the **hub opens** AND one chat line: *"The Nexus took that click -- switch to another hotbar slot to use the block."* The crafting menu does **not** appear |
| **12b-ii** | **THE CONTROL** — right-click **AIR** | the **hub opens** and **NOTHING is said** |
| **12b-iii** | right-click the crafting table **twice inside two seconds** | **ONE line, not two** |

**PREDICTED:** as above. No sound on any of the three — **deliberately, and it is not an
omission**: Ben's ruling is that the Nexus is quiet, and a sound here would be the loudest thing it
does attached to its least important event.

> **12b-ii IS NOT PADDING AND IT IS THE HALF THAT CAN ACTUALLY FAIL.** The ordinary way to reach the
> hub is right-clicking air. **A notice wired to the OPEN rather than to the COLLISION passes 12b-i
> perfectly** — the line appears, the hub opens, everything looks right — and then says the same
> thing every time a player opens their menu for the rest of the server's life. **The defect is
> invisible from the row that was written to find it.** Same shape as Row 3's 3c and Row 10's
> not-the-star control.
>
> **12b-iii guards the throttle**, which exists because right-click repeats when held. `40` ticks,
> the same window `BrokenNotice` uses.

**READING:** _(not run)_

## ROW 13 — **CREATIVE.** DOES RIGHT-CLICK-TO-OPEN BEHAVE THE SAME?

**`/gamemode creative`. ITS OWN ROW, NOT A CAVEAT ON ROWS 9–12b**, and it exists because **Row 8
measured the two modes disagreeing about inventory interaction** on this very item. The register in
`CLAUDE.md` wants its fourth entry **checked rather than assumed** — and the honest prediction is
that this one probably agrees, which is exactly the kind of assumption Row 8 punished.

**Staging.** In creative, repeat **9** (one right-click on air), **10** (the chest, and its
not-the-star control) and **11** (close by both routes).

**PREDICTED:** **identical to survival in all three.** The opener reads no game mode, touches no
inventory slot, and `PlayerInteractEvent`'s hand-pair behaviour is not a creative divergence.

> **WHY IT IS STILL WORTH A BOOT SLOT, STATED SO THE ROW IS NOT DROPPED AS OBVIOUS.** Row 8's defect
> was in `InventoryCreativeEvent`, a class that only exists because creative edits its own inventory
> by a different route. **This path raises no inventory event at all** — which is the argument that
> it should agree, and is precisely the shape of argument that failed for 6.4 and 6.5. **The
> difference is that a disagreement here would be a NEW mechanism, not a known one**, so the value
> of the row is in the surprise, not in the expected reading.
>
> **IF IT DIVERGES, IT IS THE REGISTER'S FOURTH ENTRY.** If it does not, **say so in the register**
> — *"checked, agrees"* is a different and more useful record than silence, which is
> indistinguishable from nobody having looked.

**READING:** _(not run)_

---

# SLICE 3 — THE STATS HEAD. ROWS 14–19

**Status: NOT RUN.** Every row below was written **before any boot**, and before the branch that
adds them was pushed.

**GAME MODE: `/gamemode survival` for all six**, declared here and repeated per row. Nothing in this
slice reads a game mode, but Row 6 taught this file that a mode which is not stated costs a boot to
find out — and **Row 18 stages an inventory gesture with a menu open, which is the surface Row 8
proved behaves differently in creative.**

**What the unit suite already settled, so no row re-asks it.** `NexusStatsLoreTest` has 5 rows over
the tooltip's text and `NexusMenuLayoutTest` has 4 over the layout. Between them, eight mutations
were applied and measured. **They cover every figure, the absence of the header, the untracked
notice, the italic discipline, and the slot.** What they cannot see is a `SkullMeta`, a real
`PLAYER_HEAD`, a live `StatsSheetProjection`, or whether two surfaces rendered from one input path
actually agree on screen. **That gap is these six rows.**

> **AND ONE THING NO ROW HERE CAN SETTLE, SAID PLAINLY.** The projection extraction — forty lines
> moved out of `RpgCommand.stats` — has **no executed check at all** until Row 15 runs.
> `RpgCommand.stats` has never had a unit test (it needs a live `Player`), and this file has never
> been booted. Its only verification today is a **textual diff with a positive control**, recorded
> in the PR body. **Row 15 is the first thing that will actually execute the moved code.**

## ROW 14 — THE HEAD WEARS **YOUR** SKIN, NOT STEVE'S AND NOT SOMEONE ELSE'S

`SkullMeta.setOwningPlayer` **returns a boolean and can fail**, and its failure mode is silent: the
head renders as the default skin and nothing is logged. That is indistinguishable from a head that
was never given an owner at all.

**Staging.** `/gamemode survival`. Right-click the star to open the hub. **Look at slot 13 — the
MIDDLE slot of the second row.** Then have a second player open their own hub and look at theirs.

**PREDICTED:** the head is **your own skin**, recognisably, and the second player's is **theirs**.
Not Steve, not Alex, and not each other's.

> **THE SECOND PLAYER IS NOT PADDING AND IS THE HALF THAT CAN FAIL.** A head wired to a constant
> profile, or to the wrong `Player` reference, **passes a one-player reading perfectly** — the
> operator sees their own face and ticks the row. Only two players distinguish "wears the viewer's
> skin" from "wears somebody's skin".
>
> **If it is Steve:** that is `setOwningPlayer` returning false, not a layout fault. Say so in the
> reading rather than "the head looked wrong", because the two have different fixes.

**READING:** _(not run)_

## ROW 15 — **THE ANTI-DRIFT ROW.** THE LORE AND `/rpg stats` AGREE, FIGURE FOR FIGURE

**This is the row the slice exists around, and it is also the row whose meaning CHANGED while the
slice was being built — so what it now proves is written down rather than assumed.**

Before the extraction this would have compared **two computations**. It now compares **two
renderings of ONE**: both surfaces read `StatsSheetProjection.of` and both render through
`StatsSheet.statLines`. **That is a weaker row for a stronger reason**, and it is said here so the
reading is not mistaken for coverage that was never lost.

**What it can still catch, which is not nothing:** the two surfaces being fed at different moments,
a held-weapon difference between the command and the menu open, a unit error surviving the move, and
**the moved projection failing to execute correctly at all** — which nothing else checks.

**Staging.** `/gamemode survival`. **Hold a quiver weapon** (the Boltor), so all ten lines are in
play. Run `/rpg stats`, leave the chat visible, then open the hub **in the same session without
changing what you are holding**. Read the head's tooltip against the chat output **line by line**.

**PREDICTED:** **ten lines in chat under the header, and the same ten in the tooltip with no
header.** Every label and every figure identical, including the two decimal places — `Max Health`,
`Health Regen`, `Max Mana`, `Mana Regen`, `Defense`, `Damage`, `Crit Chance`, `Crit Damage`,
`Quiver`, `Reload`. The tooltip's **name** is `Your Stats`, which is the header's text.

> **READ THE FIGURES, NOT THE SHAPE.** "They looked the same" is what this row must not accept —
> `Q33`'s lesson, one file over. **Name at least the Damage and Reload values in the reading**, with
> their digits, so a later reader can tell the row was actually read.
>
> **AND THE TOOLTIP MUST NOT SAY *"Not implemented yet."*** — Row 12's forward-looking half, now
> due. The head is `MenuIcons.icon`, not `placeholder`. If that string appears above live numbers
> anywhere on this screen, that is the `Q33` defect recurring.

**READING:** _(not run)_

## ROW 16 — CLICKING IT DOES NOTHING, AND SAYS NOTHING

The head is a readout with an unbuilt click. **`NexusMenu.onClick` has no `STATS_SLOT` branch at
all**, deliberately: a no-op branch would read as a wired button whose body someone forgot to write.

**Staging.** `/gamemode survival`. Open the hub. **Left-click the head. Then right-click it. Then
shift-click it. Then number-key over it.**

**PREDICTED:** on all four — **nothing.** No chat line, no sound, no screen change, no item moves to
the cursor, and the hub stays open. The head is still in slot 13 afterwards.

> **THE FOUR GESTURES ARE NOT ONE GESTURE REPEATED.** Shift-click and the number key are the
> **performed** routes — the ones `MenuRouting` writes directly rather than merely permitting — and
> they are the pair that would move the head out of the menu if `inputSlots()` were ever widened.
> A left-click-only reading cannot see that.

**READING:** _(not run)_

## ROW 17 — THE UNTRACKED PLAYER IS **TOLD**, AND IS NEVER SHOWN ZEROES

**A real state, and the hub is the surface a player meets it on** — `/rpg stats` has to be typed;
the head is simply there. A readout showing `0` when nothing was counted is indistinguishable from a
working readout that measured zero.

**Staging.** `/gamemode survival`. **Join the server and open the hub as fast as you can**, before
the stat engine has registered you. If that window is too tight to hit by hand, reach the same state
the way Row 5 reaches its staging and say in the reading which route was used.

**PREDICTED:** the tooltip has **exactly one lore line** — *"No stats tracked yet -- try
rejoining."* — and **no numbers of any kind.** `/rpg stats` in the same moment says the same
sentence, word for word, because both read it from one constant.

> **IF THIS ROW CANNOT BE STAGED, SAY SO AND LEAVE IT UNREAD.** Do not tick it from the unit test:
> `NexusStatsLoreTest` covers the lore's TEXT in that state and cannot cover the projection
> returning empty on a live server, which is the half this row is for. **An unstageable row recorded
> as unstaged is an honest gate; one ticked from a unit test is a false witness.**

**READING:** _(not run)_

## ROW 18 — THE QUIVER PAIR GOES STALE, AND THIS ROW **RECORDS** IT RATHER THAN FAILING IT

**KNOWN AND ACCEPTED FOR THIS SLICE.** The menu paints once, on open. Eight of the ten lines are
maxima and rates and cannot move while a screen is up. **The quiver pair is keyed to the HELD
weapon**, and `MenuRouting` deliberately permits a player to rearrange their own inventory with a
menu open — so the head can go on showing a capacity for a weapon no longer selected.

**Staging.** `/gamemode survival`. Hold the Boltor. Open the hub. **Read the Quiver and Reload
lines. Then, with the hub still open, move the Boltor out of your selected hotbar slot. Re-read the
tooltip.**

**PREDICTED:** the two lines are **unchanged** — still showing the Boltor's capacity — because
nothing repaints. **This is the expected behaviour, not a defect**, and the row exists so the slice
that makes this head clickable finds the answer instead of rediscovering the question.

> **WHAT WOULD MAKE IT A DEFECT, so the reading can tell them apart:** if the lines **disappear**,
> or show a **different** weapon's numbers, or the head renders blank. Any of those means something
> is repainting, and repainting is what this slice decided not to build.
>
> **SURVIVAL IS STATED AND IT MATTERS HERE.** This is an own-inventory gesture with a menu open —
> the exact surface Row 8 shows behaves differently in creative. **A creative reading of this row
> certifies creative and nothing else.**

**READING:** _(not run)_

## ROW 19 — **THE CONTROL.** THE CLOSE BUTTON STILL CLOSES

The new icon is the first thing ever added to this screen's body. **The control is that adding it
broke nothing that already worked** — filler painted over a live button is invisible until someone
clicks it, and `FILLER_SLOTS` is now built by three subtractions instead of two.

**Staging.** `/gamemode survival`, with a **known, counted inventory**. Open the hub. **Close it
with the BARRIER at slot 49.** Open it again and **close it with Esc.**

**PREDICTED:** both close the screen, and the inventory is **byte-for-byte what it was** — nothing
gained, nothing lost, nothing dropped at your feet. **The star is still in slot 8.**

> **THIS IS ROW 11 RE-RUN, AND IT IS RE-RUN DELIBERATELY RATHER THAN CITED.** Row 11 passed against
> a two-button screen. This slice changed `FILLER_SLOTS` — the set that decides which slots get
> painted over — so Row 11's result is about a layout that no longer exists. **A control carried
> past its precondition stops being a control without stopping being quotable.**

**READING:** _(not run)_

---

# SLICE 4a — THE LOCKED SLOT GOES PER-PLAYER. ROWS 20–24

**Status: NOT RUN.** Every row below was written **before any boot**, and before the branch that
adds them was pushed.

**GAME MODE: `/gamemode survival` for all five**, declared here and per row.

**THE DEFAULT IS STILL 8, WHICH IS WHY ROWS 1–19 ARE NOT RESTAGED.** `PlayerProfile.DEFAULT_NEXUS_SLOT`
is 8 and **nothing in this slice lets a player change it** — the settings screen is 4b. So every
existing row still describes what will happen, and this block adds the cases the change creates
rather than rewriting the ones it did not touch. **The restaging this file has warned about since
Row 8 is owed by 4b, not by 4a.**

> **WHAT THE UNIT SUITE ALREADY SETTLED, SO NO ROW HERE RE-ASKS IT.** `NexusSlotsTest` pins the slot
> bound, `NexusLockTest` pins the per-player arm and the unknown-slot window, `ProfileServiceTest`
> pins that `whenSettled` waits for the load and still runs when it fails, and
> `PlayerProfileMigrationTest` + `FilePlayerRepositoryTest` pin the v2 → v3 migration through real
> Gson. **Nine mutations were applied and measured against those**, kill sets recorded in the PR
> body.
>
> **What none of them can see** is a live `PlayerInventory`, a real join ordering, or a disk read
> racing a tick. **That gap is these five rows** — and two of them cover code the suite measured as
> having NO unit guard at all (`converge`'s use of the target, and the whole join wiring).

## ROW 20 — THE ORDINARY JOIN, WHICH MUST LOOK EXACTLY AS IT DID BEFORE

**The control, and it runs first.** Everything below changes how the star is placed; this row says
that for a player who has never touched the setting — which is every player today — nothing about
the observable behaviour moved.

**Staging.** `/gamemode survival`. Join the server with a **known, counted inventory**. Watch slot 9
of the hotbar (index 8, the rightmost) as the world loads.

**PREDICTED:** the star is in the rightmost hotbar slot, exactly one of it, and the rest of the
inventory is untouched. Right-clicking it opens the hub.

> **THE STAR MAY APPEAR A FRACTION LATER THAN IT USED TO, AND THAT IS THE CHANGE.** Placement now
> waits for the profile read instead of running on the join tick. On a local server that is
> single-digit milliseconds and **should be invisible**. **If it is visible — a frame where the slot
> is empty, or the star arriving after the inventory is interactable — SAY SO AND TIME IT.** That is
> the cost of the fix and nobody has measured it on real hardware.

**READING:** _(not run)_

## ROW 21 — **THE RACE.** THE ROW THIS SLICE EXISTS FOR, AND IT NEEDS A HAND-EDITED FILE

**There is no way to set a non-default slot in-game until 4b**, so this row edits the JSON directly.
That is not a workaround; it is the only way to test the migration's output and the race before the
UI that produces them exists.

**Staging**, and **the order matters**:

1. `/gamemode survival`. Join once so a profile file exists, then **quit**.
2. With the player OFFLINE, open `plugins/<plugin>/players/<uuid>.json`. Confirm it reads
   `"schemaVersion": 3` and `"nexusSlot": 8`.
3. Edit **`"nexusSlot"` to `3`** and save. Change nothing else.
4. Rejoin.

**PREDICTED:** the star is in **hotbar slot 4** (index 3) and **nowhere else** — not in slot 9, and
not in both. Whatever was in slot 4 has been moved elsewhere in the inventory, or dropped at the
player's feet with a message, and **is not destroyed**.

**Then, still in that session, the half that is the actual defect:**

| | gesture | expected |
|---|---|---|
| **21a** | try to pick up / drag / Q the star in slot 4 | **REFUSED**, every route |
| **21b** | **THE ROW.** put an ordinary item in **hotbar slot 9** (index 8), then pick it up, move it, drop it | **PERMITTED, every time.** Slot 9 is an ordinary slot for this player |

> **21b IS THE ONE THAT CAN FAIL SILENTLY AND IS THE REASON THIS ROW IS WRITTEN.** The defect the
> slice fixes places the star at the default 8 while the lock protects 3. **The star would still be
> guarded** — the lock's second arm follows the item, so 21a passes either way and is NOT
> discriminating — but slot 9 would be **inert for the whole session**, refusing every gesture, with
> nothing logged and no message. A player would report "my hotbar is broken sometimes".
>
> **So 21a is the reassurance and 21b is the measurement.** If 21b fails, the placement is racing
> the profile read and the fix did not take.

**READING:** _(not run)_

## ROW 22 — THE MIGRATION, ON A FILE THAT PREDATES THE FIELD

**The first schema migration this project has ever run against real player data**, and the only row
that reads it on a file the server itself wrote at v2.

**Staging.** `/gamemode survival`. With the player OFFLINE, edit their JSON: set `"schemaVersion"`
to **2** and **delete the `"nexusSlot"` line entirely**. Save. Rejoin, then quit, then read the file
again.

**PREDICTED:** the star is in the **rightmost hotbar slot** on rejoin, and after the quit the file
reads `"schemaVersion": 3` with `"nexusSlot": 8`.

> **THE FAILURE TO WATCH FOR IS SLOT 1, NOT AN ERROR.** An absent integer deserialises to **0**, and
> 0 is a legal slot — the LEFTMOST hotbar cell. If the migration step were ever reduced to a stamp
> bump, like the two steps before it, **every existing player's star would move to slot 1 and
> nothing would throw**. That is the whole reason this row edits a file rather than trusting the
> unit tests, which stage the zero by hand.
>
> **And nothing else in the file may change.** Read `level`, `experience`, `archetypeId` and
> `elementId` before and after; a migration that resets a field it does not own is a data loss the
> player discovers later.

**READING:** _(not run)_

## ROW 23 — A CORRUPT PROFILE STILL GETS A STAR

**The permanent-empty arm.** A load that fails settles too, and the placement must still happen —
otherwise the one population that cannot fix its own file also loses the only route into the hub.

**Staging.** `/gamemode survival`. With the player OFFLINE, replace their JSON with `{` — a single
brace, deliberately unparseable. Rejoin.

**PREDICTED:** the star is in the **rightmost hotbar slot** (the default, because their preference
is unreadable), the hub opens from it, and the **server log carries one SEVERE line** naming the
player and saying their data will not be touched this session.

> **AND THE FILE MUST BE UNCHANGED WHEN THEY QUIT.** Read it after. `onQuit` chains its save onto
> the failed load, so the save never runs — that invariant predates this slice and this row is
> checking the slice did not break it. **A profile overwritten here is a player's entire progress,
> destroyed by the error handler meant to protect it.**
>
> **If no star appears at all**, the settle path is not running its failure arm and every
> corrupt-profile player is locked out of the Nexus permanently.

**READING:** _(not run)_

## ROW 24 — DEATH AND RESPAWN, WHERE THE PROFILE IS ALREADY IN MEMORY

Respawn converges too, and it takes the **synchronous** path — the profile is not reloaded on death,
so the slot is known without waiting. **Different code path, same expected outcome**, which is
exactly the shape that hides a defect in one of the two.

**Staging.** `/gamemode survival`, with `nexusSlot` still edited to **3** from Row 21. Die — lava,
fall, `/kill`. Respawn.

**PREDICTED:** the star is in **hotbar slot 4** after respawning, exactly one of it. **Not slot 9.**

> **SLOT 9 HERE MEANS THE RESPAWN PATH IGNORED THE SETTING**, which is a different bug from Row 21's
> and would be invisible to it: join could be perfectly correct while respawn welds the default.
> Two call sites, one of which waits and one of which does not.
>
> **Row 24 is also the only row that exercises the star being RE-placed while the player already
> has one**, so read whether anything was displaced or duplicated.

**READING:** _(not run)_

---

# SLICE 4b — THE SETTINGS SCREEN. ROWS 25–30

**Status: NOT RUN.** Every row below was written **before any boot**.

**GAME MODE: `/gamemode survival` for all six**, declared here and per row.

## THE RESTAGING THIS FILE HAS OWED SINCE ROW 8 — AND IT IS A PRECONDITION, NOT A REWRITE

`NexusLockTest`'s pin row has warned since slice 1 that *"GATE-nexus.md's rows are staged against 8
and need restaging"* if the locked slot ever moves. Slice 4a made the slot **per-player** and kept
the default at 8, so nothing moved and no row needed touching. **4b is what lets a player move it**,
and the debt comes due — but not as a rewrite, because the rows are not wrong.

> **EVERY ROW IN THIS FILE, 1 THROUGH 30, ASSUMES THE PLAYER'S NEXUS SLOT IS THE DEFAULT 8 —
> EXCEPT ROWS 21, 24, 26 AND 29, WHICH MOVE IT ON PURPOSE.**
>
> That was a fact about the code until 4b. **It is now a fact about the OPERATOR'S STATE**, and it
> can be false without anything being broken: an operator who runs Row 26, then goes back to Row 9,
> is reading a screen staged against a slot they personally changed three rows earlier.
>
> **So: after any row that moves the slot, put it back to 9 (index 8) before running an earlier
> row** — Row 26 itself is the cheapest way, or edit the JSON as Row 21 does. **Rows that move it
> say so in their own staging.**
>
> **This is the restaging, and it is a precondition rather than a rewrite because the rows measure
> the same things they always did.** A rewrite would have been the wrong repair: it would have
> re-staged 19 rows against a slot no default player has.

**What the unit suite already settled, so no row here re-asks it.** `SettingsMenuLayoutTest` pins
the nine choosers, the two buttons and the filler set; `MenuIconsTest` pins Ben's *"Back to
\[destination\]"* form; `ProfileServiceTest` pins the writer, the four availability arms and that
the two refusal messages differ. **Six mutations applied and measured**, kill sets in the PR body.

**What none of them can see** is a live `InventoryView`, a menu-to-menu transition, or a star moving
between hotbar cells while a screen is open. **That gap is these six rows.**

## ROW 25 — THE TORCH GRADUATED, AND THE ROW READS THE **LORE**

**Row 12's twin, and it must not be written the way `Q33` was.** Row 12 read the torch as a
placeholder and expected *"Not implemented yet."* **That expectation is now WRONG** and this row
replaces it.

**Staging.** `/gamemode survival`. Open the hub. **Hover the REDSTONE_TORCH at slot 50 and read the
whole tooltip aloud.** Then click it.

**PREDICTED:** the name is **Settings** and the lore is **exactly one line** — *"Choose where the
Nexus sits."* **The string "Not implemented yet." appears NOWHERE on this screen.** Clicking opens a
six-row screen titled **Nexus Settings**.

> **ROW 12 IS NOW STALE AND IS DELIBERATELY NOT EDITED.** Its prediction was correct when written
> and is falsified by this slice; the convention in this file is that a prediction is never revised
> after the fact. **Read Row 12 as history and this row as the live one.** If Row 12 is ever run, it
> will fail, and that failure is the graduation rather than a defect.

**READING:** _(not run)_

## ROW 26 — **THE ROW.** CHOOSING A SLOT MOVES THE STAR AND THE LOCK TOGETHER

**MOVES THE SLOT. Put it back to 9 before running any earlier row.**

**Staging.** `/gamemode survival`, with a **known, counted hotbar** — put a recognisable item in
**slot 4** (index 3) first. Open the hub, click the torch, and **click the fourth chooser from the
left**.

**PREDICTED, in order:**

| | expected |
|---|---|
| 26a | one chat line: *"The Nexus now sits in slot 4."* |
| 26b | the star is **in hotbar slot 4**, and **not** in slot 9 |
| 26c | the item that was in slot 4 is **elsewhere in the inventory or at your feet with a message** — **not destroyed** |
| 26d | the settings screen **repaints**: the fourth chooser is now the lime one, the ninth is not |
| 26e | close the screen. Pick up / drag / Q the star in slot 4 → **REFUSED** |
| 26f | **THE DISCRIMINATING HALF.** Put an ordinary item in **slot 9** and move it freely → **PERMITTED** |

> **26f IS THE ONE THAT CAN FAIL SILENTLY.** If the write and the placement disagree — if the star
> moved but the lock still protects 9, or the reverse — then 26e passes anyway, because the lock's
> second arm follows the star wherever it sits. **Only 26f distinguishes "the setting took" from
> "the star moved and the guard did not".** It is slice 4a's defect, reachable by hand.
>
> **26c is the item-safety half and it is not padding.** `converge` is the only code in the Nexus
> that deletes anything, and a displaced occupant goes through `MenuSafety.give`. A vanished item
> here is the worst outcome in this file.

**READING:** _(not run)_

## ROW 27 — BACK AND CLOSE ARE DIFFERENT INTENTS AND MUST NOT BE THE SAME BUTTON

**Staging.** `/gamemode survival`. Open the hub → torch → settings. Then, from the settings screen:

| | gesture | expected |
|---|---|---|
| 27a | click **Back** at slot 48 -- an ARROW, immediately left of Close | the **hub** appears. Not the world, not a flicker of both |
| 27b | reopen settings, click **Close** at slot 49 | the **world**. The hub does not reappear behind it |
| 27c | reopen settings, press **Esc** | the world, same as 27b |
| 27d | from the hub after 27a, press Esc | the world |

**PREDICTED:** as above. **Hover Back and read its name: it says "Back to the Nexus", not "Back".**

> **27a IS A MENU-TO-MENU TRANSITION AND IT IS THE FIRST ONE THE NEXUS HAS.** What a broken one
> looks like, so the reading can name it: a **flicker** of the world between the two screens; the
> hub opening and instantly closing; or the settings screen still being there behind the hub, so
> the first Esc reveals it. **Any of those means the tick hop is wrong**, and `Menu.open`'s javadoc
> is where the rule it would be violating lives.
>
> **27b and 27c must agree.** They are the same path — `MenuIcons.close()` calls
> `closeInventory()` and Esc raises the close directly, meeting at `onClose`. A button that did
> something *else* would pass 27c and fail 27b, or vice versa.

**READING:** _(not run)_

## ROW 28 — THE TWO REFUSALS, AND THEY MUST NOT SAY THE SAME THING

**The row the write path exists for.** A menu is reachable in states a command is not: the star
opens the hub off its PDC tag, so a player rejoining can be looking at this screen before their
profile has loaded — or holding a profile that will never load at all.

**Staging**, two sub-rows, and **28b is the row**:

| | staging | expected on clicking a chooser |
|---|---|---|
| **28a** | rejoin and reach settings **as fast as possible**, before the profile lands. If that window cannot be hit by hand, say so and leave 28a unread | *"Your profile is still loading -- try again in a moment."* in GRAY, and **the slot does not change** |
| **28b** | with the player OFFLINE, replace their JSON with `{`. Rejoin, open settings, click a chooser | *"Your profile could not be read, so changes cannot be saved. Try rejoining."* in RED |

**PREDICTED:** as above, and in 28b **no chooser is highlighted at all** — the screen does not know
which slot is theirs and does not claim one.

> **THE TWO MESSAGES MUST DIFFER, AND THAT IS THE WHOLE ROW.** Before this slice every surface said
> *"still loading"* for both. For 28b that is a **lie**: the load finished and failed, so nothing is
> still happening and the player retries until they give up. **If 28b shows the GRAY "try again in a
> moment" text, the distinction has collapsed** and `availability` is not being consulted.
>
> **And the file must be untouched after 28b.** Quit, then read it: still `{`. A failed load must
> never be written over, which is the invariant `onQuit` has held since before the Nexus existed.

**READING:** _(not run)_

## ROW 29 — THE CHOICE SURVIVES A REJOIN, WHICH IS THE POINT OF PERSISTING IT

**MOVES THE SLOT. Put it back to 9 afterwards.**

**Staging.** `/gamemode survival`. Set the slot to **2** through the settings screen. **Quit.** Read
`players/<uuid>.json` from disk. **Rejoin.**

**PREDICTED:** the file reads `"nexusSlot": 1` — **index 1, the second cell, because the screen
says "Slot 2" and the file stores the index**. On rejoin the star is in **hotbar slot 2**, the
settings screen shows the second chooser lime, and slot 9 is an ordinary cell.

> **THE OFF-BY-ONE IS THE POINT OF READING THE FILE.** The screen counts slots the way a player
> does, from 1; the inventory indexes from 0. **If the file says `2`, the screen and the store
> disagree by one** and every player's star will drift one cell right on their next login.
>
> **And this is the row that proves the write reached DISK rather than only the cache.** A write
> that updated the in-memory profile and failed to persist looks perfect until exactly here.

**READING:** _(not run)_

## ROW 30 — **THE CONTROL.** THE HUB IS UNCHANGED BY ANY OF THIS

**Staging.** `/gamemode survival`, slot back at the default 9. Open the hub and **do nothing except
read it**.

**PREDICTED:** the stats head is still at slot 13 wearing your skin with live figures; Close is
still at 49 and still closes; the star is still in slot 9; and **no chat line is printed by opening
the hub**.

> **THE HUB GAINED A LIVE BUTTON AND A CONSTRUCTOR ARGUMENT THIS SLICE**, and `render()` now paints
> three things instead of two. This row says the two that already worked still do. It is Row 19
> re-run against a changed screen, deliberately rather than by citation: **a control carried past
> its precondition stops being a control without stopping being quotable.**

**READING:** _(not run)_

---

# SLICE 5 — CRAFTING AND ENCHANTING FROM THE NEXUS. ROWS 31–34

**Status: NOT RUN.** Every row below was written **before any boot**.

**GAME MODE: `/gamemode survival` for all four.**

**The two stations are at 31 and 32 — row 4, the crafting-type band.** Their band was picked by
their KIND, not chosen; `NexusMenuLayout`'s band table is the rule.

## ROW 31 — THE TWO STATIONS OPEN, AND THE ENCHANT SCREEN IS UNPOWERED

**Staging.** `/gamemode survival`. Open the hub. **Read both new icons in row 4, then click each.**

**PREDICTED:**

| | expected |
|---|---|
| 31a | slot 31 is a **CRAFTING_TABLE** named *Crafting*; slot 32 is an **ENCHANTING_TABLE** named *Enchanting* |
| 31b | the enchanting icon's lore says **"Unpowered -- no bookshelves here."** before you click it |
| 31c | clicking 31 opens the crafting screen; clicking 32 opens the enchant screen |
| 31d | **THE ROW.** On the enchant screen, hover the bookshelf slot at index 8. It reads **"Bookshelf Power 0/30"** |

> **31d MUST NOT SAY "NOT AVAILABLE", AND MUST NOT BE A `placeholder`.** The reading is REAL and
> CORRECT — it measured zero. `MenuIcons.placeholder`'s javadoc settled this exact case as its first
> worked example: *"'0/30' reads as a measurement where a bare '0%' could not."* **A screen that says
> "not available" where it means "zero" is the recipe browser's empty state wearing the placeholder's
> clothes, and gate row `Q33` would have passed on that one too.**
>
> **If the lore reads *"Not implemented yet."* anywhere on this screen, that is the `Q33` defect
> recurring for the third time.**

**READING:** _(not run)_

## ROW 32 — **THE CONTROL FOR 31d.** A REAL TABLE STILL COUNTS ITS SHELVES

**Without this row, `0/30` passes whether or not the count is wired at all.**

**Staging.** `/gamemode survival`. Place a real enchanting table with **at least one bookshelf** in
the ring around it. Right-click it. **Hover the bookshelf slot.**

**PREDICTED:** a **NON-ZERO numerator** — `Bookshelf Power N/30` where N is at least 1 — and the
stack in that slot is **N books deep**, not one.

> **THE STACK IS THE GLANCE AND THE NAME IS THE MEASUREMENT.** The amount floors at 1, because an
> `ItemStack` of amount 0 renders as **nothing at all** and an empty cell cannot be told from a
> feature that is not there. So power 0 and power 1 both show ONE book and are distinguished by the
> NAME. **Read the name, not the pile.**
>
> **With 30 or more shelves the stack GLINTS.** That marks the ceiling, so a player can see they
> have stopped gaining without reading the number.

**READING:** _(not run)_

## ROW 33 — **BACK EXISTS ONLY WHEN THERE IS SOMEWHERE TO GO BACK TO**

**Staging.** `/gamemode survival`, **four sub-rows across BOTH screens**, and **33b and 33d are
the controls**:

| | staging | expected |
|---|---|---|
| **33a** | open crafting **FROM THE HUB** (slot 31) | an **ARROW at slot 48** named **"Back to the Nexus"**, immediately left of Close. Clicking it returns to the hub |
| **33b** | **THE CONTROL** — right-click a crafting table **in the world** | **NO button at slot 48.** The cell is **plain filler**, and Close is still at 49 beside it |
| **33c** | **THE ENCHANT TWIN.** Open enchanting **FROM THE HUB** (slot 32) | an **ARROW at slot 48** named **"Back to the Nexus"**. Clicking it returns to the hub |
| **33d** | **THE TWIN'S CONTROL** — right-click an enchanting table **in the world** | **NO button at slot 48** |

> **33b IS THE HALF THAT CAN FAIL SILENTLY.** A build that always paints Back passes 33a perfectly.
> The world-opened screen would then offer to return a player to a hub they never opened — a
> back-arrow promising a destination they did not come from, which is the line `MenuIcons.close`'s
> javadoc draws from the other side.
>
> **33b NOW READS A FILLER PANE RATHER THAN "WHATEVER WAS THERE BEFORE".** Back left the
> navigation column for the bottom row, and the bar gave up slot 48 **permanently** — so on the
> world path the cell is neither bar nor button and must be explicitly filler. **An EMPTY cell mid
> bar is the failure**: it means the subtraction happened and nothing filled the hole.
>
> **AND THE STATUS BAR IS SEVEN GRAY CELLS IN BOTH.** Count them on each sub-row. Back is at 48,
> inside the bar's row, and the bar subtracts it **whether or not the button is painted** —
> **a readout whose geometry changes by origin is not a readout.** If 33a shows seven and 33b shows
> eight, the subtraction has been made conditional and `Q22` is the row that will fail next.
>
> **33c AND 33d ARE NEW, AND THEIR ABSENCE IS THE DEFECT THEY RECORD.** The enchant screen was
> given no back button by the slice that gave crafting one, and **nothing caught it because no row
> asked this screen the question 33a asks the crafting one.** A feature missing from one of two
> parallel surfaces is invisible to a gate that stages only the other.
>
> **Load the grid before clicking Back in 33a, and put a weapon in the input slot before clicking
> it in 33c.** The items must come back: both screens HAVE input slots, so each closes before it
> navigates, and `returnEverything` runs on that close. **33c is the sharper of the two** — the
> enchant screen's input slot moved to 19 in the same ruling.

**READING:** _(not run)_

## ROW 34 — THE NAVIGATION COLUMN, READ AS A COLUMN

> ### **DEAD. THIS ROW'S PREMISE WAS WITHDRAWN, AND IT IS DELIBERATELY NOT EDITED.**
>
> **The rule it reads no longer exists.** Ben moved crafting's back button from 17 to **48**, and
> *"column 8 is where you go somewhere else"* had exactly two instances — Back and the recipe book.
> One of them left. **A rule with one instance is not a rule**, so it was deleted from
> `CraftingMenuLayout` rather than renumbered, and this row has nothing left to make visible.
>
> **WHY IT IS DEAD RATHER THAN RESTAGED, AND THE DISTINCTION IS THE POINT.** Row 33 was restaged in
> place because its SUBJECT MOVED: Back still exists, at a different cell, so its expected value
> could be re-pointed. **This row's subject CEASED TO EXIST.** Re-pointing it at slot 48 would
> produce a row that reads *"Back is where Back is"* — a check that cannot fail, which is worse
> than no check, and it would carry this row's name while testing something else entirely.
>
> **Read it as history.** It was correct when written, and it is falsified by the second set of
> layout rulings rather than by any defect. **If it is ever run it will fail, and that failure is
> the withdrawal rather than a bug** — the same treatment Row 12 has, and for the same reason.
>
> **ITS SUCCESSORS ARE ROWS 33a–33d**, which read Back at 48 on both screens and both origins. The
> half of this row worth keeping is its last paragraph — *the recipe book must still work* — and
> that is not lost: `BROWSER_SLOT` is untouched at 26, and `CraftingMenuLayoutTest` pins it.

**Staging.** `/gamemode survival`. Open crafting **from the hub** and look at **column 8** — the
rightmost — in rows 1 and 2. That is slots 17 and 26.

**PREDICTED:** **Back at 17, the recipe book at 26, stacked.** Both are navigation; nothing else in
that column is.

> **THIS ROW EXISTS TO MAKE THE RULE VISIBLE RATHER THAN TO CATCH A DEFECT.** The rule is *column 8
> is where you go somewhere else*, and it is the reason 17 was chosen — not "above the status bar",
> which is true of that slot and is an accident. **A reader who learns the accidental reason will
> put the next navigation button anywhere in rows 1-5.**
>
> **The recipe book must still work from the hub-opened screen.** Click it. Removing a working
> feature conditionally is the collision notice's lesson inverted: a line of chat was added so a
> shadowed crafting table would not be silent, and silently deleting a button is worse.

**READING:** _(not run)_

---

# SLICE 6 — THE GRINDSTONE. ROWS 35–47

**Status: NOT RUN.** Every row below was written **before any boot**, and before the branch that
adds them was pushed.

> **THESE ROWS WERE WRITTEN ONCE AGAINST A FOURTEEN-CELL TRAY WITH NO STATUS BAR, AND REWRITTEN
> BEFORE ANY OF THEM WAS READ.** Ben's screenshot review added a third tray row, a four-state
> status bar and a fourth button colour. **No prediction here has ever been read, so rewriting them
> in place is the correct treatment** — the no-edit rule binds a prediction that has been READ, and
> none of these has. Recorded because a reader comparing this block to the branch's first commit
> will find them different.

**GAME MODE: `/gamemode survival` for all thirteen**, declared here and repeated per row.

> ### AND HERE THE MODE IS LOAD-BEARING RATHER THAN BOILERPLATE
>
> **Creative hides the XP bar.** Seven of these rows read a refund, and in creative a refund reading
> is **VOID rather than PASS or FAIL** — there is nothing on screen to read it off.
>
> **That is Row 6's lesson applied before the boot instead of after it.** Row 6 was staged without a
> mode, booted in creative, and lost two of its six readings to exactly this; those two are still
> VOID. **Nobody asked this slice to declare a mode — it declares one because Row 6 already paid.**

**What the unit suite already settled, so no row here re-asks it.** `GrindstoneRefundTest` pins the
inclusive bound, the tray-level flooring, the integer-versus-floating-point form at a computed
divergence, and the no-ledger licence over a 93-point sweep. `GrindstoneMenuLayoutTest` pins the
twenty-one cells, the seven bar cells, and **the four-way partition** — filler, tray, bar and chrome
covering the screen exactly once. `GrindstoneButtonTest` pins the four faces, the palette, the
precedence, and that the bar's pane is a lookup on the button's state rather than a second decision.

**What none of them can see** is a live `Inventory`, a `RepeatingTask` against a real player, an item
re-minted, or **a repaint landing on a cell a player is looking at.** That gap is these rows.

## ROW 35 — THE REFUND FIGURE, AGAINST HAND ARITHMETIC

**Staging.** `/gamemode survival`. `/rpg give ironblade`, `/rpg enchant candidate 0 sharpness`,
unlock it to **II** at a table with no shelves. `/xp query`. One item in the tray, wait for LIME,
read the button, click.

**PREDICTED:** **`Strip 1 item -- +441 XP`**, and the wallet is **exactly 441 points higher**.

> **441 IS `1262 * 35 / 100`, AND THE HAND ARITHMETIC IS THE POINT.** Level II costs `352 + 910`;
> 35% of 1262 floors to 441. **Do the multiplication before reading the screen** — reading the
> button and then the wallet checks one expression against itself.
>
> **If it says 882 or 1262, the bound is exclusive or the percentage is missing.**

**READING:** _(not run)_

## ROW 36 — A **LEVEL-I-ONLY** ITEM. **THE ROW THE OLD REPO'S LOOP RETURNS ZERO FOR**

**Staging.** `/gamemode survival`. A fresh weapon, one candidate unlocked to **I** and no further.

**PREDICTED:** **`Strip 1 item -- +123 XP`**, LIME, and 123 points arrive.

> **THE COMMONEST ITEM ON THE SERVER, AND THE TRANSPLANTED BOUND PAYS NOTHING FOR IT.** The
> predecessor's `for (lvl = 1; lvl < a.level(); lvl++)` does not execute at all at level 1.
>
> **A RED BUTTON IS THE DEFECT HERE, NOT AN EMPTY TRAY.** If it reads **RED** — *"These have nothing
> to strip"* — with a visibly enchanted weapon in the tray, the bound is exclusive.

**READING:** _(not run)_

## ROW 37 — A FULL TRAY OF **TWENTY-ONE**, AND **ONE** XP GRANT

**Staging.** `/gamemode survival`. **Twenty-one** enchanted items, every tray cell full. `/xp query`,
click once, `/xp query` again.

**PREDICTED:** the wallet moves **once**, by the figure on the button, and chat reads
**`Stripped 21 items for N XP.`** — **one line, not twenty-one.**

> **TWENTY-ONE GRANTS WOULD LOOK ALMOST RIGHT, WHICH IS WHY THIS ROW COUNTS THE LINE.** Per-item
> granting lands within **18** points of the correct total, so the wallet alone cannot distinguish
> it. **The chat line can.**
>
> **And the button's figure must equal the wallet delta exactly** — they come from one expression.

**READING:** _(not run)_

## ROW 38 — THE ROLL SURVIVES THE STRIP

**Staging.** `/gamemode survival`. Before stripping, open the enchant table and **write down every
candidate the item offers, in order, in all three slots.** Strip it. Reopen the enchant table.

**PREDICTED:** **the same candidates, same slots, same order** — every one at level 0, none active.

> **THE FAILURE TO WATCH FOR IS A NEW SET OF CANDIDATES**, which means the strip cleared the rolled
> flag and the item re-rolled on next open. **That is a player's roll destroyed, and it is invisible
> unless somebody wrote the old one down first. Write it down first.**

**READING:** _(not run)_

## ROW 39 — THE COUNTDOWN RUNS, AND **RE-ARMS ON EVERY MUTATION**

**Staging.** `/gamemode survival`. One item in, **watch without clicking**. Then with the button
LIME, **add a second**. Then **take one out**.

| | expected |
|---|---|
| 39a | **YELLOW**, counting **3, 2, 1**, then **LIME** — and the **bar is yellow with it** |
| 39b | adding an item turns both **YELLOW again at 3** |
| 39c | **REMOVING one re-arms too.** Yellow, from 3, again |

> **39c IS THE ROW.** *"Removals are safe"* is the obvious exception and it was **considered and
> refused** — an exception is a boundary. A build that re-arms on placement only passes 39a and 39b.
>
> **THE BAR AND THE BUTTON MOVE TOGETHER, ALWAYS.** They are one state machine with two renderers.
> **If the bar is green while the button counts down, they are computing separately** — which is the
> defect `Q23` exists to record.

**READING:** _(not run)_

## ROW 40 — A CLICK DURING THE LOCKOUT DOES NOTHING **AND SAYS NOTHING**

**Staging.** `/gamemode survival`. Load the tray and, while **YELLOW**, click the button **five
times**. `/xp query` before and after.

**PREDICTED:** **nothing happens, five times.** No strip, no XP, **no chat line**, no sound. The
countdown continues and the items are untouched.

> **THE SILENCE IS DELIBERATE AND IS NOT A ROW 14 VIOLATION.** Row 14's case was **a button that
> looked identical whether it worked or not.** This one never does — the countdown is persistent
> visible feedback **on the button being clicked**, and the bar carries the same state behind it.
>
> **If clicking during yellow strips anything, the arming delay does not exist** — it is a decoration
> over a live button, which is worse than no delay because the player trusts it.

**READING:** _(not run)_

## ROW 41 — **PRECEDENCE.** AN EMPTY TRAY IS GRAY, NOT YELLOW

**Staging.** `/gamemode survival`. Open the grindstone and **look at slot 40 and the bottom row
immediately, before touching anything.** Then click the button.

**PREDICTED:** button **GRAY**, reading **`Add items to strip`**; bar **GRAY**. **Not yellow and not
counting down**, though the deadline initialises at open and the clock is genuinely running.
Clicking does nothing and says nothing.

> **PRECEDENCE IS NOTHING, THEN ARMING, THEN READY.** A lock on an action with nothing to lose is not
> information the player needs. **A yellow countdown on an empty tray is the failure**, and it is
> what a build that tests the clock before the tray produces.

**READING:** _(not run)_

## ROW 42 — **THE CONTROL.** TWENTY-ONE ITEMS STILL THERE AFTER THIRTY SECONDS OF TICKING

**Staging.** `/gamemode survival`. Fill **all twenty-one** tray cells with **recognisable,
individually identifiable** items. **Do not click anything.** Leave the screen open **thirty
seconds** — roughly sixty countdown fires — then close it and count what comes back.

**PREDICTED:** **all twenty-one items, unchanged.** Same items, same enchants, same durability,
nothing blanked and nothing duplicated.

> ### *** THE ROW THAT CATCHES THE REPAINT CLOBBER. WITHOUT IT, A TICK THAT WIPES THE TRAY PASSES EVERY OTHER ROW IN THIS BLOCK. ***
>
> The arming task fires twice a second for the menu's whole life. **If it called `render()` instead
> of writing its named cells, every fire would paint filler over all twenty-one** — and the items
> are gone, with **no output slot to recover them from and no undo.**
>
> **THE BAR MADE THIS WORSE, NOT SAFER.** The tick now has **two** writers — the button at 40 and
> the seven bar cells — and the tray has grown from fourteen cells to twenty-one. **Either writer
> reaching the tray destroys gear**, and the bar is the newer and less obvious of the two.
>
> **Every other row in this block is short.** Rows 35–41 are read in seconds, well inside the
> three-second arm, so a tick that destroys the tray on its fourth fire never gets the chance.
> **Thirty seconds is chosen to outlast every other row's dwell time**, not because thirty is
> significant.
>
> **`MUTS5-BACK`'s shape with items in place of chrome** — and where that cost an invisible button,
> this costs twenty-one pieces of gear. **Unrecoverable, not cosmetic.**

**READING:** _(not run)_

## ROW 43 — SHIFT-CLICK FILLS THE **FIRST FREE CELL IN INDEX ORDER**

**Staging.** `/gamemode survival`. With an **empty** tray, shift-click three items in one at a time,
watching where each lands. Then **take the middle one out** and shift-click a fourth in.

**PREDICTED:** the three land at **10, 11, 12** in that order. The fourth lands in the **hole at
11**, not at 13.

> **NO CODE WAS WRITTEN FOR THIS, WHICH IS WHY IT IS BOOTED.** `MenuRouting.firstEmptyInput` already
> iterates a sorted set of `inputSlots()`. **Inheriting a behaviour is not observing it**, and this
> is the project's first twenty-one-slot surface.
>
> **10, NOT 19** — the tray gained a row above, so the first cell moved. A build still filling from
> 19 has the old constant.

**READING:** _(not run)_

## ROW 44 — THE WORLD BLOCK, AND THE TWO CONTROLS 4b AND 4c EXIST FOR

**Staging.** `/gamemode survival`. Place a real grindstone.

| | staging | expected |
|---|---|---|
| **44a** | right-click with an **empty hand** | **our** screen opens. **Never the vanilla one** |
| **44b** | **sneak**-right-click with an **empty hand** | **nothing opens at all** |
| **44c** | **sneak**-right-click holding a **BLOCK** | the block is **NOT placed**; nothing opens |
| **44d** | right-click holding a weapon that binds `right_click` | the ability **fires**; no screen |

> **44b IS THE SHAPE THAT SHIPPED BROKEN ON THE ENCHANT TABLE**, whose cancel sat inside the
> `!isSneaking` guard so this path opened **vanilla enchanting**. The grindstone **inherits the fix**
> because it is one entry in `hijackedBlocks`. **Inheriting is why this should pass; it is not why it
> can be skipped.**
>
> **44c IS THE ACCEPTED CONSEQUENCE, AND ONE NEVER OBSERVED IS AN ASSUMPTION.** No block can be
> placed against a grindstone face while sneaking. Paid knowingly.

**READING:** _(not run)_

## ROW 45 — CLOSE AND ESC RETURN THE WHOLE TRAY, INCLUDING THE DROP

| | staging | expected |
|---|---|---|
| **45a** | twenty-one items in the tray, click **Close** (49) | **all twenty-one** back |
| **45b** | twenty-one in the tray, press **Esc** | **all twenty-one** — the SAME path |
| **45c** | inventory **36/36** full, items in the tray, **Esc** | they **drop at your feet**, yellow line |
| **45d** | an item on the **cursor** when you press Esc | returned; not lost, not duplicated |
| **45e** | tray loaded, click **Back** (48) on the hub route | the tray returns **AND** the hub opens |

> **45e IS THE ONE WITH AN ORDERING IN IT.** This screen HAS input slots, so Back must **close first
> and hop second** — the close is what runs `returnEverything`. A build that hops without closing
> swaps the inventory out from under twenty-one items.
>
> **45c WILL PRODUCE UP TO TWENTY-ONE YELLOW LINES**, because `MenuSafety.give` speaks per call.
> **Known, and not this slice's to fix.** Record how many appear; the row passes on the items
> arriving, not on the line count.

**READING:** _(not run)_

## ROW 46 — **GRAY VERSUS RED.** THE TWO "NOTHING" STATES, READ AGAINST EACH OTHER

**Staging.** `/gamemode survival`, two readings **one after the other, in one sitting**, with both
the button **and the bar** read each time, and the strings written down **verbatim**:

| | staging | expected |
|---|---|---|
| **46a** | open with an **EMPTY** tray | button **GRAY**, `Add items to strip`; bar **GRAY** |
| **46b** | fill the tray with **already-stripped** items | button **RED**, `These have nothing to strip`; bar **RED** |

> **THE CONTROL IS THAT THEY DIFFER IN COLOUR AND IN TEXT. THE FAILURE IS THAT BOTH ARE
> CORRECT-LOOKING AND THE SAME.**
>
> **This row used to read gray against gray**, when both states shared a colour and differed only by
> wording — **Row 28's remedy applied halfway**, since Row 28's fix changed the colour as well. Ben
> ruled the fourth state in, so they now separate on both axes and this row reads both.
>
> **GRAY FOR EMPTY, NOT RED, AND THE ROW SHOULD FAIL IF THAT IS INVERTED.** Red means something is
> **wrong**; an empty tray is not wrong, it is unstarted. **If 46a is RED, red is doing two jobs** —
> *"these are the wrong items"* and *"you have not started"* — which is exactly the collapse Row 28
> exists to record.
>
> **Write both strings and both colours down rather than ticking the row.** *"They looked different"*
> is not a reading.

**READING:** **2026-09-17, booted by Ben. 46a PASS, 46b not run.** The button and the bar both read
**GRAY** with `Add items to strip`, as predicted. **The prediction above is untouched.**

### *** 46a IS SUPERSEDED BY RULING. NOT VOID, NOT FAILED. ***

**The reading was correct when taken and was falsified afterwards by a decision**, which is a third
outcome this file has not had before in this block:

```
VOID        the CONDITIONS were wrong      -- Row 6.4, booted in creative against a row naming no mode
FAIL        the BUILD was wrong            -- nothing here
SUPERSEDED  the reading was right and the RULING moved underneath it
```

**THE RULING:** the bar's and button's empty state moved **GRAY → LIGHT GRAY**. Ben reported slot 48
as *"gray glass"* from a world block; measured, that cell is `BLACK_STAINED_GLASS_PANE` via
`MenuIcons.filler()` and **is correct** — what he was seeing is **black chrome against gray bar
cells**, which at 16 pixels is not a distinction a player can make. **That is `Q23`'s two-grays
finding arriving one shade over**, and the fix is on the BAR rather than the chrome, because
`MenuIcons.FILLER` is black on every screen in the plugin.

**THE ORIGINAL PREDICTION AND ITS READING ARE NOT EDITED.** The no-edit rule binds from the moment a
row is read, and this one has been. Read them as history; the live staging is below.

**RESTAGED 46a:** open with an **EMPTY** tray. **PREDICTED:** button **LIGHT GRAY**,
`Add items to strip`; bar **LIGHT GRAY**. **It must be visibly lighter than the black filler beside
it** — that difference IS the ruling, and a reading that cannot tell them apart is a fail.

> **46b IS UNAFFECTED AND IS NOT RESTAGED.** It is RED on both surfaces, and nothing in this ruling
> touches red. **Row 41 restages with 46a** — its prose says GRAY for the same state — and **Row 47
> is unaffected**, because it is about the BLACK filler, which did not move.

**READING:** restaged 46a — _(never run — see below)_

### *** THE SUPERSESSION WAS ITSELF REVERTED, THE SAME DAY, AND THE ORIGINAL READING IS TRUE AGAIN ***

**Ben ruled the empty state back to GRAY.** He had asked for grey twice; the light-gray version was
the second time the code shipped something else, and the argument for it — that
`MenuIcons.EMPTY_SUGGESTION` already means *"a cell waiting to hold something"* — **was a real
precedent and was not what he asked for.**

**SO THE RESTAGED 46a ABOVE IS WITHDRAWN, NOT READ AND NOT FAILED.** It was never booted; the
ruling that required it was reverted before anyone reached a keyboard.

> **AND THE ORIGINAL PREDICTION AND READING ARE ONCE AGAIN ABOUT THE SHIPPED BEHAVIOUR.** 46a
> predicted **GRAY on both surfaces** and read **PASS**; the build is gray again. **That reading is
> not re-instated by fiat — it is simply no longer superseded**, because the thing that superseded
> it is gone.
>
> **NOTHING IS EDITED, IN EITHER DIRECTION.** The original prediction, the original reading, the
> supersession note and the withdrawn restaging all stand as written. **A file that records what was
> observed must also record what was decided and undecided**, and collapsing this back to *"46a
> PASS"* would delete a day in which the shipped screen was something else.
>
> **46a DOES NOT NEED RE-READING.** The colour it was read against is the colour that ships.

> ### AND A **FAIL** WAS TICKED AGAINST THE RESTAGED 46a ON THE PAGE. IT IS NOT RECORDED AS ONE, AND THE REASON IS THE WHOLE DISTINCTION THIS ROW HAS BEEN TEACHING
>
> **The restaged row predicted LIGHT GRAY and the screen showed LIGHT GRAY. The prediction matched.**
> What was rejected was **the RULING**, not the build.
>
> **A FAIL HERE WOULD SAY THE BUILD WAS BROKEN, AND IT WAS NOT.** It did exactly what it had been
> told to do, and the instruction was withdrawn — which is the difference between
> `SUPERSEDED BY RULING` and `FAIL`, and the reason this file now distinguishes them.
>
> **Recorded rather than dropped**, because a tick on a page is an observation too, and a reader who
> hears about it later with no entry here would reasonably conclude a failure was buried.

## ROW 47 — **FROM A BLOCK, SLOT 48 IS PLAIN FILLER.** THE ROW THAT WOULD HAVE CAUGHT THE HOLE

**Staging.** `/gamemode survival`. Open the grindstone **from a world block** — NOT from the hub —
and look at **slot 48**, immediately left of Close. Then run the tray through **empty → loaded →
armed → stripped** and look at 48 after each.

**PREDICTED:** slot 48 is a **plain filler pane, throughout.** **Never empty**, never a back arrow,
and **never a bar cell** — it does not change colour as the bar does.

> ### THIS ROW EXISTS BECAUSE THE DEFECT SHIPPED AND A SCREENSHOT CAUGHT IT
>
> `BACK_SLOT` is subtracted from the filler set **permanently, both origins** — so the bar is seven
> cells whatever the origin, because **a readout whose geometry depends on how you got there is not
> a readout.** Back is painted **only from the hub.** From a block, something else must paint 48,
> and for one commit **nothing did**: an invisible, clickable hole in the middle of the bar's row.
>
> **THE OTHER HALF OF THE SAME DEFECT WAS ON THE HUB**, where `GRINDSTONE_SLOT` was subtracted from
> the filler and painted by nothing — slot 33, invisible, with a working click handler behind it.
> **Two screens, one shape: a cell removed from the filler set and given no painter.**
>
> **THE INVARIANT NOW ASSERTED IN UNIT TESTS:** filler, tray, bar and chrome are four disjoint sets
> whose union is the whole screen. **That form catches an unpainted cell AND a repaint that reaches
> the tray**, which are the two ways this screen can go wrong.
>
> **This row is the in-play half.** A unit test can prove the sets partition; only a boot can prove
> the painter ran.
>
> **If 48 is EMPTY, the else-arm is missing. If it CHANGES COLOUR with the bar, the subtraction is
> conditional** — and the bar's width now forks on origin, which is what the permanent subtraction
> exists to prevent.

**READING:** **2026-09-17, booted by Ben. PASS.** Slot 48 from a world block holds a pane; the tooltip
reads **BLACK STAINED GLASS PANE**. Not empty, not an arrow, and it does not change with the bar.
**The prediction above is untouched.**

> **AND THIS ROW PRODUCED A FINDING BY BEING READ CORRECTLY, WHICH IS WORTH MORE THAN THE PASS.**
> The cell was reported as *"gray glass"* and looked wrong. **Measured, it is black and it is
> right** — `GrindstoneMenu` paints `MenuIcons.filler()` there and `MenuIcons.FILLER` is
> `BLACK_STAINED_GLASS_PANE`, the same furniture every screen in the plugin uses.
>
> **What was actually wrong was the cell NEXT to it.** Black chrome against GRAY bar cells is not a
> distinction a player can make at 16 pixels, so the bar's empty state moved to LIGHT GRAY — see
> Row 46's supersession note. **A row that passes and still surfaces a defect one cell over is the
> argument for reading a screen rather than ticking it.**

### *** SUPERSEDED BY RULING, 2026-09-17. NOT VOID, NOT FAILED. ***

**The reading above was correct when taken and is falsified by a later decision** — the second time
this file has used that outcome, after Row 46a.

**THE RULING:** Ben, having seen the screen, ruled that **from a world block slot 48 is a BAR CELL
and changes colour with the rest of the readout.** From the hub it is still the Back arrow.

```
from the hub     bar = 45,46,47,   50,51,52,53     SEVEN   Back at 48, Close at 49
from a block     bar = 45,46,47,48,50,51,52,53     EIGHT   Close at 49
```

**THIS ROW'S PREDICTION IS NOW WRONG IN THE ONE CLAUSE IT WAS BUILT ON** — *"never a bar cell — it
does not change colour as the bar does"* — and it is **not edited**, per the no-edit rule, which
binds because this row has been read.

> **AND IT OVERTURNS AN ARGUMENT MADE AT LENGTH IN THIS REPO.** `GrindstoneMenuLayout` said SEVEN
> CELLS, BOTH ORIGINS, on the borrowed rule that *"a readout whose geometry depends on how you got
> there is not a readout"*. **Ben agreed to that before he had seen it and ruled the other way once
> he had.** The argument is about a reader comparing two screens; what a player sees is one screen
> with a black hole in its readout.
>
> **SECOND LAYOUT RULE TO DIE BY LOOKING** — the first was *"column 8 is the navigation column"*,
> killed when Back moved to 48 in `#112`. **Both were well-argued and both were overturned by the
> person in front of the screen.**

**RESTAGED 47 — and its SUBJECT changes, not just its expected value.** The old row asked *is 48
painted at all*; the new one asks *does 48 move with the bar*.

**Staging.** `/gamemode survival`. Open the grindstone **from a world block**. Drive the tray
through **empty → loaded → armed → ready**, and after each step read **48 together with 45, 46 and
47**.

**PREDICTED:** slot 48 is a **coloured pane that changes with its neighbours at every step** —
gray, then yellow, then lime — and is **never black, never an arrow, and never a different colour
from 45–47.**

> **THE DISCRIMINATING HALF IS READING IT *WITH* ITS NEIGHBOURS.** A build that painted 48 from the
> bar's colour but on a stale state would pass a glance at 48 alone. **Read the run of four.**
>
> **AND THE HUB PATH IS THE CONTROL:** open from the hub, drive the same states, and 48 must be the
> **ARROW throughout** — never a pane, never changing colour. If 48 changes colour from the hub,
> the bar is painting over Back and `MUTS5-BACK` has returned by a new route.

**READING:** restaged 47 — _(not run)_

---

# SLICE 7 — THE STAR OPENS THE HUB FROM YOUR INVENTORY. ROWS 49–57

**Status: RUN 2026-09-17, 03:58–04:00, booted by Ben. ALL TEN READINGS PASS** — 49, 50, 51, 52,
53, 54, 55, 56a, 56b, 57. **Every row below was written before that boot**, and before the branch
that adds them was pushed; **no prediction was edited afterwards.**

> **TEN READINGS FROM NINE ROWS** — Row 56 is the creative control and was read on both sides.
>
> **56b IS THE ONE THAT MATTERED AND IT PASSED**: in creative, the star's slot refused silently and
> nothing opened. **That turns Row 6.1's continued truth from an argument about click types into an
> observation** — the whole reason the row exists. The day someone widens the arm to include
> `ClickType.CREATIVE`, this is what goes red.
>
> **53, 55 and 57 were the other three that could have failed alone**: a build opening on any touch
> of the star breaks four of Row 6's readings while passing 49 and 50; a build keyed on *"is this
> slot in the player's half"* opens the hub from inside every chest; and a build refusing or
> hijacking every inventory click passes the whole block except 57.

**GAME MODE: `/gamemode survival` for all nine unless the row says otherwise.** **Row 56 stages
BOTH modes and the creative half is the point of it.**

## THIS IS NOT "UNLOCK THE STAR". `NexusLock` IS UNTOUCHED

**The star still does not move.** No chat line, no sound, no title — **Ben's silence ruling survives
whole**, and every refusal refuses exactly what it refused before. **What changes is that ONE
already-refused gesture gains a SIDE EFFECT.**

The hook goes where the refusal already lives, which is the design rather than a convenience: the
star's slot is **the one cell in a player's inventory where a click is already guaranteed to be
intercepted.**

```
OPENS     LEFT or RIGHT click, on the star's OWN slot, with an EMPTY CURSOR,
          in the OWN-INVENTORY SCREEN
REFUSES   everything else, silently, exactly as today -- drag, Q, F, number-key,
          creative, and any click with an item on the cursor
```

## *** ROW 6.1 IS NOT SUPERSEDED BY THIS SLICE, AND THAT WAS CHECKED RATHER THAN ASSUMED ***

**The brief for this slice said 6.1 would be falsified. It is not.**

Row 6's reading is scoped **`CONDITIONS: CREATIVE mode, own-inventory screen (E)`** — the line two
above the readings table. **In creative, a click on an own-inventory slot arrives as
`ClickType.CREATIVE`**, not `LEFT` or `RIGHT`; that is the whole basis of `NexusLock`'s step 2b
payload guard. **So this arm never fires in creative, and 6.1's reading — *pick up the star →
silent refusal* — is still true.**

**What Row 6 does NOT have is a SURVIVAL reading of any kind.** It named no game mode and was
booted creative-only. **So this slice changes a gesture the file has never read**, which is a gap
Row 6 always had rather than a supersession — and these rows are where it is closed.

> **THE RULE THIS EARNED: A READING IS NEVER CITED WITHOUT ITS CONDITIONS LINE.** The conditions are
> **part of** the reading, not context around it. A reading quoted bare has been promoted from
> *"true here"* to *"true"*, by whoever quoted it, silently.

## What the unit suite already settled, so no row here re-asks it

`NexusOpenGestureTest` pins the rule itself over the **whole `ClickType` enum** — exactly two open —
and over **all eight combinations** of the three booleans, of which exactly one opens.

**What it cannot see** is whether those four booleans are computed from the right things: whether
*"the own-inventory screen"* really excludes a chest, whether *"the star's own slot"* really tracks a
re-chosen slot, and whether the open lands on the next tick rather than inline. **That gap is these
nine rows.**

## ROW 49 — **LEFT-CLICK THE STAR. THE HUB OPENS.**

**Staging.** `/gamemode survival`. Press **E**. **Left-click the star**, once, with an empty cursor.

**PREDICTED:** the **Nexus hub opens**. The star **does not move** and is still in its slot when you
close the hub. **No chat line, no sound, no title** — the ruling is unchanged; a screen is none of
those things.

> **WATCH THE SLOT AS WELL AS THE SCREEN.** A build that opened the hub by *picking the star up
> first* would look identical for the first frame and leave the star on the cursor underneath.

**READING:** **2026-09-17, 03:58–04:00, booted by Ben. PASS.** The prediction above is untouched.

## ROW 50 — **RIGHT-CLICK DOES THE SAME**

**Staging.** `/gamemode survival`, **E**, **right-click the star** with an empty cursor.

**PREDICTED:** identical to Row 49. The hub opens; the star does not move.

> **BOTH, NOT EITHER.** Right-click on an occupied slot is vanilla's *take half*, a different
> `InventoryAction` from left-click's *take all*, and the rule is keyed on `ClickType` rather than
> action precisely so the two agree. A build keyed on the action passes one of these rows.

**READING:** **2026-09-17, 03:58–04:00, booted by Ben. PASS.** The prediction above is untouched.

## ROW 51 — **A LOADED CURSOR DOES NOT OPEN. IT IS AN ATTEMPTED PLACE.**

**Staging.** `/gamemode survival`, **E**. **Pick up any ordinary item** so it is on the cursor. Now
**left-click the star's slot**.

**PREDICTED:** **nothing opens.** The click is refused silently, exactly as today: the star does not
move, the held item stays on the cursor, no chat line.

> **THIS IS THE ROW THAT KEEPS ROW 10c's PROBLEM OUT OF THE HUB.** A click with something on the
> cursor is the player trying to PUT IT DOWN. Opening a menu there drops them into the hub **holding
> an item**, and the hub has no input slots and no `returnEverything` obligation to hand it back —
> `PLAN-enchant-table-ui`'s cursor branch arriving somewhere it was never solved.
>
> **MINE, NOT BEN'S** — flag it. The narrower rule widens later without a migration.

**READING:** **2026-09-17, 03:58–04:00, booted by Ben. PASS.** The prediction above is untouched.

## ROW 52 — **A DRAG ACROSS THE STAR'S CELL DOES NOT OPEN**

**Staging.** `/gamemode survival`, **E**. With an ordinary item on the cursor, **drag across several
slots INCLUDING the star's**, and release elsewhere.

**PREDICTED:** **nothing opens**, and the drag is refused as today — the star is untouched.

> **A DRAG IS A DIFFERENT EVENT AND MUST NOT BE MADE TO LOOK LIKE A CLICK.** `InventoryDragEvent`
> never reaches the click handler, so this row is confirming the arm was not added to the drag guard
> as well — which a reasonable person might do for symmetry, and which would open the hub every time
> a player swept a stack across their hotbar.

**READING:** **2026-09-17, 03:58–04:00, booted by Ben. PASS.** The prediction above is untouched.

## ROW 53 — **Q, F AND NUMBER-KEY STILL REFUSE SILENTLY. THE FOUR OLD ROWS STILL HOLD.**

**Staging.** `/gamemode survival`, **E**, on the star's slot, in order: press **Q**; press **F**;
**number-key** it to slot 1. Then **drag from it** (Row 6.2's gesture).

**PREDICTED:** **all four refuse, silently, and NOTHING OPENS on any of them.** The star never
appears to leave its slot.

> ### *** THE ROW THAT CATCHES THE OBVIOUS WRONG IMPLEMENTATION ***
>
> **A build that opens the hub on ANY touch of the star passes Rows 49 and 50 perfectly and breaks
> four readings this file already has.** Rows 6.2, 6.3, 6.4 and 6.5 are not restaged by this slice —
> they must simply still be true — and nothing in Rows 49–52 would notice if they stopped being.
>
> **Note 6.4 and 6.5 are VOID in creative and this row is SURVIVAL**, so a survival reading here is
> new information about them either way.

**READING:** **2026-09-17, 03:58–04:00, booted by Ben. PASS.** The prediction above is untouched.

## ROW 54 — **A NON-DEFAULT SLOT. THE WHOLE REASON THIS SLICE EXISTS.**

**MOVES THE SLOT. Put it back to 9 (index 8) before running any earlier row.**

**Staging.** `/gamemode survival`. Open the hub the old way, choose a **different** hotbar slot in
settings — say **slot 1 (index 0)** — and confirm the star has moved there. Now press **E** and
**left-click the star in its NEW slot**.

**PREDICTED:** the hub opens. **And left-clicking slot 9 (index 8) — where the star used to be, now
an ordinary empty cell — opens NOTHING.**

> **BEN'S ANSWER TO "A STAR IN SLOT 27 IS INVISIBLE" IS THIS SLICE, SO THIS IS THE ROW IT EXISTS
> FOR.** A build that hardcoded the default slot passes every other row in this block, because every
> other row leaves the star at the default.
>
> **THE SECOND HALF IS THE DISCRIMINATING ONE.** The arm requires the star to be PRESENT in the
> clicked slot, not merely that the slot is the locked one — so an empty locked slot opens nothing.
> **MINE, NOT BEN'S**: his brief said *"the star's OWN slot"*, and requiring the star to actually be
> there is the reading that stops an empty cell opening a menu.

**READING:** **2026-09-17, 03:58–04:00, booted by Ben. PASS.** The prediction above is untouched.

## ROW 55 — **ANOTHER SCREEN REFUSES. A CHEST IS NOT YOUR INVENTORY.**

**Staging.** `/gamemode survival`. Open a **chest**. The star's slot is visible in the bottom half of
that view. **Left-click it.**

**PREDICTED:** **nothing opens**, and the click is refused as today.

> **THE NEAR-MISS THIS ROW EXISTS FOR IS A PREDICATE THAT ALREADY EXISTED AND ANSWERS A DIFFERENT
> QUESTION.** `NexusSlots.touchedOf` asks *"is this SLOT in the player's half"*, which is **true of a
> chest's bottom half too**. A build using it would open the hub from inside every chest on the
> server, and **Rows 49–54 would all still pass.**
>
> **Also stage it with one of OUR menus open** — the grindstone, say, with items in the tray. The
> hub must not open, because that is a nested transition out of a menu with `returnEverything`
> obligations. **MINE, NOT BEN'S** — flag it.

**READING:** **2026-09-17, 03:58–04:00, booted by Ben. PASS.** The prediction above is untouched.

## ROW 56 — **THE CREATIVE CONTROL. SURVIVAL OPENS, CREATIVE DOES NOT.**

**Staging.** Two readings, **in one sitting**, the same gesture in both:

| | staging | expected |
|---|---|---|
| **56a** | `/gamemode survival`, **E**, left-click the star | the hub **OPENS** |
| **56b** | `/gamemode creative`, **E**, left-click the star | **NOTHING OPENS.** Silent refusal, star unmoved |

> ### *** THIS IS THE ROW THAT KEEPS ROW 6.1 HONEST, AND IT IS WHY IT IS A ROW RATHER THAN AN ARGUMENT ***
>
> **6.1's reading stays true only because the arm is keyed `LEFT`/`RIGHT` and creative clicks arrive
> as `ClickType.CREATIVE`.** That is a true statement about click types and **it is not observed
> anywhere until this row is read.**
>
> **THE DAY SOMEONE WIDENS THE ARM TO INCLUDE `CREATIVE`, 6.1's READING BECOMES FALSE WITH NOTHING
> GOING RED.** And it is a perfectly reasonable thing to want — **a creative player probably does
> want the hub.** This row makes that widening a **visible change** instead of a silent
> contradiction somebody finds in a month.
>
> **Whether creative SHOULD open is Ben's question and is deliberately not asked here.** The narrow
> arm plus this control is the version where asking it later costs a ruling and a row rather than a
> quiet contradiction.
>
> **56b is also a direct re-read of Row 6.1 under its own conditions**, so if it fails, 6.1 has been
> falsified and this block is what says so.

**READING:** **2026-09-17, booted by Ben. 56a PASS, 56b PASS.** Survival opened the hub; **creative refused silently and nothing opened.** The prediction above is untouched.

## ROW 57 — **THE CONTROL. AN ORDINARY ITEM STILL JUST PICKS UP.**

**Staging.** `/gamemode survival`, **E**. **Left-click an ordinary item** elsewhere in the inventory
— a stack of cobblestone in slot 20, say. Then put it back.

**PREDICTED:** it **behaves exactly as vanilla**: it lifts onto the cursor. **No menu opens**, and
nothing about the click is refused.

> **WITHOUT THIS ROW, A BUILD THAT REFUSED OR HIJACKED EVERY INVENTORY CLICK PASSES THIS ENTIRE
> BLOCK.** Every other row stages the star's slot; this is the only one that stages a cell the
> feature must not touch.
>
> **The handler runs at `LOWEST` on EVERY `InventoryClickEvent` on the server**, so the cost of
> getting it wrong is not confined to the star — it is every click by every player.

**READING:** **2026-09-17, 03:58–04:00, booted by Ben. PASS.** The prediction above is untouched.

---

# SLICE 8 — THE NEXUS GOES IN ANY OF THE 36 INVENTORY SLOTS. ROWS 58–65

**Status: RUN 2026-09-17, 05:25-05:27. ALL EIGHT PASS.**

> **Every row below was still written BEFORE any boot, and no prediction was edited after a reading
> was taken.** That is the property the old `NOT RUN` line was carrying, and it survives the status
> flipping — the readings were appended beside the predictions, never over them.
>
> **ROW 64 IS THIS BLOCK'S SHIP CONDITION AND ITS READING PREDATES THE MERGE IT AUTHORISED.**
> See the note under that row: the operator had read it before `#117` merged, but it was not
> written down until now, and **a ship condition with no recorded reading is indistinguishable from
> one nobody checked.**

**GAME MODE: `/gamemode survival` for all eight.**

## THE CODE CHANGE IS ONE CONSTANT, AND THE ROWS ARE WHERE THAT CLAIM IS TESTED

`NexusSlots.MAX_SLOT` went from **8** to **35**. Nothing else in the lock, the convergence or the
raw-slot conversion moved — **slice 4a had already put `NexusLock` in `PlayerInventory` index space,
where 0-8 is the hotbar and 9-35 is storage**, so the coordinate work was done two slices ago.

**THE PICKER IS THE INVENTORY, MIRRORED:** menu `9..35` → inventory `9..35` (identity), menu
`36..44` → inventory `0..8` (the hotbar, on the bottom row).

> **NO MIGRATION, AND THESE ROWS ARE WHY THAT IS SAFE RATHER THAN HOPED.** Every stored value today
> is 0-8 and every one stays legal in 0-35 — a widened bound cannot invalidate what it already
> accepted. **Row 61 is the rejoin that proves a stored value survives**, and Row 58 is the one that
> proves a NEW value outside the old range persists at all.

**What the unit suite already settled.** `NexusSlotsTest` pins the bound at both ends — 35 in, 36
out — and pins that 8 and 9 are now BOTH in, which is the pair that reddens on an un-widened build.
`SettingsMenuLayoutTest` pins the mirror at both seams and asserts it is a **bijection onto 0..35**,
so no cell is unreachable and none is reachable twice.

**What none of them can see** is a real `PlayerInventory`, an item being displaced, or a profile
surviving a disconnect. **That gap is these eight rows.**

## ROW 58 — **A STORAGE SLOT. THE ROW THE WHOLE SLICE EXISTS FOR.**

**MOVES THE SLOT. Put it back to hotbar 9 before running any earlier row in this file.**

**Staging.** `/gamemode survival`. Open the hub → settings. **Click a cell in the middle of the
picker** — one clearly in the storage rows, not the bottom row. Note which.

**PREDICTED:** the star **moves there immediately**, the chat line names that cell, and the picker
repaints with **that** cell lime and the old one white.

> **THE PICKER IS THE INVENTORY MIRRORED, so the cell you clicked is the cell the star appears in.**
> Close the screen and look: it must be in the position the picker showed, not nine cells away.
>
> **AN OFF-BY-NINE IS THE FAILURE TO WATCH FOR**, and it is invisible in the storage rows because
> that half of the mapping is the IDENTITY. **If the star lands nine cells off, you clicked a hotbar
> cell and got storage, or the reverse** — which is Row 59's job to separate.

**READING:** **PASS.** Read 2026-09-17, 05:25-05:27, with the SLICE 8 block.

## ROW 59 — **A HOTBAR SLOT STILL WORKS, AND IT IS THE HALF THAT IS NOT THE IDENTITY**

**Staging.** `/gamemode survival`. In the picker, click a cell on the **BOTTOM ROW** — the hotbar.
Say the **third** one.

**PREDICTED:** the star moves to **hotbar slot 3**, the chat line says so, and it is on the hotbar
where the player can hold it.

> ### *** THIS IS THE ONLY HALF OF THE MAPPING THAT CAN BE WRONG QUIETLY ***
>
> Menu `9..35` → inventory `9..35` is the **identity**: twenty-seven of the thirty-six cells map to
> themselves, and **a build with the translation missing entirely passes Row 58.** The nine hotbar
> cells are the only ones where the arithmetic does anything.
>
> **If the star lands in a storage cell instead, the mirror is not being applied** — and the screen
> will look almost right, because the cell it lands in is the one directly above where you clicked.

**READING:** **PASS.** Read 2026-09-17, 05:25-05:27, with the SLICE 8 block.

## ROW 60 — **THE DISPLACED ITEM COMES BACK**

**Staging.** `/gamemode survival`. Put a **recognisable, countable** stack — 17 cobblestone — in a
storage cell. With inventory space free, choose **that cell** in the picker.

**PREDICTED:** the star takes the cell, and the **17 cobblestone are still in the inventory**,
somewhere else, **all seventeen**. Nothing is destroyed and nothing is duplicated.

> **COUNT THEM.** A displaced stack that comes back as 16 or 34 is the failure, and "the cobblestone
> is still there" does not distinguish those from the pass.
>
> **`converge` already routes the occupant through `MenuSafety.give`**, which is the shared
> give-or-drop path — this row is confirming that in play rather than asserting the call exists.

**READING:** **PASS.** Read 2026-09-17, 05:25-05:27, with the SLICE 8 block.

## ROW 61 — **A FULL INVENTORY DROPS IT, AND SAYS SO**

**Staging.** `/gamemode survival`. Fill the inventory to **36/36**, with a recognisable stack in the
cell you are about to choose. Choose it.

**PREDICTED:** the star takes the cell, the displaced stack **drops at your feet**, and the
**yellow line** appears: *"Your inventory was full -- dropped at your feet."*

> **THE LINE IS THE ROW, NOT THE DROP.** The predecessor project dropped it **silently**, and Row 10b
> ruled that a full-inventory drop is SAID OUT LOUD. **A silent drop looks identical to a deleted
> item from the player's side**, and the only difference is whether they think to look down.
>
> **Watch the ground as well as the chat** — a line with no item, or an item with no line, are
> different defects.

**READING:** **PASS.** Read 2026-09-17, 05:25-05:27, with the SLICE 8 block.

## ROW 62 — **THE CHOICE SURVIVES A REJOIN**

**Staging.** `/gamemode survival`. Choose a **storage** cell. **Quit the server entirely and
rejoin.**

**PREDICTED:** the star is in **that storage cell**, not the default hotbar slot, and the picker
shows it lime.

> **THIS IS WHERE A MISSING PERSIST OR AN OVER-EAGER VALIDATOR SHOWS.** A stored 20 that fails
> validation on load comes back as the DEFAULT, silently — `validSlotOr` hands out the fallback and
> says nothing, which is correct behaviour for a corrupt value and indistinguishable from this
> defect.
>
> **If the star is back on the hotbar, read the JSON before concluding anything**: the value being
> 20 on disk and 8 in play is a validator bug; the value being 8 on disk is a persist bug. **They
> look the same in game.**

**READING:** **PASS.** Read 2026-09-17, 05:25-05:27, with the SLICE 8 block.

## ROW 63 — **THE PICKER HIGHLIGHTS THE CURRENT SLOT, AND ONLY THAT ONE**

**Staging.** `/gamemode survival`. With the star in a known storage cell, open settings and
**look at the whole picker**.

**PREDICTED:** **exactly one** cell is lime and reads *"The Nexus sits here."*; **the other
thirty-five are white.** The lime one is the cell the star is actually in.

> **COUNT THE LIME CELLS.** A picker that highlights the right cell AND a second one — the old
> default, say — is a build reading two sources for one fact, and "my slot is highlighted" passes it.
>
> **The old screen had nine cells and a wrong highlight was obvious. Thirty-six is where one extra
> lime cell stops being obvious.**

**READING:** **PASS.** Read 2026-09-17, 05:25-05:27, with the SLICE 8 block.

## ROW 64 — **THE CONTROL. THE STAR IN SLOT 27 STILL OPENS THE HUB ON LEFT-CLICK.**

**Staging.** `/gamemode survival`. Put the star in a **storage** cell — slot 27, the middle of the
backpack. Press **E** and **left-click it**.

**PREDICTED:** the **hub opens**, exactly as it does from the hotbar. The star does not move; no
chat line, no sound.

> ### *** THIS IS SLICE 7's WHOLE REASON FOR EXISTING, AND THE REASON THIS SLICE IS SAFE TO SHIP ***
>
> **A star in slot 27 is invisible** — it is not on the hotbar, so right-clicking it in the world is
> not available. **Without slice 7's inventory click there would be no way to open the hub at all**
> from a storage cell, and this slice would be shipping a setting that can strand a player.
>
> **Rows 49–57 read that gesture at the DEFAULT slot on 2026-09-17.** This row reads it at a slot
> that did not exist as a choice when they were run. **If this fails, slice 8 must not ship** —
> not because the picker is wrong, but because the escape hatch is.
>
> **AND THE SECOND HALF: press E and left-click an ORDINARY item beside it.** It must still just
> pick up. Row 57's control, re-read in the one place this slice could have broken it.

**READING:** **PASS.** Read 2026-09-17, 05:25-05:27, with the SLICE 8 block.

> ### *** THIS READING PREDATES THE MERGE IT AUTHORISED, AND THAT IS WHY IT IS SAID OUT LOUD ***
>
> This row says **"if this fails, slice 8 must not ship"**. It did not fail — but **`#117` merged
> with this file still reading `_(not run)_`**, and for a day the record showed a slice shipped
> against an unmet ship condition.
>
> **NOTHING WAS WRONG WITH THE DECISION. THE OPERATOR HAD READ THIS ROW BEFORE MERGING.** What was
> missing was the WRITING DOWN, and **a ship condition with no recorded reading is
> indistinguishable from one nobody checked** — which is the single thing this row exists to say.
>
> **The gap is the gap this file's own rule names:** a finding that lives only in the conversation
> is not recorded. The reading existed, in a person's memory, where nothing can grep it and nothing
> can fail.
>
> **Practically, and it is the transferable part: a row marked as a SHIP CONDITION should be
> written in before the merge it gates, not after.** The reading is the authorisation; the merge is
> what it authorises, and they went in the wrong order.

## ROW 65 — **THE ARMOUR SLOTS AND THE OFFHAND ARE STILL OUT**

**Staging.** `/gamemode survival`. Look at the picker. Then, with the star in a storage cell, try to
**drag or shift-click it into an armour slot or the offhand** from your own inventory.

**PREDICTED:** the picker offers **no cell for armour or the offhand** — thirty-six choosers and no
more — and the lock **refuses** the move, silently, as it does today.

> **THE PICKER IS THE FIRST HALF AND THE LOCK IS THE SECOND, AND THEY ARE DIFFERENT GUARDS.** The
> picker not offering a cell is a layout fact; the lock refusing the gesture is behaviour that
> predates this slice. **A build that widened `MAX_SLOT` to 40 would pass every other row in this
> block** — the picker would still show 36 cells, because its size is its own constant.
>
> **`MAX_SLOT` is what this row is really reading**, one layer down.

**READING:** **PASS.** Read 2026-09-17, 05:25-05:27, with the SLICE 8 block.

---

# SLICE 9 — PLAYER LEVEL. ROWS 66–77

**Status: RUN 2026-09-17, 06:48-06:52. ALL TWELVE PASS.**

> **Every row below was still written BEFORE any boot**, and no prediction was edited once a
> reading was taken — the readings sit beside them.
>
> **THE TWO ROWS THAT WERE CARRYING MORE THAN A PREDICTION BOTH HELD.** Row 67, the mob row, is the
> only row in this file that touches the real player path, and it is what stops a build with the
> listener deleted passing the block. Row 74 carried a provenance flag saying its ruling was made
> from the javadoc rather than the screen; **the screen agreed, and the flag is discharged** in a
> note under that row rather than left reading as open.

**GAME MODE: `/gamemode survival` for all twelve.**

> **SURVIVAL IS LOAD-BEARING HERE, NOT BOILERPLATE, AND IT IS THE SAME REASON SLICE 6 GAVE.**
> **Creative hides the XP bar.** Rows 67 and 68 read the vanilla bar and the player level *against
> each other* — that comparison is the whole content of both — so in creative they would be
> **VOID rather than failed**, which is what `GATE-nexus.md` Row 6 cost a boot to learn.

## *** STAGING PREAMBLE: `/xp` DOES NOT FIRE `PlayerExpChangeEvent`. MEASURED, NOT ASSUMED. ***

**This was measured before these rows were written, because the answer decides how every one of
them stages.** Instrument: `javap` over the pinned `run/versions/26.1.2/paper-26.1.2.jar`
(19,635 entries, 10,178 classes), 2026-09-17.

**Exactly two classes in the whole server jar reference `PlayerExpChangeEvent`:**

```
$ grep -rla "PlayerExpChangeEvent" --include='*.class' .
./net/minecraft/world/entity/ExperienceOrb.class            <- raises it
./org/bukkit/craftbukkit/event/CraftEventFactory.class      <- builds it
```

**Positive control, identical search:** `PlayerJoinEvent` → 2 classes. The search can find things.

The factory settles it: `callPlayerExpChangeEvent(Player, ExperienceOrb, int)` — **it requires an
orb and there is no orbless overload.** `/xp`'s four arms resolve, from `ExperienceCommand$Type`'s
BootstrapMethods table, to `Player.giveExperiencePoints`, `ServerPlayer.giveExperienceLevels`,
`setExperiencePoints` and `setExperienceLevels`. **No orb among them**, and
`giveExperiencePoints` raises no Bukkit event at all.

**SO: NO ROW MAY STAGE PLAYER XP WITH `/xp`.** Every row that needs a level stages with
**`/rpg playerxp`**, which writes the profile directly. **Row 67 is the sole exception and stages
from a REAL ORB PICKUP**, and Row 68 reads the `/xp` divergence deliberately.

> **AND `/rpg playerxp` MAKES EVERY THRESHOLD A ONE-LINER, WHICH IS EXACTLY THE HAZARD.** The
> command bypasses `PlayerExpChangeEvent` entirely, so **a build with the listener deleted passes
> every command-staged row in this block.** Row 67 is the only row in the file that touches the real
> player path, and it is mandatory for that reason alone.
>
> `ProgressionWiringSignatureTest` is the two-second half of the same guard — it fails if the
> handler is missing, is not at `LOWEST`, or grows a multiplier — but **a source scan cannot
> witness an orb**, which is why both exist.

## THE CHEAP STAGING INSTRUMENT, IF A ROW NEEDS REAL ORBS

**A bottle o' enchanting produces real `ExperienceOrb` entities** — measured the same way:
`ThrownExperienceBottle` calls `ExperienceOrb.awardWithDirection`. It is `/give`-able, throwable in
survival, and repeatable, so **Row 67 does not need a mob farm** if a mob is inconvenient. The row
is written for a mob because that is the path players actually walk.

**What the unit suite already settled.** `PlayerLevelTest` pins the curve, both clamps and every
boundary; `XpGrantTest` pins the four `add|set × levels|xp` arms and the negative refusal;
`PlayerLevelLinesTest` pins the three rendered lines including the cap; `NexusStationGateTest` pins
`3 / 10 / 13` inclusively and both locked-lore facts; `NexusStatsLoreTest` pins the block's position
and the missing "To Next" line at 99; `ProfileServiceTest` pins accumulation and the level-change
write; `FilePlayerRepositoryTest` pins the absent-`lifetimeXp` zero against a real v3 JSON file.

**What none of them can see** is an orb, a vanilla XP bar, a hijacked world block, or a rendered
tooltip. **That gap is these twelve rows.**

---

## ROW 66 — `/rpg playerxp set` LANDS EXACTLY ON A THRESHOLD, AND THE HEAD SAYS SO

**CONDITIONS:** survival, operator. Fresh or arbitrary starting level.

**STAGE:** `/rpg playerxp set <you> 13 levels`, then open the Nexus and hover the stats head.

**PREDICTED:** the command replies naming **level 13** and **19,980 lifetime**. The head reads:

```
Level        13
Lifetime XP  19,980
To Next      2,770
```

> **`set N levels` IS A LOOKUP HERE AND WAS A DIRECT FIELD WRITE IN THE PREDECESSOR.** We store
> lifetime XP and derive the level, so it resolves through the prefix-sum table.
> **Zero progress into 13 is the point** — an operator staging a threshold wants to be ON it.
>
> **2,770 IS RUNG 13 AND IS NOT 3,020.** That literal was typed from memory once and
> `PlayerLevelLinesTest` caught it; 3,020 is rung 14.

**READING:** **PASS.** Read 2026-09-17, 06:48-06:52, with the SLICE 9 block.

---

## ROW 67 — *** THE MOB ROW. THE ONLY ROW THAT TOUCHES THE REAL PLAYER PATH. ***

**CONDITIONS:** survival. **Not creative — the bar is the point.**

**STAGE:** `/rpg playerxp set <you> 0 xp` first, so the starting total is known. Note the vanilla
XP bar. **Kill one mob.** Read the bar, then open the Nexus and read the head.

**PREDICTED:** **BOTH NUMBERS MOVE, FROM ONE GESTURE.** The vanilla bar rises by the mob's own
award, and `Lifetime XP` on the head rises by **the same number**, one for one.

> **THIS ROW IS WHY THE BLOCK IS NOT SELF-SATISFYING.** Every other row stages with
> `/rpg playerxp`, which writes the profile and never raises `PlayerExpChangeEvent`. **Delete
> `RpgListeners.onPlayerExpChange` and every other row here still passes.** This one goes red.
>
> **READ THE TWO NUMBERS AGAINST EACH OTHER, not just that each moved.** A zombie awards 5; if the
> bar moves 5 and lifetime moves 5, the 1:1 claim is tested. If they move by *different* amounts a
> multiplier has appeared between the event and the profile.
>
> **Do not stage this at a level boundary.** Crossing one triggers the disk write, which is Row 76's
> subject; keeping them apart means neither row can pass on the other's mechanism.

**READING:** **PASS.** Read 2026-09-17, 06:48-06:52, with the SLICE 9 block.

---

## ROW 68 — `/xp` MOVES THE BAR AND NOT THE LEVEL, AND THAT IS RECORDED RATHER THAN DISCOVERED

**CONDITIONS:** survival, operator.

**STAGE:** note the head's `Lifetime XP`. Run `/xp add 5000 points`. Read the bar, then re-open the
Nexus and read the head.

**PREDICTED:** **the vanilla XP bar jumps. `Lifetime XP` and `Level` DO NOT MOVE AT ALL.**

> **THIS IS THE BYTECODE MEASUREMENT IN THE PREAMBLE, CONFIRMED IN PLAY.** It gets its own row
> because **an operator granting XP by command is a thing that will happen again**, and *"the level
> did not move"* deserves a recorded answer instead of a second investigation.
>
> **It is a CONSEQUENCE OF THE HOOK, NOT A GUARD.** There is no check anywhere that refuses `/xp`;
> the event simply is not raised. Nothing goes red if someone adds a third wallet writer — which is
> exactly why it is written down.
>
> **The same mechanism is why the enchant table's spend and the grindstone's refund cannot move a
> player's level:** both are wallet-symmetric `setLevel`/`setExp` writes, and `CraftPlayer` is not
> one of the two classes that reference the event.

**READING:** **PASS.** Read 2026-09-17, 06:48-06:52, with the SLICE 9 block.

---

## ROW 69 — A NEW PLAYER'S HUB HAS THREE LOCKED STATIONS, AND THE LORE SAYS BOTH FACTS

**CONDITIONS:** survival. `/rpg playerxp set <you> 0 xp` — level 1.

**STAGE:** open the Nexus. Hover each of crafting (31), enchanting (32) and the grindstone (33).

**PREDICTED:** all three are **dimmed** — the name in dark gray rather than gray — and each keeps
**its own material and its own name**, so the player can still tell which is which. Each reads:

```
Locked -- unlocks at level 3        (10 for enchanting, 13 for the grindstone)
You are level 1.
A crafting table in the world still works.
```

> **BOTH FACTS OR THE LORE IS USELESS.** Line 1 alone tells a player the feature is unavailable,
> which is **false** — it is twenty blocks away in their base. Line 3 alone hides that the hub route
> is coming.
>
> **CRAFTING IS LOCKED AT LEVEL 1 TOO**, and that is deliberate rather than an oversight: 3 is not 1.
> "The first station is effectively open" is the assumption someone will make when trimming this.
>
> **The settings torch (50) and the stats head (13) are NOT gated** — a locked-out player must still
> be able to move their star and read the level they need. Check both are undimmed while here.

**READING:** **PASS.** Read 2026-09-17, 06:48-06:52, with the SLICE 9 block.

---

## ROW 70 — CRAFTING OPENS AT EXACTLY 3

**CONDITIONS:** survival, operator.

**STAGE:** `/rpg playerxp set <you> 2 levels`, open the hub, click crafting. Then
`/rpg playerxp set <you> 3 levels`, re-open, click crafting.

**PREDICTED:** at **2** the click does **nothing** and the icon is dimmed. At **3** the crafting
screen opens.

> **THE BOUNDARY, BOTH SIDES, BECAUSE AN OFF-BY-ONE HERE IS INVISIBLE IN PLAY.** A gate that fired
> at 4 instead of 3 would never be noticed by anyone who did not stand exactly on the threshold.
>
> **`set N levels` lands on the threshold with zero progress**, so "level 3" here means exactly
> 2,090 lifetime — the tightest staging available.

**READING:** **PASS.** Read 2026-09-17, 06:48-06:52, with the SLICE 9 block.

---

## ROW 71 — ENCHANTING OPENS AT EXACTLY 10

**CONDITIONS:** survival, operator.

**STAGE:** `/rpg playerxp set <you> 9 levels`, click enchanting. Then `set 10 levels`, click again.

**PREDICTED:** locked at **9**, opens at **10** — an unpowered table reading **0/30**.

> **SEPARATE FROM ROW 70 RATHER THAN FOLDED INTO IT**, because the three thresholds are three
> independent literals and a single row staged at one level cannot distinguish "the gate works" from
> "all three share one number".

**READING:** **PASS.** Read 2026-09-17, 06:48-06:52, with the SLICE 9 block.

---

## ROW 72 — THE GRINDSTONE OPENS AT EXACTLY 13

**CONDITIONS:** survival, operator.

**STAGE:** `/rpg playerxp set <you> 12 levels`, click the grindstone. Then `set 13 levels`, again.

**PREDICTED:** locked at **12**, opens at **13** — the tray, and the gray *"Add weapons to strip"*
button.

**READING:** **PASS.** Read 2026-09-17, 06:48-06:52, with the SLICE 9 block.

---

## ROW 73 — *** THE WORLD BLOCKS ARE NOT GATED. AT LEVEL 1, ALL THREE OPEN. ***

**CONDITIONS:** survival. `/rpg playerxp set <you> 0 xp` — level 1, every hub station locked.

**STAGE:** place a crafting table, an enchanting table and a grindstone. Right-click each,
**not sneaking**.

**PREDICTED:** **all three of our screens open normally**, at level 1, with every hub station locked
behind them.

> **BEN'S RULING, AND IT IS THE HALF THE IMPLEMENTATION HAD TO BE SHAPED AROUND.** The gate lives in
> `NexusStationGate`, consulted by `NexusMenu` alone. **`CraftingMenu`, `EnchantMenu` and
> `GrindstoneMenu` were not touched by this slice** — putting the check in them would have gated the
> blocks too, and would have put one rule in three places.
>
> **THIS ROW IS THE ONE THAT FAILS IF SOMEBODY "TIDIES" THE GATE INTO THE MENUS.** Nothing else in
> the block would notice, because every other row reaches those screens through the hub.

**READING:** **PASS.** Read 2026-09-17, 06:48-06:52, with the SLICE 9 block.

---

## ROW 74 — A LOCKED STATION'S CLICK REFUSES **AND SAYS WHY**, WITH BOTH FACTS

**CONDITIONS:** survival, level 1, chat visible and clear.

**STAGE:** open the hub and click all three locked stations, several times each.
**Do NOT hover first** — that is the condition this row exists for.

**PREDICTED:** no screen change and no sound; the hub stays open. **One gray chat line per click**,
naming the station, its unlock level, the player's level, and the world block:

```
Crafting unlocks at level 3. You are level 1. A crafting table in the world still works.
Enchanting unlocks at level 10. You are level 1. An enchanting table in the world still works.
Grindstone unlocks at level 13. You are level 1. A grindstone in the world still works.
```

> ***THIS ROW WAS WRITTEN AS "DOES NOTHING AND SAYS NOTHING" AND WAS INVERTED BY RULING BEFORE
> ANY BOOT.*** It is restaged rather than edited-in-place-and-forgotten, and the reason it flipped
> is worth more than either prediction:
>
> **THE MESSAGE IS MANDATORY BECAUSE THE ICON KEEPS THE STATION'S MATERIAL.** A locked station
> renders as its own crafting table / enchanting table / grindstone, dimmed only in its NAME — and
> **a name lives in the hover tooltip.** Without hovering, a locked station is **pixel-identical**
> to an open one. Silence there is a normal-looking crafting table that does nothing and explains
> nothing: indistinguishable from a broken menu, and **it lands on the player least equipped to
> interpret it** — someone at level 1 opening the hub for the first time.
>
> **THE GRINDSTONE'S SILENCE RULING DOES NOT TRANSFER, AND THE PREMISE IS WHY.** It reads *"the
> button has already said it"* — and what THAT button says without being asked is **COLOUR**: lime,
> yellow, red, gray, at a glance, on the cell being clicked. This one says nothing without a hover.
> **Same shape of rule, different premise, opposite answer.**
>
> **ONLY THREE OF THE FOUR COMBINATIONS ARE COHERENT:**
>
> | icon | click | |
> |---|---|---|
> | material CHANGES | silent | you can see it is locked before you click |
> | **material SAME** | **SPEAKS** | **you find out by clicking, the only gesture you have** ← ours |
> | material changes | speaks | coherent, mildly redundant |
> | material SAME | silent | **a crafting table that does nothing** ← the one to avoid |
>
> **PROVENANCE, AND IT IS PART OF THE ROW.** This ruling was made **from the javadoc, not from the
> screen** — nobody had opened the hub at level 1 when it was made. **Overrule it in one word the
> first time you do.** Recorded because a call made without looking should be visible as one.

**READING:** **PASS.** Read 2026-09-17, 06:48-06:52, with the SLICE 9 block.

> ### *** THE PROVENANCE FLAG IS DISCHARGED. THE SCREEN AGREED WITH THE JAVADOC. ***
>
> The ruling above was made without opening the hub at level 1, and said so, and invited being
> overruled by the first person who did. **Somebody has now done that and it held.**
>
> **RECORDED RATHER THAN LEFT STANDING, BECAUSE AN OPEN FLAG AND A DISCHARGED ONE READ IDENTICALLY.**
> A caveat that has been checked looks exactly like one nobody has got to yet, and the next reader
> would otherwise treat a settled question as still open — or, worse, re-open it on the strength of
> a warning that has already done its job.
>
> **What is discharged is narrow: that the message is right FOR THIS SCREEN AS IT RENDERS TODAY.**
> It is still coupled to the icon keeping the station's material. **Change that and the question
> comes back**, and `NexusStationGate.refusal`'s four-combination table is what it comes back to.

---

## ROW 75 — AT THE CAP THERE IS NO "TO NEXT" LINE, AND THE LEVEL CARRIES THE MARKER

**CONDITIONS:** survival, operator.

**STAGE:** `/rpg playerxp set <you> 99 levels`. Open the Nexus, hover the head.

**PREDICTED:** two progression lines, **not three**:

```
Level        99 (MAX)
Lifetime XP  11,642,250
```

**No "To Next" line at all.** Then `/rpg playerxp set <you> 98 levels` and re-open: **three lines
return.**

> ***THE PREDECESSOR RETURNED `Long.MAX_VALUE` HERE AND IT RENDERED.*** The tempting fix is a word
> in the same column — `To Next      MAX` — and it is the same defect in better clothes: that
> column's subject is an **amount remaining**, and there is none. So the line is **absent** and the
> marker goes on the level line, where it is a fact about a state rather than a number in a column
> that should be empty.
>
> **The 98 half is what stops "two lines" being equally consistent with the line having been dropped
> everywhere.**

**READING:** **PASS.** Read 2026-09-17, 06:48-06:52, with the SLICE 9 block.

---

## ROW 76 — LIFETIME XP SURVIVES A DISCONNECT, AND A LEVEL-UP IS WRITTEN IMMEDIATELY

**CONDITIONS:** survival, operator. Two parts, both needed.

**STAGE, part A:** `/rpg playerxp set <you> 800 xp` (level 1, below the 1,000 boundary). **Quit and
rejoin.** Read the head.

**STAGE, part B:** `/rpg playerxp set <you> 800 xp` again, then **kill mobs until the head shows
level 2**, then **kill the server process without quitting cleanly**. Restart, rejoin, read the head.

**PREDICTED:** **A** — 800 lifetime, level 1, unchanged. **B** — **level 2 survives**, because
crossing a level boundary writes through immediately.

> **THE PERSISTENCE POLICY, AND IT IS THE ONE JUDGEMENT CALL IN THIS SLICE THAT WAS MINE.** Every
> gain updates the cache; **only a level change writes to disk.** `setNexusSlot` writes through on
> every call and is right to — it is rare and deliberate. **This fires once per orb**, so the same
> policy would be a file write per orb per player, which is a forty-player problem rather than a
> one-player one.
>
> **The accepted cost, stated so part B is read as the bound and not as a guarantee:** a server that
> dies without quitting loses whatever has been earned **since the last level-up**.
>
> **THE BOUND IS ONE RUNG OF THE CURVE, AND NEAR THE CAP THAT IS NOT SMALL.** This sentence said
> *"bounded by one level, never by a whole session"* until 2026-09-17, and **the second half was
> false**: rung 98 is **400,000 XP**, which can span several sessions. **The write-on-level-change
> bounds the loss in LEVELS, not in TIME**, and the two diverge exactly where a player has most to
> lose. Still the right trade against a file write per orb per player — but the cost is now stated
> honestly rather than inherited as a comfortable number.
>
> **Part B is deliberately a HARD KILL.** A clean quit persists everything and would pass whether
> the level-change write exists or not — **a control that succeeds for the wrong reason.**

**READING:** **PASS.** Read 2026-09-17, 06:48-06:52, with the SLICE 9 block.

---

## ROW 77 — `add` REFUSES A NEGATIVE, IN BOTH UNITS, AND NAMES THE COMMAND THAT DOES NOT

**CONDITIONS:** survival, operator.

**STAGE:** `/rpg playerxp add <you> -100 xp`, then `/rpg playerxp add <you> -1 levels`. Then
`/rpg playerxp set <you> 5 levels` to confirm `set` is not refused.

**PREDICTED:** both `add` forms print, in red:

```
'add' requires a non-negative amount; use 'set' for absolute values.
```

and **the level does not move**. `set` is accepted.

> **BOTH UNITS, BECAUSE THE PREDECESSOR ONLY REFUSED ONE.** Its check sat inside the `xp` branch, so
> `add -3 levels` there **quietly demoted a player** through the very command that refuses
> `add -3 xp`. The message is about `add`, so it binds `add`; this is a **tightening**, and it is the
> defect the port found in its own source.
>
> **`set` IS THE SANCTIONED EXCEPTION TO THE MONOTONIC INVARIANT** — `PlayerLevel` says lifetime XP
> never decreases and every other writer obeys it. An operator's hand on the dial is not the game.

**READING:** **PASS.** Read 2026-09-17, 06:48-06:52, with the SLICE 9 block.

---

# SLICE 10 — /menu, AND THE SETTINGS MENU BECOMES A HUB. ROWS 78–91

**Status: NOT RUN.** Every row below was written **before any boot**.

**GAME MODE: `/gamemode survival` for all fourteen.**

> **SURVIVAL IS LOAD-BEARING FOR THREE OF THEM, NOT BOILERPLATE.** Rows 83, 84 and 85 read whether a
> CLONE CAN LEAVE THE PICKER. **Creative removes the cost of having an item at all** — a player can
> conjure the same stack from the creative menu, so *"there is now an extra one"* is satisfied for
> free and the row passes without exercising anything. That is the creative-divergence register's
> own shape: **creative removes a cost, and a row whose reading is "the thing is there" then tests
> nothing.**

## *** THE DENOMINATOR, AND IT NAMES A SHA ***

**77 at `79a64a3`** (`#119`'s squash), before this block. Recounted, not adjusted, and the ref is a
SHA rather than `origin/master` — see the rule in the STATUS section: a count against a moving ref
stays correct while its referent moves out from under it.

```
git grep -c '^## ROW' 79a64a3 -- GATE-nexus.md
```

## EVERY READING LINE BELOW BEGINS `**READING:**`

**This is the first block written since `#119` fixed the vocabulary, so it is the one that says
whether the fix held.** No `READING — <date>` variant, no `READING (restaged N)` variant. If a row
here is ever restaged, the restaging goes AFTER the prefix.

## *** WHAT THIS SLICE PUTS AT RISK, AND IT IS NOT THE MENU ***

**The picker now contains CLONES OF EVERY ITEM THE PLAYER OWNS.** If any route lets one leave that
screen, it is free duplication of anything in their inventory — **an economy hole, not a display
bug**, and one that scales with how much they are carrying.

**Rows 83, 84 and 85 are that risk, read three times, and they MUST NOT BE COLLAPSED INTO ONE.**
`MenuRouting` handles shift-click, the number keys and F on **three different code paths**, and
**two of them act on the HOVERED slot rather than on a declared input** — so a build that refuses
one can permit another. **One row passing says nothing about the other two.**

**What the unit suite already settled.** `NexusSlotPickerLayoutTest` pins the mirror at both seams,
the bijection onto 0..35, the filler by subtraction, the unique slot names, and the chrome agreeing
with the screen one click up; `SettingsMenuLayoutTest` pins the two buttons, the deliberate gap at
23, and `SETTING_SLOTS` as one list used twice; `FilePlayerRepositoryTest` pins that an absent
`starEnabled` key reads as ENABLED against a real v3 file, and that an untouched toggle writes no
key at all.

**What none of them can see** is a router, a real `PlayerInventory`, a cloned `ItemStack`, a join,
a respawn, or a command typed by a player who is not an operator. **That gap is these fourteen
rows.**

---

## ROW 78 — `/menu` OPENS THE HUB

**CONDITIONS:** survival, operator, star present and enabled.

**STAGE:** type `/menu`.

**PREDICTED:** the **Nexus hub opens** — the same screen the star opens, with the stats head at 13,
the three stations at 31/32/33 and the settings torch at 50. No chat line.

> **THE SAME SCREEN, NOT A SECOND ONE.** `MenuCommand` builds a `NexusMenu` with the services the
> plugin already holds, including **the one shared `RecipeCatalogue`** — a second instance would be
> a second lifetime cache, and the two would agree until one had seen a recipe the other had not.

**READING:** _(not run)_

---

## ROW 79 — `/menu` WORKS FOR A NON-OPERATOR

**CONDITIONS:** survival. **A player who is NOT op** — `/deop <them>` first, and confirm with a
command they should be refused, e.g. `/rpg playerxp add <them> 1 xp`, which must say they lack
permission.

**STAGE:** as that player, type `/menu`.

**PREDICTED:** **the hub opens.** No permission error.

> **`rpg.command.menu` IS DECLARED `default: true`**, like `cast`, `class` and `stats`. It is
> declared rather than left off `requires()` **so an admin can revoke it** — an undeclared node
> cannot be taken away, only worked around.
>
> **THE `/deop` HALF IS THE CONTROL AND IT IS NOT OPTIONAL.** Run as an operator, this row passes
> on a build where the node defaults to `op` and proves nothing. **The refused command is what says
> the player really is unprivileged**; without it, the row is measuring the fixture.

**READING:** _(not run)_

---

## ROW 80 — *** `/menu` WITH THE STAR DISABLED. THE ROW THE WHOLE ORDERING EXISTS FOR. ***

**CONDITIONS:** survival. Open the hub, go to Settings, **switch the Nexus star OFF**, and confirm
it is gone from the inventory.

**STAGE:** with **no star anywhere in the inventory**, type `/menu`.

**PREDICTED:** **the hub opens.**

> ***THIS IS WHY `/menu` AND THE TOGGLE SHIP IN THE SAME SLICE.*** The toggle deletes the star.
> Without a second route to the hub, **that switch is a ONE-WAY DOOR**: a player turns the star off,
> and the screen that would turn it back on is the screen they can no longer reach.
>
> **If this row fails, the slice must not ship** — not because the toggle is wrong, but because the
> escape hatch is. Same shape as Row 64 for slice 8, and **it is a SHIP CONDITION, so its reading is
> written into this file BEFORE the merge it gates**, which is the lesson Row 64 cost.

**READING:** _(not run)_

---

## ROW 81 — THREE SCREENS, TWO BACKS, ONE CLOSE CELL — AND THE LABELS DIFFER

**CONDITIONS:** survival, star enabled.

**STAGE:** `/menu` → click Settings (50) → click **Nexus Slot** (22). Then press **Back** (48) once,
and again. Then re-enter and press **Close** (49) on each of the three screens.

**PREDICTED:** the chain is `Nexus → Settings → Nexus Slot`. **Back on the picker reads "Back to
Settings"** and lands on Settings; **Back on Settings reads "Back to the Nexus"** and lands on the
hub. **Close is 49 on all three** and closes outright.

> **THE PREDECESSOR GOT THIS EXACTLY WRONG AND THIS ROW IS AIMED AT ITS DEFECT.** Its picker used
> **back 49 / close 53** while its own settings screen used **48/49** — so **49 meant "back" on one
> screen and "close" on the very next one.** A player who had learned either had learned a trap.
>
> **Same slot, same shape, DIFFERENT WORD is the convention working, not a collision.** The label
> names the DESTINATION, and the two destinations genuinely differ — one step up versus two.

**READING:** _(not run)_

---

## ROW 82 — THE PICKER SHOWS REAL ITEMS, AND EXACTLY ONE CELL IS LIME

**CONDITIONS:** survival. Put **at least six distinguishable items** in scattered cells — hotbar and
storage both — and leave several cells empty. Star enabled, in a known slot.

**STAGE:** `/menu` → Settings → Nexus Slot. Read the grid.

**PREDICTED:**

- every occupied cell renders **the player's own item**, recognisable, with its own name;
- every empty cell is a **LIGHT GRAY** pane reading `Empty (Row 2, slot 4)` and
  `Click to move the Nexus here.`;
- **exactly ONE cell is LIME**, reading `Current slot (Hotbar 9)` — and it is the star's cell;
- the hotbar is the **bottom row** of the picker, and storage is above it, in the player's own
  order.

> **COUNT THE LIME CELLS RATHER THAN CONFIRMING THE EXPECTED ONE IS LIME.** *"Slot 9 is lime"* is
> satisfied by a build that paints every cell lime. **Exactly one** is the claim.
>
> **AND THE CURRENT CELL SHOWS THE PANE, NOT THE STAR IT CONTAINS.** Rendering the occupant there
> would paint a second Nexus star onto the screen — the shape `converge`'s surplus deletion exists
> to stop people creating.

**READING:** _(not run)_

---

## ROW 83 — *** SHIFT-CLICK A CLONED CELL. NOTHING LEAVES. ***

**CONDITIONS:** survival. **Count the target stack first** — hold it, read the number, write it
down. Leave at least one free inventory cell.

**STAGE:** in the picker, **shift-click a cell showing a real item.**

**PREDICTED:** **nothing moves.** No item appears in the inventory, the picker cell is unchanged,
and the screen stays open. **Close the picker and RE-COUNT the stack: the number is the same.**

> **THE RE-COUNT IS THE READING, NOT THE SCREEN.** A clone that left would land in the inventory
> behind the open menu, where it is invisible until the screen closes. *"Nothing happened on
> screen"* is not the same claim.
>
> **THIS IS THE DECLARED-INPUT PATH.** The picker declares `inputSlots()` empty, so `MenuRouting`
> should have no destination to move to. **Rows 84 and 85 are the other two paths and are NOT
> covered by this one.**

**READING:** _(not run)_

---

## ROW 84 — *** NUMBER-KEY A CLONED CELL. NOTHING LEAVES. ***

**CONDITIONS:** survival. **Empty hotbar slot 1**, and the count of the target stack written down.

**STAGE:** in the picker, **hover a cell showing a real item and press `1`.**

**PREDICTED:** **nothing moves.** Hotbar slot 1 is still empty after closing, and the stack count is
unchanged.

> ***A SEPARATE ROW BECAUSE THIS PATH ASKS A DIFFERENT QUESTION.*** `hotbarMove` acts on the
> **HOVERED** slot, not on a declared input — so a build with no input slots can still be asked to
> move the hovered cell into hotbar 1. **Row 83 passing says nothing about this.**
>
> **HOTBAR 1 MUST BE EMPTY FIRST.** Pressing `1` over a cell while hotbar 1 is occupied can be a
> SWAP, and *"nothing arrived"* would then be satisfied by a swap that failed for an unrelated
> reason.

**READING:** _(not run)_

---

## ROW 85 — *** PRESS F ON A CLONED CELL. NOTHING LEAVES. ***

**CONDITIONS:** survival. **Empty offhand**, and the count of the target stack written down.

**STAGE:** in the picker, **hover a cell showing a real item and press `F`.**

**PREDICTED:** **nothing moves.** The offhand is still empty after closing, and the stack count is
unchanged.

> **THE THIRD PATH.** `offhandMove` is its own arm and also reads the **HOVERED** slot. Three
> gestures, three arms, three rows — **and the reason they cannot be one row is that they are not
> one code path.** A single "extraction" row would pass on a build that refuses two of three, and
> the third is a duplication hole.
>
> **EMPTY OFFHAND FIRST**, for Row 84's reason: F is a swap, and a full offhand makes "nothing
> arrived" ambiguous.

**READING:** _(not run)_

---

## ROW 86 — THE TOGGLE OFF REMOVES THE STAR **AND FREES THE SLOT**

**CONDITIONS:** survival. Star enabled and in a known slot — say Hotbar 9. Have a spare ordinary
item to hand.

**STAGE:** `/menu` → Settings → click the toggle (24). Then **close everything and try to put the
ordinary item into Hotbar 9**, and to pick it back out.

**PREDICTED:** the star **disappears from the inventory**; the toggle reads **`Nexus Star: OFF`** in
gray with lore naming `/menu`; and **Hotbar 9 accepts the ordinary item and gives it back
normally.**

> ***BOTH HALVES, AND THE SECOND IS THE ONE THAT WOULD BE MISSED.*** `NexusLock` refuses the locked
> slot **whether or not it holds a star** — deliberately, so a lost star cannot have its cell taken
> before the next join restores it. **Right while the feature is ON and exactly wrong while it is
> off:** removing the item and leaving the slot locked hands the player a cell they can neither fill
> nor use, containing nothing, with nothing on screen to explain it.
>
> **A build that only deletes the item passes the first half of this row and fails the second.**
> That is why the item test is in the prediction rather than a follow-up.

**READING:** _(not run)_

---

## ROW 87 — A JOIN DOES NOT RE-MINT WHILE DISABLED

**CONDITIONS:** survival, star already switched OFF (Row 86).

**STAGE:** quit and rejoin. Read the inventory.

**PREDICTED:** **no star.** The chosen cell is still ordinary.

> **`converge` MINTS WHEN IT FINDS NONE**, so without a guard the toggle would last exactly until
> the next join and read as *the setting not sticking*. **This row is that guard.**

**READING:** _(not run)_

---

## ROW 88 — A RESPAWN DOES NOT RE-MINT WHILE DISABLED

**CONDITIONS:** survival, star OFF, and a way to die that is quick — a fall, or `/kill`.

**STAGE:** die, respawn. Read the inventory.

**PREDICTED:** **no star.**

> ***SEPARATE FROM ROW 87 BECAUSE IT IS A SEPARATE CALL SITE.*** Join and respawn each call
> `converge` and each needed its own guard. **Guarding only the join would leave the star returning
> on death** — a stranger report than it not sticking across a session, and one nobody would connect
> to a setting.

**READING:** _(not run)_

---

## ROW 89 — THE TOGGLE ON REFUSES AN OCCUPIED SLOT, AND NAMES IT

**CONDITIONS:** survival, star OFF, chosen cell known — say Hotbar 9.

**STAGE:** **put an ordinary item into Hotbar 9** (legal now — the lock is off). Then `/menu` →
Settings → click the toggle.

**PREDICTED:** **no star appears, the item is untouched**, the toggle still reads OFF, and a red
chat line names the cell:

```
Hotbar 9 is occupied. Clear it or use the slot picker first.
```

Then clear the cell, click the toggle again: **the star arrives in Hotbar 9.**

> **THE ALTERNATIVE IS DISPLACING AN ITEM THE PLAYER DID NOT ASK US TO MOVE**, at the moment they
> pressed a button about something else. `converge` would happily displace it — **right on JOIN,
> where nobody is watching and the star must exist; wrong here.**
>
> **THE SECOND HALF IS NOT OPTIONAL.** Without clearing and re-clicking, this row is equally
> satisfied by a build where the toggle never turns on at all.

**READING:** _(not run)_

---

## ROW 90 — PICKING A SLOT DOES **NOT** SWITCH THE STAR BACK ON

**CONDITIONS:** survival, star OFF.

**STAGE:** `/menu` → Settings → Nexus Slot → click a different, **empty** cell.

**PREDICTED:** **no star appears.** The message names the new cell and says it arrives once the
player switches it back on. The lime cell moves to the new choice. Then switch the toggle on: **the
star appears in the NEW cell**, not the old one.

> **THE PREDECESSOR'S PICKER SILENTLY RE-ENABLED THE ITEM.** One setting changed by a different
> setting's button is a surprise, and there is a dedicated toggle one screen up. **A player picking
> a cell has expressed something about WHERE, not about WHETHER.**
>
> **The second half proves the preference was actually stored** rather than merely not acted on —
> without it, "no star appeared" is equally consistent with the click doing nothing at all.

**READING:** _(not run)_

---

## ROW 91 — *** THE CONTROL. WITH THE STAR ENABLED, SLICES 7 AND 8 STILL WORK. ***

**CONDITIONS:** survival, star **ENABLED**.

**STAGE:** four gestures, in order:

1. put the star in a **storage** cell via the picker — say Row 2, slot 4;
2. press **E** and **left-click the star** in that storage cell;
3. press **E** and **left-click an ordinary item** beside it;
4. try to **drag the star out** of its cell.

**PREDICTED:** **(1)** the star moves and the picker's lime cell follows; **(2)** the hub opens —
slice 7's gesture, at a slice 8 slot; **(3)** the ordinary item just picks up, normally;
**(4)** refused, silently, as before.

> **THE WHOLE POINT OF A CONTROL IS THAT THIS SLICE TOUCHED THE THINGS IT DEPENDS ON.**
> `lockedSlotOf` was **split in two** this slice — it now answers `NO_LOCKED_SLOT` while the star is
> off, and `chosenSlotOf` answers the stored preference. **Every refusal in slices 7 and 8 reads
> `lockedSlotOf`**, so a mistake in that split takes the lock off for everyone rather than only for
> a disabled star.
>
> **Gesture (3) is Row 57's control, re-read**, because a lock that refuses everything is as broken
> as one that refuses nothing and passes every other row here.

**READING:** _(not run)_

---

## MUTATION EVIDENCE — SLICE 10. RUN 2026-09-17

**Six mutations, every one applied and measured, NO KILL SET PREDICTED, no zero-kill results.**

| mutation | file | rows killed | what it proves |
|---|---|---|---|
| `MUTPICK-OFFBYNINE` identity arm → `menuSlot - 9` | `NexusSlotPickerLayout` | 1 | the mirror's identity half |
| `MUTPICK-BACK49` `BACK_SLOT 48 → 49` | `NexusSlotPickerLayout` | **3** | **the predecessor's exact defect** |
| `MUTSETTINGS-HOLE` drop the toggle from `SETTING_SLOTS` | `SettingsMenuLayout` | 1 | the painted-hole guard |
| `MUTSTAR-DEFAULTFALSE` accessor → `!= null &&` | `PlayerProfile` | **3** | absent means ENABLED |
| `MUTSTAR-UNKNOWNOFF` `orElse(true) → orElse(false)` | `ProfileService` | 1 | unknown cannot strip a star |
| `MUTSTAR-INVERT` `withStarEnabled(!enabled)` | `ProfileService` | 2 | the toggle writes what was asked |

> **`MUTPICK-BACK49` IS THE PREDECESSOR'S DEFECT RE-CREATED ON PURPOSE.** Its picker used back 49 —
> which is our Close — while its own settings screen used 48/49. The mutation kills **three** rows,
> including the one that asserts the two screens agree, so the shape cannot come back silently.

---

## *** A CORRECTION TO THE SLICE 9 TABLE BELOW: EVERY CORE-SCOPED KILL SET IN IT IS A LOWER BOUND ***

**Found 2026-09-17, while running slice 10's pass, and it falsifies numbers already merged.**

**A mutation upstream kills tests in its own module, which FAILS that module — and `rpg-paper`
DEPENDS ON `rpg-storage`, so Maven SKIPS it.** The runner then reported a kill set with an entire
module never executed, and **nothing in the output said so**: a truncated set and a complete one are
the same list of names.

```
[INFO] rpg-storage ........ FAILURE
[INFO] rpg-paper .......... SKIPPED      <- the kill set is a lower bound, and reads as complete
```

**`-fae` DOES NOT FIX IT.** `--fail-at-end` still skips modules whose **dependencies** failed, which
is exactly this case — measured both ways, same `SKIPPED`. The fix is
`-Dmaven.test.failure.ignore=true`, which makes surefire RECORD failures without failing the module,
so every module runs. **The build then exits 0 even when rows died, so the KILL COUNT rather than
the exit status is the result** — and the runner now prints a `module control` line that refuses to
let a `SKIPPED` pass unremarked.

**MEASURED MAGNITUDE, on one re-run rather than estimated:**

| mutation | recorded below | re-measured with every module running |
|---|---|---|
| `MUTLEVELFOR-STRICT` | **9** | **12** — three `NexusStatsLoreTest` rows it never reached |

**Every `-pl core` row in the slice 9 table is understated the same way**, because `paper` holds
tests that read `PlayerLevel` through the progression block. **The table is left as it was recorded
and this note sits above it**, rather than the numbers being edited: they are what that run
measured, and a reading is not improved by rewriting it — it is corrected by saying what was wrong
with the instrument.

> **AND THE SLICE 10 TABLE ABOVE IS CLEAN**, because it was measured after the fix; each of its rows
> carries `every module ran (no SKIPPED in the reactor)`.

---

## MUTATION EVIDENCE — SLICE 9, PLAYER LEVEL. RUN 2026-09-17

**Thirteen mutations, every one applied and measured, and NO KILL SET WAS PREDICTED.** The runner
refuses to report a test result until the substring check, both halves of the marker count, the line
delta against a pristine scratchpad copy, and a `test-compile` gate have all passed.

| mutation | file | rows killed | what it proves |
|---|---|---|---|
| `MUTXPPRIO` `LOWEST -> NORMAL` | `RpgListeners` | 1 | the hook's priority has a guard at all |
| `MUTPLUS-RAW` saturating add → `sum` | `PlayerLevel` | 3 | an overflow reads as level 1, and three rows see it |
| `MUTLEVELFOR-STRICT` `>=` → `>` | `PlayerLevel` | **9** | every boundary in the curve, across three test classes |
| `MUTREFUSE-XPONLY` scope the refusal to `xp` | `XpGrant` | 1 | **the predecessor's own defect, re-introduced** |
| `MUTADD-THRESHOLD` delta → new threshold | `XpGrant` | 3 | `add N levels` keeps partial progress |
| `MUTCLAMP-INT` saturating → `from + levels` | `XpGrant` | 1 | **the bug I actually shipped in the first draft** |
| `MUTMAX-ALWAYS` drop the `isMaxed` branch | `PlayerLevelLines` | 1 | the `(MAX)` marker |
| `MUTGATE-STRICT` `>=` → `>` | `NexusStationGate` | 1 | the gate opens AT the level, not after |
| `MUTAT-TRANSPOSE` grindstone slot → `ENCHANTING` | `NexusStationGate` | 1 | slot→station is a bijection |
| `MUTLORE-ONEFACT` drop the third lore line | `NexusStationGate` | 1 | the world-block sentence is required |
| `MUTBLOCK-APPEND` progression lines dropped | `NexusStatsLore` | 4 | the block's position AND its content |
| `MUTCAP-ALWAYS` render "To Next" at the cap | `NexusStatsLore` | 1 | the absent line at 99 |
| `MUTXP-REPLACE` accumulate → replace | `ProfileService` | 4 | a gain adds to the stored total |
| `MUTXP-ALWAYSSAVE` drop the level comparison | `ProfileService` | 1 | the persistence policy |
| `MUTLOCKED-SILENT` delete the refusal send | `NexusMenu` | 1 | **the icon/click coupling** |
| `MUTREFUSAL-ONEFACT` drop the world-block clause | `NexusStationGate` | 1 | the message carries both facts |

**Fifteen mutations. No mutation in this pass killed zero rows.**

> **`MUTLOCKED-SILENT` IS A DELETION, SO THE MARKER COUNT DOES NOT APPLY TO IT** — the replacement
> text is a SUBSTRING of what it replaces, which is CLAUDE.md's eighth row. Measured: `marker
> present: 6` and `markers left: 6`, where a working edit gives `1` and `0`. **Neither number is a
> bug; both count text that is legitimately still there.** The runner warned about the substring
> before applying, and **the line delta of `2` is the whole verification.**

> ### TWO INSTRUMENT FAILURES IN THIS PASS, AND THE RUNNER CAUGHT BOTH BY REFUSING TO REPORT
>
> **1. `MUTBLOCK-APPEND` silently did not apply.** A multi-line search pattern typed with `\n`
> **cannot match a CRLF file**, and `core.autocrlf=true` on this clone means the working tree is
> CRLF — measured, `GATE-nexus.md` carries one CR per line. The `perl` substitution exited 0 and
> changed nothing. **`line delta: 0` is what caught it**, exactly as the sixth and eighth rows of
> CLAUDE.md's table predict; the marker count could not have.
>
> **2. The `original gone` count was itself wrong, for a different reason.** It used `grep -F`,
> which is **LINE-BASED**, so a multi-line needle is read as several independent patterns and the
> number means nothing — it reported `2` for a pattern that had not been removed at all.
>
> **Both were fixed in the INSTRUMENT, not worked around in the pattern:** newlines in the pattern
> now compile to `\r?\n`, and both counts are taken with `perl -0777` so they can see across lines.
> **Then the instrument's own positive control was re-run** — a pattern that cannot match must be
> REFUSED — before any further result was believed.
>
> **The control also demonstrates which guard is load-bearing:** it reported `marker present: 45`,
> because the one-character replacement text occurs all over the file. **Only the line delta said
> no.**

---

## MUTATION EVIDENCE — WHICH ROW GUARDS WHAT, MEASURED RATHER THAN ASSUMED

Run 2026-09-15 against the unit suite. Both halves of the marker grep confirmed on each (marker
present, original gone), line deltas measured against scratchpad copies, and every file restored
byte-identical with `cmp` — never `git checkout --`, which **cannot restore `NexusLock.java` at all
because it is untracked**.

| mutation | rows killed | note |
|---|---|---|
| `MUTLOCKED` `LOCKED_SLOT 8 -> 7` | **0, then 1** | **APPLIED AND DID NOT BITE on the first run.** See below. |
| `MUTOFFHAND` `OFFHAND_SLOT 40 -> 39` | **0, then 1** | **THE SAME DEFECT, MISSED BY THE SWEEP THAT SHOULD HAVE FOLLOWED THE FIRST.** See below. |
| `MUTIGNORE` strip `ignoreCancelled` from `onMenuClick` | 1 | `NexusWiringSignatureTest` only; `NexusLockTest` stayed green, correctly |
| `MUTALWAYSFALSE` | 10 | every refusal row |
| `MUTALWAYSTRUE` | 8 | **every PERMIT row — the dead-menu guard** |
| `MUTAXISLOCKED` drop the `LOCKED_SLOT` arm | 2 | both about the locked slot |
| `MUTAXISSTAR` drop the `starAt` arm | 2 | both about a star elsewhere |
| `MUTDRAGCURSOR` drop `cursorIsStar` from `refusesDrag` | 1 | the drag path only |

**`MUTAXISLOCKED` and `MUTAXISSTAR` have DISJOINT kill sets**, which is the point of running both:
`touchesTheStar` has two independent arms and one mutation could not have certified the other.

> ### THE FIRST MUTATION FAILED, AND IT IS THE MOST USEFUL THING IN THIS FILE
>
> `LOCKED_SLOT 8 -> 7` was applied — marker present, original gone, 13-byte delta, 2 lines changed
> — and **all twenty rows stayed GREEN**.
>
> Every row named `NexusLock.LOCKED_SLOT` **symbolically**, so the mutation moved the code and the
> expectation together. That is CLAUDE.md's *applied, no bite*, and nothing mechanical catches it:
> both halves of the marker grep pass, the delta is right, the scope is right.
>
> **Worse, the constant's own javadoc asserted the opposite** — that the tests referring to it
> symbolically was *why* it was guarded. A symbolic reference cannot pin a value. Only a literal
> can. `theLockedSlotIsTheRIGHTMOSTHOTBARSLOT_theONLYRowThatPinsTheVALUE` was added, the mutation
> re-run, and it then reddened **exactly that one row**. The javadoc now says which row is the sole
> guard.
>
> **Every row in this gate is staged against 8.** If the locked slot is ever re-ruled, that test
> row goes red first and this file needs restaging.
>
> > ### THAT PREDICTION WAS RIGHT ABOUT THE OUTCOME AND WRONG ABOUT THE MECHANISM — CORRECTED 2026-09-16
> >
> > It anticipates a re-ruling **to a different constant**, where `assertEquals(8, LOCKED_SLOT)`
> > goes red. Slice 4a did something the sentence did not consider: it made the slot a
> > **per-player VARIABLE**. So the row did not go red — **`NexusLockTest` STOPPED COMPILING**,
> > 108 errors across 22 rows, because `refusesClick` grew an argument.
> >
> > **Louder, and fine.** But a file that predicts the wrong signal teaches the next reader to
> > watch for the wrong thing, and *"it went red"* is a sentence someone will look for and not
> > find. **The two failures are not interchangeable**: a red row is a staging problem you fix in
> > the gate, and a compile error is an API change you fix in the test before the gate is even
> > reachable.
> >
> > **The rows themselves are still correctly staged**, which is why 4a changed no row here:
> > `PlayerProfile.DEFAULT_NEXUS_SLOT` is **8**, so every player in an ungated boot still has
> > their star at 8 and every prediction below still describes what will happen. **The restaging
> > this warning promises is owed by 4b**, which is the slice that lets a player choose a
> > different one.

> ### AND THE SAME DEFECT WAS SHIPPED A SECOND TIME, IN THE SAME FILE, BY NOT SWEEPING FOR IT
>
> `OFFHAND_SLOT = 40` was in **exactly** the state `LOCKED_SLOT` had just been found in. Its only
> appearance in the whole test file named it symbolically **on both sides** — the touched set built
> `Touched(true, OFFHAND_SLOT)` and the predicate tested `index == NexusLock.OFFHAND_SLOT` — so the
> two moved together. Measured: `40 -> 39` applied, all 21 rows **green**.
>
> **And 39 is the HELMET slot** in the index space this class declares. The mutant lock watches a
> player's helmet instead of their offhand; every F-swap of the star out of the locked slot goes
> unrefused, and nothing reddens.
>
> **THE LESSON IS NOT "PIN YOUR CONSTANTS". IT IS THAT FINDING A SHAPE IS NOT SWEEPING FOR IT.**
> The first instance was found by mutation, written up, and fixed — and the write-up said *"the
> javadoc now says which row is the sole guard"*, a sentence scoped to one constant while a second
> instance of the identical defect sat two lines below it. It was caught in **review**, not by the
> work that had just identified the shape.
>
> **The rule, in the form that would have caught it: when you find a defect shape, enumerate where
> else it can live BEFORE writing the fix.** Here that enumeration is two lines long — `NexusLock`
> has exactly two constants.
>
> Swept afterwards, and recorded so the next reader need not redo it:
>
> | value | pinned by |
> |---|---|
> | `NexusLock.LOCKED_SLOT = 8` | `theLockedSlotIsTheRIGHTMOSTHOTBARSLOT_theONLYRowThatPinsTheVALUE` |
> | `NexusLock.OFFHAND_SLOT = 40` | `theOffhandIsSLOT40AndNOT39_theHELMET_theONLYRowThatPinsThatVALUE` |
> | `NexusItems` — `NETHER_STAR`, the `BYTE` tag, `setMaxStackSize(1)` | **nothing in the suite.** Row 7 of this file, and only that. A different gap: visibly untested rather than falsely guarded. |
> | `NexusSlots` — the conversion | **nothing in the suite.** Rows 4–5, and only those. |

> ### THE SUITE PRODUCED TWO HOLLOW CHECKS, NOT ONE — AND THE SECOND WAS FOUND IN REVIEW
>
> One is an accident; two in one file is a pattern worth naming.
>
> The first was `MUTLOCKED` passing against symbolic rows. The second was a row named
> *`theSameLockedSlotIsRefusedWhicheverViewItArrivedFrom`*, which asserted
> `refusesClick(X) == refusesClick(X)`: two `Touched` **records** built from identical arguments,
> compared through the same function. `Touched` is a record, so they were equal. **It could not fail
> for any implementation whatsoever.** Two comments — *"raw 44 converted"*, *"raw 89 converted"* —
> described a conversion the test never performed, because `NexusSlots` was never on the stack. Its
> mutation note named a mutation that **cannot be written**: `refusesClick` has no view-shaped
> parameter to corrupt.
>
> It read as coverage of the coordinate-space property — the property this entire slice exists
> around — and provided none.
>
> **The conversion cannot be unit-tested here, and that is measured rather than assumed.** Probed
> against the pinned `paper-api`: both `InventoryView.convertSlot(int)` and
> `InventoryView.getInventory(int)` are **ABSTRACT, not default** — *"abstract method convertSlot(int)
> in InventoryView cannot be accessed directly"*. The arithmetic lives in the server's
> `CraftInventoryView`, off the test classpath. A stub would have to implement `convertSlot` itself,
> and the test would then assert its own fake arithmetic: the same hollowness one layer down.
>
> The row was replaced with one that claims only what it can show — the single branch of
> `touchedOf` that returns before the view is consulted, which a plausible reordering would turn
> into an NPE inside an event handler. **Rows 4–5 below carry the conversion, alone.**

---

# SLICE 11, PR 1 — THE VAULT'S STORAGE LAYER. ROWS 92–96.

**Status: RUN 2026-09-17, booted by Ben. 2 PASS, 2 PARTIAL, 1 VOID** — 92 PASS, 93 PARTIAL, 94 PASS,
95 PARTIAL, 96 VOID. **Every row below was still written before that boot**, and before the branch
that adds them was pushed; **no prediction was edited afterwards.**

> **THE TWO ROWS THAT WERE THE ENTIRE EVIDENCE FOR THIS PR ARE THE TWO THAT ARE FULLY WITNESSED.**
> 92 and 94 are the sole witnesses, and 94's empty-vault control — the half most likely to be
> skipped — was run. The two PARTIALs are on rows with other coverage, and the VOID is on a row with
> unit coverage by construction. **That, and only that, is why the block shipped at two full rows of
> five.**
>
> **THIS BLOCK IS NOT 5/5 AND MUST NOT BE READ AS 5/5.** Two halves are still `NOT RUN` and are left
> that way in their own rows: row 93's `ls` of the vault directory, and row 95's `/rpg stats`
> control. **Neither was scheduled and neither blocked the merge.** A block recorded as finished when
> it was 2 full, 2 partial and 1 void is worth less than nothing, because the next person reads it as
> evidence.

**THIS PR SHIPS NO PLAYER-VISIBLE CHANGE AT ALL**, which is why these rows exist and why they are
staged through `/rpg vault`. The storage layer lands and is witnessed here; the seven-page screen,
the ender-chest hijack and the migration are **PR 2** and have no rows in this block.

> **THE TWO CLAIMS THIS BLOCK EXISTS FOR CANNOT BE UNIT-TESTED IN ANY MODULE.** Encoding an
> `ItemStack` routes through `Bukkit.getUnsafe()`, and `new ItemStack(...)` throws *"No RegistryAccess
> implementation found"* without a running server — **the project has no MockBukkit.** So the PDC
> round trip and the shutdown flush have no unit rows anywhere, by construction, and **rows 92 and 94
> are their only witnesses in the entire project.**
>
> The suite passes with either deleted. Measured: `1931` tests green (core 1052 / storage 57 /
> paper 822) with the vault never having held a real item.

## GAME MODE

**EVERY ROW IN THIS BLOCK IS `/gamemode survival` UNLESS ITS OWN HEADING SAYS OTHERWISE.**

> **AND CREATIVE WOULD HOLLOW OUT ROW 92 IN PARTICULAR.** Creative removes a cost, and a row whose
> reading is *"the item is still there"* is satisfied for free once the cost is gone. Row 92 asserts
> a specific enchanted, damaged instance survives a restart — in creative, an identically-named item
> is one click away and the reading would not distinguish a working codec from a convincing coincidence.

---

## ROW 92 — *** A MINTED WEAPON SURVIVES A RESTART WITH ITS PDC INTACT. SOLE WITNESS. ***

**CONDITIONS:** survival, operator (`rpg.command.dev`). A fresh `plugins/Rpg/vaults/` — delete it
first, so the absent-file path is exercised on the way in.

**STAGE:**

1. `/rpg give boltor` — a weapon with PDC, lore and an attribute modifier.
2. Enchant it so `enchant_data` and `enchant_rolled` are both non-trivial, and damage it so
   `Damageable.getDamage()` is non-zero: `/rpg enchant ...`, then `/rpg durability damage 7`.
3. **Write down what the tooltip says** — name, rarity colour, every lore line, the durability bar.
4. `/rpg vault store 3 17` — the item leaves your hand.
5. `/rpg vault dump` — confirm `page 3: 1 item(s)` and `slot 17: <MATERIAL> x1`.
6. `/stop`. Restart. Rejoin.
7. `/rpg vault dump`, then `/rpg vault take 3 17`.

**PREDICTED:** the item returns **byte-identical in every way the player can see**: the same display
name and rarity colour, the same lore lines in the same order, the same durability (damage 7, not a
fresh item), the same glint state, and its enchants still active. `/rpg enchant show` reads what it
read at step 2.

> **PAGE 3 SLOT 17, AND NEITHER NUMBER IS AN ACCIDENT.** Not page 1, not slot 0, and `3 != 17` — a
> row staged at `page 1 slot 1` cannot detect a transposition between the two coordinates, and one
> staged at page 1 passes whether the page index is read at all.

> **THE DURABILITY AND THE ENCHANT ARE THE DISCRIMINATING HALF, NOT THE MATERIAL.** A codec that
> stored only the material and amount would pass any reading phrased *"a Boltor came back"*. Wear and
> `enchant_data` are the fields a naive key-by-key projection drops, and they are invisible in a
> screenshot of the after state unless they were written down at step 3.

**READING:** **2026-09-17, booted by Ben. PASS. SOLE WITNESS, FULLY WITNESSED.** Boltor stored at
page 3 slot 17, `/stop`, restart, `/rpg vault take 3 17`. The item returned matching the step-3
note: name, rarity colour and lore lines the same, **the damage-7 durability intact and the enchants
still reading what they read before the restart** — both checked, not eyeballed. **The codec
preserves PDC across a real restart.**

---

## ROW 93 — AN ABSENT VAULT FILE IS AN EMPTY VAULT, NOT AN ERROR

**CONDITIONS:** survival, operator. **A player who has never opened a vault**, and a
`plugins/Rpg/vaults/` directory containing no file for them — verify with `ls` before joining.

**STAGE:** join, then `/rpg vault dump`.

**PREDICTED:** `Vault: 0 occupied slot(s), schema v1`. **No** error, **no** stack trace in the
console, and **no file is created by the read** — `ls plugins/Rpg/vaults/` still shows nothing for
that player.

> **THE "NO FILE IS CREATED" HALF IS THE ONE WITH TEETH.** A load that wrote an empty vault on first
> read would look identical in chat and would mean every player who has ever joined owns a file.
> Check the directory, not the message.

> **AND `0 occupied slot(s)` IS NOT THE SAME MESSAGE AS THE FAILURE ARM.** An unloaded or unreadable
> vault says *"No vault is loaded for you"* in RED. If that is what appears, the row has NOT passed —
> it has found the thing the two messages were deliberately separated to distinguish.

**READING:** **2026-09-17, booted by Ben. PARTIAL — DO NOT RECORD THIS AS A CLEAN PASS.** The
message half PASSES: `Vault: 0 occupied slot(s), schema v1`, no error, no stack trace. **THE
DIRECTORY WAS NOT CHECKED**, so the *"no file is created by the read"* half is **NOT RUN** — the
half this row's own note above calls the one with teeth. Row stands PARTIAL.

---

## ROW 94 — *** THE LAST WRITE SURVIVES A `/stop`. SOLE WITNESS. ***

**CONDITIONS:** survival, operator.

**STAGE:** `/rpg give ember_staff`, then `/rpg vault store 5 31`, and **immediately** `/stop` — within
a second, without disconnecting first. Restart, rejoin, `/rpg vault dump`.

**PREDICTED:** `page 5: 1 item(s)`. The staff is there.

> **WHAT THIS ROW ACTUALLY TESTS IS AN ORDERING, AND ONLY A REAL `/stop` EXERCISES IT.** Write-through
> means the write was already issued; the question is whether it had *drained* before `storageIo`
> was shut down. `onDisable` flushes the vaults and waits, **then** shuts the executor down. Reverse
> those two and the final write is queued onto a stopped executor: it never runs, the future never
> completes, and the shutdown looks perfectly clean.
>
> **Quitting first would NOT test this** — a quit gives the executor all the time it needs. The
> immediacy is the fixture.

> **A CONTROL, AND IT IS THE ROW:** repeat with the store **omitted** — `/stop` with an empty vault,
> restart, dump. It must read `0 occupied slot(s)` rather than erroring. A shutdown path that throws
> on an empty vault would look like a pass in the main reading, because nobody checks the console
> when the item is there.

**READING:** **2026-09-17, booted by Ben. PASS, BOTH ARMS. SOLE WITNESS, FULLY WITNESSED.** Ember
staff stored at page 5 slot 31, immediate `/stop`, restart, rejoin — `page 5: 1 item(s)`. **AND THE
CONTROL WAS RUN:** `/stop` on an empty vault, restart, dump reads `0 occupied slot(s)` with no
error. The flush-then-shutdown ordering holds in both directions.

---

## ROW 95 — `/rpg vault` IS REFUSED WITHOUT `rpg.command.dev`

**CONDITIONS:** survival. **A player who is NOT an operator** — `/deop <them>` first.

**STAGE:** as that player: `/rpg vault dump`, then `/rpg vault store 3 17` holding an item.

**PREDICTED:** **both are refused** — the subcommand does not appear in tab-completion and typing it
gives the unknown-command error. The item stays in their hand.

> **THE CONTROL IS NOT OPTIONAL AND IT IS `/rpg stats`.** Run as an operator this row passes on a
> build with no gate at all. The same player must be able to run `/rpg stats`, which is
> `default: true` — that is what says they are a real unprivileged player rather than someone the
> server is refusing everything.

> **UNGATED, THESE THREE ARE AN ITEM DUPLICATOR**: `store` moves an item out of the world into a file
> and `take` puts one back. That is why this row is in the block rather than left to the signature
> test, which can only see the source.

**READING:** **2026-09-17, booted by Ben. PARTIAL.** The refusal PASSES: as a deopped player,
`/rpg vault dump` and `/rpg vault store 3 17` were both refused and the item stayed in hand. **THE
`/rpg stats` CONTROL WAS NOT RUN**, so this reading does not yet distinguish the gate working from
the server refusing that player everything. Row stands PARTIAL.

---

## ROW 96 — TWO PLAYERS DO NOT SHARE A VAULT

**CONDITIONS:** survival, two operator accounts online at once.

**STAGE:** player A: `/rpg give boltor`, `/rpg vault store 2 24`. Player B: `/rpg vault dump`, then
`/rpg vault store 2 24` with a **different** item (`/rpg give emberblade`). Both `/rpg vault dump`.

**PREDICTED:** B's first dump reads `0 occupied slot(s)` — **A's item is not in it.** After both
stores, A's dump shows the Boltor at page 2 slot 24 and B's shows the emberblade at the same
coordinates. `ls plugins/Rpg/vaults/` shows **two** files, named for the two UUIDs.

> **THE SAME CELL FOR BOTH IS THE POINT.** Staging them at different pages would pass against a
> single shared vault keyed by nothing, because the two items would not collide. Identical
> coordinates is what makes a shared-state bug produce a visible wrong answer rather than a
> coincidentally correct one.

**READING:** **2026-09-17. VOID — could not be staged, no second account online.** **NOT a pass,
NOT deferred.**

> **WHY A VOID HERE WAS SURVIVABLE, AND IT IS WHY THIS ROW WAS NEVER MARKED A SOLE WITNESS.** The
> per-UUID keying is not left unwitnessed by this: `FileVaultRepositoryTest` writes `<uuid>.json`
> and `VaultServiceTest` keys its map by UUID. That is what makes a VOID here survivable, and it is
> the whole difference between this row and 92 or 94, whose claims no module can construct at all.
