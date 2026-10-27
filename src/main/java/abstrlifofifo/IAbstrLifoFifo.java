package abstrlifofifo;

import java.util.Iterator;

// společné rozhraní zásobníku (LIFO) a fronty (FIFO)
public interface IAbstrLifoFifo<T> extends Iterable<T> {

    // zruší celý zásobník/frontu
    void zrus();

    // test prázdnosti
    boolean jePrazdny();

    // vloží prvek
    void vloz(T data);

    // odebere a vrátí prvek (LIFO: naposledy vložený, FIFO: nejdříve vložený)
    T odeber();

    // iterátor v pořadí, v jakém by se prvky odebíraly
    Iterator<T> vytvorIterator();

    @Override
    default Iterator<T> iterator() {
        return vytvorIterator();
    }
}
