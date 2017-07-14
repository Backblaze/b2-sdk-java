/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client;

import com.backblaze.b2.client.structures.B2AccountAuthorization;
import com.backblaze.b2.util.B2Preconditions;

import java.util.ArrayList;
import java.util.List;

class B2PartSizes {
    private final long minimumPartSize;
    private final long recommendedPartSize;

    private B2PartSizes(long minimumPartSize,
                long recommendedPartSize) {
        this.minimumPartSize = minimumPartSize;
        this.recommendedPartSize = recommendedPartSize;
    }

    /**
     * Captures the part sizes from an account authorization.
     * @param auth the accountAuthorization to inspect
     * @return a B2PartSizes with the sizes specified in the accountAuthorization.
     */
    static B2PartSizes from(B2AccountAuthorization auth) {
        return new B2PartSizes(
                auth.getAbsoluteMinimumPartSize(),
                auth.getRecommendedPartSize()
        );
    }

    public long getMinimumPartSize() {
        return minimumPartSize;
    }

    public long getRecommendedPartSize() {
        return recommendedPartSize;
    }

    /**
     * Note that this is a minimum requirement.
     *
     * @param contentLength the length of some content.
     * @return true iff the content is large enough to be a large file.
     */
    boolean isBigEnoughToBeLargeFile(long contentLength) {
        // this is greater than because there must be at least two parts
        // and the first one must be minimumPartSize and the second must
        // be at least one byte long.
        return contentLength > minimumPartSize;
    }

    boolean shouldTreatAsLargeFile(long contentLength) {
        // if we can't make at least two reasonably-sized parts, we should just upload as a small file!
        return contentLength >= (2 * recommendedPartSize);
    }

    List<B2PartSpec> pickParts(long contentLength) {
        B2Preconditions.checkArgument(isBigEnoughToBeLargeFile(contentLength),
                "contentLength=" + contentLength + " is too small to make at least two parts.  minimumPartSize=" + minimumPartSize);

        // how many parts should we make?  be sure to not go over the maximum we're allowed!
        final int partCount;
        final long partSize;
        final long lastPartSize;

        if (contentLength < 2 * minimumPartSize) {
            // we have to have at least two parts, so...
            partCount = 2;
            partSize = minimumPartSize;
            lastPartSize = contentLength - minimumPartSize;
        } else if (contentLength < 2 * recommendedPartSize) {
            // we have to have at least two parts, so...
            partCount = 2;
            partSize = (contentLength+1) / partCount;
            lastPartSize = contentLength - partSize;
        } else {
            partCount = Math.min(B2StorageLimits.MAX_PARTS_PER_LARGE_FILE, (int) (contentLength / recommendedPartSize));

            // all of the parts are the same size, except for the last one which may be bigger.
            partSize = contentLength / partCount;
            lastPartSize = contentLength - ((partCount-1) * partSize);
            B2Preconditions.checkState(lastPartSize >= partSize);
        }

        B2Preconditions.checkState(partCount >= 2);
        B2Preconditions.checkState(partSize >= minimumPartSize);
        B2Preconditions.checkState(lastPartSize >= 1);
        final List<B2PartSpec> parts = new ArrayList<>();

        // add all but the last part.
        for (int i=0; i < (partCount-1); i++) {
            parts.add(new B2PartSpec(i+1, i*partSize, partSize));
        }

        // add the last part
        parts.add(new B2PartSpec(partCount, contentLength-lastPartSize, lastPartSize));

        return parts;
    }
}
