# Powerline V2 Roadmap

> **Status:** 進行中（Phase 1–6 が完了。Phase 7 は自動化できる項目が完了し、実機の端末確認・main へのマージ・配布が残り）  
> **Baseline:** `main` @ `1c739ae`  
> **作業ブランチ:** `v2/phase1-alt-space-trigger` → `v2/phase2-remove-monetization-settings` → `v2/phase3-status-screen` → `v2/phase4-search-pipeline`（Phase 5 も同じブランチ。積み上げ。`main` へは未マージ）  
> **Target:** V2.0（内部ベータ → 公開）  
> **Last updated:** 2026-10-23

---

## 1. V2 のコンセプト

Powerline を「ホーム画面ランチャー」から、**Alt+Space でどこからでも呼び出すキーボードファーストの検索レイヤー**へ作り替える。

```
アプリ使用中
   │
   ├─ Alt+Space ──▶ 検索オーバーレイ（SearchOverlay ウィンドウ）
   │                    │   待機状態: 検索エンジンチップ / 最近のアプリ / 全アプリ
   │                    │   入力後:   アプリ・計算・Web・共有・システム
   │                    │
   │                    └─ 実行 ──▶ 元のアプリの上で目的のアプリ / ブラウザが開く
   │
   └─ アプリアイコン ──▶ MainActivity（状態 / 権限 / About）
```

### Before / After

| 項目 | V1 | V2 |
|---|---|---|
| 主役の画面 | ホーム画面（MainActivity） | **検索オーバーレイ**（アクセシビリティサービスが描く `TYPE_ACCESSIBILITY_OVERLAY` ウィンドウ） |
| トリガー | 画面下の透明オーバーレイ、ステータスバー、QS、アシスタント、Alt+Space | **Alt+Space のみ** |
| MainActivity | HOME + LAUNCHER。検索画面を表示 | **状態 / 権限 / About の案内画面**。LAUNCHER のみ |
| SettingsActivity / OverlayActivity | 設定専用 Activity / 透明 Activity の検索画面 | **どちらも廃止** |
| 設定 UI・設定の保存 | 多数の設定を SharedPreferences に保存 | **設定を持たない**。固定デフォルトで動作。例外は状態画面の Built-in（Apps / Calculator / Web search / Share / Home / Chips のオン・オフ。Web search / Share をオフにすると結果・エイリアス・チップ・設定セクションが消える。Home は未入力時のアプリ一覧、Chips はエイリアスのチップ行）/ Web search / Share（第 8 章 #8。Share の先頭には固定の Share dialog）/ Advanced（Show package name: アプリ結果のパッケージ名表示のオン・オフ） |
| テーマ | Theme Selection / Material You トグル | **Material You 固定**（Android 12+ は壁紙色、11 以下はブランドカラー） |
| 課金・広告 | Powerline Advanced / AdMob / UMP | **完全撤去** |
| バックアップ | JSON export / import | **削除** |
| ユーザーへの質問 | 移行・バックアップ・広告同意の確認 | **システム権限画面以外は質問しない** |
| 空クエリ時 | 検索欄のみ / Recents のリスト | **Spotlight 風の待機状態**（エンジンチップ・最近のアプリ・全アプリのタイル） |
| 検索 | プラグインを並列実行して連結 | **正規化・ランキング・重複排除・タイムアウト**を行うパイプライン（Phase 4） |
| 機能 | プラグインごとに実装が分散 | **SearchProvider + SearchAction で再構築**（Apps / Calculator / Web / Share の 4 つ。Recents / System は廃止）（Phase 5） |
| テスト | 自動テストを追加 | **自動テストで受け入れ判定**（JVM ユニットテスト + エミュレーターでの計装 / E2E テスト） |

---

## 2. ゴールと非ゴール

### ゴール

| # | ゴール | 状態 |
|---|---|---|
| G1 | **トリガーの統一** — Alt+Space に一本化し、画面下・ステータスバー・QS・アシスタント経由の起動を撤去 | ✅ 完了 |
| G2 | **画面の役割反転** — `MainActivity` を状態 / 権限 / About にし、検索オーバーレイを唯一の検索サーフェスにする | ✅ 完了 |
| G3 | **マネタイズ層の撤去** — `isAdvanced`、課金、AdMob / UMP を削除 | ✅ 完了 |
| G4 | **検索コアの再構築** — クエリ解析、並列実行、タイムアウト、ランキング、重複排除 | ✅ 自動テスト通過 |
| G5 | **機能の再構築** — Apps / Calculator / Web / Share を Provider で作り直す（Recents / System は Provider にしない） | ✅ 自動テスト通過 |
| G6 | **設定・バックアップを持たない** — 設定の永続化と JSON を削除 | ✅ |
| G7 | **品質とプライバシー** — キーボード操作、IME、アクセシビリティ、OEM 対策、説明責任 | 🟡 ほぼ完了（実機の端末確認が残り） |
| G8 | **Don't Ask User** — システム権限画面以外で、設定・移行・バックアップ・同意を質問しない | ✅ 完了（Welcome は削除） |

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

## 3. 現状（`v2/phase4-search-pipeline`）

### 3.1 コンポーネント

