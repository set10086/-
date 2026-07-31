# LedgerBook Lite V1.3 Navigation, Search, and Calendar Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add ledger and bill drawers, scoped text search, a monthly calendar page, and a global quick-entry button while preserving V1.2 data and signing compatibility.

**Architecture:** Keep the native Java View and SQLite stack. Add pure-Java query/date rules, extend `LedgerDb` with bound-parameter queries and transactional ledger management, and introduce a new `LedgerV13Activity` that coordinates reusable search, drawer, calendar, and detail components. Existing amount, category, account, time, and bookkeeper pickers remain unchanged and are reused by the V1.3 entry form.

**Tech Stack:** Java 17, Android SDK 35, minSdk 26, native Android Views, SQLite, JUnit 4, GitHub Actions, existing fixed JKS signing key.

## Global Constraints

- Application ID remains `com.ledgerbook.lite`.
- Minimum Android version remains Android 8.0 / API 26.
- Do not change the existing SQLite table schema or database version.
- Preserve V1.2 local data and fixed signing certificate compatibility.
- Bottom navigation is exactly `首页｜日历｜账户｜统计`.
- Search defaults to the current ledger and can switch to all ledgers.
- Bill-drawer time presets are exactly `本月｜上月｜本年｜自定义日期范围`.
- Calendar day cells show daily expense, a green income dot, and a blue transfer dot when relevant.
- Do not add a “我的” page or transaction editing in V1.3.

---

## File Map

**Create**
- `app/src/main/java/com/ledgerbook/lite/TransactionFilter.java`: immutable filter state used by database and UI.
- `app/src/main/java/com/ledgerbook/lite/TransactionQueryRules.java`: date bounds, search escaping, and 42-cell calendar calculations.
- `app/src/main/java/com/ledgerbook/lite/TransactionDetailDialog.java`: shared read-only transaction detail and delete action.
- `app/src/main/java/com/ledgerbook/lite/TransactionSearchDialog.java`: full-screen scoped text search.
- `app/src/main/java/com/ledgerbook/lite/CalendarPageView.java`: month grid and selected-day transaction list.
- `app/src/main/java/com/ledgerbook/lite/LedgerDrawerView.java`: left ledger drawer and management entry.
- `app/src/main/java/com/ledgerbook/lite/BillDrawerView.java`: right filtered bill timeline.
- `app/src/main/java/com/ledgerbook/lite/LedgerV13Activity.java`: application shell and global state coordinator.
- `app/src/test/java/com/ledgerbook/lite/TransactionQueryRulesTest.java`
- `app/src/test/java/com/ledgerbook/lite/TransactionFilterTest.java`

**Modify**
- `app/src/main/java/com/ledgerbook/lite/LedgerDb.java`: query models, search/filter/calendar queries, detail lookup, ledger rename/delete/count operations.
- `app/src/main/AndroidManifest.xml`: launch `LedgerV13Activity`.
- `app/build.gradle`: versionCode 4, versionName 1.3.0.
- `.github/workflows/build-apk.yml`: build and publish `LedgerBook-Lite-v1.3.0.apk`.

---

### Task 1: Query and Calendar Rules

**Files:**
- Create: `app/src/test/java/com/ledgerbook/lite/TransactionQueryRulesTest.java`
- Create: `app/src/test/java/com/ledgerbook/lite/TransactionFilterTest.java`
- Create: `app/src/main/java/com/ledgerbook/lite/TransactionQueryRules.java`
- Create: `app/src/main/java/com/ledgerbook/lite/TransactionFilter.java`

**Interfaces:**
- Produces: `TransactionQueryRules.escapeLike(String)`, `dayBounds(LocalDate, ZoneId)`, `monthBounds(YearMonth, ZoneId)`, `previousMonthBounds(YearMonth, ZoneId)`, `yearBounds(int, ZoneId)`, `monthGrid(YearMonth)`, `compactAmount(long)`.
- Produces: immutable `TransactionFilter` with `ledgerId`, `fromInclusive`, `toExclusive`, `type`, `category`, `accountId`, and `bookkeeper`.

- [ ] **Step 1: Write failing rule tests**

