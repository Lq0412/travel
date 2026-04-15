# Product Address Distance Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add address and coordinate support to mall products so recommendations can prefer nearby food options instead of any same-city item.

**Architecture:** Extend the product domain model with `address`, `latitude`, and `longitude`, geocode addresses during product save through the existing Amap integration, and surface the new fields in frontend product editing and recommendation flows. Recommendation ranking stays lightweight: same-city filtering first, then nearest-distance ordering against itinerary activity coordinates when available.

**Tech Stack:** Spring Boot, MyBatis-Plus, Hutool HTTP, Vue 3 `<script setup lang="ts">`, Ant Design Vue, Vitest, Maven.

---

### Task 1: Persist product address coordinates

**Files:**
- Modify: `C:\Users\Lq304\Desktop\travel\travel_backend\src\main\java\com\lq\travel\model\entity\Product.java`
- Modify: `C:\Users\Lq304\Desktop\travel\travel_backend\src\main\java\com\lq\travel\model\dto\product\ProductSaveRequest.java`
- Modify: `C:\Users\Lq304\Desktop\travel\travel_backend\src\main\java\com\lq\travel\model\vo\ProductVO.java`
- Modify: `C:\Users\Lq304\Desktop\travel\travel_backend\src\main\java\com\lq\travel\config\ProductSchemaInitializer.java`
- Modify: `C:\Users\Lq304\Desktop\travel\travel_backend\src\main\resources\sql\travel.sql`

- [ ] **Step 1: Write the failing backend assertions**

Add assertions proving `ProductSaveRequest`, `ProductVO`, and `Product` carry `address`, `latitude`, and `longitude`.

- [ ] **Step 2: Run targeted compile/test to verify failure**

Run: `mvn -DskipTests compile`
Expected: compile or later tests fail until new fields are added consistently.

- [ ] **Step 3: Add the new fields**

Update the entity/DTO/VO/schema to include:

```java
private String address;
private Double latitude;
private Double longitude;
```

Use SQL columns:

```sql
`address` varchar(512) NULL COMMENT '详细地址',
`latitude` decimal(10,7) NULL COMMENT '纬度',
`longitude` decimal(10,7) NULL COMMENT '经度',
```

- [ ] **Step 4: Run backend compile**

Run: `mvn -DskipTests compile`
Expected: PASS

### Task 2: Geocode address on product save

**Files:**
- Modify: `C:\Users\Lq304\Desktop\travel\travel_backend\src\main\java\com\lq\travel\service\AmapService.java`
- Modify: `C:\Users\Lq304\Desktop\travel\travel_backend\src\main\java\com\lq\travel\service\impl\AmapServiceImpl.java`
- Modify: `C:\Users\Lq304\Desktop\travel\travel_backend\src\main\java\com\lq\travel\service\impl\ProductServiceImpl.java`
- Create: `C:\Users\Lq304\Desktop\travel\travel_backend\src\test\java\com\lq\travel\service\impl\ProductServiceImplTest.java`

- [ ] **Step 1: Write the failing service test**

Cover two behaviors:
1. saving a product with address triggers geocoding and persists coordinates
2. saving without address keeps coordinates empty

- [ ] **Step 2: Run the test to verify failure**

Run: `mvn "-Dtest=ProductServiceImplTest" test`
Expected: FAIL because the service does not geocode or persist the new fields yet.

- [ ] **Step 3: Extend Amap service**

Add a small geocode result contract:

```java
public record GeoPoint(Double longitude, Double latitude, String formattedAddress) {}
GeoPoint geocode(String city, String address);
```

Implement it with the Amap geocode REST API, returning `null` when config is unavailable or lookup fails.

- [ ] **Step 4: Update product save flow**

In `ProductServiceImpl.saveProduct(...)`:
- validate `address`
- call geocode with `city + address`
- persist `address`, `longitude`, `latitude`
- allow graceful fallback when geocoding fails by saving address only

- [ ] **Step 5: Run the targeted backend test**

Run: `mvn "-Dtest=ProductServiceImplTest" test`
Expected: PASS

### Task 3: Expose address fields to frontend product editing

