// Copyright 2026, Backblaze Inc. All Rights Reserved.
// License https://www.backblaze.com/using_b2_code.html

import com.backblaze.b2.client.exceptions.B2Exception;
import com.backblaze.b2.client.structures.B2Bucket;
import com.backblaze.b2.client.structures.B2FileVersion;
import com.backblaze.b2.client.structures.B2ListFileNamesRequest;

import java.util.Arrays;
import java.util.List;

/**
 * files.hide. Run by ../run-conformance (see ../../README.md).
 *
 * <p>b2_hide_file "hides a file so that downloading by name will not find the file, but previous
 * versions of the file are still stored": the hide is a new version with action "hide", the name
 * disappears from b2_list_file_names, it stays visible in b2_list_file_versions, and the original
 * version still downloads by its fileId.
 * https://www.backblaze.com/apidocs/b2-hide-file
 */
public class FilesHide {
    public static void main(String[] args) {
        Support.run("files.hide", () -> {
            Support.step("bucket");
            final B2Bucket bucket = Support.bucket();
            final String id = bucket.getBucketId();

            Support.step("upload");
            final String name = "st/hide/visible.txt";
            final byte[] data = Support.utf8("hide me");
            final B2FileVersion original = Support.upload(bucket, name, data, "text/plain", null);
            Support.eq("name listing before the hide", Arrays.asList(name),
                    Support.names(Support.client.fileNames(B2ListFileNamesRequest.builder(id).setPrefix(name).build())));

            Support.step("hide");
            final B2FileVersion hidden = Support.client.hideFile(id, name);
            Support.eq("hide marker action", "hide", hidden.getAction());
            Support.eq("hide marker fileName", name, hidden.getFileName());
            Support.check(!original.getFileId().equals(hidden.getFileId()), "the hide marker reused the uploaded version's fileId");

            Support.step("not in the default listing");
            Support.eq("name listing after the hide", 0,
                    Support.names(Support.client.fileNames(B2ListFileNamesRequest.builder(id).setPrefix(name).build())).size());

            Support.step("visible as a hidden version");
            final List<B2FileVersion> versions = Support.versions(id, name);
            Support.eq("versions after the hide", 2, versions.size());
            Support.eq("newest version is the hide marker", hidden.getFileId(), versions.get(0).getFileId());
            Support.check(versions.get(0).isHide(), "the newest version is not marked as a hide");
            Support.eq("older version is the upload", original.getFileId(), versions.get(1).getFileId());

            Support.step("download by name is not found");
            final B2Exception notFound = Support.expectError("download by name of a hidden file",
                    () -> Support.downloadByName(bucket.getBucketName(), name));
            Support.eq("hidden download status", 404, notFound.getStatus());

            Support.step("the original version is still stored");
            Support.check(Arrays.equals(data, Support.downloadById(original.getFileId()).bytes),
                    "the hidden file's original version no longer downloads by fileId");
        });
    }
}
