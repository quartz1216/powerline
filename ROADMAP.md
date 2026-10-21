# Powerline V2 Roadmap

> **Status:** Draft  
> **Baseline:** `main` @ `1c739ae`  
> **Target:** V2.0（内部ベータ → 公開）  
> **Last updated:** 2026-10-21

---

## 1. V2 のコンセプト

Powerline を「ホーム画面ランチャー」から、**Alt+Space でどこからでも呼び出すキーボードファーストの検索レイヤー**へ作り替える。

```
アプリ使用中
   │
   ├─ Alt+Space ──▶ Overlay（検索・計算・Web・共有）
   │                    │
   │                    └─ 実行 ──▶ 元のアプリへ戻る
   │
   └─ アプリアイコン ──▶ MainActivity（状態 / 権限 / About）
```

### Before / After

| 項目 | V1（現在） | V2 |
|---|---|---|
| 主役の画面 | ホーム画面（MainActivity） | オーバーレイ検索（OverlayActivity） |
| トリガー | 画面下の透明オーバーレイ、ステータスバー、QS、アシスタント、Alt+Space | **Alt+Space のみ**（QS・ASSIST・画面下トリガーは削除） |
| MainActivity | HOME + LAUNCHER。検索画面を表示 | **状態 / 権限 / About の案内画面**。LAUNCHER のみ |
| SettingsActivity | 設定専用 Activity | **廃止**（MainActivity は状態 / 権限 / About のみ） |
| 設定 UI | General / Overlay / Appearance / Additional / Powerline Functions | **設定を持たない**。既存項目は全削除 |
| 設定の保存 | SharedPreferences に多数の設定を保存 | **保持しない**。固定デフォルトで動作 |
| テーマ | Theme Selection / Material You トグル | **Material You 固定**（設定なし） |
| 課金 | Powerline Advanced（`isAdvanced`） | **完全撤去** |
| 広告 | AdMob / UMP 同意 | **撤去（確定）** |
| バックアップ | JSON export / import | **削除**。JSON は出力も入力もしない |
| ユーザーへの質問 | 移行・バックアップ・広告同意の確認 | **システム権限ダイアログ以外は質問しない** |
| テスト | 自動テストを追加 | **人間が手動で実施** |
| 検索 | プラグインを並列実行して連結 | **正規化・ランキング・重複排除・キャンセル**を行うパイプライン |
| 機能 | プラグインごとに実装が分散 | **全削除して SearchProvider + Action で再構築** |

---

## 2. ゴールと非ゴール

### ゴール

1. **G1: トリガーの統一** — 常時トリガーを Alt+Space に一本化し、画面下・ステータスバー・QS・アシスタント経由の起動を撤去する。
2. **G2: 画面の役割反転** — `MainActivity` を状態 / 権限 / About の案内画面にし、`OverlayActivity` を唯一の検索サーフェスにする。設定画面は持たない。
3. **G3: マネタイズ層の撤去** — `isAdvanced`、`PurchaseActivity`、デバッグ用の Advanced トグル、AdMob/UMP を削除する。
4. **G4: 検索コアの再構築** — クエリ解析、並列実行、タイムアウト、ランキング、重複排除、インクリメンタル表示を実装する。
5. **G5: 機能の再構築** — Apps / Recents / Calculator / Web / Share / System を新しい Provider 契約で作り直す。
6. **G6: 設定・バックアップを持たない** — 設定は永続化せず、固定デフォルトで動作する。JSON バックアップ / 復元 / 移行を削除する。
7. **G7: 品質とプライバシー** — キーボード操作、IME、アクセシビリティ、OEM 対策、アクセシビリティサービスとしての説明責任を整える。テストは人間が実施する。
8. **G8: Don't Ask User** — システム権限ダイアログ以外に、ユーザーへ設定・移行・バックアップ・同意を質問しない。

### 非ゴール（V2 ではやらない）

- iOS / Web 版
- クラウド同期、アカウント、複数端末同期
- 公開プラグイン SDK / サードパーティ拡張
- 課金・サブスクリプション・広告の再導入
- 設定の永続化・同期・バックアップ JSON
- ユーザーへの移行 / バックアップ / 同意の確認ダイアログ
- テーマ選択・アクセントカラー設定（Material You 固定）
- タッチ専用端末向けの本格最適化（V2.1 以降で検討）
- AGP / Kotlin / Compose の大規模バージョンアップ（別タスク）
- 単位変換・辞書・AI 検索などの大型新機能

---

## 3. 現状（As-Is）

### 3.1 主要コンポーネント

| ファイル | 現在の役割 | V2 での扱い |
|---|---|---|
| `MainActivity.kt` | HOME + LAUNCHER。`LauncherScreen` を表示 | **状態 / 権限 / About に変更**。HOME を外す |
| `SettingsActivity.kt` | 設定画面のホスト | **削除**（MainActivity は状態 / 権限 / About のみ） |
| `OverlayActivity.kt` | 透明オーバーレイ。ASSIST / 各種トリガーから起動 | **唯一の検索画面**。Alt+Space からのみ起動 |
| `PurchaseActivity.kt` | Advanced のプレースホルダー | **削除** |
| `WelcomeActivity.kt` | 6 ページのオンボーディング（ホーム設定・下部トリガー調整・広告） | **権限が無効なときだけ表示する最小構成に刷新** |
| `StatusBarAccessibilityService.kt` | 下部トリガーの透明 View、キーボード監視、Alt+Space | **キーイベントのみ**に縮小 |
| `QuickSettingsService.kt` | QS タイルから Overlay 起動 | **削除** |
| `PowerlineAssistantService.kt` / `PowerlineAssistantSessionService.kt` | 音声アシスタントのスタブ | **削除** |
| `MainViewModel.kt` | 検索状態 + 設定ナビゲーション + 画面状態 | **`SearchViewModel` に整理** |
| `LauncherScreen.kt` | 検索 UI（Main / Overlay 共用） | **`SearchScreen` に改名**。Overlay 専用 |
| `SettingsScreen.kt` | 設定本体 + 下部トリガー設定 + Debug | **全項目を削除し、状態 / 権限 / About 画面に置き換え** |
| `AppearanceSettingsScreen.kt` | テーマ・アクセント・オーバーレイ設定 | **削除**（Material You 固定） |
| `SettingsRepository.kt` | SharedPreferences の巨大な集約 | **削除**（設定を保持しない） |
| `Plugin.kt` / `PluginManager.kt` | 検索・実行・Compose 設定を 1 インターフェースに混在 | **全削除し `SearchProvider` + `Action` で再構築** |
| `plugins/impl/*` | 各機能プラグイン | **全削除し Provider として新規実装** |
| `ActionExecutor.kt` | `pluginId` から実行先を逆引き | **`SearchAction` を直接実行** |
| `AndroidManifest.xml` | HOME / ASSIST / 広告 / アシスタント / QS | 役割を整理して最小化 |
| `app/build.gradle.kts` | AdMob / UMP 依存を含む | 広告依存を削除 |

### 3.2 現状の課題

