# GATE — THE SWORD LADDER, PR 1: THE SEVEN SWORDS AS CONTENT (feat/sword-tiers)

**Status: NOT RUN, and no boot of its own.** Written 2026-11-20 on `feat/sword-tiers` (on master `70cbab66`), before any
boot. **Every prediction below was written before the boot.**

> **SUPERSEDED IN PART, 2026-11-20 (the seat's split ruling): THE R0 AND THE RUN SHEET ARE NOT HERE.** Three PRs are
> stacked (`feat/sword-tiers`, `feat/sword-smithing`, `feat/sword-convert`) with ONE boot on the top.
> `GATE-sword-convert.md` carries the one R0 and the one run sheet for all three. This file's rows are run on that boot,
> and their readings go in this file's cells, as each row's own.

**Game mode:** SURVIVAL, every row. **Difficulty:** normal (the setup sets it). **Nothing here can hurt him:** every
mob is a NoAI zombie, which never swings, and nothing else is staged. The arithmetic: **0 against his 100.**

**Ben's words** (PLAN-weapon-tiers.md 1.4, verbatim): "Wood - 11 / Stone - 14 / Copper - 17 / Gold - 20 / Iron - 23 /
Diamond - 27 / Netherite - 31", "Yes all swords should have the built in sweep", "Names plan vanilla names with the
rarity color".

## WHAT THIS BRANCH IS

- **Seven weapon files**, the shape of ironblade's basic attack (the vanilla-driven left click, `weapon_damage`,
  kinetic), no special, no flavour (Ben gave none, and `SwordTiersContentTest` shows a weapon loads cleanly without it).
  Attack speed 1.6, sweep 0.5, class melee, element kinetic.
- **The ids are the vanilla item names:** `wooden_sword`, `stone_sword`, `copper_sword`, `golden_sword`, `iron_sword`,
  `diamond_sword`, `netherite_sword`.
  - **Why:** Ben ruled vanilla names. The id then equals both its `material` and its `craft_result`, so the claim can be
    checked by eye. And `/rpg give iron_sword` reads as what it gives.
  - **The cost, named:** a grep for `iron_sword` finds the weapon id and the material alike. `ironblade` and `emberblade`
    also author `iron_sword` as their material.
- **`craft_result` on six; netherite has none** (it is smithed, PR 2). **Exactly one gear claims each crafted sword**
  (`SwordTiersContentTest`, over every shipped weapon, shield, armour piece and tool).
