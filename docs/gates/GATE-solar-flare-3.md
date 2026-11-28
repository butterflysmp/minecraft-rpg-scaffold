# GATE — SOLAR FLARE'S BURST ALL ORANGE, WITH A DEEP BOOM (THE SEAT'S V2, V3), ONE ROW, ONE ASK (2026-11-28)

**Status: NOT RUN.** Built on `feat/ultimates-3`, from master `2386162e`. The account of the change is
`docs/plans/PLAN-solar-flare.md` section 7 (V2, V3). The scenario is
`paper/src/main/resources/selftest/GATE-solar-flare-3.yml`. **Every prediction below was written before any boot.** `GATE-solar-flare-2.md` and its
READINGS are not edited. The queue, the lint, the one ask, the pick, the staging and the three view steps are held by
`UltimatesThreeGateCommandTest`; the burst's file by `SolarFlareContentTest`.

**GAME MODE: SURVIVAL.**

## WHAT BEN TYPES — THE ONE COMMAND IS `GATE-smolder-3.md`'s

**This is the pointer; the account (the command, typing either gate alone, riding the next piece's boot, the two
questions, the builds, how long) is `GATE-smolder-3.md`'s WHAT BEN TYPES:** `/rpg selftest smolder-3+solar-flare-3`.
This gate runs second; SF12 is the command's last step. Alone: `/rpg selftest solar-flare-3`.

## CARRIED STATE — `GATE-smolder-3.md`'s, FOR THE WHOLE COMMAND

**This is the pointer; the booter's checklist, R0 and the SAFETY sum (0.000) are `GATE-smolder-3.md`'s,** and hold when
this gate is typed alone. This gate adds nothing to the carried state: it places no block and summons no mob, and the run
puts back his builds (SF12's pick).

## THE CLOCK AND THE STAND

`GATE-solar-flare-2.md`'s THE CLOCK and its coordinate table hold unchanged: **shot 1 at C = P + 1, shot 2 at C + 10,
shot 3 at C + 20**, his view set one tick before each (P − 1, P + 10, P + 20). He stands at **H = F + (5, 0)**,
block-centred, facing east; with nothing in the way each shot bursts at **32.0** blocks on its launch tick + 15, at
**H + (32, 1.620, 0)**, and every burst's 6 blocks lie inside the interior and this gate's preflight `-2 -13 45 13`
(x 31 .. 43 from F).

## WHAT THE BURST NOW IS (V2, V3; `solar_flare_burst.yml`)

| step | before (read on `4f26ca31`) | now |
|---|---|---|
| the ball | 90 TRAIL `[255, 120, 0]`, out to 6.0 in 10 ticks | unchanged |
| the inner ball | 60 TRAIL **`[255, 220, 40]` (yellow)**, out to 4.5 in 10 ticks | 60 TRAIL **`[255, 120, 0]`**, out to 4.5 in 10 ticks |
| the sparks | 40 **FIREWORK (white)**, spread 0.2, speed 0.3 | 40 TRAIL **`[255, 120, 0]`**, out to **5.5** in **20** ticks |
| the sound | the firework's large blast and its twinkle, volume 3.0, pitch 1.0 | **one** `entity.generic.explode`, volume **3.0**, pitch **0.7** |

- **Why the sparks are a TRAIL now (READ, Ben's unobfuscated 26.1.2 client):** `ParticleTypes` registers `firework` as a
  `SimpleParticleType` (no options travel with its packet), and `FireworkParticles$SparkProvider.createParticle` sets
  only the spark's alpha, never a colour. A spark CANNOT be coloured from the server. **CC'S CALL:** an orange TRAIL
  burst, which the server colours and times (its lifetime is exactly `duration_ticks`, READ).
- **The throw it keeps, EXECUTED** over 200000 sparks on the READ rules (a velocity of 0.3 x a gaussian on each axis,
  `ClientPacketListener.handleParticleEvent`; friction 0.91, `SimpleAnimatedParticle`; 48 to 59 ticks, `SparkParticle`;
  `Particle.tick`'s order): **5.57 blocks on average** (median 5.37), half of it in 7 ticks, about 86% in 20. The TRAIL
  goes out to 5.5 at a steady speed and is gone at 20 ticks; a spark slowed and lingered to 59.
- **The sound (READ):** `entity.generic.explode` is `SoundEvents.GENERIC_EXPLODE` on the pinned 26.1.2 server jar;
  sounds.json (Ben's asset index 30) gives it no `attenuation_distance`, so 16; **reach 16 x 3.0 = 48** (the server's
  send and the client's hearing), past the 32.04 he is from a range burst; pitch 0.7 inside the client's [0.5, 2.0] clamp
  (`SoundEngine.calculatePitch`). **Three bursts 10 ticks apart play it three times: intended** (the seat's V3). How loud
  a volume above 1 plays was not READ.

## THE ROW

### SF12 — THE PICK, THEN THE ASK (the command's last step)

- **The pick, as SF1R:** `rpg playerxp`, `clear_inventory`, `clear_state`, `/build`, Class, Ranger, Element, Fire, `Q: `,
  Solar Flare, close (SF1R's clicks and waits; its menu dumps left out). Then **`CELL class=ranger element=fire`**; any
  other cell stops the row, `SKIPPED`, before any cast.
- **The staging, as SF9R, step for step:** the stand at H, `stone`, `hold`, the mana refilled, the tell line, his view
  set at P − 1, the press at P, the view set again at P + 10 and P + 20.
- **`DRIVE drop … yaw=-90.000 pitch=0.000`.** **`SOLAR_FLARE cast … tick=C`** (the witness that his Q is Solar Flare).
  **`SOLAR_FLARE shot n=1 dir=-90.000 0.000 tick=C`**, **`n=2 … tick=C+10`**, **`n=3 … tick=C+20`** (each pitch `0.000`
  or `-0.000`; the sign of a zero is not predicted).
- **`SOLAR_FLARE burst n=1 cause=range origin=<H + (32, 1.620, 0)> tick=C+15`**, **`n=2 … tick=C+25`**, **`n=3 …
  tick=C+35`**: one origin, three times. **No `SOLAR_FLARE hit` line** (no body is staged).
- The tell line: "Solar Flare is cast in a moment, east down the pad. Keep your mouse still and stand still; watch the
  fireballs leave and where each one bursts."
- **The question at P + 60 (C + 59)**, after the third burst's longest-lived particles (the sparks, C + 35 to C + 55).
  SEAT PLACEHOLDER 2026-11-28 wording: **"Was each burst all orange, with a deep boom in place of the firework sound?
  (yes / no / not sure)"**. Answer logged verbatim; no answer is NOT STATED.

## CC'S CALLS (this gate's)

`GATE-smolder-3.md`'s four, and: **the sparks as an orange TRAIL burst, 40, to 5.5 in 20 ticks** (the brief's
alternative, DUST, lives `max(1, (int)(8 / (r x 0.8 + 0.2)) x size)` ticks, READ in M3: a random lifetime the server
cannot set; and the radius is the executed mean throw, rounded to the half block, below the burst's own 6).

## OPEN FOR BEN

- **V2's look:** the inner ball orange; the sparks as an orange burst to 5.5 in 20 ticks (SEAT PLACEHOLDERS).
- **V3's sound:** `entity.generic.explode`, pitch 0.7, volume 3.0 (SEAT PLACEHOLDER).
- SF12's question wording (SEAT PLACEHOLDER 2026-11-28).

## READINGS

None yet.
