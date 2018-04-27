/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client;

import com.backblaze.b2.client.structures.B2AccountAuthorization;
import com.backblaze.b2.util.B2BaseTest;
import com.backblaze.b2.util.B2Collections;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class B2PartSizesTest extends B2BaseTest {
    private final long GB = 1000 * 1000 * 1000;
    private final B2AccountAuthorization accountAuth = B2TestHelpers.makeAuth(1);
    private final B2PartSizes partSizes = B2PartSizes.from(accountAuth);
    private final long minSize = partSizes.getMinimumPartSize();
    private final long recSize = partSizes.getRecommendedPartSize();

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Test
    public void testFrom() {
        assertEquals(accountAuth.getAbsoluteMinimumPartSize(), partSizes.getMinimumPartSize());
        assertEquals(accountAuth.getRecommendedPartSize(), partSizes.getRecommendedPartSize());
    }

    @Test
    public void testCharacterizingSizes() {
        assertTrue(!partSizes.mustBeLargeFile(5 * GB));
        assertTrue(partSizes.mustBeLargeFile((5 * GB) + 1));

        assertTrue(!partSizes.couldBeLargeFile(100));
        assertTrue(partSizes.couldBeLargeFile(100+1));

        assertTrue(!partSizes.shouldBeLargeFile(101));
        assertTrue(!partSizes.shouldBeLargeFile(1999));
        assertTrue(partSizes.shouldBeLargeFile(2000));
    }

    @Test
    public void testPickParts_tooSmall() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("contentLength=100 is too small to make at least two parts.  minimumPartSize=100");
        partSizes.pickParts(minSize);
    }

    @Test
    public void testPickParts_barelyBigEnough() {
        final List<B2PartSpec> specs = partSizes.pickParts(minSize  + 1);
        checkSpecs(specs,
                new B2PartSpec(1, 0, minSize),
                new B2PartSpec(2, minSize, 1)
        );
    }

    @Test
    public void testPickParts_notBigEnoughToUseRecommendedSize() {
        final List<B2PartSpec> specs = partSizes.pickParts((2 * recSize) - 1);
        checkSpecs(specs,
                new B2PartSpec(1, 0, recSize),
                new B2PartSpec(2, recSize, recSize-1)
        );
    }

    @Test
    public void testPickParts_barelyBigEnoughToUseRecommendedSize() {
        final List<B2PartSpec> specs = partSizes.pickParts(2 * recSize);
        checkSpecs(specs,
                new B2PartSpec(1, 0, recSize),
                new B2PartSpec(2, recSize, recSize)
        );
    }

    @Test
    public void testPickParts_bigEnoughToUseRecommendedSize() {
        final List<B2PartSpec> specs = partSizes.pickParts(3 * recSize + (recSize-2));
        checkSpecs(specs,
                new B2PartSpec(1, 0, 1332),
                new B2PartSpec(2, 1332, 1332),
                new B2PartSpec(3, 2664, 1334)
        );
    }

    private void checkSpecs(List<B2PartSpec> specs,
                            B2PartSpec... expectedSpecs) {
        assertEquals(B2Collections.listOf(expectedSpecs), specs);
    }
}
