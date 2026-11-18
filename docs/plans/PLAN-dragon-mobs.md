# PLAN — THE DRAGON-FIGHT MOBS: THE cfde822 SURVEY, AND BEN'S REWORK OF 2026-11-15

> **READ THIS FIRST. THIS FILE HAS TWO LAYERS, AND THE LATER ONE GOVERNS.**
> - **The REWORK, 2026-11-15** — the RULINGS block below, §3.R (four mobs), §4.2 (the capability table),
>   §5 (the build order), §7 (facts from the pinned jar) and the QUESTIONS list. Ben redesigned the mobs, so **five
>   mobs became four**: the Knell, the Phage (one name, five stages, on a CREEPER), the Phalanx, and the Skreel with
>   its mount. **The Goldrot Goon and the wither-skeleton Lesser Phage are retired.**
> - **The cfde822 SURVEY, 2026-11-06** — everything else: §1, §2, §3.1–§3.5, §4.1 and §5.0. **It is kept as the
>   record of what the old game did and is NOT deleted.** Every number in it is a fact about cfde822, never adopted.
>   Where the survey and the rework disagree, the rework governs, and the survey section carries a banner saying so.
> - **Still docs only.** No Java, no content, no boot. Nothing below is built. *(The rework's first-day state, kept as
>   the record; superseded by the status below.)*
> - **STATUS, 2026-11-17: ALL FOUR DRAGON MOBS ARE BUILT** (the Knell, the Phage, the Phalanx, and the Skreel with its
>   mount), **each with its gate read on a server**: the bodies in `GATE-dragon-bodies.md` and `GATE-dragon-fix.md`;
>   the Knell's in `GATE-dragon-beam.md`, `GATE-dragon-teleport.md` and `GATE-dragon-knell-2.md` to `-5`; the Phage's
>   in `GATE-dragon-split.md`, `GATE-dragon-growth.md`, `GATE-dragon-blast.md` and `GATE-dragon-blast-2.md`; the
>   Phalanx's in `GATE-dragon-shot.md` and `GATE-dragon-blink.md`; the Skreel's in `GATE-dragon-skreel.md`. **What
>   remains** is in the carried list (§8 FINDINGS, *CARRIED AT THE MERGE OF #203*), the open QUESTIONS FOR BEN, and
>   §3.R.5 *Numbers for Ben*.
> - **STATUS, 2026-11-18: THE PHAGE'S VOID IMPLOSION IS COMPLETE AS BEN DESIGNED IT:** the fuse and the reach scaled by
>   its size, the swirl, and the one-second drag (#204, #205, #206; read in `GATE-void-implosion.md`,
>   `GATE-implosion-drag.md` and `GATE-drag-additive.md`). **The `Implosion` shape is available to abilities and
>   weapons from content** (`type: implosion`); **none ships yet.** What remains is in §8, *CARRIED AT THE MERGE OF
>   #206*.

## RULINGS

*Ben's words are VERBATIM, his spelling included, copied from the seat's brief of 2026-11-15. Where the seat asked a
question, the question comes first, then his words. The seat's own readings and rulings follow, marked as the
seat's.*

### Ben, 2026-11-15, VERBATIM: the rework

**On the Knell:**
> "The Knell's two main abilities, Knell Beam, and it's teleport are completely independent of
> one another. The teleport is especially detrimental to Ranger players. Let's combine the two.
> The Knell can only teleport when it's Knell Beam hits a target (The word "Target" is important
> here because we will add a Summoner class later and we want it to teleport for summons too).
> Instead of a Ranger Provoking a teleport, it will teleport shortly after it's Knell beam lands"

**On the Phage and the Goldrot Goon:**
> "Phage and Goldrot Goon, I'd like to combine these two simple mobs into a more complex and
> unique mob. Instead of being a wither skeleton, I'd like this to be a Creeper base. We will
> retexture it with a resource pack later. We will keep the name of Phage. The Larger Phage will
> be about 3 blocks tall and very slow. When it gets close to a player it explodes dealing a lot
> of damage. When killed it splits into 2 fast Phage's with low HP. When they get close to the
> player they explode dealing a little damage. Here's the unique part. If enough Void mobs are
> killed nearby a small phage, they will grow back into a big Phage, allowing them to split once
> more."

**On the Phalanx:**
> "The Phalanx, there's a few problems with this one. The projectile is hard to see and even
> when you do see it there's nothing the player can do about it. I'd like to make it's
> projectile similar to the Shulker shot. Any attack destroys it. I'd also like to change the
> colors from black to something more visible like light purple and pink flashes. I'm not too
> familiar with the capabilities here with Shulker shots and the destruction. I'd rather not use
> an actual shulker shot but if we have to it's okay."

### Ben, 2026-11-15, VERBATIM: the seat's first follow-up questions

- **A.** *Where does the Knell teleport to (the old one sidestepped 4-5 blocks)?*
  > "The side step but I'd like to expand the range of it. I'm not sure how the old repo does
  > it but a degree arc from the player would be nice. Maybe between 30 and 70 degrees."
- **B.** *How long is "shortly"?*
  > "The same time between the warning shot and the actual beam to keep the feeling if the
  > mob. This should be a tunable number as well."
- **C.** *Is there a warning before it moves?*
  > "The hit itself is the warning"
- **D.** *What stops the Phage snowball (each regrown big Phage splits again)?*
  > "Nothing is in charge of the snowball, the players have to keep them in check. The ammount
  > of Void mobs killed as well as the range from the Phage should be tunable numbers."
- **E.** *When a big Phage explodes on its own, does it still split?*
  > "No, it only splits when killed, not exploding"
- **F.** *Can a player back away to cancel the explosion, like a normal Creeper?*
  > "Yes but I might change my mind in testing"
- **G.** *Does the explosion hurt other mobs?*
  > "Only players and Summons"
- **H.** *What counts as a "Void mob"?*
  > "Void as in Void element. The Phage will be a Void element mob and it is fueled by fallen
  > Void mobs. The Knell will also be a Void mob. The other two are Wither Mobs."

  **The last sentence of H is SUPERSEDED the same day** by Ben's additional note below.
- **I.** *How many deaths, how close, and from which causes?*
  > "Start with 4, let's try 6 block radius. Any deaths."
- **J.** *Names for the two sizes?*
  > "Both are called Phage. The distinction is between size"
- **K.** *Does the rest of the Phalanx stay (the blink and invisibility, the one-hit shield, the unfired bow)?*
  > "Yes"
- **L.** *Does "any attack destroys it" include area attacks that catch the projectile?*
  > "Yes"

### Ben, 2026-11-15, VERBATIM: the second round

- **On A.** *The seat read: the Knell reappears on the circle around its TARGET, at the distance it stood, 30 to 70
  degrees around, left or right.*
  > "A. Yes that's correct. We will need checks to make sure it doesn't small in a wall,
  > underground, or above ground as well."
- **On I.** *The seat read: a Phage is a Void mob, so a Phage's death, a self-destruct included, counts toward a
  nearby small Phage's growth.*
  > "I. Yes, I have an idea for an easier way about this. 5 stages of growth. The large Phage
  > is the 5th stage, the small Phage is the first. Only the 5th stage Phage splits. The small
  > Phage grow each kill by a fraction of the size."
- **An additional note:**
  > "One additional note, I'd like to change the Phalanx and Skreel to Void mobs as well, so
  > the whole fight is void. Also, normal Vanilla Enderman are also Void."
- **On Wither.** *Should Wither damage-over-time now land on the Knell (a Void mob on a `wither_skeleton` body, which
  `withering.yml`'s `immune` type key refuses today)?*
  > "Well come back to the wither effect"

### THE SEAT'S READINGS OF THE GROWTH STAGES — THE SEAT'S WORDING, **CONFIRMED BY BEN 2026-11-15** (*"Yes it's correct"*, below)

- **G1.** One counted Void death within the radius advances a Phage ONE stage. Stage 1 to stage 5 is four deaths,
  which is his *"Start with 4"*.
- **G2.** One death advances EVERY Phage below stage 5 within the radius, not only the nearest.
- **G3.** Stages 2–4 are in-between Phages: scale, speed, HP and explosion step evenly between the stage-1 and
  stage-5 values. They explode near a target and do NOT split when killed.
- **G4.** A stage 5 that is KILLED splits into two stage 1s. One that explodes does not (answer E).

~~**Until Ben confirms them, every place below that leans on G1–G4 says so.**~~ **CONFIRMED 2026-11-15**: G1–G4
now stand as ruled. Text below written before that date may still say "awaiting Ben"; this line governs it.

### THE SEAT'S MECHANISM RULINGS, 2026-11-15 (the seat's, stated in this file's words)

1. **The Goldrot Goon is RETIRED as a mob.** Four mobs remain: Knell, Phage (one name, five stages), Phalanx, and
   Skreel with its mount. The wither-skeleton Lesser Phage is gone.
2. **All four are element `void`.** A mob's element is authored in its file and names an existing
   `content/elements` id. A vanilla Enderman is Void and has no mob file, so the element also needs a content table
   from vanilla entity type to element (`enderman: void`), which a mob file overrides. §3.R.0 says where that table
   lives, what a creature in neither has, and what an element on a mob does on day one.
3. **"Target" is a combatant, never specifically a player**, in every new mob capability: the beam's hit, the
   teleport's anchor, the explosion's victims, the projectile's target. A summon then needs no rewrite. §7 F-E says
   what the engine's notion of "hostile to mobs" is today.
4. **The Knell's arrow dodge is REMOVED.** Its teleport follows its own beam landing on its target: a tunable delay
   (ticks) that starts equal to the beam's telegraph, then a move to a point on the circle around the target at the
   Knell's current distance, 30 to 70 degrees around (both bounds content), side chosen at random. A landing point
   is VALID only if the body fits in passable space there, there is solid ground directly beneath, and it is near the
   target's level (the tolerance is Ben's number). Try points across the arc on the chosen side, then the other
   side; if none is valid the Knell does not move. **A beam that misses moves nothing.** This needs no pre-damage
   hook.
5. **The Phage's numbers are content**: the stage count (5); for stage 1 and stage 5 the body scale, speed, HP, and
   the explosion's trigger distance, fuse, radius and damage, with stages between interpolated per G3; the split
   count (2); the growth radius (6 blocks); which element's deaths count (void). **A stage is state on the live mob,
   never a separate mob file.** Explosion damage is ours, priced by the mob-scaling rules, to players and summons
   only, with NO block damage and no vanilla explosion damage.
6. **The Phalanx's projectile is destroyed by ANY attack, area attacks included.** The seat leans to our own
   projectile (a small hittable carrier that our weapons damage through the normal path, wearing a resource-pack
   model, light purple with pink flashes), and rules between that and a real `ShulkerBullet` after §7 F-S.
7. **The Wither question is PARKED by Ben.** The Skreel's "Withered Mount" and any other wither-worded name now sit
   on Void mobs; that is a naming question for Ben and nothing more.

### Ben, 2026-11-15, VERBATIM: the questions that blocked step R1

*The seat's question, then Ben's words.*

1. *Are the seat's four growth readings G1–G4 right?*
   > "Yes it's correct"

   **G1–G4 are CONFIRMED** (above). Answers Q14–Q17.
2. *Does a stage 5's own death grow its two children, so they start at stage 2?*
   > "No,"

   Answers Q21: the children are born at stage 1.
3. *Does any death split a stage 5 (a fall, another mob), or only a kill by a player or a summon?*
   > "Any deaths"

   Answers Q26. **Answer E still stands: a Phage that EXPLODES does not split.** "Any deaths" is every way a stage 5
   can die; a self-destruct is not a death (§7 F-C), and E rules it out by name.
4. *When a Phage grows a stage, does it heal to full or keep the same share of its health?*
   > "Same share"

   Answers Q19.
5. *Is the Skreel's spider mount Void too?*
   > "No it's no element"

   Answers Q24. The mount has no element; it is in neither the mob file's `element` nor the vanilla table.
6. *The seat offered this table of starting bodies from the old game, for him to tune later:*

   | mob | size | speed | health |
   |---|---|---|---|
   | Knell | 1.6x | 0.6x | 480 in the old game / 360 in ours today |
   | Phage stage 5 | about 3 blocks tall | 0.75x | 160 |
   | Phage stage 1 | normal creeper size | 1.1x | 80 |
   | Phalanx | normal size | 0.65x | 240 |
   | Skreel rider | normal size | normal speed | 120 |
   | Skreel mount | 1.5x | 1.3x | 120 |

   > "Yes except the Phage will range from 1 block to 3 blocks in height. Speed
   > will range from 1.5x to 0.5x"

   **Ben did not choose between the Knell's two health values**, so **the seat rules that R1 leaves `knell.yml`'s
   `max_health` as it ships (360)**. It stays a number for Ben (§3.R.5 K3). The Phage's height and speed are Ben's
   words above; its health (80 and 160) is the table's, which he accepted.

### THE SEAT'S RULINGS OWED FROM #177, AND FOR STEP R1, 2026-11-15

8. **The Phalanx's projectile is our own small, invisible LIVING carrier with a display entity as its passenger,
   never a `ShulkerBullet`.** The reason is a fact: our weapons reach only living entities
   (`PaperCombatWorld.combatantsNear` keeps `LivingEntity` only, §7 F-S), so a real bullet would let every ability
   pass through it, against Ben's L. *(Ruled after #177 merged; recorded here, as the seat directed, in the next
   branch to touch this file.)*
9. **The per-stage Phage BODY moves from R2 into R1** (§5.R): the Phage's body cannot be authored without it. In R1
   the stage is set at spawn and nothing changes it.
10. **R1 leaves `knell.yml`'s health as it ships** (answer 6 above).

### Ben, 2026-11-15, VERBATIM: the split's gear score

*The seat asked: do the two children keep their parent's gear score, as slime children do?*
> "gearscore should be the same as the parent Phage."

Answers Q22.

### THE SEAT'S RULINGS ON #189 (STEP R1), 2026-11-15

11. **A mob file with no `element:` falls through to the vanilla table: ACCEPTED.** Absence is not an override.
12. **The `MiningWiringSignatureTest` needle update: ACCEPTED.**
13. **DB10's `CREEPERGUARD` count as a lower bound: ACCEPTED.**
14. **The mount's 1.3x speed, always on in R1: ACCEPTED.** R8 revisits it.
15. **The guard's production log line: ACCEPTED for R1.** Revisit at R3.

### THE SPLIT OFFSET (P6) — A STARTING NUMBER PUT TO BEN, NOT YET CONFIRMED BY HIM

The seat put the old game's offset to Ben as a starting number he may overrule: **half a block either side** of the
parent, along one axis (cfde822: +0.5 and −0.5 on x, §3.2). **It is NOT Ben's ruling yet**; it ships as content
(`phage.yml`, `split.spread`) so he can change it without a build.

### THE BUILD ORDER CHANGED: GROWTH (R4) IS BUILT AHEAD OF THE SELF-DESTRUCT (R3) — NOT YET BEN'S RULING

**What happened, 2026-11-15, stated plainly.** The seat proposed building the Phage's growth (§5.R step R4) before its
self-destruct (R3), because every growth ruling exists and the explosion's numbers (P13-P22) do not. The seat put it to
Ben as a yes/no. **His reply, verbatim:**
> "I'm still not gonna be home for another 90 minutes. Is there anything more we can do?"

**The seat TOOK that as a go-ahead.** It is **not a ruling on the order in Ben's words**: he did not say yes to R4
before R3. **The order is Ben's to confirm or reverse** (Q28). If he reverses it, R4 waits on its branch for R3.

### THE SEAT'S RULINGS ON #190 (STEP R2), 2026-11-15

16. **The split in its own MONITOR, ignoreCancelled handler beside `onEntityDeath`: ACCEPTED.**
17. **S1's `/rpg mobdamage 159` helper: ACCEPTED.**
18. **Noted, not blocking:** DS1 rests on an INFERRED fact (an iron sword's hit takes at least 1 custom health from a
    mob), and the split's first red was the mutation pass, not a run before the code existed.
19. **Merge order:** #189 first; each stacked PR is retargeted to master BEFORE its base branch is deleted.

### THE FIRST BOOT OF THE STACK (`bda8e89c`, 2026-11-15): THE SEAT'S MISSES, THE BOOT'S DEPARTURES, THE SEAT'S RULINGS

The readings are in the three gates on `feat/dragon-growth` (`39bb2e90`); the account of that boot is
`GATE-dragon-growth.md`'s *READINGS NOTE, 2026-11-15*. Most AUTO rows read NOT AS PREDICTED. The fix is
`feat/dragon-fix`, `GATE-dragon-fix.md`.

**THE SEAT'S MISSES, in the seat's words:**
1. The seat passed GATE-dragon-bodies.md's speed predictions by redoing arithmetic on speedDefault=0.25, without
   having the jar read for what AttributeInstance#getDefaultValue returns; that broke the seat's own rule against
   reasoning about Paper internals from memory.
2. The seat passed #190 without seeing that B2's and B6's cleanup lines kill a stage 5, which splits.
3. The run sheet let a hostile mob stand beside Ben before the difficulty was set; he was killed on hard in the first
   dragon-bodies run.

**THE BOOT'S DEPARTURES** (as the booter reported them):
- `/rpg selftest dragon-bodies` ran TWICE: first on hard (before `/difficulty normal`, and Ben was killed by a leaked
  Skreel), then on normal. The DB readings are the second run's.
- The B, S and G rows were posted to Ben in ONE message, at his request, and answered in one reply.
- Ben set his own gear score to 500 (`/rpg gearscore set 500`) before the sight rows.

**BEN'S SIGHT READINGS STAND:** B1-B5, S1, S2, G1, G2 "yes", as he gave them. **B6's first answer is recorded as he
said it, *"it stayed swelled until I move away from it"*, and is NOT AS PREDICTED**: the gate predicted swell, reset,
swell. (Why, from the jar: `GATE-dragon-fix.md`, D4.)

**BEN'S PROPOSALS, ACCEPTED BY THE SEAT AS MECHANISM:** automate B5, B6, S1 and S2, keeping two or three by-sight rows;
an ASK step in the selftest (the question in chat, his next chat line logged); and, the seat's addition, **the
selftest sets the difficulty itself**.

**Q30 is new and OPEN** (below). Nothing is built for it until Ben answers.

### THE BOOT OF THE FIX (`d59d2bf5`, 2026-11-15): THE SEAT'S READING, AND TWO CARRIED NOTES

The readings are in `GATE-dragon-fix.md`. **The seat's reading:** R0a-R0d PASS; 28 of 29 rows as predicted. The stack
was approved to merge on that boot (the dated line in each of the four gates' Status).

**Two notes, carried:**
1. **DB10R2 part (4) stays NOT AS PREDICTED.** The prediction said "equal" where it meant "did not fall": Ben's custom
   health rose from 166 to 176 over the row, with no damage line. The cause, regeneration, is INFERRED. It is not a
   pass, and it does not block: parts (1)-(3) held, and step R3 replaces this row.
2. **Not shown by this boot: the difficulty restore putting back a DIFFERENT value.** Every run read
   `DIFFICULTY found=NORMAL set=NORMAL` and restored `difficulty=NORMAL`, so the restore ran but put back the value it
   had set. A run started from another difficulty is the reading still owed.

### WHAT GROWTH (R4) LEAVES FOR THE SELF-DESTRUCT (R3)

- **A Phage's self-destruct counts as a Void death** (Ben's second round, I: *"Yes"*, the seat having read that a
  self-destruct is included). Nothing of ours explodes yet, so R4 cannot count it. **R4 leaves the seam**: one method
  R3's detonation will call to report "a creature of this element died here" without a death event (§5.R.3).
- **Q20 is still OPEN:** whether a Void death that happens while a Phage is mid-fuse still counts. R4 grows a Phage
  whatever it is doing, because no fuse of ours exists; R3 must rule it before it ships.

### Ben, 2026-11-15, VERBATIM: the blast's numbers (step R3)

*The seat asked, per size: how close before the fuse starts (vanilla 3 blocks), how far back cancels it (vanilla 7),
the fuse length (vanilla 1.5 seconds), the blast radius (vanilla 3), and the damage.*
> "for the explosion keep vanilla creeper numbers for everything except the damage"

### THE SEAT'S PROPOSALS FOR THE BLAST, 2026-11-15 — THE SEAT'S, NOT BEN'S. PROVISIONAL UNTIL HE RULES

*Put to Ben in these terms. R3 builds them as written; each is his to confirm or change.*

- ~~**P-a. Damage:** stage 1 deals 20, stage 5 deals 50 (the old Goon's blast), stages between linear, as G3 has the
  body.~~ **REPLACED 2026-11-17** (RULINGS, *the 60 and the 150, and knockback*): 60 at stage 1, 150 at
  stage 5, linear between: the seat's reading of Ben's "increase 3x". **CONFIRMED BY PLAY 2026-11-17**
  (RULINGS, the BQ1R2 answers).
- **P-b. Damage is multiplied by the Phage's gear score / 100**, as its health is (old Q3).
- ~~**P-c. Full damage to every victim inside the radius;** no falloff, no cover check, no knockback.~~ **REPLACED
  2026-11-16 by Ben's "keep vanillas shap"** (below): vanilla's reach and falloff; still no cover and no knockback.
- **P-d. Q30 unchanged:** a newborn child can grow from other deaths at once, as built.
- **P-e. Q20 unchanged:** a Void death during a fuse counts, as built.

### THE SEAT'S MECHANISM RULINGS FOR STEP R3, 2026-11-15 (the seat's, stated in this file's words)

20. **Because Ben's numbers are vanilla's, R3 keeps vanilla's swell as the fuse.** The creeper's own goal starts the
    swell inside 3 blocks and cancels it past 7 or without line of sight (§7 F-C), and the client animates it natively
    (`GATE-dragon-fix.md`, D4). **§4.2 N4's "replace the goal" is NOT built.** What that defers: the trigger and cancel
    distances are vanilla LITERALS, so they cannot be tuned, per stage or at all, until the goal is replaced; nor do
    they follow the Phage's scale. **Ben's "Yes but I might change my mind in testing" (F) is the ruling that would
    reopen it.** **SUPERSEDED 2026-11-17** by Ben's side note asking that the Phage's size scale its fuse radius
    (RULINGS, *the Knell's slab bug, and a void explosion that pulls*): N4 is REOPENED. Its build STOPPED at the jar
    (*PART C STOPPED AT THE JAR*), so vanilla's swell is still the fuse until the seat rules the route.
21. **The detonation is ours, at the prime.** When one of our creepers' `ExplosionPrimeEvent` fires, the event is still
    cancelled (no vanilla explosion, no block damage, ever), and in its place our detonation runs: damage to the
    victims, an explosion's sight and sound with no block effect, and the Phage **REMOVED WITHOUT A DEATH**. The
    ignite (flint and steel) and power (lightning) guards stay as they are.
22. **No split on a blast** (Ben, E: *"No, it only splits when killed, not exploding"*). Removal without a death event
    is what guarantees it; pinned by a test and by a gate row at stage 5.
23. **A blast is a Void death for growth** (Ben's second round, I). The detonation calls the seam growth left for
    exactly this (§5.R.3). The exploding Phage never grows itself; other Phages in its growth radius advance a stage.
    One blast is counted once.
24. **Victims: "Only players and Summons"** (Ben, G). R3 builds the ONE named predicate (§4.2 N3) for "a combatant a
    mob is hostile to"; today it is true for players only, and it is the one place a Summoner will widen. No mob is
    hurt, the Phage's own kind included. A creative or spectator player is not a victim.
25. **The blast in content, on the Phage, each quantity named:** the radius in BLOCKS (3), a sphere measured between
    the Phage's and the victim's feet positions, inclusive; the damage for the first and last stage (20 and 50), linear
    between, core-tested like the stage table; the fuse in TICKS (30), applied through the API if the jar offers a
    setter, else recorded as vanilla's and not authored. **The damage goes through the SAME path any mob's hit on a
    player takes**, so gear-score pricing and the player's defence apply exactly as for a melee hit. Whether the blast
    is Void damage is a question for Ben; no typing is invented.
26. **The guard's production log line** (accepted for R1, ruling 15, "revisit at R3"): the per-prime `CREEPERGUARD`
    info line goes; one line per detonation stays, naming the mob, uuid, stage, gear score, each victim and the damage
    dealt, so the gate reads the blast from the log.

**Also in R3, for the instrument (the seat's):** a damage line for the player during a selftest row (source, amount
before and after defence), because D5 stayed UNVERIFIED and DB10R2 mis-predicted for want of one; and every row says
what gear score and health it runs at and why the largest predicted blast cannot kill the player.

**NOT IN R3:** replacing the swell goal, knockback, falloff, block damage, any change to growth or the split beyond
calling the seam, the other three mobs. *(Falloff came IN on 2026-11-16, below: Ben's "keep vanillas shap".)*

### THE SEAT'S MISS ON THE BLAST'S RADIUS, AND BEN'S RULING ON ITS SHAPE, 2026-11-16

**The seat's miss, in the seat's words:** the seat told Ben a vanilla creeper's blast radius is 3 blocks, from memory.
3 is the explosion POWER; `ServerExplosion.hurtEntities` reaches `radius * 2` with falloff (§8 F2, read for #193). **It
is the seat's second miss on this work from an unread vanilla number** (the first: `GATE-dragon-bodies.md`'s speeds,
RULINGS, *THE FIRST BOOT OF THE STACK*, miss 1).

*The seat put three options to Ben: (1) vanilla's real shape, reaching about 6 blocks, full damage up close and fading
with distance; (2) as built, 3 blocks all or nothing; (3) a radius of his own; and told him the consequence of (2): the
swell starts inside 3 blocks, the Phage stands still while it swells, and one step back makes it blow harmlessly.*

**Ben, 2026-11-16, VERBATIM:**
> "keep vanillas shap"

**This answers Q33 and REPLACES the seat's provisional P-c** (full damage inside 3 blocks). P-a (20 and 50), P-b (×
gear score / 100), P-d and P-e stay the seat's provisional proposals, unruled.

### THE SEAT'S MECHANISM RULINGS ON THE BLAST'S SHAPE, 2026-11-16 (the seat's, stated in this file's words)

27. **Reach and falloff are vanilla's, read from the jar, never from memory** (§7 F-B: the reach for power 3, the two
    points measured between, the expression from distance to damage, and where exposure and knockback enter).
28. **Our damage × vanilla's curve.** The authored damage (20 at stage 1, 50 at stage 5, × GS/100) is what a victim
    takes at ZERO distance; at distance d it is that times vanilla's distance curve NORMALISED to 1 at zero and 0 at
    the reach. One named pure function in core (`MobBlast.falloff`), tested against values computed from the jar's
    expression in jshell. **Vanilla's damage constants and its +1 are not carried over; only the shape.**
29. **Content names the quantity:** `radius: 3` becomes the reach in BLOCKS as read (6), and the file says in words
    what a victim at half the reach takes (three eighths).
30. **Not built, recorded as questions for Ben, not asked yet:** cover (vanilla weakens a blast by the blocks between,
    Q34) and knockback (Q35). The blast still ignores walls and pushes no one.
31. **Everything else in #193 stands.** The seat ACCEPTED: the husk in place of a zombie; ruling numbers 20-26; the
    HURT line's `dealerType` not predicted; the fuse readable only by the unit test; BQ1 at GS 100; Q31 and Q32 open
    and parked; the Folia threading note in §8 (F3).

### THE BLAST AND THE ROWS ALREADY MERGED, 2026-11-16: THE SEAT'S MISS AND MINE, THE AUDIT, AND WHAT WAS DONE

**The miss, recorded as the seat's AND mine, both.** The brief for R3 did not ask what the blast does to the rows
already merged, and my report on #193 did not raise it. Those rows (`GATE-dragon-fix.md`, merged with #189-#192 on
2026-11-15) were written when a Phage could never explode. On #193 a live Phage within 3 blocks of the player swells,
blows, hurts him and is removed, and the `CREEPERGUARD prime` line some of them predict no longer exists. Their gate was
not on #193's run sheet, so the boot would have read clean and left rows on master whose predictions the code could no
longer meet, waiting for the owed full `/rpg selftest all` reading (ST9R2). **The seat found it in its diff of
`ba74df59`.**

**The audit** (every row of `GATE-dragon-bodies.yml`, `-split.yml` and `-growth.yml`, for a LIVE Phage that can come
within the swell's 3 blocks of the player, split children and Phages left live by an earlier step of the row
included). The jar facts it stands on: a creeper's swell rises by its direction every tick whatever its AI
(`Creeper.tick`); a fresh direction is −1 (`Creeper.defineSynchedData`); only `SwellGoal.tick` sets it to +1, and only
with a target within 3 blocks (`SwellGoal.canUse`); NoAI stops the goals (`Mob.isEffectiveAi`). **So a Phage frozen
from birth never swells, and every frozen SPAWN in these files is one.** The live ones:

| row | the live Phage | what it would read on #193 | done |
|---|---|---|---|
| DB10R2 | a live stage 1, two blocks east, 100 ticks | a BLAST and a HURT line, the Phage gone (its BODY reads none), no `CREEPERGUARD` line: (3) and (4) fail, and "never explodes" is false. Which tick: vanilla's AI (it primed three times in 100 ticks on 2026-11-15) | **RETIRED.** `GATE-dragon-blast.md` BL1 carries it |
| DQ1 | two live (stage 5 and 1), five blocks east, Ben walking away and back | each blows on him once within 3 blocks, at a tick his walking decides. Already ANSWERED (yes, yes) on 2026-11-15 | **RETIRED**, not re-asked |
| DS1R2 | two live children at 2.5 and 3.5 blocks, for 90 ticks | they primed four times before the TARGET probe on 2026-11-15, so each would blow on him first (22.5 to 60 before defence each, at GS 300 and the swell inside 3 blocks, the second perhaps grown a stage), and the TARGET probe would find fewer than two. Which ticks: vanilla's AI | **RE-PREDICTED** as DS1R3: children born frozen for S1, thawed for S2 |
| DS5R2 | two live children, 45+ ticks | they primed three times between its two counts on 2026-11-15, so the second count would read fewer than 2 | **RE-PREDICTED** as DS5R3, children born frozen |
| DS6R2 | none; reads DS5R2's children | stays true only if it follows DS5R2 | **MOVED with it** as DS6R3 |
| DS2R2, DG6R2, DG9R2 | two live children each, alive about 22-28 ticks before the next row's clean (a 20-tick death settle, 2 or 5 ticks of wait, the clean's own ticks INFERRED) | **probably the same lines**: a blast needs 30 ticks of swell after a target is found, and no prime was read in their brackets on 2026-11-15. But the margin is a few ticks and rests on runner timing that was not read | **RE-PREDICTED** as DS2R3, DG6R3 and DG9R3, the same steps, children born frozen |
| the other 17 rows | none (every Phage frozen, no split) | as read on 2026-11-15 | **unchanged, not re-run** |

**What was done.** (1) A split child of a FROZEN tagged parent is born frozen, in its pre-spawn consumer, and logs
`CHILD … frozen=true` (`SelfTestSnapshot.freezeSplitChild`). Freezing later would not do: a swell already begun runs
on to the blast. (2) The eight rows above left their files; their predictions and readings stay in
`GATE-dragon-fix.md`, each under a dated note. (3) The six re-predicted rows are in `GATE-dragon-blast.md` and its
scenario file, so `/rpg selftest dragon-blast` drives every changed row and the run sheet is unchanged. (4)
`SelfTestScenariosTest.noScenarioOutsideTheBlastGateLeavesALivePhage` pins on the FILES that no scenario outside the
blast gate spawns a live Phage or thaws one, with the blast gate's six as its control.

**S2 (the children come for him) is read in DS1R3**, from each thawed child's BLAST line: a creeper swells only at a
target within 3 blocks, its targets are players and whoever hurt it, and Ben is the one player. DS1R2's `TARGET`
probe is not re-read anywhere.

### Ben, 2026-11-17, VERBATIM, BY THE SELFTEST'S ASK STEP: the blast's damage — AND THE SEAT'S READING, NOT YET CONFIRMED BY HIM

At the boot of `bd7457a3` (`GATE-dragon-blast.md`, BQ1, and its dated section), the selftest asked:

- *"Your design says the small Phage explodes 'dealing a little damage'. Did that feel like a little?"*
- *"Your design says the big Phage explodes 'dealing a lot of damage'. Did that feel like a lot?"*

In the second run Ben answered each **"increase 3x"**. (In the first run, the run sheet's, he answered *"i didn't see
the damage"* and *"i didn't see the little phage damage"*, recorded as neither yes nor no.)

**THE SEAT'S READING, put to Ben and not yet confirmed in other words:** the authored damage goes from **20 to 60** at
stage 1 and from **50 to 150** at stage 5, replacing the provisional P-a. **NOT built in #193.**

### Ben, 2026-11-17, VERBATIM: the 60 and the 150, and knockback — AND THE SEAT'S READINGS OF IT

The seat told Ben it read his two "increase 3x" answers as: small Phage 20 to 60, big Phage 50 to 150, and that at
150 a big Phage at the feet of a 100-health player kills outright; and asked whether the blast should push players
back as a vanilla explosion does, since he had said he did not see the damage. Ben:

> "I didn't see the damage because I wasn't paying attention. But yes, knockback sounds like a good addition"

**THE SEAT'S READINGS OF THAT, 2026-11-17 (the seat's, stated in this file's words):**

- **The 60 and the 150 stand**, replacing the provisional P-a (stage 1 deals 60, stage 5 deals 150, the stages between
  linear: 60, 82.5, 105, 127.5, 150). **This is the seat's reading of "increase 3x", given twice, and of his not
  objecting when it was put to him, not a number he typed.**
- **Q35 (knockback) is ANSWERED: yes.**
- **His first-run answers to ASK 2 and ASK 3** (*"i didn't see the damage"*, *"i didn't see the little phage
  damage"*) **are explained by him** ("I wasn't paying attention") **and are not a verdict on feel.**
- **Still the seat's provisional and unruled:** P-b (× gear score / 100), P-d and P-e. **Still open:** Q31 (is the
  blast Void damage), Q32 (a shield cannot block it), Q34 (cover).

**Built in `feat/dragon-blast-2`:** the 60 and 150; vanilla's knockback, read from the jar (§7 F-B, *Knockback,
re-read 2026-11-17*), as a multiplier in content (1.0 today); the growth decision fixed at the death (§8 F5, F6); and
the instrument's `thaw` step (§8 F4).

