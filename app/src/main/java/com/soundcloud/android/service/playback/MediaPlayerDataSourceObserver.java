package com.soundcloud.android.service.playback;

import com.soundcloud.android.rx.observers.DefaultObserver;

import android.media.MediaPlayer;
import android.net.Uri;

import java.io.IOException;
import java.lang.ref.WeakReference;

public class MediaPlayerDataSourceObserver extends DefaultObserver<Uri> {
    private WeakReference<MediaPlayer> mMediaPlayer;
    private WeakReference<MediaPlayer.OnErrorListener> mErrorListener;

    public MediaPlayerDataSourceObserver(MediaPlayer mediaPlayer, MediaPlayer.OnErrorListener errorListener) {
        this.mMediaPlayer = new WeakReference<MediaPlayer>(mediaPlayer);
        this.mErrorListener = new WeakReference<MediaPlayer.OnErrorListener>(errorListener);
    }

    @Override
    public void onError(Throwable e) {
        super.onError(e);

        MediaPlayer.OnErrorListener errorListener = mErrorListener.get();
        MediaPlayer mediaPlayer = mMediaPlayer.get();

        if (errorListener != null && mediaPlayer != null) {
            errorListener.onError(mediaPlayer, 0, 0);
        }
    }

    @Override
    public void onNext(Uri uri) {
        MediaPlayer mediaPlayer = mMediaPlayer.get();

        if (mediaPlayer != null) {
            try {
                mediaPlayer.setDataSource(uri.toString());
                mediaPlayer.prepareAsync();
            } catch (IOException e) {
                onError(e);
            } catch (IllegalStateException e) {
                onError(e);
            }
        }
    }
}
