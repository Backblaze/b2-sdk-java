// Copyright 2026, Backblaze Inc. All Rights Reserved.
// License https://www.backblaze.com/using_b2_code.html

import com.backblaze.b2.client.contentSources.B2ByteArrayContentSource;
import com.backblaze.b2.client.exceptions.B2Exception;
import com.backblaze.b2.client.structures.B2Bucket;
import com.backblaze.b2.client.structures.B2FileSseForRequest;
import com.backblaze.b2.client.structures.B2FileVersion;
import com.backblaze.b2.client.structures.B2ServerSideEncryptionMode;
import com.backblaze.b2.client.structures.B2UploadFileRequest;

import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;

/**
 * enc.sse_c. Run by ../run-conformance (see ../../README.md).
 *
 * <p>b2_upload_file takes the X-Bz-Server-Side-Encryption-Customer-Algorithm (AES256), -Customer-Key and
 * -Customer-Key-Md5 headers; its response serverSideEncryption carries "the mode ("SSE-B2" or "SSE-C") and
 * algorithm" (https://www.backblaze.com/apidocs/b2-upload-file). The key's MD5 comes back on download:
 * X-Bz-Server-Side-Encryption-Customer-Key-Md5, "if the file was encrypted with SSE-C, then the value is the MD5
 * digest of the customer-managed encryption key used for encryption"
 * (https://www.backblaze.com/apidocs/b2-download-file-by-id). "Backblaze B2 manages the encryption process using the
 * key that you provide when uploading or accessing each file"; the documented error is for the WRONG key, "a 403
 * error is returned" (https://www.backblaze.com/docs/cloud-storage-server-side-encryption).
 *
 * <p>A 32-byte customer key (derived from a fixed string; it protects nothing and is never printed) is used for an
 * upload. Asserted: the upload response reports SSE-C / AES256; downloading with the key returns the plaintext and the
 * response carries the key's MD5; downloading without the key fails with a service error (B2 documents no status for
 * a missing key, so a 400, 401 or 403 counts) and does not return the plaintext; downloading with a different key
 * fails with 403, the documented status for a wrong key.
 */
public class EncSseC {
    public static void main(String[] args) {
        Support.run("enc.sse_c", () -> {
            Support.step("bucket and keys");
            final B2Bucket bucket = Support.bucket();
            final byte[] keyBytes = MessageDigest.getInstance("SHA-256").digest(Support.utf8("sdkharness-java-sse-c-key"));
            final byte[] otherBytes = MessageDigest.getInstance("SHA-256").digest(Support.utf8("sdkharness-java-sse-c-other-key"));
            final B2FileSseForRequest sse = B2FileSseForRequest.createSseCAes256(Base64.getEncoder().encodeToString(keyBytes));
            final B2FileSseForRequest wrong = B2FileSseForRequest.createSseCAes256(Base64.getEncoder().encodeToString(otherBytes));
            final String keyMd5 = Base64.getEncoder().encodeToString(MessageDigest.getInstance("MD5").digest(keyBytes));

            Support.step("upload with SSE-C");
            final byte[] data = Support.payload(1024, 92);
            final B2FileVersion uploaded = Support.client.uploadSmallFile(
                    B2UploadFileRequest.builder(bucket.getBucketId(), "st/enc/sse-c.bin", "application/octet-stream", B2ByteArrayContentSource.build(data))
                            .setServerSideEncryption(sse).build());
            Support.check(uploaded.getServerSideEncryption() != null, "the upload response carries no serverSideEncryption");
            Support.eq("upload response mode", B2ServerSideEncryptionMode.SSE_C, uploaded.getServerSideEncryption().getMode());
            Support.eq("upload response algorithm", "AES256", uploaded.getServerSideEncryption().getAlgorithm());

            Support.step("download with the key");
            final Support.Downloaded got = Support.downloadById(uploaded.getFileId(), sse);
            Support.check(Arrays.equals(data, got.bytes), "the download with the key is not the plaintext");
            Support.eq("X-Bz-Server-Side-Encryption-Customer-Key-Md5 on the download", keyMd5, got.headers.getSseCustomerKeyMd5OrNull());
            Support.eq("X-Bz-Server-Side-Encryption-Customer-Algorithm on the download", "AES256", got.headers.getSseCustomerAlgorithmOrNull());

            Support.step("download without the key");
            final B2Exception missing = Support.expectError("download of an SSE-C file without its key", () -> Support.downloadById(uploaded.getFileId()));
            Support.check(Arrays.asList(400, 401, 403).contains(missing.getStatus()),
                    "refused with status " + missing.getStatus() + " code " + missing.getCode() + ", expected 400, 401 or 403");

            Support.step("download with a different key");
            final B2Exception other = Support.expectError("download of an SSE-C file with the wrong key", () -> Support.downloadById(uploaded.getFileId(), wrong));
            Support.eq("status for the wrong key", 403, other.getStatus());
        });
    }
}
