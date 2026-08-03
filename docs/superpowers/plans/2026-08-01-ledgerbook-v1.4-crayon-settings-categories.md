# LedgerBook Lite V1.4 Crayon Settings and Categories Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build V1.4.0 with safe-area drawers, a large icon-backed two-level category picker, fast year/month calendar navigation, and a functional Settings tab while preserving V1.3 data and signing identity.

**Architecture:** Keep native Java Views and SQLite. Add focused pure-Java catalog/date/preference helpers and small Android views/dialogs. Generate the V1.4 activity deterministically from the verified V1.3 activity to avoid duplicating its large transaction form while still producing a normal compiled launcher activity.

**Tech Stack:** Java 17, Android SDK 35, minSdk 26, native Android Views, SQLite, SharedPreferences, JUnit 4, Python 3 source generator, GitHub Actions.

## Global Constraints

- Application ID remains `com.ledgerbook.lite`.
- Minimum SDK remains 26; target and compile SDK remain 35.
- SQLite schema and database version remain unchanged.
- Version becomes `1.4.0`, versionCode `5`.
- Existing stable sideload certificate remains unchanged.
- No official Crayon Shin-chan artwork, names in UI, logos, or copied reference-app icon files.
- Bottom navigation is exactly `首页｜日历｜账户｜统计｜设置`.

---

## File Map

**Create**
- `app/src/main/java/com/ledgerbook/lite/YearMonthPickerRules.java`: pure date range and shortcut rules.
- `app/src/main/java/com/ledgerbook/lite/YearMonthPickerDialog.java`: native year/month wheel dialog.
- `app/src/main/java/com/ledgerbook/lite/AppSettings.java`: SharedPreferences keys, validation and defaults.
- `app/src/main/java/com/ledgerbook/lite/SettingsPageView.java`: functional Settings page.
- `scripts/prepare_v14.py`: deterministic generator for `LedgerV14Activity.java`.
- `app/src/test/java/com/ledgerbook/lite/InputCatalogV14Test.java`: catalog tests.
- `app/src/test/java/com/ledgerbook/lite/YearMonthPickerRulesTest.java`: calendar jump tests.
- `app/src/test/java/com/ledgerbook/lite/V14SourceGeneratorTest.java`: generated activity contract tests.

**Modify**
- `InputCatalog.java`: two-level category data, search and icon lookup.
- `CategoryPickerDialog.java`: group rail, search field and icon grid.
- `CartoonStyle.java`: category icon lookup and refined crayon palette.
- `CalendarPageView.java`: clickable month title and year/month picker.
- `LedgerDrawerView.java`: explicit inset application API and attach-time inset request.
- `BillDrawerView.java`: explicit inset application API and attach-time inset request.
- `app/build.gradle`: source generation task, version 1.4.0.
- `AndroidManifest.xml`: launch generated V1.4 activity.
- `.github/workflows/build-apk.yml`: generate, test and publish V1.4 artifact.

---

### Task 1: Category catalog rules

**Interfaces:**
- Produces `InputCatalog.groups(String)`, `InputCatalog.categories(String)`, `InputCatalog.search(String,String)`, and `InputCatalog.iconFor(String,String)`.

- [ ] **Step 1: Write failing tests** in `InputCatalogV14Test.java` asserting:
  - expense catalog has at least 16 groups and 120 selectable options;
  - every option has non-empty `icon`, `label`, `name`, and `group`;
  - labels are unique;
  - search for `咖啡` returns `餐饮/咖啡`;
  - `iconFor("餐饮/咖啡", EXPENSE)` returns the coffee icon;
  - legacy `iconFor("餐饮", EXPENSE)` returns a non-empty group icon.
- [ ] **Step 2: Run** `gradle --no-daemon :app:testDebugUnitTest --tests com.ledgerbook.lite.InputCatalogV14Test` and confirm compilation/test failure because V1.4 APIs are missing.
- [ ] **Step 3: Implement** the catalog using immutable `Group` and `Option` values. Store full labels as `group + "/" + name`; flatten groups for existing filter consumers.
- [ ] **Step 4: Run the focused test** and confirm it passes.
- [ ] **Step 5: Commit** `feat: expand icon-backed category catalog`.

### Task 2: Category picker UI

**Interfaces:**
- Consumes `InputCatalog.groups()` and `InputCatalog.search()`.
- Preserves `CategoryPickerDialog.Listener.onCategory(String icon, String label)`.

