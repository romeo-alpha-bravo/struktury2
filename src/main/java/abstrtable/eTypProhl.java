package abstrtable;

// typ prohlídky (průchodu) stromu
public enum eTypProhl {
    SIRKA,    // do šířky (level-order): po úrovních shora dolů, zleva doprava - pomocí fronty
    HLOUBKA   // do hloubky (in-order): levý podstrom, uzel, pravý podstrom - pomocí zásobníku
}
