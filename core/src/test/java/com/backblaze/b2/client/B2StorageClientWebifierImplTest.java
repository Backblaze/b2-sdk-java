/*
 * Copyright 2021, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client;

import com.backblaze.b2.client.contentHandlers.B2ContentSink;
import com.backblaze.b2.client.contentSources.B2ByteArrayContentSource;
import com.backblaze.b2.client.contentSources.B2ContentSource;
import com.backblaze.b2.client.contentSources.B2ContentTypes;
import com.backblaze.b2.client.contentSources.B2Headers;
import com.backblaze.b2.client.exceptions.B2BadRequestException;
import com.backblaze.b2.client.exceptions.B2Exception;
import com.backblaze.b2.client.exceptions.B2UnauthorizedException;
import com.backblaze.b2.client.structures.B2AccountAuthorization;
import com.backblaze.b2.client.structures.B2AuthorizeAccountRequest;
import com.backblaze.b2.client.structures.B2BucketTypes;
import com.backblaze.b2.client.structures.B2CancelLargeFileRequest;
import com.backblaze.b2.client.structures.B2CopyFileRequest;
import com.backblaze.b2.client.structures.B2CopyPartRequest;
import com.backblaze.b2.client.structures.B2CreateBucketRequest;
import com.backblaze.b2.client.structures.B2CreateBucketRequestReal;
import com.backblaze.b2.client.structures.B2DeleteBucketRequestReal;
import com.backblaze.b2.client.structures.B2DeleteFileVersionRequest;
import com.backblaze.b2.client.structures.B2DownloadByIdRequest;
import com.backblaze.b2.client.structures.B2DownloadByNameRequest;
import com.backblaze.b2.client.structures.B2FileRetention;
import com.backblaze.b2.client.structures.B2FileRetentionMode;
import com.backblaze.b2.client.structures.B2FileSseForRequest;
import com.backblaze.b2.client.structures.B2FileVersion;
import com.backblaze.b2.client.structures.B2FinishLargeFileRequest;
import com.backblaze.b2.client.structures.B2GetDownloadAuthorizationRequest;
import com.backblaze.b2.client.structures.B2GetFileInfoByNameRequest;
import com.backblaze.b2.client.structures.B2GetFileInfoRequest;
import com.backblaze.b2.client.structures.B2GetUploadPartUrlRequest;
import com.backblaze.b2.client.structures.B2GetUploadUrlRequest;
import com.backblaze.b2.client.structures.B2HideFileRequest;
import com.backblaze.b2.client.structures.B2LegalHold;
import com.backblaze.b2.client.structures.B2ListBucketsRequest;
import com.backblaze.b2.client.structures.B2ListFileNamesRequest;
import com.backblaze.b2.client.structures.B2ListFileVersionsRequest;
import com.backblaze.b2.client.structures.B2ListPartsRequest;
import com.backblaze.b2.client.structures.B2ListUnfinishedLargeFilesRequest;
import com.backblaze.b2.client.structures.B2StartLargeFileRequest;
import com.backblaze.b2.client.structures.B2TestMode;
import com.backblaze.b2.client.structures.B2UpdateBucketRequest;
import com.backblaze.b2.client.structures.B2UpdateFileLegalHoldRequest;
import com.backblaze.b2.client.structures.B2UpdateFileRetentionRequest;
import com.backblaze.b2.client.structures.B2UploadFileRequest;
import com.backblaze.b2.client.structures.B2UploadListener;
import com.backblaze.b2.client.structures.B2UploadPartRequest;
import com.backblaze.b2.client.structures.B2UploadPartUrlResponse;
import com.backblaze.b2.client.structures.B2UploadProgress;
import com.backblaze.b2.client.structures.B2UploadState;
import com.backblaze.b2.client.structures.B2UploadUrlResponse;
import com.backblaze.b2.client.webApiClients.B2WebApiClient;
import com.backblaze.b2.util.B2BaseTest;
import com.backblaze.b2.util.B2ByteRange;
import com.backblaze.b2.util.B2Collections;
import com.backblaze.b2.util.B2IoUtils;
import com.backblaze.b2.util.B2Preconditions;
import com.backblaze.b2.util.B2StringUtil;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static com.backblaze.b2.client.B2TestHelpers.bucketId;
import static com.backblaze.b2.client.B2TestHelpers.bucketName;
import static com.backblaze.b2.client.B2TestHelpers.fileId;
import static com.backblaze.b2.client.B2TestHelpers.fileName;
import static com.backblaze.b2.client.B2TestHelpers.makeBucket;
import static com.backblaze.b2.client.B2TestHelpers.makeFileHeaders;
import static com.backblaze.b2.client.B2TestHelpers.uploadPartUrlResponse;
import static com.backblaze.b2.client.B2TestHelpers.uploadUrlResponse;
import static com.backblaze.b2.client.exceptions.B2UnauthorizedException.RequestCategory.ACCOUNT_AUTHORIZATION;
import static com.backblaze.b2.client.exceptions.B2UnauthorizedException.RequestCategory.OTHER;
import static com.backblaze.b2.client.exceptions.B2UnauthorizedException.RequestCategory.UPLOADING;
import static com.backblaze.b2.json.B2Json.toJsonOrThrowRuntime;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * This test verifies that the B2StorageClientWebifierImpl translates
 * calls to it into the proper web api calls.
 */
public class B2StorageClientWebifierImplTest extends B2BaseTest {
    private static final String USER_AGENT = "SecretAgentMan/3.19.28";
    private static final String MASTER_URL = "https://api.testb2.com";

    private static final B2AccountAuthorization ACCOUNT_AUTH = B2TestHelpers.makeAuth(1);
    private static final String ACCOUNT_ID = ACCOUNT_AUTH.getAccountId();
    private static final String APPLICATION_KEY = "applicationKey";

    private static final String CONTENTS = "Hello, World!";
    private static final byte[] CONTENTS_BYTES = B2StringUtil.getUtf8Bytes(CONTENTS);
    private static final String SHA1 = "0a0a9f2a6772942557ab5355d76af442f8f65e01";
    private static final long SRC_LAST_MODIFIED_MILLIS = 1234567L;

    private static final CountingContentSource contentSourceWithSha1 = new CountingContentSource(B2ByteArrayContentSource
            .builder(CONTENTS_BYTES)
            .setSha1OrNull(SHA1)
            .setSrcLastModifiedMillisOrNull(SRC_LAST_MODIFIED_MILLIS)
            .build());
    private static final CountingContentSource contentSourceWithoutSha1 = new CountingContentSource(B2ByteArrayContentSource
            .builder(CONTENTS_BYTES)
            .setSrcLastModifiedMillisOrNull(SRC_LAST_MODIFIED_MILLIS)
            .build());

    private static final CountingContentSource contentSourceWithoutOptionalData = new CountingContentSource(B2ByteArrayContentSource
            .builder(CONTENTS_BYTES)
            .build());

    private static final CountingContentSource contentSourceWithExceptionInGetSrcLastModifiedMillis = new CountingContentSource(new ThrowsInGetSrcLastModifiedMillis(B2ByteArrayContentSource
            .builder(CONTENTS_BYTES)
            .build()));




    private RecordingWebApiClient webApiClient = new RecordingWebApiClient();

    // i'm doing most tests with the a test mode to ensure it's set on all request type.
    // there's a separate test it can be disabled; that test only checks one request type.
    private B2StorageClientWebifierImpl webifier = new B2StorageClientWebifierImpl(
            webApiClient,
            USER_AGENT,
            MASTER_URL,
            B2TestMode.FORCE_CAP_EXCEEDED
    );

    private B2ContentSink noopContentHandler = (r, i) -> {};


    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private interface Requester {
        void request(B2StorageClientWebifier webifier) throws B2Exception;
    }

    private static class AlwaysThrowsUnauthorizedWebClientApi implements B2WebApiClient {

        @Override
        public <ResponseType> ResponseType postJsonReturnJson(String url,
                                                              B2Headers headersOrNull,
                                                              Object request,
                                                              Class<ResponseType> responseClass) throws B2Exception {
            throw new B2UnauthorizedException("unauthorized", null, "unauthorized msg");
        }

        @Override
        public <ResponseType> ResponseType postDataReturnJson(String url,
                                                              B2Headers headersOrNull,
                                                              InputStream contentSource,
                                                              long contentLength,
                                                              Class<ResponseType> responseClass) throws B2Exception {
            throw new B2UnauthorizedException("unauthorized", null, "unauthorized msg");
        }

        @Override
        public void getContent(String url,
                               B2Headers headersOrNull,
                               B2ContentSink handler) throws B2Exception {
            throw new B2UnauthorizedException("unauthorized", null, "unauthorized msg");
        }

        @Override
        public B2Headers head(String url, B2Headers headersOrNull) throws B2Exception {
            throw new B2UnauthorizedException("unauthorized", null, "unauthorized msg");
        }

        @Override
        public void close() {
        }
    }


    private static class RecordingWebApiClient implements B2WebApiClient {
        private String callDescription;

        @Override
        public <ResponseType> ResponseType postJsonReturnJson(String url,
                                                              B2Headers headersOrNull,
                                                              Object request,
                                                              Class<ResponseType> responseClass) {
            B2Preconditions.checkArgument(callDescription == null, "called more than once?");

            callDescription = ("postJsonReturnJson.\n" +
                    "url:\n" +
                    indent(url) + "\n" +
                    "headers:\n" +
                    indent(toString(headersOrNull)) + "\n" +
                    "request:\n" +
                    indent(toJsonOrThrowRuntime(request)) + "\n" +
                    "responseClass:\n" +
                    indent(responseClass.getSimpleName()) + "\n");
            return null;
        }

        private String indent(Object objectOrNull) {
            final String indentation = "    ";
            final String toIndent = (objectOrNull != null) ? objectOrNull.toString() : "null";
            return indentation + toIndent.replace("\n", "\n" + indentation);
        }

        private String toString(B2Headers headers) {
            StringBuilder b = new StringBuilder();
            boolean isFirst = true;
            for (String name : headers.getNames()) {
                if (isFirst) {
                    isFirst = false;
                } else {
                    b.append("\n");
                }
                b.append(name);
                b.append(": ");
                b.append(headers.getValueOrNull(name));
            }
            return b.toString();
        }

        private String copyUtf8ToString(InputStream inputStream) {
            try {
                final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                B2IoUtils.copy(inputStream, outputStream);
                return outputStream.toString(B2StringUtil.UTF8);
            } catch (IOException e) {
                throw new RuntimeException("unexpected exception: " + e, e);
            }
        }

        @Override
        public <ResponseType> ResponseType postDataReturnJson(String url,
                                                              B2Headers headersOrNull,
                                                              InputStream inputStream,
                                                              long contentLength,
                                                              Class<ResponseType> responseClass) {
            B2Preconditions.checkArgument(callDescription == null, "called more than once?");

            callDescription = ("postJsonReturnJson.\n" +
                    "url:\n" +
                    indent(url) + "\n" +
                    "headers:\n" +
                    indent(toString(headersOrNull)) + "\n" +
                    "inputStream:\n" +
                    indent(copyUtf8ToString(inputStream)) + "\n" +
                    "contentLength:\n" +
                    indent("") + contentLength + "\n" +
                    "responseClass:\n" +
                    indent(responseClass.getSimpleName()) + "\n");
            return null;
        }

