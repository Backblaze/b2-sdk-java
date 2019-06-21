/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */

package com.backblaze.b2.client;

import com.backblaze.b2.client.contentHandlers.B2ContentSink;
import com.backblaze.b2.client.contentSources.B2ContentSource;
import com.backblaze.b2.client.exceptions.B2Exception;
import com.backblaze.b2.client.structures.B2AccountAuthorization;
import com.backblaze.b2.client.structures.B2ApplicationKey;
import com.backblaze.b2.client.structures.B2Bucket;
import com.backblaze.b2.client.structures.B2CancelLargeFileRequest;
import com.backblaze.b2.client.structures.B2CopyFileRequest;
import com.backblaze.b2.client.structures.B2CreateBucketRequest;
import com.backblaze.b2.client.structures.B2CreateKeyRequest;
import com.backblaze.b2.client.structures.B2CreatedApplicationKey;
import com.backblaze.b2.client.structures.B2DeleteBucketRequest;
import com.backblaze.b2.client.structures.B2DeleteFileVersionRequest;
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
import com.backblaze.b2.client.structures.B2ListFileVersionsRequest;
import com.backblaze.b2.client.structures.B2ListKeysRequest;
import com.backblaze.b2.client.structures.B2ListPartsRequest;
import com.backblaze.b2.client.structures.B2ListUnfinishedLargeFilesRequest;
import com.backblaze.b2.client.structures.B2StartLargeFileRequest;
import com.backblaze.b2.client.structures.B2UpdateBucketRequest;
import com.backblaze.b2.client.structures.B2UploadFileRequest;
import com.backblaze.b2.client.structures.B2UploadListener;
import com.backblaze.b2.client.structures.B2UploadPartUrlResponse;
import com.backblaze.b2.client.structures.B2UploadUrlResponse;

import java.io.Closeable;
import java.util.List;
import java.util.concurrent.ExecutorService;

/*****
 * B2StorageClient is the interface for performing B2 operations.
 * Be sure to close() any instance you create when you are done with it.
 * Try-with-resources can be very useful for that.
 *
 * Here are some design principles:
 *
 *   Be type-safe.
 *   Use different types for requests &amp; responses.
 *   Keep things final.
 *   Use builder classes to instantiate objects
 *     so it's easier to add new attributes.
 *     In addition, we provide convenience methods so the user
 *     doesn't have to use the builder, when the object only
 *     has one or two simple, obvious arguments.
 *
 *   Throw B2Exception, a checked exception, for most problems.
 *
 *  THREAD-SAFETY:  You may call any methods from any thread at any time.
 *    Calls may be blocked waiting for resources, but calling from multiple
 *    threads is safe.  (This assumes the Webifier is thread-safe, and the
 *    default implementation is.)
 *
 *  XXX: how much checking of requests should we do?  for the moment, i'm
 *       inclined to let the server do the bulk of the checking so
 *       (1) i don't have to repeat it here and
 *       (2) i don't have to rev. the client to let people take advantage of
 *           a wider range of inputs, such as more fileInfos.)
 *       we may very well revisit this.
 */
public interface B2StorageClient extends Closeable {

    /**
     * @return the accountId for this client.
     */
    String getAccountId() throws B2Exception;

    /**
     * @return an object for answering policy questions, such as
     * whether to upload a file as a small or large file.
     */
    B2FilePolicy getFilePolicy() throws B2Exception;

    /**
     * Creates a new bucket with the given request.
     *
     * @param request the request to create the bucket.
     * @return the newly created bucket.
     * @throws B2Exception if there's any trouble.
     */
    B2Bucket createBucket(B2CreateBucketRequest request) throws B2Exception;

    /**
     * Creates a new bucket with the specified bucketName and bucketType
     * and default settings for all of the other bucket attributes.
     *
     * @param bucketName the requested name for the bucket
     * @param bucketType the requested type of the bucket
     * @return the newly created bucket
     * @throws B2Exception if there's any trouble
     */
    default B2Bucket createBucket(String bucketName,
                                  String bucketType) throws B2Exception {
        return createBucket(B2CreateBucketRequest.builder(bucketName, bucketType).build());
    }