| ファイル | 現在の役割 | 残作業 |
|---|---|---|
| `MainActivity.kt` | LAUNCHER のみ。常に `StatusScreen` を表示（Welcome は削除済み） | — |
| `ui/StatusScreen.kt` | 2 ペイン（縦は縦積み、横は左右）。ロゴ + 状態 / Permissions チップ（Accessibility・Power option）/ Alt+Space が効かない場合の案内 / About | PRIVACY.md へのリンク（Phase 7） |
| `services/StatusBarAccessibilityService.kt` | Alt+Space の検出（デバウンス付き）と `SearchOverlay` の表示切り替え。前面アプリが変わったら閉じる（IME は除外） | — |
| `overlay/SearchOverlay.kt` | `TYPE_ACCESSIBILITY_OVERLAY` ウィンドウに `SearchScreen` を載せる。独自の Lifecycle / ViewModelStore / SavedState。Esc・Back・背景タップで閉じ、閉じるときは 150ms フェード | — |
| `ui/SearchScreen.kt` / `ui/StandbyContent.kt` / `ui/StandbyGrid.kt` | 検索パネル。空クエリ時は待機状態（エンジンチップ、最近のアプリ最大 8、全アプリ）、入力時は結果リスト | Tab でのセクション移動（Phase 7） |
| `ui/components/CommandBar.kt` | 検索欄。↑↓←→ / Enter を処理 | — |
| `ui/viewmodel/SearchViewModel.kt` | クエリ・`SearchUiState`（セクション・選択位置・読み込み中）・待機状態の最近のアプリ。入力のたびに前の検索を即キャンセルし、40ms 待ってから `SearchPipeline` を呼ぶ | — |
| `search/*` | `SearchPipeline`（解析 → 並列実行 + タイムアウト → ランキング → 重複排除 → 件数制限）、`QueryParser`、`SearchRanker`、`SearchDeduplicator`、`ProviderRegistry`、契約型（`SearchProvider` / `SearchRequest` / `SearchItem` / `SearchAction` / `ResultSection` / `SearchUiState` / `WebSearchEngine`） | — |
| `search/AppIndex.kt` | 起動可能なアプリの一覧と正規化済みの検索キー（ラベル / パッケージ / 組み込みエイリアス）。`LauncherApps.Callback` でインストール・削除・更新のたびに読み直す。待機状態と Apps で共有 | — |
| `search/providers/*` | `AppsProvider` / `CalculatorProvider` / `WebProvider` / `ShareProvider` | — |
| `utils/ActionExecutor.kt` | `SearchAction` の `when` で実行。アプリ起動が成功したら履歴に記録 | — |
| `data/UserDataRepository.kt` | `history`（アプリ起動のみ）/ `favorite_apps` を保持。frecency は履歴から計算。生成時にそれ以外のキーを削除 | — |
| `data/PowerlineDefaults.kt` | 固定値（背景タップで閉じる、待機状態の最近のアプリ 8 件、履歴 100 件・半減期 7 日、検索パイプラインの各値、検索エンジン 7 種、アプリの組み込みエイリアス） | — |
| `ui/theme/Theme.kt` / `Color.kt` | Android 12+ は `dynamicLight/DarkColorScheme`、11 以下は #4CDC8F から生成したブランド配色 | — |

### 3.2 現状の課題

- **リリース準備が未着手**: `README.md` / `PRIVACY.md` がない。`versionCode = 1` / `versionName = "1.0"`、`isMinifyEnabled = false`（Phase 7）。

---

## 4. ターゲット（To-Be）

### 4.1 エントリーポイント

| 入口 | V2 の扱い | 状態 |
|---|---|---|
| **Alt+Space** | 唯一のトリガー。検索オーバーレイを開く / 閉じる | ✅ |
| アプリアイコン | `MainActivity`（状態 / 権限 / About） | ✅ |
| QS タイル / ASSIST / 画面下 / ステータスバー | 削除 | ✅ |
| 通知 | 使用しない | ✅ |

### 4.2 Activity / Service / ウィンドウ構成

| コンポーネント | V2 の役割 | Manifest | 状態 |
|---|---|---|---|
| `MainActivity` | 状態 / 権限 / About。設定は持たない | `LAUNCHER` のみ。`exported=true`、`singleTask` | ✅ |
| `StatusBarAccessibilityService` | Alt+Space のキーフィルタと検索オーバーレイのホスト | `canRetrieveWindowContent=false`、`flagRequestFilterKeyEvents` のみ | ✅ |
| `SearchOverlay` | 検索サーフェス（Activity ではなくウィンドウ） | なし（サービスが `WindowManager` で追加） | ✅ |
| `OverlayActivity` / `SettingsActivity` / `PurchaseActivity` | 廃止 | エントリ削除 | ✅ |
| `QuickSettingsService` / 音声アシスタント | 廃止 | エントリ削除 | ✅ |

> **検索をウィンドウにした理由**: HyperOS のデスクトップモードで `OverlayActivity` がフリーフォームのアプリウィンドウとして開いてしまい、Android 16 では全画面化の手段が非公開 API で塞がれているため。オーバーレイから起動する Activity には `FLAG_ACTIVITY_NEW_TASK` を付ける。

### 4.3 検索オーバーレイの仕様

- パネルは画面中央、最大高さを制限した Material 3 サーフェス（角丸 32dp、内側 12dp、タイル角丸 20dp）。背景の dim / blur はなし。
- 開くときはアニメーションなし（即時表示）、閉じるときは 150ms フェードアウト。
- 閉じる操作: Alt+Space、Esc、Back、背景タップ、別アプリが前面に来たとき（IME は除く）。
- **待機状態（空クエリ）**: 検索エンジンのエイリアスチップ → 最近起動したアプリ（最大 8）→ 全アプリ（名前順）。セクション見出しは置かず、区切り線で分ける。矢印キーで移動、Enter で起動。
  - 最近のアプリと全アプリに**同じアプリが両方出るのは仕様**（Spotlight と同じ）。重複排除の対象外。
- **入力状態**: 結果リストは待機状態のタイルと同じ見た目（角丸、選択は `secondaryContainer`、1 行表示）。
- 検索欄は英数モードで開く（`KeyboardType.Ascii` = `IME_FLAG_FORCE_ASCII`）。IME へのヒントなので、日本語名のアプリを探すときは IME 側で切り替えられる。切り替えのトグルは置かない（設定を持たない方針）。

### 4.4 検索アーキテクチャ（Phase 4–5 の目標）

```
SearchOverlay
  └─ SearchScreen
       └─ SearchViewModel
            └─ SearchPipeline
                 ├─ QueryParser        … alias / prefix / 正規化
                 ├─ ProviderRegistry   … すべての Provider を常時有効
                 ├─ 並列実行           … Provider ごとに timeout、新しい入力でキャンセル
                 ├─ SearchRanker       … 一致度 + frecency + favorite + provider priority
                 ├─ Deduplicator       … app package / URL / action target で重複排除
                 └─ SearchUiState      … sections, selectedIndex, isLoading
                      └─ ActionExecutor
                           └─ SearchAction（LaunchApp / OpenUrl / CopyText / ShareText）
```

#### 新しい契約（案）

```kotlin
interface SearchProvider {
    val id: String
    val displayName: String
    val priority: Int
    val icon: ImageVector

    suspend fun search(request: SearchRequest, context: Context): List<SearchItem>
}

data class SearchRequest(
    val rawQuery: String,
    val normalizedQuery: String,
    val tokens: List<String>,
    val mode: SearchMode, // TEXT, ALIAS
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
}
```

> 設定を持たないため、V1 案にあった `isEnabled(settings)` は外した。実行は Provider ではなく `ActionExecutor` が `SearchAction` を見て行う。待機状態は `SearchViewModel` が履歴とアプリ一覧から直接組み立てるため、パイプラインは空クエリを扱わない。

#### パイプライン仕様