        @Override
        public void getContent(String url,
                               B2Headers headersOrNull,
                               B2ContentSink handler) {
            B2Preconditions.checkArgument(callDescription == null, "called more than once?");

            callDescription = ("getContent.\n" +
                    "url:\n" +
                    indent(url) + "\n" +
                    "headers:\n" +
                    indent(toString(headersOrNull)) + "\n");
        }

        @Override
        public B2Headers head(String url, B2Headers headersOrNull) {
            callDescription = ("head.\n" +
                    "url:\n" +
                    indent(url) + "\n" +
                    "headers:\n" +
                    indent(toString(headersOrNull)) + "\n" );
            return makeFileHeaders(1);
        }

        @Override
        public void close() {
        }

        void check(String expectedRequest) {
            assertEquals(expectedRequest, callDescription);
        }
    }

    /**
     * Use this to wrap another B2ContentSource and count the number of times
     * createInputStream is called and the number of times the streams are
     * closed.  Useful for checking that they're balanced!
     */
    private static class CountingContentSource implements B2ContentSource {
        private final B2ContentSource nested;
        private final AtomicInteger numCreated = new AtomicInteger(0);
        private final AtomicInteger numClosed = new AtomicInteger(0);

        private CountingContentSource(B2ContentSource nested) {
            this.nested = nested;
        }


        @Override
        public long getContentLength() throws IOException {
            return nested.getContentLength();
        }

        @Override
        public String getSha1OrNull() throws IOException {
            return nested.getSha1OrNull();
        }

        @Override
        public Long getSrcLastModifiedMillisOrNull() throws IOException {
            return nested.getSrcLastModifiedMillisOrNull();
        }

        @Override
        public InputStream createInputStream() throws IOException, B2Exception {
            numCreated.incrementAndGet();
            final InputStream newStream = nested.createInputStream();
            return new InputStream() {
                @Override
                public int read() throws IOException {
                    return newStream.read();
                }
                @Override
                public void close() throws IOException {
                    newStream.close();
                    numClosed.incrementAndGet();
                }
            };
        }

        void checkBalanced() {
            assertEquals(numCreated.get(), numClosed.get());
        }
    }

    @After
    public void tearDown() {
        contentSourceWithSha1.checkBalanced();
        contentSourceWithoutOptionalData.checkBalanced();
    }


    @Test
    public void testThrowsForUserAgentWithControlCharacters() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("control character in user-agent!");

