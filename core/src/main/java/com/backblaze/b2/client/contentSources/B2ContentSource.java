/*
 * Copyright 2019, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */

package com.backblaze.b2.client.contentSources;

import java.io.IOException;
import java.io.InputStream;

/**
 * Implementations of B2ContentSource provide the length, SHA1, and the
 * bytes that the B2Client should upload.
 *
 * There are common implementations of this class, such as B2FileContentSource
 * and MemoryContentSource.
 */
public interface B2ContentSource {
    /**
     * @return the number of bytes in the content.  the length must be non-negative.
     * @throws IOException if there's trouble
     */
    long getContentLength() throws IOException;

    /**
     * You are encouraged to implement this.  If it returns non-null,
     * the B2Client can provide the sha1 before performing the upload.
     *
     * Definitely implement this if you've stored the sha1 separately
     * from the file.  That way you can ensure that B2 doesn't take the
     * file if there's trouble reading it from your source.  If you
     * return null, B2StorageClient will compute the SHA1 from the bytes in the
     * stream.
     *
     * Note that large files do not have SHA-1s for the entire file.
     * If you provide a SHA-1 for a large file upload, the SDK follows
     * the recommendation of putting your value into the 'large_file_sha1'
     * fileInfo.  See "SHA1 Checksums" in https://www.backblaze.com/b2/docs/large_files.html
     *
     * @return the hex-encoded sha1 for the content or null if it's not known yet.
     * @throws IOException if there's trouble
     */
    String getSha1OrNull() throws IOException;

    /**
     * @return the time the source was last modified (in milliseconds since the epoch)
     *         or null if there isn't a reasonable value for that.
     * @throws IOException if there's trouble
     */
    Long getSrcLastModifiedMillisOrNull() throws IOException;

    /**
     * NOTE: this may be called multiple times as uploads
     *       are retried, etc.  The content is expected to be identical
     *       each time this is called.
     * @return a new inputStream containing the contents.
     * @throws IOException if there's trouble
     */
    InputStream createInputStream() throws IOException;

    /**
     * If possible, this returns a NEW input stream for just the specified range of the
     * content.  If it's not possible (or just not implemented), this returns null.
     *
     * The large file uploading mechanism uses this call to get a stream for each
     * part that will be uploaded separately.  If this returns null, the large file
     * uploader will use createInputStream() and read and discard the initial
     * part of the stream to get to the part it needs.
     *
     * This method is optional.  However, if your content source will be used
     * for large file uploads, please implement it to make your uploads more
     * efficient.
     *
     * NOTE: this may be called multiple times as uploads are retried, etc.
     *       The overall content is expected to be identical each time this is called.
     * @return a new inputStream containing the specified range of the overall contents.
     * @throws IOException if there's trouble
     */
    default B2ContentSource createContentSourceWithRangeOrNull(long start, long length) throws IOException {
        return null;
    }
}
