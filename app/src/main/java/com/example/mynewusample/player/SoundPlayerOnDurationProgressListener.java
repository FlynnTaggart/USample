package com.example.mynewusample.player;

public interface SoundPlayerOnDurationProgressListener {
    void onDurationProgress(SoundPlayer player, Long duration, Long currentTimestamp);
}
