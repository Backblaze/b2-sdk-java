/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client;

import com.backblaze.b2.client.contentSources.B2ContentTypes;
import com.backblaze.b2.client.structures.B2AccountAuthorization;
import com.backblaze.b2.client.structures.B2Bucket;
import com.backblaze.b2.client.structures.B2BucketTypes;
import com.backblaze.b2.client.structures.B2FileVersion;
import com.backblaze.b2.client.structures.B2LifecycleRule;
import com.backblaze.b2.client.structures.B2Part;
import com.backblaze.b2.client.structures.B2UploadPartUrlResponse;
import com.backblaze.b2.client.structures.B2UploadUrlResponse;
import com.backblaze.b2.util.B2Collections;
import com.backblaze.b2.util.B2Sha1;

public class B2TestHelpers {
    public static final String SAMPLE_SHA1 = "da39a3ee5e6b4b0d3255bfef95601890afd80709";

    /**
     * @return a bucketId based on the provided number.  it's a bogus id, but readable.
     */
    public static String bucketId(int i) {
        return "bucket" + i;
    }

    /**
     * @return a bucketName based on the provided number.
     */
    public static String bucketName(int i) {
        return "bucketName" + i;
    }

    /**
     * @param i a number used to distinguish between different authorizations.
     * @return a new B2AccountAuthorization, with bogus-but-recognizable values based on i.
     */
    public static B2AccountAuthorization makeAuth(int i) {
        return new B2AccountAuthorization(Integer.toString(i),
                "accountToken" + i,
                "apiUrl" + i,
                "downloadUrl" + i,
                i * 1000,
                i * 100);
    }

    /**
     * @param bucketId the id of the bucket
     * @param i a number used to distinguish between different responses.
     * @return a new B2UploadUrlResponse with bogus-but-recognizable values based on i.
     */
    public static B2UploadUrlResponse uploadUrlResponse(String bucketId,
                                                        int i) {
        return new B2UploadUrlResponse(
                bucketId,
                "uploadUrl" + i,
                "downloadToken" + i);
    }

    /**
     * @param largeFileId the id of the large file whose part we want to upload
     * @param i a number used to distinguish between different responses.
     * @return a new B2UploadPartUrlResponse with bogus-but-recognizable values based on i.
     */
    public static B2UploadPartUrlResponse uploadPartUrlResponse(String largeFileId,
                                                                int i) {
        return new B2UploadPartUrlResponse(
                largeFileId,
                "uploadUrl" + i,
                "downloadToken" + i);
    }
    public static B2UploadPartUrlResponse uploadPartUrlResponse(int iFile, int i) {
        return uploadPartUrlResponse(fileId(iFile), i);
    }

    public static B2FileVersion makeVersion(int iId,
                                            int iName) {
        return new B2FileVersion(fileId(iId),
                fileName(iName),
                iId * 1000,
                B2ContentTypes.TEXT_PLAIN,
                SAMPLE_SHA1,
                B2Collections.mapOf(),
                "upload",
                System.currentTimeMillis());
    }

    public static B2Part makePart(int i) {

        return new B2Part(
                fileId(i),
                i,
                i * 1000,
                makeSha1(i),
                i);
    }

    // returns an array with the given number of bytes.
    public static byte[] makeBytes(int size) {
        final byte[] v =  new byte[size];

        for (int i=0; i < size; i++) {
            v[i] = (byte) (i % 256);
        }

        return v;
    }

    // returns a string that's as long as a sha1 hex string should be, based on 'i'.
    public static String makeSha1(int i) {
        String toCopy = Integer.toString(i);
        StringBuilder copies = new StringBuilder();
        while (copies.length() < B2Sha1.HEX_SHA1_SIZE) {
            copies.append(toCopy);
        }
        return copies.toString().substring(0, B2Sha1.HEX_SHA1_SIZE);
    }

    public static B2Bucket makeBucket(int i) {
        return new B2Bucket("accountId" + i,
                bucketId(1),
                bucketName(i),
                B2BucketTypes.ALL_PUBLIC,
                B2Collections.mapOf("color", "blue"),
                B2Collections.listOf(makeLifecycleRule(i)),
                i);
    }

    public static B2LifecycleRule makeLifecycleRule(int i) {
        return B2LifecycleRule
                .builder("/prefix" + i + "/")
                .setDaysFromUploadingToHiding(i)
                .setDaysFromHidingToDeleting(2 * i)
                .build();
    }

    public static String fileId(int i) {
        return String.format("4_zBlah_%07d", i);
    }

    public static String fileName(int i) {
        return String.format("files/%04d", i);
    }

}
