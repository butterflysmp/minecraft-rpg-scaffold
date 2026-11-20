# PLAN — THE GAME-WIDE DAMAGE RULES (THE WORLD HURTING A PLAYER)

**What this file is.** The rules for damage that comes from the WORLD rather than from a weapon, a mob or a fight: the
void today. **No plan held these before 2026-11-19.** The vanilla-damage boundary they extend is
`VanillaDamagePolicy` and `RpgListeners.onEnvironmentalDamage` (its account is in their javadoc). The values live in
`content/world_damage.yml`, read by `WorldDamageLoader`.

## 1. RULINGS

*a. Ben's ruling on the void (2026-11-19), verbatim. The seat had asked whether a player should go down as soon as he
is a set distance below the floor, suggesting 10 blocks:*

> "A player should lose 10% of their health each damage tick (0.5s) to the void regardless of their max health."

**SEAT READINGS, each the seat's (2026-11-19):**
- Each void damage tick takes 10% of the player's MAXIMUM health in our store, so the tenth tick takes the last of a
  full bar.
- It ignores defence and every mitigation (a share of maximum, not a hit). It has no element.
- It applies to PLAYERS in EVERY world, not only fight worlds. Mobs are unchanged.
- Where the void begins and how often it ticks stay the server's own.
- A DOWN player still takes nothing (F9d), and fix B's rule still holds.

(Recorded in `PLAN-dragon-fight.md` section 1 too, as its entry j, because the fight is where it was asked.)

## 2. WHAT WAS READ, EACH MARKED (2026-11-19; the pinned server jar `run/versions/26.1.2/paper-26.1.2.jar`, its manifest
`Implementation-Version: 26.1.2-74-e4e17fc`, the build every boot log names)

- **What damages an entity below a world's floor** (READ, `Entity.checkBelowWorld`, called from `Entity.baseTick` every
  tick): if the world's void damage is enabled and the entity's y is below `Level.getMinY()` plus the world's
  `void-damage-min-build-height-offset`, it calls `onBelowWorld()`. (A NETHER-only ceiling branch is separate and is not
  ours.) `LivingEntity.onBelowWorld` is `hurt(damageSources().fellOutOfWorld(), CraftWorld.getVoidDamageAmount())`.
- **The height and the amount** (READ, `run/config/paper-world-defaults.yml`): `void-damage-min-build-height-offset:
  -64.0`, `void-damage-amount: 4.0`. So the void begins 64 below the world's minimum y:
  - a NORMAL overworld (minimum y -64, the dimension type's): **y -128**;
  - an END-type world (minimum y 0): **y -64** (as F9c read it).
- **The interval: 10 ticks, 0.5 s, as Ben said** (READ, `LivingEntity.hurtServer`). `hurt` is called EVERY tick below the
  line, but a hit lands in full only when `invulnerableTime <= invulnerableDuration / 2`. `invulnerableDuration` is 20
  (`LivingEntity.<init>`), and a landed hit sets `invulnerableTime` to it. Inside that window a hit lands only if its
  amount exceeds `lastHurt`.
- **The cause Bukkit reports:** `DamageCause.VOID` (the source is `fellOutOfWorld`; `F9d`'s down line already read it,
  `cause=VOID`).
- **How it reached our store before this ruling: IT DID NOT** (READ, our source). `VanillaDamagePolicy` PASSes `VOID`,
  so vanilla's 4 landed on the VANILLA health bar, which for a tracked player is only a display: `HeartBarRenderer`
  rewrites it from the store on every store change, as `13 hearts` (26 points) at a maximum of 350
  (`HeartScale.heartCount`, READ).

## 3. WHY FD2 FELL 166 TICKS TO Y -671 BEFORE GOING DOWN (the boot of 2026-11-19 at `cb821675`)

- **READ, the log:** he was teleported to x 60, y -130 between t=16 and t=20, in an END world, so already 66 below its
  void line. Ring strikes hit him at t=40, 80, 120 and 160 (20, 40, 80, 160; 272.727 after defence), leaving 77.273 of
  350. He went down at t=186, `cause=VOID`, at y -671.72.
- **READ, our source:** the void's 4 a tick never touched the store (section 2). It only lowered the display bar, and the
  display was put back each time the store changed: on every strike, and on every regeneration payment (each second
  while he was below his maximum, `HealthRegenSystem`).
- **INFERRED, the timeline:** while his store was high, the display was rewritten to 18 to 26 points about every 20
  ticks, and the void's 8 points in those 20 ticks never emptied it. After the fourth strike the store was 77.273, which
  renders as about 5.7 points. Two void hits (8) emptied that before the next rewrite. Vanilla's own health then reached
  zero and raised the death F9d turned into a down, about 26 ticks after the fourth strike. **So the void "killed" only
  because the lightning had already emptied him.** With no strikes, a player at a full store would have fallen for as
  long as regeneration kept rewriting the bar: the void scaled with nothing and killed by accident.
- **What it means:** before this ruling, the void killed a tracked player through the display's arithmetic, not through
  any rule of ours. That is what Ben's ruling replaces.

## 4. AS BUILT (`feat/fight-template`, 2026-11-19)

- **Core `VoidShare.hitFor(max, current, share)`**: `max x share`, defence `BYPASSED`. If what would remain is under
  `1e-9` of the maximum, the tick takes everything left.
  - **EXECUTED:** naive subtraction reaches zero only on the ELEVENTH tick for 16,001 of the 39,999 half-step maxima from
    1 to 20,000 (the first is 1.0). `VoidShareTest` walks the whole range through the rule.
- **Content `world_damage.yml`:** `void.share_of_max_per_tick: 0.10` (Ben's). One `Loaded world damage` line; a
  `Refusing the void rule` line leaves the void the server's own.
- **Paper, `RpgListeners.onEnvironmentalDamage`:** a tracked PLAYER's `VOID` is taken before the PASS table is asked.
  - One claim on the shared 10-tick `DamageWindow` keeps the cadence at one hit per 0.5 s. The token becomes vanilla's
    `lastHurt`, which lets the in-between ticks through as events; the window absorbs them, as it does lava's.
  - The vanilla event is tokened, so vanilla's 4 no longer reaches the display.
  - Then `applyDamage(share hit, self, NORMAL, BYPASSED)`.
  - Mobs still fall through to `VOID`'s PASS, unchanged. A DOWN player's damage is cancelled at LOWEST and this handler
    ignores cancelled events.
- **The selftest's HURT line names it:** `… dealer=self dealerType=self cause=void`.

## 5. WHAT IT CHANGES, AND A FINDING FOR THE SEAT

- **Regeneration moves "the tenth tick".** Natural regeneration pays every 20 ticks while he is below his maximum (0.2 a
  second, x5 when saturated, plus his regeneration stat; READ, `HealthRegen`). So between the first void tick and the
  tenth (90 ticks) about four payments land. **The tenth tick of 35 then leaves the regenerated few points, and an
  ELEVENTH tick takes the rest.** The seat's reading "the tenth tick takes the last of a full bar" holds for a bar that
  does not regenerate; in play he goes down on the eleventh. **For the seat:** whether that matters, or whether the void
  should suppress regeneration (not built; no ruling).
- **Every merged row that falls into the void is re-predicted** in `GATE-fight-down.md`: FD2 as FD2R2 (restaged inside the
  ring), AE6 as AE6R4.
- **The ring and the void are now both rules of ours.** Outside the ring, a falling player takes the ring's strikes and
  the void's share on their own clocks. Both are real damage, and both are lethal on purpose in every row that stages
  them.
