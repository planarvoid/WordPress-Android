package com.soundcloud.android.view;

import static java.lang.Math.max;
import static java.lang.Math.min;

import com.google.android.imageloader.ImageLoader;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.activity.ScPlayer;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.service.playback.CloudPlaybackService;
import com.soundcloud.android.utils.ImageUtils;
import org.jetbrains.annotations.Nullable;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Shader;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

public class NowPlayingIndicator extends ProgressBar {
    private static final int REFRESH = 1;

    private static final int BACKGROUND_COLORS[] = {
        0xFF1B1B1B,
        0xFF1B1B1B,
        0xFF131313,
        0xFF020202,
        0xFF020202
    };

    private static final int FOREGROUND_COLORS[] = {
        0xFFFF4400,
        0xFFFF4400,
        0xFFED2800,
        0xFFA82400,
        0xFFA82400
    };

    private static final float COLOR_STOPS[] = {
        0.0f,
        0.70f,
        0.72f,
        0.74f,
        1.0f
    };

    private @Nullable Bitmap mWaveform;
    private @Nullable Bitmap mWaveformMask;
    private @Nullable Track  mTrack;

    private long mRefreshDelay;
    private Paint mBackgroundPaint;
    private Paint mForegroundPaint;
    private Rect mCanvasRect;

    private Boolean mListening;

    private int mSideOffset;
    private int mAdjustedWidth;

    public NowPlayingIndicator(Context context) {
        super(context);
        init(context);
    }

    public NowPlayingIndicator(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public NowPlayingIndicator(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(final Context context) {

        mSideOffset = (int) (context.getResources().getDisplayMetrics().density * 5);
        mBackgroundPaint = new Paint();
        mBackgroundPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));

        mForegroundPaint = new Paint();
        mForegroundPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));

        setIndeterminate(false);
    }

    void startListening(){
        mListening = true;
        IntentFilter f = new IntentFilter();
        f.addAction(CloudPlaybackService.PLAYSTATE_CHANGED);
        f.addAction(CloudPlaybackService.META_CHANGED);
        f.addAction(CloudPlaybackService.SEEK_COMPLETE);
        f.addAction(CloudPlaybackService.SEEKING);
        getContext().registerReceiver(mStatusListener, new IntentFilter(f));
    }

    void stopListening(){
        if (mListening){
            getContext().unregisterReceiver(mStatusListener);
        }
        mListening = false;
    }

    public void resume() {
        // Update the current track
        setCurrentTrack();
        startRefreshing();
        startListening();
    }

    private void startRefreshing() {
        if (mTrack != null && mTrack.duration > 0 && getWidth() > 0){
            mRefreshDelay = mTrack.duration / getWidth();
            if (CloudPlaybackService.getState().isSupposedToBePlaying()) queueNextRefresh(refreshNow());
        }
    }

    private void setCurrentTrack() {
        final Track currentTrack = CloudPlaybackService.getCurrentTrack();
        if (mTrack != currentTrack) {
            mTrack = currentTrack;
            setProgress((int) CloudPlaybackService.getCurrentProgress());

            if (mTrack == null) {
                setMax(0);
                // set default bitmap
            } else {
                setMax(mTrack.duration);
                ImageLoader.get(getContext()).getBitmap(
                        mTrack.waveform_url,
                        new ImageLoader.BitmapCallback() {
                            public void onImageLoaded(Bitmap mBitmap, String uri) {
                                setWaveform(mBitmap);
                            }
                        }
                );
            }
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mAdjustedWidth = getWidth() - mSideOffset * 2;
        mCanvasRect = new Rect(mSideOffset, 0, mAdjustedWidth, getHeight());
        mBackgroundPaint.setShader(new LinearGradient(0, 0, 0, getHeight(), BACKGROUND_COLORS, COLOR_STOPS, Shader.TileMode.MIRROR));
        mForegroundPaint.setShader(new LinearGradient(0, 0, 0, getHeight(), FOREGROUND_COLORS, COLOR_STOPS, Shader.TileMode.MIRROR));
        setWaveformMask();
        startRefreshing();
    }

    public void pause(){
        stopRefreshing();
        stopListening();
    }

    public void destroy(){
        pause();
        mWaveform = null;
        mWaveformMask = null;
    }

    private void stopRefreshing() {
        mHandler.removeMessages(REFRESH);
    }

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case REFRESH:
                    queueNextRefresh(refreshNow());
            }
        }
    };

    private void queueNextRefresh(long delay) {
        Message msg = mHandler.obtainMessage(REFRESH);
        mHandler.removeMessages(REFRESH);
        if (delay != -1) mHandler.sendMessageDelayed(msg, delay);
    }

    private long refreshNow() {
        if (mTrack == null) return -1;

        long progress  = CloudPlaybackService.getCurrentProgress();
        setProgress((int) progress);
        return mRefreshDelay - (progress % mRefreshDelay);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mWaveformMask == null) return;

        Canvas tmp = new Canvas(mWaveformMask);

        tmp.drawRect(mSideOffset, 0, mAdjustedWidth, getHeight(), mBackgroundPaint);

        float fraction = (float) getProgress() / (float) getMax();
        fraction = min(max(fraction, 0), getMax());

        tmp.drawRect(mSideOffset, 0, mAdjustedWidth * fraction, getHeight(), mForegroundPaint);

        canvas.drawBitmap(
                mWaveformMask,
                mCanvasRect,
                mCanvasRect,
                null
        );
    }

    private void setWaveformMask() {
        this.mWaveformMask = createWaveformMask(mWaveform, mAdjustedWidth, getHeight());
    }

    private final BroadcastReceiver mStatusListener = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(CloudPlaybackService.META_CHANGED)) {
                setCurrentTrack();

            } else if (action.equals(CloudPlaybackService.PLAYSTATE_CHANGED)) {
              if (intent.getBooleanExtra(CloudPlaybackService.BroadcastExtras.isPlaying, false)){
                  startRefreshing();
              } else {
                  stopRefreshing();
              }

            } else if (action.equals(CloudPlaybackService.SEEK_COMPLETE) || action.equals(CloudPlaybackService.SEEKING)) {
                queueNextRefresh(refreshNow());

            }
        }
    };

    public void setWaveform(Bitmap waveform) {
        this.mWaveform     = waveform;
        setWaveformMask();
    }

    private static Bitmap createWaveformMask(Bitmap waveform, int width, int height) {
        if (waveform == null || width == 0 || height == 0) return null;

        Bitmap mask   = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_4444);
        Canvas canvas = new Canvas(mask);
        float  ratio  = 0.75f;

        Paint black = new Paint();
        black.setColor(Color.BLACK);

        canvas.drawRect(0, 0, width, height, black);

        Paint xor = new Paint();
        xor.setColor(Color.BLACK);
        xor.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.XOR));

        // Top half waveform
        int dstHeight = (int) (height * ratio);

        canvas.drawBitmap(
            waveform,
            new Rect(0, 0, waveform.getWidth(), waveform.getHeight() / 2),
            new Rect(0, 0, width, dstHeight),
            xor
        );

        // Bottom half waveform
        canvas.drawBitmap(
            waveform,
            new Rect(0, waveform.getHeight() / 2, waveform.getWidth(), waveform.getHeight()),
            new Rect(0, dstHeight, width, height),
            xor
        );

        return mask;
    }
}
