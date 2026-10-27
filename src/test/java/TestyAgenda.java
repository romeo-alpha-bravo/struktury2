import abstrlifofifo.AbstrFIFO;
import abstrlifofifo.AbstrLIFO;
import abstrlifofifo.AbstrLifoFifoException;
import abstrtable.AbstrTable;
import abstrtable.AbstrTableException;
import abstrtable.eTypProhl;
import sklady.AgendaSklady;
import sklady.GeneratorProduktu;
import sklady.Produkt;
import sklady.enumPozice;
import sklady.enumSklad;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

/**
 * Testy semestrální práce B (bez JUnit). Spuštění: pravým tlačítkem → Run 'TestyAgenda.main()'.
 * Pracovní adresář = kořen projektu (kvůli sklady.csv).
 */
public class TestyAgenda {

    private static int celkem = 0;
    private static int chyb = 0;

    public static void main(String[] args) throws Exception {
        testLifoFifo();
        testZakladBVS();
        testOdebiraniTriPripady();
        testPruchody();
        testRozsah();
        testVybuduj();
        testBVSProtiTreeMap();
        testHlubokyStrom();
        testAgendaImport();
        testAgendaOperace();
        testAgendaNahodne();

        System.out.println();
        System.out.println(chyb == 0 ? "VŠECHNY TESTY PROŠLY (" + celkem + " kontrol)"
                : "SELHÁNÍ: " + chyb + " z " + celkem + " kontrol");
        System.exit(chyb == 0 ? 0 : 1);
    }

    // ------------------------------------------------------------------ LIFO / FIFO

    static void testLifoFifo() {
        System.out.println("-- zásobník a fronta");
        AbstrLIFO<Integer> z = new AbstrLIFO<>();
        AbstrFIFO<Integer> f = new AbstrFIFO<>();
        kontrola(z.jePrazdny() && f.jePrazdny(), "nové jsou prázdné");
        for (int i = 1; i <= 4; i++) {
            z.vloz(i);
            f.vloz(i);
        }
        kontrola(text(z.vytvorIterator()).equals("4 3 2 1"), "iterátor zásobníku od vrcholu: 4 3 2 1");
        kontrola(text(f.vytvorIterator()).equals("1 2 3 4"), "iterátor fronty od začátku: 1 2 3 4");
        kontrola(z.odeber() == 4 && z.odeber() == 3, "LIFO odebírá naposledy vložený");
        kontrola(f.odeber() == 1 && f.odeber() == 2, "FIFO odebírá nejdříve vložený");
        z.zrus();
        f.zrus();
        kontrola(z.jePrazdny() && f.jePrazdny(), "zrus vyprázdní");
        vyjimka(z::odeber, AbstrLifoFifoException.class, "odebrání z prázdného zásobníku");
        vyjimka(f::odeber, AbstrLifoFifoException.class, "odebrání z prázdné fronty");
    }

    // ------------------------------------------------------------------ BVS

    static void testZakladBVS() throws Exception {
        System.out.println("-- BVS: vložení, hledání, duplicita");
        AbstrTable<String, String> t = new AbstrTable<>();
        kontrola(t.jePrazdny() && t.getPocet() == 0 && t.getVyska() == 0, "nová tabulka je prázdná");
        kontrola(t.najdi("X") == null && t.odeber("X") == null, "najdi/odeber v prázdné vrací null");
        for (String k : new String[]{"P050", "P030", "P070", "P020", "P040"}) {
            t.vloz(k, "h" + k);
        }
        kontrola(t.getPocet() == 5 && t.getVyska() == 3, "5 záznamů, výška 3");
        kontrola("hP040".equals(t.najdi("P040")), "najdi P040");
        kontrola(t.najdi("P041") == null, "nenalezený klíč -> null");
        vyjimka(() -> t.vloz("P030", "jiny"), AbstrTableException.class, "duplicitní klíč odmítnut");
        kontrola(t.getPocet() == 5 && "hP030".equals(t.najdi("P030")), "po odmítnutí se nic nezměnilo");
        vyjimka(() -> t.vloz(null, "x"), AbstrTableException.class, "klíč null");
        vyjimka(() -> t.vloz("Q", null), AbstrTableException.class, "hodnota null");
        kontrola(strukturaOk(t), "odkazy parent a uspořádání BVS jsou v pořádku");
        t.zrus();
        kontrola(t.jePrazdny() && t.getPocet() == 0 && !t.vytvorIterator(eTypProhl.SIRKA).hasNext(), "zrus");
    }

