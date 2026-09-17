# PLAN — SLICE 11, PR 2: THE VAULT SCREEN, THE HIJACK AND THE MIGRATION

**Status: ANSWERED AND BUILT.** This is the rewrite the withdrawn PR 2 draft was parked for. The two
findings that were withdrawn with it are **decided here**, not deferred again.

## BEN'S ANSWERS, 2026-09-17 — THE THREE QUESTIONS THIS PLAN PUT

**1. NO SPLIT.** PR 2 carries the screen, the hijack and the migration together. **The proposed 2a/2b
seam had a defect and the plan was wrong to offer it:** 2a ships the screen, so 2a makes the vault
WRITABLE from the hub; a level-20 player fills page 1; 2b then runs migration into a page that
already holds thirty items and the 27 vanilla stacks have nowhere to go. **That is the same window
this plan caught between PR 1 and PR 2, one instant later.** Migration ships with the first write
path into the vault, and the screen IS the write path.

> **ALSO REJECTED, SO IT IS NOT RE-PROPOSED:** the base-class change alone as its own PR
> (`returnedSlots` + degrade, defaulting to `inputSlots()`, no vault). Invisible, and it isolates the
> shared-class risk — but **`Menu` cannot be constructed without a server, so that PR has almost no
> test surface**, and the opt-out row needs the vault to exist. **A PR that proves nothing is not a
> smaller risk, it is a deferred one.**

**2. THE RESIDUAL IS ACCEPTED, WITH ONE CONDITION.** Duplication over destruction is right, for the
reason given: recoverability is asymmetric. **But "reversible by hand" is only true if the SEVERE
line says what to reverse** — it must name the player UUID, the page, the slot, and the item in a
form an operator can match against what the player is holding. **A line reading "vault write failed"
makes the argument aspirational rather than true.** That line's content is asserted in a test; it is
load-bearing, not logging.

**3. POISON THE CACHE.** Ben's words: *"your finding, and it is the one I missed."* A degrade that
flips the hook and leaves the cache ahead of disk rebuilds the duplication arm through
`saveAllAndClear()` at `/stop` — the fix for one arm building the other. Poison the cache, refuse
further input, and make `saveAllAndClear()` **skip** a poisoned vault rather than republish it.

**`writeQueued` + `MUT-COALESCE`: taken.** Two gestures in one tick issuing two writes is not merely
wasteful — the second read could land between them.

---

## Base — the squash, not the branch

```
origin/master  0735c6909e2b5347c37c3abb79956c42e8271dc7   (git ls-remote, 2026-09-17)
local master   0735c6909e2b5347c37c3abb79956c42e8271dc7   identical
```

`feat/vault-storage` is **deleted from the wire and locally**; `check-absorbed.sh` said `ABSORBED at
0735c690` with its control line, and the branch tree and the squash tree are the same object
(`870e78e0`). PR 2 branches from `0735c69`.

**Baselines, measured at that SHA rather than carried forward:**

```bash
git grep -ho '@Test' 0735c69 -- '<module>/src/test/**/*.java' | wc -l   # core 1052 / storage 57 / paper 822 = 1931
git grep -c '^## ROW' 0735c69 -- GATE-nexus.md                          # 96
```

**New gate rows start at ROW 97.** Re-derive the denominator at merge; do not adjust this one.

**What PR 1 already put on disk**, so this plan can stop re-describing it: `VaultShape` (7 × 36),
`PlayerVault` / `VaultEntry` / `VaultRepository` / `FileVaultRepository` / `VaultMigrations`,
`VaultCodec`, `VaultService` (cache keyed to the *future*, write-through, `saveAllAndClear`), the
`/rpg vault` dev command, and the join/quit wiring in `RpgListeners`. Rows 92 and 94 booted: the PDC
round trip and the flush-then-shutdown ordering are **witnessed**, not assumed.

---

# DECISION 1 — HOP ALWAYS, THEN ENCODE ALL 36, THEN WRITE

**This is settled, and it is one rule rather than a per-intent table.** It was already the withdrawn
draft's conclusion; what follows is the re-verification against the code as it stands at `0735c69`,
because a conclusion carried across a withdrawal is a claim until someone re-reads the file.

### Re-verified, not recalled

`GridClickIntent.stacking` reaches **`PERMIT`** on two arms, and both matter:

```java
if (OUTBOUND.contains(action)) return PERMIT;          // PICKUP_ALL/SOME/HALF/ONE  -- TAKE OUT
if (INBOUND.contains(action)) {
    if (!accepted) return REFUSE;
    if (restingEmpty) return PERMIT;                   // PLACE_ALL/SOME/ONE        -- PUT IN
    ...
}
```

