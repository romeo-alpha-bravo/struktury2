package abstrtable;

import abstrdoublelist.AbstrDoubleList;
import abstrdoublelist.IAbstrDoubleList;
import abstrlifofifo.AbstrFIFO;
import abstrlifofifo.AbstrLIFO;

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;

/*
 * ============================================================================
 *  AbstrTable<K, V> — двоичное дерево поиска (BVS, binární vyhledávací strom)
 * ============================================================================
 *
 *  Главное свойство BVS (на нём держится ВСЁ остальное):
 *      для каждого узла X:
 *         все ключи в ЛЕВОМ поддереве  <  X.key
 *         все ключи в ПРАВОМ поддереве >  X.key
 *
 *  Пример (ключи — ID продуктов):
 *
 *                    P004            <- root (корень)
 *                  /      \
 *              P002        P006
 *             /    \      /    \
 *          P001   P003  P005   P007   <- листья (у них left == right == null)
 *
 *  Каждый узел хранит:
 *     key    — ключ (ID продукта, String; сравнивается через compareTo),
 *     value  — значение (ССЫЛКА на тот же объект Produkt, что лежит в списке
 *              склада; копия НЕ создаётся),
 *     left   — левый сын, right — правый сын,
 *     parent — родитель (у корня null). parent нужен, чтобы при удалении
 *              быстро переподключить узел к родителю без рекурсии.
 *
 *  ПОЧЕМУ БЕЗ РЕКУРСИИ (требование преподавателя):
 *     - поиск / вставка / удаление — обычные циклы while, спускаемся по left/right;
 *     - обход в глубину (in-order) — через стек AbstrLIFO,
 *     - обход в ширину (level-order) — через очередь AbstrFIFO,
 *     - построение сбалансированного дерева — через очередь "кусков" массива.
 *     Рекурсия по сути тоже использует стек (системный стек вызовов),
 *     здесь мы просто держим этот стек сами.
 *
 *  Сложность: поиск / вставка / удаление — O(h), где h = высота дерева.
 *     сбалансированное дерево: h ≈ log2(n)  -> O(log n)
 *     вырожденное ("палка"):   h = n        -> O(n)
 *  Вырожденное получается, если вставлять ключи ПО ПОРЯДКУ (P001, P002, ...),
 *  как раз так, как они идут в sklady.csv. Поэтому есть vybuduj().
 *
 *  <K extends Comparable<K>> — ключи должны уметь сравниваться (compareTo),
 *  иначе не понять, идти влево или вправо. String это умеет.
 */
// Binární vyhledávací strom (BVS) v dynamické paměti, všechny operace bez rekurze.
public class AbstrTable<K extends Comparable<K>, V> implements IAbstrTable<K, V> {

    // Узел дерева. static — узлу не нужна ссылка на само дерево.
    private static class Uzel<K, V> {
        K key;
        V value;
        Uzel<K, V> left;
        Uzel<K, V> right;
        Uzel<K, V> parent;

        Uzel(K key, V value, Uzel<K, V> parent) {
            this.key = key;
            this.value = value;
            this.parent = parent;    // новый узел всегда лист: left и right = null
        }
    }

    private Uzel<K, V> root;  // корень (null = дерево пустое)
    private int pocet;        // число узлов (чтобы не считать их обходом каждый раз)

    // Счётчик изменений. Каждая операция, меняющая дерево, его увеличивает.
    // Итератор запоминает значение при создании; если во время обхода оно
    // поменялось — дерево изменили "под ногами", и итератор бросает
    // ConcurrentModificationException (так же делают коллекции Java — "fail-fast").
    private int zmeny;

    // ================================================================ základní operace (базовые операции)

    // Удалить всё дерево за O(1): забываем корень, остальное соберёт garbage collector.
    @Override
    public void zrus() {
        root = null;
        pocet = 0;
        zmeny++;
    }

    @Override
    public boolean jePrazdny() {
        return root == null;
    }

    @Override
    public int getPocet() {
        return pocet;
    }

    // Найти значение по ключу. O(h). Не нашли -> null (так сказано в задании).
    @Override
    public V najdi(K key) {
        Uzel<K, V> u = najdiUzel(key);
        return u == null ? null : u.value;
    }

