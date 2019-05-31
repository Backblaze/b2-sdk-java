/*
 * Copyright 2019, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client;

import com.backblaze.b2.client.exceptions.B2Exception;
import com.backblaze.b2.client.structures.B2Part;

import java.util.Objects;

/**
 * This implementation "stores" a part that has already been successfully
 * stored in B2. Its intended use is in retrying or resuming the storage
 * of a large file, after an attempt where some, but not all, parts were
 * successfully stored.
 *
 * When resuming the storage of a large part, use this class for parts
 * that are already stored, and do not need to be stored again.
 */
public class B2AlreadyStoredPartStorer implements B2PartStorer {

    private B2Part part;

    public B2AlreadyStoredPartStorer(B2Part part) {
        this.part = part;
    }

    @Override
    public B2Part storePart(B2LargeFileStorer largeFileCreationManager) throws B2Exception {
        return part;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        B2AlreadyStoredPartStorer that = (B2AlreadyStoredPartStorer) o;
        return Objects.equals(part, that.part);
    }
}

