/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class B2ErrorStructureTest {
    private static final int STATUS = 407;
    private static final String CODE = "test";
    private static final String MSG = "test message";

    @Test
    public void testMostlyForCoverage() {
        final B2ErrorStructure err = new B2ErrorStructure(STATUS, CODE, MSG);
        assertEquals(STATUS, err.status);
        assertEquals(CODE, err.code);
        assertEquals(MSG, err.message);
    }
}
