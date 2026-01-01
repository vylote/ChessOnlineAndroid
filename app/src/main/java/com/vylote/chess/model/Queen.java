package com.vylote.chess.model;

public class Queen extends Piece {
    public Queen(int color, int row, int col) {
        super(color, row, col); // Đã sửa: Truyền đủ 3 tham số cho lớp cha Piece
        type = Type.QUEEN;
    }

    @Override
    public boolean canMove(int targetCol, int targetRow) {
        if (isWithinBoard(targetCol, targetRow) && !isSameSquare(targetCol, targetRow)) {
            if (Math.abs(targetRow - preRow) == Math.abs(targetCol - preCol)) {
                if (isValidSquare(targetCol, targetRow) && !pieceIsOnDiagonalLine(targetCol, targetRow)) {
                    return true;
                }
            }
            if (targetCol == preCol || targetRow == preRow) {
                return isValidSquare(targetCol, targetRow) && !pieceIsOnStraightline(targetCol, targetRow);
            }
        }
        return false;
    }
}