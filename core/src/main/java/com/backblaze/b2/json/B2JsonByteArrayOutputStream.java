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
 * A B2Json version of ByteArrayOutputStream that throws IOException (as opposed to OutOfMemoryError)
 * when new capacity would be over the maximum limit (Integer.MAX_VALUE - 8). Everything else works
 * about the same way as the ByteArrayOutputStream from the standard Java version.
 *
 * THREAD-SAFE
 */
public class B2JsonByteArrayOutputStream extends OutputStream {

    /**
     * The maximum array size to allocate (leave some space for array
     * overhead). Will throw IOException: "Requested array size exceeds
     * maximum limit" if the new capacity would be over this threshold.
     *
     */
    private static final int maxCapacity = Integer.MAX_VALUE - 8;

    /* The output buffer to store bytes */
    protected byte[] outputBuffer;

    /* The number of bytes in the buffer */
    protected int count;

    /**
     * Creates a new byte array output stream with initial capacity
     * set to 64 bytes
     */
    public B2JsonByteArrayOutputStream() {
        // init streamBuffer to have 64-byte capacity to start
        outputBuffer = new byte[64];
    }

    /**
     * returns the max capacity
     *
     * @return maxCapacity
     */
    protected int getMaxCapacity() {
        return maxCapacity;
    }

    /**
     * Checks the capacity if necessary to ensure that it can store
     * at least the number of bytes specified by the minimum capacity
     *
     * @param minCapacity the desired minimum capacity
     * @throws IOException if the minimum capacity would be over maxCapacity
     */
    private void checkCapacity(int minCapacity) throws IOException {
        // overflow-conscious code
        if (minCapacity - outputBuffer.length > 0) {
            expandCapacity(minCapacity);
        }
    }

    /**
     * Expands the capacity to ensure that it can store at least the
     * number of elements specified by the minimum capacity
     *
     * @param minCapacity the desired minimum capacity
     * @throws IOException if new capacity would grow over maxCapacity
     */
    private void expandCapacity(int minCapacity) throws IOException {
        final int oldCapacity = outputBuffer.length;

        // double the current capacity
        int newCapacity = oldCapacity * 2;
        if (newCapacity - minCapacity < 0) {
            newCapacity = minCapacity;
        }

        // throws if the new capacity will be over the specified limit
        if (newCapacity - getMaxCapacity() > 0) {
            throw new IOException("Requested array size exceeds maximum limit");
        }

        outputBuffer = Arrays.copyOf(outputBuffer, newCapacity);
    }

    /**
     * Writes the specified byte to this {@code B2JsonByteArrayOutputStream}
     *
     * @param b the byte to be written.
     * @throws IOException if new capacity would grow over maxCapacity
     */
    public synchronized void write(int b) throws IOException {
        checkCapacity(count + 1);
        outputBuffer[count] = (byte) b;
        count += 1;
    }

    /**
     * Writes len bytes from the specified byte array
     * starting at offset off to this {@code B2JsonByteArrayOutputStream}
     *
     * @param b the data.
     * @param off the start offset in the data.
     * @param len the number of bytes to write.
     * @throws IOException if new capacity would grow over MAX_ARRAY_SIZE
     */
    public synchronized void write(byte[] b, int off, int len) throws IOException {
        if ((off < 0) || (off > b.length) || (len < 0) ||
                ((off + len) - b.length > 0)) {
            throw new IndexOutOfBoundsException();
        }
        checkCapacity(count + len);

        // copy the len bytes from b into this array
        System.arraycopy(b, off, outputBuffer, count, len);
        count += len;
    }

    /**
     * Creates a newly allocated byte array. Its size is the current
     * size of this output stream and the valid contents of the buffer
     * have been copied into it.
     *
     * @return the current contents of this output stream, as a byte array.
     */
    public synchronized byte[] toByteArray() {
        return Arrays.copyOf(outputBuffer, count);
    }

    /**
     * Returns the current size of the array buffer.
     *
     * @return the value of the count field, which is the number
     *         of valid bytes in this output stream.
     */
    public synchronized int size() {
        return count;
    }

    /**
     * Converts the buffer's contents into a string decoding bytes using the
     * platform's default character set. The length of the new String is a
     * function of the character set, and hence may not be equal to the size
     * of the buffer.
     *
     * <p> This method always replaces malformed-input and unmappable-character
     * sequences with the default replacement string for the platform's
     * default character set. The {@linkplain java.nio.charset.CharsetDecoder}
     * class should be used when more control over the decoding process is
     * required.
     *
     * @return String decoded from the buffer's contents.
     */
    public synchronized String toString() {
        return new String(outputBuffer, 0, count);
    }

    /**
     * Converts the buffer's contents into a string by decoding the bytes using
     * the named {@link java.nio.charset.Charset charset}. The length of the new
     * String is a function of the charset, and hence may not be equal to the
     * length of the byte array.
     *
     * <p> This method always replaces malformed-input and unmappable-character
     * sequences with this charset's default replacement string. The {@link
     * java.nio.charset.CharsetDecoder} class should be used when more control
     * over the decoding process is required.
     *
     * @param charsetName the name of a supported
     *        {@link java.nio.charset.Charset charset}
     * @return String decoded from the buffer's contents.
     * @exception UnsupportedEncodingException
     *            If the named charset is not supported
     */
    public synchronized String toString(String charsetName)
            throws UnsupportedEncodingException {
        return new String(outputBuffer, 0, count, charsetName);
    }

    /**
     * Closing a <tt>ByteArrayOutputStream</tt> has no effect. The methods in
     * this class can be called after the stream has been closed without
     * generating an <tt>IOException</tt>.
     */
    public void close() throws IOException {
    }
}
