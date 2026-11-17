package sklady;

import abstrdoublelist.IAbstrDoubleList;
import abstrtable.AbstrTable;
import abstrtable.IAbstrTable;
import abstrtable.eTypProhl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class AgendaSklady {
    private final Sklady sklady = new Sklady();
    private final IAbstrTable<String, Produkt> strom = new AbstrTable<>();
    private final GeneratorProduktu generator;

    private int posledniDuplicity;

    public AgendaSklady() {
        this(new GeneratorProduktu());
    }

    public AgendaSklady(GeneratorProduktu generator) {
        this.generator = generator;
    }

    public Produkt najdi(String id) {
        return strom.najdi(upravId(id));
    }

    public void vloz(Produkt produkt, enumPozice pozice, enumSklad sklad) {
        if (produkt == null) {
            throw new IllegalArgumentException("Produkt nesmí být null.");
        }
        Produkt existujici = strom.najdi(produkt.getId());
        if (existujici != null) {
            throw new IllegalArgumentException("Produkt s ID " + produkt.getId() + " už existuje ("
                    + existujici.getSklad().getNazev() + "). ID musí být jedinečné napříč sklady.");
        }
        sklady.vlozProdukt(produkt, pozice, sklad);
        strom.vloz(produkt.getId(), produkt);
    }

    public Produkt odeber(String id) {
        Produkt produkt = strom.najdi(upravId(id));
        if (produkt == null) {
            return null;
        }
        sklady.odeberKonkretni(produkt);
        strom.odeber(produkt.getId());
        return produkt;
    }

    public Produkt[] najdiVRozsahu(String idOd, String idDo) {
        IAbstrDoubleList<Produkt> nalezene = strom.najdiVRozsahu(upravId(idOd), upravId(idDo));
        List<Produkt> vysledek = new ArrayList<>();
        for (Produkt p : nalezene) {
            vysledek.add(p);
        }
        return vysledek.toArray(new Produkt[0]);
    }

    public void vybuduj() {
        strom.vybuduj();
    }

    public Iterator<Produkt> vytvorIterator(eTypProhl typ) {
        return strom.vytvorIterator(typ);
    }

    public int getPocetVeStromu() {
        return strom.getPocet();
    }

    public int getVyskaStromu() {
        return strom.getVyska();
    }

    public int getNejmensiVyskaStromu() {
        return strom.getNejmensiVyska();
    }

    public void vlozProdukt(Produkt produkt, enumPozice pozice, enumSklad sklad) {
        vloz(produkt, pozice, sklad);
    }

    public Produkt zpristupniProdukt(enumPozice pozice, enumSklad sklad) {
        return sklady.zpristupniProdukt(pozice, sklad);
    }

    public Produkt odeberProdukt(enumPozice pozice, enumSklad sklad) {
        Produkt produkt = sklady.odeberProdukt(pozice, sklad);
        strom.odeber(produkt.getId());
        return produkt;
    }

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
