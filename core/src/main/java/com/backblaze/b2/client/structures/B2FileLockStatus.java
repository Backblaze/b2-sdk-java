/*
 * Copyright 2020, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

/**
 * B2FileLockStatus provides constants for a file lock status
 */
public interface B2FileLockStatus {
    /**
     * File lock is enabled/set and the requester has permissions to read the status
     */
    String ON = "on";

    /**
     * File lock is disabled/not-set and the requester has permissions to read the status
     */
    String OFF = "off";

    /**
     * The requester does not have permissions to read the status.  The lock may or may not be set.
     */
    String UNAUTHORIZED = "unauthorized";
}
