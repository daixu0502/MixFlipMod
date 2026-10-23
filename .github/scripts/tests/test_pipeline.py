import hashlib
import json
import os
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

from test_release import FakeGitHub, item, release


class PipelineTests(unittest.TestCase):
    def setUp(self):
        directory = tempfile.TemporaryDirectory()
        self.addCleanup(directory.cleanup)
        self.root = Path(directory.name)
        self.metadata = {"versionName": "3.1.0", "versionCode": 30100, "compileSdk": 37,
                         "compileSdkMinor": 0, "buildToolsVersion": "36.0.0"}
        self.work = self.root / "build/github-release"
        for name, value in (("ROOT", self.root), ("WORK", self.work),
                            ("PLAN", self.work / "plan.json"), ("NOTES", self.work / "notes.md"),
                            ("DIST", self.work / "dist")):
            self.enterContext(patch.object(release, name, value))
        metadata_file = self.root / "app/build/release-metadata.json"
        metadata_file.parent.mkdir(parents=True)
        metadata_file.write_text(json.dumps(self.metadata), encoding="utf-8")
        self.api = FakeGitHub()
        self.client = self.enterContext(patch.object(release, "GitHub", return_value=self.api))
        self.outputs = self.enterContext(patch.object(release, "output"))
        self.enterContext(patch.object(release, "summary"))

    def test_wrong_tag_fails_before_github_access(self):
        with patch.dict(os.environ, {"GITHUB_REF": "refs/tags/v9.9"}):
            with self.assertRaisesRegex(ValueError, "Tag must match"):
                release.prepare()
        self.client.assert_not_called()

    def test_public_version_skips_build_and_plan(self):
        self.api.releases = [item(1, "v3.1.0")]
        with patch.dict(os.environ, {"GITHUB_REF": "refs/heads/main"}):
            release.prepare()
        self.outputs.assert_any_call("publish", "false")
        self.assertFalse(release.PLAN.exists())

    def prepare_apk(self):
        self.work.mkdir(parents=True)
        release.PLAN.write_text(json.dumps(self.metadata), encoding="utf-8")
        directory = self.root / "app/build/outputs/apk/release"
        directory.mkdir(parents=True)
        filename = "MixFlipMod_3.1.0_30100.apk"
        source = directory / filename
        source.write_bytes(b"test-only APK")
        output = {"applicationId": "vip.chener.mixflipmod", "elements": [{
            "outputFile": filename, "versionName": "3.1.0", "versionCode": 30100,
        }]}
        return directory, source, output

    def test_apk_keeps_generated_filename_and_valid_checksum(self):
        directory, source, output = self.prepare_apk()
        (directory / "output-metadata.json").write_text(json.dumps(output), encoding="utf-8")
        with patch.object(release, "verify_apk_metadata") as verify:
            release.package()
        verify.assert_called_once_with(source, self.metadata)
        apk = release.DIST / source.name
        self.assertEqual(source.read_bytes(), apk.read_bytes())
        digest = hashlib.sha256(apk.read_bytes()).hexdigest()
        self.assertEqual(f"{digest}  {apk.name}\n",
                         apk.with_suffix(".apk.sha256").read_text(encoding="utf-8"))

    def test_other_app_or_version_is_not_staged(self):
        directory, _, output = self.prepare_apk()
        invalid = [
            {**output, "applicationId": "vip.chener.musicenhance"},
            {**output, "elements": [{**output["elements"][0], "versionCode": 999}]},
            {**output, "elements": [{**output["elements"][0], "outputFile": "../wrong.apk"}]},
            {**output, "elements": output["elements"] * 2},
        ]
        for metadata in invalid:
            with self.subTest(metadata=metadata):
                (directory / "output-metadata.json").write_text(json.dumps(metadata), encoding="utf-8")
                with self.assertRaises(ValueError):
                    release.package()
                self.assertFalse(release.DIST.exists())

    def test_bad_apk_header_stops_staging(self):
        directory, _, output = self.prepare_apk()
        (directory / "output-metadata.json").write_text(json.dumps(output), encoding="utf-8")
        with patch.object(release, "verify_apk_metadata", side_effect=ValueError("Bad APK header")):
            with self.assertRaisesRegex(ValueError, "Bad APK"):
                release.package()
        self.assertFalse(release.DIST.exists())

    def test_signing_requires_secrets(self):
        with patch.dict(os.environ, {}, clear=True), self.assertRaisesRegex(ValueError, "KEY_STORE"):
            release.signing()

    def test_signing_rejects_bad_base64(self):
        env = {"MIXFLIPMOD_KEYSTORE_BASE64": "@@@",
               "ORG_GRADLE_PROJECT_androidStorePassword": "test-password",
               "ORG_GRADLE_PROJECT_androidKeyAlias": "test-alias",
               "ORG_GRADLE_PROJECT_androidStoreFile": str(self.root / "bad.jks")}
        with patch.dict(os.environ, env, clear=True), self.assertRaisesRegex(ValueError, "Base64"):
            release.signing()
        self.assertFalse((self.root / "bad.jks").exists())

    def test_signing_accepts_wrapped_base64(self):
        keystore = self.root / "test-signing.jks"
        env = {"MIXFLIPMOD_KEYSTORE_BASE64": " dGVzdC1rZXk=\n",
               "ORG_GRADLE_PROJECT_androidStorePassword": "test-password",
               "ORG_GRADLE_PROJECT_androidKeyAlias": "test-alias",
               "ORG_GRADLE_PROJECT_androidStoreFile": str(keystore)}
        with patch.dict(os.environ, env, clear=True):
            release.signing()
        self.assertEqual(b"test-key", keystore.read_bytes())


if __name__ == "__main__":
    unittest.main()
