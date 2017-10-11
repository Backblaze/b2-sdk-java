/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.util;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * String utilities
 */
public class B2StringUtil {
    public static final String UTF8 = "UTF-8";

    private static final char [] LOWER_HEX_DIGITS = new char [] {
            '0', '1', '2', '3', '4', '5', '6', '7',
            '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
    };

    /**
     * @param str the string to check
     * @return true if str is null or zero-length.
     */
    static boolean isEmpty(String str) {
        if (str == null) {
            return true;
        }
        str = str.trim();
        return (str.length() == 0);
    }

    /**
     * @param s the string to encode.
     * @return a string with the contents of 's' encoded with percents as is done for URLs.
     */
    public static String percentEncode(String s) {
        try {
            return URLEncoder.encode(s, UTF8).replace("%2F", "/");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("UTF8 isn't supported? " + e.getMessage(), e);
        }
    }

    private static boolean isDecimalDigitChar(char c) {
        return ('0' <= c) && (c <= '9');
    }

    /**
     * @param str the string to examine
     * @param startIndex the index of the first character to examine
     * @param endIndex the index just past the last character to examine.  must be &gt;= startIndex.
     * @return true iff there are no non-decimal characters in the specified substring of str.
     */
    @SuppressWarnings("SameParameterValue")
    static boolean allDecimal(String str,
                              int startIndex,
                              int endIndex) {
        B2Preconditions.checkArgument(startIndex <= endIndex);
        for (int i = startIndex; i < endIndex; i++) {
            if (!isDecimalDigitChar(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static int decimalCharToInt(char c) {
        if (c < '0' || '9' < c) {
            throw new IllegalArgumentException("Character " + c + " is not a digit");
        }
        return c - '0';
    }

    /**
     *
     * @param s the string to examine
     * @param startIndex the index of the first character to examine
     * @param endIndex the index just past the last character to examine.  must be &gt;= startIndex.
     * @return 0 if beginIndex == endIndex, otherwise the specified substring of 's' as an integer.
     * @throws IllegalArgumentException if the substring doesn't represent an integer.
     */
    public static int decimalSubstringToInt(String s, int startIndex, int endIndex) {
        int result = 0;
        for (int i = startIndex; i < endIndex; i++) {
            result = result * 10 + decimalCharToInt(s.charAt(i));
        }
        return result;
    }

    /**
     * @param str the string to examine
     * @param offset index of the first character to examine.  must be &gt;= 0.
     * @param length the length of the substring to examine.  must be &gt;= 0.
     * @param minInclusive the smallest value allowed
     * @param maxInclusive the largest value allowed
     * @return true iff the specified substring of 'str' is an integer in the specified range.
     */
    public static boolean decimalNumberInRange(String str, int offset, int length,
                                               int minInclusive, int maxInclusive) {
        B2Preconditions.checkArgument(offset >= 0 && length >= 0);
        int value = 0;
        for (int i = offset; i < offset + length; i++) {
            char c = str.charAt(i);
            if (c < '0' || '9' < c) {
                return false;
            }
            value = (value * 10) + (c - '0');
        }
        return (minInclusive <= value) && (value <= maxInclusive);
    }

    public static String join(String delimiter, Object[] objects) {
        final StringBuilder builder = new StringBuilder();
        for (Object o : objects) {
            if (builder.length() > 0) {
                builder.append(delimiter);
            }
            final String s = (o == null) ? null : o.toString();
            builder.append(s);
        }
        return builder.toString();
    }

    /**
     * Returns the UTF-8 representation of a string.
     *
     * We assume that there's always a UTF-8 charset installed, and
     * it's a bother to catch the exception everywhere, so this method
     * catches the exception for you.
     */
    public static byte [] getUtf8Bytes(String str) {
        try {
            return str.getBytes(UTF8);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("No UTF-8 charset");
        }
    }

    /**
     * @param b the array of bytes to convert.
     * @return if b is null or empty, "".  otherwise, returns a string with
     *         two lowercase hex digits for each byte in the string, representing
     *         the value of that byte.
     */
    public static String toHexString(byte[] b) {
        if (b == null || b.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder(b.length * 2);
        for (int i = 0; i < b.length; i++) {
            int unsignedByte = (b[i] & 0xff);
            sb.append(LOWER_HEX_DIGITS[unsignedByte >> 4]);
            sb.append(LOWER_HEX_DIGITS[unsignedByte & 0x0f]);
        }
        return sb.toString();
    }


    // this exists so it can be called for code coverage purposes in the unit test.
    // it is package-private so that no one else can call it.
    B2StringUtil() {
    }
}
