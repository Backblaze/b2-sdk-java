# SDK harness contract

`tests.tsv` is the versioned contract read by Backblaze's centralized SDK quality harness
(`sdkharness` in `backblaze-labs/demand-side-ai`). This repository owns the executable
assertions in `tests/`; the harness builds this checkout, starts a fresh local B2 simulator,
and retains and reports the evidence.

Scenarios so far: `health/golden-path` and 27 `conformance` (one per capability the SDK claims) and sixteen `resilience` scenarios (below). `tests/run-health` runs
`tests/health/GoldenPath.java` as a single-file Java program that authorizes, uploads a small
file, downloads it and compares the bytes, lists it, deletes it and confirms it is gone, then
prints one result line:

```text
SDKHARNESS_RESULT	health	golden-path	PASS	-
```

## Conformance

`tests/run-conformance` runs one capability per call, named by `SDKHARNESS_SCENARIO`; every row in
`tests.tsv` points at it. Each scenario is a Java program in `tests/conformance/` that talks to the
simulator through the SDK's public API and asserts what B2's documentation says happens (each file
cites its pages). They share `tests/conformance/Support.java`; a JDK 11 single-file launch cannot
import a second source file, so the wrapper compiles `Support.java` and the check with `javac`
against the harness-built jars, then runs the check class. Each check creates its own
`sdkharness-conf-*` buckets and tries to delete everything it made, on failure too; it attempts every
deletion, and a deletion that fails turns the verdict into a `FAIL` (`cleanup -- left N bucket(s) behind`).

| Scenario | What it asserts |
|---|---|
| `files.upload` | the response (name, size, sha1, type, fileInfo), listing, byte- and sha1-identical download, an empty file, an unusual file name |
| `files.download_content` | download by name: bytes and headers, a Range, a missing name is 404 |
| `files.download_by_id` | two versions of one name each download as themselves; by name is the latest |
| `files.list` | prefix, paging (page size 2), start name, delimiter folders, versions newest first |
| `files.delete_version` | one version deleted: gone from the listing and 404 by id, the other survives |
| `files.hide` | hide marker is a new version; not in the default listing, visible in the version listing; 404 by name; the original still downloads by id |
| `bucket.crud` | create, duplicate name refused, list, update type and info (revision grows), delete refused while non-empty, delete |
| `files.metadata` | by id (`b2_get_file_info`) and by name (an HTTP HEAD of the download URL: the cell is `partial`); the check records the SDK's own requests and asserts that boundary |
| `files.server_side_copy` | `b2_copy_file` sends no bytes (no `b2_upload_file`), same sha1 under a new fileId, the COPY and REPLACE metadata directives, a ranged copy |
| `large.multipart` | `uploadLargeFile` of three parts' worth: one start, more than one `b2_upload_part`, one finish, contentSha1 `none`, `large_file_sha1` carried, bytes intact |
| `large.concurrent_parts` | the same eight-part file on a one-thread and a four-thread executor: the SDK's own HTTP client counts `b2_upload_part` requests in flight (1 with one worker, more than 1 with four; wall time is only a `NOTE`) |
| `large.resume` | a first upload interrupted after two parts, then `finishUploadingLargeFile` on the unfinished file: no new `b2_start_large_file`, only the missing parts, same fileId, bytes intact; run with and without a known sha1 (see the note below) |
| `large.compose_mixed` | one large file from local bytes, a copied byte range and a whole copied file (`storeLargeFile` with `B2UploadingPartStorer` and `B2CopyingPartStorer`), equal to their ordered concatenation |
| `urls.native_download` | `getDownloadByNameUrl` is a native `/file/<bucket>/<name>` URL (no `X-Amz-*`); 401 without a token, 200 with a download authorization, 401 outside its prefix and after it expires |
| `client.progress` | a `B2UploadListener` on a large upload (one worker): it fires, every part's bytes and the running total never go down, every part ends SUCCEEDED, the total equals the byte count |
| `lock.bucket_default` | a default retention of governance, 1 day on a lock-enabled bucket: read back, and an uploaded file inherits a retention of about now + 1 day |
| `lock.per_file_retention` | governance retention now + 2 days on a file: read back, and a delete without bypass is refused (401 `access_denied`) and leaves the file |
| `lock.legal_hold` | hold on: read back and the delete is refused; hold off: read back and the delete succeeds |
| `lock.bypass_governance` | under governance retention a delete is refused (401 `access_denied`), `setBypassGovernance(true)` deletes the version |
| `enc.sse_b2` | an SSE-B2 upload reports SSE-B2 / AES256 and downloads as plaintext; a bucket default of SSE-B2 applies to a plain upload |
| `enc.sse_c` | an SSE-C upload reports SSE-C / AES256; download with the key returns the plaintext and the key's MD5; without it and with another key it is refused (403 for a wrong key) |
| `keys.crud` | a bucket-restricted key is created (secret returned), listed (no secret anywhere in what the SDK returns), used by a second client, deleted (gone, and its secret no longer authorizes) |
| `bucket.cors` | typed `B2CorsRule`: set at create, replaced by update, cleared; read back field for field |
| `bucket.lifecycle` | `daysFromHidingToDeleting` 1 round trips with no other field populated; a replacement list replaces; an empty list clears |
| `bucket.notification_rules` | a webhook rule with a signing secret is set, read back (the secret unchanged) and deleted |
| `bucket.inbound_webhook` | offline: a payload signed in the check with the documented HMAC is accepted and parsed by `B2EventNotification.parse`; a wrong secret, a changed payload and a malformed signature are refused; a test event parses |
| `bucket.replication_config` | a source rule (destination bucket, prefix, priority, flags) with a replication key is set and read back field for field |

