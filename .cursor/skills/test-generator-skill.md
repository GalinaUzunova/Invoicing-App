# Test Generator Skill

## Purpose

Generate reliable tests for the Invoicing Manager Spring Boot application. Prioritize service-layer coverage first, then integration and MVC/API route tests. Keep tests focused, deterministic, and aligned with the existing Controller -> Service -> Repository architecture.

## Testing Targets

- Cover 100% of public service methods with unit tests.
- Cover important server-rendered routes with MVC/API tests using `MockMvc`.
- Cover persistence wiring and application bootstrapping with integration tests when database behavior is involved.
- Keep frontend empty-state messages intact. Do not change templates just to satisfy tests.

## Unit Test Best Practices

- Use JUnit 5, AssertJ, and Mockito.
- Use `@ExtendWith(MockitoExtension.class)` for service unit tests.
- Mock repositories and external collaborators. Do not start Spring for pure service tests.
- Test both happy path and failure path for every service method:
  - successful create/update/delete flows
  - not-found behavior from repository `Optional.empty()`
  - duplicate number checks
  - invalid status transitions
  - null/blank validation guards
- Verify meaningful repository interactions with `verify(...)` only when behavior matters.
- Prefer real domain entities and DTOs over broad stubs.
- Use `ArgumentCaptor` when asserting saved entity state.
- For money calculations, assert with `isEqualByComparingTo(...)`.
- Keep tests independent. Do not rely on execution order.

## Integration Test Best Practices

- Use `@SpringBootTest` for application-context smoke tests and cross-bean wiring.
- Use test profile or in-memory database when persistence integration is required.
- Keep integration tests fewer than unit tests; they should cover wiring, transactions, and security boundaries.
- Do not duplicate every service unit test as an integration test.

## MVC/API Test Best Practices

- Use `@WebMvcTest(ControllerClass.class)` with `MockMvc`.
- Import `SecurityConfig` when route security matters.
- Use `@WithMockUser` for authenticated routes.
- Assert:
  - anonymous users redirect to login
  - GET routes return the expected view and model attributes
  - POST routes require CSRF
  - successful POST routes redirect correctly
  - validation errors return the form view
- Mock service dependencies with `@MockitoBean`.
- Treat Thymeleaf pages as API surfaces: verify status, view, redirect, model, and security behavior.

## Naming

- Name test classes after the class under test: `InvoiceServiceTest`, `EstimateControllerTest`.
- Use descriptive test method names:
  - `createSavesInvoiceWithCalculatedTotals`
  - `findByIdForUserThrowsNotFoundWhenMissing`
  - `postInvoiceRequiresCsrfToken`

## Things To Avoid

- Do not test Lombok-generated getters/setters directly.
- Do not mock the class under test.
- Do not assert exact generated UUID values; assert the expected prefix/pattern.
- Do not hide or remove server validation errors.
- Do not alter user-facing empty-state messages for invoices or estimates.
