# PLAN — slice B: the single-wield Boltor

> `PLAN-q7.md` was slice B's commit 1 (the instrument); Q7 is discharged, and `GATE-q7.md` carries
> the reading. This is the rest of the slice. Dual-wield, alternation and the off-hand ruling are
> slice C's.

## Context

The Ranger's identity weapon. A2 made the quiver a pair of real stats; Q7 measured the input floor
at **4 ticks** and found that an authored `cooldown_ticks` is **rounded up to the next input**. With
that number in hand the operator ruled the Boltor's full stat set, so this slice authors a weapon
whose numbers are decided rather than derived.

**Nothing here is priced against a dev weapon.** That constraint is the slice's spine and the reason
the damage ruling took the shape it did.

---

## THE RULED NUMBER SET — COMPLETE, WITH PROVENANCE

| | value | how it was decided |
|---|---|---|
| `quiver_size` | **8** | operator, original brief |
| `cooldown_ticks` | **16** | operator — **only multiples of 8 survive halving** (12→6→8 is 1.5×, 20→10→12 is 1.67×); 8 rejected for headroom, since a dual cooldown of 4 sits **on** the input floor and would make attack speed inert |
| `reload_ticks` | **60** (3.00s) | operator |
| `attack_damage` | **19** | operator — **the project's first balanced damage number** |
| `range` | **96** | operator; flight-time variance ruled IN |
| pierce | **none** | operator — stops at the first body, so damage is priced PER TARGET |

**`attack_damage: 19` IS NOT JUSTIFIED BY COMPARISON TO ANY EXISTING WEAPON, AND THE FILE MUST NOT
IMPLY IT IS.** `ironblade` and `hunters_bow` are both dev weapons; parity with either is parity with
a placeholder. 19 is ruled outright, and it becomes **the anchor the next class prices against** —
which the weapon file says, because otherwise the next slice repeats this in reverse.

**`19`, `152` and `96` are authored nowhere else in `content/`** — verified by grep over the whole
tree. The Boltor's set `{8, 16, 19, 60, 96}` is pairwise distinct, so any bare gate reading of one
of them is unambiguous *within this weapon*. Note this is **not** the fixture collision sweep:
`8`, `16` and `60` are authored elsewhere and deliberately so. A balanced weapon is allowed to share
a number with another file; a **fixture** is not, because a gate row staged on a fixture must be
able to tell the stat from the constant.

### Not ruled, and flagged rather than folded in

`rarity` and `element` were not in the ruled set. The file authors `rare` and `kinetic` and says at
each key that it is a choice, with the reasoning and an invitation to re-rule. `epic` is unused in
shipped content and introducing a tier for one weapon sets a precedent nothing else is ready for.

---

## CORRECTION OWED BEFORE ANYTHING IS PRICED: THE CONSEQUENCE FIGURES WERE WRONG TWICE OVER

**The cycle figures given to the operator — which his ruling record then quoted — were wrong, by two
compounding errors. The RULINGS stand; the consequences did not.**

```
                        as given (wrong)    corrected
  fire span             128t / 6.40s        112t / 5.60s
  cycle                 188t / 9.40s        172t / 8.60s
  sustained             0.851 sh/s          0.9302 sh/s
  sustained DPS @19     16.17               17.67
  burst DPS @19         23.75               27.14   (152 over 5.60s)
```

**1 · A FENCEPOST. Eight shots span SEVEN intervals, not eight.** `t = 0, 16 … 112`.

> **This project already documents this exact error, about this exact weapon.**
> `PLAN-quiver.md:817` — *"The fire-rate anchors I gave the operator were quoted past a fencepost.
> Eight shots at 14 ticks span 4.9 s … not 5.6."* It was re-made against the document that names it.

**2 · IT USED THE BRANCH A1 EXPLICITLY REFUSED.** Whether the last shot's cooldown gates the reload
was **ruled in A1: ungated, the reload interrupts freely** (`PLAN-quiver.md:831-836`). The reload
was added to a fire span that already contained the eighth shot's cooldown — the gated branch.

