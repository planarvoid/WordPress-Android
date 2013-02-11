package com.soundcloud.android.view;

import static java.lang.Math.max;
import static java.lang.Math.min;

import com.soundcloud.android.cache.WaveformCache;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.WaveformData;
import com.soundcloud.android.service.playback.CloudPlaybackService;
import com.soundcloud.android.view.play.WaveformController;
import org.jetbrains.annotations.Nullable;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.widget.ProgressBar;

import java.lang.ref.WeakReference;

public class NowPlayingIndicator extends ProgressBar {
    private static final int REFRESH = 1;

    private static final int TOP_ORANGE       = 0xFFFF4400;
    private static final int SEPARATOR_ORANGE = 0xFF661400;
    private static final int BOTTOM_ORANGE    = 0xFFAA2200;

    private static final int TOP_GREY       = 0xFF666666;
    private static final int SEPARATOR_GREY = 0xFF2D2D2D;
    private static final int BOTTOM_GREY    = 0xFF535353;

    private static final float TOP_WAVEFORM_FRACTION = 0.75f;

    private @Nullable Bitmap mWaveformMask;
    private @Nullable Track  mTrack;

    private long mRefreshDelay;

    private Paint mTopOrange;
    private Paint mSeparatorOrange;
    private Paint mBottomOrange;

    private Paint mTopGrey;
    private Paint mSeparatorGrey;
    private Paint mBottomGrey;

    private Rect mCanvasRect;

    private boolean mListening;

    private int mAdjustedWidth;
    private WaveformController.WaveformState mWaveformState;
    private int mWaveformErrorCount;
    private WaveformData mWaveformData;

    private final Handler mHandler = new RefreshHandler(this);

    @SuppressWarnings("UnusedDeclaration")
    public NowPlayingIndicator(Context context) {
        super(context);
        init(context);
    }

