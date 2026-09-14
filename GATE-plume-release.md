# GATE — the Dragon's Plume's release (slice H2)

**Status: NOT RUN, AND THE SLICE IS NOT BUILT.** H2 spawns the projectiles through the existing cast
path with F/F2's homing, spends the rounds, and handles R4′ (a partial draw: one arrow, no homing, 12
damage) and R10 (under three ticks, nothing — vanilla's own floor). **None of that exists.**

**This file exists early for one reason: a row had to leave `GATE-plume-draw.md`, and a row with
nowhere to go is a row that gets deleted.** More rows join it when H2 is scoped.

---

## THE ROWS

| row | staging | what to record |
|---|---|---|
| **R-1** | **THE CAP — moved from H1's gate, where it could not be run.** Fire enough arrows to bring the magazine **under five**, then hold a full draw for five seconds. | **The ticks must STOP at the number of rounds left.** Record how many sounded against how many remained. Then, **without releasing**, have a reload complete mid-draw and record whether the ticks **RESUME**. |

> **R-1 IS `PLAN-dragons-plume.md`'s P5, LANDED — NOT A SECOND COPY OF IT.** The plan's §8 drafted its
> rows *"before the boot"* and says they go into a gate file when the slice is written; this is that
> file for the release half. **Anyone adding P5 again later would be writing the same row twice**,
> and two copies of a row drift exactly the way two accounts of a rule do.

> **THE SECOND HALF IS THE ONE THAT EARNS THE RULING, AND IT IS WHY THE ROW IS TWO PARTS.** R3 says
> the cap is read **LIVE**. *"Reads the magazine live"* and *"reads it once at full charge"* are
> **indistinguishable** on a magazine that does not change during the hold — so a row that only holds
> a short magazine would pass under either implementation. **Staging a reload that completes during
> the draw is what makes the two disagree.**

## WHY THIS ROW COULD NOT BE RUN AT H1

**Not "hard to stage" — unobservable in principle.** `roundsRemaining()` is `min(loaded, capacity)`,
clamped (slice G), and for the cap to bite it must come in under **5**, the maximum charge:

```
loaded    25 at mint, and IT CANNOT FALL AT H1. Quivers.spendRound has exactly ONE call
          site -- WeaponFire:183, gated on a Success and on the input not being
          left_click -- and the Plume binds NEITHER click, so WeaponFire.attempt returns
          empty for it. H1 fires nothing; left-click only reloads; a full magazine is
          ALREADY_FULL.

capacity  authored 25 plus modifiers, and every instrument ONLY ADDS: /rpg quiversize is
          DoubleArgumentType.doubleArg(0.0, 200.0) -- floor ZERO -- and the
          quiver_size_boost item it mints adds the same bonus.

          min(25, >= 25) = 25,  against a maximum charge of 5.
```

**BOTH HALVES WERE BLOCKED, not just the count:** raising capacity mid-draw does not move
`min(25, capacity)`, so the live re-read had no lever either.

> **THE TRIGGER THAT RELEASED IT WAS THIS SLICE, AND IT IS ALREADY SATISFIED THE MOMENT H2 FIRES
> ONCE.** One arrow takes `loaded` to 24; a few more put it under five and both halves become
> stageable immediately. **That is why the row lives here rather than waiting on a ruling.**
>
> **One other path exists in the arithmetic and not in the tree:** a capacity modifier that
> SUBTRACTS. `QuiverSize.arrows` handles a negative — `QuiverSizeTest` asserts `arrows(-2.1) == -3`
> — so the maths permits it and only the instruments forbid it. **The argument above rests on no
> instrument producing one**, which is a weaker claim than the arithmetic refusing it, and is stated
> as such.

## WHAT THIS GATE DOES NOT COVER

- **The input layer.** The draw, the tracker, the tick and the release's suppression of vanilla's
  shot are `GATE-plume-draw.md`'s, and H-1b there is still unread.
- **The homing constants.** `PLAN-dragons-plume.md` §5 carries them `INHERITED AND UNJUDGED` with
  rows P1-P4, and the sight gate's visible half is P9. H2 makes those runnable for the first time,
  because nothing has ever fired one of these arrows.
