import abstrdoublelist.AbstrDoubleList;
import abstrdoublelist.AbstrDoubleListException;
import abstrdoublelist.IAbstrDoubleList;
import sklady.GeneratorProduktu;
import sklady.Produkt;
import sklady.Sklady;
import sklady.enumPozice;
import sklady.enumSklad;

import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Random;

/**
 * Jednoduché testy bez knihoven (JUnit není potřeba). Spuštění: v IntelliJ pravým tlačítkem → Run 'TestySklady.main()'.
 * Každý test vypíše OK / CHYBA, na konci se vypíše souhrn.
 */
public class TestySklady {

    private static int celkem = 0;
    private static int chyb = 0;

    public static void main(String[] args) throws Exception {
        testPrazdnySeznam();
        testVkladaniAPoradi();
        testAktualniPrvek();
        testOdebirani();
        testOdebraniAktualnihoNastaviPrvni();
        testIterator();
        testSeznamProtiReferenci();
        testImport();
        testImportWindows1250();
        testSklady();
        testPrumerAPodLimitem();
        testPresun();
        testProdukt();
        testGenerator();

        System.out.println();
        System.out.println(chyb == 0 ? "VŠECHNY TESTY PROŠLY (" + celkem + " kontrol)"
                : "SELHÁNÍ: " + chyb + " z " + celkem + " kontrol");
        System.exit(chyb == 0 ? 0 : 1);
    }

    // ------------------------------------------------------------------ seznam

    static void testPrazdnySeznam() {
        System.out.println("-- prázdný seznam");
        IAbstrDoubleList<Integer> l = new AbstrDoubleList<>();
        kontrola(l.jePrazdny(), "nový seznam je prázdný");
        vyjimka(l::zpristupniAktualni, AbstrDoubleListException.class, "zpristupniAktualni na prázdném");
        vyjimka(l::zpristupniPrvni, AbstrDoubleListException.class, "zpristupniPrvni na prázdném");
        vyjimka(l::zpristupniPosledni, AbstrDoubleListException.class, "zpristupniPosledni na prázdném");
        vyjimka(l::odeberAktualni, AbstrDoubleListException.class, "odeberAktualni na prázdném");
        vyjimka(l::odeberPrvni, AbstrDoubleListException.class, "odeberPrvni na prázdném");
        vyjimka(l::odeberPosledni, AbstrDoubleListException.class, "odeberPosledni na prázdném");
        vyjimka(() -> l.vlozNaslednika(1), AbstrDoubleListException.class, "vlozNaslednika do prázdného");
        vyjimka(() -> l.vlozPredchudce(1), AbstrDoubleListException.class, "vlozPredchudce do prázdného");
        kontrola(!l.iterator().hasNext(), "iterátor prázdného seznamu nemá další prvek");
    }

    static void testVkladaniAPoradi() {
        System.out.println("-- vkládání a pořadí");
        IAbstrDoubleList<Integer> l = new AbstrDoubleList<>();
        l.vlozPosledni(2);          // [2]       aktuální 2
        l.vlozPrvni(1);             // [1 2]
        l.vlozPosledni(4);          // [1 2 4]
        l.vlozNaslednika(3);        // [1 2 3 4]  (následník aktuálního = 2)
        l.vlozPredchudce(0);        // [1 0 2 3 4] (předchůdce aktuálního = 2)
        kontrola(text(l).equals("1 0 2 3 4"), "pořadí po vkládání je 1 0 2 3 4, je: " + text(l));
        kontrola(zpet(l).equals("4 3 2 0 1"), "zpětné procházení (předchůdci) je 4 3 2 0 1, je: " + zpet(l));
        kontrola(!l.jePrazdny(), "seznam není prázdný");
    }