    /**
     * Creates a new application key.
     *
     * @param request the request to create the key.
     * @return the newly created key
     * @throws B2Exception if there's any trouble.
     */
    B2CreatedApplicationKey createKey(B2CreateKeyRequest request) throws B2Exception;

    /**
     * Returns an iterable whose iterator yields the application keys that
     * match the given request.
     *
     * It will automatically call B2 to get batches of answers as needed.  If there's
     * any trouble during hasNext() or next(), it will throw a B2RuntimeException
     * since Iterable&lt;&gt; doesn't allow checked exceptions to be thrown.
     *
     * @param request specifies which application keys to list.
     * @return a new iterable to iterate over fileVersions that match the given request.
     * @throws B2Exception if there's any trouble
     */
    B2ListKeysIterable applicationKeys(B2ListKeysRequest request) throws B2Exception;

    /**
     * Deletes the specified application key.
     *
     * @param request specifies the key to delete.
     * @return the deleted key
     * @throws B2Exception if there's any trouble.
     */
    B2ApplicationKey deleteKey(B2DeleteKeyRequest request) throws B2Exception;

    /**
     * Just like applicationKeys(request), except that it makes a request for all
     * application keys for the account.
     *
     * @return a new iterable to iterate over all of the keys in the specified account
     * @throws B2Exception if there's any trouble
     */
    default B2ListKeysIterable applicationKeys() throws B2Exception {
        return applicationKeys(B2ListKeysRequest.builder().build());
    }

    /**
     * @return the response from B2 with the listed buckets.
     * @throws B2Exception if there's any trouble.
     */
    default B2ListBucketsResponse listBuckets() throws B2Exception {
        // Note:
        //   Usually we nest all retryable code in one retryer loop
        //   so that we share the retry count and backoff behavior.
        //   in this case, doing that is awkward, so we're just
        //   calling getAccountId() which will do its own retry loop
        //   before calling listBuckets() with its retry loop.
        //   The AccountAuthorizationCache holds onto the accountId
        //   after the first time we capture it, so the only time
        //   getAccountId() might have to retry is if we haven't
        //   succeeding in authorizing yet.
        final String accountId = getAccountId();
        return listBuckets(B2ListBucketsRequest.builder(accountId).build());
    }

    /**
     * @return the response from B2 with the listed buckets using a listBucketsRequest object.
     * @throws B2Exception if there's any trouble.
     */
    B2ListBucketsResponse listBuckets(B2ListBucketsRequest listBucketsRequest) throws B2Exception;

    /**
     * @return a new list with all of the account's buckets for the b2 default buckets.
     * @throws B2Exception if there's any trouble.
     */
    default List<B2Bucket> buckets() throws B2Exception {
        return listBuckets().getBuckets();
    }

    /**
     * @return this account's bucket with the given name,
     * or null if this account doesn't have a bucket with the given name.
     * @throws B2Exception if there's any trouble.
     */
    default B2Bucket getBucketOrNullByName(String name) throws B2Exception {
        for (B2Bucket bucket : buckets()) {
            if (bucket.getBucketName().equals(name)) {
                return bucket;
            }
        }
        return null;
    }

    /**
     * Uploads the specified content as a normal B2 file.
     * The file must be smaller than the maximum file size (5 GB).
     *
     * @param request describes the content to upload and extra metadata about it.
     * @return the B2FileVersion that represents it.
     * @throws B2Exception if there's any trouble.
     */
    B2FileVersion uploadSmallFile(B2UploadFileRequest request) throws B2Exception;

    /**
     * Makes a copy of a file in the same bucket.
     * The new file must be smaller than the maximum file size (5 GB).
     *
     * @param request describes what file to copy, the range of bytes to copy, and how to handle file metadata.
     * @return The B2FileVersion of the new file.
     * @throws B2Exception if there's any trouble.
     */
    B2FileVersion copySmallFile(B2CopyFileRequest request) throws B2Exception;

