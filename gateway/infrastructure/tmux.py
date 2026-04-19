"""tmux インテグレーション。

tmux セッションの列挙と管理機能を提供します。
"""

import logging
import subprocess
from dataclasses import dataclass
from datetime import datetime, timezone

logger = logging.getLogger(__name__)

TMUX_COMMAND_TIMEOUT_SECONDS = 5


class SessionNotFoundError(Exception):
    """tmux セッションが見つからない場合のエラー。"""


@dataclass(frozen=True, slots=True)
class Session:
    """tmux セッション情報。

    Attributes:
        name: セッション名（例: agent-0001）
        last_activity: 最終アクティビティ日時
        created_at: 作成日時
    """

    name: str
    last_activity: datetime
    created_at: datetime


def list_sessions() -> list[Session]:
    """tmux セッション一覧を取得します。

    `tmux list-sessions` コマンドを使用してセッション情報を取得します。
    存在するすべてのセッションを返します。

    Returns:
        セッション情報のリスト

    Raises:
        subprocess.CalledProcessError: tmux コマンドが失敗した場合
        OSError: tmux がインストールされていない場合
    """
    try:
        # tmux list-sessions の実行
        # フォーマット: session_name\tsession_activity\tsession_created
        result = subprocess.run(
            [
                "tmux",
                "list-sessions",
                "-F",
                "#{session_name}\t#{session_activity}\t#{session_created}",
            ],
            capture_output=True,
            text=True,
            check=True,
            timeout=TMUX_COMMAND_TIMEOUT_SECONDS,
        )
    except subprocess.CalledProcessError as e:
        # セッションが存在しない場合は空リストを返す（終了ステータス 1）
        if e.returncode == 1 and not e.stdout:
            logger.debug("No tmux sessions found")
            return []
        raise
    except FileNotFoundError as e:
        logger.error("tmux command not found")
        raise OSError("tmux is not installed") from e
    except subprocess.TimeoutExpired as e:
        logger.error("tmux list-sessions timed out: %s", e)
        raise OSError("tmux list-sessions timed out") from e

    sessions: list[Session] = []

    for line in result.stdout.strip().split("\n"):
        if not line:
            continue

        parts = line.split("\t")
        if len(parts) != 3:
            logger.warning("Invalid tmux output format: %s", line)
            continue

        name, activity_str, created_str = parts

        # タイムスタンプのパース
        try:
            # tmux のタイムスタンプ形式に対応
            # フォーマット例: "2025-02-08T12:34:56"
            last_activity = _parse_tmux_timestamp(activity_str)
            created_at = _parse_tmux_timestamp(created_str)
        except ValueError as e:
            logger.warning(
                "Failed to parse timestamp for session %s: %s",
                name,
                e,
            )
            try:
                created_at = _parse_tmux_timestamp(created_str)
                last_activity = created_at
            except ValueError:
                fallback_now = datetime.now(tz=timezone.utc)
                logger.warning(
                    "Falling back to current time for session %s due to timestamp errors",
                    name,
                )
                created_at = fallback_now
                last_activity = fallback_now

        sessions.append(
            Session(
                name=name,
                last_activity=last_activity,
                created_at=created_at,
            )
        )

    logger.info("Found %d matching tmux sessions", len(sessions))
    return sessions


def _parse_tmux_timestamp(timestamp_str: str) -> datetime:
    """tmux のタイムスタンプ文字列をパースします。

    Args:
        timestamp_str: タイムスタンプ文字列

    Returns:
        パースされた datetime オブジェクト

    Raises:
        ValueError: パースに失敗した場合
    """
    # tmux の UNIX epoch 秒形式に対応
    # フォーマット例: "1770648271"
    if timestamp_str.isdigit():
        return datetime.fromtimestamp(int(timestamp_str), tz=timezone.utc)

    # tmux のタイムスタンプ形式に対応
    # フォーマット例: "2025-02-08T12:34:56"
    for fmt in (
        "%Y-%m-%dT%H:%M:%S",
        "%Y-%m-%d %H:%M:%S",
        "%Y-%m-%dT%H:%M:%S.%f",
    ):
        try:
            dt = datetime.strptime(timestamp_str, fmt)
            return dt.replace(tzinfo=timezone.utc)
        except ValueError:
            continue

    raise ValueError(f"Unsupported timestamp format: {timestamp_str}")


