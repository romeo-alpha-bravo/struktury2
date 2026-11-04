package abstrdoublelist;
import java.util.Iterator;

/*
 * ============================================================================
 *  AbstrDoubleList<T> — двусвязный некольцевой линейный список
 *  (obousměrně necyklicky zřetězený lineární seznam)
 * ============================================================================
 *
 *  Как это выглядит в памяти (пример списка из трёх элементов A, B, C):
 *
 *        first                     current                   last
 *          |                          |                        |
 *          v                          v                        v
 *   null <- [A] <-previous- next-> [B] <-previous- next-> [C] -> null
 *
 *  - Каждый элемент лежит в отдельном узле (Node). Узел хранит данные (data)
 *    и две ссылки: на предыдущий узел (previous) и на следующий (next).
 *  - "Двусвязный" = в обе стороны есть ссылки, можно ходить вперёд и назад.
 *  - "Некольцевой" = у первого узла previous == null, у последнего next == null,
 *    конец НЕ замкнут на начало.
 *  - Список держит три указателя:
 *      first   — первый узел,
 *      last    — последний узел,
 *      current — "текущий" узел (aktuální prvek), это курсор, относительно
 *                которого работают операции "předchůdce" / "následník".
 *
 *  Инварианты (то, что ВСЕГДА должно быть правдой после любого метода):
 *  - пустой список  <=> first == null && last == null && current == null;
 *  - непустой список => first, last и current не null;
 *  - first.previous == null, last.next == null;
 *  - для любого узла X внутри: X.next.previous == X и X.previous.next == X.
 *
 *  Сложность: все вставки, удаления и доступы здесь O(1), потому что место
 *  операции всегда известно по указателю (first / last / current).
 *  Обход итератором — O(n).
 */
public class AbstrDoubleList<T>  implements IAbstrDoubleList <T> {

    // Узел списка. static = узлу не нужна ссылка на сам список (экономит память).
    // Свой параметр типа <E>, потому что static-класс не видит <T> внешнего класса;
    // используется всегда как Node<T>.
    private static class Node <E>{
        E data;           // сами данные (у нас — ссылка на Produkt)
        Node<E> previous; // предыдущий узел или null, если это первый
        Node<E> next;     // следующий узел или null, если это последний


        public Node(E data) {
            this.data = data;   // previous и next по умолчанию null
        }
    }

    private Node<T> first;    // первый узел (null = список пуст)
    private Node<T> current;  // текущий узел, "курсор"
    private Node<T> last;     // последний узел




    // Удалить весь список за O(1).
    // Достаточно забыть три указателя: на узлы больше никто не ссылается,
    // и их освободит сборщик мусора (garbage collector). В C пришлось бы
    // пройти все узлы и вызвать free() у каждого — там это было бы O(n).
    @Override
    public void zrus() {

        first = null;
        current = null;
        last = null;

    }

    // Пустой ли список. Хватает проверить first: если нет первого, нет ничего.
    @Override
    public boolean jePrazdny() {


        return first == null;
    }

    // Вставить в НАЧАЛО списка. O(1).
    // Текущий элемент НЕ меняется (кроме случая пустого списка).
    @Override
    public void vlozPrvni(T data) {
        Node<T> new_node = new Node<>(data);


       if (first == null){
            // Список пуст: новый узел становится сразу и первым, и последним,
            // и текущим (по заданию — вложение в пустой seznam делает его aktuální).
            first = new_node;
            current = new_node;
            last = new_node;
       }
       else {
           // Было:   first -> [A] ...
           // Станет: first -> [N] <-> [A] ...
           first.previous = new_node;   // старый первый теперь знает, кто перед ним
           new_node.next = first;       // новый узел указывает вперёд на старый первый

           first = new_node;            // и только теперь сдвигаем указатель first
       }

    }

    // Вставить в КОНЕЦ списка. O(1). Зеркально vlozPrvni.
    @Override
    public void vlozPosledni(T data) {
        Node<T> new_node = new Node<>(data);



        if(last == null){
            // пустой список: узел = first = last = current
            first = new_node;
            last = new_node;
            current = new_node;
        }
        else{
            // Было:   ... [Z] <- last
            // Станет: ... [Z] <-> [N] <- last
            last.next = new_node;
            new_node.previous = last;

            last = new_node;
        }
    }
//vložení prvku do seznamu jakožto následníka
// aktuálního prvku

