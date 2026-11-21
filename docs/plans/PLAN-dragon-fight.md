# PLAN — THE DRAGON FIGHT: BEN'S RULINGS OF 2026-11-18, THE cfde822 SURVEY, AND WHAT THE SERVER ALLOWS

> **READ THIS FIRST.**
> - **Docs only.** No Java, no content, no gate, no boot. Nothing below is built.
> - **Base:** `master` at `25d5e870` (the squash of #206).
> - **What governs:** §1 (Ben's words) and the parts of §2 marked as BEN'S RULING. Everything else in §2 is the seat's
>   reading and says so. §3 is a fact about cfde822 and is never adopted by being written down. §4 is a fact about
>   the pinned jar. §6 is a proposal; Ben rules the order. §7 is open.
> - **The mobs are not here.** The four fight mobs (the Knell, the Phage, the Phalanx, the Skreel and its mount) are
>   built and live in `PLAN-dragon-mobs.md`; its §6 holds the fight's design constraints C1 to C6, which bind this
>   plan.

---

## 1. RULINGS

*Ben's words, VERBATIM, his spelling included, all dated **2026-11-18**. Where the seat asked a question, the
question comes first, in the seat's words, then Ben's answer.*

### First message: answering the seat's nine decisions

*The nine decisions, as the seat put them: (1) the dragon, (2) the storm, (3) the mini-bosses, (4) the crystals,
(5) the waves, (6) the Endermen, (7) entry, group size and death mid-fight, (8) loot, (9) dialogue.*

> "1. Not sure yet, for now it stays vanilla with increased health
> 2. Yes keep the storm largely the same but with some tweaks ill speak to in a moment
> 3. Yes to the dark mage, no to the commander, again with changes
> 4. Not sure yet
> 5. we will rework it entirely
> 6. drop the war, the enderman are now the main fodder enemy
> 7. not sure yet
> 8. we will keep one guaranteed item per player but I want to build a cascading loot table system for all drops, not just this fight
> 9. Likely keep it"

### Second message: the problems with the old design, and the new storm

> "There was a few problems with the old design:
> 1. The random circle appearing with lightning was very hard to keep track of for a few reasons: players focusing on the fight and the particle render distance not showing the circle. This is less important for the arena border.
> 2. The Mage and Dragon both were basically immune to the entire melee class.
> My solutions to this:
> Keep the 48 block border ring. Instead of a random circle appearing, the Mage teleports somewhere in the arena (glowing) and channels the storm towards him, pulling the border circle closed around him. I want the circle to close in 2 ways. First it quickly closes (Slightly slower than a player sprinting) from 48 blocks around the center to 12 blocks around the Mage. Once it gets to this point, the storm starts slowly closing and the end crystals respawn one at a time aiming at the mage. If all the crystals power the mage before it is killed, the players die and the fight ends. Killing mobs while the PLAYER is inside the storm expands it. Im sure im probably missing some details so ask questions if you have any"

### Third message: answering the seat's questions

*The seat's questions: (1) does "inside the storm" mean inside the ring; (2) is outside still lightning every 2 s;
(3) can players destroy crystals; (4) does a fully closed ring also kill; (5) how far can kills expand the ring;
(6) does the Mage stay put, and can he be hurt from landing; (7) on the Mage's death, does the ring return to 48,
do the crystals go out, does the dragon become hurtable; (8) one Mage per phase; (9) the dragon and melee.*

> "1. Yes, a player inside the ring, specifically the 12 block ring while the mage is channeling
> 2. Yes, but we will change the damage to ramp based on how many times in a row the player has been struck
> 3. no they cannot
> 4. Technically yes, if the ring fully closes they it cannot be reopened and players die to lightning
> 5. The 12 blocks around the mage
> 6. Yes he stays put. Only damagable while channeling
> 7. Correct, the 48 block ring should be returned gradually, an inverse of it's shrinking instead of a snap back
> 8. One mage per phase
> 9. Leave it alone for now. One change I want to make is removing the lingering fireball attack from the dragon."

### Fourth message: when the Mage can be hurt

> "No he cannot be hurt during the fast close, only once the slow close starts"

### Fifth message: the fireball and the perched breath

*The seat asked whether the lingering cloud should go from the fireball only, or from the perched breath too.*

> "Only the fireball"

**So:** the dragon's fireball attack is removed. **The perched breath stays vanilla, its lingering cloud included.**

### Sixth message: how the ring is drawn, and the build order

*The seat asked (1) how the ring is drawn: solid display segments, forced particles, or the square world border;
(2) whether CC's proposed build order stands.*

> "1. I would like to keep particles. We can try forced particles if that helps. I don't care about the minimal particle setting.
> 2. Order is fine. Lightning ramping damage numbers can start at 20, 40, 80 and keeps doubling"

**Seat readings of the sixth message, not Ben's words:**
- The ring is drawn with **particles sent with force** (step F4).
- Visibility under the client's **Minimal** particle setting is **accepted as not required**.
- §6's build order is **RULED**.
- A player's **k-th consecutive strike deals 20 × 2^(k−1)**, with **no cap**.

### Seventh: Ben's answers at the first boot (`GATE-fight-ring.md`, booted at `f85280ee`)

*Each question as the selftest asked it, then Ben's answer, verbatim from the gate's ASK lines.*

1. "Standing here, can you see the whole ring clearly, all the way round?" — **"yes"**
2. "Sprinting east, did the ring's west edge catch you? And did its speed feel slightly slower than a player
   sprinting?" — **"no"** *(the answer does not say which of the two questions it answers)*
3. "The slow close (after it reached 12 blocks): is its pace right?" — **"I think it should be a little faster. Maybe
   .6 blocks per second"**
4. "When the mob died the ring jumped out one block at once. Is that fine, or should it glide out?" — **"the jump is
   fine"**
5. "Do 20, then 40, then 80 feel right for lightning outside the ring?" — **"yes"**
6. "Should armour reduce the lightning's damage?" — **"yes"**
7. "The ring's look: is the colour, the height and the density right? Say what you would change." — **"Higher wall and
   a larger gap between each row. Adding a slight twist to the particles would be cool"**

### Eighth message: the fast close, the Mage's health, the wall, the Mage's body

*The seat asked (1) whether 5 blocks a second felt right for the fast close; (2) the Mage's starting health, suggesting
1,500; (3) the wall as four rows 1.5 blocks apart, each row shifted round from the one below, the whole wall turning
slowly; (4) keep the Mage as a glowing wither skeleton.*

> "1. Let's lower it to 3.8 blocks a second to allow players to react and catch up when struct
> 2. 1500 works for now
> 3. yes
> 4. yes for now"

**So, Ben's from here on:**
- `v_fast` **3.8** and `v_slow` **0.6** (seventh, answer 3).
- The Mage's health **1500** ("for now").
- **Armour reduces the lightning** (seventh, answer 6).
- **A kill's pop stays a jump** (seventh, answer 4).
- **The wall:** four rows 1.5 apart, shifted and turning.
- **The Mage:** a glowing wither skeleton ("for now").

### Ninth: Ben's answers at the second boot (`GATE-fight-mage.md`, booted at `65482e4c`), 2026-11-18

*Each question as the selftest asked it, then Ben's answer, verbatim from the gate's ASK lines.*

1. "Did the fast close's speed feel right?" — **"Needs to be faster. Try 4.5 blocks/s"** *(MQ1's ring did not move
   in that row: its landing was `REFUSED_NO_GROUND`. SUPERSEDED by the tenth message, note 2.)*
2. "Is the slow close's pace right now?" — **"yes"**
3. "Is the wall's height right?" — **"yes, maybe another row or 2"**
4. "Are the gaps between the rows right?" — **"yes"**
5. "Is the twist right?" — **"maybe alternate twists on every other row"**
6. "When you hit him, was it clear that the hit did not count?" — **"yes"**
7. "What should a hit that does not count look or sound like?" — **"that was good"**
8. "Could you see a beam from each crystal to the Mage?" — **"yes"**
9. "Ten crystals, one every four seconds: is that pace right?" — **"no make it 12 seconds each crystal"**
10. "His name is "Withered Dark Mage". Keep it?" — **"for now"**
11. "For one player, is 1500 health right for the Mage?" — **"for now"**

### Tenth message: the arena, the dragon's health, the Mages, entry; the Mage at 6,000 (2026-11-18)

*The seat asked:*
1. Start from a generated stand-in arena, a flat end-stone disc with ten obsidian pillars, replaceable later by a file
   Ben builds.
2. The dragon's starting health, the old fight's being 18,000 plus 6,000 per player beyond three.
3. Keep a Mage at 80%, 60% and 40%.
4. As a stand-in: a dev command to enter; a death returns you to where you entered; the fight fails once nobody is
   alive inside.

> "1. Yes that's fine for now
> 2. 20,000
> 3. let's do 80 60 40 and 20 percent
> 4. yes
>
> Some additional notes
>
> 1. Change the Mage health to 6,000
> 2. leave the fast close as 3.8"

**So, Ben's from here on:**
- The generated arena, "for now".
- The dragon at **20,000**.
- A Mage at **80, 60, 40 and 20 percent**.
- The dev entry, with death returning you to where you entered.
- The Mage at **6,000**.
- **`v_fast` stays 3.8.**
- One crystal every **12 seconds**, ten of them.
- The wall gets **another row or 2**, with **alternate twists on every other row**.
- The refused-hit cue stays ("that was good"; no longer a seat placeholder).
- The Mage's name stays "for now".

**SEAT READINGS of the tenth message, marked as the seat's, not Ben's:**
- **Four Mages, one per crossing** of 80, 60, 40 and 20 percent.
- **No per-player scaling** of the dragon or the Mage yet. That stays OPEN.
- **"leave the fast close as 3.8" SUPERSEDES the ninth's answer 1** ("Try 4.5 blocks/s"). MQ1's ring did not move in
  the row that asked it, so the fast close is re-asked with the wall actually chasing him.

**SEAT PLACEHOLDERS named in the F8 + F9 brief (2026-11-18), each replaceable by Ben:**
- **The sky:** the fight's world fixed at midnight, weather clear.
- **The generated arena:**
  - centre (0, 0) of its world (§4 c: the dragon orbits it; recorded against C1);
  - floor y 64;
  - an end-stone disc of radius 56, 3 thick;
  - ten 3×3 obsidian pillars on a circle of radius 40, tops 12 above the floor;
  - crystal spot n on pillar n.
- **Entry:** `/rpg fight enter` (create or join) and `/rpg fight leave`, dev only.
- **The boss bar:** purple, titled with the dragon's name, tracking the custom store.
- **The win:** vanilla's drops and XP suppressed.
- **The border lightning outside 48:** applies from the fight's first tick (cfde822 waited for the first crossing).
  Flagged.

**A SEPARATE DECISION FOR BEN, not acted on:** Paper **26.3** is out. The `GATE-fight-mage.md` boot printed six
Paper WARN lines saying 26.1.2 is "4 release(s) behind the latest stable release (26.3)". The upgrade procedure
(`CLAUDE.md`, *Upgrade procedure*) is his to start, PacketEvents first.

### Eleventh: Ben's answers at the F8 boots (`GATE-fight-site.md` at `7630e37c`, `GATE-fight-arena.md` at `b70dc88e`), 2026-11-18

*Each question as the selftest asked it, then Ben's answer, verbatim from the gates' ASK lines.*

1. "With the wall chasing you, did the fast close's speed feel right?" — **"yes"** *(MQ1R2, boot of `7630e37c`)*
2. "The arena stand-in, its floor size and its pillars: is there anything that must change before waves are built?"
   — **"no it's good for testing"** *(BA1R2, boot of `b70dc88e`)*
3. "The night sky: is it fine as a stand-in?" — **"It's fine for now but I wonder if it's possible to have it be like
   in the end"** *(BA1R2)*
4. "The six-row wall, alternate rows turning opposite ways: is it right?" — **"yes"** *(BA2R2)*
5. "For one player, is the Mage's 6,000 health right?" — **"yes"** *(BA3R2)*
6. "Twelve seconds a crystal: is that right?" — **"yes"** *(BA3R2)*

*The seat asked whether, in BA3R2, he reached the Mage late or was hitting him without his health dropping:*

> "I reached him late"

**Confirmed by these answers:** `v_fast` 3.8; the six-row wall, turning alternately; the Mage at 6000; 240 ticks a
crystal.

**SEAT READING of "I reached him late", marked as the seat's:** no defect. The landed-hit line of F9 part 1 is still
built, so that the log can say it next time.

**Still open for Ben:**
- keep-inventory and immediate-respawn in a fight world;
- entering at the arena's centre;
- the sky (answer 3; the options are §6.8).

### Twelfth: Ben's answers at the F9 boot (`GATE-fight-dragon.md` at `7e2ebfa8`), and his rulings on the fight's rules, 2026-11-19

*a. Each question as the selftest asked it, then Ben's answer, verbatim from the gate's ASK lines.*

1. "The dragon: did it fly and land as you expect, with no fireball?" — **"yes"** *(DB1)*
2. "Could you hit it with your own weapon while it was in the air?" — **"yes"** *(DB1)*
3. "Could you hit it with your own weapon while it had landed?" — **"yes"** *(DB1)*
4. "The boss bar at the top of the screen: is it right?" — **"not we'll get to that"** *(DB1)*
5. "For one player, is the dragon's 20,000 health right?" — **"yes"** *(DB2; he used `/rpg mana refill` 35 times
   in his fights, so this is read with that)*
6. "What most needs to change before waves are built?" — **"not much if anything"** *(DB2)*

*b. The seat asked five things: (1) the dragon hurt while a Mage is owed: immune from the crossing until that phase's
Mage is dead, or land the queued Mage at once; (2) should the fight's crystals heal the dragon; (3) try a temporary
End-type world with the Paper switch, falling back to the night sky; (4) what a bite and a wing hit should do; (5)
keep-inventory, immediate respawn, entering at the centre. Ben:*

> "1. No it can't be hurt while a mage is owed
> 2. No crystals should not heal the dragon
> 3. yes let's try it
> 4. Suggest some
> 5. Keep inventory yes. The real fight will have multiple players and when they die, should be set to spectator mode until respawned. Players will respawn once the mage has been defeated. It's important that they spawn inside of the storm ring so they don't take storm damage. When all players are defeated at once they are all in spectator mode for a few seconds and get sent back to the normal world and put back to survival.
>
> A few extra changes
>
> 1. Add a lightning strike to the mage landing"

*c. The seat suggested bite 80, wing 40, breath 30 a second, through armour, and asked what happens to a player who
dies when no Mage is alive. Ben:*

> "1. Yes those numbers are fine to start with
> 2. You know what, replace the respawn system with a 60s timer and then they respawn."

*then:*

> "actually, a 60s timer when the mage is not up, when the mage is up, wait until it's defeated"

*d. THEN BEN REPLACED THE RESPAWN RULES. This SUPERSEDES the respawn parts of b and c:*

> "Before I send this, I want to change respawns again. Forget the 60s timer and the blocked while the mage is alive. When a player dies, they are thrown into spectator for the remainder of the fight or until they are respawned by another player. When they die, a copy of them is on the floor. Another player has to hold shift/crouch on top of that players copy for 3 seconds to respawn them."

*e. The seat asked: (1) where the copy lies if he died outside the ring or fell into the void; (2) the health he comes
back with; (3) whether a hit interrupts the reviver; (4) any limit on revives. Ben:*

> "1. The copy should be placed just inside the ring in both cases
> 2. 50% health
> 3. No
> 4. No but we will add modifiers to change that later"

**SEAT READINGS, each the seat's and FLAGGED FOR BEN:**
- the copy is KEPT just inside a moving ring;
- "on top of" means within 1.5 blocks;
- a progress bar for the reviver;
- two revivers do not stack;
- "a few seconds" for all down is 5;
- the landing's lightning is effect only.

**Entering at the centre:** unanswered; it stands as CC's choice.

**Where each ruling is built (the seat's split ruling, 2026-11-19):**
- F9b (`feat/fight-rules`): 1, 2 and 4 (bite, wing, breath), and the lightning;
- F9c (`feat/fight-end`): 3;
- F9d and F9e: the respawn rules of d and e (down, the copy, the revive, all down).
- Until F9d, deaths stay as they are on master.

*f. Ben's own rerun of `GATE-fight-end.md` (2026-11-19, 13:09, a boot OUTSIDE the procedure; the seat: its readings
stand). Each question as the selftest asked it, then Ben's answer, verbatim from its ASK lines:*

1. "The End world: is the sky what you wanted?" — **"yes"** *(EB1)*
2. "Is anything about the End world wrong (the music, the light, fire, the edge)? Say what, or say none." —
   **"good"** *(EB1)*
3. "The lightning at the Mage's landing: is it right?" — **"yes"** *(EB2)*
4. "The dragon's bite (80) and wing (40): do they feel right?" — **"pass"** *(EB3)*
5. "The breath, 30 a second: does it feel right?" — **"remove the knock back from it"** *(EB4)*
6. "For one player, is the dragon's 20,000 health right, now that every phase has its Mage?" — **"yes"** *(EB5)*
7. "What most needs to change before waves are built?" — **"nothing right now"** *(EB5)*

**SEAT READINGS, each the seat's (2026-11-19):**
- "pass" on the bite and wing is NOT STATED.
- The 20,000 "yes" was given with 12 mana refills, in a fight that ended at 59% by his death.

*g. Ben's answers at the second boot of `GATE-fight-end.md` (2026-11-19, 14:30 to 14:37, at `e772f8b5`). Each
question as the selftest asked it, then Ben's answer, verbatim from its ASK lines:*

- **Question 4:** "The dragon's bite (80) and wing (40), at most one of them a second: do they feel right?" — **"yes"** *(EB3R2)*
- **Question 5:** "The breath, 30 a second, with no push: does it feel right?" — **"yes"** *(EB4R2)*
- **Question 8:** "Did the breath push you at all?" — **"no"** *(EB4R2)*
- **Question 6:** "For one player, is the dragon's 20,000 health right, now that every phase has its Mage?" — **"yes"** *(EB5R2)*
- **Question 7:** "What most needs to change before waves are built?" — **"nothing"** *(EB5R2)*

**SEAT READINGS, each the seat's (2026-11-19):**
- `contact_hit_interval_ticks` 20 is now **BEN'S**: the question named it ("at most one of them a second").
- The 20,000 "yes" was given with 27 mana refills.

*h. Ben's answers to the seat's six questions (2026-11-19): waves and Endermen, what "cascading" loot means, the
dialogue, the boss bar, how a group gets in, scaling by group size. Verbatim:*

> "Before I answer any of these questions I want to clarify, Finer tuning of the fight as a whole requires all of it's
> systems made and in order. Changing the health of the dragon before waves is pointless because waves inherently make
> the fight harder.
>
> 1. Endermen should spawn frequently throughout the fight but only agro the players inside either via normal vanilla
> agro or when the mage is channeling. For mob spawning I suggest a random wave roll system. We will design several
> different groups of mobs with a mixture of endermen, knells, skreels, phages, and phalanx, and when the previous group
> is basically defeated, say 25% of them left, the next group spawns in spread out throughout the arena.
> 2. A Cascading loot system is used for deciding the rarity of drops given out to players. It works like this. First
> the rarest rarity is rolled. then if that doesn't land, the next rarest rarity is rolled and so on. In our world today,
> that would look something like this: Exotic 1% then Legendary 3% then Epic 8% then Rare 12% and so on. Numbers are place
> holders. We can tune these numbers to specific drop sources but this is the over arching system.
> 3. We will save this for later as a general polish
> 4. General polish we'll get to it later
> 5. We'll get to this later
> 6. I don't think so"

**SEAT READINGS, each the seat's (2026-11-19):**
- No more "does this number feel right" questions to Ben until the fight's systems all exist. A boot's Ben rows ask
  whether a thing WORKS and is CLEAR, not whether it is tuned.
- No scaling by group size.
- Dialogue, the boss bar and entry are deferred by Ben.
- Waves and Endermen are not built yet: the seat is settling three questions with Ben.

*i. Ben's answers at the second start of `GATE-fight-revive.md` (2026-11-19, 22:38 to 22:42, at `cb821675`). Each
question as the selftest asked it, then Ben's answer, verbatim from its ASK lines:*

- **Question 1:** "Does the copy look like a player lying on the floor, with a name and a line above it?" — **"yes"** *(RVB1)*
- **Question 2:** "What, if anything, looks wrong with the copy? Say what, or say none." — **"nothing"** *(RVB1)*
- **Question 3:** "Did three seconds of holding sneak feel right?" — **"yes"** *(RVB2)*
- **Question 4:** "Did you have to be on top of the copy (not just near it) for it to count?" — **"yea"** *(RVB2)*
- **Question 5:** "Was the progress at the bottom of the screen clear?" — **"yes"** *(RVB2)*
- **Question 6:** "When you went down, was it clear what had happened to you?" — **"yea"** *(RVB3)*
- **Question 7:** "What, if anything, about going down or spectating was unclear? Say what, or say none." — **"its good"** *(RVB3)*

**SEAT READING, the seat's (2026-11-19):** that the copy draws lying down is now CONFIRMED by Ben's eyes.

*j. Ben's ruling on the void (2026-11-19). The seat had asked whether a player should go down as soon as he is a set
distance below the floor, suggesting 10 blocks. Ben, verbatim:*

> "A player should lose 10% of their health each damage tick (0.5s) to the void regardless of their max health."

**SEAT READINGS, each the seat's (2026-11-19):**
- Each void damage tick takes 10% of the player's MAXIMUM health in our store, so the tenth tick takes the last of a
  full bar.
- It ignores defence and every mitigation (a share of maximum, not a hit). It has no element.
- It applies to PLAYERS in EVERY world, not only fight worlds. Mobs are unchanged.
- Where the void begins and how often it ticks stay the server's own.
- A DOWN player still takes nothing (F9d), and fix B's rule still holds.

The game-wide account is `PLAN-world-damage.md`.

---

## 2. SEAT READINGS

*Each item is marked. Only (a) is Ben's; every other item is the seat's reading of his words, not his words, and
stands until he rules otherwise.*

- **a. BEN'S RULING, not a seat reading:** the Mage cannot be hurt during the fast close. **He is damageable only from
  the start of the slow close (the ring at 12 blocks around him) until he dies.** *The seat's earlier reading, that he
  is damageable from the moment he lands, was wrong and is withdrawn* (fourth message, §1).
- **b. Seat reading, not Ben's words.** A fully closed ring is not an instant wipe. No point is inside it, so every
  player is outside, and the ramping lightning kills. It cannot be reopened.
- **c. Seat reading, not Ben's words.** A player's strike count (the "how many times in a row" of the third message,
  answer 2) resets when that player is back inside the ring.
- **d. Seat reading, not Ben's words.** Kills expand the ring only during the slow close, only when the killer is
  inside it, and never past 12 blocks around the Mage.
- **e. Seat reading, not Ben's words.** The Knight Commander is removed ("no to the commander"). The Goldrot Goon was
  already retired (`PLAN-dragon-mobs.md`, its RULINGS of 2026-11-15).
- **f. Seat reading, not Ben's words, FLAGGED TO BEN AS A CONSEQUENCE, NOT RULED.** Endermen are Void
  (`PLAN-dragon-mobs.md` §6 C6), so **an Enderman that dies within 6 blocks of a small Phage grows it**. With
  Endermen as the main fodder (first message, answer 6), every fodder kill near a Phage feeds it. Whether that is
  intended is not ruled.
- **g. Seat reading, not Ben's words.** The dragon is immune while the Mage is alive (cfde822's rule, §3, kept).
  **Crystals as a separate immunity gate: OPEN** (Ben: "Not sure yet").

### SEAT MECHANISM RULING: THE RING IS ONE PURE SHAPE IN `core/`

*The seat's ruling on the mechanism, not Ben's words.* The ring is **one pure function in `core/`**: given the fight's
clock, it returns a centre and a radius. No Bukkit. Everything that draws the ring, strikes outside it or measures
"inside" reads the same function, so the picture and the damage cannot disagree.

**The quantities, named.** Every number not quoted from Ben is OPEN (§7).

| name | what it is | value |
|---|---|---|
| `C` | the arena's centre (from the template, never (0, 0); C1 and C3) | template |
| `R_border` | the border ring's radius about `C` | **48** (Ben, "Keep the 48 block border ring") |
| `M` | where the Mage lands | OPEN (where he may land, §7) |
| `R_mage` | the ring's radius about `M` at the end of the fast close | **12** (Ben) |
| `d` | the distance from `C` to `M`, horizontal | follows from `M` |
| `v_fast` | the speed of the ring's FASTEST-MOVING point during the fast close, blocks per second | **5.0**, *seat placeholder, not Ben's; he sets it at the first boot* ("Slightly slower than a player sprinting") |
| `v_slow` | the slow close's rate, blocks of radius per second | **0.4**, *seat placeholder, not Ben's; he sets it at the first boot* |
| `e_kill` | the radius one counted kill adds back during the slow close, blocks | **1.0**, *seat placeholder, not Ben's; he sets it at the first boot* |
| crystals | how many light, and how often, from the start of the slow close | **10, one every 80 ticks**, *seat placeholder, not Ben's; he sets it at the first boot* |
| the return's speeds | how fast the ring returns after the Mage's death | **the same as the close**: the slow part at `v_slow`, the fast part capped at `v_fast`; *seat placeholder, not Ben's; he sets it at the first boot* |
| strike damage | the k-th consecutive strike outside the ring | **20 × 2^(k−1), no cap** (Ben, sixth message; the seat's reading of "start at 20, 40, 80 and keeps doubling"). **Whether it is also multiplied by gear score stays OPEN** (`PLAN-dragon-mobs.md` Q3); F2 does not scale. |
| `r(t)` | the ring's radius at time `t` | follows |
| **SINCE 2026-11-18 (eighth message): `v_fast`** | | **3.8, Ben's** ("Let's lower it to 3.8 blocks a second"). The 5.0 above was the seat's placeholder |
| **`v_slow`** | | **0.6, Ben's** ("Maybe .6 blocks per second"). The 0.4 above was the seat's placeholder |
| **strike mitigation** | | **armour reduces it, Ben's** (seventh, answer 6: "yes"); **no element, OPEN** (whether Void resistance or any element applies) |
| **the Mage's health** | | **1500, Ben's** ("1500 works for now"), at gear score 100 |
| the Mage's name | | "Withered Dark Mage", *seat placeholder (cfde822's name)* |
| the refused-hit cue | | `item.shield.block` at the Mage, volume 1.0, pitch 0.8, *seat placeholder* |
| crystal spots | | `crystal_count` spots evenly round a circle about C, radius 40, 12 above C's ground, *seat placeholder until the arena* |
| the crystal cue | | `block.beacon.power_select` at the crystal, *seat placeholder* |
| the wall | | rows at +0.2, +1.7, +3.2, +4.7 (Ben's "yes" to four rows 1.5 apart); `row_shift_blocks` 0.25 and `turn_blocks_per_second` 1.0, *seat starting values* |

**The fast close** (*seat derivation, to be pinned by a `core/` unit test before anything draws it*). The ring moves
from (`C`, 48) to (`M`, 12) along one straight interpolation: at progress `s` from 0 to 1, the centre is
`C + s·(M − C)` and the radius is `48 − 36·s`. A point on the ring at angle `θ` then moves, per unit of `s`, by
`(M − C) − 36·u(θ)`, where `u(θ)` is the unit vector at `θ`. **Its fastest point** is the one where `u(θ)` points
away from `M`, and it moves `d + 36` blocks per unit of `s`. **So no point outruns `v_fast` when `s` advances at
`v_fast / (d + 36)` per second, and the fast close lasts `(d + 36) / v_fast` seconds.** Two consequences:
- **The rings nest only when `d ≤ 36`.** Each later ring lies inside every earlier one exactly when the Mage lands at
  most 36 blocks from `C` (`R_border − R_mage`). Past that, part of the 12-block ring lies outside the 48 border, and
  a player standing still can find the ring sweeping OUTWARD over them. **This bounds where the Mage may land**
  (§7), and it is the seat's derivation, not a ruling.
- **The fast close's duration depends on where he lands.** A Mage at `d = 0` closes in `36 / v_fast` seconds; one at
  `d = 36` in `72 / v_fast`.

**The slow close.** The centre stays at `M`; the radius falls from 12 at `v_slow`. A counted kill (§2 d) adds
`e_kill`, capped at 12. At 0 the ring is closed for good (Ben, third message, answer 4).

**The return after the Mage's death** is the same path run backwards (Ben, answer 7: "an inverse of it's shrinking
instead of a snap back"): from (`M`, its radius at his death) back up to (`M`, 12), then from (`M`, 12) back to
(`C`, 48) with the same fastest-point cap. **Whether the slow part of the return runs at `v_slow` or faster is OPEN**
(§7). *(Since 2026-11-18 a seat placeholder runs it at `v_slow`, in the quantity table; Ben sets it at the first
boot.)*

**Lightning.** Outside the ring, a player is struck every 40 ticks (2 s, Ben's answer 2 keeps it). The damage ramps
with that player's consecutive strikes, `k`, and `k` resets inside (§2 c). The ramp's numbers are OPEN. Outside the
48 border, the same lightning applies (cfde822's border, §3; Ben's second message keeps the ring).

---

## 3. THE OLD FIGHT (cfde822 SURVEY)

*Read at `CreaperCrusher/Butterfly-SMP` commit `cfde8222ec2ac35661963776e9d8f0582bc4e12a`, fetched 2026-11-18. Paths
are under `src/main/java/org/example1/butterflysmp/`. **Line citations are safe here and only here**: they name a
fixed commit, which no insertion can move.* Each fact the seat reported is marked CONFIRMED or WRONG. Where the code
adds something the brief did not say, it follows as **Also**.

**Tally: 33 facts checked, 33 CONFIRMED, 0 WRONG.** Several carry an **Also** that changes how they read; those are
the ones to read.

### 3.1 The dragon

1. **HP 18000.** CONFIRMED. `DRAGON_PDC_MAX_HP = 18000.0` (`encounter/dragonfight/DragonfightManager.java:89`). The
   real HP lived in PDC; the engine's max health was pinned at 1000 (`:87`) and re-pinned every second (`:147-177`).
2. **+6000 per player beyond 3 in the largest party.** CONFIRMED (`:93`, `:96`, `:476`). **Also:** counted once at
   the fight's start, over the players in the End world, grouped by party leader, a solo player as a group of one
   (`computeAdaptiveExtraPlayers`, `:568-578`). Never recounted.
3. **Thresholds in code 0.80 / 0.60 / 0.40; comments say 75 / 50 / 25.** CONFIRMED. `double[] thresholds = {0.80,
   0.60, 0.40}` (`:822`); the comments at `:53` and `:833` say 75%, 50%, 25%. Three crossings, then no more phases
   (`:813`).
4. **Immune while any End crystal, Dark Mage or Knight Commander lives.** CONFIRMED
   (`encounter/dragonfight/DragonfightCombatListener.java:87-132`, `onDragonfightImmunityGate`, cancels the event).
   **Also:** "any crystal" means any live `EnderCrystal` in the dragon's world, not the fight's own (`:127`). And a
   stale comment in the manager says the opposite, *"no crystal invulnerability"* (`DragonfightManager.java:804`).
5. **Perched dragon takes × 0.4 from non-melee.** CONFIRMED (`DragonfightCombatListener.java:63-81`). Melee means a
   `Player` damager with cause `ENTITY_ATTACK` or `ENTITY_SWEEP_ATTACK`. **Also:** "perched" is the phase being one of
   `LAND_ON_PORTAL`, `LEAVE_PORTAL`, `BREATH_ATTACK`, `SEARCH_FOR_BREATH_ATTACK_TARGET`, `ROAR_BEFORE_ATTACK`
   (`encounter/BossService.java:208-215`). And every `MAGIC` hit was × 4, to undo vanilla's 0.25
   (`DragonfightCombatListener.java:50-56`).
6. **No invulnerability frames.** CONFIRMED. Each hit schedules `setNoDamageTicks(0)` for the next tick
   (`DragonfightManager.java:795-800`), so it is cleared one tick late, not prevented.

### 3.2 The Dark Mage (`encounter/withered/WitheredDarkMageBehavior.java`)

7. **HP 7200.** CONFIRMED (`:54`).
8. **+2400 per extra player.** CONFIRMED (`:98-101`; `DragonfightManager.java:99`), using the dragon's count from
   fact 2.
9. **On a pillar top.** CONFIRMED (`:79`, `pickRandomPillarTop`). **Also:** the pillars were found by scanning the
   End within ±60 of (0, 0) for columns 5 or more blocks above the central island (`:183-240`), falling back to ten
   hardcoded pillar coordinates (`:49-52`).
10. **AI off.** CONFIRMED (`m.setAI(false)`, `:84`); gravity off too (`:85`); knockback resistance 1.0 (`:103-104`).
11. **Glowing.** CONFIRMED (a permanent `GLOWING` effect, `:107-108`).
12. **Teleports every 8 to 14 s.** CONFIRMED. `160 + random × (280 − 160)` ticks (`:55-56`, `:127-142`), to a
    different pillar at least 5 blocks away when one exists (`:155-166`).

**Also, the Mage's damage gate:** he took damage only while the storm was in GRACE or ACTIVE
(`encounter/StormSystem.java:602-611`, `onWitheredDarkMageStormGate`, cancels the event).

**Also, one Mage per threshold crossing:** `spawnPhaseWave` spawns one at every crossing
(`DragonfightReinforcements.java:184-188`), refused while one is alive by the Mage cap of 1
(`DragonfightManager.java:114-117`; `WitheredDarkMageBehavior.java:73-78`). The wave's own comment says phase 0's
wave has none (`DragonfightReinforcements.java:137-141`); the code spawns one at the first crossing too.

### 3.3 The storm (`encounter/StormSystem.java`)

13. **Brew 80 t.** CONFIRMED (`STORM_BREWING_TICKS`, `:70`).
14. **Grace 160 t.** CONFIRMED (`:71`).
15. **Active 800 t.** CONFIRMED. `STORM_DURATION_TICKS = 960` is grace plus active (`:72`); ACTIVE begins at 240
    ticks and ends at 80 + 960 = 1040 (`:283`, `:293`).
16. **Circle radius 8.** CONFIRMED (`:78`, set at `:276`).
17. **Centred 12 to 25 from the middle.** CONFIRMED (`:76-77`, `:268-275`). **Also:** "the middle" is (0, 0), not the
    arena; the comment says 15 to 30 (`:267`).
18. **Shrink 0.4 / 0.6 / 0.8 blocks per s by phase.** CONFIRMED (`:81`, `:374-384`), read from the dragon's current
    phase each step.
19. **Lightning every 40 t.** CONFIRMED (`:73`, `:299-302`), in ACTIVE only.
20. **160 / 200 / 240 outside.** CONFIRMED (`STORM_LIGHTNING_DAMAGE`, `:89`, for phases 1, 2, 3; `:446-470`).
    **Also:** a real `world.strikeLightning` at the player PLUS `p.damage(damage, LIGHTNING_BOLT)` (`:467-468`), so
    the vanilla bolt's own damage and fire came on top. A player outside 48 is skipped here, because the border
    strikes them (`:461-463`).
21. **+1 radius per kill by a player inside.** CONFIRMED (`:543-567`, `onStormMobKillGrowth`). **Also:** in ACTIVE
    only, not GRACE (`:557`); capped at the starting 8 (`:563-565`); ANY non-player death counted, not only fight
    mobs (`:547`); a summon's kill counted for its owner (`:549-554`, `:575-596`).
22. **Radius 0 wipes and fails.** CONFIRMED (`:311-317`). The wipe is `p.damage(health + 1000, LIGHTNING_BOLT)` to
    every non-creative, non-spectator player in the world (`:472-481`).
23. **3 Phages every 60 t around it.** CONFIRMED (`:98-101`, `:393-416`). **Also:** placed 20 to 35 blocks OUTSIDE
    the circle's edge (`:408-410`); in GRACE as well as ACTIVE (`:289`, `:303`); under the Phage cap. And while the
    storm is in GRACE or ACTIVE, the reinforcement trickle spawns only Phages, 3 per tick
    (`DragonfightReinforcements.java:242-246`).
24. **Next storm 600 t.** CONFIRMED (`STORM_GAP_TICKS = {-1, 600, 600, 600}`, `:92`). **Also:** the comment says
    90 / 60 / 30 s (`:91`); every phase is 600.
25. **Or 400 t after a phase change.** CONFIRMED (`:95`, `:202-209`, consumed once at `:517-519`). **Also:** the
    Mage's death ends the storm and cancels the next brew, so no storm comes until the next crossing (`:214-218`).

**How the old code measured "player inside the circle"** (the brief's question): the player's feet position,
horizontal only, `dx² + dz² ≤ radius²` against the circle's centre (`:464-466`); height ignored. The kill-growth
check measures the KILLER's position the same way (`:560-562`). The border measures from (0, 0), not from any
arena: `x² + z² > 48²` and `< 300²` (`:366-368`).

### 3.4 The arena border

26. **Radius 48.** CONFIRMED (`ARENA_BORDER_RADIUS = 48.0`, `StormSystem.java:109`).
27. **The same lightning outside it.** CONFIRMED: the same 40 t cadence (`:114`) and the same per-phase damage array
    (`:236-247`). **Also, and it matters:** in phase 0 the damage is 0, so **the border did nothing before the first
    crossing** (`:240-241`); it struck **every living entity except the dragon**, fight mobs and Endermen included,
    between 48 and 300 blocks from (0, 0) (`:354-372`); it was drawn as 220 `DUST` particles at ground height every
    4 ticks (`:116`, `:341-349`), with no `force` flag.

### 3.5 Waves, caps and trickle (SUPERSEDED: Ben, "we will rework it entirely"; kept only as history)

*Already tabled in `PLAN-legacy-port.md` §3.0; restated here so this file stands alone.*
`Phage` was `spawnWitheredGrunt`, `Goon` was `spawnWitheredRouge`, `Phalanx` was `spawnWitheredArcher`, `Knell` was
`spawnWitheredKnight`.

**The wave at each crossing** (`DragonfightReinforcements.java:144-194`; the wave index is the phase being left):

| crossing | Phage | Goon | Phalanx | Knell | Skreel pairs | also |
|---|---|---|---|---|---|---|
| 80% | 6 | 3 | 0 | 0 | 0 | a Mage |
| 60% | 4 | 3 | 2 | 1 | 0 | a Mage |
| 40% | 2 | 2 | 3 | 3 | 2 | a Mage and the Knight Commander (`:168-179`) |

**The per-type caps on live mobs, by phase** (`PHASE_TYPE_CAPS`, `DragonfightManager.java:112-118`):

| phase | Phage | Goon | Phalanx | Knell | Skreel pair | Mage | Commander |
|---|---|---|---|---|---|---|---|
| 0 | 10 | 4 | 0 | 0 | 0 | 0 | 0 |
| 1 | 8 | 4 | 3 | 2 | 0 | 1 | 0 |
| 2 | 8 | 3 | 4 | 4 | 3 | 1 | 1 |
| 3 | 7 | 2 | 4 | 5 | 4 | 1 | 1 |

**The trickle:** one attempt every **160 / 120 / 80 / 60 ticks** by phase (`DragonfightReinforcements.java:40`,
`:200-210`), started at the fight's first tick (`DragonfightManager.java:545`). Each attempt tries one Phage and one
Goon, plus a Phalanx and a Knell from phase 1, plus a Skreel pair from phase 2 (`:248-259`), at one random anchor.
**The anchors:** 3, evenly spaced on a **radius-15 ring around (0, 0)** with a random rotation, each spawn jittered
±2 blocks (`:43-45`, `:115-132`, `:263-267`).

**The Knight Commander** (removed by Ben; history only; `encounter/withered/WitheredCommanderEncounter.java`): HP
5600 (`:133`), netherite armour with Protection III and a Sharpness V netherite sword (`:107-128`), follow range 64
(`:135`), a wither-bomb small fireball every 100 ticks at a target within 30 blocks (`:70-71`, `:161-211`), and kept
20 or more blocks from the storm's centre (`DragonfightManager.java:589-648`).

**The war:** every second, each fight mob without a real target was pointed at the nearest Enderman within 30
blocks, and each idle Enderman at the nearest fight mob (`DragonfightFactionListener.java:46-114`); natural Enderman
spawns were refused within ±200 of (0, 0) while a fight ran (`:135-144`). Dropped by Ben (first message, answer 6).

### 3.6 Failure

28. **All players within 48 dead.** CONFIRMED (`DragonfightManager.java:849-878`). Creative and spectator do not
    count as alive; the dying player is excluded; "within 48" is horizontal from (0, 0).
29. **Or the storm closed.** CONFIRMED (`StormSystem.java:311-317`).
30. **Mobs dropped nothing.** CONFIRMED. A death tagged `DRAGONFIGHT_MINION` clears drops and sets XP to 0
    (`DragonfightManager.java:893-899`).

### 3.7 Win

31. **Top three damage announced.** CONFIRMED (`encounter/BossLootListener.java:392-409`, called from
    `DragonfightManager.java:929`), broadcast server-wide.
32. **Each player in the End gets one item at 2% legendary / 13% epic / 85% rare.** CONFIRMED
    (`BossLootListener.java:216-233`: `roll < 0.02`, `< 0.15`, else). **Also:** "each player" is every player in the
    dragon's world, with no presence, damage or game-mode check; the method's own comment says non-creative,
    non-spectator (`:213`), and the code does not filter (`:220`). Overflow drops at the player's feet; each find is
    broadcast server-wide (`:259-283`).
33. **The item ids per tier** (`BossLootListener.java:176-210`; ids from `item/ItemFactory.java`):

| tier | ids |
|---|---|
| Legendary (1) | `DRAGONS_PLUME` |
| Epic (4) | `WITHERED_FALCHION`, `WHISPER_OF_THE_VOID`, `KNELL_BEAM`, `VOIDBLOOM_CHARM` |
| Rare (20) | the four pieces each of the `WITHERED_KNIGHT`, `DRAGON_SCALE`, `GOLDROT` and `VOID_BEARER` diamond armour sets (16, non-Elite); `WITHERED_GAUNTLET`, `VOID_QUIVER`, `GOLDROT_CHALICE`, `VOIDREN_HEART` |

*`combat/DragonsRageListener.java` was in the brief's file list. It is the Dragon Scale **set bonus** (+100% speed
for 20 s at 40% health or below; elite +160% and +25% ranged), not part of the fight; recorded and not surveyed
further.*

### 3.8 The Dark Mage's attacks, if any

**None.** His AI was off (`WitheredDarkMageBehavior.java:84`), and his targeting of players and Endermen was
cancelled (`:274-283`). A search of all of `src/main` for `Dark Mage`, `DARK_MAGE` and `DarkMage` finds only his
spawn, his registration, the storm gate and the immunity gate. **His threat was the storm; he dealt no damage
himself.**

### 3.9 Dialogue, verbatim

**The three phase scripts** (`DragonfightReinforcements.java:54-88`): eight lines per crossing, 80 ticks apart
(`:42`, `:269-278`), sent to the End world only. `§a` is the green speaker, `§b` the cyan; `§c§kXXXXXXXX` is an
obfuscated red name.

```
At the 80% crossing ("Phase 1 (75%)" in the source):
§aSomething strange is happening...
§bWhat's going on?
§c§kXXXXXXXX§r§a is breaking down.
§aThere's a tear!
§aThey're getting through!!
§bCalm down. Can we seal it?
§aNot for long. And not alone.
§aWe need help.

At the 60% crossing ("Phase 2 (50%)"):
§aThey're showing up all over the Nexus.
§aThey're spreading like a cancer.
§bSever the corrupted worlds from the Nexus.
§bBefore it's too late.
§aNo! We can't!
§aTrillions will die!
§bAnd many more if we don't!
§bWe can't save everyone.

At the 40% crossing ("Phase 3 (25%)"):
§aWait! The people of this world are fighting back.
§aAnd they're winning!
§bIt doesn't matter.
§bSever the connection now!
§aNo! They can win.
§bFine, §c§kXXXXXXXX§r§b.
§bLet them tear it all down.
§bThe worlds will burn.
```

**The fight's other lines:**

```
Start, server-wide (DragonfightManager.java:561):
§6[Boss] §5The Ender Dragon §6awakens! §7The fight has begun.
Failure, server-wide (:725-726), the reason one of "all challengers have fallen", "the storm consumed the arena",
"manual failure triggered by <name>":
§6[Boss] §5The Ender Dragon §6retreats into the void... §c[The fight has failed: <reason>]
A Mage spawns, End world (DragonfightReinforcements.java:187):
§5✦ A Withered Dark Mage appears atop a pillar...
Storm brewing / raging / fading, End world (StormSystem.java:490, :284, :496):
§5⚡ The Storm is brewing…
§5⚡ The Storm rages!
§5⚡ The Storm has faded.
A Mage dies, End world (DragonfightManager.java:911, :915):
§6[Dragonfight] §5The Withered Dark Mage §6has fallen! §7The dragon is exposed.
§5⚡ The Storm fades with the Mage's fall.
The Commander dies, End world (WitheredCommanderEncounter.java:361):
§6[Dragonfight] §4The Withered Knight Commander §6has fallen!
Win, server-wide (DragonfightManager.java:944; BossLootListener.java:400, :407, :277-282):
§6[Boss] §5The Ender Dragon §6has fallen!
§6[Boss] §7Top damagers vs §f§5The Ender Dragon§7:
  §6① §f<name> §7— §c<damage> dmg      (then §7② and §c③)
✦ <name> found <item name>!
```

*All of these are legacy `§` text; a port writes them as MiniMessage (CLAUDE.md, BANNED PATTERNS).*

---

## 4. WHAT THE SERVER ALLOWS

*Read 2026-11-18 by javap against `run/versions/26.1.2/paper-26.1.2.jar` (mojang-mapped) and from the paper-api
`26.1.2.build.74-stable` sources jar. **The two are the same build:** the server jar's manifest reads
`Implementation-Version: 26.1.2-74-e4e17fc`, `Build-Number: 74`, matching `pom.xml`'s `paper.version`. Nothing from
memory; no boot. Each claim is marked **READ** (seen in bytecode or source), **INFERRED** (follows from what was
read; says from what) or **UNVERIFIED** (not settled). The reading was delegated to a subagent; **the seat
re-ran the decisive one (c, the dragon's nodes) with a control** and it held.*

> **CORRECTED 2026-11-18: CC ran that control, not the seat.** The line above, and "The seat's control" in the next
> section, credit the seat with a check that CC (Claude Code) ran; the seat verifies from origin and did not re-run
> the jar. The finding stands; only its attribution was wrong. Kept as written, with this note, so the correction is
> visible.

### THE FINDING THAT SHAPES THE ARENA: THE DRAGON ORBITS (0, 0) OF ITS WORLD, WHATEVER WE SET

**READ:** `EnderDragon.findClosestNode()` builds its 24 path nodes once, on rings of radius **60** (nodes 0–11),
**40** (12–19) and **20** (20–23), from `Mth.cos` / `Mth.sin` of fixed angles, with **no `fightOrigin` term**. The
seat's control *(CC's, not the seat's: see the correction of 2026-11-18 above)*: `fightOrigin` appears 4 times in the class's bytecode and 0 times in that method. The node height is
`max(73, the MOTION_BLOCKING_NO_LEAVES heightmap + an offset)` (READ: 73, and the first ring's offset 5; the other
rings' offsets not read). **INFERRED: the arena's centre `C` must be (0, 0) of the temporary world.**

> **THIS MEETS C1** (`PLAN-dragon-mobs.md` §6), which forbids *"fixed coordinates as fight geometry ((0, 0), …)"*.
> One temporary world per group keeps it honest: (0, 0) is then the template's own origin, not a shared world's
> landmark, and no other fight is there. **But the template must be placed centred on (0, 0), and that is a
> constraint the dragon imposes, not a choice.** Flagged in §7.

### a. A temporary world: create, unload, delete

- **READ** `CraftServer.createWorld(WorldCreator)`: refuses only during server STARTUP. **INFERRED main thread
  only:** `prepareLevel` pumps server tasks until its pending chunks reach 0.
- **Cost. READ:** there is no spawn-radius preload any more. `WorldCreator.keepSpawnLoaded` is documented
  *"completely unfunctional"*, and `World.setKeepSpawnInMemory` *"No longer functional since 1.21.9"*. **READ:**
  without `WorldCreator.forcedSpawnPosition(...)`, a new world searches for a spawn, which loads chunks (INFERRED);
  with it, *"without loading any chunks"* (its javadoc). **INFERRED:** with a forced spawn, the cost is building the
  level and its generator, not chunks.
- **Unload. READ** `CraftServer.unloadWorld(World, boolean)`: returns **false if any player is in it**, moving no
  one; fires a cancellable `WorldUnloadEvent`; saves if asked; closes chunks and entities. **So everyone is moved
  out first.** UNVERIFIED: whether region files are released before it returns, so a delete retries.
- **Where it lives. READ:** `CraftWorld.getWorldPath()`. INFERRED: `world/dimensions/<namespace>/<key>`.
- **At a server stop. READ** `MinecraftServer.stopServer`: players are saved, then every level. **The temporary
  world and each player's position in it are written to disk.**
- **After a crash or a stop. READ:** at boot `PaperWorldLoader.loadInitialWorlds` loads only the registered
  dimensions, so **INFERRED: an API world is not reloaded**, and its folder is an orphan. **READ, decisive:**
  `PrepareSpawnTask.start` looks the saved world up and, if it is missing, uses the OVERWORLD with the **same saved
  position**. **INFERRED: a player who logged out or crashed inside an arena logs back in at the same x, y, z of
  the overworld.** **READ:** `AsyncPlayerSpawnLocationEvent` exists, which can send such a player elsewhere.
- **Against C1, C2, C4.** One world per group is buildable on the API alone. The fight must own the cleanup (move
  out, unload, delete), sweep orphans at boot, and catch stale joins. Q12 (a restart mid-fight) gets this answer
  from the server: the world is gone and the player is somewhere unsafe unless we catch it.

### b. The arena from a template file

- **READ:** `StructureManager.loadStructure(File)` and `(InputStream)`; `Structure.place(Location, includeEntities,
  StructureRotation, Mirror, palette, integrity, Random)`, plus overloads taking a `RegionAccessor` and
  block/entity transformers.
- **READ** `CraftStructure.place`: places with block flag 2 (client update, no neighbour updates); the only size
  check is a minimum of 1×1×1. The API javadoc says it may exceed the structure block's 48×48×48. No thread check;
  **INFERRED: synchronous on the main thread, loading every chunk it touches.**
- **Another route, INFERRED** from the `RegionAccessor` overload: place it during the temporary world's own
  generation, moving the cost into chunk generation.
- **Against C3:** a template file is supported directly. Place it with entities off, and spawn the fight's own
  entities ourselves (C4: the fight owns its list).

### c. An Ender Dragon outside the End's battle

- **READ:** `EnderDragon.aiStep` runs the phase manager; every use of `dragonFight` in `aiStep`, `findClosestNode`,
  `findPath`, `kill`, `tickDeath` and `getExpReward` is **null-guarded. Nothing breaks with no fight.**
- **READ:** a level gets its own vanilla `EnderDragonFight` when its dimension type has one. **INFERRED:** an
  END-environment temporary world would build a vanilla fight (a podium, its own dragon); **a NORMAL world does not.**
- **With no fight, READ:** the path and the holding, strafe and takeoff phases treat "no fight" as **no crystals
  alive**: the node floor is 12 (the inner rings, 40 and 20), and the holding pattern goes to landing on one draw in
  3 after each path. **INFERRED: it flies as if every crystal were destroyed, perching often; our own crystals do
  not change its AI.**
- **The perch. READ:** `getPodium()` returns a podium if one was set, else one derived from `fightOrigin`, which
  defaults to (0, 0, 0). The landing, takeoff and death phases use it. **READ Bukkit API:** `getPhase`, `setPhase`,
  `getDragonBattle` (null with no fight), `getPodium`, **`setPodium(Location)`** (same world only). `fightOrigin`
  itself is not in the API (NMS only: a cost, not needed).
- **READ:** `checkDespawn()` is empty (it never despawns). It breaks blocks only with `mobGriefing` on, only
  non-dragon-immune blocks, through a cancellable `EntityExplodeEvent`.
- **Against C1, C3:** a NORMAL world, the arena centred on (0, 0), `setPodium` on the template's perch,
  `mobGriefing` off. No NMS.

### d. The dragon's attacks

- **The fireball. READ** `DragonStrafePlayerPhase.doServerTick`: builds a `DragonFireball` and fires Paper's
  **`EnderDragonShootFireballEvent`**. Not cancelled: a sound (level event 1017) and the spawn. Cancelled: the
  fireball is discarded. **Either way the phase then resets its charge, advances its path and goes to
  `HOLDING_PATTERN`.** With no target it returns to `HOLDING_PATTERN` too.
- **The lingering cloud from the fireball. READ** `DragonFireball.onHit`: it makes an `AreaEffectCloud`
  (`DRAGON_BREATH`, instant damage) and fires `EnderDragonFireballHitEvent`; cancelled, the cloud is discarded.
- **The cleanest removal (Ben's fifth message):** **cancel `EnderDragonShootFireballEvent`.** READ: the phase machine
  runs on exactly as if it had fired, and no fireball means no cloud. Redirecting the phase instead is worse: READ,
  `EnderDragonPhaseManager.setPhase` ends the old phase BEFORE firing `EnderDragonChangePhaseEvent`, and READ,
  `DragonChargePlayerPhase` with no target aborts back to `HOLDING_PATTERN`, so a redirect bounces.
- **The perched breath, a fact about the jar only; RULED to stay vanilla (§1, fifth message).** READ
  `DragonSittingFlamingPhase.doServerTick`: at flame tick 10 it makes its own `AreaEffectCloud` (radius 5, duration
  200, `DRAGON_BREATH`, instant damage) and fires `EnderDragonFlameEvent`; at 200 it moves to takeoff or scanning.
  **Nothing removes it unless we cancel that event, and we will not.** READ, also: while perched,
  `AbstractDragonSittingPhase.onHurt` makes arrows and wind charges deal 0 (a fact for the melee question, which
  Ben left alone: "Leave it alone for now").

### e. End crystals

- **READ:** spawned with `world.spawn(..., EnderCrystal.class)`, removed with `remove()`. `setBeamTarget(Location)`
  (same world only) and `setShowingBottom` exist and are synced. READ: a crystal with a beam target always renders
  at any distance on the server's side. **UNVERIFIED: whether the beam renders outside the End**; it is client
  code. **A boot must look.**
- **Unbreakable. READ** `EndCrystal.hurtServer`: the base invulnerability check, then a dragon attacker is refused,
  then **a cancellable damage event**; then explosion damage removes it with no new blast, and any other damage primes
  a radius-6 explosion and removes it. **READ:** `setInvulnerable(true)` is bypassed by `/kill`-class damage and by a
  creative player. **So: cancel the damage event for our crystals** (players, projectiles and explosions alike),
  with `setInvulnerable(true)` as the backstop.
- **READ:** fire under a crystal and Paper's invulnerable-crystal fix apply only with a dragon fight, so neither
  acts in a NORMAL world.

### f. A moving ring at radius 48, seen by every player

| option | READ | limit |
|---|---|---|
| **Particles with `force`** | `CraftWorld.spawnParticle(..., force)` sends to players within **512** blocks with force, **32** without (`ServerLevel.sendParticles`). **The API never sets the packet's `alwaysShow` flag**; the `ParticleBuilder.force` javadoc says without force *"the particle will show according to the client's particle settings"*. PacketEvents 2.13.0's particle wrapper exposes both `longDistance` and `alwaysShow`. | **UNVERIFIED (client):** whether force alone survives "Minimal"; `alwaysShow` is the flag meant for that, and only a packet can set it. A 48 ring is ~301 blocks round: hundreds of particles per redraw per player. **This is Ben's complaint about the old ring** (§1, second message), which used neither flag (§3 fact 27). |
| **Display entities** | Block, item and text displays track at 10 chunks; this server's `spigot.yml` sets `entity-tracking-range.display: 128`. `setTeleportDuration` (0 to 59 ticks), `setInterpolationDuration`, `setTransformation`, `setViewRange` exist. | **INFERRED:** independent of particle settings, and every ring point is within ~96 blocks of a player inside it, so the ring stays tracked. A smooth ring is some tens of stretched segments (INFERRED, ~48 to 96); moving and resizing it means teleporting each segment with a duration and interpolating its transform. |
| **A per-player world border** | `Player.setWorldBorder`, `Server.createWorldBorder`, `WorldBorder.setCenter` (instant) and `changeSize(size, ticks)` (smooth). | **INFERRED from the server's own bounds check: SQUARE**, and nothing makes it round. Moving it is repeated `setCenter`, a jump (client interpolation UNVERIFIED). A per-player border is packets only; the world's own border is the damaging one. |

**INFERRED, the seat's reading of the table:** display entities are the only round option that does not depend on
the client's particle setting; particles through PacketEvents with `alwaysShow` are an accent; the border only if a
square is acceptable. **Ben picks** (F4 waits on it). And the ring's damage never reads the display: it reads the
`core/` shape (§2), so the picture is never the test.

### g. Lightning as effect only

- **READ:** `World.strikeLightningEffect` marks the bolt as an effect; `LightningBolt` then skips its fire, its
  entity hits and its lightning-rod branch. **No damage and no fire.** It still fires `LightningStrikeEvent`.
- **READ:** the thunder and impact sounds are played client-side; the API javadoc says the sound *"is now client
  side and cannot be removed"*. Tracking range 16 chunks; `setFlashCount` exists.
- **So:** the effect, and our own damage through our pipeline. **cfde822 used the REAL bolt plus its own damage**
  (§3 fact 20), so vanilla's bolt damage and fire landed on top; the effect bolt removes both.

### h. Endermen as fodder

- **Hostility. READ:** `EnderMan.setTarget` applies its attack speed and its "creepy" flag. **READ:** `aiStep` renews
  the anger timer every tick while a target exists, ending only if the target dies (and is a mob), is creative or
  spectator, or the difficulty is PEACEFUL. **INFERRED: `setTarget(player)` sticks.** `setScreaming` and
  `setHasBeenStaredAt` are cosmetic flags (READ).
- **Teleporting. READ:** Paper's **`EndermanEscapeEvent`** covers every self-teleport: running from daylight,
  drowning, a critical hit, a projectile (which it is always immune to), and the stare. **READ, the exception:**
  `teleportTowards` (target more than 16 blocks away, in the stare goal) raises no escape event. **READ:** every
  teleport ends in `randomTeleport`, which fires a cancellable **`EntityTeleportEvent`** with a settable destination.
- **So:** cancel both events for the fight's Endermen, or clamp the destination inside the ring. Keep the arena dry
  and out of rain (water triggers the drowning escape). **And note C6:** these are Void, and their deaths grow
  Phages (§2 f).

### i. Damageable only while the slow close runs

*The state is "slow close running" (§2 a), not "channelling".*

- **`setInvulnerable(true)`. READ:** `LivingEntity.hurtServer` returns at its first check: no event, no red flash,
  no knockback, no hurt sound; and creative players still hit it.
- **Cancelling `EntityDamageEvent`. READ:** `actuallyHurt` returns false when cancelled, so `hurtServer` does too.
  **All feedback is lost**, the same as invulnerable.
- **Setting the damage to 0, uncancelled. READ:** for a non-player, `actuallyHurt` proceeds: invulnerability ticks
  start, the hurt time is set, the damage event is broadcast (the red flash), knockback 0.4 and the hurt sound play.
  The early return on 0 damage is for a player target only. **INFERRED: a 0-damage hit plays every piece of
  feedback, knockback and invulnerability frames included.**
- **INFERRED, and it matters more than the three above:** our weapon and ability hits do not raise this vanilla
  event at all. They land through our own health store (`.claude/rules/standing-decisions.md`, *A TRAVELLING RANGED
  WEAPON AUTHORS KNOCKBACK*: *"every shape routes through the custom health store, so nothing calls
  `entity.damage()`"*). **So the Mage's gate must live in our damage pipeline**, keyed on the fight's slow-close
  state, and the vanilla event is only the path for vanilla hits. Which feedback a refused hit shows is a design
  choice for F6, not settled here.

---

## 5. WHAT WE ALREADY HAVE

**The old loot ids against our content today** (searched 2026-11-18 at `25d5e870`: each id as a file name and as an
`id:` line under `paper/src/main/resources/content/`, then a wider case-insensitive search for each name's words;
**the positive control is `dragons_plume`, which both searches found**):

| old id | in our content? |
|---|---|
| `DRAGONS_PLUME` | **YES**: `weapons/dragons_plume.yml` |
| `WITHERED_FALCHION`, `WHISPER_OF_THE_VOID`, `KNELL_BEAM`, `VOIDBLOOM_CHARM` | no |
| `WITHERED_KNIGHT`, `DRAGON_SCALE`, `GOLDROT`, `VOID_BEARER` armour sets | no (our `armor/` holds only the six vanilla materials) |
| `WITHERED_GAUNTLET`, `VOID_QUIVER`, `GOLDROT_CHALICE`, `VOIDREN_HEART` | no (our `accessories/` holds `brawlers_gauntlet`, `fletchers_quiver`, and four others; none of these) |

**So 1 of the 25 old ids exists, the legendary; none of the epic or rare pools does.** The wider search's other hits
are names that share a word (`withered_shortbow`, `withering`, `withered_mount`), not these items.

**Drops and loot tables today** (searched for `loot`, `getDrops()`, `setDrops`, `LootTable` and `dropItem` across
`core/`, `storage/` and `paper/src/main`, and for `drops` / `loot` keys in content): **our game has no loot table
and no drop system.** A mob dies with its vanilla drops, except a Phalanx's shot, whose death clears them
(`RpgListeners`, `onShotDeath`); a player's death drops nothing (`RpgListeners`, the death handler). The one thing
that pays items out is mining: an ore's `payout` block (`ores/iron.yml`) pays a plain vanilla item per spot plus a
completion bonus, overflow dropping at the player's feet (`MiningService`, THE PAYOUT). Our custom items reach a
player through `/rpg give` and recipes; the mint paths say they are built for "a loot drop" later
(`EnchantRollItems`, `GearScoreItems`, their class comments). **The cascading loot system Ben named is a separate
design and is not designed here.**

---

## 6. PROPOSED BUILD ORDER

**RULED 2026-11-18** (sixth message: "Order is fine"). *(Was: proposal only; Ben rules the order.)* Small steps, each bootable on its own, auto rows preferred, fewest boots.
**Steps F1 to F3 need no server at all**, and **F4 to F7 need no arena and no dragon**: they run in an ordinary
world around a fixed point.

| step | what | needs | boot |
|---|---|---|---|
| **F1** | The ring as one pure shape in `core/` (§2, the mechanism ruling): fast close, slow close, kill expansion, closed-for-good, the return. Unit tests pin the fastest-point cap, the nesting bound `d ≤ 36`, and the return as the same path. | nothing | none |
| **F2** | The strike ramp in `core/`: a per-player count of consecutive strikes, reset inside (§2 c). Unit tests. | the ramp's numbers, or placeholders marked as such | none |
| **F3** | One Mage cycle as a pure state machine in `core/`: landed → fast close (undamageable) → slow close (damageable; crystals on their schedule) → his death (return) or all crystals (fail). Unit tests. | crystal count and pace, or placeholders | none |
| **F4** | The ring drawn and struck in an ordinary world about a fixed point, driven by a dev command. Auto rows: the server's radius each tick against F1's. | §4 f picks the display | 1 (with F5) |
| **F5** | Lightning as effect only, our damage through our pipeline, the ramp. Auto rows on the HURT line. | §4 g | (with F4) |
| **F6** | The Dark Mage as a content mob with the channel: lands, glows, stays put, undamageable until the slow close. Auto rows. | §4 i; Mage health | 1 (with F7) |
| **F7** | The crystals: spawned one at a time, beams aimed at the Mage, unbreakable, all powered → the fight fails. | §4 e | (with F6) |
| **F8** | The temporary world per group and the arena from a template file. | §4 a, b; entry and group size | 1 |
| **F9** | The dragon in that world: increased health, the fireball removed, immune while the Mage lives. | §4 c, d; dragon health | (with F8) |
| **F10** | Endermen as fodder; the new waves. | §4 h; Ben's wave tables | 1 |
| **F11** | One guaranteed item per player. | the cascading loot system, designed first | later |
| **F12** | Dialogue. | Ben's lines | with any boot |

**Fewest boots: four** (F4+F5, F6+F7, F8+F9, F10), with F1 to F3 at zero.

### 6.1 F1 TO F3 AS BUILT (`feat/fight-core`, 2026-11-18): WHAT THE CODE SHOWED

*The classes are in `core/.../core/fight/`: `FightSettings`, `Ring`, `Landing`, `FastClosePath`, `SlowClose`,
`ReturnPath`, `StrikeRamp`, `MageCycle`. Every finding below is a seat choice or a gap in the brief, recorded here so
the first boot can rule on it. None stops F4.*

- **WITH THE SEAT PLACEHOLDERS, THE RING ALWAYS CLOSES BEFORE THE CRYSTALS FINISH, UNLESS THE PLAYERS KILL.** The slow
  close lasts 12 / 0.4 = 30 s = **600 ticks**; ten crystals at 80 ticks take **800**. Each counted kill adds 1 block,
  which is 50 ticks of slow close, so **at least 4 net kills inside the ring** are needed before POWERED can happen at
  all. With no kills, every Mage cycle ends CLOSED at 30 s. A property of the placeholders, not of the code; Ben sets
  them at the first boot (`MageCycleTest`, *withNoKillsTheRingClosesBeforeTheCrystalsFinish*, and the six-kill staging
  in *theLastCrystalPowersTheMageOnTheExactTick*).
- **"No cap" and "finite for any k" meet at k ≈ 1020.** 20 × 2^(k−1) passes the largest finite double there, so
  `StrikeRamp.damageForStrike` returns `Double.MAX_VALUE` from that k on. Any finite value that large kills, so this is
  not a gameplay cap. **Owed to F5:** the damage pipeline must take a `Double.MAX_VALUE` hit without overflowing (a
  multiplier above 1 on it gives infinity). A dev-only edge: a player struck ten times in a row has already taken
  20 × 2^9 = 10240 on his tenth.
- **A closed ring contains no point, its centre included.** "Inside" is inclusive at the edge, and a radius-0 ring
  would otherwise contain exactly its centre. `Ring.contains` refuses every point at radius 0 (§2 reading b).
- **"One every 80 ticks from the start of the slow close" is read as the first at +80**, the tenth at +800. The other
  reading, the first at +0, would power him at +720. Seat choice.
- **Ties on one tick go against the players.** The machine places every transition on its exact tick before it acts
  on that tick's input, so a kill on the tick the ring closes is too late, and a Mage death on the tick the last
  crystal lights is too late (the fight fails POWERED). If the ring closes and the last crystal lights on the same
  tick, the outcome is CLOSED. Seat choices.
- **A held Mage counts as alive.** From a landing HELD during the return, the dragon's immunity flag (`mageAlive`) is
  true again, though his fast close has not begun. A held Mage's death cancels the held landing. Seat choices; the
  brief did not say.
- **A kill's expansion is a snap, not a glide.** One counted kill moves every point of the ring 1 block in one tick,
  20 blocks per second, above `v_fast`. The brief caps only the fast close and the return. F4 draws it; Ben may want
  it eased.
- **Two checks the settings record adds:** `mageRingRadius < borderRadius`, and **`v_slow ≤ v_fast`** (otherwise the
  return's slow part would outrun the cap the brief requires of the return).
- **Fractional durations land on whole ticks.** The slow close starts on the first whole tick at which the fast
  close's progress has reached 1; the return's slow part ends on the first whole tick at which the radius is back at
  12. The return's fast part then replays the fast close's own tick positions in reverse, so the "visits the close's
  positions in reverse" property holds exactly. With the placeholders and d = 0, 18, 36 every duration is already
  whole (144, 216, 288 ticks).
- **The strike clock is the fight's tick**, a strike on every multiple of 40 from the fight's tick 0, not restarted per
  Mage cycle. The brief did not say which clock.
- **`mageDamageable` is advice to the wiring.** The machine cannot stop a hit; F6's damage gate (in our own pipeline,
  §4 i) must read it. A Mage death fed in during the fast close (an admin kill) is handled: the ring returns from where
  it stands.

### 6.2 F4 + F5 AS BUILT (`feat/fight-ring`, 2026-11-18): WHAT THE CODE SHOWED

*The fight file is `content/fights/dragon.yml`, the fixture `/rpg fight start | land <dx> <dz> | mage-die | stop`
(`paper/.../fight/`), its logic core's `RingFixture`. The gate is `docs/gates/GATE-fight-ring.md`, NOT RUN at this
writing.*

- **A `Double.MAX_VALUE` strike is finite on the path the fixture takes, and becomes INFINITY on the defence path.**
  MEASURED, not reasoned: `MaxValueStrikeTest` passes with `DefenseRule.BYPASSED`, and the same strike with
  `DefenseRule.APPLIES` (armour 40) reads `dealt Infinity`. `Defense.applyDefense` computes `damage × 100 / (100 +
  defence)`, and `MAX_VALUE × 100` overflows before the division. **So if Ben rules that armour reduces the lightning
  (GATE-fight-ring.md BQ5), the ramp's saturation must come down to `MAX_VALUE / 100`, or the curve must divide first
  (`damage × (100 / (100 + d))`; that reorders a floating-point product every hit in the game reaches, so it is not done
  here).** A dev-only edge: a strike count of about 1014 in a row.
- **The forced ring costs 906 packets per redraw at radius 48, 4530 a second per player** (5 redraws a second; one
  `ClientboundLevelParticlesPacket` per point, READ in `CraftPlayer.spawnParticle`). cfde822's border sent 220 a redraw.
  Spacing, heights and period are content.
- **No PacketEvents for the forced send** (READ): `CraftPlayer.spawnParticle(..., force)` sets the packet's
  `overrideLimiter` to force and `alwaysShow` to false, and sends to that player with no distance check.
- **Folia, owed by the real fight:** the dev fixture reads participants' positions from C's region thread, safe on
  Paper only. The fight in its temporary world must read each player on his own scheduler.
- **Kill credit is `getKiller()`**: `MobDeathSystem` sets it for a player's store damage, vanilla for a vanilla kill. **A
  summon's kill is not credited** (cfde822 credited the summon's owner, §3 fact 21). Which kills count is still §7
  item 7.
- **`start` takes C from the command's source**, not the player's feet, so a gate row can put C 60 blocks away with
  `execute positioned` instead of teleporting the player into unknown ground. A seat choice.
- **Lines the brief did not ask for, added so rows read exact ticks:** `FIGHTPHASE` (each change, on its own tick, a
  landing's and a death's included), `FIGHTKILL` (radius before and after), and RINGTICK's `points=` and `viewers=`.
- **Every selftest run's end stops a fixture a row left running** (one `SELFTEST RUN fight-fixture …` line), so an
  aborted row cannot leave lightning striking the player; plugin disable stops it too.

### 6.3 F6 + F7 AS BUILT (`feat/fight-mage`, 2026-11-18): WHAT THE CODE SHOWED

*The Mage is `content/mobs/dark_mage.yml`, his gate core's `DamageGate`, the crystals the fixture's port. The gate is
`docs/gates/GATE-fight-mage.md`, NOT RUN at this writing.*

- **The gate has ONE custom choke point and ONE vanilla one, and both are needed.**
  - The custom one: every route to a mob's custom health ends in `BukkitCombatant.Handle.applyDamage` (a read-only
    survey of every caller), and the gate is asked there before the store.
  - Why the vanilla one too: `/kill`, the void and **mob-on-mob melee never reach the store** (`VanillaDamagePolicy`
    passes `KILL` and `VOID`; no handler reroutes a mob's melee on a mob). So `RpgListeners.onGatedDamage` cancels any
    vanilla damage on him at LOWEST.
  - **While he IS damageable, a mob's melee on him lands on vanilla health, not the store.** That is UNVERIFIED in game
    and a hazard for when the fight has mobs near him.
- **Knockback cannot be stopped by the attribute alone.** Our own pushes (`applyKnockback`, `applyImpulse`, pulls) set
  a velocity and ignore knockback resistance. So the fixture **pins** the Mage's x and z each tick. The real fight
  needs the same pin, or an "anchored" check at every push site.
- **"Stationary" removes every goal** (Paper's `MobGoals.removeAllGoals`, read) and keeps AI on, so gravity acts.
  `NoAI` was not used: it would have to be READ whether it stops gravity, and goals-off needs no such question.
- **Non-persistence, READ:** `CraftEntity.setPersistent` writes `Entity.persist`, and `Entity.saveAsPassenger` writes
  nothing when it is false (its force flag aside). The Mage and every crystal are not persisted.
- **A closed ring puts its crystals out.** `MageCycle`'s CLOSED snapshot reports 0 crystals, and the fixture follows
  the snapshot. POWERED keeps them. **A seat choice; the brief named only POWERED.**
- **LINGER cannot see a crystal**, which is not a projectile, a cloud or a marker. The gate's witness is a
  PROBE_ENTITIES of `end_crystal` within 48 at each row's end; a spot is 41.8 from C.
- **The selftest's ATTACK reaches only 16 blocks**, and every crystal is further. MG7 hits one with vanilla's
  `/damage … player_attack by @s` instead.
- **POWERED needs fifteen spaced kills at Ben's numbers.** The slow close lasts 400 ticks against 800 for ten crystals,
  so the ring must gain 12 blocks or more. MG8 stages it and shows the arithmetic.
- **The play row cannot promise "cannot die" by healing alone.** A sixth consecutive strike (640 before armour)
  outruns any heal. So the fixture's play mode stops after a third consecutive strike: 80 at most, each on a bar
  healed within 10 ticks.
- **The wall at Ben's four rows costs 6040 packets a second per player** at radius 48, under the 6500 line.
- **The armour path's overflow is guarded in the ramp** (`StrikeRamp.CEILING_DAMAGE` = `MAX_VALUE / 200`, executed
  finite at defences 0 to `MAX_VALUE`). `Defense.applyDefense` is not reordered.

---

### 6.4 F8 + F9 SPLIT (SEAT RULING, 2026-11-18): THREE PRs, TWO BOOTS

*The F8 + F9 brief stays the specification. CC measured, from the pinned jar, two facts that made it larger than one
step:*
- *A hit on a dragon PART reaches the dragon's own damage event, but the melee pre-attack event and vanilla projectiles
  hand our code the part, which is not a `LivingEntity`.*
- *In build 74, an API-created world is a DIMENSION inside the main save (`PaperWorldLoader.dimensionKey`). Its folder
  is therefore under `run/world/dimensions/`, so deleting one deletes inside the live save.*

| step | branch | what | boot |
|---|---|---|---|
| **F8a** | `feat/fight-site` | the brief's §0 docs, §1 tuning, §2 instrument fixes, §3 one runtime taking a SITE (the ordinary world its only site); the fast close re-asked with a floor staged | none of its own |
| **F8b** | `feat/fight-arena`, stacked on F8a | §4 the fight world, the generated arena, enter and leave, the return location, death, quit, the stale login, every ending, the orphan sweep (four refusals: the brief's three plus a real-path containment in the fight's own namespace), R0e; §5 i and j; ring, Mage and crystals, no dragon | **ONE boot, on F8b's head, covering BOTH gates** |
| **F9** | after both merge | §5 a to h, the dragon; Ben rows 5 to 8 | the second boot, confirmed by the seat first |

**Before any delete code in F8b,** READ from the jar:
- whether the main save records an API dimension outside its own folder;
- what the next boot does when that folder is missing;
- whether `unloadWorld` releases the region files.

If deleting cannot be shown safe, STOP.

### 6.5 F8a AS BUILT (`feat/fight-site`, 2026-11-18): WHAT THE CODE SHOWED

*The gate is `docs/gates/GATE-fight-site.md`, NOT RUN at this writing; read at F8b's one boot.*

- **One runtime, one site so far.**
  - `FightInstance` holds one fight's whole state and is the only place in `paper/` that builds the core fight
    (`new RingFixture(`) and its port (`FightSiteSourceTest`).
  - It takes a `FightSite`: world, C and its ground y, crystal spots, where the Mage may stand, who takes part.
  - `OrdinaryWorldSite` is the dev fixture's site.
  - `FightFixtures` holds fights by id, offers a death to every fight, and ends every fight at disable. The dev command
    drives one.
- **The wall at six rows costs 9060 packets a second per player at r = 48**, under the brief's 9,500 line by 440.
  **A seventh row would pass it** (10,570).
- **240 ticks a crystal against a 400-tick slow close means at most ONE crystal without kills.**
  - So ten crystals, POWERED, needs the ring held open for 2400 ticks: in the gate, 68 kills 35 ticks apart.
  - In play it means a group that does not kill sees at most one crystal before the ring closes on them.
  - **That is a design consequence of Ben's two numbers taken together, for him to see, not a defect.**
- **MG2's driven attack was the instrument, not the gate.**
  - READ: `Player.attack` raises no damage event when the attacker's attack damage and enchanted bonus are both 0.
  - READ: `WeaponItems` pins a non-melee weapon's attack damage to 0.
  - INFERRED: Ben held his staff at MG2.
  - ATTACK now logs `attack_damage=` and names a zero; MG2R2 holds `emberblade`.
- **The crystal listener is reachable, but only past the invulnerable flag.**
  - READ: `EndCrystal.hurtServer` returns on `isInvulnerableToBase` before the event.
  - Only `out_of_world`, `generic_kill` (build 74's `bypasses_invulnerability` tag) or a creative player's hit reach it.
  - It is KEPT for those, and MG7R2 reaches it.
  - **`/kill` removes a fight crystal anyway** (READ, `EndCrystal.kill` → `Entity.kill`, no damage event). That is an
    op command, recorded rather than guarded.
- **The entity probe counted dying bodies.** READ: a dead mob stays in the world for 20 ticks. It now leaves them out
  and says how many (`dying=`).
- **`/rpg mobdamage` printed an estimate made before the hit landed.** It now replies from the store 2 ticks later.
- **AWAIT_FIGHT** ends a wait when the dev command's fight ends, with a ceiling.

### 6.6 F8b AS BUILT (`feat/fight-arena`, stacked on F8a, 2026-11-18): WHAT THE CODE SHOWED

*The gate is `docs/gates/GATE-fight-arena.md`, NOT RUN at this writing. It carries the one R0 and run sheet for F8a's
gate too.*

- **Deleting a fight world was shown safe BEFORE any delete code was written** (the seat's condition). Each answer is
  marked in the gate:
  - The world's data lives in its own folder, `dimensions/rpgfight/fight_<hex>`.
  - The boot loads only the stem registry's worlds.
  - `unloadWorld` closes every region file synchronously.
- **Every delete passes one judge, `FightFolders` (core), with four refusals**, and logs its absolute path first. The
  symlink arm of the fourth is skipped on this machine: this Windows account cannot make a symbolic link.
- **The return location is a nullable `FightReturn` on `PlayerProfile`** (C5), with no schema bump, for
  `lifetimeXp`'s reason. A reflection test pins that every `with…` keeps it.
- **CC'S CHOICES, FOR BEN** (not in the brief):
  - **KEEP_INVENTORY in a fight world:** its drops would be deleted with it.
  - **IMMEDIATE_RESPAWN:** a death sends him home with no death screen.
  - **Entry at the centre.**
- **A death retires a player's own scheduled tasks.** So the selftest's `await_fight` now polls on the global scheduler,
  and the arena rows that kill him die inside one. `Scheduler` gained `onGlobalLater`: Bukkit's scheduler is banned, and
  an unload has no region to own it.
- **POWERED's kill lands a tick after it is dealt** (`applyDamage` is deferred). So where failure kills, the fight ends
  through "nobody inside", never at the outcome, or he would be returned home and die there.
- **The stale login is caught once his profile settles.** Where vanilla puts a player whose saved world is gone is
  UNVERIFIED in the jar read here, and **AQ1's `at=` is its first reading.**
- **The template-file arena (C3) is not built:** there is no file to test it. That is a deviation from C3, owed when
  Ben builds an arena.
- **The border lightning already strikes from the fight's first tick** (as the F4 fixture always did). AE3 reads it.

### 6.7 THE SELFTEST IN A FIGHT WORLD AND THROUGH A DEATH (the seat's ruling 2b, 2026-11-18): EVERY STEP, READ

*The first boot's arena run aborted inside AE1 three times: "the player left the dev world". The seat ruled: fix the
guard, and do not stop at the guard. Below is every selftest step and end-of-row routine that assumes the run's world
or a living player, read in `SelfTestRun`, `SelfTestSnapshot`, `SelfTestDrivers` and `SelfTestStaging` on
`feat/fight-arena`.*

| step or routine | what it reads or assumes | when he is in a fight world | when he has just died and respawned | done |
|---|---|---|---|---|
| **the guard**, `invalidated` before every pass | online, alive, in the run's world, one player online | **ABORTED the run** ("left the dev world") | "the player died", unless the row is `death_rules`: `advance` goes to the respawn wait first | **FIXED:** `SelfTestStaging.invalid` accepts the run's world OR a fight world, and still refuses any other; walked by `SelfTestArenaGuardTest` |
| **the "player died" check** (AE4, AE5, AE6, BA3) | `advance`: `waitForRespawn(death_rules, dead)`, then `awaitRespawn` on the region scheduler at a location | n/a | `awaitRespawn` polled at the DEAD PLAYER'S location, a fight world that may be unloading | **FIXED:** both respawn waits (`advance` and `awaitFight`) poll at the run's start |
| **`wait`** | `onEntityLater(player)` | runs | **a death retires his own tasks: a death DURING a wait would hang the run** | **Staged around:** every arena death happens inside `await_fight`, which polls on the global scheduler (F8b). No arena row dies during a `wait` |
| **`await_fight`** | the global scheduler; dead at the end → the respawn wait | runs | handled | already F8b's |
| **`save_blocks` and its restore** | blocks round his feet, in HIS world; written back at the next row's clean and at the run's end | would save fight-world blocks, and the restore would write them into a deleted world | n/a | **FIXED:** refused by name in a fight world (the row SKIPs); no arena row uses it |
| **CLEAN** (row start) | `teleportAsync` to the run's start, then the saved blocks back, the tagged mobs removed near him, the inventory at a gate boundary | he is home: every arena row ends with the fight over, and the teleport runs FIRST anyway | his respawn put him at his return location, beside the start | none |
| **LEFTOVER** | `player.getNearbyEntities`, after the CLEAN's teleport | reads the run's world | the same | none |
| **LINGER** | `start.getWorld()`, round the start | the run's world | the same | none |
| **RESTORED** (the run's end) | the snapshot: inventory, XP, cell, game mode, held slot, mana, his position (the start), and the RUN's world's rules and difficulty | if he is still inside: restored to the start, then `stopFight` ends the arena fight, which returns him to his return location, unloads and deletes | n/a | none |
| **the difficulty restore** | the run's world only | the fight world has its own difficulty and rules (`FightWorlds.create` sets the fight's own) and is deleted at the end | n/a | none; recorded |
| **`death_rules`** | KEEP_INVENTORY and IMMEDIATE_RESPAWN on the RUN's world | **does not reach the fight world**: the fight world sets both itself (`FightWorlds.create`, CC's choice for Ben) | which world's IMMEDIATE_RESPAWN a death reads is INFERRED to be the death's world | none |
| **`probe_entities`** | `player.getNearbyEntities`, in HIS world | reads the fight world | after the respawn, the run's world | none: every arena row's end probes run after he is home |
| **`probe_blocks`** | a box round his block, in HIS world | reads the fight world (AE1R2's floor and pillar probes: intended) | n/a | none |
| **`ask`** | chat; a timeout on his own scheduler | runs | n/a (the BA rows ask before or after the fight, alive) | none |
| **the HURT line** | the damage tap; the dealer looked up in HIS world | finds a dealer in the fight world | n/a | none |
| **the run's end while he is inside** | `end` → restore → `stopFight` | he ends at his return location, and the world goes | n/a | none |

### 6.8 F9 AS BUILT (`feat/fight-dragon`, 2026-11-18): THE DRAGON, WHAT THE CODE AND THE JAR SHOWED

**Every server fact is from the pinned server jar, build 74 (`Implementation-Version: 26.1.2-74-e4e17fc`, matching
`pom.xml`'s `26.1.2.build.74-stable`), by `javap -c -p`, or from its data files.** Each is marked READ, INFERRED or
UNVERIFIED. The research agent's full notes, with the quoted instructions, were the session's working file. The
decisive lines are quoted here.

**WHAT THE CODE SHOWED THE BRIEF HAD WRONG, OR DID NOT SAY:**

1. **A plugin-spawned dragon never moves.**
   - READ: `EnderDragonPhaseManager.<init>` sets `HOVERING`, and `EnderDragon.aiStep` skips all movement while the
     phase is `HOVERING` (`getstatic EnderDragonPhase.HOVERING` / `if_acmpeq` past the movement block). Only vanilla's
     own `EnderDragonFight` moves a new dragon on.
   - So the fight sets `CIRCLING` (Bukkit's name for `HOLDING_PATTERN`) at the spawn. Without it, "fly and perch" would
     have read as a dragon hanging still.
2. **The fireball row cannot be staged by the API.**
   - READ: only `DragonStrafePlayerPhase` raises `EnderDragonShootFireballEvent`. Its `begin()` nulls the target, and only
     the holding pattern sets one.
   - With no target, the next tick logs `Skipping player strafe phase because no player was found` (a vanilla `warn`)
     and returns to the holding pattern.
   - DA6 reads that. The cancel is witnessed only by fireballs the dragon shoots on its own (DB1, DB2).
3. **A hit on a part only reaches the dragon from a player, or from a damage type in
   `always_hurts_ender_dragons`.** READ: `EnderDragon.hurt(level, part, source, amount)` calls `reallyHurt` only if
   `source.getEntity() instanceof Player` or the type is in that tag; anything else is dropped with no event. Our
   abilities never take this path (they write the store directly); a player's melee does.
4. **Melee on a part landed nothing before this step.** READ, our source: `onPrePlayerAttack` returned on
   `!(getAttacked() instanceof LivingEntity)`, and a part is not one. The damage event that follows is raised on the
   dragon, found no recorded swing, and landed only the 0.01 token. Fixed: `LiveTargets.body`.
5. **Our ray could miss a part outside the dragon's own box.** INFERRED from READ geometry:
   - while sitting, the head's box is y−1 to y, wholly below the dragon's box (y to y+8);
   - tail3's edge can reach 8.5 along an axis, against the dragon box's 8.0.

   READ: Bukkit's `rayTraceEntities` and `getNearbyEntities` include the parts (`Level.getEntities` adds them through
   `PaperHooks.addToGetEntities`), each tested against its own box. Our filters dropped them as not living. Fixed:
   the filters and the hit resolve a part to its dragon.
6. **Vanilla's per-part scaling never reaches our number.**
   - READ: on any part but the head, the amount becomes `amount/4 + min(amount, 1)`.
   - It acts on the amount vanilla passes to `reallyHurt`. Our melee sets that to the 0.01 token, and lands its own
     figure in the store; abilities never call it.
   - A plain vanilla arrow, which none of our weapons fires, is rerouted from vanilla's `event.getDamage()`, already
     scaled, so the scaling is applied once, by vanilla.
   - **Nothing is bypassed: the scaling is not applied twice anywhere.** The seat's rule, "our store's number is the
     number", holds for every weapon we ship.
7. **A perched dragon takes 0 from an arrow.** READ: `AbstractDragonSittingPhase.onHurt` zeroes damage whose direct
   entity is an arrow or a wind charge, and sets an arrow alight. Our ranged weapons are rays and marker bodies, so
   this does not reach them. Recorded for Ben's DB rows.
8. **The XP.** READ: `getExpReward` is 500 with no vanilla fight and `mob_drops` on. READ (agent):
   `tickDeath` spends the `expToDrop` set from the death event's dropped XP, so `setDroppedExp(0)` suppresses it. Our
   death path is `setHealth(0)`, so vanilla's 200-tick death animation still plays; the fight removes the dragon at
   the win's return.
9. **The crystals heal the dragon's VANILLA health only.**
   - READ: any End Crystal within 32 of the dragon heals 1 every 10 ticks, raising `EntityRegainHealthEvent`
     `ENDER_CRYSTAL`.
   - READ, our source: `VanillaHealPolicy` acts only on tracked players.
   - So the store is never healed, and the vanilla mirror resets vanilla's health at the next store write.
   - INFERRED: the client draws a beam from a crystal to the dragon when it is near one.

   > **CORRECTED 2026-11-19, BY THE BOOT (`GATE-fight-dragon.md`, findings 1): ITEM 9 IS WRONG.** The store IS healed.
   > `VanillaHealPolicy` is the player arm; the mob arm, `MobHealPolicy` (M15), maps `ENDER_CRYSTAL` to REROUTE, and
   > `healTrackedMob` heals the store by `DamageScale.toCustom`: 1 / 200 × 20000 = 100 a heal. Read at the boot:
   > +1400 in DB1, and +100 and +300 in DB2, each while the fight's own crystal was lit. The item is left as written.
   > The miss was reading one of two arms.
10. **The fight world does not grief.** READ: `checkWalls` breaks blocks only with `mob_griefing` on, and the fight
    world sets it off.

**WHAT THE DRAGON'S ATTACKS DO TO A PLAYER TODAY, THROUGH OUR PIPELINE (the brief's 3 g; NOTHING CHANGED):**

| attack | vanilla (READ) | through our pipeline |
|---|---|---|
| head and neck | 10, `mob_attack`, damager the dragon, when the dragon's `hurtTime` is 0 | **0 to custom health** (INFERRED). READ: the dragon has no `ATTACK_DAMAGE` attribute (`EnderDragon.createAttributes` adds only `MAX_HEALTH 200` and `CAMERA_DISTANCE 16` to the mob defaults), and our seed is 0.0 without one. Vanilla's 10 is tokened by the mob-melee rider |
| wings | 5, the same source, only while not sitting and not hit by a mob in the last 2 ticks; push `dx/d·4, 0.2, dz/d·4` | **0 to custom health** (INFERRED, as above); **the push is vanilla's** |
| perched breath | a cloud of radius 5 for about 190 ticks, Instant Damage level 0 (3 a hit, every 20 ticks), `indirect_magic`, direct entity the cloud, causing entity the dragon | **UNKNOWN, to be read in DB1 and DB2.** INFERRED: Bukkit names it `ENTITY_ATTACK` with the cloud as damager, so our mob rider (damager not living) does not take it, and `VanillaDamagePolicy` passes `ENTITY_ATTACK` |
| charge | no damage of its own | nothing |
| fireball | its cloud: radius 3 growing to 7, Instant Damage level 1 (6 a hit) | **never shot**: cancelled for the fight's dragon |

**THE SKY (the brief's section 2; READ ONLY, NOTHING BUILT). Ben: "I wonder if it's possible to have it be like in
the end".**
- READ: in 26.1 a dimension type carries `skybox` (`none`, `overworld`, `end`) and visual `attributes`. There is no
  `effects` field.
  - `the_end.json` reads `"skybox": "end"`, `"has_ender_dragon_fight": true`, sky `#000000`, fog `#181318`.
  - The login and respawn packets name the type by registry id; its fields reach the client through registry sync.
    No packet sets them live.
- READ: `WorldCreator`'s environment chooses one of the three vanilla stems and nothing else.
- READ: `ServerLevel.<init>` builds vanilla's `EnderDragonFight` exactly when the type's `has_ender_dragon_fight` is
  true.
- READ: the Paper API has no runtime registry for dimension types; a datapack dimension is loaded as a world at
  startup.

| option | what Ben would see | what it costs | marks |
|---|---|---|---|
| **(a)** the fight's world made an END world | the real End sky | vanilla's own dragon fight starts there: its boss bar, a bedrock exit portal at the centre, and it adopts our dragon or spawns a second. The world is 0 to 256 high | the trigger, the height and the sky READ; what is drawn INFERRED |
| **(b)** a datapack world type with the End's sky and no fight | the End sky, no vanilla fight | **a server restart** to add or change it; the arena becomes ONE permanent world loaded at startup, not made and deleted per fight; our world creation cannot reach it | the loader and the API READ; the restart and the loss of per-fight worlds INFERRED |
| **(c)** each player's view changed by packet | possibly the End sky | high risk: the End is 256 high and the world 384, so his chunks would be misread; against the project's packet rule | the packet READ; the breakage INFERRED |
| **(d)** night forced and a dark biome, through the API | a dark sky, **not** the End's | cheap | UNVERIFIED |

**CC's reading, for Ben to decide:** (b) is the only one that gives the End's sky without vanilla's fight. It
changes how the arena exists: one world kept, not one made per fight. So it is a design decision, not a build step.

> **SUPERSEDED IN PART 2026-11-19 by §6.9:** a per-world Paper switch makes (a)'s vanilla fight inert, so (b) is no
> longer the only way to the End's sky without vanilla's fight. Left as written.

### 6.9 "CAN WE NOT JUST SET THE ARENA TO BE IN THE END?" — THE END OPTIONS, READ (the seat's brief, 2026-11-19; NOTHING BUILT)

Ben, verbatim: **"Can we not just set the arena to be in the end?"** and **"I mean a temp world of the end type, and If
that doesn't work, could we set the biome of the temp world to the end biome?"** Read from the pinned server jar
(`paper-26.1.2.jar`, build 74) and the data it ships. Each fact is marked. **No look is CONFIRMED until Ben sees it at
a boot.**

**1. What vanilla's fight does in an API-created END world, and when.**
- **On creation.**
  - READ: `CraftServer.createWorld` maps `Environment.THE_END` to `LevelStem.END`, so the world gets the `the_end`
    dimension type (`has_ender_dragon_fight: true`, `skybox: end`, `min_y 0`, `height 256`).
  - READ: `ServerLevel.<init>` checks `hasEnderDragonFight()` and then
    `getDataStorage().computeIfAbsent(EnderDragonFight.TYPE)` (a saved record in the world's data). It calls
    `init(level, seed, BlockPos.ZERO)`.
  - READ: a fresh record is `createDefault()`, which gives `needsStateScanning=true`, `dragonKilled=false` and
    `previouslyKilled=false`. `SavedDataStorage.set` marks it dirty.
  - READ: `init` builds a PINK boss bar ("Ender Dragon", boss music, world fog). It makes players within 192 of
    (0, 128, 0) valid, and shuffles 20 gateway slots.
  - READ: Paper's step inside `init`: when the record is dirty and `entities.spawning.scan-for-legacy-ender-dragon` is
    **false**, it sets `needsStateScanning=false` and **`dragonKilled=true`**. The default is `true`
    (`paper-world-defaults.yml` line 188).
- **Each tick** (`EnderDragonFight.tick`, READ):
  - It sets the bar visible as `!dragonKilled`, and every 20 ticks puts valid players on the bar.
  - While the bar has players:
    - it holds a radius-9 chunk ticket at chunk (0, 0);
    - once the arena is loaded, it runs `scanState` (if `needsStateScanning`) and the respawn stage (if any);
    - if `!dragonKilled`, it calls `findOrCreateDragon` when it knows no dragon or has not seen it for 1200 ticks, and
      counts crystals every 100 ticks.
- **On the first player's arrival** (`scanState`, READ; it logs `Scanning for legacy world dragon fight...`):
  - With no exit portal it **builds one**: `spawnExitPortal(false)`, at the top block over (0, 0). That is our arena's
    centre.
  - If a dragon exists it **takes that dragon's UUID** (`Found that there's a dragon still alive`). If none exists, the
    next tick `createNewDragon` spawns one at (0, 128, 0) in HOLDING_PATTERN and resets the spike crystals.
- **Respawn sequence:** only through `tryRespawn` (crystals placed on an exit portal), or the API's
  `initiateRespawn`. Not on its own (READ).

**2. Every way to make it inert or absent, and what each leaves behind.**

| way | does it work | what it leaves | marks |
|---|---|---|---|
| **A per-world `paper-world.yml` with `entities.spawning.scan-for-legacy-ender-dragon: false`**, written into the new world's folder before `createWorld` | **Inert.** `dragonKilled=true` from `init`: no scan, no exit portal, no dragon created, nothing adopted (`dragonUUID` stays null), the bar invisible | the record (deleted with the world); an INVISIBLE bar that still collects players; the radius-9 ticket at chunk (0, 0) while players are near (our arena is there anyway); **our End crystals set fire on their own block** (READ: `EndCrystal.tick` places fire whenever the level has a fight object) | the `init` branch READ. Paper loads `<the world's dimension path>/paper-world.yml` (READ, `PaperConfigurations`), but that it is read for our worlds at creation is **UNVERIFIED**. The witness: no `Scanning for legacy world dragon fight...` line in the log |
| **Paper's `DragonBattle` API** | **No.** It has no disable. `setPreviouslyKilled(true)` stops only the egg. The bar is reset every tick (`setVisible(!dragonKilled)`) and re-filled every 20 | everything in section 1 | READ (API listing; `tick`) |
| **The global `paper-world-defaults.yml` switch** | works, but for **every** world, the survival End included | the survival End's own fight would stop scanning | READ |
| **`entities.behavior.should-remove-dragon: true`** | **No:** it *discards a found dragon* when there is no portal. **That would be OUR dragon** | a removed dragon | READ (`scanState`) |
| **A generator choice** | **No:** the fight is decided by the dimension type, not the generator | everything in section 1 | READ |
| **A datapack dimension type** with `has_ender_dragon_fight: false` | **Absent:** no fight object at all | nothing, but see (b) in §6.8: a restart, one permanent world | READ |
| `ServerLevel.setDragonFight(null)` | would remove it | **NMS, banned** | READ, not proposed |

**3. Is OUR dragon adopted?**
- **Yes, without the switch:** `scanState` or `findOrCreateDragon` takes our dragon's UUID. Our dragon's `aiStep` then
  attaches the level's fight to itself, because the UUIDs match (READ). From then on:
  - `updateDragon` drives the PINK bar from the dragon's **vanilla** health, a second bar beside our purple one;
  - its death runs `setDragonKilled`. INFERRED from vanilla: the exit portal is activated, the egg placed on the first
    kill, a gateway built;
  - `getExpReward` reads the fight (READ the call); our `setDroppedExp(0)` should still zero it (INFERRED).
  - Our CIRCLING and podium are left alone; the bar and the portal are what change.
- **With the switch: no** (`dragonUUID` null, so no match).

**4. What else the End changes that we rely on** (data READ from `the_end.json`, behaviour INFERRED unless marked).
- **The void:** `min_y 0` against the overworld's `-64`. Paper's `void-damage-min-build-height-offset: -64.0` puts void
  damage below **y −64**, not y −128: a fall off the disc (floor y 64) reaches the void about 64 blocks sooner.
- **Light:** `ambient_light 0.25`, `sky_light_factor 0`, sky-light colour `#ac60cd`: the End's flat, dim light.
  `has_fixed_time: true`, so the arena's `time 18000` does nothing.
- **Weather:** the End biome has no precipitation; the fight world already stops the weather.
- **Respawn rules:** `bed_rule` never sets a spawn and explodes. Respawn anchors don't work. Our fight world sets
  keep-inventory and immediate-respawn itself (`FightWorlds`, READ), and the return is ours, so nothing we use moves.
- **Endermen:** the End biome spawns them (`the_end.json`, weight 10). Our `spawn_mobs` / `spawn_monsters` off stop
  natural spawns.
- **Music:** `music.end` with `replace_current_music: true`.
- **NEW, NOT IN THE BRIEF: gear score.** READ: `MobOrigin.dimensionOf` maps `THE_END` to the End curve, measured from
  (0, 0). The dragon and the Mage are stamped GS 100 by the fight, so they don't move. Any unstamped mob in the fight
  world would (later waves: name it before they are built).
- **Height:** 0 to 256. The arena (floor 64, pillars to 76, crystals at 77, dragon at 85) fits.

**5. The real End (`world_the_end`) as the arena: what collides with C1 to C4** (`PLAN-dragon-mobs.md` §6).
- **C1, one temporary world per group:**
  - it is one shared, permanent world;
  - every group would fight in it at once;
  - it can't be made or deleted per fight, so the folder lifecycle, the orphan sweep and the leave and return go;
  - C1 forbids finding the fight by `THE_END` or by `getEnderDragonBattle()`, and fixed (0, 0) geometry.
- **C2, the fight doesn't care where it runs:**
  - the survival End is per-server shared state that players visit for progression;
  - its vanilla fight, exit portal, gateways and egg are theirs.
- **C3, the arena is a template:**
  - (0, 0) holds vanilla's main island, obsidian spikes, crystals and exit portal;
  - C3 forbids vanilla End generation as the arena;
  - building our 30,615 blocks there would overwrite survival terrain.
- **C4, fight state lives in the fight:**
  - its cleanup would have to sweep a world it does not own;
  - turning off its game rules (`spawn_mobs`, `mob_griefing`) changes them for every survival player.
- Also: the End's gear-score curve applies to everything there, and vanilla's real fight would adopt or fight beside
  ours. **The seat doesn't recommend it, and nothing here argues for it.**

**6. Ben's fallback: a NORMAL temporary world with the End biome.**
- **What decides the sky, the fog, the sun, moon and stars (READ):**
  - **the dimension type's `skybox`** (`none`, `overworld`, `end`) chooses the sky model: the End's textured sky
    against the overworld's sun, moon and stars;
  - it is a field of the dimension type only, and no biome carries it;
  - the colours and angles are **environment attributes**: `visual/sky_color`, `fog_color`, `star_brightness`,
    `sun_angle`, `moon_angle`, `sky_light_color`, `sky_light_factor`, `cloud_color` and others;
  - they are positional, so a biome may set them over the dimension's (READ: `plains.json` sets `sky_color`; each of
    these is built without `notPositional`).
- **The vanilla `the_end` biome sets NO attributes** (READ: `the_end.json` has no `attributes` block). In a NORMAL
  world it would change:
  - no rain or snow drawn;
  - its spawn list (Endermen, off for us);
  - **nothing in the sky: the overworld skybox, the overworld's colours, sun, moon, stars and clouds.**
  - INFERRED from the data. The client's renderer is not in the server jar, so how it draws is **UNVERIFIED**.
- **What it costs:**
  - the API call is `WorldCreator.biomeProvider(BiomeProvider)`, a provider returning `Biome.THE_END` at creation (READ
    the method). Our empty generator takes it, and a world made per fight needs **no chunk resend**;
  - setting a biome over an existing arena (`World.setBiome` per block, then `refreshChunk`) would need the resend
    (INFERRED). We never need it.
- **A CUSTOM datapack biome could carry End-like attributes:** sky `#000000`, fog `#181318`, `star_brightness 0`, the
  End's light colour.
  - That gives the End's colours over the overworld sky model: dark sky, no stars. The sun and moon are drawn unless
    their angles are moved (INFERRED).
  - It needs a datapack and **a restart** (the biome registry is loaded and synced at startup), but keeps one world per
    fight.

**THE RANKING FOR BEN** (closest to the End's look first; every look UNVERIFIED until his eyes):

| rank | option | how close | cost | risk |
|---|---|---|---|---|
| 1 | **A temporary END world plus the per-world switch** | **The real thing:** the End's sky model, colours, light and music | small. `environment(THE_END)` and one YAML file written before creation; no restart; still one world per fight | the switch's load for our worlds is UNVERIFIED (one log line witnesses it); crystal fire; void 64 higher; leftover invisible bar and ticket; unstamped mobs read the End curve |
| 2 | (b) a datapack dimension type (End sky, no fight) | the same look, no fight object at all | **a restart**; ONE permanent arena world, not one per fight | changes how the arena exists |
| 3 | a custom datapack biome with End colours, NORMAL world | the colours, not the sky model | a datapack and a restart; keeps one world per fight | the sun and moon still drawn |
| 4 | **Ben's fallback: the vanilla End biome** | **likely no visible change** | cheapest code: one `BiomeProvider` | it is likely to show nothing |
| 5 | (a) a temporary END world, no switch | the real look | `environment(THE_END)` only | vanilla adopts our dragon: a second (pink) bar, an exit portal built at the centre, the egg and a gateway at its death |
| — | (c) packets; the real End | — | — | refused: §6.8 (c), and section 5 |

**Cheapest to try first: rank 1.** It is the same `environment` change as rank 5 plus one file. Its failure is readable
on the log: `Scanning for legacy world dragon fight...`, or a second dragon, or a pink bar. Without the file, it falls
back to rank 5's behaviour, not to anything worse. Rank 4 is cheaper in code, but the data says it shows nothing.

### 6.10 F9b AND F9c AS BUILT (`feat/fight-rules`, `feat/fight-end`, 2026-11-19): WHAT THE CODE AND THE JAR SHOWED

The seat's split ruling of 2026-11-19: four PRs, two boots. F9b (sections 0, 1, 2, 4, 6, 7) and F9c (section 3) share
boot 1, whose one R0 and run sheet are `GATE-fight-end.md`'s. F9d and F9e (the respawn rules) are boot 2.

**THE C1 EXCEPTION, RECORDED (the seat's ruling, 2026-11-19).** `World.getEnderDragonBattle()` MAY be read:
- on the fight's own world handle;
- in the END world's self-check only (`FightWorlds.endSelfCheck`);
- to confirm vanilla's fight is inert.

It is never used to find, drive or identify our fight; C1 otherwise stands (`PLAN-dragon-mobs.md` §6, C1, carries a
pointer). `FightEndSourceTest` asserts one call in all of `paper`'s main code, there.

**What the code showed the brief got wrong or left open:**
1. **"Before the Mage lands" has no window in a fresh fight.** A crossing in IDLE lands its Mage on the next loop tick,
   so a hit between the two cannot be staged. RA1 stages the owed window where it really occurs: during the ring's
   return (Ben's DB2 case).
2. **Nothing could tell a bite from a wing by cause.** READ: both are `mobAttack(dragon)`. Which attack it was is read
   from the dragon's own part boxes, as the server chose its victims.
3. **The breath never reached our pipeline.** READ:
   - Bukkit names the cloud's hit `ENTITY_ATTACK` with the CLOUD as the damager;
   - the mob rider needs a living damager;
   - `VanillaDamagePolicy` passes `ENTITY_ATTACK`.

   So vanilla's own number landed and our store took nothing: F9's "the breath: NOT READ" had no HURT line to read.
4. **The Mage's death message credited the dragon because the dragon really hit him in vanilla.** Ours to fix,
   and fixed: the fight's dragon no longer hurts its own Mage in vanilla.
5. **An END world's self-check cannot be synchronous.** No API read distinguishes an inert fight from a live one
   until the fight has ticked once. So an END fight's first entrant enters 4 ticks after the world is made, not on
   `enter`'s tick.
6. **The marker is now written before the END world exists** (with the Paper file); the same file and the same text.
   The sweep's four tests are unchanged.

**Jar facts, each marked** (the pinned server jar, build 74, by `javap`):
- READ: `EnderDragon.aiStep`:
  - the wings' boxes, inflated (4, 2, 4) and moved down 2, push, and (not sitting) hurt for 5;
  - the head's and the neck's boxes, inflated 1, hurt for 10;
  - all of it only when the dragon's `hurtTime` is 0.
- READ: `EnderDragon.checkCrystals`:
  - searches its box inflated by 32 for crystals;
  - tries a heal every 10 ticks through `EntityRegainHealthEvent(ENDER_CRYSTAL)`;
  - calls `setHealth` only if that event is not cancelled.
- READ: `DragonSittingFlamingPhase`:
  - at its 10th tick, a cloud owned by the dragon on the floor below a point 5 ahead of the head;
  - radius 5, 200 ticks, Instant Damage, potion duration scale 0.25;
  - Paper's `EnderDragonFlameEvent`.
- READ: `AreaEffectCloud`'s defaults are a reapplication delay of 20 and a wait of 20.
- READ: `CraftEventFactory.handleEntityDamageEvent` names a hit with a damager and no mapped type `ENTITY_ATTACK`.
  That the breath's type is indirect magic is INFERRED.
- READ: the API's `EnderDragon.Phase` maps to vanilla's by order. `BREATH_ATTACK` is `SITTING_FLAMING`.
- READ: `CraftWorld.strikeLightningEffect` makes a visual-only bolt; `LightningBolt.tick` and `spawnFire` skip a
  visual-only bolt's damage and fire.
- READ: `AABB.intersects` is strict.
- READ: `ServerLevel.<init>`:
  - the world's Paper config is made in the `Level` constructor (offset 155);
  - vanilla's fight is built at offset 921, only if the type's `has_ender_dragon_fight`.
- READ: `EnderDragonFight`:
  - `init` takes Paper's branch on a fresh (dirty) record with `scan-for-legacy-ender-dragon` false, setting
    `dragonKilled` true and `needsStateScanning` false;
  - `tick` first sets its bar visible as `!dragonKilled`.
- READ: `SavedDataStorage.computeIfAbsent` marks a fresh record dirty.
- READ: `ServerLevel.tick` ticks the fight while `emptyTime` is under 300. A new world's chunk source has no tickets,
  so it ticks from the start.
- READ: `CraftDragonBattle`:
  - `getEnderDragon` looks up the fight's `dragonUUID`;
  - `getEndPortalLocation` returns its `exitPortalLocation`;
  - `getBossBar` wraps its `dragonEvent`.
  - What `getEntity(null)` does is not READ, so a throw there reads as not inert.
- READ: `EndCrystal.tick` sets fire on its block in a level with a fight object, after a `BlockIgniteEvent` naming
  the crystal.
- READ: `DimensionType.getStorageFolder` is `<level>/dimensions/<ns>/<path>`.
- READ: `PaperConfigurations` reads `<that>/paper-world.yml`. **UNVERIFIED until the boot's `end-check` line: that it
  reads OUR file for OUR world.**
- INFERRED: a fight dragon's hit reaches the event at Normal difficulty unscaled (10 and 5). The boot reads `vanilla=`.

## 7. OPEN, FOR BEN

*Listed, not answered.*

1. **Crystals as an immunity gate** on the dragon, separate from the Mage (Ben: "Not sure yet").
2. **Crystal pace ANSWERED 2026-11-18** (ninth, answer 9): "no make it 12 seconds each crystal". The count stays 10.
   *(Asked as:)* **Crystal count and pace** during the slow close.
3. **ANSWERED 2026-11-18** (eighth message): "Let's lower it to 3.8 blocks a second to allow players to react and
   catch up when struct". *(Asked as:)* **Fast-close speed** `v_fast` in blocks per second ("Slightly slower than a
   player sprinting").
4. **ANSWERED 2026-11-18** (seventh, answer 3): "I think it should be a little faster. Maybe .6 blocks per second".
   *(Asked as:)* **Slow-close speed** `v_slow`.
5. **ANSWERED 2026-11-18** (sixth message): "Lightning ramping damage numbers can start at 20, 40, 80 and keeps
   doubling". *(Asked as:)* **The lightning ramp's numbers**: the first strike's damage, the step, any cap.
6. **The Mage's health: ANSWERED again 2026-11-18** (tenth message, note 1): "Change the Mage health to 6,000",
   replacing the eighth's "1500 works for now". **Still open: where
   he may land** (the seat's nesting bound, §2: no more than 36 blocks from the
   centre, or the ring sweeps outward over a standing player).
7. **Expansion per kill** `e_kill`. *(Seat-added: which deaths count, any mob or the fight's own, and a summon's kill
   for its owner, as cfde822 did, §3 fact 21.)*
8. **The wave tables** (Ben: "we will rework it entirely").
9. **Endermen growing Phages** (§2 f): intended or not.
10. **Entry, group size, and death mid-fight** (Ben: "not sure yet"). **The dev stand-in ANSWERED 2026-11-18**
    (tenth, answer 4): "yes". A dev command enters; a death returns you to where you entered; the fight fails once
    nobody is alive inside. The real entry rules and group size are still open.
11. **The dragon's health ANSWERED 2026-11-18** (tenth, answer 2): "20,000". *(Asked as:)* "for now it stays vanilla
    with increased health": how much. **Per-player scaling is still OPEN.**
12. **Dialogue** ("Likely keep it": as is, or rewritten).
13. *(Seat-added.)* **The return's slow part:** does it run back at `v_slow`, or faster? *(A seat placeholder runs it
    at `v_slow` until Ben sets it.)*
14. **ANSWERED 2026-11-18** (sixth message): "I would like to keep particles. We can try forced particles if that
    helps. I don't care about the minimal particle setting." *(Asked as:)* *(Seat-added, from §4.)* **How the ring
    is drawn:** display entities (round, independent of particle settings),
    particles through PacketEvents with `alwaysShow`, or a world border (square). §4 f.
15. *(Seat-added, from §4, a FLAG, not a choice.)* **The dragon orbits (0, 0) of its world**, so the arena's
    template is centred there, which C1 otherwise forbids as fight geometry. One world per group keeps it safe; Ben
    to note it.
16. **Every fight question still open in `PLAN-dragon-mobs.md`**, QUESTIONS FOR BEN: **Q1** (fight-only mobs or
    world mobs too), **Q2** (the Knell's hit strength), **Q3** (mob numbers × gear score / 100), **Q9** (the
    fight's gear score), **Q11** (do the fight's mobs drop or pay), **Q12** (a restart mid-fight), **Q20** (a Void
    death during a Phage's fuse), **Q27** (split children against the fight's caps), **Q29** (growing in too small
    a space), **Q30** (a newborn's moment of no growth), **Q31** (is the blast Void damage), **Q32** (does a shield
    block the blast), **Q34** (does cover protect from the blast).
    **Q10 (the mobs against Endermen) is ANSWERED by the first message, answer 6:** "drop the war, the enderman are
    now the main fodder enemy".
17. *(Seat-added, 2026-11-18.)* **Paper 26.3**, a separate decision: the upgrade procedure is Ben's to start
    (§1, tenth message).
18. **When waves start, PARTLY ANSWERED 2026-11-21, and waves are NOT started.** Ben, verbatim: "Channelling is when
    the slow close starts but I don't want to start waves yet". Recorded on `feat/crystal-shard-staff` from the
    seat's brief of that day; nothing is built from it. The wave tables (item 8) stay open.

---

## 8. THREE READ-ONLY REPORTS (the seat's queue, item 3, 2026-11-19; written on `feat/fight-template`; no code)

Each is a decision paper. **Nothing in this section is built.** Every jar fact is marked READ (javap on the pinned
`paper-api` 26.1.2 build 74, or the server jar under `run/versions`), INFERRED, or an estimate (est.).

### 8.a A SECOND PLAYER FOR TESTING

**What is unread on a server, because every boot so far had one player:**
- reviving a real player (F9e);
- ALL DOWN with two or more (F9d);
- a group's entry and ring;
- anything one player does to another.

The selftest's guard aborts any run with `onlinePlayers != 1` (READ, `SelfTestStaging.invalid`).

| option | what it can witness | what it cannot | cost |
|---|---|---|---|
| **1. A second real account on Ben's LAN** (a second Minecraft licence, a second machine or client) | everything a player does: a real client's sneak, its screen, its packets, its rendering | nothing on the server side is out of reach | a second licence. Someone has to drive it, so it is a two-person boot or Ben alternating windows. The server stays online-mode |
| **2. A headless bot client** (a scripted client process, e.g. a Node or Python Minecraft client library, outside this repo) | the server path end to end: it joins, enters, sneaks and moves by script, so a revive and all-down are read from real packets | what a person sees and feels. The bot must speak 26.1's protocol (INFERRED: these libraries lag new versions, as PacketEvents does) | **a new tool and a new language, and it needs `online-mode=false` or a second licence.** Offline mode on a LAN-only dev server is a security change to say aloud, not to slip in |
| **3. A stand-in the server drives** | — | **READ: build 74 has no fake-player API.** `org.bukkit.entity.NPC` is an empty marker interface, and no class matches fake or bot. A `Mannequin` is not a `Player`, so it can never be a participant (`takesPart` takes a `Player`) | a new dependency (an NPC plugin that makes a server-side player) and its own fake-player model, which our code would treat as real in some places and not others. **Not recommended** |
| **4. Core tests only** (today) | the rules, with two and three players (`DownRosterTest`, `ReviveTrackerTest`) | the wiring on a server | nothing more |

**What the guard would need, for options 1 and 2:**
- `invalid(...)`'s `onlinePlayers != 1` becomes "exactly the run's player plus the declared second, by name", taken
  from a new run argument (`/rpg selftest <gate> with <name>`);
- a row's steps name which player they drive (`cmd_as <name>`, or the bot's own script).

That is one PR of selftest work, and a gate of its own.

**Recommendation: option 1, a second real account, for the first multiplayer boot.** It witnesses everything with no
new tool and no security change, and the selftest work is the same for either.
- Option 2 is worth it only if multiplayer rows become routine. Whether it is then is Ben's call.
- **Open for Ben:** whether a second account exists, or can.

### 8.b THE RING'S COST WITH A GROUP

**Today (the content's numbers):**
- 6 rows of 302 points = **1,812 particles a redraw**;
- a redraw every 4 ticks, so 5 a second: **9,060 particle packets a second per player**;
- each sent with force (READ, `BukkitFightPort.draw`, one `spawnParticle` per point per viewer).

**Per viewer, the ring is the same packets, so the cost is linear in viewers** (one ring, every viewer sent every
point):

| players | packets a second | bytes a second (est.: about 40 bytes a particle packet before compression) |
|---|---|---|
| 1 | 9,060 | about 362 KB/s |
| 2 | 18,120 | about 725 KB/s |
| 4 | 36,240 | about 1.45 MB/s |
| 8 | 72,480 | about 2.9 MB/s |

The packet counts are arithmetic on the content's numbers. **The byte figures are estimates, not measured**; a boot
reading the network (or Paper's own packet stats) would replace them.

**What is cheaper for the same look (options; none built):**
- **A bundle** (READ: `ClientboundBundlePacket` exists in the server jar). It groups one redraw's packets so the client
  handles them in one tick. INFERRED that it saves framing and ordering, not bytes: each particle is still its own
  packet inside it. Small win.
- **One packet with a count and a spread** (READ: `spawnParticle(…, count, offsetX, offsetY, offsetZ, extra, data,
  force)` and `ParticleBuilder`). INFERRED from vanilla: the spread is a random offset about one point, so it cannot
  draw a ring's exact points. **It changes the look**; not the same ring.
- **A different redraw rule for a ring that is not moving.** In IDLE and between closes the ring does not change, so it
  needs redrawing only as fast as a dust particle fades. INFERRED: a dust particle lives several ticks, so its
  lifetime would need a READ. Redrawing every 8 ticks instead of 4 while still would halve the cost then; a moving ring
  keeps 4.
- **Display entities instead of particles** (a ring of block or item displays). A still ring costs nothing per tick
  once sent; a moving one is a teleport per point.
  - That is a different look, and entities to clean up: the copy's exclusion work, again.
  - The biggest saving, and the most work.
- **Fewer points:** spacing 1.0 becomes 1.5, a third fewer. A look change, and Ben's.

**Recommendation: none yet.** The first group boot should measure first (a packet count per second at 2 players).
**Open for Ben:** whether any change to the look is acceptable.

### 8.c PAPER 26.3: WHAT MOVING FROM 26.1.2 BUILD 74 WOULD INVOLVE

**A decision paper, not a recommendation to do it now.**

**What Paper says about 26.3** (its announcement post, found by a web search on 2026-11-19; the full changelog was not
in the search results and **must be read from the post itself before deciding**):
- Most of the data component API is no longer marked experimental. That includes `ResolvableProfile`, which the copy
  uses.
- There are new events, `EntityBreakEvent` and `EntityBreakByEntityEvent`, and a new `Player#postEffects`.
- A config merge (Bukkit, Spigot and Paper options together) is deferred to 26.4.
- **Paper's own download page keeps 26.3 behind an "experimental builds" switch**, and its javadoc lists release
  candidates (26.3-rc-3).
- **Nothing found in it touches worlds, the dragon, mannequins, particles or the damage path specifically.** That is
  "not found in the summary", NOT "unchanged". Each needs reading in the full notes and in 26.3's jar.

**The jar facts this project rests on, which would each need re-reading on 26.3's jar.** By area, from this plan and
`PLAN-dragon-mobs.md`. `READ` appears 130 times in this plan and 147 in that one, so the list is by subject, not one
by one:
- **The dragon:**
  - `EnderDragon`'s parts, its `aiStep` hurts (head and neck every tick), `knockBack` and the wing condition;
  - its phases and `setPodium`;
  - the fireball;
  - `DragonSittingFlamingPhase`'s cloud;
  - `AreaEffectCloud.serverTick` and its apply event.
- **The fight world:**
  - the per-world `paper-world.yml` switch `scan-for-legacy-ender-dragon`;
  - `getEnderDragonBattle`;
  - an END-type world's fixed time and void line;
  - `WorldCreator`;
  - `unloadWorld` closing region files;
  - an API world at `dimensions/<ns>/<name>`.
- **Damage:**
  - `LivingEntity.hurtServer`'s invulnerability window and `lastHurt`;
  - `Player.hurtServer`'s difficulty scaling and its invulnerable check;
  - the `bypasses_invulnerability` tag;
  - knockback only after a successful hurt;
  - the death path (`ServerPlayer.die` on a cancelled `PlayerDeathEvent`).
- **End crystals:** `EndCrystal.tick`'s fire through `CraftEventFactory`, and the crystal heal.
- **Mannequins and structures:**
  - `Mannequin`'s API and `VALID_POSES` (SLEEPING);
  - `CraftStructure.fill` and `place`;
  - the structure block's 48 limit.
- **Particles and the HUD:** forced particles; the action bar and its holds.
- **Earlier slices' facts:** the selftest drivers (`Player#attack`, synthetic interact events, `performCommand`); the
  creative-divergence register; the Phage, the Knell, the Phalanx and the Skreel.

**The upgrade procedure** (`.claude/rules/upgrade-procedure.md`, the account):
0. Notice the release.
1. Check that PacketEvents supports 26.3; it is the gate, and it typically lags.
2. Bump `packetevents.version` first and build.
3. Bump `paper.version`, an exact build.
4. `./mvnw -pl core test`.
5. Boot and smoke-test one ability.

Then, for this project: re-read every fact above on the new jar, and re-boot every merged gate whose rows rest on one.
**That is most of the fight's gates.**

**What cannot be undone:**
- **A world opened on 26.3 is upgraded, and 26.1 will not load it again** (INFERRED: Minecraft's world format only
  goes forward). The backup before the first 26.3 boot is the only way back, so it must be taken and its counts read
  first.
- Player data upgraded the same way.
- Our own files (profiles, `arenas/*.nbt`) are ours. INFERRED unaffected, but a structure file saved on 26.3 carries a
  newer data version, which 26.1 may refuse.

**What it costs:** the PacketEvents wait (unknown), a build, and a re-read and re-boot cycle across the fight's gates.
Days of booting, not hours. **Open for Ben:** when, and whether before or after the fight's waves.

Sources (8.c): [papermc.io/news/26-3](https://papermc.io/news/26-3), [jd.papermc.io/paper/26.3](https://jd.papermc.io/paper/26.3/index.html).
