/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

/**
 * The B2UploadProgress summarizes the state of an upload.
 * It's a structure so we can extend it later, if needed.
 *
 * NOTE: the length field may sometimes be a little longer than you expect.
 *       For example, if we're adding a SHA1 to the thing being uploaded,
 *       the contentLength will be at least 40 bytes longer than you expect
 *       because we're adding an ascii-hex-encoded SHA1.
 */
public class B2UploadProgress {
    /**
     * the index of the part.  0 <= partIndex < partCount
     */
    private final int partIndex;

    /**
     * how many parts are there for this upload? (always 1 for small files)
     */
    private final int partCount;

    /**
     * at what byte in the contentSource does this part begin.  (0 for small files)
     */
    private final long startByte;

    /**
     * how long is this part (always greater than or equal to the contentSource's contentLength for small files)
     */
    private final long length;

    /**
     * how many bytes have we sent so far?
     */
    private final long bytesSoFar;

    /**
     * see enum above
     */
    private final B2UploadState state;

    public B2UploadProgress(int partIndex,
                            int partCount,
                            long startByte,
                            long length,
                            long bytesSoFar,
                            B2UploadState state) {
        this.partIndex = partIndex;
        this.partCount = partCount;
        this.startByte = startByte;
        this.length = length;
        this.bytesSoFar = bytesSoFar;
        this.state = state;
    }

    public int getPartIndex() {
        return partIndex;
    }

    public int getPartCount() {
        return partCount;
    }

    public long getStartByte() {
        return startByte;
    }

    public long getLength() {
        return length;
    }

    public long getBytesSoFar() {
        return bytesSoFar;
    }

    public B2UploadState getState() {
        return state;
    }

    @Override
    public String toString() {
        return "B2UploadProgress{" +
                "partIndex=" + partIndex +
                ", partCount=" + partCount +
                ", startByte=" + startByte +
                ", length=" + length +
                ", bytesSoFar=" + bytesSoFar +
                ", state=" + state +
                '}';
    }
}
