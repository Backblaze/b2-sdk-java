/*
 * Copyright 2019, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client;

import com.backblaze.b2.client.contentSources.B2ContentSource;
import com.backblaze.b2.client.exceptions.B2Exception;
import com.backblaze.b2.client.exceptions.B2LocalException;
import com.backblaze.b2.client.structures.B2CopyPartRequest;
import com.backblaze.b2.client.structures.B2FileVersion;
import com.backblaze.b2.client.structures.B2FinishLargeFileRequest;
import com.backblaze.b2.client.structures.B2Part;
import com.backblaze.b2.client.structures.B2UploadPartRequest;
import com.backblaze.b2.client.structures.B2UploadPartUrlResponse;
import com.backblaze.b2.util.B2ByteRange;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Supplier;

/**
 * A class for handling the creation of large files.
 *
 * @see B2StorageClient#storeLargeFile(B2FileVersion, List, ExecutorService)
 * for a description of the various ways large files can be created
 * and the uses cases supported.
 */
public class B2LargeFileStorer {

    /**
     * The B2FileVersion for the large file that is being created.
     */
    private final B2FileVersion fileVersion;

    /**
     * The parts that need to be stored before finishing the large file.
     */
    private final List<B2PartStorer> partStorers;

    private final B2AccountAuthorizationCache accountAuthCache;
    private final B2UploadPartUrlCache uploadPartUrlCache;
    private final B2StorageClientWebifier webifier;
    private final B2Retryer retryer;
    private final Supplier<B2RetryPolicy> retryPolicySupplier;
    private final ExecutorService executor;

    B2LargeFileStorer(
            B2FileVersion fileVersion,
            List<B2PartStorer> partStorers,
            B2AccountAuthorizationCache accountAuthCache,
            B2StorageClientWebifier webifier,
            B2Retryer retryer,
            Supplier<B2RetryPolicy> retryPolicySupplier,
            ExecutorService executor) {

        this.fileVersion = fileVersion;
        this.partStorers = partStorers;

        this.accountAuthCache = accountAuthCache;
        this.uploadPartUrlCache = new B2UploadPartUrlCache(webifier, accountAuthCache, fileVersion.getFileId());
        this.webifier = webifier;
        this.retryer = retryer;
        this.retryPolicySupplier = retryPolicySupplier;
        this.executor = executor;
    }

    List<B2PartStorer> getPartStorers() {
        return partStorers;
    }

    public static B2LargeFileStorer forLocalContent(
            B2FileVersion largeFileVersion,
            B2ContentSource contentSource,
            B2PartSizes partSizes,
            B2AccountAuthorizationCache accountAuthCache,
            B2StorageClientWebifier webifier,
            B2Retryer retryer,
            Supplier<B2RetryPolicy> retryPolicySupplier,
            ExecutorService executor) throws B2Exception {

        // Convert the contentSource into a list of B2PartStorer objects.
        final List<B2PartStorer> partContentSources = new ArrayList<>();
        try {
            for (final B2PartSpec partSpec : partSizes.pickParts(contentSource.getContentLength())) {
                final B2UploadingPartStorer localPartContentSource = new B2UploadingPartStorer(
                        partSpec.getPartNumber(),
                        createRangedContentSource(contentSource, partSpec.getStart(), partSpec.getLength()));
                partContentSources.add(localPartContentSource);
            }
        } catch (IOException e) {
            throw new B2LocalException("trouble", "exception working with content source" + e, e);
        }

        // Instantiate and return the manager.
        return new B2LargeFileStorer(
                largeFileVersion,
                partContentSources,
                accountAuthCache,
                webifier,
                retryer,
                retryPolicySupplier,
                executor);
    }

    B2FileVersion storeFile() throws B2Exception {
        final List<Future<B2Part>> partFutures = new ArrayList<>();

        // Store each part in parallel.
        for (final B2PartStorer partStorer : partStorers) {
            partFutures.add(executor.submit(() -> partStorer.storePart(this)));
        }

        return finishLargeFileFromB2PartFutures(fileVersion, partFutures);
    }

    private B2FileVersion finishLargeFileFromB2PartFutures(B2FileVersion largeFileVersion,
                                                           List<Future<B2Part>> partFutures) throws B2Exception {

        final List<String> partSha1s = new ArrayList<>();
        try {
            for (final Future<B2Part> partFuture : partFutures) {
                final B2Part part = partFuture.get();
                partSha1s.add(part.getContentSha1());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new B2LocalException("interrupted", "interrupted while trying to copy parts: " + e, e);

        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof B2Exception) {
                throw (B2Exception) e.getCause();
            } else {
                throw new B2LocalException("trouble", "exception while trying to upload parts: " + cause, cause);
            }
        } finally {
            // we've either called get() on all of the futures, or we've hit an exception and
            // we aren't going to wait for the others.  let's call cancel on all of them.
            // the ones that have finished already won't mind and the others will be stopped.
            for (Future<B2Part> future : partFutures) {
                future.cancel(true);
            }
        }

        // finish the large file.
        B2FinishLargeFileRequest finishRequest = B2FinishLargeFileRequest
                .builder(largeFileVersion.getFileId(), partSha1s)
                .build();
        return retryer.doRetry(
                "b2_finish_large_file",
                accountAuthCache,
                () -> webifier.finishLargeFile(accountAuthCache.get(), finishRequest),
                retryPolicySupplier.get());
    }

    B2Part uploadPart(int partNumber, B2ContentSource contentSource) throws B2Exception {

        return retryer.doRetry(
                "b2_upload_part",
                accountAuthCache,
                (isRetry) -> {
                    final B2UploadPartUrlResponse uploadPartUrlResponse = uploadPartUrlCache.get(isRetry);

                    final B2UploadPartRequest uploadPartRequest = B2UploadPartRequest
                            .builder(partNumber, contentSource)
                            .build();

                    final B2Part part = webifier.uploadPart(uploadPartUrlResponse, uploadPartRequest);

                    // Return the upload part URL, because it works and can be reused.
                    uploadPartUrlCache.unget(uploadPartUrlResponse);

                    return part;
                },
                retryPolicySupplier.get()
        );
    }

    B2Part copyPart(int partNumber, String sourceFileId, B2ByteRange byteRangeOrNull) throws B2Exception {
        final B2CopyPartRequest copyPartRequest = B2CopyPartRequest
                .builder(partNumber, sourceFileId, fileVersion.getFileId())
                .setRange(byteRangeOrNull)
                .build();

        return retryer.doRetry(
                "b2_copy_part",
                accountAuthCache, () -> webifier.copyPart(accountAuthCache.get(), copyPartRequest),
                retryPolicySupplier.get());
    }

    static B2ContentSource createRangedContentSource(
            B2ContentSource contentSource, long start, long length) throws IOException {

        final B2ContentSource contentSourceWithRangeOrNull = contentSource.createContentSourceWithRangeOrNull(
                start, length);
        if (contentSourceWithRangeOrNull != null) {
            return contentSourceWithRangeOrNull;
        }

        return new B2PartOfContentSource(contentSource, start, length);
    }

}

