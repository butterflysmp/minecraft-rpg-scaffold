---
paths:
  - "docs/gates/**"
  - "scripts/**"
---

## BOOTING A GATE — THE BOOTER'S CHECKLIST. THE POINTER IS `CLAUDE.md`'s, UNDER *VERIFICATION*

**This is the account.** It loads when a gate file or a script is open; when you are about to boot and neither is,
open it yourself.

**Every step below is TICKED in the boot's report, one line each, with what it printed. None is remembered.** A step
whose line is missing from the report did not happen.

1. **The seat's word and Ben's.** The seat has diffed the code AND the gate from origin, and approved this boot.
   **Ben has said "ready" in his own words.** Neither stands in for the other.
2. **The heads.** `git ls-remote --heads origin <branch>` equals what the seat diffed. If it has moved, STOP.
3. **The gate's own CARRIED STATE**, in its order (no JVM, the backup and its counts, the config line, the staged or
   absent folders).
   **The tree is clean and pushed before the boot command:** `git status --porcelain` prints nothing, and `git
   rev-parse HEAD` equals `git ls-remote --heads origin <branch>`; both lines go in the report. A note written into the
   gate during the checklist is committed and pushed first, and the seat diffs the new head before the boot: a dirty
   tree boots a jar that is no commit.
4. ***"BOOTING NOW", IN ITS OWN MESSAGE, SENT BEFORE THE BOOT COMMAND IS SENT***, with: **"Ben: do NOT log in until
   I tell you R0 passed."** A message sent in the same turn as the boot command, or after it, does not count: Ben
   can read it only once the server is already up.
5. The boot command (`./scripts/dev-server.sh --refresh-content`), and the gate's REQUIRED deployed lines.
   **The wait for `Done` is matched to a log written after this boot's deploy, never to the first `Done` found:**
   `run/logs/latest.log`'s first line is timestamped after the deployed jar's mtime, and the `Done` read is in that
   file. A `Done` older than the deploy is the previous boot's.
6. **R0 read on THIS boot's log**: its `[Rpg] Build:` names the branch's head.
7. ***"R0 PASSED", IN ITS OWN MESSAGE, SENT BEFORE BEN LOGS IN.*** Any R0 not as predicted: STOP, tell the seat, and
   tell Ben NOT to log in.
8. **His login is checked against it:** the first `joined the game` line's time is reported beside the time R0 was
   read. If he joined first, that goes in the gate as a dated note, with whose omission it was.
9. After his `/stop`: the gate's after-run steps, the clean-stop lines, no `java` process left.

> **WHY THIS IS A CHECKLIST, 2026-11-18, THE SECOND BOOT OF `GATE-fight-arena.md`.** Steps 4 and 7 were in that
> gate's carried state, in capitals, and both were skipped: the boot command ran in the same turn as the reminder not
> to log in, and Ben was never told R0 passed. **He logged in at 22:08:55, before R0 was read.** The readings stood
> only because every R0 line predated his join (the seat's ruling). **A step that is written down and not ticked is
> a step that is remembered, and this one was not.**

> **THE TWO LINES UNDER STEPS 3 AND 5, 2026-11-23, `GATE-enchant-columns.md`** (accepted as worded by the seat). The
> first boot that day read `Build: 004ba1cc-dirty`: a dated note the seat asked for was written into the gate and not
> committed before the boot, and the boot was void. On the same boot the first `Done` matched was the previous boot's.
