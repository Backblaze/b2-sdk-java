/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */

package com.backblaze.b2.client;

import com.backblaze.b2.client.contentHandlers.B2ContentSink;
import com.backblaze.b2.client.exceptions.B2Exception;
import com.backblaze.b2.client.structures.B2Bucket;
import com.backblaze.b2.client.structures.B2CancelLargeFileRequest;
import com.backblaze.b2.client.structures.B2CreateBucketRequest;
import com.backblaze.b2.client.structures.B2DeleteBucketRequest;
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
import com.backblaze.b2.client.structures.B2ListFileVersionsRequest;
import com.backblaze.b2.client.structures.B2ListPartsRequest;
import com.backblaze.b2.client.structures.B2ListUnfinishedLargeFilesRequest;
import com.backblaze.b2.client.structures.B2UpdateBucketRequest;
import com.backblaze.b2.client.structures.B2UploadFileRequest;

import java.io.Closeable;
import java.util.List;
import java.util.concurrent.ExecutorService;

/*****
 * B2StorageClient is the interface for performing B2 operations.
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
    String getAccountId();

    /**
     * @return an object for answering policy questions, such as
     *         whether to upload a file as a small or large file.
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
    default B2Bucket createBucket(String bucketName, String bucketType) throws B2Exception {
        return createBucket(B2CreateBucketRequest.builder(bucketName, bucketType).build());
    }

    /**
     * @return the response from B2 with the listed buckets.
     * @throws B2Exception if there's any trouble.
     */
    B2ListBucketsResponse listBuckets() throws B2Exception;

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
     *         or null if this account doesn't have a bucket with the given name.
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
     * Uploads the specified content as separate parts to form a B2 large file.
     *
     * @param request describes the content to upload and extra metadata about it.
     * @param executor the executor to use for uploading parts in parallel.
     *                 the caller retains ownership of the executor and is
     *                 responsible for shutting it down.
     * @return the B2FileVersion that represents it.
     * @throws B2Exception if there's any trouble.
     */
    B2FileVersion uploadLargeFile(B2UploadFileRequest request,
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
     * @param request describes the content we want to use to finish the large file
     * @param executor the executor to use for uploading parts in parallel.
     *                 the caller retains ownership of the executor and is
     *                 responsible for shutting it down.
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
     * @param fileId the id of the file you want to download.
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
     * @param fileName the name of the file you want to download.
     * @param handler the handler to process the data as its downloaded.
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
     * @param fileId the id of the file to delete.
     * @throws B2Exception if there's any trouble.
     */
    default void deleteFileVersion(String fileName, String fileId) throws B2Exception {
        deleteFileVersion(B2DeleteFileVersionRequest.builder(fileName, fileId).build());
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
    default B2FileVersion hideFile(String bucketId, String fileName) throws B2Exception {
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
     * Closes this instance, releasing resources.
     */
    @Override
    void close();


    // just for tests!  really don't use it in real life.
    B2StorageClientWebifier getWebifier();
}
