# Changelog
All notable user-facing changes to this project.

## [Unreleased]

## [1.0.0] - 2026-09-02
### Changed
- Replaced the Qualtrics form scraper with Leroy Merlin's own public workshops sitemap
  (`sitemap-idee-projet1.xml`) — no more Selenium/ChromeDriver/browser automation.
- Notifications are no longer store-specific: any newly-added workshop triggers an email,
  regardless of store.
- `workshops.json` schema changed to a flat `url -> entry` map (previously grouped per
  store). **Not compatible with pre-1.0 state files** — delete/replace `~/workshops.json`
  when upgrading.
- Default polling interval changed from 6 hours to 1 hour (`--interval-minutes` remains
  configurable).
- Emails are now sent as HTML, including each workshop's title and thumbnail image (from
  the sitemap) alongside its link.

### Added
- First-run seeding: on a first run (no existing state file), the app seeds state silently
  instead of emailing every pre-existing workshop as "new".

### Removed
- Selenium/ChromeDriver dependency and related env vars (`HEADLESS_MODE`, `IS_CHROMIUM`,
  `CHROMIUM_BROWSER_PATH`, `CHROMIUM_DRIVER_PATH`, `FORM_TO_MONITOR_URL`).
- CI's Chrome install step — no browser needed to build or test anymore.

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

[Unreleased]: https://github.com/davidvsaraiva/merlin-workshop-monitor/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/davidvsaraiva/merlin-workshop-monitor/releases/tag/v1.0.0
[0.2.0]: https://github.com/davidvsaraiva/merlin-workshop-monitor/releases/tag/v0.2.0
[0.1.0]: https://github.com/davidvsaraiva/merlin-workshop-monitor/releases/tag/v0.1.0