    /**
     * Uploads the specified content as separate parts to form a B2 large file.
     *
     * @param request  describes the content to upload and extra metadata about it.
     * @param executor the executor to use for uploading parts in parallel.
     *                 the caller retains ownership of the executor and is
     *                 responsible for shutting it down.
     * @return the B2FileVersion that represents it.
     * @throws B2Exception if there's any trouble.
     */
    B2FileVersion uploadLargeFile(B2UploadFileRequest request,
                                  ExecutorService executor) throws B2Exception;

    /**
     * Uploads the specified content source as separate parts to form a B2 large file.
     *
     * This method assumes you have already called startLargeFile(). The return value
     * of that call needs to be passed into this method. However, this method will
     * currently call finish file.
     *
     * XXX: should we switch to letting the caller finish the large file?
     *
     * @param fileVersion The B2FileVersion for the large file getting stored.
     *                    This is the return value of startLargeFile().
     * @param contentSource The contentSource to upload.
     * @param uploadListenerOrNull The object that handles upload progress events.
     *                             This may be null if you do not need to be notified
     *                             of progress events.
     * @param executor The executor for uploading parts in parallel. The caller
     *                 retains ownership of the executor and is responsible for
     *                 shutting it down.
     * @return The fileVersion of the large file after it has been finished.
     * @throws B2Exception If there's trouble.
     */
    B2FileVersion storeLargeFileFromLocalContent(
            B2FileVersion fileVersion,
            B2ContentSource contentSource,
            B2UploadListener uploadListenerOrNull,
            ExecutorService executor) throws B2Exception;

    /**
     * Stores a large file, where storing each part may involve different behavior
     * or byte sources.
     *
     * For example, this method supports the use case of making a copy of a file
     * that mostly has not changed, and the user only wishes to upload the parts
     * that have changed. In this case partStorers would be a mix of
     * B2CopyingPartStorers and one or more B2UploadingPartStorers.
     *
     * Another use case would be reattempting an upload of a large file where some
     * parts have completed, and some haven't. In this case, partStorers would
     * be a mix of B2AlreadyStoredPartStorer and B2UploadingPartStorers.
     *
     * This method assumes you have already called startLargeFile(). The return value
     * of that call needs to be passed into this method. However, this method will
     * currently call finish file. Note that each part, whether copied or uploaded,
     * is still subject to the minimum part size.
     *
     * XXX: should we switch to letting the caller finish the large file?
     *
     * @param fileVersion The B2FileVersion for the large file getting stored.
     *                    This is the return value of startLargeFile().
     * @param partStorers The list of objects that know how to store the part
     *                    they are responsible for.
     * @param uploadListenerOrNull The object that handles upload progress events.
     *                             This may be null if you do not need to be notified
     *                             of progress events.
     * @param executor The executor for uploading parts in parallel. The caller
     *                 retains ownership of the executor and is responsible for
     *                 shutting it down.
     * @return The fileVersion of the large file after it has been finished.
     * @throws B2Exception If there's trouble.
     */
    B2FileVersion storeLargeFile(
            B2FileVersion fileVersion,
            List<B2PartStorer> partStorers,
            B2UploadListener uploadListenerOrNull,
            ExecutorService executor) throws B2Exception;

    /**
     * Verifies that the given fileVersion represents an unfinished large file
     * and that the specified content is compatible-enough with the information
     * in that B2FileVersion.  If it is, this will find the parts that haven't yet been
     * uploaded and upload them to finish the large file.
     *
     * XXX: describe "compatible-enough".  basically it's some sanity checks
     * such as the size of the content and the large-file-sha1 if it's available.
     * just enough to believe the request probably does go with the presented
     * fileVersion.  (Note that if you make up a bogus fileVersion, you're on
     * your own!)
     *
     * @param fileVersion describes the unfinished large file we want to finish.
     * @param request     describes the content we want to use to finish the large file
     * @param executor    the executor to use for uploading parts in parallel.
     *                    the caller retains ownership of the executor and is
     *                    responsible for shutting it down.
     * @return the B2FileVersion that represents the finished large file.
     * @throws B2Exception if there's any trouble.
     */
    B2FileVersion finishUploadingLargeFile(B2FileVersion fileVersion,
                                           B2UploadFileRequest request,
                                           ExecutorService executor) throws B2Exception;

