# GATE — the Dragon's Plume's draw (slice H1)

**Status: NOT RUN.** Every row below was written **before any boot**, and no figure in this file is
an observation. A row that later carries a reading will say so in its own cell.

> ## AND IT CANNOT BE RUN YET — THE BLOCKER IS NAMED RATHER THAN WORKED AROUND
>
> **H1's gate is: one of our weapons, `material: bow`, binding no `right_click`.** The only bow in
> the project is `hunters_bow`, which **binds `right_click`** — so `RpgListeners` cancels its vanilla
> interaction, `BowItem.use` never runs, the draw never starts, and there is nothing for H1 to read.
>
> **The first weapon that matches the gate is the Dragon's Plume itself**, whose content file is the
> content chat's seat. **So these rows wait on `dragons_plume.yml`** — the same state `GATE-locust.md`
> row 1 sat in, and the reason its verdict column stayed empty.
>
> **What is NOT blocked:** H1's arithmetic is `core` and is guarded by `DrawChargeTest` (8 rows),
> which needs no server at all. What waits here is only what a server can answer.

---

## WHAT H1 IS FOR, IN ONE SENTENCE

**A bow that ticks and does not shoot.** Every high-risk unknown in this weapon is in the input layer,
and all of them are answered by a bow that only ticks — so firing, homing, damage and round-spending
are H2's, deliberately, and none of them can confuse a symptom here.

## THE ROWS

| row | staging | what to record |
|---|---|---|
| **H-1** | Hold a full draw for **five seconds** with a full magazine. | **How many ticks sound, and whether the rise is tellable apart BY EAR** — have a second person, not watching the screen, call the count. Five expected, at ~4 semitones a step (R13). **The second half is the ruling's actual test**; a sound can be pleasant and still fail it. |
| **H-2** | **THE CAP.** Load exactly **two** rounds, then hold a full draw for five seconds. | **The ticks must STOP at two.** Record how many sounded. Then, **without releasing**, have a reload complete mid-draw and record whether the ticks RESUME — R3's cap is live, and a cap read once at full charge is indistinguishable from a live one on a magazine that never changes. |
| **H-3** | **THE ARROW — and this row proves a MECHANISM, not an absence.** One arrow in the off-hand, nothing else in the bag. Draw, hold past full charge, release. | **Is the arrow still there?** It survives only if `clearActiveItem()` inside `PlayerStopUsingItemEvent` made `LivingEntity.releaseUsingItem`'s re-read at offset 72 yield EMPTY, so `BowItem.releaseUsing` — and therefore `draw()`, and therefore `useAmmo` — never ran. **If the arrow is gone, that specific chain is what failed**, and the server log will say so: the `EntityShootBowEvent` guard fires loudly precisely here. |
| **H-4** | **THE VISUAL DESYNC.** Draw, hold to five, release — and **watch the first-person hand and a second player's view of you**. Repeat while moving, and while looking up. | **Does the client keep animating a draw it no longer has?** The server clears the active item mid-release; nothing guarantees the client agrees. **Write this row's answer in words, not a verdict** — "the bow snapped back instantly" and "the arm stayed pulled for about a second" are different findings and both are passes for the mechanism. **No amount of bytecode reading could have predicted this row**, which is why it is here. |
| **H-5** | **THE NEGATIVE ROW, and it is the one that fails if the gate is wrong.** With H1 installed: eat a food item to completion; raise and lower a shield; fire an **ordinary vanilla bow**; drink a potion; use a spyglass. | **Each must behave exactly as it did before.** `PlayerStopUsingItemEvent` fires for **every** item release on the server, so an ungated `clearActiveItem()` would break all of these silently. **This row exists because of the gate, and it is the row that catches a gate that does not gate.** |

> **H-3 AND H-5 FAIL IN OPPOSITE DIRECTIONS, AND THAT IS WHY BOTH ARE HERE.** H-3 fails if the clear
> does not happen; H-5 fails if it happens to things it should not. A gate that is too narrow loses
> the arrow; a gate that is too wide breaks eating. Neither row can see the other's defect.

## WHAT THIS GATE DOES NOT COVER

- **Anything that fires.** H1 spawns no projectile, spends no round, deals no damage. H2's rows are
  about arrows; these are about architecture.
- **The homing constants.** `PLAN-dragons-plume.md` §5 carries them `INHERITED AND UNJUDGED` with
  rows P1-P4, and nothing here touches them.
- **The pitch ceiling.** `2.0` is outside knowledge this machine cannot measure — the pinned API
  documents no range and the packet carries a raw float. **H-1 is where a person finds out**, and if
  the top of the ladder is inaudible or ugly that is a reading, not a failure.

## THE ONE THING MEASURED AND THE ONE THING NOT

**MEASURED, from `run/versions/26.1.2/paper-26.1.2.jar`:** that consumption happens inside `draw()`
at offset 71 while `EntityShootBowEvent` is constructed inside `shoot()` at 133 — so the arrow is
gone before that event object exists, and cancelling it can never keep the arrow; that
`PlayerStopUsingItemEvent` fires at 67 and the `useItem` field is re-read at 72; that
`stopUsingItem()` empties that field; and that `Item.releaseUsing` on an empty stack is
`iconst_0; ireturn`, which `AirItem` does not override.

**NOT MEASURED, AND IT IS THIS GATE'S JOB:** that `clearActiveItem()` **called from inside that
handler** behaves live as the bytecode reads. The handler runs on the main thread inside the same
call, so it *should* — and **"should" is what the boot is for.**
