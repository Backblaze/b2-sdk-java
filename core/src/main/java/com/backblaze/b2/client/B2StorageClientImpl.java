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
import com.backblaze.b2.client.structures.B2ApplicationKey;
import com.backblaze.b2.client.structures.B2Bucket;
import com.backblaze.b2.client.structures.B2CancelLargeFileRequest;
import com.backblaze.b2.client.structures.B2CopyFileRequest;
import com.backblaze.b2.client.structures.B2CreateBucketRequest;
import com.backblaze.b2.client.structures.B2CreateBucketRequestReal;
import com.backblaze.b2.client.structures.B2CreateKeyRequest;
import com.backblaze.b2.client.structures.B2CreateKeyRequestReal;
import com.backblaze.b2.client.structures.B2CreatedApplicationKey;
import com.backblaze.b2.client.structures.B2DeleteBucketRequest;
import com.backblaze.b2.client.structures.B2DeleteBucketRequestReal;
import com.backblaze.b2.client.structures.B2DeleteFileVersionRequest;
import com.backblaze.b2.client.structures.B2DeleteKeyRequest;
import com.backblaze.b2.client.structures.B2DownloadAuthorization;
import com.backblaze.b2.client.structures.B2DownloadByIdRequest;
import com.backblaze.b2.client.structures.B2DownloadByNameRequest;
import com.backblaze.b2.client.structures.B2FileSseForRequest;
import com.backblaze.b2.client.structures.B2FileVersion;
import com.backblaze.b2.client.structures.B2FinishLargeFileRequest;
import com.backblaze.b2.client.structures.B2GetDownloadAuthorizationRequest;
import com.backblaze.b2.client.structures.B2GetFileInfoByNameRequest;
import com.backblaze.b2.client.structures.B2GetFileInfoRequest;
import com.backblaze.b2.client.structures.B2GetUploadPartUrlRequest;
import com.backblaze.b2.client.structures.B2GetUploadUrlRequest;
import com.backblaze.b2.client.structures.B2HideFileRequest;
import com.backblaze.b2.client.structures.B2ListBucketsRequest;
import com.backblaze.b2.client.structures.B2ListBucketsResponse;
import com.backblaze.b2.client.structures.B2ListFileNamesRequest;
import com.backblaze.b2.client.structures.B2ListFileNamesResponse;
import com.backblaze.b2.client.structures.B2ListFileVersionsRequest;
import com.backblaze.b2.client.structures.B2ListFileVersionsResponse;
import com.backblaze.b2.client.structures.B2ListKeysRequest;
import com.backblaze.b2.client.structures.B2ListKeysRequestReal;
import com.backblaze.b2.client.structures.B2ListKeysResponse;
import com.backblaze.b2.client.structures.B2ListPartsRequest;
import com.backblaze.b2.client.structures.B2ListPartsResponse;
import com.backblaze.b2.client.structures.B2ListUnfinishedLargeFilesRequest;
import com.backblaze.b2.client.structures.B2ListUnfinishedLargeFilesResponse;
import com.backblaze.b2.client.structures.B2Part;
import com.backblaze.b2.client.structures.B2StartLargeFileRequest;
import com.backblaze.b2.client.structures.B2StoreLargeFileRequest;
import com.backblaze.b2.client.structures.B2UpdateBucketRequest;
import com.backblaze.b2.client.structures.B2UpdateFileLegalHoldRequest;
import com.backblaze.b2.client.structures.B2UpdateFileLegalHoldResponse;
import com.backblaze.b2.client.structures.B2UpdateFileRetentionRequest;
import com.backblaze.b2.client.structures.B2UpdateFileRetentionResponse;
import com.backblaze.b2.client.structures.B2UploadFileRequest;
import com.backblaze.b2.client.structures.B2UploadListener;
import com.backblaze.b2.client.structures.B2UploadPartUrlResponse;
import com.backblaze.b2.client.structures.B2UploadUrlResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
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
    private final Supplier<B2RetryPolicy> retryPolicySupplier;
    private final B2Retryer retryer;

    private final B2AccountAuthorizationCache accountAuthCache;
    private final B2UploadUrlCache uploadUrlCache;


    // protected by synchronized(this)
    // starts out false.  it is changed to true when close() is called.
    private boolean closed;

    /**
     * Creates a client with the given webifier and config and a default B2Sleeper.
     * This is the normal constructor.
     *
     * @param webifier the object to convert API calls into web calls.
     * @param config   the object used to configure this.
     */
    public B2StorageClientImpl(B2StorageClientWebifier webifier,
                               B2ClientConfig config,
                               Supplier<B2RetryPolicy> retryPolicySupplier) {
        this(webifier, config, retryPolicySupplier, new B2Retryer(new B2Sleeper()));
    }

    /**
     * Creates a client with default webifier and config and the specified B2Retryer.
     * This is used by tests where we don't really want to sleep so we make a retryer
     * with a mock B2Sleeper.
     * @param webifier the object to convert API calls into web calls.
     * @param config the object used to configure this.
     */
    B2StorageClientImpl(B2StorageClientWebifier webifier,
                        B2ClientConfig config,
                        Supplier<B2RetryPolicy> retryPolicySupplier,
                        B2Retryer retryer) {
        this.webifier = webifier;
        this.retryPolicySupplier = retryPolicySupplier;
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
    public String getAccountId() throws B2Exception {
        return retryer.doRetry("getAccountId", accountAuthCache, accountAuthCache::getAccountId, retryPolicySupplier.get());
    }

    private String getAccountIdWithoutRetry() throws B2Exception {
        return accountAuthCache.getAccountId();
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
        return retryer.doRetry("b2_create_bucket", accountAuthCache, () -> {
            B2CreateBucketRequestReal realRequest = new B2CreateBucketRequestReal(getAccountIdWithoutRetry(), request);
            return webifier.createBucket(accountAuthCache.get(), realRequest);
        }, retryPolicySupplier.get());
    }

    @Override
    public B2CreatedApplicationKey createKey(B2CreateKeyRequest request) throws B2Exception {
        return retryer.doRetry(
                "b2_create_key",
                accountAuthCache,
                () -> {
                    final B2CreateKeyRequestReal realRequest = new B2CreateKeyRequestReal(getAccountIdWithoutRetry(), request);
                    return webifier.createKey(accountAuthCache.get(), realRequest);
                } ,
                retryPolicySupplier.get()
        );
    }

    @SuppressWarnings("RedundantThrows")
    @Override
    public B2ListKeysIterable applicationKeys(B2ListKeysRequest request) throws B2Exception {
        return new B2ListKeysIterable(this, request);
    }

    @Override
    public B2ApplicationKey deleteKey(B2DeleteKeyRequest request) throws B2Exception {
        return retryer.doRetry(
                "b2_delete_key",
                accountAuthCache,
                () -> webifier.deleteKey(accountAuthCache.get(), request),
                retryPolicySupplier.get()
        );
    }

    @Override
    public B2ListBucketsResponse listBuckets(B2ListBucketsRequest listBucketsRequest) throws B2Exception {
        return retryer.doRetry("b2_list_buckets", accountAuthCache, () -> webifier.listBuckets(accountAuthCache.get(), listBucketsRequest), retryPolicySupplier.get());
    }

    @Override
    public B2FileVersion finishUploadingLargeFile(B2FileVersion fileVersion,
                                                  B2UploadFileRequest request,
                                                  ExecutorService executor) throws B2Exception {
        // note that we assume that the contents of the B2ContentSource don't change during the upload.
        final long contentLength = getContentLength(request.getContentSource());
        final B2PartSizes partSizes = getPartSizes();

        B2LargeFileUploader uploader = new B2LargeFileUploader(retryer, webifier, accountAuthCache, retryPolicySupplier, executor, partSizes, request, contentLength);
        final List<B2Part> alreadyUploadedParts = new ArrayList<>();
        for (B2Part part : parts(fileVersion.getFileId())) {
            alreadyUploadedParts.add(part);
        }
        return uploader.finishUploadingLargeFile(fileVersion, alreadyUploadedParts);
    }

    @Override
    public B2FileVersion uploadSmallFile(B2UploadFileRequest request) throws B2Exception {
        return retryer.doRetry("b2_upload_file",
                accountAuthCache,
                (isRetry) -> {
                    final B2UploadUrlResponse uploadUrlResponse = uploadUrlCache.get(request.getBucketId(), isRetry);
                    final B2FileVersion version = webifier.uploadFile(uploadUrlResponse, request);
                    uploadUrlCache.unget(uploadUrlResponse);
                    return version;
                },
                retryPolicySupplier.get());
    }

    @Override
    public B2FileVersion copySmallFile(B2CopyFileRequest request) throws B2Exception {
        return retryer.doRetry("b2_copy_file",
                accountAuthCache,
                isRetry -> webifier.copyFile(accountAuthCache.get(), request),
                retryPolicySupplier.get());
    }

    @Override
    public B2FileVersion uploadLargeFile(B2UploadFileRequest request,
                                         ExecutorService executor) throws B2Exception {
        final long contentLength = getContentLength(request.getContentSource());
        final B2PartSizes partSizes = getPartSizes();

        return uploadLargeFileGuts(executor, partSizes, request, contentLength);
    }

    @Override
    public B2FileVersion storeLargeFileFromLocalContent(
            B2FileVersion fileVersion,
            B2ContentSource contentSource,
            B2UploadListener uploadListener,
            ExecutorService executor) throws B2Exception {

        return storeLargeFileFromLocalContent(B2StoreLargeFileRequest.builder(fileVersion).build(), contentSource, uploadListener, executor);
    }

    @Override
    public B2FileVersion storeLargeFileFromLocalContent(
            B2StoreLargeFileRequest storeLargeFileRequest,
            B2ContentSource contentSource,
            B2UploadListener uploadListener,
            ExecutorService executor) throws B2Exception {

        return B2LargeFileStorer.forLocalContent(
                storeLargeFileRequest,
                contentSource,
                getPartSizes(),
                accountAuthCache,
                webifier,
                retryer,
                retryPolicySupplier,
                executor).storeFile(uploadListener);
    }

    @Override
    public CompletableFuture<B2FileVersion> storeLargeFileFromLocalContentAsync(
            B2FileVersion fileVersion,
            B2ContentSource contentSource,
            B2UploadListener uploadListenerOrNull,
            ExecutorService executor) throws B2Exception {

        final B2LargeFileStorer storer = B2LargeFileStorer.forLocalContent(
                fileVersion,
                contentSource,
                getPartSizes(),
                accountAuthCache,
                webifier,
                retryer,
                retryPolicySupplier,
                executor);

        return storer.storeFileAsync(uploadListenerOrNull);
    }

    @Override
    public B2FileVersion storeLargeFile(
            B2FileVersion fileVersion,
            List<B2PartStorer> partStorers,
            B2UploadListener uploadListenerOrNull,
            ExecutorService executor) throws B2Exception {
        return storeLargeFile(B2StoreLargeFileRequest.builder(fileVersion).build(), partStorers, uploadListenerOrNull, executor);
    }

    @Override
    public B2FileVersion storeLargeFile(
            B2StoreLargeFileRequest storeLargeFileRequest,
            List<B2PartStorer> partStorers,
            B2UploadListener uploadListenerOrNull,
            ExecutorService executor) throws B2Exception {

        // Instantiate and return the manager.
        return new B2LargeFileStorer(
                storeLargeFileRequest,
                partStorers,
                accountAuthCache,
                webifier,
                retryer,
                retryPolicySupplier,
                executor).storeFile(uploadListenerOrNull);
    }

    private B2FileVersion uploadLargeFileGuts(ExecutorService executor,
                                              B2PartSizes partSizes,
                                              B2UploadFileRequest request,
                                              long contentLength) throws B2Exception {
        B2LargeFileUploader uploader = new B2LargeFileUploader(retryer, webifier, accountAuthCache, retryPolicySupplier, executor, partSizes, request, contentLength);
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
        return B2PartSizes.from(retryer.doRetry("get_part_sizes", accountAuthCache, accountAuthCache::get, retryPolicySupplier.get()));
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

    @SuppressWarnings("RedundantThrows")
    @Override
    public B2ListFilesIterable fileVersions(B2ListFileVersionsRequest request) throws B2Exception {
        return new B2ListFileVersionsIterable(this, request);
    }

    @SuppressWarnings("RedundantThrows")
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
        retryer.doRetry("b2_cancel_large_file",
                accountAuthCache,
                () -> {
                    webifier.cancelLargeFile(accountAuthCache.get(), request);
                    return 0; // to meet Callable api!
                },
                retryPolicySupplier.get());
    }

    @Override
    public void downloadById(B2DownloadByIdRequest request,
                             B2ContentSink handler) throws B2Exception {
        retryer.doRetry("b2_download_file_by_id",
                accountAuthCache,
                () -> {
                    B2AccountAuthorization accountAuth = accountAuthCache.get();
                    webifier.downloadById(accountAuth, request, handler);
                    return 0; // to meet Callable api!
                },
                retryPolicySupplier.get());
    }

    @Override
    public String getDownloadByIdUrl(B2DownloadByIdRequest request) throws B2Exception {
        return retryer.doRetry("getDownloadByIdUrl",
                accountAuthCache,
                () -> {
                    B2AccountAuthorization accountAuth = accountAuthCache.get();
                    return webifier.getDownloadByIdUrl(accountAuth, request);
                },
                retryPolicySupplier.get());
    }

    @Override
    public String getDownloadByNameUrl(B2DownloadByNameRequest request) throws B2Exception {
        return retryer.doRetry("getDownloadByNameUrl",
                accountAuthCache,
                () -> {
                    B2AccountAuthorization accountAuth = accountAuthCache.get();
                    return webifier.getDownloadByNameUrl(accountAuth, request);
                },
                retryPolicySupplier.get());
    }


    @Override
    public void downloadByName(B2DownloadByNameRequest request,
                               B2ContentSink handler) throws B2Exception {
        retryer.doRetry("b2_download_file_by_name",
                accountAuthCache,
                () -> {
                    B2AccountAuthorization accountAuth = accountAuthCache.get();
                    webifier.downloadByName(accountAuth, request, handler);
                    return 0; // to meet Callable api!
                },
                retryPolicySupplier.get());
    }

    @Override
    public void deleteFileVersion(B2DeleteFileVersionRequest request) throws B2Exception {
        retryer.doRetry("b2_delete_file_version",
                accountAuthCache,
                () -> {
                    webifier.deleteFileVersion(accountAuthCache.get(), request);
                    return 0; // to meet Callable api!
                },
                retryPolicySupplier.get());
    }

    @Override
    public B2DownloadAuthorization getDownloadAuthorization(B2GetDownloadAuthorizationRequest request) throws B2Exception {
        return retryer.doRetry("b2_get_download_authorization", accountAuthCache, () -> webifier.getDownloadAuthorization(accountAuthCache.get(), request), retryPolicySupplier.get());
    }

    @Override
    public B2FileVersion getFileInfo(B2GetFileInfoRequest request) throws B2Exception {
        return retryer.doRetry("b2_get_file_info", accountAuthCache, () -> webifier.getFileInfo(accountAuthCache.get(), request), retryPolicySupplier.get());
    }

    @Override
    public B2FileVersion getFileInfoByName(B2GetFileInfoByNameRequest request) throws B2Exception {
        return retryer.doRetry("get_file_info_by_name", accountAuthCache, () -> webifier.getFileInfoByName(accountAuthCache.get(), request), retryPolicySupplier.get());
    }

    @Override
    public B2FileVersion hideFile(B2HideFileRequest request) throws B2Exception {
        return retryer.doRetry("b2_hide_file", accountAuthCache, () -> webifier.hideFile(accountAuthCache.get(), request), retryPolicySupplier.get());
    }

    @Override
    public B2Bucket updateBucket(B2UpdateBucketRequest request) throws B2Exception {
        return retryer.doRetry("b2_update_bucket", accountAuthCache, () -> webifier.updateBucket(accountAuthCache.get(), request), retryPolicySupplier.get());
    }

    @Override
    public B2Bucket deleteBucket(B2DeleteBucketRequest request) throws B2Exception {
        return retryer.doRetry("b2_delete_bucket", accountAuthCache, () ->  {
            B2DeleteBucketRequestReal realRequest = new B2DeleteBucketRequestReal(getAccountIdWithoutRetry(), request.getBucketId());
            return webifier.deleteBucket(accountAuthCache.get(), realRequest);
        }, retryPolicySupplier.get());
    }

    @Override
    public B2AccountAuthorization getAccountAuthorization() throws B2Exception {
        return retryer.doRetry("b2_authorize_account",
                accountAuthCache,
                accountAuthCache::get,
                retryPolicySupplier.get());
    }

    @Override
    public void invalidateAccountAuthorization() {
        accountAuthCache.clear();
    }


    @Override
    public B2UploadUrlResponse getUploadUrl(B2GetUploadUrlRequest request) throws B2Exception {
        return retryer.doRetry("b2_get_upload_url",
                accountAuthCache,
                () -> webifier.getUploadUrl(accountAuthCache.get(), request),
                retryPolicySupplier.get());
    }

    @Override
    public B2UploadPartUrlResponse getUploadPartUrl(B2GetUploadPartUrlRequest request) throws B2Exception {
        return retryer.doRetry("b2_get_upload_part_url",
                accountAuthCache,
                () -> webifier.getUploadPartUrl(accountAuthCache.get(), request),
                retryPolicySupplier.get());
    }

    @Override
    public B2FileVersion startLargeFile(B2StartLargeFileRequest request) throws B2Exception {
        return retryer.doRetry("b2_start_large_file",
                accountAuthCache, () ->
                        webifier.startLargeFile(accountAuthCache.get(), request),
                retryPolicySupplier.get()
        );
    }

    @Override
    public B2FileVersion finishLargeFile(B2FinishLargeFileRequest request) throws B2Exception {
        return retryer.doRetry("b2_finish_large_file", accountAuthCache,
                () -> webifier.finishLargeFile(accountAuthCache.get(), request),
                retryPolicySupplier.get());
    }

    @Override
    public B2UpdateFileLegalHoldResponse updateFileLegalHold(B2UpdateFileLegalHoldRequest request) throws B2Exception {
        return retryer.doRetry("b2_update_file_legal_hold", accountAuthCache,
                () -> webifier.updateFileLegalHold(accountAuthCache.get(), request),
                retryPolicySupplier.get());
    }

    @Override
    public B2UpdateFileRetentionResponse updateFileRetention(B2UpdateFileRetentionRequest request) throws B2Exception {
        return retryer.doRetry("b2_update_file_retention", accountAuthCache,
                () -> webifier.updateFileRetention(accountAuthCache.get(), request),
                retryPolicySupplier.get());
    }


    //
    // For use by our iterators
    // XXX: make private somehow, or move to B2StorageClient interface.
    //
    B2ListFileVersionsResponse listFileVersions(B2ListFileVersionsRequest request) throws B2Exception {
        return retryer.doRetry("b2_list_file_versions", accountAuthCache, () -> webifier.listFileVersions(accountAuthCache.get(), request), retryPolicySupplier.get());
    }
    B2ListFileNamesResponse listFileNames(B2ListFileNamesRequest request) throws B2Exception {
        return retryer.doRetry("b2_list_file_names", accountAuthCache, () -> webifier.listFileNames(accountAuthCache.get(), request), retryPolicySupplier.get());
    }
    B2ListKeysResponse listKeys(B2ListKeysRequest request) throws B2Exception {

        return retryer.doRetry("b2_list_keys", accountAuthCache, () -> {
            final B2ListKeysRequestReal realRequest =
                    new B2ListKeysRequestReal(
                            getAccountIdWithoutRetry(),
                            request.getMaxKeyCount(),
                            request.getStartApplicationKeyId()
                    );
            return webifier.listKeys(accountAuthCache.get(), realRequest);
        }, retryPolicySupplier.get());
    }
    B2ListUnfinishedLargeFilesResponse listUnfinishedLargeFiles(B2ListUnfinishedLargeFilesRequest request) throws B2Exception {
        return retryer.doRetry("b2_list_unfinished_large_files", accountAuthCache, () -> webifier.listUnfinishedLargeFiles(accountAuthCache.get(), request), retryPolicySupplier.get());
    }
    B2ListPartsResponse listParts(B2ListPartsRequest request) throws B2Exception {
        return retryer.doRetry("b2_list_parts", accountAuthCache, () -> webifier.listParts(accountAuthCache.get(), request), retryPolicySupplier.get());
    }
}