- **トリガーが多重**: 下部透明 View、ステータスバー、QS、ASSIST、Alt+Space が併存し、設定・オンボーディング・バックアップにまでトリガー設定が漏れている。
- **画面の責務が逆**: 検索の主役がホーム画面で、設定が別 Activity。V2 の「Alt+Space で呼ぶ」体験と一致していない。
- **機能ゲートが default OFF**: `isAdvanced` は既定 `false` のため、Share、隠しアプリ管理、検索エンジン編集などが初期状態で使えない。課金実装もない。
- **検索結果が単純連結**: `SearchEngine` はプラグインを並列実行するが、プラグイン順に `flatten()` するだけで、横断ランキング・重複排除・部分表示がない。
- **プラグイン契約が重い**: `Plugin` が検索・実行・Compose 設定・アイコンを同時に持つため、検索コアだけをテストしづらい。
- **履歴が結果型依存**: `ResultType` と `pluginId` の逆引きに依存し、Recents が他プラグインの代行をする構造になっている。
- **設定項目が過剰**: General に Advanced / Overlay / Appearance / Additional、その下に Powerline Functions が並び、V2 の Alt+Space 体験に対して設定が多すぎる。V2 では設定自体を持たない。
- **設定が永続化される**: SharedPreferences にテーマ・トリガー・Overlay・Launcher・Provider 設定が保存され、移行とバックアップ JSON まで必要になっている。V2 では設定を保持しない。
- **ユーザーへの質問が多い**: オンボーディング、ホーム設定、下部トリガー調整、広告同意、バックアップ確認など、ユーザーに判断を求める画面が多い。
- **オンボーディングが旧仕様**: ホーム設定、下部トリガー調整、広告同意が前提。Alt+Space の説明がない。
- **バックアップが旧設定を含む**: `statusBar*`、`bottomTrigger*`、`hideTriggersOnKeyboard` など、V2 で消えるキーを輸出している。V2 では JSON を削除する。

---

## 4. ターゲット（To-Be）

### 4.1 エントリーポイント

| 入口 | V2 の扱い |
|---|---|
| **Alt+Space** | 唯一のトリガー。Overlay を開く / 閉じる |
| アプリアイコン | `MainActivity`（状態 / 権限 / About）を開く |
| QS タイル | **削除** |
| ASSIST / 音声アシスタント | 削除 |
| 画面下トリガー / ステータスバー | 削除 |
| 通知 | 使用しない |

### 4.2 Activity / Service 構成

| コンポーネント | V2 の役割 | Manifest |
|---|---|---|
| `MainActivity` | 状態 / 権限 / About。設定は持たない | `LAUNCHER` のみ。`exported=true`、`singleTask` |
| `OverlayActivity` | 検索サーフェス。`SearchViewModel` を使用 | `exported=false`、`singleInstance`、`excludeFromRecents` |
| `WelcomeActivity` | 権限が無効なときだけ表示する権限案内 | `exported=false` |
| `SettingsActivity` | 廃止 | エントリ削除 |
| `PurchaseActivity` | 廃止 | エントリ削除 |
| `StatusBarAccessibilityService` | Alt+Space のキーフィルタのみ | 継続。`canRetrieveWindowContent=false` |
| `QuickSettingsService` | 廃止 | エントリ削除 |
| `PowerlineAssistantService` / `PowerlineAssistantSessionService` | 廃止 | エントリ削除 |

### 4.3 検索アーキテクチャ

```
OverlayActivity
  └─ SearchViewModel
       └─ SearchPipeline
            ├─ QueryParser        … alias / prefix / blank query
            ├─ ProviderRegistry   … 有効な Provider の選択
            ├─ 並列実行           … 各 Provider に timeout、collectLatest でキャンセル
            ├─ SearchRanker       … 一致度 + frecency + favorite + provider priority
            ├─ Deduplicator       … app package / URL / action target で重複排除
            └─ SearchUiState      … sections, selectedIndex, isLoading
                 └─ SearchScreen
                      └─ ActionExecutor
                           └─ SearchAction（LaunchApp / OpenUrl / Copy / Share / OpenSettings …）
```

#### 新しい契約（案）

```kotlin
interface SearchProvider {
    val id: String
    val displayName: String
    val priority: Int
    val icon: ImageVector

    fun isEnabled(settings: SettingsSnapshot): Boolean
    suspend fun search(request: SearchRequest, context: Context): List<SearchItem>
    suspend fun execute(item: SearchItem, context: Context): ActionResult
}

data class SearchRequest(
    val rawQuery: String,
    val tokens: List<String>,
    val mode: SearchMode, // BLANK, TEXT, COMMAND
)

data class SearchItem(
    val id: String,
    val providerId: String,
    val title: String,
    val subtitle: String?,
    val icon: Drawable?,
    val section: ResultSection,
    val score: Int,
    val action: SearchAction,
)

sealed interface SearchAction {
    data class LaunchApp(val packageName: String, val className: String?) : SearchAction
    data class OpenUrl(val url: String) : SearchAction
    data class CopyText(val text: String) : SearchAction
    data class ShareText(val text: String) : SearchAction
    data object OpenSettings : SearchAction
    data class SystemAction(val id: String) : SearchAction
}
```

> 移行期間中は既存の `Plugin` / `SearchResult` をアダプタで包み、Phase 5 で各 Provider を順次移行してから旧モデルを削除する。

#### パイプライン仕様

- 入力は 30–50ms デバウンス。新しい入力が来たら前の検索を `collectLatest` でキャンセル。
- Provider ごとに 150ms、全体で 300ms のタイムアウト。遅い Provider は結果から外す。
- 空クエリ時は Recents のみ。テキスト入力時は Apps / Calculator / Web / Share / System。
- セクション順: **Apps → Calculator → Web → Share → System**。空クエリ時は **Recents**。
- 1 Provider あたり最大 5 件、全体で最大 20 件。
- ランキング: 完全一致 > 前方一致 > 部分一致 > 曖昧一致。favorite / frecency / alias にボーナス。
- 重複排除: 同じ `packageName`、同じ URL、同じ `SearchAction` は 1 件にまとめる。
- 実行後は履歴に記録し、Overlay を閉じて元のアプリへ戻る。

### 4.4 設定とユーザーへの質問（MainActivity）

**方針: V2 は設定を永続化しない。ユーザーに設定を質問しない。**

#### 削除確定（V1 の項目）

| セクション | 削除する項目 |
|---|---|
| General | Powerline Advanced (Active) |
| General > Overlay Settings | Enable Overlay & Trigger |
| General > Overlay Settings | Trigger |
| General > Overlay Settings | Feature |
| General > Appearance | Theme Selection（デフォルト = Material You） |
| General > Appearance | Use Material You Colors |
| General > Appearance | Overlay |
| General > Appearance | Launcher |
| General | Additional Settings |
| Powerline Functions | すべて（機能ごと作り直すため） |

- `Overlay Settings` / `Appearance Settings` は中身が空になるため、**サブページ自体を削除**する。
- `Powerline Functions` の一覧・各プラグイン設定画面も**すべて削除**する。
- テーマは **Material You 固定**。テーマ選択・アクセントカラー・Material You トグルは持たない。
- オーバーレイの dim / blur / 背景タップで閉じる / ヘッダーテキストは**設定項目にしない**（固定値または内部定数）。
- `Additional Settings`（バックアップ / システム / Debug / リセット）は削除する。
- Powerline Advanced、Debug、広告、プライバシー同意（UMP）は表示しない。
- SharedPreferences / DataStore に設定を保存しない。設定変更を前提とした UI も作らない。

#### MainActivity の表示内容

```
MainActivity（状態 / 権限 / About）
├─ Alt+Space の状態（アクセシビリティ権限）
├─ 権限設定を開く
├─ 使い方（Alt+Space）
└─ About（version / privacy）
```

- Provider の ON/OFF、履歴設定、バックアップ、テーマ、Overlay、Launcher の設定は持たない。
- システムの権限ダイアログ以外に、ユーザーへ設定・移行・バックアップ・同意を質問しない。
- 旧 General / Overlay Settings / Appearance / Additional Settings / Powerline Functions は**復活させない**。

