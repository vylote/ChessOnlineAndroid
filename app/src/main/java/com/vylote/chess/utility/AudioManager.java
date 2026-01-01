package com.vylote.chess.utility;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.SoundPool;
import java.util.HashMap;

public class AudioManager {
    private Context context;

    // Quản lý BGM (Nhạc nền)
    private MediaPlayer bgmPlayer;
    private int currentBgmResId = -1;
    private float bgmVolume = 0.5f; // Tỉ lệ 0.0 đến 1.0

    // Quản lý SFX (Hiệu ứng)
    private SoundPool soundPool;
    private HashMap<Integer, Integer> soundMap; // Map giữa Resource ID và Sound ID
    private float sfxVolume = 1.0f;

    public AudioManager(Context context) {
        this.context = context;
        this.soundMap = new HashMap<>();

        // Khởi tạo SoundPool cho SFX
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        soundPool = new SoundPool.Builder()
                .setMaxStreams(5) // Cho phép phát tối đa 5 âm thanh cùng lúc
                .setAudioAttributes(audioAttributes)
                .build();
    }

    // --- QUẢN LÝ BGM ---
    public void setBGMVolumeFromSlider(int value) {
        this.bgmVolume = value / 100.0f;
        if (bgmPlayer != null) {
            bgmPlayer.setVolume(bgmVolume, bgmVolume);
        }
    }

    public void playBGM(int resId) {
        if (currentBgmResId == resId && bgmPlayer != null && bgmPlayer.isPlaying()) return;

        stopBGM();
        currentBgmResId = resId;
        bgmPlayer = MediaPlayer.create(context, resId);
        bgmPlayer.setLooping(true); // Lặp lại liên tục
        bgmPlayer.setVolume(bgmVolume, bgmVolume);
        bgmPlayer.start();
    }

    public void stopBGM() {
        if (bgmPlayer != null) {
            bgmPlayer.stop();
            bgmPlayer.release();
            bgmPlayer = null;
        }
        currentBgmResId = -1;
    }

    // --- QUẢN LÝ SFX ---
    public void setSFXVolumeFromSlider(int value) {
        this.sfxVolume = value / 100.0f;
    }

    /**
     * Nạp trước âm thanh vào bộ nhớ để phát ngay lập tức không trễ
     */
    public void loadSFX(int resId) {
        if (!soundMap.containsKey(resId)) {
            int soundId = soundPool.load(context, resId, 1);
            soundMap.put(resId, soundId);
        }
    }

    public void playSFX(int resId) {
        Integer soundId = soundMap.get(resId);
        if (soundId != null) {
            // Phát âm thanh ngay lập tức
            soundPool.play(soundId, sfxVolume, sfxVolume, 1, 0, 1.0f);
        } else {
            // Nếu chưa nạp thì nạp và phát (có thể trễ lần đầu)
            int newSoundId = soundPool.load(context, resId, 1);
            soundMap.put(resId, newSoundId);
            soundPool.setOnLoadCompleteListener((pool, id, status) -> {
                if (status == 0) pool.play(id, sfxVolume, sfxVolume, 1, 0, 1.0f);
            });
        }
    }

    public void release() {
        stopBGM();
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
    }
}
