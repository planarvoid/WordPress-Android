package com.soundcloud.android.cast.api;

import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaStatus;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.media.RemoteMediaClient;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.ApiMapperException;
import com.soundcloud.android.cast.RemoteMediaClientLogger;
import com.soundcloud.android.cast.SimpleRemoteMediaClientListener;
import com.soundcloud.android.playback.PlayStateReason;
import com.soundcloud.android.playback.PlaybackConstants;
import com.soundcloud.android.utils.Log;
import com.soundcloud.annotations.VisibleForTesting;
import com.soundcloud.java.optional.Optional;
import org.json.JSONException;
import org.json.JSONObject;
import rx.functions.Action2;

import android.support.annotation.Nullable;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;

@Singleton
public class CastProtocol extends SimpleRemoteMediaClientListener {

    public static final String TAG = "GoogleCast";
    private static final String PROTOCOL_CHANNEL_NAMESPACE = "urn:x-cast:com.soundcloud.chromecast";
    @VisibleForTesting static final String MIME_TYPE_AUDIO_MPEG = "audio/mpeg";
    private static final long SEEK_CORRECTION_ON_OVERFLOW = 100L;

    private static final String UPDATE_QUEUE = "UPDATE_QUEUE";

    private Optional<CastSession> castSession = Optional.absent();
    private Listener listener;
    private Optional<RemoteState> remoteState = Optional.absent();
    private boolean loadRequestSentForSession;
    private final CastJsonHandler jsonHandler;
    private final AccountOperations accountOperations;

    public interface Listener extends RemoteMediaClient.ProgressListener {
        void onIdle(long progress, long duration, PlayStateReason idleReason);

        void onPlaying(long progress, long duration);

        void onPaused(long progress, long duration);

        void onBuffering(long progress, long duration);

        void onRemoteEmptyStateFetched();

        void onQueueReceived(CastPlayQueue castPlayQueue);
    }

    @Inject
    CastProtocol(CastJsonHandler castJsonHandler,
                 AccountOperations accountOperations) {
        this.jsonHandler = castJsonHandler;
        this.accountOperations = accountOperations;
    }

    public void registerCastSession(CastSession castSession) {
        Log.d(TAG, "CastProtocol::registerCastSession() for session: " + castSession);
        this.castSession = Optional.of(castSession);
    }

    public void unregisterCastSession() {
        Log.d(TAG, "CastProtocol::unregisterCastSession() called");
        this.castSession = Optional.absent();
        this.remoteState = Optional.absent();
        this.loadRequestSentForSession = false;
    }

    private CastCredentials getCredentials() {
        return new CastCredentials(accountOperations.getSoundCloudToken());
    }

    public void requestStatusUpdate() {
        Log.w(TAG, "CastProtocol::requestStatusUpdate() called");
        getRemoteMediaClient().requestStatus();
    }

    public void play() {
        getRemoteMediaClient().play();
    }

    public void pause() {
        getRemoteMediaClient().pause();
    }

    public void togglePlayback() {
        getRemoteMediaClient().togglePlayback();
    }

    public void seek(long position, Action2<Long, Long> callback) {
        long trackDuration = getRemoteMediaClient().getStreamDuration();
        long correctedPosition = correctSeekingPositionIfNeeded(position, trackDuration);

        callback.call(correctedPosition, trackDuration);
        getRemoteMediaClient().seek(correctedPosition);
    }

    private long correctSeekingPositionIfNeeded(long seekPosition, long trackDuration) {
        if (seekPosition >= trackDuration) {
            // if the user tries to seek past the end of the track we have to correct it
            // to avoid weird states because of the syncing time between cast sender & receiver
            return trackDuration - SEEK_CORRECTION_ON_OVERFLOW;
        } else {
            return seekPosition;
        }
    }