```java
@Test public void escapeLikeTreatsWildcardsAsText() {
    assertEquals("100\\%\\_\\\\", TransactionQueryRules.escapeLike("100%_\\"));
}

@Test public void monthGridAlwaysContainsFortyTwoMondayFirstDates() {
    List<LocalDate> cells = TransactionQueryRules.monthGrid(YearMonth.of(2026, 8));
    assertEquals(42, cells.size());
    assertEquals(DayOfWeek.MONDAY, cells.get(0).getDayOfWeek());
    assertEquals(LocalDate.of(2026, 7, 27), cells.get(0));
}

@Test public void transactionFilterNormalizesBlankValues() {
    TransactionFilter filter = new TransactionFilter(-1, 10, 20, " ", null, 0, "  ");
    assertNull(filter.type);
    assertNull(filter.category);
    assertNull(filter.bookkeeper);
    assertEquals(0, filter.accountId);
}
```

- [ ] **Step 2: Run tests and verify RED**

Run: `gradle --no-daemon :app:testDebugUnitTest --tests '*TransactionQueryRulesTest' --tests '*TransactionFilterTest'`

Expected: compilation failure because the production classes do not exist.

- [ ] **Step 3: Implement minimal pure-Java rules and filter model**

Use `java.time`, `ZoneId`, and exact `[fromInclusive, toExclusive)` ranges. `escapeLike` must escape `\\`, `%`, and `_` in that order. `monthGrid` must back up to Monday and return exactly 42 dates.

- [ ] **Step 4: Run all unit tests and verify GREEN**

Run: `gradle --no-daemon :app:testDebugUnitTest`

Expected: all existing and new tests pass.

- [ ] **Step 5: Commit**

Commit message: `feat: add V1.3 transaction query rules`

---

### Task 2: Extend the SQLite Query and Ledger Management Layer

**Files:**
- Modify: `app/src/main/java/com/ledgerbook/lite/LedgerDb.java`

**Interfaces:**
- Consumes: `TransactionFilter` and date bounds from Task 1.
- Produces:
  - `List<TxnView> searchTransactions(Long ledgerId, String query, int offset, int limit)`
  - `List<TxnView> getFilteredTransactions(TransactionFilter filter, int offset, int limit)`
  - `List<TxnView> getTransactionsForDay(long ledgerId, long from, long to)`
  - `List<DaySummary> getMonthDaySummaries(long ledgerId, long from, long to)`
  - `TxnView getTransaction(long transactionId)`
  - `List<String> getBookkeepers(Long ledgerId)`
  - `List<Account> getAccountsForScope(Long ledgerId)`
  - `LedgerCounts getLedgerCounts(long ledgerId)`
  - `void renameLedger(long ledgerId, String name)`
  - `long deleteLedger(long ledgerId)` returning the next active ledger ID.

- [ ] **Step 1: Add compile-time API tests to `TransactionFilterTest`**

Add assertions for valid all-ledger (`ledgerId=-1`) and exact-ledger filters, invalid inverted ranges, and stable `equals/hashCode` behavior.

- [ ] **Step 2: Run tests and verify RED**

Expected: tests fail until validation and equality are implemented.

- [ ] **Step 3: Add database display models and bound queries**

`TxnView` must include transaction fields plus `ledgerId`, `ledgerName`, and `currency`. Build SQL from fixed clauses only; all user values must be `?` bindings. Search must use `LIKE ? ESCAPE '\\'` over category, note, tags, bookkeeper, both account names, and ledger name in all-ledger scope.

- [ ] **Step 4: Add transactional ledger management**

`deleteLedger` must reject deletion when only one ledger remains, collect the next ledger before deleting, then delete transactions, accounts, and ledger inside one transaction. `renameLedger` must reject blank names.

- [ ] **Step 5: Run unit tests and Android Java compilation**

Run:
- `gradle --no-daemon :app:testDebugUnitTest`
- `gradle --no-daemon :app:compileDebugJavaWithJavac`

Expected: both succeed.

- [ ] **Step 6: Commit**

Commit message: `feat: add scoped transaction queries and ledger management`

---

### Task 3: Shared Transaction Detail and Search

**Files:**
- Create: `app/src/main/java/com/ledgerbook/lite/TransactionDetailDialog.java`
- Create: `app/src/main/java/com/ledgerbook/lite/TransactionSearchDialog.java`

**Interfaces:**
- Consumes: `LedgerDb.TxnView`, `LedgerDb.searchTransactions`, `LedgerDb.deleteTransaction`.
- Produces: `TransactionSearchDialog.show(Activity, LedgerDb, long currentLedgerId, Listener)` and `TransactionDetailDialog.show(...)`.

- [ ] **Step 1: Add pure helper tests for search result text**

Add a package-private static formatter method in `TransactionSearchDialog` only after a failing test demonstrates that blank optional fields are omitted and ledger name appears only in all-ledger mode.

- [ ] **Step 2: Verify RED**

