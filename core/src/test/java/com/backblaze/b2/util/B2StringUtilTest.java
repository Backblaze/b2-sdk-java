/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.util;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static com.backblaze.b2.util.B2StringUtil.allDecimal;
import static com.backblaze.b2.util.B2StringUtil.decimalNumberInRange;
import static com.backblaze.b2.util.B2StringUtil.decimalSubstringToInt;
import static com.backblaze.b2.util.B2StringUtil.getUtf8Bytes;
import static com.backblaze.b2.util.B2StringUtil.isEmpty;
import static com.backblaze.b2.util.B2StringUtil.join;
import static com.backblaze.b2.util.B2StringUtil.percentEncode;
import static com.backblaze.b2.util.B2StringUtil.startsWithIgnoreCase;
import static com.backblaze.b2.util.B2StringUtil.toHexString;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class B2StringUtilTest extends B2BaseTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testIsEmpty() {
        assertTrue(isEmpty(null));
        assertTrue(isEmpty(""));
        assertTrue(!isEmpty("foo"));
    }

    @Test
    public void testPercentEncode() {
        assertEquals("", percentEncode(""));
        assertEquals("a--/--b--%2B--%3A--%7C--+--", percentEncode("a--/--b--+--:--|-- --"));
    }

    @Test
    public void testAllDecimal() {
        assertTrue(allDecimal("", 0, 0));
        assertTrue(allDecimal("a123b", 1, 4));

        assertTrue(!allDecimal("a123b", 0, 4));
        assertTrue(!allDecimal("a123b", 1, 5));
    }

    @Test
    public void testDecimalSubstringToInt() {
        assertEquals(0, decimalSubstringToInt("a123456b", 0, 0));
        assertEquals(2, decimalSubstringToInt("a123456b", 2, 3));
        assertEquals(123456, decimalSubstringToInt("a123456b", 1, 7));

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Character a is not a digit");
        decimalSubstringToInt("012a345", 2, 4);
    }

    @Test
    public void testDecimalNumberInRange() {
        // true!
        assertTrue(decimalNumberInRange("a123b", 1, 3, 123, 123));
        assertTrue(decimalNumberInRange("a123b", 1, 0, 0, 0));
        assertTrue(decimalNumberInRange("a123b", 1, 1, 1, 1));

        // false 'cuz non-decimal
        assertTrue(!decimalNumberInRange("a123b", 0, 4, 0, 1000));
        assertTrue(!decimalNumberInRange("a123b", 1, 4, 0, 1000));

        // false 'cuz out-of-range
        assertTrue(!decimalNumberInRange("a123b", 1, 3, 100, 122));
        assertTrue(!decimalNumberInRange("a123b", 1, 3, 124, 200));
    }

    @Test
    public void testJoin() {
        assertEquals("", join(",", new Object[0]));
        assertEquals("null", join(",", new Object[] {null}));
        assertEquals("null,null", join(",", new Object[] {null, null}));
        assertEquals("abc, 1, 2, 3", join(", ", new Object[] {"abc", 1, 2, 3}));
    }

    @Test
    public void testGetUtf8Bytes() {
        assertArrayEquals(new byte[0], getUtf8Bytes(""));
        assertArrayEquals(new byte[] { 97, 98, 99 }, getUtf8Bytes("abc"));
        assertArrayEquals(new byte[] { (byte) 0xc3, (byte) 0xa2, (byte) 0xc6, (byte) 0x83, (byte) 0xc3, (byte) 0xa7 }, getUtf8Bytes("âƃç"));
    }

    @Test
    public void testToHexString() {
        assertEquals("", toHexString(null));
        assertEquals("", toHexString(new byte[0]));
        assertEquals("abf0", toHexString(new byte[] {(byte) 0xab, (byte) 0xf0}));
    }

    @Test
    public void testStartsWithIgnoreCase() {

        // cases with nulls
        assertFalse(startsWithIgnoreCase(null, "prefix"));
        assertFalse(startsWithIgnoreCase("all", null));
        assertFalse(startsWithIgnoreCase(null, null));

        // cases where all is too short to possibly contain the prefix.
        assertFalse(startsWithIgnoreCase("", "prefix"));
        assertFalse(startsWithIgnoreCase("prefi", "prefix"));

        // cases where it's just not the right prefix
        assertFalse(startsWithIgnoreCase("real", "fake"));
        assertFalse(startsWithIgnoreCase("real", "read"));

        // cases where it's the right prefix, even with different cases.
        assertTrue(startsWithIgnoreCase("real", "real"));
        assertTrue(startsWithIgnoreCase("really", "real"));
        assertTrue(startsWithIgnoreCase("REALly", "real"));
        assertTrue(startsWithIgnoreCase("REALly", "ReAl"));
    }

    @Test
    public void test_forCoverage() {
        // grrrr....  i really don't want to have to do this.
        new B2StringUtil();
    }

    @Test
    public void testUnderscoresToCamelCase() {
        assertEquals("", B2StringUtil.underscoresToCamelCase("", false));
        assertEquals("listFiles", B2StringUtil.underscoresToCamelCase("LIST_FILES", false));
        assertEquals("ListFiles", B2StringUtil.underscoresToCamelCase("_LIST_FILES", false));
        assertEquals("ListFiles", B2StringUtil.underscoresToCamelCase("LIST_FILES", true));

    }
}
