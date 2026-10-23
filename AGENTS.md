# AGENTS.md

Powerline is a keyboard-first launcher for Android (Alt+Space opens the search overlay). This file lists what agents need to know before working here. For product details see README.md, PRIVACY.md and ROADMAP.md.

## Language

- Chat with the user in Japanese.
- Everything else in English: code, comments, commit messages, file contents, documents, and any other deliverable.
- Existing Japanese text stays as is unless the task is to update it. New or rewritten text is English.

## Layout

- `app/` — Android app (applicationId / namespace `dev.quxomiya.powerline`)
- `app/src/test/` — JVM unit tests (search pipeline, providers, history)
- `app/src/androidTest/` — instrumented / E2E tests (need an emulator)
- `scripts/device-tests.sh` — runs the instrumented / E2E tests
- `scripts/measure.sh` — measures the numbers in ROADMAP.md §6 on an emulator (release build; needs root like device-tests.sh)
- `ROADMAP.md` — current plan and progress. Read it first when a decision is unclear

## Build and test

```sh
./gradlew :app:assembleDebug        # build
./gradlew :app:testDebugUnitTest    # JVM tests
scripts/device-tests.sh             # instrumented / E2E (emulator or rooted device)
```

- After a change, make sure at least `testDebugUnitTest` passes.
- For changes touching UI or key handling, run `device-tests.sh` when possible. If you cannot, say so in the report.
- Requires JDK 17 and Android SDK 34.
- Alt+Space is only caught from a physical keyboard. Key events injected with `adb shell input keyevent` / `keycombination` do not open the overlay, so do not try them for testing. The overlay can be driven only by `device-tests.sh` (writes to `/dev/input`, so it needs an emulator or rooted device); otherwise ask the user to check it with a physical keyboard.

## Installing on a device

- After a successful build, check `adb devices`. If a device is connected, install to it in preference to an emulator.
- If installation fails (for example a signature mismatch), uninstalling and reinstalling is allowed. State in the report that device data was lost.
- After installing, enable the accessibility service over ADB. Do not ask the user to do it by hand.

```sh
adb shell settings put secure enabled_accessibility_services dev.quxomiya.powerline/dev.quxomiya.powerline.services.PowerlineAccessibilityService
adb shell settings put secure accessibility_enabled 1
```

- After enabling, confirm with `adb shell settings get secure enabled_accessibility_services`.
- If no device is connected, stop after the build and report that installation was not possible.

## Principles

- **No settings**: no settings screen, no persisted preferences, no JSON backup. The exceptions are those listed in README and ROADMAP only.
- **Do not ask the user**: outside system permission screens, do not show dialogs asking for consent or migration.
- **No INTERNET permission**: Powerline has no `INTERNET` permission. Web search goes to the browser.
- **Minimal history**: only launch history (last 100 apps) and favorites are stored. If you store anything new, update PRIVACY.md too.
- **Accessibility service is only for detecting Alt+Space**. Do not extend its use.
- Keep the fixed Material You theme (wallpaper colors on Android 12+, brand colors on 11 and below).

## Code style

- Follow Kotlin official style (`kotlin.code.style=official`).
- Match the comment density, naming and idioms of the surrounding code.
- Before adding a new dependency, state the reason.

## Git

- Branches: work on a branch cut from `main`. Do not commit directly to `main`.
- Commit messages: short imperative English sentence, as in the existing history (e.g. `Add search settings: built-in switches, web engines, share aliases`).
- **Commit automatically per feature**: when one feature, fix or cohesive change is finished and verified, commit it without being asked. Do not mix several features in one commit.
- Do not commit changes that are unverified or half-done.
- Push only when the user asks.

## Notes

- Never commit signing material: `local.properties`, `keystore.properties`, `*.jks` and similar (already in `.gitignore`).
- Update ROADMAP.md progress and its "last updated" date when related work is done.
