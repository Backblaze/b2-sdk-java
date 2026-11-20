/*
 * Copyright 2026, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 *
 * The samples module had no tests.  The B2 command-line sample is driven here through its private methods
 * (reflection), with a mocked B2StorageClient and with System.exit turned into an exception, so the checks
 * observe behaviour.  The two B2Sample checks named "...Static" still read the sample's source text, because the
 * defects are inside a long main()-style method that cannot be run without a real B2 account; they are
 * source-pattern checks, not behaviour tests.
 */
package com.backblaze.b2.sample;

import com.backblaze.b2.testing.KnownDefect;
import org.junit.experimental.categories.Category;
import com.sun.net.httpserver.HttpServer;
import org.junit.Test;

import com.backblaze.b2.client.B2ListFilesIterable;
import com.backblaze.b2.client.B2StorageClient;
import com.backblaze.b2.client.structures.B2Bucket;
import com.backblaze.b2.client.structures.B2FileVersion;
import com.backblaze.b2.client.structures.B2UploadFileRequest;
import org.mockito.ArgumentCaptor;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.Permission;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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
        final CountDownLatch requestSeen = new CountDownLatch(1);
        final AtomicReference<String> rangeHeader = new AtomicReference<>();
        final HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 5);
        server.createContext("/", ex -> {
            rangeHeader.set(ex.getRequestHeaders().getFirst("Range"));
            requestSeen.countDown();
            ex.sendResponseHeaders(200, whole.length); // ignores Range, no 206
            ex.getResponseBody().write(whole);
            ex.close();
        });
        server.start();
        try {
            final String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/f";
            final UrlContentSource part = new UrlContentSource.Builder(url, 100).setStart(200).build();
            IOException rejection = null;
            try (InputStream in = part.createInputStream()) {
                int total = 0;
                while (in.read() != -1) {
                    total++;
                }
                fail("a 200 reply to a range request was accepted and returned " + total + " bytes for a 100-byte part");
            } catch (IOException e) {
                rejection = e;
            }
            // the exception only counts if the server really saw the range request (not a connection failure)
            assertTrue("the server never received the request; the failure was: " + rejection,
                    requestSeen.await(5, TimeUnit.SECONDS));
            assertEquals("bytes=200-299", rangeHeader.get());
            assertTrue("expected the IOException to identify the unexpected 200 reply, but it was: " + rejection,
                    rejection.getMessage() != null && rejection.getMessage().contains("200"));
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

    /** Finding C38: resuming must not use getContentSha1() ("none" for unfinished large files) as the source SHA-1. */
    @Category(KnownDefect.class)
    @Test
    public void testFindingC38_finishUploadingDoesNotUseNoneAsSourceSha1() throws Exception {
        final File local = File.createTempFile("c38", ".bin");
        try {
            final B2FileVersion unfinished = new B2FileVersion("fileId1", "big.bin", 0L, "b2/x-auto", "none", null,
                    Collections.<String, String>emptyMap(), "start", 1L, null, null, null, null);
            final CapturedCommand cmd = runFinishUploading(unfinished,
                    new String[] {"bucket1", "fileId1", local.getPath()});
            assertNull("command exited: " + cmd.exitStatus, cmd.exitStatus);
            assertEquals("the unfinished large file was not passed on", unfinished, cmd.finishedVersion);
            final String sha1 = cmd.request.getContentSource().getSha1OrNull();
            assertFalse("the source SHA-1 given for the local file is the unfinished file's \"none\"", "none".equals(sha1));
        } finally {
            //noinspection ResultOfMethodCallIgnored
            local.delete();
        }
    }

    /** Finding C39: usage text of get_download_authorization must mention the duration the command requires. */
    @Category(KnownDefect.class)
    @Test
    public void testFindingC39_downloadAuthorizationUsageMentionsDuration() throws Exception {
        final ByteArrayOutputStream err = new ByteArrayOutputStream();
        final PrintStream savedErr = System.err;
        Integer exit = null;
        final SecurityManager savedManager = System.getSecurityManager();
        System.setSecurityManager(new NoExitSecurityManager());
        System.setErr(new PrintStream(err, true, "UTF-8"));
        try {
            B2.main(new String[0]); // no command: prints the usage text and exits
        } catch (ExitException e) {
            exit = e.status;
        } finally {
            System.setErr(savedErr);
            System.setSecurityManager(savedManager);
        }
        assertEquals("main() with no arguments should exit with status 1", Integer.valueOf(1), exit);
        final Matcher m = Pattern.compile("b2 get_download_authorization([^\n]*)\n").matcher(err.toString("UTF-8"));
        assertTrue("usage line for get_download_authorization not found in:\n" + err, m.find());
        assertTrue("usage says: " + m.group(1), m.group(1).toLowerCase().contains("duration"));
    }

    /** Finding C40: finish_uploading_large_file must accept as many arguments as its usage text lists (6). */
    @Category(KnownDefect.class)
    @Test
    public void testFindingC40_finishUploadingAcceptsAllUsageArguments() throws Exception {
        final File local = File.createTempFile("c40", ".bin");
        try {
            final B2FileVersion unfinished = new B2FileVersion("fileId1", "big.bin", 0L, "b2/x-auto", "none", null,
                    Collections.<String, String>emptyMap(), "start", 1L, null, null, null, null);
            // usage: [--noProgress] [--threads N] <bucketName> <largeFileId> <localFileName>  => 6 words
            final CapturedCommand cmd = runFinishUploading(unfinished,
                    new String[] {"--noProgress", "--threads", "2", "bucket1", "fileId1", local.getPath()});
            assertNull("the command rejected the full list of arguments from its own usage text: " + cmd.exitStatus
                    + " / " + cmd.stderr, cmd.exitStatus);
            assertNotNull("finishUploadingLargeFile was not called", cmd.request);
        } finally {
            //noinspection ResultOfMethodCallIgnored
            local.delete();
        }
    }

    /** Finding C41: --info must accept values containing '=' (the value is everything after the first '='). */
    @Category(KnownDefect.class)
    @Test
    public void testFindingC41_infoOptionAllowsEqualsInValue() throws Exception {
        final File local = File.createTempFile("c41", ".bin");
        try {
            final B2Bucket bucket = mock(B2Bucket.class);
            when(bucket.getBucketName()).thenReturn("bucket1");
            when(bucket.getBucketId()).thenReturn("bucketId1");
            final B2StorageClient client = mock(B2StorageClient.class);
            when(client.buckets()).thenReturn(Collections.singletonList(bucket));

            final Object[] result = new Object[1];
            final CapturedCommand cmd = runCommand(client, "makeUploadRequestFromArgs",
                    new String[] {"--info", "key=a=b", "bucket1", local.getPath(), "b2name"}, result);
            assertNull("the command exited instead of building the request: " + cmd.exitStatus + " / " + cmd.stderr,
                    cmd.exitStatus);
            final B2UploadFileRequest request = (B2UploadFileRequest) result[0];
            assertNotNull(request);
            assertEquals("a=b", request.getFileInfo().get("key"));
        } finally {
            //noinspection ResultOfMethodCallIgnored
            local.delete();
        }
    }

    // ---- helpers: drive the private B2 command methods without a real account and without exiting the JVM ----

    private static final class ExitException extends SecurityException {
        final int status;

        ExitException(int status) {
            super("System.exit(" + status + ")");
            this.status = status;
        }
    }

    private static final class NoExitSecurityManager extends SecurityManager {
        @Override
        public void checkExit(int status) {
            throw new ExitException(status);
        }

        @Override
        public void checkPermission(Permission perm) {
            // allow everything else
        }
    }

    private static final class CapturedCommand {
        Integer exitStatus;       // null when the command did not call System.exit
        String stderr = "";
        B2FileVersion finishedVersion;
        B2UploadFileRequest request;
    }

    private static CapturedCommand runFinishUploading(B2FileVersion unfinished, String[] args) throws Exception {
        final B2Bucket bucket = mock(B2Bucket.class);
        when(bucket.getBucketName()).thenReturn("bucket1");
        when(bucket.getBucketId()).thenReturn("bucketId1");
        final B2StorageClient client = mock(B2StorageClient.class);
        when(client.buckets()).thenReturn(Collections.singletonList(bucket));
        final B2ListFilesIterable files = mock(B2ListFilesIterable.class);
        when(files.iterator()).thenAnswer(inv -> Collections.singletonList(unfinished).iterator());
        when(client.unfinishedLargeFiles("bucketId1")).thenReturn(files);

        final CapturedCommand cmd = runCommand(client, "finish_uploading_large_file", args, null);
        final ArgumentCaptor<B2FileVersion> version = ArgumentCaptor.forClass(B2FileVersion.class);
        final ArgumentCaptor<B2UploadFileRequest> request = ArgumentCaptor.forClass(B2UploadFileRequest.class);
        if (cmd.exitStatus == null) {
            verify(client).finishUploadingLargeFile(version.capture(), request.capture(), any(ExecutorService.class));
            cmd.finishedVersion = version.getValue();
            cmd.request = request.getValue();
        }
        return cmd;
    }

    /** Runs the named private method of a B2 instance that talks to the given client; System.exit becomes ExitException. */
    private static CapturedCommand runCommand(B2StorageClient client,
                                              String methodName,
                                              String[] args,
                                              Object[] resultOut) throws Exception {
        // B2's constructor builds a real client from B2_* environment variables, so create the instance
        // without running it and set the fields the commands use.
        final Field unsafeField = Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        final Object unsafe = unsafeField.get(null);
        final Method allocateInstance = unsafe.getClass().getMethod("allocateInstance", Class.class);
        final Field clientField = B2.class.getDeclaredField("client");
        clientField.setAccessible(true);
        final Field outField = B2.class.getDeclaredField("out");
        outField.setAccessible(true);
        final Field threadsField = B2.class.getDeclaredField("numThreads");
        threadsField.setAccessible(true);
        final Method method = B2.class.getDeclaredMethod(methodName, String[].class);
        method.setAccessible(true);

        final CapturedCommand cmd = new CapturedCommand();
        final ByteArrayOutputStream err = new ByteArrayOutputStream();
        final PrintStream savedErr = System.err;
        final SecurityManager savedManager = System.getSecurityManager();
        System.setSecurityManager(new NoExitSecurityManager());
        System.setErr(new PrintStream(err, true, "UTF-8"));
        try (B2 b2 = (B2) allocateInstance.invoke(unsafe, B2.class)) {
            clientField.set(b2, client);
            outField.set(b2, new PrintStream(new ByteArrayOutputStream(), true, "UTF-8"));
            threadsField.setInt(b2, 2);
            final Object result = method.invoke(b2, (Object) args);
            if (resultOut != null) {
                resultOut[0] = result;
            }
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof ExitException) {
                cmd.exitStatus = ((ExitException) e.getCause()).status;
            } else {
                throw new AssertionError("B2 command " + methodName + " failed unexpectedly: " + e.getCause(), e.getCause());
            }
        } finally {
            System.setErr(savedErr);
            System.setSecurityManager(savedManager);
        }
        cmd.stderr = err.toString("UTF-8");
        return cmd;
    }
}
