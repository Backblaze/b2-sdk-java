/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client;

import com.backblaze.b2.client.contentSources.B2ContentSource;
import com.backblaze.b2.client.contentSources.B2ContentTypes;
import com.backblaze.b2.client.contentSources.B2Headers;
import com.backblaze.b2.client.exceptions.B2Exception;
import com.backblaze.b2.client.exceptions.B2LocalException;
import com.backblaze.b2.client.structures.B2FileVersion;
import com.backblaze.b2.client.structures.B2FinishLargeFileRequest;
import com.backblaze.b2.client.structures.B2Part;
import com.backblaze.b2.client.structures.B2StartLargeFileRequest;
import com.backblaze.b2.client.structures.B2UploadFileRequest;
import com.backblaze.b2.client.structures.B2UploadListener;
import com.backblaze.b2.client.structures.B2UploadPartRequest;
import com.backblaze.b2.client.structures.B2UploadPartUrlResponse;
import com.backblaze.b2.client.structures.B2UploadState;
import com.backblaze.b2.util.B2ByteProgressListener;
import com.backblaze.b2.util.B2Collections;
import com.backblaze.b2.util.B2Preconditions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Supplier;

import static com.backblaze.b2.util.B2DateTimeUtil.ONE_SECOND_IN_MSECS;

class B2LargeFileUploader {
    private final B2Retryer retryer;
    private final B2StorageClientWebifier webifier;
    private final B2AccountAuthorizationCache accountAuthCache;
    private final Supplier<B2RetryPolicy> retryPolicySupplier;
    private final ExecutorService executor;
    private final B2PartSizes partSizes;
    private final B2UploadFileRequest request;
    private final long contentLength;

    B2LargeFileUploader(B2Retryer retryer,
                        B2StorageClientWebifier webifier,
                        B2AccountAuthorizationCache accountAuthCache,
                        Supplier<B2RetryPolicy> retryPolicySupplier,
                        ExecutorService executor,
                        B2PartSizes partSizes,
                        B2UploadFileRequest request,
                        long contentLength) {
        this.retryer = retryer;
        this.webifier = webifier;
        this.accountAuthCache = accountAuthCache;
        this.retryPolicySupplier = retryPolicySupplier;
        this.executor = executor;
        this.partSizes = partSizes;

        this.request = request;
        this.contentLength = contentLength;
    }

    B2FileVersion uploadLargeFile() throws B2Exception {
        final List<B2PartSpec> allPartSpecs = partSizes.pickParts(contentLength);

        // start the large file.
        final B2FileVersion largeFileVersion = retryer.doRetry("b2_start_large_file",
                accountAuthCache, () ->
                webifier.startLargeFile(accountAuthCache.get(), B2StartLargeFileRequest.buildFrom(request)),
                retryPolicySupplier.get()
        );

        final Map<B2PartSpec, B2Part> uploadedAlready = B2Collections.mapOf();
        return uploadPartsAndFinish(largeFileVersion, allPartSpecs, uploadedAlready);
    }

    B2FileVersion finishUploadingLargeFile(B2FileVersion largeFileVersion,
                                           List<B2Part> alreadyUploadedParts) throws B2Exception {
        throwIfLargeFileVersionDoesntSeemToMatchRequest(largeFileVersion, contentLength, request);

        // sort the alreadyUploadedParts so it's easy to walk through them in order.
        alreadyUploadedParts.sort(Comparator.comparingInt(B2Part::getPartNumber));

        // we could use the part#1's size as the recommendedPartSize,
        // but sometimes we won't have part#1, so we could pick the lowest-numbered part's size
        // or the most common size, or we could just compute from scratch...
        final List<B2PartSpec> allPartSpecs = partSizes.pickParts(contentLength);

        // figure out which parts that have already been uploaded that we can use.
        // note that if the recommended part size has changed, we will end up
        // reuploading all the parts.  any parts that don't match won't be
        // used when we finish the file later.
        final Map<B2PartSpec, B2Part> alreadyUploadedSpecs = new TreeMap<>();
        {
            int iPartSpec = 0;
            int iUploadedPart = 0;
            while (iPartSpec < allPartSpecs.size() && iUploadedPart < alreadyUploadedParts.size()) {
                final B2PartSpec partSpec = allPartSpecs.get(iPartSpec);
                final B2Part alreadyUploadedPart = alreadyUploadedParts.get(iUploadedPart);
                if (similarEnough(partSpec, alreadyUploadedPart)) {
                    alreadyUploadedSpecs.put(partSpec, alreadyUploadedPart);
                    iUploadedPart++;
                }
                iPartSpec++;
            }
        }

        return uploadPartsAndFinish(largeFileVersion, allPartSpecs, alreadyUploadedSpecs);
    }

