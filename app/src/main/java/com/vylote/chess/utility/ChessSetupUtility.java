package com.vylote.chess.utility;

import java.util.concurrent.CopyOnWriteArrayList;
import com.vylote.chess.model.*;

public class ChessSetupUtility {
    public static void setupStandardGame(CopyOnWriteArrayList<Piece> pieces) {
        // Đen (Phía trên)
        pieces.add(new Rook(1, 0, 0)); pieces.add(new Knight(1, 0, 1));
        pieces.add(new Bishop(1, 0, 2)); pieces.add(new Queen(1, 0, 3));
        pieces.add(new King(1, 0, 4)); pieces.add(new Bishop(1, 0, 5));
        pieces.add(new Knight(1, 0, 6)); pieces.add(new Rook(1, 0, 7));
        for(int i=0; i<8; i++) pieces.add(new Pawn(1, 1, i));

        // Trắng (Phía dưới)
        pieces.add(new Rook(0, 7, 0)); pieces.add(new Knight(0, 7, 1));
        pieces.add(new Bishop(0, 7, 2)); pieces.add(new Queen(0, 7, 3));
        pieces.add(new King(0, 7, 4)); pieces.add(new Bishop(0, 7, 5));
        pieces.add(new Knight(0, 7, 6)); pieces.add(new Rook(0, 7, 7));
        for(int i=0; i<8; i++) pieces.add(new Pawn(0, 6, i));
    }
}
