/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client;

import com.backblaze.b2.client.structures.B2UploadState;
import com.backblaze.b2.util.B2BaseTest;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class B2UploadProgressUtilTest extends B2BaseTest {
    private static final int PART_COUNT = 10;
    private static final long LENGTH = 100;
    private static final B2PartSpec PART_SPEC = new B2PartSpec(2, 123, LENGTH);
    private static final long BYTES_SO_FAR = 6;


    @Test
    public void forPart() throws Exception {
        assertEquals("B2UploadProgress{partIndex=1, partCount=10, startByte=123, length=100, bytesSoFar=1, state=UPLOADING}",
                B2UploadProgressUtil.forPart(PART_SPEC, PART_COUNT, 1, B2UploadState.UPLOADING).toString());
    }

    @Test
    public void forPartSucceeded() throws Exception {
        assertEquals("B2UploadProgress{partIndex=1, partCount=10, startByte=123, length=100, bytesSoFar=100, state=SUCCEEDED}",
                B2UploadProgressUtil.forPartSucceeded(PART_SPEC, 10).toString());
    }

    @Test
    public void forPartFailed() throws Exception {
        assertEquals("B2UploadProgress{partIndex=1, partCount=10, startByte=123, length=100, bytesSoFar=6, state=FAILED}",
                B2UploadProgressUtil.forPartFailed(PART_SPEC, 10, BYTES_SO_FAR).toString());
    }

    @Test
    public void forSmallFile() throws Exception {
        assertEquals("B2UploadProgress{partIndex=0, partCount=1, startByte=0, length=100, bytesSoFar=6, state=FAILED}",
                B2UploadProgressUtil.forSmallFile(LENGTH, BYTES_SO_FAR, B2UploadState.FAILED).toString());
    }

    @Test
    public void forSmallFileWaitingToStart() throws Exception {
        assertEquals("B2UploadProgress{partIndex=0, partCount=1, startByte=0, length=100, bytesSoFar=0, state=WAITING_TO_START}",
                B2UploadProgressUtil.forSmallFileWaitingToStart(LENGTH).toString());
    }

    @Test
    public void forSmallFileStarting() throws Exception {
        assertEquals("B2UploadProgress{partIndex=0, partCount=1, startByte=0, length=100, bytesSoFar=0, state=STARTING}",
                B2UploadProgressUtil.forSmallFileStarting(LENGTH).toString());
    }

    @Test
    public void forSmallFileSucceeded() throws Exception {
        assertEquals("B2UploadProgress{partIndex=0, partCount=1, startByte=0, length=100, bytesSoFar=100, state=SUCCEEDED}",
                B2UploadProgressUtil.forSmallFileSucceeded(LENGTH).toString());
    }

    @Test
    public void forSmallFileFailed() throws Exception {
        assertEquals("B2UploadProgress{partIndex=0, partCount=1, startByte=0, length=100, bytesSoFar=6, state=FAILED}",
                B2UploadProgressUtil.forSmallFileFailed(LENGTH, BYTES_SO_FAR).toString());
    }

}