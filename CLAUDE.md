# CLAUDE.md

Read this before writing any code in this repository.

## Commands

```bash
./mvnw clean package     # build all modules
./mvnw -pl core test     # unit tests (fast, no server)
./scripts/dev-server.sh  # build + deploy + boot a local Paper server
./scripts/check-crlf.sh  # is any file mixed CRLF/LF? RUN BEFORE TAKING CR READINGS
```

`check-crlf.sh` guards the INSTRUMENT, not the repo: a part-CRLF/part-LF file makes
`tr -cd '\r' | wc -c` return a number that is neither. **It is a REQUIRED STEP, not a tool that
exists** — *VERIFICATION* says when to run it and what to report.

Always use the wrapper, never a system `mvn`. It pins Maven 3.9.9 so the build is
reproducible; there is no system Maven on this machine.

Always run `./mvnw -pl core test` after changing anything in `core/`. Prefer adding
a unit test to `core/` over testing in-game. In-game testing is a 60-second loop;
unit tests are a 2-second loop.

## Environment (verify before assuming)

| Thing | Value |
|---|---|
| Java | **25** (required by Minecraft 26.1+) |
| Paper API | pinned in `pom.xml` as `paper.version` |
| PacketEvents | pinned as `packetevents.version`, `provided` scope, **not shaded** |
| Minecraft versioning | year-based since 26.1 (`26.1`, `26.2`, ...), not `1.21.x` |

## BANNED PATTERNS

Your training data is saturated with Bukkit tutorials from 2015-2023. Most of
what you will reach for first is deprecated or wrong here. Do not write:

| Never write | Write instead |
|---|---|
| `plugin.yml` | `paper-plugin.yml` |
| `onCommand`, `CommandExecutor`, `TabCompleter` | Brigadier via `LifecycleEvents.COMMANDS` |
| `ChatColor`, `§` codes, `ChatColor.translateAlternateColorCodes` | Adventure `Component`, MiniMessage |
| `BukkitRunnable`, `Bukkit.getScheduler().runTask*` | the `Scheduler` interface in `paper/scheduler` |
| NBT reflection, NMS, `CraftPlayer` casts | Persistent Data Containers, `Keys.java` |
| `getServer().getPluginManager().registerEvents` sprawl | one registration point in `RpgPlugin` |
| Hardcoding an ability/weapon/boss in Java | a YAML file in `content/` |

If you believe an exception is warranted, say so and ask. Do not just do it.

## THREADING — the rule that will actually break production

**PacketEvents callbacks run on Netty I/O threads.** They are not the main
thread and not a Folia region thread.

- Inside `onPacketReceive` / `onPacketSend`: read the packet, compute, cancel.
  **Touch nothing else.**
- To reach the Bukkit API from a packet callback, use
  `PacketListenerBase.bukkit(player, () -> ...)`. There is no other sanctioned route.
- Never call `player.sendMessage(...)`, `world.spawnParticle(...)`,
  `entity.setVelocity(...)` or anything else Bukkit from a packet thread.

This bug does not reproduce with one player on a test server. It corrupts state
at forty. Treat a violation as a build-breaking error.

Separately: all scheduling goes through `Scheduler` (`onEntity`, `onRegion`,
`onRegionLater`, `onGlobal`, `async`). This exists so the project runs on Folia
later without a rewrite. `async` must never touch the Bukkit API.

## WHERE THE RULES LIVE — decided 2026-09-10, after the split happened twice

**`CLAUDE.md` carries the rule in the form that changes what you do next. `NEXT.md` carries the named
rule, its worked examples, and the history of what it cost to learn.**

Two families had already split across both files — the mutation-lies table (here, examples there) and
the control-rules pair (operational form here, named rules there). **Two occurrences is a convention
forming by accident**, so it is stated rather than left: a reader looking for "the rules" should not
have to know which family a rule is in before knowing where to look.

**Why this way round, and the argument is about who reads what.** This file's own first line is *read
this before writing any code*, and it is loaded every session; `NEXT.md` is **10,966 lines** read
on demand (measured at `0dd9bbe`; this said *"nine thousand"* until 2026-09-15). **A rule that lives only in `NEXT.md` will not be read by the person about to break it.**
The cost is that this file grows, and it is paid down by keeping each entry here to the operational
core — what to DO — and leaving the persuasion, the worked example and the dated instance to
`NEXT.md`.