    /**
     * Compares attributes of largeFileVersion with our request.  If they don't seem
     * to represent the same content, it throws a B2Exception.
     *
     * @param largeFileVersion the already started largeFileVersion that someone wants to finish uploading.
     */
    /*forTests*/ static void throwIfLargeFileVersionDoesntSeemToMatchRequest(B2FileVersion largeFileVersion,
                                                                             long contentLength,
                                                                             B2UploadFileRequest request) throws B2Exception {
        throwIfMismatch("fileName", request.getFileName(), largeFileVersion.getFileName());
        throwIfMismatch("sha1", getSha1FromRequest(request), largeFileVersion.getLargeFileSha1OrNull());
        if (!request.getContentType().equals(B2ContentTypes.B2_AUTO)) {
            throwIfMismatch("contentType", request.getContentType(), largeFileVersion.getContentType());
        }

        // we can't check the contentLength because it's not set on large files until their finished.  :(
        // throwIfMismatch("contentLength", Long.toString(contentLength), Long.toString(largeFileVersion.getContentLength()));


        // LARGE_FILE_SHA1 is a bit "special" since the SDK quietly adds it for the user.
        // we've checked LARGE_FILE_SHA1 above, so here, remove it and check any other entries against the request.
        final Map<String,String> infos = new TreeMap<>();
        infos.putAll(largeFileVersion.getFileInfo());
        infos.remove(B2Headers.LARGE_FILE_SHA1);
        throwIfMismatch("fileInfo", toString(request.getFileInfo()), toString(infos));
    }

    private static void throwIfMismatch(String name, String contentSourceValue, String largeFileVersionValue) throws B2LocalException {
        if (!Objects.equals(contentSourceValue, largeFileVersionValue)) {
            throw new B2LocalException("mismatch", "contentSource has " + name  + " '" + contentSourceValue + "', but largeFileVersion has '" + largeFileVersionValue + "'");
        }
    }

    private static String toString(Map<String,String> map) {
        final StringBuilder builder = new StringBuilder();
        builder.append("{\n");

        for (String key : map.keySet()) {
            builder.append("  ");
            builder.append(key);
            builder.append("=");
            builder.append(map.get(key));
            builder.append("\n");
        }

        builder.append("}");
        return builder.toString();
    }

    private static String getSha1FromRequest(B2UploadFileRequest request) throws B2Exception {
        try {
            return request.getContentSource().getSha1OrNull();
        } catch (IOException e) {
            throw new B2LocalException("trouble", "failed to get large file's sha1: " + e, e);
        }

    }

    private boolean similarEnough(B2PartSpec partSpec,
                                  B2Part alreadyUploadedPart) {
        // i wish i could check the starting index of each part too, but, that's not
        // available.  checking that the overall size is the same and that the
        // part numbers and lengths match should be good enough.
        return ((partSpec.partNumber == alreadyUploadedPart.getPartNumber()) &&
                (partSpec.length == alreadyUploadedPart.getContentLength()));
    }

