# GATE — the Nexus: the six routes out, and the two things a unit test cannot see

**Status: PARTIALLY RUN — ROW 6 ONLY, 2026-09-15. Rows 1–5 and 7 remain NOT RUN, and so does every
row of SLICE 2 (8–13), SLICE 3 (14–19), SLICE 4a (20–24), SLICE 4b (25–30) and SLICE 5 (31–34)** —
each block carries its own status line. **Twenty-nine of thirty-four rows have never been booted,
and the file is still growing**; that is flagged for the operator rather than hidden in a block
header.

> ### FOUR PREDICTIONS WERE RESTAGED ON 2026-09-16, AND EDITING A PREDICTION NORMALLY IS NOT ALLOWED
>
> Ben re-ruled four layout constants: the stats head **20 → 13**, the settings back button
> **45 → 48**, back's material **NETHER_STAR → ARROW**, and the hub's title **"Nexus" → "Nexus
> Menu"**. **Rows 14, 16, 27 and 30 were staged against the old values** and would have sent an
> operator to look at the wrong cell.
>
> **THE NO-EDIT RULE BINDS A PREDICTION THAT HAS BEEN READ, AND NONE OF THESE HAS BEEN.** This file
> carries exactly ONE reading — Row 6's — so there was nothing to falsify and nothing to void.
> **The predictions were rewritten in place**, which is the correct treatment for an unread row whose
> subject moved; the alternative is a gate that describes a screen that no longer exists.
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
| **26, 29** | **SURVIVAL**, and **26c especially** | 26c reads a **displaced item** surviving, and 29 counts hotbar cells. Creative makes items free, so *"the item is still there"* is satisfied for nothing — the register's own shape: **creative removes a cost, and a row whose reading is "the thing is still there" passes without exercising anything** |
| **18** | **SURVIVAL**, and **this one is load-bearing** | it moves an item **in the player's own inventory with a menu open** — the exact surface Row 8 shows behaves differently in creative. **A creative reading of 18 certifies creative and says nothing about the shipped path** |

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
> > RE-QUOTED.** The file is now `PARTIALLY RUN` — Row 6 has been read. The paragraph above is a
> > correct account of a change made while nothing had been read, and it is **no longer a licence to
> > make another one.** A precondition that lapses silently is `CLAUDE.md`'s own *control carried
> > past its precondition*: 5c and 5d are still unread, but the sentence *"nothing has been read"* is
> > now false of this file and cannot be cited again as written.

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

**READING — 2026-09-15, booted by Ben. CONDITIONS: CREATIVE mode, own-inventory screen (E).**
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
read here and are VOID against a survival gate. **Status: NOT RUN** — every prediction below was
written before any boot of this row.

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

**NOT BUILT, PENDING THE RULING.** The prohibition on `NexusLock` was given because the two-member
arms are correct and must not be "fixed" — **and A does not touch them**: it adds a second axis
(*what is being written*) to one arm, leaving every slot decision as it is. **But it is still an edit
to a file that was named off-limits, so it is asked rather than assumed.** B is available and needs
no ruling; it costs the test.

**EITHER WAY THIS ROW SHIPS.** Ben's instruction was *fix it if it is cheap; gate it as survival
either way* — 8d is that survival gate, and it is written before any fix so the fix cannot be
back-fitted to it.

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

**Staging.** `/gamemode survival`, two sub-rows, and **33b is the row**:

| | staging | expected |
|---|---|---|
| **33a** | open crafting **FROM THE HUB** (slot 31) | an **ARROW at slot 17** named **"Back to the Nexus"**. Clicking it returns to the hub |
| **33b** | **THE CONTROL** — right-click a crafting table **in the world** | **NO button at slot 17.** The slot is whatever it was before this slice |

> **33b IS THE HALF THAT CAN FAIL SILENTLY.** A build that always paints Back passes 33a perfectly.
> The world-opened screen would then offer to return a player to a hub they never opened — a
> back-arrow promising a destination they did not come from, which is the line `MenuIcons.close`'s
> javadoc draws from the other side.
>
> **AND THE STATUS BAR IS EIGHT GRAY CELLS IN BOTH.** Count them on each sub-row. Back is at 17,
> not in the bottom row, precisely so the bar's width does not depend on how the screen was opened
> — **a readout whose geometry changes by origin is not a readout.** If 33a shows seven, Back has
> been put in the bar and row `Q18` needs restaging.
>
> **Load the grid before clicking Back in 33a.** The items must come back: this screen HAS input
> slots, so it closes before it navigates, and `returnEverything` runs on that close.

**READING:** _(not run)_

## ROW 34 — THE NAVIGATION COLUMN, READ AS A COLUMN

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