Run the formatter test and confirm missing method failure.

- [ ] **Step 3: Implement full-screen search dialog**

Use a full-screen `Dialog`, an `EditText`, current/all ledger segmented controls, a 300ms `Handler` debounce, `ScrollView` results, 200-row initial limit, and a “继续加载” control. Empty query shows instructions; no result shows an empty state.

- [ ] **Step 4: Implement shared detail dialog**

Show type, category, amount, ledger, accounts, timestamp, bookkeeper, tags, note, discount, reimbursable, and budget state. Provide close and delete; deletion calls a listener so every caller refreshes.

- [ ] **Step 5: Compile and inspect warnings**

Run: `gradle --no-daemon :app:compileDebugJavaWithJavac`

Expected: success with no new compile errors.

- [ ] **Step 6: Commit**

Commit message: `feat: add transaction search and detail views`

---

### Task 4: Calendar Page

**Files:**
- Create: `app/src/main/java/com/ledgerbook/lite/CalendarPageView.java`

**Interfaces:**
- Consumes: `TransactionQueryRules.monthGrid`, `LedgerDb.getMonthDaySummaries`, `LedgerDb.getTransactionsForDay`, and `TransactionDetailDialog`.
- Produces: `CalendarPageView(Context, LedgerDb, Listener)`, `setLedger(long)`, `setMonth(YearMonth)`, `getMonth()`, and `getSelectedDate()`.

- [ ] **Step 1: Add failing compact amount and day-state tests**

Cover `0`, values below 10,000, `1.2万`, and negative values. Cover income and transfer marker selection.

- [ ] **Step 2: Verify RED**

Run Task 1 test class; expect failures for missing compact rules.

- [ ] **Step 3: Implement month navigation and 42-cell grid**

Use `GridLayout` with Monday–Sunday headers, previous/today/next controls, muted adjacent-month dates, today outline, selected background, daily expense text, green income dot, and blue transfer dot.

- [ ] **Step 4: Implement selected-day bill list**

Clicking a cell loads the natural-day range in the system timezone. Adjacent-month clicks switch month first. Empty days show a “记一笔” entry. Detail and long-delete actions refresh both grid summary and list.

- [ ] **Step 5: Compile**

Run: `gradle --no-daemon :app:compileDebugJavaWithJavac`

- [ ] **Step 6: Commit**

Commit message: `feat: add monthly transaction calendar`

---

### Task 5: Left Ledger Drawer

**Files:**
- Create: `app/src/main/java/com/ledgerbook/lite/LedgerDrawerView.java`

**Interfaces:**
- Consumes: ledger list, current-month summaries, `getLedgerCounts`, `renameLedger`, `deleteLedger`, and existing ledger creation flow.
- Produces: listener callbacks `onLedgerSelected(long)`, `onLedgerCreated(long)`, `onLedgerDeleted(long)`, and `onClose()`.

- [ ] **Step 1: Add failing ledger-management validation tests**

Extend pure validation coverage for blank rename and deletion of the last ledger through helper methods extracted into `LedgerManagementRules` only if needed; do not place test-only logic in production.

- [ ] **Step 2: Implement drawer content**

Width is `min(84% screen, 340dp)`. Highlight current ledger, show currency and month income/expense, and keep “账本管理” fixed at the bottom.

- [ ] **Step 3: Implement management dialog**

Support create, rename, switch, and delete. Delete confirmation must show account and transaction counts and explicitly say data cannot be recovered.

- [ ] **Step 4: Compile and manually inspect callback flow**

Run: `gradle --no-daemon :app:compileDebugJavaWithJavac`.

- [ ] **Step 5: Commit**

Commit message: `feat: add ledger drawer and management`

---

### Task 6: Right Bill Drawer and Filters

**Files:**
- Create: `app/src/main/java/com/ledgerbook/lite/BillDrawerView.java`

**Interfaces:**
- Consumes: `TransactionFilter`, date rules, ledger/account/bookkeeper catalogs, `getFilteredTransactions`, and `TransactionDetailDialog`.
- Produces: `setCurrentLedger(long)`, `open()`, `close()`, `hasActiveFilters()`, and listener `onFilterStateChanged(boolean)`.

- [ ] **Step 1: Add failing preset date-range tests**

Cover current month, previous month across January, current year, and invalid custom ranges.

- [ ] **Step 2: Verify RED then implement date presets**

Use system timezone and `[from, to)` boundaries.

- [ ] **Step 3: Implement filter controls**

