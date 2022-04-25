/*
 * Copyright 2021, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client;

import com.backblaze.b2.client.contentHandlers.B2ContentSink;
import com.backblaze.b2.client.contentSources.B2ContentSource;
import com.backblaze.b2.client.contentSources.B2ContentTypes;
import com.backblaze.b2.client.exceptions.B2Exception;
import com.backblaze.b2.client.structures.B2AccountAuthorization;
import com.backblaze.b2.client.structures.B2AuthorizationFilteredResponseField;
import com.backblaze.b2.client.structures.B2AuthorizeAccountRequest;
import com.backblaze.b2.client.structures.B2Bucket;
import com.backblaze.b2.client.structures.B2BucketFileLockConfiguration;
import com.backblaze.b2.client.structures.B2BucketReplicationConfiguration;
import com.backblaze.b2.client.structures.B2BucketServerSideEncryption;
import com.backblaze.b2.client.structures.B2BucketTypes;
import com.backblaze.b2.client.structures.B2CancelLargeFileRequest;
import com.backblaze.b2.client.structures.B2CancelLargeFileResponse;
import com.backblaze.b2.client.structures.B2CopyFileRequest;
import com.backblaze.b2.client.structures.B2CreateBucketRequest;
import com.backblaze.b2.client.structures.B2CreateBucketRequestReal;
import com.backblaze.b2.client.structures.B2DeleteBucketRequest;
import com.backblaze.b2.client.structures.B2DeleteBucketRequestReal;
import com.backblaze.b2.client.structures.B2DeleteFileVersionRequest;
import com.backblaze.b2.client.structures.B2DeleteFileVersionResponse;
import com.backblaze.b2.client.structures.B2DownloadAuthorization;
import com.backblaze.b2.client.structures.B2DownloadByIdRequest;
import com.backblaze.b2.client.structures.B2DownloadByNameRequest;
import com.backblaze.b2.client.structures.B2FileRetention;
import com.backblaze.b2.client.structures.B2FileRetentionMode;
import com.backblaze.b2.client.structures.B2FileVersion;
import com.backblaze.b2.client.structures.B2FinishLargeFileRequest;
import com.backblaze.b2.client.structures.B2GetDownloadAuthorizationRequest;
import com.backblaze.b2.client.structures.B2GetFileInfoByNameRequest;
import com.backblaze.b2.client.structures.B2GetFileInfoRequest;
import com.backblaze.b2.client.structures.B2GetUploadPartUrlRequest;
import com.backblaze.b2.client.structures.B2GetUploadUrlRequest;
import com.backblaze.b2.client.structures.B2HideFileRequest;
import com.backblaze.b2.client.structures.B2LegalHold;
import com.backblaze.b2.client.structures.B2LifecycleRule;
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
import com.backblaze.b2.client.structures.B2ReplicationRule;
import com.backblaze.b2.client.structures.B2StartLargeFileRequest;
import com.backblaze.b2.client.structures.B2UpdateBucketRequest;
import com.backblaze.b2.client.structures.B2UpdateFileLegalHoldRequest;
import com.backblaze.b2.client.structures.B2UpdateFileLegalHoldResponse;
import com.backblaze.b2.client.structures.B2UpdateFileRetentionRequest;
import com.backblaze.b2.client.structures.B2UpdateFileRetentionResponse;
import com.backblaze.b2.client.structures.B2UploadFileRequest;
import com.backblaze.b2.client.structures.B2UploadListener;
import com.backblaze.b2.client.structures.B2UploadPartRequest;
import com.backblaze.b2.client.structures.B2UploadPartUrlResponse;
import com.backblaze.b2.client.structures.B2UploadProgress;
import com.backblaze.b2.client.structures.B2UploadUrlResponse;
import com.backblaze.b2.util.B2BaseTest;
import com.backblaze.b2.util.B2ByteRange;
import com.backblaze.b2.util.B2Clock;
import com.backblaze.b2.util.B2Collections;
import com.backblaze.b2.util.B2ExecutorUtils;
import com.backblaze.b2.util.B2Preconditions;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.backblaze.b2.client.B2TestHelpers.bucketId;
import static com.backblaze.b2.client.B2TestHelpers.bucketName;
import static com.backblaze.b2.client.B2TestHelpers.fileId;
import static com.backblaze.b2.client.B2TestHelpers.fileName;
import static com.backblaze.b2.client.B2TestHelpers.makeMd5;
import static com.backblaze.b2.client.B2TestHelpers.makePart;
import static com.backblaze.b2.client.B2TestHelpers.makeSha1;
import static com.backblaze.b2.client.B2TestHelpers.makeVersion;
import static com.backblaze.b2.client.structures.B2UploadState.STARTING;
import static com.backblaze.b2.client.structures.B2UploadState.SUCCEEDED;
import static com.backblaze.b2.client.structures.B2UploadState.WAITING_TO_START;
import static com.backblaze.b2.util.B2Collections.listOf;
import static com.backblaze.b2.util.B2DateTimeUtil.parseDateTime;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 */
@SuppressWarnings("unchecked")
public class B2StorageClientImplTest extends B2BaseTest {
    private static final B2AccountAuthorization ACCOUNT_AUTH = B2TestHelpers.makeAuth(1);
    private static final String ACCOUNT_ID = ACCOUNT_AUTH.getAccountId();
    private static final String APPLICATION_KEY = "applicationKey";
    private static final String USER_AGENT = "B2StorageClientImplTest/0.0.1";
    private static final String BUCKET_NAME = "bucket1";
    private static final String BUCKET_TYPE = B2BucketTypes.ALL_PUBLIC;
    private static final String FILE_PREFIX = "files/";
    private static final String LARGE_FILE_ID = fileId(2);

    private final B2StorageClientWebifier webifier = mock(B2StorageClientWebifier.class);
    private final B2ClientConfig config = B2ClientConfig
            .builder(ACCOUNT_ID, APPLICATION_KEY, USER_AGENT)
            .build();
    private final B2Sleeper sleeper = mock(B2Sleeper.class);
    private final BackoffRetryerWithCounter retryer = new BackoffRetryerWithCounter(sleeper);
    private final B2StorageClientImpl client = new B2StorageClientImpl(webifier, config, B2DefaultRetryPolicy.supplier(), retryer);
    private final ExecutorService executor = Executors.newFixedThreadPool(1);

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @After
    public void tearDown() {
        B2ExecutorUtils.shutdownAndAwaitTermination(executor, 10, 10);
    }

    private static class BackoffRetryerWithCounter extends B2Retryer {
        private int callCount = 0;

        BackoffRetryerWithCounter(B2Sleeper sleeper) {
            super(sleeper);
        }

