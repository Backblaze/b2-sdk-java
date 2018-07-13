/*
 * Copyright 2018, Backblaze, Inc.  All rights reserved.
 */

package com.backblaze.b2.client.structures;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class B2ListUnfinishedLargeFilesRequestTest {

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