### 4.5 設定・データ・バックアップ

- **設定は保持しない。** `SettingsRepository` の設定保存、SharedPreferences / DataStore の設定キーを削除する。
- **旧設定は読み込まない。** V1 から設定を移行しない。移行確認も出さない。
- **JSON は削除する。** バックアップ / 復元 / import / export、`AppBackupData`、`BackupMigrator`、`BACKUP_SPEC.md` を削除する。
- **検索履歴 / favorite は設定ではないため保持する。** ただし設定 UI は持たず、JSON バックアップには含めない。alias / hidden apps / カスタム検索エンジンは設定として扱い、保持しない。
- 固定デフォルト:
  - テーマ: **Material You**（Android 12+ は dynamic color、非対応端末はブランドカラー）
  - Overlay: dim `0.5`、blur `16`、背景タップで閉じる `true`、ヘッダーテキストなし
  - 検索: すべての Provider を有効、履歴 ON、最大 5 件
  - 検索エンジン: 固定のデフォルトのみ（ユーザー編集なし）
- V1 の設定キー（`is_advanced`, `debug_mode`, `status_bar*`, `bottom_trigger*`, `hide_triggers_on_keyboard`, `material_you_enabled`, `app_theme`, `custom_accent_color`, `overlay_*`, `home_screen_text`, `onboarding_completed`）は**起動時に削除**する。移行はしない。
- バックアップが必要という理由で設定画面を復活させない。

---

## 5. フェーズ計画

### Phase 0 — ベースラインと安全網

**目的:** V2 の変更を安全に進めるため、現状を固定して検証手順を用意する。

**タスク**

- [ ] `main` @ `1c739ae` から作業ブランチ `v2` を作成する。
- [ ] `v1-final` タグまたは `backup/v1-2026-10-21` ブランチを作成する。
- [ ] `./gradlew :app:assembleDebug` が通ることを確認する（テストは人間が実施）。
- [ ] 現在の APK サイズ、起動時間、検索レイテンシを記録する。
- [ ] `rg -n "isAdvanced|PurchaseActivity|AdMob|MobileAds|bottom_trigger|status_bar|SettingsActivity|QuickSettingsService|ASSIST|AppBackupData|BACKUP_SPEC"` で撤去対象を一覧化する。
- [ ] 未決事項（第 9 章）を決定し、この文書に追記する。

**完了条件**

- `v2` ブランチが作成され、ベースラインのビルドが成功している。
- 自動テストの追加・移行は行わない。受け入れテストは人間が手動で実施する。

**目安:** 0.5 日

---

### Phase 1 — トリガーを Alt+Space に統一

**目的:** 常時トリガーを Alt+Space のみにし、下部トリガー用の透明オーバーレイ、QS、関連設定を撤去する。

**タスク**

- [x] `StatusBarAccessibilityService.kt`
  - [x] `leftOverlayView` / `rightOverlayView` / `isOverlaysAdded` / `isKeyboardVisible` を削除。
  - [x] `createOverlays()` / `removeOverlays()` / `updateOverlayLayout()` / `updateOverlayState()` / `setupTouchListener()` / `getTriggerHeight()` / `getNavigationBarHeight()` / `updateExclusionRects()` を削除。
  - [x] `onSharedPreferenceChangeListener` と `onAccessibilityEvent` のキーボード監視を削除。
  - [x] `onKeyEvent()` の Alt+Space 処理を残し、`ACTION_DOWN` の連打をデバウンスする。
  - [x] `Intent` に明示的な `ACTION_TOGGLE_SEARCH` を付与する（文字列定数を `OverlayActivity` と共有）。
  - [x] 不要になった import（`Color`, `PixelFormat`, `Rect`, `Gravity`, `MotionEvent`, `View`, `ViewConfiguration`, `WindowManager`, `FrameLayout`, `Handler`, `Looper`, `Toast` など）を削除。
- [x] `OverlayActivity.kt`
  - [x] `ACTION_TOGGLE_SEARCH` を `onCreate` / `onNewIntent` で処理する。
  - [x] 表示中なら `finish()`、非表示なら開いてフォーカスを要求する。
  - [x] 開いたときにクエリをクリアする（固定動作。設定項目は作らない）。
  - [x] `onNewIntent` で `setIntent(intent)` を忘れない。
- [x] `accessibility_service_config.xml`
  - [x] `accessibilityEventTypes` を最小化（`typeWindowStateChanged` のみ、またはイベント不要なら最小構成）。
  - [x] `accessibilityFlags` から `flagRetrieveInteractiveWindows` を外し、`flagRequestFilterKeyEvents` のみ残す。
  - [x] `canRetrieveWindowContent="false"` に変更。
- [x] `strings.xml`
  - [x] `accessibility_service_description` を「Alt+Space を検出して Powerline を開く。画面内容は読み取らない」に更新。
- [x] `SettingsScreen.kt`
  - [x] `OverlaySettingsSubpage` を削除する（中身ごと削除）。
  - [x] General の「Overlay Settings」エントリを削除する。
  - [x] 下部トリガーの ON/OFF、Center Gap、Trigger Height、Keyboard Hide、dim / blur、背景タップ設定の UI を削除する。
  - [x] Debug Mode の自動 ON/OFF（`DisposableEffect`）を削除する。
- [x] `QuickSettingsService.kt` を削除する。
  - [x] `AndroidManifest.xml` の QS `<service>` を削除する。
  - [x] QS タイルからの `OverlayActivity` 起動コードを削除する。
- [x] `WelcomeActivity.kt`
  - [x] 「Set as Home」「Adjust Triggers」「corner triggers」を削除。
  - [x] Alt+Space の説明とアクセシビリティ有効化に置き換える（詳細は Phase 6）。
- [x] `SettingsRepository.kt`
  - [x] 下部トリガー / ステータスバー / `debug_mode` の getter/setter を削除。
  - [x] 設定保存そのものを廃止する準備として、残りの設定キーの利用箇所を一覧化する。

> **Phase 1 時点で残る SharedPreferences キー**（Phase 2 で整理）  
> 設定（削除対象）: `is_advanced`, `material_you_enabled`, `app_theme`, `custom_accent_color`, `overlay_close_on_background_tap`, `overlay_dim_amount`, `overlay_blur_radius`, `home_screen_text`, `onboarding_completed`, `max_recent_items`, `history_enabled`, `calculator_enabled`, `share_enabled`, `share_alias`, `enable_weight_search`, `enable_package_search`, `aliases`, `hidden_apps`, `custom_search_engines`  
> データ（保持対象）: `history`, `favorite_apps`, `app_weights`  
> 備考: Overlay の dim / blur UI は `AppearanceSettingsScreen` に暫定で残っている（画面ごと Phase 2 で削除）。

**完了条件**

- サービス起動時に画面下の透明 View が追加されない（`dumpsys window` で確認）。
- Alt+Space で Overlay が開き、もう一度押すと閉じる。
- QS タイル / ステータスバー / 画面下トリガーから Overlay を開けない。
- 設定・オンボーディング・バックアップに下部トリガー / ステータスバー / QS の項目がない。
- `./gradlew :app:assembleDebug` が成功する（テストは人間が実施）。

**目安:** 1–2 日

---

### Phase 2 — Powerline Advanced / 課金 / 広告 / 設定保存の撤去

**目的:** 機能ゲートとマネタイズ層を削除し、テーマを Material You 固定にする。設定の永続化と外観設定 UI も削除する。

**タスク**