    static void testAktualniPrvek() {
        System.out.println("-- aktuální prvek");
        IAbstrDoubleList<Integer> l = new AbstrDoubleList<>();
        l.vlozPosledni(10);
        kontrola(l.zpristupniAktualni() == 10, "po vložení do prázdného je aktuální vložený prvek");
        l.vlozPosledni(20);
        l.vlozPrvni(5);
        kontrola(l.zpristupniAktualni() == 10, "další vkládání aktuální prvek nemění");
        kontrola(l.zpristupniNaslednika() == 20, "zpristupniNaslednika");
        vyjimka(l::zpristupniNaslednika, AbstrDoubleListException.class, "poslední prvek nemá následníka");
        kontrola(l.zpristupniAktualni() == 20, "po neúspěšném zpřístupnění se pozice nezměnila");
        kontrola(l.zpristupniPredchudce() == 10, "zpristupniPredchudce");
        kontrola(l.zpristupniPrvni() == 5, "zpristupniPrvni");
        vyjimka(l::zpristupniPredchudce, AbstrDoubleListException.class, "první prvek nemá předchůdce");
        kontrola(l.zpristupniPosledni() == 20, "zpristupniPosledni");
    }

    static void testOdebirani() {
        System.out.println("-- odebírání");
        IAbstrDoubleList<Integer> l = naplneny(1, 2, 3, 4, 5);   // aktuální = 1
        kontrola(l.odeberPrvni() == 1 && text(l).equals("2 3 4 5"), "odeberPrvni");
        kontrola(l.zpristupniAktualni() == 2, "odebraný první byl aktuální → aktuální je nový první");
        kontrola(l.odeberPosledni() == 5 && text(l).equals("2 3 4"), "odeberPosledni");
        kontrola(l.odeberNaslednika() == 3 && text(l).equals("2 4"), "odeberNaslednika (aktuální = 2)");
        vyjimka(l::odeberPredchudce, AbstrDoubleListException.class, "aktuální (první) nemá předchůdce");
        l.zpristupniPosledni();                                   // aktuální = 4
        kontrola(l.odeberPredchudce() == 2 && text(l).equals("4"), "odeberPredchudce");
        vyjimka(l::odeberNaslednika, AbstrDoubleListException.class, "jediný prvek nemá následníka");
        kontrola(l.odeberPrvni() == 4 && l.jePrazdny(), "odebrání posledního zbylého prvku seznam vyprázdní");
        vyjimka(l::zpristupniAktualni, AbstrDoubleListException.class, "po vyprázdnění není žádný aktuální");
        l.vlozPosledni(7);
        kontrola(l.zpristupniAktualni() == 7, "po novém vložení funguje seznam znovu");
    }

    static void testOdebraniAktualnihoNastaviPrvni() {
        System.out.println("-- odeberAktualni nastaví aktuální na první");
        IAbstrDoubleList<Integer> l = naplneny(1, 2, 3, 4);
        l.zpristupniPosledni();                                   // aktuální = 4
        kontrola(l.odeberAktualni() == 4, "odeberAktualni vrátí odebraný prvek");
        kontrola(l.zpristupniAktualni() == 1, "aktuální je teď první prvek");
        l.zpristupniNaslednika();                                 // aktuální = 2
        kontrola(l.odeberAktualni() == 2 && text(l).equals("1 3"), "odebrání prvku uprostřed");
        kontrola(l.zpristupniAktualni() == 1, "aktuální je opět první");
        l.zrus();
        kontrola(l.jePrazdny(), "zrus vyprázdní seznam");
        vyjimka(l::zpristupniAktualni, AbstrDoubleListException.class, "po zrus není aktuální prvek");
    }

    static void testIterator() {
        System.out.println("-- iterátor");
        IAbstrDoubleList<Integer> l = naplneny(1, 2, 3);
        int soucet = 0;
        for (int x : l) {
            soucet += x;
        }
        kontrola(soucet == 6, "for-each prochází všechny prvky");
        kontrola(l.zpristupniAktualni() == 1, "iterace nemění aktuální prvek");

        Iterator<Integer> it2 = l.iterator();
        int pocet = 0;
        while (it2.hasNext()) {
            it2.next();
            pocet++;
        }
        kontrola(pocet == 3, "iterátor vrátí přesně 3 prvky");
        vyjimka(it2::next, AbstrDoubleListException.class, "next za koncem seznamu");

        Iterator<Integer> a = l.iterator();
        Iterator<Integer> b = l.iterator();
        a.next();
        kontrola(b.next() == 1, "dva iterátory se navzájem neovlivňují");
    }

