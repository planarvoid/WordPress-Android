package com.soundcloud.android.cast;

import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaStatus;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.media.RemoteMediaClient;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.ApiMapperException;
import com.soundcloud.android.playback.PlaybackConstants;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.utils.Log;
import com.soundcloud.annotations.VisibleForTesting;
import com.soundcloud.java.optional.Optional;
import org.json.JSONException;
import org.json.JSONObject;

import android.support.annotation.Nullable;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;

@Singleton
public class CastProtocol extends SimpleRemoteMediaClientListener {

    public static final String TAG = "GoogleCast";
    private static final String PROTOCOL_CHANNEL_NAMESPACE = "urn:x-cast:com.soundcloud.chromecast";
    @VisibleForTesting static final String MIME_TYPE_AUDIO_MPEG = "audio/mpeg";

    private static final String UPDATE_QUEUE = "UPDATE_QUEUE";

    private Optional<CastSession> castSession = Optional.absent();
    private Listener listener;
    private Optional<RemoteState> remoteState = Optional.absent();
    private boolean loadRequestSentForSession;
    private final CastJsonHandler jsonHandler;
    private final AccountOperations accountOperations;
    private final FeatureFlags featureFlags;

    public interface Listener extends RemoteMediaClient.ProgressListener {
        void onStatusUpdated();

        void onRemoteEmptyStateFetched();

        void onQueueReceived(CastPlayQueue castPlayQueue);
    }

    @Inject
    CastProtocol(CastJsonHandler castJsonHandler,
                 AccountOperations accountOperations,
                 FeatureFlags featureFlags) {
        this.jsonHandler = castJsonHandler;
        this.accountOperations = accountOperations;
        this.featureFlags = featureFlags;
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
        return new CastCredentials(accountOperations.getSoundCloudToken(), featureFlags);
    }

    public void sendLoad(String contentId, boolean autoplay, long playPosition, CastPlayQueue playQueue) {
        this.loadRequestSentForSession = true;

        MediaInfo mediaInfo = new MediaInfo.Builder(contentId)
                .setContentType(MIME_TYPE_AUDIO_MPEG)
                .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                .build();
        playQueue.setCredentials(getCredentials());
        getRemoteMediaClient().load(mediaInfo, autoplay, playPosition, jsonHandler.toJson(playQueue));
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

    @Nullable
    public RemoteMediaClient getRemoteMediaClient() {
        return isConnected() ? castSession.get().getRemoteMediaClient() : null;
    }

    public void sendUpdateQueue(CastPlayQueue castPlayQueue) {
        CastMessage message = CastMessage.create(UPDATE_QUEUE, castPlayQueue);
        attachCredentialsToMessage(message);
        sendMessage(message);
    }

    private void attachCredentialsToMessage(CastMessage castMessage) {
        CastPlayQueue castPlayQueue = castMessage.payload();
        if (castPlayQueue != null) {
            castPlayQueue.setCredentials(getCredentials());
        } else {
            Log.e(TAG, "CastProtocol::attachCredentialsToMessage - Tried to attach credentials to null payload message: " + castMessage);
        }
    }

    private void sendMessage(CastMessage message) {
        try {
            String json = jsonHandler.toString(message);
            Log.i(TAG, "CastProtocol::sendMessage = " + json);
            castSession.get().sendMessage(PROTOCOL_CHANNEL_NAMESPACE, json);
        } catch (ApiMapperException e) {
            Log.e(TAG, "CastProtocol::sendMessage - could not map message to JSON: " + message);
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
                listener.onStatusUpdated();
            }
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
                    Log.e(TAG, "Could not parse received queue");
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
        if (hasStateChanged(playerState, Optional.fromNullable(castPlayQueue.getRevision()))) {
            RemoteMediaClientLogger.logState("onNonEmptyMetadataReceived", getRemoteMediaClient());
            listener.onQueueReceived(castPlayQueue);
            updateState(playerState, castPlayQueue.getRevision());
        } else {
            RemoteMediaClientLogger.logState("Swallowed State", getRemoteMediaClient());
        }
    }

    private void onIdleEmptyMetadataReceived() {
        final int playerState = MediaStatus.PLAYER_STATE_IDLE;
        if (hasStateChanged(playerState, Optional.absent())) {
            RemoteMediaClientLogger.logState("onIdleEmptyMetadataReceived", getRemoteMediaClient());
            listener.onRemoteEmptyStateFetched();
            updateState(playerState, null);
        } else {
            RemoteMediaClientLogger.logState("Swallowed State", getRemoteMediaClient());
        }
    }

    public boolean isConnected() {
        return castSession.isPresent() && castSession.get().isConnected();
    }

    private boolean hasStateChanged(int playerState, Optional<String> revision) {
        return !remoteState.isPresent() || playerState != remoteState.get().playerState || !revision.equals(remoteState.get().revision);
    }

    private void updateState(int playerState, String revision) {
        this.remoteState = Optional.of(new RemoteState(playerState, revision));
    }

    private static class RemoteState {
        private int playerState;
        private Optional<String> revision;

        RemoteState(int playerState, String revision) {
            this.playerState = playerState;
            this.revision = Optional.fromNullable(revision);
        }
    }
}
