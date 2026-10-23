# Powerline Cleanup & Optimization Roadmap

> **Status:** Phases 0–4 done. Phase 5 (toolchain) is optional and not started
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
- Using the accessibility service for anything beyond Alt+Space (AGENTS.md)
- UI redesign

---

## 2. Rules for this work

- **Measure before and after.** Every optimization item records its number in §6 before it is merged. If an optimization does not move the number, revert it.
- **One item per commit**, each with `testDebugUnitTest` passing. Items that touch the overlay, key handling or `AppIndex` also run `scripts/device-tests.sh`.
- **No behavior changes.** If a cleanup would change behavior (ordering, timing, what is shown), it is out of scope or needs its own decision recorded in §7.
- **New dependencies need a stated reason** (AGENTS.md). Removing dependencies needs none.
- **No compatibility code for earlier builds.** Powerline is in closed testing and not published, so stored data, pref keys and component names may change freely. Testers may have to re-enable the accessibility service or clear app data after an update; note it in the release notes for testers.

---

## 3. Findings (from a read of the V2 code)

### 3.1 Dead code and leftovers

| Where | What | Action |
|---|---|---|
| `data/UserDataRepository.kt` `StoredHistoryItem` | Reads V1 history entries (`id` / `type == "APP"`). V1 was `com.quartz.powerline`, so its data never exists in this app's storage (the file itself says so). | ✅ Removed; `HistoryItem` is read directly |
| `ui/viewmodel/SearchViewModel.kt` `refreshRecentApps` | Fallback to "the package's first activity for V1 entries without a class" | ✅ Removed; `className` is non-null in `AppInfo`, `LaunchApp` and `HistoryItem`, and launches always target the exact activity |
| `data/UserDataRepository.kt` `removeLegacyKeys` | Reads every pref key each time a repository is created, to remove keys from pre-release V2 builds | ✅ Removed (closed testing: no compatibility with earlier builds) |
| `search/SearchRequest.kt` `rawQuery` | Set by `QueryParser`, never read | Remove |
| `search/SearchUiState.kt` `isLoading` | Set by `SearchViewModel`, never read by the UI | Remove, or use it; decide in §7 |
| `search/SearchProvider.kt` `displayName`, `icon` | Not read through the interface | Remove from the interface (providers may keep a private icon) |
| `data/PowerlineDefaults.kt` `OVERLAY_CLOSE_ON_BACKGROUND_TAP` | Always `true`; a V1 preference turned into a constant | Inline |
| `overlay/SearchOverlay.kt` | `e.printStackTrace()` on add / remove view | `Log.w` with a tag, like the rest of the app |
| `services/StatusBarAccessibilityService` | Named after the V1 status bar trigger | ✅ Renamed to `PowerlineAccessibilityService` (closed testing only, so testers re-enabling the service is acceptable) |
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

- [ ] Merge V2 into `main` (prerequisite; cleanup branches are cut from `main`). Until then the work runs on `v2.1/cleanup-optimization`, cut from `v2/phase6-7-cleanup-release`.
- [x] Record the baseline in §6 with `scripts/measure.sh` (release build signed with the debug key; Alt+Space written to `/dev/input` as root).
- [x] Add a JVM benchmark over 1000 apps: `SearchBenchmarkTest` (whole pipeline with history-based signals). It replaces `PerformanceTest`, which searched an empty index.
- [x] Log `AppIndex` load time and reloads (`AppIndex: Indexed N apps in X ms`).

### Phase 1 — Dead code and dependencies

