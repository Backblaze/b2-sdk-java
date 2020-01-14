/*
 * Copyright 2020, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

/**
 * Interface defining getters for headers that can be overridden on download
 */
public interface B2OverrideableHeaders {
    String getB2ContentDisposition();

    String getB2ContentLanguage();

    String getB2Expires();

    String getB2CacheControl();

    String getB2ContentEncoding();

    String getB2ContentType();
}
