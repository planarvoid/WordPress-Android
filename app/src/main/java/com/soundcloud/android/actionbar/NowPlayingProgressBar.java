package com.soundcloud.android.actionbar;

import static com.soundcloud.android.playback.service.PlaybackService.Broadcasts;
import static java.lang.Math.max;
import static java.lang.Math.min;

import com.soundcloud.android.cache.WaveformCache;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.WaveformData;
import com.soundcloud.android.playback.service.Playa;
import com.soundcloud.android.playback.service.PlaybackStateProvider;
import com.soundcloud.android.playback.views.WaveformControllerLayout;
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

public class NowPlayingProgressBar extends ProgressBar {
    private static final int REFRESH = 1;

    private static final int TOP_ORANGE = 0xFFFF4400;
    private static final int SEPARATOR_ORANGE = 0xFF661400;
    private static final int BOTTOM_ORANGE = 0xFFAA2200;

    private static final int TOP_GREY = 0xFF666666;
    private static final int SEPARATOR_GREY = 0xFF2D2D2D;
    private static final int BOTTOM_GREY = 0xFF535353;

    private static final float TOP_WAVEFORM_FRACTION = 0.75f;

    @Nullable
    private Bitmap waveformMask;
    @Nullable
    private Track track;

    private long refreshDelay;

    private Paint topOrange, separatorOrange, bottomOrange, topGrey, separatorGrey, bottomGrey;
    private Rect canvasRect;
    private Canvas tempCanvas = new Canvas();

    private int adjustedWidth, waveformErrorCount;
    private WaveformControllerLayout.WaveformState waveformState;
    private WaveformData waveformData;
    private PlaybackStateProvider playbackStateProvider;

    private final Handler handler = new RefreshHandler(this);

    @SuppressWarnings("UnusedDeclaration")
    public NowPlayingProgressBar(Context context) {
        super(context);
        init();
    }

    @SuppressWarnings("UnusedDeclaration")
    public NowPlayingProgressBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    @SuppressWarnings("UnusedDeclaration")
    public NowPlayingProgressBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        playbackStateProvider = new PlaybackStateProvider();

        PorterDuffXfermode sourceIn = new PorterDuffXfermode(PorterDuff.Mode.SRC_IN);

        topOrange = new Paint();
        topOrange.setColor(TOP_ORANGE);
        topOrange.setXfermode(sourceIn);

        separatorOrange = new Paint();
        separatorOrange.setColor(SEPARATOR_ORANGE);
        separatorOrange.setXfermode(sourceIn);

        bottomOrange = new Paint();
        bottomOrange.setColor(BOTTOM_ORANGE);
        bottomOrange.setXfermode(sourceIn);

        topGrey = new Paint();
        topGrey.setColor(TOP_GREY);
        topGrey.setXfermode(sourceIn);

        separatorGrey = new Paint();
        separatorGrey.setColor(SEPARATOR_GREY);
        separatorGrey.setXfermode(sourceIn);

        bottomGrey = new Paint();
        bottomGrey.setColor(BOTTOM_GREY);
        bottomGrey.setXfermode(sourceIn);