- 入力は 30–50ms デバウンス。新しい入力が来たら前の検索をキャンセルする（キャンセル自体は実装済み）。
- Provider ごとに 150ms、全体で 300ms のタイムアウト。遅い Provider は結果から外す。
- 対象の Provider: **Apps / Calculator / Web / Share** の 4 つ。
  - **Recents は Provider にしない。** 最近使ったものは待機状態の「最近のアプリ」で見せる。入力時は Apps のランキングに frecency として効かせる。
  - **System（Settings）は Provider にしない。** Android の設定は Apps の検索で「設定」アプリとして出る。Powerline の状態画面は情報アイコンとアプリアイコンから開ける。アクセシビリティ / バッテリー最適化の設定は状態画面の Permissions チップから開ける。
- セクション順: **Apps → Calculator → Web → Share**。
- 1 Provider あたり最大 5 件、全体で最大 20 件。
- ランキング: 完全一致 > 前方一致 > 単語境界・頭文字一致 > 部分一致 > 曖昧一致。favorite / frecency / alias にボーナス。
- 正規化: 大文字小文字、全角半角、前後と連続の空白。
- 重複排除: **入力時の結果リストの中だけ**で行う。同じ `packageName`、同じ URL、同じ `SearchAction` は 1 件にまとめる。待機状態は対象外（第 4.3 節）。
- 実行後は履歴に記録し、オーバーレイを閉じる。

### 4.5 MainActivity（状態 / 権限 / About）

```
MainActivity
├─ ロゴペイン: タイトル / アプリアイコン / 状態（Accessibility と Power option の両方が許可なら Active）
└─ 設定ペイン
   ├─ Permissions: Accessibility / Power option（バッテリー最適化の除外）
   ├─ Alt+Space が効かない場合の案内（HyperOS などの OEM 対策）
   └─ About: version / プライバシー方針
```

- Provider の ON/OFF、履歴設定、バックアップ、テーマ、Overlay、Launcher の設定は持たない。
- 旧 General / Overlay Settings / Appearance / Additional Settings / Powerline Functions は**復活させない**。
- 「Powerline はホームアプリではなくなった」案内は Phase 3 で置いたが、状態画面の刷新（`3dae973`）で外した。案内は README に置く（Welcome は削除済み）。

### 4.6 設定・データ・バックアップ

- **設定は保持しない。** 固定値は `PowerlineDefaults` に集約する。
- **データは保持する。** `history`（アプリ起動のみ）/ `favorite_apps` は設定ではないため保持する（設定 UI なし、JSON バックアップなし）。`app_weights` は Phase 5 で履歴ベースの frecency に置き換えたため使わない（旧キーとして Phase 6 で削除）。hidden apps は扱わない。検索設定（`apps_enabled` / `calculator_enabled` / `web_enabled` / `share_enabled` / `home_enabled` / `chips_enabled` / `search_engines` / `share_targets` / `show_package_name`）は `SearchSettingsRepository` に保存する（第 8 章 #8）。
- **JSON は扱わない。** export / import / 復元 / 移行は削除済み。
- 固定デフォルト:
  - テーマ: Material You（Android 12+ は dynamic color、11 以下はブランドカラー #4CDC8F）
  - オーバーレイ: dim / blur なし、背景タップで閉じる、ヘッダーテキストなし
  - 待機状態: 最近のアプリ最大 8 件
  - 検索: すべての Provider を有効、履歴 ON
  - 検索エンジン: Google（既定）/ YouTube Music `ytm` / Maps `maps` / X `x` / YouTube `yt` / Wikipedia `wiki` / Play Store `play`。エンジンは「名前・エイリアス・URL（`%s` に検索語）」だけで定義し、URL をどのアプリで開くかは Android に任せる
- V1 の設定キー（`is_advanced`, `debug_mode`, `status_bar*`, `bottom_trigger*`, `hide_triggers_on_keyboard`, `material_you_enabled`, `app_theme`, `custom_accent_color`, `overlay_*`, `home_screen_text`, `max_recent_items`, `history_enabled`, `calculator_enabled`, `share_enabled`, `share_alias`, `enable_weight_search`, `enable_package_search`, `aliases`, `hidden_apps`, `custom_search_engines`）、`onboarding_completed`、`app_weights` は**起動時に削除**する（Phase 6）。移行はしない。

---

## 5. フェーズ計画

### 進捗サマリー

| Phase | 内容 | 状態 | コミット |
|---|---|---|---|
| 0 | ベースラインと安全網 | 🟡 一部 | — |
| 1 | トリガーを Alt+Space に統一 | ✅ | `a7710b3` |
| 2 | Advanced / 課金 / 広告 / 設定保存の撤去 | ✅ | `c54b28b` |
| 3 | MainActivity = 状態 / 権限、Overlay = 検索 | ✅ | `01221ee`, `3dae973` |
| 3.5 | 検索オーバーレイの刷新（ウィンドウ化・待機状態・Material You） | ✅ | `c097417` … `4dd288c` |
| 4 | 検索コアの再構築 | ✅ 自動テスト通過 | `3cd4201` |
| 5 | 機能（Provider）の再構築 | ✅ 自動テスト通過 | `cef04eb` |
| 6 | 初回起動と旧データの掃除 | ✅ | — |
| 7 | 品質・プライバシー・リリース | 🟡 実機確認・マージ・配布が残り | — |

---

### Phase 0 — ベースラインと安全網（一部）

- [x] V1 の退避ブランチ `backup/pre-consolidation-2026-10-21` を作成。
- [x] Phase ごとの作業ブランチ（`v2/phase1-…` → `v2/phase2-…` → `v2/phase3-…`）を積み上げで作成。単一の `v2` ブランチは作らない。
- [x] `./gradlew :app:assembleDebug` が通ることを確認。
- [x] 撤去対象を一覧化（Phase 1–2 で消化済み）。
- [ ] `main` @ `1c739ae` に `v1-final` タグを打つ。
- [ ] V1 の APK サイズ、起動時間、検索レイテンシを記録する（Phase 7 の比較用。未計測のまま進んでいる）。
- [ ] 未決事項（第 8 章）を決定する。

---

### Phase 1 — トリガーを Alt+Space に統一 ✅

- アクセシビリティサービスから画面下の透明 View・キーボード監視を削除し、Alt+Space のみ処理（`ACTION_DOWN` の連打はデバウンス）。
- `accessibility_service_config.xml` を `typeWindowStateChanged` / `flagRequestFilterKeyEvents` / `canRetrieveWindowContent="false"` に最小化。
- QS タイル（`QuickSettingsService`）、Overlay Settings、下部トリガー関連の設定・オンボーディング項目を削除。
- アクセシビリティ説明文を「Alt+Space を検出して Powerline を開く。画面内容は読み取らない」に更新。

---

### Phase 2 — Advanced / 課金 / 広告 / 設定保存の撤去 ✅

