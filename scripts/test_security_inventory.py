import importlib.util
import sys
import tempfile
import unittest
from pathlib import Path


MODULE_PATH = Path(__file__).with_name('security_inventory.py')
sys.dont_write_bytecode = True
SPEC = importlib.util.spec_from_file_location('security_inventory', MODULE_PATH)
inventory = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(inventory)


class SecurityInventoryTests(unittest.TestCase):
    def test_only_plugin_realm_artifacts_are_retained(self):
        lines = [
            '[DEBUG]   Included: ignored:before:jar:1.0',
            '[DEBUG] Populating class realm plugin>example:compiler:1.0',
            '[DEBUG]   Included: example:compiler:jar:1.0',
            '[DEBUG]   Included: example:patched:jar:4.0.3',
            '[DEBUG]   Excluded: example:patched:jar:4.0.2',
            '[DEBUG] Configuring mojo execution',
            '[DEBUG]   Included: ignored:after:jar:1.0',
            '[DEBUG] env.PRIVATE_VALUE=must-not-be-retained',
        ]
        realms = inventory.collect_realms(lines)
        self.assertEqual(realms, {
            'example:compiler:1.0': {'example:compiler:jar:1.0', 'example:patched:jar:4.0.3'}
        })

    def test_coordinate_supports_classifier_and_rejects_invalid_paths(self):
        self.assertEqual(
            inventory.coordinate_path('example.group:artifact:jar:tests:1.2'),
            Path('example/group/artifact/1.2/artifact-1.2-tests.jar'),
        )
        with self.assertRaises(ValueError):
            inventory.coordinate_path('../secret:artifact:jar:1.2')

    def test_provider_classpath_preserves_spaces_and_unions_multiple_providers(self):
        first = Path(tempfile.gettempdir()) / 'maven repository' / 'surefire-junit-platform.jar'
        second = first.with_name('common-java5.jar')
        providers = set()
        inventory.collect_realms([
            '[DEBUG] provider classpath:  ' + str(first) + '  ' + str(second),
            '[DEBUG] provider classpath:  ' + str(second),
            '[DEBUG] env.PRIVATE_VALUE=must-not-be-retained',
        ], provider_paths=providers)
        self.assertEqual(providers, {first, second})

    def test_malformed_or_partially_captured_provider_classpath_fails(self):
        valid = str(Path(tempfile.gettempdir()) / 'provider.jar')
        for value in ['', 'relative.jar', valid + '  truncated-entry', valid + '  /missing-version']:
            with self.subTest(value=value), self.assertRaises(ValueError):
                inventory.collect_realms(['[DEBUG] provider classpath:  ' + value], provider_paths=set())

    def test_executed_surefire_requires_provider_classpath(self):
        realms = {'org.apache.maven.plugins:maven-surefire-plugin:3.5.6': {
            'org.apache.maven.plugins:maven-surefire-plugin:jar:3.5.6'}}
        executions = {('surefire', '3.5.6')}
        with self.assertRaises(ValueError):
            inventory.validate_evidence(realms, [Path('project.jar')], executions, set())
        inventory.validate_evidence(realms, [Path('project.jar')], executions, {Path('provider.jar')})

    def test_missing_artifact_fails_instead_of_producing_partial_inventory(self):
        with tempfile.TemporaryDirectory() as temporary:
            repository = Path(temporary) / 'repository'
            repository.mkdir()
            output = Path(temporary) / 'output'
            with self.assertRaises(FileNotFoundError):
                inventory.copy_artifacts(repository, output, {'example:missing:jar:1.0'})
            self.assertFalse(output.exists())

    def test_empty_classpath_or_realms_cannot_pass(self):
        with self.assertRaises(ValueError):
            inventory.validate_evidence({}, [])
        with self.assertRaises(ValueError):
            inventory.validate_evidence({'example:plugin:1': {'example:plugin:jar:1'}}, [])

    def test_executed_plugin_without_captured_realm_cannot_pass(self):
        realms = {'example:maven-compiler-plugin:1.0': {'example:maven-compiler-plugin:jar:1.0'}}
        with self.assertRaises(ValueError):
            inventory.validate_evidence(realms, [Path('example.jar')], {('compiler', '1.0'), ('resources', '1.0')})
        inventory.validate_evidence(realms, [Path('example.jar')], {('compiler', '1.0')})

    def test_resolved_patch_is_copied_without_unused_cached_versions(self):
        with tempfile.TemporaryDirectory() as temporary:
            repository = Path(temporary) / 'repository'
            for version in ['4.0.2', '4.0.3']:
                artifact = repository / inventory.coordinate_path('example:utils:jar:' + version)
                artifact.parent.mkdir(parents=True, exist_ok=True)
                artifact.write_bytes(b'jar fixture')
            output = Path(temporary) / 'output'
            inventory.copy_artifacts(repository, output, {'example:utils:jar:4.0.3'})
            self.assertEqual([path.name for path in output.rglob('*.jar')], ['utils-4.0.3.jar'])


if __name__ == '__main__':
    unittest.main()
