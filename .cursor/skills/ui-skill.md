# UI Modernization Skill — Bootstrap 5 for Thymeleaf Templates

## Purpose

Transform the Invoicing Manager's plain, duplicated Thymeleaf templates into a polished, professional Business SaaS UI using Bootstrap 5.3.2. Every change must stay within the existing Spring MVC + Thymeleaf architecture. Do not introduce any new frameworks.

---

## App Analysis (current state)

Read this before making any changes so you understand exactly what exists:

| File | Current problem |
|---|---|
| `layout.html` | Exists but is **never used**. Every page duplicates its own `<head>`, navbar, and Bootstrap CDN `<link>`/`<script>`. |
| `dashboard/index.html` | Fully inline page. Status badges all use `text-bg-secondary` regardless of status. Missing Bootstrap JS `<script>`. |
| `customers/list.html` | Inline page. Missing Dashboard nav link. Empty state is plain text only. |
| `customers/detail.html` | Inline page. No logout button. `dl` styling is bare. |
| `customers/form.html` | Inline page. No logout button. No back-breadcrumb. |
| `invoices/list.html` | Inline page. Missing Dashboard nav link. Status badges all secondary. |
| `invoices/detail.html` | Inline page. No logout button. Status badge always secondary. |
| `invoices/form.html` | Inline page. No logout button. Line-item table cramped. No Bootstrap JS. |
| `auth/login.html` | No branding mark. No Bootstrap Icons. No visual weight. |
| `auth/register.html` | Same issues as login. |
| `static/css/app.css` | Only 4 CSS variables and 3 rules. No sidebar variables, no badge overrides, no icon sizing. |

**Root cause of all issues**: there are no shared Thymeleaf fragments. Every page is a self-contained copy. Fix the fragment architecture first, then improve individual pages.

---

## Step 1 — Create shared Thymeleaf fragments

Create the file `src/main/resources/templates/fragments/layout.html`.

This file must contain **three named fragments**:

### Fragment 1: `htmlHead`

Replace the duplicate `<head>` block. Accept a `pageTitle` variable.

```html
<!doctype html>
<html lang="en" xmlns:th="http://www.thymeleaf.org">
<head th:fragment="htmlHead(pageTitle)">
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title th:text="${pageTitle} + ' | Invoicing Manager'">Invoicing Manager</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css">
    <link th:href="@{/css/app.css}" rel="stylesheet">
</head>
```

### Fragment 2: `navbar`

A single authoritative navbar. It must:
- Link the brand to `/dashboard`
- Include Dashboard, Customers, and Invoices nav links
- Accept an `activePage` String parameter (`'dashboard'`, `'customers'`, or `'invoices'`) and apply the `active` class dynamically using `th:classappend`
- Include the logout form on the right side
- Include the mobile toggler and `navbar-collapse`

```html
<nav th:fragment="navbar(activePage)" class="navbar navbar-expand-lg bg-white border-bottom shadow-sm sticky-top">
    <div class="container">
        <a class="navbar-brand d-flex align-items-center gap-2 fw-bold text-primary" th:href="@{/dashboard}">
            <i class="bi bi-receipt-cutoff fs-5"></i>
            Invoicing Manager
        </a>
        <button class="navbar-toggler border-0" type="button"
                data-bs-toggle="collapse" data-bs-target="#mainNav"
                aria-controls="mainNav" aria-expanded="false" aria-label="Toggle navigation">
            <span class="navbar-toggler-icon"></span>
        </button>
        <div class="collapse navbar-collapse" id="mainNav">
            <ul class="navbar-nav me-auto mb-2 mb-lg-0 ms-3">
                <li class="nav-item">
                    <a class="nav-link" th:classappend="${activePage == 'dashboard'} ? 'active'" th:href="@{/dashboard}">
                        <i class="bi bi-grid me-1"></i>Dashboard
                    </a>
                </li>
                <li class="nav-item">
                    <a class="nav-link" th:classappend="${activePage == 'customers'} ? 'active'" th:href="@{/customers}">
                        <i class="bi bi-people me-1"></i>Customers
                    </a>
                </li>
                <li class="nav-item">
                    <a class="nav-link" th:classappend="${activePage == 'invoices'} ? 'active'" th:href="@{/invoices}">
                        <i class="bi bi-file-earmark-text me-1"></i>Invoices
                    </a>
                </li>
            </ul>
            <div class="d-flex align-items-center gap-2">
                <form th:action="@{/logout}" method="post" class="m-0">
                    <input type="hidden" th:name="${_csrf.parameterName}" th:value="${_csrf.token}">
                    <button class="btn btn-outline-secondary btn-sm" type="submit">
                        <i class="bi bi-box-arrow-right me-1"></i>Logout
                    </button>
                </form>
            </div>
        </div>
    </div>
</nav>
```

