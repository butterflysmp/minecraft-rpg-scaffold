# GATE — the quiver, slice A1

**RUN 2026-09-11.** Branch `feat/quiver`, tip `5de5346` at the boot. `./scripts/dev-server.sh`.

> ## RESULT: V1 FAILED ON ITS DISPLAY HALF. EVERYTHING ELSE ANSWERED BY BLANKET.
>
> **The evidence class is recorded before the rows, because it changes what each tick is worth.**
> The operator answered *"all gates are good"* — one blanket — with a single detailed observation on
> V1. So every row below is **per-row in shape and blanket in evidence**, except V1.
>
> | | |
> |---|---|
> | **V1** | **FAIL (display half).** Bolt **YES**, count **9 → 8**, and *"the number doesn't change in the lore of the item"*. The mechanism half passes — the magazine is not infinite. The tooltip does not move. |
> | **Q12** | **FAIL.** Not covered by the blanket: it is the same observation V1 made, staged deliberately, so with no re-render it reads `9/9` then `9/9`. Recorded as a fail rather than inherited as a pass. |
> | **Q7**, **V2** | **UNRUN.** Both need a *value*, and "good" is not one — see below. |
> | everything else | **blanket-green.** |
>
> **Q4, Q5 AND Q6 PASSING WAS CONSISTENT WITH THE DEFECT, NOT EVIDENCE AGAINST IT.** Relog,
> `/rpg refresh` and the enchant table all route through `remint`, which *does* call `applyLore` — so
> the tooltip snapped to the correct number at exactly those three moments. The bug presented as
> *"the counter only updates when you relog."* **Three greens that look like the display working.**
>
> **THE ROW WAS RIGHT; THE REPORTING FORM NEARLY LOST IT.** Q12 was written *before the lore line
> existed*, named in advance so it could not go missing — and it did not go missing. It was
> **answered by a blanket that could not see it**, and what surfaced the defect was the operator's
> one freehand note on V1. A blanket cannot distinguish a row it checked from a row it skipped; that
> is the same defect this file's own reporting section warns about, arriving through the answer
> rather than through the questions.
>
> **FIXED** in the commit that records this: `QuiverItems.setLoaded` writes the count **and**
> re-renders in one call, so a later write site cannot express one without the other, and
> `QuiversSignatureTest.quiversNeverWritesTheCountWithoutRenderingIt` fails the build if `Quivers`
> reopens the raw path. **V1's display half and Q12 must be RE-RUN.**

### Why Q7 and V2 are UNRUN rather than green

The operator has ruled that per-row figures are not worth recording on most rows, and he is right
about most of them — Q3/Q4/Q5/Q6 are binaries dressed as figures. **Two are not:**

- **Q7, the repeat floor** — a number that exists nowhere else, and the one that decides whether
  slice C's dual-wield halving buys anything at all. ***"Good" has nothing to be good against.***
- **V2, which message appeared** — *"good"* cannot separate the empty notice from *"On cooldown"*,
  and that separation **is** the row. A blanket over V2 records that something refused, which was
  never in doubt.

---

## Staging

> **Kill orphaned `java.exe` first.** The script dies; two JVMs do not. They hold the deployed jar,
> `rm -f` fails with *Device or resource busy*, `set -e` aborts the deploy, and the server boots the
> PREVIOUS build — a stale-boot trap that looks exactly like a code change having no effect.
> Confirm the deployed jar's mtime is newer than `target/`'s before trusting any row.

---

## What this gate is the SOLE WITNESS for, and what it is not

The arithmetic (`Quiver`) and the verdicts (`QuiverState`) are core-tested: 17 rows across
`QuiverTest`, `QuiverStateTest` and `QuiverSignatureTest`, with 14 mutations observed red. **This
gate does not re-witness any of that.**

What no test can reach is the **verdict → side effect mapping** in `Quivers`, which needs a
`Player` and a live `ItemStack`: the enum has **five** arms, and each maps to a different piece of
Bukkit work. **So five rows, each naming its own verdict.** A blanket "all green" cannot carry a
mapping — that is `GATE-cursed-emerald.md`'s recorded defect, where two sentences covered a whole
gate and G5a's per-setting figures were never taken.

---