**Overturnable.** The opposite convention — everything in `NEXT.md`, pointers here — keeps this file
short, and if it grows past being readable in one sitting that is the trade to revisit.

### AND THE GENERAL FORM, BECAUSE TWO HOMES IS NOT ONLY A `CLAUDE.md`/`NEXT.md` PROBLEM

**WHEN ONE RULE MUST APPEAR TWICE, ONE COPY IS THE POINTER AND ONE IS THE ACCOUNT, AND EACH SAYS
WHICH IT IS.**

- **The pointer carries the form that changes what you do, and nothing else.**
- **The account carries the mechanism, the measurements and the consequences.**

**TWO ACCOUNTS DRIFT INTO DISAGREEMENT AND NEITHER IS THEN TRUSTWORTHY** — a reader who finds two
explanations of one rule has no way to tell which was updated. **And a pointer that has grown a second
explanation has become an account: cut it back.**

**Two tests it must pass, and they are the reason the split works:**

- **The pointer must be obeyable without opening the account.** *"Author multiples of 4"* can be
  followed by someone who never reads the mechanism. **A pointer that needs its account to be useful
  is a broken copy, not a pointer.**

  > **THIS EXAMPLE USED TO READ *"multiples of 4; multiples of 8 if it may ever be dual-wielded"*, and
  > the second half was withdrawn on 2026-09-13** when the dual-wield design was refused — see the
  > rule itself under *Standing decisions*. **The quote is updated rather than left standing, because
  > an example that quotes a rule verbatim goes stale exactly when the rule moves** — and a worked
  > example of a *well-formed pointer* that misquotes its own pointer is the failure it is teaching
  > against. The example's point is untouched: the surviving half is still obeyable without the
  > account, which is the property being demonstrated.
- **The pointer names a SECTION, never a line.** `WeaponLoader`'s `cooldown_ticks` section, not
  `WeaponLoader.java:175`. A line citation is falsified by any insertion above it, silently and
  invisibly to every test — this repo carries an open finding measuring that blast radius at ~140
  sites, produced by exactly the other choice.

## VERIFICATION — THE NAMES. THE ACCOUNT IS `.claude/rules/verification.md`

**A check that did not run looks exactly like a check that passed.** The account — every mechanism,
every measurement, every dated instance — is `.claude/rules/verification.md`. It loads on its own
whenever you open a file under `**/src/test/**` or `scripts/**`. **When you are about to verify
something and are NOT in one of those paths, open it yourself: nothing will do it for you.**

**This index is NAMES, and that is the whole job.** The name is what makes someone think *this is a
hollow fixture*; the account is what they read next. **Each name is a SECTION heading in that file,
so it is greppable** — and none of them is reproduced here, because two accounts drift.

| family | reach for it when |
|---|---|
| *(unheaded)* — **the ambient bullet list**: markers, the four false absences, the false presence, file-list reconciliation, figures and their anchors, pushes, integrity hashes, estimates, provenance | you are about to write down that something was verified |
| **A PREDICTION THAT SEVERAL OUTCOMES SATISFY IS NOT A CONTROL, IT IS A RANGE** | you predicted a shape in words and are checking it against numbers |
| **TWO RULES FROM ONE REVIEW, AT THE SEAM BETWEEN A FIGURE AND THE SENTENCE ABOUT IT** | a revision touched a line it was not revising, or a general form disagrees with its own worked rows |
| **EVERY FILTER AND EVERY SCRIPTED EDIT NEEDS A POSITIVE CONTROL** | a `perl -i`, a grep filter, a needle or a count — anything whose silence you are about to read as success |
| **THE EIGHT WAYS A MUTATION LIES, AND EACH GUARD IS BLIND TO THE NEXT** | you are running a mutation and about to believe the marker grep |
| **AND THE ARM THAT MOST EARNS ITS KEEP IS THE ONE MOST LIKELY TO BE UNREACHABLE** | you are writing a guard for a case no shipped content can produce |
| **A HOLLOW FIXTURE** | a row may never present the condition it claims to test — in SPACE, TIME, KIND or OBSERVABILITY |
| **THE CREATIVE-DIVERGENCE REGISTER — THE BENCH IS NOT THE GAME** | you are writing or reading a `GATE-*.md` at all: R0, the declared game mode, the standing debt |
| **THREE THINGS THAT DO NOT ANNOUNCE THEIR OWN ABSENCE** | a planned guard silently failed to land, or a comment names a hazard and is being read as a guard against it |

