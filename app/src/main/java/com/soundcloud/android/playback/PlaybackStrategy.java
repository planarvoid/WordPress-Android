package com.soundcloud.android.playback;

import com.soundcloud.android.model.Urn;
import rx.Observable;

public interface PlaybackStrategy {

    void togglePlayback();

    void resume();

    void pause();

    Observable<Void> playCurrent();

    Observable<PlaybackResult> setNewQueue(final PlayQueue playQueue, Urn initialTrackUrn, int initialTrackPosition, boolean loadRelated, PlaySessionSource playSessionSource);

    void seek(long position);

}
