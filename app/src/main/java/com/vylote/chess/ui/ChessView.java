package com.vylote.chess.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import com.vylote.chess.controller.GameController;
import com.vylote.chess.model.Piece;

public class ChessView extends View {
    private GameController controller;
    private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int squareSize;
    private int boardSize;

    public ChessView(Context context, AttributeSet attrs) { super(context, attrs); }
    public void setController(GameController c) { this.controller = c; }

    @Override
    protected void onMeasure(int w, int h) {
        int height = MeasureSpec.getSize(h);
        setMeasuredDimension(height, height);
        this.boardSize = height;
        this.squareSize = height / 8;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (controller == null) return;

        // 1. Vẽ bàn cờ
        controller.getBoard().draw(canvas, paint, boardSize, controller.isMultiplayer && controller.playerColor == GameController.BLACK);

        // 2. Vẽ hiệu ứng chọn quân (Chỉ vẽ khi KHÔNG thăng cấp)
        Piece activeP = controller.getActiveP();
        if (activeP != null && !controller.isPromotion()) {
            paint.setColor(Color.argb(120, 255, 165, 0));
            drawSquare(canvas, controller.getDisplayCol(activeP.col), controller.getDisplayRow(activeP.row));
            for (int[] move : controller.getValidMoves()) {
                paint.setColor(move[2] == 0 ? Color.argb(100, 0, 255, 0) : Color.argb(100, 255, 0, 0));
                drawSquare(canvas, controller.getDisplayCol(move[0]), controller.getDisplayRow(move[1]));
            }
        }

        // 3. Vẽ quân cờ
        for (Piece p : controller.getSimPieces()) {
            p.drawAt(canvas, paint, controller.getDisplayCol(p.col), controller.getDisplayRow(p.row), squareSize);
        }

        // 4. KIỂM TRA LỖI TỐI MÀN HÌNH TẠI ĐÂY
        // Thêm điều kiện: CHỈ vẽ lớp phủ nếu đang thăng cấp VÀ danh sách quân chọn PHẢI có dữ liệu
        if (controller.isPromotion() && controller.getPromoPieces() != null && !controller.getPromoPieces().isEmpty()) {

            // Vẽ lớp phủ tối (Màu đen 180 alpha)
            paint.setColor(Color.argb(180, 0, 0, 0));
            canvas.drawRect(0, 0, boardSize, boardSize, paint);

            // Vẽ các quân cờ để chọn
            for (Piece p : controller.getPromoPieces()) {
                paint.setColor(Color.argb(220, 255, 255, 255)); // Nền trắng cho ô chọn
                drawSquare(canvas, p.col, p.row);
                p.drawAt(canvas, paint, p.col, p.row, squareSize);
            }
        }
    }

    private void drawSquare(Canvas canvas, int col, int row) {
        canvas.drawRect(col * squareSize, row * squareSize, (col + 1) * squareSize, (row + 1) * squareSize, paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            int rawCol = (int) event.getX() / squareSize;
            int rawRow = (int) event.getY() / squareSize;

            int col = rawCol;
            int row = rawRow;

            // Đồng bộ tọa độ chạm cho quân Đen
            if (controller.isMultiplayer && controller.playerColor == GameController.BLACK) {
                col = 7 - rawCol;
                row = 7 - rawRow;
            }

            if (controller != null) controller.handleTouchInput(col, row);
            invalidate();
        }
        return true;
    }
}