- [x] `SettingsRepository.kt`
  - [x] 設定保存を全廃する。参照を固定定数に置き換えてからクラスを削除する。
  - [x] `getIsAdvanced()` / `setIsAdvanced()` を削除。
  - [x] `getDebugMode()` / `setDebugMode()` を削除。
  - [x] `getMaterialYouEnabled()` / `setMaterialYouEnabled()` を削除。
  - [x] `getAppTheme()` / `setAppTheme()` を削除（Material You 固定）。
  - [x] `getCustomAccentColor()` / `setCustomAccentColor()` を削除。
  - [x] `getOverlayCloseOnBackgroundTap()` / `getOverlayDimAmount()` / `getOverlayBlurRadius()` を削除（固定値）。
  - [x] `getHomeScreenText()` / `setHomeScreenText()` を削除（Launcher 設定の削除）。
  - [x] その他の設定キーの getter/setter を削除し、固定値に置き換える。
- [x] `SettingsViewModel.kt`
  - [x] 設定状態（`_isAdvanced` / `_materialYouEnabled` / `_appTheme` / `_customAccentColor` / Provider / 履歴）を全削除。
  - [x] 状態画面に必要な権限状態だけを残すか、ViewModel 自体を削除する。
  - [x] `setDebugMode()` と `navigateBack()` 内の debug 処理を削除。
- [x] `SettingsScreen.kt`
  - [x] 「Powerline Advanced」リスト項目を削除。
  - [x] 「Appearance Settings」リスト項目を削除。
  - [x] 「Additional Settings」リスト項目を削除。
  - [x] 「Powerline Functions」セクションとプラグイン一覧を削除。
  - [x] 「Debug → Toggle Advanced Mode」を削除。
  - [x] 「Support Powerline」「広告」関連の文言を削除。
  - [x] 設定項目を全削除し、Phase 3 で状態 / 権限 / About 画面に置き換える。
- [x] `AppLauncherPlugin.kt`
  - [x] `isAdvanced` による Hidden Apps / Alias 追加の制限を削除（機能自体は Phase 5 で削除）。
- [x] `WebSearchPlugin.kt`
  - [x] `!isAdvanced` 時の Google 限定フィルタを削除。
  - [x] 旧プラグイン設定画面の `displayEngines` / `canEdit` / FAB の Advanced 条件を削除（画面自体は Phase 5 で削除）。
- [x] `SharePlugin.kt`
  - [x] `if (!settingsRepository.getIsAdvanced()) return emptyList()` を削除。
  - [x] 旧プラグイン設定画面の「Powerline Advanced で利用可能」表示と `enabled = isAdvanced` を削除（画面自体は Phase 5 で削除）。
- [x] `AppearanceSettingsScreen.kt` を削除する（テーマ選択・アクセント・Overlay・Launcher 設定ごと削除）。
- [x] `Theme.kt` を Material You 固定に簡素化する。
  - [x] `appTheme` / `customAccentColor` / `dynamicColor` 引数を削除。
  - [x] Android 12+ は dynamic color、非対応端末はブランドカラーにフォールバック。
  - [x] `setSystemBars` など必要な引数だけ残す。
- [x] `MainActivity.kt` / `SettingsActivity.kt` / `OverlayActivity.kt` / `SearchScreen.kt`
  - [x] `CommandLauncherTheme` の `appTheme` / `customAccentColor` / `dynamicColor` 引数を削除。
  - [x] `appTheme` / `customAccentColor` / `materialYouEnabled` の `collectAsState` を削除。
- [x] `PurchaseActivity.kt` を削除。
- [x] `AdMobBanner.kt` / `GoogleMobileAdsConsentManager.kt` を削除。
- [x] `MainActivity.kt` / `SettingsActivity.kt` / `WelcomeActivity.kt`
  - [x] `GoogleMobileAdsConsentManager` の初期化・同意取得を削除。
- [x] `app/build.gradle.kts`
  - [x] `play-services-ads` と `user-messaging-platform` を削除。
- [x] `AndroidManifest.xml`
  - [x] AdMob の `APPLICATION_ID` meta-data を削除。
  - [x] `PurchaseActivity` の `<activity>` を削除。
- [x] `WelcomeActivity.kt`
  - [x] `SupportPage`（アップグレード / 広告同意）を削除する。
- [x] `AppBackupData.kt` / `BackupMigrator` / `BACKUP_SPEC.md`
  - [x] バックアップ JSON のモデル・移行・仕様書を削除。
  - [x] `SettingsRepository` のバックアップ連携を削除。
  - [x] 旧キーを読み込むマイグレーションは作らない。

> **Phase 2 の実装メモ**  
> - `SettingsRepository` は `UserDataRepository` に改名・縮小し、`history` / `favorite_apps` / `app_weights` / `onboarding_completed` だけを扱う。固定値は `PowerlineDefaults` に集約。  
> - `onboarding_completed` は Phase 6（Welcome 刷新）までの暫定。  
> - プラグイン設定画面（`Plugin.SettingsScreen` / `PluginSettingsScreen`）は Phase 5 を待たずに前倒しで削除。hidden apps / ユーザー alias / 検索エンジン編集は廃止。  
> - 削除 API を参照していた既存ユニットテスト 8 件を削除（テストは移行しない方針）。  
> - 旧 SharedPreferences キーはディスクに残る（起動時削除は Phase 6）。`SettingsActivity` は Permissions のみの暫定画面（Phase 3 で削除）。

**完了条件**

- `rg -n "isAdvanced|PurchaseActivity|AdMob|MobileAds|consent|materialYouEnabled|appTheme|customAccentColor|debug_mode|AppBackupData|BackupMigrator|BACKUP_SPEC" app/src` が空。
- 課金・広告・Advanced の UI が一切ない。
- 旧設定項目（General / Overlay Settings / Appearance / Additional Settings / Powerline Functions）が存在しない。
- 設定用の SharedPreferences / DataStore キーを保存していない。
- JSON バックアップのコード・ファイル・UI が存在しない。
- テーマは Material You 固定で、テーマ選択 UI がない。
- Share / アプリ検索 / 電卓 / Web 検索が Advanced なしで使える。
- `./gradlew :app:assembleDebug` が成功する（テストは人間が実施）。

**目安:** 1–2 日

---

### Phase 3 — MainActivity = 状態 / 権限、Overlay = 検索

**目的:** 画面の役割を反転し、アプリアイコンからは状態 / 権限 / About、Alt+Space からは検索を開く。設定画面は持たない。

**タスク**

- [x] `MainActivity.kt` を**状態 / 権限 / About 画面**に置き換える。
  - [x] Alt+Space の状態（アクセシビリティ権限の有効/無効）を表示。
  - [x] 権限設定を開くボタン。
  - [x] 使い方（Alt+Space）。
  - [x] About（version / privacy）。
  - [x] `SettingsViewModel` / `SettingsScreen` / `SettingsNav` への依存を削除。
  - [x] `LauncherScreen` / `MainViewModel` / `ActionExecutor` / `rememberIsWallpaperLight` への依存を削除。
  - [x] 設定を保存する UI は置かない。旧 General / Overlay / Appearance / Additional / Powerline Functions は復活させない。
- [x] `SettingsActivity.kt` / `SettingsViewModel.kt` / `SettingsScreen.kt` を削除する。
  - [x] `OverlayActivity.kt` の設定遷移を削除。
  - [x] `AndroidManifest.xml` の `<activity>` を削除。
