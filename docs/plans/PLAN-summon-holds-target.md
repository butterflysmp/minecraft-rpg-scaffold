# PLAN — A SUMMON HOLDS ITS TARGET (G1 TO G8), 2026-11-26

**Status: BUILT, NOT BOOTED.** From the seat's block of 2026-11-26, "A SUMMON-TARGETING FIX TO STACK ON THE FOLLOW-UP PR",
via Ben. Branch `feat/summon-holds-target`, stacked on `feat/owed-fixes` (#330). Its rows S1 to S5 are in the one short
gate at the top of the stack.

## BEN'S WORDS, VERBATIM

- 2026-11-26, on the Blink Tome's scoping: *"7. Summons should keep their target until it is defeated or their target
  switches. This isn't related to the Blink tome. It has more to do with summon target priorities. They generally
  shouldn't retreat to their caster unless they don't have anything else to do."*
- 2026-11-26, asked whether a wolf should ever give up its target for distance: *"2. A wolf (or any summon) should give up
  if it's target if it's target is outside of a 72 block range."*
- **The seat's reading, OPEN FOR BEN:** "a 72 block range" is the distance from the OWNER to the summon's target (how the
  question was put). Ben has been told and may correct it.

## WHAT HELD ON MASTER (K10's READ, confirmed before any change)

`SummonGuard.decide` ranked directed > damaged the owner within 100 ticks > targeting the owner within 30, and within a
tier kept the current target (`keepOrNearest`); it dropped a target dead, gone, or more than 32 from the owner
(`dropReason`, `Dropped.LIMIT`); the third tier read `targetsHim`, so a mob held only by it was dropped the tick it stopped
targeting the owner; the walk back beyond 8 ran only with no live target. **And (G4, READ) the teleport beyond 48 was
computed with no regard to the target, and `SummonGoal.tick` ran it before attacking: a summon holding a target COULD be
teleported to its owner mid-fight.** The 32 was content: `bone_whistle_wolf.yml`'s `chase_limit: 32  # SEAT PLACEHOLDER`,
read by `SummonGuard` (taking and dropping) and by `SummonGoal`'s scan radius.

## G1 TO G8, EACH WITH ITS READS

- **G1, A HELD TARGET IS KEPT** until the first of: dead (`!alive`), gone (not among the candidates; a body in another
  world is left out of the scan, so it reads gone: CC'S CALL, reported `dropped:gone`), beyond 72 from the owner, or a
  switch (G2). The reason lapsing is not a drop: the held target stays, written once as `reason=kept` on the tick it
  lapses (`Held.lapsed` carries "already said"). **The owner dead, gone or in another world: today's rule, unchanged
  (READ, `SummonRemoval`):** the summon itself is removed (`OWNER_DEATH`, `QUIT`, `WORLD_CHANGE`), and the goal's tick
  returns at once while he is offline or in another world, so no decision is made without him.
- **G2, SWITCHING IS UPWARD ONLY.** `Held(id, tier, lapsed)` remembers the tier. A directed order replaces any other
  target (`switched:<from>->directed`); a mob that damaged him within the window replaces one held at the targeting tier;
  an equal or a lower tier never switches. The same order on the mob it already holds is no change. **SEAT PLACEHOLDER,
  OPEN FOR BEN:** that a mob which hits the owner pulls the summon off a mob only targeting him (today's ranking, kept);
  the alternative is that only his orders switch it. **CC'S CALL:** the held mob ITSELF reaching a higher tier (it hits
  him) is promoted in place, with no switch and no line, so another damaging mob cannot then take the summon off it; the
  alternative is to keep its first tier.
- **G3, THE GIVE-UP RANGE IS 72 (BEN), one named value where the 32 lived:** content `give_up_range: 72` (the key renamed
  from `chase_limit`, so the file says what it is), `SummonDefinition.giveUpRange`, read by the guard (dropping a held or
  directed target, and acquiring) and by the goal's scan radius. Owner's feet to target's feet; 72.0 keeps, beyond gives
  up. Any summon: no id is named. **A CONSEQUENCE, SAID PLAINLY (the seat's "no second number"):** the reach at which a
  directed or a damaging mob may be ACQUIRED grows from 32 to 72 with it; the targeting tier is still bounded by its 30.
- **G4, NO RECALL MID-FIGHT.** The teleport is now decided AFTER the target: `teleport = nothing held && beyond 48`. With
  a target the summon is neither teleported nor walked back (the walk back was already idle-only). With no target, the
  follow, walk back and teleport are exactly as they were.
- **G5, THE TABLE** (`SummonGuardTest`, below): every D4 row re-stated with its answer (two changed: a directed mob at 33
  is now taken; a held damaged mob whose window expires is now kept), and G5's rows.
- **G6, NO FLAPPING AT THE BOUNDARY:** a target given up for range comes back only by a fresh acquisition: a NEW order
  (the old one was cleared with the drop), or a damaged-him reason within the 100-tick window, inside 72. The targeting
  tier cannot re-take it there (30 < 72). So the only flap possible is a mob that hit him within the last 100 ticks
  hovering at 72, and it ends with the window. CC'S CALL: no hysteresis band; the alternative is a re-take range below 72.
- **G7, THE TRACE.** `SUMMON target … target=<id|none> why=<tier held> [dropped=<dead|gone|range>] reason=<…>`, the reason
  one of `acquired:<tier>`, `switched:<from>-><to>`, `kept`, `dropped:<reason>`, joined by a comma when a drop and an
  acquisition share a tick (`dropped:dead,acquired:damaged`). Written once, when something changed (`change != NONE` or a
  drop). `dropped:owner` is never written: the owner's loss removes the summon (its `SUMMON removed … cause=` line).
  `probe_summon` prints ` tier=<directed|damaged|targeting|none>` after `target=`.
- **G8, NOTHING ELSE MOVED:** the cap, the sides table, the spawn, the removals and the damage are untouched; the 100, the
  30, the cadence and the reach are as they were. The changed files: `SummonGuard`, `SummonDefinition` (the rename),
  `SummonTrace.target`, `SummonStore` (the held tier), `SummonLoader`'s key, `bone_whistle_wolf.yml`, `SummonGoal`, the
  probe's line and `mob_target`'s new `clear` form (the gate's S2).

## THE GUARD'S TABLE, WHOLE (`SummonGuardTest`; he at the origin, tick 1000, window 100, trigger 30, range 72, teleport 48)

| held | candidates / order | answer | reason |
|---|---|---|---|
| none | directed C, damaged A, targeting B | C | `acquired:directed` |
| none | damaged A (20), targeting B (10) | A, though farther | `acquired:damaged` |
| none | targeting B | B | `acquired:targeting` |
| none | a mob that did neither | none | `none` |
| none | damaged at exactly the window's edge / one past | taken / not | |
| none | targeting at 30 / 30.01 | taken / not | |
| none | two targeting, the summon nearer the farther | the one nearest HIM | |
| none | a player or a summon, directed, damaging, targeting | never | |
| none | directed A at 33 (D4: dropped) | **NEW: taken** | `acquired:directed` |
| none | directed A at 72.001 | none, order cleared | `dropped:range` |
| none | directed A dead / not present | none | `dropped:dead` / `dropped:gone` |
| none | summon 48.01 / 48 from him | teleport / not | |
| B targeting | B targeting, D damaged | D | `switched:targeting->damaged` |
| B targeting | B still targeting | B | nothing written |
| D damaged | D, a nearer damaged A | D | nothing written |
| D damaged | D's window expired, A damaged (D4: switched to A) | **NEW: D** | `kept` |
| A damaged | order on C | C | `switched:damaged->directed` |
| C directed | C dead, A damaged | A, order cleared | `dropped:dead,acquired:damaged` |
| B targeting | B gone, A damaged | A | `dropped:gone,acquired:damaged` |
| B targeting | B stopped targeting him | B | `kept` (once; then nothing) |
| B targeting | every mob forgets him (he blinked) | B, no teleport | `kept` |
| B targeting | A targeting, nearer | B | (equal tier) |
| D damaged | A targeting | D | (lower tier) |
| D directed | a new order on C | C | `switched:directed->directed` |
| C directed | the same order on C | C | nothing written |
| B targeting | B now damaged him, A damaged | B, held at damaged | nothing written (CC'S CALL) |
| B | B at 72.0 / 72.001 from him | kept / none | — / `dropped:range` |
| B | B 70 from him, 130 from the summon | kept | (measured from him) |
| B targeting | B targeting at 40 (D4: `dropped=limit` at 32) | B | `kept` |
| B | summon 60 from him, B present / B gone | no teleport / teleport | |
| none after a range drop | B targeting at 71 / B damaged at 71 | not taken / taken | — / `acquired:damaged` |
| (idle) | summon at 8 / 8.01 from him | stays / walks back | |

## CC'S CALLS

1. A body in another world is left out of the scan, so it reads gone (`dropped:gone`).
2. The held mob itself reaching a higher tier is promoted in place, no switch and no line.
3. No hysteresis band at 72 (G6).
4. The key is renamed `give_up_range` (the alternative keeps `chase_limit` with the new number).
5. `mob_target: "<tag> clear"` is the gate's way to stage "the reason lapses", through the same seam.

## OPEN FOR BEN (SEAT PLACEHOLDER 2026-11-26)

1. "a 72 block range" read from the OWNER to the target.
2. A mob that hits him switches the summon off one that only targets him (today's ranking, kept).

## A FROZEN ROW THIS CHANGES, SAID SO

`GATE-summoner.md`'s SM5 (THE CHASE LIMIT) predicted, and the boot of its gate READ, `SUMMON target … target=none
why=none dropped=limit` for a directed zombie moved 40 east. Under G3 that row, if run again, reads the zombie KEPT (40 is
within 72). Its prediction stays as the record of the rule it tested; the short gate's S1 is its new form. No frozen row is
edited.

## OWED, NOT READ BY THE BOOT

- **The 72 boundary itself:** the pad is 48 long, so 72 cannot be staged on it. It is the unit table's, NOT READ, owed.
- **The upward switch to a mob that DAMAGED him** needs a wound: not staged; the unit table's, owed.
- A summon in another world, an owner who dies mid-fight: the unit table and today's removal rule; not staged.
