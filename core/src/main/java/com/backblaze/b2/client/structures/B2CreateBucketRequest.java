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
    private final List<B2CorsRule> corsRules;
    private final List<B2LifecycleRule> lifecycleRules;
    private final boolean fileLockEnabled;
    private final B2BucketServerSideEncryption defaultServerSideEncryption;

    public B2CreateBucketRequest(String bucketName,
                                 String bucketType,
                                 Map<String, String> bucketInfo,
                                 List<B2CorsRule> corsRules,
                                 List<B2LifecycleRule> lifecycleRules,
                                 boolean fileLockEnabled,
                                 B2BucketServerSideEncryption defaultServerSideEncryption) {
        this.bucketName = bucketName;
        this.bucketType = bucketType;
        this.bucketInfo = bucketInfo;
        this.corsRules = corsRules;
        this.lifecycleRules = lifecycleRules;
        this.fileLockEnabled = fileLockEnabled;
        this.defaultServerSideEncryption = defaultServerSideEncryption;
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

    public List<B2CorsRule> getCorsRules() {
        return corsRules;
    }

    public List<B2LifecycleRule> getLifecycleRules() {
        return lifecycleRules;
    }

    public boolean isFileLockEnabled() {
        return fileLockEnabled;
    }

    public B2BucketServerSideEncryption getDefaultServerSideEncryption() {
        return defaultServerSideEncryption;
    }

    public static Builder builder(String bucketName, String bucketType) {
        return new Builder(bucketName, bucketType);
    }

    public static class Builder {
        private final String bucketName;
        private final String bucketType;

        private Map<String, String> bucketInfo;
        private List<B2CorsRule> corsRules;
        private List<B2LifecycleRule> lifecycleRules;
        private boolean fileLockEnabled;
        private B2BucketServerSideEncryption defaultServerSideEncryption;

        Builder(String bucketName,
                String bucketType) {
            this.bucketName = bucketName;
            this.bucketType = bucketType;
        }

        public Builder setBucketInfo(Map<String, String> bucketInfo) {
            this.bucketInfo = bucketInfo;
            return this;
        }

        public Builder setCorsRules(List<B2CorsRule> corsRules){
            this.corsRules = corsRules;
            return this;
        }

        public Builder setLifecycleRules(List<B2LifecycleRule> lifecycleRules) {
            this.lifecycleRules = lifecycleRules;
            return this;
        }

        public Builder setFileLockEnabled(boolean fileLockEnabled) {
            this.fileLockEnabled = fileLockEnabled;
            return this;
        }

        public Builder setDefaultServerSideEncryption(B2BucketServerSideEncryption defaultServerSideEncryption) {
            this.defaultServerSideEncryption = defaultServerSideEncryption;
            return this;
        }

        public B2CreateBucketRequest build() {
            return new B2CreateBucketRequest(
                    bucketName,
                    bucketType,
                    bucketInfo,
                    corsRules,
                    lifecycleRules,
                    fileLockEnabled,
                    defaultServerSideEncryption);
        }
    }
}
