# Plexus

Plexus is the dedicated runtime repository extracted from EgoGraph.

Concept documents:

- English: `docs/CONCEPT.md`
- Japanese: `docs/CONCEPT.ja.md`

It currently bootstraps the two surfaces needed for parallel run migration:

- `gateway/`: terminal runtime API for tmux session list, snapshot, websocket attach, and push
- `frontend/`: Android terminal client used to connect to the runtime from mobile

## Scope

Plexus owns runtime-oriented capabilities:

- terminal access
- tmux session lifecycle
- runtime-facing push notifications
- future worker orchestration surfaces

Plexus does not own:

- personal data ingestion
- chat and reasoning backend
- RAG and memory features
- channel adapters such as Discord

## Repository Layout

```text
plexus/
├── docs/        # runtime and terminal design notes
├── frontend/    # Android terminal client (KMP + Compose Multiplatform)
├── gateway/     # Starlette-based terminal runtime API
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

## Migration Note

This repository is the Phase A-D bootstrap for parallel run migration from EgoGraph.
The old implementation in EgoGraph is intentionally kept for now and will be removed in a later phase.
