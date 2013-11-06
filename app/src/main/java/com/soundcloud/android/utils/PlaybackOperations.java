package com.soundcloud.android.utils;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.soundcloud.android.Actions;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.track.PlaylistDetailActivity;
import com.soundcloud.android.dao.TrackStorage;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.ScModel;
import com.soundcloud.android.model.ScModelManager;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.behavior.PlayableHolder;
import com.soundcloud.android.rx.observers.DefaultObserver;
import com.soundcloud.android.service.playback.CloudPlaybackService;
import com.soundcloud.android.service.playback.PlayQueue;
import com.soundcloud.android.tracking.eventlogger.PlaySourceInfo;
import rx.Observable;
import rx.android.concurrency.AndroidSchedulers;

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
        final PlaySourceInfo playSourceInfo = new PlaySourceInfo.Builder().build();
        playFromInfo(context, new PlayInfo(track, false, playSourceInfo));
    }

    /**
     * Created by anything played from the {@link com.soundcloud.android.fragment.ExploreFragment} section.
     */
    public void playExploreTrack(Context context, Track track, String exploreTag) {
        final PlaySourceInfo playSourceInfo = new PlaySourceInfo.Builder().initialTrackId(track.getId())
                .exploreVersion(exploreTag).build();
        playFromInfo(context, new PlayInfo(track, true, playSourceInfo));
    }

    /**
     * From a uri with an initial track to show while loading the full playlist from the DB.
     * Used in {@link com.soundcloud.android.fragment.PlaylistTracksFragment}
     */
    public void playFromUriWithInitialTrack(Context context, Uri uri, int startPosition, Track initialTrack) {
        PlayInfo playInfo = PlayInfo.fromUri(uri, startPosition);
        playInfo.initialTrack = initialTrack;
        playInfo.sourceInfo = new PlaySourceInfo.Builder().build();
        playFromInfo(context, playInfo);
    }

    public void playFromAdapter(Context context, List<? extends ScModel> data, int position, Uri uri) {
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

            PlayInfo playInfo = PlayInfo.fromUriWithTrack(uri, adjustedPosition, tracks.get(adjustedPosition));
            playInfo.initialTracklist = tracks;
            playFromInfo(context, playInfo);

        } else if (playable instanceof Playlist) {
            PlaylistDetailActivity.start(context, (Playlist) playable, mModelManager);
        } else {
            throw new AssertionError("Unexpected playable type");
        }
    }

    public Observable<Track> loadTrack(long trackId) {
        return mTrackStorage.getTrackAsync(trackId).observeOn(AndroidSchedulers.mainThread());
    }

    public Observable<Track> markTrackAsPlayed(Track track) {
        return mTrackStorage.createPlayImpressionAsync(track);
    }

    private void playFromInfo(Context context, PlayInfo playInfo) {
        mModelManager.cache(playInfo.initialTrack);

        // intent for player activity
        context.startActivity(new Intent(Actions.PLAYER)
                .putExtra(Track.EXTRA_ID, playInfo.initialTrack.getId())
                .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));

        // intent for playback service
        if (CloudPlaybackService.getCurrentTrackId() != playInfo.initialTrack.getId()) {
            sendIntentViaPlayInfo(context, playInfo);
        }
    }

    private void sendIntentViaPlayInfo(final Context context, final PlayInfo info) {

        final Intent intent = new Intent(CloudPlaybackService.Actions.PLAY_ACTION);
        intent.putExtra(CloudPlaybackService.PlayExtras.fetchRelated, info.fetchRelated);
        intent.putExtra(CloudPlaybackService.PlayExtras.trackingInfo, info.sourceInfo);

        if (info.isStoredCollection()) {
            mTrackStorage.getTrackIdsForUriAsync(info.uri).subscribe(new DefaultObserver<List<Long>>() {
                @Override
                public void onNext(List<Long> idList) {
                    intent.putExtra(PlayQueue.EXTRA, createDeDupedPlayQueue(idList, info.position, info.sourceInfo, info.uri));
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
            intent.putExtra(PlayQueue.EXTRA, createDeDupedPlayQueue(idList, info.position, info.sourceInfo, Uri.EMPTY));
            context.startService(intent);
        }
    }

    /**
     * Remove duplicates from playqueue, preserving the ordering with regards to the item they clicked on
     */
    private PlayQueue createDeDupedPlayQueue(List<Long> idList, int playPosition, PlaySourceInfo sourceInfo, Uri uri){
        final Set <Long> seenIds = Sets.newHashSetWithExpectedSize(idList.size());
        final long playedId = idList.get(playPosition);

        int i = 0;
        Iterator<Long> iter = idList.iterator();
        while (iter.hasNext()) {
            final long val = iter.next().longValue();
            if (i != playPosition && (seenIds.contains(val) || val == playedId)) {
                iter.remove();
                if (i < playPosition) playPosition--;
            } else {
                seenIds.add(val);
                i++;
            }
        }
        return new PlayQueue(idList, playPosition, sourceInfo, uri);
    }

    private static class PlayInfo {

        private int position;
        private Uri uri;
        private Track initialTrack;
        private List<Track> initialTracklist;
        private boolean fetchRelated;
        private PlaySourceInfo sourceInfo;

        private PlayInfo() {
        }

        private PlayInfo(Track track, boolean fetchRelated, PlaySourceInfo playSourceInfo) {
            this.initialTrack = track;
            this.initialTracklist = Lists.newArrayList(track);
            this.fetchRelated = fetchRelated;
            this.sourceInfo = playSourceInfo;
        }

        private static PlayInfo fromUriWithTrack(Uri uri, int startPosition, Track initialTrack) {
            PlayInfo playInfo = fromUri(uri, startPosition);
            playInfo.initialTrack = initialTrack;
            playInfo.sourceInfo = new PlaySourceInfo.Builder().build();
            return playInfo;
        }

        private static PlayInfo fromUri(Uri uri, int startPosition) {
            PlayInfo playInfo = new PlayInfo();
            playInfo.uri = uri;
            playInfo.position = startPosition;
            return playInfo;
        }

        public boolean isStoredCollection() {
            return uri != null;
        }
    }
}
