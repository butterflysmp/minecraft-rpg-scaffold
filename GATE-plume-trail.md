# GATE — the Dragon's Plume purple trail

**Status: NOT RUN.** Every prediction below was written **before** any boot, and no row has been
read. When a row is read, the reading goes **beside** its prediction, never over it, and the
prediction is not edited afterwards.

**Branch:** `feat/plume-trail`, off `3ae97ff`.
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