        @Override
        <T> T doRetry(String operation,
                      B2AccountAuthorizationCache accountAuthCache,
                      Callable<T> callable,
                      B2RetryPolicy retryPolicy) throws B2Exception {
            callCount++;
            return super.doRetry(operation, accountAuthCache, callable, retryPolicy);
        }

        @Override
        <T> T doRetry(String operation,
                      B2AccountAuthorizationCache accountAuthCache,
                      RetryableCallable<T> callable,
                      B2RetryPolicy retryPolicy) throws B2Exception {
            callCount++;
            return super.doRetry(operation, accountAuthCache, callable, retryPolicy);
        }

        void assertCallCountIs(int expectedCallCount) {
            assertEquals(expectedCallCount, callCount);
        }
    }

    @Before
    public void setup() throws B2Exception {
        {
            B2Clock.useSimulator(parseDateTime("2018-04-17 01:26:32"));
            final B2AuthorizeAccountRequest request = new B2AuthorizeAccountRequest(ACCOUNT_ID, APPLICATION_KEY);
            when(webifier.authorizeAccount(request)).thenReturn(ACCOUNT_AUTH);
        }
    }


    @Test
    public void testCreateBucket_convenience() throws B2Exception {
        final B2Bucket bucket = new B2Bucket(
                "accountId",
                bucketId(1),
                BUCKET_NAME,
                BUCKET_TYPE,
                null,
                null,
                null,
                Collections.emptySet(),
                null,
                null,
                null,
                1
        );
        when(webifier.createBucket(anyObject(), anyObject())).thenReturn(bucket);

        final B2Bucket response = client.createBucket(BUCKET_NAME, BUCKET_TYPE);
        assertEquals(bucket, response);

        B2CreateBucketRequestReal expectedRequest = new B2CreateBucketRequestReal(
                ACCOUNT_ID,
                new B2CreateBucketRequest(
                        BUCKET_NAME,
                        BUCKET_TYPE,
                        null,
                        null,
                        null,
                        false,
                        null,
                        null
                )
        );
        verify(webifier, times(1)).createBucket(eq(ACCOUNT_AUTH), eq(expectedRequest));
        retryer.assertCallCountIs(2); // auth + createBucket.
    }

    @Test
    public void testCreateBucket() throws B2Exception {
        final Map<String, String> bucketInfo = B2Collections.mapOf(
                "one", "1",
                "two", "2"
        );
        final List<B2LifecycleRule> lifecycleRules = listOf(
                B2LifecycleRule.builder(FILE_PREFIX).build()
        );
        final B2BucketServerSideEncryption defaultServerSideEncryption =
                B2BucketServerSideEncryption.createSseB2Aes256();
        final List<B2ReplicationRule> replicationRules = listOf(
                new B2ReplicationRule(
                        "my-replication-rule",
                        "000011112222333344445555",
                        3,
                        "",
                        false,
                        true
                ),
                new B2ReplicationRule(
                        "my-replication-rule-2",
                        "777011112222333344445555",
                        1,
                        "abc",
                        true,
                        false
                )
        );
        final Map<String, String> sourceToDestinationKeyMapping = new TreeMap<>();
        sourceToDestinationKeyMapping.put(
                "123a0a1a2a3a4a50000bc614e", "555a0a1a2a3a4a70000bc929a"
        );
        sourceToDestinationKeyMapping.put(
                "456a0b9a8a7a6a50000fc614e", "555a0a1a2a3a4a70000bc929a"
        );
        final B2BucketReplicationConfiguration replicationConfiguration =
                B2BucketReplicationConfiguration.createForSourceAndDestination(
                        "123a0a1a2a3a4a50000bc614e",
                        replicationRules,
                        sourceToDestinationKeyMapping
                );
        final B2CreateBucketRequest request = B2CreateBucketRequest
                .builder(BUCKET_NAME, BUCKET_TYPE)
                .setBucketInfo(bucketInfo)
                .setLifecycleRules(lifecycleRules)
                .setDefaultServerSideEncryption(defaultServerSideEncryption)
                .build();
        final B2Bucket bucket = new B2Bucket(
                ACCOUNT_ID,
                bucketId(1),
                BUCKET_NAME,
                BUCKET_TYPE,
                bucketInfo,
                new ArrayList<>(),
                lifecycleRules,
                Collections.emptySet(),
                new B2AuthorizationFilteredResponseField<>(true, new B2BucketFileLockConfiguration(true)),
                new B2AuthorizationFilteredResponseField<>(true, defaultServerSideEncryption),
                new B2AuthorizationFilteredResponseField<>(true, replicationConfiguration),
                1
        );
        final B2CreateBucketRequestReal expectedRequest = new B2CreateBucketRequestReal(ACCOUNT_ID, request);
        when(webifier.createBucket(ACCOUNT_AUTH, expectedRequest)).thenReturn(bucket);

        final B2Bucket response = client.createBucket(request);
        assertEquals(bucket, response);

        retryer.assertCallCountIs(2); // auth + createBucket.

        // for coverage
        //noinspection ResultOfMethodCallIgnored
        request.hashCode();
        //noinspection ResultOfMethodCallIgnored
        expectedRequest.hashCode();

        // for coverage
        assertEquals(bucket(1), bucket(1));
        //noinspection ResultOfMethodCallIgnored
        bucket.hashCode();
        assertEquals("B2Bucket(bucket1,allPublic,bucket1,2 infos,0 corsRules,1 lifecycleRules,0 options," +
                "B2AuthorizationFilteredResponseField(isClientAuthorizedToRead=true," +
                "value={true,B2FileRetention{mode=null, period=null}})," +
                "B2AuthorizationFilteredResponseField(isClientAuthorizedToRead=true," +
                "value={B2BucketServerSideEncryption{mode='SSE-B2', algorithm=AES256}})," +
                "B2AuthorizationFilteredResponseField(isClientAuthorizedToRead=true," +
                "value={B2BucketReplicationConfiguration{asReplicationSource=B2SourceConfig{" +
                "sourceApplicationKeyId='123a0a1a2a3a4a50000bc614e', " +
                "replicationRules=[B2ReplicationRule{replicationRuleName='my-replication-rule', " +
                "destinationBucketId='000011112222333344445555', priority=3, fileNamePrefix='', " +
                "isEnabled=false, includeExistingFiles=true}, " +
                "B2ReplicationRule{replicationRuleName='my-replication-rule-2', " +
                "destinationBucketId='777011112222333344445555', priority=1, fileNamePrefix='abc', " +
                "isEnabled=true, includeExistingFiles=false}]}, " +
                "asReplicationDestination=B2DestinationConfig{" +
                "sourceToDestinationKeyMapping={123a0a1a2a3a4a50000bc614e=555a0a1a2a3a4a70000bc929a, " +
                "456a0b9a8a7a6a50000fc614e=555a0a1a2a3a4a70000bc929a}}}}),v1)",
                bucket.toString()
        );

        final B2Bucket bucketWithOptions = new B2Bucket(
                ACCOUNT_ID,
                bucketId(1),
                BUCKET_NAME,
                BUCKET_TYPE,
                bucketInfo,
                new ArrayList<>(),
                lifecycleRules,
                B2TestHelpers.makeBucketOrApplicationKeyOptions(),
                new B2AuthorizationFilteredResponseField<>(true, new B2BucketFileLockConfiguration(true)),
                new B2AuthorizationFilteredResponseField<>(true, defaultServerSideEncryption),
                new B2AuthorizationFilteredResponseField<>(true, replicationConfiguration),
                1
        );

        assertEquals("B2Bucket(bucket1,allPublic,bucket1,2 infos,0 corsRules,1 lifecycleRules,[myOption1, " +
                        "myOption2] options,B2AuthorizationFilteredResponseField(isClientAuthorizedToRead=true," +
                        "value={true,B2FileRetention{mode=null, period=null}})," +
                        "B2AuthorizationFilteredResponseField(isClientAuthorizedToRead=true," +
                        "value={B2BucketServerSideEncryption{mode='SSE-B2', algorithm=AES256}})," +
                        "B2AuthorizationFilteredResponseField(isClientAuthorizedToRead=true," +
                        "value={B2BucketReplicationConfiguration{asReplicationSource=B2SourceConfig{" +
                        "sourceApplicationKeyId='123a0a1a2a3a4a50000bc614e', " +
                        "replicationRules=[B2ReplicationRule{replicationRuleName='my-replication-rule', " +
                        "destinationBucketId='000011112222333344445555', priority=3, fileNamePrefix='', " +
                        "isEnabled=false, includeExistingFiles=true}, " +
                        "B2ReplicationRule{replicationRuleName='my-replication-rule-2', " +
                        "destinationBucketId='777011112222333344445555', priority=1, fileNamePrefix='abc', " +
                        "isEnabled=true, includeExistingFiles=false}]}, " +
                        "asReplicationDestination=B2DestinationConfig{" +
                        "sourceToDestinationKeyMapping={123a0a1a2a3a4a50000bc614e=555a0a1a2a3a4a70000bc929a, " +
                        "456a0b9a8a7a6a50000fc614e=555a0a1a2a3a4a70000bc929a}}}}),v1)",
                bucketWithOptions.toString());
    }

