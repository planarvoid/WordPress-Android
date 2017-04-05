package com.soundcloud.android.cast;

import static java.util.Collections.singletonList;

import com.soundcloud.android.cast.api.CastPlayQueue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueue;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.java.optional.Optional;

import android.support.annotation.Nullable;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.Map;

@Singleton
public class CastQueueController {

    private CastPlayQueue castPlayQueue;

    @Inject
    public CastQueueController() {
    }

    public void storePlayQueue(CastPlayQueue castPlayQueue) {
        this.castPlayQueue = castPlayQueue;
    }

    public CastPlayQueue buildCastPlayQueue(Urn currentTrackUrn, List<Urn> tracks) {
        return CastPlayQueue.create(getCurrentRevision(), currentTrackUrn, tracks);
    }

    private Optional<String> getCurrentRevision() {
        return castPlayQueue == null ? Optional.absent() : castPlayQueue.revision();
    }

    @Nullable
    public CastPlayQueue getCurrentQueue() {
        return castPlayQueue;
    }

    public Urn getRemoteCurrentTrackUrn() {
        return castPlayQueue != null ? castPlayQueue.getCurrentTrackUrn() : Urn.NOT_SET;
    }

    public boolean isCurrentlyLoadedOnRemotePlayer(Urn urn) {
        final Urn currentPlayingUrn = getRemoteCurrentTrackUrn();
        return currentPlayingUrn != Urn.NOT_SET && currentPlayingUrn.equals(urn);
    }

    public PlayQueue buildPlayQueue(PlaySessionSource playSessionSource,
                                    Map<Urn, Boolean> blockedTracks) {
        final List<Urn> trackUrns = (castPlayQueue == null || castPlayQueue.isEmpty()) ? singletonList(Urn.NOT_SET) : castPlayQueue.getQueueUrns();
        return PlayQueue.fromTrackUrnList(trackUrns, playSessionSource, blockedTracks);
    }

    public CastPlayQueue buildUpdatedCastPlayQueue(Urn currentTrackUrn, long progress) {
        return CastPlayQueue.forUpdate(currentTrackUrn, progress, castPlayQueue);
    }
}