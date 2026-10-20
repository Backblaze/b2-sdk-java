// Copyright 2026, Backblaze Inc. All Rights Reserved.
// License https://www.backblaze.com/using_b2_code.html

import com.backblaze.b2.client.B2StorageClient;
import com.backblaze.b2.client.structures.B2ApplicationKey;
import com.backblaze.b2.client.structures.B2Bucket;
import com.backblaze.b2.client.structures.B2CreateKeyRequest;
import com.backblaze.b2.client.structures.B2CreatedApplicationKey;
import com.backblaze.b2.client.structures.B2DeleteKeyRequest;
import com.backblaze.b2.client.structures.B2ListBucketsRequest;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * keys.crud. Run by ../run-conformance (see ../../README.md).
 *
 * <p>b2_create_key's applicationKey is "The secret part of the key. This is the only time it will be returned, so
 * you need to keep it. This is not returned when you list the keys in your account."; b2_list_keys' key objects carry
 * no applicationKey field; b2_delete_key "Deletes the application key specified"
 * (https://www.backblaze.com/apidocs/b2-create-key, https://www.backblaze.com/apidocs/b2-list-keys,
 * https://www.backblaze.com/apidocs/b2-delete-key).
 *
 * <p>A key restricted to one bucket and to three capabilities is created, listed and deleted. Asserted: create returns
 * the key id and a secret and echoes the name, capabilities and bucket; the listing finds the key with the same name,
 * capabilities and bucket, and nothing the SDK returns for a listed key carries the secret (no accessor of
 * {@code B2ApplicationKey} returns it, nor does its text form); the secret works (a second client authorized with it
 * reports the restricted bucket as its allowed bucket and lists that bucket by name); after the delete the key is no longer listed and its secret no longer
 * authorizes. The key is restricted to one bucket because this SDK pins API version 2, which cannot represent a key
 * scoped to several (card: keys.multi_bucket is "no").
 */
public class KeysCrud {
    public static void main(String[] args) {
        Support.run("keys.crud", () -> {
            Support.step("bucket");
            final B2Bucket bucket = Support.bucket();
            final Set<String> capabilities = new HashSet<>(Arrays.asList("listBuckets", "listFiles", "readFiles"));
            final String name = "sdkharness-" + bucket.getBucketName().substring(bucket.getBucketName().length() - 8);

            Support.step("create");
            final B2CreatedApplicationKey created = Support.client.createKey(B2CreateKeyRequest.builder(capabilities, name).setBucketId(bucket.getBucketId()).build());
            Support.trackKey(created.getApplicationKeyId());
            final String secret = created.getApplicationKey();
            Support.check(created.getApplicationKeyId() != null && !created.getApplicationKeyId().isEmpty(), "create returned no key id");
            Support.check(secret != null && !secret.isEmpty(), "create returned no secret");
            Support.eq("created key name", name, created.getKeyName());
            Support.eq("created key capabilities", capabilities, new HashSet<>(created.getCapabilities()));
            Support.eq("created key bucket", bucket.getBucketId(), created.getBucketId());

            Support.step("list");
            B2ApplicationKey listed = find(created.getApplicationKeyId());
            Support.check(listed != null, "the new key is not in the listing");
            Support.eq("listed key name", name, listed.getKeyName());
            Support.eq("listed key capabilities", capabilities, new HashSet<>(listed.getCapabilities()));
            Support.eq("listed key bucket", bucket.getBucketId(), listed.getBucketId());

            Support.step("the secret is returned only on create");
            Support.check(!listed.toString().contains(secret), "the listed key's text form carries the secret");
            for (Method method : B2ApplicationKey.class.getMethods()) {
                if (method.getParameterCount() == 0 && method.getReturnType() == String.class) {
                    Support.check(!secret.equals(method.invoke(listed)), "B2ApplicationKey." + method.getName() + "() returns the secret");
                }
            }

            Support.step("the secret works");
            final B2StorageClient restricted = Support.clientFor(created.getApplicationKeyId(), secret);
            // A key restricted to a bucket must name that bucket when it lists buckets (b2_list_buckets), so it does.
            final List<String> visible = new ArrayList<>();
            for (B2Bucket b : restricted.listBuckets(B2ListBucketsRequest.builder(restricted.getAccountId()).setBucketName(bucket.getBucketName()).build()).getBuckets()) {
                visible.add(b.getBucketName());
            }
            Support.eq("buckets the restricted key lists by name", Arrays.asList(bucket.getBucketName()), visible);
            Support.eq("authorization bucket of the restricted key", bucket.getBucketId(), restricted.getAccountAuthorization().getAllowed().getBucketId());

            Support.step("delete");
            Support.client.deleteKey(B2DeleteKeyRequest.builder(created.getApplicationKeyId()).build());
            Support.untrackKey(created.getApplicationKeyId());
            Support.check(find(created.getApplicationKeyId()) == null, "the deleted key is still listed");
            // Revocation is asserted through authorization itself (a fresh b2_authorize_account). A bucket-restricted key
            // calling buckets() gets 401 even when valid, so that call cannot tell a revoked key from a restricted one.
            Support.eq("status of authorizing with a deleted key", 401, Support.expectError("authorizing with a deleted key",
                    () -> Support.clientFor(created.getApplicationKeyId(), secret).getAccountAuthorization()).getStatus());
        });
    }

    private static B2ApplicationKey find(String keyId) throws Exception {
        for (B2ApplicationKey key : Support.client.applicationKeys()) {
            if (keyId.equals(key.getApplicationKeyId())) {
                return key;
            }
        }
        return null;
    }
}
