# PLAN: the selftest puts his stored builds back (the seat's D2, 2026-11-27)

**Status: BUILT on `feat/selftest-restore-builds` (not booted, not merged),** stacked on `feat/solar-flare` (#340), below
the one gate branch.

## The defect (the seat's words, in short)

SM1 (and SF1) pick an ability in the Build screen, and a pick is SAVED into his own build; the run did not put it back,
and `GATE-smolder.md`'s CARRIED STATE said so, citing `GATE-sunder.md`'s SU1. **A gate asks nothing of his state: a
precedent that broke the rule is not a ruling.**

## The fix, on H15's pattern (his accessory slots)

- **`SelfTestBuilds`** (new, pure; the store behind a `Port`): `capture` reads his whole `PlayerBuild` before any row
  runs; `restore` writes it back WHOLE and reads it back. Whole is the RECORD: every cell he has a loadout for, in stored
  order, every slot as stored (an unreadable or unoffered id included, an empty slot as null). **A cell with no loadout
  before the run has none after.** When the store already reads what was captured, nothing is written, so a run that
  changed nothing never creates a file.
- **`BuildService.restore(playerId, whole, onSettled)`** (new): the write `save` makes (`repository.save` of the whole
  record), but of the captured record rather than a merge of one cell; false, writing nothing, when the build is not
  loaded or the store is poisoned; a failed write poisons the session as a save's does.
- **`SelfTestSnapshot`**: captures in its constructor beside the accessory slots; restores in its one `restore` (the run's
  `end()`, reached on a finish and on an abort), after the cell, as the accessories are; the disk write's settle is logged
  (`BUILDS restore write settled ok=<b> player=<uuid>`).
- **The lines, read by named fields:**
  - `SELFTEST RUN CAPTURED … accessories=[…] builds=[<class>/<element>:Q=<id>,L=<id>,R=<id>,asp=<a>|<b>,frag=<a>|<b>|<c>|<d>;…]`
  - `SELFTEST RUN RESTORED … accessories=[…] builds=[…]`, with ` NOT-RESTORED=builds(not-captured|store-refused|
    unreadable-after|cells <class>/<element>,…)` when the read-back differs. `-` is an empty slot; `[]` is no cell.
- **The lint:** `ScenarioLint.Rule.SELF_STAGED`'s account now lists what the run puts back, builds included, and says a
  row that clicks a pick in the Build screen is allowed BECAUSE the snapshot restores it.

## CC'S CALL

**A player with NO build file before the run, whose builds a row changed, ends with a file holding no cell** (the store
has no delete; `BuildRepository` is load and save). Its content equals what he had (nothing), its bytes are not "no
file". The alternative: a delete on the repository, a persistence change no ruling covers. The gate's CARRIED STATE reads
his file's bytes before and after, so the case is named if it arises.

## Tests and mutations

`SelfTestBuildsTest` (5): a run that saves a pick into an existing cell AND into a cell he had none for puts his file
back BYTE FOR BYTE through the real `FileBuildRepository` (the unoffered `ultimate_placeholder_mage` back, `ranger/fire`
gone); a run that changed nothing writes nothing; the field's whole form; NOT-RESTORED's cases; the snapshot's wiring.
The abort path is the same `end()` restore (`SelfTestWornWiringTest`: one final restore).

| mutation | the change | red |
|---|---|---|
| MUTNOWRITE | the restore never writes | `aRunThatSavesPicks...` (bytes), `notRestoredNamesWhatDiffers` |
| MUTALWAYSWRITE | it writes even when unchanged | `aRunThatChangedNothingWritesNothing` (a file appears) |
| MUTWANTONLY | the read-back compares the captured cells only | `notRestoredNamesWhatDiffers` (an extra cell unseen) |
