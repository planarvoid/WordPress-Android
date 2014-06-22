package com.soundcloud.android.playback;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.soundcloud.android.Actions;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.ScModel;
import com.soundcloud.android.model.ScModelManager;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.TrackUrn;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.model.behavior.PlayableHolder;
import com.soundcloud.android.playback.service.PlayQueue;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.playback.service.PlaySessionSource;
import com.soundcloud.android.playback.service.PlaybackService;
import com.soundcloud.android.playlists.PlaylistDetailActivity;
import com.soundcloud.android.properties.Feature;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.storage.TrackStorage;
import com.soundcloud.android.utils.ScTextUtils;
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

// TODO, move to playback package level
public class PlaybackOperations {

    private static final Predicate<ScModel> PLAYABLE_HOLDER_PREDICATE = new Predicate<ScModel>() {
        @Override
        public boolean apply(ScModel input) {
            return input instanceof PlayableHolder &&
                    ((PlayableHolder) input).getPlayable() instanceof Track;
        }
    };

    private final Context context;
    private final ScModelManager modelManager;
    private final TrackStorage trackStorage;
    private final PlayQueueManager playQueueManager;
    private final FeatureFlags featureFlags;
    private final EventBus eventBus;

    @Inject
    public PlaybackOperations(Context context, ScModelManager modelManager, TrackStorage trackStorage, PlayQueueManager playQueueManager,
                              FeatureFlags featureFlags, EventBus eventBus) {
        this.context = context;
        this.modelManager = modelManager;
        this.trackStorage = trackStorage;
        this.playQueueManager = playQueueManager;
        this.featureFlags = featureFlags;
        this.eventBus = eventBus;
    }

    /**
     * Single play, the tracklist will be of length 1
     */
    public void playTrack(Context activityContext, Track track, Screen screen) {
        playFromIdList(activityContext, Lists.newArrayList(track.getId()), 0, track, new PlaySessionSource(screen));
    }

    private void playTrack(Context activityContext, Track track, PlaySessionSource playSessionSource, boolean loadRelated) {
        playFromIdList(activityContext, Lists.newArrayList(track.getId()), 0, track, playSessionSource, loadRelated);
    }

    /**
     * Created by anything played from the {@link com.soundcloud.android.explore.ExploreFragment} section.
     */
    public void playExploreTrack(Context activityContext, Track track, String exploreTag, String screenTag) {
        final PlaySessionSource playSessionSource = new PlaySessionSource(screenTag);
        playSessionSource.setExploreVersion(exploreTag);
        playTrack(activityContext, track, playSessionSource, true);
    }

    /**
     * From a uri with an initial track to show while loading the full playlist from the DB.
     * Used in {@link com.soundcloud.android.playlists.PlaylistFragment}
     */
    public void playPlaylistFromPosition(Context activityContext, Playlist playlist, int startPosition, Track initialTrack, Screen screen) {
        final PlaySessionSource playSessionSource = new PlaySessionSource(screen.get());
        playSessionSource.setPlaylist(playlist);
        playFromUri(activityContext, playlist.toUri(), startPosition, initialTrack, playSessionSource);
    }

    public void playPlaylist(Playlist playlist, Screen screen) {
        final PlaySessionSource playSessionSource = new PlaySessionSource(screen.get());
        playSessionSource.setPlaylist(playlist);
        final List<Long> trackIds = Lists.transform(playlist.getTracks(), new Function<Track, Long>() {
            @Override
            public Long apply(Track track) {
                return track.getId();
            }
        });
        startPlaySession(trackIds, 0, playSessionSource);
    }

    @Deprecated
    public void playFromAdapter(Context activityContext, List<? extends ScModel> data, int position, Uri uri, Screen screen) {
        if (position >= data.size() || !(data.get(position) instanceof PlayableHolder)) {
            throw new AssertionError("Invalid item " + position + ", must be a playable");
        }

        Playable playable = ((PlayableHolder) data.get(position)).getPlayable();
        if (playable instanceof Track) {

            final PlaySessionSource playSessionSource = new PlaySessionSource(screen);
            final int adjustedPosition = Collections2.filter(data.subList(0, position), PLAYABLE_HOLDER_PREDICATE).size();

            if (uri != null) {
                playFromUri(activityContext, uri, position, (Track) playable, playSessionSource);
            } else {
                playFromIdList(activityContext, getPlayableIdsFromModels(data), adjustedPosition, (Track) playable, playSessionSource);
            }

        } else if (playable instanceof Playlist) {
            PlaylistDetailActivity.start(activityContext, (Playlist) playable, modelManager, screen);
        } else {
            throw new AssertionError("Unexpected playable type");
        }
    }

    public Subscription playTracks(Context context, TrackUrn initialTrack, Observable<TrackUrn> allTracks, int position, Screen screen) {
        final PlaySessionSource playSessionSource = new PlaySessionSource(screen);
        if (shouldChangePlayQueue(initialTrack, playSessionSource)) {
            return allTracks.map(new Func1<TrackUrn, Long>() {
                @Override
                public Long call(TrackUrn trackUrn) {
                    return trackUrn.numericId;
                }
            }).toList().observeOn(AndroidSchedulers.mainThread())
                    .subscribe(trackListLoadedSubscriber(context, position, playSessionSource, initialTrack, null));
        } else {
            showPlayer(context, initialTrack);
            return Subscriptions.empty();
        }
    }

