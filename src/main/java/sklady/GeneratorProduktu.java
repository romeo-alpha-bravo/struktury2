package sklady;

import java.util.Random;


public class GeneratorProduktu {

    private record Sablona(String nazev, String kategorie, double cenaOd, double cenaDo) { }

    private static final Sablona[] SABLONY = {
            new Sablona("iPhone 16", "iPhone", 22990, 28990),
            new Sablona("iPhone 16 Plus", "iPhone", 25990, 31990),
            new Sablona("iPhone 16 Pro", "iPhone", 29990, 37990),
            new Sablona("iPhone 16 Pro Max", "iPhone", 35990, 46990),
            new Sablona("iPhone 16e", "iPhone", 16990, 22990),
            new Sablona("iPad 11\"", "iPad", 9990, 14990),
            new Sablona("iPad mini", "iPad", 14490, 19990),
            new Sablona("iPad Air 11\"", "iPad", 16990, 24990),
            new Sablona("iPad Pro 13\"", "iPad", 39990, 59990),
            new Sablona("MacBook Air 13\" M4", "Mac", 29990, 39990),
            new Sablona("MacBook Air 15\" M4", "Mac", 36990, 46990),
            new Sablona("MacBook Pro 14\"", "Mac", 46990, 74990),
            new Sablona("MacBook Pro 16\"", "Mac", 74990, 109990),
            new Sablona("iMac 24\"", "Mac", 36990, 54990),
            new Sablona("Mac mini", "Mac", 16990, 34990),
            new Sablona("Mac Studio", "Mac", 59990, 119990),
            new Sablona("Apple Watch SE", "Apple Watch", 6990, 8990),
            new Sablona("Apple Watch Series 10", "Apple Watch", 11490, 17990),
            new Sablona("Apple Watch Ultra 2", "Apple Watch", 22990, 24990),
            new Sablona("AirPods 4", "Audio", 3690, 4990),
            new Sablona("AirPods Pro 2", "Audio", 6490, 6990),
            new Sablona("AirPods Max", "Audio", 13990, 14990),
            new Sablona("HomePod mini", "Domácnost", 2690, 2990),
            new Sablona("HomePod", "Domácnost", 8490, 8990),
            new Sablona("Apple TV 4K", "Domácnost", 3990, 4990),
            new Sablona("AirTag (1 ks)", "Příslušenství", 890, 990),
            new Sablona("Magic Keyboard", "Příslušenství", 2690, 4990),
            new Sablona("Magic Mouse", "Příslušenství", 2190, 2490),
            new Sablona("Magic Trackpad", "Příslušenství", 3490, 3990),
            new Sablona("Apple Pencil Pro", "Příslušenství", 3490, 3790),
            new Sablona("Nabíječka USB-C 20 W", "Příslušenství", 490, 590),
            new Sablona("Kabel USB-C 1 m", "Příslušenství", 490, 590),
            new Sablona("Studio Display", "Monitory", 42990, 54990),
    };

    private final Random nahoda;
    private int poradi = 0;   // pro ID G0001, G0002, ...

    public GeneratorProduktu() {
        this(new Random());
    }


    public GeneratorProduktu(long seed) {
        this(new Random(seed));
    }

    private GeneratorProduktu(Random nahoda) {
        this.nahoda = nahoda;
    }


    public Produkt dalsi() {
        Sablona s = SABLONY[nahoda.nextInt(SABLONY.length)];
        poradi++;
        String id = String.format("G%04d", poradi);
        double cena = s.cenaOd() + nahoda.nextDouble() * (s.cenaDo() - s.cenaOd());
        cena = Math.round(cena / 100) * 100 - 10;
        int mnozstvi = nahoda.nextInt(41);
        return new Produkt(id, s.nazev(), s.kategorie(), cena, mnozstvi);
    }


    public enumSklad nahodnySklad() {
        enumSklad[] vsechny = enumSklad.values();
        return vsechny[nahoda.nextInt(vsechny.length)];
    }
}
