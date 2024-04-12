/*
 * Copyright 2019, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client;

import com.backblaze.b2.client.contentSources.B2ContentSource;
import com.backblaze.b2.client.exceptions.B2Exception;
import com.backblaze.b2.client.exceptions.B2InternalErrorException;
import com.backblaze.b2.client.structures.B2AccountAuthorization;
import com.backblaze.b2.client.structures.B2FileSseForRequest;
import com.backblaze.b2.client.structures.B2FileVersion;
import com.backblaze.b2.client.structures.B2FinishLargeFileRequest;
import com.backblaze.b2.client.structures.B2Part;
import com.backblaze.b2.client.structures.B2StoreLargeFileRequest;
import com.backblaze.b2.client.structures.B2UploadListener;
import com.backblaze.b2.client.structures.B2UploadPartUrlResponse;
import com.backblaze.b2.client.structures.B2UploadProgress;
import com.backblaze.b2.client.structures.B2UploadState;
import com.backblaze.b2.util.B2BaseTest;
import org.junit.*;
import org.junit.rules.ExpectedException;
import org.mockito.Matchers;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.backblaze.b2.client.B2TestHelpers.fileId;
import static com.backblaze.b2.client.B2TestHelpers.makeMd5;
import static com.backblaze.b2.client.B2TestHelpers.makeSha1;
import static com.backblaze.b2.client.B2TestHelpers.makeVersion;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/*
 * Copyright 2019, Backblaze, Inc. All rights reserved.
 */
public class B2LargeFileStorerTest extends B2BaseTest {

    private static final long FIVE_MEGABYTES = 5000000;

    private static final long FILE_SIZE = 16000000;
    private static final long PART_COUNT = 3;
    private static final long PART_SIZE_FOR_FIRST_TWO = FILE_SIZE / PART_COUNT;
    private static final long LAST_PART_SIZE = FILE_SIZE - (PART_COUNT - 1) * PART_SIZE_FOR_FIRST_TWO;

    // The file version of the large file we are creating.
    private final B2FileVersion largeFileVersion = makeVersion(4, 4);

    // Set up the parts of the large file.
    private final B2Part part1 = new B2Part(fileId(1), 1, PART_SIZE_FOR_FIRST_TWO, makeSha1(1), makeMd5(1), 1111, null);
    private final B2Part part2 = new B2Part(fileId(2), 2, PART_SIZE_FOR_FIRST_TWO, makeSha1(2), makeMd5(2), 2222, null);
    private final B2Part part3 = new B2Part(fileId(3), 3, LAST_PART_SIZE, makeSha1(3), makeMd5(3), 3333, null);


    private final B2PartSizes partSizes;
    private final B2AccountAuthorizationCache authCache = mock(B2AccountAuthorizationCache.class);
    private final B2StorageClientWebifier webifier = mock(B2StorageClientWebifier.class);

    private final B2Retryer retryer = new B2Retryer(mock(B2Sleeper.class));
    private final Supplier<B2RetryPolicy> retryPolicySupplier = B2DefaultRetryPolicy.supplier();
    // Use an executor that has a predictable order of events.
    private final ExecutorService executor = new ExecutorThatUsesMainThread();
    private final ExecutorService singleThreadedExecutor = Executors.newSingleThreadExecutor();
    private final B2UploadListener uploadListenerMock = mock(B2UploadListener.class);

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    public B2LargeFileStorerTest() throws B2Exception {
        final B2AccountAuthorization accountAuth = mock(B2AccountAuthorization.class);
        when(accountAuth.getAbsoluteMinimumPartSize()).thenReturn(FIVE_MEGABYTES);
        when(accountAuth.getRecommendedPartSize()).thenReturn(FIVE_MEGABYTES);
        partSizes = B2PartSizes.from(accountAuth);

        when(webifier.getUploadPartUrl(any(), any())).thenReturn(mock(B2UploadPartUrlResponse.class));
        when(webifier.uploadPart(any(), any())).thenReturn(part1);
        when(webifier.copyPart(any(), any())).thenReturn(part2);
    }

    @After
    public void tearDown() {
        executor.shutdown();
        singleThreadedExecutor.shutdown();
    }

    public List<B2PartStorer> createB2LargeFileStorerAndGetSortedPartStorers(List<B2PartStorer> outOfOrderPartStorers) {
        return createB2LargeFileStorerAndGetSortedPartStorers(outOfOrderPartStorers, false);
    }

