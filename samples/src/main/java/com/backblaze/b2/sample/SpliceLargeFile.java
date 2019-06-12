/*
 * Copyright 2019, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.sample;

import com.backblaze.b2.client.B2ClientConfig;
import com.backblaze.b2.client.B2CopyingPartStorer;
import com.backblaze.b2.client.B2UploadingPartStorer;
import com.backblaze.b2.client.B2PartStorer;
import com.backblaze.b2.client.B2StorageClient;
import com.backblaze.b2.client.contentSources.B2ByteArrayContentSource;
import com.backblaze.b2.client.exceptions.B2Exception;
import com.backblaze.b2.client.structures.B2FileVersion;
import com.backblaze.b2.client.structures.B2StartLargeFileRequest;
import com.backblaze.b2.client.structures.B2UploadListener;
import com.backblaze.b2.client.webApiHttpClient.B2StorageHttpClientBuilder;
import com.backblaze.b2.util.B2ByteRange;
import com.backblaze.b2.util.B2ExecutorUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * This sample creates a copy of a file with 5MB of zeroes inserted into it.
 *
 * The first 10MB of the new file are the same as the first 10MB of the
 * source file.
 *
 * The next 5MB of the new file are zeroes.
 *
 * The bytes of the new file from byte 15,000,001 and on are identical to the bytes
 * of the source from byte 10,000,001 and on.
 */
public class SpliceLargeFile {

    private static final long SPLICE_INDEX = 10000000;
    private static final String USER_AGENT = "SpliceZerosIntoLargeFile";

    public static void main(String[] args) throws B2Exception {
        if (args.length != 4) {
            System.err.println("usage:");
            System.err.println("  java -classpath blahBlah " + SpliceLargeFile.class.getCanonicalName() +
                    " applicationKeyId applicationKey bucketId sourceFileId");
            System.exit(1);
        }
        final String appKeyId = args[0];
        final String appKey = args[1];
        final String bucketId = args[2];
        final String sourceFileId = args[3];

        // A thread pool that allows up to 3 parts to be stored concurrently.
        final ExecutorService executor = Executors.newFixedThreadPool(3);

        final B2ClientConfig config = B2ClientConfig.builder(appKeyId, appKey, USER_AGENT).build();
        try (final B2StorageClient client = B2StorageHttpClientBuilder.builder(config).build()) {

            // Start a new large file. The B2LargeFileStorer requires a fileId for the large file
            // to be established before it is constructed.
            final B2StartLargeFileRequest startLargeFileRequest = B2StartLargeFileRequest.builder(
                    bucketId, "b2-sdk-sample-splice", "x/b2-auto").build();
            final B2FileVersion largeFileVersion = client.startLargeFile(startLargeFileRequest);

            // The list of B2PartStorers. These are objects that each know how to store a single
            // part. Note specifically that when assembling a new large file, each part can come
            // from a different source of bytes. In this case, the middle part (part 2) is coming
            // from a 5MB byte array. Parts 1 and 3 collectively represent the entirety of an
            // existing file.
            final List<B2PartStorer> partContentSources = new ArrayList<>();

            partContentSources.add(new B2CopyingPartStorer(
                    1, sourceFileId, B2ByteRange.between(0, SPLICE_INDEX-1)));
            partContentSources.add(new B2UploadingPartStorer(
                    2, B2ByteArrayContentSource.build(new byte[5000000])));
            partContentSources.add(new B2CopyingPartStorer(
                    3, sourceFileId, B2ByteRange.startAt(SPLICE_INDEX)));

            // Store the large file. This method will finish the large file and return the
            // B2FileVersion for the new large file.
            client.storeLargeFile(largeFileVersion, partContentSources, B2UploadListener.noopListener(), executor);
        } finally {
            B2ExecutorUtils.shutdownAndAwaitTermination(executor, 10, 10);
        }
    }



}

