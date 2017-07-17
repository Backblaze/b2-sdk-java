/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */

package com.backblaze.b2.json;

import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Unit tests for JsonReader
 */
public class B2JsonReaderTest {

    @Test
    public void testReadNumberAsString() throws IOException, B2JsonException {
        // Start with bogus.
        checkNumber("xxx", null);

        // Something after the number
        checkNumber("5 a", "5");

        // Negative and not
        checkNumber("999 5", "999");
        checkNumber("-0 5", "-0");

        // Nothing following a leading 0
        checkNumber("05", null);

        // Decimal point
        checkNumber(".45", null);
        checkNumber("0.45", "0.45");
        checkNumber("123.", "123.");
        checkNumber("3.14", "3.14");

        // Exponents
        checkNumber("5e10", "5e10");
        checkNumber("5e+10", "5e+10");
        checkNumber("5e-10", "5e-10");
        checkNumber("5ex10", null);

        // Comments
        checkNumber("// comment \n 8", "8");
    }

    private void checkNumber(String text, String expectedOrNull) throws IOException, B2JsonException {
        B2JsonReader reader = new B2JsonReader(new StringReader(text));
        if (expectedOrNull == null) {
            try {
                reader.readNumberAsString();
                fail("should have thrown: " + text);
            }
            catch (B2JsonException e) {
                // cool
            }
        }
        else {
            assertEquals(expectedOrNull, reader.readNumberAsString());
        }
    }

    @Test
    public void testReadString() throws IOException, B2JsonException {
        checkString("", null);
        checkString("a", null);
        checkString("\"a", null);
        checkString("\"\\x\"", null);

        checkString("\"\"", "");
        checkString("\"abc\"", "abc");
        checkString("\"\\u0000\"", "\u0000");
        checkString("\"\\uffFF\"", "\uffFF");

        checkBackslashUCodePoint("\u0041", 0x41);
        checkBackslashUCodePoint("\u00DF", 0xDF);
        checkBackslashUCodePoint("\u6771", 0x6771);
        checkBackslashUCodePoint("\uD7FF", 0xD7FF);
        checkBackslashUCodePoint("\uE000", 0xE000);
        checkBackslashUCodePoint("\uFFFF", 0xFFFF);
        checkBackslashUCodePoint("\uD802\uDC5A", 0x1085A); // Imperial Aramaic Number Three

        // These cases come from http://www.oracle.com/us/technologies/java/supplementary-142654.html
        checkUtf8CodePoint(new byte[]{0x41}, 0x41);
        checkUtf8CodePoint(new byte[]{(byte) 0xc3, (byte) 0x9f}, 0xDF);
        checkUtf8CodePoint(new byte[]{(byte) 0xe6, (byte) 0x9d, (byte) 0xb1}, 0x6771);
        checkUtf8CodePoint(new byte[]{(byte) 0xf0, (byte) 0x90, (byte) 0x90, (byte) 0x80}, 0x10400);
    }

    private void checkString(String text, String expectedOrNull) throws IOException, B2JsonException {
        B2JsonReader reader = new B2JsonReader(new StringReader(text));
        if (expectedOrNull == null) {
            try {
                reader.readString();
                fail("should have thrown: " + text);
            }
            catch (B2JsonException e) {
                // cool
            }
        }
        else {
            assertEquals(expectedOrNull, reader.readString());
        }
    }

    private void checkBackslashUCodePoint(String text, int expectedCodePoint) throws IOException, B2JsonException {
        B2JsonReader reader = new B2JsonReader(new StringReader("\"" + text + "\""));
        String s = reader.readString();
        assertEquals(1, s.codePointCount(0, s.length()));
        assertEquals(expectedCodePoint, s.codePointAt(0));
    }

    private void checkUtf8CodePoint(byte [] utf8Bytes, int expectedCodePoint) throws IOException, B2JsonException {
        String text = new String(utf8Bytes, "UTF-8");
        B2JsonReader reader = new B2JsonReader(new StringReader("\"" + text + "\""));
        String s = reader.readString();
        assertEquals(1, s.codePointCount(0, s.length()));
        assertEquals(expectedCodePoint, s.codePointAt(0));
    }

    @Test
    public void testSkipValue() throws IOException, B2JsonException {
        checkSkipValue(" null 8 ");
        checkSkipValue(" true 8 ");
        checkSkipValue(" false 8 ");
        checkSkipValue(" -1 8 ");
        checkSkipValue(" 99 8 ");
        checkSkipValue(" \"hello\" 8 ");
        checkSkipValue(" [ 3, 4 ] 8 ");
        checkSkipValue(" [] 8 ");
        checkSkipValue(" { \"a\": 5 } 8 ");
        checkSkipValue(" {} 8 ");
    }

    private void checkSkipValue(String s) throws IOException, B2JsonException {
        B2JsonReader reader = new B2JsonReader(new StringReader(s));
        reader.skipValue();
        assertEquals("8", reader.readNumberAsString());
    }

    @Test
    public void testNextNotWhitespaceIsEof() throws IOException, B2JsonException {
        {
            B2JsonReader reader = new B2JsonReader(new StringReader("    x    "));
            assertFalse(reader.nextNonWhitespaceIsEof());
        }
        {
            B2JsonReader reader = new B2JsonReader(new StringReader("  \n // comment \n    "));
            assertTrue(reader.nextNonWhitespaceIsEof());
        }
    }
}
