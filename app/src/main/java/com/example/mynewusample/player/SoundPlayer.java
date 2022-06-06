package com.example.mynewusample.player;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.example.mynewusample.R;
import com.masoudss.lib.WaveformSeekBar;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

public class SoundPlayer {

    private static final String ACTION_PLAY = "PLAY";

    private final MediaPlayer mediaPlayer = new MediaPlayer();
    private Handler handler = new Handler();
    private Runnable runnable;
    private AtomicLong durationCounter = new AtomicLong();

    private final long INTERVAL = 32;

    private boolean isPrepared = false;

    private SoundPlayerOnPreparedListener onPreparedListener;
    private SoundPlayerOnPauseListener onPauseListener;
    private SoundPlayerOnPlayListener onPlayListener;
    private SoundPlayerOnDurationProgressListener onDurationProgressListener;
    private SoundPlayerOnCompleteListener onCompleteListener;

    private SoundPlayerOnSeekToListener onSeekToListener;


    public void preparePlayer() {
        mediaPlayer.prepareAsync();
        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                isPrepared = true;
                onPreparedListener.onPrepared(SoundPlayer.this);
            }
        });

        runnable = new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer.isPlaying()) {
                    onDurationProgressListener.onDurationProgress(SoundPlayer.this, getDuration(), (long) mediaPlayer.getCurrentPosition());
                    handler.postDelayed(this, INTERVAL);
                }
            }
        };

        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            public void onCompletion(MediaPlayer mp) {
                onCompleteListener.onComplete(SoundPlayer.this);
                durationCounter.set(0);
                handler.removeCallbacks(runnable);
            }
        });

        mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
                return false;
            }
        });
    }

    public void setAudioSource(Context context, Uri uri) throws IOException {
        mediaPlayer.setDataSource(context, uri);

        preparePlayer();
    }

    public void setAudioSource(String url) throws IOException {
        mediaPlayer.setDataSource(url);

        preparePlayer();
    }

    public void setAudioSource(Context context, File file) throws IOException {
        mediaPlayer.setDataSource(context, Uri.fromFile(file));

        preparePlayer();
    }

    public void play() {
        mediaPlayer.start();
        onPlayListener.onPlay(this);

        handler.postDelayed(runnable, INTERVAL);
    }

    public void pause() {
        mediaPlayer.pause();
        onPauseListener.onPause(this);
    }

    public void stop() {
        mediaPlayer.stop();
        try {
            mediaPlayer.prepare();
            onPreparedListener.onPrepared(this);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void toggle() {
        if(mediaPlayer.isPlaying()) {
            pause();
        } else {
            play();
        }
    }

    public void release(){
        handler.removeCallbacks(runnable);
        mediaPlayer.release();
    }

    public boolean isPlaying() {
        return mediaPlayer.isPlaying();
    }

    public long getDuration() {
        return mediaPlayer.getDuration();
    }

    public void seekTo(long msec){
        onSeekToListener.onSeekTo(this, msec);
        durationCounter.set((msec / INTERVAL) * INTERVAL);
        mediaPlayer.seekTo(msec, MediaPlayer.SEEK_CLOSEST_SYNC);
    }

    public boolean isPrepared() {
        return isPrepared;
    }


    public static String convertFromMSecToMinSec(long msec){
        int seconds = (int) (msec / 1000);
        int minutes = seconds / 60;
        seconds = seconds % 60;
        if (minutes > 60){
            return minutes / 60 + ":" + String.format("%02d", minutes % 60) + ":" + String.format("%02d", seconds);
        } else {
            return minutes % 60 + ":" +  String.format("%02d", seconds);
        }
    }

    public SoundPlayer setOnPreparedListener(SoundPlayerOnPreparedListener onPreparedListener) {
        this.onPreparedListener = onPreparedListener;
        return this;
    }

    public SoundPlayer setOnPauseListener(SoundPlayerOnPauseListener onPauseListener) {
        this.onPauseListener = onPauseListener;
        return this;
    }

    public SoundPlayer setOnPlayListener(SoundPlayerOnPlayListener onPlayListener) {
        this.onPlayListener = onPlayListener;
        return this;
    }

    public SoundPlayer setOnDurationProgressListener(SoundPlayerOnDurationProgressListener onDurationProgressListener) {
        this.onDurationProgressListener = onDurationProgressListener;
        return this;
    }

    public SoundPlayer setOnCompleteListener(SoundPlayerOnCompleteListener onCompleteListener) {
        this.onCompleteListener = onCompleteListener;
        return this;
    }

    public SoundPlayer setOnSeekToListener(SoundPlayerOnSeekToListener onSeekToListener) {
        this.onSeekToListener = onSeekToListener;
        return this;
    }
}
