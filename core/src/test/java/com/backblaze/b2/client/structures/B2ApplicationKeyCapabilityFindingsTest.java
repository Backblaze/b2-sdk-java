/*
 * Copyright 2026, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

import com.backblaze.b2.util.B2BaseTest;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class B2ApplicationKeyCapabilityFindingsTest extends B2BaseTest {
    /**
     * Finding C48: the existing round-trip test would pass for any spelling.  Pin the exact wire names
     * (the SDK expects these strings from, and sends them to, the B2 API).
     */
    @Test
    public void testFindingC48_notificationCapabilityNamesAreExact() {
        assertEquals("readBucketNotifications", B2Capabilities.READ_BUCKET_NOTIFICATIONS);
        assertEquals("writeBucketNotifications", B2Capabilities.WRITE_BUCKET_NOTIFICATIONS);
    }
}
