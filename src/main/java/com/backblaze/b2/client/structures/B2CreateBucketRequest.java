/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

import java.util.List;
import java.util.Map;

public class B2CreateBucketRequest {
    private final String bucketName;
    private final String bucketType;
    private final Map<String,String> bucketInfo;
    private final List<B2LifecycleRule> lifecycleRules;

    public B2CreateBucketRequest(String bucketName,
                                 String bucketType,
                                 Map<String, String> bucketInfo,
                                 List<B2LifecycleRule> lifecycleRules) {
        this.bucketName = bucketName;
        this.bucketType = bucketType;
        this.bucketInfo = bucketInfo;
        this.lifecycleRules = lifecycleRules;
    }

    public String getBucketName() {
        return bucketName;
    }

    public String getBucketType() {
        return bucketType;
    }

    public Map<String, String> getBucketInfo() {
        return bucketInfo;
    }

    public List<B2LifecycleRule> getLifecycleRules() {
        return lifecycleRules;
    }

    public static Builder builder(String bucketName, String bucketType) {
        return new Builder(bucketName, bucketType);
    }

    public static class Builder {
        private final String bucketName;
        private final String bucketType;

        private Map<String, String> bucketInfo;
        private List<B2LifecycleRule> lifecycleRules;

        Builder(String bucketName,
                String bucketType) {
            this.bucketName = bucketName;
            this.bucketType = bucketType;
        }

        public Builder setBucketInfo(Map<String, String> bucketInfo) {
            this.bucketInfo = bucketInfo;
            return this;
        }

        public Builder setLifecycleRules(List<B2LifecycleRule> lifecycleRules) {
            this.lifecycleRules = lifecycleRules;
            return this;
        }

        public B2CreateBucketRequest build() {
            return new B2CreateBucketRequest(bucketName, bucketType, bucketInfo, lifecycleRules);
        }
    }
}
