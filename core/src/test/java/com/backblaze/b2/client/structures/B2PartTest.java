/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

import com.backblaze.b2.json.B2Json;
import com.backblaze.b2.util.B2BaseTest;
import org.junit.Test;

import static com.backblaze.b2.client.B2TestHelpers.fileId;
import static com.backblaze.b2.client.B2TestHelpers.makePart;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class B2PartTest extends B2BaseTest {

    @Test
    public void test() {
        B2Part one = makePart(1);
        B2Part two = makePart(2);

        assertNotEquals(one, two);
        assertEquals(one, one);
        assertEquals(one, makePart(1));

        assertEquals("B2Part{fileId='" + fileId(1) + "', partNumber='1', contentLength=1000, contentSha1='1111111111111111111111111111111111111111', contentMd5='11111111111111111111111111111111', uploadTimestamp=1, serverSideEncryption=null}", one.toString());

        // for coverage
        //noinspection ResultOfMethodCallIgnored
        one.hashCode();
    }

    @Test
    public void testJson() {
        final B2Part part = makePart(1);
        assertEquals(
                part,
                B2Json.fromJsonOrThrowRuntime(
                        B2Json.toJsonOrThrowRuntime(part),
                        B2Part.class
                )
        );
    }
}
