# GATE — THE SWORD LADDER, PR 3: CONVERT ON ARRIVAL (feat/sword-convert), AND THE ONE BOOT FOR ALL THREE

**Seat approval 2026-11-20: code and gate diffed at 3538f2f7, booted at 90ea1517, readings at b24bf8f3. Merge on Ben's words.** **Status: READ, 2026-11-20, at `90ea1517`; SCJ1 UNREAD, carried** (was: NOT RUN).

> **UNREAD, CARRIED (2026-11-20, the seat's merge approval):** SCJ1, the join sweep, on a server. He held no plain
> claimed sword at either join, so the arm stands on `ArrivalSourceTest` only.

Written 2026-11-20 on `feat/sword-convert`, stacked on `feat/sword-smithing` (#222), which is
stacked on `feat/sword-tiers` (#221) on master `70cbab66`. Written before any boot. **This file carries the ONE R0 and
the ONE run sheet for all three PRs** (the seat's split ruling, 2026-11-20). The rows it boots:
- STA1-STA5 and STM1 — `GATE-sword-tiers.md`;
- SSA1, SSM1 and SSA2 — `GATE-sword-smithing.md`;
- SCA1-SCA5, SCB1-SCB3 and SCJ1 — this file.

**NO BOOT until the seat has diffed all three branches and all three gates from origin and Ben says "ready"**; then only
CC boots, by `.claude/rules/booting.md`.

**Game mode:** SURVIVAL, every row. **Difficulty:** normal (each selftest setup sets it). **Nothing on this boot can
hurt him** (the arithmetic, below).

**Ben's words:**
- PLAN-weapon-tiers.md 1.2, answer 5, asked whether swords found or dropped, not crafted, become minted swords: "Yes".
- The seat's ruling, 2026-11-20: "any route" is built as CONVERT ON ARRIVAL.

## WHAT WAS READ, EACH MARKED: EVERY WAY AN ITEM ENTERS A PLAYER'S INVENTORY IN BUILD 74

**The one event that covers the live routes** (READ, javap on the pinned server jar `run/versions/26.1.2/paper-26.1.2.jar`,
`Implementation-Version: 26.1.2-74-e4e17fc`):
- `io.papermc.paper.event.player.PlayerInventorySlotChangeEvent`: player, slot, old stack, new stack (READ, the pinned
  paper-api).
- It is fired from `ServerPlayer$2.slotChanged`, a `ContainerListener`. It fires for **any changed slot whose
  `Slot.container` IS the player's own `Inventory`**, result slots excepted (`instanceof ResultSlot` skips them).
- That listener is attached by `ServerPlayer.initMenu`, called from `initInventoryMenu`, `openMenu`,
  `openHorseInventory` and one further call site.
- **So it fires for the player's own slots under ANY open menu**: his own inventory, a chest, a villager's trade
  screen, our custom menus.

| route | covered by | marked |
|---|---|---|
| ground pickup (`EntityPickupItemEvent` / `PlayerAttemptPickupItemEvent` fire first) | the slot-change event, when the item lands in a slot | READ (the event), INFERRED (the pickup lands in an `Inventory` slot) |
| a mob's drop, picked up | the same as a pickup | INFERRED |
| chest, barrel, shulker or any container take | the slot-change event: the player's slots in that menu are his `Inventory` | READ |
| a generated loot chest's sword, taken | the same; `LootGenerateEvent` exists (READ) but is not needed | READ |
| a villager trade result, taken | the slot-change event when it lands in his inventory (the result slot itself is skipped) | READ (the skip), INFERRED (the landing) |
| `/give` | the slot-change event: the item is added to his `Inventory` | INFERRED, **read by SCA1** |
| creative inventory | the slot-change event after `InventoryCreativeEvent` sets the slot | INFERRED |
| a crafter or a hopper | **cannot reach a player's inventory** (no hopper or crafter feeds a player) | READ (no `Container` in that path is the player's) |
| our own `/rpg give`, the crafting mint, the smithing upgrade | already minted, so `LEAVE_TAGGED` | READ (`ArrivalClaims`) |
| a plain sword ALREADY in an inventory (an old save, or a route no event saw) | **the join sweep**: `convertArrivalsOnJoin`, after `GearRefresher.refresh` | READ (our source) |

**The stated backstop is the join sweep.** Nothing found reaches a player's `Inventory` without a slot change, so it
covers what was already there and anything that arrived while no listener was registered.

## AS BUILT

- **Core `ArrivalClaims`**, pure. `decide(tagged, material)` has three arms:
  - MINT an untagged item a weapon claims;
  - LEAVE_TAGGED anything already carrying a gear id;
  - LEAVE_UNCLAIMED the rest.

  A weapon claims its material by its craft index entry (contests applied) **or by being the RESULT of a smithing
  upgrade**: that is how a netherite sword found in loot converts, though nothing crafts it. A material two weapons
  claim is refused by name (`Refusing arrival claim '<m>'`) and claims nothing. **Only weapons claim on arrival.**
- **Paper `ArrivalConverter.convert`:** the same sequence as the crafting mint (mint, the enchant roll, the gear-score
  stamp banded on his average), with **the plain item's wear carried** (clamped). **Its vanilla enchantments are NOT
  carried** (flagged for Ben).
- **`onInventorySlotChange`** converts a claimed arrival on the player's next tick, after re-checking the slot.
  **`convertArrivalsOnJoin`** is the backstop. Each conversion logs **`ARRIVAL minted player=<n> slot=<s> material=<m>
  weapon=<id> wear=<d> vanilla_enchants=<k> score=<s> via=<slot-change|join>`**.
- **The boot line:** **`Convert-on-arrival: 7 materials {copper_sword=copper_sword, diamond_sword=diamond_sword,
  golden_sword=golden_sword, iron_sword=iron_sword, netherite_sword=netherite_sword, stone_sword=stone_sword,
  wooden_sword=wooden_sword}`**.

## CARRIED STATE (CC, IN THIS ORDER) — A CHECKLIST, EACH LINE TICKED IN THE REPORT, NOT REMEMBERED

The booter's checklist is `.claude/rules/booting.md`; this list is its instance for this boot.

1. **No JVM:** `Get-Process java -ErrorAction SilentlyContinue` prints nothing.
2. **A fresh dated backup:** `run/plugins/Rpg` and `run/world` copied, as they are, to a NEW folder
   `../rpg-run-backups/<yyyy-mm-dd>-pre-sword-ladder`. Report:
   - the file and directory counts;
   - `diff -rq lines: 0` for each;
   - the `ls` of `world/dimensions`;
   - the overworld's file count.

   Nothing is restored (Ben's word only).
3. `grep -n "dev-pack-on-join" run/plugins/Rpg/config.yml` prints `12:  dev-pack-on-join: true`.
4. **No fight folders:** `find run/world/dimensions/rpgfight -mindepth 1` prints nothing.
5. **IN ITS OWN MESSAGE, BEFORE THE BOOT COMMAND IS SENT:** "booting now", and Ben told **NOT to log in until told R0
   passed.** The boot command goes in the next turn.
6. `./scripts/dev-server.sh --refresh-content`. **The REQUIRED deployed lines** (under `run/plugins/Rpg/content`):
   - `weapons/iron_sword.yml` reads `attack_damage: 23` and `craft_result: iron_sword`;
   - `weapons/netherite_sword.yml` reads `attack_damage: 31` and has no `craft_result:` line;
   - `smithing.yml` reads `  - base: diamond_sword`.
7. R0a to R0e read on **this boot's** log (its `[Rpg] Build:` names this branch's head).
8. **IN ITS OWN MESSAGE: Ben told R0 passed.** Any R0 not as predicted: STOP, tell the seat, tell Ben NOT to log in.
9. *(After his `/stop`.)* Report:
   - `rpgfight` empty;
   - the overworld's file count against step 2's;
   - `All dimensions are saved`;
   - no `java` process left.

## THE RUN SHEET — BEN TYPES THESE, IN THIS ORDER. EVERY LINE IS LITERAL

0. **DO NOT LOG IN UNTIL CC HAS TOLD YOU R0 PASSED. IF YOU ARE TOLD NOTHING, DO NOT LOG IN.**
1. `/gamemode survival` — **alone, the first line after login.**
2. Stand on flat open ground with room all round.
3. `/rpg selftest sword-tiers` — **ONCE.** Five rows, about two minutes. Swords are handed to you and swung at frozen
   zombies. **Nothing hurts you.**
4. **STM1:** at a crafting table, craft one iron sword (two iron ingots and a stick) and take it. Then hold it and type
   `/rpg enchant show`.
5. `/rpg selftest sword-smithing SSA1` — **ONCE.** You are given an enchanted Diamond Sword, a netherite upgrade
   template and a netherite ingot.
6. **SSM1:** at a smithing table, put in the template, the Diamond Sword and the ingot, and take the Netherite Sword.
7. `/rpg selftest sword-smithing SSA2` — **ONCE.**
8. `/rpg selftest sword-convert` — **ONCE.** Plain swords are given to you and turn into ours. Then **three questions:
   answer each in chat.**
9. `/stop`

Nothing else this boot. If a row stalls or aborts, do not intervene.

## THE ARITHMETIC (SELFTEST SAFETY): AT DEFENCE 0, AGAINST 100, AND WHERE EACH ROW PUTS HIM

| row | where he is | what can hurt him | the most it does | puts him down? |
|---|---|---|---|---|
| STA1-STA5, SCA1 | the overworld, facing a NoAI zombie 2 blocks east | nothing: a NoAI zombie never swings | 0 | no |
| STA3 | the same, ten zombies in turn | nothing | 0 | no |
| STA4, SCA2-SCA5, SSA1, SSA2, SCB1-SCB3 | wherever he stands; commands only | nothing | 0 | no |
| STM1, SSM1 | at a crafting table, then a smithing table | nothing | 0 | no |

**No row puts him down.** No row sets `death_rules`, because no row can kill.

## R0 — THE BUILD, FOR THE WHOLE STACK. READ IT FIRST. IF ANY R0 FAILS, STOP

### R0a — the build line names THIS branch's head

| prediction | instrument | READING |
|---|---|---|
| `[Rpg] Build: <sha>`, `feat/sword-convert`'s head as `git ls-remote origin refs/heads/feat/sword-convert` prints it, shortened. Not `-dirty`, not `unknown` | `Select-String -Path run\logs\latest.log -Pattern '\[Rpg\] Build:' \| Select-Object -First 1` | **AS PREDICTED (2026-11-20, 13:56:27).** `[Rpg] Build: 90ea1517`; `git ls-remote origin refs/heads/feat/sword-convert` printed `90ea1517b1c5d8f05a40a431df50510b0794cd71`, the head after the seat's STA2 control commit and the restack. Not `-dirty`, not `unknown` |

### R0b — the jar carries all three steps

| prediction | instrument | READING |
|---|---|---|
| PRESENT: `content/weapons/iron_sword.yml` line `attack_damage: 23`; `content/weapons/netherite_sword.yml` line `attack_damage: 31`; `content/smithing.yml` line `  - base: diamond_sword`; the entries `core/weapon/SmithingUpgrades.class`, `core/weapon/ArrivalClaims.class`, `paper/weapon/ArrivalConverter.class`, `paper/content/SmithingLoader.class`; `RpgListeners.class` carrying `ARRIVAL minted` and `SMITH took`; `CraftResultIndex.class` carrying `Refusing craft_result`; the scenarios `GATE-sword-tiers.yml` `STA1:`, `GATE-sword-smithing.yml` `SSA1:`, `GATE-sword-convert.yml` `SCA1:`. ABSENT (the controls): `iron_sword.yml` line `attack_damage: 24`; `netherite_sword.yml` line `craft_result: netherite_sword` | the PowerShell below | **AS PREDICTED.** The jar written 13:56:20: every PRESENT line PRESENT; `attack_damage: 24` and `craft_result: netherite_sword` ABSENT |

```powershell
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::OpenRead((Resolve-Path 'run\plugins\rpg-0.1.0-SNAPSHOT.jar'))
function Bytes($name) { $e = $zip.GetEntry($name); if ($e -eq $null) { return $null }; $s = $e.Open(); $m = New-Object IO.MemoryStream; $s.CopyTo($m); $s.Close(); [Text.Encoding]::GetEncoding(28591).GetString($m.ToArray()) }
function Line($text, $l, $label) { if ($text -ne $null -and $text -match ("(?m)^" + [regex]::Escape($l) + "\s*$")) { "PRESENT $label '$l'" } else { "ABSENT  $label '$l'" } }
function Has($entry, $needle) { $b = Bytes $entry; if ($b -ne $null -and $b.Contains($needle)) { "PRESENT $entry '$needle'" } else { "ABSENT  $entry '$needle'" } }
function Entry($entry) { if ($zip.GetEntry($entry) -ne $null) { "PRESENT entry $entry" } else { "ABSENT  entry $entry" } }
$i = Bytes 'content/weapons/iron_sword.yml'; Line $i 'attack_damage: 23' 'iron_sword.yml'; Line $i 'attack_damage: 24' 'iron_sword.yml'
$n = Bytes 'content/weapons/netherite_sword.yml'; Line $n 'attack_damage: 31' 'netherite_sword.yml'; Line $n 'craft_result: netherite_sword' 'netherite_sword.yml'
Line (Bytes 'content/smithing.yml') '  - base: diamond_sword' 'smithing.yml'
$p = 'io/github/butterflysmp/rpg/'
Entry ($p + 'core/weapon/SmithingUpgrades.class'); Entry ($p + 'core/weapon/ArrivalClaims.class')
Entry ($p + 'paper/weapon/ArrivalConverter.class'); Entry ($p + 'paper/content/SmithingLoader.class')
Has ($p + 'paper/listener/RpgListeners.class') 'ARRIVAL minted'; Has ($p + 'paper/listener/RpgListeners.class') 'SMITH took'
Has ($p + 'core/weapon/CraftResultIndex.class') 'Refusing craft_result'
Line (Bytes 'selftest/GATE-sword-tiers.yml') 'STA1:' 'GATE-sword-tiers.yml'
Line (Bytes 'selftest/GATE-sword-smithing.yml') 'SSA1:' 'GATE-sword-smithing.yml'
Line (Bytes 'selftest/GATE-sword-convert.yml') 'SCA1:' 'GATE-sword-convert.yml'
$zip.Dispose()
```

### R0c — every Loaded line, and nothing refused

| prediction | instrument | READING |
|---|---|---|
| Exactly one of each, up to the first `joined the game`: **`[Rpg] Loaded 11 abilities, 30 visuals, 5 statuses, 7 elements, 11 enchants, 3 pools, 6 fragments, 6 aspects, 23 weapons, 1 shields, 24 armor, 5 tools, 6 accessories, 7 mobs, 1 mob elements, 3 recipes, 1 ores, 4 vein sizes`** (weapons 16 -> 23); **`GATE-fight-revive.md`'s R0c fight line, word for word**; **`[Rpg] Loaded world damage: void takes 0.10 of a player's maximum health per void tick, defence bypassed; mobs unchanged`**; **`GATE-loot-roll.md`'s R0c loot line, word for word**; **`[Rpg] Loaded smithing: 1 upgrades [diamond_sword + netherite_upgrade_smithing_template + netherite_ingot -> netherite_sword]`** (`SmithingLoaderTest` asserts it verbatim); **`[Rpg] Mint-on-craft: 36 indexed, 36 claiming, of 53 gear definitions`** (30, 30, 46 at the last boot; +6 claims, +7 weapons; no contested and no refused clause); **`[Rpg] Convert-on-arrival: 7 materials {copper_sword=copper_sword, diamond_sword=diamond_sword, golden_sword=golden_sword, iron_sword=iron_sword, netherite_sword=netherite_sword, stone_sword=stone_sword, wooden_sword=wooden_sword}`**. **No `Refusing`** (shipped content has no bad claim, no bad smithing row and no arrival contest: the seat's ruling), no `Skipping`, `SEVERE` or `Exception`. **No `[Rpg]` WARN beyond `volley_stone`'s known pair.** No `No fights loaded` | `Select-String run\logs\latest.log -Pattern 'Loaded\|Mint-on-craft\|Convert-on-arrival\|Refusing\|Skipping\|SEVERE\|WARN\|Exception\|No fights loaded'` | **AS PREDICTED.** Every predicted line once, each equal to its text by a scripted string comparison: content (23 weapons), fight, world damage, loot, `Loaded smithing: 1 upgrades […]`, **`Mint-on-craft: 36 indexed, 36 claiming, of 53 gear definitions`**, **`Convert-on-arrival: 7 materials {copper_sword=copper_sword, … wooden_sword=wooden_sword}`**. **No `Refusing` anywhere in the log (0).** `[Rpg]` WARN only `volley_stone`'s pair. No `Skipping`, `SEVERE`, `Exception` or `No fights loaded` |

### R0d — every scenario loaded

| prediction | instrument | READING |
|---|---|---|
| Exactly one **`[Rpg] Selftest: dev server, 250 scenarios across 33 gates [melee-cell, level, nexus-polish, legacy-a, legacy-b, wither-status, withered-shortbow, sunder, deft-hands, mining-auto, nexus-back, dragon-bodies, dragon-split, dragon-growth, dragon-blast, dragon-beam, dragon-teleport, dragon-shot, dragon-blink, dragon-skreel, drag-additive, fight-site, fight-arena, fight-dragon, fight-rules, fight-end, fight-down, fight-revive, fight-template, loot-roll, sword-tiers, sword-smithing, sword-convert]`**, no refusal after it. **The control:** the last boot's `235 scenarios across 30 gates` is ABSENT | `Select-String run\logs\latest.log -Pattern 'Selftest:'` | **AS PREDICTED.** `250 scenarios across 33 gates [...]` once, equal by a scripted comparison; `235 scenarios across 30 gates` ABSENT (0) |

**RECOUNTED** (2026-11-20, this branch's tree, from `index.yml` and every gate file it names; `SelfTestScenariosTest`
asserts 250):
- the gates: **33**;
- `^[A-Za-z0-9_-]+:` over them: **283**;
- `^setup:`: **33**;
- so **250**.

**Control:** 265 (the last boot) + sword-tiers' 6 (setup, STA1-STA5) + sword-smithing's 3 (setup, SSA1, SSA2) +
sword-convert's 9 (setup, SCA1-SCA5, SCB1-SCB3) = 283.

### R0e — THE ORPHAN SWEEP WITH NOTHING STAGED

| prediction | instrument | READING |
|---|---|---|
| During the enable, exactly one **`FIGHTWORLD sweep path=<abs>\run\world\dimensions\rpgfight folders=0`**, and no `deleting` and no `orphan` line | `Select-String run\logs\latest.log -Pattern 'FIGHTWORLD (sweep\|deleting\|orphan)'` | **AS PREDICTED.** One `FIGHTWORLD sweep … rpgfight folders=0` (13:56:28); no `deleting`, no `orphan` |

## THE ROWS (`/rpg selftest sword-convert`) — SURVIVAL

### SCJ1 — his first join: any plain claimed sword already in his inventory converts (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| At his first join, before any row, one `ARRIVAL minted … via=join` line per plain claimed sword in his saved inventory (any vanilla sword he carried before this build). **If he carries none, no such line, and the join arm is UNREAD on a server** (`ArrivalSourceTest` only). There is no row to stage a rejoin: a plain sword cannot be held across a logout once the live route converts it | `ArrivalSourceTest` *aPlainSwordAlreadyHeldConvertsOnJoinAfterTheRefresh* | **NO JOIN CONVERSION: THE JOIN ARM IS UNREAD ON A SERVER.** He joined at 13:57:44 and again at 14:43:37. **No `ARRIVAL … via=join` line at either**: he held no plain claimed sword. The arm stands on `ArrivalSourceTest` only |

### SCA1 — a plain iron sword given by /give is minted on arrival (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| `give @s minecraft:iron_sword 1`; within 5 ticks **`ARRIVAL minted player=BaronVonYeetus slot=<s> material=iron_sword weapon=iron_sword wear=0 vanilla_enchants=0 score=<T> via=slot-change`**. `probe_inventory: iron_sword` finds it at slot s. `dump: held`: **"Iron Sword"** (common colour), `Kinetic`, `Gear Score: <T>`, `Melee Damage: <23 x T/100>`, `Attack Speed: 1.6`, `Common Melee Weapon`. One `PLAYERHIT … source=iron_sword/left_click … sent=<23 x T/100>`, with T this line's `triggerScore` and the ARRIVAL line's `score` | `ArrivalClaimsTest`; `ArrivalSourceTest` | **AS PREDICTED.** "Gave 1 [Iron Sword]", then on the same second **`ARRIVAL minted player=BaronVonYeetus slot=2 material=iron_sword weapon=iron_sword wear=0 vanilla_enchants=0 score=100 via=slot-change`**. `INVENTORY iron_sword slots=[0x1, 2x1]`: the converted sword at slot 2, and another minted iron sword at slot 0. `hold:` takes the first, so the HELD dump may be slot 0's; it reads exactly as predicted either way: "Iron Sword" white, `Kinetic`, `Gear Score: 100`, `Melee Damage: 23`, `Attack Speed: 1.6`, `Common Melee Weapon`. PLAYERHIT `source=iron_sword/left_click … sent=46.000 crit=true triggerScore=100` (23 x 100/100 x the crit's 2.0) |

### SCA2 — a plain netherite sword (loot's case) converts too (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| **`ARRIVAL minted … material=netherite_sword weapon=netherite_sword wear=0 vanilla_enchants=0 … via=slot-change`**. `dump: held`: "Netherite Sword", uncommon colour, `Melee Damage: <31 x T/100>`, `Uncommon Melee Weapon` | `ArrivalClaimsTest` *theNetheriteSwordIsClaimedAsTheResultOfItsUpgrade* | **AS PREDICTED.** **`ARRIVAL minted … slot=3 material=netherite_sword weapon=netherite_sword wear=0 vanilla_enchants=0 score=100 via=slot-change`**. `HELD netherite_sword … "Netherite Sword" [white, green]`, `Melee Damage: 31`, `Uncommon Melee Weapon` [green] |

### SCA3 — vanilla enchantment and wear on a converted sword (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| The `give` with components succeeds (READ, its REPLY; **if the command's component syntax is refused, the row says so and reads nothing else**). **`ARRIVAL minted … material=diamond_sword weapon=diamond_sword wear=40 vanilla_enchants=1 …`**. `rpg enchant show`: **our roll only**; the vanilla Sharpness 5 is not carried (flagged). `rpg durability damage 1`: REPLY **`… /1561 uses left (damage 41)`**: the vanilla wear of 40 carried, plus the read | `ArrivalConverter` (wear carried, enchantments not) | **AS PREDICTED.** The give with components was accepted ("Gave 1 [Diamond Sword]"). **`ARRIVAL minted … slot=4 material=diamond_sword weapon=diamond_sword wear=40 vanilla_enchants=1 score=100 via=slot-change`**. `rpg enchant show`: **our roll only**, raw `v1;sharpness=0:-1;sharpness=0:-1;sharpness=0:-1`: the vanilla Sharpness 5 is gone. `rpg durability damage 1`: **`1520/1561 uses left (damage 41)`**: the vanilla wear of 40 carried, plus the read's 1. That also proves this is the converted sword held |

### SCA4 — a minted sword arriving is left alone (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| `rpg give ironblade` (minted, on `iron_sword`): **no ARRIVAL line names `ironblade` or a slot it holds**; `probe_inventory: ironblade` finds it, still ironblade | `ArrivalClaimsTest` *aTaggedItemIsLeftWhateverItsMaterial* | **AS PREDICTED.** "Given Ironblade"; `INVENTORY ironblade slots=[5x1]`; **no ARRIVAL line in the row** |

### SCA5 — an unclaimed item is left alone (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| `give @s minecraft:stick 1`: **no ARRIVAL line** in the row | `ArrivalClaimsTest` *anUnclaimedItemIsLeft* | **AS PREDICTED.** "Gave 1 [Stick]"; **no ARRIVAL line in the row** |

### SCB1 — question 1, for STM1 (`GATE-sword-tiers.md` STB1)

| prediction | stands on | READING |
|---|---|---|
| His answer, verbatim, to **"Is the crafted Iron Sword's name, colour and tooltip what you expect?"** (words). No PASS or FAIL; "not sure" is NOT STATED | STM1 | His answer, verbatim (14:46:30): **"yes"** |

### SCB2 — question 2, for SSM1 (`GATE-sword-smithing.md` SSB1)

| prediction | stands on | READING |
|---|---|---|
| His answer, verbatim, to **"Did the smithed Netherite Sword keep its enchantments, as far as you can see?"** (yes or no) | SSM1, SSA2 | His answer, verbatim (14:46:36): **"yes"** |

### SCB3 — question 3, for SCA1

| prediction | stands on | READING |
|---|---|---|
| His answer, verbatim, to **"When the plain sword you were given turned into an Iron Sword in your inventory, was it clear what happened?"** (yes or no) | SCA1 | His answer (14:46:59) was **"not sure"**, so it is **NOT STATED** |

## FLAGGED FOR BEN

- **A converted sword's VANILLA ENCHANTMENTS are dropped**, replaced by our roll (a villager's Sharpness V, a mob's
  enchanted sword). Its wear is kept.
- **The netherite sword's claim is expressed as "the result of a smithing upgrade claims its material".** No new content
  key was added. A netherite sword from loot therefore converts to `netherite_sword` with a fresh roll and stamp. It
  does not come from a diamond sword, so it carries no enchants of its own.

## WHAT THIS GATE DOES NOT READ

- **Pickup, container, trade and creative arrivals on a server:** only `/give` is staged (SCA1-SCA3). The rest rest on
  the READ of the one event they all reach. A pickup row would need a dropped item and his hands.
- **Converting armour, tools or shields on arrival:** weapons only, by design.

## THE BOOT, 2026-11-20, 13:56 TO 14:47 — THE CHECKLIST, THE AFTER-RUN, AND WHAT IT FOUND

Written after Ben's `/stop`. **No prediction above was edited.** The readings are in the cells above and in each row's
own gate (`GATE-sword-tiers.md`, `GATE-sword-smithing.md`).

### The booter's checklist (`.claude/rules/booting.md`), ticked

1. **The seat's word and Ben's:** the seat's `BOOT APPROVED for the stack PR #221 + #222 + #223, 2026-11-20, WITH ONE
   COMMIT FIRST`, then Ben's "ready" (his own message).
2. **The one commit first:** STA2's control, on `feat/sword-tiers` as `b05211d1`. Then `feat/sword-smithing` was
   rebased to `c21d2ab4` and `feat/sword-convert` to `90ea1517` with `--onto`, all four patch ids unchanged. The heads
   before it were the seat's (`e3e0440f`, `b1276d10`, `3538f2f7`).
3. **The carried state, in order:**
   1. `Get-Process java` printed nothing.
   2. Backup `../rpg-run-backups/2026-11-20-pre-sword-ladder` (13:08:45):
      - Rpg `files=136 dirs=26`, `diff -rq lines: 0`;
      - world `files=193 dirs=32`, `diff -rq lines: 0`;
      - `world/dimensions`: `minecraft`, `rpgfight`;
      - the overworld: **106**.
   3. `12:  dev-pack-on-join: true`.
   4. `rpgfight` empty.
4. **"Booting now", in its own message**, with "Ben: do NOT log in until I tell you R0 passed". The turn ended there,
   and the boot command came after Ben's "ok".
5. `./scripts/dev-server.sh --refresh-content`; `Done` at 13:56:28. The REQUIRED deployed lines:
   - `iron_sword.yml` `17: attack_damage: 23`, `20: craft_result: iron_sword`;
   - `netherite_sword.yml` `17: attack_damage: 31`, and no `craft_result:`;
   - `smithing.yml` `11:   - base: diamond_sword`.
6. **R0a to R0e read on this boot's log**, `[Rpg] Build: 90ea1517`: all as predicted, and no Refusing line.
7. **"R0 passed", in its own message**, with the R0 lines read at 13:56:54.
8. **His login against it:** every R0 line is at 13:56:27 to 13:56:28; his first `joined the game` is **13:57:44**,
   after "R0 passed".
9. **After his `/stop` (14:47:22):**
   - `rpgfight` empty;
   - **the overworld 106 files, against the backup's 106**;
   - `Saving worlds`, `All dimensions are saved` and `All RegionFile I/O tasks to complete`, all at 14:47:22;
   - `Get-Process java` printed nothing.

### What he typed, in order (every `issued server command` line), and how it differs from the sheet

1. `/gamemode survival` (13:57:52).
2. `/rpg selftest sword-tiers` (13:57:55): **after golden, every row SKIPPED, `no hotbar slot holds …`**. His hotbar was
   full, and the scenario gives each sword and then selects it from the hotbar.
3. `/rpg selftest sword-tiers` **again** (13:58:37): STA1-STA4 ran; STA5 SKIPPED (hotbar).
4. `/rpg selftest sword-smithing` **with no row named** (14:00:00): SSA1 ran, and SSA2 SKIPPED before any smith.
5. He disconnected at 14:00:05 and rejoined at 14:43:37.
6. `/give BaronVonYeetus minecraft:crafting_table`, `… iron_ingot 2`, `… stick 1` (14:44:25-38), then `/rpg enchant show`
   (14:44:57): STM1.
7. `/gamemode creative` (14:45:08), `survival` (14:45:20), `creative` (14:45:24), `survival` (14:45:30). A diamond sword
   from the creative menu converted at 14:45:11. He smithed it at 14:45:34; it was not SSA1's sword.
8. `/rpg selftest sword-smithing SSA2` (14:45:46).
9. `/rpg selftest sword-convert` (14:46:18), with his three answers.
10. `/gamemode creative` (14:47:18), then `/stop` (14:47:22).

### The lines the seat asked for

- **SCJ1:** no `ARRIVAL … via=join` at either join, so he held no plain claimed sword.
- **STA1 (second run):** seven PLAYERHIT lines, every score 100. Each `sent` equals number x 100/100: 11, 14, 17,
  golden 40 on a crit (20 x 2.0), 23, 27, 31. The first run read wooden, stone and copper at 100 and golden 22.800 at
  score 114 (20 x 114/100).
- **STA2:** the control pair `90.000` and `90.000` EQUAL; the after-swing `78.500`. The zombie was burning
  (`fireTicks` 151 and 146).
- **STA3:** `250/250 (damage 0)` then `239/250 (damage 11)`. **Single wear: vanilla does not also wear a minted
  sword.**
- **STA4:** sharpness and unbreaking only (read on the netherite sword).
- **STA5:** skipped twice. Ironblade's arrival is read in SCA4.
- **STM1:** no server line exists; his answer is "yes".
- **SSM1 beside SSA1:**

  | | SSA1's sword | the sword SSM1 took |
  |---|---|---|
  | from | `diamond_sword` | `diamond_sword` (the creative one) |
  | to | — | `netherite_sword` |
  | wear | 100 / 1561 | 0 / 2031 |
  | enchant data | `v1;unbreaking=0,sharpness=0:-1` x3 | `v1;unbreaking=0,sharpness=0:-1;sharpness=0,unbreaking=0:-1;sharpness=0:-1` |
  | score | not printed | 100 |

  SSA2 read the smithed sword's blob byte for byte.
- **SCA1-SCA5 ARRIVAL lines:**
  - iron: `wear=0 vanilla_enchants=0 via=slot-change`;
  - netherite: `wear=0 vanilla_enchants=0 via=slot-change`;
  - diamond: `wear=40 vanilla_enchants=1 via=slot-change`;
  - ironblade and the stick: none.
  - Also the creative diamond sword (14:45:11): `wear=0 vanilla_enchants=0 via=slot-change`.
- **The Convert-on-arrival Loaded line:** as predicted, word for word.
- **`[Rpg]` lines matching `Refusing|Skipping|SEVERE|WARN|Exception`:** only `volley_stone`'s pair, in the whole log.

### WHAT IT FOUND

1. **Every rule built reads on a server as built.**
   - The seven numbers, scaled by score.
   - The sweep's 0.5 share.
   - Single wear (part i needs no fix).
   - Only sharpness and unbreaking offered.
   - The upgrade carrying enchants and score.
   - Conversion on arrival by `/give` and **by the creative menu** (a route the gate marked INFERRED, read here).
   - Wear carried and vanilla enchantments dropped.
   - Tagged and unclaimed items left alone.
2. **THE SCENARIOS ASSUME FREE HOTBAR SLOTS, AND HIS WAS FULL.** `hold:` selects only from the hotbar, so every row that
   gives a sword and then holds it SKIPS when the hotbar is full. It cost a second run of sword-tiers, and STA5 twice.
   **A staging defect of these gates, not of the code.**
3. **The first run's golden sword read score 114, not 100**: the gear-score stamp is banded on his average, as built.
   The prediction was in terms of T, so it held.
4. **STA2's zombie was burning.** The control pair held still over its 5 ticks, and the after-swing fall was exactly
   the sweep's share. **A NoAI zombie in daylight burns** (READ in the jar, before the boot; seen here as `fireTicks`).
   Any future row reading a mob's health in daylight needs night, a helmet, or this control.
5. **SSM1 smithed a different diamond sword than SSA1's**, one taken from the creative menu. The upgrade is read; the
   "wear 100 carried" is unread.
6. **STM1 has no server witness.** A craft writes no log line, and a player's own command reply is not logged.
7. **His answer to question 3 was "not sure"**: NOT STATED. Whether the conversion was clear to him is open.

---

> **2026-11-21, THE FOUR DEV WEAPONS DELETED** (`feat/remove-dev-weapons`, `GATE-remove-dev-weapons.md`; the seat's
> brief, rulings 1 and 2). **This gate's scenario changed: SCA4 now gives and probes the minted `wooden_sword`**, where it
> gave and probed `ironblade`, a dev weapon deleted that day. Its meaning is kept: a minted item on a material a weapon
> claims is left alone because it is tagged. Wooden, because no other row in the file gives or converts one. **Every
> prediction and reading above was taken with the old weapon** and none is edited. **A re-run of SCA4 needs a
> re-prediction under the next suffix.** NOT RE-READ on a server.