    @SuppressWarnings("UnusedDeclaration")
    public NowPlayingIndicator(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    @SuppressWarnings("UnusedDeclaration")
    public NowPlayingIndicator(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(final Context context) {
        PorterDuffXfermode sourceIn = new PorterDuffXfermode(PorterDuff.Mode.SRC_IN);

        mTopOrange = new Paint();
        mTopOrange.setColor(TOP_ORANGE);
        mTopOrange.setXfermode(sourceIn);

        mSeparatorOrange = new Paint();
        mSeparatorOrange.setColor(SEPARATOR_ORANGE);
        mSeparatorOrange.setXfermode(sourceIn);

        mBottomOrange = new Paint();
        mBottomOrange.setColor(BOTTOM_ORANGE);
        mBottomOrange.setXfermode(sourceIn);

        mTopGrey = new Paint();
        mTopGrey.setColor(TOP_GREY);
        mTopGrey.setXfermode(sourceIn);

        mSeparatorGrey = new Paint();
        mSeparatorGrey.setColor(SEPARATOR_GREY);
        mSeparatorGrey.setXfermode(sourceIn);

        mBottomGrey = new Paint();
        mBottomGrey.setColor(BOTTOM_GREY);
        mBottomGrey.setXfermode(sourceIn);

        setIndeterminate(false);
    }

    void startListening() {
        if (!mListening) {
            mListening = true;
            IntentFilter f = new IntentFilter();
            f.addAction(CloudPlaybackService.PLAYSTATE_CHANGED);
            f.addAction(CloudPlaybackService.META_CHANGED);
            f.addAction(CloudPlaybackService.SEEK_COMPLETE);
            f.addAction(CloudPlaybackService.SEEKING);
            getContext().registerReceiver(mStatusListener, new IntentFilter(f));
        }
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
            setProgress((int) CloudPlaybackService.getCurrentProgress());
            if (CloudPlaybackService.getState().isSupposedToBePlaying()) queueNextRefresh(mRefreshDelay);
        }
    }

    private void setCurrentTrack() {
        final Track currentTrack = CloudPlaybackService.getCurrentTrack();
        if (mTrack != currentTrack || mWaveformState == WaveformController.WaveformState.ERROR) {

            if (mTrack != currentTrack) mWaveformErrorCount = 0;

            mTrack = currentTrack;
            setMax(mTrack == null ? 0 : mTrack.duration);
            setProgress((int) CloudPlaybackService.getCurrentProgress());

            if (mTrack == null || !mTrack.hasWaveform() || mWaveformErrorCount > 3) {
                setDefaultWaveform();
            } else {
                if (WaveformCache.get().getData(mTrack, new WaveformCache.WaveformCallback() {
                    @Override
                    public void onWaveformDataLoaded(Track track, WaveformData data, boolean fromCache) {
                        if (track.equals(mTrack)){
                            mWaveformErrorCount = 0;
                            mWaveformState = WaveformController.WaveformState.OK;
                            setWaveform(data);
                        }
                    }

                    @Override
                    public void onWaveformError(Track track) {
                        if (track.equals(mTrack)) {
                            mWaveformState = WaveformController.WaveformState.ERROR;
                            mWaveformErrorCount++;
                            setCurrentTrack();
                        }
                    }

                }) == null) {
                    // loading
                    // TODO, loading indicator?
                }

            }
        }
    }

    private void setDefaultWaveform(){
        // TODO, set default bitmap
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mAdjustedWidth = getWidth() - 0 * 2;
        mCanvasRect = new Rect(0, 0, mAdjustedWidth, getHeight());

        setWaveformMask();
        startRefreshing();
    }

    public void pause(){
        stopRefreshing();
        stopListening();
    }

    public void destroy(){
        pause();
        mWaveformMask = null;
    }

    private void stopRefreshing() {
        mHandler.removeMessages(REFRESH);
    }

    private void queueNextRefresh(long delay) {
        Message msg = mHandler.obtainMessage(REFRESH);
        mHandler.removeMessages(REFRESH);
        if (delay != -1) mHandler.sendMessageDelayed(msg, delay);
    }


    @Override
    protected void onDraw(Canvas canvas) {
        if (mWaveformMask == null) return;

        Canvas tmp = new Canvas(mWaveformMask);

        float density = getResources().getDisplayMetrics().density;

        int topPartHeight   = (int) (getHeight() * TOP_WAVEFORM_FRACTION);
        int separatorTop    = (int) (topPartHeight - density);
        int separatorBottom = topPartHeight;

        // Grey
        tmp.drawRect(0, 0,             mAdjustedWidth, getHeight(),     mTopGrey);
        tmp.drawRect(0, topPartHeight, mAdjustedWidth, getHeight(),     mBottomGrey);
        tmp.drawRect(0, separatorTop,  mAdjustedWidth, separatorBottom, mSeparatorGrey);

        float playedFraction = (float) getProgress() / (float) getMax();
        playedFraction = min(max(playedFraction, 0), getMax());

        // Make sure to at least draw an 1dp line of progress
        int progressWidth = (int) max(mAdjustedWidth * playedFraction, density);

        // Orange
        tmp.drawRect(0, 0,             progressWidth, getHeight(),     mTopOrange);
        tmp.drawRect(0, topPartHeight, progressWidth, getHeight(),     mBottomOrange);
        tmp.drawRect(0, separatorTop,  progressWidth, separatorBottom, mSeparatorOrange);

        canvas.drawBitmap(
            mWaveformMask,
            mCanvasRect,
            mCanvasRect,
            null
        );
    }

    private void setWaveformMask() {
        this.mWaveformMask = createWaveformMask(mWaveformData, mAdjustedWidth, getHeight());
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
                if (CloudPlaybackService.getState().isSupposedToBePlaying()) queueNextRefresh(mRefreshDelay);
            }
        }
    };

    public void setWaveform(WaveformData waveformData) {
        mWaveformData = waveformData;
        setWaveformMask();
    }

    private static Bitmap createWaveformMask(WaveformData waveformData, int width, int height) {
        if (waveformData == null || width == 0 || height == 0) return null;

        Bitmap mask   = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_4444);
        Canvas canvas = new Canvas(mask);

        Paint black = new Paint();
        black.setColor(Color.BLACK);

        canvas.drawRect(0, 0, width, height, black);

        Paint xor = new Paint();
        xor.setColor(Color.BLACK);
        xor.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.XOR));

        // Top half waveform
        int dstHeight = (int) (height * TOP_WAVEFORM_FRACTION);

        WaveformData scaled = waveformData.scale(width);
        for (int i = 0; i < scaled.samples.length; i++) {
            final float scaledHeight1 = (scaled.samples[i] * (float) dstHeight / waveformData.maxAmplitude);
            canvas.drawLine(
                i, 0,
                i, dstHeight - scaledHeight1,
                xor
            );

            final float scaledHeight2 = (scaled.samples[i] * (float) (height-dstHeight) / waveformData.maxAmplitude);
            canvas.drawLine(
                i, dstHeight + scaledHeight2,
                i, height,
                xor
            );
        }

        return mask;
    }

    private static final class RefreshHandler extends Handler {
        private WeakReference<NowPlayingIndicator> mRef;

        private RefreshHandler(NowPlayingIndicator nowPlaying) {
            this.mRef = new WeakReference<NowPlayingIndicator>(nowPlaying);
        }

        @Override
        public void handleMessage(Message msg) {
            final NowPlayingIndicator nowPlaying = mRef.get();
            if (nowPlaying != null && nowPlaying.mTrack != null) {
                switch (msg.what) {
                    case REFRESH:
                        nowPlaying.setProgress((int) CloudPlaybackService.getCurrentProgress());
                        nowPlaying.queueNextRefresh(nowPlaying.mRefreshDelay);
                }
            }
        }
    }
}
