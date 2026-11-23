# GATE — THE QUIVER IN THE NEXUS SLOT, THE VANILLA BOW AND CROSSBOW, RULING H'S POOLS AND RULING M'S HEAL

**Status: NOT RUN.** Written 2026-11-22 on `feat/vanilla-bows-content` (the content PR), before any boot. **This file
carries the ONE R0 and the ONE run sheet for the whole stack of EIGHT PRs:** #237 `feat/breacher-rework-owed`, #238
`feat/breacher-rework`, #239 `feat/nexus-never-ingredient`, #240 `feat/quiver-mirror`, #241
`feat/vanilla-bow-mechanism`, #242 `feat/enchant-quantity-gate`, #243 `feat/nexus-heal`, and the content PR, on master
`13a31174`. **`GATE-breacher-rework.md`'s rows are read on this same boot** (its own selftest command, first on the run
sheet); its R0 lines are carried into this R0.

> **2026-11-22, LATER: THE STACK IS NINE PRs.** The seat's follow-ups put a commit on #243 (`e00c6324`, the heal waits for
> the profile) and a new PR between #243 and the content PR: **#245 `feat/vanilla-shot-owed`** (the mirror's last arrow
> put back by the shot, `NEXUSRESTORE`; the refused Crossbow's charge logged, `CROSSBOWCHARGE`; `/rpg rounds`). This
> file's R0 covers all nine through **R0bR2** and **R0dR2** below; R0a names the content PR's new head. New and changed
> rows: **VX5R2** (VX5 re-predicted) and **VX10** (new, prompted twice). The text above and below is kept as committed.

**NO BOOT until the seat has diffed the whole stack AND this gate from origin, and Ben says "ready" in his own words**;
then only CC boots, by `.claude/rules/booting.md`. **NO MERGE without Ben's own words.**

**Game mode:** SURVIVAL, every row. **The rows VB0-VB3, VM1-VM10 and VX7 are AUTO; VX1-VX6 PROMPT him in gold and read
the log; VX9 is a free go and the ONE question.** *(2026-11-22, later: VX5 is read as VX5R2, and VX10, after VX7, PROMPTS twice.)*
**Time:** the setup sets night (18000) and `spawn_mobs false`; the
run's restore puts both back. **Every mob is a frozen NoAI zombie; no fight world.** The setup kills the natural
hostiles within 64 blocks by named type, stages his level at 1, and logs where his Nexus item is.

**Ben's words this gate serves, verbatim (2026-11-22), recorded in `PLAN-weapon-tiers.md` 1.12:**
- "The vanilla bow and cross bow are the wood/stone tier weapons"; "3. Bow at 14 and crossbow at 37"; "4. Both common
  and kinetic"; "5. Vanilla crafting recipe and vanilla mob drops convert"; "I'm concerned we're owning too much of the
  action of the Bow and crossbow. I want it to be as Vanilla as possible"; "I want to own the Quiver for the bow but not
  the crossbow."
- His idea: "Any time a bow is in the players hand, the menu item (our nexus menu) turns into arrows of a stack size
  equal to the remaining Quiver shots"; "1. Any weapon with a Quiver"; "2. Yes, all that changes is the item material";
  "3. Fall back and add a warning message that shows only to the player ...".
- **On ruling I: "I'd rather have our unbreaking work on the two".** The Bow's and Crossbow's wear lines are BEN'S.

## WHAT THE EIGHT PRs DO

| PR | what |
|---|---|
| #237, #238 | the burn fix, the action-bar reader, the Breacher reworked (`GATE-breacher-rework.md`) |
| #239 | ruling A: the Nexus item is never an ingredient (the star a beacon could spend: master's own defect) |
| #240 | the quiver in the Nexus slot; no mirror arrow ever real (rulings B to G) |
| #241 | the vanilla-driven shot (`shot` trigger, `cast: type: vanilla`), the stamp and the hit; ruling I's wear |
| #242 | ruling H: an enchant rolls only on a weapon that has its quantity |
| #243 | ruling M: the Nexus item heals on the beat (and, from `e00c6324`, only on a LOADED profile) |
| #245 | the mirror's last arrow put back by the shot (`NEXUSRESTORE`); `CROSSBOWCHARGE`; `/rpg rounds` (2026-11-22, later) |
| content | `bow.yml`, `crossbow.yml`, the claims, `/rpg enchant pool`, the PLAN entry, this gate |

## OPEN FOR BEN (SEAT PLACEHOLDERS unless marked)

- **"14 x force"** (linear in the draw), and the Bow's `reload_ticks 60` (short_bow's).
- **A quiver-paid arrow cannot be picked up** (ruling B); a tipped or spectral arrow is not given back.
- **The warning's wording and timing** (chat, at join, at star-off, at a quiver weapon's arrival); **the Breacher's
  mirror shows arrows** (ruling 4).
- **Knockback (ruling K):** neither weapon authors knockback of ours; vanilla's arrow does what it does on the token
  hit. Beside his ruling of 2026-09-13 that a travelling ranged weapon authors knockback. UNVERIFIED on a server.
- **The enchantment table (ruling H, NAME THE QUANTITY):**

  | enchant | quantity | Bow | Crossbow |
  |---|---|---|---|
  | power [5, 10, 15]% | the shot's damage (enchant term, stamped at the shot) | rolls | rolls |
  | deft_hands [10, 20, 30]% | reload_ticks | rolls (60) | cannot roll (none) |
  | expanded_quiver +[1, 2, 3] | quiver_size | rolls (8) | cannot roll (none) |
  | unbreaking | durability by our rule (ruling I, BEN) | rolls | rolls |

  Vanilla's Power, Punch, Flame, Infinity, Multishot, Piercing and Quick Charge are dropped at conversion and counted.
- **A known leftover (ruling L):** a crash followed by removing the plugin before he rejoins leaves mirror arrows in a
  saved inventory. With the plugin, the join's `converge` finds them by their tag and the redraw corrects them.

## WHAT WAS READ, EACH MARKED

- **Vanilla's projectile order (READ, `Player.getProjectile`):** the OFF HAND, then the MAIN HAND, then inventory slots
  0 upward. `BowItem.releaseUsing` takes the projectile (`useAmmo`) BEFORE `shoot` fires `EntityShootBowEvent`, and no
  flag gives it back (READ). `BowItem.use` fails with no projectile outside creative (READ).
- **The force (READ):** a bow's `getForce()` is its draw power, 0.1 to 1 (`getPowerForTime`; full at 20 ticks); a
  crossbow's is the constant 1. So `sent = attack x score/100 x enchant + class + level, x force, x crit`.
- **Wear (READ):** `shoot` calls `hurtAndBreak` on its own bow reference after the event; that path fires a cancellable
  `PlayerItemDamageEvent`, which the stack now cancels on a weapon that binds `shot`; ours is charged at the shot.
  Our broken floor is damage = max - 1 (`Durability.MIN_USES` 1), so vanilla still lets a broken Bow draw.
- **The Nexus item (READ):** found by its TAG everywhere (lock, drop, swap, click, drag, `converge`, the probe). The
  mirror is ARROW x the held weapon's rounds, at most 99 (`requireRange(1, 99)`). It is redrawn after a shot, at reload
  begin and end, on a held change, at join and respawn, at the star turned on, after the selftest's restore, and on
  every 10-tick stats beat. The star is put back at quit and plugin disable.
- **`/clear` (INFERRED):** vanilla's clear matches the item type, so `clear @s minecraft:arrow` takes the mirror (the
  Nexus item itself) as well as his arrows. Ruling M heals it on the next beat (below).
