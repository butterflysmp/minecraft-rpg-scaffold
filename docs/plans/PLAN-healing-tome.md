# PLAN — THE HEALING TOME: A MAGE CLASS ACCESSORY WITH AN ALWAYS-ON RADIANT PULSE (2026-11-26)

**Status: BUILT, NOT BOOTED.** The gate is `docs/gates/GATE-healing-tome.md`. Stacked on the Bone Club stack (#323).

## BEN'S WORDS, VERBATIM, 2026-11-26

- "Let's do the healing tome"
- "Before I answer these, I have a major rework for the healing tome. Instead of it being a weapon/artifact, I want it to
  be a mage specific class accessory. Keeping the Radiant pulse ability but it's active at all times."
- "1. Heals 1 health per the players level that has it equipped. A level 50 player would heal 50. Pulse every 4 seconds. 8
  block radius 2. No mana cost. I might change this layer 3. It heals you and any players near you. This will change when
  we add a party system to heal only players and their Summons within your party. 4. Only heals when someone is hurt 5.
  No, it's only the healing pulse 6. Ready above 7. Rare Book 8. Gone 9. Loot only"

**CARRIED, in Ben's words:** *"This will change when we add a party system to heal only players and their Summons within
your party."* Until then the pulse heals PLAYERS only, never a summon or a mob.

**The old game (CreaperCrusher/Butterfly-SMP at cfde822), a reference, not a ruling:** HEALING_TOME, a BOOK, rare;
"Radiant Pulse" a right-click cast (60 mana, 18 s) healing everything within 6 blocks once a second for 8 seconds, with
HAPPY_VILLAGER particles and a HEART per healed target; "Mending Touch" (GONE). Loot: pillager outposts, ancient cities,
villages, stronghold libraries, fishing treasure.

## THE ITEM (BEN'S), AS COMMITTED — `content/accessories/healing_tome.yml`

`healing_tome`, "Healing Tome", rare, `book`, `slot: class`, `class: mage`, `type: tome`, **no modifiers**, and one
behaviour: `healing_pulse`, `heal_per_level: 1`, `interval_ticks: 80`, `radius: 8`, `targets: players`, with the feedback
keys (SEAT PLACEHOLDER). No mana cost. Loot only: no source is built.

## THE STACK

| PR branch | what |
|---|---|
| `feat/healing-tome` | core: the behaviour type, TOME, the decision; the loader, the content, the tooltip; this plan |
| `feat/tome-pulse` | the clock (`TomePulses`), the heal, the feedback, the trace; H12's book guards |
| `feat/tome-selftest` | H15: the snapshot's class and slots; the class, accessory, saturation and regen verbs; HEALED; the lint |
| `feat/blast-fungus-gate` (#321) | moved up again, its three commits unchanged; `GATE-healing-tome.md` added |

## H1 TO H16, EACH WITH ITS READS

**H1, ONE BEHAVIOUR IN YAML.** `core/accessory/AccessoryBehaviour` is sealed, `EnchantBehaviour`'s shape; its one member
`HealingPulse(name, healPerLevel, intervalTicks, radius, targets, particle, particleCount, heartParticle, sound)`. The
loader reads a `behaviour:` block (`AccessoryLoader.parseBehaviour`) and refuses a malformed one by name. No accessory id in
Java. **READ:** `AccessoryDefinition`'s constructor REFUSED an empty modifiers map ("has no modifiers -- an accessory that
changes nothing has nothing to show on its tooltip"); it now refuses it only when no behaviour is present. **The six
shipped accessories (the brief said five; READ six: ward, keen and mending charms, the gauntlet, the quiver, the scroll)
load unchanged**, each pinned as before in `AccessoryLoaderTest` and asserted behaviour-free.

**H2, THE TYPE.** **READ:** `AccessoryType` is read only by `AccessoryLoader` (the content token and its refusal message)
and by `AccessoryDefinition`'s pairing check. **No player sees the word** (the tooltip's slot line is the CLASS: "Mage class
accessory"). So `TOME(MAGE)` was added; two types now name MAGE, and the one table still refuses every mismatch
(`AccessoryDefinitionTest.theTypeMustMatchTheClass_everyPairChecked`: 4 load, every other pair refused).

**H3, WHEN IT RUNS.** `AccessoryContributions.of` now also returns the ACTIVE behaviours, from the same loop and the same
`AccessorySlots.contributes` check the stats use (class slot, profile class mage). `PlayerHealthSystem`'s reconcile pass
hands its ONE reading's behaviours to `TomePulses.reconcile`; at each pulse the clock asks the same function again
(`accessoryContributionsFor`). Nothing in `TomePulses` reads a slot (`TomePulsesWiringTest` forbids the store reads).
Inert on a class change, live on changing back; whatever he holds.

**H4, THE CLOCK.** One `RepeatingTask` per wearer on his `EntityTaskTarget` (READ: `HealthRegenSystem`'s shape, so it runs on
his own thread, the main thread on Paper and his region on Folia, where `getNearbyEntities` at his feet is legal). Started
by the reconcile pass that first sees the tome active, the ANCHOR (CC'S CALL below); the first pulse 80 after (READ:
`RepeatingTask.start` arms one period out), every 80 after. Stopped by a pass that sees it inactive, by the pulse's own
re-check, on quit (`PlayerHealthSystem.onQuit`), and on death (READ: `EntityTaskTarget.isActive` is false while dead, and
the step stops without running). A restart is a new task, so the count is from 0. A world change keeps it (nothing watches
the world). Not a scan of every player: only wearers have a task; a non-wearer costs a map lookup per pass.

**H5, A PULSE AS A PURE DECISION.** `core/accessory/HealingPulses.decide`, pinned as a table (`HealingPulsesTest`, below).
Health and max are the store's (`CombatantStats.current/max`, what the stats sheet reads). L is the wearer's level at the
pulse. **READ: how other effects treat creative.** `MobBlaster` exempts creative and spectator from a BLAST (harm);
`BukkitCombatant.applyHeal` has no game-mode filter for a heal. **CC'S CALL: a creative player is a candidate** (a heal
harms nothing; the brief excludes only spectators). Alternative: exclude creative as the blast does.

**H6, THE HEAL.** **READ:** `BukkitCombatant.applyHeal` and `HealthRegenSystem` both heal through `CombatantStats.heal(id,
amount, dealer, dealerIsPlayer)`; that is the one path, with the WEARER as the dealer. The wearer is healed inline (his own
thread); every other target through `scheduler.onEntity(target, …)`, as `applyHeal` does. Each application re-reads the
target (dead or untracked: skipped) and caps the amount at what it misses then. No mana is read or spent.

**H7, NOT HEALED.** Mobs, summons (`CombatSides.of == SUMMON`), armour stands, the dead, another world, spectators: each a
row of `HealingPulsesTest.theTable_atLevelSeven`. Ben's party sentence is carried above.

**H8, TWO WEARERS** each pulse on their own clock and both heal (each wearer's task is independent; nothing de-duplicates).
**No line of sight is needed** (the decision reads distance only). **Both SEAT PLACEHOLDER, OPEN FOR BEN.**

**H9, FEEDBACK (SEAT PLACEHOLDER, the old game's):** on each player actually healed, `happy_villager` ×6, one `heart`, and
`block.amethyst_block.chime` at volume 0.6, pitch 1.2 to that player only (`Player#playSound`); nothing when nobody is
healed (called only after a heal landed). Keys in content; **READ:** the particles resolve (`Particle.valueOf`, checked at
load: the shipped file loads with no warning) and the sound key is in the 26.1.2 jar's `SoundEvents` (a misspelt control
is not).

**H10, THE TRACE (under `/rpg mobtrace`, the selftest's `trace`: READ, one switch, `nameplates::tracing`):**
`TOMEPULSE wearer=<name> level=<L> tick=<tick> candidates=<n> healed=<n>` on every pulse, then per healed target
`TOMEPULSE target=<name> before=<hp> amount=<a> after=<hp>`, and `TOMEPULSE feedback …`; plus `TOMEPULSE START … tick=` (the
anchor) and `TOMEPULSE STOP … reason=`. Formats pinned in `TomePulsesWiringTest.theTraceLines`.

**H11, THE TOOLTIP AND THE SHEET (SEAT PLACEHOLDER wording), from the YAML's own numbers:**

```
Radiant Pulse                                      (gold)
Every 4s, heals hurt players within 8 blocks       (gray)
for 1 health per your level                        (gray)
Mage class accessory                               (gray)

Rare Accessory                                     (the rare colour)
```

The sheet: **`Healing Tome: Radiant Pulse`** when active, **`Healing Tome: inactive (requires Mage)`** otherwise. The
golden file gained an ACCESSORIES section: the tome's rendering was written down before the section was generated and
matched it line for line (and a control that changed one word did not); the other six were RECORDED, not predicted.

**H12, A BOOK'S VANILLA BEHAVIOUR (READ in the 26.1.2 server jar) AND WHAT STOPS EACH FOR OUR ITEM ONLY:**

| use | READ | before this work | now |
|---|---|---|---|
| a librarian buys a book | `villager_trade/librarian/2/book_emerald.json`; four more librarian trades take a book as a second cost (control: `armorer/1/emerald_iron_boots.json` reads `minecraft:emerald`) | NOTHING: the five accessories sat on materials no villager buys | `onAccessoryTrade` at `PlayerPurchaseEvent` (READ: fired in `MerchantResultSlot.onTake` before `MerchantOffer.take`; cancelled, the cost slots are never taken; `PlayerTradeEvent` has no handler list of its own) |
| a chiseled bookshelf takes a book | `tags/item/bookshelf_books.json` lists `minecraft:book` | NOTHING | `onAccessoryShelved` (a right-click), `onAccessoryHoppered` (a hopper move) |
| a lectern | `tags/item/lectern_books.json`: written and writable books only | not reachable | nothing needed |
| crafting: book and quill, bookshelf, enchanting table | `recipe/writable_book.json`, `bookshelf.json`, `enchanting_table.json` | `CraftMatrixScreen.isGear` knows `accessory_id`; every crafting surface refuses CONTAINS_GEAR | unchanged |
| the enchanting table, anvil, grindstone | the three blocks are hijacked into our menus (`RpgListeners.hijackedBlocks`) | each refuses an accessory with the A4 words | unchanged |

A plain book is untouched by every guard: each keys on `accessory_id`. `AccessoryDefinition.MATERIALS` gains `book` with
this table as its reason. **The refusal words are SEAT PLACEHOLDER, OPEN FOR BEN** ("Accessories cannot be traded.",
"Accessories cannot be shelved.").

**H13, A4 HOLDS.** Every A4 arm keys on the accessory id, not the material, so the tome is refused by enchanting, the
anvil, the grindstone, gear score and durability with the existing words (`AccessoryRefusals`, asserted by text in
`TomePulsesWiringTest.a4sWordsAreUnchanged`; three by command in the gate's HT1).

**H14, LOOT ONLY.** No source is built. **CARRIED beside the others in the world-loot list:** the Healing Tome (the old
game's five sources are a reference, not a ruling). Until the loot pass it is obtainable only by `/rpg give healing_tome`.

**H15, THE SELFTEST MUST NOT COST BEN HIS BUILD.** The run's snapshot captures his four accessory slots EXACTLY (the stored
text, unreadable items included) beside his class, and puts them back whole through `AccessoryService.replace` at the
run's end and on every abort (the one funnel, `SelfTestRun.end`), then READS BACK: `SelfTestAccessorySlots`, W1's mechanism.
CAPTURED gains `class=… accessories=[…]`; RESTORED gains `accessories=[…]` and `NOT-RESTORED=accessories(…)` when it did
not land. **READ: his LEVEL is restored by his lifetime XP** (`ProfileService.setLifetimeXp`, as before), his class by the
existing `setCell`. **READ: the restore's write is safe at both exits:** `onQuit` calls the selftest FIRST, before the
accessory service drops him; `onDisable` calls the selftest first and drains `storageIo` last. New verbs, through the real
functions: `class` (the Build screen's write), `accessory equip <slot>` / `clear` (`canEquip`, `AccessoryService.write`;
never over an occupied slot), `saturation` (his regen tier; restored), `probe_regen`. Lint `ACCESSORY_UNSTAGED`: a row
that equips stages class, slots and level itself, first.

**H16, RECOUNTS WITH CONTROLS.** Accessories 6 → 7 (`ls content/accessories/*.yml | wc -l`; `UniqueSlotContentTest`); gear
definitions of the unique-slot set 67 → 68; the crafting index's 61 unchanged (accessories are not in it, A3;
`BootCountLinesTest` green); golden renderings 146 → 153 (+7 accessories); gates 53 → 54, scenarios 511 → 519 (573 keys,
control 564 + 9).

## THE PULSE'S DECISION TABLE (`HealingPulsesTest.theTable_atLevelSeven`; the wearer at the origin, L = 7)

| entity near him | a candidate? | healed by |
|---|---|---|
| himself, hurt by 20 | yes | 7 |
| a player 8.0 blocks away exactly | yes (inclusive, CC'S CALL) | 7 |
| a player 8.001 away | no | — |
| a player missing 3 (less than L) | yes | 3, to max |
| a player missing exactly L | yes | 7 |
| a player at full health | yes, skipped | — |
| a dead player | no | — |
| a player at 0 custom health, not yet dead | no | — |
| a spectator | no | — |
| a player in another world at the same coordinates | no | — |
| a hurt mob | no (H7) | — |
| a hurt summon | no (H7, until parties) | — |
| an armour stand | no (H7) | — |

And: L is the WEARER'S level for every target (L = 1, 2, 3, 5, 50 rows); `heal_per_level` scales it; nobody hurt decides
no heal and still counts candidates; one entity found twice is one candidate.

## CC'S CALLS

1. **The anchor is the reconcile pass that first sees the tome active** (up to 4 ticks after an equip, a profile load, a
   class change or a respawn): the stats an accessory grants start at that pass too, so "starts contributing" has one
   meaning. Alternative: hook each act (equip, login, class change, respawn), which misses any act nobody listed.
2. **A creative player is a candidate**; only spectators are excluded. Alternative: exclude creative as the blast does.
3. **The radius is inclusive** (8.0 exactly is in). Alternative: strict.
4. **L is his EFFECTIVE level** (capped at 50, as every level bonus reads it). Alternative: the raw level past 50.
5. **The pulse re-checks contribution at its own tick** through the reconcile's function, so a pulse due in the up-to-5
   ticks between an unequip and the next pass does not fire.
6. **H12 closes the chiseled bookshelf** (by hand and by hopper) as well as the trade: storage there would not change the
   item, but the brief says block what is not blocked.
7. **The golden file gained all seven accessories,** not only the tome, so the section is complete.

## PLACEHOLDERS, OPEN FOR BEN (SEAT PLACEHOLDER 2026-11-26)

1. The ability's name, "Radiant Pulse".
2. The tooltip's lines: "Every 4s, heals hurt players within 8 blocks" / "for 1 health per your level".
3. The sheet's line, "Healing Tome: Radiant Pulse".
4. The feedback: `happy_villager` ×6, one `heart`, `block.amethyst_block.chime` at volume 0.6, pitch 1.2, to the healed
   player only.
5. Two wearers near each other both heal (stacking).
6. No line of sight is needed.
7. The refusal words "Accessories cannot be traded." and "Accessories cannot be shelved."
8. Ben's own "No mana cost. I might change this later": nothing is built for a cost.

## OWED, CARRIED

- **A second player healed:** NOT READ by the gate's boot (one player); covered by the decision's table and the wiring
  test. Owed to a boot with two players.
- **The feedback's look and sound, a real villager refusing the trade, a real chiseled bookshelf refusing it, the anvil's
  and grindstone's screens with the tome:** NOT READ by the gate (it asks nothing).
- **Loot sources** (H14): the loot pass.
- **Parties** (Ben's carried sentence): when parties exist, the targets become "players and their Summons within your
  party".
