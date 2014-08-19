package com.soundcloud.android.playback.service.managers;

import com.soundcloud.android.playback.service.NotificationTrack;
import com.soundcloud.propeller.PropertySet;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.media.RemoteControlClient;

@SuppressWarnings("UnusedDeclaration")
@TargetApi(14)
public class ICSRemoteAudioManager extends FroyoRemoteAudioManager {
    private final RemoteControlClient client;
    private final Resources resources;

    public ICSRemoteAudioManager(Context context) {
        super(context);
        client = createRemoteControlClient(context);
        resources = context.getResources();
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
    public void setPlaybackState(boolean isSupposedToBePlaying) {
        final int playbackState = translateState(isSupposedToBePlaying);
        client.setPlaybackState(playbackState);
    }

    @Override
    public void onTrackChanged(PropertySet track, Bitmap artwork) {
        applyRemoteMetadata(track, artwork);
    }

    private void applyRemoteMetadata(PropertySet track, Bitmap artwork) {
        final NotificationTrack trackViewModel = new NotificationTrack(resources, track);
        client.editMetadata(false)
                .putBitmap(RemoteControlClient.MetadataEditor.BITMAP_KEY_ARTWORK, artwork)
                .putString(MediaMetadataRetriever.METADATA_KEY_TITLE, trackViewModel.getTitle())
                .putString(MediaMetadataRetriever.METADATA_KEY_ALBUM, trackViewModel.getCreatorName())
                .putLong(MediaMetadataRetriever.METADATA_KEY_DURATION, trackViewModel.getDuration())
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

    private static int translateState(boolean isSupposedToBePlaying) {
        return isSupposedToBePlaying ? RemoteControlClient.PLAYSTATE_PLAYING : RemoteControlClient.PLAYSTATE_PAUSED;
    }
}
