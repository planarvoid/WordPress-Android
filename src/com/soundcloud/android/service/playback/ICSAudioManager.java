package com.soundcloud.android.service.playback;

import com.soundcloud.android.model.Track;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.media.RemoteControlClient;
import android.util.Log;

@SuppressWarnings("UnusedDeclaration")
public class ICSAudioManager extends FroyoAudioManager {
    private final RemoteControlClient client;

    public ICSAudioManager(Context context) {
        super(context);
        client = createRemoteControlClient(context);
    }

    @Override
    public boolean isTrackChangeSupported() {
        return true;
    }

    @Override
    public void onFocusObtained() {
        super.onFocusObtained();
        registerRemoteControlClient();
    }

    @Override
    public void onFocusAbandoned() {
        super.onFocusAbandoned();
        unregisterRemoteControlClient();
    }

    @Override
    public void setPlaybackState(State state) {
        super.setPlaybackState(state);
        final int playbackState = translateState(state);

        Log.d(getClass().getName(), "set playbackstate ("+state+") to "+playbackState);
        client.setPlaybackState(playbackState);
    }

    @Override
    public void onTrackChanged(Track track, Bitmap artwork) {
        applyRemoteMetadata(track, artwork);
    }

    private void applyRemoteMetadata(Track track, Bitmap artwork) {
        client.editMetadata(true)
                .putBitmap(RemoteControlClient.MetadataEditor.BITMAP_KEY_ARTWORK, artwork)
                .putString(MediaMetadataRetriever.METADATA_KEY_TITLE, track.title)
                .putString(MediaMetadataRetriever.METADATA_KEY_ARTIST, track.getUserName())
                .putLong(MediaMetadataRetriever.METADATA_KEY_DURATION, track.duration)
                .apply();
    }

    private void registerRemoteControlClient() {
        getAudioManager().registerRemoteControlClient(client);
    }

    private void unregisterRemoteControlClient() {
        client.editMetadata(false).clear();
        getAudioManager().unregisterRemoteControlClient(client);
    }

    private RemoteControlClient createRemoteControlClient(Context context) {
        PendingIntent pi = PendingIntent.getBroadcast(
                context,
                0 /* request code */,
                new Intent(Intent.ACTION_MEDIA_BUTTON).setComponent(receiverComponent),
                PendingIntent.FLAG_UPDATE_CURRENT);

        RemoteControlClient client = new RemoteControlClient(pi);
        final int flags = RemoteControlClient.FLAG_KEY_MEDIA_PREVIOUS
                | RemoteControlClient.FLAG_KEY_MEDIA_NEXT
                | RemoteControlClient.FLAG_KEY_MEDIA_PLAY
                | RemoteControlClient.FLAG_KEY_MEDIA_PAUSE
                | RemoteControlClient.FLAG_KEY_MEDIA_PLAY_PAUSE
                | RemoteControlClient.FLAG_KEY_MEDIA_STOP;

        client.setTransportControlFlags(flags);
        return client;
    }

    private static int translateState(State state) {
        switch (state) {
            case PLAYING:
                return RemoteControlClient.PLAYSTATE_PLAYING;
            case PREPARING:
            case PAUSED_FOR_BUFFERING:
                return RemoteControlClient.PLAYSTATE_BUFFERING;
            case STOPPED:
                return RemoteControlClient.PLAYSTATE_STOPPED;
            case PAUSED:
            case PAUSED_FOCUS_LOST:
                return RemoteControlClient.PLAYSTATE_PAUSED;
            case ERROR:
                return RemoteControlClient.PLAYSTATE_ERROR;

            default:
                return RemoteControlClient.PLAYSTATE_STOPPED;
        }
    }
}