    public List<B2PartStorer> createB2LargeFileStorerAndGetSortedPartStorers(List<B2PartStorer> outOfOrderPartStorers,
                                                                             boolean partNumberGapsAllowed) {
        return new B2LargeFileStorer(
                B2StoreLargeFileRequest.builder(largeFileVersion.getFileId()).build(),
                outOfOrderPartStorers,
                authCache,
                webifier,
                retryer,
                retryPolicySupplier,
                singleThreadedExecutor,
                partNumberGapsAllowed).getPartStorers();
    }

    @Test
    public void testOutOfOrderPartStorers() throws IOException {
        final List<B2PartStorer> partStorers = Arrays.asList(
                new B2AlreadyStoredPartStorer(part2),
                new B2UploadingPartStorer(1, createContentSourceWithSize(100)),
                new B2CopyingPartStorer(3, fileId(3)));

        final List<B2PartStorer> sortedPartStorers = createB2LargeFileStorerAndGetSortedPartStorers(partStorers);

        assertEquals(
                Arrays.asList(1, 2, 3),
                sortedPartStorers.stream().map(B2PartStorer::getPartNumber).collect(Collectors.toList())
        );
    }

    @Test
    public void testPartStorers_duplicatePartNumber() throws IOException {
        final List<B2PartStorer> partStorers = Arrays.asList(
                new B2AlreadyStoredPartStorer(part2),
                new B2UploadingPartStorer(1, createContentSourceWithSize(100)),
                new B2CopyingPartStorer(2, fileId(3)));

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("part number 2 has multiple part storers");

        createB2LargeFileStorerAndGetSortedPartStorers(partStorers);
    }

    @Test
    public void testPartStorers_missingPartNumber() throws IOException {
        final List<B2PartStorer> partStorers = Arrays.asList(
                new B2AlreadyStoredPartStorer(part2),
                new B2UploadingPartStorer(1, createContentSourceWithSize(100)),
                new B2CopyingPartStorer(4, fileId(4)));

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("part number 3 has no part storers");

        createB2LargeFileStorerAndGetSortedPartStorers(partStorers);
    }

    @Test
    public void testPartStorers_missingPartNumber_gapsAllowed() throws IOException {
        final List<B2PartStorer> partStorers = Arrays.asList(
                new B2AlreadyStoredPartStorer(part2),
                new B2UploadingPartStorer(1, createContentSourceWithSize(100)),
                new B2CopyingPartStorer(4, fileId(4)));

        final List<B2PartStorer> sortedPartStorers = createB2LargeFileStorerAndGetSortedPartStorers(partStorers, true);
        assertEquals(
                Arrays.asList(1, 2, 4),
                sortedPartStorers.stream().map(B2PartStorer::getPartNumber).collect(Collectors.toList())
        );
    }

    @Test
    public void testPartStorers_partNumbersStartWithTwo() {
        final List<B2PartStorer> partStorers = Arrays.asList(
                new B2AlreadyStoredPartStorer(part2),
                new B2AlreadyStoredPartStorer(part3),
                new B2CopyingPartStorer(4, fileId(4)));

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("part number 1 has no part storers");

        createB2LargeFileStorerAndGetSortedPartStorers(partStorers);
    }

    @Test
    public void testPartStorers_partNumbersStartWithTwo_gapsAllowed() {
        final List<B2PartStorer> partStorers = Arrays.asList(
                new B2AlreadyStoredPartStorer(part2),
                new B2AlreadyStoredPartStorer(part3),
                new B2CopyingPartStorer(4, fileId(4)));

        final List<B2PartStorer> sortedPartStorers = createB2LargeFileStorerAndGetSortedPartStorers(partStorers, true);
        assertEquals(
                Arrays.asList(2, 3, 4),
                sortedPartStorers.stream().map(B2PartStorer::getPartNumber).collect(Collectors.toList())
        );
    }

    private B2LargeFileStorer createFromLocalContent() throws B2Exception {
        final B2ContentSource contentSource = new TestContentSource(0, FILE_SIZE);

        return B2LargeFileStorer.forLocalContent(
                largeFileVersion,
                contentSource,
                partSizes,
                authCache,
                webifier,
                retryer,
                retryPolicySupplier,
                executor);

    }

