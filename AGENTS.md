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

## Tech Stack
- Java 21
- Spring Boot 4.0.2
- Spring Framework 8
- Gradle Kotlin DSL
- SpringDoc OpenAPI 3.0
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

## Core Domain Flow
- The product analyzes assignment PDFs and generates step-by-step roadmaps and customized prompts.
- User selects writing type.
- User uploads assignment PDF and enters text they want to write.
- The system loads the roadmap template for the selected writing type from DB.
- The system generates the first-step prompt from:
    - prompt template
    - parsed and cleaned PDF content
    - user's input text
- User enters the result for the current step.
- The system generates the next-step prompt.
- Repeat until the project is complete.
- Completed projects must be accessible from My Page.

## AI Policy
- Use AI mainly for parsing, summarization, and prompt generation.
- Minimize AI usage cost.
- Avoid sending unnecessary data to Gemini.
- Prefer deterministic processing with Java code when AI is not needed.
- Keep Gemini integration isolated behind a service/client abstraction.

## PDF/File Policy
- Use Apache PDFBox for PDF text extraction and preprocessing.
- File upload limit:
    - Max 20MB
    - Max 3 files
- Currently validate PDF.
- JPG and PNG support is planned.
- Validate file extension and actual file signature/content type.
- Do not trust only client-provided MIME type.

## Redis Policy
- Use Redis for refresh token TTL management.
- Use Redis for PDF parsing result cache later.
- Planned cache key strategy:
    - MD5 hash of uploaded file
    - Prevent duplicate parsing for identical PDFs

## Refactoring Policy
- First inspect existing Controller, Service, Repository, DTO, Entity, Security config.
- Prioritize:
    - transaction boundary issues
    - duplicated validation logic
    - DTO/entity coupling
    - N+1 query risk
    - exception handling consistency
    - security filter/order issues
    - oversized service methods
- Before editing, explain:
    - target files
    - reason
    - expected behavior change
    - risk
    - test command
- Avoid unrelated formatting changes.
- Keep changes small enough for code review.

## Test Policy
- Prefer existing tests.
- If behavior changes, add or update tests.
- After changes, suggest the smallest relevant Gradle test command.
