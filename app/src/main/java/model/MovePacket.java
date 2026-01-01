package model;
import java.io.Serializable;

public class MovePacket implements Serializable {
    private static final long serialVersionUID = 1L;
    public int oldCol, oldRow, newCol, newRow, promotionType;

    public MovePacket(int oC, int oR, int nC, int nR, int pType) {
        this.oldCol = oC; this.oldRow = oR;
        this.newCol = nC; this.newRow = nR;
        this.promotionType = pType;
    }
}