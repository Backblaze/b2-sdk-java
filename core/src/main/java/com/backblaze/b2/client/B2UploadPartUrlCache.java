/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client;

import com.backblaze.b2.client.exceptions.B2Exception;
import com.backblaze.b2.client.structures.B2GetUploadPartUrlRequest;
import com.backblaze.b2.client.structures.B2UploadPartUrlResponse;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * The B2UploadPartUrlCache holds upload responses for a single large file.
 * When you need one, call get().
 * If you use it and it works, unget() it when you're done.
 * If there's trouble, don't unget() it so it won't be used again.
 *
 * THREAD-SAFETY: this class may be used from multiple threads safely.
 *    (for this to be true, the accountAuthCache and webifier must be thread safe!)
 */
class B2UploadPartUrlCache {
    private final B2StorageClientWebifier webifier;
    private final B2AccountAuthorizationCache accountAuthCache;
    private final String largeFileId;

    // this is a deque so that i can inexpensively pull from the
    // front and add to the end so that answers don't get stuck too long.
    private final Deque<B2UploadPartUrlResponse> responses = new ArrayDeque<>();

    B2UploadPartUrlCache(B2StorageClientWebifier webifier,
                         B2AccountAuthorizationCache accountAuthCache,
                         String largeFileId) {
        this.webifier = webifier;
        this.accountAuthCache = accountAuthCache;
        this.largeFileId = largeFileId;
    }

    /**
     * @param isRetry says whether we want a url for a retry or for an initial attempt.
     * @return an upload url response suitable for uploading to that bucket.
     *
     * THREADING: Note that we intentionally do *NOT* serialize other threads
     *            while a thread is waiting for an answer from the server.
     *            Each thread will need its own upload url, so there is a
     *            benefit to asking for them in parallel.
     */
    B2UploadPartUrlResponse get(boolean isRetry) throws B2Exception {
        // we don't use cached URLs for retries because many of them may have
        // gotten stale simultaneously and we don't want to use up all of client's
        // retries on stale URLs.  as long as every client first calls this
        // with isRetry=false and does not unget() the url before calling it
        // with isRetry=true, we will make progress clearing stale URLS and
        // we won't build up lots of URLs.
        if (!isRetry) {
            // first, look to see if we have an answer already.
            synchronized (this) {
                B2UploadPartUrlResponse response = responses.pollFirst();
                if (response != null) {
                    return response;
                }
            }
        }

        // we don't have an answer yet, so ask the server for one and return it.
        final B2GetUploadPartUrlRequest request = B2GetUploadPartUrlRequest.builder(largeFileId).build();
        return webifier.getUploadPartUrl(accountAuthCache.get(), request);
    }

    /**
     * Call this to offer a response back to the cache for later use.
     * Only call this if you successfully used the response to do an upload.
     * Do not use the response again after calling this because it may
     *   given to another thread before this method even returns.
     *
     * @param response the response to return to the cache for later use.
     */
    synchronized void unget(B2UploadPartUrlResponse response) {
        responses.offerLast(response);
    }
}
