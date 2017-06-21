/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client;

import com.backblaze.b2.client.exceptions.B2Exception;
import com.backblaze.b2.client.structures.B2ListPartsRequest;
import com.backblaze.b2.client.structures.B2ListPartsResponse;
import com.backblaze.b2.client.structures.B2Part;

import java.util.Iterator;

public class B2ListPartsIterableImpl extends B2ListPartsIterableBase {
    private final B2ListPartsRequest request;

    private class Iter extends IterBase {
        private B2ListPartsResponse currentResponse;

        Iter() throws B2Exception {
        }

        @Override
        protected void advance() throws B2Exception {
            B2ListPartsRequest.Builder builder =
                    B2ListPartsRequest.builder(request);

            if (currentResponse != null) {
                builder.setStartPartNumber(currentResponse.getNextPartNumber());
            }

            currentResponse = getClient().listParts(builder.build());
        }

        @Override
        protected B2ListPartsResponse getCurrentResponseOrNull() {
            return currentResponse;
        }
    }

    public B2ListPartsIterableImpl(B2StorageClientImpl b2Client,
                                   B2ListPartsRequest request) {
        super(b2Client);
        this.request = request;
    }

    @Override
    Iterator<B2Part> createIter() throws B2Exception {
        return new Iter();
    }
}