    @Test
    public void testGetAndInvalidateAccountAuthorization() throws B2Exception {
        assertNotNull(client.getAccountAuthorization());
        assertNotNull(client.getAccountAuthorization());
        verify(webifier, times(1)).authorizeAccount(anyObject());
        retryer.assertCallCountIs(4); // 2*auth + 2*authorizeAccount.

        // invalidate the cache.
        client.invalidateAccountAuthorization();

        assertNotNull(client.getAccountAuthorization());
        verify(webifier, times(2)).authorizeAccount(anyObject());
        retryer.assertCallCountIs(6); // added another call to auth * authorizeAccount.
    }

    // test that the retryer is caching account authorizations.
    // note that i'm not testing this for every api call!
    @Test
    public void testUsesAccountAuthorizationCache() throws B2Exception {
        client.createBucket(BUCKET_NAME, BUCKET_TYPE);
        client.createBucket(BUCKET_NAME, BUCKET_TYPE);
        verify(webifier, times(1)).authorizeAccount(anyObject());
        verify(webifier, times(2)).createBucket(anyObject(), anyObject());
        retryer.assertCallCountIs(4); // 2*auth + 2*createBucket.

        // for coverage
        //noinspection ResultOfMethodCallIgnored
        new B2AuthorizeAccountRequest(ACCOUNT_ID, APPLICATION_KEY).hashCode();
    }

    @Test
    public void testListBuckets() throws B2Exception {
        final B2ListBucketsRequest expectedRequest = B2ListBucketsRequest
                .builder(ACCOUNT_ID)
                .build();
        final B2ListBucketsResponse response = new B2ListBucketsResponse(
                listOf(bucket(1))
        );
        when(webifier.listBuckets(ACCOUNT_AUTH, expectedRequest)).thenReturn(response);

        assertEquals(response, client.listBuckets());
        assertEquals(response.getBuckets(), client.buckets());

        // for coverage
        //noinspection ResultOfMethodCallIgnored
        expectedRequest.hashCode();
        //noinspection ResultOfMethodCallIgnored
        response.hashCode();
        assertEquals(response, new B2ListBucketsResponse(listOf(bucket(1))));
    }

    @Test
    public void testListBucketsWithFiltering() throws B2Exception {
        final B2ListBucketsRequest expectedRequest = B2ListBucketsRequest
                .builder(ACCOUNT_ID)
                .setBucketTypes(Collections.singleton("allPublic"))
                .build();
        final B2ListBucketsResponse response = new B2ListBucketsResponse(
                listOf(bucket(1))
        );
        when(webifier.listBuckets(ACCOUNT_AUTH, expectedRequest)).thenReturn(response);

        assertEquals(response, client.listBuckets(expectedRequest));

        // for coverage
        //noinspection ResultOfMethodCallIgnored
        expectedRequest.hashCode();
        //noinspection ResultOfMethodCallIgnored
        response.hashCode();
        assertEquals(response, new B2ListBucketsResponse(listOf(bucket(1))));
    }

    @Test
    public void testGetBucketByName() throws B2Exception {
        final B2ListBucketsRequest expectedRequest = B2ListBucketsRequest
                .builder(ACCOUNT_ID)
                .setBucketName(BUCKET_NAME)
                .build();
        final B2ListBucketsResponse response = new B2ListBucketsResponse(
                listOf(bucket(1))
        );
        when(webifier.listBuckets(ACCOUNT_AUTH, expectedRequest)).thenReturn(response);

        assertEquals(bucket(1), client.getBucketOrNullByName(BUCKET_NAME));

        final String noSuchBucketName = "noSuchBucket";
        final B2ListBucketsRequest expectedNoSuchBucketRequest = B2ListBucketsRequest
                .builder(ACCOUNT_ID)
                .setBucketName(noSuchBucketName)
                .build();
        final B2ListBucketsResponse noSuchBucketResponse = new B2ListBucketsResponse(Collections.emptyList());
        when(webifier.listBuckets(ACCOUNT_AUTH, expectedNoSuchBucketRequest)).thenReturn(noSuchBucketResponse);

        assertNull(client.getBucketOrNullByName("noSuchBucket"));
    }

