# GATE — DEFT HANDS: the Ranger reload enchant, the one-tick floor, the RELOAD line (and the stack's one R0)

**Status at `61e722f2` (the stack-top boot, 2026-11-10): R0a-d PASS (the stack's one R0); DH5 PASS (unit); DH1, DH3, DH6 SKIPPED (a staging defect: no hotbar slot for the Boltor); DH2, DH4, DH7, DH8 NOT READ (Ben paused; carried). NOT a merge candidate.**

**The boot, 2026-11-10, on `61e722f2` (#180's head, the stack top), Paper 26.1.2 build 74, PacketEvents 2.13.0.** CC booted it with `./scripts/dev-server.sh --refresh-content` on the seat's word; Ben was the only player (LOGIN L81, 09:32:48). Line numbers are this boot's `run/logs/latest.log`.
- ST7 first (L82-L88), then four selftest runs.
- **The level run** (L88-L302): ST7's start half.
- **Run 1**, `all` (L304-L1551): ABORTED. Ben drowned in the Sunder rows (L1522, 09:38:54) and then disconnected (L1549). It certifies nothing.
- **Run 2**, `all` (L1557-L2903): STOPPED by Ben (`/rpg selftest stop`, L2900) after he drowned (L2831, 09:43:59, in SU11's opening wait). The Sunder rows stage relative to the player, so each drop lowered the next row's floor (y 63, 59, 56, about 51) into water. Its SU10 is VOID; it certifies nothing else.
- **Run 3**, `all` (L2917-L4311): **the run of record** (the seat, 2026-11-10). Ben started it himself, and it finished.
- **Ben's hand rows**, after L4311 with the mob trace on (L4312): SB1 and WB1 only (L4539-L4819). He paused before DH2, DH4 and SU3 (11:02:40). **No clean stop is in the log:** Ben left without `/stop`, and the JVM was left running, not killed (the seat's word).
- **The stop, 2026-11-12: the JVM is GONE, but the log records NO CLEAN STOP.** Ben: *"yes, the server is stopped"*. Read by CC the same day: zero `java.exe` (`Get-CimInstance Win32_Process -Filter "Name='java.exe'"` and `tasklist`, both empty). `run/logs/latest.log` is 5196 lines and its LAST line is L5196, `[11:02:50] [Rpg] MOBREMOVE 114989de-…` (mtime 2026-11-10 11:02:50). **None of the three shutdown lines is in it:** no `Stopping the server`, no `[Rpg] Disabling Rpg`, and the only `All dimensions are saved` is L70, the startup save (09:32:37). Pattern control: the same grep finds all three in `2026-10-15-2.log.gz` (L1112, L1114, L1144). **So the process ended without a logged shutdown** (a closed window or a kill, not `/stop`): `RpgPlugin.onDisable` left no line, the profile and vault flush is not witnessed, and the world on disk is whatever the last autosave wrote. **The next boot must not assume Ben's profile or the world is as the session left them.** This log rotates on the next boot; the run 3 line numbers cited across the stack stay valid in the rotated `.log.gz`.

**Defects found in the reading (seat-accepted, 2026-11-10; fixes owed before the next boot):** (1) `PaperCombatWorld.castRay` accepts a dying `LivingEntity`, so a shot ends on the previous row's corpse (GATE-legacy-b.md LB2 and LB5 (a), GATE-withered-shortbow.md WB2). (2) The selftest's staging leaks between rows: dying targets take the look-ray (GATE-wither-status.md WS2b, WS2c, WS3); `n`-tagged mobs are never removed (GATE-sunder.md SU2); placed blocks persist until the run ends (run 2's flood); the inventory fills across gates (DH1 below).

**Status: NOT RUN.** Written 2026-11-06, before any boot of this branch. **NO BOOT until the seat says so.** This is the
new STACK TOP (`#168 → … → #175 → #178 Sunder → this`), so the stack's one R0 moves here from `GATE-sunder.md`, which
records its R0 SUPERSEDED by this file.

Every prediction below was written **before** any row of this file was read, and **this file is COMMITTED before the
boot**. Readings go **beside** a prediction, never over it, and a prediction is not edited once its row has been read.

**Plan:** `docs/plans/PLAN-deft-hands.md`. **Ben, 2026-11-06:** *"I'd like to pivot to adding a new Enchantment, Deft
hands, increases reload speed by 10/20/30% Ranger weapon exclusive"*, and on the plan's question 4: *"a"*, meaning
reload **TIME** × 0.9 / 0.8 / 0.7. **The seat's rulings, 2026-11-06:**
- the percent is converted once, against the held weapon's AUTHORED `reload_ticks`, in integer arithmetic;
- the scanner gates on `ReloadTime.declares`;
- the gate is `class: ranger` plus `DeftHandsContentInvariantTest` (`hunters_bow` can roll it uselessly: a finding);
- **the resolved reload never goes below 1 tick**;
- the RELOAD trace line exists;
- the display name is "Deft Hands" and the icon is feather.

## GAME MODE

**SURVIVAL on every row.** `/rpg selftest` refuses any other mode (GATE-selftest.md ST7). **A reload consumes arrows
in SURVIVAL** (`QuiverAmmo`), so Ben's hand rows carry arrows.

```
ROWS     12   R0a R0b R0c R0d
              DH1 DH2 DH3 DH4 DH5 DH6 DH7 DH8
         ──
         12   = 4 R0 + 8 DH     git grep -c '^### R0\|^### DH' <ref> -- docs/gates/GATE-deft-hands.md
```

## THE NEXT BOOT: THE STACK UN-PARKED ONTO MASTER (dated additions, 2026-11-14, BEFORE ANY BOOT)

**Nothing above or below is edited; these are additions.** On 2026-11-14 the six branches were rebased, bottom-up,
onto master `af7a1bd5`, which now carries the Mining system (#181, #182, #183, #184, #185, #186). Old tips → new tips:
#172 `2c99b55c` → `f3bb0a4c`, #173 `c4b39e13` → `22907818`, #174 `d06a749d` → `03bc17d8`, #175 `3eec8a22` → `5656a0e0`,
#178 `2bc97dc5` → `30b5cbc6`, #180 `d7aa63b4` → `aa637901` (then this file's commit on top). 61 commits, none
squashed, reordered, dropped or reworded; 60 keep their `git patch-id --stable`. **One commit conflicted:** #175's
`df9e2a14` → `85233eb0`, in `RpgPlugin.java` and `RpgListeners.java`, where master had appended `MiningService mining`
to the `RpgListeners` constructor and to `RpgCommand.build`, and the commit appended `SelfTestService selfTest` and
`rpg.addChild(SelfTestCommand.build(selfTest))`. Both kept: `…, stoneCaster, mining, selfTest)`, and
`RpgCommand.build(…, vaults, mining)` followed by the `addChild`. `./mvnw clean test` at every new tip: core / storage
/ paper `1667/92/1155`, `1672/92/1173`, `1672/92/1176`, `1672/92/1216`, `1688/92/1234`, **`1693/92/1246` at
`aa637901`**; master `1665/92/1142`.

**THE RESOLUTION'S RUNTIME WITNESSES, because two of its lines are visible to no test.** Deleting
`getLogger().info(selfTest.enableLine())` or `rpg.addChild(SelfTestCommand.build(selfTest))` left the whole suite green
(`1693/92/1246` both times), so **R0d is the sole witness of the first, and any `/rpg selftest` reply or
`SELFTEST RUN START` line is the sole witness of the second** (ST7's start half is the first such line). Mining's half
of the resolution is compile-enforced (dropping or swapping either `mining` argument does not build); at boot, the
`MINING pack offer` line at LOGIN (below) shows the listener holds the mining service.

### CARRIED STATE, DONE BY THE BOOTER BEFORE THE BOOT, IN THIS ORDER (added 2026-11-14)

1. **No JVM running:** `Get-Process java -ErrorAction SilentlyContinue` prints nothing.
2. **A fresh dated backup**, with no server running, in ONE bash invocation from the repo root (the folder is named
   by the day it is taken; `mkdir` without `-p` refuses an existing folder):

   ```bash
   B="/c/Users/Neb91/IdeaProjects/rpg-run-backups/$(date +%F)-pre-combat-stack"; mkdir "$B" && cp -a run/plugins/Rpg run/world "$B/" && for p in "run/plugins/Rpg $B/Rpg" "run/world $B/world"; do set -- $p; echo "$1: $(find "$1" -type f | wc -l) files $(find "$1" -type f -printf '%s\n' | awk '{s+=$1} END{print s}') bytes; $2: $(find "$2" -type f | wc -l) files $(find "$2" -type f -printf '%s\n' | awk '{s+=$1} END{print s}') bytes; diff -rq lines: $(diff -rq "$1" "$2" | wc -l)"; done
   ```

   Report the folder, both pairs of counts, and `diff -rq lines: 0` for each.
3. **Check, do not assume, the join toggle in Ben's local file:** `grep -n "dev-pack-on-join" run/plugins/Rpg/config.yml`
   must print `12:  dev-pack-on-join: true` (read 2026-11-14 while writing this section). If it does not, STOP.
4. **Boot:** `./scripts/dev-server.sh --refresh-content`. **The flag is required** (PLAN-mining.md F-M16): the deployed
   `run/plugins/Rpg/content` is master's (14 weapons, 4 statuses, no `deft_hands.yml`, and the deleted
   `active_placeholder_melee_thrust.yml` still present), and `saveResource` never overwrites. The flag `rm -rf`s that
   folder, so nothing master shipped and this stack deleted can linger.
5. **R0 binds to THIS boot** (PLAN-mining.md F-M44): the jar now deployed is the S2 build `03bd1bf0`, so a wait on
   `Done (` can match the old log. Read R0 only on a `latest.log` with a fresh mtime whose `[Rpg] Build:` line names
   the stack top's head.

**Also carried, not actions:**
- **The last session ended CLEAN.** The S2 boot (2026-11-14, build `03bd1bf0`) logged `/stop` (L171), `Disabling Rpg`
  (L174) and `All dimensions are saved` (L204) in `run/logs/latest.log` (mtime 2026-11-14 08:02:58), and no
  `java.exe` was running when this was written. **This supersedes, for the next boot, the "must not assume" in the
  2026-11-12 stop note above**, which describes the `61e722f2` session; four master boots have run since.
- **A resource pack is offered at EVERY LOGIN, and that is expected, not a defect:** Ben's local config has
  `mining.dev-pack-on-join: true` (PLAN-mining.md F-M26). Predicted, in the same second as the LOGIN line:
  `MINING pack offer player=BaronVonYeetus url=http://127.0.0.1:` a port `/rpg-pack.zip
  sha1=49f2e465b2b6bbb24380425830c8572f14acd11c bytes=8227`, then `MINING pack status … status=ACCEPTED`,
  `DOWNLOADED`, `SUCCESSFULLY_LOADED`. **S2's sha1 and bytes**, because nothing the pack is built from moved:
  `git diff --stat 1cefaea9 aa637901 -- paper/src/main/resources/resourcepack paper/src/main/java/io/github/butterflysmp/rpg/paper/mining/MiningPack.java`
  prints nothing. A different sha1 is recorded, not an R0 failure; no row of this stack reads the pack.
- **No mining slot exists at boot** (slots are in memory and lost on restart, PLAN-mining.md 5.4), so mining's block
  handlers find nothing during a selftest run. Nothing in this stack's rows places one.
- **Mining's dev commands and the selftest are behind two different guards, side by side:** `/rpg mining` is behind
  `Permissions.DEV` only (`MiningWiringSignatureTest` pins it); `/rpg selftest` is behind `SelfTestGuard` (`-Drpg.dev=true`
  from `dev-server.sh`, the dev world, one player, SURVIVAL). PLAN-mining.md records that mining's move under the
  `rpg.dev` guard follows #175's merge; the rebase did not move it.

### RUN-SHEET RULES FOR THIS BOOT (added 2026-11-14, learned on the Mining boots after the stack was parked)

- **No placeholder in any command** (PLAN-mining.md F-M25). Every command Ben types is posted by the booter as the
  literal command, filled from a reading already in hand. Where a value is not known until the run, the sheet says
  to ask for it. *(The row text above still carries forms like `<bonus>`; those are descriptions of a command, and
  the booter posts the literal one.)*
- **The FIRST line after LOGIN is `/gamemode survival`, on its own** (PLAN-mining.md F-M43 and the seat's ruling
  F-M46), read in the log as `BaronVonYeetus issued server command: /gamemode survival` before any other command
  line. `/rpg selftest` refuses outside SURVIVAL anyway (ST7). **ST7 then switches to CREATIVE on purpose**
  (`/gamemode creative`, `/rpg selftest level`, `/gamemode survival`, `/rpg selftest level`); that comes AFTER the
  first line, and does not replace it.
- **Anything read from chat is read from the log where it can be.** Inside a selftest run, a command's reply is a
  `REPLY` line. **Outside a run there is no `REPLY` line**: ST7's refusal text and any `/rpg reloadtime` reply Ben
  types by hand are chat only, and stay Ben's halves.

## THE WITNESSES, AND THE HONESTY RULE

- **`RELOAD player=<uuid> weapon=<id> authored=<n> resolved=<n> start=<tick> completesAt=<tick>`**: new in this slice,
  under `/rpg mobtrace`. It is printed by `Quivers.beginReload` from the numbers it just stamped, and formatted by
  `ReloadTime.line`, which `ReloadTimeTest` pins.
- **`/rpg reloadtime <bonus>`'s reply**, `<authored> -> <resolved> ticks (<s> -> <s>)`: the held weapon's reload as
  `ReloadTime.resolve` prices it from the live stat plus `<bonus>`. **It also mints a clock item into the inventory**
  (the dev instrument). The clock counts only when it is EQUIPPED, so the readings below never equip it. During a
  selftest run the reply is a `REPLY` line. **Its full text is `Gave reload_time_boost (<±n> ticks, <SLOWER|FASTER|no change>: <outcome>). Takes effect on your NEXT reload, not on one already running.`; every row below predicts the `<outcome>` as a CONTAINED substring.** It is formatted with the JVM's default locale (`String.format`, no `Locale`), so the seconds read `3.00s` on an English-locale JVM; a comma separator is that, not a FAIL.
- **A reload begins from the left-click PACKET** (`WeaponSwingListener`, a PacketEvents listener), so the instrument
  cannot drive one: its synthetic `PlayerArmSwingEvent` never reaches that path. **DH2 and DH4 are Ben's input, with
  the server's `RELOAD` line as the reading.** No verb was added to bypass the packet path, because a reading through
  a bypass would not be a reading of the shipped path.

| row | witness | the instrument | Ben's half / why not AUTO |
|---|---|---|---|
| DH1 | AUTO-PARTIAL | enchants a held Boltor III through the dev commands, dumps its lore, reads `/rpg reloadtime 0` | the stats sheet's `Reload` line, a glance |
| DH2 | AUTO-PARTIAL | — | **Ben** empties and reloads; the reading is the `RELOAD` line (a packet starts a reload) |
| DH3 | AUTO | the tiers on 60, 40 and 34 through `/rpg reloadtime 0` | — |
| DH4 | BEN | — | held right-click through a reload; the instrument cannot hold a key |
| DH5 | UNIT | `EnchantRollTest.deftHandsEntersTheRangerPoolAndNoOther` | candidates are rolled at random per item, so one weapon's table is not a reading of the pool |
| DH6 | AUTO | Deft III on `quiver_stone` plus `/rpg reloadtime -30`: the floor | — |
| DH7 | BEN | — | the reload cue is a sound, the client's |
| DH8 | BEN | — | the grindstone refund through the real screen |

---

## R0 — THE ONE R0 FOR THE WHOLE STACK (moved here from GATE-sunder.md). READ IT FIRST. IF ANY R0 FAILS, STOP

Every gate beneath carries its R0 SUPERSEDED, never PASS or FAIL. `GATE-sunder.md`'s note names this file.

### R0a — the build line names this PR's head

| prediction | instrument | READING |
|---|---|---|
| `[Rpg] Build: <sha>`, this PR's head as `gh pr view <n> --json headRefOid` prints it, shortened. Not `-dirty`, not `unknown`, and not Sunder's (#178) or any head beneath | `Select-String -Path run\logs\latest.log -Pattern '\[Rpg\] Build:' \| Select-Object -First 1` | **61e722f2, 2026-11-10: PASS.** L43 `[Rpg] Build: 61e722f2`, #180's head (`gh pr view 180 --json headRefOid` printed `61e722f2d5e39fd04589dbc81ec3b2f99556afb5`); not `-dirty`, not `unknown`. |

### R0b — the jar carries every slice, Sunder, and Deft Hands

| prediction | instrument | READING |
|---|---|---|
| PRESENT: `GATE-sunder.md` R0b's thirteen slice entries (unchanged), and **this slice's** `content/enchants/deft_hands.yml` and paper `health/DeftHandsModifierItems.class`. ABSENT: `content/abilities/active_placeholder_melee_thrust.yml` (Sunder deleted it) and the control. In `deft_hands.yml`, as anchored YAML: PRESENT `effect: reload_time`, `class: ranger`, `icon: feather`; ABSENT `class: universal` | the scan below | **61e722f2, 2026-11-10: PASS.** The scan above, run against `run\plugins\rpg-0.1.0-SNAPSHOT.jar`: all 15 slice entries PRESENT; `content/abilities/active_placeholder_melee_thrust.yml` and the control ABSENT. In `deft_hands.yml`: PRESENT `^effect: reload_time`, `^class: ranger`, `^icon: feather`; ABSENT `^class: universal`. |

```powershell
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::OpenRead((Resolve-Path 'run\plugins\rpg-0.1.0-SNAPSHOT.jar'))
foreach ($c in 'io/github/butterflysmp/rpg/core/combat/TracedHit.class',
               'io/github/butterflysmp/rpg/core/progression/LevelBonus.class',
               'io/github/butterflysmp/rpg/paper/menu/NexusScreens.class',
               'content/weapons/short_bow.yml',
               'io/github/butterflysmp/rpg/paper/adapter/FireballDrive.class',
               'io/github/butterflysmp/rpg/paper/adapter/DotStatus.class',
               'content/statuses/withering.yml',
               'content/weapons/withered_shortbow.yml',
               'io/github/butterflysmp/rpg/paper/selftest/SelfTestService.class',
               'selftest/index.yml',
               'io/github/butterflysmp/rpg/core/build/Touchdown.class',
               'content/abilities/sunder.yml',
               'content/builds/melee.yml',
               'content/enchants/deft_hands.yml',
               'io/github/butterflysmp/rpg/paper/health/DeftHandsModifierItems.class',
               'content/abilities/active_placeholder_melee_thrust.yml',
               'io/github/butterflysmp/rpg/paper/menu/NoSuchClassControl.class') {
  if ($zip.GetEntry($c)) { "PRESENT $c" } else { "ABSENT  $c" }
}
$r = New-Object IO.StreamReader($zip.GetEntry('content/enchants/deft_hands.yml').Open()); $d = $r.ReadToEnd(); $r.Close()
foreach ($k in '^effect: reload_time\s*$', '^class: ranger\s*$', '^icon: feather\s*$', '^class: universal\s*$') {
  if ($d -match ('(?m)' + $k)) { "PRESENT $k" } else { "ABSENT  $k" }
}
$zip.Dispose()
```

**R0b, ADDED 2026-11-14, before the next boot (the prediction above stands and is read as written).** On master
`af7a1bd5` the jar must ALSO carry Mining, which no slice of this stack wrote, so a jar built from the parked tips
(or any head beneath master) fails here. **PRESENT:** core `mining/SizeRoll.class`, `mining/MissSoundCooldown.class`,
`hud/ActionBarHolds.class`; paper `mining/MiningService.class`, `mining/MiningPack.class`,
`content/MiningContentLoader.class`; `content/ores/iron.yml`, `content/vein_sizes/extra_large.yml`; the resource pack's
`resourcepack/pack.mcmeta`, `resourcepack/assets/rpg/items/mining/iron_patch_small.json`,
`resourcepack/assets/rpg/textures/item/mining/iron_patch_extra_large.png`. **ABSENT:**
`resourcepack/assets/rpg/items/mining/iron_patch.json` (slice 1's name, renamed in S2: present means a pre-S2 master)
and the control `mining/NoSuchClassControl.class`. **11 PRESENT, 2 ABSENT.** The same scan, pointed at
`paper\target\rpg-0.1.0-SNAPSHOT.jar` built from `aa637901` on 2026-11-14 (not deployed, not booted), printed exactly
that; it is a dry run of the instrument, not a reading of R0b.

```powershell
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::OpenRead((Resolve-Path 'run\plugins\rpg-0.1.0-SNAPSHOT.jar'))
foreach ($c in 'io/github/butterflysmp/rpg/core/mining/SizeRoll.class',
               'io/github/butterflysmp/rpg/core/mining/MissSoundCooldown.class',
               'io/github/butterflysmp/rpg/core/hud/ActionBarHolds.class',
               'io/github/butterflysmp/rpg/paper/mining/MiningService.class',
               'io/github/butterflysmp/rpg/paper/mining/MiningPack.class',
               'io/github/butterflysmp/rpg/paper/content/MiningContentLoader.class',
               'content/ores/iron.yml',
               'content/vein_sizes/extra_large.yml',
               'resourcepack/pack.mcmeta',
               'resourcepack/assets/rpg/items/mining/iron_patch_small.json',
               'resourcepack/assets/rpg/textures/item/mining/iron_patch_extra_large.png',
               'resourcepack/assets/rpg/items/mining/iron_patch.json',
               'io/github/butterflysmp/rpg/paper/mining/NoSuchClassControl.class') {
  if ($zip.GetEntry($c)) { "PRESENT $c" } else { "ABSENT  $c" }
}
$zip.Dispose()
```

### R0c — the cumulative Loaded line, and nothing refused

| prediction | instrument | READING |
|---|---|---|
| `Loaded 11 abilities, 29 visuals, 5 statuses, 7 elements, **11 enchants**, 3 pools, 6 fragments, 6 aspects, 16 weapons, 1 shields, 24 armor, 5 tools, 6 accessories, 1 mobs, 3 recipes`. That is GATE-sunder.md's R0c with enchants 10 → 11 (`deft_hands.yml`; `ls content/enchants/*.yml` = 11 at this head). No `Refusing`, `Skipping`, `SEVERE` or exception beyond `volley_stone`'s known pair, and **none naming `deft_hands` or `reload_time`** | `Select-String run\logs\latest.log -Pattern 'Loaded ','Refusing','Skipping','SEVERE','Exception'` | **61e722f2, 2026-11-10: PASS.** L44 `[Rpg] Loaded 11 abilities, 29 visuals, 5 statuses, 7 elements, 11 enchants, 3 pools, 6 fragments, 6 aspects, 16 weapons, 1 shields, 24 armor, 5 tools, 6 accessories, 1 mobs, 3 recipes`, exactly as predicted. No `Refusing`, `Skipping`, `SEVERE` or `Exception` line at all (not even `volley_stone`'s known pair). |

**R0c RE-PREDICTED FOR THE NEXT BOOT, 2026-11-14, before any boot (the prediction and reading above stay as written;
PLAN-mining.md F-M20).** Master `af7a1bd5` appends `, N ores, M vein sizes` after `N recipes` (`RpgPlugin`'s `Loaded`
line). On the rebased stack the line is predicted as:

**`[Rpg] Loaded 11 abilities, 29 visuals, 5 statuses, 7 elements, 11 enchants, 3 pools, 6 fragments, 6 aspects, 16 weapons, 1 shields, 24 armor, 5 tools, 6 accessories, 1 mobs, 3 recipes, 1 ores, 4 vein sizes`**

No `Refusing`, `Skipping`, `SEVERE` or `Exception` beyond `volley_stone`'s known pair, none naming `deft_hands` or
`reload_time`; and Mining's absences, as S2's R0c predicted them: no `No ores`, no `No vein`, no `Skipping malformed
vein size`, no `Content:` line naming `ore 'iron'`. Instrument: `Select-String run\logs\latest.log -Pattern 'Loaded ','Refusing','Skipping','SEVERE','Exception','No ores','No vein','Content:'`.

**Every count is DERIVED from the content at `aa637901`, not copied** (the content is unchanged by this file's
commit). Each registry is the loader's size: one entry per `.yml` file, except **pools** (only the `builds/` files with
a top-level `element:` are cells; `ranger.yml` and `melee.yml` are class files, `PoolLoader`), **armor** (each tier
file expands to its four `pieces:`, `ArmorLoader`) and **tools** (one file, keyed under `tools:`, `ToolLoader`). Run
from the repo root:

| count | command | printed |
|---|---|---|
| abilities | `git ls-tree --name-only aa637901 paper/src/main/resources/content/abilities/ \| grep -c '\.yml$'` | 11 |
| visuals | `git ls-tree --name-only aa637901 paper/src/main/resources/content/visuals/ \| grep -c '\.yml$'` | 29 |
| statuses | `git ls-tree --name-only aa637901 paper/src/main/resources/content/statuses/ \| grep -c '\.yml$'` | 5 |
| elements | `git ls-tree --name-only aa637901 paper/src/main/resources/content/elements/ \| grep -c '\.yml$'` | 7 |
| enchants | `git ls-tree --name-only aa637901 paper/src/main/resources/content/enchants/ \| grep -c '\.yml$'` | 11 |
| pools | `git grep -l -E '^element:' aa637901 -- 'paper/src/main/resources/content/builds/*.yml' \| wc -l` | 3 |
| fragments | `git ls-tree --name-only aa637901 paper/src/main/resources/content/fragments/ \| grep -c '\.yml$'` | 6 |
| aspects | `git ls-tree --name-only aa637901 paper/src/main/resources/content/aspects/ \| grep -c '\.yml$'` | 6 |
| weapons | `git ls-tree --name-only aa637901 paper/src/main/resources/content/weapons/ \| grep -c '\.yml$'` | 16 |
| shields | `git ls-tree --name-only aa637901 paper/src/main/resources/content/shields/ \| grep -c '\.yml$'` | 1 |
| armor | `git grep -h -E '^  (head\|chest\|legs\|feet):' aa637901 -- 'paper/src/main/resources/content/armor/*.yml' \| wc -l` | 24 |
| tools | `git show aa637901:paper/src/main/resources/content/tools/iron.yml \| grep -cE '^  [a-z_]+:\s*$'` | 5 |
| accessories | `git ls-tree --name-only aa637901 paper/src/main/resources/content/accessories/ \| grep -c '\.yml$'` | 6 |
| mobs | `git ls-tree --name-only aa637901 paper/src/main/resources/content/mobs/ \| grep -c '\.yml$'` | 1 |
| recipes | `git ls-tree --name-only aa637901 paper/src/main/resources/content/recipes/ \| grep -c '\.yml$'` | 3 |
| ores | `git ls-tree --name-only aa637901 paper/src/main/resources/content/ores/ \| grep -c '\.yml$'` | 1 |
| vein sizes | `git ls-tree --name-only aa637901 paper/src/main/resources/content/vein_sizes/ \| grep -c '\.yml$'` | 4 |

**The control, run the same day:** the same commands at `af7a1bd5` print `11, 27, 4, 7, 10, 3, 6, 6, 14, 1, 24, 5, 6,
1, 3, 1, 4`, which is exactly the `Loaded` line `GATE-mining-s2.md`'s R0c READ on the S2 boot. So the derivation
reproduces a line a server printed, on the tree that printed it. **The counts that move from master:** visuals 27 → 29
(the Blaze King's Staff's two), statuses 4 → 5 (`withering.yml`), enchants 10 → 11 (`deft_hands.yml`), weapons 14 → 16
(`blaze_kings_staff.yml`, `withered_shortbow.yml`); abilities stay 11 (Sunder added, `active_placeholder_melee_thrust.yml`
deleted). The result equals the 61e722f2 prediction above with Mining's tail appended. *(In this table `\|` is a
Markdown escape; type `|`.)*

### R0d — the dev marker reached the JVM, and every scenario loaded

| prediction | instrument | READING |
|---|---|---|
| Exactly one line **`[Rpg] Selftest: dev server, 37 scenarios across 9 gates [melee-cell, level, nexus-polish, legacy-a, legacy-b, wither-status, withered-shortbow, sunder, deft-hands]`** (34 + this gate's 3: DH1, DH3, DH6), and **no** `Refusing selftest` line | `Select-String run\logs\latest.log -Pattern 'Selftest:','Refusing selftest'` | **61e722f2, 2026-11-10: PASS.** L50 `[Rpg] Selftest: dev server, 37 scenarios across 9 gates [melee-cell, level, nexus-polish, legacy-a, legacy-b, wither-status, withered-shortbow, sunder, deft-hands]`, exactly; no `Refusing selftest` line. |

**R0d RE-PREDICTED FOR THE NEXT BOOT, 2026-11-10 (the reading above stays as read):** SU8 gained a scenario
(`GATE-sunder.md` SU8, its death half), so the line becomes **`[Rpg] Selftest: dev server, 38 scenarios across 9
gates [melee-cell, level, nexus-polish, legacy-a, legacy-b, wither-status, withered-shortbow, sunder, deft-hands]`**,
with still no `Refusing selftest` line. `SelfTestScenariosTest.theScenariosAreExactlyTheCoreRows` pins the 38.

---

## THE ROWS

**The log instrument for every AUTO row:** `Select-String run\logs\latest.log -Pattern 'SELFTEST deft-hands','RELOAD '`,
read after Ben's LOGIN.

### DH1 — Deft Hands III on a Boltor: the lore line, and the reload it buys (SURVIVAL) — witness: the log + a glance

| prediction | READING |
|---|---|
| `/rpg give boltor`, held; `/rpg enchant clear`, `candidate 0 deft_hands`, `level 0 0 3`, `active 0 0`; 40 t for the health scan. The held `DUMP`'s lore carries a line naming **`Deft Hands`** and **`(-30% Reload Time)`**. `/rpg reloadtime 0`'s `REPLY`: **`60 -> 42 ticks (3.00s -> 2.10s)`**. Ben's half: the stats sheet's `Reload` line reads the same 2.1 s | **61e722f2, 2026-11-10 (run 3): SKIPPED, a staging defect.** L4304 `CMD /rpg give boltor`, then L4305 `SKIPPED no hotbar slot holds boltor` in the same tick. Every earlier gate's gives stay in the inventory until the run's final restore, so the likeliest cause is a full hotbar; that is not proven, since no inventory probe precedes the hold. Carried; a per-gate inventory snapshot is owed in the selftest (seat, 2026-11-10). |

### DH2 — a real reload on the Deft III Boltor takes 42 ticks (SURVIVAL) — witness: the RELOAD line

| prediction | READING |
|---|---|
| **Ben's input, with `/rpg mobtrace` ON:** DH1's Boltor and a stack of arrows. Fire all 8, then left-click to reload. **One `RELOAD player=<Ben> weapon=boltor authored=60 resolved=42 start=<t> completesAt=<t+42>`**, with `completesAt − start = 42` exactly. Then once more with the enchant cleared (`/rpg enchant clear`, 40 t): `authored=60 resolved=60`, a difference of 60 | **61e722f2, 2026-11-10: NOT READ.** Ben paused before it (seat, 2026-11-10); carried. |

### DH3 — every tier on every shipped reload length (SURVIVAL) — witness: the log

| prediction | READING |
|---|---|
| Via `/rpg reloadtime 0`'s `REPLY`, 40 t after each enchant change. **Boltor: I `60 -> 54 ticks (3.00s -> 2.70s)`, II `60 -> 48 ticks (3.00s -> 2.40s)`. `dragons_breath` I: `40 -> 36 ticks (2.00s -> 1.80s)`. `quiver_stone` I: `34 -> 30 ticks (1.70s -> 1.50s)`**, where 3.4 floors to a 4-tick cut. The full 18-point grid is `ReloadTimeTest.deftHandsCutsEveryTierOnEveryShippedReloadAndNoneRoundsToZero`; this row reads four of its cells through the shipped stat | **61e722f2, 2026-11-10 (run 3): SKIPPED** (L4307: chained to DH1). Carried. |

### DH4 — the first shot after a shortened reload (SURVIVAL) — witness: Ben

| prediction | READING |
|---|---|
| **Hand row.** The Deft III Boltor, empty, with arrows: reload, then hold right-click. The first shot comes no earlier than the `RELOAD` line's `completesAt`, and within one held-input spacing of it (the 4-tick grid; `GATE-locust.md` row 1 measured inputs no closer than 3 t). **The felt gap is RECORDED, not predicted**: maturity is read on the next input (`QuiverState`), so part of a short saving can be swallowed | **61e722f2, 2026-11-10: NOT READ.** Ben paused before it (seat, 2026-11-10); carried. |

### DH5 — eligibility is by class: every ranger pool, no other (UNIT) — witness: the unit row

| prediction | READING |
|---|---|
| `EnchantRollTest.deftHandsEntersTheRangerPoolAndNoOther` green in the suite at this head (ranger in; mage, melee, shield, armor out), and `DeftHandsContentInvariantTest` green (every ranger weapon but `hunters_bow` authors `reload_ticks`). **Not a boot read:** a weapon's table shows candidates rolled at random, so its absence or presence on one item says nothing about the pool. **`hunters_bow` CAN roll Deft Hands and gain nothing:** a recorded finding, by ruling | **61e722f2, 2026-11-10: PASS (unit).** `./mvnw clean test` at `61e722f2`, run before the boot: core 1567, storage 92, paper 1172, 0 failures, which includes `EnchantRollTest.deftHandsEntersTheRangerPoolAndNoOther` and `DeftHandsContentInvariantTest`. |

### DH6 — stacking reaches the one-tick floor and stops (SURVIVAL) — witness: the log

| prediction | READING |
|---|---|
| `quiver_stone` with Deft III (34 → 23), then `/rpg reloadtime -30`'s `REPLY`: **`34 -> 1 ticks (1.70s -> 0.05s)`**. 34 − 11 − 30 would be −7, an instant reload; the floor (`ReloadTime.MIN_TICKS`, the seat's ruling) holds it at one tick. The clock item the command mints is NOT equipped, so this is the price the command quotes, not a stat change | **61e722f2, 2026-11-10 (run 3): SKIPPED** (L4309: chained to DH3). Carried. |

### DH7 — the reload cue sounds at the shortened completion (SURVIVAL) — witness: Ben

| prediction | READING |
|---|---|
| **Hand row**, with DH2's reload. The reload-complete sound plays about 2.1 s after the left-click (42 t), not 3 s. `Quivers.beginReload` hands the cue the resolved `reloadTicks`; this row is the client's ear | **61e722f2, 2026-11-10: NOT READ.** Carried. |

### DH8 — the grindstone strips it like any enchant (SURVIVAL) — witness: Ben

| prediction | READING |
|---|---|
| **Hand row.** The Deft III Boltor into the grindstone, stripped. The refund matches any level-III enchant's (`GrindstoneRefund`, 35% of the list spend; Deft Hands has no cost of its own), and `/rpg reloadtime 0` then reads `60 -> 60 ticks` | **61e722f2, 2026-11-10: NOT READ.** Carried. |

---

## UNIT ROWS AND MUTATIONS (run 2026-11-06 against this branch's working tree, before this file was committed)

Rows:
- `ReloadTimeTest` (11; four new, one re-ruled);
- `DeftHandsModifierItemsTest` (5);
- `DeftHandsContentInvariantTest` (3);
- `EnchantLoaderTest` (+2);
- `EnchantEffectLineTest` (+2);
- `EnchantRollTest` (+1).

Each mutation landed with marker 1 and one changed line, its test reddened, and the file was restored byte-identical:

| mutation | killed by |
|---|---|
| `DeftHandsModifierItems.desired`: `declares` replaced by `cut > ReloadTime.NONE` (the seat's required mutant) | `DeftHandsModifierItemsTest` (3 rows, incl. `aBoltorWithDeftHandsIIIDeclaresANegativeEighteenTickCut`) |
| `ReloadTime.resolve`: the `MIN_TICKS` floor removed | `ReloadTimeTest.theResolvedDurationGoesDownToTheOneTickFloorAndNoFurther`, `…StopsAtTheOneTickFloor` |
| `ReloadTime.percentCut`: truncation (`-(R*p/100)`) instead of `floorDiv` | `ReloadTimeTest` grid, `aFractionalCutFloorsToTheLargerCut`, the stacking row |
| `EnchantDefinition`: `RELOAD_TIME` gated `MAIN_HAND_ONLY` (admits universal) | `EnchantLoaderTest.aReloadTimeEnchantOnAnyClassButRangerIsRefused` |
| `EnchantEffectLine`: ruling (b)'s wording `+N% Reload Speed` | `EnchantEffectLineTest.deftHandsSaysTheReloadTimeCutAtEveryLevel` |
| `DeftHandsContentInvariantTest`: its reload pattern made blind | its own positive control, and the invariant row |

---

## EVERY ROW OF THE STACK, AT THE NEXT BOOT (added 2026-11-14, before any boot)

**One table for all six gates, read from the gate files at `aa637901`.** "State" is the latest reading (the `61e722f2`
boot, 2026-11-10, run 3) plus any later dated amendment. **AUTO** = driven by `/rpg selftest`: the row is a top-level
key in `paper/src/main/resources/selftest/GATE-<gate>.yml` (38 keys across 9 files at `aa637901`; R0d predicts 38).
The ST rows are read from the selftest run's own log. **HAND** = Ben's input or eyes. **AUTO-PARTIAL** = an AUTO
server half plus a HAND half, named. "Handoff" is the previous seat's merge-bar list as relayed in the 2026-11-14 brief;
a blank there means the handoff does not name the row. 82 rows: 15 + 14 + 10 + 15 + 16 + 12.

| PR, gate | row | witness | state | handoff |
|---|---|---|---|---|
| #172 legacy-b | R0a, R0b, R0c | — | SUPERSEDED at `61e722f2`; SUPERSEDED again at the next boot (this file's R0) | |
| #172 | LB1 | HAND | NOT READ | |
| #172 | LB2 | AUTO | FAIL as staged at `61e722f2` (a dying mob took the shot); `LiveTargets` fix `27dda674`; prediction amended to allow a crit (`f3bb0a4c`). Unread since | bar |
| #172 | LB3, LB4, LB4b | HAND | NOT READ | |
| #172 | LB5 | AUTO-PARTIAL; HAND: each fireball vanishes at the wall | VACUOUS as written at `61e722f2`; band re-derived (`f3bb0a4c`). Unread since | bar (re-derived band) |
| #172 | LB6, LB7, LB8, LB9, LB10, LB11 | HAND (LB8: Ben + the log) | NOT READ | |
| #173 wither-status | R0a, R0b, R0c | — | SUPERSEDED, as #172's | |
| #173 | WS-C | AUTO-PARTIAL; HAND: no blast particles seen | AUTO half as predicted at `61e722f2`; HAND half NOT READ | bar; HAND half carried |
| #173 | WS1 | AUTO | **PASS** at `61e722f2` | bar (see finding 1) |
| #173 | WS2b | AUTO | VOID (a dying mob took the look-ray) | bar |
| #173 | WS2c | AUTO-PARTIAL; HAND: no wither swirl seen | VOID | bar |
| #173 | WS3 | AUTO-PARTIAL; HAND: no explosion seen | VOID | bar |
| #173 | WS3-P, WS5, WS6 | HAND (WS3-P and WS5 read DOTTICK) | NOT READ | |
| #173 | WS4 | none here: MOVED to WB3 | — | |
| #173 | WS8, WS9 | none | recorded UNWITNESSED as written | |
| #174 withered-shortbow | R0a, R0b, R0c | — | SUPERSEDED, as #172's | |
| #174 | WB1 | AUTO-PARTIAL; HAND: the tooltip as drawn | **PASS** at `61e722f2` (AUTO as re-predicted, and Ben) | already PASS |
| #174 | WB2 | AUTO | FAIL as staged (LB2's defect); prediction amended to allow a crit (`03bc17d8`). Unread since | bar |
| #174 | WB3 | AUTO-PARTIAL (chained after WB2); HAND: the wither swirl | VOID (no WB2 hit to follow) | bar |
| #174 | WB4b, WB5, WB6, WB7 | HAND (WB4b reads DOTTICK) | NOT READ | |
| #175 selftest | R0a, R0b, R0c, R0d | — | SUPERSEDED at `61e722f2` (PASS at `38cc9903`, as that boot's top) | |
| #175 | ST1 | AUTO (the run's log); its restore half needs Ben's Lifetime XP as a baseline | log half as re-predicted at `61e722f2`; restore half NOT READ (no baseline sent). Re-predicted `rows=38` | bar; XP restore carried |
| #175 | ST2 | AUTO (the log) + HAND | PASS at `38cc9903`; NOT READ at `61e722f2` | (see finding 3) |
| #175 | ST3 | AUTO (the log); its sight half HAND | log half PASS at `38cc9903`; NOT READ at `61e722f2` | (see finding 3) |
| #175 | ST4, ST6 | AUTO (the log) | PASS at `38cc9903`; NOT READ at `61e722f2` | (see finding 3) |
| #175 | ST5 | AUTO (the log): the control for every AUTO row | FAIL in part at `61e722f2` (the level half holds; the WS2b half VOID) | bar |
| #175 | ST7 | HAND + the log (Ben changes mode; the refusal is chat, never logged) | log half as predicted at `61e722f2`; refusal text NOT READ | bar; refusal text carried |
| #175 | ST8, ST9, ST10, ST11 | AUTO (the log) | written 2026-11-10, after the boot; NOT RUN | bar |
| #178 sunder | R0a, R0b, R0c, R0d | — | SUPERSEDED at `61e722f2` | |
| #178 | SU1 | AUTO-PARTIAL; the not-offered half is a unit row | as predicted at `61e722f2` | bar (see finding 2) |
| #178 | SU2 | AUTO; HAND: the leap's look | as predicted, plus one unpredicted hit on a leftover mob (ST8 reads the fix) | bar |
| #178 | SU3 | HAND (a held movement key) | NOT READ | bar; carried |
| #178 | SU4 | AUTO | as predicted; the slam through a wall confirmed and **ruled by Ben, 2026-11-12** (the slam ignores walls, revisitable) | recorded |
| #178 | SU5, SU6 | AUTO | as predicted | |
| #178 | SU7 | AUTO-PARTIAL (its ending is a range) | as predicted | |
| #178 | SU8 | AUTO death half (added 2026-11-10, accepted by the seat 2026-11-12); HAND quit half | NOT STAGEABLE at `61e722f2`; unread since | (see finding 3) |
| #178 | SU9, SU12 | AUTO-PARTIAL | NOT AS PREDICTED; diagnosed as staging artefacts, accepted 2026-11-12; unread since | (see finding 3) |
| #178 | SU10 | AUTO | as predicted, with a staging caveat (run 2's SU10 is VOID) | bar |
| #178 | SU11 | AUTO; HAND: the popup and nameplate (secondary) | PARTIAL; diagnosed, accepted 2026-11-12; unread since | (see finding 3) |
| #180 deft-hands | R0a, R0b, R0c, R0d | the stack's ONE R0 | PASS at `61e722f2`, on that head. **Re-read on the new head**, with R0b's and R0c's 2026-11-14 additions | |
| #180 | DH1 | AUTO-PARTIAL; HAND: the stats sheet's `Reload` line | SKIPPED (no hotbar slot; ST11 reads the fix) | bar |
| #180 | DH2 | HAND input; the `RELOAD` line is the reading | NOT READ | bar; carried |
| #180 | DH3, DH6 | AUTO (chained: DH3 after DH1, DH6 after DH3) | SKIPPED (chained to DH1) | bar |
| #180 | DH4, DH7, DH8 | HAND | NOT READ | carried |
| #180 | DH5 | UNIT | PASS (unit) at `61e722f2`; green again at `aa637901` (`EnchantRollTest` 25, `DeftHandsContentInvariantTest` 3, in `./mvnw clean test`) | |

**FINDINGS ON THE HANDOFF'S LIST, checked against the gate files (2026-11-14). Nothing above is changed by them; they
are the seat's to rule.**

1. **#173's WS1 is already PASS** (`GATE-wither-status.md`'s `61e722f2` status line). It is in the bar list because
   that gate's `38cc9903` status names "the VOID rows" as its bar, and WS1 was VOID then.
2. **#178's SU1 already read "as predicted"** at `61e722f2`. SU2 and SU10 are also "as predicted", each with a caveat
   (an extra hit; run 2's VOID), which is a reason to re-read them; SU1 has no caveat recorded.
3. **Rows the handoff does not name, which are not PASS:** SU8, SU9, SU11, SU12 (`GATE-sunder.md`: the seat accepted
   their amendment and diagnoses on 2026-11-12, and "the four rows stay unread until the next boot"); ST2, ST3, ST4,
   ST6 (NOT READ at `61e722f2`; their only PASS is on `38cc9903`, an earlier build).
4. **Only two gates name a bar in their own text:** `GATE-legacy-b.md` ("LB2 is its bar") and `GATE-wither-status.md`
   ("the VOID rows are its bar"), both in their `38cc9903` status lines. Every other bar in the list is the handoff's,
   not a gate's.
5. **HAND halves the carried list does not name:** WS2c's (no wither swirl), WS3's (no explosion), WB3's (the wither
   swirl), LB5's (each fireball vanishes at the wall), ST2's, ST3's sight half, DH1's glance, SU2's look, SU8's quit
   half, SU11's popup.
6. Every name in the carried list matches a HAND half or a HAND row in the gates: DH2, DH4, SU3, DH7, DH8, ST7's
   refusal text, ST1's restore half (the `RUN RESTORED xp=` line needs Ben's baseline), WS-C's no-blast half.