**THE FIRST ROW HAS NO HEADING IN THE ACCOUNT AND THEREFORE NO NAME** — 721 of that file's 1630
lines, 44% of it, carrying most of the rules anyone quotes. *The ambient bullet list* is this
index's invention, not a quotation, and it is the one row a grep will not find. Giving it a real
heading is owed work, and it is deliberately not done here: this slice moves text and does not
reword it.

**MERGING, DELETING A BRANCH, OR WRITING A SQUASH BODY: `.claude/skills/merge-procedure`.**
`git branch --merged` prints NOTHING for a fully merged branch here and two-dot `git diff` inverts
once `master` moves — every PR on this repo is squash-merged. Run `./scripts/check-absorbed.sh
<branch>`; it has FOUR outcomes, not two.

## Architecture invariants

```
core/     pure Java. ZERO dependencies. No Bukkit, no Paper, no PacketEvents, no Gson.
storage/  PlayerRepository port + File impl. Async by contract.
paper/    adapters. The only module that knows Minecraft exists.
```

1. **`core/` must never import `org.bukkit`, `io.papermc`, or
   `com.github.retrooper`.** Its `pom.xml` has no dependencies on purpose. If you
   need a Bukkit type in `core`, you are modelling the wrong thing — define a
   port interface (see `CombatantHandle`, `CombatWorld`) and implement it in `paper/`.
   Reads and writes are separate ports: `CombatantSnapshot` is a value captured on the
   thread that owns the entity, `CombatantHandle` only dispatches. You cannot hop a
   thread and still return a value.

2. **Content is data, not code.** Abilities, weapons, elements' *instances*,
   loot tables, boss phases live in YAML under `content/`. `AbilityLoader` is the
   only class that knows the schema. Adding the 500th weapon must not require a
   recompile.

3. **Never assume one server.** No static mutable singletons holding player
   state. Player data goes through `PlayerRepository`. `Bukkit.getOnlinePlayers()`
   is not the source of truth for "who is playing".

4. **Prefer the Bukkit API over packets.** Every hand-written packet is a thing
   that breaks on protocol changes. Reach for PacketEvents only when the API
   genuinely cannot express the effect (custom UI, damage numbers, telegraphs,
   fake entities).

## Standing decisions — the operator's, not derivable from the material

A decision that isn't written down will eventually be contradicted by prose reasoned from first
principles. The reasoning will be sound; the premise was simply unavailable. Everything here is a
call that has already been made, so **do not re-derive it — and do not write a note that assumes
the opposite because the material suggests it.**

- **NO CUSTOM ITEM STACKS ABOVE 1.** Every weapon, tool, armour piece and shield we mint is a
  single item, whatever its base material does. `WeaponItems.mint`, `ToolItems`, `ArmorItems` and
  `ShieldItems` all call `meta.setMaxStackSize(1)`, at the source, so `/rpg give`, a craft and a
  re-mint all inherit it. Per-item state is why: durability, enchants and instance data are per
  item, and one write to a stack of two would edit both.

  > **The worked example is in this repo.** `flint_staff.yml` shipped a trap note reading *"THE
  > MINTED STAFF STACKS TO 64, because a stick does… isSimilar and will merge."* Correct reasoning
  > from the material, written **nine days after** `347967b` capped the mint at 1, and wrong on the
  > day it landed. It shipped in one PR and was copied into `lapis_staff.yml` in the next. Only a
  > gate row that physically stacked two staves caught it.

