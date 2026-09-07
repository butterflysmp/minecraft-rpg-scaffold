# GATE — the vanilla damage boundary

**This file is the source of truth for this slice's boot gate.** It is versioned with the code
because, for every behaviour listed below, **these rows are the only check that exists anywhere in
the project.** 1300 tests pass with any of them deleted, and not one of them can watch a reconcile
tick revert a number.

## Status after the 2026-09-07 run — the three conversion rows are GREEN

**Run and passed 2026-09-07 — named, eight:** **D1b**, **D2b**, **D3e**, **D3b′**, **D4c** (both
subjects AND the control), **D5**, **D5b**, **D7**, **D8**.

**Earlier:** B1, B2 (partial), D3d, D16 run; D3 failed two ways and drove commits 1 and 2; D1, D2 and
D6 are struck through or superseded — D1/D2 passed while being **incapable of seeing the 5x error**,
which is why they were re-specified.

**Still owed:** B2's death half, and **D3b″, D4, D5c, D9, D10, D11, D12, D13, D14, D15**.

**763 STEP2 events across the boot, ZERO exceptions and zero `Could not pass event`.**

> **Both commits are now witnessed end to end.** Commit 1's window: D3e's 7 landings against 44
> absorbed, and D3b′'s four consecutive 10-tick gaps. Commit 2's conversion: k confirmed **two ways on
> every line** at three different factors — **5** (player), **1** (untagged mob), **18** (the Knell) —
> and the same fall taking **60% of bar from all three**.

Nothing here is a pass until someone says it was **observed**, and half-run is not passed.

### THE 2026-09-07 RUN'S OWN CONTAMINATION AND CORRECTIONS

**A leftover IRON GOLEM killed the first Knell mid-row** (*"Knell was slain by Iron Golem"*, 00:25:37).
Twelve golems were still in the world from the earlier lava-multiplicity testing, and they aggro on
wither skeletons. **This is the precondition class recorded one commit earlier — "what else is in the
world" — biting the very next row.** Enumerating the hazard and then not applying it to the row in
hand is worse than never having written it. **Clear them (`/kill @e[type=iron_golem]`) before the
control drop.**

**A "≈2 defense" reading was inferred and was WRONG.** Drowning drops read 9.80 against `applied=10`,
and that was attributed to armour. **The lava run contradicted it in the same capture** — its first
landing dropped exactly 20.00 against `applied=20`, which is defense **0**. The real cause is
`HealthRegen.BASE_PER_SECOND = 0.2` on a `REGEN_PERIOD_TICKS = 20` loop: drowning ticks every 20 ticks,
so every interval is `−10 +0.2 = −9.8`. **Two data sets disagreed and only one was explained** — and
the operator's "I don't have any armor" was the correction, not the confirmation.

**The monitor filter missed a death it was armed for.** *"BaronVonYeetus fell from a high place"*
matches neither `died` nor any other pattern in the watch, so D1b's pass was nearly recorded as a
survival. A filter that cannot see the outcome it was built for is the same defect as a blind grep,
one layer out.

### An unplanned finding the traces gave for free

```
tick=21656  cause=LAVA  raw=4.0000  toDeal=3.0000
```

**The ratchet returning a DIFFERENCE, in game.** A `FIRE_TICK` (raw 1) had landed first, so lava's 4
arriving inside that window admitted only the excess — **3**. That is vanilla's `amount > lastHurt`
rule reimplemented in our units, firing on the lava-while-burning case that decided the per-victim
design. **No unit test could show it and no row asked for it.**

### B1 FOUND A SECOND LIVE DEFECT, and the prediction was wrong in a way worth keeping

The plan predicted creepers might deal **~0** because `attackDamageOf` returns `0.0` for a mob with
no `ATTACK_DAMAGE` attribute. **It reads 1, not 0** — so the attribute is present, the reasoning
about its absence was simply wrong, and **the conclusion survived anyway**: a point-blank creeper
costing 1 of a player's 100 HP is harmless in exactly the way "0" would have been.

