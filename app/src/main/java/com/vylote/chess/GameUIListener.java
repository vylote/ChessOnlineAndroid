package com.vylote.chess;

public interface GameUIListener {
    void onTimerUpdate(int seconds);
    void onTurnUpdate(String text, int color, boolean isMyTurn);
    void onGameStarted(); // Hàm mới để chuyển màn hình
}