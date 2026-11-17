/*
 * Copyright 2026, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 *
 * The samples module had no tests.  Tests named "...Static" read the sample's source text, because the
 * defects are in the body of main()-style methods that cannot be called without a real B2 account
 * (or that call System.exit).  They are source-pattern checks, not behaviour tests.
 */
package com.backblaze.b2.sample;

import com.backblaze.b2.testing.KnownDefect;
import org.junit.experimental.categories.Category;
import com.sun.net.httpserver.HttpServer;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

public class SampleFindingsTest {
    private static String source(String name) throws IOException {
        final File f = new File("src/main/java/com/backblaze/b2/sample/" + name);
        // skip (not fail) when the sample source is not available, for example outside a full checkout
        assumeTrue("sample source not found: " + f.getAbsolutePath(), f.isFile());
        return new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
    }

    /** Finding C28: a server that ignores the Range header (answers 200, whole body) must be detected, not read as the part. */
    @Category(KnownDefect.class)
    @Test
    public void testFindingC28_serverIgnoringRangeIsRejected() throws Exception {
        final byte[] whole = new byte[1000];
        final HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 5);
        server.createContext("/", ex -> {
            ex.sendResponseHeaders(200, whole.length); // ignores Range, no 206
            ex.getResponseBody().write(whole);
            ex.close();
        });
        server.start();
        try {
            final String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/f";
            final UrlContentSource part = new UrlContentSource.Builder(url, 100).setStart(200).build();
            try (InputStream in = part.createInputStream()) {
                int total = 0;
                while (in.read() != -1) {
                    total++;
                }
                fail("a 200 reply to a range request was accepted and returned " + total + " bytes for a 100-byte part");
            } catch (IOException expected) {
                // expected: the sample should refuse a non-206 answer.
            }
        } finally {
            server.stop(0);
        }
    }

    /** Finding C43 (static): compressed bytes must not be turned into a String and back. */
    @Category(KnownDefect.class)
    @Test
    public void testFindingC43_compressedBytesNotRoundTrippedThroughStringStatic() throws IOException {
        assertFalse("B2Sample converts deflated bytes with new String(...), which corrupts them",
                source("B2Sample.java").contains("new String(buffer, 0, compressedLength)"));
    }

    /**
     * Finding C44: the huge-file helper must work with the file name the sample passes (it contains '/').
     * Note: this reflects JDK 11/17 behaviour (createTempFile accepts the name) and creates a sparse file of about
     * 2 GiB, which a file system without sparse-file support may fully allocate; the file is deleted afterwards.
     */
    @Test
    public void testFindingC44_makeHugeFileAcceptsTheSamplesFileName() throws Exception {
        final Method m = B2Sample.class.getDeclaredMethod("makeHugeFile", String.class);
        m.setAccessible(true);
        File created = null;
        try {
            created = (File) m.invoke(null, "demo/large/huge2GBfile.txt");
            assertTrue(created.exists());
        } catch (InvocationTargetException e) {
            fail("makeHugeFile threw for the sample's own file name: " + e.getCause());
        } finally {
            if (created != null) {
                //noinspection ResultOfMethodCallIgnored
                created.delete();
            }
        }
    }

    /** Finding C45 (static): the loop over unfinished large files must list the parts of that loop's file. */
    @Category(KnownDefect.class)
    @Test
    public void testFindingC45_listsPartsOfLoopVariableStatic() throws IOException {
        final String src = source("B2Sample.java");
        final int loop = src.indexOf("for (B2FileVersion largeFileVersion : client.unfinishedLargeFiles(bucketId))");
        assertTrue("loop not found", loop >= 0);
        final String body = src.substring(loop, Math.min(src.length(), loop + 400));
        assertTrue("parts are listed for another file:\n" + body, body.contains("client.parts(largeFileVersion.getFileId())"));
    }

    /** Finding C38 (static): resuming must not use getContentSha1() ("none" for unfinished large files) as the source SHA-1. */
    @Category(KnownDefect.class)
    @Test
    public void testFindingC38_finishUploadingUsesLargeFileSha1Static() throws IOException {
        assertFalse("B2.java still passes getContentSha1() of the unfinished large file as the source SHA-1",
                source("B2.java").contains(".setSha1(largeFileVersion.getContentSha1())"));
    }

    /** Finding C39 (static): usage text of get_download_authorization must mention the duration the command requires. */
    @Category(KnownDefect.class)
    @Test
    public void testFindingC39_downloadAuthorizationUsageMentionsDurationStatic() throws IOException {
        final String src = source("B2.java");
        final Matcher m = Pattern.compile("\"\\s*b2 get_download_authorization([^\"]*)\\\\n\"").matcher(src);
        assertTrue("usage line not found", m.find());
        assertTrue("usage says: " + m.group(1), m.group(1).contains("validDurationInSecs") || m.group(1).toLowerCase().contains("duration"));
    }

    /** Finding C40 (static): finish_uploading_large_file must accept as many arguments as its usage text lists (6). */
    @Category(KnownDefect.class)
    @Test
    public void testFindingC40_finishUploadingAcceptsAllUsageArgumentsStatic() throws IOException {
        final String src = source("B2.java");
        final int at = src.indexOf("private void finish_uploading_large_file");
        final Matcher m = Pattern.compile("checkArgCount\\(args, 3, (\\d+)\\)").matcher(src.substring(at, at + 400));
        assertTrue(m.find());
        // usage: [--noProgress] [--threads N] <bucketName> <largeFileId> <localFileName>  => up to 6 words
        assertEquals(6, Integer.parseInt(m.group(1)));
    }

    /** Finding C41 (static): --info must accept values containing '=' (split must be limited to 2 parts). */
    @Category(KnownDefect.class)
    @Test
    public void testFindingC41_infoOptionAllowsEqualsInValueStatic() throws IOException {
        assertFalse("B2.java still calls pair.split(\"=\") without a limit, so a value containing '=' is cut",
                source("B2.java").contains("pair.split(\"=\")"));
    }
}
