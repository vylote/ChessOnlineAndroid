package com.vylote.chess.model;

import com.vylote.chess.controller.GameController;

public class Pawn extends Piece {
    public Pawn(int color, int row, int col) {
        super(color, row, col);
        type = Type.PAWN;
        // Image sẽ được nạp thông qua ImageLoader trong Controller
    }

    @Override
    public boolean canMove(int targetCol, int targetRow) {
        if (isWithinBoard(targetCol, targetRow) && !isSameSquare(targetCol, targetRow)) {
            int turn = (color == GameController.WHITE) ? -1 : 1;
            hittingP = gettingHitP(targetCol, targetRow);

            // Logic di chuyển giữ nguyên 100% từ bản Desktop
            if (targetCol == col && targetRow == row + turn && hittingP == null) return true;
            if (targetCol == col && targetRow == row + turn * 2 && hittingP == null && !moved) {
                if (gettingHitP(targetCol, row + turn) == null) return true;
            }
            if (Math.abs(targetCol - col) == 1 && targetRow == row + turn && hittingP != null && hittingP.color != this.color) return true;

            // En Passant
            if (targetRow == row + turn && Math.abs(targetCol - col) == 1) {
                for (Piece piece : GameController.simPieces) {
                    if (piece.col == targetCol && piece.row == row && piece.twoStepped) {
                        hittingP = piece;
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
