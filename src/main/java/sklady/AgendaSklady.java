package sklady;

import abstrdoublelist.IAbstrDoubleList;
import abstrtable.AbstrTable;
import abstrtable.IAbstrTable;
import abstrtable.eTypProhl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

// Modul AgendaSklady: navazuje na Sklady (seznamy produktů po skladech) a přidává
// vyhledávací BVS podle ID produktu. Hlídá, aby obě struktury obsahovaly stejné produkty:
//  - vložení / import / generování: nastaví produkt.sklad, vloží do seznamu i do BVS
//  - odebrání: odstraní produkt ze seznamu i z BVS
//  - přesun: změní jen produkt.sklad a seznamy, BVS ne (klíč ID se nemění)
// BVS ukládá jen odkazy na tytéž objekty Produkt, žádné kopie.
public class AgendaSklady {

    private final Sklady sklady = new Sklady();
    private final IAbstrTable<String, Produkt> strom = new AbstrTable<>();
    private final GeneratorProduktu generator;

    private int posledniDuplicity;   // kolik řádků přeskočil poslední import kvůli duplicitnímu ID

    public AgendaSklady() {
        this(new GeneratorProduktu());
    }

    public AgendaSklady(GeneratorProduktu generator) {
        this.generator = generator;
    }

    // ================================================================ podle ID (BVS)

    // vyhledá produkt podle ID, O(h); vrací null, když neexistuje
    public Produkt najdi(String id) {
        return strom.najdi(upravId(id));
    }

    // vloží produkt do seznamu skladu na zadanou pozici a do BVS; duplicitní ID odmítne
    public void vloz(Produkt produkt, enumPozice pozice, enumSklad sklad) {
        if (produkt == null) {
            throw new IllegalArgumentException("Produkt nesmí být null.");
        }
        Produkt existujici = strom.najdi(produkt.getId());
        if (existujici != null) {
            throw new IllegalArgumentException("Produkt s ID " + produkt.getId() + " už existuje ("
                    + existujici.getSklad().getNazev() + "). ID musí být jedinečné napříč sklady.");
        }
        sklady.vlozProdukt(produkt, pozice, sklad);   // může selhat (např. následník v prázdném skladu)
        strom.vloz(produkt.getId(), produkt);         // sem se dojde jen po úspěšném vložení do seznamu
    }

    // odebere produkt podle ID z BVS i ze seznamu jeho skladu; vrací null, když neexistuje
    public Produkt odeber(String id) {
        Produkt produkt = strom.najdi(upravId(id));
        if (produkt == null) {
            return null;
        }
        sklady.odeberKonkretni(produkt);
        strom.odeber(produkt.getId());
        return produkt;
    }

    // produkty s ID v intervalu <idOd; idDo> včetně mezí, vzestupně podle ID; nic -> prázdné pole
    public Produkt[] najdiVRozsahu(String idOd, String idDo) {
        IAbstrDoubleList<Produkt> nalezene = strom.najdiVRozsahu(upravId(idOd), upravId(idDo));
        List<Produkt> vysledek = new ArrayList<>();
        for (Produkt p : nalezene) {
            vysledek.add(p);
        }
        return vysledek.toArray(new Produkt[0]);
    }

    // vybuduje vyvážený BVS volbou středních ID (jediné místo, kde se strom vyvažuje)
    public void vybuduj() {
        strom.vybuduj();
    }

    // iterátor produktů v BVS: do šířky nebo do hloubky (in-order = vzestupně podle ID)
    public Iterator<Produkt> vytvorIterator(eTypProhl typ) {
        return strom.vytvorIterator(typ);
    }

    public int getPocetVeStromu() {
        return strom.getPocet();
    }

    public int getVyskaStromu() {
        return strom.getVyska();
    }

    // ================================================================ operace ze semestrální práce A

    public void vlozProdukt(Produkt produkt, enumPozice pozice, enumSklad sklad) {
        vloz(produkt, pozice, sklad);
    }

    public Produkt zpristupniProdukt(enumPozice pozice, enumSklad sklad) {
        return sklady.zpristupniProdukt(pozice, sklad);
    }

    // odebrání podle pozice v seznamu - produkt zmizí i z BVS
    public Produkt odeberProdukt(enumPozice pozice, enumSklad sklad) {
        Produkt produkt = sklady.odeberProdukt(pozice, sklad);
        strom.odeber(produkt.getId());
        return produkt;
    }

    // přesune aktuální produkt zdroje na konec cíle; aktualizuje se produkt.sklad, BVS zůstává
    public void presunProdukt(enumSklad zdroj, enumSklad cil) {
        sklady.presunProdukt(zdroj, cil);
    }

    public float zjistiPrumernouCenu(enumSklad sklad) {
        return sklady.zjistiPrumernouCenu(sklad);
    }

    public Produkt[] zobrazProdukty(enumSklad sklad) {
        return sklady.zobrazProdukty(sklad);
    }

    public Produkt[] zobrazProduktyPodLimitem(enumSklad sklad, int limit) {
        return sklady.zobrazProduktyPodLimitem(sklad, limit);
    }

    // zruší produkty skladu (null = všech skladů) v seznamech i v BVS
    public void zrus(enumSklad sklad) {
        if (sklad == null) {
            sklady.zrus(null);
            strom.zrus();
            return;
        }
        for (Produkt p : sklady.zobrazProdukty(sklad)) {
            strom.odeber(p.getId());
        }
        sklady.zrus(sklad);
    }

    // ================================================================ vstup dat

    // import ze souboru; produkty s ID, které už existuje (v souboru nebo v aplikaci), se přeskočí
    public int importData(String soubor) {
        int nacteno = 0;
        int duplicity = 0;
        for (Produkt p : Sklady.nactiProdukty(soubor)) {
            if (strom.najdi(p.getId()) != null) {
                duplicity++;
                continue;
            }
            vloz(p, enumPozice.POSLEDNI, p.getSklad());
            nacteno++;
        }
        posledniDuplicity = duplicity;
        return nacteno;
    }

    public int getPosledniDuplicity() {
        return posledniDuplicity;
    }

    // vygeneruje produkty na konec skladu (sklad == null -> každý do náhodného skladu)
    // a vloží je i do BVS; vygenerované ID, které už existuje, se přeskočí a generuje se další
    public int generuj(int pocet, enumSklad sklad) {
        if (pocet < 0) {
            throw new IllegalArgumentException("Počet nesmí být záporný.");
        }
        int vlozeno = 0;
        int pokusu = 0;
        while (vlozeno < pocet && pokusu < pocet + 10_000) {
            pokusu++;
            Produkt p = generator.dalsi();
            if (strom.najdi(p.getId()) != null) {
                continue;
            }
            vloz(p, enumPozice.POSLEDNI, sklad != null ? sklad : generator.nahodnySklad());
            vlozeno++;
        }
        return vlozeno;
    }

    private static String upravId(String id) {
        if (id == null) {
            throw new IllegalArgumentException("ID nesmí být null.");
        }
        return id.trim();
    }
}