- [x] `MainViewModel.kt` を `SearchViewModel.kt` に改名。
  - [x] `Screen` enum、`openSettings()` / `closeSettings()` / `openAppearanceSettings()` / `openPluginSettings()` / `openPermissions()` / `closePermissions()` など、設定ナビゲーション用の不要 API を削除。
  - [x] `currentScreen` / `selectedPluginId` を削除。
  - [x] `OverlayActivity` / `SearchScreen` の参照を更新。
- [x] `LauncherScreen.kt` を `SearchScreen.kt` に改名。
  - [x] 内側の `CommandLauncherTheme` を削除し、`OverlayActivity` のテーマをそのまま使う（現在は `isSystemInDarkTheme()` で上書きしている）。
  - [x] 情報アイコンは `MainActivity`（状態 / 権限 / About）を開く。
- [x] `AndroidManifest.xml`
  - [x] `MainActivity`: `HOME` / `DEFAULT` カテゴリと `Theme.CommandLauncher.Home` を削除。`LAUNCHER` のみ残す。
  - [x] `MainActivity`: `stateNotNeeded` を削除。`launchMode="singleTask"` は維持。
  - [x] `OverlayActivity`: `ASSIST` intent-filter を削除。`exported="false"` に変更。
  - [x] `SettingsActivity` / `PurchaseActivity` の `<activity>` を削除。
  - [x] `PowerlineAssistantService` / `PowerlineAssistantSessionService` の `<service>` を削除。
- [x] `themes.xml`
  - [x] `Theme.CommandLauncher.Home` を削除。
  - [x] `Theme.CommandLauncher.Overlay` は維持。
- [x] `WelcomeActivity.kt`
  - [x] 完了後の遷移先を `MainActivity`（状態 / 権限）にする。
  - [x] 既存ユーザー向けに「Powerline はホームアプリではなくなりました」という案内を状態画面に表示する（保存はしない）。
- [x] 設定の永続化を削除する。
  - [x] `SettingsRepository` / SharedPreferences / DataStore の設定保存を削除。
  - [x] 設定変更の `StateFlow` 連携を削除。
  - [x] JSON バックアップ / 復元 / 移行を削除。

> **Phase 3 の実装メモ**  
> - 状態画面は `ui/StatusScreen.kt`（旧 `PermissionsScreen.kt`）。Alt+Space の状態 / アクセシビリティ設定 / 使い方 / ホームアプリ廃止の案内 / OEM 注意書き / About（version とプライバシー方針の本文）を表示。通知権限とデフォルトホームのチェックは削除。PRIVACY.md へのリンクは Phase 7 で追加する。  
> - 設定の永続化・JSON の削除は Phase 2 で完了済み。`onboarding_completed` による Welcome 振り分けは Phase 6 まで残す。  
> - 使われなくなった `WallpaperTheme.kt` / `NotificationHelper.kt` / 音声アシスタント関連（3 ファイル + `assistant_service.xml`）も削除。  
> - 検索候補「settings」は「Powerline Status」として状態画面を開く。

**完了条件**

- ホームボタン / ホームアプリ選択に Powerline が出ない。
- アプリアイコンから状態 / 権限 / About が開く。
- Alt+Space からのみ Overlay が開く（QS は削除済み）。
- `SettingsActivity` / `SettingsViewModel` / `SettingsScreen` / `SettingsRepository` / `QuickSettingsService` への参照が 0。
- 設定を保存するコード（設定用の SharedPreferences / DataStore / `SettingsRepository`）が存在しない。
- JSON バックアップ / 復元 / 移行のコードが存在しない。
- `./gradlew :app:assembleDebug` が成功する（テストは人間が実施）。

**目安:** 2–3 日

---

### Phase 4 — 検索コアの再構築

**目的:** 検索を「プラグインの連結」から、ランキングとキャンセルを備えたパイプラインに置き換える。

**タスク**

- [ ] 新パッケージ `com.quartz.powerline.search` を作成。
  - [ ] `SearchProvider.kt`
  - [ ] `SearchRequest.kt` / `SearchMode.kt`
  - [ ] `SearchItem.kt` / `ResultSection.kt`
  - [ ] `SearchAction.kt` / `ActionResult.kt`
  - [ ] `SearchPipeline.kt`
  - [ ] `SearchRanker.kt`
  - [ ] `SearchDeduplicator.kt`
  - [ ] `SearchUiState.kt`
  - [ ] `ProviderRegistry.kt`
- [ ] `SearchViewModel`
  - [ ] `MutableStateFlow<String>` のクエリと `StateFlow<SearchUiState>` を公開。
  - [ ] `collectLatest` + `debounce(30–50ms)` で検索をキャンセル可能にする。
  - [ ] `selectedIndex` の上下移動、Enter、Esc、Tab のハンドリングを追加。
  - [ ] 空クエリ時は Recents を返す。
- [ ] `SearchPipeline`
  - [ ] クエリを解析し、alias / prefix / blank を判定。
  - [ ] すべての Provider を並列実行（`async(Dispatchers.Default)`）。
  - [ ] Provider ごとの `withTimeout(150)` と全体 `withTimeout(300)`。
  - [ ] 結果を `SearchRanker` で採点し、`SearchDeduplicator` で重複排除。
  - [ ] セクションごとに最大 5 件、全体 20 件に制限。
- [ ] `SearchRanker`
  - [ ] 完全一致 / 前方一致 / 部分一致 / 曖昧一致のスコアリング。
  - [ ] favorite / frecency / alias / provider priority の加算。
  - [ ] 大文字小文字・全角半角・空白の正規化。
- [ ] `ActionExecutor`
  - [ ] `SearchAction` の `when` で実行する形に変更。
  - [ ] `pluginId` の逆引きを廃止。
  - [ ] 実行後に `HistoryItem` を記録。
- [ ] 旧 `Plugin` / `SearchResult` のアダプタ
  - [ ] 移行期間中は `PluginSearchProviderAdapter` を用意し、既存プラグインを包む。
  - [ ] Phase 5 の完了後に旧モデルとアダプタを削除。
- [ ] テストは人間が手動で実施する。自動テストの追加・移行は行わない。

**完了条件**

- 1–2 文字のアプリ検索で、最初の結果が 100ms 以内（ミドルレンジ端末）。
- 入力中に前の検索がキャンセルされ、古い結果が一瞬表示されない。
- Recents と Apps に同じアプリが二重表示されない。
- すべての Provider が設定なしで有効になっている。
- 人間による手動テストで主要フローが動作する。

**目安:** 4–5 日

---

### Phase 5 — 機能（Provider）の再構築

**目的:** 既存のプラグイン実装と設定画面を**全削除**し、新しい Provider 契約でゼロから作り直して V2 の検索体験を完成させる。

**タスク**

#### Apps

- [ ] `AppIndex` を導入し、起動時にアプリ一覧・ラベル・パッケージ・クラス名をキャッシュ。
- [ ] ラベル / パッケージ / 組み込みエイリアスの検索。
- [ ] favorite / frecency は設定 UI なしで保持する（履歴データとして）。
- [ ] あいまい一致を改善（単語境界、頭文字、大文字小文字）。
- [ ] 長押しメニュー: favorite、アプリ情報、共有。

#### Recents

- [ ] `HistoryItem` を `SearchAction` ベースに移行。
- [ ] アプリ / Web / Calc / Share を統一して重複排除。
- [ ] 時間減衰（例: 7 日でスコア半減）を導入。
- [ ] 最大 5 件、履歴 ON を固定値として実装（設定 UI なし）。履歴は V2 のデータストアに保持する。
- [ ] 空クエリ時に表示。

#### Calculator