    private B2ContentSource createContentSourceWithSize(long size) throws IOException {
        final B2ContentSource contentSource = mock(B2ContentSource.class);
        when(contentSource.getContentLength()).thenReturn(size);
        return contentSource;
    }

    private B2LargeFileStorer createLargeFileStorerForStartByteTests(boolean partNumberGapsAllowed) throws IOException {
        final List<B2PartStorer> partStorers = Arrays.asList(
                new B2UploadingPartStorer(1, createContentSourceWithSize(100)),
                new B2AlreadyStoredPartStorer(part2),
                new B2CopyingPartStorer(3, fileId(3)),
                new B2UploadingPartStorer(4, createContentSourceWithSize(900)));

        return new B2LargeFileStorer(
                B2StoreLargeFileRequest.builder(largeFileVersion.getFileId()).build(),
                partStorers,
                authCache,
                webifier,
                retryer,
                retryPolicySupplier,
                executor,
                partNumberGapsAllowed);
    }

    @Test
    public void testStartByte() throws IOException {
        final B2LargeFileStorer largeFileStorer = createLargeFileStorerForStartByteTests(false);

        assertEquals(0, largeFileStorer.getStartByteOrUnknown(1));
        assertEquals(100, largeFileStorer.getStartByteOrUnknown(2));
        assertEquals(100 + PART_SIZE_FOR_FIRST_TWO, largeFileStorer.getStartByteOrUnknown(3));
        assertEquals(B2UploadProgress.UNKNOWN_PART_START_BYTE, largeFileStorer.getStartByteOrUnknown(4));
    }

