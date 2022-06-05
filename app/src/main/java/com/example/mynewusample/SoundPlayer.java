package com.example.mynewusample;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.masoudss.lib.WaveformSeekBar;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

public class SoundPlayer extends Service {

    private static final String ACTION_PLAY = "PLAY";

    private final MediaPlayer mediaPlayer = new MediaPlayer();
    private Handler handler = new Handler();
    private Runnable runnable;
    private AtomicLong durationCounter = new AtomicLong();

    private final long INTERVAL = 32;

    private Context mContext;
    private WaveformSeekBar waveformSeekBar;
    private ImageView imageViewPlayButton;

    private boolean isPrepared = false;

    public SoundPlayer() {
    }

    public SoundPlayer(Context mContext, WaveformSeekBar waveformSeekBar) {
        this.mContext = mContext;
        this.waveformSeekBar = waveformSeekBar;
    }

    public SoundPlayer(Context mContext, WaveformSeekBar waveformSeekBar, ImageView imageViewPlayButton) {
        this.mContext = mContext;
        this.waveformSeekBar = waveformSeekBar;
        this.imageViewPlayButton = imageViewPlayButton;
    }

    public void preparePlayer() {
        mediaPlayer.prepareAsync();
        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                isPrepared = true;
            }
        });

        runnable = new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer.isPlaying()) {
                    waveformSeekBar.setProgress(100 * mediaPlayer.getCurrentPosition() / (float) getDuration());
                    handler.postDelayed(this, INTERVAL);
                }
            }
        };

        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            public void onCompletion(MediaPlayer mp) {
                durationCounter.set(0);
                waveformSeekBar.setProgress(0);
                handler.removeCallbacks(runnable);
                imageViewPlayButton.setImageDrawable(mContext.getDrawable(R.drawable.ic_round_play_arrow_24));
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

        handler.postDelayed(runnable, INTERVAL);
    }

    public void pause() {
        mediaPlayer.pause();
    }

    public void stop() {
        mediaPlayer.stop();
        try {
            mediaPlayer.prepare();
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
        durationCounter.set((msec / INTERVAL) * INTERVAL);
        mediaPlayer.seekTo(msec, MediaPlayer.SEEK_CLOSEST_SYNC);
    }

    public boolean isPrepared() {
        return isPrepared;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCreate() {
        preparePlayer();
        super.onCreate();
    }

}
