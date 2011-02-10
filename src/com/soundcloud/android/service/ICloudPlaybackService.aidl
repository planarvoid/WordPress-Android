package com.soundcloud.android.service;

import android.graphics.Bitmap;
import com.soundcloud.android.objects.Track;
import com.soundcloud.android.objects.Comment;

interface ICloudPlaybackService
{
    void playFromAppCache(int playPos);
    int getQueuePosition();
    boolean isSeekable();
    boolean isPlaying();
    void stop();
    void pause();
    void forcePause();
    void play();
    void prev();
    void next();
    void restart();
    long duration();
    long position();
    int loadPercent();
    long seek(long pos);
    long getSeekResult(long pos);
    Track getTrack();
    String getTrackName();
    long getTrackId();
    String getUserName();
    String getUserPermalink();
    String getWaveformUrl();
    boolean isBuffering();
    void setFavoriteStatus(long trackId, boolean favoriteStatus);
    void setQueuePosition(int index);
    boolean getDownloadable();
}