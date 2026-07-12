# AGENTS.md

## Response Rules
- Always respond in Korean.
- Put the conclusion first.
- Keep explanations concise.
- Do not make broad refactors without proposing a plan first.
- Prefer small, reviewable changes.
- Do not change API behavior unless explicitly requested.
- Do not introduce new dependencies without asking.
- Follow existing package structure and code style.

## Repository Rule
- If this document conflicts with `build.gradle.kts` or existing code, follow the repository code first.
- First inspect existing Controller, Service, Repository, DTO, Entity, Security config before editing.
- Avoid unrelated formatting changes.

## Tech Stack
- Java 21
- Spring Boot
- Gradle Kotlin DSL
- SpringDoc OpenAPI
- Apache PDFBox
- PostgreSQL via Supabase
- Redis via Docker
- OAuth2 Login: Kakao, Google
- JWT with Access Token in HttpOnly Cookie
- Refresh Token stored in Redis with RTR and blacklist strategy
- AI Model: Google Gemini 1.5 Flash
- Target server: Raspberry Pi 4B, 8GB RAM, Ubuntu 22.04.5 LTS

## Architecture
- Prefer DDD-oriented package structure.
- `global/` is for cross-cutting concerns:
    - Security
    - S3
    - Exception handling
    - Common config
- `domain/` is for business domains.
- Keep domain logic inside each domain package.
- Avoid leaking domain-specific logic into `global/`.

## SEED Planning Docs
- For SEED PoC work, first read `docs-local/planning/seed-poc-context.md`.
- Do not read full ERD/API/issue files unless the current task requires exact schema or endpoint details.
- If exact table columns are needed, search only the relevant table in `docs-local/planning/seed-erd-poc.sql`.
- If exact API details are needed, search the API CSV by endpoint name instead of reading the full file.
- `seed-erd-full.sql` is for post-PoC/full-scope reference only.

## PoC Scope
- Include:
    - PDF-based project creation
    - roadmap step creation
    - prompt generation/edit/save
    - step result save/load
    - self-check save/load
    - AI mentor chat save/load
    - mentor student/project review pages
- Exclude unless explicitly requested:
    - image upload
    - PDF export
    - share links
    - public read-only pages

## Core Domain Flow
- User selects writing type.
- User uploads assignment PDF files and enters project intent:
    - desired outcome
    - key focus
    - required elements
- The system extracts and cleans PDF text.
- The system creates a project and roadmap steps.
- The system generates step prompts from:
    - prompt template
    - parsed PDF content
    - user intent fields
    - previous step result when needed
- User edits prompt if needed.
- User may ask AI mentor questions.
- User writes step result.
- User must submit self-check before moving to the next step.
- Repeat until the project is complete.

## AI Policy
- Use AI mainly for parsing assistance, summarization, prompt generation, and AI mentor responses.
- Minimize AI usage cost.
- Avoid sending unnecessary data to Gemini.
- Prefer deterministic Java processing when AI is not needed.
- Keep Gemini integration isolated behind a service/client abstraction.
- For AI mentor chat, send only necessary context and recent messages.

## PDF/File Policy
- Use Apache PDFBox for PDF text extraction and preprocessing.
- File upload limit:
    - Max 20MB
    - Max 3 files
- Currently validate PDF.
- JPG and PNG support is planned, not PoC scope.
- Validate file extension and actual file signature/content type.
- Do not trust only client-provided MIME type.

## Mentor Policy
- Mentor users are stored in `users` with role-based branching.
- `mentor_students` controls which students a mentor can access.
- Mentor project detail must include:
    - original prompt
    - edited prompt
    - step result
    - self-check
- Mentor project detail must NOT expose AI mentor chat messages.
- `project_reviews` stores mentor review status separately from project progress status.

## Step Record Policy
- `project_step_prompts` stores original and edited prompts.
- `project_step_results` stores user output.
- `project_step_self_checks.check_items_json` stores question snapshots and answers.
- `project_step_ai_messages` stores student-AI chat logs.
- Keep these responsibilities separate.

## Redis Policy
- Use Redis for refresh token TTL management.
- Use Redis for PDF parsing result cache later.
- Planned cache key strategy:
    - MD5 hash of uploaded file
    - Prevent duplicate parsing for identical PDFs

## Refactoring Policy
- Before editing, explain:
    - target files
    - reason
    - expected behavior change
    - risk
    - test command
- Prioritize:
    - transaction boundary issues
    - duplicated validation logic
    - DTO/entity coupling
    - N+1 query risk
    - exception handling consistency
    - security filter/order issues
    - oversized service methods

## Issue Workflow
- Implement one issue-sized task at a time.
- Before editing, map the issue to Controller, Service, Repository, DTO, Entity, and tests.
- Do not implement deferred PoC features unless explicitly requested.
- Keep changes small enough for code review.

## Test Policy
- Prefer existing tests.
- If behavior changes, add or update tests.
- After changes, suggest the smallest relevant Gradle test command.