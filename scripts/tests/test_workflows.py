"""Release workflow policy and GitHub Pages status contract checks."""

import json
import pathlib
import re
import subprocess
import sys
import unittest


ROOT = pathlib.Path(__file__).resolve().parents[2]


class WorkflowTest(unittest.TestCase):
    def test_every_external_action_is_pinned_to_commit(self):
        for workflow in (ROOT / ".github/workflows").glob("*.yml"):
            for action in re.findall(r"uses:\s*([^\s#]+)", workflow.read_text()):
                with self.subTest(workflow=workflow.name, action=action):
                    self.assertRegex(action, r"^[A-Za-z0-9_./-]+@[0-9a-f]{40}$")

    def test_replaced_community_actions_are_absent(self):
        workflows = "\n".join(path.read_text() for path in (ROOT / ".github/workflows").glob("*.yml"))
        for action in ("crazy-max/ghaction-github-pages", "crazy-max/ghaction-github-status", "softprops/action-gh-release"):
            self.assertNotIn(action, workflows)

    def test_publish_command_and_signing_secrets_are_preserved(self):
        workflow = (ROOT / ".github/workflows/deploy.yml").read_text()
        self.assertIn("./gradlew publishToSonatype closeAndReleaseSonatypeStagingRepository createBundle --no-daemon --stacktrace", workflow)
        for secret in ("GPG_SIGNING_KEY", "GPG_PASSPHRASE", "SONATYPE_TOKEN_USERNAME", "SONATYPE_TOKEN_PASSWORD"):
            self.assertIn("secrets." + secret, workflow)

    def test_release_checks_and_deploy_use_the_same_event_commit(self):
        workflow = (ROOT / ".github/workflows/deploy.yml").read_text()
        self.assertIn('verify-commit "$RELEASE_TAG" "$GITHUB_SHA"', workflow)
        deploy = workflow.split("\n  deploy:\n", 1)[1]
        self.assertIn("ref: ${{ github.sha }}", deploy)


class PagesStatusTest(unittest.TestCase):
    def run_status(self, payload):
        return subprocess.run([sys.executable, str(ROOT / "scripts/release.py"), "pages-status"],
                              input=json.dumps(payload), text=True, capture_output=True)

    def test_operational_pages_passes(self):
        result = self.run_status({"components": [{"name": "Pages", "status": "operational"}]})
        self.assertEqual(0, result.returncode, result.stderr)

    def test_major_outage_blocks_deployment(self):
        result = self.run_status({"components": [{"name": "Pages", "status": "major_outage"}]})
        self.assertNotEqual(0, result.returncode)
        self.assertIn("major outage", result.stderr)

    def test_unreadable_status_is_not_treated_as_operational(self):
        result = self.run_status({"components": []})
        self.assertNotEqual(0, result.returncode)
        self.assertIn("Pages status", result.stderr)


if __name__ == "__main__":
    unittest.main()
