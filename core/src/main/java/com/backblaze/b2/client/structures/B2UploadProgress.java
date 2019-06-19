/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

import java.util.Objects;

/**
 * The B2UploadProgress summarizes the state of an upload.
 * It's a structure so we can extend it later, if needed.
 *
 * NOTE: the length field may sometimes be a little longer than you expect.
 *       For example, if we're adding a SHA1 to the thing being uploaded,
 *       the contentLength will be at least 40 bytes longer than you expect
 *       because we're adding an ascii-hex-encoded SHA1.
 *
 * NOTE: when storing a large file that involves copying parts, it's not
 *       possible to reliably compute the startByte for parts that come
 *       after a copied part. As a result, these values will be set to
 *       the constant UNKNOWN_PART_START_BYTE.
 *
 *       Additionally, until copied parts are successfully stored and the
 *       B2 API returns the corresponding B2Part, progress events for copied
 *       parts will show the length as UNKNOWN_PART_SIZE_PLACEHOLDER. Once a copy succeeds,
 *       the progress event for that success will contain the correct length.
 */
public class B2UploadProgress {

    /**
     * Placeholder value for part sizes that will not be known until the
     * part is stored in B2. For copy operations, we will not always know
     * the true part size until we receive the B2Part from b2_copy_part.
     * Unless the part being stored is 1 byte, using this value will lead
     * to underestimates on the progress percentage until the part is
     * properly stored.
     */
    public static final long UNKNOWN_PART_SIZE_PLACEHOLDER = 1;

    /**
     * Constant value used to indicate that the starting byte position of
     * the part is unknown. This can happen when storing a large file
     * involves copy operations. Since the sizes of those parts will not
     * be known until they finish copying, parts that come after them
     * will have unknown starting positions in the final file.
     */
    public static final long UNKNOWN_PART_START_BYTE = -1;

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

    public boolean isStartByteKnown() {
        return startByte != UNKNOWN_PART_START_BYTE;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        B2UploadProgress progress = (B2UploadProgress) o;
        return partIndex == progress.partIndex &&
                partCount == progress.partCount &&
                startByte == progress.startByte &&
                length == progress.length &&
                bytesSoFar == progress.bytesSoFar &&
                state == progress.state;
    }

    @Override
    public int hashCode() {
        return Objects.hash(partIndex, partCount, startByte, length, bytesSoFar, state);
    }
}
