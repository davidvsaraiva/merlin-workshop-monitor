# Changelog
All notable user-facing changes to this project.

## [Unreleased]

## [0.1.0] - 2025-09-12
### Added
- CLI: `--once` (single run) and `--interval-minutes <N>` (default 360).
- Scrapes Qualtrics form for each store and emails newly found workshops.
- State persisted to `~/workshops.json` to avoid duplicate alerts.
- Headless mode via `HEADLESS_MODE` (default: true).

### Known
- Monitored stores are hard-coded (`Loulé`, `Albufeira`).

[Unreleased]: 
[0.1.0]: 