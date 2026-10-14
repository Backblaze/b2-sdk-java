// Copyright 2026, Backblaze Inc. All Rights Reserved.
// License https://www.backblaze.com/using_b2_code.html

import com.backblaze.b2.client.exceptions.B2Exception;
import com.backblaze.b2.client.structures.B2Bucket;
import com.backblaze.b2.client.structures.B2FileVersion;
import com.backblaze.b2.client.structures.B2ListFileNamesRequest;
import com.backblaze.b2.client.structures.B2ListFileVersionsRequest;

import java.util.Arrays;
import java.util.List;

/**
 * files.delete_version. Run by ../run-conformance (see ../../README.md).
 *
 * <p>b2_delete_file_version "deletes one version of a file"; the other versions of the name stay.
 * Once deleted, the version is gone from the version listing and a download of its fileId is a 404
 * not_found, while the surviving version still downloads and is what the name listing shows.
 * https://www.backblaze.com/apidocs/b2-delete-file-version
 */
public class FilesDeleteVersion {
    public static void main(String[] args) {
        Support.run("files.delete_version", () -> {
            Support.step("bucket");
            final B2Bucket bucket = Support.bucket();
            final String id = bucket.getBucketId();

            Support.step("upload two versions");
            final String name = "st/delete/versioned.txt";
            final byte[] firstBytes = Support.utf8("first version");
            final byte[] secondBytes = Support.utf8("second version");
            final B2FileVersion v1 = Support.upload(bucket, name, firstBytes, "text/plain", null);
            final B2FileVersion v2 = Support.upload(bucket, name, secondBytes, "text/plain", null);

            Support.step("delete the first version");
            Support.client.deleteFileVersion(v1);

            Support.step("the deleted version is gone");
            final List<B2FileVersion> remaining = Support.versions(id, name);
            Support.eq("versions left after deleting the first", 1, remaining.size());
            Support.eq("the version that is left", v2.getFileId(), remaining.get(0).getFileId());
            final B2Exception gone = Support.expectError("download of the deleted version",
                    () -> Support.downloadById(v1.getFileId()));
            Support.eq("deleted version download status", 404, gone.getStatus());

            Support.step("the other version survives");
            Support.check(Arrays.equals(secondBytes, Support.downloadById(v2.getFileId()).bytes),
                    "the surviving version's bytes changed");
            Support.eq("name listing", Arrays.asList(name),
                    Support.names(Support.client.fileNames(B2ListFileNamesRequest.builder(id).setPrefix(name).build())));

            Support.step("delete the last version");
            Support.client.deleteFileVersion(v2);
            Support.eq("versions left after deleting both", 0, Support.versions(id, name).size());
            Support.eq("names left after deleting both", 0,
                    Support.names(Support.client.fileNames(B2ListFileNamesRequest.builder(id).setPrefix(name).build())).size());
            Support.eq("whole-bucket version listing", 0,
                    Support.names(Support.client.fileVersions(B2ListFileVersionsRequest.builder(id).build())).size());
        });
    }
}
