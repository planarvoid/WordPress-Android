package com.soundcloud.android.utils;

import com.soundcloud.android.Actions;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.track.PlaylistActivity;
import com.soundcloud.android.model.PlayInfo;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.behavior.PlayableHolder;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.ScModel;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.service.playback.CloudPlaybackService;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import java.util.ArrayList;
import java.util.List;

public final class PlayUtils {

    // TODO, Playlists...

    private PlayUtils() {}

    public static void playTrack(Context context, PlayInfo info) {
        playTrack(context, info, true, false);
    }

    public static void playTrack(Context context, PlayInfo info, boolean goToPlayer, boolean commentMode) {
        final Track t = info.initialTrack;
        Intent intent = new Intent();
        if (CloudPlaybackService.getCurrentTrackId() != t.getId()) {
            // changing tracks
            intent.putExtra(CloudPlaybackService.PlayExtras.trackId, t.getId());
            CloudPlaybackService.playlistXfer = info.playables;

            if (info.uri != null) {
                SoundCloudApplication.MODEL_MANAGER.cache(info.initialTrack);
                intent.putExtra(CloudPlaybackService.PlayExtras.trackId, info.initialTrack.getId())
                        .putExtra(CloudPlaybackService.PlayExtras.playPosition, info.position)
                        .setData(info.uri);
            } else {
                intent.putExtra(CloudPlaybackService.PlayExtras.playPosition, info.position)
                        .putExtra(CloudPlaybackService.PlayExtras.playFromXferCache, true);
            }
        }
        if (goToPlayer) {
            intent.setAction(Actions.PLAY)
                .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                .putExtra("commentMode", commentMode);

            context.startActivity(intent);
        } else {
            intent.setAction(CloudPlaybackService.PLAY_ACTION);

            context.startService(intent);
        }
    }

    public static Track getTrackFromIntent(Intent intent){
        if (intent.getBooleanExtra(CloudPlaybackService.PlayExtras.playFromXferCache,false)){
            final int position = intent.getIntExtra(CloudPlaybackService.PlayExtras.playPosition,-1);
            final List<Track> list = CloudPlaybackService.playlistXfer;
            if (list != null && position > -1 && position < list.size() && list.get(position).getPlayable() instanceof Track){
                return (Track) list.get(position).getPlayable();
            }
        } else if (intent.getLongExtra(CloudPlaybackService.PlayExtras.trackId,-1l) > 0) {
            return SoundCloudApplication.MODEL_MANAGER.getTrack(intent.getLongExtra(CloudPlaybackService.PlayExtras.trackId,-1l));
        } else if (intent.getParcelableExtra(Track.EXTRA) != null) {
            return intent.getParcelableExtra(Track.EXTRA);
        }
        return null;
    }

    public static void playFromAdapter(Context context, List<? extends ScModel> data, int position, Uri streamUri) {
        if (position > data.size() || !(data.get(position) instanceof PlayableHolder)) {
            throw new AssertionError("Invalid item " + position + ", must be a playable");
        }

        Playable playable = ((PlayableHolder) data.get(position)).getPlayable();
        if (playable instanceof Track) {
            PlayInfo info = new PlayInfo();
            info.initialTrack = (Track) ((PlayableHolder) data.get(position)).getPlayable();
            info.uri = streamUri;

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

            info.position = adjustedPosition;
            info.playables = tracks;

            playTrack(context, info);

        } else if (playable instanceof Playlist) {
            PlaylistActivity.start(context, (Playlist) playable);
        } else {
            throw new AssertionError("Unexpected playable type");
        }
    }
}
