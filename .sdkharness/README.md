# SDK harness contract

`tests.tsv` is the versioned contract read by Backblaze's centralized SDK quality harness
(`sdkharness` in `backblaze-labs/demand-side-ai`). This repository owns the executable
assertions in `tests/`; the harness builds this checkout, starts a fresh local B2 simulator,
and retains and reports the evidence.

Scenarios so far: `health/golden-path` and eight `conformance` scenarios (below). `tests/run-health` runs
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
`sdkharness-conf-*` buckets and deletes everything it made, on failure too.

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

A failing scenario prints one `SDKHARNESS_RESULT ... FAIL` line with the step and the reason.

## What the check can and cannot reach

- **Only the loopback simulator.** `run-health` refuses any `SDKHARNESS_SIMULATOR_URL` that
  is not `http://127.0.0.1:<port>`. The SDK's HTTP support for test servers
  (`HttpClientFactoryImpl.Builder.setSupportInsecureHttp`) is enabled for that reason only.
- **Only the simulator's fixed test credential** (`test-key-id` / `test-key`, in the source).
  Every ambient `B2_*` variable except `B2_BUCKET_NAME` is dropped first (conformance drops all of
  them and makes its own buckets).
- **No build.** The harness builds the `core` and `httpclient` jars from this checkout and
  passes them, with their runtime dependencies, as `SDKHARNESS_JAVA_CLASSPATH`. The build
  files here are not changed. A missing `java` on `PATH` reports `SKIP`, which the harness
  counts as a failure.

## Run it by hand

For conformance, set `SDKHARNESS_TEST_LEVEL=conformance`, `SDKHARNESS_SCENARIO=<capability>` and run
`.sdkharness/tests/run-conformance` (needs `javac`, and no `B2_BUCKET_NAME`).

Requires JDK 11 or later and a simulator (`backblaze-labs/b2-simulator`) with a bucket named
`sdkharness-healthcheck`:

```bash
./gradlew :b2-sdk-httpclient:jar
SDKHARNESS_TEST_LEVEL=health SDKHARNESS_SCENARIO=golden-path \
SDKHARNESS_SIMULATOR_URL=http://127.0.0.1:<port> B2_BUCKET_NAME=sdkharness-healthcheck \
SDKHARNESS_JAVA_CLASSPATH=<httpclient jar>:<core jar>:<httpclient, httpcore, commons-logging, commons-codec jars> \
  .sdkharness/tests/run-health
```