## V — the verdict mapping. FIVE ROWS, ONE PER ARM

| # | verdict | staging | pass condition |
|---|---|---|---|
| **V1** | `FIRE` | `/rpg give quiver_stone`, fire once | **BOTH halves.** A bolt lands **and** the count goes 9 → 8. *A shot that fires without decrementing is an infinite magazine, and it looks identical to a working weapon for the first eight shots.* |
| **V2** | `EMPTY` | fire 9, then press again **immediately** | **RECORD WHICH MESSAGE APPEARS.** **PASS** = the empty notice, *"Your quiver is empty -- left-click to reload."* **FAIL** = *"On cooldown for 0.Xs"* — the 9th shot stamped an 11-tick cooldown, so the empty check is running **after** the cooldown and `WeaponFire`'s gate ordering is wrong. **THIS ROW'S FAIL MESSAGE RENDERS ONLY BECAUSE OF A CONTENT DECISION — see below.** |
| **V3** | `RELOADING` | left-click, then press fire twice ~1s apart | refused, with **ticks remaining shown**, and **the number MOVES between the two presses**. *A frozen counter reads identically to a live one at a glance — "a number appeared" is not the test.* |
| **V4** | `RELOAD_MATURED` | empty the magazine. Left-click to reload, **then RELEASE the fire button entirely.** Wait for the reload to complete. Then **one** deliberate press. | **PASS = a bolt on that press.** **FAIL = no bolt, even if the next press works.** The release is load-bearing — see below. |
| **V5** | `UNSTAMPED` | — | **NOT STAGEABLE. See below.** |

> **V2's DISCRIMINATOR DEPENDS ON A FILE V2 DOES NOT MENTION, AND THAT COUPLING IS WRITTEN AT BOTH
> ENDS.** The row can only FAIL while `quiver_stone.yml`'s `on_hit` is a plain `type: damage`.
> Change it to `weapon_damage` — which someone would plausibly do to make the fixture more realistic,
> and which is exactly what `hunters_bow` does — and `DamagePayload.isBasicAttack` becomes true,
> `firesABasicAttack` returns early, `RpgListeners`' switch is never reached, and **the FAIL message
> stops rendering entirely.**
>
> **Nothing would look wrong.** The row still reads correctly, a PASS still shows the empty notice,
> and the only thing that changed is that the failure has become unobservable. That is a rule
> outliving its premise — the arithmetic still evaluating after the premise is gone — and the
> counterpart note lives beside the `on_hit` in `quiver_stone.yml`, because **a one-ended note is the
> half read by whoever is not editing that file.**
>
> **V4 IS THE ROW TO WRITE MOST CAREFULLY, AND IT IS NOT OBVIOUS WHY.** Mid-reload the item's stored
> count is **0** — the reload has not refilled it yet. An empty-first implementation therefore
> **swallows the press that finishes the reload**: the magazine refills, but that shot never
> happens, and the next press works fine. **It reads as input lag, not as a bug**, and nobody
> reports input lag on a crossbow. `QuiverStateTest.theShotThatMaturesAReloadIsNotDropped` pins the
> verdict; this row is the only thing that pins the wiring around it.
>
> **AND THE STAGING MUST RELEASE THE FIRE BUTTON, OR IT CONCEALS EXACTLY THE DEFECT IT EXISTS FOR.**
> The fire input is **hold**-right-click. An operator who holds through the reload — the natural
> thing to do, and what a player does — gets the swallowed press followed by a successful one **one
> repeat-interval later**, which is on the order of a tenth of a second. **A bolt appears, the row
> reads as a pass, and the failure is invisible.**
>
> So: release the button, wait out the reload, and make **exactly one** press, so there is only one
> press to observe. *"No bolt on that press, even if the next press works"* is only a checkable
> statement when the row guarantees a second press has not already happened.

### V5 — `UNSTAMPED` is MECHANISM-UNREACHABLE, and this gate does not pretend otherwise

**RULED: option (b). `UNSTAMPED` is witnessed by its core row alone, and this file says so rather
than carrying a row nobody can run.**

