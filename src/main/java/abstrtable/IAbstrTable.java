package abstrtable;

import abstrdoublelist.IAbstrDoubleList;

import java.util.Iterator;

// Vyhledávací tabulka realizovaná binárním vyhledávacím stromem (BVS)
public interface IAbstrTable<K extends Comparable<K>, V> {

    // zruší celou tabulku
    void zrus();

    // test prázdnosti tabulky
    boolean jePrazdny();

    // vyhledá a vrátí hodnotu podle klíče, jinak null
    V najdi(K key);

    // vloží záznam; duplicitní klíč odmítne výjimkou AbstrTableException
    void vloz(K key, V value);

    // odebere záznam a vrátí jeho hodnotu, jinak null
    V odeber(K key);

    // iterátor hodnot v zadaném pořadí (do šířky / do hloubky in-order)
    Iterator<V> vytvorIterator(eTypProhl typ);

    // hodnoty s klíčem v intervalu <od; doKlic> (včetně mezí) seřazené vzestupně podle klíče,
    // složitost O(h + k), u vyváženého stromu O(log n + k)
    IAbstrDoubleList<V> najdiVRozsahu(K od, K doKlic);

    // přestaví strom na vyvážený volbou středních klíčů, O(n)
    void vybuduj();

    // počet záznamů
    int getPocet();

    // výška stromu (počet úrovní, prázdný strom = 0)
    int getVyska();
}