- [ ] 四則演算、括弧、パーセント、負数、小数点をサポート。
- [ ] 計算結果のコピー、履歴への記録。
- [ ] エラー時の表示を整理。
- [ ] 単位変換は V2 では扱わない。

#### Web

- [ ] 固定のデフォルト検索エンジンのみ（ユーザー編集なし）。
- [ ] 組み込みエイリアス prefix（例: `yt laneige`）と固定のデフォルトエンジン。
- [ ] URL 直接入力、`https://` 補完。
- [ ] favicon のキャッシュ（失敗時はアイコンにフォールバック）。
- [ ] 検索時はネットワークアクセスせず、実行時のみブラウザを開く。

#### Share

- [ ] クエリ文字列の共有。
- [ ] 共有シート起動、履歴記録。
- [ ] Advanced ゲートなし。共有設定は持たない。

#### System

- [ ] 「settings」で Android の設定を開く。
- [ ] アプリ情報、アクセシビリティ設定、バッテリー最適化設定へのショートカット。
- [ ] 危険なシステム操作は V2 では実装しない。

#### 共通

- [ ] `plugins/impl/*` の既存実装を**全削除**する（コピーせず、Provider として新規実装）。
- [ ] 旧 `Plugin` / `PluginManager` / プラグイン設定画面を**全削除**する。
- [ ] `ResultType` / `SearchResult` を削除する。
- [ ] `ProviderRegistry` を実装する。すべての Provider を常時有効にし、ON/OFF 設定は持たない。
- [ ] Provider ごとの設定画面は作らない。固定デフォルトで動作する。

**完了条件**

- すべての機能が Advanced なしで動作する。
- 検索結果のセクション表示とキーボード操作が破綻しない。
- 旧 `Plugin` / `PluginManager` / `plugins/impl/*` / `SearchResult` への参照が 0。
- 旧「Powerline Functions」のプラグイン一覧・設定画面が存在しない。
- Provider の ON/OFF 設定、カスタム検索エンジン設定、履歴設定が存在しない。
- 人間による手動テストで主要フローが動作する。

**目安:** 4–5 日

---

### Phase 6 — 状態画面・権限・初回起動

**目的:** MainActivity を状態 / 権限 / About として完成させ、設定・バックアップ・移行を持たない状態にする。

**タスク**

- [ ] `MainActivity.kt` / 新しい状態画面
  - [ ] Alt+Space の状態（アクセシビリティ権限の有効/無効）を表示。
  - [ ] 権限設定を開くボタン。
  - [ ] 使い方（Alt+Space）。
  - [ ] About（version / privacy）。
  - [ ] 設定項目、テーマ、Overlay、Launcher、Provider、履歴、バックアップの UI を作らない。
- [ ] `WelcomeActivity.kt` を最小化。
  - [ ] アクセシビリティ権限が有効なら権限案内をスキップして MainActivity へ。
  - [ ] 権限が無効な場合のみ、1. コンセプト、2. 権限有効化 + プライバシー説明を表示。
  - [ ] テーマ選択、ホーム設定、下部トリガー調整、広告同意、バックアップ確認を削除。
- [ ] 設定・バックアップ・移行の残骸を削除。
  - [ ] `SettingsNav` / `SettingsScreen` / `SettingsViewModel` / `SettingsRepository` の残りを削除。
  - [ ] JSON バックアップ / 復元 / 移行 / `BACKUP_SPEC.md` を削除。
  - [ ] 旧設定キーを起動時に削除する。移行しない。ユーザーに確認しない。
  - [ ] アプリ管理下の旧 JSON ファイルを起動時に削除する（ユーザーには通知しない）。
- [ ] 広告 / UMP の残骸を削除。
  - [ ] AdMob / UMP の依存、初期化、同意取得、Banner を削除。

**完了条件**

- MainActivity は状態 / 権限 / About のみで、設定画面を持たない。
- 設定を保存するコード（設定用の SharedPreferences / DataStore / `SettingsRepository`）が存在しない。
- JSON バックアップ / 復元 / 移行のコード・ファイル・UI が存在しない。
- システム権限ダイアログ以外に、ユーザーへ質問する画面がない。
- 広告 / UMP のコードと UI が存在しない。
- アクセシビリティ権限が有効なら、初回起動でも権限案内を挟まない。
- 人間による手動テストで主要フローが動作する。

**目安:** 2–3 日

---

### Phase 7 — 品質・プライバシー・リリース

**目的:** V2 を安定して配布できる状態にする。

**タスク**

- [ ] キーボード UX
  - [ ] 上下キーで選択、Enter で実行、Esc で閉じる、Tab でセクション移動。
  - [ ] 入力開始時に IME を確実に表示（`FocusRequester` + `SoftwareKeyboardController`）。
  - [ ] Overlay 表示アニメーション（フェード / スケール）と dim / blur の調整。
  - [ ] 画面回転・マルチウィンドウ・タブレット幅でのレイアウト確認。
- [ ] アクセシビリティ
  - [ ] すべての Icon に `contentDescription`。
  - [ ] TalkBack で状態画面と検索結果が読めることを確認。
  - [ ] タッチターゲット 48dp 以上。
- [ ] パフォーマンス
  - [ ] コールドスタートから Overlay 表示まで 200ms 以内。
  - [ ] アプリ一覧のロードを初回のみにし、2 回目以降はキャッシュ。
  - [ ] 入力 1 文字あたりの UI 更新を 16ms 以内に保つ。
  - [ ] リリース APK サイズを測定（広告 SDK 削除後の削減量を記録）。
- [ ] 手動テストマトリクス（人間が実施）
  - [ ] Pixel / stock Android（Android 14）
  - [ ] Samsung One UI
  - [ ] Xiaomi HyperOS（バッテリー制限・サービス kill）
  - [ ] Android 8（minSdk 26）
  - [ ] 物理キーボード（US / JIS）
  - [ ] Gboard / Google 日本語入力 / Samsung キーボード
  - [ ] ライト / ダーク / Material You
  - [ ] マルチウィンドウ / 画面回転
  - [ ] 再起動後に設定・JSON が保持されないこと（履歴 / favorite は保持される）
  - [ ] 設定画面 / バックアップ / 移行 / 広告同意の質問が出ないこと
- [ ] ドキュメント
  - [ ] `README.md` に V2 のセットアップ（アクセシビリティ有効化、Alt+Space）を記載。
  - [ ] `PRIVACY.md` に「画面内容を読まない」「キーログを取らない」を明記。
  - [ ] `README.md` / `PRIVACY.md` に「設定・JSON バックアップは保持しない」と明記。
  - [ ] `ROADMAP.md` の完了項目を更新。
- [ ] リリース
  - [ ] `versionCode = 2` / `versionName = "2.0.0"`。
  - [ ] リリースビルド（署名、`isMinifyEnabled` の検討）。
  - [ ] 内部テスト → ベータ → 公開の順で配布。
  - [ ] ストア掲載文を「ホームアプリ」から「Alt+Space 検索レイヤー」に変更。

**完了条件**

- 人間による手動テストマトリクスでクラッシュ / ANR がない。
- Alt+Space が主要端末・主要 IME で動作する。
- 設定・バックアップ・移行・広告同意の質問が一切出ない。
- プライバシー説明とアクセシビリティ説明がストア / アプリ内で一致している。
- 署名済み APK が生成できる。
- `assembleDebug` / リリースビルドが成功する（テストは人間が実施）。

**目安:** 3–5 日

---

## 6. 設定・バックアップの扱い

### 6.1 設定

