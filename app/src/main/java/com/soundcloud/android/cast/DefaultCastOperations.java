package com.soundcloud.android.cast;

import static com.soundcloud.android.playback.PlaybackUtils.correctInitialPosition;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.ads.AdUtils;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueue;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.policies.PolicyOperations;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.tracks.TrackRepository;
import rx.Observable;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;

public class DefaultCastOperations {

    private final TrackRepository trackRepository;
    private final PolicyOperations policyOperations;
    private final PlayQueueManager playQueueManager;
    private final CastJsonHandler jsonHandler;
    private final CastQueueController castQueueController;
    private final AccountOperations accountOperations;

    @Inject
    public DefaultCastOperations(TrackRepository trackRepository,
                                 PolicyOperations policyOperations,
                                 PlayQueueManager playQueueManager,
                                 CastJsonHandler jsonHandler,
                                 CastQueueController castQueueController,
                                 AccountOperations accountOperations) {
        this.trackRepository = trackRepository;
        this.policyOperations = policyOperations;
        this.playQueueManager = playQueueManager;
        this.jsonHandler = jsonHandler;
        this.castQueueController = castQueueController;
        this.accountOperations = accountOperations;
    }

    Observable<LoadMessageParameters> createLoadMessageParameters(final Urn currentTrackUrn, final boolean autoplay, final long playPosition, final List<Urn> queueTracks) {
        return filterTracksToBePlayedRemotely(currentTrackUrn, queueTracks)
                .map(filteredUrns -> {
                    if (filteredUrns.isEmpty()) {
                        throw new IllegalStateException("Cannot play track " + currentTrackUrn + " as it was filtered out of the list of tracks");
                    } else {
                        return filteredUrns;
                    }
                }).zipWith(trackRepository.track(currentTrackUrn), (tracks, trackItem) -> {
                    CastPlayQueue castPlayQueue = castQueueController.buildCastPlayQueue(currentTrackUrn, tracks);
                    castPlayQueue.setCredentials(getCastCredentials());
                    return new LoadMessageParameters(autoplay, playPosition, jsonHandler.toJson(castPlayQueue));
                });
    }

    Observable<List<Urn>> filterTracksToBePlayedRemotely(Urn currentTrackUrn, List<Urn> unfilteredTracks) {
        return filterMonetizableTracks(unfilteredTracks)
                .toList()
                .map(filteredUrns -> {
                    if (filteredUrns.contains(currentTrackUrn)) {
                        return filteredUrns;
                    } else {
                        return Collections.emptyList();
                    }
                });
    }

    private Observable<Urn> filterMonetizableTracks(List<Urn> unfilteredTracks) {
        return policyOperations.filterMonetizableTracks(unfilteredTracks)
                               .flatMap(RxUtils.iterableToObservable())
                               .flatMap(trackRepository::track)
                               .map(PlayableItem::getUrn);
    }

    public void setNewPlayQueue(List<Urn> urns, Urn initialTrackUrn, PlaySessionSource playSessionSource) {
        PlayQueue playQueue = PlayQueue.fromTrackUrnList(urns, playSessionSource, Collections.emptyMap());
        playQueueManager.setNewPlayQueue(playQueue, playSessionSource, correctInitialPosition(playQueue, 0, initialTrackUrn));
    }

    List<Urn> getCurrentQueueUrnsWithoutAds() {
        return PlayQueue.fromPlayQueueItems(playQueueManager.getPlayQueueItems(AdUtils.IS_NOT_AD)).getTrackItemUrns();
    }

    CastCredentials getCastCredentials() {
        return new CastCredentials(accountOperations.getSoundCloudToken());
    }
}
