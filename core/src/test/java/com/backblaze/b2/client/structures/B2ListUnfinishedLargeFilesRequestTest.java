/*
 * Copyright 2018, Backblaze, Inc.  All rights reserved.
 */

package com.backblaze.b2.client.structures;

import com.backblaze.b2.util.B2BaseTest;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class B2ListUnfinishedLargeFilesRequestTest extends B2BaseTest {

    @Test
    public void testBuilderCopies() {
        final B2ListUnfinishedLargeFilesRequest original =
                new B2ListUnfinishedLargeFilesRequest(
                        "bucketId",
                        "namePrefix",
                        "startFileId",
                        123
                );
        assertEquals(
                original,
                B2ListUnfinishedLargeFilesRequest.builder(original).build()
        );
    }

    @Test
    public void testBuilderSetsAll() {
        final B2ListUnfinishedLargeFilesRequest original =
                new B2ListUnfinishedLargeFilesRequest(
                        "bucketId",
                        "namePrefix",
                        "startFileId",
                        123
                );
        assertEquals(
                original,
                B2ListUnfinishedLargeFilesRequest
                        .builder("bucketId")
                        .setNamePrefix("namePrefix")
                        .setStartFileId("startFileId")
                        .setMaxFileCount(123)
                        .build()
        );
    }
}