**Being right about the consequence is not being right about the mechanism.** Had B1 not been run,
the record would have carried a confident, specific and false claim ("creepers have no
`ATTACK_DAMAGE`") that happened to point at a true symptom — the shape `NEXT.md` records as a single
measurement generalised into a second claim that was never itself measured. **B1 is why this file
says 1.**

## How to use it

- **NAME THE ROWS YOU ARE ABOUT TO RUN, BEFORE YOU RUN THEM.** A count against an unnamed set is not
  an answer, however precise the number looks.
- A row marked **figure** wants a written observation, not a tick. **A figure row has no checkbox** —
  its text field is what marks it complete, and a blank field is UNRUN, not passed.
- A row marked **sole witness** names the behaviour it is the only check for. Skipping it is not
  reduced confidence; it is zero.
- A row marked **discriminating** fails if the specific defect the slice exists to prevent is present.
- A row marked **control** exists to stop another row crediting coverage it does not have.

## Rule 4 applies to every row here

*A gate row can be impossible, or real but non-discriminating, and both credit coverage that does not
exist.*

---

## FIRST: B1 and B2, on master's behaviour

**These two establish a before-state, and D7's expectation is written from B1.** The branch does not
destroy it — `master` is still exactly where it was — but the working tree no longer carries it, so
they need a checkout:

```bash
git checkout master
./scripts/dev-server.sh --refresh-content     # boots master's behaviour
#   ... run B1 and B2, write the figures in below ...
git checkout feat/vanilla-damage-boundary
./scripts/dev-server.sh --refresh-content     # boots the slice
```

**B1 is not a formality.** `MobNameplateManager.attackDamageOf` returns `0.0` for a mob with no
`ATTACK_DAMAGE` attribute and says so in its javadoc — but whether a **Creeper** has that attribute is
a vanilla fact nobody here has measured. If B1 reads ~0, creepers have been dealing no real damage to
players and D7 is *"check creepers now work"*. If it reads a real number, D7 is *"check we did not
break creepers"*. **Write whichever one B1 says is true; do not write both and pick afterwards.**

| # | action | expect | marks | figure |
|---|---|---|---|---|
| **B1** | *On master.* Let a creeper explode on you. Does the custom HP number move, and by how much? | *figure* | figure · **baseline; D7's expectation is written from this** | **RUN 2026-09-06 — 1 custom HP, AT POINT-BLANK.** `❤ 100 → 99`; the vanilla hearts did not visibly move. **The hypothesis was wrong in the letter and right in the substance:** the creeper's seeded `ATTACK_DAMAGE` is not absent (that would have read 0), it is **1** — so `onMobMeleeAttack` *was* claiming the blast, and pricing a point-blank creeper at **1% of a player's health**. Vanilla point-blank is roughly 22. **Creepers have been very nearly harmless, and this is a SECOND LIVE DEFECT** found by the same investigation |
| **B2** | *On master.* Shoot a mob with a plain vanilla bow. Does its nameplate number move? | *figure* | figure · baseline for the PROJECTILE gap | **RUN 2026-09-06, PARTIAL. The nameplate number did NOT move** — the recorded `PROJECTILE` gap is real and confirmed in game. **The death half is UNRUN** (only one or two arrows fired), so *"does it die with its nameplate still reading full"* is still owed and must not be written up as observed. Nothing else depends on it |

---

## The rows

| # | action | expect | marks | result |
|---|---|---|---|---|
| **D1** | ~~Take fall damage. Watch the number for a full 2s.~~ **RE-SPECIFIED — see D1b.** | ~~Number drops and stays down.~~ | ~~discriminating~~ | **PASSED 2026-09-06 AND COULD NOT HAVE SEEN THE 5x ERROR.** "Does the number move" is true at any scale factor. Kept, struck through, because *"was this checked"* and *"it passed"* are different answers |
| **D1b** | **D1's replacement.** Full custom HP, no armour. Fall from a height vanilla kills outright from (~23 blocks). | **The player DIES from the one fall.** | discriminating · **its PASS is impossible without the conversion**: 25 unconverted is a quarter of the bar, and the measured pre-fix reading left the player alive | **PASS 2026-09-07.** `raw=20 -> applied=100.0000` against a 100.0 max -- exactly lethal, not merely enough. "BaronVonYeetus fell from a high place", 00:27:14 |
| **D2** | ~~Hold yourself underwater until you drown.~~ **RE-SPECIFIED — see D2b.** | ~~Number drops and stays.~~ | ~~binary~~ | **PASSED 2026-09-06 AND COULD NOT HAVE SEEN THE 5x ERROR**, same reason |
| **D2b** | **D2's replacement, and it is a STOPWATCH row.** Full custom HP, no armour. Drown, timing it. | **~10 seconds, vanilla's own time to drown** — not the ~100 s measured before the fix. **A reading near ~2 s means the conversion was applied TWICE.** | discriminating · time-to-death is the only form of this row that can see a scale error · **NAMED WITNESS for mutation M5**, which has no unit test | **PASS 2026-09-07 — 200 ticks, EXACTLY 10.0 s.** 11 events, every one `raw=2.0000 → toDeal=2.0000 → applied=10.0000`. **M5 excluded**: a double conversion would read `applied=50` and ~2 s. The k control agrees both ways on all 11 lines — `10/2 = 5` and `100.0/20.0 = 5`. `iFrames=0` throughout at a clean 20-tick spacing: the window correctly did nothing to a cause it does not gate |
| **D3e** | **Stopwatch, lava.** Full custom HP, no armour, straight into lava. | **~2.5 seconds** — 4 raw per 10-tick window × k=5 = 40 custom/sec against 100 HP. **~0.5 s means M5**; a reading anywhere near 20 Hz means commit 1's window regressed. | discriminating · **M5's second witness, and the only row that also re-checks the D3a cadence after the conversion** | **PASS 2026-09-07 — 51 ticks, 2.55 s.** And it re-witnesses commit 1: **51 events, 7 LANDINGS, 44 ABSORBED.** Lava still attempts damage every tick — the poisoned ratchet still admits everything — so **our window is now the only thing pacing it**, which is the whole design. Landing gaps 10/10/10 and one 7+13 pair summing to 20, one of them at `iFrames=10`, the exact tick vanilla's normal path re-arms. `raw=4 → toDeal=4 → applied=20`; **M5 excluded** (would be ~0.5 s) |
| **D3** | Stand in lava for ~3 seconds. | Number drops in roughly **6 steps, not ~60** — vanilla's i-frame cadence survived the ride. | discriminating · **sole witness that REROUTE tokens rather than cancels** | **FAILED 2026-09-06, THREE WAYS — GATE STOPPED HERE.** (a) **4 damage per TICK, not per 10 ticks** — amount right, cadence wrong by 10x. (b) **the player could not die or respawn.** (c) the test world was left unusable. **D3 was written as the sole witness for the tokening decision and it FALSIFIED it.** See the two diagnoses below |
| **D4** | Drop a mob off a ledge while looking at it. | Its nameplate drops by the fall damage. | binary · the mob half of the boundary | |
| **D4b** | ~~`/rpg spawn knell`, get it into lava, time it.~~ **WITHDRAWN BEFORE IT WAS RUN — THE SUBJECT IS FIRE-IMMUNE.** | ~~20% of its bar per tick~~ | **NEVER A TEST.** `knell.yml` is `base_entity: wither_skeleton`, and wither skeletons are fire-immune; `is_fire` in `paper-26.1.2.jar` contains `minecraft:lava`. **The Knell would have taken NOTHING while the control died normally, and that reads as "the conversion does not reach tagged mobs" — the exact opposite of the truth.** A false negative pointing at a specific wrong diagnosis is worse than no reading. Replaced by **D4c**, which is the same property on a cause both subjects feel | |
| **D4c** | **D4b's replacement, and it also covers D1b — TWO DROPS, THREE SUBJECTS.** **PRECONDITION: THE OPERATOR WEARS NO ARMOUR.** Not a note — the row is invalid otherwise, see below. Stand a `/rpg spawn knell` and an **ordinary wither skeleton** (same base entity, so the ONLY variable is the `mob_id` tag) on a pillar with you. **Drop 1: ~15 blocks. Drop 2: ~23 blocks.** | **Drop 1 — all three land on exactly 60% of bar** (player 60/100, skeleton 12/20, Knell 216/360). **Drop 2 — all three die.** Under the shipped defect the skeleton dies to one fall and the Knell needs **eighteen**. | discriminating · **an 18x change on shipped content** · the untagged skeleton is the control, and this is `MobSeeding`'s documented symmetric property witnessed in game rather than by proxy | **PARTIAL 2026-09-07 — THE KNELL PASSED BOTH DROPS; THE CONTROL NEVER RAN.** `raw=12 → applied=216.0000` on 360 max = **60.0%**, beside the player's `raw=12 → applied=60.0000` on 100 = **60.0%**. Lethal drop: `raw=20 → applied=360.0000` on 360, and *"Knell fell from a high place"*. k confirmed both ways — `216/12 = 18` and `360.0/20.0 = 18`. **COMPLETED 2026-09-07 — THE CONTROL RAN AND k=1 EXACTLY.** Untagged wither skeleton, same base entity: `raw=12 → applied=12.0000`, `customMax=20.0`, alive at **8/20 = 60%** — the same fraction as the Knell's 216/360 and the player's 60/100. Lethal drop `raw=20 → applied=20.0000` on 20. **So the `mob_id` tag is the ONLY variable: 18x for the tagged mob, 1x for an ordinary one of the same species.** `MobSeeding`'s documented symmetric property, witnessed in game rather than by proxy. *(The first Knell was killed mid-row by a leftover IRON GOLEM — see below.)* |
| **D5** | One melee swing on a mob. | Nameplate drops by the weapon's number **exactly once**. | discriminating · **`ENTITY_ATTACK`'s PASS arm; a doubled number means the arm is wrong** | **PASS 2026-09-07.** Operator-observed, and **necessarily so — a PASS arm produces NO `[STEP2]` line by definition**, so the log cannot corroborate it. Its silence in the trace *is* the arm passing |
| **D5b** | Sweep-attack **three mobs at once** with a sweeping sword. | Each nameplate drops by the sweep number **exactly once**. | discriminating · **`ENTITY_SWEEP_ATTACK`'s PASS arm — the second one, and it has its own handler** | **PASS 2026-09-07**, operator-observed, same reason. **Both PASS arms witnessed SEPARATELY** — which is exactly what one shared row could never have done |
| **D5c** | Put vanilla Thorns on armour (anvil + book), wear it, let a mob hit you. Then repeat **with one of our Thorns shields also raised**. | Nameplate drops from the thorns hit, credited to you. **WITH BOTH: EXPECT TWO REFLECT NUMBERS OFF ONE HIT. THAT IS CORRECT — READ THE NOTE BELOW BEFORE JUDGING IT.** *Figure: are the two readable, or do they collide?* | figure · vanilla armour Thorns is reachable (the anvil is not hijacked) and is a **separate source** from our shield Thorns | |
| **D6** | **RUN THIS SOMEWHERE RECOVERABLE — over water, or with an escape ready.** A failed D6 reproduces D3's stuck state, and the second time costs more because the world is already dirty. Full custom HP, fall from lethal height onto ordinary ground **beside deep water**. | The player **dies AND gets a respawn screen**. Both halves — D3 showed the death message can fire without the screen appearing, so "it died" is not the whole row. | discriminating · **now the row the whole diagnosis turns on** | **PASS 2026-09-06. A single lethal fall kills.** This is the measurement that **pins D3b to REPEATING causes** and makes the slice a repair rather than a redesign: the deferred kill completes fine when nothing competes with it, and loses only when a second damage event queues a floor render behind it. The four code reads predicted exactly this |
| **D3b'** | **Bury yourself in sand or gravel and suffocate to death.** Full custom HP, somewhere recoverable. | The player **dies AND can respawn.** | discriminating · D3b's INDEPENDENT witness, and it must exist before either fix lands · suffocation repeats and is lethal, and its cadence is **untouched by anything the D3a fix changes** — lava's is not | **PASS 2026-09-07.** *"suffocated in a wall"* 00:36:24, from `customNow=4.6` with `applied=5.0000`. **42 events, 5 landings, 37 ABSORBED, gaps 10/10/10/10** — four consecutive perfect windows, the cleanest cadence trace of the run. `raw=1 → applied=5`, k=5. **All three repeating causes have now killed and respawned on this build** |
| **D3b''** | **Drown to death**, reusing D2's setup. | The player **dies AND can respawn.** | **the THIRD point in the matrix, at a different (amount, interval) again** · cheap, because the setup already exists | |
| **D7** | Creeper blast on a player, **at point-blank**, unarmoured. Then repeat from ~4 blocks. | **CHECK CREEPERS NOW WORK** — written from B1, which measured **1** at point-blank. Expect **substantially more than 1** (vanilla's own blast damage, tens not units), and the ~4-block shot to read **visibly smaller** than the point-blank one. | discriminating · **its PASS is impossible without the reroute**: 1 was the old number, and distance-scaling cannot come from a flat melee stat | **PASS 2026-09-07, AND FAR LARGER THAN THE ROW PREDICTED.** Point-blank `raw=20.3064 → applied=101.5320` against a 100 max — **lethal outright**, where B1 measured **1**. Distance scales cleanly: `raw=11.2937 → 56.47`, then `raw=1.0 → 5.0` further out. The row expected "tens"; the real blast is ~100x B1's reading |
| **D8** | Take a creeper blast twice: shield **raised**, then **down**. Record both numbers. | Raised is smaller, by what the shield's DR says. | **sole witness** that shields survived the melee gate | **PASS 2026-09-07 — and vanilla FULLY blocked while ours did not.** Shielded blast: `raw=4.2871 final=0.0000 applied=13.9329`. Vanilla's `final=0` is a total block; ours took 13.93 of the unshielded 21.44 = **35% DR**. **The shield DID reach an explosion**, which is this row's whole job. The vanilla-total-block vs our-partial-DR gap is new evidence for the standing *which causes should mitigation touch* question, not a defect in this slice |
| **D9** | Take a warden's sonic boom. | Number moves. | binary | |
| **D10** | Spawn a creeper and never let it melee anything. Look at it. | Nameplate present, with HP. | **control** — proves the cause gate did not withdraw tracking | **PASS 2026-09-07.** The `ENTITY_ATTACK` gate on `onMobMeleeAttack` withdrew nothing: seeding still happens at `EntityAddToWorldEvent`, exactly as the code read predicted before the gate was written |
| **D11** | `/kill` yourself. Then walk off into the void. | Both kill, and **promptly**. | binary · the two PASS-by-removal arms | **PASS 2026-09-07, and the LOG corroborates it in the strongest available form.** *"BaronVonYeetus was killed"* 00:41:21 and *"fell out of the world"* 00:41:49 — **and `cause=VOID` and `cause=KILL` produced ZERO `[STEP2]` lines across all 769 events.** So they killed promptly AND never entered the reroute at all, which is exactly what the PASS arms claim. Had they been rerouted, the token would have made `/kill` a no-op and the void survivable |
| **D12** | Stand in lava ~3s, **then** take one ordinary fall. | *figure, **both separately**: readable / noisy / unreadable* | figure · **the self-facing popup decision, judged on the REPEATING case** | |
| **D13** | Let a zombie chase you, then let it take fall damage. | It **keeps chasing you** — it did not retarget itself. | discriminating · the self-aggro guard's only witness | **PASS 2026-09-07.** The zombie kept chasing. Operator-observed and **necessarily so — there is no log line for a mob's target**, which is precisely why this row is the guard's SOLE witness: `BukkitCombatant`'s `!entity.equals(attacker)` clause has no other check anywhere in the project |
| **D14** | Regression: thorns-on-melee, shield-block-on-melee, damage popup, mob death, player death, respawn. | All unchanged. | binary · **melee and sweep are NOT judged here** — D5/D5b own them | |
| **D15** | Light a mob with a fire weapon and watch its nameplate. | It drains while burning. Rate: *figure* | figure · the Scorch side effect, arriving as a consequence rather than a feature | |
| **D16** | Read the boot log. | Silent — no new warnings. | binary | **PASS 2026-09-06**, and run as a DIFF rather than a read. Master's build was booted first and its 29 unique `WARNING:`/`SEVERE:` lines captured; the slice's boot produced **the same 29, with zero new** (`comm -13` empty) and **zero** exceptions or `Could not pass event` lines. All 29 are pre-existing negative-content fixtures (`bad.yml`, `tin.yml`, `cursed.yml`, `odd.yml`, `bogus.yml`, …) plus JVM/native-access noise. A read from memory could not have told 29 from 30 |

---

## Why D12 is written against lava and not a fall

One fall number over your own head is obviously fine, and a row that only tested that would read as a
pass and settle nothing. Lava damages roughly every 10 ticks, and **tokening preserves that cadence**
— so three seconds in lava is roughly **six popups stacked in the same place.** That is the case the
decision actually turns on.

If lava reads noisy and a fall reads fine, the answer is probably a per-cause rule rather than
"always" or "never" — but that is a decision to take **from two figures**, not in advance. Nothing has
been built for it.

## D3a — THE TOKEN DOES NOT PRESERVE I-FRAMES FOR A REPEATING CAUSE. IT DESTROYS THEM.

**Read out of `run/versions/26.1.2/paper-26.1.2.jar` with `javap -c -p`, not reasoned.**
`LivingEntity.hurtServer`, offsets 136–191:

```
136  this.invulnerableTime  >  this.invulnerableDuration / 2   ... else GOTO 248 (normal path)
152  source.is(DamageTypeTags.BYPASSES_COOLDOWN)               ... if true GOTO 248
162  amount  >  this.lastHurt ?
168      if NOT greater  ->  171: iconst_0 / 172: ireturn      ... FULLY IGNORED, NO EVENT
173      if greater      ->  handleEntityDamage(source, amount, this.lastHurt)   <- FIRES A FRESH EVENT
188                          computeAmountFromEntityDamageEvent(event) -> fstore_3
239/311                      putfield lastHurt <- fload_3      ... THE POST-EVENT AMOUNT
```

**Both `lastHurt` writes take `fload_3`, which offset 191 overwrote with the amount computed FROM OUR
EVENT.** So `setDamage(0.01)` sets `lastHurt = ~0.01`. Every subsequent 4-damage lava tick then
satisfies `amount > lastHurt`, skips the `ireturn` at 172, and **raises a fresh `EntityDamageEvent`**,
which this slice's handler reroutes in full. The i-frame window is still open; it just stopped
suppressing anything, because we told vanilla the last hit was worth a hundredth of a heart.

The event is constructed with `lastHurt` passed **separately** as the third argument — the
`INVULNERABILITY_REDUCTION` modifier the API documents as sitting "right under BASE". So
`event.getDamage()`, which is `getDamage(BASE)`, reads the **full 4**, not the difference. Mechanism
and measurement agree: **4 per tick, 20 times a second.**

**The three shipped riders never met this because melee is player-paced.** Lava is the first
*repeating* cause ever tokened in this project. **The entire argument for tokening over cancelling was
that i-frames survive** — and this row was written as its sole witness precisely so the claim would be
tested rather than believed. It was, and it is false.

**Cancelling is NOT automatically the answer** — it has its own i-frame problem, which is why tokening
was chosen. This needs a fresh diagnosis, not a swap to the other option.

## D3b — THE FLOOR THAT STOPS THE TOKEN KILLING YOU ALSO STOPS YOU DYING

**Four reads, no inference:**

1. `EntityScheduler.run` is documented **"Schedules a task to execute on the next tick"** — so
   `PlayerHealthSystem`'s `setHealth(0)` is *always* deferred by a tick, never immediate.
2. `CombatantStats.damage` calls `listener.onChange(...)` **unconditionally**, even when the state did
   not move.
3. `HealthState.damage` returns `reachedZero` **only on the transition** (`before > 0 && current == 0`).
   A hit on a combatant already at 0 returns **false**.
4. So the *next* damage event takes `onChange`'s **render** branch, and `HeartBarRenderer` floors the
   write at `MIN_LIVE_HEALTH_POINTS = 1.0`.

**The kill is queued for next tick; the very next lava tick queues a render that puts health back to
1.0.** `PlayerHealthSystem:99` carries the comment `// real death; no floor render competes` — **true
for the population it was written against** (single mob hits, `/rpg damage`, all i-frame-paced) and
**false for any repeating source**.

> #### CORRECTION, 2026-09-06 — the first draft of this section was WRONG, and the server log said so
>
> It read *"vanilla's death check never sees a zero."* **It does.** The boot log carries six
> `BaronVonYeetus tried to swim in lava` lines — 14:32:49, 14:35:28, 14:35:39, 14:50:05, 14:50:30,
> 14:50:41 — and that is a vanilla death message, so `die()` ran and `PlayerDeathEvent` fired **every
> time**.
>
> **What actually fails is the RESPAWN TRANSITION, not the death.** The operator reported no respawn
> screen, and the log shows why the state looked like "cannot die": each relog puts the player back at
> the *same* coordinates (9.76 → 9.79 → 10.22 → 10.24 → 10.26, drifting as flowing lava pushes the
> body), `PlayerHealthSystem.onJoin` re-registers custom HP at **100**, and at D3a's 4-per-tick that is
> **gone in ~1.25 s** — which is exactly the ~1 s between each join line and the next death message.
> A death that fires, does not hand over a respawn screen, and is re-entered on every login reads
> from inside the game as "I cannot die or respawn", and that is what was reported.
>
> **The mechanism above may still be the cause — a post-death render restoring health to 1.0 would
> leave a player dead server-side and alive by health, which is a state with no respawn screen — but
> it is now a HYPOTHESIS with contrary evidence attached, not a finding.** It was written from three
> code reads that were each individually correct, and the conclusion drawn from them was not.
> `CLAUDE.md`: an explanation for why a signal does not count is a hypothesis; test it.
>
> **D6 is the test**, and this correction is why it must run before any fix.

> #### AND A GREEN D3b' DOES NOT ACQUIT D3b — DO NOT PRE-REGISTER THAT READING
>
> It was written here that a green `D3b'` would mean *"D3b is narrower than we think and (a) may not
> even be needed."* **That is wrong, and it is the shape of pre-registering an acquittal.**
>
> **Suffocation differs from lava in TWO variables at once** — 1 damage every tick versus 4 every ten.
> A green says only that *the reproducer is neither cadence alone nor amount alone*. **It does not
> localize, and it does not retract D3.**
>
> **The matrix that matters is KILL × REPEATING, and it currently holds exactly ONE point: D3,
> failed.** D1 and D2 tested *sticking*; D6 tested a *single-hit kill*. So `D3b'` is the **second**
> point, not the deciding one, and `D3b''` (drown to death, on D2's existing setup) is a cheap third
> at yet another `(amount, interval)`. **Two witnesses at two different points localize this; one does
> not.**
>
> If `D3b'` is green, the finding is **"suffocation does not reproduce it"**, and candidate (a) stays
> on the table until something explains why lava did.

**This is a PRE-EXISTING defect in the death path that this slice made REACHABLE**, not one it
introduced. Nothing repeating had ever drained custom HP before.

**Consequence for D6, and it is now a DISCRIMINATING row rather than a formality:** a single lethal
fall is one event, so it should still kill. **If D6 passes while D3b fails, the defect is pinned to
repeating causes specifically** — which is a much smaller fix than "death is broken". Run D6 before
believing anything wider.

## STEP2, 2026-09-06 — 149 MEASURED EVENTS, AND THE AXIS WAS NOT "REPEATING"

**The defect splits the 29 REROUTE causes by whether VANILLA GATES THE CAUSE ON THE INVULNERABILITY
WINDOW — not by whether it repeats.** Counted over the whole capture:

| cause | events | with `iFrames=0` | verdict |
|---|---|---|---|
| `SUFFOCATION` | 85 | **1** (the first) | i-frame gated → **corrupted, 20 Hz** |
| `LAVA` | 26 | **1** (the first) | i-frame gated → **corrupted, 20 Hz** |
| `DROWNING` | 32 | **32 — all of them** | **never gated at all**; clean 20-tick interval |
| `FALL` | 4 | 4 | not gated |

**`DROWNING` REPEATS AND IS NOT AFFECTED.** Its cadence comes from the air-supply timer, so the
ratchet has nothing to corrupt — which is why `D3b''` did not reproduce. **Not because it "usually
wins a race": because it is not in the race.** The reviewer's refusal to score it as a green was
right, and the reason is now mechanism rather than suspicion.

**And `SUFFOCATION` reproduces in full** — 85 events at 20 Hz with the identical signature. `D3b'`
would have reproduced had it been run before the fix.

### The token poisoning, measured rather than inferred

```
tick=191119  LAVA  iFrames=0   lastDmg=0.0000  raw=4.0000  final=4.0000   <- first hit, window shut
tick=191120  LAVA  iFrames=19  lastDmg=0.0100  raw=4.0000  final=3.9900   <- and every tick after
tick=191139  LAVA  iFrames=10  lastDmg=0.0100  raw=4.0000  final=4.0000   <- normal path, re-arms to 20
```

`lastDmg` reads **exactly 0.0100** — `TOKEN_DAMAGE`, on the wire. `raw − final = 0.01` is `lastHurt`
arriving as `INVULNERABILITY_REDUCTION`, and it **vanishes at `iFrames=10`** where the window check
fails and the normal path runs. The modifier appears on exactly the branch the bytecode said.

### `(a)` IS WITNESSED, THREE TIMES, INCLUDING BOTH 20 Hz CAUSES

`tried to swim in lava` + `died` (20:52:02); `drowned` (20:53:09); `suffocated in a wall` + `died`
(20:53:42). Deaths completed and respawns reset custom HP to 100. The suffocation tail shows the floor
doing its **remaining** job correctly — `vanillaHP=1.0000` while `customNow` ran 3.6 → 0.6 (a sliver
of a heart, floored), then death at 0.0.

### The denomination defect, measured on the two causes the operator reported

- **Player `FALL raw=25.0000`** on `customMax=100`, and **the player lived.** 25 on a 20-point vanilla
  bar is lethal; spent unconverted against a 100-scale store it is a quarter of the bar.
  `customFromHealthPoints(25, 100)` = **125** — instantly lethal, as vanilla intends. That is the
  operator's "fall → 5x", arrived at from the code.
- **Player `DROWNING raw=2.0000`** → ~1.0 dealt after Defense, so ~100 s to drown against vanilla's
  ~10 s. That is the operator's "drowning → 10% of max health".

**Both numbers are the same missing conversion, reported in different units.**

### The mob asymmetry, measured — mobs are already correct

| victim | `customMax` | `vanillaHP` per event | dealt |
|---|---|---|---|
| `PLAYER` | 100 | −0.8 (**= 4/5**, the renderer rewriting it) | 4 of 100 |
| `SPIDER` | 16 | **−0.01, the token only** | 2 of 16 = 12.5%, exactly vanilla |
| `GLOW_SQUID` | 10 | −0.01 | — |

A spider's 8-damage fall took 8 of 16 — **50%, matching vanilla exactly**. `k = 1` for mobs,
confirmed by measurement, and converting them would be the bug.

*(The 61.8 HP discontinuity at 20:53:02 is `/rpg damage 60` plus a drowning tick, not a defect.)*

### NO ARMOURED READING EXISTS. ABSENT IS NOT ZERO.

**The mitigation question is UNMEASURED, and a conclusion was drawn from its absence anyway.** Stated
here because the report that drew it did not.

`bypasses_armor.json` in `paper-26.1.2.jar` contains `drown`, `in_wall`, `fall`, `starve`, `freeze`,
`magic`, `wither`, `sonic_boom`, `on_fire`, `cramming`, `generic`, `out_of_world`, `generic_kill` and
more. **So `DROWNING`, `SUFFOCATION` and `FALL` — three of the four causes captured — bypass vanilla
armour entirely.** `LAVA` is the only armour-mitigated cause in the capture, **and the player was
unarmoured for it** (custom fell by the full `applied=4.0`, so Defense was 0).

The armoured run was **drowning**, which vanilla does not mitigate — hence `raw == final` throughout.
**Every trace in the capture shows `raw == final` apart from the token's 0.01, which is exactly what
an unarmoured run looks like.** The two pipelines were never observed disagreeing because they were
never observed on the same cause.

**Consequence:** *"possibly no `DamageWindow`, if we can stop suppressing"* rests on vanilla's
mitigation and ours agreeing, `final=` is the only instrument for that, and **its discriminating case
did not run.** That conclusion has nothing under it and no suppression change may be designed on it.

**The discriminating row is LAVA IN FULL ARMOUR** — the one cause here vanilla mitigates, measured
against a player whose Defense is non-zero, comparing `raw`, `final`, and the custom drop.

**Scope, narrower than first stated:** `bypasses_armor` also holds `on_fire`, so the armour-exposed
rerouted set is small — lava, cactus/`CONTACT`, explosions. Most of what this boundary reroutes,
vanilla never mitigated. **A green on the armoured-lava row therefore closes more than the two-pipeline
objection first implied**, but it still gates the design: it is the only cause where the two mitigation
curves can be seen on the same event.

| # | action | expect | marks | result |
|---|---|---|---|---|
| **D3d** | **Full diamond**, non-zero `⛨`. Stand in lava ~2 s. Read the STEP2 lines. | *figure:* `raw`, `final`, and the per-tick custom drop | **the only instrument for whether vanilla's armour curve and `Defense.applyDefense` agree** · gates the D3a design | **RAN 2026-09-06. THE PIPELINES DIVERGE. "STOP SUPPRESSING" IS DEAD.** See below |

### D3d — MEASURED, AND IT CLOSES THE SUPPRESSION QUESTION

Armoured `LAVA`, 18 events, two triples — and the decomposition is exact:

```
raw=4.0000  final=3.6704  applied=4.0000   x16   in-window: 4.00 - 0.01 invuln = 3.99, then -8.01%
raw=4.0000  final=3.6800  applied=4.0000   x2    normal path: 4.00 - 8.00%
```

**`final` is below `raw` by 0.33 — thirty-three times the token.** That is not the invulnerability
modifier; **it is vanilla running its own armour curve.**

**And the two pipelines take different amounts from the same event:**

| pipeline | reduction on this hit |
|---|---|
| vanilla's armour formula | **8.01%** → 3.67 |
| our `Defense.applyDefense` | **20%** → `applied` 4.0 becomes **3.2** in the store |

The custom figure is corroborated from outside the log: the damage popup read **3**, and
`Defense.applyDefense(4, 25)` = 3.2. Solving vanilla's formula backwards from 8.01% gives an ARMOR
attribute of **≈ 4.0**, and `Defense.armorBarPoints(25)` = `0.2 * 20` = **4.0**. Every number closes.

> **AND THE CAUSE OF THE DIVERGENCE IS OUR OWN DISPLAY OVERRIDE.** `ArmorBarOverride` writes our
> damage reduction *into the vanilla ARMOR attribute* so the bar reads as DR — and **vanilla then runs
> its own non-linear curve over that number.** Our linear 20% becomes vanilla's 8%. The override was
> built to change a display; it also changes vanilla's mitigation, because vanilla reads the same
> field. **A third display-becomes-truth leak in this slice**, and the only one where the leak flows
> *out* of our system into vanilla's.

**Consequence for the design:** leaving the event amount alone means vanilla mitigates on a curve that
disagrees with ours by ~2.5x on this hit, against a bar whose scale is already a projection. The
suppression must stay, and the cadence must be owned. **`DamageWindow` is not deletable.**

### THE IRON GOLEM — D3a's SEVERITY IS 20 Hz x BLOCKS CONTACTED

Grouping every `LAVA` line by `(tick, victim)` across the whole capture:

| victim | events sharing one tick |
|---|---|
| `PLAYER` | **44 groups, every one of them 1** |
| `IRON_GOLEM` | 87x1, 38x2, **309x4**, 5x6, 7x7, **15x8** |

**Up to EIGHT `LAVA` events for one victim in one tick.** The hypothesis holds: contact is per lava
block, and a golem's 1.4 x 2.7 hitbox occupies several where a player's 0.6 x 1.8 occupies one. The
player capture showing exactly one per tick was not a property of lava — it was a property of standing
in a single block.

**The arithmetic matches the observation exactly:** 8 events x 4 raw = 32 per tick against 100 custom
HP is **3.1 ticks**, and the golem died in 2-3. Twenty hertz alone predicts ~25 ticks and never
explained it.

**So D3a's severity is not 20 Hz. It is 20 Hz x blocks contacted** — up to 160 events/second on a
large hitbox.

### AND THIS IS THE PER-VICTIM RATCHET'S EVIDENCE, FROM THE WIRE

Eight same-tick events, **all of them `cause=LAVA`**. A **per-cause** window passes every one through:
they share a cause, so they share a window slot. A **per-victim** window absorbs them exactly as
vanilla does — the largest lands, the rest fall inside it.

**The per-victim decision was taken before this evidence existed, on the lava-while-burning argument.**
It now has a case from measurement rather than from preference, and the deciding case is not the one
it was chosen on.

## D4b WAS IMPOSSIBLE, AND THE QUESTION THAT WOULD HAVE CAUGHT IT WAS ASKED BEFORE THE BOOT

**Rule 4's first failure mode — a row that cannot run — and this one is worse than the milk-bucket
case, because it would not merely have failed to run. It would have produced a confident wrong
answer.**

`knell.yml` is `base_entity: wither_skeleton`. Wither skeletons are fire-immune, and
`data/minecraft/tags/damage_type/is_fire.json` in `paper-26.1.2.jar` contains **`minecraft:lava`**. So
in lava the Knell takes **nothing** while the untagged control dies normally — which reads as *"the
conversion does not reach tagged mobs"*, **the precise opposite of the truth.**

> **The reviewer raised the fire-immunity risk AND the defense question before the boot, and neither
> reached the run list I then handed over.** The row survived into a run list because it was checked
> for *arithmetic* (18x, correct) and never for *physical possibility* — `CLAUDE.md`'s first rule,
> applied to a gate table instead of to code, one slice after this file recorded that exact lesson.

**`knell.yml` sets NO defense stat** — the whole file is `base_entity`, `display_name`, `max_health:
360`. Mobs are never reconciled either, so `defenseValue` is 0 for both subjects. **D4c therefore has
an exact expected value rather than a guess**, and a Knell surviving a drop the skeleton dies to would
mean the conversion, not armour.

**Same property, on a cause both subjects feel:** fall damage is `bypasses_armor` and nobody is immune
to it.

```
fall of raw R          max    k        R=12 (~15 blocks)     R=20 (~23 blocks)
  player               100    5        60/100  = 60%         100 -> dies
  ordinary w.skeleton   20    1        12/20   = 60%          20 -> dies
  Knell                360   18       216/360  = 60%         360 -> dies
```

**Two drops cover D4c and D1b together**, and the mob pair shares a base entity so the only variable
is the `mob_id` tag.

### AND THE SAME DEFECT ONE ROW LATER: `D4c` NEEDS "NO ARMOUR" AS A PRECONDITION

**Our `Defense` applies to EVERY cause** — that is the open *"which causes should `Defense` touch?"*
question — **so the player row is armour-dependent while both mob rows are not.** Mobs are never
reconciled, so their defense is 0; the operator's is not.

At his own measured diamond value (**defense ≈ 25**: the popup read 3 against `raw=4`, and
`applyDefense(4, 25) = 3.2`):

```
                    mobs      naked                 IN DIAMOND
  ~15 blocks (R=12) 60%       60/100 = 60%          applyDefense(60,25) = 48  -> 48% taken, 52% left
  ~23 blocks (R=20) die       100 -> dies           applyDefense(100,25) = 80 -> SURVIVES at 20/100
```

**In armour the lethal drop kills both mobs and leaves him standing — which reads as "the conversion
works for mobs and not for players."** Second false negative in this table pointing at a *specific*
wrong diagnosis, in the same sitting as the first.

> **THE PATTERN, and it is why this is a precondition rather than a note: ARITHMETIC CHECKED,
> CONDITIONS UNCHECKED.** D4b's arithmetic was right and its *subject* was immune. D4c's arithmetic is
> right and its *run* is armour-dependent. **Fire immunity is a property of the subject; armour is a
> property of the run.** Both are invisible to a table that only verifies numbers, and both were
> caught by the reviewer rather than by the table. Generalised in `NEXT.md`.

## M5 HAS A NAMED WITNESS NOW — AND THE ROW FIRST NAMED FOR IT WAS BLIND

**Correction to `5c2fb0b`'s commit body.** It said mutation M5 — the conversion applied twice — *"would
surface at the gate as an instantly-lethal fall"*. **It would not.**

| cause | correct | double-converted | what the row records |
|---|---|---|---|
| **fall** | 25 → **125** (lethal) | 25 → **625** (lethal) | **a death either way — BLIND** |
| **drowning** | 10/s → **~10 s** | 50/s → **~2 s** | 10 s vs 2 s — **discriminates** |
| **lava** | 40/s → **~2.5 s** | 200/s → **~0.5 s** | 2.5 s vs 0.5 s — **discriminates** |

Fall is already lethal at 125 against a 100 max, so doubling it changes nothing a human can see. The
row would have passed under the mutation it was named as guarding.

> **RULE 4, ONE LEVEL UP — and this is the general form worth keeping:**
> **WHEN A MUTATION HAS NO UNIT WITNESS AND A GATE ROW IS NAMED AS ITS SUBSTITUTE, THAT ROW NEEDS ITS
> DISCRIMINATION CHECKED LIKE ANY OTHER ROW.** Otherwise *"guarded at the gate"* records coverage that
> does not exist — **in a commit body, where it reads as settled** and where nothing will ever re-run it.

**D2b and D3e are M5's witnesses**, both stopwatch rows, both with the mutant reading written beside
the expected one.

## M6 NEEDS A SENTENCE, NOT A ROW — AND NO ROW SHOULD EVER BE CLAIMED FOR IT

`5c2fb0b` filed M6 with M5. **They are not two of a kind.** Converting *inside* the window instead of
upstream is **algebraically invariant**, not merely unwitnessed:

```
ratchet returns  a        ->  k*a      ==  k*a
ratchet returns  a - b    ->  k*(a-b)  ==  k*a - k*b
comparison       a > b    <=> k*a > k*b        (k > 0)
```

**Identical output, so no gate row will ever catch M6 and none should be claimed.** Its guard is the
proof, and that is stronger than the body's *"guarded by the window staying upstream"*.

**The proof has one precondition, and it is where the note belongs:** k must be **constant per victim
within a window**. It is today — `customMax` does not change mid-window in practice. The note now sits
in `DamageWindow`'s denomination section beside the linearity invariant it is a sibling of, so anyone
who makes k non-constant finds what it breaks.

## D5c: TWO REFLECT NUMBERS IS THE PASS, NOT THE FAILURE

**Read this before running D5c, not after.** This slice's headline risk is doubled numbers — D5 and
D5b exist entirely to catch one — so an observer who sees two reflects off a single mob hit and has
not been warned will report a bug, and someone will then go hunting a double-count that does not
exist.

**Two numbers is two mechanisms each firing once:**

| source | what it is | where it fires |
|---|---|---|
| **our** Thorns | a **SHIELD** enchant (`thorns.yml` opens *"The second SHIELD enchant"*) | resolved in `ShieldBlock.resolve`, reflected through `applyDamage` — **raises no event** |
| **vanilla** Thorns | an **ARMOUR** enchantment | raises a real `THORNS` damage event, which `onEnvironmentalDamage` reroutes like any other cause |

Different items, different slots, different code paths. A player wearing both is wearing **two things
that reflect**, and each reflects once. That is not trap 1's shape, and it is not a defect.

**What WOULD be a defect here:** two numbers with only ONE of the two items equipped. That is the
observation worth escalating.

**What actually changed for vanilla Thorns** is the slice working as intended: before this it moved
the mob's vanilla health and the nameplate never budged — a silent no-op on truth. Now it lands in
custom HP, credited to the armour wearer.

## Why D5 and D5b are two rows

`ENTITY_ATTACK` and `ENTITY_SWEEP_ATTACK` are **two separate PASS arms handled by two separate
handlers**. One shared witness would credit the second arm with coverage it never got — rule 4 in the
form it actually ships in. A sweep landing on three mobs at once is precisely where one extra
application is hardest to notice by eye, and judging it as "unchanged" from memory across a rebuild is
not a count.
