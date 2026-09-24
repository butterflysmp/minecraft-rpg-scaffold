# GATE — the Dragon's Plume purple trail

**Status: NOT RUN.** Every prediction below was written **before** any boot, and no row has been
read. When a row is read, the reading goes **beside** its prediction, never over it, and the
prediction is not edited afterwards.

**Branch:** `feat/plume-trail`, off `3ae97ff`.

> ### *** THE TIP MOVED BEFORE ANY ROW WAS READ, AND IT MOVED THE THING THIS GATE MEASURES ***
>
> **R14 (tap speeds 0.5 / 1.3 / 2.2) and R15 (proposed tap cooldown 8) landed on this branch on
> 2026-09-24, after every prediction below was written and before any of them was read.** Nothing
> here has a reading, so nothing is being overwritten — but **two rows and one question were written
> against `speed: 2.5` on every binding, and that is no longer true of three of the four.**
>
> | row | written assuming | still true? |
> |---|---|---|
> | **R1** | band 1 at speed 2.5: *"starting about 2.5 blocks from your eye"*, *"one mote every 2.5 blocks"* | **NO.** Band 1 is now **0.5** — first mote **0.5 blocks** out, and **one mote every 0.5 blocks**, five times denser. |
> | **R3** | nothing is drawn at the eye | **AT RISK, AND THIS IS THE ONE TO WATCH.** The guard is `elapsed > 0`, which is unchanged — but the first mote it *does* draw is now **0.5 blocks from the eye** instead of 2.5. If R3 fails on band 1 and passes on a charged shot, **the guard is fine and the distance is the cause**, which is a different finding entirely. |
> | **R2**, **R5** | full charge at 2.5 | **YES.** `draw:` is untouched, so R11's ~300-block reach and the 595-mote figure stand. |
> | **Q1a** | *"At speed 2.5 there is one mote every ~2.5 blocks"* | **NO for the taps.** And see the note under Q1a: lowering `speed` was listed there as a **balance** change and *"therefore not a trail decision at all"* — **R14 has now made that change for balance reasons, and it lands on the dotted question by accident.** A band-1 tap is the densest trail in the project. |
>
> **THE PREDICTIONS ARE NOT EDITED.** They are correct about the build they were written for, and
> rewriting them to match a tip that moved underneath them would destroy the record that the tip
> moved. **The T rows below are the same questions asked of the new speeds**, and R1/Q1a are read as
> history against `dee262c`.
**Declared game mode: CREATIVE.** See *GAME MODE* below — it is declared because a gate that does
not say costs readings, and two of this file's rows would otherwise be unreadable.

**What is NOT being asked.** Whether a DUST step's colour and size arrive at all: they do, measured,
and `dragons_plume_trail.yml` records it. Two shipped visuals (`emerald_impact`, `lapis_impact`)
already author DUST on the same `present` port a trail uses. **This gate is about APPEARANCE.**

---

## R0 — THE DEPLOYED BUILD CARRIES THIS SLICE. If R0 fails, STOP; no other row is readable.

**Run `./scripts/dev-server.sh --refresh-content`, not the bare script.** This is not optional and
it is not tidiness:

> ### *** WITHOUT `--refresh-content` EVERY ROW BELOW READS "NO TRAIL", AND THE CAUSE LOOKS LIKE THE VISUAL ***
>
> The plugin ships content with `saveResource(path, false)`, which **never overwrites an existing
> file**. `run/plugins/Rpg/content/weapons/dragons_plume.yml` already exists from earlier boots and
> **has no `trail:` key in it.** So a normal boot loads the STALE weapon, four bindings are silently
> absent, and the new visual — which *is* copied, being new — sits in the folder doing nothing.
>
> **The failure is a correct-looking absence.** Nothing warns: an unreferenced visual is not an
> error, and a weapon with no trail is a legal weapon. `dev-server.sh`'s own comment records this
> trap stranding a re-elementing pass once already.

**R0 rows are PowerShell**, because that is the shell they get run in. No `unzip`, no `javap`.