    @SuppressWarnings("SameParameterValue")
    private B2Bucket bucket(int i) {
        return new B2Bucket(
                ACCOUNT_ID,
                bucketId(i),
                BUCKET_NAME,
                BUCKET_TYPE,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                2
        );
    }

    @Test
    public void testCancelLargeFile() throws B2Exception {
        final B2CancelLargeFileRequest request = B2CancelLargeFileRequest.builder(LARGE_FILE_ID).build();
        client.cancelLargeFile(request);
        verify(webifier, times(1)).cancelLargeFile(anyObject(), eq(request));

        client.cancelLargeFile(LARGE_FILE_ID);
        verify(webifier, times(2)).cancelLargeFile(anyObject(), eq(request));

        // for coverage
        //noinspection ResultOfMethodCallIgnored
        request.hashCode();

        final B2CancelLargeFileResponse response = new B2CancelLargeFileResponse(fileId(1), bucketId(1), fileName(1));
        assertEquals(response, new B2CancelLargeFileResponse(fileId(1), bucketId(1), fileName(1)));
        //noinspection ResultOfMethodCallIgnored
        response.hashCode();
        assertEquals("B2FileVersion{fileId='" + fileId(1) + "', bucketId='" + bucketId(1) + "', fileName='" + fileName(1) + "'}", response.toString());
    }

    @Test
    public void testDeleteFileVersion() throws B2Exception {
        final B2DeleteFileVersionRequest request = B2DeleteFileVersionRequest.builder(fileName(1), fileId(1)).build();
        client.deleteFileVersion(request);
        verify(webifier, times(1)).deleteFileVersion(anyObject(), eq(request));

        final B2FileVersion fileVersion = makeVersion(1, 1);
        client.deleteFileVersion(fileVersion);
        verify(webifier, times(2)).deleteFileVersion(anyObject(), eq(request));

        // for coverage
        //noinspection ResultOfMethodCallIgnored
        request.hashCode();

        final B2DeleteFileVersionResponse response = new B2DeleteFileVersionResponse(fileId(1), fileName(1));
        assertEquals(response, new B2DeleteFileVersionResponse(fileId(1), fileName(1)));
        //noinspection ResultOfMethodCallIgnored
        response.hashCode();
        assertEquals("B2DeleteFileVersionResponse{fileId='" + fileId(1) + "', fileName='" + fileName(1) + "'}", response.toString());

    }

    @Test
    public void testGetDownloadAuthorization() throws B2Exception {
        final B2DownloadAuthorization downloadAuth = new B2DownloadAuthorization(bucketId(1), FILE_PREFIX, "downloadAuthToken");
        final B2GetDownloadAuthorizationRequest request = B2GetDownloadAuthorizationRequest.builder(bucketId(1), FILE_PREFIX, 100).build();
        when(webifier.getDownloadAuthorization(anyObject(), eq(request))).thenReturn(downloadAuth);

        assertEquals(downloadAuth, client.getDownloadAuthorization(request));
        verify(webifier, times(1)).getDownloadAuthorization(anyObject(), eq(request));

        // for coverage
        //noinspection ResultOfMethodCallIgnored
        request.hashCode();
        assertEquals(request, B2GetDownloadAuthorizationRequest.builder(bucketId(1), FILE_PREFIX, 100).build());
    }


    @Test
    public void testGetFileInfo() throws B2Exception {
        final B2FileVersion fileVersion = makeVersion(1, 2);
        final B2GetFileInfoRequest request = B2GetFileInfoRequest.builder(fileId(1)).build();
        when(webifier.getFileInfo(anyObject(), eq(request))).thenReturn(fileVersion);

        assertEquals(fileVersion, client.getFileInfo(request));
        verify(webifier, times(1)).getFileInfo(anyObject(), eq(request));

        assertEquals(fileVersion, client.getFileInfo(fileId(1)));
        verify(webifier, times(2)).getFileInfo(anyObject(), eq(request));

        // for coverage
        //noinspection ResultOfMethodCallIgnored
        request.hashCode();
    }

    @Test
    public void testGetFileInfoByName() throws B2Exception {
        final B2FileVersion fileVersion = makeVersion(1, 2);
        final B2GetFileInfoByNameRequest request = B2GetFileInfoByNameRequest
                .builder(bucketName(1), fileName(1))
                .build();

        when(webifier.getFileInfoByName(anyObject(), eq(request))).thenReturn(fileVersion);

        assertEquals(fileVersion, client.getFileInfoByName(request));

        verify(webifier, times(1)).getFileInfoByName(anyObject(), eq(request));

        assertEquals(fileVersion, client.getFileInfoByName(bucketName(1), fileName(1)));
        verify(webifier, times(2)).getFileInfoByName(anyObject(), eq(request));

        // for coverage
        //noinspection ResultOfMethodCallIgnored
        request.hashCode();
    }

    @Test
    public void testHideFile() throws B2Exception {
        final B2FileVersion fileVersion = makeVersion(6, 2);
        final B2HideFileRequest request = B2HideFileRequest.builder(bucketId(1), fileName(2)).build();
        when(webifier.hideFile(anyObject(), eq(request))).thenReturn(fileVersion);

        assertEquals(fileVersion, client.hideFile(request));
        verify(webifier, times(1)).hideFile(anyObject(), eq(request));

        // for coverage
        //noinspection ResultOfMethodCallIgnored
        request.hashCode();
        assertEquals(request, B2HideFileRequest.builder(bucketId(1), fileName(2)).build());
    }


    @Test
    public void testUpdateBucket() throws B2Exception {
        final B2UpdateBucketRequest request = B2UpdateBucketRequest
                .builder(bucket(1))
                .setBucketInfo(B2Collections.mapOf())
                .setLifecycleRules(listOf())
                .setBucketType(B2BucketTypes.ALL_PUBLIC)
                .build();
        when(webifier.updateBucket(anyObject(), eq(request))).thenReturn(bucket(1));

        assertEquals(bucket(1), client.updateBucket(request));
        verify(webifier, times(1)).updateBucket(anyObject(), eq(request));

        // for coverage
        //noinspection ResultOfMethodCallIgnored
        request.hashCode();
        assertEquals(
                request,
                B2UpdateBucketRequest
                        .builder(bucket(1))
                        .setBucketInfo(B2Collections.mapOf())
                        .setLifecycleRules(listOf())
                        .setBucketType(B2BucketTypes.ALL_PUBLIC)
                        .build());
    }


