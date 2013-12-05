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
import com.soundcloud.android.playback.service.PlaybackService;
import com.soundcloud.android.playlists.PlaylistDetailActivity;
import com.soundcloud.android.rx.observers.DefaultObserver;
import com.soundcloud.android.storage.TrackStorage;
import com.soundcloud.android.tracking.eventlogger.PlaySessionSource;
import com.soundcloud.android.utils.UriUtils;
import rx.Observable;
import rx.android.concurrency.AndroidSchedulers;
import rx.util.functions.Func1;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import javax.inject.Inject;
import java.util.ArrayList;
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


    Uri TEMP_ORIGIN = Uri.EMPTY;

    public PlaybackOperations() {
        this(SoundCloudApplication.MODEL_MANAGER, new TrackStorage());
    }

    @Inject
    public PlaybackOperations(ScModelManager modelManager, TrackStorage trackStorage) {
        mModelManager = modelManager;
        mTrackStorage = trackStorage;
    }

    public Observable<Track> loadTrack(final long trackId) {
        return mTrackStorage.getTrackAsync(trackId).map(new Func1<Track, Track>() {
            @Override
            public Track call(Track track) {
                if (track == null) {
                    track = new Track(trackId);
                }
                return mModelManager.cache(track);
            }
        }).observeOn(AndroidSchedulers.mainThread());
    }

    public Observable<Track> markTrackAsPlayed(Track track) {
        return mTrackStorage.createPlayImpressionAsync(track);
    }

    /**
     * Single play, the tracklist will be of length 1
     */
    public void playTrack(Context context, Track track, Screen screen) {
        playFromIdList(context, Lists.newArrayList(track.getId()), 0, track, new PlaySessionSource(screen.toUri()));
    }

    /**
     * Created by anything played from the {@link com.soundcloud.android.explore.ExploreFragment} section.
     */
    public void playExploreTrack(Context context, Track track, String exploreTag, Uri originPage) {
        playTrack(context, track, new PlaySessionSource(originPage, exploreTag));
    }


    private void playTrack(Context context, Track track, PlaySessionSource playSessionSource) {
        playFromIdList(context, Lists.newArrayList(track.getId()), 0, track, playSessionSource);
    }



    /**
     * From a uri with an initial track to show while loading the full playlist from the DB.
     * Used in {@link com.soundcloud.android.playlists.PlaylistTracksFragment}
     */
    public void playFromPlaylist(Context context, Uri uri, int startPosition, Track initialTrack, Screen screen) {
        final PlaySessionSource playSessionSource = new PlaySessionSource(screen.toUri(), UriUtils.getLastSegmentAsLong(uri));
        playFromUri(context, uri, startPosition, initialTrack, playSessionSource);
    }

    public void playFromAdapter(Context context, List<? extends ScModel> data, int position, Uri uri, Screen screen) {
        if (position >= data.size() || !(data.get(position) instanceof PlayableHolder)) {
            throw new AssertionError("Invalid item " + position + ", must be a playable");
        }

        Playable playable = ((PlayableHolder) data.get(position)).getPlayable();
        if (playable instanceof Track) {

            final PlaySessionSource playSessionSource = new PlaySessionSource(screen.toUri());
            final int adjustedPosition = Collections2.filter(data.subList(0, position), PLAYABLE_HOLDER_PREDICATE).size();

            if (uri != null){
                playFromUri(context, uri, position, (Track) playable, playSessionSource);
            } else {
                playFromIdList(context, getPlayableIdsFromModels(data), adjustedPosition, (Track) playable, playSessionSource);
            }

        } else if (playable instanceof Playlist) {
            PlaylistDetailActivity.start(context, (Playlist) playable, mModelManager);
        } else {
            throw new AssertionError("Unexpected playable type");
        }
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

    private void playFromUri(final Context context, Uri uri, final int startPosition, final Track initialTrack, final PlaySessionSource playSessionSource){
        cacheAndGoToPlayer(context, initialTrack);

        if (isNotCurrentlyPlaying(initialTrack)) {
            mTrackStorage.getTrackIdsForUriAsync(uri).subscribe(new DefaultObserver<List<Long>>() {
                @Override
                public void onNext(List<Long> idList) {
                    final int updatedPosition = correctStartPositionAndDeduplicateList(idList, startPosition, initialTrack);
                    final Intent playIntent = getPlayIntent(idList, updatedPosition, playSessionSource);
                    context.startService(playIntent);
                }
            });
        }
    }

    private void playFromIdList(Context context, List<Long> idList, int startPosition, Track initialTrack, PlaySessionSource playSessionSource){
        cacheAndGoToPlayer(context, initialTrack);

        if (isNotCurrentlyPlaying(initialTrack)) {
            final int adjustedPosition = getDeduplicatedIdList(idList, startPosition);
            final Intent playIntent = getPlayIntent(idList, adjustedPosition, playSessionSource);
            context.startService(playIntent);
        }
    }

    private void cacheAndGoToPlayer(Context context, Track initialTrack) {
        mModelManager.cache(initialTrack);

        // intent for player activity
        context.startActivity(new Intent(Actions.PLAYER)
                .putExtra(Track.EXTRA_ID, initialTrack.getId())
                .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
    }

    private boolean isNotCurrentlyPlaying(Track track){
        return (PlaybackService.getCurrentTrackId() != track.getId());
    }

    private Intent getPlayIntent(final List<Long> trackList, int startPosition,
                                         PlaySessionSource playSessionSource) {

        final Intent intent = new Intent(PlaybackService.Actions.PLAY_ACTION);
        intent.putExtra(PlaybackService.PlayExtras.trackIdList, Longs.toArray(trackList));
        intent.putExtra(PlaybackService.PlayExtras.startPosition, startPosition);
        intent.putExtra(PlaybackService.PlayExtras.playSessionSource, playSessionSource);
        return intent;
    }

    private int correctStartPositionAndDeduplicateList(List<Long> idList, int startPosition, final Track initialTrack) {
        int updatedPosition = idList.get(startPosition) == initialTrack.getId() ? startPosition :
                Iterables.indexOf(idList, new Predicate<Long>() {
                    @Override
                    public boolean apply(Long input) {
                        return input == initialTrack.getId();
                    }
                });

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

}
