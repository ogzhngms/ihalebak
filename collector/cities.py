"""Turkey's 81 provinces and how to find one in a notice's address."""

from __future__ import annotations

import re

PROVINCES = [
    "Adana", "Adıyaman", "Afyonkarahisar", "Ağrı", "Aksaray", "Amasya", "Ankara", "Antalya", "Ardahan", "Artvin",
    "Aydın", "Balıkesir", "Bartın", "Batman", "Bayburt", "Bilecik", "Bingöl", "Bitlis", "Bolu", "Burdur",
    "Bursa", "Çanakkale", "Çankırı", "Çorum", "Denizli", "Diyarbakır", "Düzce", "Edirne", "Elazığ", "Erzincan",
    "Erzurum", "Eskişehir", "Gaziantep", "Giresun", "Gümüşhane", "Hakkari", "Hatay", "Iğdır", "Isparta", "İstanbul",
    "İzmir", "Kahramanmaraş", "Karabük", "Karaman", "Kars", "Kastamonu", "Kayseri", "Kilis", "Kırıkkale", "Kırklareli",
    "Kırşehir", "Kocaeli", "Konya", "Kütahya", "Malatya", "Manisa", "Mardin", "Mersin", "Muğla", "Muş",
    "Nevşehir", "Niğde", "Ordu", "Osmaniye", "Rize", "Sakarya", "Samsun", "Siirt", "Sinop", "Sivas",
    "Şanlıurfa", "Şırnak", "Tekirdağ", "Tokat", "Trabzon", "Tunceli", "Uşak", "Van", "Yalova", "Yozgat",
    "Zonguldak",
]

# Older or everyday names that addresses still use.
ALIASES = {"Afyon": "Afyonkarahisar", "Antep": "Gaziantep", "Maraş": "Kahramanmaraş", "Urfa": "Şanlıurfa", "İçel": "Mersin"}

_LETTER = "A-Za-zÇĞİÖŞÜçğıöşüÂâÎîÛû"
_NAMES = {name: name for name in PROVINCES} | ALIASES


def _fold(text: str) -> str:
    # Bulletins mix "İZMİR", "Izmir" and "izmir"; compare on one spelling.
    return text.replace("İ", "i").replace("I", "ı").lower()


_FOLDED = {_fold(name): province for name, province in _NAMES.items()}
_FOLDED_PATTERN = re.compile(
    rf"(?<![{_LETTER}])(" + "|".join(sorted(map(re.escape, _FOLDED), key=len, reverse=True)) + rf")(?![{_LETTER}])",
)


def find_province(*texts: str) -> str | None:
    """The province named last in the first text that names one: addresses end with "District/Province"."""
    for text in texts:
        if not text:
            continue
        matches = list(_FOLDED_PATTERN.finditer(_fold(text)))
        if matches:
            return _FOLDED[matches[-1].group(1)]
    return None


def slug(province: str) -> str:
    table = str.maketrans("çğıöşüÇĞİÖŞÜâîû", "cgiosucgiosuaiu")
    return province.translate(table).lower()
