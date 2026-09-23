# GATE — The anvil screen (Slices 13a and 13b)

**Status: READ — 32 of 32 PASS.** Booted by Ben on `feat/13b-anvil-confirm`, recorded 2026-09-22.
Every prediction below was written BEFORE the boot, and **no prediction was edited once a row had
been read.** Readings sit in the `READ` cell beside the prediction they answer, never over it.

**FIGURES ARE NOT CAPTURED AND THE VERDICT IS THE READING** — Ben's ruling, 2026-09-20. It is cited
rather than restated; the entry is in `GATE-gearscore.md`'s status section.

```
PASS, 13a   22   R0 R1 R2 R3 R4 R5 R6 R7 R8 R9 R10 R11 R12 R12b R13 R14 R15 R16 R17 R18 R19 R20
PASS, 13b   10   R21 R22 R23 R24 R25 R26 R27 R28 R29 R30
            ──
            32   = git grep -c '^### R[0-9]' <ref> -- GATE-anvil.md
```

**Both figures are re-derived from the cells, not adjusted from the old block:** `grep -c` for the
`READ` cell marker returns **32**, every one of those cells is byte-identical to the others
(`grep ... | sort -u | wc -l` is **1**), and the unread placeholder survives in **no** `READ` cell.
So every row carries a reading and none was missed. The `32` on the tally line is the file's own
count command, unchanged.

> **THE PLACEHOLDER COUNT IS DESCRIBED RATHER THAN QUOTED, AND THAT IS DELIBERATE.** The first draft
> of this paragraph asserted the literal `not-run` marker was gone **while containing that marker**,
> so the count it claimed was `0` and the true count was `1` — **the sentence falsified itself by
> being written.** Same defect as `CLAUDE.md`'s test-count debt entry, which is why that one gives a
> command and no live figure. **Run the grep; do not read a number off this page.**

> ### *** THE BINDING IS A RULING, NOT A PROBE READING, AND THIS IS THE WEAKEST LINK IN THE FILE ***
>
> **R0 DID NOT REPORT A TREE.** Its fourth prediction asks the boot to state which tree the jar was
> built from, and that field was not filled.
>
> **The readings are bound to `5f429a9` (tree `ba7aab07`) by Ben's statement that he booted this
> branch — not by the probe.** Ben ruled the binding; it is recorded here as a ruling with an
> author, which is what makes it different from an assumption nobody made.
>
> **THE ONLY CORROBORATION IS CIRCUMSTANTIAL, AND IT IS NAMED SO NOBODY LATER MISTAKES IT FOR THE
> PROBE:** the working tree was checked out to `feat/13b-anvil-confirm` at **22:09:35** and back to
> `master` at 22:24:14 (`git reflog`), and `run/plugins/rpg-0.1.0-SNAPSHOT.jar` was written at
> **22:09**, the same size as `paper/target/`'s. **That is consistent with the boot and proves
> nothing about it** — a jar in the right place at the right time is exactly what a wrong-worktree
> build produced on 2026-09-20.
>
> **THE COST, STATED PLAINLY: R0 IS THE SOLE WITNESS FOR EVERY OTHER ROW IN THIS FILE, AND ITS
> BINDING IS THE ONE LINK THAT WAS NOT MEASURED.** Thirty-one rows rest on a jar identification
> that rests on a statement. If the jar was not built from `5f429a9`, every PASS above is a true
> reading of the wrong subject — which is the failure mode R0 exists to close and, here, did not.
>
> **AND THE BOOT DATE WAS NOT CAPTURED.** `2026-09-22` is the date the readings were RECORDED. The
> date of the boot itself is not in the record and cannot now be recovered from it.
>
> **Ruled by Ben. Do not re-litigate it and do not soften it.** It is written down at this strength
> so that a later reader weighing this file knows exactly which plank is load-bearing.

> ***THE 13b ROWS ARE THE DESTRUCTIVE ONES, AND THEY ARE WHAT THE SPLIT BOUGHT.*** Every 13a row
> could be read with nothing at stake. From R21 down, **a wrong reading costs the player an item and
> some XP with no undo and no output slot to take anything back from.** Read R0 first, and read it
> against a 13b symbol — the probe below has moved.

**R12b is a SOLE WITNESS and is marked as one in its own row.** It is the only end-to-end reading of
the armour split anywhere — the composition it covers has no unit guard and cannot have one, because
no test in this project can construct an `ItemStack`. **If one row in this file is going to be run,
it is R0; if two, the second is R12b.**

