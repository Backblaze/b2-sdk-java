/*
 * Copyright 2020, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

/**
 * B2ServerSideEncryptionMode provides constants for the Server-Side Encryption Modes
 *
 */
public interface B2ServerSideEncryptionMode {

    String SSE_C = "SSE-C";     // Customer-provided encryption key
    String SSE_B2 = "SSE-B2";   // B2-managed encryption key
}
