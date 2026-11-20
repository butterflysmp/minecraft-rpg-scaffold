# GATE — THE SWORD LADDER, PR 3: CONVERT ON ARRIVAL (feat/sword-convert), AND THE ONE BOOT FOR ALL THREE

**Status: NOT RUN.** Written 2026-11-20 on `feat/sword-convert`, stacked on `feat/sword-smithing` (#222), which is
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
| `[Rpg] Build: <sha>`, `feat/sword-convert`'s head as `git ls-remote origin refs/heads/feat/sword-convert` prints it, shortened. Not `-dirty`, not `unknown` | `Select-String -Path run\logs\latest.log -Pattern '\[Rpg\] Build:' \| Select-Object -First 1` | |

### R0b — the jar carries all three steps

| prediction | instrument | READING |
|---|---|---|
| PRESENT: `content/weapons/iron_sword.yml` line `attack_damage: 23`; `content/weapons/netherite_sword.yml` line `attack_damage: 31`; `content/smithing.yml` line `  - base: diamond_sword`; the entries `core/weapon/SmithingUpgrades.class`, `core/weapon/ArrivalClaims.class`, `paper/weapon/ArrivalConverter.class`, `paper/content/SmithingLoader.class`; `RpgListeners.class` carrying `ARRIVAL minted` and `SMITH took`; `CraftResultIndex.class` carrying `Refusing craft_result`; the scenarios `GATE-sword-tiers.yml` `STA1:`, `GATE-sword-smithing.yml` `SSA1:`, `GATE-sword-convert.yml` `SCA1:`. ABSENT (the controls): `iron_sword.yml` line `attack_damage: 24`; `netherite_sword.yml` line `craft_result: netherite_sword` | the PowerShell below | |

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
| Exactly one of each, up to the first `joined the game`: **`[Rpg] Loaded 11 abilities, 30 visuals, 5 statuses, 7 elements, 11 enchants, 3 pools, 6 fragments, 6 aspects, 23 weapons, 1 shields, 24 armor, 5 tools, 6 accessories, 7 mobs, 1 mob elements, 3 recipes, 1 ores, 4 vein sizes`** (weapons 16 -> 23); **`GATE-fight-revive.md`'s R0c fight line, word for word**; **`[Rpg] Loaded world damage: void takes 0.10 of a player's maximum health per void tick, defence bypassed; mobs unchanged`**; **`GATE-loot-roll.md`'s R0c loot line, word for word**; **`[Rpg] Loaded smithing: 1 upgrades [diamond_sword + netherite_upgrade_smithing_template + netherite_ingot -> netherite_sword]`** (`SmithingLoaderTest` asserts it verbatim); **`[Rpg] Mint-on-craft: 36 indexed, 36 claiming, of 53 gear definitions`** (30, 30, 46 at the last boot; +6 claims, +7 weapons; no contested and no refused clause); **`[Rpg] Convert-on-arrival: 7 materials {copper_sword=copper_sword, diamond_sword=diamond_sword, golden_sword=golden_sword, iron_sword=iron_sword, netherite_sword=netherite_sword, stone_sword=stone_sword, wooden_sword=wooden_sword}`**. **No `Refusing`** (shipped content has no bad claim, no bad smithing row and no arrival contest: the seat's ruling), no `Skipping`, `SEVERE` or `Exception`. **No `[Rpg]` WARN beyond `volley_stone`'s known pair.** No `No fights loaded` | `Select-String run\logs\latest.log -Pattern 'Loaded\|Mint-on-craft\|Convert-on-arrival\|Refusing\|Skipping\|SEVERE\|WARN\|Exception\|No fights loaded'` | |

### R0d — every scenario loaded

| prediction | instrument | READING |
|---|---|---|
| Exactly one **`[Rpg] Selftest: dev server, 250 scenarios across 33 gates [melee-cell, level, nexus-polish, legacy-a, legacy-b, wither-status, withered-shortbow, sunder, deft-hands, mining-auto, nexus-back, dragon-bodies, dragon-split, dragon-growth, dragon-blast, dragon-beam, dragon-teleport, dragon-shot, dragon-blink, dragon-skreel, drag-additive, fight-site, fight-arena, fight-dragon, fight-rules, fight-end, fight-down, fight-revive, fight-template, loot-roll, sword-tiers, sword-smithing, sword-convert]`**, no refusal after it. **The control:** the last boot's `235 scenarios across 30 gates` is ABSENT | `Select-String run\logs\latest.log -Pattern 'Selftest:'` | |

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
| During the enable, exactly one **`FIGHTWORLD sweep path=<abs>\run\world\dimensions\rpgfight folders=0`**, and no `deleting` and no `orphan` line | `Select-String run\logs\latest.log -Pattern 'FIGHTWORLD (sweep\|deleting\|orphan)'` | |

## THE ROWS (`/rpg selftest sword-convert`) — SURVIVAL

### SCJ1 — his first join: any plain claimed sword already in his inventory converts (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| At his first join, before any row, one `ARRIVAL minted … via=join` line per plain claimed sword in his saved inventory (any vanilla sword he carried before this build). **If he carries none, no such line, and the join arm is UNREAD on a server** (`ArrivalSourceTest` only). There is no row to stage a rejoin: a plain sword cannot be held across a logout once the live route converts it | `ArrivalSourceTest` *aPlainSwordAlreadyHeldConvertsOnJoinAfterTheRefresh* | |

### SCA1 — a plain iron sword given by /give is minted on arrival (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| `give @s minecraft:iron_sword 1`; within 5 ticks **`ARRIVAL minted player=BaronVonYeetus slot=<s> material=iron_sword weapon=iron_sword wear=0 vanilla_enchants=0 score=<T> via=slot-change`**. `probe_inventory: iron_sword` finds it at slot s. `dump: held`: **"Iron Sword"** (common colour), `Kinetic`, `Gear Score: <T>`, `Melee Damage: <23 x T/100>`, `Attack Speed: 1.6`, `Common Melee Weapon`. One `PLAYERHIT … source=iron_sword/left_click … sent=<23 x T/100>`, with T this line's `triggerScore` and the ARRIVAL line's `score` | `ArrivalClaimsTest`; `ArrivalSourceTest` | |

### SCA2 — a plain netherite sword (loot's case) converts too (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| **`ARRIVAL minted … material=netherite_sword weapon=netherite_sword wear=0 vanilla_enchants=0 … via=slot-change`**. `dump: held`: "Netherite Sword", uncommon colour, `Melee Damage: <31 x T/100>`, `Uncommon Melee Weapon` | `ArrivalClaimsTest` *theNetheriteSwordIsClaimedAsTheResultOfItsUpgrade* | |

### SCA3 — vanilla enchantment and wear on a converted sword (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| The `give` with components succeeds (READ, its REPLY; **if the command's component syntax is refused, the row says so and reads nothing else**). **`ARRIVAL minted … material=diamond_sword weapon=diamond_sword wear=40 vanilla_enchants=1 …`**. `rpg enchant show`: **our roll only**; the vanilla Sharpness 5 is not carried (flagged). `rpg durability damage 1`: REPLY **`… /1561 uses left (damage 41)`**: the vanilla wear of 40 carried, plus the read | `ArrivalConverter` (wear carried, enchantments not) | |

### SCA4 — a minted sword arriving is left alone (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| `rpg give ironblade` (minted, on `iron_sword`): **no ARRIVAL line names `ironblade` or a slot it holds**; `probe_inventory: ironblade` finds it, still ironblade | `ArrivalClaimsTest` *aTaggedItemIsLeftWhateverItsMaterial* | |

### SCA5 — an unclaimed item is left alone (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| `give @s minecraft:stick 1`: **no ARRIVAL line** in the row | `ArrivalClaimsTest` *anUnclaimedItemIsLeft* | |

### SCB1 — question 1, for STM1 (`GATE-sword-tiers.md` STB1)

| prediction | stands on | READING |
|---|---|---|
| His answer, verbatim, to **"Is the crafted Iron Sword's name, colour and tooltip what you expect?"** (words). No PASS or FAIL; "not sure" is NOT STATED | STM1 | |

### SCB2 — question 2, for SSM1 (`GATE-sword-smithing.md` SSB1)

| prediction | stands on | READING |
|---|---|---|
| His answer, verbatim, to **"Did the smithed Netherite Sword keep its enchantments, as far as you can see?"** (yes or no) | SSM1, SSA2 | |

### SCB3 — question 3, for SCA1

| prediction | stands on | READING |
|---|---|---|
| His answer, verbatim, to **"When the plain sword you were given turned into an Iron Sword in your inventory, was it clear what happened?"** (yes or no) | SCA1 | |

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