    static void testOdebiraniTriPripady() throws Exception {
        System.out.println("-- BVS: odebrání (list, jeden syn, dva synové, kořen)");
        //            8
        //        6        15
        //      1   7    10
        //         6.4     14
        //           6.5
        AbstrTable<Double, String> t = new AbstrTable<>();
        for (double k : new double[]{8, 6, 15, 1, 7, 10, 6.4, 14, 6.5}) {
            t.vloz(k, "v" + k);
        }
        kontrola("v14.0".equals(t.odeber(14.0)) && strukturaOk(t), "odebrání listu (14)");
        kontrola("v10.0".equals(t.odeber(10.0)) && strukturaOk(t), "odebrání uzlu s jedním synem (10)");
        // odebrání 8 (kořen, dva synové): nahradí ho nejpravější prvek levého podstromu = 7,
        // 7 nemá pravý podstrom, na jeho místo jde jeho levý syn 6.4
        kontrola("v8.0".equals(t.odeber(8.0)) && strukturaOk(t), "odebrání kořene se dvěma syny (8)");
        kontrola(text(t.vytvorIterator(eTypProhl.SIRKA)).equals("v7.0 v6.0 v15.0 v1.0 v6.4 v6.5"),
                "po odebrání 8 je kořenem 7 a na místě 7 je 6.4, je: " + text(t.vytvorIterator(eTypProhl.SIRKA)));
        kontrola(t.najdi(8.0) == null && t.getPocet() == 6, "8 už ve stromu není, zbývá 6 záznamů");
        kontrola(t.odeber(99.0) == null && t.getPocet() == 6, "odebrání neexistujícího klíče vrátí null");
        for (double k : new double[]{7, 6, 15, 1, 6.4, 6.5}) {
            t.odeber(k);
        }
        kontrola(t.jePrazdny() && t.getPocet() == 0, "odebrání všech -> prázdný strom");
    }

    static void testPruchody() {
        System.out.println("-- BVS: průchod do hloubky (in-order) a do šířky");
        AbstrTable<String, String> t = new AbstrTable<>();
        for (String k : new String[]{"P004", "P002", "P006", "P001", "P003", "P005", "P007"}) {
            t.vloz(k, k);
        }
        kontrola(text(t.vytvorIterator(eTypProhl.HLOUBKA)).equals("P001 P002 P003 P004 P005 P006 P007"),
                "in-order = vzestupně");
        kontrola(text(t.vytvorIterator(eTypProhl.SIRKA)).equals("P004 P002 P006 P001 P003 P005 P007"),
                "level-order = po úrovních");
        Iterator<String> it = t.vytvorIterator(eTypProhl.HLOUBKA);
        it.next();
        t.vloz("P008", "P008");
        vyjimka(it::next, ConcurrentModificationException.class, "změna stromu během průchodu");
        Iterator<String> konec = t.vytvorIterator(eTypProhl.SIRKA);
        while (konec.hasNext()) {
            konec.next();
        }
        vyjimka(konec::next, NoSuchElementException.class, "next za koncem průchodu");
    }

    static void testRozsah() {
        System.out.println("-- BVS: hledání v rozsahu");
        AbstrTable<String, String> t = new AbstrTable<>();
        for (int i = 1; i <= 60; i++) {
            String k = String.format("P%03d", (i * 37) % 60 + 1);   // promíchané pořadí vkládání
            t.vloz(k, k);
        }
        kontrola(text(t.najdiVRozsahu("P010", "P015").iterator()).equals("P010 P011 P012 P013 P014 P015"),
                "P010..P015 včetně obou mezí, vzestupně");
        kontrola(text(t.najdiVRozsahu("P0095", "P0105").iterator()).equals("P010"), "meze mezi klíči");
        kontrola(t.najdiVRozsahu("P061", "P099").jePrazdny(), "nic nenalezeno -> prázdný výsledek");
        kontrola(t.najdiVRozsahu("P015", "P010").jePrazdny(), "obrácené meze -> prázdný výsledek");
        kontrola(text(t.najdiVRozsahu("P060", "P060").iterator()).equals("P060"), "jednoprvkový rozsah");
        int vse = 0;
        for (String ignored : t.najdiVRozsahu("A", "Z")) {
            vse++;
        }
        kontrola(vse == 60, "rozsah přes všechno vrátí všech 60");
    }

