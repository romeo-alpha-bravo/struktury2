package abstrdoublelist;

// Своё исключение структуры: по типу сразу видно, ГДЕ ошибка (список / стек-очередь / дерево).
// extends RuntimeException = непроверяемое: не нужно писать throws у каждого метода.
public class AbstrDoubleListException extends RuntimeException {
    public AbstrDoubleListException(String message) {
        super(message);
    }
}
