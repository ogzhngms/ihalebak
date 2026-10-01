"""Downloads the daily Kamu İhale Bülteni from EKAP's archive page.

The page is a plain ASP.NET form with no login or captcha, and EKAP's robots.txt allows it. We ask for one
bulletin per category per day, a handful of requests, and identify ourselves in the User-Agent.
"""

from __future__ import annotations

import datetime as dt
import http.cookiejar
import io
import re
import ssl
import time
import urllib.parse
import urllib.request
import zipfile

URL = "https://ekap.kik.gov.tr/ekap/ilan/bultenindirme.aspx"
USER_AGENT = "ihalebak/1.0 (+https://github.com/ogzhngms/ihalebak; reads the public Kamu Ihale Bulteni)"

# The archive form's category codes, and the name each one uses in this project.
CATEGORIES = {"mal": "1", "yapim": "2", "hizmet": "3", "danismanlik": "4"}


# EKAP speaks only TLS 1.2 with RSA key exchange (AES128-GCM-SHA256), which Python 3.10+ leaves out of its default
# cipher list. Allow OpenSSL's default list instead; the certificate is still verified as usual.
_TLS = ssl.create_default_context()
_TLS.set_ciphers("DEFAULT")


class BulletinMissing(Exception):
    """No bulletin for that day and category, e.g. a weekend or holiday."""


def _hidden_fields(html: str) -> dict[str, str]:
    return dict(re.findall(r'<input[^>]*type="hidden"[^>]*name="([^"]+)"[^>]*value="([^"]*)"', html))


def download(day: dt.date, category: str, retries: int = 3) -> dict[str, bytes]:
    """The PDFs inside one day's bulletin zip, by file name: the notices and, separately, the results."""
    last_error: Exception | None = None
    for attempt in range(retries):
        try:
            return _download_once(day, category)
        except BulletinMissing:
            raise
        except Exception as e:  # network hiccup or EKAP maintenance window
            last_error = e
            time.sleep(5 * (attempt + 1))
    raise RuntimeError(f"{category} {day}: {last_error}")


def _download_once(day: dt.date, category: str) -> dict[str, bytes]:
    opener = urllib.request.build_opener(
        urllib.request.HTTPSHandler(context=_TLS),
        urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()),
    )
    opener.addheaders = [("User-Agent", USER_AGENT)]
    page = opener.open(URL, timeout=60).read().decode("utf-8", "ignore")
    form = _hidden_fields(page) | {
        "__EVENTTARGET": "ctl00$ContentPlaceHolder1$btnYukle",
        "__EVENTARGUMENT": "",
        "ctl00$ContentPlaceHolder1$ddlstBxIhaleTur": CATEGORIES[category],
        "ctl00$ContentPlaceHolder1$etBultenTarihi$EkapTakvimTextBox_etBultenTarihi": day.strftime("%d.%m.%Y"),
    }
    response = opener.open(URL, data=urllib.parse.urlencode(form).encode(), timeout=120)
    body = response.read()
    if body[:2] != b"PK":
        raise BulletinMissing(f"{category} {day}")
    archive = zipfile.ZipFile(io.BytesIO(body))
    return {name: archive.read(name) for name in archive.namelist() if name.lower().endswith(".pdf")}
