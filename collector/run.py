"""Collects the latest bulletins and refreshes the published data.

    python -m collector.run --out site/data            # today and yesterday
    python -m collector.run --out site/data --days 10  # backfill the last ten days
"""

from __future__ import annotations

import argparse
import datetime as dt
import sys
from pathlib import Path
from zoneinfo import ZoneInfo

from collector import build, fetch
from collector.parse import parse_notices, pdf_text

ISTANBUL = ZoneInfo("Europe/Istanbul")


def main(argv: list[str] | None = None) -> int:
    args = argparse.ArgumentParser()
    args.add_argument("--out", type=Path, required=True)
    args.add_argument("--days", type=int, default=2, help="how many days back to read, today included")
    options = args.parse_args(argv)

    now = dt.datetime.now(ISTANBUL)
    today = now.date()
    store = build.load(options.out)
    bulletins: list[str] = []
    errors: list[str] = []

    # Oldest first, so a later correction or cancellation lands after the notice it changes.
    for back in range(options.days - 1, -1, -1):
        day = today - dt.timedelta(days=back)
        for category in fetch.CATEGORIES:
            try:
                pdfs = fetch.download(day, category)
            except fetch.BulletinMissing:
                continue
            except Exception as e:
                errors.append(str(e))
                print(f"! {e}", file=sys.stderr)
                continue
            for name, data in pdfs.items():
                if "SONUC" in name.upper():
                    continue  # results bulletin: awarded contracts, not upcoming tenders
                notices = parse_notices(pdf_text(data), category, day.isoformat())
                build.merge(store, notices)
                bulletins.append(name)
                print(f"{name}: {len(notices)} notices")

    build.prune(store, today)
    build.write(store, options.out, now.isoformat(timespec="minutes"), bulletins, errors)
    print(f"{len(store)} records, {len(errors)} errors")
    # Fail the run only if nothing at all could be read, so a quiet holiday is not an alarm.
    return 1 if errors and not bulletins else 0


if __name__ == "__main__":
    sys.exit(main())
