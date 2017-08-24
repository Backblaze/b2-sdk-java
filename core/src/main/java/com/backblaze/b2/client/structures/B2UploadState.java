/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

/**
 * <p>
 * Generally, uploads follow this state diagram:
 * </p>
 *<pre>
 *   WAITING_TO_START--|
 *           |         |
 *           |         |
 *           v         |
 *        STARTING ----|
 *           |         |
 *           |         |
 *           v         |
 *        UPLOADING    |
 *        |       |    |
 *        |       |    |
 *        v       v    v
 *  SUCCEEDED     FAILED
 * </pre>
 *  Generally, the purpose of these updates is to provide high-level
 *  progress information.  Don't count on the rate at which you get the
 *  updates and don't count on seeing all of them.  For instance, if a
 *  part of an upload succeeded previously, you may only see an update
 *  for state SUCCEEDED.  Similarly, if the upload is canceled violently
 *  somehow you might not get any more updates.
 *
 *  If the upload hits retryable errors, it may jump back to an earlier state
 *  and the bytesSoFar may go back to zero.  Attempt count will be incremented.
 */
public enum B2UploadState {
    WAITING_TO_START,
    STARTING,
    UPLOADING,
    FAILED,
    SUCCEEDED
}
