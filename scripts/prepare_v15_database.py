#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
TARGET = ROOT / "app/src/main/java/com/ledgerbook/lite/LedgerDb.java"


def replace_once(text: str, needle: str, replacement: str, label: str) -> str:
    count = text.count(needle)
    if count != 1:
        raise SystemExit(f"{label}: expected one source anchor, found {count}")
    return text.replace(needle, replacement, 1)


def verify_prepared(text: str) -> None:
    required = [
        "private static final int DB_VERSION = LedgerV2Migration.VERSION;",
        "if (oldVersion < 2) applyV2Migration(db);",
        "LedgerV2Migration.apply((sql, args) ->",
    ]
    missing = [value for value in required if value not in text]
    if missing:
        raise SystemExit("partially prepared LedgerDb.java: " + ", ".join(missing))


def main() -> None:
    text = TARGET.read_text(encoding="utf-8")
    if "private static final int DB_VERSION = LedgerV2Migration.VERSION;" in text:
        verify_prepared(text)
        print(f"Verified {TARGET.relative_to(ROOT)}")
        return

    text = replace_once(
        text,
        "    private static final int DB_VERSION = 1;",
        "    private static final int DB_VERSION = LedgerV2Migration.VERSION;",
        "database version",
    )

    text = replace_once(
        text,
        "        db.execSQL(\"CREATE INDEX idx_transactions_category ON transactions(ledger_id, category)\");\n"
        "    }",
        "        db.execSQL(\"CREATE INDEX idx_transactions_category ON transactions(ledger_id, category)\");\n"
        "        applyV2Migration(db);\n"
        "    }",
        "new database migration",
    )

    text = replace_once(
        text,
        "    @Override\n"
        "    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {\n"
        "        // Version 1 remains compatible with V1.0–V1.3.\n"
        "    }\n\n"
        "    public void ensureDefaults() {",
        "    @Override\n"
        "    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {\n"
        "        if (oldVersion < 2) applyV2Migration(db);\n"
        "    }\n\n"
        "    private static void applyV2Migration(SQLiteDatabase db) {\n"
        "        LedgerV2Migration.apply((sql, args) -> {\n"
        "            if (args == null || args.length == 0) db.execSQL(sql);\n"
        "            else db.execSQL(sql, args);\n"
        "        }, System.currentTimeMillis());\n"
        "    }\n\n"
        "    public void ensureDefaults() {",
        "upgrade migration",
    )

    verify_prepared(text)
    TARGET.write_text(text, encoding="utf-8")
    print(f"Prepared {TARGET.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