    static void testVybuduj() throws Exception {
        System.out.println("-- BVS: vybudování vyváženého stromu");
        AbstrTable<String, String> t = new AbstrTable<>();
        for (int i = 1; i <= 1000; i++) {
            String k = String.format("K%04d", i);
            t.vloz(k, k);                                // seřazené vkládání -> degenerovaný strom
        }
        kontrola(t.getVyska() == 1000, "po seřazeném vkládání je strom degenerovaný (výška 1000)");
        String predtim = text(t.vytvorIterator(eTypProhl.HLOUBKA));
        t.vybuduj();
        kontrola(t.getVyska() == 10, "po vybuduj je výška 10 = ⌈log2(1001)⌉, je: " + t.getVyska());
        kontrola(t.getPocet() == 1000 && text(t.vytvorIterator(eTypProhl.HLOUBKA)).equals(predtim),
                "obsah a pořadí se nezměnily");
        kontrola(t.vytvorIterator(eTypProhl.SIRKA).next().equals("K0500"), "kořenem je střední klíč K0500");
        kontrola(strukturaOk(t), "odkazy parent po vybudování sedí");
        t.vloz("K0000", "K0000");
        kontrola("K0000".equals(t.najdi("K0000")) && t.odeber("K0500") != null && strukturaOk(t),
                "po vybudování funguje vkládání i odebírání");

        AbstrTable<Integer, Integer> maly = new AbstrTable<>();
        maly.vybuduj();
        kontrola(maly.jePrazdny(), "vybuduj prázdného stromu nic nerozbije");
        for (int n = 1; n <= 40; n++) {
            AbstrTable<Integer, Integer> s = new AbstrTable<>();
            for (int i = 0; i < n; i++) {
                s.vloz(i, i);
            }
            s.vybuduj();
            int ocekavana = 32 - Integer.numberOfLeadingZeros(n);   // ⌈log2(n+1)⌉
            if (s.getVyska() != ocekavana || !strukturaOk(s)) {
                kontrola(false, "vybuduj pro n = " + n);
                return;
            }
        }
        kontrola(true, "vybuduj dá minimální výšku pro všechna n = 1..40");
    }

    // náhodné operace proti java.util.TreeMap jako vzoru
    static void testBVSProtiTreeMap() throws Exception {
        System.out.println("-- BVS: náhodné operace proti TreeMap");
        boolean shoda = true;
        for (int seed = 1; seed <= 40 && shoda; seed++) {
            Random r = new Random(seed);
            AbstrTable<Integer, String> t = new AbstrTable<>();
            TreeMap<Integer, String> vzor = new TreeMap<>();
            for (int krok = 0; krok < 1500 && shoda; krok++) {
                int k = r.nextInt(300);
                int op = r.nextInt(10);
                if (op < 5) {
                    boolean melo = vzor.containsKey(k);
                    try {
                        t.vloz(k, "v" + k + "_" + krok);
                        shoda &= !melo;
                        vzor.put(k, "v" + k + "_" + krok);
                    } catch (AbstrTableException e) {
                        shoda &= melo;
                    }
                } else if (op < 8) {
                    shoda &= eq(t.odeber(k), vzor.remove(k));
                } else if (op == 8) {
                    shoda &= eq(t.najdi(k), vzor.get(k));
                } else if (r.nextInt(20) == 0) {
                    t.vybuduj();
                }
                if (krok % 50 == 0) {
                    shoda &= t.getPocet() == vzor.size();
                    shoda &= text(t.vytvorIterator(eTypProhl.HLOUBKA)).equals(String.join(" ", vzor.values()));
                    int a = r.nextInt(320) - 10;
                    int b = a + r.nextInt(80);
                    shoda &= text(t.najdiVRozsahu(a, b).iterator())
                            .equals(String.join(" ", vzor.subMap(a, true, b, true).values()));
                    List<String> sirka = new ArrayList<>();
                    t.vytvorIterator(eTypProhl.SIRKA).forEachRemaining(sirka::add);
                    List<String> hodnoty = new ArrayList<>(vzor.values());
                    Collections.sort(sirka);
                    Collections.sort(hodnoty);
                    shoda &= sirka.equals(hodnoty);
                    shoda &= strukturaOk(t);
                }
            }
        }
        kontrola(shoda, "40 × 1500 náhodných operací se shoduje s TreeMap (vč. rozsahů a struktury)");
    }

