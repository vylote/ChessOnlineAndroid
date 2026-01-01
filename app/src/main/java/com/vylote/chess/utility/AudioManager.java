package com.vylote.chess.utility;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.SoundPool;
import java.util.HashMap;

public class AudioManager {
    private Context context;
    private MediaPlayer bgmPlayer;
    private int currentBgmResId = -1;
    private float bgmVolume = 1.0f; // Mặc định 100%
    private SoundPool soundPool;
    private HashMap<Integer, Integer> soundMap;
    private float sfxVolume = 1.0f; // Mặc định 100%

    public AudioManager(Context context) {
        this.context = context;
        this.soundMap = new HashMap<>();
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        soundPool = new SoundPool.Builder()
                .setMaxStreams(10)
                .setAudioAttributes(audioAttributes)
                .build();
    }

    public float getBgmVolume() { return bgmVolume; }
    public float getSfxVolume() { return sfxVolume; }

    public void setBGMVolumeFromSlider(int value) {
        this.bgmVolume = value / 100.0f;
        if (bgmPlayer != null) bgmPlayer.setVolume(bgmVolume, bgmVolume);
    }

    public void setSFXVolumeFromSlider(int value) {
        this.sfxVolume = value / 100.0f;
    }

    public void playBGM(int resId) {
        if (currentBgmResId == resId && bgmPlayer != null && bgmPlayer.isPlaying()) return;
        stopBGM();
        currentBgmResId = resId;
        bgmPlayer = MediaPlayer.create(context, resId);
        if (bgmPlayer != null) {
            bgmPlayer.setLooping(true);
            bgmPlayer.setVolume(bgmVolume, bgmVolume);
            bgmPlayer.start();
        }
    }

    public void stopBGM() {
        if (bgmPlayer != null) {
            bgmPlayer.stop();
            bgmPlayer.release();
            bgmPlayer = null;
        }
        currentBgmResId = -1;
    }

    public void loadSFX(int resId) {
        if (!soundMap.containsKey(resId)) {
            soundMap.put(resId, soundPool.load(context, resId, 1));
        }
    }

    public void playSFX(int resId) {
        Integer soundId = soundMap.get(resId);
        if (soundId != null) {
            soundPool.play(soundId, sfxVolume, sfxVolume, 1, 0, 1.0f);
        } else {
            loadSFX(resId); // Nạp nếu chưa có
        }
    }

    // Thêm vào cuối class AudioManager.java
    public void release() {
        // 1. Dừng và giải phóng Nhạc nền
        stopBGM();

        // 2. Giải phóng SoundPool (Hiệu ứng âm thanh)
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }

        // 3. Xóa Map để dọn dẹp bộ nhớ
        if (soundMap != null) {
            soundMap.clear();
        }
    }
}