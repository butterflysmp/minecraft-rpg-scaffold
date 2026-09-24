# GATE — repeating-loop isolation: a crashed loop stops cleanly instead of wedging

**Status: R0 PASS (build 76d574d); L1-L9 NOT RUN, skipped by Ben's ruling 2026-09-24; L10 CANNOT BE RUN.** Every prediction below was written **before** any boot. Readings go **beside** a
prediction, never over it, and a prediction is not edited once its row has been read. **Ben's ruling,
2026-09-20: readings are verdicts, not figures** — a PASS cell carries no measurement unless the row
asked for one.

**Branch:** `fix/repeating-task-isolation`, off `e41a3c3`.
**Declared game mode: SURVIVAL**, and it is a formality here rather than load-bearing: nothing below
depends on a cost creative removes. Said anyway, because a gate that does not declare one costs
readings.

---

## *** THIS FILE IS SHORT ON PURPOSE. ALMOST NOTHING HERE NEEDS A BOOT ***

**The behaviour under change is unit-visible and is unit-tested.** `RepeatingTaskIsolationTest` drives
a real `RepeatingTask` against the clock fake with a throwing body and reads back all four properties
— the throw does not escape, `onStop` ran, `isRunning()` went false, a replacement starts — plus one
row per status proving its modifier is released. **Every one of those was mutation-verified** (see the
squash body), so writing boot rows for them would be writing rows whose failure a red suite already
reports faster.

**What a unit row CANNOT see is exactly two things**, and only they are below:

1. **That the nine rewired call sites still work at all.** The fix changed `RepeatingTask.start`'s
   signature, `EntityTaskTarget`'s constructor and three system constructors. Nothing in the suite
   boots a server, so *"the stats bar still appears"* is a boot question.
2. **That `EntityTaskTarget.loopFailed` reaches the real log with the right text.** The fake records
   the call; it cannot prove a `java.util.logging` SEVERE with a stack trace lands in `latest.log`.

---

## R0 — THE DEPLOYED BUILD CARRIES THIS SLICE. If R0 fails, STOP.

| # | prediction | instrument | READING |
|---|---|---|---|
| **R0a** | the build line names this branch's tip | `Select-String -Path run\logs\latest.log -Pattern '\[Rpg\] Build:' \| Select-Object -First 1` | PASS: [Rpg] Build: 76d574d |
| **R0b** | `RepeatingTask` in the deployed jar references `loopFailed` | the constant-pool scan below | PASS: loopFailed: PRESENT (controls: isRunning PRESENT, impossible string ABSENT) |

```powershell
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::OpenRead((Resolve-Path 'run\plugins\rpg-0.1.0-SNAPSHOT.jar'))
$e = $zip.GetEntry('io/github/butterflysmp/rpg/paper/scheduler/RepeatingTask.class')
if (-not $e) { 'RepeatingTask: ABSENT -- STOP' } else {
  $r = New-Object IO.StreamReader($e.Open()); $t = $r.ReadToEnd(); $r.Close()
  if ($t -match 'loopFailed') { 'loopFailed: PRESENT' } else { 'loopFailed: ABSENT -- the fix is NOT in this jar. STOP.' }
}
$zip.Dispose()
```

> **A jar is a ZIP and its classes are deflated, so `Select-String` on the jar itself returns nothing
> for everything.** The entry has to be inflated before any needle can match — the same instrument
> `GATE-quiver-feedback.md` R0d uses, and for the same reason.

---

## THE ROWS — ALL NEGATIVE. NOTHING SHOULD LOOK DIFFERENT

**Every row here predicts NO VISIBLE CHANGE**, which is the point: nine call sites were rewired and a
constructor gained a parameter in three systems. **A regression would show up as one of these loops
simply not running**, and a loop that does not run looks like a feature that was never built.

