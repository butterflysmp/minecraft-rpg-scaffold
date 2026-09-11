# PLAN — the quiver, slice A2: quiver size and reload time as real stats

> Slice A1 shipped on `feat/quiver` and its record is `PLAN-quiver.md`. This is A2, reviewed and
> accepted before any code, with two amendments folded in and marked.

## Context

A1 shipped the quiver as a per-item count with **authored constants** for capacity and reload
duration. A2 promotes both to stats so gear and enchants can move them. That is what the A1/A2
split was bought with, and this is the slice that spends it.

**Commit 0 is the boot record**, because plan mode cannot write it. `GATE-quiver.md` must record
V1's display half, Q12 and V2 as **answered by blanket** (per-row in shape, blanket in evidence —
the `setLoaded` fix holds), and **Q7 as UNRUN**. Q7 is not green: it produces a number that exists
nowhere else, so there is no expected value for "green" to be against, and **slice C's 7-tick dual
cooldown is priced on an unmeasured floor.** Carried forward as owed.

---

## TWO CONSEQUENCES FLAGGED FOR ENDORSEMENT, NOT BURIED

**1 · YOU PACK YOUR QUIVER, AND WHAT YOU PACKED IS WHAT YOU CARRY.** Because the stamp enforces
(below), equipping capacity gear does not change what you can hold until the next **quiver write**.
That is a *feel* statement about how Ranger plays, not a mechanism detail, so it is the operator's
to endorse.

> **One refinement to the framing, because the precise mechanic is slightly wider than "until you
> reload".** `setLoaded` is called on **every shot and every reload**, so capacity re-resolves at
> whichever comes first — and after equipping gear the first thing a player does is usually fire.
> The honest statement is **"capacity is as of your last shot or reload."** The spirit is the
> operator's phrasing; this is what the code will do.

**2 · THE MODIFIER SHAPE IS A RECOMMENDATION TAKEN, NOT A RULING RECEIVED.** Planned as **flat
whole-arrow modifiers** on the `classDamage` precedent (*"a SUMMAND in points, so 0"*), because at
`quiver_stone`'s 9 rounds **one arrow is 11%** and any percentage under that delivers zero — a
tooltip advertising a buff that grants nothing. `Quiver.applyPercent` still ships and stays tested:
it is the rule for a percentage *reaching* an integer stat, not the quiver's own modifier design.

---

## CORRECTIONS OWED — two claims of mine, both wider than what was checked

**These are the `NEXT.md` class from A1, recurring. Both are recorded rather than quietly fixed.**

### "A2 is a change to the TWO call sites that supply them" — FALSE WHEN MADE

Mine first, in A1's commit 1, repeated back as settled, never checked. Measured
(`grep -rn "quiverSize()\|reloadTicks()"`): **six sites.**

| # | site | reads |
|---|---|---|
| 1 | `QuiverItems.java:90` — `stampFull`, at MINT | `quiverSize()` |
| 2 | `Quivers.java:124` — the `UNSTAMPED` repair arm | `quiverSize()` |
| 3 | `Quivers.java:195` — `beginReload`, already-full check | `quiverSize()` |
| 4 | `Quivers.java:213` — `beginReload`, the deadline | `reloadTicks()` |
| 5 | `Quivers.java:244` — `finishReload` | `quiverSize()` |
| 6 | `WeaponLore.java:84` — the TOOLTIP | `quiverSize()` |

**What the guard proved is still true and is worth separating from the wrong number.**
`QuiverSignatureTest` pins `Quiver`'s primitives-only contract, and every core row constructs at
capacity 3 / reload 7 — not the shipped values. **`Quiver` demonstrably does not know them, and A2
must not reach into it.** Only the count of *callers* was wrong.

> **AND THE DESIGN BELOW MAKES THE CLAIM TRUE BY CONSTRUCTION** — one stat read for capacity, one
> for reload duration, with every other site reading a stamp. Asserted, not assumed.

### "Mint paths structurally cannot have a Player" — ALSO FALSE