    // degenerovaný strom s 20 000 uzly: rekurzivní implementace by spadla na StackOverflowError
    static void testHlubokyStrom() {
        System.out.println("-- BVS: hluboký degenerovaný strom (ověření, že nic není rekurzivní)");
        AbstrTable<Integer, Integer> t = new AbstrTable<>();
        int n = 20_000;
        try {
            for (int i = 0; i < n; i++) {
                t.vloz(i, i);
            }
            int vyska = t.getVyska();
            int pocetHloubka = 0;
            for (Iterator<Integer> it = t.vytvorIterator(eTypProhl.HLOUBKA); it.hasNext(); it.next()) {
                pocetHloubka++;
            }
            int pocetSirka = 0;
            for (Iterator<Integer> it = t.vytvorIterator(eTypProhl.SIRKA); it.hasNext(); it.next()) {
                pocetSirka++;
            }
            int vRozsahu = 0;
            for (Integer ignored : t.najdiVRozsahu(n - 100, n + 100)) {
                vRozsahu++;
            }
            Integer odebrany = t.odeber(n - 1);
            t.vybuduj();
            kontrola(vyska == n && pocetHloubka == n && pocetSirka == n && vRozsahu == 100
                            && odebrany == n - 1 && t.getVyska() == 15,
                    "výška " + n + ", průchody, rozsah, odebrání i vybudování bez přetečení zásobníku");
        } catch (StackOverflowError e) {
            kontrola(false, "StackOverflowError - někde je rekurze");
        }
    }

    // ------------------------------------------------------------------ AgendaSklady

    static void testAgendaImport() {
        System.out.println("-- AgendaSklady: import");
        Path soubor = Path.of("sklady.csv");
        if (!Files.exists(soubor)) {
            System.out.println("  (sklady.csv nenalezen v pracovním adresáři, test přeskočen)");
            return;
        }
        AgendaSklady a = new AgendaSklady();
        int n = a.importData(soubor.toString());
        kontrola(n == 60 && a.getPocetVeStromu() == 60 && a.zobrazProdukty(null).length == 60,
                "60 produktů v seznamech i v BVS");
        kontrola(a.getVyskaStromu() == 60, "ID v souboru jsou seřazená -> BVS je degenerovaný (výška 60)");
        a.vybuduj();
        kontrola(a.getVyskaStromu() == 6, "po vybuduj výška 6");
        Produkt p = a.najdi("P033");
        kontrola(p != null && p.getNazev().startsWith("Napajeci adapter") && p.getSklad() == enumSklad.SKLAD_3,
                "najdi P033 včetně skladu");
        kontrola(a.najdi(" P033 ") == p, "ID se ořízne");
        kontrola(ids(a.najdiVRozsahu("P010", "P015")).equals("P010 P011 P012 P013 P014 P015"), "rozsah P010..P015");
        kontrola(a.najdiVRozsahu("X1", "X9").length == 0, "rozsah bez výsledku -> prázdné pole");
        int znovu = a.importData(soubor.toString());
        kontrola(znovu == 0 && a.getPosledniDuplicity() == 60 && a.getPocetVeStromu() == 60,
                "opakovaný import: všech 60 odmítnuto jako duplicity");
        kontrola(soulad(a), "seznamy a BVS jsou v souladu");
    }

