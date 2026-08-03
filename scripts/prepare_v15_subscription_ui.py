#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ACTIVITY = ROOT / "app/src/main/java/com/ledgerbook/lite/LedgerV14Activity.java"
SETTINGS = ROOT / "app/src/main/java/com/ledgerbook/lite/SettingsPageView.java"

def replace_once(text, needle, replacement, label):
    count = text.count(needle)
    if count != 1: raise SystemExit(f"{label}: expected one source anchor, found {count}")
    return text.replace(needle, replacement, 1)

def prepare_activity():
    text = ACTIVITY.read_text(encoding="utf-8")
    if "SubscriptionHomeCard.create(" in text:
        if "ModuleRepository.SUBSCRIPTIONS" not in text: raise SystemExit("subscription home UI partially prepared")
        print(f"Verified {ACTIVITY.relative_to(ROOT)} subscription UI"); return
    needle = ('        if (homeModules.isEnabled(ModuleRepository.RECURRING)) {\n'
              '            content.addView(V13Ui.gap(this, 9));\n'
              '            content.addView(RecurringHomeCard.create(\n'
              '                    this, db, currentLedgerId, this::refreshAll));\n'
              '        }\n'
              '        content.addView(V13Ui.gap(this, 18));')
    replacement = ('        if (homeModules.isEnabled(ModuleRepository.RECURRING)) {\n'
                   '            content.addView(V13Ui.gap(this, 9));\n'
                   '            content.addView(RecurringHomeCard.create(\n'
                   '                    this, db, currentLedgerId, this::refreshAll));\n'
                   '        }\n'
                   '        if (homeModules.isEnabled(ModuleRepository.SUBSCRIPTIONS)) {\n'
                   '            content.addView(V13Ui.gap(this, 9));\n'
                   '            content.addView(SubscriptionHomeCard.create(\n'
                   '                    this, db, currentLedgerId, this::refreshAll));\n'
                   '        }\n'
                   '        content.addView(V13Ui.gap(this, 18));')
    ACTIVITY.write_text(replace_once(text, needle, replacement, "home subscription card"), encoding="utf-8")

def prepare_settings():
    text = SETTINGS.read_text(encoding="utf-8")
    if "private void addSubscriptionCard()" in text:
        if "SubscriptionManagerDialog.show(" not in text: raise SystemExit("subscription settings UI partially prepared")
        print(f"Verified {SETTINGS.relative_to(ROOT)} subscription UI"); return
    text = replace_once(text,
        "        if (modules.isEnabled(ModuleRepository.RECURRING)) {\n            addRecurringCard();\n            gap();\n        }\n        addAppearanceCard();",
        "        if (modules.isEnabled(ModuleRepository.RECURRING)) {\n            addRecurringCard();\n            gap();\n        }\n        if (modules.isEnabled(ModuleRepository.SUBSCRIPTIONS)) {\n            addSubscriptionCard();\n            gap();\n        }\n        addAppearanceCard();",
        "settings subscription render")
    method = r'''
    private void addSubscriptionCard() {
        LinearLayout card = card("🗓️  订阅管理", CartoonStyle.SOFT_PEACH,
                "查看月均与年度成本；续费只提醒，确认后才记账。");
        SubscriptionRepository.Snapshot snapshot = new SubscriptionRepository(db)
                .snapshot(currentLedgerId, java.time.LocalDate.now(), 7);
        TextView row = choiceRow("管理订阅",
                snapshot.dueCount + " 项到期 · " + snapshot.upcomingCount + " 项即将续费");
        row.setOnClickListener(v -> SubscriptionManagerDialog.show(
                activity, db, currentLedgerId, () -> { changed(); render(); }));
        card.addView(row, rowParams());
        content.addView(card);
    }

'''
    SETTINGS.write_text(replace_once(text, "    private void addAppearanceCard() {",
        method + "    private void addAppearanceCard() {", "subscription settings method"), encoding="utf-8")

def main():
    prepare_activity(); prepare_settings()
    print("Prepared V1.5 subscription UI")

if __name__ == "__main__": main()
