/*
 * Copyright 2019, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client;

import com.backblaze.b2.client.exceptions.B2CannotComputeException;
import com.backblaze.b2.client.exceptions.B2Exception;
import com.backblaze.b2.client.exceptions.B2LocalException;
import com.backblaze.b2.client.structures.B2Part;
import com.backblaze.b2.client.structures.B2UploadListener;
import com.backblaze.b2.client.structures.B2UploadProgress;
import com.backblaze.b2.util.B2ByteRange;

import java.util.Objects;

/**
 * This implementation stores a part of a large file by copying
 * from a file that is already stored in B2.
 */
public class B2CopyingPartStorer implements B2PartStorer {

    private final int partNumber;
    private final String sourceFileId;
    private final B2ByteRange byteRangeOrNull;

    public B2CopyingPartStorer(int partNumber, String sourceFileId) {
        this(partNumber, sourceFileId, null);
    }

    public B2CopyingPartStorer(int partNumber, String sourceFileId, B2ByteRange byteRangeOrNull) {
        this.partNumber = partNumber;
        this.sourceFileId = sourceFileId;
        this.byteRangeOrNull = byteRangeOrNull;
    }

    @Override
    public int getPartNumber() {
        return partNumber;
    }

    @Override
    public long getPartSizeOrThrow() throws B2CannotComputeException {
        throw new B2CannotComputeException("cannot determine copied part size.");
    }

    @Override
    public B2Part storePart(
            B2LargeFileStorer largeFileCreationManager,
            B2UploadListener uploadListener) throws B2Exception {

        return largeFileCreationManager.copyPart(partNumber, sourceFileId, byteRangeOrNull, uploadListener);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        B2CopyingPartStorer that = (B2CopyingPartStorer) o;
        return partNumber == that.partNumber &&
                Objects.equals(sourceFileId, that.sourceFileId) &&
                Objects.equals(byteRangeOrNull, that.byteRangeOrNull);
    }

    @Override
    public int hashCode() {
        return Objects.hash(partNumber, sourceFileId, byteRangeOrNull);
    }
}

