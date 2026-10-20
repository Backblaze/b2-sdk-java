// Copyright 2026, Backblaze Inc. All Rights Reserved.
// License https://www.backblaze.com/using_b2_code.html

import com.backblaze.b2.client.structures.B2Bucket;
import com.backblaze.b2.client.structures.B2CopyFileRequest;
import com.backblaze.b2.client.structures.B2FileVersion;
import com.backblaze.b2.util.B2ByteRange;

import java.util.Arrays;
import java.util.Collections;

/**
 * files.server_side_copy. Run by ../run-conformance (see ../../README.md).
 *
 * <p>b2_copy_file "Copies an entire file" from a sourceFileId to a new fileName: the request carries no
 * file bytes, and the response's fileId is "The unique identifier for this version of this file" while its
 * contentSha1 is "The SHA1 of the bytes stored in the file", so a whole-file copy reports the source's sha1
 * under a new fileId. The check also reads the copy back, and asserts the metadata directive (COPY keeps the
 * source's content type and file info, REPLACE takes the ones in the request) and a ranged copy.
 * https://www.backblaze.com/apidocs/b2-copy-file
 */
public class FilesServerSideCopy {
    public static void main(String[] args) {
        Support.run("files.server_side_copy", true, () -> {
            Support.step("bucket");
            final B2Bucket bucket = Support.bucket();

            Support.step("source");
            final byte[] data = Support.payload(512, 11);
            final B2FileVersion source = Support.upload(bucket, "st/copy/source.bin", data, "application/x-sdkharness",
                    Collections.singletonMap("origin", "source"));

            Support.step("copy");
            Support.wire.clear();
            final B2FileVersion copy = Support.client.copySmallFile(
                    B2CopyFileRequest.builder(source.getFileId(), "st/copy/destination.bin").build());
            Support.eq("copy fileName", "st/copy/destination.bin", copy.getFileName());
            Support.check(!source.getFileId().equals(copy.getFileId()), "the copy reused the source's fileId");
            Support.eq("copy contentSha1", source.getContentSha1(), copy.getContentSha1());
            Support.eq("copy contentLength", (long) data.length, copy.getContentLength());
            Support.eq("COPY keeps the content type", "application/x-sdkharness", copy.getContentType());
            Support.eq("COPY keeps the file info", "source", copy.getFileInfo().get("origin"));

            Support.step("no bytes were uploaded");
            Support.eq("b2_copy_file requests", 1, Support.wireCount("POST", "b2_copy_file"));
            Support.eq("b2_upload_file requests during the copy", 0, Support.wireCount("POST", "b2_upload_file"));

            Support.step("read back");
            Support.check(Arrays.equals(data, Support.downloadById(copy.getFileId()).bytes), "the copy's bytes differ from the source's");
            Support.check(Arrays.equals(data, Support.downloadById(source.getFileId()).bytes), "the source changed");

            Support.step("metadata directive REPLACE");
            final B2FileVersion replaced = Support.client.copySmallFile(
                    B2CopyFileRequest.builder(source.getFileId(), "st/copy/replaced.bin")
                            .setMetadataDirective(B2CopyFileRequest.REPLACE_METADATA_DIRECTIVE)
                            .setContentType("text/x-replaced")
                            .setFileInfo(Collections.singletonMap("origin", "replaced"))
                            .build());
            Support.eq("REPLACE content type", "text/x-replaced", replaced.getContentType());
            Support.eq("REPLACE file info", "replaced", replaced.getFileInfo().get("origin"));
            Support.eq("REPLACE contentSha1", source.getContentSha1(), replaced.getContentSha1());

            Support.step("a range copy");
            final B2FileVersion ranged = Support.client.copySmallFile(
                    B2CopyFileRequest.builder(source.getFileId(), "st/copy/ranged.bin")
                            .setRange(B2ByteRange.between(100, 199)).build());
            Support.eq("range copy length", 100L, ranged.getContentLength());
            Support.check(Arrays.equals(Arrays.copyOfRange(data, 100, 200), Support.downloadById(ranged.getFileId()).bytes),
                    "the range copy's bytes are not bytes 100..199 of the source");
        });
    }
}