    /** Náhodné operace nad seznamem proti jednoduchému modelu (ArrayList + index aktuálního prvku). */
    static void testSeznamProtiReferenci() {
        System.out.println("-- náhodné operace proti referenčnímu modelu");
        Random r = new Random(42);
        boolean shoda = true;
        for (int pokus = 0; pokus < 200 && shoda; pokus++) {
            IAbstrDoubleList<Integer> l = new AbstrDoubleList<>();
            java.util.ArrayList<Integer> model = new java.util.ArrayList<>();
            int akt = -1;       // index aktuálního prvku v modelu
            int cislo = 0;
            for (int krok = 0; krok < 300 && shoda; krok++) {
                int op = r.nextInt(9);
                try {
                    switch (op) {
                        case 0 -> { l.vlozPrvni(cislo); model.add(0, cislo++); akt = model.size() == 1 ? 0 : akt + 1; }
                        case 1 -> { l.vlozPosledni(cislo); model.add(cislo++); if (model.size() == 1) akt = 0; }
                        case 2 -> {
                            if (akt >= 0) { l.vlozNaslednika(cislo); model.add(akt + 1, cislo++); }
                            else vyjimka(() -> l.vlozNaslednika(0), AbstrDoubleListException.class, null);
                        }
                        case 3 -> {
                            if (akt >= 0) { l.vlozPredchudce(cislo); model.add(akt, cislo++); akt++; }
                            else vyjimka(() -> l.vlozPredchudce(0), AbstrDoubleListException.class, null);
                        }
                        case 4 -> { if (akt >= 0) { akt = model.size() - 1; shoda &= l.zpristupniPosledni().equals(model.get(akt)); } }
                        case 5 -> { if (akt >= 0 && akt + 1 < model.size()) { akt++; shoda &= l.zpristupniNaslednika().equals(model.get(akt)); } }
                        case 6 -> { if (akt > 0) { akt--; shoda &= l.zpristupniPredchudce().equals(model.get(akt)); } }
                        case 7 -> { if (akt >= 0) { shoda &= l.odeberAktualni().equals(model.remove(akt)); akt = model.isEmpty() ? -1 : 0; } }
                        default -> {
                            if (akt >= 0) {
                                Integer pryc = l.odeberPosledni();
                                shoda &= pryc.equals(model.remove(model.size() - 1));
                                if (akt >= model.size()) akt = model.isEmpty() ? -1 : 0;
                            }
                        }
                    }
                } catch (RuntimeException e) {
                    shoda = false;
                }
                StringBuilder sb = new StringBuilder();
                for (int x : model) sb.append(x).append(' ');
                shoda &= text(l).equals(sb.toString().trim());
                if (akt >= 0) shoda &= l.zpristupniAktualni().equals(model.get(akt));
            }
        }
        kontrola(shoda, "200 náhodných scénářů po 300 krocích se shoduje s modelem");
    }

    // ------------------------------------------------------------------ Sklady