The arm fires only for an item minted by a path that does not stamp a count. **Since the carry
landed there is no such path** — every mint and re-mint routes through `WeaponItems.mint` or
`GearItems.carryInstanceData`. Staging it would need an item hand-built with vanilla `/give` and a
raw `custom_data` component carrying `rpg:weapon_id` but not `rpg:quiver_loaded`.

**That was considered and refused, and the reason is this seat's own standing rule.** Getting the
component slightly wrong yields an item the plugin does not recognise as one of ours **at all** —
which fails the `weapon_id` lookup long before the quiver gate, produces no warning, and **passes
the row by never reaching the code it claims to test.** A control succeeding for the wrong reason,
on the one row whose subject is a guard nobody can otherwise see.

**So, stated plainly rather than hidden in a pass:**

- the **verdict** is covered by `QuiverStateTest.anUnstampedQuiverIsADefectAndNotAnEmptyMagazine`;
- the **side effect** — `warnOnce`, stamp full, write back — is **witnessed by NOTHING**, and the
  forward-cover note in `Quivers`' `UNSTAMPED` arm says so in the code too;
- **if the operator wants it witnessed**, the honest instrument is a dev command that STRIPS the key
  from a held item (`/rpg quiver strip`, ~15 lines) — a clearly-marked dev path, *not* a mint path
  that fails to stamp, which would be the very defect the arm guards against. **Not built. Ask.**

---

## Q — the carry, the schema, and the fixture

| # | check | expected |
|---|---|---|
| **Q1** | boot log | clean load, **zero skipped content**, and **no unknown-key warning from any shipped weapon** — the `WeaponLoader` guard staying quiet on real content, at both key levels |
| **Q2** | `/rpg give quiver_stone`, fire and **count** | shot **9** lands; shot **10** refused, refusal **seen**. *Both halves named before the run: a stop-only row passes whether the capacity rule exists or not, since capacity and shots-to-empty are necessarily equal.* |
| **Q3** | left-click mid-magazine | reload starts, weapon dead for **34 ticks (~1.7s)**, then full at **9** |
| **Q4** | fire once, then **relog** | the count is **exactly 8** — the `carryInstanceData` row, via the **join refresh** |
| **Q5** | fire once, then `/rpg refresh` | same, via **`/rpg refresh`** |
| **Q6** | fire once, then use the **enchant table** | same, via the **enchant-table re-mint** |
| **Q7** | **the repeat-floor count** | see below |
| **Q8** | two `quiver_stone`s, one spent | **two halves, one new.** *They do not stack* re-witnesses `setMaxStackSize(1)`; **the spent one stays spent** is the NEW witness — per-item state, not per-weapon-id. A pass must say which half it saw |
| **Q9** | start a reload, swap to another item, swap back | **resumes** — does not restart, does not complete early. *This is what the reload living on the ITEM buys, and a per-player timer would fail it* |
| **Q10** | author a weapon file with a typo'd key — one top-level (`quivver_size`) and one inside a trigger (`cooldwon_ticks`) | boot **warns by name for each**, naming the weapon, and the trigger one names the **trigger** too. **Confirmatory**: both are already caused-not-asserted in `WeaponLoaderTest`, so this witnesses the wiring |
| **Q11** | fire once, left-click **on the next tick** | the reload **starts at once** — the fencepost ruling witnessed in play. Core row 8 is the authority |
| **Q12** | `/rpg give quiver_stone` and read the tooltip; fire once and read it again | **`Quiver: 9/9`**, then **`Quiver: 8/9`**. *The row this gate named before the line existed.* **If either reads `--/9`** the stamp is missing or `applyLore` ran before it — see below |

> **Q12 HAS A THIRD OUTCOME THAT IS NOT A PASS OR A FAIL, AND IT IS THE INFORMATIVE ONE.**
> `--/9` is the ABSENT rendering, and it means the item carries no count: either a mint path did not
> stamp, or the stamp ran after `applyLore`. **It is deliberately not `0/9`** — a spent magazine and
> an unstamped one must not read alike on a tooltip any more than they do in code, and the dashes are
> meant to look wrong.
>
> **Expect to see `--/9` in one legitimate place**: `golden-lore.txt`. That harness renders from
> DEFINITIONS with no item to read, so the absent form is the honest answer there and its presence in
> the golden is correct. **In game it is a defect.**