    // ПОИСК УЗЛА (используют najdi и odeber).
    // Идём от корня; на каждом шаге сравниваем искомый ключ с ключом узла:
    //     равны        -> нашли;
    //     искомый меньше -> идём влево (всё меньшее лежит слева);
    //     искомый больше -> идём вправо.
    // Упёрлись в null -> такого ключа нет.
    //
    // Пример: ищем P005 в дереве из шапки файла:
    //     P004: P005 > P004 -> вправо; P006: P005 < P006 -> влево; P005: нашли.
    //     3 сравнения = высота дерева, а не число узлов.
    private Uzel<K, V> najdiUzel(K key) {
        if (key == null) {
            throw new AbstrTableException("Klíč nesmí být null.");
        }
        Uzel<K, V> u = root;
        while (u != null) {
            int c = key.compareTo(u.key);  // <0: key меньше, 0: равны, >0: key больше
            if (c == 0) {
                return u;
            }
            u = c < 0 ? u.left : u.right;
        }
        return null;
    }

    // ВСТАВКА. O(h).
    // Спускаемся так же, как при поиске, пока не найдём ПУСТОЕ место (null)
    // в нужной стороне — туда вешаем новый лист. Новый узел всегда становится листом,
    // существующие узлы не двигаются.
    // Если по пути встретили такой же ключ — это дубликат, вставку отклоняем.
    //
    // Пример: вставляем P008 в дерево из шапки:
    //     P004 -> вправо, P006 -> вправо, P007 -> вправо: там null -> P008 = P007.right
    @Override
    public void vloz(K key, V value) {
        if (key == null) {
            throw new AbstrTableException("Klíč nesmí být null.");
        }
        // null-значение запрещаем: najdi возвращает null как "не найдено",
        // и тогда было бы не отличить "нет ключа" от "ключ есть, значение null"
        if (value == null) {
            throw new AbstrTableException("Hodnota nesmí být null (null znamená \"nenalezeno\").");
        }
        if (root == null) {
            root = new Uzel<>(key, value, null);   // первый узел = корень, родителя нет
        } else {
            Uzel<K, V> u = root;
            while (true) {
                int c = key.compareTo(u.key);
                if (c == 0) {
                    throw new AbstrTableException("Klíč " + key + " už v tabulce je, duplicita se odmítá.");
                }
                if (c < 0) {
                    if (u.left == null) {           // слева пусто -> сюда
                        u.left = new Uzel<>(key, value, u);   // u становится родителем
                        break;
                    }
                    u = u.left;                     // слева занято -> спускаемся дальше
                } else {
                    if (u.right == null) {          // справа пусто -> сюда
                        u.right = new Uzel<>(key, value, u);
                        break;
                    }
                    u = u.right;
                }
            }
        }
        pocet++;
        zmeny++;
    }

    // УДАЛЕНИЕ. O(h). Три случая:
    //
    //  1) у узла НЕТ детей (лист)       -> просто отцепить его от родителя;
    //  2) у узла ОДИН ребёнок            -> ребёнок встаёт на место узла;
    //  3) у узла ДВА ребёнка             -> нельзя просто вырезать (куда девать
    //     два поддерева?). Поэтому берём in-order ПРЕДШЕСТВЕННИКА — самый правый
    //     узел левого поддерева (наибольший ключ, который меньше удаляемого).
    //     Копируем его key/value в удаляемый узел и удаляем уже ПРЕДШЕСТВЕННИКА.
    //     У предшественника нет правого сына (иначе он не был бы самым правым),
    //     значит, его удаление — это случай 1 или 2.
    //     Свойство BVS сохраняется: предшественник больше всего, что слева,
    //     и меньше всего, что справа.
    //
    //  Пример (как на листке): удаляем 8
    //                8                         7
    //             /     \                   /     \
    //            6       15     ==>        6       15
    //           / \     /                 / \     /
    //          1   7   10                1  6.4  10
    //             /      \                    \     \
    //           6.4      14                   6.5    14
    //             \
    //             6.5
    //  Самый правый в левом поддереве узла 8 — это 7. Ключ 7 переезжает в корень,
    //  а сам узел 7 удаляется: у него только левый сын 6.4, он и встаёт на место 7.
    @Override
    public V odeber(K key) {
        Uzel<K, V> u = najdiUzel(key);
        if (u == null) {
            return null;                    // нечего удалять -> null (по заданию)
        }
        V hodnota = u.value;                // запоминаем, что вернуть (до перезаписи!)

        // случай 3: два ребёнка -> ищем предшественника: шаг влево, потом вправо до упора
        if (u.left != null && u.right != null) {
            Uzel<K, V> predchudce = u.left;
            while (predchudce.right != null) {
                predchudce = predchudce.right;
            }
            u.key = predchudce.key;         // предшественник "переезжает" в узел u
            u.value = predchudce.value;
            u = predchudce;                 // дальше удаляем физически его
        }

        // теперь у u максимум ОДИН ребёнок (случаи 1 и 2 сводятся к одному коду):
        // syn = единственный ребёнок или null у листа
        Uzel<K, V> syn = (u.left != null) ? u.left : u.right;
        nahradVRodici(u, syn);

        // отцепляем удалённый узел полностью
        u.left = null;
        u.right = null;
        u.parent = null;
        pocet--;
        zmeny++;
        return hodnota;
    }

