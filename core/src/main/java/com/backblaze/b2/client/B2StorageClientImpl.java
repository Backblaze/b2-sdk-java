/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client;

import com.backblaze.b2.client.contentHandlers.B2ContentSink;
import com.backblaze.b2.client.contentSources.B2ContentSource;
import com.backblaze.b2.client.exceptions.B2Exception;
import com.backblaze.b2.client.exceptions.B2LocalException;
import com.backblaze.b2.client.structures.B2AccountAuthorization;
import com.backblaze.b2.client.structures.B2Bucket;
import com.backblaze.b2.client.structures.B2CancelLargeFileRequest;
import com.backblaze.b2.client.structures.B2CreateBucketRequest;
import com.backblaze.b2.client.structures.B2CreateBucketRequestReal;
import com.backblaze.b2.client.structures.B2DeleteBucketRequest;
import com.backblaze.b2.client.structures.B2DeleteBucketRequestReal;
import com.backblaze.b2.client.structures.B2DeleteFileVersionRequest;
import com.backblaze.b2.client.structures.B2DownloadAuthorization;
import com.backblaze.b2.client.structures.B2DownloadByIdRequest;
import com.backblaze.b2.client.structures.B2DownloadByNameRequest;
import com.backblaze.b2.client.structures.B2FileVersion;
import com.backblaze.b2.client.structures.B2GetDownloadAuthorizationRequest;
import com.backblaze.b2.client.structures.B2GetFileInfoRequest;
import com.backblaze.b2.client.structures.B2HideFileRequest;
import com.backblaze.b2.client.structures.B2ListBucketsRequest;
import com.backblaze.b2.client.structures.B2ListBucketsResponse;
import com.backblaze.b2.client.structures.B2ListFileNamesRequest;
import com.backblaze.b2.client.structures.B2ListFileNamesResponse;
import com.backblaze.b2.client.structures.B2ListFileVersionsRequest;
import com.backblaze.b2.client.structures.B2ListFileVersionsResponse;
import com.backblaze.b2.client.structures.B2ListPartsRequest;
import com.backblaze.b2.client.structures.B2ListPartsResponse;
import com.backblaze.b2.client.structures.B2ListUnfinishedLargeFilesRequest;
import com.backblaze.b2.client.structures.B2ListUnfinishedLargeFilesResponse;
import com.backblaze.b2.client.structures.B2Part;
import com.backblaze.b2.client.structures.B2UpdateBucketRequest;
import com.backblaze.b2.client.structures.B2UploadFileRequest;
import com.backblaze.b2.client.structures.B2UploadUrlResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

/**
 * B2StorageClientImpl implements B2StorageClient and it acquires credentials as needed
 * and implements our retry policies.  It's also smart about using other
 * threads to help with the uploads.
 *
 * THREAD-SAFETY: As long at the subobjects it's given are thread-safe,
 *    this object may be used from multiple threads simultaneously.
 */
public class B2StorageClientImpl implements B2StorageClient {
    private final B2StorageClientWebifier webifier;
    private final String accountId;
    private final B2ClientConfig config;
    private final B2Retryer retryer;

    private final B2AccountAuthorizationCache accountAuthCache;
    private final B2UploadUrlCache uploadUrlCache;

    private final Supplier<B2RetryPolicy> retryPolicySupplier = B2DefaultRetryPolicy::new;

    // protected by synchronized(this)
    // starts out false.  it is changed to true when close() is called.
    private boolean closed;

    /**
     * Creates a client with the given webifier and config and a default B2Sleeper.
     * This is the normal constructor.
     * @param webifier the object to convert API calls into web calls.
     * @param config the object used to configure this.
     */
    public B2StorageClientImpl(B2StorageClientWebifier webifier,
                               B2ClientConfig config) {
        this(webifier, config, new B2Retryer(new B2Sleeper()));
    }

    /**
     * Creates a client with default webifier and config and the specified B2Sleeper.
     * This is used by tests where we don't really want to sleep.
     * @param webifier the object to convert API calls into web calls.
     * @param config the object used to configure this.
     */
    B2StorageClientImpl(B2StorageClientWebifier webifier,
                        B2ClientConfig config,
                        B2Retryer retryer) {
        this.webifier = webifier;
        this.accountId = config.getAccountAuthorizer().getAccountId();
        this.config = config;
        this.retryer = retryer;
        this.accountAuthCache = new B2AccountAuthorizationCache(webifier, config.getAccountAuthorizer());
        this.uploadUrlCache = new B2UploadUrlCache(webifier, accountAuthCache);
    }

