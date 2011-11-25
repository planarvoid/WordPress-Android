package com.soundcloud.android.service.playback;

import com.soundcloud.android.model.Track;
interface ICloudPlaybackService
{
    void playFromAppCache(int playPos);
    int getQueuePosition();
    boolean isSeekable();
    boolean isPlaying();
    boolean isSupposedToBePlaying();
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
    void setClearToPlay(boolean clearToPlay);
    void setAutoAdvance(boolean autoAdvance);
    int getQueueLength();
    Track getTrackAt(int pos);
    long getTrackIdAt(int pos);
}
