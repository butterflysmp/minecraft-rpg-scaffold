# GATE — The anvil screen (Slice 13a)

**Status: NOT RUN.** Every prediction below was written BEFORE any boot, and **no prediction is
edited once a row has been read.** Readings go in the `READ` cell beside the prediction they answer,
never over it.

```
NOT RUN   22   R0 R1 R2 R3 R4 R5 R6 R7 R8 R9 R10 R11 R12 R12b R13 R14 R15 R16 R17 R18 R19 R20
          ──
          22   = git grep -c '^### R' <ref> -- GATE-anvil.md
```

**R12b is a SOLE WITNESS and is marked as one in its own row.** It is the only end-to-end reading of
the armour split anywhere — the composition it covers has no unit guard and cannot have one, because
no test in this project can construct an `ItemStack`. **If one row in this file is going to be run,
it is R0; if two, the second is R12b.**

**GAME MODE: SURVIVAL, for every row unless the row says otherwise.**

> **CREATIVE IS NOT A SUBSTITUTE HERE, AND THIS SCREEN IS A PARTICULARLY BAD PLACE TO TRY.**
> `CLAUDE.md`'s creative-divergence register records that the own-inventory screen **decomposes a
> gesture into independent single-slot writes** (`InventoryCreativeEvent`, one slot and its new
> item), so a slot guard is asked about half a gesture. **This is a TWO-INPUT screen**, and every
> row below about moving an item between them is reading a different mechanism in creative.

> **13a IS DISPLAY AND REFUSAL ONLY.** There is no confirm button, no XP is spent and nothing is
> consumed. **A row asking whether a transfer HAPPENED does not belong in this file** — that is 13b's
> gate, and it is separate on purpose so the one irreversible operation in the feature gets an
> undivided reading.

---

### R0 — The deployed build carries this slice

**THE SOLE WITNESS FOR EVERY OTHER ROW IN THIS FILE.** A wrong jar does not announce itself: it
produces readings, in the right shape, at plausible values. Slice 12b lost hours to a jar built from
the wrong worktree whose mtime was current and which passed every staleness check.

> **WRITTEN IN POWERSHELL, BECAUSE THAT IS THE SHELL THIS ROW IS RUN IN.** There is no `unzip` on
> the operator's machine, and `java` on `PATH` resolves to Oracle's JRE shim
> (`Common Files\Oracle\Java\javapath`), which ships **no `javap`**. **A bash R0 row is a row that
> does not get run.**

| | |
|---|---|
| **Setup** | <pre>Copy-Item run/plugins/rpg-0.1.0-SNAPSHOT.jar "$env:TEMP\deployed.zip" -Force<br>Expand-Archive "$env:TEMP\deployed.zip" -DestinationPath "$env:TEMP\deployed" -Force<br>$classes = Get-ChildItem "$env:TEMP\deployed\io\github\butterflysmp\rpg" -Recurse -Filter *.class<br>Write-Host "$($classes.Count) class files scanned"<br>$classes \| Select-String -Pattern ARMOR_HEAD -Encoding ascii \| Select-Object -ExpandProperty Path</pre> |
| **Predict** | **The class count is non-zero**, and it is RECORDED in the reading. **`0 scanned` means the unpack failed, and an absence underneath a zero means nothing at all.** |
| **Predict** | At least one path is printed, and it includes `TransferKey.class`. A **zero-match** result means the deployed jar predates this slice, whatever its mtime says. |
| **Predict** | **STATE WHICH TREE THE JAR WAS BUILT FROM.** A worktree checkout makes "the repo" ambiguous, and that ambiguity is what cost slice 12b a boot. |
| **READ** | _(not run)_ |

> `-Encoding ascii` is load-bearing: a `.class` file is binary, and a text-mode read will not find a
> constant-pool string it is perfectly capable of matching.

**IF R0 FAILS, STOP. No other row in this file is readable.**

---

## THE HUB STATION

### R1 — The anvil appears at slot 30 and the station row is contiguous

| | |
|---|---|
| **Setup** | Open the Nexus star at any level. Look at row 4. |
| **Predict** | Five stations, **left to right with no gap**: ender chest, anvil, crafting table, enchanting table, grindstone. The anvil is an **ANVIL** item, third from the left of the run. |
| **Predict** | **No black filler pane anywhere inside 29–33.** The hole at 30 that `VAULT_SLOT`'s javadoc held open is closed. |
| **READ** | _(not run)_ |

### R2 — Locked at level 6, with all three lore lines

