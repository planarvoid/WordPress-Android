package com.soundcloud.android.utils;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.ScPlayer;
import com.soundcloud.android.adapter.PlayableAdapter;
import com.soundcloud.android.model.PlayInfo;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.ScModel;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.service.playback.CloudPlaybackService;

import android.content.Context;
import android.content.Intent;

import java.util.ArrayList;
import java.util.List;

public class PlayUtils {

    public static void playTrack(Context c, PlayInfo info) {
        playTrack(c, info, true, false);
    }

    public static void playTrack(Context c, PlayInfo info, boolean goToPlayer, boolean commentMode) {

        final Track t = info.getTrack();
        Intent intent = new Intent(c, CloudPlaybackService.class).setAction(CloudPlaybackService.PLAY_ACTION);
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
        c.startService(intent);

        if (goToPlayer) {
            launchPlayer(c, commentMode);
        }

    }

    private static void launchPlayer(Context c, boolean commentMode) {
        Intent i = new Intent(c, ScPlayer.class);
        i.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        i.putExtra("commentMode", commentMode);
        c.startActivity(i);
    }

    public static void playFromAdapter(Context c, PlayableAdapter adapter, List<? extends ScModel> data, int position, long id) {
        if (position > data.size() || !(data.get(position) instanceof Playable)) {
            throw new AssertionError("Invalid item " + position);
        }

        PlayInfo info = new PlayInfo();
        info.uri = adapter.getPlayableUri();

        List<Playable> playables = new ArrayList<Playable>(data.size());


        int adjustedPosition = position;
        for (int i = 0; i < data.size(); i++) {
            if (data.get(i) instanceof Playable) {
                playables.add((Playable) data.get(i));
            } else if (i < position) {
                adjustedPosition--;
            }
        }

        info.position = adjustedPosition;
        info.playables = playables;
        PlayUtils.playTrack(c, info);

    }

}