    @Test
    public void testDeleteBucket() throws B2Exception {
        final B2Bucket bucket = bucket(1);
        final B2DeleteBucketRequest request = B2DeleteBucketRequest.builder(bucketId(1)).build();
        final B2DeleteBucketRequestReal realRequest = new B2DeleteBucketRequestReal(ACCOUNT_ID, bucketId(1));
        when(webifier.deleteBucket(anyObject(), eq(realRequest))).thenReturn(bucket);

        assertEquals(bucket, client.deleteBucket(request));
        verify(webifier, times(1)).deleteBucket(anyObject(), eq(realRequest));

        assertEquals(bucket, client.deleteBucket(bucketId(1)));
        verify(webifier, times(2)).deleteBucket(anyObject(), eq(realRequest));

        // for coverage
        //noinspection ResultOfMethodCallIgnored
        request.hashCode();
        //noinspection ResultOfMethodCallIgnored
        realRequest.hashCode();
        assertEquals(request, B2DeleteBucketRequest.builder(bucketId(1)).build());
    }

    @Test
    public void testListFileVersions() throws B2Exception {
        final B2ListFileVersionsRequest request = B2ListFileVersionsRequest.builder(bucketId(1)).build();
        final B2ListFileVersionsResponse response = new B2ListFileVersionsResponse(listOf(), null, null);
        when(webifier.listFileVersions(anyObject(), eq(request))).thenReturn(response);

        assertEquals(response, client.listFileVersions(request));

        // for coverage
        //noinspection ResultOfMethodCallIgnored
        response.hashCode();
        assertEquals(response, new B2ListFileVersionsResponse(listOf(), null, null));
    }

    @Test
    public void testFileVersions() throws B2Exception {
        final B2ListFileVersionsRequest request = B2ListFileVersionsRequest.builder(bucketId(1)).setMaxFileCount(1000).build();
        final B2FileVersion version1 = makeVersion(1, 1);
        final B2ListFileVersionsResponse response = new B2ListFileVersionsResponse(listOf(version1), null, null);
        when(webifier.listFileVersions(anyObject(), eq(request))).thenReturn(response);

        // with request object
        assertIterContents(client.fileVersions(request),
                version1
        );

        // convenience version.
        assertIterContents(client.fileVersions(bucketId(1)),
                version1
        );
    }

    @Test
    public void testFileNames() throws B2Exception {
        final B2ListFileNamesRequest request = B2ListFileNamesRequest.builder(bucketId(1)).setMaxFileCount(1000).build();
        final B2FileVersion version1 = makeVersion(1, 1);
        final B2ListFileNamesResponse response = new B2ListFileNamesResponse(listOf(version1), null);
        when(webifier.listFileNames(anyObject(), eq(request))).thenReturn(response);

        // with request object
        assertIterContents(client.fileNames(request),
                version1
        );

        // convenience version.
        assertIterContents(client.fileNames(bucketId(1)),
                version1
        );
    }

    @Test
    public void testUnfinishedLargeFiles() throws B2Exception {
        final B2ListUnfinishedLargeFilesRequest request = B2ListUnfinishedLargeFilesRequest.builder(bucketId(1)).setMaxFileCount(100).build();
        final B2FileVersion version1 = makeVersion(1, 1);
        final B2ListUnfinishedLargeFilesResponse response = new B2ListUnfinishedLargeFilesResponse(listOf(version1), null);
        when(webifier.listUnfinishedLargeFiles(anyObject(), eq(request))).thenReturn(response);

        // with request object
        assertIterContents(client.unfinishedLargeFiles(request),
                version1
        );

        // convenience version.
        assertIterContents(client.unfinishedLargeFiles(bucketId(1)),
                version1
        );
    }

    @Test
    public void testParts() throws B2Exception {
        final B2ListPartsRequest request = B2ListPartsRequest.builder(LARGE_FILE_ID).setMaxPartCount(100).build();
        final B2Part part = makePart(1);
        final B2ListPartsResponse response = new B2ListPartsResponse(listOf(part), null);
        when(webifier.listParts(anyObject(), eq(request))).thenReturn(response);

        // with request object
        assertIterContents(client.parts(request),
                part
        );

        // convenience version.
        assertIterContents(client.parts(LARGE_FILE_ID),
                part
        );
    }

    private <T> void assertIterContents(Iterable<T> iterable, T... expecteds) {
        int iExpected = 0;
        for (T actual : iterable) {
            B2Preconditions.checkState(iExpected < expecteds.length,
                    "more items in iterable than in expected? (expecteds.length=" + expecteds.length + ", iExpected=" + iExpected + ")");
            assertEquals(expecteds[iExpected], actual);
            iExpected++;
        }
        B2Preconditions.checkState(iExpected == expecteds.length,
                "different number of items in expected than in iterable?  (expecteds.length=" + expecteds.length + ", iExpected=" + iExpected + ")");
    }


    @Test
    public void testListFileNames() throws B2Exception {
        final B2ListFileNamesRequest request = B2ListFileNamesRequest.builder(bucketId(1)).build();
        final B2ListFileNamesResponse response = new B2ListFileNamesResponse(listOf(), null);
        when(webifier.listFileNames(anyObject(), eq(request))).thenReturn(response);

        assertEquals(response, client.listFileNames(request));

        // for coverage
        //noinspection ResultOfMethodCallIgnored
        response.hashCode();
        assertEquals(response, new B2ListFileNamesResponse(listOf(), null));
    }

    @Test
    public void testListUnfinishedLargeFiles() throws B2Exception {
        final B2ListUnfinishedLargeFilesRequest request = B2ListUnfinishedLargeFilesRequest.builder(bucketId(1)).build();
        final B2ListUnfinishedLargeFilesResponse response = new B2ListUnfinishedLargeFilesResponse(listOf(), null);
        when(webifier.listUnfinishedLargeFiles(anyObject(), eq(request))).thenReturn(response);

        assertEquals(response, client.listUnfinishedLargeFiles(request));

        // for coverage
        //noinspection ResultOfMethodCallIgnored
        response.hashCode();
        assertEquals(response, new B2ListUnfinishedLargeFilesResponse(listOf(), null));
    }

    @Test
    public void testListParts() throws B2Exception {
        final B2ListPartsRequest request = B2ListPartsRequest.builder(bucketId(1)).build();
        final B2ListPartsResponse response = new B2ListPartsResponse(listOf(), null);
        when(webifier.listParts(anyObject(), eq(request))).thenReturn(response);

        assertEquals(response, client.listParts(request));

        // for coverage
        //noinspection ResultOfMethodCallIgnored
        response.hashCode();
        assertEquals(response, new B2ListPartsResponse(listOf(), null));
    }

    @Test
    public void test_forCoverage() {
        new B2StorageClientImpl(webifier, config, B2DefaultRetryPolicy.supplier());
    }