**GAME MODE: SURVIVAL, for every row unless the row says otherwise.**

> **CREATIVE IS NOT A SUBSTITUTE HERE, AND THIS SCREEN IS A PARTICULARLY BAD PLACE TO TRY.**
> `CLAUDE.md`'s creative-divergence register records that the own-inventory screen **decomposes a
> gesture into independent single-slot writes** (`InventoryCreativeEvent`, one slot and its new
> item), so a slot guard is asked about half a gesture. **This is a TWO-INPUT screen**, and every
> row below about moving an item between them is reading a different mechanism in creative.

> **13a IS DISPLAY AND REFUSAL ONLY.** There is no confirm button, no XP is spent and nothing is
> consumed. **A row asking whether a transfer HAPPENED does not belong in this file** — that is 13b's
> gate, and it is separate on purpose so the one irreversible operation in the feature gets an
> undivided reading.

---

### R0 — The deployed build carries this slice

**THE SOLE WITNESS FOR EVERY OTHER ROW IN THIS FILE.** A wrong jar does not announce itself: it
produces readings, in the right shape, at plausible values. Slice 12b lost hours to a jar built from
the wrong worktree whose mtime was current and which passed every staleness check.

> **WRITTEN IN POWERSHELL, BECAUSE THAT IS THE SHELL THIS ROW IS RUN IN.** There is no `unzip` on
> the operator's machine, and `java` on `PATH` resolves to Oracle's JRE shim
> (`Common Files\Oracle\Java\javapath`), which ships **no `javap`**. **A bash R0 row is a row that
> does not get run.**

