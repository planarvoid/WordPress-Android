package com.soundcloud.android.playback.views;

import com.soundcloud.android.R;
import com.soundcloud.android.model.Track;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.text.TextUtils;
import android.view.View;
import android.widget.RemoteViews;

public abstract class PlaybackRemoteViews extends RemoteViews {

    private int playBtnId;
    private int pauseBtnId;

    protected Track track;
    protected boolean isPlaying;

    public PlaybackRemoteViews(String packageName, int layoutId, int playBtnId, int pauseBtnId) {
        super(packageName, layoutId);
        this.playBtnId = playBtnId;
        this.pauseBtnId = pauseBtnId;
    }

    @SuppressWarnings("UnusedDeclaration")
    public PlaybackRemoteViews(Parcel parcel) {
        super(parcel);
    }

    public void setCurrentTrackTitle(CharSequence title) {
        setTextViewText(R.id.title_txt, title);
    }

    public void setCurrentUsername(CharSequence username) {
        if (TextUtils.isEmpty(username)){
            setViewVisibility(R.id.by_txt, View.GONE);
            setViewVisibility(R.id.user_txt, View.GONE);
        } else {
            setTextViewText(R.id.user_txt, username);
            setViewVisibility(R.id.by_txt, View.VISIBLE);
            setViewVisibility(R.id.user_txt, View.VISIBLE);
        }
    }

    public void setPlaybackStatus(boolean playing) {
        setImageViewResource(R.id.toggle_playback, playing ? pauseBtnId : playBtnId);
    }

    public void setIcon(Bitmap icon){
        setImageViewBitmap(R.id.icon,icon);
        setViewVisibility(R.id.icon,View.VISIBLE);
    }

    public void clearIcon(){
        setViewVisibility(R.id.icon,View.GONE);
    }
}
