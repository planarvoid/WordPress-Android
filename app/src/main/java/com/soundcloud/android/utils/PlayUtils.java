package com.soundcloud.android.utils;

import com.soundcloud.android.Actions;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.adapter.PlayableAdapter;
import com.soundcloud.android.model.PlayInfo;
import com.soundcloud.android.model.PlayableHolder;
import com.soundcloud.android.model.ScModel;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.service.playback.CloudPlaybackService;

import android.content.Context;
import android.content.Intent;

import java.util.ArrayList;
import java.util.List;

public final class PlayUtils {
    private PlayUtils() {}

    public static void playTrack(Context c, PlayInfo info) {
        playTrack(c, info, true, false);
    }

    public static void playTrack(Context c, PlayInfo info, boolean goToPlayer, boolean commentMode) {
        final Track t = info.getTrack();
        Intent intent = new Intent();
        if (CloudPlaybackService.getCurrentTrackId() != t.id) {
            // changing tracks
            intent.putExtra(CloudPlaybackService.PlayExtras.trackId, t.id);
            if (info.uri != null) {
                SoundCloudApplication.MODEL_MANAGER.cache(info.getTrack());
                intent.putExtra(CloudPlaybackService.PlayExtras.trackId, info.getTrack().id)
                        .putExtra(CloudPlaybackService.PlayExtras.playPosition, info.position)
                        .setData(info.uri);
            } else {
                CloudPlaybackService.playlistXfer = info.playables;
                intent.putExtra(CloudPlaybackService.PlayExtras.playPosition, info.position)
                        .putExtra(CloudPlaybackService.PlayExtras.playFromXferCache, true);
            }
        }
        if (goToPlayer) {
            intent.setAction(Actions.PLAY)
                .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                .putExtra("commentMode", commentMode);

            c.startActivity(intent);
        } else {
            intent.setAction(CloudPlaybackService.PLAY_ACTION);

            c.startService(intent);
        }
    }

    public static Track getTrackFromIntent(Intent intent){
        if (intent.getBooleanExtra(CloudPlaybackService.PlayExtras.playFromXferCache,false)){
            final int position = intent.getIntExtra(CloudPlaybackService.PlayExtras.playPosition,-1);
            final List<PlayableHolder> list = CloudPlaybackService.playlistXfer;
            if (list != null && position > -1 && position < list.size()){
                return list.get(position).getTrack();
            }
        } else if (intent.getLongExtra(CloudPlaybackService.PlayExtras.trackId,-1l) > 0) {
            return SoundCloudApplication.MODEL_MANAGER.getTrack(intent.getLongExtra(CloudPlaybackService.PlayExtras.trackId,-1l));
        } else if (intent.getParcelableExtra(Track.EXTRA) != null) {
            return intent.getParcelableExtra(Track.EXTRA);
        }
        return null;
    }

    public static void playFromAdapter(Context c, PlayableAdapter adapter, List<? extends ScModel> data, int position) {
        if (position > data.size() || !(data.get(position) instanceof PlayableHolder)) {
            throw new AssertionError("Invalid item " + position);
        }

        PlayInfo info = new PlayInfo();
        info.uri = adapter.getPlayableUri();

        List<PlayableHolder> playables = new ArrayList<PlayableHolder>(data.size());

        int adjustedPosition = position;
        for (int i = 0; i < data.size(); i++) {
            if (data.get(i) instanceof PlayableHolder) {
                playables.add((PlayableHolder) data.get(i));
            } else if (i < position) {
                adjustedPosition--;
            }
        }

        info.position = adjustedPosition;
        info.playables = playables;
        playTrack(c, info);
    }
}
