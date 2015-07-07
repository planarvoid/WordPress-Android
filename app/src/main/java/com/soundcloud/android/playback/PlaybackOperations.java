package com.soundcloud.android.playback;

import static com.soundcloud.android.playback.PlaybackResult.ErrorReason.MISSING_PLAYABLE_TRACKS;
import static com.soundcloud.android.playback.PlaybackResult.ErrorReason.UNSKIPPABLE;

import com.google.common.collect.Lists;
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
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.playback.service.PlayQueueOperations;
import com.soundcloud.android.playback.service.PlaySessionSource;
import com.soundcloud.android.playback.ui.view.PlaybackToastHelper;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.storage.TrackStorage;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;

import android.net.Uri;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class PlaybackOperations {
    public static final boolean WITH_RELATED = true;
    public static final boolean WITHOUT_RELATED = false;

    private static final long PROGRESS_THRESHOLD_FOR_TRACK_CHANGE = TimeUnit.SECONDS.toMillis(3L);
    private static final long SEEK_POSITION_RESET = 0L;

    private static final Func1<List<Urn>, List<Urn>> SHUFFLE_TRACKS = new Func1<List<Urn>, List<Urn>>() {
        @Override
        public List<Urn> call(List<Urn> trackUrns) {
            List<Urn> shuffled = Lists.newArrayList(trackUrns);
            Collections.shuffle(shuffled);
            return shuffled;
        }
    };

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
        return playTracksList(Observable.from(trackUrns).toList(), trackUrn, position, playSessionSource, WITHOUT_RELATED);
    }

    public Observable<PlaybackResult> playTracks(Observable<List<Urn>> allTracks, Urn initialTrack, int position,
                                            PlaySessionSource playSessionSource) {
        return playTracksList(allTracks, initialTrack, position, playSessionSource, WITHOUT_RELATED);
    }

    @Deprecated
    public Observable<PlaybackResult> playTracksFromUri(Uri uri, int startPosition, Urn initialTrack,
                                                        PlaySessionSource playSessionSource) {
        return playTracksList(trackStorage.getTracksForUriAsync(uri), initialTrack, startPosition,
                playSessionSource, WITHOUT_RELATED);
    }

    @Deprecated
    // Please, use playTrackWithRecommendations instead.
    public Observable<PlaybackResult> playTrackWithRecommendationsLegacy(Urn track, PlaySessionSource playSessionSource) {
        // TODO : move to the alternative solution when playing the tracking story DROID-1028
        return playTracksList(Observable.just(track).toList(), track, 0, playSessionSource, WITH_RELATED);
    }

    public Observable<PlaybackResult> playTrackWithRecommendations(final Urn seedTrack, final PlaySessionSource playSessionSource, final int startPosition) {
        return playQueueOperations
                .getRelatedTracksUrns(seedTrack)
                .startWith(seedTrack)
                .toList()
                .flatMap(playTracks(startPosition, playSessionSource));
    }

    private Func1<List<Urn>, Observable<PlaybackResult>> playTracks(final int startPosition, final PlaySessionSource playSessionSource) {
        return new Func1<List<Urn>, Observable<PlaybackResult>>() {
            @Override
            public Observable<PlaybackResult> call(List<Urn> tracks) {
                return playTracks(tracks, startPosition, playSessionSource);
            }
        };
    }

    public Observable<PlaybackResult> playTracksShuffled(Observable<List<Urn>> trackUrnsObservable,
                                                    PlaySessionSource playSessionSource) {
        if (shouldDisableSkipping()) {
            return Observable.just(PlaybackResult.error(UNSKIPPABLE));
        } else {
            return trackUrnsObservable
                    .map(SHUFFLE_TRACKS)
                    .flatMap(playNewQueue(playbackStrategyProvider.get(), Urn.NOT_SET, 0, playSessionSource, WITHOUT_RELATED))
                    .observeOn(AndroidSchedulers.mainThread());
        }
    }

    private Observable<PlaybackResult> playTracksList(Observable<List<Urn>> trackUrns,
                                                      Urn initialTrack,
                                                      int startPosition,
                                                      PlaySessionSource playSessionSource,
                                                      boolean loadRelated) {
        if (!shouldChangePlayQueue(initialTrack, playSessionSource)) {
            return Observable.just(PlaybackResult.success());
        } else if (shouldDisableSkipping()) {
            return Observable.just(PlaybackResult.error(UNSKIPPABLE));
        } else {
            return trackUrns
                    .flatMap(playNewQueue(playbackStrategyProvider.get(), initialTrack, startPosition, playSessionSource, loadRelated))
                    .observeOn(AndroidSchedulers.mainThread());
        }
    }

    private Func1<List<Urn>, Observable<PlaybackResult>> playNewQueue(final PlaybackStrategy playbackStrategy,
                                                                      final Urn initialTrack,
                                                                      final int startPosition,
                                                                      final PlaySessionSource playSessionSource,
                                                                      final boolean loadRelated) {
        return new Func1<List<Urn>, Observable<PlaybackResult>>() {
            @Override
            public Observable<PlaybackResult> call(List<Urn> trackUrns) {
                if (trackUrns.isEmpty()) {
                    return Observable.just(PlaybackResult.error(MISSING_PLAYABLE_TRACKS));
                } else {
                    return playbackStrategy.playNewQueue(trackUrns, initialTrack, startPosition, loadRelated, playSessionSource);
                }
            }
        };
    }

    public Observable<PlaybackResult> startPlayback(PublicApiTrack track, Screen screen, boolean withRecommendations) {
        playQueueManager.clearAll();
        modelManager.cache(track);
        return playTracksList(Observable.just(track.getUrn()).toList(), track.getUrn(), 0, new PlaySessionSource(screen), withRecommendations);
    }

    public Observable<PlaybackResult> startPlaybackWithRecommendations(Urn urn, Screen screen, SearchQuerySourceInfo searchQuerySourceInfo) {
        PlaySessionSource playSessionSource = new PlaySessionSource(screen);
        playSessionSource.setSearchQuerySourceInfo(searchQuerySourceInfo);
        return playTracksList(Observable.just(urn).toList(), urn, 0, playSessionSource, WITH_RELATED);
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
        return !isCurrentTrack(trackUrn) || !isCurrentScreenSource(playSessionSource) || isPlaylist() && !isCurrentPlaylist(playSessionSource);
    }

    private boolean isCurrentPlaylist(PlaySessionSource playSessionSource) {
        return playQueueManager.isCurrentPlaylist(playSessionSource.getPlaylistUrn());
    }

    private boolean isPlaylist() {
        return playQueueManager.isPlaylist();
    }

    private boolean isCurrentScreenSource(PlaySessionSource playSessionSource) {
        return playSessionSource.getOriginScreen().equals(playQueueManager.getScreenTag());
    }

    private boolean isCurrentTrack(Urn trackUrn) {
        return playQueueManager.isCurrentTrack(trackUrn);
    }

}