    /**
     * Returns an iterable whose iterator yields the fileVersions that match the given request.
     *
     * It will automatically call B2 to get batches of answers as needed.  If there's
     * any trouble during hasNext() or next(), it will throw a B2RuntimeException
     * since Iterable&lt;&gt; doesn't allow checked exceptions to be thrown.
     *
     * @param request specifies which fileVersions to list.
     * @return a new iterable to iterate over fileVersions that match the given request.
     * @throws B2Exception if there's any trouble
     */
    B2ListFilesIterable fileVersions(B2ListFileVersionsRequest request) throws B2Exception;

    /**
     * Just like fileVersions(request), except that it makes a request for all
     * fileVersions in the specified bucket.
     *
     * @param bucketId the bucket whose fileVersions you want an Iterable for.
     * @return a new iterable to iterate over all of the fileVersions in the specified bucket.
     * @throws B2Exception if there's any trouble
     */
    default B2ListFilesIterable fileVersions(String bucketId) throws B2Exception {
        return fileVersions(B2ListFileVersionsRequest.builder(bucketId).setMaxFileCount(1000).build());
    }

    /**
     * Returns an iterable whose iterator yields the fileNames that match the given request.
     *
     * It will automatically call B2 to get batches of answers as needed.  If there's
     * any trouble during hasNext() or next(), it will throw a B2RuntimeException
     * since Iterable&lt;&gt; doesn't allow checked exceptions to be thrown.
     *
     * @param request specifies which fileVersions to list.
     * @return a new iterable to iterate over fileVersions that match the given request.
     * @throws B2Exception if there's any trouble
     */
    B2ListFilesIterable fileNames(B2ListFileNamesRequest request) throws B2Exception;

    /**
     * Just like fileNames(request), except that it makes a request for all
     * fileVersions in the specified bucket.
     *
     * @param bucketId the bucket whose fileNames you want an Iterable for.
     * @return a new iterable to iterate over all of the fileNames in the specified bucket.
     * @throws B2Exception if there's any trouble
     */
    default B2ListFilesIterable fileNames(String bucketId) throws B2Exception {
        return fileNames(B2ListFileNamesRequest.builder(bucketId).setMaxFileCount(1000).build());
    }

    /**
     * Returns an iterable whose iterator yields the fileVersions of large,
     * unfinished files that match the given request.
     *
     * It will automatically call B2 to get batches of answers as needed.  If there's
     * any trouble during hasNext() or next(), it will throw a B2RuntimeException
     * since Iterable&lt;&gt; doesn't allow checked exceptions to be thrown.
     *
     * @param request specifies which unfinished large files to list
     * @return a new iterable to iterate over fileVersions that match the given request.
     * @throws B2Exception if there's any trouble
     */
    B2ListFilesIterable unfinishedLargeFiles(B2ListUnfinishedLargeFilesRequest request) throws B2Exception;

    /**
     * Just like unfinishedLargeFiles(request), except that it makes a request for all
     * unfinished large files in the specified bucket.
     *
     * @param bucketId the bucket whose fileNames you want an Iterable for.
     * @return a new iterable to iterate over all of the unfinished large files in the specified bucket.
     * @throws B2Exception if there's any trouble
     */
    default B2ListFilesIterable unfinishedLargeFiles(String bucketId) throws B2Exception {
        return unfinishedLargeFiles(B2ListUnfinishedLargeFilesRequest.builder(bucketId).setMaxFileCount(100).build());
    }

