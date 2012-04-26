package com.soundcloud.android.service.playback;

import com.soundcloud.android.model.Track;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.RemoteControlClient;

@SuppressWarnings("UnusedDeclaration")
public class FroyoAudioManager implements IAudioManager {
    private final RemoteControlClient client;
    private boolean mAudioFocusLost;
    private AudioManager.OnAudioFocusChangeListener listener;

    private final Context mContext;

    private static Class<? extends BroadcastReceiver> RECEIVER = RemoteControlReceiver.class;

    public FroyoAudioManager(Context context) {
        mContext = context;
        client = createRemoteControlClient(context);
    }

    @Override
    public int requestMusicFocus(final MusicFocusable focusable) {
        if (listener == null) {
            listener = new AudioManager.OnAudioFocusChangeListener() {
                @Override
                public void onAudioFocusChange(int focusChange) {
                    if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
                        if (mAudioFocusLost) {
                            focusable.focusGained();
                            mAudioFocusLost = false;
                        }
                    } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                        mAudioFocusLost = true;
                        focusable.focusLost(false, false);
                    } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                        mAudioFocusLost = true;
                        focusable.focusLost(true, false);
                    } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
                        mAudioFocusLost = true;
                        focusable.focusLost(true, true);
                    }
                }
            };
        }
        final int ret = getAudioManager().requestAudioFocus(listener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN);

        if (ret == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            registerMediaButton();
            registerRemoteControlClient();
        }
        return ret;
    }

    @Override
    public int abandonMusicFocus(boolean isTemporary) {
        if (listener != null) {
            final int ret = getAudioManager().abandonAudioFocus(listener);
            if (ret == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                unregisterMediaButton();
                unregisterRemoteControlClient();
            }
            return ret;
        } else {
            return AudioManager.AUDIOFOCUS_REQUEST_FAILED;
        }
    }

    @Override
    public void applyRemoteMetadata(Track track, Bitmap bitmap) {
        client.editMetadata(true)
              .putBitmap(RemoteControlClient.MetadataEditor.BITMAP_KEY_ARTWORK, bitmap)
              .putString(MediaMetadataRetriever.METADATA_KEY_TITLE, track.title)
              .putString(MediaMetadataRetriever.METADATA_KEY_ARTIST, track.getUserName())
              .putLong(MediaMetadataRetriever.METADATA_KEY_DURATION, track.duration)
              .apply();
    }

    @Override
    public void setPlaybackState(boolean isPlaying) {
        client.setPlaybackState(isPlaying ? RemoteControlClient.PLAYSTATE_PLAYING : RemoteControlClient.PLAYSTATE_PAUSED);
    }

    @Override
    public boolean isSupported() {
        return true;
    }

    private AudioManager getAudioManager() {
        return (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
    }

    private void registerMediaButton() {
        getAudioManager().registerMediaButtonEventReceiver(new ComponentName(mContext, RECEIVER));
    }

    private void unregisterMediaButton() {
        getAudioManager().unregisterMediaButtonEventReceiver(new ComponentName(mContext, RECEIVER));
    }

    private void registerRemoteControlClient() {
        getAudioManager().registerRemoteControlClient(client);
    }

    private void unregisterRemoteControlClient() {
        getAudioManager().unregisterRemoteControlClient(client);
    }

    private RemoteControlClient createRemoteControlClient(Context context) {
        PendingIntent mediaPendingIntent = PendingIntent.getBroadcast(context,
                0, new Intent(Intent.ACTION_MEDIA_BUTTON)
                  .setComponent(getReceiverComponent()), PendingIntent.FLAG_UPDATE_CURRENT);

        RemoteControlClient client = new RemoteControlClient(mediaPendingIntent);
        final int flags = RemoteControlClient.FLAG_KEY_MEDIA_PREVIOUS
                | RemoteControlClient.FLAG_KEY_MEDIA_NEXT
                | RemoteControlClient.FLAG_KEY_MEDIA_PLAY
                | RemoteControlClient.FLAG_KEY_MEDIA_PAUSE
                | RemoteControlClient.FLAG_KEY_MEDIA_PLAY_PAUSE
                | RemoteControlClient.FLAG_KEY_MEDIA_STOP;

        client.setTransportControlFlags(flags);
        return client;
    }

    private ComponentName getReceiverComponent() {
       return new ComponentName(mContext, RECEIVER);
    }
}
