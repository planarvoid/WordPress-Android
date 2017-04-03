package com.soundcloud.android.cast;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueue;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaybackResult;
import rx.Observable;

public interface CastPlayer {
    void onConnected(boolean wasPlaying);

    void onDisconnected();

    void togglePlayback();

    void resume();

    void pause();

    void playCurrent();

    Observable<PlaybackResult> setNewQueue(PlayQueue playQueue, Urn initialTrackUrn, PlaySessionSource playSessionSource);

    void seek(long position);
}
