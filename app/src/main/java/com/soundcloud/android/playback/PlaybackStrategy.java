package com.soundcloud.android.playback;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.service.PlayQueue;
import com.soundcloud.android.playback.service.PlaySessionSource;
import rx.Observable;

public interface PlaybackStrategy {

    void togglePlayback();

    void resume();

    void pause();

    void playCurrent();

    Observable<PlaybackResult> playNewQueue(final PlayQueue playQueue, Urn initialTrackUrn, int initialTrackPosition, boolean loadRelated, PlaySessionSource playSessionSource);

    void seek(long position);

}