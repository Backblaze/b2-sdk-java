# SDK harness contract

`tests.tsv` is the versioned contract read by Backblaze's centralized SDK quality harness
(`sdkharness` in `backblaze-labs/demand-side-ai`). This repository owns the executable
assertions in `tests/`; the harness builds this checkout, starts a fresh local B2 simulator,
and retains and reports the evidence.

One scenario so far: `health/golden-path`, the customer golden path. `tests/run-health` runs
`tests/health/GoldenPath.java` as a single-file Java program that authorizes, uploads a small
file, downloads it and compares the bytes, lists it, deletes it and confirms it is gone, then
prints one result line:

```text
SDKHARNESS_RESULT	health	golden-path	PASS	-
```

## What the check can and cannot reach

- **Only the loopback simulator.** `run-health` refuses any `SDKHARNESS_SIMULATOR_URL` that
  is not `http://127.0.0.1:<port>`. The SDK's HTTP support for test servers
  (`HttpClientFactoryImpl.Builder.setSupportInsecureHttp`) is enabled for that reason only.
- **Only the simulator's fixed test credential** (`test-key-id` / `test-key`, in the source).
  Every ambient `B2_*` variable except `B2_BUCKET_NAME` is dropped first.
- **No build.** The harness builds the `core` and `httpclient` jars from this checkout and
  passes them, with their runtime dependencies, as `SDKHARNESS_JAVA_CLASSPATH`. The build
  files here are not changed. A missing `java` on `PATH` reports `SKIP`, which the harness
  counts as a failure.

## Run it by hand

Requires JDK 11 or later and a simulator (`backblaze-labs/b2-simulator`) with a bucket named
`sdkharness-healthcheck`:

```bash
./gradlew :b2-sdk-httpclient:jar
SDKHARNESS_TEST_LEVEL=health SDKHARNESS_SCENARIO=golden-path \
SDKHARNESS_SIMULATOR_URL=http://127.0.0.1:<port> B2_BUCKET_NAME=sdkharness-healthcheck \
SDKHARNESS_JAVA_CLASSPATH=<httpclient jar>:<core jar>:<httpclient, httpcore, commons-logging, commons-codec jars> \
  .sdkharness/tests/run-health
```
