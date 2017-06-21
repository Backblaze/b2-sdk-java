/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Wrap your FileOutputStreams with a B2FlushAndSyncFileOnCloseOutputStream to ensure that
 * your files are always sync'd before being closed.  See
 *     http://www.pointsoftware.ch/en/4-ext4-vs-ext3-filesystem-and-why-delayed-allocation-is-bad/
 */
public class B2FlushAndSyncFileOnCloseOutputStream extends OutputStream {
    private final FileOutputStream out;

    private B2FlushAndSyncFileOnCloseOutputStream(FileOutputStream out) {
        super();
        this.out = out;
    }

    @Override
    public void write(@SuppressWarnings("NullableProblems") byte[] b) throws IOException {
        out.write(b);
    }

    @Override
    public void write(@SuppressWarnings("NullableProblems") byte[] b, int off, int len) throws IOException {
        out.write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
        out.flush();
    }

    @Override
    public void close() throws IOException {
        out.flush();
        out.getFD().sync();
        out.close();
    }

    @Override
    public void write(int b) throws IOException {
        out.write(b);
    }

    /**
     * @param out the stream to wrap
     * @return a new stream which will carefully finish the file on disk when it's closed.
     */
    public static B2FlushAndSyncFileOnCloseOutputStream create(FileOutputStream out) {
        return new B2FlushAndSyncFileOnCloseOutputStream(out);
    }

    /**
     * @param outputFile the file to write to.  will be wrapped by a new FileOutputStream.
     * @return a new stream which will carefully finish the file on disk when it's closed.
     */
    public static B2FlushAndSyncFileOnCloseOutputStream create(File outputFile) throws FileNotFoundException {
        return create(new FileOutputStream(outputFile));
    }
}
