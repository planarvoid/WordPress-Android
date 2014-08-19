package com.soundcloud.android.playback;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.soundcloud.android.ads.AdConstants;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.api.legacy.model.Playable;
import com.soundcloud.android.api.legacy.model.PublicApiPlaylist;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.api.legacy.model.ScModelManager;
import com.soundcloud.android.api.legacy.model.behavior.PlayableHolder;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.ScModel;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.service.PlayQueue;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.playback.service.PlaySessionSource;
import com.soundcloud.android.playback.service.PlaybackService;
import com.soundcloud.android.playback.ui.view.AdToastViewController;
import com.soundcloud.android.playlists.PlaylistDetailActivity;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.storage.TrackStorage;
import com.soundcloud.android.tracks.TrackUrn;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.propeller.PropertySet;
import org.jetbrains.annotations.Nullable;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.subscriptions.Subscriptions;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

// TODO, move to playback package level
public class PlaybackOperations {
    private static final long PROGRESS_THRESHOLD_FOR_TRACK_CHANGE = TimeUnit.SECONDS.toMillis(3L);

    private static final Predicate<ScModel> PLAYABLE_HOLDER_PREDICATE = new Predicate<ScModel>() {
        @Override
        public boolean apply(ScModel input) {
            return input instanceof PlayableHolder &&
                    ((PlayableHolder) input).getPlayable() instanceof PublicApiTrack;
        }
    };

    private static final long SEEK_POSITION_RESET = 0L;

    private final Context context;
    private final ScModelManager modelManager;
    private final TrackStorage trackStorage;
    private final PlayQueueManager playQueueManager;
    private final EventBus eventBus;
    private final PlaySessionStateProvider playSessionStateProvider;
    private final AdToastViewController adToastViewController;

    @Inject
    public PlaybackOperations(Context context, ScModelManager modelManager, TrackStorage trackStorage,
                              PlayQueueManager playQueueManager, EventBus eventBus,
                              PlaySessionStateProvider playSessionStateProvider, AdToastViewController adToastViewController) {
        this.context = context;
        this.modelManager = modelManager;
        this.trackStorage = trackStorage;
        this.playQueueManager = playQueueManager;
        this.eventBus = eventBus;
        this.playSessionStateProvider = playSessionStateProvider;
        this.adToastViewController = adToastViewController;
    }

    /**
     * Single play, the tracklist will be of length 1
     */
    public void playTrack(PublicApiTrack track, Screen screen) {
        playFromIdList(Lists.newArrayList(track.getId()), 0, track, new PlaySessionSource(screen));
    }

    private void playTrack(PublicApiTrack track, PlaySessionSource playSessionSource, boolean loadRelated) {
        playFromIdList(Lists.newArrayList(track.getId()), 0, track, playSessionSource, loadRelated);
    }

    /**
     * Created by anything played from the {@link com.soundcloud.android.explore.ExploreFragment} section.
     */
    public void playExploreTrack(PublicApiTrack track, String exploreTag, String screenTag) {
        final PlaySessionSource playSessionSource = new PlaySessionSource(screenTag);
        playSessionSource.setExploreVersion(exploreTag);
        playTrack(track, playSessionSource, true);
    }

    /**
     * From a urn with an initial track to show while loading the full playlist from the DB.
     * Used in {@link com.soundcloud.android.playlists.PlaylistFragment}
     */
    public void playPlaylistFromPosition(PropertySet playlist, Observable<TrackUrn> allTracks, TrackUrn initialTrackUrn, int startPosition, Screen screen) {
        final PlaySessionSource playSessionSource = new PlaySessionSource(screen.get());
        playSessionSource.setPlaylist(playlist.get(PlayableProperty.URN).numericId, playlist.get(PlayableProperty.CREATOR_URN).numericId);
        playTracks(initialTrackUrn, allTracks, startPosition, playSessionSource);
    }

