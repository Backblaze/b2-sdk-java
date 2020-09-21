/*
 * Copyright 2020, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.contentHandlers;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * B2ContentOutputStreamWriter is a subclass of B2ContentWriter which uses a Helper to
 * create the OutputStream to write to, and optionally, the InputStream to use to
 * reread and verify the SHA1.
 *
 * This is a B2ContentWriter which writes the incoming data into an output stream created
 * by the provided outputStreamCreator.  When it is constructed, it is give one or two Suppliers.
 * The outputStreamCreator must return a NEW OutputStream for writing to each time it is called.
 * The inputStreamCreator is optional.  If it is provided, it must return a NEW InputStream each
 * time it is called; that InputStream will be used to read back the written contents to verify
 * the SHA1 of the written content (if the SHA1 is available).
 *
 * Note that the arguments are Suppliers, not just stream objects because B2ContentWriters
 * need to be able to handle the SDK performing retries.  As mentioned above, be sure to
 * return a new Stream object each time your creators are called.
 */
public class B2ContentOutputStreamWriter extends B2ContentWriter {
    private final Helper helper;

    /**
     * A Helper provides the OutputStream(s) to write to and the InputStream(s)
     * to use to re-read the content after writing it.
     *
     * If you want to have the SHA1 verified by re-reading the written content,
     * override shouldVerifySha1ByRereadingFromDestination() to return true
     * and createInputStreamOrNull() to return a new InputStream on the written
     * content each time it is called.
     */
    public interface Helper {
        /**
         * This might be called multiple times.  Be sure to return a NEW OutputStream each time.
         * The Writer will write the entire contents of downloaded stream to the returned
         * OutputStream or fail while trying and possibly ask for another output stream.
         *
         * @return a new OutputStream to write the content to.
         * @throws IOException if there's trouble making the OutputStream.
         */
        OutputStream createOutputStream() throws IOException;

        /**
         * This might be called multiple times.  Be sure to return the same result every time.

         * @return whether or not we should try to verify the SHA1 be re-reading
         *         the content from the destination.  The Helper must return the
         *         same answer for its entire lifetime.
         */
        default boolean shouldVerifySha1ByRereadingFromDestination() {
            return false;
        }

        /**
         * This might be called multiple times.  Be sure to return a NEW InputStream each time.
         * The returned InputStream must start reading at the beginning of the data written
         * by the writer.

         * This will only be called if shouldVerifySha1ByRereadingFromDestination() returns true.
         * @return an InputStream to use to read the written content from its destination.
         * @throws IOException if there's trouble creating the InputStream.
         */
        default InputStream createInputStream() throws IOException {
            throw new RuntimeException("if you override shouldVerifySha1ByRereadingFromDestination() to return true, " +
                    "you need to implement createInputStream() too.");
        }

        /**
         * Called after the download succeeds.  This can be used to commit to the results of the download.
         * For instance, it could be a good time to move from a temp file name to a final filename.
         *
         * If you hit a serious problem, you will need to throw a RuntimeException to get the exception
         * all the way back to whomever called the download.
         */
        default void succeeded() {
        }
        /**
         * Called after the download fails (if it got as far as calling createOutputStream()).
         * This can be used to cleanup anything that was downloaded.  For instance, it could be a good time
         * to delete a temporary file that was used for the download.
         *
         * If you hit a serious problem, you will need to throw a RuntimeException to get the exception
         * all the way back to whomever called the download.
         */
        default void failed() {
        }
    }

    private B2ContentOutputStreamWriter(Helper helper) {
        super(helper.shouldVerifySha1ByRereadingFromDestination());
        this.helper = helper;
    }

    public static Builder builder(Helper helper) {
        return new Builder(helper);
    }

    @Override
    protected OutputStream createDestinationOutputStream() throws IOException {
        return helper.createOutputStream();
    }

    @Override
    protected InputStream createDestinationInputStream() throws IOException {
        return helper.createInputStream();
    }

    @Override
    protected void succeeded() {
        helper.succeeded();
    }

    @Override
    protected void failed() {
        helper.failed();
    }

    public static class Builder {
        private final Helper helper;

        private Builder(Helper helper) {
            this.helper = helper;
        }

        public B2ContentOutputStreamWriter build() {
            return new B2ContentOutputStreamWriter(helper);
        }
    }
}
