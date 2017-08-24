/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

/**
 * The B2UploadProgress summarizes the status of an upload.
 * It's a structure so we can extend it later, if needed.
 */
public class B2UploadProgress {
    /**
     * the index of the part.  0 <= partIndex < partCount
     */
    final int partIndex;

    /**
     * how many parts are there for this upload? (always 1 for small files)
     */
    final int partCount;

    /**
     * at what byte in the contentSource does this part begin.  (0 for small files)
     */
    final long startByte;

    /**
     * how long is this part (always matches the contentSource's contentLength for small files)
     */
    final long length;

    /**
     * how many bytes have we sent so far?
     */
    final long bytesSoFar;

    /**
     * see enum above
     */
    final B2UploadState status;

    public B2UploadProgress(int partIndex,
                            int partCount,
                            long startByte,
                            long length,
                            long bytesSoFar,
                            B2UploadState status) {
        this.partIndex = partIndex;
        this.partCount = partCount;
        this.startByte = startByte;
        this.length = length;
        this.bytesSoFar = bytesSoFar;
        this.status = status;
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

    public B2UploadState getStatus() {
        return status;
    }

    @Override
    public String toString() {
        return "B2UploadProgress{" +
                "partIndex=" + partIndex +
                ", partCount=" + partCount +
                ", startByte=" + startByte +
                ", length=" + length +
                ", bytesSoFar=" + bytesSoFar +
                ", status=" + status +
                '}';
    }
}
