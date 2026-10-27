package sklady;

public enum enumSklad {
    SKLAD_1(1), SKLAD_2(2), SKLAD_3(3), SKLAD_4(4), SKLAD_5(5);

    private final int cislo;

    enumSklad(int cislo) {
        this.cislo = cislo;
    }


    public int getCislo() {
        return cislo;
    }


    public int getIndex() {
        return cislo - 1;
    }


    public String getNazev() {
        return "Sklad " + cislo;
    }


    public static enumSklad zCisla(int cislo) {
        for (enumSklad sklad : values()) {
            if (sklad.cislo == cislo) {
                return sklad;
            }
        }
        throw new IllegalArgumentException("Číslo skladu musí být 1 až " + values().length + ", zadáno: " + cislo);
    }
}