**Every mint call site has one in scope.** All five menu sites inherit `Menu.viewer`
(`Menu.java:36`) or hold their own (`InventoryCraft.java:85`); both command sites have `player`;
`GearRefresher.rebuild` has one a frame up at `refresh(Player, …)`. **Plumbing was never the
obstacle.** What a browser icon lacks is not a player but a *meaning* — it previews a definition,
not a held item. The real obstacle is the invariant, below.

---

## THE TOOLTIP — RULED, AND WHY IT DOES NOT BREAK THE INVARIANT IT APPEARS TO

`WeaponLoreLines.java:30-33` states the rule, and `:18-21` and `WeaponLore.java:29-30` restate it:

> *"Lore describes the weapon, not whoever is holding it … **A boosted player and an unboosted one
> reading the same sword must see the same number.**"*

It is also **structurally enforced**: `WeaponItems.java:132-137` hides vanilla's attribute block
precisely because *"the attack speed vanilla would show is the reconciled total, which includes
whatever boost the holder happens to carry — exactly the drift the lore rule exists to prevent."*
**Rendering a per-player number on a tooltip would be the first instance in this repo.**

**RULED: stamp the resolved capacity, and the stamp is what ENFORCES.** The deciding argument is
that file's own closing sentence — the deeper principle the static rule serves:

> *"Each formatter reads **the number that actually governs** its own weapon, so neither line can
> drift from what the weapon really does."*

Static-ness is the *mechanism* by which non-drifting is achieved for attack damage, because there
the definition **is** what governs. **Under A2 that inverts**: a player with +2 genuinely fires
eleven, so authored-always would satisfy the surface rule while violating the deeper one — and it
would be **permanently** wrong rather than stale.

**And the tooltip does not actually cross the item→holder axis at render time.** It reads a value
off the item's own PDC, exactly as the count already does. What is new is that an item's stored
value is now derived from whoever last wrote it. Everyone looking at that item sees the same
number, and no `Player` is needed at render.

### Condition 1 — THREE ORDERED SOURCES, not one value read once

If the tooltip rendered the stamp while the refusal logic resolved the holder's capacity **live**,
the two would diverge and the tooltip would lie by a new mechanism — the defect `ResourceCost`
already records: *"Two literals cannot drift apart if there is only one."*

> **"ONE VALUE, READ ONCE" IS NOT WHAT THIS DESIGN ACTUALLY IS, AND SAYING IT WOULD BE FALSE.**
> There are **three** sources, and they do not compete — **they are ORDERED**, each answering a
> different question:
>
> | source | answers | when |
> |---|---|---|
> | **the stat** | *what capacity does this wielder resolve?* | **only at a write**, inside `QuiverItems.setLoaded` |
> | **the stamp** | *what does this item hold, and what does it enforce?* | everywhere else — render **and** refusal |
> | **the authored value** | *what does this weapon hold when there is no item at all?* | the unstamped fallback (below) |
>
> The invariant that matters is the **order**, not a count: nothing reads the stat except a write;
> nothing reads the authored value except in the absence of a stamp.

**Made structural rather than promised:**

- `QuiverState` gains a `capacity` component; **`QuiverState.from` resolves it** and `Quivers.stateOf` merely reads five values and passes them. *(An earlier draft said `stateOf` resolved it — true for one commit, until the decision moved into core so a row could reach it.)*
- **`reloadVerdict(long now, int capacity)` LOSES its capacity parameter.** With nowhere to pass a
  different value, a divergent capacity is **unrepresentable** — the same move that removed
  `reloadTicks` from `reloadComplete` and made A1's free-instant-reload defect impossible.
- **The stat is read in exactly one place: `QuiverItems.setLoaded`.** Nothing else resolves it.

### THE UNSTAMPED FALLBACK — and the check I nominated was the thing the change breaks

**Measured.** `golden-lore.txt:94` reads:

```
"Quiver: --/9"
```

The numerator is absent (the harness renders from **definitions**, with no item) and **the
denominator, 9, comes from the definition.** Commit 1 moves the tooltip to read the stamp — and
with no item there is no stamp, so the denominator would lose its source entirely.