- **V2 は設定を保持しない。** SharedPreferences / DataStore の設定キーを削除し、固定デフォルトで動作する。
- **旧設定は削除する。** V1 の設定（`is_advanced`, `debug_mode`, `status_bar*`, `bottom_trigger*`, `hide_triggers_on_keyboard`, `material_you_enabled`, `app_theme`, `custom_accent_color`, `overlay_*`, `home_screen_text`, `onboarding_completed`）は起動時に削除する。
- マイグレーション処理は作らない。ユーザーに移行確認も出さない。
- 設定変更を前提とした画面（テーマ、Overlay、Launcher、Provider、履歴、検索エンジン）を作らない。

### 6.2 バックアップ JSON

- JSON の export / import / 復元 / 移行を**削除**する。
- `AppBackupData` / `BackupMigrator` / `BACKUP_SPEC.md` を削除する。
- 旧 JSON は読み込まない。新しい JSON も出力しない。アプリが作成した旧 JSON ファイルは起動時に削除する。
- バックアップを理由に設定画面やユーザーへの確認を復活させない。

### 6.3 既存ユーザー

- V1 の設定は削除する。V1 の履歴・favorite・alias・hidden apps・検索エンジンは引き継がない。V2 以降の履歴 / favorite は V2 のデータストアに保持する。
- Powerline をホームアプリとして使っているユーザーは、更新後にホームアプリを選び直す必要がある。
- 案内は README と状態画面に記載する。移行確認ダイアログは出さない。
- 再起動で設定・JSON が復元されないことを仕様とする。履歴 / favorite は保持する。

---

## 7. テスト計画

### 7.1 テスト方針

- **テストは人間が実施する。** V2 では新しい自動テストを追加しない。
- 既存テストの移行・拡張は行わない。受け入れ判定には使わない。
- 開発中の確認は `./gradlew :app:assembleDebug` と実機・エミュレータでの手動操作で行う。
- 検索パイプライン、ランキング、重複排除、Provider の動作は、下記の手動テストで確認する。

### 7.2 UI / 手動テスト

- Alt+Space で開く / 閉じる（連打、長押し、キーリピート）。
- 物理キーボードで入力 → 矢印 → Enter → アプリ起動。
- Esc / 背景タップ / Back で閉じる。
- IME 表示中に Alt+Space が効くか（Gboard / 日本語入力）。
- 設定画面が存在せず、設定・JSON が再起動後に保持されないこと（履歴 / favorite は保持される）。
- 移行・バックアップ・広告同意の質問が出ないこと。
- 回転 / マルチウィンドウ / タブレット。
- アクセシビリティ権限を OFF にしたときの案内。
- 広告 / UMP の UI が存在しないこと。

### 7.3 パフォーマンス目標

| 項目 | 目標 |
|---|---|
| Overlay 表示 | 200ms 以内 |
| 最初の検索結果 | 100ms 以内（アプリ名 1–2 文字） |
| 入力中の UI 更新 | 16ms / frame |
| アプリ一覧ロード | 初回 500ms 以内、2 回目以降はキャッシュ |
| リリース APK | 15MB 以下（目安） |

### 7.4 ストア / ポリシー

- アクセシビリティサービスは「Alt+Space の検出」にのみ使用し、画面内容の取得・キーログを行わない。
- `canRetrieveWindowContent="false"`、`flagRetrieveInteractiveWindows` を外す。
- プライバシーポリシーと権限案内で明示する。
- 広告 / UMP を使用しないため、広告同意の取得は行わない。
- Google Play のアクセシビリティポリシー上、キー検出目的の利用は審査リスクがある。配布方法（Play / GitHub Releases / F-Droid）は第 9 章で決定する。

---

## 8. リスクと対策

| リスク | 影響 | 対策 |
|---|---|---|
| アクセシビリティサービスが Play ポリシーに抵触 | ストア公開不可 | 目的を最小化し、プライバシーを明記。必要なら GitHub Releases / F-Droid を主軸にする |
| Alt+Space がアプリ / IME に横取りされる | 起動できない | `flagRequestFilterKeyEvents` を維持。主要 IME で検証。QS は削除するためフォールバックなし。README と権限案内で既知の制限を明記 |
| OEM（Xiaomi 等）がサービスを kill | 常時トリガーが死ぬ | バッテリー最適化の除外を案内。状態画面に診断リンクを置く |
| 設定を持たないためカスタマイズできない | ユーザーの好みに合わせられない | V2 は固定デフォルトと割り切る。状態 / 権限 / About のみ表示する |
| ホームアプリではなくなる | 既存ユーザーのホームが空になる | README と状態画面で明示。別ホームアプリの設定手順を案内。確認ダイアログは出さない |
| 設定を引き継がない | V1 のカスタマイズが消える | 仕様として明記する。移行処理・JSON バックアップは作らない |
| 履歴 / favorite の設定 UI がない | 履歴のクリアや最大件数を変更できない | 固定値（最大 5 件）とし、必要なら V2.1 で再検討 |
| 検索コアの作り直しで regression | 主要機能が動かない | Provider 単位で移行し、各 Phase で人間が手動確認する |
| スコープが大きい | リリース遅延 | Phase ごとに完了条件を満たしてから次へ。V2.1 に回す項目を明示 |
| タッチ端末で使えない | ユーザー層が限定される | V2 はキーボード前提と割り切る。QS は削除するためタッチ用の代替導線は持たない。将来のタッチ UI は V2.1 で検討 |

---

## 9. 未決事項

| # | 決定事項 | 推奨 | 期限 |
|---|---|---|---|
| 1 | パッケージ名を維持するか | **維持**（`com.quartz.powerline`） | Phase 0 |
| 2 | minSdk を 26 のままにするか | **維持** | Phase 0 |
| 3 | コマンド prefix を導入するか | **V2 では組み込み alias のみ**。`?` / `=` / `>` は V2.1 | Phase 4 |
| 4 | UI 言語 | **V2 は英語**。日本語は V2.1 | Phase 6 |
| 5 | 配布方法 | まず **GitHub Releases / 内部ベータ**。Play はポリシー確認後 | Phase 7 |

---

## 10. Definition of Done（V2.0）

- [ ] `main` から `v2` ブランチが切られ、`v1-final` が保存されている。
- [ ] 常時トリガーは Alt+Space のみ。QS タイルは存在しない。
- [ ] 画面下トリガー / ステータスバートリガー / 下部透明 View が存在しない。
- [ ] `MainActivity` が状態 / 権限 / About の案内画面で、`LAUNCHER` のみを持つ。
- [ ] `SettingsActivity` / `PurchaseActivity` / `QuickSettingsService` が削除されている。
- [ ] 既存の Settings 項目（Powerline Advanced / Overlay Settings / Appearance / Additional Settings / Powerline Functions）がすべて削除されている。
- [ ] 設定を保存するコード（設定用の SharedPreferences / DataStore / `SettingsRepository`）が存在しない。
- [ ] 設定画面、テーマ選択、アクセントカラー、Overlay / Launcher 設定、Provider ON/OFF、履歴設定が存在しない。
- [ ] テーマは Material You 固定。Overlay の dim / blur / 背景タップ / ヘッダーテキストは固定値。
- [ ] `isAdvanced` / Advanced トグル / 課金 UI が存在しない。
- [ ] AdMob / UMP / 広告 Banner が存在しない。
- [ ] QS / ASSIST / 音声アシスタントのサービス・エントリが削除されている。
- [ ] `OverlayActivity` が Alt+Space からのみ開く。
- [ ] 検索パイプライン（解析・並列・タイムアウト・ランキング・重複排除）が動作する。
- [ ] Apps / Recents / Calculator / Web / Share / System が新 Provider で動作する。
- [ ] JSON バックアップ / 復元 / 移行のコード・ファイル・UI が存在しない。
- [ ] システム権限ダイアログ以外に、ユーザーへ質問する画面がない。
- [ ] アクセシビリティ権限が有効なら、初回起動でも権限案内を挟まない。
- [ ] 再起動後に設定・JSON が保持されない（履歴 / favorite は保持される）。
- [ ] `./gradlew :app:assembleDebug` が成功する（テストは人間が実施）。
- [ ] 人間による手動テストマトリクスでクラッシュ / ANR がない。
- [ ] `README.md` / `PRIVACY.md` が更新されている。`BACKUP_SPEC.md` は削除されている。
- [ ] `versionCode = 2` / `versionName = "2.0.0"` の署名済み APK が生成できる。

