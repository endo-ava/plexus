# セッション CRUD 設計

フロントエンドから tmux セッションの作成・削除を行うための機能設計。

関連:

- [ゲートウェイアーキテクチャ](./03-gateway/architecture.md)
- [ターミナル機能設計](./02-frontend/terminal.md)
- [システムアーキテクチャ](./01-overview/system-architecture.md)

## 概要

現状、ゲートウェイ API はセッションの読み取り（一覧・詳細・スナップショット）のみを提供し、
セッションの作成・削除は tmux コマンドの直接実行に依存している。
本設計ではゲートウェイ API にセッションの作成・削除エンドポイントを追加し、
フロントエンドから tmux セッションのライフサイクルを完結できるようにする。

## 設計方針

- tmux をセッションの真実ソースとし、DB は使用しない
- 既存の認証・バリデーションパターンに従う
- セッション名はユーザーの自由入力とし、空入力時は自動採番する
- 削除は長押しコンテキストメニューから確認 Dialog を経由して実行する

## アーキテクチャ

```text
┌───────────────────────────────────────────────────────┐
│  Frontend (Android)                                   │
│                                                       │
│  SessionList.kt                                       │
│    ヘッダー [+] アイコン → CreateSessionDialog         │
│                                                       │
│  SessionListItem.kt                                   │
│    長押し → ContextMenu(削除) → ConfirmDialog         │
│                                                       │
│  AgentListScreenModel                                 │
│    createSession(name?)  ──→ TerminalRepository       │
│    deleteSession(id)     ──→ TerminalRepository       │
│                                                       │
│  TerminalRepository                                   │
│    createSession(id, workingDir?)                     │
│    deleteSession(id)                                  │
│                                                       │
│  TerminalRepositoryImpl                               │
│    POST   /api/v1/terminal/sessions                   │
│    DELETE /api/v1/terminal/sessions/{id}              │
│                                                       │
│  RepositoryClient                                     │
│    delete() メソッド追加                              │
└───────────────────────┬───────────────────────────────┘
                        │ HTTP (X-API-Key)
                        ▼
┌───────────────────────────────────────────────────────┐
│  Gateway (Starlette)                                  │
│                                                       │
│  api/terminal.py                                      │
│    POST   /v1/terminal/sessions        → 201 Created  │
│    DELETE /v1/terminal/sessions/{id}   → 204 No Body  │
│                                                       │
│  infrastructure/tmux.py                               │
│    create_session(name, working_dir?)                  │
│    kill_session(name)                                 │
│                                                       │
│  domain/models.py                                     │
│    CreateSessionRequest(session_id, working_dir?)      │
└───────────────────────┬───────────────────────────────┘
                        │ subprocess
                        ▼
┌───────────────────────────────────────────────────────┐
│  tmux (source of truth)                               │
│    new-session -d -s <name> [-c <working_dir>]        │
│    kill-session -t =<name>                            │
└───────────────────────────────────────────────────────┘
```

---

## Gateway 変更

### 1. infrastructure/tmux.py — tmux 操作の追加

#### `create_session(session_name, working_dir=None) -> Session`

```text
tmux new-session -d -s {session_name} [-c {working_dir}]
```

- セッション作成後、`list_sessions()` から該当セッションを引いて返す
- 既存の `TMUX_COMMAND_TIMEOUT_SECONDS` とエラーハンドリングパターンに従う
- `FileNotFoundError` → `OSError("tmux is not installed")`
- `subprocess.CalledProcessError` → そのまま raise
- `subprocess.TimeoutExpired` → `OSError`

#### `kill_session(session_name) -> None`

```text
tmux kill-session -t ={session_name}
```

- `=` 付きで完全一致させる（前方一致による誤爆防止）
- `session_exists()` で事前確認は行わず、ハンドラ側で実施する
- セッションが存在しない場合の `CalledProcessError` はそのまま raise

### 2. domain/models.py — リクエストモデル追加

#### `CreateSessionRequest`

```python
class CreateSessionRequest(BaseModel):
    """セッション作成リクエスト。"""

    session_id: str = Field(..., min_length=1, max_length=100, description="セッション名")
    working_dir: str | None = Field(None, description="初期作業ディレクトリ")
```

レスポンスは既存の `_build_session_response()` の戻り値をそのまま使う。新規モデル不要。

### 3. api/terminal.py — ルート追加

#### `POST /v1/terminal/sessions` — セッション作成

処理フロー:

1. `verify_gateway_token(request)` で認証
2. `CreateSessionRequest` で request body をバリデーション
3. `_validate_session_id()` でセッション名を検証
4. `session_exists()` で重複チェック → `409 Conflict`
5. `anyio.to_thread.run_sync(create_session, ...)` で tmux セッション作成
6. `_build_session_response()` でレスポンス構築
7. `201 Created` を返す

エラーレスポンス:

| ステータス | 条件 | detail |
| --- | --- | --- |
| 400 | セッション名が不正 | `invalid_session_id: <reason>` |
| 409 | セッション名が重複 | `Session already exists: <name>` |
| 500 | tmux コマンド失敗 | `Failed to create session` |

#### `DELETE /v1/terminal/sessions/{session_id}` — セッション削除

処理フロー:

1. `verify_gateway_token(request)` で認証
2. `session_id` パスパラメータ取得 → `_validate_session_id()`
3. `session_exists()` で存在確認 → `404 Not Found`
4. `anyio.to_thread.run_sync(kill_session, session_id)` で tmux セッション削除
5. `204 No Content` を返す（body なし）

エラーレスポンス:

| ステータス | 条件 | detail |
| --- | --- | --- |
| 400 | セッション名が不正 | `invalid_session_id: <reason>` |
| 404 | セッション不在 | `Session not found` |
| 500 | tmux コマンド失敗 | `Failed to delete session` |

#### ルート定義への追加

```python
def get_terminal_routes() -> list[Route]:
    return [
        Route("/v1/terminal/sessions", get_sessions, methods=["GET"]),
        Route("/v1/terminal/sessions", create_session, methods=["POST"]),      # NEW
        Route("/v1/terminal/sessions/{session_id}", get_session, methods=["GET"]),
        Route("/v1/terminal/sessions/{session_id}", delete_session, methods=["DELETE"]),  # NEW
        Route(...),  # ws-token, snapshot, websocket (既存)
    ]
```

---

## Frontend 変更

### 1. RepositoryClient — delete メソッド追加

既存の `get()` / `post()` / `put()` と同じパターンで `delete<T>(path)` を追加する。

### 2. TerminalRepository — interface 追加

```kotlin
suspend fun createSession(sessionId: String, workingDir: String? = null): RepositoryResult<Session>
suspend fun deleteSession(sessionId: String): RepositoryResult<Unit>
```

### 3. TerminalRepositoryImpl — 実装追加

```kotlin
override suspend fun createSession(sessionId: String, workingDir: String?): RepositoryResult<Session> =
    wrapRepositoryOperation {
        repositoryClient.post<Session>("/api/v1/terminal/sessions",
            body = CreateSessionRequest(sessionId, workingDir))
    }

override suspend fun deleteSession(sessionId: String): RepositoryResult<Unit> =
    wrapRepositoryOperation {
        repositoryClient.delete<Unit>("/api/v1/terminal/sessions/${sessionId.encodeURLPathPart()}")
    }
```

成功時に `sessionsCache` と `sessionCache` をクリアする。

### 4. AgentListState — フィールド追加

```kotlin
data class AgentListState(
    val sessions: List<Session> = emptyList(),
    val isLoadingSessions: Boolean = false,
    val sessionsError: String? = null,
    val isCreatingSession: Boolean = false,         // NEW
    val deletingSessionIds: Set<String> = emptySet(), // NEW
)
```

### 5. AgentListEffect — 追加

```kotlin
sealed class AgentListEffect {
    data class ShowError(val message: String) : AgentListEffect()
    data class NavigateToSession(val sessionId: String) : AgentListEffect()
    data class SessionCreated(val session: Session) : AgentListEffect()  // NEW
    data class SessionDeleted(val sessionId: String) : AgentListEffect() // NEW
}
```

### 6. AgentListScreenModel — メソッド追加

#### `createSession(sessionId: String)`

1. `_state.update { isCreatingSession = true }`
2. `terminalRepository.createSession(sessionId)` 呼び出し
3. 成功 → `isCreatingSession = false`, `SessionCreated` effect, `loadSessions()`
4. 失敗 → `isCreatingSession = false`, `ShowError` effect

#### `deleteSession(sessionId: String)`

1. `_state.update { deletingSessionIds + sessionId }`
2. `terminalRepository.deleteSession(sessionId)` 呼び出し
3. 成功 → `deletingSessionIds - sessionId`, `SessionDeleted` effect, `loadSessions()`
4. 失敗 → `deletingSessionIds - sessionId`, `ShowError` effect

#### `suggestSessionName(): String`

既存セッションから `session-\d+` にマッチするものの最大値を取得し、+1 した値を返す。
マッチするものがない場合は `session-01` を返す。

