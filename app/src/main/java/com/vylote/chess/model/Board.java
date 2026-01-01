package com.vylote.chess.model;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

public class Board {
    public static final int MAX_COL = 8;
    public static final int MAX_ROW = 8;

    // Chúng ta không dùng hằng số SQUARE_SIZE ở đây nữa
    private Bitmap boardImage;
    private Bitmap boardFlippedImage;

    public Board(Bitmap normal, Bitmap flipped) {
        this.boardImage = normal;
        this.boardFlippedImage = flipped;
    }

    // Canvas sẽ vẽ ảnh bàn cờ khít với chiều cao màn hình
    public void draw(Canvas canvas, Paint paint, int boardWidth, boolean flipped) {
        Bitmap imgToDraw = flipped ? boardFlippedImage : boardImage;
        if (imgToDraw == null) imgToDraw = boardImage;

        if (imgToDraw != null) {
            // Vẽ ảnh bàn cờ thành hình vuông (boardWidth x boardWidth)
            Rect dest = new Rect(0, 0, boardWidth, boardWidth);
            canvas.drawBitmap(imgToDraw, null, dest, paint);
        }
    }
}