### Fragment 3: `scripts`

Common script block for every page that needs Bootstrap JS:

```html
<th:block th:fragment="scripts">
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"></script>
</th:block>
```

---

## Step 2 — Enhance `app.css`

Replace the entire content of `src/main/resources/static/css/app.css` with the following. Keep all existing variables and extend them:

```css
/* ─── Design tokens ─────────────────────────────────────────────── */
:root {
    --app-page-bg:        #f1f5f9;
    --app-border-color:   #e2e8f0;
    --app-shadow-sm:      0 1px 3px rgba(15, 23, 42, 0.06), 0 1px 2px rgba(15, 23, 42, 0.04);
    --app-shadow-card:    0 4px 16px rgba(15, 23, 42, 0.06);
    --app-radius:         0.75rem;
    --app-primary:        #2563eb;
    --app-primary-light:  #eff6ff;
}

/* ─── Base ───────────────────────────────────────────────────────── */
body {
    background-color: var(--app-page-bg);
    min-height: 100vh;
}

/* ─── Navbar ─────────────────────────────────────────────────────── */
.navbar-brand {
    letter-spacing: -0.02em;
}

.navbar .nav-link {
    font-size: 0.875rem;
    font-weight: 500;
    color: #475569;
    border-radius: 0.375rem;
    padding: 0.375rem 0.625rem;
    transition: color 0.15s, background-color 0.15s;
}

.navbar .nav-link:hover,
.navbar .nav-link.active {
    color: var(--app-primary);
    background-color: var(--app-primary-light);
}

/* ─── Cards ──────────────────────────────────────────────────────── */
.app-card {
    border: 1px solid var(--app-border-color);
    border-radius: var(--app-radius);
    box-shadow: var(--app-shadow-card);
}

/* ─── Page header ────────────────────────────────────────────────── */
.page-header {
    padding-bottom: 1rem;
    margin-bottom: 1.5rem;
    border-bottom: 1px solid var(--app-border-color);
}

/* ─── Stat cards (dashboard) ─────────────────────────────────────── */
.stat-card-label {
    font-size: 0.75rem;
    font-weight: 600;
    text-transform: uppercase;
    letter-spacing: 0.06em;
    color: #64748b;
    margin-bottom: 0.5rem;
}

.stat-card-value {
    font-size: 1.75rem;
    font-weight: 700;
    line-height: 1.1;
    color: #0f172a;
    margin-bottom: 0.75rem;
}

.stat-card-icon {
    width: 2.5rem;
    height: 2.5rem;
    border-radius: 0.5rem;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 1.1rem;
}

/* ─── Status badges ──────────────────────────────────────────────── */
.badge-draft  { background-color: #f1f5f9; color: #475569; border: 1px solid #cbd5e1; }
.badge-sent   { background-color: #eff6ff; color: #2563eb; border: 1px solid #bfdbfe; }
.badge-paid   { background-color: #f0fdf4; color: #16a34a; border: 1px solid #bbf7d0; }

/* ─── Tables ─────────────────────────────────────────────────────── */
.table > :not(caption) > * > * {
    vertical-align: middle;
    padding: 0.875rem 0.75rem;
}

.table thead th {
    font-size: 0.75rem;
    font-weight: 600;
    text-transform: uppercase;
    letter-spacing: 0.05em;
    color: #64748b;
    background-color: #f8fafc;
    border-bottom-color: var(--app-border-color);
}

/* ─── Empty states ───────────────────────────────────────────────── */
.empty-state {
    padding: 4rem 2rem;
    text-align: center;
    color: #94a3b8;
}

.empty-state-icon {
    font-size: 3rem;
    margin-bottom: 1rem;
    opacity: 0.4;
}

.empty-state-title {
    font-size: 1rem;
    font-weight: 600;
    color: #64748b;
    margin-bottom: 0.5rem;
}

.empty-state-text {
    font-size: 0.875rem;
    margin-bottom: 1.5rem;
}

/* ─── Auth pages ─────────────────────────────────────────────────── */
.auth-shell {
    min-height: 100vh;
    display: flex;
    align-items: center;
    justify-content: center;
    background: linear-gradient(135deg, #eff6ff 0%, #f1f5f9 60%, #f8fafc 100%);
}

.auth-card {
    width: 100%;
    max-width: 440px;
    padding: 2.5rem;
}

.auth-logo {
    font-size: 1.375rem;
    font-weight: 700;
    color: var(--app-primary);
    letter-spacing: -0.02em;
}

/* ─── Invoice detail ─────────────────────────────────────────────── */
.invoice-meta dt {
    font-weight: 600;
    color: #64748b;
    font-size: 0.8125rem;
}

.invoice-meta dd {
    margin-bottom: 0.5rem;
    color: #0f172a;
}

/* ─── Form helpers ───────────────────────────────────────────────── */
.form-label {
    font-weight: 500;
    font-size: 0.875rem;
    color: #374151;
    margin-bottom: 0.375rem;
}

.form-section-title {
    font-size: 0.8125rem;
    font-weight: 600;
    text-transform: uppercase;
    letter-spacing: 0.06em;
    color: #64748b;
    padding-bottom: 0.5rem;
    border-bottom: 1px solid var(--app-border-color);
    margin-bottom: 1rem;
}

/* ─── Validation ─────────────────────────────────────────────────── */
.field-error {
    font-size: 0.8125rem;
    color: #dc2626;
    margin-top: 0.25rem;
}

/* ─── Totals summary strip ───────────────────────────────────────── */
.totals-strip {
    background-color: #f8fafc;
    border: 1px solid var(--app-border-color);
    border-radius: 0.5rem;
    padding: 1rem 1.25rem;
}

.totals-strip .total-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 0.25rem 0;
    font-size: 0.9375rem;
    color: #475569;
}

.totals-strip .total-row.grand {
    border-top: 1px solid var(--app-border-color);
    margin-top: 0.5rem;
    padding-top: 0.75rem;
    font-weight: 700;
    font-size: 1.125rem;
    color: #0f172a;
}
```

