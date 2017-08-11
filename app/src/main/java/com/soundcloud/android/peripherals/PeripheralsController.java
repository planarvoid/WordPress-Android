package com.soundcloud.android.peripherals;

import com.soundcloud.android.events.CurrentPlayQueueItemEvent;
import com.soundcloud.android.events.CurrentUserChangedEvent;
import com.soundcloud.android.playback.Durations;
import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.android.playback.PlayStateEvent;
import com.soundcloud.android.rx.observers.DefaultMaybeObserver;
import com.soundcloud.android.tracks.Track;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.java.strings.Strings;

import android.content.Context;
import android.content.Intent;

import javax.inject.Inject;

public class PeripheralsController {

    private static final String AVRCP_PLAYSTATE_CHANGED = "com.android.music.playstatechanged";
    private static final String AVRCP_META_CHANGED = "com.android.music.metachanged";

    private final Context context;
    private final TrackRepository trackRepository;

    @Inject
    PeripheralsController(Context context, TrackRepository trackRepository) {
        this.context = context;
        this.trackRepository = trackRepository;
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
        intent.putExtra("artist", getSafeClippedString(track.creatorName(), 30));
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

    void onCurrentUserChanged(CurrentUserChangedEvent event) {
        resetTrackInformation();
    }

    void onPlayStateEvent(PlayStateEvent state) {
        notifyPlayStateChanged(state.playSessionIsActive());
    }

    void onCurrentPlayQueueItem(CurrentPlayQueueItemEvent event) {
        PlayQueueItem playQueueItem = event.getCurrentPlayQueueItem();
        if (playQueueItem.isTrack()) {
            trackRepository.track(playQueueItem.getUrn()).subscribe(new CurrentTrackSubscriber());
        } else {
            resetTrackInformation();
        }
    }

    private class CurrentTrackSubscriber extends DefaultMaybeObserver<Track> {
        @Override
        public void onSuccess(Track track) {
            super.onSuccess(track);
            notifyPlayQueueChanged(track);
        }
    }

}