    static void testImport() throws Exception {
        System.out.println("-- import dat");
        Path soubor = Files.createTempFile("sklady-test", ".csv");
        Files.writeString(soubor, "﻿sklad;id;nazev;kategorie;cena;mnozstvi\n"
                + "1;A1;Prvni;Kat;10,50;5\n"
                + "2;A2;Druhy;Kat;20.25;0\n"
                + "\n"
                + "9;A3;Neexistující sklad;Kat;1;1\n"
                + "3;A4;Záporná cena;Kat;-1;1\n"
                + "3;A5;Špatné číslo;Kat;abc;1\n"
                + "5;A6;Bez kategorie;;99.9;100\n"
                + "5;A7;Poslední;Kat;99.9;100\n");
        Sklady s = new Sklady();
        int n = s.importData(soubor.toString());
        kontrola(n == 3, "importovány 3 platné záznamy, vráceno: " + n);
        kontrola(s.zobrazProdukty(enumSklad.SKLAD_1)[0].getCena() == 10.5, "desetinná čárka 10,50 se přečte jako 10.5");
        kontrola(s.zobrazProdukty(enumSklad.SKLAD_1)[0].getId().equals("A1"), "BOM na začátku souboru nevadí");
        kontrola(s.zobrazProdukty(null).length == 3, "ve všech skladech jsou 3 produkty");
        kontrola(ids(s.zobrazProdukty(enumSklad.SKLAD_5)).equals("A7"), "řádek s prázdnou kategorií se přeskočil");

        Path ukazka = Path.of("sklady.csv");
        if (Files.exists(ukazka)) {
            Sklady u = new Sklady();
            int radku = (int) Files.readAllLines(ukazka).stream().filter(r -> !r.isBlank()).count();
            kontrola(u.importData(ukazka.toString()) == radku - 1, "ukázkový sklady.csv: načteny všechny řádky kromě hlavičky");
        }
        vyjimka(() -> s.importData("/neexistujici/soubor.csv"), UncheckedIOException.class, "neexistující soubor");
        Files.delete(soubor);
    }

    static void testImportWindows1250() throws Exception {
        System.out.println("-- import souboru v kódování Windows-1250 (Excel)");
        Path soubor = Files.createTempFile("sklady-cp1250", ".csv");
        Files.write(soubor, "1;A1;Žlutý kůň;Úklid;10;5\n".getBytes(Charset.forName("windows-1250")));
        Sklady s = new Sklady();
        kontrola(s.importData(soubor.toString()) == 1, "řádek se načetl");
        kontrola(s.zobrazProdukty(enumSklad.SKLAD_1)[0].getNazev().equals("Žlutý kůň"), "diakritika zůstala správně");
        Files.delete(soubor);
    }

    static void testSklady() {
        System.out.println("-- modul Sklady: vkládání, zpřístupnění, odebírání");
        Sklady s = new Sklady();
        enumSklad k = enumSklad.SKLAD_2;
        s.vlozProdukt(p("A", 10, 1), enumPozice.POSLEDNI, k);
        s.vlozProdukt(p("B", 20, 2), enumPozice.POSLEDNI, k);
        s.vlozProdukt(p("C", 30, 3), enumPozice.PRVNI, k);                 // C A B, aktuální A
        s.vlozProdukt(p("D", 40, 4), enumPozice.NASLEDNIK, k);             // C A D B
        s.vlozProdukt(p("E", 50, 5), enumPozice.PREDCHUDCE, k);            // C E A D B
        kontrola(ids(s.zobrazProdukty(k)).equals("C E A D B"), "pořadí produktů C E A D B, je: " + ids(s.zobrazProdukty(k)));
        kontrola(s.zpristupniProdukt(enumPozice.AKTUALNI, k).getId().equals("A"), "aktuální je A");
        kontrola(s.zpristupniProdukt(enumPozice.NASLEDNIK, k).getId().equals("D"), "následník je D");
        kontrola(s.zpristupniProdukt(enumPozice.POSLEDNI, k).getId().equals("B"), "poslední je B");
        kontrola(s.zpristupniProdukt(enumPozice.PRVNI, k).getId().equals("C"), "první je C");
        kontrola(s.odeberProdukt(enumPozice.NASLEDNIK, k).getId().equals("E"), "odebrán následník (E)");
        kontrola(s.odeberProdukt(enumPozice.POSLEDNI, k).getId().equals("B"), "odebrán poslední (B)");
        kontrola(s.odeberProdukt(enumPozice.PRVNI, k).getId().equals("C"), "odebrán první (C)");
        kontrola(ids(s.zobrazProdukty(k)).equals("A D"), "zbývá A D, je: " + ids(s.zobrazProdukty(k)));
        vyjimka(() -> s.vlozProdukt(p("X", 1, 1), enumPozice.AKTUALNI, k), IllegalArgumentException.class, "vložení na pozici AKTUALNI");
        vyjimka(() -> s.vlozProdukt(p("X", 1, 1), enumPozice.PRVNI, null), IllegalArgumentException.class, "vložení bez skladu");
        vyjimka(() -> s.zpristupniProdukt(enumPozice.PRVNI, enumSklad.SKLAD_1), AbstrDoubleListException.class, "zpřístupnění z prázdného skladu");
        kontrola(s.zobrazProdukty(enumSklad.SKLAD_1).length == 0, "ostatní sklady zůstaly prázdné");
        s.zrus(k);
        kontrola(s.zobrazProdukty(null).length == 0, "zrus(sklad) vyprázdnil sklad");
    }