    // Поставить узел "novy" на место узла "u" у родителя u (novy может быть null).
    // Именно здесь нужна ссылка parent: без неё пришлось бы искать родителя от корня.
    private void nahradVRodici(Uzel<K, V> u, Uzel<K, V> novy) {
        if (novy != null) {
            novy.parent = u.parent;                 // у ребёнка новый родитель — "дед"
        }
        if (u.parent == null) {
            root = novy;                            // u был корнем -> новый корень
        } else if (u.parent.left == u) {
            u.parent.left = novy;                   // u был левым сыном
        } else {
            u.parent.right = novy;                  // u был правым сыном
        }
    }

    // ================================================================ iterátory (итераторы = обходы)

    // Вернуть итератор ЗНАЧЕНИЙ (продуктов) в нужном порядке.
    // Внутри обходим узлы (iteratorUzlu), а наружу отдаём только value.
    @Override
    public Iterator<V> vytvorIterator(eTypProhl typ) {
        Iterator<Uzel<K, V>> uzly = iteratorUzlu(typ);
        return new Iterator<V>() {           // "обёртка": узел -> его значение
            @Override
            public boolean hasNext() {
                return uzly.hasNext();
            }

            @Override
            public V next() {
                return uzly.next().value;
            }
        };
    }

    private Iterator<Uzel<K, V>> iteratorUzlu(eTypProhl typ) {
        if (typ == null) {
            throw new AbstrTableException("Typ prohlídky nesmí být null.");
        }
        return typ == eTypProhl.HLOUBKA ? new InOrder() : new DoSirky();
    }

    // ОБХОД В ГЛУБИНУ IN-ORDER: левое поддерево -> узел -> правое поддерево.
    // Для BVS это даёт ключи ПО ВОЗРАСТАНИЮ.
    //
    // Без рекурсии, через стек:
    //   - в начале кладём в стек корень и всю его "левую ветку" (идём влево до упора);
    //     на вершине стека оказывается наименьший ключ;
    //   - next(): снимаем узел с вершины — это следующий по порядку;
    //     затем кладём левую ветку его ПРАВОГО поддерева (оно идёт сразу после узла).
    //
    // Пример на дереве из шапки (P001..P007):
    //   старт: стек [P004, P002, P001]            (вершина справа)
    //   next -> P001, у него нет right             стек [P004, P002]
    //   next -> P002, кладём левую ветку P003      стек [P004, P003]
    //   next -> P003                               стек [P004]
    //   next -> P004, кладём левую ветку P006: P006, P005   стек [P006, P005]
    //   next -> P005 ... next -> P006, кладём P007 ... next -> P007, стек пуст.
    //   Итог: P001 P002 P003 P004 P005 P006 P007.
    // В стеке одновременно лежит максимум h узлов (один путь сверху вниз).
    private class InOrder implements Iterator<Uzel<K, V>> {
        private final AbstrLIFO<Uzel<K, V>> zasobnik = new AbstrLIFO<>();
        private final int zmenyPriVzniku = zmeny;   // "снимок" счётчика изменений

        InOrder() {
            vlozLevouVetev(root);
        }

        // положить в стек узел u и всех его левых потомков (идём влево до упора)
        private void vlozLevouVetev(Uzel<K, V> u) {
            while (u != null) {
                zasobnik.vloz(u);
                u = u.left;
            }
        }

        @Override
        public boolean hasNext() {
            return !zasobnik.jePrazdny();
        }

        @Override
        public Uzel<K, V> next() {
            kontrolaZmen(zmenyPriVzniku);
            if (zasobnik.jePrazdny()) {
                throw new NoSuchElementException("Průchod stromem skončil.");
            }
            Uzel<K, V> u = zasobnik.odeber();
            vlozLevouVetev(u.right);    // после узла идёт его правое поддерево
            return u;
        }
    }

