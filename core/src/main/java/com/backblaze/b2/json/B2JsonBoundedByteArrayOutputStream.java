/*
 * Copyright 2021, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.json;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

/**
 * A B2Json implementation of OutputStream that
 * - stores output content in a byte array
 * - expands capacity to hold new content
 * - has a max capacity threshold
 * - throws IOException if the threshold is reached
 *
 * THREAD-SAFE
 */
public class B2JsonBoundedByteArrayOutputStream extends OutputStream {
    // byte array to hold output content
    private byte[] output;

    // the current number of bytes
    private int size;

    // maximum capacity the output array is allowed to grow to
    private static final int MAX_CAPACITY = Integer.MAX_VALUE - 8;

    public B2JsonBoundedByteArrayOutputStream() {
        // initialize the outputBuffer to 64 bytes
        this.output = new byte[64];
        this.size = 0;
    }

    /**
     * writes one byte into the output array
     *
     * @param b one byte
     * @throws IOException if new expanded capacity is over
     *                     the MAX_CAPACITY
     */
    @Override
    public synchronized void write(int b) throws IOException {
        // check if we need to expand the current capacity
        if (size + 1 > output.length) {
            expandCurrentCapacity(size + 1);
        }

        // store the byte and increment size
        output[size] = (byte) b;
        size++;
    }

    /**
     * writes an array of bytes into the output array
     *
     * @param b input array of bytes
     * @param off offset for the input array
     * @param len number of bytes to write
     * @throws IOException if new expanded capacity is over
     *                     the MAX_CAPACITY
     */
    @Override
    public synchronized void write(byte[] b, int off, int len) throws IOException {
        // validate the parameters
        validateParameters(b, off, len);

        // check if we need to expand the current capacity
        if (size + len > output.length) {
            expandCurrentCapacity(size + len);
        }

        // append the new content to the output, and increment size
        System.arraycopy(output, size, b, off, len);
        size += len;
    }

    /**
     * Constructs a new String by decoding the specified array of bytes
     * using the specified charset.
     *
     * @param charsetName the charset to be used in decoding
     * @return the string representation of output bytes after decoding
     * @throws UnsupportedEncodingException if the named charset is not supported
     */
    public synchronized String toString(String charsetName) throws UnsupportedEncodingException {
        return new String(output, 0, size, charsetName);
    }

    /**
     * returns a copy of output byte array
     *
     * @return a copy of output buffer
     */
    public synchronized byte[] toByteArray() {
        // make a copy of internal output content
        return Arrays.copyOf(output, size);
    }

    /**
     * returns the max capacity threshold
     *
     * @return MAX_CAPACITY
     */
    protected int getMaxCapacity() {
        return MAX_CAPACITY;
    }

    /**
     * expands the current capacity and create
     *
     * @param neededCapacity the needed capacity to hold all content
     * @throws IOException if new expanded capacity is over
     *                     the MAX_CAPACITY
     */
    private void expandCurrentCapacity(int neededCapacity) throws IOException {
        int newCapacity = output.length * 2;

        // is this new capacity enough to hold the new content
        if (newCapacity < neededCapacity) {
            newCapacity = neededCapacity;
        }

        // check if we had hit the max capacity limit
        if (newCapacity > getMaxCapacity()) {
            throw new IOException("Requested array size exceeds maximum limit");
        }

         /* for new capacity we need to:
            - allocate a new byte array
            - copy the original content to the new array
            - use the new array as the output byte array
          */
        output = Arrays.copyOf(output, newCapacity);
    }

    /* validates the parameters of write */
    private void validateParameters(byte[] b, int off, int len) {
        if ((off < 0) || (off > b.length) || (len < 0) ||
                ((off + len) - b.length > 0)) {
            throw new IllegalArgumentException();
        }
    }
}
