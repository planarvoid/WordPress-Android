package com.soundcloud.android.peripherals;

import com.soundcloud.android.model.Track;
import com.soundcloud.android.utils.ScTextUtils;

import android.content.Context;
import android.content.Intent;

import javax.inject.Inject;

public class PeripheralsOperations {

    private static final String AVRCP_PLAYSTATE_CHANGED = "com.android.music.playstatechanged";
    private static final String AVRCP_META_CHANGED = "com.android.music.metachanged";

    @Inject
    public PeripheralsOperations() {
    }

    public void notifyMetaChanged(Context context, Track track, boolean isPlaying) {
        notifyChangeViaGoogleMusic(context, track, isPlaying, AVRCP_META_CHANGED);
    }

    public void notifyPlayStateChanged(Context context, Track track, boolean isPlaying) {
        notifyChangeViaGoogleMusic(context, track, isPlaying, AVRCP_PLAYSTATE_CHANGED);
    }

    private void notifyChangeViaGoogleMusic(Context context, Track track, boolean isPlaying, String action) {
        if(track != null) {
            Intent intent = new Intent(action);
            intent.putExtra("id", track.getId());
            intent.putExtra("track", ScTextUtils.getClippedString(track.getTitle(), 40));
            intent.putExtra("playing", isPlaying);
            intent.putExtra("duration", track.duration);

            if(track.getUserName() != null) {
                intent.putExtra("artist", ScTextUtils.getClippedString(track.getUserName(), 30));
            }

            context.sendBroadcast(intent);
        }
    }
}
