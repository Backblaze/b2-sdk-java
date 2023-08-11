/*
 * Copyright 2019, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client;

import com.backblaze.b2.client.contentSources.B2ContentSource;
import com.backblaze.b2.client.exceptions.B2CannotComputeException;
import com.backblaze.b2.client.exceptions.B2Exception;
import com.backblaze.b2.client.exceptions.B2LocalException;
import com.backblaze.b2.client.structures.B2CopyPartRequest;
import com.backblaze.b2.client.structures.B2FileSseForRequest;
import com.backblaze.b2.client.structures.B2FileVersion;
import com.backblaze.b2.client.structures.B2FinishLargeFileRequest;
import com.backblaze.b2.client.structures.B2Part;
import com.backblaze.b2.client.structures.B2StoreLargeFileRequest;
import com.backblaze.b2.client.structures.B2UploadListener;
import com.backblaze.b2.client.structures.B2UploadPartRequest;
import com.backblaze.b2.client.structures.B2UploadPartUrlResponse;
import com.backblaze.b2.client.structures.B2UploadProgress;
import com.backblaze.b2.client.structures.B2UploadState;
import com.backblaze.b2.util.B2ByteProgressListener;
import com.backblaze.b2.util.B2ByteRange;
import com.backblaze.b2.util.B2Preconditions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.*;
import java.util.function.Supplier;

/**
 * A class for handling the creation of large files.
 *
 * @see B2StorageClient#storeLargeFile(B2FileVersion, List, B2UploadListener, ExecutorService)
 * for a description of the various ways large files can be created
 * and the uses cases supported.
 */
public class B2LargeFileStorer {

    /**
     * The file ID of the large file that is being created.
     */
    private final String largeFileId;

    /**
     * The B2FileSseForRequest for the large file that is being created.
     * This contains the SSE-C parameters for SSE-C uploads and is null
     * otherwise.
     */
    private final B2FileSseForRequest serverSideEncryptionOrNull;

    /**
     * The parts that need to be stored before finishing the large file.
     */
    private final List<B2PartStorer> partStorers;

    /**
     * Stores where each part will begin in the finished large file.
     * Will be set to B2UploadProgress.UNKNOWN_PART_START_BYTE for
     * parts that come after a copied part.
     */
    private final List<Long> startingBytePositions;

    /**
     * Map from part number to index into the partStorers and
     * startingBytePositions lists.
     */
    private final Map<Integer, Integer> indexesByPartNumber;

    /**
     * The cancellation token used to abort uploads in progress.
     */
    private final B2CancellationToken cancellationToken = new B2CancellationToken();

    private final B2AccountAuthorizationCache accountAuthCache;
    private final B2UploadPartUrlCache uploadPartUrlCache;
    private final B2StorageClientWebifier webifier;
    private final B2Retryer retryer;
    private final Supplier<B2RetryPolicy> retryPolicySupplier;
    private final ExecutorService executor;

    B2LargeFileStorer(
            B2StoreLargeFileRequest storeLargeFileRequest,
            List<B2PartStorer> partStorers,
            B2AccountAuthorizationCache accountAuthCache,
            B2StorageClientWebifier webifier,
            B2Retryer retryer,
            Supplier<B2RetryPolicy> retryPolicySupplier,
            ExecutorService executor,
            boolean partNumberGapsAllowed) {
        B2Preconditions.checkArgumentIsNotNull(storeLargeFileRequest, "storeLargeFileRequest");

        this.largeFileId = storeLargeFileRequest.getFileId();
        this.serverSideEncryptionOrNull = storeLargeFileRequest.getServerSideEncryption();
        this.partStorers = validateAndSortPartStorers(new ArrayList<>(partStorers), partNumberGapsAllowed);
        this.startingBytePositions = computeStartingBytePositions(partStorers);
        this.indexesByPartNumber = computeIndexesByPartNumbers(partStorers);

        this.accountAuthCache = accountAuthCache;
        this.uploadPartUrlCache = new B2UploadPartUrlCache(webifier, accountAuthCache, largeFileId);
        this.webifier = webifier;
        this.retryer = retryer;
        this.retryPolicySupplier = retryPolicySupplier;
        this.executor = executor;
    }

