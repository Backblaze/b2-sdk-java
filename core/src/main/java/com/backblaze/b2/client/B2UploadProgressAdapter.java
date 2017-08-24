/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client;

import com.backblaze.b2.client.structures.B2UploadListener;
import com.backblaze.b2.client.structures.B2UploadProgress;
import com.backblaze.b2.client.structures.B2UploadState;
import com.backblaze.b2.util.B2ByteProgressListener;

/**
 * B2UploadProgressAdapter turns byte-level updates into B2UploadProgress updates
 * and notifies the given B2UploadListener.
 */
class B2UploadProgressAdapter implements B2ByteProgressListener {
    private final B2UploadListener uploadListener;
    private final int partIndex;
    private final int partCount;
    private final long startByte;
    private final long length;

    /**
     * Constructs an B2UploadProgressAdapter with the B2UploadListener it should
     * call and all of the constant data needed to turn a byte-level update
     * into a B2UploadProgress object.  See B2UploadProgress for more details
     * about these arguments.
     *
     * @param uploadListener the listener to notify
     * @param partIndex which part is this? (0-based index)
     * @param partCount how many parts are there total?
     * @param startByte at which byte of the overall file does this part start?
     * @param length what's the total length of this part.
     */
    B2UploadProgressAdapter(B2UploadListener uploadListener,
                            int partIndex,
                            int partCount,
                            long startByte,
                            long length) {
        this.uploadListener = uploadListener;
        this.partIndex = partIndex;
        this.partCount = partCount;
        this.startByte = startByte;
        this.length = length;
    }

    @Override
    public void progress(long nBytesSoFar) {
        final B2UploadProgress progress = new B2UploadProgress(
            partIndex,
                partCount,
                startByte,
                length,
                nBytesSoFar,
                B2UploadState.UPLOADING
        );
        uploadListener.progress(progress);
    }

    @Override
    public void hitException(Exception e,
                             long nBytesSoFar) {
        final B2UploadProgress progress = new B2UploadProgress(
                partIndex,
                partCount,
                startByte,
                length,
                nBytesSoFar,
                B2UploadState.FAILED
        );
        uploadListener.progress(progress);

    }

    @Override
    public void reachedEof(long nBytesSoFar) {
        final B2UploadProgress progress = new B2UploadProgress(
                partIndex,
                partCount,
                startByte,
                nBytesSoFar,
                nBytesSoFar,
                B2UploadState.UPLOADING
        );
        uploadListener.progress(progress);
    }
}
