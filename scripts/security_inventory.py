"""Collect the JARs Maven actually loads, without persisting its debug configuration."""

import argparse
import json
import os
import re
import shutil
import subprocess
from pathlib import Path


ANSI_ESCAPE = re.compile(r'\x1b\[[0-9;]*m')
REALM = re.compile(r'^\[DEBUG\] Populating class realm plugin>(\S+)\s*$')
INCLUDED = re.compile(r'^\[DEBUG\]\s+Included:\s+(\S+)\s*$')
EXECUTION = re.compile(r'^\[INFO\] --- ([^: ]+):([^: ]+):[^ ]+')
TOKEN = re.compile(r'^[A-Za-z0-9_.+\-]+$')


def collect_realms(lines, executions=None, provider_paths=None):
    realms = {}
    current = None
    for raw_line in lines:
        line = ANSI_ESCAPE.sub('', raw_line).strip()
        execution = EXECUTION.match(line)
        if execution and executions is not None:
            executions.add((execution.group(1), execution.group(2)))
        if line.startswith('[DEBUG] provider classpath:'):
            # Surefire separates entries with two spaces; a single space can be part of a path.
            entries = re.split(r' {2,}', line.removeprefix('[DEBUG] provider classpath:').strip())
            paths = [Path(entry) for entry in entries]
            if not paths or any(not path.is_absolute() or path.suffix != '.jar' for path in paths):
                raise ValueError('Malformed Surefire provider classpath; refusing partial coverage.')
            if provider_paths is not None:
                provider_paths.update(paths)
        match = REALM.match(line)
        if match:
            current = match.group(1)
            realms.setdefault(current, set())
            continue
        included = INCLUDED.match(line)
        if current and included:
            realms[current].add(included.group(1))
        elif line.startswith('[DEBUG] Populating class realm') or line.startswith('[DEBUG] Configuring mojo'):
            current = None
    return realms


def coordinate_path(coordinate):
    parts = coordinate.split(':')
    if len(parts) not in (4, 5) or any(not TOKEN.fullmatch(part) or part in ('.', '..') for part in parts):
        raise ValueError('Invalid artifact coordinate: ' + coordinate)
    group, artifact, packaging = parts[:3]
    if packaging != 'jar' or any(not segment for segment in group.split('.')):
        raise ValueError('Unsupported classpath artifact: ' + coordinate)
    classifier = '-' + parts[3] if len(parts) == 5 else ''
    version = parts[-1]
    return Path(*group.split('.'), artifact, version, artifact + '-' + version + classifier + '.jar')


def copy_paths(repository, output, paths):
    repository = repository.resolve()
    verified = []
    for path in sorted(set(paths)):
        source = path.resolve()
        if not source.is_relative_to(repository) or source.suffix != '.jar':
            raise ValueError('Artifact is not a JAR inside the selected Maven repository: ' + str(path))
        if not source.is_file():
            raise FileNotFoundError('Resolved artifact is missing: ' + str(source))
        verified.append((source, source.relative_to(repository)))
    for source, relative in verified:
        destination = output / relative
        destination.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(source, destination)
    return [str(relative) for _, relative in verified]


def copy_artifacts(repository, output, coordinates):
    return copy_paths(repository, output, [repository / coordinate_path(value) for value in coordinates])


def validate_evidence(realms, project_paths, executions=None, provider_paths=None):
    if not realms or not all(realms.values()):
        raise ValueError('Maven did not report nonempty plugin classpaths; refusing a partial inventory.')
    if not project_paths:
        raise ValueError('Maven did not report application/test dependencies; refusing an empty inventory.')
    if executions is not None:
        if not executions:
            raise ValueError('No executed Maven goals were detected; refusing unverified plugin coverage.')
        aliases = set()
        for realm in realms:
            _, artifact, version = realm.split(':')
            aliases.add((artifact, version))
            aliases.add((artifact.removeprefix('maven-').removesuffix('-plugin'), version))
            aliases.add((artifact.removesuffix('-maven-plugin'), version))
        if not executions.issubset(aliases):
            raise ValueError('Some executed Maven plugins have no captured classpath; refusing partial coverage.')
        if any(prefix in ('surefire', 'maven-surefire-plugin') for prefix, _ in executions) and not provider_paths:
            raise ValueError('Surefire executed without a captured provider classpath; refusing partial coverage.')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--maven', default='mvn')
    parser.add_argument('--repository', type=Path, default=Path.home() / '.m2/repository')
    parser.add_argument('--output', type=Path, default=Path('security-inventory'))
    parser.add_argument('--reports', type=Path, default=Path('security-reports'))
    parser.add_argument('--plugin-goal', action='append', default=[])
    args = parser.parse_args()
    output = args.output.resolve()
    reports = args.reports.resolve()
    repository = args.repository.resolve()
    if output.exists() and any(output.iterdir()):
        raise ValueError('Use a new or empty inventory directory, so stale JARs cannot enter the scan.')
    reports.mkdir(parents=True, exist_ok=True)
    classpath_file = reports / 'project-classpath.txt'
    classpath_file.unlink(missing_ok=True)
    command = [
        args.maven, '-B', '-X', '-Dstyle.color=never',
        '-Dmaven.repo.local=' + str(repository),
        '-Dmdep.outputFile=' + str(classpath_file), '-DincludeScope=test',
        'clean', 'verify', 'dependency:build-classpath', *args.plugin_goal,
    ]
    # Debug output can contain credentials. Consume it only in memory and retain artifact coordinates alone.
    executions = set()
    provider_paths = set()
    with subprocess.Popen(command, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, errors='replace') as build:
        try:
            realms = collect_realms(build.stdout, executions, provider_paths)
        except Exception:
            build.kill()
            build.wait()
            raise
        return_code = build.wait()
    if return_code:
        raise SystemExit('Maven inventory build failed (exit ' + str(return_code) + '); no scan may pass.')
    project_paths = [Path(value) for value in classpath_file.read_text().strip().split(os.pathsep) if value]
    validate_evidence(realms, project_paths, executions, provider_paths)
    project_inventory = copy_paths(repository, output / 'application', project_paths)
    plugin_inventory = copy_artifacts(repository, output / 'plugins', set().union(*realms.values()))
    provider_inventory = copy_paths(repository, output / 'test-providers', provider_paths)
    manifest = {
        'project_jars': project_inventory,
        'plugin_jars': plugin_inventory,
        'test_provider_jars': provider_inventory,
        'plugin_realms': {name: sorted(values) for name, values in sorted(realms.items())},
    }
    (reports / 'inventory-manifest.json').write_text(json.dumps(manifest, indent=2) + '\n')
    print('Collected ' + str(len(project_inventory)) + ' project/test JARs and '
          + str(len(plugin_inventory)) + ' plugin JARs from ' + str(len(realms)) + ' executed plugin realms and '
          + str(len(provider_inventory)) + ' test-provider JARs.')


if __name__ == '__main__':
    main()
