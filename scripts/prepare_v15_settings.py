#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
TARGET = ROOT / "app/src/main/java/com/ledgerbook/lite/SettingsPageView.java"


def replace_once(text: str, needle: str, replacement: str, label: str) -> str:
    count = text.count(needle)
    if count != 1:
        raise SystemExit(f"{label}: expected one source anchor, found {count}")
    return text.replace(needle, replacement, 1)


def verify(text: str) -> None:
    required = [
        "private final ModuleRepository modules;",
        "addModuleCenterCard();",
        "ModuleCenterDialog.show(activity, modules",
        "new ModuleRepository(db)",
    ]
    missing = [value for value in required if value not in text]
    if missing:
        raise SystemExit("partially prepared SettingsPageView.java: " + ", ".join(missing))


def main() -> None:
    text = TARGET.read_text(encoding="utf-8")
    if "private final ModuleRepository modules;" in text:
        verify(text)
        print(f"Verified {TARGET.relative_to(ROOT)}")
        return

    text = replace_once(
        text,
        "    private final AppSettings settings;\n"
        "    private final Listener listener;",
        "    private final AppSettings settings;\n"
        "    private final ModuleRepository modules;\n"
        "    private final Listener listener;",
        "module repository field",
    )

    text = replace_once(
        text,
        "        this.settings = settings;\n"
        "        this.currentLedgerId = currentLedgerId;",
        "        this.settings = settings;\n"
        "        this.modules = new ModuleRepository(db);\n"
        "        this.currentLedgerId = currentLedgerId;",
        "module repository initialization",
    )

    text = replace_once(
        text,
        "        content.addView(V13Ui.gap(activity, 14));\n\n"
        "        addAppearanceCard();",
        "        content.addView(V13Ui.gap(activity, 14));\n\n"
        "        addModuleCenterCard();\n"
        "        gap();\n"
        "        addAppearanceCard();",
        "module center render order",
    )

    method = r'''
    private void addModuleCenterCard() {
        LinearLayout card = card("🧩  模块中心", CartoonStyle.SOFT_GREEN,
                "默认保持核心记账简洁，高级能力按需启用并可调整显示顺序。");
        TextView row = choiceRow("管理功能模块", moduleSummary(modules.list()));
        row.setOnClickListener(v -> ModuleCenterDialog.show(activity, modules, () -> {
            setChoiceValue(row, "管理功能模块", moduleSummary(modules.list()));
            changed();
        }));
        card.addView(row, rowParams());
        content.addView(card);
    }

    private static String moduleSummary(List<ModuleRepository.Module> values) {
        int enabled = 0;
        StringBuilder names = new StringBuilder();
        for (ModuleRepository.Module value : values) {
            if (!value.enabled) continue;
            enabled++;
            if (names.length() > 0) names.append("、");
            names.append(ModuleRepository.titleFor(value.key));
        }
        String summary = enabled + "/" + values.size() + " 已启用";
        return names.length() == 0 ? summary : summary + " · " + names;
    }

'''
    text = replace_once(
        text,
        "    private void addAppearanceCard() {",
        method + "    private void addAppearanceCard() {",
        "module center methods",
    )

    verify(text)
    TARGET.write_text(text, encoding="utf-8")
    print(f"Prepared {TARGET.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
