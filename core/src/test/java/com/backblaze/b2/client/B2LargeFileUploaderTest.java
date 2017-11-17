/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client;

import com.backblaze.b2.client.contentSources.B2ContentSource;
import com.backblaze.b2.client.contentSources.B2ContentTypes;
import com.backblaze.b2.client.contentSources.B2Headers;
import com.backblaze.b2.client.exceptions.B2Exception;
import com.backblaze.b2.client.exceptions.B2InternalErrorException;
import com.backblaze.b2.client.exceptions.B2LocalException;
import com.backblaze.b2.client.structures.B2AccountAuthorization;
import com.backblaze.b2.client.structures.B2FileVersion;
import com.backblaze.b2.client.structures.B2FinishLargeFileRequest;
import com.backblaze.b2.client.structures.B2GetUploadPartUrlRequest;
import com.backblaze.b2.client.structures.B2Part;
import com.backblaze.b2.client.structures.B2UploadFileRequest;
import com.backblaze.b2.client.structures.B2UploadListener;
import com.backblaze.b2.client.structures.B2UploadPartRequest;
import com.backblaze.b2.client.structures.B2UploadPartUrlResponse;
import com.backblaze.b2.client.structures.B2UploadProgress;
import com.backblaze.b2.client.structures.B2UploadState;
import com.backblaze.b2.util.B2Clock;
import com.backblaze.b2.util.B2Collections;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static com.backblaze.b2.client.B2LargeFileUploaderTest.When.IN_GET;
import static com.backblaze.b2.client.B2LargeFileUploaderTest.When.IN_GET_AFTER_RUNNING_IN_SUBMIT;
import static com.backblaze.b2.client.B2LargeFileUploaderTest.When.IN_SUBMIT;
import static com.backblaze.b2.client.B2TestHelpers.bucketId;
import static com.backblaze.b2.client.B2TestHelpers.fileId;
import static com.backblaze.b2.client.B2TestHelpers.fileName;
import static com.backblaze.b2.client.B2TestHelpers.makePart;
import static com.backblaze.b2.client.B2TestHelpers.makeSha1;
import static com.backblaze.b2.client.B2TestHelpers.makeVersion;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The normal, easy successful use of B2LargeFileUploader is covered in B2StorageClientImplTest.
 * This test class covers exception paths.
 */
public class B2LargeFileUploaderTest {
    private final B2Sleeper sleeper = mock(B2Sleeper.class);
    private final B2Retryer retryer = new B2Retryer(sleeper);
    private final B2StorageClientWebifier webifier = mock(B2StorageClientWebifier.class);
    private final B2AccountAuthorizationCache accountAuthCache = mock(B2AccountAuthorizationCache.class);

    private final B2AccountAuthorization ACCOUNT_AUTH = B2TestHelpers.makeAuth(1);
    private final B2PartSizes PART_SIZES = B2PartSizes.from(ACCOUNT_AUTH);

    // a content source that's barely big enough to be a large file.
    private final B2ContentSource contentSource = mock(B2ContentSource.class);

    // useful in some tests.
    private RecordingUploadListener recordingListener = new RecordingUploadListener();

    /**
     * A RecordingUploadListener makes a list of strings describing the
     * progress calls that it gets.  Before using it, call setExpected().
     *
     * Because the WAITING_TO_START progress updates come from the "main" thread
     * and the others come from within the executor, the updates may occur
     * in different orders during different runs (and on different machines).
     * We use Sets to let our comparisons work even with the reordering.
     *
     * It checks that the state for each partIndex always moves forward, to
     * verify that the messages for one partIndex are reasonably ordered.
     */
    private static class RecordingUploadListener implements B2UploadListener {
        private final Set<String> expected = new TreeSet<>();
        private final Set<String> actual = new TreeSet<>();
        private final Map<Integer, B2UploadState> mostRecentStatePerPartIndex = new TreeMap<>();

        @Override
        public synchronized void progress(B2UploadProgress progress) {
            actual.add(progress.toString());

            final B2UploadState prevState = mostRecentStatePerPartIndex.get(progress.getPartIndex());
            if (prevState != null) {
                assertTrue("prevState (" + prevState + ") must be less than (" + progress.getState() + ")",
                        prevState.compareTo(progress.getState()) < 0);
            }
            mostRecentStatePerPartIndex.put(progress.getPartIndex(), progress.getState());
        }

