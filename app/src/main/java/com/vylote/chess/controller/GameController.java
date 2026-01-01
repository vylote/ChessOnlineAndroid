package com.vylote.chess.controller;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import com.vylote.chess.MainActivity;
import com.vylote.chess.GameUIListener;
import com.vylote.chess.model.*;
import com.vylote.chess.network.NetworkManager;
import com.vylote.chess.utility.*;
import com.vylote.chess.ui.ChessView;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;
import model.GameConfigPacket;
import model.MovePacket;
import model.PlayerProfile;

public class GameController implements Runnable {
    // --- 1. FIELDS ---
    private final Context context;
    private ChessView chessView;
    private GameUIListener uiListener;
    private Thread gameThread;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());

    public static final int WHITE = 0, BLACK = 1;
    private int currentColor = WHITE;
    private final Board board;
    public static CopyOnWriteArrayList<Piece> simPieces = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Piece> pieces = new CopyOnWriteArrayList<>();
    private Piece activeP;
    public static Piece castlingP;
    private final ArrayList<int[]> validMoves = new ArrayList<>();
    private final ArrayList<Piece> promoPieces = new ArrayList<>();

    private boolean promotion = false, gameOver = false;
    public boolean isTimeRunning = false;
    private int timeLeft = 20;
    private long lastSecond = System.currentTimeMillis();

    public boolean isMultiplayer = false, isServer = false;
    public int playerColor = WHITE;
    public NetworkManager netManager;
    private String myName = "Android Player";
    private PlayerProfile opponentProfile;

    // =========================================================
    // NHÓM MỚI: CÁC PHƯƠNG THỨC GETTER
    // =========================================================
    public Board getBoard() { return board; }
    public Piece getActiveP() { return activeP; }
    public boolean isPromotion() { return promotion; }
    public ArrayList<int[]> getValidMoves() { return validMoves; }
    public CopyOnWriteArrayList<Piece> getSimPieces() { return simPieces; }
    public ArrayList<Piece> getPromoPieces() { return promoPieces; }
    public Context getContext() { return context; }
    public boolean isGameOver() { return gameOver; }
    public int getCurrentColor() { return currentColor; }

    public GameController(Context context, ChessView chessView, GameUIListener uiListener) {
        this.context = context; this.chessView = chessView; this.uiListener = uiListener;
        this.board = new Board(ImageLoader.getBitmap(context, "board"), ImageLoader.getBitmap(context, "board_flipped"));
        ImageLoader.loadSprites(context);
        setPieces(); copyPieces(pieces, simPieces);
    }

    public void startNewGame() {
        pieces.clear(); simPieces.clear(); setPieces(); copyPieces(pieces, simPieces);
        currentColor = WHITE; gameOver = false; promotion = false;
        activeP = null; validMoves.clear(); promoPieces.clear();
        resetTime(); isTimeRunning = true; updateTurnUI();
        if (gameThread == null || !gameThread.isAlive()) { gameThread = new Thread(this); gameThread.start(); }
    }

    @Override
    public void run() {
        double interval = 1000000000.0 / 60;
        double delta = 0; long lastTime = System.nanoTime();
        while (gameThread != null) {
            long now = System.nanoTime(); delta += (now - lastTime) / interval; lastTime = now;
            if (delta >= 1) { update(); if (chessView != null) chessView.postInvalidate(); delta--; }
        }
    }

    private void update() {
        if (gameOver) return;
        long now = System.currentTimeMillis();
        if (isTimeRunning && now - lastSecond >= 1000) {
            lastSecond = now; timeLeft--; if (uiListener != null) uiListener.onTimerUpdate(timeLeft);
            if (timeLeft <= 0) handleTimeOut();
        }
    }

    private void handleTimeOut() {
        uiHandler.post(() -> {
            if (promotion && !promoPieces.isEmpty()) replacePawnAndFinish(promoPieces.get(0));
            else finalizeTurn();
        });
    }

    public void onOpponentConnected() {
        if (isServer) netManager.sendConfig(new GameConfigPacket(this.playerColor, this.myName));
    }

    public void onConfigReceived(GameConfigPacket p) {
        opponentProfile = new PlayerProfile(p.playerName, p.hostColor, "");
        if (!isServer) {
            this.playerColor = (p.hostColor == WHITE) ? BLACK : WHITE;
            netManager.sendConfig(new GameConfigPacket(this.playerColor, this.myName));
        }

        uiHandler.post(() -> {
            if (uiListener != null) {
                uiListener.onGameStarted();
                uiListener.onTurnUpdate("ĐÃ KẾT NỐI. CHỜ HOST...", Color.YELLOW, false);
            }
        });
    }

    // SỬA LỖI ĐỒNG BỘ TẠI ĐÂY
    public void receiveNetworkMove(MovePacket packet) {
        // -2 là lệnh từ PC Host, -3 là lệnh từ Android Host
        if (packet.oldCol == -2 || packet.oldCol == -3) {
            uiHandler.post(() -> {
                if (context instanceof MainActivity) {
                    ((MainActivity) context).startCountdownSync();
                }
            });
            return;
        }

        for (Piece p : simPieces) {
            if (p.col == packet.oldCol && p.row == packet.oldRow) {
                activeP = p; activeP.canMove(packet.newCol, packet.newRow);
                simulateClickToMove(packet.newCol, packet.newRow);
                if (packet.promotionType != -1) replacePawnAndFinishNetwork(packet.promotionType);
                else activeP.finishMove();
                finalizeTurn(); break;
            }
        }
    }

    public void handleTouchInput(int col, int row) {
        if (gameOver) return;
        if (promotion && !promoPieces.isEmpty()) {
            for (Piece p : promoPieces) if (p.col == col && p.row == row) { replacePawnAndFinish(p); return; }
            return;
        }
        if (isMultiplayer && currentColor != playerColor) return;

        if (activeP == null) {
            for (Piece p : simPieces) if (p.color == currentColor && p.col == col && p.row == row) {
                activeP = p; calculateValidMoves(activeP); break;
            }
        } else {
            boolean valid = false;
            for (int[] mv : validMoves) if (mv[0] == col && mv[1] == row) { valid = true; break; }
            if (valid) {
                int oc = activeP.col, or = activeP.row;
                simulateClickToMove(col, row);
                if (canPromote()) { setPromoPieces(); promotion = true; }
                else finalizeMoveNetwork(oc, or, col, row, -1);
            } else { activeP = null; validMoves.clear(); }
        }
    }

    public boolean canPromote() {
        if (activeP == null || activeP.type != Type.PAWN) return false;
        return (activeP.color == WHITE && activeP.row == 0) || (activeP.color == BLACK && activeP.row == 7);
    }

    public void setPromoPieces() {
        promoPieces.clear(); int pColor = activeP.color; int col = activeP.col;
        promoPieces.add(new Queen(pColor, 0, 0)); promoPieces.add(new Rook(pColor, 0, 0));
        promoPieces.add(new Bishop(pColor, 0, 0)); promoPieces.add(new Knight(pColor, 0, 0));
        for (int i = 0; i < promoPieces.size(); i++) {
            Piece p = promoPieces.get(i); p.image = ImageLoader.getPieceImage(getPieceKey(p));
            p.col = col; p.row = (pColor == WHITE) ? i : 7 - i;
        }
    }

    public void replacePawnAndFinish(Piece p) {
        int type = (p instanceof Rook) ? 1 : (p instanceof Knight) ? 2 : (p instanceof Bishop) ? 3 : 0;
        replacePawnAndFinishNetwork(type);
        if (isMultiplayer) netManager.sendMove(new MovePacket(activeP.preCol, activeP.preRow, activeP.col, activeP.row, type));
        promotion = false; finalizeTurn();
    }

    public void replacePawnAndFinishNetwork(int type) {
        Piece newP; int color = activeP.color, r = activeP.row, c = activeP.col;
        switch (type) {
            case 1: newP = new Rook(color, r, c); break; case 2: newP = new Knight(color, r, c); break;
            case 3: newP = new Bishop(color, r, c); break; default: newP = new Queen(color, r, c); break;
        }
        newP.image = ImageLoader.getPieceImage(getPieceKey(newP));
        simPieces.add(newP); simPieces.remove(activeP);
    }

    private void finalizeMoveNetwork(int oc, int or, int nc, int nr, int type) {
        activeP.finishMove(); copyPieces(simPieces, pieces);
        if (isMultiplayer) netManager.sendMove(new MovePacket(oc, or, nc, nr, type));
        finalizeTurn();
    }

    public void finalizeTurn() {
        copyPieces(simPieces, pieces); for (Piece p : pieces) p.updatePosition();
        activeP = null; validMoves.clear(); castlingP = null; promotion = false;
        currentColor = (currentColor == WHITE) ? BLACK : WHITE; resetTime(); updateTurnUI();
    }

    public void setupMultiplayer(boolean h, int c, String i) {
        this.isMultiplayer = true; this.isServer = h; this.playerColor = c;
        this.netManager = new NetworkManager(this);
        if (h) netManager.hostGame(5555); else netManager.joinGame(i, 5555);
    }

    public void simulateClickToMove(int tc, int tr) {
        copyPieces(pieces, simPieces);
        if (activeP.gettingHitP(tc, tr) != null) simPieces.remove(activeP.gettingHitP(tc, tr));
        activeP.col = tc; activeP.row = tr;
    }

    private void setPieces() { pieces.clear(); ChessSetupUtility.setupStandardGame(pieces); for (Piece p : pieces) p.image = ImageLoader.getPieceImage(getPieceKey(p)); }
    private String getPieceKey(Piece p) { return (p.color == WHITE ? "w_" : "b_") + p.type.toString().toLowerCase(); }
    public void copyPieces(CopyOnWriteArrayList<Piece> s, CopyOnWriteArrayList<Piece> t) { t.clear(); t.addAll(s); }
    public void resetTime() { timeLeft = 20; lastSecond = System.currentTimeMillis(); if (uiListener != null) uiListener.onTimerUpdate(timeLeft); }
    public void setMyProfile(String n, int c) { this.myName = n; this.playerColor = c; }
    public String getMyName() { return myName; }
    public int getDisplayCol(int c) { return (isMultiplayer && playerColor == BLACK) ? 7 - c : c; }
    public int getDisplayRow(int r) { return (isMultiplayer && playerColor == BLACK) ? 7 - r : r; }
    public PlayerProfile getOpponentProfile() { return opponentProfile; }
    private void updateTurnUI() { if (uiListener == null) return; boolean myTurn = isMultiplayer ? (currentColor == playerColor) : (currentColor == WHITE); uiListener.onTurnUpdate(myTurn ? "LƯỢT BẠN" : "ĐỐI THỦ ĐANG ĐI...", Color.WHITE, myTurn); }
    public void setChessView(ChessView v) { this.chessView = v; }
    public void exitToMenu() { isTimeRunning = false; if (netManager != null) { netManager.closeConnection(); netManager = null; } isMultiplayer = false; isServer = false; opponentProfile = null; uiHandler.post(() -> { if (context instanceof MainActivity) ((MainActivity) context).showMenu(); }); }
    private void calculateValidMoves(Piece p) { validMoves.clear(); for (int r=0; r<8; r++) for (int c=0; c<8; c++) if (p.canMove(c, r)) validMoves.add(new int[]{c, r, (p.gettingHitP(c, r) != null ? 1 : 0)}); }
}