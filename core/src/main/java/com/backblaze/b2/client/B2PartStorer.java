/*
 * Copyright 2019, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client;

import com.backblaze.b2.client.exceptions.B2Exception;
import com.backblaze.b2.client.structures.B2Part;

/**
 * Implementations of B2PartStorer are responsible for storing a single
 * part of a large file in B2.
 */
public interface B2PartStorer {

    /**
     * Store the part this B2PartStorer is responsible for.
     *
     * @param largeFileCreationManager The object managing the storage of the whole
     *                                 large file.
     * @return The part that is stored, if successful.
     * @throws B2Exception if there's trouble.
     */
    B2Part storePart(B2LargeFileStorer largeFileCreationManager) throws B2Exception;

}
