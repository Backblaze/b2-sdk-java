"""Exercise release-preparation diffs using real Git and Changie."""

import os
import pathlib
import shutil
import subprocess
import sys
import tempfile
import unittest


ROOT = pathlib.Path(__file__).resolve().parents[2]


class ReleasePrepTest(unittest.TestCase):
    def test_renaming_an_existing_fragment_does_not_count_as_a_new_entry(self):
        with tempfile.TemporaryDirectory(prefix="b2-fragment-rename-") as temporary:
            root = pathlib.Path(temporary)
            shutil.copy(ROOT / ".changie.yaml", root)
            (root / ".changes/unreleased").mkdir(parents=True)
            (root / ".changes/header.tpl.md").write_text("# Changelog\n")
            (root / ".changes/v1.2.3.md").write_text("## [1.2.3] - 2026-01-01\n\n### Fixed\n\n* Prior release.\n")
            (root / "gradle.properties").write_text("version=1.2.3\n")
            original = root / ".changes/unreleased/original.yaml"
            original.write_text("kind: Fixed\nbody: Existing entry.\ntime: 2026-01-02T00:00:00Z\n")
            changie = os.environ.get("CHANGIE", "changie")
            subprocess.run([changie, "merge"], cwd=root, check=True, capture_output=True)
            for args in (("init", "-q"), ("config", "user.name", "Release Fixture"),
                         ("config", "user.email", "fixture@example.invalid"), ("config", "diff.renames", "false"),
                         ("add", "."), ("-c", "commit.gpgSign=false", "commit", "-qm", "Base fixture")):
                subprocess.run(["git", *args], cwd=root, check=True, capture_output=True)
            base = subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=root, text=True).strip()
            original.rename(original.with_name("renamed.yaml"))
            (root / "README.md").write_text("An unrelated SDK change without a new changelog entry.\n")
            subprocess.run(["git", "add", "."], cwd=root, check=True, capture_output=True)
            subprocess.run(["git", "-c", "commit.gpgSign=false", "commit", "-qm", "Rename existing fragment"],
                           cwd=root, check=True, capture_output=True)
            changes = subprocess.check_output(["git", "diff", "--name-status", "--find-renames", base + "...HEAD"],
                                              cwd=root, text=True)
            self.assertIn("R100\t.changes/unreleased/original.yaml\t.changes/unreleased/renamed.yaml", changes)
            result = subprocess.run([sys.executable, str(ROOT / "scripts/release.py"), "--root", str(root),
                                     "changelog-check", "--base", base], capture_output=True, text=True)
            self.assertNotEqual(0, result.returncode, result.stdout + result.stderr)
            self.assertIn("add a Changie fragment", result.stderr)

    def test_batch_detected_as_a_rename_is_accepted(self):
        with tempfile.TemporaryDirectory(prefix="b2-release-prep-") as temporary:
            root = pathlib.Path(temporary)
            shutil.copy(ROOT / ".changie.yaml", root)
            (root / ".changes/unreleased").mkdir(parents=True)
            (root / ".changes/header.tpl.md").write_text("# Changelog\n")
            (root / ".changes/v1.2.3.md").write_text("## [1.2.3] - 2026-01-01\n\n### Fixed\n\n* Prior release.\n")
            (root / "gradle.properties").write_text("version=1.2.3\n")
            body = "Validate release metadata before preparing the next version. " * 120
            (root / ".changes/unreleased/long.yaml").write_text(
                "kind: Fixed\nbody: " + body + "\ntime: 2026-01-02T00:00:00Z\n")
            changie = os.environ.get("CHANGIE", "changie")
            subprocess.run([changie, "merge"], cwd=root, check=True, capture_output=True)
            for args in (("init", "-q"), ("config", "user.name", "Release Fixture"),
                         ("config", "user.email", "fixture@example.invalid"), ("config", "diff.renames", "true"),
                         ("add", "."), ("-c", "commit.gpgSign=false", "commit", "-qm", "Base fixture")):
                subprocess.run(["git", *args], cwd=root, check=True, capture_output=True)
            base = subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=root, text=True).strip()
            for args in (("batch", "v1.2.4", "--allow-no-changes=false"), ("merge",)):
                subprocess.run([changie, *args], cwd=root, check=True, capture_output=True)
            subprocess.run(["git", "add", "."], cwd=root, check=True, capture_output=True)
            subprocess.run(["git", "-c", "commit.gpgSign=false", "commit", "-qm", "Batch release"],
                           cwd=root, check=True, capture_output=True)
            changes = subprocess.check_output(["git", "diff", "--name-status", base + "...HEAD"],
                                              cwd=root, text=True)
            self.assertRegex(changes, r"R\d+\t.changes/unreleased/long.yaml\t.changes/v1.2.4.md")
            result = subprocess.run([sys.executable, str(ROOT / "scripts/release.py"), "--root", str(root),
                                     "changelog-check", "--base", base], capture_output=True, text=True)
            self.assertEqual(0, result.returncode, result.stdout + result.stderr)


if __name__ == "__main__":
    unittest.main()
