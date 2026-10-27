# Sklady – semestrální práce A (Datové struktury)

Správa produktů v pěti skladech. Každý sklad je abstraktní obousměrný necyklický lineární seznam
(`abstrdoublelist.AbstrDoubleList`), sklady jsou uloženy v poli (`sklady.Sklady`).

## Spuštění

- **IntelliJ:** otevřít složku projektu (Maven), po načtení závislostí spustit `gui.Main` (zelená šipka).
- **Maven:** `mvn javafx:run`

Potřeba je JDK 25 (stejné jako v `pom.xml`), JavaFX stáhne Maven sám.

Testy logiky (bez JUnit): `src/test/java/TestySklady.java` → pravým tlačítkem *Run 'TestySklady.main()'*.
Pracovní adresář musí být kořen projektu, aby se našel `sklady.csv`.

## Struktura

```
src/main/java/abstrdoublelist/  IAbstrDoubleList, AbstrDoubleList, AbstrDoubleListException
src/main/java/sklady/           Sklady, Produkt, enumSklad, enumPozice, GeneratorProduktu
src/main/java/gui/              ProgSklady (JavaFX okno), Main (spouštěč)
src/main/resources/gui/         styl.css
src/test/java/                  TestySklady
sklady.csv                      vstupní data ze zadání
```

## Ovládání

- **Horní lišta:** výběr skladu (Všechny / Sklad 1–5), u každého počet produktů.
- **Tabulka:** produkty vybraného skladu v pořadí seznamu, **žlutý řádek = aktuální prvek**.
  Nad tabulkou filtr „množství menší než“.
- **Záložka Produkt:** pohyb v seznamu (zpřístupnění první / poslední / předchůdce / následník / aktuální),
  formulář pro nový produkt, volba pozice, Vložit / Odebrat.
- **Záložka Přesun:** přesun aktuálního produktu z jednoho skladu na konec jiného.
- **Záložka Generátor:** náhodné produkty Apple do vybraného skladu nebo náhodně do všech.
- **Záložka Sklad:** průměrná cena, uložení výpisu a logu do souboru, zrušení skladu.
- **Menu Soubor:** import CSV (Ctrl/Cmd+O), uložení výpisu (Ctrl/Cmd+S), uložení logu.

Vstup: klávesnice, soubor, generátor. Výstup: obrazovka (tabulka, log), soubor (výpis CSV, log TXT).

## Formát souboru

`sklad;id;nazev;kategorie;cena;mnozstvi` – hlavička a vadné řádky se přeskočí, cena může mít
desetinnou čárku i tečku, soubor může být v UTF-8 i Windows-1250. Uložený výpis má stejný formát,
takže jde znovu importovat.
