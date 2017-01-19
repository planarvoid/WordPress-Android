package com.soundcloud.android.peripherals;

import com.soundcloud.android.events.CurrentPlayQueueItemEvent;
import com.soundcloud.android.events.CurrentUserChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.playback.Durations;
import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.android.playback.PlayStateEvent;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.tracks.Track;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.java.strings.Strings;
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
        eventBus.queue(EventQueue.CURRENT_PLAY_QUEUE_ITEM).subscribe(new PlayQueueChangedSubscriber());
    }

    private void notifyPlayStateChanged(boolean isPlaying) {
        Intent intent = new Intent(AVRCP_PLAYSTATE_CHANGED);
        intent.putExtra("playing", isPlaying);
        context.sendBroadcast(intent);
    }

    private void notifyPlayQueueChanged(Track track) {
        Intent intent = new Intent(AVRCP_META_CHANGED);
        intent.putExtra("id", track.urn().getNumericId());
        intent.putExtra("track", ScTextUtils.getClippedString(track.title(), 40));
        intent.putExtra("duration", Durations.getTrackPlayDuration(track));
        intent.putExtra("artist", getSafeClippedString(track.creatorName().or(Strings.EMPTY), 30));
        context.sendBroadcast(intent);
    }

    private String getSafeClippedString(String string, int maxLength) {
        if (Strings.isBlank(string)) {
            return Strings.EMPTY;
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

    private class PlayStateChangedSubscriber extends DefaultSubscriber<PlayStateEvent> {
        @Override
        public void onNext(PlayStateEvent state) {
            notifyPlayStateChanged(state.playSessionIsActive());
        }
    }

    private class PlayQueueChangedSubscriber extends DefaultSubscriber<CurrentPlayQueueItemEvent> {
        @Override
        public void onNext(CurrentPlayQueueItemEvent event) {
            PlayQueueItem playQueueItem = event.getCurrentPlayQueueItem();
            if (playQueueItem.isTrack()) {
                trackRepository.track(playQueueItem.getUrn()).subscribe(new CurrentTrackSubscriber());
            } else {
                resetTrackInformation();
            }
        }
    }

    private class CurrentTrackSubscriber extends DefaultSubscriber<Track> {
        @Override
        public void onNext(Track track) {
            notifyPlayQueueChanged(track);
        }
    }
}