- **NEW CONTENT CITES THE BOLTOR, NOT `hunters_bow`, `ironblade` OR `quiver_stone`.** Those three are
  **in a deletion set** (ruled 2026-09-12, parked on *enough shipped weapons to replace them* — see
  `NEXT.md`). Every citation of one written between now and then is a comment that will point at a
  file that does not exist.

  **When a dev weapon is genuinely the only precedent, cite it AND MARK the citation** as standing on
  a weapon in the deletion set, so the eventual sweep finds it by **grepping for the marker** instead
  of re-deriving the reference graph.

  > **THE PLAN HAS A LOOP IN IT AND THIS IS THE FREE HALF OF THE FIX.** The precondition for deleting
  > the dev weapons is **more real weapons** — and new weapons are also **what adds references to the
  > dev weapons**, because the dev weapons are the precedents new prose derives from. `boltor.yml`,
  > the newest weapon in the project, **cites all three.** So every weapon authored between now and
  > the deletion raises the deletion's cost.
  >
  > It compounds quietly because it is **staleness, not breakage**: nothing fails, nothing is listed,
  > and the bill arrives later as a sweep nobody scoped. Measured at `2a3fb68`: **every mention of
  > these three in `main` is a comment except one**, so the deletion's real cost is prose, not code.

  **TWO THINGS TO KNOW ABOUT THE BOLTOR, SINCE IT IS NOW THE REFERENCE POINT:**

  - **Its numbers are RULED, not derived** — `quiver_size 8`, `cooldown_ticks 16`, `range 96`,
    `reload_ticks 60`, `attack_damage 19`. **`19` was ruled outright BECAUSE deriving it from
    `ironblade` was wrong**: `ironblade` is a dev weapon and is not balanced meaningfully.
    **So: never derive a new weapon's numbers from a dev weapon's.** Parity with a placeholder is
    parity with nothing, and it propagates — the derived number then becomes the next weapon's
    precedent and the placeholder's arbitrariness outlives the placeholder.
  - **`16` is on the 4-tick input grid, deliberately.** A held right-click's inputs are quantised onto
    a 4-tick grid, so a weapon's real fire interval is its authored cooldown **rounded UP to the next
    multiple of 4** — author `13` or `14` and you have authored `16`, **and the tooltip will not say
    so.** **Author multiples of 4.**

    > **THIS SAID "delivers an input only every 4 ticks" UNTIL 2026-09-13, AND THAT PERIOD CLAIM IS
    > FALSE.** `GATE-locust.md` row 1 read `INPUTS min 3t`, twice. **Holding right-click does not
    > produce a periodic stream at all** — operator's ruling, a property of the vanilla client.
    > **The grid survives and every prediction it makes survives with it**; what died is the sentence
    > explaining *why* there is a grid. Do not restore *every*, and do not change the 4 — the
    > arithmetic is confirmed at four measured points and the mechanism was never what the arithmetic
    > rested on. The mechanism, the measurements and the tooltip consequence are
    at `WeaponLoader`'s `cooldown_ticks` section — **this is the pointer, that is the account.**

    > **THE "MULTIPLES OF 8 IF IT MAY EVER BE DUAL-WIELDED" CLAUSE IS WITHDRAWN, 2026-09-13 — AND IT
    > IS WITHDRAWN FOR WANT OF A SUBJECT, NOT BECAUSE IT WAS WRONG.** The operator ruled that the
    > second Ranger weapon is a single item: *"it's not going to be Dual wielded."* **Nothing halves
    > any more**, so a rule about surviving halving has nothing to apply to. The `locust` ships at
    > **12** — not a multiple of 8, and correct.
    >
    > **`16` IS NOT RE-RULED, AND THE RECORD MUST STILL EXPLAIN WHY 16 AND NOT 12.** The clause was
    > one of the Boltor's reasons and its other reasons stand — the account at `WeaponLoader`'s
    > `cooldown_ticks` section, and the derivation in `boltor.yml`'s `cooldown_ticks` block, which
    > carries the same withdrawal note.
    >
    > **AND THE CLAUSE WAS UNSATISFIABLE ANYWAY, WHICH IS WHY THIS IS A WITHDRAWAL AND NOT A PAUSE.**
    > Halving moves a weapon onto a smaller cooldown, and **the attack-speed dead zone widens as the
    > cooldown shrinks** — `8 -> 4` is inert entirely, `16 -> 8` dead to +77.8%, `32 -> 16` to +28.0%.
    > To buy a *dual* dead zone as narrow as the Boltor's *single* +28% you must author **32**, a
    > 1.6-second shot. **Clean halving, attack-speed headroom and a fast weapon are jointly
    > unsatisfiable.** Full argument in `PLAN-locust.md`.

### NAME THE QUANTITY, AND NAME THE SET OF THINGS THAT HAVE IT

**Before writing any `value_by_level`: name the quantity the curve moves, and answer BOTH halves.**

1. **ARITHMETIC — is the quantity non-zero and continuous at the values it will meet?** If it is
   quantised, floored, or absent, **author a FLAT value instead** — or park the feature.