    public void sendLoad(String contentId, boolean autoplay, long playPosition, CastPlayQueue playQueue) {
        this.loadRequestSentForSession = true;

        MediaInfo mediaInfo = new MediaInfo.Builder(contentId)
                .setContentType(MIME_TYPE_AUDIO_MPEG)
                .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                .build();
        getRemoteMediaClient().load(mediaInfo, autoplay, playPosition, jsonHandler.toJson(playQueue.withCredentials(getCredentials())));
        Log.d(TAG, "CastProtocol::sendLoad" + (autoplay ? " in autoplay" : "") + " for pos. " + playPosition + " with playQueue = " + playQueue);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
        if (getRemoteMediaClient() != null) {
            getRemoteMediaClient().addListener(this);
            getRemoteMediaClient().addProgressListener(listener, PlaybackConstants.PROGRESS_DELAY_MS);
        }
    }

    public void removeListener(Listener listener) {
        if (getRemoteMediaClient() != null) {
            getRemoteMediaClient().removeListener(this);
            getRemoteMediaClient().removeProgressListener(listener);
        }
        this.listener = null;
    }

    public void sendUpdateQueue(CastPlayQueue castPlayQueue) {
        CastMessage message = CastMessage.create(UPDATE_QUEUE, castPlayQueue.withCredentials(getCredentials()));
        sendMessage(message);
    }

    private void sendMessage(CastMessage message) {
        try {
            String json = jsonHandler.toString(message);
            Log.i(TAG, "CastProtocol::sendMessage = " + json);
            castSession.get().sendMessage(PROTOCOL_CHANNEL_NAMESPACE, json);
        } catch (ApiMapperException e) {
            Log.e(TAG, "CastProtocol::sendMessage - could not map message to JSON: " + message, e);
        }
    }

    @Override
    public void onStatusUpdated() {
        RemoteMediaClient remoteMediaClient = getRemoteMediaClient();
        if (listener != null && remoteMediaClient != null) {
            RemoteMediaClientLogger.logState("onStatusUpdated", getRemoteMediaClient());

            if (wasIdleReceivedAfterLoad(getRemoteMediaClient().getPlayerState())) {
                Log.w(TAG, "onStatusUpdated() ignored IDLE state: those shouldn't exist after LOAD has been issued");
            } else {
                dispatchStatusUpdate(remoteMediaClient);
            }
        }
    }

    private void dispatchStatusUpdate(RemoteMediaClient remoteMediaClient) {
        final long remoteTrackProgress = remoteMediaClient.getApproximateStreamPosition();
        final long remoteTrackDuration = remoteMediaClient.getStreamDuration();

        switch (remoteMediaClient.getPlayerState()) {
            case MediaStatus.PLAYER_STATE_PLAYING:
                listener.onPlaying(remoteTrackProgress, remoteTrackDuration);
                break;
            case MediaStatus.PLAYER_STATE_PAUSED:
                listener.onPaused(remoteTrackProgress, remoteTrackDuration);
                break;
            case MediaStatus.PLAYER_STATE_BUFFERING:
                listener.onBuffering(remoteTrackProgress, remoteTrackDuration);
                break;
            case MediaStatus.PLAYER_STATE_IDLE:
                final PlayStateReason translatedIdleReason = getTranslatedIdleReason(remoteMediaClient.getIdleReason());
                if (translatedIdleReason != null) {
                    listener.onIdle(remoteTrackProgress, remoteTrackDuration, translatedIdleReason);
                }
                break;
            case MediaStatus.PLAYER_STATE_UNKNOWN:
                Log.e(TAG, "Received an unknown media status");
                break;
            default:
                throw new IllegalArgumentException("Unknown Media State code returned " + remoteMediaClient.getPlayerState());
        }
    }

    @Nullable
    private PlayStateReason getTranslatedIdleReason(int idleReason) {
        switch (idleReason) {
            case MediaStatus.IDLE_REASON_ERROR:
                return PlayStateReason.ERROR_FAILED;
            case MediaStatus.IDLE_REASON_FINISHED:
                return PlayStateReason.PLAYBACK_COMPLETE;
            case MediaStatus.IDLE_REASON_CANCELED:
                return PlayStateReason.NONE;
            case MediaStatus.IDLE_REASON_INTERRUPTED:
            case MediaStatus.IDLE_REASON_NONE:
            default:
                // do not fail fast, we want to ignore non-handled codes
                return null;
        }
    }

