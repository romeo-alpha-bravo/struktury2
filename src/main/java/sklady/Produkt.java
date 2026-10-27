package sklady;

import java.util.Locale;


public class Produkt {
    private final String id;
    private final String nazev;
    private final String kategorie;
    private final double cena;
    private final int mnozstvi;

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

    @Override
    public String toString() {
        return String.format(Locale.ROOT, "%s | %s | %s | %.2f Kč | %d ks",
                id, nazev, kategorie, cena, mnozstvi);
    }
}
