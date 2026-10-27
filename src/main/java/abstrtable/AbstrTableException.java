package abstrtable;

// Своё исключение структуры: по типу сразу видно, ГДЕ ошибка (список / стек-очередь / дерево).
// extends RuntimeException = непроверяемое: не нужно писать throws у каждого метода.
public class AbstrTableException extends RuntimeException {
    public AbstrTableException(String zprava) {
        super(zprava);
    }
}