`large.resume` reports one SDK behavior as a `FAIL` whose reason starts with `FINDING`. When the content source knows its sha1, the
SDK stores it as the file info `large_file_sha1`; `finishUploadingLargeFile` then compares the request's file info
with the stored one after removing the key `X-Bz-Info-large_file_sha1` (`B2Headers.LARGE_FILE_SHA1`,
`B2LargeFileUploader.throwIfLargeFileVersionDoesntSeemToMatchRequest`), but the stored key is
`large_file_sha1` (`B2Headers.LARGE_FILE_SHA1_INFO_NAME`), so the same request that started the file is refused with a
`mismatch` error. The check does not work around that refusal: the scenario fails with the finding as its reason (the
harness detects, it does not fix the SDK) and passes once the SDK accepts the same request.

Every check that locks files releases them on exit (legal hold off, governance retention bypassed); none uses
compliance mode, which nothing could lift.

A failing scenario prints one `SDKHARNESS_RESULT ... FAIL` line with the step and the reason. Details are
formatted to one capped printable line from exception class names, status codes and the check's own
assertions; that is formatting, not redaction, and other lines the check prints are forwarded as they
are. The only credential in play is the simulator's fixed test one.

## Resilience

`tests/run-resilience` runs one scenario per call, named by `SDKHARNESS_SCENARIO`; every
`resilience` row in `tests.tsv` points at it. Each scenario is a Java program in
`tests/resilience/` that arms **one** documented fault through the simulator's control listener
(`SDKHARNESS_SIMULATOR_CONTROL_URL`), makes one SDK call with the SDK's own default retry policy,
and asserts two things: the end state (the bytes read back intact, the listing succeeds, or the
error reaches the caller) and the recovery path, read from the simulator's request journal
(`GET /journal`: endpoint, status, fault, an opaque upload URL id; never a header, token or body).
Each file cites the B2 pages it asserts. They share `tests/resilience/Support.java`; as for
conformance, the wrapper compiles `Support.java` and the check with `javac` against the
harness-built jars, then runs the check class. Each check creates its own `sdkharness-res-*`
bucket, clears every fault it armed, and tries to delete everything it made, on failure too; it attempts
every step, and one that fails turns the verdict into a `FAIL` (`cleanup -- N step(s) failed`).

| Scenario | Fault, and what it asserts |
|---|---|
| `upload.retry_503`, `upload.retry_500`, `upload.retry_408` | status on `b2_upload_file`: a fresh `b2_get_upload_url`, the retried upload on a different upload URL id, one stored version, bytes intact |
| `upload.expired_token_401` | 401 `expired_auth_token` on `b2_upload_file`: a fresh upload URL, then the file lands intact |
| `upload.get_url_503` | 503 on `b2_get_upload_url`, armed before the first upload in the bucket (the SDK pools upload URLs, so after any earlier upload the fault would never fire): retried, then the file lands |
| `upload.cap_exceeded_403` | 403 `cap_exceeded`: the error reaches the caller, exactly one `b2_upload_file`, nothing stored |
| `upload.reset_mid_request`, `upload.reset_before_response` | connection reset: a fresh upload URL, the file reads back intact (a second version is accepted after a reset before the response) |
| `upload.stall` | the server holds the connection for 60 s: the check sets a 2 s socket timeout (`HttpClientFactoryImpl.Builder.setSocketTimeoutSeconds`, the default is 20 s), the stalled request is abandoned and retried on a new upload URL |
| `part.retry_503` | 503 on `b2_upload_part`: a successful part on a different URL id, `b2_finish_large_file`, bytes intact |
| `auth.expired_401` | 401 `expired_auth_token` on `b2_list_file_names`: `b2_authorize_account` again, then the listing succeeds |
| `auth.clock_expiry` | the simulator's clock moves 24 h and 1 ms: the real 401, a new authorization, the listing succeeds, all on one long-lived client |
| `api.retry_after_429`, `api.retry_after_503` | `Retry-After: 2`: one retry, the whole call took at least 2 s |
| `api.backoff_503` | no `Retry-After`: one retry, the whole call took at least 1 s |
| `download.retry_503` | 503 on `b2_download_file_by_id`: retried, the bytes are intact |

