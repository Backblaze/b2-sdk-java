/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.sample;

import com.backblaze.b2.client.B2ClientConfig;
import com.backblaze.b2.client.B2StorageClient;
import com.backblaze.b2.client.contentHandlers.B2ContentFileWriter;
import com.backblaze.b2.client.contentHandlers.B2ContentMemoryWriter;
import com.backblaze.b2.client.contentHandlers.B2ContentSink;
import com.backblaze.b2.client.contentSources.B2ByteArrayContentSource;
import com.backblaze.b2.client.contentSources.B2ContentSource;
import com.backblaze.b2.client.contentSources.B2ContentTypes;
import com.backblaze.b2.client.contentSources.B2FileContentSource;
import com.backblaze.b2.client.exceptions.B2Exception;
import com.backblaze.b2.client.structures.B2Bucket;
import com.backblaze.b2.client.structures.B2BucketTypes;
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
import com.backblaze.b2.client.structures.B2LifecycleRule;
import com.backblaze.b2.client.structures.B2ListFileNamesRequest;
import com.backblaze.b2.client.structures.B2ListFileVersionsRequest;
import com.backblaze.b2.client.structures.B2TestMode;
import com.backblaze.b2.client.structures.B2UpdateBucketRequest;
import com.backblaze.b2.client.structures.B2UploadFileRequest;
import com.backblaze.b2.util.B2ByteRange;
import com.backblaze.b2.util.B2ExecutorUtils;
import com.backblaze.b2.util.B2Preconditions;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.backblaze.b2.util.B2ExecutorUtils.createThreadFactory;

public class B2Sample {

    // emailc57fbed9c0bb@test.backblaze.com
    private static final String accountId = "5857d33a7f65";
    private static final String applicationKey = "100e703fc514db5d5ea413c5ff37d42cc39bddea23";

    public static void main(String[] args) throws B2Exception {
        PrintWriter writer = new PrintWriter(System.out, true);

        final B2ClientConfig config = B2ClientConfig
                .builder(accountId, applicationKey, "B2Sample")
                .setMasterUrl("http://api.testb2.blaze:8180")  // XXX: this is for me to do local development of the SDK.  it defaults to the real URL.
                .setTestModeOrNull(B2TestMode.EXPIRE_SOME_ACCOUNT_AUTHORIZATION_TOKENS) // XXX: for testing
                .build();
        // convenience version that should be good for most people, most of the time.
        // final B2StorageClient client2 = B2StorageClient.builder(accountId, applicationKey).build();

        final ExecutorService executor = Executors.newFixedThreadPool(10, createThreadFactory("B2Sample-executor-%02d"));

        try (final B2StorageClient client = B2StorageClient.builder(config).build()) {
            mainGuts(writer, client, executor);
        } finally {
            B2ExecutorUtils.shutdownAndAwaitTermination(executor, 10, 10);
        }
    }

