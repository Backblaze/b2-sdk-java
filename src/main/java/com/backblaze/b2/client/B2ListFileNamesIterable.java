/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client;

import com.backblaze.b2.client.exceptions.B2Exception;
import com.backblaze.b2.client.structures.B2FileVersion;
import com.backblaze.b2.client.structures.B2ListFileNamesRequest;
import com.backblaze.b2.client.structures.B2ListFileNamesResponse;
import com.backblaze.b2.client.structures.B2ListFilesResponse;

import java.util.Iterator;

public class B2ListFileNamesIterable extends B2ListFilesIterableBase {
    private final B2ListFileNamesRequest request;

    private class Iter extends IterBase {
        private B2ListFileNamesResponse currentResponse;

        Iter() throws B2Exception {
        }

        @Override
        protected void advance() throws B2Exception {
            B2ListFileNamesRequest.Builder builder =
                    B2ListFileNamesRequest.builder(request);

            if (currentResponse != null) {
                builder.setStartFileName(currentResponse.getNextFileName());
            }

            currentResponse = getClient().listFileNames(builder.build());
        }

        @Override
        protected B2ListFilesResponse getCurrentResponseOrNull() {
            return currentResponse;
        }
    }

    public B2ListFileNamesIterable(B2StorageClientImpl b2Client,
                                   B2ListFileNamesRequest request) {
        super(b2Client);
        this.request = request;
    }

    @Override
    Iterator<B2FileVersion> createIter() throws B2Exception {
        return new Iter();
    }
}