def session_exists(session_name: str) -> bool:
    """指定された名前の tmux セッションが存在するか確認します。

    Args:
        session_name: セッション名

    Returns:
        セッションが存在する場合は True、そうでない場合は False
    """
    try:
        target = f"={session_name}"
        subprocess.run(
            ["tmux", "has-session", "-t", target],
            capture_output=True,
            check=True,
            timeout=TMUX_COMMAND_TIMEOUT_SECONDS,
        )
        return True
    except subprocess.CalledProcessError:
        return False
    except (FileNotFoundError, subprocess.TimeoutExpired):
        return False


def create_session(session_name: str, working_dir: str | None = None) -> Session:
    """tmux セッションを作成します。

    Args:
        session_name: セッション名
        working_dir: セッションの作業ディレクトリ（省略時はtmuxのデフォルト）

    Returns:
        作成されたセッション情報

    Raises:
        SessionNotFoundError: セッション作成後に list_sessions で見つからない場合
        subprocess.CalledProcessError: tmux コマンドが失敗した場合
        OSError: tmux がインストールされていない、またはタイムアウトした場合
    """
    try:
        cmd: list[str] = ["tmux", "new-session", "-d", "-s", session_name]
        if working_dir is not None:
            cmd.extend(["-c", working_dir])
        subprocess.run(
            cmd,
            capture_output=True,
            check=True,
            timeout=TMUX_COMMAND_TIMEOUT_SECONDS,
        )
    except FileNotFoundError as e:
        raise OSError("tmux is not installed") from e
    except subprocess.TimeoutExpired as e:
        raise OSError("tmux new-session timed out") from e

    sessions = list_sessions()
    for session in sessions:
        if session.name == session_name:
            return session

    raise SessionNotFoundError(f"Session '{session_name}' not found after creation")


def kill_session(session_name: str) -> None:
    """tmux セッションを削除します。

    Args:
        session_name: セッション名

    Raises:
        subprocess.CalledProcessError: tmux コマンドが失敗した場合
        OSError: tmux がインストールされていない、またはタイムアウトした場合
    """
    try:
        subprocess.run(
            ["tmux", "kill-session", "-t", f"={session_name}"],
            capture_output=True,
            check=True,
            timeout=TMUX_COMMAND_TIMEOUT_SECONDS,
        )
    except FileNotFoundError as e:
        raise OSError("tmux is not installed") from e
    except subprocess.TimeoutExpired as e:
        raise OSError("tmux kill-session timed out") from e


def get_active_pane_metadata(session_name: str) -> tuple[str | None, str | None]:
    """指定セッションのアクティブ pane から title と current_path を取得する。"""
    try:
        result = subprocess.run(
            [
                "tmux",
                "list-panes",
                "-t",
                session_name,
                "-F",
                "#{pane_active}\t#{pane_title}\t#{pane_current_path}",
            ],
            capture_output=True,
            text=True,
            check=True,
            timeout=TMUX_COMMAND_TIMEOUT_SECONDS,
        )
    except (
        subprocess.CalledProcessError,
        FileNotFoundError,
        subprocess.TimeoutExpired,
    ) as e:
        logger.debug("Failed to list tmux panes for %s: %s", session_name, e)
        return (None, None)

    fallback: tuple[str | None, str | None] | None = None
    for raw_line in result.stdout.splitlines():
        if not raw_line:
            continue

        pane_active, separator, remainder = raw_line.partition("\t")
        if not separator:
            continue

        title, separator, current_path = remainder.partition("\t")
        if not separator:
            current_path = ""

        metadata = (title or None, current_path or None)
        if fallback is None:
            fallback = metadata
        if pane_active == "1":
            return metadata

    return fallback or (None, None)