    // ОБХОД В ШИРИНУ (level-order): по уровням сверху вниз, слева направо.
    // Через очередь: берём узел из начала очереди, его детей кладём в конец.
    // Так дети любого уровня выходят только после всех узлов предыдущего уровня.
    //
    // Пример на дереве из шапки:
    //   очередь [P004] -> выдаём P004, кладём P002, P006   очередь [P002, P006]
    //   выдаём P002, кладём P001, P003                     [P006, P001, P003]
    //   выдаём P006, кладём P005, P007                     [P001, P003, P005, P007]
    //   дальше листья по очереди.
    //   Итог: P004 P002 P006 P001 P003 P005 P007.
    private class DoSirky implements Iterator<Uzel<K, V>> {
        private final AbstrFIFO<Uzel<K, V>> fronta = new AbstrFIFO<>();
        private final int zmenyPriVzniku = zmeny;

        DoSirky() {
            if (root != null) {
                fronta.vloz(root);
            }
        }

        @Override
        public boolean hasNext() {
            return !fronta.jePrazdny();
        }

        @Override
        public Uzel<K, V> next() {
            kontrolaZmen(zmenyPriVzniku);
            if (fronta.jePrazdny()) {
                throw new NoSuchElementException("Průchod stromem skončil.");
            }
            Uzel<K, V> u = fronta.odeber();
            if (u.left != null) {
                fronta.vloz(u.left);
            }
            if (u.right != null) {
                fronta.vloz(u.right);
            }
            return u;
        }
    }

    // если дерево поменяли во время обхода — бросить исключение (см. поле zmeny)
    private void kontrolaZmen(int zmenyPriVzniku) {
        if (zmenyPriVzniku != zmeny) {
            throw new ConcurrentModificationException("Strom se během průchodu změnil.");
        }
    }

    // ================================================================ rozsah a vybudování (диапазон и построение)

    // ПОИСК В ДИАПАЗОНЕ <od; doKlic> (обе границы включительно), результат по возрастанию.
    // Сложность O(h + k), k = сколько нашли; в сбалансированном дереве O(log n + k).
    //
    // Идея: это тот же in-order обход через стек, но с ОТСЕЧЕНИЕМ лишнего:
    //   - если ключ узла < od, то и сам узел, и ВСЁ его левое поддерево меньше od —
    //     туда не идём вообще, сразу шаг вправо;
    //   - если очередной (по порядку) узел > doKlic, то все следующие ещё больше —
    //     обход можно прекратить.
    // Поэтому посещаются только узлы на "границе" диапазона (O(h)) плюс найденные (k),
    // а не всё дерево (O(n)).
    //
    // Пример: od = P003, do = P005 в дереве из шапки:
    //   P004 >= P003 -> в стек, влево; P002 < P003 -> не в стек, вправо; P003 -> в стек, влево: null
    //   снимаем P003 (<= P005) -> в результат; вправо: null
    //   снимаем P004 -> в результат; вправо: P006 >= P003 -> в стек, влево: P005 -> в стек
    //   снимаем P005 -> в результат; снимаем P006 > P005 -> стоп.
    //   P001 и P007 не посещались вообще. Итог: P003 P004 P005.
    @Override
    public IAbstrDoubleList<V> najdiVRozsahu(K od, K doKlic) {
        if (od == null || doKlic == null) {
            throw new AbstrTableException("Meze rozsahu nesmí být null.");
        }
        // результат собираем в наш же список из работы A (в порядке обхода = по возрастанию)
        IAbstrDoubleList<V> vysledek = new AbstrDoubleList<>();
        if (od.compareTo(doKlic) > 0) {
            return vysledek;                        // od > do -> пустой интервал
        }

        AbstrLIFO<Uzel<K, V>> zasobnik = new AbstrLIFO<>();
        Uzel<K, V> u = root;
        while (u != null || !zasobnik.jePrazdny()) {
            // спуск влево, но узлы с ключом < od пропускаем (их левое поддерево тоже лишнее)
            while (u != null) {
                if (u.key.compareTo(od) < 0) {
                    u = u.right;
                } else {
                    zasobnik.vloz(u);
                    u = u.left;
                }
            }
            if (zasobnik.jePrazdny()) {
                break;
            }
            Uzel<K, V> x = zasobnik.odeber();       // следующий по возрастанию ключ >= od
            if (x.key.compareTo(doKlic) > 0) {
                break;                              // вышли за верхнюю границу -> дальше только больше
            }
            vysledek.vlozPosledni(x.value);
            u = x.right;                            // продолжаем с правого поддерева
        }
        return vysledek;
    }