    /**
     * Returns an iterable whose iterator yields the parts of large,
     * unfinished files that match the given request.
     *
     * It will automatically call B2 to get batches of answers as needed.  If there's
     * any trouble during hasNext() or next(), it will throw a B2RuntimeException
     * since Iterable&lt;&gt; doesn't allow checked exceptions to be thrown.
     *
     * @param request specifies which parts to list
     * @return a new iterable to iterate over parts that match the given request.
     * @throws B2Exception if there's any trouble
     */
    B2ListPartsIterable parts(B2ListPartsRequest request) throws B2Exception;

    /**
     * Just like parts(request), except that it makes a request for the parts
     * of the specified largeFileId.
     *
     * @param largeFileId the large file whose parts you want an Iterable for.
     * @return a new iterable to iterate over all of the parts of the specified file.
     * @throws B2Exception if there's any trouble
     */
    default B2ListPartsIterable parts(String largeFileId) throws B2Exception {
        return parts(B2ListPartsRequest.builder(largeFileId).setMaxPartCount(100).build());
    }

    /**
     * Cancels an unfinished large file.
     *
     * @param cancelRequest specifies which unfinsihed large file to cancel.
     * @throws B2Exception if there's any trouble.
     */
    void cancelLargeFile(B2CancelLargeFileRequest cancelRequest) throws B2Exception;

    /**
     * Just like cancelLargeFile(request), except that you only need to specify
     * the largeFileId for the unfinished large file you're trying to cancel.
     *
     * @param largeFileId specifies the unfinished, large file to cancel.
     * @throws B2Exception if there's any trouble.
     */
    default void cancelLargeFile(String largeFileId) throws B2Exception {
        cancelLargeFile(B2CancelLargeFileRequest.builder(largeFileId).build());
    }

    /**
     * Asks to download the specified file by id.
     *
     * @param request specifies the file and which part of the file to request.
     * @param handler if the server starts sending us the file, the headers and input
     *                stream are passed to the handler.  NOTE: if you get an exception
     *                while processing the stream, be sure to clean up anything you've
     *                created.  the handler may or may not be called again based on the
     *                exception.
     * @throws B2Exception if there's trouble with the request or if the handler throws
     *                     an exception.
     */
    void downloadById(B2DownloadByIdRequest request,
                      B2ContentSink handler) throws B2Exception;

    /**
     * Just like downloadById(request), but you only have to specify the fileId
     * instead of a request object.
     *
     * @param fileId  the id of the file you want to download.
     * @param handler the handler to process the data as its downloaded.
     * @throws B2Exception if there's any trouble.
     */
    default void downloadById(String fileId,
                              B2ContentSink handler) throws B2Exception {
        downloadById(B2DownloadByIdRequest.builder(fileId).build(), handler);
    }


    /**
     * Asks to download the specified file by bucket name and file name.
     *
     * @param request specifies the file and which part of the file to request.
     * @param handler if the server starts sending us the file, the headers and input
     *                stream are passed to the handler.  NOTE: if you get an exception
     *                while processing the stream, be sure to clean up anything you've
     *                created.  the handler may or may not be called again based on the
     *                exception.
     * @throws B2Exception if there's trouble with the request or if the handler throws
     *                     an exception.
     */
    void downloadByName(B2DownloadByNameRequest request,
                        B2ContentSink handler) throws B2Exception;

    /**
     * Just like downloadByName(request), but you only have to specify the
     * bucketName and the fileName instead of a request object.
     *
     * @param bucketName the name of the bucket you want to download from.
     * @param fileName   the name of the file you want to download.
     * @param handler    the handler to process the data as its downloaded.
     * @throws B2Exception if there's any trouble.
     */
    default void downloadByName(String bucketName,
                                String fileName,
                                B2ContentSink handler) throws B2Exception {
        downloadByName(B2DownloadByNameRequest.builder(bucketName, fileName).build(), handler);
    }


    /**
     * Deletes the specified file version.
     *
     * @param request specifies which fileVersion to delete.
     * @throws B2Exception if there's any trouble.
     */
    void deleteFileVersion(B2DeleteFileVersionRequest request) throws B2Exception;

