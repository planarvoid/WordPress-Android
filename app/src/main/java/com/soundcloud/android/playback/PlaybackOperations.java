package com.soundcloud.android.playback;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.soundcloud.android.ads.AdConstants;
import com.soundcloud.android.ads.AdsOperations;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.api.legacy.model.ScModelManager;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.service.PlayQueue;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.playback.service.PlaySessionSource;
import com.soundcloud.android.playback.service.PlaybackService;
import com.soundcloud.android.playback.ui.view.PlaybackToastViewController;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.storage.TrackStorage;
import com.soundcloud.android.utils.ErrorUtils;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import javax.inject.Inject;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
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

    private final Context context;
    private final ScModelManager modelManager;
    private final TrackStorage trackStorage;
    private final PlayQueueManager playQueueManager;
    private final PlaySessionStateProvider playSessionStateProvider;
    private final PlaybackToastViewController playbackToastViewController;
    private final EventBus eventBus;
    private final AdsOperations adsOperations;

    @Inject
    public PlaybackOperations(Context context, ScModelManager modelManager, TrackStorage trackStorage,
                              PlayQueueManager playQueueManager,
                              PlaySessionStateProvider playSessionStateProvider,
                              PlaybackToastViewController playbackToastViewController, EventBus eventBus,
                              AdsOperations adsOperations) {
        this.context = context;
        this.modelManager = modelManager;
        this.trackStorage = trackStorage;
        this.playQueueManager = playQueueManager;
        this.playSessionStateProvider = playSessionStateProvider;
        this.playbackToastViewController = playbackToastViewController;
        this.eventBus = eventBus;
        this.adsOperations = adsOperations;
    }

    public Observable<List<Urn>> playTracks(List<Urn> trackUrns, int position, PlaySessionSource playSessionSource) {
        return playTracksList(Observable.from(trackUrns).toList(), trackUrns.get(position), position, playSessionSource, false);
    }

    public Observable<List<Urn>> playTracks(Observable<Urn> allTracks, Urn initialTrack, int position, PlaySessionSource playSessionSource) {
        return playTracksList(allTracks.toList(), initialTrack, position, playSessionSource, false);
    }

    @Deprecated
    public Observable<List<Urn>> playTracksFromUri(Uri uri, final int startPosition, final Urn initialTrack, final PlaySessionSource playSessionSource) {
        return playTracksList(trackStorage.getTracksForUriAsync(uri), initialTrack, startPosition, playSessionSource, false);
    }

    public Observable<List<Urn>> playTrackWithRecommendations(Urn track, PlaySessionSource playSessionSource) {
        return playTracksList(Observable.from(track).toList(), track, 0, playSessionSource, true);
    }

    public Observable<List<Urn>> playTracksShuffled(List<Urn> trackUrns, PlaySessionSource playSessionSource) {
        List<Urn> shuffled = Lists.newArrayList(trackUrns);
        Collections.shuffle(shuffled);
        return playTracksList(Observable.from(shuffled).toList(), shuffled.get(0), 0, playSessionSource, true);
    }

    private Observable<List<Urn>> playTracksList(Observable<List<Urn>> trackUrns, final Urn initialTrack, final int startPosition, final PlaySessionSource playSessionSource, boolean loadRelated) {
        if (!shouldChangePlayQueue(initialTrack, playSessionSource)) {
            return Observable.empty();
        }

        return trackUrns
                .filter(FILTER_EMPTY_TRACK_LIST)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(playNewQueueAction(startPosition, playSessionSource, initialTrack, loadRelated));
    }

    public Observable<List<Urn>> startPlaybackWithRecommendations(PublicApiTrack track, Screen screen) {
        modelManager.cache(track);
        return playTracksList(Observable.from(track.getUrn()).toList(), track.getUrn(), 0, new PlaySessionSource(screen), true);
    }

    public Observable<List<Urn>> startPlaybackWithRecommendations(Urn urn, Screen screen) {
        return playTracksList(Observable.from(urn).toList(), urn, 0, new PlaySessionSource(screen), true);
    }

    public void togglePlayback() {
        if (playSessionStateProvider.isPlayingCurrentPlayQueueTrack()) {
            context.startService(new Intent(PlaybackService.Actions.TOGGLEPLAYBACK_ACTION));
        } else {
            playCurrent();
        }
    }

    public void playCurrent() {
        context.startService(new Intent(PlaybackService.Actions.PLAY_CURRENT));
    }

    public void setPlayQueuePosition(int position) {
        publishSkipEventIfAudioAd();
        playQueueManager.setPosition(position);
    }

    public void previousTrack() {
        if (shouldDisableSkipping()) {
            playbackToastViewController.showUnkippableAdToast();
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
            playbackToastViewController.showUnkippableAdToast();
        } else {
            publishSkipEventIfAudioAd();
            playQueueManager.nextTrack();
        }
    }

    private void publishSkipEventIfAudioAd() {
        if (adsOperations.isCurrentTrackAudioAd()) {
            final UIEvent event = UIEvent.fromSkipAudioAdClick(playQueueManager.getCurrentMetaData(), playQueueManager.getCurrentTrackUrn());
            eventBus.publish(EventQueue.UI_TRACKING, event);
        }
    }

    public void stopService() {
        context.startService(new Intent(PlaybackService.Actions.STOP_ACTION));
    }

    public void seek(long position) {
        if (!shouldDisableSkipping()) {
            if (playSessionStateProvider.isPlayingCurrentPlayQueueTrack()) {
                Intent intent = new Intent(PlaybackService.Actions.SEEK);
                intent.putExtra(PlaybackService.ActionsExtras.SEEK_POSITION, position);
                context.startService(intent);
            } else {
                playQueueManager.saveCurrentProgress(position);
            }

        }
    }

    public boolean shouldDisableSkipping() {
        return adsOperations.isCurrentTrackAudioAd() &&
                playSessionStateProvider.getCurrentPlayQueueTrackProgress().getPosition() < AdConstants.UNSKIPPABLE_TIME_MS;
    }

    private Action1<List<Urn>> playNewQueueAction(final int startPosition,
                                                       final PlaySessionSource playSessionSource,
                                                       final Urn initialTrackUrn,
                                                       final boolean loadRecommended) {
        return new Action1<List<Urn>>() {
            @Override
            public void call(List<Urn> trackUrns) {
                final int updatedPosition = correctStartPositionAndDeduplicateList(trackUrns, startPosition, initialTrackUrn);
                playNewQueue(trackUrns, updatedPosition, playSessionSource);
                if (loadRecommended) {
                    playQueueManager.fetchTracksRelatedToCurrentTrack();
                }
            }
        };
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

    private void playNewQueue(List<Urn> trackUrns, int startPosition, PlaySessionSource playSessionSource) {
        if (shouldDisableSkipping()) {
            throw new UnSkippablePeriodException();
        }

        final PlayQueue playQueue = PlayQueue.fromTrackUrnList(trackUrns, playSessionSource);
        playQueueManager.setNewPlayQueue(playQueue, startPosition, playSessionSource);
        playCurrent();
    }

    private int correctStartPositionAndDeduplicateList(List<Urn> trackUrns, int startPosition, Urn initialTrack) {
        int updatedPosition;
        if (startPosition < trackUrns.size() && trackUrns.get(startPosition).equals(initialTrack)) {
            updatedPosition = startPosition;
        } else {
            updatedPosition = trackUrns.indexOf(initialTrack);
        }

        if (updatedPosition < 0) {
            ErrorUtils.handleSilentException(new IllegalStateException("Attempting to play an adapter track that's not in the list"));
            updatedPosition = 0;
        }

        return getDeduplicatedList(trackUrns, updatedPosition);
    }

    /**
     * Remove duplicates from playqueue, preserving the ordering with regards to the item they clicked on
     * Returns the new startPosition
     */
    private int getDeduplicatedList(List<Urn> trackUrns, int startPosition) {
        final Set<Urn> seenTracks = Sets.newHashSetWithExpectedSize(trackUrns.size());
        final Urn playedTrack = trackUrns.get(startPosition);

        int i = 0;
        Iterator<Urn> iterator = trackUrns.iterator();
        int adjustedPosition = startPosition;
        while (iterator.hasNext()) {
            final Urn track = iterator.next();
            if (i != adjustedPosition && (seenTracks.contains(track) || track.equals(playedTrack))) {
                iterator.remove();
                if (i < adjustedPosition) {
                    adjustedPosition--;
                }
            } else {
                seenTracks.add(track);
                i++;
            }
        }
        return adjustedPosition;
    }

    public static class UnSkippablePeriodException extends RuntimeException {
    }
}
