package com.soundcloud.android.service.playback;

import static com.soundcloud.android.service.playback.CloudPlaybackService.TAG;

import com.soundcloud.android.rx.observers.ScObserver;
import rx.Observer;

import android.media.MediaPlayer;
import android.net.Uri;
import android.util.Log;

import java.io.IOException;
import java.lang.ref.WeakReference;

public class MediaPlayerDataSourceObserver extends ScObserver<Uri> {
    private WeakReference<MediaPlayer> mMediaPlayer;
    private WeakReference<MediaPlayer.OnErrorListener> mErrorListener;

    public MediaPlayerDataSourceObserver(MediaPlayer mediaPlayer, MediaPlayer.OnErrorListener errorListener) {
        this.mMediaPlayer = new WeakReference<MediaPlayer>(mediaPlayer);
        this.mErrorListener = new WeakReference<MediaPlayer.OnErrorListener>(errorListener);
    }

    @Override
    public void onError(Throwable e) {
        Log.e(TAG, "error", e);

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
