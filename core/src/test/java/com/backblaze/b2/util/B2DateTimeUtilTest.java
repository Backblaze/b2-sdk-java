/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.util;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class B2DateTimeUtilTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testFormatSolidDate() {
        Assert.assertEquals("20150314", B2DateTimeUtil.formatSolidDate(LocalDate.of(2015, 3, 14)));
    }

    @Test
    public void testParseDateTime() {
        assertEquals("2015-03-14T09:26", toDateTimeToString("2015-03-14T09:26:00"));
        assertEquals("2015-03-14T09:26:54", toDateTimeToString("2015-03-14T09:26:54"));
        assertEquals("2015-03-14T09:26:54", toDateTimeToString("2015-03-14 09:26:54"));
        assertEquals("2015-03-14T09:26:54", toDateTimeToString("2015-03-14T09:26:54.000"));
        assertEquals("2015-03-14T09:26:54", toDateTimeToString("20150314092654"));
        assertEquals("2015-03-14T09:26:54", toDateTimeToString("d20150314_m092654"));
    }

    @Test
    public void testParseDateTime_badLength() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("bad date time: 123");

        B2DateTimeUtil.parseDateTime("123");
    }
    @Test
    public void testBadDateTime1() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("bad date time: Text '1970-02-29T09:26:54' could not be parsed: Invalid date 'February 29' as '1970' is not a leap year");

        B2DateTimeUtil.parseDateTime("1970-02-29T09:26:54");
    }

    @Test
    public void testFormatFguidDateTime() {
        final LocalDateTime dateTime = B2DateTimeUtil.parseDateTime("2015-03-14T09:26:54");
        Assert.assertEquals("d20150314_m092654", B2DateTimeUtil.formatFguidDateTime(dateTime));
    }

    private String toDateTimeToString(String str) {
        LocalDateTime dateTime = B2DateTimeUtil.parseDateTime(str);
        return dateTime.toString();
    }


    @Test
    public void checkDurationString() {
        final long S = 1;
        final long M = B2DateTimeUtil.ONE_MINUTE_IN_SECONDS;
        final long H = B2DateTimeUtil.ONE_HOUR_IN_SECONDS;
        final long D = B2DateTimeUtil.ONE_DAY_IN_SECONDS;

        Assert.assertEquals("1d2h3m4s", B2DateTimeUtil.durationString(D + 2 * H + 3 * M + 4 * S));
        Assert.assertEquals("-1d2h3m4s", B2DateTimeUtil.durationString(-(D + 2 * H + 3 * M + 4 * S)));

        Assert.assertEquals("366d", B2DateTimeUtil.durationString(366 * D));
        Assert.assertEquals("23h",  B2DateTimeUtil.durationString(23 * H));
        Assert.assertEquals("59m",  B2DateTimeUtil.durationString(59 * M));
        Assert.assertEquals("59s",  B2DateTimeUtil.durationString(59 * S));
        Assert.assertEquals("-59s", B2DateTimeUtil.durationString(-59 * S));
        Assert.assertEquals("0s", B2DateTimeUtil.durationString(0));

        // one of each unit, to make sure using >= rather than just >.
        Assert.assertEquals("1d1h1m1s", B2DateTimeUtil.durationString(D + H + M + S));
    }

    @Test
    public void testParseDuration() {
        final long S = 1;
        final long M = B2DateTimeUtil.ONE_MINUTE_IN_SECONDS;
        final long H = B2DateTimeUtil.ONE_HOUR_IN_SECONDS;
        final long D = B2DateTimeUtil.ONE_DAY_IN_SECONDS;

        assertNull(B2DateTimeUtil.parseDuration(null));
        assertNull(B2DateTimeUtil.parseDuration(""));
        assertNull(B2DateTimeUtil.parseDuration("-"));
        assertNull(B2DateTimeUtil.parseDuration("1")); // missing unit
        assertNull(B2DateTimeUtil.parseDuration("1a")); // unknown unit "a"
        assertNull(B2DateTimeUtil.parseDuration("1s2d")); // units in the wrong order
        assertNull(B2DateTimeUtil.parseDuration("1ha")); // extra characters after a good duration.

        checkParseDuration("366d", 366 * D);
        checkParseDuration("23h", 23 * H);
        checkParseDuration("59m", 59 * M);
        checkParseDuration("59s", 59 * S);
        checkParseDuration("-59s", -59 * S);
        checkParseDuration("0s", 0);

        checkParseDuration("1d2h3m4s", D + 2 * H + 3 * M + 4 * S);
        checkParseDuration("-1d2h3m4s", -(D + 2 * H + 3 * M + 4 * S));
    }

    private void checkParseDuration(String durString, long expectedNumSecs) {
        final Duration dur = B2DateTimeUtil.parseDuration(durString);
        assertNotNull(dur);
        assertEquals(expectedNumSecs, dur.getSeconds());
    }

    @Test
    public void testMillisecondsSinceEpoch() {
        checkSinceEpochDateTime(0, "1970-01-01 00:00:00");
        checkSinceEpochDateTime(B2DateTimeUtil.ONE_SECOND_IN_MILLIS, "1970-01-01 00:00:01");
        checkSinceEpochDateTime(B2DateTimeUtil.ONE_MINUTE_IN_MILLIS, "1970-01-01 00:01:00");
        checkSinceEpochDateTime(B2DateTimeUtil.ONE_HOUR_IN_MILLIS, "1970-01-01 01:00:00");
        checkSinceEpochDateTime(B2DateTimeUtil.ONE_DAY_IN_MILLIS, "1970-01-02 00:00:00");
        checkSinceEpochDateTime(31 * B2DateTimeUtil.ONE_DAY_IN_MILLIS, "1970-02-01 00:00:00");
    }

    private void checkSinceEpochDateTime(long expectedMillis, String dateTimeStr) {
        LocalDateTime d = B2DateTimeUtil.parseDateTime(dateTimeStr);
        assertEquals(expectedMillis, B2DateTimeUtil.getMillisecondsSinceEpoch(d));
    }


    @Test
    public void test_forCoverage() {
        // grrrr....  i really don't want to have to do this.
        new B2DateTimeUtil();
    }
}
