package com.soundcloud.android.playback;

import com.google.common.collect.Lists;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.ads.AdConstants;
import com.soundcloud.android.ads.AdsOperations;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.api.legacy.model.ScModelManager;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.playback.service.PlaySessionSource;
import com.soundcloud.android.playback.service.PlaybackService;
import com.soundcloud.android.playback.ui.view.PlaybackToastHelper;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.storage.TrackStorage;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class PlaybackOperations {
    private static final long PROGRESS_THRESHOLD_FOR_TRACK_CHANGE = TimeUnit.SECONDS.toMillis(3L);
    private static final long SEEK_POSITION_RESET = 0L;

    private static final Func1<List<Urn>, Boolean> FILTER_EMPTY_TRACK_LIST = new Func1<List<Urn>, Boolean>() {
        @Override
        public Boolean call(List<Urn> trackUrnList) {
            if (trackUrnList.isEmpty()) {
                throw new IllegalStateException("Attempting to play a track on an empty track list");
            } else {
                return true;
            }
        }
    };

    private static final Func1<List<Urn>, List<Urn>> SHUFFLE_TRACKS = new Func1<List<Urn>, List<Urn>>() {
        @Override
        public List<Urn> call(List<Urn> trackUrns) {
            List<Urn> shuffled = Lists.newArrayList(trackUrns);
            Collections.shuffle(shuffled);
            return shuffled;
        }
    };

    private final Context context;
    private final ScModelManager modelManager;
    private final TrackStorage trackStorage;
    private final PlayQueueManager playQueueManager;
    private final PlaySessionStateProvider playSessionStateProvider;
    private final PlaybackToastHelper playbackToastHelper;
    private final EventBus eventBus;
    private final AdsOperations adsOperations;
    private final AccountOperations accountOperations;
    private final Provider<PlaybackStrategy> playbackStrategyProvider;


    @Inject
    public PlaybackOperations(Context context, ScModelManager modelManager, TrackStorage trackStorage,
                              PlayQueueManager playQueueManager,
                              PlaySessionStateProvider playSessionStateProvider,
                              PlaybackToastHelper playbackToastHelper, EventBus eventBus,
                              AdsOperations adsOperations, AccountOperations accountOperations,
                              Provider<PlaybackStrategy> playbackStrategyProvider) {
        this.context = context;
        this.modelManager = modelManager;
        this.trackStorage = trackStorage;
        this.playQueueManager = playQueueManager;
        this.playSessionStateProvider = playSessionStateProvider;
        this.playbackToastHelper = playbackToastHelper;
        this.eventBus = eventBus;
        this.adsOperations = adsOperations;
        this.accountOperations = accountOperations;
        this.playbackStrategyProvider = playbackStrategyProvider;
    }

    public Observable<List<Urn>> playTracks(List<Urn> trackUrns, int position, PlaySessionSource playSessionSource) {
        return playTracks(trackUrns, trackUrns.get(position), position, playSessionSource);
    }

    public Observable<List<Urn>> playTracks(List<Urn> trackUrns, Urn trackUrn, int position,
                                            PlaySessionSource playSessionSource) {
        return playTracksList(Observable.from(trackUrns).toList(), trackUrn, position, playSessionSource, false);
    }

    public Observable<List<Urn>> playTracks(Observable<List<Urn>> allTracks, Urn initialTrack, int position,
                                            PlaySessionSource playSessionSource) {
        return playTracksList(allTracks, initialTrack, position, playSessionSource, false);
    }

    @Deprecated
    public Observable<List<Urn>> playTracksFromUri(Uri uri, final int startPosition, final Urn initialTrack,
                                                   final PlaySessionSource playSessionSource) {
        return playTracksList(trackStorage.getTracksForUriAsync(uri), initialTrack, startPosition,
                playSessionSource, false);
    }

    public Observable<List<Urn>> playTrackWithRecommendations(Urn track, PlaySessionSource playSessionSource) {
        return playTracksList(Observable.just(track).toList(), track, 0, playSessionSource, true);
    }

    public Observable<List<Urn>> playTracksShuffled(List<Urn> trackUrns, PlaySessionSource playSessionSource) {
        List<Urn> shuffled = Lists.newArrayList(trackUrns);
        Collections.shuffle(shuffled);
        return playTracksList(Observable.from(shuffled).toList(), shuffled.get(0), 0, playSessionSource, false);
    }

    public Observable<List<Urn>> playTracksShuffled(Observable<List<Urn>> trackUrnsObservable,
                                                    PlaySessionSource playSessionSource) {
        return trackUrnsObservable
                .filter(FILTER_EMPTY_TRACK_LIST)
                .map(SHUFFLE_TRACKS)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(playNewQueue(0, playSessionSource, Urn.NOT_SET, false));
    }

    public void reloadAndPlayCurrentQueue(long fromLastProgressPosition) {
        playbackStrategyProvider.get().reloadAndPlayCurrentQueue(fromLastProgressPosition);
    }

    private Observable<List<Urn>> playTracksList(Observable<List<Urn>> trackUrns, final Urn initialTrack,
                                                 final int startPosition, final PlaySessionSource playSessionSource,
                                                 boolean loadRelated) {
        if (!shouldChangePlayQueue(initialTrack, playSessionSource)) {
            return Observable.empty();
        }

        return trackUrns
                .filter(FILTER_EMPTY_TRACK_LIST)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(playNewQueue(startPosition, playSessionSource, initialTrack, loadRelated));
    }

    private Action1<List<Urn>> playNewQueue(final int initialTrackPosition,
                                            final PlaySessionSource playSessionSource,
                                            final Urn initialTrackUrn,
                                            final boolean loadRecommended) {
        return new Action1<List<Urn>>() {
            @Override
            public void call(List<Urn> trackUrns) {
                if (shouldDisableSkipping()) {
                    throw new UnskippablePeriodException();
                }
                playbackStrategyProvider.get().playNewQueue(trackUrns, initialTrackUrn, initialTrackPosition, playSessionSource);
                if (loadRecommended) {
                    playQueueManager.fetchTracksRelatedToCurrentTrack();
                }
            }
        };
    }

    public Observable<List<Urn>> startPlaybackWithRecommendations(PublicApiTrack track, Screen screen) {
        modelManager.cache(track);
        return playTracksList(Observable.just(track.getUrn()).toList(), track.getUrn(), 0, new PlaySessionSource(screen), true);
    }

    public Observable<List<Urn>> startPlaybackWithRecommendations(Urn urn, Screen screen, SearchQuerySourceInfo searchQuerySourceInfo) {
        PlaySessionSource playSessionSource = new PlaySessionSource(screen);
        playSessionSource.setSearchQuerySourceInfo(searchQuerySourceInfo);
        return playTracksList(Observable.just(urn).toList(), urn, 0, playSessionSource, true);
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

    public void playCurrent() {
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
        context.startService(createExplicitServiceIntent(PlaybackService.Actions.STOP_ACTION));
    }

    public void resetService() {
        context.startService(createExplicitServiceIntent(PlaybackService.Actions.RESET_ALL));
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

    private Intent createExplicitServiceIntent(String action) {
        Intent intent = new Intent(context, PlaybackService.class);
        intent.setAction(action);
        return intent;
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

    public static class UnskippablePeriodException extends RuntimeException {}
}
