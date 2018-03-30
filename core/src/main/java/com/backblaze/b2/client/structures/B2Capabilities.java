/*
 * Copyright 2017"; Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */

package com.backblaze.b2.client.structures;

/**
 * B2Capabilities provides constants capabilities in application keys.
 *
 * DESIGN NOTE: It is *not* an enum because new capabilities will be added
 *              over time and we want code to be able to get a key from
 *              the service and use the list of capabilities to create a new
 *              key with the same capabilities. operation.  If we used an enum,
 *              even with the BzJson.defaultForInvalidEnumValue() annotation,
 *              we wouldn't be able to operate generically on keys with unknown
 *              capabilities.
 */
@SuppressWarnings("unused")
public interface B2Capabilities {

    String LIST_KEYS = "listKeys";
    String WRITE_KEYS = "writeKeys";
    String DELETE_KEYS = "deleteKeys";

    String LIST_BUCKETS = "listBuckets";
    String WRITE_BUCKETS = "writeBuckets";
    String DELETE_BUCKETS = "deleteBuckets";

    String LIST_FILES = "listFiles";
    String READ_FILES = "readFiles";
    String SHARE_FILES = "shareFiles";
    String WRITE_FILES = "writeFiles";
    String DELETE_FILES = "deleteFiles";
}
