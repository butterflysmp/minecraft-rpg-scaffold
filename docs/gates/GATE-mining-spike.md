# GATE — MINING INPUT SPIKE: how a hit on a weak spot is detected (candidates A and B), and the pack

**Status: NOT RUN.** Written 2026-11-12, before any boot of this branch. **NO BOOT until the seat has diffed the
spike code AND this gate and says so, and Ben says ready.**

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
| `[Rpg] Build: <sha>`: **`spike/mining-input`'s head as read from `git ls-remote origin refs/heads/spike/mining-input` at boot time**, shortened. Not `-dirty`, not `unknown`, not master's `55425aad` | `Select-String -Path run\logs\latest.log -Pattern '\[Rpg\] Build:' \| Select-Object -First 1` | _(not run)_ |

### R0b — the jar carries the spike and its pack

| prediction | instrument | READING |
|---|---|---|
| PRESENT: core `mining/spike/MineSpikeLine` and `mining/spike/FacePoint`; paper `mining/spike/MineSpike`, `MineSpikeListener`, `MineSpikePack`; `resourcepack/pack.mcmeta`; `resourcepack/assets/rpg/textures/item/mining_spot.png`. ABSENT: the control | the scan below | _(not run)_ |

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
| `Loaded 11 abilities, 27 visuals, 4 statuses, 7 elements, 10 enchants, 3 pools, 6 fragments, 6 aspects, 14 weapons, 1 shields, 24 armor, 5 tools, 6 accessories, 1 mobs, 3 recipes`. **Identical to `GATE-legacy-a.md` R0c's prediction**, which is master `55425aad`'s content: the spike adds no content file. No `Refusing`, `Skipping`, `SEVERE` or exception at enable | `Select-String -Path run\logs\latest.log -Pattern 'Loaded \d+ abilities','Refusing','Skipping','SEVERE','Exception'` | _(not run)_ |

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
| **From the jar.**<br>• **PLACE:** one `MINESPIKE PLACE mode=block block=<x>,<y>,<z> face=NORTH spot=<x>,<y>,<z> spotId=1500000000 boxId=-` (the boot's first spike use, so the first id), with the spot's z = the block's z − 0.001.<br>• **(a)** carries one EVENT `ARM_SWING` with **`cooldown=1.0000`** and `aim=…,in` (`PlayerArmSwingEvent` reads before the reset, F1). An EVENT `INTERACT_LEFT_BLOCK` comes from the **dig START** (`handleBlockBreakAction` step 4), NOT from the swing: in SURVIVAL, `handleAnimate` fires LEFT_CLICK_BLOCK only for ADVENTURE (158 `if_acmpne`), and LEFT_CLICK_AIR only when its server ray hits nothing (114, 121–137). **So INTERACT_LEFT_BLOCK's seq, against ARM_SWING's, is a second witness of the DIG_START-vs-SWING order**, and its cooldown is pre-reset exactly when it comes first.<br>• **(b)**'s EVENT `ARM_SWING` reads **`(Δ + 0.5) / 16.666666`**, where Δ is (b)'s tick minus (a)'s ARM_SWING tick. It is < 1.0000.<br>• **(c)**'s EVENT `ARM_SWING` reads `cooldown=1.0000` with **`aim=…,out`**, `|du|` about 4.<br>• Every BLOCK_DAMAGE line is the spiked block's, cancelled. No `BLOCK_BREAK`. No kick.<br>**MEASUREMENT (client), with every outcome:** see the table below | _(not run)_ |

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

### MS2 — the same three clicks against the INTERACTION box (SURVIVAL) — witness: the log + Ben's sight

**Steps.**
- **`/rpg minespike block`** again. Predicted: `MINESPIKE STOP mode=block reason=second_run`.
- Then **`/rpg minespike box`**, looking at the same face.
- Optional: F3+B shows entity hitboxes (client debug); record whether the box outline appears.
- Repeat MS1's **(a)**, **(b)** and **(c)**. (c) is a click beside the box, on the deepslate.

| prediction | READING |
|---|---|
| **From the jar.**<br>• **PLACE:** `MINESPIKE PLACE mode=box … spotId=<s> boxId=<s − 1>`. Ids count DOWN across every session of the boot, from MS1's `1500000000`, so `<s>` is one below the previous PLACE's lowest id. **Every `entity=` below must equal THIS PLACE's `boxId`.**<br>• **If the client sends an attack on the box,** it arrives as PACKET **`ATTACK` with `entity=<boxId>:box`** (`Play.Client.ATTACK`, F3), and the server fires EVENT **`UNKNOWN_ENTITY_ATTACK` `entity=<boxId>:box`** (handleAttack 289). **No kick**, and no `INTERACT_ENTITY`-based line.<br>• `handleAttack` does not reset the ticker (F1). So `UNKNOWN_ENTITY_ATTACK`'s cooldown is **pre-reset (1.0000 on (a)) exactly when its seq is lower than the click's SWING**, and **post-reset (0.0300 or 0.0900) when it is higher**.<br>• **(c)**: no ATTACK, only SWING / DIG_START / BLOCK_DAMAGE, with `aim=…,out`. That is B's MISS (PLAN §4).<br>**MEASUREMENT (client):** the order, and whether ATTACK arrives at all; see the table below. Record the full seq-ordered sequence for one click of each kind | _(not run)_ |

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

### MS3 — HOLDING left-click on the block (SURVIVAL) — witness: the log

**Steps.**
- **`/rpg minespike box`** again to stop. Predicted: `STOP mode=box reason=second_run`.
- Then **`/rpg minespike block`**.
- **(a)** HOLD left-click on the emerald for about 3 seconds, then release.
- **(b)** Stop, start `box`, and hold left-click on the box for about 3 seconds.

| prediction | READING |
|---|---|
| **From a prior measurement, not the jar.** Master's `RpgListeners.onStoneSwing` javadoc: *"A held left click on a block swings every tick (measured)"*, under the same condition (the stone's `BlockDamageEvent` is cancelled).<br>• **(a)** therefore gives one EVENT `ARM_SWING` per tick, each resetting the ticker. After the first, each reads **`cooldown=0.0900`** (Δ=1). **No ARM_SWING after the first reads 1.0000.**<br>• No `BLOCK_BREAK` line: the server never set `destroyPos` (F2b).<br>**MEASUREMENT (client):** the DIG_START / DIG_STOP cadence, and (b) entirely; see the table below | _(not run)_ |

