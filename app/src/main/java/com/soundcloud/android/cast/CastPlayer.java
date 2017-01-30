package com.soundcloud.android.cast;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueue;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaybackResult;
import rx.Observable;

import java.util.List;

public interface CastPlayer {
    void onConnected();

    void onDisconnected();

    void togglePlayback();

    boolean resume();

    void pause();

    void playCurrent();

    Observable<PlaybackResult> setNewQueue(PlayQueue playQueue, Urn initialTrackUrn, PlaySessionSource playSessionSource);

    long seek(long position);
}