- `isAdvanced`、`PurchaseActivity`、Debug トグル、AdMob / UMP（依存・Manifest・同意取得・Banner）を削除。
- `SettingsRepository` を `UserDataRepository` に縮小（`history` / `favorite_apps` / `app_weights` / `onboarding_completed`）。固定値は `PowerlineDefaults` に集約。
- `AppearanceSettingsScreen`、プラグイン設定画面、JSON バックアップ（`AppBackupData` / `BackupMigrator` / `BACKUP_SPEC.md`）を削除。hidden apps / ユーザー alias / 検索エンジン編集は廃止。
- `Theme.kt` から `appTheme` / `customAccentColor` / `dynamicColor` 引数を削除。
- 削除 API を参照していたユニットテスト 8 件を削除。

---

### Phase 3 — MainActivity = 状態 / 権限、Overlay = 検索 ✅

- `MainActivity` を状態 / 権限 / About 画面（`StatusScreen`）に置き換え、HOME カテゴリと `stateNotNeeded` を削除。
- `SettingsActivity` / `SettingsScreen` / `SettingsViewModel`、音声アシスタント関連、`WallpaperTheme.kt`、`NotificationHelper.kt` を削除。
- `MainViewModel` → `SearchViewModel`、`LauncherScreen` → `SearchScreen` に改名し、設定ナビゲーションを削除。
- 検索候補「settings」は「Powerline Status」として状態画面を開く（Phase 5 で System ごと廃止）。
- `3dae973` で状態画面を 2 ペインに刷新。Permissions チップに Power option（バッテリー最適化の除外）を追加し、ホームアプリ廃止の案内を外した。

---

### Phase 3.5 — 検索オーバーレイの刷新 ✅（計画外で追加）

- [x] `OverlayActivity` を廃止し、アクセシビリティサービスの `TYPE_ACCESSIBILITY_OVERLAY` ウィンドウ（`SearchOverlay`）で検索を描く（`c097417`）。
- [x] Esc / Back / 背景タップ / 別アプリの前面化で閉じる。
- [x] Spotlight 風の待機状態（エンジンチップ / 最近のアプリ / 全アプリ）と、矢印キー + Enter での操作（`60a0fda`）。
- [x] dim / blur を廃止し、パネルを中央に配置して高さを制限。
- [x] セクション見出しを外し、全アプリが 3 行入る高さに調整（`e13a392`）。
- [x] 閉じるときの 150ms フェードアウト（`7b6b5c7`）。
- [x] ハードウェアアクセラレーションの有効化と、favicon をハードウェアビットマップ以外で読み込む対策（回転時・ソフトウェア描画時のクラッシュ回避）。
- [x] 結果リストを待機状態のタイルと同じ見た目に統一（`5c6e2e8`）。
- [x] テーマを Material You に戻す（`3dae973` で一度ブランドカラー固定にしたものを `4dd288c` で壁紙連動に変更。11 以下はブランドカラー）。
- [x] 検索欄を英数モード（`IME_FLAG_FORCE_ASCII`）で開く。トグルは置かない。

---

### Phase 4 — 検索コアの再構築

**目的:** 検索を「プラグインの連結」から、ランキングとタイムアウトを備えたパイプラインに置き換える。

**タスク**

- [x] 新パッケージ `dev.quxomiya.powerline.search` を作成。
  - [x] `SearchProvider.kt` / `SearchRequest.kt` / `SearchMode.kt`
  - [x] `SearchItem.kt` / `ResultSection.kt` / `SearchAction.kt`
  - [x] `QueryParser.kt`（正規化・エイリアス判定）
  - [x] `SearchPipeline.kt` / `SearchRanker.kt` / `SearchDeduplicator.kt`
  - [x] `SearchUiState.kt` / `ProviderRegistry.kt`
- [ ] `SearchViewModel`
  - [x] 新しい入力で前の検索をキャンセル（`searchJob?.cancel()`）。
  - [x] ↑↓ / Enter による選択と実行。
  - [x] 40ms のデバウンス。`debounce` + `collectLatest` ではなく、入力のたびに前のジョブを即キャンセルし、新しいジョブの先頭で `delay(40)` する（`collectLatest` だとデバウンス中に古い検索が結果を出せてしまうため）。結果の反映は Main で行い、キャンセル済みの検索は反映されない。
  - [x] 空クエリに戻ったら結果を即座に捨てる（次の入力で古い結果が一瞬出ない）。
  - [x] `SearchUiState`（セクション・選択位置・読み込み中）を公開する。
- [ ] `SearchPipeline`
  - [x] すべての Provider を並列実行（`Dispatchers.Default`）。Provider の処理は呼び出し側と別のスコープで動かし、ブロッキングしていてもタイムアウトで待つのをやめて結果を捨てる。
  - [x] Provider ごとに 150ms、全体で 300ms の締め切り（各 Provider の待ち時間は `min(150ms, 全体の残り)`）。例外を投げた Provider も結果から外す。
  - [x] `SearchRanker` で採点し、`SearchDeduplicator` で重複排除。
  - [x] セクションごとに最大 5 件、全体 20 件に制限（重複排除のあとに適用）。
  - [x] エイリアス入力（`yt laneige`）は Web だけを実行。
- [ ] `SearchRanker`
  - [x] 完全一致 1000 / 前方一致 800 / 単語境界・頭文字・全トークンが単語の先頭 600 / 部分一致 400 / 曖昧一致（部分列）200。タイトルと `keywords`（アプリはパッケージ名）の最大値。
  - [x] favorite +300、frecency は `app_weights`（0–100）× 2（Phase 5 で履歴の時間減衰ベースに変更）。provider priority は同点時のタイブレーク。
  - [x] 計算・Web・共有のようにクエリから作る結果（`rankByMatch = false`）は文字列一致で採点せず、Provider の順序を保つ。
  - [x] 大文字小文字・全角半角（NFKC）・前後と連続の空白の正規化。
  - [x] alias ボーナス → Phase 5 でアプリの組み込みエイリアスを検索キーに加える形で実装。
  - [x] 履歴の時間減衰を frecency に使う → Phase 5 で実装。
- [ ] `ActionExecutor`
  - [x] `SearchAction` の `when` で実行する形に変更し、`pluginId` の逆引きを廃止。`PluginManager` は使われなくなった。
  - [x] 実行後に履歴を記録。アプリの重みの更新は `AppLauncherPlugin.execute()` から `UserDataRepository.recordAppLaunch()` へ移動。
