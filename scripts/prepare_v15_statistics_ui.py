#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ACTIVITY = ROOT / "app/src/main/java/com/ledgerbook/lite/LedgerV14Activity.java"


def verify(text: str) -> None:
    required = [
        "private void showStats(LinearLayout content)",
        "new StatisticsDashboardView(this, db, currentLedgerId)",
    ]
    missing = [value for value in required if value not in text]
    if missing:
        raise SystemExit("partially prepared statistics UI: " + ", ".join(missing))


def main() -> None:
    text = ACTIVITY.read_text(encoding="utf-8")
    marker = "new StatisticsDashboardView(this, db, currentLedgerId)"
    if marker in text:
        verify(text)
        print(f"Verified {ACTIVITY.relative_to(ROOT)} statistics UI")
        return

    start_marker = "    private void showStats(LinearLayout content) {"
    end_marker = "    private void addTransactionCard(LinearLayout content, long transactionId) {"
    if text.count(start_marker) != 1 or text.count(end_marker) != 1:
        raise SystemExit("statistics method anchors changed")
    start = text.index(start_marker)
    end = text.index(end_marker, start)
    if end <= start:
        raise SystemExit("statistics method anchors are out of order")

    replacement = '''    private void showStats(LinearLayout content) {
        StatisticsDashboardView dashboard =
                new StatisticsDashboardView(this, db, currentLedgerId);
        content.addView(dashboard, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
    }

'''
    text = text[:start] + replacement + text[end:]
    verify(text)
    ACTIVITY.write_text(text, encoding="utf-8")
    print(f"Prepared {ACTIVITY.relative_to(ROOT)} statistics UI")


if __name__ == "__main__":
    main()
