package com.soundcloud.android.service;

import android.graphics.Bitmap;
import com.soundcloud.android.objects.Track;
import com.soundcloud.android.objects.Comment;

interface ICloudPlaybackService
{
    void openFile(in Track track, boolean oneShot);
    void enqueueTrack(in Track track, int action);
    void enqueue(in Track[] trackData, int playPos);
    void playFromAppCache(int playPos);
    void clearQueue();
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
    Track getTrack();
    String getTrackName();
    int getTrackId();
    String getUserName();
    String getUserPermalink();
    String getWaveformUrl();
    boolean isBuffering();
    void setComments(in Comment[] commentData, int trackId);
    void addComment(in Comment commentData);
    void setFavoriteStatus(int trackId, String favoriteStatus);
    List<Track> getQueue();
    void moveQueueItem(int from, int to);
    void setQueuePosition(int index);
    boolean getDownloadable();
    int removeTracks(int first, int last);
    int removeTrack(int id);
}

