/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.sample;

import com.backblaze.b2.client.B2Sdk;
import com.backblaze.b2.client.B2StorageClient;
import com.backblaze.b2.client.B2StorageClientFactory;
import com.backblaze.b2.client.contentHandlers.B2ContentFileWriter;
import com.backblaze.b2.client.contentHandlers.B2ContentMemoryWriter;
import com.backblaze.b2.client.contentSources.B2ByteArrayContentSource;
import com.backblaze.b2.client.contentSources.B2ContentSource;
import com.backblaze.b2.client.contentSources.B2ContentTypes;
import com.backblaze.b2.client.contentSources.B2FileContentSource;
import com.backblaze.b2.client.exceptions.B2Exception;
import com.backblaze.b2.client.structures.B2AccountAuthorization;
import com.backblaze.b2.client.structures.B2ApplicationKey;
import com.backblaze.b2.client.structures.B2Bucket;
import com.backblaze.b2.client.structures.B2BucketTypes;
import com.backblaze.b2.client.structures.B2Capabilities;
import com.backblaze.b2.client.structures.B2CreateKeyRequest;
import com.backblaze.b2.client.structures.B2CreatedApplicationKey;
import com.backblaze.b2.client.structures.B2DeleteKeyRequest;
import com.backblaze.b2.client.structures.B2DownloadAuthorization;
import com.backblaze.b2.client.structures.B2DownloadByNameRequest;
import com.backblaze.b2.client.structures.B2FileVersion;
import com.backblaze.b2.client.structures.B2GetDownloadAuthorizationRequest;
import com.backblaze.b2.client.structures.B2LifecycleRule;
import com.backblaze.b2.client.structures.B2ListFileVersionsRequest;
import com.backblaze.b2.client.structures.B2Part;
import com.backblaze.b2.client.structures.B2UpdateBucketRequest;
import com.backblaze.b2.client.structures.B2UploadFileRequest;
import com.backblaze.b2.client.structures.B2UploadListener;
import com.backblaze.b2.util.B2ByteRange;
import com.backblaze.b2.util.B2ExecutorUtils;
import com.backblaze.b2.util.B2Preconditions;

import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.backblaze.b2.util.B2ExecutorUtils.createThreadFactory;

public class B2Sample {

    private static final String USER_AGENT = "B2Sample";

    public static void main(String[] args) throws B2Exception {
        PrintWriter writer = new PrintWriter(System.out, true);

        final ExecutorService executor = Executors.newFixedThreadPool(10, createThreadFactory("B2Sample-executor-%02d"));

        try (final B2StorageClient client = B2StorageClientFactory.createDefaultFactory().create(USER_AGENT)) {
            mainGuts(writer, client, executor);
        } finally {
            B2ExecutorUtils.shutdownAndAwaitTermination(executor, 10, 10);
        }
    }

