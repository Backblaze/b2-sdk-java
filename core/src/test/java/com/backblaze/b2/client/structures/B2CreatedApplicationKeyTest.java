/*
 * Copyright 2018, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */

package com.backblaze.b2.client.structures;

import com.backblaze.b2.util.B2Collections;
import org.junit.Test;

import java.util.TreeSet;

import static org.junit.Assert.assertEquals;

public class B2CreatedApplicationKeyTest {

    @Test
    public void testToApplicationKey() {
        final TreeSet<String> capabilities = new TreeSet<>();
        capabilities.add(B2Capabilities.WRITE_FILES);

        assertEquals(
                new B2ApplicationKey(
                        "accountId",
                        "appKeyId",
                        "keyName",
                        capabilities,
                        "bucketId",
                        "namePrefix",
                        12345678L,
                        B2Collections.emptySet()
                ),
                new B2CreatedApplicationKey(

                        "accountId",
                        "appKeyId",
                        "appKeySecret",
                        "keyName",
                        capabilities,
                        "bucketId",
                        "namePrefix",
                        12345678L,
                        B2Collections.emptySet()
                ).toApplicationKey()
        );
    }


}
