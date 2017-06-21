/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client;

import com.backblaze.b2.client.exceptions.B2Exception;
import com.backblaze.b2.client.exceptions.B2RuntimeException;
import com.backblaze.b2.client.structures.B2ListPartsResponse;
import com.backblaze.b2.client.structures.B2Part;
import com.backblaze.b2.util.B2Preconditions;

import java.util.Iterator;

public abstract class B2ListPartsIterableBase implements B2ListPartsIterable {
    private final B2StorageClientImpl b2Client;

    protected abstract class IterBase implements Iterator<B2Part> {
        private int currentIndex;

        IterBase() throws B2Exception {
            advanceIfNeeded();
        }

        @Override
        public boolean hasNext() {
            return (currentIndex < getCurrentResponseSize());
        }

        @Override
        public B2Part next() {
            if (!hasNext()) {
                throw new IllegalStateException("don't call when hasNext() returns false!");
            }

            final B2Part part = getIthCurrentResponse(currentIndex);
            currentIndex++;
            try {
                advanceIfNeeded();
            } catch (B2Exception e) {
                throw new B2RuntimeException("failed to advance iterator: " + e.getMessage(), e);
            }

            return part;
        }

        private void advanceIfNeeded() throws B2Exception {
            if (hasNext()) {
                // no need to advance.
                return;
            }

            if (getCurrentResponseOrNull() != null) {
                // we've gotten at least one page of results.  was it the last page?
                if (getCurrentResponseOrNull().atEnd()) {
                    // no more pages to fetch.
                    return;
                }
            }

            advance();
            currentIndex = 0;
        }

        // may be called when there's no current response yet.
        private int getCurrentResponseSize() {
            final B2ListPartsResponse response = getCurrentResponseOrNull();
            return (response == null) ?
                    0 :
                    response.getParts().size();
        }

        // must only be called when there should be a current response!
        private B2Part getIthCurrentResponse(int i) {
            final B2ListPartsResponse response = getCurrentResponseOrNull();
            B2Preconditions.checkState(response != null);
            return response.getParts().get(i);
        }

        // returns the most recently fetched response, or null if none yet.
        protected abstract B2ListPartsResponse getCurrentResponseOrNull();

        // this will only be called when we aren't at the end of the list yet.
        // namely, if there's no current response yet or if the current response
        // says we're not atEnd().
        abstract protected void advance() throws B2Exception;
    }

    B2ListPartsIterableBase(B2StorageClientImpl b2Client) {
        this.b2Client = b2Client;
    }

    @Override
    public Iterator<B2Part> iterator() {
        try {
            return createIter();
        } catch (B2Exception e) {
            throw new B2RuntimeException("failed to create/advance iterator: " + e.getMessage(), e);
        }
    }

    protected B2StorageClientImpl getClient() {
        return b2Client;
    }

    // creates a new iterator.  you should probably subclass IterBase.
    abstract Iterator<B2Part> createIter() throws B2Exception;
}