| | |
|---|---|
| **Setup** | <pre>Copy-Item run/plugins/rpg-0.1.0-SNAPSHOT.jar "$env:TEMP\deployed.zip" -Force<br>Expand-Archive "$env:TEMP\deployed.zip" -DestinationPath "$env:TEMP\deployed" -Force<br>$classes = Get-ChildItem "$env:TEMP\deployed\io\github\butterflysmp\rpg" -Recurse -Filter *.class<br>Write-Host "$($classes.Count) class files scanned"<br>$classes \| Select-String -Pattern CANNOT_AFFORD -Encoding ascii \| Select-Object -ExpandProperty Path</pre> |
| **Predict** | **THE SYMBOL MOVED WITH THE SLICE.** It was `ARMOR_HEAD` through 13a; a 13a jar carries that and none of 13b, so probing it now would clear a build with no confirm button in it. `CANNOT_AFFORD` exists only from 13b, and the hit should name `AnvilFace$State.class`. |
| **Predict** | **The class count is non-zero**, and it is RECORDED in the reading. **`0 scanned` means the unpack failed, and an absence underneath a zero means nothing at all.** |
| **Predict** | At least one path is printed, and it includes `TransferKey.class`. A **zero-match** result means the deployed jar predates this slice, whatever its mtime says. |
| **Predict** | **STATE WHICH TREE THE JAR WAS BUILT FROM.** A worktree checkout makes "the repo" ambiguous, and that ambiguity is what cost slice 12b a boot. |
| **READ** | **PASS** -- booted by Ben, recorded 2026-09-22. Figures not captured (Ben's ruling, 2026-09-20; see `GATE-gearscore.md`). |

> `-Encoding ascii` is load-bearing: a `.class` file is binary, and a text-mode read will not find a
> constant-pool string it is perfectly capable of matching.

**IF R0 FAILS, STOP. No other row in this file is readable.**

---

## THE HUB STATION

### R1 — The anvil appears at slot 30 and the station row is contiguous

| | |
|---|---|
| **Setup** | Open the Nexus star at any level. Look at row 4. |
| **Predict** | Five stations, **left to right with no gap**: ender chest, anvil, crafting table, enchanting table, grindstone. The anvil is an **ANVIL** item, third from the left of the run. |
| **Predict** | **No black filler pane anywhere inside 29–33.** The hole at 30 that `VAULT_SLOT`'s javadoc held open is closed. |
| **READ** | **PASS** -- booted by Ben, recorded 2026-09-22. Figures not captured (Ben's ruling, 2026-09-20; see `GATE-gearscore.md`). |

### R2 — Locked at level 6, with all three lore lines

| | |
|---|---|
| **Setup** | `/rpg playerxp set <you> 6 levels`, open the hub, **hover** the anvil. |
| **Predict** | The name reads **Anvil**, dimmed. Three lore lines, exactly:<br>`Locked -- unlocks at level 7`<br>`You are level 6.`<br>`An anvil in the world still works.` |
| **Predict** | **The icon is still an ANVIL, not a barrier** — `NexusMenu.station` dims the name and keeps the material, which is precisely why R3's message is mandatory. |
| **READ** | **PASS** -- booted by Ben, recorded 2026-09-22. Figures not captured (Ben's ruling, 2026-09-20; see `GATE-gearscore.md`). |

### R3 — A locked click SPEAKS, and says both facts

| | |
|---|---|
| **Setup** | At level 6, **click** the anvil cell. |
| **Predict** | Nothing opens, and chat says exactly:<br>`Anvil unlocks at level 7. You are level 6. An anvil in the world still works.` |
| **Predict** | **Capital "A" on the third sentence.** A draft lower-cased the equivalent line for the grindstone and produced a sentence opening in lower case mid-message. |
| **READ** | **PASS** -- booted by Ben, recorded 2026-09-22. Figures not captured (Ben's ruling, 2026-09-20; see `GATE-gearscore.md`). |

### R4 — Opens at EXACTLY 7, and the boundary is read on both sides

**THE OFF-BY-ONE IS INVISIBLE IN PLAY**, which is why both sides are staged rather than just the
open one. `set N levels` lands on the threshold with zero progress, so "level 7" here means exactly
**7,500** lifetime XP — the tightest staging available.

| | |
|---|---|
| **Setup** | `/rpg playerxp set <you> 6 levels`, click. Then `set 7 levels`, re-open, click. |
| **Predict** | At **6** the click refuses with R3's sentence. At **7** the anvil screen opens. |
| **READ** | **PASS** -- booted by Ben, recorded 2026-09-22. Figures not captured (Ben's ruling, 2026-09-20; see `GATE-gearscore.md`). |

### R5 — Back from the hub returns to the hub

| | |
|---|---|
| **Setup** | At level 7+, open the anvil **from the hub**. Look at 48, then click it. |
| **Predict** | Slot 48 is an **ARROW** reading "Back to the Nexus". Clicking it returns to the hub. |
| **Predict** | The bottom row's coloured panes are **SEVEN** cells — 45, 46, 47, 50, 51, 52, 53 — with 48 and 49 as buttons. |
| **READ** | **PASS** -- booted by Ben, recorded 2026-09-22. Figures not captured (Ben's ruling, 2026-09-20; see `GATE-gearscore.md`). |

---

## THE WORLD BLOCK

### R6 — All THREE anvil materials open our screen

**TWO OF THE THREE ARE THE TRAP.** An anvil degrades as it is used, so chipped and damaged are what a
lived-in world mostly contains. A missing entry opens **vanilla** and presents as a bug that
"sometimes" happens.

| | |
|---|---|
| **Setup** | At level **1** (below the hub gate), place and right-click each of: an **anvil**, a **chipped anvil**, a **damaged anvil**. |
| **Predict** | All three open OUR screen. **The vanilla anvil screen never appears** — no two-slot-plus-rename bar, no "Repair & Name" text field. |
| **Predict** | Level 1 is deliberate: **the world block is not gated**, only the hub route is. |
| **READ** | **PASS** -- booted by Ben, recorded 2026-09-22. Figures not captured (Ben's ruling, 2026-09-20; see `GATE-gearscore.md`). |

### R7 — Vanilla rename and vanilla repair are GONE, with no fallthrough

| | |
|---|---|
| **Setup** | Right-click an anvil holding a damaged vanilla iron sword, and again holding a name tag. |
| **Predict** | Our screen opens both times. **There is no way to rename anything and no way to repair anything.** Ben's ruling; this is the expected reading, not a defect. |
| **Predict** | `NEXT.md` carries the parked entry for repair. **If this row surprises you, read that entry before filing anything.** |
| **READ** | **PASS** -- booted by Ben, recorded 2026-09-22. Figures not captured (Ben's ruling, 2026-09-20; see `GATE-gearscore.md`). |

### R8 — From a block there is NO Back button, and 48 is readout

| | |
|---|---|
| **Setup** | Open from a world anvil. Look at 48. |
| **Predict** | **EIGHT** coloured panes in the bottom row — 45, 46, 47, **48**, 50, 51, 52, 53 — and Close alone at 49. **No arrow, and no black filler pane sitting in the middle of a coloured row.** |
| **Predict** | Clicking 48 does **nothing**. |
| **READ** | **PASS** -- booted by Ben, recorded 2026-09-22. Figures not captured (Ben's ruling, 2026-09-20; see `GATE-gearscore.md`). |

---

## THE FOUR FACES

Each row reads **the bar colour**, **the preview cell's icon**, and **the sentence**. All three come
from one verdict, so a row where they disagree is a finding regardless of which looks right.

### R9 — Empty: GRAY, and an instruction rather than a complaint

| | |
|---|---|
| **Setup** | Open the screen and touch nothing. |
| **Predict** | Bar **GRAY**. Slot 24 is a **BARRIER** named `Place the item to upgrade, and one to sacrifice.` in **gray**. |
| **Predict** | **It is not an error face.** This is what the screen looks like the moment it opens, and it should read as an instruction to a player who has never seen it. |
| **READ** | **PASS** -- booted by Ben, recorded 2026-09-22. Figures not captured (Ben's ruling, 2026-09-20; see `GATE-gearscore.md`). |

### R10 — One item in: still GRAY, still the empty face

| | |
|---|---|
| **Setup** | Put a weapon in slot 20. Leave 22 empty. Then move it to 22 and leave 20 empty. |
| **Predict** | Bar stays **GRAY** and the sentence is unchanged, **both ways round**. |
| **READ** | **PASS** -- booted by Ben, recorded 2026-09-22. Figures not captured (Ben's ruling, 2026-09-20; see `GATE-gearscore.md`). |

### R11 — A tool: RED, and NOT the mismatch sentence

| | |
|---|---|
| **Setup** | `/rpg give` a tool into both slots. |
| **Predict** | Bar **RED**. Sentence: `Both items must be gear that carries a score.` |
| **Predict** | **It must NOT say "Both items must be …" naming a kind.** Two pickaxes ARE the same kind; telling the player so would be true and useless. This is the distinction `AnvilTransferTest` pins, read here on the real screen. |
| **READ** | **PASS** -- booted by Ben, recorded 2026-09-22. Figures not captured (Ben's ruling, 2026-09-20; see `GATE-gearscore.md`). |

### R12 — Boots into a helmet: RED, and the sentence names HELMETS

**THE HALF `GearClass` CANNOT EXPRESS.** Both items are `GearClass.ARMOR`; only the transfer key
distinguishes them.

| | |
|---|---|
| **Setup** | A **helmet** in slot 20, a **pair of boots** in slot 22. |
| **Predict** | Bar **RED**. Sentence: `Both items must be helmets.` — the **TARGET's** kind, plural. |
| **Predict** | Swap them: sentence becomes `Both items must be boots.` |
| **READ** | **PASS** -- booted by Ben, recorded 2026-09-22. Figures not captured (Ben's ruling, 2026-09-20; see `GATE-gearscore.md`). |

### R12b — SOLE WITNESS: a real helmet derives ARMOR_HEAD, and that key drives the refusal

**THIS ROW EXISTS BECAUSE A MUTATION KILLED FEWER TESTS THAN PREDICTED, AND THE GAP IT EXPOSED IS
WIDER THAN THE ONE GUARD.**

`MUT13A-KEYARMOR` collapses all four arms of `TransferKey.ofArmor` to `ARMOR_HEAD`. It was predicted
to redden two rows and reddened **one** — `TransferKeyTest.ARMOURSplitsFOURWays`. The other,
`AnvilTransferTest.BOOTSCannotFeedAHELMET`, stages its two sides as `TransferKey` constants
**directly** and never calls `of()`, so it tests the COMPARISON and is blind to the DERIVATION.

> **THE COMPOSITION HAS NO UNIT GUARD ANYWHERE, AND CANNOT HAVE ONE.** The two halves are each
> covered — the derivation by `TransferKeyTest`, the comparison by `AnvilTransferTest` — and
> **nothing tests them joined**: that a REAL helmet, read off a REAL `ItemStack`, resolves to
> `ARMOR_HEAD`, and that THAT key is what refuses. **No unit test can construct an `ItemStack`**
> (`new ItemStack(...)` throws without a `RegistryAccess`, and there is no MockBukkit), so this is
> not an omission that a better test would close. **This row is the only end-to-end witness that
> exists.**

**R12 IS NOT A SUBSTITUTE, AND THE DIFFERENCE IS THE DONOR'S SCORE.** R12 leaves the two scores
unstaged, so it cannot say WHICH rule refused — with a lower-scored donor the pair would be refused
by `DonorNotHigher` whether or not the key rule exists. **Staging the donor STRICTLY HIGHER removes
every other reason to refuse**, so the only thing standing between these two items and a completed
transfer is the armour split.

| | |
|---|---|
| **Setup** | A **helmet** in slot 20 and a **pair of boots** in slot 22. **Both must be OURS and both must carry a score** — check each tooltip reads a `Gear Score:` line before starting; an unstamped item reads 100 and is still scored, but an item that is none of ours is `Unscoreable` and refuses for a different reason entirely. |
| **Setup** | **Stamp the BOOTS strictly higher than the helmet**, and not by one — e.g. helmet **140**, boots **310**. Two numbers that cannot be confused for each other, and far enough apart that a misread is obvious. |
| **Predict** | The bar is **RED** and slot 24 reads `Both items must be helmets.` |
| **Predict** | ***IT MUST NOT BE LIME.*** A LIME face here means the four armour keys have collapsed and **a pair of boots is about to hand its roll to a helmet** — which is the half of Ben's rule `GearClass` cannot express, arriving in the one place nothing else can see it. |
| **Predict** | **It must not read `The sacrifice must score above 140.`** either. That sentence means the key rule never ran and the refusal came from the score comparison instead — the pair would then be refused today and legal the moment somebody raised the helmet's score. |
| **READ** | **PASS** -- booted by Ben, recorded 2026-09-22. Figures not captured (Ben's ruling, 2026-09-20; see `GATE-gearscore.md`). |

> **WHAT TO DO IF THIS ROW FAILS:** do not look at the screen code. The suspects are
> `TransferKey.ofArmor` (four arms collapsed), `AnvilMenu.armorSlotOf` (returning the wrong slot or
> `null`), and `GearItems.gearClassOf` (armour not resolving to `ARMOR`). The unit rows for each are
> green, so a failure here is in the JOIN.

### R13 — Equal scores: RED, and it names both numbers

| | |
|---|---|
| **Setup** | Two items of the same kind at the **same** gear score. Use `/rpg` to stamp them equal if needed. |
| **Predict** | Bar **RED**. Sentence: `The sacrifice must score above <N>. This one is <N>.` with the same N twice. |
| **Predict** | **EQUAL IS REFUSED, not accepted as a harmless no-op.** Ben's ruling: accepting it would eat the donor in 13b and change nothing. |
| **READ** | **PASS** -- booted by Ben, recorded 2026-09-22. Figures not captured (Ben's ruling, 2026-09-20; see `GATE-gearscore.md`). |

### R14 — A lower donor: RED, and the numbers differ

| | |
|---|---|
| **Setup** | Target at a **higher** score than the donor. |
| **Predict** | Sentence names the target's score first and the donor's second, and **they differ** — so the player can see how far short it is without hovering two tooltips. |
| **READ** | **PASS** -- booted by Ben, recorded 2026-09-22. Figures not captured (Ben's ruling, 2026-09-20; see `GATE-gearscore.md`). |

### R15 — A legal pair: LIME, the item, the new score and the cost

| | |
|---|---|
| **Setup** | Two items of the same kind, donor **strictly higher**. |
| **Predict** | Bar **LIME**. Slot 24 shows **the target item itself** — same name, same material — carrying **the donor's** gear score on its tooltip. |
| **Predict** | Lore ends with `Sacrifice to raise this to <donor score>.`, then `Costs <N> XP.`, then `Preview only -- you cannot take this.`, then `Confirm upgrades the item on the LEFT.` |
| **Predict** | **The cost is in XP POINTS and the LEVEL figure appears nowhere.** A RARE target reads **910**, an EPIC **2920**. |
| **Predict** | **THE LAST LINE NAMES THE DESTINATION, AND THAT IS THE POINT OF READING IT.** This screen mutates the target IN PLACE, so the result appears in slot **20** — the cell the player loaded — and **not** in slot 24, the cell they were watching. A player who presses confirm and looks at 24 sees the preview redraw as GRAY and their upgraded item nowhere in sight unless they know to look left. |
| **READ** | **PASS** -- booted by Ben, recorded 2026-09-22. Figures not captured (Ben's ruling, 2026-09-20; see `GATE-gearscore.md`). |

> ***THIS PREDICTION WAS EDITED ON 2026-09-21, BEFORE ANY BOOT, AND THAT IS THE ONLY CONDITION
> UNDER WHICH IT MAY BE.*** The rule is that a prediction is not edited **once a row has been
> read**; R15's `READ` cell is empty, so nothing is being retro-fitted to a result. The edit added
> the fourth lore line and the destination paragraph. **Recorded here rather than left silent,
> because an edited prediction with no note is indistinguishable from one written after the fact.**

### R16 — The cost keys on the TARGET's rarity

| | |
|---|---|
| **Setup** | A **COMMON** target and an **EXOTIC** donor of the same kind, donor higher. Then reverse the rarities. |
| **Predict** | COMMON target: **160 XP**. EXOTIC target: **24045 XP**. The two are unmistakable for one another. |
| **Predict** | **Upgrading a cheap item stays cheap however precious the thing you feed it.** |
| **READ** | **PASS** -- booted by Ben, recorded 2026-09-22. Figures not captured (Ben's ruling, 2026-09-20; see `GATE-gearscore.md`). |

---

## THE PREVIEW IS NEVER CARGO

**EVERY GESTURE, BECAUSE ONE UNGUARDED ROUTE IS A DUPLICATION EXPLOIT.** Count the items before and
after each: a gesture that DUPLICATES and a gesture that STEALS look identical in a screenshot of the
after state, and every row below is written against loss while a duplicate satisfies all of them.

### R17 — Slot 24 cannot be taken by any gesture

| | |
|---|---|
| **Setup** | With a LIME preview showing, try on slot 24: **left-click**, **right-click**, **shift-click**, **double-click**, a **number key**, and **F**. |
| **Predict** | **Nothing leaves slot 24 on any of the six.** Nothing reaches the cursor, the hotbar or the offhand. The player's item count is unchanged after all six. |
| **Predict** | The two input items stay exactly where they are. |
| **READ** | **PASS** -- booted by Ben, recorded 2026-09-22. Figures not captured (Ben's ruling, 2026-09-20; see `GATE-gearscore.md`). |

### R18 — Closing returns BOTH items and nothing else

| | |
|---|---|
| **Setup** | Both slots full, LIME preview showing. Close with **Esc**, then repeat and close with the **49 button**, then repeat and close with **Back** from the hub. |
| **Predict** | **Exactly two items** come back, all three times. **No third item**, and no copy of the preview. |
| **Predict** | Identical items — same score, same enchants, same durability. The preview never wrote to either of them. |
| **READ** | **PASS** -- booted by Ben, recorded 2026-09-22. Figures not captured (Ben's ruling, 2026-09-20; see `GATE-gearscore.md`). |

### R19 — The screen survives a disconnect and a shutdown with the items intact

| | |
|---|---|
| **Setup** | Both slots full: (i) disconnect and rejoin; (ii) with the screen open, `stop` the server and restart. |
| **Predict** | Both items are in the player's inventory afterwards, both times. **Two items, not three.** |
| **READ** | **PASS** -- booted by Ben, recorded 2026-09-22. Figures not captured (Ben's ruling, 2026-09-20; see `GATE-gearscore.md`). |

---

### R20 — TWO BARRIERS ON ONE SCREEN, AND THEY ARE TOLD APART BY LORE

**The cost of choosing a barrier for the refusal, named rather than discovered.** `MenuIcons.close()`
is also a BARRIER, so a refused screen carries two.

| | |
|---|---|
| **Setup** | With a RED face showing, hover slot **49**, then slot **24**. One screenshot reads both. |
| **Predict** | **49 is a barrier with NO LORE AT ALL** — its whole content is its name. **24 is a barrier that is ALL LORE** — the instruction as its name, plus `Nothing is spent until you confirm.` |
| **Predict** | **Neither reads "Not implemented yet."** That string belongs to `MenuIcons.placeholder`, which means NOT BUILT; a refusal is a working readout and this project has shipped a working readout in placeholder clothes once already. |
| **READ** | **PASS** -- booted by Ben, recorded 2026-09-22. Figures not captured (Ben's ruling, 2026-09-20; see `GATE-gearscore.md`). |

---

# SLICE 13b — THE DESTRUCTIVE ROWS

**Nothing below can be read twice from one staging.** Every row that confirms consumes an item and
spends XP, so each needs its own `/rpg give` and its own `/xp query` before and after.

> ***COUNT THE ITEMS AND THE XP BEFORE AND AFTER EVERY ROW.*** A gesture that DUPLICATES and a
> gesture that STEALS look identical in a screenshot of the after state, and **every row here is
> written against loss while a duplicate satisfies all of them.** The vault's ROW 103 is the
> precedent: *"the number to write down is ONE, and the failure is TWO."*

### R21 — The confirm cell exists, is armed, and counts down

| | |
|---|---|
| **Setup** | Open the screen from the hub. Put a legal, affordable pair in 20 and 22. Watch slot **31** and the bottom row. |
| **Predict** | 31 holds a dye reading **`Arming... 3`**, then `2`, then `1`, then **`Raise to <N> -- <C> XP`**. The bar is **YELLOW** while it counts and **LIME** when it lands. |
| **Predict** | **The bar and the button move together, always.** They are two lookups on one state; a screenshot showing a lime bar under a yellow button is a defect regardless of which looks right. |
| **READ** | **PASS** -- booted by Ben, recorded 2026-09-22. Figures not captured (Ben's ruling, 2026-09-20; see `GATE-gearscore.md`). |

### R22 — A click during the lockout does NOTHING and SAYS nothing

| | |
|---|---|
| **Setup** | `/xp query` first. Place the pair, then click 31 **five times** while the bar is yellow. `/xp query` again. |
| **Predict** | Nothing is consumed, nothing is stamped, **the XP figure is identical**, and **no chat line appears at all** — not even a refusal. The countdown is the feedback. |
| **Predict** | **If clicking during yellow transfers anything, the arming delay does not exist** — it is a decoration over a live button, which is worse than no delay because the player trusts it. |
| **READ** | **PASS** -- booted by Ben, recorded 2026-09-22. Figures not captured (Ben's ruling, 2026-09-20; see `GATE-gearscore.md`). |

### R23 — *** THE DONOR IS CONSUMED EXACTLY ONCE. SOLE WITNESS. ***

| | |
|---|---|
| **Setup** | **One** target and **one** donor in the world, both ours, both scored, donor higher. `/xp query`. Wait for LIME. Click 31 **once**. |
| **Predict** | Slot 22 is **EMPTY**. Slot 20 holds **exactly one** item. **Your inventory gains nothing.** The XP figure drops by exactly the number the button displayed. |
| **Predict** | Now click 31 **four more times, fast**. **Still nothing** — the donor slot is empty so the face is GRAY, and a GRAY click is silent. The XP figure does not move again. |
| **Predict** | ***THE NUMBER TO WRITE DOWN IS ONE DONOR CONSUMED AND ONE CHARGE MADE.*** A build that consumed twice, or charged twice, passes every other row in this block. |
| **READ** | **PASS** -- booted by Ben, recorded 2026-09-22. Figures not captured (Ben's ruling, 2026-09-20; see `GATE-gearscore.md`). |

### R24 — The target keeps everything except its score

| | |
|---|---|
| **Setup** | Use a target that is **enchanted**, **damaged** (swing it at something first) and carries its normal name. Note all three. Transfer onto it. |
| **Predict** | Same name, same enchant lines, **same durability bar**, and the `Gear Score:` line is now the donor's number. |
| **Predict** | **Losing durability here would be a relog-to-repair exploit and losing the enchants a relog-to-unlock one** — both are carried by `carryInstanceData`, and this row is what reads that the in-place write did not bypass it. |
| **READ** | **PASS** -- booted by Ben, recorded 2026-09-22. Figures not captured (Ben's ruling, 2026-09-20; see `GATE-gearscore.md`). |

### R25 — The XP deducted equals the figure the button displayed

| | |
|---|---|
| **Setup** | `/xp query`. Note the button's number. Confirm. `/xp query`. |
| **Predict** | `before − after` equals the button's number **exactly**. Not approximately, and not off by one. |
| **Predict** | **Check at a part-full bar, not on a round level.** The wallet is read and written in points through the curve's exact inverse; a build using `setLevel(getLevel() − n)` loses the fraction and is invisible at a full bar. |
| **READ** | **PASS** -- booted by Ben, recorded 2026-09-22. Figures not captured (Ben's ruling, 2026-09-20; see `GATE-gearscore.md`). |

### R26 — *** CONFIRM AFTER SWAPPING AN INPUT MID-ARM. THE ROW THIS SLICE EXISTS FOR. ***

| | |
|---|---|
| **Setup** | Get to LIME. **Swap the donor for a different item** and click 31 **immediately**. |
| **Predict** | **Nothing happens, NO MESSAGE appears, and the countdown is visibly back at 3.** The restarting countdown IS the feedback; this row reads it. |
| **Predict** | Wait for LIME again and click: **now** it transfers, using the NEW donor's score. |
| **Predict** | **A build that transferred on the first click has used the score of an item that is no longer in the slot.** Silent, plausible and permanent. |
| **READ** | **PASS** -- booted by Ben, recorded 2026-09-22. Figures not captured (Ben's ruling, 2026-09-20; see `GATE-gearscore.md`). |

### R27 — Spend XP elsewhere while the face is LIME, then confirm

**THE OTHER HALF OF THE WALLET, AND IT MUST NOT BE MERGED WITH R26.** One reads a re-arm; this reads
a spoken refusal. They are different mechanisms and neither can pass for the other.

| | |
|---|---|
| **Setup** | Get to LIME with a wallet **barely** above the cost. **Without touching either slot**, spend XP elsewhere — a second player, a command block, or `/xp set` from another account. Click 31. |
| **Predict** | Refused **WITH a chat line**: `This transfer costs <C> XP; you have <W>.` Nothing consumed, nothing stamped. |
| **Predict** | **THE WINDOW IS HALF A SECOND AND THAT IS NOT A DEFECT.** The repaint re-decides every ten ticks, so after that the face is already RED and the click is silent. **If the row is hard to stage, say so and rely on `AnvilReconcileTest` — the unit rows are the primary witness here and this boot row is best-effort by construction.** |
| **READ** | **PASS** -- booted by Ben, recorded 2026-09-22. Figures not captured (Ben's ruling, 2026-09-20; see `GATE-gearscore.md`). |

### R28 — Gain XP while the face is RED: it must go YELLOW, never straight to LIME

| | |
|---|---|
| **Setup** | Stage a legal pair you **cannot afford**. The bar is RED and 31 reads `This transfer costs … you have …`. **Without touching either slot**, gain XP until you cross the price. |
| **Predict** | The moment it crosses, the cell goes **YELLOW at 3 seconds** — a fresh countdown — and only then LIME. |
| **Predict** | ***IT MUST NOT ARRIVE LIME.*** A face that becomes actionable with no countdown is a click already in flight landing on an irreversible action through the one path the arm does not otherwise watch. |
| **READ** | **PASS** -- booted by Ben, recorded 2026-09-22. Figures not captured (Ben's ruling, 2026-09-20; see `GATE-gearscore.md`). |

### R29 — Close, death and disconnect mid-arm return both items and consume nothing

| | |
|---|---|
| **Setup** | Both slots full, countdown running. (i) **Esc**. (ii) re-stage, **`/kill`**. (iii) re-stage, **disconnect and rejoin**. (iv) re-stage, **`stop` the server** and restart. |
| **Predict** | **Exactly two items** back each time, unchanged, and the XP figure never moves. No third item, and no copy of the preview. |
| **READ** | **PASS** -- booted by Ben, recorded 2026-09-22. Figures not captured (Ben's ruling, 2026-09-20; see `GATE-gearscore.md`). |

### R30 — THE CONTROL: thirty seconds of ticking destroys nothing

**The grindstone gate's Row 42 twin, and it exists for the same reason.** Every other row in this
block is read inside a few seconds — well inside one or two countdowns — so **a repaint that
clobbers an input cell on its tenth fire never gets the chance to show itself.**

| | |
|---|---|
| **Setup** | Put both items in and **walk away for thirty seconds** without clicking anything. |
| **Predict** | Both items are still in 20 and 22, unchanged. The preview still shows the result. The bar is LIME and the button reads its ready text. |
| **Predict** | Thirty seconds is chosen to **outlast every other row's dwell time**, not because thirty is significant. |
| **READ** | **PASS** -- booted by Ben, recorded 2026-09-22. Figures not captured (Ben's ruling, 2026-09-20; see `GATE-gearscore.md`). |