    @Test
    public void testStartByte_partNumberGapsAllowed() throws IOException {
        final B2LargeFileStorer largeFileStorer = createLargeFileStorerForStartByteTests(true);

        assertEquals(0, largeFileStorer.getStartByteOrUnknown(1));
        assertEquals(100, largeFileStorer.getStartByteOrUnknown(2));
        assertEquals(100 + PART_SIZE_FOR_FIRST_TWO, largeFileStorer.getStartByteOrUnknown(3));
        assertEquals(B2UploadProgress.UNKNOWN_PART_START_BYTE, largeFileStorer.getStartByteOrUnknown(4));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testStartByte_partNumberTooLow() throws IOException {
        createLargeFileStorerForStartByteTests(false).getStartByteOrUnknown(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testStartByte_partNumberTooLow_partNumberGapsAllowed() throws IOException {
        createLargeFileStorerForStartByteTests(true).getStartByteOrUnknown(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testStartByte_partNumberTooHigh() throws IOException {
        createLargeFileStorerForStartByteTests(false).getStartByteOrUnknown(5);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testStartByte_partNumberTooHigh_partNumberGapsAllowed() throws IOException {
        createLargeFileStorerForStartByteTests(true).getStartByteOrUnknown(5);
    }

    @Test
    public void testForLocalContent() throws B2Exception {
        final B2LargeFileStorer largeFileStorer = createFromLocalContent();

        List<B2PartStorer> partStorers = largeFileStorer.getPartStorers();
        assertEquals(3, partStorers.size());
        assertEquals(
                new B2UploadingPartStorer(1, new TestContentSource(0, PART_SIZE_FOR_FIRST_TWO, false)),
                partStorers.get(0));
        assertEquals(
                new B2UploadingPartStorer(2, new TestContentSource(PART_SIZE_FOR_FIRST_TWO, PART_SIZE_FOR_FIRST_TWO, false)),
                partStorers.get(1));
        assertEquals(
                new B2UploadingPartStorer(3, new TestContentSource(2 * PART_SIZE_FOR_FIRST_TWO, LAST_PART_SIZE, false)),
                partStorers.get(2));
    }

    private void storeFile(B2UploadListener uploadListener) throws IOException, B2Exception {
        final List<B2PartStorer> partStorers = new ArrayList<>();
        final B2ContentSource contentSourceForPart1 = mock(B2ContentSource.class);
        when(contentSourceForPart1.getContentLength()).thenReturn(PART_SIZE_FOR_FIRST_TWO);
        partStorers.add(new B2UploadingPartStorer(1, contentSourceForPart1));

        final String copySourceFile = fileId(1);
        partStorers.add(new B2CopyingPartStorer(2, copySourceFile));

        partStorers.add(new B2AlreadyStoredPartStorer(part3));

        final B2LargeFileStorer largeFileStorer = new B2LargeFileStorer(
                B2StoreLargeFileRequest.builder(largeFileVersion.getFileId()).build(),
                partStorers,
                authCache,
                webifier,
                retryer,
                retryPolicySupplier,
                executor,
                false);

        assertEquals(partStorers, largeFileStorer.getPartStorers());

        largeFileStorer.storeFile(uploadListener);
    }

    private void storeFileAsync(B2UploadListener uploadListener) throws IOException, ExecutionException, InterruptedException {
        final List<B2PartStorer> partStorers = new ArrayList<>();
        final B2ContentSource contentSourceForPart1 = mock(B2ContentSource.class);
        when(contentSourceForPart1.getContentLength()).thenReturn(PART_SIZE_FOR_FIRST_TWO);
        partStorers.add(new B2UploadingPartStorer(1, contentSourceForPart1));

        final String copySourceFile = fileId(1);
        partStorers.add(new B2CopyingPartStorer(2, copySourceFile));

        partStorers.add(new B2AlreadyStoredPartStorer(part3));

        final B2LargeFileStorer largeFileStorer = new B2LargeFileStorer(
                B2StoreLargeFileRequest.builder(largeFileVersion.getFileId()).build(),
                partStorers,
                authCache,
                webifier,
                retryer,
                retryPolicySupplier,
                executor,
                false);

        assertEquals(partStorers, largeFileStorer.getPartStorers());

        largeFileStorer.storeFileAsync(uploadListener).get();
    }

    @Test
    public void testStoreFile_success() throws IOException, B2Exception {
        storeFile(uploadListenerMock);

        verifySuccessfulUpload();
    }

    @Test
    public void testStoreFileAsync_success() throws IOException, B2Exception, ExecutionException, InterruptedException {
        storeFileAsync(uploadListenerMock);

        verifySuccessfulUpload();
    }

    private void verifySuccessfulUpload() throws B2Exception {
        // Checks for the part that is uploaded.
        verify(webifier).getUploadPartUrl(anyObject(), anyObject());
        verify(webifier).uploadPart(anyObject(), anyObject());

        // Checks for the part that is copied.
        verify(webifier).copyPart(anyObject(), anyObject());

        // The part that is already stored doesn't activate the webifier.

        // Check that the large file storer calls finish file
        verify(webifier).finishLargeFile(
                anyObject(),
                Matchers.eq(new B2FinishLargeFileRequest(
                        largeFileVersion.getFileId(),
                        Arrays.asList(part1.getContentSha1(), part2.getContentSha1(), part3.getContentSha1())))
        );

        // Make sure progress events are as expected.
        // There should be 2 WAITING_TO_START from the uploading and copying part storers
        // There should be 2 STARTING from the uploading and copying part storers
        // There should be 3 SUCCEEDED (one for each part)
        // Things to note: for copies we don't really know the number of bytes that will be copied. Even if a byte range
        // is supplied in the copy, the range may exceed the file's size, and so it may get clamped during the actual
        // copy operation. We use 1 byte as the placeholder until the copy succeeds, then use the result to update the
        // real part size.
        verify(uploadListenerMock).progress(
                new B2UploadProgress(0, 3, 0, PART_SIZE_FOR_FIRST_TWO, 0, B2UploadState.WAITING_TO_START));
        verify(uploadListenerMock).progress(
                new B2UploadProgress(0, 3, 0, PART_SIZE_FOR_FIRST_TWO, 0, B2UploadState.STARTING));
        verify(uploadListenerMock).progress(
                new B2UploadProgress(0, 3, 0, PART_SIZE_FOR_FIRST_TWO, PART_SIZE_FOR_FIRST_TWO, B2UploadState.SUCCEEDED));
        verify(uploadListenerMock).progress(
                new B2UploadProgress(1, 3, PART_SIZE_FOR_FIRST_TWO, 1, 0, B2UploadState.WAITING_TO_START));
        verify(uploadListenerMock).progress(
                new B2UploadProgress(1, 3, PART_SIZE_FOR_FIRST_TWO, 1, 0, B2UploadState.STARTING));
        verify(uploadListenerMock).progress(
                new B2UploadProgress(
                        1,
                        3,
                        PART_SIZE_FOR_FIRST_TWO,
                        PART_SIZE_FOR_FIRST_TWO,
                        PART_SIZE_FOR_FIRST_TWO,
                        B2UploadState.SUCCEEDED));
        verify(uploadListenerMock).progress(
                new B2UploadProgress(2, 3, B2UploadProgress.UNKNOWN_PART_START_BYTE, LAST_PART_SIZE, LAST_PART_SIZE, B2UploadState.SUCCEEDED));
    }

    @Test
    public void testStoreFile_cannotUpload() throws B2Exception {
        when(webifier.uploadPart(anyObject(), anyObject())).thenThrow(new B2InternalErrorException("error"));

        try {
           storeFile(uploadListenerMock);
           fail("should have thrown");
        } catch (B2InternalErrorException e) {
            checkCannotUpload();
        } catch (Exception e) {
           fail("should have thrown B2InternalErrorException");
        }
    }

    @Test
    public void testStoreFileAsync_cannotUpload() throws B2Exception {
        when(webifier.uploadPart(anyObject(), anyObject())).thenThrow(new B2InternalErrorException("error"));

        try {
            storeFileAsync(uploadListenerMock);
            fail("should have thrown");
        } catch (ExecutionException e) {
            assertEquals(B2InternalErrorException.class, e.getCause().getClass());

            checkCannotUpload();
        } catch (Exception e) {
            fail("should have thrown ExecutionException");
        }
    }

    private void checkCannotUpload() throws B2Exception {

        // Make sure retries work
        verify(webifier, times(8)).uploadPart(anyObject(), anyObject());
        verify(webifier, times(0)).finishLargeFile(anyObject(), anyObject());

        // Make sure progress events are as expected.
        // There should be 2 WAITING_TO_START from the uploading and copying part storers
        // There should be 2 STARTING from the uploading and copying part storers
        // There should be 1 FAILED (for the part that uploads)
        // There should be 2 SUCCEEDED (one for each remaining part)
        // Things to note: for copies we don't really know the number of bytes that will be copied. Even if a byte range
        // is supplied in the copy, the range may exceed the file's size, and so it may get clamped during the actual
        // copy operation. We use 1 byte as the placeholder until the copy succeeds, then use the result to update the
        // real part size.
        verify(uploadListenerMock).progress(
                new B2UploadProgress(0, 3, 0, PART_SIZE_FOR_FIRST_TWO, 0, B2UploadState.WAITING_TO_START));
        verify(uploadListenerMock, times(8)).progress(
                new B2UploadProgress(0, 3, 0, PART_SIZE_FOR_FIRST_TWO, 0, B2UploadState.STARTING));
        verify(uploadListenerMock).progress(
                new B2UploadProgress(0, 3, 0, PART_SIZE_FOR_FIRST_TWO, 0, B2UploadState.FAILED));
        verify(uploadListenerMock).progress(
                new B2UploadProgress(1, 3, PART_SIZE_FOR_FIRST_TWO, 1, 0, B2UploadState.WAITING_TO_START));
        verify(uploadListenerMock).progress(
                new B2UploadProgress(1, 3, PART_SIZE_FOR_FIRST_TWO, 1, 0, B2UploadState.STARTING));
        verify(uploadListenerMock).progress(
                new B2UploadProgress(
                        1,
                        3,
                        PART_SIZE_FOR_FIRST_TWO,
                        PART_SIZE_FOR_FIRST_TWO,
                        PART_SIZE_FOR_FIRST_TWO,
                        B2UploadState.SUCCEEDED));
        verify(uploadListenerMock).progress(
                new B2UploadProgress(
                        2,
                        3,
                        B2UploadProgress.UNKNOWN_PART_START_BYTE,
                        LAST_PART_SIZE,
                        LAST_PART_SIZE,
                        B2UploadState.SUCCEEDED));
    }

    @Test
    public void testStoreFileAsyncCancelled() throws B2Exception, IOException {
        final List<B2PartStorer> partStorers = new ArrayList<>();
        final B2ContentSource contentSourceForPart1 = mock(B2ContentSource.class);
        final B2ContentSource contentSourceForPart2 = mock(B2ContentSource.class);

        final AtomicReference<CompletableFuture<B2FileVersion>> future = new AtomicReference<>();

        when(contentSourceForPart1.getContentLength()).thenReturn(PART_SIZE_FOR_FIRST_TWO);
        when(contentSourceForPart2.getContentLength()).thenReturn(PART_SIZE_FOR_FIRST_TWO);

        // when the first call to uploadPart is called, we will cancel the request
        when(webifier.uploadPart(any(), any())).thenAnswer(invocation -> {
            // this method could be called before value is assigned to future, so we need to use
            // waitForReferenceValue() instead of directly calling future.get()
            waitForReferenceValue(future).cancel(true);
            return mock(B2Part.class);
        });

        partStorers.add(new B2UploadingPartStorer(1, contentSourceForPart1));
        partStorers.add(new B2UploadingPartStorer(2, contentSourceForPart2));
        partStorers.add(new B2AlreadyStoredPartStorer(part3));

        final B2LargeFileStorer largeFileStorer = new B2LargeFileStorer(
                B2StoreLargeFileRequest.builder(largeFileVersion.getFileId()).build(),
                partStorers,
                authCache,
                webifier,
                retryer,
                retryPolicySupplier,
                singleThreadedExecutor,
                false);

        assertEquals(partStorers, largeFileStorer.getPartStorers());

        future.set(largeFileStorer.storeFileAsync(uploadListenerMock));

        try {
            future.get().get();
            Assert.fail("we should have gotten a CancellationException");
        } catch (CancellationException e) {
            // upload part should only be called once
            verify(webifier, times(1)).uploadPart(anyObject(), anyObject());
            verify(webifier, times(0)).finishLargeFile(anyObject(), anyObject());
        } catch (Exception e) {
            Assert.fail("we should have gotten a CancellationException");
        }
    }

    @Test
    public void testStorePartsAsyncCancelled() throws B2Exception, IOException {
        final List<B2PartStorer> partStorers = new ArrayList<>();
        final B2ContentSource contentSourceForPart1 = mock(B2ContentSource.class);
        final B2ContentSource contentSourceForPart2 = mock(B2ContentSource.class);

        final AtomicReference<CompletableFuture<List<B2Part>>> future = new AtomicReference<>();

        when(contentSourceForPart1.getContentLength()).thenReturn(PART_SIZE_FOR_FIRST_TWO);
        when(contentSourceForPart2.getContentLength()).thenReturn(PART_SIZE_FOR_FIRST_TWO);

        // when the first call to uploadPart is called, we will cancel the request
        when(webifier.uploadPart(any(), any())).thenAnswer(invocation -> {
            // this method could be called before value is assigned to future, so we need to use
            // waitForReferenceValue() instead of directly calling future.get()
            waitForReferenceValue(future).cancel(true);
            return mock(B2Part.class);
        });

        partStorers.add(new B2UploadingPartStorer(1, contentSourceForPart1));
        partStorers.add(new B2UploadingPartStorer(2, contentSourceForPart2));
        partStorers.add(new B2AlreadyStoredPartStorer(part3));

        final B2LargeFileStorer largeFileStorer = new B2LargeFileStorer(
                B2StoreLargeFileRequest.builder(largeFileVersion.getFileId()).build(),
                partStorers,
                authCache,
                webifier,
                retryer,
                retryPolicySupplier,
                singleThreadedExecutor,
                false);

        assertEquals(partStorers, largeFileStorer.getPartStorers());

        future.set(largeFileStorer.storePartsAsync(uploadListenerMock));

        try {
            future.get().get();
            Assert.fail("we should have gotten a CancellationException");
        } catch (CancellationException e) {
            // upload part should only be called once
            verify(webifier, times(1)).uploadPart(anyObject(), anyObject());
        } catch (Exception e) {
            Assert.fail("we should have gotten a CancellationException");
        }
    }

    @Test
    public void testStorePartsAsyncCannotUpload_SingleThreaded()
            throws B2Exception, IOException, InterruptedException {
        testStorePartsAsyncCannotUpload(singleThreadedExecutor);
    }

    @Test
    public void testStorePartsAsyncCannotUpload_MultiThreaded()
            throws B2Exception, IOException, InterruptedException {
        testStorePartsAsyncCannotUpload(Executors.newFixedThreadPool(3));
    }

    private void testStorePartsAsyncCannotUpload(ExecutorService executor)
            throws B2Exception, IOException, InterruptedException {
        final List<B2PartStorer> partStorers = new ArrayList<>();
        final B2ContentSource contentSourceForPart = mock(B2ContentSource.class);

        when(contentSourceForPart.getContentLength()).thenReturn(FIVE_MEGABYTES);

        // every time uploadPart is called, we throw an exception to simulate a failed upload
        when(webifier.uploadPart(any(), any())).thenThrow(new B2InternalErrorException("test"));

        partStorers.add(new B2UploadingPartStorer(1, contentSourceForPart));
        partStorers.add(new B2UploadingPartStorer(2, contentSourceForPart));
        partStorers.add(new B2UploadingPartStorer(3, contentSourceForPart));

        final B2LargeFileStorer largeFileStorer = new B2LargeFileStorer(
                B2StoreLargeFileRequest.builder(largeFileVersion.getFileId()).build(),
                partStorers,
                authCache,
                webifier,
                retryer,
                retryPolicySupplier,
                executor,
                false);

        assertEquals(partStorers, largeFileStorer.getPartStorers());

        try {
            largeFileStorer.storePartsAsync(uploadListenerMock).get();
            Assert.fail("should have thrown ExecutionException");
        } catch (ExecutionException e) {
            assertEquals(B2InternalErrorException.class, e.getCause().getClass());
            // all three parts should be attempted even though each fails (8x each because of default retry policy)
            verify(webifier, times(24)).uploadPart(anyObject(), anyObject());
        }
    }

    @Test
    public void testStoreLargeFileRequest_withSseB2_throwsIllegalArgumentException() {
        thrown.expect(IllegalArgumentException.class);

        B2StoreLargeFileRequest.builder(largeFileVersion.getFileId())
                .setServerSideEncryption(B2FileSseForRequest.createSseB2Aes256())
                .build();
    }

    @Test
    public void testCreateRangedContentSource() throws IOException {
        final B2ContentSource contentSource = new TestContentSource(100, 1000);

        final B2ContentSource firstRangedContentSource = B2LargeFileStorer.createRangedContentSource(
                contentSource, 5, 200);
        assertTrue(firstRangedContentSource instanceof TestContentSource);

        final B2ContentSource secondRangedContentSource = B2LargeFileStorer.createRangedContentSource(
                firstRangedContentSource, 10, 50);
        assertTrue(secondRangedContentSource instanceof B2PartOfContentSource);

    }

    /**
     * A content source that can be ranged once.
     */
    static class TestContentSource implements B2ContentSource {

        private final long start;
        private final long length;
        private final boolean canCreateRanges;

        TestContentSource(long start, long length) {
            this(start, length, true);
        }

        private TestContentSource(long start, long length, boolean canCreateRanges) {
            this.start = start;
            this.length = length;
            this.canCreateRanges = canCreateRanges;
        }

        @Override
        public B2ContentSource createContentSourceWithRangeOrNull(long start, long length) {
            if (!canCreateRanges) {
                return null;
            }

            // Assume the new bounds are fully contained within the original...
            return new TestContentSource(this.start + start, length, false);
        }

        @Override
        public long getContentLength() {
            return length;
        }

        @Override
        public String getSha1OrNull() {
            return null;
        }

        @Override
        public Long getSrcLastModifiedMillisOrNull() {
            return null;
        }

        @Override
        public InputStream createInputStream() {
            // will not get called
            return null;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TestContentSource that = (TestContentSource) o;
            return start == that.start &&
                    length == that.length &&
                    canCreateRanges == that.canCreateRanges;
        }
    }

    /**
     * An executor that runs tasks in the order they are submitted. It accomplishes this by running them in the main
     * thread. This is done so our assertions can assume a specific order of progress events. This works because the
     * tasks are all independent of each other and the main thread has no work to do while the tasks are running.
     */
    private static class ExecutorThatUsesMainThread extends AbstractExecutorService {
        @Override
        public void shutdown() {
        }

        @Override
        public List<Runnable> shutdownNow() {
            return new ArrayList<>();
        }

        @Override
        public boolean isShutdown() {
            return false;
        }

        @Override
        public boolean isTerminated() {
            return false;
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) {
            return false;
        }

        @Override
        public void execute(Runnable command) {
            command.run();
        }
    }

    /**
     * Repeatedly sleeps and retries until the value of reference.get() is non-null, and then returns that
     * value.
     */
    private <T> T waitForReferenceValue(AtomicReference<T> reference) {
        final long startTime = System.currentTimeMillis();
        T value = reference.get();
        while (value == null) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                throw new RuntimeException("Interrupted", e);
            }
            // just in case, let's set a 30s timeout so that we don't spin forever waiting for a reference value
            if (System.currentTimeMillis() - startTime > 30_000) {
                throw new RuntimeException("timed out while waiting for reference to be set!");
            }

            value = reference.get();
        }
        return value;
    }
}