- **FAIL CLOSED** (the seat's ruling, 2026-11-20). A claim that is not a material, is not its own material, or has no
  durability is **refused by name at boot** (`Refusing craft_result on gear '<id>': …`) and never indexed. A contest is
  `Refusing craft_result '<x>': gear … all claim it, so neither …`. A refused claimant still contests a sound one, so
  neither mints. **Shipped content has none of either**; the refusals are unit tests (`CraftResultIndexTest`,
  `CraftResultValidationTest`).
- **The enchant pool:** sharpness and unbreaking, by class alone (`SwordTiersContentTest`).
- **The tooltip, as minted:** `Kinetic`, `Gear Score: <s>`, blank, `Melee Damage: <N x s/100>`, `Attack Speed: 1.6`,
  blank, `<Rarity> Melee Weapon`, the rarity line in the rarity colour (white common, green uncommon). The display name is
  the vanilla name in the rarity colour. Pinned at score 100 and 150 by `SwordTiersContentTest`, and unscored in
  `golden-lore.txt`.
- **`ironblade` and `emberblade` are UNTOUCHED.** They keep `iron_sword` as their material and claim nothing, so:
  - crafting an iron sword now mints **Iron Sword**;
  - a player already holding an ironblade or emberblade sees **no change** (its file, tooltip and damage are as before;
    STA5 reads it).

**A FINDING FOR THE SEAT, from the code:** a minted sword deals and shows **Ben's number x its gear score / 100**
(`WeaponLore`'s ruling, `WeaponLoreTest`). Ben's 23 is what an Iron Sword reads and deals at gear score 100; at 150 it
reads 34.5. The brief's "his number" is the authored figure, which is the score-100 figure.

## THE AUTO ROWS (`/rpg selftest sword-tiers`) — SERVER ONLY, SURVIVAL

**Staging common to every row:** level 1 (`rpg playerxp set @s 1 levels`, so no level damage), aim level facing east,
mob trace on. Each mob is a **vanilla zombie, NoAI, at `~2 ~ ~0`**, seeded by our mob seeding; **its defence is read
from its `MOBSEED` line, not predicted.** Every swing waits 40 ticks: **a full charge.** The gear score is T, the
`triggerScore` of the sword's own PLAYERHIT line. `/rpg give` stamps it; WD1 read **100** for a given weapon at
`38cc9903`. **Every prediction is against PLAYERHIT's `sent`, which is the damage BEFORE the mob's defence, never a
health figure.**

### STA1 — each of the seven swords, one full-charge hit (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| Seven `PLAYERHIT … source=<id>/left_click target=<uuid> zombie … element=kinetic sent=<N x T/100> crit=false triggerScore=<T>`, in this order. **At T=100: `wooden_sword` 11.000, `stone_sword` 14.000, `copper_sword` 17.000, `golden_sword` 20.000, `iron_sword` 23.000, `diamond_sword` 27.000, `netherite_sword` 31.000.** A line with `crit=true` reads `sent` = that figure x his crit multiplier, and is read with it, not failed. Any other T: N x T/100. Each sword's `REPLY` from `/rpg give` names it | `SwordTiersContentTest`; WD1 (sent = authored at L1, T=100) | **AS PREDICTED, ON THE SECOND RUN (2026-11-20, build `90ea1517`; the boot is recorded in `GATE-sword-convert.md`).** The FIRST run (13:57:55) read wooden 11.000, stone 14.000 and copper 17.000 at T=100, and golden **22.800 at T=114** (20 x 114/100). Then **iron, diamond and netherite SKIPPED: `no hotbar slot holds iron_sword`**: his hotbar was full, a staging defect of this file's (each sword is given, then selected from the hotbar). **The SECOND run (13:58:37), all seven, every T=100:** `wooden_sword` **11.000**, `stone_sword` **14.000**, `copper_sword` **17.000**, `golden_sword` **`sent=40.000 crit=true`** (20 x his crit multiplier, which reads 2.0), `iron_sword` **23.000**, `diamond_sword` **27.000**, `netherite_sword` **31.000**. Every zombie `MOBSEED … gs=100 … max=100`. Each `/rpg give` REPLY named its sword: "Given Wooden Sword" … "Given Netherite Sword", common white and uncommon green |

### STA2 — the sweep onto a second frozen zombie (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| The iron sword hits `t` (STA1's iron line: `sent=23.000` at T=100). `u`, one block to its side, is swept. **`u`'s PROBE custom HP falls between the two PROBEs, by no more than 11.500 (half of the primary's 23.000) and by more than 0.** The sweep is applied straight to the store with ironblade's share, 0.5 (`SweepShare.of`, READ), and the zombie's defence then cuts it; the exact landed figure is INFERRED, not predicted. **No PLAYERHIT names `u`** (the sweep is not traced; INFERRED) | `RpgListeners.onPlayerSweepAttack` (READ); `WeaponLoaderTest` (sweep 0.5) | **AS PREDICTED, AND THE CONTROL HELD, ON A BURNING ZOMBIE.** The first run SKIPPED (hotbar). The second run: **the control pair, 5 ticks apart, `customHP=90.000/100.000` and `90.000/100.000`: EQUAL**. But `fireTicks=151` and `146`: **`u` WAS BURNING** (the dated note's READ), and it had already lost 10 before the first probe. The iron sword's PLAYERHIT `sent=23.000`. The after-swing PROBE: **`customHP=78.500`, a fall of exactly 11.500**, which is 0.5 x 23, the predicted bound (no more than 11.500, more than 0). No PLAYERHIT names `u`. **The 11.500 is read as the sweep's, because it is exactly the share and the control pair held still; a fire tick inside that window would have shown as more than 11.500** |

> **A CONTROL ADDED BEFORE ANY BOOT (2026-11-20, the seat's boot approval).** As first written, STA2 read `u`'s health
> before and after the swing and predicted a fall. Nothing showed that `u`'s health holds still WITHOUT a swing: the
> row runs at time 6000 with a NoAI zombie in the open. The scenario now reads `probe: u`, `wait: 5`, `probe: u`, then
> the attack and the third `probe: u`.
>
> - **The added prediction: the first two PROBEs read the SAME custom HP.** If they differ, something other than the
>   sweep moves `u`'s health, and the after-swing fall is not read as the sweep's.
> - **What the jar says, marked; the row does not rely on it:**
>   - READ, the pinned server jar: `Mob.aiStep` calls `burnUndead()` for any mob in `EntityTypeTags.BURN_IN_DAYLIGHT`,
>     with no NoAI check on that path. INFERRED: `aiStep` runs for a NoAI mob (`LivingEntity.tick` calls it).
>   - READ, our source: `FIRE_TICK` is REROUTE in `VanillaDamagePolicy`, so a burning zombie's fire reaches its custom
>     HP.
>   - **So at time 6000 under open sky the control may well read UNEQUAL**: the zombie burning. That is the control
>     doing its job, and the row then says so.
> - The table above is not edited.

### STA3 — wear: ten hits wear the sword by exactly ten (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| `rpg enchant clear`, then `rpg durability set 0`: REPLY `<u>/<max> uses left (damage 0)`, max read. Ten iron-sword PLAYERHIT lines, each on a fresh zombie 45 ticks apart. Then `rpg durability damage 1`: **REPLY `… (damage 11)`**, which is ten wears of 1 (our pipeline's `WEAR_PER_USE`, one per mob per 10-tick window) plus the read's own 1. **`(damage 21)` would read DOUBLE WEAR**: vanilla's own `Weapon(1)` component wearing it a second time on each hit (the survey's 2g risk, INFERRED from the jar). Any other figure is read as it stands. **The fix for double wear is built only if this reads 21, as a follow-up** | `WeaponDurability` (READ); PLAN-weapon-tiers.md 2g | **AS PREDICTED: SINGLE WEAR. VANILLA DOES NOT ALSO WEAR A MINTED SWORD.** The second run: `Enchant data cleared -- both keys removed.`; **`250/250 uses left (damage 0)`**; ten iron-sword PLAYERHIT lines, all `sent=23.000 crit=false triggerScore=100`, each on a fresh zombie; then **`239/250 uses left (damage 11)`**: ten wears plus the read's 1. **Not 21, so no `PlayerItemDamageEvent` fix is owed** (the seat's part i). The first run SKIPPED (hotbar) |

### STA4 — a tier sword is offered sharpness and unbreaking only (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| `rpg enchant show` on a minted Iron Sword, then on a minted Netherite Sword: every candidate either REPLY names is **`sharpness` or `unbreaking`**, and no other enchant id appears. The rolled count is the roll's (at most 2, the pool's size) | `SwordTiersContentTest` *aTierSwordIsOfferedSharpnessAndUnbreakingOnly*; `EnchantRoll.poolFor` | **AS PREDICTED FOR THE NETHERITE SWORD; THE IRON HALF IS UNREAD.** The second run: both `rpg enchant show` replies came back as **"Netherite Sword -- 3 slot(s)"**: Slot 0 `[unbreaking locked]`, Slot 1 `[sharpness locked, unbreaking locked]`, Slot 2 `[sharpness locked, unbreaking locked]`, raw `v1;unbreaking=0:-1;sharpness=0,unbreaking=0:-1;sharpness=0,unbreaking=0:-1`, printed twice. **Only sharpness and unbreaking.** The first command's reply arrived after the second `hold:`, so the iron sword's own state was not printed. The first run SKIPPED (hotbar) |

### STA5 — ironblade and emberblade are untouched (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| `dump: held` on a given ironblade: its tooltip as `golden-lore.txt`'s ironblade block, with a `Gear Score: <T>` line under `Kinetic` and `Melee Damage: <8 x T/100>`. One `PLAYERHIT … source=ironblade/left_click … sent=<8 x T/100>`, **8.000 at T=100.** `dump: held` on a given emberblade: its tooltip as before this branch | `golden-lore.txt` (ironblade unchanged, +50/−1 only for the seven new blocks and the count) | **NOT READ: SKIPPED BOTH RUNS, `no hotbar slot holds ironblade`** (the staging defect: his hotbar was full). Ironblade's own arrival is read in `GATE-sword-convert.md` SCA4: given, and left as ironblade |

## THE MANUAL ROW AND BEN'S ROW

### STM1 — craft ONE iron sword at a crafting table (SURVIVAL; Ben's hands)

No selftest handle drives a crafting table (the verbs are READ: no craft verb exists), so this is Ben's:

| prediction | stands on | READING |
|---|---|---|
| **The line Ben types or does:** "At a crafting table, craft one iron sword (two iron ingots and a stick), and take it." He receives **"Iron Sword"** in the common colour, not a plain one. Read on the server by `/rpg enchant show` with it held (a minted sword has an enchant state; a plain one is refused) | mint-on-craft (`CraftResultIndex`, `iron_sword` claimed once) | **THE CRAFT IS UNREAD ON THE SERVER; HIS ANSWER IS THE WITNESS.** At 14:44 he gave himself a crafting table, 2 iron ingots and a stick, then typed `/rpg enchant show` (14:44:57). Neither the crafting mint nor the reply to his own command writes a log line. **INFERRED, not read:** no `ARRIVAL … material=iron_sword` came between then and SCA1, and a plain crafted sword would have produced one. His answer to question 1 (`GATE-sword-convert.md` SCB1): "yes" |

### STB1 — the crafted Iron Sword: question 1 (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| His answer, verbatim, to **"Is the crafted Iron Sword's name, colour and tooltip what you expect?"** No PASS or FAIL; "not sure" is NOT STATED | STM1 | His answer, verbatim, asked as `GATE-sword-convert.md` SCB1 (14:46:30): **"yes"** |

## WHAT THIS GATE DOES NOT READ

- **A contested or refused claim on a server:** shipped content has none, and R0c predicts no Refusing line. The refusals
  are unit tests.
- **The netherite sword's source:** the smithing table (`GATE-sword-smithing.md`) and conversion on arrival
  (`GATE-sword-convert.md`).
