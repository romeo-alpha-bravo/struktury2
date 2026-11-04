package sklady;

import java.util.Locale;


/*
 * Один продукт склада. Все поля, кроме sklad, final: после создания их не изменить.
 * Особенно важно, что не меняется ID: это КЛЮЧ в дереве поиска. Если бы ключ поменялся,
 * узел оказался бы "не на своём месте" (нарушилось бы свойство BVS) и najdi его не нашёл бы.
 *
 * Один и тот же объект Produkt одновременно лежит и в списке склада, и в дереве
 * (там и там — ссылка на него). Поэтому изменение sklad при переносе сразу видно в обоих местах.
 */
public class Produkt {
    private final String id;
    private final String nazev;
    private final String kategorie;
    private final double cena;
    private final int mnozstvi;
    // aktuální sklad: nastaví se při vložení či importu, změní se při přesunu
    private enumSklad sklad;

    // Конструктор сразу проверяет данные: плохой продукт просто не может появиться.
    // IllegalArgumentException ловит importData и пропускает такую строку CSV.
    public Produkt(String id, String nazev, String kategorie, double cena, int mnozstvi) {
        if ((id == null) || (id.isBlank())) {
            throw new IllegalArgumentException("ID produktu nesmí být prázdné.");
        }
        if ((nazev == null) || (nazev.isBlank())) {
            throw new IllegalArgumentException("Název produktu nesmí být prázdný.");
        }
        if ((kategorie == null) || (kategorie.isBlank())) {
            throw new IllegalArgumentException("Kategorie produktu nesmí být prázdná.");
        }

        if (!Double.isFinite(cena) || cena < 0) {
            throw new IllegalArgumentException("Cena musí být nezáporné číslo.");
        }
        if (mnozstvi < 0) {
            throw new IllegalArgumentException("Množství nesmí být záporné.");
        }
        this.id = id.trim();
        this.nazev = nazev.trim();
        this.kategorie = kategorie.trim();
        this.cena = cena;
        this.mnozstvi = mnozstvi;
    }

    public String getId() {
        return id;
    }

    public String getNazev() {
        return nazev;
    }

    public String getKategorie() {
        return kategorie;
    }


    public double getCena() {
        return cena;
    }


    public int getMnozstvi() {
        return mnozstvi;
    }

    public enumSklad getSklad() {
        return sklad;
    }

    // вызывают Sklady.vlozProdukt / presunProdukt / nactiProdukty
    public void setSklad(enumSklad sklad) {
        this.sklad = sklad;
    }

    @Override
    public String toString() {
        String text = String.format(Locale.ROOT, "%s | %s | %s | %.2f Kč | %d ks",
                id, nazev, kategorie, cena, mnozstvi);
        return sklad == null ? text : text + " | " + sklad.getNazev();
    }
}