> **SO THE GOLDEN MOVES, AND I HAD NOMINATED THE GOLDEN AS COMMIT 1'S PROOF.** The check that would
> catch a regression is the one the change disables. Left as written, it surfaces at the *end* of
> commit 1 as a puzzling golden diff instead of at the start as a design question.

**RULED: `quiverLine` falls back to the AUTHORED capacity when no stamp is present** — the same
argument already made for mint keeping authored capacity, which is what *"keeps the preview and
browser icons honest."* A definitions-only harness, a recipe-browser icon and a craft preview all
describe a weapon rather than a held item, and the authored number is the true answer for all three.

**And the golden stays commit 1's verification, with a stronger claim than "nothing changed":**

> **byte-identical BECAUSE the unstamped fallback returns the authored capacity** — which is a claim
> commit 1 can actually make, and which exercises the third source specifically. Record at the
> golden that a definitions-only harness renders a number the running game never shows, and why.

### Condition 2 — THE TRANSFER CASE, named here and in the code beside the stamp

```
boosted player packs to 11, drops the weapon
unboosted player picks it up and reads        Quiver: 8/11
```

**Worse than the stale-gear case and it must not be unstated.** The stale value came from *someone
else's* gear; *"the same number for everyone"* is satisfied while being wrong for the person
holding it. It self-corrects on their first shot — but **a player reading a weapon they just picked
up is exactly when the tooltip is load-bearing.** Acceptable; not acceptable unstated, because a
reader meeting `8/11` on an unboosted character will read it as a bug.

---

## THE CLAMP — it is not routed through `setLoaded`, it IS `setLoaded`

`DESIGN-stat-engine.md`'s ruled semantics: **increase is headroom, decrease clamps.**

Under the stamp design, **capacity cannot change except at a write**, and at that write `setLoaded`
holds both the new capacity and the count. So the clamp is one line inside the funnel:

```
count = Quiver.clamp(count, resolvedCapacity)      // already exists, already tested
```

**This removes the need for a reconcile-loop clamp entirely.** `MUTA2CLAMP` — the mutation that put
a clamp in `PlayerHealthSystem` and reddened
`theCountKeyIsTouchedInExactlyTwoFilesAcrossAllOfPaper` — describes a site that now never needs to
exist. The guard stays green because nothing outside `QuiverItems` touches the key, and **no
exemption is wanted.**

**Mint keeps the authored capacity**, and that is correct rather than a compromise: a fresh weapon
full to its authored size, held by a boosted player, is *exactly* a count below capacity with
headroom to reload into — the ruled semantics, needing no player at mint and keeping the preview
and browser icons honest.

---

## THE FIXTURES — two, one per stat, and PERMANENT

**Two, not one.** A single fixture carrying both bonuses has **no staging in which one stat moves
and the other does not**, so no row could ever make the negative observation — and the negative
observation is the whole of the crit precedent (*"one item can raise how often you crit without
touching how hard"*). The defect one fixture is blind to is the stats being **entangled** — a size
modifier that also moves reload, or a reload scanner wired to the size key by copy-paste, which is
exactly what happens when two scanners are written minutes apart from the same template.

> **Write this sentence at the fixtures, so whoever wants to merge them knows the cost: the pair
> buys THE ABILITY TO OBSERVE ONE STAT HOLDING STILL WHILE THE OTHER MOVES.** The price is two
> Keys pairs, two dev-command arms and two scanners, and that is the larger half of why merging is
> tempting.

**PERMANENT, not `_TEMP`, and decided rather than inherited.** A1 ruled its carrier permanent on
`volley_stone`'s precedent and the reasoning applies unchanged: **A2 ships no quiver enchant** (out
of scope), so after this slice these fixtures remain *the only things that can move these stats* —
deleting them removes the ability to re-gate. The `_TEMP` label would add two more entries to a
removal debt `MEMORY` already tracks and nobody discharges; owning them is more honest.

**Numbers must differ from each other and from everything shipped**, or a crossed wire yields a
reading consistent with both hypotheses. The authored set (`content/weapons/` + `content/abilities/`,
the same named sweep `quiver_stone` ran, re-run at planning time):

