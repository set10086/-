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


def prepare_activity() -> None:
    text = ACTIVITY.read_text(encoding="utf-8")
    if "TemplateHomeCard.create(" in text:
        if "ModuleRepository.TEMPLATES" not in text:
            raise SystemExit("template home card is partially prepared")
        print(f"Verified {ACTIVITY.relative_to(ROOT)} template UI")
        return
    needle = (
        '        if (homeModules.isEnabled(ModuleRepository.BUDGET)) {\n'
        '            content.addView(V13Ui.gap(this, 9));\n'
        '            content.addView(BudgetHomeCard.create(\n'
        '                    this, db, currentLedgerId, this::refreshAll));\n'
        '        }\n'
        '        content.addView(V13Ui.gap(this, 18));'
    )
    replacement = (
        '        if (homeModules.isEnabled(ModuleRepository.BUDGET)) {\n'
        '            content.addView(V13Ui.gap(this, 9));\n'
        '            content.addView(BudgetHomeCard.create(\n'
        '                    this, db, currentLedgerId, this::refreshAll));\n'
        '        }\n'
        '        if (homeModules.isEnabled(ModuleRepository.TEMPLATES)) {\n'
        '            content.addView(V13Ui.gap(this, 9));\n'
        '            content.addView(TemplateHomeCard.create(\n'
        '                    this, db, currentLedgerId, this::refreshAll));\n'
        '        }\n'
        '        content.addView(V13Ui.gap(this, 18));'
    )
    text = replace_once(text, needle, replacement, "home template card")
    ACTIVITY.write_text(text, encoding="utf-8")
    print(f"Prepared {ACTIVITY.relative_to(ROOT)} template UI")


def prepare_settings() -> None:
    text = SETTINGS.read_text(encoding="utf-8")
    if "private void addTemplateCard()" in text:
        if "TemplateManagerDialog.show(" not in text:
            raise SystemExit("template settings card is partially prepared")
        print(f"Verified {SETTINGS.relative_to(ROOT)} template UI")
        return
    text = replace_once(
        text,
        "        if (modules.isEnabled(ModuleRepository.BUDGET)) {\n"
        "            addBudgetCard();\n"
        "            gap();\n"
        "        }\n"
        "        addAppearanceCard();",
        "        if (modules.isEnabled(ModuleRepository.BUDGET)) {\n"
        "            addBudgetCard();\n"
        "            gap();\n"
        "        }\n"
        "        if (modules.isEnabled(ModuleRepository.TEMPLATES)) {\n"
        "            addTemplateCard();\n"
        "            gap();\n"
        "        }\n"
        "        addAppearanceCard();",
        "settings template render",
    )
    method = r'''
    private void addTemplateCard() {
        LinearLayout card = card("📌  记账模板", CartoonStyle.SOFT_LAVENDER,
                "把最近一笔常用账单保存为模板，使用时自动采用当前时间。");
        TextView row = choiceRow("管理记账模板", "创建、使用和删除模板");
        row.setOnClickListener(v -> TemplateManagerDialog.show(
                activity, db, currentLedgerId, () -> {
                    changed();
                    render();
                }));
        card.addView(row, rowParams());
        content.addView(card);
    }

'''
    text = replace_once(
        text,
        "    private void addAppearanceCard() {",
        method + "    private void addAppearanceCard() {",
        "template settings method",
    )
    SETTINGS.write_text(text, encoding="utf-8")
    print(f"Prepared {SETTINGS.relative_to(ROOT)} template UI")


def main() -> None:
    prepare_activity()
    prepare_settings()


if __name__ == "__main__":
    main()
