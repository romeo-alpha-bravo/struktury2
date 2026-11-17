package abstrlifofifo;

import abstrdoublelist.AbstrDoubleList;
import abstrdoublelist.IAbstrDoubleList;

import java.util.Iterator;

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

    @Override
    public void vloz(T data) {
        seznam.vlozPosledni(data);
    }

    @Override
    public T odeber() {
        if (seznam.jePrazdny()) {
            throw new AbstrLifoFifoException("Fronta je prázdná.");
        }
        return seznam.odeberPrvni();
    }

    @Override
    public Iterator<T> vytvorIterator() {
        return seznam.iterator();
    }
}
