/*
 * Copyright 2018, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */

package com.backblaze.b2.client;

import com.backblaze.b2.client.structures.B2ApplicationKey;
import com.backblaze.b2.client.structures.B2ListKeysRequest;

import java.util.Iterator;

/**
 * This interface collects the APIs we provide on our B2ApplicationKey iterables.
 * For now, it's just the Iterable-ness.  Someday, I expect it, or some interfaces
 * to provide some kind of "get resume point" functionality.
 */
public class B2ListKeysIterable implements Iterable<B2ApplicationKey> {

    private final B2StorageClientImpl b2Client;
    private final B2ListKeysRequest request;

    public B2ListKeysIterable(B2StorageClientImpl b2Client, B2ListKeysRequest request) {
        this.b2Client = b2Client;
        this.request = request;
    }

    @Override
    public Iterator<B2ApplicationKey> iterator() {
        return new B2ListKeysIterator(b2Client, request);
    }
}
