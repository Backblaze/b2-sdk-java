/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client;

/**
 * We try to not hard-code limitations into the SDK.
 * However, we've chosen to put these numbers in the code.
 */
interface B2StorageLimits {

    /**
     * What's the largest number of parts we're allowed to have in a single large file?
     *   (needed in the client so we make bigger parts rather than making too many.)
     */
    static final int MAX_PARTS_PER_LARGE_FILE = 10 * 1000;
}