        synchronized void setExpected(String... v) {
            expected.addAll(B2Collections.listOf(v));
        }
        synchronized void checkAgainstExpected() {
            if (!expected.equals(actual)) {
                String msg = "expected:\n" +
                        join(expected) + "\n" +
                        "actual:\n" +
                        join(actual) + "\n";
                System.err.println(msg);
                fail(msg);
            }
        }

        private static String join(Set<String> lines) {
            final String indent = "  ";
            StringBuilder s = new StringBuilder();
            for (String line : lines) {
                if (s.length() == 0) {
                    s.append(indent);
                } else {
                    s.append("\n").append(indent);
                }
                s.append(line);
            }
            return s.toString();
        }
    }


    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @After
    public void tearDown() {
        // clear the interrupted flag.
        Thread.interrupted();

        // either the test case isn't using the listener or we should've checked all notifications already.
        recordingListener.checkAgainstExpected();
    }

    public B2LargeFileUploaderTest() throws IOException {
        // barely big enough to be a large file
        final long contentLen = (2 * ACCOUNT_AUTH.getRecommendedPartSize());
        when(contentSource.getContentLength()).thenReturn(contentLen);
    }

    @Test
    public void testExceptionFromGetSha1OrNull_inUpload() throws B2Exception, IOException {
        // arrange to throw when asked for the sha1.
        when(contentSource.getSha1OrNull()).thenThrow(new IOException("testing"));

        recordingListener.setExpected(); // no progress because threw before starting uploads.

        thrown.expect(B2LocalException.class);
        thrown.expectMessage("failed to get large file's sha1 from contentSource: testing");
        makeUploader(contentSource, recordingListener).uploadLargeFile();
    }

    @Test
    public void testExceptionFromUploadPart_inResume() throws B2Exception, IOException {
        // arrange to answer startLargeRequest
        final B2FileVersion largeFileVersion = makeVersion(1, 2);
        when(webifier.startLargeFile(anyObject(), anyObject())).thenReturn(largeFileVersion);

        // arrange to answer get_upload_part_url (which will be called several times, but it's ok to reuse the same value since it's all mocked!)
        final B2GetUploadPartUrlRequest partUrlRequest = new B2GetUploadPartUrlRequest(largeFileVersion.getFileId());
        final B2UploadPartUrlResponse partUrl = new B2UploadPartUrlResponse(largeFileVersion.getFileId(), "uploadPartUrl", "uploadPartAuthToken");
        when(webifier.getUploadPartUrl(anyObject(), eq(partUrlRequest))).thenReturn(partUrl);

        // arrange to answer upload_part, based on the request.
        when(webifier.uploadPart(anyObject(), anyObject())).thenAnswer(invocationOnMock -> {
            B2UploadPartRequest request = (B2UploadPartRequest) invocationOnMock.getArguments()[1];
            return makePart(request.getPartNumber());
        });

        // depending on thread timing we'll get one of these results:
        recordingListener.setExpected(
                "B2UploadProgress{partIndex=0, partCount=2, startByte=0, length=1000, bytesSoFar=0, state=WAITING_TO_START}",
                "B2UploadProgress{partIndex=0, partCount=2, startByte=0, length=1000, bytesSoFar=0, state=STARTING}",
                "B2UploadProgress{partIndex=0, partCount=2, startByte=0, length=1000, bytesSoFar=1000, state=SUCCEEDED}",
                "B2UploadProgress{partIndex=1, partCount=2, startByte=1000, length=1000, bytesSoFar=0, state=WAITING_TO_START}",
                "B2UploadProgress{partIndex=1, partCount=2, startByte=1000, length=1000, bytesSoFar=0, state=STARTING}",
                "B2UploadProgress{partIndex=1, partCount=2, startByte=1000, length=1000, bytesSoFar=1000, state=SUCCEEDED}"
                );

        thrown.expect(B2Exception.class);
        thrown.expectMessage("testing");
        makeUploader(PART_SIZES, new ThrowingExecutor(IN_GET_AFTER_RUNNING_IN_SUBMIT, ExceptionType.B2_EXCEPTION), contentSource, recordingListener).uploadLargeFile();
    }


