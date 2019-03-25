/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client;

import com.backblaze.b2.client.exceptions.B2Exception;
import com.backblaze.b2.client.structures.B2AuthorizeAccountRequest;
import com.backblaze.b2.util.B2BaseTest;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static com.backblaze.b2.client.structures.B2TestMode.FAIL_SOME_UPLOADS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class B2ClientConfigTest extends B2BaseTest {
    private static final String USER_AGENT = "B2ClientConfigTest/0.0.1";
    private final B2AccountAuthorizer AUTHORIZER = webifier -> {
        throw new RuntimeException("not expected to be called!");
    };


    @Test
    public void testMinimal() {
        final B2ClientConfig config = B2ClientConfig
                .builder(AUTHORIZER, USER_AGENT)
                .build();
        assertEquals(AUTHORIZER, config.getAccountAuthorizer());
        assertEquals(USER_AGENT, config.getUserAgent());
        assertNull(config.getMasterUrl());
        assertNull(config.getTestModeOrNull());
    }

    @Test
    public void testMaximal() {
        final B2ClientConfig config = B2ClientConfig
                .builder(AUTHORIZER, USER_AGENT)
                .setMasterUrl("https://api.backblazeb2.net/")
                .setTestModeOrNull(FAIL_SOME_UPLOADS)
                .build();
        assertEquals(AUTHORIZER, config.getAccountAuthorizer());
        assertEquals(USER_AGENT, config.getUserAgent());
        assertEquals("https://api.backblazeb2.net/", config.getMasterUrl());
        assertEquals(FAIL_SOME_UPLOADS, config.getTestModeOrNull());
    }

    @Test
    public void testSimpleBuilder() throws B2Exception {
        final B2ClientConfig config = B2ClientConfig
                .builder("applicationKeyId", "applicationKey", USER_AGENT)
                .build();

        assertEquals(USER_AGENT, config.getUserAgent());

        final B2AccountAuthorizer authorizer = config.getAccountAuthorizer();
        assertTrue(authorizer instanceof B2AccountAuthorizerSimpleImpl);

        B2StorageClientWebifier webifier = mock(B2StorageClientWebifier.class);
        authorizer.authorize(webifier);

        ArgumentCaptor<B2AuthorizeAccountRequest> requestCaptor = ArgumentCaptor.forClass(B2AuthorizeAccountRequest.class);
        verify(webifier, times(1)).authorizeAccount(requestCaptor.capture());
        final B2AuthorizeAccountRequest request = requestCaptor.getValue();
        assertEquals("applicationKeyId", request.getApplicationKeyId());
        assertEquals("applicationKey", request.getApplicationKey());
    }

    @Test
    public void test_forCoverage() {
        ////////////
        // cover equals()
        ////////////

        final B2ClientConfig config = B2ClientConfig
                .builder(AUTHORIZER, USER_AGENT)
                .build();

        final B2ClientConfig config2 = B2ClientConfig
                .builder(AUTHORIZER, USER_AGENT)
                .build();
        assertEquals(config, config2);

        ////////////
        // cover hashCode()
        ////////////

        //noinspection ResultOfMethodCallIgnored
        config.hashCode();
    }

}