    private static void mainGuts(PrintWriter writer,
                                 B2StorageClient client,
                                 ExecutorService executor) throws B2Exception {
        writer.println("Running with " + B2Sdk.getName() + " version " + B2Sdk.getVersion());

        final String bucketName = "sample-" + client.getAccountId();

        final B2UploadListener uploadListener = (progress) -> {
            final double percent = (100. * (progress.getBytesSoFar() / (double) progress.getLength()));
            writer.println(String.format("  progress(%3.2f, %s)", percent, progress.toString()));
        };

        bigHeader(writer, "create application key");
        final String applicationKeyId;
        {
            final Set<String> capabilities = new HashSet<>();
            capabilities.add(B2Capabilities.LIST_BUCKETS);
            capabilities.add(B2Capabilities.READ_FILES);

            final B2CreatedApplicationKey applicationKey =
                    client.createKey(
                            B2CreateKeyRequest.builder(capabilities, "testKey").build()
                    );
            applicationKeyId = applicationKey.getApplicationKeyId();
            writer.println("key id: " + applicationKey.getApplicationKeyId() + "   key: " + applicationKey.getApplicationKey());
        }

        bigHeader(writer, "list application keys");
        for (B2ApplicationKey key : client.applicationKeys()) {
            writer.println("key id: " + key.getApplicationKeyId() + "   capabilities: " + key.getCapabilities());
        }

        bigHeader(writer, "delete application key");
        {
            client.deleteKey(B2DeleteKeyRequest.builder(applicationKeyId).build());
        }

        bigHeader(writer, "cleanup existing bucket, if any");
        deleteBucketIfAny(writer, client, bucketName);


        bigHeader(writer, "Create Bucket");
        final B2Bucket bucket = client.createBucket(bucketName, B2BucketTypes.ALL_PRIVATE);
        final String bucketId = bucket.getBucketId();

        // list buckets.
        bigHeader(writer, "List Buckets");
        for (B2Bucket scan : client.buckets()) {
            writer.println(" " + scan);
        }

        // create a file on disk that we can upload.
        final File fileOnDisk = new File("/tmp/B2Sample-uploadMe.txt");
        writeToFile(fileOnDisk, "hello world!\n".getBytes());

        // upload a file from the disk.
        bigHeader(writer, "Upload file from disk");
        final B2FileVersion file1;
        {
            try {
                final B2ContentSource source = B2FileContentSource.build(fileOnDisk);

                final String fileName = "demo/file.txt";
                B2UploadFileRequest request = B2UploadFileRequest
                        .builder(bucketId, fileName, B2ContentTypes.B2_AUTO, source)
                        .setCustomField("color", "blue")
                        .setListener(uploadListener)
                        .build();
                file1 = client.uploadSmallFile(request);
                writer.println("uploaded " + file1);
            } finally {
                //noinspection ResultOfMethodCallIgnored
                fileOnDisk.delete();
            }
        }

        // upload a file from memory.
        bigHeader(writer, "Upload file from memory");
        final B2FileVersion file2;
        {
            final B2ContentSource source = B2ByteArrayContentSource.build("this came from memory!".getBytes());
            final String fileName = "demo/memory.txt";
            B2UploadFileRequest request = B2UploadFileRequest
                    .builder(bucketId, fileName, B2ContentTypes.B2_AUTO, source)
                    .setCustomField("color", "red")
                    .build();
            file2 = client.uploadSmallFile(request);
            writer.println("uploaded " + file2);
        }

        // create the an array with enough bytes for a large file
        bigHeader(writer, "create large file in memory");
        final byte[] largeFileBytes = makeLargeFileBytes();

        // uploading a large file is about the same.
        bigHeader(writer, "Upload large (10MB) file");
        final B2FileVersion file3;
        {
            final File largeFileOnDisk = new File("/tmp/B2Sample-uploadMeLarge.txt");
            writeToFile(largeFileOnDisk, largeFileBytes);

            final String fileName = "demo/large/superLarge.txt";
            final B2ContentSource source = B2FileContentSource.builder(largeFileOnDisk).build();

            B2UploadFileRequest request = B2UploadFileRequest
                    .builder(bucketId, fileName, B2ContentTypes.APPLICATION_OCTET, source)
                    .setCustomField("color", "green")
                    .setListener(uploadListener)
                    .build();
            file3 = client.uploadLargeFile(request, executor);
            writer.println("uploaded " + file3);
        }

        // list the parts of unfinished large files.  (XXX: needs to be a partially uploaded one!)
        bigHeader(writer, "list parts of any unfinished large files");
        for (B2FileVersion largeFileVersion : client.unfinishedLargeFiles(bucketId)) {
            System.out.println("====== unfinished large: " + largeFileVersion.getFileId() + " ======");
            for (B2Part part : client.parts(file3.getFileId())) {
                System.out.println("  " + part);
            }
        }

        // upload a huge file but use the small file API.
        bigHeader(writer, "Upload huge (2GB+) file using small file API");
        final File largeFileOnDisk;
        try {
            final String fileName = "demo/large/huge2GBfile.txt";
            largeFileOnDisk = B2Sample.makeHugeFile(fileName);
            final B2ContentSource source = B2FileContentSource.builder(largeFileOnDisk).build();

            B2UploadFileRequest request = B2UploadFileRequest
                    .builder(bucketId, fileName, B2ContentTypes.APPLICATION_OCTET, source)
                    .setCustomField("color", "green")
                    .setListener(uploadListener)
                    .build();
            B2FileVersion file4 = client.uploadSmallFile(request);
            writer.println("uploaded " + file4);
        } catch (IOException e) {
            writer.println("test failed: "+e.getMessage());
        }

        // upload some more files here so the folder structure is interesting.
        // XXX...

        // list all file versions
        // the listBlahBuilders will have setters to control depth, delimiter, page size, total count, etc.
        // the object returned (and used only for iteration below) has a method to grab a "resumePoint"
        //   which can be given to a builder later to resume where we left off.  (one for resumeWithCurrent?
        //   and one for resumeWithNext? always resumeWithNext and you can save it from before calling next?)
        bigHeader(writer, "list file versions");
        for (B2FileVersion version : client.fileVersions(bucketId)) {
            writer.println("fileVersion: " + version);
        }


        // list all file names
        bigHeader(writer, "list file names");
        for (B2FileVersion version : client.fileNames(bucketId)) {
            writer.println("fileName: " + version);
        }

        // list file versions in a given directory
        bigHeader(writer, "list file versions in a single directory");
        {
            final B2ListFileVersionsRequest request = B2ListFileVersionsRequest
                    .builder(bucketId)
                    .setWithinFolder("demo/large/")
                    .build();
            for (B2FileVersion version : client.fileVersions(request)) {
                writer.println("fileNameWithinFolder: " + version);
            }
        }

        // XXX: start a large file, so there's something to list and cancel below!

        // list unfinished large files.  and cancel them.
        bigHeader(writer, "list and cancel unfinished large files");
        for (B2FileVersion largeFileVersion : client.unfinishedLargeFiles(bucketId)) {
            client.cancelLargeFile(largeFileVersion.getFileId());
        }

        // downloadById b2 file into memory.
        bigHeader(writer, "Download b2 file and print it");
        {
            B2ContentMemoryWriter sink = B2ContentMemoryWriter.build();
            client.downloadById(file1.getFileId(), sink);
            writer.println("read from file [" + new String(sink.getBytes()) + "]");
        }

        // we provide a helpful handler implementation for saving to a file.
        // this is an example of using it with a download_by_name & a range.
        bigHeader(writer, "download b2 file to disk");
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
            B2Preconditions.checkState(Arrays.equals(largeFileBytes, handler.getBytes()));
        }

