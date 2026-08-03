#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
TARGET = ROOT / "app/src/main/java/com/ledgerbook/lite/SettingsPageView.java"

def replace_once(text, needle, replacement, label):
    count = text.count(needle)
    if count != 1: raise SystemExit(f"{label}: expected one source anchor, found {count}")
    return text.replace(needle, replacement, 1)

def main():
    text = TARGET.read_text(encoding="utf-8")
    if "private void addDataManagementCard()" in text:
        if "DataManagementDialog.show(" not in text: raise SystemExit("data settings partially prepared")
        print(f"Verified {TARGET.relative_to(ROOT)} data UI"); return
    text = replace_once(text,
        "        addModuleCenterCard();\n        gap();\n        if (modules.isEnabled(ModuleRepository.BUDGET)) {",
        "        addModuleCenterCard();\n        gap();\n        if (modules.isEnabled(ModuleRepository.DATA_MANAGEMENT)) {\n            addDataManagementCard();\n            gap();\n        }\n        if (modules.isEnabled(ModuleRepository.BUDGET)) {",
        "data settings render")
    method = r'''
    private void addDataManagementCard() {
        LinearLayout card = card("💾  数据管理", CartoonStyle.SOFT_GREEN,
                "CSV 导出、JSON 完整备份与恢复；恢复前自动创建安全快照。");
        TextView row = choiceRow("导出、备份与恢复", "文件仅保存在当前手机本地");
        row.setOnClickListener(v -> DataManagementDialog.show(
                activity, db, currentLedgerId, () -> {
                    changed();
                    render();
                }));
        card.addView(row, rowParams());
        content.addView(card);
    }

'''
    text = replace_once(text, "    private void addAppearanceCard() {",
        method + "    private void addAppearanceCard() {", "data settings method")
    TARGET.write_text(text, encoding="utf-8")
    print(f"Prepared {TARGET.relative_to(ROOT)} data UI")

if __name__ == "__main__": main()
