/*
 * Copyright 2026, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client;

import com.backblaze.b2.testing.KnownDefect;
import org.junit.experimental.categories.Category;
import com.backblaze.b2.client.contentSources.B2ContentSource;
import com.backblaze.b2.client.exceptions.B2ConnectionBrokenException;
import com.backblaze.b2.client.exceptions.B2Exception;
import com.backblaze.b2.client.structures.B2AccountAuthorization;
import com.backblaze.b2.client.structures.B2CopyFileRequest;
import com.backblaze.b2.client.structures.B2CreateKeyRequest;
import com.backblaze.b2.client.structures.B2DeleteKeyRequest;
import com.backblaze.b2.client.structures.B2StartLargeFileRequest;
import com.backblaze.b2.client.structures.B2UploadFileRequest;
import com.backblaze.b2.util.B2BaseTest;
import com.backblaze.b2.util.B2Clock;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;

import static com.backblaze.b2.client.B2TestHelpers.bucketId;
import static com.backblaze.b2.client.B2TestHelpers.fileId;
import static com.backblaze.b2.client.B2TestHelpers.fileName;
import static com.backblaze.b2.util.B2DateTimeUtil.parseDateTime;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class B2StorageClientImplFindingsTest extends B2BaseTest {
    private static final B2AccountAuthorization ACCOUNT_AUTH = B2TestHelpers.makeAuth(1);
    private final B2StorageClientWebifier webifier = mock(B2StorageClientWebifier.class);
    private final B2ClientConfig config = B2ClientConfig.builder(ACCOUNT_AUTH.getAccountId(), "applicationKey", "FindingsTest/0.0.1").build();
    private final B2Sleeper sleeper = mock(B2Sleeper.class);
    private final B2StorageClientImpl client =
            new B2StorageClientImpl(webifier, config, B2DefaultRetryPolicy.supplier(), new B2Retryer(sleeper));

    private B2ConnectionBrokenException lostReply() {
        return new B2ConnectionBrokenException("broken", null, "reply lost");
    }

    /** The failure the client reports must still be the lost reply (the exception itself, or one that names it). */
    private static void assertLostReplyPropagated(B2Exception thrown) {
        for (Throwable t = thrown; t != null; t = t.getCause()) {
            if (t instanceof B2ConnectionBrokenException
                    && t.getMessage() != null && t.getMessage().contains("reply lost")) {
                return;
            }
        }
        fail("the lost-reply failure was not propagated; the client threw: " + thrown);
    }

    private void authorize() throws B2Exception {
        B2Clock.useSimulator(parseDateTime("2018-04-17 01:26:32"));
        when(webifier.authorizeAccount(anyObject())).thenReturn(ACCOUNT_AUTH);
    }

    /** Finding C3: create key must not be blindly retried after a lost reply (could create a duplicate key). */
    @Category(KnownDefect.class)
    @Test
    public void testFindingC3_createKeyNotRetried() throws B2Exception {
        authorize();
        when(webifier.createKey(anyObject(), anyObject())).thenThrow(lostReply());
        try {
            client.createKey(B2CreateKeyRequest.builder(Collections.singleton("listFiles"), "k").build());
            fail("client.createKey returned normally although the reply was lost");
        } catch (B2Exception expected) {
            assertLostReplyPropagated(expected);
        }
        verify(webifier, times(1)).createKey(anyObject(), anyObject());
    }

    /** Finding C3: start large file must not be blindly retried. */
    @Category(KnownDefect.class)
    @Test
    public void testFindingC3_startLargeFileNotRetried() throws B2Exception {
        authorize();
        when(webifier.startLargeFile(anyObject(), anyObject())).thenThrow(lostReply());
        try {
            client.startLargeFile(B2StartLargeFileRequest.builder(bucketId(1), fileName(1), "text/plain").build());
            fail("client.startLargeFile returned normally although the reply was lost");
        } catch (B2Exception expected) {
            assertLostReplyPropagated(expected);
        }
        verify(webifier, times(1)).startLargeFile(anyObject(), anyObject());
    }

    /** Finding C3: create bucket must not be blindly retried. */
    @Category(KnownDefect.class)
    @Test
    public void testFindingC3_createBucketNotRetried() throws B2Exception {
        authorize();
        when(webifier.createBucket(anyObject(), anyObject())).thenThrow(lostReply());
        try {
            client.createBucket("bucket1", "allPublic");
            fail("client.createBucket returned normally although the reply was lost");
        } catch (B2Exception expected) {
            assertLostReplyPropagated(expected);
        }
        verify(webifier, times(1)).createBucket(anyObject(), anyObject());
    }

    /** Finding C3: delete key must not be blindly retried (a retry would report a false "not found"). */
    @Category(KnownDefect.class)
    @Test
    public void testFindingC3_deleteKeyNotRetried() throws B2Exception {
        authorize();
        when(webifier.deleteKey(anyObject(), anyObject())).thenThrow(lostReply());
        try {
            client.deleteKey(B2DeleteKeyRequest.builder("keyId").build());
            fail("client.deleteKey returned normally although the reply was lost");
        } catch (B2Exception expected) {
            assertLostReplyPropagated(expected);
        }
        verify(webifier, times(1)).deleteKey(anyObject(), anyObject());
    }

    /** Finding C3: copy file must not be blindly retried (could create a duplicate version). */
    @Category(KnownDefect.class)
    @Test
    public void testFindingC3_copyFileNotRetried() throws B2Exception {
        authorize();
        when(webifier.copyFile(anyObject(), anyObject())).thenThrow(lostReply());
        try {
            client.copySmallFile(B2CopyFileRequest.builder(fileId(1), fileName(2)).build());
            fail("client.copySmallFile returned normally although the reply was lost");
        } catch (B2Exception expected) {
            assertLostReplyPropagated(expected);
        }
        verify(webifier, times(1)).copyFile(anyObject(), anyObject());
    }

    /** Finding C16: after close(), calls must be refused cleanly without reaching the (closed) HTTP layer. */
    @Category(KnownDefect.class)
    @Test
    public void testFindingC16_callAfterCloseIsRefusedCleanly() throws B2Exception {
        authorize();
        client.close();
        try {
            client.listBuckets(com.backblaze.b2.client.structures.B2ListBucketsRequest.builder("accountId").build());
            fail("a call after close() should be refused");
        } catch (B2Exception | IllegalStateException expected) {
            // a clean refusal is expected.
        }
        verify(webifier, never()).listBuckets(anyObject(), anyObject());
    }

    /** Finding C37: a large upload must send the source's last-modified time just like a small upload does. */
    @Category(KnownDefect.class)
    @Test
    public void testFindingC37_startLargeFileCarriesSrcLastModifiedMillis() throws Exception {
        final B2ContentSource source = mock(B2ContentSource.class);
        when(source.getContentLength()).thenReturn(10L);
        when(source.getSha1OrNull()).thenReturn(null);
        when(source.getSrcLastModifiedMillisOrNull()).thenReturn(1234567L);
        final B2UploadFileRequest upload = B2UploadFileRequest
                .builder(bucketId(1), fileName(1), "text/plain", source)
                .build();
        final B2StartLargeFileRequest start = B2StartLargeFileRequest.buildFrom(upload);
        assertEquals("1234567", start.getFileInfo().get("src_last_modified_millis"));
    }
}
