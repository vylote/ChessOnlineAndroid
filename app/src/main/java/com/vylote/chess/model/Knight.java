package com.vylote.chess.model;

public class Knight extends Piece {
    public Knight(int color, int row, int col) {
        super(color, row, col);
        type = Type.KNIGHT;
    }

    @Override
    public boolean canMove(int targetCol, int targetRow) {
        if (isWithinBoard(targetCol, targetRow) && !isSameSquare(targetCol, targetRow)) {
            // Logic toán học (1x2 hoặc 2x1) cực kỳ tối ưu
            if (Math.abs(targetRow - row) * Math.abs(targetCol - col) == 2) {
                return isValidSquare(targetCol, targetRow);
            }
        }
        return false;
    }
}