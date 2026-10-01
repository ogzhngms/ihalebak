"""Keeps the set of open tenders up to date and writes the files the app reads.

New tender notices are added, corrections update a tender's date and time, cancellations mark it cancelled,
and tenders whose date has passed are dropped. The app reads one small file per province plus an index.
"""

from __future__ import annotations

import datetime as dt
import json
from pathlib import Path

from collector.cities import PROVINCES, slug
from collector.parse import tidy_address

# How long finished or undated records stay before they are dropped.
KEEP_CANCELLED_DAYS = 7
KEEP_UNDATED_DAYS = 45
QUANTITY_LIMIT = 400


def merge(store: dict[str, dict], notices: list[dict]) -> None:
    """Applies one bulletin's notices to the store, which is keyed by registration number (İKN)."""
    for notice in notices:
        ikn = notice["ikn"]
        kind = notice["kind"]
        if kind in ("tender", "sale"):
            previous = store.get(ikn, {})
            record = {k: v for k, v in notice.items() if k != "kind"} | {"type": kind, "status": previous.get("status", "open")}
            if "quantity" in record and len(record["quantity"]) > QUANTITY_LIMIT:
                record["quantity"] = record["quantity"][:QUANTITY_LIMIT].rsplit(" ", 1)[0] + "…"
            store[ikn] = previous | record
        elif kind == "correction" and ikn in store:
            for field in ("date", "time"):
                if field in notice:
                    store[ikn][field] = notice[field]
            store[ikn]["corrected_on"] = notice["bulletin_date"]
        elif kind == "cancellation":
            if ikn in store:
                store[ikn]["status"] = "cancelled"
                store[ikn]["cancelled_on"] = notice.get("cancelled_on", notice["bulletin_date"])
            else:
                # Cancelled before we saw it: keep a short record so the app can still say so.
                store[ikn] = {k: v for k, v in notice.items() if k in ("ikn", "title", "category", "bulletin_date", "authority", "province")} | {
                    "type": "tender",
                    "status": "cancelled",
                    "cancelled_on": notice.get("cancelled_on", notice["bulletin_date"]),
                }


def prune(store: dict[str, dict], today: dt.date) -> None:
    for ikn, record in list(store.items()):
        if record.get("status") == "cancelled":
            if dt.date.fromisoformat(record["cancelled_on"]) < today - dt.timedelta(days=KEEP_CANCELLED_DAYS):
                del store[ikn]
        elif "date" in record:
            if dt.date.fromisoformat(record["date"]) < today:
                del store[ikn]
        elif dt.date.fromisoformat(record["bulletin_date"]) < today - dt.timedelta(days=KEEP_UNDATED_DAYS):
            del store[ikn]


def load(out: Path) -> dict[str, dict]:
    path = out / "all.json"
    if not path.exists():
        return {}
    records = json.loads(path.read_text(encoding="utf-8"))["tenders"]
    # Records kept from earlier runs get the same address clean-up as fresh ones.
    return {record["ikn"]: record | {"address": tidy_address(record.get("address"))} for record in records}


def write(store: dict[str, dict], out: Path, updated_at: str, bulletins: list[str], errors: list[str]) -> None:
    """all.json holds everything (the collector's own memory); il/<province>.json and index.json are for the app."""
    out.mkdir(parents=True, exist_ok=True)
    records = sorted(store.values(), key=lambda r: (r.get("date", "9999"), r.get("time", ""), r["ikn"]))
    _dump(out / "all.json", {"updated_at": updated_at, "tenders": records})

    provinces_dir = out / "il"
    provinces_dir.mkdir(exist_ok=True)
    by_province: dict[str, list[dict]] = {}
    for record in records:
        by_province.setdefault(record.get("province") or "", []).append(record)
    for name in PROVINCES:
        _dump(provinces_dir / f"{slug(name)}.json", {"province": name, "updated_at": updated_at, "tenders": by_province.get(name, [])})
    _dump(provinces_dir / "bilinmeyen.json", {"province": None, "updated_at": updated_at, "tenders": by_province.get("", [])})

    counts = {
        slug(name): _counts(by_province.get(name, []))
        for name in PROVINCES
    }
    _dump(out / "index.json", {
        "updated_at": updated_at,
        "bulletins": bulletins,
        "errors": errors,
        "total": _counts(records),
        "provinces": [{"name": name, "slug": slug(name), **counts[slug(name)]} for name in PROVINCES],
        "source": "Kamu İhale Bülteni, Kamu İhale Kurumu (ekap.kik.gov.tr)",
    })


def _counts(records: list[dict]) -> dict:
    open_records = [r for r in records if r.get("status") == "open"]
    by_category: dict[str, int] = {}
    for record in open_records:
        by_category[record["category"]] = by_category.get(record["category"], 0) + 1
    return {"open": len(open_records), "by_category": by_category}


def _dump(path: Path, data: dict) -> None:
    path.write_text(json.dumps(data, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