- **One command, two gates? (READ, `SelfTestCommand`):** the argument is ONE gate word, or `all` (every gate). So the
  run sheet has two selftest lines.
- **`probe_items` (READ):** counts every ARROW stack in storage, the MIRROR INCLUDED; the mirror's slot is the one the
  NEXUS line names, so every ITEMS prediction below lists it.
- **His level bonus at level 1 is 0** (`LevelBonus.weaponDamage` is level - 1, executed in `GATE-crystal-shard-staff.md`
  CS2 at level 50: 49). Class bonus 0: nothing in the run grants it. No enchant is ACTIVE on a freshly given weapon.

## RULING M, THE SEAT'S THREE READ QUESTIONS

1. **What leaves a switched-on star absent for 10 ticks or more (READ):** no snapshot path: the run's start, every gate
   boundary (`restoreInventory`) and every restore (finish, quit, stop, server stop) write the inventory captured at RUN
   START. Deaths keep the inventory (`setKeepInventory`). `hold` and `hold_only` move, never delete. Two scenario paths
   can: **GATE-breacher's four `clear @s minecraft:arrow`** (BR6, BR7, BR8, HF2), each in the same tick as a clear of
   crossbows, so the mirror still shows if a loaded Breacher was held (the heal re-mints at the next beat into an empty
   slot, displacing nothing); and **GATE-mining-auto's `item replace entity @s weapon.mainhand`** (16 rows) WHEN his held
   slot at run start is the star's (that overwrote the star before the mirror; the heal now re-mints the star into that
   held slot and gives the pickaxe to `MenuSafety.give`: his inventory first, dropped at his feet with a chat line only
   when full, which takes it out of his hand).
2. **Rows whose prediction the heal changes:** those two gates were READ (GATE-breacher: all 14 rows on its boot;
   GATE-mining-auto: read), so they are LISTED, NOT RE-RUN. **No row of GATE-breacher-rework (UNREAD) changes:** its one
   arrow clear, BR7R2's, follows BR6R2, which empties the Breacher, so the slot shows the star when it runs.
3. **No STOP:** nothing the heal changes needs a re-prediction that is not simple.

## HIS HEALTH

As `GATE-breacher-rework.md`'s: the setup's first `SELF customHP=<c0>/<m0>`; after the staging `SELF
customHP=<c1>/<m1>` with `m0 - m1 = 250.000` and `c1 = min(c0, m1)`. At the end, `SELFTEST RUN RESTORED … health=<s>`
then `SELFTEST RUN HEALTH saved=<s> customHP=<s>/<m0> wrote=<s>`.

## CARRIED STATE (CC, IN THIS ORDER) — A CHECKLIST, EACH LINE TICKED IN THE REPORT, NOT REMEMBERED

1. **No JVM:** `Get-Process java -ErrorAction SilentlyContinue` prints nothing.
2. **A fresh dated backup** of `run/plugins/Rpg` and `run/world` in a NEW folder
   `../rpg-run-backups/<yyyy-mm-dd>-pre-vanilla-bows` (first free `-2`, `-3`); counts, `diff -rq lines: 0` for each,
   the `ls` of `world/dimensions`, the overworld's file count.
3. `grep -n "dev-pack-on-join" run/plugins/Rpg/config.yml` prints `12:  dev-pack-on-join: true`.
4. **No fight folders:** `find run/world/dimensions/rpgfight -mindepth 1` prints nothing.
5. **IN ITS OWN MESSAGE, BEFORE THE BOOT COMMAND:** "booting now", and Ben told NOT to log in until told R0 passed.
6. `./scripts/dev-server.sh --refresh-content`. **Required deployed lines:** `weapons/bow.yml` holds `attack_damage: 14`;
   `weapons/crossbow.yml` holds `attack_damage: 37`; `weapons/` holds **24** `.yml` files.
7. R0a to R0e on **this boot's** log, matched to this boot's own `Done` line.
8. **IN ITS OWN MESSAGE: Ben told R0 passed.** Any R0 not as predicted: STOP, tell the seat, tell Ben NOT to log in.
9. *(After his `/stop`.)* `rpgfight` empty; the overworld's file count against step 2's; `All dimensions are saved`; no
   `java` process left.

## THE RUN SHEET — BEN TYPES EXACTLY THESE FOUR LINES

0. **DO NOT LOG IN UNTIL CC HAS TOLD YOU R0 PASSED.** Stand on open, flat ground with a clear view for about 15 blocks.
   **Your Nexus item must be switched ON** (Settings). **Never shoot straight up: your own arrow can hurt you.** For the
   run your level is set to 1 (health about 100); it is put back at the end. **If the second run stops early, open
   `/menu`, Settings, and check the Nexus toggle is ON** (the run turns it off and on again; the snapshot does not hold
   the toggle).
1. `/gamemode survival`
2. `/rpg selftest breacher-rework` — **ONCE**, as `GATE-breacher-rework.md` says (its last row gives you the Breacher
   for 30 seconds, then one question).
3. `/rpg selftest vanilla-bows` — **ONCE.** About a minute of automatic rows, then **gold messages ask you to shoot**:
   the Crossbow once, the Bow at full draw once, the Bow on a short draw once, the Bow once more, a broken Bow and a
   broken Crossbow once each, then **craft one Bow** at a crafting table it gives you (place it beside or behind you),
   then **the Bow on its last round once, at full draw, and once more after it gives you one arrow** (that one should
   refuse and keep your arrow), then 30 seconds with both and **one question: type your answer in chat.** Your items, time, level and health are put
   back at the end.
4. `/stop`

Nothing else. If a row stalls or aborts, do not intervene. "not sure" is NOT STATED.

## THE ARITHMETIC (SAFETY): THE MOST HITS THAT CAN LAND ON BEN TIMES THE BIGGEST HIT, AGAINST THE HEALTH HE HAS IN THE ROW

