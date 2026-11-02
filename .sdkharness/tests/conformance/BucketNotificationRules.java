// Copyright 2026, Backblaze Inc. All Rights Reserved.
// License https://www.backblaze.com/using_b2_code.html

import com.backblaze.b2.client.structures.B2Bucket;
import com.backblaze.b2.client.structures.B2EventNotificationRule;
import com.backblaze.b2.client.structures.B2SetBucketNotificationRulesRequest;
import com.backblaze.b2.client.structures.B2WebhookConfiguration;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;

/**
 * bucket.notification_rules. Run by ../run-conformance (see ../../README.md).
 *
 * <p>b2_get_bucket_notification_rules lists hmacSha256SigningSecret as a response field and its example response
 * carries it unredacted, so the secret is expected back unchanged
 * (https://www.backblaze.com/apidocs/b2-get-bucket-notification-rules); b2_set_bucket_notification_rules replaces the
 * whole rule set (https://www.backblaze.com/apidocs/b2-set-bucket-notification-rules). The webhook URL is never
 * called: the rule only has to be stored and read back.
 *
 * <p>Asserted: a rule with a webhook target and a signing secret is set; the SDK's read-back (and the set call's own
 * response) carries the same name, event types, object name prefix, enabled flag, target URL and signing secret;
 * setting an empty list deletes it. The secret is generated here, compared and never printed.
 */
public class BucketNotificationRules {
    private static final String WEBHOOK_URL = "https://example.com/sdkharness-conf";

    public static void main(String[] args) {
        Support.run("bucket.notification_rules", () -> {
            Support.step("bucket");
            final B2Bucket bucket = Support.bucket();
            Support.eq("rules on a new bucket", 0, Support.client.getBucketNotificationRules(bucket.getBucketId()).getEventNotificationRules().size());

            Support.step("set a rule");
            // 32 alphanumeric characters, the documented shape of a signing secret.
            final String secret = "*******" + bucket.getBucketName().substring(bucket.getBucketName().length() - 12) + "0123456789abcdef";
            final B2EventNotificationRule rule = new B2EventNotificationRule("sdkharness-conf",
                    new TreeSet<>(Arrays.asList("b2:ObjectCreated:*", "b2:ObjectDeleted:*")), "st/",
                    new B2WebhookConfiguration(WEBHOOK_URL, secret), true);
            final List<B2EventNotificationRule> set = Support.client.setBucketNotificationRules(
                    B2SetBucketNotificationRulesRequest.builder(bucket.getBucketId(), Collections.singletonList(rule)).build()).getEventNotificationRules();
            assertRule("set response", rule, set, secret);

            Support.step("read back");
            assertRule("read-back", rule, Support.client.getBucketNotificationRules(bucket.getBucketId()).getEventNotificationRules(), secret);

            Support.step("delete");
            Support.client.setBucketNotificationRules(B2SetBucketNotificationRulesRequest.builder(bucket.getBucketId(), Collections.emptyList()).build());
            Support.eq("rules after deleting", 0, Support.client.getBucketNotificationRules(bucket.getBucketId()).getEventNotificationRules().size());
        });
    }

    private static void assertRule(String what, B2EventNotificationRule expected, List<B2EventNotificationRule> actual, String secret) {
        Support.eq(what + ": number of rules", 1, actual.size());
        final B2EventNotificationRule rule = actual.get(0);
        Support.eq(what + ": name", expected.getName(), rule.getName());
        Support.eq(what + ": eventTypes", expected.getEventTypes(), rule.getEventTypes());
        Support.eq(what + ": objectNamePrefix", expected.getObjectNamePrefix(), rule.getObjectNamePrefix());
        Support.eq(what + ": isEnabled", true, rule.isEnabled());
        Support.check(rule.getTargetConfiguration() instanceof B2WebhookConfiguration, what + ": the target is not a webhook");
        final B2WebhookConfiguration target = (B2WebhookConfiguration) rule.getTargetConfiguration();
        Support.eq(what + ": webhook url", WEBHOOK_URL, target.getUrl());
        Support.check(secret.equals(target.getHmacSha256SigningSecret()), what + ": the signing secret did not come back unchanged");
    }
}