**The corrected model is validated against A1's own published figure**: at 14t it yields
`7.90 s → 1.0127 sh/s`, which is exactly what `PLAN-quiver.md:825` prints. The model reproduces a
number it was not fitted to, which is why it is trusted here and the first one was not.

> ### THE CORRECTED FIGURES COLLIDE WITH THE PUBLISHED ROW A1 REFUSED — READ THIS BEFORE CHECKING THEM
>
> `PLAN-quiver.md:824` prints, for the **14-tick** Boltor on the **`cooldown gates reload`** row —
> *the branch A1 refused* — **`8.60 s → 0.9302 sh/s`**. Those are the same two numbers this plan
> gives the **16-tick** Boltor.
>
> **It is arithmetic, not a mistake: `8 × 14 = 112 = 7 × 16`.** The gated span at 14 ticks and the
> ungated span at 16 ticks are the same 112 ticks, so both cycles are `112 + 60 = 172t`. Each figure
> is correct under its own branch.
>
> **The Boltor's figures come from the UNGATED branch at 16 ticks**, which is the branch A1 ruled
> live. A reader checking this against that table would otherwise find the Boltor's sustained figure
> sitting on the line marked *"the branch that was refused"* and conclude the wrong branch was used —
> **the number is right and it looks wrong, which is worse than the reverse.**
>
> **This is the sweep discipline arriving at a DERIVED number for the first time.** Authored numbers
> are swept against `content/`; cycle figures are not swept against anything, and this is the first
> one to collide with a published figure. **Every cycle figure from here on is derived**, so the
> check is named here rather than left to the next collision.

> **19 was ruled with `16.17` sustained in view; the true figure is `17.67`, about 9% higher.** The
> operator was told, and **ruled that 19 stands.**

**AND EVERY FIGURE IN THAT TABLE IS CONDITIONAL ON THE BOUNDARY READING.** They assume a 16-tick
delivered interval. If `GATE-boltor.md`'s first row reads 20, every one of them moves. Nothing is
priced on 16 until that row is taken.

---

## THE FINDING: NOTHING IN THE REPO DISTINGUISHES A BALANCED NUMBER FROM A PLACEHOLDER

**Recorded as a worked example, with this plan's own author as the instance**, because the rule
existed and the error was still made.

`PLAN-quiver.md:69-71` says the Boltor's damage must not be derived *"from `hunters_bow` (a dev
weapon by the operator's ruling)"*. **The rule named one instance. The anchor moved to `ironblade`
instead, and nobody asked whether the new anchor was in the same class.** It was.

**Why the error was available:**

- `ironblade.yml`'s header reads *"The first weapon. A plain sword."* — no marking, no caveat — and
  its `attack_damage: 8` carries a comment describing it as a source of truth for the tooltip, which
  **reads as authority rather than as scaffolding**.
- `hunters_bow`'s dev status lives in a **plan document**, not in `hunters_bow.yml`.
- A `grep` for "dev weapon" across `content/` returns two files, neither of them these.

**PROPERTY: a weapon whose numbers are not balanced should say so IN ITS OWN FILE, where someone
reading it to derive a number will see it.**

**Proposed remedy — operator's to rule, and deliberately small:** an optional `tuning:` key with two
values, `dev` and `balanced`, and **a one-line boot inventory naming every weapon that carries
neither.** Unmarked is the dangerous state precisely because it is silent, so unmarked is what gets
reported. `ironblade` and `hunters_bow` are marked `dev`; **the Boltor is the first `balanced`.**
A prose header line alone would be the same class of thing that failed — unenforceable and invisible
at the point of use.

---

## RANGE 96 TOUCHES A STANDING RULING, AND THE DISTINCTION MATTERS

**96 is three times the measured 32-block client particle cap** (`GATE-volley` V3) and the longest
range in the project by half again — the authored set is `23, 26, 30, 32, 64`, and only
`volley_stone`'s 64 exceeds the cap at all, as a deliberate grammar-limit fixture.

