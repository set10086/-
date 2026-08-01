#!/usr/bin/env python3
"""Generate LedgerV14Activity.java from the verified V1.3 activity source.

The generator intentionally uses exact anchors and aborts when the baseline changes.
This prevents a partially patched launcher from being packaged silently.
"""

from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "app/src/main/java/com/ledgerbook/lite/LedgerV13Activity.java"
TARGET = ROOT / "app/src/main/java/com/ledgerbook/lite/LedgerV14Activity.java"


def replace_once(text: str, needle: str, replacement: str, label: str) -> str:
    count = text.count(needle)
    if count != 1:
        raise SystemExit(f"{label}: expected exactly one match, found {count}")
    return text.replace(needle, replacement, 1)


def main() -> None:
    text = SOURCE.read_text(encoding="utf-8")

    text = replace_once(
        text,
        "public final class LedgerV13Activity extends Activity {",
        "public final class LedgerV14Activity extends Activity {",
        "activity class",
    )
    text = replace_once(
        text,
        '    private static final String PREFS = "ledgerbook_v13";',
        '    private static final String PREFS = "ledgerbook_v14";',
        "preferences namespace",
    )
    text = replace_once(
        text,
        "    private static final int STATS = 3;\n\n    private final TextView[] navItems = new TextView[4];",
        "    private static final int STATS = 3;\n"
        "    private static final int SETTINGS = 4;\n\n"
        "    private final TextView[] navItems = new TextView[5];",
        "settings navigation constant",
    )
    text = replace_once(
        text,
        "    private LedgerDb db;\n    private FrameLayout root;",
        "    private LedgerDb db;\n"
        "    private AppSettings appSettings;\n"
        "    private FrameLayout root;",
        "settings field",
    )
    text = replace_once(
        text,
        "    private int currentPage = HOME;\n    private int bottomInset;",
        "    private int currentPage = HOME;\n"
        "    private int topInset;\n"
        "    private int bottomInset;",
        "inset fields",
    )
    text = replace_once(
        text,
        "        db = new LedgerDb(this);\n        db.ensureDefaults();\n        buildShell();",
        "        db = new LedgerDb(this);\n"
        "        db.ensureDefaults();\n"
        "        appSettings = new AppSettings(this);\n"
        "        buildShell();",
        "settings initialization",
    )
    text = replace_once(
        text,
        '        addNav(nav, STATS, "📊\\n统计");',
        '        addNav(nav, STATS, "📊\\n统计");\n'
        '        addNav(nav, SETTINGS, "⚙️\\n设置");',
        "settings nav item",
    )
    text = replace_once(
        text,
        "            int top = insets.getSystemWindowInsetTop();\n"
        "            bottomInset = insets.getSystemWindowInsetBottom();\n"
        "            headerWrap.setPadding(V13Ui.dp(this, 10), V13Ui.dp(this, 8) + top,",
        "            topInset = insets.getSystemWindowInsetTop();\n"
        "            bottomInset = insets.getSystemWindowInsetBottom();\n"
        "            headerWrap.setPadding(V13Ui.dp(this, 10), V13Ui.dp(this, 8) + topInset,",
        "root inset cache",
    )
    text = replace_once(
        text,
        "        long saved = getSharedPreferences(PREFS, MODE_PRIVATE).getLong(PREF_LEDGER, -1L);\n"
        "        currentLedgerId = containsLedger(saved) ? saved : ledgers.get(0).id;\n"
        "        persistLedger();\n"
        "        updateLedgerButton();\n"
        "        showPage();",
        "        long saved = getSharedPreferences(PREFS, MODE_PRIVATE).getLong(PREF_LEDGER, -1L);\n"
        "        long configured = appSettings.defaultLedgerId();\n"
        "        long preferred = containsLedger(configured) ? configured : saved;\n"
        "        currentLedgerId = containsLedger(preferred) ? preferred : ledgers.get(0).id;\n"
        "        appSettings.setDefaultLedgerId(currentLedgerId);\n"
        "        persistLedger();\n"
        "        updateLedgerButton();\n"
        "        currentPage = appSettings.startupPage();\n"
        "        showPage();",
        "initial ledger and page",
    )
    text = replace_once(
        text,
        "        int[] fills = {CartoonStyle.SOFT_YELLOW, CartoonStyle.SOFT_PEACH,\n"
        "                CartoonStyle.SOFT_SKY, CartoonStyle.SOFT_GREEN};",
        "        int[] fills = {CartoonStyle.SOFT_YELLOW, CartoonStyle.SOFT_PEACH,\n"
        "                CartoonStyle.SOFT_SKY, CartoonStyle.SOFT_GREEN,\n"
        "                CartoonStyle.SOFT_LAVENDER};",
        "five navigation fills",
    )
    text = replace_once(
        text,
        "        ScrollView scroll = new ScrollView(this);",
        "        if (currentPage == SETTINGS) {\n"
        "            SettingsPageView settingsPage = new SettingsPageView(\n"
        "                    this, db, appSettings, currentLedgerId,\n"
        "                    new SettingsPageView.Listener() {\n"
        "                        @Override public void onLedgerSelected(long ledgerId) {\n"
        "                            switchLedger(ledgerId);\n"
        "                        }\n"
        "                        @Override public void onOpenLedgerManagement() {\n"
        "                            openLedgerDrawer();\n"
        "                        }\n"
        "                        @Override public void onOpenAccounts() {\n"
        "                            currentPage = ACCOUNTS;\n"
        "                            showPage();\n"
        "                        }\n"
        "                        @Override public void onSettingsChanged() { }\n"
        "                    });\n"
        "            pageContainer.addView(settingsPage, new FrameLayout.LayoutParams(\n"
        "                    ViewGroup.LayoutParams.MATCH_PARENT,\n"
        "                    ViewGroup.LayoutParams.MATCH_PARENT));\n"
        "            return;\n"
        "        }\n"
        "        ScrollView scroll = new ScrollView(this);",
        "settings page route",
    )
    text = replace_once(
        text,
        "        root.addView(drawer, params);\n        openDrawer = drawer;",
        "        root.addView(drawer, params);\n"
        "        applyDrawerInsets(drawer);\n"
        "        drawer.requestApplyInsets();\n"
        "        openDrawer = drawer;",
        "drawer inset dispatch",
    )
    text = replace_once(
        text,
        "        shade.animate().alpha(1f).setDuration(200L).start();\n"
        "        drawer.animate().translationX(0f).setDuration(210L).start();",
        "        if (appSettings.animationsEnabled()) {\n"
        "            shade.animate().alpha(1f).setDuration(200L).start();\n"
        "            drawer.animate().translationX(0f).setDuration(210L).start();\n"
        "        } else {\n"
        "            shade.setAlpha(1f);\n"
        "            drawer.setTranslationX(0f);\n"
        "        }",
        "drawer open animation setting",
    )
    text = replace_once(
        text,
        "    private void closeDrawer() {\n"
        "        if (openDrawer == null) return;\n"
        "        View closing = openDrawer;\n"
        "        int width = closing.getWidth();\n"
        "        closing.animate().translationX(openDrawerLeft ? -width : width)\n"
        "                .setDuration(190L).withEndAction(() -> {\n"
        "                    root.removeView(closing);\n"
        "                    if (openDrawer == closing) openDrawer = null;\n"
        "                }).start();\n"
        "        shade.animate().alpha(0f).setDuration(180L).withEndAction(() -> {\n"
        "            shade.setVisibility(View.GONE);\n"
        "            shade.setAlpha(0f);\n"
        "        }).start();\n"
        "    }",
        "    private void closeDrawer() {\n"
        "        if (openDrawer == null) return;\n"
        "        if (!appSettings.animationsEnabled()) {\n"
        "            closeDrawerImmediately();\n"
        "            return;\n"
        "        }\n"
        "        View closing = openDrawer;\n"
        "        int width = closing.getWidth();\n"
        "        closing.animate().translationX(openDrawerLeft ? -width : width)\n"
        "                .setDuration(190L).withEndAction(() -> {\n"
        "                    root.removeView(closing);\n"
        "                    if (openDrawer == closing) openDrawer = null;\n"
        "                }).start();\n"
        "        shade.animate().alpha(0f).setDuration(180L).withEndAction(() -> {\n"
        "            shade.setVisibility(View.GONE);\n"
        "            shade.setAlpha(0f);\n"
        "        }).start();\n"
        "    }",
        "drawer close animation setting",
    )
    text = replace_once(
        text,
        "        LedgerDb.Account[] source = {accounts.get(0)};",
        "        LedgerDb.Account[] source = {preferredAccount(accounts)};",
        "default account use",
    )
    text = replace_once(
        text,
        '        String[] bookkeeper = {"本人"};',
        "        String[] bookkeeper = {appSettings.defaultBookkeeper()};",
        "default bookkeeper use",
    )
    text = replace_once(
        text,
        '                        toast("记好啦");\n                        refreshAll();',
        '                        toast("记好啦");\n'
        '                        if (appSettings.returnHomeAfterSave()) currentPage = HOME;\n'
        '                        refreshAll();',
        "post-save destination",
    )

    helper_methods = r'''

    private void applyDrawerInsets(View drawer) {
        if (drawer instanceof LedgerDrawerView) {
            ledgerDrawer.applySystemInsets(topInset, bottomInset);
        } else if (drawer instanceof BillDrawerView) {
            int side = V13Ui.dp(this, 12);
            int top = V13Ui.dp(this, 14) + topInset;
            int bottom = V13Ui.dp(this, 14) + bottomInset;
            drawer.setPadding(side, top, side, bottom);
        }
    }

    private LedgerDb.Account preferredAccount(List<LedgerDb.Account> accounts) {
        long preferredId = appSettings.defaultAccountId(currentLedgerId);
        for (LedgerDb.Account account : accounts) {
            if (account.id == preferredId) return account;
        }
        LedgerDb.Account fallback = accounts.get(0);
        appSettings.setDefaultAccountId(currentLedgerId, fallback.id);
        return fallback;
    }
'''
    final_brace = text.rfind("\n}")
    if final_brace < 0:
        raise SystemExit("class closing brace not found")
    text = text[:final_brace] + helper_methods + text[final_brace:]

    TARGET.write_text(text, encoding="utf-8")
    print(f"Generated {TARGET.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