---

## Step 3 — Rewrite each template

Apply the fragments and new CSS classes to every template. Follow this exact approach for each file.

### Rules that apply to every template

1. Replace the inline `<head>` block with `<th:block th:replace="~{fragments/layout :: htmlHead('Page Title')}"></th:block>`.
2. Replace the inline navbar with `<nav th:replace="~{fragments/layout :: navbar('activePage')}"></nav>`.
3. Add `<th:block th:replace="~{fragments/layout :: scripts}"></th:block>` before `</body>` on every page that does not have its own extra scripts. For `invoices/form.html`, add Bootstrap JS **before** the invoice-form.js script.
4. Remove every `<link>` and `<script>` tag for Bootstrap CDN from the individual template files.
5. Wrap `<body>` content in `<div class="app-shell d-flex flex-column">` only if needed by layout.

---

### `auth/login.html`

- Do **not** include the navbar fragment (auth pages have no nav).
- Use `.auth-shell` as the body wrapper.
- Structure:
  ```
  <body class="auth-shell">
    <div class="auth-card app-card bg-white">
      [logo mark using .auth-logo + bi-receipt-cutoff icon]
      [h1 "Welcome back" with subtitle]
      [error/logout/registered alerts]
      [form]
      [link to /register]
    </div>
  </body>
  ```
- Make the submit button full-width (`w-100`) with a lock icon.

### `auth/register.html`

- Same auth-shell pattern as login.
- `max-width` of the card: use `col-lg-6` centered or the `.auth-card` class (widen to `max-width: 540px` using a modifier class or inline style on the card).
- Title: "Create your account" with subtitle "Set up your invoicing workspace."
- Validation errors: replace `<div class="text-danger small" th:errors="...">` with `<div class="field-error" th:errors="...">`.

### `dashboard/index.html`

- Include navbar fragment with `activePage = 'dashboard'`.
- Page header: use `.page-header` with a flex row (title+subtitle left, action buttons right).
- Stat cards: upgrade each `app-card` to use `.stat-card-label`, `.stat-card-value`, and a `.stat-card-icon` with a colored `bg-*-subtle` + bootstrap-icons icon:
  - Total revenue → `bi-currency-dollar`, `bg-success-subtle text-success`
  - Pending amount → `bi-clock`, `bg-primary-subtle text-primary`
  - Pending invoices → `bi-hourglass-split`, `bg-warning-subtle text-warning`
  - Draft invoices → `bi-pencil`, `bg-secondary-subtle text-secondary`
- Status badges in the recent-invoices table: replace hardcoded `text-bg-secondary` with dynamic class using `th:classappend`:
  ```html
  <span class="badge"
        th:classappend="${invoice.status.name() == 'PAID'} ? 'badge-paid' :
                        (${invoice.status.name() == 'SENT'} ? 'badge-sent' : 'badge-draft')"
        th:text="${invoice.status}">DRAFT</span>
  ```
- Empty state: replace the plain text `div` with an `.empty-state` block that has an icon, title, text, and a primary action link.
- Add Bootstrap JS via the `scripts` fragment.

### `customers/list.html`

- Include navbar fragment with `activePage = 'customers'`.
- Page header: `.page-header` with "Customers" h1, subtitle, and "New customer" button with `bi-person-plus` icon.
- Empty state: use `.empty-state` with `bi-people` icon, "No customers yet" title, and a "Create your first customer" primary button.
- Table: add `table-hover` class. Customer name column: bold the link text using `fw-medium`.

