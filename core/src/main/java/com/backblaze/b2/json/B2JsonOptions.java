/*
 * Copyright 2018, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */

package com.backblaze.b2.json;

/**
 * Options used by JSON serialization and de-serialization.
 */
public class B2JsonOptions {

    /**
     * The default set of options.
     */
    public static final B2JsonOptions DEFAULT = builder().build();

    /**
     * The default set of options, except that extra fields are ignored.
     */
    public static final B2JsonOptions DEFAULT_AND_ALLOW_EXTRA_FIELDS =
            builder().setExtraFieldOption(ExtraFieldOption.IGNORE).build();

    /**
     * What to do with extra fields found when de-serializing.
     */
    public enum ExtraFieldOption {
        ERROR,   // Throw an exception if an unexpected field is found.
        IGNORE   // Silently ignore extra fields.
    }

    /**
     * What to do with extra fields found when de-serializing.
     */
    private final ExtraFieldOption extraFieldOption;

    /**
     * What version of the (de-)serialized structure to use.
     *
     * This affects which fields are included in the structure.  Only fields that are
     * part of the given version are included.
     */
    private final int version;

    /**
     * Whether to redact sensitive fields
     *
     * When set, fields marked as @B2Json.sensitive will be redacted from the serialized
     * JSON and replaced with the string value "***REDACTED***"
     * The output will be valid Json but the structure/types will not conform to the expected
     * output. Use for logging situations where round-tripping the JSON is not required
     */
    private final boolean redactSensitive;

    /**
     * Initialize a new B2JsonOptions.
     */
    private B2JsonOptions(ExtraFieldOption extraFieldOption, int version, boolean redactSensitive) {
        this.extraFieldOption = extraFieldOption;
        this.version = version;
        this.redactSensitive = redactSensitive;
    }

    /**
     * What to do with extra fields found when de-serializing.
     */
    public ExtraFieldOption getExtraFieldOption() {
        return extraFieldOption;
    }

    /**
     * What version of the (de-)serialized structure to use.
     */
    public int getVersion() {
        return version;
    }

    /**
     * Redact sensitive fields from output
     */
    public boolean getRedactSensitive() {
        return redactSensitive;
    }

    /**
     * Returns a new builder for B2JsonOptions.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for B2JsonOptions
     */
    public static class Builder {

        private ExtraFieldOption extraFieldOption = ExtraFieldOption.ERROR;
        private int version = 1;
        private boolean redactSensitive = false;

        public Builder setExtraFieldOption(ExtraFieldOption extraFieldOption) {
            this.extraFieldOption = extraFieldOption;
            return this;
        }

        public Builder setRedactSensitive(boolean redactSensitive) {
            this.redactSensitive = redactSensitive;
            return this;
        }

        public Builder setVersion(int version) {
            this.version = version;
            return this;
        }

        public B2JsonOptions build() {
            return new B2JsonOptions(extraFieldOption, version, redactSensitive);
        }
    }
}
