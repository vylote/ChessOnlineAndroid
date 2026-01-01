package com.vylote.chess.model;

import com.vylote.chess.controller.GameController;

public class Rook extends Piece {
    public Rook(int color, int row, int col) {
        super(color, row, col);
        type = Type.ROOK;
    }

    @Override
    public boolean canMove(int targetCol, int targetRow) {
        if (isWithinBoard(targetCol, targetRow) && !isSameSquare(targetCol, targetRow)) {
            if (targetCol == col || targetRow == row) { // Dùng col/row hiện tại
                return isValidSquare(targetCol, targetRow) && !pieceIsOnStraightline(targetCol, targetRow);
            }
        }
        return false;
    }
}