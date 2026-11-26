# PLAN — the armor stack's owed items (the follow-up on master), 2026-11-25

The seat's merge approval of the armor stack (#300 to #307 and #304, merged as `ca482930`) named one follow-up PR on
master. It does not boot, and it sits at the bottom of the next stack. Every reading cited is the boot of `732c4e02`
(`GATE-armor-sets.md` and `GATE-summoner.md`, READINGS).

| item | what was done | where |
|---|---|---|
| AS15R | re-predicted: the mint read by the held item's lore, because armor logs no craft line (READ) | `GATE-armor-sets.md`, `GATE-armor-sets.yml` |
| SM9R | re-predicted: the death staged under P-1, a dealer whose SEEDED attack kills (200.0, EXECUTED); P-1 untouched | `GATE-summoner.md`, `GATE-summoner.yml` |
| SM16R2 | re-predicted: the probe's sum with the wolf's hits named, each term counted off its own lines | `GATE-summoner.md`, `GATE-summoner.yml` |
| the level seam | a lint rule, `LEVEL_UNSTAGED`, run over all 49 gates, its findings pinned; no old row edited | section 1 |
| REPLY drained late | READ and MEASURED: a reply can be logged after a LATER step's line; PROPOSED, nothing built | section 2 |
| Paper's notice | carried for Ben to schedule; nothing built | section 3, and `PLAN-armor-sets.md` O12 |
| the seat's two own errors | recorded in its words | `GATE-armor-sets.md`, THE FOLLOW-UP |

## 1. THE LEVEL SEAM, AS A RULE

**The defect:**
- His RPG level carries from row to row. The row-start CLEAN (`SelfTestRun`) restores blocks, removes mobs, puts the
  inventory back at a gate boundary and returns him to the start. It never touches his level.
- AS13 set 13, and AS14 to AS19 ran at it.

**The two ways the seat offered, with the cost of each:**

| | (a) THE LINT: a row after a level change stages its own | (b) THE RUNNER: the row-start CLEAN resets the setup's level |
|---|---|---|
| where it acts | before any boot, on the files (`ScenarioLint`, pure) | at every row start, on the live server |
| what it costs a row | one `rpg playerxp set @s <n> levels` and a `wait: 40` (the reconcile, `GATE-level.md` WD1), only in rows after a change | a write of his XP and a 40-tick reconcile at EVERY row start, or the runner tracking whether the level moved |
| what it costs a run | nothing for gates that never change his level | 40 ticks × 442 rows ≈ 14.7 min, unless it tracks the change |
| old gates | REPORTED, not edited (their readings stand on the scenario as it ran) | changed under them: every old row's staging moves, so an old reading no longer describes the row that would run |
| verified by | a unit test and the shipped-file pin, here, with no boot | a boot (it is runtime staging) |

**CC'S CALL: (a), and it is built.** Three reasons:
- It is the only one that needs no boot, and this PR does not boot.
- It edits no old row and changes no old row's staging.
- It is the shape the lint already has for HOLD, SELF_STAGED and SUMMON_UNSTAGED: a row stages what it reads.

**What (a) does not cover, named rather than claimed:**
- It cannot see whether a row DEPENDS on the level. It asks only that the level is staged once it has moved.
- A gate whose setup stages no level runs its first rows at whatever the run carried in (his own level on a run's first
  gate). The lint does not flag that.

**The rule as built** (`ScenarioLint.levelSeam`):
- The baseline is the setup's last `rpg playerxp set @s <n>`. When the setup sets none, the baseline is "none".
- A row that starts at a carried level other than the baseline, and sets none of its own, is a finding.
- A chained row (`after:`) is exempt; SELF_STAGED already names it.
- CC'S CALL: the row's set may sit anywhere in the row.
- **Tests:** `ScenarioLintTest.aRowAfterALevelChangeStagesItsOwnLevel` has six cases, CAUSED. Two mutations were run,
  and both went red:
  - the call dropped from `lint()`: 2 rows red;
  - the chained exemption dropped: 1 row red.

**What it flags over all 49 shipped gates** (pinned EXACTLY in `SelfTestScenariosTest`; `paper/target/scenario-lint.txt`):

