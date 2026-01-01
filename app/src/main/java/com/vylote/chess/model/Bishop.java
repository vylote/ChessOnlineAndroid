package com.vylote.chess.model;

public class Bishop extends Piece {
    public Bishop(int color, int row, int col) {
        super(color, row, col);
        type = Type.BISHOP;
    }

    @Override
    public boolean canMove(int targetCol, int targetRow) {
        if (isWithinBoard(targetCol, targetRow) && !isSameSquare(targetCol, targetRow)) {
            // Kiểm tra đường chéo
            if (Math.abs(targetRow - row) == Math.abs(targetCol - col)) {
                return isValidSquare(targetCol, targetRow) && !pieceIsOnDiagonalLine(targetCol, targetRow);
            }
        }
        return false;
    }
}