```
0 0.03 0.05 0.3 0.5 0.6 1 1.2 1.4 1.6 2 2.3 2.5 3.0 3.5 4.0 5 6 7 8 9 10 11 12 13 15 16
20 21 23 24 25 26 27 30 32 34 35 40 60 64 80 100 120 200 300
```

> **THE SWEEP MUST COVER THE RESOLVED VALUE, NOT THE BONUS — AND MY FIRST TWO CANDIDATES FAILED ON
> EXACTLY THAT.** A gate row observes what is on screen, which is `base ± bonus`. `+17` on a base of
> 9 resolves to **26**, and `−19` on 34 resolves to **15**; both are in the set above. The bonus was
> protected and **the observation was not.**

Swept over `base ± bonus`, with both halves required free:

| stat | bonus | resolved | verdict |
|---|---|---|---|
| quiver size, base 9 | **+19** | **28** | both free, and 19 ≠ 28 |
| reload, base 34 | **+14** ticks | **48** | both free, and 14 ≠ 48 |

All four numbers are distinct and none is authored anywhere.

> **A REDUCING RELOAD FIXTURE HAS NO COLLISION-FREE VALUE AGAINST BASE 34, AND THAT IS A FINDING
> RATHER THAN AN INCONVENIENCE.** Exhaustively: `−14→20`, `−18→16`, `−19→15`, `−22→12`, `−28→6`,
> `−29→5`, `−31→3`, `−33→1` all land on authored numbers, and the only survivor, `−17`, resolves to
> **17 — the bonus itself**, which is the "two independent quantities equal" defect in its purest
> form. So **the reload fixture ADDS ticks**: a deliberately slow-reload dev instrument.
>
> That is also the *better* instrument. A longer reload is easier to time at a boot than a shorter
> one, and this fixture's whole job is to make the stat observable.

**Say in each fixture's header that the checked quantity is the OBSERVED value**, since that is the
one a gate row can confuse.

### THE FIXTURE MOVES RELOAD ONLY UPWARD — so the direction gear will move it is never exercised

**The adding fixture is right and this is its cost, stated rather than discovered.** In game,
reload gear will **reduce** the number — that is what a player wants from it — and **A2 ships no
instrument that moves it down.** The fixture cannot, and the proof above says none can exist at
base 34.

**And there is no floor.** `Quiver.reloadCompletesAt` is `now + Math.max(reloadTicks, 0)`, so a
resolved reload of **0 is legal and means an instant reload** — which deletes the brake the whole
slice is about. `Math.max(reloadTicks, 0)` is **a ruling made by omission**, and nothing in A2 as
first planned would have noticed: the fixture cannot reach it, no core row asks for it, and the
first reduction item to ship would find out in play.

> **NOT REACHABLE TODAY, AND THAT IS WHY IT BELONGS TO COMMIT 4 RATHER THAN NOW.**
> `WeaponDefinition` already refuses `quiverSize > 0 && reloadTicks <= 0`, so an authored zero is
> impossible. **The hole opens the moment a MODIFIER can reduce the value** — which is commit 5.

**RULED — `Quiver.MIN_RELOAD_TICKS = 1`, structural, NOT a balance number.**

> **THIS REPLACES A RULING OF 10 WHOSE ARGUMENT WAS FALSE ON BOTH PREMISES, and the retraction is
> kept because the failure mode is the interesting part: a balance number was wearing a mechanism
> argument.** The first version read *"below ~10 ticks the reload is shorter than the fire cooldown
> of every quiver weapon — `quiver_stone` 11, Boltor 14 — so it is invisible: you could not have
> fired during it anyway."*
>
> **Premise 1 was contradicted by line 15 of this same file.** *"Slice C's **7-tick** dual cooldown
> is priced on an unmeasured floor"* — and four more citations say the same. The quantity that
> matters is not a *declared* cooldown but **the shortest interval a player can achieve, which is
> 7.** At 7 a 10-tick floor is not invisible; it is three ticks of felt brake. The argument did not
> merely fail to support 10 — **it pointed the other way.**
>
> **Premise 2 was false outright: a reload does not overlap the cooldown.** Measured — `beginReload`
> ← `tryReloadHeldWeapon` ← `WeaponSwingListener:104`, **one path, the left-click swing**, and
> `case EMPTY` starts nothing, so there is no auto-reload. **The reload begins when the player
> chooses to swing.** In the ordinary pattern — fire until refused, then reload — **every tick of
> the reload is additive and felt, whatever the cooldown is.** The premise was true of one play
> pattern and stated as a property of the mechanism.
>
> **With "invisible" gone there is no mechanism argument left for any number above 1**, and picking
> one anyway would be a balance call disguised as a derivation — which the slice that is supposed to
> rule the final value would inherit by reading rather than re-deriving.

