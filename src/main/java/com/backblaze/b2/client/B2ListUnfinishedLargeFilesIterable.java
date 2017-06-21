/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client;

import com.backblaze.b2.client.exceptions.B2Exception;
import com.backblaze.b2.client.structures.B2FileVersion;
import com.backblaze.b2.client.structures.B2ListFilesResponse;
import com.backblaze.b2.client.structures.B2ListUnfinishedLargeFilesRequest;
import com.backblaze.b2.client.structures.B2ListUnfinishedLargeFilesResponse;

import java.util.Iterator;

public class B2ListUnfinishedLargeFilesIterable extends B2ListFilesIterableBase {
    private final B2ListUnfinishedLargeFilesRequest request;

    private class Iter extends IterBase {
        private B2ListUnfinishedLargeFilesResponse currentResponse;

        Iter() throws B2Exception {
        }

        @Override
        protected B2ListFilesResponse getCurrentResponseOrNull() {
            return currentResponse;
        }

        @Override
        protected void advance() throws B2Exception {
            B2ListUnfinishedLargeFilesRequest.Builder builder =
                    B2ListUnfinishedLargeFilesRequest.builder(request);

            if (currentResponse != null) {
                builder.setStartFileId(currentResponse.getNextFileId());
            }

            currentResponse = getClient().listUnfinishedLargeFiles(builder.build());
        }
    }

    public B2ListUnfinishedLargeFilesIterable(B2StorageClientImpl b2Client,
                                              B2ListUnfinishedLargeFilesRequest request) {
        super(b2Client);
        this.request = request;
    }

    @Override
    Iterator<B2FileVersion> createIter() throws B2Exception {
        return new Iter();
    }
}
