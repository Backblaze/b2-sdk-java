/*
 * Copyright 2019, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client;

import com.backblaze.b2.client.credentialsSources.B2Credentials;
import com.backblaze.b2.client.credentialsSources.B2CredentialsFromEnvironmentSource;

/**
 * Implementations of B2StorageClientFactory can create a B2StorageClient from a B2ClientConfig.
 * There are a couple of convenience methods for constructing B2ClientConfigs.
 *
 * THREAD-SAFE.
 */
public interface B2StorageClientFactory {
    /**
     * @return a new B2StorageClientFactory that picks whichever of the built-in B2StorageClientFactorys is
     *         on the java class path.
     */
    static B2StorageClientFactory createDefaultFactory() {
        return new B2StorageClientFactoryPathBasedImpl();
    }

    /**
     * @param config the configuration to use.
     * @return a new B2StorageClient or throws a RuntimeException if it can't make one.
     */
    B2StorageClient create(B2ClientConfig config);

    /**
     * @param applicationKeyId the id of the secret to use to authenticate with the b2 servers
     * @param applicationKey the secret used to authenticate with the b2 servers.
     * @param userAgent the user agent to use when performing http requests.
     * @return a new B2StorageClient or throws a RuntimeException if it can't make one.
     */
    default B2StorageClient create(String applicationKeyId, String applicationKey, String userAgent) {
        final B2AccountAuthorizer accountAuthorizer = B2AccountAuthorizerSimpleImpl
                .builder(applicationKeyId, applicationKey)
                .build();
        final B2ClientConfig config = B2ClientConfig
                .builder(accountAuthorizer, userAgent)
                .build();
        return create(config);
    }

    /**
     * Gets the applicationKeyId and applicationKey from the environment and then
     *
     * @param userAgent the user agent to use when performing http requests.
     * @return a new B2StorageClient or throws a RuntimeException if it can't make one.
     * @throws RuntimeException if there's a problem getting the credentials from the environment or any other
     *                          problem creating the client.
     */
    default B2StorageClient create(String userAgent) {
        final B2Credentials credentials = B2CredentialsFromEnvironmentSource.build().getCredentials();
        return create(credentials.getApplicationKeyId(), credentials.getApplicationKey(), userAgent);
    }
}
