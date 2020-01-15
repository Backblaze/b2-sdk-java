/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

import com.backblaze.b2.client.B2TestHelpers;
import com.backblaze.b2.client.contentSources.B2ContentTypes;
import com.backblaze.b2.json.B2Json;
import com.backblaze.b2.util.B2BaseTest;
import com.backblaze.b2.util.B2Collections;
import org.junit.Test;

import java.util.HashMap;

import static com.backblaze.b2.client.B2TestHelpers.fileId;
import static com.backblaze.b2.client.B2TestHelpers.fileName;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class B2FileVersionTest extends B2BaseTest {

    @Test
    public void test() {
        final B2FileVersion one = make(1);
        final B2FileVersion two = make(2);

        assertNotEquals(one, two);
        assertEquals(one, one);
        assertEquals(one, make(1));

        assertEquals("B2FileVersion{fileId='" + fileId(1) + "', contentLength=1000, contentType='text/plain', contentSha1='" + B2TestHelpers.SAMPLE_SHA1 + "', contentMd5='" + B2TestHelpers.SAMPLE_MD5 + "', action='upload', uploadTimestamp=1, fileInfo=[1], fileName='" + fileName(1) + "'}", one.toString());

        // just for code coverage.
        //noinspection ResultOfMethodCallIgnored
        one.hashCode();
    }

    @Test
    public void testActions() {
        checkAction(B2FileVersion.UPLOAD_ACTION, true, false, false, false);
        checkAction(B2FileVersion.HIDE_ACTION, false, true, false, false);
        checkAction(B2FileVersion.START_ACTION, false, false, true, false);
        checkAction(B2FileVersion.FOLDER_ACTION, false, false, false, true);
        checkAction(null, false, false, false, false);
    }

    @Test
    public void testJson() {
        final B2FileVersion fileVersion = make(1);
        assertEquals(
                fileVersion,
                B2Json.fromJsonOrThrowRuntime(
                        B2Json.toJsonOrThrowRuntime(fileVersion),
                        B2FileVersion.class
                )
        );
    }

    private void checkAction(String action, boolean expectUpload, boolean expectHide, boolean expectStart, boolean expectFolder) {
        B2FileVersion fileVersion =
                new B2FileVersion(
                        "fileId",
                        "fileName",
                        0L,
                        "contentType",
                        "contentSha1",
                        "contentMd5",
                        new HashMap<>(),
                        action,
                        0L
                );
        assertEquals(expectUpload, fileVersion.isUpload());
        assertEquals(expectHide, fileVersion.isHide());
        assertEquals(expectStart, fileVersion.isStart());
        assertEquals(expectFolder, fileVersion.isFolder());
    }

    private B2FileVersion make(int i) {
        return new B2FileVersion(
                fileId(i),
                fileName(i),
                i * 1000,
                B2ContentTypes.TEXT_PLAIN,
                B2TestHelpers.SAMPLE_SHA1,
                B2TestHelpers.SAMPLE_MD5,
                B2Collections.mapOf("key" + i, "value" + i),
                "upload",
                i);
    }
}
