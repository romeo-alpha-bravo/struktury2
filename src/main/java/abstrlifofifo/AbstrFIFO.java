package abstrlifofifo;

import abstrdoublelist.AbstrDoubleList;
import abstrdoublelist.IAbstrDoubleList;

import java.util.Iterator;

// Fronta (FIFO - first in, first out) nad seznamem ze semestrální práce A.
// Vkládá se na konec seznamu, odebírá se ze začátku, obojí O(1).
/*
 * ОЧЕРЕДЬ поверх двусвязного списка из работы A.
 *     vloz   -> vlozPosledni (встать в конец очереди)   O(1)
 *     odeber -> odeberPrvni  (выйти из начала очереди)   O(1)
 * Обе операции O(1) только потому, что у списка есть указатели и на first, и на last.
 * Пример: vloz(1), vloz(2), vloz(3) -> список [1 2 3] -> odeber() вернёт 1, потом 2, потом 3.
 */
public class AbstrFIFO<T> implements IAbstrLifoFifo<T> {

    private final IAbstrDoubleList<T> seznam = new AbstrDoubleList<>();   // данные очереди лежат в списке

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
