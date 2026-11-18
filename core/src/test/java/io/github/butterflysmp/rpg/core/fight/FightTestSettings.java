package io.github.butterflysmp.rpg.core.fight;

/** The numbers the tests run on: Ben's rulings plus the seat's placeholders (PLAN-dragon-fight.md section 2). */
final class FightTestSettings {

    private FightTestSettings() {}

    /** 48 and 12 are Ben's; 5.0, 0.4, 1.0, 10 crystals every 80 ticks are seat placeholders; 40 and 20 are Ben's. */
    static FightSettings placeholders() {
        return new FightSettings(
                48.0,   // borderRadiusBlocks
                12.0,   // mageRingRadiusBlocks
                5.0,    // fastCloseBlocksPerSecond
                0.4,    // slowCloseBlocksPerSecond
                1.0,    // killExpansionBlocks
                10,     // crystalCount
                80L,    // crystalIntervalTicks
                40L,    // strikeIntervalTicks
                20.0);  // firstStrikeDamage
    }
}
