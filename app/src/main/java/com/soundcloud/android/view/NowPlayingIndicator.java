package com.soundcloud.android.view;

import android.content.*;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import com.google.android.imageloader.ImageLoader;
import com.soundcloud.android.Actions;
import com.soundcloud.android.activity.ScPlayer;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.service.LocalBinder;
import com.soundcloud.android.service.playback.CloudPlaybackService;
import com.soundcloud.android.utils.AndroidUtils;
import com.soundcloud.android.view.play.PlayerTrackView;

import static java.lang.Math.max;
import static java.lang.Math.min;

public class NowPlayingIndicator extends ProgressBar {
    private static final int REFRESH = 1;

    private static final int BACKGROUND_COLORS[] = {
        0xFF1B1B1B,
        0xFF1B1B1B,
        0xFF131313,
        0xFF222222,
        0xFF222222
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

    private Bitmap mWaveform;
    private Bitmap mWaveformMask;

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
        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (context.getClass().equals(ScPlayer.class)) return;

                Intent intent = new Intent(context, ScPlayer.class);
                context.startActivity(intent);
            }
        });



        long next = refreshNow();
        queueNextRefresh(next);

        IntentFilter f = new IntentFilter();
        f.addAction(CloudPlaybackService.PLAYLIST_CHANGED);
        f.addAction(CloudPlaybackService.PLAYSTATE_CHANGED);
        f.addAction(CloudPlaybackService.META_CHANGED);
        f.addAction(CloudPlaybackService.PLAYBACK_ERROR);
        f.addAction(CloudPlaybackService.TRACK_UNAVAILABLE);
        f.addAction(CloudPlaybackService.STREAM_DIED);
        f.addAction(CloudPlaybackService.PLAYBACK_COMPLETE);
        f.addAction(CloudPlaybackService.BUFFERING);
        f.addAction(CloudPlaybackService.BUFFERING_COMPLETE);
        f.addAction(CloudPlaybackService.COMMENTS_LOADED);
        f.addAction(CloudPlaybackService.SEEKING);
        f.addAction(CloudPlaybackService.SEEK_COMPLETE);
        f.addAction(CloudPlaybackService.FAVORITE_SET);
        f.addAction(Actions.COMMENT_ADDED);
        context.registerReceiver(mStatusListener, new IntentFilter(f));
    }

    public void startRefresh() {
    }

    public void stopRefresh() {
        mHandler.removeMessages(REFRESH);
    }

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case REFRESH:
                    long next = refreshNow();
                    queueNextRefresh(next);
                    return;
            }
        }
    };

    private void queueNextRefresh(long delay) {
        Message msg = mHandler.obtainMessage(REFRESH);
        mHandler.removeMessages(REFRESH);
        mHandler.sendMessageDelayed(msg, delay);
    }

    private int getRefreshDelay() {
        if (getWidth() == 0) return 1000;

        return 1000 / getWidth();
    }

    private long refreshNow() {
        Track track = CloudPlaybackService.getCurrentTrack();

        if (track == null) return getRefreshDelay();

        long progress  = CloudPlaybackService.getCurrentProgress();
        long remaining = getRefreshDelay() - (progress % getRefreshDelay());

        setProgress((int) CloudPlaybackService.getCurrentProgress());
        setMax(track.duration);

        return !CloudPlaybackService.getState().isSupposedToBePlaying() ? getRefreshDelay() : remaining;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mWaveformMask == null) return;

        Canvas tmp = new Canvas(mWaveformMask);

        // Grey background
        LinearGradient backgroundGradient = new LinearGradient(0, 0, 0, getHeight(), BACKGROUND_COLORS, COLOR_STOPS, Shader.TileMode.MIRROR);

        Paint background = new Paint();
        background.setShader(backgroundGradient);
        background.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));

        tmp.drawRect(0, 0, getWidth(), getHeight(), background);

        // Orange foreground
        LinearGradient foregroundGradient = new LinearGradient(0, 0, 0, getHeight(), FOREGROUND_COLORS, COLOR_STOPS, Shader.TileMode.MIRROR);

        Paint foreground = new Paint();
        foreground.setShader(foregroundGradient);
        foreground.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));

        float fraction = (float) getProgress() / (float) getMax();
        fraction = min(max(fraction, 0), getMax());

        tmp.drawRect(0, 0, getWidth() * fraction, getHeight(), foreground);

        canvas.drawBitmap(
            mWaveformMask,
            new Rect(0, 0, getWidth(), getHeight()),
            new Rect(0, 0, getWidth(), getHeight()),
            null
        );
    }

    private final BroadcastReceiver mStatusListener = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
        ImageLoader.get(getContext()).getBitmap(
            CloudPlaybackService.getCurrentTrack().waveform_url,
            new ImageLoader.BitmapCallback() {
                public void onImageLoaded(Bitmap mBitmap, String uri) {
                    setWaveform(mBitmap);
                }
            }
        );
        }
    };

    public Bitmap getWaveform() {
        return mWaveform;
    }

    public void setWaveform(Bitmap waveform) {
        this.mWaveform     = waveform;
        this.mWaveformMask = createWaveformMask(waveform, getWidth(), getHeight());
    }

    private static Bitmap createWaveformMask(Bitmap waveform, int width, int height) {
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
