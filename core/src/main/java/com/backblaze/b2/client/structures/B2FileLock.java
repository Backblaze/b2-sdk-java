/*
 * Copyright 2020, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

import com.backblaze.b2.client.contentSources.B2Headers;
import com.backblaze.b2.json.B2Json;
import com.backblaze.b2.util.B2Preconditions;

import java.util.Objects;

public class B2FileLock {

    /**
     * The B2FileLockMode, i.e. "governance" or "compliance", will be null if status != "on"
     */
    @B2Json.required
    private final String mode;

    /**
     * How long the file must be retained for (in millis since 1970), will be null if status != "on"
     */
    @B2Json.required
    private final Long retainUntilTimestamp;

    @B2Json.constructor(params = "mode, retainUntilTimestamp")
    public B2FileLock(String mode, Long retainUntilTimestamp) {
        B2Preconditions.checkArgument(mode != null && retainUntilTimestamp != null, "neither mode nor retainUntilTimestamp can be null");
        this.mode = mode;
        this.retainUntilTimestamp = retainUntilTimestamp;
    }

    /**
     * Construct a B2FileLock from B2Headers, or null if the required headers are not present
     * @param headers B2Headers
     * @return a new B2FileLock or null
     */
    public static B2FileLock getFileLockFromHeadersOrNull(B2Headers headers) {
        if (headers == null) {
            return null;
        }

        final String mode = headers.getFileLockRetentionModeOrNull();
        final Long retainUntilTimestamp = headers.getFileLockRetentionRetainUntilTimestampOrNull();

        if (mode == null || retainUntilTimestamp == null) {
            return null;
        }
        return new B2FileLock(mode, retainUntilTimestamp);
    }

    public String getMode() { return mode; }

    public Long getRetainUntilTimestamp() { return retainUntilTimestamp; }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        B2FileLock that = (B2FileLock) o;
        return Objects.equals(mode, that.getMode()) &&
                Objects.equals(retainUntilTimestamp, that.getRetainUntilTimestamp());
    }

    @Override
    public int hashCode() {
        return Objects.hash(mode, retainUntilTimestamp);
    }

    public String toString() {
        return "B2FileLock{" +
                "mode=" + mode + ", " +
                "retainUntilTimestamp=" + retainUntilTimestamp +
                '}';
    }
}
