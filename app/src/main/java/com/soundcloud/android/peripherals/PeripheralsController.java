package com.soundcloud.android.peripherals;

import static com.soundcloud.android.playback.service.Playa.StateTransition;

import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.utils.ScTextUtils;

import android.content.Context;
import android.content.Intent;

import javax.inject.Inject;

public class PeripheralsController {

    private static final String AVRCP_PLAYSTATE_CHANGED = "com.android.music.playstatechanged";
    private static final String AVRCP_META_CHANGED = "com.android.music.metachanged";

    private final Context context;
    private final EventBus eventBus;

    @Inject
    public PeripheralsController(Context context, EventBus eventBus) {
        this.context = context;
        this.eventBus = eventBus;
    }

    public void subscribe() {
        eventBus.subscribe(EventQueue.PLAYBACK_STATE_CHANGED, new PlayStateChangedSubscriber());
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

    private void notifyPlayStateChanged(boolean isPlaying) {
        Intent intent = new Intent(AVRCP_PLAYSTATE_CHANGED);
        intent.putExtra("playing", isPlaying);
        context.sendBroadcast(intent);
    }

    private class PlayStateChangedSubscriber extends DefaultSubscriber<StateTransition> {
        @Override
        public void onNext(StateTransition state) {
            notifyPlayStateChanged(state.playSessionIsActive());
        }
    }
}
