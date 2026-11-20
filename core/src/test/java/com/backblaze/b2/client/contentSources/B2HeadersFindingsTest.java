/*
 * Copyright 2026, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.contentSources;

import com.backblaze.b2.testing.KnownDefect;
import org.junit.experimental.categories.Category;
import com.backblaze.b2.util.B2BaseTest;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class B2HeadersFindingsTest extends B2BaseTest {
    /** Finding C30: a raw value with a bad percent sequence (e.g. "100%") must not make getB2FileInfo() throw. */
    @Category(KnownDefect.class)
    @Test
    public void testFindingC30_badPercentSequenceInFileInfoValueDoesNotThrow() {
        final B2Headers headers = B2HeadersImpl.builder()
                .set(B2Headers.FILE_INFO_PREFIX + "discount", "100%")
                .set(B2Headers.FILE_INFO_PREFIX + "color", "gr%C3%BCn")
                .build();
        final Map<String, String> info;
        try {
            info = headers.getB2FileInfo();
        } catch (RuntimeException e) {
            fail("getB2FileInfo threw: " + e);
            return;
        }
        assertEquals("100%", info.get("discount"));
        assertEquals("gr\u00fcn", info.get("color"));
    }
}
