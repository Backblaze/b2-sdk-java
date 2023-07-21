/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

import com.backblaze.b2.util.B2BaseTest;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertEquals;

public class B2LifecycleRuleTest extends B2BaseTest {
    private static final String FILE_PREFIX = "files/";

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testMinimal() {
        final B2LifecycleRule a = B2LifecycleRule
                .builder(FILE_PREFIX)
                .build();
        assertEquals("files/:null:null:null", a.toString());
    }

    @Test
    public void testMaximal() {
        final B2LifecycleRule a = B2LifecycleRule
                .builder(FILE_PREFIX)
                .setDaysFromUploadingToHiding(2)
                .setDaysFromHidingToDeleting(1)
                .setDaysFromStartingToCancelingUnfinishedLargeFiles(3)
                .build();
        final B2LifecycleRule b = B2LifecycleRule
                .builder(FILE_PREFIX)
                .setDaysFromUploadingToHiding(2)
                .setDaysFromHidingToDeleting(1)
                .setDaysFromStartingToCancelingUnfinishedLargeFiles(3)
                .build();
        assertEquals(a, b);
        assertEquals("files/:2:1:3", a.toString());
    }

    @Test
    public void testNullPrefixIsBad() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("fileNamePrefix must not be null");

        B2LifecycleRule
                .builder(null)
                .build();
    }

    @Test
    public void testNegativeDaysIsBad1() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("daysFromUploadingToHiding must be positive");

        B2LifecycleRule
                .builder(FILE_PREFIX)
                .setDaysFromUploadingToHiding(0)
                .build();
    }

    @Test
    public void testNegativeDaysIsBad2() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("daysFromHidingToDeleting must be positive");

        B2LifecycleRule
                .builder(FILE_PREFIX)
                .setDaysFromHidingToDeleting(0)
                .build();
    }

    @Test
    public void testNegativeDaysIsBad3() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("daysFromStartingToCancelingUnfinishedLargeFiles must be positive");

        B2LifecycleRule
                .builder(FILE_PREFIX)
                .setDaysFromStartingToCancelingUnfinishedLargeFiles(0)
                .build();
    }

}
