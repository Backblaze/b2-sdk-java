/*
 * Copyright 2026, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.testing;

/**
 * JUnit 4 category for a test that documents a defect that is still present in the code.
 *
 * <p>Tests carrying <code>@Category(KnownDefect.class)</code> are excluded from the default <code>test</code>
 * task, so they do not turn the build red, and run with <code>./gradlew knownDefectTests</code>.
 * A test is expected to FAIL there until the defect is fixed.  When it is fixed, remove the
 * annotation so the test joins the normal suite and protects the fix.  The test method name carries
 * the code-review finding id (for example testFindingC21_...).</p>
 */
public interface KnownDefect {
}