    /**
     * Just like deleteFileVersion(request), except that the request is created from
     * the specified fileVersion.
     *
     * @param version specifies the fileVersion to delete.
     * @throws B2Exception if there's any trouble.
     */
    default void deleteFileVersion(B2FileVersion version) throws B2Exception {
        deleteFileVersion(version.getFileName(), version.getFileId());
    }

    /**
     * Just like deleteFileVersion(request), except that the request is created from
     * the specified fileName and fileId.
     *
     * @param fileName the name of the file to delete.
     * @param fileId   the id of the file to delete.
     * @throws B2Exception if there's any trouble.
     */
    default void deleteFileVersion(String fileName,
                                   String fileId) throws B2Exception {
        deleteFileVersion(B2DeleteFileVersionRequest.builder(fileName, fileId).build());
    }

    /**
     * Delete all files in bucket.
     *
     * @param bucketId the bucket whose file versions should be deleted
     * @throws B2Exception if there's any trouble. if there's trouble, it's undefined which
     * file versions have been deleted (if any) and which haven't (if any).
     */
    default void deleteAllFilesInBucket(String bucketId) throws B2Exception {
        for (B2FileVersion fileVersion: fileNames(bucketId)) {
           deleteFileVersion(fileVersion);
        }
    }

    /**
     * @param request specifies what the download authorization should allow.
     * @return a download authorization
     * @throws B2Exception if there's any trouble.
     */
    B2DownloadAuthorization getDownloadAuthorization(B2GetDownloadAuthorizationRequest request) throws B2Exception;

    /**
     * @param request specifies the file whose info to fetch.
     * @return a B2FileVersion object
     * @throws B2Exception if there's any trouble.
     */
    B2FileVersion getFileInfo(B2GetFileInfoRequest request) throws B2Exception;

    /**
     * Just like getFileInfo(request) except that the request is created
     * from the given fileId.
     *
     * @param fileId specifies the file whose info to fetch.
     * @return a B2FileVersion object
     * @throws B2Exception if there's any trouble.
     */
    default B2FileVersion getFileInfo(String fileId) throws B2Exception {
        return getFileInfo(B2GetFileInfoRequest.builder(fileId).build());
    }

    /**
     * @param request specifies the file whose info to fetch.
     * @return a B2FileVersion object
     * @throws B2Exception if there's any trouble.
     */
    B2FileVersion getFileInfoByName(B2GetFileInfoByNameRequest request) throws B2Exception;

    /**
     * Just like getFileInfoByName(request), but for the most recent version of file
     * with the specified fileName in the specified bucket.
     *
     * @param bucketName bucketName the name of the bucket containing the file you want info about.
     * @param fileName   fileName the name of the file whose info you're interested in.
     * @throws B2Exception if there's any trouble.
     */
    default B2FileVersion getFileInfoByName(String bucketName, String fileName) throws B2Exception {
        return getFileInfoByName(B2GetFileInfoByNameRequest.builder(bucketName, fileName).build());
    }

    /**
     * Hides the specified file.
     *
     * @param request specifies the file to hide
     * @return the fileVersion that's hiding the specified path
     * @throws B2Exception if there's any trouble.
     */
    B2FileVersion hideFile(B2HideFileRequest request) throws B2Exception;

    /**
     * Just like hideFile(request) except the request is created from the
     * given bucketId and fileName.
     *
     * @param bucketId the id of the bucket containing the file we want to hide
     * @param fileName the name of the file we want to hide
     * @return the fileVersion that's hiding the specified path
     * @throws B2Exception if there's any trouble.
     */
    default B2FileVersion hideFile(String bucketId,
                                   String fileName) throws B2Exception {
        return hideFile(B2HideFileRequest.builder(bucketId, fileName).build());
    }

    /**
     * Updates the specified bucket as described by the request.
     *
     * @param request specifies which bucket to update and how to update it.
     * @return the new state of the bucket
     * @throws B2Exception if there's any trouble.
     * @see <a href="https://www.backblaze.com/b2/docs/b2_update_bucket.html">b2_update_bucket</a>
     */
    B2Bucket updateBucket(B2UpdateBucketRequest request) throws B2Exception;

