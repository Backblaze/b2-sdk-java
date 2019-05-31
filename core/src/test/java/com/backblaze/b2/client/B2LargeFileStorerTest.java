/*
 * Copyright 2019, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client;

import com.backblaze.b2.client.contentSources.B2ContentSource;
import com.backblaze.b2.client.exceptions.B2Exception;
import com.backblaze.b2.client.exceptions.B2InternalErrorException;
import com.backblaze.b2.client.structures.B2AccountAuthorization;
import com.backblaze.b2.client.structures.B2FileVersion;
import com.backblaze.b2.client.structures.B2FinishLargeFileRequest;
import com.backblaze.b2.client.structures.B2Part;
import com.backblaze.b2.client.structures.B2UploadPartUrlResponse;
import org.junit.After;
import org.junit.Test;
import org.mockito.Matchers;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

import static com.backblaze.b2.client.B2TestHelpers.fileId;
import static com.backblaze.b2.client.B2TestHelpers.makeSha1;
import static com.backblaze.b2.client.B2TestHelpers.makeVersion;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
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
public class B2LargeFileStorerTest {

    private static final long FIVE_MEGABYTES = 5000000;

    private static final long FILE_SIZE = 16000000;
    private static final long PART_COUNT = 3;
    private static final long PART_SIZE_FOR_FIRST_TWO = FILE_SIZE / PART_COUNT;
    private static final long LAST_PART_SIZE = FILE_SIZE - (PART_COUNT - 1) * PART_SIZE_FOR_FIRST_TWO;

    // The file version of the large file we are creating.
    private final B2FileVersion largeFileVersion = makeVersion(4, 4);

    // Set up the parts of the large file.
    private final B2Part part1 = new B2Part(fileId(1), 1, PART_SIZE_FOR_FIRST_TWO, makeSha1(1), 1111);
    private final B2Part part2 = new B2Part(fileId(2), 2, PART_SIZE_FOR_FIRST_TWO, makeSha1(2), 2222);
    private final B2Part part3 = new B2Part(fileId(3), 3, PART_SIZE_FOR_FIRST_TWO, makeSha1(3), 3333);


    private final B2PartSizes partSizes;
    private final B2AccountAuthorizationCache authCache = mock(B2AccountAuthorizationCache.class);
    private final B2StorageClientWebifier webifier = mock(B2StorageClientWebifier.class);

    private final B2Retryer retryer = new B2Retryer(mock(B2Sleeper.class));
    private final Supplier<B2RetryPolicy> retryPolicySupplier = B2DefaultRetryPolicy.supplier();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

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

    public void storeFile() throws B2Exception {
        final List<B2PartStorer> partStorers = new ArrayList<>();
        partStorers.add(new B2UploadingPartStorer(1, mock(B2ContentSource.class)));

        final String copySourceFile = fileId(1);
        partStorers.add(new B2CopyingPartStorer(2, copySourceFile));

        partStorers.add(new B2AlreadyStoredPartStorer(part3));

        final B2LargeFileStorer largeFileStorer = new B2LargeFileStorer(
                largeFileVersion,
                partStorers,
                authCache,
                webifier,
                retryer,
                retryPolicySupplier,
                executor);

        assertEquals(partStorers, largeFileStorer.getPartStorers());

        largeFileStorer.storeFile();
    }

    @Test
    public void testStoreFile_success() throws B2Exception {
        storeFile();

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
    }

    @Test
    public void testStoreFile_cannotUpload() throws B2Exception {
        when(webifier.uploadPart(anyObject(), anyObject())).thenThrow(new B2InternalErrorException("error"));

        try {
           storeFile();
           fail("should have thrown");
        } catch (B2InternalErrorException e) {
            // Make sure retries work
            verify(webifier, times(8)).uploadPart(anyObject(), anyObject());
            verify(webifier, times(0)).finishLargeFile(anyObject(), anyObject());
        } catch (Exception e) {
           fail("should have thrown B2InternalErrorException");
        }
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
    class TestContentSource implements B2ContentSource {

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
            throw new NotImplementedException();
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

}