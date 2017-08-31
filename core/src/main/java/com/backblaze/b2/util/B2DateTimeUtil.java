/*
 * Utilies having to do with dates and times.
 *
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */

package com.backblaze.b2.util;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;

public class B2DateTimeUtil {
    static final long ONE_MINUTE_IN_SECONDS = 60;
    static final long ONE_HOUR_IN_SECONDS = 60 * ONE_MINUTE_IN_SECONDS;
    static final long ONE_DAY_IN_SECONDS = 24 * ONE_HOUR_IN_SECONDS;

    public static final long ONE_SECOND_IN_MSECS = 1000;
    static final long ONE_MINUTE_IN_MSECS = ONE_MINUTE_IN_SECONDS * ONE_SECOND_IN_MSECS;
    static final long ONE_HOUR_IN_MSECS = ONE_HOUR_IN_SECONDS * ONE_SECOND_IN_MSECS;
    static final long ONE_DAY_IN_MSECS = ONE_DAY_IN_SECONDS * ONE_SECOND_IN_MSECS;

    static final int ONE_SECOND_IN_NANOS = 1000000000;
    static final long ONE_MSEC_IN_NANOS = ONE_SECOND_IN_NANOS / ONE_SECOND_IN_MSECS;


    public static final int MIN_YEAR = 1970;
    public static final int MAX_YEAR = 2999;

    public static final int MIN_MONTH = 1;
    public static final int MAX_MONTH = 12;

    public static final int MIN_DAY = 1;
    public static final int MAX_DAY = 31;

    private static final int MIN_HOUR = 0;
    private static final int MAX_HOUR = 23;

    private static final int MIN_MINUTE = 0;
    private static final int MAX_MINUTE = 59;

    private static final int MIN_SECOND = 0;
    private static final int MAX_SECOND = 59; // no leap seconds in Java time


    //private static LocalDate EPOCH = LocalDate.of(1970, 1, 1);
    static LocalDateTime EPOCH_TIME = LocalDateTime.of(1970, 1, 1, 0, 0);


    /**
     * Formats a date in "solid" format, like "20150314"
     */
    public static String formatSolidDate(LocalDate date) {
        return String.format("%04d%02d%02d", date.getYear(), date.get(ChronoField.MONTH_OF_YEAR), date.getDayOfMonth());
    }

    /**
     * Parses a date-time string in any one of these formats:
     *
     *    "20150314092654"          - "solid" Backblaze dates
     *    "d20150314_m092654"       - FGUID-style Backblaze dates
     *    "2015-03-14 09:26:54"     - ISO-8601 with space
     *    "2015-03-14T09:26"        - ISO-8601 but no seconds
     *    "2015-03-14T09:26:54"     - ISO-8601
     *    "2015-03-14T09:26:54.547" - ISO-8601
     */
    public static LocalDateTime parseDateTime(String str) {
        try {
            int len = str.length();

            // Try ISO-8601:
            //     "2015-03-14T09:26"
            //     "2015-03-15T09:26:54"
            //     "2015-03-15T09:26:54.123"
            //     "2015-03-15T09:26:54.123456"
            //     "2015-03-15T09:26:54.123456789"
            //
            // Also handles:
            //     "2015-03-15 09:26:54"

            if (((len == 16) || (19 <= len && len <= 29)) &&
                    B2StringUtil.decimalNumberInRange(str, 0, 4, MIN_YEAR, MAX_YEAR) &&
                    B2StringUtil.decimalNumberInRange(str, 5, 2, MIN_MONTH, MAX_MONTH) &&
                    B2StringUtil.decimalNumberInRange(str, 8, 2, MIN_DAY, MAX_DAY) &&
                    B2StringUtil.decimalNumberInRange(str, 11, 2, MIN_HOUR, MAX_HOUR) &&
                    B2StringUtil.decimalNumberInRange(str, 14, 2, MIN_MINUTE, MAX_MINUTE) &&
                    (len == 16 || B2StringUtil.decimalNumberInRange(str, 17, 2, MIN_SECOND, MAX_SECOND)) &&
                    (len <= 19 || (str.charAt(19) == '.' && B2StringUtil.allDecimal(str, 20, len)))) {
                if (str.charAt(10) == ' ') {
                    str = str.substring(0, 10) + "T" + str.substring(11);
                }
                return LocalDateTime.parse(str);
            }

            // Try FGUID-style: "d20150315_m092654"
            if (len == 17 &&
                    B2StringUtil.decimalNumberInRange(str, 1, 4, MIN_YEAR, MAX_YEAR) &&
                    B2StringUtil.decimalNumberInRange(str, 5, 2, MIN_MONTH, MAX_MONTH) &&
                    B2StringUtil.decimalNumberInRange(str, 7, 2, MIN_DAY, MAX_DAY) &&
                    B2StringUtil.decimalNumberInRange(str, 11, 2, MIN_HOUR, MAX_HOUR) &&
                    B2StringUtil.decimalNumberInRange(str, 13, 2, MIN_MINUTE, MAX_MINUTE) &&
                    B2StringUtil.decimalNumberInRange(str, 15, 2, MIN_SECOND, MAX_SECOND)) {
                return LocalDateTime.of(
                        B2StringUtil.decimalSubstringToInt(str, 1, 5),
                        B2StringUtil.decimalSubstringToInt(str, 5, 7),
                        B2StringUtil.decimalSubstringToInt(str, 7, 9),
                        B2StringUtil.decimalSubstringToInt(str, 11, 13),
                        B2StringUtil.decimalSubstringToInt(str, 13, 15),
                        B2StringUtil.decimalSubstringToInt(str, 15, 17)
                );
            }

            // Try old-style Backblaze date/time: "20150315092654"
            if (len == 14 &&
                    B2StringUtil.decimalNumberInRange(str, 0, 4, MIN_YEAR, MAX_YEAR) &&
                    B2StringUtil.decimalNumberInRange(str, 4, 2, MIN_MONTH, MAX_MONTH) &&
                    B2StringUtil.decimalNumberInRange(str, 6, 2, MIN_DAY, MAX_DAY) &&
                    B2StringUtil.decimalNumberInRange(str, 8, 2, MIN_HOUR, MAX_HOUR) &&
                    B2StringUtil.decimalNumberInRange(str, 10, 2, MIN_MINUTE, MAX_MINUTE) &&
                    B2StringUtil.decimalNumberInRange(str, 12, 2, MIN_SECOND, MAX_SECOND)) {
                return LocalDateTime.of(
                        B2StringUtil.decimalSubstringToInt(str, 0, 4),
                        B2StringUtil.decimalSubstringToInt(str, 4, 6),
                        B2StringUtil.decimalSubstringToInt(str, 6, 8),
                        B2StringUtil.decimalSubstringToInt(str, 8, 10),
                        B2StringUtil.decimalSubstringToInt(str, 10, 12),
                        B2StringUtil.decimalSubstringToInt(str, 12, 14)
                );
            }
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("bad date time: " + e.getMessage(), e);
        }

        throw new IllegalArgumentException("bad date time: " + str);
    }

