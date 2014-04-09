package com.soundcloud.android.playback.service.skippy;

import static com.soundcloud.android.skippy.Skippy.Reason.BUFFERING;
import static com.soundcloud.android.skippy.Skippy.Reason.ERROR;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.playback.service.Playa;
import com.soundcloud.android.skippy.Skippy;
import com.soundcloud.android.utils.Log;
import com.soundcloud.api.Token;
import org.jetbrains.annotations.Nullable;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import javax.inject.Inject;


public class SkippyAdapter implements Playa, Skippy.PlayListener {

    private static final String TAG = "SkippyAdapter";

    private final Skippy mSkippy;
    private final AccountOperations mAccountOperations;
    private final StateChangeHandler mStateHandler = new StateChangeHandler();

    private Skippy.State mLastState = Skippy.State.IDLE;
    private Skippy.Reason mLastReason = Skippy.Reason.NOTHING;
    private Skippy.Error mLastError = Skippy.Error.OK;

    @Inject
    public SkippyAdapter(Context context, AccountOperations accountOperations) {
        this(context, new SkippyFactory(), accountOperations);
    }

    @VisibleForTesting
    SkippyAdapter(Context context, SkippyFactory skippyFactory, AccountOperations accountOperations) {
        mSkippy = skippyFactory.create(context, this);
        mAccountOperations = accountOperations;
    }

    @Override
    public boolean isPlaying(){
        return getState().isPlaying();
    }

    @Override
    public boolean isPlayerPlaying(){
        return getState().isPlayerPlaying();
    }

    @Override
    public boolean isBuffering(){
        return getState().isBuffering();
    }

    @Override
    public void play(Track track) {
        play(track, 0);
    }

    @Override
    public void play(Track track, long fromPos) {
        mSkippy.play(authorizeUrl(track.getStreamUrlWithAppendedId()), fromPos);
    }

    @Override
    public boolean resume() {
        mSkippy.resume();
        return true;
    }

    @Override
    public void pause() {
        mSkippy.pause();
    }

    @Override
    public long seek(long position, boolean performSeek) {
        mSkippy.seek(position);
        return position;
    }

    @Override
    public long getProgress() {
        return mSkippy.getPosition();
    }

    @Override
    public void setVolume(float level) {
        mSkippy.setVolume(level);
    }

    @Override
    public void stop() {
        mSkippy.pause();
    }

    @Override
    public void destroy() {
        mSkippy.destroy();
    }

    @Override
    public void setListener(PlayaListener playaListener) {
        mStateHandler.setPlayaListener(playaListener);
    }

    @Override
    public StateTransition getLastStateTransition() {
        return new StateTransition(getState(), getTranslatedReason());
    }

    @Override
    public Playa.PlayaState getState() {
        return getTranslatedState();
    }

    @Override
    public boolean isSeekable() {
        return true;
    }

    @Override
    public boolean isNotSeekablePastBuffer() {
        return false;
    }

    @Override
    public void onStateChanged(Skippy.State state, Skippy.Reason reason, Skippy.Error errorcode) {
        Log.i(TAG, "State = " + state + " : " + reason + " : " + errorcode);
        mLastState = state;
        mLastReason = reason;
        mLastError = errorcode;

        Message msg = Message.obtain();
        msg.obj = new StateTransition(getTranslatedState(), getTranslatedReason());
        mStateHandler.sendMessage(msg);
    }

    private PlayaState getTranslatedState() {
        switch (mLastState) {
            case IDLE:
                return PlayaState.IDLE;
            case PLAYING:
                return mLastReason == BUFFERING ? PlayaState.BUFFERING : PlayaState.PLAYING;
            default:
                throw new IllegalArgumentException("Unexpected skippy state : " + mLastState);
        }
    }

    private Reason getTranslatedReason() {
        if (mLastReason == ERROR) {
            switch (mLastError) {
                case FAILED:
                    return Reason.ERROR_FAILED;
                case FORBIDDEN:
                    return Reason.ERROR_FORBIDDEN;
                case MEDIA_NOT_FOUND:
                    return Reason.ERROR_NOT_FOUND;
                default:
                    throw new IllegalArgumentException("Unexpected skippy error code : " + mLastError);
            }
        } else {
            return Reason.NONE;
        }
    }

    @Override
    public void onPerformanceMeasured(Skippy.PlaybackMetric metric, long value, String uri) {
        Log.i(TAG, "Performance Metric : " + metric + " : " + value);
    }

    @Override
    public void onProgressChange(long position, long duration) {
        Log.d(TAG, "Progress changed : " + position + " : " + duration);
    }

    @Override
    public void onErrorMessage(String category, String errorMsg) {

    }

    static class StateChangeHandler extends Handler {

        @Nullable
        private PlayaListener mPlayaListener;

        private StateChangeHandler() {
            super(Looper.getMainLooper());
        }

        public void setPlayaListener(@Nullable PlayaListener playaListener) {
            mPlayaListener = playaListener;
        }

        @Override
        public void handleMessage(Message msg) {
            if (mPlayaListener != null) {
                mPlayaListener.onPlaystateChanged((StateTransition) msg.obj);
            }
        }
    }

    @VisibleForTesting
    static class SkippyFactory {
        public Skippy create(Context context, Skippy.PlayListener listener) {
            return new Skippy(context, listener);
        }
    }

    private String authorizeUrl(String url) {
        final Token soundCloudToken = mAccountOperations.getSoundCloudToken();
        if (soundCloudToken != null) {
            return Uri.parse(url).buildUpon().appendQueryParameter("oauth_token", soundCloudToken.access).build().toString();
        } else {
            return url;
        }

    }
}
