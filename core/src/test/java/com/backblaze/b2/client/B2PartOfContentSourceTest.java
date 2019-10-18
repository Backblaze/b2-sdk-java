/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client;

import com.backblaze.b2.client.contentSources.B2ContentSource;
import com.backblaze.b2.client.exceptions.B2Exception;
import com.backblaze.b2.util.B2BaseTest;
import com.backblaze.b2.util.B2InputStreamExcerpt;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static com.backblaze.b2.client.B2TestHelpers.SAMPLE_SHA1;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class B2PartOfContentSourceTest extends B2BaseTest {
    final B2ContentSource source = mock(B2ContentSource.class);
    final B2PartOfContentSource partOf = new B2PartOfContentSource(source, 26, 100);

    @Rule
    public ExpectedException thrown = ExpectedException.none();


    @Test
    public void testSimpleAccessors() throws IOException {
        assertTrue(partOf.toString().startsWith("B2PartOfContentSource{start=26, length=100, source=Mock for B2ContentSource"));

        when(source.getContentLength()).thenReturn(666L);
        assertEquals(100, partOf.getContentLength());  // uses the part's length, not the underlying stream's!

        when(source.getSha1OrNull()).thenReturn(SAMPLE_SHA1);
        assertNull(partOf.getSha1OrNull()); // we don't know the sha1 of the part.

        thrown.expect(IllegalStateException.class);
        thrown.expectMessage("why are we asking about the srcLastModifiedMillis of a PART?");
        partOf.getSrcLastModifiedMillisOrNull();
    }

    @Test
    public void testCreateInputStream() throws IOException, B2Exception {
        final InputStream underlyingStream = new ByteArrayInputStream(new byte[0]);
        when(source.createInputStream()).thenReturn(underlyingStream);

        final InputStream wrappedStream = partOf.createInputStream();
        assertTrue(wrappedStream instanceof B2InputStreamExcerpt);
        final B2InputStreamExcerpt excerpt = (B2InputStreamExcerpt) wrappedStream;
        assertEquals(26, excerpt.getExcerptStart());
        assertEquals(100, excerpt.getExcerptLength());

    }
}
