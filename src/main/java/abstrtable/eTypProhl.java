package abstrtable;

// typ prohlídky (průchodu) stromu
// Тип обхода дерева, который передаётся в vytvorIterator(...).
// SIRKA   = в ширину: P004 P002 P006 P001 P003 P005 P007 (по уровням)
// HLOUBKA = в глубину in-order: P001 P002 ... P007 (по возрастанию ключа)
public enum eTypProhl {
    SIRKA,    // do šířky (level-order): po úrovních shora dolů, zleva doprava - pomocí fronty
    HLOUBKA   // do hloubky (in-order): levý podstrom, uzel, pravý podstrom - pomocí zásobníku
}
