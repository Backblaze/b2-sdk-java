"""Check provider and generic alert queries against the documented API contract."""

import json
import os
import pathlib
import subprocess
import sys
import tempfile
import unittest
from urllib.parse import parse_qs


SCRIPT = pathlib.Path(__file__).resolve().parents[1] / "release.py"
GENERIC_TYPES = ("ec_private_key", "generic_private_key", "http_basic_authentication_header",
                 "http_bearer_authentication_header", "mongodb_connection_string", "mysql_connection_url",
                 "openssh_private_key", "pgp_private_key", "postgres_connection_string", "rsa_private_key", "password")


class SecretPatternGateTest(unittest.TestCase):
    def setUp(self):
        temporary = tempfile.TemporaryDirectory(prefix="b2-secret-query-")
        self.addCleanup(temporary.cleanup)
        self.root = pathlib.Path(temporary.name)
        self.log = self.root / "requests.jsonl"
        gh = self.root / "gh"
        gh.write_text("#!" + sys.executable + "\n" + '''
import json, os, sys
from urllib.parse import parse_qs
if '--paginate' not in sys.argv or '--slurp' not in sys.argv:
    sys.exit(90)
request = next(arg for arg in sys.argv if arg.startswith('repos/example/sdk/'))
resource, _, query = request.partition('?')
parameters = parse_qs(query)
with open(os.environ['SECRET_QUERY_LOG'], 'a') as output:
    output.write(json.dumps(request) + '\\n')
pages = [[]]
if resource.endswith('/secret-scanning/alerts'):
    target = os.environ['SECRET_QUERY_TYPE']
    generic = 'secret_type' in parameters
    selected = parameters.get('secret_type', [''])[0].split(',')
    matches = (generic and target in selected) or (not generic and not target)
    mode = os.environ['SECRET_QUERY_MODE']
    if generic and mode == 'unreadable':
        sys.exit(1)
    if generic and mode == 'malformed':
        print(json.dumps({'message': 'not an alert list'}))
        sys.exit(0)
    if matches and mode != 'empty':
        pages = [[], [{'state': 'open', 'secret_type': target or 'example_provider', 'secret': 'DO-NOT-PRINT'}]]
print(json.dumps(pages))
''')
        gh.chmod(0o755)

    def run_gate(self, secret_type="rsa_private_key", mode="open"):
        self.log.write_text("")
        environment = dict(os.environ, PATH=str(self.root) + os.pathsep + os.environ["PATH"], GH_TOKEN="test-only",
                           SECRET_QUERY_LOG=str(self.log), SECRET_QUERY_TYPE=secret_type, SECRET_QUERY_MODE=mode)
        result = subprocess.run([sys.executable, str(SCRIPT), "security", "--repo", "example/sdk"],
                                env=environment, capture_output=True, text=True)
        requests = [json.loads(line) for line in self.log.read_text().splitlines()]
        return result, requests

    def test_each_generic_pattern_blocks_without_disclosing_values(self):
        for secret_type in GENERIC_TYPES:
            with self.subTest(secret_type=secret_type):
                result, requests = self.run_gate(secret_type)
                self.assertNotEqual(0, result.returncode, result.stdout + result.stderr)
                self.assertIn("Generic secret scanning: HOLD", result.stdout)
                self.assertNotIn("DO-NOT-PRINT", result.stdout + result.stderr)
                self.assertTrue(any(secret_type in parse_qs(request.partition('?')[2]).get('secret_type', [''])[0].split(',')
                                    for request in requests), requests)

    def test_unreadable_or_malformed_generic_alerts_hold_the_release(self):
        for mode in ("unreadable", "malformed"):
            with self.subTest(mode=mode):
                result, _ = self.run_gate(mode=mode)
                self.assertNotEqual(0, result.returncode, result.stdout + result.stderr)
                self.assertIn("cannot verify", result.stderr)

    def test_provider_alerts_remain_blocking(self):
        result, _ = self.run_gate(secret_type="")
        self.assertNotEqual(0, result.returncode)
        self.assertIn("Secret scanning: HOLD", result.stdout)
        self.assertNotIn("DO-NOT-PRINT", result.stdout + result.stderr)

    def test_empty_queries_pass_and_request_hidden_secret_values(self):
        result, requests = self.run_gate(mode="empty")
        self.assertEqual(0, result.returncode, result.stdout + result.stderr)
        parameters = [parse_qs(request.partition('?')[2]) for request in requests if '/secret-scanning/alerts?' in request]
        self.assertTrue(any('secret_type' in item for item in parameters), requests)
        self.assertTrue(any('secret_type' not in item for item in parameters), requests)
        self.assertTrue(all(item.get('hide_secret') == ['true'] for item in parameters), requests)


if __name__ == "__main__":
    unittest.main()
