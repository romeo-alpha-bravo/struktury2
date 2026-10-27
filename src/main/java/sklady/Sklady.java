package sklady;

import abstrdoublelist.AbstrDoubleList;
import abstrdoublelist.IAbstrDoubleList;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;


public class Sklady {


    private final IAbstrDoubleList<Produkt>[] sklady;

    @SuppressWarnings({"unchecked", "rawtypes"})
    public Sklady() {

        sklady = (IAbstrDoubleList<Produkt>[]) new IAbstrDoubleList[enumSklad.values().length];

        for (int i = 0; i < sklady.length; i++) {
            sklady[i] = new AbstrDoubleList<>();
        }
    }




    private IAbstrDoubleList<Produkt> seznamSkladu(enumSklad sklad) {
        if (sklad == null) {
            throw new IllegalArgumentException("Pro tuto operaci je nutné vybrat konkrétní sklad.");
        }
        return sklady[sklad.getIndex()];
    }


    private List<IAbstrDoubleList<Produkt>> vybraneSeznamy(enumSklad sklad) {
        List<IAbstrDoubleList<Produkt>> vysledek = new ArrayList<>();
        if (sklad == null) {
            for (IAbstrDoubleList<Produkt> seznam : sklady) {
                vysledek.add(seznam);
            }
        } else {
            vysledek.add(seznamSkladu(sklad));
        }
        return vysledek;
    }



    public void vlozProdukt(Produkt produkt, enumPozice pozice, enumSklad sklad) {
        if (produkt == null) {
            throw new IllegalArgumentException("Produkt nesmí být null.");
        }
        IAbstrDoubleList<Produkt> seznam = seznamSkladu(sklad);
        switch (pozice) {
            case PRVNI -> seznam.vlozPrvni(produkt);
            case POSLEDNI -> seznam.vlozPosledni(produkt);
            case PREDCHUDCE -> seznam.vlozPredchudce(produkt);
            case NASLEDNIK -> seznam.vlozNaslednika(produkt);
            default -> throw new IllegalArgumentException(
                    "Vložit lze jen na pozici první, poslední, předchůdce nebo následník.");
        }
        produkt.setSklad(sklad);   // až po úspěšném vložení
    }

    public Produkt zpristupniProdukt(enumPozice pozice, enumSklad sklad) {
        IAbstrDoubleList<Produkt> seznam = seznamSkladu(sklad);
        return switch (pozice) {
            case PRVNI -> seznam.zpristupniPrvni();
            case POSLEDNI -> seznam.zpristupniPosledni();
            case PREDCHUDCE -> seznam.zpristupniPredchudce();
            case NASLEDNIK -> seznam.zpristupniNaslednika();
            case AKTUALNI -> seznam.zpristupniAktualni();
        };
    }

    public Produkt odeberProdukt(enumPozice pozice, enumSklad sklad) {
        IAbstrDoubleList<Produkt> seznam = seznamSkladu(sklad);
        return switch (pozice) {
            case PRVNI -> seznam.odeberPrvni();
            case POSLEDNI -> seznam.odeberPosledni();
            case PREDCHUDCE -> seznam.odeberPredchudce();
            case NASLEDNIK -> seznam.odeberNaslednika();
            case AKTUALNI -> seznam.odeberAktualni();
        };
    }


    public void presunProdukt(enumSklad zdroj, enumSklad cil) {
        IAbstrDoubleList<Produkt> odkud = seznamSkladu(zdroj);
        IAbstrDoubleList<Produkt> kam = seznamSkladu(cil);
        Produkt produkt = odkud.odeberAktualni();
        kam.vlozPosledni(produkt);
        produkt.setSklad(cil);
    }

    // Odebere konkrétní produkt ze seznamu jeho skladu (podle identity objektu, ne podle pozice).
    // Seznam umí odebírat jen podle pozice, proto se k produktu nejdřív dojde přes zpřístupňování.
    // Aktuální prvek zůstane tam, kde byl (pokud se neodebíral právě on). Složitost O(n).
    public boolean odeberKonkretni(Produkt produkt) {
        if (produkt == null || produkt.getSklad() == null) {
            return false;
        }
        IAbstrDoubleList<Produkt> seznam = seznamSkladu(produkt.getSklad());
        int pozice = poradi(seznam, produkt);
        if (pozice < 0) {
            return false;
        }
        Produkt puvodniAktualni = seznam.zpristupniAktualni();
        int puvodniPozice = poradi(seznam, puvodniAktualni);

        presunNa(seznam, pozice);
        seznam.odeberAktualni();

        if (puvodniAktualni != produkt) {
            // za odebraným prvkem se všechno posunulo o jedno místo dopředu
            presunNa(seznam, puvodniPozice > pozice ? puvodniPozice - 1 : puvodniPozice);
        }
        return true;
    }

