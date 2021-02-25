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
     * Legal hold is on (enabled)
     */
    String ON = "on";

    /**
     * Legal hold is off (disabled)
     */
    String OFF = "off";

    /**
     * Legal hold is not set
     */
    String UNSET = "unset";
}
