package abstrlifofifo;

import abstrdoublelist.AbstrDoubleList;
import abstrdoublelist.IAbstrDoubleList;

import java.util.Iterator;

// Fronta (FIFO - first in, first out) nad seznamem ze semestrální práce A.
// Vkládá se na konec seznamu, odebírá se ze začátku, obojí O(1).
public class AbstrFIFO<T> implements IAbstrLifoFifo<T> {

    private final IAbstrDoubleList<T> seznam = new AbstrDoubleList<>();

    @Override
    public void zrus() {
        seznam.zrus();
    }

    @Override
    public boolean jePrazdny() {
        return seznam.jePrazdny();
    }

    // vloží prvek na konec fronty
    @Override
    public void vloz(T data) {
        seznam.vlozPosledni(data);
    }

    // odebere prvek ze začátku fronty
    @Override
    public T odeber() {
        if (seznam.jePrazdny()) {
            throw new AbstrLifoFifoException("Fronta je prázdná.");
        }
        return seznam.odeberPrvni();
    }

    // od začátku fronty ke konci
    @Override
    public Iterator<T> vytvorIterator() {
        return seznam.iterator();
    }
}
