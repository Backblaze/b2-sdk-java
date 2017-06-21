/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client;

import com.backblaze.b2.client.exceptions.B2Exception;
import com.backblaze.b2.client.structures.B2GetUploadUrlRequest;
import com.backblaze.b2.client.structures.B2UploadUrlResponse;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;

/**
 * The B2UploadUrlCache holds upload urls for buckets.
 * When you need one, call get() with your bucket id.
 * If you use it and it works, unget() it when you're done.
 * If there's trouble, don't unget() it so it won't be used again.
 *
 * THREAD-SAFETY: this class may be used from multiple threads safely.
 */
class B2UploadUrlCache {
    // how many buckets are we willing to track at once?
    static final int MAX_BUCKETS = 100;

    private final B2StorageClientWebifier webifier;
    private final B2AccountAuthorizationCache accountAuthCache;

    // this is a LinkedHashMap because i want get rid of the least recently
    // used entries when we've got more than 100 buckets.  that's the most
    // buckets any account is allowed to have, so it's reasonable time to
    // start discarding the oldest buckets if someone is cycling through
    // buckets.
    //
    // this contains deques so that i can inexpensively pull from the
    // front and add to the end so that answers don't get stuck too long.
    //
    // it might be nice to have a maximum number of responses per bucket
    // that i'm willing to keep or a maximum age before we consider them
    // too old, or something else to help bound memory usage.  meanwhile,
    // it should be roughly bound by the number of threads that have
    // ever uploaded to a given bucket simultaneously and that's probably
    // good enough.
    private final Map<String, Deque<B2UploadUrlResponse>> perBucket;

    B2UploadUrlCache(B2StorageClientWebifier webifier,
                     B2AccountAuthorizationCache accountAuthCache) {
        this.webifier = webifier;
        this.accountAuthCache = accountAuthCache;
        perBucket = B2BoundedLruMap.withMax(MAX_BUCKETS);
    }

    /**
     * @param bucketId the bucket we want to upload to.
     * @param isRetry says whether we want a url for a retry or for an initial attempt.
     * @return an upload url response suitable for uploading to that bucket.
     *
     * THREADING: Note that we intentionally do *NOT* serialize other threads
     *            while a thread is waiting for an answer from the server.
     *            Each thread will need its own upload url, so there is a
     *            benefit to asking for them in parallel.
     */
    B2UploadUrlResponse get(String bucketId,
                            boolean isRetry) throws B2Exception {
        // we don't use cached URLs for retries because many of them may have
        // gotten stale simultaneously and we don't want to use up all of client's
        // retries on stale URLs.  as long as every client first calls this
        // with isRetry=false and does not unget() the url before calling it
        // with isRetry=true, we will make progress clearing stale URLS and
        // we won't build up lots of URLs.
        if (!isRetry) {
            // first, look to see if we have an answer already.
            synchronized (this) {
                Deque<B2UploadUrlResponse> responses = perBucket.get(bucketId);
                if (responses != null) {
                    B2UploadUrlResponse response = responses.pollFirst();
                    if (response != null) {
                        return response;
                    }
                }
            }
        }

        // we don't have an answer yet, so ask the server for one and return it.
        final B2GetUploadUrlRequest request = new B2GetUploadUrlRequest(bucketId);
        return webifier.getUploadUrl(accountAuthCache.get(), request);
    }

    /**
     * Call this to offer a response back to the cache for later use.
     * Only call this if you successfully used the response to do an upload.
     * Do not use the response again after calling this because it may
     *   given to another thread before this method even returns.
     *
     * @param response the response to return to the cache for later use.
     */
    synchronized void unget(B2UploadUrlResponse response) {
        Deque<B2UploadUrlResponse> responses = perBucket.computeIfAbsent(response.getBucketId(), k -> new ArrayDeque<>());
        responses.offerLast(response);
    }
}
