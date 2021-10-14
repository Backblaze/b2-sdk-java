/*
 * Copyright 2020, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.contentSources;

import com.backblaze.b2.util.B2Preconditions;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static com.backblaze.b2.util.B2StringUtil.percentDecode;
import static com.backblaze.b2.util.B2StringUtil.startsWithIgnoreCase;

/**
 * B2Headers represents the HTTP headers that come with a response from the server.
 *
 * It stores the values as they are *set* on it, without modifying or encoding them.
 * Callers are expected to store values as they should be encoded in HTTP requests.
 *
 * Note that getValueOrNull() returns the stored value without percentDecoding.
 * Note that some convenience methods do percentDecode the value.  See the per-method
 * documentation.
 */
// XXX: is the way that the convenience methods handle trouble consistent enough?
//      on the upside, they're just convenience methods and a client can always use
//      getNames() & getValueOrNull() to do their own processing.
public interface B2Headers {
    // B2Headers used by public APIs:
    String FILE_ID = "X-Bz-File-Id";
    String FILE_NAME = "X-Bz-File-Name";
    String CONTENT_SHA1 = "X-Bz-Content-Sha1";
    String TEST_MODE = "X-Bz-Test-Mode";
    String PART_NUMBER = "X-Bz-Part-Number";
    String UPLOAD_TIMESTAMP = "X-Bz-Upload-Timestamp";
    String FILE_INFO_PREFIX = "X-Bz-Info-";

    // some values for CONTENT_SHA1:
    String HEX_DIGITS_AT_END = "hex_digits_at_end";

    // well-known file-info names & their header names.
    String SRC_LAST_MODIFIED_MILLIS_INFO_NAME = "src_last_modified_millis";
    String SRC_LAST_MODIFIED_MILLIS = FILE_INFO_PREFIX + SRC_LAST_MODIFIED_MILLIS_INFO_NAME;

    String LARGE_FILE_SHA1_INFO_NAME = "large_file_sha1";
    String LARGE_FILE_SHA1 = FILE_INFO_PREFIX + LARGE_FILE_SHA1_INFO_NAME;

    // some standard headers
    String AUTHORIZATION = "Authorization";
    String CONTENT_LENGTH = "Content-Length";
    String CONTENT_MD5 = "Content-MD5";
    String CONTENT_TYPE = "Content-Type";
    String RANGE = "Range";                  // for range requests.
    String CONTENT_RANGE = "Content-Range";  // for range responses.
    String RETRY_AFTER = "Retry-After";
    String USER_AGENT = "User-Agent";
    String EXPECT = "Expect";

    // Header used for authorization-filtered response fields
    String CLIENT_UNAUTHORIZED_TO_READ = "X-Bz-Client-Unauthorized-To-Read";

    // some headers for File Lock
    String FILE_RETENTION_MODE = "X-Bz-File-Retention-Mode";
    String FILE_RETENTION_RETAIN_UNTIL_TIMESTAMP = "X-Bz-File-Retention-Retain-Until-Timestamp";
    String FILE_LEGAL_HOLD = "X-Bz-File-Legal-Hold";

    // some headers for Server Side Encryption
    String SERVER_SIDE_ENCRYPTION = "X-Bz-Server-Side-Encryption";
    String SERVER_SIDE_ENCRYPTION_CUSTOMER_ALGORITHM = "X-Bz-Server-Side-Encryption-Customer-Algorithm";
    String SERVER_SIDE_ENCRYPTION_CUSTOMER_KEY = "X-Bz-Server-Side-Encryption-Customer-Key";
    String SERVER_SIDE_ENCRYPTION_CUSTOMER_KEY_MD5 = "X-Bz-Server-Side-Encryption-Customer-Key-Md5";

    // header for Cloud Replication
    String REPLICATION_STATUS = "X-Bz-Replication-Status";

    /**
     * @return a collection with the names of all the headers in this object.  never null.
     */
    Collection<String> getNames();

    /**
     * @return the value for the given header name, or null if none.
     */
    String getValueOrNull(String name);