| # | prediction | instrument | READING |
|---|---|---|---|
| **R0a** | the build line names this branch's tip | `Select-String -Path run\logs\latest.log -Pattern '\[Rpg\] Build:' \| Select-Object -First 1` | _(not run)_ |
| **R0b** | **4** — the deployed weapon carries all four bindings | `(Select-String -Path run\plugins\Rpg\content\weapons\dragons_plume.yml -Pattern 'trail: dragons_plume_trail').Count` | _(not run)_ |
| **R0c** | the deployed visual exists and is this one: `color: [60, 20, 90]`, `size: 0.6` | `Select-String -Path run\plugins\Rpg\content\visuals\dragons_plume_trail.yml -Pattern 'color:|size:|count:'` | _(not run)_ |
| **R0d** | **no** boot warning naming `dragons_plume_trail` or an unknown visual | `Select-String -Path run\logs\latest.log -Pattern 'dragons_plume_trail\|Unknown visual'` | _(not run)_ |

**R0b is the row that catches the stale-content trap**, and it is the reason R0 is four rows rather
than one: R0a can pass on a correct jar whose *deployed content folder* is a month old.

**R0d's expected reading is EMPTY, which is a reading and not an absence of one.** A trail id that
resolves warns nothing; `ContentValidator` reports a trail naming no visual as a named problem, so an
empty result plus a passing R0b is the pair that means "bound", and either alone is not.

---

## THE ROWS

Fire from level ground with sky behind the shot where possible — a dark-violet mote against dark
terrain is the hardest case and is **not** what row 1 is asking about.

| # | what to do | PREDICTION, written before the boot | READING |
|---|---|---|---|
| **R1** | Tap the bow: hold **3–8 ticks** and release (band 1). | ONE arrow. A faint dark-violet line of single motes behind it, **starting about 2.5 blocks from your eye** and trailing back. Sparse — roughly one mote every 2.5 blocks. | _(not run)_ |
| **R2** | Hold **20+ ticks** and release at full charge. | **FIVE** arrows, each with its own line, and they **converge** as the homing pulls them onto a target. Five lines is ~5 motes per tick in the air. | _(not run)_ |
| **R3** | On both shots, watch the space **directly in front of your own face** on the release frame. | **NOTHING is drawn at the eye.** No mote inside the camera, on any shot, at any charge. | _(not run)_ |
| **R4** | Follow one arrow in flight and watch where the motes sit relative to the shaft. | The motes sit **on the arrow's path**, not offset from it and not lagging behind by more than one step. | _(not run)_ |
| **R5** | Let a charged release fly at nothing, into open sky, and watch it to the end. | The line keeps drawing for the **whole** flight — up to 120 ticks, ~6 seconds — and then stops. It does not thin out or stop early. | _(not run)_ |
| **R6** | Fire a tap **through** a target you hit at close range. | The line stops **at the hit**, not past it. | _(not run)_ |

**R3 is the control for the launch-frame skip**, and it is the one row whose prediction is a
NEGATIVE. `ProjectileFlight.step` guards the trail with `elapsed > 0`, and `ProjectileFlightTest`
pins it for an arrow-bodied projectile — so a failure here means the guard was lost in a way the
suite can see, and the suite should be run before anyone looks further.

**R5 exists because the shot total is the figure nobody has watched.** 119 motes per bolt, 595 at
full charge — the largest of any shot in the project. If it reads as too much, that is a finding
about `max_lifetime_ticks` meeting a per-tick trail, not about the colour.

---

## THE QUESTION ROW — BEN'S EYE, AND NOTHING IS BUILT UNTIL HE ANSWERS

**Q1 is not a pass/fail row.** It is the reading the whole slice exists to get, and the numbers were
authored from arithmetic precisely so this question could be asked of something real.

| # | the question | the options, so the answer is a pick rather than an essay | ANSWER |
|---|---|---|---|
| **Q1a** | **Dotted or continuous?** At speed 2.5 there is one mote every ~2.5 blocks. | (a) reads as a **line** — leave it. (b) reads as **dots** — and dots are wrong. (c) reads as dots and that is **fine**. | _(not run)_ |
| **Q1b** | **Too faint, right, or too strong?** `size 0.6` is below every other authored size in the project. | (a) **invisible** — raise size. (b) **faint, right**. (c) **too strong** — lower it. | _(not run)_ |
| **Q1c** | **The colour**: does `[60, 20, 90]` read as void-purple, or as something else? | (a) right. (b) too dark to read as purple at all. (c) wrong hue. | _(not run)_ |
| **Q1d** | **Five converging lines**: legible as five, or one smear? | (a) five. (b) a smear. | _(not run)_ |

