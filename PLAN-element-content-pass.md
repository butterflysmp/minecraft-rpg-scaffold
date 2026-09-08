# PLAN — the element content pass

**DONE. This stood between the accrual slice and its boot gate.** Not because the code was unfinished, but
because a gate run before it would have measured an interim state, and any tuning ruling taken from it would
have encoded that state as a requirement. *A tuning request is a measurement of current behaviour.*

Read `CLAUDE.md` first. The mechanisms this pass tunes are in `cad80f0`..`bc414eb`.

---

## It had TWO jobs, not three

A third — *"author the six missing `damage_symbol` glyphs"* — **was raised in review and does not
exist.** Measured:

```
$ grep -c '^damage_symbol:' content/elements/*.yml
fire 1   kinetic 1   nature 1   undead 1   void 1   water 1   wither 1
```

All seven were authored in `caa462e`. The glyph question that remains is **legibility on a real
client**, which is a gate observation rather than a content job — though it may *produce* a content
edit if a codepoint draws as a missing-glyph box. The candidate set is `✦ ◆ ✿ ☠ ✧ ≈ ✖`, and the yml
comments already mark it as boot-tunable.

---

## JOB 1 — RULED: shape (a). And it is a RECONCILIATION, not a buff.

**The framing in the first draft of this plan was wrong, and the correction matters more than the
ruling.** It called (a) *"a 2x-4x global scorch duration buff shipped as a cleanup"*, which invites a
reader to ask who approved it. `content/statuses/scorch.yml` already answered that, at the time:

> *"WAS `kind: fire` UNTIL THIS SLICE, AND THE EIGHT AUTHORED `duration_ticks` IN content/ KEPT THEIR
> VALUES WHILE CHANGING THEIR MEANING. `duration_ticks: 80` used to mean "burn for four seconds"; it
> now means "the stack window stays open four seconds, refreshed by each new stack." Mostly the same,
> and NOT the same for anything that applies scorch repeatedly ... No diff anywhere shows this; that
> is why it is written down here."*

**The eight values are not decisions. They are inheritance from the old vanilla-fire meaning, flagged
at the time as having changed meaning without being re-decided.** And 160 is not an arbitrary
constant either -- it is the operator ruling *"scorch only lasts for 8s by default"*, which is
exactly 160 ticks.

So (a) applies a ruling that predates the values in their current meaning, to values nobody ever
brought in line. **That is the reconciliation `scorch.yml` recorded as owed.**

### Why not (c) — and the argument that was used to reject it does NOT hold

(c) was rejected in review on the grounds that it falsifies `emberblade.yml:67`, *"that scorches all
it catches."* **Checked: that line is the `right_click` Fireball's own description, and the Fireball's
burst carries an explicit `status: scorch`.** Under (c) the explicit path still starts burns, so the
Fireball still scorches and the line stays true. It does not discriminate between the shapes.

**The weapon-level flavour does discriminate, and it points at (c):**

```
emberblade.yml:37-39
flavor:
  - "Its edge remembers the forge-fire."
  - "Swing to cut; loose to burn."
```

*"Swing to cut; loose to burn"* separates the arms explicitly. **So (a) inherits the very problem
that was used to reject (c)** -- after the strip, the swing burns too, and that player-visible line
becomes misleading. **Paid, not argued away:** the line is edited in this pass.

