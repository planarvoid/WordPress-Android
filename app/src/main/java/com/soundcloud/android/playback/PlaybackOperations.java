package com.soundcloud.android.playback;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.soundcloud.android.ads.AdConstants;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.api.legacy.model.PublicApiPlaylist;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.api.legacy.model.ScModelManager;
import com.soundcloud.android.playback.service.PlayQueue;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.playback.service.PlaySessionSource;
import com.soundcloud.android.playback.service.PlaybackService;
import com.soundcloud.android.playback.ui.view.PlaybackToastViewController;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.storage.TrackStorage;
import com.soundcloud.android.tracks.TrackUrn;
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

// TODO, move to playback package level
public class PlaybackOperations {
    private static final long PROGRESS_THRESHOLD_FOR_TRACK_CHANGE = TimeUnit.SECONDS.toMillis(3L);
    private static final long SEEK_POSITION_RESET = 0L;

    private static final Func1<List<TrackUrn>, Boolean> FILTER_EMPTY_TRACK_LIST = new Func1<List<TrackUrn>, Boolean>() {
        @Override
        public Boolean call(List<TrackUrn> trackUrnList) {
            if (trackUrnList.isEmpty()) {
                ErrorUtils.handleSilentException(new IllegalStateException("Attempting to play a track on an empty track list"));
                return false;
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

    @Inject
    public PlaybackOperations(Context context, ScModelManager modelManager, TrackStorage trackStorage,
                              PlayQueueManager playQueueManager,
                              PlaySessionStateProvider playSessionStateProvider, PlaybackToastViewController playbackToastViewController) {
        this.context = context;
        this.modelManager = modelManager;
        this.trackStorage = trackStorage;
        this.playQueueManager = playQueueManager;
        this.playSessionStateProvider = playSessionStateProvider;
        this.playbackToastViewController = playbackToastViewController;
    }

    public void playTrack(TrackUrn track, PlaySessionSource playSessionSource) {
        playFromTrackUrnList(Lists.newArrayList(track), 0, track, playSessionSource, false);
    }

    public void playTrackWithRecommendations(TrackUrn track, PlaySessionSource playSessionSource) {
        playFromTrackUrnList(Lists.newArrayList(track), 0, track, playSessionSource, true);
    }

    public void playTracksShuffled(List<TrackUrn> trackUrns, PlaySessionSource playSessionSource) {
        List<TrackUrn> shuffled = Lists.newArrayList(trackUrns);
        Collections.shuffle(shuffled);
        playFromTrackUrnList(shuffled, 0, shuffled.get(0), playSessionSource);
    }

    public Observable<List<TrackUrn>> playTracks(Observable<TrackUrn> allTracks, TrackUrn initialTrack, int position, PlaySessionSource playSessionSource) {
        if (!shouldChangePlayQueue(initialTrack, playSessionSource)) {
            return Observable.empty();
        }

        return allTracks
                .toList()
                .filter(FILTER_EMPTY_TRACK_LIST)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(playNewQueueAction(position, playSessionSource, initialTrack));
    }

    @Deprecated
    public Observable<List<TrackUrn>> playFromUri(Uri uri, final int startPosition, final TrackUrn initialTrack,
                                    final PlaySessionSource playSessionSource) {
        if (!shouldChangePlayQueue(initialTrack, playSessionSource)) {
            return Observable.empty();
        }

        return trackStorage.getTracksForUriAsync(uri)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(playNewQueueAction(startPosition, playSessionSource, initialTrack));
    }

    public void playTracks(List<TrackUrn> trackUrns, int position, PlaySessionSource playSessionSource) {
        if (position >= trackUrns.size()) {
            throw new AssertionError("Invalid position is greater that the list size");
        }

        TrackUrn initialTrack = trackUrns.get(position);
        playFromTrackUrnList(trackUrns, position, initialTrack, playSessionSource);
    }

    public void playPlaylist(PublicApiPlaylist playlist, PlaySessionSource playSessionSource) {
        final List<TrackUrn> trackUrns = Lists.transform(playlist.getTracks(), new Function<PublicApiTrack, TrackUrn>() {
            @Override
            public TrackUrn apply(PublicApiTrack track) {
                return track.getUrn();
            }
        });
        startPlaySession(trackUrns, 0, playSessionSource);
    }

    public void startPlaybackWithRecommendations(PublicApiTrack track, Screen screen) {
        modelManager.cache(track);
        startPlaybackWithRecommendations(track.getUrn(), screen);
    }

    public void startPlaybackWithRecommendations(TrackUrn urn, Screen screen) {
        startPlaySession(Lists.newArrayList(urn), 0, new PlaySessionSource(screen), true);
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
        playQueueManager.setPosition(position);
    }

    public void previousTrack() {
        if (shouldDisableSkipping()) {
            playbackToastViewController.showUnkippableAdToast();
        } else {
            if (playSessionStateProvider.getLastProgressEvent().getPosition() >= PROGRESS_THRESHOLD_FOR_TRACK_CHANGE
                    && !playQueueManager.isCurrentTrackAudioAd()){
                seek(SEEK_POSITION_RESET);
            } else {
                playQueueManager.moveToPreviousTrack();
            }
        }
    }

    public void nextTrack() {
        if (shouldDisableSkipping()) {
            playbackToastViewController.showUnkippableAdToast();
        } else {
            playQueueManager.nextTrack();
        }
    }

    public void stopService() {
        context.startService(new Intent(PlaybackService.Actions.STOP_ACTION));
    }

    public void seek(long position) {
        if (!shouldDisableSkipping()){
            if (playSessionStateProvider.isPlayingCurrentPlayQueueTrack()) {
                Intent intent = new Intent(PlaybackService.Actions.SEEK);
                intent.putExtra(PlaybackService.ActionsExtras.SEEK_POSITION, position);
                context.startService(intent);
            } else {
                playQueueManager.saveCurrentProgress(position);
            }

        }
    }

    public boolean shouldDisableSkipping(){
        return playQueueManager.isCurrentTrackAudioAd() &&
                playSessionStateProvider.getCurrentPlayQueueTrackProgress().getPosition() < AdConstants.UNSKIPPABLE_TIME_MS;
    }

    private Action1<List<TrackUrn>> playNewQueueAction(final int startPosition,
                                                       final PlaySessionSource playSessionSource,
                                                       final TrackUrn initialTrackUrn) {
        return new Action1<List<TrackUrn>>() {
            @Override
            public void call(List<TrackUrn> trackUrns) {
                final int updatedPosition = correctStartPositionAndDeduplicateList(trackUrns, startPosition, initialTrackUrn);
                playNewQueue(trackUrns, updatedPosition, playSessionSource);
            }
        };
    }

    private void playFromTrackUrnList(List<TrackUrn> tracks, int startPosition, TrackUrn initialTrack,
                                      PlaySessionSource playSessionSource) {
        playFromTrackUrnList(tracks, startPosition, initialTrack, playSessionSource, false);
    }

    private void playFromTrackUrnList(List<TrackUrn> tracks, int startPosition, TrackUrn initialTrack,
                                      PlaySessionSource playSessionSource, boolean loadRelated) {
        if (shouldChangePlayQueue(initialTrack, playSessionSource)) {
            final int adjustedPosition = getDeduplicatedList(tracks, startPosition);
            startPlaySession(tracks, adjustedPosition, playSessionSource, loadRelated);
        }
    }

    private boolean shouldChangePlayQueue(TrackUrn trackUrn, PlaySessionSource playSessionSource) {
        return !isCurrentTrack(trackUrn) || !isCurrentScreenSource(playSessionSource) || isPlaylist() && !isCurrentPlaylist(playSessionSource);
    }

    private boolean isCurrentPlaylist(PlaySessionSource playSessionSource) {
        return playQueueManager.isCurrentPlaylist(playSessionSource.getPlaylistId());
    }

    private boolean isPlaylist() {
        return playQueueManager.isPlaylist();
    }

    private boolean isCurrentScreenSource(PlaySessionSource playSessionSource) {
        return playSessionSource.getOriginScreen().equals(playQueueManager.getScreenTag());
    }

    private boolean isCurrentTrack(TrackUrn trackUrn) {
        return playQueueManager.isCurrentTrack(trackUrn);
    }

    private void startPlaySession(final List<TrackUrn> trackList, int startPosition,
                                  PlaySessionSource playSessionSource) {
        startPlaySession(trackList, startPosition, playSessionSource, false);
    }

    private void startPlaySession(final List<TrackUrn> trackUrns, int startPosition,
                                  PlaySessionSource playSessionSource, boolean loadRecommended) {
        playNewQueue(trackUrns, startPosition, playSessionSource);
        if (loadRecommended) {
            playQueueManager.fetchTracksRelatedToCurrentTrack();
        }
    }

    private void playNewQueue(List<TrackUrn> trackUrns, int startPosition, PlaySessionSource playSessionSource) {
        if (shouldDisableSkipping()) {
            playbackToastViewController.showUnkippableAdToast();
        } else {
            final PlayQueue playQueue = PlayQueue.fromTrackUrnList(trackUrns, playSessionSource);
            playQueueManager.setNewPlayQueue(playQueue, startPosition, playSessionSource);
            playCurrent();
        }
    }

    private int correctStartPositionAndDeduplicateList(List<TrackUrn> trackUrns, int startPosition, TrackUrn initialTrack) {
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
    private int getDeduplicatedList(List<TrackUrn> trackUrns, int startPosition) {
        final Set<TrackUrn> seenTracks = Sets.newHashSetWithExpectedSize(trackUrns.size());
        final TrackUrn playedTrack = trackUrns.get(startPosition);

        int i = 0;
        Iterator<TrackUrn> iterator = trackUrns.iterator();
        int adjustedPosition = startPosition;
        while (iterator.hasNext()) {
            final TrackUrn track = iterator.next();
            if (i != adjustedPosition && (seenTracks.contains(track) || track.equals(playedTrack))) {
                iterator.remove();
                if (i < adjustedPosition) adjustedPosition--;
            } else {
                seenTracks.add(track);
                i++;
            }
        }
        return adjustedPosition;
    }
}