    @Test
    public void testExceptionFromGetSha1OrNull_inResume() throws B2Exception, IOException {
        // arrange to throw when asked for the sha1.
        when(contentSource.getSha1OrNull()).thenThrow(new IOException("testing"));

        final B2FileVersion version = new B2FileVersion(fileId(1),
                fileName(1),
                contentSource.getContentLength(),
                B2ContentTypes.B2_AUTO,
                null,
                B2Collections.mapOf(),
                "upload",
                123L);

        recordingListener.setExpected(); // no progress because threw before starting uploads.

        thrown.expect(B2LocalException.class);
        thrown.expectMessage("failed to get large file's sha1: java.io.IOException: testing");
        makeUploader(contentSource, recordingListener).finishUploadingLargeFile(version, B2Collections.listOf());
    }

    @Test
    public void testRejectionWhileSubmitting() throws B2Exception, IOException {
        // arrange to answer startLargeRequest
        final B2FileVersion largeFileVersion = makeVersion(1, 2);
        when(webifier.startLargeFile(anyObject(), anyObject())).thenReturn(largeFileVersion);

        // we have to return
        final ExecutorService executor = new ThrowingExecutor(IN_SUBMIT, ExceptionType.INTERRUPTED);

        recordingListener.setExpected(
                "B2UploadProgress{partIndex=0, partCount=2, startByte=0, length=1000, bytesSoFar=0, state=WAITING_TO_START}"
                // nothing else cuz it throws.
        );

        thrown.expect(B2LocalException.class);
        thrown.expectMessage("The executor rejected an upload task. Does it have a hard limit? Did you call shutdown() on it? (java.util.concurrent.RejectedExecutionException: testing)");
        makeUploader(PART_SIZES, executor, contentSource, recordingListener).uploadLargeFile();
    }

    @Test
    public void testInterruptedWhenCallingGet() throws B2Exception, IOException {
        // arrange to answer startLargeRequest
        final B2FileVersion largeFileVersion = makeVersion(1, 2);
        when(webifier.startLargeFile(anyObject(), anyObject())).thenReturn(largeFileVersion);

        // we have to return
        final ExecutorService executor = new ThrowingExecutor(IN_GET, ExceptionType.INTERRUPTED);

        recordingListener.setExpected(
                "B2UploadProgress{partIndex=0, partCount=2, startByte=0, length=1000, bytesSoFar=0, state=WAITING_TO_START}",
                "B2UploadProgress{partIndex=1, partCount=2, startByte=1000, length=1000, bytesSoFar=0, state=WAITING_TO_START}"
                // nothing else cuz it throws.
        );

        thrown.expect(B2LocalException.class);
        thrown.expectMessage("interrupted while trying to upload parts: java.lang.InterruptedException: sleep interrupted");
        makeUploader(PART_SIZES, executor, contentSource, recordingListener).uploadLargeFile();
    }

    @Test
    public void testB2ExceptionWhenCallingGet() throws B2Exception, IOException {
        // arrange to answer startLargeRequest
        final B2FileVersion largeFileVersion = makeVersion(1, 2);
        when(webifier.startLargeFile(anyObject(), anyObject())).thenReturn(largeFileVersion);

        // we have to return
        final ExecutorService executor = new ThrowingExecutor(IN_GET, ExceptionType.B2_EXCEPTION);

        recordingListener.setExpected(
                "B2UploadProgress{partIndex=0, partCount=2, startByte=0, length=1000, bytesSoFar=0, state=WAITING_TO_START}",
                "B2UploadProgress{partIndex=1, partCount=2, startByte=1000, length=1000, bytesSoFar=0, state=WAITING_TO_START}"
                // nothing else cuz it throws.
        );

        thrown.expect(B2InternalErrorException.class);
        thrown.expectMessage("testing");
        makeUploader(PART_SIZES, executor, contentSource, recordingListener).uploadLargeFile();
    }

