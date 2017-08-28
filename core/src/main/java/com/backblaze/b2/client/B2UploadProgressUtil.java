/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client;

import com.backblaze.b2.client.structures.B2UploadProgress;
import com.backblaze.b2.client.structures.B2UploadState;

interface B2UploadProgressUtil {

    /**
     * @param partSpec    the spec for the part to report progress for.
     * @param numParts    the total number of parts in the upload
     * @param bytesSoFar  how many bytes have we uploaded so far?
     * @param uploadState what's the upload's state?
     * @return a new B2UploadProgress for the given spec
     */
    static B2UploadProgress forPart(B2PartSpec partSpec,
                                    int numParts,
                                    long bytesSoFar,
                                    B2UploadState uploadState) {
        return new B2UploadProgress(partSpec.getPartNumber()-1,
                numParts,
                partSpec.getStart(),
                partSpec.getLength(),
                bytesSoFar,
                uploadState);
    }

    /**
     * @param partSpec the spec for the part to report progress for.
     * @param numParts the total number of parts in the upload
     * @return a new B2UploadProgress that says the upload of the part completed successfully.
     */
    static B2UploadProgress forPartSucceeded(B2PartSpec partSpec,
                                             int numParts) {
        return forPart(partSpec,
                numParts,
                partSpec.getLength(),
                B2UploadState.SUCCEEDED);
    }

    /**
     * @param partSpec the spec for the part to report progress for.
     * @param numParts the total number of parts in the upload
     * @return a new B2UploadProgress that says the upload of the part completed successfully.
     */
    static B2UploadProgress forPartFailed(B2PartSpec partSpec,
                                          int numParts,
                                          long bytesSoFar) {
        return forPart(partSpec,
                numParts,
                bytesSoFar,
                B2UploadState.FAILED);
    }

    static B2UploadProgress forSmallFile(long contentLength,
                                         long bytesSoFar,
                                         B2UploadState state) {
        return new B2UploadProgress(0, // part index
                1, // partCount
                0, // startByte
                contentLength,
                bytesSoFar,
                state);
    }

    static B2UploadProgress forSmallFileWaitingToStart(long contentLen) {
        return forSmallFile(contentLen, 0, B2UploadState.WAITING_TO_START);
    }

    static B2UploadProgress forSmallFileStarting(long contentLen) {
        return forSmallFile(contentLen, 0, B2UploadState.STARTING);
    }

    static B2UploadProgress forSmallFileSucceeded(long contentLen) {
        return forSmallFile(contentLen, contentLen, B2UploadState.SUCCEEDED);
    }

    static B2UploadProgress forSmallFileFailed(long contentLength, long bytesSoFar) {
        return forSmallFile(contentLength, bytesSoFar, B2UploadState.FAILED);
    }
}
