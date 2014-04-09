package com.soundcloud.android.playback.service;

import static com.soundcloud.android.playback.service.PlaybackService.Actions;
import static com.soundcloud.android.playback.service.PlaybackService.Broadcasts;
import static com.soundcloud.android.playback.service.PlaybackService.PlayExtras;

import com.google.common.primitives.Longs;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayControlEvent;
import com.soundcloud.android.utils.Log;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.List;

class PlaybackReceiver extends BroadcastReceiver {

    private PlaybackService mPlaybackService;
    private final AccountOperations mAccountOperations;
    private final PlayQueueManager mPlayQueueManager;
    private final EventBus mEventBus;

    public PlaybackReceiver(PlaybackService playbackService, AccountOperations accountOperations,
                            PlayQueueManager playQueueManager, EventBus eventBus) {
        mPlaybackService = playbackService;
        mAccountOperations = accountOperations;
        mPlayQueueManager = playQueueManager;
        mEventBus = eventBus;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(PlaybackService.TAG, "BroadcastReceiver#onReceive(" + action + ")");

        if (intent.hasExtra(PlayControlEvent.EXTRA_EVENT_SOURCE)) {
            trackPlayControlEvent(intent);
        }

        if (Actions.RESET_ALL.equals(action)) {
            mPlaybackService.resetAll();
            mPlayQueueManager.clearAll();

        } else if (mAccountOperations.soundCloudAccountExists()) {

            if (Actions.NEXT_ACTION.equals(action)) {
                if (mPlaybackService.next()) {
                    mPlaybackService.openCurrent();
                }
            } else if (Actions.PREVIOUS_ACTION.equals(action)) {
                if (mPlaybackService.prev()) {
                    mPlaybackService.openCurrent();
                }
            } else if (Actions.TOGGLEPLAYBACK_ACTION.equals(action)) {
                mPlaybackService.togglePlayback();
            } else if (Actions.PAUSE_ACTION.equals(action)) {
                mPlaybackService.pause();
            } else if (Broadcasts.UPDATE_WIDGET_ACTION.equals(action)) {
                // a widget was just added. Fake a playstate changed so it gets updated
                mPlaybackService.notifyChange(Broadcasts.PLAYSTATE_CHANGED);
            } else if (Actions.PLAY_ACTION.equals(action)) {
                handlePlayAction(intent);
            } else if (Actions.RETRY_RELATED_TRACKS.equals(action)) {
                mPlayQueueManager.retryRelatedTracksFetch();
            } else if (Broadcasts.PLAYQUEUE_CHANGED.equals(action)) {
                if (mPlaybackService.isWaitingForPlaylist()) {
                    mPlaybackService.openCurrent();
                }
            } else if (Actions.STOP_ACTION.equals(action)) {
                if (mPlaybackService.isSupposedToBePlaying()) {
                    mPlaybackService.saveProgressAndStop();
                } else {
                    // make sure we go to a stopped stat. No-op if there already
                    mPlaybackService.stop();
                }
            }
        } else {
            Log.e(PlaybackService.TAG, "Aborting playback service action, no soundcloud account");
        }
    }

    private void handlePlayAction(Intent intent) {
        if (intent.hasExtra(PlayExtras.TRACK_ID_LIST)) {

            final List<Long> trackIds = Longs.asList(intent.getLongArrayExtra(PlayExtras.TRACK_ID_LIST));
            final int startPosition = intent.getIntExtra(PlayExtras.START_POSITION, 0);
            final PlaySessionSource playSessionSource = intent.getParcelableExtra(PlayExtras.PLAY_SESSION_SOURCE);
            PlayQueue playQueue = PlayQueue.fromIdList(trackIds, startPosition, playSessionSource);

            mPlayQueueManager.setNewPlayQueue(playQueue, playSessionSource);
            mPlaybackService.openCurrent();

            if (playSessionSource.originatedInExplore() && !playQueue.isEmpty()) {
                mPlayQueueManager.fetchRelatedTracks(playQueue.getCurrentTrackId());
            }

        } else {
            Log.w(PlaybackService.TAG, "Received play intent without a play queue");
        }
    }

    private void trackPlayControlEvent(Intent intent) {
        String source = intent.getStringExtra(PlayControlEvent.EXTRA_EVENT_SOURCE);

        if (Actions.TOGGLEPLAYBACK_ACTION.equals(intent.getAction())) {
            mEventBus.publish(EventQueue.PLAY_CONTROL,
                    PlayControlEvent.toggle(source, mPlaybackService.isSupposedToBePlaying()));
        } else if (Actions.PLAY_ACTION.equals(intent.getAction())) {
            mEventBus.publish(EventQueue.PLAY_CONTROL, PlayControlEvent.play(source));
        } else if (Actions.PAUSE_ACTION.equals(intent.getAction())) {
            mEventBus.publish(EventQueue.PLAY_CONTROL, PlayControlEvent.pause(source));
        } else if (Actions.NEXT_ACTION.equals(intent.getAction())) {
            mEventBus.publish(EventQueue.PLAY_CONTROL, PlayControlEvent.skip(source));
        } else if (Actions.PREVIOUS_ACTION.equals(intent.getAction())) {
            mEventBus.publish(EventQueue.PLAY_CONTROL, PlayControlEvent.previous(source));
        }
    }
}