> **AND THE REPLACEMENT DID IT AGAIN, WHICH IS THE MORE USEFUL HALF.** The first version of this
> paragraph said *"1 is the minimum that keeps the mechanic representable — exactly
> `Durability.MIN_USES`'s argument."* **That analogy is false.** Measured against
> `Quiver.reloadComplete`'s `if (now >= completesAt) return true;` — at `reloadTicks` 0,
> `completesAt == startedAt == now`, so the reload matures on the same tick. The state stamps,
> matures and clears; the restart guard is unaffected. **A zero-tick reload is FULLY
> REPRESENTABLE.** `MIN_USES` prevents a *different and irreversible* state — the item destroyed
> rather than inert — which is why that floor is structural. **A reload floor prevents no state at
> all.**
>
> **So the genre repeated: 10 was a balance number wearing a MECHANISM argument; 1 was a balance
> number wearing a STRUCTURAL one.** The number got smaller and more honest and the justification
> did not change kind. That is worth more than the constant, and it is why both retractions stay.

**What the floor actually is: A NAMED PLACEHOLDER AT THE NO-OP VALUE.** There is **no mechanism
argument for any floor, including this one.** `MIN_RELOAD_TICKS = 1` is a documented hook where the
balance slice finds the question — chosen because it **changes nothing a player can observe** and
because it **matches the authored floor `WeaponDefinition:109` already enforces.**

The thinnest true statement, and it is deliberately thin: at 0 the `RELOADING` state is never
observable for a single tick; at 1 it is observable for one. **That difference is invisible at 20
tps.**

**AND IT RESOLVES THE TWO-FLOOR PROBLEM BY BEING THE SAME NUMBER.** `WeaponDefinition:109` already
refuses `quiverSize > 0 && reloadTicks <= 0`, so the **authored** floor is 1. A resolved floor of 10
would have meant an author could ship `reload_ticks: 3` — below the minimum the resolver enforces —
**two literals that can drift apart, which is the defect the capacity javadoc quotes `ResourceCost`
for three paragraphs earlier.** At 1 they are one number enforced at two layers, and the constant
says so.

**The balance floor is still owed, and the chain is named so the next reader sees a chain rather
than a constant:** *what reload duration still brakes* depends on the shortest achievable fire
interval, which is **7**, which is priced on **Q7 — unrun**. The first slice that ships
reload-reduction gear rules it, because that is the first time anyone can feel it.

**And the downward direction is covered in CORE, since no fixture can cover it** — a row that
resolves a reload below the floor and asserts the floor holds, plus the mutation that removes the
floor. That is the **only** witness this slice will have for the direction gear actually moves.

---

## COMMIT SPLIT — measured against A1's own shape

A1 ran **15 commits**; largest 12 files / +573, median ~4 files / ~180 lines. A2's budget is
**40–55 files** (re-measured: `4a453ac` 19 files/+1461, `8362262` 21/+1501, `48b8db9` 26/+1439), so
**eight commits** in that range.

