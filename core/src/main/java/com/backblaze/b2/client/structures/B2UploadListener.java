/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

/**
 * A B2UploadListener is called by periodically by the upload processes to
 * provide an indication of progress.
 *
 * This interface is used for both small files and for large files.
 * In both cases, partIndex is zero-based.  (Don't confuse partIndex with
 * a large file's part's partNumber.)  For a small file, you'll only get
 * updates for partIndex=0 and partCount will always be 1.
 * For a large file with N parts, partIndex will range from 0 to N-1 and
 * partCount will always be N.
 *
 * You may be called with state FAILED more than once in a row as various
 * layers of the software notice the error.  Further, because of the way
 * the SDK retries, you may see updates indicating progress and perhaps
 * even failure and then you may see updates showing one or more subsequent
 * attempts to upload the same content.
 *
 * THREAD-SAFETY: Listeners will be called from arbitrary threads and may be
 * called from multiple threads simultaneously.  As a result, implementations
 * of B2UploadListener must be thread-safe.
 *
 * PERFORMANCE: do not do anything that might block the thread for any appreciable
 *              amount of time.  doing so will slow down the upload.
 */
public interface B2UploadListener {


    /**
     * Called with progress about an upload.
     */
    void progress(B2UploadProgress progress);

    /**
     * @return A listener that doesn't do anything when called.
     */
    static B2UploadListener noopListener() {
        return (progress) -> {
        };
    }
}
