# どうぶつ戦争

1台のAndroid端末を2人で交互に使う、オフライン専用ボードゲームです。
Kotlin + Jetpack Compose、パッケージ名は `com.aokimasanori.doubutsusensou` です。

## 今回の実装（0.2.0）

- タイトルから草チーム、秘密の受け渡し画面、土チームの順に10個ずつ配置できます。
- 横6列、草4段・川1段・土4段。各陣地の最奥中央2列は、1個だけ置ける横長の家です。
- 駒をタップしてからマスをタップ。配置済みの駒も移動・入れ替え・手持ちへの取り消しができます。
- おとしあなは橋の出口へ置けず、とりは家へ置けません。家におとしあなを置くのは可能です。
- 全10個を置くと「これでOK！」が有効になります。確定後は元のチームの配置を編集できません。
- 相手の駒は見た目・読み上げとも「？」だけを表示。受け渡し画面には盤面自体を表示しません。
- 画面再作成・プロセス復元でも配置を維持し、再開時には秘密画面を挟みます。最近使ったアプリ一覧のスクリーンショットも抑止します（API 33以上）。
- 配置がある状態で戻る場合はリセット前に確認します。
- 両チームの配置完了までが今回の範囲です。移動・戦闘・勝敗は次の段階です。
- 駒のベクター絵は日本語の名前付き。元イラストの個別素材へ差し替えられる独立コンポーネントです。
- ネットワーク権限は使用しません。明暗テーマ、小さな画面、横向き、文字拡大に対応したスクロール可能なUIです。

## 構成

```text
app/                          Androidアプリ
  src/main/kotlin/.../
    MainActivity.kt           起動・Composeの設定
    ui/GameViewModel.kt       UI状態・画面遷移・保存状態
    ui/DoubutsuSensouApp.kt    画面の切り替え・戻る操作
    ui/screens/               状態を表示し、操作をコールバックで渡す画面
    ui/theme/                 色・テーマ
  src/main/res/               日本語の文言・アイコン
  src/test/                   UI状態の単体テスト
  src/androidTest/            画面遷移・再作成の端末テスト
game/                         Androidに依存しないKotlin/JVMモジュール
  src/main/kotlin/.../game/
    GameState.kt              プレイヤー・ゲームの段階・不変の状態
    GameEngine.kt             ゲームの状態遷移の入口
  src/test/                   ゲームロジックの単体テスト
```

依存方向は `app → game` のみです。Compose画面にはゲームルールを置きません。
今後は `game` に盤面・駒・移動・戦闘・ターン・勝敗のルールと状態を追加し、
`GameViewModel` から呼び出してUIへ結果を流します。
確定ルールと盤面の出典は [docs/RULES.md](docs/RULES.md) に記録しています。
配置制約・入れ替え・確定・受け渡しの遷移はすべて game モジュールで検証します。

## 開発環境とビルド

- Android Studio（AGP 9.0対応版以降）
- JDK 17以上（Gradle 9.3.0はJDK 25にも対応）
- Android SDK Platform 35、SDK Build Tools 36.0.0
- Android 8.0（API 26）以上の端末
- AGP 9.0.1 / Kotlin 2.2.10 / Gradle 9.3.0 / Compose BOM 2025.04.01

Android Studioでこのリポジトリのフォルダーを開き、Gradle同期を行ってください。
`local.properties` は各PCのSDKパスを設定するローカルファイルで、Gitには含めません。
AGP 9の組み込みKotlinを利用しているため、`app` に `org.jetbrains.kotlin.android` は適用しません。

Windows PowerShell:

```powershell
# Android Studioに付属するJDKを利用する場合
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :app:assembleDebug :game:test :app:testDebugUnitTest :app:lintDebug
```

macOS / Linux:

```sh
./gradlew :app:assembleDebug :game:test :app:testDebugUnitTest :app:lintDebug
```

APKは `app/build/outputs/apk/debug/app-debug.apk` に生成されます。
端末またはエミュレーターを接続した場合は、以下で画面操作テストも実行できます。

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest
```

GitHub ActionsでもAPK生成・単体テスト・Lintを実行し、デバッグAPKを成果物として保存します。

## Pixel 10へのFirebase配布

Firebaseプロジェクト `kyukabomanager` に、既存アプリとは別のAndroidアプリとして登録しています。
App IDは `1:34404720470:android:65e0010cc99f3189a080f7` です。
`main` への更新と、`main` からのActions手動実行で、テスト・Lint・ビルドの成功後に
`aokimasanori@gmail.com` へFirebase App Distributionで配布します。
Pixel 10でそのアカウントのFirebase App Testerを開き、「どうぶつ戦争」をインストールしてください。
プルリクエストの検証では配布しません。

上書きインストールのため、配布版は固定のテスト署名鍵を使います。
ビルド番号は `1000 + GitHub Actionsのrun_number` として増加します。
以下は、このリポジトリのActions secretsに設定済みです。鍵の内容はソースに含めません。

- `CI_DEBUG_KEYSTORE_BASE64`: どうぶつ戦争専用のテスト署名鍵
- `FIREBASE_SERVICE_ACCOUNT_JSON`: 既存Firebaseプロジェクトの配布用認証

ローカルの通常ビルドは、そのPCのデバッグ署名鍵と `versionCode=1` を使います。
配布版と同じ鍵でローカルビルドする場合は `CI_DEBUG_KEYSTORE` に専用鍵のパスを指定し、
`APP_VERSION_CODE` にインストール済み版以上のビルド番号を指定してください。
配布機能だけを利用するため、アプリ内へのFirebase SDK追加は不要です。
[Firebaseの公式配布手順](https://firebase.google.com/docs/app-distribution/android/distribute-cli)。

## 次の段階

1. 通常の駒・チーター・とりの移動、橋・川・家の接続を実装する。
2. 戦闘と、おとしあな・相打ち・家への到達を実装する。
3. 手番・秘密の受け渡し・勝敗を実装し、対戦全体を検証する。

開発環境の仕様: [AGP 9.0](https://developer.android.com/build/releases/agp-9-0-0-release-notes)、
[GradleのJava互換性](https://docs.gradle.org/9.3.0/userguide/compatibility.html)。