**CONFIRMED BY PLAY, 2026-11-17 (the boot of `3b07cbee`, `GATE-dragon-blast-2.md` BQ1R2).** After a small Phage's
blast (33.333 before defence at 2 blocks) and a big one's (83.333), each pushing him with strength 0.666667, the
selftest asked and Ben answered, verbatim:

- *"Did the blast push you back?"* **"yes"**
- *"Your design says the small Phage explodes 'dealing a little damage'. Watch your health bar: did that feel like a
  little?"* **"yes"**
- *"Your design says the big Phage explodes 'dealing a lot of damage'. Watch your health bar: did that feel like a
  lot?"* **"yes"**

**The seat takes these as Ben's confirmation, by play, of the 60 and 150 damage and of knockback at 1.0.** They are no
longer only the seat's reading of "increase 3x".

### Ben, 2026-11-17, VERBATIM: the Knell's beam starts from the old game's numbers (step R5)

*The seat asked: may the Knell's beam start from the old game's numbers: fires every 5 to 10 seconds, warns for 0.6
seconds before firing (and that same 0.6 seconds becomes the delay before its teleport, as he ruled), reaches 40
blocks, deals 35 damage?* Ben:

> "yes"

**So K6 is 100 to 200 ticks, K7 12 ticks, K8 40 blocks, K10 35, and K11 starts at 12 ticks**, each a starting number
of his to tune (§3.R.5).

**THE SEAT'S, PROVISIONAL AND UNRULED:** beam damage is multiplied by the Knell's gear score / 100, as the Phage's blast
is (P-b).

**Not asked and not ruled:** K9 (the hit tolerance, old +1.0); whether the beam is Void damage; whether a shield blocks
it. Step R5 ships K9 at the old 1.0 as a content number, the beam with no element, and no shield block (§3.R.1.a).

### THE SEAT ON #195 (STEP R5), 2026-11-17 — PASSED BY DIFF, LEFT UNBOOTED SO ONE BOOT READS R5 AND R6 TOGETHER

**ACCEPTED (the seat's words, stated in this file's):**
- **I11's additions**, the transient MOVEMENT_SPEED × 0 modifier and the one-time zeroing of horizontal velocity at the
  lock, because the record's intent is a rooted Knell.
- **I23**: a NoAI Knell never starts a beam of its own.
- **The logged gap is the box-growth distance**, not a perpendicular one.
- **The report is this plan's "MB3 + E5"**, not `PLAN-legacy-port.md`'s E5 (the aim-locked wind-up).
- **The live cycle's bound is measured from the Knell's appearance** (I3).
- **The pen for the live rows.**

**FINDINGS, NOTHING BUILT:**
- **I24** (a Knell under our Freeze status still beams) is a question for Ben.
- **The 13 closed manual gates that spawn a live Knell** keep their readings and would not re-read as predicted on
  this branch.

**The "inherited or chosen" list I1-I24 has been put to Ben in plain words by the seat; he has not ruled on any item.**

### THE SEAT'S PROPOSALS FOR THE TELEPORT (STEP R6), 2026-11-17 — THE SEAT'S, PROVISIONAL AND UNRULED

*Put to Ben in these terms on 2026-11-17. R6 builds them as written; each is his to confirm or change.*

- **T-a. K14:** it tries 5 spots on its chosen side, then 5 on the other, then stays put.
- **T-b. K15:** the landing ground may be up to 3 blocks above or below the target's feet.
- **T-c. Q18:** a landing spot must have line of sight to the target, so the Knell never lands behind a wall.

**K11 (the delay) is 12 ticks**, Ben's "the same time between the warning shot and the actual beam" (B), its own number.
**K12 and K13 are Ben's 30 and 70** (A, *"Maybe"*), starting values.

### Ben, 2026-11-17, VERBATIM, BY THE SELFTEST'S ASK STEP: the beam and the teleport (the stacked boot of `e52e54b6`)

*Read in `GATE-dragon-beam.md` KQ1 and `GATE-dragon-teleport.md` TQ1.*

**The beam (KQ1):**
- Asked *"Did the hit feel fair for a single shot?"*, Ben:
  > "i think it needs to be more damage"
- Asked *"Was it possible to step out of the way in time?"*, Ben:
  > "maybe a slight increase in duration"

**The seat has asked him for the numbers:** the damage (K10, 35 today); the telegraph (K7, 12 today; the seat
suggesting 16 ticks); and whether the teleport delay (K11, 12 today) follows it. **NOT built.**

**The teleport (TQ1):** asked *"After it hit you, did it move to a new spot?"*, *"Could you find it again straight
away?"* and *"Does that feel fair to someone fighting it with a bow?"*, Ben answered each:
> "yes"

> "yes"

> "yes"

**The seat takes these as Ben's confirmation by play of the arc (30 to 70 degrees), the delay (12 ticks) and the
seat's provisional T-a, T-b and T-c, as they played.**

### THE STACKED BOOT OF R5 AND R6 (`e52e54b6`, 2026-11-17): TWO CARRIED NOTES

1. **THE ROOT ON A WALKING KNELL IS UNREAD.** `GATE-dragon-beam.md` KB2 read HOLLOW: its penned live Knell never
   walked (both AT reads and both BEAM lines at its spawn point), so "rooted during the telegraph" (I11) was never
   tested against a Knell trying to move. **Cause INFERRED, not read:** the pen's glass blocks the line of sight
   vanilla's targeting needs, so it never targeted the player. A fixture that can see him and cannot reach him is
   owed.
2. **KB4's TIMING NOTE.** The dated note predicted a `knell-gone` skip for KB4's Knell; instead it moved
   (`TELEPORT … tick=3567 … moved=true`, in KB5's bracket before KB5's CLEAN). The row ran 25 ticks past the force,
   not 23, so the move (the FIRE + 12) came before the next row's removal. **The code read right; the timing
   prediction was wrong.** The thin margin was accepted beforehand; nothing was changed.

### Ben, 2026-11-17, VERBATIM: the Knell's numbers after the boot, and the bell timing (the follow-up, `feat/dragon-knell-2`)

*Asked for the numbers behind "i think it needs to be more damage" and "maybe a slight increase in duration", Ben:*

> "1. For damage I think 65 is good"

> "2. I'm not sure for the duration yet. I'd like to know how long the bell sound is that plays after the real shot"

**THE FACTS HE WAS GIVEN, 2026-11-17 (the measurement, read only).** The beam played three sounds: at the lock,
`block.bell.resonate` (volume 1.4, pitch 0.7); at the fire, `block.bell.use` (1.0, 1.0) and
`block.amethyst_block.chime` (1.0, 1.0). The client's files and lengths, READ from asset index `30` (named by
`versions/26.1.2/26.1.2.json`) and its `sounds.json`, each length the Ogg's final granule position over its sample
rate:

| event | file | file length | variant pitches in `sounds.json` | as played (our pitch x the variant's) |
|---|---|---|---|---|
| `block.bell.resonate` | `block/bell/resonate.ogg` | 3.780 s | 1.0, 0.85, 0.9 | at our 0.7: 5.400, 6.353, 6.000 s |
| `block.bell.use` | `block/bell/bell_use01.ogg`, `bell_use02.ogg` | 2.492 s | 0.93, 0.95, 0.97 | at our 1.0: 2.679, 2.623, 2.569 s |
| `block.amethyst_block.chime` | `block/amethyst/shimmer.ogg` | 4.322 s | (none) | 4.322 s |

**"As played" ASSUMES playback speed equals pitch; how the client applies pitch was not read.**

*Then Ben:*

> "Here's what I want to try. The bell plays with the warning. The real shot comes when the resonance part starts. The teleport happens when the sound is fully finished, if a target is hit of course."

*The seat's reading, put to him: the bell strike (`block.bell.use`) plays when the warning starts; the resonance
(`block.bell.resonate`) starts at the real shot, at normal pitch; the teleport comes when the resonance has finished,
about 4.4 seconds after a hit; and the slowed 0.7 pitch cannot be kept, because the resonance would then outlast the
100-tick minimum between beams. The seat asked whether that reading was right, and how long the warning should be
(0.8, 1.0 or the strike's full 2.6 seconds; 0.6 today). Ben:*

> "1. yes"

> "2. let's try 1.3 seconds"

**WHAT THIS SUPERSEDES:** K7 (12 ticks; now 26, 1.3 seconds) and K11 (12 ticks; now 89, below), and Ben's earlier
ruling that the teleport comes after *"The same time between the warning shot and the actual beam"* (B), which his new
ruling replaces **for now** (*"what I want to try"*). **K10 goes from 35 to 65.**

**THE SEAT'S, CHOSEN AND UNRULED, listed for Ben:**
- the amethyst chime still plays at the shot;
- the second warning flash moves from tick 9 to tick 20 (three quarters through, as before);
- the teleport delay is **89 ticks**: the resonance file is 3.780 s, the client's slowest variant is pitch 0.85, so it
  ends at 3.780 / 0.85 = 4.447 s = 88.9 ticks, rounded UP to 89 so the sound has always finished. **It is one authored
  number in TICKS; the code never computes it from a sound.** The server cannot know which variant a client drew, and
  "playback speed equals pitch" is still assumed. **89 is below the cycle's 100-tick minimum with 11 to spare; a longer
  resonance (a lower pitch) would need a longer cycle**, and the load-time check refuses a delay that is not shorter.

### THE BOOT OF KNELL 2 (`58c0f7a1`, 2026-11-17): BEN'S FIVE ANSWERS, VERBATIM, AND WHAT THEY CONFIRM

Read from the server log of that boot (`GATE-dragon-knell-2.md`, KQ1R2 and TQ1R2), verbatim:

```
KQ1R2 ASK 1 question="Did the bell strike warn you before the beam?" answer="yes" ticks=114
KQ1R2 ASK 2 question="Did 65 feel right for a single shot?" answer="yes" ticks=65
KQ1R2 ASK 3 question="With 1.3 seconds, could you step out of the way in time?" answer="yes" ticks=87
TQ1R2 ASK 1 question="Did the Knell move as the ringing ended?" answer="yes" ticks=130
TQ1R2 ASK 2 question="Did that wait feel right?" answer="no" ticks=45
```

- **CONFIRMED BY PLAY, Ben:** `damage` 65; the 26-tick warning (he stepped out of the second beam, `gap=2.377 miss`);
  the bell strike as the warning's cue.
- **His "yes" that the Knell moved as the ringing ended is recorded as HIS EAR**, not as a measurement of "playback
  speed equals pitch", which stays INFERRED.
- **His "no" to the wait: `teleport.delay_ticks` 89 is NOT CONFIRMED, awaiting his number.** The seat has asked him
  what he wants to try in its place.

### Ben, 2026-11-17, VERBATIM: the re-timing of the bell (the follow-up, `feat/dragon-knell-3`)

After answering "no" to *"Did that wait feel right?"* (the boot of `58c0f7a1`), Ben:

> "Before I send this to CC, an elaboration on the no:
> The Bell and warning shot are good, after that nothing is timed properly. The resonance doesn't become audible for
> about a second after the sound starts and it ceases being audible for about a second at the end. I think we do a few
> things here:
> 1. Start playing the resonance at the same time as the bell
> 2. change the warning shot time from 1.3s to 1s
> 3. change the teleport time to one second before the resonance sound ends"

**THE SEAT'S READING, as put to him:** at the warning's start the bell strike AND the resonance both begin; the shot
comes 20 ticks later; the teleport comes 44 ticks after a hit.

**THE 44 IS THE SEAT'S ARITHMETIC, NOT HIS NUMBER.** The client draws the resonance at pitch 1.0, 0.9 or 0.85, so it
plays for 3.780 s, 4.200 s or 4.447 s (the measurement of 2026-11-17). The seat took the MIDDLE variant: 4.200 s from
the sound's start, less his one second, is 3.200 s = **64 ticks after the telegraph's start**; less the 20-tick
telegraph, **44 ticks after the fire**. By variant the move lands **0.58 s, 1.00 s or 1.25 s** before the sound ends;
the server cannot know which variant a client drew.

**CHOSEN BY THE SEAT, UNRULED:** the amethyst chime still plays at the fire; `flash_at` becomes [0, 15].

**SUPERSEDES** his 1.3-second warning (26 ticks) and the seat's 89-tick delay. Damage stays 65 (confirmed by play).

### THE BOOT OF KNELL 3 (`9d0cbffb`, 2026-11-17): BEN'S FOUR ANSWERS, VERBATIM, AND THE SEAT'S DECISION

Read from the server log of that boot (`GATE-dragon-knell-3.md`, KQ1R3 and TQ1R3), verbatim:

```
KQ1R3 ASK 1 question="Did the beam fire as the ringing became audible?" answer="yes" ticks=182
KQ1R3 ASK 2 question="With 1 second, could you still step out of the way in time?" answer="yes" ticks=55
TQ1R3 ASK 1 question="Did the Knell move about a second before the ringing faded?" answer="it moved about a second after the rining stopped" ticks=203
TQ1R3 ASK 2 question="Does the whole sequence feel timed right now?" answer="not quite" ticks=105
```

- **CONFIRMED BY PLAY, Ben:** the 20-tick warning (he stepped out of the second beam, `gap=1.949 miss`), and the bell
  strike with the resonance at the lock as its cue.
- **`teleport.delay_ticks` 44 is NOT CONFIRMED.**
- **THE SEAT'S DECISION:** the delay will not be derived from a sound file again; **Ben will tune it by ear with a dev
  tool and rule the number.**

### Ben, 2026-11-17, VERBATIM: one more number for the teleport delay (the follow-up, `feat/dragon-knell-4`)

After the boot of `9d0cbffb`, on a 44-tick delay, Ben answered *"it moved about a second after the rining stopped"* and
*"not quite"*. The seat offered a dev tuning tool or one more number; Ben:

> "I want to try one more number"

The seat told him the delay stood at 2.2 seconds after the shot and that his description pointed about 2 seconds
earlier; Ben:

> "let's try 1.4 seconds after the shot"

- **`teleport.delay_ticks` 28: HIS number** (1.4 s × 20), replacing the seat's 44. **Counted from the FIRE, and no longer
  derived from any sound's length.**
- **The dev tuning tool is NOT built: offered, and declined for now.**
- Everything else stands: the 20-tick telegraph, the sounds, the damage, the arc and the landing checks.

### Ben, 2026-11-17, VERBATIM: the second bell, after the stopped run of `90768891` (still `feat/dragon-knell-4`)

After the boot of `90768891`, whose run he stopped 23 seconds in (`GATE-dragon-knell-4.md`, THE STOPPED RUN), Ben:

> "before I send this to CC, my take on the timing changes:
> 1. The timing is perfect on the sounds but something is still missing
> 2. Add a second, higher pitched bell for the actual shot. Maybe even lower the pitch of the initial bell"

**THE SEAT'S READING:**
- **His point 1 CONFIRMS, by ear and in his words, the 20-tick warning and the 28-tick teleport delay.** It was given
  OUTSIDE the ASK step, and is recorded as such.
- **His point 2 adds a sound.** That there is a second, higher bell at the shot, and that the first bell is lower, are
  HIS. **THE SEAT'S STARTING NUMBERS, put to him as the seat's, not his:** the bell at the shot at pitch **1.5**; the
  first bell lowered from 1.0 to **0.8**.

**CARRIED, NEW:** a server stop during a selftest run now restores synchronously from `onDisable` (built in this PR,
pinned by `SelfTestDisableWiringTest`, never read on a server). **STILL UNBUILT: a crash-survival file for a JVM that
dies without disabling**; it would need the run's snapshot written to disk at run start and at every row's staging,
and read back and restored at the next enable, before any player joins.

### THE BOOT OF KNELL 4 (`0b4037ec`, 2026-11-17): BEN'S THREE ANSWERS, VERBATIM, AND THE SEAT'S READING OF THE THIRD

Read from the server log of that boot (`GATE-dragon-knell-4.md`, KQ1R4 and TQ1R4), verbatim:

```
KQ1R4 ASK 1 question="Does the higher bell at the shot give it what was missing?" answer="yes" ticks=88
KQ1R4 ASK 2 question="Is the lower first bell better than before?" answer="no" ticks=64
TQ1R4 ASK 1 question="Does the whole sequence feel right now?" answer="the timing of the warning and teleport are good, I want the shot to be in the middle" ticks=547
```

- **The second bell at the shot: CONFIRMED BY PLAY.** Its pitch 1.5 is the seat's starting value, unobjected.
- **The first bell at 0.8: REJECTED by him.**
- **THE SEAT'S READING of his third answer, put to him and NOT YET CONFIRMED in other words:** the lock and the move
  stay 48 ticks apart, and the fire moves to their middle, so the telegraph becomes **24 ticks** and the teleport delay
  **24**. **NOT built.**

### THE SHOT IN THE MIDDLE, AS BUILT (`feat/dragon-knell-5`, 2026-11-17)

Ben's answers after the boot of `0b4037ec` are verbatim above. **THE SEAT'S READING, as built here, and told to Ben
before the brief was sent:** the lock and the move stay **48 ticks apart, as today** (his "the timing of the warning
and teleport are good"), and the fire moves to their **middle** (his "I want the shot to be in the middle"): the
telegraph becomes **24 ticks** (was 20) and the teleport delay **24** (was 28). And, for his "no" to the lower first
bell, the telegraph's bell goes **back to pitch 1.0**. The shot's bell (1.5) and the chime are unchanged.
**The two 24s are a pair: they must sum to 48 until Ben rules otherwise; neither is derived from a sound.**

### THE BOOT OF KNELL 5 (`07d06837`, 2026-11-17): BEN'S TWO ANSWERS, VERBATIM; WHAT PLAY CONFIRMED; ONE RULING NOT BUILT

Read from the server log of that boot (`GATE-dragon-knell-5.md`, KQ1R5 and TQ1R5), verbatim:

```
KQ1R5 ASK 1 question="Is the shot in the middle now, the way you wanted?" answer="yes" ticks=73
TQ1R5 ASK 1 question="Does the whole sequence feel right now?" answer="it's almost there. The second purple line is out of place. remove it other than that everything is perfect" ticks=463
```

- **CONFIRMED BY PLAY:** the 24-tick telegraph; the 24-tick teleport delay; the first bell at pitch 1.0 with the
  resonance at the lock; the bell at 1.5 with the chime at the shot; 65 damage.
- **RULED AND NOT BUILT:** the telegraph's second flash (`flash_at` tick 18) is to be REMOVED, leaving the one flash at
  the lock. The seat carries it into the next branch that boots, where Ben sees it gone.

### Ben, 2026-11-17, VERBATIM: the Phalanx's shot starts from the old game's numbers (step R7a, `feat/dragon-shot`)

*The seat offered the old game's Phalanx numbers as a start: fires a shot every 5 seconds at a target within 30
blocks; the shot flies at about 4 blocks per second, steers gently toward the target, and expires after 20 seconds or
35 blocks; a hit deals 40; it blinks away when a player comes within 5 blocks, landing 6 to 16 blocks off; its shield
absorbs the first hit it takes.* Ben:

> "change the duration of the shot to 12s and it fires a shot every 6s"

**THE SEAT'S READING (the seat's, stated in this file's words):** X7 is **120 ticks** and the shot's life is **240
ticks**, his numbers. **Every other number in that list stands as a STARTING number he left unchanged, not one he
typed:** X8 (30, re-acquire 35), X9 (0.2 blocks a tick), X10 (10%), the 35-block limit, X12 (1.2), X13 (40), and X4-X6
and X15 for the later step.

**THE SEAT'S, PROVISIONAL:** the shot's damage is multiplied by the Phalanx's gear score / 100, as elsewhere (P-b, the
beam's I21).

**ALSO BUILT IN THIS BRANCH:** the Knell's second warning flash, ruled removed on 2026-11-17 (*THE BOOT OF KNELL 5*,
above): `knell.yml` `flash_at` becomes `[0]`.

### THE SEAT'S RULING ON #201, 2026-11-17: THE HOMING MATCHES THE OLD GAME (the seat's, stated in this file's words)

Ben accepted the old game's steering ("steers gently", the seat's words for the record's 10%). **The 10% is a fact about
the record: cfde822 blended 10% once per 2-TICK step** (it moved 0.4 blocks every 2 ticks, section 3.4). #201 first
built 10% per tick, about twice as hard per block. **The seat ruled that the old game's feel is what he accepted**, so
`phalanx.yml`'s `homing` is **0.0513 a tick**: 1 - sqrt(0.9) = 0.0513167, to four places, the per-tick fraction two of
whose ticks give the record's one step. **That value is the seat's conversion of a number Ben left unchanged, not a
number he typed.** The blend is the same expression as the record's; renormalising between our two ticks makes the match
exact only for a target nearly ahead (5.R.9 X-c).

**ALSO RECORDED, put to Ben by the seat and UNRULED:** at 0.2 blocks a tick and a 35-block limit, a straight shot
expires by range at tick 176, so his 12 seconds (240 ticks) binds only for a shot that curves; a straight shot lasting
the full 240 would need a limit of 48 blocks (F14).

### Ben, 2026-11-17, VERBATIM, BY THE SELFTEST'S ASK STEP: the Phalanx's shot (the boots of `8ec16e89` and `3dc0e184`)

*Read in `GATE-dragon-shot.md`, SQ1, KQ1R6 and SQ1R2.* At the boot of `8ec16e89`:

```
SQ1 ASK 1 question="Could you see the shot coming (light purple, with pink flashes)?" answer="yes" ticks=90
SQ1 ASK 2 question="Did hitting it destroy it?" answer="yes" ticks=88
SQ1 ASK 3 question="Does the Phalanx's shot feel fair now?" answer="yes" ticks=78
KQ1R6 ASK 1 question="Is the second purple line gone?" answer="yes" ticks=113
```

(SQ1's ASK 2 "yes" disagrees with that boot's server, which logged both of his shots as hits. The question was put again
in SQ1R2.) At the boot of `3dc0e184`:

```
SQ1R2 ASK 1 question="Did your swing destroy it before it reached you?" answer="yes" ticks=37
SQ1R2 ASK 2 question="Was it easy enough to hit?" answer="yes" ticks=66
```

There the server agrees: `SWING kind=attack … along=1.265`, then `SHOT destroyed … age=67 … by=vanilla cause=ENTITY_ATTACK
damager=player <Ben>`, one tick before the reach.

**CONFIRMED BY PLAY, 2026-11-17:**
- he could see the shot;
- the look (light purple with pink flashes);
- a hit destroys it (his own swing, read by the server in SQ1R2);
- it feels fair;
- it was easy enough to hit;
- the Knell's second flash is gone.

**THE SEAT'S, UNRULED BY HIM:** the 0.8 hit box (`shot.hit_box`); the frog carrier; only attacks destroy it (environmental
damage refused, `/kill` ends it); an ownerless or dispenser arrow is refused; the 0.0513 homing (the seat's conversion
of the old game's 10% per 2-tick step).

### Ben, 2026-11-17, VERBATIM: the Phalanx's shield (step R7b, `feat/dragon-blink`)

*The seat asked: should the Phalanx's shield absorb the first damage of any kind, including a fall or fire, as in the old
game, or only the first hit from a player?* Ben:

> "1. yes the first damage of any kind"

**That answers Q7.** **THE SEAT'S READING:** the blink's numbers stand as STARTING numbers he left unchanged on
2026-11-17, **not numbers he typed**: X4 (a player within 5 blocks), X5 (it lands 6 to 16 blocks off) and X6 (8 attempts).

### Ben, 2026-11-17, VERBATIM, BY THE SELFTEST'S ASK STEP: the blink and the shield (the boot of `ef770b8a`)

*Read in `GATE-dragon-blink.md`, BQ1, in the order logged:*

```
BQ1 ASK 1 question="Does the glowing shape sit where you can hit it?" answer="yes" ticks=266
BQ1 ASK 2 question="Did the shot shove you?" answer="I don't think so" ticks=331
BQ1 ASK 3 question="Did the Phalanx blink away when you got close?" answer=NOT STATED after 1200 ticks
BQ1 ASK 4 question="Did its shield visibly eat your first hit?" answer="yes but it needs a larger cooldown. Say 12 seconds. Answer for question 3" ticks=627
```

And in chat after the run, his own correction: `<BaronVonYeetus> question 4: yes it did`. So the line the ASK step
captured for question 4 is his answer to question 3, and question 4's answer is "yes it did".

- **CONFIRMED BY PLAY:** the shape sits where it can be hit; the blink; the shield eating the first hit.
- **RULED BY BEN AND NOT BUILT:** the blink needs a cooldown, "Say 12 seconds" (**240 ticks**).
- **THE SEAT'S, put to him:** the cooldown is counted from a blink that LANDS; a failed attempt does not spend it.

### Ben, 2026-11-17, VERBATIM: the Skreel's numbers (step R8, `feat/dragon-skreel`)

*The seat asked Ben to accept or change the old game's Skreel numbers.* Ben:

> "can you suggest some numbers and I'll decide from there"

*The seat suggested, with the old value beside each:*

| quantity | the seat's suggestion | old (cfde822, §3.5) |
|---|---|---|
| rider health | 120 | 120 |
| mount health | 160 | 120 |
| mount speed with its rider | 1.3x | 1.3x |
| notices a target within | 32 blocks | 50 |
| circles at | 8 to 12 blocks | 8 to 12 |
| wind-up before the leap | 1 second | 0.75 s |
| time between leaps | 6 seconds | not recorded |
| landing hit | 55 | 35 |
| landing hit reaches | 3 blocks | 2.5 |
| knockback | yes, moderate | yes |
| if the mount dies first | the rider drops and fights on foot as a plain melee mob | |
| if the rider dies first | the mount carries on as a plain fast spider with no leap | |
| elements | the rider Void and the mount with no element | |
| damage | scaled by gear score like the others | |

Ben:

> "I like your numbers. Let's start there"

**RECORDED PLAINLY: these are THE SEAT'S numbers, accepted by Ben as a starting point. None is a number he typed.**

**"MODERATE" KNOCKBACK IS THE OLD GAME'S VALUES: 0.7 horizontal (away from the spider) and 0.4 vertical** (§3.5, LAND:
the hit, READ). **Against the blast's, they are moderate:** the push vector's length is √(0.7² + 0.4²) = **0.806 blocks
a tick**. The Phage's blast (`phage.yml` `blast.knockback: 1.0`, §7 F-B) adds **1.0** at its centre, **0.75** at 1.5
blocks, **0.5** at its half reach (3) and **0** at its reach (6), falling in a straight line. So the pounce pushes as
hard as a blast does at about **1.16 blocks** from it (1 − d/6 = 0.806): weaker than the blast's strongest, stronger than
the blast's push at the pounce's own reach of 3 (0.5). Nothing here shows them NOT moderate, so they stand.

### Ben, 2026-11-17, VERBATIM: the Phalanx's blink cooldown (step R8 carries it)

Ben ruled the blink (*the boot of `ef770b8a`*, above):

> "needs a larger cooldown. Say 12 seconds"

**240 ticks, his number.** **The seat told him it counts from a blink that LANDS and a failed attempt does not spend it;
he did not object.**

### Ben, 2026-11-17, VERBATIM, BY THE SELFTEST'S ASK STEP: the Skreel and the cooldown (the boot of `45bc33ce`)

*PQ1, after 30 seconds beside a live Skreel pair that wound up ONCE (THE BAND EDGE, F22):*

> "Could you see the Skreel wind up before it leapt?" — **"yes"**
>
> "Could you get out of the way in time?" — **"I didn't try to"**
>
> "Did the landing hit feel right for a pounce?" — **"yes"**

*PQ2, after 40 seconds with a live Phalanx, which he killed inside its first cooldown:*

> "Does the Phalanx's blink feel right with the 12-second cooldown?" — **"yes, but it needs to have a longer teleport
> range. Say 12-24 blocks"**

**What they confirm, and what they do not.** The wind-up is seen and the hit feels right (one landing, at 55). **The
second answer is NOT a reading of the dodge:** he did not try. The cooldown was read by PB1 only; in PQ2 the Phalanx made
one blink of its own before he killed it.

**RULED BY BEN: THE PHALANX'S BLINK LANDS 12 TO 24 BLOCKS AWAY** ("Say 12-24 blocks"), replacing X5's starting 6 to 16.
`phalanx.yml` `blink.near: 12`, `blink.far: 24`.

**WHAT HIS 24 DOES, AND WHAT IS BUILT FOR IT (CHOSEN, put to him; not his ruling).** A blink is triggered by a body within
5 of the Phalanx's feet; after a landing 24 out, that body's eye can be up to **33.1** from the Phalanx's eye
(`MobBlinkRangeTest`, the geometry), **past the shot's acquire range of 30**. At X5's 16 it was at most 25.6, so this is
new. **So from a blink that lands until its next shot, a Phalanx chooses whom to fire at out to the shot's re-acquire
range, 35** (`MobShot#chooseRange`); its first shot puts it back on 30. Its vanilla target is a separate thing: a wither
skeleton's follow range is 16 (READ: `AbstractSkeleton#createAttributes` adds nothing over `Mob#createMobAttributes`'
`FOLLOW_RANGE 16.0`, and `TargetGoal#canContinueToUse` drops a target past it), so after most blinks of 12 to 24 its
vanilla target drops, as it often did at 16; the shot's own choice is what keeps it fighting.

### THE SEAT'S RULINGS AFTER THE BOOT OF `45bc33ce`, 2026-11-17 (the seat's, stated in this file's words)

- **THE BAND EDGE (F22) IS A DEFECT; THE STAND-OFF POINT IS THE MIDDLE OF THE BAND,** (near + far) / 2 from the target,
  10 at today's numbers, never an edge. A pair nearer than the band's near edge moves OUT to the same point; inside the
  band it holds.
- **THE CANNOT-REACH RULE (CHOSEN, for Ben):** a pair that cannot reach its band winds up FROM WHERE IT STANDS once it has
  a target within its acquire range and has waited one full period ready. As built: ready 120 ticks after the last
  wind-up began, so it winds up at 240 (`PounceCycle`). A tick with no target or no AI starts the wait again.
  **REVISED by the seat's third brief (below): measured by progress, not by time ready.**
- **"The pathfinder stops short" stays INFERRED until read; READ in this step** (`MobPounceTest`,
  `theBandsHalfWidthExceedsTheMountsStopOnAnAxisAndNotOnADiagonal`): a path is done with the mob under (width / 2 + 0.5)
  from its goal on each axis: **1.55 for the 2.1-wide mount, so up to 2.19 on a diagonal**, which the band's half-width
  of 2 does NOT exceed. The cannot-reach rule is the backstop.
- **R3's anchor on floor(feet.y) is ACCEPTED,** with its cost recorded (a slab's top is never a landing). **F19 is a
  carried DEFECT**, first into the next branch that touches the Knell. **R5 as one hit raising two events is ACCEPTED**,
  "a sprint hit" marked INFERRED. **The pounce on plain KNOCKBACK_RESISTANCE is ACCEPTED** (a pounce is not an explosion).
  **"No leap" read as no pounce is ACCEPTED as the seat's reading**, put to Ben. **Cancelling a paired mob's target
  events and removing no goals is ACCEPTED.** **F21 is recorded as a finding.**

### THE SEAT'S THIRD BRIEF ON #203, 2026-11-17: THE PAIR MUST END INSIDE ITS BAND FROM ANY BEARING (the seat's, stated in this file's words)

- **The backstop must not be what makes an ordinary approach work:** a pair whose path ends outside the band corrects
  itself, and from any bearing and start within the acquire range, on open ground, comes to rest inside [near, far] and
  winds up at its normal period. **Ben's band and the band test are NOT widened or loosened.**
- **AS BUILT:** `MobPounce.goal` chooses the goal BLOCK, among those within 3 of the band's middle point, nearest that
  point, whose stopping box (its centre ± the mount's tolerance on each axis, grown by 0.25, CHOSEN) lies wholly inside
  the band; so the walk crosses into the band before its path can end, and the cycle stands it there. Proved in core
  against the READ stopping rule over every 5 degrees, twelve start distances and four target positions (3456 cases,
  all at rest inside 8 to 12); without the choice, 28 rest outside, the diagonals first. The goal is recomputed every
  tick the pair is outside its band, so a pair chasing a walking player settles when he stops.
- **THE CANNOT-REACH RULE, MEASURED BY PROGRESS:** a pair with its AI and a target, outside the band, that has not come
  1 block nearer the band for a full period (`PROGRESS_BLOCKS`, CHOSEN), winds up from where it stands once ready. A pair
  walking in good faith never counts.

### Ben, 2026-11-17, VERBATIM, BY THE SELFTEST'S ASK STEP: the second round (the boot of `3d93a532`)

> "With the Skreel leaping every six seconds, could you get out of the way in time?" — **"yes"**
>
> "Does it leap often enough now?" — **"yes"**
>
> "Does the Phalanx's blink range feel right at 12 to 24 blocks?" — **"yes"**

**CONFIRMED BY PLAY:** the Skreel's wind-up is visible (the first round's "yes"); its leap can be dodged (he dodged the
first two of PQ3's four); the landing hit feels right (the first round's "yes"); it leaps often enough (PQ3's four
wind-ups, each 120 ticks after the last). The Phalanx's blink at 12 to 24 blocks (PQ4's draws 12.091 and 21.077) and its
12-second cooldown (the first round's "yes"; PQ4's second blink 287 ticks after its first).

**THE SEAT'S AND OURS, PUT TO BEN AND UNRULED:** the Skreel's K1-K29 choices (`GATE-dragon-skreel.md`, *THE SKREEL,
INHERITED OR CHOSEN*); the cannot-reach rule, and that a player who outruns the pair triggers it; the Phalanx looking
out to the re-acquire range, 35, from a landed blink until its next shot.

### Ben, 2026-11-17, VERBATIM: the Knell's slab bug, and a void explosion that pulls (`feat/void-implosion`)

> "We can fix the knells slab bug, but i also want to add more. A void explosion that pulls things toward the origin
> with black and dark purple swirling particles. I want this to happen when the phage explodes but I also want to be
> able to use this shape elsewhere (potential future abilities and weapons)."

*The seat then proposed five points:* (1) the pull REPLACES the blast's outward push; (2) the pull happens at the moment
of the explosion, not during the fuse; (3) the same 6-block reach, about as strong as the push is today, reversed; (4)
the Phage's implosion pulls only what it damages, players and later summons, not other mobs; (5) the look replaces the
vanilla explosion puff: black and dark purple particles spiralling inward over about half a second, with a deep sound.
**Ben, VERBATIM:**