    /**
     * @return the value of the Content-Type header or B2ContentTypes.APPLICATION_OCTET if there isn't one.
     * @apiNote this returns APPLICATION_OCTET because that's how you should treat content with an undeclared type
     *          (unless you're willing to look at other headers or the URI, etc.  if you're willing to do that
     *           you can use getValueOrNull() instead of this convenience method.)
     */
    default String getContentType() {
        final String str = getValueOrNull(CONTENT_TYPE);
        return (str != null) ? str : B2ContentTypes.APPLICATION_OCTET;
    }

    /**
     * @return the value of the Content-Length header as a long.
     * @throws IllegalStateException if there isn't a Content-Length header or it can't be parsed as a long.
     * @apiNote We throw for unparseable values because this is a standard HTTP header and really, really should be valid.
     */
    default long getContentLength() {
        final String str = getValueOrNull(B2Headers.CONTENT_LENGTH);
        B2Preconditions.checkState(str != null, "don't call if there isn't a Content-Length!");

        try {
            return Long.parseLong(str);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("can't parse Content-Length '" + str + "' as a long: " + e, e);
        }
    }

    /**
     * @return the value of the X-Bz-File-Name header (percentDecoded)
     *         or null if that header isn't present or can't be percentDecoded.
     * @apiNote We return null here because this is a completely optional, non-standard header
     *          and anyone could put any type of value in it at any time.
     */
    default String getFileNameOrNull() {
        final String str = getValueOrNull(B2Headers.FILE_NAME);
        if (str == null) {
            return null;
        }

        try {
            return percentDecode(str);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * @return the value of the X-Bz-Upload-Timestamp header (parsed as a long)
     *         or null if that header isn't present or can't be parsed.
     * @apiNote We return null here because this is a completely optional, non-standard header
     *          and anyone could put any type of value in it at any time.
     */
    default Long getUploadTimestampOrNull() {
        final String str = getValueOrNull(B2Headers.UPLOAD_TIMESTAMP);
        if (str == null) {
            return null;
        }

        try {
            return Long.parseLong(str);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * @return true iff there's a value for the Content-Range header
     */
    default boolean hasContentRange() {
        return getValueOrNull(B2Headers.CONTENT_RANGE) != null;
    }

    /**
     * @return the value of the X-Bz-Content-Sha1 header, or null if none.
     * @apiNote We return null here instead of throwing an exception since this is a
     *          completely optional, non-standard header.
     */
    default String getContentSha1OrNull() {
        return getValueOrNull(CONTENT_SHA1);
    }

    /**
     * @return the value of the Content-MD5 header, or null if none.
     * @apiNote We return null here instead of throwing an exception since this is a
     *          an optional header in B2.  Older files will not have it.
     */
    default String getContentMd5OrNull() {
        return getValueOrNull(CONTENT_MD5);
    }

    /**
     * @return the value of the X-Bz-Content-Sha1 header with 'unverified:' removed from
     *         the front, if any, or null if there's no X-Bz-Content-Sha1 header.
     * @apiNote We return null here instead of throwing an exception since this is a
     *          completely optional, non-standard header.
     */
    default String getContentSha1EvenIfUnverifiedOrNull() {
        final String unverifiedPrefix = "unverified:";
        final String s = getContentSha1OrNull();
        if (s != null && s.startsWith(unverifiedPrefix)) {
            return s.substring(unverifiedPrefix.length());
        }
        return s;
    }

    /**
     * @return the value of the X-Bz-Content-Sha1 header, or null if none.
     * @apiNote We return null here instead of throwing an exception since this is a
     *          completely optional, non-standard header.
     */
    default String getLargeFileSha1OrNull() {
        return getValueOrNull(LARGE_FILE_SHA1);
    }

    /**
     * @return a new map with the names and values provided as fileInfo when the file was uploaded.
     * @apiNote the map may be empty, but it will never be null.
     */
    default Map<String,String> getB2FileInfo() {
        final Map<String,String> info = new TreeMap<>();
        for (String name : getNames()) {
            if (startsWithIgnoreCase(name, FILE_INFO_PREFIX)) {
                final String shortName = name.substring(FILE_INFO_PREFIX.length());
                // The uploader no longer encodes the name, so only decode the value
                info.put(shortName, percentDecode(getValueOrNull(name)));
            }
        }
        return info;
    }

    /**
     * @return the value of the 'src_last_modified_millis' fileInfo or null if none
     *         or if it can't be parsed as a long.
     * @apiNote We return null here because this is a completely optional, non-standard header
     *          and anyone could put any type of value in it at any time.
     */
    default Long getSrcLastModifiedMillis() {
        final String str = getValueOrNull(B2Headers.SRC_LAST_MODIFIED_MILLIS);
        if (str == null) {
            return null;
        }

        try {
            return Long.parseLong(str);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * @return the list of headers, if any, included in the `X-Bz-Client-Unauthorized-To-Read` header.
     * If no such header is present, then returns null.
     * @apiNote We return null here instead of throwing an exception since this is a
     *          completely optional, non-standard header.
     */
    @SuppressWarnings("unchecked")
    default List<String> getClientUnauthorizedToReadHeaderList() {
        final String unauthorizedHeaders = getValueOrNull(CLIENT_UNAUTHORIZED_TO_READ);
        if (unauthorizedHeaders == null) {
            return Collections.EMPTY_LIST;
        }

        return Arrays.asList(unauthorizedHeaders.split(","));
    }

    /**
     * @return true if the client does not have app key capabilities to read file retention,
     * according to the 'X-Bz-Client-Unauthorized-To-Read' header
     */
    default boolean isClientUnauthorizedToReadFileRetention() {
        return getClientUnauthorizedToReadHeaderList().contains(FILE_RETENTION_MODE);
    }

    /**
     * @return the value of the `X-Bz-File-Retention-Mode` header, or null if none.
     * @apiNote We return null here instead of throwing an exception since this is a
     *          completely optional, non-standard header.
     */
    default String getFileRetentionModeOrNull() {
        return getValueOrNull(FILE_RETENTION_MODE);
    }

    /**
     * @return the value of the `X-Bz-File-Retain-Until-Timestamp` header, or null if none.
     * @apiNote We return null here because this is a completely optional, non-standard header
     *          and anyone could put any type of value in it at any time.
     */
    default Long getFileRetentionRetainUntilTimestampOrNull() {
        final String str = getValueOrNull(B2Headers.FILE_RETENTION_RETAIN_UNTIL_TIMESTAMP);
        if (str == null) {
            return null;
        }

        try {
            return Long.parseLong(str);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * @return true if the client does not have app key capabilities to read legal hold,
     * according to the 'X-Bz-Client-Unauthorized-To-Read' header
     */
    default boolean isClientUnauthorizedToReadLegalHold() {
        return getClientUnauthorizedToReadHeaderList().contains(FILE_LEGAL_HOLD);
    }

    /**
     * @return the value of the `X-Bz-File-Legal-Hold` header, or null if none.
     * @apiNote We return null here instead of throwing an exception since this is a
     *          completely optional, non-standard header.
     */
    default String getFileLegalHoldOrNull() {
        return getValueOrNull(FILE_LEGAL_HOLD);
    }

    /**
     * @return the value of the `X-Bz-Server-Side-Encryption` header, or null if none.
     * @apiNote We return null here instead of throwing an exception since this is a
     *          completely optional, non-standard header.
     */
    default String getServerSideEncryptionOrNull() {
        return getValueOrNull(SERVER_SIDE_ENCRYPTION);
    }

    /**
     * @return the value of the `X-Bz-Server-Side-Encryption-Customer-Algorithm` header, or null if none.
     * @apiNote We return null here instead of throwing an exception since this is a
     *          completely optional, non-standard header.
     */
    default String getSseCustomerAlgorithmOrNull() {
        return getValueOrNull(SERVER_SIDE_ENCRYPTION_CUSTOMER_ALGORITHM);
    }

    /**
     * @return the value of the `X-Bz-Server-Side-Encryption-Customer-Key-Md5` header, or null if none.
     * @apiNote We return null here instead of throwing an exception since this is a
     *          completely optional, non-standard header.
     */
    default String getSseCustomerKeyMd5OrNull() {
        return getValueOrNull(SERVER_SIDE_ENCRYPTION_CUSTOMER_KEY_MD5);
    }

    /**
     * @return the value of the 'X-Bz-Replication-Status' header, or null if none.
     * @apiNote We return null here instead of throwing an exception since this is a
     *          completely optional, non-standard header.
     */
    default String getReplicationStatusOrNull() {
        return getValueOrNull(REPLICATION_STATUS);
    }
}