    @Test
    public void testDownloadById() throws B2Exception {
        final B2ContentSink handler = (responseHeaders, in) -> {
        };
        B2DownloadByIdRequest request = B2DownloadByIdRequest
                .builder(LARGE_FILE_ID)
                .setRange(B2ByteRange.between(10, 12))
                .build();
        assertEquals(B2ByteRange.between(10, 12), request.getRange());
        client.downloadById(request, handler);

        verify(webifier, times(1)).downloadById(anyObject(), eq(request), eq(handler));

        // check the "convenience" form that takes a fileId instead of a request.
        client.downloadById(fileId(2), handler);
        B2DownloadByIdRequest request2 = B2DownloadByIdRequest
                .builder(fileId(2))
                .build();
        assertNull(request2.getRange());
        verify(webifier, times(1)).downloadById(anyObject(), eq(request2), eq(handler));

        // for coverage
        //noinspection ResultOfMethodCallIgnored
        request.hashCode();
    }

    @Test
    public void testGetDownloadByIdUrl() throws B2Exception {
        final String expectedUrl = "http://example.com/blah";
        B2DownloadByIdRequest request = B2DownloadByIdRequest
                .builder(LARGE_FILE_ID)
                .build();
        when(webifier.getDownloadByIdUrl(ACCOUNT_AUTH, request)).thenReturn(expectedUrl);

        assertEquals(expectedUrl, client.getDownloadByIdUrl(request));

        // check the "convenience" form that takes a fileId instead of a request.
        assertEquals(expectedUrl, client.getDownloadByIdUrl(LARGE_FILE_ID));
    }

    @Test
    public void testDownloadByName() throws B2Exception {
        final B2ContentSink handler = (responseHeaders, in) -> {
        };
        B2DownloadByNameRequest request = B2DownloadByNameRequest
                .builder(bucketName(1), fileName(1))
                .setRange(B2ByteRange.startAt(17))
                .build();
        client.downloadByName(request, handler);

        verify(webifier, times(1)).downloadByName(anyObject(), eq(request), eq(handler));

        // check the "convenience" form that takes a bucketName & fileName instead of a request.
        client.downloadByName(bucketName(1), fileName(1), handler);
        B2DownloadByNameRequest request2 = B2DownloadByNameRequest
                .builder(bucketName(1), fileName(1))
                .build();
        verify(webifier, times(1)).downloadByName(anyObject(), eq(request2), eq(handler));

        // for coverage
        //noinspection ResultOfMethodCallIgnored
        request.hashCode();
        assertEquals(request,
                B2DownloadByNameRequest
                        .builder(bucketName(1), fileName(1))
                        .setRange(B2ByteRange.startAt(17))
                        .build());
    }

    @Test
    public void testGetDownloadByNameUrl() throws B2Exception {
        final String expectedUrl = "http://example.com/blah";

        B2DownloadByNameRequest request = B2DownloadByNameRequest
                .builder(bucketName(1), fileName(1))
                .build();
        when(webifier.getDownloadByNameUrl(ACCOUNT_AUTH, request)).thenReturn(expectedUrl);

        assertEquals(expectedUrl, client.getDownloadByNameUrl(request));

        // check the "convenience" form that takes a bucketName & fileName instead of a request.
        assertEquals(expectedUrl, client.getDownloadByNameUrl(bucketName(1), fileName(1)));
    }

//    @Test
//    public void testSimpleBuilder() throws B2Exception {
//        final B2StorageClientImpl impl = (B2StorageClientImpl) B2StorageClient.builder(ACCOUNT_ID, APPLICATION_KEY, USER_AGENT).build();
//        assertTrue(impl.getConfig().getAccountAuthorizer() instanceof B2AccountAuthorizerSimpleImpl);
//        assertNull(impl.getConfig().getMasterUrl());
//        assertNull(impl.getConfig().getTestModeOrNull());
//    }
//
//    @Test
//    public void testBuilderFromConfig() throws B2Exception {
//        final B2StorageClientImpl impl = (B2StorageClientImpl) B2StorageClient.builder(config).build();
//        assertTrue(config == impl.getConfig());
//    }
//
//    @Test
//    public void testBuilderWithWebApiClient() throws B2Exception {
//        final B2WebApiClient webApiClient = mock(B2WebApiClient.class);
//        final B2StorageClientImpl client = (B2StorageClientImpl) B2StorageClient
//                .builder(config)
//                .setWebApiClient(webApiClient)
//                .build();
//
//        when(webApiClient.postJsonReturnJson(anyObject(), anyObject(), anyObject(), eq(B2AccountAuthorization.class)))
//                .thenReturn(makeAuth(1));
//        when(webApiClient.postJsonReturnJson(anyObject(), anyObject(), anyObject(), eq(B2ListBucketsResponse.class)))
//                .thenReturn(new B2ListBucketsResponse(listOf(bucket(1))));
//
//        client.listBuckets();
//
//        verify(webApiClient, times(2)).postJsonReturnJson(anyObject(), anyObject(), anyObject(), anyObject());
//    }

    @Test
    public void testSmallFileUpload() throws B2Exception, IOException {
        // arrange for an uploadUrl
        final B2GetUploadUrlRequest uploadUrlRequest = B2GetUploadUrlRequest.builder(bucketId(1)).build();
        final B2UploadUrlResponse uploadUrl = new B2UploadUrlResponse(bucketId(1), "uploadUrl", "uploadAuthToken");
        when(webifier.getUploadUrl(anyObject(), eq(uploadUrlRequest))).thenReturn(uploadUrl);

        // make a content source that's small enough to be a small file.
        final long contentLen = (2 * ACCOUNT_AUTH.getRecommendedPartSize()) - 1; // the biggest it can be and still be a small file.
        final B2ContentSource contentSource = mock(B2ContentSource.class);
        when(contentSource.getContentLength()).thenReturn(contentLen);

        final B2UploadListener listener = mock(B2UploadListener.class);

        final B2UploadFileRequest request = B2UploadFileRequest
                .builder(bucketId(1), fileName(1), B2ContentTypes.TEXT_PLAIN, contentSource)
                .setCustomField("color", "blue")
                .setListener(listener)
                .build();

        client.uploadSmallFile(request);

        verifyNoMoreInteractions(listener);      // the webifier is a mock, so it doesn't do normal things
        verifyNoMoreInteractions(contentSource); // the webifier is a mock, so it doesn't do normal things

        verify(webifier, times(1)).uploadFile(eq(uploadUrl), eq(request));


        // for coverage
        //noinspection ResultOfMethodCallIgnored
        uploadUrlRequest.hashCode();
        //noinspection ResultOfMethodCallIgnored
        uploadUrl.hashCode();
        assertEquals(uploadUrl, new B2UploadUrlResponse(bucketId(1), "uploadUrl", "uploadAuthToken"));
    }