> ### *** NO INTERPOLATION HAS BEEN BUILT, AND THAT IS DELIBERATE RATHER THAN AN OMISSION ***
>
> If Q1a answers **(b)**, the fix is drawing more than one mote per tick along the step — sub-stepping
> the trail the way `BeamSamples` samples a beam. **That is a code change to the flight, not a number
> in this file**, and it has not been designed, let alone written. The brief said no interpolation
> unprompted, so the dotted question is asked here and the answer waits for Ben.
>
> **The cheap non-code answers, in the order they cost least**, so the ruling has options: raise
> `size` (a bigger mote closes a gap visually without closing it physically), or lower the weapon's
> `speed` — which is a **balance** change and is therefore not a trail decision at all.
>
> > **AND THAT LAST SENTENCE WAS OVERTAKEN BY EVENTS ON 2026-09-24, WHICH IS WHY IT IS LEFT
> > STANDING.** R14 lowered the tap speeds **for balance**, exactly as this note said such a change
> > would be — and it therefore changed the trail density on three of four bindings without any trail
> > decision being taken. **The note was right that it is not a trail decision. It was wrong to imply
> > that made it irrelevant to this gate.** Read Q1a on `draw:` and **T3** on the taps.

---

## THE ROWS — R14 / R15, THE TAP SPEED SPLIT AND THE PROPOSED COOLDOWN

**Ben's ruling, 2026-09-24, recorded as his:** tap speeds follow vanilla's bow-power curve —
`tap1 0.5`, `tap2 1.3`, `tap3 2.2` — and `draw:` stays `2.5`, so R11's ruled ~300-block reach is
untouched. **The cooldown is a PROPOSAL, not a ruling**, and `Q-CD` is where Ben settles it.

**Predictions computed, not estimated.** `ProjectileFlight` integrates `pos += v` then `v.y -= g`
with **no drag**, so the time to fall an eye height is **9 ticks at every speed** and flat reach is
exactly `speed × 9`. Controlled against the figure already in `ProjectileFlight`'s own javadoc —
*"a flat stray lands about 22.5 blocks"* at speed 2.5 — which the same simulation reproduces.

| # | what to do | PREDICTION | READING |
|---|---|---|---|
| **T1** | Stand on flat ground, aim level at the horizon, and fire one tap in **each** band, then a full charge. Pace out where each lands. | Four clearly different distances: **~4.5 / ~11.7 / ~19.8 / ~22.5 blocks.** Band 1 lands **almost at your feet**; band 3 lands close to the charged shot. | _(not run)_ |
| **T2** | Fire a band-1 tap and a full charge at the same distant target and watch the arrows. | The tap is **visibly slower and visibly droopier** — 1/5 the speed, so it takes 5× as long to cover the same ground and falls the same amount in that time. The difference is obvious without measuring. | _(not run)_ |
| **T3** | Fire one tap in each band and compare the trail **density** against a charged shot. | **Band 1's trail is the densest in the project** — 2.0 motes per block against the charged shot's 0.40, five times as many. Band 2 is 0.77, band 3 is 0.45. If Q1a answered *"reads as dots"* for the charged shot, **band 1 should read as a solid line.** | _(not run)_ |
| **T4** | **THE R3 RE-READ AT THE NEW SPEED.** Fire a band-1 tap and watch the space directly in front of your face on the release frame. | **Still nothing drawn at the eye.** The `elapsed > 0` guard is unchanged, so the first mote is at 0.5 blocks rather than at the eye. **If a mote appears inside the camera here but not on a charged shot, the guard is intact and 0.5 blocks is simply too close** — that is a finding about the speed, not about the trail code. | _(not run)_ |
| **T5** | Spam band-1 taps as fast as you can click. | **Roughly 2.5 shots a second, not more** — one per 8 ticks. Before R15 this was unbounded. | _(not run)_ |
| **T6** | Alternate bands deliberately: a band-1 tap, then band-2, then band-3, as fast as you can. | **SLOWER than spamming band 1**, not faster. The three bands are on three separate cooldown buckets, so the timers do not gate each other — but cycling them costs at least `3 + 9 + 15 = 27` ticks of **holding**, against 8 ticks per shot for band-1 spam. The evasion exists and does not pay. | _(not run)_ |
| **T7** | Hold the Plume and read its tooltip. | **Three new lines, `"Cooldown: 0.4s"` in dark grey, one under each of Tap1 / Tap2 / Tap3.** The tooltip now says the same thing three times. **This is MEASURED, not predicted**: `GoldenLoreTest` went red and the regenerated golden is exactly `+3 / -0`, all three that line. Before R15 a `0` rendered nothing at all — `cadenceLine` drops a free, instant trigger. | _(not run)_ |