- [x] Remove the V1 history format (`StoredHistoryItem`).
- [x] Remove the `className == null` fallbacks (recent apps, app launch).
- [x] Remove `removeLegacyKeys` and the history / favorites move out of the settings file.
- [x] Rename `StatusBarAccessibilityService` to `PowerlineAccessibilityService`.
- [x] Remove `SearchRequest.rawQuery`, `SearchProvider.displayName` / `icon`, `OVERLAY_CLOSE_ON_BACKGROUND_TAP`, `isLoading` (§7 #1), and the empty `SearchViewModelTest`.
- [x] Replace `printStackTrace` with `Log`.
- [x] Remove `mockito-*`, `core-testing`, `ui-tooling-preview`, `useSupportLibrary`, Jetifier.
- [x] Rename `CommandLauncherTheme` / `Theme.CommandLauncher` to `PowerlineTheme` / `Theme.Powerline`.
- [x] Fix `OverlayE2ETest.shiftTabSelectsEngineChip`, which had failed since the chips were sorted by name (`f4ae978`).

**Done when:** build and all tests pass; no change visible on the device. ✅ Unit tests 105/105, device tests 24/24.

### Phase 2 — Simplification

- [x] One source of truth for active features: `SearchSettingsRepository.activeEngines` / `activeShareTargets` (empty while Web search / Share is off) feed the parser, `AppIndex` and the chips; `ProviderRegistry` skips switched-off providers (`ResultSection.builtIn`). The per-provider `enabled` lambdas are gone.
- [x] Build alias chip IDs in one place (`AliasChip.of`).
- [x] Remove `SearchMode`; `SearchRequest.isAlias` derives it from `engine` / `shareTarget`.
- [x] One process-wide owner per prefs file (`SearchSettingsRepository.get`, `UserDataRepository.get`) and one JSON codec (`StoredJson`). The two repositories stay separate classes: they back different files (backed up / not backed up).
- [x] Replace Gson with `org.json`; delete the keep rules and `proguard-rules.pro`.
- [x] Move test-only constructor defaults into test helpers (`AppIndex`, `QueryParser`, `ProviderRegistry`).
- [x] Split `SearchSettings.kt` (`SwitchSettings`, `WebSearchSettings`, `ShareSettings`, `SettingsRows`); move standby keyboard navigation into `StandbySelection`, tested on the JVM.
- [x] Replace `material-icons-extended` with `PowerlineIcons` (Apps, Calculate, Public, Shortcut; path data taken from the library). The other icons come from material-icons-core, which material3 already includes.
- [x] Fix lint errors and turn `abortOnError` back on. Lint found a real crash: `layoutInDisplayCutoutMode` was set on Android 8.x, where the field does not exist. Left as warnings: dependency updates (Phase 5) and `mipmap-anydpi-v26` (AGP 8.2's aapt2 does not find the icon without the qualifier).

**Done when:** largest file < 350 lines; Built-in switches still behave exactly as before (existing provider and E2E tests pass); APK size recorded. ✅ Largest file 258 lines (`StatusScreen.kt`); unit tests 117/117, device tests 24/24.

### Phase 3 — Search and index performance

- [x] Precompute word lists and initials once per key (`MatchKey`, built in `IndexedApp.of`). Benchmark ~655 → ~277 µs per query.
- [x] Keep history and favorites in memory (`UserDataRepository` reads prefs once, then keeps in step with writes). ~277 → ~254 µs.
- [x] Reload `AppIndex` once package events settle (500 ms after the last one). 10 events → 1 reload. A full reload takes ~20 ms on a warm process, so per-package updates were not worth their code.
- [x] Load apps before the chip icons, and load each engine app's icon once per reload (most engines open in the browser). Chip icons still reload with package events: the default browser or a link-handling app may be what changed.
- [ ] ~~Try `LauncherApps.getActivityList`~~ Not done: the launcher query takes ≤ 1 ms; labels and icons (~50 ms together, cold) cost the same either way.
- [x] Checked the pipeline overhead: the four providers alone take ~100 µs per query over 1000 apps, the whole pipeline ~245 µs. The difference is mostly the thread hops for the per-provider timeouts. Kept (§7 #3): 0.25 ms is far below a frame, and the timeouts still guard against a provider that blocks. Building sort keys once per item did not move the number and was reverted.

Finding for Phase 4: on a fresh process, `AppIndex` finishes its IO work in ~50 ms, but resuming on the main thread waits ~130 ms more while the main thread draws the overlay for the first time (Compose code not yet compiled). That is why "AppIndex first load" barely moved, and it is what the Baseline Profile targets.

**Done when:** the Phase 0 benchmark improves; reload count during an update burst drops to about 1. ✅ ~680 → ~245 µs per query; 10 → 1 reloads.

### Phase 4 — Overlay open and rendering

- [x] Create `SearchViewModel` / `AppIndex` when the service connects (`SearchOverlay.prepare`). The index loads in ~20 ms while nothing else runs, and every first open shows the full grid in its first frame (10 / 10), where before the grid filled in ~185 ms after the key press. Idle memory unchanged.
- [ ] ~~Load app icons lazily~~ Not done: with the index loaded at service start, icons are ready before the first open; loading them per tile would make tiles pop in. Memory per app cannot be judged on an emulator with 17 apps; check on a phone with many apps.
- [x] Draw `AppIcon` as a cached `ImageBitmap` instead of `AndroidView(ImageView)`. Open and typing times unchanged; idle memory ~28.0 → ~26.0 MB.
- [x] Keep the overlay's composition between opens: the panel runs on its own `Recomposer` and the same view is re-added on each open. Later opens ~41 → ~16 ms. Focus, IME, reset on reopen and the fade-out pass `OverlayE2ETest`.
- [ ] ~~Add a Baseline Profile~~ Not done, because it cannot be verified here: on the reference emulator (API 37) `profileinstaller` 1.3.0 reports the ART version as unsupported, and the APK's profile (format 010) is rejected as dex metadata. Compose's own profiles already ship in the APK (`assets/dexopt/baseline.prof`). Upper bound measured with the app fully compiled ahead of time (`COMPILED=1 scripts/measure.sh`): first open ~103 → ~75 ms, typing p90 ~4.3 → ~1.8 ms. Revisit with Phase 5 (newer `profileinstaller`) or on a physical device.

**Done when:** first open < 200 ms, later opens < 80 ms on the reference emulator (log from `OverlayE2ETest`); memory while idle not higher than baseline. ✅ First open ~99 ms with the grid filled, later opens ~18 ms, idle memory ~26.0 MB (baseline ~28.7 MB).

### Phase 5 — Toolchain (optional)

Only if Phases 1–4 are done. This is the largest change, and it is where the V2 roadmap drew the line.

- [ ] Java / JVM target 1.8 → 17.
- [ ] Update `profileinstaller` (through Compose) and add the app's own Baseline Profile (see Phase 4).
- [ ] AGP 8.2 → current, Kotlin 1.9 → 2.x with the Compose compiler Gradle plugin (strong skipping cuts recomposition), Compose BOM update, `compileSdk` / `targetSdk` update with the behavior changes reviewed.

---

## 5. Risks

| Risk | Mitigation |
|---|---|
| Storage refactor resets testers' history or settings | Acceptable during closed testing; tell testers in the release notes |
| Keeping the overlay view between opens breaks focus / IME / ASCII input | `OverlayE2ETest` plus a manual check with Gboard and Google Japanese Input |
| Prewarming in `onServiceConnected` raises idle memory | Measure idle memory before / after; drop the prewarm if the cost is large |
| Lazy icons make tiles pop in while scrolling | Load the first screen of icons with the index; lazy only below the fold |

---

## 6. Measurements

Reference device: Android Desktop emulator (`sdk_gdesktop_x86_64`, Android 17, 2880×1800, userdebug), 17 launcher apps. Release build. Medians of 10 runs (`scripts/measure.sh`); the search benchmark is `SearchBenchmarkTest` on the JVM, run alone.

| Metric | Baseline | After P1 | After P2 | After P3 | After P4 |
|---|---|---|---|---|---|
| Release APK size | 1,458 KB | 1,457 KB | 1,429 KB | 1,429 KB | 1,421 KB |
| Main Kotlin lines (`app/src/main`) | 4,095 | 4,078 | 4,241 | 4,286 | 4,302 |
| `MainActivity` cold start | 252 ms | 219 ms | 218 ms | 217 ms | 209 ms |
| Overlay first open (key → first frame) | 74 ms | 86 ms | 68 ms | 84 ms | 99 ms |
| `AppIndex` first load (apps appear in the grid) | 216 ms | 189 ms | 198 ms | 183 ms | 21 ms (at service start) |
| Overlay later opens | 47 ms | 41 ms | 40 ms | 49 ms | 18 ms |
| Search, 1000 apps (µs / query, JVM) | ~680 | ~660 | ~655 | ~245 | ~245 |
| `AppIndex` reloads, 10 package events | 10 | 10 | 9 | 1 | 1 |
| Idle memory (PSS, overlay closed) | 28,715 KB | 28,145 KB | 28,094 KB | 28,071 KB | 25,988 KB |
| First opens with the grid filled in the first frame | 0 / 10 | | | | 10 / 10 |
| Typing UI-thread p50 / p90 per frame | | | | | 0.5 / 4.3 ms |

Timings on the emulator vary by about ±15% between runs; Phases 1 and 2 changed no code on these paths, so their differences are noise. Typing frames were added to `scripts/measure.sh` in Phase 4. Main Kotlin lines went up in Phase 2: the split settings files each carry their own imports, and `StoredJson`, `StandbySelection` and `PowerlineIcons` are new.

The open-time targets in §1 (first < 200 ms, later < 80 ms) were met from the start. What users saw on a first open was the grid filling in once `AppIndex` had loaded; Phase 4 removed that wait.

---|---|---|---|---|---|
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
| 1 | ~~`isLoading`: remove, or show a subtle loading state?~~ | **Removed** in Phase 1 |
| 2 | ~~Keep the `className == null` fallback for history entries?~~ | **Decided 2026-10-23:** no. Powerline is in closed testing, so nothing is kept for compatibility with earlier builds. |
| 3 | ~~Simplify `SearchPipeline` timeouts after Phase 3 timings?~~ | **Kept** (Phase 3): the overhead is ~0.15 ms per query |
| 4 | ~~Android Auto Backup included launch history and favorites. Keep, or exclude?~~ | **Decided 2026-10-23:** back up only the search settings (Built-in switches, Web search entries, Share entries, Advanced). History and favorites moved to `powerline_user_data`, which is not backed up. Done. |
| 5 | Do Phase 5 (toolchain) in this roadmap or separately? | Separately |
