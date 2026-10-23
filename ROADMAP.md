# Powerline Cleanup & Optimization Roadmap

> **Status:** Not started
> **Baseline:** `v2/phase6-7-cleanup-release` @ `4b6aa57` (V2 feature-complete, not yet merged to `main`)
> **Target:** V2.1 — same features, less code, faster overlay
> **Last updated:** 2026-10-23

---

## 1. Goal

V2 rebuilt Powerline around the Alt+Space overlay. It works, but it still carries V1 compatibility code, test-only defaults in production classes, unused dependencies, and loading work that is repeated more often than it needs to be.

This roadmap makes the app **smaller, simpler and faster without changing what the user sees or does**. Every phase must leave behavior, stored data and the settings on the status screen exactly as they are.

### Goals

| # | Goal | Measure |
|---|---|---|
| C1 | **Remove dead code** — V1 leftovers, unused fields, unused dependencies | Lines of main code, dependency count |
| C2 | **Simplify** — one way to do each thing; state out of composables; smaller files | Largest file < 350 lines; no logic duplicated across classes |
| C3 | **Faster overlay open** — first and later Alt+Space presses | `SearchOverlay` "Shown N ms" log (target: first open < 200 ms, later opens < 80 ms) |
| C4 | **Faster search** — less work per keystroke | `PerformanceTest` (1000 apps), per-query allocations |
| C5 | **Less idle work** — no full reloads on unrelated package events | Reload count during a Play Store update burst |
| C6 | **Smaller APK** | Release APK size |

### Non-goals

- New features, new settings, or changes to existing settings (AGENTS.md: no settings beyond README / this file)
- Changing the stored format of `history`, `favorite_apps`, `search_engines` or `share_targets` in a way that loses existing data
- Renaming `StatusBarAccessibilityService` or moving its package. Android stores the enabled service by component name, so a rename turns the service off and the user would have to enable it again.
- Using the accessibility service for anything beyond Alt+Space (AGENTS.md)
- UI redesign

---

## 2. Rules for this work

- **Measure before and after.** Every optimization item records its number in §6 before it is merged. If an optimization does not move the number, revert it.
- **One item per commit**, each with `testDebugUnitTest` passing. Items that touch the overlay, key handling or `AppIndex` also run `scripts/device-tests.sh`.
- **No behavior changes.** If a cleanup would change behavior (ordering, timing, what is shown), it is out of scope or needs its own decision recorded in §7.
- **New dependencies need a stated reason** (AGENTS.md). Removing dependencies needs none.

---

## 3. Findings (from a read of the V2 code)

### 3.1 Dead code and leftovers

| Where | What | Action |
|---|---|---|
| `data/UserDataRepository.kt` `StoredHistoryItem` | Reads V1 history entries (`id` / `type == "APP"`). V1 was `com.quartz.powerline`, so its data never exists in this app's storage (the file itself says so). | Remove the V1 fields; read `HistoryItem` directly |
| `ui/viewmodel/SearchViewModel.kt` `refreshRecentApps` | Fallback to "the package's first activity for V1 entries without a class" | Keep the fallback only if pre-release V2 builds wrote entries without `className`; otherwise remove |
| `data/UserDataRepository.kt` `removeLegacyKeys` | Reads every pref key each time a repository is created, to remove keys from pre-release V2 builds | Run once per app version (or drop after V2.0 is out) |
| `search/SearchRequest.kt` `rawQuery` | Set by `QueryParser`, never read | Remove |
| `search/SearchUiState.kt` `isLoading` | Set by `SearchViewModel`, never read by the UI | Remove, or use it; decide in §7 |
| `search/SearchProvider.kt` `displayName`, `icon` | Not read through the interface | Remove from the interface (providers may keep a private icon) |
| `data/PowerlineDefaults.kt` `OVERLAY_CLOSE_ON_BACKGROUND_TAP` | Always `true`; a V1 preference turned into a constant | Inline |
| `overlay/SearchOverlay.kt` | `e.printStackTrace()` on add / remove view | `Log.w` with a tag, like the rest of the app |
| `ui/theme` `CommandLauncherTheme`, style `Theme.CommandLauncher` | Pre-Powerline name | Rename to `PowerlineTheme` / `Theme.Powerline` (safe: not stored anywhere) |

