/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.sample;

import com.backblaze.b2.client.B2ClientConfig;
import com.backblaze.b2.client.B2StorageClient;
import com.backblaze.b2.client.contentSources.B2ContentSource;
import com.backblaze.b2.client.contentSources.B2ContentTypes;
import com.backblaze.b2.client.contentSources.B2FileContentSource;
import com.backblaze.b2.client.exceptions.B2Exception;
import com.backblaze.b2.client.structures.B2Bucket;
import com.backblaze.b2.client.structures.B2BucketTypes;
import com.backblaze.b2.client.structures.B2FileVersion;
import com.backblaze.b2.client.structures.B2ListFileVersionsRequest;
import com.backblaze.b2.client.structures.B2UploadFileRequest;
import com.backblaze.b2.client.webApiHttpClient.B2StorageClientBuilder;
import com.backblaze.b2.util.B2ExecutorUtils;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class B2SimpleSample {
    public static void main(String[] args) throws B2Exception {
        ExecutorService executor = Executors.newFixedThreadPool(6, B2ExecutorUtils.createThreadFactory("B2SimpleSample-%d"));
        try {
            mainGuts(executor);
        } finally {
            B2ExecutorUtils.shutdownAndAwaitTermination(executor, 10, 10);
        }
    }

    private static void mainGuts(ExecutorService executor) throws B2Exception {
        // emailc57fbed9c0bb@test.backblaze.com
        final String accountId = "5857d33a7f65";
        final String applicationKey = "100e703fc514db5d5ea413c5ff37d42cc39bddea23";
        final String bucketName = "sample-" + accountId;

        // convenience version that should be good for most people, most of the time.
        //final B2StorageClient client = B2StorageClient.builder(accountId, applicationKey, "B2SimpleSample/0.0.1").build();

        // the version i need to run locally against my own tomcat
        final B2ClientConfig config = B2ClientConfig
                .builder(accountId, applicationKey, "B2SimpleSample/0.0.1")
                .setMasterUrl("http://api.testb2.blaze:8180")  // XXX: this is for me to do local development of the SDK.  it defaults to the real URL.
                .build();
        final B2StorageClient client = B2StorageClientBuilder.builder(config).build();


        final B2Bucket bucket = client.createBucket(bucketName, B2BucketTypes.ALL_PRIVATE);
        final String bucketId = bucket.getBucketId();


        String srcFileName = "/tmp/fido.jpg";
        String folderName = "bigdogs/cute_puppies/";
        String dstFileName = "bigdogs/cute_puppies/fido.jpg";

        final B2ContentSource source = B2FileContentSource.build(new File(srcFileName));

        B2UploadFileRequest request = B2UploadFileRequest
                .builder(bucketId, dstFileName, B2ContentTypes.B2_AUTO, source)
                .build();
        client.uploadFile(request, executor);


        // list file versions in a given directory
        final B2ListFileVersionsRequest listRequest = B2ListFileVersionsRequest
                .builder(bucketId)
                .setWithinFolder(folderName)
                .build();
        int i = 0;
        for (B2FileVersion version : client.fileVersions(listRequest)) {
            System.out.println("File " + i + " is named " + version.getFileName() +
            " and has " + version.getContentLength() + " bytes.");
            i++;
        }
    }
}
