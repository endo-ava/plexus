# ターミナル機能設計

関連:

- [フロントエンドアーキテクチャ](./architecture.md)
- [システムアーキテクチャ](../01-overview/system-architecture.md)

## 画面構成

セッション一覧画面（`AgentListScreen`）とターミナル画面（`TerminalScreen`）の 2 画面で構成する。

---

## AgentListScreen: セッション一覧

### 起動時

1. ゲートウェイ API からアクティブな tmux セッション一覧を取得する
2. セッション名、ID、ステータスを表示する

### 操作

- セッション選択 → `TerminalScreen` へ遷移（セッション ID を渡す）
- リフレッシュボタン → セッション一覧を再取得
- ゲートウェイ設定ボタン → `GatewaySettings` 画面へ遷移

---

## TerminalScreen: ターミナルセッション

### 接続フロー

1. 画面表示時、WebSocket URL と API キーを設定から取得する
2. WebView 内の xterm.js が WebSocket 接続を確立する
3. 接続状態を `Flow<Boolean>` で監視する
4. 切断時は自動再接続を試みる

### キーボード対応

- ソフトウェアキーボード表示時、画面下部の入力欄へフォーカスする
- `SpecialKeysBar` で特殊キー（Ctrl, Alt, Tab, Esc, 矢印キー）を送信する
- モバイルで通常入力できないキーを補完する

### エラーハンドリング

- 接続エラー時、ヘッダーにエラーメッセージを表示する
- ゲートウェイ URL / API キーが未設定の場合は `GatewaySettings` へ誘導する
