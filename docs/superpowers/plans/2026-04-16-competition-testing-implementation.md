# Competition Testing Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a competition-ready test suite and manual test pack for the five agreed core modules, then execute it and collect report data.

**Architecture:** Keep the existing repo structure. Add manual test documentation under `docs/`, extend frontend `vitest` coverage for deterministic logic, extend backend `JUnit` coverage for service/controller behaviors, then run verification commands to capture measurable results.

**Tech Stack:** Markdown, Vue 3 + Vitest, Spring Boot + JUnit 5 + Mockito, npm, Maven

---

### Task 1: Lock Testing Scope In Docs

**Files:**
- Create: `C:\Users\Lq304\Desktop\travel\docs\superpowers\specs\2026-04-16-competition-testing-design.md`
- Create: `C:\Users\Lq304\Desktop\travel\docs\competition-test-cases.md`
- Create: `C:\Users\Lq304\Desktop\travel\docs\superpowers\plans\2026-04-16-competition-testing-implementation.md`

- [x] **Step 1: Write the testing design spec**

Completed in `C:\Users\Lq304\Desktop\travel\docs\superpowers\specs\2026-04-16-competition-testing-design.md`.

- [x] **Step 2: Write the manual test case document**

Completed in `C:\Users\Lq304\Desktop\travel\docs\competition-test-cases.md`.

- [x] **Step 3: Write the implementation plan**

Completed in `C:\Users\Lq304\Desktop\travel\docs\superpowers\plans\2026-04-16-competition-testing-implementation.md`.

### Task 2: Add Frontend Tests For Auth, Workspace, Trips, Monitor, Product

**Files:**
- Modify: `C:\Users\Lq304\Desktop\travel\travel_frontend\src\utils\workspaceSession.test.ts`
- Modify: `C:\Users\Lq304\Desktop\travel\travel_frontend\src\utils\tripDetail.test.ts`
- Create: `C:\Users\Lq304\Desktop\travel\travel_frontend\src\utils\productCatalog.behavior.test.ts`
- Create: `C:\Users\Lq304\Desktop\travel\travel_frontend\src\utils\productOrder.behavior.test.ts`

- [ ] **Step 1: Write one failing frontend test for the next uncovered behavior**

Run: `npm test -- src/utils/workspaceSession.test.ts`
Expected: FAIL for the new case before implementation.

- [ ] **Step 2: Implement the minimal test additions to express the behavior**

Use existing test style and keep cases deterministic.

- [ ] **Step 3: Re-run the targeted test**

Run: `npm test -- src/utils/workspaceSession.test.ts`
Expected: PASS

- [ ] **Step 4: Repeat TDD for trip detail and product behaviors**

Run:
`npm test -- src/utils/tripDetail.test.ts`
`npm test -- src/utils/productCatalog.behavior.test.ts`
`npm test -- src/utils/productOrder.behavior.test.ts`

- [ ] **Step 5: Run the full frontend test suite**

Run: `npm test`
Expected: PASS

### Task 3: Add Backend Tests For Auth, Trip, Monitor, Product

**Files:**
- Create: `C:\Users\Lq304\Desktop\travel\travel_backend\src\test\java\com\lq\travel\util\ResponseUtilsTest.java`
- Create: `C:\Users\Lq304\Desktop\travel\travel_backend\src\test\java\com\lq\travel\controller\MonitoringControllerTest.java`
- Modify: `C:\Users\Lq304\Desktop\travel\travel_backend\src\test\java\com\lq\travel\service\impl\ProductServiceImplTest.java`

- [ ] **Step 1: Write one failing backend test for a missing agreed behavior**

Run: `.\mvnw.cmd -Dtest=MonitoringControllerTest test`
Expected: FAIL before implementation.

- [ ] **Step 2: Add minimal test code only**

Use JUnit 5 and Mockito consistent with the repo.

- [ ] **Step 3: Re-run targeted backend tests**

Run:
`.\mvnw.cmd -Dtest=MonitoringControllerTest test`
`.\mvnw.cmd -Dtest=ProductServiceImplTest test`

- [ ] **Step 4: Run full backend tests**

Run: `.\mvnw.cmd test`
Expected: PASS

### Task 4: Execute Tests And Capture Report Data

**Files:**
- Modify: `C:\Users\Lq304\Desktop\travel\docs\competition-test-cases.md`
- Create: `C:\Users\Lq304\Desktop\travel\docs\competition-test-execution-summary.md`

- [ ] **Step 1: Run frontend verification commands**

Run:
`npm test`
`npm run type-check`
`npm run build`

- [ ] **Step 2: Run backend verification commands**

Run: `.\mvnw.cmd test`

- [ ] **Step 3: Record pass/fail counts and notable failures**

Write the measured results into `C:\Users\Lq304\Desktop\travel\docs\competition-test-execution-summary.md`.

- [ ] **Step 4: Update manual cases with actual execution result**

Fill `执行结果` and `备注` in `C:\Users\Lq304\Desktop\travel\docs\competition-test-cases.md`.
