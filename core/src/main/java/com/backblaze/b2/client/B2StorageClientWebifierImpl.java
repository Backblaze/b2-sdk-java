/*
 * Copyright 2020, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client;

import com.backblaze.b2.client.contentHandlers.B2ContentSink;
import com.backblaze.b2.client.contentSources.B2ContentSource;
import com.backblaze.b2.client.contentSources.B2Headers;
import com.backblaze.b2.client.contentSources.B2HeadersImpl;
import com.backblaze.b2.client.exceptions.B2BadRequestException;
import com.backblaze.b2.client.exceptions.B2Exception;
import com.backblaze.b2.client.exceptions.B2LocalException;
import com.backblaze.b2.client.exceptions.B2UnauthorizedException;
import com.backblaze.b2.client.structures.B2AccountAuthorization;
import com.backblaze.b2.client.structures.B2ApplicationKey;
import com.backblaze.b2.client.structures.B2AuthorizationFilteredResponseField;
import com.backblaze.b2.client.structures.B2AuthorizeAccountRequest;
import com.backblaze.b2.client.structures.B2Bucket;
import com.backblaze.b2.client.structures.B2CancelLargeFileRequest;
import com.backblaze.b2.client.structures.B2CancelLargeFileResponse;
import com.backblaze.b2.client.structures.B2Capabilities;
import com.backblaze.b2.client.structures.B2CopyFileRequest;
import com.backblaze.b2.client.structures.B2CopyPartRequest;
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
import com.backblaze.b2.client.structures.B2FileRetention;
import com.backblaze.b2.client.structures.B2FileSseForRequest;
import com.backblaze.b2.client.structures.B2FileSseForResponse;
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
import com.backblaze.b2.client.structures.B2OverrideableHeaders;
import com.backblaze.b2.client.structures.B2Part;
import com.backblaze.b2.client.structures.B2StartLargeFileRequest;
import com.backblaze.b2.client.structures.B2TestMode;
import com.backblaze.b2.client.structures.B2UpdateBucketRequest;
import com.backblaze.b2.client.structures.B2UpdateFileLegalHoldRequest;
import com.backblaze.b2.client.structures.B2UpdateFileLegalHoldResponse;
import com.backblaze.b2.client.structures.B2UpdateFileRetentionRequest;
import com.backblaze.b2.client.structures.B2UpdateFileRetentionResponse;
import com.backblaze.b2.client.structures.B2UploadFileRequest;
import com.backblaze.b2.client.structures.B2UploadListener;
import com.backblaze.b2.client.structures.B2UploadPartRequest;
import com.backblaze.b2.client.structures.B2UploadPartUrlResponse;
import com.backblaze.b2.client.structures.B2UploadUrlResponse;
import com.backblaze.b2.client.webApiClients.B2WebApiClient;
import com.backblaze.b2.json.B2Json;
import com.backblaze.b2.util.B2ByteProgressListener;
import com.backblaze.b2.util.B2ByteRange;
import com.backblaze.b2.util.B2InputStreamWithByteProgressListener;
import com.backblaze.b2.util.B2Preconditions;
import com.backblaze.b2.util.B2StringUtil;

import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static com.backblaze.b2.client.contentSources.B2Headers.FILE_ID;
import static com.backblaze.b2.client.contentSources.B2Headers.FILE_NAME;
import static com.backblaze.b2.client.structures.B2ServerSideEncryptionMode.SSE_B2;
import static com.backblaze.b2.client.structures.B2ServerSideEncryptionMode.SSE_C;
import static com.backblaze.b2.util.B2StringUtil.percentEncode;

public class B2StorageClientWebifierImpl implements B2StorageClientWebifier {

    // This path specifies which version of the B2 APIs to use.
    // See: https://www.backblaze.com/b2/docs/versions.html
    private static String API_VERSION_PATH = "b2api/v2/";

    private final B2WebApiClient webApiClient;
    private final String userAgent;
    private final Base64.Encoder base64Encoder = Base64.getEncoder();

    // the masterUrl is a url like "https://api.backblazeb2.com/".
    // this url is only used for authorizeAccount.  after that,
    // the urls from the accountAuthorization or other requests
    // that return a url are used.
    //
    // it always ends with a '/'.
    private final String masterUrl;
    private final B2TestMode testModeOrNull;

    public B2StorageClientWebifierImpl(B2WebApiClient webApiClient,
                                       String userAgent,
                                       String masterUrl,
                                       B2TestMode testModeOrNull) {
        throwIfBadUserAgent(userAgent);
        this.webApiClient = webApiClient;
        this.userAgent = userAgent;
        this.masterUrl = masterUrl.endsWith("/") ?
                masterUrl :
                masterUrl + "/";
        this.testModeOrNull = testModeOrNull;
    }

    String getMasterUrl() {
        return masterUrl;
    }

    // see https://tools.ietf.org/html/rfc7231
    // for now, let's just make sure there aren't any characters that are
    // traditional ascii control characters, including \r and \n since they
    // could mess up our HTTP headers.
    private static void throwIfBadUserAgent(String userAgent) {
        userAgent.chars().forEach( c -> B2Preconditions.checkArgument(c >= 32, "control character in user-agent!"));
    }

    private static class Empty {
        @B2Json.constructor(params = "")
        Empty() {
        }
    }

    @Override
    public void close() {
        webApiClient.close();
    }

    @Override
    public B2AccountAuthorization authorizeAccount(B2AuthorizeAccountRequest request) throws B2Exception {
        final B2HeadersImpl.Builder headersBuilder = B2HeadersImpl
                .builder()
                .set(B2Headers.AUTHORIZATION, makeAuthorizationValue(request));
        setCommonHeaders(headersBuilder);
        final B2Headers headers = headersBuilder.build();

        final String url = masterUrl + API_VERSION_PATH + "b2_authorize_account";
        try {
            return webApiClient.postJsonReturnJson(
                    url,
                    headers,
                    new Empty(), // the arguments are in the header.
                    B2AccountAuthorization.class);
        } catch (B2UnauthorizedException e) {
            e.setRequestCategory(B2UnauthorizedException.RequestCategory.ACCOUNT_AUTHORIZATION);
            throw e;
        }
    }

    private String makeAuthorizationValue(B2AuthorizeAccountRequest request) {
        final String value = request.getApplicationKeyId() + ":" + request.getApplicationKey();
        return "Basic " + base64Encoder.encodeToString(B2StringUtil.getUtf8Bytes(value));
    }

    @Override
    public B2Bucket createBucket(B2AccountAuthorization accountAuth,
                                 B2CreateBucketRequestReal request) throws B2Exception {
        return webApiClient.postJsonReturnJson(
                makeUrl(accountAuth, "b2_create_bucket"),
                makeHeaders(accountAuth),
                request,
                B2Bucket.class);
    }

    @Override
    public B2CreatedApplicationKey createKey(B2AccountAuthorization accountAuth, B2CreateKeyRequestReal request) throws B2Exception {
        return webApiClient.postJsonReturnJson(
                makeUrl(accountAuth, "b2_create_key"),
                makeHeaders(accountAuth),
                request,
                B2CreatedApplicationKey.class
        );
    }

    @Override
    public B2ListKeysResponse listKeys(B2AccountAuthorization accountAuth, B2ListKeysRequestReal request) throws B2Exception {
        return webApiClient.postJsonReturnJson(
                makeUrl(accountAuth, "b2_list_keys"),
                makeHeaders(accountAuth),
                request,
                B2ListKeysResponse.class
        );
    }

    @Override
    public B2ApplicationKey deleteKey(B2AccountAuthorization accountAuth, B2DeleteKeyRequest request) throws B2Exception {
        return webApiClient.postJsonReturnJson(
                makeUrl(accountAuth, "b2_delete_key"),
                makeHeaders(accountAuth),
                request,
                B2ApplicationKey.class
        );
    }

    @Override
    public B2ListBucketsResponse listBuckets(B2AccountAuthorization accountAuth,
                                             B2ListBucketsRequest request) throws B2Exception {
        return webApiClient.postJsonReturnJson(
                makeUrl(accountAuth, "b2_list_buckets"),
                makeHeaders(accountAuth),
                request,
                B2ListBucketsResponse.class);
    }

    @Override
    public B2UploadUrlResponse getUploadUrl(B2AccountAuthorization accountAuth,
                                            B2GetUploadUrlRequest request) throws B2Exception {
        return webApiClient.postJsonReturnJson(
                makeUrl(accountAuth, "b2_get_upload_url"),
                makeHeaders(accountAuth),
                request,
                B2UploadUrlResponse.class);

    }

    @Override
    public B2UploadPartUrlResponse getUploadPartUrl(B2AccountAuthorization accountAuth,
                                                    B2GetUploadPartUrlRequest request) throws B2Exception {
        return webApiClient.postJsonReturnJson(
                makeUrl(accountAuth, "b2_get_upload_part_url"),
                makeHeaders(accountAuth),
                request,
                B2UploadPartUrlResponse.class);
    }

    @Override
    public B2FileVersion uploadFile(B2UploadUrlResponse uploadUrlResponse,
                                    B2UploadFileRequest request) throws B2Exception {
        final B2UploadListener uploadListener = request.getListener();
        final B2ContentSource source = request.getContentSource();
        try (final B2ContentDetailsForUpload contentDetails = new B2ContentDetailsForUpload(request.getContentSource())) {
            final long contentLen = contentDetails.getContentLength();

            uploadListener.progress(B2UploadProgressUtil.forSmallFileWaitingToStart(contentLen));
            uploadListener.progress(B2UploadProgressUtil.forSmallFileStarting(contentLen));

            // build the headers.
            final B2HeadersImpl.Builder headersBuilder = B2HeadersImpl
                    .builder()
                    .set(B2Headers.EXPECT, "100-continue")
                    .set(B2Headers.AUTHORIZATION, uploadUrlResponse.getAuthorizationToken())
                    .set(FILE_NAME, percentEncode(request.getFileName()))
                    .set(B2Headers.CONTENT_TYPE, request.getContentType())
                    .set(B2Headers.CONTENT_SHA1, contentDetails.getContentSha1HeaderValue());
            setCommonHeaders(headersBuilder);

            if (request.getServerSideEncryption() != null) {
                switch (request.getServerSideEncryption().getMode()) {
                    case SSE_B2:
                        headersBuilder.set(B2Headers.SERVER_SIDE_ENCRYPTION,
                                request.getServerSideEncryption().getAlgorithm());
                        break;
                    case SSE_C:
                        headersBuilder.set(B2Headers.SERVER_SIDE_ENCRYPTION_CUSTOMER_ALGORITHM,
                                request.getServerSideEncryption().getAlgorithm());
                        headersBuilder.set(B2Headers.SERVER_SIDE_ENCRYPTION_CUSTOMER_KEY,
                                request.getServerSideEncryption().getCustomerKey());
                        headersBuilder.set(B2Headers.SERVER_SIDE_ENCRYPTION_CUSTOMER_KEY_MD5,
                                request.getServerSideEncryption().getCustomerKeyMd5());
                        break;
                    default:
                        throw new B2LocalException("invalid_sse_mode", "invalid SSE mode in uploadFile");
                }
            }

            if (request.getLegalHold() != null) {
                headersBuilder.set(B2Headers.FILE_LEGAL_HOLD,
                        request.getLegalHold());
            }

            if (request.getFileRetention() != null) {
                // no need to send file retention headers; but may need to receive one for HEAD calls
                // discussed inside getFileInfoByName
                if (request.getFileRetention().getMode() != null) {
                    headersBuilder.set(B2Headers.FILE_RETENTION_MODE,
                            request.getFileRetention().getMode());
                }
                if (request.getFileRetention().getRetainUntilTimestamp() != null) {
                    headersBuilder.set(B2Headers.FILE_RETENTION_RETAIN_UNTIL_TIMESTAMP,
                            request.getFileRetention().getRetainUntilTimestamp().toString());
                }
            }

            // if the source provides a last-modified time, add it.
            final Long lastModMillis;
            try {
                lastModMillis = source.getSrcLastModifiedMillisOrNull();
            } catch (IOException e) {
                throw new B2LocalException("read_failed", "failed to get lastModified from source: " + e, e);
            }
            if (lastModMillis != null) {
                headersBuilder.set(B2Headers.SRC_LAST_MODIFIED_MILLIS, Long.toString(lastModMillis, 10));
            }

            // add any custom file infos.
            // Only percent encode the values.  Check the keys for legal characters
            for (Map.Entry<String, String> entry : request.getFileInfo().entrySet()) {
                validateFileInfoName(entry.getKey());
                headersBuilder.set(B2Headers.FILE_INFO_PREFIX + entry.getKey(), percentEncode(entry.getValue()));
            }

            final B2ByteProgressListener progressAdapter = new B2UploadProgressAdapter(uploadListener, 0, 1, 0, contentLen);
            final B2ByteProgressFilteringListener progressListener = new B2ByteProgressFilteringListener(progressAdapter);

            try {
                final B2FileVersion version = webApiClient.postDataReturnJson(
                        uploadUrlResponse.getUploadUrl(),
                        headersBuilder.build(),
                        new B2InputStreamWithByteProgressListener(contentDetails.getInputStream(), progressListener),
                        contentLen,
                        B2FileVersion.class);
                        //if (System.getenv("FAIL_ME") != null) {
                        //    throw new B2LocalException("test", "failing on purpose!");
                        //}

                uploadListener.progress(B2UploadProgressUtil.forSmallFileSucceeded(contentLen));
                return version;
            } catch (B2UnauthorizedException e) {
                e.setRequestCategory(B2UnauthorizedException.RequestCategory.UPLOADING);
                uploadListener.progress(B2UploadProgressUtil.forSmallFileFailed(contentLen, progressListener.getBytesSoFar()));
                throw e;
            } catch (B2Exception e) {
                uploadListener.progress(B2UploadProgressUtil.forSmallFileFailed(contentLen, progressListener.getBytesSoFar()));
                throw e;
            }
        }
    }

    @Override
    public B2FileVersion copyFile(B2AccountAuthorization accountAuth, B2CopyFileRequest request) throws B2Exception {
            return webApiClient.postJsonReturnJson(
                    makeUrl(accountAuth, "b2_copy_file"),
                    makeHeaders(accountAuth),
                    request,
                    B2FileVersion.class);
    }

    @Override
    public B2Part uploadPart(B2UploadPartUrlResponse uploadPartUrlResponse,
                             B2UploadPartRequest request) throws B2Exception {
        final B2ContentSource source = request.getContentSource();
        try (final B2ContentDetailsForUpload contentDetails = new B2ContentDetailsForUpload(source)) {

            final B2HeadersImpl.Builder headersBuilder = B2HeadersImpl
                    .builder()
                    .set(B2Headers.EXPECT, "100-continue")
                    .set(B2Headers.AUTHORIZATION, uploadPartUrlResponse.getAuthorizationToken())
                    .set(B2Headers.PART_NUMBER, Integer.toString(request.getPartNumber()))
                    .set(B2Headers.CONTENT_SHA1, contentDetails.getContentSha1HeaderValue());
            setCommonHeaders(headersBuilder);

            if (request.getServerSideEncryption() != null) {
                B2Preconditions.checkArgument(request.getServerSideEncryption().getMode().equals(SSE_C));
                headersBuilder.set(B2Headers.SERVER_SIDE_ENCRYPTION_CUSTOMER_ALGORITHM,
                        request.getServerSideEncryption().getAlgorithm());
                headersBuilder.set(B2Headers.SERVER_SIDE_ENCRYPTION_CUSTOMER_KEY,
                        request.getServerSideEncryption().getCustomerKey());
                headersBuilder.set(B2Headers.SERVER_SIDE_ENCRYPTION_CUSTOMER_KEY_MD5,
                        request.getServerSideEncryption().getCustomerKeyMd5());
            }

            try {
                return webApiClient.postDataReturnJson(
                        uploadPartUrlResponse.getUploadUrl(),
                        headersBuilder.build(),
                        contentDetails.getInputStream(),
                        contentDetails.getContentLength(),
                        B2Part.class);
            } catch (B2UnauthorizedException e) {
                e.setRequestCategory(B2UnauthorizedException.RequestCategory.UPLOADING);
                throw e;
            }
        }
    }


    @Override
    public B2Part copyPart(B2AccountAuthorization accountAuth, B2CopyPartRequest request) throws B2Exception {
        return webApiClient.postJsonReturnJson(
                makeUrl(accountAuth, "b2_copy_part"),
                makeHeaders(accountAuth),
                request,
                B2Part.class);
    }


    @Override
    public B2ListFileVersionsResponse listFileVersions(B2AccountAuthorization accountAuth,
                                                       B2ListFileVersionsRequest request) throws B2Exception {
        return webApiClient.postJsonReturnJson(
                makeUrl(accountAuth, "b2_list_file_versions"),
                makeHeaders(accountAuth),
                request,
                B2ListFileVersionsResponse.class);
    }

    @Override
    public B2ListFileNamesResponse listFileNames(B2AccountAuthorization accountAuth,
                                                 B2ListFileNamesRequest request) throws B2Exception {
        return webApiClient.postJsonReturnJson(
                makeUrl(accountAuth, "b2_list_file_names"),
                makeHeaders(accountAuth),
                request,
                B2ListFileNamesResponse.class);
    }

    @Override
    public B2ListUnfinishedLargeFilesResponse listUnfinishedLargeFiles(B2AccountAuthorization accountAuth,
                                                                       B2ListUnfinishedLargeFilesRequest request) throws B2Exception {
        return webApiClient.postJsonReturnJson(
                makeUrl(accountAuth, "b2_list_unfinished_large_files"),
                makeHeaders(accountAuth),
                request,
                B2ListUnfinishedLargeFilesResponse.class);
    }

    @Override
    public B2FileVersion startLargeFile(B2AccountAuthorization accountAuth,
                                        B2StartLargeFileRequest request) throws B2Exception {
        return webApiClient.postJsonReturnJson(
                makeUrl(accountAuth, "b2_start_large_file"),
                makeHeaders(accountAuth),
                request,
                B2FileVersion.class);
    }

    @Override
    public B2FileVersion finishLargeFile(B2AccountAuthorization accountAuth,
                                         B2FinishLargeFileRequest request) throws B2Exception {
        return webApiClient.postJsonReturnJson(
                makeUrl(accountAuth, "b2_finish_large_file"),
                makeHeaders(accountAuth),
                request,
                B2FileVersion.class);
    }

    @Override
    public B2ListPartsResponse listParts(B2AccountAuthorization accountAuth,
                                         B2ListPartsRequest request) throws B2Exception {
        return webApiClient.postJsonReturnJson(
                makeUrl(accountAuth, "b2_list_parts"),
                makeHeaders(accountAuth),
                request,
                B2ListPartsResponse.class);
    }

    @Override
    public B2CancelLargeFileResponse cancelLargeFile(
            B2AccountAuthorization accountAuth,
            B2CancelLargeFileRequest request) throws B2Exception {
        return webApiClient.postJsonReturnJson(
                makeUrl(accountAuth, "b2_cancel_large_file"),
                makeHeaders(accountAuth),
                request,
                B2CancelLargeFileResponse.class);
    }

    @Override
    public void downloadById(B2AccountAuthorization accountAuth,
                             B2DownloadByIdRequest request,
                             B2ContentSink handler) throws B2Exception {
        downloadGuts(accountAuth,
                makeDownloadByIdUrl(accountAuth, request),
                request.getRange(),
                request.getServerSideEncryption(),
                handler);
    }

    @Override
    public String getDownloadByIdUrl(B2AccountAuthorization accountAuth,
                              B2DownloadByIdRequest request) {
        return makeDownloadByIdUrl(accountAuth, request);
    }

    @Override
    public void downloadByName(B2AccountAuthorization accountAuth,
                               B2DownloadByNameRequest request,
                               B2ContentSink handler) throws B2Exception {
        downloadGuts(accountAuth,
                makeDownloadByNameUrl(accountAuth, request.getBucketName(), request.getFileName(), request),
                request.getRange(),
                request.getServerSideEncryption(),
                handler);
    }

    @Override
    public String getDownloadByNameUrl(B2AccountAuthorization accountAuth,
                                       B2DownloadByNameRequest request) {
        return makeDownloadByNameUrl(accountAuth, request.getBucketName(), request.getFileName(), request);
    }

    private void downloadGuts(B2AccountAuthorization accountAuth,
                              String url,
                              B2ByteRange rangeOrNull,
                              B2FileSseForRequest serverSideEncryptionOrNull,
                              B2ContentSink handler) throws B2Exception {
        final Map<String, String> extras = new TreeMap<>();
        if (rangeOrNull != null) {
            extras.put(B2Headers.RANGE, rangeOrNull.toString());
        }
        if (serverSideEncryptionOrNull != null) {
            B2Preconditions.checkArgument(serverSideEncryptionOrNull.getMode().equals(SSE_C));
            extras.put(B2Headers.SERVER_SIDE_ENCRYPTION_CUSTOMER_ALGORITHM, serverSideEncryptionOrNull.getAlgorithm());
            extras.put(B2Headers.SERVER_SIDE_ENCRYPTION_CUSTOMER_KEY, serverSideEncryptionOrNull.getCustomerKey());
            extras.put(B2Headers.SERVER_SIDE_ENCRYPTION_CUSTOMER_KEY_MD5, serverSideEncryptionOrNull.getCustomerKeyMd5());
        }
        webApiClient.getContent(
                url,
                makeHeaders(accountAuth, extras),
                handler);
    }

    @Override
    public B2DeleteFileVersionResponse deleteFileVersion(B2AccountAuthorization accountAuth,
                                                         B2DeleteFileVersionRequest request) throws B2Exception {
        return webApiClient.postJsonReturnJson(
                makeUrl(accountAuth, "b2_delete_file_version"),
                makeHeaders(accountAuth),
                request,
                B2DeleteFileVersionResponse.class);
    }

    @Override
    public B2DownloadAuthorization getDownloadAuthorization(B2AccountAuthorization accountAuth,
                                                            B2GetDownloadAuthorizationRequest request) throws B2Exception {
        return webApiClient.postJsonReturnJson(
                makeUrl(accountAuth, "b2_get_download_authorization"),
                makeHeaders(accountAuth),
                request,
                B2DownloadAuthorization.class);
    }

    @Override
    public B2FileVersion getFileInfo(B2AccountAuthorization accountAuth,
                                     B2GetFileInfoRequest request) throws B2Exception {
        return webApiClient.postJsonReturnJson(
                makeUrl(accountAuth, "b2_get_file_info"),
                makeHeaders(accountAuth),
                request,
                B2FileVersion.class);
    }

    @Override
    public B2FileVersion getFileInfoByName(B2AccountAuthorization accountAuth,
                                           B2GetFileInfoByNameRequest request) throws B2Exception {
        final Map<String, String> extras = new TreeMap<>();
        if (request.getServerSideEncryption() != null) {
            B2Preconditions.checkArgument(request.getServerSideEncryption().getMode().equals(SSE_C));
            extras.put(B2Headers.SERVER_SIDE_ENCRYPTION_CUSTOMER_ALGORITHM,
                request.getServerSideEncryption().getAlgorithm());
            extras.put(B2Headers.SERVER_SIDE_ENCRYPTION_CUSTOMER_KEY,
                request.getServerSideEncryption().getCustomerKey());
            extras.put(B2Headers.SERVER_SIDE_ENCRYPTION_CUSTOMER_KEY_MD5,
                request.getServerSideEncryption().getCustomerKeyMd5());
        }

        B2Headers headers = webApiClient.head(makeGetFileInfoByNameUrl(accountAuth, request.getBucketName(),
                request.getFileName()), makeHeaders(accountAuth, extras));

        final B2FileRetention b2FileRetentionOrNull = B2FileRetention.getFileRetentionFromHeadersOrNull(headers);
        final String legalHoldOrNull = headers.getFileLegalHoldOrNull();

        final List<String> capabilities = accountAuth.getAllowed().getCapabilities();
        final B2AuthorizationFilteredResponseField<B2FileRetention> fileRetention;
        // we rely on getCapabilities() rather than the CLIENT_UNAUTHORIZED_TO_READ header because the header is
        // not sent for all files in all buckets due to header size limitations
        if (capabilities.contains(B2Capabilities.READ_FILE_RETENTIONS)) {
            fileRetention = new B2AuthorizationFilteredResponseField<>(true, b2FileRetentionOrNull);
        } else {
            fileRetention = new B2AuthorizationFilteredResponseField<>(false, null);
        }

        final B2AuthorizationFilteredResponseField<String> legalHold;
        // we rely on getCapabilities() rather than the CLIENT_UNAUTHORIZED_TO_READ header because the header is
        // not sent for all files in all buckets due to header size limitations
        if (capabilities.contains(B2Capabilities.READ_FILE_LEGAL_HOLDS)) {
            legalHold = new B2AuthorizationFilteredResponseField<>(true, legalHoldOrNull);
        } else {
            legalHold = new B2AuthorizationFilteredResponseField<>(false, null);
        }

        // b2_download_file_by_name promises most of these will be present, except as noted below,
        return new B2FileVersion(
                headers.getValueOrNull(FILE_ID),
                headers.getFileNameOrNull(),
                headers.getContentLength(),
                headers.getContentType(),
                headers.getContentSha1OrNull(),    // might be null.
                headers.getContentMd5OrNull(),    // might be null.
                headers.getB2FileInfo(),           // might be empty.
                "upload",
                headers.getUploadTimestampOrNull(),
                fileRetention,
                legalHold,
                B2FileSseForResponse.getEncryptionFromHeadersOrNull(headers), // might be null.
                null // might be null
        );
    }

    @Override
    public B2FileVersion hideFile(B2AccountAuthorization accountAuth,
                                  B2HideFileRequest request) throws B2Exception {
        return webApiClient.postJsonReturnJson(
                makeUrl(accountAuth, "b2_hide_file"),
                makeHeaders(accountAuth),
                request,
                B2FileVersion.class);
    }

    @Override
    public B2Bucket updateBucket(B2AccountAuthorization accountAuth,
                                 B2UpdateBucketRequest request) throws B2Exception {
        return webApiClient.postJsonReturnJson(
                makeUrl(accountAuth, "b2_update_bucket"),
                makeHeaders(accountAuth),
                request,
                B2Bucket.class);
    }

    @Override
    public B2Bucket deleteBucket(B2AccountAuthorization accountAuth,
                                 B2DeleteBucketRequestReal request) throws B2Exception {
        return webApiClient.postJsonReturnJson(
                makeUrl(accountAuth, "b2_delete_bucket"),
                makeHeaders(accountAuth),
                request,
                B2Bucket.class);
    }

    @Override
    public B2UpdateFileLegalHoldResponse updateFileLegalHold(B2AccountAuthorization accountAuth,
                                                             B2UpdateFileLegalHoldRequest request) throws B2Exception {
        return webApiClient.postJsonReturnJson(
                makeUrl(accountAuth, "b2_update_file_legal_hold"),
                makeHeaders(accountAuth),
                request,
                B2UpdateFileLegalHoldResponse.class);
    }

    @Override
    public B2UpdateFileRetentionResponse updateFileRetention(B2AccountAuthorization accountAuth,
                                                             B2UpdateFileRetentionRequest request) throws B2Exception {
        return webApiClient.postJsonReturnJson(
                makeUrl(accountAuth, "b2_update_file_retention"),
                makeHeaders(accountAuth),
                request,
                B2UpdateFileRetentionResponse.class);
    }

    private void addAuthHeader(B2HeadersImpl.Builder builder,
                               B2AccountAuthorization accountAuth) {
        builder.set(B2Headers.AUTHORIZATION, accountAuth.getAuthorizationToken());
    }

    private B2Headers makeHeaders(B2AccountAuthorization accountAuth) {
        return makeHeaders(accountAuth, null);
    }

    private B2Headers makeHeaders(B2AccountAuthorization accountAuth, Map<String,String> extrasPairsOrNull) {
        final B2HeadersImpl.Builder builder = B2HeadersImpl
                .builder();
        addAuthHeader(builder, accountAuth);
        if (extrasPairsOrNull != null) {
            extrasPairsOrNull.forEach(builder::set);
        }
        setCommonHeaders(builder);

        return builder.build();
    }

    private void setCommonHeaders(B2HeadersImpl.Builder builder) {
        builder.set(B2Headers.USER_AGENT, userAgent);

        //
        // note that not all test modes affect every request,
        // but let's keep it simple and send with every request.
        //
        if (testModeOrNull != null) {
            builder.set(B2Headers.TEST_MODE, testModeOrNull.getValueForHeader());
        }
    }


    private String makeUrl(B2AccountAuthorization accountAuth,
                           String apiName) {
        String url = accountAuth.getApiUrl();
        if (!url.endsWith("/")) {
            url += "/";
        }
        url += API_VERSION_PATH;
        url += apiName;
        return url;
    }

    private String makeDownloadByIdUrl(B2AccountAuthorization accountAuth,
                                       B2DownloadByIdRequest request) {
        B2Preconditions.checkArgumentIsNotNull(request, "request");
        final String downloadUrl = accountAuth.getDownloadUrl();
        final StringBuilder uriBuilder = new StringBuilder(downloadUrl);

        if (!downloadUrl.endsWith("/")) {
            uriBuilder.append("/");
        }

        uriBuilder
                .append(API_VERSION_PATH)
                .append("b2_download_file_by_id?fileId=")
                .append(request.getFileId());

        maybeAddOverrideHeadersToUrl(uriBuilder, 1, request);
        return uriBuilder.toString();
    }

    private String makeGetFileInfoByNameUrl(B2AccountAuthorization accountAuth,
                                         String bucketName,
                                         String fileName) {
        return makeDownloadByNameUrl(accountAuth, bucketName, fileName, null);
    }

    private String makeDownloadByNameUrl(B2AccountAuthorization accountAuth,
                                         String bucketName,
                                         String fileName,
                                         B2DownloadByNameRequest request) {
        final String downloadUrl = accountAuth.getDownloadUrl();
        final StringBuilder uriBuilder = new StringBuilder(downloadUrl);

        if (!downloadUrl.endsWith("/")) {
            uriBuilder.append("/");
        }

        uriBuilder
                .append("file/")
                .append(bucketName)
                .append("/")
                .append(percentEncode(fileName));

        if (request != null) {
            maybeAddOverrideHeadersToUrl(uriBuilder, 0, request);
        }
        return uriBuilder.toString();
    }

    /**
     * Add query parameters for each overridden header
     *
     * @param uriBuilder StringBuilder of the URI to append to
     * @param countOfQueryParameters number of query parameters already added to the URI
     * @param overrideableHeaders overridden headers to add to the URI
     * @return number of query parameters that have been added to the URI (including countOfQueryParameters)
     */
    private int maybeAddOverrideHeadersToUrl(StringBuilder uriBuilder, int countOfQueryParameters, B2OverrideableHeaders overrideableHeaders) {
        countOfQueryParameters = maybeAddQueryParamToUrl(uriBuilder, countOfQueryParameters, "b2ContentDisposition", overrideableHeaders.getB2ContentDisposition());
        countOfQueryParameters = maybeAddQueryParamToUrl(uriBuilder, countOfQueryParameters, "b2ContentLanguage", overrideableHeaders.getB2ContentLanguage());
        countOfQueryParameters = maybeAddQueryParamToUrl(uriBuilder, countOfQueryParameters, "b2Expires", overrideableHeaders.getB2Expires());
        countOfQueryParameters = maybeAddQueryParamToUrl(uriBuilder, countOfQueryParameters, "b2CacheControl", overrideableHeaders.getB2CacheControl());
        countOfQueryParameters = maybeAddQueryParamToUrl(uriBuilder, countOfQueryParameters, "b2ContentEncoding", overrideableHeaders.getB2ContentEncoding());
        countOfQueryParameters = maybeAddQueryParamToUrl(uriBuilder, countOfQueryParameters, "b2ContentType", overrideableHeaders.getB2ContentType());

        return countOfQueryParameters;
    }

    /**
     * If argValue isn't null, this will append a query parameter to uri builder
     * with the prefix '?' when countOfQueryParameters is zero and '&' otherwise
     * This will return the countOfQueryParameters + 1 if the query parameter was
     * added to the uri builder and countOfQueryParameters otherwise.
     *
     * @param uriBuilder StringBuilder of the URI to append to
     * @param countOfQueryParameters number of query parameters already added to the URI
     * @param argName name of query parameter
     * @param argValue value of query parameter
     * @return countOfQueryParameters + 1 if a query parameter was added,
     *         countOfQueryParameters otherwise
     */
    private int maybeAddQueryParamToUrl(StringBuilder uriBuilder, int countOfQueryParameters, String argName, String argValue) {
        if (argValue != null) {
            final char separator = countOfQueryParameters == 0 ? '?' : '&';
            uriBuilder
                    .append(separator)
                    .append(argName)
                    .append('=')
                    .append(percentEncode(argValue));

            return countOfQueryParameters + 1;
        }

        return countOfQueryParameters;
    }

    /**
     * Validates whether each char in key is a valid header character according to RFC 7230:
     * https://tools.ietf.org/html/rfc7230#section-3.2
     *
     * @param name The String to validate
     * @throws B2BadRequestException if any of the characters are not valid
     */
    /*testing*/ void validateFileInfoName(String name) throws B2BadRequestException {
        for (int i = 0; i < name.length(); i++) {
            if (!isLegalInfoNameCharacter(name.charAt(i))) {
                throw new B2BadRequestException(B2BadRequestException.DEFAULT_CODE,
                        null,
                        "Illegal file info name: " + name);
            }
        }
    }

    private boolean isLegalInfoNameCharacter(char c) {
        /*
          Chars allowed in header as defined by: https://tools.ietf.org/html/rfc7230#section-3.2.6
         */
        return
                ('a' <= c && c <= 'z') ||
                ('A' <= c && c <= 'Z') ||
                ('0' <= c && c <= '9') ||
                c == '-'  ||
                c == '_'  ||
                c == '.'  ||
                c == '!'  ||
                c == '#'  ||
                c == '$'  ||
                c == '%'  ||
                c == '&'  ||
                c == '\'' ||
                c == '*'  ||
                c == '+'  ||
                c == '^'  ||
                c == '`'  ||
                c == '|'  ||
                c == '~';
    }
}
