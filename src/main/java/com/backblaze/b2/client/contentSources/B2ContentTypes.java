/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */

package com.backblaze.b2.client.contentSources;

/**
 * B2ContentTypes provides constants for well-known mime types, especially
 * ones that have meaning to B2.
 */
public interface B2ContentTypes {
    // well-known mime types.

    // if the file is uploaded with this mime-type, the server will assign
    // a mime-type, usually based on the fileName's extension.
    String B2_AUTO = "b2/x-auto";

    String APPLICATION_OCTET = "application/octet";

    String TEXT_PLAIN = "text/plain";
}
