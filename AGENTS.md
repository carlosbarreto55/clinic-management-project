# AGENTS.md

## Project Context

This repository contains the Clinic Room Scheduling API. All work for new features, refactors, bug fixes, and improvements is coordinated through the Orchestrator to keep planning, execution, verification, and documentation aligned.

## Subagent Roles

### Orchestrator

- Coordinates all non-trivial work across the project.
- Receives every request for new features, refactors, bug fixes, and improvements.
- Defines the workflow, delegates tasks to the correct subagents, and ensures each required gate is completed.
- Confirms that `PLAN.md` is synchronized when the workflow completes.

### Git Guardian

- Owns all git operations for the repository.
- Handles branch setup before work begins.
- Performs commits, pushes, and other git commands after approval when committing is required.
- Ensures no other subagent runs git commands.

### Architect

- Analyzes requirements, existing architecture, dependencies, and design constraints.
- Proposes the technical approach before implementation begins.
- Identifies risks, integration points, and required changes.

### TDD Tester

- Writes failing tests first for new features and missing coverage.
- Defines expected behavior before implementation.
- Ensures tests are meaningful, focused, and aligned with the requested change.

### Developer

- Implements approved changes after planning and test definition.
- Keeps changes minimal, maintainable, and aligned with the existing codebase.
- Fixes issues found during testing, security review, or code review.

### Security Analyst

- Reviews changes for security risks before final approval.
- Checks authentication, authorization, data validation, sensitive data handling, and dependency-related risks.
- Flags vulnerabilities or unsafe patterns that must be resolved before release.

### Tester

- Validates behavior when appropriate through automated and manual verification.
- Confirms bug fixes, regressions, edge cases, and integration behavior.
- Performs final verification before code review when required.

### Code Reviewer

- Acts as the final quality gate before completion.
- Reviews correctness, maintainability, test coverage, security concerns, and consistency with project conventions.
- Approves the work only after required issues are resolved.

## End-to-End Workflow

For all new features, refactors, bug fixes, and improvements:

- The request must first be passed to the Orchestrator.
- Git Guardian sets up the working branch before implementation begins.
- Architect analyzes the request, system impact, and technical approach.
- TDD Tester writes failing tests first for new features or missing coverage.
- Developer implements the approved change and makes the tests pass.
- Security Analyst reviews the change for security risks.
- Tester validates behavior when appropriate and performs final verification.
- Code Reviewer performs the final review gate.
- Orchestrator ensures `PLAN.md` is synchronized when the workflow completes.
- Git Guardian commits and pushes after approval when committing is required.

## Git Rules

- All git operations must go through Git Guardian.
- No other subagent should run git commands.
- Branch creation, status checks, diffs, commits, pushes, merges, rebases, and any other git command are Git Guardian responsibilities.

## Test-First Rules

- New features must start with failing tests written by TDD Tester whenever practical.
- Missing coverage for bug fixes, refactors, and improvements should be added before implementation when practical.
- Tests should describe expected behavior, edge cases, and regressions clearly.

## Security Review Rules

- Security Analyst review is required before final approval.
- Changes touching authentication, authorization, validation, persistence, external integrations, or sensitive data require careful security review.
- Security issues must be resolved before Code Reviewer final approval.
