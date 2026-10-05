package io.github.butterflysmp.rpg.paper.selftest;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads a compiled class's UTF-8 constants: every method name it calls, every class it names, every string literal.
 * A call the class does not make has no constant, so "never calls X" is "has no constant X".
 *
 * <p>Parsed properly, not grepped: a substring search for {@code cancel} would match {@code isCancelled}, which the
 * reader DOES call.
 */
final class ClassConstants {

    private ClassConstants() {}

    static List<String> of(Class<?> type) throws IOException {
        String path = type.getName().replace('.', '/') + ".class";
        try (InputStream in = type.getClassLoader().getResourceAsStream(path)) {
            if (in == null) throw new IOException("no class file " + path);
            return parse(in.readAllBytes());
        }
    }

    static List<String> parse(byte[] b) {
        List<String> out = new ArrayList<>();
        int count = u2(b, 8);
        int i = 10;
        for (int n = 1; n < count; n++) {
            int tag = b[i] & 0xff;
            switch (tag) {
                case 1 -> {   // CONSTANT_Utf8
                    int len = u2(b, i + 1);
                    out.add(new String(b, i + 3, len, StandardCharsets.UTF_8));
                    i += 3 + len;
                }
                case 3, 4, 9, 10, 11, 12, 17, 18 -> i += 5;
                case 5, 6 -> { i += 9; n++; }   // long and double take two slots
                case 7, 8, 16, 19, 20 -> i += 3;
                case 15 -> i += 4;
                default -> throw new IllegalStateException("unknown constant tag " + tag + " at " + i);
            }
        }
        return out;
    }

    private static int u2(byte[] b, int at) {
        return ((b[at] & 0xff) << 8) | (b[at + 1] & 0xff);
    }
}
