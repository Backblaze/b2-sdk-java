/*
 * Copyright 2018, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */

package com.backblaze.b2.client.structures;

import java.util.Objects;

public class B2ListKeysRequest {

    private final Integer maxKeyCount;
    private final String startApplicationKeyId;

    public B2ListKeysRequest(Integer maxKeyCount, String startApplicationKeyId) {
        this.maxKeyCount = maxKeyCount;
        this.startApplicationKeyId = startApplicationKeyId;
    }

    @SuppressWarnings("unused")
    public Integer getMaxKeyCount() {
        return maxKeyCount;
    }

    @SuppressWarnings("unused")
    public String getStartApplicationKeyId() {
        return startApplicationKeyId;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Integer maxKeyCount = null;
        private String startApplicationKeyId = null;

        @SuppressWarnings("unused")
        public Builder setMaxKeyCount(Integer maxKeyCount) {
            this.maxKeyCount = maxKeyCount;
            return this;
        }

        @SuppressWarnings("unused")
        public Builder setStartApplicationKeyId(String startApplicationKeyId) {
            this.startApplicationKeyId = startApplicationKeyId;
            return this;
        }

        public B2ListKeysRequest build() {
            return new B2ListKeysRequest(maxKeyCount, startApplicationKeyId);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        B2ListKeysRequest that = (B2ListKeysRequest) o;
        return Objects.equals(maxKeyCount, that.maxKeyCount) &&
                Objects.equals(startApplicationKeyId, that.startApplicationKeyId);
    }

    @Override
    public int hashCode() {

        return Objects.hash(maxKeyCount, startApplicationKeyId);
    }
}
