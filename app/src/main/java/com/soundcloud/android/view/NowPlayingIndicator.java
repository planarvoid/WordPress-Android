package com.soundcloud.android.view;

import android.content.*;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import com.soundcloud.android.Actions;
import com.soundcloud.android.activity.ScPlayer;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.service.LocalBinder;
import com.soundcloud.android.service.playback.CloudPlaybackService;
import com.soundcloud.android.utils.AndroidUtils;
import com.soundcloud.android.view.play.PlayerTrackView;

public class NowPlayingIndicator extends ProgressBar {
    private static final int REFRESH = 1;
    private static final int REFRESH_DELAY = 50;

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

    }

    public void startRefresh() {
        long next = refreshNow();
        queueNextRefresh(next);
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

    private long refreshNow() {
        Track track = CloudPlaybackService.getCurrentTrack();

        if (track == null) return REFRESH_DELAY;

        long progress  = CloudPlaybackService.getCurrentProgress();
        long remaining = REFRESH_DELAY - (progress % REFRESH_DELAY);

        setProgress((int) CloudPlaybackService.getCurrentProgress());
        setMax(CloudPlaybackService.getCurrentTrack().duration);

        return !CloudPlaybackService.getState().isSupposedToBePlaying() ? REFRESH_DELAY  : remaining;
    }
}
