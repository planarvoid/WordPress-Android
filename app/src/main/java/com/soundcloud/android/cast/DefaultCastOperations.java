package com.soundcloud.android.cast;

import static com.soundcloud.android.ApplicationModule.LOW_PRIORITY;
import static com.soundcloud.android.cast.CastProtocol.TAG;
import static com.soundcloud.android.playback.PlaybackUtils.correctInitialPositionLegacy;

import com.google.android.gms.cast.MediaInfo;
import com.soundcloud.android.ads.AdUtils;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueue;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaybackConstants;
import com.soundcloud.android.policies.PolicyOperations;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.android.utils.Log;
import com.soundcloud.java.collections.PropertySet;
import org.json.JSONException;
import org.json.JSONObject;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Func1;
import rx.schedulers.TimeInterval;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class DefaultCastOperations implements CastOperations {

    private final TrackRepository trackRepository;
    private final PolicyOperations policyOperations;
    private final PlayQueueManager playQueueManager;
    private final Scheduler progressPullIntervalScheduler;
    private final CastProtocol castProtocol;

    private final Func1<Urn, Observable<PropertySet>> loadTracks = new Func1<Urn, Observable<PropertySet>>() {
        @Override
        public Observable<PropertySet> call(Urn urn) {
            return trackRepository.track(urn);
        }
    };

    @Inject
    public DefaultCastOperations(TrackRepository trackRepository,
                                 PolicyOperations policyOperations,
                                 PlayQueueManager playQueueManager,
                                 CastProtocol castProtocol,
                                 @Named(LOW_PRIORITY) Scheduler progressPullIntervalScheduler) {
        this.trackRepository = trackRepository;
        this.policyOperations = policyOperations;
        this.playQueueManager = playQueueManager;
        this.castProtocol = castProtocol;
        this.progressPullIntervalScheduler = progressPullIntervalScheduler;
    }

    Observable<LocalPlayQueue> loadLocalPlayQueueWithoutMonetizableAndPrivateTracks(final Urn currentTrackUrn,
                                                                                    List<Urn> unfilteredLocalPlayQueueTracks) {
        return filterMonetizableAndPrivateTracks(unfilteredLocalPlayQueueTracks)
                .toList().flatMap(new Func1<List<Urn>, Observable<LocalPlayQueue>>() {
                    @Override
                    public Observable<LocalPlayQueue> call(List<Urn> filteredLocalPlayQueueTracks) {
                        if (filteredLocalPlayQueueTracks.isEmpty()) {
                            return Observable.just(LocalPlayQueue.empty());
                        } else if (filteredLocalPlayQueueTracks.contains(currentTrackUrn)) {
                            return loadLocalPlayQueue(currentTrackUrn, filteredLocalPlayQueueTracks);
                        } else {
                            return loadLocalPlayQueue(filteredLocalPlayQueueTracks.get(0),
                                                      filteredLocalPlayQueueTracks);
                        }
                    }
                });
    }

    Observable<LocalPlayQueue> loadLocalPlayQueue(Urn currentTrackUrn, List<Urn> filteredLocalPlayQueueTracks) {
        return Observable.zip(trackRepository.track(currentTrackUrn),
                              Observable.from(filteredLocalPlayQueueTracks).toList(),
                              (track, filteredLocalPlayQueueTracks1) -> new LocalPlayQueue(
                                      castProtocol.createPlayQueueJSON(filteredLocalPlayQueueTracks1),
                                      filteredLocalPlayQueueTracks1,
                                      castProtocol.createMediaInfo(track),
                                      track.get(TrackProperty.URN)));
    }

    private Observable<Urn> filterMonetizableAndPrivateTracks(List<Urn> unfilteredLocalPlayQueueTracks) {
        return policyOperations.filterMonetizableTracks(unfilteredLocalPlayQueueTracks)
                               .flatMap(RxUtils.iterableToObservable())
                               .flatMap(loadTracks)
                               .filter(track -> !track.get(TrackProperty.IS_PRIVATE))
                               .map(track1 -> track1.get(TrackProperty.URN));
    }


    RemotePlayQueue loadRemotePlayQueue(MediaInfo mediaInfo) {
        try {
            if (mediaInfo != null) {
                final JSONObject customData = mediaInfo.getCustomData();
                if (customData != null) {
                    return RemotePlayQueue.create(castProtocol.convertRemoteDataToTrackList(customData),
                                               castProtocol.getRemoteCurrentTrackUrn(mediaInfo));
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "Unable to retrieve remote play queue", e);
        }
        return RemotePlayQueue.create(new ArrayList<>(), Urn.NOT_SET);
    }

    void setNewPlayQueue(LocalPlayQueue localPlayQueue, PlaySessionSource playSessionSource) {
        playQueueManager.setNewPlayQueue(
                PlayQueue.fromTrackUrnList(localPlayQueue.playQueueTrackUrns,
                                           playSessionSource,
                                           Collections.emptyMap()),
                playSessionSource,
                correctInitialPositionLegacy(localPlayQueue.playQueueTrackUrns, 0, localPlayQueue.currentTrackUrn)
        );
    }

    List<Urn> getCurrentQueueUrnsWithoutAds() {
        return PlayQueue.fromPlayQueueItems(playQueueManager.getPlayQueueItems(AdUtils.IS_NOT_AD)).getTrackItemUrns();
    }

    @Override
    public Observable<TimeInterval<Long>> intervalForProgressPull() {
        return Observable.interval(PlaybackConstants.PROGRESS_DELAY_MS,
                                   TimeUnit.MILLISECONDS,
                                   progressPullIntervalScheduler).timeInterval();
    }

}