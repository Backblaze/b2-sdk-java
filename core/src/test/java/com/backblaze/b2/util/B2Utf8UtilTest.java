/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.util;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static com.backblaze.b2.util.B2StringUtil.UTF8;
import static java.lang.Character.MAX_CODE_POINT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class B2Utf8UtilTest extends B2BaseTest {


    //////////////////////////////////////////////////////////////////////////////
    //
    // write()
    //
    //////////////////////////////////////////////////////////////////////////////

    @Test
    public void testWrite() throws IOException {
        int numTested = 0;
        int numBaddish = 0;
        for (int codePoint = 0; codePoint <= MAX_CODE_POINT; codePoint++) {
            final String codePointStr = stringForCodePoint(codePoint);
            try {
                if (codePoint >= 0xD800 && codePoint <= 0xDFFF) {
                    checkBadSurrogateLikeCodePoint(codePointStr);
                    numBaddish++;
                } else {
                    checkValidCodePoint(codePointStr);
                }
                numTested++;
            } catch (Throwable t) {
                throw new RuntimeException("trouble for codePoint " + codePointNum(codePoint) + ": " + t, t);
            }
        }
        System.err.println("testWrite: tested " + numTested + " code points, including " + numBaddish + " bad-ish ones.");
    }


    private void checkBadSurrogateLikeCodePoint(String str) {
        try {
            convert(str);
            fail("should've thrown!");
        } catch (IOException e) {
            assertTrue("actual message = " + e.getMessage(),
                    e.getMessage().startsWith("bad surrogate pair:"));
        }
    }

    private void checkValidCodePoint(String str) throws IOException {
        final byte[] fromJava = B2StringUtil.getUtf8Bytes(str);
        final byte[] ourBytes = convert(str);

        if (!Arrays.equals(fromJava, ourBytes)) {
            // do assertion based on a string representation so it's a lot easier
            // to see the diffs.  it's just too slow to do it that way all the time.
            assertEquals(pretty(fromJava), pretty(ourBytes));
        }
    }


    private byte[] convert(String str) throws IOException {
        // make a stream to catch the results.
        final ByteArrayOutputStream out = new ByteArrayOutputStream();

        // write
        B2Utf8Util.write(str, out);

        // return the bytes.
        return out.toByteArray();
    }

    //////////////////////////////////////////////////////////////////////////////
    //
    // writeJsonString()
    //
    //////////////////////////////////////////////////////////////////////////////


    @Test
    public void testWriteJsonString() throws IOException {
        int numTested = 0;
        int numBaddish = 0;
        for (int codePoint = 0; codePoint <= MAX_CODE_POINT; codePoint++) {
            try {
                final String codePointStr = stringForCodePoint(codePoint);
                if (codePoint < 32) {
                    final String expected = '"' + String.format("\\u%04x", codePoint) + '"';
                    checkByteArrays(B2StringUtil.getUtf8Bytes(expected), convertForJsonString(codePointStr));
                } else if (codePoint == '"') {
                    // in json, it should be "\"", so...
                    final byte[] expected = { '"', '\\', '"', '"'};
                    checkByteArrays(expected, convertForJsonString(codePointStr));
                } else if (codePoint == '\\') {
                    // in json, it should be "\\", so...
                    final byte[] expected = { '"', '\\', '\\', '"'};
                    checkByteArrays(expected, convertForJsonString(codePointStr));
                } else if (codePoint >= 0xD800 && codePoint <= 0xDFFF) {
                    checkBadSurrogateLikeCodePointForJsonString(codePointStr);
                    numBaddish++;
                } else {
                    checkValidCodePointForJsonString(codePointStr);
                }
                numTested++;
            } catch (Throwable t) {
                throw new RuntimeException("trouble for codePoint " + codePointNum(codePoint) + ": " + t, t);
            }
        }
        System.err.println("testWriteJsonString: tested " + numTested + " code points, including " + numBaddish + " bad-ish ones.");
    }

    private void checkBadSurrogateLikeCodePointForJsonString(String str) {
        try {
            convert(str);
            fail("should've thrown!");
        } catch (IOException e) {
            assertTrue("actual message = " + e.getMessage(),
                    e.getMessage().startsWith("bad surrogate pair:"));
        }
    }

    private void checkValidCodePointForJsonString(String str) throws IOException {
        final String quotedStr = '"' + str + '"';
        final byte[] fromJava = B2StringUtil.getUtf8Bytes(quotedStr);
        final byte[] ourBytes = convertForJsonString(str);

        checkByteArrays(fromJava, ourBytes);
    }


    private byte[] convertForJsonString(String str) throws IOException {
        // make a stream to catch the results.
        final ByteArrayOutputStream out = new ByteArrayOutputStream();

        // write
        B2Utf8Util.writeJsonString(str, out);

        // return the bytes.
        return out.toByteArray();
    }


    //////////////////////////////////////////////////////////////////////////////
    //
    // helpers
    //
    //////////////////////////////////////////////////////////////////////////////

    private String codePointNum(int codePoint) {
        return String.format("%06x", codePoint);
    }

    private String pretty(byte[] bytes) {
        StringBuilder str = new StringBuilder();
        for (byte b : bytes) {
            if (str.length() > 0) {
                str.append(", ");
            }
            str.append(String.format("%02x", b));
        }
        return "[" + str + "]";
    }

    // makes a string with the given codePoint as its only character.
    private String stringForCodePoint(int codePoint) {
        final StringBuilder builder = new StringBuilder();
        builder.appendCodePoint(codePoint);
        return builder.toString();
    }

    private void checkByteArrays(byte[] expected, byte[] actual) {
        if (!Arrays.equals(expected, actual)) {
            // do assertion based on a string representation so it's a lot easier
            // to see the diffs.  it's just too slow to do it that way all the time.
            assertEquals(pretty(expected), pretty(actual));
        }
    }
}
