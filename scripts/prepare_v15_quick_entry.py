#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
TARGET = ROOT / "app/src/main/java/com/ledgerbook/lite/LedgerV14Activity.java"


def replace_once(text: str, needle: str, replacement: str, label: str) -> str:
    count = text.count(needle)
    if count != 1:
        raise SystemExit(f"{label}: expected one source anchor, found {count}")
    return text.replace(needle, replacement, 1)


def verify(text: str) -> None:
    required = [
        "ModuleRepository.QUICK_ENTRY",
        "QuickEntryRules.recentCategories(",
        "QuickEntryRules.copyOf(",
        "复制上一笔",
        "最近使用",
        "更多设置",
        "advanced.setVisibility(",
    ]
    missing = [value for value in required if value not in text]
    if missing:
        raise SystemExit("partially prepared LedgerV14Activity.java: " + ", ".join(missing))


def main() -> None:
    text = TARGET.read_text(encoding="utf-8")
    if "QuickEntryRules.recentCategories(" in text:
        verify(text)
        print(f"Verified {TARGET.relative_to(ROOT)}")
        return

    text = replace_once(
        text,
        "        LinearLayout form = form();\n"
        "        Spinner type = spinner(new String[]{\"支出\", \"收入\", \"转账\"});",
        "        ModuleRepository moduleRepository = new ModuleRepository(db);\n"
        "        PreferenceRepository quickPreferences = new PreferenceRepository(db);\n"
        "        boolean compactEntry = moduleRepository.isEnabled(ModuleRepository.QUICK_ENTRY);\n"
        "        List<LedgerDb.Txn> latestTransactions = db.getRecentTransactions(currentLedgerId, 30);\n\n"
        "        LinearLayout form = form();\n"
        "        Spinner type = spinner(new String[]{\"支出\", \"收入\", \"转账\"});",
        "quick-entry initialization",
    )

    text = replace_once(
        text,
        "        addField(form, \"账单类型\", type);",
        "        addField(form, \"账单类型\", type);\n"
        "        TextView copyLast = V13Ui.button(this, \"⧉  复制上一笔\", CartoonStyle.SOFT_YELLOW);\n"
        "        copyLast.setVisibility(compactEntry && !latestTransactions.isEmpty()\n"
        "                ? View.VISIBLE : View.GONE);\n"
        "        form.addView(copyLast, new LinearLayout.LayoutParams(\n"
        "                ViewGroup.LayoutParams.MATCH_PARENT, V13Ui.dp(this, 46)));",
        "copy-last button",
    )

    text = replace_once(
        text,
        "        addField(form, \"优惠\", discount);",
        "        TextView discountLabel = addField(form, \"优惠\", discount);",
        "discount label capture",
    )

    text = replace_once(
        text,
        "        TextView categoryView = selectField(categoryIcon[0] + \"  \" + category[0], \"点击选择具体分类\");\n"
        "        addField(form, \"分类\", categoryView);",
        "        TextView categoryView = selectField(categoryIcon[0] + \"  \" + category[0], \"点击选择具体分类\");\n"
        "        TextView recentLabel = formLabel(\"最近使用\");\n"
        "        LinearLayout recentCategories = new LinearLayout(this);\n"
        "        recentCategories.setOrientation(LinearLayout.HORIZONTAL);\n"
        "        form.addView(recentLabel);\n"
        "        form.addView(recentCategories, new LinearLayout.LayoutParams(\n"
        "                ViewGroup.LayoutParams.MATCH_PARENT, V13Ui.dp(this, 62)));\n"
        "        addField(form, \"分类\", categoryView);",
        "recent category row",
    )

    text = replace_once(
        text,
        "        addField(form, \"发生时间\", timeView);",
        "        TextView timeLabel = addField(form, \"发生时间\", timeView);",
        "time label capture",
    )

    text = replace_once(
        text,
        "        addField(form, \"记账人\", bookkeeperView);",
        "        TextView bookkeeperLabel = addField(form, \"记账人\", bookkeeperView);",
        "bookkeeper label capture",
    )

    text = replace_once(
        text,
        "        addField(form, \"标签\", tags);\n"
        "        form.addView(reimbursable);\n"
        "        form.addView(budget);\n"
        "        addField(form, \"备注\", note);",
        "        TextView tagsLabel = addField(form, \"标签\", tags);\n"
        "        form.addView(reimbursable);\n"
        "        form.addView(budget);\n"
        "        TextView noteLabel = addField(form, \"备注\", note);\n\n"
        "        TextView moreToggle = V13Ui.button(this, \"更多设置 ▾\", CartoonStyle.SOFT_LAVENDER);\n"
        "        moreToggle.setVisibility(compactEntry ? View.VISIBLE : View.GONE);\n"
        "        form.addView(moreToggle, new LinearLayout.LayoutParams(\n"
        "                ViewGroup.LayoutParams.MATCH_PARENT, V13Ui.dp(this, 48)));",
        "advanced fields and toggle",
    )

    logic = r'''        View[] advanced = {discountLabel, discount, timeLabel, timeView,
                bookkeeperLabel, bookkeeperView, tagsLabel, tags,
                reimbursable, budget, noteLabel, note};
        boolean[] advancedVisible = {!compactEntry
                || quickPreferences.getBoolean("quick_more_expanded", false)};
        Runnable[] applyAdvanced = new Runnable[1];
        applyAdvanced[0] = () -> {
            boolean visible = !compactEntry || advancedVisible[0];
            for (View advancedView : advanced) {
                advancedView.setVisibility(visible ? View.VISIBLE : View.GONE);
            }
            moreToggle.setText(visible ? "收起更多设置 ▴" : "更多设置 ▾");
        };
        moreToggle.setOnClickListener(v -> {
            advancedVisible[0] = !advancedVisible[0];
            quickPreferences.putBoolean("quick_more_expanded", advancedVisible[0]);
            applyAdvanced[0].run();
        });

        Runnable[] renderRecent = new Runnable[1];
        renderRecent[0] = () -> {
            int selectedType = type.getSelectedItemPosition();
            String dbType = selectedType == 0 ? LedgerDb.TYPE_EXPENSE
                    : selectedType == 1 ? LedgerDb.TYPE_INCOME : LedgerDb.TYPE_TRANSFER;
            List<QuickEntryRules.CategoryChoice> choices = QuickEntryRules.recentCategories(
                    latestTransactions, dbType, 4);
            if (choices.isEmpty()) {
                choices = new ArrayList<>();
                for (InputCatalog.Option option : InputCatalog.categories(dbType)) {
                    if (option.custom) continue;
                    choices.add(new QuickEntryRules.CategoryChoice(option.icon, option.label));
                    if (choices.size() >= 4) break;
                }
            }
            recentCategories.removeAllViews();
            for (QuickEntryRules.CategoryChoice choice : choices) {
                String display = choice.label;
                int slash = display.lastIndexOf('/');
                if (slash >= 0 && slash < display.length() - 1) display = display.substring(slash + 1);
                TextView chip = V13Ui.button(this, choice.icon + "\n" + shorten(display, 5),
                        CartoonStyle.SURFACE);
                chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
                chip.setGravity(Gravity.CENTER);
                chip.setOnClickListener(v -> {
                    category[0] = choice.label;
                    categoryIcon[0] = choice.icon;
                    categoryView.setText(choice.icon + "  " + choice.label);
                });
                LinearLayout.LayoutParams chipParams = new LinearLayout.LayoutParams(
                        0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
                chipParams.setMargins(V13Ui.dp(this, 2), 0, V13Ui.dp(this, 2), 0);
                recentCategories.addView(chip, chipParams);
            }
            recentLabel.setVisibility(compactEntry ? View.VISIBLE : View.GONE);
            recentCategories.setVisibility(compactEntry ? View.VISIBLE : View.GONE);
        };

        copyLast.setOnClickListener(v -> {
            if (latestTransactions.isEmpty()) return;
            QuickEntryRules.Snapshot copied = QuickEntryRules.copyOf(
                    latestTransactions.get(0), System.currentTimeMillis());
            int copiedType = LedgerDb.TYPE_INCOME.equals(copied.type) ? 1
                    : LedgerDb.TYPE_TRANSFER.equals(copied.type) ? 2 : 0;
            type.setSelection(copiedType);
            type.post(() -> {
                amountCents[0] = copied.amountCents;
                discountCents[0] = copied.discountCents;
                category[0] = copied.category;
                categoryIcon[0] = InputCatalog.iconFor(copied.type, copied.category);
                source[0] = QuickEntryRules.preferredAccount(accounts, copied.accountId);
                LedgerDb.Account copiedTarget = null;
                if (copied.toAccountId != null) {
                    for (LedgerDb.Account account : accounts) {
                        if (account.id == copied.toAccountId) copiedTarget = account;
                    }
                }
                if (copiedTarget == null || copiedTarget.id == source[0].id) {
                    copiedTarget = firstDifferent(accounts, source[0].id);
                }
                target[0] = copiedTarget;
                occurredAt[0] = copied.occurredAt;
                bookkeeper[0] = copied.bookkeeper;
                amount.setText("💴  " + formatMoney(copied.amountCents));
                discount.setText(copied.discountCents == 0L
                        ? "🏷️  无优惠" : "🏷️  " + formatMoney(copied.discountCents));
                categoryView.setText(categoryIcon[0] + "  " + copied.category);
                sourceView.setText(accountLabel(source[0]));
                targetView.setText(target[0] == null
                        ? "请先创建另一个账户" : accountLabel(target[0]));
                timeView.setText("🕒  " + V13Ui.dateTime(copied.occurredAt));
                bookkeeperView.setText("🙂  " + copied.bookkeeper);
                tags.setText(copied.tags);
                reimbursable.setChecked(copied.reimbursable);
                budget.setChecked(copied.includeBudget);
                note.setText(copied.note);
                renderRecent[0].run();
                toast("已复制上一笔，时间已更新");
            });
        });
        applyAdvanced[0].run();

'''
    text = replace_once(
        text,
        "        Runnable updateType = () -> {",
        logic + "        Runnable updateType = () -> {",
        "quick-entry behavior",
    )

    text = replace_once(
        text,
        "            categoryView.setAlpha(transfer ? 0.65f : 1f);\n"
        "        };",
        "            categoryView.setAlpha(transfer ? 0.65f : 1f);\n"
        "            renderRecent[0].run();\n"
        "        };",
        "recent categories on type change",
    )

    text = replace_once(
        text,
        "        categoryView.setOnClickListener(v -> {",
        "        renderRecent[0].run();\n"
        "        categoryView.setOnClickListener(v -> {",
        "initial recent category render",
    )

    text = replace_once(
        text,
        "    private void addField(LinearLayout form, String label, View field) {\n"
        "        form.addView(formLabel(label));\n"
        "        form.addView(field, new LinearLayout.LayoutParams(\n"
        "                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));\n"
        "    }",
        "    private TextView addField(LinearLayout form, String label, View field) {\n"
        "        TextView labelView = formLabel(label);\n"
        "        form.addView(labelView);\n"
        "        form.addView(field, new LinearLayout.LayoutParams(\n"
        "                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));\n"
        "        return labelView;\n"
        "    }",
        "field label return value",
    )

    verify(text)
    TARGET.write_text(text, encoding="utf-8")
    print(f"Prepared {TARGET.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
