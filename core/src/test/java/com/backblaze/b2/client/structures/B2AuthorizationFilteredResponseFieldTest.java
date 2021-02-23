/*
 * Copyright 2021, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */

package com.backblaze.b2.client.structures;

import com.backblaze.b2.client.exceptions.B2ForbiddenException;
import com.backblaze.b2.json.B2Json;
import com.backblaze.b2.util.B2BaseTest;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Objects;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class B2AuthorizationFilteredResponseFieldTest extends B2BaseTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    static class B2AuthorizationFilteredResponseFieldForTest {
        @B2Json.required
        private final B2AuthorizationFilteredResponseField<B2BucketServerSideEncryption> serverSideEncryption;

        @B2Json.constructor(params = "serverSideEncryption")
        private B2AuthorizationFilteredResponseFieldForTest(B2AuthorizationFilteredResponseField<B2BucketServerSideEncryption> serverSideEncryption) {
            this.serverSideEncryption = serverSideEncryption;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final B2AuthorizationFilteredResponseFieldForTest that = (B2AuthorizationFilteredResponseFieldForTest) o;
            return Objects.equals(serverSideEncryption,  that.serverSideEncryption);
        }

        @Override
        public int hashCode() {
            return Objects.hash(serverSideEncryption);
        }
    }

    @Test
    public void testNormalB2BucketServerSideEncryptionFromJson() throws B2ForbiddenException {
        final String jsonString = "{\n" +
                "  \"serverSideEncryption\": {\n" +
                "    \"isClientAuthorizedToRead\": true,\n" +
                "    \"value\": {\n" +
                "      \"algorithm\": \"AES256\",\n" +
                "      \"mode\": \"SSE-B2\"\n" +
                "    }\n" +
                "  }\n" +
                "}";
        final B2AuthorizationFilteredResponseFieldForTest converted =
                B2Json.fromJsonOrThrowRuntime(
                        jsonString,
                        B2AuthorizationFilteredResponseFieldForTest.class);

        assertTrue(converted.serverSideEncryption.isClientAuthorizedToRead());
        assertEquals(B2BucketServerSideEncryption.createSseB2Aes256(), converted.serverSideEncryption.getValue());
    }

    @Test
    public void testNullB2BucketServerSideEncryptionFromJson() throws B2ForbiddenException {
        final String jsonString = "{\n" +
                "  \"serverSideEncryption\": {\n" +
                "    \"isClientAuthorizedToRead\": true,\n" +
                "    \"value\": null\n" +
                "  }\n" +
                "}";
        final B2AuthorizationFilteredResponseFieldForTest converted =
                B2Json.fromJsonOrThrowRuntime(
                        jsonString,
                        B2AuthorizationFilteredResponseFieldForTest.class);

        assertTrue(converted.serverSideEncryption.isClientAuthorizedToRead());
        assertNull(converted.serverSideEncryption.getValue());
    }

    @Test
    public void testUnauthorizedB2BucketServerSideEncryptionFromJson() throws B2ForbiddenException {
        final String jsonString = "{\n" +
                "  \"serverSideEncryption\": {\n" +
                "    \"isClientAuthorizedToRead\": false,\n" +
                "    \"value\": null\n" +
                "  }\n" +
                "}";
        final B2AuthorizationFilteredResponseFieldForTest converted =
                B2Json.fromJsonOrThrowRuntime(
                        jsonString,
                        B2AuthorizationFilteredResponseFieldForTest.class);

        assertFalse(converted.serverSideEncryption.isClientAuthorizedToRead());

        thrown.expect(B2ForbiddenException.class);
        final B2BucketServerSideEncryption ignored = converted.serverSideEncryption.getValue();
    }

    @Test
    public void testNormalB2BucketServerSideEncryptionToJson() {
        final String expectedJsonString = "{\n" +
                "  \"serverSideEncryption\": {\n" +
                "    \"isClientAuthorizedToRead\": true,\n" +
                "    \"value\": {\n" +
                "      \"algorithm\": \"AES256\",\n" +
                "      \"mode\": \"SSE-B2\"\n" +
                "    }\n" +
                "  }\n" +
                "}";

        final B2AuthorizationFilteredResponseField<B2BucketServerSideEncryption> authorizedBucketSse =
                new B2AuthorizationFilteredResponseField<>(
                        true,
                        B2BucketServerSideEncryption.createSseB2Aes256()
                );

        assertTrue(authorizedBucketSse.isClientAuthorizedToRead());

        final String converted = B2Json.toJsonOrThrowRuntime(new B2AuthorizationFilteredResponseFieldForTest(authorizedBucketSse));

        assertEquals(expectedJsonString, converted);
    }

    @Test
    public void testNullB2BucketServerSideEncryptionToJson() {
        final String expectedJsonString = "{\n" +
                "  \"serverSideEncryption\": {\n" +
                "    \"isClientAuthorizedToRead\": true,\n" +
                "    \"value\": null\n" +
                "  }\n" +
                "}";

        final B2AuthorizationFilteredResponseField<B2BucketServerSideEncryption> authorizedBucketSse =
                new B2AuthorizationFilteredResponseField<>(
                        true,
                        null
                );

        assertTrue(authorizedBucketSse.isClientAuthorizedToRead());

        final String converted = B2Json.toJsonOrThrowRuntime(new B2AuthorizationFilteredResponseFieldForTest(authorizedBucketSse));

        assertEquals(expectedJsonString, converted);
    }

    @Test
    public void testUnauthorizedB2BucketServerSideEncryptionToJson() {
        final String expectedJsonString = "{\n" +
                "  \"serverSideEncryption\": {\n" +
                "    \"isClientAuthorizedToRead\": false,\n" +
                "    \"value\": null\n" +
                "  }\n" +
                "}";

        final B2AuthorizationFilteredResponseField<B2BucketServerSideEncryption> unauthorizedBucketSse =
                new B2AuthorizationFilteredResponseField<>(
                        false,
                        null
                );

        assertFalse(unauthorizedBucketSse.isClientAuthorizedToRead());

        final String converted = B2Json.toJsonOrThrowRuntime(new B2AuthorizationFilteredResponseFieldForTest(unauthorizedBucketSse));

        assertEquals(expectedJsonString, converted);
    }
}