        new B2StorageClientWebifierImpl(webApiClient, USER_AGENT + "\n", MASTER_URL, null);
    }

    @Test
    public void testDoesntAddExtraSlashAtEndOfMasterUrl() {
        //noinspection ConstantConditions
        assertFalse(MASTER_URL.endsWith("/"));

        final B2StorageClientWebifierImpl ifier = new B2StorageClientWebifierImpl(
                webApiClient,
                USER_AGENT,
                MASTER_URL + "/",
                null);
        assertTrue(ifier.getMasterUrl().endsWith("/"));
        assertFalse(ifier.getMasterUrl().endsWith("//"));
    }

    @Test
    public void testAuthorizeAccount() throws B2Exception {
        final B2AuthorizeAccountRequest request = B2AuthorizeAccountRequest
                .builder(ACCOUNT_ID, APPLICATION_KEY)
                .build();
        webifier.authorizeAccount(request);

        webApiClient.check("postJsonReturnJson.\n" +
                        "url:\n" +
                        "    https://api.testb2.com/b2api/v2/b2_authorize_account\n" +
                        "headers:\n" +
                        "    Authorization: Basic MTphcHBsaWNhdGlvbktleQ==\n" +
                        "    User-Agent: SecretAgentMan/3.19.28\n" +
                        "    X-Bz-Test-Mode: force_cap_exceeded\n" +
                        "request:\n" +
                        "    {}\n" +
                        "responseClass:\n" +
                        "    B2AccountAuthorization\n"
        );

        checkRequestCategory(ACCOUNT_AUTHORIZATION, w -> w.authorizeAccount(request));
    }


    @Test
    public void testCreateBucket() throws B2Exception {
        final B2CreateBucketRequest request = B2CreateBucketRequest
                .builder(bucketName(1), B2BucketTypes.ALL_PUBLIC)
                .setCorsRules(Collections.singletonList(B2TestHelpers.makeCorsRule()))
                .build();
        final B2CreateBucketRequestReal requestReal = new B2CreateBucketRequestReal(ACCOUNT_ID, request);
        webifier.createBucket(ACCOUNT_AUTH, requestReal);

        webApiClient.check("postJsonReturnJson.\n" +
                "url:\n" +
                "    apiUrl1/b2api/v2/b2_create_bucket\n" +
                "headers:\n" +
                "    Authorization: accountToken1\n" +
                "    User-Agent: SecretAgentMan/3.19.28\n" +
                "    X-Bz-Test-Mode: force_cap_exceeded\n" +
                "request:\n" +
                "    {\n" +
                "      \"accountId\": \"1\",\n" +
                "      \"bucketInfo\": null,\n" +
                "      \"bucketName\": \"bucketName1\",\n" +
                "      \"bucketType\": \"allPublic\",\n" +
                "      \"corsRules\": [\n" +
                "        {\n" +
                "          \"allowedHeaders\": null,\n" +
                "          \"allowedOperations\": [\n" +
                "            \"b2_download_file_by_id\"\n" +
                "          ],\n" +
                "          \"allowedOrigins\": [\n" +
                "            \"https://something.com\"\n" +
                "          ],\n" +
                "          \"corsRuleName\": \"rule-name\",\n" +
                "          \"exposeHeaders\": null,\n" +
                "          \"maxAgeSeconds\": 0\n" +
                "        }\n" +
                "      ],\n" +
                "      \"defaultServerSideEncryption\": null,\n" +
                "      \"fileLockEnabled\": false,\n" +
                "      \"lifecycleRules\": null,\n" +
                "      \"replicationConfiguration\": null\n" +
                "    }\n" +
                "responseClass:\n" +
                "    B2Bucket\n"
        );

        checkRequestCategory(OTHER, w -> w.createBucket(ACCOUNT_AUTH, requestReal));
    }

    @Test
    public void testListBuckets() throws B2Exception {
        final B2ListBucketsRequest request = B2ListBucketsRequest
                .builder(ACCOUNT_ID)
                .build();
        webifier.listBuckets(ACCOUNT_AUTH, request);

        webApiClient.check("postJsonReturnJson.\n" +
                "url:\n" +
                "    apiUrl1/b2api/v2/b2_list_buckets\n" +
                "headers:\n" +
                "    Authorization: accountToken1\n" +
                "    User-Agent: SecretAgentMan/3.19.28\n" +
                "    X-Bz-Test-Mode: force_cap_exceeded\n" +
                "request:\n" +
                "    {\n" +
                "      \"accountId\": \"1\",\n" +
                "      \"bucketId\": null,\n" +
                "      \"bucketName\": null,\n" +
                "      \"bucketTypes\": null\n" +
                "    }\n" +
                "responseClass:\n" +
                "    B2ListBucketsResponse\n"
        );

        checkRequestCategory(OTHER, w -> w.listBuckets(ACCOUNT_AUTH, request));
    }

    @Test
    public void testGetUploadUrl() throws B2Exception {
        final B2GetUploadUrlRequest request = B2GetUploadUrlRequest.builder(bucketId(1)).build();
        webifier.getUploadUrl(ACCOUNT_AUTH, request);

        webApiClient.check("postJsonReturnJson.\n" +
                "url:\n" +
                "    apiUrl1/b2api/v2/b2_get_upload_url\n" +
                "headers:\n" +
                "    Authorization: accountToken1\n" +
                "    User-Agent: SecretAgentMan/3.19.28\n" +
                "    X-Bz-Test-Mode: force_cap_exceeded\n" +
                "request:\n" +
                "    {\n" +
                "      \"bucketId\": \"bucket1\"\n" +
                "    }\n" +
                "responseClass:\n" +
                "    B2UploadUrlResponse\n"
        );

        checkRequestCategory(OTHER, w -> w.getUploadUrl(ACCOUNT_AUTH, request));
    }

    @Test
    public void testGetUploadPartUrl() throws B2Exception {
        final B2GetUploadPartUrlRequest request = B2GetUploadPartUrlRequest.builder(fileId(1)).build();
        webifier.getUploadPartUrl(ACCOUNT_AUTH, request);

        webApiClient.check("postJsonReturnJson.\n" +
                "url:\n" +
                "    apiUrl1/b2api/v2/b2_get_upload_part_url\n" +
                "headers:\n" +
                "    Authorization: accountToken1\n" +
                "    User-Agent: SecretAgentMan/3.19.28\n" +
                "    X-Bz-Test-Mode: force_cap_exceeded\n" +
                "request:\n" +
                "    {\n" +
                "      \"fileId\": \"4_zBlah_0000001\"\n" +
                "    }\n" +
                "responseClass:\n" +
                "    B2UploadPartUrlResponse\n"
        );

        checkRequestCategory(OTHER, w -> w.getUploadPartUrl(ACCOUNT_AUTH, request));
    }

    @Test
    public void testListFileVersions() throws B2Exception {
        final B2ListFileVersionsRequest request = B2ListFileVersionsRequest
                .builder(bucketId(1))
                .build();
        webifier.listFileVersions(ACCOUNT_AUTH, request);

        webApiClient.check("postJsonReturnJson.\n" +
                "url:\n" +
                "    apiUrl1/b2api/v2/b2_list_file_versions\n" +
                "headers:\n" +
                "    Authorization: accountToken1\n" +
                "    User-Agent: SecretAgentMan/3.19.28\n" +
                "    X-Bz-Test-Mode: force_cap_exceeded\n" +
                "request:\n" +
                "    {\n" +
                "      \"bucketId\": \"bucket1\",\n" +
                "      \"delimiter\": null,\n" +
                "      \"maxFileCount\": null,\n" +
                "      \"prefix\": null,\n" +
                "      \"startFileId\": null,\n" +
                "      \"startFileName\": null\n" +
                "    }\n" +
                "responseClass:\n" +
                "    B2ListFileVersionsResponse\n"
        );

        checkRequestCategory(OTHER, w -> w.listFileVersions(ACCOUNT_AUTH, request));
    }

    @Test
    public void testListFileNames() throws B2Exception {
        final B2ListFileNamesRequest request = B2ListFileNamesRequest
                .builder(bucketId(1))
                .build();
        webifier.listFileNames(ACCOUNT_AUTH, request);

        webApiClient.check("postJsonReturnJson.\n" +
                "url:\n" +
                "    apiUrl1/b2api/v2/b2_list_file_names\n" +
                "headers:\n" +
                "    Authorization: accountToken1\n" +
                "    User-Agent: SecretAgentMan/3.19.28\n" +
                "    X-Bz-Test-Mode: force_cap_exceeded\n" +
                "request:\n" +
                "    {\n" +
                "      \"bucketId\": \"bucket1\",\n" +
                "      \"delimiter\": null,\n" +
                "      \"maxFileCount\": null,\n" +
                "      \"prefix\": null,\n" +
                "      \"startFileName\": null\n" +
                "    }\n" +
                "responseClass:\n" +
                "    B2ListFileNamesResponse\n"
        );

        checkRequestCategory(OTHER, w -> w.listFileNames(ACCOUNT_AUTH, request));
    }

    @Test
    public void testListUnfinishedLargeFiles() throws B2Exception {
        final B2ListUnfinishedLargeFilesRequest request = B2ListUnfinishedLargeFilesRequest
                .builder(bucketId(1))
                .build();
        webifier.listUnfinishedLargeFiles(ACCOUNT_AUTH, request);

        webApiClient.check("postJsonReturnJson.\n" +
                "url:\n" +
                "    apiUrl1/b2api/v2/b2_list_unfinished_large_files\n" +
                "headers:\n" +
                "    Authorization: accountToken1\n" +
                "    User-Agent: SecretAgentMan/3.19.28\n" +
                "    X-Bz-Test-Mode: force_cap_exceeded\n" +
                "request:\n" +
                "    {\n" +
                "      \"bucketId\": \"bucket1\",\n" +
                "      \"maxFileCount\": null,\n" +
                "      \"namePrefix\": null,\n" +
                "      \"startFileId\": null\n" +
                "    }\n" +
                "responseClass:\n" +
                "    B2ListUnfinishedLargeFilesResponse\n"
        );

        checkRequestCategory(OTHER, w -> w.listUnfinishedLargeFiles(ACCOUNT_AUTH, request));
    }

    @Test
    public void testStartLargeFile_withoutSha1() throws B2Exception {
        final B2UploadFileRequest uploadRequest = B2UploadFileRequest
                .builder(bucketId(1), fileName(1), B2ContentTypes.TEXT_PLAIN, contentSourceWithoutSha1)
                .setCustomField("color", "blue")
                .build();
        final B2StartLargeFileRequest request = B2StartLargeFileRequest
                .buildFrom(uploadRequest);
        webifier.startLargeFile(ACCOUNT_AUTH, request);

        webApiClient.check("postJsonReturnJson.\n" +
                "url:\n" +
                "    apiUrl1/b2api/v2/b2_start_large_file\n" +
                "headers:\n" +
                "    Authorization: accountToken1\n" +
                "    User-Agent: SecretAgentMan/3.19.28\n" +
                "    X-Bz-Test-Mode: force_cap_exceeded\n" +
                "request:\n" +
                "    {\n" +
                "      \"bucketId\": \"bucket1\",\n" +
                "      \"contentType\": \"text/plain\",\n" +
                "      \"fileInfo\": {\n" +
                "        \"color\": \"blue\"\n" +
                "      },\n" +
                "      \"fileName\": \"files/\u81ea\u7531/0001\",\n" +
                "      \"serverSideEncryption\": null\n" +
                "    }\n" +
                "responseClass:\n" +
                "    B2FileVersion\n"
        );

        checkRequestCategory(OTHER, w -> w.startLargeFile(ACCOUNT_AUTH, request));
    }

    @Test
    public void testStartLargeFile_withSha1() throws B2Exception, IOException {
        final String sha1 = contentSourceWithSha1.getSha1OrNull();
        final B2UploadFileRequest uploadRequest = B2UploadFileRequest
                .builder(bucketId(1), fileName(1), B2ContentTypes.TEXT_PLAIN, contentSourceWithSha1)
                .setCustomField("color", "blue")
                .setCustomField(B2Headers.LARGE_FILE_SHA1_INFO_NAME, sha1)
                .build();
        final B2StartLargeFileRequest request = B2StartLargeFileRequest
                .buildFrom(uploadRequest);
        webifier.startLargeFile(ACCOUNT_AUTH, request);

        webApiClient.check("postJsonReturnJson.\n" +
                "url:\n" +
                "    apiUrl1/b2api/v2/b2_start_large_file\n" +
                "headers:\n" +
                "    Authorization: accountToken1\n" +
                "    User-Agent: SecretAgentMan/3.19.28\n" +
                "    X-Bz-Test-Mode: force_cap_exceeded\n" +
                "request:\n" +
                "    {\n" +
                "      \"bucketId\": \"bucket1\",\n" +
                "      \"contentType\": \"text/plain\",\n" +
                "      \"fileInfo\": {\n" +
                "        \"color\": \"blue\",\n" +
                "        \"large_file_sha1\": \"" + sha1 + "\"\n" +
                "      },\n" +
                "      \"fileName\": \"files/\u81ea\u7531/0001\",\n" +
                "      \"serverSideEncryption\": null\n" +
                "    }\n" +
                "responseClass:\n" +
                "    B2FileVersion\n"
        );

        checkRequestCategory(OTHER, w -> w.startLargeFile(ACCOUNT_AUTH, request));
    }

    @Test
    public void testStartLargeFile_withSseC() throws B2Exception, IOException {
        final String sha1 = contentSourceWithSha1.getSha1OrNull();
        final B2UploadFileRequest uploadRequest = B2UploadFileRequest
                .builder(bucketId(1), fileName(1), B2ContentTypes.TEXT_PLAIN, contentSourceWithSha1)
                .setCustomField("color", "blue")
                .setCustomField(B2Headers.LARGE_FILE_SHA1_INFO_NAME, sha1)
                .setServerSideEncryption(B2FileSseForRequest.createSseCAes256("customer-key", "customer-key-md5"))
                .build();
        final B2StartLargeFileRequest request = B2StartLargeFileRequest
                .buildFrom(uploadRequest);
        webifier.startLargeFile(ACCOUNT_AUTH, request);

        webApiClient.check("postJsonReturnJson.\n" +
                "url:\n" +
                "    apiUrl1/b2api/v2/b2_start_large_file\n" +
                "headers:\n" +
                "    Authorization: accountToken1\n" +
                "    User-Agent: SecretAgentMan/3.19.28\n" +
                "    X-Bz-Test-Mode: force_cap_exceeded\n" +
                "request:\n" +
                "    {\n" +
                "      \"bucketId\": \"bucket1\",\n" +
                "      \"contentType\": \"text/plain\",\n" +
                "      \"fileInfo\": {\n" +
                "        \"color\": \"blue\",\n" +
                "        \"large_file_sha1\": \"" + sha1 + "\"\n" +
                "      },\n" +
                "      \"fileName\": \"files/\u81ea\u7531/0001\",\n" +
                "      \"serverSideEncryption\": {\n" +
                        "        \"algorithm\": \"AES256\",\n" +
                        "        \"customerKey\": \"customer-key\",\n" +
                        "        \"customerKeyMd5\": \"customer-key-md5\",\n" +
                        "        \"mode\": \"SSE-C\"\n" +
                        "      }\n" +
                "    }\n" +
                "responseClass:\n" +
                "    B2FileVersion\n"
        );

        checkRequestCategory(OTHER, w -> w.startLargeFile(ACCOUNT_AUTH, request));
    }

    @Test
    public void testStartLargeFile_withFileRetention() throws B2Exception, IOException {
        final String sha1 = contentSourceWithSha1.getSha1OrNull();
        final long expirationInMillis = Instant.now().plus(3L, ChronoUnit.DAYS).toEpochMilli();
        final B2UploadFileRequest uploadRequest = B2UploadFileRequest
                .builder(bucketId(1), fileName(1), B2ContentTypes.TEXT_PLAIN, contentSourceWithSha1)
                .setFileRetention(new B2FileRetention(B2FileRetentionMode.COMPLIANCE, expirationInMillis))
                .build();
        final B2StartLargeFileRequest request = B2StartLargeFileRequest
                .buildFrom(uploadRequest);
        webifier.startLargeFile(ACCOUNT_AUTH, request);

        webApiClient.check("postJsonReturnJson.\n" +
                "url:\n" +
                "    apiUrl1/b2api/v2/b2_start_large_file\n" +
                "headers:\n" +
                "    Authorization: accountToken1\n" +
                "    User-Agent: SecretAgentMan/3.19.28\n" +
                "    X-Bz-Test-Mode: force_cap_exceeded\n" +
                "request:\n" +
                "    {\n" +
                "      \"bucketId\": \"bucket1\",\n" +
                "      \"contentType\": \"text/plain\",\n" +
                "      \"fileInfo\": {\n" +
                "        \"large_file_sha1\": \"" + sha1 + "\"\n" +
                "      },\n" +
                "      \"fileName\": \"files/\u81ea\u7531/0001\",\n" +
                "      \"fileRetention\": {\n" +
                "        \"mode\": \"compliance\",\n" +
                "        \"retainUntilTimestamp\": " + expirationInMillis + "\n" +
                "      },\n" +
                "      \"serverSideEncryption\": null\n" +
                "    }\n" +
                "responseClass:\n" +
                "    B2FileVersion\n"
        );

        checkRequestCategory(OTHER, w -> w.startLargeFile(ACCOUNT_AUTH, request));
    }

    @Test
    public void testStartLargeFile_withLegalHold() throws B2Exception, IOException {
        final String sha1 = contentSourceWithSha1.getSha1OrNull();
        final B2UploadFileRequest uploadRequest = B2UploadFileRequest
                .builder(bucketId(1), fileName(1), B2ContentTypes.TEXT_PLAIN, contentSourceWithSha1)
                .setLegalHold(B2LegalHold.ON)
                .build();
        final B2StartLargeFileRequest request = B2StartLargeFileRequest
                .buildFrom(uploadRequest);
        webifier.startLargeFile(ACCOUNT_AUTH, request);

        webApiClient.check("postJsonReturnJson.\n" +
                "url:\n" +
                "    apiUrl1/b2api/v2/b2_start_large_file\n" +
                "headers:\n" +
                "    Authorization: accountToken1\n" +
                "    User-Agent: SecretAgentMan/3.19.28\n" +
                "    X-Bz-Test-Mode: force_cap_exceeded\n" +
                "request:\n" +
                "    {\n" +
                "      \"bucketId\": \"bucket1\",\n" +
                "      \"contentType\": \"text/plain\",\n" +
                "      \"fileInfo\": {\n" +
                "        \"large_file_sha1\": \"" + sha1 + "\"\n" +
                "      },\n" +
                "      \"fileName\": \"files/\u81ea\u7531/0001\",\n" +
                "      \"legalHold\": \"on\",\n" +
                "      \"serverSideEncryption\": null\n" +
                "    }\n" +
                "responseClass:\n" +
                "    B2FileVersion\n"
        );

        checkRequestCategory(OTHER, w -> w.startLargeFile(ACCOUNT_AUTH, request));
    }

    @Test
    public void testFinishLargeFile() throws B2Exception {
        final B2FinishLargeFileRequest request = B2FinishLargeFileRequest
                .builder(fileId(1),
                        B2Collections.listOf("sha1_1", "sha1_2", "sha1_3"))
                .build();
        webifier.finishLargeFile(ACCOUNT_AUTH, request);

        webApiClient.check("postJsonReturnJson.\n" +
                "url:\n" +
                "    apiUrl1/b2api/v2/b2_finish_large_file\n" +
                "headers:\n" +
                "    Authorization: accountToken1\n" +
                "    User-Agent: SecretAgentMan/3.19.28\n" +
                "    X-Bz-Test-Mode: force_cap_exceeded\n" +
                "request:\n" +
                "    {\n" +
                "      \"fileId\": \"4_zBlah_0000001\",\n" +
                "      \"partSha1Array\": [\n" +
                "        \"sha1_1\",\n" +
                "        \"sha1_2\",\n" +
                "        \"sha1_3\"\n" +
                "      ]\n" +
                "    }\n" +
                "responseClass:\n" +
                "    B2FileVersion\n"
        );

        checkRequestCategory(OTHER, w -> w.finishLargeFile(ACCOUNT_AUTH, request));
    }

    @Test
    public void testCancelLargeFile() throws B2Exception {
        final B2CancelLargeFileRequest request = B2CancelLargeFileRequest
                .builder(fileId(1))
                .build();
        webifier.cancelLargeFile(ACCOUNT_AUTH, request);

        webApiClient.check("postJsonReturnJson.\n" +
                "url:\n" +
                "    apiUrl1/b2api/v2/b2_cancel_large_file\n" +
                "headers:\n" +
                "    Authorization: accountToken1\n" +
                "    User-Agent: SecretAgentMan/3.19.28\n" +
                "    X-Bz-Test-Mode: force_cap_exceeded\n" +
                "request:\n" +
                "    {\n" +
                "      \"fileId\": \"4_zBlah_0000001\"\n" +
                "    }\n" +
                "responseClass:\n" +
                "    B2CancelLargeFileResponse\n"
        );

        checkRequestCategory(OTHER, w -> w.cancelLargeFile(ACCOUNT_AUTH, request));
    }

    @Test
    public void testListParts() throws B2Exception {
        final B2ListPartsRequest request = B2ListPartsRequest
                .builder(fileId(1))
                .build();
        webifier.listParts(ACCOUNT_AUTH, request);

        webApiClient.check("postJsonReturnJson.\n" +
                "url:\n" +
                "    apiUrl1/b2api/v2/b2_list_parts\n" +
                "headers:\n" +
                "    Authorization: accountToken1\n" +
                "    User-Agent: SecretAgentMan/3.19.28\n" +
                "    X-Bz-Test-Mode: force_cap_exceeded\n" +
                "request:\n" +
                "    {\n" +
                "      \"fileId\": \"4_zBlah_0000001\",\n" +
                "      \"maxPartCount\": null,\n" +
                "      \"startPartNumber\": null\n" +
                "    }\n" +
                "responseClass:\n" +
                "    B2ListPartsResponse\n"
        );

        checkRequestCategory(OTHER, w -> w.listParts(ACCOUNT_AUTH, request));
    }

    @Test
    public void testDeleteFileVersion() throws B2Exception {
        final B2DeleteFileVersionRequest request = B2DeleteFileVersionRequest
                .builder(fileName(1), fileId(1))
                .build();
        webifier.deleteFileVersion(ACCOUNT_AUTH, request);

        webApiClient.check("postJsonReturnJson.\n" +
                "url:\n" +
                "    apiUrl1/b2api/v2/b2_delete_file_version\n" +
                "headers:\n" +
                "    Authorization: accountToken1\n" +
                "    User-Agent: SecretAgentMan/3.19.28\n" +
                "    X-Bz-Test-Mode: force_cap_exceeded\n" +
                "request:\n" +
                "    {\n" +
                "      \"bypassGovernance\": false,\n" +
                "      \"fileId\": \"4_zBlah_0000001\",\n" +
                "      \"fileName\": \"files/\u81ea\u7531/0001\"\n" +
                "    }\n" +
                "responseClass:\n" +
                "    B2DeleteFileVersionResponse\n"
        );

        checkRequestCategory(OTHER, w -> w.deleteFileVersion(ACCOUNT_AUTH, request));
    }

    @Test
    public void testGetDownloadAuthorization() throws B2Exception {
        final B2GetDownloadAuthorizationRequest request = B2GetDownloadAuthorizationRequest
                .builder(bucketId(1), fileName(1), 123)
                .setB2ContentDisposition("attachment; filename=\"example file name.txt\"")
                .build();
        webifier.getDownloadAuthorization(ACCOUNT_AUTH, request);

        webApiClient.check("postJsonReturnJson.\n" +
                "url:\n" +
                "    apiUrl1/b2api/v2/b2_get_download_authorization\n" +
                "headers:\n" +
                "    Authorization: accountToken1\n" +
                "    User-Agent: SecretAgentMan/3.19.28\n" +
                "    X-Bz-Test-Mode: force_cap_exceeded\n" +
                "request:\n" +
                "    {\n" +
                "      \"b2CacheControl\": null,\n" +
                "      \"b2ContentDisposition\": \"attachment; filename=\\\"example file name.txt\\\"\",\n" +
                "      \"b2ContentEncoding\": null,\n" +
                "      \"b2ContentLanguage\": null,\n" +
                "      \"b2ContentType\": null,\n" +
                "      \"b2Expires\": null,\n" +
                "      \"bucketId\": \"bucket1\",\n" +
                "      \"fileNamePrefix\": \"files/\u81ea\u7531/0001\",\n" +
                "      \"validDurationInSeconds\": 123\n" +
                "    }\n" +
                "responseClass:\n" +
                "    B2DownloadAuthorization\n"
        );

        checkRequestCategory(OTHER, w -> w.getDownloadAuthorization(ACCOUNT_AUTH, request));
    }

    @Test
    public void testGetDownloadAuthorizationWithAllOverrides() throws B2Exception {
        final B2GetDownloadAuthorizationRequest request = B2GetDownloadAuthorizationRequest
                .builder(bucketId(1), fileName(1), 123)
                .setB2ContentDisposition("attachment; filename=\"example file name.txt\"")
                .setB2ContentLanguage("en-us")
                .setB2Expires(LocalDateTime.of(2030, 1, 1, 1, 0))
                .setB2CacheControl("max-age=100")
                .setB2ContentEncoding("gzip")
                .setB2ContentType("text/plain")
                .build();
        webifier.getDownloadAuthorization(ACCOUNT_AUTH, request);

        webApiClient.check("postJsonReturnJson.\n" +
                "url:\n" +
                "    apiUrl1/b2api/v2/b2_get_download_authorization\n" +
                "headers:\n" +
                "    Authorization: accountToken1\n" +
                "    User-Agent: SecretAgentMan/3.19.28\n" +
                "    X-Bz-Test-Mode: force_cap_exceeded\n" +
                "request:\n" +
                "    {\n" +
                "      \"b2CacheControl\": \"max-age=100\",\n" +
                "      \"b2ContentDisposition\": \"attachment; filename=\\\"example file name.txt\\\"\",\n" +
                "      \"b2ContentEncoding\": \"gzip\",\n" +
                "      \"b2ContentLanguage\": \"en-us\",\n" +
                "      \"b2ContentType\": \"text/plain\",\n" +
                "      \"b2Expires\": \"Tue, 1 Jan 2030 01:00:00 GMT\",\n" +
                "      \"bucketId\": \"bucket1\",\n" +
                "      \"fileNamePrefix\": \"files/\u81ea\u7531/0001\",\n" +
                "      \"validDurationInSeconds\": 123\n" +
                "    }\n" +
                "responseClass:\n" +
                "    B2DownloadAuthorization\n"
        );

        checkRequestCategory(OTHER, w -> w.getDownloadAuthorization(ACCOUNT_AUTH, request));
    }

    @Test
    public void testGetFileInfo() throws B2Exception {
        final B2GetFileInfoRequest request = B2GetFileInfoRequest
                .builder(fileId(1))
                .build();
        webifier.getFileInfo(ACCOUNT_AUTH, request);

        webApiClient.check("postJsonReturnJson.\n" +
                "url:\n" +
                "    apiUrl1/b2api/v2/b2_get_file_info\n" +
                "headers:\n" +
                "    Authorization: accountToken1\n" +
                "    User-Agent: SecretAgentMan/3.19.28\n" +
                "    X-Bz-Test-Mode: force_cap_exceeded\n" +
                "request:\n" +
                "    {\n" +
                "      \"fileId\": \"4_zBlah_0000001\"\n" +
                "    }\n" +
                "responseClass:\n" +
                "    B2FileVersion\n"
        );

        checkRequestCategory(OTHER, w -> w.getFileInfo(ACCOUNT_AUTH, request));
    }

    @Test
    public void testGetFileInfoByName() throws B2Exception {
        final B2GetFileInfoByNameRequest request = B2GetFileInfoByNameRequest
                .builder(bucketName(1), fileName(1))
                .build();

        B2FileVersion version = webifier.getFileInfoByName(ACCOUNT_AUTH, request);

        Map<String, String> expectedFileInfo = new HashMap<>();
        expectedFileInfo.put("Color-with.special_chars`~!#$%^|\\'*&+", "gr\u00fcn");
        expectedFileInfo.put("src_last_modified_millis", "1");

        assertEquals(fileId(1), version.getFileId());
        assertEquals(fileName(1), version.getFileName());
        assertEquals(1L, version.getContentLength());
        assertEquals(1L, version.getUploadTimestamp());
        assertEquals(expectedFileInfo, version.getFileInfo());
        assertEquals(1L, Long.parseLong(version.getFileInfo().get("src_last_modified_millis")));

        webApiClient.check("head.\n" +
                "url:\n" +
                "    downloadUrl1/file/bucketName1/files/%E8%87%AA%E7%94%B1/0001\n" +
                "headers:\n" +
                "    Authorization: accountToken1\n" +
                "    User-Agent: SecretAgentMan/3.19.28\n" +
                "    X-Bz-Test-Mode: force_cap_exceeded\n");
        checkRequestCategory(OTHER, w -> w.getFileInfoByName(ACCOUNT_AUTH, request));
    }

    @Test
    public void testGetFileInfoByNameWithSseC() throws B2Exception {
        final B2GetFileInfoByNameRequest request = B2GetFileInfoByNameRequest
            .builder(bucketName(1), fileName(1))
            .setServerSideEncryption(B2FileSseForRequest.createSseCAes256("customerKey", "customerKeyMd5"))
            .build();

        final B2FileVersion version = webifier.getFileInfoByName(ACCOUNT_AUTH, request);

        final Map<String, String> expectedFileInfo = new HashMap<>();
        expectedFileInfo.put("Color-with.special_chars`~!#$%^|\\'*&+", "gr\u00fcn");
        expectedFileInfo.put("src_last_modified_millis", "1");

        assertEquals(fileId(1), version.getFileId());
        assertEquals(fileName(1), version.getFileName());
        assertEquals(1L, version.getContentLength());
        assertEquals(1L, version.getUploadTimestamp());
        assertEquals(expectedFileInfo, version.getFileInfo());
        assertEquals(1L, Long.parseLong(version.getFileInfo().get("src_last_modified_millis")));

        webApiClient.check("head.\n" +
            "url:\n" +
            "    downloadUrl1/file/bucketName1/files/%E8%87%AA%E7%94%B1/0001\n" +
            "headers:\n" +
            "    Authorization: accountToken1\n" +
            "    User-Agent: SecretAgentMan/3.19.28\n" +
            "    X-Bz-Server-Side-Encryption-Customer-Algorithm: AES256\n" +
            "    X-Bz-Server-Side-Encryption-Customer-Key: customerKey\n" +
            "    X-Bz-Server-Side-Encryption-Customer-Key-Md5: customerKeyMd5\n" +
            "    X-Bz-Test-Mode: force_cap_exceeded\n");
        checkRequestCategory(OTHER, w -> w.getFileInfoByName(ACCOUNT_AUTH, request));
    }

    @Test
    public void testHideFile() throws B2Exception {
        final B2HideFileRequest request = B2HideFileRequest
                .builder(bucketId(1), fileName(1))
                .build();
        webifier.hideFile(ACCOUNT_AUTH, request);

        webApiClient.check("postJsonReturnJson.\n" +
                "url:\n" +
                "    apiUrl1/b2api/v2/b2_hide_file\n" +
                "headers:\n" +
                "    Authorization: accountToken1\n" +
                "    User-Agent: SecretAgentMan/3.19.28\n" +
                "    X-Bz-Test-Mode: force_cap_exceeded\n" +
                "request:\n" +
                "    {\n" +
                "      \"bucketId\": \"bucket1\",\n" +
                "      \"fileName\": \"files/\u81ea\u7531/0001\"\n" +
                "    }\n" +
                "responseClass:\n" +
                "    B2FileVersion\n"
        );

        checkRequestCategory(OTHER, w -> w.hideFile(ACCOUNT_AUTH, request));
    }

    @Test
    public void testUpdateBucket() throws B2Exception {
        final B2UpdateBucketRequest request = B2UpdateBucketRequest
                .builder(makeBucket(1))
                .setCorsRules(Collections.singletonList(B2TestHelpers.makeCorsRule()))
                .build();
        webifier.updateBucket(ACCOUNT_AUTH, request);

        webApiClient.check("postJsonReturnJson.\n" +
                "url:\n" +
                "    apiUrl1/b2api/v2/b2_update_bucket\n" +
                "headers:\n" +
                "    Authorization: accountToken1\n" +
                "    User-Agent: SecretAgentMan/3.19.28\n" +
                "    X-Bz-Test-Mode: force_cap_exceeded\n" +
                "request:\n" +
                "    {\n" +
                "      \"accountId\": \"accountId1\",\n" +
                "      \"bucketId\": \"bucket1\",\n" +
                "      \"bucketInfo\": null,\n" +
                "      \"bucketType\": null,\n" +
                "      \"corsRules\": [\n" +
                "        {\n" +
                "          \"allowedHeaders\": null,\n" +
                "          \"allowedOperations\": [\n" +
                "            \"b2_download_file_by_id\"\n" +
                "          ],\n" +
                "          \"allowedOrigins\": [\n" +
                "            \"https://something.com\"\n" +
                "          ],\n" +
                "          \"corsRuleName\": \"rule-name\",\n" +
                "          \"exposeHeaders\": null,\n" +
                "          \"maxAgeSeconds\": 0\n" +
                "        }\n" +
                "      ],\n" +
                "      \"defaultRetention\": null,\n" +
                "      \"defaultServerSideEncryption\": null,\n" +
                "      \"ifRevisionIs\": 1,\n" +
                "      \"lifecycleRules\": null,\n" +
                "      \"replicationConfiguration\": null\n" +
                "    }\n" +
                "responseClass:\n" +
                "    B2Bucket\n"
        );

        checkRequestCategory(OTHER, w -> w.updateBucket(ACCOUNT_AUTH, request));
    }

    @Test
    public void testDeleteBucket() throws B2Exception {
        final B2DeleteBucketRequestReal requestReal =
                new B2DeleteBucketRequestReal(ACCOUNT_ID, bucketId(1));
        webifier.deleteBucket(ACCOUNT_AUTH, requestReal);

        webApiClient.check("postJsonReturnJson.\n" +
                "url:\n" +
                "    apiUrl1/b2api/v2/b2_delete_bucket\n" +
                "headers:\n" +
                "    Authorization: accountToken1\n" +
                "    User-Agent: SecretAgentMan/3.19.28\n" +
                "    X-Bz-Test-Mode: force_cap_exceeded\n" +
                "request:\n" +
                "    {\n" +
                "      \"accountId\": \"1\",\n" +
                "      \"bucketId\": \"bucket1\"\n" +
                "    }\n" +
                "responseClass:\n" +
                "    B2Bucket\n"
        );

        checkRequestCategory(OTHER, w -> w.deleteBucket(ACCOUNT_AUTH, requestReal));
    }

    @Test
    public void testDownloadById() throws B2Exception {
        final String expectedUrl = "downloadUrl1/b2api/v2/b2_download_file_by_id?fileId=4_zBlah_0000001";
        final B2DownloadByIdRequest request = B2DownloadByIdRequest
                .builder(fileId(1))
                .build();
        webifier.downloadById(ACCOUNT_AUTH, request, noopContentHandler);

        webApiClient.check("getContent.\n" +
                "url:\n" +
                "    " + expectedUrl + "\n" +
                "headers:\n" +
                "    Authorization: accountToken1\n" +
                "    User-Agent: SecretAgentMan/3.19.28\n" +
                "    X-Bz-Test-Mode: force_cap_exceeded\n"
        );

        assertEquals(expectedUrl, webifier.getDownloadByIdUrl(ACCOUNT_AUTH, request));

        checkRequestCategory(OTHER, w -> w.downloadById(ACCOUNT_AUTH, request, noopContentHandler));
    }

    @Test
    public void testDownloadByIdWithSseC() throws B2Exception {
        final String expectedUrl = "downloadUrl1/b2api/v2/b2_download_file_by_id?fileId=4_zBlah_0000001";
        final B2DownloadByIdRequest request = B2DownloadByIdRequest
                .builder(fileId(1))
                .setServerSideEncryption(B2FileSseForRequest.createSseCAes256("customerKey", "customerKeyMd5"))
                .build();
        webifier.downloadById(ACCOUNT_AUTH, request, noopContentHandler);

        webApiClient.check("getContent.\n" +
                "url:\n" +
                "    " + expectedUrl + "\n" +
                "headers:\n" +
                "    Authorization: accountToken1\n" +
                "    User-Agent: SecretAgentMan/3.19.28\n" +
                "    X-Bz-Server-Side-Encryption-Customer-Algorithm: AES256\n" +
                "    X-Bz-Server-Side-Encryption-Customer-Key: customerKey\n" +
                "    X-Bz-Server-Side-Encryption-Customer-Key-Md5: customerKeyMd5\n" +
                "    X-Bz-Test-Mode: force_cap_exceeded\n"
        );

        assertEquals(expectedUrl, webifier.getDownloadByIdUrl(ACCOUNT_AUTH, request));

        checkRequestCategory(OTHER, w -> w.downloadById(ACCOUNT_AUTH, request, noopContentHandler));
    }

    @Test
    public void testDownloadByIdWithRange() throws B2Exception {
        final B2DownloadByIdRequest request = B2DownloadByIdRequest
                .builder(fileId(1))
                .setRange(B2ByteRange.between(100, 200))
                .build();
        webifier.downloadById(ACCOUNT_AUTH, request, noopContentHandler);

        webApiClient.check("getContent.\n" +
                "url:\n" +
                "    downloadUrl1/b2api/v2/b2_download_file_by_id?fileId=4_zBlah_0000001\n" +
                "headers:\n" +
                "    Authorization: accountToken1\n" +
                "    Range: bytes=100-200\n" +
                "    User-Agent: SecretAgentMan/3.19.28\n" +
                "    X-Bz-Test-Mode: force_cap_exceeded\n"
        );

        checkRequestCategory(OTHER, w -> w.downloadById(ACCOUNT_AUTH, request, noopContentHandler));
    }
    @Test
    public void testDownloadByIdWithB2ContentDisposition() throws B2Exception {
        final B2DownloadByIdRequest request = B2DownloadByIdRequest
                .builder(fileId(1))
                .setB2ContentDisposition("attachment; filename=\"surprise.txt\"")
                .build();
        webifier.downloadById(ACCOUNT_AUTH, request, noopContentHandler);

        webApiClient.check("getContent.\n" +
                "url:\n" +
                "    downloadUrl1/b2api/v2/b2_download_file_by_id?fileId=4_zBlah_0000001&b2ContentDisposition=attachment%3B+filename%3D%22surprise.txt%22\n" +
                "headers:\n" +
                "    Authorization: accountToken1\n" +
                "    User-Agent: SecretAgentMan/3.19.28\n" +
                "    X-Bz-Test-Mode: force_cap_exceeded\n"
        );

        checkRequestCategory(OTHER, w -> w.downloadById(ACCOUNT_AUTH, request, noopContentHandler));
    }

    @Test
    public void testDownloadByIdWithAllOverrides() throws B2Exception {
        final B2DownloadByIdRequest request = B2DownloadByIdRequest
                .builder(fileId(1))
                .setB2ContentDisposition("attachment; filename=\"surprise.txt\"")
                .setB2ContentLanguage("en-us")
                .setB2Expires("Tue, 01 Jan 2030 01:00:00 GMT")
                .setB2CacheControl("max-age=100")
                .setB2ContentEncoding("gzip")
                .setB2ContentType("text/plain")
                .build();
        webifier.downloadById(ACCOUNT_AUTH, request, noopContentHandler);

        webApiClient.check("getContent.\n" +
                "url:\n" +
                "    downloadUrl1/b2api/v2/b2_download_file_by_id?fileId=4_zBlah_0000001&b2ContentDisposition=attachment%3B+filename%3D%22surprise.txt%22&b2ContentLanguage=en-us&b2Expires=Tue%2C+01+Jan+2030+01%3A00%3A00+GMT&b2CacheControl=max-age%3D100&b2ContentEncoding=gzip&b2ContentType=text/plain\n" +
                "headers:\n" +
                "    Authorization: accountToken1\n" +
                "    User-Agent: SecretAgentMan/3.19.28\n" +
                "    X-Bz-Test-Mode: force_cap_exceeded\n"
        );

        checkRequestCategory(OTHER, w -> w.downloadById(ACCOUNT_AUTH, request, noopContentHandler));
    }

    @Test
    public void testDownloadByName() throws B2Exception {
        final String expectedUrl = "downloadUrl1/file/bucketName1/files/%E8%87%AA%E7%94%B1/0001";
        final B2DownloadByNameRequest request = B2DownloadByNameRequest
                .builder(bucketName(1), fileName(1))
                .build();
        webifier.downloadByName(ACCOUNT_AUTH, request, noopContentHandler);

        webApiClient.check("getContent.\n" +
                "url:\n" +
                "    " + expectedUrl + "\n" +
                "headers:\n" +
                "    Authorization: accountToken1\n" +
                "    User-Agent: SecretAgentMan/3.19.28\n" +
                "    X-Bz-Test-Mode: force_cap_exceeded\n"
        );
        assertEquals(expectedUrl, webifier.getDownloadByNameUrl(ACCOUNT_AUTH, request));

        checkRequestCategory(OTHER, w -> w.downloadByName(ACCOUNT_AUTH, request, noopContentHandler));
    }

    @Test
    public void testDownloadByNameWithSseC() throws B2Exception {
        final String expectedUrl = "downloadUrl1/file/bucketName1/files/%E8%87%AA%E7%94%B1/0001";
        final B2DownloadByNameRequest request = B2DownloadByNameRequest
                .builder(bucketName(1), fileName(1))
                .setServerSideEncryption(B2FileSseForRequest.createSseCAes256("customerKey", "customerKeyMd5"))
                .build();
        webifier.downloadByName(ACCOUNT_AUTH, request, noopContentHandler);

        webApiClient.check("getContent.\n" +
                "url:\n" +
                "    " + expectedUrl + "\n" +
                "headers:\n" +
                "    Authorization: accountToken1\n" +
                "    User-Agent: SecretAgentMan/3.19.28\n" +
                "    X-Bz-Server-Side-Encryption-Customer-Algorithm: AES256\n" +
                "    X-Bz-Server-Side-Encryption-Customer-Key: customerKey\n" +
                "    X-Bz-Server-Side-Encryption-Customer-Key-Md5: customerKeyMd5\n" +
                "    X-Bz-Test-Mode: force_cap_exceeded\n"
        );
        assertEquals(expectedUrl, webifier.getDownloadByNameUrl(ACCOUNT_AUTH, request));

        checkRequestCategory(OTHER, w -> w.downloadByName(ACCOUNT_AUTH, request, noopContentHandler));
    }

    @Test
    public void testDownloadByNamePercentEncoded() throws B2Exception {
        final B2DownloadByNameRequest request = B2DownloadByNameRequest
                .builder(bucketName(1), "\u81ea\u7531")
                .build();
        webifier.downloadByName(ACCOUNT_AUTH, request, noopContentHandler);

        webApiClient.check("getContent.\n" +
                "url:\n" +
                "    downloadUrl1/file/bucketName1/%E8%87%AA%E7%94%B1\n" +
                "headers:\n" +
                "    Authorization: accountToken1\n" +
                "    User-Agent: SecretAgentMan/3.19.28\n" +
                "    X-Bz-Test-Mode: force_cap_exceeded\n"
        );

        checkRequestCategory(OTHER, w -> w.downloadByName(ACCOUNT_AUTH, request, noopContentHandler));
    }

    @Test
    public void testDownloadByNameWithRange() throws B2Exception {
        final B2DownloadByNameRequest request = B2DownloadByNameRequest
                .builder(bucketName(1), fileName(1))
                .setRange(B2ByteRange.startAt(200))
                .build();
        webifier.downloadByName(ACCOUNT_AUTH, request, noopContentHandler);

        webApiClient.check("getContent.\n" +
                "url:\n" +
                "    downloadUrl1/file/bucketName1/files/%E8%87%AA%E7%94%B1/0001\n" +
                "headers:\n" +
                "    Authorization: accountToken1\n" +
                "    Range: bytes=200-\n" +
                "    User-Agent: SecretAgentMan/3.19.28\n" +
                "    X-Bz-Test-Mode: force_cap_exceeded\n"
        );

        checkRequestCategory(OTHER, w -> w.downloadByName(ACCOUNT_AUTH, request, noopContentHandler));
    }

    @Test
    public void testDownloadByNameWithB2ContentDisposition() throws B2Exception {
        final B2DownloadByNameRequest request = B2DownloadByNameRequest
                .builder(bucketName(1), fileName(1))
                .setB2ContentDisposition("attachment; filename=\"with space.txt\"")
                .build();
        webifier.downloadByName(ACCOUNT_AUTH, request, noopContentHandler);

        webApiClient.check("getContent.\n" +
                "url:\n" +
                "    downloadUrl1/file/bucketName1/files/%E8%87%AA%E7%94%B1/0001?b2ContentDisposition=attachment%3B+filename%3D%22with+space.txt%22\n" +
                "headers:\n" +
                "    Authorization: accountToken1\n" +
                "    User-Agent: SecretAgentMan/3.19.28\n" +
                "    X-Bz-Test-Mode: force_cap_exceeded\n"
        );

        checkRequestCategory(OTHER, w -> w.downloadByName(ACCOUNT_AUTH, request, noopContentHandler));
    }

    @Test
    public void testDownloadByNameWithAllOverrides() throws B2Exception {
        final B2DownloadByNameRequest request = B2DownloadByNameRequest
                .builder(bucketName(1), fileName(1))
                .setB2ContentDisposition("attachment; filename=\"with space.txt\"")
                .setB2ContentLanguage("en-us")
                .setB2Expires("Tue, 01 Jan 2030 01:00:00 GMT")
                .setB2CacheControl("max-age=100")
                .setB2ContentEncoding("gzip")
                .setB2ContentType("text/plain")
                .build();
        webifier.downloadByName(ACCOUNT_AUTH, request, noopContentHandler);

        webApiClient.check("getContent.\n" +
                "url:\n" +
                "    downloadUrl1/file/bucketName1/files/%E8%87%AA%E7%94%B1/0001?b2ContentDisposition=attachment%3B+filename%3D%22with+space.txt%22&b2ContentLanguage=en-us&b2Expires=Tue%2C+01+Jan+2030+01%3A00%3A00+GMT&b2CacheControl=max-age%3D100&b2ContentEncoding=gzip&b2ContentType=text/plain\n" +
                "headers:\n" +
                "    Authorization: accountToken1\n" +
                "    User-Agent: SecretAgentMan/3.19.28\n" +
                "    X-Bz-Test-Mode: force_cap_exceeded\n"
        );

        checkRequestCategory(OTHER, w -> w.downloadByName(ACCOUNT_AUTH, request, noopContentHandler));
    }

    @Test
    public void testUploadPart() throws B2Exception {
        final B2UploadPartRequest request = B2UploadPartRequest
                .builder(6, contentSourceWithSha1)
                .build();
        final B2UploadPartUrlResponse partUrl = uploadPartUrlResponse(1,2);
        webifier.uploadPart(partUrl, request);

        webApiClient.check("postJsonReturnJson.\n" +
                "url:\n" +
                "    uploadUrl2\n" +
                "headers:\n" +
                "    Authorization: downloadToken2\n" +
                "    Expect: 100-continue\n" +
                "    User-Agent: SecretAgentMan/3.19.28\n" +
                "    X-Bz-Content-Sha1: " + SHA1 + "\n" +
                "    X-Bz-Part-Number: 6\n" +
                "    X-Bz-Test-Mode: force_cap_exceeded\n" +
                "inputStream:\n" +
                "    Hello, World!\n" +
                "contentLength:\n" +
                "    13\n" +
                "responseClass:\n" +
                "    B2Part\n"
        );


        checkRequestCategory(UPLOADING, w -> w.uploadPart(partUrl, request));
    }

    @Test
    public void testUploadPartRequest_withSseB2_throwsIllegalArgumentException() {
        thrown.expect(IllegalArgumentException.class);

        B2UploadPartRequest.builder(6, contentSourceWithSha1)
                .setServerSideEncryption(B2FileSseForRequest.createSseB2Aes256())
                .build();
    }

    @Test
    public void testUploadPart_withSseC() throws B2Exception {
        final B2UploadPartRequest request = B2UploadPartRequest
                .builder(6, contentSourceWithSha1)
                .setServerSideEncryption(B2FileSseForRequest.createSseCAes256("customer-key", "customer-key-md5"))
                .build();
        final B2UploadPartUrlResponse partUrl = uploadPartUrlResponse(1,2);
        webifier.uploadPart(partUrl, request);

        webApiClient.check("postJsonReturnJson.\n" +
                "url:\n" +
                "    uploadUrl2\n" +
                "headers:\n" +
                "    Authorization: downloadToken2\n" +
                "    Expect: 100-continue\n" +
                "    User-Agent: SecretAgentMan/3.19.28\n" +
                "    X-Bz-Content-Sha1: " + SHA1 + "\n" +
                "    X-Bz-Part-Number: 6\n" +
                "    X-Bz-Server-Side-Encryption-Customer-Algorithm: AES256\n" +
                "    X-Bz-Server-Side-Encryption-Customer-Key: customer-key\n" +
                "    X-Bz-Server-Side-Encryption-Customer-Key-Md5: customer-key-md5\n" +
                "    X-Bz-Test-Mode: force_cap_exceeded\n" +
                "inputStream:\n" +
                "    Hello, World!\n" +
                "contentLength:\n" +
                "    13\n" +
                "responseClass:\n" +
                "    B2Part\n"
        );


        checkRequestCategory(UPLOADING, w -> w.uploadPart(partUrl, request));
    }

    @Test
    public void testUploadPartWithNoSha1InContentSource() throws B2Exception {
        final B2UploadPartRequest request = B2UploadPartRequest
                .builder(6, contentSourceWithoutOptionalData)
                .build();
        final B2UploadPartUrlResponse partUrl = uploadPartUrlResponse(1,2);
        webifier.uploadPart(partUrl, request);

        webApiClient.check("postJsonReturnJson.\n" +
                "url:\n" +
                "    uploadUrl2\n" +
                "headers:\n" +
                "    Authorization: downloadToken2\n" +
                "    Expect: 100-continue\n" +
                "    User-Agent: SecretAgentMan/3.19.28\n" +
                "    X-Bz-Content-Sha1: hex_digits_at_end\n" +
                "    X-Bz-Part-Number: 6\n" +
                "    X-Bz-Test-Mode: force_cap_exceeded\n" +
                "inputStream:\n" +
                "    Hello, World!" + SHA1 + "\n" +
                "contentLength:\n" +
                "    " + (CONTENTS_BYTES.length + SHA1.length()) + "\n" +
                "responseClass:\n" +
                "    B2Part\n"
        );


        checkRequestCategory(UPLOADING, w -> w.uploadPart(partUrl, request));
    }

    @Test
    public void testCopyPart() throws B2Exception {
        final String sourceKey = "iLNDwUxG7jW5Dk8K4L5MmtRlFYGtHCPWWYkzpFZ6cb8=";
        final String destinationKey = "hIoRG+b7TqbVdXxBb66XkD2F1xnquDx1JLjP0vcryIM=";

        final B2CopyPartRequest request = B2CopyPartRequest
                .builder(3, fileId(1), fileId(2))
                .setSourceServerSideEncryption(B2FileSseForRequest.createSseCAes256(sourceKey))
                .setDestinationServerSideEncryption(B2FileSseForRequest.createSseCAes256(destinationKey))
                .build();
        webifier.copyPart(ACCOUNT_AUTH, request);

        webApiClient.check("postJsonReturnJson.\n" +
                "url:\n" +
                "    apiUrl1/b2api/v2/b2_copy_part\n" +
                "headers:\n" +
                "    Authorization: accountToken1\n" +
                "    User-Agent: SecretAgentMan/3.19.28\n" +
                "    X-Bz-Test-Mode: force_cap_exceeded\n" +
                "request:\n" +
                "    {\n" +
                "      \"destinationServerSideEncryption\": {\n" +
                "        \"algorithm\": \"AES256\",\n" +
                "        \"customerKey\": \"hIoRG+b7TqbVdXxBb66XkD2F1xnquDx1JLjP0vcryIM=\",\n" +
                "        \"customerKeyMd5\": \"F13Y6Zu3HEuFE+e+53QWzA==\",\n" +
                "        \"mode\": \"SSE-C\"\n" +
                "      },\n" +
                "      \"largeFileId\": \"4_zBlah_0000002\",\n" +
                "      \"partNumber\": 3,\n" +
                "      \"range\": null,\n" +
                "      \"sourceFileId\": \"4_zBlah_0000001\",\n" +
                "      \"sourceServerSideEncryption\": {\n" +
                "        \"algorithm\": \"AES256\",\n" +
                "        \"customerKey\": \"iLNDwUxG7jW5Dk8K4L5MmtRlFYGtHCPWWYkzpFZ6cb8=\",\n" +
                "        \"customerKeyMd5\": \"uNesypp/GNphraVA9wPL5A==\",\n" +
                "        \"mode\": \"SSE-C\"\n" +
                "      }\n" +
                "    }\n" +
                "responseClass:\n" +
                "    B2Part\n"
        );
    }

    @Test
    public void testUploadFile() throws B2Exception {
        final B2UploadListener listener = mock(B2UploadListener.class);

        final B2UploadFileRequest request = B2UploadFileRequest
                .builder(bucketId(1), fileName(1), B2ContentTypes.B2_AUTO, contentSourceWithSha1)
                .setCustomField("color", "gr\u00fcn")
                .setCustomField("number", "six")
                .setListener(listener)
                .build();
        final B2UploadUrlResponse uploadUrl = uploadUrlResponse(bucketId(1), 1);
        webifier.uploadFile(uploadUrl, request);

        webApiClient.check("postJsonReturnJson.\n" +
                "url:\n" +
                "    uploadUrl1\n" +
                "headers:\n" +
                "    Authorization: downloadToken1\n" +
                "    Content-Type: b2/x-auto\n" +
                "    Expect: 100-continue\n" +
                "    User-Agent: SecretAgentMan/3.19.28\n" +
                "    X-Bz-Content-Sha1: 0a0a9f2a6772942557ab5355d76af442f8f65e01\n" +
                "    X-Bz-File-Name: files/%E8%87%AA%E7%94%B1/0001\n" +
                "    X-Bz-Info-color: gr%C3%BCn\n" +
                "    X-Bz-Info-number: six\n" +
                "    X-Bz-Info-src_last_modified_millis: 1234567\n" +
                "    X-Bz-Test-Mode: force_cap_exceeded\n" +
                "inputStream:\n" +
                "    Hello, World!\n" +
                "contentLength:\n" +
                "    13\n" +
                "responseClass:\n" +
                "    B2FileVersion\n"
        );

        verify(listener, times(1)).progress(eq(new B2UploadProgress(0, 1, 0, 13,  0, B2UploadState.WAITING_TO_START)));
        verify(listener, times(1)).progress(eq(new B2UploadProgress(0, 1, 0, 13,  0, B2UploadState.STARTING)));
        // we get two UPLOADING events with bytesSoFar=13:
        //   the first happens when our first read succeeds.
        //   the second happens when we hit eof.
        // that's fine.  (in the real world there will probably be more than one UPLOADING event and the final one might be suppressed by the B2ByteProgressFilteringListener
        //                which is added above this code, in the B2StorageClientImpl, so it's important that send one for reachedEof.  and i don't want to complicate things
        //                enough to filter this case out.)
        verify(listener, times(2)).progress(eq(new B2UploadProgress(0, 1, 0, 13, 13, B2UploadState.UPLOADING)));
        verify(listener, times(1)).progress(eq(new B2UploadProgress(0, 1, 0, 13, 13, B2UploadState.SUCCEEDED)));

        reset(listener); // to clear the counts.

        checkRequestCategory(UPLOADING, w -> w.uploadFile(uploadUrl, request));
        verify(listener, times(1)).progress(eq(new B2UploadProgress(0, 1, 0, 13,  0, B2UploadState.WAITING_TO_START)));
        verify(listener, times(1)).progress(eq(new B2UploadProgress(0, 1, 0, 13,  0, B2UploadState.STARTING)));
        verify(listener, times(1)).progress(eq(new B2UploadProgress(0, 1, 0, 13,  0, B2UploadState.FAILED)));
    }

    @Test
    public void testUploadFileWithSseC() throws B2Exception {
        final B2UploadListener listener = mock(B2UploadListener.class);

        final B2UploadFileRequest request = B2UploadFileRequest
                .builder(bucketId(1), fileName(1), B2ContentTypes.B2_AUTO, contentSourceWithSha1)
                .setServerSideEncryption(B2FileSseForRequest.createSseCAes256("customerKey", "customerKeyMd5"))
                .setListener(listener)
                .build();
        final B2UploadUrlResponse uploadUrl = uploadUrlResponse(bucketId(1), 1);
        webifier.uploadFile(uploadUrl, request);

        webApiClient.check("postJsonReturnJson.\n" +
                "url:\n" +
                "    uploadUrl1\n" +
                "headers:\n" +
                "    Authorization: downloadToken1\n" +
                "    Content-Type: b2/x-auto\n" +
                "    Expect: 100-continue\n" +
                "    User-Agent: SecretAgentMan/3.19.28\n" +
                "    X-Bz-Content-Sha1: 0a0a9f2a6772942557ab5355d76af442f8f65e01\n" +
                "    X-Bz-File-Name: files/%E8%87%AA%E7%94%B1/0001\n" +
                "    X-Bz-Info-src_last_modified_millis: 1234567\n" +
                "    X-Bz-Server-Side-Encryption-Customer-Algorithm: AES256\n" +
                "    X-Bz-Server-Side-Encryption-Customer-Key: customerKey\n" +
                "    X-Bz-Server-Side-Encryption-Customer-Key-Md5: customerKeyMd5\n" +
                "    X-Bz-Test-Mode: force_cap_exceeded\n" +
                "inputStream:\n" +
                "    Hello, World!\n" +
                "contentLength:\n" +
                "    13\n" +
                "responseClass:\n" +
                "    B2FileVersion\n"
        );
    }

    @Test
    public void testUploadFileWithFileLockInfo() throws B2Exception {
        final B2UploadListener listener = mock(B2UploadListener.class);

        final B2UploadFileRequest request = B2UploadFileRequest
                .builder(bucketId(1), fileName(1), B2ContentTypes.B2_AUTO, contentSourceWithSha1)
                .setLegalHold(B2LegalHold.ON)
                .setFileRetention(new B2FileRetention( "governance", 9876543210L))
                .setListener(listener)
                .build();
        final B2UploadUrlResponse uploadUrl = uploadUrlResponse(bucketId(1), 1);
        webifier.uploadFile(uploadUrl, request);

        webApiClient.check("postJsonReturnJson.\n" +
                "url:\n" +
                "    uploadUrl1\n" +
                "headers:\n" +
                "    Authorization: downloadToken1\n" +
                "    Content-Type: b2/x-auto\n" +
                "    Expect: 100-continue\n" +
                "    User-Agent: SecretAgentMan/3.19.28\n" +
                "    X-Bz-Content-Sha1: 0a0a9f2a6772942557ab5355d76af442f8f65e01\n" +
                "    X-Bz-File-Legal-Hold: on\n" +
                "    X-Bz-File-Name: files/%E8%87%AA%E7%94%B1/0001\n" +
                "    X-Bz-File-Retention-Mode: governance\n" +
                "    X-Bz-File-Retention-Retain-Until-Timestamp: 9876543210\n" +
                "    X-Bz-Info-src_last_modified_millis: 1234567\n" +
                "    X-Bz-Test-Mode: force_cap_exceeded\n" +
                "inputStream:\n" +
                "    Hello, World!\n" +
                "contentLength:\n" +
                "    13\n" +
                "responseClass:\n" +
                "    B2FileVersion\n"
        );
    }

    @Test
    public void testUploadFileContainingEmptyFileLockRetention() throws B2Exception {
        final B2UploadListener listener = mock(B2UploadListener.class);

        final B2UploadFileRequest request = B2UploadFileRequest
                .builder(bucketId(1), fileName(1), B2ContentTypes.B2_AUTO, contentSourceWithSha1)
                .setFileRetention(new B2FileRetention(null, null))
                .setListener(listener)
                .build();
        final B2UploadUrlResponse uploadUrl = uploadUrlResponse(bucketId(1), 1);
        webifier.uploadFile(uploadUrl, request);

        webApiClient.check("postJsonReturnJson.\n" +
                "url:\n" +
                "    uploadUrl1\n" +
                "headers:\n" +
                "    Authorization: downloadToken1\n" +
                "    Content-Type: b2/x-auto\n" +
                "    Expect: 100-continue\n" +
                "    User-Agent: SecretAgentMan/3.19.28\n" +
                "    X-Bz-Content-Sha1: 0a0a9f2a6772942557ab5355d76af442f8f65e01\n" +
                "    X-Bz-File-Name: files/%E8%87%AA%E7%94%B1/0001\n" +
                "    X-Bz-Info-src_last_modified_millis: 1234567\n" +
                "    X-Bz-Test-Mode: force_cap_exceeded\n" +
                "inputStream:\n" +
                "    Hello, World!\n" +
                "contentLength:\n" +
                "    13\n" +
                "responseClass:\n" +
                "    B2FileVersion\n"
        );
    }

    @Test
    public void testUploadFileWithInvalidLegalHoldValue() {
        final B2UploadListener listener = mock(B2UploadListener.class);

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Invalid legalHold value. Valid values: on, off");

        B2UploadFileRequest.builder(
                bucketId(1), fileName(1), B2ContentTypes.B2_AUTO, contentSourceWithSha1)
                .setLegalHold("hold")
                .setListener(listener)
                .build();
    }

        @Test
    public void testUploadFileWithNoSha1InContentSource() throws B2Exception {
        final B2UploadFileRequest request = B2UploadFileRequest
                .builder(bucketId(1), fileName(1), B2ContentTypes.B2_AUTO, contentSourceWithoutOptionalData)
                .setCustomField("color", "blue")
                .setCustomField("number", "six")
                .build();
        final B2UploadUrlResponse uploadUrl = uploadUrlResponse(bucketId(1), 1);
        webifier.uploadFile(uploadUrl, request);

        webApiClient.check("postJsonReturnJson.\n" +
                "url:\n" +
                "    uploadUrl1\n" +
                "headers:\n" +
                "    Authorization: downloadToken1\n" +
                "    Content-Type: b2/x-auto\n" +
                "    Expect: 100-continue\n" +
                "    User-Agent: SecretAgentMan/3.19.28\n" +
                "    X-Bz-Content-Sha1: hex_digits_at_end\n" +
                "    X-Bz-File-Name: files/%E8%87%AA%E7%94%B1/0001\n" +
                "    X-Bz-Info-color: blue\n" +
                "    X-Bz-Info-number: six\n" +
                "    X-Bz-Test-Mode: force_cap_exceeded\n" +
                "inputStream:\n" +
                "    Hello, World!0a0a9f2a6772942557ab5355d76af442f8f65e01\n" +
                "contentLength:\n" +
                "    53\n" +
                "responseClass:\n" +
                "    B2FileVersion\n"
        );

        checkRequestCategory(UPLOADING, w -> w.uploadFile(uploadUrl, request));
    }

    @Test
    public void testUploadFileWithExceptionGettingSrcLastModifiedMillis() throws B2Exception {

        final B2UploadFileRequest request = B2UploadFileRequest
                .builder(bucketId(1), fileName(1), B2ContentTypes.B2_AUTO, contentSourceWithExceptionInGetSrcLastModifiedMillis)
                .build();
        final B2UploadUrlResponse uploadUrl = uploadUrlResponse(bucketId(1), 1);

        thrown.expect(B2Exception.class);
        thrown.expectMessage("failed to get lastModified from source: java.io.IOException: testing!");
        webifier.uploadFile(uploadUrl, request);
    }

    @Test
    public void testCopyFile() throws B2Exception {
        final String sourceKey = "hIoRG+b7TqbVdXxBb66XkD2F1xnquDx1JLjP0vcryIM=";
        final String destinationKey = "iLNDwUxG7jW5Dk8K4L5MmtRlFYGtHCPWWYkzpFZ6cb8=";

        final B2CopyFileRequest request = B2CopyFileRequest
                .builder(fileId(1), fileName(2))
                .setDestinationBucketId(bucketId(3))
                .setContentType("b2/x-auto")
                .setMetadataDirective(B2CopyFileRequest.COPY_METADATA_DIRECTIVE)
                .setRange(B2ByteRange.between(10, 100))
                .setSourceServerSideEncryption(B2FileSseForRequest.createSseCAes256(sourceKey))
                .setDestinationServerSideEncryption(B2FileSseForRequest.createSseCAes256(destinationKey))
                .build();
        webifier.copyFile(ACCOUNT_AUTH, request);

        webApiClient.check("postJsonReturnJson.\n" +
                "url:\n" +
                "    apiUrl1/b2api/v2/b2_copy_file\n" +
                "headers:\n" +
                "    Authorization: accountToken1\n" +
                "    User-Agent: SecretAgentMan/3.19.28\n" +
                "    X-Bz-Test-Mode: force_cap_exceeded\n" +
                "request:\n" +
                "    {\n" +
                "      \"contentType\": \"b2/x-auto\",\n" +
                "      \"destinationBucketId\": \"bucket3\",\n" +
                "      \"destinationServerSideEncryption\": {\n" +
                "        \"algorithm\": \"AES256\",\n" +
                "        \"customerKey\": \"iLNDwUxG7jW5Dk8K4L5MmtRlFYGtHCPWWYkzpFZ6cb8=\",\n" +
                "        \"customerKeyMd5\": \"uNesypp/GNphraVA9wPL5A==\",\n" +
                "        \"mode\": \"SSE-C\"\n" +
                "      },\n" +
                "      \"fileInfo\": null,\n" +
                "      \"fileName\": \"files/\u81ea\u7531/0002\",\n" +
                "      \"metadataDirective\": \"COPY\",\n" +
                "      \"range\": \"bytes=10-100\",\n" +
                "      \"sourceFileId\": \"4_zBlah_0000001\",\n" +
                "      \"sourceServerSideEncryption\": {\n" +
                "        \"algorithm\": \"AES256\",\n" +
                "        \"customerKey\": \"hIoRG+b7TqbVdXxBb66XkD2F1xnquDx1JLjP0vcryIM=\",\n" +
                "        \"customerKeyMd5\": \"F13Y6Zu3HEuFE+e+53QWzA==\",\n" +
                "        \"mode\": \"SSE-C\"\n" +
                "      }\n" +
                "    }\n" +
                "responseClass:\n" +
                "    B2FileVersion\n"
        );

        checkRequestCategory(OTHER, w -> w.copyFile(ACCOUNT_AUTH, request));
    }

    @Test
    public void testCopyFileWithReplaceAndFileRetentionAndLegalHold() throws B2Exception {
        final long expirationInMillis = Instant.now().plus(10L, ChronoUnit.DAYS).toEpochMilli();

        final B2CopyFileRequest request = B2CopyFileRequest
                .builder(fileId(1), fileName(2))
                .setDestinationBucketId(bucketId(3))
                .setMetadataDirective(B2CopyFileRequest.REPLACE_METADATA_DIRECTIVE)
                .setFileRetention(
                        new B2FileRetention(
                                B2FileRetentionMode.COMPLIANCE,
                                expirationInMillis
                        )
                )
                .setLegalHold(B2LegalHold.ON)
                .build();
        webifier.copyFile(ACCOUNT_AUTH, request);

        webApiClient.check("postJsonReturnJson.\n" +
                "url:\n" +
                "    apiUrl1/b2api/v2/b2_copy_file\n" +
                "headers:\n" +
                "    Authorization: accountToken1\n" +
                "    User-Agent: SecretAgentMan/3.19.28\n" +
                "    X-Bz-Test-Mode: force_cap_exceeded\n" +
                "request:\n" +
                "    {\n" +
                "      \"contentType\": null,\n" +
                "      \"destinationBucketId\": \"bucket3\",\n" +
                "      \"destinationServerSideEncryption\": null,\n" +
                "      \"fileInfo\": null,\n" +
                "      \"fileName\": \"files/\u81ea\u7531/0002\",\n" +
                "      \"fileRetention\": {\n" +
                "        \"mode\": \"compliance\",\n" +
                "        \"retainUntilTimestamp\": " + expirationInMillis + "\n" +
                "      },\n" +
                "      \"legalHold\": \"on\",\n" +
                "      \"metadataDirective\": \"REPLACE\",\n" +
                "      \"range\": null,\n" +
                "      \"sourceFileId\": \"4_zBlah_0000001\",\n" +
                "      \"sourceServerSideEncryption\": null\n" +
                "    }\n" +
                "responseClass:\n" +
                "    B2FileVersion\n"
        );

        checkRequestCategory(OTHER, w -> w.copyFile(ACCOUNT_AUTH, request));
    }

    @Test
    public void testUpdateFileLegalHold() throws B2Exception {
        final B2UpdateFileLegalHoldRequest requestReal = B2UpdateFileLegalHoldRequest
                .builder(fileName(1), fileId(1), B2LegalHold.ON)
                .build();
        webifier.updateFileLegalHold(ACCOUNT_AUTH, requestReal);

        webApiClient.check("postJsonReturnJson.\n" +
                "url:\n" +
                "    apiUrl1/b2api/v2/b2_update_file_legal_hold\n" +
                "headers:\n" +
                "    Authorization: accountToken1\n" +
                "    User-Agent: SecretAgentMan/3.19.28\n" +
                "    X-Bz-Test-Mode: force_cap_exceeded\n" +
                "request:\n" +
                "    {\n" +
                "      \"fileId\": \"4_zBlah_0000001\",\n" +
                "      \"fileName\": \"files/\u81ea\u7531/0001\",\n" +
                "      \"legalHold\": \"on\"\n" +
                "    }\n" +
                "responseClass:\n" +
                "    B2UpdateFileLegalHoldResponse\n"
        );

        checkRequestCategory(OTHER, w -> w.updateFileLegalHold(ACCOUNT_AUTH, requestReal));
    }

    @Test
    public void testUpdateFileRetention() throws B2Exception {
        final B2FileRetention fileRetention = new B2FileRetention(B2FileRetentionMode.COMPLIANCE, 10000L);
        final B2UpdateFileRetentionRequest requestReal = B2UpdateFileRetentionRequest
                .builder(fileName(1), fileId(1), fileRetention)
                .build();
        webifier.updateFileRetention(ACCOUNT_AUTH, requestReal);

        webApiClient.check("postJsonReturnJson.\n" +
                "url:\n" +
                "    apiUrl1/b2api/v2/b2_update_file_retention\n" +
                "headers:\n" +
                "    Authorization: accountToken1\n" +
                "    User-Agent: SecretAgentMan/3.19.28\n" +
                "    X-Bz-Test-Mode: force_cap_exceeded\n" +
                "request:\n" +
                "    {\n" +
                "      \"bypassGovernance\": false,\n" +
                "      \"fileId\": \"4_zBlah_0000001\",\n" +
                "      \"fileName\": \"files/\u81ea\u7531/0001\",\n" +
                "      \"fileRetention\": {\n" +
                "        \"mode\": \"compliance\",\n" +
                "        \"retainUntilTimestamp\": 10000\n" +
                "      }\n" +
                "    }\n" +
                "responseClass:\n" +
                "    B2UpdateFileRetentionResponse\n"
        );

        checkRequestCategory(OTHER, w -> w.updateFileRetention(ACCOUNT_AUTH, requestReal));
    }

    @Test
    public void testTestModes() throws B2Exception {
        // test each possible testMode, including "none".
        checkTestMode(B2TestMode.EXPIRE_SOME_ACCOUNT_AUTHORIZATION_TOKENS, "expire_some_account_authorization_tokens");
        checkTestMode(B2TestMode.FAIL_SOME_UPLOADS, "fail_some_uploads");
        checkTestMode(B2TestMode.FORCE_CAP_EXCEEDED, "force_cap_exceeded");
        checkTestMode(null, "NOT USED!");
    }

    private void checkTestMode(B2TestMode testMode, String testModeName) throws B2Exception {
        // we need local instances of these objects to use custom testMode and only use the webApiClient once.
        final RecordingWebApiClient localWebApiClient = new RecordingWebApiClient();
        final B2StorageClientWebifier localWebifier = new B2StorageClientWebifierImpl(
                localWebApiClient,
                USER_AGENT,
                MASTER_URL,
                testMode
        );

        // I'm only checking on one API call.
        final B2ListBucketsRequest request = B2ListBucketsRequest
                .builder(ACCOUNT_ID)
                .build();
        localWebifier.listBuckets(ACCOUNT_AUTH, request);

        localWebApiClient.check("postJsonReturnJson.\n" +
                "url:\n" +
                "    apiUrl1/b2api/v2/b2_list_buckets\n" +
                "headers:\n" +
                "    Authorization: accountToken1\n" +
                "    User-Agent: SecretAgentMan/3.19.28\n" +
                ((testMode != null) ?
                        "    X-Bz-Test-Mode: " + testModeName + "\n" :
                        "") +
                "request:\n" +
                "    {\n" +
                "      \"accountId\": \"1\",\n" +
                "      \"bucketId\": null,\n" +
                "      \"bucketName\": null,\n" +
                "      \"bucketTypes\": null\n" +
                "    }\n" +
                "responseClass:\n" +
                "    B2ListBucketsResponse\n"
        );
    }

    private void checkRequestCategory(B2UnauthorizedException.RequestCategory expectedCategory,
                                      Requester requester) {
        final B2WebApiClient unauthWebApiClient = new AlwaysThrowsUnauthorizedWebClientApi() ;

        final B2StorageClientWebifierImpl unauthWebifier = new B2StorageClientWebifierImpl(
                unauthWebApiClient,
                USER_AGENT,
                MASTER_URL,
                B2TestMode.EXPIRE_SOME_ACCOUNT_AUTHORIZATION_TOKENS
        );

        boolean threw = false;
        try {
            requester.request(unauthWebifier);
        } catch (B2UnauthorizedException e) {
            threw = true;
            assertEquals(expectedCategory, e.getRequestCategory());
        } catch (B2Exception e) {
            fail("threw unexpected exception");
        }
        assertTrue(threw);
    }

    private static class ThrowsInGetSrcLastModifiedMillis implements B2ContentSource {
        private final B2ContentSource nested;
        ThrowsInGetSrcLastModifiedMillis(B2ContentSource nested) {
            this.nested = nested;
        }

        @Override
        public long getContentLength() throws IOException {
            return nested.getContentLength();
        }

        @Override
        public String getSha1OrNull() throws IOException {
            return nested.getSha1OrNull();
        }

        @Override
        public Long getSrcLastModifiedMillisOrNull() throws IOException {
            throw new IOException("testing!");
        }

        @Override
        public InputStream createInputStream() throws IOException, B2Exception {
            return nested.createInputStream();
        }
    }

    @Test
    public void testValidateFileInfoNameThrowsForIllegalChars() throws B2BadRequestException {
        final String nameWithIllegalChars = "my-header<>illegalChars()@";

        thrown.expect(B2BadRequestException.class);
        thrown.expectMessage("Illegal file info name: " + nameWithIllegalChars);

        webifier.validateFileInfoName(nameWithIllegalChars);
    }
}
