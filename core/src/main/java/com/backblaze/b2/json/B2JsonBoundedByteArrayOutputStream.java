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
 * - stores stream content in a byte array
 * - expands capacity when needed (typically doubles)
 * - has a max capacity threshold
 * - throws IOException if the threshold is reached
 *
 * THREAD-SAFE
 */
public class B2JsonBoundedByteArrayOutputStream extends OutputStream {
    private static final int INITIAL_CAPACITY = 64;

    // byte array to hold output content
    private byte[] output;

    // the current number of bytes written so far
    private int size;

    // maximum capacity the output array is allowed to grow to
    private static final int MAX_CAPACITY = Integer.MAX_VALUE - 8;

    public B2JsonBoundedByteArrayOutputStream() {
        // initialize the outputBuffer to 64 bytes
        this.output = new byte[INITIAL_CAPACITY];
        this.size = 0;
    }

    /**
     * writes one byte into the output array
     *
     * @param i one byte
     * @throws IOException if new expanded capacity is over
     *                     the MAX_CAPACITY
     */
    @Override
    public synchronized void write(int i) throws IOException {
        // expand capacity if necessary
        if (size + 1 > output.length) {
            final int newCapacity = expandCapacity(size + 1);

            // allocate a new byte array with old content copied
            output = Arrays.copyOf(output, newCapacity);
        }

        output[size] = (byte) i;
        size++;
    }

    /**
     * writes an array of bytes into the output array
     *
     * @param bytes input array of bytes
     * @param offset offset for the input array
     * @param length number of bytes to write
     * @throws IOException if new expanded capacity is over
     *                     the MAX_CAPACITY
     */
    @Override
    public synchronized void write(byte[] bytes, int offset, int length) throws IOException {
        // expand capacity if necessary
        if (size + length > output.length) {
            final int newCapacity = expandCapacity(size + length);

            // allocate a new byte array with old content copied
            output = Arrays.copyOf(output, newCapacity);
        }

        // append the new content to the output, and increment size
        System.arraycopy(output, size, bytes, offset, length);
        size += length;
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
     * expands the current capacity to meet the least required capacity
     *
     * @param  leastCapacityRequired the least required capacity to hold all content
     * @return the new capacity
     * @throws IOException if new expanded capacity is over the MAX_CAPACITY
     */
    private int expandCapacity(int leastCapacityRequired) throws IOException {
        // double current capacity
        int newCapacity = output.length * 2;

        // is this new capacity enough to meet leastCapacityRequired
        if (newCapacity <  leastCapacityRequired) {
            newCapacity =  leastCapacityRequired;
        }

        // throw if we had hit the max capacity limit
        if (newCapacity > getMaxCapacity()) {
            throw new IOException("Requested array size exceeds maximum limit");
        }
        return newCapacity;
    }
}
