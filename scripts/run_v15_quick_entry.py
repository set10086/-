#!/usr/bin/env python3
import importlib.util
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "scripts/prepare_v15_quick_entry.py"

spec = importlib.util.spec_from_file_location("prepare_v15_quick_entry", SOURCE)
if spec is None or spec.loader is None:
    raise SystemExit("cannot load prepare_v15_quick_entry.py")
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)


def verify_generated(text: str) -> None:
    required = [
        "ModuleRepository.QUICK_ENTRY",
        "QuickEntryRules.recentCategories(",
        "QuickEntryRules.copyOf(",
        "复制上一笔",
        "最近使用",
        "更多设置",
        "advancedView.setVisibility(",
    ]
    missing = [value for value in required if value not in text]
    if missing:
        raise SystemExit("partially prepared LedgerV14Activity.java: " + ", ".join(missing))


module.verify = verify_generated
module.main()