    private List<B2PartStorer> validateAndSortPartStorers(List<B2PartStorer> partStorers,
                                                          boolean partNumberGapsAllowed) {
        partStorers.sort(Comparator.comparingInt(B2PartStorer::getPartNumber));

        validatePartStorers(partStorers, partNumberGapsAllowed);

        return partStorers;
    }

    private void validatePartStorers(List<B2PartStorer> partStorers, boolean partNumberGapsAllowed) {
        // Go through the parts - throw if there are duplicates or gaps.
        int expectedPartNumber = 0;
        for (B2PartStorer partStorer : partStorers) {
            expectedPartNumber++;
            final int partNumber = partStorer.getPartNumber();

            if (partNumber < 1) {
                throw new IllegalArgumentException("invalid part number: " + partNumber);
            }
            if (partNumber < expectedPartNumber) {
                throw new IllegalArgumentException(
                        "part number " + partNumber + " has multiple part storers");
            }
            if (partNumber > expectedPartNumber) {
                if (partNumberGapsAllowed) {
                    // for internal use only: do not enforce requirement that part numbers must start with 1 and
                    // be contiguous
                    expectedPartNumber = partNumber;
                } else {
                    throw new IllegalArgumentException("part number " + expectedPartNumber + " has no part storers");
                }
            }
        }
    }

    private static List<Long> computeStartingBytePositions(List<B2PartStorer> partStorers) {
        final List<Long> startingPositions = new ArrayList<>(partStorers.size());

        long cursor = 0;
        try {
            for (final B2PartStorer partStorer : partStorers) {
                startingPositions.add(cursor);
                cursor += partStorer.getPartSizeOrThrow();
            }
        } catch (B2CannotComputeException e) {
            while (startingPositions.size() < partStorers.size()) {
                startingPositions.add(B2UploadProgress.UNKNOWN_PART_START_BYTE);
            }
        }

        return startingPositions;
    }

    private Map<Integer, Integer> computeIndexesByPartNumbers(List<B2PartStorer> partStorers) {
        final Map<Integer, Integer> indexes = new TreeMap<>();

        for (int i = 0; i < partStorers.size(); i++) {
            final B2PartStorer partStorer = partStorers.get(i);
            indexes.put(partStorer.getPartNumber(), i);
        }

        return indexes;
    }

    List<B2PartStorer> getPartStorers() {
        return partStorers;
    }


    private int getIndexForPartNumber(int partNumber) {
        Integer indexOrNull = indexesByPartNumber.get(partNumber);
        if (indexOrNull == null) {
            throw new IllegalArgumentException("invalid part number: " + partNumber);
        }

        return indexOrNull;
    }

    /**
     * @return The start byte for the part, or UNKNOWN_PART_START_BYTE if not known.
     */
    long getStartByteOrUnknown(int partNumber) {
        return startingBytePositions.get(getIndexForPartNumber(partNumber));
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
        return forLocalContent(
                B2StoreLargeFileRequest.builder(largeFileVersion.getFileId()).build(),
                contentSource,
                partSizes,
                accountAuthCache,
                webifier,
                retryer,
                retryPolicySupplier,
                executor);
    }

    public static B2LargeFileStorer forLocalContent(
            B2StoreLargeFileRequest storeLargeFileRequest,
            B2ContentSource contentSource,
            B2PartSizes partSizes,
            B2AccountAuthorizationCache accountAuthCache,
            B2StorageClientWebifier webifier,
            B2Retryer retryer,
            Supplier<B2RetryPolicy> retryPolicySupplier,
            ExecutorService executor) throws B2Exception {
        return forLocalContent(
                storeLargeFileRequest,
                contentSource,
                partSizes,
                accountAuthCache,
                webifier,
                retryer,
                retryPolicySupplier,
                executor,
                false);
    }

