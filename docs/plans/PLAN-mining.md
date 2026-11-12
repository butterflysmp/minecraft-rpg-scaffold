# PLAN — MINING: PHASE 0 (FACTS, THE INPUT QUESTION, SLICE 1'S SHAPE)

**Status: PLAN ONLY, NOT MERGEABLE YET.** Written 2026-11-12 against master **`55425aad`** (read from the wire:
`git ls-remote origin refs/heads/master` → `55425aadf907f8faa2f1c7707a4fa0f713a87a7e`). This branch,
`docs/mining-plan`, does not merge until the input spike's readings are folded into it and the seat has diffed it
(the seat's ruling, 2026-11-12).

**The spike** is `spike/mining-input`, also from `55425aad`. It will NOT merge. Its gate, `GATE-mining-spike.md`,
lives on that branch beside the code it tests, because the boot is bound to that branch's build sha. After its
readings, the read gate is copied byte for byte onto this branch with a dated note naming the spike sha it was read
at. The spike branch is not deleted until the seat has confirmed that the two blobs diff empty.

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

**From the planning round (2026-11-12):**
- **The spike's gate is `.requires(Permissions.DEV)` and nothing else.** There is no `-D` property, no change to
  `scripts/dev-server.sh`, and nothing is copied from feat/selftest. The brief named `-Drpg.dev=true`, but that
  property exists only on the parked feat/selftest branch (`SelfTestGuard`).
  - **Mining's real dev commands move to the `rpg.dev` guard once #175 has merged.**
- Where the docs live: see the status block above.

**From Ben's addendum, the AREA RULE is the target:** custom areas are no-break as a whole, so cancelling block
damage is a property of the AREA, not of a patch.
- **What is missing:** no custom area exists in the repo. Slice 1 needs (a) an area model, a core type that answers
  "is this block position in a custom area", with its source of truth still to be ruled, and (b) ONE listener
  consulting it for `BlockDamageEvent` and `BlockBreakEvent`.
- Until then, the spike cancels damage and break on the spiked block only.

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

---

## 5. SLICE 1

**Scope** (the brief): Trail style only (hit spots stay as cracks, the last spot shatters), one ore, no levels, no
pickaxe stats beyond attack speed, placed by a dev command on the wall the player looks at. Custom-model visuals
from the project pack.

**The module split:**
- **`core/`, no Bukkit:**
  - `mining/` holds the patch state machine: spots, which spot is live, the hit or miss rule given a charge and an
    input event, and the Trail-style crack and shatter states.
  - The face-box maths for A: a ray against a 5/16 square on a block face, in 1/16 units.
  - The regrowth timers: a `Map<UUID, …>` in memory, a deadline per player per slot, kept on logout, lost on restart.
  - The size and spot re-roll on regrowth, seeded and testable.
- **`content/`:** one ore YAML (`content/mining/ores/<id>.yml`) carrying the drop, the regrowth ticks, the spot
  count range, the patch size range and the model ids. A loader in the `AbilityLoader` tradition is the only class
  that knows the schema.
- **`paper/`:** the packet visuals (ITEM_DISPLAY with the item_model, plus INTERACTION if B wins), the input
  listener (the winning candidate's packet or event), the dev placement command, the BlockDamage/BlockBreak
  cancellation (per patch block in slice 1, the area rule once areas exist), the pickaxe check ("denied if not
  holding a pickaxe"), and the pack offer.

**WHAT SLICE 1 DOES NOT BUILD:**
- mining levels, and the XP curve below (recorded, not built);
- Fortune, level rewards, recipes, and combat-gear requirements (Ben deferred these);
- `PlayerProfile` persistence;
- custom areas and the area no-break rule (the target, §2);
- Unstable veins (3–5 spots: recorded only);
- vanilla-variant ores;
- natural placement (slot authoring beyond the dev command).

### MINING XP: THE PLAYER LEVEL CURVE DIVIDED BY 10 (not built in slice 1)
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

### TOOLS: ATTACK SPEED ON A CUSTOM PICKAXE, UNDER NAME THE QUANTITY
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

### THE RESOURCE PACK
- **Source path: `paper/src/main/resources/resourcepack/`.** It ships inside the plugin jar as a resource, so there
  is no new Maven plugin and no separate artefact.
- **Build:** the plugin zips that directory from its own jar in memory, in a fixed entry order with fixed
  timestamps, so the SHA-1 is stable across builds of the same sources. The spike's `MineSpikePack` does this, and
  `MineSpikePackTest` pins it.
- **The dev offer** (spike only, `Permissions.DEV`):
  - On first use, a `com.sun.net.httpserver.HttpServer` **bound to 127.0.0.1 only** serves the zip. It is stopped
    on plugin disable.
  - The player is sent `sendResourcePacks(ResourcePackRequest)` with that URI and the SHA-1 hex, `required(false)`.
  - **This assumes the client runs on the server's machine.** The gate checks it.
- **Shipping is NOT this.** A shipped pack needs a public HTTPS host and either `server.properties`
  `resource-pack`/`-sha1` or a join-time request. That host is an operator decision, and is open.

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
