# AGENTS.md

## Project Coordination

This project is the Clinic Room Scheduling API. Work is coordinated by the Orchestrator, which routes requests through the appropriate specialist subagents and ensures the workflow is completed in order.

All new features, refactors, bug fixes, and improvements must be passed to the Orchestrator before work begins.

## Subagent Roles

- **Orchestrator**: Coordinates the full workflow, assigns work to subagents, tracks completion, and ensures the required gates are followed.
- **Git Guardian**: Handles all git operations, including syncing `main`, creating branches, checking status, committing, and pushing after approval.
- **Architect**: Analyzes requirements, system design, data flow, interfaces, and implementation approach before code changes begin.
- **TDD Tester**: Writes failing tests first for new features or missing coverage so implementation is guided by expected behavior.
- **Developer**: Implements the approved solution with minimal, focused code changes that satisfy the tests and requirements.
- **Security Analyst**: Reviews changes for security risks, unsafe data handling, authorization issues, dependency concerns, and misuse of sensitive information.
- **Tester**: Validates behavior when appropriate, runs final verification, and confirms the implementation meets the requested outcome.
- **Code Reviewer**: Performs the final quality gate, checking correctness, maintainability, regressions, and readiness for approval.

## End-To-End Workflow

- All new features, refactors, bug fixes, and improvements must be routed through the Orchestrator.
- Git Guardian syncs local `main` and prepares the working branch before implementation work starts.
- Architect reviews the request and defines the approach, constraints, and risks.
- TDD Tester writes failing tests first for new features or any missing coverage.
- Developer implements the change to satisfy the approved design and tests.
- Security Analyst reviews the change for security, privacy, and abuse-resistance concerns.
- Tester validates behavior when appropriate and performs final verification.
- Code Reviewer acts as the final gate before approval.
- `PLAN.md` must be synchronized when the workflow completes.
- When committing is required, Git Guardian commits and pushes only after approval.

## Git Rules

- All git operations must go through Git Guardian.
- No other subagent should run git commands.
- Branch creation, syncing, status checks, commits, and pushes are Git Guardian responsibilities only.

## Test-First Rules

- New features must start with failing tests before implementation.
- Missing coverage for bug fixes or refactors should be added before code changes when practical.
- Tests should describe observable behavior and protect against regressions.

## Security Review Rules

- Security review is required before final approval.
- Changes must avoid exposing secrets, sensitive data, or unsafe authorization paths.
- Inputs, outputs, authentication, authorization, and data access patterns should be reviewed for risk.