| what is measured (client behaviour) | outcome | means for A | means for B |
|---|---|---|---|
| ARM_SWING cadence while held on the block | **every tick, cooldown 0.0900** | a held click can never be a charged hit, so no farming by holding | misses while holding stay misses |
| | **any held swing reads 1.0000** | **A needs a rule**: a held click could hit every 17 ticks | — |
| DIG lines while held | **one DIG_START, then a DIG_STOP after the client's local break time** | the client predicts a break: see MS4 | — |
| | **repeated DIG_START** | the client restarts the dig: noise, harmless | — |
| (b) held on the box | **one ATTACK, then SWINGs with no further ATTACK** | — | holding cannot repeat a hit |
| | **repeated ATTACKs** | — | B needs its own repeat guard |
| either | **the block breaks or ghost-breaks on Ben's screen** | the patch's block is not safe under click input: a STOP for the area design (MS4) | the same, for misses only |

### MS4 — the cancelled block: crack, flicker, or break (SURVIVAL) — witness: Ben's sight + the log

Read during MS1 and MS3(a).

| prediction | READING |
|---|---|
| **From the jar.**<br>• The server sends **no destroy progress and no block update** for a cancelled START (F2).<br>• A STOP for the spiked block breaks nothing (`destroyPos` mismatch, F2b).<br>• **The server's block stays deepslate: no `BLOCK_BREAK` line**, and if one appears, it was cancelled and the server sent a resync (`destroyAndAck` 19–42).<br>**MEASUREMENT (client), what Ben SEES:** see the table below | _(not run)_ |

