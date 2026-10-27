# Sklady – semestrální práce A a B (Datové struktury)

Správa produktů v pěti skladech. Každý sklad je abstraktní obousměrný necyklický lineární seznam
(`abstrdoublelist.AbstrDoubleList`), sklady jsou uloženy v poli (`sklady.Sklady`).
Semestrální práce B přidává vyhledávání podle ID v binárním vyhledávacím stromu
(`abstrtable.AbstrTable<String, Produkt>`), který ukládá odkazy na tytéž objekty `Produkt`.

## Spuštění

- **NetBeans / IntelliJ:** otevřít složku projektu s `pom.xml` (Maven), spustit `gui.Main`.
- **Maven:** `mvn javafx:run`

Potřeba je JDK 25 (stejné jako v `pom.xml`), JavaFX stáhne Maven sám.

Testy logiky (bez JUnit), spouštějí se jako obyčejné `main`:
- `src/test/java/TestySklady.java` – semestrální práce A (seznam, sklady)
- `src/test/java/TestyAgenda.java` – semestrální práce B (LIFO/FIFO, BVS, AgendaSklady)

Pracovní adresář musí být kořen projektu, aby se našel `sklady.csv`.

## Struktura

```
src/main/java/abstrdoublelist/  IAbstrDoubleList, AbstrDoubleList, AbstrDoubleListException   (A)
src/main/java/abstrlifofifo/    IAbstrLifoFifo, AbstrLIFO, AbstrFIFO, AbstrLifoFifoException    (B)
src/main/java/abstrtable/       IAbstrTable, AbstrTable (BVS), eTypProhl, AbstrTableException  (B)
src/main/java/sklady/           Sklady, Produkt, enumSklad, enumPozice, GeneratorProduktu,
                                AgendaSklady (B)
src/main/java/gui/              ProgSklady (JavaFX okno), Main (spouštěč)
src/main/resources/gui/         styl.css
src/test/java/                  TestySklady, TestyAgenda
sklady.csv                      vstupní data ze zadání
```

## Semestrální práce B – jak je to udělané

- **Bez rekurze.** Hledání, vkládání i odebírání v BVS jdou cyklem po odkazech `left` / `right` /
  `parent`. Průchod do hloubky (in-order) používá zásobník `AbstrLIFO`, průchod do šířky frontu
  `AbstrFIFO`; obě struktury jsou postavené nad `AbstrDoubleList` z práce A.
- **Odebrání uzlu se dvěma syny:** nahradí ho nejpravější prvek levého podstromu (in-order předchůdce),
  ten pak má nejvýš levého syna, který zaujme jeho místo.
- **`najdiVRozsahu(od, do)`** – in-order průchod zásobníkem, který vynechává podstromy mimo interval,
  složitost O(h + k), u vyváženého stromu O(log n + k). Obě meze se započítávají, výsledek je
  vzestupně podle ID, nic nenalezeno = prázdné pole.
- **`vybuduj()`** – in-order průchodem seřadí uzly do pole a strom postaví znovu: kořenem úseku je
  jeho prostřední prvek. Úseky se zpracovávají z fronty (ne rekurzí), složitost O(n).
  Strom se vyvažuje jen touto metodou.
- **`AgendaSklady`** drží seznamy a BVS v souladu: vložení / import / generování nastaví
  `produkt.sklad` a vloží do seznamu i BVS (duplicitní ID se odmítá napříč sklady),
  odebrání odstraní z obou struktur, přesun mění jen `produkt.sklad` a seznamy.

## Ovládání

- **Horní lišta:** výběr skladu (Všechny / Sklad 1–5) s počtem produktů, rychlé hledání podle ID, import.
- **Tabulka:** produkty vybraného skladu v pořadí seznamu, **žlutý řádek = aktuální prvek**,
  sloupec # = pořadí. Nad tabulkou filtr „množství menší než“. Výsledek hledání v rozsahu nebo
  průchodu BVS se zobrazí v téže tabulce (tlačítko „Zpět na sklad“).
- **Záložka Produkt:** pohyb v seznamu (zpřístupnění), formulář pro nový produkt, volba pozice,
  Vložit / Odebrat.
- **Záložka Hledání:** najít produkt podle ID (karta s údaji včetně skladu), ukázat ve skladu,
  odebrat podle ID, produkty v rozsahu ID.
- **Záložka BVS:** počet uzlů a výška stromu, průchod do hloubky / do šířky, vybudování vyváženého stromu.
- **Záložka Přesun:** přesun aktuálního produktu z jednoho skladu na konec jiného.
- **Záložka Generátor:** náhodné produkty Apple do vybraného skladu nebo náhodně do všech.
- **Záložka Sklad:** průměrná cena, uložení výpisu a logu do souboru, zrušení skladu.
- **Menu Soubor:** import CSV (Ctrl/Cmd+O), uložení výpisu (Ctrl/Cmd+S), uložení logu.

Vstup: klávesnice, soubor, generátor. Výstup: obrazovka (tabulka, karta produktu, log),
soubor (výpis právě zobrazené tabulky do CSV, log do TXT).

## Formát souboru

`sklad;id;nazev;kategorie;cena;mnozstvi` – hlavička a vadné řádky se přeskočí, cena může mít
desetinnou čárku i tečku, soubor může být v UTF-8 i Windows-1250. Řádky s ID, které už existuje,
se přeskočí. Uložený výpis má stejný formát, takže jde znovu importovat.
