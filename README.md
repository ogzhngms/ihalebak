# İhaleBak

Upcoming Turkish public tenders by province and type, taken from the official daily **Kamu İhale Bülteni**
(Public Procurement Bulletin) of the Kamu İhale Kurumu. Information only: no bidding happens here, and the
authoritative record is always [EKAP](https://ekap.kik.gov.tr).

## How the data is collected

```
EKAP bulletin page ─► fetch.py (zip of PDFs) ─► parse.py (one record per notice) ─► build.py ─► GitHub Pages JSON
```

- **Source:** the bulletin archive at `ekap.kik.gov.tr/ekap/ilan/bultenindirme.aspx`. It is a plain form with
  no login or captcha, and EKAP's `robots.txt` disallows nothing. Each run asks for four files per day (goods,
  services, works, consultancy), a handful of requests.
- **Parsing:** every notice starts with a numbered line holding its registration number (İKN) and title, under a
  section heading that says what it is: a tender, a correction, a cancellation, a sale. Section A notices (under
  law 4734) number their fields `1.1. Adı`, section B notices letter them `a) Adresi`; both are read.
- **Merging:** new tenders are added, corrections update the date and time, cancellations mark the tender
  cancelled, and tenders whose date has passed are dropped.
- **Schedule:** GitHub Actions runs twice a day (07:30 and 13:30 in Turkey) and replaces the `gh-pages` branch,
  so history never piles up. Everything runs on free tiers.

## Published files

| File | Contents |
|---|---|
| `data/index.json` | Update time, bulletins read, open tenders per province and type |
| `data/il/<province>.json` | That province's tenders, e.g. `data/il/izmir.json` |
| `data/il/bilinmeyen.json` | Notices whose province could not be read |
| `data/all.json` | Everything; the collector's own memory between runs |

A tender looks like this:

```json
{"ikn":"2026/1783854","title":"AKARYAKIT SATIN ALINACAKTIR","category":"mal","type":"tender","status":"open",
 "authority":"İzmir Büyükşehir Belediyesi ...","province":"İzmir","date":"2026-10-22","time":"10:00",
 "subject":"... Motorin Alımı","quantity":"Motorin - 250.000 Litre","bulletin_date":"2026-10-01"}
```

## Run it locally

```bash
pip install -r collector/requirements.txt
python -m unittest discover -s collector/tests -t .
python -m collector.run --out site/data --days 3
```