    static void testPrumerAPodLimitem() {
        System.out.println("-- průměrná cena a produkty pod limitem");
        Sklady s = new Sklady();
        s.vlozProdukt(p("A", 100, 5), enumPozice.POSLEDNI, enumSklad.SKLAD_1);
        s.vlozProdukt(p("B", 200, 15), enumPozice.POSLEDNI, enumSklad.SKLAD_1);
        s.vlozProdukt(p("C", 600, 10), enumPozice.POSLEDNI, enumSklad.SKLAD_3);
        kontrola(s.zjistiPrumernouCenu(enumSklad.SKLAD_1) == 150f, "průměr skladu 1 = 150");
        kontrola(s.zjistiPrumernouCenu(enumSklad.SKLAD_3) == 600f, "průměr skladu 3 = 600");
        kontrola(s.zjistiPrumernouCenu(null) == 300f, "průměr všech skladů = 300");
        kontrola(s.zjistiPrumernouCenu(enumSklad.SKLAD_2) == 0f, "průměr prázdného skladu = 0");
        kontrola(ids(s.zobrazProduktyPodLimitem(null, 11)).equals("A C"), "pod limitem 11 (všechny sklady): A C");
        kontrola(ids(s.zobrazProduktyPodLimitem(enumSklad.SKLAD_1, 11)).equals("A"), "pod limitem 11 (sklad 1): A");
        kontrola(s.zobrazProduktyPodLimitem(null, 5).length == 0, "limit je ostrá nerovnost (množství < limit)");
        kontrola(s.zobrazProduktyPodLimitem(null, 1000).length == 3, "pod limitem 1000: všechny");
        s.zrus(null);
        kontrola(s.zobrazProdukty(null).length == 0, "zrus(null) vyprázdnil všechny sklady");
    }

    static void testPresun() {
        System.out.println("-- přesun produktu");
        Sklady s = new Sklady();
        s.vlozProdukt(p("A", 1, 1), enumPozice.POSLEDNI, enumSklad.SKLAD_1);
        s.vlozProdukt(p("B", 1, 1), enumPozice.POSLEDNI, enumSklad.SKLAD_1);
        s.vlozProdukt(p("X", 1, 1), enumPozice.POSLEDNI, enumSklad.SKLAD_2);
        s.zpristupniProdukt(enumPozice.POSLEDNI, enumSklad.SKLAD_1);       // aktuální = B
        s.presunProdukt(enumSklad.SKLAD_1, enumSklad.SKLAD_2);
        kontrola(ids(s.zobrazProdukty(enumSklad.SKLAD_1)).equals("A"), "ze skladu 1 zmizel B");
        kontrola(ids(s.zobrazProdukty(enumSklad.SKLAD_2)).equals("X B"), "B je na konci skladu 2");
        kontrola(s.zpristupniProdukt(enumPozice.AKTUALNI, enumSklad.SKLAD_1).getId().equals("A"), "po přesunu je ve zdroji aktuální první");
        vyjimka(() -> s.presunProdukt(enumSklad.SKLAD_3, enumSklad.SKLAD_1), AbstrDoubleListException.class, "přesun z prázdného skladu");
        kontrola(ids(s.zobrazProdukty(enumSklad.SKLAD_1)).equals("A"), "neúspěšný přesun nic nezměnil");
    }

