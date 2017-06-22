/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.contentHandlers;

import com.backblaze.b2.client.contentSources.B2Headers;
import com.backblaze.b2.client.exceptions.B2Exception;

import java.io.IOException;
import java.io.InputStream;

/**
 * Implement B2ContentSink to process files downloaded from B2.
 *
 * There are some common implementations, such as ContentSaver which
 * writes downloaded content to disk.
 */
public interface B2ContentSink {
    /**
     * Reads the data from the input stream.
     *
     * Does NOT need to close the stream.  The caller of readContent()
     * will take care of that.
     *
     * If you don't read to the end of the inputStream, the connection
     * probably won't be re-usable, so read to the end unless you have
     * a good reason not to.
     *
     * @param responseHeaders the headers from the response
     * @param in an input stream to read the content of the response
     * @throws B2Exception if there's trouble
     * @throws IOException if there's an IOException
     */
    void readContent(B2Headers responseHeaders,
                     InputStream in) throws B2Exception, IOException;
}