        setIndeterminate(false);
    }

    public void resume() {
        // Update the current track
        setCurrentTrack();
        startRefreshing();
    }

    private void startRefreshing() {
        if (track != null && track.duration > 0 && getWidth() > 0) {
            refreshDelay = track.duration / getWidth();
            setProgress((int) playbackStateProvider.getPlayProgress());
            if (playbackStateProvider.isSupposedToBePlaying()) queueNextRefresh(refreshDelay);
        }
    }

    private void setCurrentTrack() {
        final Track currentTrack = playbackStateProvider.getCurrentTrack();
        if (track != currentTrack || waveformState == WaveformControllerLayout.WaveformState.ERROR) {

            if (track != currentTrack) waveformErrorCount = 0;

            track = currentTrack;
            setMax(track == null ? 0 : track.duration);
            setProgress((int) playbackStateProvider.getPlayProgress());

            if (track == null || !track.hasWaveform() || waveformErrorCount > 3) {
                setDefaultWaveform();
            } else {
                if (WaveformCache.get().getData(track, new WaveformCache.WaveformCallback() {
                    @Override
                    public void onWaveformDataLoaded(Track track, WaveformData data, boolean fromCache) {
                        if (track.equals(NowPlayingProgressBar.this.track)) {
                            waveformErrorCount = 0;
                            waveformState = WaveformControllerLayout.WaveformState.OK;
                            setWaveform(data);
                        }
                    }

                    @Override
                    public void onWaveformError(Track track) {
                        if (track.equals(NowPlayingProgressBar.this.track)) {
                            waveformState = WaveformControllerLayout.WaveformState.ERROR;
                            waveformErrorCount++;
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

    private void setDefaultWaveform() {
        // TODO, set default bitmap
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        adjustedWidth = getWidth() - 0 * 2;
        canvasRect = new Rect(0, 0, adjustedWidth, getHeight());

        setWaveformMask();
        startRefreshing();
    }

    public void destroy() {
        stopRefreshing();
        waveformMask = null;
    }

    private void stopRefreshing() {
        handler.removeMessages(REFRESH);
    }

    private void queueNextRefresh(long delay) {
        Message msg = handler.obtainMessage(REFRESH);
        handler.removeMessages(REFRESH);
        if (delay != -1) handler.sendMessageDelayed(msg, delay);
    }


    @Override
    protected void onDraw(Canvas canvas) {
        if (waveformMask == null) return;

        tempCanvas.setBitmap(waveformMask);

        float density = getResources().getDisplayMetrics().density;

        int topPartHeight = (int) (getHeight() * TOP_WAVEFORM_FRACTION);
        int separatorTop = (int) (topPartHeight - density);
        int separatorBottom = topPartHeight;

        // Grey
        tempCanvas.drawRect(0, 0, adjustedWidth, getHeight(), topGrey);
        tempCanvas.drawRect(0, topPartHeight, adjustedWidth, getHeight(), bottomGrey);
        tempCanvas.drawRect(0, separatorTop, adjustedWidth, separatorBottom, separatorGrey);

        float playedFraction = (float) getProgress() / (float) getMax();
        playedFraction = min(max(playedFraction, 0), getMax());

        // Make sure to at least draw an 1dp line of progress
        int progressWidth = (int) max(adjustedWidth * playedFraction, density);

        // Orange
        tempCanvas.drawRect(0, 0, progressWidth, getHeight(), topOrange);
        tempCanvas.drawRect(0, topPartHeight, progressWidth, getHeight(), bottomOrange);
        tempCanvas.drawRect(0, separatorTop, progressWidth, separatorBottom, separatorOrange);

        canvas.drawBitmap(
                waveformMask,
                canvasRect,
                canvasRect,
                null
        );
    }

    private void setWaveformMask() {
        this.waveformMask = createWaveformMask(waveformData, adjustedWidth, getHeight());
    }

    public BroadcastReceiver getStatusListener() {
        return statusListener;
    }

    private final BroadcastReceiver statusListener = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Broadcasts.META_CHANGED)) {
                setCurrentTrack();

            } else if (action.equals(Broadcasts.PLAYSTATE_CHANGED)) {
                if (Playa.StateTransition.fromIntent(intent).getNewState().isPlaying()) {
                    startRefreshing();
                } else {
                    stopRefreshing();
                }

            }
        }
    };

    public void setWaveform(WaveformData waveformData) {
        this.waveformData = waveformData;
        setWaveformMask();
    }

    private static Bitmap createWaveformMask(WaveformData waveformData, int width, int height) {
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
        for (int i = 0; i < scaled.samples.length; i++) {
            final float scaledHeight1 = (scaled.samples[i] * (float) dstHeight / waveformData.maxAmplitude);
            canvas.drawLine(
                    i, 0,
                    i, dstHeight - scaledHeight1,
                    xor
            );

            final float scaledHeight2 = (scaled.samples[i] * (float) (height - dstHeight) / waveformData.maxAmplitude);
            canvas.drawLine(
                    i, dstHeight + scaledHeight2,
                    i, height,
                    xor
            );
        }

        return mask;
    }

    private static final class RefreshHandler extends Handler {
        private WeakReference<NowPlayingProgressBar> ref;

        private RefreshHandler(NowPlayingProgressBar nowPlaying) {
            this.ref = new WeakReference<NowPlayingProgressBar>(nowPlaying);
        }

        @Override
        public void handleMessage(Message msg) {
            final NowPlayingProgressBar nowPlaying = ref.get();
            if (nowPlaying != null && nowPlaying.track != null) {
                switch (msg.what) {
                    case REFRESH:
                        nowPlaying.setProgress((int) nowPlaying.playbackStateProvider.getPlayProgress());
                        nowPlaying.queueNextRefresh(nowPlaying.refreshDelay);
                }
            }
        }
    }
}
