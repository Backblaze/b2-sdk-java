/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

import java.util.List;

public interface B2ListFilesResponse {

    /**
     * @return the files in this response.
     */
    List<B2FileVersion> getFiles();

    /**
     * @return true iff this is the last set of responses.
     */
    boolean atEnd();
}
