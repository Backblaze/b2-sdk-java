/*
 * Copyright 2018, Backblaze Inc. All Rights Reserved.
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
    String LIST_ALL_BUCKET_NAMES = "listAllBucketNames";
    String WRITE_BUCKETS = "writeBuckets";
    String DELETE_BUCKETS = "deleteBuckets";
    String READ_BUCKETS = "readBuckets";

    String LIST_FILES = "listFiles";
    String READ_FILES = "readFiles";
    String SHARE_FILES = "shareFiles";
    String WRITE_FILES = "writeFiles";
    String DELETE_FILES = "deleteFiles";

    String READ_BUCKET_ENCRYPTION = "readBucketEncryption";
    String WRITE_BUCKET_ENCRYPTION = "writeBucketEncryption";

    String BYPASS_GOVERNANCE = "bypassGovernance";
    String READ_BUCKET_RETENTIONS = "readBucketRetentions";
    String WRITE_BUCKET_RETENTIONS = "writeBucketRetentions";
    String READ_FILE_RETENTIONS = "readFileRetentions";
    String WRITE_FILE_RETENTIONS = "writeFileRetentions";
    String READ_FILE_LEGAL_HOLDS = "readFileLegalHolds";
    String WRITE_FILE_LEGAL_HOLDS = "writeFileLegalHolds";

    String READ_BUCKET_REPLICATIONS = "readBucketReplications";
    String WRITE_BUCKET_REPLICATIONS = "writeBucketReplications";
}
