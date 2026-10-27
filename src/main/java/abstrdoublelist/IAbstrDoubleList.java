package abstrdoublelist;

import java.util.Iterator;

/*
 * Интерфейс = "договор": КАКИЕ операции есть у списка, без того, КАК они сделаны.
 * Это и есть "abstraktní datová struktura" (ADS): Sklady, AbstrLIFO и AbstrFIFO
 * работают с типом IAbstrDoubleList и не знают, что внутри узлы со ссылками.
 *
 * <T> — тип хранимых данных (у нас Produkt). extends Iterable<T> — значит, по списку
 * можно ходить циклом for-each (Java вызовет iterator()).
 *
 * Комментарии на чешском ниже — формулировки из задания.
 */
public interface IAbstrDoubleList<T> extends Iterable<T>{
    //zrušení celého seznamu,
    void zrus();

    //test naplněnosti seznamu,
    boolean jePrazdny();

    // vložení prvku do seznamu na první místo
    void vlozPrvni(T data);

    //vložení prvku do seznamu na poslední místo
    void vlozPosledni(T data);

    //vložení prvku do seznamu jakožto následníka
    //aktuálního prvku
    void vlozNaslednika(T data);

    //vložení prvku do seznamu jakožto předchůdce
    //aktuálního prvku
    void vlozPredchudce(T data);


    //zpřístupnění aktuálního prvku seznamu;Pozn.
    // Operace typu zpřístupni, přenastavují pozici aktuálního prvku
    T zpristupniAktualni();

    //zpřístupnění prvního prvku seznamu
    T zpristupniPrvni();

    //zpřístupnění posledního prvku seznamu
    T zpristupniPosledni();

    //zpřístupnění následníka aktuálního prvku
    T zpristupniNaslednika();

    //zpřístupnění předchůdce aktuálního prvku
    T zpristupniPredchudce();


    // odebrání (vyjmutí) aktuálního prvku ze seznamu poté je
    //aktuální prvek nastaven na první prvek
    T odeberAktualni();

    // odebrání prvního prvku ze seznamu,
    T odeberPrvni();


    //odebrání posledního prvku ze seznamu
    T odeberPosledni();


    //odebrání následníka aktuálního prvku ze seznamu,
    T odeberNaslednika();


    //odebrání předchůdce aktuálního prvku ze seznamu,
    T odeberPredchudce();

    //vytvoří iterátor (dle rozhraní Iterable)
    @Override
    Iterator<T> iterator();

}