### 3.2 Unused or replaceable dependencies (`app/build.gradle.kts`)

| Dependency / setting | Status | Action |
|---|---|---|
| `mockito-core`, `mockito-inline` | No test imports Mockito | Remove |
| `androidx.arch.core:core-testing` | No test uses it | Remove |
| `ui-tooling-preview` (implementation) | No `@Preview` in the code | Remove, or move to `debugImplementation` |
| `material-icons-extended` | 13 icons used (`Apps`, `Calculate`, `Public`, `Shortcut`, `FavoriteBorder`, ...). Large artifact; slows builds; R8 removes the rest only in release | Copy the used icons as `ImageVector`s or vector drawables, drop the dependency |
| `gson` | Used for 3 small JSON lists, needs keep rules in `proguard-rules.pro` | Replace with `org.json` (in the platform) reading the same JSON; drop the keep rules |
| `vectorDrawables.useSupportLibrary` | Not needed with `minSdk 26` | Remove |
| `android.enableJetifier=true` | Probably no support-library dependency left | Remove if the build passes without it |
| `lint { abortOnError = false }` | Lint errors never fail the build | Fix current errors, then let lint fail the build |

### 3.3 Duplicated or scattered logic

- **Built-in switches are applied in four places**: each provider's `enabled` lambda, the `QueryParser` lambdas in `SearchViewModel`, the chip filter in `SearchViewModel.aliasChips`, and `homeEnabled`. One place should answer "which engines / targets / providers are active".
- **Alias chip IDs** (`"web:${id}"`, `"share:${id}"`) are built in `aliasChips` and again in `setQuery`.
- **`SearchMode`** repeats what `engine` / `shareTarget` already say (`ALIAS` ⇔ one of them is set).
- **Two repositories on one prefs file** (`powerline_settings`): `SearchSettingsRepository` is a process singleton, `UserDataRepository` is created per `SearchViewModel`. Each has its own `Gson`. The singleton cache holds a `Pair<Context, Repository>` to handle a context change that does not happen outside tests.
- **Test-only defaults in production constructors**: `AppIndex(engines = ..., shareTargets = ...)`, `QueryParser(List, List)`, `ProviderRegistry(isEnabled = { true }, ...)`. Move test conveniences into test helpers.

### 3.4 Large files

| File | Lines | Split |
|---|---|---|
| `ui/settings/SearchSettings.kt` | 573 | Built-in / Advanced, Web search (list + engine dialog), Share (list + target dialog), shared rows |
| `ui/SearchScreen.kt` | 287 | Move standby keyboard navigation (`standbyIndex`, `chipIndex`, Tab order) into a plain state holder next to `StandbyGrid`, unit-tested on the JVM |
| `ui/viewmodel/SearchViewModel.kt` | 252 | Alias chip / field splitting into its own class |

### 3.5 Startup and runtime cost

**Overlay open (Alt+Space)**
- `SearchViewModel` is created lazily on the first `show()`. That first press builds `AppIndex`, and `AppIndex` loads every app's label and icon. The first open after the service starts pays for all of it.
- Every `show()` creates a new `ComposeView` and composes the whole panel from scratch; `hide()` throws it away.

**App index**
- `AppIndex.loadApps` loads **every app's icon** (`loadIcon`) up front, although the grid shows about 30 tiles at once.
- Every `LauncherApps.Callback` event runs a **full reload** (all apps, plus all engine and share target icons) with no debounce. A Play Store update burst reloads once per package.
- Engine icons are resolved again on every package event (two `PackageManager` queries per engine), even when no browser changed.