    @Test
    public void testSmallFileCopy() throws B2Exception {
        final B2FileVersion fileVersion = makeVersion(2, 2);
        final B2CopyFileRequest request = B2CopyFileRequest.builder(fileId(1), fileName(2)).build();
        when(webifier.copyFile(anyObject(), eq(request))).thenReturn(fileVersion);

        assertEquals(fileVersion, client.copySmallFile(request));
        verify(webifier, times(1)).copyFile(anyObject(), eq(request));

        // for coverage
        //noinspection ResultOfMethodCallIgnored
        request.hashCode();
    }

    @Test
    public void testLargeFileUpload() throws B2Exception, IOException {
        // make a content source that's barely big enough to be a large file.
        final long contentLen = (2 * ACCOUNT_AUTH.getRecommendedPartSize());
        final B2ContentSource contentSource = mock(B2ContentSource.class);
        when(contentSource.getContentLength()).thenReturn(contentLen);

        final B2UploadListener listener = mock(B2UploadListener.class);
        final B2UploadFileRequest request = B2UploadFileRequest
                .builder(bucketId(1), fileName(1), B2ContentTypes.TEXT_PLAIN, contentSource)
                .setListener(listener)
                .build();

        // arrange to answer start_large_file
        final B2StartLargeFileRequest startLargeRequest = B2StartLargeFileRequest.buildFrom(request);
        final B2FileVersion largeFileVersion = makeVersion(1, 2);
        when(webifier.startLargeFile(anyObject(), eq(startLargeRequest))).thenReturn(largeFileVersion);

        // arrange to answer get_upload_part_url (which will be called several times, but it's ok to reuse the same value since it's all mocked!)
        final B2GetUploadPartUrlRequest partUrlRequest = B2GetUploadPartUrlRequest.builder(largeFileVersion.getFileId()).build();
        final B2UploadPartUrlResponse partUrl = new B2UploadPartUrlResponse(largeFileVersion.getFileId(), "uploadPartUrl", "uploadPartAuthToken");
        when(webifier.getUploadPartUrl(anyObject(), eq(partUrlRequest))).thenReturn(partUrl);

        // arrange to answer upload_part (which will be called several times, but it's ok to reuse the same value since it's all mocked!)
        final B2Part part = makePart(1);
        when(webifier.uploadPart(anyObject(), anyObject())).thenReturn(part);

        // arrange to answer finish_large_file
        final B2FinishLargeFileRequest finishRequest = new B2FinishLargeFileRequest(largeFileVersion.getFileId(), listOf(B2TestHelpers.SAMPLE_SHA1));
        when(webifier.finishLargeFile(anyObject(), eq(finishRequest))).thenReturn(largeFileVersion);

        client.uploadLargeFile(request, executor);

        verify(contentSource, times(1)).getContentLength();
        verify(contentSource, times(2)).getSha1OrNull(); // once above while making the startLargeRequest for the mock & once for real
        verify(contentSource, times(2)).createContentSourceWithRangeOrNull(anyLong(), anyLong()); // once above while making the startLargeRequest for the mock & once for real
        verifyNoMoreInteractions(contentSource); // the webifier is a mock, so it doesn't do normal things


        verify(listener, times(1)).progress(eq(new B2UploadProgress(0, 2, 0, 1000, 0, WAITING_TO_START)));
        verify(listener, times(1)).progress(eq(new B2UploadProgress(0, 2, 0, 1000, 0, STARTING)));
        verify(listener, times(1)).progress(eq(new B2UploadProgress(0, 2, 0, 1000, 1000, SUCCEEDED)));

        verify(listener, times(1)).progress(eq(new B2UploadProgress(1, 2, 1000, 1000, 0, WAITING_TO_START)));
        verify(listener, times(1)).progress(eq(new B2UploadProgress(1, 2, 1000, 1000, 0, STARTING)));
        verify(listener, times(1)).progress(eq(new B2UploadProgress(1, 2, 1000, 1000, 1000, SUCCEEDED)));

        verifyNoMoreInteractions(listener);      // the webifier is a mock, so we don't get calls for progress, just calls for overall state changes.


        verify(webifier, times(1)).startLargeFile(anyObject(), anyObject());
        // there's a very unlikely race condition where we might upload one part and then unget the url and upload another.  it's really unlikely, but it means there's a chance there might only be one call, not two...
        //verify(webifier, times(2)).getUploadPartUrl(anyObject(), anyObject());
        verify(webifier, times(2)).uploadPart(anyObject(), anyObject());
        verify(webifier, times(1)).finishLargeFile(anyObject(), anyObject());


        // for coverage
        //noinspection ResultOfMethodCallIgnored
        partUrlRequest.hashCode();
        //noinspection ResultOfMethodCallIgnored
        startLargeRequest.hashCode();
        //noinspection ResultOfMethodCallIgnored
        partUrl.hashCode();
        //noinspection ResultOfMethodCallIgnored
        finishRequest.hashCode();
        assertEquals(partUrl, new B2UploadPartUrlResponse(largeFileVersion.getFileId(), "uploadPartUrl", "uploadPartAuthToken"));
        final B2UploadPartRequest partRequest = B2UploadPartRequest.builder(1, contentSource).build();
        assertEquals(partRequest, B2UploadPartRequest.builder(1, contentSource).build());
        //noinspection ResultOfMethodCallIgnored
        partRequest.hashCode();

    }

