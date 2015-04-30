package com.soundcloud.android.playback;

import com.soundcloud.android.R;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUICommand;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.playback.ui.view.PlaybackToastHelper;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.utils.ErrorUtils;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.widget.Toast;

import javax.inject.Inject;

public class ExpandPlayerSubscriber extends DefaultSubscriber<PlaybackResult> {
    public static final int EXPAND_DELAY_MILLIS = 100;

    private final EventBus eventBus;
    private final Context context;
    private final PlaySessionStateProvider playSessionStateProvider;

    private final Handler expandDelayHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.expandPlayer());
            eventBus.publish(EventQueue.TRACKING, UIEvent.fromPlayerOpen(UIEvent.METHOD_TRACK_PLAY));
        }
    };

    @Inject
    public ExpandPlayerSubscriber(EventBus eventBus,
                                  Context context,
                                  PlaySessionStateProvider playSessionStateProvider) {
        this.eventBus = eventBus;
        this.context = context;
        this.playSessionStateProvider = playSessionStateProvider;
    }

    @Override
    public void onNext(PlaybackResult result) {
        switch (result) {
            case SUCCESS:
                expandDelayHandler.sendEmptyMessageDelayed(0, EXPAND_DELAY_MILLIS);
                break;
            case UNSKIPPABLE:
                showUnskippableAdToast();
                break;
            case TRACK_UNAVAILABLE_OFFLINE:
                showTrackUnavailableOfflineToast();
                break;
            case TRACK_NOT_FOUND:
                showUnableToFindTracksToPlayToast();
                break;
            default:
                throw new IllegalStateException("Unknown result:" + result);
        }
    }

    public void showUnskippableAdToast() {
        Toast.makeText(context, playSessionStateProvider.isPlaying()
                        ? R.string.ad_in_progress
                        : R.string.ad_resume_playing_to_continue,
                Toast.LENGTH_SHORT).show();
    }

    private void showTrackUnavailableOfflineToast() {
        Toast.makeText(context, R.string.offline_track_not_available, Toast.LENGTH_SHORT).show();
    }

    private void showUnableToFindTracksToPlayToast() {
        Toast.makeText(context, R.string.playback_missing_playable_tracks, Toast.LENGTH_SHORT).show();
    }


}