    private B2FileVersion uploadPartsAndFinish(B2FileVersion largeFileVersion,
                                               List<B2PartSpec> allPartSpecs,
                                               Map<B2PartSpec, B2Part> uploadedAlready) throws B2Exception {
        // create a cache for upload part urls.  it's specific to the largeFile, so we don't need
        // to keep it outside this method.  we could *consider* keeping it in case we had too many
        // errors and ended up resuming later, but there's a good chance the urls would be bad
        // and it's ok to not optimize for that failure case.

        final B2UploadListener listener = request.getListener();
        final int partCount = allPartSpecs.size();

        final B2UploadPartUrlCache uploadPartUrlCache = new B2UploadPartUrlCache(
                webifier,
                accountAuthCache,
                largeFileVersion.getFileId());

        final List<String> partSha1s = new ArrayList<>();
        final List<Future<B2Part>> uploadedPartFutures = new ArrayList<>();
        try {
            // upload parts.
            for (B2PartSpec partSpec : allPartSpecs) {
                // tell the listener that this part will be waiting to start.
                listener.progress(B2UploadProgressUtil.forPart(partSpec, partCount, 1, B2UploadState.WAITING_TO_START));

                final B2Part alreadyUploadedPart = uploadedAlready.get(partSpec);
                if (alreadyUploadedPart == null) {
                    // do the upload
                    uploadedPartFutures.add(executor.submit(() -> uploadOnePart(uploadPartUrlCache, request, partCount, partSpec)));
                } else {
                    // tell the listener about our prior success as soon as we can.
                    listener.progress(B2UploadProgressUtil.forPartSucceeded(partSpec, partCount));

                    // shortcut the upload by just returning the previously uploaded B2Part.
                    // i could change the code to not submit all the tasks, but this is straight-forward,
                    // so i'll start with this for now.
                    uploadedPartFutures.add(executor.submit(() -> alreadyUploadedPart));
                }
            }

            B2Preconditions.checkState(partCount == uploadedPartFutures.size(), "didn't we add a future for every spec?");

            for (Future<B2Part> future : uploadedPartFutures) {
                try {
                    partSha1s.add(future.get().getContentSha1());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new B2LocalException("interrupted", "interrupted while trying to upload parts: " + e, e);
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();
                    if (cause instanceof B2Exception) {
                        throw (B2Exception) e.getCause();
                    } else {
                        throw new B2LocalException("trouble", "exception while trying to upload parts: " + cause, cause);
                    }
                }
            }
        } finally {
            // we've either called get() on all of the futures, or we've hit an exception and
            // we aren't going to wait for the others.  let's call cancel on all of them.
            // the ones that have finished already, won't mind and the others will be stopped.
            for (Future<B2Part> future : uploadedPartFutures) {
                future.cancel(true);
            }
        }

        // finish the large file.
        B2FinishLargeFileRequest finishRequest = B2FinishLargeFileRequest
                .builder(largeFileVersion.getFileId(), partSha1s)
                .build();
        return retryer.doRetry("b2_finish_large_file", accountAuthCache, () -> webifier.finishLargeFile(accountAuthCache.get(), finishRequest), new B2DefaultRetryPolicy());
    }

    private B2Part uploadOnePart(B2UploadPartUrlCache uploadPartUrlCache,
                                 B2UploadFileRequest request,
                                 int partCount,
                                 B2PartSpec partSpec) throws B2Exception {
        return retryer.doRetry("b2_upload_part",
                accountAuthCache,
                (isRetry) -> {
                    try {
                        final B2UploadPartUrlResponse uploadPartUrlResponse = uploadPartUrlCache.get(isRetry);

                        B2ByteProgressListener byteProgressListener = new B2UploadProgressAdapter(request.getListener(), partSpec.getPartNumber() - 1, partCount, partSpec.getStart(), partSpec.getLength());

                        request.getListener().progress(B2UploadProgressUtil.forPart(partSpec, partCount, 0, B2UploadState.STARTING));

                        final long nMsecsBetween = 5 * ONE_SECOND_IN_MSECS; // let progress through every few seconds.
                        byteProgressListener = new B2ByteProgressFilteringListener(byteProgressListener, nMsecsBetween);

                        B2ContentSource source = new B2PartOfContentSource(request.getContentSource(), partSpec.start, partSpec.length);
                        source = new B2ContentSourceWithByteProgressListener(source, byteProgressListener);

                        final B2UploadPartRequest partRequest = B2UploadPartRequest
                                .builder(partSpec.partNumber, source)
                                .build();

                        final B2Part part = webifier.uploadPart(uploadPartUrlResponse, partRequest);
                        uploadPartUrlCache.unget(uploadPartUrlResponse);

                        request.getListener().progress(B2UploadProgressUtil.forPartSucceeded(partSpec, partCount));
                        return part;
                    } catch (Exception e) {
                        request.getListener().progress(B2UploadProgressUtil.forPartFailed(partSpec, partCount));
                        throw e;
                    }
                },
                new B2DefaultRetryPolicy());
    }
}