    @Test
    public void testFinishUploadingLargeFile() throws B2Exception, IOException {
        final long contentLen = (3 * ACCOUNT_AUTH.getRecommendedPartSize() + 124);
        final B2ContentSource contentSource = mock(B2ContentSource.class);
        when(contentSource.getContentLength()).thenReturn(contentLen);

        final B2UploadFileRequest request = B2UploadFileRequest
                .builder(bucketId(1), fileName(1), B2ContentTypes.TEXT_PLAIN, contentSource)
                .build();

        final B2FileVersion largeFileVersion = new B2FileVersion(fileId(1),
                fileName(1),
                contentLen,
                B2ContentTypes.TEXT_PLAIN,
                null,
                null,
                B2Collections.mapOf(),
                "upload",
                B2Clock.get().wallClockMillis(),
                null,
                null,
                null,
                null);

        final String largeFileId = largeFileVersion.getFileId();

        // arrange to find that two parts -- the first and third -- have already been uploaded.
        final List<B2Part> alreadyUploadedParts = listOf(
                new B2Part(largeFileId, 1, 1041, makeSha1(1), makeMd5(1), 1111, null),
                new B2Part(largeFileId, 3, 1042, makeSha1(3), makeMd5(3), 3333, null)
        );
        final B2ListPartsResponse listPartsResponse = new B2ListPartsResponse(alreadyUploadedParts, null);
        when(webifier.listParts(anyObject(), anyObject())).thenReturn(listPartsResponse);

        // arrange to answer get_upload_part_url (which will be called several times, but it's ok to reuse the same value since it's all mocked!)
        final B2GetUploadPartUrlRequest partUrlRequest = B2GetUploadPartUrlRequest.builder(largeFileId).build();
        final B2UploadPartUrlResponse partUrl = new B2UploadPartUrlResponse(largeFileId, "uploadPartUrl", "uploadPartAuthToken");
        when(webifier.getUploadPartUrl(anyObject(), eq(partUrlRequest))).thenReturn(partUrl);

        // arrange to answer upload_part
        final B2Part part = makePart(2);
        when(webifier.uploadPart(anyObject(), anyObject())).thenReturn(part);

        // arrange to answer finish_large_file
        final B2FinishLargeFileRequest finishRequest = new B2FinishLargeFileRequest(largeFileId, listOf(B2TestHelpers.SAMPLE_SHA1));
        when(webifier.finishLargeFile(anyObject(), eq(finishRequest))).thenReturn(largeFileVersion);

        client.finishUploadingLargeFile(largeFileVersion, request, executor);

        verify(contentSource, times(1)).getContentLength();
        verify(contentSource, times(1)).getSha1OrNull();
        verify(contentSource, times(1)).createContentSourceWithRangeOrNull(anyLong(), anyLong());
        verifyNoMoreInteractions(contentSource); // the webifier is a mock, so it doesn't do normal things

        // we should be using the existing largeFile, not starting a new one.
        verify(webifier, never()).startLargeFile(anyObject(), anyObject());

        // we're only uploading 1 of the three, since two were already uploaded
        verify(webifier, times(1)).getUploadPartUrl(anyObject(), anyObject());
        verify(webifier, times(1)).uploadPart(anyObject(), anyObject());
        verify(webifier, times(1)).finishLargeFile(anyObject(), anyObject());
    }

    @Test
    public void testGetUploadUrl() throws B2Exception {
        final B2GetUploadUrlRequest request = B2GetUploadUrlRequest.builder(bucketId(1)).build();
        final B2UploadUrlResponse response = new B2UploadUrlResponse(bucketId(1), "uploadUrl", "uploadAuthToken");
        when(webifier.getUploadUrl(anyObject(), eq(request))).thenReturn(response);

        assertSame(response, client.getUploadUrl(request));

        verify(webifier, times(1)).authorizeAccount(anyObject());
        verify(webifier, times(1)).getUploadUrl(anyObject(), anyObject());
    }

    @Test
    public void testGetUploadPartUrl() throws B2Exception {
        final B2GetUploadPartUrlRequest request = B2GetUploadPartUrlRequest.builder(bucketId(1)).build();
        final B2UploadPartUrlResponse response = new B2UploadPartUrlResponse(bucketId(1), "uploadUrl", "uploadAuthToken");
        when(webifier.getUploadPartUrl(anyObject(), eq(request))).thenReturn(response);

        assertSame(response, client.getUploadPartUrl(request));

        verify(webifier, times(1)).authorizeAccount(anyObject());
        verify(webifier, times(1)).getUploadPartUrl(anyObject(), anyObject());
    }

    @Test
    public void testStartLargeFile() throws B2Exception {
        final B2StartLargeFileRequest request = B2StartLargeFileRequest
                .builder(bucketId(1), fileName(2), B2ContentTypes.APPLICATION_OCTET)
                .build();
        final B2FileVersion fileVersion = makeVersion(1, 2);
        when(webifier.startLargeFile(anyObject(), eq(request))).thenReturn(fileVersion);

        assertSame(fileVersion, client.startLargeFile(request));

        verify(webifier, times(1)).authorizeAccount(anyObject());
        verify(webifier, times(1)).startLargeFile(anyObject(), anyObject());
    }

    @Test
    public void testFinishLargeFile() throws B2Exception {
        final B2FinishLargeFileRequest request = B2FinishLargeFileRequest
                .builder(fileId(1), new ArrayList<>())
                .build();
        final B2FileVersion fileVersion = makeVersion(1, 2);
        when(webifier.finishLargeFile(anyObject(), eq(request))).thenReturn(fileVersion);

        assertSame(fileVersion, client.finishLargeFile(request));

        verify(webifier, times(1)).authorizeAccount(anyObject());
        verify(webifier, times(1)).finishLargeFile(anyObject(), anyObject());
    }

    @Test
    public void testUpdateFileLegalHold() throws B2Exception {
        final B2UpdateFileLegalHoldRequest request = B2UpdateFileLegalHoldRequest
                .builder(fileName(1), fileId(1), B2LegalHold.ON)
                .build();
        final B2UpdateFileLegalHoldResponse response =
                new B2UpdateFileLegalHoldResponse(fileName(1), fileId(1), B2LegalHold.ON);
        when(webifier.updateFileLegalHold(any(), eq(request))).thenReturn(response);

        assertSame(response, client.updateFileLegalHold(request));

        verify(webifier, times(1)).authorizeAccount(any());
        verify(webifier, times(1)).updateFileLegalHold(any(), eq(request));
    }

    @Test
    public void testUpdateFileRetention() throws B2Exception {
        final B2FileRetention fileRetention = new B2FileRetention(B2FileRetentionMode.COMPLIANCE, 10000L);
        final B2UpdateFileRetentionRequest request = B2UpdateFileRetentionRequest
                .builder(fileName(1), fileId(1), fileRetention)
                .build();
        final B2UpdateFileRetentionResponse response =
                new B2UpdateFileRetentionResponse(fileName(1), fileId(1), fileRetention);
        when(webifier.updateFileRetention(any(), eq(request))).thenReturn(response);

        assertSame(response, client.updateFileRetention(request));

        verify(webifier, times(1)).authorizeAccount(any());
        verify(webifier, times(1)).updateFileRetention(any(), eq(request));
    }

    @Test
    public void testClose() {
        final B2AccountAuthorizer authorizer = mock(B2AccountAuthorizer.class);
        final B2ClientConfig config = mock(B2ClientConfig.class);
        when(config.getAccountAuthorizer()).thenReturn(authorizer);

        final B2StorageClientImpl client = new B2StorageClientImpl(webifier, config, B2DefaultRetryPolicy.supplier());

        // closing the client should close the config the first time.
        client.close();
        verify(webifier, times(1)).close();

        // closing the client should do nothing the second time.
        client.close();
        verify(webifier, times(1)).close();
    }

}