    /**
     * Deletes the specified bucket.
     * Note that it must be empty.
     *
     * @param request specifies the bucket to delete.
     * @return the deleted bucket
     * @throws B2Exception if there's any trouble.
     */
    B2Bucket deleteBucket(B2DeleteBucketRequest request) throws B2Exception;

    /**
     * Just like deleteBucket(request) except that the request is created
     * from the specified bucketId.
     *
     * @param bucketId the bucket to delete.
     * @return the deleted bucket
     * @throws B2Exception if there's any trouble.
     */
    default B2Bucket deleteBucket(String bucketId) throws B2Exception {
        return deleteBucket(B2DeleteBucketRequest.builder(bucketId).build());
    }


    /**
     * Returns the URL for downloading the file specified by the request.
     * Note that note all of the request will be represented in the URL.
     * For instance, the url will not contain authorization or range information
     * since we normally send those in the request headers.
     * <p>
     * This is useful for generating public URLs and as part of generating
     * signed download URLs.
     *
     * @param request specifies what to download.
     * @return a URL for fetching the file.
     * @throws B2Exception if there's any trouble.
     */
    String getDownloadByIdUrl(B2DownloadByIdRequest request) throws B2Exception;

    /**
     * Just like getDownloadByIdUrl(request) except that the request is created
     * from the given fileId.
     *
     * @param fileId the file whose download url we're interested in.
     * @return the URL
     * @throws B2Exception if there's any trouble.
     */
    default String getDownloadByIdUrl(String fileId) throws B2Exception {
        return getDownloadByIdUrl(B2DownloadByIdRequest.builder(fileId).build());
    }


    /**
     * Returns the URL for downloading the file specified by the request.
     * Note that note all of the request will be represented in the URL.
     * For instance, the url will not contain authorization or range information
     * since we normally send those in the request headers.
     * <p>
     * This is useful for generating public URLs and as part of generating
     * signed download URLs.
     *
     * @param request specifies what to download.
     * @return a URL for fetching the file.
     * @throws B2Exception if there's any trouble.
     */
    String getDownloadByNameUrl(B2DownloadByNameRequest request) throws B2Exception;

    /**
     * Just like getDownloadByIdUrl(request) except that the request is created
     * from the given bucketName and fileName.
     *
     * @param bucketName the name of the bucket that contains the desired file.
     * @param fileName   the name of the file whose download URL we want.
     * @return the URL
     * @throws B2Exception if there's any trouble.
     */
    default String getDownloadByNameUrl(String bucketName,
                                     String fileName) throws B2Exception {
        return getDownloadByNameUrl(B2DownloadByNameRequest.builder(bucketName, fileName).build());
    }

    /**
     * This method provides access to an account authorization structure.
     * The returned structure may have been cached.
     *
     * When possible, you should use other objects and helpers instead of
     * using the account authorization directly, so that the B2StorageClient
     * can properly invalidate the cached authorization, if any, in response
     * to interactions with the server.
     *
     * For instance, for downloading:
     *
     *   * to download files, use downloadByName() or downloadById().
     *
     *   * if you don't want to download directly, but need to give a download
     *     url to some other code, use getDownloadByIdUrl() or getDownloadByNameUrl().
     *
     *   * if neither of those will work for you because you need some other
     *     code to be able to form the download urls itself, you may need to
     *     getAccountAuthorization() and get the downloadUrl from it.
     *
     * For instance, when deciding whether to use uploadSmallFile() or uploadLargeFile(),
     * call getFilePolicy() and use the helper methods on the result.
     *
     * @return the account authorization, possibly from a cache.
     * @throws B2Exception if there's trouble getting the authorization.
     */
    B2AccountAuthorization getAccountAuthorization() throws B2Exception;

