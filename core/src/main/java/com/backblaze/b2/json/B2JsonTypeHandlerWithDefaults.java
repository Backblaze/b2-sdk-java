/*
 * Copyright 2019, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */

package com.backblaze.b2.json;

/**
 * Base class for all handlers that deal with default values.
 */
public abstract class B2JsonTypeHandlerWithDefaults<T> extends B2JsonInitializedTypeHandler<T> {

    private enum DefaultValueState { NOT_CHECKED, GOOD, BAD }

    /**
     * Have we checked the default values, and are the good?
     *
     * Guarded by: this
     */
    private DefaultValueState defaultValueState = DefaultValueState.NOT_CHECKED;

    /**
     * If the state is BAD, the error message saying what's wrong.  Null otherwise.
     *
     * Guarded by: this
     */
    private String errorMessage;

    /**
     * Before (de)serializing, make sure that the defaults are OK.
     */
    protected synchronized void throwIfBadDefaultValue() throws B2JsonException {
        if (defaultValueState == DefaultValueState.BAD) {
            throw new B2JsonException(errorMessage);
        }
    }

    /**
     * Check the default values and remember if they are bad.
     */
    synchronized void checkDefaultValuesAndRememberResult() {
        try {
            checkDefaultValues();
            defaultValueState = DefaultValueState.GOOD;
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
        this.defaultValueState = DefaultValueState.BAD;
        this.errorMessage = errorMessage;
    }

    /**
     * Checks whether default values are OK, and throws if not.
     */
    protected abstract void checkDefaultValues() throws B2JsonException;

}
