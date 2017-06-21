/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.util;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class B2IoUtils {
    private static final int EOF = -1;

    /**
     * Copies the contents of 'in' to 'out' stopping
     * when it hits the end of 'in' or when it hit an
     * exception.
     */
    @SuppressWarnings("UnusedReturnValue")
    public static long copy(InputStream in,
                            OutputStream out) throws IOException {
        return copyGuts(in, out, new byte[4 * 1024]);
    }

    /**
     * If closable isn't null, this will close it, ignoring any IOExceptions
     * that might happen.  This is especially useful in finally blocks.
     *
     * @param closeable the object to close.
     */
    public static void closeQuietly(Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (IOException ioe) {
            // ignore
        }
    }


    private static long copyGuts(InputStream in,
                                 OutputStream out,
                                 byte[] buffer) throws IOException {
        long count = 0;
        int n;
        while (EOF != (n = in.read(buffer))) {
            out.write(buffer, 0, n);
            count += n;
        }
        return count;
    }

    public static void readToEnd(InputStream in) throws IOException {
        readToEnd(in, new byte[4 * 1024]);
    }

    private static void readToEnd(InputStream in,
                                  byte[] scratchBuffer) throws IOException {
        //noinspection StatementWithEmptyBody
        while (in.read(scratchBuffer) != EOF) {
        }
    }
}
