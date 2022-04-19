/*
 * Copyright 2021, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

/**
 * B2ReplicationStatus provides constants for cloud file replication
 */
public interface B2ReplicationStatus {

    String PENDING = "pending";
    String COMPLETED = "completed";
    String FAILED = "failed";
    String REPLICA = "replica";
}
