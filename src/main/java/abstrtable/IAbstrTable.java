package abstrtable;

import abstrdoublelist.IAbstrDoubleList;

import java.util.Iterator;

public interface IAbstrTable<K extends Comparable<K>, V> {
    void zrus();

    boolean jePrazdny();

    V najdi(K key);

    void vloz(K key, V value);

    V odeber(K key);

    Iterator<V> vytvorIterator(eTypProhl typ);

    IAbstrDoubleList<V> najdiVRozsahu(K od, K doKlic);

    void vybuduj();

    int getPocet();

    int getVyska();

    int getNejmensiVyska();
}
