package com.backblaze.b2.client.structures;/*
 * Copyright 2017, Backblaze, Inc. All rights reserved. 
 */

import com.backblaze.b2.util.B2BaseTest;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class B2UploadProgressTest extends B2BaseTest {
    private final static int PART_INDEX = 1;
    private final static int PART_COUNT = 2;
    private final static long START_BYTE = 3;
    private final static long LENGTH = 4;
    private final static long BYTES_SO_FAR = 5;
    private final static B2UploadState STATE = B2UploadState.SUCCEEDED;

    @Test
    public void test() {
        final B2UploadProgress progress = new B2UploadProgress(PART_INDEX,
                PART_COUNT,
                START_BYTE,
                LENGTH,
                BYTES_SO_FAR,
                STATE);

        assertEquals(PART_INDEX, progress.getPartIndex());
        assertEquals(PART_COUNT, progress.getPartCount());
        assertEquals(START_BYTE, progress.getStartByte());
        assertEquals(LENGTH, progress.getLength());
        assertEquals(BYTES_SO_FAR, progress.getBytesSoFar());
        assertEquals(STATE, progress.getState());

        assertEquals("B2UploadProgress{partIndex=1, partCount=2, startByte=3, length=4, bytesSoFar=5, state=SUCCEEDED}", progress.toString());
    }
}