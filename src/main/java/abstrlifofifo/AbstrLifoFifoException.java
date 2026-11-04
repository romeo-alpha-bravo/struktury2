package abstrlifofifo;

// Своё исключение структуры: по типу сразу видно, ГДЕ ошибка (список / стек-очередь / дерево).
// extends RuntimeException = непроверяемое: не нужно писать throws у каждого метода.
public class AbstrLifoFifoException extends RuntimeException {
    public AbstrLifoFifoException(String zprava) {
        super(zprava);
    }
}
