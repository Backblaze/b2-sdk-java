"""Exercise API decisions using paginated fixtures; never contact GitHub."""

import json
import os
import pathlib
import subprocess
import sys
import tempfile
import unittest


SCRIPT = pathlib.Path(__file__).resolve().parents[1] / "release.py"
SOURCES = ("code-scanning/alerts", "dependabot/alerts", "secret-scanning/alerts", "security-advisories", "issues")


class ReleaseGateTest(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.root = pathlib.Path(self.temporary.name)
        fake = self.root / "gh"
        fake.write_text("""#!/usr/bin/env python3
import json, os, sys
if '--paginate' not in sys.argv or '--slurp' not in sys.argv:
    sys.exit(90)
endpoint = next(arg for arg in sys.argv if arg.startswith('repos/')).split('?')[0].split('repos/example/sdk/')[1]
fixture = json.loads(os.environ['RELEASE_GATE_FIXTURE'])
value = fixture.get(endpoint, 'unreadable')
if value == 'unreadable':
    sys.exit(1)
print(json.dumps(value))
""")
        fake.chmod(0o755)
        self.data = {source: [[]] for source in SOURCES}
        self.data["milestones"] = [[{"title": "v1.2.3", "number": 4, "open_issues": 0}]]

    def run_gate(self, command="security", *arguments):
        environment = dict(os.environ, PATH=str(self.root) + os.pathsep + os.environ["PATH"],
                           RELEASE_GATE_FIXTURE=json.dumps(self.data), GH_TOKEN="test-only")
        return subprocess.run([sys.executable, str(SCRIPT), command, *arguments, "--repo", "example/sdk"],
                              env=environment, text=True, capture_output=True)

    def test_all_readable_empty_sources_pass(self):
        result = self.run_gate()
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertIn("security gate: clear", result.stdout)

    def test_medium_high_and_critical_dependencies_block_even_without_patch(self):
        for severity in ("medium", "high", "critical"):
            with self.subTest(severity=severity):
                self.data["dependabot/alerts"] = [[], [{"state": "open", "security_advisory": {"severity": severity}}]]
                result = self.run_gate()
                self.assertNotEqual(0, result.returncode)
                self.assertIn("Dependabot", result.stdout)

    def test_open_code_scanning_and_severity_labeled_issues_block(self):
        self.data["code-scanning/alerts"] = [[{"state": "open", "rule": {"security_severity_level": "high"}}]]
        self.data["issues"] = [[{"state": "open", "labels": [{"name": "severity: medium"}]}]]
        result = self.run_gate()
        self.assertNotEqual(0, result.returncode)
        self.assertIn("Code scanning: HOLD", result.stdout)
        self.assertIn("Issues: HOLD", result.stdout)

    def test_low_or_closed_findings_do_not_block(self):
        self.data["dependabot/alerts"] = [[
            {"state": "open", "security_advisory": {"severity": "low"}},
            {"state": "dismissed", "security_advisory": {"severity": "critical"}},
        ]]
        result = self.run_gate()
        self.assertEqual(0, result.returncode, result.stderr)

    def test_unreadable_or_malformed_security_response_blocks(self):
        for value in ("unreadable", {"message": "not an alerts list"}):
            with self.subTest(value=value):
                self.data["code-scanning/alerts"] = value
                result = self.run_gate()
                self.assertNotEqual(0, result.returncode)
                self.assertIn("cannot verify", result.stderr)

    def test_draft_advisory_and_secret_alerts_block_without_disclosing_contents(self):
        self.data["security-advisories"] = [[{"state": "draft", "severity": "medium", "summary": "PRIVATE"}]]
        self.data["secret-scanning/alerts"] = [[{"state": "open", "secret": "PRIVATE"}]]
        result = self.run_gate()
        self.assertNotEqual(0, result.returncode)
        self.assertNotIn("PRIVATE", result.stdout + result.stderr)

    def test_unscored_active_advisory_blocks(self):
        self.data["security-advisories"] = [[{"state": "triage", "severity": None}]]
        self.assertNotEqual(0, self.run_gate().returncode)

    def test_unscored_code_scanning_alert_blocks(self):
        self.data["code-scanning/alerts"] = [[{"state": "open", "rule": {"id": "unscored-fixture"}}]]
        result = self.run_gate()
        self.assertNotEqual(0, result.returncode)
        self.assertIn("Code scanning: HOLD", result.stdout)

    def test_unknown_finding_state_is_not_clean(self):
        self.data["dependabot/alerts"] = [[{"state": "unexpected", "security_advisory": {"severity": "high"}}]]
        result = self.run_gate()
        self.assertNotEqual(0, result.returncode)
        self.assertIn("cannot verify", result.stderr)

    def test_complete_version_milestone_passes(self):
        result = self.run_gate("milestone", "v1.2.3")
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertIn("milestone gate: clear", result.stdout)

    def test_missing_milestone_or_open_work_blocks(self):
        self.data["milestones"] = [[]]
        self.assertNotEqual(0, self.run_gate("milestone", "v1.2.3").returncode)
        self.data["milestones"] = [[{"title": "v1.2.3", "number": 4, "open_issues": 1}]]
        self.assertNotEqual(0, self.run_gate("milestone", "v1.2.3").returncode)


if __name__ == "__main__":
    unittest.main()
