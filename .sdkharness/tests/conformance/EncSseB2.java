// Copyright 2026, Backblaze Inc. All Rights Reserved.
// License https://www.backblaze.com/using_b2_code.html

import com.backblaze.b2.client.contentSources.B2ByteArrayContentSource;
import com.backblaze.b2.client.structures.B2Bucket;
import com.backblaze.b2.client.structures.B2BucketServerSideEncryption;
import com.backblaze.b2.client.structures.B2FileSseForRequest;
import com.backblaze.b2.client.structures.B2FileSseForResponse;
import com.backblaze.b2.client.structures.B2FileVersion;
import com.backblaze.b2.client.structures.B2ServerSideEncryptionMode;
import com.backblaze.b2.client.structures.B2UploadFileRequest;

import java.util.Arrays;

/**
 * enc.sse_b2. Run by ../run-conformance (see ../../README.md).
 *
 * <p>b2_upload_file's X-Bz-Server-Side-Encryption: "B2 will encrypt the uploaded data before storing the file using
 * ... (SSE-B2) with the algorithm specified in this header. Currently, the only supported value for this header is
 * AES256." Its response serverSideEncryption carries "the mode ("SSE-B2" or "SSE-C") and algorithm"
 * (https://www.backblaze.com/apidocs/b2-upload-file). A bucket default: "All uploads to that bucket, from the time
 * default encryption is enabled onward, will then be encrypted with SSE-B2 by default unless you explicitly specify
 * SSE-C encryption for a given file at upload time." (https://www.backblaze.com/docs/cloud-storage-server-side-encryption);
 * defaultServerSideEncryption is "The default server-side encryption settings for new files uploaded to this
 * bucket", example {"mode": "SSE-B2", "algorithm": "AES256"} (https://www.backblaze.com/apidocs/b2-create-bucket).
 *
 * <p>Asserted: a file uploaded requesting SSE-B2 reports SSE-B2 / AES256 in the upload response and in its metadata,
 * the download returns the plaintext and its response says AES256; a bucket created with SSE-B2 as its default
 * reports that default, and a file uploaded to it without asking reports SSE-B2 too.
 */
public class EncSseB2 {
    public static void main(String[] args) {
        Support.run("enc.sse_b2", () -> {
            Support.step("bucket");
            final B2Bucket bucket = Support.bucket();

            Support.step("upload requesting SSE-B2");
            final byte[] data = Support.payload(1024, 91);
            final B2FileVersion uploaded = Support.client.uploadSmallFile(
                    B2UploadFileRequest.builder(bucket.getBucketId(), "st/enc/sse-b2.bin", "application/octet-stream", B2ByteArrayContentSource.build(data))
                            .setServerSideEncryption(B2FileSseForRequest.createSseB2Aes256()).build());
            assertSseB2("upload response", uploaded.getServerSideEncryption());

            Support.step("metadata");
            assertSseB2("file metadata", Support.client.getFileInfo(uploaded.getFileId()).getServerSideEncryption());

            Support.step("download");
            final Support.Downloaded got = Support.downloadById(uploaded.getFileId());
            Support.check(Arrays.equals(data, got.bytes), "the download is not the plaintext");
            Support.eq("X-Bz-Server-Side-Encryption on the download", "AES256", got.headers.getServerSideEncryptionOrNull());

            Support.step("bucket default");
            final B2Bucket defaultBucket = Support.bucket(false, b -> b.setDefaultServerSideEncryption(B2BucketServerSideEncryption.createSseB2Aes256()));
            Support.eq("bucket default mode", B2ServerSideEncryptionMode.SSE_B2, defaultBucket.getDefaultServerSideEncryption().getMode());
            Support.eq("bucket default algorithm", "AES256", defaultBucket.getDefaultServerSideEncryption().getAlgorithm());
            final B2FileVersion inherited = Support.upload(defaultBucket, "st/enc/default.bin", data, "application/octet-stream", null);
            assertSseB2("a file uploaded to a bucket with default encryption", inherited.getServerSideEncryption());
            Support.check(Arrays.equals(data, Support.downloadById(inherited.getFileId()).bytes), "the default-encrypted file's download is not the plaintext");
        });
    }

    private static void assertSseB2(String what, B2FileSseForResponse sse) {
        Support.check(sse != null, what + " carries no serverSideEncryption");
        Support.eq(what + " mode", B2ServerSideEncryptionMode.SSE_B2, sse.getMode());
        Support.eq(what + " algorithm", "AES256", sse.getAlgorithm());
    }
}