| outcome (sight) | means for A | means for B |
|---|---|---|
| **nothing: no crack, no flicker** | the cancel is enough | the same |
| **a crack overlay grows, then vanishes** (the client's local dig) | the patch art is undermined by a crack under it: slice 1 must stop the client's dig, e.g. with a zero block-break-speed attribute in areas (a candidate, unread) | the same, but only on misses |
| **the block disappears, then reappears** (a predicted break, then rolled back on the ack) | the same as above, worse | the same, on misses |
| **the block disappears and STAYS gone** (a ghost) until relog or re-interaction | **a STOP for the area design**: the client must be stopped from predicting breaks | the same, on misses |
| **a `BLOCK_BREAK` line appears** | F2b missed a route: **STOP, finding** | the same |
| **the block is actually gone server-side** (others, or a relog, see air) | a break route bypassed both cancels: **STOP, finding** | the same |

### MS5 — the attack speed argument moves the refill (SURVIVAL) — witness: the log + Ben's sight

**Steps.**
- Stop any session, then run **`/rpg minespike block`**.
- For each of **`/rpg minespike pick 1.0`**, **`2.0`** and **`4.0`**: select the new pickaxe, wait at least 2 seconds,
  click once on the emerald (the full read), then click twice fast (the early reads). Watch the indicator refill.
- Also hover each pickaxe, and **`/give @s minecraft:iron_pickaxe`** once, for the tooltip comparison.

| prediction | READING |
|---|---|
| **From the jar and the executed arithmetic.**<br>• **The PICK lines:** `speed=1.00 modifier=-3.00 delay=20.0`, `speed=2.00 modifier=-2.00 delay=10.0`, `speed=4.00 modifier=0.00 delay=5.0`.<br>• **The full read:** ARM_SWING `cooldown=1.0000` once Δ reaches the first full ticker, **20, 10 and 5** respectively (17 at 1.2).<br>• **Each early read:** ARM_SWING `cooldown = (Δ + 0.5) / delay`. Δ=0 → **0.0250 / 0.0500 / 0.1000**; Δ=1 → **0.0750 / 0.1500 / 0.3000**.<br>• **The override does not interfere.** A pickaxe carries no weapon id, so `heldMeleeSpeed` is 0.0 and `AttackSpeedAttributeOverride` only removes its own key (PLAN §3, the override reading).<br>• **Tooltip:** a green **"N Attack Speed"** line with N = **1, 2, 4** (format `#.##`; key `minecraft:base_attack_speed`, F4). **The same Attack Damage line as the plain `/give` iron pickaxe**, because the defaults are copied.<br>**MEASUREMENT (client):** the refill Ben sees, against 20, 10 and 5 ticks (1.0 s, 0.5 s, 0.25 s) | _(not run)_ |

| outcome | means |
|---|---|
| **the refill time scales with speed, and every logged read equals `(Δ+0.5)/delay`** | the item modifier drives both the client indicator and the server charge: the pickaxe-stat design (PLAN §5, TOOLS) stands |
| **the logged reads match but Ben sees no refill difference** | the client indicator does not follow the item's attribute: a finding against Ben's indicator ask |
| **the Attack Damage line is missing or differs from the plain pickaxe** | the defaults were not copied: a spike defect, and the exact F4 hazard |
| **a read disagrees with `(Δ+0.5)/delay` beyond a tick of phase** | something else writes ATTACK_SPEED: the override reading is wrong; STOP |

### MS6 — the spot and box are not in the server's entity list (SURVIVAL) — witness: the log

**Steps, while MS2's `box` session is up** (re-run `/rpg minespike box` if needed), standing next to the block:
1. `/execute if entity @e[type=minecraft:interaction,distance=..8] run say MS6-FOUND-interaction`
2. `/execute if entity @e[type=minecraft:item_display,distance=..8] run say MS6-FOUND-item_display`
3. **Positive control:** `/summon minecraft:interaction ~ ~ ~`, then step 1 again.
4. `/kill @e[type=minecraft:interaction,distance=..8]`

| prediction | READING |
|---|---|
| **From the code.** The spike only SENDS spawn packets (`MineSpikeVisuals`) and never adds an entity to a world.<br>• Steps 1 and 2: **no `MS6-FOUND` line in the log** (chat: `Test failed`).<br>• Step 3: **exactly one `[BaronVonYeetus] MS6-FOUND-interaction`** line. **This is the control: it shows the instrument can find an interaction when one exists.**<br>• Step 4 kills exactly the summoned one (`Killed Interaction`). The packet box is untouched, because the server does not know it | _(not run)_ |

### MS7 — the custom patch and spot, pack DECLINED and then ACCEPTED (SURVIVAL) — witness: Ben's sight + the log

**Steps.**
- Stop any session.
- **(a) Decline:** run **`/rpg minespike pack`** and press **No** on the prompt. Then look at the deepslate's north face
  and run **`/rpg minespike model`**, and record what you see. Then run `/rpg minespike model` again to stop.
- **(b) Accept:** run **`/rpg minespike pack`** again and press **Yes**. If no prompt appears, see the table. Then run
  **`/rpg minespike model`** on the same face.

| prediction | READING |
|---|---|
| **From F6.**<br>• **Each offer logs** `MINESPIKE PACK url=http://127.0.0.1:<port>/mining-spike.zip sha1=<40 hex> bytes=<n>`. The sha1 is the same on both offers (the zip is deterministic: `MineSpikePackTest`).<br>• **(a)** gives EVENT `PACK_DECLINED`, and **no disconnect** (`require-resource-pack=false`; the server kicks only on DECLINED with it true).<br>• **(b)** gives `PACK_ACCEPTED`, then `PACK_DOWNLOADED`, then **`PACK_SUCCESSFULLY_LOADED`**.<br>• **`model` logs** `MINESPIKE PLACE mode=model … boxId=-` and `MINESPIKE PATCH patchId=<spotId − 1> model=rpg:mining_patch spotModel=rpg:mining_spot` (the spot takes its id first, then the patch).<br>**From F5.** The spot is an ITEM_DISPLAY at the PLACE line's `spot=` position, showing `item_model rpg:mining_spot`, scaled 3/16 × 3/16. The patch is centred on the face, 0.0005 off it, showing `rpg:mining_patch` scaled 2 × 3. Both are rotated so the model's +Z faces NORTH (`FacePoint.rotation`).<br>**MEASUREMENT (client), with every outcome:** see the table below | _(not run)_ |

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

## NO MUTATIONS AT BOOT

The spike's guards were mutation-checked at build time, before this file was committed:
- `FacePointTest.eachRotationTurnsPlusZOntoTheFaceNormal` reddened under an EAST sign flip (marker 1, delta 1;
  restored, 0 markers left).
- `MineSpikePackTest.filesNamesEveryFileInTheSourceDirectoryAndNothingElse` reddened with one `FILES` entry deleted
  (delta 1; restored).

Nothing is mutated at boot.
