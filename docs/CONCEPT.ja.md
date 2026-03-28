# プロジェクト概要

## 1. Plexusとは

**Plexus** は、tmux を中心に AI エージェントや Worker を動かすためのソフトウェア実行基盤です。

モバイルから tmux session にアクセスできる terminal surface を持ちつつ、将来的には複数 Worker が task を受け渡しながら動く orchestration surface まで含むことを前提にしています。

Plexus が目指すのは、単なる「モバイル terminal アプリ」ではありません。
tmux session、git worktree、Worker、task 実行状態をひとつの runtime として扱い、人間もエージェントも同じ execution substrate に接続できるようにすることです。

---

## 2. プロジェクトの目的

### 2.1 ビジョン

**「tmux を中心に、モバイルからもアクセスできる AI 実行基盤を作る」**

AI エージェントが本格的にコードを書き、並列に作業し、将来的には互いに仕事を委譲するなら、背後には安定した runtime が必要です。

Plexus はその runtime を担います。

- tmux を agent / Worker 実行の中心に据える
- git worktree により並列作業を物理的に分離する
- terminal access と orchestration を同じ runtime 思想で扱う
- 人間はモバイルからでもその runtime に入れる

### 2.2 解決する課題

| 課題 | Plexusによる解決 |
| --- | --- |
| **実行環境が脆い** | tmux session を中心に、再接続可能で継続性のある実行環境を作る |
| **並列作業が衝突する** | git worktree を使い、attempt / Worker ごとに作業領域を分離する |
| **端末アクセスが desktop 前提** | モバイルからも tmux session に接続できる terminal surface を提供する |
| **将来の複数 Worker 協調が難しい** | task / attempt / lease / heartbeat を持つ orchestration surface へ拡張できる構造にする |

---

## 3. 基本方針 (Design Philosophy)

### 3.1 tmux-Centered Runtime

Plexus は、tmux を単なる terminal multiplexer ではなく、AI 実行の中核となる runtime body として扱います。

session はただの画面ではなく、

- 実行の継続点
- attach 可能な接続先
- Worker の生存場所
- orchestration と接続できる実体

として機能します。

### 3.2 Terminal + Orchestration

Plexus では terminal access と worker orchestration を別物として断絶させません。

- **Terminal Surface**: session list, websocket terminal, snapshot, mobile access
- **Orchestration Surface**: task, attempt, worker claim, lease, heartbeat, reconcile

両者は役割は違っても、同じ runtime を支える surface です。

### 3.3 Isolation by Worktree

複数の Worker が安全に並列作業するために、Plexus は git worktree を重要な primitive として扱います。

branch は論理的な成果物を保ち、worktree は物理的な作業衝突を避けるための境界になります。

---

## 4. システム像

Plexus は大きく次の要素で構成されます。

- **tmux session**: 実行の中心
- **git worktree**: 並列作業の分離
- **terminal surface**: 人間やモバイルが runtime に入る入口
- **orchestration surface**: Worker が task を安全に実行するための control plane

これにより、Plexus は「端末に繋ぐ仕組み」と「複数 Worker を動かす仕組み」を別々ではなく、ひとつの実行基盤として扱います。

---

## 5. 名前の由来

**Plexus** という名前には、複数のものが絡み合って機能する、という意味を込めています。

- tmux session 管理と将来の Worker orchestration の両方に合う
- 端末や Worker が交差する network のイメージに重なる
- 「網状組織」「神経叢」の意味合いが、接続された runtime という性格に合う

音の面でも、

- 「プレクサス」と発音しやすい
- 技術的でありながら記憶しやすい
- `Plex` が `Multiplexer (mux)` を連想させる

という理由から、この名前を採用しています。