    private static void mainGuts(PrintWriter writer,
                                 B2StorageClient client,
                                 ExecutorService executor) throws B2Exception {

        final String bucketName = "sample-" + accountId;

        bigHeader(writer, "cleanup existing bucket, if any");
        deleteBucketIfAny(writer, client, bucketName);


        bigHeader(writer, "Create Bucket");

        final B2Bucket bucket;
        {
            B2CreateBucketRequest request = B2CreateBucketRequest
                    .builder(bucketName, B2BucketTypes.ALL_PRIVATE)
                    .build();
            bucket = client.createBucket(request);

            // maybe have this convenience:
            //   bucket = client.createBucket(bucketName, bucketType);
            // or
            //   bucket = client.createBucket(B2CreateBucketRequest.build(bucketName, bucketType));
        }
        final String bucketId = bucket.getBucketId();

        // list buckets.
        bigHeader(writer, "List Buckets");
        {
            for (B2Bucket scan : client.buckets()) {
                writer.println(" " + scan);
            }
        }

        // upload a file from the disk.
        //

        bigHeader(writer, "Upload files");
        final B2FileVersion file1;
        {
            final File fileOnDisk = new File("/tmp/B2Sample-uploadMe.txt");
            writeToFile(fileOnDisk, "hello world!\n".getBytes());

            try {
                final B2ContentSource source = B2FileContentSource.build(fileOnDisk);

                final String fileName = "demo/file.txt";
                B2UploadFileRequest request = B2UploadFileRequest
                        .builder(bucketId, fileName, B2ContentTypes.B2_AUTO, source)
                        .setCustomField("color", "blue")
                        .build();
                file1 = client.uploadFile(request, executor);
                writer.println("uploaded " + file1);
            } finally {
                //noinspection ResultOfMethodCallIgnored
                fileOnDisk.delete();
            }
        }

        // upload a file from memory.
        final B2FileVersion file2;
        {
            final B2ContentSource source = B2ByteArrayContentSource.build("this came from memory!".getBytes());
            final String fileName = "demo/memory.txt";
            B2UploadFileRequest request = B2UploadFileRequest
                    .builder(bucketId, fileName, B2ContentTypes.B2_AUTO, source)
                    .setCustomField("color", "red")
                    .build();
            file2 = client.uploadFile(request, executor);
            writer.println("uploaded " + file2);
        }

        // uploading a large file is about the same.
        // but you can also specify a TransferManager or something like that to manage the uploads.
        // do we want/need to expose the start/finish/listParts apis for large files?
        // maybe there should be an object that manages a large file upload.  maybe it's hidden.
        final B2FileVersion file3;
        {
            final File fileOnDisk = new File("/tmp/B2Sample-uploadMeLarge.txt");
            writeToFile(fileOnDisk, makeLargeFileBytes());

            final String fileName = "demo/large/superLarge.txt";
            final B2ContentSource source = B2FileContentSource.builder(fileOnDisk).build();

            B2UploadFileRequest request = B2UploadFileRequest
                    .builder(bucketId, fileName, B2ContentTypes.APPLICATION_OCTET, source)
                    .setCustomField("color", "green")
                    //.setTransferManager(xferMgr); // XXX: or an executorService? or params? or maybe they're on the client?
                    .build();
            file3 = client.uploadFile(request, executor);
            writer.println("uploaded " + file3);
        }

        //
        // list the parts of unfinished large files.  (XXX: needs to be a partially uploaded one!)
        //
        //bigHeader(writer, "List Parts of any unfinished large files");
        //for (B2FileVersion largeFileVersion : client.unfinishedLargeFiles(bucketId)) {
        //    System.out.println("====== unfinished large: " + largeFileVersion.getFileId() + " ======");
        //    for (B2Part part : client.parts(file3.getFileId())) {
        //        System.out.println("  " + part);
        //    }
        //}


        // upload some more files here so the folder structure is interesting.

        // list all file versions
        // the listBlahBuilders will have setters to control depth, delimiter, page size, total count, etc.
        // the object returned (and used only for iteration below) has a method to grab a "resumePoint"
        //   which can be given to a builder later to resume where we left off.  (one for resumeWithCurrent?
        //   and one for resumeWithNext? always resumeWithNext and you can save it from before calling next?)
        bigHeader(writer, "List File Versions");
        {
            final B2ListFileVersionsRequest request = B2ListFileVersionsRequest
                    .builder(bucketId)
                    .build();
            for (B2FileVersion version : client.fileVersions(request)) {
                writer.println("fileVersions: " + version);
            }
        }

        // list all file names
        bigHeader(writer, "List File Names");
        {
            final B2ListFileNamesRequest request = B2ListFileNamesRequest
                    .builder(bucketId)
                    .build();
            for (B2FileVersion version : client.fileNames(request)) {
                writer.println("fileNames: " + version);
            }
        }

        // list file versions in a given directory
        bigHeader(writer, "List File Versions in a single directory");
        {
            final B2ListFileVersionsRequest request = B2ListFileVersionsRequest
                    .builder(bucketId)
                    .setWithinFolder("demo/large/")
                    .build();
            for (B2FileVersion version : client.fileVersions(request)) {
                writer.println("fileNamesWithinFolder: " + version);
            }
        }

        // XXX: start a large file, so there's something to list and cancel below!

        // list unfinished large files.  and cancel them.
        bigHeader(writer, "List and cancel unfinished large files");
        {
            for (B2FileVersion largeFileVersion : client.unfinishedLargeFiles(bucketId)) {
                // long hand:
                B2CancelLargeFileRequest cancelRequest = B2CancelLargeFileRequest
                        .builder(largeFileVersion.getFileId())
                        .build();
                client.cancelLargeFile(cancelRequest);

                // or convenience method:
                //   client.cancelLargeFile(largeFileVersion.getFileId());
                // or:
                //   client.cancelLargeFile(largeFileVersion);
            }
        }

        // downloadById b2 file
        bigHeader(writer, "Download b2 file and print it");
        {
            final B2DownloadByIdRequest request = B2DownloadByIdRequest
                    .builder(file1.getFileId())
                    .build();
            final B2ContentSink handler = (headers, input) -> {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                while (true) {
                    int nextByte = (byte) input.read();
                    if (nextByte == -1) {
                        break;
                    }
                    outputStream.write(nextByte);
                }
                String s = outputStream.toString();
                writer.println("read from file [" + s + "]");
            };
            client.downloadById(request, handler);
        }

        // we provide a helpful handler implementation for saving to a file.
        // this is an example of using it with a download_by_name & a range.
        bigHeader(writer, "Download b2 file to disk");
        {
            final B2DownloadByNameRequest request = B2DownloadByNameRequest
                    .builder(bucketName, file1.getFileName())
                    .setRange(B2ByteRange.between(1, 4))
                    .build();
            final B2ContentFileWriter handler = B2ContentFileWriter
                    .builder(new File("/tmp/saveToThisLocalFile.txt"))
                    .setVerifySha1ByRereadingFromDestination(true)
                    .build();
            client.downloadByName(request, handler);
            writer.println("headers: " + handler.getHeadersOrNull());
        }

        // make sure that we can downloadById the whole large file.
        // this is more of a test than a sample.
        bigHeader(writer, "downloadById the large file.");
        {
            final B2ContentMemoryWriter handler = B2ContentMemoryWriter.build();
            client.downloadById(file3.getFileId(), handler);
            B2Preconditions.checkState(Arrays.equals(makeLargeFileBytes(), handler.getBytes()));
        }

        // delete file versions
        {
            final B2DeleteFileVersionRequest request = B2DeleteFileVersionRequest
                    .builder(file1.getFileName(), file1.getFileId())
                    .build();
            client.deleteFileVersion(request);

            // a more convenient way...
            //   client.deleteFileVersion(file1);

            // slightly less convenient
            //   client.deleteFileVersion(B2DeleteFileVersionRequest.builder(file1).build());
        }

        // get a download authorization.
        {
            final B2GetDownloadAuthorizationRequest request = B2GetDownloadAuthorizationRequest
                    .builder(bucketId, "dem", 3600)
                    .build();
            B2DownloadAuthorization auth = client.getDownloadAuthorization(request);
            writer.println("downloadAuth: " + auth);
        }

        // get file info.
        {
            final B2GetFileInfoRequest request = B2GetFileInfoRequest
                    .builder(file2.getFileId())
                    .build();
            B2FileVersion file2again = client.getFileInfo(request);
            writer.println("file2Again: " + file2again);

            // convenience version:
            //client.getFileInfo(file2.getFileId());
        }

        // hide file
        {
            B2HideFileRequest request = B2HideFileRequest
                    .builder(bucketId, file2.getFileName())
                    .build();
            client.hideFile(request);

            // convenience functions:
            //client.hideFile(file2.getBucketId(), file2.getFileName());
            //client.hideFile(file2); // ??
        }

        // update the bucket
        {
            final List<B2LifecycleRule> lifecycleRules = new ArrayList<>();
            final Map<String, String> bucketInfo = new TreeMap<>();

            // i'm inclined to *always* require ifRevisionIs.  if we want to be flexible,
            // i can make it possible to opt out.
            final B2UpdateBucketRequest request = B2UpdateBucketRequest
                    .builder(bucket)
                    .setBucketType(B2BucketTypes.ALL_PUBLIC)
                    .setLifecycleRules(lifecycleRules)
                    .setBucketInfo(bucketInfo)
                    .build();
            B2Bucket bucketAgain = client.updateBucket(request);
            writer.println("bucketAgain: " + bucketAgain);
        }

        // delete all files with a given filename
        {
            final String fileNameToDelete = file1.getFileName();
            B2ListFileVersionsRequest request = B2ListFileVersionsRequest
                    .builder(bucketId)
                    .setStartFileName(fileNameToDelete)
                    .setPrefix(fileNameToDelete)
                    .build();
            for (B2FileVersion version : client.fileVersions(request)) {
                if (version.getFileName().equals(fileNameToDelete)) {
                    client.deleteFileVersion(version);
                } else {
                    break;
                }
            }
        }


        // delete all files, so we can delete the bucket.
        {
            // convenience version of fileVersions.
            for (B2FileVersion version : client.fileVersions(bucketId)) {
                client.deleteFileVersion(version);
            }
        }

        // delete the bucket
        {
            final B2DeleteBucketRequest request = B2DeleteBucketRequest
                    .builder(bucketId)
                    .build();
            client.deleteBucket(request);

            // or convenience method:
            // client.deleteBucket(bucketId);
        }
    }

