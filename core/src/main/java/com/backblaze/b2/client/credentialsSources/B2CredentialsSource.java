/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.credentialsSources;

/**
 * A B2CredentialsSource returns credentials.
 */
public interface B2CredentialsSource {

    /**
     * @return credentials to use for the api.
     * @throws RuntimeException if there's any trouble finding the credentials.
     */
    B2Credentials getCredentials();
}