    public void playPlaylist(PublicApiPlaylist playlist, Screen screen) {
        final PlaySessionSource playSessionSource = new PlaySessionSource(screen.get());
        playSessionSource.setPlaylist(playlist.getId(), playlist.getUserId());
        final List<Long> trackIds = Lists.transform(playlist.getTracks(), new Function<PublicApiTrack, Long>() {
            @Override
            public Long apply(PublicApiTrack track) {
                return track.getId();
            }
        });
        startPlaySession(trackIds, 0, playSessionSource);
        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.forShowPlayer());
    }

    @Deprecated
    public void playFromAdapter(Context activityContext, List<? extends ScModel> data, int position, Uri uri, Screen screen) {
        if (position >= data.size() || !(data.get(position) instanceof PlayableHolder)) {
            throw new AssertionError("Invalid item " + position + ", must be a playable");
        }

        Playable playable = ((PlayableHolder) data.get(position)).getPlayable();
        if (playable instanceof PublicApiTrack) {

            final PlaySessionSource playSessionSource = new PlaySessionSource(screen);
            final int adjustedPosition = Collections2.filter(data.subList(0, position), PLAYABLE_HOLDER_PREDICATE).size();

            if (uri != null) {
                playFromUri(uri, position, (PublicApiTrack) playable, playSessionSource);
            } else {
                playFromIdList(getPlayableIdsFromModels(data), adjustedPosition, (PublicApiTrack) playable, playSessionSource);
            }

        } else if (playable instanceof PublicApiPlaylist) {
            PlaylistDetailActivity.start(activityContext, (PublicApiPlaylist) playable, modelManager, screen);
        } else {
            throw new AssertionError("Unexpected playable type");
        }
    }

    public Subscription playTracks(TrackUrn initialTrack, Observable<TrackUrn> allTracks, int position, Screen screen) {
        return playTracks(initialTrack, allTracks, position, new PlaySessionSource(screen));
    }

    private Subscription playTracks(TrackUrn initialTrack, Observable<TrackUrn> allTracks, int position, PlaySessionSource playSessionSource) {
        if (shouldChangePlayQueue(initialTrack, playSessionSource)) {
            return allTracks.map(new Func1<TrackUrn, Long>() {
                @Override
                public Long call(TrackUrn trackUrn) {
                    return trackUrn.numericId;
                }
            }).toList().observeOn(AndroidSchedulers.mainThread())
                    .subscribe(trackListLoadedSubscriber(position, playSessionSource, initialTrack));
        } else {
            expandPlayer();
            return Subscriptions.empty();
        }
    }

    public void startPlaybackWithRecommendations(PublicApiTrack track, Screen screen) {
        modelManager.cache(track);
        startPlaybackWithRecommendations(track.getUrn(), screen);
    }

    public void startPlaybackWithRecommendations(TrackUrn urn, Screen screen) {
        startPlaySession(Lists.newArrayList(urn.numericId), 0, new PlaySessionSource(screen), true);
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
            adToastViewController.showUnkippableAdToast();
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
            adToastViewController.showUnkippableAdToast();
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

    public void playFromIdListShuffled(List<Long> ids, Screen screen) {
        List<Long> shuffled = Lists.newArrayList(ids);
        Collections.shuffle(shuffled);
        startPlaySession(shuffled, 0, new PlaySessionSource(screen));
    }

    private ArrayList<Long> getPlayableIdsFromModels(List<? extends ScModel> data) {
        final Iterable<? extends ScModel> playables = Iterables.filter(data, PLAYABLE_HOLDER_PREDICATE);
        Iterable<Long> trackIds = Iterables.transform(playables, new Function<ScModel, Long>() {
            @Override
            public Long apply(ScModel input) {
                return ((PlayableHolder) input).getPlayable().getId();
            }
        });
        return Lists.newArrayList(trackIds);
    }

    @Deprecated
    private Subscription playFromUri(Uri uri, final int startPosition, final PublicApiTrack initialTrack,
                                     final PlaySessionSource playSessionSource) {
        if (shouldChangePlayQueue(initialTrack.getUrn(), playSessionSource)) {
            return trackStorage.getTrackIdsForUriAsync(uri)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(trackListLoadedSubscriber(startPosition, playSessionSource, initialTrack.getUrn()));
        } else {
            expandPlayer();
            return Subscriptions.empty();
        }
    }

    private Subscriber<List<Long>> trackListLoadedSubscriber(final int startPosition,
                                                             final PlaySessionSource playSessionSource,
                                                             final TrackUrn initialTrackUrn) {
        return new DefaultSubscriber<List<Long>>() {
            @Override
            public void onNext(List<Long> idList) {
                final int updatedPosition = correctStartPositionAndDeduplicateList(idList, startPosition, initialTrackUrn);
                playNewQueue(idList, updatedPosition, playSessionSource);
            }
        };
    }

    private void playFromIdList(List<Long> idList, int startPosition, PublicApiTrack initialTrack,
                                PlaySessionSource playSessionSource) {
        playFromIdList(idList, startPosition, initialTrack, playSessionSource, false);
    }

    private void playFromIdList(List<Long> idList, int startPosition, PublicApiTrack initialTrack,
                                PlaySessionSource playSessionSource, boolean loadRelated) {
        if (shouldChangePlayQueue(initialTrack.getUrn(), playSessionSource)) {
            final int adjustedPosition = getDeduplicatedIdList(idList, startPosition);
            startPlaySession(idList, adjustedPosition, playSessionSource, loadRelated);
        } else {
            expandPlayer();
        }
    }

    private boolean shouldChangePlayQueue(TrackUrn trackUrn, PlaySessionSource playSessionSource) {
        return !playQueueManager.isCurrentTrack(trackUrn) ||
                !playSessionSource.getOriginScreen().equals(playQueueManager.getScreenTag()) ||
                (playQueueManager.isPlaylist() && !playQueueManager.isCurrentPlaylist(playSessionSource.getPlaylistId()));
    }

    private void startPlaySession(final List<Long> trackList, int startPosition,
                                  PlaySessionSource playSessionSource) {
        startPlaySession(trackList, startPosition, playSessionSource, false);
    }

    private void startPlaySession(final List<Long> trackList, int startPosition,
                                  PlaySessionSource playSessionSource, boolean loadRecommended) {
        playNewQueue(trackList, startPosition, playSessionSource);
        if (loadRecommended) {
            playQueueManager.fetchTracksRelatedToCurrentTrack();
        }
    }

    private void playNewQueue(List<Long> trackIdList, int startPosition, PlaySessionSource playSessionSource) {
        if (shouldDisableSkipping()) {
            adToastViewController.showUnkippableAdToast();
        } else {
            final PlayQueue playQueue = PlayQueue.fromIdList(trackIdList, playSessionSource);
            playQueueManager.setNewPlayQueue(playQueue, startPosition, playSessionSource);
            playCurrent();
        }
    }

    private int correctStartPositionAndDeduplicateList(List<Long> idList, int startPosition, TrackUrn initialTrack) {
        int updatedPosition;
        if (startPosition < idList.size() && idList.get(startPosition) == initialTrack.numericId) {
            updatedPosition = startPosition;
        } else {
            updatedPosition = idList.indexOf(initialTrack.numericId);
        }

        if (updatedPosition < 0) {
            ErrorUtils.handleSilentException(new IllegalStateException("Attempting to play an adapter track that's not in the list"));
            updatedPosition = 0;
        }

        return getDeduplicatedIdList(idList, updatedPosition);
    }

    private void expandPlayer() {
        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.forExpandPlayer());
    }

    /**
     * Remove duplicates from playqueue, preserving the ordering with regards to the item they clicked on
     * Returns the new startPosition
     */
    private int getDeduplicatedIdList(List<Long> idList, int startPosition) {
        final Set<Long> seenIds = Sets.newHashSetWithExpectedSize(idList.size());
        final long playedId = idList.get(startPosition);

        int i = 0;
        Iterator<Long> iter = idList.iterator();
        while (iter.hasNext()) {
            final long val = iter.next().longValue();
            if (i != startPosition && (seenIds.contains(val) || val == playedId)) {
                iter.remove();
                if (i < startPosition) startPosition--;
            } else {
                seenIds.add(val);
                i++;
            }
        }
        return startPosition;
    }

    /**
     * Legacy Player navigation functions.
     */

    @Deprecated
    @Nullable
    public Intent getServiceBasedUpIntent() {
        final String originScreen = playQueueManager.getScreenTag();
        if (ScTextUtils.isBlank(originScreen)) {
            return null; // might have come from widget and the play queue is empty
        }

        if (playQueueManager.isPlaylist()) {
            return getUpDestinationFromPlaylist(playQueueManager.getPlaylistId(), originScreen);
        } else {
            return Screen.getUpDestinationFromScreenTag(originScreen);
        }
    }

    @Deprecated
    private Intent getUpDestinationFromPlaylist(long playlistId, String originScreen) {
        final Screen screen = Screen.fromScreenTag(originScreen);
        final Intent upIntent = PlaylistDetailActivity.getIntent(Urn.forPlaylist(playlistId), screen);
        upIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return upIntent;
    }
}