- [ ] 移行用アダプタ
  - [x] `PluginSearchProviderAdapter` で既存プラグインを包み、Phase 5 で順次置き換える。
  - [x] 登録するのは Apps / Calculator / Web / Share の 4 つだけ。Recents（空クエリ専用）と System（`settings` → Powerline Status）は登録しないため、Phase 4 の時点で候補から消える。`SearchOverlay` の `settings_action` 特別扱いも削除。
  - [x] `domain/SearchEngine.kt` を削除（Phase 5 の予定を前倒し）。

**完了条件**

- 1–2 文字のアプリ検索で、最初の結果が 100ms 以内（ミドルレンジ端末）。
- 入力中に前の検索がキャンセルされ、古い結果が一瞬表示されない。
- 入力時の結果リストで同じアプリ・URL・アクションが二重に出ない（待機状態の重複は対象外）。
- 遅い Provider があっても他の結果が 300ms 以内に表示される。
- 人間による手動テストで主要フローが動作する。

**状態:** 完了。`SearchPipelineTest`（タイムアウト・並列・例外・件数上限・重複排除・エイリアス）と、エミュレーターでの `SearchOnDeviceTest`（1–2 文字の検索が 100ms 以内、入力中に古い結果が出ない）、`OverlayE2ETest` で確認。

**目安:** 4–5 日

---

### Phase 5 — 機能（Provider）の再構築

**目的:** 既存プラグインを**全削除**し、Provider 契約でゼロから作り直す。

#### Apps

- [x] `AppIndex` を導入し、アプリ一覧・ラベル・パッケージ・クラス名と正規化済みの検索キーをキャッシュ（待機状態の全アプリ表示と共有）。`LauncherApps.Callback` でパッケージの変更時に読み直す（以前はサービスの起動中ずっと同じ一覧だった）。
- [x] ラベル / パッケージ（先頭セグメントを除く）/ 組み込みエイリアス（`PowerlineDefaults.APP_ALIASES`。例: `phone` → 電話、`play store` → Play ストア）の検索。
- [x] あいまい一致（単語境界、頭文字、大文字小文字、全角半角）— Phase 4 の `SearchRanker.match` を使用。
- [x] favorite / frecency は設定 UI なしで保持（`favorite_apps` / 履歴）。
- [x] 長押しメニュー（`AppActionMenu`）: favorite、アプリ情報、共有（アプリ名 + Play ストアのリンク）、ショートカット。
- [x] 起動は既知のランチャー Activity を明示して行う（ランチャーアイコンが複数あるパッケージで別の画面が開かないように）。重複排除もパッケージ単位ではなく Activity 単位。

#### 履歴（Provider ではない）

Recents は Provider にしない（第 4.4 節）。履歴は待機状態の「最近のアプリ」と Apps の frecency のためのデータとして扱う。

- [x] `HistoryItem` を `(packageName, className, timestamp)` にし、`ResultType` / `pluginId` への依存をなくす。V1 の履歴（`type = "APP"` の項目）はそのまま読める。
- [x] 履歴の記録はアプリ起動のみにする（Web / Calc / Share は記録しない。V1 の該当項目は読み飛ばす）。
- [x] 時間減衰: frecency = Σ 0.5^(経過時間 / 7 日)。ボーナスは 1 回あたり 40、上限 200。`app_weights` は使わなくなった。
- [x] 待機状態の「最近のアプリ」は `SearchViewModel` が履歴から直接組み立てる（起動した Activity を優先）。

#### Calculator

- [x] 四則演算（`×` `÷` も可）、括弧、後置パーセント（`50%` = 0.5、`200*10%` = 20）、負数、小数点。
- [x] 計算結果のコピー。
- [x] 表示: 有効数字 12 桁に丸めて指数表記を使わない（`0.1+0.2` = `0.3`）。
- [x] エラー時は結果を出さない（入力途中の `12+`、閉じ括弧のない `(1+2`、`1/0`、演算子のない `-5`）。入力中にエラーが点滅しないため。
- [x] 単位変換は V2 では扱わない。

#### Web

- [x] 固定の検索エンジン（第 4.6 節）。ユーザー編集なし。
- [x] エイリアス prefix（例: `yt laneige`、大文字小文字・全角は問わない）。待機状態のエンジンチップからも選べる。
- [x] URL 直接入力（`https://` 補完、ポート・パス・クエリ可）。URL 候補は既定エンジンでの検索より上に出る。
- [x] エンジンは URL テンプレートだけで定義する（`SearchEngine(id, name, alias, url)`）。組み込みも、将来ユーザーが追加するエンジン（第 8 章 #8）も同じ扱い。アプリ固有の検索インテント（Gmail・Kindle などのアプリ内検索）は、アプリによって使えないものがあり公平でもないため入れない。
- [x] favicon の取得をやめ、エンジンのアイコンは**その URL を Android が開くアプリのアイコン**にする（YouTube の URL なら YouTube アプリ、リンクを持つアプリがなければ既定のブラウザ。既定がなく選択画面になる場合は検索アイコン）。ブランドロゴは同梱しない。`AppIndex` がパッケージの変更時に更新する。
- [x] AI アプリ（Claude / ChatGPT / Gemini）・翻訳などは追加しない（Share で渡せるため）。
- [x] 検索時に検索クエリを外部へ送らない。実行時のみブラウザを開く。favicon 取得をやめたため **Coil 依存と `INTERNET` 権限を削除**。

#### Share

- [x] クエリ文字列の共有。共有シート起動。

#### 廃止する Provider

- [x] `RecentsPlugin` を削除し、作り直さない（代わりは待機状態と frecency）。
- [x] `SystemPlugin` を削除し、作り直さない（「settings → Powerline Status」の候補もなくなる）。
- [x] `PowerlineDefaults.MAX_RECENT_ITEMS` / `ENABLE_WEIGHT_SEARCH` / `ENABLE_PACKAGE_SEARCH` を削除。

#### 共通

- [x] `plugins/`（`Plugin` / `PluginManager` / `impl/*`）、`search/PluginSearchProviderAdapter`、`domain/AppRepository`（`AppIndex` に統合）を**全削除**。
- [x] `ResultType` / `SearchResult` / `CustomSearchEngine`（→ `WebSearchEngine`）を削除。
- [x] Provider ごとの設定・ON/OFF は持たない。
- [x] `CalculatorTest` に新しい挙動（パーセント、`×` `÷`、丸め、エラー）のケースを追加。

**完了条件**

- 旧 `Plugin` / `PluginManager` / `plugins/impl/*` / `SearchResult` / `SearchEngine` への参照が 0。✅
- 検索結果のセクション表示とキーボード操作が破綻しない。
- 人間による手動テストで主要フローが動作する。

**状態:** 完了。`ProvidersTest` / `QueryParserTest` / `SearchRankerTest` / `UserDataRepositoryTest` と、エミュレーターでの `SearchOnDeviceTest` / `OverlayE2ETest` で確認。

