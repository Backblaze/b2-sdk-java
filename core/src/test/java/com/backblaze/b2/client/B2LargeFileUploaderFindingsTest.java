/*
 * Copyright 2026, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client;

import com.backblaze.b2.testing.KnownDefect;
import org.junit.experimental.categories.Category;
import com.backblaze.b2.client.contentSources.B2ContentSource;
import com.backblaze.b2.client.contentSources.B2ContentTypes;
import com.backblaze.b2.client.contentSources.B2Headers;
import com.backblaze.b2.client.exceptions.B2Exception;
import com.backblaze.b2.client.exceptions.B2InternalErrorException;
import com.backblaze.b2.client.structures.B2AccountAuthorization;
import com.backblaze.b2.client.structures.B2FileVersion;
import com.backblaze.b2.client.structures.B2UploadFileRequest;
import com.backblaze.b2.util.B2BaseTest;
import com.backblaze.b2.util.B2Collections;
import org.junit.After;
import org.junit.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.backblaze.b2.client.B2TestHelpers.bucketId;
import static com.backblaze.b2.client.B2TestHelpers.fileId;
import static com.backblaze.b2.client.B2TestHelpers.fileName;
import static com.backblaze.b2.client.B2TestHelpers.makeSha1;
import static com.backblaze.b2.client.B2TestHelpers.makeVersion;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class B2LargeFileUploaderFindingsTest extends B2BaseTest {
    private final B2StorageClientWebifier webifier = mock(B2StorageClientWebifier.class);
    private final B2AccountAuthorizationCache authCache = mock(B2AccountAuthorizationCache.class);
    private final B2AccountAuthorization auth = B2TestHelpers.makeAuth(1);
    private final B2ContentSource source = mock(B2ContentSource.class);
    private final ExecutorService pool = Executors.newFixedThreadPool(2);

    @After
    public void tearDown() {
        pool.shutdownNow();
    }

    /** Finding C34: resuming a large file that was started with a known SHA-1 must not be refused as a "fileInfo mismatch". */
    @Category(KnownDefect.class)
    @Test
    public void testFindingC34_resumeOfFileStartedWithKnownSha1IsAccepted() throws Exception {
        final String sha1 = makeSha1(7);
        when(source.getContentLength()).thenReturn(2000L);
        when(source.getSha1OrNull()).thenReturn(sha1);
        final B2UploadFileRequest request = B2UploadFileRequest
                .builder(bucketId(1), fileName(2), B2ContentTypes.APPLICATION_OCTET, source)
                .build();
        final B2FileVersion started = new B2FileVersion(
                fileId(1), fileName(2), 0, B2ContentTypes.APPLICATION_OCTET, null, null,
                B2Collections.mapOf(B2Headers.LARGE_FILE_SHA1_INFO_NAME, sha1),
                "start", 1234, null, null, null, null);
        try {
            B2LargeFileUploader.throwIfLargeFileVersionDoesntSeemToMatchRequest(started, 2000L, request);
        } catch (B2Exception e) {
            fail("resume was refused although the content matches: " + e.getMessage());
        }
    }

    /** Finding C9: when a part upload fails after the large file was started, the error must identify the file so it can be resumed or cancelled. */
    @Category(KnownDefect.class)
    @Test
    public void testFindingC9_failureAfterStartNamesTheLargeFileId() throws Exception {
        when(source.getContentLength()).thenReturn(2 * auth.getRecommendedPartSize());
        final B2FileVersion started = makeVersion(1, 2);
        when(webifier.startLargeFile(anyObject(), anyObject())).thenReturn(started);
        when(webifier.getUploadPartUrl(anyObject(), anyObject())).thenThrow(new B2InternalErrorException("test", "part trouble"));
        final B2UploadFileRequest request = B2UploadFileRequest
                .builder(bucketId(1), fileName(2), B2ContentTypes.APPLICATION_OCTET, source)
                .build();
        final B2LargeFileUploader uploader = new B2LargeFileUploader(
                new B2Retryer(mock(B2Sleeper.class)), webifier, authCache, B2DefaultRetryPolicy.supplier(), pool,
                B2PartSizes.from(auth), request, 2 * auth.getRecommendedPartSize());
        try {
            uploader.uploadLargeFile();
            fail("expected failure");
        } catch (B2Exception e) {
            assertTrue("message does not contain the large file id " + started.getFileId() + ": " + e.getMessage(),
                    e.getMessage().contains(started.getFileId()));
        }
    }
}
