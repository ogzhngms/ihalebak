"""Turns the text of a Kamu İhale Bülteni PDF into one record per notice.

Every notice opens with a numbered line such as "1.   2026/1783854       AKARYAKIT SATIN ALINACAKTIR" and belongs
to the section heading above it (tender notices, corrections, cancellations...). The fields we keep are written
the same way in every notice: "1.1. Adı :", "1.2. Adresi :", "2.1. Tarih ve Saati :" and so on.
"""

from __future__ import annotations

import io
import re

from pypdf import PdfReader

from collector.cities import find_province

# Section headings, most specific first; anything that is not one of these kinds is a plain tender notice.
_KINDS = [
    ("ÖN İLAN", "pre_notice"),
    ("DÜZELTME", "correction"),
    ("ZEYİLNAME", "addendum"),
    ("İPTAL", "cancellation"),
    ("SATIŞ", "sale"),
    ("İLAN", "tender"),
]

# A section heading is a short all-capitals line such as "4. İHALE İPTAL İLANLARI" (checked in _section).
_HEADING = re.compile(r"^\s*\d\.\s+([A-ZÇĞİÖŞÜ][A-ZÇĞİÖŞÜ ]{3,80})$")
_ENTRY = re.compile(r"^\s*\d+\.\s+(\d{4}/\d{4,})\s+(\S.*?)\s*$")
_PAGE_FURNITURE = re.compile(
    r"^\s*(KAMU İHALE BÜLTENİ\b.*|.*İHALELERİ BÜLTENİ\s*|Kamu İhale Kurumu\s*[–-]\s*www\.kik\.gov\.tr.*|\d+)\s*$"
)

# Notices under the procurement law number their fields "1.1. Adı"; notices of institutions outside it (section B)
# letter them "a) Adresi" and put the institution's name in capitals above the registration number.
_FIELD_END = r"(?=\s*(?:\d\.\d\.|\d\s?-\s|[a-zç]\)\s|Ayrıntılı Bilgiye|İhale Türü|$))"
_AUTHORITY = re.compile(r"1\.1\.\s*Adı\s*:\s*(.+?)" + _FIELD_END, re.S)
_ADDRESS = re.compile(r"(?:1\.2\.|a\))\s*Adresi\s*:\s*(.+?)" + _FIELD_END, re.S)
_WHEN = re.compile(
    r"Tarih(?:i)? ve [Ss]aati\s*:?\s*(\d{1,2})[./](\d{1,2})[./](\d{4})(?:\s*[-–/]?\s*(?:[Ss]aat\s*:?\s*)?(\d{1,2})[:.](\d{2}))?"
)
_SUBJECT = re.compile(r"(?:3\.1\.\s*Adı|İşin Adı)\s*:\s*(.+?)" + _FIELD_END, re.S)
_QUANTITY = re.compile(r"(?:3\.2\.|a\))\s*Niteliği, türü ve miktarı\s*:\s*(.+?)" + _FIELD_END, re.S)
# Titles end with a verb in capitals: SATIN ALINACAKTIR, YAPTIRILACAKTIR, KİRAYA VERİLECEKTİR...
_TITLE_DONE = re.compile(r"(?:TIR|TİR|DIR|DİR|TUR|TÜR|DUR|DÜR)\s*$")
_CANCELLED_ON = re.compile(r"İhale İptal Tarihi\s*:\s*(\d{2})\.(\d{2})\.(\d{4})")


def pdf_text(data: bytes) -> str:
    return "\n".join(page.extract_text() or "" for page in PdfReader(io.BytesIO(data)).pages)


def _kind(heading: str) -> str:
    for marker, kind in _KINDS:
        if marker in heading:
            return kind
    return "tender"


def _clean(text: str | None) -> str | None:
    if not text:
        return None
    text = " ".join(text.split()).strip(" :-")
    return text or None


def _iso_date(day: str, month: str, year: str) -> str:
    return f"{year}-{int(month):02d}-{int(day):02d}"


def parse_notices(text: str, category: str, bulletin_date: str) -> list[dict]:
    """Every notice in the bulletin's text, in order, skipping the table of contents."""
    lines = [line for line in text.splitlines() if not _PAGE_FURNITURE.match(line)]
    notices: list[dict] = []
    kind = "tender"
    current: dict | None = None
    body: list[str] = []
    title_open = False

    def close():
        if current is not None:
            notices.append(_fields(current, "\n".join(body)))

    for line in lines:
        heading = _HEADING.match(line.rstrip())
        if heading and ("İLAN" in heading.group(1) or "ZEYİLNAME" in heading.group(1)):
            close()
            current, body, title_open = None, [], False
            kind = _kind(heading.group(1))
            continue
        entry = _ENTRY.match(line)
        # The table of contents repeats each entry with dot leaders and a page number; those are not notices.
        if entry and "...." not in line:
            close()
            current = {
                "ikn": entry.group(1),
                "title": entry.group(2),
                "kind": kind,
                "category": category,
                "bulletin_date": bulletin_date,
            }
            body, title_open = [], True
            continue
        if current is None:
            continue
        if title_open:
            # A long title wraps onto the next line until it reaches its closing verb.
            if line.strip() and line.strip().isupper() and not _TITLE_DONE.search(current["title"]):
                if "...." in line:
                    # A table-of-contents entry whose title wrapped: its dot leaders are on this line.
                    current, title_open = None, False
                    continue
                current["title"] += " " + line.strip()
                continue
            title_open = False
        body.append(line)
    close()
    # The same notice can be split by a page break into two entries with the same number; keep the fuller one.
    unique: dict[tuple, dict] = {}
    for notice in notices:
        key = (notice["ikn"], notice["kind"])
        if key not in unique or len(notice.get("subject") or "") > len(unique[key].get("subject") or ""):
            unique[key] = notice
    return list(unique.values())


def _fields(notice: dict, body: str) -> dict:
    authority = _clean(_first(_AUTHORITY, body)) or _capitals_before_ikn(body)
    address = _clean(_first(_ADDRESS, body))
    when = _WHEN.search(body)
    cancelled = _CANCELLED_ON.search(body)
    notice = notice | {
        "title": _clean(notice["title"]),
        "authority": authority,
        "address": address,
        "province": find_province(address, authority, notice["title"], body),
        "date": _iso_date(*when.group(1, 2, 3)) if when else None,
        "time": f"{int(when.group(4)):02d}:{when.group(5)}" if when and when.group(4) else None,
        "subject": _clean(_first(_SUBJECT, body)),
        "quantity": _clean(_first(_QUANTITY, body)),
        "cancelled_on": _iso_date(*cancelled.groups()) if cancelled else None,
    }
    return {key: value for key, value in notice.items() if value is not None}


def _capitals_before_ikn(body: str) -> str | None:
    """Section B notices name the institution in capitals between the title and "İhale Kayıt Numarası"."""
    head = body.split("İhale Kayıt Numarası", 1)[0]
    lines = [line.strip() for line in head.splitlines() if line.strip()]
    capitals = [line for line in lines if line.isupper() and not _TITLE_DONE.search(line)]
    return _clean(" ".join(capitals)) if capitals and len(capitals) == len(lines) else None


def _first(pattern: re.Pattern, text: str) -> str | None:
    match = pattern.search(text)
    return match.group(1) if match else None