    @Override
    public void onMetadataUpdated() {
        RemoteMediaClient remoteMediaClient = getRemoteMediaClient();
        if (listener != null && remoteMediaClient != null) {
            RemoteMediaClientLogger.logState("onMetadataUpdated", remoteMediaClient);

            int playerState = remoteMediaClient.getPlayerState();
            if (wasIdleReceivedAfterLoad(playerState)) {
                Log.w(TAG, "onMetadataUpdated() ignored IDLE state: those shouldn't exist after LOAD has been issued");
            } else {
                Optional<JSONObject> remoteLoadedData = getRemoteLoadedData(remoteMediaClient);

                try {
                    if (remoteLoadedData.isPresent()) {
                        if (playerState != MediaStatus.PLAYER_STATE_UNKNOWN) {
                            onNonEmptyMetadataReceived(playerState, remoteLoadedData.get());
                        }
                    } else {
                        if (playerState == MediaStatus.PLAYER_STATE_IDLE) {
                            onIdleEmptyMetadataReceived();
                        }
                    }
                } catch (IOException | ApiMapperException | JSONException e) {
                    Log.e(TAG, "Could not parse received queue", e);
                }
            }
        }
    }

    private boolean wasIdleReceivedAfterLoad(int playerState) {
        return loadRequestSentForSession && playerState == MediaStatus.PLAYER_STATE_IDLE;
    }

    private Optional<JSONObject> getRemoteLoadedData(RemoteMediaClient remoteMediaClient) {
        if (remoteMediaClient.getMediaInfo() != null && remoteMediaClient.getMediaInfo().getCustomData() != null) {
            return Optional.of(remoteMediaClient.getMediaInfo().getCustomData());
        } else {
            return Optional.absent();
        }
    }

    private void onNonEmptyMetadataReceived(int playerState, JSONObject remoteLoadedData) throws IOException, ApiMapperException, JSONException {
        CastPlayQueue castPlayQueue = jsonHandler.parseCastPlayQueue(remoteLoadedData);
        if (hasStateChanged(playerState, castPlayQueue.revision())) {
            RemoteMediaClientLogger.logState("onNonEmptyMetadataReceived", getRemoteMediaClient());
            listener.onQueueReceived(castPlayQueue);
            updateState(playerState, castPlayQueue.revision());
        } else {
            RemoteMediaClientLogger.logState("Swallowed State", getRemoteMediaClient());
        }
    }

    private void onIdleEmptyMetadataReceived() {
        final int playerState = MediaStatus.PLAYER_STATE_IDLE;
        if (hasStateChanged(playerState, Optional.absent())) {
            RemoteMediaClientLogger.logState("onIdleEmptyMetadataReceived", getRemoteMediaClient());
            listener.onRemoteEmptyStateFetched();
            updateState(playerState, Optional.absent());
        } else {
            RemoteMediaClientLogger.logState("Swallowed State", getRemoteMediaClient());
        }
    }

    private boolean isConnected() {
        return castSession.isPresent() && castSession.get().isConnected();
    }

    @Nullable
    private RemoteMediaClient getRemoteMediaClient() {
        return isConnected() ? castSession.get().getRemoteMediaClient() : null;
    }

    private boolean hasStateChanged(int playerState, Optional<String> revision) {
        return !remoteState.isPresent() || playerState != remoteState.get().playerState || !revision.equals(remoteState.get().revision);
    }

    private void updateState(int playerState, Optional<String> revision) {
        this.remoteState = Optional.of(new RemoteState(playerState, revision));
    }

    private static class RemoteState {
        private int playerState;
        private Optional<String> revision;

        RemoteState(int playerState, Optional<String> revision) {
            this.playerState = playerState;
            this.revision = revision;
        }
    }
}
