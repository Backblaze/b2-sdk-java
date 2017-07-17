/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */

package com.backblaze.b2.client.structures;

/**
 * B2BucketTypes provides constants for well-known bucket types.
 *
 * DESIGN NOTE: It is *not* an enum because new bucket types will be added
 *              over time and we want code to be able to get a bucket from
 *              the service, change an attribute, and use it in an update
 *              operation.  If we used an enum, even with the
 *              BzJson.defaultForInvalidEnumValue() annotation, we wouldn't
 *              be able to operate generically on buckets with unknown
 *              bucketTypes.  (hmmmm...b2_update_bucket could treat bucketType
 *              'unknown' as a way of saying "don't change".)
 */
public interface B2BucketTypes {
    // well-known types.
    String ALL_PRIVATE = "allPrivate";
    String ALL_PUBLIC = "allPublic";
}