- [ ] **Step 1: Replace** the three-column flat grid with a vertical layout containing a search EditText and horizontal content row.
- [ ] **Step 2: Add** a left ScrollView group rail (96dp) and right ScrollView four-column GridLayout.
- [ ] **Step 3: Render** each subcategory as icon + short name while returning the full stored label.
- [ ] **Step 4: Add** empty-search state and custom-category action.
- [ ] **Step 5: Run** the full unit suite and Android Java compilation with `gradle --no-daemon :app:testDebugUnitTest :app:compileDebugJavaWithJavac`.
- [ ] **Step 6: Commit** `feat: add grouped searchable category picker`.

### Task 3: Calendar month jumping

**Interfaces:**
- Produces `YearMonthPickerRules.minYear(int)`, `maxYear(int)`, `clamp(YearMonth,int)`, and shortcut methods.
- Produces `YearMonthPickerDialog.show(Activity, YearMonth, Listener)`.

- [ ] **Step 1: Write failing tests** for current-year bounds `-30/+10`, clamping, previous month, current-year January, and previous-year January.
- [ ] **Step 2: Run** focused tests and verify failure.
- [ ] **Step 3: Implement** pure rules and NumberPicker dialog with shortcuts.
- [ ] **Step 4: Modify** `CalendarPageView` so month title opens the dialog and applies the selected month.
- [ ] **Step 5: Run** focused tests plus Android compilation.
- [ ] **Step 6: Commit** `feat: add fast calendar year month picker`.

### Task 4: Drawer system-bar safety

**Interfaces:**
- `LedgerDrawerView.applySystemInsets(int top, int bottom)`.
- `BillDrawerView.applySystemInsets(int top, int bottom)`.

- [ ] **Step 1: Add** base padding constants and the explicit inset methods to both drawers.
- [ ] **Step 2: Override** `onAttachedToWindow()` in both drawers and call `requestApplyInsets()`.
- [ ] **Step 3: Ensure** each existing inset listener delegates to `applySystemInsets()`.
- [ ] **Step 4: Add** generator assertions that newly attached drawers receive cached insets and request fresh insets.
- [ ] **Step 5: Run** generator tests and Android compilation.
- [ ] **Step 6: Commit** `fix: apply safe areas to dynamically opened drawers`.

### Task 5: Settings page and generated V1.4 activity

**Interfaces:**
- `AppSettings` exposes default ledger/account/bookkeeper, startup page, animations, and return-home-after-save preferences.
- `SettingsPageView.Listener` exposes ledger switching, ledger management, and page refresh callbacks.
- `scripts/prepare_v14.py` writes `LedgerV14Activity.java`.

- [ ] **Step 1: Write generator test** that runs the Python script on a temporary copy of V1.3 source and asserts class name, five nav items, Settings route, drawer inset dispatch, and preference default use.
- [ ] **Step 2: Run** the test and verify failure because generator/settings files are absent.
- [ ] **Step 3: Implement** `AppSettings` with validated SharedPreferences getters/setters.
- [ ] **Step 4: Implement** `SettingsPageView` cards for theme information, defaults, startup page, animations, post-save behavior, management and about.
- [ ] **Step 5: Implement** idempotent `prepare_v14.py` with exact-anchor replacements and loud mismatch errors.
- [ ] **Step 6: Update** Gradle so `preBuild` runs the generator; set versionCode 5 and versionName 1.4.0.
- [ ] **Step 7: Update** manifest launcher to `.LedgerV14Activity`.
- [ ] **Step 8: Run** generator tests, all unit tests and Android compilation.
- [ ] **Step 9: Commit** `feat: add V1.4 settings navigation and preferences`.

### Task 6: Release workflow and verification

- [ ] **Step 1: Update** workflow artifact names to `LedgerBook-Lite-v1.4.0.apk` and `.sha256`.
- [ ] **Step 2: Ensure** workflow executes source generation before tests and build.
- [ ] **Step 3: Open** a draft PR from `ledgerbook-lite-v1.4-crayon-settings` to `main` to trigger CI.
- [ ] **Step 4: Verify** CI unit tests, debug APK build, v2 signature, ZIP integrity, and artifact upload all pass.
- [ ] **Step 5: Download** artifact, compare SHA-256 with supplied checksum, and run local `unzip -t`.
- [ ] **Step 6: Confirm** signer certificate SHA-256 remains `88a65dc9225510ea4266e1cc296ae8db3f730083dfc9c23fd35271128a30830e`.
- [ ] **Step 7: Update** completion status document with evidence.
- [ ] **Step 8: Commit** any final documentation-only update and re-run CI if executable files changed.
