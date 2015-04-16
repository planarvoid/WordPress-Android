package com.soundcloud.android.cast;

import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.MediaStatus;
import com.google.android.gms.common.images.WebImage;
import com.google.android.libraries.cast.companionlibrary.cast.VideoCastManager;
import com.google.android.libraries.cast.companionlibrary.cast.callbacks.VideoCastConsumerImpl;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.CastException;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.NoConnectionException;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.TransientNetworkDisconnectionException;
import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlaybackProgressEvent;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.playback.ProgressReporter;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.playback.service.Playa;
import com.soundcloud.android.playback.service.Playa.PlayaState;
import com.soundcloud.android.playback.service.Playa.Reason;
import com.soundcloud.android.playback.service.Playa.StateTransition;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.utils.CollectionUtils;
import com.soundcloud.android.utils.Log;
import com.soundcloud.propeller.PropertySet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import rx.Subscription;
import rx.subscriptions.Subscriptions;

import android.content.res.Resources;
import android.net.Uri;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class CastPlayer extends VideoCastConsumerImpl implements ProgressReporter.ProgressPusher {
    @VisibleForTesting
    static final String KEY_URN = "urn";
    static final String KEY_PLAY_QUEUE = "play_queue";

    private final static String TAG = "CastPlayer";

    private final VideoCastManager castManager;
    private final ProgressReporter progressReporter;
    private final ImageOperations imageOperations;
    private final Resources resources;
    private final EventBus eventBus;
    private final TrackRepository trackRepository;
    private final PlayQueueManager playQueueManager;

    private Subscription trackInfoSubscription = Subscriptions.empty();

    @Inject
    public CastPlayer(VideoCastManager castManager, ProgressReporter progressReporter,
                      ImageOperations imageOperations, Resources resources, EventBus eventBus, TrackRepository trackRepository, PlayQueueManager playQueueManager) {
        this.castManager = castManager;
        this.progressReporter = progressReporter;
        this.imageOperations = imageOperations;
        this.resources = resources;
        this.eventBus = eventBus;
        this.trackRepository = trackRepository;
        this.playQueueManager = playQueueManager;

        castManager.addVideoCastConsumer(this);
        progressReporter.setProgressPusher(this);
    }

    @Override
    public void onRemoteMediaPlayerStatusUpdated() {
        onMediaPlayerStatusUpdatedListener(castManager.getPlaybackStatus(), castManager.getIdleReason());
    }

    public boolean isConnected(){
        return castManager.isConnected();
    }

    @Override
    public void onDisconnected() {
        reportStateChange(getStateTransition(PlayaState.IDLE, Reason.NONE)); // possibly show disconnect error here instead?
    }

    public void onMediaPlayerStatusUpdatedListener(int playerState, int idleReason) {
        switch (playerState) {
            case MediaStatus.PLAYER_STATE_PLAYING:
                reportStateChange(getStateTransition(PlayaState.PLAYING, Reason.NONE));
                break;

            case MediaStatus.PLAYER_STATE_PAUSED:
                // we have to suppress pause events while we should be playing.
                // The receiver sends thes back often, as in when the track first loads, even if autoplay is true
                reportStateChange(getStateTransition(PlayaState.IDLE, Reason.NONE));
                break;

            case MediaStatus.PLAYER_STATE_BUFFERING:
                reportStateChange(getStateTransition(PlayaState.BUFFERING, Reason.NONE));
                break;

            case MediaStatus.PLAYER_STATE_IDLE:
                final Reason translatedIdleReason = getTranslatedIdleReason(idleReason);
                if (translatedIdleReason != null) {
                    reportStateChange(getStateTransition(PlayaState.IDLE, translatedIdleReason));
                }
                break;

            case MediaStatus.PLAYER_STATE_UNKNOWN:
                Log.e(this, "received an unknown media status"); // not sure when this happens yet
                break;
            default:
                throw new IllegalArgumentException("Unknown Media State code returned " + playerState);
        }
    }

    private void reportStateChange(StateTransition stateTransition) {
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, stateTransition);
        final boolean playerPlaying = stateTransition.isPlayerPlaying();
        if (playerPlaying){
            progressReporter.start();
        } else {
            progressReporter.stop();
        }
    }

    @Override
    public void pushProgress() {
        try {
            final PlaybackProgress playbackProgress = new PlaybackProgress(castManager.getCurrentMediaPosition(), castManager.getMediaDuration());
            eventBus.publish(EventQueue.PLAYBACK_PROGRESS, new PlaybackProgressEvent(playbackProgress, getCurrentPlayingUrn()));
        } catch (TransientNetworkDisconnectionException | NoConnectionException e) {
            Log.e(TAG, "Unable to report progress", e);
        }
    }

    private StateTransition getStateTransition(PlayaState state, Reason reason) {
        return new StateTransition(state, reason, getCurrentPlayingUrn(), getProgress(), getDuration());
    }

    @Nullable
    private Reason getTranslatedIdleReason(int idleReason) {
        switch (idleReason) {
            case MediaStatus.IDLE_REASON_ERROR:
                return Reason.ERROR_FAILED;
            case MediaStatus.IDLE_REASON_FINISHED:
                return Reason.TRACK_COMPLETE;
            case MediaStatus.IDLE_REASON_CANCELED:
                return Reason.NONE;
            default:
                // do not fail fast, we want to ignore non-handled codes
                return null;
        }
    }

    public void playCurrent() {
        playCurrent(0);
    }

    public void playCurrent(final long time) {
        trackInfoSubscription.unsubscribe();
        Urn currentTrackUrn = playQueueManager.getCurrentTrackUrn();
        trackInfoSubscription = trackRepository.track(currentTrackUrn).subscribe(new TrackInformationSubscriber(time, currentTrackUrn));
    }

    private void play(PropertySet track, long fromPos) {
        final Urn urn = track.get(TrackProperty.URN);
        if (isCurrentlyLoadedInPlayer(urn)){
            reconnectToExistingSession();

        } else {
            try {
                MediaMetadata mediaMetadata = new MediaMetadata(MediaMetadata.MEDIA_TYPE_MUSIC_TRACK);
                mediaMetadata.putString(MediaMetadata.KEY_TITLE, track.get(TrackProperty.TITLE));
                mediaMetadata.putString(MediaMetadata.KEY_ARTIST, track.get(TrackProperty.CREATOR_NAME));
                mediaMetadata.putString(KEY_URN, urn.toString());
                mediaMetadata.addImage(new WebImage(Uri.parse(imageOperations.getUrlForLargestImage(resources, urn))));

                MediaInfo mediaInfo = new MediaInfo.Builder(urn.toString())
                        .setContentType("audio/mpeg")
                        .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                        .setMetadata(mediaMetadata)
                        .build();

                reportStateChange(new StateTransition(PlayaState.BUFFERING, Reason.NONE, urn));
                final JSONObject playQueueObject = createPlayQueueObject();
                castManager.loadMedia(mediaInfo, true, (int) fromPos, playQueueObject);

            } catch (TransientNetworkDisconnectionException | NoConnectionException e) {
                Log.e(TAG, "Unable to load track", e);
            }
        }
    }

    private JSONObject createPlayQueueObject() {
        JSONObject playQueue = new JSONObject();
        try {
            playQueue.put(KEY_PLAY_QUEUE, getPlayQueueArray());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return playQueue;
    }

    private JSONArray getPlayQueueArray() {
        return new JSONArray(CollectionUtils.urnsToStrings(playQueueManager.getCurrentQueueAsUrnList()));
    }

    private boolean isCurrentlyLoadedInPlayer(Urn urn) {
        final Urn currentPlayingUrn = getCurrentPlayingUrn();
        return currentPlayingUrn != Urn.NOT_SET && currentPlayingUrn.equals(urn);
    }

    private Urn getCurrentPlayingUrn(){
        MediaInfo mediaInfo = getCurrentRemoteMediaInfo();
        return mediaInfo == null ? Urn.NOT_SET : getUrnFromMediaMetadata(mediaInfo);
    }

    @Nullable
    private MediaInfo getCurrentRemoteMediaInfo(){
        try {
            return castManager.getRemoteMediaInformation();
        } catch (TransientNetworkDisconnectionException | NoConnectionException e) {
            Log.e(TAG, "Unable to get remote media information", e);
        }
        return null;
    }

    private void reconnectToExistingSession() {
        onMediaPlayerStatusUpdatedListener(castManager.getPlaybackStatus(), castManager.getIdleReason());
    }

    public boolean resume() {
        try {
            castManager.play();
            return true;
        } catch (CastException | TransientNetworkDisconnectionException | NoConnectionException | IllegalStateException e) {
            Log.e(TAG, "Unable to resume playback", e);
        }
        return false;
    }

    public void pause() {
        try {
            castManager.pause();
        } catch (CastException | TransientNetworkDisconnectionException | NoConnectionException | IllegalStateException  e) {
            Log.e(TAG, "Unable to pause playback", e);
        }
    }

    public void togglePlayback() {
        try {
            castManager.togglePlayback();
        } catch (CastException | TransientNetworkDisconnectionException | NoConnectionException | IllegalStateException  e) {
            Log.e(TAG, "Unable to pause playback", e);
        }
    }

    public long seek(long ms) {
        try {
            castManager.seek((int) ms);
        } catch (TransientNetworkDisconnectionException | NoConnectionException | IllegalStateException  e) {
            Log.e(TAG, "Unable to seek", e);
        }
        return ms;
    }

    public long getProgress() {
        try {
            return castManager.getCurrentMediaPosition();
        } catch (TransientNetworkDisconnectionException | NoConnectionException | IllegalStateException  e) {
            Log.e(TAG, "Unable to get progress", e);
        }
        return 0;
    }

    private long getDuration() {
        try {
            return castManager.getMediaDuration();
        } catch (TransientNetworkDisconnectionException | NoConnectionException | IllegalStateException  e) {
            Log.e(TAG, "Unable to get duration", e);
        }
        return 0;
    }

    public void stop() {
        pause(); // stop has more long-running implications in cast. pause is sufficient
    }

    public void destroy() {
        castManager.onDeviceSelected(null);
        castManager.removeVideoCastConsumer(this);
    }

    public static Urn getUrnFromMediaMetadata(MediaInfo mediaInfo){
        return mediaInfo == null ? Urn.NOT_SET : new Urn(mediaInfo.getMetadata().getString(CastPlayer.KEY_URN));
    }

    private class TrackInformationSubscriber extends DefaultSubscriber<PropertySet> {
        private final long playFromPosition;
        private final Urn urn;

        TrackInformationSubscriber(long playFromPosition, Urn urn) {
            this.playFromPosition = playFromPosition;
            this.urn = urn;
        }

        @Override
        public void onError(Throwable throwable) {
            reportStateChange(new Playa.StateTransition(Playa.PlayaState.IDLE, Playa.Reason.ERROR_FAILED, urn));
        }

        @Override
        public void onNext(PropertySet track) {
            play(track, playFromPosition);
        }
    }

}
