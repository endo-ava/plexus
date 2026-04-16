#!/usr/bin/env bash
# plexus-gateway systemd サービスのインストール / アンインストール / ステータス確認
#
# 使い方:
#   ./install.sh install [WORKDIR]   # systemd に登録して起動
#   ./install.sh uninstall          # 停止して登録解除
#   ./install.sh status             # サービス状態を表示

set -euo pipefail

# sudo の secure_path で ~/.local/bin が消えるのを防ぐ
export PATH="$HOME/.local/bin:$PATH"

SERVICE_NAME="plexus-gateway"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
WORKDIR_DEFAULT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
SERVICE_FILE="/etc/systemd/system/${SERVICE_NAME}.service"
TEMPLATE="${SCRIPT_DIR}/${SERVICE_NAME}.service.tmpl"

# ── ユーティリティ ──────────────────────────────────────────────

info()  { printf "\033[1;34m[INFO]\033[0m  %s\n" "$*"; }
ok()    { printf "\033[1;32m[OK]\033[0m    %s\n" "$*"; }
warn()  { printf "\033[1;33m[WARN]\033[0m  %s\n" "$*"; }
error() { printf "\033[1;31m[ERROR]\033[0m %s\n" "$*" >&2; exit 1; }

check_root() {
  if [[ $EUID -ne 0 ]]; then
    error "この操作には root 権限が必要です。sudo で実行してください。"
  fi
}

find_uv() {
  local uv_path
  uv_path="$(command -v uv 2>/dev/null || true)"
  if [[ -z "$uv_path" ]]; then
    error "uv が見つかりません。https://docs.astral.sh/uv/ からインストールしてください。"
  fi
  printf "%s" "$uv_path"
}

# ── テンプレート描画 ────────────────────────────────────────────

render_template() {
  local workdir="$1"
  local uv_path="$2"
  local user="${SUDO_USER:-$(whoami)}"
  local group

  group="$(id -gn "$user" 2>/dev/null || printf "%s" "$user")"

  sed \
    -e "s|__WORKDIR__|${workdir}|g" \
    -e "s|__UV__|${uv_path}|g" \
    -e "s|__USER__|${user}|g" \
    -e "s|__GROUP__|${group}|g" \
    "$TEMPLATE"
}

# ── サブコマンド ─────────────────────────────────────────────────

do_install() {
  local workdir="${1:-${WORKDIR_DEFAULT}}"
  local uv_path

  check_root

  # バリデーション
  if [[ ! -d "$workdir" ]]; then
    error "${workdir} が見つかりません。WORKDIR を確認してください。"
  fi
  if [[ ! -f "$workdir/gateway/main.py" ]]; then
    error "${workdir}/gateway/main.py が見つかりません。"
  fi
  if [[ ! -f "$TEMPLATE" ]]; then
    error "テンプレートファイルが見つかりません: ${TEMPLATE}"
  fi

  uv_path="$(find_uv)"

  info "Plexus Gateway を systemd にインストールします"
  info "  WorkDir: ${workdir}"
  info "  uv:      ${uv_path}"
  info "  User:    ${SUDO_USER:-$(whoami)}"
  echo ""

  # 既存サービスの停止（冪等）
  if systemctl is-active --quiet "$SERVICE_NAME" 2>/dev/null; then
    info "既存サービスを停止します..."
    systemctl stop "$SERVICE_NAME"
  fi

  # テンプレート描画 & 配置
  render_template "$workdir" "$uv_path" > "$SERVICE_FILE"
  ok "サービスファイルを配置: ${SERVICE_FILE}"

  # systemd 再読込 & 有効化
  systemctl daemon-reload
  systemctl enable "$SERVICE_NAME"
  ok "自動起動を有効化しました"

  # 起動
  systemctl start "$SERVICE_NAME"
  sleep 1

  # 起動確認
  if systemctl is-active --quiet "$SERVICE_NAME"; then
    ok "サービスが起動しました"
  else
    warn "サービスが起動していない可能性があります。ログを確認してください:"
    echo "  journalctl -u ${SERVICE_NAME} -n 20 --no-pager"
  fi

  echo ""
  info "コマンド一覧:"
  echo "  sudo systemctl status ${SERVICE_NAME}     # 状態確認"
  echo "  sudo journalctl -u ${SERVICE_NAME} -f     # ログ追跡"
  echo "  sudo systemctl restart ${SERVICE_NAME}    # 再起動"
  echo "  sudo ${SCRIPT_DIR}/install.sh uninstall   # アンインストール"
}

do_uninstall() {
  check_root

  info "Plexus Gateway をアンインストールします"

  if [[ -f "$SERVICE_FILE" ]]; then
    systemctl disable --now "$SERVICE_NAME" 2>/dev/null || true
    ok "自動起動を無効化しました"
    rm -f "$SERVICE_FILE"
    ok "サービスファイルを削除しました: ${SERVICE_FILE}"
  else
    info "サービスファイルは存在しません"
  fi

  systemctl daemon-reload
  systemctl reset-failed "$SERVICE_NAME" 2>/dev/null || true
  ok "systemd を再読込しました"

  ok "アンインストール完了"
}

do_status() {
  echo "Service: ${SERVICE_NAME}"
  echo "Unit:    ${SERVICE_FILE}"
  echo ""

  if [[ ! -f "$SERVICE_FILE" ]]; then
    warn "サービスファイルが存在しません（インストール未実行）"
    exit 0
  fi

  systemctl status "$SERVICE_NAME" --no-pager 2>/dev/null || true
}

# ── メイン ───────────────────────────────────────────────────────

usage() {
  echo "使い方: $0 {install [WORKDIR]|uninstall|status}"
  echo ""
  echo "  install [WORKDIR]   systemd サービスを登録して起動する"
  echo "                      WORKDIR 省略時はスクリプトの親ディレクトリ"
  echo "  uninstall           サービスを停止・削除する"
  echo "  status              サービス状態を表示する"
  echo ""
  echo "例:"
  echo "  sudo ./install.sh install"
  echo "  sudo ./install.sh install /opt/plexus"
  echo "  sudo ./install.sh uninstall"
  echo "  ./install.sh status"
}

cmd="${1:-}"
case "$cmd" in
  install)
    do_install "${2:-}"
    ;;
  uninstall)
    do_uninstall
    ;;
  status)
    do_status
    ;;
  *)
    usage
    exit 1
    ;;
esac
