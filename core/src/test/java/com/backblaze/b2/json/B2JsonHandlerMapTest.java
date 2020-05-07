/*
 * Copyright 2019, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */


package com.backblaze.b2.json;

import com.backblaze.b2.util.B2BaseTest;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.HashSet;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class B2JsonHandlerMapTest extends B2BaseTest {

    @Rule
    public ExpectedException thrown  = ExpectedException.none();

    /**
     * When there's an error, it should not leave an uninitialized handler in the map.
     *
     * This a regression test for a bug where a handler was left in the map even though
     * its initialize() method threw an exception.
     */
    @Test
    public void testBadClassNotLeftInMap() {
        final B2JsonHandlerMap handlerMap = new B2JsonHandlerMap();

        try {
            handlerMap.getHandler(BadClassHolder.class);
            fail("should have thrown");
        } catch (Throwable t) {
            assertThat(t.getMessage(), containsString("BadClass has no constructor annotated with B2Json.constructor"));
        }

        try {
            handlerMap.getHandler(BadClassHolder.class);
            fail("should have thrown");
        } catch (Throwable t) {
            assertThat(t.getMessage(), containsString("BadClass has no constructor annotated with B2Json.constructor"));
        }
    }

    private static class BadClassHolder {
        @B2Json.required
        private final BadClass badClass;

        @B2Json.constructor(params = "badClass")
        public BadClassHolder(BadClass badClass) {
            this.badClass = badClass;
        }
    }

    private static class BadClass {
        @B2Json.required
        private int n;

        // error: no constructor
    }

    /**
     * The standard collections classes should not work at the top level.
     *
     * Ths is a regression test for a bug that was letting them slip through, but then
     * not serializing them properly.
     */
    @Test
    public void testSet() throws B2JsonException {
        final B2JsonHandlerMap handlerMap = new B2JsonHandlerMap();

        thrown.expectMessage("actualTypeArguments must be same length as class' type parameters");
        handlerMap.getHandler(HashSet.class);
    }

}
