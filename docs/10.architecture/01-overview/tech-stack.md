# Plexus 技術スタック

## 概要

Plexus はランタイム向けバックエンドと Android ターミナルクライアントを組み合わせた構成を取る。
コンポーネントごとの技術選定を整理する。

## コンポーネント別スタック

| コンポーネント | 主要技術 | 役割 |
| --- | --- | --- |
| `gateway/` | Python, Starlette, Uvicorn, WebSocket, libtmux | tmux ランタイムを API / WebSocket として公開する |
| `frontend/` | Kotlin Multiplatform, Compose Multiplatform, Voyager, Koin, Ktor | モバイルターミナルクライアントを実装する |
| ターミナル描画 | xterm.js, WebView | ターミナル画面を Android 上で描画する |
| プッシュ通知 | Firebase Cloud Messaging | ランタイム向けプッシュ通知を扱う |
| 実行基盤 | tmux | 実行継続とアタッチの基盤 |

## フロントエンド

Kotlin Multiplatform + Compose Multiplatform ベースの Android ターミナルクライアント。

| カテゴリ | 技術 |
| --- | --- |
| 言語 | Kotlin 2.2.21 |
| UI | Compose Multiplatform 1.9.0 |
| ナビゲーション / ScreenModel | Voyager 1.1.0-beta03 |
| DI | Koin 4.0.0 |
| HTTP | Ktor 3.3.3 |
| ロギング | Kermit |
| 非同期 | Kotlin Coroutines + Flow |

## ゲートウェイ

tmux セッションランタイムをフロントエンドから扱える形に変換する層。

| カテゴリ | 技術 |
| --- | --- |
| 言語 | Python 3.12+ |
| API | Starlette |
| サーバー | Uvicorn |
| ターミナル連携 | libtmux |
| リアルタイム通信 | WebSocket |

## 設計メモ

- tmux は単なるターミナルバックエンドではなく、実行基盤として扱う
- フロントエンドとゲートウェイは同じランタイムを別視点で扱うため、通信と UI を分離する
- xterm.js はターミナル描画に限定し、ランタイム状態はゲートウェイ API 経由で取得する

## 関連ドキュメント

- [システムアーキテクチャ](./system-architecture.md)
- [フロントエンドアーキテクチャ](../02-frontend/architecture.md)
- [ゲートウェイアーキテクチャ](../03-gateway/architecture.md)
