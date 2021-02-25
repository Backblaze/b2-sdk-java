/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

import com.backblaze.b2.client.B2TestHelpers;
import com.backblaze.b2.client.contentSources.B2ContentTypes;
import com.backblaze.b2.client.exceptions.B2ForbiddenException;
import com.backblaze.b2.json.B2Json;
import com.backblaze.b2.util.B2BaseTest;
import com.backblaze.b2.util.B2Collections;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.HashMap;

import static com.backblaze.b2.client.B2TestHelpers.fileId;
import static com.backblaze.b2.client.B2TestHelpers.fileName;
import static junit.framework.TestCase.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class B2FileVersionTest extends B2BaseTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testToString() {
        final B2FileVersion one = make(1);
        final B2FileVersion two = make(2);

        assertNotEquals(one, two);
        assertEquals(one, one);
        assertEquals(one, make(1));

        assertEquals(
                "B2FileVersion{fileId='" + fileId(1) + "', " +
                        "contentLength=1000, contentType='text/plain', contentSha1='" + B2TestHelpers.SAMPLE_SHA1 + "', " +
                        "contentMd5='" + B2TestHelpers.SAMPLE_MD5 + "', " +
                        "action='upload', uploadTimestamp=1, fileInfo=[1], fileName='" + fileName(1) + "', " +
                        "fileLock='B2AuthorizationFilteredResponseField(isClientAuthorizedToRead=true,value={B2FileLock{mode=governance, retainUntilTimestamp=123456}})" + "', " +
                        "legalHold='B2AuthorizationFilteredResponseField(isClientAuthorizedToRead=true,value={B2LegalHold{status=on}})'" +  ", " +
                        "serverSideEncryption='null'}",
                        one.toString());
      
        // just for code coverage.
        //noinspection ResultOfMethodCallIgnored
        one.hashCode();
    }

    @Test
    public void testToStringNull() {

        final B2FileVersion one = new B2FileVersion(
                null,
                null,
                0L,
                null,
                null,
                null,
                null,
                null,
                0L,
                null,
               null,
                null
        );

        assertEquals(
                "B2FileVersion{fileId='null', " +
                        "contentLength=0, contentType='null', contentSha1='null', " +
                        "contentMd5='null', " +
                        "action='null', uploadTimestamp=0, fileInfo=[], fileName='null', " +
                        "fileLock='null', " +
                        "legalHold='null', " +
                        "serverSideEncryption='null'}",
                one.toString());
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
    public void testJson() throws B2ForbiddenException {
        final B2FileVersion fileVersion = make(1);
        assertEquals(
                fileVersion,
                B2Json.fromJsonOrThrowRuntime(
                        B2Json.toJsonOrThrowRuntime(fileVersion),
                        B2FileVersion.class
                )
        );

        assertTrue(fileVersion.isClientAuthorizedToReadFileLock());
        assertEquals(new B2FileLock("governance", 123456L), fileVersion.getFileLock());
        assertTrue(fileVersion.isClientAuthorizedToReadLegalHold());
        assertEquals(new B2LegalHold("on"), fileVersion.getLegalHold());
    }

    @Test
    public void testDefaultJson() {
        final String jsonString = "{\n" +
                "   \"fileName\": \"file.txt\",\n" +
                "   \"uploadTimestamp\": 12345\n" +
                "}";
        final B2FileVersion converted = B2Json.fromJsonOrThrowRuntime(
                jsonString,
                B2FileVersion.class);
        final B2FileVersion defaultVersion = new B2FileVersion(
                null,
                "file.txt",
                0L,
                null,
                null,
                null,
                null,
                null,
                12345L,
                null,
                null,
                null);
        assertEquals(defaultVersion, converted);
    }

    @Test
    public void testServerSideEncryption() throws B2ForbiddenException {
        final String jsonString = "{\n" +
                "   \"fileName\": \"file.txt\",\n" +
                "   \"serverSideEncryption\": {\n" +
                "      \"algorithm\": \"AES256\",\n" +
                "      \"mode\": \"SSE-B2\"\n" +
                "   },\n" +
                "   \"uploadTimestamp\": 12345\n" +
                "}";
        final B2FileVersion converted = B2Json.fromJsonOrThrowRuntime(
                jsonString,
                B2FileVersion.class);
        final B2FileVersion defaultVersion = new B2FileVersion(
                null,
                "file.txt",
                0L,
                null,
                null,
                null,
                null,
                null,
                12345L,
                null,
                null,
                new B2FileSseForResponse("SSE-B2", "AES256", null));

        assertEquals(defaultVersion, converted);
        assertNull(defaultVersion.getFileLock());
        assertNull(defaultVersion.getLegalHold());
    }

    @Test
    public void testClientUnAuthorizedToReadFileLock() throws B2ForbiddenException {
        final String jsonString = "{\n" +
                "   \"fileName\": \"file.txt\",\n" +
                "   \"fileLock\": {\n" +
                "      \"isClientAuthorizedToRead\": false,\n" +
                "      \"value\": null\n" +
                "   },\n" +
                "   \"uploadTimestamp\": 12345\n" +
                "}";
        final B2FileVersion converted = B2Json.fromJsonOrThrowRuntime(
                jsonString,
                B2FileVersion.class);
        final B2FileVersion defaultVersion = new B2FileVersion(
                null,
                "file.txt",
                0L,
                null,
                null,
                null,
                null,
                null,
                12345L,
                new B2AuthorizationFilteredResponseField<>(false, null),
                null,
                null);

        assertEquals(defaultVersion, converted);
        assertFalse(converted.isClientAuthorizedToReadFileLock());
        thrown.expect(B2ForbiddenException.class);
        converted.getFileLock();
    }

    @Test
    public void testClientUnAuthorizedToReadLegalHold() throws B2ForbiddenException {
        final String jsonString = "{\n" +
                "   \"fileName\": \"file.txt\",\n" +
                "   \"legalHold\": {\n" +
                "      \"isClientAuthorizedToRead\": false,\n" +
                "      \"value\": null\n" +
                "   },\n" +
                "   \"uploadTimestamp\": 12345\n" +
                "}";
        final B2FileVersion converted = B2Json.fromJsonOrThrowRuntime(
                jsonString,
                B2FileVersion.class);
        final B2FileVersion defaultVersion = new B2FileVersion(
                null,
                "file.txt",
                0L,
                null,
                null,
                null,
                null,
                null,
                12345L,
                null,
                new B2AuthorizationFilteredResponseField<>(false, null),
                null);

        assertEquals(defaultVersion, converted);
        assertFalse(converted.isClientAuthorizedToReadLegalHold());
        thrown.expect(B2ForbiddenException.class);
        converted.getLegalHold();
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
                        0L,
                        null,
                        null,
                        null
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
                i * 1000L,
                B2ContentTypes.TEXT_PLAIN,
                B2TestHelpers.SAMPLE_SHA1,
                B2TestHelpers.SAMPLE_MD5,
                B2Collections.mapOf("key" + i, "value" + i),
                "upload",
                i,
                new B2AuthorizationFilteredResponseField<>(true, new B2FileLock("governance", 123456L)),
                new B2AuthorizationFilteredResponseField<>(true, new B2LegalHold("on")),
                null);
    }
}
