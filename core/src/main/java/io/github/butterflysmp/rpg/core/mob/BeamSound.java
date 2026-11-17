package io.github.butterflysmp.rpg.core.mob;

/**
 * ONE SOUND THE KNELL'S BEAM PLAYS (the follow-up of 2026-11-17; Ben: "The bell plays with the warning. The real shot
 * comes when the resonance part starts."). Content, never a constant in code: authored in the beam's {@code sounds:}
 * block, under {@code telegraph} (played at the telegraph's first tick, the lock) or {@code fire} (played at the fire).
 *
 * <p><b>NAME THE QUANTITY:</b>
 * <ul>
 *   <li>{@code key}: the SOUND EVENT key, as vanilla's {@code sounds.json} names it ({@code block.bell.use}); the
 *       loader refuses one the server does not know, by name.</li>
 *   <li>{@code volume}: the volume passed to {@code World#playSound}, 0 or more.</li>
 *   <li>{@code pitch}: the pitch passed to {@code World#playSound}, above 0. The CLIENT multiplies it by the pitch of
 *       whichever {@code sounds.json} variant it draws, which the server cannot know.</li>
 * </ul>
 */
public record BeamSound(String key, double volume, double pitch) {

    public BeamSound {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("beam.sounds key must name a sound event, was '" + key + "'");
        }
        if (!(volume >= 0 && Double.isFinite(volume))) {
            throw new IllegalArgumentException("beam.sounds volume must be 0 or more, was " + volume + " (" + key + ")");
        }
        if (!(pitch > 0 && Double.isFinite(pitch))) {
            throw new IllegalArgumentException("beam.sounds pitch must be above 0, was " + pitch + " (" + key + ")");
        }
    }
}
