package com.soundcloud.android.service;

import android.graphics.Bitmap;
import com.soundcloud.android.objects.Track;
import com.soundcloud.android.objects.Comment;

interface ICloudPlaybackService
{
    void openFile(in Track track, boolean oneShot);
    void enqueueTrack(in Track track, int action);
    void enqueue(in Track[] trackData, int playPos);
    void clearQueue();
    int getQueuePosition();
    boolean isSeekable();
    boolean isPlaying();
    void stop();
    void pause();
    void play();
    void prev();
    void next();
    void restart();
    long duration();
    long position();
    long lastSuccessfulSeek();
    int loadPercent();
    long seek(long pos);
    Track getTrack();
    String getTrackName();
    String getTrackId();
    String getUserName();
    String getUserPermalink();
    String getWaveformUrl();
    boolean isAsyncOpening();
    void setComments(in Comment[] commentData, String trackId);
    void addComment(in Comment commentData);
    void setFavoriteStatus(String trackId, String favoriteStatus);
    List<Track> getQueue();
    void moveQueueItem(int from, int to);
    void setQueuePosition(int index);
    String getPath();
    String getDuration();
    String getDownloadable();
    int removeTracks(int first, int last);
    int removeTrack(String id);
}