Top row contains time and ledger selectors; right-side detailed filter dialog includes type, dynamic category, account, and bookkeeper. Clearing filters resets type/category/account/bookkeeper while retaining default current-ledger and current-month scope.

- [ ] **Step 4: Implement grouped bill timeline**

Group by natural date, show daily income/expense totals, ledger name in cross-ledger scope, detail click, delete long-press, and a no-results summary with clear button.

- [ ] **Step 5: Compile**

Run: `gradle --no-daemon :app:compileDebugJavaWithJavac`.

- [ ] **Step 6: Commit**

Commit message: `feat: add filtered bill drawer`

---

### Task 7: V1.3 Application Shell and Global Quick Entry

**Files:**
- Create: `app/src/main/java/com/ledgerbook/lite/LedgerV13Activity.java`
- Modify: `app/src/main/AndroidManifest.xml`

**Interfaces:**
- Consumes all components from Tasks 3–6 and existing V1.2 pickers.
- Produces the launch activity and shared global ledger/page state.

- [ ] **Step 1: Implement shell layout**

Use a root `FrameLayout`; main vertical content has a 56dp header, page container, and four-item bottom nav. Header uses left current-ledger control, geometrically centered search icon, and right bill icon with active-filter dot.

- [ ] **Step 2: Implement mutually exclusive animated drawers**

Use overlay shade plus 180–220ms translation animations. Back closes dialog/drawer before exiting. Opening one drawer closes the other.

- [ ] **Step 3: Implement pages and bottom navigation**

Port V1.2 home, account, and statistics behavior; replace bills with `CalendarPageView`. Preserve calendar month and selected day during page switches.

- [ ] **Step 4: Add global circular plus button**

Place a 58dp round button 16dp above the bottom bar and 16dp from the right. It must appear on all four pages. Calendar-origin entry preselects the selected date while preserving current clock time.

- [ ] **Step 5: Persist current ledger**

Store current ledger ID in `SharedPreferences`. Validate on startup and fall back to the first ledger when missing or deleted.

- [ ] **Step 6: Reuse and adapt V1.2 transaction form**

Move the form logic into V1.3 or a focused helper without changing amount calculator, category picker, account picker, time wheel, bookkeeper picker, or database insert semantics.

- [ ] **Step 7: Compile and run all unit tests**

Run:
- `gradle --no-daemon :app:testDebugUnitTest`
- `gradle --no-daemon :app:assembleDebug`

Expected: both succeed.

- [ ] **Step 8: Commit**

Commit message: `feat: integrate V1.3 navigation shell`

---

### Task 8: Version, CI, Signing, and Release Verification

**Files:**
- Modify: `app/build.gradle`
- Modify: `.github/workflows/build-apk.yml`

**Interfaces:**
- Produces: `LedgerBook-Lite-v1.3.0.apk` and matching `.sha256`.

- [ ] **Step 1: Set release metadata**

Set `versionCode 4` and `versionName "1.3.0"`. Keep the existing fixed signing configuration.

- [ ] **Step 2: Update CI artifact names**

CI order must be unit tests, APK build, `apksigner verify --verbose --print-certs`, SHA-256 generation, and artifact upload.

- [ ] **Step 3: Run fresh full verification**

Run:
- `gradle --no-daemon :app:testDebugUnitTest`
- `gradle --no-daemon :app:assembleDebug`
- `$ANDROID_HOME/build-tools/35.0.0/apksigner verify --verbose --print-certs app/build/outputs/apk/debug/app-debug.apk`
- `unzip -t app/build/outputs/apk/debug/app-debug.apk`
- `sha256sum app/build/outputs/apk/debug/app-debug.apk`

Expected: zero test failures, successful build, v2 signature verification, no ZIP errors, and a generated digest.

- [ ] **Step 4: Confirm signing compatibility**

Certificate SHA-256 must remain `88a65dc9225510ea4266e1cc296ae8db3f730083dfc9c23fd35271128a30830e`.

- [ ] **Step 5: Commit**

Commit message: `build: release LedgerBook Lite V1.3.0`

---

## Plan Self-Review

- Spec coverage: header actions, left/right drawers, scoped search, detailed filters, four-item bottom nav, calendar, day bills, global plus button, ledger management, data safety, versioning, and fixed signing are each assigned to a task.
- Placeholder scan: no TBD, TODO, deferred behavior, or undefined implementation step remains.
- Type consistency: `TransactionFilter`, `TxnView`, date rules, and component callbacks use the same names across producing and consuming tasks.
- Scope: V1.3 intentionally excludes “我的”, transaction editing, amount-range filters, cloud sync, and database schema migration.
