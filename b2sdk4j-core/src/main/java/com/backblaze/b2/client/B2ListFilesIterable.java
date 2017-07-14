/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */

package com.backblaze.b2.client;

import com.backblaze.b2.client.structures.B2FileVersion;

/**
 * This interface collects the APIs we provide on our B2FileVersion iterables.
 * For now, it's just the Iterable-ness.  Someday, I expect it, or some interfaces
 * to provide some kind of "get resume point" functionality.
 */
public interface B2ListFilesIterable extends Iterable<B2FileVersion> {
}
