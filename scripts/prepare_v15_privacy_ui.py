#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ACTIVITY = ROOT / "app/src/main/java/com/ledgerbook/lite/LedgerV14Activity.java"
SETTINGS = ROOT / "app/src/main/java/com/ledgerbook/lite/SettingsPageView.java"


def replace_once(text: str, needle: str, replacement: str, label: str) -> str:
    count = text.count(needle)
    if count != 1:
        raise SystemExit(f"{label}: expected one source anchor, found {count}")
    return text.replace(needle, replacement, 1)


def verify_activity(text: str) -> None:
    required = [
        "private PinStore pinStore;",
        "ModuleRepository.PRIVACY_LOCK",
        "PinUnlockDialog.show(",
        "protected void onResume()",
        "protected void onStop()",
        "pinStore.markBackground(",
        "pinStore.shouldLock(",
    ]
    missing = [value for value in required if value not in text]
    if missing:
        raise SystemExit("partially prepared LedgerV14Activity.java: " + ", ".join(missing))


def verify_settings(text: str) -> None:
    required = [
        "addPrivacyCard();",
        "private void addPrivacyCard()",
        "PrivacySettingsDialog.show(",
        "ModuleRepository.PRIVACY_LOCK",
    ]
    missing = [value for value in required if value not in text]
    if missing:
        raise SystemExit("partially prepared SettingsPageView.java: " + ", ".join(missing))


def prepare_activity() -> None:
    text = ACTIVITY.read_text(encoding="utf-8")
    if "private PinStore pinStore;" in text:
        verify_activity(text)
        print(f"Verified {ACTIVITY.relative_to(ROOT)} privacy lifecycle")
        return

    text = replace_once(
        text,
        "    private AppSettings appSettings;\n",
        "    private AppSettings appSettings;\n"
        "    private PinStore pinStore;\n"
        "    private boolean pinPromptVisible;\n",
        "privacy fields",
    )
    text = replace_once(
        text,
        "        appSettings = new AppSettings(this);\n"
        "        buildShell();",
        "        appSettings = new AppSettings(this);\n"
        "        pinStore = new PinStore(this);\n"
        "        buildShell();",
        "privacy initialization",
    )

    lifecycle = r'''
    @Override
    protected void onResume() {
        super.onResume();
        if (!privacyLockEnabled() || pinPromptVisible
                || !pinStore.shouldLock(System.currentTimeMillis())) {
            return;
        }
        pinStore.markLocked();
        pinPromptVisible = true;
        PinUnlockDialog.show(this, pinStore, () -> pinPromptVisible = false);
    }

    @Override
    protected void onStop() {
        if (!isChangingConfigurations() && privacyLockEnabled()) {
            pinStore.markBackground(System.currentTimeMillis());
        }
        super.onStop();
    }

    private boolean privacyLockEnabled() {
        return db != null && pinStore != null && pinStore.isConfigured()
                && new ModuleRepository(db).isEnabled(ModuleRepository.PRIVACY_LOCK);
    }

'''
    text = replace_once(
        text,
        "    @Override\n"
        "    protected void onDestroy() {",
        lifecycle + "    @Override\n    protected void onDestroy() {",
        "privacy lifecycle",
    )
    verify_activity(text)
    ACTIVITY.write_text(text, encoding="utf-8")
    print(f"Prepared {ACTIVITY.relative_to(ROOT)} privacy lifecycle")


def prepare_settings() -> None:
    text = SETTINGS.read_text(encoding="utf-8")
    if "private void addPrivacyCard()" in text:
        verify_settings(text)
        print(f"Verified {SETTINGS.relative_to(ROOT)} privacy UI")
        return

    text = replace_once(
        text,
        "        if (modules.isEnabled(ModuleRepository.DATA_MANAGEMENT)) {\n"
        "            addDataManagementCard();\n"
        "            gap();\n"
        "        }\n"
        "        if (modules.isEnabled(ModuleRepository.BUDGET)) {",
        "        if (modules.isEnabled(ModuleRepository.DATA_MANAGEMENT)) {\n"
        "            addDataManagementCard();\n"
        "            gap();\n"
        "        }\n"
        "        if (modules.isEnabled(ModuleRepository.PRIVACY_LOCK)) {\n"
        "            addPrivacyCard();\n"
        "            gap();\n"
        "        }\n"
        "        if (modules.isEnabled(ModuleRepository.BUDGET)) {",
        "privacy settings render",
    )

    text = replace_once(
        text,
        "        row.setOnClickListener(v -> ModuleCenterDialog.show(activity, modules, () -> {\n"
        "            setChoiceValue(row, \"管理功能模块\", moduleSummary(modules.list()));\n"
        "            changed();\n"
        "        }));",
        "        row.setOnClickListener(v -> ModuleCenterDialog.show(activity, modules, () -> {\n"
        "            setChoiceValue(row, \"管理功能模块\", moduleSummary(modules.list()));\n"
        "            changed();\n"
        "            render();\n"
        "        }));",
        "module center live refresh",
    )

    method = r'''
    private void addPrivacyCard() {
        PinStore store = new PinStore(activity);
        LinearLayout card = card("🔒  隐私与安全", CartoonStyle.SOFT_LAVENDER,
                "本地 PIN 锁使用加盐 PBKDF2 摘要；连续失败会触发限流。");
        String value = store.isConfigured()
                ? "PIN 已启用 · " + privacyTimeoutLabel(store.getTimeoutSeconds())
                : "尚未设置 PIN";
        TextView row = choiceRow("PIN 与自动锁定", value);
        row.setOnClickListener(v -> PrivacySettingsDialog.show(activity, store, () -> {
            String updated = store.isConfigured()
                    ? "PIN 已启用 · " + privacyTimeoutLabel(store.getTimeoutSeconds())
                    : "尚未设置 PIN";
            setChoiceValue(row, "PIN 与自动锁定", updated);
            changed();
        }));
        card.addView(row, rowParams());
        content.addView(card);
    }

    private static String privacyTimeoutLabel(int seconds) {
        if (seconds == 0) return "立即锁定";
        if (seconds == 30) return "30 秒后锁定";
        if (seconds == 60) return "1 分钟后锁定";
        if (seconds == 300) return "5 分钟后锁定";
        return "15 分钟后锁定";
    }

'''
    text = replace_once(
        text,
        "    private void addDataManagementCard() {",
        method + "    private void addDataManagementCard() {",
        "privacy settings methods",
    )
    verify_settings(text)
    SETTINGS.write_text(text, encoding="utf-8")
    print(f"Prepared {SETTINGS.relative_to(ROOT)} privacy UI")


def main() -> None:
    prepare_activity()
    prepare_settings()


if __name__ == "__main__":
    main()