    // ПОСТРОИТЬ СБАЛАНСИРОВАННОЕ ДЕРЕВО ("vybuduj ... volbou středních ID"). O(n).
    //
    //  Шаг 1: in-order обходом выложить узлы в массив — он получается отсортированным.
    //  Шаг 2: собрать дерево заново: корень = СРЕДНИЙ элемент массива,
    //         левая половина -> левое поддерево, правая половина -> правое,
    //         и так же для каждой половины.
    //  Обычно это пишут рекурсией; здесь вместо неё очередь "заданий" (Usek):
    //  каждое задание = "из куска массива [od..doIndex] построй поддерево
    //  и прицепи к узлу rodic слева/справа".
    //
    //  Пример на 7 ключах [1 2 3 4 5 6 7] (индексы 0..6):
    //     [0..6] середина 3 -> ключ 4 = корень
    //     [0..2] середина 1 -> ключ 2 = левый сын 4;  [4..6] середина 5 -> ключ 6 = правый сын 4
    //     [0..0] -> 1, [2..2] -> 3, [4..4] -> 5, [6..6] -> 7
    //  Высота получается минимально возможной: ⌈log2(n+1)⌉ (для 60 продуктов = 6).
    //
    //  Узлы НЕ создаются заново — переиспользуются старые, меняются только ссылки.
    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void vybuduj() {
        if (pocet < 3) {
            return;                                 // 0, 1 или 2 узла — всегда сбалансировано
        }
        // Шаг 1. Java не умеет new Uzel<K,V>[n], поэтому создаём "сырой" массив и приводим тип
        Uzel<K, V>[] serazene = (Uzel<K, V>[]) new Uzel[pocet];
        Iterator<Uzel<K, V>> it = new InOrder();
        for (int i = 0; i < serazene.length; i++) {
            serazene[i] = it.next();
        }

        // Шаг 2. Сборка из очереди кусков массива
        root = null;
        AbstrFIFO<Usek<K, V>> fronta = new AbstrFIFO<>();
        fronta.vloz(new Usek<>(0, serazene.length - 1, null, false));   // весь массив, без родителя
        while (!fronta.jePrazdny()) {
            Usek<K, V> s = fronta.odeber();
            if (s.od > s.doIndex) {
                continue;                           // пустой кусок -> тут ребёнка нет
            }
            int stred = (s.od + s.doIndex) >>> 1;   // середина (>>> 1 = деление на 2 без переполнения)
            Uzel<K, V> u = serazene[stred];
            u.left = null;                          // старые ссылки сбрасываем,
            u.right = null;                         // детей назначат следующие задания
            u.parent = s.rodic;
            if (s.rodic == null) {
                root = u;
            } else if (s.vlevo) {
                s.rodic.left = u;
            } else {
                s.rodic.right = u;
            }
            // два новых задания: левая и правая половина куска
            fronta.vloz(new Usek<>(s.od, stred - 1, u, true));
            fronta.vloz(new Usek<>(stred + 1, s.doIndex, u, false));
        }
        zmeny++;
    }

    // "Задание" для vybuduj: построить поддерево из serazene[od..doIndex]
    // и прицепить его к rodic (vlevo = true -> левым сыном, иначе правым).
    // record = короткая запись класса только с полями и конструктором.
    private record Usek<K, V>(int od, int doIndex, Uzel<K, V> rodic, boolean vlevo) { }

    // ВЫСОТА ДЕРЕВА = число уровней (пустое = 0, только корень = 1). O(n).
    // Обход в ширину целыми уровнями: в начале каждого круга в очереди лежит
    // ровно один уровень (naUrovni узлов); вынимаем их все, считаем детей —
    // это размер следующего уровня. Сколько кругов — такая и высота.
    @Override
    public int getVyska() {
        if (root == null) {
            return 0;
        }
        AbstrFIFO<Uzel<K, V>> fronta = new AbstrFIFO<>();
        fronta.vloz(root);
        int naUrovni = 1;
        int vyska = 0;
        while (naUrovni > 0) {
            vyska++;
            int dalsiUroven = 0;
            for (int i = 0; i < naUrovni; i++) {
                Uzel<K, V> u = fronta.odeber();
                if (u.left != null) {
                    fronta.vloz(u.left);
                    dalsiUroven++;
                }
                if (u.right != null) {
                    fronta.vloz(u.right);
                    dalsiUroven++;
                }
            }
            naUrovni = dalsiUroven;
        }
        return vyska;
    }
}
