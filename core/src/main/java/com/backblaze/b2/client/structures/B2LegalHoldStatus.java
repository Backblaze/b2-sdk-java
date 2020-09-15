/*
 * Copyright 2020, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

/**
 * B2LegalHoldStatus provides constants for a legal hold status
 */
public interface B2LegalHoldStatus {
    /**
     * Legal hold is on/enabled/set and the requester has permissions to read the status
     */
    String ON = "on";

    /**
     * Legal hold is off/disabled/not-set and the requester has permissions to read the status
     */
    String OFF = "off";

    /**
     * The requester does not have permissions to read the status.  The hold may or may not be set.
     */
    String UNAUTHORIZED = "unauthorized";
}