    /**
     * If there's a cached account authorization, this will flush the cache
     * so that the authorization will need to be reacquired the next time
     * it is needed.  (Keep in mind that another thread may need it and
     * reacquire it before this method even returns.)
     *
     * You should never need to call this unless you have called getAccountAuthorization()
     * and have gotten some kind of authorization exception when using the contents of
     * that authorization.  It's really a lot simpler for you if you never do that
     * and instead always do your work through B2StorageClient's APIs!
     */
    void invalidateAccountAuthorization();


    /**
     * This method allows the caller to get an upload url and authorization
     * token directly.
     *
     * Note that the SDK has lots of logic to upload files and getting upload URLs
     * and using them outside the SDK means that you need to handle lots of details
     * of uploading by yourself including:
     *   * retrying based on the types of errors you get, with proper backoff.
     *   * refreshing your account authorization when it expires.
     *   * reusing upload urls when possible
     *   * etc.
     *
     * When possible you should seriously consider using uploadSmallFile() and
     * uploadLargeFile() instead of reimplementing that logic.  If there's a
     * reason you can't use those methods, let us know.  Perhaps we can improve
     * things together to meet your needs.
     *
     * @param request specifies details about the desired upload url and credentials.
     * @return the response from the server.
     * @throws B2Exception if there's any trouble.
     */
    B2UploadUrlResponse getUploadUrl(B2GetUploadUrlRequest request) throws B2Exception;

    /**
     * This method allows the caller to get an upload url and authorization
     * token directly for uploading a large file part.
     *
     * Note that the SDK has lots of logic to upload files and getting upload URLs
     * and using them outside the SDK means that you need to handle lots of details
     * of uploading by yourself including:
     *   * retrying based on the types of errors you get, with proper backoff.
     *   * refreshing your account authorization when it expires.
     *   * reusing upload urls when possible
     *   * etc.
     *
     * When possible you should seriously consider using uploadSmallFile() and
     * uploadLargeFile() instead of reimplementing that logic.  If there's a
     * reason you can't use those methods, let us know.  Perhaps we can improve
     * things together to meet your needs.

     * @param request specifies details about the desired upload url and credentials.
     * @return the response from the server.
     * @throws B2Exception if there's any trouble.
     */
    B2UploadPartUrlResponse getUploadPartUrl(B2GetUploadPartUrlRequest request) throws B2Exception;

    /**
     * This method allows the caller to start a large file.
     *
     * Note that the SDK has lots of logic to upload large files and doing your
     * own uploading outside the SDK means that you need to handle lots of details
     * of uploading by yourself including:
     *   * retrying based on the types of errors you get, with proper backoff.
     *   * refreshing your account authorization when it expires.
     *   * reusing upload urls when possible
     *   * etc.
     *
     * When possible you should seriously consider using uploadLargeFile()
     * instead of reimplementing that logic.  If there's a reason you can't use
     * those methods, let us know.  Perhaps we can improve things together to
     * meet your needs.

     * @param request specifies details about the file to start.
     * @return the response from the server.
     * @throws B2Exception if there's any trouble.
     */
    B2FileVersion startLargeFile(B2StartLargeFileRequest request) throws B2Exception;

    /**
     * This method allows the caller to finish a large file.
     *
     * Note that the SDK has lots of logic to upload large files and doing your
     * own uploading outside the SDK means that you need to handle lots of details
     * of uploading by yourself including:
     *   * retrying based on the types of errors you get, with proper backoff.
     *   * refreshing your account authorization when it expires.
     *   * reusing upload urls when possible
     *   * etc.
     *
     * When possible you should seriously consider using uploadLargeFile()
     * or finishUploadingLargeFile() instead of reimplementing that logic.
     * If there's a reason you can't use those methods, let us know.  Perhaps
     * we can improve things together to meet your needs.

     * @param request specifies details about the file to finish.
     * @return the response from the server.
     * @throws B2Exception if there's any trouble.
     */
    B2FileVersion finishLargeFile(B2FinishLargeFileRequest request) throws B2Exception;


    /**
     * Closes this instance, releasing resources.
     */
    @Override
    void close();


    // just for tests!  really don't use it in real life.
    B2StorageClientWebifier getWebifier();
}
