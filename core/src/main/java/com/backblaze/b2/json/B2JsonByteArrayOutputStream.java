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
 * the same way as the ByteArrayOutputStream from the standard Java version.
 *
 * THREAD-SAFE
 */
public class B2JsonByteArrayOutputStream extends OutputStream {

    /**
     * The maximum size of array to allocate. Reserve some header words
     * in an array. Attempts to allocate larger arrays will result in
     * IOException: Requested array size exceeds maximum limit.
     */
    private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

    /* The buffer where data are stored */
    protected byte[] buf;

    /* The number of valid bytes in the buffer */
    protected int count;

    /**
     * Creates a new byte array output stream. The buffer capacity is
     * initially 32 bytes, though its size increases if necessary.
     */
    public B2JsonByteArrayOutputStream() {
        this(32);
    }

    /**
     * Creates a new byte array output stream, with a buffer capacity of
     * the specified size, in bytes.
     *
     * @param size the initial size.
     * @exception IllegalArgumentException if size is negative.
     */
    public B2JsonByteArrayOutputStream(int size) {
        if (size < 0) {
            throw new IllegalArgumentException("Negative initial size: " + size);
        }
        buf = new byte[size];
    }

    /**
     * Increases the capacity if necessary to ensure that it can hold
     * at least the number of elements specified by the minimum
     * capacity argument.
     *
     * @param minCapacity the desired minimum capacity
     * @throws IOException if the minimum capacity would be over MAX_ARRAY_SIZE
     */
    private void ensureCapacity(int minCapacity) throws IOException {
        // overflow-conscious code
        if (minCapacity - buf.length > 0) {
            grow(minCapacity);
        }
    }

    /**
     * Increases the capacity to ensure that it can hold at least the
     * number of elements specified by the minimum capacity argument.
     *
     * @param minCapacity the desired minimum capacity
     * @throws IOException if new capacity would grow over MAX_ARRAY_SIZE
     */
    protected void grow(int minCapacity) throws IOException {
        // overflow-conscious code
        final int oldCapacity = buf.length;

        // double the current capacity
        int newCapacity = oldCapacity << 1;
        if (newCapacity - minCapacity < 0) {
            newCapacity = minCapacity;
        }

        // throws if the new capacity will be over the specified limit
        if (newCapacity - MAX_ARRAY_SIZE > 0) {
            throw new IOException("Requested array size exceeds maximum limit");
        }

        buf = Arrays.copyOf(buf, newCapacity);
    }

    /**
     * Writes the specified byte to this byte array output stream.
     *
     * @param b the byte to be written.
     * @throws IOException if new capacity would grow over MAX_ARRAY_SIZE
     */
    public synchronized void write(int b) throws IOException {
        ensureCapacity(count + 1);
        buf[count] = (byte) b;
        count += 1;
    }

    /**
     * Writes len bytes from the specified byte array
     * starting at offset off to this byte array output stream.
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
        ensureCapacity(count + len);

        // copy the len bytes from b into this array
        System.arraycopy(b, off, buf, count, len);
        count += len;
    }

    /**
     * Writes the complete contents of this byte array output stream to
     * the specified output stream argument, as if by calling the output
     * stream's write method using <code>out.write(buf, 0, count)</code>.
     *
     * @param out the output stream to which to write the data.
     * @exception IOException if an I/O error occurs.
     */
    public synchronized void writeTo(OutputStream out) throws IOException {
        out.write(buf, 0, count);
    }

    /**
     * Resets the count field of this byte array output stream to zero,
     * so that all currently accumulated output in the output stream is
     * discarded. The output stream can be used again, reusing the already
     * allocated buffer space.
     */
    public synchronized void reset() {
        count = 0;
    }

    /**
     * Creates a newly allocated byte array. Its size is the current
     * size of this output stream and the valid contents of the buffer
     * have been copied into it.
     *
     * @return the current contents of this output stream, as a byte array.
     */
    public synchronized byte[] toByteArray() {
        return Arrays.copyOf(buf, count);
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
        return new String(buf, 0, count);
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
        return new String(buf, 0, count, charsetName);
    }

    /**
     * Closing a <tt>ByteArrayOutputStream</tt> has no effect. The methods in
     * this class can be called after the stream has been closed without
     * generating an <tt>IOException</tt>.
     */
    public void close() throws IOException {
    }
}
