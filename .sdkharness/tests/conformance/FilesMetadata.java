// Copyright 2026, Backblaze Inc. All Rights Reserved.
// License https://www.backblaze.com/using_b2_code.html

import com.backblaze.b2.client.structures.B2Bucket;
import com.backblaze.b2.client.structures.B2FileVersion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * files.metadata, a PARTIAL cell. Run by ../run-conformance (see ../../README.md).
 *
 * <p>By id, b2_get_file_info returns contentType, contentLength, contentSha1 and fileInfo ("the custom
 * information that was uploaded with the file"; keys come back lower-cased).
 * https://www.backblaze.com/apidocs/b2-get-file-info
 * B2 has no get-file-info by NAME; that read is the headers of b2_download_file_by_name, which "contain
 * the same information they did when the file was uploaded".
 * https://www.backblaze.com/apidocs/b2-download-file-by-name
 *
 * <p>The boundary is asserted, not routed around: the card says by-id is an endpoint call and by-name is
 * emulated with an HTTP HEAD against the download URL. The check records the SDK's own requests and
 * requires exactly that, so a change in how the SDK reads by name fails here and the card is corrected.
 */
public class FilesMetadata {
    public static void main(String[] args) {
        Support.run("files.metadata", true, () -> {
            Support.step("bucket");
            final B2Bucket bucket = Support.bucket();

            Support.step("upload");
            final String name = "st/metadata/meta.bin";
            final byte[] data = Support.payload(1500, 5);
            final Map<String, String> info = Collections.singletonMap("sdkharness-meta", "meta-value");
            final B2FileVersion uploaded = Support.upload(bucket, name, data, "application/x-sdkharness", info);

            Support.step("read by id");
            int mark = Support.wire.size();
            final B2FileVersion byId = Support.client.getFileInfo(uploaded.getFileId());
            final List<String> idRequests = new ArrayList<>(Support.wire.subList(mark, Support.wire.size()));
            expectMetadata("by id", byId, uploaded.getFileId(), name, data, info);

            Support.step("by-id request path");
            Support.eq("requests sent for a by-id read", 1, idRequests.size());
            Support.check(idRequests.get(0).startsWith("POST ") && idRequests.get(0).endsWith("/b2_get_file_info"),
                    "a by-id read was not a POST to b2_get_file_info: " + idRequests);

            Support.step("read by name");
            mark = Support.wire.size();
            final B2FileVersion byName = Support.client.getFileInfoByName(bucket.getBucketName(), name);
            final List<String> nameRequests = new ArrayList<>(Support.wire.subList(mark, Support.wire.size()));
            expectMetadata("by name", byName, uploaded.getFileId(), name, data, info);

            Support.step("by-name request path");
            Support.eq("requests sent for a by-name read", 1, nameRequests.size());
            final String request = nameRequests.get(0);
            Support.check(request.startsWith("HEAD ") && request.contains("/file/" + bucket.getBucketName() + "/"),
                    "a by-name read was not a HEAD of the download URL: " + request);
            Support.note("PARTIAL: by id is the b2_get_file_info endpoint; by name has no endpoint, so the SDK sends "
                    + "an HTTP HEAD to the b2_download_file_by_name URL and builds the file version from its headers");
        });
    }

    private static void expectMetadata(String how, B2FileVersion got, String fileId, String name, byte[] data,
                                       Map<String, String> info) {
        Support.eq(how + " fileId", fileId, got.getFileId());
        Support.eq(how + " fileName", name, got.getFileName());
        Support.eq(how + " contentLength", (long) data.length, got.getContentLength());
        Support.eq(how + " contentSha1", Support.sha1(data), got.getContentSha1());
        Support.eq(how + " contentType", "application/x-sdkharness", got.getContentType());
        for (Map.Entry<String, String> entry : info.entrySet()) {
            Support.eq(how + " fileInfo " + entry.getKey(), entry.getValue(), got.getFileInfo().get(entry.getKey()));
        }
    }
}
