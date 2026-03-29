# Plexus

Plexus is a tmux-centered runtime platform for AI agents and workers.

Concept documents:

- English: `docs/CONCEPT.md`
- Japanese: `docs/CONCEPT.ja.md`
- Architecture: `docs/10.architecture/`

Plexus is built around two connected surfaces:

- `gateway/`: runtime API for tmux session list, snapshot, websocket attach, push, and webhook handling
- `frontend/`: Android terminal client used to access the runtime from mobile

## Scope

Plexus owns runtime-oriented capabilities:

- terminal access
- tmux session lifecycle
- runtime-facing push notifications
- worker execution control
- future orchestration surfaces

## Repository Layout

```text
plexus/
├── docs/        # runtime, terminal, FCM, webhook, and orchestration notes
├── frontend/    # Android terminal client (KMP + Compose Multiplatform)
├── gateway/     # Starlette-based runtime API
└── maestro/     # E2E flows for terminal UI
```

## Development

### Gateway

```bash
cd /root/workspace/plexus/gateway
uv run python -m gateway.main
```

### Frontend

```bash
cd /root/workspace/plexus/frontend
./gradlew :androidApp:assembleDebug
```

### Tests

```bash
cd /root/workspace/plexus/gateway
uv run pytest tests -v

cd /root/workspace/plexus/frontend
./gradlew :shared:testDebugUnitTest
```