    public void startPlaybackWithRecommendations(Track track, Screen screen) {
        modelManager.cache(track);
        startPlaybackWithRecommendations(track.getId(), screen);
    }

    public void startPlaybackWithRecommendations(long id, Screen screen) {
        startPlaySession(Lists.newArrayList(id), 0, new PlaySessionSource(screen), true);
    }

    public void togglePlayback() {
        context.startService(new Intent(PlaybackService.Actions.TOGGLEPLAYBACK_ACTION));
    }

    public void playCurrent() {
        context.startService(new Intent(PlaybackService.Actions.PLAY_CURRENT));
    }

    public void setPlayQueuePosition(int position) {
        playQueueManager.setPosition(position);
    }

    public void previousTrack() {
        playQueueManager.previousTrack();
    }

    public void nextTrack() {
        playQueueManager.nextTrack();
    }

    public void playFromIdListShuffled(final Context activityContext, List<Long> ids, Screen screen) {
        List<Long> shuffled = Lists.newArrayList(ids);
        Collections.shuffle(shuffled);
        startPlaySession(shuffled, 0, new PlaySessionSource(screen));
        openLegacyPlayer(activityContext, shuffled.get(0));
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
    private Subscription playFromUri(final Context activityContext, Uri uri, final int startPosition, final Track initialTrack,
                                     final PlaySessionSource playSessionSource) {
        if (shouldChangePlayQueue(initialTrack.getUrn(), playSessionSource)) {
            return trackStorage.getTrackIdsForUriAsync(uri)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(trackListLoadedSubscriber(
                            activityContext, startPosition, playSessionSource, initialTrack.getUrn(), initialTrack));
        } else {
            showPlayer(activityContext, initialTrack.getUrn(), initialTrack);
            return Subscriptions.empty();
        }
    }

    private Subscriber<List<Long>> trackListLoadedSubscriber(final Context context, final int startPosition,
                                                             final PlaySessionSource playSessionSource,
                                                             final TrackUrn initialTrackUrn,
                                                             @Nullable final Track initialTrack) {
        showPlayer(context, initialTrackUrn, initialTrack);
        return new DefaultSubscriber<List<Long>>() {
            @Override
            public void onNext(List<Long> idList) {
                final int updatedPosition = correctStartPositionAndDeduplicateList(idList, startPosition, initialTrackUrn);
                PlayQueue playQueue = PlayQueue.fromIdList(idList, updatedPosition, playSessionSource);
                playQueueManager.setNewPlayQueue(playQueue, playSessionSource);
                playCurrent();
            }
        };
    }

    private void playFromIdList(Context activityContext, List<Long> idList, int startPosition, Track initialTrack,
                                PlaySessionSource playSessionSource) {
        playFromIdList(activityContext, idList, startPosition, initialTrack, playSessionSource, false);
    }

    private void playFromIdList(Context activityContext, List<Long> idList, int startPosition, Track initialTrack,
                                PlaySessionSource playSessionSource, boolean loadRelated) {
        showPlayer(activityContext, initialTrack.getUrn(), initialTrack);

        if (shouldChangePlayQueue(initialTrack.getUrn(), playSessionSource)) {
            final int adjustedPosition = getDeduplicatedIdList(idList, startPosition);
            startPlaySession(idList, adjustedPosition, playSessionSource, loadRelated);
        }
    }

    private void showPlayer(Context activityContext, TrackUrn trackUrn) {
        showPlayer(activityContext, trackUrn, null);
    }

    @Deprecated
    private void showPlayer(Context activityContext, TrackUrn trackUrn, @Nullable Track initialTrack) {
        modelManager.cache(initialTrack);
        if (featureFlags.isEnabled(Feature.VISUAL_PLAYER)) {
            eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.forExpandPlayer());
        } else {
            openLegacyPlayer(activityContext, trackUrn.numericId);
        }
    }

    private void openLegacyPlayer(Context activityContext, long initialTrackId) {
        Intent playerActivityIntent = new Intent(Actions.PLAYER)
                .putExtra(Track.EXTRA_ID, initialTrackId)
                .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        activityContext.startActivity(playerActivityIntent);
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

        PlayQueue playQueue = PlayQueue.fromIdList(trackList, startPosition, playSessionSource);
        playQueueManager.setNewPlayQueue(playQueue, playSessionSource);
        playCurrent();

        if (loadRecommended) {
            playQueueManager.fetchRelatedTracks(playQueue.getCurrentTrackId());
        }
    }

    private int correctStartPositionAndDeduplicateList(List<Long> idList, int startPosition, TrackUrn initialTrack) {
        final int updatedPosition;
        if (startPosition < idList.size() && idList.get(startPosition) == initialTrack.numericId) {
            updatedPosition = startPosition;
        } else {
            updatedPosition = idList.indexOf(initialTrack.numericId);
        }

        return getDeduplicatedIdList(idList, updatedPosition);
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