    /**
     * Closes resources used by this client.
     * It's safe to call when it's already been called.
     * It won't return until we've tried to close the resources.
     */
    @Override
    public synchronized void close() {
        if (!closed) {
            closed = true;
            webifier.close();
        }
    }

    @Override
    public String getAccountId() {
        return config.getAccountAuthorizer().getAccountId();
    }

    @Override
    public B2FilePolicy getFilePolicy() throws B2Exception {
        return getPartSizes();
    }

    @Override
    public B2StorageClientWebifier getWebifier() {
        return webifier;
    }


    @Override
    public B2Bucket createBucket(B2CreateBucketRequest request) throws B2Exception {
        B2CreateBucketRequestReal realRequest = new B2CreateBucketRequestReal(accountId, request);
        return retryer.doRetry(accountAuthCache, () -> webifier.createBucket(accountAuthCache.get(), realRequest), retryPolicySupplier.get());
    }

    @Override
    public B2ListBucketsResponse listBuckets() throws B2Exception {
        return retryer.doRetry(accountAuthCache, () -> webifier.listBuckets(accountAuthCache.get(), B2ListBucketsRequest.builder(accountId).build()), retryPolicySupplier.get());
    }

    @Override
    public B2FileVersion finishUploadingLargeFile(B2FileVersion fileVersion,
                                                  B2UploadFileRequest request,
                                                  ExecutorService executor) throws B2Exception {
        // note that we assume that the contents of the B2ContentSource don't change during the upload.
        final long contentLength = getContentLength(request.getContentSource());
        final B2PartSizes partSizes = getPartSizes();

        B2LargeFileUploader uploader = new B2LargeFileUploader(retryer, webifier, accountAuthCache, executor, partSizes, request, contentLength);
        final List<B2Part> alreadyUploadedParts = new ArrayList<>();
        for (B2Part part : parts(fileVersion.getFileId())) {
            alreadyUploadedParts.add(part);
        }
        return uploader.finishUploadingLargeFile(fileVersion, alreadyUploadedParts);
    }

    @Override
    public B2FileVersion uploadSmallFile(B2UploadFileRequest request) throws B2Exception {
        return retryer.doRetry(accountAuthCache,
                (isRetry) -> {
                    final B2UploadUrlResponse uploadUrlResponse = uploadUrlCache.get(request.getBucketId(), isRetry);
                    final B2FileVersion version = webifier.uploadFile(uploadUrlResponse, request);
                    uploadUrlCache.unget(uploadUrlResponse);
                    return version;
                },
                retryPolicySupplier.get());
    }

    @Override
    public B2FileVersion uploadLargeFile(B2UploadFileRequest request,
                                         ExecutorService executor) throws B2Exception {
        final long contentLength = getContentLength(request.getContentSource());
        final B2PartSizes partSizes = getPartSizes();

        return uploadLargeFileGuts(executor, partSizes, request, contentLength);
    }

    private B2FileVersion uploadLargeFileGuts(ExecutorService executor,
                                              B2PartSizes partSizes,
                                              B2UploadFileRequest request,
                                              long contentLength) throws B2Exception {
        B2LargeFileUploader uploader = new B2LargeFileUploader(retryer, webifier, accountAuthCache, executor, partSizes, request, contentLength);
        return uploader.uploadLargeFile();
    }

    /**
     * NOTE: this might have to authenticate the client if there isn't currently a
     *       cached account authorization.  that's fine.  we're probably about to
     *       use it anyway.
     *
     * @return the recommendedPartSize from the server.
     * @throws B2Exception if there's trouble.
     */
    private B2PartSizes getPartSizes() throws B2Exception {
        return B2PartSizes.from(retryer.doRetry(accountAuthCache, accountAuthCache::get, retryPolicySupplier.get()));
    }

    /**
     * @param contentSource the contentSource whose content length the caller is asking about.
     * @return contentSource's contentLength.
     * @throws B2LocalException if there's any trouble.
     */
    private long getContentLength(B2ContentSource contentSource) throws B2LocalException {
        try {
            return contentSource.getContentLength();
        } catch (IOException e) {
            throw new B2LocalException("read_failed", "failed to get contentLength from source: " + e, e);
        }

    }

    @Override
    public B2ListFilesIterable fileVersions(B2ListFileVersionsRequest request) throws B2Exception {
        return new B2ListFileVersionsIterable(this, request);
    }

    @Override
    public B2ListFilesIterable fileNames(B2ListFileNamesRequest request) throws B2Exception {
        return new B2ListFileNamesIterable(this, request);
    }