**`cursed_emerald` was ruled DOWN from 64 to 32 on exactly this**, with the ruling stated as
*"VISIBLE REACH EQUALS REAL REACH"* and the harm named as *"the ray hitting a target the beam never
reaches."*

> **THE RULING'S HARM DOES NOT OCCUR AT 96, AND THAT IS WHY THE TWO CAN COEXIST.** Particles are
> emitted server-side along the **whole** ray; what is capped is each **client's** render distance.
> So the shooter renders the first ~32 blocks and **the target renders the last ~32 — the beam does
> reach them.** What no single observer sees is the middle third.
>
> The earlier carry-forward flattened this into *"a ray past 32 blocks draws only its near half"*,
> which is a shooter-relative observation stated as a property of the ray. **The operator caught
> that; it is corrected here and in `boltor.yml`, and `cursed_emerald.yml` should gain a
> cross-reference** so the next reader does not read 96 as an oversight against a ruling that is
> still good.

**Consequence for the gate: the beam-density row needs an observer AT DISTANCE, not only the
shooter.** Staged from the muzzle alone it measures a third of what is drawn — and the densest
third, at that.

---

## WHAT SLICE B INHERITS

- **The hitscan reversal is KNOWING, and it is the SECOND reversal on this weapon.** The old repo
  moved *off* hitscan deliberately, to keep the bolt *"a real, dodgeable arrow"*. The operator is
  reversing that knowingly: the new Boltor is `CastSpec.Ray`. **Write both reversals; do not let the
  second delete the first.** Not carried: the arrow, the velocity, ammo-type mirroring, tipped
  arrows, pickup status.
- **Flight time is a FEATURE, and it is aim-dependent.** Segment 0 runs inline; every later chunk
  column costs a tick. At 96: **vertical 0 ticks (still hitscan), axis-aligned 5-6, 45° 8-10** —
  0.25-0.50s from click to damage. State it as designed, not as a limitation discovered.
- **Bolts OVERLAP in flight**, by design — 16t between shots against a 5-10 tick walk. Nothing in the
  schema warns; `CastSpec.minimumCooldownTicks` returns **0** for `Ray`.
- **No pierce needs NO code.** `stepRay` returns unconditionally on the first hit, and its javadoc
  names the already-hit-id set as the work pierce *would* require. The set stays unwritten.
- **The i-frame apparatus is NOT ported**, and it was checked rather than assumed: custom HP drains
  through `stats().damage(...)` and the vanilla hit is cosmetic. Carry the note, or the next reader
  re-derives it from the old repo's architecture.
- **The main-hand guard stays.** Two repos reached it independently. An off-hand-only Boltor would
  not fire at all under it — **slice C states which behaviour ships; B must not decide it by
  omission.**
- **A Boltor WEARS and EMPTIES.** `material: crossbow` has vanilla durability, so two independent
  per-item counters decrement per shot and both route to a notice. **Which message wins is settled
  from code order, not from a boot:** `WeaponFire.attempt` checks the broken gate before the quiver
  gate, and `RpgListeners` handles `Broken` before `Empty`, so a broken Boltor says broken even with
  an empty magazine.
- **A quiver weapon claims left-click**, so the Boltor binds no `left_click` trigger — enforced by
  `WeaponDefinition`, which rejects a weapon declaring both.
- **The Boltor is the load test for `BEAM_ORIGIN_GAP`**, adopted at 1.0 on a blanket-answered ruling
  and **never compared against 0.5 or 1.5**. The beam-density row is needed **from the start**.
- **An unmeasured cost, named rather than fixed:** `presentAlong` schedules its region hop **before**
  `BeamSamples` returns empty, so every suppressed segment costs a no-op hop — *"nobody has timed
  it."* A held Boltor at 96 is **6-7 hops axis-aligned, 9-11 at 45°, per shot, at 1.25 shots/s**, and
  **beyond 32 blocks those hops do full work no client renders.** This slice is its first real load.

---

## COMMIT SPLIT

