/*
 * Copyright 2020, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

/**
 * B2BucketFileLockStatus provides constants for a bucket's default file lock status
 */
@SuppressWarnings("unused")
public interface B2BucketFileLockStatus {
    /**
     * File locking is enabled/set and the requester has permissions to read the status
     */
    String ENABLED = "enabled";

    /**
     * File locking is disabled/not-set and the requester has permissions to read the status
     */
    String DISABLED = "disabled";

    /**
     * The requester does not have permissions to read the status.  The lock may or may not be set.
     */
    String UNAUTHORIZED = "unauthorized";
}