        // delete file versions
        client.deleteFileVersion(file1);

        // get a download authorization.
        {
            final B2GetDownloadAuthorizationRequest request = B2GetDownloadAuthorizationRequest
                    .builder(bucketId, "dem", 3600)
                    .build();
            B2DownloadAuthorization auth = client.getDownloadAuthorization(request);
            writer.println("downloadAuth: " + auth);
        }

        // get file info.
        B2FileVersion file2again = client.getFileInfo(file2.getFileId());
        writer.println("file2Again: " + file2again);

        // get file info by name.
        B2FileVersion file2ByName = client.getFileInfoByName(bucket.getBucketName(), file2.getFileName());
        writer.println("file2ByName: " + file2ByName);

        if (!file2again.equals(file2ByName)) {
            throw new RuntimeException("file!");
        }

        // hide file
        client.hideFile(bucketId, file2.getFileName());

        // update the bucket
        {
            final List<B2LifecycleRule> lifecycleRules = new ArrayList<>();
            final Map<String, String> bucketInfo = new TreeMap<>();

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
        for (B2FileVersion version : client.fileVersions(bucketId)) {
            client.deleteFileVersion(version);
        }

        // delete the bucket
        client.deleteBucket(bucketId);


        // exercise account authorization.
        bigHeader(writer, "exercise getAccountAuthorization() & invalidateAccountAuthorization().");
        {
            // do an explicit getAccountAuthorization.
            // you should rarely need to do this.
            // we're just doing it here to exercise it.
            final B2AccountAuthorization auth = client.getAccountAuthorization();
            final B2AccountAuthorization auth2 = client.getAccountAuthorization();
            client.invalidateAccountAuthorization();
            final B2AccountAuthorization auth3 = client.getAccountAuthorization();

            writer.println("auth = " + auth);
            writer.println("auth3 = " + auth3);

            B2Preconditions.checkState(auth == auth2, "same object because it's cached.");
            B2Preconditions.checkState(auth != auth3, "different object because we cleared the cache.");
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
        final byte[] bytes = new byte[10 * MB];

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

    /**
     * Create a file large enough to be considered a large file in production;
     * @return File too big to be completely loaded into a byte array
     */


    private static File makeHugeFile(String fileNamePrefix) throws IOException {
        final long fileSize = (long)Integer.MAX_VALUE + 1000*1000;
        final byte[] bytes = {0xD, 0xE, 0xA, 0xD, 0xB, 0xE, 0xE, 0xF};
        final File tempFile = File.createTempFile(fileNamePrefix, ".tmp");
        final RandomAccessFile randomAccessFile= new RandomAccessFile(tempFile, "rw");
        // write bytes at end to size the file
        long seekTo = fileSize - bytes.length;
        randomAccessFile.seek(seekTo);
        randomAccessFile.write(bytes);
        randomAccessFile.close();
        tempFile.deleteOnExit();
        return tempFile;
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
