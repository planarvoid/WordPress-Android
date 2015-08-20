package com.soundcloud.android.peripherals;

import static com.soundcloud.android.playback.Playa.StateTransition;

import com.soundcloud.android.events.CurrentPlayQueueTrackEvent;
import com.soundcloud.android.events.CurrentUserChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.rx.eventbus.EventBus;

import android.content.Context;
import android.content.Intent;

import javax.inject.Inject;

public class PeripheralsController {

    private static final String AVRCP_PLAYSTATE_CHANGED = "com.android.music.playstatechanged";
    private static final String AVRCP_META_CHANGED = "com.android.music.metachanged";

    private final Context context;
    private final EventBus eventBus;
    private final TrackRepository trackRepository;

    @Inject
    public PeripheralsController(Context context, EventBus eventBus, TrackRepository trackRepository) {
        this.context = context;
        this.eventBus = eventBus;
        this.trackRepository = trackRepository;
    }

    public void subscribe() {
        eventBus.subscribe(EventQueue.CURRENT_USER_CHANGED, new CurrentUserChangedSubscriber());
        eventBus.subscribe(EventQueue.PLAYBACK_STATE_CHANGED, new PlayStateChangedSubscriber());
        eventBus.queue(EventQueue.PLAY_QUEUE_TRACK).subscribe(new PlayQueueChangedSubscriber());
    }

    private void notifyPlayStateChanged(boolean isPlaying) {
        Intent intent = new Intent(AVRCP_PLAYSTATE_CHANGED);
        intent.putExtra("playing", isPlaying);
        context.sendBroadcast(intent);
    }

    private void notifyPlayQueueChanged(PropertySet track) {
        Intent intent = new Intent(AVRCP_META_CHANGED);
        intent.putExtra("id", track.get(TrackProperty.URN).getNumericId());
        intent.putExtra("track", ScTextUtils.getClippedString(track.get(PlayableProperty.TITLE), 40));
        intent.putExtra("duration", track.get(PlayableProperty.DURATION).longValue());
        intent.putExtra("artist", getSafeClippedString(track.get(PlayableProperty.CREATOR_NAME), 30));
        context.sendBroadcast(intent);
    }

    private String getSafeClippedString(String string, int maxLength) {
        if (ScTextUtils.isBlank(string)) {
            return ScTextUtils.EMPTY_STRING;
        } else {
            return ScTextUtils.getClippedString(string, maxLength);
        }
    }

    private void resetTrackInformation() {
        Intent intent = new Intent(AVRCP_META_CHANGED);
        intent.putExtra("id", "");
        intent.putExtra("track", "");
        intent.putExtra("duration", 0);
        intent.putExtra("artist", "");

        context.sendBroadcast(intent);
    }

    private class CurrentUserChangedSubscriber extends DefaultSubscriber<CurrentUserChangedEvent> {
        @Override
        public void onNext(CurrentUserChangedEvent event) {
            resetTrackInformation();
        }
    }

    private class PlayStateChangedSubscriber extends DefaultSubscriber<StateTransition> {
        @Override
        public void onNext(StateTransition state) {
            notifyPlayStateChanged(state.playSessionIsActive());
        }
    }

    private class PlayQueueChangedSubscriber extends DefaultSubscriber<CurrentPlayQueueTrackEvent> {
        @Override
        public void onNext(CurrentPlayQueueTrackEvent event) {
            trackRepository.track(event.getCurrentTrackUrn()).subscribe(new CurrentTrackSubscriber());
        }
    }

    private class CurrentTrackSubscriber extends DefaultSubscriber<PropertySet> {
        @Override
        public void onNext(PropertySet track) {
            notifyPlayQueueChanged(track);
        }
    }
}