    private static void deleteBucketIfAny(PrintWriter writer,
                                          B2StorageClient client,
                                          String bucketName) throws B2Exception {
        for (B2Bucket bucket : client.buckets()) {
            if (bucketName.equals(bucket.getBucketName())) {
                final String bucketId = bucket.getBucketId();
                deleteAllFilesInBucket(writer, client, bucketId);
                client.deleteBucket(bucketId);
                writer.println("deleted bucket " + bucketName + " (" + bucketId + ")");
            }
        }
    }

    private static void deleteAllFilesInBucket(PrintWriter writer,
                                               B2StorageClient client,
                                               String bucketId) throws B2Exception {
        int numDeleted = 0;
        for (B2FileVersion version : client.fileVersions(bucketId)) {
            client.deleteFileVersion(version);
            numDeleted++;
            writer.println("  deleted " + version);
        }
        writer.println("deleted " + numDeleted + " files");
    }

    private static void writeToFile(File file,
                                    byte[] bytes) throws B2Exception {
        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write(bytes);
        } catch (IOException e) {
            throw new RuntimeException("couldn't write file '" + file + "': " + e.getMessage(), e);
        }
    }

    /**
     * @return a new byte[] containing enough bytes to count as a large file in dev and staging.
     *  XXX: this isn't enough bytes to be a large file in production.  :(
     */
    private static byte[] makeLargeFileBytes() {
        final int MB = 1000 * 1000;
        final byte[] bytes = new byte[2 * MB];

        int iByte = 0;
        for (int iPart = 0; iPart < 2; iPart++) {
            final int baseByte = (iPart == 0) ? 0 : 128;
            for (int iByteInPart = 0; iByteInPart < MB; iByteInPart++) {
                bytes[iByte] = (byte) (baseByte + (iByteInPart % 128));
                iByte++;
            }
        }
        return bytes;
    }

    private static void bigHeader(PrintWriter writer,
                                  String title) {
        writer.println("########################################################################");
        writer.println("#");
        writer.println("# " + title);
        writer.println("#");
        writer.println("########################################################################");
    }
}
