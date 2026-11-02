// Copyright 2026, Backblaze Inc. All Rights Reserved.
// License https://www.backblaze.com/using_b2_code.html

import com.backblaze.b2.client.structures.B2Bucket;
import com.backblaze.b2.client.structures.B2CorsRule;
import com.backblaze.b2.client.structures.B2UpdateBucketRequest;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

/**
 * bucket.cors. Run by ../run-conformance (see ../../README.md).
 *
 * <p>Every field sent is a documented rule field (corsRuleName, allowedOrigins, allowedOperations, allowedHeaders,
 * exposeHeaders, maxAgeSeconds), and b2_download_file_by_name and b2_download_file_by_id are documented
 * allowedOperations values. Rules can be "set when you create the bucket with b2_create_bucket, or updated on an
 * existing bucket using b2_update_bucket", and b2_list_buckets returns corsRules
 * (https://www.backblaze.com/docs/cloud-storage-cross-origin-resource-sharing-rules,
 * https://www.backblaze.com/apidocs/b2-update-bucket, https://www.backblaze.com/apidocs/b2-list-buckets).
 *
 * <p>This SDK models a rule as the typed {@code B2CorsRule} (the card's cell is "yes", not "partial"). Asserted: a
 * bucket created with one rule reports it, field for field, when listed; updating the bucket to a different pair of
 * rules replaces the set (the read-back is exactly the new pair); updating to an empty list removes every rule.
 */
public class BucketCors {
    public static void main(String[] args) {
        Support.run("bucket.cors", () -> {
            final B2CorsRule first = B2CorsRule.builder("sdkharness-first", Arrays.asList("https://example.com", "https://app.example.com"),
                            new HashSet<>(Arrays.asList("b2_download_file_by_name", "b2_download_file_by_id")), 3600)
                    .setAllowedHeaders(Arrays.asList("range", "authorization"))
                    .setExposeHeaders(Arrays.asList("x-bz-content-sha1"))
                    .build();

            Support.step("create with a rule");
            final B2Bucket bucket = Support.bucket(false, b -> b.setCorsRules(Collections.singletonList(first)));
            Support.step("read back the created rule");
            assertRules("rules on create", Collections.singletonList(first), bucket.getCorsRules());
            assertRules("rules when listed", Collections.singletonList(first), listed(bucket).getCorsRules());

            Support.step("replace the rules");
            final B2CorsRule second = B2CorsRule.builder("sdkharness-second", Collections.singletonList("*"),
                    new HashSet<>(Collections.singletonList("b2_download_file_by_name")), 60).build();
            final B2CorsRule third = B2CorsRule.builder("sdkharness-third", Collections.singletonList("https://other.example.com"),
                    new HashSet<>(Collections.singletonList("b2_download_file_by_id")), 120)
                    .setAllowedHeaders(Collections.singletonList("*")).build();
            final B2Bucket replaced = Support.client.updateBucket(B2UpdateBucketRequest.builder(bucket).setCorsRules(Arrays.asList(second, third)).build());
            assertRules("rules after the update", Arrays.asList(second, third), listed(bucket).getCorsRules());

            Support.step("remove the rules");
            // updateBucket is optimistic: each update names the revision it was made from, so it starts from the last response.
            Support.client.updateBucket(B2UpdateBucketRequest.builder(replaced).setCorsRules(Collections.emptyList()).build());
            Support.eq("rules after clearing", 0, listed(bucket).getCorsRules().size());
        });
    }

    private static B2Bucket listed(B2Bucket bucket) throws Exception {
        final B2Bucket found = Support.client.getBucketOrNullByName(bucket.getBucketName());
        Support.check(found != null, "the bucket is not listed");
        return found;
    }

    /** Field for field, in order. */
    private static void assertRules(String what, List<B2CorsRule> expected, List<B2CorsRule> actual) {
        Support.eq(what + ": number of rules", expected.size(), actual.size());
        for (int i = 0; i < expected.size(); i++) {
            final B2CorsRule e = expected.get(i);
            final B2CorsRule a = actual.get(i);
            Support.eq(what + ": corsRuleName", e.getCorsRuleName(), a.getCorsRuleName());
            Support.eq(what + ": allowedOrigins of " + e.getCorsRuleName(), e.getAllowedOrigins(), a.getAllowedOrigins());
            Support.eq(what + ": allowedOperations of " + e.getCorsRuleName(), new HashSet<>(e.getAllowedOperations()), new HashSet<>(a.getAllowedOperations()));
            Support.eq(what + ": allowedHeaders of " + e.getCorsRuleName(), nullToEmpty(e.getAllowedHeaders()), nullToEmpty(a.getAllowedHeaders()));
            Support.eq(what + ": exposeHeaders of " + e.getCorsRuleName(), nullToEmpty(e.getExposeHeaders()), nullToEmpty(a.getExposeHeaders()));
            Support.eq(what + ": maxAgeSeconds of " + e.getCorsRuleName(), e.getMaxAgeSeconds(), a.getMaxAgeSeconds());
        }
    }

    private static List<String> nullToEmpty(List<String> list) {
        return list == null ? Collections.emptyList() : list;
    }
}
