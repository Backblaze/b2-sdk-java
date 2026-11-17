/*
 * Copyright 2026, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client;

import com.backblaze.b2.testing.KnownDefect;
import org.junit.experimental.categories.Category;
import com.backblaze.b2.client.structures.B2DownloadByIdRequest;
import com.backblaze.b2.client.structures.B2DownloadByNameRequest;
import com.backblaze.b2.client.webApiClients.B2WebApiClient;
import com.backblaze.b2.util.B2BaseTest;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;

public class B2WebifierUrlFindingsTest extends B2BaseTest {
    private final B2StorageClientWebifierImpl webifier = new B2StorageClientWebifierImpl(
            mock(B2WebApiClient.class), "agent/1.0", "https://api.testb2.com", null);

    /** Finding C24: an "&" or "=" in a file id must be encoded so it cannot inject extra download options. */
    @Category(KnownDefect.class)
    @Test
    public void testFindingC24_fileIdWithAmpersandIsEncodedInDownloadByIdUrl() throws Exception {
        final String url = webifier.getDownloadByIdUrl(B2TestHelpers.makeAuth(1),
                B2DownloadByIdRequest.builder("4_zid&b2ContentType=text/html").build());
        assertFalse("raw & survived in: " + url, url.substring(url.indexOf("fileId=")).contains("&"));
    }

    /** Finding C24: an "&" in a bucket name must be encoded in the download-by-name URL. */
    @Category(KnownDefect.class)
    @Test
    public void testFindingC24_bucketNameWithAmpersandIsEncodedInDownloadByNameUrl() throws Exception {
        final String url = webifier.getDownloadByNameUrl(B2TestHelpers.makeAuth(1),
                B2DownloadByNameRequest.builder("my&bucket", "file.txt").build());
        assertEquals("downloadUrl1/file/my%26bucket/file.txt", url);
    }
}