    @Test
    public void testOtherExceptionWhenCallingGet() throws B2Exception, IOException {
        // arrange to answer startLargeRequest
        final B2FileVersion largeFileVersion = makeVersion(1, 2);
        when(webifier.startLargeFile(anyObject(), anyObject())).thenReturn(largeFileVersion);

        // we have to return
        final ExecutorService executor = new ThrowingExecutor(IN_GET, ExceptionType.IO_EXCEPTION);

        recordingListener.setExpected(
                "B2UploadProgress{partIndex=0, partCount=2, startByte=0, length=1000, bytesSoFar=0, state=WAITING_TO_START}",
                "B2UploadProgress{partIndex=1, partCount=2, startByte=1000, length=1000, bytesSoFar=0, state=WAITING_TO_START}"
                // nothing else cuz it throws.
        );

        thrown.expect(B2LocalException.class);
        thrown.expectMessage("exception while trying to upload parts: java.io.IOException: testing");
        makeUploader(PART_SIZES, executor, contentSource, recordingListener).uploadLargeFile();
    }

    // throwIfLargeFileVersionDoesntSeemToMatchRequest's "happy path" is exercised as part of other tests.
    // the following tests concentrate on the various mismatch cases.

    @Test
    public void testThrowIfLargeFileVersionDoesntSeemToMatchRequest_mismatchingFileName() throws IOException, B2Exception {
        final B2UploadFileRequest request = B2UploadFileRequest
                .builder(bucketId(1), fileName(2), B2ContentTypes.APPLICATION_OCTET, contentSource)
                .build();
        final B2FileVersion largeFileVersion = new B2FileVersion(
                fileId(1),
                fileName(1), // this is the mismatch!
                contentSource.getContentLength(),
                B2ContentTypes.APPLICATION_OCTET,
                null, // sha1
                B2Collections.mapOf(),
                "action",
                1234
        );

        thrown.expect(B2LocalException.class);
        thrown.expectMessage("contentSource has fileName 'files/0002', but largeFileVersion has 'files/0001'");
        B2LargeFileUploader.throwIfLargeFileVersionDoesntSeemToMatchRequest(largeFileVersion, contentSource.getContentLength(), request);
    }

    @Test
    public void testThrowIfLargeFileVersionDoesntSeemToMatchRequest_mismatchingSha1() throws IOException, B2Exception {
        final B2UploadFileRequest request = B2UploadFileRequest
                .builder(bucketId(1), fileName(2), B2ContentTypes.APPLICATION_OCTET, contentSource)
                .build();
        final B2FileVersion largeFileVersion = new B2FileVersion(
                fileId(1),
                fileName(2),
                contentSource.getContentLength(),
                B2ContentTypes.APPLICATION_OCTET,
                null, // sha1 of the file (always null for large files.  they use LARGE_FILE_SHA1!)
                B2Collections.mapOf(B2Headers.LARGE_FILE_SHA1_INFO_NAME, makeSha1(1)),
                "action",
                1234
        );

        thrown.expect(B2LocalException.class);
        thrown.expectMessage("contentSource has sha1 'null', but largeFileVersion has '1111111111111111111111111111111111111111'");
        B2LargeFileUploader.throwIfLargeFileVersionDoesntSeemToMatchRequest(largeFileVersion, contentSource.getContentLength(), request);
    }

    @Test
    public void testThrowIfLargeFileVersionDoesntSeemToMatchRequest_mismatchingContentType() throws IOException, B2Exception {
        final B2UploadFileRequest request = B2UploadFileRequest
                .builder(bucketId(1), fileName(2), B2ContentTypes.APPLICATION_OCTET, contentSource)
                .build();
        final B2FileVersion largeFileVersion = new B2FileVersion(
                fileId(1),
                fileName(2),
                contentSource.getContentLength(),
                B2ContentTypes.TEXT_PLAIN,  // this is the mismatch!
                null,
                B2Collections.mapOf(),
                "action",
                1234
        );

        thrown.expect(B2LocalException.class);
        thrown.expectMessage("contentSource has contentType 'application/octet', but largeFileVersion has 'text/plain'");
        B2LargeFileUploader.throwIfLargeFileVersionDoesntSeemToMatchRequest(largeFileVersion, contentSource.getContentLength(), request);
    }

