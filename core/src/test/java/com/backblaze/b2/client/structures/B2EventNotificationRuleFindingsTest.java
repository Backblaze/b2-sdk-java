/*
 * Copyright 2026, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

import com.backblaze.b2.util.B2BaseTest;
import org.junit.Test;

import java.util.TreeSet;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

public class B2EventNotificationRuleFindingsTest extends B2BaseTest {
    /** Finding C36: a rule built with the 5-argument constructor must answer isSuspended() (false), not throw a NullPointerException. */
    @Test
    public void testFindingC36_isSuspendedOnFiveArgConstructorReturnsFalse() {
        final B2EventNotificationRule rule = new B2EventNotificationRule(
                "myRule", new TreeSet<String>(), "prefix/", null, true);
        try {
            assertFalse(rule.isSuspended());
        } catch (NullPointerException e) {
            fail("isSuspended() threw NullPointerException for a rule that was never suspended");
        }
    }
}
