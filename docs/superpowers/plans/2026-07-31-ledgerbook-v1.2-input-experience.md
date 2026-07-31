# LedgerBook Lite V1.2 Input Experience Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a signed V1.2 APK with system-bar-safe layout, calculator amount entry, icon category selection, account cards, wheel date/time, and reusable bookkeeper selection.

**Architecture:** Keep the existing Java View/SQLite application and add focused pure-Java domain helpers plus Android dialog components. `LedgerV12Activity` coordinates the UI while calculation, category, calendar, and bookkeeper rules remain independently testable.

**Tech Stack:** Java 17, Android SDK 35, SQLiteOpenHelper, Android View widgets, JUnit 4, GitHub Actions, fixed RSA sideload signing certificate.

## Global Constraints

- Application ID remains `com.ledgerbook.lite`.
- Minimum SDK remains 26 and target/compile SDK remains 35.
- Existing V1.1 SQLite schema is unchanged.
- versionCode is 3 and versionName is `1.2.0`.
- Fixed V1.1 sideload signing certificate must be reused.
- No third-party runtime dependency is added.
- All monetary calculations use `BigDecimal`; stored amounts remain integer cents.

---

### Task 1: Pure-Java Input Rules

**Files:**
- Create: `app/src/main/java/com/ledgerbook/lite/AmountExpression.java`
- Create: `app/src/main/java/com/ledgerbook/lite/InputCatalog.java`
- Create: `app/src/main/java/com/ledgerbook/lite/DateWheelRules.java`
- Test: `app/src/test/java/com/ledgerbook/lite/AmountExpressionTest.java`
- Test: `app/src/test/java/com/ledgerbook/lite/InputCatalogTest.java`
- Test: `app/src/test/java/com/ledgerbook/lite/DateWheelRulesTest.java`

**Interfaces:**
- Produces: `AmountExpression.evaluate(String): BigDecimal`
- Produces: `InputCatalog.categories(String): List<Option>` and `InputCatalog.mergeBookkeepers(Set<String>): List<String>`
- Produces: `DateWheelRules.daysInMonth(int,int): int`

- [ ] Write tests for precedence (`12.5+8*2=28.5`), parentheses-free subtraction, decimal division, division by zero, and malformed expressions.
- [ ] Run `gradle :app:testDebugUnitTest` and verify missing classes fail compilation.
- [ ] Implement recursive-descent expression parsing with `BigDecimal` and scale-10 division.
- [ ] Implement icon/category catalogs and stable bookkeeper merge order.
- [ ] Implement Gregorian month-day rules with leap years.
- [ ] Run tests and commit `feat: add V1.2 input rules`.

### Task 2: Reusable Dialog Components

**Files:**
- Create: `app/src/main/java/com/ledgerbook/lite/AmountCalculatorDialog.java`
- Create: `app/src/main/java/com/ledgerbook/lite/CategoryPickerDialog.java`
- Create: `app/src/main/java/com/ledgerbook/lite/AccountPickerDialog.java`
- Create: `app/src/main/java/com/ledgerbook/lite/WheelDateTimeDialog.java`
- Create: `app/src/main/java/com/ledgerbook/lite/BookkeeperPickerDialog.java`

**Interfaces:**
- Produces callback-based `show(...)` methods returning cents, category, account, timestamp, or bookkeeper.
- Consumes: `CartoonStyle`, `AmountExpression`, `InputCatalog`, `DateWheelRules`, and `LedgerDb.Account`.

- [ ] Build the four-column amount keypad with expression/result display and positive-result validation.
- [ ] Build three-column category grid with custom category entry.
- [ ] Build scrollable account cards showing icon/type/balance.
- [ ] Build five `NumberPicker` wheels with dynamic day maximum.
- [ ] Build preset/history/custom bookkeeper picker backed by SharedPreferences.
- [ ] Compile with `gradle :app:compileDebugJavaWithJavac` and commit `feat: add structured input dialogs`.

### Task 3: V1.2 Activity and Insets

**Files:**
- Create: `app/src/main/java/com/ledgerbook/lite/LedgerV12Activity.java`
- Modify: `app/src/main/AndroidManifest.xml`

**Interfaces:**
- Consumes all Task 2 dialog callbacks.
- Preserves existing `LedgerDb` transaction insertion and deletion calls.

- [ ] Recreate the V1.1 cartoon shell and attach `WindowInsets` listener to top and bottom wrappers.
- [ ] Replace amount and discount EditTexts with clickable display cards.
- [ ] Replace category, account, time, and bookkeeper fields with clickable selection cards.
- [ ] Keep transaction type switching, transfer target visibility, tags, reimbursement, budget, note, save, and delete behavior.
- [ ] Point the launcher manifest to `LedgerV12Activity`.
- [ ] Compile and commit `feat: integrate V1.2 recording experience`.

### Task 4: Version, CI, and Signed APK

**Files:**
- Modify: `app/build.gradle`
- Modify: `.github/workflows/build-apk.yml`

**Interfaces:**
- Produces: `LedgerBook-Lite-v1.2.0.apk` and SHA-256 file.

- [ ] Set versionCode 3 and versionName `1.2.0`.
- [ ] Add the V1.2 branch to workflow triggers and remove the V1.1 source patch step.
- [ ] Run unit tests, assemble debug APK, and verify APK v2 signature.
- [ ] Upload APK and SHA-256 artifact.
- [ ] Inspect CI steps and commit `build: publish signed V1.2 APK`.

### Task 5: Final Verification

**Files:**
- Review all changed files and CI logs.

- [ ] Verify unit-test step succeeds.
- [ ] Verify Android APK build succeeds.
- [ ] Verify `apksigner` reports v2 scheme true and the same certificate SHA-256 as V1.1.
- [ ] Download artifact, verify ZIP integrity and APK SHA-256.
- [ ] Deliver APK with upgrade and data-preservation notes.
