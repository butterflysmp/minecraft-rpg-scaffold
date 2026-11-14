# PLAN — MINING: PHASE 0 (FACTS, THE INPUT QUESTION, SLICE 1'S SHAPE)

**Status: PLAN ONLY, NOT MERGEABLE YET.** Written 2026-11-12 against master **`55425aad`** (read from the wire:
`git ls-remote origin refs/heads/master` → `55425aadf907f8faa2f1c7707a4fa0f713a87a7e`). This branch,
`docs/mining-plan`, does not merge until the input spike's readings are folded into it and the seat has diffed it
(the seat's ruling, 2026-11-12).

**2026-11-13: the spike's readings are folded in** (§1's 2026-11-13 rulings, §2's 2026-11-13 rulings, §3's F7, §6's
findings F-M12 to F-M18), and §5 is now SLICE 1 as a buildable design. Still PLAN ONLY: it waits for the seat's diff.

**2026-11-14: SLICE 1 READ.** One boot of the stack top `feat/mining-s1b` at **`863bf10a`** (S1-a + S1-b, gate
`GATE-mining-slice1.md`); readings committed at **`9eb45815`**, Ben's follow-up words as a dated addendum at
**`a95e56f6`**. The seat read both from origin and accepted them, 2026-11-14. Findings F-M25 to F-M28 and the
**carried hand rows** the next mining gate must include are at the end of §6; §5.4 carries a dated correction.
> **APPROVED TO MERGE, 2026-11-14 (gate-less: the approval line lives here and in #184's description, per the seat's ruling of 2026-11-14):** the seat's merge approval of 2026-11-14, approved head `#184 9a398711`, on Ben's words *"go ahead and merge them"*.

**2026-11-14: S1-c READ.** `feat/mining-s1c` (#185) booted at **`0578366d`**; readings at **`0574e940`**, accepted by
the seat the same day (the sound rows count). Findings F-M32 to F-M36, and the CARRIED HAND ROWS list dated after
S1-c (which supersedes slice 1's), are in §6.

**2026-11-14: S2 OPENS** on `feat/mining-s2` from master **`6470ee8d`** (read from the wire:
`git ls-remote origin refs/heads/master` → `6470ee8dd62b8a5a1db9205c7a1a4df121e768a7`). Four vein sizes, each patch
rolling its size per regrowth, the DENIED sound and the miss-sound cooldown. Ben's words of 2026-11-14 are in §1, the
seat's rulings in §2. The design (§5.12) and the gate (`GATE-mining-s2.md`) land in later commits on this branch.

**2026-11-14: S2 READ.** `feat/mining-s2` (#186) booted at **`03bd1bf0`**, on the seat's approval and Ben's "ready";
readings in `GATE-mining-s2.md`. Findings F-M41 to F-M44, and the CARRIED HAND ROWS list dated after the S2 boot
(which supersedes the earlier ones), are in §6.

**2026-11-14: MINING JOINS `/rpg selftest`** on `feat/mining-selftest` from master **`01727a8c`** (read from the
wire: `git ls-remote origin refs/heads/master` → `01727a8c621ddd67c18b74c698ae9861c4d9496d`). The review seat's brief:
mining's server-side gate rows become selftest scenarios (`docs/gates/GATE-mining-auto.md`), the selftest's settle
fix (GATE-selftest.md ST9) lands first, and `/rpg mining` moves under the `rpg.dev` guard. The seat's ruling on the
guard and the carried rows this branch turns AUTO are in §2 and §6, dated 2026-11-14 (selftest).

**The spike** is `spike/mining-input`, also from `55425aad`. It will NOT merge. Its gate, `GATE-mining-spike.md`,
lives on that branch beside the code it tests, because the boot is bound to that branch's build sha. After its
readings, the read gate is copied byte for byte onto this branch with a dated note naming the spike sha it was read
at. The spike branch is not deleted until the seat has confirmed that the two blobs diff empty.

**THE GATE COPY, 2026-11-13.** `docs/gates/GATE-mining-spike.md` on this branch is a byte-for-byte copy of the same
path on `spike/mining-input` at **`d6879d868c48bc13ce987b3d954a5cfaff443ed4`**. The readings are from the boot of
`1dbcc156` (2026-11-12) and were committed at `11e94e2c`; `d6879d86` adds only the seat's dated line, *"Seat read
2026-11-13 from origin at 11e94e2c: readings accepted"*. The copy was made with
`git checkout spike/mining-input -- docs/gates/GATE-mining-spike.md`, so the index took the spike's blob as it
stands, **`fde186791f824b819e19cb38e727d6a98ac14341`** on both refs (`git rev-parse <ref>:docs/gates/GATE-mining-spike.md`).
This note lives here and not in the copied file, so the two blobs stay identical. `spike/mining-input` is kept until
the seat confirms the copy.

**The open stack is PARKED, not abandoned** (Ben, 2026-11-12): #172–#180 and #177. Nothing here touches those branches.

---

## 1. BEN'S RULINGS, 2026-11-12, VERBATIM

| topic | Ben's words |
|---|---|
| starting | *"I would like to work on mining now"* |
| a hit needs a charged swing | *"Could we require a full charged pick axe hit for affect?"* and *"Ideally I'd like the vanilla hit indicator for this and modify the attack speed attribute of custom pickaxes"* |
| target size | *"The visible weak spot will be 3x3 but the hit box for it can be 5x5"*. The units are 1/16 of a block. |
| art | *"We will build a resource pack with this"* |
| regrowth timer storage | *"Memory sounds better"* |
| needing a pickaxe | *"Yes, denied if not holding a pickaxe"* |
| ores | *"New custom ores as well as vanilla variants. Yes players can still dig them in the normal world"* |
| where patches live | *"I don't like the idea of \"Mines\". These ore patches will appear in the majority of Custom areas, specifically any non dungeon/raid area. They will not appear in the normal world"* |
| Mining XP | *"we'll give it it's own but based off of the player level curve. I was thinking the player level curve divided by 10"* |
| Unstable veins | *"Unstable veins should have 3-5 spots"* |
| what players see | *"the goal of mining is to have the patches and weak spots be represented by custom texture pack entities"* |
| breaking blocks in custom areas | *"No they cannot"* |

**Deferred by Ben:** Mining Fortune, mining level rewards, recipes, and whether top ores need combat gear.

### BEN'S RULINGS, 2026-11-13, VERBATIM (relayed by the seat after spike 1)

| topic | Ben's words |
|---|---|
| Adventure mode | *"No players will never be in adventure mode."* |
| requiring the resource pack | *"yes it will be required for the real server but it might be better to have it prompt or toggle for dev work"* |
| vanilla tooltip order (Attack Damage, then Attack Speed) | *"yes for now"* |
| stopping the client's dig (the crack and ghost while holding) | *"I feel like the Dig stop is pointless because block breaking is already blocked and the cracks are purely client side visuals. At best defer this to a later date as a polish"* |
| the first ore, custom or vanilla | *"let's keep it vanilla for now, we'll do iron"* |
| Small vein only, or all four sizes | *"keep it as the small vein for now"* |
| the first pickaxe's swing speed (vanilla is 1.2) | *"Vanilla speed for now is fine"* |
| regrowth time (the draft said about 1 minute for common ore) | *"1 minute is fine for now"* |
| what an iron patch hands the player (raw iron, iron ingots, or iron ore blocks) | *"raw iron"* |
| *"Small vein is 3 wide by 2 tall with 2-3 spots, as your doc's table says. Is that right?"* | *"yes"* |
| *"How much raw iron per spot? Your doc gives Small no completion bonus; keep that?"* | *"2-4 per spot with a 5 completion bonus"* |
| *"Is the gold window in slice 1, or later?"* | *"later"* |
| *"Should a miss make a sound in slice 1? Your doc mentions a dull clink."* | *"yes"* |

**The last four rows were relayed by the seat, 2026-11-13, with the questions quoted as asked.** What they set:
- **N7 and N8 are RULED**: the Small vein is width 3, height 2, with 2 to 3 spots.
- **N2 is RULED as a range**: 2 to 4 raw iron per spot. **N3 is RULED**: a completion bonus of 5 for Small.
- **N4, N5 and N6 (the gold window, and any perfect-chain pay) are DEFERRED by Ben.** Slice 1 builds no gold window
  and no gold art. Keeping each spot's reveal tick and hit tick in the patch state is still fine.
- **A miss plays a sound.**

**The four rows before the last are RULED "for now" values, not provisional** (the seat, 2026-11-13). The last row,
*"raw iron"*, reached the plan in the seat's diff of 2026-11-13. In slice 1's terms:
- **the ore is vanilla iron.** *(Written before the raw-iron ruling: "WHICH ITEM a patch pays (raw iron, an ingot or
  the ore block) is NOT ruled: it is on §5's Numbers for Ben".)* **Now RULED: a patch pays `minecraft:raw_iron`**, a
  plain vanilla item, not minted, delivered to the inventory. Whatever does not fit drops at the player's feet (§2);
- **the Small vein only.** Per Ben's design doc, as relayed by the seat, that is a patch 2 blocks high × 3 wide with
  2–3 weak spots. The schema describes a size generally, so the other three sizes are content later, not code;
- **attack speed 1.2**, the vanilla iron pickaxe's own default: a delay of `(float)(1.0 / 1.2 × 20.0)` = 16.666666
  ticks, first full read at ticker 17 (§5.7 TOOLS table, executed). Spike 1 did NOT isolate the 17: at 1.2 its full reads were all ≥ 20 ticks apart and its latest early read was 0.8100 (Δ=13); its early reads fit the formula (MS1);
- **regrowth 1 minute = 1200 ticks**, per player, per patch slot.

**What they settle:**
- **Adventure mode is OUT** as a mechanism for anything mining does. F2's ADVENTURE branch and F-M6 stay as facts,
  and are no longer a design route.
- **The pack is REQUIRED on the real server**: that is the target. In dev it is offered with a prompt or a toggle (§5,
  THE RESOURCE PACK). Requiring it is recorded, not built, in slice 1.
- **The minted pickaxe's tooltip keeps vanilla's order**: Attack Damage above Attack Speed (F-M15, §5 TOOLS).
- **The dig-stop is DEFERRED as polish.** Slice 1 does not build it. F7a is recorded as the candidate (§3, F7).

### BEN'S RULINGS, 2026-11-14, VERBATIM (relayed by the seat in the S2 brief)

After the S1-c boot:

| | topic | Ben's words |
|---|---|---|
| a | the S1-c gate rows he skipped | *"Everything after the break row was already run which is why i chose to skip them, no point running them again"* |
| b | asked if a placeholder sound is fine for the "need a pickaxe" message | *"Placeholder for now is fine"* |
| c | asked if the miss sound should play only on a charged miss | *"Miss sound should have a slight cooldown, let's say .4 seconds"* |

On vein sizes (the seat's four questions, his four answers):

| | topic | Ben's words |
|---|---|---|
| d | asked whether a placed patch keeps one size or rolls a size each regrowth | *"The end goal is for patches to grow of random sizes and ores and types"* |
| e | asked whether completion bonuses of 6, 8 and 10 for Medium, Large and Extra Large are right (Small is 5) | *"This is fine for now. Tuning will come later"* |
| f | asked for regrowth time by size | *"Regrowth can be 1 minute across the board"* |
| g | asked whether per-spot yield stays 2-4 for every size | *"yes"* |

**FROM BEN'S DESIGN DOC, RELAYED BY THE SEAT, 2026-11-14.** These are the doc's values, NOT Ben's words in chat.
Each is marked *design doc, relayed* wherever it is used.

| size | width × height (blocks) | spots | roll weight |
|---|---|---|---|
| small | 3 × 2 | 2–3 | 50 |
| medium | 5 × 3 | 4–5 | 30 |
| large | 7 × 3 | 6–7 | 15 |
| extra_large | 7 × 4 | 8–9 | 5 |

The roll weights are **PROVISIONAL** (the seat's words). Small's 3 × 2 and 2–3 were already RULED by Ben on 2026-11-13
(the table above).

## 2. THE SEAT'S RULINGS

**From the brief (2026-11-12):**
- Per-player visuals are packet-only entities, following `PacketDamagePopupSender`. Every packet callback obeys
  CLAUDE.md's THREADING section; `PacketListenerBase.bukkit` is the only route to Bukkit.
- Regrowth timers live in memory, keyed by player UUID. They are KEPT when the player logs out and LOST on server
  restart. Nothing goes into `PlayerProfile` in slice 1.
- The block a patch sits on cannot be broken or cracked by that patch's player.
- Minigame state and rules go in `core/` (no Bukkit). Content goes in YAML under `content/`.
- OPEN, to be settled by the spike: how a hit is detected. **A**: the server ray-casts the look vector against the
  spot's 5×5 box on the face. **B**: a packet-only INTERACTION entity, sized to the box, reports an attack.
  - **SETTLED 2026-11-13: A** (the seat's ruling 2, below).

**From the planning round (2026-11-12):**
- **The spike's gate is `.requires(Permissions.DEV)` and nothing else.** There is no `-D` property, no change to
  `scripts/dev-server.sh`, and nothing is copied from feat/selftest. The brief named `-Drpg.dev=true`, but that
  property exists only on the parked feat/selftest branch (`SelfTestGuard`).
  - **Mining's real dev commands move to the `rpg.dev` guard once #175 has merged.**
  - **THE SEAT'S RULING, 2026-11-14 (the selftest brief; #175 merged as `472a358e`):** the whole `/rpg mining`
    subtree requires BOTH `Permissions.DEV` AND the dev-server marker (`-Drpg.dev=true`, set only by
    `scripts/dev-server.sh`), reusing `SelfTestGuard.DEV_PROPERTY`, the one definition of that property. It does NOT
    take the selftest's other conditions (dev world, one player, SURVIVAL): those belong to a run, not to a dev
    command. Built on `feat/mining-selftest`.
- Where the docs live: see the status block above.

**From Ben's addendum, the AREA RULE is the target:** custom areas are no-break as a whole, so cancelling block
damage is a property of the AREA, not of a patch.
- **What is missing:** no custom area exists in the repo. Slice 1 needs (a) an area model, a core type that answers
  "is this block position in a custom area", with its source of truth still to be ruled, and (b) ONE listener
  consulting it for `BlockDamageEvent` and `BlockBreakEvent`.
- Until then, the spike cancels damage and break on the spiked block only.
- **2026-11-13: slice 1 builds NO areas** (Ben's scope, relayed by the seat). It cancels `BlockDamageEvent` and
  `BlockBreakEvent` on the blocks under a placed patch slot (§5). The area rule stays the target, unbuilt.

**From the seat, 2026-11-13, after reading the spike's readings from origin at `11e94e2c`:**
1. **THE DIG-STOP IS DEFERRED** (Ben, §1). Slice 1 does not build it. F7a (`block_break_speed` at 0, per player, per
   area) is recorded as the candidate for that later polish, **UNREAD on the client** (§3, F7). The server-side
   cancel of `BlockDamageEvent` and `BlockBreakEvent` on a patch's block stays in slice 1.
2. **HIT DETECTION IS CANDIDATE A, unconditionally:** a left-click on the patch's block, with the server's own ray
   against the spot's 5×5 box on the face. Spike 1 read that the charge is readable before the reset, that
   single-click aim landed in the box, and that a held click never reads charged. **Candidate B (the INTERACTION box)
   is NOT built.** Its two unread questions, the hold repeat (MS3(b)) and the early click (MS2(b)), are recorded as
   UNREAD, not answered.
3. **A reads the charge at `BLOCK_DAMAGE` or at the FIRST `ARM_SWING` of a click**, never at the second swing of a
   click and never at the packet hop. §5 says which one slice 1 uses and why, and how the second swing of the same
   click is ignored.

**From the seat's diff of the slice 1 plan, 2026-11-13 (plan read at `1abaf4a0`; DESIGN APPROVED):**
1. **Read site: `BlockDamageEvent`**, approved for §5.4's four reasons. Slice 1 has no arm-swing handler and no packet
   listener.
2. **Charged means `getAttackCooldown() == 1.0f` exactly**, approved, as ONE named constant in core with its test,
   so that a later grace window is a one-line, visible change. M-CHARGE-17 reads it.
3. **A Miss and an Uncharged hit change nothing in slice 1**, approved. (The swing itself spends the charge.)
4. **Slots, part-mined patches and regrowth clocks live in memory and are lost on restart.** No dev file. Approved.
5. **S1-T is deferred to the first pickaxe with a non-vanilla speed** (the `attack_speed` key, the ordered mint, the
   iron.yml rewrite). Approved. iron.yml is not touched in slice 1.
6. **The DERIVED mechanism values are the seat's to rule, and are approved** (they came off Ben's list):
   - **the edge margin: 2 px** (the smallest that keeps a 5 × 5 box on the patch);
   - **the spot spacing: 5 px**, Chebyshev, between centres (the smallest at which no two 5 × 5 boxes overlap);
   - **the display offsets off the wall: 0.0005 blocks for the patch and 0.001 for a spot.** M-LOOK reads how they
     look.
7. **The pickaxe test is vanilla's `#minecraft:pickaxes` tag, minted or not**, until Ben says otherwise. **The dev
   pack gets both** the join toggle (default false) and the command. **The break's sound and particle are
   placeholders, named as placeholders in content**, not hard-coded.
8. **The shared entity-id allocator** is approved as the smallest move that gives the popups and mining one counter,
   with the popup tests passing unchanged.
9. **The slice order: S1-0, then S1-a, then S1-b on S1-a merged.** Approved.
- **A full inventory** (the seat's ruling): the payout goes to the inventory, and whatever does not fit is dropped at
  the player's feet, as vanilla does. The trace line says how much was dropped (§5.2).
- **The gate copy is confirmed.** `spike/mining-input` is kept anyway until S1-b has merged, because slice 1
  re-implements from it.
- **Until Ben's remaining numbers arrive:** nothing that depends on a gold window is built, and `payout.per_spot`
  and `payout.completion_bonus` are left UNSET in `iron.yml`. The loader refuses an ore without them, with a test,
  so no value is picked silently. **Consequence:** until they land, the shipped `iron.yml` is refused at boot, so
  the build loads 0 ores and the zero-check warning fires. That is why S1-a's PR is not marked ready.
  *(LIFTED the same day by the seat's ruling 5 below: `iron.yml` carries the ruled values. The loader's refusal of
  an ore with no payout stays, with its test.)*

**From the seat, 2026-11-13, after Ben's remaining numbers (§1):**
1. **`payout.per_spot` is a RANGE**, `{ min: 2, max: 4 }`, whole items, `0 ≤ min ≤ max`. The amount is rolled
   uniformly and **independently for each spot, at the break**, from the injected `RandomGenerator`. The trace's
   break line shows each spot's roll and the total.
2. **The completion bonus is per VEIN SIZE**, authored in the ore file as a map keyed by size id:
   `completion_bonus: { small: 5 }`. The loader refuses an ore that lists a size in `sizes:` with no
   `completion_bonus` entry for it, and refuses an entry for a size it does not list. The reason: Ben's design has
   the bonus growing with size, and it is an ore's economy number.
3. **Payout = the sum of the per-spot rolls + the size's completion bonus.** For Small iron that is **9 to 17 raw
   iron per break** (§5.2, executed), with both ends pinned by a seeded test. NAME THE QUANTITY still applies:
   whole item counts, flat, no percentages.
4. **Sounds are content, not code.** The content names a sound key for MISS and one for BREAK, each a placeholder
   vanilla sound marked PLACEHOLDER. UNCHARGED and NOT_LIVE play nothing in slice 1. DENIED keeps its action-bar
   line. The sound plays to that one player only. Playing it is S1-b's job; S1-a carries only the schema, the loader
   and the validator's check that the key resolves. (Where the keys live is argued in §5.3: the ore file.)
5. **The instruction to leave the payout counts UNSET is lifted.** `iron.yml` carries the ruled values. The loader
   still refuses an ore with no payout, and keeps its test.

**From the seat, 2026-11-14, on Ben's (a) (the S1-c rows he skipped):** these S1-c carried rows are **CLOSED BY
BEN'S RULING 2026-11-14, NOT READ, never passes**: the place refusal texts, the server-list check, the
full-inventory ground check, the relog and logout sights, the post-remove dig. **STILL CARRIED, never read by
anything:** a second player sees their own patch; a same-world respawn re-shows the patch; whether a death drops a
server-offered pack.

**From the seat, 2026-11-14: the mechanism rulings for S2 (four vein sizes rolled per regrowth, and two sound
rulings). Code + gate, no boot.**
1. **A SLOT IS A LOCATION WITH A MAXIMUM SIZE.** `/rpg mining place <ore> <size>` now means "the largest size this
   location can hold". The slot's footprint is that maximum size's footprint, from the same bottom-centre anchor rule
   as today. Every block of it is protected always.
2. **EACH PATCH ROLLS ITS SIZE WHEN IT IS CREATED** (first show, and every regrowth), per player, from the ore's size
   weights, restricted to sizes that fit the slot (width and height both at most the maximum's), weights renormalised
   over those. The patch sits at the slot's anchor by the same rule, so it is centred on the slot and its bottom row
   is the slot's bottom row.
3. The weights are the ore's: in iron.yml, `sizes` becomes a map of size id to an integer weight greater than 0.
   NAME THE QUANTITY: a relative weight, an integer; a size's chance is its weight over the sum of the eligible
   weights. The roll uses the injected RandomGenerator.
4. A click on a slot block OUTSIDE the current patch's footprint (or its face while the patch is regrowing) is
   cancelled and judged as nothing: no click outcome, no sound. The trace may log it as its own line; pin the format
   if so.
5. Payout: per_spot stays 2..4 for every size; completion_bonus is small 5, medium 6, large 8, extra_large 10;
   regrowth_ticks stays 1200. Put the executed payout range for each size in the plan and pin both ends of each with
   seeded tests.
6. THE PATCH ART: one flat model per size, at that size's aspect, named per size in the ore's models (placeholder
   textures; the look is Ben's later). The spot and crack models are unchanged. The pack's sha1 will change;
   MiningPack's file list and its tests follow.
7. A DEV COMMAND TO FORCE A SIZE, so a gate does not depend on luck: `/rpg mining roll <size>` (Permissions.DEV,
   wired and pinned like the others) replaces the caller's patch at the looked-at slot with a fresh patch of that
   size, refusing a size that does not fit the slot or that the ore does not list. It logs always.
8. THE DENIED SOUND: a placeholder vanilla sound, named in content and marked PLACEHOLDER, played to the one player
   when a DENIED action-bar hold STARTS with no hold already live. A DENIED click that replaces a live hold plays
   nothing.
9. THE MISS SOUND COOLDOWN: 8 server ticks (Ben's 0.4 s), per player, counted from the last miss sound that actually
   PLAYED. A MISS inside it is still a MISS, still logged, and plays nothing. One named value, the rule in core, tests
   first. The click line gains a field that says whether the miss sound played, so a gate can read it without an ear.
10. Ore and minigame style are NOT rolled in S2 (one ore, Trail only). Say in the plan how the slot and the patch roll
    would take them later, so nothing here has to be undone; build none of it.
11. Core first, tests first, no Bukkit in core, no packet listener, everything on the main thread as slice 1
    documents. Mutation-check the guards that matter: the fit rule, the renormalised roll, the outside-the-patch rule,
    the cooldown boundary at exactly 8 ticks, the denied-sound "no live hold" condition, each size's payout ends.

**THE DESIGN.** Every ore patch and every weak spot is a **packet-only display entity showing a CUSTOM MODEL from the
project's resource pack**, shown to one player. **A vanilla item is never the shipped look.** The spike's
vanilla-item spot exists only so that the input readings (MS1–MS6) do not depend on the pack. This makes F5 (how a
custom model is selected) and F6 (how the pack reaches the player) load-bearing, not background.

---

## 3. FACTS FROM THE PINNED JARS

**The instruments.**
- `javap` is `C:\Users\Neb91\.jdks\openjdk-26.0.1\bin\javap.exe`, run as `javap -c -p -cp <jar or extract> <class>`.
- The server jar is `run/versions/26.1.2/paper-26.1.2.jar`. Its MANIFEST reads `Build-Number: 74` and
  `Implementation-Version: 26.1.2-74-e4e17fc`, the same build as `pom.xml`'s `paper.version`
  (`26.1.2.build.74-stable`). It is mojang-mapped.
- The API is `paper-api-26.1.2.build.74-stable-sources.jar`, read with `unzip -p`.
- PacketEvents is `packetevents-api-2.13.0-sources.jar`, plus `run/plugins/packetevents-spigot-2.13.0.jar`.
- Bytecode offsets below are from that server jar.

### F1. The attack-strength ticker: who resets it, and what reads it
- **Every caller.** I grepped the extracted jar for `resetAttackStrengthTicker` (`grep -rl --include=*.class`).
  Exactly three classes call it:
  - `ServerPlayer#swing(InteractionHand)` (the one-argument overload): `invokespecial Player.swing` then
    `invokevirtual resetAttackStrengthTicker`;
  - `Player#tick`, only when the main-hand item TYPE changes (`ItemStack.isSameItem` false);
  - `Player#detectEquipmentUpdates`, behind `GlobalConfiguration.unsupportedSettings.updateEquipmentOnPlayerActions`.
  - (`CraftPlayer#resetCooldown` is the API route to the same method.)
- **Does an ARM SWING reset it? YES.** `ServerGamePacketListenerImpl#handleAnimate` calls
  `ServerPlayer.swing(InteractionHand)` at bytecode **340**. **`PlayerArmSwingEvent` is constructed at 289 and
  tested for `isCancelled` at 325, BEFORE that reset.** A cancelled event returns at 331 with no reset.
- **Does a BLOCK-DIG START reset it? NO.** `handlePlayerAction` and `ServerPlayerGameMode#handleBlockBreakAction`
  do not reference the reset method.
- **Does an ENTITY ATTACK reset it? NO.** `handleAttack` checks `cannotAttackWithItem(stack, 5)` and calls
  `ServerPlayer.attack(entity)`. `Player#attack` only READS the scale (`ldc 0.5f; getAttackStrengthScale`). Any reset
  around an attack is the swing packet's.
- The two-argument `swing(hand, boolean)` (from use-item and interact) is NOT overridden in `ServerPlayer`, so it does
  not reset.
- **The API.** `HumanEntity#getAttackCooldown()`. Its javadoc: *"1.0 representing a fully charged attack and 0.0
  representing a non-charged attack … A float between 0.0-1.0"*. `CraftHumanEntity#getAttackCooldown` is
  `getHandle(); ldc 0.5f; Player.getAttackStrengthScale(F)`.
- **Mid-charge.** `getAttackStrengthScale(p)` = `Mth.clamp((attackStrengthTicker + p) / delay, 0, 1)`, and
  `getCurrentItemAttackStrengthDelay()` = `(float)(1.0 / ATTACK_SPEED * 20.0)`. So the value is
  `(ticks since reset + 0.5) / delay`, rising linearly and clamped at 1.0. The vanilla base ATTACK_SPEED is
  **4.0** (`Attributes`: `ldc "attack_speed"`, `4.0d`), giving a delay of 5 ticks.
- **All three handlers run on the main thread in packet arrival order.** `handleAttack`, `handleAnimate` and
  `handlePlayerAction` each open with `PacketUtils.ensureRunningOnSameThread` (bytecode 9).

### F2. A SURVIVAL left-click on a block whose `BlockDamageEvent` is cancelled
- **Route.** `handlePlayerAction` → `ServerPlayerGameMode#handleBlockBreakAction(pos, action, dir, maxY, seq)`, then
  `ackBlockChangesUpTo(seq)`. The ack is only recorded there; the `ClientboundBlockChangedAckPacket` is sent from
  `ServerGamePacketListenerImpl#tick`.
- **START in SURVIVAL, in order:**
  1. the range and height checks;
  2. spawn protection;
  3. `mayInteract`;
  4. `PlayerInteractEvent` LEFT_CLICK_BLOCK;
  5. the creative branch;
  6. `blockActionRestricted`;
  7. `BlockState.attack` and `EnchantmentHelper.onHitBlock`;
  8. `CraftEventFactory.callBlockDamageEvent`;
  9. then `531: ifeq 535; 534: return`.
- **Cancelled, the server sends NOTHING:** no block update and no destroy progress. `isDestroyingBlock` and
  `destroyPos` are not set (they are set at 624–634, after the return). The only reply is the sequence ack on the
  next connection tick. `BlockState.attack` has already run, which is why the test block must have no attack
  behaviour (§5, and the gate's block choice).
- **ADVENTURE:** `Player#blockActionRestricted` is true for ADVENTURE without a `can_break` tool. The dig sends a
  `ClientboundBlockUpdatePacket` and returns at step 6. **It never reaches `BlockDamageEvent`.**
  `handleAnimate` also fires LEFT_CLICK_BLOCK from the swing in ADVENTURE (158–211). **So the AREA rule, enforced
  by events, assumes SURVIVAL, and ADVENTURE would be a different mechanism.** That is a finding (§6).

### F2b. After that cancelled START: STOP, and an instant break
- **STOP_DESTROY_BLOCK for the same block** (`handleBlockBreakAction`, 683–836):
  - It tests `pos.equals(destroyPos)` (697). `destroyPos` starts as `BlockPos.ZERO` (constructor, 12–15), and a
    cancelled START never set it.
  - **When they differ, the branch jumps to 825: a debug log "stopped destroying", and NOTHING breaks and NOTHING
    is sent** apart from the ack.
  - **When they are EQUAL**, because an earlier UNCANCELLED dig on the same block left `destroyPos` there, the
    server computes progress from `destroyProgressStart`. At progress ≥ 0.7 it calls `destroyAndAck(..., "destroyed")`;
    otherwise it may arm `hasDelayedDestroy`, which `tick()` can complete later.
  - Both routes go through `destroyBlock`, which fires `BlockBreakEvent`.
- **An instant break:**
  - A CREATIVE START runs `destroyAndAck(pos, seq, …)` at bytecode **256, before `callBlockDamageEvent`** (531). So
    `BlockDamageEvent` never sees a creative break.
  - A survival insta-mine (`getInstaBreak()` or progress ≥ 1) runs `destroyAndAck(..., "insta mine")` at 568, after
    an UNCANCELLED BlockDamageEvent.
- **`destroyBlock` fires `BlockBreakEvent`.** If it is cancelled (`callEvent` at 166, `ifne 226` not taken), it
  returns `false` (177/224). `destroyAndAck` then sends `ClientboundBlockUpdatePacket(pos, level.getBlockState(pos))`
  (19–42): **a resync to the true block.**
- **Consequence:** cancelling `BlockDamageEvent` alone does not cover every break route. The spike ALSO cancels
  `BlockBreakEvent` on the spiked block and logs it as a `MINESPIKE … src=EVENT type=BLOCK_BREAK` line, so a break
  attempt is witnessed rather than silent. Slice 1's area listener must cancel both.

### F3. The INTERACTION entity, and what a left-click on one sends
- **Fields** (`static {}` defineId order): `Interaction extends Entity` directly. `Entity` takes indices 0–7:
  - 0 shared flags (BYTE), 1 air (INT), 2 custom name (OPTIONAL_COMPONENT), 3 name visible (BOOLEAN),
    4 silent (BOOLEAN), 5 no gravity (BOOLEAN), 6 pose (POSE), 7 ticks frozen (INT);
  - then **8 `DATA_WIDTH_ID` FLOAT, 9 `DATA_HEIGHT_ID` FLOAT, 10 `DATA_RESPONSE_ID` BOOLEAN**.
- PacketEvents 2.13.0 has `EntityTypes.INTERACTION = define("interaction", DISPLAY)`.
- **On 26.1 a left-click attack is its own packet, `ServerboundAttackPacket(int entityId)`.**
  `ServerboundInteractPacket` now carries only `entityId, hand, location, usingSecondaryAction`, with no action enum.
  - In PacketEvents this is **`PacketType.Play.Client.ATTACK` / `WrapperPlayClientAttack#getEntityId`.**
    `WrapperPlayClientInteractEntity` on `V_26_1` forces `action = INTERACT_AT // compat`.
  - **`ExampleTelegraphListener`'s INTERACT_ENTITY filter does not see attacks on this version.**
- **An entity id the server does not know.** `handleAttack` resolves `getEntityOrPart(id)`. If that is null it calls
  `CraftEventFactory.callPlayerUseUnknownEntityEvent(player, id, true /*attack*/, MAIN_HAND, null)` at **289**.
  **No kick.** (What does kick: attacking yourself, or an item, an XP orb or an arrow.)
  - `com.destroystokyo.paper.event.player.PlayerUseUnknownEntityEvent` has `getEntityId()`, `isAttack()` and
    `getHand()`.
- **PacketEvents sees the packet first.** `packetevents-spigot` `ServerConnectionInitializer` does
  `ctx.pipeline().addBefore(decoderName, PacketEvents.DECODER_NAME, decoder)`, with `decoderName` being `"decoder"`
  or `"inbound_config"`. `PacketEventsDecoder extends MessageToMessageDecoder<ByteBuf>`, so it runs before vanilla
  decoding and before `packet_handler`.

### F4. An attack-speed modifier on an item
- **API:** `Attribute.ATTACK_SPEED`. `Attribute` is now an interface (`extends OldEnum<Attribute>, Keyed`), and there
  is no `GENERIC_ATTACK_SPEED`.
  - The constructor is `AttributeModifier(NamespacedKey key, double amount, Operation, EquipmentSlotGroup)`. The UUID
    and String constructors are `@Deprecated(forRemoval = true)`.
  - The call is `ItemMeta#addAttributeModifier`. The data-component route is `DataComponentTypes.ATTRIBUTE_MODIFIERS`
    / `ItemAttributeModifiers`.
- **Adding ANY modifier through ItemMeta REPLACES the type's defaults.**
  - `CraftMetaItem.applyModifiers` builds from `ItemAttributeModifiers.builder()` (empty) and never loads the type
    defaults.
  - On a fresh pickaxe `getAttributeModifiers()` is null. One `addAttributeModifier` call writes a component holding
    only that modifier, **and the pickaxe's default attack damage and speed are gone.**
  - The `setAttributeModifiers` javadoc says it: *"an empty map … will override the default modifiers"*.
  - **The remedy:** copy `getType().asItemType().getDefaultAttributeModifiers()` (or `ItemType#…`) into the meta
    first, then replace the speed entry.
- **Tooltip:**
  - `ItemAttributeModifiers$Display$Default#apply` prints `attribute.modifier.equals.*` (DARK_GREEN, base value
    added) only when the modifier id is `Item.BASE_ATTACK_DAMAGE_ID` / `BASE_ATTACK_SPEED_ID`, which are
    `minecraft:base_attack_damage` / `minecraft:base_attack_speed`. Any other key prints `plus` or `take` as "+N".
  - It is preceded by a blank line and an `item.modifiers.<slot>` header, with the number format `#.##`.
  - **So keying the speed modifier `minecraft:base_attack_speed` keeps the vanilla-looking "N Attack Speed" line.**
  - `ItemFlag.HIDE_ATTRIBUTES` maps to hiding `ATTRIBUTE_MODIFIERS` in `TOOLTIP_DISPLAY`.
- **`ToolItems.mint` today pins no modifier** (its javadoc: *"Nothing in this project ever calls
  `setAttributeModifiers`"*). What changes: see §5's TOOLS.

### F5. ITEM_DISPLAY, and how a custom model is chosen
- **Metadata.** `Display extends Entity` directly. `static {}` order:

  | idx | field | type |
  |---|---|---|
  | 8 | interpolation start delta (delay) | INT |
  | 9 | transformation interpolation duration | INT |
  | 10 | pos/rot interpolation duration (teleport) | INT |
  | **11** | **translation** | VECTOR3 |
  | **12** | **scale** | VECTOR3 |
  | **13** | **left rotation** | QUATERNION |
  | **14** | **right rotation** | QUATERNION |
  | 15 | billboard (FIXED=0) | BYTE |
  | **16** | **brightness override** (`block<<4 \| sky<<20`, −1 = none) | INT |
  | 17 | view range (default 1.0) | FLOAT |
  | 18, 19 | shadow radius / strength | FLOAT |
  | 20, 21 | width / height (culling) | FLOAT |
  | **22** | **glow colour override** | INT |
  | **23** | **ItemDisplay item stack** | ITEM_STACK |
  | **24** | **ItemDisplay display type** (`ItemDisplayContext`: FIXED = 8) | BYTE |

  - The indices are consistent with `PacketDamagePopupSender`'s TEXT_DISPLAY use of 15 for the billboard and 23 for
    the first subclass field.
  - Glow is the entity shared-flags bit `FLAG_GLOWING = 6` (0x40) in index 0.
  - PacketEvents has `EntityTypes.ITEM_DISPLAY`, `EntityDataTypes.VECTOR3F`, `QUATERNION` (`Quaternion4f`) and
    `ITEMSTACK`. `ServerVersion.V_26_1_2(775)` matches `version.json`'s protocol 775.
- **1/16 precision** is a position or translation in doubles and floats. Nothing in the server quantises it. Whether
  the client renders it exactly is client behaviour (gate MS7).
- **Selecting a custom model: the `minecraft:item_model` data component.**
  - `ItemMeta#setItemModel(@Nullable NamespacedKey)`, javadoc *"Sets the custom item model"*, or
    `DataComponentTypes.ITEM_MODEL` (type `Key`).
  - Every item carries an ITEM_MODEL by default (`Item$Properties#finalizeInitializer`), so setting it replaces the
    look outright.
  - `CUSTOM_MODEL_DATA` is now four lists (floats, flags, strings, colours) that a model definition can switch on.
    It is not needed for one model per id.
- **The packet route:** `SpigotConversionUtil.fromBukkitItemStack(org.bukkit.inventory.ItemStack)` exists in
  `packetevents-spigot-2.13.0.jar` (`javap -p`). It carries a Bukkit stack, with its item_model, into index 23.
- **CLIENT-SIDE, unanswerable from the server jar:** that `item_model rpg:<id>` resolves to
  `assets/rpg/items/<id>.json` and thence to a model and texture. The server jar's `assets/` holds only `lang/`.

### F6. Offering a resource pack
- **API:**
  - `Audience#sendResourcePacks(ResourcePackRequest)` (Adventure 4.26.1) with
    `ResourcePackInfo.resourcePackInfo(UUID id, URI uri, String sha1hex)`, plus `.required(bool)` and `.prompt(…)`.
  - The Paper overload `Player#addResourcePack(UUID, String url, byte[] sha1, String prompt, boolean force)` also
    exists. `setResourcePack(…)` is `@Deprecated`.
  - Status arrives as `PlayerResourcePackStatusEvent` (SUCCESSFULLY_LOADED, DECLINED, FAILED_DOWNLOAD, ACCEPTED,
    DOWNLOADED, INVALID_URL, FAILED_RELOAD, DISCARDED), fired from `handleResourcePackResponse`.
- **server.properties:** `DedicatedServerProperties` reads `resource-pack`, `resource-pack-sha1`,
  `resource-pack-id` (defaulting to `UUID.nameUUIDFromBytes(url)`), `resource-pack-prompt` and
  `require-resource-pack`. On this seat all are empty or false (`run/server.properties` L55–59).
- **On decline:** `ServerCommonPacketListenerImpl#handleResourcePackResponse` disconnects **only when
  `action == DECLINED && server.isResourcePackRequired()`**, logging "Disconnecting {} due to resource pack {}
  rejection". **FAILED_DOWNLOAD and the rest never kick server-side.** A per-request `required` flag is enforced by
  the CLIENT (Adventure javadoc: *"Vanilla clients will disconnect themselves if their player rejects a required
  pack"*).
- **The server serves nothing.** The client downloads the pack over HTTP(S) from the URL.
- **Pack format: 84.0.** `version.json` has `"resource_major": 84, "resource_minor": 0`, and `SharedConstants` has
  `RESOURCE_PACK_FORMAT_MAJOR = 84`. `pack.mcmeta` uses **`min_format` / `max_format`**. `PackFormat.fullCodec` is
  `ExtraCodecs.compactListCodec(NON_NEGATIVE_INT, NON_NEGATIVE_INT.listOf(1, 2))`, so a bare `84` and `[84, 0]` are
  both accepted.

### THE OVERRIDE READING: does `AttackSpeedAttributeOverride` defeat a pickaxe's item modifier? NO
Read on `55425aad`, from `paper/.../health/AttackSpeedAttributeOverride.java` and its one caller,
`PlayerHealthSystem` L299–300:
```
AttackSpeedAttributeOverride.apply(player, keys,
        WeaponAttackItems.heldMeleeSpeed(player, keys, weapons), stats.attackSpeedValue(id));
```
- `WeaponAttackItems.heldMeleeSpeed` returns `WeaponItems.heldWeaponId(...)` → `weapons::find` →
  `meleeCadence` → **`.orElse(0.0)`**. An iron pickaxe carries no weapon id, so the result is 0.0.
- `AttackSpeedAttribute.desiredSpeed`: `if (weaponBaseSpeed <= 0) return 0.0;`.
- `apply` L57–61: `if (desiredSpeed <= 0) { if (existing != null) attribute.removeModifier(keys.attackSpeedOverride); return; }`.
- **It only ever adds or removes ITS OWN key** (`keys.attackSpeedOverride`). It is the only writer of
  `Attribute.ATTACK_SPEED` in `paper/src/main` (`git grep -n "Attribute.ATTACK_SPEED" 55425aad -- 'paper/src/main/*.java'`
  → that file only).
- **So holding the test pickaxe, the player's ATTACK_SPEED is `4.0 + (the item's modifier)`, untouched by the
  override.** The override is removed within one 5-tick reconcile of switching away from a melee weapon. The switch
  itself resets the ticker (F1, `Player#tick`), so MS5 starts each trial after a fresh switch.

**Client behaviour the server jar CANNOT answer, so each becomes a gate row, not a fact:**
- whether the crosshair attack indicator resets and refills on a block click, on a cancelled dig, and on an attack
  against a packet-only Interaction;
- whether the client sends a SWING on a block click whose dig was cancelled, and in what order it sends ATTACK and
  SWING;
- the packet cadence while left-click is held on a block;
- whether the client hit-tests a packet-only Interaction box and sends ATTACK for it;
- whether a cancelled dig shows a crack, a flicker or a ghost break;
- item_model resolution, and the model's size and placement on screen;
- the pack prompt, and what a declined pack looks like.

*(Spike 1 read every one of those on 2026-11-12. The answers are the gate's readings, `GATE-mining-spike.md`, and
the ones that change the design are §6's F-M12 to F-M18.)*

### F7. Stopping the client's dig: what the server controls per player (read 2026-11-12, NO BOOT; accepted 2026-11-13)
**Status: DEFERRED as polish (Ben, §1). F7a is the candidate. Nothing here is built in slice 1.**

The instruments are the same jars as above. `javap -c -p` was run on the mojang-mapped classes
`net.minecraft.world.entity.player.Player`, `net.minecraft.world.entity.ai.attributes.Attributes`,
`...attributes.AttributeMap`, `net.minecraft.world.level.block.state.BlockBehaviour`,
`net.minecraft.server.level.ServerPlayerGameMode`, `net.minecraft.world.item.Item` and
`net.minecraft.world.item.component.Tool`. `javap -p` was run on the pinned `paper-api` for the Bukkit names.

**How dig progress is computed.**
- Each tick, progress grows by `BlockBehaviour#getDestroyProgress`, which is
  `player.getDestroySpeed(state) / hardness / (hasCorrectToolForDrops ? 30 : 100)`. It is 0 when hardness is −1.
- **`Player#getDestroySpeed(BlockState)`**, in bytecode order:
  1. `ItemStack.getDestroySpeed` (4–8);
  2. plus `MINING_EFFICIENCY`, only when that speed is above 1 (14–27);
  3. × `(1 + (amplifier + 1) × 0.2)` when `MobEffectUtil.hasDigSpeed` (Haste or Conduit Power, 30–50);
  4. × the Mining Fatigue factor (53–124);
  5. × **`BLOCK_BREAK_SPEED`** (128–135);
  6. × `SUBMERGED_MINING_SPEED` when the eyes are in water (138–155);
  7. ÷ 5 when not on the ground.
- `ServerPlayer` does not override it. The client shares `Player`, so it is expected (not read) to compute the same.

**F7a: the `block_break_speed` attribute, THE CANDIDATE.**
- `Attributes` `<clinit>` 126–149: `RangedAttribute("attribute.name.block_break_speed", 1.0, 0.0, 1024.0)`, then
  **`setSyncable(true)`** (142–143). `AttributeMap` puts every `isClientSyncable()` instance into
  `attributesToSync`, so a change reaches the client.
- The Bukkit name is **`Attribute.BLOCK_BREAK_SPEED`** (`javap -p org.bukkit.attribute.Attribute` on the pinned API).
- **At 0, progress is exactly 0**, because the attribute multiplies the whole speed. Then, in
  `ServerPlayerGameMode#handleBlockBreakAction` (when the START is not cancelled):
  - **START:** an insta-mine needs progress ≥ 1 (554 `fload`, 556 `fconst_1`, 557 `fcmpl`, then `destroyAndAck`
    at 568). 0 is not.
  - **STOP:** `progress × (ticks + 1) ≥ 0.7` (743 `getDestroyProgress`, 756 `ldc 0.7f`, 759 `fcmpl`), then
    `destroyAndAck` at 791. 0 is not, so the server arms `hasDelayedDestroy` instead.
  - **`tick()`:** the delayed destroy completes only when `incrementDestroyProgress ≥ 1` (60 `invokevirtual`,
    65 `fconst_1`, 66 `fcmpl`). That never happens.
- **The design it implies (deferred):** a modifier on the player's `BLOCK_BREAK_SPEED`, added on entering an area and
  removed on leaving. It is per player and per area. It is not per item, so vanilla digging in the normal world is
  untouched.

**F7b: Mining Fatigue never reaches zero.** `Player#getDestroySpeed` switches on the amplifier (100–118): 0 → ×0.3,
1 → ×0.09, 2 → ×0.0027, 3 and above → ×8.1E-4. No level stops progress outright. The Bukkit name is
`PotionEffectType.MINING_FATIGUE`. How the effect reaches the client was not read.

**F7c: the held item's `minecraft:tool` component.**
- `Item#getDestroySpeed` reads `DataComponents.TOOL` and returns `Tool#getMiningSpeed(state)`: the first matching
  rule's speed, else `default_mining_speed`. With no tool component it returns 1.0.
- `Tool(List<Rule> rules, float defaultMiningSpeed, int damagePerBlock, boolean canDestroyBlocksInCreative)`.
  `default_mining_speed` is `Codec.FLOAT`, **so 0 is accepted**. A rule's `speed` is `ExtraCodecs.POSITIVE_FLOAT`,
  so a rule cannot be 0. The stream codecs are plain `FLOAT`.
- The Bukkit route is `ItemMeta#getTool` / `setTool`, then `ToolComponent#setDefaultMiningSpeed(float)`.
- **The cost:** it lives on the ITEM, so it applies wherever the item goes, including the normal world, where Ben
  ruled vanilla ores stay diggable. It only works with area-only items, or by swapping items at area borders.

**F7d: everything else the server controls per player.** Haste and Conduit Power, `SUBMERGED_MINING_SPEED`,
`MINING_EFFICIENCY` (only above speed 1), the on-ground ÷5, the correct-tool divisor (30 or 100), and Paper's
`BlockDamageEvent#setInstaBreak`, which only forces progress up. **Only F7a at 0, and F7c's `default_mining_speed`
at 0, reach exactly zero.**

**Client-only, UNREAD, so these become gate rows if the polish is ever built:**
- with `BLOCK_BREAK_SPEED` at 0, does the client still draw the crack, predict a break, flicker or ghost the block?
- does it still send a swing and a `START_DESTROY_BLOCK` per click, and still two swings per click (F-M12)? That
  decides whether A's read site survives the polish;
- do the attack indicator and the swing animation still work?
- does the attribute take effect on the client the tick it is applied, and clear when it is removed?

### F8. Chunk load/unload on respawn, dimension change and quit; the entity scheduler (read 2026-11-14, NO BOOT; the seat's D2)

**Instrument.** `run/versions/26.1.2/paper-26.1.2.jar`, `META-INF/MANIFEST.MF` `Build-Number: 74`
(`26.1.2.build.74-stable`, Git-Commit `e4e17fc`), not Paperclip-nested. Classes extracted with
`jar.exe xf <jar> net ca io org/bukkit/craftbukkit` and read with
`C:\Users\Neb91\.jdks\openjdk-26.0.1\bin\javap.exe -c -p <class>`; callers found by grepping the extracted classes
for the target's name, then reading the enclosing method in the dump. The events are
`io.papermc.paper.event.packet.PlayerChunkLoadEvent` / `PlayerChunkUnloadEvent` (paper-api `jar tf`; the
`-sources.jar` for their constructors).

- **F8a. `PlayerChunkUnloadEvent` fires for every sent chunk on a dimension change AND on a same-world respawn (and
  on quit), naming the OLD world.**
  - Its only live construction site is `RegionizedPlayerChunkLoader$PlayerChunkLoaderData#sendUnloadChunkRaw`
    (new at 90, `callEvent` at 131, built only when a listener exists, 80–87). `PlayerChunkSender#dropChunk` also
    builds one, and nothing in the jar calls it.
  - The path: `Entity#setRemoved` 117 → `EntityLookup` → `ServerLevel$EntityCallbacks#onTrackingEnd` 312 →
    `ChunkMap#removeEntity` (ServerPlayer) → `updatePlayerStatus(p, false)` 138 →
    `RegionizedPlayerChunkLoader#removePlayer` 29 → `PlayerChunkLoaderData#remove` 34 → the broadcast map's
    `removeFromOld` → `removeCallback` → `sendUnloadChunk` (if `sentChunks.remove`) → `sendUnloadChunkRaw`.
  - Dimension change: `ServerPlayer#teleport(TeleportTransition)` calls `removePlayerImmediately(this,
    CHANGED_DIMENSION)` at 467 and `setServerLevel` at **534**, the method's only level change. Respawn:
    `PlayerList#respawn` removes at **77** and calls `spawnIn(newLevel)` at **180**. The event's chunk comes from
    `CraftPlayer.getWorld()` (`sendUnloadChunkRaw` 98–116), which is `CraftEntity#getWorld` →
    `entity.level().getWorld()` (CraftPlayer does not override it). **So both paths name the OLD world**, and
    `MiningService.onChunkUnload` finds the slot and clears `shown`.
- **F8b. `PlayerChunkLoadEvent` fires again for those chunks afterwards, over the following ticks.** The re-add
  (`ServerLevel#addRespawnedPlayer` from respawn 501; `addDuringTeleport` from teleport 561) reaches
  `RegionizedPlayerChunkLoader#addPlayer`, which builds a NEW `PlayerChunkLoaderData` (35–44) with an empty
  `sentChunks`, so every chunk is sent again: `ServerChunkCache#tick` 81 → `RegionizedPlayerChunkLoader#tick` 92 →
  `updateQueues` 1098 → `sendChunk` 85 → `PlayerChunkSender#sendChunk` (event at 42, `callEvent` at 64; its only
  caller).
- **F8c. The Player is the SAME object after a respawn and a dimension change, and its entity scheduler is NOT
  retired; on QUIT it is, and a task with a null retired callback is dropped.**
  - `PlayerList#respawn` 87–88 is `aload_1; astore 9` (no `new ServerPlayer`; `restoreFrom` 103, `unsetRemoved`
    185). `ServerPlayer#teleport` returns `aload_0` (695–696) and calls `unsetRemoved` at 471.
  - `Entity#setRemoved` reaches `retireScheduler()` (146) only for a non-ServerPlayer (instanceof 127–131), so a
    ServerPlayer is exempt there whatever the reason. The only other caller is `PlayerList#remove(ServerPlayer,
    Component)` at 300, the quit.
  - `EntityScheduler#retire` calls a task's retired callback only if it is non-null (ifnonnull at 94 and 186).
    `PaperScheduler.onEntityLater` passes null, so **a quit drops the regrowth task silently** (D1).
  - **So D1's fix needs no respawn hook.**
- **F8d. Both events fire on the main (server tick) thread, and are constructed synchronous.**
  `MinecraftServer#tickChildren` 431 → `ServerLevel#tick` 436 → `ServerChunkCache#tick` 81 →
  `RegionizedPlayerChunkLoader#tick`, which opens with `TickThread.ensureTickThread("Cannot tick player chunk
  loader async")` (0–2); `removePlayer`, `addPlayer` (1–3) and `PlayerChunkLoaderData#remove` (4–7) carry the same
  guard. Both event constructors call `super(chunk)` → `ChunkEvent(Chunk)` → `WorldEvent(World)`, the one-argument
  constructor, not `(World, boolean)`. **UNREAD:** the body of `WorldEvent(World)` was not opened; it is expected to
  reach `Event()`, i.e. not async. `RpgListeners`' javadoc ("synchronous, main thread") rests on the tick-thread
  guards above.

**D2 therefore needs no code** (the seat: "if the jar shows the events already cover it, record that and change
nothing"). The gate reads the dimension half (`M-RESPAWN`); the respawn half is NOT READ (it needs a death).

---

## 4. CANDIDATES A AND B, COMPARED ON THOSE FACTS

| | **A: the block click, server ray-cast** | **B: a packet-only INTERACTION per live spot** |
|---|---|---|
| what arrives for a click ON the spot | SWING (`handleAnimate`), plus a dig START on the block behind it | **ATTACK(fake id)** (`handleAttack` → `PlayerUseUnknownEntityEvent`), plus a SWING (client order unknown) |
| what arrives for a click BESIDE the spot | the same SWING + dig START, with a different aim | a SWING + dig START with **no ATTACK** |
| who decides hit vs miss | **the server**: eye position + server rotation, ray-cast against the 5/16 box on the face (pure core maths, unit-testable) | **the client**: its own hit-test, pixel-true to the crosshair |
| where the charge is read, pre-reset | `PlayerArmSwingEvent`, which fires at 289–325, before the reset at 340 (F1) | `PlayerUseUnknownEntityEvent` at `handleAttack` 289, **pre-reset only if the ATTACK is handled before the SWING** (both on the main thread, in arrival order, F1) |
| risks | the server's rotation is the last movement packet's, which may lag the crosshair; it depends on the client sending a swing on a cancelled dig | the box is a CUBE (an Interaction is axis-aligned, width × width × height), so 5/16 deep; one extra fake entity per live spot; whether the client attacks a packet-only Interaction at all |
| the vanilla crosshair indicator | client-side either way. Under B the "attackable entity" crosshair may appear (client-side) | |
| decided by | MS1, MS3, MS4 (and MS5) | MS2, MS3, MS6 (and MS5) |

**B MUST DEFINE A MISS, AND IT IS DEFINED HERE.**
- **Pairing.** Every client packet and every pre-reset event carries one monotonic `seq` (§5's spike line).
  - A **HIT** is an ATTACK on a live spot's fake id, paired with the nearest SWING by seq: the SWING that
    immediately precedes or follows it with no other ATTACK between them.
  - A **MISS** is a SWING with no ATTACK since the previous SWING. Such a click lands on the block, or on air, as a
    dig START.
- **The charge for a hit** is read at `PlayerUseUnknownEntityEvent`, **if the ATTACK is handled before its SWING**.
  If the client sends the SWING first, the attack-time read is POST-reset (F1). B must then read the charge at the
  paired SWING's `PlayerArmSwingEvent` instead, which is pre-reset by construction.
  - **Either way the charge is read at a pre-reset point. Which point it is, is MS2's measured order.**
- Under A the same pairing is unnecessary: the SWING is the hit and the ray-cast is the test.

**The question the spike settles:** does the click reliably produce a pre-reset charge reading, and does the hit
test agree with what Ben aimed at? A wins if its ray-cast matches Ben's aim and a swing always arrives. B wins if
ATTACK reliably arrives for the box and Ben's indicator matches. Neither is picked here.

**SETTLED 2026-11-13: A, unconditionally (the seat's ruling 2, §2).** B is not built. B's two unread rows (MS2(b),
an early click on the box, and MS3(b), whether holding repeats an ATTACK) stay UNREAD.

---

## 5. SLICE 1: THE DESIGN (2026-11-13; replaces the Phase 0 sketch)

**Scope, per Ben:** Trail style only (hit spots stay as cracks, the last hit breaks the patch); one ore (vanilla iron);
one size (the Small vein); no levels; no pickaxe stats beyond attack speed (and the vanilla 1.2 is the value); no
areas. Hit detection is A (§2). Patches are placed by a dev command on the wall the player looks at.

**Every number below is either RULED (§1, with its quote), DERIVED (from a fact, with the derivation), or on the
*Numbers for Ben* list at the end of this section, marked PROVISIONAL.** No other number is picked here.

**Slice 1 branches from master and takes NOTHING from `spike/mining-input` by cherry-pick.** What it re-implements,
with fresh tests, is listed at the end of this section.

### 5.1 The words
- **slot**: a fixed place on a wall where a patch can appear. One anchor block, one face (N, S, E or W), one vein
  size. Shared by every player. Placed and removed by dev commands in slice 1.
- **footprint**: the slot's blocks. For a size `width × height` in blocks, the anchor is the bottom-centre block as
  the player sees the face, and the footprint runs `⌊width/2⌋` blocks to each side and `height − 1` blocks up. For
  the Small vein (3 × 2), that is the anchor, one block left, one right, and the three above them.
- **patch**: one player's instance at a slot. It has a rolled spot layout and a progress. Nobody else sees it.
- **spot**: a weak spot on the patch. Its visible mark is 3 × 3 pixels and its hit box is 5 × 5 pixels (Ben, §1).
  1 pixel = 1/16 block.
- **crack**: a spot that has been hit. It stays on the patch until the patch breaks (Trail style).

### 5.2 core: the rules, unit-tested first (package `io.github.butterflysmp.rpg.core.mining`)
All core types are pure Java: no Bukkit, no PacketEvents. **Each one's test is written before the paper wiring.**

**The face geometry (`FaceFrame`).**
- For each wall face, the frame gives the outward normal and the two in-plane axes as the player sees the face:
  `u` to the player's right, and `v` up. NORTH: the normal is −Z, and right is −X (a player facing south sees west
  on their right). The other three rotate it. A test pins all four.
- **Patch pixel coordinates**: `(pu, pv)`, integers, origin at the footprint's bottom-left as the player sees it.
  `pu ∈ [0, width·16 − 1]`, `pv ∈ [0, height·16 − 1]`. For the Small vein: 48 × 32 pixels.
- `worldToPatch(eye, direction)`: intersect the ray with the face plane (the footprint's outer face), returning
  fractional patch pixels, or EMPTY when the ray is parallel to the plane or points away from it.

**The hit test (`SpotBox`).**
- A spot is centred on pixel `(cu, cv)`. Its 3 × 3 mark covers pixels `cu−1..cu+1` × `cv−1..cv+1`. Its 5 × 5 box
  covers `cu−2..cu+2` × `cv−2..cv+2`, which is fractional patch coordinates `[cu − 2, cu + 3)` × `[cv − 2, cv + 3)`.
- **in** when the ray's patch point lies in that box. Edge rule: the low edge is in, the high edge is out, so two
  adjacent boxes never both claim a point. Tests: the four corners, the four edges either side, the centre, and a ray
  that misses the plane.
- **Only the LIVE spot's box counts.** A click on a crack, or anywhere else on the patch, is a miss.

**The charged rule (`ChargeRule`).**
- **Charged iff `getAttackCooldown()` returns exactly 1.0f.** The API clamps at 1.0 (F1), so a full charge reads
  exactly 1.0f.
- **No tolerance.** At 1.2 the read one tick before full is **0.9900** (§5.7, TOOLS table). A threshold like
  `≥ 0.99` would accept an uncharged swing. The test pins 1.0f as charged, and 0.99f and 0.9999f as not.

**The spot layout (`SpotLayout.roll(size, rng)`).**
- The count is drawn uniformly from the size's `spots.min..spots.max`, inclusive.
- **Each centre is placed so its 5 × 5 box lies wholly inside the patch**: `cu ∈ [2, width·16 − 3]`,
  `cv ∈ [2, height·16 − 3]`. This is the EDGE MARGIN of 2 pixels, DERIVED from the 5 × 5 box. It is on the list
  below because it could be wider for the art's sake.
- **No two spots' 5 × 5 boxes overlap**: the centres are at Chebyshev distance ≥ 5. That is the SPOT SPACING of 5
  pixels, DERIVED from the box (it is the smallest spacing at which no point is in two boxes). Also on the list.
- **The reveal order is the roll order.**
- The generator is injected (`java.util.random.RandomGenerator`), so tests seed it. Placement is rejection sampling
  with a bounded number of attempts, then `IllegalStateException`. The size's constructor refuses a `spots.max`
  greater than the number of disjoint 5 × 5 tiles in the margin-bounded area, so a legal size cannot hit that bound
  in practice. The test rolls 10,000 seeds of every shipped size and asserts every layout obeys both rules.
  *(2026-11-13, the seat's F-A, fixed on S1-a in `e7308aa4`: "the number of disjoint 5 × 5 tiles" counted a
  perfect tiling, and a random packing can stop far short of one, so the bound overstated its guarantee. It is now
  the WORST-CASE count, `ceil(aw/9) × ceil(ah/9)` over the centre area `aw = widthPx − 4`, `ah = heightPx − 4`.
  Each placed centre blocks a 9 × 9 square of centres, and the grid points `(9i, 9j)` are pairwise 9 apart, so
  fewer spots than that always leave a free legal position. Small: 5 × 4 = 20, which Small's 3 is well under.
  Rejection sampling and its exception are unchanged.)*
- **Re-rolled at every regrowth**: size and spot positions re-roll each regrowth (the brief). With one size, only
  the spots move.

**The patch state machine (`Patch`).**
```
FRESH(spot 1 live) --charged hit on the live spot--> spot 1 becomes a CRACK, spot 2 is revealed and live
   ...
last spot live --charged hit--> BROKEN: pays the yield, starts regrowth, and every mark goes
BROKEN --regrowth deadline passes--> a fresh roll, FRESH
```
- **Input:** one `Click(float cooldown, Optional<PatchPoint> aim, boolean holdingPickaxe, long tick)`.
- **Outcome**, a sealed type (a closed set of record types that a `switch` can check exhaustively):
  - `NotLive`: the patch is BROKEN and regrowing. Nothing changes.
  - `Denied`: not holding a pickaxe. Nothing changes. (Ben: *"Yes, denied if not holding a pickaxe"*.)
  - `Miss`: the aim is outside the live spot's box. **Nothing changes in slice 1: no penalty.**
  - `Uncharged`: the aim is in the box, but the charge is under 1.0. **Nothing changes in slice 1: no penalty.**
  - `Hit(revealed)`: the live spot cracks and the next is revealed.
  - `Broke(payout)`: the last spot was hit. The patch is BROKEN.
- The order of the checks is the order of that list. Each outcome has a test, plus one that walks a 3-spot patch
  from FRESH to BROKEN.
- **The state records each spot's reveal tick and hit tick.** Nothing reads them for payout in slice 1. They exist
  so a gold window can be computed once Ben rules it (the list below), and the dev trace prints them.
- **What a miss and an uncharged hit SHOW** (a sound, a message, nothing) is a question for Ben, on the list. Slice
  1's default is nothing beyond the vanilla swing, except `Denied`, which sends an action-bar line.
  *(2026-11-13, RULED: a `Miss` plays the ore's miss sound (Ben: "yes"), and a `Broke` plays its break sound, each to
  that player only. `Uncharged` and `NotLive` play nothing. `Denied` keeps its action-bar line. Core only reports
  the outcome; S1-b plays the sound.)*

**The yield (`Payout`).**
- `count = per_spot × spots_in_this_patch + completion_bonus`. Every term is a whole number of items, ≥ 0.
- **NAME THE QUANTITY.** The quantity is an item count, an integer, so it is quantised by nature. Every term is
  authored flat; there are no percentages and no multipliers. The eligible set is every patch of the ore.
- `per_spot`, `completion_bonus` and the item are on the list. **No gold-window or perfect-chain term exists until
  Ben rules one.** *(2026-11-13: the item is now RULED, `minecraft:raw_iron`, §1. `per_spot` and
  `completion_bonus` are still Ben's, and are UNSET until he rules them, §2.)*
- **2026-11-13, RULED (Ben, and the seat's rulings 1–3 in §2). This replaces the `count` formula above:**
  - `count = Σ roll_i + completion_bonus[size]`. There is one `roll_i` per spot in this patch, each drawn uniformly
    and independently from `per_spot.min..per_spot.max` inclusive, at the break, from the injected
    `RandomGenerator`. The bonus is the ore's entry for the patch's vein size.
  - **Small iron:** `per_spot` 2..4, bonus 5, spots 2..3. **Executed** (a shell loop over the spot counts):
    2 spots → 9..13, 3 spots → 11..17, **so 9..17 raw iron per break.** A test pins both ends with a deterministic
    generator (every roll at `min` with 2 spots gives 9; every roll at `max` with 3 spots gives 17). A seeded sweep
    asserts every total lies in 9..17 and that both ends occur.
  - **NAME THE QUANTITY:** whole item counts, flat, no percentages and no multipliers.
  - **No gold-window or perfect-chain term:** Ben deferred them (*"later"*).
- **Delivery:** `PlayerInventory#addItem`. Whatever it returns as not fitting is dropped at the player's feet with
  `World#dropItemNaturally`, as vanilla does with a full inventory (the seat's ruling, §2). The trace's break line
  carries both parts.

**The regrowth clock (`RegrowthClock`).**
- In memory: `Map<UUID, Map<SlotId, Long>>`, the server tick at which that player's patch at that slot regrows. Time
  comes from an injected `LongSupplier` (the paper side passes `Bukkit.getCurrentTick()`), so tests drive it.
- **Kept when the player logs out**, because nothing removes the entry on quit. **Lost on restart**, because it is
  memory. Nothing goes into `PlayerProfile`.
- **NAME THE QUANTITY.** `regrowth_ticks` is server ticks, an integer, continuous to one tick. **1200 (Ben, §1:
  *"1 minute is fine for now"*).** It counts SERVER ticks: under 20 TPS a "minute" is longer in real time. That is
  stated here, not hidden.
- **A partly mined patch is kept the same way** (state in memory per UUID per slot, kept across logout, lost on
  restart). That is a choice made here, on the seat's list in the report. *(Approved 2026-11-13, §2 ruling 4.)*

**The dev trace line (`MiningTraceLine`).** One format, pinned exactly by a test as `MineSpikeLine` was:
`MINING click player=<name> slot=<id> outcome=<HIT|BROKE|MISS|UNCHARGED|DENIED|NOT_LIVE> spot=<i>/<n> cooldown=%.4f
aim=<du>,<dv> tick=<t>`, plus `MINING break … payout=<item>x<count> regrow_at=<tick>` and `MINING regrow …`. It uses
`Locale.ROOT`, with `-` for an absent field. It is printed only for players who turned the trace on (§5.4).
*(2026-11-13: the break line becomes `… payout=<item>x<count> kept=<n> dropped=<n> regrow_at=<tick>`, where
`kept + dropped = count`. `dropped` is what a full inventory left at the player's feet. The full-inventory case
gets its own gate row in S1-b.)*
*(2026-11-13, the seat's ruling 1: the break line also carries each spot's roll and the bonus,
`rolls=<r1>+<r2>[+<r3>] bonus=<b>`, so that `count` is checkable by addition on the line itself.)*

### 5.3 content: the schema, the loader and ContentValidator
Two new directories, flat like every other content directory (`RpgPlugin` copies every `content/*.yml` from the jar
by prefix).

**`content/ores/iron.yml`:**
```yaml
ores:
  iron:
    display_name: "Iron"
    payout:
      item: raw_iron                       # RULED, Ben 2026-11-13: "raw iron". A vanilla material, NOT minted
      per_spot: { min: 2, max: 4 }         # RULED, Ben 2026-11-13: "2-4 per spot". Rolled per spot, at the break
      completion_bonus: { small: 5 }       # RULED, Ben 2026-11-13: "with a 5 completion bonus". Keyed by vein size
    regrowth_ticks: 1200                   # RULED, Ben 2026-11-13: "1 minute is fine for now"
    sizes: [small]                         # RULED, Ben 2026-11-13: "keep it as the small vein for now"
    models:                                # item_model ids in the project pack
      patch: rpg:mining/iron_patch
      spot: rpg:mining/spot
      crack: rpg:mining/crack
    sounds:                                # PLACEHOLDER vanilla sounds (the seat's ruling 4); Ben's art pass replaces them
      miss: minecraft:block.stone.hit      # PLACEHOLDER. Ben: a miss makes a sound ("yes"; his doc: "a dull clink")
      break: minecraft:block.stone.break   # PLACEHOLDER
```
*(This block was rewritten on 2026-11-13 for Ben's numbers. The earlier text had `per_spot` and `completion_bonus`
UNSET, with the note that the loader refuses the file until they land. That refusal of a missing payout stays.)*

**Why the sound keys live in the ORE file, not in a mining defaults file** (the seat left the choice to me, to be
argued here):
- **It reuses the one schema and loader** that already exist. A defaults file is a third file type, with its own
  loader and validator path and its own zero-check, all to carry two keys.
- **A sound is an ore's character.** Iron's clink and a future gem ore's chime are per-ore by nature, as the models
  already are, and there is one place to look for everything an ore shows and plays.
- **The cost:** the second ore repeats two lines if it wants the same sounds. That is cheaper than a defaults file
  and an override rule, and it is the trade to revisit if many ores share sounds.
- **The break particle** (the seat's ruling 7: placeholder, named in content) joins the same `sounds`-style block in
  S1-b, the slice that plays it. S1-a ships only the two sound keys the seat named.
**`content/vein_sizes/small.yml`:**
```yaml
vein_sizes:
  small:
    width: 3        # blocks along the face (design doc, relayed by the seat)
    height: 2       # blocks up the face
    spots: { min: 2, max: 3 }   # design doc, relayed by the seat
```
*(2026-11-13: all three values are now RULED. Ben said "yes" to "3 wide by 2 tall with 2-3 spots", §1.)*

**NAME THE QUANTITY for every key:**

| key | quantity | arithmetic | eligibility |
|---|---|---|---|
| `width`, `height` | whole blocks of wall | integer ≥ 1; it scales one flat model (F-M4), so there is no −16..32 element limit | every slot of this size |
| `spots.min`, `spots.max` | spot count | integer, 1 ≤ min ≤ max ≤ the worst-case guaranteed count (§5.2, F-A; was "the tile capacity") | every patch of this size |
| `regrowth_ticks` | server ticks | integer > 0, continuous to 1 tick | every patch of this ore |
| `payout.per_spot.min`, `.max` | item count per spot | integer, 0 ≤ min ≤ max, flat, rolled uniformly per spot | every patch of this ore |
| `payout.completion_bonus.<size>` | item count per break | integer ≥ 0, flat; exactly one entry per listed size | every patch of this ore at that size |
| `payout.item` | a vanilla item | must resolve to an item `Material` | — |
| `models.*` | an item_model id | must name a model the project pack ships | — |
| `sounds.miss`, `sounds.break` | a vanilla sound key | must resolve in the sound registry; PLACEHOLDER | — |

*(The two payout rows replace the 2026-11-13 morning row "`payout.per_spot`, `payout.completion_bonus` — item count
— integer ≥ 0, flat", per the seat's rulings 1 and 2.)*

**Refusals added by Ben's numbers** (each with a unit test): an ore with no `payout` or no `per_spot` (the refusal
that predates the numbers stays); `per_spot.min > max`, or a negative value; a size in `sizes:` with no
`completion_bonus` entry; a `completion_bonus` entry for a size not in `sizes:`; a negative bonus; a missing
`sounds.miss` or `sounds.break`. ContentValidator adds: each sound key resolves.

**Refusals.**
- The core records (`OreDefinition`, `VeinSize`) refuse what needs no registry, in the `ToolDefinition` style:
  blanks, ranges, `min > max`, capacity, and `regrowth_ticks ≤ 0`. The loader turns a refusal into a named skipped
  entry. A unit test covers each refusal.
- **`ContentValidator.validateMining`** does what needs the server, through predicate seams as the rest of that
  class does:
  - every `sizes:` entry names a loaded vein size;
  - `payout.item` resolves (`Material.matchMaterial`, and `isItem()`);
  - every model id has `assets/<ns>/items/<path>.json` in the jar's `resourcepack/`.
- `RpgPlugin` gains the zero-check warning that tools and armour carry (*"No ores loaded from content/ores…"*), and
  the `Loaded …` line gains `N ores, M vein sizes`. **R0c predicts that line.**

### 5.4 paper: input, visuals, commands and storage

**Input: the read site is `BlockDamageEvent`** (the seat's ruling 3 allowed it or the first `ARM_SWING`).
- **Why `BLOCK_DAMAGE`:**
  1. It reads before the reset: on every MS1 click it equalled the first `ARM_SWING`.
  2. **It fires once per click by construction.** It comes from the dig START, and each click sends one START. MS1 read a
     `BLOCK_DAMAGE` on every click (equal to its first `ARM_SWING`), and one `DIG_START` for the whole of MS3's 58-swing hold. So a hold gives one read, the
     first, which is an honest click. A held swing can never be read.
     > **CORRECTED 2026-11-14 (the seat's ruling 1 on the slice 1 readings; F-M27). What was claimed:** "fires once
     > per click by construction", so "a hold gives one read". The seat approved that reason. **What was read** (slice
     > 1 boot of `863bf10a`): a hold produced **two** click lines in M-HOLD and **three** in M-PROTECT. The client
     > restarts the dig during a hold. **What actually protects a hold from mining is the CHARGE RULE, not the event
     > count:** the restarted digs read `0.1500`, `0.0900` and `0.2700` (2, 1 and 4 ticks after a reset, at a
     > 16.67-tick delay; they logged MISS because the aim was off the live spot), and a read below `1.0` can never be a HIT. The `0.8100` restart in M-HOLD (13 ticks) is not explained by a hold
     > and stays UNREAD (a re-press is the likely cause, not asserted). The sentence above is kept as written.
  3. It carries what the hit test needs: the block, `getBlockFace()` and `getItemInHand()` (on the pinned API,
     `javap -p org.bukkit.event.block.BlockDamageEvent`). And it is the event slice 1 cancels anyway.
  4. It needs no per-player pairing state.
- **The second swing of the same click is ignored because no swing is a read site.** Slice 1 listens to no
  `PlayerArmSwingEvent` and installs no packet listener. Nothing runs on a Netty thread.
- **The handler**, one method in `RpgListeners`, on the main thread:
  - it looks up the block in the footprint index; if the block is not in a footprint, it returns;
  - it cancels the event, always, for every player;
  - if `getBlockFace()` is not the slot's face, it returns, because a click on the side of the wall is not on the
    patch;
  - it builds a `Click` from `getAttackCooldown()`, the eye ray (`getEyeLocation()` and its direction) through
    `FaceFrame.worldToPatch`, `Tag.ITEMS_PICKAXES.isTagged(getItemInHand().getType())`, and
    `Bukkit.getCurrentTick()`;
  - it hands the outcome to the visuals and the payout.
- It is **not** `ignoreCancelled`: master's `onStoneBlockDamage` cancels the same event when a stone is held, and the
  patch block must stay protected regardless (that click is `Denied` anyway).
- **The aim is the server's rotation at the dig START.** The spike logged aim at `ARM_SWING`, one packet later. The
  trace logs the aim, and gate row M-HIT compares it to where Ben aimed (§5.10).
- **Not covered** by a dev slot: creative players (a creative START breaks before `BlockDamageEvent`, F2b, so
  creative cannot mine, though `BlockBreakEvent` still protects the block), explosions, pistons, fluids and entity
  block changes. **That is the area rule's job, and areas are out of scope.** F-M8 also stands: dev slots go on
  blocks with no `attack` override (stone or deepslate), and the placement command does not police it.
- **`BlockBreakEvent`** on a footprint block is cancelled for every player (F-M5).

**The pickaxe requirement.** Any item in vanilla's `#minecraft:pickaxes` tag (`Tag.ITEMS_PICKAXES`), minted or not.
Whether some pickaxes should not count (for example by tier) is a question for Ben, on the list.

**Visuals: packet-only `ITEM_DISPLAY`s from the pack's models, per player** (F5; sent from the main thread, as
`PacketDamagePopupSender` does).
- Each patch is one display with `models.patch`, scaled `(width, height, 1)` and centred on the footprint's face,
  0.0005 out. Each revealed spot is one display with `models.spot`, scaled 3/16, at its centre pixel, 0.001 out.
  A hit swaps that display's item (index 23) to `models.crack`. The rotation follows `FaceFrame`. The display type
  is FIXED (index 24 = 8).
- **The break:** every display of that patch is destroyed, and the player alone gets a sound and a particle burst.
  `Player#playSound` and `Player#spawnParticle` are per-player Bukkit calls, on the main thread. Which sound and
  which particle are art choices, on the list.
- **Lifecycle:** displays are spawned when a slot is placed (for players in that world), on
  `PlayerChunkLoadEvent` for chunks that hold a slot, and on regrowth. They are destroyed on
  `PlayerChunkUnloadEvent`, on slot removal and on quit. Both events are in the pinned API
  (`io.papermc.paper.event.packet`).
- **Regrowth while online:** on the break, `Scheduler.onEntityLater(player, …, 1200)` respawns the patch. If the
  player has left, the entity scheduler drops the task, and the join or chunk-load path renders from the clock. The
  `Scheduler` interface has no repeating call, and none is needed.
  > **CORRECTED 2026-11-14 (the seat's D1, F-M21): the sentence above was FALSE as built, and the seat approved
  > it.** Nothing in the join or chunk-load path armed a respawn: `show` found the patch regrowing and returned, so
  > after a relog the wall stayed bare past `regrow_at` until a click. The design now: **whenever `show` finds the
  > player's patch at a slot regrowing, it makes sure ONE pending task exists for that player and slot on the
  > CURRENT Player** (`MiningBook.claimRegrowthTask`, `RegrowthTasks`: deduped per player and slot, with a
  > never-reused token; the marker clears when the task shows the patch, on quit and on slot removal). The break
  > arms the first task the same way. A respawn or dimension change needs no hook (F8c).
- **Entity ids: ONE allocator** (F-M7). `PopupEntityIds` moves out of `health/` to a shared counter, used by the
  damage popups and mining alike. The spike's second range picked by eye is not repeated.
  > **As built (S1-b), the class did NOT move or rename** (F-M23): it stays `health/PopupEntityIds`, made public
  > and shared.

**The dev commands** (`/rpg mining …`, each with `.requires(Permissions.DEV)` on the line after `literal`, pinned
by a wiring-signature test). Mining's dev commands move to the `rpg.dev` guard once #175 merges (§2).
- `place <ore> <size>` places a slot on the looked-at wall face (`rayTraceBlocks(5)`). It refuses UP and DOWN
  faces, any footprint block that is not solid, any footprint block whose neighbour in front is not passable, and
  any overlap with another slot, naming the block that failed.
- `remove` removes the slot under the crosshair: it destroys every player's displays and clears its patches and
  clocks.
- `regrow` clears the caller's own regrowth deadlines. It exists for the gate.
- `trace` toggles the caller's `MINING` log lines.
- `pack` offers the dev pack now (§5.8).

**Where placed slots live in slice 1: in memory, in one `MiningSlots` registry** (slot id → slot, plus a footprint
index from block to slot). **Lost on restart; the gate re-places its slot each boot.** It is NOT:
- the area model (no notion of a region, a no-break rule, or which areas get patches);
- persistence (no file and no repository);
- natural placement (nothing places a slot except the dev command).

A dev file would save the gate a command per boot, at the price of storage code that the area model will replace.
That trade is the seat's to rule, and it is in the report.

### 5.5 What slice 1 does NOT build
- mining levels, and the XP curve below (recorded, not built);
- Fortune, level rewards, recipes, and combat-gear requirements (Ben deferred these);
- `PlayerProfile` persistence;
- custom areas and the area no-break rule (the target, §2);
- the dig-stop (Ben deferred it, F7);
- candidate B (the seat, §2);
- Unstable veins (3–5 spots: recorded only), and every size but Small;
- custom ores, and any custom (minted) payout item;
- any gold-window or perfect-chain payout, until Ben rules its numbers;
- natural placement, and any persistence of slots;
- requiring the pack (the real-server target, §5.8);
- the `attack_speed` tool key (§5.7: slice 1 does not need it).

### 5.6 MINING XP: THE PLAYER LEVEL CURVE DIVIDED BY 10 (not built in slice 1)
- **Executed, not predicted.** I compiled master's core (`./mvnw -q -pl core clean compile`) and ran jshell over
  `core/target/classes`, reading `PlayerLevel.XP_TO_NEXT` reflectively and calling `PlayerLevel.totalForLevel`:
  ```
  entries=99 nonMultiplesOf10=0
  totalForLevel(50)=712580 /10=71258 rem=0
  XP_TO_NEXT[49] (49->50)=58940 /10=5894
  XP_TO_NEXT[50] (50->51)=64160 /10=6416
  totalForLevel(99)=11642250 /10=1164225
  ```
- **Mining level 50 needs 71,258 cumulative mining XP.** The rung from 49 to 50 is 5,894, and the rung from 50 to 51
  is 6,416. `totalForLevel(50) = 712,580` matches `GATE-level.md`.
- **The rounding rule proposed: divide each RUNG (`XP_TO_NEXT[i]`) by 10 with `Math.floorDiv`, and build the
  cumulative totals from the divided rungs.** It does not divide the totals.
  - Today every rung is a multiple of 10 (the table is hand-rounded to the nearest 10), so the floor never bites and
    the two orders agree.
  - The slice that builds this ships a core test asserting **"every `XP_TO_NEXT` entry is a multiple of 10"**. That
    pins the floor as a no-op, and goes red if the table is ever re-authored, which is the moment the rounding rule
    starts to matter.
- **NAME THE QUANTITY:** mining XP is an integer per rung, continuous enough at every rung (the smallest rung is
  100, from `XP_TO_NEXT[1] = 1000`). Eligibility: every player.

### 5.7 TOOLS: ATTACK SPEED ON A CUSTOM PICKAXE, UNDER NAME THE QUANTITY

**SLICE 1 NEEDS NO ATTACK-SPEED MODIFIER, AND NOT THE KEY EITHER (2026-11-13).** Ben ruled the first pickaxe's speed
*"Vanilla speed for now is fine"*. The vanilla iron pickaxe's own default is 1.2, so the value is already on the item:
`ToolItems.mint` pins no modifier, so a minted `iron_pickaxe` keeps its type defaults, and `AttackSpeedAttributeOverride`
leaves a pickaxe alone (§3, the override reading). Holding it, the player's speed is 1.2: a 16.666666-tick delay,
first full read at ticker 17 by the executed formula. Spike 1 fitted that formula at 1.2 (MS1) but never isolated the tick-17 boundary, so a slice 1 row reads it.
- **So the `attack_speed` key, the mint path and the tooltip-order fix below are for the FIRST PICKAXE WITH A
  NON-VANILLA SPEED.** That pickaxe does not exist yet.
- **Recommendation: build them in their own slice, with that pickaxe's content, and not in slice 1.** A key that no
  shipped content declares has no in-game witness: the tooltip order (F-M15) could only be read through a test-only
  tool. Until then, iron.yml's *"a minted tool changes nothing"* stays TRUE and is not rewritten.
- If the seat wants it in slice 1 anyway, it is the separate PR S1-T in §5.10, and everything below applies unchanged.

**The design, for whichever slice builds it:**
- **The quantity:** the player's `ATTACK_SPEED` attribute while the pickaxe is held, in attacks per second. It is a
  continuous double.
  - **What it buys is quantised to whole ticks.** The ticker is an integer, and full charge is the first ticker with
    `(t + 0.5) / delay ≥ 1`.
  - Executed in jshell (mirroring `getCurrentItemAttackStrengthDelay` as `(float)(1.0 / speed * 20.0)` and
    `getAttackStrengthScale` as `clamp((t + 0.5f) / delay)`):

    | speed | modifier (speed − 4.0) | delay (float) | first full ticker | scale at ticker 0 | one tick before full |
    |---|---|---|---|---|---|
    | 1.0 | −3.0 | 20.0 | 20 | 0.0250 | 0.9750 |
    | 1.2 | −2.8 | 16.666666 | 17 | 0.0300 | 0.9900 |
    | 2.0 | −2.0 | 10.0 | 10 | 0.0500 | 0.9500 |
    | 4.0 | 0.0 | 5.0 | 5 | 0.1000 | 0.9000 |

  - Speeds whose delays round to the same integer tick are indistinguishable. **Author speeds by the tick they buy.**
- **The eligible set:** pickaxes only (`ToolKind.PICKAXE`). An axe, shovel or hoe has no mining-minigame use, so the
  key is refused on any other kind.
- **How `ToolDefinition` gains it:**
  - A new component, `OptionalDouble attackSpeed`, holding the RESULTING speed in attacks per second, not a
    modifier. It is refused if ≤ 0 or non-finite, and refused on a non-pickaxe kind.
  - `ToolLoader` reads an optional `attack_speed:` key.
  - `ToolItems.mint`, when the value is present:
    - copies the type's default modifiers (F4: anything less erases the pickaxe's attack damage);
    - replaces the `minecraft:base_attack_speed` entry with `attackSpeed − 4.0`, `ADD_NUMBER`, `MAINHAND`, so the
      tooltip keeps vanilla's green "N Attack Speed" line;
    - **builds the modifiers in an insertion-ordered multimap (`MultimapBuilder.linkedHashKeys().arrayListValues()`),
      attack damage first, then attack speed**, so the tooltip keeps vanilla's order (Ben: *"yes for now"*). Never
      `ArrayListMultimap`: its keys live in a `HashMap`, and that is how the spike's tooltip came out reversed
      (F-M15). The order is pinned by a test on the ordered list of attribute keys the mint writes, and read on a
      gate row;
    - keeps `setMaxStackSize(1)`.
  - When absent, mint is unchanged and pins nothing.
- **What happens to iron.yml's statement.** iron.yml says *"WHAT A MINTED TOOL DOES NOT CHANGE: … ToolItems.mint
  pins no attribute modifier and sets no item flag, so a minted iron pickaxe digs exactly as a plain one does. There
  is no stat key here because there is no stat"*.
  - **It stays TRUE of every tool that declares no `attack_speed`, and becomes FALSE for any that does.**
  - The slice that adds the key rewrites that header in the same commit, to read: a minted tool changes nothing
    *unless it declares `attack_speed`*, which pins the main-hand speed and copies the defaults.
  - That slice also rewrites `ToolItems`' javadoc line *"Nothing in this project ever calls
    `setAttributeModifiers`"*, and adds a `ToolDefinitionTest` row for the refusal on a non-pickaxe.
  - The spike changes neither: its test pickaxe is minted inside the spike command, not through `ToolItems`.
- **The override interaction is settled (§3, THE OVERRIDE READING):** a pickaxe is not a melee weapon, so the
  override removes its own key and leaves the item's modifier as the player's speed.

### 5.8 THE RESOURCE PACK
*(The 2026-11-12 text described the spike's pack. It is rewritten for slice 1's real pack; the spike's own account
is its gate's MS7.)*
- **Source: `paper/src/main/resources/resourcepack/`.** It ships inside the plugin jar as a resource: no new Maven
  plugin and no separate artefact.
  - `pack.mcmeta`: format 84, `min_format`/`max_format` (F6).
  - `assets/rpg/items/mining/{iron_patch,spot,crack}.json`, each `{"model":{"type":"minecraft:model","model":"rpg:item/mining/<id>"}}`.
  - `assets/rpg/models/item/mining/<id>.json`: one flat 16 × 16 face each (F-M4).
  - `assets/rpg/textures/item/mining/<id>.png`. **Placeholder art; the look is Ben's.** The patch texture is drawn at
    the Small vein's 3 : 2 aspect, because one model is scaled `(3, 2, 1)` and a square texture would stretch.
- **Built:** the plugin zips that directory from its own jar in memory, in a fixed entry order with fixed
  timestamps, and hashes it with SHA-1.
  - **The zip is only deterministic per commit once line endings are pinned (F-M11).** That PR adds
    `.gitattributes` lines: `paper/src/main/resources/resourcepack/**/*.json` and `*.mcmeta` as `text eol=lf`, and
    `*.png binary`. Then every checkout copies the same bytes into the jar.
  - **And it lands after the F-M9 fix**, so `check-crlf.sh` stops false-MIXING the PNGs. That fix is its own small PR
    to master with a positive control (the seat's F-M9 ruling), and it is §5.10's S1-0.
  - A paper test (re-implementing `MineSpikePackTest`'s checks): `pack.mcmeta` parses at format 84; every items
    JSON resolves to a model and every model to a PNG; the PNGs decode; two builds hash equal; and every ore's
    `models.*` id exists in the pack (the same check ContentValidator makes at boot).
- **Offered in dev, per Ben (*"it might be better to have it prompt or toggle for dev work"*):**
  - a `com.sun.net.httpserver.HttpServer` **bound to 127.0.0.1 only** serves the zip. It starts on first offer
    and stops on plugin disable (the standing rule). That assumes the client runs on the server's machine, which
    spike 1 confirmed for Ben's seat (F-M17);
  - **the toggle:** `config.yml` gains `mining.dev-pack-on-join: false`. When it is true, each join offers the pack
    with `required(false)` and a prompt line. **The prompt** is the client's own accept-or-decline screen;
  - **on demand:** `/rpg mining pack` offers it at any time;
  - whether "prompt or toggle" means one of these or both is Ben's to say. Both are cheap, so both are planned and
    the question is on the list.
- **The real server: the pack is REQUIRED** (Ben, §1). **That is the target, and it is NOT built in slice 1.** It
  needs a public HTTPS host and `required(true)` (or `require-resource-pack=true`, which is the only setting that
  kicks server-side, F6). The host is an operator decision, and it is open.

### 5.9 WHAT HAPPENS TO THE SPIKE CODE
**Slice 1 branches from master and cherry-picks nothing from `spike/mining-input`.** That branch never merges, and is
deleted only after the seat confirms the gate copy (status block).

| spike idea | slice 1 |
|---|---|
| `FacePoint`: face axes, the pixel snap, the box test, the rotation | **re-implemented** as `FaceFrame` and `SpotBox` (§5.2), with the edge rule stated and fresh tests |
| `MineSpikeVisuals`: ITEM_DISPLAY metadata 11–15, 23 and 24, a fresh wrapper per send | **re-implemented** in `MiningVisuals`, with the item swap for a crack |
| `MineSpikePack`: the deterministic in-memory zip, the SHA-1, the 127.0.0.1 server, the offer and the status log | **re-implemented**, plus the join toggle and F-M11's pinned line endings |
| the pack layout (`items/` → `models/` → `textures/`, `pack.mcmeta` at 84) | **re-implemented** with slice 1's three models |
| `MineSpikePackTest`'s checks | **re-implemented**, plus the ore-model check |
| `givePick`'s default-modifier copy | **re-implemented only in the tools slice (§5.7)**, with the ordered multimap, never `ArrayListMultimap` |
| `MineSpikeLine`'s pinned format | the **idea** only: `MiningTraceLine` is a new format with its own test |
| `MineSpikeListener` (the packet classifier on the Netty thread) | **NOT carried.** Slice 1 has no packet listener (§5.4) |
| the INTERACTION box (candidate B) | **NOT carried** (the seat, §2) |
| `/rpg minespike` and its `PICK`/`PACK`/`PLACE` lines | **NOT carried.** Replaced by `/rpg mining …` |
| the second entity-id range from `1_500_000_000` | **NOT carried.** One shared allocator (F-M7) |

### 5.10 THE SLICE BREAKDOWN AND EACH SLICE'S GATE ROWS (in outline)
One PR is too big to read: core, content, the pack, the visuals, the input and the commands. So:

| PR | from | contents | gate |
|---|---|---|---|
| **S1-0** | master | F-M9: `check-crlf.sh` skips files git classes as `-text`, with a positive control (a PNG fixture is not flagged, and a mixed text fixture still is). An instrument change: its own small PR (the seat's F-M9 ruling) | none; the script's own controls |
| **S1-a** | master | core `mining/` (§5.2) with every unit test; `OreDefinition` and `VeinSize`; the loader; `ContentValidator.validateMining`; `iron.yml` and `small.yml`; the zero-check and the `Loaded` line. No visuals, no input, no commands | R0 only; read at S1-b's boot |
| **S1-b** | S1-a merged | the pack (with F-M11's `.gitattributes` lines, after S1-0); the shared entity-id allocator; `MiningVisuals`; `MiningSlots`; the `BlockDamageEvent` and `BlockBreakEvent` handler; the payout; the regrowth scheduling; `/rpg mining …`; the trace | the rows below |
| **S1-T** | master, when the seat says | §5.7's `attack_speed` key, the ordered mint and the iron.yml rewrite, with the first pickaxe that declares a non-vanilla speed | a tooltip row and a refill row |

**THE ORDER, CHANGED BY THE SEAT, 2026-11-13: S1-b IS STACKED ON S1-a, AND S1-a DOES NOT MERGE FIRST.** S1-a changes
plugin enable (two new loaders, the `Loaded` line, two zero-checks and `validateMining`) and has never been booted.
So, under the standing stacked-PR rule:
- S1-b branches from `feat/mining-s1a` (with its F-A commit), not from a master that has S1-a;
- the ONE R0 lives on the stack top's gate, `docs/gates/GATE-mining-slice1.md`, and one boot reads both slices;
- they merge **bottom-up** after the seat approves: S1-a first, then S1-b rebased with `--onto` onto the squash, with
  its patch ids shown unchanged across the rebase.
The table's "S1-a merged" in S1-b's *from* column describes the plan before this ruling. S1-0 (#181) is
independent of both: script only, no runtime effect.

**The S1-b gate's rows, in outline** (`docs/gates/GATE-mining-slice1.md`, SURVIVAL on every row, committed before
any boot):
- **R0a** the build line names the head. **R0b** the jar carries `resourcepack/` and the mining classes, with a
  control class that must be absent. **R0c** the `Loaded …` line, with `1 ores, 1 vein sizes` and no refusal.
- **M-PACK**: `/rpg mining pack`, accepted: ACCEPTED → DOWNLOADED → SUCCESSFULLY_LOADED, the sha1 equal to the
  build's. The join toggle off: no offer at join. On: an offer at join.
- **M-PLACE**: `place iron small` on a deepslate wall logs the slot and footprint. Each refusal (an UP face, a
  non-solid footprint block, a blocked front, an overlap) names its block.
- **M-LOOK (sight, F-M18)**: Ben states the patch's SIZE (3 wide × 2 tall?), FACING (flat on the wall, facing out?)
  and POSITION (centred on the footprint?), and that the live spot sits where the trace says.
- **M-HIT**: a charged click on the live spot logs `HIT` with cooldown 1.0000 and aim `in`. The mark becomes a crack
  and the next spot appears. The aim at the dig START matches where Ben aimed (§5.4).
- **M-MISS**: a charged click elsewhere on the patch logs `MISS`, nothing changes, and **Ben hears the miss sound
  (`sounds.miss`, the PLACEHOLDER `minecraft:block.stone.hit`)**, which a second player does not hear (if one is
  present). An uncharged click plays nothing. **M-UNCHARGED**: an early
  click on the spot logs `UNCHARGED` with the predicted `(Δ+0.5)/16.666666`, and nothing changes. **M-CHARGE-17** reads the boundary spike 1 never isolated: a click at Δ=16 logs `UNCHARGED` 0.9900, and one at Δ=17 logs `HIT` 1.0000.
- **M-HOLD**: holding on the live spot logs at most one click, and nothing after it.
- **M-DENY**: an empty hand and a sword each log `DENIED` with the action-bar line. A plain vanilla pickaxe (not
  minted) works.
- **M-BREAK**: the last spot logs `BROKE`, and the break line shows one roll per spot, **each in 2..4**, plus
  `bonus=5`, with **a total inside 9..17** that equals their sum (11..17 if the patch had 3 spots, 9..13 if 2). The
  inventory gains that many raw iron. Every mark goes, and the break sound (`sounds.break`, PLACEHOLDER) and the
  particles play. Clicks then log `NOT_LIVE` and play nothing.
- **M-FULL**: the same break with a nearly full inventory: `kept + dropped = count`, and the dropped raw iron lies at
  Ben's feet.
- **M-REGROW**: 1200 ticks after the break (the trace's `regrow_at`), the patch returns with a re-rolled layout:
  the spot positions in the trace differ.
- **M-LOGOUT**: break, log out, log back in before `regrow_at`: still regrowing. After it: fresh.
- **M-PROTECT**: a survival hold breaks nothing (no BlockBreakEvent gets through; the crack visual is accepted,
  F-M14). A creative break attempt is cancelled and the block resyncs.
- **M-REMOVE**: `remove` clears the marks, and the wall digs normally again.
- **M-SERVER-LIST** (control, as MS6): no display entity in the server's entity list, with a positive control.
- **M-SECOND-PLAYER** (only if a second client is available): the second player sees their own patch, not the
  first's progress. **Otherwise recorded as NOT READ, not as passed.**
- **LAST STEP**: a clean `/stop`.

### 5.11 NUMBERS FOR BEN: NOTHING OPEN FOR SLICE 1 (2026-11-13)
**Every number slice 1 needs is now RULED.** This section shrank on 2026-11-13 once Ben's remaining numbers
arrived. The rulings are quoted in §1 and the mechanism values in §2:

| # | number | value | ruled by |
|---|---|---|---|
| N1 | the payout item | `minecraft:raw_iron` | Ben, *"raw iron"* |
| N2 | ore per spot | **2 to 4**, rolled per spot | Ben, *"2-4 per spot"* |
| N3 | the Small completion bonus | **5** | Ben, *"with a 5 completion bonus"* |
| N7 | spots per Small vein | **2 to 3** | Ben, *"yes"* |
| N8 | the Small vein's size | **3 wide × 2 high** | Ben, *"yes"* |
| N9, N10, N11 | edge margin 2 px; spot spacing 5 px; offsets 0.0005 / 0.001 | as listed | the seat (derived mechanism values) |

**DEFERRED by Ben (*"later"*), so not open for slice 1:** N4 (the gold-window length; the design doc proposes about
1.5 s = 30 ticks), N5 (its bonus) and N6 (any perfect-chain pay). Slice 1 builds no gold window and no gold art.

**Kept for that later slice, because it is arithmetic on ruled numbers and costs nothing to keep:** if the window
opens when a spot is revealed (my reading of the words, NOT confirmed), then at 1.2 the next full charge is at ticker
17, so a 30-tick window leaves 13 ticks (0.65 s) to land a charged hit. What opens the FIRST spot's window is
unanswered. `Patch` already records each spot's reveal tick and hit tick (§5.2).

**The non-number questions, all settled for slice 1:**
- a miss plays a sound (Ben, *"yes"*); an uncharged hit plays nothing (the seat's ruling 4);
- the pickaxe test is the `#minecraft:pickaxes` tag (the seat's ruling 7, until Ben says otherwise);
- the dev pack gets both the join toggle and the command (the seat's ruling 7);
- the miss and break sounds are PLACEHOLDER keys in the ore file, and S1-a carries their schema (the seat's ruling
  4). The break particle is a placeholder named in content, and its key joins the schema in S1-b, the slice that
  shows it. *(This corrects the morning's note that both the sound and the particle keys wait for S1-b.)*

### 5.12 S2: FOUR VEIN SIZES ROLLED PER PATCH, THE DENIED SOUND, THE MISS-SOUND COOLDOWN (2026-11-14)

Built on `feat/mining-s2` to the seat's S2 rulings 1–11 (§2), on Ben's words (d)–(g) and (b)–(c) (§1). Code + gate,
no boot. The gate is `docs/gates/GATE-mining-s2.md`.

**The words, changed.**
- **slot**: a place on a wall with a **MAXIMUM size**, the size `/rpg mining place <ore> <size>` names. Its
  footprint is that size's, from the same bottom-centre anchor (§5.1). Every footprint block is protected always.
- **patch**: one player's instance at a slot, which now **rolls its own size** when it is created (its first show,
  and every regrowth). It sits at the slot's anchor at its own size (`FaceFrame.withSize`), so it is centred on the
  slot and shares the slot's bottom row. Its patch pixels, its aim and its spots are in ITS frame.

**The sizes** (*design doc, relayed*; small RULED, §1). Spot ceilings executed in jshell, worst case (§5.2, F-A):

| size | blocks | pixels | spots | ceiling | weight (PROVISIONAL) | completion bonus |
|---|---|---|---|---|---|---|
| small | 3 × 2 | 48 × 32 | 2–3 | 20 | 50 | 5 (RULED 2026-11-13) |
| medium | 5 × 3 | 80 × 48 | 4–5 | 45 | 30 | 6 (Ben (e)) |
| large | 7 × 3 | 112 × 48 | 6–7 | 60 | 15 | 8 (Ben (e)) |
| extra_large | 7 × 4 | 112 × 64 | 8–9 | 84 | 5 | 10 (Ben (e)) |

**The fit rule and the roll (core `SizeRoll`, tests first).** A size is eligible at a slot when its width AND its
height are at most the maximum's, and the ore lists it. One draw from `[0, sum of the ELIGIBLE weights)`, walked
over the eligible sizes in the ore's authored order: renormalised by construction, because an ineligible size has
no slice of the draw. **NAME THE QUANTITY:** a relative integer weight, at least 1 (`OreDefinition` refuses 0); a
size's chance at a slot is its weight over the eligible sum. For iron, by slot maximum (exact fractions):

| slot maximum | eligible | draw range | chances |
|---|---|---|---|
| small | small | [0, 50) | small 1 |
| medium | small, medium | [0, 80) | 50/80, 30/80 |
| large | small, medium, large | [0, 95) | 50/95, 30/95, 15/95 |
| extra_large | all four | [0, 100) | 50, 30, 15, 5 in 100 |

large (7 × 3) fits extra_large (7 × 4); extra_large does not fit large (the height); large does not fit medium (the
width). `SizeRollTest` has a row where each half of the rule is the only half that refuses.

**The outside-the-patch rule (the seat's ruling 4).** `MiningBook.click` takes the clicked block. A slot block
outside the current patch's footprint returns NO outcome: nothing changes, no sound, and the trace logs
`MINING click-outside player=P slot=ID size=S block=x,y,z tick=t`. **My reading of the ruling's "(or its face while
the patch is regrowing)":** while regrowing there is no current patch, so the whole slot face counts as "the patch",
and every slot block is judged `NOT_LIVE` as in slice 1 (no outcome change, nothing played). The other reading, that
a regrowing slot judges every click as nothing, would remove `NOT_LIVE`; it is the seat's to correct (report).

**Payout (the seat's ruling 5), executed in jshell over each size's spot range** (`per_spot` 2..4, Ben (g)):

| size | min spots → min | max spots → max | range per break |
|---|---|---|---|
| small | 2 → 2·2+5 = 9 | 3 → 3·4+5 = 17 | **9..17** |
| medium | 4 → 4·2+6 = 14 | 5 → 5·4+6 = 26 | **14..26** |
| large | 6 → 6·2+8 = 20 | 7 → 7·4+8 = 36 | **20..36** |
| extra_large | 8 → 8·2+10 = 26 | 9 → 9·4+10 = 46 | **26..46** |

Both ends of each are pinned by `PayoutTest` (a scripted min and max generator), and a seeded sweep of 400,000
breaks per size stays inside each range and reaches both ends. The bonus is the ROLLED size's, not the slot's
maximum's (`MiningBookTest` stages a medium patch in an extra_large slot: bonus 6, not 10 or 5). Regrowth stays 1200
ticks for every size (Ben (f)).

**The art (ruling 6).** `models.patch` is a map, one per size: `rpg:mining/iron_patch_{small,medium,large,extra_large}`.
Slice 1's `iron_patch` is renamed `iron_patch_small` (the same PNG). The three new textures are PLACEHOLDERS, the
small texture tiled to 80 × 48, 112 × 48 and 112 × 64 (16 px per block, `MiningPackTest` reads each size's
dimensions from the shipped YAML). The pack is 19 files. **Its sha1 is `49f2e465b2b6bbb24380425830c8572f14acd11c`,
8227 bytes**, computed from the committed blobs by a stand-alone copy of `MiningPack.build`. Control: the same
program over master `6470ee8d`'s pack gives slice 1's `1e11f1c1…` and 3225 bytes.

**The roll command (ruling 7).** `/rpg mining roll <size>`, inside the `/rpg mining` branch whose next line requires
`Permissions.DEV`. It replaces the caller's patch at the looked-at slot with a fresh one of that size (no size draw)
and ends that slot's regrowth for them. It refuses an unloaded size, a size the ore does not list, and a size that
does not fit, naming the reason. It logs ALWAYS: `MINING roll player=P slot=ID size=S spots=n`, or
`MINING roll-refused player=P reason=TEXT`.

**The DENIED sound (ruling 8).** `sounds.denied: minecraft:entity.villager.no` in iron.yml, PLACEHOLDER (Ben (b)).
`deny` asks `ActionBarHolds.isLive(player, tick)` BEFORE the new hold replaces the old one, and plays the sound only
when no hold was live. The hold line gains `sound=yes|no`.

**The miss-sound cooldown (ruling 9).** `MiningNotices.MISS_SOUND_COOLDOWN_TICKS` = 8 server ticks (Ben (c), 0.4 s),
per player, counted from the last miss sound that PLAYED (`MissSoundCooldown`): a miss at exactly 8 ticks plays; at
7 it does not; a silent miss does not restart the count. The click line gains `miss_sound=yes|no` on a MISS, `-`
otherwise.

**The trace lines, as changed** (all pinned by `MiningTraceLineTest`):
- `MINING click … tick=t miss_sound=yes|no|-`
- `MINING show player=P slot=ID size=S spots=… live=i/n tick=t`
- `MINING break player=P slot=ID size=S payout=… rolls=… bonus=b kept=k dropped=d regrow_at=r`
- `MINING regrow player=P slot=ID size=S spots=n tick=t`
- `MINING hold player=P reason=denied start=s until=u sound=yes|no`
- `MINING place slot=ID ore=O max_size=S footprint=… by=P` (was `size=`)
- new: `click-outside`, `roll`, `roll-refused` (above).

**HOW ORE AND STYLE WOULD BE ROLLED LATER (ruling 10, Ben (d): "random sizes and ores and types"). Nothing of it is
built.**
- **Ore.** Today a slot names its ore. Later a slot would name an ORE TABLE (ore id → weight), and `MiningBook.patch`
  would draw the ore first, then the size from THAT ore's weights restricted to the slot's maximum, exactly as now.
  The patch would carry its ore, as it now carries its size, and everything that reads `slot.oreId()` would read
  the patch's ore instead (the payout, models and sounds already come from an `OreDefinition` passed in). Nothing
  here has to be undone: the size roll already takes the ore as an argument.
- **Style (minigame type).** Trail is the only style; `Patch` is its state machine. A later style would be a second
  implementation behind a sealed interface over `click`, chosen by a third draw (or an ore's or area's style
  table) at the same creation point. The slot, the fit rule and the size roll do not change; the trace's `size=`
  field would gain a `style=` neighbour.
- **The one creation point** is `MiningBook.patch` (and `force` for the dev command). Every roll lives there, so
  adding a draw is one place.

**What S2 does NOT build:** ore or style rolling; any change to Trail; gold windows; areas; persistence; a
non-vanilla pickaxe; the dig-stop. The weights are PROVISIONAL and the art is placeholder.

---

## 6. FINDINGS

- **F-M1 (the brief, the seat's error, accepted 2026-11-12):** `-Drpg.dev=true` is not on master. It is
  `SelfTestGuard` on the parked feat/selftest. Ruled: the spike uses `Permissions.DEV` only.
- **F-M2 (the brief, accepted):** on 26.1 an attack is `ServerboundAttackPacket` (`Play.Client.ATTACK`), not
  INTERACT_ENTITY. `ExampleTelegraphListener`'s filter would never see a hit on a fake Interaction.
- **F-M3 (the brief, accepted):** "the cooldown read on the main thread" after a packet hop can read AFTER the
  swing's reset (`handleAnimate` resets at 340). A SWING-packet line's cooldown can be the floor value (0.0250–0.1000
  above), whatever the charge was. The spike also logs from the pre-reset events, `PlayerArmSwingEvent` and
  `PlayerUseUnknownEntityEvent`.
- **F-M4 (the brief, accepted):** a 2 × 3 block model cannot be one cuboid, because model elements are bounded to
  −16..32 in each axis, three blocks at most, and the pack wants room. The patch is a 1 × 1 flat model scaled
  (2, 3, 1) by the display's scale (index 12). The spot is a 1 × 1 model scaled to 3/16. *The −16..32 bound is client
  model-loader behaviour, not readable from the server jar: stated as the reason for the choice, not as a measured
  fact.*
- **F-M5:** cancelling `BlockDamageEvent` does not stop every break route. A creative START breaks at
  `handleBlockBreakAction` 256, before the event, and a STOP on a block left in `destroyPos` by an earlier
  uncancelled dig can still break it (F2b). The area rule must cancel `BlockBreakEvent` too.
- **F-M6:** ADVENTURE never reaches `BlockDamageEvent` (F2). If custom areas are ever run in ADVENTURE, the no-break
  rule is enforced by vanilla (`blockActionRestricted`) and the event listener is dead code there. The input side
  changes too: a dig START sends a block update. The gate is SURVIVAL on every row, and that choice is recorded here.
- **F-M7:** packet-only entity ids must not collide with the damage popups'. `PopupEntityIds` counts down from
  `Integer.MAX_VALUE`, so the spike counts down from `1_500_000_000`. Slice 1 needs one shared allocator, not two
  ranges chosen by eye.
- **F-M8:** a SURVIVAL dig START on the patch block still runs `BlockState.attack` before `BlockDamageEvent` (F2).
  For a block with attack behaviour, a cancelled dig is not silent. `javap -p` shows `RedStoneOreBlock` and
  `NoteBlock` each declare `attack(BlockState, Level, BlockPos, Player)`, and `RotatedPillarBlock` and `Block`
  declare none. Deepslate is `register("deepslate", RotatedPillarBlock::new, …strength(3.0f, 6.0f)…)` (`javap -c -v
  Blocks`: the bootstrap is `RotatedPillarBlock."<init>"`), so it is the gate's test block. Patch
  blocks must be chosen from blocks with no `attack` override, or the area rule must cancel earlier, at
  `PlayerInteractEvent` LEFT_CLICK_BLOCK.

**Found while building the spike (2026-11-12, spike head `1dbcc156`), recorded here because the spike does not
merge:**

- **F-M9 (instrument):** `scripts/check-crlf.sh` has no binary exclusion, and the spike adds the repo's first image
  files.
  - It sweeps every file from `git ls-files --cached --others --exclude-standard` and reported
    `1 MIXED: …/mining_spot.png CR=3 LF=2`. A PNG's 8-byte signature is `\x89 P N G \r \n \x1a \n` (`od -c` on the
    file), so every PNG carries a CR and an LF by definition. **The verdict is a false MIXED. "Normalising" the file,
    as the script advises, would corrupt the image.**
  - Git itself classifies both PNGs correctly: `git ls-files --eol` reads `i/-text w/-text`, and
    `git show HEAD:<png> | cmp - <png>` is silent for both.
  - **Before any pack lands on master:** either `check-crlf.sh` skips files whose eol class is `-text`, or
    `.gitattributes` gains `*.png binary` and the script honours it. That is an instrument change, so it is a
    ruling, not done in the spike.
- **F-M10 (for candidate A):** in SURVIVAL, `handleAnimate` fires **no** `PlayerInteractEvent` LEFT_CLICK_BLOCK.
  - It fires LEFT_CLICK_BLOCK only `if gameMode == ADVENTURE` (bytecode 158 `if_acmpne 218`), and LEFT_CLICK_AIR
    only when its own server ray hits nothing (114 `ifnonnull 144`, then 121–137).
  - So a survival block click's LEFT_CLICK_BLOCK comes from the **dig START** (F2 step 4), not from the swing.
  - **Under A the server's hit test must be its own ray at `PlayerArmSwingEvent`**, which is what the spike's `aim`
    field does. No vanilla event carries the swing's block hit in survival.
- **F-M11:** the spike's zip is deterministic per working tree, not per commit. The JSON files are text, so a CRLF
  checkout and an LF checkout build different bytes and different SHA-1s. Harmless for a dev offer. **A shipped pack
  must be built from the blobs (or with `eol=lf` pinned on `resourcepack/**`)**, or two seats publish two hashes for
  one commit.

**Found by spike 1's boot (2026-11-12, build `1dbcc156`, readings at `11e94e2c`; accepted by the seat 2026-11-13).**
The gate's READING blocks are the account; these are the pointers.

- **F-M6 RESOLVED 2026-11-13:** Ben ruled *"No players will never be in adventure mode."* The ADVENTURE facts stand;
  ADVENTURE is not a design route, and every mining gate stays SURVIVAL.
- **F-M12: a block click carries TWO arm swings.** On every block click, two `PlayerArmSwingEvent`s fire in the same
  tick. The first reads before the reset (1.0000 on a charged click); the second reads `0.0300` (Δ=0 at 1.2),
  because the first just reset the ticker (MS1). A click on the INTERACTION box carried ONE (MS2). **So any read site
  that fires per swing must take the first and ignore the second.** §5 removes the question by reading at
  `BLOCK_DAMAGE`.
- **F-M13: on the box, ATTACK precedes SWING**, on all 8 box attacks, and `PlayerUseUnknownEntityEvent` read 1.0000
  each time (MS2, MS3(b)). Recorded for the record. B is not built.
- **F-M14: holding left-click on the patch block.** The client sends one `DIG_START`, then a `DIG_STOP` at +16 ticks
  and again every 20 or 21 ticks (MS3: ticks 9902, then 9918, 9938, 9959), and never a second `DIG_START`. Ben saw the
  deepslate *"cracked flickered vanished and came back. but only when not mining the emerald"* (MS4, quoted, not
  interpreted). The server never broke it: zero `BLOCK_BREAK` lines. **Ben accepted this as a client visual and
  deferred stopping it as polish** (§1; F7a is the candidate).
  > **2026-11-14: the same client dig is also HEARD** (F-M33): the client plays its own block dig sound on every
  > click on the wall. Stopping the dig (F7) would stop both.
- **F-M15: the spike pickaxe's tooltip showed Attack Speed ABOVE Attack Damage**, the reverse of vanilla (MS5).
  - The cause: `MineSpike.givePick` collected the modifiers in Guava's `ArrayListMultimap`, whose keys live in a
    `HashMap` (Guava 33.5.0: `Platform.newHashMapWithExpectedSize` → `Maps.newHashMapWithExpectedSize`).
    `CraftMetaItem.setAttributeModifiers` and `applyModifiers` copy `entries()` in that map's order, and the tooltip
    prints in component order.
  - The fix, for the slice that gives `ToolItems.mint` its modifier: an insertion-ordered multimap
    (`MultimapBuilder.linkedHashKeys().arrayListValues()`), filled damage first, then speed, plus a test pinning the
    order. Ben: *"yes for now"* to vanilla order.
- **F-M16: the next boot of the PARKED stack must also use `--refresh-content`.** The spike boot found
  `run/plugins/Rpg/content` holding the STACK's content (16 weapons, plus sunder, deft_hands, withering, melee.yml),
  because `saveResource` never overwrites. That boot refreshed it to master-plus-spike content (14 weapons). A stack
  boot without the flag would load master's content under the stack's code. The backup taken before the boot is
  `C:\Users\Neb91\IdeaProjects\rpg-run-backups\2026-11-12-pre-mining-spike`.
- **F-M17: my mid-boot warning was WRONG.** During the boot I told Ben the pack download would fail, because the
  HTTP server binds 127.0.0.1 and his client connected over the LAN address. It did not fail: the client runs on the
  server's machine, so 127.0.0.1 reached the server, and the fifth offer went ACCEPTED → DOWNLOADED →
  SUCCESSFULLY_LOADED (MS7). Recorded as wrong in the gate.
- **F-M19 (the seat's F-A, 2026-11-13): `VeinSize`'s spot ceiling overstated its guarantee.** It refused
  `spots.max` only above the perfect-tiling count, but `SpotLayout` places at random. The fix is on S1-a
  (`e7308aa4`): the ceiling is the worst-case count, `ceil(aw/9) × ceil(ah/9)` (§5.2). Small is unaffected (3 ≤ 20).
- **F-M20 (the seat's F-B, 2026-11-13): S1-a changes the `Loaded` line, and the PARKED stack's gates predict the old
  line verbatim.** S1-a appends `…, N ores, M vein sizes` after `N recipes`. Read on 2026-11-13, read-only, from the
  parked tips on the wire (`git diff --name-only origin/master...origin/<branch> -- docs/gates`, then
  `grep -n "Loaded [0-9]* abilities"` in each). **Six gate files** on the stack carry a verbatim `Loaded …` R0c:
  `GATE-legacy-b.md` (L144), `GATE-wither-status.md` (L164), `GATE-withered-shortbow.md` (L157),
  `GATE-selftest.md` (L121 on feat/selftest, L125 above it), `GATE-sunder.md` (L140 on feat/sunder, L142 above it)
  and `GATE-deft-hands.md` (L121), which ends `… 1 mobs, 3 recipes`. `docs/dragon-mobs` adds no gate.
  - **When the stack is rebased onto a master that has S1-a, each such R0c needs a dated re-prediction BEFORE its
    boot**, appending `, 1 ores, 1 vein sizes` (or whatever counts that master ships), with the old prediction
    kept beside it.
  - The parked branches are NOT touched now (the standing constraint).
- **F-M18: the patch model's on-screen SIZE, FACING and POSITION were NOT STATED in spike 1.** Ben described the
  accepted look only as *"an orange square with a gray background"* (MS7). Slice 1's gate reads them. **Note:** the
  spike scaled its patch (2, 3, 1), 2 wide × 3 tall, while the Small vein is 2 high × 3 wide (§1). Slice 1's patch
  model and scale follow the Small vein, not the spike.
- **F-M21 (the seat's D1, 2026-11-13): regrowth never showed after a relog.** The break's task ran on the breaking
  Player's entity scheduler with a null retired callback, and a quit retires that scheduler (F8c), so the task was
  dropped. On rejoin the chunk load reached `show`, which found the patch regrowing and returned; nothing re-armed
  the respawn. §5.4's *"the join or chunk-load path renders from the clock"* was false, the seat approved it, and the
  gate's M-LOGOUT predicted the opposite of the code. **Fixed on S1-b (`7c16235b`)**: §5.4's correction. Core
  `RegrowthTasks` and `MiningBook.claimRegrowthTask` (tests first; the first run was a compile failure rather than a
  behavioural red), wired in `show`, `onBroke` and `onQuit`; six mutations, all red. The gate gains M-RELOG (the
  chunk-load path, ruling 4), and M-LOGOUT is VOID if the wall is clicked before `regrow_at`.
- **F-M22 (the seat's D2, 2026-11-13): a stale `shown` after a respawn or a dimension change. NOT A DEFECT, from
  the jar.** F8a/F8b: both paths fire `PlayerChunkUnloadEvent` for every sent chunk, naming the old world, and then
  `PlayerChunkLoadEvent` again, so `shown` is cleared and rebuilt. No code changed. The gate's M-RESPAWN reads the
  dimension half via the nether roof; the respawn half is NOT READ, because staging it needs a death and Ben's
  inventory holds his Ability Stone and Nexus star.
- **F-M23: `PopupEntityIds` keeps its popup name and package (`paper/health`) although mining now shares it.** §5.4
  said it would move to a shared counter; S1-b made it public and shared it, and left the name to keep the change
  minimal (`PopupEntityIdsTest` unchanged). The seat accepted it, 2026-11-13. A cosmetic debt: a rename to a
  neutral name in a neutral package, in its own change.
- **F-M24 (the seat's G1, 2026-11-13): the gate as first written spent more spots than a Small patch has.** M-HIT,
  M-UNCHARGED (its first click was a HIT), M-CHARGE-17 (ten charged clicks on the live spot), M-HOLD and M-DENY step
  3 needed far more than the n − 1 = 1 or 2 HITs a patch holds before it breaks, so it would have broken mid-row
  into a 60-second regrowth. Amended before any boot: M-UNCHARGED and M-CHARGE-17 read on clicks beside the spot,
  and a standing rule makes an early break M-BREAK's reading.

**From the slice 1 boot, 2026-11-14** (`863bf10a`; readings `9eb45815`, addendum `a95e56f6`, on `feat/mining-s1b`;
`L<n>` is a line of that boot's `run/logs/latest.log`):

- **F-M25: A RUN-SHEET COMMAND WITH A PLACEHOLDER IN IT GETS TYPED LITERALLY, AND THIS ONE COST A DEATH.** The sheet
  gave M-RESPAWN's way back as `/execute in minecraft:overworld run tp @s <x> <y> <z−3> 0 0`. Ben typed it as
  written (L225); it did nothing, and he left the nether with `/kill` (L226–L228), the one thing the staging said
  must not happen. **The rule: every command on a run sheet is the literal command, filled in by whoever writes the
  sheet from a reading already in hand** (the slot id is in the `MINING place` line), never a template. Where the
  value is not known until the run, the sheet says to ask for it. **What the player data showed** (read-only,
  `run/world/players/data/<uuid>.dat` against the 2026-11-14 backup): the Ability Stone (`rpg:build_stone`) and the
  Nexus star (`rpg:nexus`) are still in Ben's inventory; **five iron pickaxes present before the run (four named, with
  attribute modifiers, and one plain) are not.** Whether the death dropped them is UNREAD. Ben: *"questions 1-4 not
  sure and skip"*. Nothing is restored or minted (the seat).
- **F-M26: THE DEV PACK IS NOT RE-OFFERED AT LOGIN WHILE THE TOGGLE IS OFF, SO EVERY RECONNECT DROPS THE MODELS.**
  After each reconnect the patch drew as *"purple and black squares"* (Ben) until `/rpg mining pack` ran again. That
  is the designed behaviour with `mining.dev-pack-on-join: false` (M-PACK predicted no offer at LOGIN), and the gate's
  relog rows did not account for it (the seat's miss). M-RELOG's show at 05:36:39 and M-LOGOUT's return at 05:38:11
  reached a client without the models. **Before the next mining boot, Ben's local `run/plugins/Rpg/config.yml` gets
  `mining.dev-pack-on-join: true`** (his file, not the repo default), which also lets the toggle's ON half be read.
- **F-M27 (the seat's ruling 1, 2026-11-14): A HELD BUTTON RESTARTS THE DIG; THE CHARGE RULE, NOT THE EVENT COUNT,
  KEEPS A HOLD FROM MINING.** §5.4's "fires once per click by construction" was false (dated note there): M-HOLD logged
  two click lines and M-PROTECT three. The restarted digs read `0.1500`, `0.0900`, `0.2700`: 2, 1 and 4 ticks after a
  reset at vanilla's 16.67-tick delay. **The fast-pickaxe consequence, to be solved before any pickaxe with a
  non-vanilla attack speed ships (S1-T, §5.7):** if a held dig restarts after a gap of about 4 ticks, a pickaxe whose
  full-charge delay is that short reads FULL on a held button and mines while held. **The client's restart gap must
  be established from a reading when that slice is scoped, not from memory**; the three numbers above are ticks
  since the last reset, not a measured restart interval. M-HOLD's `0.8100` restart (13 ticks) stays UNREAD.
  > **THE FIRST READING, 2026-11-14 (S1-c's boot; the seat's ruling 6).** A held button SWEPT across the patch
  > produced **72 MISS lines in 246 ticks, gaps mostly 1 to 4 ticks** (1 tick 5 times, 2 ticks 37 times, 3 ticks 12
  > times, 4 ticks 8 times), and **65 of the 72 cooldowns equal `(gap + 0.5) / 16.666666` exactly** (the other 7 read
  > lower). So a held button moving across blocks restarts the dig as often as every 1 to 2 ticks, each restart far
  > below `1.0` at attack speed 1.2. **The restart gap is far shorter than any plausible pickaxe delay, so the open
  > question for a fast pickaxe is what a held button reads when it stays on ONE block.** Still to be read when S1-T
  > is scoped.
- **F-M28: THE DENIED ACTION BAR IS OVERWRITTEN BY THE HUD BEFORE IT CAN BE READ (a DEFECT, awaiting Ben).** Ben, Q14:
  *"Yes but the HP/defense/Mana overode it faster than it could be read"*. `hud/StatsBarSystem` redraws each
  player's action bar every **10 ticks** (`BAR_PERIOD_TICKS`) on a per-player repeating task, and **nothing in the repo
  lets another message hold the bar** (read 2026-11-14). `StoneNotice`'s javadoc states the same collision and says
  `QuiverNotice` lives with it; their gate rows read that the line is visible, not for how long. **Where the
  "You need a pickaxe" message goes is Ben's ruling, asked by the seat and not yet given.** Not fixed.
  > **RULED AND BUILT ON S1-c, 2026-11-14.** Ben: *"Is it possible to have the pickaxe message over ride the action
  > bar for 1.5s"*; the seat answered yes. `core.hud.ActionBarHolds` holds a player's bar until a tick, and the
  > stats bar re-sends the held message on each beat inside the hold; DENIED holds it for
  > `MiningNotices.DENIED_HOLD_TICKS` = 30 server ticks. Read by `GATE-mining-s1c.md`'s C-DENY-HOLD.
- **F-M29 (S1-c, 2026-11-14): `StoneNotice` and `QuiverNotice` keep the overwrite.** They write the same action bar
  and are overwritten the same way, and **neither uses the hold**, because Ben has not ruled on them (the seat's
  ruling 4 for S1-c). `StoneNotice`'s javadoc now points at `ActionBarHolds`. A hold is one call per notice
  when he does.
- **F-M30 (S1-c, 2026-11-14): A CORRECTION TO F-M25. THE DEATH COULD NOT HAVE DROPPED THE PICKAXES.**
  `RpgListeners.onPlayerDeath` sets `keepInventory`, clears the drops and keeps the level, for every death (its
  javadoc: *"Death is a setback, not a loot loss: keep inventory + XP, drop nothing"*). So the five iron pickaxes
  missing after slice 1's boot did not leave by the `/kill`; their cause stays UNREAD. F-M25's placeholder rule is
  unaffected. **The same handler bears on the carried respawn rows:** a death here keeps the Ability Stone and the
  Nexus star. Whether that makes a staged death acceptable is the seat's and Ben's call; S1-c's gate does not stage
  one (the brief).
- **F-M31 (S1-c, 2026-11-14): slice 1's gate predicted the duplicate-placement refusal with a full stop the code
  does not print.** `SlotRegistry.place` throws `"a slot already exists at " + slot.id()`, no full stop; M-PLACE's
  prediction ended `… at <id>."`. Its chat text was never read, so nothing was recorded against it. S1-c's gate
  predicts the code's text, now a log reading (`MINING place-refused`).

**From the S1-c boot, 2026-11-14** (`0578366d`; readings `0574e940` on `feat/mining-s1c`; the seat's read of the
same day):

- **F-M32 (the seat's ruling 1): THE SOUND ROWS COUNT.** S1-c's precondition (Q3 names the `/playsound` bell) was
  not met in its words (Ben: *"I don't likeit"*), but Q9 to Q12 tell the sounds apart in his own words, which shows
  the audio was on better than a name would. **Carried rows 1, 2 and 3 are READ:** the miss sound plays; an
  UNCHARGED click in the box plays nothing of ours (*"The miss sound didn't play"*); the break sound plays.
- **F-M33 (the seat's ruling 2; beside F-M14 and F7): THE CLIENT PLAYS ITS OWN DIG SOUND ON EVERY CLICK ON THE
  WALL.** Every click starts a client dig (spike 1's MS4, F-M14), and the client plays the block's dig sound for it.
  So every sound COUNT in S1-c's gate was one too low per click; the seat's miss as well as the gate's. Ben ruled
  the client dig cosmetic and deferred the dig-stop (§1, F7); this is the same thing heard instead of seen.
- **F-M34 (the seat's ruling 3): C-DENY-HOLD PASS.** The hold is read in game: 13 DENIED clicks, 13 holds of exactly
  30 ticks, 6 releases 0 to 8 ticks after their end, and no release for any of the 7 replaced holds. **Carried row
  7, the join toggle ON, is READ** (the pack offered at both LOGINs with no command, slice 1's sha1).
- **F-M35 (the seat's ruling 5): set-up step 1 was not run in S1-c's boot**, so SURVIVAL was not set by command. It
  is inferred from the click lines: `BlockDamageEvent` is not reached in ADVENTURE (F2), and a creative START breaks
  first (F2b). Recorded as inferred.
- **F-M36 (the seat's ruling 7): TWO DESIGN REQUESTS ARE WITH BEN, NOT BUILT.** (a) A sound with the DENIED message
  (Ben, Q6: *"we should add a sound to go with it"*). (b) Whether the miss sound should play only on a FULLY CHARGED
  miss: the held sweep (F-M27's note) played it 72 times in 246 ticks. Both await Ben's ruling.
- **S1-c's rows NOT RUN** (Ben stopped after C-BREAK-SOUND) are NOT READ and carried below; Ben's Q14 to Q21 answers
  stay recorded verbatim in the gate and bound to no reading. They do not block S1-c's merge: every path is behind
  `Permissions.DEV` (the seat's ruling 4).

**From S2's build, 2026-11-14** (`feat/mining-s2`, no boot):

- **F-M37: the brief's "(or its face while the patch is regrowing)" has two readings, and S2 builds one.** Built:
  while regrowing, the whole slot face is "the patch", so every slot block is `NOT_LIVE` as in slice 1. Not built:
  every click on a regrowing slot is judged as nothing (no `NOT_LIVE` at all). §5.12 states it; the seat's to
  correct.
- **F-M38: "several regrows" is staged as remove-then-place, not break-then-regrow.** A medium slot's patch holds 4
  or 5 spots, so one real regrowth costs 4 to 5 charged clicks and a `/rpg mining regrow`; twenty-one of them would
  cost about 100 clicks. `remove` clears the slot's patches and `place` shows a fresh one through the same
  `MiningBook.patch` creation point that regrowth uses, so each pair is one roll. The real regrowth path is read on
  its own row. The gate says why it reads 21 (`GATE-mining-s2.md`, S2-MEDIUM-CAP).
- **F-M39: a `NOT_LIVE` line's aim is in the SLOT's frame**, because a regrowing slot has no patch to aim in. Every
  other click line's aim is in the PATCH's frame. A gate comparing aims across a regrowth compares two frames.
- **F-M40: the order "ask `isLive`, then replace the hold" is pinned by a source-order signature test and was NOT
  mutation-checked.** Its guard (`if (noLiveHold)`) and `isLive`'s boundary were.

**From the S2 boot, 2026-11-14** (`03bd1bf0`; readings in `GATE-mining-s2.md` on `feat/mining-s2`):

- **F-M41: THE SEAT'S RULINGS ON S2'S POINTS, 2026-11-14.** (1) A regrowing slot judges every block `NOT_LIVE`:
  accepted as built (F-M37). (2) S2-MEDIUM-CAP at 5 placements, a weak witness; then, before the run sheet, NOT RUN.
  (3) "Ore does not list size" unit-tested only, NOT READ: accepted. (4) **`sounds.denied` in `iron.yml` is accepted
  for now, but it is not really an ore's property** (the DENIED text is one value in code, `MiningNotices`), **and may
  move when a second ore exists.** (5) The three trace format changes: accepted. Also by ruling: S2-SIZE-LARGE and
  S2-SIZE-XL roll and sight only (break halves NOT RUN), S2-SECOND-PLAYER NOT READ unless a second client exists.
- **F-M42: S2 READ.** Every rule built was read in game: the size roll (a real regrowth rolled medium after a small
  broke), each forced size at its footprint (Ben: yes, yes for all four), the payout rule following the rolled size
  (medium's bonus 6 at an extra_large slot), the outside-the-patch rule (8 `click-outside` lines on three blocks, no
  outcome), the DENIED sound once per message (5 holds, `sound=` exactly when no hold was live), and the miss-sound
  cooldown on 14 MISS lines **including a gap of exactly 8 ticks reading `yes`**. Pack `49f2e465…`/8227 as predicted.
- **F-M43: SET-UP STEP 1 WAS SKIPPED AGAIN** (as F-M35): no `/gamemode survival` in the log, so SURVIVAL is inferred
  from the click lines a second time. Twice is a pattern: the next run sheet should put the mode command FIRST and
  read it before any other step.
- **F-M44 (instrument): MY FIRST R0 READ WAS OF THE PREVIOUS BOOT.** A wait loop on `Done (` in `latest.log` matched
  S1-c's log before the new server had rotated it; the jar scan ran before the new jar was deployed. Caught by the
  `Build:` hash (`0578366d`) and the log's mtime, discarded before Ben logged in, and re-read on this boot. **A
  readiness wait must bind to the boot it waits for** (a fresh mtime and the expected `Build:` hash), not to a
  line that any previous boot also printed.
- **S2's rows NOT RUN** (Ben stopped after step 13's `remove`): S2-ROLL-REFUSED's two texts, S2-RESPAWN and
  S2-PACK-AFTER-DEATH. Ben's Q21 ("not sure", NOT STATED) and Q22 ("yes") were given before those rows ran and are
  bound to no reading.

**From the seat's read of the S2 readings, 2026-11-14** (read from origin at `37534ac2`: ACCEPTED; #186 approved to
merge, only on Ben's own words):

- **F-M45: THE SEAT'S RULINGS ON THE S2 READINGS.**
  1. **PASS as recorded:** R0a–c, S2-PACK-JOIN, S2-PLACE-XL, S2-SIZE-SMALL, S2-SIZE-MEDIUM, the roll and sight halves
     of S2-SIZE-LARGE and S2-SIZE-XL, S2-DENIED-SOUND, S2-MISS-COOLDOWN, S2-REGROW, LAST. The large patch's break, read
     out of the steps, is accepted as an extra reading and stays recorded that way.
  2. **S2-OUTSIDE: the rule is READ** (eight lines, three blocks, no outcome, no sound, the control heard). "Which
     block" is NOT AS PREDICTED and does not matter to the rule.
  3. **SURVIVAL is inferred again** (F-M43). The run-sheet rule is F-M46, below.
  4. **NOT READ, never passes, carried:** S2-ROLL-REFUSED (both refusals), S2-RESPAWN, S2-PACK-AFTER-DEATH,
     S2-SECOND-PLAYER, S2-MEDIUM-CAP, and the extra_large break half. Q21 is NOT STATED; Q22 is bound to no reading.
     They do not block this merge: every path is behind `Permissions.DEV`, and the rules are unit-tested and
     mutation-checked.
  5. **F-M44 is accepted as recorded.** The build-line check caught it, which is what R0a is for.
- **F-M46 (the seat's ruling 3, 2026-11-14): A RUN SHEET'S FIRST LINE AFTER LOGIN IS `/gamemode survival`**, on its
  own, before anything else, **and the gate's first row reads it** in the log. Set-up step 1 was skipped in both the
  S1-c and the S2 boots (F-M35, F-M43), and both times SURVIVAL rested on an inference.

### CARRIED ROWS, 2026-11-14 (selftest): WHICH OF THE LIST BELOW `feat/mining-selftest` TURNS AUTO

The list below stands as written. `feat/mining-selftest` adds `docs/gates/GATE-mining-auto.md`, whose scenarios
`/rpg selftest` drives through the shipped `BlockDamageEvent` path. **An AUTO reading is a reading of the SERVER**
(`.claude/rules/verification.md`, *AN AUTO READING IS A READING OF THE SERVER*): a row with a sight or sound half
keeps that half for Ben. None of these is read until that gate boots; until then each stays **NOT READ**.

| carried row (the list below) | this branch | the gate row |
|---|---|---|
| 1. M-SECOND-PLAYER | **stays HAND**: a selftest refuses with two players online (`SelfTestGuard`) | — |
| 2. the same-world respawn re-shows the patch | **AUTO** (the log half: the same `size=`, `spots=`, `live=` after the respawn); the sight stays Ben's | A4 |
| 3. whether a death drops the server-offered pack | **split**: AUTO reads that no `MINING pack offer` or `pack status` line follows the death; **whether the art survives is Ben's sight** (one run-sheet question) | A4 |
| 4. `/rpg mining roll`'s two stageable refusals | **AUTO** | A1 |
| 5. S2-MEDIUM-CAP | **AUTO**, at the 21 placements `GATE-mining-s2.md` derived | A2 |
| 6. the extra_large break half | **AUTO** | A3 |

**Also recorded here, at the seat's instruction:** Ben's ruling of 2026-11-14 on the Deft Hands tooltip ("just Deft
Hands III") is already on master; this branch adds nothing to it.

### CARRIED HAND ROWS, 2026-11-14, AFTER THE S2 BOOT: THE NEXT MINING GATE MUST INCLUDE THESE

**This list SUPERSEDES the one below.** Each is **NOT READ** and **never a pass**:

1. **M-SECOND-PLAYER**: each player sees their own patch. Needs a second client.
2. **The same-world respawn** re-shows the patch. A death is stageable (F-M30).
3. **Whether a death and respawn drops a server-offered pack.**
4. **`/rpg mining roll`'s two stageable refusals as log lines** (S2-ROLL-REFUSED): a size that does not fit, and a
   size that is not loaded.
5. **S2-MEDIUM-CAP**: a medium slot showing only small and medium over several rolls (added 2026-11-14 by the seat's
   ruling 4 on the readings, F-M45).
6. **The extra_large break half**: a break of an extra_large patch with its payout inside 26..46 (added 2026-11-14 by
   the same ruling).

### CARRIED HAND ROWS, 2026-11-14, AFTER BEN'S (a) AND THE SEAT'S RULING ON IT: S2'S GATE INCLUDES THESE — SUPERSEDED 2026-11-14, AFTER THE S2 BOOT (the list above)

**This list SUPERSEDED the one below.** Rows 1 to 6 of the list below (the place refusal texts, the server-list
check, the full-inventory ground check, the relog and logout sights, the post-remove dig) are **CLOSED BY BEN'S
RULING 2026-11-14, NOT READ, never passes** (§2). Each row here is **NOT READ** and **never a pass**:

1. **M-SECOND-PLAYER**: each player sees their own patch. Needs a second client.
2. **The same-world respawn** re-shows the patch. A death is stageable: it keeps inventory and drops nothing (F-M30).
3. **Whether a death and respawn drops a server-offered pack.**

### CARRIED HAND ROWS, 2026-11-14, AFTER S1-c: THE NEXT MINING GATE MUST INCLUDE THESE (the seat's ruling 4) — SUPERSEDED 2026-11-14 (the list above)

**This list SUPERSEDED the one below** (kept as the record of what slice 1 carried). Each is **NOT READ** and
**never a pass**. Read since: rows 1, 2 and 3 of the old list (the sounds, F-M32) and row 7 (the toggle ON, F-M34).

1. **The three `/rpg mining place` refusals as log lines** (`MINING place-refused`; the line is unit-tested only):
   a top face, a footprint with a non-solid block, a duplicate slot.
2. **The displays are not server entities** (M-SERVER-LIST), in the log-carried form with its `/summon` control.
3. **The full-inventory ground check**: items on the ground before the clean-up, none after, both in the log.
4. **The relog of a part-mined patch** (C-RELOG-SIGHT): the log half (the same centres, `live=2/n`, no click
   before the show) AND **its sight half with the pack loaded**: the patch drawn with its art, the crack, one live
   spot.
5. **The logout** (C-LOGOUT-SIGHT): the log half (no show at LOGIN, show and regrow at exactly `regrow_at`, no click
   before) AND **its sight half with the pack loaded**: bare at first, back by itself, drawn with its art.
6. **The wall digs normally after `/rpg mining remove`** (C-REMOVE-DIG), both directions in the log.
7. **M-SECOND-PLAYER**: each player sees their own patch. Needs a second client.
8. **The same-world respawn** re-shows the patch. Needs a death (F-M30: a death keeps inventory).
9. **Whether a death and respawn drops a server-offered pack.** Needs a death.

### CARRIED HAND ROWS: THE NEXT MINING GATE MUST INCLUDE THESE (the seat, 2026-11-14) — SUPERSEDED 2026-11-14, AFTER S1-c (the list above)

Each is **NOT READ** in the slice 1 boot and is **never a pass**. They do not block slice 1's merge (the seat: the
slice is reachable only through `Permissions.DEV`, and the packet-only display mechanism was read with its control
in spike 1, MS6).

1. **The miss sound plays** to the clicker only (M-MISS; audio was off, Q11).
2. **An UNCHARGED click plays nothing** (M-UNCHARGED; Q13).
3. **The break sound plays** (M-BREAK; Q17).
4. **M-SERVER-LIST:** the displays are not server entities, with its `/summon` control (the results were not logged
   or relayed).
5. **M-SECOND-PLAYER:** each player sees their own patch (no second client).
6. **The same-world respawn** re-shows the patch (M-RESPAWN's respawn half).
7. **The join toggle's ON half:** `MINING pack offer` at LOGIN with `mining.dev-pack-on-join: true` (F-M26).
8. **M-PLACE's refusal texts** (steps 1, 2 and 4).
9. **M-FULL's final ground check** (`/execute if entity @e[type=minecraft:item,distance=..16]` reads "Test failed").
10. **The deepslate digs normally after `/rpg mining remove`.**
11. **Whether a death and respawn drops a server-offered pack** (the premise the M-RESPAWN addendum marks unchecked).
12. **The sight halves of M-RELOG and M-LOGOUT, read WITH the pack loaded** (the toggle ON allows it).
