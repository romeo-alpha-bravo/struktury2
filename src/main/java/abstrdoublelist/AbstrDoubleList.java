package abstrdoublelist;
import java.util.Iterator;


public class AbstrDoubleList<T>  implements IAbstrDoubleList <T> {

    private static class Node <E>{
        E data;
        Node<E> previous;
        Node<E> next;


        public Node(E data) {
            this.data = data;
        }
    }

    private Node<T> first;
    private Node<T> current;
    private Node<T> last;




    @Override
    public void zrus() {

        first = null;
        current = null;
        last = null;

    }

    @Override
    public boolean jePrazdny() {


        return first == null;
    }

    @Override
    public void vlozPrvni(T data) {
        Node<T> new_node = new Node<>(data);


       if (first == null){
            first = new_node;
            current = new_node;
            last = new_node;
       }
       else {
           first.previous = new_node;
           new_node.next = first;

           first = new_node;
       }

    }

    @Override
    public void vlozPosledni(T data) {
        Node<T> new_node = new Node<>(data);



        if(last == null){
            first = new_node;
            last = new_node;
            current = new_node;
        }
        else{
            last.next = new_node;
            new_node.previous = last;

            last = new_node;
        }
    }
//vložení prvku do seznamu jakožto následníka
// aktuálního prvku

    @Override
    public void vlozNaslednika(T data) {
        // if it is a successor - it has to go after his parent

        if(current == null){
            throw new AbstrDoubleListException("List is empty,there is no way you can insert a successor!");
        }

        Node<T> successor = new Node<>(data);
        Node<T> after = current.next;

        successor.previous = current;
        successor.next = after;

        current.next = successor;

        // if after is null -> successor becomes  last element; A X
        if (after == null){
            last = successor;
        }// if after is not null -> successor becomes prev elem of after; A X C
        else{
            after.previous = successor;
        }



    }
    //vložení prvku do seznamu jakožto předchůdce
    //aktuálního prvku
    @Override
    public void vlozPredchudce(T data) {
        // if it is an ancestor - it has to go before the parent

        if (current == null){
            throw  new AbstrDoubleListException("List is empty,there is no way you can insert an ancestor!");
        }

        Node<T> ancestor = new Node<>(data);
        Node<T> before = current.previous;

        ancestor.next = current;
        ancestor.previous = before;

        current.previous = ancestor;

        if(before == null){ // if the before is null -> an ancestor becomes the first elem
            first = ancestor;
        }
        else{ // if before is not null -> an ancestor becomes the next elem of before

           before.next = ancestor;

        }


    }
//
//zpřístupnění aktuálního prvku seznamu;Pozn.
// Operace typu zpřístupni, přenastavují pozici aktuálního prvku
    @Override
    public T zpristupniAktualni() {
        if(current == null){
            throw  new AbstrDoubleListException("List is empty,there is now way you can access current element!");
        }

        return current.data;
    }

    // zpřístupnění prvního prvku seznamu -> first becomes the current element
    @Override
    public T zpristupniPrvni() {
        if (first == null){
            throw new AbstrDoubleListException("List is empty, there is no first element!");
        }

        current = first;
        return current.data;
    }

    // zpřístupnění posledního prvku seznamu -> last becomes the current element
    @Override
    public T zpristupniPosledni() {
        if (last == null){
            throw new AbstrDoubleListException("List is empty, there is no last element!");
        }

        current = last;
        return current.data;
    }

    // zpřístupnění následníka aktuálního prvku -> current moves one step forward
    @Override
    public T zpristupniNaslednika() {
        if (current == null){
            throw new AbstrDoubleListException("List is empty, there is no current element!");
        }

        Node<T> after = current.next;

        // current is the last element -> nothing after it, current stays where it was
        if (after == null){
            throw new AbstrDoubleListException("Current element has no successor!");
        }

        current = after;
        return current.data;
    }

    // zpřístupnění předchůdce aktuálního prvku -> current moves one step back
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

    private void unlink(Node<T> node){
        Node<T> before = node.previous;
        Node<T> after = node.next;

        if(before == null){
            first = after;
        }else{
            before.next = after;
        }

        if (after == null){
            last = before;
        }else{
            after.previous = before;
        }

        node.previous = null;
        node.next = null;

    }

    // odebrání (vyjmutí) aktuálního prvku ze seznamu poté je
    //aktuální prvek nastaven na první prvek
    @Override
    public T odeberAktualni() {
        if(current == null){
            throw new AbstrDoubleListException("List is empty!");
        }
        Node<T> temp = current;
        unlink(temp);
        current = first;
        return temp.data;

    }

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

    @Override
    public T odeberNaslednika() {
        if (current == null){
            throw new AbstrDoubleListException("List is empty!");
        }

        Node<T> after = current.next;
        if(after == null){
            throw new AbstrDoubleListException("Current element has no successor!");
        }
        unlink(after);

        return after.data;
    }

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

    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {

            private Node<T> cursor = first;

            @Override
            public boolean hasNext() {
                return cursor != null;
            }

            @Override
            public T next() {

                if (cursor == null){
                    throw  new AbstrDoubleListException("No more elements!");
                }
               T data = cursor.data;
                cursor = cursor.next;

                return data; 
            }
        };
    }
}
