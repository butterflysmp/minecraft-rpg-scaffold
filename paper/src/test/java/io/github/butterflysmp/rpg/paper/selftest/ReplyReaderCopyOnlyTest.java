package io.github.butterflysmp.rpg.paper.selftest;

import io.github.butterflysmp.rpg.paper.RpgPlugin;
import io.github.butterflysmp.rpg.paper.packet.VanillaCritParticleListener;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The seat's ruling a: the reply reader is COPY-ONLY. It never cancels, modifies, re-encodes or delays a packet, and
 * it is registered only by a run, never at enable.
 *
 * <p>Read from the compiled class: a mutating call it does not make has no constant in it. <b>The control is
 * {@link VanillaCritParticleListener}, which does cancel</b>, so the scan is shown able to see the call it guards.
 */
class ReplyReaderCopyOnlyTest {

    /** Every PacketEvents call that changes, cancels or re-sends a packet or its wrapper. */
    private static final List<String> MUTATORS = List.of("setCancelled", "markForReEncode", "setLastUsedWrapper",
            "setMessage", "setMessageJson", "setOverlay", "setType", "write", "setPacketId", "setByteBuf",
            "sendPacket", "sendPacketSilently", "writePacket");

    @Test
    void theReaderMakesNoMutatingCall() throws IOException {
        List<String> constants = ClassConstants.of(ReplyReader.class);
        for (String call : MUTATORS) assertFalse(constants.contains(call), "ReplyReader references " + call);
        assertTrue(constants.contains("getMessage"), "the scan must see the read it does make");
    }

    @Test
    void controlTheScanSeesACancelWhereThereIsOne() throws IOException {
        assertTrue(ClassConstants.of(VanillaCritParticleListener.class).contains("setCancelled"));
    }

    @Test
    void theReaderIsNeverRegisteredAtEnableOnlyByARun() throws IOException {
        String reader = ReplyReader.class.getName().replace('.', '/');
        assertFalse(ClassConstants.of(RpgPlugin.class).contains(reader), "RpgPlugin names ReplyReader");
        assertTrue(ClassConstants.of(SelfTestRun.class).contains(reader), "control: the run does name it");
        assertTrue(ClassConstants.of(SelfTestRun.class).contains("unregisterListener"), "and unregisters it");
    }
}
