package sklady;

/*
 * Пять складов как константы. enum вместо int: нельзя случайно передать склад 7 или -1,
 * компилятор не даст. У каждой константы есть поле cislo (номер склада 1..5).
 * SKLAD_1(1) = вызов конструктора enumSklad(1) при загрузке класса.
 */
public enum enumSklad {
    SKLAD_1(1), SKLAD_2(2), SKLAD_3(3), SKLAD_4(4), SKLAD_5(5);

    private final int cislo;

    enumSklad(int cislo) {
        this.cislo = cislo;
    }


    // номер склада 1..5 (как в sklady.csv и в окне)
    public int getCislo() {
        return cislo;
    }


    // индекс в массиве складов: SKLAD_1 -> 0, ..., SKLAD_5 -> 4
    public int getIndex() {
        return cislo - 1;
    }


    // подпись для окна и логов: "Sklad 3"
    public String getNazev() {
        return "Sklad " + cislo;
    }


    // число -> константа (3 -> SKLAD_3); нужно при чтении CSV, где склад записан цифрой.
    // static: вызывается на типе (enumSklad.zCisla(3)), а не на конкретном складе.
    public static enumSklad zCisla(int cislo) {
        for (enumSklad sklad : values()) {
            if (sklad.cislo == cislo) {
                return sklad;
            }
        }
        throw new IllegalArgumentException("Číslo skladu musí být 1 až " + values().length + ", zadáno: " + cislo);
    }
}
