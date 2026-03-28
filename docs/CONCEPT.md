# Project Overview

## 1. What Plexus Is

**Plexus** is a software execution platform built around tmux for AI agents and workers.

It starts with a terminal surface that makes tmux sessions reachable from mobile, and it is designed to grow into an orchestration surface where multiple workers can exchange and execute tasks safely.

Plexus is not just a mobile terminal app.
It treats tmux sessions, git worktrees, workers, and execution state as one runtime so that both humans and agents can connect to the same execution substrate.

---

## 2. Project Goal

### 2.1 Vision

**Build an AI runtime centered on tmux and reachable from mobile.**

If coding agents are going to write code seriously, run in parallel, and eventually delegate work to one another, they need a stable runtime underneath them.

Plexus is that runtime.

- tmux sits at the center of agent and worker execution
- git worktree provides physical isolation for parallel work
- terminal access and orchestration are treated as parts of the same runtime model
- humans can enter that runtime from mobile as well as desktop

### 2.2 Problems It Solves

| Problem | How Plexus solves it |
| --- | --- |
| **Execution environments are fragile** | tmux sessions provide continuity and reconnectable execution contexts |
| **Parallel work collides** | git worktree isolates work per attempt or worker |
| **Terminal access is desktop-first** | a mobile-accessible terminal surface connects to tmux sessions directly |
| **Future multi-worker coordination is hard to scale** | the runtime can grow into an orchestration surface with tasks, attempts, leases, and heartbeats |

---

## 3. Design Philosophy

### 3.1 tmux-Centered Runtime

Plexus treats tmux as more than a terminal multiplexer.
It is the runtime body of execution.

A session is not just a screen.
It is:

- the continuity point of a running task
- an attachable endpoint
- the place a worker lives
- a concrete execution object that orchestration can reason about

### 3.2 Terminal + Orchestration

Plexus does not treat terminal access and worker orchestration as unrelated systems.

- **Terminal Surface**: session list, websocket terminal, snapshot, mobile access
- **Orchestration Surface**: task, attempt, worker claim, lease, heartbeat, reconcile

They have different roles, but they support the same runtime.

### 3.3 Isolation by Worktree

To make parallel worker execution safe, Plexus treats git worktree as a first-class primitive.

Branches preserve the logical line of work.
Worktrees provide the physical boundary that prevents concurrent execution from colliding.

---

## 4. System Shape

Plexus consists of four main elements:

- **tmux session**: the center of execution
- **git worktree**: isolation for concurrent work
- **terminal surface**: the entry point for humans and mobile clients
- **orchestration surface**: the control plane for safe worker execution

This lets Plexus act as one execution platform rather than two unrelated tools.

---

## 5. Why The Name "Plexus"

The name **Plexus** was chosen because it suggests multiple things interwoven into one functioning system.

That fits both:

- tmux session management
- future worker orchestration

It also carries the meaning of a network or plexus, which matches a system where terminals, workers, and runtime state intersect.

Phonetically, it works well too:

- smooth and memorable to pronounce
- technical without being awkward
- `Plex` subtly echoes `Multiplexer (mux)`
