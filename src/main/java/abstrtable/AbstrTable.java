package abstrtable;

import abstrdoublelist.AbstrDoubleList;
import abstrdoublelist.IAbstrDoubleList;
import abstrlifofifo.AbstrFIFO;
import abstrlifofifo.AbstrLIFO;

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;

// Binární vyhledávací strom (BVS) v dynamické paměti.
// Všechny operace jsou BEZ REKURZE: hledání, vkládání a odebírání jdou cyklem po odkazech
// left / right / parent, průchody používají zásobník (AbstrLIFO) a frontu (AbstrFIFO).
public class AbstrTable<K extends Comparable<K>, V> implements IAbstrTable<K, V> {

    private static class Uzel<K, V> {
        K key;
        V value;
        Uzel<K, V> left;
        Uzel<K, V> right;
        Uzel<K, V> parent;

        Uzel(K key, V value, Uzel<K, V> parent) {
            this.key = key;
            this.value = value;
            this.parent = parent;
        }
    }

    private Uzel<K, V> root;
    private int pocet;
    private int zmeny;      // počítadlo změn - iterátor pozná, že se strom během průchodu změnil

    // ================================================================ základní operace

    @Override
    public void zrus() {
        root = null;        // zbytek stromu uklidí garbage collector
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

    @Override
    public V najdi(K key) {
        Uzel<K, V> u = najdiUzel(key);
        return u == null ? null : u.value;
    }

    // jde od kořene: menší klíč -> doleva, větší -> doprava, dokud nenajde nebo nespadne na null
    private Uzel<K, V> najdiUzel(K key) {
        if (key == null) {
            throw new AbstrTableException("Klíč nesmí být null.");
        }
        Uzel<K, V> u = root;
        while (u != null) {
            int c = key.compareTo(u.key);
            if (c == 0) {
                return u;
            }
            u = c < 0 ? u.left : u.right;
        }
        return null;
    }

    @Override
    public void vloz(K key, V value) {
        if (key == null) {
            throw new AbstrTableException("Klíč nesmí být null.");
        }
        if (value == null) {
            throw new AbstrTableException("Hodnota nesmí být null (null znamená \"nenalezeno\").");
        }
        if (root == null) {
            root = new Uzel<>(key, value, null);
        } else {
            Uzel<K, V> u = root;
            while (true) {
                int c = key.compareTo(u.key);
                if (c == 0) {
                    throw new AbstrTableException("Klíč " + key + " už v tabulce je, duplicita se odmítá.");
                }
                if (c < 0) {
                    if (u.left == null) {           // volné místo vlevo -> nový list
                        u.left = new Uzel<>(key, value, u);
                        break;
                    }
                    u = u.left;
                } else {
                    if (u.right == null) {          // volné místo vpravo -> nový list
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

    @Override
    public V odeber(K key) {
        Uzel<K, V> u = najdiUzel(key);
        if (u == null) {
            return null;
        }
        V hodnota = u.value;

        // dva synové: uzel nahradí nejpravější prvek levého podstromu (in-order předchůdce).
        // Předchůdce nemá pravého syna, takže pak stačí odebrat jeho (případ 0 nebo 1 syna).
        if (u.left != null && u.right != null) {
            Uzel<K, V> predchudce = u.left;
            while (predchudce.right != null) {
                predchudce = predchudce.right;
            }
            u.key = predchudce.key;
            u.value = predchudce.value;
            u = predchudce;
        }

        // teď má u nejvýš jednoho syna: ten (nebo null) zaujme jeho místo
        Uzel<K, V> syn = (u.left != null) ? u.left : u.right;
        nahradVRodici(u, syn);

        u.left = null;
        u.right = null;
        u.parent = null;
        pocet--;
        zmeny++;
        return hodnota;
    }

    // přepojí rodiče uzlu u na uzel novy (novy může být null)
    private void nahradVRodici(Uzel<K, V> u, Uzel<K, V> novy) {
        if (novy != null) {
            novy.parent = u.parent;
        }
        if (u.parent == null) {
            root = novy;                            // odebíral se kořen
        } else if (u.parent.left == u) {
            u.parent.left = novy;
        } else {
            u.parent.right = novy;
        }
    }

    // ================================================================ iterátory

    @Override
    public Iterator<V> vytvorIterator(eTypProhl typ) {
        Iterator<Uzel<K, V>> uzly = iteratorUzlu(typ);
        return new Iterator<V>() {
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

    // do hloubky in-order pomocí zásobníku: klíče vycházejí vzestupně
    private class InOrder implements Iterator<Uzel<K, V>> {
        private final AbstrLIFO<Uzel<K, V>> zasobnik = new AbstrLIFO<>();
        private final int zmenyPriVzniku = zmeny;

        InOrder() {
            vlozLevouVetev(root);
        }

        // uloží uzel a všechny jeho levé potomky - nejmenší klíč skončí na vrcholu zásobníku
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
            vlozLevouVetev(u.right);    // po uzlu přijde na řadu jeho pravý podstrom
            return u;
        }
    }

    // do šířky (level-order) pomocí fronty: po úrovních shora dolů, zleva doprava
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

    private void kontrolaZmen(int zmenyPriVzniku) {
        if (zmenyPriVzniku != zmeny) {
            throw new ConcurrentModificationException("Strom se během průchodu změnil.");
        }
    }

    // ================================================================ rozsah a vybudování

    @Override
    public IAbstrDoubleList<V> najdiVRozsahu(K od, K doKlic) {
        if (od == null || doKlic == null) {
            throw new AbstrTableException("Meze rozsahu nesmí být null.");
        }
        IAbstrDoubleList<V> vysledek = new AbstrDoubleList<>();
        if (od.compareTo(doKlic) > 0) {
            return vysledek;                        // prázdný interval
        }

        // in-order průchod zásobníkem, který vynechává podstromy mimo interval:
        //  - klíč < od: uzel i celý jeho levý podstrom jsou mimo, jde se rovnou doprava
        //  - klíč > doKlic u vybraného uzlu: všechny další klíče jsou ještě větší, konec
        // Navštíví se jen O(h) uzlů na hranici intervalu plus k nalezených.
        AbstrLIFO<Uzel<K, V>> zasobnik = new AbstrLIFO<>();
        Uzel<K, V> u = root;
        while (u != null || !zasobnik.jePrazdny()) {
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
            Uzel<K, V> x = zasobnik.odeber();
            if (x.key.compareTo(doKlic) > 0) {
                break;
            }
            vysledek.vlozPosledni(x.value);         // in-order -> vzestupně podle klíče
            u = x.right;
        }
        return vysledek;
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void vybuduj() {
        if (pocet < 3) {
            return;                                 // 0, 1 nebo 2 uzly jsou vždy vyvážené
        }
        // 1) in-order průchodem seřadit uzly podle klíče do pole
        Uzel<K, V>[] serazene = (Uzel<K, V>[]) new Uzel[pocet];
        Iterator<Uzel<K, V>> it = new InOrder();
        for (int i = 0; i < serazene.length; i++) {
            serazene[i] = it.next();
        }

        // 2) postavit strom znovu: kořenem úseku je jeho prostřední prvek, levá polovina úseku
        //    tvoří levý podstrom a pravá polovina pravý. Úseky se místo rekurze zpracují z fronty.
        root = null;
        AbstrFIFO<Usek<K, V>> fronta = new AbstrFIFO<>();
        fronta.vloz(new Usek<>(0, serazene.length - 1, null, false));
        while (!fronta.jePrazdny()) {
            Usek<K, V> s = fronta.odeber();
            if (s.od > s.doIndex) {
                continue;
            }
            int stred = (s.od + s.doIndex) >>> 1;
            Uzel<K, V> u = serazene[stred];
            u.left = null;
            u.right = null;
            u.parent = s.rodic;
            if (s.rodic == null) {
                root = u;
            } else if (s.vlevo) {
                s.rodic.left = u;
            } else {
                s.rodic.right = u;
            }
            fronta.vloz(new Usek<>(s.od, stred - 1, u, true));
            fronta.vloz(new Usek<>(stred + 1, s.doIndex, u, false));
        }
        zmeny++;
    }

    // úsek seřazeného pole, ze kterého se má postavit podstrom, a kam ho připojit
    private record Usek<K, V>(int od, int doIndex, Uzel<K, V> rodic, boolean vlevo) { }

    @Override
    public int getVyska() {
        if (root == null) {
            return 0;
        }
        // průchod do šířky po celých úrovních: na začátku kola je ve frontě právě jedna úroveň
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
