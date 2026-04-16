# Itinerary Detail Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild the trip detail page so clicking a trip shows parsed structured itinerary content with day switching, timeline cards, and a synced map panel.

**Architecture:** Keep `TripDetailPage.vue` as the route-level container, move structured itinerary UI into focused trip-detail components, and derive all detail state from a parsed `StructuredItinerary`. Use a plain utility for parsing/filtering so the feature has a stable test seam before wiring the UI.

**Tech Stack:** Vue 3 SFCs with `<script setup lang="ts">`, Ant Design Vue, Vitest, existing `DynamicMap.vue`

---

### Task 1: Add structured itinerary utility coverage

**Files:**
- Create: `travel_frontend/src/utils/tripDetail.ts`
- Create: `travel_frontend/src/utils/tripDetail.test.ts`

- [ ] Write the failing tests for parsing raw structured data and filtering a single day view.
- [ ] Run `npm test -- src/utils/tripDetail.test.ts` from `travel_frontend` and confirm the new tests fail for the expected missing module/functions.
- [ ] Implement the minimal parser/normalizer/filter helpers in `src/utils/tripDetail.ts`.
- [ ] Re-run `npm test -- src/utils/tripDetail.test.ts` and confirm pass.

### Task 2: Build focused itinerary detail components

**Files:**
- Create: `travel_frontend/src/components/trips/detail/TripDetailSummaryCards.vue`
- Create: `travel_frontend/src/components/trips/detail/TripDaySwitcher.vue`
- Create: `travel_frontend/src/components/trips/detail/TripItineraryTimeline.vue`

- [ ] Add the new components with typed props and explicit emits where needed.
- [ ] Keep route-level orchestration out of the child components; they should only render derived inputs.
- [ ] Reuse current Ant-style spacing, radius, card hierarchy, and avoid decorative layout drift.

### Task 3: Rebuild the trip detail page around structured data

**Files:**
- Modify: `travel_frontend/src/pages/trips/TripDetailPage.vue`

- [ ] Parse `trip.structuredData` safely and derive `selectedDay`, `selectedActivityKey`, `visibleItinerary`, and fallback state from it.
- [ ] Replace the old highlight-only overview with summary cards, day switcher, timeline panel, and sticky map panel.
- [ ] Keep the existing photo manager tab and completed-status action working.
- [ ] Gracefully degrade to the legacy summary view if structured data is missing or invalid.

### Task 4: Verify behavior

**Files:**
- Modify if needed: `travel_frontend/src/pages/trips/TripDetailPage.vue`
- Test: `travel_frontend/src/utils/tripDetail.test.ts`

- [ ] Run `npm test -- src/utils/tripDetail.test.ts`.
- [ ] Run `npm run type-check`.
- [ ] Fix any type or template errors introduced by the detail-page refactor.
