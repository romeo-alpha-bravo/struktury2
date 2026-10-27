package sklady;


// Позиция в списке склада, с которой работают vloz / zpristupni / odeber.
// PREDCHUDCE и NASLEDNIK всегда считаются относительно текущего (aktuální) элемента.
// AKTUALNI нельзя использовать для вставки (вставляют "перед" или "после" него).
public enum enumPozice {
    PRVNI, POSLEDNI, PREDCHUDCE, NASLEDNIK, AKTUALNI
}
