package com.soundcloud.android.view.play;

import com.soundcloud.android.R;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.service.playback.CloudPlaybackService;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Parcel;
import android.view.View;
import android.widget.RemoteViews;

public abstract class PlaybackRemoteViews extends RemoteViews {
    private int mPlayBtnId;
    private int mPauseBtnId;

    protected Track mTrack;
    protected boolean mIsPlaying;

    public PlaybackRemoteViews(String packageName, int layoutId, int playBtnId, int pauseBtnId) {
        super(packageName, layoutId);
        mPlayBtnId = playBtnId;
        mPauseBtnId = pauseBtnId;
    }

    @SuppressWarnings("UnusedDeclaration")
    public PlaybackRemoteViews(Parcel parcel) {
        super(parcel);
    }

    protected void linkPlayerControls(Context context) {
        // Connect up various buttons and touch events
        final Intent previous = new Intent(CloudPlaybackService.PREVIOUS_ACTION);
        setOnClickPendingIntent(R.id.prev, PendingIntent.getService(context,
                0 /* requestCode */, previous, 0 /* flags */));

        final Intent pause = new Intent(CloudPlaybackService.TOGGLEPAUSE_ACTION);
        setOnClickPendingIntent(R.id.pause, PendingIntent.getService(context,
                0 /* requestCode */, pause, 0 /* flags */));

        final Intent next = new Intent(CloudPlaybackService.NEXT_ACTION);
        setOnClickPendingIntent(R.id.next, PendingIntent.getService(context,
                0 /* requestCode */, next, 0 /* flags */));
    }


    public void setCurrentTrackTitle(CharSequence title) {
        setTextViewText(R.id.title_txt, title);
    }

    public void setCurrentUsername(CharSequence username) {
        setTextViewText(R.id.user_txt, username);
        setViewVisibility(R.id.by_txt, View.VISIBLE);
        setViewVisibility(R.id.user_txt, View.VISIBLE);
    }

    public void setPlaybackStatus(boolean playing) {
        setImageViewResource(R.id.pause, playing ? mPauseBtnId : mPlayBtnId);
    }

    public void setIcon(Bitmap icon){
        setImageViewBitmap(R.id.icon,icon);
        setViewVisibility(R.id.icon,View.VISIBLE);
    }

    public void clearIcon(){
        setViewVisibility(R.id.icon,View.GONE);
    }
}