### 7. UI 変更

#### ヘッダー: 追加ボタン

`SessionList.kt` のヘッダー行に `Icons.Default.Add` を追加する。

```text
Before:  [>_Terminal] SESSIONS          [↻] [⚙]
After:   [>_Terminal] SESSIONS      [+] [↻] [⚙]
```

- `isCreatingSession = true` の間は disabled にする

#### 作成 Dialog

`[+]` タップ時に `AlertDialog` を表示する。

```text
┌── New Session ──────────────────┐
│                                  │
│  Session name                    │
│  ┌────────────────────────────┐ │
│  │                            │ │
│  └────────────────────────────┘ │
│  placeholder: session-03        │
│                                  │
│          ┌────────┐ ┌────────┐  │
│          │ Cancel │ │ Create │  │
│          └────────┘ └────────┘  │
└──────────────────────────────────┘
```

- テキストフィールドは任意入力
- プレースホルダーに `suggestSessionName()` の結果を表示
- 空入力で Create → プレースホルダーの自動採番名で作成
- 名前入力で Create → 入力値をそのまま使用
- `isCreatingSession = true` の間は Create ボタンを loading 状態にする

#### 削除: 長押しコンテキストメニュー

`SessionListItem` を長押しで `DropdownMenu` を表示する。

```text
┌── session-01 ────────────┐
│  preview...              │  ← 長押し
│                          │
│              ┌───────────┤
│              │ 🗑 削除    │  ← ドロップダウンメニュー
│              └───────────┤
└──────────────────────────┘
```

「削除」選択 → 確認 `AlertDialog` → `deleteSession()` 実行。

- `deletingSessionIds` に含まれるセッションはカード全体を disabled 表示にする

---

## 変更ファイル一覧

### Gateway

| ファイル | 変更内容 |
| --- | --- |
| `gateway/infrastructure/tmux.py` | `create_session()`, `kill_session()` を追加 |
| `gateway/domain/models.py` | `CreateSessionRequest` を追加 |
| `gateway/api/terminal.py` | `POST`/`DELETE` ルート + ハンドラを追加 |
| `gateway/tests/unit/` | 各ハンドラのユニットテストを追加 |

### Frontend

| ファイル | 変更内容 |
| --- | --- |
| `RepositoryClient.kt` | `delete()` メソッドを追加 |
| `TerminalRepository.kt` | `createSession()`, `deleteSession()` を interface に追加 |
| `TerminalRepositoryImpl.kt` | 上記の実装を追加 |
| `AgentListState.kt` | `isCreatingSession`, `deletingSessionIds` を追加 |
| `AgentListEffect.kt` | `SessionCreated`, `SessionDeleted` を追加 |
| `AgentListScreenModel.kt` | `createSession()`, `deleteSession()`, `suggestSessionName()` を追加 |
| `AgentListScreen.kt` | `SessionCreated`/`SessionDeleted` effect 処理を追加 |
| `SessionList.kt` | ヘッダーに `[+]` アイコンを追加、Dialog 表示ロジックを追加 |
| `SessionListItem.kt` | 長押しコンテキストメニューを追加 |

---

## API 仕様

### POST /api/v1/terminal/sessions

セッションを作成する。

Request:

```json
{
  "session_id": "my-project",
  "working_dir": "/root/workspace"
}
```

| フィールド | 型 | 必須 | 説明 |
| --- | --- | --- | --- |
| `session_id` | string | ○ | セッション名（1〜100文字） |
| `working_dir` | string | - | 初期作業ディレクトリ |

Response `201`:

```json
{
  "session_id": "my-project",
  "name": "my-project",
  "last_activity": "2026-04-18T10:30:00+00:00",
  "created_at": "2026-04-18T10:30:00+00:00",
  "status": "connected",
  "preview_available": false,
  "preview_lines": [],
  "title": null,
  "current_path": "/root/workspace"
}
```

### DELETE /api/v1/terminal/sessions/{session_id}

セッションを削除する。

Response `204`: body なし

---

## セッション名の採番ロジック

フロントエンド側で以下のロジックにより自動採番名を生成する。

1. 現在のセッション一覧から `session-\d+` にマッチするものを抽出
2. マッチしたものの数値部分の最大値を取得
3. 最大値 + 1 をゼロ埋め2桁でフォーマット（`session-03`）
4. マッチするものがない場合は `session-01`

ユーザーが名前を入力した場合はその値をそのまま使用する。
入力値のバリデーションはゲートウェイ側の `_validate_session_id()` に委ねる。