    // Вставить СРАЗУ ПОСЛЕ текущего элемента. O(1). current не меняется.
    //
    //   Было:   [current] <-> [after]
    //   Станет: [current] <-> [successor] <-> [after]
    //
    // Нужно переставить 4 ссылки. Порядок важен: сначала запоминаем after
    // (иначе, переписав current.next, мы бы его потеряли).
    @Override
    public void vlozNaslednika(T data) {
        // if it is a successor - it has to go after his parent

        // нет текущего = список пуст -> вставлять "после чего?" не после чего
        if(current == null){
            throw new AbstrDoubleListException("List is empty,there is no way you can insert a successor!");
        }

        Node<T> successor = new Node<>(data);
        Node<T> after = current.next;     // кто стоял после current (может быть null)

        successor.previous = current;     // ссылка 1: новый смотрит назад на current
        successor.next = after;           // ссылка 2: новый смотрит вперёд на after

        current.next = successor;         // ссылка 3: current смотрит вперёд на новый

        // if after is null -> successor becomes  last element; A X
        if (after == null){
            // current был последним -> новый узел теперь последний
            last = successor;
        }// if after is not null -> successor becomes prev elem of after; A X C
        else{
            after.previous = successor;   // ссылка 4: after смотрит назад на новый
        }



    }
    //vložení prvku do seznamu jakožto předchůdce
    //aktuálního prvku

    // Вставить СРАЗУ ПЕРЕД текущим элементом. O(1). Зеркально vlozNaslednika.
    //
    //   Было:   [before] <-> [current]
    //   Станет: [before] <-> [ancestor] <-> [current]
    @Override
    public void vlozPredchudce(T data) {
        // if it is an ancestor - it has to go before the parent

        if (current == null){
            throw  new AbstrDoubleListException("List is empty,there is no way you can insert an ancestor!");
        }

        Node<T> ancestor = new Node<>(data);
        Node<T> before = current.previous;  // кто стоял перед current (может быть null)

        ancestor.next = current;
        ancestor.previous = before;

        current.previous = ancestor;

        if(before == null){ // if the before is null -> an ancestor becomes the first elem
            // current был первым -> новый узел теперь первый
            first = ancestor;
        }
        else{ // if before is not null -> an ancestor becomes the next elem of before

           before.next = ancestor;

        }


    }
//
//zpřístupnění aktuálního prvku seznamu;Pozn.
// Operace typu zpřístupni, přenastavují pozici aktuálního prvku

    // ---- Операции zpřístupni ("сделать доступным") ----
    // Возвращают данные и ПЕРЕСТАВЛЯЮТ current на этот элемент
    // (кроме zpristupniAktualni — тот current просто читает).
    // Если элемента нет, бросают исключение, а current остаётся на месте.

    // Вернуть данные текущего элемента. O(1). current не меняется.
    @Override
    public T zpristupniAktualni() {
        if(current == null){
            throw  new AbstrDoubleListException("List is empty,there is now way you can access current element!");
        }

        return current.data;
    }

    // zpřístupnění prvního prvku seznamu -> first becomes the current element
    // Сделать текущим первый элемент и вернуть его. O(1).
    @Override
    public T zpristupniPrvni() {
        if (first == null){
            throw new AbstrDoubleListException("List is empty, there is no first element!");
        }

        current = first;
        return current.data;
    }

    // zpřístupnění posledního prvku seznamu -> last becomes the current element
    // Сделать текущим последний элемент и вернуть его. O(1).
    @Override
    public T zpristupniPosledni() {
        if (last == null){
            throw new AbstrDoubleListException("List is empty, there is no last element!");
        }

        current = last;
        return current.data;
    }

    // zpřístupnění následníka aktuálního prvku -> current moves one step forward
    // Шаг курсора ВПЕРЁД: current = current.next. O(1).
    @Override
    public T zpristupniNaslednika() {
        if (current == null){
            throw new AbstrDoubleListException("List is empty, there is no current element!");
        }

        Node<T> after = current.next;

        // current is the last element -> nothing after it, current stays where it was
        // сначала ПРОВЕРКА, потом сдвиг: при ошибке курсор не должен уехать в null
        if (after == null){
            throw new AbstrDoubleListException("Current element has no successor!");
        }

        current = after;
        return current.data;
    }

    // zpřístupnění předchůdce aktuálního prvku -> current moves one step back
    // Шаг курсора НАЗАД: current = current.previous. O(1).
    // Это возможно только потому, что список двусвязный (в односвязном было бы O(n)).
    @Override
    public T zpristupniPredchudce() {
        if (current == null){
            throw new AbstrDoubleListException("List is empty, there is no current element!");
        }

        Node<T> before = current.previous;

        // current is the first element -> nothing before it, current stays where it was
        if (before == null){
            throw new AbstrDoubleListException("Current element has no predecessor!");
        }

        current = before;
        return current.data;
    }

    ///** Vyjme uzel ze seznamu: sousedy spojí mezi sebou a opraví first/last.
    // *  O aktuální prvek se stará volající metoda. */