    @Test
    public void testThrowIfLargeFileVersionDoesntSeemToMatchRequest_mismatchingContentType_actuallyMatchesIfRequestSaysAuto() throws IOException, B2Exception {
        final B2UploadFileRequest request = B2UploadFileRequest
                .builder(bucketId(1), fileName(2), B2ContentTypes.B2_AUTO, contentSource)
                .build();
        final B2FileVersion largeFileVersion = new B2FileVersion(
                fileId(1),
                fileName(2),
                contentSource.getContentLength(),
                B2ContentTypes.TEXT_PLAIN,  // this is NOT a mismatch, because request is B2_AUTO.
                null,
                B2Collections.mapOf(),
                "action",
                1234
        );

        // shouldn't throw.  :)
        B2LargeFileUploader.throwIfLargeFileVersionDoesntSeemToMatchRequest(largeFileVersion, contentSource.getContentLength(), request);
    }

    @Test
    public void testThrowIfLargeFileVersionDoesntSeemToMatchRequest_mismatchingFileInfo() throws IOException, B2Exception {
        final B2UploadFileRequest request = B2UploadFileRequest
                .builder(bucketId(1), fileName(2), B2ContentTypes.APPLICATION_OCTET, contentSource)
                .setCustomField("a", "b")
                .build();
        final B2FileVersion largeFileVersion = new B2FileVersion(
                fileId(1),
                fileName(2),
                contentSource.getContentLength(),
                B2ContentTypes.APPLICATION_OCTET,
                null,
                B2Collections.mapOf(),
                "action",
                1234
        );

        thrown.expect(B2LocalException.class);
        thrown.expectMessage("contentSource has fileInfo '{\n  a=b\n}', but largeFileVersion has '{\n}'");
        B2LargeFileUploader.throwIfLargeFileVersionDoesntSeemToMatchRequest(largeFileVersion, contentSource.getContentLength(), request);
    }

    @Test
    public void testNoAlreadyUploadedParts() throws IOException, B2Exception {
        final List<B2Part> alreadyUploadedParts = B2Collections.listOf();

        recordingListener.setExpected(
                "B2UploadProgress{partIndex=0, partCount=3, startByte=0, length=1041, bytesSoFar=0, state=WAITING_TO_START}",
                "B2UploadProgress{partIndex=1, partCount=3, startByte=1041, length=1041, bytesSoFar=0, state=WAITING_TO_START}",
                "B2UploadProgress{partIndex=2, partCount=3, startByte=2082, length=1042, bytesSoFar=0, state=WAITING_TO_START}",
                "B2UploadProgress{partIndex=0, partCount=3, startByte=0, length=1041, bytesSoFar=0, state=STARTING}",
                "B2UploadProgress{partIndex=0, partCount=3, startByte=0, length=1041, bytesSoFar=1041, state=SUCCEEDED}",
                "B2UploadProgress{partIndex=1, partCount=3, startByte=1041, length=1041, bytesSoFar=0, state=STARTING}",
                "B2UploadProgress{partIndex=1, partCount=3, startByte=1041, length=1041, bytesSoFar=1041, state=SUCCEEDED}",
                "B2UploadProgress{partIndex=2, partCount=3, startByte=2082, length=1042, bytesSoFar=0, state=STARTING}",
                "B2UploadProgress{partIndex=2, partCount=3, startByte=2082, length=1042, bytesSoFar=1042, state=SUCCEEDED}"
        );

        checkPartMatching(alreadyUploadedParts, 1, 2, 3);
    }

    @Test
    public void testOneAlreadyUploadedPartThatMatches_part1() throws IOException, B2Exception {
        final String largeFileId = fileId(1);
        final List<B2Part> alreadyUploadedParts = B2Collections.listOf(
                new B2Part(largeFileId, 1, 1041, makeSha1(1), 1111)
        );

        recordingListener.setExpected(
                "B2UploadProgress{partIndex=0, partCount=3, startByte=0, length=1041, bytesSoFar=0, state=WAITING_TO_START}",
                "B2UploadProgress{partIndex=0, partCount=3, startByte=0, length=1041, bytesSoFar=1041, state=SUCCEEDED}",
                "B2UploadProgress{partIndex=1, partCount=3, startByte=1041, length=1041, bytesSoFar=0, state=WAITING_TO_START}",
                "B2UploadProgress{partIndex=2, partCount=3, startByte=2082, length=1042, bytesSoFar=0, state=WAITING_TO_START}",
                "B2UploadProgress{partIndex=1, partCount=3, startByte=1041, length=1041, bytesSoFar=0, state=STARTING}",
                "B2UploadProgress{partIndex=1, partCount=3, startByte=1041, length=1041, bytesSoFar=1041, state=SUCCEEDED}",
                "B2UploadProgress{partIndex=2, partCount=3, startByte=2082, length=1042, bytesSoFar=0, state=STARTING}",
                "B2UploadProgress{partIndex=2, partCount=3, startByte=2082, length=1042, bytesSoFar=1042, state=SUCCEEDED}"
        );

        checkPartMatching(alreadyUploadedParts, 2, 3);
    }


