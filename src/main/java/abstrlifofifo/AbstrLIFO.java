package abstrlifofifo;

import abstrdoublelist.AbstrDoubleList;
import abstrdoublelist.IAbstrDoubleList;

import java.util.Iterator;

// Zásobník (LIFO - last in, first out) nad seznamem ze semestrální práce A.
// Vrchol zásobníku = první prvek seznamu, takže vložení i odebrání je O(1).
/*
 * СТЕК поверх двусвязного списка из работы A.
 * Вершина стека = НАЧАЛО списка:
 *     vloz   -> vlozPrvni  (положить наверх)        O(1)
 *     odeber -> odeberPrvni (снять сверху)           O(1)
 * Пример: vloz(1), vloz(2), vloz(3) -> список [3 2 1] -> odeber() вернёт 3, потом 2, потом 1.
 * Свой стек не пишем с нуля — переиспользуем готовую ADS (так требует задание).
 */
public class AbstrLIFO<T> implements IAbstrLifoFifo<T> {

    private final IAbstrDoubleList<T> seznam = new AbstrDoubleList<>();   // данные стека лежат в списке

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
        // своё понятное исключение вместо исключения списка
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