    // Вспомогательный метод: "вырезать" узел из цепочки. O(1).
    // Им пользуются ВСЕ odeber... — логика удаления написана один раз.
    //
    //   Было:   [before] <-> [node] <-> [after]
    //   Станет: [before] <-----------> [after]
    //
    // Если соседа нет (node был первым или последним), правим first / last.
    // current НЕ трогаем — что с ним делать, решает вызывающий метод.
    private void unlink(Node<T> node){
        Node<T> before = node.previous;
        Node<T> after = node.next;

        if(before == null){
            first = after;          // удаляем первый -> первым становится следующий
        }else{
            before.next = after;    // левый сосед перепрыгивает через node
        }

        if (after == null){
            last = before;          // удаляем последний -> последним становится предыдущий
        }else{
            after.previous = before; // правый сосед перепрыгивает через node назад
        }

        // обнуляем ссылки вырезанного узла, чтобы он не "держал" соседей
        node.previous = null;
        node.next = null;

    }

    // odebrání (vyjmutí) aktuálního prvku ze seznamu poté je
    //aktuální prvek nastaven na první prvek

    // Удалить ТЕКУЩИЙ элемент и вернуть его данные. O(1).
    // По заданию после этого текущим становится ПЕРВЫЙ элемент
    // (или null, если список опустел).
    @Override
    public T odeberAktualni() {
        if(current == null){
            throw new AbstrDoubleListException("List is empty!");
        }
        Node<T> temp = current;   // запоминаем, чтобы вернуть данные
        unlink(temp);
        current = first;          // first уже исправлен в unlink (может быть null)
        return temp.data;

    }

    // Удалить ПЕРВЫЙ элемент. O(1).
    // Если удаляемый был текущим, текущим становится новый первый.
    // wasCurrent считаем ДО unlink: после него сравнивать уже не с чем.
    @Override
    public T odeberPrvni() {
        if(first == null){
            throw new AbstrDoubleListException("List is empty!");
        }

        Node<T> temp = first;
        boolean wasCurrent = (temp == current);

        unlink(temp);

        if(wasCurrent){
            current = first; // new first or null if list is empty
        }

        return temp.data;

    }

    // Удалить ПОСЛЕДНИЙ элемент. O(1) — благодаря указателю last и ссылке previous.
    // (В односвязном списке пришлось бы искать предпоследний с начала — O(n).)
    @Override
    public T odeberPosledni() {
        if (last == null){
            throw new  AbstrDoubleListException("Last element is null!");
        }

        Node<T> temp = last;
        boolean wasCurrent = (temp == current);
        unlink(temp);

        if(wasCurrent){
            current = first; // new first or null if list is empty
        }

        return temp.data;
    }

    // Удалить элемент ПОСЛЕ текущего. O(1). current не меняется.
    @Override
    public T odeberNaslednika() {
        if (current == null){
            throw new AbstrDoubleListException("List is empty!");
        }

        Node<T> after = current.next;
        if(after == null){
            // current последний — удалять нечего
            throw new AbstrDoubleListException("Current element has no successor!");
        }
        unlink(after);

        return after.data;
    }

    // Удалить элемент ПЕРЕД текущим. O(1). current не меняется.
    // Важно: проверка before == null идёт ДО unlink, иначе unlink(null) упал бы
    // с NullPointerException.
    @Override
    public T odeberPredchudce() {
        if (current == null){
            throw new AbstrDoubleListException("List is empty!");
        }

        Node<T> before = current.previous;

        if (before == null){
            throw new AbstrDoubleListException("Current element has no predecessor!");
        }
        unlink(before);

        return before.data;
    }

    // Итератор: позволяет писать for (T x : seznam) { ... }.
    // Java превращает такой цикл в:
    //     Iterator<T> it = seznam.iterator();
    //     while (it.hasNext()) { T x = it.next(); ... }
    //
    // Итератор ходит СВОИМ указателем cursor и НЕ трогает current —
    // поэтому обход (например, подсчёт средней цены) не сбивает курсор списка.
    // Каждый вызов iterator() создаёт новый объект со своим cursor,
    // так что два обхода одновременно друг другу не мешают.
    @Override
    public Iterator<T> iterator() {
        // анонимный класс: объект, реализующий интерфейс Iterator, создаётся "на месте"
        return new Iterator<T>() {

            private Node<T> cursor = first;   // обход начинается с первого узла

            @Override
            public boolean hasNext() {
                return cursor != null;        // null = вышли за последний узел
            }

            @Override
            public T next() {

                if (cursor == null){
                    throw  new AbstrDoubleListException("No more elements!");
                }
               T data = cursor.data;   // 1) запомнить данные
                cursor = cursor.next;  // 2) шагнуть вперёд

                return data;           // 3) вернуть запомненное
            }
        };
    }
}