| # | commit | files | what |
|---|---|---|---|
| **0** | the boot record | 1 | `GATE-quiver.md`: V1/Q12/V2 blanket-green, **Q7 UNRUN** |
| **1** | **the stamp seam — NO stats** | ~8 | 4th PDC key; `QuiverState.capacity`; `reloadVerdict` loses its param; `setLoaded` stamps capacity and clamps; tooltip reads the stamp. **Behaviour-identical** — capacity is still authored, so the golden must not move. That is the commit's own verification. |
| **2** | quiver size, core half | ~6 | `core/combat/QuiverSize.java` (`NONE`/`boosts`/`contribution`), `HealthState` (**8 members**), `CombatantStats` (**2**), tests |
| **3** | quiver size, paper half | ~7 | `Keys` pair, `QuiverSizeModifierItems`, reconcile line, dev command, fixture — **and `setLoaded` switches to reading the stat** |
| **4** | reload time, core half | ~5 | same shape; base is the authored duration |
| **5** | reload time, paper half | ~7 | same shape — **and `beginReload` reads the stat**, the second and last supply site |
| **6** | `/rpg stats` lines | ~5 | see the signature note below |
| **7** | prose + gate | ~5 | corrections below, `GATE-quiver-a2.md` — **and the two boot-only rows below, which it must not be descoped without** |

### COMMIT 1 RAN AS SIX — recorded because the split was not planned, it was forced

The seam was budgeted at one commit of ~8 files. It landed as `1a`–`1f`, and **every split after the
first was a review finding, not a decomposition I chose.** Written down so the commit-count estimate
above is read against what actually happened rather than against itself.

| # | sha | what forced it |
|---|---|---|
| `1a` | `270810a` | the three ordered sources; the floor's argument was false on both premises |
| `1b` | `01a52e2` | the stamp seam — and the guard missed the defect it was written for (`MUTINLINE` arrived through `capacityIn`, not `capacityOf`) |
| `1c` | `460443c` | `MUTSTAMPDROP` was **green**: the 4-arg `WeaponLore.build` had zero test callers |
| `1d` | `83b83de` | `MUTSTATEDROP` green too, and `MUTAPPLYLORE2` alongside it — the class, not the instance |
| `1e` | `336dbec` | the guard did not scan its own new front door (`QuiverState.from`) — third widening |
| `1f` | *this* | **the pin guarded the static surface; the constructor was not on it** — fourth. Closed by making `QuiverState` a final class with a private constructor, so it is *unrepresentable* rather than scanned for. |

**The shape of all four: a guard aimed at the NAME of the right thing rather than the SHAPE of the
wrong thing.** `1f` is the first one that stops widening the list and removes the door instead.

### TWO KNOWN-GREEN MUTATIONS ON THE LIVE PATH — recorded HERE because the gate does not exist yet

**`GATE-quiver-a2.md` is commit 7 and is not written.** Both of these were first promised to it by a
javadoc — *"named in GATE-quiver-a2 rather than left to look covered"* — which pointed at a file
that does not exist. **This branch already has that precedent**: the `ContentValidator` arm was
named in advance in four places and then was not there. So they live in the commit table, which
exists now, and descoping commit 7 cannot silently discharge them.

| mutation | site | status |
|---|---|---|
| replace the stamp with `empty` in `Quivers.stateOf`'s five-value read | `Quivers.java` | **GREEN and will stay green.** The decision moved to `QuiverState.from` and is covered; what remains is argument-passing. |
| `MUTAPPLYLORE2` — pass `empty` while keeping the `capacityInMeta` call | `WeaponItems.applyLore` | **GREEN and will stay green.** This is the live in-game tooltip path. |

**Both need an `ItemStack` and therefore have no unit test.** Each needs a boot row: *equip a
capacity modifier, reload, and read the tooltip* covers the second; *fire to the authored capacity
and press reload* covers the first — it must say **BEGIN**, not "full".

> **AND THE PRICE IS A STANDING PROJECT DECISION, WHICH IS THE REUSABLE HALF.** `new ItemStack(...)`
> throws *"No RegistryAccess implementation found"* without a server and the parent pom's only test
> dependency is `junit-jupiter` — recorded independently at `WeaponItemsTest:14`,
> `GoldenLoreTest:39` and `GridClickIntentTest:16`. That is a **choice the project has already
> made**, not a property of the universe, and it prices every future placement decision:
>
> **ANY DECISION PLACED IN `paper` IS PERMANENTLY BOOT-ONLY.**
>
> So *"does this need to be a decision in `paper`?"* is a design question with a **known price**,
> not a matter of taste. It was answered correctly twice in this chain — the verdict moving to
> `QuiverState`, then the capacity resolution moving to `QuiverState.from` — both times without
> naming the rule. Named now.