### `customers/detail.html`

- Include navbar fragment with `activePage = 'customers'`.
- Add a breadcrumb above the page header: `Customers / [Customer Name]`.
- Details card: style the `dl` using Bootstrap's `row` layout (`dt col-5`, `dd col-7`) and the `.invoice-meta` class for typography.
- Invoice table: use the dynamic status badge (same pattern as dashboard).
- Delete button: move it to a visually separated danger zone below the details card, inside a small `app-card` with a warning label like "Danger zone".

### `customers/form.html`

- Include navbar fragment with `activePage = 'customers'`.
- Add breadcrumb: `Customers / New Customer` or `Customers / Edit [name]`.
- Group form fields into sections using `.form-section-title`:
  - "Basic info" — Name, Email, Phone
  - "Address" — Billing address, City, Country
  - "Additional" — Tax number, Notes
- Validation errors: use `.field-error` class instead of `text-danger small`.
- Footer actions: right-align `Save customer` (primary) and `Cancel` (link/outline), separated from the form body with `mt-4 pt-3 border-top`.

### `invoices/list.html`

- Include navbar fragment with `activePage = 'invoices'`.
- Page header: `.page-header` with "Invoices" h1 and "New invoice" button with `bi-plus-circle` icon.
- Filter form: wrap it in a visually lighter panel (small `bg-light rounded p-3 mb-3`) rather than inside the main card.
- Status badges: use the dynamic badge pattern.
- Empty state: `.empty-state` with `bi-file-earmark-text` icon, "No invoices found", and a "Create invoice" primary button.

### `invoices/detail.html`

- Include navbar fragment with `activePage = 'invoices'`.
- Add breadcrumb: `Invoices / [Invoice Number]`.
- Status badge in the heading area: use the dynamic badge pattern.
- Dates row: use a definition list with `.invoice-meta` styling.
- Line-items table: add `table-hover`. Align numeric columns right.
- Totals: replace the raw `dl.row` with a `.totals-strip` block containing `.total-row` divs for Subtotal, Tax, and Grand Total (with `.grand` class on the last one).
- Notes section: wrap in an `alert alert-light` with `bi-chat-left-text` icon.
- Action buttons: "Mark sent" only when `DRAFT`; "Mark paid" only when not `PAID`. Use `btn-outline-success` for "Mark paid" to distinguish from the primary "Mark sent".
- Delete button: styled as `btn-outline-danger btn-sm` inside a clearly labelled danger section at the bottom of the card.

### `invoices/form.html`

- Include navbar fragment with `activePage = 'invoices'`.
- Add breadcrumb: `Invoices / New Invoice` or `Invoices / Edit [number]`.
- Customer-missing alert: upgrade `alert-warning` to include a `bi-exclamation-triangle` icon.
- Group header fields into sections (`.form-section-title`):
  - "Invoice details" — Customer, Invoice number, Issue date, Due date
  - "Notes"
  - "Line items"
- Line-items table: make input columns compact using `style="min-width: 7rem"` on the header cells.
- Preview total: upgrade the text to a small `.totals-strip` container showing "Estimated total" with `data-invoice-preview`.
- Scripts: add Bootstrap JS fragment **before** the existing `invoice-form.js` script tag. Do not remove the `invoice-form.js` script.

---

## Step 4 — Verify after applying

After all changes:

1. Check that **no individual template** still has its own `<link href="bootstrap...">` or `<script src="bootstrap...">`.
2. Check that **every authenticated page** (dashboard, customers, invoices) renders the shared navbar with the correct active link.
3. Check that **auth pages** (login, register) do NOT include the navbar.
4. Check that **status badges** show three distinct visual styles (draft = grey, sent = blue, paid = green).
5. Check that **all forms** preserve CSRF inputs and Thymeleaf `th:field` bindings.
6. Check that **`invoice-form.js`** still works (Bootstrap JS must load before it or at least before DOMContentLoaded; `bootstrap.bundle.min.js` with `defer` or before the script tag is fine).
7. Check that the `app.css` you wrote does not conflict with Bootstrap utility classes — custom classes use the `.app-*`, `.stat-card-*`, `.auth-*`, `.badge-draft/sent/paid`, `.empty-state`, `.totals-strip` namespaces.

---

## What NOT to do

- Do not remove `th:field`, `th:object`, `th:errors`, or CSRF inputs from any form.
- Do not add calculations or business logic to templates.
- Do not introduce JavaScript beyond what already exists in `invoice-form.js`.
- Do not change any controller, service, entity, or repository class.
- Do not add inline `style=""` attributes except for `min-width` on table header cells in the invoice form.
- Do not use Tailwind, React, Vue, or any other framework.
