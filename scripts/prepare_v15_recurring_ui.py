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
    if "RecurringHomeCard.create(" in text:
        if "ModuleRepository.RECURRING" not in text:
            raise SystemExit("recurring home card is partially prepared")
        print(f"Verified {ACTIVITY.relative_to(ROOT)} recurring UI")
        return
    needle = (
        '        if (homeModules.isEnabled(ModuleRepository.TEMPLATES)) {\n'
        '            content.addView(V13Ui.gap(this, 9));\n'
        '            content.addView(TemplateHomeCard.create(\n'
        '                    this, db, currentLedgerId, this::refreshAll));\n'
        '        }\n'
        '        content.addView(V13Ui.gap(this, 18));'
    )
    replacement = (
        '        if (homeModules.isEnabled(ModuleRepository.TEMPLATES)) {\n'
        '            content.addView(V13Ui.gap(this, 9));\n'
        '            content.addView(TemplateHomeCard.create(\n'
        '                    this, db, currentLedgerId, this::refreshAll));\n'
        '        }\n'
        '        if (homeModules.isEnabled(ModuleRepository.RECURRING)) {\n'
        '            content.addView(V13Ui.gap(this, 9));\n'
        '            content.addView(RecurringHomeCard.create(\n'
        '                    this, db, currentLedgerId, this::refreshAll));\n'
        '        }\n'
        '        content.addView(V13Ui.gap(this, 18));'
    )
    text = replace_once(text, needle, replacement, "home recurring card")
    ACTIVITY.write_text(text, encoding="utf-8")
    print(f"Prepared {ACTIVITY.relative_to(ROOT)} recurring UI")


def prepare_settings() -> None:
    text = SETTINGS.read_text(encoding="utf-8")
    if "private void addRecurringCard()" in text:
        if "RecurringManagerDialog.show(" not in text:
            raise SystemExit("recurring settings card is partially prepared")
        print(f"Verified {SETTINGS.relative_to(ROOT)} recurring UI")
        return
    text = replace_once(
        text,
        "        if (modules.isEnabled(ModuleRepository.TEMPLATES)) {\n"
        "            addTemplateCard();\n"
        "            gap();\n"
        "        }\n"
        "        addAppearanceCard();",
        "        if (modules.isEnabled(ModuleRepository.TEMPLATES)) {\n"
        "            addTemplateCard();\n"
        "            gap();\n"
        "        }\n"
        "        if (modules.isEnabled(ModuleRepository.RECURRING)) {\n"
        "            addRecurringCard();\n"
        "            gap();\n"
        "        }\n"
        "        addAppearanceCard();",
        "settings recurring render",
    )
    method = r'''
    private void addRecurringCard() {
        LinearLayout card = card("🔁  周期记账", CartoonStyle.SOFT_SKY,
                "到期只生成待确认项目，确认后才会改变账户余额。");
        RecurringRepository repository = new RecurringRepository(db);
        repository.materializeDue(currentLedgerId, java.time.LocalDate.now());
        int pending = repository.listPending(currentLedgerId).size();
        TextView row = choiceRow("管理周期账单",
                pending == 0 ? "暂无待确认项目" : pending + " 项待确认");
        row.setOnClickListener(v -> RecurringManagerDialog.show(
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
        "recurring settings method",
    )
    SETTINGS.write_text(text, encoding="utf-8")
    print(f"Prepared {SETTINGS.relative_to(ROOT)} recurring UI")


def main() -> None:
    prepare_activity()
    prepare_settings()


if __name__ == "__main__":
    main()
