# jwt-auth-filter-demo

A minimal, from-scratch JWT authentication filter for **Spring Boot 3 /
Spring Security 6**, written as a standalone demo of a pattern — not
extracted or copied from any employer/client codebase. Runs with an
in-memory H2 database, no external services required.

## Why this exists

This demonstrates the core mechanics of stateless JWT auth in Spring
Security: issuing a signed token on login, validating it on every
subsequent request via a custom `OncePerRequestFilter`, and the handful
of mistakes that are easy to make and easy to miss in testing (see
below).

## Run it

```bash
mvn spring-boot:run
```

```bash
# 1. Register
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","password":"s3cret-password"}'

# 2. Login, get a token
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","password":"s3cret-password"}'
# -> {"token":"eyJ..."}

# 3. Call a protected endpoint without a token -> 401
curl -i http://localhost:8080/api/me

# 4. Call it with the token -> 200
curl http://localhost:8080/api/me -H "Authorization: Bearer eyJ..."
```

## What's actually being tested

`AuthFlowIntegrationTest` runs the full real flow through Spring's test
MVC harness (register → login → call a protected endpoint with the
issued token → confirm a missing/tampered token is rejected) — not just
unit tests against mocks. CI runs `mvn verify` on every push; see the
Actions badge/workflow for the real pass/fail signal, since this repo
was authored without a local Maven/JDK 21 toolchain available to run it
before pushing.

## Design notes / things that are easy to get wrong here

- **Signature verification happens inside `JwtService.extractAllClaims()`**
  via `parseSignedClaims()`, which throws on a tampered or expired token.
  `isTokenValid()` is a *secondary* check (token belongs to the user
  currently being resolved) — not the only line of defense.
- **The filter never throws a raw exception on a bad token.** A
  malformed/expired JWT is caught and the request falls through
  unauthenticated, letting `authorizeHttpRequests` reject it with a
  clean 401 — instead of leaking a 500.
- **`SessionCreationPolicy.STATELESS`** is set explicitly; without it,
  Spring Security still tries to use session/cookie machinery alongside
  the token, producing inconsistent auth behavior.
- **Default Spring Security returns 403, not 401, for an unauthenticated
  request** unless you set an explicit `AuthenticationEntryPoint`. 403
  technically means "I know who you are, and you're not allowed"; 401
  means "you never proved who you are" - for a pure JWT API, 401 is the
  correct status, and it takes one explicit `exceptionHandling(...)`
  bean to get it (see `SecurityConfig`). This repo's CI caught this for
  real: the first push compiled fine and genuinely looked correct, and
  the integration test is what caught the wrong status code.
- **`/api/auth/**` is `permitAll()`** — an easy thing to forget, and
  forgetting it means nobody can ever log in, because the login endpoint
  itself gets blocked by the "must already be authenticated" rule.

## Stack

Spring Boot 3.3, Spring Security 6, Spring Data JPA, H2 (in-memory),
[jjwt](https://github.com/jwtk/jjwt) 0.12.x, Java 21.

## License

MIT — do whatever you want with it.