| gate | row | carried | set by | the setup's |
|---|---|---|---|---|
| armor-sets | AS14 | 13 levels | AS13 | 1 levels |
| armor-sets | AS15 | 13 levels | AS13 | 1 levels |
| armor-sets | AS17 | 13 levels | AS13 | 1 levels |
| armor-sets | AS18 | 13 levels | AS13 | 1 levels |
| armor-sets | AS19 | 13 levels | AS13 | 1 levels |
| mage-guaranteed | MB1R | 1 levels | MG11 | none |

- **AS16 is not in the list.** AS15R, inserted ahead of it, stages 1 again. So from this commit AS16 runs at level 1,
  where it ran at 13 on `732c4e02`. That is said in the gate.
- **MB1R** rolls the fire weapons' pools 500 times and reads no level-scaled figure (READ, its steps). The lint cannot
  tell whether it depends on the level; it names the seam, and the judgement is the gate's.
- **The six existing "must lint clean" assertions exclude this one rule**, so that MB1R does not redden
  `mage-guaranteed`'s pin. The exact pin above is where the rule is held instead.
- **The three new rows lint clean on every rule**, asserted with a control that each is loaded.

## 2. "REPLY LINES ARE DRAINED A STEP LATE": READ, MEASURED, PROPOSED

**READ:**
- `ReplyReader` copies each system-chat and action-bar packet on the Netty thread into a queue.
- `SelfTestRun` drains that queue on his own thread, at four places: after each step's `drivers.execute`, at the start
  of every `advance()` (that is, after every wait), at `rowDone` before `DONE`, and at the run's end.
- Each line is logged with the row that is current WHEN IT IS DRAINED.

**MEASURED, the run log of `732c4e02`:**
- Paper sends the packet from the channel's event loop after the command returns, so the drain straight after the step
  often finds the queue empty, or only part of a multi-line reply in it. That timing is INFERRED from the reading below;
  `Connection.send` was not read.
- **The reading:** AS4's steps `rpg stats`, `rpg maxsummons 0`, `rpg give bone_whistle` logged:
  ```
  CMD /rpg maxsummons 0
  REPLY "❤ Max Health   100" … "  Mana Regen   5.00/5s"     <- rpg stats' reply
  CMD /rpg give bone_whistle
  REPLY "⛨ Defense      0" … "  Crit Damage  2.00x"         <- rpg stats' reply, still
  REPLY "max_summons: 0 (base set to 0)"
  REPLY "Given Bone Whistle"
  ```
- **The order of the replies is preserved.** There is one queue, FIFO, from one channel.
- **No REPLY crossed a row boundary in this run.** 1,378 REPLY lines were checked by two awk passes: none between a
  row's `START` and its `CLEAN`, and none after its `DONE`.

**So it is more than a late timestamp.** A reply can sit after a LATER step's `CMD` line. A reader who takes a REPLY
to belong to the nearest `CMD` above it attributes `rpg stats`' Defense line to `/rpg give`. **Not built; PROPOSED:**

- **P-R1, a reading rule (the cheapest; no code):** within a row, a REPLY belongs to the earliest unanswered command
  whose text it answers. It never belongs to "the CMD above it". Replies keep their order. If the seat accepts it, this
  goes into `verification.md`'s AUTO-reading account.
- **P-R2, close the cross-row case (code, needs a boot):** `rowDone` waits one tick and drains before `DONE`. Then a
  reply to a row's last step cannot be logged under the next row's name. That has not happened yet, and nothing
  prevents it when a row ends on a command with no wait.
- **P-R3, bind each reply to its step (code, needs a boot; NOT recommended):** stamping the step on the Netty side
  races in the same way. The send runs after the step counter has moved on.

**CC's recommendation: P-R1 now, P-R2 with the next selftest code change.**

## 3. CARRIED FOR BEN: PAPER IS FOUR RELEASES BEHIND

At boot, `run/logs/latest.log` of 2026-11-25, 16:28:45:
`[Paper Async Task Handler Thread - 1/WARN]: However, you are 4 release(s) behind the latest stable release (26.3)!`

This is the upgrade procedure's step 0 (`CLAUDE.md`, *Upgrade procedure*): PacketEvents goes first, because it is the
gate, and `paper.version` is never bumped alone. **It is an item for Ben to schedule. Nothing is built, and no version
is checked here.** It is carried as `PLAN-armor-sets.md` O12.