> "1. Yes, swap the push for the pull. Also side note, can we make the size of the Phage affect the Fuse radius,
> explosion radius, etc. The Large phage being the current distances and scaling everything down to the small phage.
> 2. Yes
> 3. Yes but view 1's answer. A little bit stronger than the push today.
> 4. Yes
> 5. Yes"

### THE SEAT'S READINGS OF THAT, 2026-11-17 — PUT TO BEN IN THESE TERMS, MARKED AS THE SEAT'S

- **Distances scale with the stage's HEIGHT over the last stage's** (1/3 at stage 1 to 1 at stage 5): the fuse's trigger
  1 to 3 blocks, its cancel 2.33 to 7, the blast's and the pull's reach 2 to 6.
- **"etc." covers distances only**, so the fuse stays 30 ticks and damage stays 60 to 150.
- **The pull's strength is 1.25 x today's push**: the seat's number for "a little bit stronger".
- **The pull never carries a victim past the origin** (the seat's rule).
- The seat also told him **a stage-1 Phage will rarely finish a fuse on a moving player**.

**This REOPENS N4: the seat's R3 ruling 20 (vanilla's swell is the fuse) is SUPERSEDED.** Ruling 20 named Ben's F
("Yes but I might change my mind in testing") as the ruling that would reopen it; it is Ben's side note above that does.

### PART C STOPPED AT THE JAR, 2026-11-17: THE API CANNOT SHOW A SWELL THAT RESETS AT OUR OWN DISTANCES

The brief: read the jar first; if the API cannot drive the swell so that the CLIENT still shows the swelling and its
reset, STOP and tell the seat what it can do, before building anything else in Part C. **It cannot, and nothing of Part
C is built** (no fuse state machine, no per-stage distances; the reach stays 6 at every stage). Every line READ with
`C:\Users\Neb91\.jdks\openjdk-26.0.1\bin\javap.exe -c -p -cp run/versions/26.1.2/paper-26.1.2.jar <class>`, the API in the
pinned `paper-api-26.1.2.build.74-stable-sources.jar`:

- **The client animates from the swell DIRECTION, never the swell value** (READ, `net.minecraft.world.entity.monster.
  Creeper#tick`, `#getSwelling`, `#defineSynchedData`): `DATA_SWELL_DIR` is synched data defaulting to −1; `tick` adds
  the direction to `swell` each tick (both sides run it), and `getSwelling` interpolates `oldSwell`→`swell`. `swell`
  itself is a plain field, never synched. **INFERRED:** the client's swell comes only from the direction (and
  `DATA_IS_IGNITED`, which forces +1 in `tick`).
- **Only `SwellGoal.tick` ever sets the direction back to −1** (READ, `SwellGoal#tick`): with no target or a dying one,
  `distanceToSqr > 49.0` (the literal 7), a creative or spectator target, or no line of sight. `Creeper#setSwellDir` is
  called from `Creeper#tick` (+1 when ignited) and from `SwellGoal` only. `canUse` starts it at `distanceToSqr < 9.0`
  (the literal 3) or when the direction is already up. Its flags are `MOVE` (`setFlags(EnumSet.of(Flag.MOVE))`) and
  `start` calls `getNavigation().stop()`: **that is "stands still while fusing"**.
- **The API has no swell-direction setter** (READ, `org.bukkit.entity.Creeper` source: `setMaxFuseTicks`,
  `setFuseTicks` (writes `swell`), `setExplosionRadius`, `explode`, `ignite`, `setIgnited`, `isIgnited`;
  `CraftCreeper`'s bytecode adds nothing else). **`setIgnited(false)` clears `DATA_IS_IGNITED` and leaves the direction
  at +1** (READ, `Creeper#setIgnited`: it writes `DATA_IS_IGNITED` only), so with vanilla's goal gone a creeper once
  ignited swells to the end; with it present, the reset happens only at vanilla's 7. **A cancelled prime does not reset
  the direction either** (READ, `Creeper#explodeCreeper`'s cancelled branch: `swell = 0`, `DATA_IS_IGNITED` false).
- **What the API DOES offer** (READ, `com.destroystokyo.paper.entity.ai.MobGoals`, `VanillaGoal`, `Goal`):
  `removeGoal(mob, VanillaGoal.CREEPER_SWELL)`, and `addGoal(mob, priority, Goal)` with a custom goal whose
  `getTypes()` can claim `MOVE`, which would hold the Phage still exactly as `SwellGoal` does. NMS is not needed for
  either; **it is the client's swell animation that has no API route.**

**What can be built, put to the seat (none built):**

1. **Our fuse, no client swell.** Remove `CREEPER_SWELL`; a custom `MOVE` goal holds the Phage still and runs core's
   state machine at our distances; `setFuseTicks` keeps the server's `swell` for anyone reading it; **the client shows
   no swelling**, so a replacement telegraph (our particles and the `entity.creeper.primed` sound, both API) stands in.
2. **Our fuse, a client swell sent by packet.** As 1, plus PacketEvents' `WrapperPlayServerEntityMetadata` (the
   nameplate senders already use it) carrying the creeper's swell-direction index to each viewer, +1 at the start and
   −1 at the reset, and re-sent to a viewer who starts tracking mid-fuse. The metadata INDEX is a protocol fact
   (UNVERIFIED, not read) that breaks on protocol changes: CLAUDE.md's invariant 4 allows packets only where the API
   cannot express the effect, which this is.
3. **Vanilla's swell kept (3 and 7 at every stage), and only the blast's and the pull's reach scaled by stage.** The
   reach is ours and needs no swell; the trigger and cancel stay vanilla's literals.
4. **A one-way swell.** `setIgnited(true)` at our start shows the swell; a reset cannot be shown, and the server's own
   `tick` keeps swelling unless we rewrite `swell` every tick. Not recommended.

### THE PULL TRAVELS LESS THAN THE PUSH DID — A CONSEQUENCE OF THE SEAT'S RULE, PUT BACK TO THE SEAT (2026-11-17)

Built as briefed: 1.25 x the push's strength, HORIZONTAL (the seat's lean), capped so the modelled slide stops half a
block short of the Phage. **Executed in jshell** (scratch `push.jsh`, the model only, no player input: the old push
along the eye direction with vanilla's 0.08 gravity, 0.98f vertical drag, 0.91f air and 0.546f stone friction; the new
pull horizontal on stone, capped):

| d (blocks) | old push: speed (horizontal, up) | old push: travel | new pull: speed applied | new pull: travel | capped |
|---|---|---|---|---|---|
| 1 | 0.438, 0.709 | 4.150 | 0.227000 | 0.500 | yes |
| 2 | 0.518, 0.420 | 4.121 | 0.681000 | 1.500 | yes |
| 3 | 0.440, 0.238 | 2.663 | 0.625000 | 1.377 | no |
| 4 | 0.309, 0.125 | 1.545 | 0.416667 | 0.918 | no |
| 5 | 0.159, 0.051 | 0.592 | 0.208333 | 0.459 | no |

**The push's lift put the player in the air, where the horizontal drag is 0.91 a tick, not 0.546; the pull, flat and
capped, slides on the ground.** On stone the cap binds inside d = 2.229995 (bisected in the same jshell). So in TRAVEL the pull is weaker than the
push at every distance, though 1.25x in STRENGTH. **Ben's "a little bit stronger" is asked by BQ1R3 (ASK 3), and the
seat should expect a "no".** What would change it is the seat's: a lift (and a cap that models the flight), a larger
multiplier where the cap does not bind, or a smaller margin.

**ANSWERED 2026-11-17 by the seat's two rulings on #204 (below).**
**THE SEAT'S RULINGS STILL OWED BEFORE ANY BOOT:** Part C's route (1-4 above); the pull's travel against "a little bit
stronger".

> **CORRECTED 2026-11-17, THE SAME DAY, AND THE TABLE ABOVE IS KEPT AS WRITTEN.** Its "old push travel" column (4.150,
> 4.121, 2.663, 1.545, 0.592) came from a model that applied the AIR's friction on the take-off tick. Vanilla applies the
> friction of the state the body STARTED the tick in (READ, `LivingEntity#travelInAir`: `onGround()` is read before the
> move), and a pushed player started that tick on the ground. Re-executed in that order (`flight.jsh`), the old push
> carried him **2.703, 2.768, 1.894, 1.153, 0.482** at d = 1 to 5. **The conclusion stands, smaller:** the flat, capped
> pull (0.500, 1.500, 1.377, 0.918, 0.459) still travelled less than the push at every distance. The core test that pins
> the corrected figures is `ImplosionTest.theTravelAtTwoThreeFourAndFiveBesideTheOldPush`; its mutation MUTTAKEOFF (the
> air's friction on the take-off tick) reproduces the wrong figure, 4.268 at d = 2, and is red.

### THE SEAT'S TWO RULINGS ON #204, 2026-11-17 (the seat's, stated in this file's words)

1. **PART C IS BUILT BY ROUTE (b): OUR OWN FUSE, THE SWELL SHOWN BY PACKET.** PacketEvents 2.13.0 is already pinned and
   already carries entity metadata for mining. Its conditions: the metadata index and type READ from the server jar and
   agreed with PacketEvents' own naming (else STOP); the server's truth is OUR fuse (idle, fusing, detonate), the packet
   only shows it, vanilla's swell goal removed through `MobGoals` and ours claiming `MOVE`; every viewer, and a viewer who
   starts tracking mid-fuse, sees the state, and nothing stale survives a split, a growth, a removal or an unload; the
   vanilla prime still cannot happen, the cancel kept as the backstop with its line; a startup self-check line naming the
   index, read by R0; and a fall-back to route (a) for the fuse only if the packet cannot work. **The distances by stage
   as the first brief's Part C item 3 ruled**, the reach scaling the blast, the pull and the swirl's width together.
2. **THE PULL GETS THE PUSH'S LIFT, AND THE CAP MODELS THE FLIGHT.** The horizontal is 1.25 x the push's horizontal at
   that distance, toward the origin; the vertical is the push's own lift, a named value a use can set to zero (the
   fixture keeps zero). The cap is on where the victim COMES TO REST, through the flight and the slide, scaling the
   horizontal down, never the lift; a victim already in the air or on ice is capped by its own model. No fall damage. Core
   tests sweep the distance on stone, ice and in the air, and pin that where the cap does not bind the travel is 1.25 x
   the old push's.

**The seat also accepted, as built in `bf3bf2d9`:** F19's fix and its cost; the implosion as a shape with "who" belonging
to each use; the content form; the test-only fixture; what it never pulls; the look; `h=` on the BLAST line; KS1 in the
blast scenario file; the 12-tick wait. **And said its STOP had been wider than it needed: the reach scaling should not
have waited.**

### PART C AND THE LIFT, AS BUILT (`feat/void-implosion`, 2026-11-17)

- **The index, READ and agreed.** Counted down the class chain in the pinned jar (`javap -c -p -cp
  run/versions/26.1.2/paper-26.1.2.jar net.minecraft.world.entity.<Class>`, each `static {}`): `Entity` 8, `LivingEntity`
  7, `Mob` 1, `PathfinderMob` and `Monster` 0, then `Creeper.DATA_SWELL_DIR` first: **16**, `EntityDataSerializers.INT`
  (registered second, READ). `ClassTreeIdRegistry#define` numbers after the superclass's last (READ). PacketEvents 2.13.0
  names the type `EntityDataTypes.INT` and gives it **id 1 at V_26_1** in its own `entity_data_serializer` mapping
  (EXECUTED from its pinned jar, its own `SequentialNBTReader` and `ListDiff`; its list holds 43, as the server registers
  43). PacketEvents names no per-entity index: the 16 is ours. `PhageFuser.selfCheck` logs `Phage swell packet: index=16
  type=int typeId=<id> server=<version> readFor=V_26_1`, or `UNVERIFIED …` as a WARN.
- **The fuse:** core `MobFuse` (idle, fusing, detonate; vanilla's order and strictness, read), ticked each server tick by
  `PhageFuser` on the Phage's own thread; `SwellGoal` removed and a `MOVE`-claiming hold added at EVERY appearance (a
  creeper read from disk re-registers its goals); a START or RESET sent as metadata (index 16, +1 or −1) to every
  `getTrackedBy()` player and logged `SWELL`; a new tracker of a lit Phage sent +1 a tick later (READ:
  `PlayerTrackEntityEvent` fires before `ServerEntity#addPairing`). Detonation through `PhageFuser.detonate`, which marks
  the prime it raises; the prime handler REFUSES an unmarked prime (`CREEPERGUARD prime refused`). The selftest's DETONATE
  goes through the same mark; `probe_fuse` and `await_fuse` read our fuse.
- **The distances:** `StagedDistance`: the last stage's authored, earlier stages by height (trigger 1 to 3, cancel 7/3
  to 7, reach 2 to 6), or `{ first, last }` linear, content-only. `MobBlast.checkStages` refuses a cancel not beyond its
  trigger at any stage.
- **The lift and the cap:** `Implosion` now splits the old push (strength `(1 − d/R)(1 − res)` along the eye) into its
  horizontal part (x 1.25 toward the origin) and vertical (x `lift`); the horizontal is capped so the modelled rest point
  (the flight, READ: gravity 0.08, drag 0.98f, friction by the state at the tick's START, the landing zeroing the
  vertical; then the slide) is the horizontal distance less 0.5. **A lift whose apex reaches 2.5 blocks arms the dash's
  safe-landing mark**: the lift is never scaled, and near the centre it carries a player above vanilla's 3-block safe fall
  (d = 1 at stage 5: 3.200).
- **The travel, beside the old push's** (stone, R = 0; jshell `flight2.jsh`): d = 2: push 2.768, pull **1.500**
  (capped); d = 3: 1.894, **2.367**; d = 4: 1.153, **1.441**; d = 5: 0.482, **0.602**. Where the cap does not bind (d ≥
  about 2.23), exactly 1.25 x.

**STILL THE SEAT'S AND BEN'S, UNRULED:** the 0.5 margin; the 2.5 safe-landing threshold; the substitute for a client
that never receives the packet (none: a missed packet is a missed animation, never a missed fuse).

### Ben, 2026-11-17, VERBATIM: his four answers in the boot of `6d079ead` (`GATE-void-implosion.md`, BQ1R3)

*Each question as the selftest asked it, then his answer, copied from the ASK lines of the log.*

1. *"Did you see the big Phage swell up before it blew?"*
   > "Not sure"

   **NOT STATED.** Whether the swell packet shows on his client is UNREAD.
2. *"Did you see black and dark purple particles swirl in to where it stood?"*
   > "yes"
3. *"Did it pull you in, and did the pull feel a little stronger than the old push?"*
   > "yes it pulled me in, i think it needs to either be stronger or pull in multiple times or continue pulling in for a second"
4. *"The small Phage now lights only within 1 block and its blast reaches 2. Does it still feel like a threat?"*
   > "yes"

**CONFIRMED BY PLAY:** the swirl; that the blast pulls him in; the small Phage still a threat at its scaled distances.

**RULED BY BEN AND NOT BUILT:** the pull "needs to either be stronger or pull in multiple times or continue pulling in
for a second". The seat has put a one-second continuing pull to him and awaits his answer. Nothing is built from it.

### Ben, 2026-11-18, VERBATIM: the implosion's pull continues for a second (`feat/implosion-drag`)

After his answer "yes it pulled me in, i think it needs to either be stronger or pull in multiple times or continue
pulling in for a second", the seat proposed: (1) it drags everything in reach steadily toward the centre for 1 second
after the blast, where today it is a single yank, the swirl lasting the same second; (2) the damage stays at the instant
of the blast; (3) you can fight it: walking away slows the drag but does not cancel it, and sprinting away from the edge
gets you out; (4) it still never carries you past the centre; if you reach it you are held there until the second is up.
**Ben:**

> "1. yes
> 2. yes
> 3. yes
> 4. yes"

**THE SEAT'S OWN CHANGE, told to him:** the Phage's lift goes to 0; a steady drag needs no toss. **THE SEAT'S FALLBACK,
told to him:** if a resistible drag cannot be made to work on a player, several pulls in a row, his own second option.

### THE DRAG AS BUILT (`feat/implosion-drag`, 2026-11-18): D1-D3, THE QUANTITY, THE NUMBERS

**Every line READ with `C:\Users\Neb91\.jdks\openjdk-26.0.1\bin\javap.exe -c -p -cp <jar> <class>`**, the client jar
`C:\Users\Neb91\AppData\Roaming\.minecraft\versions\26.1.2\26.1.2.jar` (unobfuscated, Mojang's names) where the question
is the client's, the server jar `run/versions/26.1.2/paper-26.1.2.jar` where it is the server's.

**D1. WHO MOVES A PLAYER.**
- **A velocity the server sends REPLACES his** (READ, client): `ClientPacketListener#handleSetEntityMotion` calls
  `Entity#lerpMotion`, which is `setDeltaMovement` (no override in `LocalPlayer` or `LivingEntity`).
- **It lands before his next tick, and that tick still adds his input** (READ, client): `Minecraft#runTick` runs
  `PacketProcessor#processQueuedPackets` before `tick`; in his tick, `LocalPlayer#applyInput` sets `xxa`/`zza` from his
  keys (`modifyInput`: `0.98f`, then `LivingEntity#applyInput`: `0.98f` again), and `LivingEntity#travelInAir` ADDS them
  (`Entity#moveRelative`: the input, not normalised under length 1, times the speed), moves, then applies friction.
  **So a send wipes the momentum he carried, never the input of the tick it lands in.**
- **The server sends at the end of its tick, only when marked** (READ, server): `CraftEntity#setVelocity` sets the
  delta and `hurtMarked`; `ServerEntity#sendChanges` sends one `ClientboundSetEntityMotionPacket` of the entity's delta
  to the player and his trackers when `hurtMarked`, after a `PlayerVelocityEvent`.
- **What the server learns of where he went:** his positions, by move packet (`ServerGamePacketListenerImpl
  #handleMovePlayer`, which compares them with the server's own `getDeltaMovement` only in its "moved too quickly" check;
  READ); the server keeps his reported movement apart, `ServerPlayer#lastKnownClientMovement` (READ, the field and
  `setKnownMovement`). **That the server's `getDeltaMovement` for a player never holds his walking is INFERRED** from
  those reads: no path writes his reported motion into it.
- **SO: a pull every tick that ADDED to `getVelocity()` would wipe his walking momentum every tick**; his fight against it
  shrinks to one tick's input (0.096 walking, 0.125 sprinting, READ and EXECUTED below), where his own momentum carries
  0.2115 and 0.2750 a tick. **Every second tick:** on the unsent tick he keeps his momentum and adds his input, so he
  rebuilds half of it; the drag arrives as two-tick tugs. **Every fifth tick:** four ticks of his own walking, then a
  jolt that wipes it: pulses, not a drag. **A resistible per-tick drag CAN work, and is what is built:** the server sends
  HIS OWN MOMENTUM, estimated from his last two positions (the displacement times the friction of the tick's starting
  state; vertical `(dy − 0.08) × 0.98f`), PLUS the drag's add (`DragMotion`). Each send then leaves his momentum as it
  was and adds the drag: a force he fights with his whole speed. **No STOP:** the seat's fallback was not needed.
  **INFERRED, the estimate's two assumptions:** one move packet a server tick, and a send reaching him the next tick (a
  local server). A displacement over 1.0 is a teleport: no estimate that tick. **Nothing is sent to a victim the drag
  adds nothing to.**

**D2. THE 7 TO 12 PERCENT: A CAUSE FOUND, AND IT IS OURS.** The blast set `victim.getVelocity().add(impulse)`, and a
standing player's server-side velocity holds the resting `(0 − 0.08) × 0.98f = −0.0784` (READ: `LivingEntity
#travelInAir`'s gravity and drag, and `Player#canSimulateMovement` true off the client, one half of `LivingEntity
#aiStep`'s gate on `travel`; that the other half, `isEffectiveAi`, lets it run for a player is INFERRED). So the sent
lift was 0.0784 short. **EXECUTED** (jshell `d2.jsh`, the real
`Implosion.travelFactor`) with lift − 0.0784: FZ1 0.423, IM3 1.400, FZ3 1.324, FZ5 2.169, against the readings 0.42,
1.40, 1.32, 2.16, each to the printed place; BL9R3's 2.21 fits a deficit of 0.04 to 0.06 (one air tick short, 2.217),
not 0.0784's 2.052. **INFERRED** that his server velocity held exactly that at each blast: no row read it. **The new
drag cannot repeat it:** it has no lift, and it never sends the server's copy of a player's velocity. **The tolerance on
every figure resting on his own physics is ± 0.20 blocks**, BL9R3's 0.158 residual rounded up (`GATE-implosion-drag.md`
J5).