    @Override
    public B2ListFilesIterable unfinishedLargeFiles(B2ListUnfinishedLargeFilesRequest request) throws B2Exception {
        return new B2ListUnfinishedLargeFilesIterable(this, request);
    }

    @Override
    public B2ListPartsIterable parts(B2ListPartsRequest request) throws B2Exception {
        return new B2ListPartsIterableImpl(this, request);
    }

    @Override
    public void cancelLargeFile(B2CancelLargeFileRequest request) throws B2Exception {
        retryer.doRetry(accountAuthCache,
                () -> {
                    webifier.cancelLargeFile(accountAuthCache.get(), request);
                    return 0; // to meet Callable api!
                },
                retryPolicySupplier.get());
    }

    @Override
    public void downloadById(B2DownloadByIdRequest request,
                             B2ContentSink handler) throws B2Exception {
        retryer.doRetry(accountAuthCache,
                () -> {
                    B2AccountAuthorization accountAuth = accountAuthCache.get();
                    webifier.downloadById(accountAuth, request, handler);
                    return 0; // to meet Callable api!
                },
                retryPolicySupplier.get());
    }

    @Override
    public void downloadByName(B2DownloadByNameRequest request,
                               B2ContentSink handler) throws B2Exception {
        retryer.doRetry(accountAuthCache,
                () -> {
                    B2AccountAuthorization accountAuth = accountAuthCache.get();
                    webifier.downloadByName(accountAuth, request, handler);
                    return 0; // to meet Callable api!
                },
                retryPolicySupplier.get());
    }

    @Override
    public void deleteFileVersion(B2DeleteFileVersionRequest request) throws B2Exception {
        retryer.doRetry(accountAuthCache,
                () -> {
                    webifier.deleteFileVersion(accountAuthCache.get(), request);
                    return 0; // to meet Callable api!
                },
                retryPolicySupplier.get());
    }

    @Override
    public B2DownloadAuthorization getDownloadAuthorization(B2GetDownloadAuthorizationRequest request) throws B2Exception {
        return retryer.doRetry(accountAuthCache, () -> webifier.getDownloadAuthorization(accountAuthCache.get(), request), retryPolicySupplier.get());
    }

    @Override
    public B2FileVersion getFileInfo(B2GetFileInfoRequest request) throws B2Exception {
        return retryer.doRetry(accountAuthCache, () -> webifier.getFileInfo(accountAuthCache.get(), request), retryPolicySupplier.get());
    }

    @Override
    public B2FileVersion hideFile(B2HideFileRequest request) throws B2Exception {
        return retryer.doRetry(accountAuthCache, () -> webifier.hideFile(accountAuthCache.get(), request), retryPolicySupplier.get());
    }

    @Override
    public B2Bucket updateBucket(B2UpdateBucketRequest request) throws B2Exception {
        return retryer.doRetry(accountAuthCache, () -> webifier.updateBucket(accountAuthCache.get(), request), retryPolicySupplier.get());
    }

    @Override
    public B2Bucket deleteBucket(B2DeleteBucketRequest request) throws B2Exception {
        B2DeleteBucketRequestReal realRequest = new B2DeleteBucketRequestReal(accountId, request.getBucketId());
        return retryer.doRetry(accountAuthCache, () -> webifier.deleteBucket(accountAuthCache.get(), realRequest), retryPolicySupplier.get());
    }

    //
    // For use by our iterators
    // XXX: make private somehow, or move to B2StorageClient interface.
    //
    public B2ListFileVersionsResponse listFileVersions(B2ListFileVersionsRequest request) throws B2Exception {
        return retryer.doRetry(accountAuthCache, () -> webifier.listFileVersions(accountAuthCache.get(), request), retryPolicySupplier.get());
    }
    public B2ListFileNamesResponse listFileNames(B2ListFileNamesRequest request) throws B2Exception {
        return retryer.doRetry(accountAuthCache, () -> webifier.listFileNames(accountAuthCache.get(), request), retryPolicySupplier.get());
    }
    public B2ListUnfinishedLargeFilesResponse listUnfinishedLargeFiles(B2ListUnfinishedLargeFilesRequest request) throws B2Exception {
        return retryer.doRetry(accountAuthCache, () -> webifier.listUnfinishedLargeFiles(accountAuthCache.get(), request), retryPolicySupplier.get());
    }
    public B2ListPartsResponse listParts(B2ListPartsRequest request) throws B2Exception {
        return retryer.doRetry(accountAuthCache, () -> webifier.listParts(accountAuthCache.get(), request), retryPolicySupplier.get());
    }
}