| | |
|---|---|
| **Setup** | `/rpg playerxp set <you> 6 levels`, open the hub, **hover** the anvil. |
| **Predict** | The name reads **Anvil**, dimmed. Three lore lines, exactly:<br>`Locked -- unlocks at level 7`<br>`You are level 6.`<br>`An anvil in the world still works.` |
| **Predict** | **The icon is still an ANVIL, not a barrier** — `NexusMenu.station` dims the name and keeps the material, which is precisely why R3's message is mandatory. |
| **READ** | _(not run)_ |

### R3 — A locked click SPEAKS, and says both facts

| | |
|---|---|
| **Setup** | At level 6, **click** the anvil cell. |
| **Predict** | Nothing opens, and chat says exactly:<br>`Anvil unlocks at level 7. You are level 6. An anvil in the world still works.` |
| **Predict** | **Capital "A" on the third sentence.** A draft lower-cased the equivalent line for the grindstone and produced a sentence opening in lower case mid-message. |
| **READ** | _(not run)_ |

### R4 — Opens at EXACTLY 7, and the boundary is read on both sides

**THE OFF-BY-ONE IS INVISIBLE IN PLAY**, which is why both sides are staged rather than just the
open one. `set N levels` lands on the threshold with zero progress, so "level 7" here means exactly
**7,500** lifetime XP — the tightest staging available.

| | |
|---|---|
| **Setup** | `/rpg playerxp set <you> 6 levels`, click. Then `set 7 levels`, re-open, click. |
| **Predict** | At **6** the click refuses with R3's sentence. At **7** the anvil screen opens. |
| **READ** | _(not run)_ |

### R5 — Back from the hub returns to the hub

| | |
|---|---|
| **Setup** | At level 7+, open the anvil **from the hub**. Look at 48, then click it. |
| **Predict** | Slot 48 is an **ARROW** reading "Back to the Nexus". Clicking it returns to the hub. |
| **Predict** | The bottom row's coloured panes are **SEVEN** cells — 45, 46, 47, 50, 51, 52, 53 — with 48 and 49 as buttons. |
| **READ** | _(not run)_ |

---

## THE WORLD BLOCK

### R6 — All THREE anvil materials open our screen

**TWO OF THE THREE ARE THE TRAP.** An anvil degrades as it is used, so chipped and damaged are what a
lived-in world mostly contains. A missing entry opens **vanilla** and presents as a bug that
"sometimes" happens.

| | |
|---|---|
| **Setup** | At level **1** (below the hub gate), place and right-click each of: an **anvil**, a **chipped anvil**, a **damaged anvil**. |
| **Predict** | All three open OUR screen. **The vanilla anvil screen never appears** — no two-slot-plus-rename bar, no "Repair & Name" text field. |
| **Predict** | Level 1 is deliberate: **the world block is not gated**, only the hub route is. |
| **READ** | _(not run)_ |

### R7 — Vanilla rename and vanilla repair are GONE, with no fallthrough

| | |
|---|---|
| **Setup** | Right-click an anvil holding a damaged vanilla iron sword, and again holding a name tag. |
| **Predict** | Our screen opens both times. **There is no way to rename anything and no way to repair anything.** Ben's ruling; this is the expected reading, not a defect. |
| **Predict** | `NEXT.md` carries the parked entry for repair. **If this row surprises you, read that entry before filing anything.** |
| **READ** | _(not run)_ |

### R8 — From a block there is NO Back button, and 48 is readout

| | |
|---|---|
| **Setup** | Open from a world anvil. Look at 48. |
| **Predict** | **EIGHT** coloured panes in the bottom row — 45, 46, 47, **48**, 50, 51, 52, 53 — and Close alone at 49. **No arrow, and no black filler pane sitting in the middle of a coloured row.** |
| **Predict** | Clicking 48 does **nothing**. |
| **READ** | _(not run)_ |

---

## THE FOUR FACES

Each row reads **the bar colour**, **the preview cell's icon**, and **the sentence**. All three come
from one verdict, so a row where they disagree is a finding regardless of which looks right.

### R9 — Empty: GRAY, and an instruction rather than a complaint

| | |
|---|---|
| **Setup** | Open the screen and touch nothing. |
| **Predict** | Bar **GRAY**. Slot 24 is a **BARRIER** named `Place the item to upgrade, and one to sacrifice.` in **gray**. |
| **Predict** | **It is not an error face.** This is what the screen looks like the moment it opens, and it should read as an instruction to a player who has never seen it. |
| **READ** | _(not run)_ |

### R10 — One item in: still GRAY, still the empty face

