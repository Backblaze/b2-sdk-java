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
 * If either is missing, getCredentials() will throw.
 */
public class B2CredentialsFromEnvironmentSource implements B2CredentialsSource {
    private final B2Credentials credentials;

    private B2CredentialsFromEnvironmentSource() {
        B2Credentials tmp;
         try {
             final String applicationKeyId = System.getenv("B2_APPLICATION_KEY_ID");
             B2Preconditions.checkState(applicationKeyId != null, "B2_APPLICATION_KEY_ID must be set in the environment");
             B2Preconditions.checkState(!applicationKeyId.isEmpty(), "B2_APPLICATION_KEY_ID must be non-empty.");

             final String appKey = System.getenv("B2_APPLICATION_KEY");
             B2Preconditions.checkState(appKey != null, "B2_APPLICATION_KEY must be set in the environment");
             B2Preconditions.checkState(!appKey.isEmpty(), "B2_APPLICATION_KEY must be non-empty.");

             tmp = new B2CredentialsImpl(applicationKeyId, appKey);
        } catch (IllegalStateException e) {
             tmp = null;
        }
        credentials = tmp;
    }

    /**
     * @return the credentials specified in the environment.
     * @throws IllegalStateException if any of the required environment variables are missing.
     */
    @Override
    public B2Credentials getCredentials() {
        B2Preconditions.checkState(credentials != null,
                "B2_APPLICATION_KEY_ID and B2_APPLICATION_KEY must be set to non-empty values in the environment.");
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
}