    /**
     * Returns a date-time in FGUID form: "d20150315_m092654"
     */
    public static String formatFguidDateTime(LocalDateTime dateTime) {
        return String.format(
                "d%04d%02d%02d_m%02d%02d%02d",
                dateTime.getYear(),
                dateTime.getMonthValue(),
                dateTime.getDayOfMonth(),
                dateTime.getHour(),
                dateTime.getMinute(),
                dateTime.getSecond()
        );
    }

    private static class DurationParser {
        private long secondsSoFar;
        private int numMatched;

        DurationParser(String str) {
            if (B2StringUtil.isEmpty(str)) {
                throw new IllegalArgumentException();
            }

            final boolean wasNegative = str.startsWith("-");
            if (wasNegative) {
                str = str.substring(1);
            }

            str = split(str, 'd', ONE_DAY_IN_SECONDS);
            str = split(str, 'h', ONE_HOUR_IN_SECONDS);
            str = split(str, 'm', ONE_MINUTE_IN_SECONDS);
            str = split(str, 's', 1);

            if (numMatched == 0) {
                throw new IllegalArgumentException();
            }
            if (str.length() > 0) {
                // extra stuff at the end!
                throw new IllegalArgumentException();
            }

            if (wasNegative) {
                secondsSoFar = -secondsSoFar;
            }
        }

        private String split(String str, char unitsChar, long numSecondsPerUnit) {
            final int iUnitsChar = str.indexOf(unitsChar);
            if (iUnitsChar < 0) {
                // this unitsChar isn't present.
                return str;
            }

            final String numPart = str.substring(0, iUnitsChar);

            // parsing this will fail if other units (ie: letter) present before
            // the one we're looking for.  this is how we ensure they're in the
            // right order.
            final long count = Long.parseLong(numPart);
            secondsSoFar += count * numSecondsPerUnit;
            numMatched++;

            return str.substring(iUnitsChar+1);
        }

        Duration getDuration() {
            return Duration.ofSeconds(secondsSoFar);
        }
    }

    /**
     *
     * @param str string to parse.  it must match:
     *            [-][Dd][Dh][Dm][Ds]
     *            where D is [0-9]+
     *            and at least one of the numeric parts must be specified.
     * @return the duration described by the given string or null if it doesn't match the pattern.
     */
    public static Duration parseDuration(String str) {
        try {
            return new DurationParser(str).getDuration();
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * @return a succinct string describing the given number of seconds.
     */
    public static String durationString(long deltaSecs) {
        String s = "";
        boolean wasNegative = (deltaSecs < 0);
        if (wasNegative) {
            deltaSecs = -deltaSecs;
        }
        if (deltaSecs >= ONE_DAY_IN_SECONDS) {
            final long nDays = deltaSecs / ONE_DAY_IN_SECONDS;
            s += nDays + "d";
            deltaSecs -= nDays * ONE_DAY_IN_SECONDS;
        }
        if (deltaSecs >= ONE_HOUR_IN_SECONDS) {
            final long nHours = deltaSecs / ONE_HOUR_IN_SECONDS;
            s += nHours + "h";
            deltaSecs -= nHours * ONE_HOUR_IN_SECONDS;
        }
        if (deltaSecs >= ONE_MINUTE_IN_SECONDS) {
            final long nMins = deltaSecs / ONE_MINUTE_IN_SECONDS;
            s += nMins + "m";
            deltaSecs -= nMins * ONE_MINUTE_IN_SECONDS;
        }
        if (deltaSecs > 0) {
            s += deltaSecs + "s";
        }
        if (s.length() == 0) {
            s = "0s";
        }
        if (wasNegative) {
            s = "-" + s;
        }
        return s;
    }

    /**
     * Returns the number of milliseconds since 1970-01-01 00:00:00.
     */
    static long getMillisecondsSinceEpoch(LocalDateTime dateTime) {
        // we have to use EPOCH_TIME instead of EPOCH because LocalDate
        // doesn't support ChronoUnit.SECONDS.
        return Duration.between(EPOCH_TIME, dateTime).toMillis();
    }

    // this exists so it can be called for code coverage purposes in the unit test.
    // it is package-private so that no one else can call it.
    B2DateTimeUtil() {
    }
}
