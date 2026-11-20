"""Exercise the real Gradle init script with a minimal multi-project Java fixture."""

import os
import pathlib
import subprocess
import tempfile
import unittest
import zipfile


ROOT = pathlib.Path(__file__).resolve().parents[2]
INIT_SCRIPT = ROOT / "scripts/health-classpath.gradle"


class HealthClasspathTest(unittest.TestCase):
    def test_init_script_resolves_the_sdk_runtime_classpath(self):
        with tempfile.TemporaryDirectory(prefix="b2-health-classpath-") as temporary:
            root = pathlib.Path(temporary).resolve()
            (root / "settings.gradle").write_text(
                "rootProject.name = 'health-classpath-fixture'\ninclude 'core', 'httpclient'\n"
                "project(':core').name = 'b2-sdk-core'\n"
                "project(':httpclient').name = 'b2-sdk-httpclient'\n")
            (root / "build.gradle").write_text("subprojects { apply plugin: 'java'; version = '1.0' }\n")
            (root / "core").mkdir()
            httpclient = root / "httpclient"
            httpclient.mkdir()
            (httpclient / "build.gradle").write_text(
                "dependencies { implementation project(':b2-sdk-core'); runtimeOnly files('../deps/fixture.jar') }\n")
            dependency = root / "deps/fixture.jar"
            dependency.parent.mkdir()
            with zipfile.ZipFile(dependency, "w") as jar:
                jar.writestr("META-INF/MANIFEST.MF", "Manifest-Version: 1.0\n")
            result = subprocess.run(
                [str(ROOT / "gradlew"), "--quiet", "--console=plain", "--offline", "--no-daemon",
                 "--project-dir", str(root), "--init-script", str(INIT_SCRIPT),
                 ":b2-sdk-httpclient:healthClasspath"], capture_output=True, text=True, timeout=300)
            self.assertEqual(0, result.returncode, result.stdout + result.stderr)
            expected = {str(httpclient / "build/classes/java/main"), str(httpclient / "build/resources/main"),
                        str(root / "core/build/libs/b2-sdk-core-1.0.jar"), str(dependency)}
            self.assertEqual(expected, set(result.stdout.strip().split(os.pathsep)))


if __name__ == "__main__":
    unittest.main()
