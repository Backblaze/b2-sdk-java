/*
 * Copyright 2019, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client;

import com.backblaze.b2.client.exceptions.B2CannotComputeException;
import com.backblaze.b2.client.exceptions.B2Exception;
import com.backblaze.b2.client.exceptions.B2LocalException;
import com.backblaze.b2.client.structures.B2Part;
import com.backblaze.b2.client.structures.B2UploadListener;

import java.io.IOException;

/**
 * Implementations of B2PartStorer are responsible for storing a single
 * part of a large file in B2.
 */
public interface B2PartStorer {

    /**
     * @return The size of the part that this object will store, or throw if that
     * cannot be determined.
     */
    long getPartSizeOrThrow() throws B2CannotComputeException;

    /**
     * Store the part this B2PartStorer is responsible for.
     *
     * @param largeFileCreationManager The object managing the storage of the whole
     *                                 large file.
     * @param uploadListener The listener that tracks upload progress events.
     * @return The part that is stored, if successful.
     * @throws B2Exception if there's trouble.
     */
    B2Part storePart(
            B2LargeFileStorer largeFileCreationManager,
            B2UploadListener uploadListener) throws IOException, B2Exception;

}