**Files:**
- Modify: `C:\Users\Lq304\Desktop\travel\travel_frontend\src\types\product.ts`
- Modify: `C:\Users\Lq304\Desktop\travel\travel_frontend\src\api\productController.ts`
- Modify: `C:\Users\Lq304\Desktop\travel\travel_frontend\src\utils\productCatalog.ts`
- Modify: `C:\Users\Lq304\Desktop\travel\travel_frontend\src\pages\mall\MallPage.vue`
- Modify: `C:\Users\Lq304\Desktop\travel\travel_frontend\src\utils\productCatalog.test.ts`

- [ ] **Step 1: Write the failing frontend test**

Add a test asserting `saveProductToServer()` sends `address`, `latitude`, and `longitude`.

- [ ] **Step 2: Run the targeted test to verify failure**

Run: `npm test -- src/utils/productCatalog.test.ts`
Expected: FAIL because the current payload omits the new location fields.

- [ ] **Step 3: Add frontend fields**

Extend `Product` and API payloads with:

```ts
address: string
latitude?: number
longitude?: number
```

Add an address form item in the mall drawer and show the address in product cards/details where helpful.

- [ ] **Step 4: Run targeted frontend verification**

Run: `npm test -- src/utils/productCatalog.test.ts`
Expected: PASS

### Task 4: Sort recommendation products by distance

**Files:**
- Modify: `C:\Users\Lq304\Desktop\travel\travel_frontend\src\composables\useRecentTripRecommendations.ts`
- Modify: `C:\Users\Lq304\Desktop\travel\travel_frontend\src\utils\recentTripPrompt.ts`
- Modify: `C:\Users\Lq304\Desktop\travel\travel_frontend\src\pages\workspace\WorkspacePage.vue`
- Modify: `C:\Users\Lq304\Desktop\travel\travel_frontend\src\types\chat.ts`
- Modify: `C:\Users\Lq304\Desktop\travel\travel_frontend\src\pages\workspace\components\ChatRecommendationCards.vue`
- Modify: `C:\Users\Lq304\Desktop\travel\travel_frontend\src\composables\useRecentTripRecommendations.test.ts`
- Modify: `C:\Users\Lq304\Desktop\travel\travel_frontend\src\utils\recentTripPrompt.test.ts`

- [ ] **Step 1: Write the failing recommendation tests**

Cover:
1. nearer products sort ahead of farther same-city products
2. products without coordinates sort after products with coordinates
3. prompt text still includes matched products in sorted order

- [ ] **Step 2: Run targeted frontend tests**

Run: `npm test -- src/composables/useRecentTripRecommendations.test.ts src/utils/recentTripPrompt.test.ts`
Expected: FAIL until distance-aware ordering exists.

- [ ] **Step 3: Implement minimal distance ranking**

Use a pure helper that computes Haversine distance when trip context exposes a reference coordinate from itinerary activity locations. Fall back to city-only ordering when coordinates are unavailable.

- [ ] **Step 4: Surface distance metadata**

Include optional `distanceText` in recommendation card payloads so the UI can label nearby options, for example `距当日行程 1.2km`.

- [ ] **Step 5: Run targeted tests**

Run: `npm test -- src/composables/useRecentTripRecommendations.test.ts src/utils/recentTripPrompt.test.ts`
Expected: PASS

### Task 5: Full verification

**Files:**
- Modify: `C:\Users\Lq304\Desktop\travel\travel_frontend\src\pages\mall\ProductDetailPage.vue` (only if address display is added)
- Verify existing touched files above

- [ ] **Step 1: Run frontend type-check**

Run: `npm run type-check`
Expected: PASS

- [ ] **Step 2: Run relevant frontend tests**

Run: `npm test -- src/utils/productCatalog.test.ts src/composables/useRecentTripRecommendations.test.ts src/utils/recentTripPrompt.test.ts src/utils/productOrder.test.ts src/router/index.test.ts src/utils/productCoverUpload.test.ts`
Expected: PASS

- [ ] **Step 3: Run backend compile and targeted tests**

Run: `mvn -DskipTests compile`

Run: `mvn "-Dtest=ProductServiceImplTest" test`

Expected: PASS