2. **ELIGIBILITY — does every piece of gear this can ROLL ON actually possess that quantity?** If
   some do not, **the gate is wrong, and no curve fixes it.**

> **THE TWO HALVES ARE ONE RULE, NOT TWO, AND THAT IS THE WHOLE POINT.** An enchant can be
> **arithmetically honest and still grant zero**, because the weapon it landed on does not possess
> the thing it modifies. **Passing the first half reads as passing** — the number is real, the
> multiplicand is real, and the player still receives nothing.
>
> **THE SAME ENCHANT FAILED BOTH HALVES**, which is why they cannot be separate rules: Expanded
> Quiver failed the arithmetic half (a percentage of an integer magazine floors to nothing) and was
> ruled FLAT — **and then failed the eligibility half anyway**, because `hunters_bow` is
> `class: ranger` and authors no `quiver_size`. Fixing the first did not touch the second.

**The failure is not a weak enchant. It is a tooltip that advertises a number while the player
receives ZERO**, and nothing goes red, because a curve that resolves correctly and lands on nothing
is indistinguishable from one that works.

Three enchants proposed in one slice (2026-09-13) produced **four** failures of this rule, across both
halves — which is why it is stated as a rule rather than four notes:

| proposed | half | the quantity | how it fails |
|---|---|---|---|
| **Rapid Fire** | arithmetic | a **QUANTISED** cooldown | the 4-tick input grid swallows anything under **+28%** |
| **Expanded Quiver** | arithmetic | an **INTEGER** magazine | `QuiverSize.arrows` floors — one arrow is 11% at 9 rounds, so every tier below that grants nothing |
| **Punch** | **eligibility** | a knockback base that **DOES NOT EXIST** on a ray | `applyDamage` never calls `entity.damage()`, so a ray hit raises no `EntityKnockbackEvent`; base push `0.0` |
| **Expanded Quiver**, *again* | **eligibility** | a magazine the weapon **does not have** | `hunters_bow` is `class: ranger` with no `quiver_size`, so the enchant rolls on and renders `+2 Quiver Arrows` for nothing |

**Quantised, floored, absent, unpossessed — four different mechanisms, one question catches all
four**, and it is cheaper than any of the four investigations that found them separately.

> **THE FOURTH ROW IS WHY THE RULE HAS TWO HALVES.** It was found only after the arithmetic half had
> already "passed" the enchant and a flat value had been ruled. **`+2 arrows` is `+2 arrows`** — the
> arithmetic test cannot see it. It arrived through the **ROLL TABLE** instead, which is a door the
> one-half version of this rule does not watch.

> **AND PUNCH IS THE ONE THAT SURVIVED, WHICH IS THE HALF OF THIS RULE WORTH KNOWING.** Operator
> ruling, 2026-09-13: *"Other ranged weapons will have knockback, the instant hitting ranged weapons
> don't."* A **travelling** weapon authors an `EffectSpec.Knockback` in its `on_hit`, and +20/40/60%
> of an authored `strength` is a real percentage of a real value. **Punch's numbers shipped as first
> ruled — the only one of the three that needed no renumbering at all.**
>
> **THE FIX WAS NOT A DIFFERENT CURVE. IT WAS NAMING THE QUANTITY.** The other two were repaired by
> changing the arithmetic — park it, or go flat. This one was repaired by **supplying the missing
> multiplicand**, and the curve never moved. So the rule's instruction is *name the quantity and
> check it*, in that order, and **not** *"prefer flat"*: two of three failures did need flat, and
> reading the rule as a preference for flat would have renumbered a curve that was correct.
>
> **The check is the same either way, and that is the point** — you cannot tell which of the two
> outcomes you are in until you have named the quantity and gone and looked at it.

> **THE THIRD ONE IS THE REASON THIS IS A CHECK AND NOT A REMINDER TO BE CAREFUL.** *Is this
> continuous?* is a question about a number you can see. *Does this base exist at all?* is a question
> about a call you have to go and read — and the plausible answer was the wrong one, because
> `VanillaDamagePolicy` says knockback rides the vanilla event and the ability path raises no vanilla
> event for that sentence to be about.

**Practically: write down the quantity's actual authored values before the curve.** The live
magazine spread is `8` and `12`; the live knockback base on a ray is `0.0`. Both took one grep and
one trace, and both were available before any design.

Full account, all three instances, and the Punch base trace: `PLAN-enchants-ranged.md`.

