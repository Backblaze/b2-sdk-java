/*
 * Copyright 2026, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client;

import com.backblaze.b2.testing.KnownDefect;
import org.junit.experimental.categories.Category;
import com.backblaze.b2.client.contentSources.B2ContentSource;
import com.backblaze.b2.client.exceptions.B2Exception;
import com.backblaze.b2.client.exceptions.B2InternalErrorException;
import com.backblaze.b2.client.exceptions.B2LocalException;
import com.backblaze.b2.client.structures.B2Part;
import com.backblaze.b2.client.structures.B2StoreLargeFileRequest;
import com.backblaze.b2.client.structures.B2UploadListener;
import com.backblaze.b2.client.structures.B2UploadProgress;
import com.backblaze.b2.util.B2BaseTest;
import org.junit.After;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import static com.backblaze.b2.client.B2TestHelpers.fileId;
import static com.backblaze.b2.client.B2TestHelpers.makeMd5;
import static com.backblaze.b2.client.B2TestHelpers.makeSha1;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

public class B2LargeFileStorerFindingsTest extends B2BaseTest {
    private final B2AccountAuthorizationCache authCache = mock(B2AccountAuthorizationCache.class);
    private final B2StorageClientWebifier webifier = mock(B2StorageClientWebifier.class);
    private final B2Retryer retryer = new B2Retryer(mock(B2Sleeper.class));
    private final ExecutorService pool = Executors.newFixedThreadPool(4);

    @After
    public void tearDown() {
        Thread.interrupted(); // never leak an interrupt flag into other tests
        pool.shutdownNow();
    }

    /** A part storer we fully control. */
    private static class FakePartStorer implements B2PartStorer {
        private final int partNumber;
        private final long size;
        private final CountDownLatch release;
        private final boolean failImmediately;
        final AtomicReference<B2CancellationToken> token = new AtomicReference<>();
        /** counted down when storePart() is entered. */
        final CountDownLatch started = new CountDownLatch(1);
        /** counted down when storePart() is left, however it is left. */
        final CountDownLatch stopped = new CountDownLatch(1);

        FakePartStorer(int partNumber, long size, CountDownLatch release, boolean failImmediately) {
            this.partNumber = partNumber;
            this.size = size;
            this.release = release;
            this.failImmediately = failImmediately;
        }

        @Override
        public int getPartNumber() {
            return partNumber;
        }

        @Override
        public long getPartSizeOrThrow() {
            return size;
        }

        @Override
        public B2Part storePart(B2LargeFileStorer storer, B2UploadListener listener, B2CancellationToken cancellationToken)
                throws IOException, B2Exception {
            token.set(cancellationToken);
            started.countDown();
            try {
                if (failImmediately) {
                    throw new B2InternalErrorException("simulated part failure");
                }
                // a worker that behaves like a real upload: it stops when interrupted or when it is told to cancel
                final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
                try {
                    while (!release.await(20, TimeUnit.MILLISECONDS)) {
                        if (cancellationToken != null && cancellationToken.isCancelled()) {
                            throw new B2LocalException("cancelled", "cancelled");
                        }
                        if (System.nanoTime() > deadline) {
                            break;
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new B2LocalException("interrupted", "interrupted");
                }
                return new B2Part(fileId(1), partNumber, size, makeSha1(partNumber), makeMd5(partNumber), 1, null);
            } finally {
                stopped.countDown();
            }
        }
    }

    private B2LargeFileStorer storerFor(List<B2PartStorer> parts) {
        return new B2LargeFileStorer(
                B2StoreLargeFileRequest.builder(fileId(9)).build(),
                parts,
                authCache,
                webifier,
                retryer,
                B2DefaultRetryPolicy.supplier(),
                pool,
                false);
    }

    /** Finding C6: an interrupt of the calling thread must be restored (flag set again) when storeFile reports failure. */
    @Category(KnownDefect.class)
    @Test
    public void testFindingC6_interruptFlagRestoredWhenStoreFileInterrupted() throws Exception {
        final CountDownLatch never = new CountDownLatch(1);
        final B2LargeFileStorer storer = storerFor(Arrays.asList(
                new FakePartStorer(1, 10, never, false),
                new FakePartStorer(2, 10, never, false)));
        Thread.currentThread().interrupt();
        try {
            storer.storeParts(null);
            fail("expected a failure because the thread is interrupted");
        } catch (B2Exception expected) {
            // expected
        }
        final boolean flagRestored = Thread.interrupted();
        never.countDown();
        assertTrue("interrupt flag was swallowed by storeParts", flagRestored);
    }

    /**
     * Finding C6: after the caller is interrupted, the part uploads that were in progress must stop.
     * The caller runs on its own thread; the test waits until both uploads have really started before
     * interrupting it, then waits (bounded) for each started upload to stop, by whatever means (cancellation
     * token or interrupt).
     */
    @Category(KnownDefect.class)
    @Test
    public void testFindingC6_partUploadsAreCancelledWhenCallerInterrupted() throws Exception {
        final CountDownLatch never = new CountDownLatch(1);
        final FakePartStorer part1 = new FakePartStorer(1, 10, never, false);
        final FakePartStorer part2 = new FakePartStorer(2, 10, never, false);
        final B2LargeFileStorer storer = storerFor(Arrays.<B2PartStorer>asList(part1, part2));

        final AtomicReference<Throwable> callerOutcome = new AtomicReference<>();
        final Thread caller = new Thread(() -> {
            try {
                storer.storeParts(null);
            } catch (Throwable t) {
                callerOutcome.set(t);
            }
        }, "C6-caller");
        caller.setDaemon(true);
        try {
            caller.start();
            assertTrue("part 1 never started", part1.started.await(5, TimeUnit.SECONDS));
            assertTrue("part 2 never started", part2.started.await(5, TimeUnit.SECONDS));

            caller.interrupt();

            caller.join(5000);
            assertTrue("storeParts did not return after its caller was interrupted", !caller.isAlive());
            assertTrue("part 1 upload was still running after the caller was interrupted",
                    part1.stopped.await(3, TimeUnit.SECONDS));
            assertTrue("part 2 upload was still running after the caller was interrupted",
                    part2.stopped.await(3, TimeUnit.SECONDS));
        } finally {
            never.countDown();
            caller.interrupt();
            pool.shutdownNow();
            pool.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    /** Finding C7: the exception raised for an interrupt must carry the InterruptedException as its cause. */
    @Category(KnownDefect.class)
    @Test
    public void testFindingC7_interruptedExceptionIsTheCause() throws Exception {
        final CountDownLatch never = new CountDownLatch(1);
        final B2LargeFileStorer storer = storerFor(Arrays.<B2PartStorer>asList(new FakePartStorer(1, 10, never, false)));
        Thread.currentThread().interrupt();
        try {
            storer.storeParts(null);
            fail("expected a failure");
        } catch (B2Exception e) {
            never.countDown();
            Thread.interrupted();
            assertTrue("cause was: " + e.getCause(), e.getCause() instanceof InterruptedException);
        }
    }

    /** Finding C8: when one part fails, the caller must see the error without waiting for the other parts to finish. */
    @Category(KnownDefect.class)
    @Test
    public void testFindingC8_firstPartFailureFailsFastWithoutWaitingForOtherParts() throws Exception {
        final CountDownLatch release = new CountDownLatch(1);
        final B2LargeFileStorer storer = storerFor(Arrays.<B2PartStorer>asList(
                new FakePartStorer(1, 10, release, true),
                new FakePartStorer(2, 10, release, false),
                new FakePartStorer(3, 10, release, false)));
        final CompletableFuture<List<B2Part>> future = storer.storePartsAsync(null);
        try {
            future.get(2, TimeUnit.SECONDS);
            fail("expected the failure of part 1");
        } catch (TimeoutException e) {
            fail("the failure of part 1 was not reported while parts 2 and 3 were still running");
        } catch (ExecutionException expected) {
            assertTrue(expected.getCause() instanceof B2InternalErrorException);
        } finally {
            release.countDown();
        }
    }

    private B2ContentSource sizedSource(long size) throws IOException {
        final B2ContentSource s = mock(B2ContentSource.class);
        org.mockito.Mockito.when(s.getContentLength()).thenReturn(size);
        return s;
    }

    /** Finding C22: start bytes must follow part-number order even when the caller lists the parts out of order. */
    @Test
    public void testFindingC22_startBytesComputedInPartNumberOrder() throws Exception {
        final List<B2PartStorer> outOfOrder = new ArrayList<>(Arrays.<B2PartStorer>asList(
                new B2UploadingPartStorer(2, sizedSource(500)),
                new B2UploadingPartStorer(1, sizedSource(100))));
        final B2LargeFileStorer storer = storerFor(outOfOrder);
        assertEquals(0L, storer.getStartByteOrUnknown(1));
        assertEquals(100L, storer.getStartByteOrUnknown(2));
        assertTrue(B2UploadProgress.UNKNOWN_PART_START_BYTE != storer.getStartByteOrUnknown(2));
    }
}