**Q4, Q5 and Q6 are the discriminating carry rows**: each names a **different funnel caller**, so a
pass says *which*. One of them failing while the others pass localises the break immediately.

---

## Q7 — THE REPEAT FLOOR. A COUNT, NOT AN IMPRESSION

**Nobody has measured vanilla's held-right-click repeat interval, and it is a floor under every
fire rate.** If it is slower than 7 ticks, slice C's dual-wield halving buys **nothing**: the weapon
fires at the repeat rate while the tooltip advertises 2×, and it reads as a balance opinion rather
than a bug.

**Method — and the cooldown must not be the limiter:**

1. Temporarily set `quiver_stone.yml`'s `cooldown_ticks` to **1** and its `quiver_size` high enough
   that the magazine does not run dry inside the window (**60**).
2. Hold right-click for a **fixed wall-clock window** — 10 seconds, timed.
3. **Count the shots.** Interval = window ÷ count, in ticks at 20/s.
4. **Run it on a CANCELLED binding** — i.e. exactly as shipped, since `quiver_stone` binds
   `right_click` and therefore suppresses the vanilla draw. *A measurement on an uncancelled binding
   is a different configuration and its number does not transfer.*
5. Restore the file.

**Record the raw count and the window, not just the derived interval** — the division is
recomputable, a remembered "about 5 a second" is not.

**Then write the relationship AT THE CONSTANT**, the way `BEAM_ORIGIN_GAP`'s ceiling is written:
nothing enforces it, so it is written where the number lives. Name the measured floor and the two
numbers that must stay above it — **14 single, 7 dual**. If the floor is above 7, **the operator has
a decision to make about what dual-wield actually buys**, and he should make it holding the number.

---

## What this gate does NOT cover, named so the absence is not mistaken for an oversight

- ~~**The quiver lore line — OWED AND NOT YET BUILT.**~~ **LANDED. It is Q12**, which is the row this
  section named in advance so it could not go missing. `WeaponLore.build` gained an `OptionalInt`
  overload, `applyLore` reads the count off the meta it is building, and the ordering that was free
  when the stamp was placed is load-bearing now. *The gate no longer has a reason to be withheld.*
- **Anything A2 owns**: quiver size and reload time as real stats, gear or enchants moving them, the
  rounding rule's dead zone. `quiver_stone`'s numbers are authored constants today.
- **Dual-wield, alternation, the Boltor.** Slices B and C.
- **`UNSTAMPED`'s side effect**, per V5.

---

## Reporting

**Per row, not by blanket.** `GATE-cursed-emerald.md` answered a whole gate in two sentences and its
per-setting figures were never taken; the Lapis gate's L10 shipped an expectation that working code
could not have met. **A row that cannot fail is worse than a row not run**, because it leaves a tick
behind.

For each: **PASS / FAIL / FINDING**, and for V1, Q2 and Q7 the **number observed**, not the verdict
alone. **For V2, the message TEXT**, not "it refused".

> **TWO ROWS WERE REWRITTEN BEFORE THIS GATE WAS EVER RUN, AND BOTH FOR THE SAME REASON: THE
> OBSERVATION WAS A JUDGEMENT WHERE A FACT WAS AVAILABLE.**
>
> - **V2** asked whether a refusal came *"at once"* rather than after a cooldown. At 11 ticks that is
>   a **0.55-second** difference judged by feel — the shape `GATE-lapis-staff.md`'s L5 already came
>   back soft on. But a cooldown refusal and an empty refusal print **different strings**, so the row
>   now records **which message**. Verified before the rewrite that both actually render for this
>   weapon: `quiver_stone`'s `on_hit` is a plain `damage`, so `DamagePayload.isBasicAttack` is false
>   and `RpgListeners`' switch is reached (`OnCooldown` prints); and `Empty` is handled at `:504`,
>   *before* the basic-attack silence at `:525` (the empty notice prints). **A row whose discriminator
>   does not render is a row that cannot fail.**
> - **V4** would have been staged by an operator **holding** the fire button, which is what the
>   weapon's input invites — and holding hides the swallowed press behind the next one.
>
> Both are the same defect class this gate's own header warns about, caught in the draft rather than
> in the results: *an observation that yields a plausible reading whichever way the code behaves.*
