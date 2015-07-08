package com.soundcloud.android.playback;

import com.soundcloud.android.model.Urn;
import rx.Observable;

public interface PlaybackStrategy {

    void togglePlayback();

    void resume();

    void pause();

    void playCurrent();

    Observable<PlaybackResult> playNewQueue(final PlayQueue playQueue, Urn initialTrackUrn, int initialTrackPosition, boolean loadRelated, PlaySessionSource playSessionSource);

    void seek(long position);

}