| # | commit | what |
|---|---|---|
| **1** | `boltor.yml` + `boltor_beam.yml`, fully ruled | all six numbers, the reversal note, wears-and-empties, no `left_click`; the beam, because a 96-block ray must not tune the Lapis Staff's appearance |
| **2** | `GATE-boltor.md` | **the boundary row and only that** — see below |
| **3** | the elapsed figure | Q7's owed control: ticks between the single input and the command, on the `NO_REPEAT` arm only — **not** by changing `window` |
| **4** | `tuning:` marking | if ruled: the key, the boot inventory, `dev` on the two, `balanced` on the Boltor |
| **5** | prose + the density row | `cursed_emerald` cross-reference, the carry-forward corrections |

**`range` is unvalidated — `CastSpec.Ray` has no compact constructor and `ContentValidator` checks
only `beam`.** Negative, zero and NaN are all representable. Worth a guard, but it is **not** this
slice's job to add one silently; it is named as owed, in `boltor.yml` at the key.

### GATE-boltor.md's FIRST ROW IS THE BOUNDARY READING, AND ONLY THAT

**16 is a multiple of 4 — the one residue class neither Q7 reading covered.** 11 and 15 both resolved
by rounding up to a strictly larger input, so the boundary never arose.

```
inputs at t = 0, 4, 8, 12, 16 …    fire at 0, cooldown 16 expires at t = 16
  elapsed >= cooldown  ->  fires at 16
  elapsed >  cooldown  ->  16 missed, next input at 20  ->  FIRES AT 20
```

**RECORDED BEFORE THE READING:** `CooldownTracker.isReady` is `currentTick >= ready` and `trigger`
stamps `now + cooldownTicks`, so **the source predicts 16** — and that model is **2-for-2** on the
measured points (11→12, 15→16). **It is still a prediction**: those two could not distinguish this
case, and Q7 exists because a 15-tick cooldown did not produce a 15-tick rate. **Read the source,
then measure it.**

**Expected shape, written in advance:** `FIRES min 16, mean 16.00, zero variance` — the
`quiver_stone` signature, since the Boltor's inputs should be periodic on the same grid.

> **If it reads 20, the grid is strictly-greater and the honest multiples shift** — 16 would become
> 20, and the multiples-of-8 rule becomes multiples-of-8-minus-one. **Nothing downstream is computed
> from 16 until this row confirms it.**

**THE ROW HAS A PLAYER-FACING CONSEQUENCE, NOT ONLY A BALANCE ONE.** The tooltip renders
`String.format("%.1f", 20.0 / 16)` = **"Attack Speed: 1.3"**, which is what `golden-lore.txt` now
records. On the `>=` branch the weapon delivers 1.25/s and the tooltip rounds to 1.3; on the `>`
branch it delivers 1.00/s and the tooltip **overstates it by 30%**. Separately, and true on either
branch: `20.0/15` and `20.0/16` both format to `"1.3"`, so **the tooltip cannot distinguish a
15-tick weapon from a 16-tick one** — `hunters_bow` and the Boltor render the identical line.

**And the general authoring rule goes beside the quantisation note at `cooldown_ticks`, not in this
gate:** under held fire the useful values are **multiples of 4**; for anything that will ever be
dual-wielded, **multiples of 8**.

---

## VERIFICATION

- `./mvnw -pl core test` per edit; `./mvnw clean package` as the final verify, suite quoted
  `core / storage / paper` and **re-read after the last file lands**.
- **`golden-lore.txt` WILL move** — a new weapon adds tooltip lines. That is the one expected golden
  change in this slice; regenerating it in any other commit is the tell that something else moved.
- Content tests: the loader's unknown-key guard stays quiet on `boltor.yml`; `ContentValidator` clean.
- **Mutations** on anything new, each spliced by line number, marker grepped both directions, byte
  delta derivable, restored from a scratchpad copy, **and the region printed back afterwards.**
- **Boot-only and recorded as such:** the boundary reading, the beam-density row at distance, and
  wears-and-empties.

## OUT OF SCOPE

Dual-wield, alternation, and the off-hand-only ruling — **slice C**, stated there rather than
falling out of B. `CastSpec.Volley`'s non-atomicity is C's too.
