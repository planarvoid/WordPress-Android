package com.soundcloud.android.cast;

import static com.soundcloud.android.playback.PlaybackUtils.correctInitialPosition;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueue;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.tracks.TrackRepository;
import rx.Observable;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;

public class DefaultCastOperations {

    private final TrackRepository trackRepository;
    private final PlayQueueManager playQueueManager;
    private final CastJsonHandler jsonHandler;
    private final CastQueueController castQueueController;
    private final AccountOperations accountOperations;

    @Inject
    public DefaultCastOperations(TrackRepository trackRepository,
                                 PlayQueueManager playQueueManager,
                                 CastJsonHandler jsonHandler,
                                 CastQueueController castQueueController,
                                 AccountOperations accountOperations) {
        this.trackRepository = trackRepository;
        this.playQueueManager = playQueueManager;
        this.jsonHandler = jsonHandler;
        this.castQueueController = castQueueController;
        this.accountOperations = accountOperations;
    }

    Observable<LoadMessageParameters> createLoadMessageParameters(final Urn currentTrackUrn, final boolean autoplay, final long playPosition, final List<Urn> queueTracks) {
        return Observable.just(queueTracks)
                         .zipWith(trackRepository.track(currentTrackUrn), (tracks, currentTrackItem) -> {
                             CastPlayQueue castPlayQueue = castQueueController.buildCastPlayQueue(currentTrackUrn, tracks);
                             castPlayQueue.setCredentials(getCastCredentials());
                             return new LoadMessageParameters(autoplay, playPosition, jsonHandler.toJson(castPlayQueue));
                         });
    }

    public void setNewPlayQueue(List<Urn> urns, Urn initialTrackUrn, PlaySessionSource playSessionSource) {
        PlayQueue playQueue = PlayQueue.fromTrackUrnList(urns, playSessionSource, Collections.emptyMap());
        playQueueManager.setNewPlayQueue(playQueue, playSessionSource, correctInitialPosition(playQueue, 0, initialTrackUrn));
    }

    CastCredentials getCastCredentials() {
        return new CastCredentials(accountOperations.getSoundCloudToken());
    }
}
