/*
 * Copyright 2023, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */

package com.backblaze.b2.json;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Base class for all handlers that deal with default values.
 */
public abstract class B2JsonTypeHandlerWithDefaults<T> extends B2JsonInitializedTypeHandler<T> {

    private enum DefaultValueState { NOT_CHECKED, GOOD, BAD }

    /**
     * Have we checked the default values, and are the good?
     */
    private final AtomicReference<DefaultValueState> defaultValueStateAtomicReference =
            new AtomicReference<>(DefaultValueState.NOT_CHECKED);

    /**
     * If the state is BAD, the error message saying what's wrong.  Null otherwise.
     *
     * Guarded by: this
     */
    private String errorMessage;

    /**
     * Before (de)serializing, make sure that the defaults are OK.
     */
    protected void throwIfBadDefaultValue() throws B2JsonException {
        if (defaultValueStateAtomicReference.get() == DefaultValueState.BAD) {
            throw new B2JsonException(errorMessage);
        }
    }

    /**
     * Check the default values and remember if they are bad.
     */
    synchronized void checkDefaultValuesAndRememberResult() {
        try {
            checkDefaultValues();
            defaultValueStateAtomicReference.set(DefaultValueState.GOOD);
        } catch (B2JsonException e) {
            setDefaultValueBad(e.getMessage());
        }
    }

    /**
     * Remember that the default value(s) are bad.
     *
     * Overridden by B2JsonUnionBaseHandler because it needs to propagate
     * the message to members of the union.
     */
    synchronized void setDefaultValueBad(String errorMessage) {
        defaultValueStateAtomicReference.set(DefaultValueState.BAD);
        this.errorMessage = errorMessage;
    }

    /**
     * Checks whether default values are OK, and throws if not.
     */
    protected abstract void checkDefaultValues() throws B2JsonException;

}