    // NOTE: Setting allowGaps to true is only intended for internal B2 use; even if it's set to true, the
    // B2 Native API will not allow gaps between part numbers for large file uploads.
    public static B2LargeFileStorer forLocalContent(
            B2StoreLargeFileRequest storeLargeFileRequest,
            B2ContentSource contentSource,
            B2PartSizes partSizes,
            B2AccountAuthorizationCache accountAuthCache,
            B2StorageClientWebifier webifier,
            B2Retryer retryer,
            Supplier<B2RetryPolicy> retryPolicySupplier,
            ExecutorService executor,
            boolean allowGaps) throws B2Exception {
        B2Preconditions.checkArgumentIsNotNull(storeLargeFileRequest, "storeLargeFileRequest");

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
                storeLargeFileRequest,
                partContentSources,
                accountAuthCache,
                webifier,
                retryer,
                retryPolicySupplier,
                executor,
                allowGaps);
    }

    B2FileVersion storeFile(B2UploadListener uploadListenerOrNull) throws B2Exception {
        try {
            return storeFileAsync(uploadListenerOrNull).get();
        } catch (ExecutionException e) {
            final Throwable cause = e.getCause();
            if (cause instanceof B2Exception) {
                throw (B2Exception) cause;
            } else {
                throw new B2LocalException("trouble", "exception while trying to upload parts: " + cause, cause);
            }
        } catch (InterruptedException e) {
            throw new B2LocalException("trouble", "interrupted exception");
        }
    }

    /**
     * Stores the file asynchronously and returns a CompletableFuture to manage the task.
     *
     * The returned future can be cancelled, and that will attempt to abort any already started
     * uploads by causing them to fail.
     *
     * Cancelling after the b2_finish_large_file API call has been started will result in the
     * future being cancelled, but the API call can still succeed. There is no way to tell from
     * the future whether this is the case. The caller is responsible for checking and calling
     * B2StorageClient.cancelLargeFile.
     *
     * @param uploadListenerOrNull upload listener
     * @return CompletableFuture that returns the finished file's B2FileVersion
     */
    CompletableFuture<B2FileVersion> storeFileAsync(B2UploadListener uploadListenerOrNull) {
        final CompletableFuture<List<B2Part>> partsFuture = storePartsAsync(uploadListenerOrNull);

        final CompletableFuture<B2FileVersion> retval = partsFuture
                .thenApplyAsync(parts -> finishLargeFileFromB2PartsInCompletionStage(largeFileId, parts), executor);

        // The caller can call cancel on the future that we give them, but that will only
        // stop futures chained to the end of this future from running; it does not stop
        // processing the part uploads that are started by partsFuture. So we add our own
        // handler to detect this, propagating the cancellation to partsFuture to try
        // to cancel any not-yet-started uploads, and setting the cancellationToken to try to
        // fail any in-progress uploads.
        retval.whenComplete((result, error) -> {
           if (error instanceof CancellationException) {
               cancellationToken.cancel();
               partsFuture.cancel(true);
           }
        });

        return retval;
    }

    List<B2Part> storeParts(B2UploadListener uploadListenerOrNull) throws B2Exception {
        try {
            return storePartsAsync(uploadListenerOrNull).get();
        } catch (ExecutionException e) {
            final Throwable cause = e.getCause();
            if (cause instanceof B2Exception) {
                throw (B2Exception) cause;
            } else {
                throw new B2LocalException("trouble", "exception while trying to upload parts: " + cause, cause);
            }
        } catch (InterruptedException e) {
            throw new B2LocalException("trouble", "interrupted exception");
        }
    }

    CompletableFuture<List<B2Part>> storePartsAsync(B2UploadListener uploadListenerOrNull) {
        final B2UploadListener uploadListener;
        if (uploadListenerOrNull == null) {
            uploadListener = B2UploadListener.noopListener();
        } else {
            uploadListener = uploadListenerOrNull;
        }

        final List<CompletableFuture<B2Part>> completableFutures = new ArrayList<>();

        // Store each part in parallel.
        for (final B2PartStorer partStorer : partStorers) {
            CompletableFuture<B2Part> future = CompletableFuture.supplyAsync(
                    adaptB2Supplier(() -> partStorer.storePart(this, uploadListener, cancellationToken)),
                    executor);

            completableFutures.add(future);
        }

        // future that tracks when all the parts are stored
        final CompletableFuture<Void> allPartsCompletedFuture = CompletableFuture.allOf(completableFutures.toArray(new CompletableFuture[0]));

        final List<Future<B2Part>> partFutures = new ArrayList<>(completableFutures);

        // this is the future to return to the caller
        final CompletableFuture<List<B2Part>> retval = allPartsCompletedFuture
                .thenApplyAsync((voidParam) -> getB2PartListFromB2PartFuturesInCompletionStage(partFutures), executor);

        // The caller can call cancel on the future that we give them, but that will only
        // stop futures chained to the end of this future from running; it does not stop
        // processing the parts that may be uploading currently. So we add our own handler
        // to detect this and cancel any remaining part uploads so they don't start, and
        // flag the cancellation token to try to fail any in-progress uploads.
        retval.whenComplete((result, error) -> {
            if (error instanceof CancellationException) {
                cancellationToken.cancel();
                completableFutures.forEach(x -> x.cancel(true));
            }
        });

        return retval;
    }

    /**
     * Adapts finishLargeFileFromB2Parts to be used in completion stages.
     * <p>
     * These functions cannot return B2Exceptions, so those must be caught here and converted
     * to CompletionExceptions.
     */
    private B2FileVersion finishLargeFileFromB2PartsInCompletionStage(String largeFileId,
                                                                      List<B2Part> parts) {
        return callSupplierAndConvertErrorsForCompletableFutures(
                () -> finishLargeFileFromB2Parts(largeFileId, parts)
        );
    }

    /**
     * Adapts getB2PartListFromB2PartFutures to be used in completion stages.
     * <p>
     * These functions cannot return B2Exceptions, so those must be caught here and converted
     * to CompletionExceptions.
     */
    private List<B2Part> getB2PartListFromB2PartFuturesInCompletionStage(List<Future<B2Part>> partFutures) {
        return callSupplierAndConvertErrorsForCompletableFutures(
                () -> getB2PartListFromB2PartFutures(partFutures)
        );
    }

    private List<B2Part> getB2PartListFromB2PartFutures(List<Future<B2Part>> partFutures) throws B2Exception {
        cancellationToken.throwIfCancelled();

        final List<B2Part> parts = new ArrayList<>();
        try {
            for (final Future<B2Part> partFuture : partFutures) {
                parts.add(partFuture.get());
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

        return parts;
    }

    /**
     * Supplier interface that throws B2Exception or IOException
     * @param <Type> return type
     */
    private interface B2Supplier<Type> {
        Type get() throws B2Exception, IOException;
    }

    /**
     * Calls the supplier and converts B2Exception or IOException into CompletionException to be
     * suitable for use in CompletionStages
     * @param supplier supplier function can throw B2Exception or IOException
     * @param <Type> type the supplier returns
     * @return result of the supplier function
     */
    <Type> Type callSupplierAndConvertErrorsForCompletableFutures(B2Supplier<Type> supplier) {
        try {
            return supplier.get();
        } catch (IOException | B2Exception error) {
            throw new CompletionException(error);
        }
    }

    /**
     *
     * @param supplier converts a supplier that can throw B2Exception or IOException into a
     *                 Supplier instance that does not; these exceptions will be converted into
     *                 CompletionExceptions instead.
     *
     *                 The resulting supplier is suitable to use in CompletionStages
     * @param <Type> return type of the supplier
     * @return supplier
     */
    private <Type> Supplier<Type> adaptB2Supplier(B2Supplier<Type> supplier) {
        return () -> callSupplierAndConvertErrorsForCompletableFutures(supplier);
    }

    private B2FileVersion finishLargeFileFromB2Parts(String largeFileId,
                                                     List<B2Part> parts) throws B2Exception {
        cancellationToken.throwIfCancelled();

        final List<String> partSha1s = new ArrayList<>();
        for (final B2Part part : parts) {
            partSha1s.add(part.getContentSha1());
        }

        // finish the large file.
        B2FinishLargeFileRequest finishRequest = B2FinishLargeFileRequest
                .builder(largeFileId, partSha1s)
                .build();
        return retryer.doRetry(
                "b2_finish_large_file",
                accountAuthCache,
                () -> {
                    cancellationToken.throwIfCancelled();
                    return webifier.finishLargeFile(accountAuthCache.get(), finishRequest);
                },
                retryPolicySupplier.get());
    }

    void updateProgress(
            B2UploadListener uploadListener,
            int partNumber,
            long partLength,
            long bytesSoFar,
            B2UploadState uploadState) {

        uploadListener.progress(
                new B2UploadProgress(
                        getIndexForPartNumber(partNumber),
                        partStorers.size(),
                        getStartByteOrUnknown(partNumber),
                        partLength,
                        bytesSoFar,
                        uploadState));
    }

    /**
     * Stores a part by uploading the bytes from a content source.
     */
    public B2Part uploadPart(
            int partNumber,
            B2ContentSource contentSource,
            B2UploadListener uploadListener) throws IOException, B2Exception {
        cancellationToken.throwIfCancelled();

        updateProgress(
                uploadListener,
                partNumber,
                contentSource.getContentLength(),
                0,
                B2UploadState.WAITING_TO_START);

        // Set up the listener for the part upload.
        final B2ByteProgressListener progressAdapter = new B2UploadProgressAdapter(
                uploadListener,
                getIndexForPartNumber(partNumber),
                partStorers.size(),
                getStartByteOrUnknown(partNumber),
                contentSource.getContentLength());
        final B2ByteProgressFilteringListener progressListener = new B2ByteProgressFilteringListener(progressAdapter);

        try {
            return retryer.doRetry(
                    "b2_upload_part",
                    accountAuthCache,
                    (isRetry) -> {
                        cancellationToken.throwIfCancelled();
                        final B2UploadPartUrlResponse uploadPartUrlResponse = uploadPartUrlCache.get(isRetry);

                        final B2ContentSource contentSourceThatReportsProgress =
                                new B2ContentSourceWithByteProgressListener(contentSource, progressListener);
                        final B2UploadPartRequest uploadPartRequest = B2UploadPartRequest
                                .builder(partNumber, contentSourceThatReportsProgress)
                                .setServerSideEncryption(serverSideEncryptionOrNull)
                                .build();

                        updateProgress(
                                uploadListener,
                                partNumber,
                                contentSource.getContentLength(),
                                0,
                                B2UploadState.STARTING);

                        final B2Part part = webifier.uploadPart(uploadPartUrlResponse, uploadPartRequest);

                        // Return the upload part URL, because it works and can be reused.
                        uploadPartUrlCache.unget(uploadPartUrlResponse);

                        updateProgress(
                                uploadListener,
                                partNumber,
                                part.getContentLength(),
                                part.getContentLength(),
                                B2UploadState.SUCCEEDED);

                        return part;
                    },
                    retryPolicySupplier.get()
            );
        } catch (B2Exception e) {
            updateProgress(
                    uploadListener,
                    partNumber,
                    contentSource.getContentLength(),
                    0,
                    B2UploadState.FAILED);

            throw e;
        }
    }

    /**
     * Stores a part by copying from a file that is already stored in a bucket.
     *
     * We do not know the true size of the part until it is finally stored. Some
     * copy operations do not provide a byte range being copied, and byte ranges
     * can be clamped down if they exceed the bounds of the file. Therefore, we
     * use a placeholder value until the operation succeeds. Once the API returns
     * a B2Part object, we supply the true size in the SUCCEEDED event.
     */
    B2Part copyPart(
            int partNumber,
            String sourceFileId,
            B2ByteRange byteRangeOrNull,
            B2UploadListener uploadListener) throws B2Exception {
        cancellationToken.throwIfCancelled();

        updateProgress(
                uploadListener,
                partNumber,
                B2UploadProgress.UNKNOWN_PART_SIZE_PLACEHOLDER,
                0,
                B2UploadState.WAITING_TO_START);

        final B2CopyPartRequest copyPartRequest = B2CopyPartRequest
                .builder(partNumber, sourceFileId, largeFileId)
                .setRange(byteRangeOrNull)
                .build();

        try {
            return retryer.doRetry(
                    "b2_copy_part",
                    accountAuthCache,
                    () -> {
                        cancellationToken.throwIfCancelled();

                        updateProgress(
                                uploadListener,
                                partNumber,
                                B2UploadProgress.UNKNOWN_PART_SIZE_PLACEHOLDER,
                                0,
                                B2UploadState.STARTING);

                        final B2Part part = webifier.copyPart(accountAuthCache.get(), copyPartRequest);

                        updateProgress(
                                uploadListener,
                                partNumber,
                                part.getContentLength(),
                                part.getContentLength(),
                                B2UploadState.SUCCEEDED);

                        return part;
                    },
                    retryPolicySupplier.get());
        } catch (B2Exception e) {
            updateProgress(
                    uploadListener,
                    partNumber,
                    B2UploadProgress.UNKNOWN_PART_SIZE_PLACEHOLDER,
                    0,
                    B2UploadState.FAILED);

            throw e;
        }
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
