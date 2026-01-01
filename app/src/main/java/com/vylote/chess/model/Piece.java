package com.vylote.chess.model;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import java.io.Serializable;
import com.vylote.chess.controller.GameController;

public abstract class Piece implements Serializable {
    private static final long serialVersionUID = 1L;

    public Type type;
    public transient Bitmap image; // Android sử dụng Bitmap thay vì BufferedImage
    public int x, y; // Tọa độ pixel trên màn hình
    public int row, col, preRow, preCol;
    public int color;
    public Piece hittingP;
    public boolean moved, twoStepped;

    public Piece(int color, int row, int col) {
        this.color = color;
        this.row = row;
        this.col = col;
        this.preRow = row;
        this.preCol = col;
    }

    public void drawAt(Canvas canvas, Paint paint, int displayCol, int displayRow, int squareSize) {
        if (image != null) {
            // Tính toán khung hình chữ nhật dựa trên tọa độ hiển thị được truyền vào
            Rect dest = new Rect(
                    displayCol * squareSize,
                    displayRow * squareSize,
                    (displayCol + 1) * squareSize,
                    (displayRow + 1) * squareSize
            );
            // Vẽ bitmap vào vị trí hiển thị tương ứng
            canvas.drawBitmap(image, null, dest, paint);
        }
    }

    public void draw(Canvas canvas, Paint paint, int squareSize) {
        if (image != null) {
            // Tính toán khung hình chữ nhật để vẽ quân cờ vào đúng ô
            Rect dest = new Rect(
                    col * squareSize,
                    row * squareSize,
                    (col + 1) * squareSize,
                    (row + 1) * squareSize
            );
            // Vẽ bitmap vào ô tương ứng
            canvas.drawBitmap(image, null, dest, paint);
        }
    }


    // Trong Piece.java
    public void updatePosition() {
        // Trên Android, việc tính x, y thực tế sẽ do Canvas đảm nhiệm dựa trên squareSize
        // Chúng ta giữ hàm này để đồng bộ logic với bản Desktop
    }
    public void finishMove() {
        if (type == Type.PAWN && Math.abs(preRow - row) == 2) {
            twoStepped = true;
        }
        preCol = col;
        preRow = row;
        moved = true;
    }

    public boolean canMove(int targetCol, int targetRow) {
        return false;
    }

    public boolean isWithinBoard(int targetCol, int targetRow) {
        return targetCol >= 0 && targetCol <= 7 && targetRow >= 0 && targetRow <= 7;
    }

    public Piece gettingHitP(int targetCol, int targetRow) {
        for (Piece piece : GameController.simPieces) {
            if (piece.col == targetCol && piece.row == targetRow && piece != this) {
                return piece;
            }
        }
        return null;
    }

    public boolean isValidSquare(int targetCol, int targetRow) {
        hittingP = gettingHitP(targetCol, targetRow);
        if (hittingP == null) {
            return true;
        } else {
            if (hittingP.color != this.color) {
                return true;
            } else {
                hittingP = null;
            }
        }
        return false;
    }

//    public int getIndex() {
//        for (int index = 0; index < GameController.simPieces.size(); ++index) {
//            if (GameController.simPieces.get(index) == this) {
//                return index;
//            }
//        }
//        return 0;
//    }

    public boolean isSameSquare(int targetCol, int targetRow) {
        return targetCol == preCol && targetRow == preRow;
    }

    public boolean pieceIsOnStraightline(int targetCol, int targetRow) {
        //move to the left
        for (int c = preCol-1; c > targetCol; c--) {
            for (Piece piece : GameController.simPieces) {
                if (piece.row == targetRow && piece.col == c) {
                    hittingP = piece;
                    return true;
                }
            }
        }
        //move to the right
        for (int c = preCol+1; c < targetCol; c++) {
            for (Piece piece : GameController.simPieces) {
                if (piece.row == targetRow && piece.col == c) {
                    hittingP = piece;
                    return true;
                }
            }
        }
        //move up
        for (int r = preRow-1; r > targetRow; r--) {
            for (Piece piece : GameController.simPieces) {
                if (piece.col == targetCol && piece.row == r) {
                    hittingP = piece;
                    return true;
                }
            }
        }
        //move down
        for (int r = preRow+1; r < targetRow; r++) {
            for (Piece piece : GameController.simPieces) {
                if (piece.col == targetCol && piece.row == r) {
                    hittingP = piece;
                    return true;
                }
            }
        }
        return false;
    }

    public boolean pieceIsOnDiagonalLine(int targetCol, int targetRow) {
        int diff;
        if (targetRow < preRow) {
            //up left
            for (int c = preCol-1; c > targetCol; c--) {
                diff = Math.abs(c-preCol);
                for (Piece piece : GameController.simPieces) {
                    if (piece.row == preRow-diff && piece.col == c) {
                        hittingP = piece;
                        return true;
                    }
                }
            }
            //up right
            for (int c = preCol+1; c < targetCol; c++) {
                diff = Math.abs(c-preCol);
                for (Piece piece : GameController.simPieces) {
                    if (piece.row == preRow-diff && piece.col == c) {
                        hittingP = piece;
                        return true;
                    }
                }
            }

        }
        if (targetRow > preRow) {
            //down left
            for (int c = preCol-1; c > targetCol; c--) {
                diff = Math.abs(c-preCol);
                for (Piece piece : GameController.simPieces) {
                    if (piece.row == preRow+diff && piece.col == c) {
                        hittingP = piece;
                        return true;
                    }
                }
            }
            //down right
            for (int c = preCol+1; c < targetCol; c++) {
                diff = Math.abs(c-preCol);
                for (Piece piece : GameController.simPieces) {
                    if (piece.row == preRow+diff && piece.col == c) {
                        hittingP = piece;
                        return true;
                    }
                }
            }
        }
        return false;
    }
}