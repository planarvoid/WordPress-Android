package com.soundcloud.android.audio.managers;

import com.soundcloud.android.model.Track;
import com.soundcloud.android.service.playback.State;
import org.jetbrains.annotations.Nullable;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

public class FallbackAudioManager implements IRemoteAudioManager {
    private static String TAG = FallbackAudioManager.class.getSimpleName();

    private final TelephonyManager mTelephonyManager;
    private final PhoneStateListener mPhoneStateListener;
    private final AudioManager mAudioManager;
    private MusicFocusable mMusicFocusable;

    public FallbackAudioManager(Context context){
        mPhoneStateListener = createPhoneStateListener();
        mTelephonyManager = ((TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE));
        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    }

    @Override
    public boolean requestMusicFocus(final MusicFocusable focusable, int durationHint) {
        mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        mMusicFocusable = focusable;
        return true;
    }

    private PhoneStateListener createPhoneStateListener() {
        return new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int callState, String incomingNumber) {
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "onCallStateChanged(state=" + callState);
                }
                if (mMusicFocusable != null){
                    if (callState == TelephonyManager.CALL_STATE_OFFHOOK ||
                            (callState == TelephonyManager.CALL_STATE_RINGING &&
                                    mAudioManager.getStreamVolume(AudioManager.STREAM_RING) > 0)) {
                        mMusicFocusable.focusLost(true, false);
                    } else if (callState == TelephonyManager.CALL_STATE_IDLE) {
                        mMusicFocusable.focusGained();
                    }
                }
            }
        };

    }

    @Override
    public boolean abandonMusicFocus(boolean isTemporary) {
        mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
        mMusicFocusable = null;
        return true;
    }

    @Override
    public boolean isFocusSupported() {
        return false;
    }

    @Override
    public void onFocusObtained() {
    }

    @Override
    public void onFocusAbandoned() {
    }

    @Override
    public void setPlaybackState(State state) {
    }

    @Override
    public boolean isTrackChangeSupported() {
        return false;
    }

    @Override
    public void onTrackChanged(Track track, @Nullable Bitmap artwork) {
    }
}
