/*
 * Copyright 2019, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.sample;

import com.backblaze.b2.client.B2ClientConfig;
import com.backblaze.b2.client.B2Sdk;
import com.backblaze.b2.client.B2StorageClient;
import com.backblaze.b2.client.contentSources.B2ContentSource;
import com.backblaze.b2.client.contentSources.B2ContentTypes;
import com.backblaze.b2.client.exceptions.B2Exception;
import com.backblaze.b2.client.structures.B2Bucket;
import com.backblaze.b2.client.structures.B2FileVersion;
import com.backblaze.b2.client.structures.B2UploadFileRequest;
import com.backblaze.b2.client.structures.B2UploadListener;
import com.backblaze.b2.client.webApiHttpClient.B2StorageHttpClientBuilder;
import com.backblaze.b2.util.B2ExecutorUtils;

import java.io.PrintWriter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.backblaze.b2.util.B2ExecutorUtils.createThreadFactory;

/**
 * This program uploads a large file using the UrlContentSource to
 * get the contents from a URL.
 *
 * Unlike other sample programs, it requires a lot of arguments,
 * so that it can work on non-trivial data.
 *
 * Also note that, unlike other sample programs, you must provide
 * applicationKey and applicationKeyId on the command line. This lets
 * us show how you can use B2ClientConfig.builder() to provide credentials
 * instead of having to put them in environment variables.
 */
public class UploadLargeFileFromUrl {

    private static final String USER_AGENT = "UploadLargeFileFromUrl";

    public static void main(String[] args) throws B2Exception {
        if (args.length != 6 && args.length != 7) {
            System.err.println("usage:");
            System.err.println("  java -classpath blahBlah " + UploadLargeFileFromUrl.class.getCanonicalName() +
                    " applicationKeyId applicationKey bucketName fileNameInB2 url contentLen [sha1]");
            System.exit(1);
        }
        final String appKeyId = args[0];
        final String appKey = args[1];
        final String bucketName = args[2];
        final String fileNameInB2 = args[3];
        final String url = args[4];
        final long contentLen = Long.parseLong(args[5]);
        final String sha1OrNull = (args.length >= 7) ? args[6] : null;

        final PrintWriter writer = new PrintWriter(System.out, true);
        final ExecutorService executor = Executors.newFixedThreadPool(10, createThreadFactory("sample-executor-%02d"));

        final B2ClientConfig config = B2ClientConfig.builder(appKeyId, appKey, USER_AGENT).build();
        try (final B2StorageClient client = B2StorageHttpClientBuilder.builder(config).build()) {
            mainGuts(writer, client, executor, bucketName, fileNameInB2, url, contentLen, sha1OrNull);
        } finally {
            B2ExecutorUtils.shutdownAndAwaitTermination(executor, 10, 10);
        }
    }

    @SuppressWarnings("SameParameterValue")
    private static void mainGuts(PrintWriter writer,
                                 B2StorageClient client,
                                 ExecutorService executor,
                                 String bucketName,
                                 String fileNameInB2,
                                 String url,
                                 long contentLen,
                                 String sha1OrNull) throws B2Exception {
        writer.println("Running with " + B2Sdk.getName() + " version " + B2Sdk.getVersion());

        B2Bucket bucket = client.getBucketOrNullByName(bucketName);
        if (bucket == null) {
            System.out.println("bucket " + bucketName + " doesn't exist");
            System.exit(1);
        }

        final B2UploadListener uploadListener = (progress) -> {
            final double percent = (100. * (progress.getBytesSoFar() / (double) progress.getLength()));
            writer.println(String.format("  progress(%3.2f, %s)", percent, progress.toString()));
        };


        final B2FileVersion file;
        {
            B2ContentSource source = new UrlContentSource.Builder(url, contentLen).setSha1OrNull(sha1OrNull).build();

            B2UploadFileRequest request = B2UploadFileRequest
                    .builder(bucket.getBucketId(), fileNameInB2, B2ContentTypes.B2_AUTO, source)
                    .setListener(uploadListener)
                    .build();
            file = client.uploadLargeFile(request, executor);
            writer.println("uploaded " + file);
        }
    }

}