**目安:** 4–5 日

---

### Phase 6 — 初回起動と旧データの掃除 ✅

**目的:** 初回起動で余計な案内を挟まず、V1 の残骸をディスクからも消す。状態画面は Phase 3 で完成済み。

- [x] 状態画面（Alt+Space の状態、権限、使い方、About）— Phase 3 で完了。
- [x] Welcome からテーマ選択・ホーム設定・下部トリガー調整・広告同意・バックアップ確認を削除 — Phase 1–2 で完了。
- [x] AdMob / UMP の残骸を削除 — Phase 2 で完了。
- [x] `WelcomeActivity`（ランディング / チュートリアル）を削除。アプリアイコンからは常に状態画面を開く。
- [x] `UserDataRepository` の `onboarding_completed` API を削除（ディスク上のキーは旧キー削除で消す）。
- [x] 第 4.6 節の V1 設定キーを起動時に削除する（ユーザーに確認しない）。→ `UserDataRepository` の生成時に `powerline_settings` から `history` / `favorite_apps` 以外のキーを消す（キー名の列挙ではなく許可リスト）。実際には V1 は `com.quartz.powerline` で、V2（`dev.quxomiya.powerline`）とはストレージが別のため、V1 のキーはここに存在しない。消えるのはパッケージ名変更後の開発ビルドが残した `onboarding_completed` / `app_weights` など。
- [x] アプリ管理下の旧 JSON ファイルを起動時に削除する。→ 対象なし。V1 の JSON エクスポートはユーザーが選んだ場所（SAF）に書いており、アプリ管理下にファイルはない。

**完了条件**

- 初回起動でも Welcome を挟まず、状態画面を開く。
- `powerline_settings` に `history` / `favorite_apps` 以外のキーが残らない。
- システム権限画面以外に、ユーザーへ質問する画面がない。

**目安:** 1 日

---

### Phase 7 — 品質・プライバシー・リリース（実機確認・マージ・配布が残り）

**キーボード UX**

- [x] ↑↓←→ で選択、Enter で実行、Esc / Back で閉じる。
- [x] 閉じるときのフェードアウト。dim / blur は廃止。
- [x] 画面回転でクラッシュしない（ハードウェアビットマップ対策）。
- [x] Tab / Shift+Tab でセクション移動（待機状態: チップ → 最近のアプリ → 全アプリ、入力時: 結果セクション）。端で反対側へ戻る。待機状態のチップは ←→ で選び Enter でエイリアスを入力、↑ で最上段からチップへ。キーボードで選んだチップは primary の枠で示す。
- [x] Enter の離し（KEY_UP）が背景の `clickable` に届いてオーバーレイが閉じる不具合を修正（チップで Enter を押すと閉じていた）。検索欄が押下時に処理したキーは離しも消費する。
- [x] 開いたときに IME / フォーカスを確実に取る（`FocusRequester` + `SoftwareKeyboardController`）。最初のフレームでウィンドウフォーカスがない場合に備えて数回リトライする。
- [ ] マルチウィンドウ・タブレット幅・HyperOS デスクトップモードでのレイアウト確認。（Android Desktop エミュレーター 2880×1800 では確認済み。HyperOS は実機が必要）

**アクセシビリティ**

- [x] `contentDescription = null` を見直す（現在 9 か所）。すべてテキストラベルの隣の装飾アイコンで null のまま。アプリアイコン（`AppIcon` の `ImageView`）は読み上げ対象から外した。
- [ ] TalkBack で状態画面と検索結果が読めることを確認。（代わりに `OverlayE2ETest.clickableElementsAreLabeled` で、待機状態と結果のクリックできる要素がすべて読み上げ用のテキストを持つことを確認。実際の TalkBack 操作は未確認）
- [x] タッチターゲット 48dp 以上。アプリタイルと結果行は 48dp を超え、チップと IconButton は Material の最小サイズ（48dp）が効いている。

**パフォーマンス**

- [x] Alt+Space から表示まで 200ms 以内。`SearchOverlay` がキーイベントの時刻から最初のフレームまでをログに出し、`OverlayE2ETest.opensWithin200ms` が判定する。エミュレーターで中央値 139ms（debug）、リリースビルドでは 62–73ms。
- [x] アプリ一覧のロードを初回のみにし、2 回目以降はキャッシュ。`AppIndex` はサービスの生存中 `SearchViewModel` に保持され、パッケージ変更時だけ読み直す（Phase 5）。
- [x] 入力 1 文字あたりの UI 更新を 16ms 以内に保つ。`gfxinfo framestats` の UI スレッド時間（HandleInputStart → SyncQueued）で判定（`OverlayE2ETest.typingFramesFitIn16ms`）。エミュレーターで p50 6.5ms / p90 11.9ms。フレーム全体の時間は注入入力の遅延とエミュレーターのソフトウェア描画が大半を占めるため使わない。
- [x] リリース APK サイズを測定。V1（`main`、minify なし）14.85MB → V2（minify なし）10.50MB → V2（R8 + リソース縮小）1.41MB。

**手動テストマトリクス（人間が実施）**

- [ ] Pixel / stock Android
- [ ] Samsung One UI
- [ ] Xiaomi HyperOS（バッテリー制限・サービス kill・デスクトップモード）
- [ ] Android 8（minSdk 26）と Android 11（ブランドカラーのフォールバック）
- [ ] 物理キーボード（US / JIS）
- [ ] Gboard / Google 日本語入力 / Samsung キーボード
- [ ] ライト / ダーク / Material You（壁紙を変えたときに配色が追従するか）
- [ ] マルチウィンドウ / 画面回転
- [ ] 再起動後に設定・JSON が保持されないこと（履歴 / favorite は保持される）

**ドキュメント**

- [x] `README.md`: V2 のセットアップ（アクセシビリティ有効化、Power option、Alt+Space）、ホームアプリではなくなったこと、設定・JSON を保持しないこと。キー操作と開発手順も記載。
- [x] `PRIVACY.md`: 画面内容を読まない、キーログを取らない、`INTERNET` 権限を持たない（通信しない）。保存するデータ（履歴 / favorite）、Android Auto Backup、`QUERY_ALL_PACKAGES` の用途も記載。
- [x] 状態画面の About から `PRIVACY.md` へリンク（`PowerlineDefaults.PRIVACY_POLICY_URL`、GitHub の `main` 上のファイル。`main` へマージするまではリンク切れ）。

**リリース**

