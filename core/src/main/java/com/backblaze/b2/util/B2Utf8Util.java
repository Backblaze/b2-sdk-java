/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.util;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * B2Utf8Util has methods that are useful for processing Utf8.
 */
public class B2Utf8Util {
    /**
     * Writes the characters from str to out encoded as UTF-8.
     *
     * @param str the string to convert and write.
     * @param out the stream to write to
     * @throws IOException if there's a problem converting to UTF-8 or writing to out.
     *                     if an exception is thrown, out's state is undefined.
     */
    public static void write(String str,
                             OutputStream out) throws IOException {
        final int strLen = str.length();
        for (int i=0; i < strLen; i++) {
            final char c = str.charAt(i);
            i = doCommonWrite(str, i, c, out);
        }
    }

    /**
     * Writes the characters from str to out encoded as a quoted
     * JSON string.  This is just like write(), except that it handles
     * ASCII control characters, double-quotes, and backslashes specially.
     *
     * @param value the string to convert and write.
     * @param out the stream to write to
     * @throws IOException if there's a problem converting to UTF-8 or writing to out.
     *                     if an exception is thrown, out's state is undefined.
     */
    public static void writeJsonString(CharSequence value,
                                       OutputStream out) throws IOException {
        out.write('"');

        final int strLen = value.length();
        for (int i=0; i < strLen; i++) {
            final char c = value.charAt(i);
            if (c < 32) {
                out.write('\\');
                out.write('u');
                out.write('0');
                out.write('0');
                out.write(B2StringUtil.LOWER_HEX_DIGITS[c / 16]);
                out.write(B2StringUtil.LOWER_HEX_DIGITS[c % 16]);
            } else if (c == '"') {
                out.write('\\');
                out.write('"');
            } else if (c == '\\') {
                out.write('\\');
                out.write('\\');
            } else {
                i = doCommonWrite(value, i, c, out);
            }

        }
        out.write('"');
    }

    /**
     *
     * @param value  the string we're trying to write.  (needed if c started a surrogate pair)
     * @param currentIndex the index of the character in value that we're looking at (needed if c started a surrogate pair)
     * @param c the character we're looking at.
     * @param out where we write our output.
     * @return the currentIndex, which might've been updated (if c started a surrogate pair)
     * @throws IOException if there's trouble with surrogates or writing the output.
     */
    private static int doCommonWrite(CharSequence value,
                                     int currentIndex,
                                     char c,
                                     OutputStream out) throws IOException {
        int currentIndexOut = currentIndex;
        if (c < 0x80) {
            // Have at most seven bits
            out.write(c);
        } else if (c < 0x800) {
            // 2 bytes, 11 bits
            out.write(0xc0 | (c >> 6));
            out.write(0x80 | (c & 0x3f));
        } else if (Character.isSurrogate(c)) {
            // we're gonna need the other half of the surrogate pair.
            currentIndexOut++;
            if (currentIndexOut >= value.length()) {
                throw new IOException("bad surrogate pair: truncated");
            }
            final char low = value.charAt(currentIndexOut);
            if (!Character.isSurrogatePair(c, low)) {
                throw new IOException("bad surrogate pair");
            }

            final int cp = Character.toCodePoint(c, low);
            out.write(0xf0 | ((cp >> 18)));
            out.write(0x80 | ((cp >> 12) & 0x3f));
            out.write(0x80 | ((cp >>  6) & 0x3f));
            out.write(0x80 | (cp & 0x3f));
        } else {
            // 3 bytes, 16 bits
            out.write(0xe0 | ((c >> 12)));
            out.write(0x80 | ((c >>  6) & 0x3f));
            out.write(0x80 | (c & 0x3f));
        }

        return currentIndexOut;
    }
}