**Per keystroke**
- `SearchRanker.match` splits each target into words and builds initials on **every call**, for every app key, on every keystroke. These depend only on the app and can be computed once in `IndexedApp`.
- `RankingSignals` reads `getFavoriteApps()` and `getAppFrecency()` per search. `getAppFrecency()` parses the history JSON every time.
- `isFavorite()` reads SharedPreferences during composition of the long-press menu.

**Rendering**
- `AppIcon` wraps an `ImageView` in `AndroidView` for every tile and result. Inside a lazy grid this is costlier than a Compose `Image` of a cached bitmap.

**Process start**
- No Baseline Profile, so Compose code is interpreted / JIT-compiled on the first opens after install or update.

---

## 4. Phases

### Phase 0 — Baseline

- [ ] Merge V2 into `main` (prerequisite; cleanup branches are cut from `main`).
- [ ] Record the baseline in §6: release APK size, debug/release method count, cold start of `MainActivity`, overlay first open and later opens (the `SearchOverlay` log), `PerformanceTest` timing, number of `AppIndex` reloads while 10 apps update.
- [ ] Add a JVM benchmark for `AppsProvider` + `SearchRanker` over 1000 apps (extends `PerformanceTest`) so Phase 3 has a number to move.

### Phase 1 — Dead code and dependencies

- [ ] Remove the V1 history format (`StoredHistoryItem` V1 fields) — keep reading the current format unchanged (`UserDataRepositoryTest` covers it).
- [ ] Decide and apply the `className == null` fallback in `refreshRecentApps`.
- [ ] Run `removeLegacyKeys` once per `versionCode` instead of on every repository creation.
- [ ] Remove `SearchRequest.rawQuery`, `SearchProvider.displayName` / `icon`, `OVERLAY_CLOSE_ON_BACKGROUND_TAP`; resolve `isLoading` (§7).
- [ ] Replace `printStackTrace` with `Log`.
- [ ] Remove `mockito-*`, `core-testing`, `ui-tooling-preview` (or move to debug), `useSupportLibrary`, Jetifier.
- [ ] Rename `CommandLauncherTheme` / `Theme.CommandLauncher` to Powerline names.

**Done when:** build and all tests pass; no change visible on the device.

### Phase 2 — Simplification

- [ ] One source of truth for active features: a small `ActiveFeatures` (or similar) derived from `SearchSettingsRepository` that providers, parser and chips all read. Remove the per-provider `enabled` lambdas.
- [ ] Build alias chip IDs in one place.
- [ ] Remove `SearchMode`; derive alias mode from `engine` / `shareTarget`.
- [ ] Merge storage: one process-wide owner of `powerline_settings` (settings + history + favorites), one JSON codec. Keep the keys and JSON shape unchanged.
- [ ] Replace Gson with `org.json` for the three lists; delete the Gson keep rules. Add a test that reads JSON written by the Gson version.
- [ ] Move test-only constructor defaults into test helpers.
- [ ] Split `SearchSettings.kt`; move standby keyboard navigation out of `SearchScreen` into a tested state holder.
- [ ] Replace `material-icons-extended` with the 13 icons the app uses.
- [ ] Fix lint errors and turn `abortOnError` back on.

**Done when:** largest file < 350 lines; Built-in switches still behave exactly as before (existing provider and E2E tests pass); APK size recorded.

### Phase 3 — Search and index performance

- [ ] Precompute word lists and initials in `IndexedApp`; `SearchRanker.match` takes the precomputed form.
- [ ] Keep favorites and frecency in memory (updated on launch / toggle); stop parsing history JSON per search.
- [ ] Debounce `LauncherApps.Callback` reloads (e.g. 500 ms), and update only the changed package where possible.
- [ ] Reload engine / share target icons only when engines or targets change, or when a package that could handle them changes.
- [ ] Try `LauncherApps.getActivityList` instead of `queryIntentActivities` for the app list (badged icons for work profiles come for free); keep it only if it is faster.
- [ ] Check whether the per-provider `async` + timeout in `SearchPipeline` still earns its cost with four in-memory providers. Simplify only if timings show the providers finish in well under a millisecond, and record the decision in §7.