- [x] `versionCode = 2` / `versionName = "2.0.0"`。
- [x] `isMinifyEnabled` / `isShrinkResources` を有効化。Gson が使う `HistoryItem` / `StoredHistoryItem` のフィールド名を `proguard-rules.pro` で保持。匿名 `TypeToken` は R8 でジェネリクス情報が消えて履歴が読めなくなったため、`TypeToken.getParameterized` に変更（リリースビルドをエミュレーターに入れて、履歴の保存・読み込み、計算、状態画面を確認）。
- [ ] 署名付きリリースビルド。→ `keystore.properties`（git 管理外）があれば `release` を署名する設定を追加済み（一時的な鍵で動作確認済み）。リリース用の鍵の作成と保管は未着手。
- [ ] V2 の作業ブランチ（現在は `v2/phase6-7-cleanup-release` まで積み上げ）を `main` へマージ（PR）。
- [ ] 内部テスト → ベータ → 公開の順で配布。

**完了条件**

- 手動テストマトリクスでクラッシュ / ANR がない。
- Alt+Space が主要端末・主要 IME で動作する。
- プライバシー説明とアクセシビリティ説明がストア / アプリ内 / README で一致している。
- 署名済み APK が生成できる。

**目安:** 3–5 日

---

## 6. テスト方針

- **受け入れ判定は自動テスト**で行う。
  - JVM: `./gradlew :app:testDebugUnitTest`（検索コア・Provider・履歴。Robolectric）
  - エミュレーター: `scripts/device-tests.sh`（実アプリ一覧での検索と速度、Alt+Space → 入力 → Enter / Esc / Tab の E2E、表示時間・入力中の UI スレッド時間・読み上げラベル）
- Powerline は物理キーボードの Alt+Space にしか反応しない（`input keyevent` はアクセシビリティのキーフィルタを通らない）。E2E は `/dev/input` に `sendevent` で書き込むため、`adb root` と `chmod 666 /dev/input/event*` が必要（スクリプトが行う。できない端末では E2E はスキップ）。
- `connectedDebugAndroidTest` は終了時にアプリをアンインストールする。続けて触るときは `./gradlew :app:installDebug` とアクセシビリティの再有効化が必要。
- 自動化していない確認（実機・ソフトウェアキーボード・回転・マルチウィンドウなど）は Phase 7 の端末確認で行う。
- リポジトリには `CalculatorTest` / `StandbyGridTest` / `SearchViewModelTest` / `PerformanceTest` の 4 ファイルが残っている（`StandbyGridTest` は Phase 3.5 で追加）。扱いは第 8 章 #7 で決める。

### 手動テストの主な項目

- Alt+Space で開く / 閉じる（連打、長押し、キーリピート、IME 表示中）。
- 物理キーボードで入力 → 矢印 → Enter → アプリ起動。待機状態のタイルでも同様。
- Esc / Back / 背景タップ / 別アプリへの切り替えで閉じる。
- アクセシビリティ権限・Power option を OFF にしたときの状態画面の表示。
- 回転 / マルチウィンドウ / タブレット / HyperOS デスクトップモード。

### パフォーマンス目標

| 項目 | 目標 |
|---|---|
| オーバーレイ表示 | 200ms 以内 |
| 最初の検索結果 | 100ms 以内（アプリ名 1–2 文字） |
| 入力中の UI 更新 | 16ms / frame |
| アプリ一覧ロード | 初回 500ms 以内、2 回目以降はキャッシュ |
| リリース APK | 15MB 以下（目安） |

### ストア / ポリシー

- アクセシビリティサービスは「Alt+Space の検出」と「検索オーバーレイの表示」にのみ使用し、画面内容の取得・キーログを行わない。
- `TYPE_ACCESSIBILITY_OVERLAY` の利用目的もプライバシーポリシーと権限案内で説明する。
- Google Play のアクセシビリティポリシー上、キー検出目的の利用は審査リスクがある。配布方法は第 8 章で決める。

---

## 7. リスクと対策

| リスク | 影響 | 対策 |
|---|---|---|
| アクセシビリティサービスが Play ポリシーに抵触 | ストア公開不可 | 目的を最小化し、プライバシーを明記。GitHub Releases を主軸にする |
| Alt+Space がアプリ / IME に横取りされる | 起動できない | `flagRequestFilterKeyEvents` を維持。主要 IME で検証。README と状態画面で既知の制限を明記 |
| OEM（Xiaomi 等）がサービスを kill | トリガーと検索の両方が止まる | Power option チップでバッテリー最適化の除外を案内。状態画面に対処方法を表示（実装済み） |
| 検索がサービス内のウィンドウになった | サービスが落ちると検索も使えない。メモリはサービスの寿命に引きずられる | オーバーレイを閉じたら ViewModel / Lifecycle を破棄する。kill 対策は上と同じ |
| OEM のウィンドウ管理（デスクトップモード等） | 表示崩れ | `TYPE_ACCESSIBILITY_OVERLAY` で回避済み。HyperOS で継続確認 |
| 設定を持たない | 好みに合わせられない | 固定デフォルトと割り切る |
| ホームアプリではなくなる | 既存ユーザーのホームが空になる | README で明示 |
| 検索コアの作り直しで regression | 主要機能が動かない | アダプタ経由で Provider 単位に移行し、都度手動確認 |
| タッチ端末で使えない | ユーザー層が限定される | V2 はキーボード前提。タッチの導線は V2.1 で検討 |

---

## 8. 未決事項

| # | 決定事項 | 推奨 | 期限 |
|---|---|---|---|
| 1 | パッケージ名を維持するか | **決定済み**：`dev.quxomiya.powerline` に変更 | — |
| 2 | minSdk を 26 のままにするか | **維持** | Phase 7 前 |
| 3 | コマンド prefix を導入するか | **V2 では組み込み alias のみ**。`?` / `=` / `>` は V2.1 | Phase 4 |
| 4 | UI 言語 | **V2 は英語**。日本語は V2.1 | Phase 6 |
| 5 | 配布方法 | まず **GitHub Releases / 内部ベータ**。Play はポリシー確認後 | Phase 7 |
| 6 | favicon を外部（`google.com/s2/favicons`）から取得し続けるか | **決定済み**：同梱のベクターアイコンに置き換え、`INTERNET` 権限も削除 | — |
| 7 | 残っているユニットテスト 4 ファイルの扱い | **決定済み**：自動テストを受け入れ判定に使う方針に変更。既存 4 ファイルも残す | — |
| 8 | ユーザー登録のエイリアス（設定は次の 2 項目のみ） | **実装済み**（状態画面の Web search / Share セクション。組み込みエンジンも編集・削除・並べ替えでき、「Restore defaults」で戻せる。チップは Web → 共有の順に 1 行。エイリアスをスペース付きで打つと検索欄の中でチップになり、空欄で Backspace を押すとエイリアスの文字に戻る）。当初案: (1) **URL クエリー**: 名前・エイリアス・URL（`%s`）。エンジンは URL テンプレートだけで定義済みなので、登録 UI と保存を足せば済む。(2) **クイック共有**: エイリアス・共有先アプリ（`text/plain` の `SEND` を受け付けるアプリから選ぶ）。`cl 質問` で共有シートを出さずにそのアプリへ送る。V2 の「設定を持たない」（G6）の例外になるため、入れるときに方針を見直す | V2.1 |

