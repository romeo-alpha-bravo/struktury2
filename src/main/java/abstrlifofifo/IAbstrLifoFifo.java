package abstrlifofifo;

import java.util.Iterator;

public interface IAbstrLifoFifo<T> extends Iterable<T> {
    void zrus();

    boolean jePrazdny();

    void vloz(T data);

    T odeber();

    Iterator<T> vytvorIterator();

    @Override
    default Iterator<T> iterator() {
        return vytvorIterator();
    }
}
