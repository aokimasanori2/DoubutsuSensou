# どうぶつ戦争

1台のAndroid端末を2人で交互に使う、オフライン専用ボードゲームです。
Kotlin + Jetpack Compose、パッケージ名は `com.aokimasanori.doubutsusensou` です。

## 今回の実装（0.1.0）

- タイトル画面で「ふたりであそぶ」を押すと、プレイヤー1の初期配置画面へ進みます。
- 「タイトルにもどる」またはAndroidの戻る操作で、タイトルへ戻ります。
- 画面回転やActivityの再作成後も、開いている画面を維持します。
- 初期配置画面は案内とプレースホルダーです。駒の配置や対戦はまだできません。
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
盤面の寸法、駒の種類や強さ、特殊ルールは、この段階で仮決定していません。
駒の配置を追加する際は、画面名だけでなくゲーム状態の保存・復元も追加してください。

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

1. 確定したルールに合わせて盤面と駒のモデルを追加する。
2. 2人の初期配置と、相手に伏せ駒を見せない端末受け渡しを実装する。
3. 移動・戦闘・ターン交代・勝敗判定を順に追加する。

開発環境の仕様: [AGP 9.0](https://developer.android.com/build/releases/agp-9-0-0-release-notes)、
[GradleのJava互換性](https://docs.gradle.org/9.3.0/userguide/compatibility.html)。