    @Test
    public void testOneAlreadyUploadedPartThatMatches_part3() throws IOException, B2Exception {
        final String largeFileId = fileId(1);
        final List<B2Part> alreadyUploadedParts = B2Collections.listOf(
                new B2Part(largeFileId, 3, 1042, makeSha1(3), 3333)
        );

        recordingListener.setExpected(
                "B2UploadProgress{partIndex=0, partCount=3, startByte=0, length=1041, bytesSoFar=0, state=WAITING_TO_START}",
                "B2UploadProgress{partIndex=1, partCount=3, startByte=1041, length=1041, bytesSoFar=0, state=WAITING_TO_START}",
                "B2UploadProgress{partIndex=2, partCount=3, startByte=2082, length=1042, bytesSoFar=0, state=WAITING_TO_START}",
                "B2UploadProgress{partIndex=2, partCount=3, startByte=2082, length=1042, bytesSoFar=1042, state=SUCCEEDED}",
                "B2UploadProgress{partIndex=0, partCount=3, startByte=0, length=1041, bytesSoFar=0, state=STARTING}",
                "B2UploadProgress{partIndex=0, partCount=3, startByte=0, length=1041, bytesSoFar=1041, state=SUCCEEDED}",
                "B2UploadProgress{partIndex=1, partCount=3, startByte=1041, length=1041, bytesSoFar=0, state=STARTING}",
                "B2UploadProgress{partIndex=1, partCount=3, startByte=1041, length=1041, bytesSoFar=1041, state=SUCCEEDED}"
        );

        checkPartMatching(alreadyUploadedParts, 1, 2);
    }

    @Test
    public void testThreeAlreadyUploadedParts_allMatch() throws IOException, B2Exception {
        final String largeFileId = fileId(1);
        final List<B2Part> alreadyUploadedParts = B2Collections.listOf( // out-of-order on purpose.  should still be found.
                new B2Part(largeFileId, 3, 1042, makeSha1(3), 3333),
                new B2Part(largeFileId, 2, 1041, makeSha1(2), 2222),
                new B2Part(largeFileId, 1, 1041, makeSha1(1), 1111)
        );

        recordingListener.setExpected(
                "B2UploadProgress{partIndex=0, partCount=3, startByte=0, length=1041, bytesSoFar=0, state=WAITING_TO_START}",
                "B2UploadProgress{partIndex=0, partCount=3, startByte=0, length=1041, bytesSoFar=1041, state=SUCCEEDED}",
                "B2UploadProgress{partIndex=1, partCount=3, startByte=1041, length=1041, bytesSoFar=0, state=WAITING_TO_START}",
                "B2UploadProgress{partIndex=1, partCount=3, startByte=1041, length=1041, bytesSoFar=1041, state=SUCCEEDED}",
                "B2UploadProgress{partIndex=2, partCount=3, startByte=2082, length=1042, bytesSoFar=0, state=WAITING_TO_START}",
                "B2UploadProgress{partIndex=2, partCount=3, startByte=2082, length=1042, bytesSoFar=1042, state=SUCCEEDED}"
        );

        checkPartMatching(alreadyUploadedParts);
    }

