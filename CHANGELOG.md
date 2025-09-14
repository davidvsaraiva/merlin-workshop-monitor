# Changelog
All notable user-facing changes to this project.

## [Unreleased]

## [0.2.0] - 2025-09-14
### Added
- Downloadable **fat JAR** (shaded) published with each GitHub Release.
- CI: headless Selenium tests on push/PR (Java 17 + Chrome).
- Release workflow: on `v*` tags, run tests and attach JARs to the release.
- Dependabot: weekly updates for GitHub Actions & Maven (ignores majors; grouped minor/patch).


## [0.1.0] - 2025-09-14
### Added
- CLI: `--once` (single run) and `--interval-minutes <N>` (default 360).
- Scrapes Qualtrics form for each store and emails newly found workshops.
- State persisted to `~/workshops.json` to avoid duplicate alerts.
- Headless mode via `HEADLESS_MODE` (default: true).

### Known
- Monitored stores are hard-coded (`Loulé`, `Albufeira`).

[Unreleased]: https://github.com/davidvsaraiva/merlin-workshop-monitor/compare/v0.2.0...HEAD
[0.2.0]: https://github.com/davidvsaraiva/merlin-workshop-monitor/releases/tag/v0.2.0
[0.1.0]: https://github.com/davidvsaraiva/merlin-workshop-monitor/releases/tag/v0.1.0