---

## 9. Definition of Done（V2.0）

- [ ] `main` に `v1-final` タグがあり、V2 の作業ブランチが `main` にマージされている。
- [x] 常時トリガーは Alt+Space のみ。QS / ASSIST / 画面下 / ステータスバーのトリガーが存在しない。
- [x] `MainActivity` が状態 / 権限 / About の案内画面で、`LAUNCHER` のみを持つ。
- [x] `SettingsActivity` / `PurchaseActivity` / `OverlayActivity` / `QuickSettingsService` / 音声アシスタントが削除されている。
- [x] 旧設定画面（Powerline Advanced / Overlay Settings / Appearance / Additional Settings / Powerline Functions）が存在しない。
- [x] 設定画面、テーマ選択、アクセントカラー、Provider ON/OFF、履歴設定が存在しない。
- [x] テーマは Material You 固定（Android 12+ は壁紙連動、11 以下はブランドカラー）。
- [x] `isAdvanced` / 課金 UI / AdMob / UMP が存在しない。
- [x] JSON バックアップ / 復元 / 移行のコード・ファイル・UI が存在しない。
- [x] 検索オーバーレイが Alt+Space からのみ開き、待機状態と結果リストがキーボードで操作できる。
- [x] 検索パイプライン（解析・並列・タイムアウト・ランキング・重複排除）が動作する。
- [x] Apps / Calculator / Web / Share が新 Provider で動作する（Recents / System の Provider は存在しない）。
- [x] V1 の設定キーと `onboarding_completed` がディスクに残らない。
- [x] 初回起動でも Welcome を挟まない。
- [x] システム権限画面以外に、ユーザーへ質問する画面がない。
- [ ] 人間による手動テストマトリクスでクラッシュ / ANR がない。
- [x] `README.md` / `PRIVACY.md` があり、状態画面からリンクされている。
- [ ] `versionCode = 2` / `versionName = "2.0.0"` の署名済み APK が生成できる。

---

## 10. マイルストーン

| マイルストーン | 内容 | 目安 | 状態 |
|---|---|---|---|
| M0 | ベースライン | 0.5 日 | 🟡 |
| M1 | Alt+Space 単一トリガー | 1–2 日 | ✅ |
| M2 | Advanced / 課金 / 広告 / 設定保存の撤去 | 1–2 日 | ✅ |
| M3 | 状態画面 + 検索オーバーレイの刷新 | 2–3 日 | ✅ |
| M4 | 検索コア（パイプライン / ランキング） | 4–5 日 | ✅ |
| M5 | Provider 再実装（Apps / Calc / Web / Share） | 4–5 日 | ✅ |
| M6 | 初回起動と旧データの掃除 | 1 日 | ✅ |
| M7 | QA / プライバシー / リリース | 3–5 日 | 🟡 実機確認・鍵・マージ・配布が残り |
| **残り** | | **実機確認と配布準備**（個人開発の目安で数日） | |

---

## 11. 付録: ファイル別の変更マップ

| ファイル | V2 の変更 | 状態 |
|---|---|---|
| `MainActivity.kt` | 状態 / 権限 / About | ✅ |
| `WelcomeActivity.kt` | 削除 | ✅ |
| `services/StatusBarAccessibilityService.kt` | Alt+Space のキーフィルタ + オーバーレイのホスト | ✅ |
| `overlay/SearchOverlay.kt` | 新規。検索ウィンドウ | ✅ |
| `ui/StatusScreen.kt` | 新規。状態 / 権限 / About | ✅（PRIVACY リンクのみ残り） |
| `ui/SearchScreen.kt` / `StandbyContent.kt` / `StandbyGrid.kt` / `components/*` | 検索パネル・待機状態 | ✅（Tab 移動が残り） |
| `ui/viewmodel/SearchViewModel.kt` | `SearchUiState` とパイプラインへの置き換え | ✅ |
| `ui/theme/Theme.kt` / `Color.kt` | Material You + ブランドカラーのフォールバック | ✅ |
| `domain/SearchEngine.kt` | 削除し `search/SearchPipeline` へ | ✅ |
| `plugins/Plugin.kt` / `PluginManager.kt` / `plugins/impl/*` | 削除し `search/` 以下に Apps / Calculator / Web / Share の Provider を新規実装。Recents / System は作り直さない | ✅ |
| `domain/models/SearchResult.kt` | 削除し `SearchItem` へ | ✅ |
| `utils/ActionExecutor.kt` | `SearchAction` ベースに | ✅ |
| `data/UserDataRepository.kt` | `onboarding_completed` API を削除、旧キーの起動時削除 | ✅ |
| `data/PowerlineDefaults.kt` | 固定値の集約 | ✅ |
| `AndroidManifest.xml` | HOME / ASSIST / QS / 広告 / 廃止 Activity を削除 | ✅ |
| `res/xml/accessibility_service_config.xml` | キー検出のみに最小化 | ✅ |
| `app/build.gradle.kts` | 広告依存の削除 ✅ / バージョンと署名 ⬜ | 🟡 |
| `README.md` / `PRIVACY.md` | 新規作成 | ⬜ |

---

## 12. 参考: V1 の主要な状態（`main` @ `1c739ae`）

- `MainActivity` は HOME + LAUNCHER で、`LauncherScreen` が検索 UI。
- `OverlayActivity`（`singleInstance`）が ASSIST / QS / 画面下トリガー / Alt+Space から開く。
- `SettingsActivity` が設定ホスト。General（Powerline Advanced / Overlay / Appearance / Additional）と Powerline Functions を持つ。
- `isAdvanced` は既定 `false` で、Share / 隠しアプリ管理 / 検索エンジン編集 / Material You トグルが制限される。
- `SearchEngine` はプラグインを並列実行し、`flatten()` で連結する。
- オンボーディングは 6 ページ（ホーム設定、下部トリガー、広告支援を含む）。
- JSON バックアップ（`AppBackupData` / `BackupMigrator` / `BACKUP_SPEC.md`）と AdMob / UMP が存在する。
- V2 は V1 の設定・JSON・履歴を引き継がない（V2 以降の履歴 / favorite は保持する）。