`MenuRouting.inputClick`'s `PERMIT` arm then calls `event.setCancelled(false)` and yields a
`MenuClick` with `itemMoved=true` — **before vanilla has applied anything.** `Menu.handleClick` calls
`onClick` immediately after. So on a `STACKING` grid, `onClick` runs **one tick early in both
directions**:

| gesture | intent | what a synchronous read sees | what a synchronous write persists | outcome |
|---|---|---|---|---|
| **put-in**, empty cell | `PERMIT` | the cell still empty | the page **without** the item just placed | the close returns nothing (`returnedSlots()` is empty) — **THE ITEM IS DESTROYED** |
| **take-out** | `PERMIT` | the cell still occupied | the page **with** the item still in it | the cursor branch hands the player their copy — **THE PLAYER HAS TWO** |

> ***THE DUPLICATION ARM IS THE ONE THAT SURVIVES REVIEW.*** Every gate row in the block is written
> against loss — *"the item is still there"* — and **a duplicate satisfies all of them.** Nobody is
> looking for a gain, which is why it needs its own row (ROW 103 below) rather than a clause in
> somebody else's.

`MERGE_ALL`, `MERGE_ONE` and `SWAP` are **performed by us, synchronously**, and would be correct to
write synchronously. **Do not special-case them.** A hop after a synchronous perform still reads the
settled state, so one rule is correct for all five results and there is no table for a later reader
to get half-right:

> ### HOP ALWAYS, THEN ENCODE ALL 36, THEN WRITE.

### And `onDragPermitted()` is the same defect — the draft did not mention it at all

`Menu.handleDrag` un-cancels an enumerated drag whose every raw slot is a `STACKING` input slot, then
calls `onDragPermitted()`. That hook's **own javadoc already says the contents have not changed yet**
and prescribes the one-tick hop. A drag spreading a stack across the page, written synchronously,
persists the page as it was before the spread — and then the close hands back nothing. Same hop, same
write, same method.

### Shape

```java
/** Schedule the one write. Called by onClick, onDragPermitted and the page flip, and by nothing else. */
private void scheduleWrite(int page) {
    adapters.scheduler().onEntity(viewer, () -> {
        if (!viewer.isOnline()) return;               // a load can settle after they have left
        Map<Integer, String> encoded = encodeAllSlots();   // ALL 36, on the main thread
        vaults.writePage(viewer.getUniqueId(), page, encoded);
    });
}
```

- **All 36, never the cell that changed.** `writePage` is already wholesale for this reason and says
  so: *"right until a gesture moves two and a drag moves nine."*
- **`Scheduler.onEntity` hops a tick** — measured in `Menu.open`'s javadoc against the pinned
  `paper-api`: `EntityScheduler.run` is *"schedules a task to execute on the next tick"*. No
  `onEntityLater(..., 1)` needed; the two are equivalent here.
- **Encoding is on the main thread and only the string crosses.** `VaultCodec` needs
  `Bukkit.getUnsafe()`; `VaultService` never sees an `ItemStack`. That is `CombatantSnapshot`'s rule
  applied to items, and PR 1 already built the seam for it.
- **The page flip needs no hop of its own.** It becomes *write current page, then load target*
  **after** the same hop everything else uses.

### The one thing this shape adds that the withdrawn draft did not name

**Two gestures in one tick must not issue two writes**, and a fast player produces them. A `boolean
writeQueued` field, cleared inside the scheduled task, collapses them — the second gesture is already
included in the *all 36* snapshot the first one will take. **Mutation `MUT-COALESCE` deletes the
flag** and a row asserts the write count, so the collapse cannot silently become "only the first
gesture is written".

---

# DECISION 2 — THE FAILURE PATH, AND A DEFECT THE WITHDRAWN DRAFT DID NOT REACH

`returnedSlots() = Set.of()` — the vault's whole reason for existing as a menu that does **not** hand
its contents back — rests on one premise: **the file is current.** PR 1's `VaultService.writePage`
javadoc already names this as owed:

> *"This reports a SYNCHRONOUS refusal (the vault is not loaded) and logs an ASYNCHRONOUS failure (the
> disk write threw). The screen will need the second one surfaced."*

### FIRST, THE DEFECT NOBODY HAS WRITTEN DOWN: A FAILED WRITE LEAVES THE CACHE AHEAD OF DISK, AND EVERY LATER WRITE REPUBLISHES IT

Read `writePage` in order:

```java
PlayerVault updated = current.withPage(page, contents);
vaults.put(playerId, CompletableFuture.completedFuture(updated));   // CACHE SWAPPED FIRST
repository.save(updated).exceptionally(error -> { log.log(SEVERE, ...); return null; });
```

The cache is swapped **before** the save is issued, and `repository.save` writes the **whole vault**,
not the page. So after a failed write:

1. the cache claims page *N* holds the new contents; disk holds the old,
2. **any later successful write of any OTHER page persists page *N* too** — `withPage` carries every
   other page's entries forward, and `save` writes the lot,
3. **and `saveAllAndClear()` at `/stop` writes the cache**, so a clean shutdown alone republishes it.

**Combine that with a degraded close that hands the page back and you have built a duplicator out of
the failure path** — the same arm as Decision 1, arriving from the direction nobody was watching.
`onJoin` already has the right instinct for the read side (*"an unreadable one completes exceptionally
and stays that way for the session, so every write is refused and nothing saves over it"*). **The
write side has no such policy. This adds one.**

### THE DECISION

On an **exceptionally-completed** page write:

1. **Log `SEVERE`** — already done, keep it, and it must name the player and the page, because this
   line is the only thing that makes the residual below repairable by hand.
2. **POISON the vault for the session.** Replace the cached future with a failed one, so
   `vault()` and `writePage()` refuse forever after and **`saveAllAndClear` cannot republish the
   ahead-of-disk cache.** Disk stops at the last successful write and is never written over.
3. **Mark the open menu DEGRADED**, and **from that moment `returnedSlots()` answers `inputSlots()`
   again**, so the close hands the page back rather than holding items nothing persisted.
4. **Refuse all further input** while degraded, and say so once in chat. A screen that silently stops
   accepting items is the *"crafting table that does nothing"* case `NexusStationGate` already
   names.

**THREADING: the exceptional completion arrives on the `storageIo` thread.** Steps 3 and 4 touch the
menu, so the callback hops with `Scheduler.onEntity` before it touches anything Bukkit. Step 2 is
pure map work and stays where it lands.

### THE RESIDUAL, NAMED RATHER THAN BURIED — THIS DECISION ACCEPTS A DUPLICATION ARM

**A take-out whose write failed duplicates that item.** Disk keeps its last good copy (which still
contains it), the item is already on the player's cursor, and the degraded close hands the cursor
back. **There is no repair that does not confiscate**, because the cursor at that moment is
indistinguishable from an item the player brought in from their own inventory.

**The alternative, stated so the trade is visible rather than implied:** trust disk, return nothing,
and every item put in since the last successful write is **destroyed**. That converts a disk fault
into item loss — the one outcome `MenuSafety` exists to make unreachable, and the one a player cannot
recover from.

> **WHY DUPLICATION IS THE RIGHT SIDE TO FAIL ON *HERE*, AND ONLY HERE:** the `SEVERE` line names the
> player, the page and the time, so **an operator can reverse a duplicate.** Nobody can reverse a
> deletion. This is not a general licence — Decision 1 refuses duplication outright, because there
> the alternative is also free.

**BEN'S TO OVERRULE, and it is a one-field change if he does.** If the call is "never duplicate, even
at the cost of a put-in", step 3 inverts to *keep `returnedSlots()` empty and re-render the page from
the last known-good vault*.

### CONSEQUENCE FOR THE TEST, WHICH IS THE HALF THAT GETS DROPPED

**`returnedSlots()` is no longer a constant.** The *"the opt-out is load-bearing"* test must exercise
**both answers** — empty while healthy, `inputSlots()` while degraded. A test that only ever sees
`Set.of()` cannot tell a working degrade path from one that never fires, which is this project's
unreachable-guard rule with the guard on the safety side.

---

# DECISION 3 — `NexusStationGate`: THE HEADER STANDS, THE RATIONALE DOES NOT

**Do not amend the class header.** *"THE GATE IS ON THE HUB ONLY. THE WORLD BLOCKS OPEN AT EVERY
LEVEL"* stays **literally true** under hijack-always: the ender chest opens our screen at every level,
exactly as the crafting table does.

**What stops being true is the rationale two paragraphs below it** — *"the feature is twenty blocks
away in their base"* — and the locked lore's third line that rationale justifies:

```
A grindstone in the world still works.     <-- TRUE
An ender chest in the world still works.   <-- FALSE. The hijack takes the vanilla chest away.
```

So: **amend the rationale, state in the same breath that the header still holds, and give the vault
station its own third line** that does not promise a world route it no longer has. The exact wording
is Ben's — `lockedLore` is already marked **DRAFTED, NOT SETTLED**, and it is one method and one test
row.

---

## The rest of the slice, carried forward and re-grounded

**The screen.** 54 slots. Top row: `0` left arrow, `1..7` the seven page buttons, `8` right arrow —
`VaultShape`'s javadoc already fixes this and the layout test asserts `PAGE_COUNT == 7` buttons rather
than leaving a reader to notice. Rows 2–5 (indices 9–44) are the 36 input slots, **all `STACKING`**.
Bottom row is chrome with Close at **49**, where all four existing screens put it.

> **PAGES ARE 0-BASED IN CODE AND 1-BASED TO THE PLAYER.** `VaultShape.isPage` is `0..6`; the brief's
> thresholds are pages 1–7. **Every button, message and lore line converts at the edge**, and
> `MUT-PAGEIDX` exists to make a missing conversion go red.

**Gating — hijack always, migrate at 20.** All seven of Ben's thresholds survive unchanged
(20/25/30/**35**/40/45/50 — the p4 row the brief dropped). Below level 20 the ender chest opens a
vault with seven dimmed buttons and no reachable storage. Level comes from
`PlayerLevel.levelFor(profile.lifetimeXp())`, the same call `NexusMenu` already makes.

**The hijack is one map entry.** `RpgListeners.hijackedBlocks` gets `Material.ENDER_CHEST`, and
inherits `openHijackedBlock`'s unconditional cancel — the ordering that the enchanting table's
sneak-guard bug paid for. **No new listener method**, for exactly that reason.

**Migration, once and only once.** `PlayerProfile` goes to **v4** with a `vaultMigrated` stamp;
`ProfileMigrations` already carries the `v3 -> v4: add the next step here.` line. Deferred until page
1 unlocks, which is safe **because of the standing "do not clear the vanilla container" ruling** — the
27 stacks sit untouched until the player can reach them, and the copy is unreachable because the block
is hijacked. **The gate row asserts both halves: items appear on page 1 AND the vanilla container
still holds them.**

---

## Tests, mutations and rows

**Headless first, per `CLAUDE.md`.** `VaultScreenLayoutTest` (the seven-buttons-for-seven-pages
equality, the 36 input slots, the arrows, Close at 49, the 0-based/1-based conversion),
`VaultPageGateTest` (the seven thresholds, inclusive), `VaultMigrationTest` (the stamp; migrating
twice is a no-op), and the `returnedSlots()` pair **exercising both answers**.

| marker | mutation | the axis it moves |
|---|---|---|
| `MUT-NOHOP-PLACE` | the write reads slots synchronously on a `PERMIT` place | put-in, destruction arm |
| `MUT-NOHOP-TAKE` | same, on a `PERMIT` take-out | take-out, **duplication arm** — a separate axis, not the same mutation twice |
| `MUT-NOHOP-DRAG` | `onDragPermitted` writes synchronously | the drag path |
| `MUT-COALESCE` | the `writeQueued` flag is deleted | two gestures in one tick |
| `MUT-DEGRADE` | a failed write leaves `returnedSlots()` empty | the failure path |
| `MUT-POISON` | a failed write leaves the cache in place | **the republication defect above** |
| `MUT-RETURN` | `returnedSlots()` returns `inputSlots()` unconditionally | the opt-out itself |
| `MUT-STAMP` | migration does not stamp | migrates every join |
| `MUT-PAGEIDX` | the 0/1 conversion is dropped | off-by-one |
| `MUT-GATE` | page locks never fire | the thresholds |

**Count the axes before counting the mutations:** put-in and take-out are **two degrees of freedom of
one expression**, which is why `MUT-NOHOP-PLACE` and `MUT-NOHOP-TAKE` are both listed. One splice
certifies one arm.

**Gate rows from ROW 97**, `Status: NOT RUN`, predictions before the boot, game mode declared in the
header **and** per row. The two that cost someone their things — **migration** and **take-out-then-
rejoin-and-count** — are sole-witness rows and say so. **ROW 103 is the duplication row and it exists
because no loss row can fail on it.**

---

## Three small items to fold in while these files are open

Batched deliberately so they do not each cost a PR.

**1. `CLAUDE.md`'s ref rule is scoped backwards — one line.** It reads *"Reason from `origin/<ref>`,
never a local ref."* That is right for *is this on the wire now* and **wrong for anything you write
down**, which is the rule `GATE-nexus.md` states in full: a count against `origin/master` is a count
against a moving target. Proposed:

> **Reason from `origin/<ref>`, never a local ref — and RECORD from a SHA, never from `origin/<ref>`.**
> The remote ref is the authority on what is on the wire *now*, and a moving target the moment it is
> written down.

**2. The `perl` traps exist and are not being found — a DISCOVERABILITY fix, not a new entry.** The
delimiter rule lives inside the mutation-lies table's commentary; the CRLF `$`-anchor rule lives
inside *A LONG-LIVED BRANCH*'s instrument table. **Neither is where a person about to write a scripted
edit is reading**, and the `\Q`-quotes-an-embedded-`\n` trap is in memory and in **no repo file at
all.** Proposed: one pointer in the *scripted edit* bullet, naming the three traps in their
operational form and their accounts **by section**, never by line.

> The account for `\Q` has nowhere in the repo to go. That is the standing `NEXT.md` debt, and it is
> named here rather than quietly solved by putting an account in the pointer file.

**3. The instrument-family entry.** Proposed for the *narrower question* table:

> ***A ZERO IN THE WRONG SCOPE IS INDISTINGUISHABLE FROM AN ABSENCE.*** The other members hand you a
> misleading number. This one hands you a **zero**, which reads as *nothing is there* in every
> instrument, so it needs no misreading to do its damage.

**Its worked instance is OWED and is deliberately not invented here.** Two candidates are already
measured in the record — `git diff --numstat` printing `20  0` for `MUTSTATS19`, where the zero
deletions belong to the HEAD scope and not the mutation's; and `-Dtest` aborting in `rpg-core` with
*"No tests matching pattern"*, a zero in the wrong module scope for a test that lives in `paper/`.
**Ben names which one he meant, or supplies the one I have not got.** A taxonomy with a plausible
example instead of a witnessed one is the defect this repo files under *hollow*.

---

## WHAT THE BUILD ADDED THAT THIS PLAN DID NOT FORESEE

Recorded here because a plan that reads as if it predicted everything is a plan nobody learns from.

1. **OPAQUE ENTRIES WOULD HAVE BEEN DELETED BY THE FIRST WRITE.** The screen writes a page by
   encoding the 36 live cells — and an entry that cannot be DECODED cannot be rendered into a cell to
   be re-encoded, so it would simply be absent from the snapshot. **PR 1 promises those bytes are
   never discarded.** Fixed with an `opaque` map: kept out of `inputSlots()`, painted as a barrier,
   written back verbatim. Gate row 107 reads it.

2. **A LOCKED PAGE MUST NEVER BE WRITTEN.** Its cells hold PANES, and encoding them would store
   chrome over whatever the page really holds. Input is refused there anyway; the flip needed an
   explicit guard.

3. **`degrade()` MUST NOT REPAINT THE STORAGE.** The first draft called `render()` — which repaints
   the cells FROM THE VAULT, overwriting the player's only copy with the last good contents.
   **That is the exact loss the degrade path exists to prevent, written into the degrade path.**

4. **A POISONED VAULT CAN BE POISONED BY SOMEBODY ELSE.** `/rpg vault store` failing poisons the
   vault while this screen is open and healthy. The first draft of `scheduleWrite` returned quietly
   when the page was not writable, which would have left the cells unpersisted behind a
   `returnedSlots()` that hands back nothing.

5. **THE MIGRATION STAMP LIVES ON THE PROFILE, AND THE VAULT FILE WAS CONSIDERED.** A vault-side flag
   would cost only the vault on a rollback, where the profile bump refuses **every** login. **The
   profile was kept** because `PlayerVault`'s javadoc already rules it there and this plan was
   reviewed on that basis — the alternative is recorded rather than taken.

## OPEN — the question this plan asked, and the answer it got

**ANSWERED ABOVE: NO SPLIT.** Kept below as written, because the reasoning that was wrong is worth
more than a deleted section — the seam looked clean and it had a window in it.

**PR 2 as scoped is the screen, the hijack and the migration in one branch**, and it carries both
duplication arms, a failure path, a schema bump and an irreversible one-time item move.

**My recommendation: split it.**

| | contents | why here |
|---|---|---|
| **PR 2a** | the screen, `returnedSlots()`, the hop, the degrade path. Reached from the **hub station only**. | every hazard in Decisions 1 and 2 is in this half, and it is bootable without touching anybody's ender chest |
| **PR 2b** | the `ENDER_CHEST` hijack, the v4 stamp, the migration | one-time, irreversible, and it wants a gate block that is not sharing a boot with a screen nobody has clicked yet |

**Ben's call — the two-PR split was his ruling and this would make it three.** I have not assumed it:
everything above is written so it holds either way.
