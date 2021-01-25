/*
 * Copyright 2021, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.json;

import com.backblaze.b2.util.B2Preconditions;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

/**
 * A B2Json implementation of OutputStream that
 * - stores stream content in a byte array
 * - expands capacity when needed (typically doubles)
 * - has an upper bound limit on output array size (max capacity)
 * - throws IOException if the max capacity threshold is crossed.
 *
 *   The last behavior is different from standard Java ByteArrayOutputStream
 *   Implementation where an OutOfMemoryError would be thrown. OutOfMemoryError
 *   is typically interpreted as JVM running out of heap space, and JVM could be
 *   killed if configured so. This may not be desired for Java applications
 *   configured with heap space much larger than 2 GB, and such applications may
 *   want to continue to run after catching this array size threshold-crossing error.
 *
 * THREAD-SAFE
 */
public class B2JsonBoundedByteArrayOutputStream extends OutputStream {
    private static final int INITIAL_CAPACITY = 64;

    // slightly less than Integer.MAX_VALUE to be conservative
    private static final int SYSTEM_MAX_CAPACITY = Integer.MAX_VALUE - 8;

    // byte array to hold output content
    private byte[] output;

    // the current number of bytes written so far
    private int size;

    // maximum capacity the output array is allowed to grow to
    private final int maxCapacity;

    public B2JsonBoundedByteArrayOutputStream(int maxCapacity) {
        /* explicitly say the maxCapacity must be in the the range [64, Integer.MAX_VALUE - 8].
           maxCapacity has to be no less than initial capacity (64)
         */
        B2Preconditions.checkArgument(maxCapacity >= 64, "maxCapacity must not be less than " + INITIAL_CAPACITY);
        B2Preconditions.checkArgument(maxCapacity <= SYSTEM_MAX_CAPACITY, "maxCapacity must not be bigger than " + SYSTEM_MAX_CAPACITY);

        this.maxCapacity = maxCapacity;
        this.size = 0;

        // initialize the output array to 64 bytes
        this.output = new byte[INITIAL_CAPACITY];
    }

    /**
     * writes one byte into the output array
     *
     * @param i one byte
     * @throws IOException if expanding capacity would cross
     *                     the maxCapacity threshold
     */
    @Override
    public synchronized void write(int i) throws IOException {
        // check and expand capacity if needed
        checkCapacity(size + 1);

        output[size] = (byte) i;
        size++;
    }

    /**
     * writes an array of bytes into the output array
     *
     * @param bytes input array of bytes
     * @param offset offset for the input array
     * @param length number of bytes to write
     * @throws IOException if expanding capacity would cross
     *                     the maxCapacity threshold
     */
    @Override
    public synchronized void write(byte[] bytes, int offset, int length) throws IOException {
        final int newSize = size + length;
        // check and expand capacity if needed
        checkCapacity(newSize);

        // append the new content to the output, and reset size
        System.arraycopy(bytes, offset, output, size, length);
        size = newSize;
    }

    /**
     * makes a new String by decoding the bytes in output array
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
     * returns a copy of the output byte array
     *
     * @return a copy of output buffer
     */
    public synchronized byte[] toByteArray() {
        // make a copy of internal output content
        return Arrays.copyOf(output, size);
    }

    /**
     * returns the max capacity of output array
     *
     * @return the max capacity
     */
    public int getMaxCapacity() {
        return this.maxCapacity;
    }

    /**
     * returns the current output array size
     *
     * @return output output array size
     */
    public int getSize() {
        return this.size;
    }

    /**
     * checks and expands capacity if needed
     *
     * @param leastRequiredCapacity the lease capacity required
     * @throws IOException if expanding capacity would cross the max capacity threshold
     */
    private void checkCapacity(int leastRequiredCapacity) throws IOException {
        // expand capacity if necessary
        if (leastRequiredCapacity > output.length) {
            final int newCapacity = expandCapacity(leastRequiredCapacity);

            // allocate a new byte array with old content copied
            output = Arrays.copyOf(output, newCapacity);
        }
    }

    /**
     * determines a new capacity to meet the least required capacity
     *
     * @param  leastCapacityRequired the least required capacity to hold all content
     * @return the new capacity
     * @throws IOException if newly expanded capacity is bigger than the maxCapacity
     */
    private int expandCapacity(int leastCapacityRequired) throws IOException {
        int newCapacity = output.length * 2;

        // in case newCapacity is still not enough
        newCapacity = Math.max(newCapacity, leastCapacityRequired);

        // throw if we had hit the max capacity limit
        if (newCapacity > maxCapacity) {
            throw new IOException("Requested array size exceeds maximum limit");
        }
        return newCapacity;
    }
}
