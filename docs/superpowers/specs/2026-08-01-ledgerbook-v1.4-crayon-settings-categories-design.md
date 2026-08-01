# LedgerBook Lite V1.4 Crayon Settings and Categories Design

## Goal

Upgrade V1.3 to V1.4.0 with reliable system-bar-safe drawers, a five-item bottom navigation including Settings, a broad two-level category catalog with an independent icon for every selectable subcategory, and fast year/month calendar navigation. Preserve all V1.3 data and signing compatibility.

## Product direction

The visual language is an original family-crayon notebook theme: warm cream paper, yellow/peach/green/sky accents, rounded sticker-like controls, hand-drawn-feeling borders, and playful household copy. It must not include Crayon Shin-chan characters, logos, traced official art, or copied icon files from the reference application.

The uploaded screenshots are used only to derive information architecture and coverage: a vertical list of category groups and a dense grid of individually illustrated subcategories. V1.4 recreates that interaction with original Unicode/Android-rendered symbols and existing app styling.

## Scope

### 1. Drawer safe areas

- The left ledger drawer and right bill drawer must never overlap the status bar, display cutout, gesture navigation region, or three-button navigation region.
- Insets must be applied when a drawer is attached after the root view has already received system insets.
- The activity caches current top and bottom insets and applies them immediately to newly opened drawers.
- Drawers also request fresh insets when attached, covering rotation and navigation-mode changes.

### 2. Category catalog

- Replace the flat small catalog with a two-level catalog.
- Expense groups cover the reference screenshots: Default, Beauty, Family, Education, Other, Gardening, Renovation, Daily, Auto, Campus, Entertainment, Gifts, Property, Travel, Income-related payments, Dining, and Medical.
- Income groups cover salary, bonuses, side work, investments, reimbursements, gifts, loans, and other income.
- Every selectable subcategory has its own icon string; no selectable subcategory may have an empty icon.
- Stored category values use `Group/Subcategory`, for example `餐饮/早餐`. Existing flat values such as `餐饮` remain valid and display normally.
- The catalog exposes group lookup, flattened options, text search, and icon lookup for transaction lists and filters.
- The category picker uses a left group rail, a search field, and a four-column icon grid. Selecting a group filters the grid; search operates across all groups.
- Custom categories remain supported and use a pencil icon.

### 3. Calendar year/month navigation

- Clicking the calendar month title opens a dedicated year/month picker.
- Use native NumberPicker wheels for year and month.
- Year range is current year minus 30 through current year plus 10.
- Provide shortcuts for this month, previous month, this year January, and previous year January.
- Confirming the picker updates the visible month and selects the first day when the previous selected date is outside that month.
- Existing previous/next month buttons, swipe navigation, Today button, daily summaries, and selected-day transaction list remain.

### 4. Settings destination

- Bottom navigation becomes exactly `首页｜日历｜账户｜统计｜设置`.
- The persistent quick-add button remains visible above the bottom navigation on all five pages.
- Settings is a real page, not a placeholder. It contains:
  - original crayon-family theme information;
  - default ledger selection;
  - default account selection for the current ledger;
  - default bookkeeper selection;
  - startup page selection;
  - animation toggle;
  - post-save navigation toggle;
  - ledger management entry;
  - category catalog preview entry;
  - version and local-storage information.
- Preferences use SharedPreferences and do not require a database migration.
- The transaction form consumes default account and default bookkeeper preferences when valid.

### 5. Generated V1.4 activity

- Preserve the verified V1.3 activity source as the baseline.
- A deterministic source-generation script creates `LedgerV14Activity.java` from `LedgerV13Activity.java` before Android compilation.
- The generator must be idempotent and fail loudly if an expected source anchor is missing.
- The generated activity adds the Settings page, five-item navigation, drawer inset dispatch, preference defaults, and optional animation behavior.
- AndroidManifest launches `LedgerV14Activity`.

### 6. Compatibility

- Application ID remains `com.ledgerbook.lite`.
- Minimum SDK remains 26; target and compile SDK remain 35.
- SQLite database version and existing tables remain unchanged.
- Version becomes `1.4.0`, versionCode `5`.
- Use the existing stable sideload certificate so V1.2 and V1.3 can be upgraded in place.

## Error handling

- Empty category search shows a friendly empty state rather than an exception.
- Invalid saved ledger/account IDs fall back to the current valid ledger and first valid account.
- Year/month values are clamped to the supported picker range.
- Source generation aborts the build when anchors do not match exactly, preventing a silently incomplete V1.4 APK.

## Testing

- Pure Java unit tests cover category count, unique labels, non-empty icons, icon lookup, search, and legacy flat-category fallback.
- Pure Java unit tests cover year range and shortcut calculations.
- Source-generator tests verify five navigation items, Settings routing, drawer inset application, and V1.4 class generation.
- CI runs generation, unit tests, Android compilation, APK v2 signature verification, ZIP integrity verification, and SHA-256 generation.

## Out of scope for V1.4

- Copying or bundling image files from the reference screenshots.
- Official Crayon Shin-chan artwork or branding.
- Cloud synchronization, user accounts, biometric lock, Excel export, and schema-backed editable custom category persistence.
