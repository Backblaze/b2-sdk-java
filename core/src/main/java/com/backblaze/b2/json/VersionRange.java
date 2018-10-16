/*
 * Copyright 2018, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */

package com.backblaze.b2.json;

/**
 * Holds a range of version numbers.
 *
 * Used to record which versions a field belongs to.  Version numbers start at 1.
 */
public class VersionRange {

    /**
     * Version range that includes all versions.
     */
    public static final VersionRange ALL_VERSIONS = new VersionRange(Integer.MIN_VALUE, Integer.MAX_VALUE);

    /**
     * The first version the field appears in.
     */
    private final int firstVersion;

    /**
     * The last version (inclusive!) that the field appears in.
     */
    private final int lastVersion;

    /**
     * Initializes
     */
    private VersionRange(int firstVersion, int lastVersion) {
        this.firstVersion = firstVersion;
        this.lastVersion = lastVersion;
    }

    /**
     * Factory for a range.
     */
    public static VersionRange range(int firstVersion, int lastVersion) throws B2JsonException {
        if (lastVersion < firstVersion) {
            throw new B2JsonException("last version " + lastVersion + " is before first version " + firstVersion);
        }
        return new VersionRange(firstVersion, lastVersion);
    }

    /**
     * Factory for a range with a start version that includes all later
     * versions.
     */
    public static VersionRange allVersionsFrom(int firstVersion) {
        return new VersionRange(firstVersion, Integer.MAX_VALUE);
    }

    /**
     * Does this range include the version?
     */
    public boolean includesVersion(int version) {
        return firstVersion <= version && version <= lastVersion;
    }
}
