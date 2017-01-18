package com.soundcloud.android.cast;

import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaStatus;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.media.RemoteMediaClient;
import com.soundcloud.android.api.ApiMapperException;
import com.soundcloud.android.playback.PlaybackConstants;
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
    @VisibleForTesting static final String PROTOCOL_CHANNEL_NAMESPACE = "urn:x-cast:com.soundcloud.chromecast";
    @VisibleForTesting static final String MIME_TYPE_AUDIO_MPEG = "audio/mpeg";

    private static final String UPDATE_QUEUE = "UPDATE_QUEUE";

    private Optional<CastSession> castSession = Optional.absent();
    private Listener listener;
    private final CastJsonHandler jsonHandler;
    private CastCredentials credentials;

    public interface Listener extends RemoteMediaClient.ProgressListener {
        void onStatusUpdated();

        void onRemoteEmptyStateFetched();

        void onQueueReceived(CastPlayQueue castPlayQueue);
    }

    @Inject
    CastProtocol(CastJsonHandler castJsonHandler) {
        this.jsonHandler = castJsonHandler;
    }

    public void registerCastSession(CastSession castSession) {
        Log.d(TAG, "CastProtocol::registerCastSession() for session: " + castSession);
        this.castSession = Optional.of(castSession);
    }

    public void unregisterCastSession() {
        Log.d(TAG, "CastProtocol::unregisterCastSession() called");
        this.castSession = Optional.absent();
    }

    public void attachCredentials(CastCredentials credentials) {
        Log.d(TAG, "CastProtocol::attachCredentials for token " + credentials);
        this.credentials = credentials;
    }

    public void sendLoad(String contentId, boolean autoplay, long playPosition, JSONObject customData) {
        Log.d(TAG, "CastProtocol::sendLoad" + (autoplay ? " in autoplay" : "") + " with customData = " + customData);
        MediaInfo mediaInfo = new MediaInfo.Builder(contentId)
                .setContentType(MIME_TYPE_AUDIO_MPEG)
                .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                .build();
        getRemoteMediaClient().load(mediaInfo, autoplay, playPosition, customData);
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
            castPlayQueue.setCredentials(credentials);
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
        if (listener != null) {
            listener.onStatusUpdated();
        }
    }

    @Override
    public void onMetadataUpdated() {
        RemoteMediaClientLogger.logState("onMetadataUpdated", getRemoteMediaClient());
        if (listener != null && getRemoteMediaClient() != null) {
            int playerState = getRemoteMediaClient().getPlayerState();
            Optional<JSONObject> remoteLoadedData = getRemoteLoadedData();

            try {
                if (remoteLoadedData.isPresent()) {
                    if (playerState != MediaStatus.PLAYER_STATE_UNKNOWN) {
                        listener.onQueueReceived(jsonHandler.parseCastPlayQueue(remoteLoadedData.get()));
                    }
                } else {
                    if (playerState == MediaStatus.PLAYER_STATE_IDLE) {
                        listener.onRemoteEmptyStateFetched();
                    }
                }
            } catch (IOException | ApiMapperException | JSONException e) {
                Log.e(TAG, "Could not parse received queue");
            }
        }
    }

    private Optional<JSONObject> getRemoteLoadedData() {
        if (getRemoteMediaClient().getMediaInfo() != null && getRemoteMediaClient().getMediaInfo().getCustomData() != null) {
            return Optional.of(getRemoteMediaClient().getMediaInfo().getCustomData());
        } else {
            return Optional.absent();
        }
    }

    public boolean isConnected() {
        return castSession.isPresent() && castSession.get().isConnected();
    }
}