**His health in every row is `100 + G`** (the setup's second SELF line); G = 0 and defence 0 below, the worst case.

| row | what can hurt him | most hits x biggest hit | puts him down at 100 + G? |
|---|---|---|---|
| setup, VB0-VB3, VM1-VM10 | nothing the run makes: the Boltor's ray and the short_bow's bolt exclude their caster (READ, as every ranged gate); the Breacher is not fired | 0 | no |
| VX1-VX5, VX9 | **his own vanilla arrow falling on him** if he shoots upward: a player victim keeps today's REROUTE path (READ, `VanillaShots.stamped` is player-to-mob only): vanilla's arrow, about 6 to 11 at full draw (vanilla, UNVERIFIED), scaled into his custom health about x5 (INFERRED: 100 over vanilla 20): **about 30 to 55 a hit** | 2 x 55 = 110 | **yes, two arrows straight up could.** The run sheet's plain line: never shoot straight up. A death aborts the run and costs no items (keepInventory, READ) |
| VX10 (2026-11-22, later) | as VX1-VX5: his own vanilla arrow if he shoots upward; two prompted shots, the second refused (cancelled, nothing leaves) | 1 x 55 | no |
| VX1-VX9 | frozen NoAI zombies do not move or attack (INFERRED vanilla) | 0 | no |
| every row | **a natural hostile from beyond 64 blocks** (spawns off) | as `GATE-breacher-rework.md`: 7 zombie hits or one close creeper at 100 | possible, not likely |

**What the run clears, and the restore puts back:** every **bow** and **crossbow** (his own included), **arrows** (his
own included: VM8, VM10 and the VX rows clear them; the run's restore returns them), by `clear @s minecraft:<item>`.
His inventory, time, level, health and the gamerules are put back by the run's restore on finish, quit, stop and server
stop. **The star's toggle is NOT in the snapshot** (READ, `SelfTestSnapshot`): VM9 turns it off and VM10 back on; if
the run ends between them, his star stays off until he turns it on (the run sheet's line).

**The run's length** (`wait:` summed per row; recount with `tr -d '\r' <
paper/src/main/resources/selftest/GATE-vanilla-bows.yml | awk '/^[A-Za-z0-9_-]+:/{row=$1} /^  - wait:/{w[row]+=$3}
END{for (r in w) print r, w[r]}'`): setup 40, VB0 0, VB1 20, VB2 15, VB3 0, VM1 2, VM2 24, VM3 2, VM4 6, VM5 72, VM6
150, VM7 2, VM8 24, VM9 61, VM10 86, VX1 440, VX2 442, VX3 440, VX4 512, VX5 702, VX6 1200, VX7 50, VX9 600: **4,890**,
plus the ASK's 0 to 1,200, about 5 ticks per row-start clean (22: 110) and up to 40 per `kill` step (34: 1,360).
**Worst case 7,560 ticks (6 min 18 s).**

> **RECOUNTED 2026-11-22, LATER (VX10 added, VX5 renamed VX5R2, its waits unchanged), by the same command:** VX5R2 702,
> VX10 744, the rest as above: **5,634**, plus the ASK's 0 to 1,200, about 5 per row-start clean (23: 115) and up to 40 per
> `kill` step (35: 1,400). **Worst case 8,349 ticks (6 min 57 s).**

## THE SNAPSHOT, THE RESTORE AND THE MIRROR (READ)

The run's snapshot is taken in `SelfTestRun.start`, before `RUN START`: whatever his Nexus item is then (the star, or the
mirror if he held a quiver weapon) is in it. **On finish and stop**, `SelfTestRun.end` restores, then calls
`NexusMirror.redraw` from what he holds now (PR #240), so no restore leaves arrows where his star should be. **On quit**,
`onQuit` restores the inventory and `NexusMirror.restoreStar` puts the star back before the save. **On server stop**,
`onDisable` restores (the selftest's `now` path) and then puts the star back for every online player. **At every gate
boundary** `restoreInventory` writes RUN START's inventory; the next beat redraws. The heal (ruling M) only ever acts
when no Nexus item is found and his star is on. *(2026-11-22, later: and only on a LOADED profile, `e00c6324`; the run
starts long after his load, so no row's heal changes. The shot that takes the mirror's last arrow puts it back itself,
`NEXUSRESTORE`, #245: VX10.)*

## R0 — THE BUILD, FOR THE WHOLE STACK OF EIGHT. READ IT FIRST. IF ANY R0 FAILS, STOP

### R0a — the build line names the stack top's head

| prediction | instrument | READING |
|---|---|---|
| `[Rpg] Build: <sha>`, `feat/vanilla-bows-content`'s head as `git ls-remote` prints it, shortened. Not `-dirty`, not `unknown` | `Select-String -Path run\logs\latest.log -Pattern '\[Rpg\] Build:' \| Select-Object -First 1` | |

### R0b — the jar carries all eight PRs

**Every value line is checked by `ValueLine`** (the value, then end of line or whitespace and `#`). **Ruling I's cancel
and ruling M's ordering have no string a scan can tell from a wrong build**: R0a binds the jar to the commit, and VX1,
VX2 and VM8 are their server witnesses.

| prediction | instrument | READING |
|---|---|---|
| **PRESENT, 32:** GATE-breacher-rework's nine (`breacher.yml` `count: 4`, `angle_degrees: 3`, `rotation_degrees: 90`, `amount: 40`; `GATE-breacher-rework.yml` `LL9R2:`; `index.yml` `  - breacher-rework`; `ReplyReader.class` `WrapperPlayServerActionBar`; `CastSpec$Spread.class` `spread rotation_degrees must be finite`; `AbilitySchema.class` `rotation_degrees`); #239 `CraftMatrixScreen.class` `neverAnIngredient`; #240 `QuiverMirror.class` `warnNow`, `NexusMirror.class` `restoreStar`, `QuiverWarning.class` `Your Nexus menu is off`, `MirrorShot.class` `NO_PICKUP`; #241 `VanillaShots.class` `VANILLASHOT`, `ShotStamp.class` `applyTo`, `CastSpec$Vanilla.class` `Vanilla`, `WeaponDurability.class` `applyWearOnShot`, `RpgListeners.class` `onVanillaShotWeaponWear`; #242 `EnchantRoll$Requires.class` `QUIVER`, `deft_hands.yml` `requires: reload`, `expanded_quiver.yml` `requires: quiver`; #243 `NexusHealing.class` `NEXUSHEAL`, `NexusHeal.class` `heals`; content `bow.yml` `attack_damage: 14`, `quiver_size: 8`, `      type: vanilla`, `crossbow.yml` `attack_damage: 37`, `GATE-vanilla-bows.yml` `VM8:`, `index.yml` `  - vanilla-bows`, `RpgCommand.class` `Pool: `. **The controls, ABSENT, 9:** GATE-breacher-rework's four (`breacher.yml` `count: 7`, `amount: 4`, `rotation_degrees: 9`; `dragons_breath.yml` `rotation_degrees: 90`) and this stack's five: `bow.yml` `attack_damage: 1` (a prefix of 14), `crossbow.yml` `attack_damage: 3` (a prefix of 37), `crossbow.yml` `quiver_size: 8` (it authors none), `deft_hands.yml` `requires: quiver` (the wrong requirement), `boltor.yml` `      type: vanilla` (a file that authors none) | the scan below, `-JarPath run\plugins\rpg-0.1.0-SNAPSHOT.jar` | |

```powershell
param([string]$JarPath = 'run\plugins\rpg-0.1.0-SNAPSHOT.jar')
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::OpenRead((Resolve-Path $JarPath))
function Bytes($name) { $e = $zip.GetEntry($name); if ($e -eq $null) { return $null }; $s = $e.Open(); $m = New-Object IO.MemoryStream; $s.CopyTo($m); $s.Close(); [Text.Encoding]::GetEncoding(28591).GetString($m.ToArray()) }
function ValueLine($text, $l, $label) { if ($text -ne $null -and $text -match ("(?m)^" + [regex]::Escape($l) + '([ \t]*\r?$|[ \t]+#)')) { "PRESENT $label '$l'" } else { "ABSENT  $label '$l'" } }
function Has($entry, $needle) { $b = Bytes $entry; if ($b -ne $null -and $b.Contains($needle)) { "PRESENT $entry '$needle'" } else { "ABSENT  $entry '$needle'" } }
$p = 'io/github/butterflysmp/rpg/'
$b = Bytes 'content/weapons/breacher.yml'
ValueLine $b '        count: 4' 'breacher.yml'
ValueLine $b '        angle_degrees: 3' 'breacher.yml'
ValueLine $b '        rotation_degrees: 90' 'breacher.yml'
ValueLine $b '        amount: 40' 'breacher.yml'
ValueLine (Bytes 'selftest/GATE-breacher-rework.yml') 'LL9R2:' 'GATE-breacher-rework.yml'
ValueLine (Bytes 'selftest/index.yml') '  - breacher-rework' 'index.yml'
Has ($p + 'paper/selftest/ReplyReader.class') 'WrapperPlayServerActionBar'
Has ($p + 'core/ability/CastSpec$Spread.class') 'spread rotation_degrees must be finite'
Has ($p + 'paper/content/AbilitySchema.class') 'rotation_degrees'
Has ($p + 'paper/menu/CraftMatrixScreen.class') 'neverAnIngredient'
Has ($p + 'core/weapon/QuiverMirror.class') 'warnNow'
Has ($p + 'paper/nexus/NexusMirror.class') 'restoreStar'
Has ($p + 'paper/nexus/QuiverWarning.class') 'Your Nexus menu is off'
Has ($p + 'core/weapon/MirrorShot.class') 'NO_PICKUP'
Has ($p + 'paper/weapon/VanillaShots.class') 'VANILLASHOT'
Has ($p + 'core/weapon/ShotStamp.class') 'applyTo'
Has ($p + 'core/ability/CastSpec$Vanilla.class') 'Vanilla'
Has ($p + 'paper/weapon/WeaponDurability.class') 'applyWearOnShot'
Has ($p + 'paper/listener/RpgListeners.class') 'onVanillaShotWeaponWear'
Has ($p + 'core/enchant/EnchantRoll$Requires.class') 'QUIVER'
ValueLine (Bytes 'content/enchants/deft_hands.yml') 'requires: reload' 'deft_hands.yml'
ValueLine (Bytes 'content/enchants/expanded_quiver.yml') 'requires: quiver' 'expanded_quiver.yml'
Has ($p + 'paper/nexus/NexusHealing.class') 'NEXUSHEAL'
Has ($p + 'core/build/NexusHeal.class') 'heals'
$bow = Bytes 'content/weapons/bow.yml'
$xb = Bytes 'content/weapons/crossbow.yml'
ValueLine $bow 'attack_damage: 14' 'bow.yml'
ValueLine $bow 'quiver_size: 8' 'bow.yml'
ValueLine $bow '      type: vanilla' 'bow.yml'
ValueLine $xb 'attack_damage: 37' 'crossbow.yml'
ValueLine (Bytes 'selftest/GATE-vanilla-bows.yml') 'VM8:' 'GATE-vanilla-bows.yml'
ValueLine (Bytes 'selftest/index.yml') '  - vanilla-bows' 'index.yml'
Has ($p + 'paper/command/RpgCommand.class') 'Pool: '
'--- controls'
ValueLine $b '        count: 7' 'breacher.yml CONTROL-OLD'
ValueLine $b '        amount: 4' 'breacher.yml CONTROL-PREFIX'
ValueLine $b '        rotation_degrees: 9' 'breacher.yml CONTROL-PREFIX'
ValueLine (Bytes 'content/weapons/dragons_breath.yml') '        rotation_degrees: 90' 'dragons_breath.yml CONTROL-NONE'
ValueLine $bow 'attack_damage: 1' 'bow.yml CONTROL-PREFIX'
ValueLine $xb 'attack_damage: 3' 'crossbow.yml CONTROL-PREFIX'
ValueLine $xb 'quiver_size: 8' 'crossbow.yml CONTROL-NONE'
ValueLine (Bytes 'content/enchants/deft_hands.yml') 'requires: quiver' 'deft_hands.yml CONTROL-WRONG'
ValueLine (Bytes 'content/weapons/boltor.yml') '      type: vanilla' 'boltor.yml CONTROL-NONE'
$zip.Dispose()
```

### R0bR2 — the jar carries all NINE PRs (2026-11-22, later; R0b re-predicted, its text above kept)

> **R0b'S "32" IS A MISCOUNT, FOUND BY THIS ROW'S OWN RUN BEFORE COMMIT.** R0b's row says "PRESENT, 32" and its recorded
> reading says the scan "printed 32 PRESENT". Its committed script holds **31** checks before `'--- controls'`
> (counted from `ada7c809`'s file by its `Has`/`ValueLine` lines: the breacher-rework nine, #239's 1, #240's 4, #241's 5,
> #242's 3, #243's 2, the content's 7), and this row's run of the same 31 printed 31 PRESENT. So either R0b's reading was
> taken on a different script or it was miscounted; which is not knowable from the record. This row was first written
> "R0b's 32 ... 40", inheriting the error, and corrected to 39 BEFORE commit, when the run read 39. R0b's text is kept.

| prediction | instrument | READING |
|---|---|---|
| **PRESENT, 39:** R0b's script's 31, and these 8: #243's fix `NexusHeal.class` `(ZZIZ)Z` (the four-input rule); #245 `VanillaShot.class` `restoresNexus`, `VanillaShots.class` `NEXUSRESTORE player=`, `VanillaShots.class` `CROSSBOWCHARGE `, `RpgCommand.class` `Rounds: %d/%d (%s)`; content `BootLines.class` `Convert-on-arrival: `, `GATE-vanilla-bows.yml` `VX10:` and `VX5R2:`. **The controls, ABSENT, 12:** R0b's 9, and these 3: `NexusHeal.class` `(ZIZ)Z` (the three-input rule the fix replaced; not a substring of the new one), `VanillaShots.class` `NEXUSHEAL` (the restore never writes the heal's line), `GATE-vanilla-bows.yml` `VX5:` (renamed VX5R2) | the scan below, `-JarPath run\plugins\rpg-0.1.0-SNAPSHOT.jar` | |

```powershell
param([string]$JarPath = 'run\plugins\rpg-0.1.0-SNAPSHOT.jar')
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::OpenRead((Resolve-Path $JarPath))
function Bytes($name) { $e = $zip.GetEntry($name); if ($e -eq $null) { return $null }; $s = $e.Open(); $m = New-Object IO.MemoryStream; $s.CopyTo($m); $s.Close(); [Text.Encoding]::GetEncoding(28591).GetString($m.ToArray()) }
function ValueLine($text, $l, $label) { if ($text -ne $null -and $text -match ("(?m)^" + [regex]::Escape($l) + '([ \t]*\r?$|[ \t]+#)')) { "PRESENT $label '$l'" } else { "ABSENT  $label '$l'" } }
function Has($entry, $needle) { $b = Bytes $entry; if ($b -ne $null -and $b.Contains($needle)) { "PRESENT $entry '$needle'" } else { "ABSENT  $entry '$needle'" } }
$p = 'io/github/butterflysmp/rpg/'
$b = Bytes 'content/weapons/breacher.yml'
ValueLine $b '        count: 4' 'breacher.yml'
ValueLine $b '        angle_degrees: 3' 'breacher.yml'
ValueLine $b '        rotation_degrees: 90' 'breacher.yml'
ValueLine $b '        amount: 40' 'breacher.yml'
ValueLine (Bytes 'selftest/GATE-breacher-rework.yml') 'LL9R2:' 'GATE-breacher-rework.yml'
ValueLine (Bytes 'selftest/index.yml') '  - breacher-rework' 'index.yml'
Has ($p + 'paper/selftest/ReplyReader.class') 'WrapperPlayServerActionBar'
Has ($p + 'core/ability/CastSpec$Spread.class') 'spread rotation_degrees must be finite'
Has ($p + 'paper/content/AbilitySchema.class') 'rotation_degrees'
Has ($p + 'paper/menu/CraftMatrixScreen.class') 'neverAnIngredient'
Has ($p + 'core/weapon/QuiverMirror.class') 'warnNow'
Has ($p + 'paper/nexus/NexusMirror.class') 'restoreStar'
Has ($p + 'paper/nexus/QuiverWarning.class') 'Your Nexus menu is off'
Has ($p + 'core/weapon/MirrorShot.class') 'NO_PICKUP'
Has ($p + 'paper/weapon/VanillaShots.class') 'VANILLASHOT'
Has ($p + 'core/weapon/ShotStamp.class') 'applyTo'
Has ($p + 'core/ability/CastSpec$Vanilla.class') 'Vanilla'
Has ($p + 'paper/weapon/WeaponDurability.class') 'applyWearOnShot'
Has ($p + 'paper/listener/RpgListeners.class') 'onVanillaShotWeaponWear'
Has ($p + 'core/enchant/EnchantRoll$Requires.class') 'QUIVER'
ValueLine (Bytes 'content/enchants/deft_hands.yml') 'requires: reload' 'deft_hands.yml'
ValueLine (Bytes 'content/enchants/expanded_quiver.yml') 'requires: quiver' 'expanded_quiver.yml'
Has ($p + 'paper/nexus/NexusHealing.class') 'NEXUSHEAL'
Has ($p + 'core/build/NexusHeal.class') 'heals'
$bow = Bytes 'content/weapons/bow.yml'
$xb = Bytes 'content/weapons/crossbow.yml'
ValueLine $bow 'attack_damage: 14' 'bow.yml'
ValueLine $bow 'quiver_size: 8' 'bow.yml'
ValueLine $bow '      type: vanilla' 'bow.yml'
ValueLine $xb 'attack_damage: 37' 'crossbow.yml'
$g = Bytes 'selftest/GATE-vanilla-bows.yml'
ValueLine $g 'VM8:' 'GATE-vanilla-bows.yml'
ValueLine (Bytes 'selftest/index.yml') '  - vanilla-bows' 'index.yml'
Has ($p + 'paper/command/RpgCommand.class') 'Pool: '
'--- R0bR2: the nine-PR additions'
Has ($p + 'core/build/NexusHeal.class') '(ZZIZ)Z'
Has ($p + 'core/weapon/VanillaShot.class') 'restoresNexus'
Has ($p + 'paper/weapon/VanillaShots.class') 'NEXUSRESTORE player='
Has ($p + 'paper/weapon/VanillaShots.class') 'CROSSBOWCHARGE '
Has ($p + 'paper/command/RpgCommand.class') 'Rounds: %d/%d (%s)'
Has ($p + 'paper/content/BootLines.class') 'Convert-on-arrival: '
ValueLine $g 'VX10:' 'GATE-vanilla-bows.yml'
ValueLine $g 'VX5R2:' 'GATE-vanilla-bows.yml'
'--- controls'
ValueLine $b '        count: 7' 'breacher.yml CONTROL-OLD'
ValueLine $b '        amount: 4' 'breacher.yml CONTROL-PREFIX'
ValueLine $b '        rotation_degrees: 9' 'breacher.yml CONTROL-PREFIX'
ValueLine (Bytes 'content/weapons/dragons_breath.yml') '        rotation_degrees: 90' 'dragons_breath.yml CONTROL-NONE'
ValueLine $bow 'attack_damage: 1' 'bow.yml CONTROL-PREFIX'
ValueLine $xb 'attack_damage: 3' 'crossbow.yml CONTROL-PREFIX'
ValueLine $xb 'quiver_size: 8' 'crossbow.yml CONTROL-NONE'
ValueLine (Bytes 'content/enchants/deft_hands.yml') 'requires: quiver' 'deft_hands.yml CONTROL-WRONG'
ValueLine (Bytes 'content/weapons/boltor.yml') '      type: vanilla' 'boltor.yml CONTROL-NONE'
Has ($p + 'core/build/NexusHeal.class') '(ZIZ)Z'
Has ($p + 'paper/weapon/VanillaShots.class') 'NEXUSHEAL'
ValueLine $g 'VX5:' 'GATE-vanilla-bows.yml CONTROL-RENAMED'
$zip.Dispose()
```

### R0c — every Loaded line, and nothing refused

| prediction | instrument | READING |
|---|---|---|
| Exactly one of each, up to the first `joined the game`: **`[Rpg] Loaded 11 abilities, 43 visuals, 5 statuses, 7 elements, 11 enchants, 3 pools, 6 fragments, 6 aspects, 24 weapons, 1 shields, 24 armor, 5 tools, 6 accessories, 7 mobs, 1 mob elements, 6 recipes, 1 ores, 4 vein sizes`** (`GATE-breacher-rework.md`'s reading with **22 -> 24 weapons**); **`Mint-on-craft: 38 indexed, 38 claiming, of 54 gear definitions`** (36 / 36 / 52 + the two claims; derived, not measured); **`Convert-on-arrival: 9 materials {bow=bow, copper_sword=copper_sword, crossbow=crossbow, diamond_sword=diamond_sword, golden_sword=golden_sword, iron_sword=iron_sword, netherite_sword=netherite_sword, stone_sword=stone_sword, wooden_sword=wooden_sword}`** (a TreeMap, READ); the loot, fight, world-damage and smithing lines word for word `GATE-breacher-rework.md`'s boot. **No `Refusing`, `Skipping`, `SEVERE`, `Exception`, `Recipe '`; no `[Rpg]` WARN beyond `volley_stone`'s known pair** | `Select-String run\logs\latest.log -Pattern 'Loaded\|Mint-on-craft\|Convert-on-arrival\|Refusing\|Skipping\|SEVERE\|WARN\|Exception\|No fights loaded\|Recipe '''` | |

> **2026-11-22, LATER (the seat's follow-up 4): R0c's two derived lines are now MEASURED, and their numbers did not
> change.** `BootCountLinesTest` (content PR) runs the plugin's own counting steps over the real `content/`, in
> `RpgPlugin.onEnable`'s order (the four gear loaders, the recipe and smithing loaders, `CraftResultIndex.build`,
> `ArrivalClaims.build`), and prints both lines through `BootLines`, the methods the plugin now logs them with. It
> asserts the two lines above verbatim and passes; its positive control (the Bow's `craft_result` removed) read `37
> indexed, 37 claiming, of 54` and `8 materials`, as predicted. **What it cannot run without a server, measured:** the
> refusal pre-pass asks `Material.getMaxDurability`, which throws `No RegistryAccess implementation found` with no server,
> so both lines are measured GIVEN that nothing is refused; that premise is this row's own "no `Refusing`", read on the
> boot. **So R0c still fails on any line not as predicted**: the two lines as MEASURED, "nothing refused" and the Loaded
> line.

### R0d — every scenario loaded

| prediction | instrument | READING |
|---|---|---|
| Exactly one **`[Rpg] Selftest: dev server, 325 scenarios across 40 gates [melee-cell, …, breacher-rework, vanilla-bows]`**. The control: `303 scenarios across 39 gates` is ABSENT | `Select-String run\logs\latest.log -Pattern 'Selftest:'` | |

**RECOUNTED** (`SelfTestScenariosTest` asserts 325): 40 gates, `^[A-Za-z0-9_-]+:` **365**, `^setup:` **40**, so **325**.
Control: 342 + 23 (setup, VB0-VB3, VM1-VM10, VX1-VX7, VX9) = 365.

### R0dR2 — every scenario loaded (2026-11-22, later; R0d re-predicted for VX10, its text above kept)

| prediction | instrument | READING |
|---|---|---|
| Exactly one **`[Rpg] Selftest: dev server, 326 scenarios across 40 gates [melee-cell, …, breacher-rework, vanilla-bows]`**. The control: R0d's `325 scenarios across 40 gates` is ABSENT | `Select-String run\logs\latest.log -Pattern 'Selftest:'` | |

**RECOUNTED** (`SelfTestScenariosTest` asserts 326): 40 gates, `^[A-Za-z0-9_-]+:` over the `GATE-*.yml` files **366**,
`^setup:` **40**, so **326**. Control: 365 + 1 (VX10; VX5 became VX5R2, a rename, not a row) = 366; the vanilla-bows file
alone counts 23 at `ada7c809` and 24 now.

### R0e — THE ORPHAN SWEEP WITH NOTHING STAGED

| prediction | instrument | READING |
|---|---|---|
| During the enable, exactly one **`FIGHTWORLD sweep path=<abs>\run\world\dimensions\rpgfight folders=0`**, no `deleting`, no `orphan` | `Select-String run\logs\latest.log -Pattern 'FIGHTWORLD (sweep\|deleting\|orphan)'` | |

**AT HIS FIRST JOIN ON THIS BUILD (not an R0, recorded):** one `ARRIVAL minted … via=join` per plain bow or crossbow in
his saved inventory (ruling 12); none if he carries none.

## THE ROWS (`/rpg selftest vanilla-bows`) — SURVIVAL, EVERY ROW

**Common to every row:** the stats bar's `REPLY actionbar "❤ …"` lines every 10 ticks are EXPECTED and NOT COUNTED.
`L` is the slot the setup's NEXUS line names. Every damage prediction names its terms: **attack x score/100 (score 100,
staged) x enchant 1.0 (none active) + class 0 + level 0 (level 1), x force, x crit 1 (or 2, the second allowed
reading).** **Cooldowns:** a vanilla draw or charge has no cooldown of ours; every row that presses one of OUR weapons
says how its first press clears it. **"Clears arrows while the mirror shows":** stated per row.

### setup — night, no spawns, the natural hostiles killed, his level at 1, his Nexus item found (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| As `GATE-breacher-rework.md`'s setup (the kills, `SELF` twice, *HIS HEALTH*); then `DRIVE hold empty` and **`NEXUS slot=L material=nether_star amount=1`**. **If it reads `NEXUS none`, his star is OFF: every VM row reads none, and the run is NOT read for VM1-VM10** | the setup | |

### VB0 — THE SPOT (its counts describe the spot; not pass or fail) (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| `SELF …` facing east; three `BLOCKS` lines (air above, the floor, water). Clears nothing | the spot | |

### VB1 — BOTH TOOLTIPS, EVERY LINE (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| **`HELD bow BOW x1 name="Bow" [white] italic lore=["Kinetic" [white]; "Gear Score: 100" [gray, aqua]; "Quiver: 8/8" [gray]; "" []; "Ranged Damage: 14" [gray, red]; "" []; "Common Ranged Weapon" [white]]`** and **`HELD crossbow CROSSBOW x1 name="Crossbow" [white] italic lore=["Kinetic" [white]; "Gear Score: 100" [gray, aqua]; "" []; "Ranged Damage: 37" [gray, red]; "" []; "Common Ranged Weapon" [white]]`**. Clears no arrows | `golden-lore.txt`'s two blocks; `GATE-breacher-rework.md` RW1's form (the Gear Score line after the element, the minted quiver full) | |

### VB2 — CONVERSION: a plain bow, an enchanted plain crossbow, and a minted short_bow left alone (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| Within 5 ticks of each give: **`ARRIVAL minted player=<n> slot=<s> material=bow weapon=bow wear=0 vanilla_enchants=0 score=<T> via=slot-change`** and **`ARRIVAL minted … material=crossbow weapon=crossbow wear=0 vanilla_enchants=2 … via=slot-change`** (Quick Charge and Multishot dropped and counted). **No ARRIVAL line for the short_bow** (tagged: LEAVE_TAGGED). `probe_inventory` finds all three. If the component syntax is refused (its REPLY), the row says so and reads the crossbow half as UNREAD. Clears no arrows | `ArrivalClaims` (READ), `GATE-sword-convert.md`'s form | |

### VB3 — THE POOLS (ruling H): the Crossbow's has neither; the Boltor's (the control) has both (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| Three `REPLY` lines in gray, in this order: **`Pool: power, unbreaking`** (the Crossbow); **`Pool: deft_hands, expanded_quiver, power, unbreaking`** (the Boltor, the control); **the same four** (the Bow). The order is the roster's (the loader sorts the files, READ). Clears no arrows | `EnchantRollItems.poolFor`, `VanillaBowsContentTest`, `EnchantQuantityGateTest` | |

### VM1 — THE BOLTOR AT 8 ROUNDS: ARROW x 8 (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| **`NEXUS slot=L material=arrow amount=8`**. Clears arrows with the slot showing the STAR (`hold: empty` first): the heal is not part of it | `QuiverMirror` | |

### VM2 — ONE SHOT: ARROW x 7 (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| The run's first Boltor press, 20 ticks after the row starts (its cooldown 16). One Boltor cast; **`NEXUS slot=L material=arrow amount=7`**; `HELD boltor …` with **`"Quiver: 7/8"`**. Clears nothing | ruling 1 | |

### VM3 — A SWORD HELD: THE STAR (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| **`NEXUS slot=L material=nether_star amount=1`**. Clears nothing | ruling 1 | |

### VM4 — NO REAL ARROW, THE MIRROR SHOWING: THE RELOAD IS REFUSED (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| Before: **`ITEMS material=arrow count=7 gear=0 slots=[L:7]`** (the mirror's own arrows, which the probe counts). `DRIVE reload`; **exactly one `REPLY actionbar "You have no arrows -- plain Arrows load a quiver."`** in the row; **`NEXUS slot=L material=arrow amount=7`**; ITEMS unchanged. Clears nothing | ruling 3, `QuiverAmmo` (the mirror is never ammunition) | |

### VM5 — 16 REAL ARROWS: THEIR OWN STACK; THE RELOAD TAKES ONE (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| After the give: **`ITEMS material=arrow count=23 gear=0 slots=[L:7, s:16]`** (s the first empty slot; the two never merge: their components differ, INFERRED); **`NEXUS slot=L material=arrow amount=7`**. After the reload and 70 ticks (reload_ticks 60): **`NEXUS slot=L material=arrow amount=8`**, **`ITEMS … count=23 … slots=[L:8, s:15]`**. Clears nothing | rulings 1 and 3 | |

### VM6 — THE BOLTOR EMPTIED: THE STAR AT 0 ROUNDS (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| Eight presses, the first 20 ticks after the row starts and each 18 after the last (cooldown 16; VM2's press long since): eight Boltor casts; **`NEXUS slot=L material=nether_star amount=1`**; `HELD boltor …` **`"Quiver: 0/8"`**. Clears nothing | ruling 2 | |

### VM7 — THE BREACHER: ARROW x 2 (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| **`NEXUS slot=L material=arrow amount=2`** (ruling 4, SEAT PLACEHOLDER). Not fired. Clears nothing | ruling 4 | |

### VM8 — RULING M, THE HEAL: a clear of arrows takes the mirror; the beat puts it back (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| After `hold_only: bow`: **`NEXUS slot=m material=arrow amount=8`**, m >= 9 (hold_only moved it, ruling G). After 5 real arrows: **`ITEMS material=arrow count=13 gear=0 slots=[1:5, m:8]`**. **THIS ROW CLEARS ARROWS WHILE THE MIRROR SHOWS, AND THE HEAL IS ITS PREDICTION:** the clear's REPLY counts 13; within 20 ticks **exactly one `NEXUSHEAL player=<n> slot=L`**; **`NEXUS slot=L material=arrow amount=8`** (the star minted into L and redrawn in the same beat); **`ITEMS … count=8 gear=0 slots=[L:8]`** (his 5 are gone: the run's restore returns his own); `HELD bow …` **`"Quiver: 8/8"`** (the rounds unchanged) | `NexusHeal`, `NexusHealing` | |

### VM9 — THE STAR OFF: NO NEXUS ITEM, THE WARNING ONCE (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| The Settings menu's toggle (`LIME_DYE`) clicked; after 12 ticks **`NEXUS none`**; **exactly one `REPLY "Your Nexus menu is off, so your quiver's rounds are not shown in its slot. Turn it back on in /menu, Settings." [yellow]`** in the row (he carries the Bow); 40 ticks later **`NEXUS none`** and no second warning. Clears nothing | ruling 5 | |

### VM10 — THE CONTROLS, THE STAR OFF; THEN THE STAR BACK ON (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| (a) **The control for VM8:** the same clear with the star OFF: **`NEXUS none`** after 20 ticks and **no `NEXUSHEAL` line in the row** (clears arrows with NO Nexus item to take). (b) Every bow and crossbow cleared (the Bow, the short_bow, the Boltor, the Breacher): **no warning** in 30 ticks. (c) `rpg give boltor`: **exactly one warning `REPLY`** within 15 ticks (a quiver weapon arriving). Then the toggle (`GRAY_DYE`): **`NEXUS slot=L material=nether_star amount=1`** (held: empty) | rulings 5 and M | |

### VX1 — THE CROSSBOW, PROMPTED: one charged shot (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| `REPLY "<M>/<M> uses left (damage 0)"` (M the crossbow's maximum, 465 in vanilla, UNVERIFIED). `MOBSEED … zombie … max=200`; probe **`customHP=200.000/200.000`**; **`ITEMS … count=4 … slots=[1:4]`**. Gold prompt. Then **exactly one `VANILLASHOT <n> weapon=crossbow hand=main took=plain_arrow rounds=0->0 force=1.000 cancel=false refund=false paid=false damage=0->1`**; **exactly one `PLAYERHIT … source=crossbow/shot … element=kinetic sent=37.000`** (37 x 100/100 x 1.0 + 0 + 0, x 1.000 x 1; or `74.000`); probe **`163.000/200.000`** (or `126.000`); **`ITEMS … count=3`**; **`REPLY "<M-2>/<M> uses left (damage 2)"`**: the shot's 1 (ours) and the read's 1. **`damage 3` would mean vanilla's wear also landed: ruling I failed.** No VANILLASHOT line: he did not shoot; the row is UNREAD. Clears arrows with the slot showing the star | rulings 7-10 and I | |

### VX2 — THE BOW AT FULL DRAW, NO ARROW OF HIS OWN: Ben's idea read (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| `REPLY "384/384 uses left (damage 0)"`; **`NEXUS slot=m material=arrow amount=8`**; probe `200.000/200.000`. Then **exactly one `VANILLASHOT <n> weapon=bow hand=main took=mirror rounds=8->7 force=1.000 cancel=false refund=false paid=true damage=0->1`**; **exactly one `PLAYERHIT … source=bow/shot … sent=14.000`** (14 x 100/100 x 1.0 + 0 + 0, x 1.000 x 1; or `28.000`); probe **`186.000/200.000`** (or `172.000`); **`NEXUS slot=m material=arrow amount=7`**; `HELD bow …` **`"Quiver: 7/8"`**; **`REPLY "382/384 uses left (damage 2)"`** (`damage 3`: vanilla's wear landed too). Clears arrows with the slot showing the star | rulings 7, 10, I; Ben's idea | |

### VX3 — THE BOW ON A SHORT DRAW (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| **Exactly one `VANILLASHOT … weapon=bow … took=mirror rounds=7->6 force=<f> … paid=true damage=2->3`**, 0.100 <= f < 1.000; **`PLAYERHIT … sent=<14 x f>`** to within 0.007 (f is printed to three places; or twice it); **`NEXUS … amount=6`**. Rounds and damage shift by one each if VX2's shot did not happen. Clears nothing | the force rule (SEAT PLACEHOLDER) | |

### VX4 — THE BACKSTOP, PROMPTED, THEN THE BOW'S RELOAD, AUTO (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| With R the rounds the first NEXUS line reads (6 if VX2 and VX3 shot): **`ITEMS … count=<16+R> … slots=[1:16, m:R]`**. After his shot: **exactly one `VANILLASHOT … took=plain_arrow rounds=R->R-1 … refund=true paid=true`**; **`ITEMS … slots=[1:16, m:R-1]`** (his arrows read 16: the one taken was given back); **`NEXUS … amount=R-1`**. After the reload and 70 ticks: **`NEXUS … amount=8`**, **`ITEMS … slots=[1:<16-(9-R)>, m:8]`** (the reload took exactly the missing rounds from his real arrows; 13 at R = 6). **The fired arrow cannot be picked up: UNREAD (no row walks him to it).** Clears arrows with the slot showing the star | ruling 8 as B amends it | |

### VX5 — A BROKEN BOW AND A BROKEN CROSSBOW REFUSE (ruling I, Ben's) (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| **`REPLY "1/384 uses left (damage 383) -- BROKEN"`** (our floor is max - 1, READ); **`NEXUS … amount=8`**. His try: **exactly one `VANILLASHOT … weapon=bow … rounds=8->8 cancel=true … paid=false damage=383->383`**; **`REPLY actionbar "Your weapon is broken -- repair it before using it."`**; **`NEXUS … amount=8`** (nothing spent); `HELD bow …` (still in his hand). The Crossbow: **`REPLY "1/<M> uses left (damage <M-1>) -- BROKEN"`**; his try: **one `VANILLASHOT … weapon=crossbow … took=plain_arrow … cancel=true refund=true … damage=<M-1>-><M-1>`**, the broken notice; **`ITEMS … count=2`** (the loaded arrow given back); `HELD crossbow …`. No VANILLASHOT: that half UNREAD. Clears arrows with the slot showing the star | ruling I | |

### VX5R2 — VX5 RE-PREDICTED, 2026-11-22, LATER: THE REFUSED CROSSBOW READ UNLOADED (SURVIVAL)

> **Why re-predicted (the seat's follow-up 3):** VX5 predicted `refund=true` and `count=2` for the broken Crossbow, which
> would make an arrow per try if the refusal left it charged. READ (javap, the pinned server jar):
> `CrossbowItem.performShooting` sets `CHARGED_PROJECTILES` to EMPTY BEFORE `shoot` fires the event, and a cancelled event
> only removes the projectile and returns; so the refusal leaves it UNLOADED and the arrow given back is the one its load
> took. No code change; the row now reads the charge, on #245's `CROSSBOWCHARGE` line (written just before VANILLASHOT).
> The scenario's steps are VX5's, renamed. VX5's committed text above stands and is not read.

| prediction | stands on | READING |
|---|---|---|
| **The Bow, VX5's half unchanged:** **`REPLY "1/384 uses left (damage 383) -- BROKEN"`**; **`NEXUS … amount=8`**. His try: **exactly one `VANILLASHOT … weapon=bow … rounds=8->8 cancel=true … paid=false damage=383->383`**; **`REPLY actionbar "Your weapon is broken -- repair it before using it."`**; **`NEXUS … amount=8`**; `HELD bow …`. **No `CROSSBOWCHARGE` line for the Bow** (written only for a crossbow's meta) and **no `NEXUSRESTORE`** (the mirror held 8: vanilla took one of eight, the item stayed). **The Crossbow:** **`REPLY "1/<M> uses left (damage <M-1>) -- BROKEN"`**; his try: **exactly one `CROSSBOWCHARGE <n> weapon=crossbow charged=false`**, then **one `VANILLASHOT … weapon=crossbow … took=plain_arrow … cancel=true refund=true … damage=<M-1>-><M-1>`**, the broken notice; **`ITEMS … count=2`** (the arrow his load took, given back); `HELD crossbow …`. **`charged=true` would mean the refusal left it loaded, and every further try would make an arrow: the hazard this row exists for.** If he tries more than once, each try is one CROSSBOWCHARGE and one VANILLASHOT, and ITEMS still reads 2 (each load takes one, each refusal gives one back). No VANILLASHOT: that half UNREAD. Clears arrows with the slot showing the star | ruling I; `CrossbowItem.performShooting` (READ) | |

### VX6 — THE CRAFT, PROMPTED (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| Before: no bow found. **`CRAFT minted player=<n> weapon=bow material=bow score=…`**; `probe_inventory` finds it; `HELD bow …` as VB1's (Quiver 8/8, Gear Score its roll). Clears no arrows | ruling 12, `GATE-breacher.md` BR10's form | |

### VX7 — THE CONTROL: A short_bow PRESS STILL FIRES OURS (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| The run's first short_bow press, 40 ticks after the row starts (cooldown 12). Probe `200.000/200.000`; **one `PLAYERHIT … source=short_bow/right_click … sent=16.000`** (or `32.000`), **no `VANILLASHOT` line** (it binds right_click: vanilla never shoots it, READ); probe **`184.000/200.000`**; **`NEXUS … material=arrow amount=7`** (the short_bow's quiver, 8 -> 7). Clears nothing | ruling 11 | |

### VX10 — THE BOW'S LAST ROUND, THEN THE EMPTY QUIVER, PROMPTED TWICE (2026-11-22, later; the seat's follow-up 2) (SURVIVAL)

> **Why (READ, javap on the pinned server jar):** with no arrow of his own, vanilla takes the mirror, and
> `ProjectileWeaponItem.useAmmo` splits one from it and, once the stack is empty, `Inventory.removeItem` takes it out. So
> the last round's shot leaves no Nexus item when the event fires. #245 puts it back in the same tick (`NEXUSRESTORE`, into
> his locked slot L by the heal's converge; the event does not carry the slot it was taken from). Staged by `/rpg rounds
> 1` (#245, dev server only), so no eight shots are prompted. The row runs after VX7 and before VX9; `L` as everywhere,
> `m` the slot hold_only moved the Nexus item to (m >= 9, ruling G), `s` the slot the arrow give uses.

| prediction | stands on | READING |
|---|---|---|
| **Staged:** **`REPLY "Rounds: 1/8 (bow)" [green]`**; **`ITEMS material=arrow count=1 gear=0 slots=[m:1]`** (the mirror alone, which the probe counts); **`NEXUS slot=m material=arrow amount=1`**; `MOBSEED … zombie … max=200`; probe **`customHP=200.000/200.000`**. **First prompt, one shot at full draw:** exactly one **`NEXUSRESTORE player=<n> slot=L cause=shot`**, then exactly one **`VANILLASHOT <n> weapon=bow hand=main took=mirror rounds=1->0 force=<f> cancel=false refund=false paid=true damage=0->1`** (f = 1.000 at the full draw asked for); exactly one **`PLAYERHIT … source=bow/shot … element=kinetic sent=<s>`** with **s = 14 (attack_damage) x 100/100 (score, staged) x 1.0 (enchant: none active) + 0 (class: nothing grants it) + 0 (level bonus at level 1), x f (force), x 1 (crit) = `14.000` at f = 1.000**, or `28.000` (crit x 2, the second allowed reading), to within 0.007 for f < 1; probe **`186.000/200.000`** at f = 1 (or `172.000`); **`NEXUS slot=L material=nether_star amount=1`** (the star at 0 rounds, in L, not m); **`ITEMS material=arrow count=0 gear=0 slots=[]`**; `HELD bow …` with **`"Quiver: 0/8"`**; **and NO `NEXUSHEAL` line anywhere in the row.** **Second prompt** (one arrow given: **`ITEMS material=arrow count=1 gear=0 slots=[s:1]`**), one try at 0 rounds: exactly one **`VANILLASHOT … weapon=bow hand=main took=plain_arrow rounds=0->0 force=<f> cancel=true refund=true paid=false damage=1->1`**; **exactly one `REPLY actionbar "Your quiver is empty -- left-click to reload." [gray]`** in the row; **`ITEMS material=arrow count=1 gear=0 slots=[s:1]`** (his arrow given back, ITEMS still 1); **`NEXUS slot=L material=nether_star amount=1`**; `HELD bow … "Quiver: 0/8"`. No VANILLASHOT line for a prompt: that half UNREAD. Clears arrows with the slot showing the star | follow-up 2 (#245); ruling 8; `QuiverNotice` (READ) | |

### VX9 — A FREE GO, THEN THE ONE QUESTION (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| 30 seconds with both and 32 arrows; then **`ASK`: "Did the Bow and Crossbow feel like vanilla to use?"** — his words, recorded verbatim. Clears arrows with the slot showing the star | Ben's "as Vanilla as possible" | |

## GATE-nexus ROWS THIS STACK TOUCHES — OWED, UNREAD

- **Row 4 (the lock in both views):** the locked slot can now hold ARROWS; a click on the mirror must be refused as the
  star's is. It needs hand clicks in the own-inventory screen and a double chest, which the selftest cannot drive.
- **Row 5 (join convergence):** after a crash the saved inventory can hold the mirror (ruling L); the join's `converge`
  must find it by its tag. It needs real joins and a `.dat` edit.

## UNREAD BY DESIGN

A fired arrow's pickup; tipped and spectral arrows; creative; a mob's drop; dropping or moving the mirror by hand;
death with the mirror showing; the off hand (the selftest cannot drive it; a minted Bow or Crossbow fired from it is
cancelled and given back, READ); a craft that would take the star (`NexusNeverAnIngredientTest` is its witness); the Dragon's Plume
drawing on the mirror with no arrow of his own (ruling D, a behaviour change listed for Ben; its draw cannot be driven);
**vanilla's knockback on the token hit; that invulnerability frames gate the hit.**

> **2026-11-22, later: the empty quiver is TAKEN OFF this list** (the seat's follow-up 2(d)). It read "the empty quiver
> (READ: no dev command sets loaded rounds, so staging 0 takes eight prompted shots)"; `/rpg rounds` (#245) now stages it,
> and VX10's second prompt reads it. **Also listed, not read:** a Crossbow in the OFF hand LOADING from the mirror's last
> arrow while a quiver weapon is held (a load, not a shot, so no `NEXUSRESTORE`: the heal puts the item back on the next
> beat with a `NEXUSHEAL` line), and VX9's free go, where the mirror may be taken before his own arrows if its slot is
> lower than theirs (a `NEXUSRESTORE` there is his play, not predicted).

## R0b, RUN BEFORE COMMIT

**Against `paper\target\rpg-0.1.0-SNAPSHOT.jar`, built by `./mvnw -q clean package -DskipTests` from the working tree
of `feat/vanilla-bows-content` at `50628a74` plus this gate's scenario, index line and test count (2026-11-22, 20:13):**
the scan above printed **32 PRESENT** (every line the R0b row names) and **the 9 controls ABSENT**, exactly as
predicted. The boot's reading is taken again on the deployed jar.

**R0bR2, against `paper\target\rpg-0.1.0-SNAPSHOT.jar` (2,363,324 bytes), built by `./mvnw -B clean verify` from the
working tree of `feat/vanilla-bows-content` at `ff83c373` plus this commit's scenario (VX10, VX5R2) and test count
(2026-11-22, 20:53; the suite in the same build: core 2,325, storage 99, paper 1,794, total 4,218, 0 failures, 1
skipped):** the scan, extracted verbatim from this file's R0bR2 block and run with `powershell -File`, printed **39
PRESENT and 0 ABSENT** before `'--- controls'` and **12 ABSENT, 0 PRESENT** after it. 39 is the corrected prediction (the
note above); the first-written 40 was not met, and that is how the miscount was found. The boot's reading is taken again
on the deployed jar.
