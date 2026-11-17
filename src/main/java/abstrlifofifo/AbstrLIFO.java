package abstrlifofifo;

import abstrdoublelist.AbstrDoubleList;
import abstrdoublelist.IAbstrDoubleList;

import java.util.Iterator;

public class AbstrLIFO<T> implements IAbstrLifoFifo<T> {
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
        seznam.vlozPrvni(data);
    }

    @Override
    public T odeber() {
        if (seznam.jePrazdny()) {
            throw new AbstrLifoFifoException("Zásobník je prázdný.");
        }
        return seznam.odeberPrvni();
    }

    @Override
    public Iterator<T> vytvorIterator() {
        return seznam.iterator();
    }
}