    static void testProdukt() {
        System.out.println("-- produkt");
        vyjimka(() -> new Produkt("", "n", "k", 1, 1), IllegalArgumentException.class, "prázdné ID");
        vyjimka(() -> new Produkt("1", " ", "k", 1, 1), IllegalArgumentException.class, "prázdný název");
        vyjimka(() -> new Produkt("1", "n", "k", -0.01, 1), IllegalArgumentException.class, "záporná cena");
        vyjimka(() -> new Produkt("1", "n", "k", Double.NaN, 1), IllegalArgumentException.class, "cena NaN");
        vyjimka(() -> new Produkt("1", "n", "k", 1, -1), IllegalArgumentException.class, "záporné množství");
        vyjimka(() -> new Produkt("1", "n", null, 1, 1), IllegalArgumentException.class, "chybějící kategorie");
        Produkt x = new Produkt(" 1 ", " n ", " k ", 0, 0);
        kontrola(x.getId().equals("1") && x.getNazev().equals("n") && x.getKategorie().equals("k"), "ořezání mezer");
    }

    static void testGenerator() {
        System.out.println("-- generátor");
        GeneratorProduktu g = new GeneratorProduktu(1L);
        Produkt[] pole = new Produkt[500];
        for (int i = 0; i < pole.length; i++) {
            pole[i] = g.dalsi();
        }
        boolean vse = true;
        for (Produkt x : pole) {
            vse &= x.getCena() > 0 && x.getMnozstvi() >= 0 && x.getMnozstvi() <= 40 && !x.getNazev().isBlank();
        }
        kontrola(vse, "500 platných produktů");
        kontrola(pole[0].getId().equals("G0001") && pole[499].getId().equals("G0500"), "ID jsou G0001 … G0500");
        GeneratorProduktu g2 = new GeneratorProduktu(1L);
        Produkt stejny = null;
        for (int i = 0; i <= 123; i++) {
            stejny = g2.dalsi();
        }
        kontrola(pole[123].toString().equals(stejny.toString()), "se stejným semínkem vzniknou stejné produkty");
        boolean vsechnySklady = true;
        for (int i = 0; i < 50; i++) {
            vsechnySklady &= g.nahodnySklad() != null;
        }
        kontrola(vsechnySklady, "náhodný sklad je vždy platný");
    }

    // ------------------------------------------------------------------ pomocné

    static Produkt p(String id, double cena, int mnozstvi) {
        return new Produkt(id, "Produkt " + id, "Test", cena, mnozstvi);
    }

    static String ids(Produkt[] pole) {
        StringBuilder sb = new StringBuilder();
        for (Produkt x : pole) {
            sb.append(x.getId()).append(' ');
        }
        return sb.toString().trim();
    }

    @SafeVarargs
    static <T> IAbstrDoubleList<T> naplneny(T... data) {
        IAbstrDoubleList<T> l = new AbstrDoubleList<>();
        for (T d : data) {
            l.vlozPosledni(d);
        }
        return l;
    }

    /** Obsah seznamu zepředu dozadu (přes iterátor). */
    static <T> String text(IAbstrDoubleList<T> l) {
        StringBuilder sb = new StringBuilder();
        for (T x : l) {
            sb.append(x).append(' ');
        }
        return sb.toString().trim();
    }

    /** Obsah seznamu odzadu dopředu (přes zpristupniPredchudce) - kontroluje i zpětné odkazy. Mění aktuální prvek! */
    static <T> String zpet(IAbstrDoubleList<T> l) {
        StringBuilder sb = new StringBuilder();
        sb.append(l.zpristupniPosledni());
        while (true) {
            try {
                T predchudce = l.zpristupniPredchudce();
                sb.append(' ').append(predchudce);
            } catch (AbstrDoubleListException e) {
                return sb.toString();
            }
        }
    }

    static void kontrola(boolean podminka, String popis) {
        celkem++;
        if (!podminka) {
            chyb++;
        }
        System.out.println((podminka ? "  OK     " : "  CHYBA  ") + popis);
    }

    static void vyjimka(Runnable akce, Class<? extends Throwable> typ, String popis) {
        boolean spravne = false;
        try {
            akce.run();
        } catch (Throwable t) {
            spravne = typ.isInstance(t);
        }
        if (popis != null) {
            kontrola(spravne, "výjimka " + typ.getSimpleName() + ": " + popis);
        }
    }
}
