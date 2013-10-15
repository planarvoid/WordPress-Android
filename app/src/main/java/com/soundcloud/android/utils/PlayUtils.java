package com.soundcloud.android.utils;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.primitives.Longs;
import com.soundcloud.android.Actions;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.track.PlaylistDetailActivity;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.ScModel;
import com.soundcloud.android.model.ScModelManager;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.behavior.PlayableHolder;
import com.soundcloud.android.service.playback.CloudPlaybackService;
import com.soundcloud.android.tracking.eventlogger.PlaySourceInfo;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import java.util.ArrayList;
import java.util.List;

public final class PlayUtils {

    private ScModelManager mModelManager;

    public PlayUtils() {
        this(SoundCloudApplication.MODEL_MANAGER);
    }

    public PlayUtils(ScModelManager modelManager) {
        mModelManager = modelManager;
    }

    /**
     * Single play, the tracklist will be of length 1
     */
    public void playTrack(Context context, Track track) {
        final PlaySourceInfo playSourceInfo = new PlaySourceInfo.Builder(track.getId()).build();
        playFromInfo(context, new PlayInfo(track, false, playSourceInfo));
    }

    /**
     * Created by anything played from the {@link com.soundcloud.android.activity.landing.ExploreActivity} section.
     */
    public void playExploreTrack(Context context, Track track, String exploreTag) {
        final PlaySourceInfo playSourceInfo = new PlaySourceInfo.Builder(track.getId()).exploreTag(exploreTag).build();
        playFromInfo(context, new PlayInfo(track, true, playSourceInfo));
    }

    /**
     * From a uri with an initial track to show while loading the full playlist from the DB.
     * Used in {@link com.soundcloud.android.fragment.PlaylistTracksFragment}
     */
    public void playFromUriWithInitialTrack(Context context, Uri uri, int startPosition, Track initialTrack) {
        PlayInfo playInfo = PlayInfo.fromUri(uri, startPosition);
        playInfo.initialTrack = initialTrack;
        playInfo.sourceInfo = new PlaySourceInfo.Builder(initialTrack.getId()).build();
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

    public static Track getTrackFromIntent(Intent intent){
        if (intent.getLongExtra(CloudPlaybackService.PlayExtras.trackId,-1l) > 0) {
            return SoundCloudApplication.MODEL_MANAGER.getTrack(intent.getLongExtra(CloudPlaybackService.PlayExtras.trackId,-1l));
        } else if (intent.getParcelableExtra(Track.EXTRA) != null) {
            return intent.getParcelableExtra(Track.EXTRA);
        }
        return null;
    }

    private void playFromInfo(Context context, PlayInfo playInfo){
        Intent intent = new Intent(Actions.PLAY).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        if (CloudPlaybackService.getCurrentTrackId() != playInfo.initialTrack.getId()) {
            configureIntentViaPlayInfo(playInfo, intent);
        }
        context.startActivity(intent);
    }

    private void configureIntentViaPlayInfo(PlayInfo info, Intent intent) {
        intent.putExtra(CloudPlaybackService.PlayExtras.fetchRelated, info.fetchRelated);
        intent.putExtra(CloudPlaybackService.PlayExtras.trackingInfo, info.sourceInfo);
        intent.putExtra(CloudPlaybackService.PlayExtras.track, info.initialTrack);

        if (info.uri != null) {
            mModelManager.cache(info.initialTrack);
            intent.putExtra(CloudPlaybackService.PlayExtras.trackId, info.initialTrack.getId())
                    .putExtra(CloudPlaybackService.PlayExtras.playPosition, info.position)
                    .setData(info.uri);

        } else if (info.initialTracklist.size() > 1) {
            final List<Long> idList = Lists.transform(info.initialTracklist, new Function<Track, Long>() {
                @Override
                public Long apply(Track input) {
                    return input.getId();
                }
            });
            intent.putExtra(CloudPlaybackService.PlayExtras.playPosition, info.position)
                    .putExtra(CloudPlaybackService.PlayExtras.trackIdList, Longs.toArray(idList));
        }
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
            playInfo.sourceInfo = new PlaySourceInfo.Builder(initialTrack.getId()).build();
            return playInfo;
        }

        private static PlayInfo fromUri(Uri uri, int startPosition) {
            PlayInfo playInfo = new PlayInfo();
            playInfo.uri = uri;
            playInfo.position = startPosition;
            return playInfo;
        }
    }
}
