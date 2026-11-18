# Releasing the B2 SDK for Java

A release is a reviewed version commit on `master`, an annotated, signed `vX.Y.Z` tag on that
commit, and the same version published to Maven Central. This process follows the release-prep
approach in [Blazer's release guide](https://github.com/Backblaze/blazer/blob/main/RELEASE.md).

## Prerequisites

- JDK 11 and the checked-in Gradle wrapper; the build also runs the JDK 17 test module.
- Python 3, Git, GitHub CLI (`gh auth status`), GnuPG or OpenSSH for tag signing.
- Changie v1.24.0, a standalone Go binary. Install with
  `go install github.com/miniscruff/changie@v1.24.0` using Go 1.24 or newer, then put
  `$(go env GOPATH)/bin` on `PATH`. This does not change the Java toolchain.
- Maven Central publishing credentials and artifact-signing secrets remain configured for
  `.github/workflows/deploy.yml`: `SONATYPE_TOKEN_USERNAME`, `SONATYPE_TOKEN_PASSWORD`,
  `GPG_SIGNING_KEY`, and `GPG_PASSPHRASE`. Tag signing and Maven artifact signing are separate.

Do not run a publishing task locally as a verification step. A tag push starts the production
deployment, including closing and releasing the Sonatype staging repository.

## Changelog and versioning

Use semantic versioning: fixes use a patch release, compatible features a minor release, and
incompatible changes a major release. Choose the version explicitly; a `Changed` entry alone
does not determine whether the release is compatible.

Each PR adds a fragment rather than editing `CHANGELOG.md`:

```bash
changie new --kind Fixed --body 'Describe the observable change.'
python3 scripts/release.py changelog-check
python3 -m unittest discover -s scripts/tests -v
```

Kinds are Added, Changed, Deprecated, Removed, Fixed, and Security. Keep unpublished security
details out of public fragments. Existing release history is preserved in `.changes/v*.md`;
unreleased changes live in `.changes/unreleased/*.yaml`. `CHANGELOG.md` contains the generated
released history. `changie merge` also sets `version=` in `gradle.properties` from the latest
version notes. Do not edit the generated changelog or bump the Gradle version independently.
The `changelog-check` CI job verifies generated output, version consistency, and a fragment for
each PR (or a complete batch and version update for a release-prep PR).
Dependabot PRs that only modify existing workflow YAML files do not need a release fragment;
generated-output checks and the release-tooling tests still run. Source/dependency changes outside
workflow files and human-authored changes still need a fragment.

## Milestones and release preparation

1. Select the version and create its milestone before preparing the release. Assign every
   intended issue and PR to it. Publicly track public work only; unpublished advisories remain
   in GitHub Security Advisories. Deferred work moves to the next milestone with a written reason.

   ```bash
   export VERSION=6.5.1              # choose the actual next version
   export TAG="v$VERSION"
   gh api --method POST repos/Backblaze/b2-sdk-java/milestones -f title="$TAG"
   gh issue edit <issue-number> --milestone "$TAG"
   gh pr edit <pr-number> --milestone "$TAG"
   git fetch origin master --tags
   git switch -c "release/$TAG" origin/master
   ```

2. Confirm the release scope has merged and the relevant CI checks are green. Check the
   repository Security tab: an open medium, high, or critical finding holds the release,
   including findings without a patched dependency. Failures to read findings are unverified,
   not clean. A maintainer must resolve or dismiss findings with a written reason before release.

3. Batch the changelog and update the version in one operation:

   ```bash
   changie batch "$TAG" --dry-run --allow-no-changes=false
   changie batch "$TAG" --allow-no-changes=false
   changie merge
   python3 scripts/release.py changelog-check
   python3 scripts/release.py notes "$TAG" > /tmp/b2-java-release-notes.md
   git diff -- gradle.properties CHANGELOG.md .changes/
   ```

   Review the generated `.changes/$TAG.md` and regenerate with `changie merge` after any edits.
   A release-prep PR changes only `.changes/`, `CHANGELOG.md`, and `gradle.properties`. Keep
   workflow or SDK behavior changes in separate PRs.

4. Verify locally without publishing:

   ```bash
   python3 -m unittest discover -s scripts/tests -v
   ./gradlew build --no-daemon
   RELEASE_BUILD=true ./gradlew publishToSonatype closeAndReleaseSonatypeStagingRepository \
     createBundle --dry-run --no-daemon \
     -PsonatypeUsername=dry-run -PsonatypePassword=dry-run
   ```

   The placeholder credentials are only for task-graph validation with `--dry-run`; this does
   not contact Sonatype, sign artifacts, or prove that production secrets are usable.
   Run the local health check below against a fresh loopback simulator as well.

5. Commit and open a release-prep PR linked to its issue, assign its milestone, and request a
   maintainer review. Include the release notes, build, health-check, and dry-run results.
   A human merges it after required CI and review pass. Do not tag an unmerged PR.

## Tag and deploy

Tag the merged release-prep commit, which may differ from the latest tip of `master`:

```bash
: "${TAG:?export the release tag, e.g. v6.5.1}"
VERSION="${TAG#v}"
git fetch origin master --tags
SHA=$(gh pr view <release-pr-number> --json mergeCommit --jq .mergeCommit.oid)
git merge-base --is-ancestor "$SHA" origin/master
git show "$SHA:gradle.properties" | grep -Fx "version=$VERSION"
git log -1 --oneline "$SHA"

# GPG: use your full signing-key fingerprint.
git -c gpg.format=openpgp tag -s -u <fingerprint> "$TAG" "$SHA" -m "$TAG"
git verify-tag "$TAG"

# Alternatively, with a registered SSH signing key:
# git -c gpg.format=ssh -c user.signingkey="$HOME/.ssh/id_ed25519.pub" \
#   tag -s "$TAG" "$SHA" -m "$TAG"
# git -c gpg.format=ssh -c gpg.ssh.allowedSignersFile=<allowed-signers-file> verify-tag "$TAG"

git push origin "$TAG"
```

The tag-triggered deploy workflow publishes using the existing Gradle command:

```bash
./gradlew publishToSonatype closeAndReleaseSonatypeStagingRepository createBundle --no-daemon --stacktrace
```

Maven Central, GPG artifact signing, the Sonatype staging endpoint, and the release/close tasks
are unchanged by the changelog tooling. Never run this production command as a dry-run substitute.

## Verify the published release

1. Inspect the deployment run for the tag and confirm the Sonatype close/release step succeeded:
   `gh run list --workflow deploy.yml --branch "$TAG"` and `gh run view <run-id> --log-failed`.
2. Confirm the GitHub Release contains this version's changelog section and all three module bundles:
   `gh release view "$TAG" --json tagName,body,assets`.
3. After Maven Central indexing completes, check each module and its signature:

   ```bash
   for module in core httpclient samples core-test-jdk17; do
     url="https://repo.maven.apache.org/maven2/com/backblaze/b2/b2-sdk-$module/$VERSION/b2-sdk-$module-$VERSION.pom"
     curl --fail --show-error --location "$url" -o "/tmp/b2-sdk-$module-$VERSION.pom"
     curl --fail --show-error --location "$url.asc" -o "/tmp/b2-sdk-$module-$VERSION.pom.asc"
     gpg --verify "/tmp/b2-sdk-$module-$VERSION.pom.asc" "/tmp/b2-sdk-$module-$VERSION.pom"
   done
   ```

   The shared Gradle publication plugin also publishes `b2-sdk-core-test-jdk17`, so this
   loop verifies all four published modules, including that test module's POM and signature.
   Verify using the established Maven artifact-signing public key, independently of tag keys.
4. Confirm there are no open issues/PRs left in the version milestone, then close it. Publish any
   advisories coordinated with the release only after the fixed artifacts are available.

## Local health check

Use a local B2 simulator with the fixed test credentials and an empty `sdkharness-healthcheck`
bucket. This check authorizes, uploads, downloads, compares bytes, lists, and deletes; it refuses
non-loopback endpoints. It must print `SDKHARNESS_RESULT` with `health`, `golden-path`, and `PASS`.
Set `SDKHARNESS_SIMULATOR_URL` to the running simulator's IPv4 loopback URL, then run:

```bash
: "${SDKHARNESS_SIMULATOR_URL:?set the local simulator URL, e.g. http://127.0.0.1:8180}"
./gradlew build --no-daemon
CP=$(./gradlew -q -I scripts/health-classpath.gradle :b2-sdk-httpclient:healthClasspath)
SDKHARNESS_TEST_LEVEL=health SDKHARNESS_SCENARIO=golden-path \
  B2_BUCKET_NAME=sdkharness-healthcheck SDKHARNESS_JAVA_CLASSPATH="$CP" \
  .sdkharness/tests/run-health
```

## Recovery

Never move a published tag or reuse a Maven Central version. If a release fails, inspect staging
and Central before retrying: the existing deploy command closes and releases staging repositories.
Do not blindly rerun a partially completed publish. If artifacts are already published, ship a new
patch version. Reverting SDK code does not remove artifacts already used by consumers.