    @Test
    public void testFourAlreadyUploadedPart_noneMatch() throws IOException, B2Exception {
        final String largeFileId = fileId(1);
        final List<B2Part> alreadyUploadedParts = B2Collections.listOf(
                new B2Part(largeFileId, 6, 1041, makeSha1(1), 1111),  // unneeded part number
                new B2Part(largeFileId, 2, 6666, makeSha1(2), 2222),  // bad size
                new B2Part(largeFileId, 3, 1041, makeSha1(3), 3333),  // bad size
                new B2Part(largeFileId, 4, 1041, makeSha1(4), 1111)   // unneeded part number
        );

        recordingListener.setExpected(
                "B2UploadProgress{partIndex=0, partCount=3, startByte=0, length=1041, bytesSoFar=0, state=WAITING_TO_START}",
                "B2UploadProgress{partIndex=1, partCount=3, startByte=1041, length=1041, bytesSoFar=0, state=WAITING_TO_START}",
                "B2UploadProgress{partIndex=2, partCount=3, startByte=2082, length=1042, bytesSoFar=0, state=WAITING_TO_START}",
                "B2UploadProgress{partIndex=0, partCount=3, startByte=0, length=1041, bytesSoFar=0, state=STARTING}",
                "B2UploadProgress{partIndex=0, partCount=3, startByte=0, length=1041, bytesSoFar=1041, state=SUCCEEDED}",
                "B2UploadProgress{partIndex=1, partCount=3, startByte=1041, length=1041, bytesSoFar=0, state=STARTING}",
                "B2UploadProgress{partIndex=1, partCount=3, startByte=1041, length=1041, bytesSoFar=1041, state=SUCCEEDED}",
                "B2UploadProgress{partIndex=2, partCount=3, startByte=2082, length=1042, bytesSoFar=0, state=STARTING}",
                "B2UploadProgress{partIndex=2, partCount=3, startByte=2082, length=1042, bytesSoFar=1042, state=SUCCEEDED}"
                );
        checkPartMatching(alreadyUploadedParts, 1, 2, 3);
    }


    private void checkPartMatching(List<B2Part> alreadyUploadedParts,
                                   Integer... expectedUploadPartNumbers) throws IOException, B2Exception {
        final long contentLen = (3 * ACCOUNT_AUTH.getRecommendedPartSize() + 124);
        final B2ContentSource contentSource = mock(B2ContentSource.class);
        when(contentSource.getContentLength()).thenReturn(contentLen);

        final String largeFileId = fileId(1);
        final B2FileVersion largeFileVersion = new B2FileVersion(largeFileId,
                fileName(1),
                contentLen,
                B2ContentTypes.APPLICATION_OCTET,
                null,
                B2Collections.mapOf(),
                "upload",
                B2Clock.get().wallClockMillis());


        // arrange to answer get_upload_part_url (which will be called several times, but it's ok to reuse the same value since it's all mocked!)
        final B2GetUploadPartUrlRequest partUrlRequest = new B2GetUploadPartUrlRequest(largeFileId);
        final B2UploadPartUrlResponse partUrl = new B2UploadPartUrlResponse(largeFileId, "uploadPartUrl", "uploadPartAuthToken");
        when(webifier.getUploadPartUrl(anyObject(), eq(partUrlRequest))).thenReturn(partUrl);

        // arrange to answer upload_part, based on the request.
        when(webifier.uploadPart(anyObject(), anyObject())).thenAnswer(invocationOnMock -> {
            B2UploadPartRequest request = (B2UploadPartRequest) invocationOnMock.getArguments()[1];
            return makePart(request.getPartNumber());
        });

        // arrange to answer finish_large_file
        final B2FinishLargeFileRequest finishRequest = new B2FinishLargeFileRequest(largeFileId, B2Collections.listOf(B2TestHelpers.SAMPLE_SHA1));
        when(webifier.finishLargeFile(anyObject(), eq(finishRequest))).thenReturn(largeFileVersion);

        final B2LargeFileUploader uploader = makeUploader(PART_SIZES, Executors.newSingleThreadExecutor(), contentSource, recordingListener);
        uploader.finishUploadingLargeFile(largeFileVersion, alreadyUploadedParts);

        // we should be using the existing largeFile, not starting a new one.
        verify(webifier, never()).startLargeFile(anyObject(), anyObject());

        // with the single-threaded executor, the uploadPartUrlCache should always have an upload url after the first time it's used.
        // of course, if we don't upload anything, we don't need any upload urls at all.
        final int expectedUploadPartCount = expectedUploadPartNumbers.length;
        final int numUploadUrlsNeeded = (expectedUploadPartCount == 0) ? 0 : 1;
        verify(webifier, times(numUploadUrlsNeeded)).getUploadPartUrl(anyObject(), anyObject());

        // check how many were uploaded & that it was the expected parts.
        final ArgumentCaptor<B2UploadPartRequest> uploadPartRequestCaptor = ArgumentCaptor.forClass(B2UploadPartRequest.class);
        verify(webifier, times(expectedUploadPartCount)).uploadPart(anyObject(), uploadPartRequestCaptor.capture());

        final Set<Integer> uploadedPartNumbers = uploadPartRequestCaptor.getAllValues().stream().map(B2UploadPartRequest::getPartNumber).collect(Collectors.toSet());
        assertEquals(expectedUploadPartCount, uploadedPartNumbers.size());
        assertEquals(B2Collections.unmodifiableSet(expectedUploadPartNumbers), uploadedPartNumbers);

        // make sure only call finish once & with the expected parts in it.
        final ArgumentCaptor<B2FinishLargeFileRequest> finishRequestCaptor = ArgumentCaptor.forClass(B2FinishLargeFileRequest.class);
        verify(webifier, times(1)).finishLargeFile(anyObject(), finishRequestCaptor.capture());

        final List<String> expectedSha1s = B2Collections.listOf(
            makeSha1(1),
            makeSha1(2),
            makeSha1(3)
        );
        final List<String> sha1s = finishRequestCaptor.getValue().getPartSha1Array();
        assertEquals(expectedSha1s, sha1s);
    }

