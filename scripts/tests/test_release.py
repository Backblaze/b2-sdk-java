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
    def test_maven_verification_checks_every_published_module_and_signature(self):
        guide = (SCRIPT.parents[1] / "RELEASE.md").read_text()
        section = guide.split("## Verify the published release", 1)[1].split("\n## ", 1)[0]
        blocks = [part.split("```", 1)[0] for part in section.split("```bash")[1:]]
        block = next(block for block in blocks if "for module in" in block)
        with tempfile.TemporaryDirectory() as temporary:
            root = pathlib.Path(temporary)
            curl = root / "curl"
            curl.write_text(
                '#!/bin/sh\nfor argument; do\n'
                '  case "$argument" in https://*) printf "%s\\n" "$argument" >> "$CURL_LOG";; esac\n'
                'done\n'
            )
            curl.chmod(0o755)
            gpg = root / "gpg"
            gpg.write_text('#!/bin/sh\nprintf "%s\\n" "$*" >> "$GPG_LOG"\n')
            gpg.chmod(0o755)
            environment = dict(os.environ, PATH=str(root) + os.pathsep + os.environ["PATH"],
                               VERSION="1.2.3", CURL_LOG=str(root / "curl.log"), GPG_LOG=str(root / "gpg.log"))
            result = subprocess.run(["bash", "-euo", "pipefail", "-c", block], env=environment,
                                    capture_output=True, text=True)
            self.assertEqual(0, result.returncode, result.stdout + result.stderr)
            urls = (root / "curl.log").read_text().splitlines()
            signatures = (root / "gpg.log").read_text().splitlines()
            for module in ("core", "httpclient", "samples", "core-test-jdk17"):
                with self.subTest(module=module):
                    artifact = "b2-sdk-" + module
                    url = "https://repo.maven.apache.org/maven2/com/backblaze/b2/" + artifact + "/1.2.3/" + artifact + "-1.2.3.pom"
                    self.assertIn(url, urls)
                    self.assertIn(url + ".asc", urls)
                    pom = "/tmp/" + artifact + "-1.2.3.pom"
                    self.assertIn("--verify " + pom + ".asc " + pom, signatures)

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
            environment = dict(os.environ, PATH=str(root) + os.pathsep + os.environ["PATH"], TAG="v1.2.3")
            environment.pop("VERSION", None)
            result = subprocess.run(["bash", "-euo", "pipefail", "-c", block], env=environment,
                                    capture_output=True, text=True)
            self.assertEqual(0, result.returncode, result.stdout + result.stderr)


if __name__ == "__main__":
    unittest.main()
