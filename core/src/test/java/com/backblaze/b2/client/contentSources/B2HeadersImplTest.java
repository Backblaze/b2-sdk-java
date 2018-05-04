/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.contentSources;

import com.backblaze.b2.util.B2BaseTest;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class B2HeadersImplTest extends B2BaseTest {
    private static final String SAMPLE_SHA1 = "da39a3ee5e6b4b0d3255bfef95601890afd80709";
    private static final Long   SAMPLE_LAST_MODIFIED_LONG = 1495210502000L;
    private static final String SAMPLE_LAST_MODIFIED = Long.toString(SAMPLE_LAST_MODIFIED_LONG);

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private B2Headers makeEmpty() {
        return B2HeadersImpl
                .builder()
                .build();
    }
    private B2Headers makeNormal() {
        return B2HeadersImpl
                .builder()
                .set(B2Headers.CONTENT_TYPE, B2ContentTypes.TEXT_PLAIN)
                .set(B2Headers.CONTENT_LENGTH, "1234")
                .set(B2Headers.SRC_LAST_MODIFIED_MILLIS, SAMPLE_LAST_MODIFIED)
                .set(B2Headers.CONTENT_SHA1, SAMPLE_SHA1)
                .set(B2Headers.FILE_INFO_PREFIX + "alphabet", "abc")
                .set(B2Headers.FILE_INFO_PREFIX.toLowerCase() + "zoo", "san diego")
                .build();
    }

    @Test
    public void testGetNames() {
        assertTrue(makeEmpty().getNames().isEmpty());

        final B2Headers headers = makeNormal();

        final List<String> expectedNames = Arrays.asList(
                B2Headers.CONTENT_LENGTH,
                B2Headers.CONTENT_TYPE,
                B2Headers.CONTENT_SHA1,
                B2Headers.FILE_INFO_PREFIX + "alphabet",
                B2Headers.SRC_LAST_MODIFIED_MILLIS,
                B2Headers.FILE_INFO_PREFIX.toLowerCase() + "zoo");
        assertEquals(expectedNames, new ArrayList<>(headers.getNames()));
    }

    @Test
    public void testGetValueOrNull() {
        final B2Headers headers = makeNormal();

        // check getValueOrNull
        assertEquals("1234", headers.getValueOrNull(B2Headers.CONTENT_LENGTH));
        assertNull(headers.getValueOrNull("noSuchHeader"));
    }

    @Test
    public void testNamesAreCaseInsensitive() {
        final B2Headers headers = makeNormal();

        // we can fetch the same value with different capitalizations of the name.
        assertEquals(SAMPLE_LAST_MODIFIED, headers.getValueOrNull(B2Headers.SRC_LAST_MODIFIED_MILLIS));
        assertEquals(SAMPLE_LAST_MODIFIED, headers.getValueOrNull(B2Headers.SRC_LAST_MODIFIED_MILLIS.toLowerCase()));
        assertEquals(SAMPLE_LAST_MODIFIED, headers.getValueOrNull(B2Headers.SRC_LAST_MODIFIED_MILLIS.toUpperCase()));

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("already have a value for one");
        B2HeadersImpl
                .builder()
                .set("ONE", "ONE")
                .set("one", "one")
                .build();
    }

    @Test
    public void testGetContentType() {
        assertEquals(B2ContentTypes.TEXT_PLAIN, makeNormal().getContentType());
        assertEquals(B2ContentTypes.APPLICATION_OCTET, makeEmpty().getContentType());
    }

    @Test
    public void testGetContentLength() {
        assertEquals(1234, makeNormal().getContentLength());

        thrown.expect(IllegalStateException.class);
        thrown.expectMessage("don't call if there isn't a Content-Length!");
        makeEmpty().getContentLength();
    }

    @Test
    public void testGetContentLengthWithBogusValue() {
        final B2Headers withBogusContentLength = B2HeadersImpl
                .builder()
                .set(B2Headers.CONTENT_LENGTH, "non-numerical")
                .build();

        thrown.expect(IllegalStateException.class);
        thrown.expectMessage("can't parse Content-Length 'non-numerical' as a long: java.lang.NumberFormatException: For input string: \"non-numerical\"");
        withBogusContentLength.getContentLength();
    }

    @Test
    public void testGetContentSha1OrNull() {
        assertEquals(SAMPLE_SHA1, makeNormal().getContentSha1OrNull());
        assertNull(makeEmpty().getContentSha1OrNull());
    }

    @Test
    public void testGetContentSha1EvenIfUnverifiedOrNull_butNotUnverified() {
        assertEquals(SAMPLE_SHA1, makeNormal().getContentSha1EvenIfUnverifiedOrNull());
        assertNull(makeEmpty().getContentSha1OrNull());
    }

    @Test
    public void testGetContentSha1EvenIfUnverifiedOrNull_andActuallyUnverified() {
        final B2Headers headers = B2HeadersImpl
                .builder()
                .set(B2Headers.CONTENT_SHA1, "unverified:" + SAMPLE_SHA1)
                .build();

        assertEquals(SAMPLE_SHA1, headers.getContentSha1EvenIfUnverifiedOrNull());
        assertNull(makeEmpty().getContentSha1OrNull());
    }

    @Test
    public void testGetB2FileInfo() {
        final Map<String,String> info = makeNormal().getB2FileInfo();
        assertEquals(3, info.size());
        assertEquals(SAMPLE_LAST_MODIFIED, info.get(B2Headers.SRC_LAST_MODIFIED_MILLIS_INFO_NAME));
        assertEquals("abc", info.get("alphabet"));
        assertEquals("san diego", info.get("zoo"));

        assertTrue(makeEmpty().getB2FileInfo().isEmpty());
    }

    @Test
    public void testGetSrcLastModifiedMillis() {
        assertEquals(SAMPLE_LAST_MODIFIED_LONG, makeNormal().getSrcLastModifiedMillis());
        assertNull(makeEmpty().getSrcLastModifiedMillis());

        final B2Headers withBogusSrcLastModifiedMillis = B2HeadersImpl
                .builder()
                .set(B2Headers.SRC_LAST_MODIFIED_MILLIS, "non-numerical")
                .build();
        assertNull(withBogusSrcLastModifiedMillis.getSrcLastModifiedMillis());
    }

    @Test
    public void testBuilderThatCopies() {
        // copying from null
        {
            final B2Headers headers = B2HeadersImpl.builder(null).build();
            assertTrue(headers.getNames().isEmpty());
        }

        // copying from null and adding another header
        {
            final B2Headers headers = B2HeadersImpl
                    .builder(null)
                    .set("name", "value")
                    .build();
            assertEquals(1, headers.getNames().size());
            assertEquals("value", headers.getValueOrNull("name"));
        }

        // copying from a non-empty header
        {
            final B2Headers orig = B2HeadersImpl
                    .builder()
                    .set("one", "1")
                    .set("two", "2")
                    .build();
            final B2Headers copyPlusSome = B2HeadersImpl
                    .builder(orig)
                    .set("three", "3")
                    .build();
            assertEquals(3, copyPlusSome.getNames().size());
            assertEquals("1", copyPlusSome.getValueOrNull("one"));
            assertEquals("2", copyPlusSome.getValueOrNull("two"));
            assertEquals("3", copyPlusSome.getValueOrNull("three"));
        }

        // check that we don't let people clobber copied values.
        // why?  ummm...because that's how the code works for now.
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("already have a value for one");
        {
            final B2Headers orig = B2HeadersImpl
                    .builder()
                    .set("one", "1")
                    .build();
            B2HeadersImpl
                    .builder(orig)
                    .set("one", "111")
                    .build();
        }
    }
}
