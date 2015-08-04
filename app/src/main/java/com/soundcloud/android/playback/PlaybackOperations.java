package com.soundcloud.android.playback;

import static com.soundcloud.android.playback.PlaybackResult.ErrorReason.MISSING_PLAYABLE_TRACKS;
import static com.soundcloud.android.playback.PlaybackResult.ErrorReason.UNSKIPPABLE;

import com.soundcloud.android.ServiceInitiator;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.ads.AdConstants;
import com.soundcloud.android.ads.AdsOperations;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.api.legacy.model.ScModelManager;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.ui.view.PlaybackToastHelper;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.storage.TrackStorage;
import com.soundcloud.java.collections.PropertySet;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;

import android.net.Uri;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class PlaybackOperations {
    public static final boolean WITH_RELATED = true;
    public static final boolean WITHOUT_RELATED = false;

    private static final long PROGRESS_THRESHOLD_FOR_TRACK_CHANGE = TimeUnit.SECONDS.toMillis(3L);
    private static final long SEEK_POSITION_RESET = 0L;

    private final ScModelManager modelManager;
    private final TrackStorage trackStorage;
    private final PlayQueueManager playQueueManager;
    private final PlaySessionStateProvider playSessionStateProvider;
    private final PlaybackToastHelper playbackToastHelper;
    private final EventBus eventBus;
    private final AdsOperations adsOperations;
    private final AccountOperations accountOperations;
    private final PlayQueueOperations playQueueOperations;
    private final Provider<PlaybackStrategy> playbackStrategyProvider;
    private final ServiceInitiator serviceInitiator;


    @Inject
    public PlaybackOperations(ServiceInitiator serviceInitiator,
                              ScModelManager modelManager, TrackStorage trackStorage,
                              PlayQueueManager playQueueManager,
                              PlaySessionStateProvider playSessionStateProvider,
                              PlaybackToastHelper playbackToastHelper, EventBus eventBus,
                              AdsOperations adsOperations, AccountOperations accountOperations,
                              PlayQueueOperations playQueueOperations,
                              Provider<PlaybackStrategy> playbackStrategyProvider) {
        this.serviceInitiator = serviceInitiator;
        this.modelManager = modelManager;
        this.trackStorage = trackStorage;
        this.playQueueManager = playQueueManager;
        this.playSessionStateProvider = playSessionStateProvider;
        this.playbackToastHelper = playbackToastHelper;
        this.eventBus = eventBus;
        this.adsOperations = adsOperations;
        this.accountOperations = accountOperations;
        this.playQueueOperations = playQueueOperations;
        this.playbackStrategyProvider = playbackStrategyProvider;
    }

    public Observable<PlaybackResult> playTracks(List<Urn> trackUrns, int position, PlaySessionSource playSessionSource) {
        return playTracks(trackUrns, trackUrns.get(position), position, playSessionSource);
    }

    public Observable<PlaybackResult> playTracks(List<Urn> trackUrns, Urn trackUrn, int position,
                                            PlaySessionSource playSessionSource) {
        final Observable<PlayQueue> playQueue = Observable.from(trackUrns).toList().map(urnsToPlayQueue(playSessionSource));
        return playTracksList(playQueue, trackUrn, position, playSessionSource, WITHOUT_RELATED);
    }

    public Observable<PlaybackResult> playTracks(Observable<List<Urn>> allTracks, Urn initialTrack, int position,
                                            PlaySessionSource playSessionSource) {
        final Observable<PlayQueue> playQueue = allTracks.map(urnsToPlayQueue(playSessionSource));
        return playTracksList(playQueue, initialTrack, position, playSessionSource, WITHOUT_RELATED);
    }

    public Observable<PlaybackResult> playPosts(Observable<List<PropertySet>> allTracks, Urn initialTrack, int position,
                                                 PlaySessionSource playSessionSource) {
        final Observable<PlayQueue> playQueue = allTracks.map(tracksToPlayQueue(playSessionSource));
        return playTracksList(playQueue, initialTrack, position, playSessionSource, WITHOUT_RELATED);
    }

    @Deprecated
    public Observable<PlaybackResult> playTracksFromUri(Uri uri, int startPosition, Urn initialTrack,
                                                        PlaySessionSource playSessionSource) {
        final Observable<PlayQueue> playQueue = trackStorage.getTracksForUriAsync(uri).map(urnsToPlayQueue(playSessionSource));
        return playTracksList(playQueue, initialTrack, startPosition, playSessionSource, WITHOUT_RELATED);
    }

    @Deprecated
    // Please, use playTrackWithRecommendations instead.
    public Observable<PlaybackResult> playTrackWithRecommendationsLegacy(Urn track, PlaySessionSource playSessionSource) {
        // TODO : move to the alternative solution when playing the tracking story DROID-1028
        final Observable<PlayQueue> playQueue = Observable.just(track).toList().map(urnsToPlayQueue(playSessionSource));
        return playTracksList(playQueue, track, 0, playSessionSource, WITH_RELATED);
    }

    public Observable<PlaybackResult> playTrackWithRecommendations(final Urn seedTrack, final PlaySessionSource playSessionSource, final int startPosition) {
        return playQueueOperations
                .relatedTracksPlayQueueWithSeedTrack(seedTrack)
                .flatMap(toPlaybackResult(playbackStrategyProvider.get(), startPosition, playSessionSource, false));
    }

    public Observable<PlaybackResult> playTracksShuffled(Observable<List<Urn>> trackUrnsObservable,
                                                    final PlaySessionSource playSessionSource) {
        if (shouldDisableSkipping()) {
            return Observable.just(PlaybackResult.error(UNSKIPPABLE));
        } else {
            return trackUrnsObservable
                    .map(toShuffledPlayQueue(playSessionSource))
                    .flatMap(toPlaybackResult(playbackStrategyProvider.get(), Urn.NOT_SET, 0, playSessionSource, WITHOUT_RELATED))
                    .observeOn(AndroidSchedulers.mainThread());
        }
    }

    private Func1<List<Urn>, PlayQueue> toShuffledPlayQueue(final PlaySessionSource playSessionSource) {
        return new Func1<List<Urn>, PlayQueue>() {
            @Override
            public PlayQueue call(List<Urn> urns) {
                return PlayQueue.shuffled(urns, playSessionSource);
            }
        };
    }

    private Observable<PlaybackResult> playTracksList(Observable<PlayQueue> playQueue,
                                                      Urn initialTrack,
                                                      int startPosition,
                                                      final PlaySessionSource playSessionSource,
                                                      boolean loadRelated) {
        if (!shouldChangePlayQueue(initialTrack, playSessionSource)) {
            return Observable.just(PlaybackResult.success());
        } else if (shouldDisableSkipping()) {
            return Observable.just(PlaybackResult.error(UNSKIPPABLE));
        } else {
            return playQueue
                    .flatMap(toPlaybackResult(playbackStrategyProvider.get(), initialTrack, startPosition, playSessionSource, loadRelated))
                    .observeOn(AndroidSchedulers.mainThread());
        }
    }

    private Func1<List<Urn>, PlayQueue> urnsToPlayQueue(final PlaySessionSource playSessionSource) {
        return new Func1<List<Urn>, PlayQueue>() {
            @Override
            public PlayQueue call(List<Urn> urns) {
                if (urns.isEmpty()) {
                    return PlayQueue.empty();
                } else {
                    return PlayQueue.fromTrackUrnList(urns, playSessionSource);
                }
            }
        };
    }

    private Func1<List<PropertySet>, PlayQueue> tracksToPlayQueue(final PlaySessionSource playSessionSource) {
        return new Func1<List<PropertySet>, PlayQueue>() {
            @Override
            public PlayQueue call(List<PropertySet> urns) {
                if (urns.isEmpty()) {
                    return PlayQueue.empty();
                } else {
                    return PlayQueue.fromTrackList(urns, playSessionSource);
                }
            }
        };
    }

    private Func1<PlayQueue, Observable<PlaybackResult>> toPlaybackResult(final PlaybackStrategy playbackStrategy,
                                                                          final int startPosition,
                                                                          final PlaySessionSource playSessionSource,
                                                                          final boolean loadRelated) {
        return new Func1<PlayQueue, Observable<PlaybackResult>>() {
            @Override
            public Observable<PlaybackResult> call(PlayQueue playQueue) {
                return playNewQueue(playbackStrategy, playQueue, playQueue.getUrn(startPosition), startPosition, loadRelated, playSessionSource);
            }
        };
    }

    private Func1<PlayQueue, Observable<PlaybackResult>> toPlaybackResult(final PlaybackStrategy playbackStrategy,
                                                                          final Urn initialTrack,
                                                                          final int startPosition,
                                                                          final PlaySessionSource playSessionSource,
                                                                          final boolean loadRelated) {
        return new Func1<PlayQueue, Observable<PlaybackResult>>() {
            @Override
            public Observable<PlaybackResult> call(PlayQueue playQueue) {
                return playNewQueue(playbackStrategy, playQueue, initialTrack, startPosition, loadRelated, playSessionSource);
            }
        };
    }

    private Observable<PlaybackResult> playNewQueue(PlaybackStrategy playbackStrategy, PlayQueue playQueue, Urn initialTrack, int startPosition, boolean loadRelated, PlaySessionSource playSessionSource) {
        if (playQueue.isEmpty()) {
            return Observable.just(PlaybackResult.error(MISSING_PLAYABLE_TRACKS));
        } else {
            return playbackStrategy.playNewQueue(playQueue, initialTrack, startPosition, loadRelated, playSessionSource);
        }
    }

    public Observable<PlaybackResult> startPlayback(PublicApiTrack track, Screen screen, boolean withRecommendations) {
        playQueueManager.clearAll();
        modelManager.cache(track);
        final PlaySessionSource playSessionSource = new PlaySessionSource(screen);
        return playTracksList(Observable.just(track.getUrn()).toList()
                .map(urnsToPlayQueue(playSessionSource)), track.getUrn(), 0, playSessionSource, withRecommendations);
    }

    public Observable<PlaybackResult> startPlaybackWithRecommendations(Urn urn, Screen screen, SearchQuerySourceInfo searchQuerySourceInfo) {
        PlaySessionSource playSessionSource = new PlaySessionSource(screen);
        playSessionSource.setSearchQuerySourceInfo(searchQuerySourceInfo);
        return playTracksList(Observable.just(urn).toList()
                .map(urnsToPlayQueue(playSessionSource)), urn, 0, playSessionSource, WITH_RELATED);
    }

    public void togglePlayback() {
        if (playSessionStateProvider.isPlayingCurrentPlayQueueTrack()) {
            playbackStrategyProvider.get().togglePlayback();
        } else {
            playCurrent();
        }
    }

    public void play() {
        playbackStrategyProvider.get().resume();
    }

    public void pause() {
        playbackStrategyProvider.get().pause();
    }

    void playCurrent() {
        playbackStrategyProvider.get().playCurrent();
    }

    public void setPlayQueuePosition(int position) {
        if (position != playQueueManager.getCurrentPosition()) {
            publishSkipEventIfAudioAd();
            playQueueManager.setPosition(position);
        }
    }

    public void previousTrack() {
        if (shouldDisableSkipping()) {
            playbackToastHelper.showUnskippableAdToast();
        } else {
            if (playSessionStateProvider.getLastProgressEvent().getPosition() >= PROGRESS_THRESHOLD_FOR_TRACK_CHANGE
                    && !adsOperations.isCurrentTrackAudioAd()) {
                seek(SEEK_POSITION_RESET);
            } else {
                publishSkipEventIfAudioAd();
                playQueueManager.moveToPreviousTrack();
            }
        }
    }

    public void nextTrack() {
        if (shouldDisableSkipping()) {
            playbackToastHelper.showUnskippableAdToast();
        } else {
            publishSkipEventIfAudioAd();
            playQueueManager.nextTrack();
        }
    }

    private void publishSkipEventIfAudioAd() {
        if (adsOperations.isCurrentTrackAudioAd()) {
            final UIEvent event = UIEvent.fromSkipAudioAdClick(playQueueManager.getCurrentMetaData(), playQueueManager.getCurrentTrackUrn(), accountOperations.getLoggedInUserUrn(), playQueueManager.getCurrentTrackSourceInfo());
            eventBus.publish(EventQueue.TRACKING, event);
        }
    }

    public void stopService() {
        serviceInitiator.stopPlaybackService();
    }

    public void resetService() {
        serviceInitiator.resetPlaybackService();
    }

    public void seek(long position) {
        if (!shouldDisableSkipping()) {
            if (playSessionStateProvider.isPlayingCurrentPlayQueueTrack()) {
                playbackStrategyProvider.get().seek(position);
            } else {
                playQueueManager.saveCurrentProgress(position);
            }

        }
    }

    public boolean shouldDisableSkipping() {
        return adsOperations.isCurrentTrackAudioAd() &&
                playSessionStateProvider.getCurrentPlayQueueTrackProgress().getPosition() < AdConstants.UNSKIPPABLE_TIME_MS;
    }

    private boolean shouldChangePlayQueue(Urn trackUrn, PlaySessionSource playSessionSource) {
        return !isCurrentTrack(trackUrn) || !isCurrentScreenSource(playSessionSource) ||
                !playQueueManager.isCurrentCollection(playSessionSource.getCollectionUrn());
    }

    private boolean isCurrentScreenSource(PlaySessionSource playSessionSource) {
        return playSessionSource.getOriginScreen().equals(playQueueManager.getScreenTag());
    }

    private boolean isCurrentTrack(Urn trackUrn) {
        return playQueueManager.isCurrentTrack(trackUrn);
    }

}