### A TRAVELLING RANGED WEAPON AUTHORS KNOCKBACK; AN INSTANT-HITTING ONE DOES NOT

**Operator ruling, 2026-09-13.** A `type: projectile` ranged weapon **lands with impact** and declares
an `EffectSpec.Knockback` in its `on_hit`. A `type: ray` ranged weapon is **hitscan** and declares
none.

```yaml
on_hit:
  - type: weapon_damage
    element: kinetic
  - type: knockback          # travelling weapons only
    strength: 0.4
```

**No new schema.** `EffectSpec.Knockback(double)`, `EffectApplier` and
`CombatantHandle.applyKnockback` all already exist; `AbilitySchema` already parses `type: knockback`.

**THIS IS A CONTENT RULE, AND THE CODE DOES NOT IMPLY IT — WHICH IS THE ONLY REASON IT NEEDS WRITING
DOWN.** Neither cast shape produces knockback on its own: `CastExecutor.detonate` is the single
on-hit site and **every** shape routes through the custom health store, so nothing calls
`entity.damage()` and no `EntityKnockbackEvent` is ever raised. **You cannot read this rule off the
engine — both shapes look identical there.** It is a decision about what content declares.

**This is why Punch does nothing on a Boltor, and that is CORRECT rather than a gap.** A ray is
meant to have no push.

> **SCOPE, STATED BECAUSE THE OBVIOUS SWEEP IS WRONG.** The ruling is about **ranged** weapons.
> Measured 2026-09-13: three shipped weapons are `type: projectile` and author no knockback —
> `ember_staff` and `flint_staff` (mage) and `emberblade` (melee). **This rule does not reach them.**
> Do not "fix" them, and do not cite this rule at a staff.
>
> **AND THEY ARE UNRULED, NOT EXCLUDED — THE TWO ARE ONE KEYSTROKE APART AND ONLY ONE IS TRUE.**
> Nobody has decided that a travelling MAGE weapon should not push. **The question was never put.**
>
> > **AN UNRULED CASE MUST BE RECORDED AS UNRULED, NOT AS EXCLUDED.** *"Excluded"* says a decision
> > was taken and closes the question; *"unruled"* says it is still open and invites it. **Writing
> > the first when the second is true silently converts an omission into a ruling nobody made** —
> > and the next person to look finds a settled-looking answer with no author.

**THE ROLL GATE, AND IT IS WHAT KEEPS THE TOOLTIP HONEST:**

> **PUNCH MUST NOT ROLL ON A WEAPON THAT AUTHORS NO KNOCKBACK.** Otherwise a player enchants a
> Boltor, the tooltip reads *"+40% knockback"*, and nothing happens — **the exact dishonesty the rule
> above eliminated three times, arriving a fourth time through the ROLL TABLE instead of the
> arithmetic.**
>
> It is **mechanically checkable**: the weapon's `on_hit` list either contains a `Knockback` or it
> does not. That is a loader / roll-eligibility check, **not a review obligation** — and it is the
> whole difference between *inert by design, visible in the content* and *inert in a way only a
> player discovers*.

**Punch is PARKED, design complete** — no weapon can take it today. Trigger and full design in
`NEXT.md`; the design itself in `PLAN-enchants-ranged.md` §4.

## Upgrade procedure

Do **not** bump `paper.version` alone. Order of operations:

0. **Notice the release. Nothing does this for you.** There is no bot on this
   repo, by decision — see `NEXT.md` D4. Check
   <https://modrinth.com/plugin/packetevents/versions> yourself. An absent
   notification looks exactly like nothing to notify, and this step is the one
   that silently never happens.
1. Check PacketEvents supports the new Minecraft drop. It typically lags a
   Minecraft release by 1–2 weeks. It is the gate.
2. Bump `packetevents.version` first, confirm it builds.
3. Bump `paper.version`.
4. Run `./mvnw -pl core test`. If `core` tests break on a Paper bump, `core` has an
   illegal dependency — that is the real bug.
5. Boot `./scripts/dev-server.sh` and smoke-test one ability end to end.

Never use version ranges. Pin exact builds so the build is reproducible.

## Working with me

- I am rusty at Java. If you use a language feature I may not know — sealed
  interfaces, records, pattern matching in `switch`, `var` — explain it in one
  line rather than assuming.
- Write the `core/` unit test before the `paper/` wiring.
- Keep changes small enough that I can read them.