> ### *** T6 IS A ROW ABOUT A KNOWN HOLE, AND IT IS WRITTEN TO CONFIRM THE HOLE IS NOT WORTH USING ***
>
> `dragons_plume.yml` has carried this warning since the bands were built, while all three cooldowns
> were `0` and the property was inert: *"the moment anyone authors a non-zero cooldown on a tap they
> will be authoring one third of the gate they think they are."* **R15 is that moment.**
>
> **The prediction is that it does not matter AT THIS VALUE**, and that is a claim about `8`, not
> about the design. It stops holding if the cooldown rises past roughly the band-cycle cost. **If
> Ben rules a larger value, one shared bucket becomes the right shape — and that is a code change in
> `PlumeDraw`, not a content one.**

| # | the question | the options | ANSWER |
|---|---|---|---|
| **Q-CD** | **The tap cooldown: too slow, right, or still spammable?** `8` is proposed. It gates **band 1 only** — bands 2 and 3 cost more than 8 ticks just to hold, so the timer is spent before they can fire. | (a) **right** — leave 8. (b) **still spammable** — raise to 12 or 20, and read T6 again, because alternation starts to pay above ~27. (c) **too slow** — drop to 4, which is near-inert, or back to 0. | _(not run)_ |
| **Q-TT** | **Three identical `"Cooldown: 0.4s"` lines on one tooltip — acceptable?** A consequence of the bands being three bindings, not of the value. | (a) **fine** — leave it. (b) **noisy** — the taps want one shared label, which is a tooltip change. (c) it is the reason to keep the cooldown at 0. | _(not run)_ |
| **Q-SP** | **The tap speeds: does band 1 at 4.5 blocks still do its job?** R4''' exists so the weapon is *"never dead at close range"*. 4.5 blocks is barely past melee. | (a) **right** — a floor band that only works in your face. (b) **too short** — raise `tap1` speed. (c) the whole split is wrong. | _(not run)_ |

> ### *** THE DERIVATION DOES NOT REPRODUCE TWO OF THE THREE RULED VALUES, AND THAT IS OPEN ***
>
> The stated basis is the curve at each band's midpoint × 2.5. Computed:
>
> | band | midpoint `t` | curve × 2.5 | BEN RULED | agrees? |
> |---|---|---|---|---|
> | 1 | 5.5 | **0.5214** | 0.5 | **yes** |
> | 2 | 11.5 | **1.2339** | 1.3 | no |
> | 3 | 17.0 | **2.0188** | 2.2 | no |
>
> Inverted, the ruled values sit at `t = 5.30`, `t = 12.00` and `t = 18.16`. Half-open midpoints
> (`3..9, 9..15, 15..20` → `t = 6, 12, 17.5`) reproduce **tap2 exactly** and miss the other two.
> **No single midpoint convention gives all three.**
>
> **THE VALUES SHIP AS RULED AND NOTHING IS BLOCKED.** What is open is the recorded REASON. It is
> written down rather than resolved by quietly rounding `1.2339` up to `1.3`, because a derivation
> that does not reproduce its own rows is the defect `CLAUDE.md` names. Full arithmetic, including
> the curve at every tick a tap can occupy, is in `dragons_plume.yml`'s R14 block.

---

## GAME MODE — CREATIVE, and what that buys and costs

**Declared because the creative-divergence register says an undeclared gate costs readings.**

| what creative changes | does it reach these rows? |
|---|---|
| `hasInfiniteMaterials()` short-circuits `BowItem.use`'s ammunition check, so the draw always starts and the arrow is never consumed | **No row here depends on ammunition.** It is what makes the session practical: R2 needs full-charge releases repeatedly, and each is five rounds of a 25-round magazine in survival — five releases and you are reloading. |
| the quiver never depletes | Helps R5, which needs an unobstructed six-second flight watched to the end, possibly several times. |

**NOTHING in this file is read in survival, so nothing here certifies survival.** The trail is drawn
by `ProjectileFlight`, which creative does not touch, so the divergence is about *getting* shots off
rather than about what a shot looks like. **That claim is reasoned, not measured** — it is stated as
the reason the mode was chosen, and if a row surprises, the mode is the first thing to re-test.