Timing is a lower bound only, never an upper bound (a busy runner would make that flaky). A check
that is still running after 60 s is killed and reported as a failure: the slowest scenario takes
about 3 s, so a minute means the SDK never gave up on something.

A failing resilience scenario prints one `SDKHARNESS_RESULT ... FAIL` line with the step and the reason,
formatted and capped as described under Conformance (formatting, not redaction).

## What the check can and cannot reach

- **Only the loopback simulator.** `run-health`, `run-conformance` and `run-resilience` refuse any
  `SDKHARNESS_SIMULATOR_URL` (and, for resilience, `SDKHARNESS_SIMULATOR_CONTROL_URL`) that is not
  `http://127.0.0.1:<port>`. In conformance and resilience the SDK is also held to it at request time:
  every check builds its client from one guarded HTTP client factory (`Support.guardedFactory`, plain
  HTTP only; in resilience it carries the SDK's request settings including the per-check socket
  timeout, and the control-listener requests are held to the same host) that refuses any request whose
  target host is not `127.0.0.1`, redirect hops included, and a refused request fails the verdict
  (`loopback guard -- blocked a request to non-loopback host ...`). `health` is guarded by its origin
  check only. `conformance/GuardSelfTest.java` and `resilience/GuardSelfTest.java` prove the guard (see
  below).
- **Only the simulator's fixed test credential** (`test-key-id` / `test-key`, in the source).
  Every ambient `B2_*` variable except `B2_BUCKET_NAME` is dropped first (conformance and resilience drop all of
  them and make their own buckets).
- **No build.** The harness builds the `core` and `httpclient` jars from this checkout and
  passes them, with their runtime dependencies, as `SDKHARNESS_JAVA_CLASSPATH`. The build
  files here are not changed. A missing `java` on `PATH` reports `SKIP`, which the harness
  counts as a failure.

## Run it by hand

For conformance, set `SDKHARNESS_TEST_LEVEL=conformance`, `SDKHARNESS_SCENARIO=<capability>` and run
`.sdkharness/tests/run-conformance` (needs `javac`, and no `B2_BUCKET_NAME`).

To re-prove the loopback guard (no simulator needed; `$CP` is the same class path; use `resilience`
in place of `conformance` for the resilience copy):

```bash
javac --release 11 -d /tmp/guard -cp "$CP" .sdkharness/tests/conformance/Support.java .sdkharness/tests/conformance/GuardSelfTest.java
java -cp "/tmp/guard:$CP" GuardSelfTest   # GUARD-SELFTEST b2-sdk-java: PASS
```

For resilience, start the simulator with its control listener (`serve.mjs --control`), set
`SDKHARNESS_TEST_LEVEL=resilience`, `SDKHARNESS_SCENARIO=<scenario>`,
`SDKHARNESS_SIMULATOR_URL`, `SDKHARNESS_SIMULATOR_CONTROL_URL` and `SDKHARNESS_JAVA_CLASSPATH`
(as below), and run `.sdkharness/tests/run-resilience` (needs `javac`, and a fresh simulator per
scenario: a fault is armed once and a stalled connection is held for a minute).

Requires JDK 11 or later and a simulator (`backblaze-labs/b2-simulator`) with a bucket named
`sdkharness-healthcheck`:

```bash
./gradlew :b2-sdk-httpclient:jar
SDKHARNESS_TEST_LEVEL=health SDKHARNESS_SCENARIO=golden-path \
SDKHARNESS_SIMULATOR_URL=http://127.0.0.1:<port> B2_BUCKET_NAME=sdkharness-healthcheck \
SDKHARNESS_JAVA_CLASSPATH=<httpclient jar>:<core jar>:<httpclient, httpcore, commons-logging, commons-codec jars> \
  .sdkharness/tests/run-health
```
