/*
 * Copyright 2019, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */

package com.backblaze.b2.json;

import com.backblaze.b2.util.B2Preconditions;

/**
 * Base class for all implementations of B2JsonTypeHandler that have an initialize() method.
 * <p>
 * (De)serialization is expected to be fast, so each implementation class gathers
 * information it needs when it's set up, so when it's time to run it has all of
 * the necessary information at hand.  This gets tricky because dependencies between
 * handlers may have loops.
 * <p>
 * The plan is to initialize in three phases:
 * <p>
 * First, the constructor, which must not depend on any other handlers, and which
 * must gather all of the information that other handlers will need.
 * <p>
 * Second, the initialize() method does any work that needs information from other
 * type handlers.
 * <p>
 * Third, check the validity of default values, now that all type handlers have
 * gone through at least the second phase.
 * <p>
 * All phases are protected by the lock on B2JsonHandlerMap, so they don't need to
 * lock, and the data they store in the object is guaranteed to be visible without
 * further locking.
 * <p>
 * Methods that return data set during initialize() should include this check:
 * <p>
 *     Preconditions.checkState(isInitialized());
 * <p>
 * NOTE: adding the initialize() method to BzJsonTypeHandler would change the interface
 * and break any clients who have written their own handlers.
 */
public abstract class B2JsonInitializedTypeHandler<T> implements B2JsonTypeHandler<T> {

    /**
     * Has the initialize() method been run?
     */
    private boolean initialized = false;

    /**
     * Does any setup that requires information from other handlers.
     *
     * This is package-private; we only expect this to be called from B2JsonHandlerMap
     * while it holds its lock.
     */
    void initialize(B2JsonHandlerMap b2JsonHandlerMap) throws B2JsonException {
        B2Preconditions.checkState(!initialized);
        initializeImplementation(b2JsonHandlerMap);
        initialized = true;
    }

    /**
     * Does any initialization specific to the concrete class.
     *
     * Override in classes that need to gather information from other handlers.
     */
    protected void initializeImplementation(B2JsonHandlerMap b2JsonHandlerMap) throws B2JsonException {}

    /**
     * Has the initialize method run?
     */
    protected boolean isInitialized() {
        return initialized;
    }
}
