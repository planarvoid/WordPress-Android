package com.soundcloud.android.view;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import com.soundcloud.android.Actions;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.UserBrowser;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.service.playback.CloudPlaybackService;

public class PlaybackRemoteViews extends RemoteViews{

    private int mPlayBtnId;
    private int mPauseBtnId;

    public PlaybackRemoteViews(String packageName, int layoutId) {
        this(packageName,layoutId, R.drawable.ic_widget_play_states, R.drawable.ic_widget_pause_states);
    }
    public PlaybackRemoteViews(String packageName, int layoutId, int playBtnId, int pauseBtnId) {
        super(packageName, layoutId);
        mPlayBtnId = playBtnId;
        mPauseBtnId = pauseBtnId;
    }

    public PlaybackRemoteViews(Parcel parcel) {
        super(parcel);
    }

    public void linkButtons(Context context, long trackId, long userId, boolean userFavorite) {
        linkButtons(context,trackId,userId,userFavorite,null);
    }

    public void linkButtons(Context context, long trackId, long userId, boolean userFavorite, String fromNotificationFlag) {
        // Connect up various buttons and touch events
        final ComponentName name = new ComponentName(context, CloudPlaybackService.class);
        final Intent previous = new Intent(CloudPlaybackService.PREVIOUS_ACTION).setComponent(name);
        if (!TextUtils.isEmpty(fromNotificationFlag)) previous.putExtra(fromNotificationFlag,true);
        setOnClickPendingIntent(R.id.prev, PendingIntent.getService(context,
                0 /* requestCode */, previous, 0 /* flags */));

        final Intent toggle = new Intent(CloudPlaybackService.TOGGLEPAUSE_ACTION).setComponent(name);
        if (!TextUtils.isEmpty(fromNotificationFlag)) toggle.putExtra(fromNotificationFlag,true);
        setOnClickPendingIntent(R.id.pause, PendingIntent.getService(context,
                0 /* requestCode */, toggle, 0 /* flags */));

        final Intent next = new Intent(CloudPlaybackService.NEXT_ACTION).setComponent(name);
        if (!TextUtils.isEmpty(fromNotificationFlag)) next.putExtra(fromNotificationFlag,true);
        setOnClickPendingIntent(R.id.next, PendingIntent.getService(context,
                0 /* requestCode */, next, 0 /* flags */));

        final Intent close = new Intent(CloudPlaybackService.STOP_ACTION).setComponent(name);
        if (!TextUtils.isEmpty(fromNotificationFlag)) close.putExtra(fromNotificationFlag,true);
        setOnClickPendingIntent(R.id.close, PendingIntent.getService(context,
                0 /* requestCode */, close, 0 /* flags */));

        if (fromNotificationFlag == null){
            final Intent player = new Intent(Actions.PLAYER).addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
            if (!TextUtils.isEmpty(fromNotificationFlag)) player.putExtra(fromNotificationFlag,true);
            setOnClickPendingIntent(R.id.title_txt, PendingIntent.getActivity(context, 0, player, 0));
        }

        if (trackId != -1) {
            if (fromNotificationFlag == null){
                final Intent browser = new Intent(context, UserBrowser.class).putExtra("userId", userId);
                if (!TextUtils.isEmpty(fromNotificationFlag)) browser.putExtra(fromNotificationFlag,true);
                setOnClickPendingIntent(R.id.user_txt,
                        PendingIntent.getActivity(context, 0, browser, PendingIntent.FLAG_UPDATE_CURRENT));
            }

            final Intent toggleLike = new Intent(
                    userFavorite ?
                            CloudPlaybackService.REMOVE_FAVORITE :
                            CloudPlaybackService.ADD_FAVORITE)
                    .setComponent(name)
                    .putExtra("trackId", trackId);
            if (!TextUtils.isEmpty(fromNotificationFlag)) toggleLike.putExtra(fromNotificationFlag,true);
            setOnClickPendingIntent(R.id.btn_favorite, PendingIntent.getService(context,
                    0 /* requestCode */, toggleLike, PendingIntent.FLAG_UPDATE_CURRENT));
        }
    }

    public void setCurrentTrack(CharSequence title, CharSequence username) {
        setTextViewText(R.id.title_txt, title);
        setTextViewText(R.id.user_txt, username);
        setViewVisibility(R.id.by_txt, View.VISIBLE);
        setViewVisibility(R.id.user_txt, View.VISIBLE);
    }

    public void setIcon(Bitmap icon){
        setImageViewBitmap(R.id.icon,icon);
        setViewVisibility(R.id.icon,View.VISIBLE);
    }

    public void clearIcon(){
        setViewVisibility(R.id.icon,View.GONE);
    }

    public void setPlaybackStatus(boolean playing) {
        setImageViewResource(R.id.pause, playing ? mPauseBtnId : mPlayBtnId);
    }
}