    static void testAgendaOperace() {
        System.out.println("-- AgendaSklady: vložení, odebrání, přesun, zrušení");
        AgendaSklady a = new AgendaSklady(new GeneratorProduktu(5L));
        a.vloz(p("A1"), enumPozice.POSLEDNI, enumSklad.SKLAD_1);
        a.vloz(p("A2"), enumPozice.POSLEDNI, enumSklad.SKLAD_1);
        a.vloz(p("A3"), enumPozice.POSLEDNI, enumSklad.SKLAD_1);
        a.vloz(p("B1"), enumPozice.POSLEDNI, enumSklad.SKLAD_2);
        kontrola(a.najdi("A2").getSklad() == enumSklad.SKLAD_1, "vložení nastaví produkt.sklad");
        vyjimka(() -> a.vloz(p("A2"), enumPozice.PRVNI, enumSklad.SKLAD_3), IllegalArgumentException.class,
                "duplicitní ID v jiném skladu odmítnuto");
        kontrola(a.zobrazProdukty(enumSklad.SKLAD_3).length == 0 && a.getPocetVeStromu() == 4,
                "odmítnutý produkt není ani v seznamu, ani v BVS");
        vyjimka(() -> a.vloz(p("C1"), enumPozice.NASLEDNIK, enumSklad.SKLAD_4), RuntimeException.class,
                "vložení následníka do prázdného skladu selže");
        kontrola(a.najdi("C1") == null, "...a produkt se pak nedostal ani do BVS");

        a.zpristupniProdukt(enumPozice.POSLEDNI, enumSklad.SKLAD_1);              // aktuální = A3
        Produkt odebrany = a.odeber("A2");
        kontrola(odebrany != null && odebrany.getId().equals("A2"), "odeber podle ID vrátí produkt");
        kontrola(ids(a.zobrazProdukty(enumSklad.SKLAD_1)).equals("A1 A3") && a.najdi("A2") == null,
                "A2 zmizel ze seznamu i z BVS");
        kontrola(a.zpristupniProdukt(enumPozice.AKTUALNI, enumSklad.SKLAD_1).getId().equals("A3"),
                "aktuální prvek zůstal na A3");
        kontrola(a.odeber("NEEXISTUJE") == null, "odeber neexistujícího ID -> null");

        a.zpristupniProdukt(enumPozice.PRVNI, enumSklad.SKLAD_1);                 // aktuální = A1
        a.presunProdukt(enumSklad.SKLAD_1, enumSklad.SKLAD_2);
        Produkt presunuty = a.najdi("A1");
        kontrola(presunuty.getSklad() == enumSklad.SKLAD_2, "přesun aktualizuje produkt.sklad");
        kontrola(ids(a.zobrazProdukty(enumSklad.SKLAD_2)).equals("B1 A1") && a.getPocetVeStromu() == 3,
                "produkt je na konci cíle, BVS má pořád 3 záznamy");

        Produkt poPozici = a.odeberProdukt(enumPozice.PRVNI, enumSklad.SKLAD_2);  // B1
        kontrola(poPozici.getId().equals("B1") && a.najdi("B1") == null, "odebrání podle pozice odstraní i z BVS");

        a.generuj(20, enumSklad.SKLAD_5);
        kontrola(a.zobrazProdukty(enumSklad.SKLAD_5).length == 20 && a.getPocetVeStromu() == 22,
                "generátor vložil 20 produktů do skladu 5 i do BVS");
        a.zrus(enumSklad.SKLAD_5);
        kontrola(a.getPocetVeStromu() == 2 && a.zobrazProdukty(enumSklad.SKLAD_5).length == 0,
                "zrus(sklad) odstraní jeho produkty i z BVS");
        kontrola(soulad(a), "seznamy a BVS jsou v souladu");
        a.zrus(null);
        kontrola(a.getPocetVeStromu() == 0 && a.zobrazProdukty(null).length == 0, "zrus(null) vyprázdní vše");
    }

    // náhodné operace nad agendou, po každém kroku kontrola souladu seznamů a BVS
    static void testAgendaNahodne() {
        System.out.println("-- AgendaSklady: náhodné operace, soulad seznamů a BVS");
        boolean ok = true;
        for (int seed = 1; seed <= 15 && ok; seed++) {
            Random r = new Random(seed);
            AgendaSklady a = new AgendaSklady(new GeneratorProduktu(seed));
            enumSklad[] s = enumSklad.values();
            for (int krok = 0; krok < 600 && ok; krok++) {
                enumSklad sk = s[r.nextInt(s.length)];
                try {
                    switch (r.nextInt(12)) {
                        case 0, 1, 2 -> a.vloz(p("X" + r.nextInt(400)),
                                enumPozice.values()[r.nextInt(4)], sk);
                        case 3 -> a.odeber("X" + r.nextInt(400));
                        case 4 -> a.odeberProdukt(enumPozice.values()[r.nextInt(5)], sk);
                        case 5 -> a.presunProdukt(sk, s[r.nextInt(s.length)]);
                        case 6 -> a.zpristupniProdukt(enumPozice.values()[r.nextInt(5)], sk);
                        case 7 -> a.generuj(r.nextInt(5), r.nextBoolean() ? sk : null);
                        case 8 -> a.vybuduj();
                        case 9 -> {
                            if (r.nextInt(15) == 0) {
                                a.zrus(r.nextBoolean() ? sk : null);
                            }
                        }
                        default -> a.najdiVRozsahu("X" + r.nextInt(400), "X" + r.nextInt(400));
                    }
                } catch (RuntimeException e) {
                    // povolené chyby: duplicita, prázdný sklad, chybějící soused...
                }
                ok &= soulad(a);
            }
        }
        kontrola(ok, "15 × 600 náhodných kroků: seznamy a BVS vždy obsahují tytéž produkty");
    }

