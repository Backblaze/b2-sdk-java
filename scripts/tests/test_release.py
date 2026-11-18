"""Local contract tests for release preparation; no network or credentials required."""

import json
import pathlib
import os
import shutil
import subprocess
import sys
import tempfile
import unittest


SCRIPT = pathlib.Path(__file__).resolve().parents[1] / "release.py"


class ReleaseNotesTest(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.root = pathlib.Path(self.temporary.name)
        (self.root / ".changes").mkdir()
        self.notes = "## [1.2.3] - 2026-01-02\n\n### Fixed\n\n- Keep this release only.\n"
        (self.root / ".changes/v1.2.3.md").write_text(self.notes)
        (self.root / "CHANGELOG.md").write_text(
            "# Changelog\n\n" + self.notes + "\n## [1.2.2] - 2026-01-01\n\n- Older.\n"
        )
        (self.root / "gradle.properties").write_text("version=1.2.3\ngroup=com.backblaze.b2\n")

    def run_notes(self, tag="v1.2.3"):
        return subprocess.run(
            [sys.executable, str(SCRIPT), "--root", str(self.root), "notes", tag],
            capture_output=True, text=True,
        )

    def test_extracts_exact_release_section(self):
        result = self.run_notes()
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertEqual(self.notes, result.stdout)
        self.assertNotIn("Older", result.stdout)

    def test_rejects_tag_version_mismatch(self):
        result = self.run_notes("v1.2.4")
        self.assertNotEqual(0, result.returncode)
        self.assertIn("gradle.properties", result.stderr)

    def test_rejects_missing_notes(self):
        (self.root / ".changes/v1.2.3.md").unlink()
        result = self.run_notes()
        self.assertNotEqual(0, result.returncode)
        self.assertIn("release notes", result.stderr)

    def test_rejects_edited_generated_changelog(self):
        (self.root / "CHANGELOG.md").write_text("# Changelog\n\n" + self.notes.replace("Keep", "Changed"))
        result = self.run_notes()
        self.assertNotEqual(0, result.returncode)
        self.assertIn("disagree", result.stderr)

    def test_rejects_path_as_tag(self):
        result = self.run_notes("../v1.2.3")
        self.assertNotEqual(0, result.returncode)
        self.assertIn("version tag", result.stderr)

    def test_rejects_duplicate_version_property(self):
        (self.root / "gradle.properties").write_text("version=1.2.3\nversion=1.2.4\n")
        result = self.run_notes()
        self.assertNotEqual(0, result.returncode)
        self.assertIn("exactly one", result.stderr)

    def run_documented_manual_update(self):
        guide = (SCRIPT.parents[1] / "RELEASE.md").read_text()
        section = guide.split("## Verify the published release", 1)[1].split("\n## ", 1)[0]
        blocks = [part.split("```", 1)[0] for part in section.split("```bash")[1:]]
        updates = [block for block in blocks if "gh release edit" in block]
        self.assertEqual(1, len(updates), "release guide must document a validated manual notes update")
        script = self.root / "scripts/release.py"
        script.parent.mkdir()
        shutil.copy(SCRIPT, script)
        binaries = self.root / "bin"
        binaries.mkdir()
        gh = binaries / "gh"
        gh.write_text(
            "#!" + sys.executable + "\n"
            "import json, os, pathlib, sys\n"
            "arguments = sys.argv[1:]\n"
            "record = {'argv': arguments}\n"
            "if arguments[:2] == ['release', 'edit']:\n"
            "    notes = pathlib.Path(arguments[arguments.index('--notes-file') + 1])\n"
            "    record.update(notes_file=str(notes), body=notes.read_text())\n"
            "with open(os.environ['GH_LOG'], 'a') as output:\n"
            "    output.write(json.dumps(record) + '\\n')\n"
        )
        gh.chmod(0o755)
        notes_directory = self.root / "temporary-notes"
        notes_directory.mkdir()
        log = self.root / "gh.jsonl"
        environment = dict(os.environ, PATH=str(binaries) + os.pathsep + os.environ["PATH"],
                           TAG="v1.2.3", GH_LOG=str(log), TMPDIR=str(notes_directory))
        result = subprocess.run(["bash", "-euo", "pipefail", "-c", updates[0]], cwd=self.root,
                                env=environment, capture_output=True, text=True)
        records = [json.loads(line) for line in log.read_text().splitlines()] if log.exists() else []
        return result, records, notes_directory

    def test_documented_manual_update_uses_only_validated_version_notes(self):
        result, records, notes_directory = self.run_documented_manual_update()
        self.assertEqual(0, result.returncode, result.stdout + result.stderr)
        self.assertEqual(2, len(records), records)
        update, view = records
        self.assertEqual(["release", "edit", "v1.2.3", "--notes-file", update["notes_file"]], update["argv"])
        self.assertEqual(self.notes, update["body"])
        self.assertNotIn("Older", update["body"])
        self.assertEqual(["release", "view", "v1.2.3", "--json", "tagName,body,assets"], view["argv"])
        self.assertFalse(pathlib.Path(update["notes_file"]).exists())
        self.assertEqual([], list(notes_directory.iterdir()))

    def test_documented_manual_update_rejects_invalid_notes_and_cleans_up(self):
        path = self.root / ".changes/v1.2.3.md"
        path.write_text(self.notes.replace("Keep", "Changed"))
        result, records, notes_directory = self.run_documented_manual_update()
        self.assertNotEqual(0, result.returncode)
        self.assertIn("disagree", result.stderr)
        self.assertEqual([], records)
        self.assertEqual([], list(notes_directory.iterdir()))


class ChangelogTest(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.root = pathlib.Path(self.temporary.name)
        repository = SCRIPT.parents[1]
        for name in (".changie.yaml", "gradle.properties", "CHANGELOG.md"):
            shutil.copy(repository / name, self.root)
        shutil.copytree(repository / ".changes", self.root / ".changes")
        (self.root / ".changes/unreleased/contract-test.yaml").write_text(
            'kind: Fixed\nbody: Exercise a complete release batch.\ntime: 2026-01-01T00:00:00Z\n'
        )
        self.changie = os.environ.get("CHANGIE", "changie")

    def run_check(self, *arguments):
        return subprocess.run(
            [sys.executable, str(SCRIPT), "--root", str(self.root), "changelog-check", *arguments],
            capture_output=True, text=True,
        )

    def test_current_generated_changelog_matches(self):
        result = self.run_check()
        self.assertEqual(0, result.returncode, result.stderr)

    def test_rejects_gradle_version_mismatch_with_or_without_replacements(self):
        path = self.root / "gradle.properties"
        properties = path.read_text()
        current = next(line for line in properties.splitlines() if line.startswith("version="))
        mismatched = properties.replace(current, "version=0.0.0", 1)
        path.write_text(mismatched)
        for replacements in (True, False):
            with self.subTest(replacements=replacements):
                if not replacements:
                    config = self.root / ".changie.yaml"
                    config.write_text(config.read_text().split("\nreplacements:", 1)[0] + "\n")
                result = self.run_check()
                self.assertNotEqual(0, result.returncode, result.stdout)
                self.assertIn("gradle.properties", result.stderr)
                self.assertEqual(mismatched, path.read_text())

    def test_rejects_latest_notes_with_a_mismatched_version_heading(self):
        current = next(line[8:] for line in (self.root / "gradle.properties").read_text().splitlines()
                       if line.startswith("version="))
        path = self.root / (".changes/v" + current + ".md")
        path.write_text(path.read_text().replace("## [" + current + "]", "## [0.0.0]", 1))
        subprocess.run([self.changie, "merge"], cwd=self.root, check=True, capture_output=True)
        result = self.run_check()
        self.assertNotEqual(0, result.returncode, result.stdout)
        self.assertIn("gradle.properties", result.stderr)

    def make_pull_request(self, path):
        workflow = self.root / ".github/workflows/dependency.yml"
        workflow.parent.mkdir(parents=True)
        workflow.write_text("name: dependency\n")
        for args in (("init", "-q"), ("config", "user.name", "Release Fixture"),
                     ("config", "user.email", "fixture@example.invalid"), ("add", "."),
                     ("-c", "commit.gpgSign=false", "commit", "-qm", "Base fixture")):
            subprocess.run(["git", *args], cwd=self.root, check=True, capture_output=True)
        base = subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=self.root, text=True).strip()
        changed = self.root / path
        changed.write_text("updated fixture\n")
        subprocess.run(["git", "add", "."], cwd=self.root, check=True, capture_output=True)
        subprocess.run(["git", "-c", "commit.gpgSign=false", "commit", "-qm", "Update fixture"],
                       cwd=self.root, check=True, capture_output=True)
        return base

    def test_dependabot_workflow_pin_update_needs_no_fragment(self):
        base = self.make_pull_request(".github/workflows/dependency.yml")
        result = self.run_check("--base", base, "--dependabot")
        self.assertEqual(0, result.returncode, result.stderr)

    def test_human_workflow_update_still_requires_fragment(self):
        base = self.make_pull_request(".github/workflows/dependency.yml")
        result = self.run_check("--base", base)
        self.assertNotEqual(0, result.returncode)
        self.assertIn("fragment", result.stderr)

    def test_dependabot_source_change_still_requires_fragment(self):
        base = self.make_pull_request("Source.java")
        result = self.run_check("--base", base, "--dependabot")
        self.assertNotEqual(0, result.returncode)
        self.assertIn("fragment", result.stderr)

    def test_modifying_an_existing_fragment_does_not_cover_a_source_change(self):
        base = self.make_pull_request("Source.java")
        fragment = self.root / ".changes/unreleased/contract-test.yaml"
        fragment.write_text(fragment.read_text().replace("complete release batch", "modified release batch"))
        subprocess.run(["git", "-c", "commit.gpgSign=false", "commit", "-qam", "Edit existing fragment"],
                       cwd=self.root, check=True, capture_output=True)
        result = self.run_check("--base", base)
        self.assertNotEqual(0, result.returncode, result.stdout)
        self.assertIn("fragment", result.stderr)

    def test_adding_a_new_fragment_covers_a_source_change(self):
        base = self.make_pull_request("Source.java")
        (self.root / ".changes/unreleased/new-change.yaml").write_text(
            'kind: Fixed\nbody: Describe the source change.\ntime: 2026-01-02T00:00:00Z\n'
        )
        subprocess.run(["git", "add", "."], cwd=self.root, check=True, capture_output=True)
        subprocess.run(["git", "-c", "commit.gpgSign=false", "commit", "-qm", "Add a change fragment"],
                       cwd=self.root, check=True, capture_output=True)
        result = self.run_check("--base", base)
        self.assertEqual(0, result.returncode, result.stderr)

    def test_migrated_unreleased_entries_survive_a_release_batch_unchanged(self):
        expected = (
            "`toString()` of credential-bearing structures (`B2AccountAuthorization`, "
            "`B2FileSseForRequest`, `B2WebhookConfiguration`, `B2WebhookCustomHeader`, and "
            "`B2HeadersImpl`) omits sensitive values; `B2WebhookCustomHeader` omits all header values.",
            "`B2EventNotificationRule` now orders and deduplicates rules by `maxEventsPerBatch`, "
            "so rules that differ only by batch size no longer collapse in sorted collections; "
            "it also uses direct webhook settings instead of the former string-based order "
            "(url, headers, then signing secret), so callers that persist or diff ordered rules "
            "may observe a different order.",
            "Security / behaviour change: the three-argument `B2EventNotification.parse` overloads "
            "now require a signature header and a signing secret; receivers should answer HTTP "
            "401 on `B2SignatureVerificationException`.\n  A missing or empty signature header "
            "throws `B2SignatureVerificationException`; a null or empty signing secret throws "
            "`IllegalArgumentException`.",
            "Signature comparison is now constant-time.",
            "Added `B2EventNotification.parseUnverified`, which parses without checking a signature. "
            "The one-argument `parse` overloads are deprecated in its favor.",
        )
        subprocess.run([self.changie, "batch", "patch", "--allow-no-changes=false"],
                       cwd=self.root, check=True, capture_output=True)
        subprocess.run([self.changie, "merge"], cwd=self.root, check=True, capture_output=True)
        releases = [path.read_text() for path in (self.root / ".changes").glob("v*.md")]
        migrated_releases = [notes for notes in releases if "* " + expected[0] + "\n" in notes]
        self.assertEqual(1, len(migrated_releases), "migrated changes must survive in one release")
        notes = migrated_releases[0]
        changelog = (self.root / "CHANGELOG.md").read_text()
        for body in expected:
            with self.subTest(body=body):
                self.assertIn("* " + body + "\n", notes)
                self.assertIn("* " + body + "\n", changelog)

    def test_hand_edited_changelog_is_rejected_without_modifying_it(self):
        path = self.root / "CHANGELOG.md"
        edited = path.read_text() + "\nHand edited.\n"
        path.write_text(edited)
        result = self.run_check()
        self.assertNotEqual(0, result.returncode)
        self.assertIn("generated", result.stderr)
        self.assertEqual(edited, path.read_text())

    def test_batch_and_merge_update_gradle_version_and_release_notes(self):
        current = next(line[8:] for line in (self.root / "gradle.properties").read_text().splitlines()
                       if line.startswith("version="))
        major, minor, patch = current.split("-")[0].split(".")
        version = major + "." + minor + "." + str(int(patch) + 1)
        for arguments in (("batch", "v" + version, "--allow-no-changes=false"), ("merge",)):
            subprocess.run([self.changie, *arguments], cwd=self.root, check=True, capture_output=True)
        self.assertIn("version=" + version + "\n", (self.root / "gradle.properties").read_text())
        self.assertTrue((self.root / (".changes/v" + version + ".md")).is_file())
        result = self.run_check()
        self.assertEqual(0, result.returncode, result.stderr)


class ReleaseInstructionsTest(unittest.TestCase):
    def test_tag_guards_stop_before_tag_or_push(self):
        guide = (SCRIPT.parents[1] / "RELEASE.md").read_text()
        block = guide.split("## Tag and deploy", 1)[1].split("```bash", 1)[1].split("```", 1)[0]
        block = block.replace("<release-pr-number>", "123").replace("<fingerprint>", "fixture-key")
        for failure in ("gh", "empty-sha", "ancestor", "version", "verify"):
            with self.subTest(failure=failure), tempfile.TemporaryDirectory() as temporary:
                root = pathlib.Path(temporary)
                git = root / "git"
                git.write_text(
                    "#!" + sys.executable + "\n"
                    "import json, os, sys\n"
                    "args = sys.argv[1:]\n"
                    "failure = os.environ['TAG_GUARD_FAILURE']\n"
                    "if 'tag' in args or args[0] in ('push', 'verify-tag'):\n"
                    "    with open(os.environ['GIT_LOG'], 'a') as output:\n"
                    "        output.write(json.dumps(args) + '\\n')\n"
                    "if args[0] == 'merge-base' and (failure == 'ancestor' or not args[2]):\n"
                    "    sys.exit(1)\n"
                    "if args[0] == 'show':\n"
                    "    print('version=9.9.9' if failure == 'version' else 'version=1.2.3')\n"
                    "if args[0] == 'verify-tag' and failure == 'verify':\n"
                    "    sys.exit(1)\n"
                )
                git.chmod(0o755)
                gh = root / "gh"
                gh.write_text(
                    "#!" + sys.executable + "\n"
                    "import os, sys\n"
                    "failure = os.environ['TAG_GUARD_FAILURE']\n"
                    "if failure == 'gh':\n"
                    "    sys.exit(1)\n"
                    "print('' if failure == 'empty-sha' else '0123456789012345678901234567890123456789')\n"
                )
                gh.chmod(0o755)
                log = root / "git.jsonl"
                environment = dict(os.environ, PATH=str(root) + os.pathsep + os.environ["PATH"],
                                   TAG="v1.2.3", TAG_GUARD_FAILURE=failure, GIT_LOG=str(log))
                result = subprocess.run(["bash", "-c", block], env=environment,
                                        capture_output=True, text=True)
                calls = [json.loads(line) for line in log.read_text().splitlines()] if log.exists() else []
                self.assertNotEqual(0, result.returncode, result.stdout + result.stderr)
                self.assertFalse(any(call[0] == "push" for call in calls), calls)
                if failure != "verify":
                    self.assertEqual([], calls)

    def run_documented_maven_verification(self, failure="none", fingerprint="A" * 40):
        guide = (SCRIPT.parents[1] / "RELEASE.md").read_text()
        section = guide.split("## Verify the published release", 1)[1].split("\n## ", 1)[0]
        blocks = [part.split("```", 1)[0] for part in section.split("```bash")[1:]]
        block = next(block for block in blocks if "for module in" in block)
        temporary = tempfile.TemporaryDirectory()
        self.addCleanup(temporary.cleanup)
        root = pathlib.Path(temporary.name)
        curl = root / "curl"
        curl.write_text(
            '#!/bin/sh\nfor argument; do\n'
            '  case "$argument" in https://*) printf "%s\\n" "$argument" >> "$CURL_LOG";; esac\n'
            '  case "$MAVEN_FAILURE:$argument" in\n'
            '    download:*b2-sdk-core-1.2.3.pom) exit 22;;\n'
            '    signature-download:*b2-sdk-core-1.2.3.pom.asc) exit 22;;\n'
            '    jar-signature-download:*b2-sdk-core-1.2.3.jar.asc) exit 22;;\n'
            '  esac\n'
            'done\n'
        )
        curl.chmod(0o755)
        gpg = root / "gpg"
        gpg.write_text(
            '#!/bin/sh\nprintf "%s\\n" "$*" >> "$GPG_LOG"\n'
            'case "$MAVEN_FAILURE:$*" in invalid-signature:*b2-sdk-core-1.2.3.pom*) exit 1;; esac\n'
            'case "$MAVEN_FAILURE:$*" in invalid-jar-signature:*b2-sdk-core-1.2.3.jar*) exit 1;; esac\n'
            'case "$MAVEN_FAILURE" in\n'
            '  wrong-signer) signer=' + "C" * 40 + ';;\n'
            '  *) signer="$MAVEN_FIXTURE_PRIMARY";;\n'
            'esac\n'
            'case "$MAVEN_FAILURE:$*" in wrong-jar-signer:*b2-sdk-core-1.2.3.jar*) signer=' + "C" * 40 + ';; esac\n'
            'key="$signer"\n'
            'case "$MAVEN_FAILURE" in signing-subkey|subkey-as-primary) key=' + "B" * 40 + ';; esac\n'
            'if [ "$MAVEN_FAILURE" != missing-status ]; then\n'
            '  printf "[GNUPG:] VALIDSIG %s 2026-01-02 1767312000 0 4 0 22 8 00 %s\\n" "$key" "$signer"\n'
            'fi\n'
        )
        gpg.chmod(0o755)
        environment = dict(os.environ, PATH=str(root) + os.pathsep + os.environ["PATH"],
                           VERSION="1.2.3", MAVEN_SIGNING_FINGERPRINT=fingerprint,
                           MAVEN_FIXTURE_PRIMARY=fingerprint,
                           MAVEN_FAILURE=failure, CURL_LOG=str(root / "curl.log"),
                           GPG_LOG=str(root / "gpg.log"))
        if failure == "missing-fingerprint":
            environment.pop("MAVEN_SIGNING_FINGERPRINT")
        elif failure == "subkey-as-primary":
            environment["MAVEN_SIGNING_FINGERPRINT"] = "B" * 40
        result = subprocess.run(["bash", "-c", block], env=environment, capture_output=True, text=True)
        urls = (root / "curl.log").read_text().splitlines() if (root / "curl.log").exists() else []
        signatures = (root / "gpg.log").read_text().splitlines() if (root / "gpg.log").exists() else []
        return result, urls, signatures

    def test_maven_verification_checks_every_published_module_and_signature(self):
        result, urls, signatures = self.run_documented_maven_verification()
        self.assertEqual(0, result.returncode, result.stdout + result.stderr)
        for module in ("core", "httpclient", "samples", "core-test-jdk17"):
            with self.subTest(module=module):
                artifact = "b2-sdk-" + module
                for suffix in (".pom", ".jar", "-sources.jar", "-javadoc.jar"):
                    with self.subTest(suffix=suffix):
                        filename = artifact + "-1.2.3" + suffix
                        url = "https://repo.maven.apache.org/maven2/com/backblaze/b2/" + artifact + "/1.2.3/" + filename
                        self.assertIn(url, urls)
                        self.assertIn(url + ".asc", urls)
                        local_file = "/tmp/" + filename
                        self.assertTrue(any("--verify " + local_file + ".asc " + local_file in call for call in signatures))
        self.assertEqual(32, len(urls))
        self.assertEqual(16, len(signatures))

    def test_maven_verification_stops_on_download_or_signature_failure(self):
        for failure in ("download", "signature-download", "invalid-signature"):
            with self.subTest(failure=failure):
                result, urls, signatures = self.run_documented_maven_verification(failure)
                self.assertNotEqual(0, result.returncode, result.stdout + result.stderr)
                self.assertFalse(any("b2-sdk-httpclient" in url for url in urls), urls)
                if failure != "invalid-signature":
                    self.assertEqual([], signatures)

    def test_maven_verification_rejects_bad_jars_even_with_a_valid_pom(self):
        for failure in ("jar-signature-download", "invalid-jar-signature", "wrong-jar-signer"):
            with self.subTest(failure=failure):
                result, urls, signatures = self.run_documented_maven_verification(failure)
                self.assertNotEqual(0, result.returncode, result.stdout + result.stderr)
                self.assertFalse(any("b2-sdk-httpclient" in url for url in urls), urls)
                self.assertTrue(any("b2-sdk-core-1.2.3.pom.asc" in call for call in signatures))

    def test_maven_verification_requires_the_expected_signer(self):
        for failure in ("wrong-signer", "missing-status", "missing-fingerprint", "subkey-as-primary"):
            with self.subTest(failure=failure):
                result, urls, _ = self.run_documented_maven_verification(failure)
                self.assertNotEqual(0, result.returncode, result.stdout + result.stderr)
                self.assertFalse(any("b2-sdk-httpclient" in url for url in urls), urls)
                if failure == "missing-fingerprint":
                    self.assertEqual([], urls)

    def test_maven_verification_accepts_the_expected_primary_keys_signing_subkey(self):
        for fingerprint in ("A" * 40, "D" * 40):
            with self.subTest(fingerprint=fingerprint):
                result, urls, signatures = self.run_documented_maven_verification("signing-subkey", fingerprint)
                self.assertEqual(0, result.returncode, result.stdout + result.stderr)
                self.assertEqual(32, len(urls))
                self.assertEqual(16, len(signatures))

    def test_maven_verification_uses_the_configured_primary_fingerprint(self):
        for fingerprint in ("A" * 40, "D" * 40):
            with self.subTest(fingerprint=fingerprint):
                result, _, signatures = self.run_documented_maven_verification(fingerprint=fingerprint)
                self.assertEqual(0, result.returncode, result.stdout + result.stderr)
                self.assertEqual(16, len(signatures))
                mismatch, urls, _ = self.run_documented_maven_verification("wrong-signer", fingerprint)
                self.assertNotEqual(0, mismatch.returncode, mismatch.stdout + mismatch.stderr)
                self.assertFalse(any("b2-sdk-httpclient" in url for url in urls), urls)

    def test_tag_commands_work_with_only_tag_exported(self):
        guide = (SCRIPT.parents[1] / "RELEASE.md").read_text()
        block = guide.split("## Tag and deploy", 1)[1].split("```bash", 1)[1].split("```", 1)[0]
        block = block.replace("<release-pr-number>", "123").replace("<fingerprint>", "fixture-key")
        with tempfile.TemporaryDirectory() as temporary:
            root = pathlib.Path(temporary)
            git = root / "git"
            git.write_text("#!/bin/sh\nif [ \"$1\" = show ]; then printf 'version=1.2.3\\n'; fi\n")
            git.chmod(0o755)
            gh = root / "gh"
            gh.write_text("#!/bin/sh\nprintf '0123456789012345678901234567890123456789\\n'\n")
            gh.chmod(0o755)
            environment = {"PATH": str(root) + os.pathsep + os.defpath, "TAG": "v1.2.3"}
            result = subprocess.run(["bash", "-euo", "pipefail", "-c", block], env=environment,
                                    capture_output=True, text=True)
            self.assertEqual(0, result.returncode, result.stdout + result.stderr)


if __name__ == "__main__":
    unittest.main()
