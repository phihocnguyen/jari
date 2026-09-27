# Git conventions

## Commit messages (Conventional Commits)

Format:

```text
<type>(optional-scope): <summary>

[optional body]
```

- **Summary**: imperative mood, ≤ ~72 chars, no trailing period
- **Types**: `feat`, `fix`, `docs`, `style`, `refactor`, `perf`, `test`, `build`, `ci`, `chore`
- **Scope** (optional): area of the change, e.g. `issue`, `cache`, `k6`, `auth`

Examples from this repo:

```text
feat(issue): allocate issue keys in a short separate transaction
fix(project): flush project before inserting issue counter row
perf(cache): evict board only when issue is on active sprint
test: restore JaCoCo branch coverage gate
ci: silence Mockito dynamic-agent warnings under JDK 21
docs: document write-heavy k6 baseline and cache invalidation
chore: remove outdated implementation plan
```

## What not to include

- Do **not** add `Co-authored-by:` trailers (including Cursor / AI agents)
- Do **not** put secrets, tokens, or `.env` contents in commits
- Prefer focused commits; avoid mixing unrelated refactors with feature work

## Branches

- Default branch: `master` (or `main`)
- Feature work: `feat/<short-name>` or `fix/<short-name>` when using PRs

## Pull requests

- Title follows the same conventional style as commits when practical
- Describe **why** and how to test; link issues if any
