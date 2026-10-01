import datetime as dt
import unittest

from collector.build import merge, prune
from collector.cities import find_province, slug
from collector.parse import parse_notices

# Trimmed from real bulletins: a section A tender, a section B tender in the other layout, and a cancellation.
BULLETIN = """KAMU İHALE BÜLTENİ   01 EKİM 2026 – Sayı 5711
MAL ALIMI İHALELERİ BÜLTENİ
  Kamu İhale Kurumu – www.kik.gov.tr  3
1.   2026/1783854  AKARYAKIT SATIN ALINACAKTIR ....................................................... 7
2. İHALE İLANLARI
2.1. MAL ALIMI İHALELERİ BÜLTENİ
1.   2026/1783854       AKARYAKIT SATIN ALINACAKTIR

Tarımsal Üreticilere Destek Kapsamında Motorin Alımı açık ihale usulü ile ihale edilecektir.
İhale Kayıt Numarası (İKN) : 2026/1783854
1- İdarenin
1.1. Adı  : İzmir Büyükşehir Belediyesi Tarımsal Hizmetler Dairesi
Başkanlığı
1.2. Adresi  : Mimar Sinan Mah. 9 Eylül Meydanı No:9/1 35220 Konak/İzmir
1.3. Telefon numarası  : 02322933147
2- İhalenin
2.1. Tarih ve Saati  : 22.10.2026 - 10:00
3- İhale konusu mal alımının
3.1. Adı  : Motorin Alımı
3.2. Niteliği, türü ve miktarı  : Motorin - 250.000 Litre
Ayrıntılı Bilgiye EKAP'ta Yer Alan İhale Dokümanı İçinde Bulunan İdari Şartnameden Ulaşılabilir.
4. İHALE İPTAL İLANLARI
4.1. MAL ALIMI İHALELERİ BÜLTENİ
2.   2026/1583434       ARAÇ ÜSTÜ MİST BLOWER İLAÇLAMA MAKİNASI SATIN
ALINACAKTIR

İhale Kayıt Numarası (İKN) : 2026/1583434
1- İdarenin
1.1. Adı  : Manisa İl Tarım ve Orman Müdürlüğü
1.2. Adresi : Yunusemre/Manisa
3- İhale İptal Tarihi : 29.09.2026
1. İSTİSNA İHALE İLANLARI
1.1. MAL ALIMI İHALELERİ BÜLTENİ
3.   2026/1810513       120000 LT MOTORİN SATIN ALINACAKTIR
TÜRKİYE CUMHURİYETİ DEVLET DEMİRYOLLARI İŞLETMESİ GENEL MÜDÜRLÜĞÜ TCDD
4. BÖLGE MÜDÜRLÜĞÜ
İhale Kayıt Numarası : 2026/1810513
İşin Adı  :  120000 lt Motorin Satın Alınması İşi
İhale Türü - Usulü  :  Mal Alımı - Açık İhale Usulü
1 - İdarenin
a) Adresi : Muhsin Yazıcıoğlu Bulvarı No:1/Sivas
b) Telefon ve faks numarası : 03463460319
b) Tarihi ve saati : 22.10.2026 - 14:00
"""


class ParseTest(unittest.TestCase):
    def setUp(self):
        self.notices = {n["ikn"]: n for n in parse_notices(BULLETIN, "mal", "2026-10-01")}

    def test_reads_a_section_a_tender(self):
        tender = self.notices["2026/1783854"]
        self.assertEqual("tender", tender["kind"])
        self.assertEqual("AKARYAKIT SATIN ALINACAKTIR", tender["title"])
        self.assertEqual("İzmir Büyükşehir Belediyesi Tarımsal Hizmetler Dairesi Başkanlığı", tender["authority"])
        self.assertEqual("İzmir", tender["province"])
        self.assertEqual(("2026-10-22", "10:00"), (tender["date"], tender["time"]))
        self.assertEqual("Motorin Alımı", tender["subject"])
        self.assertEqual("Motorin - 250.000 Litre", tender["quantity"])

    def test_skips_the_table_of_contents(self):
        self.assertEqual(3, len(self.notices))

    def test_joins_a_wrapped_title_and_reads_a_cancellation(self):
        cancelled = self.notices["2026/1583434"]
        self.assertEqual("cancellation", cancelled["kind"])
        self.assertEqual("ARAÇ ÜSTÜ MİST BLOWER İLAÇLAMA MAKİNASI SATIN ALINACAKTIR", cancelled["title"])
        self.assertEqual("2026-09-29", cancelled["cancelled_on"])

    def test_reads_a_section_b_tender(self):
        tender = self.notices["2026/1810513"]
        self.assertEqual("120000 LT MOTORİN SATIN ALINACAKTIR", tender["title"])
        self.assertEqual("TÜRKİYE CUMHURİYETİ DEVLET DEMİRYOLLARI İŞLETMESİ GENEL MÜDÜRLÜĞÜ TCDD 4. BÖLGE MÜDÜRLÜĞÜ", tender["authority"])
        self.assertEqual("Sivas", tender["province"])
        self.assertEqual("2026-10-22", tender["date"])
        self.assertEqual("120000 lt Motorin Satın Alınması İşi", tender["subject"])


class CitiesTest(unittest.TestCase):
    def test_takes_the_province_at_the_end_of_an_address(self):
        self.assertEqual("İzmir", find_province("Ankara Caddesi No:5 Bornova/İZMİR"))
        self.assertEqual("Afyonkarahisar", find_province("Merkez/AFYON"))
        self.assertIsNone(find_province("Merkez Mahallesi"))

    def test_slugs_are_ascii(self):
        self.assertEqual("sanliurfa", slug("Şanlıurfa"))
        self.assertEqual("istanbul", slug("İstanbul"))


class BuildTest(unittest.TestCase):
    def test_corrections_and_cancellations_update_the_tender(self):
        store = {}
        merge(store, [{"ikn": "1", "kind": "tender", "title": "A", "category": "mal", "bulletin_date": "2026-10-01", "date": "2026-10-20", "time": "10:00"}])
        merge(store, [{"ikn": "1", "kind": "correction", "category": "mal", "bulletin_date": "2026-10-02", "date": "2026-10-27"}])
        self.assertEqual("2026-10-27", store["1"]["date"])
        merge(store, [{"ikn": "1", "kind": "cancellation", "category": "mal", "bulletin_date": "2026-10-03", "cancelled_on": "2026-10-03"}])
        self.assertEqual("cancelled", store["1"]["status"])

    def test_past_tenders_are_dropped(self):
        store = {
            "old": {"ikn": "old", "status": "open", "date": "2026-09-30", "bulletin_date": "2026-09-01"},
            "new": {"ikn": "new", "status": "open", "date": "2026-10-20", "bulletin_date": "2026-10-01"},
        }
        prune(store, dt.date(2026, 10, 1))
        self.assertEqual(["new"], list(store))


if __name__ == "__main__":
    unittest.main()
