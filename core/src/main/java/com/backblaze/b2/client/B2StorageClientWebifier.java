/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client;

import com.backblaze.b2.client.contentHandlers.B2ContentSink;
import com.backblaze.b2.client.exceptions.B2Exception;
import com.backblaze.b2.client.structures.B2AccountAuthorization;
import com.backblaze.b2.client.structures.B2ApplicationKey;
import com.backblaze.b2.client.structures.B2AuthorizeAccountRequest;
import com.backblaze.b2.client.structures.B2Bucket;
import com.backblaze.b2.client.structures.B2CancelLargeFileRequest;
import com.backblaze.b2.client.structures.B2CancelLargeFileResponse;
import com.backblaze.b2.client.structures.B2CopyPartRequest;
import com.backblaze.b2.client.structures.B2CopyFileRequest;
import com.backblaze.b2.client.structures.B2CreateBucketRequestReal;
import com.backblaze.b2.client.structures.B2CreateKeyRequestReal;
import com.backblaze.b2.client.structures.B2CreatedApplicationKey;
import com.backblaze.b2.client.structures.B2DeleteBucketRequestReal;
import com.backblaze.b2.client.structures.B2DeleteFileVersionRequest;
import com.backblaze.b2.client.structures.B2DeleteFileVersionResponse;
import com.backblaze.b2.client.structures.B2DeleteKeyRequest;
import com.backblaze.b2.client.structures.B2DownloadAuthorization;
import com.backblaze.b2.client.structures.B2DownloadByIdRequest;
import com.backblaze.b2.client.structures.B2DownloadByNameRequest;
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
import com.backblaze.b2.client.structures.B2ListKeysRequestReal;
import com.backblaze.b2.client.structures.B2ListKeysResponse;
import com.backblaze.b2.client.structures.B2ListPartsRequest;
import com.backblaze.b2.client.structures.B2ListPartsResponse;
import com.backblaze.b2.client.structures.B2ListUnfinishedLargeFilesRequest;
import com.backblaze.b2.client.structures.B2ListUnfinishedLargeFilesResponse;
import com.backblaze.b2.client.structures.B2Part;
import com.backblaze.b2.client.structures.B2StartLargeFileRequest;
import com.backblaze.b2.client.structures.B2UpdateBucketRequest;
import com.backblaze.b2.client.structures.B2UploadFileRequest;
import com.backblaze.b2.client.structures.B2UploadPartRequest;
import com.backblaze.b2.client.structures.B2UploadPartUrlResponse;
import com.backblaze.b2.client.structures.B2UploadUrlResponse;

/**
 * A B2StorageClientWebifier is responsible for converting from request objects
 * to web calls and back again.
 *    * A webifier's main job is to hide the web-ness from the
 *      higher-level logic.
 *    * A webifier does NOT retry calls.
 *    * A webifier is stateless and does not cache anything.
 *    * Users can provide their own B2WebApiClient implementation
 *      to build on their favorite web framework or to help
 *      with unit testing.
 *
 * THREAD-SAFETY: Instances must be as thread-safe as the B2WebApiClient it's given.
 *   if the B2WebApiClient is thread-safe, so is the webifier.  If not, the webifier
 *   isn't.
 *
 */
public interface B2StorageClientWebifier extends AutoCloseable {
    /**
     * @param request the account authorization request.
     * @return an account authorization.
     * @throws B2Exception if there's any trouble.  if it's a B2UnauthorizedException,
     *                     the requestCategory will be set to ACCOUNT_AUTHORIZATION.
     */
    B2AccountAuthorization authorizeAccount(B2AuthorizeAccountRequest request) throws B2Exception;

    B2Bucket createBucket(B2AccountAuthorization accountAuth,
                          B2CreateBucketRequestReal request) throws B2Exception;

    B2CreatedApplicationKey createKey(B2AccountAuthorization accountAuth,
                                      B2CreateKeyRequestReal request) throws B2Exception;

    B2ListKeysResponse listKeys(B2AccountAuthorization accountAuth,
                                B2ListKeysRequestReal request) throws B2Exception;

