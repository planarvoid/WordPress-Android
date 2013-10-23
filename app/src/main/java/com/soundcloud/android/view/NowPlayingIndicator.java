package com.soundcloud.android.view;

import static com.soundcloud.android.service.playback.CloudPlaybackService.Broadcasts;
import static java.lang.Math.max;
import static java.lang.Math.min;

import com.soundcloud.android.R;
import com.soundcloud.android.cache.WaveformCache;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.WaveformData;
import com.soundcloud.android.service.playback.CloudPlaybackService;
import com.soundcloud.android.view.play.WaveformController;
import org.jetbrains.annotations.Nullable;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
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

    private Canvas mTempCanvas = new Canvas();

    private int mAdjustedWidth;
    private WaveformController.WaveformState mWaveformState;
    private int mWaveformErrorCount;
    private WaveformData mWaveformData;

    private int mDrawGroupSize;
    private int mDumpSize;

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

        mDrawGroupSize = context.getResources().getDimensionPixelSize(R.dimen.waveform_group_size);
        mDumpSize = context.getResources().getDimensionPixelSize(R.dimen.waveform_dump_size);
    }

    public void resume() {
        // Update the current track
        setCurrentTrack();
        startRefreshing();
    }

    private void startRefreshing() {
        if (mTrack != null && mTrack.duration > 0 && getWidth() > 0){
            mRefreshDelay = mTrack.duration / getWidth();
            setProgress((int) CloudPlaybackService.getCurrentProgress());
            if (CloudPlaybackService.getPlaybackState().isSupposedToBePlaying()) queueNextRefresh(mRefreshDelay);
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

        mTempCanvas.setBitmap(mWaveformMask);

        float density = getResources().getDisplayMetrics().density;

        int topPartHeight   = (int) (getHeight() * TOP_WAVEFORM_FRACTION);
        int separatorTop    = (int) (topPartHeight - density);
        int separatorBottom = topPartHeight;

        // Grey
        mTempCanvas.drawRect(0, 0,             mAdjustedWidth, getHeight(),     mTopGrey);
        mTempCanvas.drawRect(0, topPartHeight, mAdjustedWidth, getHeight(),     mBottomGrey);
        mTempCanvas.drawRect(0, separatorTop,  mAdjustedWidth, separatorBottom, mSeparatorGrey);

        float playedFraction = (float) getProgress() / (float) getMax();
        playedFraction = min(max(playedFraction, 0), getMax());

        // Make sure to at least draw an 1dp line of progress
        int progressWidth = (int) max(mAdjustedWidth * playedFraction, density);

        // Orange
        mTempCanvas.drawRect(0, 0,             progressWidth, getHeight(),     mTopOrange);
        mTempCanvas.drawRect(0, topPartHeight, progressWidth, getHeight(),     mBottomOrange);
        mTempCanvas.drawRect(0, separatorTop,  progressWidth, separatorBottom, mSeparatorOrange);

        canvas.drawBitmap(
            mWaveformMask,
            mCanvasRect,
            mCanvasRect,
            null
        );
    }

    private void setWaveformMask() {
        this.mWaveformMask = createWaveformMask(mWaveformData, mAdjustedWidth, getHeight(), mDrawGroupSize, mDumpSize);
    }

    public BroadcastReceiver getStatusListener() {
        return mStatusListener;
    }

    private final BroadcastReceiver mStatusListener = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Broadcasts.META_CHANGED)) {
                setCurrentTrack();

            } else if (action.equals(Broadcasts.PLAYSTATE_CHANGED)) {
              if (intent.getBooleanExtra(CloudPlaybackService.BroadcastExtras.isPlaying, false)){
                  startRefreshing();
              } else {
                  stopRefreshing();
              }

            } else if (action.equals(Broadcasts.SEEK_COMPLETE) || action.equals(Broadcasts.SEEKING)) {
                if (CloudPlaybackService.getPlaybackState().isSupposedToBePlaying()) queueNextRefresh(mRefreshDelay);
            }
        }
    };

    public void setWaveform(WaveformData waveformData) {
        mWaveformData = waveformData;
        setWaveformMask();
    }

    private static Bitmap createWaveformMask(WaveformData waveformData, int width, int height, int groupSize, int dumpSize) {
        if (waveformData == null || width == 0 || height == 0) return null;

        Bitmap mask = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_4444);
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

        int acc = 0;
        int groupIndex = 0;
        int dumpIndex = -1;
        for (int i = 0; i < scaled.samples.length; i++) {
            if (dumpIndex >= 0) {
                canvas.drawLine(
                        i, 0,
                        i, height,
                        xor
                );
                dumpIndex++;
                if (dumpIndex == dumpSize) {
                    dumpIndex = -1; // cancel dumping
                }
            } else {
                acc += scaled.samples[i];
                groupIndex++;
                if (groupIndex == groupSize || i == scaled.samples.length - 1) {
                    final int sample = acc / groupIndex;
                    for (int j = i - groupIndex + 1; j <= i; j++) {
                        final float scaledHeight1 = (sample * (float) dstHeight / waveformData.maxAmplitude);
                        canvas.drawLine(
                                j, 0,
                                j, dstHeight - scaledHeight1,
                                xor
                        );

                        final float scaledHeight2 = (sample * (float) (height - dstHeight) / waveformData.maxAmplitude);
                        canvas.drawLine(
                                j, dstHeight + scaledHeight2,
                                j, height,
                                xor
                        );
                    }
                    acc = groupIndex = dumpIndex = 0;
                }
            }


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