(a) still wins, on the reconciliation above and on `hunters_bow:31` (*"A swift arrow wreathed in
flame"*), which is true under (a) and was already true. **(b) stays refused** -- an element declaring
a duration is one field from declaring a rate, and the line was drawn at *name the status and nothing
else*.

### A falsified FLAVOUR line is worse than a falsified comment

This is the fourth falsified-prose find in the slice and the first in a string a **player** reads.

> **A falsified comment misleads a reader who can check it. A falsified flavour line misleads a player
> who cannot.** The sweep for prose that outlived its mechanism covers `flavor:` and `description:`,
> not only comments.

Every player-visible string on the branch was swept. `emberblade:39` is the only one (a) falsifies.

## JOB 2 — DONE: the nine explicit `status: scorch` sites are stripped

Every one had a co-located `element: fire` damage effect, so none lost its scorch — verified site by
site before the edit, not inferred from a count:

```
ability_stone:55  damage 8   <- status :56  |  solar_lance:30  damage 12  <- status :35
ember_staff:49    damage 16  <- status :53  |  rekindle:43     damage 8   <- status :44
flint_staff:116   damage 20  <- status :131 |  ember_step:24   damage 8   <- status :30
emberblade:83     damage 12  <- status :87  |  solar_grenade:36 damage 6  <- status :40
                                            |  solar_grenade:58 damage 2  <- status :62
```

`grep -rc "status_id: scorch"` over `abilities/` + `weapons/`: **9 before, 0 after.**

### And the answer to the question underneath

**`type: status, status_id: scorch` is no longer legitimate content.** Under (a) it says the same
thing `element: fire` says, from a different cap basis and a different stack rule, 120 lines apart in
the same file. It is not "tidying the nine" — it is that the two mechanisms are now one, and the
element is the one that owns it.

`type: status` remains correct for every status an element does NOT declare: `rooted`, `soaked`,
`freeze`, `surge`. `solar_grenade`'s `rooted_TEMP` fixture is untouched.

---

## What the pass changed, beyond the strip

**Four prose edits, three of them created by the strip itself.**

| file | what |
|---|---|
| `emberblade.yml:39` | *"Swing to cut; loose to burn."* → *"Swing or loose -- both leave embers."* The only player-visible string (a) falsifies. |
| `ember_step.yml:22` | the comment listed `status` among the per-mob effects; it no longer exists |
| `solar_grenade.yml:49` | *"It re-applies scorch"* → says HOW: each pulse's fire damage accrues it |
| `flint_staff.yml:119` | the long scorch-vs-old-DoT note was left anchored to nothing; re-anchored to the damage effect, and told that the duration moved from its authored 80 to 160 |

**One test re-pointed rather than retired.** `ScorchContentInvariantTest` counted
`KNOWN_SCORCH_APPLICATION_SITES = 9` and now counts `KNOWN_FIRE_DAMAGE_SITES = 12`.

> **IT WAS NOT SET TO ZERO, AND THAT IS THE WHOLE POINT.** A scan asserting it finds nothing cannot
> tell *correctly empty* from *the regex stopped matching the schema* — the defect `CLAUDE.md` records
> twice, and the reason that file exists. The cap invariant is carried by a damage amount either way,
> so the count moved to what now carries it. **There are more sites than there were explicit statuses,
> not fewer.**
>
> The pattern requires an INDENTED `element: fire`, because a bare one at column 0 is the owner
> element of a weapon, ability or kit — eight such lines exist, plus two kit keys and one line of
> prose in `fire.yml`. Counting those would have inflated the guard with declarations that carry no
> cap. **Measured, not guessed: 12.**

**The golden tooltip moved by exactly one line.** `GoldenLoreTest` reddened; the delta, read before
regenerating, was the `emberblade` flavour edit and nothing else — `git diff --numstat` said `1 1`.
**Stripping nine status effects moved no tooltip at all**, which is the right result: lore renders
damage, cadence and cost, never statuses.

---

## What is now true in game

**1. NEW SOURCES — wanted or harmless, source by source.**

| source | shape | per hit | verdict |
|---|---|---|---|
| `emberblade:58` | `weapon_damage` fire, 7 dmg, sweep 0.5 | ~3 stacks, ~1 per bystander | **wanted** — and the flavour line now says so |
| `hunters_bow:58` | `weapon_damage` fire, 6 dmg, `cooldown_ticks: 15` | ~3 stacks | **harmless, and it is the gate instrument** |
| `solar_grenade:23` | direct hit, 8 dmg | ~4 stacks | already covered by its own burst |

**2. DOUBLE APPLICATION — gone.** One path applies scorch: the element.

**3. DURATION — 160 everywhere**, per the reconciliation above.

**What survives the pass is the stack count**, which is the point of accrual and what Ignite is being
built to read.

---

## THE ONE THING SLICE 2 INHERITS, AND IT IS A STATEMENT RATHER THAN A NUMBER

**Ignite's threshold is UNDECIDED.** The recorded *"50% of max health as stacks"* is a denominator
mismatch, not a number to tune: stacks are ABSOLUTE (`max(1, floor(d/2))`) and that threshold is
RELATIVE, so for any hit of 2 or more, reaching it costs the target's entire health. **A 20 HP zombie
needs ten stacks; ten stacks is twenty damage; twenty damage is the zombie.** A Flint Staff's 20 buys
exactly ten and kills in the same hit — and accrual skips a lethal hit, so those stacks never land.

The only route through is the `max(1, ...)` floor, which inverts the design: Ignite would fire only on
the smallest mobs and only for weak repeated hits. Worked in full in `NEXT.md` beside the original
claim.

> **Slice 2 decides the DENOMINATOR first and the number second.**

**No stack ceiling is declared.** `ScorchStatus`'s refresh arm is `a.stacks += stacks` with no clamp,
so the bow can pile ~30 into one window — harmless while stacks do not scale damage, and a limit now
would be an undemonstrated policy wearing a safety check's costume, the same reason the glyph length
cap was declined. **The count's meaning needs deciding, not its maximum.**

---

## The gate, next — and the one row only a human can see

**The bow is the instrument, not a balance problem.** ~1.33 applications per second against a
160-tick window is roughly ten applications inside one burn. Nothing else in the repo applies scorch
fast enough to witness **refresh-without-burn** on a live client — `aRefreshDoesNotDealAnUNSCHEDULEDBurn`
guards it in `ScorchStatusTest`, and the bow is where a person can watch it hold.

> **DISCRIMINATING ROW:** shoot a high-HP mob repeatedly and watch the burn's cadence. The tick list
> must stay on its own 20-tick clock. **If re-application adds an off-schedule burn, that is the
> refresh defect visible in the only place a human can see it.**

Also wanted: glyph legibility on all seven elements; the armoured-target row that proves accrual is
post-mitigation; the `--refresh-content` verification chain; and `/rpg apply scorch 1 20` onto a live
accrued burn, which must NOT shorten it.

**The scorch rows are valid as tuning inputs for the first time**, because the interim state this
document was written to prevent measuring no longer exists.