**Commit 1 is the seam that makes the "two call sites" claim true**, and it is separately
verifiable precisely because it changes no behaviour.

> **`StatsSheet.build` is an 8-argument positional signature** (`StatsSheet.java:47`), and two stats
> take it to ten. `StatsSheetTest:44` and `:120` are positional and will break. **Commit 6 should
> introduce a parameter object rather than a ten-argument call** — flagged because it is a real
> design decision hiding inside a mechanical slice.

---

## Per-stat cost, measured — so commits 2–5 are not underestimated

Traced through `manaRegenBonus`, the most recent stat:

- **`HealthState`: 8 members** — the `Stat` field, six accessors (`…Value`, `set…Modifier`,
  `clear…Modifier`, `…ModifierAmount`, `…ModifierSources`, `…ModifierCount`), and the
  `ModifierTarget` factory — across three non-contiguous blocks, ~78 lines with javadoc.
- **`CombatantStats`: exactly 2** — the value accessor and the reconcile entry.
- **`Keys`: 2 lines.** **`*ModifierItems`: a new ~100-line file.** **`PlayerHealthSystem`: 1
  reconcile line** plus ~10 comment lines. **`RpgCommand`: ~6 registration + ~16 handler + 2
  imports.** **Core helper: a new ~50-line file.**
- **`CombatantSnapshot`: ZERO.** Neither stat is read at cast/hit time across a region hop — both
  are read on the caster's own thread in `Quivers`. `manaRegenBonus` did not touch it either.

---

## Prose corrections this slice owes

1. **`PlayerHealthSystem.java:171` AND `:177`** — *"Eleven stats"* and *"all eleven"*. **TWO count
   words, not one**; my earlier note named only the first. Both become thirteen. *The irony goes on
   the record: that comment carries a note saying it was "phrased now so it cannot go stale again",
   and it still leads with a count.*
2. **`quiver_stone.yml`** — its header calls `quiver_size` and `reload_ticks` the values themselves.
   They become **authored bases feeding stats**. Correct it where it says constants.
3. **`QuiverItems.stampFull`** — its javadoc says the mint stamps the capacity; it must now say
   **authored** capacity, and why that is the headroom rule rather than a compromise.
4. **`WeaponLoreLines.java:30-33`** — the *"same number"* rule gains the quiver as its stated
   exception, with the deeper-principle argument. **Do not delete the rule**; it still governs
   attack damage and attack speed.

---

## Verification

- `./mvnw -pl core test` per commit; `./mvnw clean package` as the **final** verify with the suite
  figure **re-read after the last file lands**, quoted as `core / storage / paper`.
- **Commit 1 must leave `golden-lore.txt` byte-identical** — it is a no-op seam, and the golden is
  the check that says so. Regenerating it in commit 1 would be the tell that something moved.
- **Mutations**, each with the marker grepped **both** directions plus a measured line/byte delta,
  restored from a scratchpad copy: the clamp inside `setLoaded` (capacity decrease must clamp);
  `reloadVerdict`'s removed parameter (reintroduce it and pass a divergent capacity — the
  condition-1 defect); each stat's `boosts()` gate; and **the entanglement mutation the two
  fixtures exist for** — wire the reload scanner to the size key and assert the size row reddens
  while the reload row does not.
- **Mutations RUN so far**, with their results, so a later reader is not left to assume:
  `MUTINLINE` (red after the scan was widened to `capacityIn`), `MUTSTAMPDROP` (green → fixed →
  red), `MUTSTATEDROP` (green → fixed → red), `MUTFACTORYDROP` (red), and **`MUTCTOR` — make
  `QuiverState`'s canonical constructor public; 1 of 6 rows red, and only the constructor assertion,
  not the static-surface one.** `MUTAPPLYLORE2` is green and stays green; it is in the boot-only
  table above.
- **Q7 remains owed** and is not discharged by this slice.
