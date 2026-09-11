# GATE — the quiver, slice A1

**DRAFT. Not run.** Written before the boot, deliberately: *a gate row written after the boot is
worthless*, and five of these rows are the only witness the verdict mapping will ever get.

Branch `feat/quiver`, tip `c30d26a` at drafting. `./scripts/dev-server.sh`.

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
| **V2** | `EMPTY` | fire 9, then press again | refused, the empty notice **seen** — **and NOTHING SPENT**: no cooldown tripped, no resource taken. Press again immediately: it must refuse **at once**, not wait out a cooldown. *That is what `WeaponFire`'s gate ordering exists for and it has to be observed, not assumed.* |
| **V3** | `RELOADING` | left-click, then press fire twice ~1s apart | refused, with **ticks remaining shown**, and **the number MOVES between the two presses**. *A frozen counter reads identically to a live one at a glance — "a number appeared" is not the test.* |
| **V4** | `RELOAD_MATURED` | **stage deliberately:** empty the magazine, left-click, wait for the reload to finish, then press fire | **the shot FIRES.** Its pass condition is the **PRESENCE of a bolt**, not the absence of a refusal. |
| **V5** | `UNSTAMPED` | — | **NOT STAGEABLE. See below.** |

> **V4 IS THE ROW TO WRITE MOST CAREFULLY, AND IT IS NOT OBVIOUS WHY.** Mid-reload the item's stored
> count is **0** — the reload has not refilled it yet. An empty-first implementation therefore
> **swallows the press that finishes the reload**: the magazine refills, but that shot never
> happens, and the next press works fine. **It reads as input lag, not as a bug**, and nobody
> reports input lag on a crossbow. `QuiverStateTest.theShotThatMaturesAReloadIsNotDropped` pins the
> verdict; this row is the only thing that pins the wiring around it.
>
> **Stage it on purpose rather than hoping to hit it**: the window is every press after the deadline,
> so an easy staging is to empty, reload, wait two full seconds, then fire. If the first press after
> a completed reload produces no bolt, V4 has FAILED even though the weapon works on the second.

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

- **The quiver lore line — OWED AND NOT YET BUILT.** `quiver_stone`'s tooltip currently shows **no
  count**: `WeaponLore.build` sees only the definition. **When it lands this gate gains one row** —
  *the tooltip shows 9/9, and 8/9 after one shot* — and that row is named here now so it cannot go
  missing. **Do not run this gate as final until it exists**, or A1 closes with an unwitnessed
  display.
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
alone.
