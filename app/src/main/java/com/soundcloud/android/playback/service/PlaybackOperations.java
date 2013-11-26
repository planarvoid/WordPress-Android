package com.soundcloud.android.playback.service;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.soundcloud.android.Actions;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.ScModel;
import com.soundcloud.android.model.ScModelManager;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.behavior.PlayableHolder;
import com.soundcloud.android.playlists.PlaylistDetailActivity;
import com.soundcloud.android.rx.observers.DefaultObserver;
import com.soundcloud.android.storage.TrackStorage;
import com.soundcloud.android.tracking.eventlogger.TrackSourceInfo;
import com.soundcloud.android.utils.UriUtils;
import rx.Observable;
import rx.android.concurrency.AndroidSchedulers;
import rx.util.functions.Func1;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class PlaybackOperations {

    private ScModelManager mModelManager;
    private TrackStorage mTrackStorage;

    // todo
    Uri mTempPageOrigin = Uri.parse("http://putsomethinghere.com");

    public PlaybackOperations() {
        this(SoundCloudApplication.MODEL_MANAGER, new TrackStorage());
    }

    public PlaybackOperations(ScModelManager modelManager, TrackStorage trackStorage) {
        mModelManager = modelManager;
        mTrackStorage = trackStorage;
    }

    /**
     * Single play, the tracklist will be of length 1
     */

    public void playTrack(Context context, Track track) {
        playTrack(context, track, mTempPageOrigin);
    }

    public void playTrack(Context context, Track track, Uri pageOrigin) {
        playFromInfo(context, PlayInitInfo.fromTrack(track, new PlaySessionSource(pageOrigin)));
    }

    /**
     * Created by anything played from the {@link com.soundcloud.android.explore.ExploreFragment} section.
     */
    public void playExploreTrack(Context context, Track track, String exploreTag) {
        playExploreTrack(context, track, exploreTag, mTempPageOrigin);
    }
    public void playExploreTrack(Context context, Track track, String exploreTag, Uri originPage) {
        final PlayInitInfo playInitInfo = PlayInitInfo.fromExplore(track, new PlaySessionSource(originPage),
                TrackSourceInfo.fromExplore(exploreTag));
        playFromInfo(context, playInitInfo);
    }

    /**
     * From a uri with an initial track to show while loading the full playlist from the DB.
     * Used in {@link com.soundcloud.android.playlists.PlaylistTracksFragment}
     */

    public void playFromPlaylist(Context context, Uri uri, int startPosition, Track initialTrack) {
        playFromPlaylist(context, uri, startPosition, initialTrack, mTempPageOrigin);
    }

    public void playFromPlaylist(Context context, Uri uri, int startPosition, Track initialTrack, Uri originPage) {
        final PlaySessionSource playSessionSource = new PlaySessionSource(originPage, UriUtils.getLastSegmentAsLong(uri));
        final PlayInitInfo playInitInfo = PlayInitInfo.fromUriWithTrack(uri, startPosition, initialTrack, playSessionSource);
        playFromInfo(context, playInitInfo);
    }

    public void playFromAdapter(Context context, List<? extends ScModel> data, int position, Uri uri) {
        playFromAdapter(context, data, position, uri, mTempPageOrigin);
    }

    public void playFromAdapter(Context context, List<? extends ScModel> data, int position, Uri uri, Uri originPage) {
        if (position >= data.size() || !(data.get(position) instanceof PlayableHolder)) {
            throw new AssertionError("Invalid item " + position + ", must be a playable");
        }

        Playable playable = ((PlayableHolder) data.get(position)).getPlayable();
        if (playable instanceof Track) {

            List<Track> tracks = new ArrayList<Track>(data.size());
            // Required for mixed adapters (e.g. mix of users and tracks, we only want tracks)
            int adjustedPosition = position;
            for (int i = 0; i < data.size(); i++) {
                if (data.get(i) instanceof PlayableHolder && ((PlayableHolder) data.get(i)).getPlayable() instanceof Track) {
                    tracks.add((Track) ((PlayableHolder) data.get(i)).getPlayable());
                } else if (i < position) {
                    adjustedPosition--;
                }
            }

            PlayInitInfo playInitInfo = PlayInitInfo.fromUriWithTrack(uri, adjustedPosition, tracks.get(adjustedPosition),
                    new PlaySessionSource(originPage));
            playInitInfo.initialTracklist = tracks;
            playFromInfo(context, playInitInfo);

        } else if (playable instanceof Playlist) {
            PlaylistDetailActivity.start(context, (Playlist) playable, mModelManager);
        } else {
            throw new AssertionError("Unexpected playable type");
        }
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

    private void playFromInfo(Context context, PlayInitInfo playInitInfo) {
        mModelManager.cache(playInitInfo.initialTrack);

        // intent for player activity
        context.startActivity(new Intent(Actions.PLAYER)
                .putExtra(Track.EXTRA_ID, playInitInfo.initialTrack.getId())
                .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));

        // intent for playback service
        if (PlaybackService.getCurrentTrackId() != playInitInfo.initialTrack.getId()) {
            sendIntentViaPlayInfo(context, playInitInfo);
        }
    }

    private void sendIntentViaPlayInfo(final Context context, final PlayInitInfo info) {

        final Intent intent = new Intent(PlaybackService.Actions.PLAY_ACTION);
        intent.putExtra(PlaybackService.PlayExtras.fetchRelated, info.fetchRelated);

        if (info.isStoredCollection()) {
            mTrackStorage.getTrackIdsForUriAsync(info.uri).subscribe(new DefaultObserver<List<Long>>() {
                @Override
                public void onNext(List<Long> idList) {
                    intent.putExtra(PlayQueue.EXTRA, createDeDupedPlayQueue(idList, info));
                    context.startService(intent);
                }
            });

        } else {
            final List<Long> idList = Lists.transform(info.initialTracklist, new Function<Track, Long>() {
                @Override
                public Long apply(Track input) {
                    return input.getId();
                }
            });
            intent.putExtra(PlayQueue.EXTRA, createDeDupedPlayQueue(idList, info));
            context.startService(intent);
        }
    }

    /**
     * Remove duplicates from playqueue, preserving the ordering with regards to the item they clicked on
     */
    private TrackingPlayQueue createDeDupedPlayQueue(List<Long> idList, PlayInitInfo playInitInfo){
        final Set <Long> seenIds = Sets.newHashSetWithExpectedSize(idList.size());
        final long playedId = idList.get(playInitInfo.startPosition);

        int i = 0;
        Iterator<Long> iter = idList.iterator();
        while (iter.hasNext()) {
            final long val = iter.next().longValue();
            if (i != playInitInfo.startPosition && (seenIds.contains(val) || val == playedId)) {
                iter.remove();
                if (i < playInitInfo.startPosition) playInitInfo.startPosition--;
            } else {
                seenIds.add(val);
                i++;
            }
        }
        return new TrackingPlayQueue(idList, playInitInfo.startPosition, playInitInfo.playSessionSource, playInitInfo.trackSourceInfo);
    }

    private static class PlayInitInfo {

        private int startPosition;
        private Uri uri;
        private Track initialTrack;
        private List<Track> initialTracklist;
        private boolean fetchRelated;

        private PlaySessionSource playSessionSource;
        private TrackSourceInfo trackSourceInfo;

        private PlayInitInfo(PlaySessionSource playSessionSource) {
            this.playSessionSource = playSessionSource;
        }

        private static PlayInitInfo fromTrack(Track track, PlaySessionSource playSessionSource){
            PlayInitInfo playInitInfo = new PlayInitInfo(playSessionSource);
            playInitInfo.initialTrack = track;
            playInitInfo. initialTracklist = Lists.newArrayList(track);
            return playInitInfo;
        }

        private static PlayInitInfo fromExplore(Track track, PlaySessionSource playSessionSource, TrackSourceInfo trackSourceInfo) {
            PlayInitInfo playInitInfo = fromTrack(track, playSessionSource);
            playInitInfo.fetchRelated = true;
            playInitInfo.trackSourceInfo = trackSourceInfo;
            return playInitInfo;
        }

        private static PlayInitInfo fromUri(Uri uri, int startPosition, PlaySessionSource playSessionSource) {
            PlayInitInfo playInitInfo = new PlayInitInfo(playSessionSource);
            playInitInfo.uri = uri;
            playInitInfo.startPosition = startPosition;
            return playInitInfo;
        }

        private static PlayInitInfo fromUriWithTrack(Uri uri, int startPosition, Track initialTrack, PlaySessionSource playSessionSource) {
            PlayInitInfo playInitInfo = fromUri(uri, startPosition, playSessionSource);
            playInitInfo.initialTrack = initialTrack;
            return playInitInfo;
        }

        public boolean isStoredCollection() {
            return uri != null;
        }
    }
}
