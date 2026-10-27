package abstrlifofifo;

import java.util.Iterator;

// společné rozhraní zásobníku (LIFO) a fronty (FIFO)
/*
 * Общий интерфейс для стека (LIFO) и очереди (FIFO): у них одинаковый набор операций,
 * отличается только то, КАКОЙ элемент возвращает odeber():
 *   LIFO (last in, first out)  — стек, "стопка тарелок": берём ПОСЛЕДНИЙ положенный;
 *   FIFO (first in, first out) — очередь в магазине: берём ПЕРВЫЙ пришедший.
 * В дереве: стек нужен для обхода в глубину (in-order), очередь — для обхода в ширину.
 */
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

    // default-метод: реализация прямо в интерфейсе. Благодаря ему по стеку/очереди
    // можно ходить циклом for-each (Iterable требует метод iterator()).
    @Override
    default Iterator<T> iterator() {
        return vytvorIterator();
    }
}
