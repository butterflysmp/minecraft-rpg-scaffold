# GATE — MINING INPUT SPIKE: how a hit on a weak spot is detected (candidates A and B), and the pack

**Status: READ 2026-11-12 at build `1dbcc156`** (one boot, 21:17:52 to 21:36:29, `--refresh-content`). R0 3/3
PASS. MS6 PASS. MS1, MS2, MS3, MS4, MS5 and MS7 read; each one's departures from its prediction are named in its
READING. The predictions below are unchanged. **The seat has not yet read these readings.** Original status: written
2026-11-12, before any boot of this branch. **NO BOOT until the seat has diffed the spike code AND this gate and says
so, and Ben says ready.**

**This branch, `spike/mining-input`, does NOT merge.** The gate lives here because the boot is bound to this branch's
build sha. After its readings are committed here, this file is copied **byte for byte** onto `docs/mining-plan`, with
a dated note naming the spike sha it was read at. This branch is not deleted until the seat has confirmed the two
blobs diff empty (the seat's ruling, 2026-11-12).

Every prediction below was written **before** any row was read, and **this file is COMMITTED before the boot**.
Readings go **beside** a prediction, never over it. A prediction is not edited once its row has been read. Readings
count only if Ben's LOGIN appears in THIS boot's log.

**Plan:** `docs/plans/PLAN-mining.md` on `docs/mining-plan` (§3 holds the facts F1–F6 and F2b, and the override
reading; §4 compares A and B). Every "from the jar" prediction below cites one of those facts. **Where the jar cannot
answer, because the question is about client behaviour, the row is a MEASUREMENT.** It says so, lists every outcome,
and says what each outcome means for A and for B.

```
ROWS 10   R0a R0b R0c / MS1 MS2 MS3 MS4 MS5 MS6 MS7   = 3 R0 + 7 MS
          git grep -c -E '^### (R0|MS)' <ref> -- docs/gates/GATE-mining-spike.md
```

## GAME MODE

**SURVIVAL on every row.**
- **ADVENTURE** never reaches `BlockDamageEvent` (PLAN F2), so it would test a different mechanism.
- **CREATIVE** breaks a block at `handleBlockBreakAction` 256, before `BlockDamageEvent` (F2b). Creative also removes
  costs (the creative-divergence register).

**Confirm with `/gamemode survival` before R0's set-up is complete.** A row read in any other mode is VOID.

## CARRIED STATE, READ BEFORE BOOTING

- **The previous server session (2026-11-10, the stack build `61e722f2`) ended WITHOUT `/stop`** (`GATE-deft-hands.md`,
  the stop). Its profile and vault flush is unwitnessed, and the world on disk is whatever the last autosave wrote.
- Ben's profile was last written by the stack build, which may name abilities this branch (master + the spike) does
  not have, e.g. `sunder`. **A profile warning at LOGIN is recorded, not an R0 failure.** R0c reads the `Loaded` line
  at enable, before LOGIN.
- `run/server.properties` has `require-resource-pack=false` and an empty `resource-pack` (L55–59). This gate does not
  change them.

> **DATED NOTE, 2026-11-12, recorded at the boot of `1dbcc156` (a note, not a prediction; no prediction moved).**
> - **The deployed content was the parked STACK's.** `run/plugins/Rpg/content` held 114 files. The previous boot's
>   log (stack build `61e722f2`) read "16 weapons", because `saveResource(false)` never overwrites. Against master it
>   had 8 extra files (`sunder.yml`, `builds/melee.yml`, `deft_hands.yml`, `withering.yml`, `blaze_kings_staff.yml`,
>   `withered_shortbow.yml` and two blaze visuals), 4 changed files (`melee_fire.yml`, `fire.yml`, `wither.yml`,
>   `expanded_quiver.yml`) and lacked 1 (`active_placeholder_melee_thrust.yml`). **By the seat's ruling this boot
>   ran `./scripts/dev-server.sh --refresh-content`**, which cleared it. The folder was re-copied from this jar with
>   107 files.
> - **Backup, taken before the boot:** `C:\Users\Neb91\IdeaProjects\rpg-run-backups\2026-11-12-pre-mining-spike`,
>   holding `plugins/Rpg` (119 files, 360,149 bytes) and `world` (193 files, 204,804,419 bytes). Source and copy
>   are equal by count, by bytes and by `diff -rq`. No `java.exe` ran before or after the copy.
> - **THE NEXT BOOT OF THE PARKED STACK MUST ALSO USE `--refresh-content`.** Otherwise it runs the stack's code on
>   master's copies of `melee_fire.yml`, `fire.yml`, `wither.yml` and `expanded_quiver.yml`, and without its 8
>   added files.
> - **Ben's saved build names `sunder`, which this build does not have.** It was not read at LOGIN: there are three
>   LOGINs (L80 21:22:52, L833 21:31:48, L841 21:32:13), and none of them produced any `[Rpg]` warning. The pre-boot
>   code reading (the seat's step 2): `FileBuildRepository.load` and `BuildMigrations.migrate` check structure only.
>   The build file is written only by `BuildPickerMenu`, which Ben was told not to open.
> - **Ben's client connected from `192.168.68.95`** (this machine's LAN address), and it fetched the pack from
>   `127.0.0.1` (MS7), so the client runs on the server's machine. A mid-boot warning to Ben that the download would
>   fail was wrong.

## THE TEST BLOCK: `minecraft:deepslate`, AND WHY

- **It has no `attack` behaviour.** A cancelled survival dig still runs `BlockState.attack` before
  `BlockDamageEvent` (F2). `javap -p` shows `RedStoneOreBlock` and `NoteBlock` override `attack`, so they would light
  or play on every click. Deepslate is `RotatedPillarBlock` (`Blocks`: the bootstrap is `RotatedPillarBlock."<init>"`),
  and that class and `Block` declare no `attack`.
- **It is mineable by an iron pickaxe, needs the right tool for drops, and is not insta-mined** (`strength(3.0f,
  6.0f)`, `requiresCorrectToolForDrops`). So a client that predicts a local break needs a real dig time to do it, and
  MS3 and MS4 have a window to see.
- **It is not the surrounding terrain.** A ghost break leaves an unmistakable hole in a single placed block.
- **It must be FRESH:** placed by the set-up and never dug before the spike is active. F2b: a STOP can break a block
  that an earlier UNCANCELLED dig left in `destroyPos`.

## THE ORDER IN THE ONE BOOT

R0 → set-up → MS1 → MS4 (read during MS1 and MS3) → MS3 → MS2 → MS6 (while MS2's box is up) → MS5 → MS7 →
LAST STEP (`/stop`).

## Set-up (after R0 passes)

1. `/gamemode survival`, `/difficulty peaceful`, `/gamerule spawn_mobs false`, `/time set 6000`, `/weather clear`.
   The world's difficulty persists from a saved `/difficulty`, so read it live.
2. Stand on flat ground facing **south**. Run **`/setblock ~ ~1 ~3 minecraft:deepslate`**. That is one deepslate
   block, three blocks south, at eye-level height. **Do not dig it.**
3. **`/rpg minespike pick 1.2`**. Select the `Spike Pickaxe (speed 1.2)` in the hotbar, and hold it for every row
   unless the row says otherwise. Predicted log line: **`MINESPIKE PICK speed=1.20 modifier=-2.80 delay=16.666666`**.
4. Video settings: **Attack Indicator: Crosshair**. That is client-side; record what it was.

**The arithmetic every cooldown prediction uses.** It was executed in jshell, mirroring
`Player#getCurrentItemAttackStrengthDelay` as `(float)(1.0 / speed * 20.0)` and `getAttackStrengthScale(0.5f)` as
`clamp((ticker + 0.5f) / delay, 0, 1)` (PLAN §5, TOOLS). At speed 1.2 the delay is `16.666666`, so:
- **A read Δ ticks after a reset is `(Δ + 0.5) / 16.666666`**: Δ=0 → **0.0300**, Δ=1 → **0.0900**, Δ=2 → **0.1500**,
  Δ=3 → **0.2100**;
- **the first full read (1.0000) comes at Δ ≥ 17.**

**Which tick Δ counts.** It is `tick` on the second line minus `tick` on the line of the swing that reset the ticker.
Both reads are inside packet handling, one player tick per server tick. **An off-by-one there is NOT AS PREDICTED,
and is recorded as an instrument-phase finding, not a mechanism failure.**

---

## R0 — THIS BRANCH'S BUILD. READ IT FIRST. IF ANY R0 FAILS, STOP

### R0a — the build line names this branch's head

| prediction | instrument | READING |
|---|---|---|
| `[Rpg] Build: <sha>`: **`spike/mining-input`'s head as read from `git ls-remote origin refs/heads/spike/mining-input` at boot time**, shortened. Not `-dirty`, not `unknown`, not master's `55425aad` | `Select-String -Path run\logs\latest.log -Pattern '\[Rpg\] Build:' \| Select-Object -First 1` | **PASS, 2026-11-12.** `latest.log` L43 `[21:17:54] … [Rpg] Build: 1dbcc156`. Before the boot, `git ls-remote origin refs/heads/spike/mining-input` gave `1dbcc1560d02c08fc6f63e0c88b799084645ebf2` and the local tree was clean at that sha |

### R0b — the jar carries the spike and its pack

| prediction | instrument | READING |
|---|---|---|
| PRESENT: core `mining/spike/MineSpikeLine` and `mining/spike/FacePoint`; paper `mining/spike/MineSpike`, `MineSpikeListener`, `MineSpikePack`; `resourcepack/pack.mcmeta`; `resourcepack/assets/rpg/textures/item/mining_spot.png`. ABSENT: the control | the scan below | **PASS, 2026-11-12.** All 7 named entries are `PRESENT`, and `ABSENT  …/NoSuchClassControl.class` |

```powershell
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::OpenRead((Resolve-Path 'run\plugins\rpg-0.1.0-SNAPSHOT.jar'))
foreach ($c in 'io/github/butterflysmp/rpg/core/mining/spike/MineSpikeLine.class',
               'io/github/butterflysmp/rpg/core/mining/spike/FacePoint.class',
               'io/github/butterflysmp/rpg/paper/mining/spike/MineSpike.class',
               'io/github/butterflysmp/rpg/paper/mining/spike/MineSpikeListener.class',
               'io/github/butterflysmp/rpg/paper/mining/spike/MineSpikePack.class',
               'resourcepack/pack.mcmeta',
               'resourcepack/assets/rpg/textures/item/mining_spot.png',
               'io/github/butterflysmp/rpg/paper/mining/spike/NoSuchClassControl.class') {
  if ($zip.GetEntry($c)) { "PRESENT $c" } else { "ABSENT  $c" }
}
$zip.Dispose()
```

### R0c — content loads, nothing refused, and the spike moved no count

| prediction | instrument | READING |
|---|---|---|
| `Loaded 11 abilities, 27 visuals, 4 statuses, 7 elements, 10 enchants, 3 pools, 6 fragments, 6 aspects, 14 weapons, 1 shields, 24 armor, 5 tools, 6 accessories, 1 mobs, 3 recipes`. **Identical to `GATE-legacy-a.md` R0c's prediction**, which is master `55425aad`'s content: the spike adds no content file. No `Refusing`, `Skipping`, `SEVERE` or exception at enable | `Select-String -Path run\logs\latest.log -Pattern 'Loaded \d+ abilities','Refusing','Skipping','SEVERE','Exception'` | **PASS, 2026-11-12.** L44 is the predicted line exactly (14 weapons). The scan matched only L44. The only `[Rpg]` WARN lines at enable are `volley_stone`'s known pair (L45–46). This was read after `--refresh-content` (CARRIED STATE note) |

---

## THE ROWS

**Reading a MINESPIKE line.** Format (pinned by `MineSpikeLineTest`):
`MINESPIKE seq=<n> tick=<t> src=<PACKET|EVENT> type=<T> cooldown=<0.0000|-> entity=<id:role|-> aim=<du,dv,in|out|off|->`.

**Sort by `seq`, never by log position.**
- A PACKET line's seq is taken on the Netty thread on arrival, but the line is logged after the hop. So it can print
  after the EVENT line its packet caused, while carrying the lower seq.
- A PACKET line's `cooldown` is read AFTER the hop, so it may be post-reset (PLAN F-M3).
- An EVENT line's `cooldown` is read inside the vanilla handler.
- `aim` is where the server's own ray meets the spiked face, in sixteenths from the spot centre. `in` means inside
  Ben's 5×5. **That ray is candidate A's whole hit test.**

### MS1 — block click on the spot: full charge, then early, then beside (SURVIVAL) — witness: the log + Ben's sight

**Steps.**
- Look at the deepslate's north face, near its centre, and run **`/rpg minespike block`**.
- **(a)** Wait at least 2 seconds, then left-click ONCE, on the emerald.
- **(b)** Click once more, as fast as you can after (a).
- **(c)** Wait at least 2 seconds, then click once a quarter of a block (4 pixels) to the RIGHT of the emerald, still
  on the deepslate.
- Watch the crosshair indicator through all three.

| prediction | READING |
|---|---|
| **From the jar.**<br>• **PLACE:** one `MINESPIKE PLACE mode=block block=<x>,<y>,<z> face=NORTH spot=<x>,<y>,<z> spotId=1500000000 boxId=-` (the boot's first spike use, so the first id), with the spot's z = the block's z − 0.001.<br>• **(a)** carries one EVENT `ARM_SWING` with **`cooldown=1.0000`** and `aim=…,in` (`PlayerArmSwingEvent` reads before the reset, F1). An EVENT `INTERACT_LEFT_BLOCK` comes from the **dig START** (`handleBlockBreakAction` step 4), NOT from the swing: in SURVIVAL, `handleAnimate` fires LEFT_CLICK_BLOCK only for ADVENTURE (158 `if_acmpne`), and LEFT_CLICK_AIR only when its server ray hits nothing (114, 121–137). **So INTERACT_LEFT_BLOCK's seq, against ARM_SWING's, is a second witness of the DIG_START-vs-SWING order**, and its cooldown is pre-reset exactly when it comes first.<br>• **(b)**'s EVENT `ARM_SWING` reads **`(Δ + 0.5) / 16.666666`**, where Δ is (b)'s tick minus (a)'s ARM_SWING tick. It is < 1.0000.<br>• **(c)**'s EVENT `ARM_SWING` reads `cooldown=1.0000` with **`aim=…,out`**, `|du|` about 4.<br>• Every BLOCK_DAMAGE line is the spiked block's, cancelled. No `BLOCK_BREAK`. No kick.<br>**MEASUREMENT (client), with every outcome:** see the table below | **READ 2026-11-12; ONE PART NOT AS PREDICTED: each click carries TWO `ARM_SWING`s, not one.** See *MS1 READING* below the table |

| what is measured (client behaviour) | outcome | means for A | means for B |
|---|---|---|---|
| does each click send a SWING? (a SWING packet / ARM_SWING event per click) | **yes, one per click** | A's charge is readable at ARM_SWING, pre-reset | B pairs on it |
| | **no SWING on a cancelled dig** | **A has no pre-reset read**: it falls back to BLOCK_DAMAGE, whose value depends on order | B needs ATTACK alone |
| order of SWING vs DIG_START by seq | **SWING first** | BLOCK_DAMAGE reads post-reset (0.0300 or 0.0900); ARM_SWING is A's read | — |
| | **DIG_START first** | BLOCK_DAMAGE reads pre-reset too: a second read site for A | — |
| the PACKET `SWING` line's cooldown | **0.0300 or 0.0900** (logged after vanilla reset it) | confirms F-M3: never read the charge at the hop | same |
| | **equal to the ARM_SWING value** (the hop ran before vanilla) | hop timing is not reliable either way: still use the event | same |
| `aim` on (a) and (b) | **`in`, with \|du\|,\|dv\| ≤ 2.5** | A's server ray agrees with where Ben aimed | — |
| | **`out` or `off` while Ben was on the emerald** | **A's ray lags or misses the crosshair**: a strike against A | — |
| crosshair indicator (sight) | **resets on each click and refills in about 0.85 s (17 ticks)** | the vanilla indicator works on block clicks: Ben's ask is met under A | — |
| | **no indicator change on a block click** | Ben's "vanilla hit indicator" is not available under A | B may still show it (MS2) |
| | **resets, but the refill time disagrees with 17 ticks** | client and server disagree on the charge: a finding, both candidates | same |

**MS1 READING, 2026-11-12** (session `spotId=1500000000`, L95 to L333; every MINESPIKE line sorted by `seq`).
- **PLACE (L95):** `mode=block block=-117,64,13 face=NORTH spot=-116.4063,64.6563,12.9990 spotId=1500000000 boxId=-`.
  This is as predicted: the first id, and spot z = 13 − 0.001. L91, `Changed the block at -117, 64, 13`, is the
  set-up's `/setblock … minecraft:deepslate`.
- **Ben clicked 22 times, not 3.** That was 19 clicks with aim `in` and 3 with aim `out`, plus one hold at tick
  9226–9243. Clicks are told apart by tick spacing: a full click is one reading 1.0000.
- **A SWING per click: YES, AND IT IS TWO, NOT ONE.**
  - **NOT AS PREDICTED:** every block click carries two `ARM_SWING` EVENTs in the same tick. The first reads before
    the reset (1.0000 on a charged click). The second reads `0.0300` (Δ=0), because the first swing just reset the
    ticker.
  - Counting PACKET `SWING` lines at the PACKET `DIG_START` tick, across every block session: 43 clicks had 2, 4 had
    1 and 3 had 3. That count uses the post-hop tick and is approximate; the EVENT pairs are exact.
  - **Consequence for A:** read the charge at the FIRST `ARM_SWING` after the click's `DIG_START`, never at any
    swing.
- **The order by seq: DIG_START comes FIRST, every time.**
  - Click 1, for example: PACKET `DIG_START` seq 1, PACKET `SWING` seq 2, EVENT `INTERACT_LEFT_BLOCK` seq 3, PACKET
    `SWING` seq 4, EVENT `BLOCK_DAMAGE` seq 5, EVENT `ARM_SWING` seq 6 at 1.0000, EVENT `ARM_SWING` seq 7 at 0.0300.
  - So `INTERACT_LEFT_BLOCK` and `BLOCK_DAMAGE` read before the reset, and equal the first `ARM_SWING` on every
    click. **That is the table's "DIG_START first" outcome: a second read site for A.**
- **(a), a charged click:** the first `ARM_SWING` reads `1.0000` on 10 clicks, each at least 20 ticks after the
  previous swing.
- **(b), an early click (12 clicks):** the first `ARM_SWING` read 0.8100, 0.5700, 0.4500, 0.3900, 0.3300 ×2,
  0.2700 ×3 and 0.2100 ×3. Each is `(Δ+0.5)/16.666666` for a whole number Δ. About 10% of all sub-1.0 reads this
  boot sit one tick off the logged tick difference; for example, click 7 at tick 8441 reads 0.2700 (Δ=4) against a
  logged difference of 5. **That is the instrument-phase finding this gate names, not a mechanism failure.**
- **PACKET-line cooldown:** every PACKET `SWING` line reads `0.0300`, which is after the reset. As predicted, this
  confirms F-M3.
- **aim:**
  - On the emerald (19 clicks): `-0.05,+0.20` · `+0.24,+0.37` · `+0.04,+0.86` · `-0.22,+0.14` (×10) · `-0.13,+0.17` ·
    `+0.07,+0.17` · `-0.02,+0.14` · `-0.25,+0.11` · `+0.07,+0.23` · `-2.21,+0.46` (the hold). All are `in`, with
    |du| and |dv| ≤ 2.21.
  - **(c), beside (3 clicks):** `-4.09,+0.14,out` each time, |du| about 4, as predicted. The 1st and 3rd read 1.0000;
    the 2nd was an early click.
  - **So A's server ray agreed with where Ben aimed.**
- **No `BLOCK_BREAK` line. No kick.** Every `BLOCK_DAMAGE` was on the spiked block.
- **Sight (Ben's answer 1, verbatim):** *"The attack indicator displays correctly every time"*. The indicator resets
  and refills on a block click. **Refill time: NOT STATED.** The log's cooldown values carry it.
- **Outcomes:** one SWING per click → **two** (above) · SWING vs DIG_START → **DIG_START first** · PACKET cooldown →
  **0.0300** · aim → **`in`** · indicator → **resets and refills** (duration NOT STATED).

### MS2 — the same three clicks against the INTERACTION box (SURVIVAL) — witness: the log + Ben's sight

**Steps.**
- **`/rpg minespike block`** again. Predicted: `MINESPIKE STOP mode=block reason=second_run`.
- Then **`/rpg minespike box`**, looking at the same face.
- Optional: F3+B shows entity hitboxes (client debug); record whether the box outline appears.
- Repeat MS1's **(a)**, **(b)** and **(c)**. (c) is a click beside the box, on the deepslate.

| prediction | READING |
|---|---|
| **From the jar.**<br>• **PLACE:** `MINESPIKE PLACE mode=box … spotId=<s> boxId=<s − 1>`. Ids count DOWN across every session of the boot, from MS1's `1500000000`, so `<s>` is one below the previous PLACE's lowest id. **Every `entity=` below must equal THIS PLACE's `boxId`.**<br>• **If the client sends an attack on the box,** it arrives as PACKET **`ATTACK` with `entity=<boxId>:box`** (`Play.Client.ATTACK`, F3), and the server fires EVENT **`UNKNOWN_ENTITY_ATTACK` `entity=<boxId>:box`** (handleAttack 289). **No kick**, and no `INTERACT_ENTITY`-based line.<br>• `handleAttack` does not reset the ticker (F1). So `UNKNOWN_ENTITY_ATTACK`'s cooldown is **pre-reset (1.0000 on (a)) exactly when its seq is lower than the click's SWING**, and **post-reset (0.0300 or 0.0900) when it is higher**.<br>• **(c)**: no ATTACK, only SWING / DIG_START / BLOCK_DAMAGE, with `aim=…,out`. That is B's MISS (PLAN §4).<br>**MEASUREMENT (client):** the order, and whether ATTACK arrives at all; see the table below. Record the full seq-ordered sequence for one click of each kind | **READ 2026-11-12. As predicted where witnessed. (b), an early click on the box, was NOT WITNESSED.** See *MS2 READING* below the table |

| what is measured (client behaviour) | outcome | means for A | means for B |
|---|---|---|---|
| the order for click (a), by seq | **ATTACK, then SWING** | — | B reads the charge at `UNKNOWN_ENTITY_ATTACK` (pre-reset); a hit = ATTACK + the next SWING |
| | **SWING, then ATTACK** | — | the attack-time read is post-reset; **B reads the charge at the paired ARM_SWING** and pairs it to the following ATTACK |
| | **ATTACK, no SWING** | — | **nothing resets the server ticker**: every B click would read full. B must call `resetCooldown()` itself |
| | **no ATTACK at all** (SWING + DIG_START on the block) | A is unaffected | **B is dead**: the client does not attack a packet-only Interaction |
| click (c), beside the box | **no ATTACK** | — | B's miss works as defined |
| | **ATTACK on (c)** | — | the client's box is larger than 5/16, or its hit-test differs: B's box needs measuring |
| crosshair indicator on the box (sight) | **resets and refills as in MS1, perhaps with an "attackable target" mark** | — | Ben's indicator ask is met under B |
| | **no change** | — | no indicator under B either |

**MS2 READING, 2026-11-12.**
- **Sessions.** L499 placed `spotId=1499999996 boxId=1499999995` and L501 stopped it one second later (an extra
  command). That session carries no lines. **The MS2 session is L503:** `mode=box … spotId=1499999994
  boxId=1499999993`, stopped at L582. The ids count down as predicted.
- **Every `entity=` in the session is `1499999993:box`, THIS PLACE's `boxId`.** It is never `…95` and never a wrong
  id. MS3(b)'s session (`boxId=1499999997`) likewise named only `…97`.
- **(a), on the box: 4 clicks, at ticks 11605, 11623, 11643 and 11662.** Each is exactly one PACKET `ATTACK`
  `entity=1499999993:box`, one EVENT `UNKNOWN_ENTITY_ATTACK` and one `SWING`. No `INTERACT_ENTITY`-based line, and
  no kick. One click by seq: PACKET `ATTACK` 393, EVENT `UNKNOWN_ENTITY_ATTACK` 394 at **1.0000**, PACKET `SWING`
  395, EVENT `ARM_SWING` 396 at 1.0000.
  - **The order is ATTACK, then SWING, on all 8 box attacks this boot** (4 here, 4 in MS3(b)). Every
    `UNKNOWN_ENTITY_ATTACK` reads **1.0000**, before the reset.
  - **Unlike a block click, a box click carries ONE swing, not two.**
  - The PACKET `ATTACK` cooldown read 0.0300 three times and 1.0000 once (seq 405), so the hop is unreliable either
    way.
- **(b), an early click on the box: NOT WITNESSED.** The four attacks are 18, 20 and 19 ticks apart. The full
  charge at 1.2 is 17 ticks, so none was early. **There is no SWING without an ATTACK anywhere in the session.** So
  either Ben's second click came after the bar refilled, or the client sent nothing for an uncharged click on the
  box. The log cannot tell which. **Ben's answer 9** (did the arm swing on the fast second click?) **was "not sure":
  NOT STATED.** The predicted post-reset read on an early box click is therefore unread.
- **(c), beside: 3 clicks (ticks 11754, 11868, 11910), with no ATTACK.** Each is a DIG_START / BLOCK_DAMAGE / two
  SWINGs, with aim `-5.46,+0.37,out`, `-6.26,-0.01,out` and `-4.79,+0.14,out`. **That is B's miss, working as
  defined.** The 2nd and 3rd were held for about 8 and 6 ticks (ticks 11868–11876 and 11910–11916). Their swings
  read 0.0900 on most ticks, with one two-swing tick (0.0300) and one skipped tick (0.1500), and then
  `DIG_ABORT`.
- **aim on the box clicks** (A's ray to the block face, logged on `ARM_SWING`): `+0.29,-0.16` · `+0.16,+0.18` ·
  `-0.51,+0.15` · `-0.13,+0.15`, all `in`.
- **Sight (Ben's answer 2, verbatim):** *"the attack indicator is also correctly displayed against the emerald"*.
  An "attackable target" mark: **NOT STATED.** **Answer 8, F3+B:** *"yes a box did appear"*.
- **Outcomes:** order → **ATTACK, then SWING** (B reads the charge at `UNKNOWN_ENTITY_ATTACK`) · ATTACK arrives →
  **yes** · (c) → **no ATTACK** · indicator → **resets and refills** (mark NOT STATED).

### MS3 — HOLDING left-click on the block (SURVIVAL) — witness: the log

**Steps.**
- **`/rpg minespike box`** again to stop. Predicted: `STOP mode=box reason=second_run`.
- Then **`/rpg minespike block`**.
- **(a)** HOLD left-click on the emerald for about 3 seconds, then release.
- **(b)** Stop, start `box`, and hold left-click on the box for about 3 seconds.

| prediction | READING |
|---|---|
| **From a prior measurement, not the jar.** Master's `RpgListeners.onStoneSwing` javadoc: *"A held left click on a block swings every tick (measured)"*, under the same condition (the stone's `BlockDamageEvent` is cancelled).<br>• **(a)** therefore gives one EVENT `ARM_SWING` per tick, each resetting the ticker. After the first, each reads **`cooldown=0.0900`** (Δ=1). **No ARM_SWING after the first reads 1.0000.**<br>• No `BLOCK_BREAK` line: the server never set `destroyPos` (F2b).<br>**MEASUREMENT (client):** the DIG_START / DIG_STOP cadence, and (b) entirely; see the table below | **READ 2026-11-12. (a) is as predicted: no held swing after the first reads 1.0000. The client sent repeated `DIG_STOP`s while holding. (b) is unresolved: Ben is not sure whether he held or clicked.** See *MS3 READING* below the table |

| what is measured (client behaviour) | outcome | means for A | means for B |
|---|---|---|---|
| ARM_SWING cadence while held on the block | **every tick, cooldown 0.0900** | a held click can never be a charged hit, so no farming by holding | misses while holding stay misses |
| | **any held swing reads 1.0000** | **A needs a rule**: a held click could hit every 17 ticks | — |
| DIG lines while held | **one DIG_START, then a DIG_STOP after the client's local break time** | the client predicts a break: see MS4 | — |
| | **repeated DIG_START** | the client restarts the dig: noise, harmless | — |
| (b) held on the box | **one ATTACK, then SWINGs with no further ATTACK** | — | holding cannot repeat a hit |
| | **repeated ATTACKs** | — | B needs its own repeat guard |
| either | **the block breaks or ghost-breaks on Ben's screen** | the patch's block is not safe under click input: a STOP for the area design (MS4) | the same, for misses only |

**MS3 READING, 2026-11-12.**
- **(a), held on the block** (session `spotId=1499999999`, L335 to L459). `ARM_SWING` ran every tick from 9901 to
  9958 (about 2.9 s): **58 events**.
  - **1 × 1.0000** (the first), **45 × 0.0900** (Δ=1), **6 × 0.1500** (a tick with no swing), and **6 × 0.0300**.
    Five ticks carried two swings: 9901 (the opening pair, as in MS1), 9909, 9922, 9935 and 9949. That accounts for
    5 of the 6 reads at 0.0300; the 6th is not attributed here.
  - **No held swing after the first reads 1.0000. As predicted, so a hold can never be a charged hit.**
  - **DIG lines:** one PACKET `DIG_START` (tick 9902), then **PACKET `DIG_STOP` at ticks 9918, 9938 and 9959**, with
    no further `DIG_START` and no `DIG_ABORT`. That is the table's "one DIG_START, then a DIG_STOP after the client's
    local break time" outcome, **repeated every 20 or 21 ticks after the first at +16**.
  - MS1's hold (ticks 9226–9243) shows the same thing: `DIG_START` 9227, `DIG_STOP` 9241.
  - **No `BLOCK_BREAK` line.**
- **(b), on the box** (session `boxId=1499999997`, L461 to L497). **4 ATTACKs**, at ticks 10549, 10570, 10606 and
  10679 (21, 36 and 73 apart). Each has exactly one `SWING`, and each `UNKNOWN_ENTITY_ATTACK` reads 1.0000. No
  ATTACK follows another without its own swing.
  - Between the 3rd and 4th there is one beside click (tick 10650, aim `-5.81,-0.46,out`), held about 4 ticks, then
    `DIG_ABORT`.
  - **Ben's answer 10** (held, or clicked several times?) **was "not sure": NOT STATED.** So "holding cannot repeat a
    hit" and "repeated ATTACKs" cannot be told apart. The irregular spacing does not settle it.
- **Outcomes:** held cadence → **every tick, 0.0900** · DIG lines → **one DIG_START, then repeated DIG_STOPs** ·
  (b) → **unresolved** · break/ghost-break → see MS4.

### MS4 — the cancelled block: crack, flicker, or break (SURVIVAL) — witness: Ben's sight + the log

Read during MS1 and MS3(a).

| prediction | READING |
|---|---|
| **From the jar.**<br>• The server sends **no destroy progress and no block update** for a cancelled START (F2).<br>• A STOP for the spiked block breaks nothing (`destroyPos` mismatch, F2b).<br>• **The server's block stays deepslate: no `BLOCK_BREAK` line**, and if one appears, it was cancelled and the server sent a resync (`destroyAndAck` 19–42).<br>**MEASUREMENT (client), what Ben SEES:** see the table below | **READ 2026-11-12. The jar half is as predicted: no `BLOCK_BREAK` line, and no sign that the server's block was ever air. Sight: crack, flicker, vanish and return all occurred.** See *MS4 READING* below the table |

| outcome (sight) | means for A | means for B |
|---|---|---|
| **nothing: no crack, no flicker** | the cancel is enough | the same |
| **a crack overlay grows, then vanishes** (the client's local dig) | the patch art is undermined by a crack under it: slice 1 must stop the client's dig, e.g. with a zero block-break-speed attribute in areas (a candidate, unread) | the same, but only on misses |
| **the block disappears, then reappears** (a predicted break, then rolled back on the ack) | the same as above, worse | the same, on misses |
| **the block disappears and STAYS gone** (a ghost) until relog or re-interaction | **a STOP for the area design**: the client must be stopped from predicting breaks | the same, on misses |
| **a `BLOCK_BREAK` line appears** | F2b missed a route: **STOP, finding** | the same |
| **the block is actually gone server-side** (others, or a relog, see air) | a break route bypassed both cancels: **STOP, finding** | the same |

**MS4 READING, 2026-11-12.**
- **Sight (Ben's answer 3, verbatim):** *"yes, it cracked flickered vanished and came back. but only when not mining
  the emerald"*. By the seat's ruling, the qualifier is recorded as given and NOT interpreted. The log facts the
  seat asked for are below.
- **Per session** (counts of MINESPIKE lines; "clicks" are `INTERACT_LEFT_BLOCK` events split by their aim):

  | session (mode, ids, log lines) | step | DIG_START | DIG_ABORT | DIG_STOP | BLOCK_DAMAGE | BLOCK_BREAK | ATTACK | block clicks aim in / out |
  |---|---|---|---|---|---|---|---|---|
  | block `…000`, L95–L333 | MS1 | 22 | 21 | **1** | 22 | 0 | 0 | 19 / 3 |
  | block `…999`, L335–L459 | MS3(a) | 1 | 0 | **3** | 1 | 0 | 0 | 1 / 0 |
  | box `…998`/`…997`, L461–L497 | MS3(b) | 1 | 1 | 0 | 1 | 0 | 4 | 0 / 1 |
  | box `…994`/`…993`, L503–L582 | MS2 | 3 | 3 | 0 | 3 | 0 | 4 | 0 / 3 |
  | block `…992`, L584–L814 | MS5 | 28 | 28 | 0 | 28 | 0 | 0 | 27 / 1 |
  | model `…989`, L848–L861 | MS7(b) | 1 | 1 | 0 | 1 | 0 | 0 | 1 / 0 |

- **All 4 `DIG_STOP`s came during HOLDS in BLOCK mode with aim `in`.** That is MS1's hold at tick 9241
  (`-2.21,+0.46`) and MS3(a) at 9918, 9938 and 9959 (`-0.05,+0.17`). A `DIG_STOP` is the client reporting that its
  local dig finished.
- In BOX mode, the clicks on the emerald produced `ATTACK` and no DIG line. Every DIG line in a box session came from
  a click beside the box (aim `out`).
- **`BLOCK_BREAK`: zero lines in the whole boot.**
- **Was the server's block ever air? There is no evidence that it was.**
  - No `BLOCK_BREAK` line appears. `BlockBreakEvent` fires on every player break route (F2b), and the spike logs and
    cancels it on the spiked block while a session is up.
  - Every one of the 8 PLACE commands ray-traced a solid block at `-117,64,13`, the last at 21:32:33 (L848).
  - What the log cannot exclude: a change made while NO session was up. The spike logs `BLOCK_BREAK` only for a live
    session's block. Nothing in the log indicates one.
- **Outcome:** the sight reading matches both "a crack overlay grows, then vanishes" and "the block disappears, then
  reappears". It is not "STAYS gone", and there is no `BLOCK_BREAK`. **For A and B: slice 1 must stop the client's
  local dig at the AREA level.** Finding the mechanism is fact F7, gathered after this commit.

### MS5 — the attack speed argument moves the refill (SURVIVAL) — witness: the log + Ben's sight

**Steps.**
- Stop any session, then run **`/rpg minespike block`**.
- For each of **`/rpg minespike pick 1.0`**, **`2.0`** and **`4.0`**: select the new pickaxe, wait at least 2 seconds,
  click once on the emerald (the full read), then click twice fast (the early reads). Watch the indicator refill.
- Also hover each pickaxe, and **`/give @s minecraft:iron_pickaxe`** once, for the tooltip comparison.

| prediction | READING |
|---|---|
| **From the jar and the executed arithmetic.**<br>• **The PICK lines:** `speed=1.00 modifier=-3.00 delay=20.0`, `speed=2.00 modifier=-2.00 delay=10.0`, `speed=4.00 modifier=0.00 delay=5.0`.<br>• **The full read:** ARM_SWING `cooldown=1.0000` once Δ reaches the first full ticker, **20, 10 and 5** respectively (17 at 1.2).<br>• **Each early read:** ARM_SWING `cooldown = (Δ + 0.5) / delay`. Δ=0 → **0.0250 / 0.0500 / 0.1000**; Δ=1 → **0.0750 / 0.1500 / 0.3000**.<br>• **The override does not interfere.** A pickaxe carries no weapon id, so `heldMeleeSpeed` is 0.0 and `AttackSpeedAttributeOverride` only removes its own key (PLAN §3, the override reading).<br>• **Tooltip:** a green **"N Attack Speed"** line with N = **1, 2, 4** (format `#.##`; key `minecraft:base_attack_speed`, F4). **The same Attack Damage line as the plain `/give` iron pickaxe**, because the defaults are copied.<br>**MEASUREMENT (client):** the refill Ben sees, against 20, 10 and 5 ticks (1.0 s, 0.5 s, 0.25 s) | **READ 2026-11-12. The log is as predicted. Sight: the refill changed with speed. NOT AS PREDICTED: the tooltip shows Attack Speed ABOVE Attack Damage, the reverse of vanilla. That is a spike defect.** See *MS5 READING* below the table |

| outcome | means |
|---|---|
| **the refill time scales with speed, and every logged read equals `(Δ+0.5)/delay`** | the item modifier drives both the client indicator and the server charge: the pickaxe-stat design (PLAN §5, TOOLS) stands |
| **the logged reads match but Ben sees no refill difference** | the client indicator does not follow the item's attribute: a finding against Ben's indicator ask |
| **the Attack Damage line is missing or differs from the plain pickaxe** | the defaults were not copied: a spike defect, and the exact F4 hazard |
| **a read disagrees with `(Δ+0.5)/delay` beyond a tick of phase** | something else writes ATTACK_SPEED: the override reading is wrong; STOP |

**MS5 READING, 2026-11-12** (session `spotId=1499999992`, L584 to L814; `/give @s minecraft:iron_pickaxe` at L811).
- **The PICK lines are exactly as predicted:** L586 `speed=1.00 modifier=-3.00 delay=20.0`, L646 `speed=2.00
  modifier=-2.00 delay=10.0`, L712 `speed=4.00 modifier=0.00 delay=5.0`.
- **The `ARM_SWING` reads, by speed:**
  - **1.0:** 0.0250 ×7, 0.0750, 0.3250, 0.4750, 0.5750, 1.0000 ×4.
  - **2.0:** 0.0500 ×8, 0.3500, 0.4500, 0.6500 ×2, 0.8500, 1.0000 ×3.
  - **4.0:** 0.1000 ×10, 0.7000, 0.9000 ×2, 1.0000 ×10.
- **Every read below 1.0000 is `(Δ+0.5)/delay` for a whole number Δ**, within one tick of the logged tick difference.
  This holds for `INTERACT_LEFT_BLOCK` and `BLOCK_DAMAGE` as well.
  - Δ=0 → 0.0250 / 0.0500 / 0.1000: **as predicted.**
  - Δ=1 → 0.0750 was read at 1.0. The 0.1500 (2.0) and 0.3000 (4.0) were **not read**, because Ben's second fast
    click came later.
  - The shortest gap before a 1.0000 read: 25 ticks at 1.0, 12 at 2.0 and 4 at 4.0. The 4 is one tick of phase short
    of 5.
  - **Nothing else writes ATTACK_SPEED. The override reading stands.**
- **Sight (Ben's answer 4, verbatim):** *"Yes, the refill time is correct between each"*. The refill time changed
  with speed. **Durations: NOT STATED.**
- **Sight (Ben's answer 5, verbatim):** *"The attack speed was above the attack damage line which is different from
  vanilla (attack damage then attack speed)"*. **The numbers on either line: NOT STATED.**
- **SPIKE DEFECT: the modifier ORDER in `MineSpike.givePick`. The cause, from the code and the jars:**
  - `givePick` collects the modifiers in `ArrayListMultimap.create()`. Guava 33.5.0's `ArrayListMultimap.<init>`
    calls `Platform.newHashMapWithExpectedSize`, which calls `Maps.newHashMapWithExpectedSize`, which returns a
    **`java.util.HashMap`**. So the attribute KEYS iterate in hash order, not insertion order (`javap -c -p` on
    `com.google.common.collect.ArrayListMultimap` and `Platform`, from
    `run/libraries/com/google/guava/guava/33.5.0-jre/`).
  - `CraftMetaItem.setAttributeModifiers` copies the argument's `entries()` in their iteration order into its own
    multimap. `CraftMetaItem.applyModifiers` then builds `ItemAttributeModifiers` by iterating `entries()`.
    (`javap -c -p org.bukkit.craftbukkit.inventory.CraftMetaItem` on `run/versions/26.1.2/paper-26.1.2.jar`.)
  - So the item's component list comes out in the HashMap's order of the two attribute keys, and in this boot that
    put ATTACK_SPEED first. **That the tooltip follows the component list's order is inferred from Ben's sighting,
    not read from the jar.**
  - **The fix:** an insertion-ordered multimap (`LinkedHashMultimap` / `LinkedListMultimap`), filled explicitly in vanilla
    order (Attack Damage, then Attack Speed), with a test that pins the order. The iteration order of
    `getDefaultAttributeModifiers()` itself was not read, so it is not relied on.
  - **Not fixed on the spike.** It belongs to the slice that gives `ToolItems.mint` its attack-speed modifier.
- **Outcome:** "the refill time scales with speed, and every logged read equals `(Δ+0.5)/delay`" → **the item
  modifier drives both the client indicator and the server charge.** The Attack Damage line's presence was not
  stated as missing. Whether its numbers equal the plain pickaxe's: **NOT STATED.**

### MS6 — the spot and box are not in the server's entity list (SURVIVAL) — witness: the log

**Steps, while MS2's `box` session is up** (re-run `/rpg minespike box` if needed), standing next to the block:
1. `/execute if entity @e[type=minecraft:interaction,distance=..8] run say MS6-FOUND-interaction`
2. `/execute if entity @e[type=minecraft:item_display,distance=..8] run say MS6-FOUND-item_display`
3. **Positive control:** `/summon minecraft:interaction ~ ~ ~`, then step 1 again.
4. `/kill @e[type=minecraft:interaction,distance=..8]`

| prediction | READING |
|---|---|
| **From the code.** The spike only SENDS spawn packets (`MineSpikeVisuals`) and never adds an entity to a world.<br>• Steps 1 and 2: **no `MS6-FOUND` line in the log** (chat: `Test failed`).<br>• Step 3: **exactly one `[BaronVonYeetus] MS6-FOUND-interaction`** line. **This is the control: it shows the instrument can find an interaction when one exists.**<br>• Step 4 kills exactly the summoned one (`Killed Interaction`). The packet box is untouched, because the server does not know it | **PASS, 2026-11-12, with its control.** This ran in the MS2 session (`boxId=1499999993`, live from L503 to L582). Step 1 (L572, repeated at L573) and step 2 (L574) printed **no `MS6-FOUND` line**. Step 3: L576 `Summoned new Interaction`, then L578 **`[Not Secure] [BaronVonYeetus] MS6-FOUND-interaction`**, exactly one. Step 4: L580 `Killed Interaction`, one entity. `MS6-FOUND-item_display` never appears |

### MS7 — the custom patch and spot, pack DECLINED and then ACCEPTED (SURVIVAL) — witness: Ben's sight + the log

**Steps.**
- Stop any session.
- **(a) Decline:** run **`/rpg minespike pack`** and press **No** on the prompt. Then look at the deepslate's north face
  and run **`/rpg minespike model`**, and record what you see. Then run `/rpg minespike model` again to stop.
- **(b) Accept:** run **`/rpg minespike pack`** again and press **Yes**. If no prompt appears, see the table. Then run
  **`/rpg minespike model`** on the same face.

| prediction | READING |
|---|---|
| **From F6.**<br>• **Each offer logs** `MINESPIKE PACK url=http://127.0.0.1:<port>/mining-spike.zip sha1=<40 hex> bytes=<n>`. The sha1 is the same on both offers (the zip is deterministic: `MineSpikePackTest`).<br>• **(a)** gives EVENT `PACK_DECLINED`, and **no disconnect** (`require-resource-pack=false`; the server kicks only on DECLINED with it true).<br>• **(b)** gives `PACK_ACCEPTED`, then `PACK_DOWNLOADED`, then **`PACK_SUCCESSFULLY_LOADED`**.<br>• **`model` logs** `MINESPIKE PLACE mode=model … boxId=-` and `MINESPIKE PATCH patchId=<spotId − 1> model=rpg:mining_patch spotModel=rpg:mining_spot` (the spot takes its id first, then the patch).<br>**From F5.** The spot is an ITEM_DISPLAY at the PLACE line's `spot=` position, showing `item_model rpg:mining_spot`, scaled 3/16 × 3/16. The patch is centred on the face, 0.0005 off it, showing `rpg:mining_patch` scaled 2 × 3. Both are rotated so the model's +Z faces NORTH (`FacePoint.rotation`).<br>**MEASUREMENT (client), with every outcome:** see the table below | **READ 2026-11-12. The log is as predicted, after three instant re-declines ("the client remembered"). Declined: "a purple black cube". Accepted: "an orange square with a gray background".** See *MS7 READING* below the tables |

| outcome (sight, with the pack ACCEPTED) | means |
|---|---|
| **an orange 3/16 square on a grey speckled 2-wide × 3-tall panel, flat on the face, the square at the logged position** | F5's route works end to end: item_model + ITEM_DISPLAY + packet-only. The design (PLAN §2) is buildable |
| **the right art but HALF the size** | the FIXED display context still applies a scale; the model's `display.fixed` or the entity scale needs adjusting. Recorded as a number for slice 1 |
| **the right art but rotated, mirrored, or facing into the wall** (invisible from the front) | the FIXED context's own rotation is not identity: `FacePoint.rotation` needs a fixed correction per face |
| **a magenta-and-black texture in the right shape** | the model resolved, the texture did not: `rpg:item/*` is not stitched into the atlas the item renderer uses. A pack-layout finding |
| **a purple-black "missing model" cube, or a paper icon** | `item_model` did not resolve to `assets/rpg/items/<id>.json` |
| **nothing visible at all** | either the metadata indices are wrong (F5) or the zero-thickness face is back-face-culled: try the other side (south face) and record |

| outcome (the pack DECLINED, step (a)) | means |
|---|---|
| **the same missing-model cube or paper as above** | expected: without the pack the item_model cannot resolve. This is what a player who declines sees, and it is the case that decides whether the shipped pack must be REQUIRED |
| **the vanilla paper item** | the client falls back to the base item's model, so a decliner sees paper |
| **nothing** | a decliner sees no patches at all: mining would be invisible to them |

| outcome (the offer itself) | means |
|---|---|
| **the second offer shows no prompt and logs `PACK_DECLINED` at once** | the client remembered the first answer for this server (the server-list entry's "Server Resource Packs" setting). Set it to **Prompt** in the multiplayer list, reconnect, re-run (b). Recorded, not a defect |
| **`PACK_FAILED_DOWNLOAD` or `PACK_INVALID_URL`** | the client could not reach `127.0.0.1:<port>`. Is the client on the server's machine? If not, the dev offer needs a reachable host. Recorded |
| **the command errors with `NoClassDefFoundError: com/sun/net/httpserver/...`** | the server's JRE lacks `jdk.httpserver`. A finding; the pack is then served by hand (`jwebserver`) |

**MS7 READING, 2026-11-12.**
- **Five offers.** Every one logged `MINESPIKE PACK url=http://127.0.0.1:57714/mining-spike.zip
  sha1=e3ff1dd312b968be7e9d6f66de3fcb146e8267ed bytes=2120`: **the same sha1 on all five**, at L816, L824, L827,
  L835 and L843.
- **(a), declined.**
  - L817 EVENT `PACK_DECLINED` (seq 681, 21:30:51), with **no disconnect**.
  - L819 `PLACE mode=model … spotId=1499999991 boxId=-`, then L820 `PATCH patchId=1499999990 model=rpg:mining_patch
    spotModel=rpg:mining_spot`: the patch id is the spot id − 1, as predicted. L822 STOP.
  - **Sight (Ben's answer 6, verbatim):** *"a purple black cube"*. That is the table's "missing-model cube" outcome,
    **as expected without the pack.**
- **The offer itself: "the client remembered".** Offers 2 and 3 (L824/L825 and L827/L828, 21:31:11 and 21:31:13)
  each gave `PACK_DECLINED` in the same second, with no prompt.
  - Ben disconnected (L829) and rejoined (L833). Offer 4 (L835/L836) was again declined at once.
  - He disconnected (L837) and rejoined (L841), after changing the server-list setting.
  - Offer 5 (L843): **L844 `PACK_ACCEPTED` and L845 `PACK_DOWNLOADED` (tick 17247), then L846
    `PACK_SUCCESSFULLY_LOADED` (tick 17270).** That is the predicted sequence.
  - **No `PACK_FAILED_DOWNLOAD`.** The client reached `127.0.0.1`, so it runs on the server's machine. It logged in
    from `192.168.68.95`, the machine's LAN address (CARRIED STATE note).
- **(b), accepted.** L848 `PLACE mode=model … spot=-116.4063,64.4688,12.9990 spotId=1499999989`, then L849 `PATCH
  patchId=1499999988`, then L861 STOP.
  - **Sight (Ben's answer 7, verbatim):** *"an orange square with a gray background"*. That fits the first outcome
    row's art: an orange square on a grey panel.
  - **Size, facing, and the square's position against the logged `spot=`: NOT STATED.** The PLACE and PATCH lines
    carry the server side only. So "the right art but HALF the size" and "rotated or mirrored" are not ruled out by
    this reading.
- **No `NoClassDefFoundError`:** `jdk.httpserver` is present.

---

## LAST STEP — STOP CLEANLY THIS TIME

- Run `/rpg minespike <mode>` to stop any session. Predicted: a `STOP … reason=second_run` line.
- Then **`/stop` in the server console or chat.**
- **Predicted in the log:** `Stopping the server`, `[Rpg] Disabling Rpg`, and `All dimensions are saved` after it. No
  `java.exe` left afterwards (`Get-CimInstance Win32_Process -Filter "Name='java.exe'"` empty).
- If a session was still active at `/stop`, a `MINESPIKE STOP mode=<m> reason=disable` line comes before the disable
  completes. `onDisable` calls `mineSpike.shutdown()` first, which also stops the pack server.
- The previous session ended without this (CARRIED STATE). **This step exists so the next boot does not inherit the
  same unknown.**

**READING, 2026-11-12: CLEAN.**
- The last session STOPped at L861 (21:32:59). Ben ran `/stop` at L862 (21:36:29).
- **L863 `Stopping the server`, L865 `[Rpg] Disabling Rpg v0.1.0-SNAPSHOT`, and L895 `All dimensions are saved`**
  (the only other match is L69, the startup save).
- No `reason=disable` line, because no session was active.
- `tasklist /FI "IMAGENAME eq java.exe"` afterwards: `No tasks are running which match the specified criteria.`
- The `dev-server.sh` process exited 0. The log is 899 lines long.

## NO MUTATIONS AT BOOT

The spike's guards were mutation-checked at build time, before this file was committed:
- `FacePointTest.eachRotationTurnsPlusZOntoTheFaceNormal` reddened under an EAST sign flip (marker 1, delta 1;
  restored, 0 markers left).
- `MineSpikePackTest.filesNamesEveryFileInTheSourceDirectoryAndNothingElse` reddened with one `FILES` entry deleted
  (delta 1; restored).

Nothing is mutated at boot.
