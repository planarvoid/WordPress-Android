package com.soundcloud.android.playback;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.primitives.Longs;
import com.soundcloud.android.Actions;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.ScModel;
import com.soundcloud.android.model.ScModelManager;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.behavior.PlayableHolder;
import com.soundcloud.android.playback.service.PlaySessionSource;
import com.soundcloud.android.playback.service.PlaybackService;
import com.soundcloud.android.playback.service.PlaybackStateProvider;
import com.soundcloud.android.playlists.PlaylistDetailActivity;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.storage.TrackStorage;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.utils.ScTextUtils;
import org.jetbrains.annotations.Nullable;

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

    private ScModelManager mModelManager;
    private TrackStorage mTrackStorage;
    private PlaybackStateProvider mPlaybackStateProvider;

    // Use @Inject instead
    @Deprecated
    public PlaybackOperations() {
        this(SoundCloudApplication.sModelManager, new TrackStorage(), new PlaybackStateProvider());
    }

    @Inject
    public PlaybackOperations(ScModelManager modelManager, TrackStorage trackStorage, PlaybackStateProvider playbackStateProvider) {
        mModelManager = modelManager;
        mTrackStorage = trackStorage;
        mPlaybackStateProvider = playbackStateProvider;
    }

    /**
     * Single play, the tracklist will be of length 1
     */
    public void playTrack(Context context, Track track, Screen screen) {
        playFromIdList(context, Lists.newArrayList(track.getId()), 0, track, new PlaySessionSource(screen));
    }

    /**
     * Created by anything played from the {@link com.soundcloud.android.explore.ExploreFragment} section.
     */
    public void playExploreTrack(Context context, Track track, String exploreTag, String screenTag) {
        final PlaySessionSource playSessionSource = new PlaySessionSource(screenTag);
        playSessionSource.setExploreVersion(exploreTag);
        playTrack(context, track, playSessionSource);
    }

    private void playTrack(Context context, Track track, PlaySessionSource playSessionSource) {
        playFromIdList(context, Lists.newArrayList(track.getId()), 0, track, playSessionSource);
    }

    /**
     * From a uri with an initial track to show while loading the full playlist from the DB.
     * Used in {@link com.soundcloud.android.playlists.PlaylistTracksFragment}
     */
    public void playFromPlaylist(Context context, Playlist playlist, int startPosition, Track initialTrack, Screen screen) {
        final PlaySessionSource playSessionSource = new PlaySessionSource(screen.get());
        playSessionSource.setPlaylist(playlist);
        playFromUri(context, playlist.toUri(), startPosition, initialTrack, playSessionSource);
    }

    public void playPlaylist(Context context, Playlist playlist, Screen screen) {
        final PlaySessionSource playSessionSource = new PlaySessionSource(screen.get());
        playSessionSource.setPlaylist(playlist);
        cacheAndGoToPlayer(context, playlist.getTracks().get(0));
        final List<Long> trackIds = Lists.transform(playlist.getTracks(), new Function<Track, Long>() {
            @Override
            public Long apply(Track track) {
                return track.getId();
            }
        });

        context.startService(getPlayIntent(trackIds, 0, playSessionSource));
    }

    @Deprecated
    public void playFromAdapter(Context context, List<? extends ScModel> data, int position, Uri uri, Screen screen) {
        if (position >= data.size() || !(data.get(position) instanceof PlayableHolder)) {
            throw new AssertionError("Invalid item " + position + ", must be a playable");
        }

        Playable playable = ((PlayableHolder) data.get(position)).getPlayable();
        if (playable instanceof Track) {

            final PlaySessionSource playSessionSource = new PlaySessionSource(screen);
            final int adjustedPosition = Collections2.filter(data.subList(0, position), PLAYABLE_HOLDER_PREDICATE).size();

            if (uri != null){
                playFromUri(context, uri, position, (Track) playable, playSessionSource);
            } else {
                playFromIdList(context, getPlayableIdsFromModels(data), adjustedPosition, (Track) playable, playSessionSource);
            }

        } else if (playable instanceof Playlist) {
            PlaylistDetailActivity.start(context, (Playlist) playable, mModelManager, screen);
        } else {
            throw new AssertionError("Unexpected playable type");
        }
    }

    public void startPlayback(final Context context, Track track, Screen screen) {
        mModelManager.cache(track);
        startPlayback(context, track.getId(), screen);
    }

    public void startPlayback(final Context context, long id, Screen screen) {
        context.startService(getPlayIntent(Lists.newArrayList(id), 0, new PlaySessionSource(screen)));
    }

    public void togglePlayback(final Context context) {
        Track currentTrack = mPlaybackStateProvider.getCurrentTrack();
        if (!mPlaybackStateProvider.isPlaying() && currentTrack != null) {
            cacheAndGoToPlayer(context, currentTrack);
        }
        context.startService(new Intent(PlaybackService.Actions.TOGGLEPLAYBACK_ACTION));
    }

    public void playFromIdListShuffled(final Context context, List<Long> ids, Screen screen) {
        List<Long> shuffled = Lists.newArrayList(ids);
        Collections.shuffle(shuffled);
        context.startService(getPlayIntent(shuffled, 0, new PlaySessionSource(screen)));
        gotoPlayer(context, shuffled.get(0));
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

    private void playFromUri(final Context context, Uri uri, final int startPosition, final Track initialTrack,
                             final PlaySessionSource playSessionSource) {
        cacheAndGoToPlayer(context, initialTrack);

        if (shouldChangePlayQueue(initialTrack, playSessionSource)) {
            mTrackStorage.getTrackIdsForUriAsync(uri).subscribe(new DefaultSubscriber<List<Long>>() {
                @Override
                public void onNext(List<Long> idList) {
                    final int updatedPosition = correctStartPositionAndDeduplicateList(idList, startPosition, initialTrack);
                    final Intent playIntent = getPlayIntent(idList, updatedPosition, playSessionSource);
                    context.startService(playIntent);
                }
            });
        }
    }

    private void playFromIdList(Context context, List<Long> idList, int startPosition, Track initialTrack, PlaySessionSource playSessionSource) {
        cacheAndGoToPlayer(context, initialTrack);

        if (shouldChangePlayQueue(initialTrack, playSessionSource)) {
            final int adjustedPosition = getDeduplicatedIdList(idList, startPosition);
            final Intent playIntent = getPlayIntent(idList, adjustedPosition, playSessionSource);
            context.startService(playIntent);
        }
    }

    private void cacheAndGoToPlayer(Context context, Track initialTrack) {
        mModelManager.cache(initialTrack);
        gotoPlayer(context, initialTrack.getId());
    }

    private void gotoPlayer(Context context, long initialTrackId) {
        Intent playerActivityIntent = new Intent(Actions.PLAYER)
                .putExtra(Track.EXTRA_ID, initialTrackId)
                .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        context.startActivity(playerActivityIntent);
    }

    private boolean shouldChangePlayQueue(Track track, PlaySessionSource playSessionSource){
        return mPlaybackStateProvider.getCurrentTrackId() != track.getId() ||
                !playSessionSource.getOriginScreen().equals(mPlaybackStateProvider.getScreenTag()) ||
                playSessionSource.getPlaylistId() != mPlaybackStateProvider.getPlayQueuePlaylistId();
    }

    public Intent getPlayIntent(final List<Long> trackList, int startPosition,
                                         PlaySessionSource playSessionSource) {

        final Intent intent = new Intent(PlaybackService.Actions.PLAY_ACTION);
        intent.putExtra(PlaybackService.PlayExtras.TRACK_ID_LIST, Longs.toArray(trackList));
        intent.putExtra(PlaybackService.PlayExtras.START_POSITION, startPosition);
        intent.putExtra(PlaybackService.PlayExtras.PLAY_SESSION_SOURCE, playSessionSource);
        return intent;
    }

    private int correctStartPositionAndDeduplicateList(List<Long> idList, int startPosition, final Track initialTrack) {
        final int updatedPosition;
        if (startPosition < idList.size() &&
                idList.get(startPosition) == initialTrack.getId()) {
            updatedPosition = startPosition;
        } else {
            updatedPosition = Iterables.indexOf(idList, new Predicate<Long>() {
                @Override
                public boolean apply(Long input) {
                    return input == initialTrack.getId();
                }
            });
        }

        return getDeduplicatedIdList(idList, updatedPosition);
    }

    /**
     * Remove duplicates from playqueue, preserving the ordering with regards to the item they clicked on
     * Returns the new startPosition
     */
    private int getDeduplicatedIdList(List<Long> idList, int startPosition){
        final Set <Long> seenIds = Sets.newHashSetWithExpectedSize(idList.size());
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

    public enum AppendState {
        IDLE, LOADING, ERROR, EMPTY;
    }

    public @Nullable Intent getServiceBasedUpIntent() {
        final String originScreen = mPlaybackStateProvider.getScreenTag();
        if (ScTextUtils.isBlank(originScreen)){
            return null; // might have come from widget and the play queue is empty
        }

        long playlistId = mPlaybackStateProvider.getPlayQueuePlaylistId();
        if (playlistId != Playable.NOT_SET) {
            return getUpDestinationFromPlaylist(playlistId, originScreen);
        } else {
            return Screen.getUpDestinationFromScreenTag(originScreen);
        }
    }

    private Intent getUpDestinationFromPlaylist(long playlistId, String originScreen) {
        Intent upIntent = new Intent(Actions.PLAYLIST).setData(Content.PLAYLIST.forId(playlistId));
        Screen.fromScreenTag(originScreen).addToIntent(upIntent);
        upIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return upIntent;
    }

}