**D3. IM4's 0.41 WEST: THE PUSH OUT OF AN OVERLAPPING MOB, ON BOTH SIDES.** `LivingEntity#pushEntities` runs on client
and server alike (READ: no side gate; Paper adds its collision config), and `Entity#push(Entity)` moves each body
`0.05` a tick along the axis apart when they overlap by `0.01` or more (READ: `ldc 0.009999999776482582d`, `ldc
0.05000000074505806d`). On the client that pushed him out of the overlap (IM4's 0.19 before the blast). On the server
it lands in the server's copy of his velocity, and `Entity#push(DDD, Entity)` sets `needsSync`, never `hurtMarked`
(READ), so it is never sent. **The blast then sent it, with the lift**: 0.41 west over his air time needs about 0.068 a
tick at take-off (EXECUTED, `d3.jsh`), of the order of that push. **The size is INFERRED.** IM4R2 is the control.

**THE QUANTITY (`Implosion`).** `duration_ticks`: 1 is the single pull, unchanged (`pull`, `lift`, the rest-point cap,
once). 2 or more is a DRAG: `pull` and `lift` must be 0 (refused by name otherwise), and `drag` is **BLOCKS PER TICK of
horizontal velocity ADDED toward the origin each tick, at the origin, times `1 − d / reach` and `1 − resistance`**,
each tick's add cut so the speed toward the origin is at most `(h − 0.5) / travelFactor(0)`, the speed whose slide ends
at the margin (`Implosion.dragStep`). Where `pull` scaled a velocity given ONCE, `drag` is given every tick and ADDED to
what he carries. `ImplosionDrag` runs one second: tick 0 is the blast's (it looks), ticks 1-20 pull; one who leaves the
reach is not pulled, one who enters is, from his second sighting; one who is dead, gone, elsewhere or no longer allowed
is dropped. `DragMotion` is the one tally: two drags on one body in one tick add to one velocity.

**THE NUMBERS FOR BEN, the seat's starting values:** `duration_ticks: 20`, **`drag: 0.28`** (CHOSEN), `pull: 0`, `lift:
0`, the 0.5 margin unchanged; the swirl's `duration_ticks` 10 → 20 (CHOSEN). **The read movement facts:** walking adds
0.096040 a tick (terminal 0.21154), sprinting 0.124852 (terminal 0.27500) (`ImplosionDragTest.theReadMovementFacts`).
**The four outcomes at a stage 5, EXECUTED through the real classes** (scratch `figures.jsh`; the table and why 0.28 in
`GATE-implosion-drag.md`, *THE DRAG AS BUILT*): a still player from 2, 3, 4 or 5 rests at the margin, 0.500; walking
away from 3 he ends the second at 0.769 (from rest) or 1.339 (already walking); sprinting away from 4, 4.5, 5 or 5.9 he
is out of the reach by tick 14, 9, 6 or 1; at the margin he is held with no tick nearer than the last, and inside it
nothing is sent. **For Ben, as information:** walking away from 4 he is still inside at the second's end (5.19).
**Between two origins:** each pulls toward its own and its margin cuts only its own add; one origin between him and
the other cannot stop the far one carrying him past it. **Unruled, listed for Ben.**

**THE SWIRL (CHOSEN):** both spirals of `void_implosion.yml` drawn over 20 ticks, the same curve, half as fast; nothing
else changed. **PART D:** the selftest's new `light: <tag>` lights a Phage's own fuse with no target and holds it lit
(`MobFuse#light`), so a FROZEN stage 5 twelve blocks away swells and blows; `tell: <text>` shows gold text before it.
**NOT IN THIS STEP, as briefed:** a suction during the fuse; cover; an element on the damage; any shipped ability or
weapon using the shape.

### Ben, 2026-11-18, VERBATIM, BY THE SELFTEST'S ASK STEP: the drag (the boot of `39c0b341`, `GATE-implosion-drag.md` BQ2)

*Each question as the selftest asked it, then his answer, copied from the ASK lines of the log. **Run 1** is the run
sheet's one run; **run 2** is the second run he started after it (a departure, recorded apart, not read as rows).*

1. *"Did you see the big Phage swell up before it blew?"*
   > run 1: "yes"
   > run 2: "yes"
2. *"Standing still, did it drag you in toward where it stood, for about a second?"*
   > run 1: "yes"
   > run 2: "yes"
3. *"Walking away, did the drag slow you down without stopping you, so you still went back toward it?"*
   > run 1: "yes"
   > run 2: "it did stop me i think"
4. *"Sprinting away from the edge, did you get out of the drag?"*
   > run 1: "yes"
   > run 2: "the pull did almost nothing"
5. *"Does the pull feel right now?"*
   > run 1: "not quite going to run the tests again"
   > run 2: "i think it needs to be a little bit stronger"

**CONFIRMED BY PLAY:** the swell shows on his client; in the first run the drag pulled him in for about a second, slowed
his walk without stopping it, and let him sprint out from the edge.

**NOT CONFIRMED:** the feel ("not quite"; "i think it needs to be a little bit stronger"). **The seat's reading, told to
Ben:** first as "not reliable", since the same build dragged a still player to 0.491 in one run and only to 1.347 in
the next; the drag's 0.28 is not raised until that variation is explained (carried as a DEFECT, §8).

### THE DRAG MUST NOT DEPEND ON PACKET TIMING: THE DIAGNOSIS, AND THE SEAT'S RULING (2026-11-18, `feat/drag-additive`)

**THE TWO RUNS OF `39c0b341`, SIDE BY SIDE.** Every DRAG line of both `/rpg selftest implosion-drag` runs, from
`run/logs/latest.log` (run 1 11:54:57 to 11:57:16, run 2 11:57:22 to 11:59:46), regenerated in this branch with `grep -n
"SELFTEST implosion-drag .* START\|DRAG source" run/logs/latest.log`. Same build, same staging, same player. *The first
session's own table is not in the repo; this one is rebuilt from the log, and agrees with every figure the gate quotes.*

| row | run 1 `start → end`, `sent` | run 2 `start → end`, `sent` |
|---|---|---|
| BL1R4 | 0.491 → 0.491, 0 | 0.491 → 0.491, 0 |
| BL2R4 | 2.000 → 0.479, 7 | 2.000 → 0.493, 8 |
| BL6R4 | 1.950 → 0.532, 16 | 1.950 → **0.837**, 20 |
| BL9R4 | 3.000 → 0.475, 8 | 3.000 → 0.504, 18 |
| IM1R2 | 5.900 → 5.506, 20 | 5.900 → **5.713**, 20 |
| IM3R2 | 2.000 → 0.479, 7 | 2.000 → 0.502, 17 |
| IM4R2 | 0.487 → 0.506, 18 | 0.425 → 0.506, 19 |
| IM5R2 | 3.950 → 3.515, 20 | 3.950 → 3.606, 20 |
| FZ1R2 | 0.950 → 0.482, 3 | 0.950 → 0.504, 14 |
| FZ3R2 | 1.950 → 0.475, 9 | 1.950 → 0.475, 9 |
| FZ5R2 | 2.950 → 0.552, 18 | 2.950 → **0.382**, 9 |
| DS1R6 X / Y | 0.577 → 0.138, 2 / 0.989 → 0.505, 16 | 0.615 → 0.085, 4 / 0.994 → 0.506, 14 |
| DR1 | 3.000 → 6.728, 6 (`left`) | 3.000 → 8.339, 6 (`left`) |
| DR2 A / B | 3.000 → 0.564, 20 / 3.964 → 5.255, 20 | 3.000 → 0.551, 17 / 3.510 → 5.366, 20 |
| BQ2 q2 (still, from 4) | 4.000 → 0.491, 15, `held` | 4.000 → **1.347**, 20, `pulled` |
| BQ2 q3 (walking) | 3.200 → 2.102, 20 | 3.200 → **3.342**, 20 |
| BQ2 q4 (sprinting) | 4.700 → 9.592, 7 ticks (`left`) | 4.700 → 8.812, 8 ticks (`left`) |

**`sent` VARIES ROW BY ROW ON IDENTICAL STAGING** (7 against 8, 8 against 18, 3 against 14, 18 against 9): a send is
made only on a tick the estimate exists and the cap leaves room, so the count follows the move packets that arrived.

**THE CANDIDATES, WITH THEIR MARKS:**
1. **A tick with no move packet reads as "at rest", and the send wipes the momentum the drag had built.** `DragMotion`
   estimated his momentum from the last two positions the server saw; a tick on which no move packet arrived repeats
   the last position, so the displacement is 0, the estimate 0, and the velocity sent (0 plus the add) REPLACES what he
   carried (READ: `ClientPacketListener#handleSetEntityMotion` → `Entity#lerpMotion` → `setDeltaMovement`).
   **INFERRED**: no instrument recorded his move packets per tick.
2. **A late send replaces a newer momentum with an older one.** A send built from tick n's estimate that lands after
   his tick n+1 replaces the momentum of n+1 with n's. **INFERRED**, for the same reason.
3. **He moved, or held a key, in run 2.** He started run 2 having decided to look again, and no line read his keys.
   **UNREAD, not ruled out.**
4. **The server's own copy of his velocity was sent.** **RULED OUT BY THE CODE**: the drag never sent it (D2).

**THE GATE'S J2 ASSUMPTION IS CONTRADICTED.** J2 marked as INFERRED "one move packet a server tick, and each send
reaching him the next tick (a local server)". **The server was not local to him:** his login line reads
`BaronVonYeetus[/192.168.68.90:56227] logged in with entity id 13` (11:54:32; again from `:64715` at 11:54:48), a
second machine on his LAN. Real players will be remote. So the estimate's two assumptions were never the game's.

**THE SEAT'S RULING (2026-11-18), its words in the brief:** *"no design that needs the server to know the client's
momentum is acceptable."* The diagnostic boot proposed was NOT run on its own; its instrument rides in this branch
(DRAGTICK, `GATE-drag-additive.md`).

**THE SEAT'S NUMBER, TOLD TO BEN AS THE SEAT'S:** `drag` **0.28 → 0.33**, because he answered "not quite" after the run
in which the drag worked as modelled, and "i think it needs to be a little bit stronger" after the second. **Sprinting
out will then work only from nearer the edge.**

### Ben, 2026-11-18, VERBATIM, BY THE SELFTEST'S ASK STEP: the additive drag (the boot of `5ee0587a`, `GATE-drag-additive.md` BQ3)

1. *"Standing still, did it drag you in toward where it stood, for about a second?"*
   > "yes"
2. *"Did that drag feel the same as the first one?"*
   > "not sure"
3. *"Walking away, did the drag slow you down without stopping you, so you still went back toward it?"*
   > "it basically stopped me which is what I want for walking"
4. *"Sprinting away from the edge, did you get out of the drag?"*
   > "yes"
5. *"During any of those drags, did you feel a stutter, or get snapped back to where you were?"*
   > "no"
6. *"Does the pull feel right now?"*
   > "feels good"

**CONFIRMED BY PLAY:** the drag pulls him in for about a second; walking away is "basically stopped", which he says is
what he wants; sprinting from the edge gets him out; no stutter or snap back; the pull **"feels good" at drag 0.33**
(the seat's number, now confirmed by play). **NOT STATED:** whether two drags felt the same ("not sure"; the server read
them identically, `start=4.000 end=1.056` each).

### THE ADDITIVE DRAG AS BUILT (`feat/drag-additive`, 2026-11-18). THE ACCOUNT IS `GATE-drag-additive.md`

*This is the pointer; B1-B4 with every READ line, the outcome table, the bands and the mutations are the gate's.*

- **THE ROUTE (B4):** a player is sent each tick only the ADD, as a `ClientboundExplodePacket` whose `playerKnockback`
  his client ADDS (`Entity#addDeltaMovement`), with the empty sound, an unseen particle 256 blocks up and no block
  particles (`DragImpulse`). **Not B1:** Paper's teleport cannot carry a delta (`Vec3.ZERO`), and any position packet
  makes the server drop his moves until he confirms it (B2), while a confirm of an id we chose that matches the server's
  last kicks him.
- **THE CAP WITHOUT HIS MOMENTUM:** the add is `min(drag x (1 - d/R) x (1 - res), gain x max(0, h - 0.5))`, the gain
  the largest that never carries a still victim past the margin while the server's view lags 0 to 2 ticks
  (`Implosion.holdGain`, CHOSEN allowance; stone 0.04067, EXECUTED). **What it does not guarantee:** a player running
  INTO the Phage passes it; a lag of 3 or more can overshoot; near the margin his walking outweighs the hold; on ice or
  in the air the gain is a tenth or less.
- **`DragMotion`'s estimate is gone;** what remains is the mob's per-tick tally. A mob's drag is unchanged.
- **The numbers at 0.33, EXECUTED:** a still player from 2, 3, 4 or 5 rests 0.578 to 1.397 from the feet within the
  allowance; walking away from 3 he ends 2.92 to 2.97 (outcome b holds at lags 0 to 3, NARROWLY, and fails by 0.001 at
  4); sprinting from rest he gets out from 3.9 to 4.3 by the lag (4.0 at lag 1, against 3.8 at 0.28).
- **The instrument (Part D):** `DRAGTICK`, on by the selftest's `drag_tick: on`, off at the run's end; its moves counted
  on the Netty thread and nothing else (`MoveCountListener`).
- **OPEN FOR BEN AND THE SEAT, unruled:** the allowance (2 ticks); that the hold makes walking away nearly a standoff;
  the jump that escapes the drag in the air; the two-origin case.

---

## THE cfde822 SURVEY, 2026-11-06 — ITS ORIGINAL HEADER, KEPT AS THE RECORD

**Phase 1: SURVEY ONLY. No Java, no content, no boot, nothing ruled.** It covers the five mobs Ben picked on
2026-09-29 (`PLAN-legacy-port.md` RULINGS): **the Knell, Skreel (with its spider mount), Phalanx, Phage (with the
Lesser Phage it splits into) and Goldrot Goon.** For each it records what cfde822 did, what our mob schema would
need to express it, and a proposed build order. Then it states the design constraints for the later dragon fight.
**Ben rules every number and every order.**

**Read against** (the survey; **the rework was read against master `cc2831ef`** (#188) and the pinned jar, §7):
- master **`cd790dcc`** (#176, the docs tidy);
- the old project `CreaperCrusher/Butterfly-SMP` at **`cfde822`** (`cfde8222ec2ac35661963776e9d8f0582bc4e12a`),
  package `org.example1.butterflysmp`. It is not a local clone; every old file below was fetched with
  `gh api "repos/CreaperCrusher/Butterfly-SMP/contents/<path>?ref=cfde8222ec2ac35661963776e9d8f0582bc4e12a"`.

**What this builds on, not redoes:** `PLAN-legacy-port.md` §2.5 (Knell Beam), §3 (the five mobs, the spawn table,
the Knell OLD vs NEW), §4 (today's schema), §6 (findings F8, F10, F13, F14, F15) and §9.1/§9.3 (the capability ids
**MB1–MB5** and **E5**, reused here unchanged so the two files name one thing one way). Every fact carried over from it
was re-read at cfde822. **Where the two disagree, §2 says so.** `PLAN-mob-scaling.md` supplies M4, M17, M21, M24,
M25, M27 and §6 F15 / F22.

**Citations** name a CLASS and a METHOD or a SECTION, never a line, and that includes the old repo. The short paths
are `PLAN-build-system.md`'s: `P/` = `paper/src/main/java/io/github/butterflysmp/rpg/paper/`, `C/` =
`core/src/main/java/io/github/butterflysmp/rpg/core/`, `content/` = `paper/src/main/resources/content/`.

**HOW TO READ A NUMBER IN THIS FILE:**
- A number labelled **cfde822** is a FACT about what the old code did. It is recorded, **never adopted**.
- Every number a port would author reads **`___ (Ben)`**.
- **Old mob HP was absolute**, set by `MobSpawnHelpers.applyVanillaMaxHp` onto the vanilla `MAX_HEALTH` attribute.
  There was no gear score. Ours is a custom store × GS/100 (M4), so no cfde822 HP transfers as-is.
- **Old mob timers in these classes are TICKS, not milliseconds.** Every timer below came from a `runTaskLater` /
  `runTaskTimer` argument or a tick counter, so nothing needs the 50 ms/tick conversion. Seconds are shown beside
  ticks for reading only (20 t = 1 s). *(The ms conversion in `PLAN-legacy-port.md` applies to weapon cooldowns.)*
- **Old mob damage came in two kinds.** A vanilla melee or arrow hit on a player was ×5
  (`CombatListener.onMobDamageMultiplier`, `Formulas.MOB_DAMAGE_MULTIPLIER = 5.0`) times a mob-level factor. A
  scripted literal (the beam, blast, swirl, leap) was **exempt** from the ×5 (`CombatListener.runMobDamageMultiplierExempt`).
  These five carry no `MOB_LEVEL`, so their level factor is 1 (*inferred*: `MobLootListener.onHostileMobDeathCoins`'s
  comment says BOSS_NAME mobs never get a level; `Formulas.mobLevelDamageFactor` was not read). **Item-level scaling
  (`GearScore.scalePower`) is a weapon fact and does not apply to any mob number here.**

**READ vs INFERRED.** Every claim is marked:
- *read*: I opened the method at the stated ref;
- *inferred*: reasoned from code around it, not opened or not run;
- *UNVERIFIED*: a Paper or vanilla behaviour not read from the pinned jar or the server jar.

---

## 1. WHAT I READ

**At cfde822, in full** (13 files fetched to a scratch directory and read; the list is the `fetch` arguments):
- `encounter/withered/`: `WitheredMobFactory`, `WitheredKnellBehavior`, `WitheredKnightSystem` (a facade),
  `WitheredKnightUtil`, `WitheredMinionDeathListener`, `WitheredPhalanxBehavior`, `WitheredSpiderJockeyBehavior`;
- `encounter/dragonfight/`: `DragonfightManager`, `DragonfightReinforcements`, `DragonfightFactionListener`,
  `DragonfightCombatListener`;
- `mob/MobSpawnHelpers`, `mob/MobLootListener`.

**At cfde822, in part:** `CombatListener.onMobDamageMultiplier` / `runMobDamageMultiplierExempt`;
`Formulas.MOB_DAMAGE_MULTIPLIER` (the constant only); `PlayerInteracts.getNearestPlayer`;
`RangedListener.resolveBounceAwareKiller`; `CatalogueRegistry`'s mob list (`MobEntry` rows); the
`spawnWitheredKnight` call in `WitheredCommanderEncounter` (grep only).

**Found by** `gh api "repos/CreaperCrusher/Butterfly-SMP/git/trees/cfde8222ec2ac35661963776e9d8f0582bc4e12a?recursive=1"
--jq '.tree[].path'` (323 paths) filtered with `grep -i -E 'dragon|knell|skreel|phalanx|phage|goldrot|goon|mob|wither|spider|minion|faction'`.
**No class is named after any of the five mobs.** They live under their pre-rename names: Phage = `Grunt`, Goldrot
Goon = `Rouge`, Phalanx = `Archer`, Knell = `Knight`, Skreel = `SpiderJockey` (*read*: `WitheredMobFactory`'s
`spawnWithered*` methods and the PDC `BOSS_NAME` strings they set).

**In this repo, at `cd790dcc`:** `content/mobs/knell.yml` (the only mob file; `ls content/mobs/`);
`C/mob/MobDefinition`; `P/content/MobLoader`; `P/command/RpgCommand.spawnMob`; `P/content/ContentValidator.validateMobs`;
`C/combat/ProjectileFlight.nearestMob`; `C/combat/Caster`'s record header; `C/mob/MobDamagePricing`'s class javadoc;
`AbilityService.cast`'s signature; `storage/.../PlayerRepository` (that it exists). **Not read:** the
`Scheduler` interface itself (only its `onEntity` call sites in `P/listener/RpgListeners`).

---

## 2. WHERE cfde822 DISAGREES WITH `PLAN-legacy-port.md`, OR ADDS TO IT

None of these changes a tier in that file. Each changes what a port would have to reproduce.

- **D1 — A WAVE IS CAPPED BY THE NEXT PHASE'S ROW.** `DragonfightManager.onDragonfightDamage` writes the new phase
  index into its phase map and only then schedules `spawnPhaseWave(world, phaseToRun)` for the next tick (*read*: the
  order of the two statements). Every `spawnWithered*` reads its cap through `currentDragonPhaseInWorld`, which reads
  that map. So **wave 0 is capped by row 1, wave 1 by row 2, wave 2 by row 3** (*inferred* from the order; not run).
  The legacy table's cap column is correct as a table; which row a wave meets was not stated. Every wave count fits
  its row on its own, so a wave is clipped only by minions already alive from the trickle (*inferred*).
- **D2 — THE GOON'S BLAST PROBABLY SPARES OTHER MINIONS.** The legacy file says the blast hits *"other minions and
  players alike."* The source's own javadoc on `WitheredMinionDeathListener.onGoldrotGoonDeathExplode` says the
  opposite: friendly fire between minions is cancelled, *"so the explosion can't chain through other Goons / Phages
  / Archers."* The blast calls `le.damage(50.0, ws)` with the dead Goon as the source (*read*), and
  `DragonfightFactionListener.onDragonfightFriendlyFire` cancels an `EntityDamageByEntityEvent` whose damager and
  victim both carry `DRAGONFIGHT_MINION` (*read*). **Whether `LivingEntity.damage(double, Entity)` raises that event is
  *UNVERIFIED*.** If it does, the blast hits players, Endermen and any untagged mob, and no minion. The dragon is
  skipped by name either way (*read*).
- **D3 — THERE WAS A FACTION WAR WITH ENDERMEN.** The legacy file records only *"no friendly fire between them."*
  `DragonfightFactionListener.tickDragonfightFactions` also ran every 20 t while a fight was live (*read*):
  - each registered minion with no target, a dead target or a friendly target was pointed at the **nearest Enderman
    within 30 blocks**;
  - each Enderman with no live target was pointed at the **nearest minion within 30 blocks**;
  - `onEndermanSpawnDuringDragonfight` cancelled **NATURAL** Enderman spawns in the End inside |x| ≤ 200 and |z| ≤ 200
    while any fight was active.
- **D4 — THE PHALANX'S BLINK TRIGGER IS A BOX, NOT A RADIUS.** `schedulePhalanxBehavior` queries
  `getNearbyEntities(loc, 5.0, 5.0, 5.0)`, which takes half-extents (*UNVERIFIED*: the Bukkit signature's meaning), so a
  player anywhere in a 10 × 10 × 10 box triggers it. Creative and spectator players are skipped (*read*).
- **D5 — THE PHALANX'S SHIELD EATS THE FIRST DAMAGE OF ANY KIND.** `onPhalanxShieldAbsorb` listens on
  `EntityDamageEvent`, not the by-entity event (*read*). So a fall, fire, or a blast spends it as readily as a
  player's hit. The legacy file says *"the first hit it takes."*
- **D6 — THE KNELL'S DODGE ALSO CATCHES HITSCAN.** `onWitheredKnightDodgeProjectile` fires on a `Projectile` damager
  **or** on any by-entity hit whose damage type is `ARROW` (the Boltor-style hitscan) (*read*). It always cancels the
  damage. If both sidestep landings are blocked it **cancels and does not move** (`teleportKnightSideways` returns
  early; *read*). So a Knell backed against a wall is immune to arrows and moves nowhere.
- **D7 — THE SKREEL DOES NOT CIRCLE.** `circleAroundPlayer`'s own javadoc says it returns a point **along the
  player-to-spider axis** at 12 blocks, *"so the spider doesn't teleport-circle"* (*read*). The legacy file says
  *"Retreat circles to a stand-off band."* Also *read*: the loop starts **20 t after spawn**; the wind-up aborts if the
  player leaves **6.4–18 blocks** (0.8 × 8 and 1.5 × 12); the landing hit's damager is **the rider, not the spider**.
- **D8 — cfde822 CONTRADICTS ITSELF ON THE SPIDER'S SURVIVOR SPEED.** The class javadoc of
  `scheduleWitheredSpiderJockeyBehavior` says the survivor gets *"default AI + speed buff"*. `convertSpiderToBrawler`
  **divides** the speed by 1.3, removing the paired buff (*read*). The code governs what it did. Neither governs a port.
- **D9 — `applyDragonfightFollowRange` IS ON `DragonfightManager`**, not on `MobSpawnHelpers` (*read*: both classes).
  It sets `FOLLOW_RANGE` base to `DRAGONFIGHT_WITHER_FOLLOW_RANGE` = 64 on every minion, the spider included.
- **D10 — COINS WERE PAID EVEN THOUGH DROPS WERE STRIPPED.** `DragonfightManager.onBossEntityDeath` clears drops and
  XP on a tagged minion (*read*). `MobLootListener.onHostileMobDeathCoins` pays coins with `plugin.addTreasure`, a
  direct credit rather than a drop (*inferred*: `addTreasure` not opened). Its table pays Knell 15, Goon 8, Phalanx 8,
  Phage 5, Mount 5, Lesser Phage 2, **and keys the Skreel as `"§7Withered Spider Jockey"`**, which no mob carries, so
  the Skreel falls through to the type default (*read*; legacy F10 says the same).
- **D11 — THE CATALOGUE'S HP COLUMN IS STALE, CONFIRMED.** `CatalogueRegistry`'s `MobEntry` rows read Phage 300,
  Goldrot Goon 150, Phalanx 400, Knell (listed as *"Withered Knight"*) 600, Skreel 200. The factory sets 160, 80, 240,
  480, 120 (*read*, both). The factory is what spawned.

---

## 3. THE MOBS

### 3.R THE REWORK, 2026-11-15 — FOUR MOBS. THIS GOVERNS; §3.1–§3.5 BELOW ARE THE cfde822 RECORD

Each mob reads **cfde822 did / Ben's design now / what it needs**. Every number a port would author is in ONE list,
§3.R.5 *Numbers for Ben*; nothing below picks one. The facts cited as **F-C**, **F-S**, **F-E** and **F-T** are §7's,
read from the pinned jar.

#### 3.R.0 Shared: the element on a mob, and "target"

**What it needs (ruling 2):**
- **A mob file names its element**: a new optional key, `element: void`, naming an existing `content/elements` id.
  `MobDefinition` gains the component; `MobLoader.parse` reads it; `ContentValidator` names a dangling id at boot, as
  it does a dangling `visual_id` (*inferred* placement, by analogy with the existing validator checks).
- **A vanilla type needs a table**, because an Enderman has no mob file. **Proposed home: one new content file,
  `content/mob_elements.yml`**, a map from a bare entity-type key to an element id (`enderman: void`), using the same
  bare-key convention as `withering.yml`'s `immune` list. **It must NOT live in `content/mobs/`**: `MobLoader.loadAll`
  parses every `*.yml` there as a mob, so a table file in that directory would load as a malformed mob (*read*:
  `MobLoader.loadAll`'s filter is `n.endsWith(".yml")`).
- **Resolution order:** the mob's `mob_id` → its file's `element`; else the entity's type → the table; else **no
  element**. A creature in neither (a zombie, a cow, a player) has no element, and nothing counts it as Void.
- **What an element on a mob does on day one: IDENTITY ONLY.** The Phage's growth asks "was the dead mob Void?"
  and nothing else asks. **It does NOT type the mob's own damage and gives NO resistance** unless Ben rules so.
  This matches `ElementDefinition`'s own javadoc: *"an element is never MATH. It multiplies nothing."* (*read*).
- **Where the code reads it:** at the one death observation point (§7 F-E), on the dying mob's own thread, captured
  before the body is gone, the same capture discipline `RpgListeners.onEntityDeath` already follows for Ignite.

**Target (ruling 3):** every new capability takes a combatant as its target or victim, never a `Player`. **Today the
engine's only "side" is a boolean, `player`**, on `HealthState` and `CombatantSnapshot`, and player-cast effects pick
their victims as "not the caster, not a player" (§7 F-E). The mob capabilities here need the mirror: **"a combatant
that is hostile to mobs"**, which today is exactly *a player*. **A Summoner would add a second member to that set**, so
the predicate must be one named function, not a `player()` test repeated at each site.

#### 3.R.1 The Knell

| | |
|---|---|
| **cfde822 did** | §3.1: a beam every 100–200 t with a 12 t telegraph, and, separately, a sidestep 4–5 blocks on every arrow, with no cooldown. |
| **Ben's design now** | **One ability, in two beats.** The beam as before. **When the beam HITS its target, the Knell teleports a tunable delay later** (B: starting equal to the telegraph). **No warning beyond the hit** (C). It lands on the circle around the target at its current distance, 30°–70° around, side random (A, ruling 4), and only on a VALID point (Ben's second-round A). **A miss moves nothing. Arrows no longer move it.** Element `void` (H). |
| **what it needs** | MB1 body; MB3 + E5 for the beam, with **the beam reporting WHICH combatant it hit** (the teleport anchors on that target, not on a player); a **post-hit arc teleport with a landing check** (§4.2 N8), whose VALID test is §7 F-T. **MB4's pre-damage dodge is no longer needed by anyone.** |

**The landing check, as ruled (ruling 4):** walk the arc on the chosen side, then the other; take the first VALID
point. VALID = (1) the Knell's body fits there without collision, (2) solid ground directly beneath, (3) within a
tolerance of the target's level. **Open for Ben: whether VALID also requires line of sight to the target**, so the
Knell never lands behind a wall from the player it just hit (Q18).

#### 3.R.1.a THE BEAM, INHERITED OR CHOSEN, FOR BEN TO SEE (step R5, 2026-11-17)

**The seat's ruling 1 for R5: where Ben has not re-ruled, the beam behaves as cfde822's did.** Ben re-ruled only the
numbers (RULINGS, *the Knell's beam starts from the old game's numbers*) and the teleport that follows a hit (R6). Every
other behaviour is below. **READ** = stated in §3.1's record of cfde822 (or `PLAN-legacy-port.md` §2.5, which §3.1
cites). **NOT IN THE RECORD** = the record is silent, so step R5 CHOSE the simplest behaviour and says what it chose.
The old source was not re-fetched for this list; each NOT IN THE RECORD item could be turned into a READ by fetching
`WitheredKnellBehavior` at cfde822, if Ben or the seat wants that before a choice stands.

| # | behaviour | mark | what R5 does |
|---|---|---|---|
| I1 | the wait between beams | **READ** (§3.1: "a random 100–200 t (5–10 s) delay") | a whole number of ticks drawn uniformly from `cycle_ticks.min` to `cycle_ticks.max`, both included (content: 100, 200). *Inclusive*: NOT IN THE RECORD, chosen |
| I2 | when the next wait starts | **READ** ("the next random 100–200 t starts after it fires") | a fresh draw at the fire, hit or miss |
| I3 | when the FIRST wait starts | NOT IN THE RECORD | when the Knell enters the world (a spawn, or its chunk loading), the moment our seed runs. **So the first beam comes 100–200 ticks after the Knell appears, not after it gains a target** (the cycle does not wait for one; I5) |
| I4 | who it aims at | **READ** ("the vanilla target if it is a player, else the nearest non-creative, non-spectator player within 40") | the Knell's vanilla target if `MobHostility.hostileTo` names it and it is in range; else the nearest combatant `hostileTo` names within range. A Summoner's summon joins by the one predicate (seat ruling 3) |
| I5 | no one to aim at | **READ** ("No player in range: wait another cycle") | a fresh draw; no line in the log (a FORCED attempt logs `BEAM none`) |
| I6 | how "in range" is measured | NOT IN THE RECORD (it says "within 40") | the straight line from the Knell's EYE to the target's AIM POINT (I8) is at most `range` blocks, inclusive. The same two points the line is drawn between, so a target in range is one the line can reach |
| I7 | does starting need sight | NOT IN THE RECORD | **no.** The record names no sight check; blocks stop the beam at the fire (I13), so a target behind a wall is telegraphed at and not hit |
| I8 | the telegraph fixes the line | **READ** ("locks origin (its eye) and endpoint (the target at 0.6 × its height) once") | the origin is the Knell's eye, the direction is the eye to the target's feet + `aim_height` (0.6, a FRACTION of its height), both fixed at the telegraph's first tick. **It does not track** |
| I9 | the telegraph's length | **READ** (12 t) | `telegraph_ticks` (12): the fire is exactly 12 ticks after the telegraph's first tick |
| I10 | the telegraph's sight and sound | **READ** (a purple dust line RGB 95,35,165, size 0.8, at tick 0 and tick 9; a bell resonate, volume 1.4, pitch 0.7, `PLAN-legacy-port.md` §2.5) | as read, drawn along the locked line to its full range. The two flash ticks are content (`flash_at: [0, 9]`), each inside the telegraph, so a shorter telegraph cannot silently lose a flash. *Particle spacing (one every 0.5 blocks) and the sound's category (hostile)*: NOT IN THE RECORD, chosen |
| I11 | the Knell is rooted | **READ** (`stopPathfinding()` every tick for 12 t) | `stopPathfinding()` every telegraph tick, **AND a MOVEMENT_SPEED × 0 modifier for the telegraph, which the record does not have.** Why: a melee goal re-paths on its own ticks (INFERRED from vanilla's `MeleeAttackGoal`, not read), so stopping the path alone may let the Knell drift; and at the lock, ONCE, its horizontal velocity is zeroed, so a
walking Knell does not slide on its momentum (a push during the telegraph is not undone, I18). The modifier is TRANSIENT (`addTransientModifier`, READ in the pinned API), so a chunk unload mid-telegraph cannot save it onto the mob. **The seat should rule whether the extra modifier stands** |
| I12 | what the fire tests | **READ** ("Hits only the locked player, and only if that player's box grown by 1.0 still meets the segment") | the LOCKED target only, at the fire, with its box at that moment grown by `tolerance` (1.0) on every side, against the segment. Every other creature the grown-box test catches is LISTED in the log as spared and never hurt |
| I13 | do blocks stop it | **READ** ("block-clipped") | the segment runs from the eye along the locked direction to the first block with a collision box, or to `range`. *Fluids and passable blocks (grass, flowers) do not stop it*: NOT IN THE RECORD, chosen |
| I14 | does the segment end at the aim point or run on | NOT IN THE RECORD | it runs on to the range (or the first block): a target who backs away along the line is still in it. The Knell Beam weapon's ray is the same length (`PLAN-legacy-port.md` §2.5) |
| I15 | the target dies, is removed or leaves the world mid-telegraph | NOT IN THE RECORD | the beam still fires along its line; the target is listed `gone` and nothing is hurt |
| I16 | the target leaves range mid-telegraph | NOT IN THE RECORD | nothing special: the line is fixed and runs to the range, so a target is hit exactly when it is still in the line |
| I17 | the target turns creative or spectator mid-telegraph | NOT IN THE RECORD | re-asked at the fire: `MobHostility.hostileTo` false → listed `exempt`, not hit |
| I18 | the Knell is hurt mid-telegraph | NOT IN THE RECORD | nothing: the telegraph runs on |
| I19 | the Knell dies or is removed mid-telegraph | NOT IN THE RECORD (cfde822's loops ended with their mob, §6 C4) | **nothing fires.** The loop stops the tick it finds the Knell gone and logs `BEAM cancelled` |
| I20 | the fire's sight and sound | **READ** (a cyan dust line RGB 40,230,235, size 1.4; a bell and an amethyst chime) | as read, along the clipped segment. *Their volume and pitch*: NOT IN THE RECORD (only the resonate's pair is), chosen 1.0 and 1.0 |
| I21 | damage | **READ** (35, `MAGIC`, ×5-exempt) | `damage` (35) × GS/100 (the seat's provisional) through `MobScaling.attackDamage`, dealt by the mob-hit path's final call, `applyDamage(amount, knellId)`: the player's defence applies, **no element, no knockback, no crit, and no shield block** (that path raises no vanilla event; Q31, Q32 for the blast are the same questions here) |
| I22 | a creative or spectator player | **READ** (skipped) | never aimed at and never hit (`MobHostility`) |
| I23 | a Knell with no AI (NoAI, "frozen") | NOT IN THE RECORD (cfde822 never froze a mob) | **its own cycle never begins a beam** (an attempt is a fresh draw), as a vanilla NoAI mob attacks nothing. The selftest's forced beam still works on one. **This is also what keeps every older selftest row's frozen Knell from beaming the player** (the R5 audit) |
| I24 | a Knell under our Freeze status | NOT IN THE RECORD | **not handled**: Freeze cancels vanilla melee events, and the beam raises none, so a Freeze-immobilised Knell still beams. Recorded for Ben, not built |

#### 3.R.2 The Phage — one name, five stages, on a creeper

| | |
|---|---|
| **cfde822 did** | §3.2 and §3.3: two wither skeletons. The Phage split into 2 Lesser Phages on a player kill; the Goon exploded on a player kill. Neither grew. |
| **Ben's design now** | **A creeper body** (retextured later). **Stage 5** is about 3 blocks tall, very slow, and explodes near a player for a lot. **Stage 1** is fast, low HP, and explodes near a player for a little. **Killed at stage 5, it splits into two stage 1s** (E: exploding does not split). **A Phage below stage 5 grows one stage per Void death in range** (I, G1–G2), so a regrown stage 5 can split again. **Nothing caps it** (answer D). A player can back away to cancel the fuse (F, *"I might change my mind in testing"*). The blast hurts **players and summons only** (G). Element `void`. |
| **what it needs** | MB1 body fields **applied to a LIVE mob** (a stage change rescales it); a **stage** as state on the mob; a **proximity self-destruct** that is ours (trigger, fuse, back-away cancel, priced damage, no blocks); a **split on kill** that tells "killed" from "exploded"; **growth** fed by Void deaths in range. §4.2 N4–N6. |

**Three jar facts shape the Phage (§7 F-C), and each is a reason it cannot simply be a vanilla creeper with numbers
changed:**
- **Vanilla's trigger distances are constants, not fields.** `SwellGoal` starts at 3 blocks (`distanceToSqr < 9.0`)
  and cancels past 7 (`> 49.0`), centre to centre, and **neither follows `SCALE`**. Ruling 5 makes the trigger
  distance content, so **the trigger has to be ours**, replacing vanilla's goal.
- **`ignite()` defeats the back-away cancel.** An ignited creeper forces its swell direction up every tick
  (`Creeper.tick`), so a fuse started with `ignite()` cannot be walked away from.
- **A vanilla creeper explosion does not die, it is discarded**, and the blast hurts every entity in range before
  any cancellable block event fires. So the explosion is ours too: cancel vanilla's at `ExplosionPrimeEvent` (the
  creeper then survives with its swell reset), deal our own priced damage to players and summons, and remove the
  Phage ourselves **with a reason we record**, so the split can ask "killed or exploded?".

**Size:** a creeper is 0.6 × 1.7 blocks (§7 F-C), so *"about 3 blocks tall"* is a `SCALE` near 3 / 1.7. **The number is
Ben's**; it is shown only so the quantity is named. The hitbox follows `SCALE` on a live mob (§7 F-C).

**Open for Ben (ruling 5, plus three the rework raises):**
- **Growing a stage: heal to full, or keep current HP as a fraction of the new max?** (Q19)
- **A Void death while a Phage is mid-fuse: does it still count?** (Q20)
- **Does a stage 5's own death count toward its two new children?** The children appear at the corpse, inside the
  radius. If the split happens before the death is counted, each child is born at stage 2 (Q21).
- **Do the children inherit the parent's gear score?** M21 already rules that slime and magma-cube children inherit
  their parent's GS, and `RpgListeners.onMobTransform` does it through `nameplates.inheritGearScore` (*read*). The
  split is not a vanilla transform, so it inherits nothing unless the split code passes it (Q22).

#### 3.R.3 The Phalanx

| | |
|---|---|
| **cfde822 did** | §3.4: a slow wither skeleton that never fires its bow, blinks away invisible when a player comes near, a non-entity dark swirl every 100 t that homes on the nearest player, and a shield that eats its first damage. |
| **Ben's design now** | **Everything stays** (K) **except the swirl.** Its projectile becomes **something like a Shulker shot**: visible, slow, homing, and **destroyed by ANY attack, area attacks included** (L). **Light purple with pink flashes**, not black. A real Shulker bullet is acceptable if it must be, not preferred. Element `void` (Ben's additional note). |
| **what it needs** | MB1 body; bow suppression; MB4 reduced to the **shield** and the **blink with invisibility**; a **destroyable homing projectile that seeks a combatant** (§4.2 N7). |

**The seat's ruling between a real `ShulkerBullet` and our own carrier waits on §7 F-S.** In brief: a real bullet is
destroyed by any damage that reaches it, but **our own weapons never reach it**, because our pipeline only damages
living combatants, so an ability-cast area attack would pass straight through. It also hits for vanilla's 4 and
applies Levitation, and moves on vanilla's block-axis path. **The case for our own carrier is a living body**, which
our pipeline does hit. Detail in §7 F-S.

#### 3.R.4 The Skreel, with its mount

| | |
|---|---|
| **cfde822 did** | §3.5: a wither skeleton on a big spider; a retreat, wind-up, homing leap and slam; the survivor becomes a brawler. |
| **Ben's design now** | **Unchanged in behaviour.** Element `void` (Ben's additional note). |
| **what it needs** | MB1 and MB5, as before. **Open for Ben:** whether the spider MOUNT is also a Void mob (it is a separate entity, and the note names "Skreel"); and the name "Withered Mount" on a Void mob (Q24, Q25). |

**2026-11-17, step R8: BUILT** (§5.R.11), from the seat's numbers Ben accepted (RULINGS) and cfde822's where they are silent; the item-by-item account is `GATE-dragon-skreel.md`, INHERITED OR CHOSEN. Q24 is answered (no element); Q25 (the name) is still open.

#### 3.R.5 NUMBERS FOR BEN — every unruled number in the rework, in one place

**Every value in the "old" column is a FACT about cfde822 or about vanilla, recorded and never adopted.** Where the
old mob had a different body (a wither skeleton where the Phage is now a creeper), the old number is not even the
same quantity, and says so. **Ben has given three numbers, and they are starting values, still his to change:** the
arc's 30° and 70° (A, *"Maybe"*), the 4 deaths (I, *"Start with 4"*) and the 6-block radius (I, *"let's try"*).
Each row names the QUANTITY it sets.

**Knell**

| # | quantity | old value (fact, not adopted) |
|---|---|---|
| K1 | body `SCALE` | cfde822 1.6 |
| K2 | movement-speed multiplier | cfde822 × 0.6 |
| K3 | custom HP (× GS/100, M4) | cfde822 480 absolute; ships 360 in `knell.yml` |
| K4 | equipment | cfde822: diamond armour, Prot I; diamond sword, Sharp IV, KB I, Fire Aspect I |
| K5 | melee damage (`attack_damage`; **M17 NOTED, NOT DECIDED**) | cfde822: vanilla × 5 |
| K6 | beam cycle, ticks between beams | cfde822 random 100–200 t |
| K7 | beam telegraph, ticks | cfde822 12 t |
| K8 | beam range, blocks | cfde822 40 |
| K9 | beam hit tolerance (the target's box grown by), blocks | cfde822 1.0 |
| K10 | beam damage (authored × GS/100 under M24, if Q-P12 so rules) | cfde822 35, `MAGIC`, ×5-exempt |
| K11 | **teleport delay after the hit, ticks** — starts equal to K7 (B) | none: the old dodge was instant |
| K12 | **arc's near bound, degrees** | Ben's starting value **30**; cfde822 had no arc (a 90° sidestep, 4–5 blocks) |
| K13 | **arc's far bound, degrees** | Ben's starting value **70** |
| K14 | **points tried per side** across the arc | none (cfde822 tried one point per side) |
| K15 | **level tolerance**: how far above or below the target's feet a landing may be, blocks | none (cfde822 checked only the block and the one above) |

**Phage** (stage-1 and stage-5 values; stages 2–4 interpolate per G3, awaiting Ben)

| # | quantity | old value (fact, not adopted) |
|---|---|---|
| P1 | stage count | Ben: 5 (I, second round) |
| P2 | **deaths per stage** (G1 reads 1, so 4 from stage 1 to 5) | Ben: *"Start with 4"* |
| P3 | **growth radius**, blocks | Ben: *"let's try 6"* |
| P4 | which element's deaths count | Ben: void (H) |
| P5 | split count | cfde822 2 (Ben: 2) |
| P6 | split spawn offset, blocks | cfde822 ±0.5 on x |
| P7 / P8 | body `SCALE`, stage 1 / stage 5 | Ben: stage 5 *"about 3 blocks tall"* (a creeper is 1.7 tall, §7 F-C); cfde822's Lesser Phage 0.6 was a wither skeleton |
| P9 / P10 | movement-speed multiplier, stage 1 / stage 5 | cfde822 Lesser × 1.10, Phage × 0.75 (wither-skeleton bodies) |
| P11 / P12 | custom HP, stage 1 / stage 5 (× GS/100, M4) | cfde822 Lesser 80, Phage 160 (wither-skeleton bodies) |
| P13 / P14 | **trigger distance**: how close a target must be to start the fuse, stage 1 / stage 5 | vanilla creeper 3 blocks, centre to centre (§7 F-C) |
| P15 / P16 | **cancel distance**: how far away the target must get to cancel it (F) | vanilla creeper 7 blocks, or no line of sight (§7 F-C) |
| P17 / P18 | **fuse, ticks**, stage 1 / stage 5 | vanilla creeper 30 t (`maxSwell`, §7 F-C) |
| P19 / P20 | **blast radius**, blocks | vanilla creeper 3; cfde822 Goon a 4-block half-extent box |
| P21 / P22 | **blast damage** (authored × GS/100 under M24, if Q-P12 so rules) | cfde822 Goon 50, ×5-exempt |

**Phalanx**

| # | quantity | old value (fact, not adopted) |
|---|---|---|
| X1 | custom HP | cfde822 240 |
| X2 | movement-speed multiplier | cfde822 × 0.65 |
| X3 | equipment | cfde822: Power V bow (never fired), shield in the off hand |
| X4 | blink trigger, blocks | cfde822 a 5-block half-extent box (D4) |
| X5 | blink landing distance, blocks | cfde822 6–16 |
| X6 | blink attempts | cfde822 8 |
| X7 | shot period, ticks | cfde822 100 t |
| X8 | acquire range, blocks | cfde822 30 (re-acquire 35) |
| X9 | **projectile speed**, blocks per tick | cfde822 0.2 (0.4 per 2 t); a vanilla Shulker bullet steps about 0.15 per tick (§7 F-S, *inferred*) |
| X10 | **homing strength** per tick | cfde822 10% blend per step |
| X11 | max flight, ticks / blocks | cfde822 400 t / 35 blocks |
| X12 | hit tolerance, blocks | cfde822 1.2 half-extent box |
| X13 | projectile damage (authored × GS/100 under M24, if Q-P12 so rules) | cfde822 40, ×5-exempt; a vanilla Shulker bullet 4 plus Levitation 200 t (§7 F-S) |
| X14 | **projectile hit points**: how much damage destroys it | Ben: *"Any attack destroys it"*, so any damage at all; listed so the quantity is named |
| X15 | what spends the one-hit shield | cfde822 the first damage of any kind (D5) |

**Skreel and its mount** — unchanged from §3.5, every row still Ben's: mount HP (cfde822 120), mount `SCALE` (1.5),
mount speed while paired (× 1.3), rider HP (120), rider gear (an iron spear), search range (50), stand-off band
(8–12, stand-off 12), wind-up (15 t), leap timeout (40 t), landing reach (2.5), landing damage (35), knockback
(0.7 horizontal, 0.4 vertical).

---

> **§3.1–§3.5 BELOW ARE THE cfde822 SURVEY, 2026-11-06, KEPT AS THE RECORD.** They describe five mobs, two of which
> no longer exist as designed (the Goldrot Goon is retired; the Phage is now a creeper with stages). Where they
> say "port ___ (Ben)", §3.R.5 is now the list.

**Shared by all five at cfde822** (*read*: `WitheredMobFactory`, `DragonfightManager`):
- **body:** a wither skeleton, except the Skreel's spider mount. `setRemoveWhenFarAway(false)`; Defense 0
  (`MobSpawnHelpers.applyMobDefense(…, 0.0)`, which per its javadoc makes worn armour cosmetic: vanilla armour and
  protection reductions are zeroed); follow range 64 (D9); every equipment drop chance set to 0;
- **identity:** a PDC `BOSS_NAME` string with a `§` colour code, and a PDC `DRAGONFIGHT_MINION` byte. The spawn method
  registers the entity in the per-world minion registry;
- **AI:** vanilla, unless the mob's own section says otherwise. Targeting is vanilla's, plus D3's Enderman reconcile;
- **every spawn is cap-checked** by `phaseCapFor(bossName, currentDragonPhaseInWorld)`, except the Lesser Phage. A
  capped spawn returns null. Outside a fight the phase is −1, which clamps to row 0 (legacy F10's NPE).

**The fight's spawn table** is in `PLAN-legacy-port.md` §3.0 and was re-read against
`DragonfightReinforcements.spawnPhaseWave` / `spawnReinforcement` / `scheduleNext` and `PHASE_TYPE_CAPS`. **Every
figure in it matches.** D1 adds which row a wave meets. Two details it omits (*read*):
- the trickle starts at `initDragonfightEncounter` (phase 0, 160 t), before any threshold;
- each trickle fire picks **one** anchor and jitters every spawn ±2 blocks around it. The anchors are 3 points on a
  radius-15 ring **around (0, 0)**, rotated randomly (`findDragonfightGroupAnchors`); a void column is skipped.

### 3.1 The Knell — SHIPS TODAY AS `content/mobs/knell.yml`, AS A NAME AND A HEALTH POOL

| | cfde822 (`spawnWitheredKnight`, `WitheredKnellBehavior`, *read*) | port |
|---|---|---|
| **what it did** | A big, slow, armoured wither skeleton with vanilla melee. Every 5–10 s it locked a beam on a player, rooted itself for the telegraph, and fired. It sidestepped every arrow. | — |
| **body** | wither skeleton; `SCALE` 1.6; speed × 0.6; HP 480; full diamond armour, Protection I on each piece; diamond sword with Sharpness IV, Knockback I, Fire Aspect I | scale ___ (Ben), speed ___ (Ben), HP ___ (Ben) (ships 360), gear ___ (Ben) |
| **AI** | vanilla wither-skeleton melee; the beam loop runs beside it | — |
| **attack: melee** | vanilla sword hit × 5 on a player. Its vanilla value with that sword is *UNVERIFIED* (not computed) | ___ (Ben); **M17 is NOTED, NOT DECIDED** |
| **attack: beam** | **trigger:** a random 100–200 t (5–10 s) delay, then the vanilla target if it is a player, else the nearest non-creative, non-spectator player within 40. No player in range: wait another cycle. **Telegraph:** locks origin (its eye) and endpoint (the target at 0.6 × its height) once; bell resonate; a purple dust line (RGB 95,35,165, size 0.8) flashed at tick 0 and tick 9; `stopPathfinding()` every tick for **12 t** (0.6 s). **Fire:** block-clipped, a cyan dust line (RGB 40,230,235, size 1.4), bell + amethyst chime. **Hits only the locked player**, and only if that player's box grown by 1.0 still meets the segment. **Damage:** 35, `DamageType.MAGIC`, ×5-exempt. **Cooldown:** the next random 100–200 t starts after it fires. **Range:** 40. | every number ___ (Ben) |
| **reaction: dodge** | **trigger:** any projectile damager, or any `ARROW`-typed by-entity hit (D6). Cancels the damage, deletes the projectile, and teleports 4–5 blocks (uniform random) perpendicular to the incoming direction, picking left or right at random and falling back to the other side if the landing is solid (`WitheredKnightUtil.isPassableLanding`: the block and the one above are non-solid). Soul and soul-fire particles and an amethyst step at both ends. **No cooldown.** Custom ranged weapons that dealt damage by direct `damage()` calls consulted it through `tryWitheredKnightDodge`. Melee, magic and summons are not dodged. | ___ (Ben), and whether at all |
| **dragon fight** | waves 0/1/3 (wave 0 at 80% has none); trickled from phase 1; cap 0/2/4/5. Drops and XP stripped; 15 coins (D10) | — |

**What ships here** (*read*, `knell.yml` and `RpgCommand.spawnMob`): `base_entity: wither_skeleton`,
`display_name: "Knell"`, `max_health: 360`. **It spawns with NO weapon and NO armour**: `spawnMob` passes
`randomizeData = (def == null)`, so a custom mob skips vanilla's spawn equipment (`PLAN-mob-scaling.md` §6 F16 records
the same). It has vanilla scale and speed, no beam and no dodge, and it hits with a bare wither skeleton's
`ATTACK_DAMAGE` × GS/100 and no ×5 (M4, §6 F15). The beam's relation to the Knell Beam weapon is
`PLAN-legacy-port.md` §2.5, re-read here: same colours, range, hit radius and telegraph length; the mob's beam hits one
locked target where the weapon pierces; the mob's `playSound` volume/pitch pairs are the weapon's swapped.

### 3.2 Phage, and the Lesser Phage

| | Phage (`spawnWitheredGrunt`, *read*) | Lesser Phage (`spawnLesserPhage`, *read*) | port |
|---|---|---|---|
| **what it did** | the fight's rank and file: an armoured wither skeleton with a stone sword | the split form: small, fast, unarmoured | — |
| **body** | HP 160; speed × 0.75; one armour tier rolled per spawn, uniform over gold / chainmail / iron, all four pieces of that tier; stone sword | HP 80; `SCALE` 0.6; speed × 1.10; all armour cleared; stone sword | HP, scale, speed, gear ___ (Ben) |
| **AI** | vanilla | vanilla | — |
| **attacks** | vanilla melee × 5 only | vanilla melee × 5 only | ___ (Ben) |
| **on death** | **split:** only when `resolveBounceAwareKiller` finds a player (the killer, or a PDC `LAST_BOUNCE_KILLER`). Spawns 2 Lesser Phages at the corpse, +0.5 and −0.5 on x. Despawns, `/kill` and non-player deaths do not split (`WitheredMinionDeathListener.onPhageDeathSplit`) | never splits | count, offset ___ (Ben) |
| **dragon fight** | waves 6/4/2; trickled from phase 0; cap 10/8/8/7; **the storm's only spawn** (3 per trickle fire during a storm's grace or active window) | **bypasses the cap** (the method has no cap check); tagged `DRAGONFIGHT_MINION` and registered, so it counts toward the global registry but has no cap column of its own | — |

Both carry the catalogue id `WITHERED_GRUNT` (*read*). Coins 5 and 2 (D10).

### 3.3 Goldrot Goon

| | cfde822 (`spawnWitheredRouge`, `onGoldrotGoonDeathExplode`, *read*) | port |
|---|---|---|
| **what it did** | a fast, gold-clad wither skeleton that explodes when a player kills it | — |
| **body** | HP 80; speed × 1.25; golden chestplate with Projectile Protection IV (cosmetic under Defense 0, *inferred* from `applyMobDefense`'s javadoc); golden sword with Sharpness IV | HP, speed, gear ___ (Ben) |
| **AI** | vanilla | — |
| **attack: melee** | vanilla melee × 5 | ___ (Ben) |
| **on death: blast** | **trigger:** a player kill as in §3.2 (`resolveBounceAwareKiller`). **Telegraph:** none; an `EXPLOSION_EMITTER` particle and a generic explode sound at the corpse. **Damage:** 50 to every living entity in a 4 × 4 × 4 half-extent box (`getNearbyLivingEntities(at, 4, 4, 4)`) except itself and any Ender Dragon, ×5-exempt, no block damage. **Who it hits:** D2. | radius, damage ___ (Ben); who it hits ___ (Ben) |
| **dragon fight** | waves 3/3/2; trickled from phase 0; cap 4/4/3/2 | — |

**Its relation to `GoldrotSet` is the name and the gold theme only** (`PLAN-legacy-port.md` §3.3; not re-read here,
*inferred* still true because neither factory method nor death listener refers to it, *read*). Coins 8.

### 3.4 Phalanx

| | cfde822 (`spawnWitheredArcher`, `WitheredPhalanxBehavior`, *read*) | port |
|---|---|---|
| **what it did** | a slow wither skeleton with a bow it never fires. It warps away and turns invisible when a player gets close, shoots a slow homing dark swirl every 5 s, and a shield absorbs its first damage | — |
| **body** | HP 240; speed × 0.65; bow with Power V in the main hand (*inferred* cosmetic, because every shot is cancelled; see AI); shield in the off hand; PDC `PHALANX_SHIELD` byte | HP, speed, gear ___ (Ben) |
| **AI** | vanilla skeleton AI still runs and draws the bow; **every `EntityShootBowEvent` from it is cancelled** (`onPhalanxBowSuppress`). Which goal vanilla picks with a bow is *UNVERIFIED*. One loop every tick runs the blink and the swirl | — |
| **behaviour: blink** | **trigger:** a non-creative, non-spectator player in the D4 box while it is **not** already invisible. Tries 8 random points 6–16 blocks out at the column's highest block + 1; the first passable one wins; none passes, no blink. **Effect:** squid-ink puffs at both ends, an enderman-teleport sound, `setInvisible(true)` and a PDC `PHALANX_INVISIBLE` byte. **Cooldown:** none, but it cannot blink again until it is visible | every number ___ (Ben) |
| **attack: swirl** | **trigger:** a tick counter reaches 100 t (5 s); target the nearest non-creative, non-spectator player within 30. No target: the counter resets and it tries again in 100 t. Firing **clears invisibility**. **Telegraph:** a wither-ambient sound at launch; no wind-up. **Flight:** not an entity. Starts at its eye aimed at the target's eye; moves 0.4 blocks every 2 t; each step blends 10% toward the target's eye and renormalises; re-acquires the nearest player within 35 if the target is lost; three squid-ink particles in a rotating triangle per step. **Ends** on a solid block, past 35 blocks from its start, after 200 steps (400 t), or when the Phalanx dies. **Hit:** the first living entity within a 1.2 half-extent box that is not itself, not the dragon and not a minion; **players and non-minion mobs alike.** **Damage:** 40 via `damage(40.0, ws)`, ×5-exempt | every number ___ (Ben) |
| **reaction: one-shot shield** | the first `EntityDamageEvent` of any cause while it holds `PHALANX_SHIELD` is cancelled; shield-break sound; off hand cleared; byte removed (D5) | ___ (Ben), and what spends it ___ (Ben) |
| **dragon fight** | waves 0/2/3; trickled from phase 1; cap 0/3/4/4. Coins 8 | — |

### 3.5 Skreel, with its Withered Mount

| | cfde822 (`spawnWitheredSpiderJockey`, `WitheredSpiderJockeyBehavior`, *read*) | port |
|---|---|---|
| **what it did** | a wither skeleton riding a big spider. The pair keeps 8–12 blocks off a player, winds up, leaps in a homing arc, and slams down. If either dies, the other becomes an ordinary brawler | — |
| **body: mount** | spider; `BOSS_NAME` "§8Withered Mount"; HP 120; `SCALE` 1.5; speed × 1.3 while paired; **no catalogue id, no equipment**; follow range 64 | every number ___ (Ben) |
| **body: rider** | wither skeleton; "§7Skreel"; catalogue `WITHERED_SPIDER_JOCKEY`; HP 120; iron spear in the main hand, no enchants (its comment: the leap's damage is scripted, the spear is for melee after a dismount); speed untouched. Mounted with `spider.addPassenger(ws)` | HP, gear ___ (Ben) |
| **AI** | **vanilla targeting suppressed:** every tick, `setTarget(null)` on both. Target is the nearest non-creative, non-spectator player within 50 blocks of the spider. No target: stop and reset to RETREAT. A target change resets to RETREAT. A four-state loop, every tick from 20 t after spawn | — |
| **RETREAT** | if the player is 8–12 blocks away, go to WINDUP; else pathfind (speed arg 1.6) to the point 12 blocks from the player along the player-to-spider axis (D7) | ___ (Ben) |
| **WINDUP (the telegraph)** | stop; on entry a spider-ambient sound and 15 crit particles. If the player leaves 6.4–18 blocks, back to RETREAT. After **15 t** (0.75 s) launch: horizontal speed = horizontal distance / 28 (a nominal 28 t flight), vertical 1.2, spider-hurt sound | ___ (Ben) |
| **LEAP** | every tick, re-aim the horizontal velocity at the player at that same speed (homing); keep the vertical. **Lands** when it touches ground after leaving it. **Aborts** to RETREAT if, once airborne, its horizontal speed falls below half the launch speed ("stuck"), or after **40 t** (2 s) | ___ (Ben) |
| **LAND: the hit** | if the player is within 2.5 blocks of the spider and not creative or spectator: **35** damage with **the rider as damager**, ×5-exempt; knockback 0.7 horizontal away from the spider, 0.4 vertical; a crit sound. Then RETREAT. **Cooldown:** none beyond the loop; the next leap needs a fresh RETREAT → WINDUP | every number ___ (Ben) |
| **pair break** | the mount dies: the rider leaves the vehicle and `setAI(true)` (a no-op, *inferred*: AI was never disabled). The rider dies: the spider stops pathing, `setAI(true)`, and its speed is divided by 1.3 (D8). The loop ends either way | ___ (Ben) |
| **dragon fight** | 2 pairs in wave 2; trickled from phase 2; cap 0/0/3/4, **counted by Skreels**, so it caps pairs. Both entities are tagged and registered. Coins: mount 5, Skreel the type default (D10) | — |

**One vanilla fact bites the spear:** what a spear does when a mob wields it is *UNVERIFIED* (`PLAN-legacy-port.md`
§6 F8).

---

## 4. WHAT OUR MOB SCHEMA CAN SAY TODAY

**The whole schema is four fields** (*read*): `MobDefinition(id, baseEntity, displayName, maxHealth)`, filled by
`MobLoader.parse` from `base_entity`, `display_name` and `max_health`; the id is the filename. **`MobDefinition` has 8
constructor call sites**: 5 in `MobRegistryTest`, 2 in `MobSeedingTest`, 1 in `MobLoader` (counted with
`grep -rn 'new MobDefinition(' --include=*.java .` piped to a per-file count, at `cd790dcc`). Its own javadoc says it
is *"Shaped to grow. {@code attackDamage} and the rest of the stat block become further components."*

**What a content file can and cannot express, read from the Knell:**
- **CAN:** which vanilla entity; its real CustomName (MiniMessage); its custom HP pool, which is uncapped (the
  `knell.yml` comment), and × GS/100 under M4 with GS from M25 (overworld 100, Nether 200, End 300 within 1,000 blocks
  of (0, 0)).
- **CAN, by omission:** vanilla AI and vanilla targeting, because nothing replaces them.
- **CANNOT:** equipment (it spawns bare, §3.1), scale, speed, attack damage, follow range, any timed behaviour, any
  reaction to damage, anything on death, a mount, a faction, a drop table.
- **ONE WAY IN:** `/rpg spawn <id>` is the only writer of `mob_id` (`RpgCommand.spawnMob`, *read*). There is no
  natural spawn and no spawner.

**Engine facts that bite** (*read* unless marked):
- **Homing skips players.** `ProjectileFlight.nearestMob` has `if (candidate.state().player()) continue;`. A
  mob-cast homing bolt reusing the projectile cast could not seek a player (legacy F14).
- **The cast pipeline takes a snapshot, not a player.** `AbilityService.cast(CombatantSnapshot caster, …)`. Every
  paper caller found (`P/build/StoneCaster`, `P/command/RpgCommand`) is player-driven (*inferred* from a grep for
  `.cast(` in `paper/src/main/java`; whether a mob snapshot runs end to end is not known).
- **Mob damage pricing is M24's:** a custom mob's hit is authored × GS/100, but no authored field exists, so every
  scripted mob hit is unpriced (legacy F13).

### 4.1 Each capability, tiered — THE cfde822 SURVEY'S TABLE, SUPERSEDED BY §4.2, KEPT AS THE RECORD

**(a)** content-only, expressible in YAML today. **(b)** an engine capability: a new field or handler in an existing
system. **(c)** a new system. The ids are `PLAN-legacy-port.md` §9.1's.

| capability | needed by | tier | note |
|---|---|---|---|
| name + HP on a vanilla body | all five, as six files | **(a)** | the Lesser Phage and the mount are their own files |
| equipment: main hand, off hand, four armour slots; enchants on them; drop chance 0 | Knell, Phage, Goon, Phalanx, Skreel | **(b) MB1** | the per-spawn **random armour tier** of the Phage is a small extra: a pick-one list |
| `SCALE`, `MOVEMENT_SPEED` multiplier, `FOLLOW_RANGE` | Knell, Lesser Phage, mount; speed on four | **(b) MB1** | `Attribute.SCALE`, `MOVEMENT_SPEED`, `FOLLOW_RANGE` exist in the pinned jar (`PLAN-legacy-port.md` §1, javap; runtime behaviour *UNVERIFIED*) |
| `attack_damage` | all five's vanilla melee | **(b) MB1** | **closes M17 only if Ben rules it** (NOTED, NOT DECIDED) |
| suppress the vanilla bow shot | Phalanx | **(b)** | one listener keyed on `mob_id` |
| on death: spawn N of mob id X | Phage | **(b) MB2** | must name its kill condition (player kill only, at cfde822) |
| on death: an area hit | Goon | **(b) MB2** | its price is legacy Q-P12; who it hits is D2 |
| a timed attack at a player: lock, telegraph, root, fire | Knell beam | **(b)/(c) MB3 + E5** | MB3 is the runner (a periodic cast by a mob); E5 is the aim-locked wind-up, shared with the Knell Beam weapon |
| a homing bolt that seeks players | Phalanx swirl | **(b)** inside MB3 | needs a player-seeking mode beside `nearestMob`'s skip (legacy F14) |
| a pre-damage reaction: cancel + sidestep | Knell dodge | **(c) MB4** | it must see custom weapons' hits too, which today resolve through our own pipeline, not vanilla projectiles (*inferred*) |
| a pre-damage reaction: one-shot shield | Phalanx | **(c) MB4** | the same hook as the dodge |
| proximity blink + invisibility, revealed on attack | Phalanx | **(c) MB4** | `LivingEntity.setInvisible` exists in the jar (legacy §1) |
| a mounted pair, spawned together, with survivor conversion | Skreel + mount | **(c) MB5** | `Entity.addPassenger` exists in the jar (legacy §1) |
| a movement state machine (retreat, wind-up, leap, land) | Skreel | **(c) MB5** | cfde822 used pathfinder `moveTo` plus `setVelocity` every tick |
| faction: no friendly fire among fight mobs; war with Endermen | all five | **(c), the fight's** | it is fight state, not a mob field (§6 C4) |
| drops/XP stripped, coin payout | all five | **(c), the fight's** | there is no coin or drop table for mobs here today (*inferred*: no `drops` key in `MobLoader`) |

### 4.2 THE REWORK'S CAPABILITY TABLE, 2026-11-15 — FOUR MOBS. THIS GOVERNS

Tiers as §4.1: **(a)** content only; **(b)** a new field or handler in an existing system; **(c)** a new system. The
MB/E ids are `PLAN-legacy-port.md` §9.1's; **N1–N8 are new here.**

**What falls away:**

| capability | was needed by | why it goes |
|---|---|---|
| ~~MB4 pre-damage reaction: cancel + sidestep~~ | the Knell's dodge | ruling 4: the dodge is removed. **The teleport now follows the beam's hit**, so no hook before damage is needed |
| ~~MB2 on-death area hit~~ | the Goldrot Goon's blast | ruling 1: the Goon is retired. Its idea lives on as the Phage's self-destruct (N4), which fires **alive and near a target**, not on death, and is re-priced |
| ~~the wither-skeleton Lesser Phage as its own file~~ | the Phage's split | rulings 1 and 5: a stage is state on one mob, never a separate file |
| ~~the Phage's random armour tier; the Goon's gold gear~~ | Phage, Goon | creeper bodies wear nothing |
| ~~the non-entity swirl~~ | Phalanx | replaced by N7, a projectile that can be hit |

**What stays, and what is new:**

| id | capability | needed by | tier | note |
|---|---|---|---|---|
| — | name + HP on a vanilla body | all four (five files with the mount) | **(a)** | ships today for the Knell |
| **MB1** | body fields: `SCALE`, speed multiplier, follow range, equipment, `attack_damage` if M17 is ruled | all four | **(b)** | **R1 BUILT scale, speed and health only (§5.R.1); equipment, follow range and `attack_damage` are DEFERRED.** **the Phage needs them applied to a LIVE mob**, not only at spawn: a stage change rescales it, and `SCALE` refreshes the hitbox live (§7 F-C) |
| **N1** | **element on a mob**: `element:` in the mob file | all four | **(b)** | one optional component and one validator check (§3.R.0) |
| **N2** | **element on a vanilla type**: `content/mob_elements.yml` | Endermen (Phage growth) | **(b)** | a new small content file and loader; a mob file overrides it (§3.R.0) |
| **N3** | **"hostile to mobs" as one named predicate** over combatants | every N below | **(b)** | today it is `player()` (§7 F-E); a Summoner adds to it in one place |
| **MB3 + E5** | a mob's timed beam: lock, telegraph, root, fire | Knell | **(b)/(c)** | as before, **plus it reports which combatant it hit**, which N8 needs |
| **N8** | **post-hit arc teleport with a landing check** | Knell | **(b)**, on MB3 | a scheduled move a delay after the hit; VALID per ruling 4 via §7 F-T's collision API |
| **N4** | **proximity self-destruct, ours**: a content trigger distance, fuse, back-away cancel; damage priced by mob-scaling to players and summons only; no blocks; the mob removed with a recorded reason | Phage | **(c)** | replaces vanilla's `SwellGoal` and cancels vanilla's explosion (§7 F-C); `MobGoals` exists in the jar (`PLAN-legacy-port.md` §1) |
| **N5** | **growth stages fed by nearby deaths**: a stage on the mob (PDC), advanced by element-`void` deaths within a radius | Phage | **(c)** | listens at the one death point (§7 F-E) **and** at N4's own detonation, because a vanilla-style explosion is not a death |
| **N6** | **split on kill**: at stage 5, a KILL (never a self-destruct) spawns N of the same mob at stage 1 | Phage | **(b)** | MB2's "spawn N of an id", narrowed; it needs N4's recorded reason to tell killed from exploded |
| **N7** | **a destroyable homing projectile that seeks a combatant** | Phalanx | **(c)** | the seat rules real bullet vs our carrier after §7 F-S; either way the homing must seek N3's set, the reverse of `ProjectileFlight.nearestMob`'s player skip (legacy F14) |
| **MB4** (reduced) | the one-shot shield; the proximity blink with invisibility | Phalanx | **(c)** | the dodge half is gone, so MB4 is the Phalanx's alone |
| — | bow suppression | Phalanx | **(b)** | unchanged |
| **MB5** | the mounted pair and its movement state machine | Skreel + mount | **(c)** | unchanged |
| — | faction, drops, coins | the fight | **(c), the fight's** | unchanged; §6 |

---

## 5. BUILD ORDER — SMALLEST FIRST. BEN RULES THE ORDER

### 5.R THE REWORK'S ORDER, 2026-11-15. THIS GOVERNS

Each step names what it unlocks and its gate rows **in outline** (each gate is written when its numbers are ruled).
**Every row declares its game mode** (`verification.md`, *THE CREATIVE-DIVERGENCE REGISTER*), and **vanilla's swell
skips creative and spectator targets** (§7 F-C), so a creative bench reads no Phage row. **"AUTO-able"** marks a row
whose prediction is server-side only, so `/rpg selftest` could drive it under an amendment committed before the boot
(`verification.md`, *AN AUTO READING IS A READING OF THE SERVER*); everything a player sees or feels stays Ben's.

1. **R1 — MB1 body fields + N1 element on a mob + N2 the vanilla table.** Core test first, then `MobLoader`
   (`MobDefinition` has 8 constructor call sites, §4). **Unlocks:** all four bodies inert, and every mob's identity
   for growth. *Rows:* `/rpg spawn` each mob in the End and the overworld (M25's 300 against 100); `SCALE`, speed and
   HP read back; an Enderman and a Knell resolve to `void`, a zombie to none. **AUTO-able:** spawn, attribute and
   element reads. **Ben's:** how each body looks.
2. **R2 — the Phage's stages + N6 the split on kill.** A stage on the live mob, its body set from P7–P12. **Unlocks:**
   a Phage that can be killed into two stage 1s. *Rows:* spawn at stage 5, kill it → two stage 1s; spawn at stage 3,
   kill it → nothing; a death by `/kill`, a fall or another mob → as Ben rules Q26. **AUTO-able:** the kill (`Player#attack`) and
   the count of children. **Ben's:** that the split reads as one creature becoming two.
3. **R3 — N3 + N4, the Phage's self-destruct.** **Unlocks:** the Phage as a threat. *Rows:* approach to inside P13
   → fuse; back out past P15 → cancel (F); stand → blast; the damage on a player is P21 × GS/100; **no block broken;
   no mob hurt**; a stage 5 that explodes does **not** split (E). **AUTO-able:** the damage line, the absence of
   children, a mob's HP unchanged. **Ben's:** the back-away feel, the swell's look and sound.
4. **R4 — N5, growth.** **Unlocks:** the snowball (answer D). *Rows:* four Void deaths within P3 take a stage 1 to 5 (G1);
   one death advances two Phages at once (G2); a death at P3 + 1 does not count; an Enderman's death counts (H); a
   zombie's does not; a self-destruct counts (second-round I). **AUTO-able:** all of those, by seeding and killing.
   **Ben's:** what growing looks like.
5. **R5 — MB3 + E5, the Knell's beam**, reporting its hit. **Unlocks:** the Knell's attack, and E5 for the Knell Beam
   weapon (`PLAN-legacy-port.md` §9.2 step 5). *Rows:* the telegraph roots it; a hit lands K10 × GS/100; a miss lands
   nothing. **AUTO-able:** the damage line. **Ben's:** the telegraph and the beam as seen.
6. **R6 — N8, the post-hit teleport.** **Unlocks:** the whole Knell. *Rows:* after a hit, it moves K11 ticks later
   to a point K12–K13 degrees around at its distance; a miss moves nothing; an arrow moves nothing; boxed in (every
   arc point walled) it stays put; never inside a block, under the floor or in the air. **AUTO-able:** the position
   before and after, its angle and distance from the target, and the block checks. **Ben's:** whether it reads fair
   to a Ranger.
7. **R7 — the Phalanx: N7 the projectile, MB4 (reduced), bow suppression.** **Unlocks:** the whole Phalanx. *Rows:*
   the shot homes on its target; one melee hit, one arrow, one ability burst and one Boltor ray each destroy it (L);
   it is light purple with pink flashes; the shield eats the first damage; the blink. **AUTO-able:** destruction by
   each attack type, the damage line. **Ben's:** the colours and whether it is now easy to see.
8. **R8 — MB5, the Skreel and its mount.** Unchanged, last, for the same reasons as before.

**Why this order:** R1 is every mob's prerequisite. The Phage comes next and whole (R2–R4), because it is the fight's
most numerous mob and the most new mechanism, and because each of its three steps is playable alone. The Knell's two
steps follow, the beam before the move that depends on its hit. The Phalanx needs N3 (from R3) for its projectile's
target. The Skreel is still last: the only pair, the only state machine, the only spear.

### 5.R.1 STEP R1 AS BUILT (`feat/dragon-bodies`, 2026-11-15) — AND THE AMENDMENT TO R1 AND R2

**THE SEAT AMENDED 5.R (ruling 9): the Phage's per-stage BODY moves from R2 into R1**, because the Phage's body cannot
be authored without it. R1 sets the stage at spawn; **nothing changes it in R1** (no growth, no split). R2 keeps the
split on kill. The gate is `docs/gates/GATE-dragon-bodies.md`.

**What R1 built** (MB1 limited to scale, speed and health; N1; N2; the stage bodies):

| piece | as built | where |
|---|---|---|
| **scale** | the value of the `SCALE` attribute, written as its BASE | `MobBody`, `MobBodyApplier` |
| **speed** | a MULTIPLIER of the base type's DEFAULT movement speed (`AttributeInstance#getDefaultValue`), written as the BASE, so re-applying it does not stack and our slows (modifiers) still sit on top | `MobBodies.applySpeed` |
| **health** | unchanged meaning: `max_health` is the AUTHORED health, × GS/100, never × 5 (M4, GS from M25). A staged mob's is its stage's | `MobSeeding.maxHealth(…, stage, …)` |
| **element** | optional `element:`, an id in `content/elements/`; an unknown id makes the file a named, skipped file at load (`Skipping malformed mob '<file>': element '<id>' names no element …`). IDENTITY ONLY | `MobLoader`, `MobDefinition` |
| **the vanilla table** | `content/mob_elements.yml`, one row `enderman: void`; a row naming no entity type or no element is refused with `Refusing row …`, and the rest load | `MobElementLoader` |
| **the resolver** | one class in core: the mob's file (if it names an element), else the table by the bare type key, else none | `core.mob.MobElementResolver` |
| **the stages** | `stages: { count, first: {height, speed, max_health}, last: {…} }`, linear between; a staged file authoring top-level `max_health`, `scale` or `speed` is refused | `MobStages`, `StageBody` |
| **the stage on the mob** | the PDC key `mob_stage` (an INTEGER), written in the spawn's pre-spawn consumer beside `mob_id`, so the add event's seed reads it; PDC, so it survives a chunk unload and a restart. A bad stored value seeds as stage 1 and is named at WARN | `Keys.mobStage`, `MobBodyApplier.stageOf` |
| **the dev spawn** | `/rpg spawn <mob> stage <n> [gs]`: a LITERAL `stage`, because `[gs]` is already a bare number. No stage given is stage 1; a stage on an unstaged mob, or outside the mob's count, is refused by name and spawns nothing | `RpgCommand.spawnMob` |
| **no explosion** | `ExplosionPrimeEvent`, `CreeperIgniteEvent` and `CreeperPowerEvent` are cancelled for a creeper carrying `mob_id`; each logs `CREEPERGUARD …` | `CustomCreeperGuard`, `RpgListeners` |

**THE QUANTITY THE PHAGE AUTHORS IS HEIGHT IN BLOCKS, NOT SCALE, AND WHY.** It is the unit Ben ruled in (*"from 1
block to 3 blocks in height"*), so the file carries his numbers verbatim (1.0 and 3.0) instead of 1/1.7 and 3/1.7.
And it is cleanly NAMED: `Entity#getHeight()`, the hitbox height, which the gate reads back directly. The plugin turns
it into scale from the creeper's own unscaled height, read off the entity (1.7 from `EntityType`, §7 F-C), so the
file never states 1.7. The other mobs author `scale`, because their ruling was a ratio (*"1.6x"*). **Linear in height
is linear in scale** (scale = height / 1.7), so G3's "evenly between" holds in both.

**THREE CHOICES R1 MADE THAT THE SEAT SHOULD SEE:**
- **A mob file that authors NO element falls through to the vanilla table** rather than meaning "none". No shipped
  mob reaches the difference (the mount is a spider, and `spider` is not in the table); `MobElementResolverTest` pins
  the choice as a row. The other reading would make "no `element:`" a third way of saying none.
- **The mount's id is `withered_mount` and its name "Withered Mount"**, the old game's, both Ben's to change (Q25).
  Rider and mount are separate mobs; nothing pairs them in R1.
- **The guard keys on the `mob_id` TAG, not on the registry, and covers any creeper of ours**, not only the Phage: a
  creeper we spawned stays ours if its file is removed. Today the Phage is the only one.

**DEFERRED FROM MB1, BY THE BRIEF:** equipment (K4, X3, the Skreel's spear) and `attack_damage` (M17). So in R1 **the
Knell is unarmed and the Phalanx has no bow: both melee like a bare wither skeleton** (the M4/M24 custom-mob melee
of today, PLAN-mob-scaling.md §6 F15), and the Skreel has no spear. **Follow range** (cfde822's 64) is also not in R1.

### 5.R.2 STEP R2 AS BUILT (`feat/dragon-split`, stacked on R1, 2026-11-15)

The gate is `docs/gates/GATE-dragon-split.md`, which also carries the ONE R0 and the ONE run sheet of the stacked boot.

| piece | as built | where |
|---|---|---|
| **the rule** | `split: { at: last, count, child_stage, spread }` on the mob file; a death at the LAST stage gives `count` children at `child_stage`; any other stage, or a mob with no block, gives none. `at` accepts only `last` (Ben: "Only the 5th stage Phage splits") | `core.mob.MobSplit`, `MobLoader` |
| **the death point** | a separate `EntityDeathEvent` handler at **MONITOR, ignoreCancelled**, so only a death that happens splits; "any deaths" is every cause that raises the event (a player, a mob, a fall, fire, the void, `/kill`); `remove()`, a despawn, a chunk unload and a vanilla creeper's own explosion raise none | `RpgListeners.onMobDeathSplit` |
| **once only** | the parent is marked `mob_split` (PDC) BEFORE its first child spawns; a marked mob never splits again | `MobSplitter`, `Keys.mobSplit` |
| **the children** | made ours on the SAME path as `/rpg spawn` (`MobSpawner.stampCustom` inside `World#spawn`'s pre-spawn consumer, now shared): `mob_id`, name, stage-1 body and stage tag; seeded at full health; the PARENT'S gear score through M21's own `MobNameplateManager.inheritGearScore` | `MobSpawner`, `MobSplitter` |
| **where they stand** | the parent's position ± `spread` (0.5, content, P6) on x; a spot whose child body would collide (`RegionAccessor#hasCollisionsIn`) is replaced by the parent's own spot, which a smaller child always fits | `MobSplitter` |
| **the instrument** | a split child of a mob the instrument tagged takes all the parent's tags, so the row-start clean (which uses `remove()`, never a death) and the restore remove it | `SelfTestSnapshot.tagsForSplitChild`, `SelfTestService.adoptSplitChild` |

**Not testable until R3:** an exploding Phage does not split (E). No explosion of ours exists and R1 cancels vanilla's;
R3's detonation must remove the Phage without raising a death.

### 5.R.3 GROWTH (THE PLAN'S STEP R4) AS BUILT (`feat/dragon-growth`, stacked on R2, BUILT AHEAD OF R3, 2026-11-15)

**The order is not yet Ben's** (RULINGS, *THE BUILD ORDER CHANGED*; Q28). The gate is `docs/gates/GATE-dragon-growth.md`,
which now carries the ONE R0 and the ONE run sheet of the stacked boot.

| piece | as built | where |
|---|---|---|
| **the rule** | `growth: { element, radius, stages_per_death }` on the mob file. A death grows a candidate when the candidate is a living staged mob with the block, below its last stage, in the same world, with its FEET within `radius` blocks (a sphere, inclusive) of the dead creature's feet, and the dead creature's element is `element`. `+stages_per_death`, capped at the last stage. EVERY such candidate grows (G2); the dying creature never grows itself | `core.mob.MobGrowth` |
| **what counts** | a death that raises `EntityDeathEvent` (R2's definition), of a creature whose element, by `MobElementResolver`, is the growth element: our Void mobs by their file, a vanilla Enderman by the table. A zombie, the mount and a player count for nothing | `MobGrower.onDeath` |
| **once only** | the dead creature is marked `mob_growth_fed` (PDC) BEFORE any stage changes; a marked death grows nothing | `Keys.mobGrowthFed` |
| **the search** | a CUBE (`World#getNearbyEntities`) of the largest growth radius any loaded mob authors; the rule decides the SPHERE | `MobGrower.reportDeath` |
| **the parent and its children** | growth and the split run in ONE `EntityDeathEvent` handler at MONITOR, ignoreCancelled, GROWTH FIRST: its candidates are collected before the split spawns the children, so a stage 5's death grows the other Phages and never its own two children. Pinned by `DragonGrowthWiringTest` (and the mutation that swaps the two calls) | `RpgListeners.onMobDeath` |
| **the stage change** | applied on the growing mob's own thread (`Scheduler.onEntity`), the tick after the death, re-reading its stage there so two deaths in one tick add up. The stage tag and the body by `MobBodyApplier.apply` (R1's re-appliable rules), then health by `CombatantStats.rescaleMaxKeepingShare`: the new stage's maximum at the mob's STORED gear score, current at the same share (Ben: "Same share"), as ONE max modifier (`mob_stage`) measured from the seeded base, emitting one `MAX_CHANGE` so the nameplate redraws. Tags, target and gear score are untouched; `mob_split` is never written, so a Phage grown to 5 splits when it dies | `MobGrower.applyGrowth` |
| **the log** | `GROWTH mob=… id=… from=… to=… fed=<type>[/<mob id>] <uuid> element=… d=… max=…`, one per stage change | `MobGrower` |
| **the seam for R3** | `MobGrower.reportDeath(world, at, element, deadId, …)`: "a creature of this element died here", with no death event. The event path calls it; R3's detonation will call it for a self-destruct (Ben, I). Nothing else calls it yet | `MobGrower` |

**FINDING, NOT FIXED IN THIS STEP: A PHAGE THAT GROWS WHERE IT DOES NOT FIT.** Read from the server jar (build 74;
`javap -c -p -cp run/versions/26.1.2/paper-26.1.2.jar net.minecraft.world.entity.Entity`): a change to `SCALE` calls
`refreshDimensions`, and when the new body is larger (and the old and new are each at most 4 blocks, which the Phage's
3 always is), it calls `fudgePositionAfterSizeChange`, which asks `Level.findFreePosition` for room for the new box
near the old one and **moves the mob there** (`setPos`) when it finds one. **When it finds none** (a 1-wide gap, a
2-high tunnel with a stage 3 → 4), the bytecode continues past that branch; what it then does was **not read**. The
**INFERRED** outcome is that the mob stays where it is, overlapping blocks, and takes vanilla suffocation damage
(`IN_WALL`), which our `VanillaDamagePolicy` prices as environmental damage, proportional (M13). **A fit check before
growing is NOT built**; whether a Phage should refuse to grow, grow anyway, or grow and be pushed is Ben's (Q29).

**AN EQUIVALENT MUTANT, REMOVED RATHER THAN KEPT:** the rule first carried `if (stage >= last) return stage` before
the cap. Its mutation (`>=` to `>`) survived the whole suite, because `min(last, last + k)` is already `last`. The line
was deleted; the cap is the one rule, and its mutation reddens three rows.

### 5.R.4 STEP R3 AS BUILT (`feat/dragon-blast`, off master `94915476`, 2026-11-15) — THE BLAST

The gate is `docs/gates/GATE-dragon-blast.md`, its own scenario file `selftest/GATE-dragon-blast.yml`. **N4 is built
WITHOUT replacing the swell** (the seat's ruling 20): vanilla's `SwellGoal` is the trigger, the cancel and the fuse.

| piece | as built | where |
|---|---|---|
| **the rule** | `blast: { reach, damage: { first, last }, fuse_ticks }` on the mob file; damage linear by stage, x GS/100 as a custom mob's hit (`MobScaling.attackDamage`), at ZERO distance; victims inside the reach (6), feet to feet, inclusive, each taking it x `MobBlast.falloff(d, reach)` = `(x*x + x) / 2`, `x = 1 - d / reach` (vanilla's shape, §7 F-B; Ben, "keep vanillas shap"). A blast needs stages. *(Until 2026-11-16: `radius: 3`, full damage, the seat's P-c.)* | `core.mob.MobBlast`, `MobLoader` |
| **N3, the predicate** | `MobHostility.hostileTo(side, exemptMode)`: true for a survival or adventure player only. A Summoner adds a `Side` here | `core.mob.MobHostility` |
| **the fuse** | `fuse_ticks` written with `Creeper#setMaxFuseTicks` in the one spawn path, so split children get it; saved by the server as `Fuse` | `MobSpawner.stampCustom` |
| **the detonation** | at our creeper's `ExplosionPrimeEvent`: cancelled FIRST (never a vanilla explosion), a frozen Phage stops there, else `MobBlaster.detonate`: victims hit through `BukkitCombatant.of(victim).handle().applyDamage(damage, phageId)`, the two-argument form a mob's melee hit takes; the particle `EXPLOSION_EMITTER` and the sound `ENTITY_GENERIC_EXPLODE`; one `BLAST` line; growth's death path; `remove()` | `RpgListeners.onCustomCreeperPrime`, `MobBlaster` |
| **no split** | `remove()` is `discard(PLUGIN)` and raises no death event, so the one death handler never sees a blown Phage | `DragonBlastWiringTest` |
| **one Void death** | `MobGrower.onDeath(phage, …)` before the removal: marks `mob_growth_fed`, resolves void, reports with the Phage's own id, which never grows itself | `MobBlaster` |
| **the log** | `BLAST mob=… id=… stage=… gs=… base=<at zero distance> reach=… victims=<n> [<type> <uuid> d=… factor=… dealt=<before defence>] spared=<n> [<type> <uuid> d=… factor=…]`; a victim exactly at the reach is listed with factor 0 and not hit. The per-prime `CREEPERGUARD prime cancelled` line is gone; the ignite and power lines stay | `MobBlaster`, `RpgListeners` |
| **the instrument** | `HURT before= after= defense= dealer= dealerType=`, one per DAMAGE change to the run's player, from the store's own change (`HealthChange.beforeDefense`, new); the steps `detonate`, `ignite`, `probe_fuse`, `await_fuse` | `SelfTestDamageTap`, `SelfTestBlast`, `SelfTestRun`, `SelfTestDrivers` |

**WHAT THE DAMAGE PATH GIVES THE BLAST TODAY, READ FROM THE CODE.** The two-argument `applyDamage` is
`CritState.NORMAL`, `DefenseRule.APPLIES`, element **null**: no element (so no Void typing, Q31), the victim's defence
applies, no crit, and **no vanilla damage event**, so no `DamageCause`, no knockback (ruling 30, Q35), no hurt cadence of vanilla's,
and **no shield block** (`ShieldBlock.resolve` reads a vanilla event; Q32). The amount is never × 5 and never moved by
difficulty (M24 undoes difficulty on vanilla events, and this is not one).

**A CREATIVE OR SPECTATOR PLAYER inside a blast another player triggered is not hurt**: the predicate excludes him and
the `BLAST` line names him under `spared`. Unit-only (`MobBlastTest`): the selftest refuses to run with two players.

### 5.R.5 STEP R5 AS BUILT (`feat/dragon-beam`, off master `8671ab16`, 2026-11-17) — THE KNELL'S BEAM

The gate is `docs/gates/GATE-dragon-beam.md`, its own scenario file `selftest/GATE-dragon-beam.yml`. Every behaviour
and its mark is §3.R.1.a (I1-I24).

| piece | as built | where |
|---|---|---|
| **the rule** | `beam: { cycle_ticks: { min, max }, telegraph_ticks, flash_at, range, aim_height, tolerance, damage }` on the mob file; each number named in `knell.yml` and `MobBeam`'s javadoc. The Knell's: 100, 200, 12, [0, 9], 40, 0.6, 1.0, 35 | `core.mob.MobBeam`, `MobLoader` |
| **the hit decision** | pure, in core: the locked line `BeamLine.aimed(eye, aim, range)`, cut at the first block (`clip`); the target is HIT iff it is a live candidate in the Knell's world, `MobHostility.hostileTo` names it, and `line.meets(box, tolerance)`: the segment meets its box grown by the tolerance on every side (a slab test, inclusive). Every other creature the grown box meets is listed `spared` and never hurt. The logged `gap` is the smallest growth that meets, so **hit ⇔ gap ≤ tolerance**; it is the largest-AXIS distance at the closest point, not a perpendicular (Euclidean) one | `MobBeam.resolve`, `BeamLine` |
| **the clock** | wait (drawn, injected `BeamCycle.Draw`), attempt, telegraph, fire; a fresh wait at the fire and after an empty attempt | `core.mob.BeamCycle` |
| **the loop** | one period-1 `RepeatingTask` per beam mob on its own entity scheduler, started at `EntityAddToWorldEvent` (and the startup sweep), stopped by its `isActive` check the tick the mob is dead, removed or unloaded, and cancelled at `EntityRemoveFromWorldEvent`; its `onStop` releases the root and forgets it | `paper.health.MobBeamer`, `RpgListeners`, `RpgPlugin` |
| **the damage** | `MobBeam.price` (= `MobScaling.attackDamage(damage, true, hostile, gs)`) through `applyDamage(amount, knellId)`: defence applies; no element, crit, knockback or shield block | `MobBeamer.fire` |
| **E5, the report** | `MobBeam.report(shot, …)`: a `BeamHit(mob, mobId, target, tick, from, to)` on a hit, empty on a miss; `MobBeamer.addListener` is told of each. Nothing listens in R5; R6's teleport will | `core.mob.BeamHit`, `MobBeamer.Listener` |
| **the log** | `BEAM telegraph …`, `BEAM fire …` (each tested creature with its gap and outcome, and the damage dealt), `BEAM report …`, `BEAM cancelled …`, `BEAM none …` (a forced attempt that found no one) | `MobBeamLines` |
| **the instrument** | `beam: <tag>` (force an attempt now, on the mob's thread; it only marks the attempt due, so the fire's timing stays the loop's) and `probe_at: <tag>` (feet to three places), both reaching 48 blocks | `SelfTestBeam`, `SelfTestDrivers` |

**WHAT E5 IS FOR IN `PLAN-legacy-port.md`, AND WHAT WAS BUILT IN COMMON.** There, E5 is the *aim-locked wind-up with a
telegraph visual*, for the Knell Beam WEAPON (§9.2 step 5), which PIERCES and spends mana, and built "castable by a
mob". The mob's beam and the weapon share: the aim locked at the start (an origin and a direction), a telegraph of
N ticks, then a segment from the origin, clipped by blocks, tested against bodies grown by a tolerance. **That shared
part is `BeamLine` (and `BeamBox`), caster-agnostic and in core.** Not shared, so not built for the weapon: the cycle
(MB3, a mob's), the single-target rule (the weapon pierces: it would take every `meets` instead of the locked one), the
report (the teleport's, though a weapon could raise it), mana, and the weapon's ray shape. The brief's "E5" for the
report is this plan's §4.2 row *MB3 + E5 … plus it reports which combatant it hit*; the legacy plan's E5 is the
wind-up alone.

### 5.R.6 STEP R6 AS BUILT (`feat/dragon-teleport`, stacked on `feat/dragon-beam` `1bfee281`, 2026-11-17) — THE TELEPORT

The gate is `docs/gates/GATE-dragon-teleport.md`, the stack top, carrying the ONE R0 and run sheet of the stacked boot.

| piece | as built | where |
|---|---|---|
| **the rule** | `teleport: { delay_ticks, arc_degrees: { min, max }, spots_per_side, level_tolerance, require_sight }` on the mob file. The Knell's: 12 (K11, B), 30 and 70 (K12, K13, A), and the seat's provisional 5 (T-a, K14), 3 (T-b, K15), true (T-c, Q18). A teleport needs a beam, and a delay not shorter than `beam.cycle_ticks.min` is refused at load, the mob and both numbers named | `core.mob.MobTeleport`, `MobDefinition`, `MobLoader` |
| **the trigger** | the beam's report and nothing else: `MobTeleporter` is a `MobBeamer.Listener`, told inside `MobBeam.report(...).ifPresent`. No arrow dodge exists in our code | `MobTeleporter`, `MobBeamer` |
| **the delay** | a one-shot `RepeatingTask` of period `delay_ticks` on the Knell's own entity scheduler, started at the FIRE; dead, removed or unloaded first, it logs `TELEPORT skipped … reason=knell-gone`; the target re-checked at its end (`BeamTeleport.skip`: gone, another world, no longer one the Knell is hostile to) | `MobTeleporter`, `core.mob.BeamTeleport` |
| **the landing** | pure, in core, from the positions at the END of the delay: centre the target's FEET; radius the HORIZONTAL distance target→Knell; angle zero the Knell's bearing from the target; a column turned by an angle in DEGREES (RIGHT, a right turn for someone at the target facing the Knell: `(x cos a − z sin a, x sin a + z cos a)`; LEFT the angle negated). **Order:** the drawn side's angles (the drawn first angle, then `spots_per_side − 1` spread evenly from min to max, both included), then the other side's, the same angles. **Per column:** loaded, else UNLOADED; the nearest standing height `feet.y + k`, k = 0, +1, −1, +2, −2, … out to `level_tolerance`, that FITS and is GROUNDED, else NO_FOOTING with the counts; then SEES, else NO_SIGHT. First OK wins; none, it stays | `BeamTeleport.decide` |
| **VALID** | `World#isChunkLoaded`; `Entity#wouldCollideUsing(box)` for the Knell's own scaled box; `RegionAccessor#hasCollisionsIn` on a thin slab under its footprint; `World#rayTraceBlocks` eye → the target's aim point, the beam's block rule. Each READ in the server jar (the gate's T4, T5) | `MobTeleporter.footing` |
| **the move** | `Entity#teleport` facing the target, level; READ: the jar's same-world teleport zeroes the velocity and touches neither the navigation nor the target, so the path is stopped after the move. The target, the beam's clock, health and tags are untouched | `MobTeleporter` |
| **sight and sound** | cfde822's sidestep's, READ (§3.1): soul and soul-fire particles and an amethyst step, at the departure and the arrival. Counts (12 each), spread, volume and pitch (1.0, 1.0): NOT IN THE RECORD, chosen | `MobTeleporter.puff` |
| **the log** | `TELEPORT mob=… id=… target=… tick=… from=… center=… radius=… bearing=… side=… first=… tried=<n> [<side> <angle> <verdict>, …] landed=…/none moved=…`; `TELEPORT skipped … reason=…`; `TELEPORT ignored … reason=pending` | `MobTeleportLines` |
| **the instrument** | `teleport_draw: "<tag> <right/left> <degrees>"` fixes that Knell's NEXT decision | `SelfTestBeam`, `SelfTestDrivers` |

**FOR BEN, CHOSEN AND NOT IN ANY RULING:** the order within a side (the random angle, then the arc spread evenly); RIGHT
and LEFT each 50%; heights tried nearest first with UP before DOWN at equal distance; a footing refused by sight is not
retried at another height; a second hit while a move is pending is ignored (only a forced beam can do it); the puff's
counts and the sound's volume.

### 5.R.7 THE KNELL FOLLOW-UP AS BUILT (`feat/dragon-knell-2`, off master `7a3e9985`, 2026-11-17)

The gate is `docs/gates/GATE-dragon-knell-2.md`; both Knell scenario files are paired with it.

| piece | as built | where |
|---|---|---|
| **the numbers** | `damage` 65 (Ben); `telegraph_ticks` 26 (Ben's 1.3 s); `flash_at` [0, 20] (the seat's); `teleport.delay_ticks` 89 (the seat's, below) | `knell.yml` |
| **the sounds, as content** | `beam.sounds.telegraph` and `beam.sounds.fire`, each a list of `{ key, volume, pitch }`, every field required; a key the server's `Registry.SOUND_EVENT` does not know is refused at load, by name. The Knell's: `block.bell.use` 1.0 / 1.0 at the telegraph (its volume kept from when it played at the fire); `block.bell.resonate` 1.4 / **1.0** (its volume kept; its pitch was 0.7) and `block.amethyst_block.chime` 1.0 / 1.0 at the fire | `core.mob.BeamSound`, `MobBeam`, `MobLoader`, `RpgPlugin.soundKnown` |
| **one line per sound played** | `BEAM sound mob=… id=… when=<telegraph/fire> tick=… key=… volume=… pitch=…`, just before the TELEGRAPH or FIRE line | `MobBeamer.playSounds`, `MobBeamLines.sound` |

**THE DELAY, 89, AND WHAT IT STANDS ON.** The resonance file is 3.780 s (`block/bell/resonate.ogg`, 181431 samples at
48000 Hz); the client's slowest variant is pitch 0.85 (`sounds.json`), so at our pitch 1.0 it ends at 3.780 / 0.85 =
4.447 s = 88.9 ticks, rounded UP to 89 so the sound has always finished. **One authored number in TICKS; the code never
computes it from a sound.** The server cannot know which variant a client drew, and "playback speed equals pitch" is
still assumed, not read. **89 is below the cycle's 100-tick minimum with 11 to spare; the load-time check refuses a
delay that is not shorter (100 refused, 99 accepted: `MobTeleportTest`). A longer resonance (a lower pitch) would need
a longer cycle.**

**WHAT A LONGER DELAY CHANGES (the brief's B6), DECIDED OUT LOUD.** Read in `MobTeleporter` and `BeamTeleport.skip`:

| during the 89 ticks | what the code does | kept or changed |
|---|---|---|
| the target dies | at the end, `skip`: `target-gone` (`LiveTargets.isLive` is false for a dead body) | KEPT |
| the target logs out | the same, `target-gone` (INFERRED: a quit player's entity is no longer valid) | KEPT |
| the target changes world | `target-world` | KEPT |
| the target goes creative or spectator | `target-exempt` (`MobHostility`) | KEPT |
| the Knell is hit | nothing: no damage listener moves or cancels it | KEPT |
| the Knell is killed | `knell-gone` (read at the stacked boot, TP13) | KEPT |
| the Knell is unloaded | its removal cancels the task: `knell-gone` | KEPT |
| a second beam | its OWN cycle cannot start one (its minimum wait after a fire, 100, exceeds 89; pinned at load and by `MobLoaderTest.theBundledKnellsDelayIsElevenTicksShorterThanItsCycle`); a FORCED one that hits logs `TELEPORT ignored … reason=pending`, and the first move still lands at its own fire + 89 | KEPT, pinned by gate row TP14 |
| the Knell walks and melees | it is rooted only during the telegraph, so for the 89 ticks it is free to walk toward its target and hit it | KEPT, **for Ben to see** |
| where it lands | the circle is drawn around where the target stands at the END of the delay, at the Knell's horizontal distance THEN | KEPT |

**THE ROOT ON A WALKING KNELL (KB2R2).** Vanilla targeting needs sight, READ: `AbstractSkeleton` targets players with a
must-see `NearestAttackableTargetGoal`, and `LivingEntity.hasLineOfSight` clips eye to eye with COLLIDER blocks, which
is why KB2's glass pen kept its Knell blind. KB2R2 puts a live Knell 15 blocks away on an open floor (within its
`FOLLOW_RANGE` of 16). Its top walking speed, READ and executed: `MeleeAttackGoal(this, 1.2, false)` × its speed
base 0.15 = 0.18 as the forward input; ground friction 0.6 × 0.91; × 0.21600002 / friction³; steady displacement
**0.0947 blocks a tick**. Live for at most 105 ticks before the row kills it, 26 of them rooted, it walks at most 79:
**7.48 blocks, against at least 12 needed to reach him. The margin is 4.5 blocks, about 48 ticks at its top speed.**

**CARRIED, UPDATED:** the root on a walking Knell is now **PREDICTED** (KB2R2), not yet read; KB4's timing note is
answered by every row ending itself (an explicit kill or wait), so no TELEPORT line rests on the runner's tick count.

**CARRIED, AFTER THE BOOT OF `58c0f7a1` (2026-11-17):**

- **THE ROOT ON A WALKING KNELL: CLOSED by KB2R2.** Its control read 0.357 blocks of walking in five ticks
  (`x=-93.024`, then `x=-93.381`), and the telegraph and the fire read the same position (`at=-93.452 63.000 6.002`).
- **"THE KNELL WALKS AGAIN AFTER A BEAM": UNREAD.** KB2R2 read it, 13 ticks after its fire, exactly where it fired.
  The seat read `MobBeamer`: unroot runs at the fire and removes the transient modifier; **the seat's INFERENCE,
  not diagnosed, is vanilla's path recalculation delay.** If a later row shows it never walks again, that is a
  defect.

### 5.R.8 BEN'S RE-TIMING AS BUILT (`feat/dragon-knell-3`, off master `232f82b4`, 2026-11-17)

The gate is `docs/gates/GATE-dragon-knell-3.md`; both Knell scenario files are paired with it.

| piece | as built | where |
|---|---|---|
| **the numbers** | `telegraph_ticks` 20 (Ben's 1 s); `flash_at` [0, 15] (the seat's); `teleport.delay_ticks` 44 (the seat's arithmetic, RULINGS); `damage` 65 unchanged | `knell.yml` |
| **the sounds** | at the telegraph: `block.bell.use` 1.0 / 1.0 then `block.bell.resonate` 1.4 / 1.0; at the fire: `block.amethyst_block.chime` 1.0 / 1.0 alone | `knell.yml` |
| **code** | **none.** The sounds were already content (KNELL 2), so the re-timing is a content change; the load-time check (delay shorter than the cycle minimum) is unchanged, 44 < 100 | |

**The 44 is counted from the FIRE and was derived for a 20-tick telegraph** (the resonance starts at the lock): change one
and the other must be re-derived. `knell.yml`'s comment says so.

**WHAT THE RE-TIMING CHANGES (B3), changed nothing:** the resonance now starts whether or not the beam will hit, and rings
on after a miss with no teleport to mark its end; a Knell killed mid-telegraph leaves both telegraph sounds ringing (the
API can stop a sound already sent, `Player#stopSound(String, SoundCategory)`, per player by key and category, so it would
cut every Knell's resonance that player hears: NOT built); the window in which a hit Knell is free to walk and melee
before it moves is 44 ticks, down from 89; a miss has only the chime at its fire.

**"THE KNELL WALKS AGAIN AFTER A BEAM" (B4), DIAGNOSED FROM THE JAR: A BOUNDED DELAY, NOT A DEFECT.** `stopPathfinding`
nulls the navigation's path; `MeleeAttackGoal` (`(this, 1.2, false)`) then reports it cannot continue, the goal
selector stops it, and it restarts only through `canUse`, which passes at most once every 20 game ticks and is asked on
alternate ticks. Our root zeroes velocity once and its modifier is removed at the fire, so nothing is left stuck. **So
the walk resumes at most 21 ticks after the fire (INFERRED from the reads: the order of our task against the mob's AI
inside one tick was not read)**, and KB2R2's 13-tick reading was inside it. Every read with its class, member and
`javap` command is in the gate's K3. **Its witness is WA1**: a live Knell, a forced MISS (he steps 3 south, so no
teleport falls due), and its distance from him 30 ticks after the fire, predicted smaller by more than 0.20.

**CARRIED, UPDATED:** "walks again after a beam" is **PREDICTED** (WA1), not yet read. The 13 closed manual gates'
finding now reads: a live Knell there hears the resonance at the warning, takes 65 one second later, and moves 44 ticks
after a hit.

**CARRIED, AFTER THE BOOT OF `9d0cbffb` (2026-11-17): "WALKS AGAIN AFTER A BEAM" CLOSED by WA1.** Its distance from
him fell 1.562 blocks between the fire (12.374) and 30 ticks later (10.811), against more than 0.20 predicted; the
21-tick bound stays INFERRED.

### 5.R.9 STEP R7a AS BUILT (`feat/dragon-shot`, off master `8365e7f6`, 2026-11-17) — THE PHALANX'S SHOT

The gate is `docs/gates/GATE-dragon-shot.md`, its own scenario file `selftest/GATE-dragon-shot.yml`. Its S1-S5 carry
every jar read this step stands on. **NOT built here:** the blink, invisibility, the one-hit shield, the shield item, any
resource-pack model, any element on the shot, the Skreel.

| piece | as built | where |
|---|---|---|
| **the rule** | `shot: { period_ticks, acquire_range, reacquire_range, speed, homing, life_ticks, max_distance, tolerance, damage, look, sounds }`, each quantity named in `MobShot`'s javadoc and `phalanx.yml`. The Phalanx's: 120 and 240 (Ben), 30, 35, 0.2, 35, 1.2, 40 (cfde822's, unchanged by him), and homing 0.0513 a tick (the seat's conversion of cfde822's 10% per 2-tick step, the ruling on #201) | `core.mob.MobShot`, `MobLoader` |
| **the flight** | pure: `flight = normalize((1 - homing) x flight + homing x normalize(aim - position))`, then `position += speed x flight`; the aim is the target's EYE (cfde822). Endings checked after each move, in order: a block on the path this tick, a hit (inside a hostile combatant's box grown by the tolerance; never a mob), the life, more than `max_distance` from the ORIGIN | `MobShot.step`, `ending`, `victim` |
| **the clock** | an attempt every 120 ticks, the first 120 after the Phalanx appears; a NoAI Phalanx never fires of its own; a forced attempt resets the count | `MobShot.Clock` |
| **the carrier** | an invisible, silent, NoAI, gravity-less, non-colliding, never-saved BAT, with a glowing block display riding it; moved only by `teleport` | `paper.health.MobShooter` |
| **any attack destroys it** | our handles strike first (`BukkitCombatant`: damage, knockback, impulse, status); vanilla damage cancelled at LOWEST and struck (`RpgListeners.onShotDamaged`) | `ShotSink`, `RpgListeners` |
| **the does-nots** | never seeded (`MobNameplateManager.seed`); never dies (removed); a death we did not make pays nothing and feeds no growth; never targeted; never through a portal | `DragonShotWiringTest` |
| **no orphan** | its Phalanx's loop's stop ends it; `setPersistent(false)`; an unloaded column, another world or a refused move ends it; an unowned `mob_shot` entity is removed where it appears; `onDisable` ends every shot | `MobShooter`, `RpgPlugin` |
| **the bow** | `main_hand: bow`, drop chance 0, set in the pre-spawn stamp; every `EntityShootBowEvent` of a mob with a shot is cancelled (`BOW suppressed` line) | `MobSpawner.stampCustom`, `RpgListeners.onShooterBow` |
| **the log** | `SHOT fired`, `SHOT hit`, `SHOT destroyed … by=`, `SHOT expired … limit=life/range`, `SHOT blocked … block=`, `SHOT ended … reason=`, `SHOT retarget`, `SHOT none`, `SHOT orphan removed`, `BOW suppressed` | `MobShotLines` |
| **the instrument** | `shot: <tag>` (fire on the next tick, frozen or not), `probe_shot: <tag>` (each live shot's position, age, distance from its origin); a tagged Phalanx's shot is tagged `<tag>_shot` | `SelfTestDrivers`, `SelfTestService.adoptShot` |
| **the Knell's one flash** | `flash_at: [0]` (Ben, 2026-11-17: "remove it"); the loader and the beam already accepted a single flash, now pinned (`BeamCycleTest.aSingleFlashAtTheLockIsAcceptedAndFlashesNowhereElse`) | `knell.yml` |

**FOR BEN: WHAT THE RECORD SAID, AND WHAT WAS CHOSEN WHERE IT WAS SILENT.** READ = §3.4's record of cfde822.

| # | behaviour | mark | as built |
|---|---|---|---|
| X-a | who it fires at | READ ("the nearest non-creative, non-spectator player within 30") | its vanilla target if `MobHostility` names it and its eye is within 30 of the Phalanx's eye; else the nearest such. *Eye to eye*: CHOSEN |
| X-b | does firing need sight | NOT IN THE RECORD | no; a wall in the way stops the shot (X-g) |
| X-c | the homing | READ (10% per 2-tick step, toward the target's eye, renormalised) | **0.0513 a TICK** (the seat's ruling on #201): 1 − √0.9 = 0.0513167, so two ticks give cfde822's one step. jshell, 2026-11-17, a target held still at an angle θ off the flight, the turn after the record's one step against our two ticks at 0.0513: θ 1° 0.099996° / 0.099964°; 10° 0.996344° / 0.995687°; 45° 4.166309° / 4.136585°; 90° 6.340192° / 6.176908°; 170° 1.241112° / 1.168353°. The per-tick value that matches exactly rises with θ, 0.0513167 (θ → 0) to 0.0540721 (170°), because ours renormalises between its two ticks. The record also moved 0.4 once per step where ours moves 0.2 twice, so the paths differ by a fraction of a step. #201's first build was 10% a tick |
| X-d | the life | Ben: 12 s | 240 ticks. **Unreachable at the shipped numbers** (the gate's E1): a straight shot passes 35 blocks at tick 176 |
| X-e | the 35-block limit | READ ("past 35 blocks from its start") | the straight line from its ORIGIN, not the path flown; past 35, not at it |
| X-f | the hit | READ (the first living entity in a 1.2 half-extent box, mobs included) | a combatant mobs are hostile to (players today), nearest if two; **never a mob** (the seat's ruling 5); the box measured as cfde822's, turned round: the shot's centre inside the body's box grown by 1.2 |
| X-g | a block | READ ("ends on a solid block") | the beam's rule: the first block with a collision box on this tick's path; fluids and passable blocks do not stop it |
| X-h | a lost target | READ ("re-acquires the nearest player within 35") | the nearest within 35 of the SHOT, by the same predicate; none, and it flies on straight. *Measured from the shot*: CHOSEN |
| X-i | its Phalanx dies | READ ("ends … when the Phalanx dies") | ended with it, and also when the Phalanx unloads (CHOSEN) |
| X-j | the order of the endings in one tick | NOT IN THE RECORD | a block, then a hit, then the life, then the range |
| X-k | the look | NOT IN THE RECORD (cfde822's was black squid ink) | a 0.4-block amethyst block display, full brightness, a glow outline RGB 200,140,255 (seen through walls); a dust trail RGB 200,140,255 size 1.5 every tick; a pink flash RGB 255,105,180 size 2.0, three particles, every 4 ticks. CHOSEN |
| X-l | the carrier's box | NOT IN THE RECORD | a bat's own 0.5 x 0.9, unscaled, around the 0.4 block: easier to hit than it looks. CHOSEN |
| X-m | the sounds | READ ("a wither-ambient sound at launch"; nothing else) | at the fire `entity.wither.ambient` 0.5 / 1.5 (which wither, the volume and the pitch: CHOSEN); destroyed `entity.shulker_bullet.hurt` 1.0 / 1.0 (CHOSEN, for "similar to the Shulker shot") |
| X-n | the bow | READ (Power V, never fired) | a plain bow: the enchantment would be a glint and nothing else |
| X-o | a frozen (NoAI) Phalanx | NOT IN THE RECORD | never fires of its own, as the Knell; the instrument's force still works |
| X-p | a player's HOMING projectile | NOT IN THE RECORD | may turn onto a shot, which is a living non-player body to `ProjectileFlight.nearestMob`. Not prevented |
| X-q | firing and invisibility | READ ("firing clears invisibility") | the seam for the next step: there is no invisibility yet |

**AFTER THE BOOT OF `8ec16e89`, 2026-11-17: THE SEAT'S RULINGS ON #201, AS BUILT** (`GATE-dragon-shot.md`, its last
section, carries D1-D3 and every row). The table above described the first build. Read with these changes:
- **The carrier is a FROG**, scaled by `Attribute.SCALE` to `shot.hit_box` (0.8 blocks, the seat's provisional number,
  for Ben). A bat could not be made a cube.
- **It is not made non-collidable.** That had made it unpickable, so no vanilla arrow could hit it: D2, a defect of ours.
- **Only an attack destroys it** (`core.mob.ShotDamage`): a combatant's melee, projectile, explosion, magic or sonic
  boom. `/kill` ends it. Everything else is refused, logged once per cause.
- **A wall ends it "blocked"** when the carrier's box, swept along the tick's path, meets a block's collision shape. A
  wall's SUFFOCATION destroyed one at that boot.
- **The display's offset is re-measured on the server and corrected**, so the shape's centre is the box's centre.
- **The instrument watches a real swing** and can strike along his look.
- **X-l** is now the frog's 0.8 cube, and **X-c's conversion table stands as the conversion's limits** (the seat).

### 5.R.10 STEP R7b AS BUILT (`feat/dragon-blink`, off master `275e9d0d`, 2026-11-17) — THE BLINK, THE SHIELD, C1-C4

**The gate is `GATE-dragon-blink.md`; its INHERITED OR CHOSEN table is the item-by-item account of the blink and the
shield, each marked READ or NOT IN THE RECORD, and is not repeated here.** What the code is:

- **The blink, pure, in core: `MobBlink`** (`blink:` in the mob file). Its trigger is `MobHostility` and a box of
  half-extent `trigger_range` round its feet. Its draws come from an injected `MobBlink.Source` (uniform in production,
  the instrument's fixed draw for BLINK). Its landing is decided through the Knell's ONE check, **`BeamTeleport.foot`**,
  which `BeamTeleport.judge` now calls too (a refactor, no behaviour change: its 20 rows stayed green), through
  **`MobTeleporter.footing(mob, aim)`**, the Knell's port with the aim optional (null: sight is never asked). One `BLINK`
  line carries the decision and every refusal.
- **The blink, Bukkit, in `MobShooter`:** last in its Phalanx's tick, after the fire and every shot's step, so it cannot
  fight or orphan a shot. Its own blink needs AI. It teleports, stops its path, goes invisible, puffs and sounds at both
  ends, and reads itself back (`BLINK moved`). Firing clears invisibility (`BLINK visible … reason=fired`).
- **The shield: `MobShield`** (`shield:` in the mob file) and **`MobShielder`**, the **`ShieldSink`** every handle of ours
  asks. The ONE point is `BukkitCombatant.Handle.applyDamage`, directly after the shot's strike. The state is the
  `mob_shield` PDC byte, set at the spawn stamp. The riders: `applyKnockback`, `applyImpulse` and `applyStatus` drop a
  call in the absorb's tick, and `RpgListeners.onShieldedKnockback` cancels a vanilla push while it is up or in that tick.
- **C1 (closed): `calibrate` runs at the START of every step, before the teleport,** and retranslates on any change. The
  diagnosis is in its javadoc: the one-time calibration read the offset before the display's first ride tick.
- **C2 (closed): `PendingWork`**, in `AdapterContext`. Every `PaperCombatWorld.schedule` link registers its point and
  runs only if still pending. The row-start clean ends the work within 48 and then removes projectiles, area-effect
  clouds and our markers there (`LINGER`).
- **C3 (closed): `SelfTestStrike.next`**: no shot yet is a wait, until the timeout.
- **C4 (closed): a scoreboard team `rpg_shot` whose collision rule is NEVER**, joined at the fire and left at the end;
  `setCollidable` is never called, so an arrow still hits it (one test pins both).

### 5.R.11 STEP R8 AS BUILT (`feat/dragon-skreel`, off master `185a6437`, 2026-11-17) — THE SKREEL PAIR, THE BLINK'S COOLDOWN, R3

**The gate is `GATE-dragon-skreel.md`; its INHERITED OR CHOSEN table (K1-K29) is the item-by-item account of the pair,
each marked READ, ACCEPTED or NOT IN THE RECORD, and is not repeated here.** Its jar facts, the cycle's state machine,
the leap's expression and R3's and R5's readings are there too. What the code is:

| piece | as built | where |
|---|---|---|
| **the numbers** | `pounce:` in the RIDER'S file: `acquire_range 32`, `band 8-12`, `path_speed 1.6`, `windup_ticks 20`, `period_ticks 120`, `launch_vertical 1.2`, `timeout_ticks 40`, `reach 3`, `damage 55`, `knockback 0.7 / 0.4`, the warning's crit (15, every 5) and three sounds; `mount: withered_mount`; `main_hand: iron_spear`. The mount: `max_health 160`, `scale 1.5`, `ridden_speed 1.3`, **no base speed** | `skreel.yml`, `withered_mount.yml`, `core.mob.MobPounce` |
| **the cycle** | pure: RECOVER → HOLD → WINDUP (20) → LEAP (to the ground, or 40) → RECOVER; the period from a wind-up's start; a frozen pair never begins one | `core.mob.PounceCycle` |
| **the leap** | the record's vertical, a horizontal of distance / T held every tick, T from replaying vanilla's air step (the mob's gravity, drag `0.98f`) to the aim's height; the aim fixed at the wind-up's start | `MobPounce.leap` |
| **one spawn** | the rider, then its mount at its feet through the same stamp, the rider's gear score and the same pre-add; `addPassenger`; the ridden modifier | `RpgCommand.spawnMount`, `MobPouncer.pair` |
| **the loop** | period 1 on the MOUNT'S scheduler; ends with either half (dead, removed, unloaded, separated); a dead mount's rider dismounts; resumed on a paired or widowed mount's appearance | `paper.health.MobPouncer` |
| **no vanilla target** | every target event of a paired mob cancelled | `RpgListeners.onPairTargeted` |
| **the hit** | every combatant within 3 of the mount's feet, `applyDamage(55 × GS/100, rider)`, pushed by `setVelocity` (× 1 − KNOCKBACK_RESISTANCE) | `MobPouncer.land` |
| **the blink's cooldown** | `blink.cooldown_ticks: 240`, required; counted first each loop tick, set by a landed blink only; blocks only the Phalanx's own blink; a forced one says `ignored` | `MobBlink.Cooldown`, `MobShooter.blinkStep` |
| **R3** | the height search anchored on `floor(feet.y)` | `MobBlink.anchor` |
| **the instrument** | `pounce: <tag>` (a wind-up now, aimed at his feet), `probe_pair: <tag>` | `SelfTestDrivers` |

**THE CARRIED ITEMS FROM #202, CLOSED BY BUILD, NOT YET BY A READING:** R3 (PB3), R5 (read from the jar, no row), BC4B's
tag (its scenario). **Still UNREAD:** zero damage not spending the shield (core test only); the shield byte surviving an
unload.

### 5.0 THE cfde822 SURVEY'S ORDER, 2026-11-06 — SUPERSEDED BY §5.R, KEPT AS THE RECORD

This refines `PLAN-legacy-port.md` §9.3; where it differs, it says why.

1. **S1 — MB1, mob body fields** (equipment, scale, speed, follow range, and `attack_damage` if M17 is ruled). Core
   unit test first, then `MobLoader`; 8 call sites (§4). **Unlocks:** Phage, Lesser Phage and Goldrot Goon as nearly
   whole mobs (all three are vanilla AI at cfde822); the Knell's body; the mount's and the Phalanx's bodies, inert.
2. **S2 — MB2, on-death effects** (spawn N of an id; an area hit). **Unlocks:** the Phage split and the Goon blast,
   so the fight's wave-0 mobs are complete. Gated on the blast's price (legacy Q-P12) and on who it hits (Q4 below).
3. **S3 — MB3 + E5, a mob's timed attack with an aim-locked telegraph.** **Unlocks:** the Knell's beam, and E5 for the
   Knell Beam weapon (legacy §9.2 step 5) for free.
4. **S4 — MB4, pre-damage reactions + blink.** **Unlocks:** the Knell's dodge and the Phalanx's shield, which share one
   hook, and the Phalanx's blink. *Differs from legacy §9.3*, which split the dodge (step 3) from the Phalanx (step 4):
   the shield and the dodge are the same hook, so building it once is the smaller slice.
5. **S5 — the Phalanx's swirl**: MB3 with a player-seeking homing mode, plus bow suppression. **Unlocks:** the whole
   Phalanx.
6. **S6 — MB5, the mounted pair and its state machine.** **Unlocks:** the Skreel. Last, because it is the only pair,
   the only state machine and the only spear.

**Why this order:** each step adds one capability; it follows the old fight's escalation (wave 0 is Phages and Goons,
wave 1 adds Phalanx and Knell, wave 2 adds Skreels); and S1–S2 give the fight its most numerous mobs before any
scripted behaviour. **The dragon fight itself is its own thread, after these**, and the faction and drop rows of §4.1
belong to it.

**Each mob slice's gate** carries `/rpg spawn <id>` rows in the End and the overworld, so M25's 300 against 100 is
read, not assumed (legacy §8). **The game mode is declared on every row** (`verification.md`, *THE
CREATIVE-DIVERGENCE REGISTER*): every cfde822 behaviour above skips creative and spectator players, so a creative
bench reads none of them.

---

## 6. DESIGN CONSTRAINTS FOR THE LATER DRAGON FIGHT

> **THE FIGHT'S OWN PLAN IS `PLAN-dragon-fight.md`** (Ben's rulings of 2026-11-18, the cfde822 fight survey, and
> what the server allows). This is the pointer; that file is the account. C1 to C6 below still bind it.

**Ben's direction, as briefed (intent, not his words verbatim):** one **TEMPORARY WORLD per group** now, built so it
can move to **a proxy plus a per-fight server** later. The fight does not care where it runs. The **arena is a
template file**. **Fight state lives in the fight**, not in static singletons (CLAUDE.md invariant 3). **Player saving
stays behind its storage port** (`PlayerRepository`).

Each constraint below says what it forbids in practice, and what cfde822 did instead (*read* unless marked). The
mobs above do not need the fight to exist, but **every mob capability must be built so the fight can own it.**

- **C1 — ONE TEMPORARY WORLD PER GROUP.**
  - **Forbids:** finding the fight by dimension or by a fixed world (`Environment.THE_END`, `getEnderDragonBattle()`,
    `getServer().getWorlds()` scans); fixed coordinates as fight geometry ((0, 0), the radius-15 anchor ring, the
    ±200 Enderman box); one fight per world assumed by a map keyed on world UUID shared across fights; a mob behaviour
    that reads anything outside its own world.
  - **cfde822 did:** all of those. The fight was the vanilla End's dragon, started through the `DragonBattle` respawn
    ritual (`spawnDragonfightEncounter`), anchored at (0, 0), and its registries were keyed by world UUID.
  - **And it meets M25** (*inferred*): GS is read from the dimension and the distance from (0, 0). A temporary world's
    environment and coordinates would set the minions' GS. Whether the fight sets its own GS through M3's
    `GearScoreSource` seam is Q9.
- **C2 — THE FIGHT DOES NOT CARE WHERE IT RUNS (proxy + per-fight server later).**
  - **Forbids:** `Bukkit.getOnlinePlayers()` or `world.getPlayers()` as the roster (invariant 3); server-wide
    broadcasts (cfde822 used `getServer().broadcast` for start, fail and death); reading another fight's state; a fight
    that assumes the player's profile was loaded by this JVM before the fight began.
  - **Requires** (*inferred*): the roster is handed to the fight when it starts; getting players in and out is one
    seam (a local teleport now, a proxy transfer later); the fight reports its result as data, not as side effects on
    shared server state.
- **C3 — THE ARENA IS A TEMPLATE FILE.**
  - **Forbids:** using vanilla End generation, pillars or crystals as the arena; anchor points, rings and boxes as Java
    literals; the spawn table (waves, caps, cadences, the storm's Phage-only rule) in Java. **Invariant 2 already says
    the spawn table is content**, and the template is where its positions live.
  - **cfde822 did:** every one as a Java constant (`PHASE_TYPE_CAPS`, `DRAGONFIGHT_REINFORCEMENT_PERIOD_TICKS`,
    `DRAGONFIGHT_PILLAR_RING_RADIUS`, the `switch` in `spawnPhaseWave`).
- **C4 — FIGHT STATE LIVES IN THE FIGHT.**
  - **Forbids:** fight state in a plugin-wide manager or a static; a mob's behaviour loop that outlives its fight; a
    cleanup that sweeps "every tagged entity in the world" rather than the fight's own list; `BukkitRunnable` or the
    Bukkit scheduler (banned; every loop goes through `Scheduler`, and a per-mob loop through `onEntity`).
  - **cfde822 did:** one manager held 7 `Map` fields of per-fight state keyed by dragon or world UUID, and
    `DragonfightReinforcements` an 8th (counted by grepping `private final Map<` in both fetched files). Every mob
    loop was a self-rescheduling `BukkitRunnable` that ended only when its mob died. Teardown swept every entity
    tagged `DRAGONFIGHT_MINION` in the world.
  - **So the mob capabilities must be fight-ownable:** an MB3/MB4/MB5 behaviour is started and stopped by its owner,
    and a `mob_id` mob spawned by a fight belongs to that fight (*inferred* design consequence).
- **C5 — PLAYER SAVING STAYS BEHIND `PlayerRepository`.**
  - **Forbids:** the fight writing player data itself (files, PDC as the record of rewards); rewards granted only in
    memory on the fight's server; `Scheduler.async` touching the Bukkit API while it saves.
  - **cfde822 did:** credited coins directly (`plugin.addTreasure`, D10) on the shared server.
- **C6 — THE WHOLE FIGHT IS VOID** (added by the rework, 2026-11-15; Ben's additional note).
  - **All four mobs are element `void`, and so is every vanilla Enderman** (§3.R.0). An Enderman has no mob file and
    is Void by the type table, so **an Enderman that dies within a small Phage's growth radius feeds it, wherever
    it dies and whoever kills it** (Ben, I: *"Any deaths."*). In the End that includes Endermen the fight never
    spawned. If the fight revives cfde822's war with Endermen (D3, Q10), **the war itself grows Phages.**
  - **Forbids:** keying growth on "a fight mob died" or on a fight's own list. Growth is keyed on the dead mob's
    element and the distance, and nothing else, so it runs the same inside and outside a fight.
  - **OPEN — BEN'S UNCAPPED GROWTH AGAINST THE FIGHT'S MOB CAPS (Q27).** Ben ruled the snowball uncapped (answer D).
    cfde822 capped every spawn per phase, but its split **bypassed** the cap (§3.2). A regrown stage 5 that splits
    adds two Phages per kill with no ceiling, so the fight must say whether split children count against a cap, are
    refused at a cap, or are exempt as cfde822's were. **That is a question about the FIGHT, not about the Phage**;
    the Phage capability spawns what it is told.

**NOTED, NOT DECIDED:**
- **M17, the Knell's damage** (`PLAN-mob-scaling.md` §0 M17, §6 F15). S1 can add `attack_damage`; whether it does,
  and the Knell's number, are Ben's (legacy Q-P11).
- **M27 / F22, the perched dragon** that damage never knocks airborne (`PLAN-mob-scaling.md` §0 M27, §6 F22). For the
  record only: cfde822's perched dragon took **× 0.4 on every non-melee hit** and **× 4 on MAGIC** to undo vanilla's
  0.25 (`DragonfightCombatListener.onPerchedDragonDamage` / `onDragonVanillaScalingCompensation`, *read*), and the
  dragon was immune while a Commander, a Dark Mage or any End crystal lived (`onDragonfightImmunityGate`, *read*).
  Recorded, not adopted.

---

## 7. FACTS FROM THE PINNED JAR, FOR THE REWORK (2026-11-15)

**The instrument.** `pom.xml`'s `paper.version` is `26.1.2.build.74-stable`. API source: `jar xf` of
`~/.m2/repository/io/papermc/paper/paper-api/26.1.2.build.74-stable/paper-api-26.1.2.build.74-stable-sources.jar`.
Behaviour: `javap -c -p -cp run/versions/26.1.2/paper-26.1.2.jar <class>`, whose manifest reads `Build-Number: 74`,
`Implementation-Version: 26.1.2-74-e4e17fc` (`jar xf … META-INF/MANIFEST.MF`), so it is the same build. `javap` and
`jar` are the JDK's, at `~/.jdks/openjdk-26.0.1/bin/`. **READ** = the bytecode or the source was opened;
**INFERRED** = reasoned from what was opened; **UNVERIFIED** = not opened. Server classes are mojang-mapped
(`net.minecraft.…`).

### F-C. The creeper

- **Start and stop (READ, `net.minecraft.world.entity.ai.goal.SwellGoal`).** `canUse`: the swell direction is
  already up, **or** it has a live target with `distanceToSqr < 9.0` (3 blocks). `tick`: the swell direction goes
  **down** (−1) if the target is null or dying, **or `distanceToSqr > 49.0` (7 blocks)**, or the target fails
  `EntitySelector.NO_CREATIVE_OR_SPECTATOR`, or `Sensing.hasLineOfSight` is false; otherwise **up** (+1). The goal
  is registered at priority 2 (`Creeper.registerGoals`). **Both distances are literals** and measured from the
  entity's position, so they **do not follow `SCALE`** (READ: the constants; INFERRED: that `distanceToSqr` uses
  positions, not boxes).
- **The fuse (READ, `Creeper.<init>`, `Creeper.tick`).** `maxSwell = 30` (ticks) and `explosionRadius = 3` are set
  in the constructor. `tick` adds the swell direction to `swell` each tick, floors it at 0, and calls
  `explodeCreeper()` when `swell >= maxSwell`. **If `isIgnited()`, `tick` forces the direction to +1 every tick**, so
  an ignited creeper cannot be backed away from.
- **The API (READ, `org.bukkit.entity.Creeper` source; `CraftCreeper`).** `setMaxFuseTicks(int)` and
  `setExplosionRadius(int)` (each rejects < 0), `setFuseTicks`, `getFuseTicks`, `explode()` (calls
  `explodeCreeper()` directly), `ignite()`, `ignite(Entity)`, `setIgnited(boolean)`, `isIgnited()`. **There is no
  API for the swell direction**, so no API call starts a back-away-cancellable fuse.
- **The events, in order (READ, `Creeper.explodeCreeper`, `ServerExplosion.explode` / `interactWithBlocks`,
  `Creeper.setIgnited`).**
  1. `CreeperIgniteEvent` (cancellable) fires only from `setIgnited`, i.e. flint and steel or `ignite()`. **A
     proximity swell fires no event at its start.**
  2. `ExplosionPrimeEvent` (cancellable; radius and fire settable) fires in `explodeCreeper`. **Cancelled: the
     creeper lives, `swell` is reset to 0 and `DATA_IS_IGNITED` set false.**
  3. Not cancelled: `dead = true`, then `ServerLevel.explode(…, radius, fire, ExplosionInteraction.MOB)`. Inside,
     **`hurtEntities()` runs BEFORE `interactWithBlocks()`**, and `EntityExplodeEvent` fires inside
     `interactWithBlocks`, and only when the explosion interacts with blocks at all. **So cancelling
     `EntityExplodeEvent` spares blocks and cannot spare entities.**
  4. Then the lingering cloud, `triggerOnDeathMobEffects(…, KILLED)`, and **`discard(EntityRemoveEvent.Cause.EXPLODE)`**.
- **Suppressing the damage while the creeper still dies.** Not offered by vanilla in one step (READ, above):
  cancelling the prime keeps the creeper alive; letting it through hurts entities first. **The route for N4 is to
  cancel `ExplosionPrimeEvent`, deal our own damage, and remove the creeper ourselves.** Whether a radius of 0 set
  on the prime event hurts nothing is **UNVERIFIED**, and the route above does not need it.
- **"Exploded" vs "was killed".** A self-exploded creeper is **discarded with cause `EXPLODE`**, and
  `explodeCreeper`'s call list contains no death call (READ), so **it raises no `EntityDeathEvent`** (INFERRED: the
  discard path was not opened). A creeper killed through our pipeline dies by `MobDeathSystem`'s `setHealth(0)`,
  which **does** raise `EntityDeathEvent` (READ, `MobDeathSystem`'s javadoc and code). Under N4 the explosion is
  ours, so our code records the reason itself.
- **`SCALE` on a live creeper (READ, `LivingEntity.onAttributeUpdated`, `LivingEntity.getDimensions`).** A change to
  `Attributes.SCALE` calls `refreshDimensions()`, and `getDimensions` multiplies the type's dimensions by
  `getScale()`. **So the hitbox follows a stage change on a live mob. The swell distances do not** (above). A
  creeper's base size is **0.6 × 1.7** (READ, `EntityType.<clinit>`, `sized(0.6f, 1.7f)` for `creeper`).

### F-B. The blast's reach and falloff (read 2026-11-16, for ruling 27)

Every line below is **READ**, with `C:\Users\Neb91\.jdks\openjdk-26.0.1\bin\javap.exe -c -p -cp
run/versions/26.1.2/paper-26.1.2.jar <class>`.

- **How far, for power 3: 6 blocks, inclusive.** `net.minecraft.world.level.ServerExplosion#hurtEntities` opens with
  `f = radius * 2.0F` (`getfield radius`, `fconst_2`, `fmul`), gathers entities in the box from `floor(center - f - 1)` to `floor(center + f + 1)` on each axis, and
  SKIPS one when `sqrt(entity.distanceToSqr(center)) / f > 1` (`ddiv`, `dconst_1`, `dcmpl`, `ifgt`): exactly at `f` is
  inside. A creeper's power is `explosionRadius` (3, `Creeper.<init>`) × 1 (× 2 when powered), passed by
  `Creeper.explodeCreeper` through `ExplosionPrimeEvent.getRadius()`. So `f` = 6.
- **The two points: the victim's position and the blast's centre, i.e. feet to feet.** `Entity#distanceToSqr(Vec3)`
  subtracts the vector from `getX()`, `getY()`, `getZ()`, the entity's position (its feet). The centre is
  `Creeper.explodeCreeper`'s `getX()`, `getY()`, `getZ()` passed to `ServerLevel.explode`: the creeper's own position.
  **Ours measures the same two points**, so there is no difference for a player or anything else. (The EYE position
  enters only the knockback's direction, below.)
- **Distance to damage.** `net.minecraft.world.level.ExplosionDamageCalculator#getEntityDamageAmount(Explosion,
  Entity, float exposure)`: `f = radius * 2`; `r = sqrt(entity.distanceToSqr(center)) / f`; `x = (1 - r) * exposure`;
  returns `(float) ((x * x + x) / 2.0 * 7.0 * f + 1.0)`. At power 3 and full exposure: 43 at zero distance, 1 at the
  reach.
- **Where exposure (cover) enters:** the third argument, computed by `ServerExplosion#getBlockDensity(Vec3, Entity)`
  (Paper's cache around `getSeenFraction`), the share of rays from the centre to the entity's box not blocked. It is
  computed only when the calculator says the entity is damaged or has a non-zero knockback multiplier. **Left out:
  exposure is 1** (ruling 30, Q34).
- **Where knockback enters:** after the hurt, `hurtEntities` scales the direction (the victim's EYE position minus the
  centre, normalised; `PrimedTnt` uses its position) by the knockback multiplier, exposure and the victim's explosion
  knockback resistance (`LivingEntity.getAttributeValue`), raises `EntityKnockbackEvent`, and `Entity.push`es it; a
  creative flying or spectator player is skipped, and Paper's `disableExplosionKnockback` turns it off. **Left out: our
  blast pushes no one** (ruling 30, Q35).
- **The shape we keep:** with exposure 1 and the `* 7 * f` and `+ 1` removed, `(x * x + x) / 2` with `x = 1 - d / f`:
  1 at zero distance, 0 at the reach. Executed in jshell from the expression above, `(vanilla(d) - 1) / (7 * f)` at
  `f = 6`: d = 0 → 1.000000000; 1.5 → 0.656250000; 2 → 0.555555571; 3 → 0.375000000; 4.5 → 0.156250000; 5.9 →
  0.008472221; 6 → 0.000000000 (the float rows are vanilla's `d2f`; ours is the double).
- **Knockback, re-read 2026-11-17 for the follow-up (Ben's "knockback sounds like a good addition").** Every line
  below is **READ**, with `C:\Users\Neb91\.jdks\openjdk-26.0.1\bin\javap.exe -c -p -cp
  run/versions/26.1.2/paper-26.1.2.jar <class>` on the classes named, unless marked otherwise.
  - **The strength:** `net.minecraft.world.level.ServerExplosion#hurtEntities`, bytecode 529-546:
    `k = (1 - r) * exposure * knockbackMultiplier * (1 - resistance)`, all in double (`dconst_1`, `dload 11`, `dsub`,
    `fload 17` `f2d` `dmul`, `fload 16` `f2d` `dmul`, `dconst_1` `dload 18` `dsub` `dmul`). `r` is the damage's own
    `sqrt(entity.distanceToSqr(center)) / f`, `f = radius * 2`. **It is LINEAR in distance, not the damage's curve:**
    1 at the centre, 0 at the reach. Executed in jshell from that expression at `f = 6`, exposure 1, multiplier 1,
    resistance 0: d = 0 → 1.000000000; 1.5 → 0.750000000; 2 → 0.666666667; 3 → 0.500000000; 4.5 → 0.250000000;
    5.9 → 0.016666667; 6 → 0.000000000. Resistance 0.5 at d = 2 → 0.333333333.
  - **The knockback multiplier:** `ExplosionDamageCalculator#getKnockbackMultiplier(Entity)` is `fconst_1`, `freturn`:
    **1** for every entity, in the default calculator a creeper's explosion uses.
  - **The direction:** the victim's EYE position minus the centre, normalised (`Entity.getEyePosition`,
    `Vec3.subtract`, `Vec3.normalize`; a `PrimedTnt` uses its feet, `Entity.position`). `Vec3#normalize` returns ZERO
    below a length of `9.999999747378752E-6` (`ldc2_w`, `dcmpg`), i.e. `1.0E-5F`. The push is that direction scaled by
    `k` (`Vec3.scale`).
  - **The resistance:** a `LivingEntity`'s `getAttributeValue(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE)`, 0 for
    anything else; in the API, `Attribute.EXPLOSION_KNOCKBACK_RESISTANCE` (read in the pinned `paper-api` jar).
    Paper's `disableExplosionKnockback` world setting makes `k` 0 for a player.
  - **The event:** `CraftEventFactory.callEntityKnockbackEvent(victim, source, attacker, Cause.EXPLOSION, k, push)`,
    and only for a `LivingEntity`. It builds and calls a CHAIN: Bukkit's `org.bukkit.event.entity.EntityKnockbackEvent`
    (or its `EntityKnockbackByEntityEvent`, with the explosion's source as the attacker), then the old
    `com.destroystokyo.paper.event.entity.EntityKnockbackByEntityEvent`, then Paper's
    `io.papermc.paper.event.entity.EntityKnockbackEvent`, each seeing the previous one's result. Cancelled, the push is
    ZERO; otherwise the event's `getKnockback()`.
  - **Can a plugin raise it? Not as vanilla does.** The chain is built inside `CraftEventFactory`, which is the
    server's, not the API's. Paper's `io.papermc.paper.event.entity.EntityKnockbackEvent` has a public constructor
    `(Entity, Cause, Vector)`, but it carries `@ApiStatus.Internal` (read with `javap -v` on the pinned `paper-api`
    jar), and raising that one alone would not raise the Bukkit two a listener may be on. **So ours raises none**;
    the seat rules whether to raise the internal one. Our own knockback listener, `RpgListeners.onCombatKnockback`,
    returns unless the cause is `ENTITY_ATTACK`, so it never acted on an explosion's push either.
  - **How it is applied:** `Entity#push(Vec3)` → `push(x, y, z, null)`: with no source entity it raises nothing more,
    adds the vector to the delta movement (`setDeltaMovement(getDeltaMovement().add(...))`) and sets `needsSync`.
    **A player's own client is not moved by that:** `hurtEntities` also puts the push in `hitPlayers`
    (`Map.put`), and `ServerLevel.explode` sends each hit player a `ClientboundExplodePacket` carrying it, which the
    client applies. **A plugin's route is `Entity#setVelocity`:** `CraftEntity.setVelocity` sets the delta movement
    and `hurtMarked`, and `ServerEntity.sendChanges` sends a `ClientboundSetEntityMotionPacket` to a `ServerPlayer`
    whose `hurtMarked` is set. **The difference, INFERRED:** vanilla's packet ADDS the push to the client's own motion;
    `setVelocity(getVelocity().add(push))` sends the server's delta plus the push as the client's whole motion. For a
    player standing still the server's delta is gravity's small downward part, so the two agree to that part.
  - **Who is skipped:** an entity whose `ignoreExplosion` is true (`Entity` returns false; not overridden in `Player`);
    anything beyond the reach (`r > 1`); an entity whose hurt was cancelled (`lastDamageCancelled`, which jumps past the
    push); and, for the CLIENT packet only, a spectator or a flying creative player (`isSpectator`,
    `isCreative` with `abilities.flying`), or any player under `disableExplosionKnockback`. **Vanilla pushes mobs too**;
    ours pushes only the blast's victims (players), as the seat ruled.
  - **And a frozen mob cannot be moved by a push, READ:** `LivingEntity#aiStep` calls `travel` only when
    `canSimulateMovement()` and `isEffectiveAi()`, and a NoAI mob is not effective AI (§7 F-C). Its delta movement
    is kept (only components under 0.003 are zeroed), so a push on a frozen mob shows in its VELOCITY, never its
    position. **A gate row that reads a frozen mob's position to show it was not pushed would be hollow.**

### F-S. The Shulker bullet, and our own alternative

- **Spawning (READ).** The server constructor is `ShulkerBullet(Level, LivingEntity owner, Entity target,
  Direction.Axis)`; the target is any `Entity`, so a player is fine. The API has `getTarget()` / `setTarget(Entity)`,
  `setTargetDelta`, `setCurrentMovementDirection`, `setFlightSteps` (`org.bukkit.entity.ShulkerBullet` source); the
  owner is the `Projectile` shooter (INFERRED: `setShooter` was not opened).
- **Movement (READ, `ShulkerBullet.selectNextMoveDirection`, `tick`).** It picks its next step along **one block
  axis** (east/west, up/down, north/south) toward the target, re-chosen over the flight, so it zig-zags on the grid
  rather than curving. Each tick its velocity is blended toward a target delta (clamped by `1.025`, `0.2` blend).
  The constant `0.15` recurs in the direction code; **that it is the per-tick speed is INFERRED.** No gravity while
  the target is valid. **It despawns on Peaceful** (`checkDespawn`).
- **What destroys it (READ, `ShulkerBullet.hurtServer`).** **Any** damage that reaches `hurtServer` and is not
  cancelled by `CraftEventFactory.handleNonLivingEntityDamageEvent` destroys it: a crit-particle puff, the hurt
  sound, `destroy(EntityRemoveEvent.Cause.DEATH)`. There is no damage-type check. **Which attacks reach
  `hurtServer`:** a player's melee (READ: `isPickable` returns true; INFERRED: that a swing then reaches `hurtServer`), vanilla arrows and
  vanilla explosions (UNVERIFIED). **OUR weapons do not reach it at all**, READ: `PaperCombatWorld.combatantsNear`
  keeps only `LiveTargets.isLive` entities cast to `LivingEntity`, and a bullet is not living. So **our rays,
  bursts, sweeps and projectiles pass through a real bullet**, which breaks ruling 6's "any attack".
- **On impact (READ, `ShulkerBullet.onHitEntity`, `onHitBlock`, `onHit`).** On an entity: `hurtOrSimulate` with
  `damageSources().mobProjectile(this, owner)` for **4.0**, then post-attack enchant effects, then **Levitation for
  200 ticks** (`EntityPotionEffectEvent.Cause.ATTACK`). On a block: an explosion particle and the hit sound. Either
  way `destroy(Cause.HIT)`. **Replacing the impact:** the damage would pass through our vanilla pricing
  (`VanillaDamagePolicy`), and the Levitation through `EntityPotionEffectEvent`, which a listener could cancel
  (INFERRED); whether `ProjectileHitEvent` can cancel the whole impact is **UNVERIFIED**.
- **Its look.** The bullet is drawn by the client from its entity type; **recolouring it needs a resource-pack
  override of the bullet's texture, which would recolour EVERY Shulker's bullet** (INFERRED). Its size is 0.3125 ×
  0.3125 (READ, `EntityType.<clinit>`).
- **Our own alternative, briefly.** The constraint is that our pipeline hits only living combatants (above), so
  **the smallest thing all our attacks AND vanilla melee and arrows can hit is a small LIVING entity**, made
  invisible (`LivingEntity.setInvisible`, in the jar per `PLAN-legacy-port.md` §1), with AI off, carrying a display
  entity as a passenger (`Entity.addPassenger`, same source) that wears a resource-pack model. **Which living type
  is smallest and behaves best is UNVERIFIED and is the first thing N7's slice reads.** An `Interaction` entity is
  hittable by a player's click but is **not living**, so it fails the same test as the real bullet (READ: the server jar's `net.minecraft.world.entity.Interaction` extends `Entity`, not `LivingEntity`; INFERRED:
  so `combatantsNear` would skip it). **The seat's lean toward our own carrier therefore rests on a READ fact, not a preference.**

### F-E. "Hostile to mobs", and where a mob death is observed

- **Hostility today (READ).** There is no faction. The only side marker is the boolean **`player`** on
  `HealthState` / `CombatantSnapshot`. Player-cast effects choose victims as "not the caster and not a player"
  at four sites: `EffectApplier.applyToNearbyMobs`, `Ignite`'s blast loop, `ProjectileFlight.nearestMob` and
  `SweptLine` (`grep -rn 'player()' core/src/main | grep continue`, at `cc2831ef`: 4 lines). **Mobs choose targets
  through vanilla AI** (`getTarget`), which in vanilla means players. `MobClassifier` (`instanceof Enemy`) decides
  gear score, **not** hostility. **A Summoner would add a second "side" member** (a summon is a mob that mobs should
  attack and our player effects should spare), so the four sites and every new N-capability should ask one named
  predicate rather than `player()`.
- **Where a death is observed (READ, `RpgListeners.onEntityDeath`).** One `EntityDeathEvent` handler, today used
  for Ignite. **It sees every living death: a vanilla Enderman we never spawned, a mob killed by `/kill` or the void
  (its own javadoc: those never reach zero custom HP, so `MobDeathSystem` misses them and this event does not), and
  our own kills** (via `setHealth(0)`). Its javadoc also records, read from the jar, that **the event can fire twice
  for one entity** (it is cancellable with a revive health), so a growth counter needs a once-ness guard, as Ignite's
  `forget` is. **It does NOT see a self-exploded vanilla creeper** (F-C), which is discarded, not killed. Under N4
  the Phage's explosion is ours, so **N5 listens in two places: this handler, and N4's detonation.**

### F-T. "Can this body stand here?" — the API, so the Knell's check is not hand-rolled

- **READ, `org.bukkit.entity.Entity` source (Paper "Collision API"):** `collidesAt(Location)` checks this entity's
  own bounding box moved to that location against blocks, the world border and hard entities (boats, shulkers), and
  **does not load chunks**. `wouldCollideUsing(BoundingBox)` does the same for a given box.
- **READ, `org.bukkit.RegionAccessor` source:** `hasCollisionsIn(BoundingBox)`, the world-side form.
- **So the body-fits half is one call**, and it uses the Knell's real, scaled box (READ: `getDimensions` scales by
  `getScale()`, F-C).
- **The ground-beneath half is NOT offered** by any of the three: they ask "is this space free", not "is there floor
  under it". The simplest form is the same call on a thin box just below the feet: **a collision there means there
  is ground** (INFERRED; it treats any colliding block as ground, a slab or a fence included). The level tolerance
  (K15) is a plain comparison of Y values. **"Does not load chunks"** means an unloaded landing point reads as free
  (INFERRED), so the check must also require the chunk to be loaded.

---

## 8. FINDINGS

**This section is new (2026-11-15):** the plan had no findings section, and the seat's ruling on #192 asked for one.

- **F1, 2026-11-15, the seat's ruling (5) on #192: two older selftest scenario files still tag a mob by a nearest-entity
  selector after spawning it, the pattern that leaked mobs at the first boot of the dragon stack.** Nothing is fixed
  there now.
  - `paper/src/main/resources/selftest/GATE-sunder.yml`, rows s11 and s12: `tag @e[type=zombie,distance=..1.5,sort=nearest,limit=1] add …`.
    Exposed the same way the dragon rows were: the row-start clean's removals run a tick later on each mob's own
    scheduler (GATE-dragon-fix.md, D2), so a zombie left on the same spot by the row before could take the tag.
  - `paper/src/main/resources/selftest/GATE-wither-status.yml`, five `data merge entity @e[type=…,tag=!t,tag=!n,sort=nearest,limit=1]`
    lines. Its selectors exclude mobs already tagged, so only an UNTAGGED survivor could be picked.
  - **The fix, when it is scheduled, is the dragon one:** the `SPAWN` step, or for a vanilla mob a `/summon` that puts
    its tags in its own NBT, so the mob is tagged by identity.
  - Since the fix, every row-start clean is followed by `LEFTOVER living=<n> radius=16`, so a leak in those gates would
    now be named in the log, though their gates do not predict the line.
- **F2, 2026-11-15, read for step R3: vanilla's creeper "radius 3" is an explosion POWER, and its damage reaches twice
  that.** `net.minecraft.world.level.ServerExplosion.hurtEntities` opens with `radius * 2.0F` (`getfield radius`,
  `fconst_2`, `fmul`) and searches that box; the damage then falls off with distance and exposure (the falloff was not
  read). `Creeper.explodeCreeper` passes `explosionRadius` (3, `Creeper.<init>`) times 1, or 2 when powered, as that
  power. **So "keep vanilla's radius" and "3 blocks, full damage" (P-c) are different quantities**: R3 builds the
  latter, as ruled, and Q33 puts the difference to Ben. READ with
  `javap -c -p -cp run/versions/26.1.2/paper-26.1.2.jar net.minecraft.world.level.ServerExplosion`.
  **Answered 2026-11-16:** Ben, *"keep vanillas shap"*; the reach and the curve are §7 F-B, and #193 now builds them.
- **F3, 2026-11-16, the seat's ruling 31: two Folia notes on the blast, not fixed (the project runs on Paper).**
  - The selftest's `detonate` step calls `Creeper#explode()` from the PLAYER'S thread
    (`SelfTestDrivers.creepers`). On Paper every entity is on the main thread; on Folia the creeper may be another
    region's, and the call would need `Scheduler.onEntity(creeper, …)`.
  - `MobBlaster.detonate` hits a victim inline only when `Bukkit.isOwnedByCurrentRegion(victim)`, else on the
    victim's own scheduler a tick later. That branch is written for Folia and has never run: on Paper every victim is
    owned.
- **F4, 2026-11-17, the seat's ruling on the boot of `bd7457a3`: the instrument has no thaw step.** DS1R3 thaws its
  children with a vanilla `execute as @e[tag=ds1,type=creeper] run data merge entity @s {NoAI:0b}`, accepted for that
  gate (that it changes nothing of ours but NoAI is INFERRED). A `thaw:` step in the selftest instrument, reading the
  change back as the `CHILD` line does, is a later tidy-up. Owed, not scheduled.
- **F5, 2026-11-17, the seat's ruling: growth decides a tick late, from where the Phage then stands.** The rule is
  *"within the radius when it dies"*. `MobGrower.reportDeath` collects the candidates at the death, but
  `MobGrower.applyGrowth`, on each candidate's own scheduler a tick or more later, asks `MobGrowth.stageAfter` again
  with the candidate's position THEN, and logs the GROWTH line's `d` from it. Read at the boot: the second run's
  DS1R3 spared its sibling at `d=0.737`, and the GROWTH line printed `d=0.75`. **The fix:** the decision is fixed at
  the death; the later tick may only re-read the STAGE, so two deaths in one tick still add up. The gate's premise
  (the spared `d` and the GROWTH `d` measured between the same two points) was wrong for a live candidate, and its
  re-prediction belongs to the same follow-up.
- **F6, 2026-11-17, INFERRED, not read: the same seam can drop a growth.** Because `applyGrowth` re-asks the rule at the
  new position, a live Phage inside 6 blocks at the death that walks beyond 6 before its own tick would not grow,
  though the BLAST line lists it as spared. No row has staged it.
- **F4, F5 AND F6 BUILT, 2026-11-17, in `feat/dragon-blast-2`** (the seat's follow-up brief; `GATE-dragon-blast-2.md`,
  NOT BOOTED). F4: the instrument's `thaw: <tag>` step, which logs the AI read back, used by DS1R4 in place of the data
  merge. F5: `MobGrower.reportDeath` decides who grows and the distance from the positions at the death, each
  `MobGrowth.Grow` carrying its distance; `applyGrowth` re-reads only the stage (`MobGrowth.restage`, which takes no
  position); the GROWTH line prints that distance to three places. **F6, answered by reading the merged code
  (`ca0ae38a`), not by a boot: yes, it could.** Its `applyGrowth` built a candidate at `vec(mob.getLocation())` on the
  growing Phage's own tick and asked `stageAfter` again; past 6 that returns the old stage, and `if (to == from)
  return;` then dropped the growth with no line. Only a LIVE candidate moves between the death and its tick, and only
  one standing within about a tick's walk of the edge could cross it, so it was rare; it could never ADD a growth,
  because only candidates decided at the death are scheduled. `restage` closes it.
- **F7, 2026-11-17, the seat's ruling on #194: the Phage's push raises NO knockback event, by decision.** Vanilla's
  explosion raises a chain from `CraftEventFactory.callEntityKnockbackEvent` (Bukkit's `EntityKnockbackEvent` or
  `EntityKnockbackByEntityEvent`, the old `com.destroystokyo` `EntityKnockbackByEntityEvent`, and Paper's
  `io.papermc.paper.event.entity.EntityKnockbackEvent`, cause `EXPLOSION`); a plugin cannot build that chain, and
  Paper's own constructor is `@ApiStatus.Internal`, which we do not call (§7 F-B). **What a future protection plugin
  would NOT see:** a region or anti-grief plugin that cancels or scales explosion knockback through any of those
  events (a safe zone, a "no knockback" flag, an anti-cheat velocity check keyed on the event) will not see the
  Phage's push, cannot cancel it, and may read the sudden velocity as unexplained. The push is still a plain
  `setVelocity`, which such plugins may watch by other means. **Revisit if one is ever installed.**
- **F5 AND F6 CLOSED, 2026-11-17, by the boot of `3b07cbee`** (`GATE-dragon-blast-2.md` DS1R4, the seat's reading). A
  thawed, walking child's first blast spared its sibling at `d=0.797`, and the GROWTH line printed `d=0.797`: the
  distance, and the decision, came from the death, not from a tick later. The reading that failed on 2026-11-17
  (spared `d=0.737`, GROWTH `d=0.75`) now agrees.
- **FOR WHOEVER NEXT PREDICTS A MULTI-PHAGE ROW, 2026-11-17: after a push, the bound on a live blast's distance is
  `D ≤ 7`, not `D < 3`.** `D < 3` holds only while the player stands still during the swell (the first blast after a
  heal). A blast pushes him, and vanilla's `SwellGoal.tick` turns a swell down only past 7 (`distanceToSqr > 49`), out
  of sight, or for a creative/spectator target, so a second Phage already swelling within 3 blows wherever he lands
  inside 7. DS1R4 read `d=3.916` for exactly that reason (NOT AS PREDICTED on that clause; the code read right).
- **F8, 2026-11-17, the seat's ruling on #199: A TOKEN RENAME OVER A FILE LEAVES OUT THE LINE THAT DESCRIBES THE
  RENAME.** Twice in one day a scripted rename of row keys (`s/\bKB1R2\b/KB1R3/g` and the like, over a whole scenario
  file) also rewrote the header sentence that names the rename, so it read `(KB1R3 -> KB1R3)` (the beam file, after
  KNELL 3) and `(KB10R5 -> KB10R5, TP2R4 -> TP2R4, TP14R3 -> TP14R3)` (the teleport file, after KNELL 4's first gate
  commit). Both were caught later, only by reading the file, and corrected in `0b4037ec`. **Exclude the describing line
  (by line number, or write the header after the rename), and read it back.**
- **F9, 2026-11-17, the seat's ruling on #199: THE SELFTEST'S SERVER-STOP RESTORE ASSUMES THE MAIN THREAD.** It runs in
  `onDisable` through an `InlineScheduler`, which runs every entity, region and global task on the calling thread.
  **Correct on Paper, where onDisable is on the main thread and nothing else ticks; WRONG on Folia**, whose entities
  and regions belong to their own threads. Accepted on Paper; revisit before any Folia move.
- **F10, 2026-11-17: THE SERVER-STOP RESTORE IS UNREAD ON A SERVER.** Built in #199 (`d7c7ff54`), pinned by
  `SelfTestDisableWiringTest` only; the boot of `0b4037ec` stopped no run, and the seat will not ask Ben to stop one
  on purpose. Its first server reading will be the next run stopped mid-way: `SELFTEST RUN ABORTED
  reason=server-stop`, then the RESTORED line.
- **F11, 2026-11-17: THE SELFTEST'S CRASH-SURVIVAL FILE IS UNBUILT.** A JVM that dies without disabling (a kill, a
  power loss) still leaves a run's staging in the world. It would need the run's snapshot (the saved blocks with their
  states, the tagged mobs' ids, the player's saved state, the game rules) written to disk at run start and at every
  row's staging, and read back and restored at the next enable, before any player joins.
- **F12, 2026-11-17, the seat's ruling on #200: `TELEPORT ignored … reason=pending` IS UNREACHABLE IN SHIPPED CONTENT
  AT 24/24.** A force is refused during a telegraph, so a second beam's fire is at least the telegraph plus one tick
  (25) after the first, while the first move lands 24 after it: no beam, forced or the Knell's own, can hit while a
  move is pending. The branch stays in `MobTeleporter`, pinned by its unit tests. **It becomes reachable again if the
  delay ever exceeds the telegraph plus one tick.** TP14R4 now reads that the pending move clears when it lands.
- **F13, 2026-11-17, step R7a: THE PHALANX'S SHOTS ARE MOVED FROM THE PHALANX'S THREAD.** Each shot is stepped inside its
  Phalanx's own loop (`MobShooter`), which teleports the carrier and reads the target's eye and the blocks on the path.
  **Correct on Paper, where every entity is on the main thread; on Folia** the shot may fly into a region its Phalanx's
  thread does not own. Accepted on Paper, like F3 and F9; revisit before any Folia move.
- **F14, 2026-11-17, step R7a: THE SHOT'S LIFE OF 240 TICKS IS UNREACHABLE AT THE SHIPPED NUMBERS.** At 0.2 blocks a tick
  a straight shot passes 35 blocks from its origin at tick 176, and the homing curls it only toward a target it then
  reaches. **Its only exercise is `MobShotTest.theLifeEndsItAtTick240AndNotAt239`.** It becomes reachable if the speed
  falls below 35 / 240 = 0.146 a tick, or the range rises past 48 (240 x 0.2).
  **UPDATED 2026-11-17, the boot of `3dc0e184`:** for a STILL or straight-line target, as above. **A target that moves
  keeps a curving shot alive far longer.** After the run, off the sheet, Ben played against a live Phalanx, and one of
  its shots chased him and hit at `age=184` (`shot=fe96b267…`), within 35 blocks of its origin the whole time. So the
  life of 240 binds a shot chasing a moving player. No row stages that, and the gate does not read it as a row.
- **F16, 2026-11-17, step R7a: THE CLIENT'S BOX LAGS THE SERVER'S BY UP TO 3 TICKS.** READ: a frog's or a bat's position
  is sent every `updateInterval` (3, the Builder's default) ticks, or on a forced sync (`ServerEntity#sendChanges`).
  INFERRED: the client interpolates between sends, so at 0.2 blocks a tick its box trails the server's by up to 0.6.
  **No API forces a per-tick send; nothing is built for it (the seat's ruling).** The display rides the client's box, so
  what he sees is what his client picks. **SQ1R2's reading beside it:** one swing, landed. His look entered the server
  box at 1.265 and the shot was destroyed at age 67. The lag did not cost that swing.
- **CARRIED FROM #201, NONE A PASS, TO BE CLOSED FIRST IN THE NEXT BRANCH THAT BOOTS** (the Phalanx's blink and shield;
  the seat's ruling at merge, 2026-11-17):
  - **C1. DEFECT:** the visible shape is not inside the hittable box. Its centre is 0.45 above and 0.40 toward −z of
    the box's centre (`inside=false`, SH1R2 at the boot of `3dc0e184`). Ruling 2 on #201 is not met.
  - **C2. UNREAD:** the swept-box wall (`blocked`, nothing refused). SH7R2 was spoiled by SH4R2's grenade field. **The
    row-start clean does not end an ability's area field; that is an instrument gap in its own right.**
  - **C3. UNREAD:** a strike in the last tick before the reach. SH15's `strike_at_age` must wait for the shot to exist.
  - **C4. UNREAD:** whether the collidable carrier pushes, or is pushed by, a player or a mob. Nothing in the instrument
    reads it.
  - **2026-11-17, step R7b: C1-C4 are built as closed (§5.R.10) and re-staged in `GATE-dragon-blink.md`. NOT READ: they
    close when its rows are read on a server.**
  - **2026-11-17, the boot of `ef770b8a`: C1-C4 CLOSED** (`GATE-dragon-blink.md`, the seat's reading at merge):
    - **C1 CLOSED:** BC1X and BC1Z read `inside=true` at ages 5, 15 and 25 on both axes, the box's and the shape's
      centres equal to three places;
    - **C2 CLOSED:** BC2B read `SHOT blocked … age=16 … block=stone`, nothing refused, after `LINGER work=1` ended BC2A's
      field;
    - **C3 CLOSED:** BC3 read `SHOT destroyed … age=32 … by=vanilla cause=ENTITY_ATTACK damager=player`, and no `no
      live shot` line;
    - **C4 CLOSED:** BC4A's two `SELF … at=` and BC4B's husk's two `AT` identical across a shot; Ben: "I don't think so".
- **CARRIED FROM #202, NEW** (the seat's ruling at merge, 2026-11-17):
  - **R3. DEFECT, TO DIAGNOSE:** a blink decided while the Phalanx is off the ground fails every draw. BQ1, tick 2647:
    from y 63.305, all eight `NO_FOOTING` on a flat floor; the next tick, from y 63.000, it landed.
  - **R5. UNREAD:** two `SHIELD knockback cancelled` lines against one `SHIELD absorbed` (BQ1, 12:43:35). No line reads
    a melee hit's damage on a mob.
  - **BC4B's cleanup:** its `/kill @e[tag=bc4b]` does not select its husk (tagged `bc4bh`); BB1's clean removed it.
  - **UNREAD:** zero damage not spending the shield (core test only); the shield byte surviving an unload.
  - **2026-11-17, step R8 (`GATE-dragon-skreel.md`):** **R3 DIAGNOSED AND FIXED, NOT READ** (the heights are whole
    blocks from the column's y, and a fractional anchor stands none on a floor; anchored on floor(y) now; PB3 reads it on
    a slab). **R5 READ, from the jar, no row:** one sprinting hit raises two knockback events (`LivingEntity#hurt`'s and
    `Player#causeExtraKnockback`'s, both `ENTITY_ATTACK`); two lines against one absorb is correct. **BC4B's cleanup
    FIXED** in its scenario.
- **F19, 2026-11-17, step R8: THE KNELL'S TELEPORT HAS R3'S DEFECT, LATENT, NOT FIXED.** `BeamTeleport.decide` puts every
  candidate column at the TARGET'S feet height, and the check tries whole blocks from it. A target mid-jump (or standing
  on a slab) when the move falls due makes every candidate NO_FOOTING, and the Knell does not move. INFERRED from the code,
  never read on a server; the Knell's 13 merged gates staged their targets standing on whole blocks. The fix is the
  blink's (`MobBlink.anchor`), the seat's to schedule.
  **2026-11-17, `feat/void-implosion`: FIXED, NOT READ.** Reproduced first in core
  (`BeamTeleportTest.aTargetOnASlabStillGivesALandingOnTheFloorBesideIt`, a target at y + 0.5, and
  `aTargetMidJumpStillGivesALandingOnTheFloorBelowIt`, at y + 0.83: both found no landing before the fix). Fixed in the
  ONE shared place, the landing check `BeamTeleport.foot`, which now anchors every column on `BeamTeleport.anchor`
  (floor(y), with the blink's snap; `MobBlink.anchor` delegates to it), so the teleport and the blink cannot diverge.
  **Its cost, stated and pinned** (`theCostOfTheAnchorASlabTopIsNeverTried`): a landing on a slab's top, or on any floor
  whose top is not a whole number, is never tried; a Knell in a field made only of slab tops does not move. The blink's
  two R3 rows were restated: a fractional anchor can no longer reach the search. `GATE-void-implosion.md` KS1 reads a
  hit on a player standing on a slab, then a landing.
- **F20, 2026-11-17, step R8: THE PAIR'S LOOP ASSUMES ITS TWO HALVES SHARE A THREAD.** It runs on the mount's scheduler
  and reads and moves the rider (a passenger) and the landing's victims inline when the region owns them. Correct on Paper;
  on Folia a passenger rides in its vehicle's region (INFERRED), and a victim outside it is struck on its own scheduler,
  as the blast's are. Accepted on Paper, like F3, F9 and F13.
- **F21, 2026-11-17, step R8: THE BLINK'S COOLDOWN IS NOT SAVED.** It lives on the Phalanx's loop, so a Phalanx that
  unloads and loads mid-cooldown may blink at once, as its shot clock restarts. The seat's to rule if it should persist.
- **F22, 2026-11-17, THE BAND EDGE (found by PS6 and PQ1 at the boot of `45bc33ce`), A DEFECT, FIXED IN STEP R8's
  SECOND ROUND.** (Not `PLAN-mob-scaling.md`'s F22, which this file cites as M27 / F22.) `MobPounce.standOff` sent the
  mount to EXACTLY the band's far edge (12), and the band counts only distances at or inside 12, so a mount that stopped
  past its goal parked outside the band and never wound up: PS6's died 12.4459 from him after 180 ticks with no second
  wind-up, and PQ1's pair wound up once in 599 ticks, at d=11.935, 473 ticks after it appeared. Fixed: the goal is the
  band's middle, and the cannot-reach rule ends any wait at one period. **Fixed further after the seat's third brief:** the
  middle's own block could still leave the mount stopped outside the band on a diagonal, so the goal is now the block
  whose stopping box lies inside the band (`MobPounce.goal`), and the cannot-reach rule is measured by progress.
- **F23, 2026-11-17, THE PROBE LAG (found by DB6R3 at the boot of `45bc33ce`), AN INSTRUMENT DEFECT, FIXED IN STEP R8's
  SECOND ROUND.** `probe_pair` and `probe_shot` read their mob on its own scheduler, a tick after the step; a kill in the
  next step runs first, and DB6R3's PAIR lines read a dead pair (`stage=none`). The reads stay on the mob's thread (every
  reading already taken keeps its tick); the loader now REFUSES a row that kills within 2 ticks of such a probe
  (`SelfTestStaging.probeLag`), and every shipped row that did so has a `wait: 2` before its kill: eight in
  `GATE-dragon-shot.yml`, two in `GATE-dragon-blink.yml`, and the Skreel gate's own.
- **F17, RESTATED 2026-11-17 (the boot of `ef770b8a`):** the calibrated offset reads `0.000 0.600 -0.400`, the derived
  0.375 × 1.6, not the 0.450 predicted. The 0.45 SH1R2 read was the shape's displacement under the old one-time
  calibration; why it was 0.45 is still unexplained. Not closed.
- **F17, 2026-11-17, step R7b: THE FROG'S PASSENGER OFFSET'S y IS UNEXPLAINED.** The boot of `3dc0e184` read the shape
  displaced (0, 0.45, −0.40) from the box's centre. Its z is the frog's READ attachment, −0.25, times the scale 1.6. Its
  y is not 0.375 × 1.6 = 0.600, and the 0.150 between them is not explained by anything read. **It changes nothing
  built:** `calibrate` measures the offset rather than deriving it, and the gate's `inside=` reads the result.
- **F18, 2026-11-17, step R7b: A FROZEN MOB NEVER MOVES UNDER A PUSH.** READ: `LivingEntity#aiStep` calls `travel` only
  when `isEffectiveAi`, and `Mob#isEffectiveAi` is false under NoAI. So a row that reads a frozen mob's POSITION to see
  a push is hollow. `GATE-dragon-blink.md` reads velocity, with a positive control. Its husk keeps its AI at speed 0.
- **F15, 2026-11-17, step R7a: THE PAPER HALF'S GUARANTEES ARE PINNED ON THE SOURCE, NOT RUN.** That every handle strikes
  first, the seed refuses a shot, the death handler returns for one, and vanilla damage is cancelled at LOWEST are pinned
  by `DragonShotWiringTest` (each killed by a mutation), which reads lines, not behaviour. **The gate's SH2-SH5, SH9 and
  SH12 are their first server reading.**
- **CARRIED AT THE MERGE OF #203** (the seat's ruling, 2026-11-17, after the boots of `45bc33ce` and `3d93a532`):
  - **CLOSED: THE BAND EDGE (F22).** PS6R2 read five wind-ups at exactly P + 120 to P + 600, every distance inside 8 to
    12 (9.817 to 10.915); PS11, on a 45-degree bearing, came to rest 10.570 out and wound up at P + 120; and in Ben's
    PQ3, four wind-ups 120 apart, 9.537 to 11.546 out.
  - **CLOSED: THE PROBE LAG (F23),** by DB6R4: both PAIR lines read `stage=RECOVER` before the kill.
  - **STILL CARRIED:**
    - **F19, A DEFECT:** the Knell's teleport anchors its level search on its target's fractional feet, so a target on a
      slab or mid-jump can fail every landing. First into the next branch that touches the Knell. **FIXED 2026-11-17
      in `feat/void-implosion`, NOT READ** (F19 above; `GATE-void-implosion.md` KS1).
    - **F21:** the blink cooldown is not saved across an unload.
    - **The limits of the band proof:** it is horizontal, on open ground, and its slide is modelled (the 0.6 friction
      INFERRED).
    - **UNREAD:** the shield byte surviving an unload; zero damage not spending the shield.
    - **UNREAD on a server:** the server-stop restore.
    - **UNBUILT:** the crash-survival file.
    - **The 13 closed manual gates that spawn live dragon mobs.**
    - **A rule owed to `.claude/rules`, where gate predictions are described, in the next branch that touches the
      rules** (the seat, 2026-11-17): predict damage against the HURT line, never a health figure regeneration can
      move. PS10's and PS11's SELF lines read 302 against a predicted 300, as DB10R2 did on 2026-11-16. **PAID
      2026-11-17 in `feat/void-implosion`:** `.claude/rules/verification.md`, *PREDICT DAMAGE AGAINST THE HURT LINE*,
      indexed in `CLAUDE.md`.
- **CARRIED AT THE MERGE OF #204** (the seat's ruling, 2026-11-18, after the boot of `6d079ead`):
  - **CLOSED: F19, by KS1** (`GATE-void-implosion.md`): the Knell's teleport from a target on a slab.
  - **NEW, CARRIED:**
    - **The client swell, UNREAD:** ASK 1 was not stated ("Not sure"), so whether the swell packet shows on his client
      is not known. The server's side read as built (every SWELL line `viewers=1`).
    - **The travel model reads 7 to 12 percent long against a real player, cause unknown.** In every pulled row he came
      to rest 0.88 to 0.93 of the modelled travel, always short, never past; the assumption that the client runs the
      same travel as the server's model is now CONTRADICTED.
    - **IM4's 0.41 west, cause unknown:** a stage-1 blast at d = 0.487 with no sideways pull, and he ended 0.41 away
      from the Phage.
    - **A blast on an airborne victim reads `h` and `ground` differently** (DS1R5's second blast: `d=2.318 h=0.934
      ground=false`), and a gate's general clause must allow it.
    - **A predicted figure at a rounding half is predicted from the game's own distance, not a literal** (BL6R3, FZ1:
      the in-game distance was `1.9500000000000028`, one ulp off 1.95, at a half). Owed to `.claude/rules`, where gate
      predictions are described, in the next branch that touches the rules.
- **CARRIED AT THE MERGE OF #205** (the seat's ruling, 2026-11-18, after the boot of `39c0b341`):
  - **CLOSED: the client swell** (#204's carried item): BQ2's question 1, "yes", in both runs.
  - **NEW, CARRIED:**
    - **A DEFECT: the drag varies from run to run on one build.** In the first run a still player was dragged from 4.000
      to 0.491 and held; in the second, the same staging read `end=1.347 outcome=pulled`, and four DRAG lines fell
      outside the gate's bounds. The seat will not raise the drag's 0.28 until this is explained. **BUILT AGAINST in
      `feat/drag-additive` (the additive drag, 0.33); NOT CLOSED until `GATE-drag-additive.md` CS1 is read.**
    - **Two drags can carry a victim inside one origin's margin, UNRULED** (DS1R6: held 0.505 from the second child,
      0.138 from the first).
    - **DR1's 0.39 of drift after a teleport mid-drag, cause INFERRED** (momentum carried through the teleport; the drag
      sent nothing after he left).
    - **Whether he was sprinting in BQ2's question 4, UNREAD:** INFERRED from its DRAG line against the model; no line
      reads his speed before the blast.
- **CARRIED AT THE MERGE OF #206** (the seat's ruling, 2026-11-18, after the boot of `5ee0587a`):
  - **CLOSED: the run-to-run variation** (#205's DEFECT), **for a clean connection, by CS1** (`GATE-drag-additive.md`):
    five stagings from 4 read `end=1.056` five times, spread 0.000, on the lag-0 model's own figure.
  - **CLOSED: the question-4 sprint** (#205's UNREAD): BQ3's DRAGTICK lines read `keys=forward,sprint` for all 21
    ticks, out of the reach in 8.
  - **CLOSED BY BUILD: DR1's 0.39 drift** (the old send's carried momentum): DR1R2 read +0.10.
  - **STILL CARRIED:**
    - **The drag UNREAD on an uneven or remote connection:** every DRAGTICK read one move packet a tick and `ping=0`;
      batching and latency were not exercised. Its independence of them rests on its design and the core sweep.
    - **The 2-tick lag allowance (`Implosion.LAG_ALLOWANCE_TICKS`), the builder's and UNRULED,** and what lies beyond
      it: at a lag of 3 or more a still player can be carried past the MARGIN (from 3 at lag 4, rests 0.379 from the
      feet), not past the origin.
    - **A jumping player barely dragged while airborne, UNRULED** (the air gain 0.00195, a twentieth of stone's).
    - **Two drags inside one margin, UNRULED** (DS1R7: X's drag carried him 0.402 from Y's feet).
    - **The fixture's drag on a mob, and the dropped-mid-drag cases, UNREAD on a server.**

## QUESTIONS FOR BEN

*Answers go into the RULINGS block at the top of this file, verbatim. Legacy Q-P11 to Q-P14 stay open where they are;
Q2, Q3 and Q5 below restate three of them in one line each so this list stands alone.* **Redone 2026-11-15 for the
rework: an answered question says where its answer is, and a new one is added at the end.**

1. **OPEN.** Should these mobs only appear inside the dragon fight, or also out in the world?
2. **OPEN.** Should the Knell get its own hit strength now (M17), or keep hitting like a bare wither skeleton for a while longer?
3. **OPEN.** The beam, the Phage's blast, the Phalanx's shot and the Skreel's leap: should each be a number you set,
   times gear score / 100, like other custom mobs? *(Was "the blast" of the Goon; the blast is now the Phage's.)*
4. **ANSWERED by G** (RULINGS): the blast hurts *"Only players and Summons"*. *(It was asked of the Goon's death
   blast; the Goon is retired and the answer now governs the Phage's.)*
5. **ANSWERED 2026-11-17 by the accepted numbers** (RULINGS, *the Skreel's numbers*: "mount health 160", its own). Is the Skreel's spider its own mob, with its own name and health bar?
6. **ANSWERED 2026-11-17 by the accepted text** (the rider "fights on foot as a plain melee mob"; the mount "carries on as a plain fast spider with no leap"). When one of the Skreel pair dies, should the other keep fighting as a normal mob?
7. **ANSWERED 2026-11-17: "1. yes the first damage of any kind"** (RULINGS, *the Phalanx's shield*). *(Asked as:)* Should the Phalanx's shield block the first damage of any kind (a fall, fire), or only the first hit from a player?
8. **REMOVED by the Knell rework** (RULINGS, the Knell; seat ruling 4). The Knell no longer reacts to arrows.
9. **OPEN.** Inside the fight's world, what gear score should the mobs have: the End's 300, or a number the fight sets?
10. **OPEN.** Should the fight's mobs fight Endermen, as the old fight's did? *(Now also: every Enderman is Void, so
    a war with them feeds the Phages; §6 C6.)*
11. **OPEN.** Should the fight's mobs drop anything or pay coins, given that the old ones dropped nothing but still paid coins?
12. **OPEN.** If the server restarts mid-fight, is that fight simply lost?
13. **SUPERSEDED.** It asked about the survey's order (§5.0). **Its successor:** is the order in §5.R right: bodies
    and elements first, then the whole Phage, then the Knell, the Phalanx, and the Skreel last?
14. **ANSWERED 2026-11-15, CONFIRMED** (RULINGS, the R1 questions, answer 1). **G1** (the seat's reading, RULINGS): one Void death in range advances a Phage one stage, so four
    take it from 1 to 5?
15. **ANSWERED 2026-11-15, CONFIRMED** (answer 1). **G2:** one death advances every Phage below stage 5 in range, not only the nearest?
16. **ANSWERED 2026-11-15, CONFIRMED** (answer 1). **G3:** stages 2–4 sit evenly between stage 1 and stage 5 in size, speed, health and blast, and
    they explode but never split?
17. **ANSWERED 2026-11-15, CONFIRMED** (answer 1). **G4:** a stage 5 that is killed splits into two stage 1s, and one that explodes does not?
18. **OPEN.** When the Knell picks where to land, must it also be able to SEE the player it just hit from there, so it
    never lands behind a wall? (§3.R.1)
19. **ANSWERED 2026-11-15: "Same share"** (answer 4). When a Phage grows a stage, does it heal to full, or keep the same fraction of its health?
20. **OPEN (ruling 5), and LEFT FOR R3** (RULINGS, *WHAT GROWTH LEAVES*). If a Void mob dies nearby while a Phage is already fusing, does that death still count?
    *(R3 ships the seat's provisional P-e: it counts, as built, until Ben rules.)*
21. **ANSWERED 2026-11-15: "No,"** (answer 2); the children start at stage 1. When a stage 5 splits, does its own death count toward its two new children, so they are born at stage
    2? (§3.R.2)
22. **ANSWERED 2026-11-15: "gearscore should be the same as the parent Phage."** (RULINGS, the split's gear score.) Do the two children keep their parent's gear score, as slime children do? (§3.R.2)
23. **PARKED by Ben** (*"Well come back to the wither effect"*): should Wither damage-over-time land on these Void
    mobs? **The fact it waits on:** `withering.yml`'s `immune` list holds the type keys `wither_skeleton` and
    `wither`, so today it refuses the Knell, the Phalanx and the Skreel's RIDER, all wither skeletons. **It does not
    refuse the Skreel's mount (a spider) or the Phage (now a creeper).**
24. **ANSWERED 2026-11-15: "No it's no element"** (answer 5). Is the Skreel's spider mount a Void mob too? Your note names the Skreel, and the mount is its own creature.
25. **OPEN — a naming question only.** The mount is called "Withered Mount", and it now sits on a Void mob. Keep the
    name, or rename it? The same goes for any other wither-worded name in the fight.
26. **ANSWERED 2026-11-15: "Any deaths"** (answer 3; answer E still stands, an explosion does not split). Does ANY death split a stage 5 (a fall, `/kill`, another mob), or only a kill by a player or a summon?
    The old Phage split only on a player's kill (§3.2).
27. **OPEN — about the fight, not the Phage.** The growth is uncapped (answer D). Should split children count against the
    fight's mob caps, be refused at a cap, or be exempt as the old Lesser Phages were? (§6 C6)
28. **OPEN — the build order.** The Phage's growing was built BEFORE its exploding (RULINGS, *THE BUILD ORDER
    CHANGED*), on the seat's reading of "Is there anything more we can do?" as a yes. Is that order right, or should
    growing wait until the explosion is in?
29. **OPEN.** A Phage that grows in a space too small for its new size (a narrow gap, a low tunnel): should it not
    grow there, grow anyway (the server shifts it if it finds room, and otherwise it may take suffocation damage), or
    something else? (§5.R.3, the finding)
30. **OPEN, put to Ben by the seat 2026-11-15.** One command that kills several stage 5s at once splits each of them,
    and the OTHER parents' deaths then grow the newborn children (read at the 2026-11-15 boot: one child went from
    stage 1 to 4 in the same moment it was born). Keep that, or give a newborn a moment in which it cannot grow?
  *(The seat's provisional P-d keeps it, as built, until Ben rules.)*
31. **OPEN, new with step R3.** Is the Phage's blast VOID damage? Today it wears no element at all: it lands on the
    same path as a mob's melee hit, which carries none (§5.R.4).
32. **OPEN, new with step R3.** A shield can block a vanilla creeper's blast. The Phage's blast cannot be blocked: it
    raises no vanilla damage event, and our shield block is read from one (§5.R.4). Should a raised shield block it?
33. **ANSWERED 2026-11-16: "keep vanillas shap"** (RULINGS, *THE SEAT'S MISS ON THE BLAST'S RADIUS*). The radius you
    kept is not quite vanilla's reach. A vanilla creeper's "radius 3" is its
    explosion POWER; the blast actually hurts creatures up to TWICE that, 6 blocks, falling off with distance and
    cover (§8 F2). The Phage hurts everything within 3 blocks for full damage and nothing beyond (the seat's P-c). Keep
    3 at full damage, or reach further?
34. **OPEN, NOT ASKED YET (the seat's ruling 30).** A vanilla blast is weaker behind cover: the blocks between it and you
    cut the damage. The Phage's goes straight through walls. Should cover protect you?
35. **ANSWERED 2026-11-17: "But yes, knockback sounds like a good addition"** (RULINGS, *the 60 and the 150, and
    knockback*; built in `feat/dragon-blast-2`). *(Asked as:)* A vanilla blast throws you back. The Phage's pushes no one. Should it?
