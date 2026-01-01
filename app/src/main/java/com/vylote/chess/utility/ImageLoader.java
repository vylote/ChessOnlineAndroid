package com.vylote.chess.utility;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import java.util.HashMap;

public class ImageLoader {
    // Bộ nhớ đệm (Cache) để tránh nạp ảnh nhiều lần gây tốn RAM
    private static HashMap<String, Bitmap> pieceImages = new HashMap<>();

    /**
     * Nạp toàn bộ quân cờ vào bộ nhớ đệm.
     * Lưu ý: Tên file trong drawable phải là chữ thường (ví dụ: w_king.png)
     */
    public static void loadSprites(Context context) {
        String[] names = {
                "w_king", "w_queen", "w_rook", "w_bishop", "w_knight", "w_pawn",
                "b_king", "b_queen", "b_rook", "b_bishop", "b_knight", "b_pawn"
        };

        for (String name : names) {
            Bitmap bitmap = getBitmap(context, name);
            if (bitmap != null) {
                pieceImages.put(name, bitmap);
            }
        }
    }

    /**
     * Lấy một Bitmap từ thư mục drawable dựa trên tên file
     */
    public static Bitmap getBitmap(Context context, String name) {
        int resId = context.getResources().getIdentifier(name, "drawable", context.getPackageName());
        if (resId != 0) {
            return BitmapFactory.decodeResource(context.getResources(), resId);
        } else {
            Log.e("ImageLoader", "Không tìm thấy ảnh: " + name);
            return null;
        }
    }

    /**
     * Truy xuất ảnh quân cờ từ Cache
     */
    public static Bitmap getPieceImage(String key) {
        return pieceImages.get(key);
    }
}