    // pořadí produktu v seznamu (0 = první), nebo -1, když tam není; prochází se iterátorem
    private static int poradi(IAbstrDoubleList<Produkt> seznam, Produkt hledany) {
        int i = 0;
        for (Produkt p : seznam) {
            if (p == hledany) {
                return i;
            }
            i++;
        }
        return -1;
    }

    // nastaví aktuální prvek na zadané pořadí
    private static void presunNa(IAbstrDoubleList<Produkt> seznam, int pozice) {
        seznam.zpristupniPrvni();
        for (int i = 0; i < pozice; i++) {
            seznam.zpristupniNaslednika();
        }
    }



    public float zjistiPrumernouCenu(enumSklad sklad) {
        double soucet = 0;
        int pocet = 0;
        for (IAbstrDoubleList<Produkt> seznam : vybraneSeznamy(sklad)) {
            for (Produkt p : seznam) {
                soucet += p.getCena();
                pocet++;
            }
        }
        if (pocet == 0) {
            return 0f;
        }

        double prumer = soucet / pocet;
        return (float) prumer;
    }

    public Produkt[] zobrazProdukty(enumSklad sklad) {
        List<Produkt> vysledek = new ArrayList<>();
        for (IAbstrDoubleList<Produkt> seznam : vybraneSeznamy(sklad)) {
            for (Produkt p : seznam) {
                vysledek.add(p);
            }
        }
        return vysledek.toArray(new Produkt[0]);
    }

    public Produkt[] zobrazProduktyPodLimitem(enumSklad sklad, int limit) {
        List<Produkt> vysledek = new ArrayList<>();
        for (IAbstrDoubleList<Produkt> seznam : vybraneSeznamy(sklad)) {
            for (Produkt p : seznam) {
                if (p.getMnozstvi() < limit) {
                    vysledek.add(p);
                }
            }
        }
        return vysledek.toArray(new Produkt[0]);
    }

    public void zrus(enumSklad sklad) {
        for (IAbstrDoubleList<Produkt> seznam : vybraneSeznamy(sklad)) {
            seznam.zrus();
        }
    }




    public int importData(String soubor) {
        int nacteno = 0;
        for (Produkt produkt : nactiProdukty(soubor)) {
            seznamSkladu(produkt.getSklad()).vlozPosledni(produkt);
            nacteno++;
        }
        return nacteno;
    }

    // Přečte soubor sklad;id;nazev;kategorie;cena;mnozstvi a vrátí platné produkty
    // s nastaveným skladem. Hlavička a vadné řádky se přeskočí. Do skladů nic nevkládá,
    // takže to může použít i AgendaSklady (která navíc hlídá duplicitní ID).
    public static List<Produkt> nactiProdukty(String soubor) {
        List<Produkt> produkty = new ArrayList<>();
        for (String radek : nactiRadky(Path.of(soubor))) {
            String[] casti = radek.split(";");
            if (casti.length < 6) {
                continue;
            }
            try {
                enumSklad sklad = enumSklad.zCisla(Integer.parseInt(casti[0].trim()));
                Produkt produkt = new Produkt(
                        casti[1], casti[2], casti[3],
                        Double.parseDouble(casti[4].trim().replace(',', '.')),
                        Integer.parseInt(casti[5].trim()));
                produkt.setSklad(sklad);
                produkty.add(produkt);
            } catch (IllegalArgumentException e) {
                // hlavička, špatné číslo, neexistující sklad nebo neplatný produkt -> řádek přeskočit
            }
        }
        return produkty;
    }


    private static List<String> nactiRadky(Path cesta) {
        List<String> radky;
        try {
            try {
                radky = Files.readAllLines(cesta, StandardCharsets.UTF_8);
            } catch (MalformedInputException e) {
                radky = Files.readAllLines(cesta, Charset.forName("windows-1250"));
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Soubor nelze načíst: " + cesta, e);
        }

        if (!radky.isEmpty() && radky.get(0).startsWith("﻿")) {
            radky.set(0, radky.get(0).substring(1));
        }
        return radky;
    }
}
