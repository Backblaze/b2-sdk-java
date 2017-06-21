/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.contentHandlers;

import com.backblaze.b2.util.B2FlushAndSyncFileOnCloseOutputStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * This is a B2ContentWriter which records the incoming data into a file on disk.
 *
 * It is careful to flush & sync the file when closing it.
 */
public class B2ContentFileWriter extends B2ContentWriter {
    private final File outputFile;

    private B2ContentFileWriter(File outputFile,
                                boolean verifySha1ByRereadingFromDisk) {
        super(verifySha1ByRereadingFromDisk);
        this.outputFile = outputFile;
    }

    public static Builder builder(File file) {
        return new Builder(file);
    }

    @Override
    protected OutputStream createDestinationOutputStream() throws IOException {
        return B2FlushAndSyncFileOnCloseOutputStream.create(outputFile);
    }

    @Override
    protected InputStream createDestinationInputStream() throws IOException {
        return new FileInputStream(outputFile);
    }

    public static class Builder {
        private final File outputFile;
        private boolean verifySha1ByRereadingFromDestination = true;

        private Builder(File file) {
            this.outputFile = file;
        }


        public B2ContentFileWriter build() {
            return new B2ContentFileWriter(outputFile, verifySha1ByRereadingFromDestination);
        }

        public Builder setVerifySha1ByRereadingFromDestination(boolean verifySha1ByRereadingFromDestination) {
            this.verifySha1ByRereadingFromDestination = verifySha1ByRereadingFromDestination;
            return this;
        }
    }
}