---

## 11. マイルストーン

| マイルストーン | 内容 | 目安 |
|---|---|---|
| M0 | ベースライン、`v2` ブランチ、`v1-final` タグ | 0.5 日 |
| M1 | Alt+Space 単一トリガー、下部トリガー / QS 撤去 | 1–2 日 |
| M2 | Advanced / 課金 / 広告 / 設定保存撤去 | 1–2 日 |
| M3 | MainActivity = 状態 / 権限、Overlay = 検索 | 2–3 日 |
| M4 | 検索コア（パイプライン / ランキング） | 4–5 日 |
| M5 | Provider 再実装（Apps / Recents / Calc / Web / Share / System） | 4–5 日 |
| M6 | 状態画面 / 権限 / 初回起動 | 2–3 日 |
| M7 | 人間による QA / プライバシー / リリース | 3–5 日 |
| **合計** | | **約 3–4 週間**（個人開発の目安） |

---

## 12. 付録: ファイル別の変更マップ

| ファイル | V2 の変更 |
|---|---|
| `app/src/main/java/com/quartz/powerline/MainActivity.kt` | 状態 / 権限 / About に変更。HOME を外す |
| `app/src/main/java/com/quartz/powerline/SettingsActivity.kt` | 削除 |
| `app/src/main/java/com/quartz/powerline/OverlayActivity.kt` | 検索サーフェス。Alt+Space 専用、`exported=false` |
| `app/src/main/java/com/quartz/powerline/PurchaseActivity.kt` | 削除 |
| `app/src/main/java/com/quartz/powerline/WelcomeActivity.kt` | 権限が無効なときだけ表示する最小の権限案内に刷新 |
| `app/src/main/java/com/quartz/powerline/services/StatusBarAccessibilityService.kt` | キーフィルタのみに縮小 |
| `app/src/main/java/com/quartz/powerline/services/QuickSettingsService.kt` | 削除 |
| `app/src/main/java/com/quartz/powerline/services/PowerlineAssistantService.kt` | 削除 |
| `app/src/main/java/com/quartz/powerline/services/PowerlineAssistantSessionService.kt` | 削除 |
| `app/src/main/java/com/quartz/powerline/services/PowerlineAssistantSession.kt` | 削除 |
| `app/src/main/java/com/quartz/powerline/ui/viewmodel/MainViewModel.kt` | `SearchViewModel.kt` に改名・整理 |
| `app/src/main/java/com/quartz/powerline/ui/viewmodel/SettingsViewModel.kt` | 削除（設定状態を持たない） |
| `app/src/main/java/com/quartz/powerline/ui/LauncherScreen.kt` | `SearchScreen.kt` に改名。Overlay 専用 |
| `app/src/main/java/com/quartz/powerline/ui/SettingsScreen.kt` | 削除（状態 / 権限 / About は MainActivity に実装） |
| `app/src/main/java/com/quartz/powerline/ui/AppearanceSettingsScreen.kt` | 削除（テーマは Material You 固定） |
| `app/src/main/java/com/quartz/powerline/ui/theme/Theme.kt` | Material You 固定に簡素化（テーマ / アクセント引数を削除） |
| `app/src/main/java/com/quartz/powerline/data/SettingsRepository.kt` | 削除（設定を永続化しない） |
| `app/src/main/java/com/quartz/powerline/domain/models/AppBackupData.kt` | 削除（JSON バックアップを廃止） |
| `app/src/main/java/com/quartz/powerline/plugins/Plugin.kt` | 削除し、`SearchProvider` を新規実装 |
| `app/src/main/java/com/quartz/powerline/plugins/PluginManager.kt` | 削除し、`ProviderRegistry` を新規実装 |
| `app/src/main/java/com/quartz/powerline/plugins/impl/*` | 全削除し、各 Provider として新規実装（コピーしない） |
| `app/src/main/java/com/quartz/powerline/utils/ActionExecutor.kt` | `SearchAction` ベースに変更 |
| `app/src/main/AndroidManifest.xml` | HOME / ASSIST / QS / 広告 / アシスタント / 廃止 Activity を削除 |
| `app/src/main/res/xml/accessibility_service_config.xml` | キー検出のみに最小化 |
| `app/src/main/res/values/strings.xml` | アクセシビリティ説明を更新 |
| `app/src/main/res/values/themes.xml` | `.Home` テーマを削除 |
| `app/build.gradle.kts` | AdMob / UMP 依存を削除 |
| `BACKUP_SPEC.md` | 削除 |
| `README.md` | V2 のセットアップ、Alt+Space、設定 / JSON を保持しないことを記載 |
| `PRIVACY.md` | キーログ / 画面内容を取らないことを明記 |

---

## 13. 参考: V1 の主要な状態

- `main` は `1c739ae`（Alt+Space、ジェスチャー、並列検索、Advanced、外観設定のマージ済み）。
- `isAdvanced` は既定 `false`。Share / 隠しアプリ管理 / 検索エンジン編集 / Material You トグル / カスタムヘッダーが制限される。
- Alt+Space は `StatusBarAccessibilityService.onKeyEvent()` で処理され、`OverlayActivity` に `TOGGLE_OVERLAY` を送る。
- `OverlayActivity` は `singleInstance`。表示中の再起動は `finish()`、非表示時はそのまま前面化する。QS タイルからも起動できる。
- `SearchEngine` はプラグインを `Dispatchers.Default` で並列実行し、`flatten()` で連結する。
- `MainActivity` は HOME + LAUNCHER。`LauncherScreen` が検索 UI。
- `SettingsActivity` が設定ホスト。`SettingsScreen` は General（Powerline Advanced / Overlay Settings / Appearance Settings / Additional Settings）と Powerline Functions を持つ。
- `AppearanceSettingsScreen` は Theme Selection（System / Light / Dark / Brand / Material You）、Use Material You Colors、Overlay（dim / blur / 背景タップ）、Launcher（ヘッダーテキスト）を持つ。
- `SettingsRepository` は `app_theme` / `custom_accent_color` / `overlay_*` / `home_screen_text` を保持する。
- `QuickSettingsService` と音声アシスタントのスタブが存在する。
- オンボーディングは 6 ページ（ホーム設定、下部トリガー、広告支援を含む）。
- バックアップは v1 フラット JSON。下部トリガー / ステータスバー / テーマ / アクセント / Overlay の設定を含む。
- `AppBackupData` / `BackupMigrator` と `BACKUP_SPEC.md` が存在する。
- AdMob / UMP の同意取得と広告 Banner が存在する。
- V2 は V1 の設定・JSON・履歴を引き継がない（V2 以降の履歴 / favorite は保持する）。

---

> このロードマップは V2 の方向性を固定するための草案です。Phase 0 の未決事項が決まり次第、該当 Phase のタスクと完了条件を更新してください。
