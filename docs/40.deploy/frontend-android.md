# Frontend Deploy (Android)

Plexus の Android アプリをビルドし、内部配布用 debug APK を作成する手順。
Kotlin Multiplatform + Compose Multiplatform を使用し、Android ネイティブアプリとして実機確認に使う。

## 1. 前提条件

- **JDK**: 17 以上（推奨: JDK 21）
- **Android SDK**: API 34（コマンドラインツール）
- **Gradle**: 8.8+ (Wrapper 同梱)

### 1.1 Android SDK セットアップ

Android Studio を使わずに CLI で開発する場合:

```bash
# Android SDK Command-line Tools をダウンロード
# https://developer.android.com/studio#command-tools

# SDK マネージャーで必要なパッケージをインストール
sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"

# 環境変数設定
export ANDROID_HOME=$HOME/Android/Sdk
export PATH=$PATH:$ANDROID_HOME/platform-tools
```

## 2. ビルド手順

### 2.1 デバッグビルド

```bash
cd frontend
./gradlew :androidApp:assembleDebug
# 成果物: androidApp/build/outputs/apk/debug/androidApp-debug.apk
```

### 2.2 内部配布向け署名付き debug ビルド

内部配布で使う debug APK に独自キーストアを使う場合の手順。

#### A. キーストア作成（初回のみ）

```bash
keytool -genkey -v \
  -keystore debug.keystore \
  -alias plexus \
  -keyalg RSA -keysize 2048 -validity 10000
```

#### B. ビルド実行

環境変数を設定してビルドします。

```bash
export KEYSTORE_PASSWORD="your-password"
export KEY_PASSWORD="your-password"

./gradlew :androidApp:assembleDebug
# 成果物: androidApp/build/outputs/apk/debug/androidApp-debug.apk
```

## 3. インストール

### デバイスへのインストール

```bash
./gradlew :androidApp:installDebug
```

## 4. CI/CD

`ci-frontend.yml` で自動テストと debug ビルドを行い、internal debug APK publish workflow で実機確認用 APK を配布できます。

内部配布用 artifact は production release ではありません。用途を明示したうえで GitHub pre-release へ配置してください。
