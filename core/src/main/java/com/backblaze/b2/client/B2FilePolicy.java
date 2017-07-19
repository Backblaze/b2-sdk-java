/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client;

/**
 * B2FilePolicy answers questions about whether to upload a file as a 'small'
 * file or as a 'large' file.
 *
 * Uploading a file as a large file *may* increase your upload-speed because
 * parts of a large file may be uploaded in parallel.
 *
 * Uploading as large file involves more state and you will need to
 * resume or cancel interrupted uploads, so it complicates your client.
 * If you never have to upload a file that's more than 5 GB, you can keep
 * your client simpler by always uploading 'small' files.
 *
 * Files that are too large, must be uploaded as large files, so you
 * can't avoid using them if you have files larger than 5 GB.
 *
 * If you have to upload arbitrarily-sized files, you have to implement the
 * cleanup and you can use shouldBeLargeFile to determine which type of upload
 * to use for each file.
 */
public interface B2FilePolicy {

    /**
     * @param contentSize the size of the file you want to upload.
     * @return true iff there would be a reasonable number of reasonably-sized
     *         parts for the given file size.
     */
    boolean shouldBeLargeFile(long contentSize);

    /**
     * @param contentSize the size of the file you want to upload.
     * @return true iff contentSize is too big to fit in a normal (non-large) file.
     */
    boolean mustBeLargeFile(long contentSize);

    /**
     * @param contentSize the size of the file you want to upload.
     * @return true iff contentSize is big enough to be a large file.
     *         this is based on the absoluteMinimumPartSize and say
     *         yes to silly things like a file just barely large enough
     *         to have one minimum-sized part and one one-byte part.
     */
    boolean couldBeLargeFile(long contentSize);
}
