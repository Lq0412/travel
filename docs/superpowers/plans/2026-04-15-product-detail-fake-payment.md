# Product Detail Fake Payment Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a product detail page and fake payment success flow so mall items and workspace recommendation cards can navigate into a complete purchase path.

**Architecture:** Keep routing in `src/router/index.ts`, add one focused utility for product lookup/order persistence, and create two route-level Vue pages: product detail and payment success. Existing mall/workspace components stay thin and only navigate to the new routes.

**Tech Stack:** Vue 3, Vue Router 4, Ant Design Vue 4, Vitest, TypeScript

---

### Task 1: Lock down route coverage

**Files:**
- Modify: `travel_frontend/src/router/index.test.ts`

- [ ] Add a failing router test that asserts `/products/:id` and `/products/:id/payment-success` are registered under `topnav`.
- [ ] Run `npm test -- src/router/index.test.ts` in `travel_frontend` and verify the new assertions fail because the routes do not exist yet.
- [ ] Add the minimal router registrations after the test fails.
- [ ] Re-run `npm test -- src/router/index.test.ts` and verify it passes.

### Task 2: Add product order utility with TDD

**Files:**
- Create: `travel_frontend/src/utils/productOrder.ts`
- Create: `travel_frontend/src/utils/productOrder.test.ts`

- [ ] Write failing tests for loading a product by id, creating a fake paid order, and reading the saved order back from storage.
- [ ] Run `npm test -- src/utils/productOrder.test.ts` and verify the tests fail for the expected missing utility behavior.
- [ ] Implement the minimal utility functions for product lookup and fake order persistence.
- [ ] Re-run `npm test -- src/utils/productOrder.test.ts` and verify it passes.

### Task 3: Build the detail and success pages

**Files:**
- Create: `travel_frontend/src/pages/mall/ProductDetailPage.vue`
- Create: `travel_frontend/src/pages/mall/ProductPaymentSuccessPage.vue`

- [ ] Create the product detail page as a route-level composition surface that loads the product from route params, renders the product information, and exposes a primary “立即购买” action.
- [ ] Create the payment success page that loads the saved fake order and shows summary information with clear next actions.
- [ ] Keep both pages aligned with the existing enterprise-style app shell and Ant Design interaction patterns.

### Task 4: Wire entry points into the new flow

**Files:**
- Modify: `travel_frontend/src/pages/workspace/WorkspacePage.vue`
- Modify: `travel_frontend/src/pages/mall/MallPage.vue`

- [ ] Replace the workspace “去看看” placeholder handler with router navigation to `/products/:id`.
- [ ] Make mall cards navigable to the same detail page while preserving edit/delete actions.
- [ ] Keep props-down/events-up boundaries unchanged for recommendation cards.

### Task 5: Verify the feature end to end

**Files:**
- Modify if needed: `travel_frontend/tsconfig.app.json`

- [ ] Run `npm test -- src/router/index.test.ts src/utils/productOrder.test.ts` in `travel_frontend`.
- [ ] Run `npm run type-check` in `travel_frontend`.
- [ ] If type-check fails because of stale exclusions or route typing issues, make the minimal fix and rerun verification.
