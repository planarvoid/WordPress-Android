package com.soundcloud.android.peripherals;

import static com.soundcloud.android.playback.service.Playa.StateTransition;

import com.soundcloud.android.events.CurrentUserChangedEvent;
import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayQueueEvent;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.track.TrackOperations;
import com.soundcloud.android.utils.ScTextUtils;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;

import android.content.Context;
import android.content.Intent;

import javax.inject.Inject;

public class PeripheralsController {

    private static final String AVRCP_PLAYSTATE_CHANGED = "com.android.music.playstatechanged";
    private static final String AVRCP_META_CHANGED = "com.android.music.metachanged";

    private final Context context;
    private final EventBus eventBus;
    private final PlayQueueManager playQueueManager;
    private final TrackOperations trackOperations;

    @Inject
    public PeripheralsController(Context context, EventBus eventBus, PlayQueueManager playQueueManager, TrackOperations trackOperations) {
        this.context = context;
        this.eventBus = eventBus;
        this.playQueueManager = playQueueManager;
        this.trackOperations = trackOperations;
    }

    public void subscribe() {
        eventBus.subscribe(EventQueue.CURRENT_USER_CHANGED, new CurrentUserChangedSubscriber());
        eventBus.subscribe(EventQueue.PLAYBACK_STATE_CHANGED, new PlayStateChangedSubscriber());
        eventBus.subscribe(EventQueue.PLAY_QUEUE, new PlayQueueChangedSubscriber());
    }

    public void notifyMetaChanged(Context context, Track track, boolean isPlaying) {
        notifyChangeViaGoogleMusic(context, track, isPlaying, AVRCP_META_CHANGED);
    }

    public void notifyPlayStateChanged(Context context, Track track, boolean isPlaying) {
        notifyChangeViaGoogleMusic(context, track, isPlaying, AVRCP_PLAYSTATE_CHANGED);
    }

    private void notifyChangeViaGoogleMusic(Context context, Track track, boolean isPlaying, String action) {
        if (track != null) {
            Intent intent = new Intent(action);
            intent.putExtra("id", track.getId());
            intent.putExtra("track", ScTextUtils.getClippedString(track.getTitle(), 40));
            intent.putExtra("playing", isPlaying);
            intent.putExtra("duration", track.duration);

            if (track.getUserName() != null) {
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

    private void notifyPlayQueueChanged(Track track) {
        Intent intent = new Intent(AVRCP_META_CHANGED);
        intent.putExtra("id", track.getId());
        intent.putExtra("track", ScTextUtils.getClippedString(track.getTitle(), 40));
        intent.putExtra("duration", track.duration);

        if (track.getUserName() != null) {
            intent.putExtra("artist", ScTextUtils.getClippedString(track.getUserName(), 30));
        }

        context.sendBroadcast(intent);
    }

    private void resetTrackInformation() {
        Intent intent = new Intent(AVRCP_META_CHANGED);
        intent.putExtra("id", "");
        intent.putExtra("track", "");
        intent.putExtra("duration", 0);
        intent.putExtra("artist", "");

        context.sendBroadcast(intent);
    }

    private Observable<Track> loadCurrentTrack() {
        return trackOperations.loadTrack(playQueueManager.getCurrentTrackId(), AndroidSchedulers.mainThread());
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

    private class PlayQueueChangedSubscriber extends DefaultSubscriber<PlayQueueEvent> {
        @Override
        public void onNext(PlayQueueEvent event) {
            loadCurrentTrack().subscribe(new CurrentTrackSubscriber());
        }
    }

    private class CurrentTrackSubscriber extends DefaultSubscriber<Track> {
        @Override
        public void onNext(Track track) {
            notifyPlayQueueChanged(track);
        }
    }
}