    // ------------------------------------------------------------------ pomocné

    // ověří: každý produkt ze seznamů je v BVS (tentýž objekt), BVS nemá nic navíc,
    // produkt.sklad odpovídá skladu, ve kterém leží, a in-order průchod je seřazený podle ID
    static boolean soulad(AgendaSklady a) {
        Map<Produkt, Boolean> veSeznamech = new IdentityHashMap<>();
        for (enumSklad s : enumSklad.values()) {
            for (Produkt p : a.zobrazProdukty(s)) {
                if (p.getSklad() != s || a.najdi(p.getId()) != p || veSeznamech.put(p, true) != null) {
                    return false;
                }
            }
        }
        Set<Produkt> veStromu = Collections.newSetFromMap(new IdentityHashMap<>());
        String predchozi = null;
        for (Iterator<Produkt> it = a.vytvorIterator(eTypProhl.HLOUBKA); it.hasNext(); ) {
            Produkt p = it.next();
            if (predchozi != null && predchozi.compareTo(p.getId()) >= 0) {
                return false;
            }
            predchozi = p.getId();
            veStromu.add(p);
        }
        return veStromu.size() == veSeznamech.size() && veStromu.containsAll(veSeznamech.keySet())
                && a.getPocetVeStromu() == veStromu.size();
    }

    // reflexí projde uzly stromu a ověří uspořádání BVS, odkazy parent a počet uzlů
    @SuppressWarnings({"unchecked", "rawtypes"})
    static boolean strukturaOk(AbstrTable<?, ?> t) throws Exception {
        Field fRoot = AbstrTable.class.getDeclaredField("root");
        fRoot.setAccessible(true);
        Object root = fRoot.get(t);
        if (root == null) {
            return t.getPocet() == 0;
        }
        Class<?> uzel = root.getClass();
        Field key = uzel.getDeclaredField("key");
        Field left = uzel.getDeclaredField("left");
        Field right = uzel.getDeclaredField("right");
        Field parent = uzel.getDeclaredField("parent");
        for (Field f : new Field[]{key, left, right, parent}) {
            f.setAccessible(true);
        }
        if (parent.get(root) != null) {
            return false;
        }
        // (uzel, dolní mez, horní mez) - kontrola bez rekurze
        ArrayDeque<Object[]> fronta = new ArrayDeque<>();
        fronta.add(new Object[]{root, null, null});
        int pocet = 0;
        while (!fronta.isEmpty()) {
            Object[] z = fronta.poll();
            Object u = z[0];
            Comparable k = (Comparable) key.get(u);
            if ((z[1] != null && k.compareTo(z[1]) <= 0) || (z[2] != null && k.compareTo(z[2]) >= 0)) {
                return false;
            }
            pocet++;
            Object l = left.get(u);
            Object p = right.get(u);
            if (l != null) {
                if (parent.get(l) != u) {
                    return false;
                }
                fronta.add(new Object[]{l, z[1], k});
            }
            if (p != null) {
                if (parent.get(p) != u) {
                    return false;
                }
                fronta.add(new Object[]{p, k, z[2]});
            }
        }
        return pocet == t.getPocet();
    }

    static Produkt p(String id) {
        return new Produkt(id, "Produkt " + id, "Test", 100, 5);
    }

    static String ids(Produkt[] pole) {
        StringBuilder sb = new StringBuilder();
        for (Produkt x : pole) {
            sb.append(x.getId()).append(' ');
        }
        return sb.toString().trim();
    }

    static String text(Iterator<?> it) {
        StringBuilder sb = new StringBuilder();
        while (it.hasNext()) {
            sb.append(it.next()).append(' ');
        }
        return sb.toString().trim();
    }

    static boolean eq(Object a, Object b) {
        return a == null ? b == null : a.equals(b);
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
        kontrola(spravne, "výjimka " + typ.getSimpleName() + ": " + popis);
    }
}
