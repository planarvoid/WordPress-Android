package com.soundcloud.android.service.playback;

import com.soundcloud.android.model.Track;
interface ICloudPlaybackService
{
    // controls
    void play();
    void pause();
    void toggle();
    void stop();
    void prev();
    long seek(long pos, boolean performSeek);
    void next();
    void restart();

    void playFromAppCache(int playPos);

    // state
    boolean isSeekable();
    boolean isPlaying();
    boolean isSupposedToBePlaying();
    boolean isBuffering();
    int loadPercent();

    long duration();
    long position();

    // metadata
    Track getTrack();
    String getTrackName();
    long getTrackId();
    String getUserName();
    String getUserPermalink();
    String getWaveformUrl();
    boolean getDownloadable();

    // favoriting
    void setFavoriteStatus(long trackId, boolean favoriteStatus);
    void setQueuePosition(int index);

    // modal
    void setClearToPlay(boolean clearToPlay);
    void setAutoAdvance(boolean autoAdvance);

    // queue
    int getQueueLength();
    Track getTrackAt(int pos);
    long getTrackIdAt(int pos);
    int getQueuePosition();
}
