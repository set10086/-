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
    if "BudgetHomeCard.create(" in text:
        if "ModuleRepository.BUDGET" not in text:
            raise SystemExit("budget home card is partially prepared")
        print(f"Verified {ACTIVITY.relative_to(ROOT)} budget UI")
        return
    needle = (
        '        content.addView(metric("👛 净资产", db.getNetAssets(currentLedgerId),\n'
        '                CartoonStyle.SOFT_YELLOW, CartoonStyle.INK));\n'
        '        content.addView(V13Ui.gap(this, 18));\n'
        '        section(content, "最近账单");'
    )
    replacement = (
        '        content.addView(metric("👛 净资产", db.getNetAssets(currentLedgerId),\n'
        '                CartoonStyle.SOFT_YELLOW, CartoonStyle.INK));\n'
        '        ModuleRepository homeModules = new ModuleRepository(db);\n'
        '        if (homeModules.isEnabled(ModuleRepository.BUDGET)) {\n'
        '            content.addView(V13Ui.gap(this, 9));\n'
        '            content.addView(BudgetHomeCard.create(\n'
        '                    this, db, currentLedgerId, this::refreshAll));\n'
        '        }\n'
        '        content.addView(V13Ui.gap(this, 18));\n'
        '        section(content, "最近账单");'
    )
    text = replace_once(text, needle, replacement, "home budget card")
    ACTIVITY.write_text(text, encoding="utf-8")
    print(f"Prepared {ACTIVITY.relative_to(ROOT)} budget UI")


def prepare_settings() -> None:
    text = SETTINGS.read_text(encoding="utf-8")
    if "private void addBudgetCard()" in text:
        if "BudgetManagerDialog.show(" not in text:
            raise SystemExit("budget settings card is partially prepared")
        print(f"Verified {SETTINGS.relative_to(ROOT)} budget UI")
        return
    text = replace_once(
        text,
        "        addModuleCenterCard();\n"
        "        gap();\n"
        "        addAppearanceCard();",
        "        addModuleCenterCard();\n"
        "        gap();\n"
        "        if (modules.isEnabled(ModuleRepository.BUDGET)) {\n"
        "            addBudgetCard();\n"
        "            gap();\n"
        "        }\n"
        "        addAppearanceCard();",
        "settings budget render",
    )
    method = r'''
    private void addBudgetCard() {
        LinearLayout card = card("🎯  预算设置", CartoonStyle.SOFT_YELLOW,
                "设置本月总预算与分类预算，关闭预算模块不会删除已保存额度。");
        TextView row = choiceRow("管理本月预算", "查看已用、剩余和日均可用金额");
        row.setOnClickListener(v -> BudgetManagerDialog.show(
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
        "budget settings method",
    )
    SETTINGS.write_text(text, encoding="utf-8")
    print(f"Prepared {SETTINGS.relative_to(ROOT)} budget UI")


def main() -> None:
    prepare_activity()
    prepare_settings()


if __name__ == "__main__":
    main()
