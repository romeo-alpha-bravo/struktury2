package abstrlifofifo;

import abstrdoublelist.AbstrDoubleList;
import abstrdoublelist.IAbstrDoubleList;

import java.util.Iterator;

// Zásobník (LIFO - last in, first out) nad seznamem ze semestrální práce A.
// Vrchol zásobníku = první prvek seznamu, takže vložení i odebrání je O(1).
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

    // vloží prvek na vrchol zásobníku
    @Override
    public void vloz(T data) {
        seznam.vlozPrvni(data);
    }

    // odebere prvek z vrcholu zásobníku
    @Override
    public T odeber() {
        if (seznam.jePrazdny()) {
            throw new AbstrLifoFifoException("Zásobník je prázdný.");
        }
        return seznam.odeberPrvni();
    }

    // od vrcholu ke dnu
    @Override
    public Iterator<T> vytvorIterator() {
        return seznam.iterator();
    }
}
