/*
 * Copyright 2018, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */

package com.backblaze.b2.client;

import com.backblaze.b2.client.exceptions.B2Exception;
import com.backblaze.b2.client.exceptions.B2RuntimeException;
import com.backblaze.b2.client.structures.B2ApplicationKey;
import com.backblaze.b2.client.structures.B2ListKeysRequest;
import com.backblaze.b2.client.structures.B2ListKeysResponse;

import java.util.Iterator;
import java.util.List;

public class B2ListKeysIterator implements Iterator<B2ApplicationKey> {

    private final B2StorageClientImpl b2Client;

    private B2ListKeysRequest nextRequest;
    private List<B2ApplicationKey> currentList;
    private int nextIndex = 0;

    /*package*/ B2ListKeysIterator(B2StorageClientImpl b2Client, B2ListKeysRequest request) {
        this.b2Client = b2Client;
        this.nextRequest = request;
        advance();
    }

    /**
     * Make the next request.
     */
    private void advance() {
        try {
            final B2ListKeysResponse response = b2Client.listKeys(nextRequest);
            if (response.getNextApplicationKeyId() != null) {
                nextRequest = new B2ListKeysRequest(
                        nextRequest.getMaxKeyCount(),
                        response.getNextApplicationKeyId()
                );
            }
            else {
                nextRequest = null;
            }
            currentList = response.getKeys();
            nextIndex = 0;
        }
        catch (B2Exception e) {
            throw new B2RuntimeException("failed to advance iterator: " + e.getMessage(), e);
        }
    }
    
    @Override
    public boolean hasNext() {
        if (currentList.size() <= nextIndex && nextRequest != null) {
            advance();
        }
        return nextIndex < currentList.size();
    }

    @Override
    public B2ApplicationKey next() {
        final B2ApplicationKey result = currentList.get(nextIndex);
        nextIndex += 1;
        return result;
    }
}