| | |
|---|---|
| **Setup** | Put a weapon in slot 20. Leave 22 empty. Then move it to 22 and leave 20 empty. |
| **Predict** | Bar stays **GRAY** and the sentence is unchanged, **both ways round**. |
| **READ** | _(not run)_ |

### R11 — A tool: RED, and NOT the mismatch sentence

| | |
|---|---|
| **Setup** | `/rpg give` a tool into both slots. |
| **Predict** | Bar **RED**. Sentence: `Both items must be gear that carries a score.` |
| **Predict** | **It must NOT say "Both items must be …" naming a kind.** Two pickaxes ARE the same kind; telling the player so would be true and useless. This is the distinction `AnvilTransferTest` pins, read here on the real screen. |
| **READ** | _(not run)_ |

### R12 — Boots into a helmet: RED, and the sentence names HELMETS

**THE HALF `GearClass` CANNOT EXPRESS.** Both items are `GearClass.ARMOR`; only the transfer key
distinguishes them.

| | |
|---|---|
| **Setup** | A **helmet** in slot 20, a **pair of boots** in slot 22. |
| **Predict** | Bar **RED**. Sentence: `Both items must be helmets.` — the **TARGET's** kind, plural. |
| **Predict** | Swap them: sentence becomes `Both items must be boots.` |
| **READ** | _(not run)_ |

### R12b — SOLE WITNESS: a real helmet derives ARMOR_HEAD, and that key drives the refusal

**THIS ROW EXISTS BECAUSE A MUTATION KILLED FEWER TESTS THAN PREDICTED, AND THE GAP IT EXPOSED IS
WIDER THAN THE ONE GUARD.**

`MUT13A-KEYARMOR` collapses all four arms of `TransferKey.ofArmor` to `ARMOR_HEAD`. It was predicted
to redden two rows and reddened **one** — `TransferKeyTest.ARMOURSplitsFOURWays`. The other,
`AnvilTransferTest.BOOTSCannotFeedAHELMET`, stages its two sides as `TransferKey` constants
**directly** and never calls `of()`, so it tests the COMPARISON and is blind to the DERIVATION.

> **THE COMPOSITION HAS NO UNIT GUARD ANYWHERE, AND CANNOT HAVE ONE.** The two halves are each
> covered — the derivation by `TransferKeyTest`, the comparison by `AnvilTransferTest` — and
> **nothing tests them joined**: that a REAL helmet, read off a REAL `ItemStack`, resolves to
> `ARMOR_HEAD`, and that THAT key is what refuses. **No unit test can construct an `ItemStack`**
> (`new ItemStack(...)` throws without a `RegistryAccess`, and there is no MockBukkit), so this is
> not an omission that a better test would close. **This row is the only end-to-end witness that
> exists.**

**R12 IS NOT A SUBSTITUTE, AND THE DIFFERENCE IS THE DONOR'S SCORE.** R12 leaves the two scores
unstaged, so it cannot say WHICH rule refused — with a lower-scored donor the pair would be refused
by `DonorNotHigher` whether or not the key rule exists. **Staging the donor STRICTLY HIGHER removes
every other reason to refuse**, so the only thing standing between these two items and a completed
transfer is the armour split.

| | |
|---|---|
| **Setup** | A **helmet** in slot 20 and a **pair of boots** in slot 22. **Both must be OURS and both must carry a score** — check each tooltip reads a `Gear Score:` line before starting; an unstamped item reads 100 and is still scored, but an item that is none of ours is `Unscoreable` and refuses for a different reason entirely. |
| **Setup** | **Stamp the BOOTS strictly higher than the helmet**, and not by one — e.g. helmet **140**, boots **310**. Two numbers that cannot be confused for each other, and far enough apart that a misread is obvious. |
| **Predict** | The bar is **RED** and slot 24 reads `Both items must be helmets.` |
| **Predict** | ***IT MUST NOT BE LIME.*** A LIME face here means the four armour keys have collapsed and **a pair of boots is about to hand its roll to a helmet** — which is the half of Ben's rule `GearClass` cannot express, arriving in the one place nothing else can see it. |
| **Predict** | **It must not read `The sacrifice must score above 140.`** either. That sentence means the key rule never ran and the refusal came from the score comparison instead — the pair would then be refused today and legal the moment somebody raised the helmet's score. |
| **READ** | _(not run)_ |

> **WHAT TO DO IF THIS ROW FAILS:** do not look at the screen code. The suspects are
> `TransferKey.ofArmor` (four arms collapsed), `AnvilMenu.armorSlotOf` (returning the wrong slot or
> `null`), and `GearItems.gearClassOf` (armour not resolving to `ARMOR`). The unit rows for each are
> green, so a failure here is in the JOIN.

