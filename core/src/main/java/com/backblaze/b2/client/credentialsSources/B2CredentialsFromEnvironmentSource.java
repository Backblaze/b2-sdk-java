/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.credentialsSources;

import com.backblaze.b2.util.B2Preconditions;

/**
 * This B2CredentialsSource reads the credentials from two environment variables:
 *   B2_APPLICATION_KEY_ID
 *   B2_APPLICATION_KEY
 *
 * If either is missing or empty, getCredentials() will throw.
 */
public class B2CredentialsFromEnvironmentSource implements B2CredentialsSource {
    private final B2Credentials credentials;
    private String errorMessageOrNull;

    private B2CredentialsFromEnvironmentSource() {
        B2Credentials tmp;
         try {
             final String applicationKeyId = getApplicationKeyIdOrThrow();
             final String appKey = System.getenv("B2_APPLICATION_KEY");
             B2Preconditions.checkState(appKey != null, "B2_APPLICATION_KEY must be set in the environment");
             B2Preconditions.checkState(!appKey.isEmpty(), "B2_APPLICATION_KEY must be non-empty.");

             tmp = new B2CredentialsImpl(applicationKeyId, appKey);
        } catch (IllegalStateException e) {
             tmp = null;
             errorMessageOrNull = e.toString();
        }
        credentials = tmp;
    }

    /**
     * @return the credentials specified in the environment.
     * @throws IllegalStateException if any of the required environment variables are missing.
     */
    @Override
    public B2Credentials getCredentials() {
        B2Preconditions.checkState(credentials != null, errorMessageOrNull);
        return credentials;
    }

    public static B2CredentialsSource build() {
        return new B2CredentialsFromEnvironmentSource();
    }

    /**
     * @return true iff we can create credentials from the environment.
     */
    public boolean canGetCredentials() {
        try {
            build().getCredentials();
            return true;
        } catch (IllegalStateException e) {
            return false;
        }
    }

    /**
     * Retrieves the application key ID from the environment or throws.
     *
     * Uses B2_APPLICATION_KEY_ID if available.
     *
     * Otherwise, for backwards compatibility, checks for B2_ACCOUNT_ID.
     *
     * @return the application key ID specified in the environment variable.
     * @throws IllegalArgumentException if a non-empty value can't be found.
     */
    private String getApplicationKeyIdOrThrow() throws IllegalArgumentException{
        String applicationKeyId = System.getenv("B2_APPLICATION_KEY_ID");

        if (applicationKeyId == null) {
            // maybe fallback to using the old variable
            final String accountId = System.getenv("B2_ACCOUNT_ID");
            if (accountId != null) {
                B2Preconditions.checkState(!accountId.isEmpty(),
                        "The B2_ACCOUNT_ID environment variable is empty, please use B2_APPLICATION_KEY_ID instead.");

                applicationKeyId = accountId;
            }
        }

        B2Preconditions.checkState(applicationKeyId != null, "B2_APPLICATION_KEY_ID must be set in the environment");
        B2Preconditions.checkState(!applicationKeyId.isEmpty(), "B2_APPLICATION_KEY_ID must be non-empty.");

        return applicationKeyId;
    }
}
