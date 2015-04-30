package com.soundcloud.android.playback;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.service.PlaySessionSource;
import rx.Observable;

import java.util.List;

public interface PlaybackStrategy {

    void togglePlayback();

    void resume();

    void pause();

    void playCurrent();

    Observable<PlaybackResult> playNewQueue(Observable<List<Urn>> playQueueTracks, Urn initialTrackUrn, int initialTrackPosition, boolean loadRelated, PlaySessionSource playSessionSource);

    void reloadAndPlayCurrentQueue(long withProgressPosition);

    void seek(long position);

}