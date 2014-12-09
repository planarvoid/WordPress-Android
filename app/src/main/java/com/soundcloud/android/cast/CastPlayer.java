package com.soundcloud.android.cast;

import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.MediaStatus;
import com.google.android.gms.common.images.WebImage;
import com.google.sample.castcompanionlibrary.cast.VideoCastManager;
import com.google.sample.castcompanionlibrary.cast.callbacks.VideoCastConsumerImpl;
import com.google.sample.castcompanionlibrary.cast.exceptions.CastException;
import com.google.sample.castcompanionlibrary.cast.exceptions.NoConnectionException;
import com.google.sample.castcompanionlibrary.cast.exceptions.TransientNetworkDisconnectionException;
import com.soundcloud.android.api.HttpProperties;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.ProgressReporter;
import com.soundcloud.android.playback.service.Playa;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.utils.Log;
import com.soundcloud.propeller.PropertySet;
import org.jetbrains.annotations.Nullable;

import android.content.res.Resources;
import android.net.Uri;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class CastPlayer extends VideoCastConsumerImpl implements Playa, ProgressReporter.ProgressPusher {
    private final static String TAG = "CastPlayer";

    private final VideoCastManager castManager;
    private final HttpProperties httpProperties;
    private final ProgressReporter progressReporter;
    private final ImageOperations imageOperations;
    private final Resources resources;

    private PlayaListener playaListener;
    private Urn currentTrackUrn;
    private boolean shouldBePlaying;

    @Inject
    public CastPlayer(VideoCastManager castManager,  HttpProperties httpProperties, ProgressReporter progressReporter,
                      ImageOperations imageOperations, Resources resources) {
        this.castManager = castManager;
        this.httpProperties = httpProperties;
        this.progressReporter = progressReporter;
        this.imageOperations = imageOperations;
        this.resources = resources;

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
        shouldBePlaying = false;
        reportStateChange(getStateTransition(PlayaState.IDLE, Reason.NONE)); // possibly show disconnect error here instead?
    }

    public void onMediaPlayerStatusUpdatedListener(int playerState, int idleReason) {
        Log.d(TAG, "New player state and reason "+ playerState + " " + idleReason);
        if (playaListener == null){
            return;
        }

        switch (playerState) {
            case MediaStatus.PLAYER_STATE_PLAYING:
                final StateTransition stateTransition = getStateTransition(PlayaState.PLAYING, Reason.NONE);
                reportStateChange(stateTransition);
                break;

            case MediaStatus.PLAYER_STATE_PAUSED:
                // we have to suppress pause events while we should be playing.
                // The receiver sends thes back often, as in when the track first loads, even if autoplay is true
                if (!shouldBePlaying){
                    reportStateChange(getStateTransition(PlayaState.IDLE, Reason.NONE));
                }
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
        playaListener.onPlaystateChanged(stateTransition);
        final boolean playerPlaying = stateTransition.isPlayerPlaying();
        if (playerPlaying){
            progressReporter.start();
        } else {
            progressReporter.stop();
        }
    }

    @Override
    public void pushProgress() {
        if (playaListener != null){
            try {
                playaListener.onProgressEvent(castManager.getCurrentMediaPosition(), castManager.getMediaDuration());
            } catch (TransientNetworkDisconnectionException | NoConnectionException e) {
                Log.e(TAG, "Unable to report progress", e);
            }
        }

    }

    private StateTransition getStateTransition(PlayaState state, Reason reason) {
        return new StateTransition(state, reason, currentTrackUrn, getProgress(), getDuration());
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

    @Override
    public void play(PropertySet track) {
        play(track, 0);
    }

    @Override
    public void playUninterrupted(PropertySet track) {
        play(track, 0);
    }

    @Override
    public void play(PropertySet track, long fromPos) {
        currentTrackUrn = track.get(TrackProperty.URN);
        shouldBePlaying = true;

        try {
            MediaMetadata mediaMetadata = new MediaMetadata(MediaMetadata.MEDIA_TYPE_MUSIC_TRACK);
            mediaMetadata.putString(MediaMetadata.KEY_TITLE, track.get(TrackProperty.TITLE));
            mediaMetadata.putString(MediaMetadata.KEY_ARTIST, track.get(TrackProperty.CREATOR_NAME));
            mediaMetadata.addImage(new WebImage(Uri.parse(imageOperations.getUrlForLargestImage(resources, currentTrackUrn))));

            final String streamUrlWithClientId = getStreamUrlWithClientId(track);
            MediaInfo mediaInfo = new MediaInfo.Builder(streamUrlWithClientId)
                    .setContentType("audio/mpeg")
                    .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                    .setMetadata(mediaMetadata)
                    .build();

            reportStateChange(new StateTransition(PlayaState.BUFFERING, Reason.NONE, track.get(TrackProperty.URN)));
            castManager.loadMedia(mediaInfo, true, (int) fromPos);

        } catch (TransientNetworkDisconnectionException | NoConnectionException e) {
            Log.e(TAG, "Unable to load track", e);
            e.printStackTrace();
        }
    }

    private String getStreamUrlWithClientId(PropertySet track) {
        // we will eventually be able to play with just a urn, this is temporary
        return track.get(TrackProperty.STREAM_URL) + "?client_id=" + httpProperties.getClientId();
    }

    @Override
    public boolean resume() {
        try {
            castManager.play();
            shouldBePlaying = true;
            return true;
        } catch (CastException | TransientNetworkDisconnectionException | NoConnectionException | IllegalStateException e) {
            Log.e(TAG, "Unable to resume playback", e);
        }
        return false;
    }

    @Override
    public void pause() {
        try {
            castManager.pause();
            shouldBePlaying = false;
        } catch (CastException | TransientNetworkDisconnectionException | NoConnectionException | IllegalStateException  e) {
            Log.e(TAG, "Unable to pause playback", e);
        }
    }

    @Override
    public long seek(long ms, boolean performSeek) {
        try {
            castManager.seek((int) ms);
        } catch (TransientNetworkDisconnectionException | NoConnectionException | IllegalStateException  e) {
            Log.e(TAG, "Unable to seek", e);
        }
        return ms;
    }

    @Override
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

    @Override
    public void setVolume(float v) {
        try {
            castManager.setVolume(v);
        } catch (CastException | TransientNetworkDisconnectionException | NoConnectionException | IllegalStateException  e) {
            Log.e(TAG, "Unable to set volume", e);
        }
    }

    @Override
    public void stop() {
        pause(); // stop has more long-running implications in cast. pause is sufficient
    }

    @Override
    public void destroy() {
        castManager.onDeviceSelected(null);
        castManager.removeVideoCastConsumer(this);
        shouldBePlaying = false;
    }

    @Override
    public void setListener(PlayaListener playaListener) {
        this.playaListener = playaListener;
    }

    @Override
    public boolean isSeekable() {
        return true;
    }

    @Override
    public boolean isNotSeekablePastBuffer() {
        return false;
    }

}