    enum When {
        IN_SUBMIT,
        IN_GET,
        IN_GET_AFTER_RUNNING_IN_SUBMIT // running task during submit makes it more sequential.
    }
    enum ExceptionType {
        INTERRUPTED,
        B2_EXCEPTION,
        IO_EXCEPTION
    }

    private static class ThrowingFuture<T> implements Future<T> {
        private final ExceptionType exceptionType;

        private ThrowingFuture(ExceptionType exceptionType) {
            this.exceptionType = exceptionType;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return false;
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public boolean isDone() {
            return false;
        }

        @Override
        public T get() throws InterruptedException, ExecutionException {
            switch (exceptionType) {
                case INTERRUPTED: {
                    Thread.currentThread().interrupt();
                    Thread.sleep(10);
                    fail("we should've thrown by now");
                    break;
                }

                case B2_EXCEPTION:
                    throw new ExecutionException("wrapper", new B2InternalErrorException("test", "testing"));

                case IO_EXCEPTION:
                    throw new ExecutionException("wrapper", new IOException("testing"));
            }

            throw new RuntimeException("unexpected exceptionType " + exceptionType);
        }

        @Override
        public T get(long timeout,
                     TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            fail("not expecting this to be called.");
            return null;
        }
    }

    private static class ThrowingExecutor extends ThreadPoolExecutor {
        private final When when;
        private final ExceptionType exceptionType;

        private ThrowingExecutor(When when,
                                 ExceptionType exceptionType) {
            super(1, 10, 10, TimeUnit.MINUTES, new ArrayBlockingQueue<>(1));
            this.when = when;
            this.exceptionType = exceptionType;
        }
        @Override
        public <T> Future<T> submit(Callable<T> task) {
            if (when == When.IN_SUBMIT) {
                throw new RejectedExecutionException("testing");
            }
            if (when == When.IN_GET_AFTER_RUNNING_IN_SUBMIT) {
                try {
                    task.call();
                } catch (Exception e) {
                    throw new RuntimeException("not expecting this to happen!", e);
                }
            }
            return new ThrowingFuture<>(exceptionType);
        }
    }

    private B2LargeFileUploader makeUploader(B2ContentSource contentSource,
                                             B2UploadListener listener) throws IOException {
        final ExecutorService executor = mock(ExecutorService.class);
        return makeUploader(PART_SIZES, executor, contentSource, listener);
    }

    private B2LargeFileUploader makeUploader(B2PartSizes partSizes,
                                             ExecutorService executor,
                                             B2ContentSource contentSource,
                                             B2UploadListener listener) throws IOException {
        final B2UploadFileRequest request = B2UploadFileRequest
                .builder(bucketId(1), fileName(1), B2ContentTypes.APPLICATION_OCTET, contentSource)
                .setListener(listener)
                .build();

        return new B2LargeFileUploader(
                retryer,
                webifier,
                accountAuthCache,
                B2DefaultRetryPolicy.supplier(),
                executor,
                partSizes,
                request,
                contentSource.getContentLength());

    }
}