**Done when:** the Phase 0 benchmark improves; reload count during an update burst drops to about 1.

### Phase 4 — Overlay open and rendering

- [ ] Create `SearchViewModel` / `AppIndex` in `onServiceConnected` on a background thread so the first Alt+Space does not load the index.
- [ ] Load app icons lazily (per visible tile) and cache them; labels stay eager for search.
- [ ] Replace `AndroidView(ImageView)` in `AppIcon` with a Compose `Image` of a cached `ImageBitmap`, if measurements show a gain.
- [ ] Keep the `ComposeView` between opens (detach / re-add, or keep the window and toggle visibility). Check that focus, IME and the fade-out still behave. Keep it only if later opens get faster.
- [ ] Add a Baseline Profile for the overlay path and `MainActivity` (needs `androidx.profileinstaller` and a profile generator module. Reason: AOT-compiles the Compose code used on first open).

**Done when:** first open < 200 ms, later opens < 80 ms on the reference emulator (log from `OverlayE2ETest`); memory while idle not higher than baseline.

### Phase 5 — Toolchain (optional)

Only if Phases 1–4 are done. This is the largest change, and it is where the V2 roadmap drew the line.

- [ ] Java / JVM target 1.8 → 17.
- [ ] AGP 8.2 → current, Kotlin 1.9 → 2.x with the Compose compiler Gradle plugin (strong skipping cuts recomposition), Compose BOM update, `compileSdk` / `targetSdk` update with the behavior changes reviewed.

---

## 5. Risks

| Risk | Mitigation |
|---|---|
| Storage refactor loses user history or settings | Keys and JSON shape stay the same; tests read data written by the previous build before the change merges |
| Keeping the overlay view between opens breaks focus / IME / ASCII input | `OverlayE2ETest` plus a manual check with Gboard and Google Japanese Input |
| Prewarming in `onServiceConnected` raises idle memory | Measure idle memory before / after; drop the prewarm if the cost is large |
| Lazy icons make tiles pop in while scrolling | Load the first screen of icons with the index; lazy only below the fold |
| Renaming anything stored by Android (service component) turns the service off | Non-goal; listed in §1 |

---

## 6. Measurements

Reference device: _to fill in Phase 0_ (emulator image and API level).

| Metric | Baseline | After P1 | After P2 | After P3 | After P4 |
|---|---|---|---|---|---|
| Release APK size | | | | | |
| Main Kotlin lines (`app/src/main`) | 4,120 | | | | |
| `MainActivity` cold start | | | | | |
| Overlay first open (ms) | | | | | |
| Overlay later opens (ms) | | | | | |
| Search, 1000 apps (ms / query) | | | | | |
| `AppIndex` reloads, 10-app update | | | | | |
| Idle memory (PSS, service only) | | | | | |

---

## 7. Open decisions

| # | Question | Default if undecided |
|---|---|---|
| 1 | `isLoading`: remove, or show a subtle loading state? | Remove (no visible change) |
| 2 | Keep the `className == null` fallback for history entries? | Keep until a check shows no stored entry lacks `className` |
| 3 | Simplify `SearchPipeline` timeouts after Phase 3 timings? | Keep |
| 4 | Android Auto Backup includes `powerline_settings` (`backup_rules.xml`, `data_extraction_rules.xml`), so launch history goes to the user's cloud backup. Keep, or exclude `history`? | Keep; if changed, update PRIVACY.md |
| 5 | Do Phase 5 (toolchain) in this roadmap or separately? | Separately |
