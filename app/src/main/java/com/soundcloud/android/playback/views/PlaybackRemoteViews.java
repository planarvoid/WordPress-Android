package com.soundcloud.android.playback.views;

import com.soundcloud.android.R;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;

import android.os.Parcel;
import android.text.TextUtils;
import android.view.View;
import android.widget.RemoteViews;

public abstract class PlaybackRemoteViews extends RemoteViews {

    private int playBtnId;
    private int pauseBtnId;

    protected PublicApiTrack track;
    protected boolean isPlaying;

    public PlaybackRemoteViews(String packageName, int layoutId, int playBtnId, int pauseBtnId) {
        super(packageName, layoutId);
        this.playBtnId = playBtnId;
        this.pauseBtnId = pauseBtnId;
    }

    public PlaybackRemoteViews(Parcel parcel) {
        super(parcel);
        playBtnId = parcel.readInt();
        pauseBtnId = parcel.readInt();
        isPlaying = parcel.readByte() == 1;
        track = parcel.readParcelable(PublicApiTrack.class.getClassLoader());
    }

    public void setCurrentTrackTitle(CharSequence title) {
        setTextViewText(R.id.title_txt, title);
    }

    public void setLikeShown(boolean showLike) {
        setViewVisibility(R.id.btn_like, showLike ? View.VISIBLE : View.GONE);
    }

    public void setCurrentCreator(CharSequence creator) {
        if (TextUtils.isEmpty(creator)){
            setViewVisibility(R.id.separator_txt, View.GONE);
            setViewVisibility(R.id.user_txt, View.GONE);
        } else {
            setTextViewText(R.id.user_txt, creator);
            setViewVisibility(R.id.separator_txt, View.VISIBLE);
            setViewVisibility(R.id.user_txt, View.VISIBLE);
        }
    }

    public void setPlaybackStatus(boolean playing) {
        setImageViewResource(R.id.toggle_playback, playing ? pauseBtnId : playBtnId);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(playBtnId);
        dest.writeInt(pauseBtnId);
        dest.writeByte((byte) (isPlaying ? 1 : 0));
        dest.writeParcelable(track, 0);
    }
}