    B2ApplicationKey deleteKey(B2AccountAuthorization accountAuth,
                               B2DeleteKeyRequest request) throws B2Exception;

    B2ListBucketsResponse listBuckets(B2AccountAuthorization accountAuth,
                                      B2ListBucketsRequest request) throws B2Exception;

    B2UploadUrlResponse getUploadUrl(B2AccountAuthorization accountAuth,
                                     B2GetUploadUrlRequest request) throws B2Exception;

    B2UploadPartUrlResponse getUploadPartUrl(B2AccountAuthorization accountAuth,
                                             B2GetUploadPartUrlRequest request) throws B2Exception;

    B2FileVersion uploadFile(B2UploadUrlResponse uploadUrlResponse,
                             B2UploadFileRequest request) throws B2Exception;

    B2FileVersion copyFile(B2AccountAuthorization accountAuth,
                           B2CopyFileRequest request) throws B2Exception;

    B2Part uploadPart(B2UploadPartUrlResponse uploadPartUrlResponse,
                      B2UploadPartRequest request) throws B2Exception;

    B2Part copyPart(B2AccountAuthorization accountAuth,
                    B2CopyPartRequest request) throws B2Exception;

    B2ListFileVersionsResponse listFileVersions(B2AccountAuthorization accountAuth,
                                                B2ListFileVersionsRequest request) throws B2Exception;

    B2ListFileNamesResponse listFileNames(B2AccountAuthorization accountAuth,
                                          B2ListFileNamesRequest request) throws B2Exception;

    B2ListUnfinishedLargeFilesResponse listUnfinishedLargeFiles(B2AccountAuthorization accountAuth,
                                                                B2ListUnfinishedLargeFilesRequest request) throws B2Exception;

    B2FileVersion startLargeFile(B2AccountAuthorization accountAuth,
                                 B2StartLargeFileRequest request) throws B2Exception;

    B2FileVersion finishLargeFile(B2AccountAuthorization accountAuth,
                                  B2FinishLargeFileRequest request) throws B2Exception;

    B2CancelLargeFileResponse cancelLargeFile(B2AccountAuthorization accountAuth,
                                              B2CancelLargeFileRequest request) throws B2Exception;

    void downloadById(B2AccountAuthorization accountAuth,
                      B2DownloadByIdRequest request,
                      B2ContentSink handler) throws B2Exception;

    void downloadByName(B2AccountAuthorization accountAuth,
                        B2DownloadByNameRequest request,
                        B2ContentSink handler) throws B2Exception;

    B2DeleteFileVersionResponse deleteFileVersion(B2AccountAuthorization accountAuth,
                                                  B2DeleteFileVersionRequest request) throws B2Exception;

    B2DownloadAuthorization getDownloadAuthorization(B2AccountAuthorization accountAuth,
                                                     B2GetDownloadAuthorizationRequest request) throws B2Exception;

    B2FileVersion getFileInfo(B2AccountAuthorization accountAuth,
                              B2GetFileInfoRequest request) throws B2Exception;

    B2FileVersion getFileInfoByName(B2AccountAuthorization accountAuth,
                                    B2GetFileInfoByNameRequest request) throws B2Exception;

    B2FileVersion hideFile(B2AccountAuthorization accountAuth,
                           B2HideFileRequest request) throws B2Exception;

    B2Bucket updateBucket(B2AccountAuthorization accountAuth,
                          B2UpdateBucketRequest request) throws B2Exception;

    B2Bucket deleteBucket(B2AccountAuthorization accountAuth,
                          B2DeleteBucketRequestReal request) throws B2Exception;

    B2ListPartsResponse listParts(B2AccountAuthorization b2AccountAuthorization,
                                  B2ListPartsRequest request) throws B2Exception;

    String getDownloadByIdUrl(B2AccountAuthorization accountAuth,
                              B2DownloadByIdRequest request) throws B2Exception;

    String getDownloadByNameUrl(B2AccountAuthorization accountAuth,
                                B2DownloadByNameRequest request) throws B2Exception;

    /**
     * Closes this object and its underlying resources.
     * This is overridden from AutoCloseable to declare that it can't throw any exception.
     */
    @Override
    void close();
}
