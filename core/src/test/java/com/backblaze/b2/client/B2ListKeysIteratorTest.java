/*
 * Copyright 2018, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */

package com.backblaze.b2.client;

import com.backblaze.b2.client.exceptions.B2Exception;
import com.backblaze.b2.client.structures.B2ApplicationKey;
import com.backblaze.b2.client.structures.B2ListKeysRequest;
import com.backblaze.b2.client.structures.B2ListKeysResponse;
import com.backblaze.b2.util.B2Collections;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class B2ListKeysIteratorTest {

    private final B2StorageClientImpl b2Client = mock(B2StorageClientImpl.class);

    @Test
    public void testNone() throws B2Exception {
        final B2ListKeysRequest request = B2ListKeysRequest.builder().build();
        when(b2Client.listKeys(eq(request))).thenReturn(makeResponse(null));
        check("", request);
    }

    @Test
    public void testAllAnswersInFirstResponse() throws B2Exception {
        final B2ListKeysRequest request = B2ListKeysRequest.builder().build();
        when(b2Client.listKeys(eq(request))).thenReturn(makeResponse(null, "id1", "id2", "id3"));
        check("id1 id2 id3", request);
    }

    @Test
    public void testMultipleRequests() throws B2Exception {
        final B2ListKeysRequest request1 = B2ListKeysRequest.builder().setMaxKeyCount(10).build();
        final B2ListKeysRequest request2 = B2ListKeysRequest.builder().setMaxKeyCount(10).setStartApplicationKeyId("id5").build();
        when(b2Client.listKeys(eq(request1))).thenReturn(makeResponse("id5", "id1", "id2", "id3"));
        when(b2Client.listKeys(eq(request2))).thenReturn(makeResponse(null, "id5"));
        check("id1 id2 id3 id5", request1);

    }

    private void check(String expectedKeyIds, B2ListKeysRequest request) {
        final StringBuilder actualKeyIds = new StringBuilder();
        B2ListKeysIterator iter = new B2ListKeysIterator(b2Client, request);
        while (iter.hasNext()) {
            if (actualKeyIds.length() != 0) {
                actualKeyIds.append(" ");
            }
            actualKeyIds.append(iter.next().getApplicationKeyId());
        }
        assertEquals(expectedKeyIds, actualKeyIds.toString());
    }

    private B2ListKeysResponse makeResponse(String nextApplicationKeyId, String ... keyIds) {
        final List<B2ApplicationKey> keys = new ArrayList<>();
        for (String keyId : keyIds) {
            keys.add(
                    new B2ApplicationKey(
                            "accountId",
                            keyId,
                            "name-" + keyId,
                            new TreeSet<>(),
                            "bucketId",
                            "namePrefix",
                            12345678L,
                            B2Collections.emptySet()
                    )
            );
        }
        return new B2ListKeysResponse(keys, nextApplicationKeyId);
    }

}