### R13 — Equal scores: RED, and it names both numbers

| | |
|---|---|
| **Setup** | Two items of the same kind at the **same** gear score. Use `/rpg` to stamp them equal if needed. |
| **Predict** | Bar **RED**. Sentence: `The sacrifice must score above <N>. This one is <N>.` with the same N twice. |
| **Predict** | **EQUAL IS REFUSED, not accepted as a harmless no-op.** Ben's ruling: accepting it would eat the donor in 13b and change nothing. |
| **READ** | _(not run)_ |

### R14 — A lower donor: RED, and the numbers differ

| | |
|---|---|
| **Setup** | Target at a **higher** score than the donor. |
| **Predict** | Sentence names the target's score first and the donor's second, and **they differ** — so the player can see how far short it is without hovering two tooltips. |
| **READ** | _(not run)_ |

### R15 — A legal pair: LIME, the item, the new score and the cost

| | |
|---|---|
| **Setup** | Two items of the same kind, donor **strictly higher**. |
| **Predict** | Bar **LIME**. Slot 24 shows **the target item itself** — same name, same material — carrying **the donor's** gear score on its tooltip. |
| **Predict** | Lore ends with `Sacrifice to raise this to <donor score>.`, then `Costs <N> XP.`, then `Preview only -- you cannot take this.` |
| **Predict** | **The cost is in XP POINTS and the LEVEL figure appears nowhere.** A RARE target reads **910**, an EPIC **2920**. |
| **READ** | _(not run)_ |

### R16 — The cost keys on the TARGET's rarity

| | |
|---|---|
| **Setup** | A **COMMON** target and an **EXOTIC** donor of the same kind, donor higher. Then reverse the rarities. |
| **Predict** | COMMON target: **160 XP**. EXOTIC target: **24045 XP**. The two are unmistakable for one another. |
| **Predict** | **Upgrading a cheap item stays cheap however precious the thing you feed it.** |
| **READ** | _(not run)_ |

---

## THE PREVIEW IS NEVER CARGO

**EVERY GESTURE, BECAUSE ONE UNGUARDED ROUTE IS A DUPLICATION EXPLOIT.** Count the items before and
after each: a gesture that DUPLICATES and a gesture that STEALS look identical in a screenshot of the
after state, and every row below is written against loss while a duplicate satisfies all of them.

### R17 — Slot 24 cannot be taken by any gesture

| | |
|---|---|
| **Setup** | With a LIME preview showing, try on slot 24: **left-click**, **right-click**, **shift-click**, **double-click**, a **number key**, and **F**. |
| **Predict** | **Nothing leaves slot 24 on any of the six.** Nothing reaches the cursor, the hotbar or the offhand. The player's item count is unchanged after all six. |
| **Predict** | The two input items stay exactly where they are. |
| **READ** | _(not run)_ |

### R18 — Closing returns BOTH items and nothing else

| | |
|---|---|
| **Setup** | Both slots full, LIME preview showing. Close with **Esc**, then repeat and close with the **49 button**, then repeat and close with **Back** from the hub. |
| **Predict** | **Exactly two items** come back, all three times. **No third item**, and no copy of the preview. |
| **Predict** | Identical items — same score, same enchants, same durability. The preview never wrote to either of them. |
| **READ** | _(not run)_ |

### R19 — The screen survives a disconnect and a shutdown with the items intact

| | |
|---|---|
| **Setup** | Both slots full: (i) disconnect and rejoin; (ii) with the screen open, `stop` the server and restart. |
| **Predict** | Both items are in the player's inventory afterwards, both times. **Two items, not three.** |
| **READ** | _(not run)_ |

---

### R20 — TWO BARRIERS ON ONE SCREEN, AND THEY ARE TOLD APART BY LORE

**The cost of choosing a barrier for the refusal, named rather than discovered.** `MenuIcons.close()`
is also a BARRIER, so a refused screen carries two.

| | |
|---|---|
| **Setup** | With a RED face showing, hover slot **49**, then slot **24**. One screenshot reads both. |
| **Predict** | **49 is a barrier with NO LORE AT ALL** — its whole content is its name. **24 is a barrier that is ALL LORE** — the instruction as its name, plus `Nothing is spent until you confirm.` |
| **Predict** | **Neither reads "Not implemented yet."** That string belongs to `MenuIcons.placeholder`, which means NOT BUILT; a refusal is a working readout and this project has shipped a working readout in placeholder clothes once already. |
| **READ** | _(not run)_ |