| # | what to do | PREDICTION | READING |
|---|---|---|---|
| **L1** | Join the server and look at the action bar. | The stats line is there and **updates** — health, quiver when held, defense, mana. `StatsBarSystem` is one of the two loops whose owner guards re-entry, so a wiring mistake here shows as a permanently absent bar. | NOT RUN (skipped, Ben 2026-09-24) |
| **L2** | Take damage, then stand still and watch health climb. | Passive regeneration still ticks. `HealthRegenSystem` is the other re-entry-guarded loop. | NOT RUN (skipped, Ben 2026-09-24) |
| **L3** | Equip and unequip a +HP armour piece and watch max health. | The reconcile loop still converges within a few ticks. `PlayerHealthSystem`'s loop stores no handle, so a failure here is silent rather than wedged — which is why it needs a row. | NOT RUN (skipped, Ben 2026-09-24) |
| **L4** | Look at a mob and watch its nameplate. | The nameplate still renders and tracks its health. | NOT RUN (skipped, Ben 2026-09-24) |
| **L5** | Root a mob (`/rpg apply rooted`), watch it freeze, and **let it expire**. | It stops dead, then **moves normally again** when the duration ends. The modifier release is now written in two places; a double-removal bug would show as a mob that never slows, and a missing one as a mob that never recovers. | NOT RUN (skipped, Ben 2026-09-24) |
| **L6** | Soak a mob, add a second stack, and let it expire. | It slows, slows further, then returns to **exactly** base speed. | NOT RUN (skipped, Ben 2026-09-24) |
| **L7** | Scorch a mob and let it burn out. | It burns on the normal period and stops; the mob can then be **scorched again**. | NOT RUN (skipped, Ben 2026-09-24) |
| **L8** | Kill a mob **while it is rooted**, and read the log. | It dies normally and **the log carries no `touched a removed entity`, no exception, and no `repeating loop` SEVERE line.** This is the row for the `isActive()` gate on the release — without it, cleanup reaches for the attribute of a mob that is gone. | NOT RUN (skipped, Ben 2026-09-24) |
| **L9** | Open the anvil and the grindstone, arm each, and close them. | Both arming animations still run and the menus still close cleanly. | NOT RUN (skipped, Ben 2026-09-24) |

> ### *** L8 IS THE ONLY ROW HERE THAT IS ABOUT A DEFECT THIS SLICE INTRODUCED ***
>
> The release rule, written the obvious way, added a `speed.removeSpeedModifier()` to `onStop` — and
> `onStop` also runs on `RepeatingTask`'s removed/dead arm, whose own comment is *"stop, touch
> nothing"*. **The existing `ImmobilizeStatusTest` and `SoakedStatusTest` caught it immediately**,
> because their fake speed attribute throws on any touch while inactive. The gate is on `isActive()`
> now and the unit rows are green, so **L8 is confirmation rather than the primary witness** — but it
> is the one behaviour where a wrong answer is this project's cardinal hazard rather than a cosmetic
> loss, so it gets a row.

---

## THE ROW NO UNIT TEST CAN REACH, AND IT CANNOT BE RUN EITHER

| # | what to do | PREDICTION | READING |
|---|---|---|---|
| **L10** | Make a loop body throw in play, and read `latest.log`. | One `SEVERE` line reading `repeating loop '<name>' threw and has been STOPPED for <player> (<uuid>)`, with a **stack trace**, appearing **once** — and the loop restartable afterwards (rejoin, or respawn). | **CANNOT BE RUN — see below** |

> ### *** THERE IS NO WAY TO MAKE A LOOP THROW IN PLAY, AND THAT IS STATED RATHER THAN LEFT BLANK ***
>
> **No shipped surface can inject a failure into a loop body.** Every one of the nine bodies throws
> only on a genuine defect, and there is no dev command, no content value and no player action that
> produces one. **So L10's condition cannot be staged**, and a blank cell here would read as an
> oversight rather than as a property of the system.
>
> **What this costs, exactly:** the message TEXT and the fact that it reaches `latest.log` are
> unwitnessed. `RepeatingTaskIsolationTest` proves `loopFailed` is CALLED, once, with the loop's name
> and the original cause — the fake records all three. What no row proves is that
> `EntityTaskTarget`'s implementation of it formats that into a log line a human can read.
>
> **It is a one-line `log.log(Level.SEVERE, …, cause)` against a `Logger` the plugin already uses for
> every other warning in the project**, which is the weakest part of this slice and the least likely
> to be wrong. Recorded as unwitnessed rather than argued into being covered.
>
> **If it is ever worth witnessing, the cheap route is a temporary dev command** that throws from a
> named loop — which would be an instrument with a deletion trigger, in this project's usual shape.
> Not built, because a permanent surface for crashing a loop is a worse thing to own than an
> unwitnessed log line.
