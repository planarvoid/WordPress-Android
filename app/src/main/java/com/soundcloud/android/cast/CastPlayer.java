package com.soundcloud.android.cast;

import com.google.android.gms.cast.framework.media.RemoteMediaClient;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaybackResult;
import rx.Observable;

import java.util.List;

public interface CastPlayer {
    void togglePlayback();

    boolean resume();

    void pause();

    void playCurrent();

    Observable<PlaybackResult> setNewQueue(List<Urn> trackItemUrns, Urn initialTrackUrn, PlaySessionSource playSessionSource);

    long seek(long position);

    void onDisconnected();

    void onConnected(RemoteMediaClient remoteMediaClient);

    void pullRemotePlayQueueAndUpdateLocalState();

    void playLocalPlayQueueOnRemote();
}
