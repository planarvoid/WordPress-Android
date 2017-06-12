package com.soundcloud.android.playback;

import com.soundcloud.android.model.Urn;
import io.reactivex.Completable;
import io.reactivex.Single;

public interface PlaybackStrategy {

    void togglePlayback();

    void resume();

    void pause();

    void fadeAndPause();

    Completable playCurrent();

    Single<PlaybackResult> setNewQueue(final PlayQueue playQueue,
                                       Urn initialTrackUrn,
                                       int initialTrackPosition,
                                       PlaySessionSource playSessionSource);

    void seek(long position);

}
